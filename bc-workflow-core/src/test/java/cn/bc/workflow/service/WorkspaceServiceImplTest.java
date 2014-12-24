package cn.bc.workflow.service;

import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.service.ActorHistoryService;
import cn.bc.identity.service.UserService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.identity.web.SystemContextImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class WorkspaceServiceImplTest {
	@Autowired
	WorkspaceService workspaceService;
	@Autowired
	private UserService userService;
	@Autowired
	private ActorHistoryService actorHistoryService;

	@Test
	public void findWorkspaceInfo() {
		this.mockSystemContext();

		String processInstanceId = "3326570";
		Map<String, Object> ws = workspaceService.findWorkspaceInfo(processInstanceId);

		// Assert.assertNotNull(u);
		// Assert.assertEquals("admin", u.getId());
	}

	private void mockSystemContext() {
		SystemContext context = new SystemContextImpl();
		SystemContextHolder.set(context);

		// 系统上下文路径
		context.setAttr(SystemContext.KEY_SYSCONTEXTPATH, "/bctest");

		// 用户信息
		Actor user = this.userService.loadByCode("admin");
		context.setAttr(SystemContext.KEY_USER, user);
		ActorHistory userHistory = this.actorHistoryService.loadByCode("admin");
		context.setAttr(SystemContext.KEY_USER_HISTORY, userHistory);
	}
}