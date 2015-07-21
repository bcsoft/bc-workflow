/**
 *
 */
package cn.bc.workflow.service;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.JsonUtils;
import cn.bc.core.util.SerializeUtils;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * 工作空间 Service
 *
 * @author dragon
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {
	private static final Logger logger = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private WorkflowFormService workflowFormService;

	@Autowired
	private WorkflowService workflowService;

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getProcessInstanceDetail(String processInstanceId) {
		Assert.hasLength(processInstanceId);
		Date start = new Date();

		// 获取流程实例明细
		String sql = "select wf__find_process_instance_detail(?)";
		logger.debug("sql={}", sql);
		String jsonString;
		try {
			jsonString = this.jdbcTemplate.queryForObject(sql, String.class, processInstanceId);
		} catch (EmptyResultDataAccessException e) {
			throw new CoreException("could not find process instance by id=" + processInstanceId);
		}
		logger.debug("json={}", jsonString);
		if (logger.isInfoEnabled()) logger.info("获取实例明细耗时 {}", DateUtils.getWasteTime(start));

		// 转换成 Map
		Map<String, Object> map = JsonUtils.toMap(jsonString);
		if (logger.isInfoEnabled()) logger.info("转换成 Map 耗时 {}", DateUtils.getWasteTime(start));
		//JsonObject map2 = Json.createReader(new StringReader(jsonString)).readObject();
		//if(logger.isInfoEnabled()) logger.info("转换成 JsonObject 耗时 {}", DateUtils.getWasteTime(start));

		// 处理 bytea 字段
		this.dealByteArrayData(map);
		if (logger.isInfoEnabled()) logger.info("处理 bytea 字段耗时 {}", DateUtils.getWasteTime(start));

		// 转换 variables 数据结构
		this.convertVariables(map);
		if (logger.isInfoEnabled()) logger.info("转换 variables 数据结构耗时 {}", DateUtils.getWasteTime(start));

		if (logger.isInfoEnabled()) logger.info("总耗时 {}", DateUtils.getWasteTime(start));
		return map;
	}

	/**
	 * 转换 variables 数据结构
	 * <p>从数组 [{name:【name】,type:【type】,value:【value】}, ...] 转换为 Map {【name】: 【value】} 结构 </p>
	 *
	 * @param instanceDetail 实例明细
	 */
	private void convertVariables(Map<String, Object> instanceDetail) {
		// 全局流程变量
		Object[] variables = (Object[]) instanceDetail.get("variables");
		Map<String, Object> newVars = this.convertVariables(variables);
		instanceDetail.put("variables", newVars);
		if (newVars != null) {
			// ==== 流程标题
			String subject = (String) newVars.get("subject");
			if (subject != null && !subject.isEmpty()) instanceDetail.put("subject", subject);
			else instanceDetail.put("subject", ((Map<String, Object>) instanceDetail.get("definition")).get("name"));
			// ==== 流程流水号
			String code = (String) newVars.get("wf_code");
			if (code != null && !code.isEmpty()) instanceDetail.put("code", code);
			else instanceDetail.put("code", null);
			// ==== 流程表单key
			String formKey = (String) newVars.get("formKey");
			if (formKey != null && !formKey.isEmpty()) instanceDetail.put("form_key", formKey);
		}

		// 待办流程变量
		convertVariables4Tasks((Object[]) instanceDetail.get("todo_tasks"));
		// 经办流程变量
		convertVariables4Tasks((Object[]) instanceDetail.get("done_tasks"));
	}

	/**
	 * 转换任务的流程变量结构
	 *
	 * @param tasks 任务集
	 */
	private void convertVariables4Tasks(Object[] tasks) {
		if (tasks == null || tasks.length == 0) return;

		Map<String, Object> task, newVars;
		String subject, formKey;
		for (Object t : tasks) {
			task = (Map<String, Object>) t;
			newVars = convertVariables((Object[]) task.get("variables"));
			task.put("variables", newVars);

			if (newVars != null) {
				// ==== 任务标题
				subject = (String) newVars.get("subject");
				if (subject != null && !subject.isEmpty()) task.put("subject", subject);
				else task.put("subject", task.get("name"));
				// ==== 任务表单key
				formKey = (String) newVars.get("formKey");
				if (formKey != null && !formKey.isEmpty()) task.put("form_key", formKey);
				// ==== 是否隐藏表单附件
				task.put("hideAttach", newVars.containsKey("hideAttach") ? (Boolean) newVars.get("hideAttach") : false);
				// ==== 是否为空表单
				task.put("emptyForm", newVars.containsKey("emptyForm") ? (Boolean) newVars.get("emptyForm") : false);
			}else{
				task.put("subject", task.get("name"));
				task.put("hideAttach", false);
				task.put("emptyForm", false);
			}
		}
	}

	/**
	 * 转换 variables 数据结构
	 * <p>从数组 [{name:【name】,type:【type】,value:【value】}, ...] 转换为 Map {【name】: 【value】} 结构 </p>
	 *
	 * @param variables 流程变量原始数组
	 */
	private Map<String, Object> convertVariables(Object[] variables) {
		if (variables == null) return null;

		Map<String, Object> newVars = new LinkedHashMap<>();
		Map<String, Object> var;
		for (Object v : variables) {
			var = (Map<String, Object>) v;
			newVars.put((String) var.get("name"), convertVariableValue((String) var.get("type"), var.get("value")));

			// 转换特殊类型的变量的值
			for (Map.Entry<String, Object> e : newVars.entrySet()) {
				convertSpecialKeyValue(e);
			}
		}
		return newVars;
	}

	private Object convertVariableValue(String type, Object value) {
		if(value == null) return null;
		Object newValue;
		if ("string".equals(type)) {
			newValue = value;
		} else if ("boolean".equals(type)) {
			newValue = new Boolean((String) value);
		} else if ("integer".equals(type)) {
			newValue = new Integer((String) value);
		} else if ("long".equals(type)) {
			newValue = new Long((String) value);
		} else if ("double".equals(type)) {
			newValue = new Double((String) value);
		} else if ("date".equals(type)) {
			newValue = new Timestamp(DateUtils.getDate((String) value).getTime());
		} else {
			newValue = value;
		}
		return newValue;
	}

	/**
	 * 特殊流程变量值的转换：当key使用list_、map_、array_前缀时，值进行特殊处理
	 *
	 * @param e 流程变量
	 */
	private void convertSpecialKeyValue(Map.Entry<String, Object> e) {
		if (e.getKey().startsWith("list_") && e.getValue() instanceof String) {// 将字符串转化为List
			e.setValue(JsonUtils.toCollection((String) e.getValue()));
		} else if (e.getKey().startsWith("map_") && e.getValue() instanceof String) {// 将字符串转化为Map
			e.setValue(JsonUtils.toMap((String) e.getValue()));
		} else if (e.getKey().startsWith("array_") && e.getValue() instanceof String) {// 将字符串转化为数组
			e.setValue(JsonUtils.toArray((String) e.getValue()));
		}
	}

	/**
	 * 处理流程实例的二进制流程变量数据，将二进制数据反序列化为相应的 Java 对象
	 *
	 * @param instanceDetail 流程实例明细数据
	 */
	private void dealByteArrayData(Map<String, Object> instanceDetail) {
		Object[] ids = (Object[]) instanceDetail.get("bytearray_ids");
		if (ids == null || ids.length == 0) return;

		// 获取流程变量的二进制数据: [{id, bytes}, ...]
		String sql = "select id_ id, bytes_ bytes from act_ge_bytearray where id_";
		if (ids.length == 1) {
			sql += " = '" + ids[0] + "'";
		} else {
			sql += " in ('" + ids[0] + "'";
			for (int i = 1; i < ids.length; i++) {
				sql += ", '" + ids[i] + "'";
			}
			sql += ")";
		}
		if (logger.isDebugEnabled()) {
			logger.debug("args={}, sql={}", org.springframework.util.StringUtils.arrayToCommaDelimitedString(ids), sql);
		}
		Map<String, Object> id_bytes = this.jdbcTemplate.query(sql, new ResultSetExtractor<Map<String, Object>>() {
			@Override
			public Map<String, Object> extractData(ResultSet rs) throws SQLException, DataAccessException {
				Map<String, Object> m = new LinkedHashMap<>();
				while (rs.next()) {
					m.put((String) rs.getObject(1), rs.getBytes("bytes"));
				}
				return m;
			}
		});
		logger.debug("id_bytes={}", id_bytes);

		// 将二进制流程变量数据反序列化为原始 Java 对象后，重新设置回流程变量中
		// ==== 全局流程变量
		logger.info("deserialize global variables");
		deserializeByteArrayVariable(id_bytes, (Object[]) instanceDetail.get("variables"));
		// ==== 待办流程变量
		logger.info("deserialize todo task variables");
		deserializeByteArrayVariable4Tasks(id_bytes, (Object[]) instanceDetail.get("todo_tasks"));
		// ==== 经办流程变量
		logger.info("deserialize done task variables");
		deserializeByteArrayVariable4Tasks(id_bytes, (Object[]) instanceDetail.get("done_tasks"));
	}

	/**
	 * 处理任务中二进制流程变量值的反序列化
	 *
	 * @param id_bytes 二进制值集 key=【id】, value=【bytes[]】
	 * @param tasks    任务列表
	 */
	private void deserializeByteArrayVariable4Tasks(Map<String, Object> id_bytes, Object[] tasks) {
		if (tasks == null || tasks.length == 0)
			return;

		Map<String, Object> task;
		for (Object t : tasks) {
			task = (Map<String, Object>) t;
			deserializeByteArrayVariable(id_bytes, (Object[]) task.get("variables"));
		}
	}

	/**
	 * 处理二进制流程变量值的反序列化
	 *
	 * @param id_bytes  二进制值集 key=【id】, value=【bytes[]】
	 * @param variables 流程变量列表
	 */
	private void deserializeByteArrayVariable(Map<String, Object> id_bytes, Object[] variables) {
		if (variables == null || variables.length == 0)
			return;

		Map<String, Object> m;
		byte[] bytes;
		Object obj;
		for (Object v : variables) {
			m = (Map<String, Object>) v;
			if ("serializable".equals(m.get("type"))) {
				logger.info("deserialize {} for {}", m.get("value"), m.get("name"));
				bytes = (byte[]) id_bytes.get(m.get("value"));    // id对应的二进制值
				obj = SerializeUtils.deserialize(bytes);        // 将二进制值反序列化为原始 Java 对象
				m.put("value", obj);
				if (logger.isDebugEnabled())
					logger.debug("	obj.class={}, obj={}", obj != null ? obj.getClass() : "null"
							, obj instanceof Collection ? StringUtils.collectionToCommaDelimitedString((Collection) obj) : obj);
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getWorkspaceData(String processInstanceId) {
		Date start = new Date();

		// 获取实例的标准格式数据
		Map<String, Object> instance = this.getProcessInstanceDetail(processInstanceId);
		logger.debug("instance={}", instance);

		// 转换为原有工作空间需要的数据结构

		// ==== 流程实例
		Map<String, Object> ws = new LinkedHashMap<>();
		ws.put("id", instance.get("id"));
		Map<String, Object> definition = (Map<String, Object>) instance.get("definition");
		ws.put("businessKey", definition.get("key"));
		ws.put("deleteReason", instance.get("delete_reason"));
		ws.put("startUser", ((Map<String, Object>) instance.get("start_user")).get("name"));
		ws.put("startTime", instance.get("start_time"));
		ws.put("endTime", instance.get("end_time"));
		ws.put("duration", instance.get("duration"));
		ws.put("flowStatus", instance.get("status")); // 状态：1-流转中、2-暂停、3-已结束
		ws.put("subject", instance.get("subject"));// 实例标题
		ws.put("code", instance.get("code"));// 流水号

		// ==== 流程定义
		ws.put("definitionId", definition.get("id"));
		ws.put("definitionName", definition.get("name"));
		ws.put("definitionCategory", definition.get("category"));
		ws.put("definitionKey", definition.get("key"));
		ws.put("definitionVersion", definition.get("version"));
		ws.put("definitionResourceName", "");// definition.get("resource_name"));
		ws.put("definitionDiagramResourceName", "");// definition.get("diagram_resource_name"));
		// ==== 流程发布
		ws.put("deploymentId", definition.get("deployment_id"));

		// ==== 公共信息处理
		ws.put("commonInfo", buildWSCommonInfo(instance));
		if (logger.isInfoEnabled()) logger.info("公共信息耗时 {}", DateUtils.getWasteTime(start));

		// 待办信息处理
		ws.put("todoInfo", buildWSTodoInfo(instance));
		if (logger.isInfoEnabled()) logger.info("待办信息耗时 {}", DateUtils.getWasteTime(start));

		// 经办信息处理
		ws.put("doneInfo", buildWSDoneInfo(instance));
		if (logger.isInfoEnabled()) logger.info("经办信息耗时 {}", DateUtils.getWasteTime(start));

		// 子流程信息
		ws.put("subProcessInfo", new org.json.JSONArray(this.workflowService.findSubProcessInstanceInfoById(processInstanceId)).toString());

		// 返回综合后的信息
		if (logger.isInfoEnabled()) logger.info("总耗时 {}", DateUtils.getWasteTime(start));
		return ws;
	}

	/**
	 * 构建工作空间公共信息
	 *
	 * @param instance 流程实例明细
	 */
	private Map<String, Object> buildWSCommonInfo(Map<String, Object> instance) {
		int flowStatus = (int) instance.get("status");
		Map<String, Object> info = new LinkedHashMap<>();

		// ==== 判断激活、暂停按钮的显示权限 ==== 开始
		// 实例流转中,当前处理人是否拥有暂停权限或流程管理员才显示
		boolean isShowSuspendedButton = false;
		// 实例已暂停,当前处理人是否拥有激活权限或流程管理员才显示
		boolean isShowActiveButton = false;
		SystemContext context = SystemContextHolder.get();
		if (context.hasAnyRole("BC_WORKFLOW")) {    // 流程管理员有权限
			isShowSuspendedButton = true;
			isShowActiveButton = true;
		} else {
			String userCode = context.getUser().getCode(); // 当前用户账号
			// 待办任务
			Object[] todo_tasks = (Object[]) instance.get("todo_tasks");
			if (todo_tasks != null) {
				Map<String, Object> task;
				for (Object t : todo_tasks) {
					task = (Map<String, Object>) t;
					// 当前用户是任务的待办人时，按流程变量的值控制权限
					if (isTaskActor(task, userCode)) {
						// 暂停权限
						Object value = getTaskVariableValue(task, "suspended");
						if (value != null) isShowSuspendedButton = (Boolean) value;

						// 激活权限
						value = getTaskVariableValue(task, "active");
						if (value != null) isShowActiveButton = (Boolean) value;
					}
				}
			}
		}
		// ==== 判断激活、暂停按钮的显示权限 ==== 结束

		// 读取隐藏按钮控制参数
		Map<String, Object> global_variables = (Map<String, Object>) instance.get("variables");
		String hiddenButtonCodes = global_variables != null ? (String) global_variables.get("hiddenButtonCodes") : null;
		info.put("buttons", this.buildHeaderDefaultButtons(flowStatus, "common", false, isShowSuspendedButton, isShowActiveButton, hiddenButtonCodes));
		info.put("hasButtons", info.get("buttons") != null);// 有否操作按钮

		// 一级条目列表
		List<Map<String, Object>> items = new ArrayList<>();
		info.put("items", items);
		Map<String, Object> item;
		List<String> detail;
		String type;

		// 构建表单条目 TODO

		// 构建意见附件条目
		List<Map<String, Object>> attachItems = buildFlowAttachsInfo((Object[]) instance.get("attachs"), flowStatus);
		if (attachItems != null) items.addAll(attachItems);

		// 统计信息条目
		item = new HashMap<>();
		items.add(item);
		type = "stat";
		item.put("id", instance.get("id"));
		item.put("type", type);// 信息类型
		item.put("iconClass", "ui-icon-flag");// 左侧显示的小图标
		item.put("subject", "统计信息");// 标题
		item.put("link", true);// 非链接标题
		item.put("hasButtons", false);// 无操作按钮
		detail = new ArrayList<>();
		item.put("detail", detail);// 详细信息
		String start_time = (String) instance.get("start_time");
		detail.add("发起时间：" + ((Map<String, Object>) instance.get("start_user")).get("name") + " " + start_time);
		if (flowStatus == WorkspaceService.FLOWSTATUS_ACTIVE) {
			detail.add("结束时间：" + "仍在流转中...");
			detail.add("办理耗时：" + DateUtils.getWasteTimeCN(DateUtils.getDate(start_time)));
		} else if (flowStatus == WorkspaceService.FLOWSTATUS_SUSPENDED) {
			detail.add("结束时间：" + "流程已暂停...");
			detail.add("办理耗时：" + DateUtils.getWasteTimeCN(DateUtils.getDate(start_time)));
		} else if (flowStatus == WorkspaceService.FLOWSTATUS_COMPLETE) {
			String end_time = (String) instance.get("end_time");
			detail.add("结束时间：" + DateUtils.formatDateTime2Minute(DateUtils.getDate(end_time)));
			detail.add("办理耗时：" + DateUtils.getWasteTimeCN(DateUtils.getDate(start_time), DateUtils.getDate(end_time)));
		}
		Map<String, Object> deploy = (Map<String, Object>) instance.get("deploy");
		detail.add("流程版本：" + deploy.get("name") + deploy.get("version") + " (" + deploy.get("key") + ")");

		// 返回
		return info;
	}

	/**
	 * 获取任务指定流程变量的值
	 *
	 * @param task         任务
	 * @param variableName 变量名称
	 * @return
	 */
	private Object getTaskVariableValue(Map<String, Object> task, String variableName) {
		Map<String, Object> vars = (Map<String, Object>) task.get("variables");
		return vars != null ? vars.get(variableName) : null;
	}

	/**
	 * 判断指定的用户是否是该任务的办理人
	 *
	 * @param task     任务
	 * @param userCode 用户账号
	 * @return
	 */
	private boolean isTaskActor(Map<String, Object> task, String userCode) {
		return userCode.equals(((Map<String, Object>) task.get("actor")).get("code"));
	}

	private final static String ITEM_BUTTON_OPEN = "<span class='itemOperate open'><span class='ui-icon ui-icon-document-b'></span><span class='text link'>查看</span></span>";
	private final static String ITEM_BUTTON_EDIT = "<span class='itemOperate edit'><span class='ui-icon ui-icon-pencil'></span><span class='text link'>编辑</span></span>";
	private final static String ITEM_BUTTON_DELETE = "<span class='itemOperate delete'><span class='ui-icon ui-icon-closethick'></span><span class='text link'>删除</span></span>";
	private final static String ITEM_BUTTON_DOWNLOAD = "<span class='itemOperate download'><span class='ui-icon ui-icon-arrowthickstop-1-s'></span><span class='text link'>下载</span></span>";
	private final static String ITEM_BUTTON_ADDCOMMENT = "<span class='mainOperate addComment'><span class='ui-icon ui-icon-document'></span><span class='text link'>添加意见</span></span>";
	private final static String ITEM_BUTTON_ADDATTACH = "<span class='mainOperate addAttach'><span class='ui-icon ui-icon-arrowthick-1-n'></span><span class='text link'>添加附件</span></span>";
	private final static String ITEM_BUTTON_SHOWDIAGRAM = "<span class='mainOperate flowImage'><span class='ui-icon ui-icon-image'></span><span class='text link'>查看流程图</span></span>";
	private final static String ITEM_BUTTON_SHOWLOG = "<span class='mainOperate excutionLog'><span class='ui-icon ui-icon-tag' title='查看流转日志'></span></span>";
	private final static String ITEM_BUTTON_ACTIVE = "<span class='mainOperate active'><span class='ui-icon ui-icon-play'></span><span class='text link'>激活流程</span></span>";
	private final static String ITEM_BUTTON_SUSPENDED = "<span class='mainOperate suspended'><span class='ui-icon ui-icon-pause'></span><span class='text link'>暂停流程</span></span>";

	/**
	 * 创建默认的公共信息(common)、个人待办信息(todo_user)、岗位待办信息(todo_group)区标题右侧的操作按钮
	 *
	 * @param flowStatus            是否流转中
	 * @param type                  类型
	 * @param isMyTask              是否是我的个人或岗位待办
	 * @param isShowSuspendedButton 是否显示暂停按钮
	 * @param isShowActiveButton    是否显示激活按钮
	 * @return
	 */
	private String buildHeaderDefaultButtons(int flowStatus, String type, boolean isMyTask, boolean isShowSuspendedButton
			, boolean isShowActiveButton, String hiddenButtonCodes) {
		if (hiddenButtonCodes == null) hiddenButtonCodes = "";
		SystemContext context = SystemContextHolder.get();
		StringBuffer buttons = new StringBuffer();
		if ("common".equals(type)) {
			if (flowStatus == WorkspaceService.FLOWSTATUS_ACTIVE) {
				if (isShowSuspendedButton) {// 实例流转中,当前处理人是否拥有暂停权限或流程管理员才显示
					buttons.append(ITEM_BUTTON_SUSPENDED);// 暂停按钮
				}
			}
			if (flowStatus == WorkspaceService.FLOWSTATUS_SUSPENDED) {
				if (isShowActiveButton) {// 实例已暂停,当前处理人是否拥有激活权限或流程管理员才显示
					buttons.append(ITEM_BUTTON_ACTIVE);// 激活按钮
				}
			}

			buttons.append(ITEM_BUTTON_SHOWDIAGRAM);// 查看流程图
			if (flowStatus == WorkspaceService.FLOWSTATUS_ACTIVE
					&& context.hasAnyRole("BC_WORKFLOW_ADDGLOBALATTACH")) {// 有权限才能添加全局意见附件
				if (hiddenButtonCodes.indexOf("BUTTON_ADDCOMMENT") == -1)
					buttons.append(ITEM_BUTTON_ADDCOMMENT);// 添加意见

				if (hiddenButtonCodes.indexOf("BUTTON_ADDATTACH") == -1)
					buttons.append(ITEM_BUTTON_ADDATTACH);// 添加附件
			}
			buttons.append(ITEM_BUTTON_SHOWLOG);// 查看流转日志
		} else if ("todo_user".equals(type)) {
			if (flowStatus == WorkspaceService.FLOWSTATUS_ACTIVE && isMyTask) {
				if (context.hasAnyRole("BC_WORKFLOW_DELEGATE"))// 有权限才能委派任务
					buttons.append("<span class='mainOperate delegate'><span class='ui-icon ui-icon-person'></span><span class='text link'>委托任务</span></span>");

				if (hiddenButtonCodes.indexOf("BUTTON_ADDCOMMENT") == -1)
					buttons.append(ITEM_BUTTON_ADDCOMMENT);// 添加意见

				if (hiddenButtonCodes.indexOf("BUTTON_ADDATTACH") == -1)
					buttons.append(ITEM_BUTTON_ADDATTACH);// 添加附件
				buttons.append("<span class='mainOperate finish'><span class='ui-icon ui-icon-check'></span><span class='text link'>完成办理</span></span>");
			}
		} else if ("todo_group".equals(type)) {
			if (flowStatus == WorkspaceService.FLOWSTATUS_ACTIVE) {
				if (context.hasAnyRole("BC_WORKFLOW_ASSIGN"))// 有权限才能分派任务
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
	 * @param instance 流程实例明细
	 */
	private Map<String, Object> buildWSTodoInfo(Map<String, Object> instance) {
		int flowStatus = (int) instance.get("status");
		Map<String, Object> info = new LinkedHashMap<>();
		List<Map<String, Object>> taskItems = new ArrayList<>();// 一级条目列表
		info.put("tasks", taskItems);

		// 待办任务列表
		Object[] tasks = (Object[]) instance.get("todo_tasks");
		if (tasks == null || tasks.length == 0) return info;

		Map<String, Object> taskItem;
		List<Map<String, Object>> items;// 详细信息：表单、意见、附件
		boolean isUserTask;// 是否是个人待办:true-个人待办、false-组待办
		boolean isMyTask;// 是否是我的个人或组待办

		// 生成展现用的数据
		Map<String, Object> task, formItem;
		List<Map<String, Object>> attachItems;
		Map<String, Object> actor, master;// 待办人或待办岗, 委托人
		Map<String, Object> local_variables;
		Date now = new Date();
		String subject;
		SystemContext context = SystemContextHolder.get();
		String userCode = context.getUser().getCode(); // 当前用户账号
		List<String> userGroups = context.getAttr(SystemContext.KEY_GROUPS); // 当前用户所在的所有岗位
		for (Object t : tasks) {
			task = (Map<String, Object>) t;
			task.put("process_instance", instance);// 方便在任务内也可以访问流程实例的信息
			actor = (Map<String, Object>) task.get("actor");
			// 判断任务类型
			if ((boolean) actor.get("candidate") == false) {    // 个人待办
				isUserTask = true;    // 个人待办
				isMyTask = userCode.equals(actor.get("code"));    // 我的待办
			} else {    // 岗位待办
				isUserTask = false;    // 岗位待办

				// 判断待办岗位是否是当前用户所在的岗位
				isMyTask = userGroups != null ? userGroups.contains(actor.get("code")) : false;
			}

			// 任务的基本信息
			taskItem = new HashMap<>();
			if (isMyTask) {
				taskItems.add(0, taskItem);// 当前用户的待办放在最前
			} else {
				taskItems.add(taskItem);
			}
			taskItem.put("id", task.get("id"));// 任务id
			taskItem.put("isUserTask", isUserTask);// 是否是个人待办:true-个人待办、false-岗位待办
			taskItem.put("isMyTask", isMyTask);// 是否是我的个人或组待办
			subject = (String) getTaskVariableValue(task, "subject");
			if (subject != null && subject.length() > 0) {
				taskItem.put("subject", subject);// 标题
			} else {
				taskItem.put("subject", task.get("name"));// 任务名称作为标题
			}

			// 读取隐藏按钮控制参数
			local_variables = (Map<String, Object>) task.get("variables");
			String hiddenButtonCodes = local_variables != null ? (String) local_variables.get("hiddenButtonCodes") : null;
			taskItem.put("buttons", this.buildHeaderDefaultButtons(flowStatus, isUserTask ? "todo_user" : "todo_group",
					isMyTask, false, false, hiddenButtonCodes));
			taskItem.put("hasButtons", taskItem.get("buttons") != null);// 有否操作按钮
			taskItem.put("formKey", getTaskVariableValue(task, "formKey"));// 记录formKey
			taskItem.put("desc", task.get("description"));// 任务描述说明
			taskItem.put("priority", task.get("priority"));// 任务优先级

			// 任务的详细信息
			items = new ArrayList<>();// 二级条目列表
			taskItem.put("items", items);

			// -- 表单信息
			if(isMyTask && isUserTask) {// 我的个人待办时方渲染表单
				formItem = buildTaskFormInfo(task, !(isUserTask && isMyTask));
				if (formItem != null) items.add(formItem);
			}

			// -- 意见、附件信息
			if(!(boolean) task.get("hideAttach")) {
				attachItems = buildFlowAttachsInfo((Object[]) task.get("attachs"), flowStatus);
				if (attachItems != null) items.addAll(attachItems);
			}

			// 任务的汇总信息
			if (isUserTask) {
				// 执行委托操作的人
				master = (Map<String, Object>) task.get("master");
				taskItem.put("master", master != null ? master.get("name") : "");
				taskItem.put("actor", "待办人：" + actor.get("name") + (master != null ? " (受" + master.get("name") + "委托)" : ""));
			} else {
				String pname = (String) actor.get("pname");
				taskItem.put("actor", "待办岗：" + (pname == null ? actor.get("name") : pname + "/" + actor.get("name")));
			}
			String start_time = ((String) task.get("start_time")).substring(0, 16);// 精确到分钟
			taskItem.put("createTime", "发起时间：" + start_time);
			if (task.get("due_date") != null) {
				taskItem.put("dueDate", "办理期限：" + ((String) task.get("due_date")).substring(0, 16));// 精确到分钟
			}
			taskItem.put("wasteTime", "办理耗时：" + DateUtils.getWasteTimeCN(DateUtils.getDate((String) task.get("start_time")), now)
					+ " (从" + start_time + "到" + DateUtils.formatDateTime2Minute(now) + ")");
		}

		// 返回
		return info;
	}

	/**
	 * 构建工作空间已办信息
	 *
	 * @param instance 流程实例明细
	 */
	private Map<String, Object> buildWSDoneInfo(Map<String, Object> instance) {
		Map<String, Object> info = new LinkedHashMap<>();
		List<Map<String, Object>> taskItems = new ArrayList<>();// 一级条目列表
		info.put("tasks", taskItems);
		Map<String, Object> taskItem;
		List<Map<String, Object>> items;// 详细信息：表单、意见、附件

		// 经办任务列表
		Object[] tasks = (Object[]) instance.get("done_tasks");
		if (tasks == null || tasks.length == 0) return info;

		// 生成展现用的数据
		Map<String, Object> task, formItem;
		List<Map<String, Object>> attachItems;
		Map<String, Object> actor, master, origin_actor;// 经办人, 委托人, 原办理人
		for (Object t : tasks) {
			task = (Map<String, Object>) t;
			task.put("process_instance", instance);// 方便在任务内也可以访问流程实例的信息
			actor = (Map<String, Object>) task.get("actor");
			// 任务的基本信息
			taskItem = new HashMap<>();
			taskItems.add(taskItem);
			taskItem.put("key", task.get("key"));// 任务的Key
			taskItem.put("orderNo", task.get("key"));// 使用任务的Key作为业务排序号
			taskItem.put("id", task.get("id"));// 任务id
			taskItem.put("actor", actor.get("name"));// 办理人

			// 执行委托操作的人
			master = (Map<String, Object>) task.get("master");
			if (master != null) {
				taskItem.put("master", master.get("name"));
				origin_actor = (Map<String, Object>) task.get("origin_actor");
				if (origin_actor != null) {
					taskItem.put("originActor", origin_actor.get("name"));
				} else {
					taskItem.put("originActor", null);
				}
			} else {
				taskItem.put("master", null);
			}

			taskItem.put("link", false);// 链接标题
			taskItem.put("name", task.get("name"));// 名称
			taskItem.put("subject", task.get("subject"));// 标题
			taskItem.put("hasButtons", false);// 有否操作按钮
			taskItem.put("formKey", task.get("form_key"));// 任务的表单Key
			taskItem.put("desc", task.get("description"));// 任务描述说明
			taskItem.put("priority", task.get("priority"));// 任务优先级

			// 任务的详细信息
			items = new ArrayList<>();// 二级条目列表
			taskItem.put("items", items);

			// -- 表单信息
			formItem = buildTaskFormInfo(task, true);
			if (formItem != null) items.add(formItem);

			// -- 意见、附件信息
			if (!(boolean) task.get("hideAttach")) {
				attachItems = buildFlowAttachsInfo((Object[]) task.get("attachs"), WorkspaceService.FLOWSTATUS_COMPLETE);
				if (attachItems != null) items.addAll(attachItems);
			}

			// 任务的汇总信息
			String start_time = ((String) task.get("start_time")).substring(0, 16);// 精确到分钟
			String end_time = ((String) task.get("end_time")).substring(0, 16);// 精确到分钟
			taskItem.put("startTime", task.get("start_time"));// 任务创建时间
			taskItem.put("endTime", task.get("end_time"));// 任务完成时间
			taskItem.put("startTime2m", start_time);// 任务创建时间
			taskItem.put("endTime2m", end_time);// 任务完成时间
			taskItem.put("wasteTime", "办理耗时："
					+ DateUtils.getWasteTimeCN(DateUtils.getDate((String) task.get("start_time")),
					DateUtils.getDate((String) task.get("end_time")))
					+ " (从" + start_time + "到" + end_time + ") - " + task.get("key"));
			if (task.get("due_date") != null) {
				taskItem.put("dueDate", "办理期限：" + ((String) task.get("due_date")).substring(0, 16));// 精确到分钟
			}
		}

		// 返回
		return info;
	}

	// 获取任务渲染区的数据
	private Map<String, Object> buildTaskFormInfo(Map<String, Object> task, boolean readonly) {
		// 判断是否需要渲染表单
		if(task.containsKey("emptyForm") && (boolean) task.get("emptyForm")) return null;

		Map<String, Object> instance = (Map<String, Object>) task.get("process_instance");
		int flowStatus = (int) instance.get("status");
		String taskId = (String) task.get("id");
		String formKey = (String) task.get("form_key");
		if (formKey == null || formKey.length() == 0) return null;
		if (logger.isDebugEnabled()) {
			logger.debug("taskId=" + taskId + ",formKey=" + formKey);
		}

		// 表单基本信息
		Map<String, Object> item;
		List<String> detail;
		String type = "form";
		item = new HashMap<>();
		item.put("id", taskId);
		item.put("pid", instance.get("id"));// 流程实例id
		item.put("tid", taskId);// 任务id
		item.put("type", type);// 信息类型
		item.put("link", false);// 链接标题
		item.put("buttons", this.buildItemDefaultButtons(flowStatus, type));// 操作按钮列表
		item.put("hasButtons", item.get("buttons") != null);// 有否有操作按钮
		item.put("iconClass", "ui-icon-document");// 左侧显示的小图标
		item.put("subject", "完成任务前需要你处理如下信息：");// 标题信息

		// 表单html
		detail = new ArrayList<>();
		item.put("detail", detail);
		// detail.add("[表单信息]");

		// 渲染任务的表单
		String html;
		try {
			html = this.workflowFormService.getRenderedTaskForm(task, readonly);
		} catch (Exception e) {
			if(logger.isErrorEnabled()) {
				String code = SystemContextHolder.get() != null ? SystemContextHolder.get().getUser().getCode() : "";
				logger.error("任务表单格式化异常：taskId={}, taskName={}, userCode={}", taskId, task.get("name"), code);
				logger.error(e.getMessage(), e);
			}
			html = "错误：任务表单格式化异常，请联系管理员修正！(id=" + taskId + ")";
		}
		item.put("form_html", html);

		detail.add(html);

		return item;
	}

	/**
	 * 创建默认的表单(form)、意见(comment)、附件(attach)操作按钮
	 *
	 * @param flowStatus 流转状态:1、2、3
	 * @param type       类型: attach|form|comment
	 * @return
	 */
	private String buildItemDefaultButtons(int flowStatus, String type) {
		StringBuffer buttons = new StringBuffer();
		if (flowStatus == WorkspaceService.FLOWSTATUS_ACTIVE) {
			buttons.append(ITEM_BUTTON_EDIT);
		}
		buttons.append(ITEM_BUTTON_OPEN);
		if ("attach".equals(type)) {
			buttons.append(ITEM_BUTTON_DOWNLOAD);
		}
		if (flowStatus == WorkspaceService.FLOWSTATUS_ACTIVE && !"form".equals(type)) {
			buttons.append(ITEM_BUTTON_DELETE);
		}
		return buttons.length() > 0 ? buttons.toString() : null;
	}


	/**
	 * 构建附件的显示信息
	 *
	 * @param flowStatus 流转状态
	 * @param attachs 附件信息
	 * @return
	 */
	private List<Map<String, Object>> buildFlowAttachsInfo(Object[] attachs, int flowStatus) {
		if(attachs == null) return null;
		List<Map<String, Object>> attachItems = new ArrayList<>();
		Map<String, Object> item;
		List<String> detail;
		String itemType;
		int attachType;
		Map<String, Object> attach;
		for (Object a : attachs) {
			attach = (Map<String, Object>) a;
			item = new HashMap<>();
			item.put("id", attach.get("id"));
			item.put("pid", attach.get("pid"));// 流程实例id
			item.put("tid", attach.get("tid"));// 任务id
			attachType = (int) attach.get("type");
			if (FlowAttach.TYPE_ATTACHMENT == attachType) {
				itemType = "attach";
				item.put("iconClass", "ui-icon-link");// 左侧显示的小图标
				item.put("subject", attach.get("subject"));// 附件名称
				item.put("size", attach.get("size"));// 附件大小
				item.put("sizeInfo", cn.bc.core.util.StringUtils.formatSize((int) attach.get("size")));// 附件大小的描述
				item.put("path", attach.get("path"));// 附件相对路径
			} else if (FlowAttach.TYPE_COMMENT == attachType) {
				itemType = "comment";
				item.put("iconClass", "ui-icon-comment");// 左侧显示的小图标
				item.put("subject", attach.get("subject"));// 意见标题
				item.put("desc", attach.get("description"));// 意见内容
			} else if (FlowAttach.TYPE_TEMP_ATTACHMENT == attachType) {
				continue;// 临时附件，将作为子流程的附件，不渲染
			} else {
				logger.warn("不支持的 FlowAttach 类型:id={}, type={}", attach.get("id"), attachType);
				itemType = "none";
				item.put("iconClass", "ui-icon-lock");// 左侧显示的小图标
				item.put("subject", "(未知类型)");
			}
			item.put("type", itemType);// 信息类型
			item.put("link", true);// 链接标题
			item.put("buttons", this.buildItemDefaultButtons(flowStatus, itemType));// 操作按钮列表
			item.put("hasButtons", item.get("buttons") != null);// 有否操作按钮

			// 详细信息
			detail = new ArrayList<>();
			item.put("detail", detail);
			detail.add(((Map<String, Object>) attach.get("author")).get("name") + " " + ((String) attach.get("file_date")).substring(0, 16)); // 创建信息

			attachItems.add(item);
		}
		return attachItems;
	}
}