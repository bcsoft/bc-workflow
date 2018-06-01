/**
 *
 */
package cn.bc.workflow.deploy.domain;

import cn.bc.docs.domain.Attach;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.domain.ActorHistory;
import cn.bc.identity.domain.RichFileEntityImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Set;

/**
 * 流程部署
 *
 * @author wis
 */
@Entity
@Table(name = "BC_WF_DEPLOY")
@Inheritance(strategy = InheritanceType.JOINED)
public class Deploy extends RichFileEntityImpl {
  private static final long serialVersionUID = 1L;
  private static Log logger = LogFactory.getLog(Deploy.class);
  public static final String ATTACH_TYPE = Deploy.class.getSimpleName();
  /**
   * 模板存储的子路径，开头末尾不要带"/"
   */
  public static String DATA_SUB_PATH = "workflow/deploy";

  /**
   * 状态：使用中
   */
  public static final int STATUS_USING = 0;
  /**
   * 状态：已停用
   */
  public static final int STATUS_STOPPED = 1;

  /**
   * 类型：XML
   */
  public static final int TYPE_XML = 0;
  /**
   * 类型：BAR
   */
  public static final int TYPE_BAR = 1;

  private String deploymentId; // 对应act_re_deployment 的id_
  private int type; // 0:XML,1:BRA
  private String orderNo;// 排序号
  private String code;// 编码
  private String path;// 物理文件保存的相对路径（相对于全局配置的app.data.realPath或app.data.subPath目录下的子路径，如"2011/bulletin/xxxx.doc"）
  private String subject;// 标题
  private String desc;// 备注
  private String version;// 版本号
  private String category;// 所属分类
  private Long size;// 文件的大小(单位为字节) 默认0
  private String source;//原始文件名
  private Calendar deployDate;// 最后部署时间
  private ActorHistory deployer;// 最后部署人

  private Set<Actor> users;// 使用人，为空代表所有人均可使用
  private Set<DeployResource> resources;// 使用人，为空代表所有人均可使用

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "BC_WF_DEPLOY_ACTOR", joinColumns = @JoinColumn(name = "DID", referencedColumnName = "ID"), inverseJoinColumns = @JoinColumn(name = "AID", referencedColumnName = "ID"))
  @OrderBy("orderNo asc")
  public Set<Actor> getUsers() {
    return users;
  }

  public void setUsers(Set<Actor> users) {
    this.users = users;
  }

  @OneToMany(mappedBy = "deploy", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy(value = "id asc")
  public Set<DeployResource> getResources() {
    return resources;
  }

  public void setResources(Set<DeployResource> resources) {
    this.resources = resources;
  }

  @Column(name = "DEPLOYMENT_ID")
  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @Column(name = "TYPE_")
  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  @Column(name = "SIZE_")
  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Column(name = "VERSION_")
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Column(name = "ORDER_")
  public String getOrderNo() {
    return orderNo;
  }

  public void setOrderNo(String orderNo) {
    this.orderNo = orderNo;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  @Column(name = "DESC_")
  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  @Column(name = "DEPLOY_DATE")
  public Calendar getDeployDate() {
    return deployDate;
  }

  public void setDeployDate(Calendar deployDate) {
    this.deployDate = deployDate;
  }

  @ManyToOne(fetch = FetchType.EAGER, optional = true)
  @JoinColumn(name = "DEPLOYER_ID", referencedColumnName = "ID")
  public ActorHistory getDeployer() {
    return deployer;
  }

  public void setDeployer(ActorHistory deployer) {
    this.deployer = deployer;
  }


  /**
   * 获取流程部署文件的附件长度
   * <p>
   * 如果是自定义文本内容返回此内容字节的长度,如果是附件类型返回附件长度
   * </p>
   *
   * @return
   */
  @Transient
  public long getSizeEx() {
    String p = Attach.DATA_REAL_PATH + "/" + DATA_SUB_PATH + "/"
      + this.getPath();
    File file = new File(p);
    return file.length();
  }

  /**
   * 获取部署的文件流
   *
   * @return
   */
  @Transient
  public InputStream getInputStream() {
    // 读取文件流并返回
    String p = Attach.DATA_REAL_PATH + "/"
      + Deploy.DATA_SUB_PATH + "/" + this.getPath();
    File file = new File(p);
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      logger.warn("getInputStream 附件文件不存在:file=" + p);
      return null;
    }
  }

}
