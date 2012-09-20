/**
 * 
 */
package cn.bc.workflow.activiti.delegate;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.SpringUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.identity.service.IdGeneratorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.template.domain.Template;
import cn.bc.template.service.TemplateService;
import cn.bc.workflow.deploy.domain.DeployResource;
import cn.bc.workflow.deploy.service.DeployService;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import cn.bc.workflow.flowattach.service.FlowAttachService;

/**
 * 
 * 根据模板编码获取附件信息并添加到指定的任务上的监听器
 * 
 * @author wis
 * 
 */
public class Attach4TaskListener implements TaskListener {
	private static final Log logger = LogFactory
			.getLog(Attach4TaskListener.class);

	private Expression attachCode; //附件编码
	private Expression resourceCode; //流程资源编码

	public void notify(DelegateTask delegateTask) {
		if (logger.isDebugEnabled()) {
			logger.debug("attachCode="
					+ (attachCode != null ? attachCode.getExpressionText() : null));
			logger.debug("processInstanceId=" 
					+ delegateTask.getProcessInstanceId());
			logger.debug("taskDefinitionKey="
					+ delegateTask.getTaskDefinitionKey());
			logger.debug("taskId=" 
					+ delegateTask.getId());
			logger.debug("eventName=" + delegateTask.getEventName());
		}
		
		Template template;
		TemplateService templateService = SpringUtils.getBean(
				"templateService", TemplateService.class);
		
		if(attachCode != null){//编码不为空,根据编码从模板模块查找附件
			template = templateService.loadByCode(attachCode
					.getExpressionText());
			if (template == null) {
				throw new CoreException("没有找到编码为“"
						+ attachCode.getExpressionText() + "”的附件");
			}else{
				// 模板文件扩展名
				String extension=StringUtils.getFilenameExtension(template.getPath());
				// 声明当前日期时间
				Calendar now = Calendar.getInstance();
				// 文件存储的相对路径（年月），避免超出目录内文件数的限制
				String subFolder = new SimpleDateFormat("yyyyMM").format(now.getTime());
				// 上传文件存储的绝对路径
				String appRealDir=Attach.DATA_REAL_PATH+"/"+FlowAttach.DATA_SUB_PATH;
				// 所保存文件所在的目录的绝对路径名
				String realFileDir=appRealDir+"/"+subFolder;
				// 不含路径的文件名
				String fileName = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(now.getTime()) + "." + extension;
				// 所保存文件的绝对路径名
				String realFilePath=realFileDir+"/"+fileName;
				// 构建文件要保存到的目录
				File _fileDir = new File(realFileDir);
				if (!_fileDir.exists()) {
					if (logger.isFatalEnabled()) 
						logger.fatal("mkdir=" + realFileDir);
					_fileDir.mkdirs();
				}
				// 直接复制附件
				if (logger.isInfoEnabled())
					logger.info("pure copy file");
				//从template目录下的指定文件复制到attachment目录下
				try{
					FileCopyUtils.copy(template.getInputStream(), new FileOutputStream(
							realFilePath));
				}catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
				
				// 插入流程附件记录信息	
				FlowAttach flowAttach = new FlowAttach();
				IdGeneratorService idGeneratorService = SpringUtils.getBean(
						"idGeneratorService", IdGeneratorService.class);
				flowAttach.setUid(idGeneratorService.next(FlowAttach.ATTACH_TYPE));
				flowAttach.setType(FlowAttach.TYPE_ATTACHMENT); //类型：1-附件，2-意见
				flowAttach.setPid(delegateTask.getProcessInstanceId()); //流程id
				flowAttach.setTid(delegateTask.getId());//任务id
				String path = subFolder+"/"+fileName; //文件夹加文件名路径
				if(path.length() > 0){
					flowAttach.setPath(path); //附件路径，物理文件保存的相对路径
					flowAttach
							.setExt(extension); // 扩展名
				}
				flowAttach.setSubject(template.getSubject()); //标题
				flowAttach.setCommon(false); //公共信息
				flowAttach.setDesc(template.getDesc());
				flowAttach.setSize(template.getSize());
				
				flowAttach.setFormatted(true);//附件是否需要格式化,类型为意见时字段为空
				//flowAttach.setTemplateId(template.getId());//模板id
				
				//创建人,最后修改人信息
				SystemContext context = SystemContextHolder.get();
				flowAttach.setAuthor(context.getUserHistory());
				flowAttach.setModifier(context.getUserHistory());
				flowAttach.setFileDate(Calendar.getInstance());
				flowAttach.setModifiedDate(Calendar.getInstance());
				
				FlowAttachService flowAttachService = SpringUtils.getBean(
						"flowAttachService", FlowAttachService.class);
				flowAttachService.save(flowAttach);
			}
		}
		
		if(resourceCode != null){//流程资源编码不为空,根据流程资源编码从流程资源获取信息生成附件
			RepositoryService repositoryService = SpringUtils.getBean(RepositoryService.class);
			
			//获取部署实例对象
			ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
					.processDefinitionId(delegateTask.getProcessDefinitionId()).singleResult();
				
			DeployResource dr; //流程资源
			if(pd != null){
				DeployService deployService = SpringUtils.getBean(
						"deployService", DeployService.class);
				
				dr = deployService.findDeployResourceByDmIdAndwfCodeAndresCode(
						pd.getDeploymentId(),
						pd.getKey(),
						resourceCode.getExpressionText());
			}else{
				throw new CoreException("通过流程定义id查找流程定义对象为空!");
			}
			
			if(dr == null)
				throw new CoreException("没有找到编码未“"
						+ resourceCode.getExpressionText() + "”的附件");
			
			// 模板文件扩展名
			String extension=StringUtils.getFilenameExtension(dr.getPath());
			// 声明当前日期时间
			Calendar now = Calendar.getInstance();
			// 文件存储的相对路径（年月），避免超出目录内文件数的限制
			String subFolder = new SimpleDateFormat("yy yyMM").format(now.getTime());
			// 上传文件存储的绝对路径
			String appRealDir=Attach.DATA_REAL_PATH+"/"+FlowAttach.DATA_SUB_PATH;
			// 所保存文件所在的目录的绝对路径名
			String realFileDir=appRealDir+"/"+subFolder;
			// 不含路径的文件名
			String fileName = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(now.getTime()) + "." + extension;
			// 所保存文件的绝对路径名
			String realFilePath=realFileDir+"/"+fileName;
			// 构建文件要保存到的目录
			File _fileDir = new File(realFileDir);
			if (!_fileDir.exists()) {
				if (logger.isFatalEnabled()) 
					logger.fatal("mkdir=" + realFileDir);
				_fileDir.mkdirs();
			}
			// 直接复制附件
			if (logger.isInfoEnabled())
				logger.info("pure copy file");
			//deploy/resource目录下的指定文件复制到attachment目录下
			try{
				FileCopyUtils.copy(dr.getInputStream(), new FileOutputStream(
						realFilePath));
			}catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
			
			// 插入流程附件记录信息	
			FlowAttach flowAttach = new FlowAttach();
			IdGeneratorService idGeneratorService = SpringUtils.getBean(
					"idGeneratorService", IdGeneratorService.class);
			flowAttach.setUid(idGeneratorService.next(FlowAttach.ATTACH_TYPE));
			flowAttach.setType(FlowAttach.TYPE_ATTACHMENT); //类型：1-附件，2-意见
			flowAttach.setPid(delegateTask.getProcessInstanceId()); //流程id
			flowAttach.setTid(delegateTask.getId());//任务id
			String path = subFolder+"/"+fileName; //文件夹加文件名路径
			if(path.length() > 0){
				flowAttach.setPath(path); //附件路径，物理文件保存的相对路径
				flowAttach
						.setExt(extension); // 扩展名
			}
			flowAttach.setSubject(dr.getSubject()); //标题
			flowAttach.setCommon(false); //公共信息
			flowAttach.setSize(dr.getSize());
			
			flowAttach.setFormatted(true);//附件是否需要格式化,类型为意见时字段为空
			
			//创建人,最后修改人信息
			SystemContext context = SystemContextHolder.get();
			flowAttach.setAuthor(context.getUserHistory());
			flowAttach.setModifier(context.getUserHistory());
			flowAttach.setFileDate(Calendar.getInstance());
			flowAttach.setModifiedDate(Calendar.getInstance());
			
			FlowAttachService flowAttachService = SpringUtils.getBean(
					"flowAttachService", FlowAttachService.class);
			flowAttachService.save(flowAttach);

		}
		
	}

}
