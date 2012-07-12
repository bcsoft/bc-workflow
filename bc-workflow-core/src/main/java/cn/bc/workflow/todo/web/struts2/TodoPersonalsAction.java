package cn.bc.workflow.todo.web.struts2;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class TodoPersonalsAction extends ViewAction<Map<String, Object>>{
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

	@Override
	public boolean isReadonly() {
		// 系统管理员
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.common"));
	}
	
	public boolean isDelegate() {
		// 任务委托角色
		SystemContext context = (SystemContext) this.getContext();
		return context
				.hasAnyRole(getText("key.role.bc.workflow.delegate"));
	}

	public boolean isAssign() {
		// 任务分派角色
		SystemContext context = (SystemContext) this.getContext();
		return context
				.hasAnyRole(getText("key.role.bc.workflow.assign"));
	}

	
	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		
		SqlObject<Map<String, Object>> sqlObject = TodoPersonalsAction.getTodoPersonalData();

		return sqlObject;
	}
	
	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("art.create_time_",Direction.Desc);
	}
	
	@Override
	protected Condition getGridSpecalCondition() {
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		Condition userCondition = new EqualsCondition("art.assignee_",context.getUser().getCode()); 
		
		Condition groupCondition = null;
		//获取当前登录用户所在岗位的code列表
		List<String> list = context.getAttr(SystemContext.KEY_GROUPS);
		if(null != list && list.size() > 0){
			groupCondition = new InCondition("ari.group_id_",list);
		}else{
			groupCondition = new EqualsCondition("ari.group_id_","");
		}
		Condition userIsNullCondition = new IsNullCondition("art.assignee_");
		
		//当前用户是否等于任务待办人 或者 当前用户所在岗位是否等于任务的参与者 前提 该任务的待办人为空
		return ConditionUtils.mix2OrCondition(userCondition,
				ConditionUtils.mix2AndCondition(groupCondition,userIsNullCondition).setAddBracket(true)).setAddBracket(true);
	}
	
	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		
		tb.addButton(new ToolbarButton().setIcon("ui-icon-pencil")
				.setText(getText("label.sign.task"))
				.setClick("bc.todoView.signTask"));
		if(this.isDelegate()){
			tb.addButton(new ToolbarButton().setIcon("ui-icon-person")
					.setText(getText("label.delegate.task"))
					.setClick("bc.todoView.delegateTask"));
		}
		if(this.isAssign()){
			tb.addButton(new ToolbarButton().setIcon("ui-icon-flag")
					.setText(getText("label.assign.task"))
					.setClick("bc.todoView.assignTask"));
		}
		
		// 搜索按钮
		tb.addButton(getDefaultSearchToolbarButton());
		
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
		// 标题
		columns.add(new TextColumn4MapKey("art.name_", "artName",
				getText("todo.personal.artName"), 250).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new AbstractFormater<String>() {

					@Override
					public String format(Object context, Object value) {
						@SuppressWarnings("unchecked")
						Map<String, Object> task = (Map<String, Object>) context;
						boolean flag = false;
						if(task.get("due_date_") != null){//办理时间是否过期
							Date d1 = (Date) task.get("due_date_");
							Date d2 = new Date();
							flag = d1.before(d2);
						}
						if(task.get("assignee_") == null){//岗位任务
							if(flag){
								return "<div style=\"\"><span style=\"float: left;\" title=\"此任务已过期\" class=\"ui-icon ui-icon-clock\"></span>" +
										"<span style=\"float: left;\" title=\"岗位任务\" class=\"ui-icon ui-icon-person\"></span>"
										+"&nbsp;"+"<span>"+task.get("artName")+"</span>"+"</div>";
							}else{
								return "<div style=\"\"><span style=\"float: left;\" title=\"岗位任务\" class=\"ui-icon ui-icon-person\"></span>"
										+"&nbsp;"+"<span>"+task.get("artName")+"</span>"+"</div>";
							}
						}else{//个人任务
							if(flag){
								return "<div style=\"\"><span style=\"float: left;\" title=\"此任务已过期\" class=\"ui-icon ui-icon-clock\"></span>"
										+"&nbsp;"+"<span>"+task.get("artName")+"</span>"+"</div>";
							}else{
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
				getText("todo.personal.description"), 150).setUseTitleFromLabel(true));
//		// 发送人
//		columns.add(new TextColumn4MapKey("aiuName", "aiuName",
//				getText("todo.personal.aiuName"), 60).setSortable(true));
		// 发送时间
		columns.add(new TextColumn4MapKey("art.create_time_", "create_time_",
				getText("todo.personal.createTime"), 120).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// 分类
		columns.add(new TextColumn4MapKey("arpName", "arpName",
				getText("todo.personal.arpName"), 70).setSortable(true));
		columns.add(new HiddenColumn4MapKey("procInstId", "procInstId"));
		columns.add(new HiddenColumn4MapKey("assignee", "assignee_"));

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
	protected String getHtmlPageNamespace(){
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
		return new String [] {"art.name_","art.description_","art.assignee_","arp.name_"};
	}
	
	@Override
	protected String getHtmlPageJs() {
		return this.getContextPath() + "/bc-workflow/todo/view.js"+","+
				this.getContextPath() + "/bc/identity/identity.js"
				;
	}
	
	private static SqlObject<Map<String, Object>> getTodoPersonalData (){
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String,Object>>();
		
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		//发送人问题未解决
		//sql.append("select art.id_,art.name_ artName,art.due_date_,aiu.first_ aiuName,art.create_time_,arp.name_ arpName");
		sql.append("select art.id_,art.proc_inst_id_ procInstId,art.name_ artName,art.due_date_,art.create_time_,arp.name_ arpName");
		sql.append(",art.description_,ari.group_id_,ari.user_id_,art.assignee_");
		sql.append(" from act_ru_task art");
		sql.append(" left join act_id_user aiu on art.assignee_ = aiu.id_");
		sql.append(" left join act_re_procdef arp on art.proc_def_id_ = arp.id_");
		sql.append(" left join act_ru_identitylink ari on art.id_ = ari.task_id_");
		
		sqlObject.setSql(sql.toString());
		
		// 注入参数
		sqlObject.setArgs(null);
		
		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id_", rs[i++]);
				map.put("procInstId", rs[i++]); //流程实例id
				map.put("artName", rs[i++]); // 标题
				map.put("due_date_", rs[i++]); // 办理期限
				//map.put("aiuName", rs[i++]); // 发送人
				map.put("create_time_", rs[i++]); //  发送时间
				map.put("arpName", rs[i++]); //  分类
				map.put("description_", rs[i++]); // 附加说明
				map.put("group_id_", rs[i++]); //  参与者岗位code
				map.put("user_id_", rs[i++]); //  参与人岗位code
				map.put("assignee_", rs[i++]); //  任务处理人code
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

	/** 检查用户选择的任务是否已经签领  **/
	
	public String isSigned() {
		Json json = new Json();
		Long excludeId = this.todoService.checkIsSign(this.excludeId);
		if(excludeId != null){//已签领
			json.put("id", excludeId);
			json.put("signed", "true");
			json.put("msg", getText("todo.personal.msg.signed"));
		}else{//未签领
			json.put("signed", "false");
		}
		this.json = json.toString();
		return "json";
	}
	
	/** 实现签领 **/
	public String claimTask(){
		// 查找当前登录用户条件
		//SystemContext context = (SystemContext) this.getContext();
		//this.todoService.doSignTask(this.excludeId,context.getUser().getCode());
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
