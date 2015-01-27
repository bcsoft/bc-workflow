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
import cn.bc.workflow.service.WorkspaceServiceImpl_old;

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

	public List<Map<String, Object>> findList(Long mid, String mtype,
			String key, String[] globalKeys) {
		// sql占位符替换参数
		List<Object> args = new ArrayList<Object>();
		String hql = "SELECT a.pid,to_char(b.start_time_,'YYYY-MM-DD HH24:MI') as statrTime";
		hql += ",to_char(b.end_time_,'YYYY-MM-DD HH24:MI') as endTime";
		hql += ",c.name_,c.key_,f.suspension_state_";

		if (globalKeys != null && globalKeys.length > 0) {
			for (String globalKey : globalKeys) {
				hql += ",getprocessglobalvalue(a.pid,?) as " + globalKey;
				args.add(globalKey);
			}
		}

		hql += " FROM bc_wf_module_relation a";
		hql += " INNER JOIN act_hi_procinst b on b.proc_inst_id_=a.pid";
		hql += " INNER JOIN act_re_procdef c on c.id_=b.proc_def_id_";
		hql += " left join act_ru_execution f on a.pid = f.proc_inst_id_";
		hql += " where f.parent_id_ is null ";
		if (mid != null) {
			hql += " and a.mid=? ";
			args.add(mid);
		}

		if (mtype != null && mtype.length() > 0) {
			hql += " and a.mtype=? ";
			args.add(mtype);
		}

		if (key != null && key.length() > 0) {
			hql += " and c.key_=? ";
			args.add(key);
		}

		hql += " ORDER BY b.start_time_ DESC";
		final String[] globalKeys_ = globalKeys;

		if (logger.isDebugEnabled()) {
			logger.debug("sql:=" + hql);
			logger.debug("args:="
					+ StringUtils.collectionToCommaDelimitedString(args));
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
							o.put("status", WorkspaceServiceImpl_old.COMPLETE);
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

	public List<Map<String, Object>> findList(Long[] mid, String mtype,
											  String key, String[] globalKeys) {
		// sql占位符替换参数
		List<Object> args = new ArrayList<Object>();
		String hql = "SELECT a.mid,a.pid,to_char(b.start_time_,'YYYY-MM-DD HH24:MI') as statrTime";
		hql += ",to_char(b.end_time_,'YYYY-MM-DD HH24:MI') as endTime";
		hql += ",c.name_,c.key_,f.suspension_state_";

		if (globalKeys != null && globalKeys.length > 0) {
			for (String globalKey : globalKeys) {
				hql += ",getprocessglobalvalue(a.pid,?) as " + globalKey;
				args.add(globalKey);
			}
		}

		hql += " FROM bc_wf_module_relation a";
		hql += " INNER JOIN act_hi_procinst b on b.proc_inst_id_=a.pid";
		hql += " INNER JOIN act_re_procdef c on c.id_=b.proc_def_id_";
		hql += " left join act_ru_execution f on a.pid = f.proc_inst_id_";
		hql += " where f.parent_id_ is null ";
		if (mid != null) {
			int len = mid.length;
			if (len > 1) {
				String ids = "";
				for(int i = 0; i < len; i++) {
					ids += mid[i];
					if (i < len - 1) ids += ",";
				}
				hql += " and a.mid in ( " + ids + " ) ";
			} else {
				hql += " and a.mid = ?";
				args.add(mid[0]);
			}
		}

		if (mtype != null && mtype.length() > 0) {
			hql += " and a.mtype=? ";
			args.add(mtype);
		}

		if (key != null && key.length() > 0) {
			hql += " and c.key_=? ";
			args.add(key);
		}

		hql += " ORDER BY b.start_time_ DESC";
		final String[] globalKeys_ = globalKeys;

		if (logger.isDebugEnabled()) {
			logger.debug("sql:=" + hql);
			logger.debug("args:="
					+ StringUtils.collectionToCommaDelimitedString(args));
		}

		return HibernateJpaNativeQuery.executeNativeSql(getJpaTemplate(), hql,
				args.toArray(),
				new cn.bc.db.jdbc.RowMapper<Map<String, Object>>() {
					public Map<String, Object> mapRow(Object[] rs, int rowNum) {
						Map<String, Object> o = new HashMap<String, Object>();
						int i = 0;
						o.put("mid", rs[i++]);
						o.put("pid", rs[i++]);
						o.put("startTime", rs[i++]);
						o.put("endTime", rs[i++]);
						o.put("name", rs[i++]);
						o.put("key", rs[i++]);
						Object suspensionState = rs[i++];
						if (o.get("endTime") != null) {// 已结束
							o.put("status", WorkspaceServiceImpl_old.COMPLETE);
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
		if (key != null && key.length() > 0) {
			hql += " INNER JOIN act_hi_procinst b on b.proc_inst_id_=a.pid";
			hql += " INNER JOIN act_re_procdef c on c.id_=b.proc_def_id_";
		}
		hql += " WHERE a.mid=? and a.mtype=?";
		args.add(mid);
		args.add(mtype);

		if (key != null && key.length() > 0) {
			hql += " and c.key_=?";
			args.add(key);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("sql:=" + hql);
			logger.debug("args:="
					+ StringUtils.collectionToCommaDelimitedString(args));
		}

		return this.jdbcTemplate.queryForInt(hql, args.toArray()) > 0;
	}

	public List<Map<String, Object>> findList(String[] mtype,
			String[] propertys, String[] values, String[] globalKeys) {
		// sql占位符替换参数
		List<Object> args = new ArrayList<Object>();
		String hql = "select b.id_,to_char(b.start_time_,'YYYY-MM-DD HH24:MI') as statrTime";
		hql += ",to_char(b.end_time_,'YYYY-MM-DD HH24:MI') as endTime,c.name_,c.key_,f.suspension_state_";
		if (globalKeys != null && globalKeys.length > 0) {
			for (String globalKey : globalKeys) {
				hql += ",getprocessglobalvalue(b.proc_inst_id_,?) as "
						+ globalKey;
				args.add(globalKey);
			}
		}
		hql += " from act_hi_procinst b";
		hql += " left join act_re_procdef c on c.id_=b.proc_def_id_";
		hql += " left join act_ru_execution f on f.proc_inst_id_=b.proc_inst_id_";
		hql += " inner join act_hi_detail d on d.proc_inst_id_ = b.proc_inst_id_";
		hql += " where f.parent_id_ is null ";
		// 流程类型
		if (mtype != null && mtype.length > 0) {

			if (mtype.length == 1) {
				hql += " and c.key_=? ";
				args.add(mtype[0]);
			} else {
				hql += " and c.key_ in ( ";
				for (int i = 0; i < mtype.length; i++) {
					if (i + 1 != mtype.length) {
						hql += "?,";
					} else {
						hql += "?)";
					}
					args.add(mtype[i]);
				}
			}
		}

		// 流程中的变量
		if (propertys != null && propertys.length > 0) {

			if (propertys.length == 1) {
				hql += " and d.name_=? ";
				args.add(propertys[0]);
			} else {
				hql += " and d.name_ in ( ";
				for (int i = 0; i < propertys.length; i++) {
					if (i + 1 != propertys.length) {
						hql += "?,";
					} else {
						hql += "?)";
					}
					args.add(propertys[i]);
				}
			}
		}
		// 流程中的变量值
		if (values != null && values.length > 0) {

			if (propertys.length == 1) {
				hql += " and d.text_=? ";
				args.add(values[0]);
			} else {
				hql += " and d.text_ in ( ";
				for (int i = 0; i < values.length; i++) {
					if (i + 1 != values.length) {
						hql += "?,";
					} else {
						hql += "?)";
					}
					args.add(values[i]);
				}
			}
		}
		hql += " order by b.start_time_ desc";
		final String[] globalKeys_ = globalKeys;

		if (logger.isDebugEnabled()) {
			logger.debug("sql:=" + hql);
			logger.debug("args:="
					+ StringUtils.collectionToCommaDelimitedString(args));
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
							o.put("status", WorkspaceServiceImpl_old.COMPLETE);
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

}
