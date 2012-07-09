/**
 * 
 */
package cn.bc.workflow.flowattach.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.core.service.DefaultCrudService;
import cn.bc.workflow.flowattach.dao.FlowAttachDao;
import cn.bc.workflow.flowattach.domain.FlowAttach;

/**
 * 流程附加信息Service的实现
 * 
 * @author lbj
 */
public class FlowAttachServiceImpl extends DefaultCrudService<FlowAttach> implements FlowAttachService {
	protected final Log logger = LogFactory.getLog(getClass());

	private FlowAttachDao flowAttachDao;

	@Autowired
	public void setFlowAttachDao(FlowAttachDao flowAttachDao) {
		this.flowAttachDao = flowAttachDao;
		this.setCrudDao(flowAttachDao);
	}
	

}