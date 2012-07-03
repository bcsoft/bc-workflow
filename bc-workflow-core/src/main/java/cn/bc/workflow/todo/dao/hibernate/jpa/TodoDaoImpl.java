/**
 * 
 */
package cn.bc.workflow.todo.dao.hibernate.jpa;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bc.workflow.todo.dao.TodoDao;

/**
 * 待办Dao的实现
 * 
 * @author wis
 */
public class TodoDaoImpl implements TodoDao {
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * 通过待办任务id判断此待办任务是否签领
	 * @param excludeId
	 * @return
	 */
	public Long checkIsSign(Long excludeId) {
		Long id = null;
		String sql = "select art.id_ from act_ru_task art where art.id_='"+excludeId+"'" +
				"and art.assignee_ is not null";
		try {
			id = this.jdbcTemplate.queryForLong(sql);
		} catch (EmptyResultDataAccessException e) {
			e.getStackTrace();
		}
		return id;
	}

	/**
	 * 通过待办任务id用户实现签领
	 * @param excludeId
	 * @param assignee
	 */
	@Deprecated
	public void doSignTask(Long excludeId,String assignee) {
		if(null != excludeId){
			String taskId = excludeId.toString();
			// sql1更新历史任务act_hi_taskinst表的历史参与人(assignee)列
			String sql1 = "update act_hi_taskinst set assignee_ = ? where id_ = ? and assignee_ is null"; 
			this.jdbcTemplate.update(sql1,
					new Object[] { assignee, taskId });
			// sql2更新任务参与者act_ru_identitylink表的清空任务参与岗位(group_id_),更新任务参与人(user_id_)列
			String sql2 = "update act_ru_identitylink set group_id_= null,user_id_ = ? where task_id_ = ?";
			this.jdbcTemplate.update(sql2,
					new Object[] { assignee, taskId });
			// sql3更新任务表act_ru_task的任务处理人(assignee_)列
			String sql3 = "update act_ru_task set assignee_ = ? where id_ = ?";
			this.jdbcTemplate.update(sql3,
					new Object[] { assignee, taskId });
		}
	}

}