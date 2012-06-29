package cn.bc.workflow.web.struts2;

import org.commontemplate.util.Assert;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

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

		return SUCCESS;
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

		return SUCCESS;
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
			this.taskService.complete(id);

			// 返回信息
			json = createSuceessMsg("完成任务成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return SUCCESS;
	}
}