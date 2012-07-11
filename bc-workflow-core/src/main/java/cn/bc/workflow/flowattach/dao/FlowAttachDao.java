/**
 * 
 */
package cn.bc.workflow.flowattach.dao;

import cn.bc.core.dao.CrudDao;
import cn.bc.workflow.flowattach.domain.FlowAttach;


/**
 * 流程附加信息Dao
 * 
 * @author lbj
 */
public interface FlowAttachDao extends CrudDao<FlowAttach> {

	/**
	 * 获取流程实例名称
	 * @param pid 流程实例id
	 * @return
	 */
	public String getProcInstName(String pid);
}