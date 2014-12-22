package cn.bc.workflow.dao.hibernate.jpa;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bc.workflow.dao.WorkflowDao;
import org.springframework.util.Assert;

/**
 * 流程Dao接口的实现
 * 
 * @author lbj
 * 
 */
public class WorkflowDaoImpl implements WorkflowDao {
	private static Log logger = LogFactory.getLog(WorkflowDaoImpl.class);
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public Map<String, Object> findGlobalValue(String pid,String[] valueKeys) {
		if(pid==null||pid.length()==0){
			if(logger.isDebugEnabled()){
				logger.debug("pid is null or pid length equals 0!");
				return null;
			}	
		}
		
		if(valueKeys==null){
			if(logger.isDebugEnabled()){
				logger.debug("valueKeys is null!");
				return null;
			}	
		}
		
		if(valueKeys.length==0){
			if(logger.isDebugEnabled()){
				logger.debug("valueKeys length equals 0!");
				return null;
			}	
		}
		
		String sql="SELECT ";

		for(String key:valueKeys){
			sql += "getprocessglobalvalue('"+pid+"','"+key+"') as "+key+",";
		}
		
		sql=sql.substring(0,sql.lastIndexOf(","));
		
		sql+=" from bc_dual";
		
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
		
		Object[] args=new Object[]{pid,taskKey,localValueKey};
		
		String sql="select getprocesstasklocalvalue(?,?,?) from bc_dual";
		
		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
			logger.debug("args="+args.toString());
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
		sql += " 		where d.name_= 'main_processInstanceId'";// 主流程变量名
		sql += " 		and d.proc_inst_id_ = ?";
		sql += " )";
		sql += " and wlog.type_ = 'process_start'";// 实例发起Code

		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
			logger.debug("processInstanceId="+processInstanceId);
		}

		return this.jdbcTemplate.queryForMap(sql, processInstanceId);
	}

	public List<Map<String, Object>> findSubProcessInstanceInfoById(String processInstanceId) {
		Assert.notNull(processInstanceId);

		String sql = "SELECT ahp.start_time_,ahp.end_time_,aha.act_name_,aha.assignee_,a.name";
		sql += " FROM act_hi_procinst ahp";
		sql += " inner join act_hi_actinst aha on aha.proc_inst_id_ = ahp.proc_inst_id_";
		sql += " inner join bc_identity_actor a on a.code = aha.assignee_";
		sql += " where ahp.proc_inst_id_ = ?";
		sql += " and aha.act_type_ = 'userTask'";
		sql += " and aha.act_id_ = (";
		sql += " 	SELECT act_id_   ";
		sql += " 	FROM act_hi_actinst";
		sql += " 	where proc_inst_id_ = ahp.proc_inst_id_ and act_type_ = 'userTask'";
		sql += " 	group by act_id_, start_time_ ";
		sql += " 	order by start_time_ asc limit 1 OFFSET 1";
		sql += " )order by aha.start_time_ asc";

		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
			logger.debug("processInstanceId="+processInstanceId);
		}

		return this.jdbcTemplate.queryForList(sql, processInstanceId);
	}
}
