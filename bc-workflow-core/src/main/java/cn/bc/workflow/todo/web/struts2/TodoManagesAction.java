package cn.bc.workflow.todo.web.struts2;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

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

/**
 * 我的待办视图Action
 * 
 * @author wis
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class TodoManagesAction extends ViewAction<Map<String, Object>>{
	private static final long serialVersionUID = 1L;
	
//	private IdentityService identityService; //用户信息service
//	private TaskService taskService; //待办任务service
//	private HistoryService historyService; //历史信息service
//	
//	@Autowired
//	public void setIdentityService(IdentityService identityService) {
//		this.identityService = identityService;
//	}
//
//	@Autowired
//	public void setTaskService(TaskService taskService) {
//		this.taskService = taskService;
//	}
//	
//	@Autowired
//	public void setHistoryService(HistoryService historyService) {
//		this.historyService = historyService;
//	}

	@Override
	public boolean isReadonly() {
		// 通用角色
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.admin"));
	}
	
	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select distinct art.id_,art.proc_inst_id_ procInstId,art.name_ artName,art.due_date_,art.create_time_,arp.name_ arpName,art.description_,aiu.first_,art.assignee_");
		sql.append(",(case when (select count(*) from act_ru_task rt inner join act_ru_identitylink ri on rt.id_ = ri.task_id_ where rt.assignee_ is null) > 0 then TRUE else FALSE end) isCandidate");
		sql.append(",(select string_agg(aig.name_,',') from act_ru_task rt2 inner join act_ru_identitylink ri2 on rt2.id_ = ri2.task_id_ inner join act_id_group aig on ri2.group_id_ = aig.id_ where rt2.id_ = art.id_)groupIds");
		sql.append(",(select string_agg(aiu.first_,',') from act_ru_task rt3 inner join act_ru_identitylink ri3 on rt3.id_ = ri3.task_id_ inner join act_id_user aiu on ri3.user_id_ = aiu.id_ where rt3.id_ = art.id_)userIds");
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
				map.put("create_time_", rs[i++]); //  发送时间
				map.put("arpName", rs[i++]); //  分类
				map.put("description_", rs[i++]); // 附加说明
				map.put("first_", rs[i++]); //  任务处理人姓名
				map.put("assignee_", rs[i++]); //  任务处理人code
				map.put("isCandidate", rs[i++]); //  是否存在候选人或岗位
				map.put("groupIds", rs[i++]); //  候选岗位列表
				map.put("userIds", rs[i++]); //  候选人员列表
				return map;
			}
		});
		return sqlObject;
	}
	
	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		
		// 查看按钮
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.todoView.open"));

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
		// 办理人
		columns.add(new TextColumn4MapKey("aiu.first_", "first_",
				getText("todo.personal.assignee"), 60).setUseTitleFromLabel(true));
		// 办理期限
		columns.add(new TextColumn4MapKey("art.due_date_", "due_date_",
				getText("todo.personal.dueDate"), 120).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// 创建时间
		columns.add(new TextColumn4MapKey("art.create_time_", "create_time_",
				getText("todo.personal.createTime"), 120).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// 候选岗位
		columns.add(new TextColumn4MapKey("groupIds", "groupIds",
				getText("todo.personal.groupIds"), 150).setUseTitleFromLabel(true));
		// 候选人
		columns.add(new TextColumn4MapKey("userIds", "userIds",
				getText("todo.personal.userIds"), 100).setUseTitleFromLabel(true));
		// 分类
		columns.add(new TextColumn4MapKey("arpName", "arpName",
				getText("todo.personal.arpName"), 70).setSortable(true));
		columns.add(new HiddenColumn4MapKey("procInstId", "procInstId"));
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
		return this.getContextPath() + "/bc-workflow/todo/view.js";
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
