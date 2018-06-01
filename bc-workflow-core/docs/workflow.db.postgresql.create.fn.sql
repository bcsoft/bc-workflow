-- ##bc-workflow 的 postgresql 自定义函数和存储过程##

-- 流程部署使用人列函数
create or replace function getdeployuser(deployid integer)
  returns character varying as
$BODY$
declare
  -- 使用者字符串
  users   character varying;
  -- 记录使用者字符串长度
  _length integer;
  -- 一行结果的记录
  rowinfo record;
begin
  -- 初始化变量
  users := '';
  _length := 0;
  for rowinfo in select ia.name
                 from bc_wf_deploy d
                 inner join bc_wf_deploy_actor da on da.did = d.id
                 inner join bc_identity_actor ia on da.aid = ia.id
                 where d.id = deployid
    -- 循环开始
  loop
    users := users || rowinfo.name || ',';
  end loop;
  _length := length(users);
  if _length > 0
  then
    users := substring(users from 1 for _length - 1);
  end if;
  return users;
end;
$BODY$
language plpgsql;

-- 获取流程实例名称为subject的流程变量的值
--	id: 流程实例ID
create or replace function getProcessInstanceSubject(id in character varying) returns character varying as $$
declare
  --定义变量
  subject varchar(4000);
begin
  select text_
  from act_hi_detail
  where name_ = 'subject' and proc_inst_id_ = id and task_id_ is null
  order by proc_inst_id_ desc, time_ desc limit 1
  into subject;
  return subject;
end;
$$ language plpgsql;
