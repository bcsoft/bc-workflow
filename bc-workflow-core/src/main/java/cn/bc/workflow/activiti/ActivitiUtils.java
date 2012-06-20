/**
 * 
 */
package cn.bc.workflow.activiti;

import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
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

	public static String toString(ProcessInstance task) {
		JSONObject json = new JSONObject();
		try {
			json.put("id", task.getId());
			json.put("processDefinitionId", task.getProcessDefinitionId());
			json.put("processInstanceId", task.getProcessInstanceId());
			json.put("businessKey", task.getBusinessKey());
			json.put("isSuspended", task.isSuspended());
			json.put("isEnded", task.isEnded());
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
}
