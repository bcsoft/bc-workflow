/**
 * 
 */
package cn.bc.workflow.historictaskinstance.dao.hibernate.jpa;

import java.util.List;
import java.util.Map;

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
		sql += " where a.assignee_= ? ";
		if(isDone)
			sql += " and a.end_time_ is not null ";

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
	
	public List<String> findTaskNames(String account, boolean isDone) {
		String sql ="select a.name_";
		sql += " from act_hi_taskinst a";
		sql += " inner join act_re_procdef c on c.id_=a.proc_def_id_";
		sql += " where a.assignee_= ? ";
		if(isDone)
			sql += " and a.end_time_ is not null ";

		sql += " GROUP BY a.name_";
		
		return this.jdbcTemplate.queryForList(sql, new Object[]{account}, String.class);
	}

	public List<String> findTaskNames() {
		String sql ="select a.name_";
		sql += " from act_hi_taskinst a";
		sql += " GROUP BY a.name_";
		
		return this.jdbcTemplate.queryForList(sql, String.class);
	}

	public List<String> findTransactors(String processInstanceId,
			String[] includeTaskKeys, String[] exclusiveTaskKeys) {
		String sql = "select assignee_ as assignee";
		sql += " FROM act_hi_taskinst";
		sql += " where proc_inst_id_ = ?";
		if (includeTaskKeys == null && exclusiveTaskKeys == null)
			return this.jdbcTemplate.queryForList(sql,
					new Object[] { processInstanceId }, String.class);
		if (exclusiveTaskKeys == null && includeTaskKeys.length > 0) {
			String s = "";
			for (int i = 0; i < includeTaskKeys.length; i++) {
				s += "'" + includeTaskKeys[i] + "'";
				if (i < includeTaskKeys.length - 1)
					s += ",";
			}
			sql += " and task_def_key_ in(" + s + ")";
		}
		if (includeTaskKeys == null && exclusiveTaskKeys.length > 0) {
			String s = "";
			for (int i = 0; i < exclusiveTaskKeys.length; i++) {
				s += "'" + exclusiveTaskKeys[i] + "'";
				if (i < exclusiveTaskKeys.length - 1)
					s += ",";
			}
			sql += " and task_def_key_ not in(" + s + ")";
		}
		return this.jdbcTemplate.queryForList(sql,
				new Object[] { processInstanceId }, String.class);
	}

	@Override
	public List<Map<String, Object>> findHisProcessTaskVarValue(String processInstanceId, String[] taskKey, String[] varName) {
		String defKey = "";
		String name = "";
		for(int i = 0; i < taskKey.length; i++) {
			defKey += ("'" + taskKey[i] + "'");
			if (i < taskKey.length - 1)
				defKey += ",";
		}

		for(int i = 0; i < varName.length; i++) {
			name += ("'" + varName[i] + "'");
			if (i < varName.length - 1)
				name += ",";
		}

		String sql = "select a.name,d.name_,d.text_,t.task_def_key_";
		sql += " from act_hi_detail d";
		sql += " inner join act_hi_taskinst t on t.proc_inst_id_ = d.proc_inst_id_";
		sql += " left join bc_identity_actor a on a.code = t.assignee_";
		sql += " where d.proc_inst_id_ = ?";
		sql += " and t.task_def_key_ in (" + defKey + ")";
		sql += " and t.id_ = d.task_id_";
		sql += " and d.name_ in (" + name + ")";

		return this.jdbcTemplate.queryForList(sql, processInstanceId);
	}
}