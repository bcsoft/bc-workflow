/**
 *
 */
package cn.bc.workflow.web.struts2;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * 流转引擎各模块视图Action的基类封装
 *
 * @author dragon
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public abstract class ViewAction<T extends Object> extends
  cn.bc.web.struts2.ViewAction<T> {
  private static final long serialVersionUID = 1L;

  @Override
  protected String getModuleContextPath() {
    return this.getContextPath() + "/bc-workflow";
  }
}