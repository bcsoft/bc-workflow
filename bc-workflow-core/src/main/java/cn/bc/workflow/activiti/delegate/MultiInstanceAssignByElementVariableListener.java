/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.core.exception.CoreException;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自动分配多实例任务办理人或办理岗位(简单版)
 * <p>MultiInstanceAssigneeListener 定义的格式太复杂，此为简化版。
 * 读取多任务实例中配置的 Element variable 变量的值，当值格式为"group|xxx"时，任务的候选岗位设为 XXX，否则设置任务的办理人为此变量的值
 * </p>
 *
 * @author dragon 2015-10-23
 */
public class MultiInstanceAssignByElementVariableListener implements TaskListener {
	private static final Logger logger = LoggerFactory.getLogger(MultiInstanceAssignByElementVariableListener.class);
	private Expression name; // Element variable 配置

	public void notify(DelegateTask delegateTask) {
		if (logger.isDebugEnabled()) {
			logger.debug("taskDefinitionKey=" + delegateTask.getTaskDefinitionKey());
			logger.debug("taskId=" + delegateTask.getId());
			logger.debug("eventName=" + delegateTask.getEventName());
		}

		//@SuppressWarnings("rawtypes")
		if (name == null) throw new CoreException("name could not be null");
		String value = (String) delegateTask.getVariable(name.getExpressionText());
		if (value == null || value.isEmpty())
			throw new CoreException("value could not be empty (name=" + name.getExpressionText() + ")");

		if (value.startsWith("group|")) {       // 分配到岗位
			delegateTask.addCandidateGroup(value.substring(6));
		} else {                                // 分配到人
			delegateTask.setAssignee(value);
		}
	}
}
