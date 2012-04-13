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
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;

/**
 * 自定义的Activiti用户管理器
 * 
 * @author dragon
 * 
 */
public class CustomUserManager extends UserManager {
	private static final Log logger = LogFactory
			.getLog(CustomUserManager.class);
	private JpaTemplate jpaTemplate;

	@Autowired
	public void setJpaTemplate(JpaTemplate jpaTemplate) {
		this.jpaTemplate = jpaTemplate;
	}

	@Override
	public UserEntity findUserById(final String userCode) {
		if (userCode == null)
			return null;

		try {
			return jpaTemplate.execute(new JpaCallback<UserEntity>() {
				public UserEntity doInJpa(EntityManager em)
						throws PersistenceException {
					String hql = "select u.code,u.name,u.email,a.password from bc_identity_actor u inner join bc_identity_auth a on a.id=u.id";
					hql += " where u.type_=4 and u.code=?";
					Query queryObject = em.createNativeQuery(hql);

					// 注入参数:jpa的索引号从1开始
					queryObject.setParameter(1, userCode);
					queryObject.setFirstResult(0);
					queryObject.setMaxResults(1);

					// test
					if (logger.isDebugEnabled()) {
						logger.debug("args=" + userCode + ";hql=" + hql);
					}

					Object[] info = (Object[]) queryObject.getSingleResult();
					int i = 0;
					UserEntity e = new UserEntity();
					e.setRevision(1);
					e.setFirstName(null);

					e.setId((String) info[i++]);
					e.setLastName((String) info[i++]);
					e.setEmail((String) info[i++]);
					e.setPassword((String) info[i++]);
					return e;
				}
			});
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
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
	public List<User> findUserByQueryCriteria(Object query, Page page) {
		throw new RuntimeException("not implement method.");
	}

	@Override
	public IdentityInfoEntity findUserInfoByUserIdAndKey(String userId,
			String key) {
		throw new RuntimeException("not implement method.");
	}

	@Override
	public List<String> findUserInfoKeysByUserIdAndType(String userId,
			String type) {
		throw new RuntimeException("not implement method.");
	}

	@Override
	public long findUserCountByQueryCriteria(Object query) {
		throw new RuntimeException("not implement method.");
	}
}
