package cn.bc.workflow.dao.jpa;

import cn.bc.workflow.dao.WorkflowDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * 流程Dao接口的实现
 *
 * @author lbj
 */
@Component
public class WorkflowDaoImpl implements WorkflowDao {
	private static Logger logger = LoggerFactory.getLogger(WorkflowDaoImpl.class);
	@Autowired
	private JdbcTemplate jdbcTemplate;

	public Map<String, Object> findGlobalValue(String pid, String[] valueKeys) {
		if (pid == null || pid.length() == 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("pid is null or pid length equals 0!");
				return null;
			}
		}

		if (valueKeys == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("valueKeys is null!");
				return null;
			}
		}

		if (valueKeys.length == 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("valueKeys length equals 0!");
				return null;
			}
		}

		String sql = "SELECT ";

		for (String key : valueKeys) {
			sql += "getprocessglobalvalue('" + pid + "','" + key + "') as " + key + ",";
		}

		sql = sql.substring(0, sql.lastIndexOf(","));

		sql += " from bc_dual";

		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
		}

		return this.jdbcTemplate.queryForMap(sql);
	}

	public Object findLocalValue(String pid, String taskKey,
	                             String localValueKey) {
		Assert.notNull(pid);
		Assert.notNull(taskKey);
		Assert.notNull(localValueKey);

		Object[] args = new Object[]{pid, taskKey, localValueKey};

		String sql = "select getprocesstasklocalvalue(?,?,?) from bc_dual";

		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
			logger.debug("args=" + args.toString());
		}

		return this.jdbcTemplate.queryForObject(sql, Object.class, args);
	}

	@Override
	public Map<String, Object> findMainProcessInstanceInfoById(String processInstanceId) {
		Assert.notNull(processInstanceId);
		String sql = "SELECT ahp.start_time_,ahp.end_time_,wlog.ename";
		sql += " ,(";
		sql += " 	select ahd.text_ ";
		sql += " 	from act_hi_detail ahd";
		sql += " 	where ahd.proc_inst_id_ = ahp.proc_inst_id_";
		sql += " 	and ahd.name_ = 'subject'";
		sql += " 	order by ahd.time_ asc limit 1";
		sql += " ) as text_";
		sql += " FROM act_hi_procinst ahp";
		sql += " inner join bc_wf_excution_log wlog on wlog.pid = ahp.proc_inst_id_";
		sql += " where ahp.proc_inst_id_ = (";
		sql += " 	SELECT text_";
		sql += " 		from act_hi_detail d";
		sql += " 		where d.name_= 'mainProcessInstanceId'";// 主流程变量名
		sql += " 		and d.proc_inst_id_ = ?";
		sql += " )";
		sql += " and wlog.type_ = 'process_start'";// 实例发起Code

		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
			logger.debug("processInstanceId=" + processInstanceId);
		}

		return this.jdbcTemplate.queryForMap(sql, processInstanceId);
	}

	public List<Map<String, Object>> findSubProcessInstanceInfoById(String processInstanceId) {
		Assert.notNull(processInstanceId);

		String sql = "with main(proc_inst_id_,task_id_) as (";// 查找主流程表，获得子流程流程实例Id与所在任务Id
		sql += " select text_,task_id_";
		sql += " from act_hi_detail";
		sql += " where proc_inst_id_ = ?";
		sql += " and name_ = 'subProcessInstanceId'";
		sql += ")";
		sql += "select p.start_time_,p.end_time_,m.task_id_";
		sql += " ,(";// 通过子流程实例获取办理人
		sql += "  select d.text_ ";
		sql += " 	from act_hi_detail d";
		sql += " 	where d.proc_inst_id_ = p.proc_inst_id_ ";
		sql += " 	and d.name_= 'mainProcessAssignedActorNames'";
		sql += " )";
		sql += " from act_hi_procinst p";
		sql += " inner join main m on m.proc_inst_id_ = p.proc_inst_id_";

		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
			logger.debug("processInstanceId=" + processInstanceId);
		}

		return this.jdbcTemplate.queryForList(sql, processInstanceId);
	}

	@Override
	public boolean updateDeploymentResource(String deploymentId, String resourceName, byte[] in) {
		String sql = "update act_ge_bytearray set bytes_ = ?";
		sql += " where deployment_id_ = ? and name_ = ?";

		Integer rows = this.jdbcTemplate.update(sql, in, deploymentId, resourceName);
		return (rows != null) && (rows > 0);
	}
}
