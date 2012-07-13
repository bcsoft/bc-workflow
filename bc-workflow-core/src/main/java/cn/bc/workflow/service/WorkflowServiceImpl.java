/**
 * 
 */
package cn.bc.workflow.service;

import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.persistence.entity.CommentEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.core.exception.CoreException;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.workflow.activiti.ActivitiUtils;

/**
 * 工作流Service的实现
 * 
 * @author dragon
 */
public class WorkflowServiceImpl implements WorkflowService {
	private static final Log logger = LogFactory
			.getLog(WorkflowServiceImpl.class);
	private TemplateService templateService;
	private RuntimeService runtimeService;
	private RepositoryService repositoryService;
	private IdentityService identityService;
	private TaskService taskService;

	// private FormService formService;
	private HistoryService historyService;

	@Autowired
	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Autowired
	public void setRuntimeService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	@Autowired
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
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

	// @Autowired
	// public void setFormService(FormService formService) {
	// this.formService = formService;
	// }

	/**
	 * 获取当前用户的帐号信息
	 * 
	 * @return
	 */
	private String getCurrentUserAccount() {
		return SystemContextHolder.get().getUser().getCode();
	}

	public String startFlowByKey(String key) {
		// 设置Activiti认证用户
		String initiator = setAuthenticatedUser();

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

	/**
	 * 初始化Activiti的当前认证用户信息
	 * 
	 * @return
	 */
	private String setAuthenticatedUser() {
		// 获取当前用户
		String initiator = getCurrentUserAccount();

		// 设置认证用户
		identityService.setAuthenticatedUserId(initiator);
		return initiator;
	}

	public void claimTask(String taskId) {
		// 设置Activiti认证用户
		setAuthenticatedUser();

		// 领取任务：TODO 表单信息的处理
		this.taskService.claim(taskId, getCurrentUserAccount());
	}

	public void completeTask(String taskId) {
		// 设置Activiti认证用户
		setAuthenticatedUser();

		// 完成任务：TODO 表单信息的处理
		this.taskService.complete(taskId);
	}

	public void delegateTask(String taskId, String toUser) {
		// 设置Activiti认证用户
		setAuthenticatedUser();

		this.taskService.delegateTask(taskId, toUser);
	}
	
	public void assignTask(String taskId, String toUser) {
		// 设置Activiti认证用户
		setAuthenticatedUser();
		
		// 领取任务：TODO 表单信息的处理
		this.taskService.claim(taskId, toUser);
	}

	public Deployment deployZipFromTemplate(String templateCode) {
		// 获取模板
		Template template = templateService.loadByCode(templateCode);
		if (template == null) {
			throw new CoreException("not exists template. code=" + templateCode);
		}
		String extension = template.getTemplateType().getExtension();
		if (!("zip".equalsIgnoreCase(extension) || "bar"
				.equalsIgnoreCase(extension))) {
			throw new CoreException("template type must be zip or bar. code="
					+ templateCode);
		}

		// 获取文件流
		InputStream zipFile = template.getInputStream();

		// 发布
		String name = template.getSubject();
		if (!name.endsWith(extension)) {
			name += "." + extension;
		}
		Deployment d = repositoryService.createDeployment().name(name)
				.addZipInputStream(new ZipInputStream(zipFile)).deploy();
		return d;
	}

	public Deployment deployXmlFromTemplate(String templateCode) {
		// 获取模板
		Template template = templateService.loadByCode(templateCode);
		if (template == null) {
			throw new CoreException("not exists template. code=" + templateCode);
		}
		String extension = template.getTemplateType().getExtension();
		if (!("xml".equalsIgnoreCase(extension) || "bpmn"
				.equalsIgnoreCase(extension))) {
			throw new CoreException("template type must be xml or bpmn. code="
					+ templateCode);
		}

		// 获取文件流
		InputStream xmlFile = template.getInputStream();

		// 发布
		String name = template.getSubject();
		if (!name.endsWith(extension)) {
			name += "." + extension;
		}
		String code = template.getCode();
		if (!code.endsWith(extension)) {
			code += "." + extension;
		}
		Deployment d = repositoryService.createDeployment().name(name)
				.addInputStream(code, xmlFile).deploy();
		return d;
	}

	public void deleteDeployment(String deploymentId) {
		repositoryService.deleteDeployment(deploymentId);
	}

	public void deleteDeployment(String deploymentId, boolean cascade) {
		repositoryService.deleteDeployment(deploymentId, cascade);
	}

	public ProcessInstance loadInstance(String id) {
		return runtimeService.createProcessInstanceQuery()
				.processInstanceId(id).singleResult();
	}

	public ProcessDefinition loadDefinition(String id) {
		return repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(id).singleResult();
	}

	public InputStream getInstanceDiagram(String processInstanceId) {
		// 获取流程实例
		ProcessInstance instance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();

		// 获取流程定义
		ProcessDefinition definition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionId(instance.getProcessDefinitionId())
				.singleResult();

		// 获取发布的资源文件
		return repositoryService.getResourceAsStream(
				definition.getDeploymentId(),
				definition.getDiagramResourceName());
	}

	public InputStream getDeploymentResource(String deploymentId,
			String resourceName) {
		return repositoryService
				.getResourceAsStream(deploymentId, resourceName);
	}

	public Map<String, Object> getInstanceParams(String processInstanceId) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Object> getTaskParams(String taskId) {
		// TODO Auto-generated method stub
		return null;
	}

}