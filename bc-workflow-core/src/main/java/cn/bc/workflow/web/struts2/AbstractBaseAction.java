package cn.bc.workflow.web.struts2;

import java.util.Map;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.apache.struts2.interceptor.RequestAware;
import org.apache.struts2.interceptor.SessionAware;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.identity.web.SystemContext;
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.service.WorkflowService;

import com.opensymphony.xwork2.ActionSupport;

/**
 * 流程处理Action的基类
 * 
 * @author dragon
 * 
 */
public abstract class AbstractBaseAction extends ActionSupport implements
		SessionAware, RequestAware {
	private static final long serialVersionUID = 1L;
	public static final String JSON = "json";
	protected Map<String, Object> session;
	protected Map<String, Object> request;

	protected WorkflowService workflowService;
	protected RuntimeService runtimeService;
	protected FormService formService;
	protected IdentityService identityService;
	protected TaskService taskService;
	protected HistoryService historyService;

	public String json;// json
	public String id;// 任务、流程实例等的id，视具体情况而定
	public String key;// 任务、流程的编码，视具体情况而定
	public String ver;// 任务、流程的版本号，视具体情况而定

	@Autowired
	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	@Autowired
	public void setRuntimeService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	@Autowired
	public void setFormService(FormService formService) {
		this.formService = formService;
	}

	@Autowired
	public void setIdentityService(IdentityService identityService) {
		this.identityService = identityService;
	}

	@Autowired
	public void setTaskService(TaskService taskService) {
		this.taskService = taskService;
	}

	@Autowired
	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}

	public void setSession(Map<String, Object> session) {
		this.session = session;
	}

	public void setRequest(Map<String, Object> request) {
		this.request = request;
	}

	/**
	 * 获取当前的系统上下文
	 * 
	 * @return
	 */
	public SystemContext getContext() {
		return (SystemContext) this.session.get(SystemContext.KEY);
	}

	/**
	 * 获取当前用户的帐号
	 * 
	 * @return
	 */
	public String getCurrentUserAccount() {
		return getContext().getUser().getCode();
	}

	/**
	 * 判断是否有流程管理角色
	 * 
	 * @return
	 */
	public boolean isFlowManager() {
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.admin"),
				getText("key.role.bc.workflow"));
	}

	/**
	 * 创建处理成功的json信息
	 * 
	 * @param msg
	 *            信息
	 * @return
	 */
	protected Json createSuccessMsg(String msg) {
		Json json = new Json();
		json.put("success", true);
		json.put("msg", msg);
		return json;
	}

	/**
	 * 创建处理失败的json信息
	 * 
	 * @param msg
	 *            信息
	 * @return
	 */
	protected Json createFailureMsg(String msg) {
		Json json = new Json();
		json.put("success", false);
		json.put("msg", msg);
		return json;
	}

	/**
	 * 创建处理失败的json信息
	 * 
	 * @param e
	 * @return
	 */
	protected Json createFailureMsg(Exception e) {
		Json json = new Json();
		json.put("success", false);
		json.put("msg", e.getMessage());
		return json;
	}
}