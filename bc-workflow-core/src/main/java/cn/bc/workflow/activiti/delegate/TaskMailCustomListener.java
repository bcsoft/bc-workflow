/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.core.exception.CoreException;
import cn.bc.mail.Mail;
import cn.bc.template.util.FreeMarkerUtils;

/**
 * 发送任务邮件提醒的的监听器：create创建、assignment分配、complete完成
 * <p>
 * 可自定义邮件的标题和内容的详细格式（使用freemark格式配置），可附加收件人、抄送人、密送人
 * </p>
 * 
 * @author dragon
 * 
 */
public class TaskMailCustomListener extends TaskMailListener {
	private static final Log logger = LogFactory
			.getLog(TaskMailCustomListener.class);
	private static String DEFAULT_CONTENT;

	static {
		DEFAULT_CONTENT = addBr("任务名称：${ti_subject}");
		DEFAULT_CONTENT += addBr("所属业务：${pi_subject}");
		DEFAULT_CONTENT += addBr("所属流程：${pd_name}");
		DEFAULT_CONTENT += addBr("创建时间：${ti_startTime?string(\"yyyy-MM-dd HH:mm\"}");
		DEFAULT_CONTENT += addBr("办理期限：${ti_dueDate?string(\"yyyy-MM-dd HH:mm\"}");
		DEFAULT_CONTENT += addBr("附加说明：${ti_description}");
	}
	private Expression subject; // 定义邮件的标题格式
	private Expression content; // 定义邮件的内容格式
	private Expression override; // 用于决定是否使用to属性的收件人覆盖任务的办理人的邮箱还是增加to指定的邮箱，默认为附加(false)
	private Expression to; // 附加的邮件接收人，多个邮箱间用分号连接
	private Expression cc; // 附加的邮件抄送人，多个邮箱间用分号连接
	private Expression bcc; // 附加的邮件密送人，多个邮箱间用分号连接

	public TaskMailCustomListener() {
		super();
	}

	public void notify(DelegateTask delegateTask) {
		if (logger.isDebugEnabled()) {
			logger.debug("execution=" + delegateTask.getClass());
			logger.debug("this=" + this.getClass());
			logger.debug("id=" + delegateTask.getId());
			logger.debug("eventName=" + delegateTask.getEventName());
			logger.debug("processInstanceId"
					+ delegateTask.getProcessInstanceId());
			logger.debug("executionId=" + delegateTask.getExecutionId());
			logger.debug("taskDefinitionKey="
					+ delegateTask.getTaskDefinitionKey());
		}

		// 创建邮件
		Mail mail = new Mail();
		mail.setHtml(true);// html邮件

		// 格式化参数
		Map<String, Object> params = new HashMap<String, Object>();
		params.putAll(delegateTask.getVariables());// 全局变量
		params.putAll(delegateTask.getVariablesLocal());// 本地任务变量

		// 任务的参数
		TaskEntity task = (TaskEntity) delegateTask;
		params.put("ti_id", task.getId());
		params.put("ti_key", task.getTaskDefinitionKey());
		params.put("ti_startTime", task.getCreateTime());
		params.put("ti_description", task.getDescription());
		params.put("ti_assignee", task.getAssignee());
		params.put("ti_owner", task.getOwner());
		params.put("ti_name", task.getName());
		params.put("ti_priority", task.getPriority());
		params.put("ti_dueDate", task.getDueDate());
		String taskSubject = (String) delegateTask.getVariableLocal("subject");
		if (taskSubject == null) {
			taskSubject = delegateTask.getName();
		}
		params.put("ti_subject", taskSubject);

		// 流程实例的一些参数
		ProcessInstance pi = task.getProcessInstance();
		params.put("pi_id", pi.getId());
		params.put("pi_businessKey", pi.getBusinessKey());
		params.put("pi_definitionId", pi.getProcessDefinitionId());

		// 流程定义的参数
		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(pi.getProcessDefinitionId())
				.singleResult();
		params.put("pd_id", pd.getId());
		params.put("pd_category", pd.getCategory());
		params.put("pd_deploymentId", pd.getDeploymentId());
		params.put("pd_key", pd.getKey());
		params.put("pd_name", pd.getName());

		// 所属业务
		String piSubject = (String) delegateTask.getVariable("subject");
		if (piSubject == null) {
			piSubject = pd.getName();// 流程的名称
		}
		params.put("pi_subject", piSubject);

		String eventType = delegateTask.getEventName();
		if (eventType.equals("create")) {
			// 判断是否是岗位待办
			boolean isGroupTask = (delegateTask.getAssignee() == null || delegateTask
					.getAssignee().isEmpty());

			// 邮件标题
			String originSubject;
			if (subject != null && subject.getExpressionText().length() > 0) {
				originSubject = FreeMarkerUtils.format(
						subject.getExpressionText(), params);
			} else {
				originSubject = taskSubject;
			}

			// 邮件接收人：岗位任务时发送到岗位中的所有人
			String[] mailAddresses;
			if (isGroupTask) {
				List<IdentityLinkEntity> identityLinks = task
						.getIdentityLinks();
				if (identityLinks == null || identityLinks.isEmpty()) {
					throw new CoreException(
							"can't find membership from table act_ru_identitylink: taskId="
									+ delegateTask.getId());
				}
				List<String> groups = new ArrayList<String>();
				for (IdentityLink l : identityLinks) {
					if (l.getGroupId() != null && !l.getGroupId().isEmpty()) {
						groups.add(l.getGroupId());
					}
				}
				mailAddresses = actorService.findMailAddressByGroup(groups);

				// 邮件标题
				mail.setSubject("BC岗位待办提醒：" + originSubject);
			} else {
				// 邮件标题
				mail.setSubject("BC个人待办提醒：" + originSubject);

				String[] userCodes = new String[] { delegateTask.getAssignee() };
				mailAddresses = actorService.findMailAddressByUser(userCodes);
			}
			mail.setTo(mailAddresses);
		} else {
			throw new CoreException("unsupport send mail for " + eventType
					+ " task");
		}

		// 邮件内容
		String c;
		if (content != null && content.getExpressionText().length() > 0) {
			c = FreeMarkerUtils.format(content.getExpressionText(), params);
		} else {
			c = FreeMarkerUtils.format(DEFAULT_CONTENT, params);
		}
		c += addParagraph(
				"此邮件由BC系统自动生成，请勿回复此邮件【邮件编号：PI" + task.getProcessInstanceId()
						+ "TI" + task.getId() + "】",
				"color:gray;font-size:80%;");
		mail.setContent(c);

		// 附加额外的主送人
		if (to != null && to.getExpressionText().length() > 0) {
			boolean replace = false;
			if (override != null) {
				replace = "true".equalsIgnoreCase(override.getExpressionText());
			}
			if (replace) {
				mail.setTo(to.getExpressionText().split(";"));
			} else {
				mail.addTo(to.getExpressionText().split(";"));
			}
		}

		// 附加额外的抄送人
		if (cc != null && cc.getExpressionText().length() > 0) {
			mail.setCc(cc.getExpressionText().split(";"));
		}

		// 附加额外的密送人
		if (bcc != null && bcc.getExpressionText().length() > 0) {
			mail.setBcc(bcc.getExpressionText().split(";"));
		}

		// 发送邮件
		if (mail.getTo() != null && mail.getTo().length > 0)
			mailService.send(mail);
	}
}
