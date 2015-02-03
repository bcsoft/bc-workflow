package cn.bc.workflow.service;

import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.service.ActorHistoryService;
import cn.bc.identity.service.UserService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.identity.web.SystemContextImpl;
import org.commontemplate.util.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class WorkflowServiceImplTest {
	private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImplTest.class);

	@Autowired
	WorkflowService workflowService;
	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	private UserService userService;
	@Autowired
	private ActorHistoryService actorHistoryService;

	@Test
	public void nativeDeal() throws Exception {
		String deploymentId = "181400";
		//String resourceName = "GeneralOrder.GeneralOrder.png";
		String resourceName = "GeneralOrder.bpmn20.xml";
		String sql = "select bytes_ from act_ge_bytearray where deployment_id_ = ? and name_ = ?";
		byte[] data = this.jdbcTemplate.queryForObject(sql, new Object[]{deploymentId, resourceName}, byte[].class);

		// 保存为文件
		FileCopyUtils.copy(data, new FileOutputStream("d:/t/" + resourceName));
	}

	@Test
	public void getDeploymentResource() throws Exception {
		this.mockSystemContext();

		String deploymentId = "181400";
		//String resourceName = "GeneralOrder.GeneralOrder.png";
		String resourceName = "GeneralOrder.bpmn20.xml";
		InputStream inputStream = this.workflowService.getDeploymentResource(deploymentId, resourceName);
		logger.info("inputStream.class=", inputStream != null ? inputStream.getClass() : null);

		// 保存为文件
		FileCopyUtils.copy(inputStream, new FileOutputStream("d:/" + resourceName));
	}

	private void mockSystemContext() {
		SystemContext context = new SystemContextImpl();
		SystemContextHolder.set(context);

		// 系统上下文
		context.setAttr(SystemContext.KEY_SYSCONTEXTPATH, "/bctest");
		context.setAttr(SystemContext.KEY_HTMLPAGENAMESPACE, "/webapp/bc");
		context.setAttr(SystemContext.KEY_APPTS, new Date().getTime());

		// 用户信息
		Actor user = this.userService.loadByCode("admin");
		context.setAttr(SystemContext.KEY_USER, user);
		ActorHistory userHistory = this.actorHistoryService.loadByCode("admin");
		context.setAttr(SystemContext.KEY_USER_HISTORY, userHistory);
	}

    @Test
    public void updateDeploymentResource() throws IOException {
        // 文件输入流
        InputStream in = WorkflowServiceImplTest.class.getClassLoader().getResourceAsStream("cn/bc/workflow/examples/ApprovalItem.bpmn20.xml");
        // ApprovalItem.bar的id
        String deployment_id = "3858400";
        // 资源文件名
        String resourceName = "ApprovalItem.bpmn20.xml";

        Assert.assertTrue(this.workflowService.updateDeploymentResource(deployment_id, resourceName, in));
    }
}