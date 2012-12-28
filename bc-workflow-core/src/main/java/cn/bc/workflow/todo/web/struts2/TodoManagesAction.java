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

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.identity.web.SystemContext;
import cn.bc.option.domain.OptionItem;
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
import cn.bc.workflow.service.WorkspaceServiceImpl;
import cn.bc.workflow.todo.service.TodoService;

/**
 * 任务监控视图Action
 * 
 * @author wis
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class TodoManagesAction extends ViewAction<Map<String, Object>>{
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean isReadonly() {
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.admin"),getText("key.role.bc.workflow"));
	}
	
	public boolean isDelegate() {
		// 任务委托角色
		SystemContext context = (SystemContext) this.getContext();
		return context.hasAnyRole(getText("key.role.bc.workflow"),getText("key.role.bc.workflow.delegate"));
	}
	
	public boolean isAssign() {
		// 任务分派角色
		SystemContext context = (SystemContext) this.getContext();
		return context.hasAnyRole(getText("key.role.bc.workflow"),getText("key.role.bc.workflow.delegate"));
	}
	
	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("a.create_time_",Direction.Desc);
	}
	
	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select a.id_,b.suspension_state_ as status,a.proc_inst_id_ as procinstid,a.name_ as taskname,a.due_date_ as duedate,a.create_time_ as createtime");
		sql.append(",d.name_ as processname,a.description_ as desc_,a.assignee_ as assignee,c.name as aname");
		sql.append(",case when a.assignee_ is not null then 1 else 2 end as type_");
		sql.append(",getprocessinstancesubject(a.proc_inst_id_) as subject");
					//候选人
		sql.append(",(select string_agg(g2.name,',') from act_ru_identitylink t2 inner join bc_identity_actor g2 on g2.code=t2.user_id_ where t2.task_id_= a.id_) as users");
					//候选岗位
		sql.append(",(select string_agg(g1.name,',') from act_ru_identitylink t1 inner join bc_identity_actor g1 on g1.code=t1.group_id_ where t1.task_id_= a.id_) as groups");
		sql.append(" from act_ru_task a");
		sql.append(" inner join act_ru_execution b on b.proc_inst_id_ = a.proc_inst_id_");
		sql.append(" inner join act_re_procdef d on d.id_ = a.proc_def_id_");
		sql.append(" left join bc_identity_actor c on c.code=a.assignee_");
		
		sqlObject.setSql(sql.toString());
		
		// 注入参数
		sqlObject.setArgs(null);
		
		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id", rs[i++]);
				map.put("status", rs[i++]);
				map.put("procInstId", rs[i++]); //流程实例id
				map.put("taskName", rs[i++]); // 标题
				map.put("dueDate", rs[i++]); // 办理期限
				map.put("createTime", rs[i++]); //  发送时间
				map.put("processName", rs[i++]); //  分类
				map.put("desc", rs[i++]); // 附加说明
				map.put("assignee", rs[i++]); //  任务处理人code
				map.put("aname", rs[i++]); //  办理人
				map.put("type", rs[i++]); //  办理人
				map.put("subject", rs[i++]); 
				map.put("users", rs[i++]); //  候选人员列表
				map.put("groups", rs[i++]); //  候选岗位列表
				return map;
			}
		});
		return sqlObject;
	}
	
	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();
		
		ac.add(new IsNullCondition("b.parent_id_"));

		return ac.isEmpty() ? null : ac;
	}
	
	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		
		// 查看按钮
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.todoView.open"));
		
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

		// 搜索按钮
		tb.addButton(getDefaultSearchToolbarButton());
		
		return tb;
	}
	
	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(800).setMinWidth(200)
				.setHeight(400).setMinHeight(200);
	}
	
	/**
	 * 流程状态值转换:流转中|已暂停|已结束|全部
	 * 
	 */
	private Map<String, String> getStatus() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put(String.valueOf(SuspensionState.ACTIVE.getStateCode()),
				getText("flow.instance.status.processing"));
		map.put(String.valueOf(SuspensionState.SUSPENDED.getStateCode()),
				getText("flow.instance.status.suspended"));
		map.put(String.valueOf(WorkspaceServiceImpl.COMPLETE),
				getText("flow.instance.status.finished"));
		map.put("", getText("bc.status.all"));
		return map;
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("a.id_", "id"));
		
		//流程状态
		columns.add(new TextColumn4MapKey("b.suspension_state_", "status",
				getText("flow.task.pstatus"), 80).setSortable(true)
				.setValueFormater(new EntityStatusFormater(getStatus())));

		// 主题
		columns.add(new TextColumn4MapKey(
				"getProcessInstanceSubject(a.proc_inst_id_)", "subject",
				getText("flow.task.subject"), 200).setSortable(true)
				.setUseTitleFromLabel(true));

		columns.add(new TextColumn4MapKey("a.name_", "taskName",
				getText("todo.personal.artName"), 250).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new AbstractFormater<String>() {

					@Override
					public String format(Object context, Object value) {
						@SuppressWarnings("unchecked")
						Map<String, Object> task = (Map<String, Object>) context;
						boolean flag = false;
						if(task.get("dueDate") != null){//办理时间是否过期
							Date d1 = (Date) task.get("dueDate");
							Date d2 = new Date();
							flag = d1.before(d2);
						}
						if("2".equals(task.get("type").toString())){//岗位任务
							if(flag){
								return "<div style=\"\"><span style=\"float: left;\" title=\"此任务已过期\" class=\"ui-icon ui-icon-clock\"></span>" +
										"<span style=\"float: left;\" title=\"岗位任务\" class=\"ui-icon ui-icon-person\"></span>"
										+"&nbsp;"+"<span>"+task.get("taskName")+"</span>"+"</div>";
							}else{
								return "<div style=\"\"><span style=\"float: left;\" title=\"岗位任务\" class=\"ui-icon ui-icon-person\"></span>"
										+"&nbsp;"+"<span>"+task.get("taskName")+"</span>"+"</div>";
							}
						}else{//个人任务
							if(flag){
								return "<div style=\"\"><span style=\"float: left;\" title=\"此任务已过期\" class=\"ui-icon ui-icon-clock\"></span>"
										+"&nbsp;"+"<span>"+task.get("taskName")+"</span>"+"</div>";
							}else{
								return (String) task.get("taskName");
							}
						}
					}
					
				}));
		// 办理人
		columns.add(new TextColumn4MapKey("c.name", "aname",
				getText("todo.personal.assignee"), 60).setUseTitleFromLabel(true));
		// 办理期限
		columns.add(new TextColumn4MapKey("art.due_date_", "dueDate",
				getText("todo.personal.dueDate"), 140).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		// 创建时间
		columns.add(new TextColumn4MapKey("art.create_time_", "createTime",
				getText("todo.personal.createTime"), 140).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		// 候选岗位
		columns.add(new TextColumn4MapKey("groups", "groups",
				getText("todo.personal.groupIds"), 150).setUseTitleFromLabel(true));
		// 候选人
		columns.add(new TextColumn4MapKey("users", "users",
				getText("todo.personal.userIds"), 100).setUseTitleFromLabel(true));
		// 分类
		columns.add(new TextColumn4MapKey("d.name_", "processName",
				getText("todo.personal.arpName")).setSortable(true).setUseTitleFromLabel(true));
		columns.add(new HiddenColumn4MapKey("procInstId", "procInstId"));
		columns.add(new HiddenColumn4MapKey("type", "type"));
		return columns;
	}

	@Override
	protected String getFormActionName() {
		return "manage";
	}
	
	@Override
	protected String getHtmlPageNamespace(){
		return this.getContextPath() + "/bc-workflow/todo";
	}

	@Override
	protected String getGridRowLabelExpression() {
		return "['taskName']";
	}
	
	@Override
	/** 获取表格双击行的js处理函数名 */
	protected String getGridDblRowMethod() {
		return "bc.todoView.open";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String [] {"d.name_","a.name_","a.description_","a.assignee_","c.name","getProcessInstanceSubject(a.proc_inst_id_)"};
	}
	
	@Override
	protected String getHtmlPageJs() {
		return this.getContextPath() + "/bc-workflow/todo/view.js" + ","
				+ this.getContextPath() + "/bc/identity/identity.js" + ","
				+ this.getContextPath() + "/bc-workflow/select/selectUsers.js";
	}
	
	private TodoService todoService;

	@Autowired
	public void setTodoService(TodoService todoService) {
		this.todoService = todoService;
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
		List<String> values=this.todoService.findProcessNames();
		List<Map<String,String>> list = new ArrayList<Map<String,String>>();
		Map<String,String> map;
		for(String value : values){
			map = new HashMap<String, String>();
			map.put("key", value);
			map.put("value", value);
			list.add(map);
		}
		this.processNames=OptionItem.toLabelValues(list);
		
		
		values=this.todoService.findTaskNames();
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
