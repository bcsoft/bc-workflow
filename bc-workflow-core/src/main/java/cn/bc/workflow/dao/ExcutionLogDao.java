package cn.bc.workflow.dao;

import java.util.Map;

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

	/**
	 * 获取流程实例所有任务的表单formKey
	 * 
	 * @param processInstanceId
	 *            流程实例ID
	 * @return 返回值值中key为任务ID，value为formKey的值
	 */
	Map<String, String> findTaskFormKeys(String processInstanceId);

	/**
	 * 获取任务的流程变量
	 * 
	 * @param taskId
	 *            任务ID
	 * @return
	 */
	Map<String, Object> findTaskVariables(String taskId);

	/**
	 * 获取任务的formKey
	 * 
	 * @param taskId
	 *            任务ID
	 * @return
	 */
	String findTaskFormKey(String taskId);
}
