/*
工作空间性能优化
*/

-- 新建执行日志的 pid 索引
--CREATE INDEX bcwfidx_excution_log_pid_type ON bc_wf_excution_log (pid, type_);

-- drop function wf__find_process_instance_detail(varchar)
create or replace function wf__find_process_instance_detail(id varchar)
  returns json as
$BODY$
/** 查询指定流程实例的详细信息(包含流程定义、部署、待办、经办等信息)
 *	@param id 流程实例ID
 *	@return 流程实例的详细信息(ACT_HI_PROCINST)
 *	返回的 json 格式为：{
 *		id: 【varchar 实例ID】,
 *		pid: 【varchar 所属主流程实例ID】,
 *		status: 【integer 状态：0-流转中、1-已结束、2-暂停】,
 *		start_time: 【发起时间】,
 *		end_time: 【结束时间】,
 *		duration: 【integer 耗时(毫秒)】,
 *		delete_reason: 【-varchar 删除原因】,
 *		subject: 【varchar 流程标题】,
 *		code: 【varchar 流水号】,
 *		// 发起人
 *		start_user: {
 *		  id: 【integer ID】,
 *		  type: 【integer 类型 1-单位,2-部门,3-岗位,4-用户】,
 *		  code: 【varchar 账号】,
 *		  name: 【varchar 姓名】
 *		},
 *
 *		// 流程定义信息(ACT_RE_PROCDEF)
 *		definition: {
 *		  id: 【varchar ID】,
 *		  key: 【varchar 编号】,
 *		  name: 【varchar 名称】,
 *		  version: 【integer 版本号(新增为1,重新发布后依次加1)】,
 *		  deployment_id: 【varchar 部署ID】
 *		},
 *
 *		// 流程部署信息(BC_WF_DEPLOY)
 *		deploy: {
 *		  id: 【integer ID】,
 *		  key: 【varchar 编号】,
 *		  name: 【varchar 名称】,
 *		  version: 【varchar 版本号】,
 *		  deployment_id: 【varchar 部署ID】
 *		},
 *
 *		// 全局流程变量 (变量的最新值，不含历史版本的值) (ACT_HI_DETAIL)
 *		variables: [
 *		  {
 *		    name: 【varchar 名称】,
 *		    type: 【varchar 值类型 boolean|string|integer|long|double|date|serializable|null】,
 *		    value: 【integer 值 boolean|text|number|date|array|json】
 *		  },
 *		  ......
 *		],
 *
 *		// 全局流程附件、意见 (BC_WF_ATTACH)
 *		attachs: [
 *		  {
 *		    id: 【integer】,
 *		    type: 【integer 类型：1-附件，2-意见，3-临时附件(将作为子流程的附件)】,
 *		    pid: 【varchar 流程实例ID】,
 *		    tid: 【varchar 任务实例ID】,
 *		    subject: 【varchar 标题】,
 *		    ext: 【varchar 附件扩展名】,
 *		    size: 【integer 附件大小】,
 *		    path: 【varchar 附件相对路径】,
 *		    description: 【varchar 意见内容】,
 *		    file_date: 【varchar 创建时间】,
 *		    author: {id|code|name}
 *		  },
 *		  ......
 *		],
 *
 *		bytearray_ids: 【[varchar] 流程变量为 serializable 类型对应的 bytearray_id_ 收集，应用需要另行通过表 act_ge_bytearray 获取对应的数据】,
 *
 *		// 经办信息
 *		done_tasks:[
 *		  {
 *		    id: 【varchar ID】,
 *		    pid: 【varchar 父任务ID】,
 *		    key: 【varchar 编号】,
 *		    name: 【varchar 名称】,
 *		    start_time: 【创建时间】,
 *		    end_time: 【完成时间】,
 *		    duration: 【integer 耗时(毫秒)】,
 *		    due_date: 【到期日期】,
 *		    priority: 【integer 优先级】,
 *		    priority: 【integer 优先级】,
 *		    description: 【varchar 描述】,
 *		    // 办理人
 *		    actor: {
 *		      id: 【integer ID】,
 *		      type: 【integer 类型 1-单位,2-部门,3-岗位,4-用户】,
 *		      code: 【varchar 账号】,
 *		      name: 【varchar 姓名】
 *		    },
 *		    // 本地变量（变量的最新值，不含历史版本的值）
 *		    variables: [
 *		      {
 *		        id: 【varchar ID】,
 *		        name: 【varchar 名称】,
 *		        type: 【varchar 值类型 boolean|string|integer|long|double|date|serializable|null】,
 *		        value: 【integer 值 boolean|text|number|date|array|json】
 *		      },
 *		      ......
 *		    ],
 *		    claim_time: 【varchar 岗位任务的签领时间】,
 *		    // 执行委托操作的用户
 *		    master: {
 *		      id: 【integer ID】,
 *		      code: 【varchar 账号】,
 *		      name: 【varchar 姓名】
 *		    },
 *		    // 执行委托前的原始用户
 *		    origin_actor: {
 *		      id: 【integer ID】,
 *		      code: 【varchar 账号】,
 *		      name: 【varchar 姓名】
 *		    },
 *
 *		    // 附件、意见 (BC_WF_ATTACH)
 *		    attachs: [
 *		      {
 *		        id: 【integer】,
 *		        type: 【integer 类型：1-附件，2-意见，3-临时附件(将作为子流程的附件)】,
 *		        pid: 【varchar 流程实例ID】,
 *		        tid: 【varchar 任务实例ID】,
 *		        subject: 【varchar 标题】,
 *		        ext: 【varchar 附件扩展名】,
 *		        size: 【integer 附件大小】,
 *		        path: 【varchar 附件相对路径】,
 *		        description: 【varchar 意见内容】,
 *		        file_date: 【varchar 创建时间】,
 *		        author: {id|code|name}
 *		      },
 *		      ......
 *		    ]
 *		  },
 *		  ......
 *		],
 *
 *		// 待办信息
 *		todo_tasks:[
 *		  {
 *		    id: 【varchar ID】,
 *		    pid: 【varchar 父任务ID】,
 *		    key: 【varchar 编号】,
 *		    name: 【varchar 名称】,
 *		    start_time: 【创建时间】,
 *		    due_date: 【到期日期】,
 *		    priority: 【integer 优先级】,
 *		    description: 【varchar 描述】,
 *		    // 办理人
 *		    actor: {
 *		      id: 【integer ID】,
 *		      type: 【integer 类型 1-单位,2-部门,3-岗位,4-用户】,
 *		      code: 【varchar 账号】,
 *		      name: 【varchar 姓名】
 *		    },
 *		    // 本地变量（变量的最新值，不含历史版本的值）
 *		    variables: [
 *		      {
 *		        id: 【varchar ID】,
 *		        name: 【varchar 名称】,
 *		        type: 【varchar 值类型 boolean|string|integer|long|double|date|serializable|null】,
 *		        value: 【integer 值 boolean|text|number|date|array|json】
 *		      },
 *		      ......
 *		    ],
 *		    claim_time: 【varchar 岗位任务的签领时间】,
 *		    // 执行委托操作的用户
 *		    master: {
 *		      id: 【integer ID】,
 *		      code: 【varchar 账号】,
 *		      name: 【varchar 姓名】
 *		    },
 *		    // 执行委托前的原始用户
 *		    origin_actor: {
 *		      id: 【integer ID】,
 *		      code: 【varchar 账号】,
 *		      name: 【varchar 姓名】
 *		    },
 *
 *		    // 附件、意见 (BC_WF_ATTACH)
 *		    attachs: [
 *		      {
 *		        id: 【integer】,
 *		        type: 【integer 类型：1-附件，2-意见，3-临时附件(将作为子流程的附件)】,
 *		        pid: 【varchar 流程实例ID】,
 *		        tid: 【varchar 任务实例ID】,
 *		        subject: 【varchar 标题】,
 *		        ext: 【varchar 附件扩展名】,
 *		        size: 【integer 附件大小】,
 *		        path: 【varchar 附件相对路径】,
 *		        description: 【varchar 意见内容】,
 *		        file_date: 【varchar 创建时间】,
 *		        author: {id|code|name}
 *		      },
 *		      ......
 *		    ]
 *		  },
 *		  ......
 *		]
 *	}
 */
declare
  r json;
begin
  -- 流程实例信息
  with instance(id, pid, duration, start_time, end_time, status, delete_reason, start_user, definition, deploy) as (
    select i.id_                                           id, i.super_process_instance_id_ pid, i.duration_ duration,
           to_char(i.start_time_, 'YYYY-MM-DD HH24:MI:SS') start_time,
           to_char(i.end_time_, 'YYYY-MM-DD HH24:MI:SS')   end_time -- 状态：1-流转中、2-暂停、3-已结束
      ,
           (case i.end_time_ is not null
            when true
              then 3 -- 已结束
            else (
              case e.id_ is not null when true
                then 2 -- 已暂停
              else 1 -- 流转中
              end
            )
            end
           ) as                                            status,
           delete_reason_                                  delete_reason
      -- 流程发起人
      ,
           (select row_to_json(t)
            from (select a.id, a.code, a.name, a.pname
                  from bc_identity_actor a
                  where a.code = i.start_user_id_) t)      start_user
      -- 流程定义
      ,
           (select row_to_json(t)
            from (
                   select d.id_ id, d.key_ as key, d.name_ as name, d.version_ as version,
                          d.deployment_id_ as deployment_id
                   from act_re_procdef d
                   where d.id_ = i.proc_def_id_
                 ) t)                                      definition
      -- 流程部署
      ,
           (select row_to_json(t)
            from (
                   select p.id id, p.code as key, p.subject as name, p.version_ as version,
                          p.deployment_id as deployment_id, p.desc_ as desc,
                          to_char(p.deploy_date, 'YYYY-MM-DD HH24:MI:SS') deploy_time
                   from bc_wf_deploy p
                   inner join act_re_procdef d on d.deployment_id_ = p.deployment_id
                   where d.id_ = i.proc_def_id_
                 ) t)                                      deploy
    from act_hi_procinst i
    left join act_ru_execution e on e.id_ = i.id_ and e.suspension_state_ = 2 -- 2代表暂停的流程
    where i.id_ = $1
  )
    -- 全部流程变量的最新版ID
    , variable_id(id) as (
    select id_ id
    from act_hi_detail h
    where h.proc_inst_id_ = $1 and type_ = 'VariableUpdate'
          -- 排除旧版本
          and not exists(
      select 0
      from act_hi_detail h1
      where h1.proc_inst_id_ = h.proc_inst_id_ and COALESCE(h1.task_id_, '') = COALESCE(h.task_id_, '')
            and h1.type_ = h.type_ and h1.name_ = h.name_ and h1.time_ > h.time_
    )
  )
    -- 全部流程变量的最新版数据
    , variable(task_id, name, type, value) as (
    select h.task_id_ task_id, h.name_ as name, h.var_type_ as type, (case h.var_type_
                                                                      when 'boolean'
                                                                        then (case h.long_ when 1
                                                                          then 'true'
                                                                              else 'false' end)
                                                                      when 'integer'
                                                                        then h.long_ :: text
                                                                      when 'long'
                                                                        then h.long_ :: text
                                                                      when 'double'
                                                                        then h.double_ :: text
                                                                      when 'string'
                                                                        then h.text_
                                                                      when 'date'
                                                                        then to_char(to_timestamp(h.long_ / 1000),
                                                                                     'YYYY-MM-DD HH24:MI:SS')
                                                                      when 'serializable'
                                                                        then h.bytearray_id_ :: text --(select b.bytes_::text from act_ge_bytearray b where b.id_ = h.bytearray_id_)
                                                                      --when 'null' then null::text
                                                                      else text_ :: text
                                                                      end
    )                                  as value
    from act_hi_detail h inner join variable_id v on v.id = h.id_
  )
    -- 全局流程变量
    , global_variable(name, type, value) as (
    select name, type, value
    from variable v
    where v.task_id is null
    order by type, name
  )
    -- 任务签领、委托日志（不含其它日志）
    , task_log(task_id, type, time, author, actor) as (
    select tid                                                                                                                   task_id,
           type_                                       as                                                                        type,
           to_char(file_date,
                   'YYYY-MM-DD HH24:MI:SS')            as                                                                        time,
           (select row_to_json(u)
            from (select l.author_id      id, l.author_code code,
                         l.author_name as name) u)                                                                               author,
           (select row_to_json(u)
            from (select l.assignee_id id, l.assignee_code code, l.assignee_name as name) u)                                     actor
    from bc_wf_excution_log l
    where pid = $1 and type_ in ('task_claim', 'task_delegate', 'task_create')
    order by file_date desc
  )
    -- 附件、意见
    , attach(id, pid, tid, type, subject, path, ext, size, author, file_date, description) as (
    select a.id, a.pid, (case common when true
      then null
                         else a.tid end)  tid, a.type_                                          as type, a.subject,
      a.path_                                                                                   as path, a.ext,
      a.size_                                                                                   as size,
      (select row_to_json(u)
       from (select h.id id, h.actor_code code, h.actor_name as name
             from bc_identity_actor_history h
             where h.id = a.author_id) u) author, to_char(a.file_date, 'YYYY-MM-DD HH24:MI:SS') as file_date,
      a.desc_                                                                                   as description
    from bc_wf_attach a
    where pid = $1
    order by a.file_date asc
  )
    -- 全部任务(ACT_HI_TASKINST)
    , task(id, pid, start_time, end_time, key, name, duration, due_date, priority, description, actor
    , assignee, owner, variables, claim_time, master, origin_actor) as (
    select t.id_                                                                        id, t.parent_task_id_ pid,
           to_char(t.start_time_, 'YYYY-MM-DD HH24:MI:SS')                              start_time,
           to_char(t.end_time_, 'YYYY-MM-DD HH24:MI:SS')                                end_time,
           t.task_def_key_                                                as            key, t.name_ as name,
           t.duration_                                                                  duration,
           to_char(t.due_date_, 'YYYY-MM-DD HH24:MI:SS')                                due_date, t.priority_ priority,
           t.description_                                                               description -- 办理人
      ,
           (
             /* 1. 经办任务 t.assignee_ 是用户编码
              * 2. 发送到用户的任务或发送到岗位但已被签领的任务, t.assignee_ 是用户编码
              */
             case (t.assignee_ is not null) when true
               then (
                 select row_to_json(u)
                 from (
                        select a.id, a.code, a.name, a.type_ as type, a.pname, false candidate
                        from bc_identity_actor a
                        where a.code = t.assignee_
                      ) u)
             -- 未签领的岗位任务：act_ru_identitylink 的 group_id_=[岗位编码]
             else (
               select row_to_json(u)
               from (
                      select a.id, a.code, a.name, a.type_ as type, a.pname, true candidate
                      from bc_identity_actor a
                      inner join act_ru_identitylink l on l.task_id_ = t.id_
                      where a.code = l.group_id_
                      order by id_ asc limit 1 -- TODO: 暂时只支持发给一个岗位，发给多个岗位时只显示第一个，不支持发给多个候选用户
                    ) u)
             end)                                                         as            actor,
           t.assignee_                                                                  assignee,
           t.owner_                                                       as            owner
      -- 任务的本地变量
      ,
           (select to_json(array_agg(row_to_json(v1)))
            from (
                   select v.name, v.type, v.value
                   from variable v
                   where v.task_id = t.id_
                   order by v.type, v.name) v1
           )                                                              as            variables
      -- 岗位任务的签领时间
      ,
           (select time
            from task_log l
            where l.task_id = t.id_ and l.type = 'task_claim' limit 1)    as            claim_time
      -- 执行委托操作的用户
      ,
           (select author
            from task_log l
            where l.task_id = t.id_ and l.type = 'task_delegate' limit 1) as            master
      -- 执行委托前的原始用户
      ,
           (select actor
            from task_log l
            where l.task_id = t.id_ and l.type in ('task_claim', 'task_create')
                  and exists(select 0
                             from task_log l1
                             where l1.task_id = t.id_ and l1.type = 'task_delegate')
            order by time desc limit 1
           )                                                              as            origin_actor
      -- 任务的附件、意见
      ,
           (select to_json(array_agg(row_to_json(a)))
            from attach a
            where a.tid = t.id_)                                                        attachs

    from ACT_HI_TASKINST t
    where t.proc_inst_id_ = $1
    order by t.start_time_ asc
  )
  select row_to_json(t) into r
  from (
         -- 流程实例、定义、部署信息
         select i.* -- 全局流程变量
           ,
           (select to_json(array_agg(row_to_json(v)))
            from global_variable v)                               variables

           -- 流程全局附件、意见
           ,
           (select to_json(array_agg(row_to_json(a)))
            from attach a
            where a.tid is null)                                  attachs

           -- 流程变量为 serializable 类型对应的 bytearray_id_ 收集，应用需要另行通过表 act_ge_bytearray 获取对应的数据
           ,
           (select to_json(array_agg(v.value))
            from variable v
            where v.type = 'serializable')                        bytearray_ids

           -- 待办任务
           ,
           coalesce((select to_json(array_agg(row_to_json(t)))
                     from task t
                     where t.end_time is null), '[]' :: json)     todo_tasks

           -- 经办任务
           ,
           coalesce((select to_json(array_agg(row_to_json(t)))
                     from task t
                     where t.end_time is not null), '[]' :: json) done_tasks

         -- 任务签领、委托日志
         --, (select to_json(array_agg(row_to_json(v))) from task_log v) task_logs

         from instance i
       ) t;

  return r;
end;
$BODY$ language plpgsql;

-- test 方便查看各key的值
-- 流转中-done0-todo1： 3538900
-- 流转中-done9-todo2： 3349285 司机新入职、留用审批流程 - 司机留用审批（崔土新2014-10-14) - IP7 13.20s
-- 已结束-done8： 3326570 宝城公司公文处理流程 - 关于粤A.G4P40车辆公共替班钟继昌终止合同申请 - IP7 11.52s
with wf(j) as (
  select wf__find_process_instance_detail('3829127')
)
-- 展开第一层的key
select *
from json_each((select wf.j
                from wf))
union all select 'variables.len', to_json(json_array_length((select wf.j -> 'variables'
                                                             from wf)))

-- 如果有待办
union all select 'todo_tasks[' || (row_number() over ()) || ']' || (task.value ->> 'id'), task.value
          from json_array_elements((select wf.j -> 'todo_tasks'
                                    from wf)) task
union all select 'todo_tasks.len', to_json(json_array_length((select wf.j -> 'todo_tasks'
                                                              from wf)))

-- 如果有经办
union all select 'done_tasks[' || (row_number() over ()) || ']' || (task.value ->> 'id'), task.value
          from json_array_elements((select wf.j -> 'done_tasks'
                                    from wf)) task
union all select 'done_tasks.len', to_json(json_array_length((select wf.j -> 'done_tasks'
                                                              from wf)))

-- 如果有任务签领、委托日志
union all select 'task_logs[' || (row_number() over ()) || ']' || (task.value ->> 'task_id'), task.value
          from json_array_elements((select wf.j -> 'task_logs'
                                    from wf)) task
union all select 'task_logs.len', to_json(json_array_length((select wf.j -> 'task_logs'
                                                             from wf)))

/* test

*/