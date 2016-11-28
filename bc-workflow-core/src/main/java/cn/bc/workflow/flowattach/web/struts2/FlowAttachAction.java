package cn.bc.workflow.flowattach.web.struts2;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.apache.struts2.ServletActionContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import cn.bc.core.exception.CoreException;
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
import cn.bc.workflow.domain.ExcutionLog;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import cn.bc.workflow.flowattach.service.FlowAttachService;
import cn.bc.workflow.service.ExcutionLogService;

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
	public String pid;// 流程实例id
	public String tid;// 任务id
	public boolean common;// 是否公共信息
	public boolean formatted;// 是否格式化，默认false
	public int type;// 类型：1-附件，2-意见，3-临时子流程附件
	public String params;

	private FlowAttachService flowAttachService;
	private ActorHistoryService actorHistroyService;
	private TemplateService templateService;
	private TemplateParamService templateParamService;
	private AttachService attachService;
	private ExcutionLogService excutionLogService;
	private HistoryService historyService;
	
	@Autowired
	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}

	@Autowired
	public void setExcutionLogService(ExcutionLogService excutionLogService) {
		this.excutionLogService = excutionLogService;
	}

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
	public void setTemplateParamService(
			TemplateParamService templateParamService) {
		this.templateParamService = templateParamService;
	}

	@Override
	public boolean isReadonly() {
		// 普通人员
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole("BC_WORKFLOW_START");
	}

	@Override
	protected boolean useFormPrint() {
		return false;
	}

	@Override
	protected void buildPageButtons(PageOption pageOption, boolean editable) {
		if (editable && !isReadonly())
			// 添加默认的保存按钮
			pageOption.addButton(new ButtonOption(getText("label.ok"), null,
					"bc.flowattachForm.save").setId("flowattachSave"));
	}

	@Override
	protected PageOption buildPageOption(boolean editable) {
		FlowAttach e = this.getE();
		PageOption po = super.buildPageOption(editable).setWidth(500);
		// 根据类型控制窗口高度。
		if (type == FlowAttach.TYPE_ATTACHMENT
				|| e.getType() == FlowAttach.TYPE_ATTACHMENT) {
			po.setHeight(270);
		} else if (type == FlowAttach.TYPE_COMMENT
				|| e.getType() == FlowAttach.TYPE_COMMENT) {
			po.setHeight(244);
		} else {
			po.setHeight(300);
		}
		po.setMinimizable(false);
		po.setMaximizable(false);
		return po;
	}

	@Override
	protected void beforeSave(FlowAttach entity) {
		super.beforeSave(entity);
		// 设置附件大小,附件扩展名
		if (entity.getType() == FlowAttach.TYPE_ATTACHMENT || entity.getType() == FlowAttach.TYPE_TEMP_ATTACHMENT) {
			entity.setSize(entity.getSizeEx());
			entity.setExt(StringUtils.getFilenameExtension(entity.getPath()));
		}
		if (params != null && params.length() > 0) {
			String[] paramsAry = params.split(",");
			Set<TemplateParam> paramSet = new LinkedHashSet<TemplateParam>();
			TemplateParam p;
			for (int i = 0; i < paramsAry.length; i++) {
				p = templateParamService.load(Long.valueOf(paramsAry[i]));
				paramSet.add(p);
			}
			entity.setParams(paramSet);
		}

	}

	@Override
	protected void afterOpen(FlowAttach entity) {
		super.afterOpen(entity);
		PageOption po = buildPageOption(false);
		if (entity.getType() == FlowAttach.TYPE_ATTACHMENT) {
			po.setHeight(210);
		} else
			po.setHeight(200);
		this.pageOption = po;
	}

	@Override
	protected void afterEdit(FlowAttach entity) {
		super.afterEdit(entity);
		String str = "";
		for (TemplateParam p : entity.getParams()) {
			str += p.getId() + ",";
		}
		params = str;
	}

	// 创建和保存
	public String subject;
	public String path;
	public Long size;

	public String createAndSave() {
		Json json = new Json();
		SystemContext context = this.getSystyemContext();
		FlowAttach e = new FlowAttach();
		this.afterCreate(e);
		e.setSubject(subject);
		e.setPath(path);
		e.setFormatted(formatted);
		e.setAuthor(context.getUserHistory());
		e.setFileDate(Calendar.getInstance());
		// 设置最后更新人的信息
		e.setModifier(context.getUserHistory());
		e.setModifiedDate(Calendar.getInstance());
		// 设置附件大小,附件扩展名
		if (type == FlowAttach.TYPE_ATTACHMENT || type == FlowAttach.TYPE_TEMP_ATTACHMENT) {
			e.setSize(size);
			e.setExt(StringUtils.getFilenameExtension(path));
		}
		e = this.flowAttachService.save(e);
		// 保存后返回信息操作
		json.put("id", e.getId());
		json.put("subject", subject);
		json.put("success", true);
		json.put("msg", getText("flowAttach.success"));
		ActorHistory ah = actorHistroyService.load(e.getAuthor().getId());
		json.put("author", ah.getName());
		json.put("fileDate", DateUtils.formatCalendar2Second(e.getFileDate()));

		// 返回给对附件的信心
		if (e.getType() == FlowAttach.TYPE_ATTACHMENT) {
			json.put("size", e.getSize());
			json.put("ext", e.getExt());
			json.put("formatted", e.getFormatted());
			json.put("path", e.getPath());
		}

		this.json = json.toString();
		return "json";
	}

	@Override
	public String save() throws Exception {
		Json json = new Json();
		// 保存前判断是否为新建保存
		boolean isNew = this.getE().isNew();
		super.save();
		// 保存后返回信息操作
		FlowAttach e = this.getE();
		json.put("id", e.getId());
		json.put("success", true);
		json.put("msg", getText("flowAttach.success"));
		ActorHistory ah = actorHistroyService.load(e.getAuthor().getId());
		json.put("author", ah.getName());
		json.put("fileDate", DateUtils.formatCalendar2Second(e.getFileDate()));

		// 返回给对附件的信心
		if (e.getType() == FlowAttach.TYPE_ATTACHMENT) {
			json.put("size", e.getSize());
			json.put("ext", e.getExt());
			json.put("formatted", e.getFormatted());
			json.put("path", e.getPath());
		}

		// 非新建保存修改时间
		if (!isNew) {
			ActorHistory mah = actorHistroyService
					.load(e.getModifier().getId());
			json.put("modifier", mah.getName());
			json.put("modifiedDate",
					DateUtils.formatCalendar2Second(e.getModifiedDate()));
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
		if (type == FlowAttach.TYPE_ATTACHMENT)
			entity.setFormatted(false);
	}

	// ##-------从模板中添加附件-----开始-------##
	public String tplCode;
	public String uid;

	public String loadAttachFromTemplate() {
		Json json = new Json();
		Template t = templateService.loadByCode(tplCode);
		if (t == null) {
			json.put("success", false);
			json.put("msg", getText("flowAttach.fromTemplate.lose"));
			this.json = json.toString();
			return "json";
		}

		try {
			// 声明当前日期时间
			Calendar now = Calendar.getInstance();
			// 文件存储的相对路径（年月），避免超出目录内文件数的限制
			String subFolder = new SimpleDateFormat("yyyyMM").format(now
					.getTime());
			// 上传文件存储的绝对路径
			String appRealDir = Attach.DATA_REAL_PATH + "/"
					+ FlowAttach.DATA_SUB_PATH;
			// 所保存文件所在的目录的绝对路径名
			String realFileDir = appRealDir + "/" + subFolder;
			// 构建文件要保存到的目录
			File _fileDir = new File(realFileDir);
			if (!_fileDir.exists()) {
				logger.warn("mkdir={}", realFileDir);
				_fileDir.mkdirs();
			}
			// 模板文件扩展名
			String extension = StringUtils.getFilenameExtension(t.getPath());
			// 不含路径的文件名
			String fileName = new SimpleDateFormat("yyyyMMddHHmmssSSSS")
					.format(now.getTime()) + "." + extension;
			// 所保存文件的绝对路径名
			String realFilePath = realFileDir + "/" + fileName;
			// 上下文
			SystemContext sc = (SystemContext) this.getContext();
			// 保存附件记录 //标识此附件从模板中添加
			saveAttachLog(t.getSubject(), "FlowAttachFromTemplate", uid, sc,
					extension, t.getSize(), now, FlowAttach.DATA_SUB_PATH + "/"
							+ subFolder + "/" + fileName, true);
			// 直接复制附件
			if (logger.isInfoEnabled())
				logger.info("pure copy file");
			FileCopyUtils.copy(t.getInputStream(), new FileOutputStream(
					realFilePath));

			json.put("path", subFolder + '/' + fileName);
			json.put("success", true);
			json.put("msg", getText("flowAttach.success"));
			this.json = json.toString();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		return "json";
	}

	// 保存一个附件记录
	private void saveAttachLog(String localFile, String ptype, String puid,
			SystemContext context, String extend, long size, Calendar now,
			String path, boolean absolute) {
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

	// 就在流程实例名称
	public String loadProcInstName() {
		Json json = new Json();
		json.put("name", flowAttachService.getProcInstName(pid));
		this.json = json.toString();
		return "json";
	}

	// 加载一个附件信息
	public String loadOneAttach4id() {
		Json json = new Json();
		FlowAttach fa = flowAttachService.load(this.getId());
		if (fa == null) {
			json.put("success", false);
			json.put("msg", "flowAttach.lose");
		} else {
			json.put("success", true);
			json.put("subject", fa.getSubject());
			json.put("ext", fa.getExt());
			json.put("path", fa.getPath());
			json.put("uid", fa.getUid());
			json.put("formatted", fa.getFormatted());
		}
		this.json = json.toString();
		return "json";
	}
	
	public String fromTid;//从哪个任务
	public String fromTids;//从哪个任务，多个请用逗号链接

	/**
	 * 将任务的附件复制到另外一个任务中
	 * 
	 * @return 
	 * @throws Exception
	 */
	public String copy2Task() throws Exception{
		//声明变量
		JSONObject json = new JSONObject();
		List<FlowAttach> attachs = null;
		String taskNames="";
		HistoricTaskInstance fromTask;
		HistoricTaskInstance task;
		//验证
		if(this.tid == null || this.tid.equals("")){
			throw new CoreException("must set tid!");
		}
		task = this.historyService.createHistoricTaskInstanceQuery().taskId(this.tid).singleResult();
		if (task == null) {
			throw new CoreException("can't find taskHistory: tid=" + this.tid);
		}
		if(this.fromTid!=null && this.fromTid.length()>0){
			 fromTask = this.historyService.createHistoricTaskInstanceQuery().taskId(this.fromTid).singleResult();
			if ( fromTask == null) {
				throw new CoreException("can't find taskHistory: fromTid=" + this.fromTid);
			}
			taskNames+= fromTask.getName();
			attachs =this.flowAttachService.findAttachsByTask(new String[]{this.fromTid});
		}else if(this.fromTids!=null && this.fromTids.length()>0){
			for(String _tid:fromTids.split(",")){
				 fromTask = this.historyService.createHistoricTaskInstanceQuery().taskId(_tid).singleResult();
				if ( fromTask == null) {
					throw new CoreException("can't find taskHistory: fromTid=" + _tid);
				}
				taskNames+=(taskNames.length()>0?"、":"")+ fromTask.getName();
			}
			attachs =this.flowAttachService.findAttachsByTask(this.fromTids.split(","));
		}else{
			throw new CoreException("must set tid!");
		}
		
		//没找到附件
		if(attachs == null){
			json.put("success", false);
			json.put("msg", "获取失败");
			this.json = json.toString();
			return "json";
		}
		
		Calendar now = Calendar.getInstance();
		// 所保存文件存储的相对路径（年月），避免超出目录内文件数的限制
		String subFolder = new SimpleDateFormat("yyyyMM").format(now
				.getTime());
		// 所保存文件存储的绝对路径
		String appRealDir = Attach.DATA_REAL_PATH + File.separator 
				+ FlowAttach.DATA_SUB_PATH;
		// 所保存文件所在的目录的绝对路径名
		String realFileDir = appRealDir + File.separator + subFolder;
		
		// 不含路径的文件名
		String fileName;
		// 所保存文件的绝对路径名
		String realFilePath;
		// 上下文
		SystemContext sc = (SystemContext) this.getContext();
		ActorHistory h = sc.getUserHistory();
		
		JSONArray ja = new JSONArray();
		JSONObject jo;
		//已经保存的新任务附件
		FlowAttach tofa;

		Set<TemplateParam> paramSet;
		
		int index=0;

		//复制附件
		for(FlowAttach fa:attachs){
			//再次加载当前时间
			now = Calendar.getInstance();
			
			// 构建文件要保存到的目录
			File _fileDir = new File(realFileDir);
			if (!_fileDir.exists()) {
				logger.warn("mkdir={}", realFileDir);
				_fileDir.mkdirs();
			}
			// 不含路径的文件名
			fileName = new SimpleDateFormat("yyyyMMddHHmmssSSSS")
					.format(now.getTime())+(index++) + "." + fa.getExt();
			// 所保存文件的绝对路径名
			realFilePath = realFileDir + File.separator + fileName;
		
			// 直接复制附件
			if (logger.isInfoEnabled())
				logger.info("pure copy file");
			FileCopyUtils.copy(new FileInputStream(
					appRealDir + File.separator + fa.getPath())
					, new FileOutputStream(realFilePath));
			
			tofa = new FlowAttach();
			tofa.setCommon(fa.isCommon());
			tofa.setDesc(fa.getDesc());
			tofa.setExt(fa.getExt());
			tofa.setFormatted(fa.getFormatted());
			tofa.setPid(task.getProcessInstanceId());
			tofa.setSize(fa.getSize());
			tofa.setSubject(fa.getSubject());
			tofa.setType(fa.getType());
			tofa.setUid(this.getIdGeneratorService().next(FlowAttach.ATTACH_TYPE));
			tofa.setTid(this.tid);
			tofa.setFileDate(now);
			tofa.setAuthor(h);
			tofa.setModifiedDate(now);
			tofa.setModifier(h);
			tofa.setPath(subFolder+ File.separator +fileName);
			
			if(fa.getParams()==null||fa.getParams().size()==0){
				tofa.setParams(null);
			}else{
				paramSet = new LinkedHashSet<TemplateParam>();
				for(TemplateParam p : fa.getParams()){
					paramSet.add(p);
				}
				tofa.setParams(paramSet);
			}
			
			tofa=this.flowAttachService.save(tofa);
			jo = new JSONObject();
			jo.put("id", tofa.getId());
			jo.put("uid", tofa.getUid());
			jo.put("subject", tofa.getSubject());
			jo.put("size", tofa.getSize());
			jo.put("path", tofa.getPath());
			jo.put("formatted", fa.getFormatted());
			jo.put("author", tofa.getAuthor().getName());
			jo.put("fileDate", DateUtils.formatCalendar2Second(tofa.getFileDate()));
			ja.put(jo);
		}
		
		//生成日志
		ExcutionLog log=new ExcutionLog();
		log.setFileDate(Calendar.getInstance());
		log.setAuthorId(h.getId());
		log.setAuthorCode(h.getCode());
		log.setAuthorName(h.getName());
		log.setListener(FlowAttachAction.class.getName());
		log.setType(ExcutionLog.TYPE_PROCESS_SYNC_INFO);
		log.setProcessInstanceId(task.getProcessInstanceId());
		log.setExcutionId(task.getId());
		log.setExcutionCode(task.getTaskDefinitionKey());
		log.setExcutionName(task.getName());
		log.setDescription("同步"+taskNames+"任务的附件，");
		this.excutionLogService.save(log);
		
		json.put("success", true);
		json.put("msg", "获取成功");
		json.put("attachs", ja);
		
		this.json=json.toString();
		return "json";
	}

	/**
	 * <p>将某流程的附件复制到一个任务中</p>
	 * <p>必须要有 pid，tid，type变量</p>
	 * <ul>
	 *     <li>pid 流程实例Id，格式为：欲查找的流程实例Id + , +任务的流程实例Id。</li>
	 *     <li>tid 子流程所在的任务Id</li>
	 *     <li>type 附件的类型</li>
	 * </ul>
	 *
	 * @return json
	 */
	public String copyProcessAttachsToTask() throws JSONException, IOException {
		JSONObject json = new JSONObject();
		//声明变量
		List<FlowAttach> attachs = null;

		//验证
		if((this.pid == null || this.tid == null || this.type == 0) || ("".equals(this.tid.trim()) || "".equals(this.pid.trim()))){
			throw new CoreException("必须设置流程实例Id!，任务Id，附件类型！");
		}

		// 获得欲查找的流程实例Id，任务所在的流程实例Id
		String[] pids = this.pid.split(",");
		String findPid = pids[0];
		String taskPid = (pids.length > 1) ? pids[1] : null;

		attachs =this.flowAttachService.findAttachsByProcess(findPid, true);
		attachs = filterAttaches(attachs);// 过滤附件类型

		//没找到附件
		if(attachs == null || (attachs != null && attachs.size() == 0)){
			json.put("success", false);
			json.put("msg", "没有找到附件");
			this.json = json.toString();
			return "json";
		}

		Calendar now = Calendar.getInstance();
		// 所保存文件存储的相对路径（年月），避免超出目录内文件数的限制
		String subFolder = new SimpleDateFormat("yyyyMM").format(now.getTime());
		// 所保存文件存储的绝对路径
		String appRealDir = Attach.DATA_REAL_PATH + File.separator + FlowAttach.DATA_SUB_PATH;
		// 所保存文件所在的目录的绝对路径名
		String realFileDir = appRealDir + File.separator + subFolder;

		// 不含路径的文件名
		String fileName;
		// 所保存文件的绝对路径名
		String realFilePath;
		// 上下文
		SystemContext sc = (SystemContext) this.getContext();
		ActorHistory h = sc.getUserHistory();

		//已经保存的新任务附件
		List<FlowAttach> tofaList = new ArrayList<>();
		FlowAttach tofa;

		Set<TemplateParam> paramSet;

		int index=0;

		//复制附件
		for(FlowAttach fa:attachs) {
			//再次加载当前时间
			now = Calendar.getInstance();

			// 构建文件要保存到的目录
			File _fileDir = new File(realFileDir);
			if (!_fileDir.exists()) {
				logger.warn("mkdir={}", realFileDir);
				_fileDir.mkdirs();
			}
			// 不含路径的文件名
			fileName = new SimpleDateFormat("yyyyMMddHHmmssSSSS")
					.format(now.getTime())+(index++) + "." + fa.getExt();
			// 所保存文件的绝对路径名
			realFilePath = realFileDir + File.separator + fileName;

			// 直接复制附件
			if (logger.isInfoEnabled())
				logger.info("pure copy file");
			FileCopyUtils.copy(new FileInputStream(
					appRealDir + File.separator + fa.getPath())
					, new FileOutputStream(realFilePath));

			tofa = new FlowAttach();
			tofa.setCommon(fa.isCommon());
			tofa.setDesc(fa.getDesc());
			tofa.setExt(fa.getExt());
			tofa.setFormatted(fa.getFormatted());
			tofa.setPid((taskPid != null) ? taskPid : findPid);// 设置附件所属的流程实例
			tofa.setSize(fa.getSize());
			tofa.setSubject(fa.getSubject());
			tofa.setType(this.type);
			tofa.setUid(this.getIdGeneratorService().next(FlowAttach.ATTACH_TYPE));
			tofa.setTid(this.tid);
			tofa.setFileDate(now);
			tofa.setAuthor(h);
			tofa.setModifiedDate(now);
			tofa.setModifier(h);
			tofa.setPath(subFolder+ File.separator +fileName);

			if(fa.getParams()==null||fa.getParams().size()==0){
				tofa.setParams(null);
			}else{
				paramSet = new LinkedHashSet();
				for(TemplateParam p : fa.getParams()){
					paramSet.add(p);
				}
				tofa.setParams(paramSet);
			}

			this.flowAttachService.save(tofa);
			tofaList.add(tofa);
		}

		//生成日志
//		ExcutionLog log=new ExcutionLog();
//		log.setFileDate(Calendar.getInstance());
//		log.setAuthorId(h.getId());
//		log.setAuthorCode(h.getCode());
//		log.setAuthorName(h.getName());
//		log.setListener(FlowAttachAction.class.getName());
//		log.setType(ExcutionLog.TYPE_PROCESS_SYNC_INFO);
//		this.excutionLogService.save(log);

		// 返回前台数据
		JSONArray flowAttachs = new JSONArray();
		for (FlowAttach f : tofaList) {
			JSONObject flowAttach = new JSONObject();
			flowAttach.put("id", f.getId());
			flowAttach.put("uid", f.getUid());
			flowAttach.put("subject", f.getSubject());
			flowAttach.put("size", f.getSize());
			flowAttach.put("path", f.getPath());
			flowAttach.put("formatted", f.getFormatted());
			flowAttach.put("author", f.getAuthor().getName());
			flowAttach.put("fileDate", DateUtils.formatCalendar2Second(f.getFileDate()));
			flowAttachs.put(flowAttach);
		}

		json.put("success", true);
		json.put("msg", "复制成功");
		json.put("flowAttachs", flowAttachs.toString());

		this.json=json.toString();
		return "json";
	}

	/**
	 * 过滤附件信息，去除公共附件，和类型为意见
	 *
	 * @param attachs 附件集合
	 * @return 过滤公共附件，和类型为意见后的附件集合
	 */
	private List<FlowAttach> filterAttaches(List<FlowAttach> attachs) {
		List<FlowAttach> tempAttachs = new ArrayList<>();
		// 过滤附件集合包含的全局附件与子流程自身的附件
		for (FlowAttach attach : attachs) {
			if (!attach.isCommon() && attach.getType() == FlowAttach.TYPE_ATTACHMENT) {
				tempAttachs.add(attach);
			}
		}
		attachs = tempAttachs;
		return attachs;
	}
}
