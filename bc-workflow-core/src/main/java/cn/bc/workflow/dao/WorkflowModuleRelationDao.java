package cn.bc.workflow.dao;

import java.util.List;
import java.util.Map;

import cn.bc.core.dao.CrudDao;
import cn.bc.workflow.domain.WorkflowModuleRelation;

/**
 * 流程关系Dao接口
 * 
 * @author lbj
 * 
 */
public interface WorkflowModuleRelationDao extends CrudDao<WorkflowModuleRelation> {
	
	/**
	 * 查找流程关系集合
	 * 
	 * @param mid
	 * @param mtype
	 * @param globalKeys
	 * @return
	 */
	List<Map<String,Object>> findList(Long mid,String mtype,String[] globalKeys);
}
