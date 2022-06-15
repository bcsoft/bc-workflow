with t(task_id, assignee_code) as (
  select t.id_, t.assignee_ from act_hi_taskinst t
  inner join act_hi_procinst p on p.proc_inst_id_ = t.proc_inst_id_
  inner join bc_wf_procinst_info pi on pi.id = p.id_
  inner join act_re_procdef pd on pd.id_ = t.proc_def_id_
  left join act_ru_execution e on e.proc_inst_id_ = t.proc_inst_id_
  left join bc_identity_actor a on a.code = t.assignee_
  <#if condition??>${condition}</#if>
)
<#if extra_condition??>
select count(*) - 1
from (
  select ''
  <#if ownActors_qc??>
    union select t.task_id from t where ${ownActors_qc}
  </#if>
  <#if deploy_qc??>
    union select t.task_id from t where ${deploy_qc}
  </#if>
  <#if pi_qc??>
    union select t.task_id from t where ${pi_qc}
  </#if>
) p;
<#else>
select count(*) from t;
</#if>