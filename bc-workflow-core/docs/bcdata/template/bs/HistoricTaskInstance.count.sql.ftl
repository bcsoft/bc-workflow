select count(*)
from act_hi_taskinst t
inner join act_hi_procinst p on p.proc_inst_id_ = t.proc_inst_id_
inner join bc_wf_procinst_info pi on pi.id = p.id_
inner join act_re_procdef pd on pd.id_ = t.proc_def_id_
left join act_ru_execution e on e.proc_inst_id_ = t.proc_inst_id_
left join bc_identity_actor a on a.code = t.assignee_
-- 条件
<#if condition??>${condition}</#if>