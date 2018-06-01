-- 任务监控性能优化
-- 原来的本地查询约15s，优化后约1s
--explain analyze 
select t.*
--, wf_get_task_instance_subject(t1.id_) as task_subject  
from (
       select t.id_                    task_id, t.name_ task_name -- 任务状态：1-处理中、2-暂停、3-已结束
         ,
              case when t.end_time_ is not null
                then 3
              else
                (case when (pd.suspension_state_ = 2 or e.suspension_state_ = 2)
                  then 2
                 else 1 end)
              end                   as task_status,
              t.start_time_            start_time,
              t.end_time_              end_time,
              t.duration_              duration,
              t.due_date_           as due_date,
              t.proc_inst_id_          process_id,
              pd.name_                 process_name,
              a.name                as assignee,
              e.suspension_state_      pstatus,
              pi.info ->> 'wf_code' as wf_code,
              pi.info ->> 'subject' as wf_subject
       from act_hi_taskinst t
       inner join act_hi_procinst p on p.proc_inst_id_ = t.proc_inst_id_
       inner join bc_wf_procinst_info pi on pi.id = p.id_
       inner join act_re_procdef pd on pd.id_ = t.proc_def_id_
       left join act_ru_execution e on e.proc_inst_id_ = t.proc_inst_id_
       -- 暂停及流转中流程需要join此表
       left join bc_identity_actor a on a.code = t.assignee_
       where e.parent_id_ is null -- 不考虑 activiti 的子流程

             -- 处理中任务
             --and t.end_time_ is null and (pd.suspension_state_ = 1 and e.suspension_state_ = 1)

             -- 已暂停任务
             --and t.end_time_ is null and (pd.suspension_state_ = 2 or e.suspension_state_ = 2)

             -- 已完成任务
             --and t.end_time_ is not null

             -- 模糊搜索
             and (
               -- 任务名称
               a.name like '%客管%'
               -- 任务办理人
               or t.name_ like '%客管%'
               -- 流程名称
               or pd.name_ like '%客管%'
               -- 流程标题、流水号
               --or pi.info->>'subject' like '%客管%' or pi.info->>'wf_code' like '%客管%'
             )
       order by t.start_time_ desc
       limit 10
     ) t;