/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 初始化流程公共信息区‘添加附件,添加意见’等按钮默认隐藏控制的监听器
 * <p>
 * 只能配置在流程的启动事件中
 * </p>
 *
 * @author lbj
 */
public class InitHiddenButtons4ProcessListener implements ExecutionListener {
  private static final Log logger = LogFactory
    .getLog(InitHiddenButtons4ProcessListener.class);

  private Expression hiddenButtonCodes; //需要默认隐藏按钮的编码，例如BUTTON_ADDCOMMENT,BUTTON_ADDATTACH 多个逗号隔开

  public void notify(DelegateExecution execution) {
    if (logger.isDebugEnabled()) {
      logger.debug("hiddenButtonCodes="
        + (hiddenButtonCodes != null ? hiddenButtonCodes.getExpressionText() : null));
      logger.debug("processInstanceId="
        + execution.getProcessInstanceId());
      logger.debug("eventName=" + execution.getEventName());
    }

    if (hiddenButtonCodes != null && hiddenButtonCodes.getExpressionText() != "") {
      execution.setVariable("hiddenButtonCodes", hiddenButtonCodes.getExpressionText());
    }
  }
}
