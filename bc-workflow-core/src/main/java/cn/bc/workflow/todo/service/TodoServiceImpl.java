/**
 *
 */
package cn.bc.workflow.todo.service;

import cn.bc.core.query.Query;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.orm.jpa.JpaNativeQuery;
import cn.bc.workflow.todo.dao.TodoDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;

/**
 * 待办Service的实现
 *
 * @author wis
 */
public class TodoServiceImpl implements TodoService {
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@PersistenceContext
	private EntityManager entityManager;
	private TodoDao todoDao;

	@Autowired
	public void setTodoDao(TodoDao todoDao) {
		this.todoDao = todoDao;
	}

	public Query<Map<String, Object>> createSqlQuery(SqlObject<Map<String, Object>> sqlObject) {
		return new JpaNativeQuery<>(entityManager, sqlObject);
	}

	/**
	 * 通过待办任务id判断此待办任务是否签领
	 *
	 * @param excludeId
	 * @return
	 */
	public Long checkIsSign(Long excludeId) {
		return this.todoDao.checkIsSign(excludeId);
	}

	/**
	 * 通过待办任务id用户实现签领
	 *
	 * @param excludeId
	 * @param assignee
	 */
	@Deprecated
	public void doSignTask(Long excludeId, String assignee) {
		this.todoDao.doSignTask(excludeId, assignee);
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