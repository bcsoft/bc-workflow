/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.service.ActorService;

/**
 * 自动加载用户的上级信息
 * <p>
 * 监听器在流程图中需要配置为"java class"类型，Fields参数中有两种配置方式：
 * <ul>
 * </ul>
 * 监听器自动加载用户的上级信息，例如单位或部门，
 * </p>
 * 
 * @author lbj
 * 
 */
public class AutoLoadAssignUpperListener implements TaskListener {
	private static final Log logger = LogFactory
			.getLog(AutoLoadAssignUpperListener.class);

	public void notify(DelegateTask delegateTask) {
		if (logger.isDebugEnabled()) {
			logger.debug("taskDefinitionKey="
					+ delegateTask.getTaskDefinitionKey());
			logger.debug("taskId=" + delegateTask.getId());
			logger.debug("eventName=" + delegateTask.getEventName());
		}
		ActorService actorService = SpringUtils.getBean("actorService",
				ActorService.class);
		//办理人
		Actor actor=actorService.loadByCode(delegateTask.getAssignee());
		
		Actor upper=actorService.loadBelong(actor.getId(), 
				new Integer[] {Actor.TYPE_UNIT,Actor.TYPE_DEPARTMENT});
		
		if(upper!=null){
			delegateTask.setVariable("upperName", upper.getName());
			delegateTask.setVariable("upperCode", upper.getCode());
			delegateTask.setVariable("upperId", upper.getId());
		}
		
	}
}
