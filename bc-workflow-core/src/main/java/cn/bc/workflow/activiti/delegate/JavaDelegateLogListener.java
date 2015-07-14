/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.service.ExcutionLogService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * 记录流转日志的监听器
 *
 * @author dragon
 */
public class JavaDelegateLogListener implements JavaDelegate {
	private static final Logger logger = LoggerFactory.getLogger(JavaDelegateLogListener.class);
	private ExcutionLogService excutionLogService;

	public JavaDelegateLogListener() {
		excutionLogService = SpringUtils.getBean(ExcutionLogService.class);
	}

	public void execute(DelegateExecution execution) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execution=" + execution.getClass());
			logger.debug("this=" + this.getClass());
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
		log.setType("javaDelegate_" + execution.getEventName());
		log.setProcessInstanceId(execution.getProcessInstanceId());
		excutionLogService.save(log);
	}
}
