package cn.bc.workflow.dao.hibernate.jpa;

import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.orm.hibernate.jpa.HibernateCrudJpaDao;
import cn.bc.workflow.dao.ExcutionLogDao;
import cn.bc.workflow.domain.ExcutionLog;

/**
 * 流转日志Dao接口的实现
 * 
 * @author dragon
 * 
 */
public class ExcutionLogDaoImpl extends HibernateCrudJpaDao<ExcutionLog>
		implements ExcutionLogDao {

	public ExcutionLog loadByTask(String taskId, String type) {
		return this
				.createQuery()
				.condition(
						new AndCondition().add(
								new EqualsCondition("taskInstanceId", taskId))
								.add(new EqualsCondition("type", type)))
				.singleResult();
	}
}
