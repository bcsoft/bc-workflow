/**
 * 
 */
package cn.bc.workflow.flowattach.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.AndCondition;
import cn.bc.core.query.condition.impl.EqualsCondition;
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

	public List<FlowAttach> find(String processInstanceId, String taskId) {
		Assert.notNull(processInstanceId);
		AndCondition and = new AndCondition();
		and.add(new EqualsCondition("pid", processInstanceId));
		if (taskId != null && taskId.length() > 0) {
			and.add(new EqualsCondition("tid", taskId));
		}
		and.add(new OrderCondition("type", Direction.Asc).add("fileDate",
				Direction.Asc));
		return this.createQuery().condition(and).list();
	}

	public String getProcInstName(String pid) {
		return flowAttachDao.getProcInstName(pid);
	}
}