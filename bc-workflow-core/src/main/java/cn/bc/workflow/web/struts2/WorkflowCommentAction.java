package cn.bc.workflow.web.struts2;

import java.util.Date;
import java.util.List;

import org.activiti.engine.impl.persistence.entity.CommentEntity;
import org.activiti.engine.task.Comment;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import cn.bc.web.ui.html.page.ButtonOption;
import cn.bc.web.ui.html.page.PageOption;

/**
 * 流程意见Action
 * 
 * @author lbj
 * 
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Controller
public class WorkflowCommentAction extends AbstractBaseAction {
	private static final long serialVersionUID = 1L;
	public PageOption pageOption;
	public String procId;//流程id
	public String tid;//任务id
	public String comment;//意见信息
	public String actorName;//添加意见人
	public Date time;//最后修改时间
	public boolean isOpen=false;//是否查看
	
	public boolean isNew() {
		return id == null ||id.length()==0;
	}
	
	public String save() throws Exception{	
		try {
			//新建时的保存
			if(isNew()){
				//设置添加意见的用户
				identityService.setAuthenticatedUserId(this.getContext().getUser().getCode());
				//添加意见
				taskService.addComment(tid, procId,comment);
			}else{
				//意见实体对象
				CommentEntity ce=null;
				//使用id和流程id加载原来的意见实体对象
				//匹配id，查找对应的意见对象
				for(Comment c:taskService.getProcessInstanceComments(procId)){
					CommentEntity ci=(CommentEntity) c;
					if(id.equals(ci.getId())){
						ce=ci;
						continue;
					}
				}
				//修改信息
				ce.setMessage(this.comment);
				ce.setUserId(this.getContext().getUser().getCode());
				ce.setTime(new Date());
				//更新意见
				this.workflowService.updateComment(ce);
			}
			// 返回信息
			this.json = createSuceessMsg(getText("form.save.success")).toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}
		return "json";
	}
	
	/**
	 * 添加意见
	 * 
	 * @return
	 * @throws Exception
	 */
	public String create() throws Exception {
		time=new Date();
		actorName=this.getContext().getUser().getName();
		// 初始化页面参数
		this.initPageOption();
		//增加保存按钮
		pageOption.addButton(new ButtonOption(
				getText("label.save"),
				null, "bc.flow.comment.save"));
		return SUCCESS;
	}
	
	/**
	 * 修改意见
	 * 
	 * @return
	 * @throws Exception
	 */
	public String edit() throws Exception {
		List<Comment> listC=null;
		if(procId!=null&&procId.length()>0)
			//通过流程实例ID
			listC=taskService.getProcessInstanceComments(procId);
		try {
			//匹配id，查找对应的意见
			for(Comment c:listC){
				CommentEntity ci=(CommentEntity) c;
				if(id.equals(ci.getId())){
					comment=ci.getMessage();
					time=ci.getTime();
					actorName=ci.getUserId();
					continue;
				}
			}
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}
		// 初始化页面参数
		this.initPageOption();
		//增加保存按钮
		pageOption.addButton(new ButtonOption(
				getText("label.save"),
				null, "bc.flow.comment.save"));
		return SUCCESS;	
	}
	
	/**
	 * 查看意见
	 * 
	 * @return
	 * @throws Exception
	 */
	public String open() throws Exception {
		isOpen=true;
		
		List<Comment> listC=null;
		if(procId!=null&&procId.length()>0)
			//通过流程实例ID
			listC=taskService.getProcessInstanceComments(procId);
		try {
			//匹配id，查找对应的意见
			for(Comment c:listC){
				CommentEntity ci=(CommentEntity) c;
				if(id.equals(ci.getId())){
					comment=ci.getMessage();
					time=ci.getTime();
					actorName=ci.getUserId();
					continue;
				}
			}
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}
		// 初始化页面参数
		this.initPageOption();
		return SUCCESS;
	}
	
	/**
	 * 删除意见
	 * 
	 * @return
	 * @throws Exception
	 */
	public String delete() throws Exception{
		try {
			workflowService.deleteComment(id);
			// 返回信息
			this.json = createSuceessMsg(getText("form.delete.success")).toString();
		} catch (Exception e) {
			json = createFailureMsg(e).toString();
		}
		return "json";
	}
	
	private void initPageOption() {
		pageOption = new PageOption();
		pageOption.setHeight(200).setWidth(550).setMinimizable(true)
				.setMaximizable(true).setMinWidth(200).setMinHeight(100);
	}
	
}