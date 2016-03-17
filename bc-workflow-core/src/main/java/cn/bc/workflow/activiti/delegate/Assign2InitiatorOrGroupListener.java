/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.BCConstants;
import cn.bc.core.exception.CoreException;
import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.service.ActorService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 兼容后台自动发起流程的任务分配监听器
 * <p>
 * 仅适用于配置在流程的首个 Task 中。
 * 监听器在流程图中需要配置为"java class"类型，Fields参数中有要配置两个参数：
 * <ul>
 * <li>groupName - 岗位名称</li>
 * </ul>
 * 监听器通过判断流程变量 startInBackground 的值来判断流程是否是后台系统发起的（不是某个用户通过系统手动发起流程）。
 * 如果 startInBackground = true 则为后台发起的流程，此时将 Task 的办理人设置为 upperId 下的岗位 groupName；
 * 如果此岗位内只有一个用户，就直接将任务分配给此用户，否则分配给此岗位。
 * 如果 startInBackground = false 或不存在，则将 Task 的办理人设置为 initiator。
 * </p>
 *
 * @author dragon
 */
public class Assign2InitiatorOrGroupListener implements TaskListener {
	private static final Logger logger = LoggerFactory.getLogger(Assign2InitiatorOrGroupListener.class);

	/**
	 * 岗位名称
	 */
	private Expression groupName;

	/**
	 * 保存岗位所属组织ID的流程变量名(默认为 orgId，必须为全局变量)
	 */
	private Expression orgVariableName;

	public Assign2InitiatorOrGroupListener() {
		logger.error("init0");
	}

	public void notify(DelegateTask delegateTask) {
		// 获取岗位所属的上级组织ID
		String orgIdKey = orgVariableName != null ? orgVariableName.getExpressionText() : "orgId";
		Long orgId = delegateTask.hasVariable(orgIdKey) ? (Long) delegateTask.getVariable(orgIdKey) : null;
		Assert.notNull(orgId, "没有设置岗位所属组织的全局流程变量 " + orgIdKey);

		// 获取配置的岗位名称
		Assert.notNull(this.groupName, "没有配置 groupName 参数");
		String groupName = this.groupName.getExpressionText();
		Assert.notNull(groupName, "没有配置 groupName 参数");

		// 判断流程的发起方式
		boolean startInBackground = delegateTask.hasVariable("startInBackground") ?
				(Boolean) delegateTask.getVariable("startInBackground") : false;

		logger.debug("orgId={}, groupName={}, startInBackground={}", orgId, groupName, startInBackground);
		ActorService actorService = SpringUtils.getBean("actorService", ActorService.class);

		if (startInBackground) {    // 后台发起流程
			// 查找指定组织下指定名称的岗位
			List<Actor> groups = actorService.findFollowerWithName(orgId,
					groupName,
					new Integer[]{ActorRelation.TYPE_BELONG},
					new Integer[]{Actor.TYPE_GROUP},
					new Integer[]{BCConstants.STATUS_ENABLED});
			Actor group;
			if (groups.isEmpty()) {
				throw new CoreException("id=" + orgId + "的组织下没有配置名称为“" + groupName + "”的岗位");
			} else if (groups.size() > 1) {
				throw new CoreException("id=" + orgId + "的组织下有多个名称为“" + groupName + "”的岗位");
			}
			group = groups.get(0);

			// 获取岗位包含的用户信息
			List<Actor> users = actorService.findFollower(group.getId(),
					new Integer[]{ActorRelation.TYPE_BELONG},
					new Integer[]{Actor.TYPE_USER});

			// 分派任务
			if (users.size() == 1) {    // 直接分派给用户
				delegateTask.setAssignee(users.get(0).getCode());
				logger.debug("assign to user '{}({})'", users.get(0).getName(), users.get(0).getCode());
			} else {                    // 分派给岗位
				delegateTask.addCandidateGroup(group.getCode());
			}
		} else {        // 用户会话内发起流程
			// 将任务直接分派给流程的发起人
			String initiator = (String) delegateTask.getVariable("initiator");
			delegateTask.setAssignee(initiator);
		}
	}
}