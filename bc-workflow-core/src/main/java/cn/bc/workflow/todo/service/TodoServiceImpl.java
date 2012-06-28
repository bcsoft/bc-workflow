/**
 * 
 */
package cn.bc.workflow.todo.service;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaTemplate;

import cn.bc.core.query.Query;
import cn.bc.db.jdbc.SqlObject;
import cn.bc.orm.hibernate.jpa.HibernateJpaNativeQuery;

/**
 * 待办Service的实现
 * 
 * @author wis
 */
public class TodoServiceImpl implements TodoService {
	protected final Log logger = LogFactory.getLog(getClass());

	private JpaTemplate jpaTemplate;
	
	@Autowired
	public void setJpaTemplate(JpaTemplate jpaTemplate) {
		this.jpaTemplate = jpaTemplate;
	}
	
	public Query<Map<String, Object>> createSqlQuery(
			SqlObject<Map<String, Object>> sqlObject) {
		return new HibernateJpaNativeQuery<Map<String, Object>>(jpaTemplate,
				sqlObject);
	}
	
}