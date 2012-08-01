/**
 * 
 */
package cn.bc.workflow.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.StringUtils;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import cn.bc.workflow.flowattach.service.FlowAttachService;

/**
 * 工作流Service的实现
 * 
 * @author dragon
 */
public class WorkspaceServiceImpl implements WorkspaceService {
	private static final Log logger = LogFactory
			.getLog(WorkspaceServiceImpl.class);
	private RepositoryService repositoryService;
	private TaskService taskService;
	private FormService formService;
	private HistoryService historyService;
	private FlowAttachService flowAttachService;
	private ExcutionLogService excutionLogService;
	private WorkflowFormService workflowFormService;
	private ActorService actorService;

	@Autowired
	public void setActorService(
			@Qualifier(value = "actorService") ActorService actorService) {
		this.actorService = actorService;
	}

	@Autowired
	public void setWorkflowFormService(WorkflowFormService workflowFormService) {
		this.workflowFormService = workflowFormService;
	}

	@Autowired
	public void setFlowAttachService(FlowAttachService flowAttachService) {
		this.flowAttachService = flowAttachService;
	}

	@Autowired
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@Autowired
	public void setTaskService(TaskService taskService) {
		this.taskService = taskService;
	}

	@Autowired
	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}

	@Autowired
	public void setFormService(FormService formService) {
		this.formService = formService;
	}

	@Autowired
	public void setExcutionLogService(ExcutionLogService excutionLogService) {
		this.excutionLogService = excutionLogService;
	}

	/**
	 * 获取当前用户的帐号信息
	 * 
	 * @return
	 */
	private String getCurrentUserAccount() {
		return SystemContextHolder.get().getUser().getCode();
	}

	public Map<String, Object> findWorkspaceInfo(String processInstanceId) {
		Assert.notNull(processInstanceId, "流程实例ID不能为空" + processInstanceId);
		Map<String, Object> ws = new HashMap<String, Object>();

		// 获取流程实例信息
		HistoricProcessInstance instance = historyService
				.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		Assert.notNull(instance, "找不到指定的流程实例记录：processInstanceId="
				+ processInstanceId);
		ws.put("id", instance.getId());
		ws.put("businessKey", instance.getBusinessKey());
		ws.put("deleteReason", instance.getDeleteReason());
		ws.put("startUser", getActorNameByCode(instance.getStartUserId()));
		ws.put("startTime", instance.getStartTime());
		ws.put("endTime", instance.getEndTime());
		ws.put("duration", instance.getDurationInMillis());

		// 流转状态
		boolean flowing = instance.getEndTime() == null;
		ws.put("flowing", flowing);

		// 流程定义
		ProcessDefinition definition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionId(instance.getProcessDefinitionId())
				.singleResult();
		ws.put("definitionId", definition.getId());
		ws.put("definitionName", definition.getName());
		ws.put("definitionCategory", definition.getCategory());
		ws.put("definitionKey", definition.getKey());
		ws.put("definitionVersion", definition.getVersion());
		ws.put("definitionResourceName", definition.getResourceName());
		ws.put("definitionDiagramResourceName",
				definition.getDiagramResourceName());

		// 流程发布
		ws.put("deploymentId", definition.getDeploymentId());

		// 用户定义的流程实例标题 TODO 从流程变量中获取
		ws.put("subject", definition.getName());

		// 公共信息处理
		ws.put("commonInfo", buildWSCommonInfo(flowing, instance));

		// 待办信息处理
		ws.put("todoInfo", buildWSTodoInfo(flowing, instance));

		// 待办信息处理
		ws.put("doneInfo", buildWSDoneInfo(flowing, instance));

		// 返回综合后的信息
		return ws;
	}

	/**
	 * 构建工作空间公共信息
	 * 
	 * @param flowing
	 *            流程是否仍在流转中
	 * @param instance
	 *            流程实例的历史记录
	 */
	private Map<String, Object> buildWSCommonInfo(boolean flowing,
			HistoricProcessInstance instance) {
		Map<String, Object> info = new LinkedHashMap<String, Object>();
		info.put("buttons",
				this.buildHeaderDefaultButtons(flowing, "common", false));// 操作按钮列表
		info.put("hasButtons", info.get("buttons") != null);// 有否操作按钮
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();// 一级条目列表
		info.put("items", items);
		Map<String, Object> item;
		List<String> detail;
		String type;

		// 构建表单条目
		// item = new HashMap<String, Object>();
		// items.add(item);
		// type = "form";
		// item.put("id", "id");// TODO
		// item.put("type", type);// 信息类型
		// item.put("iconClass", "ui-icon-document");// 左侧显示的小图标
		// item.put("link", true);// 链接标题
		// item.put("subject", "表单：测试表单");// 标题 TODO
		// item.put("buttons", this.buildItemDefaultButtons(flowing, type));//
		// 操作按钮列表
		// item.put("hasButtons", item.get("buttons") != null);// 有否操作按钮
		// detail = new ArrayList<String>();
		// item.put("detail", detail);// 详细信息
		// detail.add("小明 " + " " + "2012-01-01 00:00"); // 创建信息 TODO

		// 构建意见附件条目
		List<FlowAttach> flowAttachs = flowAttachService.findByProcess(
				instance.getId(), false);
		if (logger.isDebugEnabled())
			logger.debug("flowAttachs=" + flowAttachs);
		buildFlowAttachsInfo(flowing, items, flowAttachs);

		// 构建统计信息条目
		item = new HashMap<String, Object>();
		items.add(item);
		type = "stat";
		item.put("id", instance.getId());
		item.put("type", type);// 信息类型
		item.put("iconClass", "ui-icon-flag");// 左侧显示的小图标
		item.put("subject", "统计信息");// 标题
		item.put("link", false);// 非链接标题
		item.put("hasButtons", false);// 无操作按钮
		detail = new ArrayList<String>();
		item.put("detail", detail);// 详细信息
		detail.add("发起时间：" + getActorNameByCode(instance.getStartUserId())
				+ " "
				+ DateUtils.formatDateTime2Minute(instance.getStartTime()));
		detail.add("结束时间："
				+ (flowing ? "仍在流转中..." : DateUtils
						.formatDateTime2Minute(instance.getEndTime())));
		detail.add("办理耗时："
				+ (flowing ? DateUtils.getWasteTimeCN(instance.getStartTime())
						: DateUtils.getWasteTimeCN(instance.getStartTime(),
								instance.getEndTime())));
		detail.add("流程版本：" + instance.getProcessDefinitionId());
		// detail.add("参与人数：" + "");// TODO

		// 返回
		return info;
	}

	/**
	 * 根据用户帐号获取用户的姓名
	 * 
	 * @param userCode
	 *            用户帐号
	 * @return
	 */
	private String getActorNameByCode(String userCode) {
		return actorService.loadActorNameByCode(userCode);
	}

	private void buildFormInfo(boolean flowing,
			List<Map<String, Object>> items, String processInstanceId,
			String taskId, String formKey, boolean readonly) {
		if (formKey == null || formKey.length() == 0)
			return;
		if (logger.isDebugEnabled()) {
			logger.debug("taskId=" + taskId + ",formKey=" + formKey);
		}

		// 表单基本信息
		Map<String, Object> item;
		List<String> detail;
		String type = "form";
		item = new HashMap<String, Object>();
		items.add(item);
		item.put("id", taskId);
		item.put("pid", processInstanceId);// 流程实例id
		item.put("tid", taskId);// 任务id
		item.put("type", type);// 信息类型
		item.put("link", false);// 链接标题
		item.put("buttons", this.buildItemDefaultButtons(flowing, type));// 操作按钮列表
		item.put("hasButtons", item.get("buttons") != null);// 有否操作按钮
		item.put("iconClass", "ui-icon-document");// 左侧显示的小图标
		item.put("subject", "完成任务前需要你处理如下信息：");// 标题信息

		// 表单html
		detail = new ArrayList<String>();
		item.put("detail", detail);
		// detail.add("[表单信息]");

		int index = formKey.lastIndexOf(":");
		String engine, from, key;
		boolean seperate;
		if (index != -1) {
			String[] ss = formKey.substring(0, index).split(":");
			key = formKey.substring(index + 1);
			engine = ss[0];
			if (ss.length == 1) {// engine:
				seperate = false;
				from = "resource";
			} else if (ss.length == 2) {// engine:from:
				seperate = false;
				from = ss[1];
			} else if (ss.length == 3) {// engine:seperate:from:
				seperate = "true".equalsIgnoreCase(ss[1]);
				from = ss[2];
			} else {
				throw new CoreException("unsupport config type:formKey="
						+ formKey);
			}
		} else {
			key = formKey;
			engine = "default";
			from = "resource";
			seperate = false;
		}
		item.put("form_engine", engine);
		item.put("form_from", from);// form
		item.put("form_key", key);
		item.put("form_seperate", seperate);

		String html = (String) this.workflowFormService.getRenderedTaskForm(
				taskId, readonly);
		item.put("form_html", html);

		detail.add(html);
	}

	/**
	 * @param flowing
	 * @param items
	 * @param flowAttachs
	 */
	private void buildFlowAttachsInfo(boolean flowing,
			List<Map<String, Object>> items, List<FlowAttach> flowAttachs) {
		Map<String, Object> item;
		List<String> detail;
		String type;
		for (FlowAttach flowAttach : flowAttachs) {
			item = new HashMap<String, Object>();
			items.add(item);
			item.put("id", flowAttach.getId());
			item.put("pid", flowAttach.getPid());// 流程实例id
			item.put("tid", flowAttach.getTid());// 任务id
			if (FlowAttach.TYPE_ATTACHMENT == flowAttach.getType()) {
				type = "attach";
				item.put("iconClass", "ui-icon-link");// 左侧显示的小图标
				item.put("subject", flowAttach.getSubject());// 附件名称
				item.put("size", flowAttach.getSize() + "");// 附件大小
				item.put("path", flowAttach.getPath());// 附件相对路径
				item.put("sizeInfo",
						StringUtils.formatSize(flowAttach.getSize()));// 附件大小
			} else if (FlowAttach.TYPE_COMMENT == flowAttach.getType()) {
				type = "comment";
				item.put("iconClass", "ui-icon-comment");// 左侧显示的小图标
				item.put("subject", flowAttach.getSubject());// 意见标题
				item.put("desc", flowAttach.getDesc());// 意见内容
			} else {
				logger.error("未支持的FlowAttach类型:type=" + flowAttach.getType());
				type = "none";
				item.put("iconClass", "ui-icon-lock");// 左侧显示的小图标
				item.put("subject", "(未知类型)");
			}
			item.put("type", type);// 信息类型
			item.put("link", true);// 链接标题
			item.put("buttons", this.buildItemDefaultButtons(flowing, type));// 操作按钮列表
			item.put("hasButtons", item.get("buttons") != null);// 有否操作按钮

			// 详细信息
			detail = new ArrayList<String>();
			item.put("detail", detail);
			detail.add(flowAttach.getAuthor().getName() + " "
					+ DateUtils.formatCalendar2Minute(flowAttach.getFileDate())); // 创建信息
		}
	}

	/**
	 * 创建默认的表单(form)、意见(comment)、附件(attach)操作按钮
	 * 
	 * @param editable
	 *            是否可编辑
	 * @param type
	 *            类型
	 * @return
	 */
	private String buildItemDefaultButtons(boolean editable, String type) {
		StringBuffer buttons = new StringBuffer();
		if (editable) {
			buttons.append(ITEM_BUTTON_EDIT);
		}
		buttons.append(ITEM_BUTTON_OPEN);
		if ("attach".equals(type)) {
			buttons.append(ITEM_BUTTON_DOWNLOAD);
		}
		if (editable && !"form".equals(type)) {
			buttons.append(ITEM_BUTTON_DELETE);
		}
		return buttons.length() > 0 ? buttons.toString() : null;
	}

	private final static String ITEM_BUTTON_OPEN = "<span class='itemOperate open'><span class='ui-icon ui-icon-document-b'></span><span class='text link'>查看</span></span>";
	private final static String ITEM_BUTTON_EDIT = "<span class='itemOperate edit'><span class='ui-icon ui-icon-pencil'></span><span class='text link'>编辑</span></span>";
	private final static String ITEM_BUTTON_DELETE = "<span class='itemOperate delete'><span class='ui-icon ui-icon-closethick'></span><span class='text link'>删除</span></span>";
	private final static String ITEM_BUTTON_DOWNLOAD = "<span class='itemOperate download'><span class='ui-icon ui-icon-arrowthickstop-1-s'></span><span class='text link'>下载</span></span>";

	/**
	 * 创建默认的公共信息(common)、个人待办信息(todo_user)、岗位待办信息(todo_group)区标题右侧的操作按钮
	 * 
	 * @param flowing
	 *            是否流转中
	 * @param type
	 *            类型
	 * @param isMyTask
	 *            是否是我的个人或岗位待办
	 * @return
	 */
	private String buildHeaderDefaultButtons(boolean flowing, String type,
			boolean isMyTask) {
		StringBuffer buttons = new StringBuffer();
		if ("common".equals(type)) {
			buttons.append("<span class='mainOperate flowImage'><span class='ui-icon ui-icon-image'></span><span class='text link'>查看流程图</span></span>");
			if (flowing) {
				buttons.append("<span class='mainOperate addComment'><span class='ui-icon ui-icon-document'></span><span class='text link'>添加意见</span></span>");
				buttons.append("<span class='mainOperate addAttach'><span class='ui-icon ui-icon-arrowthick-1-n'></span><span class='text link'>添加附件</span></span>");
			}
			buttons.append("<span class='mainOperate excutionLog'><span class='ui-icon ui-icon-tag' title='查看流转日志'></span></span>");
		} else if ("todo_user".equals(type)) {
			if (flowing && isMyTask) {
				if (SystemContextHolder.get()
						.hasAnyRole("BC_WORKFLOW_DELEGATE"))// 有权限才能委派任务
					buttons.append("<span class='mainOperate delegate'><span class='ui-icon ui-icon-person'></span><span class='text link'>委派任务</span></span>");

				buttons.append("<span class='mainOperate addComment'><span class='ui-icon ui-icon-document'></span><span class='text link'>添加意见</span></span>");
				buttons.append("<span class='mainOperate addAttach'><span class='ui-icon ui-icon-arrowthick-1-n'></span><span class='text link'>添加附件</span></span>");
				buttons.append("<span class='mainOperate finish'><span class='ui-icon ui-icon-check'></span><span class='text link'>完成办理</span></span>");
			}
		} else if ("todo_group".equals(type)) {
			if (flowing) {
				if (SystemContextHolder.get().hasAnyRole("BC_WORKFLOW_ASSIGN"))// 有权限才能分派任务
					buttons.append("<span class='mainOperate assign'><span class='ui-icon ui-icon-person'></span><span class='text link'>分派任务</span></span>");

				if (isMyTask)
					buttons.append("<span class='mainOperate claim'><span class='ui-icon ui-icon-check'></span><span class='text link'>签领任务</span></span>");
			}
		} else {
			return null;
		}
		return buttons.length() > 0 ? buttons.toString() : null;
	}

	/**
	 * 构建工作空间待办信息
	 * 
	 * @param flowing
	 *            流程是否仍在流转中
	 * @param instance
	 *            流程实例的历史记录
	 */
	private Map<String, Object> buildWSTodoInfo(boolean flowing,
			HistoricProcessInstance instance) {
		Map<String, Object> info = new LinkedHashMap<String, Object>();
		List<Map<String, Object>> taskItems = new ArrayList<Map<String, Object>>();// 一级条目列表
		info.put("tasks", taskItems);
		Map<String, Object> taskItem;
		List<Map<String, Object>> items;// 详细信息：表单、意见、附件
		boolean isUserTask;// 是否是个人待办:true-个人待办、false-组待办
		boolean isMyTask;// 是否是我的个人或组待办

		// 获取待办列表
		List<Task> tasks = this.taskService.createTaskQuery()
				.processInstanceId(instance.getId()).orderByTaskCreateTime()
				.asc().list();

		// 获取所有任务的意见附件
		List<String> tids = new ArrayList<String>();
		for (Task task : tasks) {
			tids.add(task.getId());
		}
		// 构建意见附件条目
		List<FlowAttach> allFlowAttachs = flowAttachService.findByTask(tids
				.toArray(new String[] {}));

		// 获取表单formKey
		Map<String, String> formKeys = excutionLogService
				.findTaskFormKeys(instance.getId());

		// 生成展现用的数据
		Date now = new Date();
		Object subject;
		for (Task task : tasks) {
			List<IdentityLink> identityLinks;
			// 判断任务类型
			if (task.getAssignee() != null) {
				isUserTask = true;// 个人待办
				isMyTask = task.getAssignee().equals(
						this.getCurrentUserAccount());// 我的待办
				identityLinks = null;
			} else {
				isUserTask = false;// 组待办

				// 获取任务的组关联信息
				identityLinks = this.taskService.getIdentityLinksForTask(task
						.getId());
				if (identityLinks == null || identityLinks.isEmpty()) {
					throw new CoreException(
							"can't find membership from table act_ru_identitylink: taskId="
									+ task.getId());
				}
				isMyTask = judgeIsMyTask(identityLinks);
			}

			// 任务的基本信息
			taskItem = new HashMap<String, Object>();
			taskItems.add(taskItem);
			taskItem.put("id", task.getId());// 任务id
			taskItem.put("isUserTask", isUserTask);// 是否是个人待办:true-个人待办、false-组待办
			taskItem.put("isMyTask", isMyTask);// 是否是我的个人或组待办
			subject = taskService.getVariableLocal(task.getId(), "subject");
			if (subject != null) {
				taskItem.put("subject", subject);// 标题
			} else {
				taskItem.put("subject", task.getName());// 标题
			}
			taskItem.put("buttons", this.buildHeaderDefaultButtons(flowing,
					isUserTask ? "todo_user" : "todo_group", isMyTask));// 操作按钮列表
			taskItem.put("hasButtons", taskItem.get("buttons") != null);// 有否操作按钮
			taskItem.put("formKey",
					taskService.getVariableLocal(task.getId(), "formKey"));// 记录formKey
			taskItem.put("desc", task.getDescription());// 任务描述说明
			taskItem.put("priority", task.getPriority());// 任务优先级

			// 任务的详细信息
			items = new ArrayList<Map<String, Object>>();// 二级条目列表
			taskItem.put("items", items);

			// -- 表单信息
			buildFormInfo(flowing, items, task.getProcessInstanceId(),
					task.getId(), formKeys.get(task.getId()),
					!(isUserTask && isMyTask));

			// -- 意见、附件信息
			buildFlowAttachsInfo(flowing, items,
					this.findTaskFlowAttachs(task.getId(), allFlowAttachs));

			// 任务的汇总信息
			if (isUserTask) {
				taskItem.put("actor",
						"待办人：" + getActorNameByCode(task.getAssignee()));
			} else {
				taskItem.put("actor", "待办岗："
						+ getActorNameByCode(identityLinks.get(0).getGroupId()));
			}
			taskItem.put(
					"createTime",
					"发起时间："
							+ DateUtils.formatDateTime2Minute(task
									.getCreateTime()));
			if (task.getDueDate() != null) {
				taskItem.put(
						"dueDate",
						"办理期限："
								+ DateUtils.formatDateTime2Minute(task
										.getDueDate()));
			}
			taskItem.put(
					"wasteTime",
					"办理耗时："
							+ DateUtils.getWasteTimeCN(task.getCreateTime(),
									now)
							+ " (从"
							+ DateUtils.formatDateTime2Minute(task
									.getCreateTime()) + "到"
							+ DateUtils.formatDateTime2Minute(now) + ")");
		}

		// 返回
		return info;
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

	/**
	 * 判断当前用户是否与IdentityLink有关
	 * 
	 * @param identityLinks
	 * @return
	 */
	private boolean judgeIsMyTask(List<IdentityLink> identityLinks) {
		if (identityLinks == null || identityLinks.isEmpty()) {
			throw new CoreException(
					"argument identityLinks can't be null or empty");
		}

		List<String> groups = SystemContextHolder.get().getAttr(
				SystemContext.KEY_GROUPS);
		for (IdentityLink l : identityLinks) {
			if (l.getGroupId() != null && groups.contains(l.getGroupId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 构建工作空间已办信息
	 * 
	 * @param flowing
	 *            流程是否仍在流转中
	 * @param instance
	 *            流程实例的历史记录
	 */
	private Map<String, Object> buildWSDoneInfo(boolean flowing,
			HistoricProcessInstance instance) {
		Map<String, Object> info = new LinkedHashMap<String, Object>();
		List<Map<String, Object>> taskItems = new ArrayList<Map<String, Object>>();// 一级条目列表
		info.put("tasks", taskItems);
		Map<String, Object> taskItem;
		List<Map<String, Object>> items;// 详细信息：表单、意见、附件

		// 获取待办列表
		List<HistoricTaskInstance> tasks = this.historyService
				.createHistoricTaskInstanceQuery()
				.processInstanceId(instance.getId())
				.taskDeleteReason("completed")
				.orderByHistoricActivityInstanceStartTime().asc().list();

		// 获取所有任务的意见附件
		List<String> tids = new ArrayList<String>();
		for (HistoricTaskInstance task : tasks) {
			tids.add(task.getId());
		}
		// 构建意见附件条目
		List<FlowAttach> allFlowAttachs = flowAttachService.findByTask(tids
				.toArray(new String[] {}));

		// 获取表单formKey
		Map<String, String> formKeys = excutionLogService
				.findTaskFormKeys(instance.getId());

		// 生成展现用的数据
		Object subject;
		for (HistoricTaskInstance task : tasks) {
			// 任务的基本信息
			taskItem = new HashMap<String, Object>();
			taskItems.add(taskItem);
			taskItem.put("id", task.getId());// 任务id
			taskItem.put("assignee", getActorNameByCode(task.getAssignee()));// 办理人
			taskItem.put("owner", getActorNameByCode(task.getOwner()));// 委托人
			taskItem.put("link", false);// 链接标题
			subject = excutionLogService.getTaskVariableLocal(task.getId(),
					"subject");
			if (subject != null) {
				taskItem.put("subject", subject);// 标题
			} else {
				taskItem.put("subject", task.getName());// 标题
			}
			taskItem.put("hasButtons", false);// 有否操作按钮
			taskItem.put("formKey",
					excutionLogService.findTaskFormKey(task.getId()));// 记录formKey
			taskItem.put("desc", task.getDescription());// 任务描述说明
			taskItem.put("priority", task.getPriority());// 任务优先级

			// 任务的详细信息
			items = new ArrayList<Map<String, Object>>();// 二级条目列表
			taskItem.put("items", items);

			// -- 表单信息
			buildFormInfo(flowing, items, task.getProcessInstanceId(),
					task.getId(), formKeys.get(task.getId()), true);

			// -- 意见、附件信息
			buildFlowAttachsInfo(false, items,
					this.findTaskFlowAttachs(task.getId(), allFlowAttachs));

			// 任务的汇总信息
			taskItem.put(
					"wasteTime",
					"办理耗时："
							+ DateUtils.getWasteTimeCN(task.getStartTime(),
									task.getEndTime())
							+ " (从"
							+ DateUtils.formatDateTime2Minute(task
									.getStartTime())
							+ "到"
							+ DateUtils.formatDateTime2Minute(task.getEndTime())
							+ ")");
			taskItem.put("startTime",
					DateUtils.formatDateTime2Minute(task.getStartTime()));
			if (task.getDueDate() != null) {
				taskItem.put(
						"dueDate",
						"办理期限："
								+ DateUtils.formatDateTime2Minute(task
										.getDueDate()));
			}
		}

		// 返回
		return info;
	}
}