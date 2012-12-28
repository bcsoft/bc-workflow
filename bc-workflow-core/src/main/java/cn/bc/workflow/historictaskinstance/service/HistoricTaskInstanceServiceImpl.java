/**
 * 
 */
package cn.bc.workflow.historictaskinstance.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.workflow.historictaskinstance.dao.HistoricTaskInstanceDao;

/**
 * 任务监控Service的实现
 * 
 * @author lbj
 */
public class HistoricTaskInstanceServiceImpl implements HistoricTaskInstanceService {
	protected final Log logger = LogFactory.getLog(getClass());

	private HistoricTaskInstanceDao historicTaskInstanceDao;
	
	@Autowired
	public void setHistoricTaskInstanceDao(
			HistoricTaskInstanceDao historicTaskInstanceDao) {
		this.historicTaskInstanceDao = historicTaskInstanceDao;
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
	
	
}