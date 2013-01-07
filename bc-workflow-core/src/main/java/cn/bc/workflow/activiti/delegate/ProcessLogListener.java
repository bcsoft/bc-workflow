/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.domain.ExcutionLog;

/**
 * 记录流程实例的启动、结束日志的监听器
 * 
 * @author dragon
 * 
 */
public class ProcessLogListener extends ExcutionLogListener {
	@Override
	protected String getLogTypePrefix() {
		return "process_";
	}

	@Override
	protected ExcutionLog buildExcutionLog(DelegateExecution execution) {
		ExcutionLog log = super.buildExcutionLog(execution);

		// 记录流程的编码
		if (execution instanceof ExecutionEntity) {
			ExecutionEntity e = (ExecutionEntity) execution;
			log.setExcutionCode(e.getProcessDefinitionId());
			log.setExcutionName(e.getProcessDefinition().getName());
		}

		return log;
	}
	
	@Override
	protected ExcutionLog buildSyncInfoExcutionLog(DelegateExecution execution) {
		ExcutionLog log = super.buildSyncInfoExcutionLog(execution);
		SystemContext sc= SystemContextHolder.get();
		if(log!=null
			&&sc.getAttr(ExcutionLog.SYNC_INFO_VALUE)!=null
				 &&!(sc.getAttr(ExcutionLog.SYNC_INFO_VALUE) instanceof ExcutionLog)){
			// 记录流程的编码
			if (execution instanceof ExecutionEntity) {
				ExecutionEntity e = (ExecutionEntity) execution;
				log.setExcutionCode(e.getProcessDefinitionId());
				log.setExcutionName(e.getProcessDefinition().getName());
			}
		}
		
		return log;
	}
}
