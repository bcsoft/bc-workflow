package cn.bc.workflow.todo.web.struts2;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.acl.domain.AccessActor;
import cn.bc.acl.service.AccessService;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.InCondition;
import cn.bc.core.query.condition.impl.OrCondition;
import cn.bc.core.query.condition.impl.QlCondition;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;
import cn.bc.workflow.deploy.domain.Deploy;

/**
 * 部门的待办监控视图Action
 * 
 * @author lbj
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class TodoGroupsAction extends TodoManagesAction {
	private static final long serialVersionUID = 1L;
	private List<Actor> ownActors;//当前用户拥有的指定岗位对应的上组织下的对应用户
	private List<Actor> ownGroups;//ownActors 拥有的岗位
	private Actor actor;//当前用户
	
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
	
	public boolean isGroupControl() {
		// 部门监控
		SystemContext context = (SystemContext) this.getContext();
		return context
				.hasAnyRole(getText("key.role.bc.workflow.group"));
	}
	
	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();

		// 查看按钮
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.todoView.open"));
				
		tb.addButton(new ToolbarButton().setIcon("ui-icon-person")
				.setText(getText("label.delegate.task"))
				.setClick("bc.todoView.delegateTask"));

		tb.addButton(new ToolbarButton().setIcon("ui-icon-flag")
				.setText(getText("label.assign.task"))
				.setClick("bc.todoView.assignTask"));


		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(getStatus(),
				"status", 2, getText("title.click2changeSearchStatus")));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}

	@Override
	protected String getFormActionName() {
		return "group";
	}

	@Override
	protected Condition getGridSpecalCondition() {
		AndCondition ac=(AndCondition) super.getGridSpecalCondition();
		this.initActor();
		this.initOwnActors();
		this.initOwnGroups();
		
		InCondition ownActors_in=this.getOwnActorsCondition();
		QlCondition ownGroups_ql=this.getOwnGroupsCondition();
		QlCondition deploy_ql=this.getDeployAccessControlCondition();
		QlCondition pi_ql=this.getProcessInstanceAccessControlCondition();
		
		OrCondition or=new OrCondition();
		
		if(ownActors_in!=null)or.add(ownActors_in);
		if(ownGroups_ql!=null)or.add(ownGroups_ql);
		if(deploy_ql!=null)or.add(deploy_ql);
		if(pi_ql!=null)or.add(pi_ql);
		
		if(or.isEmpty()){
			ac.add(new QlCondition("false"));
		}else{
			ac.add(or.setAddBracket(true));
		}
		
		return ac;
	}

	private void initActor(){
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		//当前用户
		this.actor=context.getUser();
	}
	
	/*初始化ownActors*/
	private void initOwnActors(){
		if(!this.isGroupControl())return;
		
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		//当前用户
		Actor actor=context.getUser();
		//用户已拥有的岗位
		List<Actor> ownedGroups=this.actorService.findMaster(actor.getId(),
				new Integer[] { ActorRelation.TYPE_BELONG },
				new Integer[] { Actor.TYPE_GROUP });
		if(ownedGroups==null||ownedGroups.size()==0)return;
		
		//部门领导的岗位
		List<Actor> leaderGroups=new ArrayList<Actor>();
		//判断用户拥有的岗位名称是否为指定的部门领导岗位名称
		for(Actor a:ownedGroups){
			if(this.getText("flow.group.leaderDepartmentGroupNames").indexOf(a.getName())!=-1)
				leaderGroups.add(a);
		}
		if(leaderGroups.size()==0)return;
		
		//部门领导岗位对应的上级组织但不包括“宝城”
		List<Actor> leaderUppers=new ArrayList<Actor>();
		Actor leaderUpper;
		for(Actor a:leaderGroups){
			leaderUpper=this.actorService.loadBelong(a.getId(), 
					new Integer[] { Actor.TYPE_DEPARTMENT,Actor.TYPE_UNIT });
			if(!leaderUpper.getCode().equals("baochengzongbu")){
				leaderUppers.add(leaderUpper);
			}	
		}
		if(leaderUppers.size()==0)return;

		//上级组织拥有的用户
		List<Actor> _ownActors=null;
		List<Actor> actors;
		for(Actor a:leaderUppers){
			actors=this.actorService.findFollower(a.getId(), 
					new Integer[] { ActorRelation.TYPE_BELONG },
					new Integer[] { Actor.TYPE_USER});
			
			if(actors==null)continue;
			if(_ownActors==null)_ownActors=new ArrayList<Actor>();
			
			for(Actor _a:actors){
				if(!_ownActors.contains(_a))_ownActors.add(_a);
				
			}
		}
		
		this.ownActors=_ownActors;
	}
	
	/*初始化*/
	private void initOwnGroups(){
		if(!this.isGroupControl())return;
		if(this.ownActors==null)return;
		
		List<Actor> _ownGroups = null;
		//声明每一个用户拥有的岗位
		List<Actor> _userOwnGroups;
		
		//获取每一个用户拥有的岗位加入到集合中
		for(Actor a: this.ownActors){
			_userOwnGroups=this.actorService.findMaster(a.getId(),
					new Integer[] { ActorRelation.TYPE_BELONG },
					new Integer[] { Actor.TYPE_GROUP });
			
			if(_userOwnGroups==null)continue;
				
			if(_ownGroups==null)_ownGroups=new ArrayList<Actor>();
			
			for(Actor _a: _userOwnGroups){
				if(!_ownGroups.contains(_a))_ownGroups.add(_a);
			}	
			
		}
		
		this.ownGroups=_ownGroups;
	}

	private QlCondition getOwnGroupsCondition() {
		if(this.ownGroups==null)return null;
		
		String sql="exists(select 1 from act_ru_identitylink ogc_a where ogc_a.task_id_ = a.id_ and";
			   sql+=" ogc_a.group_id_ in(";
			   
			   for(int i=0;i<ownGroups.size();i++){
					if(i>0)sql+=",";
					
					sql+="'"+ownGroups.get(i).getCode()+"'";
				}
			   sql+="))";
		return new QlCondition(sql);
	}

	private QlCondition getProcessInstanceAccessControlCondition() {
		//流程部署的监控
		List<AccessActor> aa4list= this.accessService.find(this.actor, "ProcessInstance");
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
		
		String sql="exists(select 1 from act_ru_task pi_a";
		sql+=" where pi_a.id_=a.id_ and pi_a.proc_inst_id_ in(";
		
		for(int i=0;i<pIds.size();i++){
			if(i>0)sql+=",";
			
			sql+="'"+pIds.get(i)+"'";
		}
		sql+="))";
		
		return new QlCondition(sql);
	}

	private QlCondition getDeployAccessControlCondition() {
		//流程部署的监控
		List<AccessActor> aa4list= this.accessService.find(this.actor, Deploy.class.getSimpleName());
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
		
		String sql="exists(select 1 from act_ru_task dc_a";
		sql+=" inner join act_re_procdef dc_b on dc_b.id_=dc_a.proc_def_id_";
		sql+=" inner join bc_wf_deploy dc_c on dc_c.deployment_id=dc_b.deployment_id_";
		sql+=" where dc_a.id_=a.id_ and dc_c.id in(";
		
		for(int i=0;i<deployIds.size();i++){
			if(i>0)sql+=",";
			
			sql+=deployIds.get(i);
		}
		sql+="))";
		
		return new QlCondition(sql);
	}
	
	/*获取属于当前用户拥有的指定岗位对应的上组织下的对应用户的条件*/
	private InCondition getOwnActorsCondition(){
		if(this.ownActors==null)return null;
		
		List<String> ownActorCodes=new ArrayList<String>();
		for(Actor a:ownActors){
			ownActorCodes.add(a.getCode());
		}
		
		return new InCondition("a.assignee_",ownActorCodes);
	}


}
