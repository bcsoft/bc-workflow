package cn.bc.workflow.todo.web.struts2;

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.*;
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
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;
import cn.bc.workflow.todo.service.TodoService;
import cn.bc.workflow.web.struts2.ViewAction;
import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.*;

/**
 * 任务监控视图Action
 *
 * @author wis
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class TodoManagesAction extends ViewAction<Map<String, Object>> {
  private static final long serialVersionUID = 1L;

  public String status;

  @Override
  public boolean isReadonly() {
    SystemContext context = (SystemContext) this.getContext();
    return !context.hasAnyRole(getText("key.role.bc.admin"), getText("key.role.bc.workflow"));
  }

  public boolean isDelegate() {
    // 任务委托角色
    SystemContext context = (SystemContext) this.getContext();
    return context.hasAnyRole(getText("key.role.bc.workflow"), getText("key.role.bc.workflow.delegate"));
  }

  public boolean isAssign() {
    // 任务分派角色
    SystemContext context = (SystemContext) this.getContext();
    return context.hasAnyRole(getText("key.role.bc.workflow"), getText("key.role.bc.workflow.delegate"));
  }

  @Override
  protected OrderCondition getGridOrderCondition() {
    return new OrderCondition("a.create_time_", Direction.Desc);
  }

  @Override
  protected SqlObject<Map<String, Object>> getSqlObject() {

    SqlObject<Map<String, Object>> sqlObject = new SqlObject<Map<String, Object>>();
    // 构建查询语句,where和order by不要包含在sql中(要统一放到condition中)
    StringBuffer sql = new StringBuffer();
    sql.append("!!select a.id_,b.suspension_state_ as status,a.proc_inst_id_ as procinstid,a.name_ as taskname,a.due_date_ as duedate,a.create_time_ as createtime");
    sql.append(",d.name_ as processname,a.description_ as desc_,a.assignee_ as assignee,c.name as aname");
    sql.append(",case when a.assignee_ is not null then 1 else 2 end as type_");
    sql.append(",getprocessinstancesubject(a.proc_inst_id_) as subject");
    //候选人
    sql.append(",(select string_agg(g2.name,',') from act_ru_identitylink t2 inner join bc_identity_actor g2 on g2.code=t2.user_id_ where t2.task_id_= a.id_) as users");
    //候选岗位
    sql.append(",(select string_agg(g1.name,',') from act_ru_identitylink t1 inner join bc_identity_actor g1 on g1.code=t1.group_id_ where t1.task_id_= a.id_) as groups");
    sql.append(",(select id from bc_wf_deploy deploy where deploy.deployment_id=d.deployment_id_) as deploy_id, w.text_ as wf_code");
    sql.append(" !!from act_ru_task a");
    sql.append(" inner join act_ru_execution b on b.proc_inst_id_ = a.proc_inst_id_");
    sql.append(" inner join act_re_procdef d on d.id_ = a.proc_def_id_");
    sql.append(" left join bc_identity_actor c on c.code=a.assignee_");
    sql.append(" left join (");
    sql.append(" select d.proc_inst_id_, d.text_");
    sql.append(" from act_hi_detail d");
    sql.append(" where d.name_ = 'wf_code'");
    sql.append(" ) w on w.proc_inst_id_ = a.proc_inst_id_");

    sqlObject.setSql(sql.toString());

    // 注入参数
    sqlObject.setArgs(null);

    // 数据映射器
    sqlObject.setRowMapper(new RowMapper<Map<String, Object>>() {
      public Map<String, Object> mapRow(Object[] rs, int rowNum) {
        Map<String, Object> map = new HashMap<String, Object>();
        int i = 0;
        map.put("id", rs[i++]);
        map.put("status", rs[i++]);
        map.put("procinstId", rs[i++]); //流程实例id
        map.put("taskName", rs[i++]); // 标题
        map.put("dueDate", rs[i++]); // 办理期限
        map.put("createTime", rs[i++]); //  发送时间
        map.put("processName", rs[i++]); //  分类
        map.put("desc", rs[i++]); // 附加说明
        map.put("assignee", rs[i++]); //  任务处理人code
        map.put("aname", rs[i++]); //  办理人
        map.put("type", rs[i++]); //  办理人
        map.put("subject", rs[i++]);
        map.put("users", rs[i++]); //  候选人员列表
        map.put("groups", rs[i++]); //  候选岗位列表
        map.put("deployId", rs[i++]);
        map.put("wf_code", rs[i++]);

        map.put("accessControlDocType", "ProcessInstance");
        if (map.get("subject") != null && !map.get("subject").toString().equals("")) {
          map.put("accessControlDocName", map.get("subject").toString());
        } else {
          map.put("accessControlDocName", map.get("processName").toString());
        }
        return map;
      }
    });
    return sqlObject;
  }

  @Override
  protected Condition getGridSpecalCondition() {
    // 状态条件
    AndCondition ac = new AndCondition();
    //状态判断
    if (status != null && status.length() > 0) {
      String[] ss = status.split(",");
      if (ss.length == 1) {
        ac.add(new EqualsCondition("b.suspension_state_", Integer.valueOf(ss[0])));
      } else {
        ac.add(new InCondition("b.suspension_state_", StringUtils.stringArray2IntegerArray(ss)));
      }
    }

    ac.add(new IsNullCondition("b.parent_id_"));

    return ac;
  }

  @Override
  protected Toolbar getHtmlPageToolbar() {
    Toolbar tb = new Toolbar();

    // 查看按钮
    tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
      .setText(getText("label.read"))
      .setClick("bc.todoView.open"));

    if (this.isDelegate()) {
      tb.addButton(new ToolbarButton().setIcon("ui-icon-person")
        .setText(getText("label.delegate.task"))
        .setClick("bc.todoView.delegateTask"));
    }

    if (this.isAssign()) {
      tb.addButton(new ToolbarButton().setIcon("ui-icon-flag")
        .setText(getText("label.assign.task"))
        .setClick("bc.todoView.assignTask"));
    }

    tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(),
      "status", 2, getText("title.click2changeSearchStatus")));

    // 搜索按钮
    tb.addButton(getDefaultSearchToolbarButton());

    return tb;
  }

  @Override
  protected PageOption getHtmlPageOption() {
    return super.getHtmlPageOption().setWidth(850).setMinWidth(200)
      .setHeight(400).setMinHeight(200);
  }

  /**
   * 流程状态值转换:流转中|已暂停|全部
   */
  protected Map<String, String> getProcessStatus() {
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put(String.valueOf(SuspensionState.ACTIVE.getStateCode()),
      getText("flow.instance.status.processing"));
    map.put(String.valueOf(SuspensionState.SUSPENDED.getStateCode()),
      getText("flow.instance.status.suspended"));
    map.put("", getText("bc.status.all"));
    return map;
  }


  /**
   * 任务状态值转换:处理中|已暂停|全部
   */
  protected Map<String, String> getStatus() {
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put(String.valueOf(SuspensionState.ACTIVE.getStateCode()),
      getText("flow.task.status.doing"));
    map.put(String.valueOf(SuspensionState.SUSPENDED.getStateCode()),
      getText("flow.instance.status.suspended"));
    map.put("", getText("bc.status.all"));
    return map;
  }

  @Override
  protected List<Column> getGridColumns() {
    List<Column> columns = new ArrayList<Column>();
    columns.add(new IdColumn4MapKey("a.id_", "id"));

    //流程状态
    columns.add(new TextColumn4MapKey("b.suspension_state_", "status",
      getText("flow.task.pstatus"), 60).setSortable(true)
      .setValueFormater(new EntityStatusFormater(getProcessStatus())));
    // 流水号
    columns.add(new TextColumn4MapKey("w.wf_code", "wf_code",
      getText("flow.workFlowCode"), 120).setSortable(true)
      .setUseTitleFromLabel(true));
    // 主题
    columns.add(new TextColumn4MapKey(
      "getProcessInstanceSubject(a.proc_inst_id_)", "subject",
      getText("flow.task.subject"), 300).setSortable(true)
      .setUseTitleFromLabel(true));
    // 待办任务
    columns.add(new TextColumn4MapKey("a.name_", "taskName",
      getText("todo.personal.artName"), 200).setSortable(true)
      .setUseTitleFromLabel(false)
      .setValueFormater(new AbstractFormater<String>() {

        @Override
        public String format(Object context, Object value) {
          @SuppressWarnings("unchecked")
          Map<String, Object> task = (Map<String, Object>) context;
          boolean flag = false;
          if (task.get("dueDate") != null) {//办理时间是否过期
            Date d1 = (Date) task.get("dueDate");
            Date d2 = new Date();
            flag = d1.before(d2);
          }

          if ("2".equals(task.get("type").toString())) {//岗位任务
            if (flag) {
              return "<div title=\"此任务已过期,岗位任务\"><span style=\"float: left;\" class=\"ui-icon ui-icon-clock\"></span>" +
                "<span style=\"float: left;\" class=\"ui-icon ui-icon-person\"></span>"
                + "&nbsp;" + "<span>" + task.get("taskName") + "</span>" + "</div>";
            } else {
              return "<div title=\"岗位任务\"><span style=\"float: left;\" class=\"ui-icon ui-icon-person\"></span>"
                + "&nbsp;" + "<span>" + task.get("taskName") + "</span>" + "</div>";
            }
          } else {//个人任务
            if (flag) {
              return "<div title=\"此任务已过期\"><span style=\"float: left;\" class=\"ui-icon ui-icon-clock\"></span>"
                + "&nbsp;" + "<span>" + task.get("taskName") + "</span>" + "</div>";
            } else {
              return (String) task.get("taskName");
            }
          }
        }

      }));
    // 办理人
    columns.add(new TextColumn4MapKey("c.name", "aname",
      getText("todo.personal.assignee"), 120).setSortable(true)
      .setUseTitleFromLabel(true));
    // 候选岗位
    columns.add(new TextColumn4MapKey("groups", "groups",
      getText("todo.personal.groupIds"), 150).setSortable(true)
      .setUseTitleFromLabel(true));
    // 办理期限
    columns.add(new TextColumn4MapKey("art.due_date_", "dueDate",
      getText("todo.personal.dueDate"), 140).setSortable(true)
      .setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));

    // 创建时间
    columns.add(new TextColumn4MapKey("art.create_time_", "createTime",
      getText("todo.personal.createTime"), 140).setSortable(true)
      .setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));

    // 候选人
		/*columns.add(new TextColumn4MapKey("users", "users",
				getText("todo.personal.userIds")).setSortable(true)
				.setUseTitleFromLabel(true));*/
    // 分类
    columns.add(new TextColumn4MapKey("d.name_", "processName",
      getText("todo.personal.arpName"), 180)
      .setSortable(true).setUseTitleFromLabel(true));

    columns.add(new HiddenColumn4MapKey("procinstId", "procinstId"));
    columns.add(new HiddenColumn4MapKey("type", "type"));
    columns.add(new HiddenColumn4MapKey("deployId", "deployId"));
    columns.add(new HiddenColumn4MapKey("accessControlDocType", "accessControlDocType"));
    columns.add(new HiddenColumn4MapKey("accessControlDocName", "accessControlDocName"));
    return columns;
  }

  @Override
  protected String getHtmlPageTitle() {
    return this.getText("manage.title");
  }

  @Override
  protected String getFormActionName() {
    return "todo/manage";
  }

  @Override
  protected String getGridRowLabelExpression() {
    return "['taskName']";
  }

  @Override
  /** 获取表格双击行的js处理函数名 */
  protected String getGridDblRowMethod() {
    return "bc.todoView.open";
  }

  @Override
  protected String[] getGridSearchFields() {
    return new String[]{"d.name_", "a.name_", "a.description_", "a.assignee_"
      , "c.name", "w.text_"
      , "getProcessInstanceSubject(a.proc_inst_id_)"
      , "(select string_agg(g1.name,',') from act_ru_identitylink t1 inner join bc_identity_actor g1 on g1.code=t1.group_id_ where t1.task_id_= a.id_)"};
  }

  @Override
  protected String getHtmlPageJs() {
    return this.getModuleContextPath() + "/todo/view.js" + ","
      + this.getContextPath() + "/bc/identity/identity.js" + ","
      + this.getModuleContextPath() + "/select/selectUsers.js";
  }

  private TodoService todoService;

  @Autowired
  public void setTodoService(TodoService todoService) {
    this.todoService = todoService;
  }


  // ==高级搜索代码开始==
  @Override
  protected boolean useAdvanceSearch() {
    return true;
  }

  public JSONArray processNames;

  public JSONArray taskNames;

  @Override
  protected void initConditionsFrom() throws Exception {
    List<String> values = this.todoService.findProcessNames();
    List<Map<String, String>> list = new ArrayList<Map<String, String>>();
    Map<String, String> map;
    for (String value : values) {
      map = new HashMap<String, String>();
      map.put("key", value);
      map.put("value", value);
      list.add(map);
    }
    this.processNames = OptionItem.toLabelValues(list);


    values = this.todoService.findTaskNames();
    list = new ArrayList<Map<String, String>>();
    for (String value : values) {
      map = new HashMap<String, String>();
      map.put("key", value);
      map.put("value", value);
      list.add(map);
    }
    this.taskNames = OptionItem.toLabelValues(list);
  }


}
