/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.core.util.DateUtils;
import cn.bc.core.util.FreeMarkerUtils;

/**
 * 初始化流程实例的subject流程变量值的监听器
 * <p>
 * 只能配置在流程的启动事件中
 * </p>
 * 
 * @author dragon
 * 
 */
public class InitProcessSubjectListener implements ExecutionListener {
	private static final Log logger = LogFactory
			.getLog(InitProcessSubjectListener.class);

	/**
	 * 标题表达式:支持的参数见getDefaultParams方法
	 */
	private Expression subject;

	public void notify(DelegateExecution execution) throws Exception {
		if (execution.hasVariable("subject"))
			return;

		Calendar now = Calendar.getInstance();
		if (subject == null) {
			if (execution instanceof ExecutionEntity) {
				execution.setVariable(
						"subject",
						((ExecutionEntity) execution).getProcessDefinition()
								.getName()
								+ "("
								+ DateUtils.formatCalendar2Minute(now) + ")");
			} else {
				execution.setVariable("subject",
						DateUtils.formatCalendar2Minute(now));
			}
			if (logger.isDebugEnabled())
				logger.debug("subject=" + execution.getVariable("subject"));
		} else {
			if (logger.isDebugEnabled())
				logger.debug("subject0=" + subject.getExpressionText());

			// 用freemarker格式化标题
			String _subject = FreeMarkerUtils.format(
					subject.getExpressionText(),
					getDefaultParams(execution.getVariablesLocal()));
			execution.setVariable("subject", _subject);
			if (logger.isDebugEnabled())
				logger.debug("subject1=" + _subject);
		}
	}

	protected Map<String, Object> getDefaultParams(Map<String, Object> variables) {
		Map<String, Object> params = new LinkedHashMap<String, Object>();

		// 当前时间
		Calendar now = Calendar.getInstance();
		params.put("now", DateUtils.formatCalendar2Second(now));
		params.put("now2d", DateUtils.formatCalendar2Day(now));
		params.put("now2m", DateUtils.formatCalendar2Minute(now));
		params.put("yearOfNow", now.get(Calendar.YEAR) + "");
		params.put("monthOfNow", now.get(Calendar.MONTH) + 1);
		params.put("dayOfNow", now.get(Calendar.DAY_OF_MONTH));

		// 下个月
		now.add(Calendar.MONTH, 1);
		params.put("nextMonth", now.get(Calendar.MONTH) + 1);
		params.put("yearOfNextMonth", now.get(Calendar.YEAR) + "");

		// 添加流程变量
		params.putAll(variables);

		return params;
	}
}
