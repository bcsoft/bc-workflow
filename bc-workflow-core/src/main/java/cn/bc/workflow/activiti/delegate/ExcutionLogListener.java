/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.Calendar;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.impl.form.StartFormHandler;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.service.ExcutionLogService;

/**
 * 记录流转日志的监听器
 * 
 * @author dragon
 * 
 */
public class ExcutionLogListener implements ExecutionListener {
	private static final Log logger = LogFactory
			.getLog(ExcutionLogListener.class);
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
		log.setType("excution_" + execution.getEventName());
		log.setProcessInstanceId(execution.getProcessInstanceId());

		// 记录流程的表单key
//		if (execution instanceof ExecutionEntity) {
//			ExecutionEntity t = (ExecutionEntity) execution;
//			if (t.getEventSource() instanceof ProcessDefinitionEntity) {
//				ProcessDefinitionEntity d = (ProcessDefinitionEntity) t
//						.getEventSource();
//				if (d != null) {
//					System.out.println("--0:" + d.getClass());
//					StartFormHandler fh = d.getStartFormHandler();
//					if (fh != null) {
//						System.out.println("--1:" + fh.getClass());
//						StartFormData fd = fh.createStartFormData(d);
//						System.out.println("--3:" + fd.getClass());
//						log.setForm(fd != null ? fd.getFormKey() : "");
//					}
//				}
//			}
//		}

		excutionLogService.save(log);
	}
}
