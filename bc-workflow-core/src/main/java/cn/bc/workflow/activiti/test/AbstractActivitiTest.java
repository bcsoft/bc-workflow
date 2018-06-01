package cn.bc.workflow.activiti.test;

import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.identity.web.SystemContextImpl;
import cn.bc.web.util.WebUtils;
import org.activiti.engine.*;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * activiti流程测试基类
 *
 * @author dragon
 */
@Transactional
public class AbstractActivitiTest {
  @Autowired
  protected RuntimeService runtimeService;

  @Autowired
  protected IdentityService identityService;

  @Autowired
  protected TaskService taskService;

  @Autowired
  protected FormService formService;

  @Autowired
  protected HistoryService historyService;

  @Autowired
  @Rule
  public ActivitiRule activitiSpringRule;

  /**
   * 初始化上下文
   *
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    // 设置当前用户信息的上下文
    initContext();

    // 设置web文件所在的父路径
    initWeb();
  }

  /**
   * 设置当前用户信息的上下文
   */
  protected void initContext() {
    SystemContext context = new SystemContextImpl();
    SystemContextHolder.set(context);
    ActorHistory h = new ActorHistory();
    h.setId(new Long(1146));
    h.setActorId(new Long(9));
    h.setActorType(4);
    h.setCode("admin");
    h.setName("系统管理员");
    h.setPname("宝城总部/信息技术部");
    context.setAttr(SystemContext.KEY_USER_HISTORY, h);
  }

  /**
   * 设置web文件所在的父路径
   */
  protected void initWeb() {
    WebUtils.rootPath = "/Work/bc/bc-system/src/main/webapp";
  }

  /**
   * 设置activiti的当前认证用户信息
   *
   * @param user
   */
  protected void setAuthenticatedUser(String user) {
    identityService.setAuthenticatedUserId(user);
  }
}
