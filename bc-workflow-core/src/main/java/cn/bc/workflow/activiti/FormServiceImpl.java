/**
 *
 */
package cn.bc.workflow.activiti;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.FreeMarkerUtils;
import cn.bc.core.util.TemplateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.web.util.WebUtils;
import cn.bc.workflow.activiti.form.BcFormEngine;
import cn.bc.workflow.deploy.service.DeployService;
import cn.bc.workflow.service.ExcutionLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author dragon
 */
@Component("bcFormService")
public class FormServiceImpl extends org.activiti.engine.impl.FormServiceImpl {
  private static final Logger logger = LoggerFactory.getLogger(FormServiceImpl.class);
  private ExcutionLogService excutionLogService;
  private TemplateService templateService;
  @Autowired
  private DeployService deployService;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setTemplateService(TemplateService templateService) {
    this.templateService = templateService;
  }

  @Autowired
  public void setExcutionLogService(ExcutionLogService excutionLogService) {
    this.excutionLogService = excutionLogService;
  }

  @Override
  public Object getRenderedTaskForm(String taskId) {
    return this.getRenderedTaskForm(taskId, BcFormEngine.NAME);
  }

  @Override
  public Object getRenderedTaskForm(String taskId, String engineName) {
    if (BcFormEngine.NAME.equals(engineName)) {
      // 获取formKey
      String formKey = excutionLogService.findTaskFormKey(taskId);
      if (formKey == null || formKey.length() == 0)
        return null;
      return getForm(taskId, formKey);
    } else {
      return super.getRenderedTaskForm(taskId, engineName);
    }
  }

  /**
   * 获取表单
   *
   * @param formKey
   * @return
   */
  private String getForm(String taskId, String formKey) {
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
    if (logger.isInfoEnabled()) {
      logger.info("key=" + key);
      logger.info("from=" + from);
      logger.info("seperate=" + seperate);
    }

    // 获取模板的原始内容
    String sourceFormString = loadFormTemplate(from, key, taskId);
    if (logger.isDebugEnabled()) {
      logger.debug("source=" + sourceFormString);
    }

    // 格式化表单模板
    Map<String, Object> params = this.excutionLogService
      .findTaskVariables(taskId);
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
      form = null;
      new CoreException("unsupport form engine type:engine=" + engine);
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
  private String loadFormTemplate(String from, String key, String taskId) {
    String sourceFormString;
    if ("tpl".equalsIgnoreCase(from)) {// 从模板中加载资源
      sourceFormString = loadFormTemplateByTemplate(key);
    } else if ("url".equalsIgnoreCase(from)) {// 使用URL方式 TODO
      sourceFormString = loadFormTemplateByUrl(key);
    } else if ("file".equalsIgnoreCase(from)) {// 使用服务器文件方式
      sourceFormString = loadFormTemplateByFile(key);
    } else if ("res".equalsIgnoreCase(from)
      || "resource".equalsIgnoreCase(from)) {// 从类资源中获取
      sourceFormString = loadFormTemplateByClassResource(key);
    } else if ("wf".equalsIgnoreCase(from)) {// 从流程部署的资源中获取
      sourceFormString = loadFormTemplateByWFResource(key, taskId);
    } else {
      sourceFormString = null;
      new CoreException("unsupport form type:from=" + from);
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

  // 从流程部署资源中获取表单
  private String loadFormTemplateByWFResource(String key, String taskId) {
    String sql = "select f.key_ wf_code, f.deployment_id_ deployment_id\n" +
      " from act_re_procdef f\n" +
      " inner join act_hi_taskinst t on t.proc_def_id_ = f.id_\n" +
      " where t.id_ = ?";
    Map<String, Object> m = jdbcTemplate.queryForMap(sql, taskId);
    String deploymentId = (String) m.get("deployment_id");  // 流程部署ID
    String wfCode = (String) m.get("wf_code");              // 流程编码
    String resCode;                                         // 资源编码
    if (!key.contains("/")) {
      resCode = key;
    } else {
      String[] cs = key.split("/");
      resCode = cs[1];
    }
    logger.debug("deploymentId={}, wfCode={}, resCode={}", deploymentId, wfCode, resCode);

    // 获取资源内容
    return this.deployService.getResourceContent(deploymentId, resCode);
  }
}
