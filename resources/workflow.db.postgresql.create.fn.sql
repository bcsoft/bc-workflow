-- ##bc-workflow 的 postgresql 自定义函数和存储过程##

-- 流程部署使用人列函数
CREATE OR REPLACE FUNCTION getdeployuser(deployid INTEGER)
	RETURNS CHARACTER VARYING  AS
$BODY$
DECLARE
		-- 使用者字符串
		users CHARACTER VARYING;
		-- 记录使用者字符串长度
		_length INTEGER;
		-- 一行结果的记录	
		rowinfo RECORD;
BEGIN
		-- 初始化变量
		users:='';
		_length:=0;
		FOR rowinfo IN SELECT ia.name
						FROM bc_wf_deploy d
						INNER JOIN bc_wf_deploy_actor da on da.did=d.id
						INNER JOIN bc_identity_actor ia on da.aid=ia.id
						WHERE d.id=deployid
		-- 循环开始
		LOOP
			users:=users||rowinfo.name||',';
		END LOOP;
		_length:=length(users);
		IF _length>0 THEN
		users:=substring(users from 1 for _length-1);
		END IF;
		RETURN users;
END;
$BODY$
LANGUAGE plpgsql;

-- 获取流程实例名称为subject的流程变量的值
--	id: 流程实例ID
CREATE OR REPLACE FUNCTION getProcessInstanceSubject(id IN CHARACTER VARYING) RETURNS CHARACTER VARYING AS $$
DECLARE
	--定义变量
	subject varchar(4000);
BEGIN
	select text_ from act_hi_detail 
		where name_='subject' and proc_inst_id_ = id and task_id_ is null 
		order by proc_inst_id_ desc,time_ desc limit 1
        into subject;
	return subject;
END;
$$ LANGUAGE plpgsql;
