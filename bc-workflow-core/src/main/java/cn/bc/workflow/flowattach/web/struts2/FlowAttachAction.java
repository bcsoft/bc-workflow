package cn.bc.workflow.flowattach.web.struts2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import cn.bc.core.service.CrudService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.struts2.FileEntityAction;
import cn.bc.web.ui.html.page.ButtonOption;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.web.ui.json.Json;
import cn.bc.workflow.flowattach.domain.FlowAttach;


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

	@Autowired
	public void setFlowAttachService(
			@Qualifier(value = "flowAttachService") CrudService<FlowAttach> crudService) {
		this.setCrudService(crudService);
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
			pageOption.addButton(new ButtonOption(getText("label.save"),
					null, "bc.flowattachForm.save").setId("flowattachSave"));
		
	}

	@Override
	protected PageOption buildFormPageOption(boolean editable) {
		FlowAttach e=this.getE();
		PageOption po=super.buildFormPageOption(editable).setWidth(550);
		//根据类型控制窗口高度。
		if(type==FlowAttach.TYPE_ATTACHMENT||e.getType()==FlowAttach.TYPE_ATTACHMENT){
			po.setHeight(235);
		}else if(type==FlowAttach.TYPE_COMMENT||e.getType()==FlowAttach.TYPE_COMMENT){
			po.setHeight(205);
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
	}
	
	
	
	@Override
	public String save() throws Exception {
		super.save();
		FlowAttach e=this.getE();
		Json json=new Json();
		json.put("id", e.getId());
		json.put("size", e.getSize());
		json.put("ext", e.getExt());
		json.put("success", true);
		json.put("msg", getText("form.save.success"));
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
	}

}
