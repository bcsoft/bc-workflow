package cn.bc.workflow.web.struts2;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.geronimo.mail.util.StringBufferOutputStream;
import org.commontemplate.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.acl.domain.AccessActor;
import cn.bc.acl.domain.AccessHistory;
import cn.bc.acl.service.AccessHistoryService;
import cn.bc.acl.service.AccessService;
import cn.bc.core.util.JsonUtils;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.web.SystemContext;
import cn.bc.web.ui.html.page.PageOption;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.service.WorkspaceService;

/**
 * 工作空间Action
 * 
 * @author dragon
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class WorkspaceAction extends AbstractBaseAction {
	private static final long serialVersionUID = 1L;
	public PageOption pageOption;// 对话框参数配置
	public String title;// 对话框标题
	public String error;// 异常的简要描述信息
	public StringBuffer errorDetail;// 异常的堆栈信息
	public Map<String, Object> ws;// 包含工作空间所要显示的所有数据
	public Boolean isAccess=false;//通过的我的监控打开的工作控制 默认false
	public String accessJson;//访问历史详细信息
	
	
	private WorkspaceService workspaceService;

	@Autowired
	public void setWorkspaceService(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}

	/**
	 * 打开边栏设计页面
	 * 
	 * @return
	 * @throws Exception
	 */
	public String sidebarDesign() throws Exception {
		this.title = "待办边栏设计";

		// 初始化页面参数
		this.initPageOption();

		return SUCCESS;
	}

	/**
	 * 打开工作空间设计页面
	 * 
	 * @return
	 * @throws Exception
	 */
	public String design() throws Exception {
		this.title = "工作空间设计";

		// 初始化页面参数
		this.initPageOption();

		return SUCCESS;
	}

	/**
	 * 打开工作空间
	 * 
	 * @return
	 * @throws Exception
	 */
	public String open() throws Exception {
		// 初始化页面参数
		this.initPageOption();

		try {
			// this.error = "AAA";
			// this.errorDetail = new StringBuffer("[无]");
			// id为流程实例的id
			Assert.assertNotEmpty(id);

			// 获取工作空间信息
			this.ws = workspaceService.findWorkspaceInfo(id);

			// 对话框标题
			this.title = (String) this.ws.get("subject");
			
			//插入访问记录
			if(isAccess)this.addAccessHistory();
				
			
		} catch (Exception e) {
			this.error = "打开工作空间异常";
			this.errorDetail = new StringBuffer();
			e.printStackTrace(new PrintStream(new StringBufferOutputStream(
					errorDetail)));
		}

		return SUCCESS;
	}
	
	private AccessService accessService;
	private AccessHistoryService accessHistoryService;
	
	@Autowired
	public void setAccessHistoryService(AccessHistoryService accessHistoryService) {
		this.accessHistoryService = accessHistoryService;
	}

	@Autowired
	public void setAccessService(AccessService accessService) {
		this.accessService = accessService;
	}

	private void addAccessHistory() throws Exception {
		// 查找当前登录用户条件
		SystemContext context = (SystemContext) this.getContext();
		//当前用户
		Actor actor=context.getUser();
		//声明访问历史
		AccessHistory accessHistory=this.accessHistoryService.create();
		
		Map<String,Object> ajson4map=JsonUtils.toMap(accessJson);
		
		accessHistory.setDocId(ajson4map.get("docId").toString());
		accessHistory.setDocName(ajson4map.get("docName").toString());
		accessHistory.setDocType(ajson4map.get("docType").toString());
		accessHistory.setUrl(ajson4map.get("url").toString());
		accessHistory.setAccessDate(Calendar.getInstance());
		accessHistory.setActorHistory(context.getUserHistory());
		
		
		//优先查找流程实例监控配置
		List<AccessActor> aa4list =this.accessService.findByDocIdType(actor.getId(), id, "ProcessInstance");
		if(aa4list!=null&&aa4list.size()>0){
			accessHistory.setPid(aa4list.get(0).getAccessDoc().getId());
			accessHistory.setRole(aa4list.get(0).getRole());
			accessHistory.setSource(getText("flow.accessControl.source.ProcessInstance"));
			this.accessHistoryService.save(accessHistory);
			return;
		}
		
		
		//查找流程部署监控皮质
		aa4list =this.accessService.findByDocIdType(actor.getId(),ajson4map.get("deployId").toString(),Deploy.class.getSimpleName());
		if(aa4list!=null&&aa4list.size()>0){
			accessHistory.setPid(aa4list.get(0).getAccessDoc().getId());
			accessHistory.setRole(aa4list.get(0).getRole());
			accessHistory.setSource(getText("flow.accessControl.source.deploy"));
			this.accessHistoryService.save(accessHistory);
			return;
		}
		
		//查找部门监控的权限
		if(context.hasAnyRole(getText("key.role.bc.workflow.group"))){
			accessHistory.setSource(getText("flow.accessControl.source.group"));
			this.accessHistoryService.save(accessHistory);
			return;
		}
		
		//未知来源
		accessHistory.setSource(getText("flow.accessControl.source.unknown"));
		this.accessHistoryService.save(accessHistory);
		
	}

	private void initPageOption() {
		pageOption = new PageOption();
		pageOption.setHeight(500).setWidth(700).setMinimizable(true)
				.setMaximizable(true).setMinWidth(700).setMinHeight(250);
	}
}