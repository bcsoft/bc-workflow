package cn.bc.workflow.service;

import java.util.Map;

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

	/**
	 * 获取流程实例所有任务的表单formKey
	 * 
	 * @param processInstanceId
	 *            流程实例ID
	 * @return 返回值中key为任务ID，value为formKey的值
	 */
	Map<String, String> findTaskFormKeys(String processInstanceId);

	/**
	 * 获取任务的表单formKey
	 * 
	 * @param taskId
	 *            任务ID
	 * @return 找不到就返回null
	 */
	String findTaskFormKey(String taskId);

	/**
	 * 获取任务的流程变量
	 * 
	 * @param taskId
	 *            任务ID
	 * @return
	 */
	Map<String, Object> findTaskVariables(String taskId);

	/**
	 * 获取任务指定名称的本地流程实例的值
	 * 
	 * @param taskId
	 *            任务ID
	 * @param variableName
	 *            变量名
	 * @return
	 */
	Object getTaskVariableLocal(String taskId, String variableName);
}
