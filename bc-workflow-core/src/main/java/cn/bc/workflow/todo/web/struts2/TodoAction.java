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
import cn.bc.web.struts2.ViewAction;
import cn.bc.web.ui.html.grid.Column;
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class TodoAction extends ViewAction<Map<String, Object>>{
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
		// 车辆管理员或系统管理员
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.admin"));
	}
	
	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select art.id_,art.name_ from act_ru_task art");
		
		sqlObject.setSql(sql.toString());
		
		// 注入参数
		sqlObject.setArgs(null);
		
		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id_", rs[i++]);
				map.put("name_", rs[i++]); // 状态
				return map;
			}
		});
		return sqlObject;
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("art.id_", "id_"));
		// 标题
		columns.add(new TextColumn4MapKey("art.name_", "name_",
				"标题", 40).setSortable(true));
		return columns;
	}

	@Override
	protected String getFormActionName() {
		return "todo";
	}
	
	@Override
	protected String getGridRowLabelExpression() {
		return null;
	}

	@Override
	protected String[] getGridSearchFields() {
		return null;
	}
	
	
}
