package cn.bc.workflow.web.struts2;

import java.io.PrintStream;
import java.util.Map;

import org.apache.geronimo.mail.util.StringBufferOutputStream;
import org.commontemplate.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.web.ui.html.page.PageOption;
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
		} catch (Exception e) {
			this.error = "打开工作空间异常";
			this.errorDetail = new StringBuffer();
			e.printStackTrace(new PrintStream(new StringBufferOutputStream(
					errorDetail)));
		}

		return SUCCESS;
	}

	private void initPageOption() {
		pageOption = new PageOption();
		pageOption.setHeight(500).setWidth(700).setMinimizable(true)
				.setMaximizable(true).setMinWidth(700).setMinHeight(250);
	}
}