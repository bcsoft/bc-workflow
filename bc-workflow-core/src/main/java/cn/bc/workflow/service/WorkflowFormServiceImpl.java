/**
 * 
 */
package cn.bc.workflow.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.TemplateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.template.util.FreeMarkerUtils;
import cn.bc.web.util.WebUtils;
import cn.bc.workflow.deploy.domain.DeployResource;

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

	public String getRenderedTaskForm(String taskId, boolean readonly) {
		Map<String, Object> addParams = new HashMap<String, Object>();
		addParams.put("readonly", String.valueOf(readonly));
		return getRenderedTaskForm(taskId, addParams);
	}

	public String getRenderedTaskForm(String taskId,
			Map<String, Object> addParams) {
		// 获取formKey
		String formKey = excutionLogService.findTaskFormKey(taskId);
		if (formKey == null || formKey.length() == 0)
			return null;

		// 根据formKey确认模板渲染类型
		int index = formKey.lastIndexOf(":");
		String engine, from, key;
		boolean seperate;
		if (index != -1) {
			String[] ss = formKey.substring(0, index).split(":");
			key = formKey.substring(index + 1);
			engine = ss[0];
			if (ss.length == 1) {// engine:
				seperate = false;
				from = "res";
			} else if (ss.length == 2) {// engine:from:
				seperate = false;
				from = ss[1];
			} else if (ss.length == 3) {// engine:seperate:from:
				seperate = "true".equalsIgnoreCase(ss[1]);
				from = ss[2];
			} else {
				throw new CoreException("unsupport config type:formKey="
						+ formKey);
			}
		} else {
			key = formKey;
			engine = "default";
			from = "resource";
			seperate = false;
		}
		if (logger.isInfoEnabled()) {
			logger.info("engine=" + engine);
			logger.info("key=" + key);
			logger.info("from=" + from);
			logger.info("seperate=" + seperate);
		}

		// 添加一些任务的定义参数
		HistoricTaskInstance task = historyService
				.createHistoricTaskInstanceQuery().taskId(taskId)
				.singleResult();
		
		// 获取模板的原始内容
		String sourceFormString = loadFormTemplate(from, key, task.getProcessDefinitionId());
		if (logger.isDebugEnabled()) {
			logger.debug("source=" + sourceFormString);
		}

		Map<String, Object> params = new LinkedHashMap<String, Object>();

		// 添加一些全局变量
		Calendar now = Calendar.getInstance();// 当前时间
		params.put("now", DateUtils.formatCalendar2Second(now));
		params.put("now2d", DateUtils.formatCalendar2Day(now));
		params.put("now2m", DateUtils.formatCalendar2Minute(now));
		params.put("year", now.get(Calendar.YEAR)+"");
		params.put("month", now.get(Calendar.MONTH) + 1);
		params.put("day", now.get(Calendar.DAY_OF_MONTH));
		//加一个月
		now.add(Calendar.MONTH, 1);
		params.put("nextMonth",now.get(Calendar.MONTH) + 1);
		params.put("nextMonthOfYear", now.get(Calendar.YEAR)+"");

		// 获取任务的流程变量
		Map<String, Object> vs = this.excutionLogService
				.findTaskVariables(taskId);
		params.putAll(vs);

		// 添加额外的格式化参数
		if (addParams != null) {
			params.putAll(addParams);
		}

		if (task == null) {
			throw new CoreException("can't find taskHistory: id=" + taskId);
		}
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
	private String loadFormTemplate(String from, String key, String pdid) {
		String sourceFormString;
		if ("tpl".equalsIgnoreCase(from)) {// 从模板中加载资源
			sourceFormString = loadFormTemplateByTemplate(key);
		} else if ("url".equalsIgnoreCase(from)) {// 使用URL方式 TODO
			sourceFormString = loadFormTemplateByUrl(key);
		} else if ("file".equalsIgnoreCase(from)) {// 使用服务器文件方式
			sourceFormString = loadFormTemplateByFile(key);
		} else if ("res".equalsIgnoreCase(from)) {// 从类资源中获取
			sourceFormString = loadFormTemplateByClassResource(key);
		} else if ("wf".equalsIgnoreCase(from)){// 从部署资源中获取
			ProcessDefinition p = this.repositoryService
					.createProcessDefinitionQuery().processDefinitionId(pdid)
					.singleResult();
			String pCode = p.getKey();//流程编码
			
			sourceFormString = "";
			//sourceFormString = loadFormTemplateByDeployResource();
		} else {
			throw new CoreException("unsupport form type:from=" + from);
		}
		return sourceFormString;
	}

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

	private String loadFormTemplateByUrl(String key) {
		// TODO
		// 如果直接使用HttpClient或Jsoup对URL执行请求，session的同步需要处理
		throw new CoreException("unsupport method:loadFormTemplateByUrl");
	}

	private String loadFormTemplateByTemplate(String key) {
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
	
	private String loadFormTemplateByDeployResource(String rCode) {
		// 获取文件流
		
		String path = "/bcdata/"+DeployResource.DATA_SUB_PATH;
		//InputStream resFile = this.getClass().getResourceAsStream("/" + key);
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 通过流程实例id,部署资源code
	 * @param pCode
	 * @param rCode
	 * @return
	 */
	@SuppressWarnings("unused")
	private String loadFormTemplateByDeployResource(String pCode,String rCode) {
		// 获取文件流
		
		String path = "/bcdata/"+DeployResource.DATA_SUB_PATH;
		//InputStream resFile = this.getClass().getResourceAsStream("/" + key);
		// TODO Auto-generated method stub
		return null;
	}
}
