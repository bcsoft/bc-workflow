/**
 *
 */
package cn.bc.workflow.historictaskinstance.service;

import cn.bc.core.util.JsonUtils;
import cn.bc.workflow.historictaskinstance.dao.HistoricTaskInstanceDao;
import cn.bc.workflow.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务监控Service的实现
 *
 * @author lbj
 */
@Service("historicTaskInstanceService")
public class HistoricTaskInstanceServiceImpl implements HistoricTaskInstanceService {
	@Autowired
	private HistoricTaskInstanceDao historicTaskInstanceDao;
	@Autowired
	private WorkflowService workflowService;

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
		Map<String, Object> variables = JsonUtils.toMap(data);

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

	@Override
	public Date findProcessInstanceStartTime(String processInstanceId) {
		return this.historicTaskInstanceDao.findProcessInstanceStartTime(processInstanceId);
	}

	@Override
	public Date findProcessInstanceTaskStartTime(String processInstanceId,
	                                             String taskCode) {
		return this.historicTaskInstanceDao.findProcessInstanceTaskStartTime(processInstanceId, taskCode);
	}

	@Override
	public Date findProcessInstanceTaskEndTime(String processInstanceId,
	                                           List<String> taskCodes) {
		return this.historicTaskInstanceDao.findProcessInstanceTaskEndTime(processInstanceId, taskCodes);
	}
}