/**
 * 
 */
package cn.bc.workflow.todo.service;

import java.util.Map;

import cn.bc.core.query.Query;
import cn.bc.db.jdbc.SqlObject;


/**
 * 待办Service
 * 
 * @author wis
 */
public interface TodoService {

	/**
	 * 创建指定SqlObject的查询对象
	 * 
	 * @param sqlObject
	 * @return
	 */
	Query<Map<String, Object>> createSqlQuery(
			SqlObject<Map<String, Object>> sqlObject);
}