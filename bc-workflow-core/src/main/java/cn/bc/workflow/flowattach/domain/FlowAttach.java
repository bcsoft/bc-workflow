/**
 * 
 */
package cn.bc.workflow.flowattach.domain;

import java.io.File;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import cn.bc.docs.domain.Attach;
import cn.bc.identity.domain.FileEntityImpl;

/**
 * 流程附加信息
 * 
 * @author lbj
 */
@Entity
@Table(name = "BC_WF_ATTACH")
public class FlowAttach extends FileEntityImpl {
	private static final long serialVersionUID = 1L;
	public static String ATTACH_TYPE=FlowAttach.class.getSimpleName();
	/** 附件存储的子路径，开头末尾不要带"/" */
	public static String DATA_SUB_PATH = "workflow/attachment";
	
	/**附件**/
	public static final int TYPE_ATTACHMENT=1;
	/**意见**/
	public static final int TYPE_COMMENT=2;

	private int Type;//类型：1-附件，2-意见
	private String procId;
	private String tid;//任务id
	private String path;////附件路径，物理文件保存的相对路径（相对于全局配置的app.data.realPath或app.data.subPath目录下的子路径，如"workflow/attachment/201207/xxxx.doc"）
	private String subject;// 标题
	private boolean common;// 是否为公共信息，true是，false任务信息
	private String desc;// 备注
	private Long size;// 文件的大小(单位为字节) 默认0
	private String ext;// 扩展名
	
	private String uid;
	
	@Column(name = "UID_")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
	
	@Column(name="TYPE_")
	public int getType() {
		return Type;
	}
	public void setType(int type) {
		Type = type;
	}
	
	@Column(name="PROC_INST_ID_")
	public String getProcId() {
		return procId;
	}
	public void setProcId(String procId) {
		this.procId = procId;
	}
	@Column(name="TASK_ID_")
	public String getTid() {
		return tid;
	}
	public void setTid(String tid) {
		this.tid = tid;
	}
	@Column(name="PATH_")
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
	@Column(name="COMMON")
	public boolean isCommon() {
		return common;
	}
	public void setCommon(boolean common) {
		this.common = common;
	}
	
	@Column(name="DESC_")
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	@Column(name="SIZE_")
	public Long getSize() {
		return size;
	}
	public void setSize(Long size) {
		this.size = size;
	}
	@Column(name="EXT_")
	public String getExt() {
		return ext;
	}
	public void setExt(String ext) {
		this.ext = ext;
	}

	/**
	 * 获取附件长度
	 * @return
	 */
	@Transient
	public long getSizeEx() {
		String p = Attach.DATA_REAL_PATH + "/" + DATA_SUB_PATH + "/"
				+ this.getPath();
		File file = new File(p);
		return file.length();
	}
}
