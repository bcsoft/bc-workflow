package cn.bc.workflow.historictaskinstance.web.struts2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.IsNotNullCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.util.DateUtils;
import cn.bc.identity.web.SystemContext;
import cn.bc.option.domain.OptionItem;
import cn.bc.web.formater.AbstractFormater;
import cn.bc.web.formater.CalendarFormater;
import cn.bc.web.formater.EntityStatusFormater;
import cn.bc.web.ui.html.grid.Column;
import cn.bc.web.ui.html.grid.HiddenColumn4MapKey;
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;

/**
 * 我的经办任视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class MyHistoricTaskInstancesAction extends HistoricTaskInstancesAction {
	private static final long serialVersionUID = 1L;

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<>();
		columns.add(new IdColumn4MapKey("t.id_", "task_id"));
        // 流水号
        columns.add(new TextColumn4MapKey("wf_code", "wf_code", getText("flow.workFlowCode"), 120)
                .setSortable(true).setUseTitleFromLabel(true));
        // 主题
        columns.add(new TextColumn4MapKey("wf_subject", "wf_subject", getText("flow.task.subject"), 300)
                .setSortable(true).setUseTitleFromLabel(true));
        // 任务名称
        columns.add(new TextColumn4MapKey("t.name_", "task_name", getText("flow.task.name"), 200)
                .setSortable(true).setUseTitleFromLabel(true));
        //办理期限
        columns.add(new TextColumn4MapKey("t.due_date_", "due_date", getText("done.dueDate"), 130)
                .setSortable(true).setUseTitleFromLabel(true)
                .setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm")));
        // 发起时间
        columns.add(new TextColumn4MapKey("t.start_time_", "start_time", getText("flow.task.startTime"), 150)
                .setSortable(true).setUseTitleFromLabel(true)
                .setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
        // 完成时间
        columns.add(new TextColumn4MapKey("t.end_time_", "end_time", getText("flow.task.endTime"), 150)
                .setSortable(true).setUseTitleFromLabel(true)
                .setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
        // 耗时
        columns.add(new TextColumn4MapKey("t.duration_", "duration", getText("flow.task.duration"), 80)
                .setSortable(true).setValueFormater(new AbstractFormater<String>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public String format(Object context, Object value) {
                        Object duration_obj = ((Map<String, Object>) context).get("duration");
                        if (duration_obj == null) return null;
                        return DateUtils.getWasteTime(Long.parseLong(duration_obj.toString()));
                    }
                }));
        // 所属流程
        columns.add(new TextColumn4MapKey("pd.name_", "process_name", getText("flow.task.category"), 180)
                .setSortable(true).setUseTitleFromLabel(true));
        // 流程状态
        columns.add(new TextColumn4MapKey("", "pstatus", getText("flow.task.pstatus"), 80)
                .setSortable(true).setValueFormater(new EntityStatusFormater(getPStatus())));

		columns.add(new HiddenColumn4MapKey("assignee", "assignee"));
		columns.add(new HiddenColumn4MapKey("procinstId", "process_id"));
		columns.add(new HiddenColumn4MapKey("procinstName", "process_name"));
		columns.add(new HiddenColumn4MapKey("procinstKey", "process_key"));
		columns.add(new HiddenColumn4MapKey("procinstTaskName", "task_name"));
		columns.add(new HiddenColumn4MapKey("procinstTaskKey", "task_def_key"));
		columns.add(new HiddenColumn4MapKey("subject", "wf_subject"));
		return columns;
	}

	@Override
	protected String getFormActionName() {
		return "myDone" ;
	}


	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		// 查看
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.historicTaskInstanceSelectView.open"));
		
		tb.addButton(new ToolbarButton().setIcon("ui-icon-search")
				.setText(getText("flow.task.flow"))
				.setClick("bc.myHistoricTaskInstanceSelectView.viewflow"));
		
		tb.addButton(new ToolbarButton().setIcon("ui-icon-wrench")
				.setText(getText("flow.task.requirement"))
				.setClick("bc.historicTaskInstanceSelectView.requirement"));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}
	

	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();

		SystemContext context = (SystemContext) this.getContext();
		ac.add(new EqualsCondition("a.code", context.getUser()
				.getCode()));
		// 结束时间不能为空
		ac.add(new IsNotNullCondition("t.end_time_"));
		ac.add(new IsNullCondition("e.parent_id_"));
		
		return ac.isEmpty() ? null : ac;
	}


	@Override
	protected String getHtmlPageJs() {
		return super.getHtmlPageJs()+","
				+this.getModuleContextPath() + "/historictaskinstance/my/view.js";
	}

	// ==高级搜索代码开始==
	@Override
	protected boolean useAdvanceSearch() {
		return true;
	}

	public JSONArray processList;
	
	public JSONArray taskList;

	@Override
	protected void initConditionsFrom() throws Exception {
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		String account=context.getUserHistory().getCode();
		List<String> values=this.historicTaskInstanceService.findProcessNames(account, true);
		List<Map<String,String>> list = new ArrayList<>();
		Map<String,String> map;
		for(String value : values){
			map = new HashMap<>();
			map.put("key", value);
			map.put("value", value);
			list.add(map);
		}
		this.processList = OptionItem.toLabelValues(list);
		
		values=this.historicTaskInstanceService.findTaskNames(account, true);
		list = new ArrayList<>();
		for(String value : values){
			map = new HashMap<>();
			map.put("key", value);
			map.put("value", value);
			list.add(map);
		}
		this.taskList = OptionItem.toLabelValues(list);
	}
	// ==高级搜索代码结束==

}
