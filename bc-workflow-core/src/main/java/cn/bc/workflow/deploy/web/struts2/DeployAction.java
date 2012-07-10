package cn.bc.workflow.deploy.web.struts2;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.struts2.FileEntityAction;
import cn.bc.web.ui.html.page.ButtonOption;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.service.DeployService;

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
	public Map<String, String> statusesValue;

	@Autowired
	public void setDeployService(DeployService deployService) {
		this.deployService = deployService;
		this.setCrudService(deployService);
	}
	@Override
	public boolean isReadonly() {
		// 模板管理员或系统管理员
		SystemContext context = (SystemContext) this.getContext();
		// 配置权限：模板管理员
		return !context.hasAnyRole(getText("key.role.bc.workflow.deploy"),
				getText("key.role.bc.admin"));
	}
	
	@Override
	protected void buildFormPageButtons(PageOption pageOption, boolean editable) {
		if (!this.isReadonly()) {
			if (editable)
				pageOption.addButton(new ButtonOption(getText("label.save"),
						null, "bc.deployForm.save").setId("deploySave"));
		}
	}


	@Override
	protected PageOption buildFormPageOption(boolean editable) {
		return super.buildFormPageOption(editable).setWidth(545)
				.setMinHeight(200).setMinWidth(300).setMaxHeight(800)
				.setHelp("mubanguanli");
	}

	@Override
	protected void afterCreate(Deploy entity) {
		super.afterCreate(entity);
		// 状态正常
		entity.setStatus(Deploy.STATUS_NOT_RELEASE);
		// 默认模板类型为自定义文本
		entity.setType(Deploy.TYPE_XML);

		// uid
		entity.setUid(this.getIdGeneratorService().next(Deploy.ATTACH_TYPE));
	}

	@Override
	protected void afterEdit(Deploy entity) {
		super.afterEdit(entity);
	}

	@Override
	protected void afterOpen(Deploy entity) {
		super.afterOpen(entity);
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
		SystemContext context = getSystyemContext();
		Deploy e = this.getE();
		e.setDeployer(context.getUserHistory());
		e.setDeployDate(Calendar.getInstance());
		// 状态列表
		statusesValue = this.getStatuses();
	}
	
	// 状态键值转换
	private Map<String, String> getStatuses() {
		Map<String, String> statuses = new LinkedHashMap<String, String>();
		statuses.put(String.valueOf(Deploy.STATUS_RELEASED),
				getText("deploy.status.released"));
		statuses.put(String.valueOf(Deploy.STATUS_NOT_RELEASE),
				getText("deploy.status.not.release"));
		statuses.put("", getText("deploy.status.all"));
		return statuses;
	}

	public Long id;// 部署id
	public String code;// 编码
	public String version;// 版本号
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
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

}
