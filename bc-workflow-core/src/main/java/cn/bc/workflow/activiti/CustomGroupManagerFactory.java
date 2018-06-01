/**
 *
 */
package cn.bc.workflow.activiti;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.GroupManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 自定义的Activiti用户组会话工厂
 *
 * @author dragon
 */
public class CustomGroupManagerFactory implements SessionFactory {
  private GroupManager groupManager;

  @Autowired
  public void setGroupManager(GroupManager groupManager) {
    this.groupManager = groupManager;
  }

  public Class<?> getSessionType() {
    // 返回原始的GroupManager类型
    return GroupManager.class;
  }

  public Session openSession() {
    // 返回自定义的GroupManager实例
    return groupManager;
  }
}
