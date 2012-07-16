/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.List;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import cn.bc.core.exception.CoreException;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.service.ActorService;

/**
 * 将任务分配到指定岗位的监听器
 * <p>
 * 调用此监听器前需已设置岗位所隶属组织的id信息，此监听器自动获取此组织下指定名称的岗位内的用户信息，如果只有一个用户，直接将用户分配给此用户，
 * 否则就直接将岗位作为任务的候选岗位。
 * </p>
 * 
 * @author dragon
 * 
 */
public abstract class AbstractAssign2GroupOrUserListener implements
		TaskListener {
	private static final Log logger = LogFactory
			.getLog(AbstractAssign2GroupOrUserListener.class);
	protected ActorService actorService;

	@Autowired
	public void setActorService(
			@Qualifier(value = "actorService") ActorService actorService) {
		this.actorService = actorService;
	}

	/**
	 * 获取指定的岗位名称
	 * 
	 * @return
	 */
	protected abstract String getGroupName();

	/**
	 * 获取保存岗位所隶属组织id的流程变量名称，默认为"orgId"
	 * 
	 * @return
	 */
	protected String getVariableName() {
		return "orgId";
	}

	public void notify(DelegateTask delegateTask) {
		Long orgId = (Long) delegateTask.getVariableLocal(this
				.getVariableName());
		if (logger.isDebugEnabled()) {
			logger.debug("variableName=" + getVariableName());
			logger.debug("groupName=" + getGroupName());
			logger.debug("taskId=" + delegateTask.getId());
			logger.debug("orgId=" + orgId);
			logger.debug("eventName=" + delegateTask.getEventName());
			logger.debug("processInstanceId"
					+ delegateTask.getProcessInstanceId());
			logger.debug("executionId=" + delegateTask.getExecutionId());
			logger.debug("taskDefinitionKey="
					+ delegateTask.getTaskDefinitionKey());
		}

		// 获取岗位信息
		List<Actor> groups = actorService.findFollowerWithName(orgId,
				getGroupName(), new Integer[] { ActorRelation.TYPE_BELONG },
				new Integer[] { Actor.TYPE_GROUP });
		if (groups.isEmpty()) {
			throw new CoreException("id=" + orgId + "的单位没有配置名称为“"
					+ getGroupName() + "”的岗位");
		} else if (groups.size() > 1) {
			throw new CoreException("id=" + orgId + "的单位下有多个名称为“"
					+ getGroupName() + "”的岗位");
		}
		Actor group = groups.get(0);

		// 获取用户信息
		List<Actor> users = actorService.findFollower(group.getId(),
				new Integer[] { ActorRelation.TYPE_BELONG },
				new Integer[] { Actor.TYPE_USER });

		if (users.size() == 1) {// 直接分派到用户
			delegateTask.setAssignee(users.get(0).getCode());
		} else {// 分派到岗位
			delegateTask.addCandidateGroup(group.getCode());
		}
	}
}
