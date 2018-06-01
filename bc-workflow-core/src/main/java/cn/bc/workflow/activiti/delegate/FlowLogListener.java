/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.workflow.domain.ExcutionLog;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.process.TransitionImpl;

/**
 * 记录流向执行日志的监听器：take
 *
 * @author dragon
 */
public class FlowLogListener extends ExcutionLogListener {
  @Override
  protected String getLogTypePrefix() {
    return "flow_";
  }

  @Override
  protected ExcutionLog buildExcutionLog(DelegateExecution execution) {
    ExcutionLog log = super.buildExcutionLog(execution);

    // 记录流向的条件表达式
    if (execution instanceof ExecutionEntity) {
      ExecutionEntity e = (ExecutionEntity) execution;
      TransitionImpl t = e.getTransition();
      if (t != null) {
        log.setExcutionCode(t.getId());// 流向的编码
        log.setExcutionName((String) t.getProperty("name"));// 流向的名称
        Object conditionText = t.getProperty("conditionText");
        log.setDescription(conditionText != null ? conditionText
          .toString() : null);// 流向的条件表达式
      }
    }

    return log;
  }
}
