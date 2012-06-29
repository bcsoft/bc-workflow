package cn.bc.workflow.web.struts2;

import org.commontemplate.util.Assert;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

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

	/**
	 * 打开工作空间
	 * 
	 * @return
	 * @throws Exception
	 */
	public String open() throws Exception {
		// id为流程实例的id
		Assert.assertNotEmpty(id);

		return SUCCESS;
	}
}