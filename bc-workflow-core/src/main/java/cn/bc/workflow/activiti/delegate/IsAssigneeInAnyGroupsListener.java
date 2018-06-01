/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.BCConstants;
import cn.bc.core.exception.CoreException;
import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.service.ActorRelationService;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.service.GroupService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>自动判断办理人是否在任意一个岗位中</p>
 * <p>返回boolean型本地变量，以配置“customHaveGroupKey”的内容为key</p>
 *
 * @author Action
 */
public class IsAssigneeInAnyGroupsListener implements TaskListener {
  private static final Log logger = LogFactory.getLog(IsAssigneeInAnyGroupsListener.class);
  private ActorService actorService;
  private GroupService groupService;
  private ActorRelationService actorRelationService;

  /**
   * 岗位名称，逗号可以连接多个，英文逗号“,”
   */
  private Expression anyGroupNames;

  /**
   * 是否拥有岗位的布尔值
   */
  private Expression customHaveGroupKey;

  public void notify(DelegateTask delegateTask) {
    this.actorService = SpringUtils.getBean("actorService", ActorService.class);
    this.groupService = SpringUtils.getBean("groupService", GroupService.class);
    this.actorRelationService = SpringUtils.getBean("actorRelationService", ActorRelationService.class);
    if (logger.isDebugEnabled()) {
      logger.debug("anyGroupNames=" + (anyGroupNames != null ? anyGroupNames.getExpressionText() : null));
      logger.debug("customHaveGroupKey=" + (customHaveGroupKey != null ? customHaveGroupKey.getExpressionText() : null));
      logger.debug("taskId=" + delegateTask.getId());
      logger.debug("eventName=" + delegateTask.getEventName());
    }

    // 任务还没有办理人，不进操作
    String assignee = delegateTask.getAssignee();
    if (assignee == null) return;

    // 岗位名称没有配置，输出错误信息
    String[] groupNames = anyGroupNames.getExpressionText().split(",");
    if ((groupNames == null) || (groupNames != null && groupNames.length == 0)) {
      throw new CoreException("没有配置岗位名称！");
    }

    // 办理人的岗位
    Actor a = this.actorService.loadByCode(assignee);
    List<Actor> assigneeGroups = findAssigneeGroups(a.getId());

    // 获得配置的岗位集合
    List<Actor> masters = this.getMaster(a.getId());
    Long[] masterIds = this.getIds(masters);
    List<Actor> configGroups = this.findConfigGroupsByBelongDepartment(groupNames, masterIds);

    // 判断办理人的岗位是否在配置的岗位集合中存在
    boolean haveGroup = false;
    if (configGroups == null || (configGroups != null && configGroups.size() == 0)) {
      haveGroup = false;
    } else {
      haveGroup = this.isUserHaveGroup(assigneeGroups, configGroups);
    }

    // 设置是否拥有岗位的全局变量
    delegateTask.setVariableLocal(customHaveGroupKey.getExpressionText(), haveGroup);
  }

  /**
   * 获得id
   *
   * @param list Actor集合
   * @return actor的Id
   */
  private Long[] getIds(List<Actor> list) {
    Long[] ids = new Long[list.size()];
    for (int i = 0; i < list.size(); i++) {
      ids[i] = list.get(i).getId();
    }
    return ids;
  }

  /**
   * 在办理人所属部门下，获得配置的岗位
   *
   * @param groupNames 岗位名称
   * @param masterIds  部门Id或单位Id
   * @return 岗位集合
   */
  private List<Actor> findConfigGroupsByBelongDepartment(String[] groupNames, Long[] masterIds) {
    return this.groupService.findByNames(masterIds, groupNames,
      new Integer[]{ActorRelation.TYPE_BELONG},
      new Integer[]{Actor.TYPE_GROUP},
      new Integer[]{BCConstants.STATUS_ENABLED}
    );
  }

  /**
   * 获得办理人所属部门，或单位
   *
   * @param assigneeId 办理人Id
   * @return 办理人所属部门
   */
  private List<Actor> getMaster(Long assigneeId) {
    List<Actor> belongDepartment = new ArrayList<>();
    List<ActorRelation> ars = this.actorRelationService.findByFollower(ActorRelation.TYPE_BELONG, assigneeId);
    for (ActorRelation ar : ars) {
      Actor master = ar.getMaster();
      if (master.getType() == Actor.TYPE_DEPARTMENT || master.getType() == Actor.TYPE_UNIT)
        belongDepartment.add(master);
    }

    return belongDepartment;
  }

  /**
   * 获得办理人的岗位
   *
   * @param id 办理人id
   * @return 办理人的岗位
   */
  private List<Actor> findAssigneeGroups(Long id) {
    List<Actor> groups = new ArrayList<>();
    List<ActorRelation> ars = this.actorRelationService.findByFollower(ActorRelation.TYPE_BELONG, id);
    for (ActorRelation ar : ars) {
      Actor master = ar.getMaster();
      if (master.getType() == Actor.TYPE_GROUP)
        groups.add(master);
    }
    return groups;
  }

  /**
   * 判断办理人的岗位是否在配置的岗位集合中存在
   *
   * @param assigneeGroups 办理人的岗位集合
   * @param configGroups   配置的岗位集合
   * @return true：办理人的岗位在配置的岗位中存在； false：办理人的岗位在配置的岗位中不存在
   */
  private boolean isUserHaveGroup(List<Actor> assigneeGroups, List<Actor> configGroups) {
    boolean isExist = false;

    // 判断办理人的岗位Id是否在配置的岗位id中存在
    for (Actor assigneeGroup : assigneeGroups) {
      for (Actor configGroup : configGroups) {
        if (assigneeGroup.getId().equals(configGroup.getId()))
          isExist = true;
      }
    }

    return isExist;
  }
}
