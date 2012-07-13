package cn.bc.workflow.web.struts2;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

import org.activiti.engine.repository.Deployment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.commontemplate.util.Assert;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.docs.web.AttachUtils;
import cn.bc.web.ui.json.Json;
import cn.bc.web.util.WebUtils;

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
	private static Log logger = LogFactory.getLog(WorkflowAction.class);
	public String toUser;
	public String type;
	public boolean cascade;

	public String filename;// 下载文件的文件名
	public String contentType;// 下载文件的大小
	public long contentLength;
	public InputStream inputStream;
	public String n;// [可选]指定下载文件的文件名

	/**
	 * 查看流程图
	 * 
	 * @return
	 * @throws Exception
	 */
	public String diagram() throws Exception {
		Date startTime = new Date();

		// 下载文件的扩展名
		String extension = "png";

		// 下载文件的文件名
		if (this.n == null || this.n.length() == 0)
			this.n = "instance" + id + "," + extension;

		// debug
		if (logger.isDebugEnabled()) {
			logger.debug("n=" + n);
			logger.debug("extension=" + extension);
		}

		// 获取资源流
		this.inputStream = workflowService.getInstanceDiagram(this.id);
		if (logger.isDebugEnabled())
			logger.debug("inputStream=" + this.inputStream.getClass());
		this.contentLength = this.inputStream.available();// 资源大小

		// 设置下载文件的参数
		this.contentType = AttachUtils.getContentType(extension);
		this.filename = WebUtils.encodeFileName(
				ServletActionContext.getRequest(), this.n);

		if (logger.isDebugEnabled()) {
			logger.debug("wasteTime:" + DateUtils.getWasteTime(startTime));
		}
		return SUCCESS;
	}

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
			Json json = createSuccessMsg("流程发布成功！");
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
			this.json = createSuccessMsg("流程发布历史删除成功！").toString();
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
			json = createSuccessMsg("任务领取成功！").toString();
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
			json = createSuccessMsg("完成任务成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}

	/**
	 * 委托任务
	 * 
	 * @return
	 * @throws Exception
	 */
	public String delegateTask() throws Exception {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);

			// 委托给的用户
			Assert.assertNotEmpty(toUser);

			// 委托任务
			this.workflowService.delegateTask(id, toUser);

			// 返回信息
			json = createSuccessMsg("委托任务成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}

		return JSON;
	}
	
	/**
	 * 分派任务
	 * 
	 * @return
	 * @throws Exception
	 */
	public String assignTask() throws Exception {
		try {
			// id为任务的id
			Assert.assertNotEmpty(id);
			
			// 分派给的用户
			Assert.assertNotEmpty(toUser);
			
			// 分派任务
			this.workflowService.assignTask(id, toUser);
			
			// 返回信息
			json = createSuccessMsg("分派任务成功！").toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}
		
		return JSON;
	}
}