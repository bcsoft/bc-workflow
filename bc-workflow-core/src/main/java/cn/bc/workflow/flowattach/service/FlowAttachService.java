/**
 * 
 */
package cn.bc.workflow.flowattach.service;

import java.util.List;

import cn.bc.core.service.CrudService;
import cn.bc.workflow.flowattach.domain.FlowAttach;

/**
 * 流程附加信息Service
 * 
 * @author lbj
 */
public interface FlowAttachService extends CrudService<FlowAttach> {
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
	
	/**
	 * 获取流程实例名称
	 * @param pid 流程实例id
	 * @return
	 */
	public String getProcInstName(String pid);
}