/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.BCConstants;
import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.service.ActorService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

/**
 * 自动分配多实例任务办理人或办理岗位
 * <p>使用该监听器需配置 Multi Instance -> Collection 和 Element variable</p>
 * <ul>
 * <li><b>Collection：</b> List<Map>集合，map 数据结构 {id: 111, "groupOrAssiness": "group","group":"岗位Code", "assignee":"用户Code", "subject":"标题的本地key"...}
 * <ul>
 * <li>key=groupOrAssiness，value="group" 可选,值为“group”时送岗位；为null时Map中必须有key=assignee，value=用户Code</li>
 * <li>key=group，value=岗位Code 可选</li>
 * <li>key=assignee，value=用户Code 可选</li>
 * <li>key=subject 可选，任务标题</li>
 * </ul>
 * </li>
 * <li><b>Element variable：</b> 配置为：multiInstanceCollentionKey，activiti会迭代list集合，通过配置“Element variable”作为key获取当前的map对象</li>
 * </ul>
 * <p>
 * 监听器在流程图中需要配置为"java class"类型，Fields参数中有两种配置方式：
 * <ul>
 * </ul>
 * 监听器自动分配多实例任务办理人，并设置mcode值
 * </p>
 *
 * @author lbj
 */
public class MultiInstanceAssigneeListener implements TaskListener {
  private static final Log logger = LogFactory
    .getLog(MultiInstanceAssigneeListener.class);

  /**
   * 岗位只有一个人是直接分配到人
   */
  private Expression onlyOneUser;

  public void notify(DelegateTask delegateTask) {
    if (logger.isDebugEnabled()) {
      logger.debug("taskDefinitionKey="
        + delegateTask.getTaskDefinitionKey());
      logger.debug("taskId=" + delegateTask.getId());
      logger.debug("eventName=" + delegateTask.getEventName());
    }

    @SuppressWarnings("rawtypes")
    Map mvariable = (Map) delegateTask.getVariable("multiInstanceCollentionKey");
    Object doa = mvariable.get("groupOrAssignee");
    String doaKey = null;

    if (doa != null && "group".equals(doa)) {
      doaKey = (String) doa;
      String groupCode = mvariable.get(doaKey).toString();// 岗位Code

      // 岗位只有一人直接分配到人
      if (onlyOneUser != null && "true".equals(onlyOneUser.getExpressionText())) {
        ActorService actorService = SpringUtils.getBean("actorService", ActorService.class);

        Actor group = actorService.loadByCode(groupCode);
        List<Actor> listUsers = actorService.findUser(group.getId(), new Integer[]{BCConstants.STATUS_ENABLED});

        if (listUsers != null && listUsers.size() == 1) {// 分配到人
          delegateTask.setAssignee(listUsers.get(0).getCode());
        } else {// 分配到岗位
          delegateTask.addCandidateGroup(groupCode);
        }
      } else {// 直接分配到岗位
        // 设置任务办理岗位
        delegateTask.addCandidateGroup(groupCode);
      }

    } else if (doa == null || doa != null && "assignee".equals(doa)) {
      doaKey = "assignee";
      // 设置任务办理人
      delegateTask.setAssignee(mvariable.get(doaKey).toString());
    }

    //设置其它变量
    for (Object o : mvariable.keySet()) {
      if (!o.toString().equals(doaKey)) {
        delegateTask.setVariableLocal(o.toString(), mvariable.get(o.toString()).toString());
      }
    }
  }
}
