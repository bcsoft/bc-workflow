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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class WorkspaceServiceImplTest {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceServiceImplTest.class);

  @Autowired
  WorkspaceService workspaceService;
  @Autowired
  private UserService userService;
  @Autowired
  private ActorHistoryService actorHistoryService;

  @Test
  public void getProcessInstanceDetail() {
    this.mockSystemContext();

    String processInstanceId = "3349285";//"3326570";
    Map<String, Object> instanceDetail = workspaceService.getProcessInstanceDetail(processInstanceId);

    logger.debug("instanceDetail={}", instanceDetail);
    for (Map.Entry<String, Object> e : instanceDetail.entrySet()) {
      logger.debug("key={}", e.getKey());
      logger.debug("value.class={}", e.getValue() != null ? e.getValue().getClass() : "null");
      logger.debug("value={}", e.getValue());
    }
    logger.debug("variables.class={}", instanceDetail.get("variables").getClass());
    logger.debug("variable={}", instanceDetail.get("variables"));
    /**/
  }


  @Test
  public void getWorkspaceData() {
    this.mockSystemContext();
    String processInstanceId = "3349285";//"3349285";//"3326570";
    Map<String, Object> ws = workspaceService.getWorkspaceData(processInstanceId);
    logger.debug("ws={}", ws);
    for (Map.Entry<String, Object> e : ws.entrySet()) {
      //logger.debug("key={}, value.class={}, value={}", e.getKey(), e.getValue() != null ? e.getValue().getClass() : "null", e.getValue());
    }
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
}