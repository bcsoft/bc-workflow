package cn.bc.workflow.historictaskinstance.web.struts2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.BCConstants;
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

/**
 * 经办、任务监控视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class HistoricTaskInstancesAction extends
		ViewAction<Map<String, Object>> {
	private static final long serialVersionUID = 1L;
	public String status = String.valueOf(BCConstants.STATUS_ENABLED);
	public boolean my = false;// 是否从我的经办

	@Override
	public boolean isReadonly() {
		SystemContext context = (SystemContext) this.getContext();
		// 配置权限：、超级管理员
		return !context.hasAnyRole(getText("key.role.bc.admin"),
				getText("key.role.bc.workflow"));
	}

	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("a.start_time_", Direction.Desc);
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select a.id_,c.name_ as category,a.name_ as name,a.start_time_,a.end_time_,d.name as receiver,a.duration_,a.proc_inst_id_");
		sql.append(",a.task_def_key_,b.end_time_ as p_end_time");
		sql.append(",getProcessInstanceSubject(a.proc_inst_id_) as subject,a.due_date_ as due_date,g.name as gwei");
		sql.append(" from act_hi_taskinst a");
		sql.append(" inner join act_hi_procinst b on b.proc_inst_id_=a.proc_inst_id_");
		sql.append(" inner join act_re_procdef c on c.id_=a.proc_def_id_");
		sql.append(" left join bc_identity_actor d on d.code=a.assignee_");
		sql.append(" left join act_re_procdef e on e.id_=a.proc_def_id_");
		sql.append(" left join act_ru_identitylink f on f.task_id_=a.id_");
		sql.append(" left join bc_identity_actor g on g.code=f.group_id_");
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
				map.put("receiver", rs[i++]);
				map.put("duration", rs[i++]);
				// 根据结束时间取得状态
				if (map.get("end_time") != null) {
					// 已完成
					map.put("status", BCConstants.STATUS_DISABLED);
				} else
					// 未完成
					map.put("status", BCConstants.STATUS_ENABLED);
				// 格式化耗时
				if (map.get("duration") != null)
					map.put("frmDuration",
							DateUtils.getWasteTime(Long.parseLong(map.get(
									"duration").toString())));
				map.put("procinstid", rs[i++]);
				map.put("taskdefkey", rs[i++]);
				map.put("p_end_time", rs[i++]);
				//根据流程的结束时间获取整个流程的状态
				if (map.get("p_end_time") != null) {
					// 已完成
					map.put("pstatus", BCConstants.STATUS_DISABLED);
				} else
					// 未完成
					map.put("pstatus", BCConstants.STATUS_ENABLED);
				
				map.put("subject", rs[i++]);
				map.put("due_date", rs[i++]);
				
				if(map.get("receiver") ==null ||map.get("receiver").toString().length()==0)
				map.put("receiver", rs[i++]);
				
				return map;
			}
		});
		return sqlObject;
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("a.id_", "id"));

		// 状态
		if (!my)
			columns.add(new TextColumn4MapKey("", "status",
					getText("flow.task.status"), 50).setSortable(true)
					.setValueFormater(new EntityStatusFormater(getStatus())));
		// 主题
		columns.add(new TextColumn4MapKey(
				"getProcessInstanceSubject(a.proc_inst_id_)", "subject",
				getText("flow.task.subject"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		// 名称
		columns.add(new TextColumn4MapKey("a.name_", "name",
				getText("flow.task.name"), 200).setUseTitleFromLabel(true));

		if (!my) {
			columns.add(new TextColumn4MapKey("d.name", "receiver",
					getText("flow.task.actor"), 120).setUseTitleFromLabel(true));
		}
		//办理期限
		columns.add(new TextColumn4MapKey("a.due_date_", "due_date",
				getText("done.dueDate"), 130).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		if (my) {
			columns.add(new TextColumn4MapKey("a.start_time_", "start_time",
					getText("flow.task.startTime"), 130).setSortable(true)
					.setUseTitleFromLabel(true)
					.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
			columns.add(new TextColumn4MapKey("a.end_time_", "end_time",
					getText("flow.task.endTime"), 130).setSortable(true)
					.setUseTitleFromLabel(true)
					.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		} else {
			columns.add(new TextColumn4MapKey("a.start_time_", "start_time",
					getText("flow.task.startTime"), 150)
					.setSortable(true)
					.setUseTitleFromLabel(true)
					.setValueFormater(
							new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
			columns.add(new TextColumn4MapKey("a.end_time_", "end_time",
					getText("flow.task.endTime"), 150)
					.setSortable(true)
					.setUseTitleFromLabel(true)
					.setValueFormater(
							new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		}
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
		if (!my) {
			columns.add(new TextColumn4MapKey("a.task_def_key_", "taskdefkey",
					"任务key值", 80));
		}

		columns.add(new HiddenColumn4MapKey("procinstid", "procinstid"));
		return columns;
	}

	@Override
	protected String getGridRowLabelExpression() {
		return my ? "'我的经办：'+['name']" : "['name']";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "d.name", "a.name_", "c.name_",
				"getProcessInstanceSubject(a.proc_inst_id_)","g.name" };
	}

	@Override
	protected String getFormActionName() {
		return my ? "myDone" : "historicTaskInstance";
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
				.setClick("bc.historicTaskInstanceSelectView.open"));
		
		if(my){
			tb.addButton(new ToolbarButton().setIcon("ui-icon-search")
					.setText(getText("flow.task.flow"))
					.setClick("bc.historicTaskInstanceSelectView.viewflow"));
		}

		if (!my){
			tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(),
					"status", BCConstants.STATUS_ENABLED,
					getText("title.click2changeSearchStatus")));
		}
		
		
			

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}

	/**
	 * 状态值转换:已完成|未完成|全部
	 * 
	 */
	private Map<String, String> getStatus() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put(String.valueOf(BCConstants.STATUS_ENABLED),
				getText("flow.task.status.doing"));
		map.put(String.valueOf(BCConstants.STATUS_DISABLED),
				getText("flow.task.status.finished"));
		map.put("", getText("bc.status.all"));
		return map;
	}
	
	/**
	 * 流程状态值转换:流转中|已完成|全部
	 * 
	 */
	private Map<String, String> getPStatus() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put(String.valueOf(BCConstants.STATUS_ENABLED),
				getText("flow.instance.status.processing"));
		map.put(String.valueOf(BCConstants.STATUS_DISABLED),
				getText("flow.instance.status.finished"));
		map.put("", getText("bc.status.all"));
		return map;
	}

	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();
		if (my) {
			SystemContext context = (SystemContext) this.getContext();
			ac.add(new EqualsCondition("a.assignee_", context.getUser()
					.getCode()));
			// 结束时间不能为空
			ac.add(new IsNotNullCondition("a.end_time_"));
		} else if (status != null && status.length() > 0) {
			String[] ss = status.split(",");
			if (ss.length == 1) {
				if (ss[0].equals(String.valueOf(BCConstants.STATUS_ENABLED))) {
					ac.add(new IsNullCondition("a.end_time_"));
				} else if (ss[0].equals(String
						.valueOf(BCConstants.STATUS_DISABLED)))
					ac.add(new IsNotNullCondition("a.end_time_"));
			}
		}
		return ac.isEmpty() ? null : ac;
	}

	@Override
	protected Json getGridExtrasData() {
		Json json = new Json();
		// 状态条件
		if (status != null && status.length() > 0)
			json.put("status", status);
		if (my)
			json.put("my", my);
		return json;
	}

	@Override
	protected String getGridDblRowMethod() {
		return "bc.historicTaskInstanceSelectView.open";
	}

	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace() + "/historictaskinstance/select.js";
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
	@Autowired
	private RepositoryService repositoryService;
	
	public JSONArray processList;

	@Override
	protected void initConditionsFrom() throws Exception {
		List<ProcessDefinition> list = this.repositoryService
				.createProcessDefinitionQuery().list();
		List<Map<String,String>> list2 = new ArrayList<Map<String,String>>();
		
		for(ProcessDefinition pd : list){
			Map<String,String> map = new HashMap<String, String>();
			map.put("key", pd.getId());
			map.put("value", pd.getName());
			list2.add(map);
		}
		this.processList = OptionItem.toLabelValues(list2);
	}
	// ==高级搜索代码结束==

}
