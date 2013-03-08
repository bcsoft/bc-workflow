package cn.bc.workflow.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.core.service.DefaultCrudService;
import cn.bc.workflow.dao.WorkflowModuleRelationDao;
import cn.bc.workflow.domain.WorkflowModuleRelation;

/**
 * 流程关系Service接口实现
 * 
 * @author lbj
 * 
 */
public class WorkflowModuleRelationServiceImpl extends DefaultCrudService<WorkflowModuleRelation>
		implements WorkflowModuleRelationService {

	private WorkflowModuleRelationDao workflowModuleRelationDao;

	@Autowired
	public void setModuleRelationDao(WorkflowModuleRelationDao workflowModuleRelationDao) {
		this.workflowModuleRelationDao = workflowModuleRelationDao;
		this.setCrudDao(workflowModuleRelationDao);
	}

	public List<Map<String, Object>> findList(Long mid, String mtype,String[] globalKeys) {
		return this.workflowModuleRelationDao.findList(mid, mtype,null,globalKeys);
	}

	public List<Map<String, Object>> findList(Long mid, String mtype,
			String key, String[] globalKeys) {
		return this.workflowModuleRelationDao.findList(mid, mtype,key,globalKeys);
	}

	public boolean hasRelation(Long mid, String mtype) {
		return this.workflowModuleRelationDao.hasRelation(mid, mtype, null);
	}

	public boolean hasRelation4Key(Long mid, String mtype, String key) {
		return this.workflowModuleRelationDao.hasRelation(mid, mtype, key);
	}

}
