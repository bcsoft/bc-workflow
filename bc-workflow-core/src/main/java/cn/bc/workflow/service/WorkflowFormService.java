package cn.bc.workflow.service;

import java.util.List;
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
	 * @param task 任务
	 * @param addParams 附加的渲染参数
	 * @return
	 */
	String getRenderedTaskForm(Map<String, Object> task, Map<String, Object> addParams);

	/**
	 * 渲染任务表单
	 * 
	 * @param task 任务
	 * @param readonly 是否以只读方式渲染
	 * @return
	 */
	String getRenderedTaskForm(Map<String, Object> task, boolean readonly);
}
