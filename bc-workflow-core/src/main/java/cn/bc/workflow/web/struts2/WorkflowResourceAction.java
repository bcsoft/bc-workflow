/**
 *
 */
package cn.bc.workflow.web.struts2;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.DateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.docs.web.AttachUtils;
import cn.bc.web.util.WebUtils;
import cn.bc.workflow.deploy.domain.DeployResource;
import cn.bc.workflow.deploy.service.DeployService;
import com.opensymphony.xwork2.ActionSupport;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * 直接的物理部署资源处理Action
 *
 * @author wis
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class WorkflowResourceAction extends ActionSupport {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowResourceAction.class);
  private static final long serialVersionUID = 1L;
  private DeployService deployService;
  private RepositoryService repositoryService;

  public String filename;
  public String contentType;
  public long contentLength;
  public InputStream inputStream;
  public String key;  // 1.流程编码/资源编码 :processCode/resourceCode_ProcessDefinitionId.js 或 .css
  // 2.资源编码:resourceCode_ProcessDefinitionId.js 或 .css

  @Autowired
  public void setDeployService(DeployService deployService) {
    this.deployService = deployService;
  }

  @Autowired
  public void setRepositoryService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }


  // 获取流程资源
  public String getResource() throws Exception {
    Date startTime = new Date();
    String wfCode;// 流程编码
    String resCode;// 资源编码
    String pdId;//流程定义id

    if (key.indexOf("_") > 0) {
      String[] ss = key.split("_");
      String code = ss[0];
      if (ss[1].indexOf(".") > 0) {
        pdId = ss[1].substring(0, ss[1].indexOf("."));
      } else {
        throw new CoreException("key的值不符合processCode/resourceCode_ProcessDefinitionId.js或 .css; " +
          "resourceCode_ProcessDefinitionId.js或.css格式已致获取资源失败!");
      }
      if (code.indexOf("/") == -1) {
        wfCode = pdId.substring(0, pdId.indexOf(":"));
        resCode = code;
      } else {
        String[] cs = code.split("/");
        wfCode = cs[0];
        resCode = cs[1];
      }
    } else {
      throw new CoreException("key的值不符合processCode/resourceCode_ProcessDefinitionId.js或 .css; " +
        "resourceCode_ProcessDefinitionId.js或.css格式已致获取资源失败!");
    }

    //获取部署记录id
    List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
      .processDefinitionId(pdId).list();
    DeployResource dr;
    if (list.size() == 1) {
      dr = deployService.findDeployResourceByDmIdAndwfCodeAndresCode(
        list.get(0).getDeploymentId(), wfCode, resCode);
    } else {
      throw new CoreException("通过流程定义id查找出多个流程定义对象!");
    }

    if (dr == null)
      throw new CoreException("流程资源不能空!key参数有误");

    // 上传部署资源的存储的绝对路径
    String drRealPath = Attach.DATA_REAL_PATH + "/"
      + DeployResource.DATA_SUB_PATH + "/" + dr.getPath();

    // 附件的扩展名
    String extension = StringUtils.getFilenameExtension(drRealPath);

    // debug
    if (logger.isDebugEnabled()) {
      logger.debug("drRealPath" + drRealPath);
      logger.debug("extension=" + extension);
      logger.debug("wfCode=" + wfCode);
      logger.debug("resCode=" + resCode);
      logger.debug("pdId=" + pdId);
    }

    // 设置下载文件的参数
    this.contentType = AttachUtils.getContentType(extension);
    this.filename = WebUtils.encodeFileName(
      ServletActionContext.getRequest(), dr.getSource());
    File file = new File(drRealPath);
    this.contentLength = file.length();
    this.inputStream = new FileInputStream(file);
    if (logger.isDebugEnabled()) {
      logger.debug("resource:" + DateUtils.getWasteTime(startTime));
    }

    return SUCCESS;
  }

}
