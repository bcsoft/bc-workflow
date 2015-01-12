/**
 * 
 */
package cn.bc.workflow.service;

import org.activiti.engine.impl.persistence.entity.SuspensionState;

import java.util.Map;

/**
 * 工作空间 Service
 * 
 * @author dragon
 */
public interface WorkspaceService {
	/** 流程状态：流转中*/
	public static final int FLOWSTATUS_ACTIVE = SuspensionState.ACTIVE.getStateCode();
	/** 流程状态：已暂停*/
	public static final int FLOWSTATUS_SUSPENDED = SuspensionState.SUSPENDED.getStateCode();
	/** 流程状态：已结束*/
	public static final int FLOWSTATUS_COMPLETE = 3;

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