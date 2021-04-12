-- drop function wf_get_last_process_info(integer, varchar);
create or replace function wf_get_last_process_info(mid integer, mtype varchar)
  returns json language plpgsql as $$
/** 根据模块信息获取流程信息
 *  @param mid 模块Id
 *  @param mtype 模块类型
 *  @return json 格式为：
 *  {
 *    id: "流程实例ID",
 *    name: "流程名称",
 *    status: "流程状态：1-流转中、2-暂停、3-已结束",
 *    start_time: "流程发起时间",
 *    end_time: "流程结束时间",
 *    todo_tasks: [ // 待办列表
 *      {
 *        id: "任务ID"，
 *        name: "任务名称"，
 *        actor_code: "待办者账号"，
 *        actor_name: "待办者名称"
 *      },
 *      ...
 *    ]
 *  }
 */
declare
  --定义变量
  hi_inst json;
begin
  select row_to_json(act_hi_inst) into hi_inst from (
    select a.proc_inst_id_ as id, b.name_ as name
      -- 状态：1-流转中、2-已暂停、3-已结束
      -- f.suspension_state_: 1-流转中、2-暂停、null - 流转结束
      , (case a.end_time_ is not null when true then 3 else f.suspension_state_ end) as status
      , a.start_time_ as start_time, a.end_time_ as end_time
      -- 查找待办列表
      , to_json(
      ( -- 合并为 json 数组
        select array_agg(row_to_json(c)) from (
          select art.id_ as id, art.name_ as name
            -- 如果待办人CODE不为空则返回待办人CODE，否则返回待办岗位CODE
            , coalesce(art.assignee_, i.group_id_) as actor_code
            -- 如果待办人不为空则返回待办人，否则返回待办岗位
            , coalesce(ia.name, ai.name) as actor_name
          from act_ru_task art
          left join bc_identity_actor ia on ia.code = art.assignee_ -- 关联待办人表 ia.name
          left join act_ru_identitylink i on i.task_id_=art.id_
          left join bc_identity_actor ai on ai.code=i.group_id_ -- 关联待办岗位表 ai.name
          where art.proc_inst_id_ = a.proc_inst_id_
        ) c
      )) as todo_tasks
    from act_hi_procinst a
    inner join act_re_procdef b on b.id_=a.proc_def_id_
    inner join bc_wf_module_relation mr on mr.pid = a.proc_inst_id_
    left join act_ru_execution f on a.id_ = f.proc_inst_id_
    where mr.mid = $1 and mr.mtype = $2
    order by a.start_time_ desc
    limit 1
  ) act_hi_inst;
  return hi_inst;
end;
$$;

/* test
select * from bc_wf_module_relation where pid = '17909370';
select wf_get_last_process_info(61089060, 'Case4InfractTraffic');
*/