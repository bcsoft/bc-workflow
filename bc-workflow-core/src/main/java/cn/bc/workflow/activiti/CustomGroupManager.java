/**
 * 
 */
package cn.bc.workflow.activiti;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;

import cn.bc.identity.domain.Actor;

/**
 * 自定义的Activiti用户组管理器
 * 
 * @author dragon
 * 
 */
public class CustomGroupManager extends GroupManager {
	private static final Log logger = LogFactory
			.getLog(CustomGroupManager.class);
	private JpaTemplate jpaTemplate;

	@Autowired
	public void setJpaTemplate(JpaTemplate jpaTemplate) {
		this.jpaTemplate = jpaTemplate;
	}

	@Override
	public GroupEntity findGroupById(final String groupCode) {
		if (groupCode == null)
			return null;

		return jpaTemplate.execute(new JpaCallback<GroupEntity>() {
			public GroupEntity doInJpa(EntityManager em)
					throws PersistenceException {
				String hql = "select g.code,g.name from bc_identity_actor g where g.type_=? and g.code=?";
				Query queryObject = em.createNativeQuery(hql);

				// 注入参数:jpa的索引号从1开始
				queryObject.setParameter(1, Actor.TYPE_GROUP);
				queryObject.setParameter(2, groupCode);
				queryObject.setFirstResult(0);
				queryObject.setMaxResults(1);

				// test
				if (logger.isDebugEnabled()) {
					logger.debug("args=" + Actor.TYPE_GROUP + "," + groupCode
							+ ";hql=" + hql);
				}

				try {
					Object[] info = (Object[]) queryObject.getSingleResult();
					int i = 0;
					GroupEntity e = new GroupEntity();
					e.setRevision(1);

					// activiti有3种预定义的组类型：security-role、assignment、user
					// 如果使用Activiti
					// Explorer，需要security-role才能看到manage页签，需要assignment才能claim任务
					e.setType("assignment");

					e.setId(info[i++].toString());
					e.setName(info[i++].toString());
					return e;
				} catch (EmptyResultDataAccessException e) {
					return null;
				}
			}
		});
	}

	@Override
	public List<Group> findGroupsByUser(final String userCode) {
		if (userCode == null)
			return null;

		return jpaTemplate.execute(new JpaCallback<List<Group>>() {
			public List<Group> doInJpa(EntityManager em)
					throws PersistenceException {
				String hql = "select g.code,g.name from bc_identity_actor g";
				hql += " inner join bc_identity_actor_relation r on r.master_id=g.id";
				hql += " inner join bc_identity_actor u on u.id=r.follower_id";
				hql += " where g.type_=3 and u.type_=4 and r.type_=0 and u.code=?";
				Query queryObject = em.createNativeQuery(hql);

				// 注入参数:jpa的索引号从1开始
				queryObject.setParameter(1, userCode);
				queryObject.setFirstResult(0);
				queryObject.setMaxResults(1);

				// test
				if (logger.isDebugEnabled()) {
					logger.debug("args=" + userCode + ";hql=" + hql);
				}

				@SuppressWarnings("unchecked")
				List<Object[]> infos = (List<Object[]>) queryObject
						.getResultList();
				List<Group> gs = new ArrayList<Group>();
				GroupEntity g;
				for (Object[] info : infos) {
					g = new GroupEntity();
					g.setRevision(1);
					g.setType("assignment");

					g.setId(info[0].toString());
					g.setName(info[1].toString());
					gs.add(g);
				}
				return gs;
			}
		});
	}

	@Override
	public List<Group> findGroupByQueryCriteria(Object query, Page page) {
		throw new RuntimeException("not implement method.");
	}

	@Override
	public long findGroupCountByQueryCriteria(Object query) {
		throw new RuntimeException("not implement method.");
	}
}
