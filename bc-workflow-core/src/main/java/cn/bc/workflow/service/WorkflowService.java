/**
 * 
 */
package cn.bc.workflow.service;

/**
 * 工作流Service
 * 
 * @author dragon
 */
public interface WorkflowService {
	/**
	 * 启动指定编码流程的最新版本
	 * 
	 * @param key
	 *            流程编码
	 * @return 流程实例的id
	 */
	String startFlowByKey(String key);

	/**
	 * 领取任务
	 * 
	 * @param taskId
	 *            任务ID
	 */
	void claimTask(String taskId);

	/**
	 * 完成任务
	 * 
	 * @param taskId
	 *            任务ID
	 */
	void completeTask(String taskId);
}