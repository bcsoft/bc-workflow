package cn.bc.workflow.dao.hibernate.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.activiti.engine.impl.persistence.entity.SuspensionState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import cn.bc.orm.hibernate.jpa.HibernateCrudJpaDao;
import cn.bc.orm.hibernate.jpa.HibernateJpaNativeQuery;
import cn.bc.workflow.dao.WorkflowModuleRelationDao;
import cn.bc.workflow.domain.WorkflowModuleRelation;
import cn.bc.workflow.service.WorkspaceServiceImpl;

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
	
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public List<Map<String, Object>> findList(Long mid, String mtype,String key,
			String[] globalKeys) {
		// sql占位符替换参数
		List<Object> args = new ArrayList<Object>();
		String hql = "SELECT a.pid,to_char(b.start_time_,'YYYY-MM-DD HH24:MI') as statrTime";
		hql+=",to_char(b.end_time_,'YYYY-MM-DD HH24:MI') as endTime";
		hql+=",c.name_,c.key_,f.suspension_state_";
		
		if (globalKeys != null && globalKeys.length > 0) {
			for (String globalKey : globalKeys) {
				hql += ",getprocessglobalvalue(a.pid,?) as "+globalKey;
				args.add(globalKey);
			}
		}

		hql += " FROM bc_wf_module_relation a";
		hql += " INNER JOIN act_hi_procinst b on b.proc_inst_id_=a.pid";
		hql += " INNER JOIN act_re_procdef c on c.id_=b.proc_def_id_";
		hql += " left join act_ru_execution f on a.pid = f.proc_inst_id_";
		hql += " where f.parent_id_ is null and a.mid=? and a.mtype=?";
		args.add(mid);
		args.add(mtype);
		
		if(key!=null&&key.length()>0){
			hql+=" and c.key_=? ";
			args.add(key);
		}
		
		hql += " ORDER BY b.start_time_ DESC";
		final String[] globalKeys_ = globalKeys;
		
		if(logger.isDebugEnabled()){
			logger.debug("sql:="+hql);
			logger.debug("args:="+StringUtils.collectionToCommaDelimitedString(args));
		}

		return HibernateJpaNativeQuery.executeNativeSql(getJpaTemplate(), hql,
				args.toArray(),
				new cn.bc.db.jdbc.RowMapper<Map<String, Object>>() {
					public Map<String, Object> mapRow(Object[] rs, int rowNum) {
						Map<String, Object> o = new HashMap<String, Object>();
						int i = 0;
						o.put("pid", rs[i++]);
						o.put("startTime", rs[i++]);
						o.put("endTime", rs[i++]);
						o.put("name", rs[i++]);
						o.put("key", rs[i++]);
						Object suspensionState = rs[i++];
						if (o.get("endTime") != null) {// 已结束
							o.put("status", WorkspaceServiceImpl.COMPLETE);
						} else {
							if (suspensionState.toString().equals(
									String.valueOf(SuspensionState.ACTIVE
											.getStateCode()))) {// 流转中
								o.put("status", String
										.valueOf(SuspensionState.ACTIVE
												.getStateCode()));
							} else if (suspensionState.toString().equals(
									String.valueOf(SuspensionState.SUSPENDED
											.getStateCode()))) {// 已暂停
								o.put("status", String
										.valueOf(SuspensionState.SUSPENDED
												.getStateCode()));
							}
						}

						if (globalKeys_ != null && globalKeys_.length > 0) {
							for (String globalKey : globalKeys_) {
								o.put(globalKey, rs[i++]);
							}
						}

						return o;
					}
				});
	}

	public boolean hasRelation(Long mid, String mtype, String key) {
		// sql占位符替换参数
		List<Object> args = new ArrayList<Object>();
		String hql = "SELECT count(*)";
		hql += " FROM bc_wf_module_relation a";
		if(key!=null&&key.length()>0){
			hql += " INNER JOIN act_hi_procinst b on b.proc_inst_id_=a.pid";
			hql += " INNER JOIN act_re_procdef c on c.id_=b.proc_def_id_";
		}
		hql += " WHERE a.mid=? and a.mtype=?";
		args.add(mid);
		args.add(mtype);
		
		if(key!=null&&key.length()>0){
			hql+=" and c.key_=?";
			args.add(key);
		}
		
		if(logger.isDebugEnabled()){
			logger.debug("sql:="+hql);
			logger.debug("args:="+StringUtils.collectionToCommaDelimitedString(args));
		}
		
		return this.jdbcTemplate.queryForInt(hql,args.toArray())>0;
	}
	
	

}
