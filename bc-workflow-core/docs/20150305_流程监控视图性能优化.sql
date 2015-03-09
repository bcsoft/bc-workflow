-- 流程监控性能优化

-- 创建记录流程标题和流水号信息的表
drop table if exists bc_wf_procinst_info;
CREATE TABLE bc_wf_procinst_info (
  id varchar(64) NOT NULL,
  info json,	-- {subject|wf_code}
  CONSTRAINT bcpk_wf_procinst_info PRIMARY KEY (id),
  CONSTRAINT bcfk_wf_procinst_info_id FOREIGN KEY (id)
      REFERENCES act_hi_procinst (id_) ON DELETE cascade
);
--CREATE INDEX bcidx_wf_procinst_info_info ON bc_wf_procinst_info (info);
insert into bc_wf_procinst_info(id, info)
	select i.id_
	, (select row_to_json(t) from (
			select (select text_ from act_hi_detail d where d.proc_inst_id_ = i.id_ and d.name_ = 'subject' order by rev_ desc limit 1) as subject
				, (select text_ from act_hi_detail d where d.proc_inst_id_ = i.id_ and d.name_ = 'wf_code' order by rev_ desc limit 1) as wf_code
		) t) as info
	from act_hi_procinst i;
--select i.*, i.info->>'subject', i.info->>'wf_code' wf_code from bc_wf_procinst_info i where i.info->>'subject' like '%客管%' or i.info->>'wf_code' like '%客管%';

-- DROP FUNCTION IF EXISTS wf_procinst_info__auto_insert_or_update();
CREATE OR REPLACE FUNCTION wf_procinst_info__auto_insert_or_update()
	RETURNS trigger AS 
	$BODY$
	/** 自动插入或更新 bc_wf_procinst_info 表的信息
	 */
	BEGIN
		if (NEW.name_ = 'subject' or NEW.name_ = 'wf_code') and NEW.task_id_ is null then
			-- 新数据
			if (select 0 from bc_wf_procinst_info w where w.id = NEW.proc_inst_id_) is null then
				if NEW.name_ = 'subject' then -- 当前行为“主题”行的数据
					insert into bc_wf_procinst_info(id, info)
						select NEW.proc_inst_id_, (select row_to_json(t) from (select NEW.text_ as subject)t);
				elsif NEW.name_ = 'wf_code' then -- 当前行为“流水号”行的数据
					insert into bc_wf_procinst_info(id, info)
						select NEW.proc_inst_id_, (select row_to_json(t) from (select NEW.text_ as wf_code)t);
				else 
					insert into bc_wf_procinst_info(id, info)
						select NEW.proc_inst_id_, '{"subject":null,"wf_code":null}'::json;
				end if;
			-- 已存在的数据直接更新为新数据
			else
				update bc_wf_procinst_info set info = json_update(info, NEW.name_, NEW.text_)
					where id = NEW.proc_inst_id_;
			end if;
		end if;
		return null;
	END;
	$BODY$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS wf_procinst_info__auto_insert_or_update ON ACT_HI_DETAIL;
CREATE TRIGGER wf_procinst_info__auto_insert_or_update after INSERT OR UPDATE ON ACT_HI_DETAIL
	FOR EACH ROW EXECUTE PROCEDURE wf_procinst_info__auto_insert_or_update();


-- 流程监控视图 sql
-- 原来的本地查询约4s，优化后约160ms
--explain analyze 
select p.*
,getprocesstodotasknames(p.id) as  todo_names
from (
	select a.id_ id, b.name_ as procinstname, a.start_time_ start_time, a.end_time_ end_time, a.duration_ duration
	, f.suspension_state_ status
	, e.version_ as version, c.name start_user_name
	, i.info->>'wf_code' as wf_code
	, i.info->>'subject' as subject
	--,getaccessactors4docidtype4docidcharacter(a.id_,'ProcessInstance') 
	from act_hi_procinst a 
	inner join act_re_procdef b on b.id_ = a.proc_def_id_ 
	inner join bc_wf_procinst_info i on i.id = a.id_ 
	inner join act_re_deployment d on d.id_=b.deployment_id_ 
	inner join bc_wf_deploy e on e.deployment_id=d.id_ 
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
		i.info->>'subject' like '%客管%' or i.info->>'wf_code' like '%客管%'
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
CREATE OR REPLACE FUNCTION json_update(j json, k text, v anyelement) RETURNS json AS $$
    /** 更新 json 对象
     * @param j     原 json 对象
     * @param k     json 对象的 key
     * @param v     json 对象的 value。任意类型
     */
	SELECT concat('{', string_agg(to_json(key) || ':' || value, ','), '}')::json
	FROM (
		SELECT * FROM json_each($1) WHERE key != $2
		UNION ALL
		SELECT $2, to_json($3)
	) t;
$$ LANGUAGE sql;