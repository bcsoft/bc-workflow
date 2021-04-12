-- drop function wf__get_task_subject(varchar, varchar);
create or replace function wf__get_task_subject(process_id varchar, task_id varchar)
  returns text language plpgsql as $$
/** 获取任务本地 subject 变量的值
 *  @param process_id 流程实例的 ID
 *  @param task_id 任务的 ID
 *  @return 任务本地 subject 变量的值，如果没有此本地变量，返回 null
 */
declare
  subject text;
begin
  select text_ into subject from act_hi_detail
  where name_ = 'subject' and proc_inst_id_ = $1 and task_id_ = $2
  order by proc_inst_id_ desc, time_ desc limit 1;
  return subject;
end;
$$;

/* test
-- 内部事务流程，传递事务环节没有本地 subject，办理事务环节有本地 subject
select time_, rev_, name_, text_, task_id_, * from act_hi_detail where proc_inst_id_ = '18821695'
   --and task_id_ = ''
   --and name_ = 'subject'
   order by time_ desc;
-- 办理事务（计划财务部）
select wf__get_task_subject(process_id := '18821695', task_id := '18821874');
-- null
select wf__get_task_subject(process_id := '18821695', task_id := '18821816');

-- 内部事务流程，传递事务环节没有本地 subject，办理事务环节有本地 subject
select time_, rev_, name_, text_, task_id_, * from act_hi_detail where proc_inst_id_ = '18582384'
   --and task_id_ = ''
   --and name_ = 'subject'
   order by time_ desc;
-- 车队长调整计划
select wf__get_task_subject(process_id := '18582384', task_id := '18593838');
-- null
select wf__get_task_subject(process_id := '18582384', task_id := '18582985');
*/