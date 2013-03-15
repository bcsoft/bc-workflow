package cn.bc.workflow.historicprocessinstance.web.struts2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.acl.domain.AccessActor;
import cn.bc.acl.service.AccessService;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.OrCondition;
import cn.bc.core.query.condition.impl.QlCondition;
import cn.bc.core.util.DateUtils;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.web.formater.AbstractFormater;
import cn.bc.web.formater.CalendarFormater;
import cn.bc.web.formater.EntityStatusFormater;
import cn.bc.web.ui.html.grid.Column;
import cn.bc.web.ui.html.grid.HiddenColumn4MapKey;
import cn.bc.web.ui.html.grid.IdColumn4MapKey;
import cn.bc.web.ui.html.grid.TextColumn4MapKey;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;
import cn.bc.workflow.deploy.domain.Deploy;

/**
 * 部门经办流程监控视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class GroupHistoricProcessInstancesAction extends HistoricProcessInstancesAction {
	private static final long serialVersionUID = 1L;
	private ActorService actorService;
	private AccessService accessService;

	@Autowired
	public void setAccessService(AccessService accessService) {
		this.accessService = accessService;
	}
	
	@Autowired
	public void setActorService(ActorService actorService) {
		this.actorService = actorService;
	}
	
	@Override
	protected String[] getGridSearchFields() {
		return new String[] { "b.name_", "b.key_", "c.name",
				"getProcessInstanceSubject(a.proc_inst_id_)" };
	}


	@Override
	protected String getFormActionName() {
		return "groupHistoricProcessInstance";
	}
	
	public boolean isGroupControl() {
		// 部门监控
		SystemContext context = (SystemContext) this.getContext();
		return context
				.hasAnyRole(getText("key.role.bc.workflow.group"));
	}
	
	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		// 查看
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.groupHistoricprocessinstance.open"));
		
		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(),
				"status", 3, getText("title.click2changeSearchStatus")));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}
	
	@Override
	protected String getGridDblRowMethod() {
		return "bc.groupHistoricprocessinstance.open";
	}

	@Override
	protected String getHtmlPageJs() {
		return this.getHtmlPageNamespace() + "/historicprocessinstance/group/view.js";
	}

	@Override
	protected List<Column> getGridColumns() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new IdColumn4MapKey("a.id_", "id"));
		// 状态
		columns.add(new TextColumn4MapKey("", "status",
				getText("flow.instance.status"), 50).setSortable(true)
				.setValueFormater(new EntityStatusFormater(getStatus())));
		// 主题
		columns.add(new TextColumn4MapKey(
				"getProcessInstanceSubject(a.proc_inst_id_)", "subject",
				getText("flow.instance.subject"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		// 流程
		columns.add(new TextColumn4MapKey("b.name_", "procinst_name",
				getText("flow.instance.name"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("", "todo_names",
				getText("flow.instance.todoTask"), 200).setSortable(true)
				.setUseTitleFromLabel(true));
		// 版本号
		columns.add(new TextColumn4MapKey("e.version_", "version",
				getText("flow.instance.version"), 50).setSortable(true)
				.setUseTitleFromLabel(true).setValueFormater(new AbstractFormater<String>() {
					@SuppressWarnings("unchecked")
					@Override
					public String format(Object context, Object value) {
						Map<String, Object> version = (Map<String, Object>) context;
						return version.get("version")+"  ("+version.get("aVersion")+")";
					}
					
				}));
		// 发起人
		columns.add(new TextColumn4MapKey("a.first_", "start_name",
				getText("flow.instance.startName"), 80).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new TextColumn4MapKey("a.start_time_", "start_time",
				getText("flow.instance.startTime"), 150).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		columns.add(new TextColumn4MapKey("a.end_time_", "end_time",
				getText("flow.instance.endTime"), 150).setSortable(true)
				.setUseTitleFromLabel(true)
				.setValueFormater(new CalendarFormater("yyyy-MM-dd HH:mm:ss")));
		columns.add(new TextColumn4MapKey("a.duration_", "duration",
				getText("flow.instance.duration"), 80).setSortable(true)
				.setValueFormater(new AbstractFormater<String>() {
					@SuppressWarnings("unchecked")
					@Override
					public String format(Object context, Object value) {
						Object duration_obj=((Map<String, Object>)context).get("duration");
						if(duration_obj==null)return null;
						return DateUtils.getWasteTime(Long.parseLong(duration_obj.toString()));
					}	
				}));
		columns.add(new TextColumn4MapKey("b.key_", "key",
				getText("flow.instance.key"), 180).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new HiddenColumn4MapKey("procinstid", "procinstid"));
		columns.add(new HiddenColumn4MapKey("status", "status"));
		columns.add(new HiddenColumn4MapKey("accessControlDocType", "accessControlDocType"));
		columns.add(new HiddenColumn4MapKey("accessControlDocName", "accessControlDocName"));
		columns.add(new HiddenColumn4MapKey("deployId", "deployId"));
		return columns;
	}
	
	@Override
	protected Condition getGridSpecalCondition() {
		AndCondition ac = (AndCondition) super.getGridSpecalCondition();
		
		QlCondition ownActors_ql=this.getOwnActorCondition();
		QlCondition deploy_ql=this.getDeployAccessControlCondition();
		QlCondition pi_ql=this.getProcessInstanceAccessControlCondition();
		
		OrCondition or=new OrCondition();
		if(ownActors_ql!=null)or.add(ownActors_ql);
		if(deploy_ql!=null)or.add(deploy_ql);
		if(pi_ql!=null)or.add(pi_ql);
		
		if(or.isEmpty()){
			ac.add(new QlCondition("false"));
		}else{
			ac.add(or.setAddBracket(true));
		}
		return ac;
	}
	
	/*获取属于当前用户拥有的指定岗位对应的上组织下的对应用户的条件*/
	private QlCondition getOwnActorCondition(){
		if(!this.isGroupControl())return null;
		
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		//当前用户
		Actor actor=context.getUser();
		//用户已拥有的岗位
		List<Actor> ownedGroups=this.actorService.findMaster(actor.getId(),
				new Integer[] { ActorRelation.TYPE_BELONG },
				new Integer[] { Actor.TYPE_GROUP });
		if(ownedGroups==null||ownedGroups.size()==0)return null;
		
		//部门领导的岗位
		List<Actor> leaderGroups=null;
		//判断用户拥有的岗位名称是否为指定的部门领导岗位名称
		for(Actor a:ownedGroups){
			if(this.getText("flow.group.leaderDepartmentGroupNames").indexOf(a.getName())!=-1){
				if(leaderGroups==null)leaderGroups=new ArrayList<Actor>();
				
				leaderGroups.add(a);
			}
		}
		if(leaderGroups==null)return null;
		
		//部门领导岗位对应的上级组织但不包括“宝城”
		List<Actor> leaderUppers=null;
		Actor leaderUpper;
		for(Actor a:leaderGroups){
			leaderUpper=this.actorService.loadBelong(a.getId(), 
					new Integer[] { Actor.TYPE_DEPARTMENT,Actor.TYPE_UNIT });
			if(!leaderUpper.getCode().equals("baochengzongbu")){
				if(leaderUppers==null)leaderUppers=new ArrayList<Actor>();
				
				leaderUppers.add(leaderUpper);
			}	
		}
		if(leaderUppers==null)return null;

		//上级组织拥有的用户
		List<Actor> ownActors=new ArrayList<Actor>();
		List<Actor> _ownActors;
		for(Actor a:leaderUppers){
			_ownActors=this.actorService.findFollower(a.getId(), 
					new Integer[] { ActorRelation.TYPE_BELONG },
					new Integer[] { Actor.TYPE_USER});
			if(_ownActors==null)continue;

			for(Actor _a:_ownActors){
				if(!ownActors.contains(_a)){
					ownActors.add(_a);
				}
			}
		}
		
		if(ownActors==null||ownActors.size()==0)return null;
		
		String sql="exists(select 1 from act_hi_taskinst oac_a";
		sql+=" where a.id_=oac_a.proc_inst_id_ and oac_a.end_time_ is not null and oac_a.assignee_ in (";
		
		for(int i=0;i<ownActors.size();i++){
			if(i>0)sql+=",";
			
			sql+="'"+ownActors.get(i).getCode()+"'";
		}
		
		sql+="))";
		
		return new QlCondition(sql);
	}

	//获取可访问属于流程部署的任务的条件
	private QlCondition getDeployAccessControlCondition(){
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		//当前用户
		Actor actor=context.getUser();
		//流程部署的监控
		List<AccessActor> aa4list= this.accessService.findByDocType(actor.getId(), Deploy.class.getSimpleName());
		if(aa4list==null||aa4list.size()==0)return null;
		
		//流程部署的id
		List<String> deployIds=new ArrayList<String>();
		
		for(AccessActor aa :aa4list){
			//先进性权限的判断
			//查阅
			if(AccessActor.ROLE_TRUE.equals(aa.getRole().substring(aa.getRole().length()-1))){
				deployIds.add(aa.getAccessDoc().getDocId());
			}else{
				//编辑
				if(AccessActor.ROLE_TRUE.equals(
						aa.getRole().substring(aa.getRole().length()-2,aa.getRole().length()-1))){
					deployIds.add(aa.getAccessDoc().getDocId());
				}
				
			}
		}
		
		if(deployIds.size()==0)return null;
		
		String sql="exists(select 1 from act_hi_taskinst dc_a";
		sql+=" inner join act_re_procdef dc_b on dc_b.id_=dc_a.proc_def_id_";
		sql+=" inner join bc_wf_deploy dc_c on dc_c.deployment_id=dc_b.deployment_id_";
		sql+=" where a.id_=dc_a.proc_inst_id_ and dc_a.end_time_ is not null and dc_c.id in(";
		
		for(int i=0;i<deployIds.size();i++){
			if(i>0)sql+=",";
			
			sql+=deployIds.get(i);
		}
		sql+="))";
		
		return new QlCondition(sql);
	}
	
	//获取可访问属于流程实例的任务的条件
	private QlCondition getProcessInstanceAccessControlCondition(){
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		//当前用户
		Actor actor=context.getUser();
		//流程部署的监控
		List<AccessActor> aa4list= this.accessService.findByDocType(actor.getId(), "ProcessInstance");
		if(aa4list==null||aa4list.size()==0)return null;
		
		//流程实例的id
		List<String> pIds=new ArrayList<String>();
		
		for(AccessActor aa :aa4list){
			//先进性权限的判断
			//查阅
			if(AccessActor.ROLE_TRUE.equals(aa.getRole().substring(aa.getRole().length()-1))){
				pIds.add(aa.getAccessDoc().getDocId());
			}else{
				//编辑
				if(AccessActor.ROLE_TRUE.equals(
						aa.getRole().substring(aa.getRole().length()-2,aa.getRole().length()-1))){
					pIds.add(aa.getAccessDoc().getDocId());
				}
				
			}
		}
		
		if(pIds.size()==0)return null;
		
		String sql="exists(select 1 from act_hi_taskinst pi_a";
		sql+=" where a.id_=pi_a.proc_inst_id_ and pi_a.end_time_ is not null and pi_a.proc_inst_id_ in(";
		
		for(int i=0;i<pIds.size();i++){
			if(i>0)sql+=",";
			
			sql+="'"+pIds.get(i)+"'";
		}
		sql+="))";
		
		return new QlCondition(sql);
	}
	
}
