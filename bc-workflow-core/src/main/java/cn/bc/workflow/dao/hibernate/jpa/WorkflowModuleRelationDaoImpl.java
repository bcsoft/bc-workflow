package cn.bc.workflow.dao.hibernate.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.orm.hibernate.jpa.HibernateCrudJpaDao;
import cn.bc.orm.hibernate.jpa.HibernateJpaNativeQuery;
import cn.bc.workflow.dao.WorkflowModuleRelationDao;
import cn.bc.workflow.domain.WorkflowModuleRelation;

/**
 * 流程关系Dao接口的实现
 * 
 * @author lbj
 * 
 */
public class WorkflowModuleRelationDaoImpl extends
		HibernateCrudJpaDao<WorkflowModuleRelation> implements
		WorkflowModuleRelationDao {
	private static Log logger = LogFactory
			.getLog(WorkflowModuleRelationDaoImpl.class);

	public List<Map<String, Object>> findList(Long mid, String mtype) {
		String hql = "SELECT a.pid,to_char(b.start_time_,'YYYY-MM-DD HH:mm'),to_char(b.end_time_,'YYYY-MM-DD HH:mm'),c.name_";
		hql += " FROM bc_wf_module_relation a";
		hql += " INNER JOIN act_hi_procinst b on b.proc_inst_id_=a.pid";
		hql += " INNER JOIN act_re_procdef c on c.id_=b.proc_def_id_";
		hql += " where mid=? and mtype=?";
		hql += " ORDER BY b.start_time_ DESC";
		return HibernateJpaNativeQuery.executeNativeSql(getJpaTemplate(), hql,
				new Object[] { mid, mtype },
				new cn.bc.db.jdbc.RowMapper<Map<String, Object>>() {
					public Map<String, Object> mapRow(Object[] rs, int rowNum) {
						Map<String, Object> o = new HashMap<String, Object>();
						int i = 0;
						o.put("pid", rs[i++]);
						o.put("startTime", rs[i++]);
						o.put("endTime", rs[i++]);
						o.put("name", rs[i++]);
						return o;
					}
				});
	}

}
