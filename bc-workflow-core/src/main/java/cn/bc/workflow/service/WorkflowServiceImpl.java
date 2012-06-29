/**
 * 
 */
package cn.bc.workflow.service;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.activiti.ActivitiUtils;

/**
 * 工作流Service的实现
 * 
 * @author dragon
 */
public class WorkflowServiceImpl implements WorkflowService {
	private static final Log logger = LogFactory
			.getLog(WorkflowServiceImpl.class);
	private RuntimeService runtimeService;
	private FormService formService;
	private IdentityService identityService;
	private TaskService taskService;
	private HistoryService historyService;

	@Autowired
	public void setRuntimeService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	@Autowired
	public void setFormService(FormService formService) {
		this.formService = formService;
	}

	@Autowired
	public void setIdentityService(IdentityService identityService) {
		this.identityService = identityService;
	}

	@Autowired
	public void setTaskService(TaskService taskService) {
		this.taskService = taskService;
	}

	@Autowired
	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}

	/**
	 * 获取当前用户的帐号信息
	 * 
	 * @return
	 */
	private String getCurrentUserAccount() {
		return SystemContextHolder.get().getUser().getCode();
	}

	public String startFlowByKey(String key) {
		// 获取当前用户
		String initiator = getCurrentUserAccount();

		// 设置认证用户
		identityService.setAuthenticatedUserId(initiator);

		// 启动流程：TODO 表单信息的处理
		ProcessInstance pi = runtimeService.startProcessInstanceByKey(key);
		if (logger.isDebugEnabled()) {
			logger.debug("key=" + key);
			logger.debug("initiator=" + initiator);
			logger.debug("pi=" + ActivitiUtils.toString(pi));
		}

		// 返回流程实例的id
		return pi.getProcessInstanceId();
	}

	public void claimTask(String taskId) {
		// 领取任务：TODO 表单信息的处理
		this.taskService.claim(taskId, getCurrentUserAccount());
	}

	public void completeTask(String taskId) {
		// 完成任务：TODO 表单信息的处理
		this.taskService.complete(taskId);
	}
}