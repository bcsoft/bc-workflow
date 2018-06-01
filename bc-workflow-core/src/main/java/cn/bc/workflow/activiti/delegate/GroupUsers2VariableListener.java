/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.BCConstants;
import cn.bc.core.exception.CoreException;
import cn.bc.core.util.SpringUtils;
import cn.bc.identity.service.UserService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.el.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 用指定岗位中的所有用户的编码生成一个全局流程变量，
 * 在多实例任务中用于设置用户列表
 *
 * @author dragon
 */
public class GroupUsers2VariableListener implements ExecutionListener {
  private static final Logger logger = LoggerFactory.getLogger(GroupUsers2VariableListener.class);

  /**
   * 岗位编码
   */
  private Expression groupCode;

  /**
   * 全局变量名，默认为 users
   */
  private Expression variableName;

  public void notify(DelegateExecution execution) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("groupCode={}", groupCode != null ? groupCode.getExpressionText() : "null");
      logger.debug("variableName={}", variableName != null ? variableName.getExpressionText() : "null");
    }

    // 验证
    if (groupCode == null) throw new CoreException("没有配置岗位编码的值！");

    // 获取岗位内的有效用户
    UserService userService = SpringUtils.getBean(UserService.class);
    List<String> users = userService.findAllUserCodeByGroup(groupCode.getExpressionText(), new Integer[]{BCConstants.STATUS_ENABLED});
    logger.debug("users={}", users);

    // 设置全局变量
    execution.setVariable(variableName != null ? variableName.getExpressionText() : "users", users);
  }
}
