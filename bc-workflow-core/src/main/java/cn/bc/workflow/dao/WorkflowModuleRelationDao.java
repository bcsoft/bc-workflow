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
	 * @param key
	 * @param globalKeys
	 * @return
	 */
	List<Map<String,Object>> findList(Long mid,String mtype,String key,String[] globalKeys);
	
	/**
	 * 通过mid mtype key 返回是否存在流程关系
	 * @param mid 模块ID
	 * @param mtype 模块类型
	 * @param key 流程编码
	 * @return true：是 false：否
	 */
	boolean hasRelation(Long mid,String mtype,String key);
}
