package cn.bc.workflow.historictaskinstance.web.struts2;

import cn.bc.core.Page;
import cn.bc.core.query.Query;
import cn.bc.core.query.cfg.PagingQueryConfig;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.core.query.condition.impl.QlCondition;
import cn.bc.core.util.DateUtils;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.db.jdbc.spring.JdbcTemplatePagingQuery;
import cn.bc.identity.web.SystemContext;
import cn.bc.option.domain.OptionItem;
import cn.bc.template.service.TemplateService;
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
import cn.bc.workflow.service.WorkspaceService;
import cn.bc.workflow.web.struts2.ViewAction;
import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;

import java.util.*;

/**
 * 经办、任务监控视图Action
 *
 * @author lbj
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class HistoricTaskInstancesAction extends ViewAction<Map<String, Object>> {
  @Autowired
  private TemplateService templateService;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static final long serialVersionUID = 1L;
  private static final int TASK_STATUS_DOING = 1;// 任务状态：处理中
  private static final int TASK_STATUS_SUSPENDED = 2;// 任务状态：暂停
  private static final int TASK_STATUS_FINISHED = 3;// 任务状态：已完成

  public String status;

  protected HistoricTaskInstanceService historicTaskInstanceService;

  @Autowired
  public void setHistoricTaskInstanceService(
    HistoricTaskInstanceService historicTaskInstanceService) {
    this.historicTaskInstanceService = historicTaskInstanceService;
  }

  @Override
  public boolean isReadonly() {
    SystemContext context = (SystemContext) this.getContext();
    // 配置权限：、超级管理员
    return !context.hasAnyRole(getText("key.role.bc.admin"),
      getText("key.role.bc.workflow"));
  }

  @Override
  protected OrderCondition getGridOrderCondition() {
    return new OrderCondition("t.start_time_", Direction.Desc);
  }

  /**
   * SQL分页查询语句及参数配置
   */
  protected PagingQueryConfig getPagingQueryConfig() {
    // 加载模板，获得查询SQL
    String querySql = this.templateService.getContent("HISTORIC_TASK_INSTANCE");
    String countSql = this.templateService.getContent("HISTORIC_TASK_INSTANCE_COUNT");

    // 查询对象
    cn.bc.core.query.cfg.impl.PagingQueryConfig cfg =
      new cn.bc.core.query.cfg.impl.PagingQueryConfig(querySql, countSql, null);

    // 分页参数
    Page<Map<String, Object>> p = getPage();
    if (p != null) {
      cfg.setLimit(p.getPageSize());
      cfg.setOffset(p.getFirstResult());
    }
    return cfg;
  }

  @Override
  protected Query<Map<String, Object>> getQuery() {
    JdbcTemplatePagingQuery<Map<String, Object>> jdbcQuery =
      new JdbcTemplatePagingQuery<>(jdbcTemplate, getPagingQueryConfig(), null);

    jdbcQuery.condition(this.getGridCondition());
    return jdbcQuery;
  }

  @Override
  protected SqlObject<Map<String, Object>> getSqlObject() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected List<Column> getGridColumns() {
    List<Column> columns = new ArrayList<>();
    columns.add(new IdColumn4MapKey("t.id_", "task_id"));

    // 状态
    columns.add(new TextColumn4MapKey("", "task_status", getText("flow.task.status"), 50)
      .setSortable(true).setValueFormater(new EntityStatusFormater(getStatus())));
    // 流水号
    columns.add(new TextColumn4MapKey("wf_code", "wf_code", getText("flow.workFlowCode"), 120)
      .setSortable(true).setUseTitleFromLabel(true));
    // 主题
    columns.add(new TextColumn4MapKey("wf_subject", "wf_subject", getText("flow.task.subject"), 300)
      .setSortable(true).setUseTitleFromLabel(true));
    // 任务名称
    columns.add(new TextColumn4MapKey("t.name_", "task_name", getText("flow.task.name"), 200)
      .setSortable(true).setUseTitleFromLabel(true));
    // 办理人
    columns.add(new TextColumn4MapKey("a.name", "assignee", getText("flow.task.actor"), 120)
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
    // 任务 key 值
    columns.add(new TextColumn4MapKey("t.task_def_key_", "task_def_key", "任务key值", 200)
      .setSortable(true).setUseTitleFromLabel(true));
    //空列
    columns.add(new TextColumn4MapKey("", "", ""));

    columns.add(new HiddenColumn4MapKey("deployId", "deployment_id"));
    columns.add(new HiddenColumn4MapKey("procinstId", "process_id"));
    columns.add(new HiddenColumn4MapKey("procinstName", "process_name"));
    columns.add(new HiddenColumn4MapKey("procinstKey", "process_key"));
    columns.add(new HiddenColumn4MapKey("procinstTaskName", "task_name"));
    columns.add(new HiddenColumn4MapKey("procinstTaskKey", "task_def_key"));
    columns.add(new HiddenColumn4MapKey("subject", "wf_subject"));
    columns.add(new HiddenColumn4MapKey("accessControlDocType", "access_control_doc_type"));
    columns.add(new HiddenColumn4MapKey("accessControlDocName", "access_control_doc_name"));
    return columns;
  }

  @Override
  protected String getGridRowLabelExpression() {
    return "['wf_subject']";
  }

  @Override
  protected String[] getGridSearchFields() {
    return new String[]{
      "t.name_"// 任务名称
      , "t.task_def_key_"// 任务 Key 值
      , "a.name"// 任务办理人
      , "pd.name_"// 流程名称
      , "pi.info->>'subject'", "pi.info->>'wf_code'"// 流程标题、流水号
    };
  }

  @Override
  protected String getFormActionName() {
    return "historicTaskInstance";
  }

  @Override
  protected PageOption getHtmlPageOption() {
    return super.getHtmlPageOption().setWidth(850).setMinWidth(400)
      .setHeight(400).setMinHeight(300);
  }

  @Override
  protected Toolbar getHtmlPageToolbar() {
    Toolbar tb = new Toolbar();
    // 查看
    tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
      .setText(getText("label.read"))
      .setClick("bc.historicTaskInstanceSelectView.open"));

    tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(), "status", 3,
      getText("title.click2changeSearchStatus")));

    // 搜索按钮
    tb.addButton(this.getDefaultSearchToolbarButton());

    return tb;
  }

  /**
   * 状态值转换:处理中|暂停|已完成|全部
   */
  protected Map<String, String> getStatus() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(String.valueOf(TASK_STATUS_DOING), getText("flow.task.status.doing"));
    map.put(String.valueOf(TASK_STATUS_SUSPENDED), getText("flow.task.status.suspended"));
    map.put(String.valueOf(TASK_STATUS_FINISHED), getText("flow.task.status.finished"));
    map.put("", getText("bc.status.all"));
    return map;
  }

  /**
   * 流程状态值转换:流转中|已暂停|已结束|全部
   */
  protected Map<String, String> getPStatus() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(String.valueOf(SuspensionState.ACTIVE.getStateCode()),
      getText("done.status.processing"));
    map.put(String.valueOf(SuspensionState.SUSPENDED.getStateCode()),
      getText("done.status.suspended"));
    map.put(String.valueOf(WorkspaceService.FLOWSTATUS_COMPLETE),
      getText("done.status.finished"));
    map.put("", getText("bc.status.all"));
    return map;
  }

  @Override
  protected Condition getGridSpecalCondition() {
    // 状态条件
    AndCondition ac = new AndCondition();
    if (status != null && status.length() > 0) {
      String[] ss = status.split(",");
      if (ss.length == 1) {
        String sqlstr = "";
        if (ss[0].equals(String.valueOf(TASK_STATUS_DOING))) {// 处理中
          sqlstr += " t.end_time_ is null";
          sqlstr += " and ((pd.suspension_state_ = "
            + SuspensionState.ACTIVE.getStateCode() + ")";
          sqlstr += " and (e.suspension_state_ ="
            + SuspensionState.ACTIVE.getStateCode() + "))";
        } else if (ss[0].equals(String.valueOf(TASK_STATUS_SUSPENDED))) {// 暂停
          sqlstr += " t.end_time_ is null";
          sqlstr += " and (pd.suspension_state_ = "
            + SuspensionState.SUSPENDED.getStateCode();
          sqlstr += " or e.suspension_state_ ="
            + SuspensionState.SUSPENDED.getStateCode() + ")";
        } else if (ss[0].equals(String.valueOf(TASK_STATUS_FINISHED))) {// 已完成
          sqlstr += " t.end_time_ is not null";
        }
        ac.add(new QlCondition(sqlstr, new Object[]{}));
      }
    }

    ac.add(new IsNullCondition("e.parent_id_"));

    return ac.isEmpty() ? null : ac;
  }

  @Override
  protected void extendGridExtrasData(JSONObject json) throws JSONException {
    // 状态条件
    if (status != null && status.length() > 0)
      json.put("status", status);
  }

  @Override
  protected String getGridDblRowMethod() {
    return "bc.historicTaskInstanceSelectView.open";
  }

  @Override
  protected String getHtmlPageJs() {
    return this.getModuleContextPath() + "/historictaskinstance/view.js";
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
    List<String> values = this.historicTaskInstanceService.findProcessNames();
    List<Map<String, String>> list = new ArrayList<>();
    Map<String, String> map;
    for (String value : values) {
      map = new HashMap<>();
      map.put("key", value);
      map.put("value", value);
      list.add(map);
    }
    this.processList = OptionItem.toLabelValues(list);

    values = this.historicTaskInstanceService.findTaskNames();
    list = new ArrayList<>();
    for (String value : values) {
      map = new HashMap<>();
      map.put("key", value);
      map.put("value", value);
      list.add(map);
    }
    this.taskList = OptionItem.toLabelValues(list);
  }
  // ==高级搜索代码结束==


  public String startFlowKey;//流程的key
  public String processData;/* json格式 {
									procinstId:流程实例id,
									procinstName:流程实例名称,
									procinstKey:流程Key,
									procinstTaskName:任务名称,
									procinstTaskKey:任务key,
									procinstTaskId:任务id  }*/

  public String startFlow() throws Exception {
    Json json = new Json();
    String procinstId = this.historicTaskInstanceService.doStartFlow(this.startFlowKey, this.processData);
    json.put("success", true);
    json.put("procinstId", procinstId);
    this.json = json.toString();
    return "json";
  }

}
