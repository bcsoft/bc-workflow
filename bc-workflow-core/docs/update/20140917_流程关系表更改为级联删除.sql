-- 流程关系表更改为级联删除
alter table bc_wf_module_relation
  drop constraint if exists bcfk_wf_module_relation_pid;
alter table bc_wf_module_relation
  add constraint bcfk_wf_module_relation_pid foreign key (pid)
references act_hi_procinst (id_) on update no action on delete cascade;