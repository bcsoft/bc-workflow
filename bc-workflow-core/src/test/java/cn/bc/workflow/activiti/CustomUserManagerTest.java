package cn.bc.workflow.activiti;

import cn.bc.identity.service.UserService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class CustomUserManagerTest {
  @Autowired
  UserManager userManager;
  @Autowired
  UserService userService;

  @Test
  public void testFindUserById() {
    // // 保存一个账号
    // String userCode = "code";
    // Actor bcUser = new Actor();
    // bcUser.setType(Actor.TYPE_USER);
    // bcUser.setCode(userCode);
    // bcUser.setUid(UUID.randomUUID().toString());
    // bcUser.setName("name");
    // Assert.assertNull(bcUser.getId());
    // this.userService.save(bcUser);
    // Assert.assertNotNull(bcUser.getId());

    // 查询并验证
    UserEntity u = userManager.findUserById("admin");
    Assert.assertNotNull(u);
    Assert.assertEquals("admin", u.getId());
  }

  @Test
  public void testFindGroupsByUser() {
    // 查询并验证
    List<Group> gs = userManager.findGroupsByUser("admin");
    Assert.assertNotNull(gs);
    Assert.assertEquals(1, gs.size());
    Assert.assertEquals("chaojiguanligang", gs.get(0).getId());
  }
}
