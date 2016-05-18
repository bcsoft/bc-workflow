package cn.bc.workflow.dao;

import cn.bc.core.dao.CrudDao;
import cn.bc.workflow.domain.WorkflowModuleRelation;

import java.util.List;
import java.util.Map;

/**
 * 流程关系Dao接口
 *
 * @author lbj
 * @modified dragon 2016-05-18
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
	List<Map<String, Object>> findList(Long mid, String mtype, String key, String[] globalKeys);

	/**
	 * 查找流程关系集合
	 *
	 * @param mid
	 * @param mtype
	 * @param key
	 * @param globalKeys
	 * @return
	 */
	List<Map<String, Object>> findList(Long[] mid, String mtype, String key, String[] globalKeys);

	/**
	 * 通过mid mtype key 返回是否存在流程关系
	 *
	 * @param mid   模块ID
	 * @param mtype 模块类型
	 * @param key   流程编码
	 * @return true：是 false：否
	 */
	boolean hasRelation(Long mid, String mtype, String key);

	/**
	 * @param mtype模块类型
	 * @param propertys  流程中的变量
	 * @param values     流程中的变量值
	 * @param globalKeys 查询全局参数的key
	 * @return返回Map格式{ pid:流程实例id， startTime:发起时间， endTime:结束时间， name:流程名称，
	 * key:流程编码， status:流程状态 1流转中 2暂停 3结束
	 * <p>
	 * globalKey1：对应的流程全局参数的值， globalKey2：对应的流程全局参数的值，
	 * globalKey3：对应的流程全局参数的值， ... }
	 */
	List<Map<String, Object>> findList(String[] mtype, String[] propertys,
	                                   String[] values, String[] globalKeys);

	/**
	 * 获取指定业务信息对指定流程最新的流转结束
	 * <p>如果发起了多个流程，则获取最后发起流程的状态</p>
	 *
	 * @param mid   业务信息ID
	 * @param mtype 业务类型
	 * @return 没有发起流程返回 null，有则返回最新的流程状态：0-流转中，1-已结束
	 */
	String getLastFlowStatus(Long mid, String mtype);
}