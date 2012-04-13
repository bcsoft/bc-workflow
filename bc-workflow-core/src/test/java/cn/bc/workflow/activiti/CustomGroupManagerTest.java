package cn.bc.workflow.activiti;

import java.util.List;

import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import cn.bc.identity.service.GroupService;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class CustomGroupManagerTest {
	@Autowired
	GroupManager groupManager;
	@Autowired
	GroupService groupService;

	@Test
	public void testFindGroupById() {
		// 查询并验证
		GroupEntity u = groupManager.findGroupById("chaojiguanligang");
		Assert.assertNotNull(u);
		Assert.assertEquals("chaojiguanligang", u.getId());
	}

	@Test
	public void testFindGroupsByUser() {
		// 查询并验证
		List<Group> gs = groupManager.findGroupsByUser("admin");
		Assert.assertNotNull(gs);
		Assert.assertEquals(1, gs.size());
		Assert.assertEquals("chaojiguanligang", gs.get(0).getId());
	}
}
