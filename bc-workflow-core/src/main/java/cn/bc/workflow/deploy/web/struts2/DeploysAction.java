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

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.ConditionUtils;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.identity.web.SystemContext;
import cn.bc.option.domain.OptionItem;
import cn.bc.web.formater.CalendarFormater;
import cn.bc.web.formater.FileSizeFormater;
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
	public String status = String.valueOf(Deploy.STATUS_RELEASED);

	@Override
	public boolean isReadonly() {
		// 模板管理员或系统管理员
		SystemContext context = (SystemContext) this.getContext();
		// 配置权限：模板管理员
		return !context.hasAnyRole(getText("key.role.bc.workflow.deploy"),
				getText("key.role.bc.admin"));
	}

	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("d.order_");
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();

		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select d.id,d.uid_,d.order_ as orderNo,d.code,d.type_ as type,d.desc_,d.path,d.subject,d.source");
		sql.append(",au.actor_name as uname,d.file_date,am.actor_name as mname");
		sql.append(",d.modified_date,d.status_ as status,d.version_ as version");
		sql.append(",d.category,d.size_ as size");
		sql.append(" from bc_wf_deploy d");
		sql.append(" inner join bc_identity_actor_history au on au.id=d.author_id ");
		sql.append(" left join bc_identity_actor_history am on am.id=d.modifier_id");
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
				getText("deploy.status"), 40).setSortable(true)
				.setValueFormater(new KeyValueFormater(this.getStatuses())));
		columns.add(new TextColumn4MapKey("d.order_", "orderNo",
				getText("deploy.order"), 60).setSortable(true));
		columns.add(new TextColumn4MapKey("d.category", "category",
				getText("deploy.category"), 150).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.type_", "type",
				getText("deploy.type"), 150).setUseTitleFromLabel(true)
				.setValueFormater(new KeyValueFormater(this.getTypes())));
		columns.add(new TextColumn4MapKey("d.subject", "subject",
				getText("deploy.tfsubject"), 200).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.source", "source",
				getText("deploy.source"), 150).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.code", "code",
				getText("deploy.code"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.version_", "version",
				getText("deploy.version"), 100).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.path", "path",
				getText("deploy.tfpath")).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.size_", "size",
				getText("deploy.file.size"),110).setUseTitleFromLabel(true)
				.setValueFormater(new FileSizeFormater()));
		columns.add(new TextColumn4MapKey("d.desc_", "desc_",
				getText("deploy.desc"), 100).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("au.actor_name", "uname",
				getText("deploy.author"), 80));
		columns.add(new TextColumn4MapKey("d.file_date", "file_date",
				getText("deploy.fileDate"), 130)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		columns.add(new TextColumn4MapKey("am.actor_name", "mname",
				getText("deploy.modifier"), 80));
		columns.add(new TextColumn4MapKey("d.modified_date", "modified_date",
				getText("deploy.modifiedDate"), 130)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
		columns.add(new HiddenColumn4MapKey("uid", "uid"));
		columns.add(new HiddenColumn4MapKey("subject", "subject"));
		columns.add(new HiddenColumn4MapKey("source", "source"));
		columns.add(new HiddenColumn4MapKey("type", "type"));
		columns.add(new HiddenColumn4MapKey("path", "path"));
		return columns;
	}


	// 状态键值转换
	private Map<String, String> getStatuses() {
		Map<String, String> statuses = new LinkedHashMap<String, String>();
		statuses.put(String.valueOf(Deploy.STATUS_RELEASED),
				getText("deploy.status.released"));
		statuses.put(String.valueOf(Deploy.STATUS_NOT_RELEASE),
				getText("deploy.status.not.release"));
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
		return "['subject']";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "d.code", "am.actor_name", "d.path", "d.subject",
				"d.version_", "d.category", "d.source"};
	}

	@Override
	protected String getFormActionName() {
		return "deploy";
	}
	
	@Override
	protected String getHtmlPageNamespace(){
		return this.getContextPath() + "/bc-workflow";
	}


	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(850).setMinWidth(400)
				.setHeight(400).setMinHeight(300);
	}

	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();

		if (!this.isReadonly()) {
			// 新建按钮
			tb.addButton(this.getDefaultCreateToolbarButton());

			// 编辑按钮
			tb.addButton(this.getDefaultEditToolbarButton());

			tb.addButton(new ToolbarButton().setIcon("ui-icon-person")
					.setText(getText("label.deploy.release"))
					.setClick("bc.deploy.release"));
			
			tb.addButton(new ToolbarButton().setIcon("ui-icon-person")
					.setText(getText("label.deploy.releaseCancel"))
					.setClick("bc.deploy.releaseCancel"));
		}
		
		// 状态按钮组
		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatuses(),
				"status", 0, getText("deploy.status.tips")));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}

	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		Condition statusCondition = null;
		if(status != null && status.length() > 0){
			statusCondition = new EqualsCondition("d.status_",Integer.parseInt(status));
		}
		return ConditionUtils.mix2AndCondition(statusCondition);
	}
	
	@Override
	protected Json getGridExtrasData() {
		Json json = new Json();
		if(status != null && status.length() > 0){
			json.put("status", this.status);
		}
		if(json.isEmpty()) return null;
		return json;
	}
	
	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace() + "/deploy/view.js";
	}
	
	
	public String json;
	private Long excludeId;
	
	public Long getExcludeId() {
		return excludeId;
	}

	public void setExcludeId(Long excludeId) {
		this.excludeId = excludeId;
	}
	
	/** 检查用户选择的流程是否已经发布 **/
	public String isReleased() {
		Json json = new Json();
		Long excludeId = this.deployService.isReleased(this.excludeId);
		if(excludeId != null){//已发布
			json.put("id", excludeId);
			json.put("released", "true");
			json.put("msg", getText("deploy.msg.released"));
		}else{//未发布
			json.put("released", "false");
		}
		this.json = json.toString();
		return "json";
	}
	
	private String subject;
	private String source;
	private String path;
	private int type;
	
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	/** 发布流程 **/
	public String deployRelease(){
		Json json = new Json();
		if(this.type == 0){
			this.deployService.deployRelease4XML(this.excludeId,this.subject,this.source,this.path);
			json.put("msg", getText("deploy.msg.success"));
		}else if(this.type == 1){
			this.deployService.deployRelease4BAR(this.excludeId,this.subject,this.source,this.path);
			json.put("msg", getText("deploy.msg.success"));
		}
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
		this.categorys= OptionItem.toLabelValues(this.deployService.findCategoryOption());
	}
	// ==高级搜索代码结束==

	
}
