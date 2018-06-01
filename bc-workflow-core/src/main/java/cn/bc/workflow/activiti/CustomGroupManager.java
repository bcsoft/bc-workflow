/**
 *
 */
package cn.bc.workflow.activiti;

import cn.bc.identity.domain.Actor;
import cn.bc.orm.jpa.JpaUtils;
import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupManager;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义的Activiti用户组管理器
 *
 * @author dragon
 */
@Component("bcGroupManager")
public class CustomGroupManager extends GroupManager {
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public GroupEntity findGroupById(String groupCode) {
    if (groupCode == null)
      return null;

    String sql = "select g.code,g.name from bc_identity_actor g where g.type_=? and g.code=?";
    Query query = JpaUtils.createNativeQuery(entityManager, sql, new Object[]{Actor.TYPE_GROUP, groupCode});
    query.setFirstResult(0);
    query.setMaxResults(1);
    Object[] info = JpaUtils.getSingleResult(query);
    if (info != null) {
      int i = 0;
      GroupEntity e = new GroupEntity();
      e.setRevision(1);

      // activiti有3种预定义的组类型：security-role、assignment、user
      // 如果使用Activiti
      // Explorer，需要security-role才能看到manage页签，需要assignment才能claim任务
      e.setType("assignment");

      e.setId(info[i++].toString());
      e.setName(info[i].toString());
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
  public List<Group> findGroupByQueryCriteria(Object query, Page page) {
    throw new RuntimeException("not implement method.");
  }

  @Override
  public long findGroupCountByQueryCriteria(Object query) {
    throw new RuntimeException("not implement method.");
  }
}