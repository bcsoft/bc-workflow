/**
 * 
 */
package cn.bc.workflow.flowattach.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
import cn.bc.core.query.condition.impl.InCondition;
import cn.bc.core.query.condition.impl.IsNullCondition;
import cn.bc.core.query.condition.impl.OrderCondition;
import cn.bc.core.service.DefaultCrudService;
import cn.bc.workflow.flowattach.dao.FlowAttachDao;
import cn.bc.workflow.flowattach.domain.FlowAttach;

/**
 * 流程附加信息Service的实现
 * 
 * @author lbj
 */
public class FlowAttachServiceImpl extends DefaultCrudService<FlowAttach>
		implements FlowAttachService {
	protected final Log logger = LogFactory.getLog(getClass());

	private FlowAttachDao flowAttachDao;

	@Autowired
	public void setFlowAttachDao(FlowAttachDao flowAttachDao) {
		this.flowAttachDao = flowAttachDao;
		this.setCrudDao(flowAttachDao);
	}

	public List<FlowAttach> findByProcess(String processInstanceId) {
		return findByProcess(processInstanceId, false);
	}

	public List<FlowAttach> findByProcess(String processInstanceId,
			boolean includeTask) {
		Assert.notNull(processInstanceId);
		AndCondition and = new AndCondition();
		and.add(new EqualsCondition("pid", processInstanceId));
		if (!includeTask) {
			and.add(new IsNullCondition("tid"));
		}
		and.add(new OrderCondition("type", Direction.Asc).add("fileDate",
				Direction.Asc));
		return this.createQuery().condition(and).list();
	}

	public List<FlowAttach> findByTask(String[] taskIds) {
		if (taskIds == null || taskIds.length == 0)
			return new ArrayList<FlowAttach>();

		AndCondition and = new AndCondition();
		if (taskIds.length == 1) {
			and.add(new EqualsCondition("tid", taskIds[0]));
		} else {
			and.add(new InCondition("tid", taskIds));
		}
		and.add(new OrderCondition("type", Direction.Asc).add("fileDate",
				Direction.Asc));
		return this.createQuery().condition(and).list();
	}

	public List<FlowAttach> findByTask(String taskId) {
		if (taskId == null || taskId.length() == 0)
			return new ArrayList<FlowAttach>();

		return findByTask(new String[] { taskId });
	}

	public String getProcInstName(String pid) {
		return flowAttachDao.getProcInstName(pid);
	}
}