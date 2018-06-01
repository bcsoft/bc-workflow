/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.workflow.domain.ExcutionLog;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

import java.util.Map;

/**
 * ServiceTask任务执行日志的监听器：start开始、end结束
 *
 * @author dragon
 */
public class AutoTaskLogListener extends ExcutionLogListener {
  public AutoTaskLogListener() {
    super();
  }

  /**
   * 获取日志类型的前缀
   *
   * @return
   */
  protected String getLogTypePrefix() {
    return "autoTask_";
  }

  @Override
  protected ExcutionLog buildExcutionLog(DelegateExecution execution) {
    ExcutionLog log = super.buildExcutionLog(execution);
    if (execution instanceof ExecutionEntity) {
      ExecutionEntity e = (ExecutionEntity) execution;
      log.setExcutionCode(e.getActivityId());
      if (e.getActivity() != null
        && e.getActivity().getProperties() != null) {
        Map<String, Object> map = e.getActivity().getProperties();
        log.setExcutionName((String) map.get("name"));
      }
    }
    return log;
  }
}
