/**
 * 
 */
package cn.bc.workflow.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.JsonUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.service.ActorHistoryService;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.workflow.activiti.ActivitiUtils;
import cn.bc.workflow.dao.WorkflowDao;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.domain.DeployResource;
import cn.bc.workflow.deploy.service.DeployService;
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
	private ActorService actorService;
	private DeployService deployService;
	
	private WorkflowDao workflowDao;

	@Autowired
	public void setWorkflowtDao(WorkflowDao workflowDao) {
		this.workflowDao = workflowDao;
	}

	@Autowired
	public void setActorService(
			@Qualifier(value = "actorService") ActorService actorService) {
		this.actorService = actorService;
	}

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

	@Autowired
	public void setDeployService(DeployService deployService) {
		this.deployService = deployService;
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

	/**
	 * 启动指定编码流程的最新版本
	 * 
	 * @param key
	 *            流程编码
	 * @return 流程实例的id
	 */
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
	 * 启动指定流程定义id的流程
	 * 
	 * @param id
	 * @return
	 */
	public String startFlowByDefinitionId(String id) {
		// 设置Activiti认证用户
		String initiator = setAuthenticatedUser();

		ProcessInstance pi = runtimeService.startProcessInstanceById(id);
		if (logger.isDebugEnabled()) {
			logger.debug("id=" + id);
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

		Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();
		String originAssignee = task.getAssignee();
		// 委托任务
		this.taskService.delegateTask(taskId, toUser);
		if(originAssignee.equalsIgnoreCase(toUser)){//委托人是原办理人
			// 保存excutionlog信息
			saveExcutionLogInfo4DelegateAndAssign(taskId, toUser,
					ExcutionLog.TYPE_TASK_INSTANCE_DELEGATE, "任务委托给");
		}else{//第三方委托
			// 保存excutionlog信息
			ActorHistory ah = this.actorHistoryService.loadByCode(task.getAssignee());
			String msg = ah.getName()+"的任务委托给";
			saveExcutionLogInfo4DelegateAndAssign(taskId, toUser,
					ExcutionLog.TYPE_TASK_INSTANCE_DELEGATE, msg);
		}

		return this.actorHistoryService.loadByCode(toUser);
	}

	public ActorHistory assignTask(String taskId, String toUser) {
		// 设置Activiti认证用户
		setAuthenticatedUser();

		// 领取任务：TODO 表单信息的处理
		this.taskService.claim(taskId, toUser);

		// 保存excutionlog信息
		saveExcutionLogInfo4DelegateAndAssign(taskId, toUser,
				ExcutionLog.TYPE_TASK_INSTANCE_ASSIGN, "任务分派给");

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
		log.setExcutionCode(task.getProcessDefinitionId());
		log.setExcutionName(task.getName());

		String date = DateUtils.formatCalendar2Minute(log.getFileDate());
		log.setDescription(h.getName() + "在" + date + "成功将" + msg
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

		// 获取流程资源
		DeployResource dr = deployService
				.findDeployResourceByDmIdAndwfCodeAndresCode(
						definition.getDeploymentId(), definition.getKey(),
						definition.getKey());

		InputStream inputStream = null;

		if (dr != null) {
			// 获取流程资源的文件
			inputStream = this.getDeployDiagram(dr);
		} else {
			// 获取发布的资源文件
			inputStream = repositoryService.getResourceAsStream(
					definition.getDeploymentId(),
					definition.getDiagramResourceName());
		}

		return inputStream;
	}

	public InputStream getDiagram(Long deployId) {
		InputStream inputStream = null;
		Deploy deploy = deployService.load(deployId);
		DeployResource dr = this.deployService.findDeployResourceCode(deploy
				.getCode());
		// 根据流程编码不能获取流程部署资源,则获取指定Activiti流程部署的相关资源文件流
		if (dr != null) {
			inputStream = this.getDeployDiagram(dr);
		} else {
			inputStream = this.getDeploymentDiagram(deploy.getDeploymentId());
		}
		return inputStream;
	}

	public InputStream getDeployDiagram(DeployResource dr) {
		InputStream inputStream = null;
		// 上传部署资源的存储的绝对路径
		String drRealPath = Attach.DATA_REAL_PATH + "/"
				+ DeployResource.DATA_SUB_PATH + "/" + dr.getPath();
		File file = new File(drRealPath);
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			logger.warn(e.getMessage(), e);
			throw new CoreException(e.getMessage(), e);
		}
		return inputStream;
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
		Assert.notNull(pi, "can't find process instance:id" + processInstanceId);
		params.put("id", pi.getId());
		params.put("pdid", pi.getProcessDefinitionId());
		params.put("startUser", getActorNameByCode(pi.getStartUserId()));
		params.put("businessKey", pi.getBusinessKey());
		addDateParam(params, "startTime", pi.getStartTime());
		addDateParam(params, "endTime", pi.getEndTime());
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
			if (v.getTaskId() == null)
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
		
		// 全局附件
		List<FlowAttach> attachs = flowAttachService.findAttachsByProcess(
				processInstanceId, false);
		params.put("attachs", attachs);
		if (logger.isDebugEnabled()) {
			logger.debug("attachs.size=" + attachs.size());
		}
		params.put("attachs_str", buildAttachsString(attachs));
		
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
		List<FlowAttach> allAttachs = flowAttachService
				.findAttachsByTask(tids);
		List<Map<String,Object>> alltaskParams = new ArrayList<Map<String,Object>>();
		for (HistoricTaskInstance task : tasks) {
			taskCode = task.getTaskDefinitionKey();
			taskParams = new HashMap<String, Object>();
			taskParams.put("id", task.getId());
			taskParams.put("code", taskCode);
			taskParams.put("owner", task.getOwner());
			taskParams.put("assignee", getActorNameByCode(task.getAssignee()));
			taskParams.put("assigneeCode", task.getAssignee());
			taskParams.put("desc", task.getDescription());
			taskParams.put("dueDate", task.getDueDate());
			taskParams.put("priority", task.getPriority());
			addDateParam(taskParams, "startTime", task.getStartTime());
			addDateParam(taskParams, "endTime", task.getEndTime());
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
			
			// 任务的附件
			attachs = findTaskFlowAttachs(task.getId(), allAttachs);
			taskParams.put("attachs", attachs);
			taskParams.put("attachs_str", buildAttachsString(attachs));

			// add：如果一个节点产生多个实例，只会有最后执行任务的相关信息
			if (params.containsKey(taskCode)) {
				Object p = params.get(taskCode);
				if (p instanceof List) {
					((List<Map<String, Object>>) p).add(taskParams);// 累加到列表中
				} else if (p instanceof Map) {
					List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
					list.add((Map<String, Object>) p);// Map转换为List
					params.put(taskCode, list);
				} else {
					throw new CoreException(
							"taskCode existed and it's type is unsupport! taskCode="
									+ taskCode);
				}
			} else {
				if (taskCode.endsWith("_s")) {// 强制使用List记录
					List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
					list.add(taskParams);
					params.put(taskCode, list);
				} else {
					params.put(taskCode, taskParams);
				}
			}
			
			// 全部经办实例信息的独立记录
			alltaskParams.add(taskParams);
		}
		params.put("tasks", alltaskParams);
		
		return params;
	}

	public Map<String, Object> getTaskHistoryParams(String taskId) {
		return getTaskHistoryParams(taskId, false);
	}

	public Map<String, Object> getTaskHistoryParams(String taskId,
			boolean withProcessInfo) {
		Assert.notNull(taskId, "task instance id must not to be null:" + taskId);
		Map<String, Object> params = new HashMap<String, Object>();
		Map<String, Object> variableParams;

		// 任务实例
		HistoricTaskInstance task = this.historyService
				.createHistoricTaskInstanceQuery().taskId(taskId)
				.singleResult();
		Assert.notNull(task, "can't find task instance:id=" + taskId);
		params = new HashMap<String, Object>();
		params.put("id", task.getId());
		params.put("code", task.getTaskDefinitionKey());
		params.put("owner", task.getOwner());
		params.put("assignee", getActorNameByCode(task.getAssignee()));
		params.put("assigneeCode", task.getAssignee());
		params.put("desc", task.getDescription());
		params.put("dueDate", task.getDueDate());
		params.put("priority", task.getPriority());
		addDateParam(params, "startTime", task.getStartTime());
		addDateParam(params, "endTime", task.getEndTime());
		params.put("duration", task.getDurationInMillis());
		params.put("deleteReason", task.getDeleteReason());

		// 任务的本地流程变量
		HistoricVariableUpdate v;
		List<HistoricDetail> variables = historyService
				.createHistoricDetailQuery().taskId(taskId).variableUpdates()
				.orderByTime().asc().list();
		variableParams = new HashMap<String, Object>();
		for (HistoricDetail hd : variables) {
			v = (HistoricVariableUpdate) hd;
			variableParams.put(v.getVariableName(),
					convertSpecialValiableValue(v));
		}
		params.put("vs", variableParams);

		// 任务的意见
		List<FlowAttach> comments = flowAttachService
				.findCommentsByTask(new String[] { taskId });
		params.put("comments", comments);
		params.put("comments_str", buildCommentsString(comments));
		
		// 任务的附件
		List<FlowAttach> attachs = flowAttachService
				.findAttachsByTask(new String[] { taskId });
		params.put("attachs", attachs);
		params.put("attachs_str", buildAttachsString(attachs));

		if (withProcessInfo) {
			Map<String, Object> processParams = new HashMap<String, Object>();

			// 流程实例
			HistoricProcessInstance pi = historyService
					.createHistoricProcessInstanceQuery()
					.processInstanceId(task.getProcessInstanceId())
					.singleResult();
			Assert.notNull(
					pi,
					"can't find process instance:id"
							+ task.getProcessInstanceId());
			processParams.put("id", pi.getId());
			processParams.put("pdid", pi.getProcessDefinitionId());
			processParams.put("startUser",
					getActorNameByCode(pi.getStartUserId()));
			processParams.put("businessKey", pi.getBusinessKey());
			addDateParam(processParams, "startTime", pi.getStartTime());
			addDateParam(processParams, "endTime", pi.getEndTime());
			processParams.put("duration", pi.getDurationInMillis());
			processParams.put("deleteReason", pi.getDeleteReason());

			// 流程定义
			ProcessDefinition pd = repositoryService
					.createProcessDefinitionQuery()
					.processDefinitionId(pi.getProcessDefinitionId())
					.singleResult();
			processParams.put("category", pd.getCategory());
			processParams.put("key", pd.getKey());
			processParams.put("name", pd.getName());
			processParams.put("version", pd.getVersion());

			// 流程变量
			variables = historyService.createHistoricDetailQuery()
					.processInstanceId(task.getProcessInstanceId())
					.variableUpdates().orderByTime().asc().list();
			variableParams = new HashMap<String, Object>();
			for (HistoricDetail hd : variables) {
				v = (HistoricVariableUpdate) hd;
				if (v.getTaskId() == null)
					variableParams.put(v.getVariableName(),
							convertSpecialValiableValue(v));
			}
			processParams.put("vs", variableParams);

			// 全局意见
			comments = flowAttachService.findCommentsByProcess(
					task.getProcessInstanceId(), false);
			processParams.put("comments", comments);
			if (logger.isDebugEnabled()) {
				logger.debug("pi.comments.size=" + comments.size());
			}
			processParams.put("pi.comments_str", buildCommentsString(comments));
			
			// 全局附件
			attachs = flowAttachService.findAttachsByProcess(
					task.getProcessInstanceId(), false);
			processParams.put("attachs", attachs);
			if (logger.isDebugEnabled()) {
				logger.debug("pi.attachs.size=" + attachs.size());
			}
			processParams.put("pi.attachs_str", buildAttachsString(attachs));

			params.put("pi", processParams);
		}

		return params;
	}

	private void addDateParam(Map<String, Object> params, String key, Date date) {
		params.put(key, date);
		params.put(key + "2d", DateUtils.formatDate(date));
		params.put(key + "2m", DateUtils.formatDateTime2Minute(date));
	}

	private Object getActorNameByCode(String userCode) {
		return actorService.loadActorNameByCode(userCode);
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
		/*
		 * for (FlowAttach comment : comments) { // 意见的字符串表示：“[姓名1] [时间1]
		 * [标题1]\r\n[姓名2] [时间2] [标题2]...”
		 * comments_str.append(comment.getAuthor().getName() + " " +
		 * DateUtils.formatCalendar2Minute(comment.getFileDate()) + " " +
		 * comment.getSubject() + "\r\n"); }
		 */

		if (comments.isEmpty())
			return comments_str;

		String desc = "";

		// 构建意见字符串
		for (int i = 0; i < comments.size(); i++) {
			desc = comments.get(i).getDesc();
			if (desc == null || desc.equals("")) {
				desc = comments.get(i).getSubject();
			}
			if (i + 1 == comments.size()) {
				comments_str.append(desc);
			} else {
				comments_str.append(desc + "　");
			}
		}
		return comments_str;
	}
	
	/**
	 * @param attachs
	 * @return
	 */
	private StringBuffer buildAttachsString(List<FlowAttach> attachs) {
		StringBuffer attachs_str;
		attachs_str = new StringBuffer();

		if (attachs.isEmpty())
			return attachs_str;

		// 附件字符串
		for (int i = 0; i < attachs.size(); i++) {
			if (i + 1 == attachs.size()) {
				attachs_str.append(attachs.get(i).getSubject());
			} else {
				attachs_str.append(attachs.get(i).getSubject() + ",");
			}
		}
		return attachs_str;
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

	public void deleteInstance(String[] instanceIds) {
		if (instanceIds == null || instanceIds.length == 0) {
			throw new CoreException("没有指定要删除的流程实例信息！");
		}

		for (String id : instanceIds) {
			HistoricProcessInstance pi = this.historyService
					.createHistoricProcessInstanceQuery().processInstanceId(id)
					.singleResult();
			if (pi == null) {
				throw new CoreException("要删除的流程实例在系统总已经不存在：id=" + id);
			}
			boolean flowing = pi.getEndTime() == null;

			// 删除流转中数据
			if (flowing) {
				this.runtimeService.deleteProcessInstance(id, "force-delete");
			}

			// 删除历史数据
			this.historyService.deleteHistoricProcessInstance(id);

			// 删除流转日志、意见、附件 TODO
		}
	}
	/**
	 * 激活流程
	 */
	public void doActive(String id) {
		// 设置Activiti认证用户
		String initiator = setAuthenticatedUser();
				
		//激活流程
		runtimeService.activateProcessInstanceById(id);
		
		if (logger.isDebugEnabled()) {
			logger.debug("id=" + id);
			logger.debug("initiator=" + initiator);
		}
		
		HistoricProcessInstance pi = historyService
				.createHistoricProcessInstanceQuery()
				.processInstanceId(id).singleResult();
		
		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(pi.getProcessDefinitionId()).singleResult();

		
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
		log.setExcutionId("0");
		log.setType(ExcutionLog.TYPE_PROCESS_ACTIVE);
		log.setProcessInstanceId(id);
		log.setExcutionCode(pi.getProcessDefinitionId());
		log.setExcutionName(pd.getName());
		
		String date = DateUtils.formatCalendar2Minute(log.getFileDate());
		log.setDescription(h.getName() + "在" + date + "成功将" +pd.getName()+ "激活");
		// 保存
		this.excutionLogService.save(log);
		
	}

	/**
	 * 暂停流程
	 */
	public void doSuspended(String id) {
		// 设置Activiti认证用户
		String initiator = setAuthenticatedUser();
		
		//暂停流程
		runtimeService.suspendProcessInstanceById(id);
		
		if (logger.isDebugEnabled()) {
			logger.debug("id=" + id);
			logger.debug("initiator=" + initiator);
		}
		
		HistoricProcessInstance pi = historyService
				.createHistoricProcessInstanceQuery()
				.processInstanceId(id).singleResult();
		
		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(pi.getProcessDefinitionId()).singleResult();
		
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
		log.setExcutionId("0");
		log.setType(ExcutionLog.TYPE_PROCESS_SUSPENDED);
		log.setProcessInstanceId(id);
		log.setExcutionCode(pi.getProcessDefinitionId());
		log.setExcutionName(pd.getName());
		
		String date = DateUtils.formatCalendar2Minute(log.getFileDate());
		log.setDescription(h.getName() + "在" + date + "成功将" +pd.getName()+ "暂停");
		// 保存
		this.excutionLogService.save(log);

		
	}

	public String startFlowByKey(String key, Map<String, Object> variables) {
		// 设置Activiti认证用户
		String initiator = setAuthenticatedUser();

		// 启动流程：TODO 表单信息的处理
		ProcessInstance pi = runtimeService.startProcessInstanceByKey(key,variables);
		if (logger.isDebugEnabled()) {
			logger.debug("key=" + key);
			logger.debug("initiator=" + initiator);
			logger.debug("pi=" + ActivitiUtils.toString(pi));
			logger.debug("variables="+variables.toString());
		}

		// 返回流程实例的id
		return pi.getProcessInstanceId();
	}

	public Map<String, Object> findGlobalValue(String pid, String[] valueKeys) {
		return this.workflowDao.findGlobalValue(pid, valueKeys);
	}


}