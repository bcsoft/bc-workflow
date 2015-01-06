/**
 * 
 */
package cn.bc.workflow.service;

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
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author dragon
 * 
 */
public class WorkflowFormServiceImpl implements WorkflowFormService {
	private static final Logger logger = LoggerFactory.getLogger(WorkflowFormServiceImpl.class);
	@Autowired
	private TemplateService templateService;

	@Autowired
	private DeployService deployService;

	@Override
	public String getRenderedTaskForm(Map<String, Object> task, boolean readonly) {
		Map<String, Object> addParams = new HashMap<>();
		addParams.put("readonly", String.valueOf(readonly));
		return getRenderedTaskForm(task, addParams);
	}

	@Override
	public String getRenderedTaskForm(Map<String, Object> task, Map<String, Object> addParams) {
		Date start = new Date();
		String taskId = (String) task.get("id");

		// 获取formKey
		String formKey = (String) task.get("form_key");
		if (formKey == null || formKey.length() == 0) return null;

		// 根据formKey确认模板渲染类型
		int index = formKey.indexOf(":");
		String engine, from, key;
		if (index != -1) {// 平台定义的格式支持：http://rongjih.blog.163.com/blog/static/33574461201263124922670/
			String[] ss = formKey.split(":");// “engine:from:key”格式
			Assert.isTrue(ss.length == 3, "unsupport config type: taskId=" + taskId + ", formKey=" + formKey);
			engine = ss[0];// 格式化引擎类型
			from = ss[1];// 来源类型
			key = ss[2];// 实际的表单配置键，不能包含字符“:”
		} else {// activiti 内置的格式支持
			engine = "default";
			from = "res";
			key = formKey;
		}
		logger.info("taskId={}, engine={}, from={}, key={}", taskId, engine, from, key);

		// 获取模板的原始内容
		String sourceFormString = loadFormTemplate(task, from, key);
		logger.debug("form source={}", sourceFormString);
		if(logger.isInfoEnabled()) logger.info("loadFormTemplate waste {}", DateUtils.getWasteTime(start));

		Map<String, Object> params = new LinkedHashMap<>();

		Map<String, Object> process_instance = (Map<String, Object>) task.get("process_instance");
		// 添加一些特殊变量
		// ==== 当前时间
		addCurrentTimeParams(params);
		// ==== 系统上下文
		params.put("context",SystemContextHolder.get());
		params.put("SystemContext",SystemContextHolder.get());// old
		// ==== 任务的参数
		params.put("ti_id", task.get("id"));
		params.put("ti_key", task.get("key"));
		params.put("ti_deleteReason", task.get("delete_reason"));// deprecated
		params.put("ti_startTime", task.get("start_time"));
		params.put("ti_endTime", task.get("end_time"));
		params.put("ti_description", task.get("description"));
		params.put("ti_assignee", ((Map<String, Object>) task.get("actor")).get("name"));
		params.put("ti_owner", task.get("owner"));// TODO
		params.put("ti_name", task.get("name"));
		params.put("ti_priority", task.get("priority"));
		params.put("ti_dueDate", task.get("due_date"));
		// ==== 流程实例的一些参数
		params.put("pi_id", process_instance.get(""));
		Map<String, Object> definition = (Map<String, Object>) process_instance.get("definition");
		params.put("pi_businessKey", definition.get("key"));
		params.put("pi_definitionId", definition.get("id"));
		params.put("pi_startUserId", ((Map<String, Object>) process_instance.get("start_user")).get("name"));
		params.put("pi_deleteReason", process_instance.get("delete_reason"));
		// ==== 流程定义的参数
		params.put("pd_id", definition.get("id"));
		params.put("pd_category", definition.get("category"));// TODO
		params.put("pd_deploymentId", definition.get("deployment_id"));
		params.put("pd_key", definition.get("key"));
		params.put("pd_name", definition.get("name"));
		if(logger.isInfoEnabled()) logger.info("addSpecialParams waste {}", DateUtils.getWasteTime(start));

		// 获取任务的流程变量
		Map<String, Object> global_variables = (Map<String, Object>) process_instance.get("variables");
		Map<String, Object> local_variables = (Map<String, Object>) task.get("variables");
		params.putAll(global_variables);// 先放全局变量
		params.putAll(local_variables);// 再放本地变量(本地变量优先使用)
		if(logger.isInfoEnabled()) logger.info("findTaskVariables waste {}", DateUtils.getWasteTime(start));

		// 添加额外的格式化参数
		if (addParams != null) params.putAll(addParams);

		// 添加一些任务的定义参数 old
		//params.put("taskId", task.get("id"));
		//params.put("taskKey", task.get("key"));
		//params.put("processKey", ((Map<String, Object>) process_instance.get("definition")).get("key"));

		// 格式化表单
		logger.debug("params={}", params);
		String form = formatForm(engine, sourceFormString, params);
		logger.debug("form formatted={}", form);
		if(logger.isInfoEnabled()) logger.info("formatForm {}", DateUtils.getWasteTime(start));

		return form;
	}

	private void addCurrentTimeParams(Map<String, Object> params) {
		Calendar now = Calendar.getInstance();
		params.put("now", DateUtils.formatCalendar2Second(now));
		params.put("now2d", DateUtils.formatCalendar2Day(now));
		params.put("now2m", DateUtils.formatCalendar2Minute(now));
		params.put("year", now.get(Calendar.YEAR) + "");
		params.put("month", now.get(Calendar.MONTH) + 1);
		params.put("day", now.get(Calendar.DAY_OF_MONTH));
		// ==== 当前时间加一个月
		now.add(Calendar.MONTH, 1);
		params.put("nextMonth", now.get(Calendar.MONTH) + 1);
		params.put("nextMonthOfYear", now.get(Calendar.YEAR) + "");
	}

	/**
	 * 格式化表单
	 * 
	 * @param source
	 * @param params
	 * @return
	 */
	private String formatForm(String engine, String source, Map<String, Object> params) {
		String form;

		// 格式化
		if ("fm".equalsIgnoreCase(engine) || "freemarker".equalsIgnoreCase(engine)) {// 使用freemarker模板引擎
			form = FreeMarkerUtils.format(source, params);
		} else if ("ct".equalsIgnoreCase(engine) || "commontemplate".equalsIgnoreCase(engine)) {// 使用commontemplate模板引擎
			form = TemplateUtils.format(source, params);
		} else {
			throw new CoreException("unsupport form engine type:engine=" + engine);
		}
		return form;
	}

	/**
	 * 获取表单的原始内容
	 * 
	 * @param task 任务
	 * @param from 表单来源标识
	 * @param key 资源编码
	 * @return
	 */
	private String loadFormTemplate(Map<String, Object> task, String from, String key) {
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
	private String loadFormTemplateByWFResource(Map<String, Object> task, String key) {
		Map<String, Object> process_instance = (Map<String, Object>) task.get("process_instance");
		Map<String, Object> process_definition = (Map<String, Object>) process_instance.get("definition");
		String deploymentId = (String) process_definition.get("deployment_id");
		String wfCode;// 流程编码
		String resCode;// 资源编码
		if (key.indexOf("/") == -1) {
			wfCode = (String) process_definition.get("key");
			resCode = key;
		} else {
			String[] cs = key.split("/");
			wfCode = cs[0];
			resCode = cs[1];
		}
		logger.debug("deploymentId={}, wfCode={}, resCode={}", deploymentId, wfCode, resCode);

		// 获取资源内容
		return this.deployService.getResourceContent(deploymentId, resCode);

		/*
		String deployment_id = (String) process_definition.get("deployment_id");
		DeployResource dr = deployService.findDeployResourceByDmIdAndwfCodeAndresCode(deployment_id, wfCode, resCode);
		if(dr == null)
			throw new CoreException("找不到流程配置的资源文件：deployment_id=" + deployment_id + ", wfCode=" + wfCode + ", resCode=" + resCode);

		// 上传部署资源的存储的绝对路径
		String drRealPath = Attach.DATA_REAL_PATH + "/" + DeployResource.DATA_SUB_PATH + "/" + dr.getPath();
		// 获取文件流
		try {
			InputStream file = new FileInputStream(drRealPath);
			return FileCopyUtils.copyToString(new InputStreamReader(file,"UTF-8"));
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
			throw new CoreException(e.getMessage(), e);
		}
		*/
	}
}
