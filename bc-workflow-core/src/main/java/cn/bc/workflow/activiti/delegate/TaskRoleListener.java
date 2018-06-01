/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 根据配置控制编码分配处理人的任务权限
 *
 * @author wis
 */
public class TaskRoleListener implements TaskListener {
  private static final Log logger = LogFactory
    .getLog(TaskRoleListener.class);

  private Expression roleCode; //控制编码如:active,多个用逗号分隔如:active,suspended.

  public void notify(DelegateTask delegateTask) {
    if (logger.isDebugEnabled()) {
      logger.debug("taskDefinitionKey="
        + delegateTask.getTaskDefinitionKey());
      logger.debug("taskId=" + delegateTask.getId());
      logger.debug("eventName=" + delegateTask.getEventName());
    }
    if (roleCode != null && roleCode.getExpressionText().length() > 0) {
      String variableName = roleCode.getExpressionText();
      if (variableName.indexOf(",") > 0) {
        String[] ary = variableName.split(",");
        for (int i = 0; i < ary.length; i++) {
          delegateTask.createVariableLocal(ary[i], true);
        }
      } else {
        delegateTask.createVariableLocal(variableName, true);
      }
    }
  }
}
