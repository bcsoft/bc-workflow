package cn.bc.workflow.dao;

import java.util.Map;



/**
 * 流程dao
 * 
 * @author lbj
 * 
 */
public interface WorkflowDao {
	
	/**
	 * 查找流程全局变量
	 * 
	 * @param pid:流程实例id
	 * @param valueKeys:流程全局变量key
	 * @return {流程全部变量key:流程全部变量value}
	 */
	Map<String,Object> findGlobalValue(String pid,String[] valueKeys);
}
