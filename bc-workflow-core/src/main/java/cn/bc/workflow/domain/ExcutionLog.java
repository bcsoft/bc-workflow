package cn.bc.workflow.domain;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;

import cn.bc.core.EntityImpl;

/**
 * 流转日志
 * <p>
 * 用于弥补activiti无法获取上一任务信息的不足
 * </p>
 * 
 * @author dragon
 * 
 */
@Entity
@Table(name = "BC_WF_EXCUTION_LOG")
public class ExcutionLog extends EntityImpl {
	private static final long serialVersionUID = 1L;

	/** 类型:流程的启动 */
	public static final String TYPE_PROCESS_START = "process_"
			+ ExecutionListener.EVENTNAME_START;
	/** 类型:流程的结束 */
	public static final String TYPE_PROCESS_END = "process_"
			+ ExecutionListener.EVENTNAME_END;
	/** 类型:流程激活 */
	public static final String TYPE_PROCESS_ACTIVE = "process_active";
	/** 类型:流程暂停 */
	public static final String TYPE_PROCESS_SUSPENDED = "process_suspended";

	/** 类型:分支的执行 */
	public static final String TYPE_FLOW_TAKE = "flow_"
			+ ExecutionListener.EVENTNAME_TAKE;

	/** 类型:创建任务 */
	public static final String TYPE_TASK_INSTANCE_CREATE = "task_"
			+ TaskListener.EVENTNAME_CREATE;
	/** 类型:完成任务 */
	public static final String TYPE_TASK_INSTANCE_COMPLETE = "task_"
			+ TaskListener.EVENTNAME_COMPLETE;
	/** 类型:分配任务 */
	public static final String TYPE_TASK_INSTANCE_ASSIGNMENT = "task_"
			+ TaskListener.EVENTNAME_ASSIGNMENT;
	/** 类型:领取任务 */
	public static final String TYPE_TASK_INSTANCE_CLAIM = "task_claim";
	/** 类型:委托任务 */
	public static final String TYPE_TASK_INSTANCE_DELEGATE = "task_delegate";
	/** 类型:分派任务 */
	public static final String TYPE_TASK_INSTANCE_ASSIGN = "task_assign";

	/** 类型:活动的启动 */
	public static final String TYPE_EXCUTION_START = "excution_"
			+ ExecutionListener.EVENTNAME_START;
	/** 类型:活动的结束 */
	public static final String TYPE_EXCUTION_END = "excution_"
			+ ExecutionListener.EVENTNAME_END;
	/** 类型:活动的跳转 */
	public static final String TYPE_EXCUTION_TAKE = "excution_"
			+ ExecutionListener.EVENTNAME_TAKE;

	private Calendar fileDate;// 创建时间
	private Long authorId;// 创建人ID(对应ActoryHistory表的ID)
	private String authorCode;// 创建人帐号
	private String authorName;// 创建人姓名

	private Long assigneeId;// 处理人ID(对应ActoryHistory表的ID)
	private String assigneeCode;// 处理人帐号
	private String assigneeName;// 处理人姓名

	private String type;// 日志类型：参考TYPE_XXX常数的定义
	private String listener;// 监听器类型
	private String formKey;// 流程、任务表单formKey的值

	private String excutionId;// 执行实例的ID
	private String excutionCode;// 执行实例的编码：对应流程、任务、流向的definitionKey
	private String excutionName;// 执行实例的名称：对应流程、任务、流向的名称

	private String processInstanceId;// 流程实例ID
	private String taskInstanceId;// 任务实例ID

	private String description;// 其它信息

	@Column(name = "ECODE")
	public String getExcutionCode() {
		return excutionCode;
	}

	public void setExcutionCode(String excutionCode) {
		this.excutionCode = excutionCode;
	}

	@Column(name = "ENAME")
	public String getExcutionName() {
		return excutionName;
	}

	public void setExcutionName(String excutionName) {
		this.excutionName = excutionName;
	}

	@Column(name = "FILE_DATE")
	public Calendar getFileDate() {
		return fileDate;
	}

	public void setFileDate(Calendar fileDate) {
		this.fileDate = fileDate;
	}

	@Column(name = "AUTHOR_ID")
	public Long getAuthorId() {
		return authorId;
	}

	public void setAuthorId(Long authorId) {
		this.authorId = authorId;
	}

	@Column(name = "AUTHOR_CODE")
	public String getAuthorCode() {
		return authorCode;
	}

	public void setAuthorCode(String authorCode) {
		this.authorCode = authorCode;
	}

	@Column(name = "AUTHOR_NAME")
	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	@Column(name = "ASSIGNEE_ID")
	public Long getAssigneeId() {
		return assigneeId;
	}

	public void setAssigneeId(Long assigneeId) {
		this.assigneeId = assigneeId;
	}

	@Column(name = "ASSIGNEE_CODE")
	public String getAssigneeCode() {
		return assigneeCode;
	}

	public void setAssigneeCode(String assigneeCode) {
		this.assigneeCode = assigneeCode;
	}

	@Column(name = "ASSIGNEE_NAME")
	public String getAssigneeName() {
		return assigneeName;
	}

	public void setAssigneeName(String assigneeName) {
		this.assigneeName = assigneeName;
	}

	@Column(name = "TYPE_")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Column(name = "LISTENTER")
	public String getListener() {
		return listener;
	}

	public void setListener(String listener) {
		this.listener = listener;
	}

	@Column(name = "EID")
	public String getExcutionId() {
		return excutionId;
	}

	public void setExcutionId(String excutionId) {
		this.excutionId = excutionId;
	}

	@Column(name = "PID")
	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	@Column(name = "TID")
	public String getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(String taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	@Column(name = "FORMKEY")
	public String getFormKey() {
		return formKey;
	}

	public void setFormKey(String formKey) {
		this.formKey = formKey;
	}

	@Column(name = "DESC_")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
