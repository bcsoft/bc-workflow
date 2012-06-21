/**
 * 
 */
package cn.bc.workflow.activiti;

import java.util.List;

import org.activiti.engine.form.FormProperty;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 自定义的Activiti用户组管理器
 * 
 * @author dragon
 * 
 */
public final class ActivitiUtils {
	public static String toString(Task task) {
		if (task == null)
			return null;
		JSONObject json = new JSONObject();
		try {
			json.put("id", task.getId());
			json.put("name", task.getName());
			json.put("description", task.getDescription());
			json.put("priority", task.getPriority());
			json.put("owner", task.getOwner());
			json.put("assignee", task.getAssignee());
			json.put("processDefinitionId", task.getProcessDefinitionId());
			json.put("processInstanceId", task.getProcessInstanceId());
			json.put("executionId", task.getExecutionId());
			json.put("taskDefinitionKey", task.getTaskDefinitionKey());
			json.put("parentTaskId", task.getParentTaskId());
			json.put("dueDate", task.getDueDate());
			json.put("createTime", task.getCreateTime());
			json.put("delegationState", task.getDelegationState());
			json.put("class", task.getClass());
		} catch (JSONException e) {
			try {
				json.put("_error_", e.getMessage());
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
		return json.toString();
	}

	public static String toString(ProcessInstance pi) {
		if (pi == null)
			return null;
		JSONObject json = new JSONObject();
		try {
			json.put("id", pi.getId());
			json.put("processDefinitionId", pi.getProcessDefinitionId());
			json.put("processInstanceId", pi.getProcessInstanceId());
			json.put("businessKey", pi.getBusinessKey());
			json.put("isSuspended", pi.isSuspended());
			json.put("isEnded", pi.isEnded());
			json.put("class", pi.getClass());
		} catch (JSONException e) {
			try {
				json.put("_error_", e.getMessage());
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
		return json.toString();
	}

	public static String toString(List<FormProperty> ps) {
		if (ps == null)
			return null;
		JSONArray jsons = new JSONArray();
		JSONObject json = new JSONObject();
		for (FormProperty p : ps) {
			json = new JSONObject();
			jsons.put(json);
			try {
				json.put("id", p.getId());
				json.put("name", p.getName());
				json.put("type", p.getType());
				json.put("value", p.getValue());
				json.put("readable", p.isReadable());
				json.put("required", p.isRequired());
				json.put("writable", p.isWritable());
				json.put("class", p.getClass());
			} catch (JSONException e) {
				try {
					json.put("_error_", e.getMessage());
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			}
		}
		return jsons.toString();
	}
}