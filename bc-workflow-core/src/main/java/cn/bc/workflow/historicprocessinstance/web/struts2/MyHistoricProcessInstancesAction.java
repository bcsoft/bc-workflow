package cn.bc.workflow.historicprocessinstance.web.struts2;

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
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.core.query.condition.impl.QlCondition;
import cn.bc.core.util.DateUtils;
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
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.historictaskinstance.service.HistoricTaskInstanceService;
import cn.bc.workflow.service.WorkspaceServiceImpl;

/**
 * 我的经办流程视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class MyHistoricProcessInstancesAction extends HistoricProcessInstancesAction {
	/*private static final Log logger = LogFactory
			.getLog(MyHistoricProcessInstancesAction.class);*/
	private static final long serialVersionUID = 1L;

	public String status ;


	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("a.start_time_", Direction.Desc);
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select a.id_,b.name_ as category,a.start_time_,a.end_time_,a.duration_,f.suspension_state_ status,a.proc_inst_id_");
		sql.append(",e.version_ as version,b.version_ as aVersion,b.key_ as key,c.name");
		sql.append(",getProcessInstanceSubject(a.proc_inst_id_) as subject");
		sql.append(",(select string_agg(e.name_,',') from act_ru_task e where a.id_=e.proc_inst_id_ ) as  todo_names");
		sql.append(" from act_hi_procinst a");
		sql.append(" left join act_ru_execution f on a.id_ = f.proc_inst_id_");
		sql.append(" inner join act_re_procdef b on b.id_=a.proc_def_id_");
		sql.append(" inner join act_re_deployment d on d.id_=b.deployment_id_");
		sql.append(" inner join bc_wf_deploy e on e.deployment_id=d.id_");
		sql.append(" left join bc_identity_actor c on c.code=a.start_user_id_");
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
				map.put("start_time", rs[i++]);
				map.put("end_time", rs[i++]);
				map.put("duration", rs[i++]);
				map.put("status", rs[i++]);
				if (map.get("end_time") != null) {//已结束
					map.put("status", WorkspaceServiceImpl.COMPLETE);
				} else {
					if(map.get("status").equals(String.valueOf(SuspensionState.ACTIVE.getStateCode()))){//流转中
						map.put("status", String.valueOf(SuspensionState.ACTIVE.getStateCode()));
					}else if(map.get("status").equals(String.valueOf(SuspensionState.SUSPENDED.getStateCode()))){//已暂停
						map.put("status", String.valueOf(SuspensionState.SUSPENDED.getStateCode()));
					}
				}

				// 格式化耗时
				if (map.get("duration") != null)
					map.put("frmDuration",
							DateUtils.getWasteTime(Long.parseLong(map.get(
									"duration").toString())));

				map.put("procinstid", rs[i++]);
				map.put("version", rs[i++]);
				map.put("aVersion", rs[i++]);
				map.put("key", rs[i++]);
				map.put("startName", rs[i++]);// 发起人
				map.put("subject", rs[i++]);
				map.put("todo_names", rs[i++]);
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
		columns.add(new TextColumn4MapKey("", "status",
				getText("flow.instance.status"), 50).setSortable(true)
				.setValueFormater(new EntityStatusFormater(getStatus())));
		// 主题
		columns.add(new TextColumn4MapKey(
				"getProcessInstanceSubject(a.proc_inst_id_)", "subject",
				getText("flow.instance.subject"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		// 流程
		columns.add(new TextColumn4MapKey("b.name_", "category",
				getText("flow.instance.name"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("", "todo_names",
				getText("flow.instance.todoTask"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		// 版本号
		columns.add(new TextColumn4MapKey("e.version_", "version",
				getText("flow.instance.version"), 50).setSortable(true)
				.setUseTitleFromLabel(true).setValueFormater(new AbstractFormater<String>() {
					@SuppressWarnings("unchecked")
					@Override
					public String format(Object context, Object value) {
						Map<String, Object> version = (Map<String, Object>) context;
						return version.get("version")+"  ("+version.get("aVersion")+")";
					}
					
				}));
		// 发起人
		columns.add(new TextColumn4MapKey("a.first_", "startName",
				getText("flow.instance.startName"), 80).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("a.start_time_", "start_time",
				getText("flow.instance.startTime"), 150).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		columns.add(new TextColumn4MapKey("a.end_time_", "end_time",
				getText("flow.instance.endTime"), 150).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		columns.add(new TextColumn4MapKey("a.duration_", "frmDuration",
				getText("flow.instance.duration"), 80).setSortable(true));
		columns.add(new TextColumn4MapKey("b.key_", "key",
				getText("flow.instance.key"), 180).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new HiddenColumn4MapKey("procinstid", "procinstid"));
		columns.add(new HiddenColumn4MapKey("status", "status"));
		return columns;
	}

	/**
	 * 状态值转换:流转中|已暂停|已结束|全部
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
	protected String getGridRowLabelExpression() {
		return "['subject']";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "b.name_", "b.key_", "c.name",
				"getProcessInstanceSubject(a.proc_inst_id_)" };
	}

	@Override
	protected String getFormActionName() {
		return "myHistoricProcessInstance";
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
				.setClick("bc.historicProcessInstanceSelectView.open"));
		
		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(),
				"status", 3, getText("title.click2changeSearchStatus")));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}

	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();
		if (status != null && status.length() > 0) {
			String[] ss = status.split(",");
			if (ss.length == 1) {
				String sqlstr="";
				if (ss[0].equals(String.valueOf(SuspensionState.ACTIVE.getStateCode()))) {
					sqlstr += " a.end_time_ is null";
					sqlstr += " and ((b.suspension_state_ = "+SuspensionState.ACTIVE.getStateCode()+")";
					sqlstr += " and (f.suspension_state_ ="+SuspensionState.ACTIVE.getStateCode()+"))";
				} else if (ss[0].equals(String
						.valueOf(SuspensionState.SUSPENDED.getStateCode()))){
					sqlstr += " a.end_time_ is null";
					sqlstr += " and ((b.suspension_state_ = "+SuspensionState.SUSPENDED.getStateCode()+")";
					sqlstr += " or (f.suspension_state_ ="+SuspensionState.SUSPENDED.getStateCode()+"))";
				} else if (ss[0].equals(String.valueOf(WorkspaceServiceImpl.COMPLETE))){
					sqlstr += " a.end_time_ is not null";
				} 
				ac.add(new QlCondition(sqlstr,new Object[]{}));
			}
		}


		SystemContext context = (SystemContext) this.getContext();
		// 保存的用户id键值集合
		String code = context.getUser().getCode();
		String sql = "";
		sql += "exists(";
		sql += "select 1 ";
		sql += " from act_hi_taskinst d";
		sql += " where a.id_=d.proc_inst_id_ and d.end_time_ is not null and d.assignee_ = '";
		sql += code;
		sql += "')";

		ac.add(new QlCondition(sql, new Object[] {}));
		ac.add(new IsNullCondition("f.parent_id_"));

		return ac.isEmpty() ? null : ac;
	}

	@Override
	protected Json getGridExtrasData() {
		Json json = new Json();
		// 状态条件
		if (status != null && status.length() > 0)
			json.put("status", status);

		return json;
	}

	@Override
	protected String getGridDblRowMethod() {
		return "bc.historicProcessInstanceSelectView.open";
	}

	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace()
				+ "/historicprocessinstance/select.js"+","+
				this.getHtmlPageNamespace() + "/historicprocessinstance/view.js";
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
	}

	// ==高级搜索代码结束==
}
