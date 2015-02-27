package cn.bc.workflow.activiti.delegate;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;

/**
 * <p>判断并发任务中同时为真或者同时为假的情况</p>
 * 获取本地变量，与全局变量进行运算，运算结果作为全局变量的结果。<br/>
 * 如果全局变量不存在则将本地变量设置为全局变量
 * 
 * @author Action
 *
 */
public class ResultAndOrOperationListener implements TaskListener {
	/**
	 * 本地变量名  [必要]<br/>
	 * 注：变量名必须以"_lc"结尾，必须可以转换为Boolean型。
	 */
	private Expression varNameLocal;

	/**
	 * 运算的名称：有 or和and [必要]
	 */
	private Expression operation;

	public void notify(DelegateTask delegateTask) {
		if (varNameLocal == null || operation == null)
			return;

		String varName_lc = varNameLocal.getExpressionText();					// 本地变量名
		String varName = varName_lc.substring(0, varName_lc.indexOf("_lc"));	// 全局变量名

		Boolean varLc = (Boolean) delegateTask.getVariableLocal(varName_lc);	// 本地变量
		Object varObj = delegateTask.getVariable(varName);						// 全局变量Object类型

		if (varObj == null) {
			delegateTask.setVariable(varName, varLc);							// 设置全局变量的值为本地变量的值
			return;
		}

		Boolean var = (Boolean) varObj;
		if ("or".equals(operation.getExpressionText())) {
			delegateTask.setVariable(varName, varLc | var);						// 或运算
		} else if ("and".equals(operation.getExpressionText())) {
			delegateTask.setVariable(varName, varLc & var);						// 与运算
		}
	}

}
