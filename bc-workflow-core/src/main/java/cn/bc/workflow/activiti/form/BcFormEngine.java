package cn.bc.workflow.activiti.form;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.form.FormData;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.form.FormEngine;
import org.activiti.engine.impl.persistence.entity.ResourceEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.JsonUtils;
import cn.bc.core.util.TemplateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.template.util.FreeMarkerUtils;
import cn.bc.web.util.WebUtils;

public class BcFormEngine implements FormEngine {
	private static final Log logger = LogFactory.getLog(BcFormEngine.class);

	/** BC平台表单引擎的名称 */
	public final static String NAME = BcFormEngine.class.getSimpleName();

	private TemplateService templateService;

	@Autowired
	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

	/*
	 * 此表单引擎的名称，在调用FormService.getRenderedStartForm和getRenderedTaskForm时须指定的参数
	 */
	public String getName() {
		return NAME;
	}

	public Object renderStartForm(StartFormData startForm) {
		if (startForm.getFormKey() == null) {
			return null;
		}
		String formTemplateString = getForm(startForm);
		ScriptingEngines scriptingEngines = Context
				.getProcessEngineConfiguration().getScriptingEngines();
		return scriptingEngines.evaluate(formTemplateString,
				ScriptingEngines.DEFAULT_SCRIPTING_LANGUAGE, null);
	}

	public Object renderTaskForm(TaskFormData taskForm) {
		if (taskForm.getFormKey() == null) {
			return null;
		}

		return getForm(taskForm);
	}

	/**
	 * 获取表单
	 * 
	 * @param formInstance
	 * @return
	 */
	private String getForm(FormData formInstance) {
		// 根据formKey确认模板渲染类型
		String formKey = formInstance.getFormKey();
		int index = formKey.lastIndexOf(":");
		String engine, from, key;
		boolean seperate;
		if (index != -1) {
			String[] ss = formKey.substring(0, index).split(":");
			key = formKey.substring(index + 1);
			engine = ss[0];
			if (ss.length == 1) {// engine:
				seperate = false;
				from = "resource";
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

		// 获取模板的原始内容
		String sourceFormString = loadFormTemplate(formInstance, from, key);
		if (logger.isDebugEnabled()) {
			logger.debug("key=" + key);
			logger.debug("source=" + sourceFormString);
		}

		// 格式化表单模板
		String form = formatForm(formInstance, engine, sourceFormString);
		if (logger.isDebugEnabled()) {
			logger.debug("form=" + form);
		}

		return form;
	}

	/**
	 * 格式化表单
	 * 
	 * @param formInstance
	 * @param source
	 * @return
	 */
	private String formatForm(FormData formInstance, String engine,
			String source) {
		String form;

		// 格式化
		if ("fm".equalsIgnoreCase(engine)
				|| "freemarker".equalsIgnoreCase(engine)) {// 使用freemarker模板引擎
			Map<String, Object> args = getFormatArgs((TaskFormData) formInstance);// 读取变量
			form = FreeMarkerUtils.format(source, args);
		} else if ("ct".equalsIgnoreCase(engine)
				|| "commontemplate".equalsIgnoreCase(engine)) {// 使用commontemplate模板引擎
			Map<String, Object> args = getFormatArgs((TaskFormData) formInstance);// 读取变量
			form = TemplateUtils.format(source, args);
		} else {// 使用activiti的默认方式格式化
			form = formatFormByActivitiDefault(source,
					(TaskFormData) formInstance);
		}
		return form;
	}

	private String formatFormByActivitiDefault(String formTemplateString,
			TaskFormData taskForm) {
		ScriptingEngines scriptingEngines = Context
				.getProcessEngineConfiguration().getScriptingEngines();
		TaskEntity task = (TaskEntity) taskForm.getTask();
		return (String) scriptingEngines.evaluate(formTemplateString,
				ScriptingEngines.DEFAULT_SCRIPTING_LANGUAGE,
				task.getExecution());
	}

	private Map<String, Object> getFormatArgs(TaskFormData taskForm) {
		Map<String, Object> args = new HashMap<String, Object>();
		TaskEntity task = (TaskEntity) taskForm.getTask();
		VariableScope variableScope = task.getExecution();

		// 设置一些全局变量
		Calendar now = Calendar.getInstance();// 当前时间
		args.put("now", DateUtils.formatCalendar2Second(now));
		args.put("now2d", DateUtils.formatCalendar2Day(now));
		args.put("now2m", DateUtils.formatCalendar2Minute(now));
		args.put("year", now.get(Calendar.YEAR) + "");
		args.put("month", (now.get(Calendar.MONTH) + 1) + "");
		args.put("day", now.get(Calendar.DAY_OF_MONTH));

		// // 全局流程变量
		// Map<String, Object> gvs = variableScope.getVariables();
		// args.putAll(gvs);

		// 本地流程变量：会自动获取全局流程变量（如果本地无）
		Map<String, Object> lvs = variableScope.getVariablesLocal();
		for (Entry<String, Object> e : lvs.entrySet()) {
			if (e.getKey().startsWith("list_")
					&& e.getValue() instanceof String) {// 将字符串转化为List
				args.put(e.getKey(),
						JsonUtils.toCollection((String) e.getValue()));
			} else if (e.getKey().startsWith("map_")
					&& e.getValue() instanceof String) {// 将字符串转化为Map
				args.put(e.getKey(), JsonUtils.toMap((String) e.getValue()));
			} else if (e.getKey().startsWith("array_")
					&& e.getValue() instanceof String) {// 将字符串转化为数组
				args.put(e.getKey(), JsonUtils.toArray((String) e.getValue()));
			} else {
				args.put(e.getKey(), e.getValue());
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("args=" + args);
		}
		return args;
	}

	/**
	 * 获取表单的原始内容
	 * 
	 * @param formInstance
	 * @param from
	 * @param key
	 * @return
	 */
	private String loadFormTemplate(FormData formInstance, String from,
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
		} else {// 默认使用activiti的方法加载资源
			sourceFormString = loadFormTemplateByActivitiDefault(formInstance,
					key);
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

	/**
	 * 使用activiti的默认脚本引擎格式化模板
	 * 
	 * @param formInstance
	 * @param formKey
	 * @return
	 */
	private String loadFormTemplateByActivitiDefault(FormData formInstance,
			String formKey) {
		String formTemplateString;
		String deploymentId = formInstance.getDeploymentId();
		ResourceEntity resourceStream = Context
				.getCommandContext()
				.getResourceManager()
				.findResourceByDeploymentIdAndResourceName(deploymentId,
						formKey);

		if (resourceStream == null) {
			throw new ActivitiException("Form with formKey '" + formKey
					+ "' does not exist");
		}

		byte[] resourceBytes = resourceStream.getBytes();
		formTemplateString = new String(resourceBytes);
		return formTemplateString;
	}
}
