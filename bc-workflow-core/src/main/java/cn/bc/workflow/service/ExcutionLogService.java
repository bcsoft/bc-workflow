package cn.bc.workflow.service;

import cn.bc.core.dao.CrudDao;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.workflow.domain.ExcutionLog;

/**
 * 流转日志Service接口
 * <p>
 * 如可通过任务的id获取任务的创建人信息
 * </p>
 * 
 * @author dragon
 * 
 */
public interface ExcutionLogService extends CrudDao<ExcutionLog> {
	/**
	 * 获取任务的发送人信息
	 * 
	 * @param taskId
	 *            任务ID
	 * @return
	 */
	ActorHistory getSender(String taskId);
}
