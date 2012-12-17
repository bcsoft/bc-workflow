/**
 * 
 */
package cn.bc.workflow.flowattach.web.struts2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.util.DateUtils;
import cn.bc.core.util.TemplateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.docs.domain.AttachHistory;
import cn.bc.docs.service.AttachService;
import cn.bc.docs.util.OfficeUtils;
import cn.bc.docs.web.AttachUtils;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.template.domain.TemplateParam;
import cn.bc.template.service.TemplateParamService;
import cn.bc.template.service.TemplateTypeService;
import cn.bc.template.util.DocxUtils;
import cn.bc.template.util.FreeMarkerUtils;
import cn.bc.template.util.XlsUtils;
import cn.bc.template.util.XlsxUtils;
import cn.bc.web.util.WebUtils;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import cn.bc.workflow.flowattach.service.FlowAttachService;

import com.opensymphony.xwork2.ActionSupport;

/**
 * 格式化流程附件处理Action
 * 
 * @author lbj
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class FlowAttachFileAction extends ActionSupport {
	private static Log logger = LogFactory
			.getLog(FlowAttachFileAction.class);
	private static final long serialVersionUID = 1L;
	private AttachService attachService;
	private FlowAttachService flowAttachService;
	private TemplateTypeService templateTypeService;
	private TemplateParamService templateParamService;

	@Autowired
	public void setAttachService(AttachService attachService) {
		this.attachService = attachService;
	}

	@Autowired
	public void setFlowAttachService(FlowAttachService flowAttachService) {
		this.flowAttachService = flowAttachService;
	}

	@Autowired
	public void setTemplateTypeService(TemplateTypeService templateTypeService) {
		this.templateTypeService = templateTypeService;
	}
	
	@Autowired
	public void setTemplateParamService(TemplateParamService templateParamService) {
		this.templateParamService = templateParamService;
	}	
	
	public Long id;// 流程附件id


	public String filename;
	public String contentType;
	public long contentLength;
	public InputStream inputStream;


	private static final int BUFFER = 4096;
	public String from;// 指定原始文件的类型，默认为文件扩展名
	public String to;// 预览时转换到的文件类型，默认为pdf


	// 下载附件
	public String download() throws Exception {
		// 加载一个流程附件对象
		FlowAttach flowAttach = flowAttachService.load(id);
		Date startTime = new Date();
		// 附件的绝对路径名
		String path = Attach.DATA_REAL_PATH + File.separator
				+ FlowAttach.DATA_SUB_PATH + File.separator
				+ flowAttach.getPath();

		// debug
		if (logger.isDebugEnabled()) {
			logger.debug("path=" + path);
			logger.debug("extension=" + flowAttach.getExt());
		}

		// 设置下载文件的参数
		this.contentType = AttachUtils.getContentType(flowAttach.getExt());
		this.filename = WebUtils.encodeFileName(
				ServletActionContext.getRequest(),
				flowAttach.getSubject().lastIndexOf(".") == -1 ? flowAttach
						.getSubject() + "." + flowAttach.getExt() : flowAttach
						.getSubject());
		// 获取文件流
		InputStream inputStream = new FileInputStream(path);
		ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER);
		//格式化操作
		if(flowAttach.getFormatted()){
			// 声明格式化参数
			Map<String, Object> params;
			//docx
			if (flowAttach.getExt().equals(
					templateTypeService.loadByCode("word-docx").getExtension())) {
				params=getParams(flowAttach);
				XWPFDocument docx = DocxUtils.format(inputStream, params);
				docx.write(out);
				this.inputStream = new ByteArrayInputStream(out.toByteArray());
				out.close();
				this.contentLength=this.inputStream.available();
			//xls
			} else if (flowAttach.getExt().equals(
					templateTypeService.loadByCode("xls").getExtension())) {
				params=getParams(flowAttach);
				HSSFWorkbook xls = XlsUtils.format(inputStream, params);
				xls.write(out);
				this.inputStream=new ByteArrayInputStream(out.toByteArray());
				out.close();
				this.contentLength=this.inputStream.available();
			//xlsx
			} else if (flowAttach.getExt().equals(
					templateTypeService.loadByCode("xlsx").getExtension())) {
				params=getParams(flowAttach);
				XSSFWorkbook xlsx = XlsxUtils.format(inputStream, params);
				xlsx.write(out);
				this.inputStream=new ByteArrayInputStream(out.toByteArray());
				out.close();
				this.contentLength=this.inputStream.available();
			}else{
				this.contentLength = flowAttach.getSize();
				this.inputStream = inputStream;
			}
		}else{
			this.contentLength = flowAttach.getSize();
			this.inputStream = inputStream;
		}

		if (logger.isDebugEnabled())
			logger.debug("download:" + DateUtils.getWasteTime(startTime));

		//附件保存相对路径
		String resolatePath=FlowAttach.DATA_SUB_PATH + File.separator
				+ flowAttach.getPath();
		
		// 创建文件上传日志
		saveAttachHistory(flowAttach.getSubject(),AttachHistory.TYPE_DOWNLOAD,resolatePath,
				flowAttach.getExt(),"FlowAttach",flowAttach.getUid());
		return SUCCESS;
	}

	// 创建文件上传日志
	private void saveAttachHistory(String subject,int type, String path, String extension,String ptype,String puid) {
		if (ptype != null && puid != null) {
			AttachHistory history = new AttachHistory();
			history.setPtype(ptype);
			history.setPuid(puid);
			history.setType(type);
			history.setAuthor(SystemContextHolder.get().getUserHistory());
			history.setFileDate(Calendar.getInstance());
			history.setPath(path);
			history.setAppPath(false);
			history.setFormat(extension);
			history.setSubject(subject);
			String[] c = WebUtils.getClient(ServletActionContext.getRequest());
			history.setClientIp(c[0]);
			history.setClientInfo(c[2]);
			this.attachService.saveHistory(history);
		} else {
			logger.warn("没有指定ptype、puid参数，不保存文件上传记录");
		}
	}

	// 支持在线打开文档查看的文件下载
	public String inline() throws Exception {
		// 加载一个流程附件对象
		FlowAttach flowAttach = flowAttachService.load(id);
		Date startTime = new Date();
		// 附件的绝对路径名
		String path = Attach.DATA_REAL_PATH + File.separator
				+ FlowAttach.DATA_SUB_PATH + File.separator
				+ flowAttach.getPath();

		// debug
		if (logger.isDebugEnabled()) {
			logger.debug("path=" + path);
			logger.debug("extension=" + flowAttach.getExt());
			logger.debug("to=" + to);
		}

		if (isConvertFile(flowAttach.getExt())) {
			// 获取文件流
			InputStream inputStream = new FileInputStream(path);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
					BUFFER);
			byte[] bs = null;

			if (this.from == null || this.from.length() == 0)
				this.from = flowAttach.getExt();
			if (this.to == null || this.to.length() == 0)
				this.to = getText("jodconverter.to.extension");// 没有指定就是用系统默认的配置转换为pdf
			
			// 声明需要转换的流
			InputStream is = null;
			// 声明格式化参数
			Map<String, Object> params;
			// 判断是否为可格式化类型
			//格式化操作
			if(flowAttach.getFormatted()){
				//docx
				if (flowAttach.getExt().equals(
						templateTypeService.loadByCode("word-docx").getExtension())) {
					params=getParams(flowAttach);
					XWPFDocument docx = DocxUtils.format(inputStream, params);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					docx.write(out);
					is = new ByteArrayInputStream(out.toByteArray());
					out.close();
					// convert
					OfficeUtils.convert(is, this.from, outputStream, this.to);
					bs = outputStream.toByteArray();
				//xls
				} else if (flowAttach.getExt().equals(
						templateTypeService.loadByCode("xls").getExtension())) {
					params=getParams(flowAttach);
					HSSFWorkbook xls = XlsUtils.format(inputStream, params);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					xls.write(out);
					is =new ByteArrayInputStream(out.toByteArray());
					out.close();
					// convert
					OfficeUtils.convert(is, this.from, outputStream, this.to);
					bs = outputStream.toByteArray();
				//xlsx
				} else if (flowAttach.getExt().equals(
						templateTypeService.loadByCode("xlsx").getExtension())) {
					params=getParams(flowAttach);
					XSSFWorkbook xlsx = XlsxUtils.format(inputStream, params);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					xlsx.write(out);
					is=new ByteArrayInputStream(out.toByteArray());
					out.close();
					// convert
					OfficeUtils.convert(is, this.from, outputStream, this.to);
					bs = outputStream.toByteArray();
				//html
				} else if (flowAttach.getExt().equals(
						templateTypeService.loadByCode("html").getExtension())) {
					this.to=flowAttach.getExt();
					params=getParams(flowAttach);
					bs=FreeMarkerUtils.format(TemplateUtils.loadText(inputStream),params).getBytes();
				} else {
					is = inputStream;
					// convert
					OfficeUtils.convert(is, this.from, outputStream, this.to);
					bs = outputStream.toByteArray();
				}
			} else {
				is = inputStream;
				// convert
				OfficeUtils.convert(is, this.from, outputStream, this.to);
				bs = outputStream.toByteArray();
			}


			if (logger.isDebugEnabled())
				logger.debug("convert:" + DateUtils.getWasteTime(startTime));

			// 设置下载文件的参数（设置不对的话，浏览器是不会直接打开的）
			this.inputStream = new ByteArrayInputStream(bs);
			this.inputStream.close();
			this.contentType = AttachUtils.getContentType(this.to);
			this.contentLength =bs.length;
			
		}else {
			// 设置下载文件的参数
			this.contentType = AttachUtils.getContentType(flowAttach.getExt());
			// 无需转换的文档直接下载处理
			File file = new File(path);
			this.contentLength = file.length();
			this.inputStream = new FileInputStream(file);
		}
		
		this.filename = WebUtils.encodeFileName(
				ServletActionContext.getRequest(),
				flowAttach.getSubject().lastIndexOf(".") == -1 ? flowAttach
						.getSubject() + "." + flowAttach.getExt()
						: flowAttach.getSubject());

		//附件保存相对路径
		String resolatePath=FlowAttach.DATA_SUB_PATH + File.separator
				+ flowAttach.getPath();
		// 创建文件上传日志
		saveAttachHistory(flowAttach.getSubject(),AttachHistory.TYPE_INLINE, resolatePath,
				flowAttach.getExt(),"FlowAttach",flowAttach.getUid());

		return SUCCESS;
	}

	// 判断指定的扩展名是否为配置的要转换的文件类型
	private boolean isConvertFile(String extension) {
		String[] extensions = getText("jodconverter.from.extensions")
				.split(",");
		for (String ext : extensions) {
			if (ext.equalsIgnoreCase(extension))
				return true;
		}
		return false;
	}
	
	// 获取替换参数
	private Map<String,Object> getParams(FlowAttach flowAttach) throws Exception{
		// 声明格式化参数
		Map<String, Object> params= new HashMap<String,Object>();
		
		if(!flowAttach.getFormatted())
			return params;

		Map<String, Object> mapFormatSql=new HashMap<String, Object>();
		// 获取替换参数
		if (flowAttach.isCommon()) {
			mapFormatSql.put("pid", flowAttach.getPid());
		} else{
			mapFormatSql.put("tid", flowAttach.getTid());
		}
		for(TemplateParam tp : flowAttach.getParams()){
			params.putAll(templateParamService.getMapParams(tp, mapFormatSql));
		}

		//添加上下文内容
		params.put("SystemContext", SystemContextHolder.get());
		
		//添加上下文路径
		params.put("htmlPageNamespace",ServletActionContext.getRequest().getContextPath());
		//添加时间戳
		if ("true".equalsIgnoreCase(getText("app.debug"))) {
			// 调试状态每次登陆都自动生成
			params.put("appTs",getText(String.valueOf(new Date().getTime())));
		} else {
			// 产品环境使用资源文件配置的值
			params.put("appTs",getText("app.ts"));
		}
		return params;
	}
}
