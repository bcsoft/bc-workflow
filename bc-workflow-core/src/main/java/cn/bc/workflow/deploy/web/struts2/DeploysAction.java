package cn.bc.workflow.deploy.web.struts2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.BCConstants;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.ConditionUtils;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.identity.web.SystemContext;
import cn.bc.option.domain.OptionItem;
import cn.bc.web.formater.CalendarFormater;
import cn.bc.web.formater.KeyValueFormater;
import cn.bc.web.struts2.ViewAction;
import cn.bc.web.ui.html.grid.Column;
import cn.bc.web.ui.html.grid.HiddenColumn4MapKey;
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.service.DeployService;

/**
 * 流程部署视图Action
 * 
 * @author wis
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class DeploysAction extends ViewAction<Map<String, Object>> {
	private static final long serialVersionUID = 1L;
	public String status = "";// String.valueOf(Deploy.STATUS_RELEASED);
	public boolean my = false;

	@Override
	public boolean isReadonly() {
		// 权限控制：流程部署管理、流程管理或系统管理员
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.workflow.deploy"),
				getText("key.role.bc.workflow"), getText("key.role.bc.admin"));
	}

	public boolean isCascade() {
		// 流程部署级联删除管理
		SystemContext context = (SystemContext) this.getContext();
		return context
				.hasAnyRole(getText("key.role.bc.workflow.deploy.cascade"));
	}

	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("d.status_", Direction.Asc).add("d.order_",
				Direction.Asc);
	}
	
	@Override
	protected String getGridDblRowMethod() {
		// 强制为只读表单
		return "bc.deploy.dblclick";
	}


	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();

		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select d.id,d.uid_,d.order_ as orderNo,d.code,d.type_ as type,d.desc_,d.path,d.subject,d.source");
		sql.append(",au.actor_name as uname,d.file_date,am.actor_name as mname");
		sql.append(",d.modified_date,d.status_ as status,d.version_ as version");
		sql.append(",d.category,d.size_ as size,ap.actor_name as pname,d.deploy_date");
		sql.append(",getdeployuser(d.id)");
		sql.append(" from bc_wf_deploy d");
		sql.append(" inner join bc_identity_actor_history au on au.id=d.author_id ");
		sql.append(" left join bc_identity_actor_history am on am.id=d.modifier_id");
		sql.append(" left join bc_identity_actor_history ap on ap.id=d.deployer_id");
		sqlObject.setSql(sql.toString());

		// 注入参数
		sqlObject.setArgs(null);

		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id", rs[i++]);
				map.put("uid", rs[i++]);
				map.put("orderNo", rs[i++]);
				map.put("code", rs[i++]);
				map.put("type", rs[i++]);
				map.put("desc_", rs[i++]);
				map.put("path", rs[i++]);
				map.put("subject", rs[i++]);
				map.put("source", rs[i++]);
				map.put("uname", rs[i++]);
				map.put("file_date", rs[i++]);
				map.put("mname", rs[i++]);
				map.put("modified_date", rs[i++]);
				map.put("status", rs[i++]);
				map.put("version", rs[i++]);
				map.put("category", rs[i++]);
				map.put("size", rs[i++]);
				map.put("pname", rs[i++]);
				map.put("deploy_date", rs[i++]);
				map.put("users", rs[i++]);
				return map;
			}
		});
		return sqlObject;
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("d.id", "id"));
		columns.add(new TextColumn4MapKey("a.status_", "status",
				getText("deploy.status"), 50).setSortable(true)
				.setValueFormater(new KeyValueFormater(this.getStatuses())));
		columns.add(new TextColumn4MapKey("d.order_", "orderNo",
				getText("deploy.order"), 50).setSortable(true));
		columns.add(new TextColumn4MapKey("d.category", "category",
				getText("deploy.category"), 150).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.subject", "subject",
				getText("deploy.tfsubject"), 200).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.version_", "version",
				getText("deploy.version"), 50).setUseTitleFromLabel(true));
		// columns.add(new TextColumn4MapKey("d.source", "source",
		// getText("deploy.source"), 150).setUseTitleFromLabel(true));
		if (!my) {
			columns.add(new TextColumn4MapKey("", "users",
					getText("deploy.user"), 120).setUseTitleFromLabel(true));
		}
		columns.add(new TextColumn4MapKey("d.type_", "type",
				getText("deploy.type"), 85).setUseTitleFromLabel(true)
				.setValueFormater(new KeyValueFormater(this.getTypes())));
		columns.add(new TextColumn4MapKey("d.code", "code",
				getText("deploy.code"), 150).setSortable(true)
				.setUseTitleFromLabel(true));
		// columns.add(new TextColumn4MapKey("d.path", "path",
		// getText("deploy.tfpath"), 250).setUseTitleFromLabel(true));
		// columns.add(new TextColumn4MapKey("d.size_", "size",
		// getText("deploy.file.size"),65).setUseTitleFromLabel(true)
		// .setValueFormater(new FileSizeFormater()));
		// columns.add(new TextColumn4MapKey("d.desc_", "desc_",
		// getText("deploy.desc"), 100).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("ap.actor_name", "pname",
				getText("deploy.deployer"), 130));
		columns.add(new TextColumn4MapKey("d.deploy_date", "deploy_date",
				getText("deploy.deployDate"), 130)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		columns.add(new TextColumn4MapKey("au.actor_name", "uname",
				getText("deploy.author"), 80));
		columns.add(new TextColumn4MapKey("d.file_date", "file_date",
				getText("deploy.fileDate")).setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		// columns.add(new TextColumn4MapKey("am.actor_name", "mname",
		// getText("deploy.modifier"), 80));
		// columns.add(new TextColumn4MapKey("d.modified_date", "modified_date",
		// getText("deploy.modifiedDate"), 130)
		// .setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		columns.add(new HiddenColumn4MapKey("uid", "uid"));
		columns.add(new HiddenColumn4MapKey("status", "status"));
		return columns;
	}

	// 状态键值转换
	private Map<String, String> getStatuses() {
		Map<String, String> statuses = new LinkedHashMap<String, String>();
		statuses.put(String.valueOf(BCConstants.STATUS_DRAFT),
				getText("deploy.status.draft"));
		statuses.put(String.valueOf(Deploy.STATUS_USING),
				getText("deploy.status.using"));
		statuses.put(String.valueOf(Deploy.STATUS_STOPPED),
				getText("deploy.status.stopped"));
		statuses.put("", getText("deploy.status.all"));
		return statuses;
	}

	// 类型键值转换
	private Map<String, String> getTypes() {
		Map<String, String> statuses = new LinkedHashMap<String, String>();
		statuses.put(String.valueOf(Deploy.TYPE_XML),
				getText("deploy.type.xml"));
		statuses.put(String.valueOf(Deploy.TYPE_BAR),
				getText("deploy.type.bar"));
		return statuses;
	}

	@Override
	protected String getGridRowLabelExpression() {
		return "['subject']+'的流程部署\t-\t v'+['version']";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "d.code", "am.actor_name", "d.path", "d.subject",
				"d.version_", "d.category", "d.source" };
	}

	@Override
	protected String getFormActionName() {
		return "deploy";
	}

	@Override
	protected String getHtmlPageNamespace() {
		return this.getContextPath() + "/bc-workflow";
	}

	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(890).setMinWidth(400)
				.setHeight(400).setMinHeight(300);
	}

	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();

		if (!this.isReadonly()) {
			// 新建按钮
			tb.addButton(this.getDefaultCreateToolbarButton());

			// 编辑按钮
			tb.addButton(this.getDefaultOpenToolbarButton());

			// 发布
			tb.addButton(new ToolbarButton().setIcon("ui-icon-person")
					.setText(getText("label.deploy.release"))
					.setClick("bc.deploy.release"));
			// 停用
			tb.addButton(new ToolbarButton().setIcon("ui-icon-cancel")
					.setText(getText("label.deploy.stop"))
					.setClick("bc.deploy.stop"));
			
			// 强制删除
			tb.addButton(getDefaultDeleteToolbarButton());
			
//			// 取消发布
//			tb.addButton(new ToolbarButton().setIcon("ui-icon-trash")
//					.setText(getText("label.deploy.releaseCancel"))
//					.setClick("bc.deploy.releaseCancel"));
//
//			if (this.isCascade()) {
//				// 级联取消发布
//				tb.addButton(new ToolbarButton().setIcon("ui-icon-trash")
//						.setText(getText("label.deploy.cascadeCancel"))
//						.setClick("bc.deploy.cascadeCancel"));
//				// 强制删除
//				tb.addButton(getDefaultDeleteToolbarButton());
//			}
		}

		// 状态按钮组
		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatuses(),
				"status", 3, getText("deploy.status.tips")));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}

	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		Condition statusCondition = null;
		if (status != null && status.length() > 0) {
			statusCondition = new EqualsCondition("d.status_",
					Integer.parseInt(status));
		}
		return ConditionUtils.mix2AndCondition(statusCondition);
	}

	@Override
	protected Json getGridExtrasData() {
		Json json = new Json();
		if (status != null && status.length() > 0) {
			json.put("status", this.status);
		}
		if (json.isEmpty())
			return null;
		return json;
	}

	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace() + "/deploy/view.js";
	}

	public String json;
	private Long excludeId;
	private Boolean isCascade;// 是否级联删除

	public Long getExcludeId() {
		return excludeId;
	}

	public void setExcludeId(Long excludeId) {
		this.excludeId = excludeId;
	}

	public Boolean getIsCascade() {
		return isCascade;
	}

	public void setIsCascade(Boolean isCascade) {
		this.isCascade = isCascade;
	}

	/** 检查用户选择的流程是否已经发布 **/
	public String isReleased() {
		Json json = new Json();
		Long excludeId = this.deployService.isReleased(this.excludeId);
		if (excludeId != null) {// 已发布
			json.put("id", excludeId);
			json.put("released", "true");
			json.put("msg", getText("deploy.msg.released"));
		} else {// 未发布
			json.put("released", "false");
		}
		this.json = json.toString();
		return "json";
	}

	/** 发布流程 **/
	public String dodeployRelease() {
		Json json = new Json();
		this.deployService.dodeployRelease(this.excludeId);
		json.put("msg", getText("deploy.msg.release.success"));
		json.put("id", this.excludeId);
		this.json = json.toString();
		return "json";
	}

	/** 检查用户选择的流程是否已经发布 **/
	public String isStarted() {
		Json json = new Json();
		Deploy deploy = this.deployService.load(this.excludeId);
		Long excludeId = this.deployService.isStarted(deploy.getDeploymentId());
		if (excludeId != null) {// 已发起的流程id
			json.put("id", excludeId);
			json.put("started", "true");
			json.put("msg", getText("deploy.msg.started"));
		} else {// 未发布
			json.put("started", "false");
		}
		this.json = json.toString();
		return "json";
	}

	/** 取消发布 **/
	public String dodeployCancel() {
		Json json = new Json();
		this.deployService.dodeployCancel(this.excludeId, isCascade);
		json.put("msg", getText("deploy.msg.cancel.success"));
		json.put("id", this.excludeId);
		this.json = json.toString();
		return "json";
	}
	
	/** 禁用 **/
	public String dodeployStop() {
		Json json = new Json();
		this.deployService.dodeployStop(this.excludeId);
		json.put("msg", getText("deploy.msg.stop.success"));
		json.put("id", this.excludeId);
		this.json = json.toString();
		return "json";
	}
	
	/** 将状态改为使用中 **/
	public String dodeployChangeStatus() {
		Json json = new Json();
		this.deployService.dodeployChangeStatus(this.excludeId);
		json.put("msg", getText("deploy.msg.release.success"));
		json.put("id", this.excludeId);
		this.json = json.toString();
		return "json";
	}
	
	

	// ==高级搜索代码开始==
	@Override
	protected boolean useAdvanceSearch() {
		return true;
	}

	private DeployService deployService;

	@Autowired
	public void setDeployService(DeployService deployService) {
		this.deployService = deployService;
	}

	public JSONArray categorys;

	@Override
	protected void initConditionsFrom() throws Exception {
		this.categorys = OptionItem.toLabelValues(this.deployService
				.findCategoryOption());
	}
	// ==高级搜索代码结束==

}
