package cn.bc.workflow.deploy.dao.hibernate.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bc.BCConstants;
import cn.bc.core.query.condition.Condition;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.NotEqualsCondition;
import cn.bc.db.jdbc.RowMapper;
import cn.bc.orm.hibernate.jpa.HibernateCrudJpaDao;
import cn.bc.orm.hibernate.jpa.HibernateJpaNativeQuery;
import cn.bc.workflow.deploy.dao.DeployDao;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.domain.DeployResource;

/**
 * DAO接口的实现
 * 
 * @author wis
 * 
 */
public class DeployDaoImpl extends HibernateCrudJpaDao<Deploy> implements
		DeployDao {
	protected final Log logger = LogFactory.getLog(getClass());
	
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public Deploy loadByCode(String code) {
		if (code == null)
			return null;
		int i = code.indexOf(":");
		String version = null;
		if (i != -1) {
			version = code.substring(i + 1);
			code = code.substring(0, i);
		}
		AndCondition c = new AndCondition();
		c.add(new EqualsCondition("code", code));
		if (version != null) {
			c.add(new EqualsCondition("version", version));// 获取指定版本
		} else {
			c.add(new EqualsCondition("status", BCConstants.STATUS_ENABLED));// 获取最新版本
		}
		return this.createQuery().condition(c).singleResult();
	}

	public boolean isUniqueCodeAndVersion(Long currentId, String code,
			String version) {
		Condition c;
		if (currentId == null) {
			c = new AndCondition().add(new EqualsCondition("code", code)).add(
					new EqualsCondition("version", version));

		} else {
			c = new AndCondition().add(new EqualsCondition("code", code))
					.add(new NotEqualsCondition("id", currentId))
					.add(new EqualsCondition("version", version));
		}
		return this.createQuery().condition(c).count() > 0;
	}
	
	
	public Deploy loadByCodeAndId(String code,Long currentId){
		if(code == null )
			return null;
		AndCondition c = new AndCondition();
		c.add(new EqualsCondition("code", code));
		//状态正常
		c.add(new EqualsCondition("status", BCConstants.STATUS_ENABLED));
		
		if(currentId != null){
			//id不等于本对象
			c.add(new NotEqualsCondition("id",currentId));
		}
		return this.createQuery().condition(c).singleResult();
	}
	
	//模板分类
	public List<Map<String, String>> findCategoryOption() {
		String hql="SELECT d.category,1";
		   hql+=" FROM bc_wf_deploy d";
		   hql+=" GROUP BY d.category";
		 return	HibernateJpaNativeQuery.executeNativeSql(getJpaTemplate(), hql,null
		 	,new RowMapper<Map<String, String>>() {
				public Map<String, String> mapRow(Object[] rs, int rowNum) {
					Map<String, String> oi = new HashMap<String, String>();
					int i = 0;
					oi.put("value", rs[i++].toString());
					return oi;
				}
		});
	}

	/**
	 * 通过流程部署id判断此信息是否发布
	 * @param excludeId
	 * @return
	 */
	public Long isReleased(Long excludeId) {
		Long id = null;
		String sql = "select d.id from bc_wf_deploy d where d.id='"+excludeId+"'" +
				"and d.status_="+Deploy.STATUS_USING;
		try {
			id = this.jdbcTemplate.queryForLong(sql);
		} catch (EmptyResultDataAccessException e) {
			e.getStackTrace();
		}
		return id;
	}

	/**
	 * 通过流程id判断此信息是否已发起
	 * @param excludeId
	 * @return
	 */
	public Long isStarted(String deploymentId) {
		Long id = null;
		String sql = "select DISTINCT ard.id_ from act_re_deployment ard" +
				" inner join act_re_procdef arp on ard.id_ = arp.deployment_id_"+
				" inner join act_ru_execution ae on arp.id_ = ae.proc_def_id_ "+
				" where ard.id_='"+deploymentId+"'";
		try {
			id = this.jdbcTemplate.queryForLong(sql);
		} catch (EmptyResultDataAccessException e) {
			e.getStackTrace();
		}
		return id;
	}

	/**
	 * 判断指定的编码与版本号是否唯一
	 * 
	 * @param currentId
	 *            当前模板的id
	 * @param codes
	 *            当前模板要使用的编码列表
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Object> isUniqueResourceCodeAndExtCheck(Long id, String codes) {
		List<?> result;
		String [] codeAry = codes.split(",");
		if(codeAry.length == 0){
			return null;
		}else{
			List<Object> args = new ArrayList<Object>();
			StringBuffer hql = new StringBuffer();
			if (id == null) {
				hql.append("select dr.code from DeployResource dr where dr.code in (?");
				args.add(codeAry[0]);
				for (int i = 1; i < codeAry.length; i++) {
					hql.append(",?");
					args.add(codeAry[i]);
				}
				hql.append(")");
				result = this.getJpaTemplate().find(hql.toString(),
						args.toArray());
			}else{
				hql.append("select dr.code from DeployResource dr where dr.deploy.id !=? and dr.code in (?");
				args.add(id);
				args.add(codeAry[0]);
				for (int i = 1; i < codeAry.length; i++) {
					hql.append(",?");
					args.add(codeAry[i]);
				}
				hql.append(")");
				result = this.getJpaTemplate().find(hql.toString(),
						args.toArray());
			}
			if (result.size() == 0) {
				return null;
			} else {
				return ((ArrayList<Object>) result);
			}
		}
	}

	/**
	 * 通过流程部署记录id和流程编码和部署资源编码查找对应部署资源
	 * @param dmId
	 * @param wfCode
	 * @param resCode
	 * @return
	 */
	public DeployResource findDeployResourceByDmIdAndwfCodeAndresCode(String dmId,String wfCode, String resCode) {
		DeployResource dr = null;
		List<?> list = this.getJpaTemplate().find(
				"from DeployResource dr where dr.deploy.deploymentId=? and dr.deploy.code=? and dr.code=?",
				new Object[] { dmId,wfCode,resCode });
		if(list.size() == 0){
			logger.debug("异常！根据流程部署记录id,流程编码,流程资源编码查找不了流程资源！");
		} else if(list.size() == 1){
			dr = (DeployResource) list.get(0);
		}else{
			dr = (DeployResource) list.get(0);
			if (logger.isDebugEnabled()) {
				logger.debug("异常！存在两个或两个以上同一编码的资源，已选择第一个资源显示！");
			}
		}
		return dr;
	}

	/**
	 * 通过流程编码获取流程资源
	 * @param deployCode
	 * @return
	 */
	public DeployResource findDeployResourceCode(String deployCode) {
		DeployResource dr = null;
		List<?> list = this.getJpaTemplate().find(
				"from DeployResource dr where dr.code=?",
				new Object[] { deployCode });
		if(list.size() == 0){
			logger.debug("异常！流程编码查找不了流程资源！");
		} else if(list.size() == 1){
			dr = (DeployResource) list.get(0);
		}else{
			dr = (DeployResource) list.get(0);
			if (logger.isDebugEnabled()) {
				logger.debug("异常！存在两个或两个以上同一编码的资源，已选择第一个资源显示！");
			}
		}
		return dr;
	}
}
