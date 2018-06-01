/*
-- 1. 创建DBLINK的扩展
-- $ psql -d bcsystem -U postgres
-- postgres=# create extension dblink;

-- 2. 查看当前数据库里的扩展包
--select extname,extversion from pg_extension;
-- 3. 建立dblink连接
--select dblink_connect('bcsystem_ip222','dbname=bcsystem host=192.168.0.222 port=5432 user=bcsystem password=bcsystem');
select dblink_connect('bcsystem_ip7','dbname=bcsystem host=192.168.0.7 port=5432 user=reader password=reader');
-- 4. 使用dblink连接
select * from dblink('bcsystem_ip7','select id, code, name from bc_identity_actor limit 10') as t (id integer, CODE varchar, name varchar);
--select * from dblink('bcsystem_ip222','select id, code, name from bc_identity_actor limit 10') as t (id integer, CODE varchar, name varchar);
-- 5. 断开dblink连接
select dblink_disconnect('bcsystem_ip7');
select dblink_disconnect('bcsystem_ip222');
*/

-- 从其他数据库复制流程实例数据 
--drop function wf_copy_process_instance(varchar, varchar)
create or replace function wf_copy_process_instance(id varchar, connect_string varchar)
  returns json as
$BODY$
/**
 * 从其他数据库复制流程实例数据
 *	id - 要复制的流程实例ID
 *	connect_string - 源数据库的连接配置
 */
declare
  _dblink_name   varchar := 'wf_dblink';
  _deployment_id varchar;
  _proc_def_id   varchar;
  _sql           varchar;
  _row_count     integer;
  _r             varchar := '';

  v_state        text;
  v_msg          text;
  v_detail       text;
  v_hint         text;
  v_context text;
begin
  -- 1. 建立dblink连接
  perform dblink_connect(_dblink_name, $2);
  raise info 'create dblink %', _dblink_name;

  -- 2. 复制数据
  -- 2.1 获取 Activiti 流程部署、流程定义的ID
  _sql := 'select i.id_ proc_inst_id_, d.id_ deployment_id_, p.id_ proc_def_id_ from act_re_deployment d
				inner join act_re_procdef p on p.deployment_id_ = d.id_
				inner join act_hi_procinst i on i.proc_def_id_ = p.id_
				where i.id_ = ''' || $1 || '''';
  select deployment_id_, proc_def_id_ into _deployment_id, _proc_def_id
  from dblink(_dblink_name, _sql)
    as t(proc_inst_id_ character varying(64), deployment_id_ character varying(64), proc_def_id_ character varying(64));
  if not found
  then
    _r := '{"success": false, "msg": "deployment and definition not found"}';
    return _r :: json;
  else
    _r :=
    _r || '"deployment_id": "' || _deployment_id || '", "proc_def_id": "' || _proc_def_id || '", "proc_inst_id": "' ||
    $1 || '"';
  end if;
  raise info 'deployment_id=%, proc_def_id=%', _deployment_id, _proc_def_id;

  -- 2.2 复制 Activiti 流程部署
  insert into act_re_deployment (id_, name_, deploy_time_)
    select *
    from dblink(_dblink_name, 'select * from act_re_deployment where id_ = ''' || _deployment_id || '''')
      as t(id_ character varying(64), name_ character varying(255), deploy_time_ timestamp)
    where not exists(select 0
                     from act_re_deployment
                     where id_ = _deployment_id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_deployment": true';
    raise info 'copy_deployment %, %, %', true, _row_count, _deployment_id;
  else
    _r := _r || ', "copy_deployment": false';
    raise info 'copy_deployment %, %, %', false, _row_count, _deployment_id;
  end if;

  -- 2.3 复制 Activiti 流程部署资源
  insert into act_ge_bytearray (id_, rev_, name_, deployment_id_, bytes_, generated_)
    select *
    from dblink(_dblink_name, 'select * from act_ge_bytearray where deployment_id_ = ''' || _deployment_id || '''')
      as t(id_ character varying(64),
         rev_ integer,
         name_ character varying(255),
         deployment_id_ character varying(64),
         bytes_ bytea,
         generated_ boolean)
    where not exists(select 0
                     from act_ge_bytearray r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_ge_bytearray": true';
    raise info 'copy_act_ge_bytearray %, %, %', true, _row_count, _deployment_id;
  else
    _r := _r || ', "copy_act_ge_bytearray": false';
    raise info 'copy_act_ge_bytearray %, %, %', false, _row_count, _deployment_id;
  end if;

  -- 2.4 复制 Activiti 流程定义
  insert into act_re_procdef (id_, rev_, category_, name_, key_, version_, deployment_id_,
                              resource_name_, dgrm_resource_name_, has_start_form_key_, suspension_state_)
    select *
    from dblink(_dblink_name, 'select * from act_re_procdef where id_ = ''' || _proc_def_id || '''')
      as t(id_ character varying(64),
         rev_ integer,
         category_ character varying(255),
         name_ character varying(255),
         key_ character varying(255),
         version_ integer,
         deployment_id_ character varying(64),
         resource_name_ character varying(4000),
         dgrm_resource_name_ character varying(4000),
         has_start_form_key_ boolean,
         suspension_state_ integer)
    where not exists(select 0
                     from act_re_procdef
                     where id_ = _proc_def_id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_definition": true';
    raise info 'copy_definition %, %, %', true, _row_count, _proc_def_id;
  else
    _r := _r || ', "copy_definition": false';
    raise info 'copy_definition %, %, %', false, _row_count, _proc_def_id;
  end if;

  -- 2.5.1 复制运行时信息: act_ru_execution
  insert into act_ru_execution (id_, rev_, proc_inst_id_, business_key_, parent_id_, proc_def_id_,
                                super_exec_, act_id_, is_active_, is_concurrent_, is_scope_,
                                is_event_scope_, suspension_state_)
    select *
    from dblink(_dblink_name, 'select * from act_ru_execution where proc_inst_id_ = ''' || $1 || '''')
      as t(id_ character varying(64),
         rev_ integer,
         proc_inst_id_ character varying(64),
         business_key_ character varying(255),
         parent_id_ character varying(64),
         proc_def_id_ character varying(64),
         super_exec_ character varying(64),
         act_id_ character varying(255),
         is_active_ boolean,
         is_concurrent_ boolean,
         is_scope_ boolean,
         is_event_scope_ boolean,
         suspension_state_ integer)
    where not exists(select 0
                     from act_ru_execution r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_ru_execution": true';
    raise info 'copy_act_ru_execution %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_ru_execution": false';
    raise info 'copy_act_ru_execution %, %', false, _row_count;
  end if;

  -- 2.5.2 复制运行时信息: act_ru_task
  insert into act_ru_task (id_, rev_, execution_id_, proc_inst_id_, proc_def_id_, name_,
                           parent_task_id_, description_, task_def_key_, owner_, assignee_,
                           delegation_, priority_, create_time_, due_date_)
    select *
    from dblink(_dblink_name, 'select * from act_ru_task where proc_inst_id_ = ''' || $1 || '''')
      as t(id_ character varying(64),
         rev_ integer,
         execution_id_ character varying(64),
         proc_inst_id_ character varying(64),
         proc_def_id_ character varying(64),
         name_ character varying(255),
         parent_task_id_ character varying(64),
         description_ character varying(4000),
         task_def_key_ character varying(255),
         owner_ character varying(64),
         assignee_ character varying(64),
         delegation_ character varying(64),
         priority_ integer,
         create_time_ timestamp without time zone,
         due_date_ timestamp without time zone)
    where not exists(select 0
                     from act_ru_task r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_ru_task": true';
    raise info 'copy_act_ru_task %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_ru_task": false';
    raise info 'copy_act_ru_task %, %', false, _row_count;
  end if;

  -- 2.5.3 复制运行时信息: act_ru_identitylink
  insert into act_ru_identitylink (id_, rev_, group_id_, type_, user_id_, task_id_)
    select *
    from dblink(_dblink_name, 'select l.* from act_ru_identitylink l
				inner join act_ru_task a on a.id_ = l.task_id_ where a.proc_inst_id_ = ''' || $1 || '''')
      as t(id_ character varying(64),
         rev_ integer,
         group_id_ character varying(64),
         type_ character varying(255),
         user_id_ character varying(64),
         task_id_ character varying(64))
    where not exists(select 0
                     from act_ru_identitylink r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_ru_identitylink": true';
    raise info 'copy_act_ru_identitylink %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_ru_identitylink": false';
    raise info 'copy_act_ru_identitylink %, %', false, _row_count;
  end if;

  -- 2.5.4 复制 Activiti 流程待办任务对应大数据
  insert into act_ge_bytearray (id_, rev_, name_, deployment_id_, bytes_, generated_)
    select *
    from dblink(_dblink_name, 'select * from act_ge_bytearray where id_ in (
				SELECT bytearray_id_ FROM act_ru_variable where bytearray_id_ is not null and proc_inst_id_ = ''' || $1 ||
                              ''')')
      as t(id_ character varying(64),
         rev_ integer,
         name_ character varying(255),
         deployment_id_ character varying(64),
         bytes_ bytea,
         generated_ boolean)
    where not exists(select 0
                     from act_ge_bytearray r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_ge_bytearray": true';
    raise info 'copy_act_ge_bytearray %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_ge_bytearray": false';
    raise info 'copy_act_ge_bytearray %, %', false, _row_count;
  end if;

  -- 2.5.5 复制运行时信息: act_ru_variable
  insert into act_ru_variable (id_, rev_, type_, name_, execution_id_, proc_inst_id_, task_id_,
                               bytearray_id_, double_, long_, text_, text2_)
    select *
    from dblink(_dblink_name, 'select * from act_ru_variable where proc_inst_id_ = ''' || $1 || '''')
      as t(id_ character varying(64),
         rev_ integer,
         type_ character varying(255),
         name_ character varying(255),
         execution_id_ character varying(64),
         proc_inst_id_ character varying(64),
         task_id_ character varying(64),
         bytearray_id_ character varying(64),
         double_ double precision,
         long_ bigint,
         text_ text,
         text2_ character varying(4000))
    where not exists(select 0
                     from act_ru_variable r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_ru_variable": true';
    raise info 'copy_act_ru_variable %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_ru_variable": false';
    raise info 'copy_act_ru_variable %, %', false, _row_count;
  end if;

  -- 2.6.1 复制历史信息: act_hi_actinst
  insert into act_hi_actinst (id_, proc_def_id_, proc_inst_id_, execution_id_, act_id_, act_name_,
                              act_type_, assignee_, start_time_, end_time_, duration_)
    select *
    from dblink(_dblink_name, 'select * from act_hi_actinst where proc_inst_id_ = ''' || $1 || '''')
      as t(id_ character varying(64),
         proc_def_id_ character varying(64),
         proc_inst_id_ character varying(64),
         execution_id_ character varying(64),
         act_id_ character varying(255),
         act_name_ character varying(255),
         act_type_ character varying(255),
         assignee_ character varying(64),
         start_time_ timestamp without time zone,
         end_time_ timestamp without time zone,
         duration_ bigint)
    where not exists(select 0
                     from act_hi_actinst r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_hi_actinst": true';
    raise info 'copy_act_hi_actinst %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_hi_actinst": false';
    raise info 'copy_act_hi_actinst %, %', false, _row_count;
  end if;

  -- 2.6.2 复制历史信息: act_hi_procinst
  insert into act_hi_procinst (id_, proc_inst_id_, business_key_, proc_def_id_, start_time_,
                               end_time_, duration_, start_user_id_, start_act_id_, end_act_id_,
                               super_process_instance_id_, delete_reason_)
    select *
    from dblink(_dblink_name, 'select * from act_hi_procinst where id_ = ''' || $1 || '''')
      as t(id_ character varying(64),
         proc_inst_id_ character varying(64),
         business_key_ character varying(255),
         proc_def_id_ character varying(64),
         start_time_ timestamp without time zone,
         end_time_ timestamp without time zone,
         duration_ bigint,
         start_user_id_ character varying(255),
         start_act_id_ character varying(255),
         end_act_id_ character varying(255),
         super_process_instance_id_ character varying(64),
         delete_reason_ character varying(4000))
    where not exists(select 0
                     from act_hi_procinst r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_hi_procinst": true';
    raise info 'copy_act_hi_procinst %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_hi_procinst": false';
    raise info 'copy_act_hi_procinst %, %', false, _row_count;
  end if;

  -- 2.6.3 复制历史信息: act_hi_taskinst
  insert into act_hi_taskinst (id_, proc_def_id_, task_def_key_, proc_inst_id_, execution_id_,
                               name_, parent_task_id_, description_, owner_, assignee_, start_time_,
                               end_time_, duration_, delete_reason_, priority_, due_date_)
    select *
    from dblink(_dblink_name, 'select * from act_hi_taskinst where proc_inst_id_ = ''' || $1 || '''')
      as t(id_ character varying(64),
         proc_def_id_ character varying(64),
         task_def_key_ character varying(255),
         proc_inst_id_ character varying(64),
         execution_id_ character varying(64),
         name_ character varying(255),
         parent_task_id_ character varying(64),
         description_ character varying(4000),
         owner_ character varying(64),
         assignee_ character varying(64),
         start_time_ timestamp without time zone,
         end_time_ timestamp without time zone,
         duration_ bigint,
         delete_reason_ character varying(4000),
         priority_ integer,
         due_date_ timestamp without time zone)
    where not exists(select 0
                     from act_hi_taskinst r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_hi_taskinst": true';
    raise info 'copy_act_hi_taskinst %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_hi_taskinst": false';
    raise info 'copy_act_hi_taskinst %, %', false, _row_count;
  end if;

  -- 2.6.4 复制 Activiti 流程经办任务对应大数据
  insert into act_ge_bytearray (id_, rev_, name_, deployment_id_, bytes_, generated_)
    select *
    from dblink(_dblink_name, 'select * from act_ge_bytearray where id_ in (
				SELECT bytearray_id_ FROM act_hi_detail where bytearray_id_ is not null and proc_inst_id_ = ''' || $1 || ''')')
      as t(id_ character varying(64),
         rev_ integer,
         name_ character varying(255),
         deployment_id_ character varying(64),
         bytes_ bytea,
         generated_ boolean)
    where not exists(select 0
                     from act_ge_bytearray r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_ge_bytearray": true';
    raise info 'copy_act_ge_bytearray %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_ge_bytearray": false';
    raise info 'copy_act_ge_bytearray %, %', false, _row_count;
  end if;

  -- 2.6.5 复制历史信息: act_hi_detail
  insert into act_hi_detail (id_, type_, proc_inst_id_, execution_id_, task_id_, act_inst_id_,
                             name_, var_type_, rev_, time_, bytearray_id_, double_, long_, text_, text2_)
    select *
    from dblink(_dblink_name, 'select * from act_hi_detail where proc_inst_id_ = ''' || $1 || '''')
      as t(id_ character varying(64),
         type_ character varying(255),
         proc_inst_id_ character varying(64),
         execution_id_ character varying(64),
         task_id_ character varying(64),
         act_inst_id_ character varying(64),
         name_ character varying(255),
         var_type_ character varying(64),
         rev_ integer,
         time_ timestamp without time zone,
         bytearray_id_ character varying(64),
         double_ double precision,
         long_ bigint,
         text_ text,
         text2_ character varying(4000))
    where not exists(select 0
                     from act_hi_detail r
                     where r.id_ = t.id_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_act_hi_detail": true';
    raise info 'copy_act_hi_detail %, %', true, _row_count;
  else
    _r := _r || ', "copy_act_hi_detail": false';
    raise info 'copy_act_hi_detail %, %', false, _row_count;
  end if;

  -- 2.7.1 复制BC信息: bc_identity_actor_detail
  insert into bc_identity_actor_detail (id, create_date, work_date, iso, sex, card, duty_id, comment_)
    select *
    from dblink(_dblink_name, 'select *
                               from bc_identity_actor_detail')
      as t(id integer,
         create_date timestamp without time zone,
         work_date timestamp without time zone,
         iso boolean,
         sex integer,
         card character varying(20),
         duty_id integer,
         comment_ character varying(4000))
    where not exists(select 0
                     from bc_identity_actor_detail r
                     where r.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_identity_actor_detail": true';
    raise info 'copy_bc_identity_actor_detail %, %', true, _row_count;
  else
    _r := _r || ', "copy_bc_identity_actor_detail": false';
    raise info 'copy_bc_identity_actor_detail %, %', false, _row_count;
  end if;

  -- 2.7.2 复制BC信息: bc_identity_actor
  insert into bc_identity_actor (id, uid_, type_, status_, inner_, code, name, py, order_, email,
                                 phone, detail_id, pcode, pname)
    select *
    from dblink(_dblink_name, 'select *
                               from bc_identity_actor')
      as t(id integer,
         uid_ character varying(36),
         type_ integer,
         status_ integer,
         inner_ boolean,
         code character varying(100),
         name character varying(255),
         py character varying(255),
         order_ character varying(100),
         email character varying(255),
         phone character varying(255),
         detail_id integer,
         pcode character varying(4000),
         pname character varying(4000))
    where not exists(select 0
                     from bc_identity_actor r
                     where r.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_identity_actor": true';
    raise info 'copy_bc_identity_actor %, %', true, _row_count;
  else
    _r := _r || ', "copy_bc_identity_actor": false';
    raise info 'copy_bc_identity_actor %, %', false, _row_count;
  end if;

  -- 2.7.3 复制BC信息: bc_identity_actor_history
  insert into bc_identity_actor_history (id, pid, current, rank, create_date, start_date, end_date, actor_type,
                                         actor_id, actor_name, upper_id, upper_name, unit_id, unit_name, pcode, pname, actor_code)
    select *
    from dblink(_dblink_name, 'select *
                               from bc_identity_actor_history')
      as t(id integer,
         pid integer,
         current boolean,
         rank integer,
         create_date timestamp without time zone,
         start_date timestamp without time zone,
         end_date timestamp without time zone,
         actor_type integer,
         actor_id integer,
         actor_name character varying(100),
         upper_id integer,
         upper_name character varying(255),
         unit_id integer,
         unit_name character varying(255),
         pcode character varying(4000),
         pname character varying(4000),
         actor_code character varying(255))
    where not exists(select 0
                     from bc_identity_actor_history r
                     where r.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_identity_actor_history": true';
    raise info 'copy_bc_identity_actor_history %, %', true, _row_count;
  else
    _r := _r || ', "copy_bc_identity_actor_history": false';
    raise info 'copy_bc_identity_actor_history %, %', false, _row_count;
  end if;

  -- 2.7.4 复制BC信息: bc_identity_auth
  insert into bc_identity_auth (id, password)
    select *
    from dblink(_dblink_name, 'select *
                               from bc_identity_auth')
      as t(id integer, password character varying(32))
    where not exists(select 0
                     from bc_identity_auth r
                     where r.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_identity_auth": true';
    raise info 'copy_bc_identity_auth %, %', true, _row_count;
  else
    _r := _r || ', "copy_bc_identity_auth": false';
    raise info 'copy_bc_identity_auth %, %', false, _row_count;
  end if;

  -- 2.7.5 复制BC信息: bc_identity_actor_relation
  insert into bc_identity_actor_relation (type_, master_id, follower_id, order_)
    select *
    from dblink(_dblink_name, 'select *
                               from bc_identity_actor_relation')
      as t(type_ integer,
         master_id integer,
         follower_id integer,
         order_ character varying(100))
    where not exists(select 0
                     from bc_identity_actor_relation r
                     where r.master_id = t.master_id and r.follower_id = t.follower_id and r.type_ = t.type_);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_identity_actor_relation": true';
    raise info 'copy_bc_identity_actor_relation %, %', true, _row_count;
  else
    _r := _r || ', "copy_bc_identity_actor_relation": false';
    raise info 'copy_bc_identity_actor_relation %, %', false, _row_count;
  end if;

  -- 2.8.1 复制BC信息: bc_wf_deploy
  insert into bc_wf_deploy (id, uid_, deployment_id, order_, status_, type_, category, code,
                            version_, subject, path, size_, desc_, source, deployer_id, deploy_date,
                            file_date, author_id, modifier_id, modified_date)
    select *
    from dblink(_dblink_name, 'select * from bc_wf_deploy where deployment_id = ''' || _deployment_id || '''')
      as t(id integer,
         uid_ character varying(36),
         deployment_id character varying(64),
         order_ character varying(255),
         status_ integer,
         type_ integer,
         category character varying(255),
         code character varying(255),
         version_ character varying(255),
         subject character varying(255),
         path character varying(255),
         size_ integer,
         desc_ character varying(4000),
         source character varying(255),
         deployer_id integer,
         deploy_date timestamp without time zone,
         file_date timestamp without time zone,
         author_id integer,
         modifier_id integer,
         modified_date timestamp without time zone)
    where not exists(select 0
                     from bc_wf_deploy r
                     where r.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_wf_deploy": true';
    raise info 'copy_bc_wf_deploy %, %, %', true, _row_count, _deployment_id;
  else
    _r := _r || ', "copy_bc_wf_deploy": false';
    raise info 'copy_bc_wf_deploy %, %, %', false, _row_count, _deployment_id;
  end if;

  -- 2.8.2 复制BC信息: bc_template_type
  insert into bc_template_type (id, status_, order_, code, name, is_path, is_pure_text, ext,
                                desc_, file_date, author_id, modifier_id, modified_date)
    select *
    from dblink(_dblink_name, 'select *
                               from bc_template_type')
      as t(id integer,
         status_ integer,
         order_ character varying(255),
         code character varying(255),
         name character varying(255),
         is_path boolean,
         is_pure_text boolean,
         ext character varying(255),
         desc_ character varying(4000),
         file_date timestamp without time zone,
         author_id integer,
         modifier_id integer,
         modified_date timestamp without time zone)
    where not exists(select 0
                     from bc_template_type r
                     where r.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_template_type": true';
    raise info 'copy_bc_template_type %, %', true, _row_count;
  else
    _r := _r || ', "copy_bc_template_type": false';
    raise info 'copy_bc_template_type %, %', false, _row_count;
  end if;

  -- 2.8.3 复制BC信息: bc_wf_deploy_resource
  insert into bc_wf_deploy_resource (id, uid_, pid, code, subject, path, size_, source, type_id, formatted)
    select *
    from dblink(_dblink_name, 'select s.* from bc_wf_deploy_resource s
				inner join bc_wf_deploy o on o.id = s.pid where o.deployment_id = ''' || _deployment_id || '''')
      as t(id integer,
         uid_ character varying(72),
         pid integer,
         code character varying(255),
         subject character varying(255),
         path character varying(255),
         size_ integer,
         source character varying(255),
         type_id integer,
         formatted boolean)
    where not exists(select 0
                     from bc_wf_deploy_resource r
                     where r.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_wf_deploy_resource": true';
    raise info 'copy_bc_wf_deploy_resource %, %, %', true, _row_count, _deployment_id;
  else
    _r := _r || ', "copy_bc_wf_deploy_resource": false';
    raise info 'copy_bc_wf_deploy_resource %, %, %', false, _row_count, _deployment_id;
  end if;

  -- 2.8.4 复制BC信息: bc_wf_deploy_resource_param
  insert into bc_wf_deploy_resource_param (rid, pid)
    select *
    from dblink(_dblink_name, 'select p.* from bc_wf_deploy_resource_param p
				inner join bc_wf_deploy_resource r on r.id = p.rid 
				inner join bc_wf_deploy d on d.id = r.pid where d.deployment_id = ''' || _deployment_id || '''')
      as t(rid integer, pid integer)
    where not exists(select 0
                     from bc_wf_deploy_resource_param p
                     where p.rid = t.rid and p.pid = t.pid);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_wf_deploy_resource_param": true';
    raise info 'copy_bc_wf_deploy_resource_param %, %, %', true, _row_count, _deployment_id;
  else
    _r := _r || ', "copy_bc_wf_deploy_resource_param": false';
    raise info 'copy_bc_wf_deploy_resource_param %, %, %', false, _row_count, _deployment_id;
  end if;

  -- 2.8.5 复制BC信息: bc_wf_excution_log
  insert into bc_wf_excution_log (id, type_, listenter, eid, pid, tid, ecode, ename, formkey, desc_,
                                  author_id, author_code, author_name, assignee_id, assignee_code,
                                  assignee_name, file_date)
    select *
    from dblink(_dblink_name, 'select * from bc_wf_excution_log where pid = ''' || $1 || '''')
      as t(id integer,
         type_ character varying(255),
         listenter character varying(255),
         eid character varying(255),
         pid character varying(255),
         tid character varying(255),
         ecode character varying(255),
         ename character varying(255),
         formkey character varying(1000),
         desc_ character varying(1000),
         author_id integer,
         author_code character varying(255),
         author_name character varying(255),
         assignee_id integer,
         assignee_code character varying(255),
         assignee_name character varying(255),
         file_date timestamp without time zone)
    where not exists(select 0
                     from bc_wf_excution_log l
                     where l.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_wf_excution_log": true';
    raise info 'copy_bc_wf_excution_log %, %, %', true, _row_count, $1;
  else
    _r := _r || ', "copy_bc_wf_excution_log": false';
    raise info 'copy_bc_wf_excution_log %, %, %', false, _row_count, $1;
  end if;

  -- 2.8.6 复制BC信息: bc_wf_attach
  insert into bc_wf_attach (id, uid_, tid, pid, type_, common, subject, path_, ext, size_,
                            desc_, formatted, file_date, author_id, modified_date, modifier_id)
    select *
    from dblink(_dblink_name, 'select * from bc_wf_attach where pid = ''' || $1 || '''')
      as t(id integer,
         uid_ character varying(36),
         tid character varying(255),
         pid character varying(255),
         type_ integer,
         common boolean,
         subject character varying(255),
         path_ character varying(255),
         ext character varying(255),
         size_ integer,
         desc_ character varying(4000),
         formatted boolean,
         file_date timestamp without time zone,
         author_id integer,
         modified_date timestamp without time zone,
         modifier_id integer)
    where not exists(select 0
                     from bc_wf_attach a
                     where a.id = t.id);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_wf_attach": true';
    raise info 'copy_bc_wf_attach %, %', true, _row_count;
  else
    _r := _r || ', "copy_bc_wf_attach": false';
    raise info 'copy_bc_wf_attach %, %', false, _row_count;
  end if;

  -- 2.8.7 复制BC信息: bc_wf_attach_param
  insert into bc_wf_attach_param (aid, pid)
    select *
    from dblink(_dblink_name, 'select p.* from bc_wf_attach_param p
				inner join bc_wf_attach a on a.id = p.aid where a.pid = ''' || $1 || '''')
      as t(aid integer, pid integer)
    where not exists(select 0
                     from bc_wf_attach_param p
                     where p.aid = t.aid and p.pid = t.pid);
  get diagnostics _row_count = row_count;
  --raise info 'row_count=%', _row_count;
  if found
  then
    _r := _r || ', "copy_bc_wf_attach_param": true';
    raise info 'copy_bc_wf_attach_param %, %', true, _row_count;
  else
    _r := _r || ', "copy_bc_wf_attach_param": false';
    raise info 'copy_bc_wf_attach_param %, %', false, _row_count;
  end if;

  -- 3. 断开dblink连接
  perform dblink_disconnect(_dblink_name);
  raise info 'disconnect dblink %', _dblink_name;

  -- 返回结果
  _r := '{' || _r || ', "success": true' || '}';
  return _r :: json;

  exception when others
  then
    -- http://www.postgresql.org/docs/9.3/static/plpgsql-control-structures.html#PLPGSQL-EXCEPTION-DIAGNOSTICS
    get stacked diagnostics
    v_state = returned_sqlstate,
    v_msg = message_text,
    v_detail = pg_exception_detail,
    v_hint = pg_exception_hint,
    v_context = pg_exception_context;
    perform dblink_disconnect(_dblink_name);
    --raise EXCEPTION '% %', SQLERRM, SQLSTATE;
    raise exception E'%
  SQL状态: %
  Context: %
  Hint   : %
  Detail : %', v_msg, v_state, v_context, v_detail, v_hint;

end;
$BODY$ language plpgsql;

/*
-- 输入流程实例ID和数据库连接信息
select wf_copy_process_instance('3887597', 'dbname=bcsystem host=192.168.0.7 port=5432 user=reader password=reader');
*/
