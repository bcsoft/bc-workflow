package cn.bc.workflow.service;

import cn.bc.core.service.DefaultCrudService;
import cn.bc.identity.dao.ActorHistoryDao;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.workflow.dao.ExcutionLogDao;
import cn.bc.workflow.domain.ExcutionLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 流转日志Service接口实现
 *
 * @author dragon
 */
@Service("excutionLogService")
public class ExcutionLogServiceImpl extends DefaultCrudService<ExcutionLog> implements ExcutionLogService {
	@Autowired
	private ActorHistoryDao actorHistoryDao;
	private ExcutionLogDao excutionLogDao;

	@Autowired
	public void setExcutionLogDao(ExcutionLogDao excutionLogDao) {
		this.excutionLogDao = excutionLogDao;
		this.setCrudDao(excutionLogDao);
	}

	public ActorHistory getSender(String taskId) {
		ExcutionLog log = excutionLogDao.loadByTask(taskId, ExcutionLog.TYPE_TASK_INSTANCE_CREATE);
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

	public Object getTaskVariableLocal(String taskId, String variableName) {
		return this.excutionLogDao.getTaskVariableLocal(taskId, variableName);
	}
}
