package cn.bc.workflow.historictaskinstance.web.struts2;

import java.util.ArrayList;
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
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.IsNotNullCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.core.util.DateUtils;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.identity.web.SystemContext;
import cn.bc.option.domain.OptionItem;
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
import cn.bc.workflow.historictaskinstance.service.HistoricTaskInstanceService;
import cn.bc.workflow.service.WorkspaceServiceImpl;

/**
 * 我的经办任视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class MyHistoricTaskInstancesAction extends
		ViewAction<Map<String, Object>> {
	private static final long serialVersionUID = 1L;

	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("a.end_time_", Direction.Desc);
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select a.id_,c.name_ as category,a.name_ as name,a.start_time_,a.end_time_,a.duration_,a.proc_inst_id_");
		sql.append(",a.task_def_key_,b.end_time_ as p_end_time");
		sql.append(",getProcessInstanceSubject(a.proc_inst_id_) as subject,a.due_date_ as due_date");
		sql.append(",h.suspension_state_ status");
		sql.append(" from act_hi_taskinst a");
		sql.append(" inner join act_hi_procinst b on b.proc_inst_id_=a.proc_inst_id_");
		sql.append(" inner join act_re_procdef c on c.id_=a.proc_def_id_");
		sql.append(" left join act_ru_execution h on h.proc_inst_id_ = a.proc_inst_id_");
		sqlObject.setSql(sql.toString());

		// 注入参数
		sqlObject.setArgs(null);

		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id", rs[i++]);
				map.put("category", rs[i++]);
				map.put("name", rs[i++]);
				map.put("start_time", rs[i++]);
				map.put("end_time", rs[i++]);
				map.put("duration", rs[i++]);
				map.put("procinstid", rs[i++]);
				map.put("taskdefkey", rs[i++]);
				map.put("p_end_time", rs[i++]);
				map.put("subject", rs[i++]);
				map.put("due_date", rs[i++]);
				map.put("status", rs[i++]);
				
				if (map.get("status") == null) {
					// 已完成
					map.put("pstatus", WorkspaceServiceImpl.COMPLETE);
				} else{
					// 未完成
					if(map.get("status").toString().equals(String.valueOf(SuspensionState.ACTIVE.getStateCode()))){//处理中
						map.put("pstatus", String.valueOf(SuspensionState.ACTIVE.getStateCode()));
					}else if(map.get("status").toString().equals(String.valueOf(SuspensionState.SUSPENDED.getStateCode()))){//已暂停
						map.put("pstatus", String.valueOf(SuspensionState.SUSPENDED.getStateCode()));
					}
				}
				
				// 格式化耗时
				if (map.get("duration") != null)
					map.put("frmDuration",
							DateUtils.getWasteTime(Long.parseLong(map.get(
									"duration").toString())));
				return map;
			}
		});
		return sqlObject;
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("a.id_", "id"));
		// 主题
		columns.add(new TextColumn4MapKey(
				"getProcessInstanceSubject(a.proc_inst_id_)", "subject",
				getText("flow.task.subject"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		// 名称
		columns.add(new TextColumn4MapKey("a.name_", "name",
				getText("flow.task.name"), 200).setUseTitleFromLabel(true));
		//办理期限
		columns.add(new TextColumn4MapKey("a.due_date_", "due_date",
				getText("done.dueDate"), 130).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));

		columns.add(new TextColumn4MapKey("a.start_time_", "start_time",
				getText("flow.task.startTime"), 130).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		columns.add(new TextColumn4MapKey("a.end_time_", "end_time",
				getText("flow.task.endTime"), 130).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
	
		columns.add(new TextColumn4MapKey("a.duration_", "frmDuration",
				getText("flow.task.duration"), 80).setSortable(true));
		// 流程
		columns.add(new TextColumn4MapKey("c.name_", "category",
				getText("flow.task.category")).setSortable(true)
				.setUseTitleFromLabel(true));
		//流程状态
		columns.add(new TextColumn4MapKey("", "pstatus",
				getText("flow.task.pstatus"), 80).setSortable(true)
				.setValueFormater(new EntityStatusFormater(getPStatus())));

		columns.add(new HiddenColumn4MapKey("procinstid", "procinstid"));
		columns.add(new HiddenColumn4MapKey("assignee", "assignee"));
		return columns;
	}

	@Override
	protected String getGridRowLabelExpression() {
		return "'我的经办：'+['name']";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "a.name_", "c.name_",
				"getProcessInstanceSubject(a.proc_inst_id_)"};
	}

	@Override
	protected String getFormActionName() {
		return "myDone" ;
	}

	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(800).setMinWidth(400)
				.setHeight(400).setMinHeight(300);
	}

	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		// 查看
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.myHistoricTaskInstanceSelectView.open"));
		
		tb.addButton(new ToolbarButton().setIcon("ui-icon-search")
				.setText(getText("flow.task.flow"))
				.setClick("bc.myHistoricTaskInstanceSelectView.viewflow"));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}
	
	/**
	 * 流程状态值转换:流转中|已暂停|已结束|全部
	 * 
	 */
	private Map<String, String> getPStatus() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put(String.valueOf(SuspensionState.ACTIVE.getStateCode()),
				getText("done.status.processing"));
		map.put(String.valueOf(SuspensionState.SUSPENDED.getStateCode()),
				getText("done.status.suspended"));
		map.put(String.valueOf(WorkspaceServiceImpl.COMPLETE),
				getText("done.status.finished"));
		map.put("", getText("bc.status.all"));
		return map;
	}

	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();

		SystemContext context = (SystemContext) this.getContext();
		ac.add(new EqualsCondition("a.assignee_", context.getUser()
				.getCode()));
		// 结束时间不能为空
		ac.add(new IsNotNullCondition("a.end_time_"));
		ac.add(new IsNullCondition("h.parent_id_"));
		
		return ac.isEmpty() ? null : ac;
	}

	@Override
	protected Json getGridExtrasData() {
		Json json = new Json();
		return json;
	}

	@Override
	protected String getGridDblRowMethod() {
		return "bc.myHistoricTaskInstanceSelectView.open";
	}

	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace() + "/historictaskinstance/my/select.js";
	}

	@Override
	protected String getHtmlPageNamespace() {
		return this.getContextPath() + "/bc-workflow";
	}

	// ==高级搜索代码开始==
	@Override
	protected boolean useAdvanceSearch() {
		return true;
	}

	private HistoricTaskInstanceService historicTaskInstanceService;
	
	@Autowired
	public void setHistoricTaskInstanceService(
			HistoricTaskInstanceService historicTaskInstanceService) {
		this.historicTaskInstanceService = historicTaskInstanceService;
	}

	public JSONArray processList;
	
	public JSONArray taskList;

	@Override
	protected void initConditionsFrom() throws Exception {
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		String account=context.getUserHistory().getCode();
		List<String> values=this.historicTaskInstanceService.findProcessNames(account, true);
		List<Map<String,String>> list = new ArrayList<Map<String,String>>();
		Map<String,String> map;
		for(String value : values){
			map = new HashMap<String, String>();
			map.put("key", value);
			map.put("value", value);
			list.add(map);
		}
		this.processList = OptionItem.toLabelValues(list);
		
		values=this.historicTaskInstanceService.findTaskNames(account, true);
		list = new ArrayList<Map<String,String>>();
		for(String value : values){
			map = new HashMap<String, String>();
			map.put("key", value);
			map.put("value", value);
			list.add(map);
		}
		this.taskList = OptionItem.toLabelValues(list);
	}
	// ==高级搜索代码结束==

}
