/**
 * 
 */
package cn.bc.workflow.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.bc.workflow.dao.WorkflowDao;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.TemplateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.template.util.FreeMarkerUtils;
import cn.bc.web.util.WebUtils;
import cn.bc.workflow.deploy.domain.DeployResource;
import cn.bc.workflow.deploy.service.DeployService;

/**
 * @author dragon
 * 
 */
public class WorkflowFormServiceImpl implements WorkflowFormService {
	private static final Log logger = LogFactory
			.getLog(WorkflowFormServiceImpl.class);
	private ExcutionLogService excutionLogService;
	private TemplateService templateService;
	private HistoryService historyService;
	private RepositoryService repositoryService;
	private DeployService deployService;
	private WorkflowDao workflowDao;
	
	@Autowired
	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Autowired
	public void setExcutionLogService(ExcutionLogService excutionLogService) {
		this.excutionLogService = excutionLogService;
	}

	@Autowired
	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}
	
	@Autowired
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@Autowired
	public void setDeployService(DeployService deployService) {
		this.deployService = deployService;
	}

	@Autowired
	public void setWorkflowDao(WorkflowDao workflowDao) {
		this.workflowDao = workflowDao;
	}

	public String getRenderedTaskForm(String taskId, boolean readonly) {
		Map<String, Object> addParams = new HashMap<String, Object>();
		addParams.put("readonly", String.valueOf(readonly));
		return getRenderedTaskForm(taskId, addParams);
	}

	@Override
	public List<Map<String, Object>> findSubProcessInstanceInfoById(String processInstanceId) {

		return this.workflowDao.findSubProcessInstanceInfoById(processInstanceId);
	}

	public String getRenderedTaskForm(String taskId,
			Map<String, Object> addParams) {
		// 获取任务信息
		HistoricTaskInstance task = historyService
				.createHistoricTaskInstanceQuery().taskId(taskId)
				.singleResult();
		if (task == null) {
			throw new CoreException("can't find taskHistory: id=" + taskId);
		}

		// 获取formKey
		String formKey = excutionLogService.findTaskFormKey(taskId);
		if (formKey == null || formKey.length() == 0)
			return null;

		// 根据formKey确认模板渲染类型
		int index = formKey.indexOf(":");
		String engine, from, key;
		if (index != -1) {// 平台定义的格式支持：http://rongjih.blog.163.com/blog/static/33574461201263124922670/
			String[] ss = formKey.split(":");// “engine:from:key”格式
			Assert.isTrue(ss.length == 3, "unsupport config type:formKey="
					+ formKey);
			engine = ss[0];// 格式化引擎类型
			from = ss[1];// 来源类型
			key = ss[2];// 实际的表单配置键，不能包含字符“:”
		} else {// activiti内置的格式支持
			engine = "default";
			from = "res";
			key = formKey;
		}
		if (logger.isInfoEnabled()) {
			logger.info("engine=" + engine);
			logger.info("from=" + from);
			logger.info("key=" + key);
		}

		// 获取模板的原始内容
		String sourceFormString = loadFormTemplate(task, from, key);
		if (logger.isDebugEnabled()) {
			logger.debug("source=" + sourceFormString);
		}

		Map<String, Object> params = new LinkedHashMap<String, Object>();

		// 添加一些全局变量
		Calendar now = Calendar.getInstance();// 当前时间
		params.put("now", DateUtils.formatCalendar2Second(now));
		params.put("now2d", DateUtils.formatCalendar2Day(now));
		params.put("now2m", DateUtils.formatCalendar2Minute(now));
		params.put("year", now.get(Calendar.YEAR) + "");
		params.put("month", now.get(Calendar.MONTH) + 1);
		params.put("day", now.get(Calendar.DAY_OF_MONTH));
		// 加一个月
		now.add(Calendar.MONTH, 1);
		params.put("nextMonth", now.get(Calendar.MONTH) + 1);
		params.put("nextMonthOfYear", now.get(Calendar.YEAR) + "");
		//加载上下文信息
		params.put("SystemContext",SystemContextHolder.get());

		// 获取任务的流程变量
		Map<String, Object> vs = this.excutionLogService
				.findTaskVariables(taskId);
		params.putAll(vs);

		// 添加额外的格式化参数
		if (addParams != null) {
			params.putAll(addParams);
		}

		// 添加一些任务的定义参数
		params.put("taskId", task.getId());
		params.put("taskKey", task.getTaskDefinitionKey());
		params.put("processKey", task.getProcessDefinitionId());

		if (logger.isDebugEnabled()) {
			logger.debug("params=" + params);
		}
		String form = formatForm(engine, sourceFormString, params);
		if (logger.isDebugEnabled()) {
			logger.debug("form=" + form);
		}

		return form;
	}

	/**
	 * 格式化表单
	 * 
	 * @param source
	 * @param params
	 * @return
	 */
	private String formatForm(String engine, String source,
			Map<String, Object> params) {
		String form;

		// 格式化
		if ("fm".equalsIgnoreCase(engine)
				|| "freemarker".equalsIgnoreCase(engine)) {// 使用freemarker模板引擎
			form = FreeMarkerUtils.format(source, params);
		} else if ("ct".equalsIgnoreCase(engine)
				|| "commontemplate".equalsIgnoreCase(engine)) {// 使用commontemplate模板引擎
			form = TemplateUtils.format(source, params);
		} else {
			throw new CoreException("unsupport form engine type:engine="
					+ engine);
		}
		return form;
	}

	/**
	 * 获取表单的原始内容
	 * 
	 * @param from
	 * @param key
	 * @return
	 */
	private String loadFormTemplate(HistoricTaskInstance task, String from,
			String key) {
		String sourceFormString;
		if ("tpl".equalsIgnoreCase(from)) {// 从模板中加载资源
			sourceFormString = loadFormTemplateByTemplate(key);
		} else if ("url".equalsIgnoreCase(from)) {// 使用URL方式 TODO
			sourceFormString = loadFormTemplateByUrl(key);
		} else if ("file".equalsIgnoreCase(from)) {// 使用服务器文件方式
			sourceFormString = loadFormTemplateByFile(key);
		} else if ("res".equalsIgnoreCase(from)) {// 从类资源中获取
			sourceFormString = loadFormTemplateByClassResource(key);
		} else if ("wf".equalsIgnoreCase(from)) {// 从流程部署的资源中获取
			sourceFormString = loadFormTemplateByWFResource(task, key);
		} else {
			throw new CoreException("unsupport form type:from=" + from);
		}
		return sourceFormString;
	}

	// 从应用目录下获取表单
	private String loadFormTemplateByFile(String key) {
		// 替换路径
		if (key.startsWith("$")) {
			key = key.replace("${dataDir}", Attach.DATA_REAL_PATH);
			key = key.replace("${appDir}", WebUtils.rootPath);
		} else {// 当作相对路径处理
			key = WebUtils.rootPath + (key.startsWith("/") ? key : "/" + key);
		}
		// 获取文件流
		try {
			InputStream file = new FileInputStream(key);
			return new String(FileCopyUtils.copyToByteArray(file));
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			throw new CoreException(e.getMessage(), e);
		}
	}

	// 从类资源中获取表单
	private String loadFormTemplateByClassResource(String key) {
		// 获取文件流
		InputStream resFile = this.getClass().getResourceAsStream("/" + key);
		try {
			return new String(FileCopyUtils.copyToByteArray(resFile));
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			throw new CoreException(e.getMessage(), e);
		}
	}

	// 从URL请求结果中获取表单
	private String loadFormTemplateByUrl(String key) {
		// TODO
		// 如果直接使用HttpClient或Jsoup对URL执行请求，session的同步需要处理
		throw new CoreException("unsupport method:loadFormTemplateByUrl");
	}

	// 从模版模块中获取表单
	private String loadFormTemplateByTemplate(String key) {
		// 将符号“/”替换为“:”
		key = key.replaceAll("/", ":");

		String sourceFormString;
		Template template = templateService.loadByCode(key);
		if (template == null) {
			throw new CoreException("can't find template:code=" + key);
		}
		if (!template.isPureText()) {
			throw new CoreException("template isn't pure text:code=" + key);
		}
		sourceFormString = template.getContentEx();
		return sourceFormString;
	}

	// 从流程部署资源中获取表单
	private String loadFormTemplateByWFResource(HistoricTaskInstance task,
			String key) {
		String wfCode;// 流程编码
		String resCode;// 资源编码
		if (key.indexOf("/") == -1) {
			wfCode = task.getProcessDefinitionId().substring(0,
					task.getProcessDefinitionId().indexOf(":"));
			resCode = key;
		} else {
			String[] cs = key.split("/");
			wfCode = cs[0];
			resCode = cs[1];
		}
		if (logger.isDebugEnabled()) {
			logger.debug("wfCode=" + wfCode);
			logger.debug("resCode=" + resCode);
		}
		//获取部署记录id
		List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(task.getProcessDefinitionId()).list();
		DeployResource dr;
		if(list.size() == 1){
			dr = deployService.findDeployResourceByDmIdAndwfCodeAndresCode(
					list.get(0).getDeploymentId(),wfCode, resCode);
		}else{
			throw new CoreException("通过流程定义id查找出多个流程定义对象!");
		}
		
		if(dr == null)
			throw new CoreException("unsupport method:loadFormTemplateByWFResource");

		// 上传部署资源的存储的绝对路径
		String drRealPath = Attach.DATA_REAL_PATH + "/"
				+ DeployResource.DATA_SUB_PATH + "/" + dr.getPath();
		// 获取文件流
		try {
			InputStream file = new FileInputStream(drRealPath);
			return FileCopyUtils.copyToString(new InputStreamReader(file,"UTF-8"));
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			throw new CoreException(e.getMessage(), e);
		}
		
	}
	
}
