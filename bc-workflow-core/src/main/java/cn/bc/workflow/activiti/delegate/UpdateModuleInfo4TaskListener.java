/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.core.util.JsonUtils;
import cn.bc.core.util.SpringUtils;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.service.ExcutionLogService;

/**
 * 流程更新模块的相关信息监听器
 * 
 * @author zxr
 * 
 */
public class UpdateModuleInfo4TaskListener implements TaskListener {
	protected final Log logger = LogFactory.getLog(getClass());
	
	protected ExcutionLogService excutionLogService;

	/**
	 * service名称
	 */
	private Expression serviceName;
	/**
	 * service方法
	 */
	private Expression serviceMethod;
	/**
	 * 更新的参数 标准格式的json字符串如{"domain属性名":"属性值","name":"张三","sex":"男"...}
	 * 暂不支持更新日期格式
	 */
	private Expression parameter;

	/**
	 * 是否执行更新方法
	 */
	private Expression isExecuteUpdateMethod;

	/**
	 * 更新对象的id
	 */
	private Expression updateObjectId;
	
	/**
	 * 同步日志记录
	 */
	private Expression log;
	
	public UpdateModuleInfo4TaskListener(){
		excutionLogService = SpringUtils.getBean(ExcutionLogService.class);
	}

	public void notify(DelegateTask arg0) {
		if (logger.isDebugEnabled()) {
			logger.debug("execution=" + arg0.getClass());
			logger.debug("this=" + this.getClass());
			logger.debug("id=" + arg0.getId());
			logger.debug("eventName=" + arg0.getEventName());
			logger.debug("processInstanceId"
					+ arg0.getProcessInstanceId());
			logger.debug("executionId=" + arg0.getExecutionId());
			logger.debug("taskDefinitionKey="
					+ arg0.getTaskDefinitionKey());
		}

		// 判断是否执行更新方法
		String execute = isExecuteUpdateMethod.getExpressionText();
		if (execute == null)
			return;
		if (execute.indexOf("$") == -1)
			return;

		execute = execute.substring(execute.indexOf("{") + 1, execute.indexOf("}"));
		boolean go = "true".equals(execute);// isExecuteUpdateMethod 设置为：${true}，就执行更新
		if(!go){// 设置为其他变量，变量的值必须为true才执行更新
			Object isExecute = arg0.getVariable(execute);
			if (isExecute != null && "true".equalsIgnoreCase(isExecute.toString())) {
				go = true;
			}
		}

		if(go){
			executeUpdateMethod(arg0);
			if(log!=null&& arg0.hasVariable(log.getExpressionText())) {
				saveExcutionLog(arg0,arg0.getVariable(log.getExpressionText()).toString());
			}
		}
	}

	/**
	 * 更新方法的实现
	 * 
	 * @param execution
	 */
	private void executeUpdateMethod(DelegateTask execution) {
		Map<String, Object> arguments = JsonUtils.toMap(parameter
				.getExpressionText());
		Map<String, Object> args = new HashMap<String, Object>();
		Set<String> keySet = arguments.keySet();
		for (String key : keySet) {

			Object arg = arguments.get(key);
			if (arg instanceof String) {
				String value = arg.toString();
				// 如果包含"$"号就取变量值
				if (value.indexOf("$") != -1) {
					args.put(key, execution.getVariable(value.substring(
							value.indexOf("{") + 1, value.indexOf("}"))));
				} else {
					args.put(key, arguments.get(key));
				}

			} else {
				args.put(key, arguments.get(key));
				logger.debug(arguments.get(key) + " :arguments.get(key): "
						+ arguments.get(key).getClass());
			}
		}
		// 获取id
		String ObjectId = updateObjectId.getExpressionText();
		if (ObjectId.indexOf("$") != -1) {

			Object ModuleId = execution.getVariable(ObjectId.substring(
					ObjectId.indexOf("{") + 1, ObjectId.indexOf("}")));
			SpringUtils.invokeBeanMethod(serviceName.getExpressionText(),
					serviceMethod.getExpressionText(),
					new Object[] { Long.valueOf(ModuleId.toString()), args });
		}
	}
	
	private void saveExcutionLog(DelegateTask delegateTask,String desc){
		// 创建同步日志
		ExcutionLog log = new ExcutionLog();
		log.setFileDate(Calendar.getInstance());
		ActorHistory h = SystemContextHolder.get().getUserHistory();
		log.setAuthorId(h.getId());
		log.setAuthorCode(h.getCode());
		log.setAuthorName(h.getName());
		log.setAssigneeId(h.getId());
		log.setAssigneeCode(h.getCode());
		log.setAssigneeName(h.getName());

		log.setListener(delegateTask.getClass().getName());
		log.setExcutionId(delegateTask.getExecutionId());
		log.setType(ExcutionLog.TYPE_PROCESS_SYNC_INFO);
		log.setProcessInstanceId(delegateTask.getProcessInstanceId());

		// 任务的ID、编码、名称
		log.setTaskInstanceId(delegateTask.getId());
		log.setExcutionCode(delegateTask.getTaskDefinitionKey());
		log.setExcutionName(delegateTask.getName());

		log.setDescription(desc);
		this.excutionLogService.save(log);
	}

}
