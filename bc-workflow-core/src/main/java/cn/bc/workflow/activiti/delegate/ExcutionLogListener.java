/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.Calendar;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.service.ExcutionLogService;

/**
 * 记录流程执行日志的通用监听器：可以附加到任何节点
 * 
 * @author dragon
 * 
 */
public class ExcutionLogListener implements ExecutionListener {
	private static final Log logger = LogFactory
			.getLog(ExcutionLogListener.class);
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
}
