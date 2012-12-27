/**
 * 
 */
package cn.bc.workflow.historictaskinstance.dao.hibernate.jpa;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bc.workflow.historictaskinstance.dao.HistoricTaskInstanceDao;

/**
 * 任务监控Dao的实现
 * 
 * @author lbj
 */
public class HistoricTaskInstanceDaoImpl implements HistoricTaskInstanceDao {
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public List<String> findProcessNames(String account, boolean isDone) {
		String sql ="select c.name_";
		sql += " from act_hi_taskinst a";
		sql += " inner join act_re_procdef c on c.id_=a.proc_def_id_";
		sql += "where a.assignee_= ? ";
		if(isDone)
			sql += "and a.end_time_ is not null ";

		sql += " GROUP BY c.name_";
		
		return this.jdbcTemplate.queryForList(sql, new Object[]{account}, String.class);
	}

	public List<String> findProcessNames() {
		String sql ="select c.name_";
		sql += " from act_hi_taskinst a";
		sql += " inner join act_re_procdef c on c.id_=a.proc_def_id_";
		sql += " GROUP BY c.name_";
		
		return this.jdbcTemplate.queryForList(sql, String.class);
	}
}