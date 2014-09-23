-- 流程关系表更改为级联删除
ALTER TABLE bc_wf_module_relation DROP CONSTRAINT if exists bcfk_wf_module_relation_pid;
ALTER TABLE bc_wf_module_relation ADD CONSTRAINT bcfk_wf_module_relation_pid FOREIGN KEY (pid) 
	REFERENCES act_hi_procinst (id_) ON UPDATE NO ACTION ON DELETE CASCADE;