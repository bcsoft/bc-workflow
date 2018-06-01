/**
 *
 */
package cn.bc.workflow.flowattach.dao;

import cn.bc.core.dao.CrudDao;
import cn.bc.workflow.flowattach.domain.FlowAttach;


/**
 * 流程附加信息Dao
 *
 * @author lbj
 */
public interface FlowAttachDao extends CrudDao<FlowAttach> {

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