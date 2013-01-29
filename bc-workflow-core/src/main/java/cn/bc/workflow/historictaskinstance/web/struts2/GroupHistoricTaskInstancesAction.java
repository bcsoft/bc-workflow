package cn.bc.workflow.historictaskinstance.web.struts2;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.InCondition;
import cn.bc.core.query.condition.impl.IsNotNullCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorRelation;
import cn.bc.identity.service.ActorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.web.ui.html.toolbar.Toolbar;
import cn.bc.web.ui.html.toolbar.ToolbarButton;

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
		return this.getHtmlPageNamespace() + "/historictaskinstance/select.js,"
				+this.getHtmlPageNamespace() + "/historictaskinstance/group/select.js";
	}


	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();
		
		List<String> ownActorCodes=new ArrayList<String>();
		List<Actor> ownActors=this.getOwnActorCodes();
		if(ownActors==null||ownActors.size()==0){
			ownActorCodes.add("''");
		}else{
			for(Actor a:ownActors){
				ownActorCodes.add(a.getCode());
			}
		}
		ac.add(new InCondition("a.assignee_",ownActorCodes));
		// 结束时间不能为空
		ac.add(new IsNotNullCondition("a.end_time_"));
		
		
		ac.add(new IsNullCondition("h.parent_id_"));
		
		return ac.isEmpty() ? null : ac;
	}
	
	/*获取属于当前用户拥有的指定岗位对应的上组织下的对应用户*/
	private List<Actor> getOwnActorCodes(){
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
		
		return ownActors;
	}


}
