package cn.bc.workflow.todo.web.struts2;

import java.util.ArrayList;
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
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.html.toolbar.Toolbar;

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
		return !context.hasAnyRole(getText("key.role.bc.common"));
	}
	
	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select art.id_,aiu.first_ aiuName,art.create_time_,aiu2.first_ aiuName2,aig.name_ aigName,art.name_ artName,arp.name_ arpName,art.due_date_");
		sql.append(" from act_ru_task art");
		sql.append(" left join act_re_procdef arp on art.proc_def_id_ = arp.id_"); //待办分类
		sql.append(" left join act_ru_identitylink ari on art.id_ = ari.task_id_"); //任务参与者
		sql.append(" left join act_id_user aiu on art.assignee_ = aiu.id_"); //发送人
		sql.append(" left join act_id_user aiu2 on ari.user_id_ = aiu2.id_"); //待办人
		sql.append(" left join act_id_group aig on ari.group_id_ = aig.id_"); //待办岗位
		
		sqlObject.setSql(sql.toString());
		
		// 注入参数
		sqlObject.setArgs(null);
		
		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id_", rs[i++]);
				map.put("aiuName", rs[i++]); // 发送人
				map.put("create_time_", rs[i++]); //  发送时间
				map.put("aiuName2", rs[i++]); //  待办人
				map.put("aigName", rs[i++]); //  待办岗位
				map.put("artName", rs[i++]); // 标题
				map.put("arpName", rs[i++]); //  分类
				map.put("due_date_", rs[i++]); // 办理期限
				return map;
			}
		});
		return sqlObject;
	}
	
	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		
		// 查看按钮
		tb.addButton(Toolbar.getDefaultOpenToolbarButton(getText("label.read")));

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
		
		// 发送人
		columns.add(new TextColumn4MapKey("aiuName", "aiuName",
				getText("todo.personal.aiuName"), 60).setSortable(true));
		// 发送时间
		columns.add(new TextColumn4MapKey("art.create_time_", "create_time_",
				getText("todo.personal.createTime"), 90).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// 待办人
		columns.add(new TextColumn4MapKey("aiuName2", "aiuName2",
				getText("todo.personal.todoPeople"), 70).setSortable(true)
				.setValueFormater(new AbstractFormater<String>() {

					@Override
					public String format(Object context, Object value) {
						// 从上下文取出元素Map
						@SuppressWarnings("unchecked")
						Map<String, Object> todo = (Map<String, Object>) context;
						if(todo.get("aiuName2") != null){
							return todo.get("aiuName2").toString();
						}else if(todo.get("aigName") != null){
							return todo.get("aigName").toString();
						}else{
							return "";
						}
					}
					
				}));
		
		// 标题
		columns.add(new TextColumn4MapKey("art.name_", "artName",
				getText("todo.personal.artName"), 150).setSortable(true)
				.setUseTitleFromLabel(true));
		
		// 分类
		columns.add(new TextColumn4MapKey("arpName", "arpName",
				getText("todo.personal.arpName"), 70).setSortable(true));
		
		// 办理期限
		columns.add(new TextColumn4MapKey("art.due_date_", "due_date_",
				getText("todo.personal.dueDate"), 90).setSortable(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));

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
	protected String[] getGridSearchFields() {
		return new String [] {"art.name_","aiu.first_","aiu.last_","arp.name_"};
	}
	
	
}
