/**
 * 
 */
package cn.bc.workflow.service;

import java.util.Map;

/**
 * 工作空间Service
 * 
 * @author dragon
 */
public interface WorkspaceService_old {
	/**
	 * 获取流程实例的工作空间显示信息
	 *
	 * @param processInstanceId
	 *            流程实例ID
	 * @return
	 */
	Map<String, Object> findWorkspaceInfo(String processInstanceId);
}