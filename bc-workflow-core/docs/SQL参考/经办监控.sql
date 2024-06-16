/**
经办监控：
  1. 有部门监控权限可以看到所属部门下所有人的经办
  2. 有流程定义的监控权限可以看到此类流程的所有经办
  3. 有流程实例监控权限可以看到此流程实例的所有经办
*/

-- 要查询的用户
with transactor(id, code, name) as (
  select id, code, name from bc_identity_actor where code = 'hrj'
) --select * from transactor;
-- 用户可以监控的流程部署
, monitor_pd(id, name, role) as (
  select ad.doc_id::int as id, ad.doc_name as name, aa.role
  from bc_acl_doc ad
  inner join bc_acl_actor aa on aa.pid = ad.id
  inner join transactor u on u.id = aa.aid
  where ad.doc_type = 'Deploy'
  and (right(aa.role, 1) = '1' or left(right(aa.role, 2), 1) = '1')
) --select * from monitor_pd;
-- 用户可以监控的流程实例
, monitor_pi(id, name, role) as (
  select ad.doc_id as id, ad.doc_name as name, aa.role
  from bc_acl_doc ad
  inner join bc_acl_actor aa on aa.pid = ad.id
  inner join transactor u on u.id = aa.aid
  where ad.doc_type = 'ProcessInstance'
  and (right(aa.role, 1) = '1' or left(right(aa.role, 2), 1) = '1')
) --select * from monitor_pi;
-- 经办任务任务：编码、名称、发起时间、完成时间、耗时、办理期限
select t.id_ as id, t.task_def_key_ as key, t.name_ as name, t.start_time_ as start_time
  , t.end_time_ as end_time, t.duration_ as duration, t.due_date_ as deadline
  -- 办理人
  , u.code as transactor_code, u.name as transactor_name
  -- 流程实例：状态(1流传中2已暂停3已结束)、流水号、标题
  , t.proc_inst_id_ as pi_id, (case when e.id_ is null then 3 else e.suspension_state_ end) as pi_status
  , bc_pi.info->>'wf_code' as pi_sn, bc_pi.info->>'subject' as pi_subject
  -- 流程部署：ID、Key、名称
  , bc_pd.id as pd_id, pd.key_ as pd_key, pd.name_ as pd_name
from act_hi_taskinst t
inner join bc_wf_procinst_info bc_pi on bc_pi.id = t.proc_inst_id_
inner join act_re_procdef pd on pd.id_ = t.proc_def_id_
inner join bc_wf_deploy bc_pd on bc_pd.deployment_id = pd.deployment_id_
inner join bc_identity_actor u on u.code = t.assignee_
left join act_ru_execution e on e.proc_inst_id_ = t.proc_inst_id_
-- 经办
where t.end_time_ is not null and e.parent_id_ is null -- 806245 条
-- 排除自己的经办 805651 条
and t.assignee_ != (select code from transactor)
-- 流程状态正常
--and (pd.suspension_state_ = 2 or e.suspension_state_ = 2)
and (
  -- 有流程部署的监控权限可以看到此类流程的所有经办
  --exists (select 0 from monitor_pd d where d.id = bc_pd.id)
  bc_pd.id in (select id from monitor_pd)
  -- 有流程实例监控权限可以看到此流程实例的所有经办
  --or (exists (select 0 from monitor_pi i where i.id = pi.id_))
  or t.proc_inst_id_ in (select id from monitor_pi)
  -- 有部门监控权限并且是部门领导岗位内的人，可以看到该领导岗位所属部门下所有人的经办
  --or t.assignee_ in (select code from monitor_department_user)
)
order by t.start_time_ desc
--limit 10
;

/*
select *, 'ProcessInstance' as access_control_doc_type
  , (case when wf_subject is null || wf_subject = '' then process_name else wf_subject end) as access_control_doc_name
from t
  <#if extra_condition??>where ${extra_condition}</#if>
-- 分页
  <#if limit??>limit ${limit}</#if>
  <#if offset??>offset ${offset?c}</#if>

select * from bc_wf_deploy where id in(49420,98881);
where exists(select 1 from act_hi_taskinst dc_t inner join act_re_procdef dc_r on dc_r.id_=dc_t.proc_def_id_ inner join bc_wf_deploy dc_d on dc_d.deployment_id=dc_r.deployment_id_ where dc_t.id_=t.task_id and dc_d.id in(49420,98881))

select * from act_hi_procinst where id_ != proc_inst_id_ or proc_inst_id_ is null;
*/