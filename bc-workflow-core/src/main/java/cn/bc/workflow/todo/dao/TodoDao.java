/**
 * 
 */
package cn.bc.workflow.todo.dao;


/**
 * 待办Dao
 * 
 * @author wis
 */
public interface TodoDao {

	/**
	 * 通过待办任务id判断此待办任务是否签领
	 * @param excludeId
	 * @return
	 */
	Long checkIsSign(Long excludeId);
	
	/**
	 * 通过待办任务id用户实现签领
	 * @param excludeId
	 * @param assignee 
	 */
	@Deprecated
	void doSignTask(Long excludeId, String assignee);

}