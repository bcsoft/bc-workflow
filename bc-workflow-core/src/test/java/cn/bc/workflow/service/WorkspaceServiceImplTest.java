package cn.bc.workflow.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class WorkspaceServiceImplTest {
	@Autowired
	WorkspaceService workspaceService;

	@Test
	public void testFindUserById() {
		String processInstanceId = "1304";
		workspaceService.findWorkspaceInfo(processInstanceId);

		// Assert.assertNotNull(u);
		// Assert.assertEquals("admin", u.getId());
	}
}