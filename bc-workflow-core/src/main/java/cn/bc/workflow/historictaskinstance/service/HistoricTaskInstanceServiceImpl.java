/**
 * 
 */
package cn.bc.workflow.historictaskinstance.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.core.util.JsonUtils;
import cn.bc.workflow.historictaskinstance.dao.HistoricTaskInstanceDao;
import cn.bc.workflow.service.WorkflowService;

/**
 * 任务监控Service的实现
 * 
 * @author lbj
 */
public class HistoricTaskInstanceServiceImpl implements HistoricTaskInstanceService {
	protected final Log logger = LogFactory.getLog(getClass());

	private HistoricTaskInstanceDao historicTaskInstanceDao;
	private WorkflowService workflowService;
	
	@Autowired
	public void setHistoricTaskInstanceDao(
			HistoricTaskInstanceDao historicTaskInstanceDao) {
		this.historicTaskInstanceDao = historicTaskInstanceDao;
	}
	
	@Autowired
	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	public List<String> findProcessNames(String account, boolean isDone) {
		return this.historicTaskInstanceDao.findProcessNames(account, isDone);
	}

	public List<String> findProcessNames() {
		return this.historicTaskInstanceDao.findProcessNames();
	}

	public List<String> findTaskNames(String account, boolean isDone) {
		return this.historicTaskInstanceDao.findTaskNames(account, isDone);
	}

	public List<String> findTaskNames() {
		return this.historicTaskInstanceDao.findTaskNames();
	}

	public String doStartFlow(String key, String data) throws Exception {
		Map<String,Object> variables=JsonUtils.toMap(data);
		
		//标识由流程发起的流程
		variables.put("isWorkflow", true);
		
		return this.workflowService.startFlowByKey(key, variables);

	}

	public List<String> findTransactors(String processInstanceId) {
		return this.historicTaskInstanceDao.findTransactors(processInstanceId,
				null, null);
	}

	public List<String> findTransactorsByIncludeTaskKeys(
			String processInstanceId, String[] includeTaskKeys) {
		return this.historicTaskInstanceDao.findTransactors(processInstanceId,
				includeTaskKeys, null);
	}

	public List<String> findTransactorsByExclusiveTaskKeys(
			String processInstanceId, String[] exclusiveTaskKeys) {
		return this.historicTaskInstanceDao.findTransactors(processInstanceId,
				null, exclusiveTaskKeys);
	}

	@Override
	public List<Map<String, Object>> findHisProcessTaskVarValue(String processInstanceId, String[] taskKey, String[] varName) {
		return this.historicTaskInstanceDao.findHisProcessTaskVarValue(processInstanceId, taskKey, varName);
	}

}