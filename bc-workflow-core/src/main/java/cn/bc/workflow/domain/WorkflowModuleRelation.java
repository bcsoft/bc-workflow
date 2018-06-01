package cn.bc.workflow.domain;

import cn.bc.core.EntityImpl;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 流程关系
 * <p>
 * 流程与模块间的关系domain
 * </p>
 *
 * @author lbj
 */
@Entity
@Table(name = "BC_WF_MODULE_RELATION")
public class WorkflowModuleRelation extends EntityImpl {
  private static final long serialVersionUID = 1L;

  private Long mid;//模块id
  private String mtype;//模块类型
  private String pid;//流程实例id

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
