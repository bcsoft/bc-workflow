/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.Map;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 自动分配多实例任务办理人
 * <p>
 * 监听器在流程图中需要配置为"java class"类型，Fields参数中有两种配置方式：
 * <ul>
 * </ul>
 * 监听器自动分配多实例任务办理人，并设置mcode值
 * </p>
 * 
 * @author lbj
 * 
 */
public class MultiInstanceAssigneeListener implements TaskListener{
	private static final Log logger = LogFactory
			.getLog(MultiInstanceAssigneeListener.class);
	
	public void notify(DelegateTask delegateTask) {
		if (logger.isDebugEnabled()) {
			logger.debug("taskDefinitionKey="
					+ delegateTask.getTaskDefinitionKey());
			logger.debug("taskId=" + delegateTask.getId());
			logger.debug("eventName=" + delegateTask.getEventName());
		}
		
		@SuppressWarnings("rawtypes")
		Map mvariable=(Map) delegateTask.getVariable("multiInstanceCollentionKey");
		
		//设置任务办理人
		delegateTask.setAssignee(mvariable.get("assignee").toString());
			
		//设置其它变量
		for(Object o:mvariable.keySet()){
			if(!o.toString().equals("assignee")){
				delegateTask.setVariableLocal(o.toString(), mvariable.get(o.toString()).toString());
			}
		}
	}
}
