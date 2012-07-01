package cn.bc.workflow.web.struts2;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.commontemplate.util.Assert;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.web.ui.html.page.PageOption;

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
	public PageOption pageOption;
	public String title;

	/**
	 * 打开工作空间
	 * 
	 * @return
	 * @throws Exception
	 */
	public String open() throws Exception {
		// id为流程实例的id
		Assert.assertNotEmpty(id);
		ProcessInstance instance = workflowService.loadInstance(id);
		ProcessDefinition definition = workflowService.loadDefinition(instance
				.getProcessDefinitionId());
		this.title = definition.getName();

		// 初始化页面参数
		this.initPageOption();

		return SUCCESS;
	}

	private void initPageOption() {
		pageOption = new PageOption();
		pageOption.setHeight(500).setWidth(700);
	}
}