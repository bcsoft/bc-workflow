package cn.bc.workflow.domain;

import javax.persistence.Entity;
import javax.persistence.Table;

import cn.bc.core.EntityImpl;

/**
 * 流程关系
 * <p>
 * 流程与模块间的关系domain
 * </p>
 * 
 * @author lbj
 * 
 */
@Entity
@Table(name = "BC_WF_MODULE_RELATION")
public class WorkflowModuleRelation extends EntityImpl {
	private static final long serialVersionUID = 1L;

	private Long mid;
	private String mtype;
	private String pid;
	
	public Long getMid() {
		return mid;
	}
	public void setMid(Long mid) {
		this.mid = mid;
	}
	public String getMtype() {
		return mtype;
	}
	public void setMtype(String mtype) {
		this.mtype = mtype;
	}
	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}
	
	
}
