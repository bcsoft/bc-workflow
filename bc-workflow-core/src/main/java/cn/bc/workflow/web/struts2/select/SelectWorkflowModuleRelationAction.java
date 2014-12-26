package cn.bc.workflow.web.struts2.select;

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.IsNotNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.web.formater.CalendarFormater;
import cn.bc.web.formater.EntityStatusFormater;
import cn.bc.web.struts2.AbstractSelectPageAction;
import cn.bc.web.ui.html.grid.Column;
import cn.bc.web.ui.html.grid.HiddenColumn4MapKey;
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;
import cn.bc.web.ui.html.page.HtmlPage;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.workflow.service.WorkspaceServiceImpl_old;
import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.*;

/**
 * 选择流程模块关系视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class SelectWorkflowModuleRelationAction extends AbstractSelectPageAction<Map<String, Object>> {
	private static final long serialVersionUID = 1L;
	public Long moduleId;//模块id
	public String moduleType;//模块类型
	public String processKey;//流程编码
	public Boolean complete=false;//已完成的流程


	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("p.start_time_", Direction.Desc);
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select p.id_,d.name_,d.key_,p.start_time_,p.end_time_,e.suspension_state_,getprocessinstancesubject(r.pid)");
		sql.append(" from bc_wf_module_relation r");
		sql.append(" inner join act_hi_procinst p on p.id_=r.pid");
		sql.append(" inner join act_re_procdef d on d.id_=p.proc_def_id_");
		sql.append(" left join act_ru_execution e on e.proc_inst_id_=p.id_");

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
				map.put("key", rs[i++]);
				map.put("start_time", rs[i++]);
				map.put("end_time", rs[i++]);
				map.put("suspension_state", rs[i++]);
				map.put("subject", rs[i++]);
				
				if (map.get("end_time") != null) {//已结束
					map.put("status", WorkspaceServiceImpl_old.COMPLETE);
				} else {
					if(String.valueOf(SuspensionState.ACTIVE.getStateCode()).equals(map.get("suspension_state").toString())){//流转中
						map.put("status", String.valueOf(SuspensionState.ACTIVE.getStateCode()));
					}else if(String.valueOf(SuspensionState.SUSPENDED.getStateCode()).equals(map.get("suspension_state").toString())){//已暂停
						map.put("status", String.valueOf(SuspensionState.SUSPENDED.getStateCode()));
					}
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
		columns.add(new TextColumn4MapKey("getProcessInstanceSubject(r.pid)", "subject",
				getText("flow.instance.subject"),200).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("p.name_", "name",
				getText("flow.instance.name"),200).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("p.start_time_", "start_time",
				getText("flow.instance.startTime"), 150).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		// 状态
		columns.add(new TextColumn4MapKey("", "status",
				getText("flow.instance.status")).setSortable(true)
				.setValueFormater(new EntityStatusFormater(getStatus())));

		columns.add(new HiddenColumn4MapKey("key", "key"));
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
		map.put(String.valueOf(WorkspaceServiceImpl_old.COMPLETE),
				getText("flow.instance.status.finished"));
		map.put("", getText("bc.status.all"));
		return map;
	}

	@Override
	protected String getHtmlPageTitle() {
		return this.getText("flow.select.module.title");
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "p.name_","getProcessInstanceSubject(r.pid)"};
	}

	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(400).setMinWidth(200)
				.setHeight(360).setMinHeight(200).setModal(true);
	}

	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();
		if(moduleId!=null)
			ac.add(new EqualsCondition("r.mid",moduleId));
		if(moduleType!=null&&moduleType.length()>0)
			ac.add(new EqualsCondition("r.mtype",moduleType));
		if(processKey!=null&&processKey.length()>0)
			ac.add(new EqualsCondition("d.key_",processKey));
		if(complete!=null&&complete)
			ac.add(new IsNotNullCondition("p.end_time_"));
			
		return ac.isEmpty()?null:ac;

	}

	@Override
    protected void extendGridExtrasData(JSONObject json) throws JSONException {
		if(moduleId!=null)
			json.put("moduleId", moduleId);
		if(moduleType!=null&&moduleType.length()>0)
			json.put("moduleType", moduleType);
		if(processKey!=null&&processKey.length()>0)
			json.put("processKey", processKey);
		if(complete!=null&&complete)
			json.put("complete", complete);
	}
	
	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace() + "/modulerelation/select.js";
	}

	@Override
	protected String getClickOkMethod() {
		return "bc.flow.selectModuleRelationDialog.clickOk";
	}

	@Override
	protected String getHtmlPageNamespace() {
		return this.getContextPath() + "/bc-workflow";
	}
	

	@Override
	protected String getGridRowLabelExpression() {
		return "['name']";
	}
	
	@Override
	protected HtmlPage buildHtmlPage() {
		return super.buildHtmlPage().setNamespace(
				this.getHtmlPageNamespace() + "/select");
	}

}
