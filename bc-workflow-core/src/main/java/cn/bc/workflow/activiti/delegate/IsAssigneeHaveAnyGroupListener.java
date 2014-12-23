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
 * 自动判断办理人是否在任意一个岗位中<br/>
 * 返回boolean型全局变量，以配置“customHaveGroupKey”的内容为key
 * <p>
 * 监听器在流程图中需要配置为"java class"类型，Fields参数中有两种配置方式：
 * <ul>
 * </ul>
 * 监听器自动判断办理人是否配置的岗位中，如果办理人在其中一个岗位，那么将会为自定义值设为true。
 * </p>
 *
 * @author lbj
 */
public class IsAssigneeHaveAnyGroupListener implements TaskListener {
    private static final Log logger = LogFactory
            .getLog(IsAssigneeHaveAnyGroupListener.class);
    private ActorService actorService;

    /**
     * 岗位编码逗号可以连接多个
     */
    private Expression anyGroupCodes;

    /**
     * 岗位名称，逗号可以连接多个，英文逗号“,”
     */
        private Expression anyGroupNames;

    /**
     * 是否拥有岗位的布尔值
     */
    private Expression customHaveGroupKey;

    /**
     * 全部变量中已存在此自定key的变量，此变量判断是否需要更新自定义的key值，默认不更新；
     */
    private Expression exist;

    /**
     * 是否在办理人所属部门下找岗位的布尔值
     */
    private Expression isGetGroup;

    public void notify(DelegateTask delegateTask) {
        if (logger.isDebugEnabled()) {
            logger.debug("anyGroupCodes="
                    + (anyGroupCodes != null ? anyGroupCodes
                    .getExpressionText() : null));
            logger.debug("anyGroupNames="
                    + (anyGroupNames != null ? anyGroupNames
                    .getExpressionText() : null));
            logger.debug("customHaveGroupKey="
                    + (customHaveGroupKey != null ? customHaveGroupKey
                    .getExpressionText() : null));
            logger.debug("isGetGroup="
                    + (isGetGroup != null ? isGetGroup
                    .getExpressionText() : null));
            logger.debug("taskId=" + delegateTask.getId());
            logger.debug("eventName=" + delegateTask.getEventName());
        }

        Object customKey = delegateTask.getVariable(this.customHaveGroupKey
                .getExpressionText());

        //默认配置或判断的customHaveGroupKey已为ture时不再进行判断
        if (customKey != null
                && (exist == null || exist.getExpressionText().equals("false") || (Boolean) customKey)) {
            return;
        }

        // 任务还没有办理人 不进操作
        if (delegateTask.getAssignee() == null)
            return;

        this.actorService = SpringUtils.getBean("actorService", ActorService.class);

        // 办理人所属部门
        Actor department = null;
        Object isDivisionGroup = null;
        if (this.isGetGroup != null)
            isDivisionGroup = this.isGetGroup.getExpressionText();

        // 获取办理人所属部门
        if (isDivisionGroup != null && Boolean.parseBoolean((String) isDivisionGroup)) {
            // 办理人
            Actor actor = this.actorService.loadByCode(delegateTask.getAssignee());
            List<ActorRelation> ars = SpringUtils.getBean("actorRelationService", ActorRelationService.class)
                    .findByFollower(ActorRelation.TYPE_BELONG, actor.getId());
            for (ActorRelation ar : ars) {
                Actor a = ar.getMaster();
                if (a.getType() == Actor.TYPE_DEPARTMENT)
                    department = this.actorService.loadByCode(a.getCode());
            }
        }

        // 获得岗位集合
        List<Actor> groups = (department != null)
                ? this.findGroups(department.getId())
                : this.findGroups(null);

        // 设置错误提示信息
        if (groups == null || (groups != null && groups.size() == 0)) {
            if (isDivisionGroup == null)
                throw new CoreException("配置的岗位名称没有在系统找到！");
            else
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
            haveGroup = isUserHaveGroup(delegateTask.getAssignee(), users);

        // 设置是否拥有岗位的全局变量
        delegateTask.setVariable(customHaveGroupKey.getExpressionText(),
                haveGroup);
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
     * @return
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
     * @return
     */
    private List<Actor> findGroups(Long belongId) {
        GroupService groupService = SpringUtils.getBean("groupService",
                GroupService.class);
        // 任意岗位的编码或Code
        String[] anyGroup;
        // 按岗位编码获取岗位集合
        if (anyGroupCodes != null) {
            anyGroup = anyGroupCodes.getExpressionText().split(",");
            if (anyGroup != null)
                return groupService.findByCodes((belongId != null)
                                ? new Long[]{belongId} : null, anyGroup,
                        new Integer[]{ActorRelation.TYPE_BELONG},
                        new Integer[]{Actor.TYPE_GROUP},
                        new Integer[]{BCConstants.STATUS_ENABLED});
        }
        // 根据岗位名称获取岗位集合
        else if (anyGroupNames != null) {
            anyGroup = anyGroupNames.getExpressionText().split(",");
            if (anyGroup != null)
                return groupService.findByNames((belongId != null)
                                ? new Long[]{belongId} : null, anyGroup,
                        new Integer[]{ActorRelation.TYPE_BELONG},
                        new Integer[]{Actor.TYPE_GROUP},
                        new Integer[]{BCConstants.STATUS_ENABLED});
        }

        return null;
    }
}
