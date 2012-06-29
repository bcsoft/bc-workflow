package cn.bc.workflow.web.struts2;

import org.activiti.engine.repository.Deployment;
import org.commontemplate.util.Assert;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.web.ui.json.Json;

/**
 * 流程处理Action
 * 
 * @author dragon
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class WorkflowAction extends AbstractBaseAction {
	private static final long serialVersionUID = 1L;
	public String toUser;
	public String type;
	public boolean cascade;

	/**
	 * 发布流程(xml、zip或bar包)
	 * 
	 * @return
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
			} else if ("zip".equalsIgnoreCase(type)
					|| "bar".equalsIgnoreCase(type)) {// zip或bar模板
				deploy = this.workflowService.deployZipFromTemplate(key);
			} else {
				throw new CoreException("不支持的发布类型：type=" + type + ",key=" + key);
			}

			// 返回信息
			Json json = createSuceessMsg("流程发布成功！");
			Json d = new Json();
			d.put("id", deploy.getId());
			d.put("name", deploy.getName());
			d.put("deploymentTime",
					DateUtils.formatDateTime(deploy.getDeploymentTime()));
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
	 * @return
	 * @throws Exception
	 */
	public String deleteDeployment() throws Exception {
		try {
			// id为发布历史的id
			Assert.assertNotEmpty(id);

			// 删除发布的流程
			this.workflowService.deleteDeployment(id, cascade);

			// 返回信息
			this.json = createSuceessMsg("流程发布历史删除成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 发起流程
	 * 
	 * @return
	 * @throws Exception
	 */
	public String startFlow() throws Exception {
		try {
			// key为流程的编码
			Assert.assertNotEmpty(key);

			// 完成任务
			String processInstanceId = this.workflowService.startFlowByKey(key);

			// 返回信息
			Json json = createSuceessMsg("启动成功！");
			json.put("processInstance", processInstanceId);// 流程实例id
			this.json = json.toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 领取任务
	 * 
	 * @return
	 * @throws Exception
	 */
	public String claimTask() {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);

			// 领取任务
			this.workflowService.claimTask(id);

			// 返回信息
			json = createSuceessMsg("任务领取成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 完成任务
	 * 
	 * @return
	 * @throws Exception
	 */
	public String completeTask() throws Exception {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);

			// 完成任务
			this.workflowService.completeTask(id);

			// 返回信息
			json = createSuceessMsg("完成任务成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 委派任务
	 * 
	 * @return
	 * @throws Exception
	 */
	public String delegateTask() throws Exception {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);

			// 委派给的用户
			Assert.assertNotEmpty(toUser);

			// 委派任务
			this.workflowService.delegateTask(id, toUser);

			// 返回信息
			json = createSuceessMsg("委派任务成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}
}