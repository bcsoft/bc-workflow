package cn.bc.workflow.activiti.delegate;

import cn.bc.core.util.SpringUtils;
import cn.bc.workflow.domain.WorkflowModuleRelation;
import cn.bc.workflow.service.WorkflowModuleRelationService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 创建流程与模块关联关系的监听器
 *
 * @author zf
 */
public class CreateModuleRelationListener extends ExcutionLogListener implements TaskListener {
  protected final Log logger = LogFactory.getLog(getClass());
  /**
   * 是否直接创建流程与模块关联关系
   */
  private Expression ignore;

  protected WorkflowModuleRelationService workflowModuleRelationService;

  public CreateModuleRelationListener() {
    workflowModuleRelationService = SpringUtils.getBean(WorkflowModuleRelationService.class);
  }

  @Override
  public void notify(DelegateExecution execution) {
    // 判断是否直接创建流程与模块关联关系
    boolean ignoreValue = ignore == null || (ignore.getExpressionText().contains("$") ? (Boolean) ignore.getValue(execution) : Boolean.parseBoolean(ignore.getExpressionText()));
    if (!ignoreValue) {
      logger.debug("ignore = false，无需创建流程与模块关联关系");
    } else {
      createModuleRelation(execution, execution.getProcessInstanceId());
    }
  }

  @Override
  public void notify(DelegateTask execution) {
    if (logger.isDebugEnabled()) {
      logger.debug("execution=" + execution.getClass());
      logger.debug("this=" + this.getClass());
      logger.debug("id=" + execution.getId());
      logger.debug("eventName=" + execution.getEventName());
      logger.debug("processInstanceId" + execution.getProcessInstanceId());
      logger.debug("executionId=" + execution.getExecutionId());
      logger.debug("taskDefinitionKey=" + execution.getTaskDefinitionKey());
    }
    // 判断是否直接创建流程与模块关联关系
    boolean ignoreValue = ignore == null || (ignore.getExpressionText().contains("$") ? (Boolean) ignore.getValue(execution) : Boolean.parseBoolean(ignore.getExpressionText()));
    if (!ignoreValue) {
      logger.debug("ignore = false，无需创建流程与模块关联关系");
    } else {
      createModuleRelation(execution, execution.getProcessInstanceId());
    }
  }

  private void createModuleRelation(VariableScope execution, String processInstanceId) {
    Long mid = execution.getVariable("mid") != null ? Long.parseLong((String) execution.getVariable("mid")) : null;
    String mtype = (String) execution.getVariable("mtype");
    if (mid != null && mtype != null) {
      boolean isExist = this.workflowModuleRelationService.hasRelation(mid, mtype);
      if (isExist) {
        logger.debug("已存在模块 ID 为" + mid + "的模块流程关系，无需保存！");
      } else {
        // 保存流程与模块信息的关系
        WorkflowModuleRelation workflowModuleRelation = new WorkflowModuleRelation();
        workflowModuleRelation.setMid(mid);
        workflowModuleRelation.setPid(processInstanceId);
        workflowModuleRelation.setMtype(mtype);
        this.workflowModuleRelationService.save(workflowModuleRelation);
      }
    } else {
      logger.debug("模块唯一标识不全，无法保存流程与模块关联关系！");
    }
  }
}
