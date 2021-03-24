/**
 *
 */
package cn.bc.workflow.service;

import cn.bc.core.exception.ConstraintViolationException;
import cn.bc.core.exception.CoreException;
import cn.bc.core.exception.NotExistsException;
import cn.bc.core.exception.PermissionDeniedException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.JsonUtils;
import cn.bc.desktop.service.LoginService;
import cn.bc.docs.domain.Attach;
import cn.bc.docs.service.AttachService;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.service.ActorHistoryService;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.service.IdGeneratorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.identity.web.SystemContextImpl;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.workflow.activiti.ActivitiUtils;
import cn.bc.workflow.dao.WorkflowDao;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.domain.DeployResource;
import cn.bc.workflow.deploy.service.DeployService;
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.domain.WorkflowModuleRelation;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import cn.bc.workflow.flowattach.service.FlowAttachService;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

/**
 * 工作流Service的实现
 *
 * @author dragon
 */
@Service("workflowService")
public class WorkflowServiceImpl implements WorkflowService {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
  @Autowired
  private TemplateService templateService;
  @Autowired
  private RuntimeService runtimeService;
  @Autowired
  private RepositoryService repositoryService;
  @Autowired
  private IdentityService identityService;
  @Autowired
  private TaskService taskService;
  @Autowired
  private ExcutionLogService excutionLogService;
  @Autowired
  private ActorHistoryService actorHistoryService;
  @Autowired
  private FlowAttachService flowAttachService;
  @Autowired
  private HistoryService historyService;
  @Autowired
  @Qualifier(value = "actorService")
  private ActorService actorService;
  @Autowired
  private DeployService deployService;
  @Autowired
  private WorkflowModuleRelationService workflowModuleRelationService;
  @Autowired
  private WorkflowDao workflowDao;
  @Autowired
  private LoginService loginService;
  @Autowired
  private AttachService attachService;
  @Autowired
  private IdGeneratorService idGeneratorService;// 用于生成uid的服务

  /**
   * 获取当前用户的帐号信息
   *
   * @return
   */
  private String getCurrentUserAccount() {
    return SystemContextHolder.get().getUser().getCode();
  }

  @Transactional
  @Override
  public String startFlow(String key,
                          String id,
                          Map<String, Object> globalVariables,
                          Map<String, Object> taskLocalVariables,
                          String moduleType,
                          String moduleId,
                          boolean autoCompleteFirstTask) {
    String processInstanceId;
    if (null != key && !key.isEmpty()) {
      if (null != globalVariables && !globalVariables.isEmpty()) {
        // 启动流程，携带全局变量
        processInstanceId = this.startFlowByKey(key, globalVariables);
      } else {
        // 启动流程
        processInstanceId = this.startFlowByKey(key);
      }
    } else {
      // id 为流程定义 id（格式类似 PayPlanApproval:1:17457707）
      if (id == null || id.isEmpty()) throw new IllegalArgumentException("需指定流程编码或流程定义ID的值才能发起流程!");

      // 启动流程
      processInstanceId = this.startFlowByDefinitionId(id);
    }

    // 如果需要自动完成首个待办任务
    if (autoCompleteFirstTask) {
      // 找到待办任务
      String[] taskIds = this.findTaskIdByProcessInstanceId(processInstanceId);
      if (taskIds == null || taskIds.length == 0)
        throw new RuntimeException("流程实例 " + processInstanceId + " 没有待办任务，无法完成办理！");

      // 完成第一个待办任务的办理
      this.completeTask(taskIds[0], globalVariables, taskLocalVariables);
    }

    // 如果设置了 moduleType、moduleId 就创建模块与流程的关联关系
    if (null != moduleType && null != moduleId) {
      // 保存流程与模块信息的关系
      WorkflowModuleRelation workflowModuleRelation = new WorkflowModuleRelation();
      workflowModuleRelation.setMid(Long.parseLong(moduleId));
      workflowModuleRelation.setPid(processInstanceId);
      workflowModuleRelation.setMtype(moduleType);
      this.workflowModuleRelationService.save(workflowModuleRelation);
    }

    // 如果有附件信息
    if (globalVariables != null && globalVariables.containsKey("attachInfo")) {
      // 设计 attachInfo 的 value 值格式为 ptype_puid_search 组成的字符串，其中前两个 ptype 和 puid 必须存在，search 可以不存在
      String attachInfo = (String) globalVariables.get("attachInfo");
      String[] s = attachInfo.split("_");

      // 找到待办任务
      String[] taskIds = this.findTaskIdByProcessInstanceId(processInstanceId);
      // 流程实例没有待办任务
      if (taskIds == null || taskIds.length == 0) {
        // 复制模块附件到流程全局附件中
        this.copyModuleAttach(s[0], s[1], s.length == 3 ? s[2] : null, processInstanceId, null);
      } else {
        // 复制模块附件到流程首待办任务附件中
        this.copyModuleAttach(s[0], s[1], s.length == 3 ? s[2] : null, processInstanceId, taskIds[0]);
      }
    }

    return processInstanceId;
  }

  /**
   * 将指定模块的附件复制到流程附件中。
   * @param ptype 所关联文档的类型
   * @param puid 所关联文档的UID
   * @param search 所关联文档附件的模糊搜索名
   * @param procInstId 流程id
   * @param taskId 任务id
   * @return 如果复制失败，响应相关错误信息，如果成功，则不返回
   */
  private void copyModuleAttach(String ptype, String puid, String search, String procInstId, String taskId) {
    List<Attach> attachList = attachService.findByPtype(ptype, puid);
    List<Attach> sources = search == null ? attachList : attachList.stream()
        .filter(attach -> attach.getSubject().contains(search)).collect(Collectors.toList());
    if (sources.isEmpty()) {
      throw new NotExistsException("找不到附件！");
    }

    List<FlowAttach> flowAttachList = new ArrayList<>();
    for (Attach attach : sources) {
      // ======== 复制附件到流程任务附件位置中 ========
      // 扩展名
      String extension = this.getFilenameExtension(attach.getPath());
      // 文件存储的相对路径（年月），避免超出目录内文件数的限制
      String subFolder = DateUtils.formatCalendar(Calendar.getInstance(), "yyyyMM");
      // 上传文件存储的绝对路径
      String appRealDir = Attach.DATA_REAL_PATH + File.separator + FlowAttach.DATA_SUB_PATH;
      // 所保存文件所在的目录的绝对路径名
      String realFileDir = appRealDir + File.separator + subFolder;
      // 不含路径的文件名
      String fileName = DateUtils.formatCalendar(Calendar.getInstance(), "yyyyMMddHHmmssSSSS") + "." + extension;
      // 所保存文件的绝对路径名
      String realFilePath = realFileDir + File.separator + fileName;
      // 构建文件要保存到的目录
      File _fileDir = new File(realFileDir);
      if (!_fileDir.exists()) {
        logger.warn("mkdir={}", realFileDir);
        _fileDir.mkdirs();
      }

      // 附件路径
      String path = Attach.DATA_REAL_PATH + File.separator + attach.getPath();

      // 从附件目录下的指定文件复制到attachment目录下
      try {
        FileCopyUtils.copy(new FileInputStream(new File(path)), new FileOutputStream(realFilePath));
      } catch (IOException e) {
        throw new NotExistsException(e.getMessage());
      }

      // 插入流程附件记录信息
      FlowAttach flowAttach = new FlowAttach();
      flowAttach.setUid(idGeneratorService.next(FlowAttach.ATTACH_TYPE));
      flowAttach.setType(FlowAttach.TYPE_ATTACHMENT); // 类型：1-附件，2-意见
      flowAttach.setPid(procInstId); // 流程id
      flowAttach.setPath(subFolder + File.separator + fileName); // 附件路径，物理文件保存的相对路径
      flowAttach.setExt(extension); // 扩展名
      flowAttach.setSubject(attach.getSubject()); // 标题
      flowAttach.setSize(attach.getSize());
      flowAttach.setFormatted(false);// 附件是否需要格式化

      if (taskId == null) {
        flowAttach.setCommon(true); // 公共附件
      } else {
        flowAttach.setCommon(false); // 任务附件
        flowAttach.setTid(taskId);
      }

      // 创建人,最后修改人信息
      SystemContext context = SystemContextHolder.get();
      flowAttach.setAuthor(context.getUserHistory());
      flowAttach.setModifier(context.getUserHistory());
      flowAttach.setFileDate(Calendar.getInstance());
      flowAttach.setModifiedDate(Calendar.getInstance());

      flowAttachList.add(flowAttach);
    }

    // 开始保存
    this.flowAttachService.save(flowAttachList);
  }

  private String getFilenameExtension(String path) {
    if (path == null) {
      return null;
    }
    int sepIndex = path.lastIndexOf('.');
    return (sepIndex != -1 ? path.substring(sepIndex + 1) : null);
  }

  /**
   * 启动指定编码流程的最新版本
   *
   * @param key 流程编码
   * @return 流程实例的id
   */
  @Transactional
  public String startFlowByKey(String key) {
    // 设置Activiti认证用户
    String initiator = setAuthenticatedUser();

    // 启动流程：TODO 表单信息的处理
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(key);
    if (logger.isDebugEnabled()) {
      logger.debug("key=" + key);
      logger.debug("initiator=" + initiator);
      logger.debug("pi=" + ActivitiUtils.toString(pi));
    }

    // 返回流程实例的id
    return pi.getProcessInstanceId();
  }

  /**
   * 启动指定流程定义id的流程
   *
   * @param id
   * @return
   */
  @Transactional
  public String startFlowByDefinitionId(String id) {
    // 设置Activiti认证用户
    String initiator = setAuthenticatedUser();

    ProcessInstance pi = runtimeService.startProcessInstanceById(id);
    if (logger.isDebugEnabled()) {
      logger.debug("id=" + id);
      logger.debug("initiator=" + initiator);
      logger.debug("pi=" + ActivitiUtils.toString(pi));
    }

    // 返回流程实例的id
    return pi.getProcessInstanceId();
  }

  /**
   * 初始化 Activiti 的当前认证用户信息
   *
   * @return 账号
   */
  private String setAuthenticatedUser() {
    return setAuthenticatedUser(null);
  }

  /**
   * 初始化 Activiti 的认证用户信息为指定账号
   *
   * @param initiator 发起流程者的账号，为空则使用当前登录账号
   * @return
   */
  private String setAuthenticatedUser(String initiator) {
    // 获取当前用户
    if (initiator == null || initiator.isEmpty()) {
      initiator = getCurrentUserAccount();
    } else {
      createInitiatorContext(initiator);
    }

    // 设置认证用户
    identityService.setAuthenticatedUserId(initiator);
    return initiator;
  }

  /**
   * 创建指定账号的上下文
   */
  private void createInitiatorContext(String initiator) {
    SystemContext context = SystemContextHolder.get();
    if (context == null || context.getUser() == null || !context.getUser().getCode().equals(initiator)) {
      logger.warn("为账号 {} 创建基本的上下文信息", initiator);
      // 获取账号信息
      Map<String, Object> map = this.loginService.loadActorByCode(initiator);

      // 创建上下文记录用户信息
      context = new SystemContextImpl();
      context.setAttr(SystemContext.KEY_USER, map.get("actor"));
      context.setAttr(SystemContext.KEY_USER_HISTORY, map.get("history"));
      SystemContextHolder.set(context);
    }
  }

  @Transactional
  public void claimTask(String taskId) {
    // 设置Activiti认证用户
    setAuthenticatedUser();

    // 领取任务：TODO 表单信息的处理
    this.taskService.claim(taskId, getCurrentUserAccount());

    // 加载当前任务
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

    // 创建执行日志
    ExcutionLog log = new ExcutionLog();
    log.setFileDate(Calendar.getInstance());
    ActorHistory h = SystemContextHolder.get().getUserHistory();
    log.setAuthorId(h.getId());
    log.setAuthorCode(h.getCode());
    log.setAuthorName(h.getName());

    // 处理人信息
    log.setAssigneeId(h.getId());
    log.setAssigneeCode(h.getCode());
    log.setAssigneeName(h.getName());

    log.setListener("custom");// 自定义
    log.setExcutionId(task.getExecutionId());
    log.setType(ExcutionLog.TYPE_TASK_INSTANCE_CLAIM);
    log.setProcessInstanceId(task.getProcessInstanceId());
    log.setTaskInstanceId(task.getId());
    log.setExcutionCode(task.getTaskDefinitionKey());
    log.setExcutionName(task.getName());

    String date = DateUtils.formatCalendar2Minute(log.getFileDate());
    log.setDescription(h.getName() + "在" + date + "签领了任务");
    // 保存
    this.excutionLogService.save(log);
  }

  @Transactional
  public void completeTask(String taskId) {
    this.completeTask(taskId, null, null);
  }

  @Transactional
  public void completeTask(String taskId,
                           Map<String, Object> globalVariables,
                           Map<String, Object> localVariables) {
    // 设置Activiti认证用户
    setAuthenticatedUser();

    if (logger.isDebugEnabled()) {
      logger.debug("globalVariables=" + globalVariables);
      logger.debug("localVariables=" + localVariables);
    }

    // 设置全局流程变量
    if (globalVariables != null && !globalVariables.isEmpty())
      this.taskService.setVariables(taskId, globalVariables);

    // 设置本地流程变量
    if (localVariables != null && !localVariables.isEmpty())
      this.taskService.setVariablesLocal(taskId, localVariables);

    // 完成任务
    this.taskService.complete(taskId);

  }

  @Transactional
  public ActorHistory delegateTask(String taskId, String toUser) {
    // 设置Activiti认证用户
    setAuthenticatedUser();

    Task task = this.taskService.createTaskQuery().taskId(taskId)
      .singleResult();
    String originAssignee = task.getAssignee();
    // 委托任务
    this.taskService.delegateTask(taskId, toUser);
    if (originAssignee.equalsIgnoreCase(toUser)) {// 委托人是原办理人
      // 保存excutionlog信息
      saveExcutionLogInfo4DelegateAndAssign(taskId, toUser,
        ExcutionLog.TYPE_TASK_INSTANCE_DELEGATE, "任务委托给");
    } else {// 第三方委托
      // 保存excutionlog信息
      ActorHistory ah = this.actorHistoryService.loadByCode(task
        .getAssignee());
      String msg = ah.getName() + "的任务委托给";
      saveExcutionLogInfo4DelegateAndAssign(taskId, toUser,
        ExcutionLog.TYPE_TASK_INSTANCE_DELEGATE, msg);
    }

    return this.actorHistoryService.loadByCode(toUser);
  }

  @Transactional
  public ActorHistory assignTask(String taskId, String toUser) {
    // 设置Activiti认证用户
    setAuthenticatedUser();

    // 领取任务：TODO 表单信息的处理
    this.taskService.claim(taskId, toUser);

    // 保存excutionlog信息
    saveExcutionLogInfo4DelegateAndAssign(taskId, toUser,
      ExcutionLog.TYPE_TASK_INSTANCE_ASSIGN, "任务分派给");

    return this.actorHistoryService.loadByCode(toUser);
  }

  /**
   * 委托,分派操作保存excutionlog信息
   */
  @Transactional
  public void saveExcutionLogInfo4DelegateAndAssign(String taskId,
                                                    String toUser, String type, String msg) {
    // 加载当前任务
    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

    // 创建执行日志
    ExcutionLog log = new ExcutionLog();
    log.setFileDate(Calendar.getInstance());
    ActorHistory h = SystemContextHolder.get().getUserHistory();
    log.setAuthorId(h.getId());
    log.setAuthorCode(h.getCode());
    log.setAuthorName(h.getName());

    // 处理人信息
    ActorHistory h2 = actorHistoryService.loadByCode(toUser);
    log.setAssigneeId(h2.getId());
    log.setAssigneeCode(h2.getCode());
    log.setAssigneeName(h2.getName());

    log.setListener("custom");// 自定义
    log.setExcutionId(task.getExecutionId());
    log.setType(type);
    log.setProcessInstanceId(task.getProcessInstanceId());
    log.setTaskInstanceId(task.getId());
    log.setExcutionCode(task.getProcessDefinitionId());
    log.setExcutionName(task.getName());

    String date = DateUtils.formatCalendar2Minute(log.getFileDate());
    log.setDescription(h.getName() + "在" + date + "成功将" + msg
      + h2.getName());
    // 保存
    this.excutionLogService.save(log);
  }

  @Transactional
  public Deployment deployZipFromTemplate(String templateCode) {
    // 获取模板
    Template template = templateService.loadByCode(templateCode);
    if (template == null) {
      throw new CoreException("not exists template. code=" + templateCode);
    }
    String extension = template.getTemplateType().getExtension();
    if (!("zip".equalsIgnoreCase(extension) || "bar"
      .equalsIgnoreCase(extension))) {
      throw new CoreException("template type must be zip or bar. code="
        + templateCode);
    }

    // 获取文件流
    InputStream zipFile = template.getInputStream();

    // 发布
    String name = template.getSubject();
    if (!name.endsWith(extension)) {
      name += "." + extension;
    }
    Deployment d = repositoryService.createDeployment().name(name)
      .addZipInputStream(new ZipInputStream(zipFile)).deploy();
    return d;
  }

  @Transactional
  public Deployment deployXmlFromTemplate(String templateCode) {
    // 获取模板
    Template template = templateService.loadByCode(templateCode);
    if (template == null) {
      throw new CoreException("not exists template. code=" + templateCode);
    }
    String extension = template.getTemplateType().getExtension();
    if (!("xml".equalsIgnoreCase(extension) || "bpmn"
      .equalsIgnoreCase(extension))) {
      throw new CoreException("template type must be xml or bpmn. code="
        + templateCode);
    }

    // 获取文件流
    InputStream xmlFile = template.getInputStream();

    // 发布
    String name = template.getSubject();
    if (!name.endsWith(extension)) {
      name += "." + extension;
    }
    String code = template.getCode();
    if (!code.endsWith(extension)) {
      code += "." + extension;
    }
    Deployment d = repositoryService.createDeployment().name(name)
      .addInputStream(code, xmlFile).deploy();
    return d;
  }

  @Transactional
  public void deleteDeployment(String deploymentId) {
    repositoryService.deleteDeployment(deploymentId);
  }

  @Transactional
  public void deleteDeployment(String deploymentId, boolean cascade) {
    repositoryService.deleteDeployment(deploymentId, cascade);
  }

  @Transactional(readOnly = true)
  public ProcessInstance loadInstance(String id) {
    return runtimeService.createProcessInstanceQuery()
      .processInstanceId(id).singleResult();
  }

  @Transactional(readOnly = true)
  public ProcessDefinition loadDefinition(String id) {
    return repositoryService.createProcessDefinitionQuery()
      .processDefinitionId(id).singleResult();
  }

  @Transactional(readOnly = true)
  public InputStream getInstanceDiagram(String processInstanceId) {
    // 获取流程实例
    HistoricProcessInstance instance = historyService
      .createHistoricProcessInstanceQuery()
      .processInstanceId(processInstanceId).singleResult();

    if (instance == null) {
      throw new CoreException(
        "can't find HistoricProcessInstance: processInstanceId="
          + processInstanceId);
    }

    // 获取流程定义
    ProcessDefinition definition = repositoryService
      .createProcessDefinitionQuery()
      .processDefinitionId(instance.getProcessDefinitionId())
      .singleResult();

    // 获取流程资源
    DeployResource dr = deployService
      .findDeployResourceByDmIdAndwfCodeAndresCode(
        definition.getDeploymentId(), definition.getKey(),
        definition.getKey());

    InputStream inputStream = null;

    if (dr != null) {
      // 获取流程资源的文件
      inputStream = this.getDeployDiagram(dr);
    } else {
      // 获取发布的资源文件
      inputStream = repositoryService.getResourceAsStream(
        definition.getDeploymentId(),
        definition.getDiagramResourceName());
    }

    return inputStream;
  }

  @Transactional(readOnly = true)
  public InputStream getDiagram(Long deployId) {
    InputStream inputStream = null;
    Deploy deploy = deployService.load(deployId);
    DeployResource dr = this.deployService.findDeployResourceByCode(
      deployId, deploy.getCode());
    // 根据流程编码不能获取流程部署资源,则获取指定Activiti流程部署的相关资源文件流
    if (dr != null) {
      inputStream = this.getDeployDiagram(dr);
    } else {
      inputStream = this.getDeploymentDiagram(deploy.getDeploymentId());
    }
    return inputStream;
  }

  public InputStream getDeployDiagram(DeployResource dr) {
    InputStream inputStream = null;
    // 上传部署资源的存储的绝对路径
    String drRealPath = Attach.DATA_REAL_PATH + "/"
      + DeployResource.DATA_SUB_PATH + "/" + dr.getPath();
    File file = new File(drRealPath);
    try {
      inputStream = new FileInputStream(file);
    } catch (FileNotFoundException e) {
      logger.warn(e.getMessage(), e);
      throw new CoreException(e.getMessage(), e);
    }
    return inputStream;
  }

  @Transactional(readOnly = true)
  public InputStream getDeploymentDiagram(String deploymentId) {
    // 获取流程定义
    ProcessDefinition definition = repositoryService
      .createProcessDefinitionQuery().deploymentId(deploymentId)
      .singleResult();

    if (definition == null) {
      throw new CoreException(
        "can't find ProcessDefinition: deploymentId="
          + deploymentId);
    }

    // 获取流程图的png资源文件
    return getDeploymentResource(deploymentId,
      definition.getDiagramResourceName());
  }

  @Transactional(readOnly = true)
  public InputStream getDeploymentResource(String deploymentId,
                                           String resourceName) {
    return repositoryService
      .getResourceAsStream(deploymentId, resourceName);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getProcessHistoryParams(String processInstanceId) {
    Assert.notNull(processInstanceId,
      "process instance id must not to be null:" + processInstanceId);
    Map<String, Object> params = new HashMap<String, Object>();
    Map<String, Object> taskParams, variableParams;

    // 流程实例
    HistoricProcessInstance pi = historyService
      .createHistoricProcessInstanceQuery()
      .processInstanceId(processInstanceId).singleResult();
    Assert.notNull(pi, "can't find process instance:id" + processInstanceId);
    params.put("id", pi.getId());
    params.put("pdid", pi.getProcessDefinitionId());
    params.put("startUser", getActorNameByCode(pi.getStartUserId()));
    params.put("businessKey", pi.getBusinessKey());
    addDateParam(params, "startTime", pi.getStartTime());
    addDateParam(params, "endTime", pi.getEndTime());
    params.put("duration", pi.getDurationInMillis());
    params.put("deleteReason", pi.getDeleteReason());

    // 流程定义
    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
      .processDefinitionId(pi.getProcessDefinitionId())
      .singleResult();
    params.put("category", pd.getCategory());
    params.put("key", pd.getKey());
    params.put("name", pd.getName());
    params.put("version", pd.getVersion());

    // 流程变量
    HistoricVariableUpdate v;
    List<HistoricDetail> variables = historyService
      .createHistoricDetailQuery()
      .processInstanceId(processInstanceId).variableUpdates()
      .orderByTime().asc().list();
    variableParams = new HashMap<String, Object>();
    for (HistoricDetail hd : variables) {
      v = (HistoricVariableUpdate) hd;
      if (v.getTaskId() == null)
        variableParams.put(v.getVariableName(),
          convertSpecialValiableValue(v));
    }
    params.put("vs", variableParams);

    // 全局意见
    List<FlowAttach> comments = flowAttachService.findCommentsByProcess(
      processInstanceId, false);
    params.put("comments", comments);
    if (logger.isDebugEnabled()) {
      logger.debug("comments.size=" + comments.size());
    }
    params.put("comments_str", buildCommentsString(comments));

    // 全局附件
    List<FlowAttach> attachs = flowAttachService.findAttachsByProcess(
      processInstanceId, false);
    params.put("attachs", attachs);
    if (logger.isDebugEnabled()) {
      logger.debug("attachs.size=" + attachs.size());
    }
    params.put("attachs_str", buildAttachsString(attachs));

    // 经办任务
    String taskCode;
    List<HistoricTaskInstance> tasks = this.historyService
      .createHistoricTaskInstanceQuery()
      .processInstanceId(processInstanceId)
      .taskDeleteReason("completed")
      .orderByHistoricActivityInstanceStartTime().asc().list();
    // 获取所有任务的意见
    String[] tids = new String[tasks.size()];
    for (int i = 0; i < tasks.size(); i++) {
      tids[i] = tasks.get(i).getId();
    }
    List<FlowAttach> allComments = flowAttachService
      .findCommentsByTask(tids);
    List<FlowAttach> allAttachs = flowAttachService.findAttachsByTask(tids);
    List<Map<String, Object>> alltaskParams = new ArrayList<Map<String, Object>>();
    for (HistoricTaskInstance task : tasks) {
      taskCode = task.getTaskDefinitionKey();
      taskParams = new HashMap<String, Object>();
      taskParams.put("id", task.getId());
      taskParams.put("code", taskCode);
      taskParams.put("name", task.getName());
      taskParams.put("owner", task.getOwner());
      taskParams.put("assignee", getActorNameByCode(task.getAssignee()));
      taskParams.put("assigneeCode", task.getAssignee());
      taskParams.put("desc", task.getDescription());
      taskParams.put("dueDate", task.getDueDate());
      taskParams.put("priority", task.getPriority());
      addDateParam(taskParams, "startTime", task.getStartTime());
      addDateParam(taskParams, "endTime", task.getEndTime());
      taskParams.put("duration", task.getDurationInMillis());
      taskParams.put("deleteReason", task.getDeleteReason());

      // 任务的本地流程变量
      variables = historyService.createHistoricDetailQuery()
        .processInstanceId(processInstanceId).taskId(task.getId())
        .variableUpdates().orderByTime().asc().list();
      variableParams = new HashMap<String, Object>();
      for (HistoricDetail hd : variables) {
        v = (HistoricVariableUpdate) hd;
        variableParams.put(v.getVariableName(),
          convertSpecialValiableValue(v));
      }
      taskParams.put("vs", variableParams);

      // 任务的意见
      comments = findTaskFlowAttachs(task.getId(), allComments);
      taskParams.put("comments", comments);
      taskParams.put("comments_str", buildCommentsString(comments));

      // 任务的附件
      attachs = findTaskFlowAttachs(task.getId(), allAttachs);
      taskParams.put("attachs", attachs);
      taskParams.put("attachs_str", buildAttachsString(attachs));

      // add：如果一个节点产生多个实例，只会有最后执行任务的相关信息
      if (params.containsKey(taskCode)) {
        Object p = params.get(taskCode);
        if (p instanceof List) {
          ((List<Map<String, Object>>) p).add(taskParams);// 累加到列表中
        } else if (p instanceof Map) {
          List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
          list.add((Map<String, Object>) p);// Map转换为List
          params.put(taskCode, list);
        } else {
          throw new CoreException(
            "taskCode existed and it's type is unsupport! taskCode="
              + taskCode);
        }
      } else {
        if (taskCode.endsWith("_s")) {// 强制使用List记录
          List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
          list.add(taskParams);
          params.put(taskCode, list);
        } else {
          params.put(taskCode, taskParams);
        }
      }

      // 全部经办实例信息的独立记录
      alltaskParams.add(taskParams);
    }
    params.put("tasks", alltaskParams);

    return params;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getTaskHistoryParams(String taskId) {
    return getTaskHistoryParams(taskId, false);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getTaskHistoryParams(String taskId,
                                                  boolean withProcessInfo) {
    Assert.notNull(taskId, "task instance id must not to be null:" + taskId);
    Map<String, Object> params = new HashMap<String, Object>();
    Map<String, Object> variableParams;

    // 任务实例
    HistoricTaskInstance task = this.historyService
      .createHistoricTaskInstanceQuery().taskId(taskId)
      .singleResult();
    Assert.notNull(task, "can't find task instance:id=" + taskId);
    params = new HashMap<String, Object>();
    params.put("id", task.getId());
    params.put("code", task.getTaskDefinitionKey());
    params.put("owner", task.getOwner());
    params.put("assignee", getActorNameByCode(task.getAssignee()));
    params.put("assigneeCode", task.getAssignee());
    params.put("desc", task.getDescription());
    params.put("dueDate", task.getDueDate());
    params.put("priority", task.getPriority());
    addDateParam(params, "startTime", task.getStartTime());
    addDateParam(params, "endTime", task.getEndTime());
    params.put("duration", task.getDurationInMillis());
    params.put("deleteReason", task.getDeleteReason());

    // 任务的本地流程变量
    HistoricVariableUpdate v;
    List<HistoricDetail> variables = historyService
      .createHistoricDetailQuery().taskId(taskId).variableUpdates()
      .orderByTime().asc().list();
    variableParams = new HashMap<String, Object>();
    for (HistoricDetail hd : variables) {
      v = (HistoricVariableUpdate) hd;
      variableParams.put(v.getVariableName(),
        convertSpecialValiableValue(v));
    }
    params.put("vs", variableParams);

    // 任务的意见
    List<FlowAttach> comments = flowAttachService
      .findCommentsByTask(new String[]{taskId});
    params.put("comments", comments);
    params.put("comments_str", buildCommentsString(comments));

    // 任务的附件
    List<FlowAttach> attachs = flowAttachService
      .findAttachsByTask(new String[]{taskId});
    params.put("attachs", attachs);
    params.put("attachs_str", buildAttachsString(attachs));

    if (withProcessInfo) {
      Map<String, Object> processParams = new HashMap<String, Object>();

      // 流程实例
      HistoricProcessInstance pi = historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(task.getProcessInstanceId())
        .singleResult();
      Assert.notNull(
        pi,
        "can't find process instance:id"
          + task.getProcessInstanceId());
      processParams.put("id", pi.getId());
      processParams.put("pdid", pi.getProcessDefinitionId());
      processParams.put("startUser",
        getActorNameByCode(pi.getStartUserId()));
      processParams.put("businessKey", pi.getBusinessKey());
      addDateParam(processParams, "startTime", pi.getStartTime());
      addDateParam(processParams, "endTime", pi.getEndTime());
      processParams.put("duration", pi.getDurationInMillis());
      processParams.put("deleteReason", pi.getDeleteReason());

      // 流程定义
      ProcessDefinition pd = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionId(pi.getProcessDefinitionId())
        .singleResult();
      processParams.put("category", pd.getCategory());
      processParams.put("key", pd.getKey());
      processParams.put("name", pd.getName());
      processParams.put("version", pd.getVersion());

      // 流程变量
      variables = historyService.createHistoricDetailQuery()
        .processInstanceId(task.getProcessInstanceId())
        .variableUpdates().orderByTime().asc().list();
      variableParams = new HashMap<String, Object>();
      for (HistoricDetail hd : variables) {
        v = (HistoricVariableUpdate) hd;
        if (v.getTaskId() == null)
          variableParams.put(v.getVariableName(),
            convertSpecialValiableValue(v));
      }
      processParams.put("vs", variableParams);

      // 全局意见
      comments = flowAttachService.findCommentsByProcess(
        task.getProcessInstanceId(), false);
      processParams.put("comments", comments);
      if (logger.isDebugEnabled()) {
        logger.debug("pi.comments.size=" + comments.size());
      }
      processParams.put("pi.comments_str", buildCommentsString(comments));

      // 全局附件
      attachs = flowAttachService.findAttachsByProcess(
        task.getProcessInstanceId(), false);
      processParams.put("attachs", attachs);
      if (logger.isDebugEnabled()) {
        logger.debug("pi.attachs.size=" + attachs.size());
      }
      processParams.put("pi.attachs_str", buildAttachsString(attachs));

      params.put("pi", processParams);
    }

    return params;
  }

  private void addDateParam(Map<String, Object> params, String key, Date date) {
    params.put(key, date);
    params.put(key + "2d", DateUtils.formatDate(date));
    params.put(key + "2m", DateUtils.formatDateTime2Minute(date));
  }

  private Object getActorNameByCode(String userCode) {
    return actorService.loadActorNameByCode(userCode);
  }

  /**
   * 特殊流程变量值的转换
   *
   * @param v
   * @return
   */
  private Object convertSpecialValiableValue(HistoricVariableUpdate v) {
    if (v.getVariableName().startsWith("list_")
      && v.getValue() instanceof String) {// 将字符串转化为List
      return JsonUtils.toCollection((String) v.getValue());
    } else if (v.getVariableName().startsWith("map_")
      && v.getValue() instanceof String) {// 将字符串转化为Map
      return JsonUtils.toMap((String) v.getValue());
    } else if (v.getVariableName().startsWith("array_")
      && v.getValue() instanceof String) {// 将字符串转化为数组
      return JsonUtils.toArray((String) v.getValue());
    } else {
      return v.getValue();
    }
  }

  /**
   * @param comments
   * @return
   */
  private StringBuffer buildCommentsString(List<FlowAttach> comments) {
    StringBuffer comments_str;
    comments_str = new StringBuffer();
    /*
     * for (FlowAttach comment : comments) { // 意见的字符串表示：“[姓名1] [时间1]
     * [标题1]\r\n[姓名2] [时间2] [标题2]...”
     * comments_str.append(comment.getAuthor().getName() + " " +
     * DateUtils.formatCalendar2Minute(comment.getFileDate()) + " " +
     * comment.getSubject() + "\r\n"); }
     */

    if (comments.isEmpty())
      return comments_str;

    String desc = "";

    // 构建意见字符串
    for (int i = 0; i < comments.size(); i++) {
      desc = comments.get(i).getDesc();
      if (desc == null || desc.equals("")) {
        desc = comments.get(i).getSubject();
      }
      if (i + 1 == comments.size()) {
        comments_str.append(desc);
      } else {
        comments_str.append(desc + "　");
      }
    }
    return comments_str;
  }

  /**
   * @param attachs
   * @return
   */
  private StringBuffer buildAttachsString(List<FlowAttach> attachs) {
    StringBuffer attachs_str;
    attachs_str = new StringBuffer();

    if (attachs.isEmpty())
      return attachs_str;

    // 附件字符串
    for (int i = 0; i < attachs.size(); i++) {
      if (i + 1 == attachs.size()) {
        attachs_str.append(attachs.get(i).getSubject());
      } else {
        attachs_str.append(attachs.get(i).getSubject() + ",");
      }
    }
    return attachs_str;
  }

  /**
   * 筛选出指定任务的意见、附件
   *
   * @param taskId
   * @param allFlowAttachs
   * @return
   */
  private List<FlowAttach> findTaskFlowAttachs(String taskId,
                                               List<FlowAttach> allFlowAttachs) {
    List<FlowAttach> taskFlowAttachs = new ArrayList<FlowAttach>();
    for (FlowAttach flowAttach : allFlowAttachs) {
      if (taskId.equals(flowAttach.getTid()))
        taskFlowAttachs.add(flowAttach);
    }
    return taskFlowAttachs;
  }

  @Transactional
  public void deleteInstance(String[] instanceIds) {
    if (instanceIds == null || instanceIds.length == 0) {
      throw new CoreException("没有指定要删除的流程实例信息！");
    }

    List<WorkflowModuleRelation> wmrs;
    List<Long> wmrIds;

    for (String id : instanceIds) {
      HistoricProcessInstance pi = this.historyService
        .createHistoricProcessInstanceQuery().processInstanceId(id)
        .singleResult();
      if (pi == null) {
        throw new CoreException("要删除的流程实例在系统总已经不存在：id=" + id);
      }

      //删除流程模块关系
      wmrs = this.workflowModuleRelationService.findList(id);
      if (wmrs != null && wmrs.size() > 0) {
        wmrIds = new ArrayList<Long>();
        for (WorkflowModuleRelation wmr : wmrs) {
          wmrIds.add(wmr.getId());
        }
        this.workflowModuleRelationService.delete(wmrIds.toArray(new Long[]{}));
      }


      boolean flowing = pi.getEndTime() == null;

      // 删除流转中数据
      if (flowing) {
        this.runtimeService.deleteProcessInstance(id, "force-delete");
      }

      // 删除历史数据
      this.historyService.deleteHistoricProcessInstance(id);

      // 删除流转日志、意见、附件 TODO
    }
  }

  /**
   * 激活流程
   */
  @Transactional
  public void doActive(String id) {
    // 设置Activiti认证用户
    String initiator = setAuthenticatedUser();

    // 激活流程
    runtimeService.activateProcessInstanceById(id);

    if (logger.isDebugEnabled()) {
      logger.debug("id=" + id);
      logger.debug("initiator=" + initiator);
    }

    HistoricProcessInstance pi = historyService
      .createHistoricProcessInstanceQuery().processInstanceId(id)
      .singleResult();

    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
      .processDefinitionId(pi.getProcessDefinitionId())
      .singleResult();

    // 创建执行日志
    ExcutionLog log = new ExcutionLog();
    log.setFileDate(Calendar.getInstance());
    ActorHistory h = SystemContextHolder.get().getUserHistory();
    log.setAuthorId(h.getId());
    log.setAuthorCode(h.getCode());
    log.setAuthorName(h.getName());

    // 处理人信息
    log.setAssigneeId(h.getId());
    log.setAssigneeCode(h.getCode());
    log.setAssigneeName(h.getName());

    log.setListener("custom");// 自定义
    log.setExcutionId("0");
    log.setType(ExcutionLog.TYPE_PROCESS_ACTIVE);
    log.setProcessInstanceId(id);
    log.setExcutionCode(pi.getProcessDefinitionId());
    log.setExcutionName(pd.getName());

    String date = DateUtils.formatCalendar2Minute(log.getFileDate());
    log.setDescription(h.getName() + "在" + date + "成功将" + pd.getName()
      + "激活");
    // 保存
    this.excutionLogService.save(log);

  }

  /**
   * 暂停流程
   */
  @Transactional
  public void doSuspended(String id) {
    // 设置Activiti认证用户
    String initiator = setAuthenticatedUser();

    // 暂停流程
    runtimeService.suspendProcessInstanceById(id);

    if (logger.isDebugEnabled()) {
      logger.debug("id=" + id);
      logger.debug("initiator=" + initiator);
    }

    HistoricProcessInstance pi = historyService
      .createHistoricProcessInstanceQuery().processInstanceId(id)
      .singleResult();

    ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
      .processDefinitionId(pi.getProcessDefinitionId())
      .singleResult();

    // 创建执行日志
    ExcutionLog log = new ExcutionLog();
    log.setFileDate(Calendar.getInstance());
    ActorHistory h = SystemContextHolder.get().getUserHistory();
    log.setAuthorId(h.getId());
    log.setAuthorCode(h.getCode());
    log.setAuthorName(h.getName());

    // 处理人信息
    log.setAssigneeId(h.getId());
    log.setAssigneeCode(h.getCode());
    log.setAssigneeName(h.getName());

    log.setListener("custom");// 自定义
    log.setExcutionId("0");
    log.setType(ExcutionLog.TYPE_PROCESS_SUSPENDED);
    log.setProcessInstanceId(id);
    log.setExcutionCode(pi.getProcessDefinitionId());
    log.setExcutionName(pd.getName());

    String date = DateUtils.formatCalendar2Minute(log.getFileDate());
    log.setDescription(h.getName() + "在" + date + "成功将" + pd.getName()
      + "暂停");
    // 保存
    this.excutionLogService.save(log);

  }

  @Transactional
  public String startFlowByKey(String key, Map<String, Object> variables) {
    return startFlowByKey(null, key, variables);
  }

  @Transactional
  public String startFlowByKey(String initiator, String key, Map<String, Object> variables) {
    // 设置Activiti认证用户
    initiator = setAuthenticatedUser(initiator);

    // 启动流程： 表单信息的处理
    ProcessInstance pi = runtimeService.startProcessInstanceByKey(key, variables);
    if (logger.isDebugEnabled()) {
      logger.debug("initiator={}, key={}", initiator, key);
      logger.debug("pi={}", ActivitiUtils.toString(pi));
      logger.debug("variables={}", variables.toString());
    }

    // 返回流程实例的id
    return pi.getProcessInstanceId();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> findGlobalValue(String pid, String[] valueKeys) {
    return this.workflowDao.findGlobalValue(pid, valueKeys);
  }

  @Transactional(readOnly = true)
  public Object findLocalValue(String pid, String taskKey,
                               String localValueKey) {
    return this.workflowDao.findLocalValue(pid, taskKey, localValueKey);
  }

  @Transactional(readOnly = true)
  public String[] findTaskIdByProcessInstanceId(String processInstanceId) {
    // 获得待办任务列表
    List<Task> listTask = this.taskService.createTaskQuery()
      .processInstanceId(processInstanceId)
      .orderByTaskCreateTime().asc().list();

    if (listTask.isEmpty())
      return null;

    String[] arrTaskIds = new String[listTask.size()];
    int i = 0;
    for (Task task : listTask) {
      arrTaskIds[i] = task.getId();
      i++;
    }

    return arrTaskIds;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> findMainProcessInstanceInfoById(String processInstanceId) {
    return this.workflowDao.findMainProcessInstanceInfoById(processInstanceId);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> findSubProcessInstanceInfoById(String processInstanceId) {
    return this.workflowDao.findSubProcessInstanceInfoById(processInstanceId);
  }

  @Override
  @Transactional
  public boolean updateDeploymentResource(String deploymentId, String resourceName, InputStream in) throws IOException {
    byte[] bytes = FileCopyUtils.copyToByteArray(in);
    return this.updateDeploymentResource(deploymentId, resourceName, bytes);
  }

  @Override
  @Transactional
  public boolean updateDeploymentResource(String deploymentId, String resourceName, byte[] in) {
    return this.workflowDao.updateDeploymentResource(deploymentId, resourceName, in);
  }

  @Override
  @Transactional
  public void deleteInstanceNotDeal2Personal(String id, String code)
    throws NotExistsException, ConstraintViolationException, PermissionDeniedException {
    long length = historyService.createHistoricTaskInstanceQuery().processInstanceId(id).count();// 历史任务个数
    String initiator = (String) runtimeService.getVariable(id, "initiator");// 发起人

    //region 验证：流程实例是否存在
    if (length == 0) {
      throw new NotExistsException(this.getClass().getName() +
        " historyService.createHistoricTaskInstanceQuery().processInstanceId(" + id + ").count() 结果为0");
    }
    //endregion

    //region 验证：流程实例是否未办理
    if (length > 1) {
      throw new ConstraintViolationException(this.getClass().getName() +
        " historyService.createHistoricTaskInstanceQuery().processInstanceId(" + id + ").count() 结果为" + length);
    }
    //endregion

    //region 验证：code 变量是否是发起人
    if (!initiator.equals(code)) {
      throw new PermissionDeniedException(this.getClass().getName() +
        " 参数 code=" + code + " 流程初始化者initiator=" + initiator);
    }
    //endregion

    // 删除该流程实例
    this.deleteInstance(new String[]{id});
  }
}