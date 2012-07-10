/**
 * 
 */
package cn.bc.workflow.flowattach.service;

import java.util.List;

import cn.bc.workflow.flowattach.domain.FlowAttach;

/**
 * 流程附加信息Service
 * 
 * @author dragon
 */
public interface FlowAttachService {
	/**
	 * 获取流程附加信息列表，不包含流程任务的附加信息
	 * 
	 * @param processInstanceId
	 *            所属流程实例ID
	 * @return
	 */
	List<FlowAttach> findByProcess(String processInstanceId);

	/**
	 * 获取流程附加信息列表
	 * 
	 * @param processInstanceId
	 *            所属流程实例ID
	 * @param includeTask
	 *            是否包含流程任务的附加信息
	 * @return
	 */
	List<FlowAttach> findByProcess(String processInstanceId, boolean includeTask);

	/**
	 * 获取任务附加信息列表
	 * 
	 * @param taskIds
	 *            任务ID列表
	 * @return
	 */
	List<FlowAttach> findByTask(String[] taskIds);

	/**
	 * 获取任务附加信息列表
	 * 
	 * @param taskId
	 *            任务ID
	 * @return
	 */
	List<FlowAttach> findByTask(String taskId);
}