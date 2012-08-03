/**
 * 
 */
package cn.bc.workflow.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.JsonUtils;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.service.ActorHistoryService;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.workflow.activiti.ActivitiUtils;
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import cn.bc.workflow.flowattach.service.FlowAttachService;

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
	private ExcutionLogService excutionLogService;
	private ActorHistoryService actorHistoryService;
	private FlowAttachService flowAttachService;

	// private FormService formService;
	private HistoryService historyService;

	@Autowired
	public void setFlowAttachService(FlowAttachService flowAttachService) {
		this.flowAttachService = flowAttachService;
	}

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
	public void setExcutionLogService(ExcutionLogService excutionLogService) {
		this.excutionLogService = excutionLogService;
	}

	@Autowired
	public void setActorHistoryService(ActorHistoryService actorHistoryService) {
		this.actorHistoryService = actorHistoryService;
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

		// 加载当前任务
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

		// 创建执行日志
		ExcutionLog log = new ExcutionLog();
		log.setFileDate(Calendar.getInstance());
		ActorHistory h = SystemContextHolder.get().getUserHistory();
		log.setAuthorId(h.getId());
		log.setAuthorCode(h.getCode());
		log.setAuthorName(h.getName());

		// 处理人信息
		log.setAssigneeId(h.getId());
		log.setAssigneeCode(h.getCode());
		log.setAssigneeName(h.getName());

		log.setListener("custom");// 自定义
		log.setExcutionId(task.getExecutionId());
		log.setType(ExcutionLog.TYPE_TASK_INSTANCE_CLAIM);
		log.setProcessInstanceId(task.getProcessInstanceId());
		log.setTaskInstanceId(task.getId());
		log.setExcutionCode(task.getTaskDefinitionKey());
		log.setExcutionName(task.getName());

		String date = DateUtils.formatCalendar2Minute(log.getFileDate());
		log.setDescription(h.getName() + "在" + date + "签领了任务");
		// 保存
		this.excutionLogService.save(log);
	}

	public void completeTask(String taskId) {
		this.completeTask(taskId, null, null);
	}

	public void completeTask(String taskId,
			Map<String, Object> globalVariables,
			Map<String, Object> localVariables) {
		// 设置Activiti认证用户
		setAuthenticatedUser();

		if (logger.isDebugEnabled()) {
			logger.debug("globalVariables=" + globalVariables);
			logger.debug("localVariables=" + localVariables);
		}

		// 设置全局流程变量
		if (globalVariables != null && !globalVariables.isEmpty())
			this.taskService.setVariables(taskId, globalVariables);

		// 设置本地流程变量
		if (localVariables != null && !localVariables.isEmpty())
			this.taskService.setVariablesLocal(taskId, localVariables);

		// 完成任务
		this.taskService.complete(taskId);

	}

	public ActorHistory delegateTask(String taskId, String toUser) {
		// 设置Activiti认证用户
		setAuthenticatedUser();

		// 委托任务
		this.taskService.delegateTask(taskId, toUser);

		// 保存excutionlog信息
		saveExcutionLogInfo4DelegateAndAssign(taskId, toUser,
				ExcutionLog.TYPE_TASK_INSTANCE_DELEGATE, "委托给");

		return this.actorHistoryService.loadByCode(toUser);
	}

	public ActorHistory assignTask(String taskId, String toUser) {
		// 设置Activiti认证用户
		setAuthenticatedUser();

		// 领取任务：TODO 表单信息的处理
		this.taskService.claim(taskId, toUser);

		// 保存excutionlog信息
		saveExcutionLogInfo4DelegateAndAssign(taskId, toUser,
				ExcutionLog.TYPE_TASK_INSTANCE_ASSIGN, "分派给");

		return this.actorHistoryService.loadByCode(toUser);
	}

	/** 委托,分派操作保存excutionlog信息 */
	public void saveExcutionLogInfo4DelegateAndAssign(String taskId,
			String toUser, String type, String msg) {
		// 加载当前任务
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

		// 创建执行日志
		ExcutionLog log = new ExcutionLog();
		log.setFileDate(Calendar.getInstance());
		ActorHistory h = SystemContextHolder.get().getUserHistory();
		log.setAuthorId(h.getId());
		log.setAuthorCode(h.getCode());
		log.setAuthorName(h.getName());

		// 处理人信息
		ActorHistory h2 = actorHistoryService.loadByCode(toUser);
		log.setAssigneeId(h2.getId());
		log.setAssigneeCode(h2.getCode());
		log.setAssigneeName(h2.getName());

		log.setListener("custom");// 自定义
		log.setExcutionId(task.getExecutionId());
		log.setType(type);
		log.setProcessInstanceId(task.getProcessInstanceId());
		log.setTaskInstanceId(task.getId());
		log.setExcutionCode(task.getTaskDefinitionKey());
		log.setExcutionName(task.getName());

		String date = DateUtils.formatCalendar2Minute(log.getFileDate());
		log.setDescription(h.getName() + "在" + date + "成功将任务" + msg
				+ h2.getName());
		// 保存
		this.excutionLogService.save(log);
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
		HistoricProcessInstance instance = historyService
				.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();

		if (instance == null) {
			throw new CoreException(
					"can't find HistoricProcessInstance: processInstanceId="
							+ processInstanceId);
		}

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

	public InputStream getDeploymentDiagram(String deploymentId) {
		// 获取流程定义
		ProcessDefinition definition = repositoryService
				.createProcessDefinitionQuery().deploymentId(deploymentId)
				.singleResult();

		if (definition == null) {
			throw new CoreException(
					"can't find ProcessDefinition: deploymentId="
							+ deploymentId);
		}

		// 获取流程图的png资源文件
		return getDeploymentResource(deploymentId,
				definition.getDiagramResourceName());
	}

	public InputStream getDeploymentResource(String deploymentId,
			String resourceName) {
		return repositoryService
				.getResourceAsStream(deploymentId, resourceName);
	}

	public Map<String, Object> getProcessHistoryParams(String processInstanceId) {
		Assert.notNull(processInstanceId,
				"process instance id must not to be null:" + processInstanceId);
		Map<String, Object> params = new HashMap<String, Object>();
		Map<String, Object> taskParams, variableParams;

		// 流程实例
		HistoricProcessInstance pi = historyService
				.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		Assert.notNull(pi, "找不到指定的流程实例记录：processInstanceId="
				+ processInstanceId);
		params.put("id", pi.getId());
		params.put("pdid", pi.getProcessDefinitionId());
		params.put("startUser", pi.getStartUserId());
		params.put("businessKey", pi.getBusinessKey());
		params.put("startTime", pi.getStartTime());
		params.put("endTime", pi.getEndTime());
		params.put("duration", pi.getDurationInMillis());
		params.put("deleteReason", pi.getDeleteReason());

		// 流程定义
		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(pi.getProcessDefinitionId())
				.singleResult();
		params.put("category", pd.getCategory());
		params.put("key", pd.getKey());
		params.put("name", pd.getName());
		params.put("version", pd.getVersion());

		// 流程变量
		HistoricVariableUpdate v;
		List<HistoricDetail> variables = historyService
				.createHistoricDetailQuery()
				.processInstanceId(processInstanceId).variableUpdates()
				.orderByTime().asc().list();
		variableParams = new HashMap<String, Object>();
		for (HistoricDetail hd : variables) {
			v = (HistoricVariableUpdate) hd;
			variableParams.put(v.getVariableName(),
					convertSpecialValiableValue(v));
		}
		params.put("vs", variableParams);

		// 全局意见
		List<FlowAttach> comments = flowAttachService.findCommentsByProcess(
				processInstanceId, false);
		params.put("comments", comments);
		if (logger.isDebugEnabled()) {
			logger.debug("comments.size=" + comments.size());
		}
		params.put("comments_str", buildCommentsString(comments));

		// 经办任务
		String taskCode;
		List<HistoricTaskInstance> tasks = this.historyService
				.createHistoricTaskInstanceQuery()
				.processInstanceId(processInstanceId)
				.taskDeleteReason("completed")
				.orderByHistoricActivityInstanceStartTime().asc().list();
		// 获取所有任务的意见
		String[] tids = new String[tasks.size()];
		for (int i = 0; i < tasks.size(); i++) {
			tids[i] = tasks.get(i).getId();
		}
		List<FlowAttach> allComments = flowAttachService
				.findCommentsByTask(tids);
		for (HistoricTaskInstance task : tasks) {
			taskCode = task.getTaskDefinitionKey();
			taskParams = new HashMap<String, Object>();
			taskParams.put("id", task.getId());
			taskParams.put("owner", task.getOwner());
			taskParams.put("assignee", task.getAssignee());
			taskParams.put("desc", task.getDescription());
			taskParams.put("dueDate", task.getDueDate());
			taskParams.put("priority", task.getPriority());
			taskParams.put("startTime", task.getStartTime());
			taskParams.put("endTime", task.getEndTime());
			taskParams.put("duration", task.getDurationInMillis());
			taskParams.put("deleteReason", task.getDeleteReason());

			// 任务的本地流程变量
			variables = historyService.createHistoricDetailQuery()
					.processInstanceId(processInstanceId).taskId(task.getId())
					.variableUpdates().orderByTime().asc().list();
			variableParams = new HashMap<String, Object>();
			for (HistoricDetail hd : variables) {
				v = (HistoricVariableUpdate) hd;
				variableParams.put(v.getVariableName(),
						convertSpecialValiableValue(v));
			}
			taskParams.put("vs", variableParams);

			// 任务的意见
			comments = findTaskFlowAttachs(task.getId(), allComments);
			taskParams.put("comments", comments);
			taskParams.put("comments_str", buildCommentsString(comments));

			// add：如果一个节点产生多个实例，只会有最后执行任务的相关信息
			params.put(taskCode, taskParams);
		}

		return params;
	}

	/**
	 * 特殊流程变量值的转换
	 * 
	 * @param v
	 * @return
	 */
	private Object convertSpecialValiableValue(HistoricVariableUpdate v) {
		if (v.getVariableName().startsWith("list_")
				&& v.getValue() instanceof String) {// 将字符串转化为List
			return JsonUtils.toCollection((String) v.getValue());
		} else if (v.getVariableName().startsWith("map_")
				&& v.getValue() instanceof String) {// 将字符串转化为Map
			return JsonUtils.toMap((String) v.getValue());
		} else if (v.getVariableName().startsWith("array_")
				&& v.getValue() instanceof String) {// 将字符串转化为数组
			return JsonUtils.toArray((String) v.getValue());
		} else {
			return v.getValue();
		}
	}

	/**
	 * @param comments
	 * @return
	 */
	private StringBuffer buildCommentsString(List<FlowAttach> comments) {
		StringBuffer comments_str;
		comments_str = new StringBuffer();
		for (FlowAttach comment : comments) {
			// 意见的字符串表示：“[姓名1] [时间1] [标题1]\r\n[姓名2] [时间2] [标题2]...”
			comments_str.append(comment.getAuthor().getName() + " "
					+ DateUtils.formatCalendar2Minute(comment.getFileDate())
					+ " " + comment.getSubject() + "\r\n");
		}
		return comments_str;
	}

	/**
	 * 筛选出指定任务的意见、附件
	 * 
	 * @param taskId
	 * @param allFlowAttachs
	 * @return
	 */
	private List<FlowAttach> findTaskFlowAttachs(String taskId,
			List<FlowAttach> allFlowAttachs) {
		List<FlowAttach> taskFlowAttachs = new ArrayList<FlowAttach>();
		for (FlowAttach flowAttach : allFlowAttachs) {
			if (taskId.equals(flowAttach.getTid()))
				taskFlowAttachs.add(flowAttach);
		}
		return taskFlowAttachs;
	}

	public void deleteInstance(String instanceId) {
		if (instanceId == null || instanceId.length() == 0) {
			throw new CoreException("没有指定要删除的流程实例信息！");
		}
		HistoricProcessInstance pi = this.historyService
				.createHistoricProcessInstanceQuery()
				.processInstanceId(instanceId).singleResult();
		if (pi == null) {
			throw new CoreException("要删除的流程实例在系统总已经不存在：id=" + instanceId);
		}
		boolean flowing = pi.getEndTime() == null;

		// 删除流转中数据
		if (flowing) {
			this.runtimeService.deleteProcessInstance(instanceId,
					"force-delete");
		}

		// 删除历史数据
		this.historyService.deleteHistoricProcessInstance(instanceId);

		// 删除流转日志、意见、附件 TODO
	}
}