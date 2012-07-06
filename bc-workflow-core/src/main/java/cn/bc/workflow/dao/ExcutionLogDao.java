package cn.bc.workflow.dao;

import cn.bc.core.dao.CrudDao;
import cn.bc.workflow.domain.ExcutionLog;

/**
 * 流转日志Dao接口
 * 
 * @author dragon
 * 
 */
public interface ExcutionLogDao extends CrudDao<ExcutionLog> {
	/**
	 * 获取指定任务的流转日志
	 * 
	 * @param taskId
	 *            任务ID
	 * @param type
	 *            日志类型
	 * @return
	 */
	ExcutionLog loadByTask(String taskId, String type);
}
