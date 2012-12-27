package cn.bc.workflow.service;

import java.util.List;
import java.util.Map;

import cn.bc.core.dao.CrudDao;
import cn.bc.workflow.domain.WorkflowModuleRelation;

/**
 * 流程关系Service接口
 * 
 * @author lbj
 * 
 */
public interface WorkflowModuleRelationService extends CrudDao<WorkflowModuleRelation> {
	
	/**
	 * 通过mid和mtype 查找流程对应的关系
	 * 
	 * @param mid 模块ID
	 * @param mtype 模块类型
	 * @param globalKeys 查询全局参数的key
	 * @return 返回Map格式{
	 * 				pid:流程实例id，
	 * 				startTime:发起时间，
	 * 				endTime:结束时间，
	 * 				name:流程名称，
	 * 				key:流程编码，
	 * 				status:流程状态 1流转中 2暂停 3结束
	 * 				
	 * 				globalKey1：对应的流程全局参数的值，
	 * 				globalKey2：对应的流程全局参数的值，
	 * 				globalKey3：对应的流程全局参数的值，
	 * 				...
	 * 				}
	 */
	List<Map<String,Object>> findList(Long mid,String mtype,String[] globalKeys);
}
