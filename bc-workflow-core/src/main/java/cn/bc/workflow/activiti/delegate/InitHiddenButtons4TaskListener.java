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
 * 初始化待办区任务的‘添加附件,添加意见’等按钮默认隐藏控制的监听器
 *
 * @author lbj
 */
public class InitHiddenButtons4TaskListener implements TaskListener {
  private static final Log logger = LogFactory
    .getLog(InitHiddenButtons4TaskListener.class);

  private Expression hiddenButtonCodes; //需要默认隐藏按钮的编码，例如BUTTON_ADDCOMMENT,BUTTON_ADDATTACH 多个逗号隔开

  public void notify(DelegateTask delegateTask) {
    if (logger.isDebugEnabled()) {
      logger.debug("hiddenButtonCodes="
        + (hiddenButtonCodes != null ? hiddenButtonCodes.getExpressionText() : null));
      logger.debug("processInstanceId="
        + delegateTask.getProcessInstanceId());
      logger.debug("taskDefinitionKey="
        + delegateTask.getTaskDefinitionKey());
      logger.debug("taskId="
        + delegateTask.getId());
      logger.debug("eventName=" + delegateTask.getEventName());
    }

    if (hiddenButtonCodes != null && hiddenButtonCodes.getExpressionText() != "") {
      delegateTask.setVariableLocal("hiddenButtonCodes", hiddenButtonCodes.getExpressionText());
    }
  }

}
