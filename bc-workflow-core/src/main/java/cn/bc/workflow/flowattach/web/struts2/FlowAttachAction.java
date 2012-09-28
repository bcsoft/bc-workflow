package cn.bc.workflow.flowattach.web.struts2;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import cn.bc.core.util.DateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.docs.domain.AttachHistory;
import cn.bc.docs.service.AttachService;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.service.ActorHistoryService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.struts2.FileEntityAction;
import cn.bc.template.domain.Template;
import cn.bc.template.domain.TemplateParam;
import cn.bc.template.service.TemplateParamService;
import cn.bc.template.service.TemplateService;
import cn.bc.web.ui.html.page.ButtonOption;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.json.Json;
import cn.bc.web.util.WebUtils;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import cn.bc.workflow.flowattach.service.FlowAttachService;


/**
 * 流程附件信息表单Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class FlowAttachAction extends FileEntityAction<Long, FlowAttach> {
	private static final long serialVersionUID = 1L;
	public String pid;//流程实例id
	public String tid;//任务id
	public boolean common;//是否公共信息
	public int type;//类型：1-附件，2-意见
	public String params;
	
	private FlowAttachService flowAttachService;
	private ActorHistoryService actorHistroyService;
	private TemplateService templateService;
	private TemplateParamService templateParamService;
	private AttachService attachService;
	
	@Autowired
	public void setAttachService(AttachService attachService) {
		this.attachService = attachService;
	}

	@Autowired
	public void setFlowAttachService(FlowAttachService flowAttachService) {
		this.flowAttachService = flowAttachService;
		this.setCrudService(flowAttachService);
	}

	@Autowired
	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Autowired
	public void setActorHistroyService(ActorHistoryService actorHistroyService) {
		this.actorHistroyService = actorHistroyService;
	}
	
	@Autowired
	public void setTemplateParamService(TemplateParamService templateParamService) {
		this.templateParamService = templateParamService;
	}

	@Override
	public boolean isReadonly() {
		// 普通人员
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.common"));
	}
	
	@Override
	protected boolean useFormPrint() {
		return false;
	}
	
	@Override
	protected void buildFormPageButtons(PageOption pageOption, boolean editable) {
		if (editable && !isReadonly()) 
			// 添加默认的保存按钮
			pageOption.addButton(new ButtonOption(getText("label.ok"),
					null, "bc.flowattachForm.save").setId("flowattachSave"));
	}

	@Override
	protected PageOption buildFormPageOption(boolean editable) {
		FlowAttach e=this.getE();
		PageOption po=super.buildFormPageOption(editable).setWidth(500);
		//根据类型控制窗口高度。
		if(type==FlowAttach.TYPE_ATTACHMENT||e.getType()==FlowAttach.TYPE_ATTACHMENT){
			po.setHeight(270);
		}else if(type==FlowAttach.TYPE_COMMENT||e.getType()==FlowAttach.TYPE_COMMENT){
			po.setHeight(244);
		}else
			po.setHeight(300);
		return po;
	}
	

	@Override
	protected void beforeSave(FlowAttach entity) {
		super.beforeSave(entity);
		//设置附件大小,附件扩展名
		if(entity.getType()==FlowAttach.TYPE_ATTACHMENT){
			entity.setSize(entity.getSizeEx());
			entity.setExt(StringUtils.getFilenameExtension(entity.getPath()));
		}
		if(params != null && params.length() > 0){
			String [] paramsAry = params.split(",");
			Set<TemplateParam> paramSet = new LinkedHashSet<TemplateParam>();
			TemplateParam p;
			for(int i=0;i<paramsAry.length;i++){
				p = templateParamService.load(Long.valueOf(paramsAry[i]));
				paramSet.add(p);
			}
			entity.setParams(paramSet);
		}
		
	}
	
	@Override
	protected void afterOpen(FlowAttach entity) {
		super.afterOpen(entity);
		PageOption po=buildFormPageOption(false);
		if(entity.getType()==FlowAttach.TYPE_ATTACHMENT){
			po.setHeight(210);
		}else
			po.setHeight(200);
		this.formPageOption=po;
	}

	@Override
	protected void afterEdit(FlowAttach entity) {
		super.afterEdit(entity);
		String str = "";
		for(TemplateParam p : entity.getParams()){
			str += p.getId() +",";
		}
		params = str;
	}
	
	@Override
	public String save() throws Exception {
		Json json=new Json();
		//保存前判断是否为新建保存
		boolean isNew=this.getE().isNew();
		super.save();
		//保存后返回信息操作
		FlowAttach e=this.getE();
		json.put("id", e.getId());
		json.put("success", true);
		json.put("msg", getText("flowAttach.success"));
		ActorHistory ah=actorHistroyService.load(e.getAuthor().getId());
		json.put("author",ah.getName());
		json.put("fileDate",DateUtils.formatCalendar2Second(e.getFileDate()));
		
		//返回给对附件的信心
		if(e.getType()==FlowAttach.TYPE_ATTACHMENT){
			json.put("size", e.getSize());
			json.put("ext", e.getExt());
			json.put("formatted", e.getFormatted());
			json.put("path", e.getPath());
		}
		
		//非新建保存修改时间
		if(!isNew){
			ActorHistory mah=actorHistroyService.load(e.getModifier().getId());
			json.put("modifier", mah.getName());
			json.put("modifiedDate",DateUtils.formatCalendar2Second(e.getModifiedDate()));
		}
		this.json = json.toString();
		return "json";
	}

	@Override
	protected void afterCreate(FlowAttach entity) {
		super.afterCreate(entity);
		entity.setCommon(common);
		entity.setType(type);
		entity.setPid(pid);
		entity.setTid(tid);
		// uid
		entity.setUid(this.getIdGeneratorService().next(FlowAttach.ATTACH_TYPE));
		if(type==FlowAttach.TYPE_ATTACHMENT)
			entity.setFormatted(false);
	}
	
	// ##-------从模板中添加附件-----开始-------##
	public String tplCode;
	public String uid;
	public String loadAttachFromTemplate(){
		Json json=new Json();
		Template t=templateService.loadByCode(tplCode);
		if(t==null){
			json.put("success", false);
			json.put("msg", getText("flowAttach.fromTemplate.lose"));
			this.json=json.toString();
			return "json";
		}
		
		try{
			//声明当前日期时间
			Calendar now = Calendar.getInstance();
			// 文件存储的相对路径（年月），避免超出目录内文件数的限制
			String subFolder = new SimpleDateFormat("yyyyMM").format(now.getTime());
			// 上传文件存储的绝对路径
			String appRealDir=Attach.DATA_REAL_PATH+"/"+FlowAttach.DATA_SUB_PATH;
			// 所保存文件所在的目录的绝对路径名
			String realFileDir=appRealDir+"/"+subFolder;
			// 构建文件要保存到的目录
			File _fileDir = new File(realFileDir);
			if (!_fileDir.exists()) {
				if (logger.isFatalEnabled()) 
					logger.fatal("mkdir=" + realFileDir);
				_fileDir.mkdirs();
			}
			// 模板文件扩展名
			String extension=StringUtils.getFilenameExtension(t.getPath());
			// 不含路径的文件名
			String fileName = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(now.getTime()) + "." + extension;
			// 所保存文件的绝对路径名
			String realFilePath=realFileDir+"/"+fileName;
			// 上下文
			SystemContext sc=(SystemContext) this.getContext();
			// 保存附件记录                                                                //标识此附件从模板中添加
			saveAttachLog(t.getSubject(),"FlowAttachFromTemplate",uid,sc,extension,t.getSize()
					,now,FlowAttach.DATA_SUB_PATH+"/"+subFolder+"/"+fileName,true);
			// 直接复制附件
			if (logger.isInfoEnabled())
				logger.info("pure copy file");
			FileCopyUtils.copy(t.getInputStream(), new FileOutputStream(
					realFilePath));
			
			json.put("path",subFolder+'/'+fileName);
			json.put("success", true);
			json.put("msg", getText("flowAttach.success"));
			this.json=json.toString();
		}catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		
		return "json";
	}
	
	// 保存一个附件记录
	private void saveAttachLog(String localFile,
			String ptype, String puid, SystemContext context, String extend,
			long size, Calendar now, String path, boolean absolute) {
		// 剔除文件名中的路径部分
		int li = localFile.lastIndexOf("/");
		if (li == -1)
			li = localFile.lastIndexOf("\\");
		if (li != -1)
			localFile = localFile.substring(li + 1);

		// 创建附件记录
		Attach attach = new Attach();
		attach.setAuthor(context.getUserHistory());
		attach.setPtype(ptype);
		attach.setPuid(puid);
		attach.setFormat(extend);
		attach.setFileDate(now);
		attach.setPath(path);
		attach.setSize(size);
		attach.setSubject(localFile);
		attach.setAppPath(!absolute);
		attach = attachService.save(attach);

		// 创建附件上传日志
		AttachHistory history = new AttachHistory();
		history.setPtype(Attach.class.getSimpleName());
		history.setPuid(attach.getId().toString());
		history.setType(AttachHistory.TYPE_UPLOAD);
		history.setAuthor(context.getUserHistory());
		history.setFileDate(now);
		history.setPath(path);
		history.setAppPath(false);
		history.setFormat(attach.getFormat());
		history.setSubject(attach.getSubject());
		String[] c = WebUtils.getClient(ServletActionContext.getRequest());
		history.setClientIp(c[0]);
		history.setClientInfo(c[2]);
		attachService.saveHistory(history);
	}
	// ##-------从模板中添加附件-----结束-------##
	
	//就在流程实例名称
	public String loadProcInstName(){
		Json json=new Json();
		json.put("name", flowAttachService.getProcInstName(pid));
		this.json=json.toString();
		return "json";
	}
	
	//加载一个附件信息
	public String loadOneAttach4id(){
		Json json=new Json();
		FlowAttach fa=flowAttachService.load(this.getId());
		if(fa==null){
			json.put("success", false);
			json.put("msg", "flowAttach.lose");
		}else{
			json.put("success", true);
			json.put("subject", fa.getSubject());
			json.put("ext",fa.getExt());
			json.put("path", fa.getPath());
			json.put("uid", fa.getUid());
			json.put("formatted", fa.getFormatted());
		}
		this.json=json.toString();
		return "json";
	}
}
