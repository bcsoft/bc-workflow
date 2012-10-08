package cn.bc.workflow.deploy.web.struts2;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.struts2.ServletActionContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;

import cn.bc.BCConstants;
import cn.bc.core.util.DateUtils;
import cn.bc.docs.web.AttachUtils;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.struts2.FileEntityAction;
import cn.bc.template.domain.TemplateParam;
import cn.bc.template.domain.TemplateType;
import cn.bc.template.service.TemplateParamService;
import cn.bc.template.service.TemplateTypeService;
import cn.bc.web.ui.html.page.ButtonOption;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.json.Json;
import cn.bc.web.ui.json.JsonArray;
import cn.bc.web.util.WebUtils;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.domain.DeployResource;
import cn.bc.workflow.deploy.service.DeployService;
import cn.bc.workflow.service.WorkflowService;

/**
 * 流程部署表单Action
 * 
 * @author wis
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class DeployAction extends FileEntityAction<Long, Deploy> {
	private static final long serialVersionUID = 1L;
	private DeployService deployService;
	private ActorService actorService;
	private TemplateTypeService templateTypeService;
	private TemplateParamService templateParamService;
	protected WorkflowService workflowService;

	public Map<String, String> statusesValue;
	public Map<String, String> resourceTypeValues;
	// 模板类型集合
	public List<Map<String, String>> typeList;
	// 模板参数
	public String templateParamIds;

	public String assignUserIds;// 分配的用户id，多个id用逗号连接
	public Set<Actor> ownedUsers;// 已分配的用户
	public String resources; // 资源列表的json字符串
	
	public boolean isDoMaintenance = false;// 是否进行维护操作
	public boolean isDoLevelUp = false;// 是否进行升级操作

	@Autowired
	public void setDeployService(DeployService deployService) {
		this.deployService = deployService;
		this.setCrudService(deployService);
	}

	@Autowired
	public void setActorService(ActorService actorService) {
		this.actorService = actorService;
	}
	
	@Autowired
	public void setTemplateTypeService(TemplateTypeService templateTypeService) {
		this.templateTypeService = templateTypeService;
	}
	
	@Autowired
	public void setTemplateParamService(TemplateParamService templateParamService) {
		this.templateParamService = templateParamService;
	}

	@Autowired
	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;
	}


	@Override
	public boolean isReadonly() {
		// 权限控制：流程部署管理、流程管理或系统管理员
		SystemContext context = (SystemContext) this.getContext();
		return !context.hasAnyRole(getText("key.role.bc.workflow.deploy"),
				getText("key.role.bc.workflow"), getText("key.role.bc.admin"));
	}
	
	@Override
	protected void buildFormPageButtons(PageOption pageOption, boolean editable) {
		pageOption.addButton(new ButtonOption(getText("deploy.showDiagram"),
				null, "bc.deployForm.showDiagram"));
		if (!this.isReadonly()) {//有权限
			if (this.getE().getStatus() != BCConstants.STATUS_DRAFT && !this.getE().isNew()){
				// 非草稿状态下双击
				if (!isDoMaintenance){
					pageOption.addButton(new ButtonOption(
							getText("deploy.op.maintenance"),
							null, "bc.deployForm.doMaintenance"));
					pageOption.addButton(new ButtonOption(
							getText("deploy.op.levelUp"),
							null, "bc.deployForm.doLevelUp"));
				}
			} else {
				pageOption.addButton(new ButtonOption(getText("label.save4Draft"),
						null, "bc.deployForm.save").setId("deploySave"));				
			}
			
			// 维护操作
			if (isDoMaintenance) {
				pageOption.addButton(new ButtonOption(getText("label.save"),
						null, "bc.deployForm.save"));
			}
			
		}
		
//		if (editable)
//		pageOption.addButton(new ButtonOption(getText("label.save"),
//				null, "bc.deployForm.save").setId("deploySave"));
		
	}

	@Override
	protected PageOption buildFormPageOption(boolean editable) {
		PageOption pageOption = new PageOption().setWidth(820)
				.setMinHeight(200).setMinWidth(300).setHeight(400);
		
		// 只有可编辑表单才按权限配置，其它情况一律配置为只读状态
		boolean readonly = this.isReadonly();
		
		if (editable && !readonly) {
			pageOption.put("readonly", readonly);
		} else {
			pageOption.put("readonly", true);
		}
		
		// 添加按钮
		buildFormPageButtons(pageOption, editable);
		
		return pageOption;
	}

	@Override
	protected void afterCreate(Deploy entity) {
		super.afterCreate(entity);
		
		if(!isDoLevelUp){
			// 默认模板类型为自定义文本
			entity.setType(Deploy.TYPE_XML);
			// 设置版本号
			entity.setVersion("1.0");
			// 状态草稿
			entity.setStatus(BCConstants.STATUS_DRAFT);
			// uid
			entity.setUid(this.getIdGeneratorService().next(Deploy.ATTACH_TYPE));
			
		}else{//升级操作处理
			//复制新的流程部署
			Deploy newDeploy = this.deployService.doCopyDeploy(id);
			this.setE(newDeploy);
			
			// 加载已分配的用户
			this.ownedUsers = newDeploy.getUsers();
			
			this.typeList = this.templateTypeService.findTemplateTypeOption(true);
			
		}
		
	}

	@Override
	protected void afterEdit(Deploy entity) {
		super.afterEdit(entity);

		// 加载已分配的用户
		this.ownedUsers = entity.getUsers();
		
		this.typeList = this.templateTypeService.findTemplateTypeOption(true);
		
		// 如果非草稿状态下打开,版本号 +0.1
		if (entity.getStatus() != BCConstants.STATUS_DRAFT
				&& entity.getVersion() != null) {
			String[] version = entity.getVersion().split("\\.");
			String major = version[0];
			int minor = Integer.parseInt(version[1]);
			String result = major + "." + (minor+1);
			entity.setVersion(result);
		}
	}

	@Override
	protected void afterOpen(Deploy entity) {
		super.afterOpen(entity);

		// 加载已分配的用户
		this.ownedUsers = entity.getUsers();
		
		this.typeList = this.templateTypeService.findTemplateTypeOption(true);
		
	}

	@Override
	protected void beforeSave(Deploy entity) {
		super.beforeSave(entity);

		// 处理发布人
		if (getE().getDeployer() != null && getE().getDeployer().isNew())
			getE().setDeployer(null);

		// 处理分配的用户
		Long[] userIds = null;
		if (this.assignUserIds != null && this.assignUserIds.length() > 0) {
			String[] uIds = this.assignUserIds.split(",");
			userIds = new Long[uIds.length];
			for (int i = 0; i < uIds.length; i++) {
				userIds[i] = new Long(uIds[i]);
			}
		}

		if (userIds != null && userIds.length > 0) {
			Set<Actor> users = null;
			Actor user = null;
			for (int i = 0; i < userIds.length; i++) {
				if (i == 0) {
					users = new HashSet<Actor>();
				}
				user = this.actorService.load(userIds[i]);
				users.add(user);
			}

			if (this.getE().getUsers() != null) {
				this.getE().getUsers().clear();
				this.getE().getUsers().addAll(users);
			} else {
				this.getE().setUsers(users);
			}

		}
		
		//插入资源列表
		try{
			Set<DeployResource> resources = null;
			if(this.resources != null && this.resources.length() > 0){
				resources = new LinkedHashSet<DeployResource>();
				DeployResource resource;
				TemplateType templateType;
				JSONArray jsons = new JSONArray(this.resources);
				JSONObject json;
				for(int i=0;i<jsons.length();i++){
					json = jsons.getJSONObject(i);
					//加载流程资源
					resource = new DeployResource();
					//加载模板类型
					templateType = this.templateTypeService.load(json
							.getLong("type"));
					
					if(json.has("id"))
						resource.setId(json.getLong("id"));
					
					resource.setUid(json.getString("uid"));
					//resource.setType(json.getInt("type"));
					resource.setCode(json.getString("code"));
					resource.setSubject(json.getString("subject"));
					resource.setPath(json.getString("path"));
					resource.setSize(json.getLong("size"));
					resource.setSource(json.getString("source"));
					resource.setFormatted(json.getBoolean("formatted"));
					
					resource.setDeploy(entity); //设置部署对象
					resource.setTemplateType(templateType); //设置模板类型对象
					String idstr = json.getString("paramIds");
					
					
					//设置流程资源里的参数
					Long[] templateParamIds = null;
					if (idstr != null && idstr.length() > 0) {
						String[] tpIds = idstr.split(",");
						templateParamIds = new Long[tpIds.length];
						for (int j = 0; j < tpIds.length; j++){ 
							templateParamIds[j] = new Long(tpIds[j]);
						}
					}
					
					if(templateParamIds!=null&&templateParamIds.length>0){
						Set<TemplateParam> params=null;
						TemplateParam templateParam=null;
						for(int j=0;j<templateParamIds.length;j++){
							if(j==0)
								params=new HashSet<TemplateParam>();
							templateParam=this.templateParamService.load(templateParamIds[j]);
							params.add(templateParam);
						}
						if(resource.getParams()!=null){
							resource.getParams().clear();
							resource.getParams().addAll(params);
						}else{
							resource.setParams(params);
						}
					}

					resources.add(resource);
				}
			}
			if(this.getE().getResources() != null){
				this.getE().getResources().clear();
				this.getE().getResources().addAll(resources);
			}else{
				this.getE().setResources(resources);
			}

		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
			try {
				throw e;
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
		
	}

	@Override
	public String save() throws Exception {
		Deploy deploy = this.getE();
		// 设置保存文件大小 获取文件大小
		deploy.setSize(deploy.getSizeEx());

		this.beforeSave(deploy);
		this.deployService.save(deploy);
		this.afterSave(deploy);
		return "saveSuccess";
	}

	@Override
	protected void initForm(boolean editable) throws Exception {
		super.initForm(editable);
		// 状态列表
		statusesValue = this.getStatuses();
		//resourceTypeValues = this.getResourceType();
	}

	// 状态键值转换
	private Map<String, String> getStatuses() {
		Map<String, String> statuses = new LinkedHashMap<String, String>();
		statuses.put(String.valueOf(BCConstants.STATUS_DRAFT),
				getText("deploy.status.draft"));
		statuses.put(String.valueOf(Deploy.STATUS_USING),
				getText("deploy.status.using"));
		statuses.put(String.valueOf(Deploy.STATUS_STOPPED),
				getText("deploy.status.stopped"));
		statuses.put("", getText("deploy.status.all"));
		return statuses;
	}
	
//	/**
//	 * 资源类型列表：JS|FORM|PNG
//	 * 
//	 * @return
//	 */
//	protected Map<String, String> getResourceType() {
//		Map<String, String> statuses = new LinkedHashMap<String, String>();
//		statuses.put(String.valueOf(DeployResource.TYPE_JS),
//				"js");
//		statuses.put(String.valueOf(DeployResource.TYPE_FORM),
//				"form");
//		statuses.put(String.valueOf(DeployResource.TYPE_PNG),
//				"png");
//		return statuses;
//	}


	public Long id;// 部署id
	public String code;// 编码
	public String version;// 版本号

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	// 添加一项资源设置UID
	public String setInputUid(){
		Json json = new Json();
		json.put("uid", this.getIdGeneratorService().next(
				DeployResource.ATTACH_TYPE));
		this.json = json.toString();
		return "json";
	}
	
	// 加载模板类型列表
	public String loadTemplateTypeList(){
		JsonArray jsonArray = new JsonArray();
		Json json = null;
		List<Map<String, String>> listMap = null;
		listMap = this.templateTypeService.findTemplateTypeOption(true);
		for (Map<String, String> map : listMap) {
			if (map != null) {
				json = new Json();
				json.put("key", map.get("key").trim());
				json.put("value", map.get("value").trim());
				jsonArray.add(json);
			}
		}
		this.json =jsonArray.toString();
		return "json";
	}

	// 检查编码与版本号唯一
	public String isUniqueCodeAndVersion() {
		Json json = new Json();
		boolean flag = this.deployService.isUniqueCodeAndVersion(this.id,
				this.code, this.version);
		if (flag) {
			json.put("result", getText("deploy.save.code"));
		} else {
			json.put("result", "save");
		}
		this.json = json.toString();
		return "json";
	}
	
	public String codes;// 编码列表
	// 检测资源编码是否唯一和检测扩展是否合法
	public String isUniqueResourceCodeAndExtCheck(){
		Json json = new Json();
		ArrayList<Object> list = this.deployService
				.isUniqueResourceCodeAndExtCheck(this.id, this.codes);
		if(null != list && list.size() > 0){
			String msg="[";
			int i = 0;
			for(Object code : list){
				msg += code.toString();
				if(i+1 < list.size()){
					msg += ",";
				}
				if(i+1 == list.size()){
					msg += "]";
				}
				i++;
			}
			json.put("result", msg);
		}
		this.json = json.toString();
		return "json";
	}

	public String filename;// 下载文件的文件名
	public String contentType;// 下载文件的大小
	public long contentLength;
	public InputStream inputStream;
	public String n;// [可选]指定下载文件的文件名
	public String did;// 流程发布ID(表ACT_RE_DEPLOYMENT的主键值)

	/**
	 * 查看流程图
	 * 
	 * @return
	 * @throws Exception
	 */
	public String diagram() throws Exception {
		Assert.hasText(id+"", "need id argument");
		Date startTime = new Date();

		// 下载文件的扩展名
		String extension = "png";

		// 下载文件的文件名
		if (this.n == null || this.n.length() == 0)
			this.n = "deployment" + id + "." + extension;

		// debug
		if (logger.isDebugEnabled()) {
			logger.debug("n=" + n);
			logger.debug("extension=" + extension);
		}

		// 获取资源流
		this.inputStream = workflowService.getDiagram(this.id);
		if (logger.isDebugEnabled())
			logger.debug("inputStream=" + this.inputStream.getClass());
		this.contentLength = this.inputStream.available();// 资源大小

		// 设置下载文件的参数
		this.contentType = AttachUtils.getContentType(extension);
		this.filename = WebUtils.encodeFileName(
				ServletActionContext.getRequest(), this.n);

		if (logger.isDebugEnabled()) {
			logger.debug("wasteTime:" + DateUtils.getWasteTime(startTime));
		}
		return SUCCESS;
	}
}
