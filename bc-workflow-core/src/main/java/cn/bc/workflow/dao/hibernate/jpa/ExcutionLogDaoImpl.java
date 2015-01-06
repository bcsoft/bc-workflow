package cn.bc.workflow.dao.hibernate.jpa;

import cn.bc.core.exception.CoreException;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.util.DateUtils;
import cn.bc.core.util.JsonUtils;
import cn.bc.orm.hibernate.jpa.HibernateCrudJpaDao;
import cn.bc.orm.hibernate.jpa.HibernateJpaNativeQuery;
import cn.bc.workflow.dao.ExcutionLogDao;
import cn.bc.workflow.domain.ExcutionLog;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.*;
import org.activiti.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.jpa.JpaCallback;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.*;
import java.util.Map.Entry;

/**
 * 流转日志Dao接口的实现
 * 
 * @author dragon
 * 
 */
public class ExcutionLogDaoImpl extends HibernateCrudJpaDao<ExcutionLog>
		implements ExcutionLogDao, InitializingBean, ApplicationContextAware {
	private static Logger logger = LoggerFactory.getLogger(ExcutionLogDaoImpl.class);
	private ApplicationContext applicationContext;

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	private RepositoryService getRepositoryService() {
		return applicationContext.getBean(RepositoryService.class);
	}

	public HistoryService getHistoryService() {
		return applicationContext.getBean(HistoryService.class);
	}

	public FormService getFormService() {
		return applicationContext.getBean(FormService.class);
	}

	public TaskService getTaskService() {
		return applicationContext.getBean(TaskService.class);
	}

	public void afterPropertiesSet() throws Exception {
	}

	public ExcutionLog loadByTask(String taskId, String type) {
		return this
				.createQuery()
				.condition(
						new AndCondition().add(
								new EqualsCondition("taskInstanceId", taskId))
								.add(new EqualsCondition("type", type)))
				.singleResult();
	}

	public String findTaskFormKey(String taskId) {
		if (taskId == null || taskId.length() == 0)
			return null;
		String hql = "select id,formkey from bc_wf_excution_log where tid=? and type_=?";
		if (logger.isDebugEnabled()) {
			logger.debug("hql=" + hql);
			logger.debug("taskId=" + taskId);
		}
		List<Object[]> all = HibernateJpaNativeQuery.executeNativeSql(
				getJpaTemplate(), hql, new Object[] { taskId, "task_create" },
				null);
		if (all == null || all.isEmpty()) {
			return null;
		} else {
			return (String) all.get(0)[1];
		}
	}

	public Map<String, String> findTaskFormKeys(String processInstanceId) {
		if (processInstanceId == null || processInstanceId.length() == 0)
			return new LinkedHashMap<String, String>();
		String hql = "select tid, formkey from bc_wf_excution_log where pid=? and tid is not null and formkey is not null order by file_date";
		if (logger.isDebugEnabled()) {
			logger.debug("hql=" + hql);
			logger.debug("args=" + processInstanceId);
		}
		List<Object[]> all = HibernateJpaNativeQuery
				.executeNativeSql(getJpaTemplate(), hql,
						new Object[] { processInstanceId }, null);
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Object[] o : all) {
			map.put(o[0].toString(), o[1].toString());
		}
		return map;
	}

	public Map<String, Object> findTaskVariables(String taskId) {
		Date start = new Date();
		//if(true) return new HashMap<>();
		// activiti的流程变量存储规则：
		// 流传中的流程，所有任务的流程变量是放在act_ru_variable表中的，
		// 在act_hi_detail表中并不存在，知道流程结束后才会移动到act_hi_detail表中

		// 获取任务信息：用于判断流程的状态
		HistoricTaskInstance task = getHistoryService()
				.createHistoricTaskInstanceQuery().taskId(taskId)
				.singleResult();
		if (task == null) {
			throw new CoreException("can't find taskHistory: id=" + taskId);
		}
		boolean isCompletedTask = task.getEndTime() != null;
		if(logger.isInfoEnabled()) logger.info("findTaskVariables HistoricTaskInstance {}", DateUtils.getWasteTime(start));
		HistoricProcessInstance pi = getHistoryService()
				.createHistoricProcessInstanceQuery()
				.processInstanceId(task.getProcessInstanceId()).singleResult();
		if (pi == null) {
			throw new CoreException("can't find processInstanceHistory: id="
					+ task.getProcessInstanceId());
		}
		if(logger.isInfoEnabled()) logger.info("findTaskVariables HistoricProcessInstance {}", DateUtils.getWasteTime(start));

		// 参数容器初始化
		Map<String, Object> params = new LinkedHashMap<String, Object>();

		// 任务的参数
		params.put("ti_id", task.getId());
		params.put("ti_key", task.getTaskDefinitionKey());
		params.put("ti_deleteReason", task.getDeleteReason());
		params.put("ti_startTime", task.getStartTime());
		params.put("ti_endTime", task.getEndTime());
		params.put("ti_description", task.getDescription());
		params.put("ti_assignee", task.getAssignee());
		params.put("ti_owner", task.getOwner());
		params.put("ti_name", task.getName());
		params.put("ti_priority", task.getPriority());
		params.put("ti_dueDate", task.getDueDate());

		// 流程实例的一些参数
		params.put("pi_id", pi.getId());
		params.put("pi_businessKey", pi.getBusinessKey());
		params.put("pi_definitionId", pi.getProcessDefinitionId());
		params.put("pi_startUserId", pi.getStartUserId());
		params.put("pi_deleteReason", pi.getDeleteReason());

		// 流程定义的参数
		ProcessDefinition pd = getRepositoryService()
				.createProcessDefinitionQuery()
				.processDefinitionId(pi.getProcessDefinitionId())
				.singleResult();
		params.put("pd_id", pd.getId());
		params.put("pd_category", pd.getCategory());
		params.put("pd_deploymentId", pd.getDeploymentId());
		params.put("pd_key", pd.getKey());
		params.put("pd_name", pd.getName());
		if(logger.isInfoEnabled()) logger.info("findTaskVariables ProcessDefinition {}", DateUtils.getWasteTime(start));

		// 全局流程变量
			List<HistoricDetail> detail = getHistoryService()
				.createHistoricDetailQuery().processInstanceId(pi.getId())
				.variableUpdates().orderByTime().asc().list();
		HistoricVariableUpdate v;
		Map<String, Object> globalParams = new LinkedHashMap<String, Object>();
		for (HistoricDetail d : detail) {
			v = (HistoricVariableUpdate) d;
			if(v.getTaskId()  == null) {
				params.put(v.getVariableName(), v.getValue());
				globalParams.put(v.getVariableName(), v.getValue());
			}
		}
		params.put("global", globalParams);// 使用特殊的key记录全局流程变量
		logger.info("global.size={}", detail.size());
		if(logger.isInfoEnabled()) logger.info("findTaskVariables 全局HistoricDetail {}", DateUtils.getWasteTime(start));

		// 本地流程变量
		// detail = getHistoryService().createHistoricDetailQuery().taskId(taskId).variableUpdates().orderByTime().asc().list();
		int c = 0;
		for (HistoricDetail d : detail) {
			v = (HistoricVariableUpdate) d;
			if(taskId.equals(v.getTaskId())) {
				params.put(v.getVariableName(), v.getValue());
				c++;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("params0=" + params);
		}
		logger.info("local.size={}",c);
		if(logger.isInfoEnabled()) logger.info("findTaskVariables 本地HistoricDetail {}", DateUtils.getWasteTime(start));

		// 任务的表单属性:使用固定的前缀"f_"+属性的key作为变量的名称
		// 好像没有到表单属性，为了性能优化，先去掉 - dragon 2014-12-23
		/*
		String prefix = "";
		if (isCompletedTask) {// 从历史中查
			detail = getHistoryService().createHistoricDetailQuery()
					.taskId(taskId).formProperties().list();
			HistoricFormProperty fp;
			for (HistoricDetail hd : detail) {
				fp = (HistoricFormProperty) hd;
				params.put(prefix + fp.getPropertyId(), fp.getPropertyValue());
			}
		} else {// 从流转中查
			TaskFormData taskFormData = getFormService()
					.getTaskFormData(taskId);
			if (taskFormData != null
					&& taskFormData.getFormProperties() != null) {
				for (FormProperty fp : taskFormData.getFormProperties()) {
					params.put(prefix + fp.getId(), fp.getValue());
				}
			}
		}*/
		// params.put("f", detail != null ? detail : new
		// ArrayList<HistoricDetail>());
		if (logger.isDebugEnabled()) {
			logger.debug("formProperties.size={}", detail != null ? detail.size() : 0);
		}
		if(logger.isInfoEnabled()) logger.info("findTaskVariables 表单属性 {}", DateUtils.getWasteTime(start));

		// 转换特殊类型的变量的值
		for (Entry<String, Object> e : params.entrySet()) {
			convertSpecialKeyValue(e);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("params1=" + params);
		}
		if(logger.isInfoEnabled()) logger.info("findTaskVariables 表单属性convert {}", DateUtils.getWasteTime(start));

		return params;
	}

	public Object getTaskVariableLocal(final String taskId,
			final String variableName) {
		final String sql = "select id_,text_,var_type_ from act_hi_detail where type_ = 'VariableUpdate' and task_id_ = ? and name_ = ?";
		if (logger.isDebugEnabled()) {
			logger.debug("sql=" + sql);
			logger.debug("taskId=" + taskId);
			logger.debug("variableName=" + variableName);
		}
		List<Object[]> r = this.getJpaTemplate().execute(
				new JpaCallback<List<Object[]>>() {
					@SuppressWarnings("unchecked")
					public List<Object[]> doInJpa(EntityManager em)
							throws PersistenceException {
						javax.persistence.Query query = createSqlQuery(em, sql,
								new Object[] { taskId, variableName });
						return (List<Object[]>) query.getResultList();
					}
				});
		if (logger.isDebugEnabled())
			logger.debug("r=" + r);
		if (r == null || r.isEmpty()) {
			return null;
		} else {
			if (r.size() > 1) {
				logger.warn("more than one result return: size=" + r.size());
			}
			return r.get(0)[1];
		}
	}

	/**
	 * 特殊流程变量值的转换
	 * 
	 * @param v
	 */
	private void convertSpecialKeyValue(Entry<String, Object> e) {
		if (e.getKey().startsWith("list_") && e.getValue() instanceof String) {// 将字符串转化为List
			e.setValue(JsonUtils.toCollection((String) e.getValue()));
		} else if (e.getKey().startsWith("map_")
				&& e.getValue() instanceof String) {// 将字符串转化为Map
			e.setValue(JsonUtils.toMap((String) e.getValue()));
		} else if (e.getKey().startsWith("array_")
				&& e.getValue() instanceof String) {// 将字符串转化为数组
			e.setValue(JsonUtils.toArray((String) e.getValue()));
		}
	}
}
