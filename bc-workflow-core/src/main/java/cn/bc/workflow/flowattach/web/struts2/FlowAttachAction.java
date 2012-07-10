package cn.bc.workflow.flowattach.web.struts2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import cn.bc.core.service.CrudService;
import cn.bc.core.util.DateUtils;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.service.ActorHistoryService;
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
	private ActorHistoryService actorHistroyService;
	
	@Autowired
	public void setActorHistroyService(ActorHistoryService actorHistroyService) {
		this.actorHistroyService = actorHistroyService;
	}

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
			pageOption.addButton(new ButtonOption(getText("label.ok"),
					null, "bc.flowattachForm.save").setId("flowattachSave"));
		
	}

	@Override
	protected PageOption buildFormPageOption(boolean editable) {
		FlowAttach e=this.getE();
		PageOption po=super.buildFormPageOption(editable).setWidth(500);
		//根据类型控制窗口高度。
		if(type==FlowAttach.TYPE_ATTACHMENT||e.getType()==FlowAttach.TYPE_ATTACHMENT){
			po.setHeight(250);
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
	}

	@Override
	protected void afterOpen(FlowAttach entity) {
		super.afterOpen(entity);
		PageOption po=buildFormPageOption(false);
		if(entity.getType()==FlowAttach.TYPE_ATTACHMENT){
			po.setHeight(210);
		}else{
			po.setHeight(200);
		}
		this.formPageOption=po;
	}

	@Override
	public String save() throws Exception {
		Json json=new Json();
		//判断是否为新建保存
		boolean isNew=this.getE().isNew();
		super.save();
		FlowAttach e=this.getE();
		json.put("id", e.getId());
		json.put("size", e.getSize());
		json.put("ext", e.getExt());
		json.put("success", true);
		json.put("msg", getText("form.save.success"));
		ActorHistory ah=actorHistroyService.load(e.getAuthor().getId());
		json.put("author",ah.getName());
		json.put("fileDate",DateUtils.formatCalendar2Second(e.getFileDate()));
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
	}

}
