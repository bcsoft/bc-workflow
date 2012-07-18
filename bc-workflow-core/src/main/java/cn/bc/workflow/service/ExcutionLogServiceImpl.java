package cn.bc.workflow.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.core.service.DefaultCrudService;
import cn.bc.identity.dao.ActorHistoryDao;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.workflow.dao.ExcutionLogDao;
import cn.bc.workflow.domain.ExcutionLog;

/**
 * 流转日志Service接口实现
 * 
 * @author dragon
 * 
 */
public class ExcutionLogServiceImpl extends DefaultCrudService<ExcutionLog>
		implements ExcutionLogService {

	private ExcutionLogDao excutionLogDao;
	private ActorHistoryDao actorHistoryDao;

	@Autowired
	public void setExcutionLogDao(ExcutionLogDao excutionLogDao) {
		this.excutionLogDao = excutionLogDao;
		this.setCrudDao(excutionLogDao);
	}

	@Autowired
	public void setActorHistoryDao(ActorHistoryDao actorHistoryDao) {
		this.actorHistoryDao = actorHistoryDao;
	}

	public ActorHistory getSender(String taskId) {
		ExcutionLog log = excutionLogDao.loadByTask(taskId,
				ExcutionLog.TYPE_TASK_INSTANCE_CREATE);
		return actorHistoryDao.load(log.getAuthorId());
	}

	public Map<String, String> findTaskFormKeys(String processInstanceId) {
		return excutionLogDao.findTaskFormKeys(processInstanceId);
	}

	public String findTaskFormKey(String taskId) {
		return excutionLogDao.findTaskFormKey(taskId);
	}

	public Map<String, Object> findTaskVariables(String taskId) {
		return this.excutionLogDao.findTaskVariables(taskId);
	}
}
