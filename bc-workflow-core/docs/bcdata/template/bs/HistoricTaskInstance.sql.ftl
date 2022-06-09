select t.id_ task_id, t.name_ task_name
  , case
     when t.end_time_ is not null then 3
     else (case when (pd.suspension_state_ = 2 or e.suspension_state_ = 2) then 2 else 1 end)
  end as task_status
  , t.start_time_ start_time, t.end_time_ end_time, t.duration_ duration, t.due_date_ as due_date
  , t.proc_inst_id_ process_id, pd.name_ process_name, a.name as assignee
  , e.suspension_state_ pstatus, t.task_def_key_, pd.key_ process_key
  , pi.info->>'wf_code' as wf_code, pi.info->>'subject' as wf_subject, pd.deployment_id_
  , 'ProcessInstance' as access_control_doc_type
  , (case when pi.info->>'subject' != null && length(pi.info->>'subject') > 0 then pi.info->>'subject' else pd.name_ end) as access_control_doc_name
from act_hi_taskinst t
inner join act_hi_procinst p on p.proc_inst_id_ = t.proc_inst_id_
inner join bc_wf_procinst_info pi on pi.id = p.id_
inner join act_re_procdef pd on pd.id_ = t.proc_def_id_
left join act_ru_execution e on e.proc_inst_id_ = t.proc_inst_id_
left join bc_identity_actor a on a.code = t.assignee_
-- 条件
<#if condition??>${condition}</#if>
-- 分页
<#if limit??>limit ${limit}</#if>
<#if offset??>offset ${offset?c}</#if>