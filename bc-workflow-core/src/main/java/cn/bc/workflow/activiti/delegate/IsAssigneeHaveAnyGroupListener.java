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
 * 自动判断办理人是否在任意一个岗位中
 * <p>
 * 监听器在流程图中需要配置为"java class"类型，Fields参数中有两种配置方式：
 * <ul>
 * </ul>
 * 监听器自动判断办理人是否配置的岗位中，如果办理人在其中一个岗位，那么将会为自定义值设为true。
 * </p>
 * 
 * @author lbj
 * 
 */
public class IsAssigneeHaveAnyGroupListener implements TaskListener {
	private static final Log logger = LogFactory
			.getLog(IsAssigneeHaveAnyGroupListener.class);

	/**
	 * 岗位编码逗号可以连接多个
	 */
	private Expression anyGroupCodes;

	/**
	 * 岗位名称，逗号可以连接多个
	 */
	private Expression anyGroupNames;

	/**
	 * 是否拥有岗位的布尔值
	 * 
	 */
	private Expression customHaveGroupKey;

	/**
	 * 全部变量中已存在此自定key的变量，此变量判断是否需要更新自定义的key值，默认不更新；
	 * 
	 */
	private Expression exist;

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
			logger.debug("taskId=" + delegateTask.getId());
			logger.debug("eventName=" + delegateTask.getEventName());
		}

		Object customKey = delegateTask.getVariable(this.customHaveGroupKey
				.getExpressionText());
		
		//默认配置或判断的customHaveGroupKey已为ture时不再进行判断
		if (customKey != null
				&& (exist == null || exist.getExpressionText().equals("false") || (Boolean)customKey)) {
			return;
		}

		// 任务还没有办理人 不进操作
		if (delegateTask.getAssignee() == null) {
			return;
		}

		// 保存用于判断的岗位信息集合
		List<Actor> groups = new ArrayList<Actor>();
		// 声明变量
		List<Actor> temGroups;
		Actor group;

		ActorService actorService = SpringUtils.getBean("actorService",
				ActorService.class);

		if (anyGroupCodes != null) {// 按岗位编码获取岗位集合
			String[] groupCodes2Arr = anyGroupCodes.getExpressionText().split(
					",");
			for (String groupCode2Str : groupCodes2Arr) {
				group = actorService.loadByCode(groupCode2Str);
				if (group == null) {
					throw new CoreException("没有找到编码为“" + groupCode2Str + "”的岗位");
				} else {
					groups.add(group);
				}
			}
		}

		if (anyGroupNames != null) {// 根据岗位名称获取岗位集合
			String[] groupNames2Arr = anyGroupNames.getExpressionText().split(
					",");
			for (String gn : groupNames2Arr) {
				temGroups = actorService.findByName(gn,
						new Integer[] { Actor.TYPE_GROUP },
						new Integer[] { BCConstants.STATUS_ENABLED });
				if (temGroups == null) {
					throw new CoreException("没有找到名称为“" + gn + "”的岗位集合");
				} else {
					groups.addAll(temGroups);
				}
			}
		}

		// 定义拥有岗位的布尔值
		boolean haveGroup = false;

		for (Actor g : groups) {
			// 获取岗位中的用户
			List<Actor> users = actorService.findFollower(g.getId(),
					new Integer[] { ActorRelation.TYPE_BELONG },
					new Integer[] { Actor.TYPE_USER });
			// 判断办理人是否在岗位上
			haveGroup = isUserHaveGroup(delegateTask.getAssignee(), users);
			if (haveGroup)// 判断为岗位中的用户则跳出循环
				break;
		}
		delegateTask.setVariable(customHaveGroupKey.getExpressionText(),
				haveGroup);
	}

	// 判断用户是否在岗位中
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
}
