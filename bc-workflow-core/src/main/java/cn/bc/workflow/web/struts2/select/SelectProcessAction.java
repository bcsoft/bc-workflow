package cn.bc.workflow.web.struts2.select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.ConditionUtils;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.InCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.core.query.condition.impl.QlCondition;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.identity.web.SystemContext;
import cn.bc.web.formater.CalendarFormater;
import cn.bc.web.struts2.AbstractSelectPageAction;
import cn.bc.web.ui.html.grid.Column;
import cn.bc.web.ui.html.grid.HiddenColumn4MapKey;
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;
import cn.bc.web.ui.html.page.HtmlPage;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.deploy.domain.Deploy;

/**
 * 选择流程视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class SelectProcessAction extends AbstractSelectPageAction<Map<String, Object>> {
	private static final long serialVersionUID = 1L;
	public boolean isNewVersion=false;//是否只显示最新版本,否-显示全部版本
	private boolean constraint; //发起权限控制
	
	public boolean isNewVersion() {
		return isNewVersion;
	}

	public void setNewVersion(boolean isNewVersion) {
		this.isNewVersion = isNewVersion;
	}
	
	public boolean isConstraint() {
		return constraint;
	}

	public void setConstraint(boolean constraint) {
		this.constraint = constraint;
	}

	public boolean isManager() {
		SystemContext context = (SystemContext) this.getContext();
		// 配置权限：、超级管理员
		return !context.hasAnyRole(getText("key.role.bc.workflow"));
	}

	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("c.deploy_time_", Direction.Desc);
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select distinct a.id_,a.name_,e.version_,c.deploy_time_,a.key_");
		sql.append(" from act_re_procdef a");
		sql.append(" inner join act_re_deployment c on c.id_=a.deployment_id_");
		sql.append(" INNER join bc_wf_deploy e on e.deployment_id=c.id_");
		sql.append(" left JOIN bc_wf_deploy_actor da on da.did=e.id");

		sqlObject.setSql(sql.toString());
		
		// 注入参数
		sqlObject.setArgs(null);

		// 数据映射器
		sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
			public Map<String, Object> mapRow(Object[] rs, int rowNum) {
				Map<String, Object> map = new HashMap<String, Object>();
				int i = 0;
				map.put("id", rs[i++]);
				map.put("name", rs[i++]);
				map.put("version", rs[i++]);
				map.put("deploy_time", rs[i++]);
				map.put("key", rs[i++]);
				return map;
			}
		});
		return sqlObject;
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("a.id_", "id"));
		columns.add(new TextColumn4MapKey("a.name_", "name",
				getText("flow.name")).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("a.version_", "version",
				getText("flow.version"), 50).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("c.deploy_time", "deploy_time",
				getText("flow.deployTime"),150).setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		columns.add(new HiddenColumn4MapKey("key", "key"));
		return columns;
	}

	@Override
	protected String getHtmlPageTitle() {
		return this.getText("flow.select.title");
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "a.name_"};
	}

	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(600).setMinWidth(200)
				.setHeight(400).setMinHeight(200).setModal(true);
	}

	@Override
	protected Condition getGridSpecalCondition() {

		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		Long [] ids = context.getAttr(SystemContext.KEY_ANCESTORS);
		//Condition isNewVersionCondition = null; //显示最新版本
		Condition isUsersCondition = null; //发布是否分配使用者
		Condition userCondition = null; //当前登录用户id
		Condition groupCondition = null; //当前用户岗位列表
		Condition statusCondition = new EqualsCondition("e.status_", Deploy.STATUS_USING); //状态为使用中
		
		if(isManager() && constraint == true){//不是流程管理员并且有权限限制
			isUsersCondition = new QlCondition("e.id not in(select wda.did from  bc_wf_deploy_actor wda)"
					, (Object[]) null);
			userCondition = new EqualsCondition("da.aid",context.getUser().getId());
			groupCondition = new InCondition("da.aid",ids);
<<<<<<< HEAD
			/*if(isNewVersion){
				isNewVersionCondition = new QlCondition(
						"not exists(select 0 from act_re_procdef b where a.key_=b.key_ and a.version_<b.version_)",
						(Object[]) null);
			}*/
			return ConditionUtils.mix2AndCondition(statusCondition,
=======
//			if(isNewVersion){
//				isNewVersionCondition = new QlCondition(
//						"not exists(select 0 from act_re_procdef b where a.key_=b.key_ and a.version_<b.version_)",
//						(Object[]) null);
//			}
			return ConditionUtils.mix2AndCondition(isNewVersionCondition,statusCondition,
>>>>>>> flow
					ConditionUtils.mix2OrCondition(isUsersCondition,userCondition,groupCondition).setAddBracket(true));
		}
		return ConditionUtils.mix2AndCondition(statusCondition);
	}

	@Override
	protected Json getGridExtrasData() {
		Json json = new Json();
		if(isNewVersion)
			json.put("isNewVersion", isNewVersion);
		if(constraint)
			json.put("constraint", constraint);
		return json;
	}
	
	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace() + "/select/select.js";
	}

	@Override
	protected String getClickOkMethod() {
		return "bc.flow.selectDialog.clickOk";
	}

	@Override
	protected String getHtmlPageNamespace() {
		return this.getContextPath() + "/bc-workflow";
	}
	
	

	@Override
	protected String getGridDblRowMethod() {
		return "";
	}

	@Override
	protected String getGridRowLabelExpression() {
		return "['name'] + '.' + ['version']";
	}
	
	@Override
	protected HtmlPage buildHtmlPage() {
		return super.buildHtmlPage().setNamespace(
				this.getHtmlPageNamespace() + "/select");
	}

}
