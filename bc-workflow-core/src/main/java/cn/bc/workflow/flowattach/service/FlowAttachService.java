/**
 *
 */
package cn.bc.workflow.flowattach.service;

import cn.bc.core.service.CrudService;
import cn.bc.workflow.flowattach.domain.FlowAttach;

import java.util.List;

/**
 * 流程附加信息Service
 *
 * @author dragon
 */
public interface FlowAttachService extends CrudService<FlowAttach> {
  /**
   * 获取流程附加信息列表，不包含流程任务的附加信息
   *
   * @param processInstanceId 所属流程实例ID
   * @return
   */
  List<FlowAttach> findByProcess(String processInstanceId);

  /**
   * 获取流程附加信息列表
   *
   * @param processInstanceId 所属流程实例ID
   * @param includeTask       是否包含流程任务的附加信息
   * @return
   */
  List<FlowAttach> findByProcess(String processInstanceId, boolean includeTask);

  /**
   * 获取流程意见信息列表
   *
   * @param processInstanceId 所属流程实例ID
   * @param includeTask       是否包含流程任务的附加信息
   * @return
   */
  List<FlowAttach> findCommentsByProcess(String processInstanceId, boolean includeTask);

  /**
   * 获取流程附件信息列表
   *
   * @param processInstanceId 所属流程实例ID
   * @param includeTask       是否包含流程任务的附加信息
   * @return
   */
  List<FlowAttach> findAttachsByProcess(String processInstanceId, boolean includeTask);

  /**
   * 获取任务附加信息列表
   *
   * @param taskIds 任务ID列表
   * @return
   */
  List<FlowAttach> findByTask(String[] taskIds);

  /**
   * 获取任务意见列表
   *
   * @param taskIds 任务ID列表
   * @return
   */
  List<FlowAttach> findCommentsByTask(String[] taskIds);

  /**
   * 获取任务附件列表
   *
   * @param taskIds 任务ID列表
   * @return
   */
  List<FlowAttach> findAttachsByTask(String[] taskIds);

  /**
   * 获取任务附加信息列表
   *
   * @param taskId 任务ID
   * @return
   */
  List<FlowAttach> findByTask(String taskId, int type);

  /**
   * 获取流程实例名称
   *
   * @param pid 流程实例id
   * @return
   */
  public String getProcInstName(String pid);

  /**
   * 将流程附件更新为子流程附件
   *
   * @param ids                  附件Id
   * @param subProcessInstanceId 子流程实例Id
   * @param subProcessTaskId     子流程任务Id
   */
  void updateAttachToSubProcess(Long[] ids, String subProcessInstanceId, String subProcessTaskId);
}