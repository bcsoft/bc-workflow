/**
 * 
 */
package cn.bc.workflow.flowattach.service;

import java.util.List;

import cn.bc.workflow.flowattach.domain.FlowAttach;

/**
 * 流程附加信息Service
 * 
 * @author lbj
 */
public interface FlowAttachService {
	/**
	 * 获取可用的流程附加信息列表
	 * 
	 * @param processInstanceId
	 *            所属流程实例ID
	 * @param taskId
	 *            所属流程任务ID，为空代表获取全局流程附加信息
	 * @return
	 */
	List<FlowAttach> find(String processInstanceId, String taskId);
}