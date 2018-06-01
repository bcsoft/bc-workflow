package cn.bc.workflow.dao;

import java.util.List;
import java.util.Map;


/**
 * 流程dao
 *
 * @author lbj
 */
public interface WorkflowDao {

  /**
   * 查找流程全局变量
   *
   * @param pid:流程实例id
   * @param valueKeys:流程全局变量key
   * @return {流程全部变量key:流程全部变量value}
   */
  Map<String, Object> findGlobalValue(String pid, String[] valueKeys);

  /**
   * 查找流程任务最新的本地变量
   *
   * @param pid
   * @param taskKey
   * @param localValueKey
   * @return
   */
  Object findLocalValue(String pid, String taskKey, String localValueKey);

  Map<String, Object> findMainProcessInstanceInfoById(String processInstanceId);

  /**
   * 通过流程实例Id，查找子流程经办信息
   *
   * @param processInstanceId 流程实例Id
   * @return
   */
  List<Map<String, Object>> findSubProcessInstanceInfoById(String processInstanceId);

  /**
   * 强制更新 Activiti 流程部署资源的数据
   *
   * @param deploymentId Activiti的流程部署ID
   * @param resourceName Activiti的资源名称
   * @param in           新数据
   * @return 更新成功返回 true 否则返回 false
   */
  boolean updateDeploymentResource(String deploymentId, String resourceName, byte[] in);
}
