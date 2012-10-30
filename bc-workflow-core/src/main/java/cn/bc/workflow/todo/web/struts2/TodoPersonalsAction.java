package cn.bc.workflow.todo.web.struts2;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.Page;
import cn.bc.core.query.Query;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.ConditionUtils;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.InCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.identity.web.SystemContext;
import cn.bc.web.formater.AbstractFormater;
import cn.bc.web.formater.CalendarFormater;
import cn.bc.web.formater.EntityStatusFormater;
import cn.bc.web.struts2.ViewAction;
import cn.bc.web.ui.html.grid.Column;
import cn.bc.web.ui.html.grid.HiddenColumn4MapKey;
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.service.WorkflowService;
import cn.bc.workflow.todo.service.TodoService;

/**
 * 我的待办视图Action
 * 
 * @author wis
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class TodoPersonalsAction extends ViewAction<Map<String, Object>> {
	private static final long serialVersionUID = 1L;

	private TodoService todoService;
	private WorkflowService workflowService;

	@Autowired
	public void setTodoService(TodoService todoService) {
		this.todoService = todoService;
	}

	@Autowired
	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	public String status = String
			.valueOf(SuspensionState.ACTIVE.getStateCode());

	@Override
	public boolean isReadonly() {
		// 系统管理员
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.common"));
	}

	public boolean isDelegate() {
		// 任务委托角色
		SystemContext context = (SystemContext) this.getContext();
		return context.hasAnyRole(getText("key.role.bc.workflow.delegate"));
	}

	public boolean isAssign() {
		// 任务分派角色
		SystemContext context = (SystemContext) this.getContext();
		return context.hasAnyRole(getText("key.role.bc.workflow.assign"));
	}

	public boolean isStart() {
		// 可发起流程角色
		SystemContext context = (SystemContext) this.getContext();
		return context.hasAnyRole(getText("key.role.bc.workflow.start"));
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {

		SqlObject<Map<String, Object>> sqlObject = TodoPersonalsAction
				.getTodoPersonalData();

		return sqlObject;
	}

	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("art.create_time_", Direction.Desc);
	}

	@Override
	protected Condition getGridSpecalCondition() {
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		Condition statusCondition = null; // 状态
		Condition assigneeCondition = new EqualsCondition("art.assignee_",
				context.getUser().getCode()); // act_ru_task 任务表
		Condition userCondition = new EqualsCondition("ari.user_id_", context
				.getUser().getCode()); // act_ru_identitylink 参与成员表
		Condition ariTypeCondition = new EqualsCondition("ari.type_",
				"candidate"); // act_ru_identitylink 参与成员表了性
		Condition groupCondition = null;

		if (status != null && status.length() > 0) {
			String[] ss = status.split(",");
			if (ss[0].equals(String.valueOf(SuspensionState.ACTIVE
					.getStateCode()))) {//处理中
				statusCondition = new EqualsCondition("ae.suspension_state_",
						SuspensionState.ACTIVE.getStateCode());
			} else if (ss[0].equals(String.valueOf(SuspensionState.SUSPENDED
					.getStateCode()))) {//已暂停
				statusCondition = new EqualsCondition("ae.suspension_state_",
						SuspensionState.SUSPENDED.getStateCode());
			}
		}
		
		// 获取当前登录用户所在岗位的code列表
		List<String> list = context.getAttr(SystemContext.KEY_GROUPS);
		if (null != list && list.size() > 0) {
			groupCondition = new InCondition("ari.group_id_", list);
		}
		Condition assigneeIsNullCondition = new IsNullCondition("art.assignee_");

		// 当前用户是否等于任务待办人 或者 当前用户所在岗位是否等于任务的参与者 前提 该任务的待办人为空
		return ConditionUtils.mix2OrCondition(
				ConditionUtils.mix2AndCondition(statusCondition,
						assigneeCondition),
				ConditionUtils.mix2AndCondition(
						statusCondition,
						assigneeIsNullCondition,
						ariTypeCondition,
						ConditionUtils.mix2AndCondition(ConditionUtils
								.mix2OrCondition(userCondition, groupCondition)
								.setAddBracket(true))).setAddBracket(true));
	}

	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();

		if (this.isStart()) {
			// 发起流程
			tb.addButton(new ToolbarButton().setIcon("ui-icon-play")
					.setText(getText("flow.start"))
					.setClick("bc.todoView.startflow"));
		}

		tb.addButton(new ToolbarButton().setIcon("ui-icon-pencil")
				.setText(getText("label.sign.task"))
				.setClick("bc.todoView.signTask"));
		if (this.isDelegate()) {
			tb.addButton(new ToolbarButton().setIcon("ui-icon-person")
					.setText(getText("label.delegate.task"))
					.setClick("bc.todoView.delegateTask"));
		}
		if (this.isAssign()) {
			tb.addButton(new ToolbarButton().setIcon("ui-icon-flag")
					.setText(getText("label.assign.task"))
					.setClick("bc.todoView.assignTask"));
		}

		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(),
				"status", 0, getText("title.click2changeSearchStatus")));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}

	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(800).setMinWidth(200)
				.setHeight(400).setMinHeight(200);
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("art.id_", "id_"));

		// 状态
		columns.add(new TextColumn4MapKey("ae.suspension_state_", "status",
				getText("todo.stauts"), 50).setSortable(true).setValueFormater(
				new EntityStatusFormater(getStatus())));

		// 发送时间
		columns.add(new TextColumn4MapKey("art.create_time_", "create_time_",
				getText("todo.personal.createTime"), 120).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// 主题
		columns.add(new TextColumn4MapKey(
				"getProcessInstanceSubject(art.proc_inst_id_)", "subject",
				getText("flow.task.subject"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		// 名称
		columns.add(new TextColumn4MapKey("art.name_", "artName",
				getText("todo.personal.artName"), 200).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new AbstractFormater<String>() {

					@Override
					public String format(Object context, Object value) {
						@SuppressWarnings("unchecked")
						Map<String, Object> task = (Map<String, Object>) context;
						boolean flag = false;
						if (task.get("due_date_") != null) {// 办理时间是否过期
							Date d1 = (Date) task.get("due_date_");
							Date d2 = new Date();
							flag = d1.before(d2);
						}
						if (task.get("assignee_") == null) {// 岗位任务
							if (flag) {
								return "<div style=\"\"><span style=\"float: left;\" title=\"此任务已过期\" class=\"ui-icon ui-icon-clock\"></span>"
										+ "<span style=\"float: left;\" title=\"岗位任务\" class=\"ui-icon ui-icon-person\"></span>"
										+ "&nbsp;"
										+ "<span>"
										+ task.get("artName")
										+ "</span>"
										+ "</div>";
							} else {
								return "<div style=\"\"><span style=\"float: left;\" title=\"岗位任务\" class=\"ui-icon ui-icon-person\"></span>"
										+ "&nbsp;"
										+ "<span>"
										+ task.get("artName")
										+ "</span>"
										+ "</div>";
							}
						} else {// 个人任务
							if (flag) {
								return "<div style=\"\"><span style=\"float: left;\" title=\"此任务已过期\" class=\"ui-icon ui-icon-clock\"></span>"
										+ "&nbsp;"
										+ "<span>"
										+ task.get("artName")
										+ "</span>"
										+ "</div>";
							} else {
								return (String) task.get("artName");
							}
						}
					}

				}));
		// 办理期限
		columns.add(new TextColumn4MapKey("art.due_date_", "due_date_",
				getText("todo.personal.dueDate"), 120).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// 附加说明
		columns.add(new TextColumn4MapKey("art.description_", "description_",
				getText("todo.personal.description"))
				.setUseTitleFromLabel(true));
		// 流程
		columns.add(new TextColumn4MapKey("arpName", "arpName",
				getText("flow.task.category"), 180).setSortable(true)
				.setUseTitleFromLabel(true));
		// // 发送人
		// columns.add(new TextColumn4MapKey("aiuName", "aiuName",
		// getText("todo.personal.aiuName"), 60).setSortable(true));

		columns.add(new HiddenColumn4MapKey("procInstId", "procInstId"));
		columns.add(new HiddenColumn4MapKey("assignee", "assignee_"));
		columns.add(new HiddenColumn4MapKey("isCandidate", "isCandidate"));// 是否岗位任务
		columns.add(new HiddenColumn4MapKey("groupIds", "groupIds"));// 候选岗位列表

		return columns;
	}

	@Override
	protected List<Map<String, Object>> findList() {
		return this.getQuery().condition(this.getGridCondition()).list();
	}

	@Override
	protected Page<Map<String, Object>> findPage() {
		return this.getQuery().page(this.getPage().getPageNo(),
				this.getPage().getPageSize());
	}

	@Override
	protected Query<Map<String, Object>> getQuery() {
		return this.todoService.createSqlQuery(getSqlObject());
	}

	@Override
	protected String getFormActionName() {
		return "personal";
	}

	@Override
	protected String getHtmlPageNamespace() {
		return this.getContextPath() + "/bc-workflow/todo";
	}

	@Override
	protected String getGridRowLabelExpression() {
		return "['artName']";
	}

	@Override
	/** 获取表格双击行的js处理函数名 */
	protected String getGridDblRowMethod() {
		return "bc.todoView.open";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "art.name_", "art.description_", "art.assignee_",
				"arp.name_", "getProcessInstanceSubject(art.proc_inst_id_)" };
	}

	@Override
	protected String getHtmlPageJs() {
		return this.getContextPath() + "/bc-workflow/todo/view.js" + ","
				+ this.getContextPath() + "/bc/identity/identity.js" + ","
				+ this.getContextPath() + "/bc-workflow/select/selectUsers.js"
				+ "," + this.getContextPath()
				+ "/bc-workflow/historicprocessinstance/select.js";
	}

	@Override
	protected Json getGridExtrasData() {
		Json json = new Json();
		// 状态条件
		if (status != null && status.length() > 0)
			json.put("status", status);
		return json;
	}

	/**
	 * 状态值转换:处理中|已暂停|全部
	 * 
	 */
	private Map<String, String> getStatus() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put(String.valueOf(SuspensionState.ACTIVE.getStateCode()),
				getText("todo.status.processing"));
		map.put(String.valueOf(SuspensionState.SUSPENDED.getStateCode()),
				getText("todo.status.suspended"));
		map.put("", getText("bc.status.all"));
		return map;
	}

	private static SqlObject<Map<String, Object>> getTodoPersonalData() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();

		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		// 发送人问题未解决
		// sql.append("select art.id_,art.name_ artName,art.due_date_,aiu.first_ aiuName,art.create_time_,arp.name_ arpName");
		sql.append("select distinct art.id_,ae.suspension_state_ status,art.proc_inst_id_ procInstId,art.name_ artName,art.due_date_,art.create_time_,arp.name_ arpName,art.description_,art.assignee_");
		sql.append(",(case when (select count(*) from act_ru_task rt inner join act_ru_identitylink ri on rt.id_ = ri.task_id_ where rt.assignee_ is null) > 0 then TRUE else FALSE end) isCandidate");
		sql.append(",(select string_agg(ri2.group_id_,',') from act_ru_task rt2 inner join act_ru_identitylink ri2 on rt2.id_ = ri2.task_id_ where rt2.id_ = art.id_) groupIds");
		sql.append(",getProcessInstanceSubject(art.proc_inst_id_) as subject");
		sql.append(" from act_ru_task art");
		// sql.append(" left join act_id_user aiu on art.assignee_ = aiu.id_");
		sql.append(" left join act_re_procdef arp on art.proc_def_id_ = arp.id_");
		sql.append(" left join act_ru_identitylink ari on art.id_ = ari.task_id_");
		sql.append(" inner join act_ru_execution ae on art.execution_id_ = ae.id_");

		sqlObject.setSql(sql.toString());

		// 注入参数
		sqlObject.setArgs(null);

		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id_", rs[i++]);
				map.put("status", rs[i++]);
				map.put("procInstId", rs[i++]); // 流程实例id
				map.put("artName", rs[i++]); // 标题
				map.put("due_date_", rs[i++]); // 办理期限
				// map.put("aiuName", rs[i++]); // 发送人
				map.put("create_time_", rs[i++]); // 发送时间
				map.put("arpName", rs[i++]); // 分类
				map.put("description_", rs[i++]); // 附加说明
				map.put("assignee_", rs[i++]); // 任务处理人code
				map.put("isCandidate", rs[i++]); // 是否存在候选人或岗位
				map.put("groupIds", rs[i++]); // 候选岗位列表
				map.put("subject", rs[i++]); // 实例标题
				return map;
			}
		});
		return sqlObject;
	}

	public String json;
	private Long excludeId;

	public Long getExcludeId() {
		return excludeId;
	}

	public void setExcludeId(Long excludeId) {
		this.excludeId = excludeId;
	}

	/** 检查用户选择的任务是否已经签领 **/

	public String isSigned() {
		Json json = new Json();
		Long excludeId = this.todoService.checkIsSign(this.excludeId);
		if (excludeId != null) {// 已签领
			json.put("id", excludeId);
			json.put("signed", "true");
			json.put("msg", getText("todo.personal.msg.signed"));
		} else {// 未签领
			json.put("signed", "false");
		}
		this.json = json.toString();
		return "json";
	}

	/** 实现签领 **/
	public String claimTask() {
		// 查找当前登录用户条件
		// SystemContext context = (SystemContext) this.getContext();
		// this.todoService.doSignTask(this.excludeId,context.getUser().getCode());
		Json json = new Json();
		this.workflowService.claimTask(this.excludeId.toString());
		json.put("id", this.excludeId);
		json.put("msg", getText("todo.personal.msg.success"));

		this.json = json.toString();
		return "json";
	}

	// ==高级搜索代码开始==

	@Override
	protected boolean useAdvanceSearch() {
		return true;
	}

	@Override
	protected void initConditionsFrom() throws Exception {

	}
}
