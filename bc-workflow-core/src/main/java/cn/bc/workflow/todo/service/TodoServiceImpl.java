/**
 * 
 */
package cn.bc.workflow.todo.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaTemplate;

import cn.bc.core.query.Query;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.orm.hibernate.jpa.HibernateJpaNativeQuery;
import cn.bc.workflow.todo.dao.TodoDao;

/**
 * 待办Service的实现
 * 
 * @author wis
 */
public class TodoServiceImpl implements TodoService {
	protected final Log logger = LogFactory.getLog(getClass());

	private JpaTemplate jpaTemplate;
	private TodoDao todoDao;
	
	@Autowired
	public void setJpaTemplate(JpaTemplate jpaTemplate) {
		this.jpaTemplate = jpaTemplate;
	}
	
	@Autowired
	public void setTodoDao(TodoDao todoDao) {
		this.todoDao = todoDao;
	}

	public Query<Map<String, Object>> createSqlQuery(
			SqlObject<Map<String, Object>> sqlObject) {
		return new HibernateJpaNativeQuery<Map<String, Object>>(jpaTemplate,
				sqlObject);
	}

	/**
	 * 通过待办任务id判断此待办任务是否签领
	 * @param excludeId
	 * @return
	 */
	public Long checkIsSign(Long excludeId) {
		return this.todoDao.checkIsSign(excludeId);
	}

	/**
	 * 通过待办任务id用户实现签领
	 * @param excludeId
	 * @param assignee 
	 */
	@Deprecated
	public void doSignTask(Long excludeId,String assignee) {
		this.todoDao.doSignTask(excludeId,assignee);
	}

	public List<String> findTaskNames(String account, List<String> groupList) {
		return this.todoDao.findTaskNames(account, groupList);
	}

	public List<String> findProcessNames(String account, List<String> groupList) {
		return this.todoDao.findProcessNames(account, groupList);
	}

	public List<String> findTaskNames() {
		return this.todoDao.findTaskNames();
	}

	public List<String> findProcessNames() {
		return this.todoDao.findProcessNames();
	}
	
}