/**
 * 
 */
package cn.bc.workflow.service;

import java.util.Map;

/**
 * 工作空间 Service
 * 
 * @author dragon
 */
public interface WorkspaceService {
	/**
	 * 获取流程实例的详细信息(包含流程定义、部署、待办、经办等信息)
	 *
	 * @param processInstanceId 流程实例ID
	 * @return
	 */
	Map<String, Object> getProcessInstanceDetail(String processInstanceId);

	/**
	 * 获取流程实例用于格式化工作空间的数据
	 *
	 * @param processInstanceId 流程实例ID
	 * @return
	 */
	Map<String, Object> getWorkspaceData(String processInstanceId);
}