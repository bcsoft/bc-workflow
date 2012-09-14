/**
 * 
 */
package cn.bc.workflow.deploy.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.bc.core.EntityImpl;
import cn.bc.docs.domain.Attach;

/**
 * 流程部署资源
 * 
 * @author wis
 */
@Entity
@Table(name = "BC_WF_DEPLOY_RESOURCE")
public class DeployResource extends EntityImpl {
	private static final long serialVersionUID = 1L;
	private static Log logger = LogFactory.getLog(DeployResource.class);
	public static final String ATTACH_TYPE = DeployResource.class.getSimpleName();
	
	/** 资源存储的子路径，开头末尾不要带"/" */
	public static String DATA_SUB_PATH = "workflow/deploy/resource";
	
//	/** 类型：JS */
//	public static final int TYPE_JS = 1;
//	/** 类型：FORM*/
//	public static final int TYPE_FORM = 2;
//	/** 类型：PNG*/
//	public static final int TYPE_PNG = 3;
//	/** 类型：CSS*/
//	public static final int TYPE_CSS = 4;
	
	private String uid;
	//private int type; // 1:JS,2:FORM,3:PNG,4:CSS
	private String type; //类型
	private String code;// 编码
	private String subject;// 标题
	private String path;// 物理文件保存的相对路径（相对于全局配置的app.data.realPath或app.data.subPath目录下的子路径，如"resource/20120820/xxxx.doc"）
	private Long size;// 文件的大小(单位为字节) 默认0
	private String source;//原始文件名
	private String desc;// 备注
	
	private Deploy deploy; //部署ID

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "PID", referencedColumnName = "ID")
	public Deploy getDeploy() {
		return deploy;
	}

	public void setDeploy(Deploy deploy) {
		this.deploy = deploy;
	}

	@Column(name = "UID_")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
	
//	@Column(name = "TYPE_")
//	public int getType() {
//		return type;
//	}
//
//	public void setType(int type) {
//		this.type = type;
//	}

	@Column(name = "TYPE_")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Column(name = "SIZE_")
	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	@Column(name = "DESC_")
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	/**
	 * 获取流程资源文件的附件长度
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
	 * 获取资源的文件流
	 * 
	 * @return
	 */
	@Transient
	public InputStream getInputStream() {
		// 读取文件流并返回
		String p = Attach.DATA_REAL_PATH + "/"
				+ DeployResource.DATA_SUB_PATH + "/" + this.getPath();
		File file = new File(p);
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			logger.warn("getInputStream 附件文件不存在:file=" + p);
			return null;
		}
	}


}
