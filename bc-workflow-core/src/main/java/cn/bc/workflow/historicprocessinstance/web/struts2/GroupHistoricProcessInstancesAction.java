package cn.bc.workflow.historicprocessinstance.web.struts2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.QlCondition;
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
import cn.bc.workflow.service.WorkspaceServiceImpl;

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
	
	@Override
	protected Toolbar getHtmlPageToolbar() {
		Toolbar tb = new Toolbar();
		// 查看
		tb.addButton(new ToolbarButton().setIcon("ui-icon-check")
				.setText(getText("label.read"))
				.setClick("bc.historicProcessInstanceSelectView.open"));
		
		tb.addButton(Toolbar.getDefaultToolbarRadioGroup(this.getStatus(),
				"status", 3, getText("title.click2changeSearchStatus")));

		// 搜索按钮
		tb.addButton(this.getDefaultSearchToolbarButton());

		return tb;
	}

	@Override
	protected Condition getGridSpecalCondition() {
		// 状态条件
		AndCondition ac = new AndCondition();
		if (status != null && status.length() > 0) {
			String[] ss = status.split(",");
			if (ss.length == 1) {
				String sqlstr="";
				if (ss[0].equals(String.valueOf(SuspensionState.ACTIVE.getStateCode()))) {
					sqlstr += " a.end_time_ is null";
					sqlstr += " and ((b.suspension_state_ = "+SuspensionState.ACTIVE.getStateCode()+")";
					sqlstr += " and (f.suspension_state_ ="+SuspensionState.ACTIVE.getStateCode()+"))";
				} else if (ss[0].equals(String
						.valueOf(SuspensionState.SUSPENDED.getStateCode()))){
					sqlstr += " a.end_time_ is null";
					sqlstr += " and ((b.suspension_state_ = "+SuspensionState.SUSPENDED.getStateCode()+")";
					sqlstr += " or (f.suspension_state_ ="+SuspensionState.SUSPENDED.getStateCode()+"))";
				} else if (ss[0].equals(String.valueOf(WorkspaceServiceImpl.COMPLETE))){
					sqlstr += " a.end_time_ is not null";
				} 
				ac.add(new QlCondition(sqlstr,new Object[]{}));
			}
		}
		
		// 保存的用户id键值集合
		String codes ="";
		
		List<Actor> ownActors=this.getOwnActorCodes();
		
		if(ownActors==null||ownActors.size()==0){
			codes="''";
		}else{
			for(Actor a:ownActors){
				codes+="'"+a.getCode()+"',";
			}
			codes=codes.substring(0, codes.lastIndexOf(","));
		}
		
		String sql = "";
		sql += "exists(";
		sql += "select 1 ";
		sql += " from act_hi_taskinst d";
		sql += " where a.id_=d.proc_inst_id_ and d.end_time_ is not null and d.assignee_ in (";
		sql += codes+")";
		sql += ")";
		ac.add(new QlCondition(sql));
		ac.add(new IsNullCondition("f.parent_id_"));

		return ac.isEmpty() ? null : ac;
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
		columns.add(new TextColumn4MapKey("b.name_", "category",
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
		columns.add(new TextColumn4MapKey("a.first_", "startName",
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
		columns.add(new TextColumn4MapKey("a.duration_", "frmDuration",
				getText("flow.instance.duration"), 80).setSortable(true));
		columns.add(new TextColumn4MapKey("b.key_", "key",
				getText("flow.instance.key"), 180).setSortable(true)
				.setUseTitleFromLabel(true));
		columns.add(new HiddenColumn4MapKey("procinstid", "procinstid"));
		columns.add(new HiddenColumn4MapKey("status", "status"));
		return columns;
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
