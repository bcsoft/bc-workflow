insert into bc_template (id, uid_, status_, order_, code, version_, formatted, inner_
    , path, size_, subject, desc_, type_id, file_date, author_id, modified_date, modifier_id)
select nextval('CORE_SEQUENCE'), 'historicTaskInstance.' || nextval('CORE_SEQUENCE'), 0, '1101'
  , 'HISTORIC_TASK_INSTANCE', '1', false, false, '/bs/HistoricTaskInstance.sql.ftl', 1126
  , '任务经办监控视图SQL主模板', '', (select id from bc_template_type where code='sql')
  , now(), (select id from bc_identity_actor_history where actor_code = 'admin' and current = true)
  , now(), (select id from bc_identity_actor_history where actor_code = 'admin' and current = true)
from bc_dual where not exists (select 0 from bc_template where code = 'HISTORIC_TASK_INSTANCE' and version_ = '1');

insert into bc_template (id, uid_, status_, order_, code, version_, formatted, inner_
    , path, size_, subject, desc_, type_id, file_date, author_id, modified_date, modifier_id)
select nextval('CORE_SEQUENCE'), 'historicTaskInstance.' || nextval('CORE_SEQUENCE'), 0, '1102'
  , 'HISTORIC_TASK_INSTANCE_COUNT', '1', false, false, '/bs/HistoricTaskInstance.count.sql.ftl', 718
  , '任务经办监控视图SQL计数模板', '', (select id from bc_template_type where code='sql')
  , now(), (select id from bc_identity_actor_history where actor_code = 'admin' and current = true)
  , now(), (select id from bc_identity_actor_history where actor_code = 'admin' and current = true)
from bc_dual where not exists (select 0 from bc_template where code = 'HISTORIC_TASK_INSTANCE_COUNT' and version_ = '1');