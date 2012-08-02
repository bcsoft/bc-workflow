/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.List;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.BCConstants;
import cn.bc.core.exception.CoreException;
import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.service.ActorService;

/**
 * 自动将任务分配到岗位或岗位内用户的任务监听器
 * <p>
 * 监听器在流程图中需要配置为"java class"类型，Fields参数中有两种配置方式：
 * <ul>
 * <li>岗位名称+保存组织ID的流程变量名(默认为orgVariableName)</li>
 * <li>岗位编码</li>
 * </ul>
 * 监听器自动获取岗位内的用户信息，如果岗位内只有一个用户，就直接将任务分配给此用户，否则分配给岗位。
 * </p>
 * 
 * @author dragon
 * 
 */
public class Assign2GroupUserListener implements TaskListener {
	private static final Log logger = LogFactory
			.getLog(Assign2GroupUserListener.class);

	/**
	 * 岗位编码
	 */
	private Expression groupCode;

	/**
	 * 岗位名称
	 */
	private Expression groupName;

	/**
	 * 如果orgVariableName变量没有设置，是否使用initiator流程变量指定的人
	 */
	private Expression null2initiator;

	/**
	 * 保存组织ID的流程变量名(必须为全局变量)
	 */
	private Expression orgVariableName;

	public void notify(DelegateTask delegateTask) {
		if (logger.isDebugEnabled()) {
			logger.debug("groupCode="
					+ (groupCode != null ? groupCode.getExpressionText() : null));
			logger.debug("orgVariableName="
					+ (orgVariableName != null ? orgVariableName
							.getExpressionText() : null));
			logger.debug("groupName="
					+ (groupName != null ? groupName.getExpressionText() : null));
			logger.debug("taskDefinitionKey="
					+ delegateTask.getTaskDefinitionKey());
			logger.debug("taskId=" + delegateTask.getId());
			logger.debug("eventName=" + delegateTask.getEventName());
		}

		List<Actor> groups;
		Actor group;
		ActorService actorService = SpringUtils.getBean("actorService",
				ActorService.class);
		if (groupCode != null) {// 按岗位编码获取岗位
			group = actorService.loadByCode(groupCode.getExpressionText());
			if (group == null) {
				throw new CoreException("没有找到编码为“"
						+ groupCode.getExpressionText() + "”的岗位");
			}
		} else {// 按岗位名称获取岗位
			Long orgId = (Long) delegateTask.getVariable(orgVariableName
					.getExpressionText());
			if (orgId == null) {// 处理手动发起流程的情况
				if (null2initiator != null
						&& "true".equals(null2initiator.getExpressionText())) {
					// 将任务直接分派到流程的发起人
					String initiator = (String) delegateTask
							.getVariable("initiator");
					if (logger.isDebugEnabled()) {
						logger.debug("initiator:user=" + initiator);
					}
					if (initiator == null)
						throw new CoreException("没有配置流程的发起人信息，无法分配任务：taskId="
								+ delegateTask.getId());

					delegateTask.setAssignee(initiator);
					return;
				} else {// 通过岗位名称查找岗位
					// throw new CoreException(
					// "没有找到记录岗位所在单位ID的流程变量值：orgVariableName="
					// + orgVariableName.getExpressionText());
					if (logger.isWarnEnabled())
						logger.warn("find groups without orgId: groupName="
								+ (groupName != null ? groupName
										.getExpressionText() : null));
					groups = actorService.findByName(
							groupName.getExpressionText(),
							new Integer[] { Actor.TYPE_GROUP },
							new Integer[] { BCConstants.STATUS_ENABLED });
				}
			} else {
				// 查找指定单位下指定名称的岗位
				groups = actorService.findFollowerWithName(orgId,
						groupName.getExpressionText(),
						new Integer[] { ActorRelation.TYPE_BELONG },
						new Integer[] { Actor.TYPE_GROUP },
						new Integer[] { BCConstants.STATUS_ENABLED });
			}
			if (logger.isDebugEnabled()) {
				logger.debug("groups.size=" + groups.size());
			}
			if (groups.isEmpty()) {
				throw new CoreException("id=" + orgId + "的单位没有配置名称为“"
						+ groupName.getExpressionText() + "”的岗位");
			} else if (groups.size() > 1) {
				throw new CoreException("id=" + orgId + "的单位下有多个名称为“"
						+ groupName.getExpressionText() + "”的岗位");
			}
			group = groups.get(0);
			if (logger.isDebugEnabled()) {
				logger.debug("group=" + group.getCode() + "," + group.getName());
			}
		}

		// 获取用户信息
		List<Actor> users = actorService.findFollower(group.getId(),
				new Integer[] { ActorRelation.TYPE_BELONG },
				new Integer[] { Actor.TYPE_USER });
		if (logger.isDebugEnabled()) {
			logger.debug("users.size=" + users.size());
		}

		if (users.size() == 1) {// 直接分派到用户
			delegateTask.setAssignee(users.get(0).getCode());
			if (logger.isDebugEnabled()) {
				logger.debug("user=" + users.get(0).getCode() + ","
						+ users.get(0).getName());
			}
		} else {// 分派到岗位
			delegateTask.addCandidateGroup(group.getCode());
		}
	}
}
