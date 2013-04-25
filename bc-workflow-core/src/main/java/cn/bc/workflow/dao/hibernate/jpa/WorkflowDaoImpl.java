package cn.bc.workflow.dao.hibernate.jpa;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bc.workflow.dao.WorkflowDao;

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
		Assert.assertNotNull(pid);
		Assert.assertNotNull(taskKey);
		Assert.assertNotNull(localValueKey);
		
		Object[] args=new Object[]{pid,taskKey,localValueKey};
		
		String sql="select getprocesstasklocalvalue(?,?,?) from bc_dual";
		
		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
			logger.debug("args="+args.toString());
		}
		
		return this.jdbcTemplate.queryForObject(sql, Object.class, args);
	}

	

	 
}  
