package cn.bc.workflow.todo.web.struts2;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.Page;
import cn.bc.core.query.Query;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.InCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.OrCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.core.query.condition.impl.QlCondition;
import cn.bc.core.util.StringUtils;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.identity.web.SystemContext;
import cn.bc.option.domain.OptionItem;
import cn.bc.web.formater.AbstractFormater;
import cn.bc.web.formater.CalendarFormater;
import cn.bc.web.formater.EntityStatusFormater;
import cn.bc.web.ui.html.grid.Column;
import cn.bc.web.ui.html.grid.HiddenColumn4MapKey;
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;
import cn.bc.web.ui.html.toolbar.ToolbarMenuButton;
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.service.WorkflowService;
import cn.bc.workflow.todo.service.TodoService;
import cn.bc.workflow.web.struts2.ViewAction;

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

	public String status;

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
		return new OrderCondition("a.create_time_", Direction.Desc);
	}

	@Override
	protected Condition getGridSpecalCondition() {
		AndCondition ac=new AndCondition();
		
		//状态判断
		if (status != null && status.length() > 0) {
			String[] ss = status.split(",");
			if(ss.length==1){
				ac.add(new EqualsCondition("b.suspension_state_",Integer.valueOf(ss[0])));
			}else{
				ac.add(new InCondition("b.suspension_state_", StringUtils.stringArray2IntegerArray(ss)));
			}
		}
		
		//关联excution的流程实例
		ac.add(new IsNullCondition("b.parent_id_"));
		
		OrCondition or=new OrCondition();
		
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		//用户为任务办理人
		or.add(new EqualsCondition("a.assignee_",context.getUser().getCode()));
		
		// 获取当前登录用户所在岗位的code列表
		List<String> list = context.getAttr(SystemContext.KEY_GROUPS);
		String qlcondition="exists(select 1 from act_ru_identitylink c where c.task_id_ = a.id_ and ";
			qlcondition+="(c.user_id_ ='"+context.getUser().getCode()+"'";
			if (null != list && list.size() > 0) {
				qlcondition +=" or c.group_id_ in(";
				for(String g:list){
					qlcondition+="'"+g+"',";
				}
				qlcondition=qlcondition.substring(0, qlcondition.lastIndexOf(","));
				qlcondition+=")";
			}
			qlcondition+=")";
		qlcondition+=")";
		//用户是否有资格签领此任务
		or.add(new AndCondition(new IsNullCondition("a.assignee_"),new QlCondition(qlcondition)).setAddBracket(true));
		
		/*
		 * where b.suspension_state_ = 1 and b.parent_id_ is null and
			(a.assignee_ = 'xu' OR 
				(a.assignee_ is null  and 
					exists(select 1 from act_ru_identitylink c where c.task_id_ = a.id_ and (c.user_id_ = 'ru' or c.group_id_ in ('DriverRecruiter')))
				)
			)
		 * 
		 */
		ac.add(or.setAddBracket(true));
		
		return ac.isEmpty()?null:ac;
	}

	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();

		if (this.isStart()) {
			// 发起流程
			tb.addButton(new ToolbarButton().setIcon("ui-icon-play")
					.setText(getText("flow.start"))
					.setClick("bc.myTodoView.startflow"));
		}
		
		// "更多"按钮
		ToolbarMenuButton menuButton = new ToolbarMenuButton(
				getText("label.operate"))
				.setChange("bc.myTodoView.selectMenuButtonItem");
		tb.addButton(menuButton);

		menuButton.addMenuItem(getText("label.sign.task"),
				"signTask");
		
		if (this.isDelegate()) {
			menuButton.addMenuItem(getText("label.delegate.task"),
					"delegateTask");
		}
		if (this.isAssign()) {
			menuButton.addMenuItem(getText("label.assign.task"),
					"assignTask");
		}
		
		menuButton.addMenuItem(getText("flow.task.requirement"),
				"requirement");

		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(),
				"status", 2, getText("title.click2changeSearchStatus")));

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
		columns.add(new IdColumn4MapKey("a.id_", "id_"));

		// 状态
		columns.add(new TextColumn4MapKey("a.suspension_state_", "status",
				getText("todo.stauts"), 50).setSortable(true).setValueFormater(
				new EntityStatusFormater(getStatus())));

		// 发送时间
		columns.add(new TextColumn4MapKey("a.create_time_", "createTime",
				getText("todo.personal.createTime"), 120).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// 主题
		columns.add(new TextColumn4MapKey(
				"getProcessInstanceSubject(a.proc_inst_id_)", "subject",
				getText("flow.task.subject"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		// 名称
		columns.add(new TextColumn4MapKey("a.name_", "taskName",
				getText("todo.personal.artName"), 200).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new AbstractFormater<String>() {

					@Override
					public String format(Object context, Object value) {
						@SuppressWarnings("unchecked")
						Map<String, Object> task = (Map<String, Object>) context;
						boolean flag = false;
						if (task.get("dueDute") != null) {// 办理时间是否过期
							Date d1 = (Date) task.get("dueDute");
							Date d2 = new Date();
							flag = d1.before(d2);
						}
						if ("2".equals(task.get("type").toString())) {// 岗位任务
							if (flag) {
								return "<div style=\"\"><span style=\"float: left;\" title=\"此任务已过期\" class=\"ui-icon ui-icon-clock\"></span>"
										+ "<span style=\"float: left;\" title=\"岗位任务\" class=\"ui-icon ui-icon-person\"></span>"
										+ "&nbsp;"
										+ "<span>"
										+ task.get("taskName")
										+ "</span>"
										+ "</div>";
							} else {
								return "<div style=\"\"><span style=\"float: left;\" title=\"岗位任务\" class=\"ui-icon ui-icon-person\"></span>"
										+ "&nbsp;"
										+ "<span>"
										+ task.get("taskName")
										+ "</span>"
										+ "</div>";
							}
						} else {// 个人任务
							if (flag) {
								return "<div style=\"\"><span style=\"float: left;\" title=\"此任务已过期\" class=\"ui-icon ui-icon-clock\"></span>"
										+ "&nbsp;"
										+ "<span>"
										+ task.get("taskName")
										+ "</span>"
										+ "</div>";
							} else {
								return (String) task.get("taskName");
							}
						}
					}

				}));
		// 办理期限
		columns.add(new TextColumn4MapKey("art.due_date_", "dueDate",
				getText("todo.personal.dueDate"), 120).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// 附加说明
		columns.add(new TextColumn4MapKey("art.description_", "desc",
				getText("todo.personal.description"))
				.setUseTitleFromLabel(true));
		// 流程
		columns.add(new TextColumn4MapKey("processName", "processName",
				getText("flow.task.category"), 180).setSortable(true)
				.setUseTitleFromLabel(true));

		columns.add(new HiddenColumn4MapKey("assignee", "assignee"));
		columns.add(new HiddenColumn4MapKey("type", "type"));////任务的类型：1-个人任务，2-候选任务(包括岗位任务和候选人任务)
		
		//数据纠正需要用到的数据
		columns.add(new HiddenColumn4MapKey("procinstId", "procInstId"));
		columns.add(new HiddenColumn4MapKey("procinstName", "processName"));
		columns.add(new HiddenColumn4MapKey("procinstKey", "key"));
		columns.add(new HiddenColumn4MapKey("procinstTaskName", "taskName"));
		columns.add(new HiddenColumn4MapKey("procinstTaskKey", "taskKey"));
		columns.add(new HiddenColumn4MapKey("subject", "subject"));

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
		return "todo/personal";
	}

	@Override
	protected String getHtmlPageTitle() {
		return this.getText("personal.title");
	}

	@Override
	protected String getGridRowLabelExpression() {
		return "['taskName']";
	}

	@Override
	/** 获取表格双击行的js处理函数名 */
	protected String getGridDblRowMethod() {
		return "bc.myTodoView.open";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "a.name_", "a.description_", "a.assignee_",
				"d.name_", "getProcessInstanceSubject(a.proc_inst_id_)" };
	}

	@Override
	protected String getHtmlPageJs() {
		return this.getModuleContextPath() + "/todo/my/view.js" + ","
				+ this.getContextPath() + "/bc/identity/identity.js" + ","
				+ this.getModuleContextPath() + "/select/selectUsers.js"+ "," 
				+ this.getModuleContextPath()+ "/historicprocessinstance/select.js"+ "," 
				+ this.getModuleContextPath()+ "/historictaskinstance/view.js";
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
		
		sql.append("select a.id_,b.suspension_state_ as status,a.proc_inst_id_ as procinstid,a.name_ as taskname,a.due_date_ as duedate,a.create_time_ as createtime");
		sql.append(",d.name_ as processname,a.description_ as desc_,a.assignee_ as assignee,d.key_ as key,a.task_def_key_ as task_def_key");
		//任务的类型：1-个人任务，2-候选任务
		sql.append(",case when a.assignee_ is not null then 1 else 2 end as type_");
		//流程主题
		sql.append(",getprocessinstancesubject(a.proc_inst_id_) as subject");
		sql.append(" from act_ru_task a");
		sql.append(" inner join act_ru_execution b on b.proc_inst_id_ = a.proc_inst_id_");
		sql.append(" inner join act_re_procdef d on d.id_ = a.proc_def_id_");
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
				map.put("taskName", rs[i++]); // 任务名称
				map.put("dueDate", rs[i++]); // 办理期限
				map.put("createTime", rs[i++]); // 发送时间
				map.put("processName", rs[i++]); // 流程名称
				map.put("desc", rs[i++]); // 任务附加说明
				map.put("assignee", rs[i++]); // 任务处理人code
				map.put("key", rs[i++]); // 流程编码
				map.put("taskKey", rs[i++]); // 流程编码
				map.put("type", rs[i++]); // 任务的类型：1-个人任务，2-候选任务
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
	
	public JSONArray processNames;
	
	public JSONArray taskNames;

	@Override
	protected void initConditionsFrom() throws Exception {
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		String account=context.getUserHistory().getCode();
		// 获取当前登录用户所在岗位的code列表
		List<String> groupList = context.getAttr(SystemContext.KEY_GROUPS);
		
		List<String> values=this.todoService.findProcessNames(account, groupList);
		List<Map<String,String>> list = new ArrayList<Map<String,String>>();
		Map<String,String> map;
		for(String value : values){
			map = new HashMap<String, String>();
			map.put("key", value);
			map.put("value", value);
			list.add(map);
		}
		this.processNames=OptionItem.toLabelValues(list);
		
		values=this.todoService.findTaskNames(account, groupList);
		list = new ArrayList<Map<String,String>>();
		for(String value : values){
			map = new HashMap<String, String>();
			map.put("key", value);
			map.put("value", value);
			list.add(map);
		}
		this.taskNames=OptionItem.toLabelValues(list);
	}
}
