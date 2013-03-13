package cn.bc.workflow.historictaskinstance.web.struts2;

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
import cn.bc.core.query.condition.impl.IsNotNullCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
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
 * 部门经办监控视图Action
 * 
 * @author lbj
 * 
 */

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class GroupHistoricTaskInstancesAction extends
	HistoricTaskInstancesAction {
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
	protected String getFormActionName() {
		return "groupHistoricTaskInstance" ;
	}


	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		// 查看
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.historicTaskInstanceSelectView.open"));
		tb.addButton(new ToolbarButton().setIcon("ui-icon-search")
				.setText(getText("flow.task.flow"))
				.setClick("bc.groupHistoricTaskInstanceSelectView.viewflow"));
		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}
	
	@Override
	protected String getHtmlPageJs() {
		return super.getHtmlPageJs()+","
				+this.getHtmlPageNamespace() + "/historictaskinstance/group/view.js";
	}


	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();
		
		InCondition ownActorCodes_ic=this.getOwnActorCondition();
		QlCondition deploy_qc=this.getDeployAccessControlCondition();
		QlCondition pi_qc=this.getProcessInstanceAccessControlCondition();
		OrCondition or=new OrCondition();
		
		if(deploy_qc!=null)or.add(deploy_qc);
		if(pi_qc!=null)or.add(pi_qc);
		if(or.isEmpty()){
			ac.add(ownActorCodes_ic);
		}else{
			ac.add(or.add(ownActorCodes_ic).setAddBracket(true));
		}
		
		// 结束时间不能为空
		ac.add(new IsNotNullCondition("a.end_time_"));
		ac.add(new IsNullCondition("h.parent_id_"));
		
		return ac;
	}
	
	/*获取属于当前用户拥有的指定岗位对应的上组织下的对应用户的条件*/
	private InCondition getOwnActorCondition(){
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
		List<Actor> leaderGroups=new ArrayList<Actor>();
		//判断用户拥有的岗位名称是否为指定的部门领导岗位名称
		for(Actor a:ownedGroups){
			if(this.getText("flow.group.leaderDepartmentGroupNames").indexOf(a.getName())!=-1)
				leaderGroups.add(a);
		}
		if(leaderGroups.size()==0)return null;
		
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
		if(leaderUppers.size()==0)return null;

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
		
		List<String> ownActorCodes=new ArrayList<String>();
		if(ownActors==null||ownActors.size()==0){
			ownActorCodes.add("''");
		}else{
			for(Actor a:ownActors){
				ownActorCodes.add(a.getCode());
			}
		}
		
		return new InCondition("a.assignee_",ownActorCodes);
	}

	//获取可访问属于流程部署的任务的条件
	private QlCondition getDeployAccessControlCondition(){
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		//当前用户
		Actor actor=context.getUser();
		//流程部署的监控
		List<AccessActor> aa4list= this.accessService.find(actor, Deploy.class.getSimpleName());
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
		sql+=" where dc_a.id_=a.id_ and dc_c.id in(";
		
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
		List<AccessActor> aa4list= this.accessService.find(actor, "ProcessInstance");
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
		sql+=" where pi_a.id_=a.id_ and pi_a.proc_inst_id_ in(";
		
		for(int i=0;i<pIds.size();i++){
			if(i>0)sql+=",";
			
			sql+="'"+pIds.get(i)+"'";
		}
		sql+="))";
		
		return new QlCondition(sql);
	}

}
