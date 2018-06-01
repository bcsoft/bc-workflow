/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.service.ExcutionLogService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * 记录流程执行日志的通用监听器：可以附加到任何节点
 *
 * @author dragon
 */
public class ExcutionLogListener implements ExecutionListener {
  private static final Logger logger = LoggerFactory.getLogger(ExcutionLogListener.class);
  protected ExcutionLogService excutionLogService;

  public ExcutionLogListener() {
    excutionLogService = SpringUtils.getBean(ExcutionLogService.class);
  }

  public void notify(DelegateExecution execution) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("execution=" + execution.getClass());
      logger.debug("this=" + this.getClass());
      logger.debug("id=" + execution.getId());
      logger.debug("eventName=" + execution.getEventName());
      logger.debug("processInstanceId" + execution.getProcessInstanceId());
      logger.debug("processBusinessKey="
        + execution.getProcessBusinessKey());
    }

    // 创建日志
    ExcutionLog log = buildExcutionLog(execution);

    // 保存日志
    saveLog(log);

    // 创建信息同步日志
    log = buildSyncInfoExcutionLog(execution);

    if (log != null)
      saveLog(log);
  }

  /**
   * 保存日志
   *
   * @param log
   */
  protected void saveLog(ExcutionLog log) {
    excutionLogService.save(log);
  }

  /**
   * 创建日志
   *
   * @param execution
   * @return
   */
  protected ExcutionLog buildExcutionLog(DelegateExecution execution) {
    ExcutionLog log = new ExcutionLog();
    log.setFileDate(Calendar.getInstance());
    ActorHistory h = SystemContextHolder.get().getUserHistory();
    log.setAuthorId(h.getId());
    log.setAuthorCode(h.getCode());
    log.setAuthorName(h.getName());

    log.setListener(execution.getClass().getName());
    log.setExcutionId(execution.getId());
    log.setType(getLogTypePrefix() + execution.getEventName());
    log.setProcessInstanceId(execution.getProcessInstanceId());
    return log;
  }

  /**
   * 获取日志类型的前缀
   *
   * @return
   */
  protected String getLogTypePrefix() {
    return "excution_";
  }

  /**
   * 创建信息同步日志
   *
   * @param execution
   * @return
   */
  protected ExcutionLog buildSyncInfoExcutionLog(DelegateExecution execution) {
    //创建信息同步日志
    SystemContext sc = SystemContextHolder.get();
    //从线程中获取同步控制值，同步控制值为true时才加入流转日志
    if (sc.getAttr(ExcutionLog.SYNC_INFO_FLAG) != null
      && (Boolean) sc.getAttr(ExcutionLog.SYNC_INFO_FLAG)) {
      Object objSyncInfo = sc.getAttr(ExcutionLog.SYNC_INFO_VALUE);
      //重置同步信息控制 为false
      sc.setAttr(ExcutionLog.SYNC_INFO_FLAG, false);
      //当前用户
      ActorHistory h = sc.getUserHistory();

      if (objSyncInfo == null) {
        ExcutionLog log = new ExcutionLog();
        log.setFileDate(Calendar.getInstance());
        log.setAuthorId(h.getId());
        log.setAuthorCode(h.getCode());
        log.setAuthorName(h.getName());
        log.setListener(execution.getClass().getName());
        log.setExcutionId(execution.getId());
        log.setType(ExcutionLog.TYPE_PROCESS_SYNC_INFO);
        log.setProcessInstanceId(execution.getProcessInstanceId());
        return log;
      } else if (objSyncInfo instanceof String) {
        ExcutionLog log = new ExcutionLog();
        log.setFileDate(Calendar.getInstance());
        log.setAuthorId(h.getId());
        log.setAuthorCode(h.getCode());
        log.setAuthorName(h.getName());
        log.setListener(execution.getClass().getName());
        log.setExcutionId(execution.getId());
        log.setType(ExcutionLog.TYPE_PROCESS_SYNC_INFO);
        log.setProcessInstanceId(execution.getProcessInstanceId());
        //设置详细信息
        log.setDescription(objSyncInfo.toString());
        return log;
      } else if (objSyncInfo instanceof ExcutionLog) {
        return (ExcutionLog) objSyncInfo;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}
