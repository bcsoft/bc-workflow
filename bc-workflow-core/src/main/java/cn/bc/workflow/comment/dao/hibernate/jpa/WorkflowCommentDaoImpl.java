/**
 * 
 */
package cn.bc.workflow.comment.dao.hibernate.jpa;

import javax.sql.DataSource;

import org.activiti.engine.impl.persistence.entity.CommentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bc.workflow.comment.dao.WorkflowCommentDao;

/**
 * 流程意见Dao的实现
 * 
 * @author lbj
 */
public class WorkflowCommentDaoImpl implements WorkflowCommentDao {
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void delete(String id) {
		String sql="delete from act_hi_comment where id_ = ?";
		this.jdbcTemplate.update(sql,new Object[]{id});	
	}

	public void update(CommentEntity ce) {
		String sql="update act_hi_comment set TIME_=?, USER_ID_=?, MESSAGE_=? where id_=?";
		this.jdbcTemplate.update(sql,new Object[]{ce.getTime(),ce.getUserId(),ce.getMessage(),ce.getId()});	
	}


}