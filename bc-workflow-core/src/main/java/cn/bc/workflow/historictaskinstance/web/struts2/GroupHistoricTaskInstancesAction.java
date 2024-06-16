package cn.bc.workflow.historictaskinstance.web.struts2;

import cn.bc.acl.domain.AccessActor;
import cn.bc.acl.service.AccessService;
import cn.bc.core.query.cfg.PagingQueryConfig;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.impl.*;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;
import cn.bc.workflow.deploy.domain.Deploy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门经办监控视图Action
 *
 * @author lbj
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class GroupHistoricTaskInstancesAction extends HistoricTaskInstancesAction {
  private static final long serialVersionUID = 1L;
  private ActorService actorService;
  private AccessService accessService;

  @Autowired
  public void setAccessService(AccessService accessService) {
    this.accessService = accessService;
  }

  @Autowired
  public void setActorService(ActorService actorService) {
    this.actorService = actorService;
  }

  @Override
  protected String getFormActionName() {
    return "groupHistoricTaskInstance";
  }

  public boolean isGroupControl() {
    // 部门监控
    SystemContext context = (SystemContext) this.getContext();
    return context
      .hasAnyRole(getText("key.role.bc.workflow.group"));
  }

  @Override
  protected String getGridDblRowMethod() {
    return "bc.groupHistoricTaskInstanceSelectView.open";
  }


  @Override
  protected Toolbar getHtmlPageToolbar() {
    Toolbar tb = new Toolbar();
    // 查看
    tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
      .setText(getText("label.read"))
      .setClick("bc.groupHistoricTaskInstanceSelectView.open"));
    tb.addButton(new ToolbarButton().setIcon("ui-icon-search")
      .setText(getText("flow.task.flow"))
      .setClick("bc.groupHistoricTaskInstanceSelectView.viewflow"));
    // 搜索按钮
    tb.addButton(this.getDefaultSearchToolbarButton());

    return tb;
  }

  @Override
  protected String getHtmlPageJs() {
    return super.getHtmlPageJs() + ","
      + this.getModuleContextPath() + "/historictaskinstance/group/view.js";
  }

  /**
   * SQL分页查询语句及参数配置
   */
  @Override
  protected PagingQueryConfig getPagingQueryConfig() {
    cn.bc.core.query.cfg.impl.PagingQueryConfig config =
      (cn.bc.core.query.cfg.impl.PagingQueryConfig) super.getPagingQueryConfig();

    QlCondition ownActors_qc = this.getOwnActorCondition();
    QlCondition deploy_qc = this.getDeployAccessControlCondition();
    QlCondition pi_qc = this.getProcessInstanceAccessControlCondition();

    OrCondition or = new OrCondition();
    if (ownActors_qc != null) {
      config.addTemplateParam("ownActors_qc", ownActors_qc.getExpression());
      or.add(ownActors_qc);
    }
    if (deploy_qc != null) {
      config.addTemplateParam("deploy_qc", deploy_qc.getExpression());
      or.add(deploy_qc);
    }
    if (pi_qc != null) {
      config.addTemplateParam("pi_qc", pi_qc.getExpression());
      or.add(pi_qc);
    }

    if (!or.isEmpty()) {
      config.addTemplateParam("extra_condition", or.setAddBracket(true).getExpression());
    } else {
      // 所有条件为空应该看不到任何经办
      config.addTemplateParam("extra_condition", new IsNullCondition("t.task_id").getExpression());
    }

    return config;
  }

  @Override
  protected Condition getGridSpecalCondition() {
    // 状态条件
    AndCondition ac = new AndCondition();

    // 排除自己的经办
    ac.add(new NotEqualsCondition("t.assignee_", ((SystemContext) this.getContext()).getUser().getCode()));

    // 结束时间不能为空
    ac.add(new IsNotNullCondition("t.end_time_"));
    ac.add(new IsNullCondition("e.parent_id_"));

    return ac;
  }

  /** 有部门监控权限并且是部门领导岗位内的人，可以看到该领导岗位所属部门下所有人的经办 */
  private QlCondition getOwnActorCondition() {
    if (!this.isGroupControl()) return null;

    // 查找当前登录用户条件
    SystemContext context = (SystemContext) this.getContext();
    //当前用户
    Actor actor = context.getUser();
    //用户已拥有的岗位
    List<Actor> ownedGroups = this.actorService.findMaster(actor.getId(),
      new Integer[]{ActorRelation.TYPE_BELONG},
      new Integer[]{Actor.TYPE_GROUP});
    if (ownedGroups == null || ownedGroups.size() == 0) return null;

    //部门领导的岗位
    List<Actor> leaderGroups = null;
    //判断用户拥有的岗位名称是否为指定的部门领导岗位名称
    for (Actor a : ownedGroups) {
      if (this.getText("flow.group.leaderDepartmentGroupNames").indexOf(a.getName()) != -1) {
        if (leaderGroups == null) leaderGroups = new ArrayList<>();

        leaderGroups.add(a);
      }
    }
    if (leaderGroups == null) return null;

    //部门领导岗位对应的上级组织但不包括“宝城”
    List<Actor> leaderUppers = null;
    Actor leaderUpper;
    for (Actor a : leaderGroups) {
      leaderUpper = this.actorService.loadBelong(a.getId(),
        new Integer[]{Actor.TYPE_DEPARTMENT, Actor.TYPE_UNIT});
      if (!leaderUpper.getCode().equals("baochengzongbu")) {
        if (leaderUppers == null) leaderUppers = new ArrayList<Actor>();

        leaderUppers.add(leaderUpper);
      }
    }
    if (leaderUppers == null) return null;

    //上级组织拥有的用户
    List<Actor> ownActors = new ArrayList<>();
    List<Actor> _ownActors;
    for (Actor a : leaderUppers) {
      _ownActors = this.actorService.findFollower(a.getId(),
        new Integer[]{ActorRelation.TYPE_BELONG},
        new Integer[]{Actor.TYPE_USER});
      if (_ownActors == null) continue;

      for (Actor _a : _ownActors) {
        if (!ownActors.contains(_a)) {
          ownActors.add(_a);
        }
      }
    }

    if (ownActors == null || ownActors.size() == 0) return null;

    String sql = "\r\nt.assignee_code in (";
    for (int i = 0; i < ownActors.size(); i++) {
      if (i > 0) sql += ",";

      sql += "'" + ownActors.get(i).getCode() + "'";
    }
    sql += ")";

    return new QlCondition(sql);
  }

  /** 有流程部署的监控权限可以看到此类流程的所有经办 */
  private QlCondition getDeployAccessControlCondition() {
    // 查找当前登录用户条件
    SystemContext context = (SystemContext) this.getContext();
    //当前用户
    Actor actor = context.getUser();
    //流程部署的监控
    List<AccessActor> aa4list = this.accessService.findByDocType(actor.getId(), Deploy.class.getSimpleName());
    if (aa4list == null || aa4list.size() == 0) return null;

    //流程部署的id
    List<String> deployIds = new ArrayList<>();

    for (AccessActor aa : aa4list) {
      //先进性权限的判断
      //查阅
      if (AccessActor.ROLE_TRUE.equals(aa.getRole().substring(aa.getRole().length() - 1))) {
        deployIds.add(aa.getAccessDoc().getDocId());
      } else {
        //编辑
        if (AccessActor.ROLE_TRUE.equals(
          aa.getRole().substring(aa.getRole().length() - 2, aa.getRole().length() - 1))) {
          deployIds.add(aa.getAccessDoc().getDocId());
        }

      }
    }

    if (deployIds.size() == 0) return null;

    String sql = "\r\nexists(select 1 from act_hi_taskinst dc_t";
    sql += " inner join act_re_procdef dc_r on dc_r.id_=dc_t.proc_def_id_";
    sql += " inner join bc_wf_deploy dc_d on dc_d.deployment_id=dc_r.deployment_id_";
    sql += " where dc_t.id_=t.task_id and dc_d.id in(";

    for (int i = 0; i < deployIds.size(); i++) {
      if (i > 0) sql += ",";

      sql += deployIds.get(i);
    }
    sql += "))";

    return new QlCondition(sql);
  }

  /** 有流程实例监控权限可以看到此流程实例的所有经办 */
  private QlCondition getProcessInstanceAccessControlCondition() {
    // 查找当前登录用户条件
    SystemContext context = (SystemContext) this.getContext();
    //当前用户
    Actor actor = context.getUser();
    //流程部署的监控
    List<AccessActor> aa4list = this.accessService.findByDocType(actor.getId(), "ProcessInstance");
    if (aa4list == null || aa4list.size() == 0) return null;

    //流程实例的id
    List<String> pIds = new ArrayList<>();

    for (AccessActor aa : aa4list) {
      //先进性权限的判断
      //查阅
      if (AccessActor.ROLE_TRUE.equals(aa.getRole().substring(aa.getRole().length() - 1))) {
        pIds.add(aa.getAccessDoc().getDocId());
      } else {
        //编辑
        if (AccessActor.ROLE_TRUE.equals(
          aa.getRole().substring(aa.getRole().length() - 2, aa.getRole().length() - 1))) {
          pIds.add(aa.getAccessDoc().getDocId());
        }

      }
    }

    if (pIds.size() == 0) return null;

    String sql = "\r\nexists(select 1 from act_hi_taskinst pi_a";
    sql += " where pi_a.id_=t.task_id and pi_a.proc_inst_id_ in(";

    for (int i = 0; i < pIds.size(); i++) {
      if (i > 0) sql += ",";

      sql += "'" + pIds.get(i) + "'";
    }
    sql += "))";

    return new QlCondition(sql);
  }

}
