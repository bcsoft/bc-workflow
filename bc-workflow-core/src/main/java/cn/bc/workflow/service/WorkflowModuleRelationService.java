package cn.bc.workflow.service;

import cn.bc.core.dao.CrudDao;
import cn.bc.workflow.domain.FlowStatus;
import cn.bc.workflow.domain.WorkflowModuleRelation;

import java.util.List;
import java.util.Map;

/**
 * 流程关系Service接口
 *
 * @author lbj
 * @modified dragon 2016-05-18
 */
public interface WorkflowModuleRelationService extends
  CrudDao<WorkflowModuleRelation> {

  /**
   * 通过mid和mtype 查找流程对应关系
   *
   * @param mid   模块ID
   * @param mtype 模块类型
   * @return 返回Map格式{mid:模块id， pid:流程实例id， startTime:发起时间， endTime:结束时间， name:流程名称，
   * key:流程编码， status:流程状态 1流转中 2暂停 3结束}
   */
  List<Map<String, Object>> findList(Long[] mid, String mtype);

  /**
   * 通过mid和mtype 查找流程对应的关系
   *
   * @param mid        模块ID
   * @param mtype      模块类型
   * @param globalKeys 查询全局参数的key
   * @return 返回Map格式{ pid:流程实例id， startTime:发起时间， endTime:结束时间， name:流程名称，
   * key:流程编码， status:流程状态 1流转中 2暂停 3结束
   * <p>
   * globalKey1：对应的流程全局参数的值， globalKey2：对应的流程全局参数的值，
   * globalKey3：对应的流程全局参数的值， ... }
   */
  List<Map<String, Object>> findList(Long mid, String mtype, String[] globalKeys);

  /**
   * 通过mid mtype key 查找流程对应的关系
   *
   * @param mid        模块ID
   * @param mtype      模块类型
   * @param Key        流程编码
   * @param globalKeys 查询全局参数的key
   * @return 返回Map格式{ pid:流程实例id， startTime:发起时间， endTime:结束时间， name:流程名称，
   * key:流程编码， status:流程状态 1流转中 2暂停 3结束
   * <p>
   * globalKey1：对应的流程全局参数的值， globalKey2：对应的流程全局参数的值，
   * globalKey3：对应的流程全局参数的值， ... }
   */
  List<Map<String, Object>> findList(Long mid, String mtype, String key, String[] globalKeys);

  /**
   * @param mtype模块类型
   * @param properties 流程中的变量
   * @param values     流程中的变量值
   * @param globalKeys 查询全局参数的key
   * @return返回Map格式{ pid:流程实例id， startTime:发起时间， endTime:结束时间， name:流程名称，
   * key:流程编码， status:流程状态 1流转中 2暂停 3结束
   * <p>
   * globalKey1：对应的流程全局参数的值， globalKey2：对应的流程全局参数的值，
   * globalKey3：对应的流程全局参数的值， ... }
   */
  List<Map<String, Object>> findList(String[] mtype, String[] properties,
                                     String[] values, String[] globalKeys);

  /**
   * 通过mid mtype key 返回是否存在流程关系
   *
   * @param mid   模块ID
   * @param mtype 模块类型
   * @return true：是 false：否
   */
  boolean hasRelation(Long mid, String mtype);

  /**
   * 通过mid mtype key 返回是否存在流程关系
   *
   * @param mid   模块ID
   * @param mtype 模块类型
   * @param key   流程编码
   * @return true：是 false：否
   */
  boolean hasRelation4Key(Long mid, String mtype, String key);

  /**
   * 流程实例Id 查找对象集合
   *
   * @param pid
   * @return
   */
  List<WorkflowModuleRelation> findList(String pid);

  /**
   * 获取指定业务信息的最新流转状态
   * <p>如果发起了多个流程，则获取最后发起流程的状态</p>
   *
   * @param mid   业务信息ID
   * @param mtype 业务类型
   * @return <code>FlowStatus</code>
   */
  FlowStatus getLastFlowStatus(Long mid, String mtype);
}
