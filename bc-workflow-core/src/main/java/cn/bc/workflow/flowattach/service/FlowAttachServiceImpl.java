/**
 *
 */
package cn.bc.workflow.flowattach.service;

import cn.bc.core.query.condition.Direction;
import cn.bc.core.query.condition.impl.*;
import cn.bc.core.service.DefaultCrudService;
import cn.bc.workflow.flowattach.dao.FlowAttachDao;
import cn.bc.workflow.flowattach.domain.FlowAttach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程附加信息Service的实现
 *
 * @author lbj
 */
@Service("flowAttachService")
public class FlowAttachServiceImpl extends DefaultCrudService<FlowAttach> implements FlowAttachService {
	private FlowAttachDao flowAttachDao;

	@Autowired
	public void setFlowAttachDao(FlowAttachDao flowAttachDao) {
		this.flowAttachDao = flowAttachDao;
		this.setCrudDao(flowAttachDao);
	}

	public List<FlowAttach> findByProcess(String processInstanceId) {
		return findByProcess(processInstanceId, false);
	}

	public List<FlowAttach> findByProcess(String processInstanceId, boolean includeTask) {
		return findByProcess(processInstanceId, null, includeTask);
	}

	public List<FlowAttach> findCommentsByProcess(String processInstanceId, boolean includeTask) {
		return findByProcess(processInstanceId, new Integer[]{FlowAttach.TYPE_COMMENT}, includeTask);
	}

	public List<FlowAttach> findAttachsByProcess(String processInstanceId, boolean includeTask) {
		return findByProcess(processInstanceId, new Integer[]{FlowAttach.TYPE_ATTACHMENT}, includeTask);
	}

	public List<FlowAttach> findByProcess(String processInstanceId, Integer[] types, boolean includeTask) {
		Assert.notNull(processInstanceId);
		AndCondition and = new AndCondition();
		and.add(new EqualsCondition("pid", processInstanceId));
		if (!includeTask) {
			and.add(new OrCondition().add(new IsNullCondition("tid"))
					.add(new EqualsCondition("tid", "")).setAddBracket(true));
		}
		if (types != null && types.length > 0) {
			if (types.length == 1) {
				and.add(new EqualsCondition("type", types[0]));
			} else {
				and.add(new InCondition("type", types));
			}
		}
		and.add(new OrderCondition("type", Direction.Asc).add("fileDate",
				Direction.Asc));
		return this.createQuery().condition(and).list();
	}

	public List<FlowAttach> findByTask(String[] taskIds) {
		return findByTask(taskIds, null);
	}

	public List<FlowAttach> findCommentsByTask(String[] taskIds) {
		return findByTask(taskIds, new Integer[]{FlowAttach.TYPE_COMMENT});
	}

	public List<FlowAttach> findByTask(String[] taskIds, Integer[] types) {
		if (taskIds == null || taskIds.length == 0)
			return new ArrayList<FlowAttach>();

		AndCondition and = new AndCondition();
		if (taskIds.length == 1) {
			and.add(new EqualsCondition("tid", taskIds[0]));
		} else {
			and.add(new InCondition("tid", taskIds));
		}
		if (types != null && types.length > 0) {
			if (types.length == 1) {
				and.add(new EqualsCondition("type", types[0]));
			} else {
				and.add(new InCondition("type", types));
			}
		}
		and.add(new OrderCondition("tid", Direction.Asc).add("fileDate",
				Direction.Asc));
		return this.createQuery().condition(and).list();
	}

	@Override
	public List<FlowAttach> findByTask(String taskId, int type) {
		return this.findByTask(new String[]{taskId}, new Integer[]{type});
	}

	public String getProcInstName(String pid) {
		return flowAttachDao.getProcInstName(pid);
	}

	@Override
	public void updateAttachToSubProcess(Long[] ids, String subProcessInstanceId, String subProcessTaskId) {
		this.flowAttachDao.updateAttachToSubProcess(ids, subProcessInstanceId, subProcessTaskId);
	}

	public List<FlowAttach> findAttachsByTask(String[] taskIds) {
		return findByTask(taskIds, new Integer[]{FlowAttach.TYPE_ATTACHMENT});
	}
}