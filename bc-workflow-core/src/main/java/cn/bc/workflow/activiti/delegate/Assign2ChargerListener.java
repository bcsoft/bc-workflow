/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.ArrayList;
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
 * 自动将任务分配到部门负责人
 * <p>
 * 监听器在流程图中需要配置为"java class"类型，Fields参数中有两种配置方式：
 * <ul>
 * <li>岗位名称+保存组织ID的流程变量名(默认为orgVariableName)</li>
 * <li>岗位编码</li>
 * </ul>
 * 监听器根据部门的配置将任务分配到部门负责人岗位或人，如果岗位内只有一个用户，就直接将任务分配给此用户，否则分配给岗位。
 * </p>
 * 
 * @author lbj
 * 
 */
public class Assign2ChargerListener implements TaskListener {
	private static final Log logger = LogFactory
			.getLog(Assign2ChargerListener.class);

	/**
	 * 岗位只有一个人是直接分配到人
	 */
	private Expression onlyOneUser;

	/**
	 * 优先分配到指定办理人变量名(必须为全局变量)
	 */
	private Expression assignee;

	/**
	 * 多个经理岗位名称,逗号隔开
	 */
	private Expression groupNames;

	/**
	 * 部门组织ID的流程变量名(必须为全局变量)
	 */
	private Expression orgVariableName;

	public void notify(DelegateTask delegateTask) {
		if (logger.isDebugEnabled()) {
			logger.debug("orgVariableName="
					+ (orgVariableName != null ? orgVariableName
							.getExpressionText() : null));
			logger.debug("groupNames="
					+ (groupNames != null ? groupNames.getExpressionText()
							: null));
			logger.debug("taskDefinitionKey="
					+ delegateTask.getTaskDefinitionKey());
			logger.debug("taskId=" + delegateTask.getId());
			logger.debug("eventName=" + delegateTask.getEventName());
		}

		List<Actor> groups = new ArrayList<Actor>();
		Actor group;
		ActorService actorService = SpringUtils.getBean("actorService",
				ActorService.class);
		
		if (assignee != null) {
			Object objCode = delegateTask.getVariable(assignee
					.getExpressionText());
			if (objCode != null) {// 全局变量中有此值
				group = actorService.loadByCode(objCode.toString());
				// 直接分配到此用户
				delegateTask.setAssignee(group.getCode());
				return;
			}
		}

		if(orgVariableName==null||orgVariableName.getExpressionText().length()==0){
			throw new CoreException("监听器没有配置 部门组织ID变量！");
		}
		
		// 按岗位名称获取岗位
		Long orgId = (Long) delegateTask.getVariable(orgVariableName.getExpressionText());

		String[] groupNamesArr = groupNames.getExpressionText().split(",");
		String groupName = "";
		for (String gn : groupNamesArr) {
			groups = actorService.findFollowerWithName(orgId, gn,
					new Integer[] { ActorRelation.TYPE_BELONG },
					new Integer[] { Actor.TYPE_GROUP },
					new Integer[] { BCConstants.STATUS_ENABLED });

			if (!groups.isEmpty()) {
				groupName = gn;
				break;
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("groups.size=" + groups.size());
		}
		if (groups.isEmpty()) {
			throw new CoreException("你所在的部门没有配置“部门经理”岗位！");
		} else if (groups.size() > 1) {
			throw new CoreException("id=" + orgId + "的单位下有多个名称为“" + groupName
					+ "”的岗位");
		}

		group = groups.get(0);
		if (logger.isDebugEnabled()) {
			logger.debug("group=" + group.getCode() + "," + group.getName());
		}

		// 获取用户信息
		List<Actor> users = actorService.findFollower(group.getId(),
				new Integer[] { ActorRelation.TYPE_BELONG },
				new Integer[] { Actor.TYPE_USER });
		if (logger.isDebugEnabled()) {
			logger.debug("users.size=" + users.size());
		}

		if (users.size() == 1 && onlyOneUser != null
				&& onlyOneUser.getExpressionText().equals("true")) {// 直接分派到用户
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
