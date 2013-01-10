/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

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

/**
 * 流程更新模块的相关信息监听器
 * 
 * @author zxr
 * 
 */
public class updateModuleInfo4TaskListener implements TaskListener {
	protected final Log logger = LogFactory.getLog(getClass());

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
	 * 更新对象的id
	 */
	private Expression updateObjectId;

	public void notify(DelegateTask arg0) {
		// 组装参数
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
					args.put(key, arg0.getVariable(value.substring(
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
		String caseId = updateObjectId.getExpressionText();
		if (caseId.indexOf("$") != -1) {

			Object case4InfractTrafficId = arg0.getVariable(caseId.substring(
					caseId.indexOf("{") + 1, caseId.indexOf("}")));
			SpringUtils.invokeBeanMethod(
					serviceName.getExpressionText(),
					serviceMethod.getExpressionText(),
					new Object[] {
							Long.valueOf(case4InfractTrafficId.toString()),
							args });
		}
	}

}
