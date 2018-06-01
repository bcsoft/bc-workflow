-- 流程监控性能优化

-- 创建记录流程标题和流水号信息的表
drop table if exists bc_wf_procinst_info;
create table bc_wf_procinst_info (
  id   varchar(64) not null,
  info json, -- {subject|wf_code}
  constraint bcpk_wf_procinst_info primary key (id),
  constraint bcfk_wf_procinst_info_id foreign key (id)
  references act_hi_procinst (id_) on delete cascade
);
--CREATE INDEX bcidx_wf_procinst_info_info ON bc_wf_procinst_info (info);
insert into bc_wf_procinst_info (id, info)
  select i.id_, (select row_to_json(t)
                 from (
                        select (select text_
                                from act_hi_detail d
                                where d.proc_inst_id_ = i.id_ and d.name_ = 'subject' and d.task_id_ is null
                                order by rev_ desc limit 1) as subject, (select text_
                                                                         from act_hi_detail d
                                                                         where d.proc_inst_id_ = i.id_ and
                                                                               d.name_ = 'wf_code'
                                                                         order by rev_ desc limit 1) as wf_code
                      ) t) as info
  from act_hi_procinst i;
--select i.*, i.info->>'subject', i.info->>'wf_code' wf_code from bc_wf_procinst_info i where i.info->>'subject' like '%客管%' or i.info->>'wf_code' like '%客管%';

-- DROP FUNCTION IF EXISTS wf_procinst_info__auto_insert();
create or replace function wf_procinst_info__auto_insert()
  returns trigger as
$BODY$
/** 发起流程，自动插入 bc_wf_procinst_info 表的标题和流水号信息
 */
begin
  insert into bc_wf_procinst_info (id, info)
    select NEW.id_, '{
      "subject": null,
      "wf_code": null
    }' :: json;
  return null;
end;
$BODY$ language plpgsql;

drop trigger if exists wf_procinst_info__auto_insert on ACT_HI_PROCINST;
create trigger wf_procinst_info__auto_insert
  after insert on ACT_HI_PROCINST
  for each row execute procedure wf_procinst_info__auto_insert();


-- DROP FUNCTION IF EXISTS wf_procinst_info__auto_update();
create or replace function wf_procinst_info__auto_update()
  returns trigger as
$BODY$
/** 自动更新 bc_wf_procinst_info 表的信息
 */
begin
  if (NEW.name_ = 'subject' or NEW.name_ = 'wf_code') and NEW.task_id_ is null
  then
    update bc_wf_procinst_info set info = json_update(info, NEW.name_, NEW.text_)
    where id = NEW.proc_inst_id_;
  end if;
  return null;
end;
$BODY$ language plpgsql;

drop trigger if exists wf_procinst_info__auto_update on ACT_HI_DETAIL;
create trigger wf_procinst_info__auto_update
  after insert or update on ACT_HI_DETAIL
  for each row execute procedure wf_procinst_info__auto_update();

-- 流程监控视图 sql
-- 原来的本地查询约4s，优化后约160ms
--explain analyze 
select p.*, getprocesstodotasknames(p.id) as todo_names
from (
       select a.id_ id, b.name_ as procinstname, a.start_time_ start_time, a.end_time_ end_time, a.duration_ duration,
              f.suspension_state_ status, e.version_ as version, c.name start_user_name,
              i.info ->> 'wf_code' as wf_code, i.info ->> 'subject' as subject
       --,getaccessactors4docidtype4docidcharacter(a.id_,'ProcessInstance')
       from act_hi_procinst a
       inner join act_re_procdef b on b.id_ = a.proc_def_id_
       inner join bc_wf_procinst_info i on i.id = a.id_
       inner join act_re_deployment d on d.id_ = b.deployment_id_
       inner join bc_wf_deploy e on e.deployment_id = d.id_
       left join bc_identity_actor c on c.code = a.start_user_id_
       left join act_ru_execution f on a.id_ = f.proc_inst_id_ -- 暂停及流转中流程需要join此表
       where f.parent_id_ is null -- 不考虑 activiti 的子流程

             -- 流转中
             --and a.end_time_ is null  and ((b.suspension_state_ = 1) and (f.suspension_state_ = 1))

             -- 已暂停
             --and a.end_time_ is null  and ((b.suspension_state_ = 2) and (f.suspension_state_ = 2))

             -- 已结束
             --and a.end_time_ is not null

             -- 模糊搜索
             and (
               -- 流程标题、流水号
               i.info ->> 'subject' like '%客管%' or i.info ->> 'wf_code' like '%客管%'
               -- 流程名称
               or b.name_ like '%客管%'
               -- 发起人
               or c.name like '%客管%'
             )

       order by a.start_time_ desc
     ) p
limit 10;

-- 更新 json 对象的值，传入的 key 不存在，则直接添加到 json 对象中
-- 参考：http://stackoverflow.com/questions/18209625/how-do-i-modify-fields-inside-the-new-postgresql-json-datatype
create or replace function json_update(j json, k text, v anyelement) returns json as $$
/** 更新 json 对象
 * @param j     原 json 对象
 * @param k     json 对象的 key
 * @param v     json 对象的 value。任意类型
 */
select concat('{', string_agg(to_json(key) || ':' || value, ','), '}') :: json
from (
       select *
       from json_each($1)
       where key != $2
       union all
       select $2, to_json($3)
     ) t;
$$ language sql;

-- 修复 bc_wf_procinst_info 数据错误
update bc_wf_procinst_info as pi set info = t.info
from (
       select i.id_, (select row_to_json(t)
                      from (
                             select (select text_
                                     from act_hi_detail d
                                     where d.proc_inst_id_ = i.id_ and d.name_ = 'subject' and d.task_id_ is null
                                     order by rev_ desc limit 1) as subject, (select text_
                                                                              from act_hi_detail d
                                                                              where d.proc_inst_id_ = i.id_ and
                                                                                    d.name_ = 'wf_code' and
                                                                                    d.task_id_ is null
                                                                              order by rev_ desc limit 1) as wf_code
                           ) t) as info
       from act_hi_procinst i
     ) t
where t.id_ = pi.id;