/**
 * 
 */
package cn.bc.workflow.todo.web.struts2;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.opensymphony.xwork2.ActionSupport;

/**
 * 待办边栏Action
 * 
 * @author wis
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class TodoSideBarPersonalsAction extends ActionSupport {
	private static final long serialVersionUID = 1L;

	@Override
	public String execute() throws Exception {

		return SUCCESS;
	}
	
}