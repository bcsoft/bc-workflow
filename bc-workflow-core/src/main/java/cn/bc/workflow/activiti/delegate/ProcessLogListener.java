/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.Calendar;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.service.ExcutionLogService;

/**
 * 记录流程实例的启动、结束等日志的监听器
 * 
 * @author dragon
 * 
 */
public class ProcessLogListener implements ExecutionListener {
	private static final Log logger = LogFactory
			.getLog(ProcessLogListener.class);
	private ExcutionLogService excutionLogService;

	@Autowired
	public void setExcutionLogService(ExcutionLogService excutionLogService) {
		this.excutionLogService = excutionLogService;
	}

	public void notify(DelegateExecution execution) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("class=" + execution.getClass());
			logger.debug("id=" + execution.getId());
			logger.debug("eventName=" + execution.getEventName());
			logger.debug("processInstanceId" + execution.getProcessInstanceId());
			logger.debug("processBusinessKey="
					+ execution.getProcessBusinessKey());
		}

		// 创建执行日志
		ExcutionLog log = new ExcutionLog();
		log.setFileDate(Calendar.getInstance());
		ActorHistory h = SystemContextHolder.get().getUserHistory();
		log.setAuthorId(h.getId());
		log.setAuthorCode(h.getCode());
		log.setAuthorName(h.getName());

		log.setListener(execution.getClass().getName());
		log.setExcutionId(execution.getId());
		log.setType("process_" + execution.getEventName());
		log.setProcessInstanceId(execution.getProcessInstanceId());

		excutionLogService.save(log);
	}
}
