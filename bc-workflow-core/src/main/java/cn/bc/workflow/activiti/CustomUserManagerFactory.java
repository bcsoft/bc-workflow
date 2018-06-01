/**
 *
 */
package cn.bc.workflow.activiti;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 自定义的Activiti用户会话工厂
 *
 * @author dragon
 */
public class CustomUserManagerFactory implements SessionFactory {
  private UserManager userManager;

  @Autowired
  public void setUserManager(UserManager userManager) {
    this.userManager = userManager;
  }

  public Class<?> getSessionType() {
    // 返回原始的UserManager类型
    return UserManager.class;
  }

  public Session openSession() {
    // 返回自定义的UserManager实例
    return userManager;
  }
}
