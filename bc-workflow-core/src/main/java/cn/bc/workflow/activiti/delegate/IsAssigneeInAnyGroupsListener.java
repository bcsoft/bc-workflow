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

        // 岗位名称没有配置，不进操作
        String[] groupNames = anyGroupNames.getExpressionText().split(",");
        if ((groupNames == null) || (groupNames != null && groupNames.length == 0)) return;

        // 根据所属部门Id，获得岗位集合
        Actor belongDepartment = this.getBelongDepartment(assignee);
        List<Actor> groups = (belongDepartment != null) ? this.findGroups(belongDepartment.getId(), groupNames) : null;

        // 设置错误提示信息
        if (groups == null || (groups != null && groups.size() == 0)) {
            throw new CoreException("办理人所在部门中的岗位没有跟配置的岗位匹配");
        }

        // 拼装岗位Id
        Long[] groupsId = new Long[groups.size()];
        int i = 0;
        for (Actor a : groups) {
            groupsId[i] = a.getId();
            i++;
        }

        // 获得岗位下的用户集合
        List<Actor> users = this.findUsers(groupsId);
        // 定义拥有岗位的布尔值
        boolean haveGroup = false;
        if (users != null)
            // 判断办理人是否在岗位上
            haveGroup = isUserHaveGroup(assignee, users);

        // 设置是否拥有岗位的全局变量
        delegateTask.setVariableLocal(customHaveGroupKey.getExpressionText(), haveGroup);
    }

    /**
     * 获得所属部门
     *
     * @param assignee 办理人
     * @return 所属部门
     */
    private Actor getBelongDepartment(String assignee) {
        Actor belongDepartment = null;
        Actor actor = this.actorService.loadByCode(assignee);
        List<ActorRelation> ars = this.actorRelationService.findByFollower(ActorRelation.TYPE_BELONG, actor.getId());

        for (ActorRelation ar : ars) {
            Actor a = ar.getMaster();
            if (a.getType() == Actor.TYPE_DEPARTMENT)
                belongDepartment = this.actorService.loadByCode(a.getCode());
        }
        return belongDepartment;
    }

    /**
     * 判断用户是否在岗位中
     */
    private boolean isUserHaveGroup(String assignee, List<Actor> users) {
        if (assignee == null || assignee == "" || users == null
                || users.size() == 0)
            return false;

        for (Actor a : users) {
            if (a.getCode().equals(assignee))
                return true;
        }
        return false;
    }

    /**
     * 获得岗位下的用户集合
     *
     * @param groupsId 岗位Id
     * @return 用户集合
     */
    private List<Actor> findUsers(Long[] groupsId) {
        // 获取岗位下的用户
        return SpringUtils.getBean("actorService", ActorService.class)
                .findFollwerWithIds(groupsId,
                        new Integer[]{ActorRelation.TYPE_BELONG},
                        new Integer[]{Actor.TYPE_USER});
    }

    /**
     * 根据所属部门Id，获得与配置的岗位匹配的岗位集合
     *
     * @param belongId 岗位隶属的部门Id
     * @param groupNames 岗位名称
     * @return 岗位集合
     */
    private List<Actor> findGroups(Long belongId, String[] groupNames) {
        // 根据岗位名称获取岗位集合
        return groupService.findByNames((belongId != null) ? new Long[]{belongId} : null, groupNames,
                new Integer[]{ActorRelation.TYPE_BELONG},
                new Integer[]{Actor.TYPE_GROUP},
                new Integer[]{BCConstants.STATUS_ENABLED});
    }
}
