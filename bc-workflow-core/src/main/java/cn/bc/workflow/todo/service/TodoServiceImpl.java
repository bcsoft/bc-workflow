/**
 *
 */
package cn.bc.workflow.todo.service;

import cn.bc.core.query.Query;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.orm.jpa.JpaNativeQuery;
import cn.bc.workflow.todo.dao.TodoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;

/**
 * 待办Service的实现
 *
 * @author wis
 */
@Service
public class TodoServiceImpl implements TodoService {
  @PersistenceContext
  private EntityManager entityManager;
  @Autowired
  private TodoDao todoDao;

  @Transactional(readOnly = true)
  public Query<Map<String, Object>> createSqlQuery(SqlObject<Map<String, Object>> sqlObject) {
    return new JpaNativeQuery<>(entityManager, sqlObject);
  }

  /**
   * 通过待办任务id判断此待办任务是否签领
   *
   * @param excludeId
   * @return
   */
  @Transactional(readOnly = true)
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
  @Transactional
  public void doSignTask(Long excludeId, String assignee) {
    this.todoDao.doSignTask(excludeId, assignee);
  }

  @Transactional(readOnly = true)
  public List<String> findTaskNames(String account, List<String> groupList) {
    return this.todoDao.findTaskNames(account, groupList);
  }

  @Transactional(readOnly = true)
  public List<String> findProcessNames(String account, List<String> groupList) {
    return this.todoDao.findProcessNames(account, groupList);
  }

  @Transactional(readOnly = true)
  public List<String> findTaskNames() {
    return this.todoDao.findTaskNames();
  }

  @Transactional(readOnly = true)
  public List<String> findProcessNames() {
    return this.todoDao.findProcessNames();
  }
}