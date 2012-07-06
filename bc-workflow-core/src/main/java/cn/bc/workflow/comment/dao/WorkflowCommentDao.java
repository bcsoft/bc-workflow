/**
 * 
 */
package cn.bc.workflow.comment.dao;

import org.activiti.engine.impl.persistence.entity.CommentEntity;


/**
 * 流程意见Dao
 * 
 * @author lbj
 */
public interface WorkflowCommentDao {

	/**
	 * 删除意见
	 * 
	 * @param id 意见id
	 */
	void delete(String id);
	
	/**
	 * 修改意见
	 * 
	 * @param ce意见实体对象
	 */
	void update(CommentEntity ce);

}