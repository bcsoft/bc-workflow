/**
 * 
 */
package cn.bc.workflow.historictaskinstance.dao;

import java.util.List;
import java.util.Map;


/**
 * 任务监控Dao
 * 
 * @author lbj
 */
public interface HistoricTaskInstanceDao {

	/**
	 * 查找用户任务中的流程名称
	 * 
	 * @param account 用户账号
	 * @param isDone 是否只查找已完成任务的流程
	 * @return
	 */
	List<String> findProcessNames(String account,boolean isDone);
	
	/**
	 * 查找流程名称
	 * 
	 * @return
	 */
	List<String> findProcessNames();
	
	
	/**
	 * 查找用户任务名称
	 * 
	 * @param account 用户账号
	 * @param isDone 是否只查找已完成任务的流程
	 * @return
	 */
	List<String> findTaskNames(String account,boolean isDone);
	
	/**
	 * 查找任务名称
	 * 
	 * @return
	 */
	List<String> findTaskNames();

	/**
	 * 查找流程历史办理人
	 * 
	 * @param processInstanceId 流程实例ID
	 * @param includeTaskKeys 在这些key中查找
	 * @param exclusiveTaskKeys 不在这些key中查找
	 * @return
	 */
	List<String> findTransactors(String processInstanceId,
			String[] includeTaskKeys, String[] exclusiveTaskKeys);

	/**
	 * 查找历史流程任务变量值，变量为本地变量
	 *
	 * @param processInstanceId 流程实例Id
	 * @param taskKey 任务key
	 * @param varName 变量名
	 * @return
	 */
	List<Map<String, Object>> findHisProcessTaskVarValue(String processInstanceId, String[] taskKey, String[] varName);

}