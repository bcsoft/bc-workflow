/**
 * 
 */
package cn.bc.workflow.flowattach.dao.hibernate.jpa;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bc.orm.hibernate.jpa.HibernateCrudJpaDao;
import cn.bc.workflow.flowattach.dao.FlowAttachDao;
import cn.bc.workflow.flowattach.domain.FlowAttach;

/**
 * 流程附加信息Dao的实现
 * 
 * @author lbj
 */
public class FlowAttachDaoImpl extends HibernateCrudJpaDao<FlowAttach> implements FlowAttachDao {

	protected final Log logger = LogFactory.getLog(getClass());
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	public String getProcInstName(String pid){
		String sql="select b.name_ as name";
		 sql+=" from act_hi_procinst a";
		 sql+=" inner join act_re_procdef b on b.id_=a.proc_def_id_";
		 sql+=" where a.proc_inst_id_='"+pid+"'";
		List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
		return list.get(0).get("name").toString();
	}
	
}