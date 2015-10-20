package cn.bc.workflow.web.struts2;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.JsonUtils;
import cn.bc.core.util.StringUtils;
import cn.bc.docs.web.AttachUtils;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.web.ui.json.Json;
import cn.bc.web.util.WebUtils;
import cn.bc.workflow.flowattach.service.FlowAttachService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.task.Task;
import org.apache.struts2.ServletActionContext;
import org.commontemplate.util.Assert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.io.InputStream;
import java.util.*;

/**
 * 流程处理Action
 *
 * @author dragon
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class WorkflowAction extends AbstractBaseAction {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(WorkflowAction.class);
	public String toUser;
	public String type;
	public boolean cascade;

	public String filename;// 下载文件的文件名
	public String contentType;// 下载文件的大小
	public long contentLength;
	public InputStream inputStream;
	public String n;// [可选]指定下载文件的文件名

	/**
	 * 任务的表单数据，使用标准的Json数据格式：[{name:"",value:"",type:"int|long|string|date|...",
	 * scope:"process|task"}]
	 */
	public String formData;

	private FlowAttachService flowAttachService;

	@Autowired
	public void setFlowAttachService(FlowAttachService flowAttachService) {
		this.flowAttachService = flowAttachService;
	}

	/**
	 * 查看流程图
	 *
	 * @throws Exception
	 */
	public String diagram() throws Exception {
		Date startTime = new Date();

		// 下载文件的扩展名
		String extension = "png";

		// 下载文件的文件名
		if (this.n == null || this.n.length() == 0)
			this.n = "instance" + id + "." + extension;

		// debug
		logger.debug("n={}, extension={}", n, extension);

		// 获取资源流
		this.inputStream = workflowService.getInstanceDiagram(this.id);
		if (logger.isDebugEnabled())
			logger.debug("inputStream={}", this.inputStream.getClass());
		this.contentLength = this.inputStream.available();// 资源大小

		// 设置下载文件的参数
		this.contentType = AttachUtils.getContentType(extension);
		this.filename = WebUtils.encodeFileName(ServletActionContext.getRequest(), this.n);

		if (logger.isDebugEnabled()) {
			logger.debug("wasteTime:{}", DateUtils.getWasteTime(startTime));
		}
		return SUCCESS;
	}

	/**
	 * 发布流程(xml、zip或bar包)
	 *
	 * @throws Exception
	 */
	public String deploy() throws Exception {
		try {
			// key为模板的编码
			Assert.assertNotEmpty(key);

			// 发布流程
			Deployment deploy;
			if ("xml".equalsIgnoreCase(type)) {// xml模板
				deploy = this.workflowService.deployXmlFromTemplate(key);
			} else if ("zip".equalsIgnoreCase(type) || "bar".equalsIgnoreCase(type)) {// zip或bar模板
				deploy = this.workflowService.deployZipFromTemplate(key);
			} else {
				throw new CoreException("不支持的发布类型：type=" + type + ",key=" + key);
			}

			// 返回信息
			Json json = createSuccessMsg("流程发布成功！");
			Json d = new Json();
			d.put("id", deploy.getId());
			d.put("name", deploy.getName());
			d.put("deploymentTime", DateUtils.formatDateTime(deploy.getDeploymentTime()));
			json.put("deploy", d);// 发布对象
			this.json = json.toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 删除指定的发布历史
	 *
	 * @throws Exception
	 */
	public String deleteDeployment() throws Exception {
		try {
			// id为发布历史的id
			Assert.assertNotEmpty(id);

			// 删除发布的流程
			this.workflowService.deleteDeployment(id, cascade);

			// 返回信息
			this.json = createSuccessMsg("流程发布历史删除成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 发起流程
	 *
	 * @throws Exception
	 */
	public String startFlow() throws Exception {
		try {
			String processInstanceId;

			if (null != key && key.length() > 0) {
				//启动流程
				processInstanceId = this.workflowService.startFlowByKey(key);
			} else {
				// id为流程实例id
				Assert.assertNotEmpty(id);
				//启动流程
				processInstanceId = this.workflowService.startFlowByDefinitionId(id);
			}

			// 返回信息
			Json json = createSuccessMsg("启动成功！");
			json.put("processInstance", processInstanceId);// 流程实例id
			this.json = json.toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 领取任务
	 */
	public String claimTask() {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);

			// 领取任务
			this.workflowService.claimTask(id);

			Task task = taskService.createTaskQuery().taskId(id).singleResult();
			// 返回信息
			Json json = createSuccessMsg("任务领取成功！");
			json.put("pId", task.getProcessInstanceId()); // 流程实例id
			json.put("name", task.getName());
			this.json = json.toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 完成任务
	 *
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public String completeTask() throws Exception {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);

			// 完成任务
			Object[] variables = buildFormVariables();
			this.workflowService.completeTask(id, (Map<String, Object>) variables[0], (Map<String, Object>) variables[1]);

			// 返回信息
			json = createSuccessMsg("完成任务成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 发起子流程
	 * <p>必要变量：
	 * <ul><li><b>id</b> 当前任务的id</li>
	 * <li><b>key</b> 流程编码</li>
	 * <li><b>formData</b> 完成子流程第一个任务的表单内容</li></ul></p>
	 * <p>发起成功后，将子流程实例Id设置为该任务的本地变量<b>subProcessInstanceId_lc</b></p>
	 *
	 * @return json {"subProcessInstanceId", 子流程实例Id}
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public String startSubProcess() throws Exception {
		// id为当前任务的id
		Assert.assertNotEmpty(id);
		// key为流程编码
		Assert.assertNotEmpty(key);
		// 子流程的表单数据
		Assert.assertNotEmpty(formData);

		try {
			// 流程变量
			Object[] variables = buildFormVariables();
			Map<String, Object> globalVariables = (Map<String, Object>) variables[0];
			Map<String, Object> localVariables = (Map<String, Object>) variables[1];
			// 附件Id
			Long[] attachIds = null;
			long[] _attachIds = (long[]) localVariables.get("attachIds");
			for (int i = 0; (_attachIds != null && _attachIds.length > 0) && i < _attachIds.length; i++) {
				if (attachIds == null) attachIds = new Long[_attachIds.length];
				attachIds[i] = _attachIds[i];
			}

			if (null == globalVariables.get("mainProcessInstanceId"))
				throw new Exception("mainProcessInstanceId 全局变量不能为空，该变量要作为子流程的主流程Id");

			// 发起流程，获得流程实例id
			String processInstanceId = this.workflowService.startFlowByKey(key, globalVariables);

			// 获得待办任务Id数组
			String[] arrTaskIds = this.workflowService.findTaskIdByProcessInstanceId(processInstanceId);

			if (arrTaskIds == null)
				throw new Exception("待办任务为空！");
			if (arrTaskIds.length > 1)
				throw new Exception("待办任务不能有多个！");

			// 将主流程的附件更新为主流程的附件
			if (attachIds != null) {
				this.flowAttachService.updateAttachToSubProcess(attachIds, processInstanceId, arrTaskIds[0]);
			}

			// 完成任务
			this.workflowService.completeTask(arrTaskIds[0], null, localVariables);

			// 设置本地变量：在当前任务中，子流程的流程实例id
			this.taskService.setVariableLocal(id, "subProcessInstanceId", processInstanceId);
			this.taskService.setVariableLocal(id, "mainProcessAssignedActorNames", globalVariables.get("mainProcessAssignedActorNames"));
			this.taskService.setVariableLocal(id, "mainProcessAssignedActorCodes", globalVariables.get("mainProcessAssignedActorCodes"));

			// 返回信息
			Json json = createSuccessMsg("子流程启动成功！");
			json.put("subProcessInstanceId", processInstanceId);// 流程实例id
			this.json = json.toString();
		} catch (Exception e) {
			Json json = createFailureMsg(createFailureMsg(e).toString());
			this.json = json.toString();
		}

		return JSON;
	}

	public String globalKeys;//多个逗号链接

	public String findGlobalValues() throws Exception {
		try {
			// id为流程的id
			Assert.assertNotEmpty(id);

			if (globalKeys == null || globalKeys.length() == 0) {
				throw new CoreException("globalKeys is null");
			}

			String[] _globalKeys = this.globalKeys.split(",");

			Map<String, Object> globalValues = this.workflowService.findGlobalValue(id, _globalKeys);

			if (globalValues == null || globalValues.size() == 0) return JSON;

			Json o = new Json();

			for (String key : _globalKeys) {
				//sql返回的key值都为小写
				if (globalValues.containsKey(key.toLowerCase())) {
					o.put(key, globalValues.get(key.toLowerCase()));
				}
			}

			// 返回信息
			json = o.toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 根据表单数据构建相应的流程变量
	 */
	private Object[] buildFormVariables() {
		if (this.formData == null || this.formData.length() == 0)
			return new Object[2];

		Collection<Map<String, Object>> data = JsonUtils.toCollection(this.formData);
		Object[] variables = new Object[2];
		Map<String, Object> globalVariables = new LinkedHashMap<>();
		Map<String, Object> localVariables = new LinkedHashMap<>();
		variables[0] = globalVariables;
		variables[1] = localVariables;
		for (Map<String, Object> m : data) {
			if ("global".equals(m.get("scope"))) {// 全局
				globalVariables.put((String) m.get("name"), StringUtils.convertValueByType((String) m.get("type"), (String) m.get("value")));
			} else if ("local".equals(m.get("scope"))) {// 本地
				localVariables.put((String) m.get("name"), StringUtils.convertValueByType((String) m.get("type"), (String) m.get("value")));
			} else {
				throw new CoreException("unsupport formData scope:" + m.get("scope"));
			}
		}
		return variables;
	}

	/**
	 * 委托任务
	 *
	 * @throws Exception
	 */
	public String delegateTask() throws Exception {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);

			// 委托给的用户
			Assert.assertNotEmpty(toUser);

			// 委托任务
			ActorHistory ah = this.workflowService.delegateTask(id, toUser);

			// 返回信息
			json = createSuccessMsg("已成功将任务委托给" + ah.getName()).toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 分派任务
	 *
	 * @throws Exception
	 */
	public String assignTask() throws Exception {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);

			// 分派给的用户
			Assert.assertNotEmpty(toUser);

			// 分派任务
			ActorHistory ah = this.workflowService.assignTask(id, toUser);

			// 返回信息
			json = createSuccessMsg("已成功将任务分派给" + ah.getName()).toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 获得主流程经办信息
	 */
	public String findMainProcessInstanceInfo() {
		// id为流程实例id
		Assert.assertNotEmpty(id);

		Map<String, Object> info = this.workflowService.findMainProcessInstanceInfoById(id);

		JSONObject json = new JSONObject(info);
		this.json = json.toString();

		return JSON;
	}

	/**
	 * 通过流程实例Id获得子流程经办信息
	 */
	public String findSubProcessInstanceInfo() {
		// id为主流程实例id
		Assert.assertNotEmpty(id);

		List<Map<String, Object>> info = this.workflowService.findSubProcessInstanceInfoById(id);

		JSONArray jsonArray = new JSONArray(info);
		this.json = jsonArray.toString();

		return JSON;
	}
}