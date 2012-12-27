/**
 * 
 */
package cn.bc.workflow.historictaskinstance.dao;

import java.util.List;


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

}