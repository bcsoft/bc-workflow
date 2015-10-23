/**
 *
 */
package cn.bc.workflow.activiti;

import cn.bc.orm.jpa.JpaUtils;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserManager;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义的Activiti用户管理器
 *
 * @author dragon
 */
@Component("bcUserManager")
public class CustomUserManager extends UserManager {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public UserEntity findUserById(String userCode) {
		if (userCode == null)
			return null;

		String sql = "select u.code,u.name,u.email,a.password" +
				" from bc_identity_actor u" +
				" inner join bc_identity_auth a on a.id=u.id" +
				" where u.type_=4 and u.code=?";
		Query query = JpaUtils.createNativeQuery(entityManager, sql, new Object[]{userCode});
		query.setFirstResult(0);
		query.setMaxResults(1);
		Object[] info = JpaUtils.getSingleResult(query);
		if (info != null) {
			int i = 0;
			UserEntity e = new UserEntity();
			e.setRevision(1);
			e.setFirstName(null);

			e.setId((String) info[i++]);
			e.setLastName((String) info[i++]);
			e.setEmail((String) info[i++]);
			e.setPassword((String) info[i]);
			return e;
		} else {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Group> findGroupsByUser(String userCode) {
		if (userCode == null)
			return null;
		String sql = "select g.code,g.name from bc_identity_actor g";
		sql += " inner join bc_identity_actor_relation r on r.master_id=g.id";
		sql += " inner join bc_identity_actor u on u.id=r.follower_id";
		sql += " where g.type_=3 and u.type_=4 and r.type_=0 and u.code=?";
		Query query = JpaUtils.createNativeQuery(entityManager, sql, new Object[]{userCode});
		query.setFirstResult(0);
		query.setMaxResults(1);
		List<Object[]> infos = query.getResultList();

		List<Group> gs = new ArrayList<>();
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

	@Override
	public List<User> findUserByQueryCriteria(Object query, Page page) {
		throw new RuntimeException("not implement method.");
	}

	@Override
	public IdentityInfoEntity findUserInfoByUserIdAndKey(String userId, String key) {
		throw new RuntimeException("not implement method.");
	}

	@Override
	public List<String> findUserInfoKeysByUserIdAndType(String userId, String type) {
		throw new RuntimeException("not implement method.");
	}

	@Override
	public long findUserCountByQueryCriteria(Object query) {
		throw new RuntimeException("not implement method.");
	}
}