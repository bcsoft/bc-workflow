package cn.bc.workflow.historictaskinstance.web.struts2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.IsNotNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.core.util.DateUtils;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.identity.web.SystemContext;
import cn.bc.web.formater.CalendarFormater;
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
public class HistoricTaskInstancesAction extends ViewAction<Map<String, Object>> {
	private static final long serialVersionUID = 1L;
	public boolean my = false;// 是否从我的经办
	public String procInstId;//流程实例id

	@Override
	public boolean isReadonly() {
		SystemContext context = (SystemContext) this.getContext();
		// 配置权限：、超级管理员
		return !context.hasAnyRole(getText("key.role.bc.admin"),getText("key.role.bc.workflow"));
	}

	@Override
	protected OrderCondition getGridOrderCondition() {
		return new OrderCondition("c.name_", Direction.Asc).add("a.start_time_", Direction.Asc);
	}

	@Override
	protected SqlObject<Map<String, Object>> getSqlObject() {
		SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
		// 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
		StringBuffer sql = new StringBuffer();
		sql.append("select a.id_,c.name_ as category,a.name_ as subject,a.start_time_,a.end_time_,d.first_ as receiver,a.duration_,a.proc_inst_id_");
		sql.append(" from act_hi_taskinst a");
		sql.append(" inner join act_hi_procinst b on b.proc_def_id_=a.proc_def_id_");
		sql.append(" inner join act_re_procdef c on c.id_=a.proc_def_id_");
		sql.append(" left join act_id_user d on d.id_=a.assignee_");
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
				map.put("subject", rs[i++]);
				map.put("start_time", rs[i++]);
				map.put("end_time", rs[i++]);
				map.put("receiver", rs[i++]);
				map.put("duration", rs[i++]);
				if(map.get("end_time")!=null){
					map.put("status", "完成");
				}else
					map.put("status", "未完成");
				
				//格式化耗时
				if(map.get("duration")!=null)
					map.put("frmDuration", 
							DateUtils.getWasteTime(Long.parseLong(map.get("duration").toString())));
				
				map.put("procinstid",rs[i++]);
				return map;
			}
		});
		return sqlObject;
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("a.id_", "id"));
		if(!my){
			columns.add(new TextColumn4MapKey("", "status",
					getText("done.status"), 60).setSortable(true));
		}
		columns.add(new TextColumn4MapKey("c.name_", "category",
				getText("done.category"), 120).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("a.name_", "subject",
				getText("done.subject"), 100).setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("d.first_", "receiver",
				getText("done.receiver"),80));
		columns.add(new TextColumn4MapKey("a.start_time_", "start_time",
				getText("done.startTime"),190).setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd hh:mm:ss.SSS")));
		columns.add(new TextColumn4MapKey("a.end_time_", "end_time",
				getText("done.endTime"),190).setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd hh:mm:ss.SSS")));
		columns.add(new TextColumn4MapKey("a.duration_", "frmDuration",
				getText("done.duration")));
		columns.add(new  HiddenColumn4MapKey("procinstid", "procinstid"));
		return columns;
	}


	@Override
	protected String getGridRowLabelExpression() {
		return my?"'我的经办：'+['subject']" :"['subject']";
	}

	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "a.first_", "a.name_", "c.name_" };
	}

	@Override
	protected String getFormActionName() {
		return my ? "myDone" : "historicTaskInstance";
	}

	@Override
	protected PageOption getHtmlPageOption() {
		return super.getHtmlPageOption().setWidth(800).setMinWidth(400)
				.setHeight(400).setMinHeight(300).setMinimizable(false);
	}

	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		// 查看
		tb.addButton(new ToolbarButton()
		.setIcon("ui-icon-arrowthickstop-1-s")
		.setText(getText("label.read"))
		.setClick("bc.historicTaskInstanceSelectView.clickOk"));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}

	@Override
	protected Condition getGridSpecalCondition() {
		
		// 状态条件
		AndCondition ac = new AndCondition();
		
		if(procInstId!=null)
			ac.add(new EqualsCondition("a.proc_inst_id_",this.procInstId));
		
		if(my){
			SystemContext context = (SystemContext) this.getContext();
			ac.add(new EqualsCondition("a.assignee_",context.getUser().getCode()));
			//结束时间不能为空
			ac.add(new IsNotNullCondition("a.end_time_"));
		}
		return ac;
	}

	@Override
	protected Json getGridExtrasData() {
		Json json = new Json();
		if(procInstId!=null&&procInstId.length()>0)
			json.put("procInstId", procInstId);
		
		if(my){
			json.put("my", my);
		}
		
		return json;
	}
	
	@Override
	protected String getGridDblRowMethod() {
		return "bc.historicTaskInstanceSelectView.clickOk";
	}
	
	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace() + "-workflow/historictaskinstance/select.js";
	}

	// ==高级搜索代码开始==
	@Override
	protected boolean useAdvanceSearch() {
		return false;
	}


	@Override
	protected void initConditionsFrom() throws Exception {
	
	}
	// ==高级搜索代码结束==

}
