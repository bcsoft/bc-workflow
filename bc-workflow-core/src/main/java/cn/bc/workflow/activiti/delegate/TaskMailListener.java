/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.task.IdentityLink;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.SpringUtils;
import cn.bc.identity.service.ActorService;
import cn.bc.mail.Mail;
import cn.bc.mail.MailService;

/**
 * 发送任务邮件提醒的的监听器：create创建、assignment分配、complete完成
 * 
 * @author dragon
 * 
 */
public class TaskMailListener implements TaskListener {
	private static final Log logger = LogFactory.getLog(TaskMailListener.class);
	protected ActorService actorService;
	protected MailService mailService;
	protected TaskService taskService;
	protected RepositoryService repositoryService;

	public TaskMailListener() {
		actorService = SpringUtils.getBean("actorService", ActorService.class);
		mailService = SpringUtils.getBean(MailService.class);
		taskService = SpringUtils.getBean(TaskService.class);
		repositoryService = SpringUtils.getBean(RepositoryService.class);
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

		String eventType = delegateTask.getEventName();
		if (eventType.equals("create")) {
			// 判断是否是岗位待办
			boolean isGroupTask = (delegateTask.getAssignee() == null || delegateTask
					.getAssignee().isEmpty());

			TaskEntity task = (TaskEntity) delegateTask;
			// 任务的标题
			String taskSubject = (String) delegateTask
					.getVariableLocal("subject");
			if (taskSubject == null) {
				taskSubject = delegateTask.getName();
			}

			// 邮件内容
			String content = "任务名称：" + taskSubject;
			String businessSubject = (String) task.getVariable("subject");
			if (businessSubject != null && !businessSubject.isEmpty()) {
				content += "\r\n所属业务：" + businessSubject;
			}
			content += "\r\n所属流程："
					+ repositoryService.createProcessDefinitionQuery()
							.processDefinitionId(task.getProcessDefinitionId())
							.singleResult().getName();
			content += "\r\n创建时间："
					+ DateUtils.formatDateTime2Minute(task.getCreateTime());
			if (task.getDueDate() != null) {
				content += "\r\n办理期限："
						+ DateUtils.formatDateTime2Minute(task.getDueDate());
			}
			if (task.getDescription() != null
					&& !task.getDescription().isEmpty()) {
				content += "\r\n附加说明：" + task.getDescription();
			}
			mail.setContent(content);

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
				mail.setSubject("BC岗位待办提醒：" + taskSubject);
			} else {
				// 邮件标题
				mail.setSubject("BC个人待办提醒：" + taskSubject);

				String[] userCodes = new String[] { delegateTask.getAssignee() };
				mailAddresses = actorService.findMailAddressByUser(userCodes);
			}
			mail.setTo(mailAddresses);

			// 发送邮件
			if (mail.getTo() != null && mail.getTo().length > 0)
				mailService.send(mail);
		} else {
			throw new CoreException("unsupport send mail for " + eventType
					+ " task");
		}
	}
}
