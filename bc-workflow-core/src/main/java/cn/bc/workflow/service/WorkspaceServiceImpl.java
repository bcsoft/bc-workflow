/**
 * 
 */
package cn.bc.workflow.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cn.bc.core.util.DateUtils;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.template.service.TemplateService;

/**
 * 工作流Service的实现
 * 
 * @author dragon
 */
public class WorkspaceServiceImpl implements WorkspaceService {
	private static final Log logger = LogFactory
			.getLog(WorkspaceServiceImpl.class);
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
		ws.put("startUser", instance.getStartUserId());
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
		info.put("buttons", this.buildHeaderDefaultButtons(flowing, "common"));// 操作按钮列表
		info.put("hasButtons", info.get("buttons") != null);// 有否操作按钮
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();// 一级条目列表
		info.put("items", items);
		Map<String, Object> item;
		List<String> detail;
		String type;

		// 构建表单条目
		item = new HashMap<String, Object>();
		items.add(item);
		type = "form";
		item.put("id", "id");// TODO
		item.put("type", type);// 信息类型
		item.put("iconClass", "ui-icon-document");// 左侧显示的小图标
		item.put("link", true);// 链接标题
		item.put("subject", "表单：测试表单");// 标题 TODO
		item.put("buttons", this.buildItemDefaultButtons(flowing, type));// 操作按钮列表
		item.put("hasButtons", item.get("buttons") != null);// 有否操作按钮
		detail = new ArrayList<String>();
		item.put("detail", detail);// 详细信息
		detail.add("小明 " + " " + "2012-01-01 00:00"); // 创建信息 TODO

		// 构建附件条目
		List<Attachment> attachments;// 附件列表
		if (flowing) {
			attachments = taskService.getProcessInstanceAttachments(instance
					.getId());
		} else {
			// TODO 从历史中获取
			attachments = null;
		}
		System.out.println("attachments=" + attachments);
		item = new HashMap<String, Object>();
		items.add(item);
		type = "attach";
		item.put("id", "id");// TODO
		item.put("type", type);// 信息类型
		item.put("iconClass", "ui-icon-link");// 左侧显示的小图标
		item.put("link", true);// 链接标题
		item.put("subject", "附件：测试附件");// 标题 TODO
		item.put("buttons", this.buildItemDefaultButtons(flowing, type));// 操作按钮列表
		item.put("hasButtons", item.get("buttons") != null);// 有否操作按钮
		detail = new ArrayList<String>();
		item.put("detail", detail);// 详细信息
		detail.add("小明 " + " " + "2012-01-01 00:00"); // 创建信息 TODO

		// 构建意见条目
		List<Comment> comments;// 意见列表
		if (flowing) {
			comments = taskService.getProcessInstanceComments(instance.getId());
		} else {
			// TODO 从历史中获取
			comments = null;
		}
		System.out.println("comments=" + comments);
		item = new HashMap<String, Object>();
		items.add(item);
		type = "comment";
		item.put("id", "id");// TODO
		item.put("type", type);// 信息类型
		item.put("iconClass", "ui-icon-comment");// 左侧显示的小图标
		item.put("link", true);// 链接标题
		item.put("subject", "意见：测试意见");// 标题 TODO
		item.put("buttons", this.buildItemDefaultButtons(flowing, type));// 操作按钮列表
		item.put("hasButtons", item.get("buttons") != null);// 有否操作按钮
		detail = new ArrayList<String>();
		item.put("detail", detail);// 详细信息
		detail.add("小明 " + " " + "2012-01-01 00:00"); // 创建信息 TODO

		// 构建统计信息条目
		item = new HashMap<String, Object>();
		items.add(item);
		type = "stat";
		item.put("id", "");// TODO
		item.put("type", type);// 信息类型
		item.put("iconClass", "ui-icon-flag");// 左侧显示的小图标
		item.put("subject", "统计信息");// 标题
		item.put("link", false);// 非链接标题
		item.put("hasButtons", false);// 无操作按钮
		detail = new ArrayList<String>();
		item.put("detail", detail);// 详细信息
		detail.add("发起时间：" + instance.getStartUserId() + " "
				+ DateUtils.formatDateTime2Minute(instance.getStartTime()));
		detail.add("结束时间："
				+ (flowing ? "仍在流转中..." : DateUtils
						.formatDateTime2Minute(instance.getEndTime())));
		detail.add("办理耗时："
				+ (flowing ? DateUtils.getWasteTimeCN(instance.getStartTime())
						: DateUtils.getWasteTimeCN(instance.getStartTime(),
								instance.getEndTime())));
		detail.add("参与人数：" + "");// TODO

		// 返回
		return info;
	}

	/**
	 * 创建默认的表单(form)、意见(comment)、附件(attach)操作按钮
	 * 
	 * @param flowing
	 *            是否流转中
	 * @param type
	 *            类型
	 * @return
	 */
	private String buildItemDefaultButtons(boolean flowing, String type) {
		StringBuffer buttons = new StringBuffer();
		if (flowing) {
			buttons.append("<span class='itemOperate edit'><span class='ui-icon ui-icon-pencil'></span><span class='text link'>编辑</span></span>");
		} else {
			buttons.append("<span class='itemOperate open'><span class='ui-icon ui-icon-document-b'></span><span class='text link'>查看</span></span>");
		}
		buttons.append("<span class='itemOperate download'><span class='ui-icon ui-icon-arrowthickstop-1-s'></span><span class='text link'>下载</span></span>");
		if (flowing) {
			buttons.append("<span class='itemOperate delete'><span class='ui-icon ui-icon-closethick'></span><span class='text link'>删除</span></span>");
		}
		return buttons.length() > 0 ? buttons.toString() : null;
	}

	/**
	 * 创建默认的公共信息(common)、个人待办信息(todo_user)、岗位待办信息(todo_group)区标题右侧的操作按钮
	 * 
	 * @param flowing
	 *            是否流转中
	 * @param type
	 *            类型
	 * @return
	 */
	private String buildHeaderDefaultButtons(boolean flowing, String type) {
		StringBuffer buttons = new StringBuffer();
		if ("common".equals(type)) {
			buttons.append("<span class='mainOperate flowImage'><span class='ui-icon ui-icon-image'></span><span class='text link'>查看流程图</span></span>");
			if (flowing) {
				buttons.append("<span class='mainOperate addComment'><span class='ui-icon ui-icon-document'></span><span class='text link'>添加意见</span></span>");
				buttons.append("<span class='mainOperate addAttach'><span class='ui-icon ui-icon-arrowthick-1-n'></span><span class='text link'>添加附件</span></span>");
			}
		} else if ("todo_user".equals(type)) {
			if (flowing) {
				buttons.append("<span class='mainOperate addComment'><span class='ui-icon ui-icon-document'></span><span class='text link'>添加意见</span></span>");
				buttons.append("<span class='mainOperate addAttach'><span class='ui-icon ui-icon-arrowthick-1-n'></span><span class='text link'>添加附件</span></span>");
				buttons.append("<span class='mainOperate delegate'><span class='ui-icon ui-icon-person'></span><span class='text link'>委派任务</span></span>");
				buttons.append("<span class='mainOperate finish'><span class='ui-icon ui-icon-check'></span><span class='text link'>完成办理</span></span>");
			}
		} else if ("todo_group".equals(type)) {
			if (flowing) {
				buttons.append("<span class='mainOperate assign'><span class='ui-icon ui-icon-person'></span><span class='text link'>分派任务</span></span>");
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
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();// 一级条目列表
		info.put("items", items);
		Map<String, Object> item;
		List<Map<String, Object>> detail;// 详细信息：表单、意见、附件
		boolean isUserTask;// 是否是个人待办:true-个人待办、false-组待办
		boolean isMyTask;// 是否是我的个人或组待办

		// 获取待办列表
		List<Task> tasks = this.taskService.createTaskQuery()
				.processInstanceId(instance.getId()).orderByTaskCreateTime()
				.asc().list();
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
				isMyTask = judgeIsMyTask(identityLinks);
			}

			// 任务的基本信息
			item = new HashMap<String, Object>();
			items.add(item);
			item.put("id", task.getId());// 任务id
			item.put("isUserTask", isUserTask);// 是否是个人待办:true-个人待办、false-组待办
			item.put("isMyTask", isMyTask);// 是否是我的个人或组待办
			item.put("link", true);// 链接标题
			item.put("subject", task.getName());// 标题
			item.put("buttons", this.buildHeaderDefaultButtons(flowing,
					isUserTask ? "todo_user" : "todo_group"));// 操作按钮列表
			item.put("hasButtons", item.get("buttons") != null);// 有否操作按钮

			// 任务的详细信息
			// -- 创建信息
			if (isUserTask) {
				item.put("actor", "待办人：" + task.getAssignee());
			} else {
				item.put("actor", "待办岗：" + identityLinks.get(0).getGroupId());// TODO
			}
			item.put(
					"createTime",
					"发起时间："
							+ DateUtils.formatDateTime2Minute(task
									.getCreateTime()));
			if (task.getDueDate() != null) {
				item.put(
						"dueDate",
						"办理期限："
								+ DateUtils.formatDateTime2Minute(task
										.getDueDate()));
			}

			// -- 表单、附件、意见信息
			detail = new ArrayList<Map<String, Object>>();// 二级条目列表
			item.put("detail", detail);
			// TODO
		}

		// 返回
		return info;
	}

	/**
	 * 判断当前用户是否与IdentityLink有关
	 * 
	 * @param identityLinks
	 * @return
	 */
	private boolean judgeIsMyTask(List<IdentityLink> identityLinks) {
		// TODO
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
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();// 一级条目列表
		info.put("items", items);
		Map<String, Object> item;
		List<Map<String, Object>> detail;// 详细信息：表单、意见、附件

		// 获取待办列表
		List<HistoricTaskInstance> tasks = this.historyService
				.createHistoricTaskInstanceQuery()
				.processInstanceId(instance.getId())
				.taskDeleteReason("completed")
				.orderByHistoricActivityInstanceStartTime().asc().list();
		for (HistoricTaskInstance task : tasks) {
			// 任务的基本信息
			item = new HashMap<String, Object>();
			items.add(item);
			item.put("id", task.getId());// 任务id
			item.put("assignee", task.getAssignee());// 办理人
			item.put("owner", task.getOwner());// 委托人
			item.put("link", false);// 链接标题
			item.put("subject", task.getName());// 标题
			item.put(
					"wasteTime",
					"办理耗时："
							+ DateUtils.getWasteTimeCN(instance.getStartTime(),
									instance.getEndTime())
							+ " (从"
							+ DateUtils.formatDateTime2Minute(task
									.getStartTime())
							+ "到"
							+ DateUtils.formatDateTime2Minute(task.getEndTime())
							+ ")");
			item.put("startTime",
					DateUtils.formatDateTime2Minute(task.getStartTime()));
			if (task.getDueDate() != null) {
				item.put(
						"dueDate",
						"办理期限："
								+ DateUtils.formatDateTime2Minute(task
										.getDueDate()));
			}

			// -- 表单、附件、意见信息
			detail = new ArrayList<Map<String, Object>>();// 二级条目列表
			item.put("detail", detail);
			// TODO
		}

		// 返回
		return info;
	}
}