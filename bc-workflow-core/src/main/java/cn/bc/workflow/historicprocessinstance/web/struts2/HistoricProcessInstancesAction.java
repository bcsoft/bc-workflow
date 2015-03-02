package cn.bc.workflow.historicprocessinstance.web.struts2;

import cn.bc.core.exception.CoreException;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.core.query.condition.impl.QlCondition;
import cn.bc.core.util.DateUtils;
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
import cn.bc.web.ui.html.page.HtmlPage;
import cn.bc.web.ui.html.page.ListPage;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.historictaskinstance.service.HistoricTaskInstanceService;
import cn.bc.workflow.service.WorkflowService;
import cn.bc.workflow.service.WorkspaceServiceImpl_old;
import cn.bc.workflow.web.struts2.ViewAction;
import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 流程监控视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class HistoricProcessInstancesAction extends
		ViewAction<Map<String, Object>> {
	private static final Log logger = LogFactory
			.getLog(HistoricProcessInstancesAction.class);
	private static final long serialVersionUID = 1L;
	private WorkflowService workflowService;

	@Autowired
	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	public String status = "1";// 默认待办中

	@Override
	public boolean isReadonly() {
		SystemContext context = (SystemContext) this.getContext();
		// 配置权限：、超级管理员
		return !context.hasAnyRole(getText("key.role.bc.admin"),
				getText("key.role.bc.workflow"));
	}

	public boolean isAccessControl() {
		// 流程访问控制
		SystemContext context = (SystemContext) this.getContext();
		return context
				.hasAnyRole(getText("key.role.bc.workflow.accessControl"));
	}

	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("a.start_time_", Direction.Desc);
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>() {
			// 自己根据条件构建实际的sql
			@Override
			public String getSql(Condition condition) {
				Assert.notNull(condition);
				return getSelect() + " " + getFromWhereSql(condition);
			}

			@Override
			public String getFromWhereSql(Condition condition) {
				Assert.notNull(condition);
				String expression = condition.getExpression();
				if (!expression.startsWith("order by")) {
					expression = "where " + expression;
				}
				return getFrom().replace("${condition}", expression);
			}
		};

		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer selectSql = new StringBuffer();
		selectSql
				.append("select p.*,getProcessInstanceSubject(p.id_) as subject");

		StringBuffer fromSql = new StringBuffer();
		fromSql.append(" from (select a.id_,b.name_ as procinstname,a.start_time_,a.end_time_,a.duration_,f.suspension_state_ status,a.proc_inst_id_");
		fromSql.append(",e.version_ as version,b.version_ as aVersion,b.key_ as key,c.name,e.id as deploy_id,w.text_ as wf_code");
		fromSql.append(",getprocesstodotasknames(a.proc_inst_id_) as  todo_names");
		fromSql.append(",getaccessactors4docidtype4docidcharacter(a.id_,'ProcessInstance')");
		fromSql.append(" from act_hi_procinst a");
		fromSql.append(" inner join act_re_procdef b on b.id_=a.proc_def_id_");
		fromSql.append(" inner join act_re_deployment d on d.id_=b.deployment_id_");
		fromSql.append(" inner join bc_wf_deploy e on e.deployment_id=d.id_");
		fromSql.append(" left join bc_identity_actor c on c.code=a.start_user_id_");
		fromSql.append(" left join act_ru_execution f on a.id_ = f.proc_inst_id_");
        fromSql.append(" left join (");
        fromSql.append(" select d.proc_inst_id_, d.text_");
        fromSql.append(" from act_hi_detail d");
        fromSql.append(" where d.name_ = 'wf_code'");
        fromSql.append(" ) w on w.proc_inst_id_ = a.id_");
		fromSql.append(" ${condition} ) p");

		sqlObject.setSelect(selectSql.toString());
		sqlObject.setFrom(fromSql.toString());
		// 注入参数
		sqlObject.setArgs(null);

		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id", rs[i++]);
				map.put("procinst_name", rs[i++]);
				map.put("start_time", rs[i++]);
				map.put("end_time", rs[i++]);
				map.put("duration", rs[i++]);
				map.put("status", rs[i++]);
				map.put("procinstid", rs[i++]);
				map.put("version", rs[i++]);
				map.put("aVersion", rs[i++]);
				map.put("key", rs[i++]);
				map.put("start_name", rs[i++]);// 发起人
				map.put("deployId", rs[i++]);
				map.put("wf_code", rs[i++]);
				map.put("todo_names", rs[i++]);
				map.put("accessactors", rs[i++]);
				map.put("subject", rs[i++]);

				map.put("accessControlDocType", "ProcessInstance");

				if (map.get("end_time") != null) {// 已结束
					map.put("status", WorkspaceServiceImpl_old.COMPLETE);
				} else {
					if (map.get("status").equals(
							String.valueOf(SuspensionState.ACTIVE
									.getStateCode()))) {// 流转中
						map.put("status", String.valueOf(SuspensionState.ACTIVE
								.getStateCode()));
					} else if (map.get("status").equals(
							String.valueOf(SuspensionState.SUSPENDED
									.getStateCode()))) {// 已暂停
						map.put("status", String
								.valueOf(SuspensionState.SUSPENDED
										.getStateCode()));
					}
				}

				if (map.get("subject") != null
						&& !map.get("subject").toString().equals("")) {
					map.put("accessControlDocName", map.get("subject")
							.toString());
				} else {
					map.put("accessControlDocName", map.get("procinst_name")
							.toString());
				}

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
		columns.add(new TextColumn4MapKey("b.name_", "procinst_name",
				getText("flow.instance.name"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("", "todo_names",
				getText("flow.instance.todoTask"), 200).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new AbstractFormater<String>() {
					@SuppressWarnings("unchecked")
					@Override
					public String format(Object context, Object value) {
						String value_ = StringUtils
								.toString(((Map<String, Object>) context)
										.get("todo_names"));
						if (value_ != null)
							return value_.replaceAll(";", ",");
						return value_;
					}
				}));
		// 版本号
		columns.add(new TextColumn4MapKey("e.version_", "version",
				getText("flow.instance.version"), 50).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new AbstractFormater<String>() {
					@SuppressWarnings("unchecked")
					@Override
					public String format(Object context, Object value) {
						Map<String, Object> version = (Map<String, Object>) context;
						return version.get("version") + "  ("
								+ version.get("aVersion") + ")";
					}

				}));
		// 发起人
		columns.add(new TextColumn4MapKey("a.first_", "start_name",
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
		columns.add(new TextColumn4MapKey("a.duration_", "duration",
				getText("flow.instance.duration"), 80).setSortable(true)
				.setValueFormater(new AbstractFormater<String>() {
					@SuppressWarnings("unchecked")
					@Override
					public String format(Object context, Object value) {
						Object duration_obj = ((Map<String, Object>) context)
								.get("duration");
						if (duration_obj == null)
							return null;
						return DateUtils.getWasteTime(Long
								.parseLong(duration_obj.toString()));
					}
				}));
		if (this.isAccessControl()) {
			columns.add(new TextColumn4MapKey("", "accessactors",
					getText("flow.accessControl.accessActorAndRole"))
					.setSortable(true).setUseTitleFromLabel(true));
		}

		columns.add(new TextColumn4MapKey("b.key_", "key",
				getText("flow.instance.key"), 180).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new HiddenColumn4MapKey("procinstid", "procinstid"));
		columns.add(new HiddenColumn4MapKey("status", "status"));
		columns.add(new HiddenColumn4MapKey("accessControlDocType",
				"accessControlDocType"));
		columns.add(new HiddenColumn4MapKey("accessControlDocName",
				"accessControlDocName"));
		columns.add(new HiddenColumn4MapKey("deployId", "deployId"));
		return columns;
	}

	/**
	 * 状态值转换:流转中|已暂停|已结束|全部
	 * 
	 */
	protected Map<String, String> getStatus() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put(String.valueOf(SuspensionState.ACTIVE.getStateCode()),
				getText("flow.instance.status.processing"));
		map.put(String.valueOf(SuspensionState.SUSPENDED.getStateCode()),
				getText("flow.instance.status.suspended"));
		map.put(String.valueOf(WorkspaceServiceImpl_old.COMPLETE),
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
		return new String[] { "b.name_", "b.key_", "c.name","w.text_",
				"getProcessInstanceSubject(a.id_)",
				"getprocesstodotasknames(a.id_)" };
	}

	@Override
	protected String getFormActionName() {
		return "historicProcessInstance";
	}

	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(850).setMinWidth(400)
				.setHeight(400).setMinHeight(300);
	}

	@Override
	protected HtmlPage buildHtmlPage() {
		ListPage listPage = (ListPage) super.buildHtmlPage();
		listPage.setDeleteUrl(getHtmlPageNamespace()
				+ "/historicProcessInstances/delete");
		return listPage;
	}

	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		// 查看
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.historicProcessInstanceSelectView.open"));

		// 发起流程
		tb.addButton(new ToolbarButton().setIcon("ui-icon-bullet")
				.setText(getText("flow.start"))
				.setClick("bc.historicProcessInstanceSelectView.startflow"));

		if (!this.isReadonly()) {
			// 激活
			tb.addButton(new ToolbarButton().setIcon("ui-icon-play")
					.setText(getText("lable.flow.active"))
					.setClick("bc.historicprocessinstance.active"));
			// 暂停
			tb.addButton(new ToolbarButton().setIcon("ui-icon-pause")
					.setText(getText("lable.flow.suspended"))
					.setClick("bc.historicprocessinstance.suspended"));
		}

		// 流程实例级联删除
		if (((SystemContext) this.getContext())
				.hasAnyRole("BC_WORKFLOW_INSTANCE_DELETE")) {
			tb.addButton(this.getDefaultDeleteToolbarButton());
		}

		if (this.isAccessControl()) {
			// 访问监控
			tb.addButton(new ToolbarButton().setIcon("ui-icon-wrench")
					.setText(getText("flow.accessControl"))
					.setClick("bc.historicprocessinstance.accessControl"));
		}

		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(),
				"status", 0, getText("title.click2changeSearchStatus")));

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
				String sqlstr = "";
				if (ss[0].equals(String.valueOf(SuspensionState.ACTIVE
						.getStateCode()))) {
					sqlstr += " a.end_time_ is null";
					sqlstr += " and ((b.suspension_state_ = "
							+ SuspensionState.ACTIVE.getStateCode() + ")";
					sqlstr += " and (f.suspension_state_ ="
							+ SuspensionState.ACTIVE.getStateCode() + "))";
				} else if (ss[0].equals(String
						.valueOf(SuspensionState.SUSPENDED.getStateCode()))) {
					sqlstr += " a.end_time_ is null";
					sqlstr += " and ((b.suspension_state_ = "
							+ SuspensionState.SUSPENDED.getStateCode() + ")";
					sqlstr += " or (f.suspension_state_ ="
							+ SuspensionState.SUSPENDED.getStateCode() + "))";
				} else if (ss[0].equals(String
						.valueOf(WorkspaceServiceImpl_old.COMPLETE))) {
					sqlstr += " a.end_time_ is not null";
				}
				ac.add(new QlCondition(sqlstr, new Object[] {}));
			}
		}

		ac.add(new IsNullCondition("f.parent_id_"));

		return ac;
	}

	@Override
    protected void extendGridExtrasData(JSONObject json) throws JSONException {
		// 状态条件
		if (status != null && status.length() > 0)
			json.put("status", status);
	}

	@Override
	protected String getGridDblRowMethod() {
		return "bc.historicProcessInstanceSelectView.open";
	}

	@Override
	protected String getHtmlPageJs() {
		return this.getModuleContextPath()
				+ "/historicprocessinstance/select.js" + ","
				+ this.getModuleContextPath()
				+ "/historicprocessinstance/view.js" + ","
				+ this.getContextPath() + "/bc/acl/accessControl.js";
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
		List<String> values = this.historicTaskInstanceService
				.findProcessNames();
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Map<String, String> map;
		for (String value : values) {
			map = new HashMap<String, String>();
			map.put("key", value);
			map.put("value", value);
			list.add(map);
		}
		this.processList = OptionItem.toLabelValues(list);
	}

	// ==高级搜索代码结束==

	public String id; // 流程实例id
	public String ids;

	/**
	 * 删除流程实例
	 * 
	 * @return
	 * @throws Exception
	 */
	public String delete() throws Exception {
		Json json = new Json();
		try {
			if (ids != null) {
				throw new CoreException("为安全起见，系统限制为每次只可删除一个流程实例！");
			}
			this.workflowService.deleteInstance(new String[] { id });
			json.put("success", true);
			json.put("msg", getText("form.delete.success"));
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
			json.put("success", false);
			json.put("msg", e.getMessage());
			json.put("e", e.getClass().getSimpleName());
		}
		this.json = json.toString();
		return "json";
	}

	/** 激活流程 **/
	public String doActive() {
		Json json = new Json();
		this.workflowService.doActive(this.id);
		json.put("msg", getText("flow.msg.active.success"));
		json.put("id", this.id);
		this.json = json.toString();
		return "json";
	}

	/** 暂停流程 **/
	public String doSuspended() {
		Json json = new Json();
		this.workflowService.doSuspended(this.id);
		json.put("msg", getText("flow.msg.suspended.success"));
		json.put("id", this.id);
		this.json = json.toString();
		return "json";
	}

}
