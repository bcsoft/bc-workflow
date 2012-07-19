package cn.bc.workflow.service;

import java.util.Map;

/**
 * 表单渲染Service接口
 * 
 * @author dragon
 * 
 */
public interface WorkflowFormService {
	/**
	 * 渲染任务表单
	 * 
	 * @param taskId
	 *            任务ID
	 * @param addParams
	 *            附加的渲染参数
	 * @return
	 */
	String getRenderedTaskForm(String taskId, Map<String, Object> addParams);

	/**
	 * 渲染任务表单
	 * 
	 * @param taskId
	 *            任务ID
	 * @param readonly
	 *            是否以只读方式渲染
	 * @return
	 */
	String getRenderedTaskForm(String taskId, boolean readonly);
}
