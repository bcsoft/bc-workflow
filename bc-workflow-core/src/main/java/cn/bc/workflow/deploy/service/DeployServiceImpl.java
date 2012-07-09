package cn.bc.workflow.deploy.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.engine.RepositoryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.BCConstants;
import cn.bc.core.service.DefaultCrudService;
import cn.bc.docs.domain.Attach;
import cn.bc.workflow.deploy.dao.DeployDao;
import cn.bc.workflow.deploy.domain.Deploy;

/**
 * Service接口的实现
 * 
 * @author wis
 * 
 */
public class DeployServiceImpl extends DefaultCrudService<Deploy> implements
		DeployService {
	@SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(DeployServiceImpl.class);
	@Autowired
	private RepositoryService repositoryService;
	
	private DeployDao deployDao;
	
	@Autowired
	public void setDeployDao(DeployDao deployDao) {
		this.deployDao = deployDao;
		this.setCrudDao(deployDao);
	}

	public Deploy loadByCode(String code) {
		return this.deployDao.loadByCode(code);
	}

	public boolean isUniqueCodeAndVersion(Long currentId, String code,String version) {
		return this.deployDao.isUniqueCodeAndVersion(currentId, code,version);
	}

	public void saveTpl(Deploy deploy) {
		Deploy oldTpl= this.deployDao.loadByCodeAndId(deploy.getCode(),deploy.getId());
		if(oldTpl!=null){
			oldTpl.setStatus(BCConstants.STATUS_DISABLED);
			this.deployDao.save(oldTpl);
		}
		this.deployDao.save(oldTpl);
	}

	public List<Map<String, String>> findCategoryOption() {
		return this.deployDao.findCategoryOption();
	}

	public Long isReleased(Long excludeId) {
		return this.deployDao.isReleased(excludeId);
	}

	/**
	 * 发布吧部署流程需要部署流程id,标题,物理文件路径,原文件名称 (XML)
	 * @param excludeId
	 * @param subject
	 * @param source
	 * @param path
	 */
	public void deployRelease4XML(Long excludeId, String subject,String source,String path) {
		// 获取xml文件流
		InputStream xmlFile = this.getClass().getResourceAsStream(
				Attach.DATA_REAL_PATH+"/workflow/deploy/"+path);
		// 发布xml
		org.activiti.engine.repository.Deployment d = repositoryService
				.createDeployment().name(subject)
				.addInputStream(source, xmlFile).deploy();
		
		if(null != d){//发布成功 
			Deploy deploy = this.deployDao.load(excludeId);
			deploy.setStatus(Deploy.STATUS_RELEASED); //设置为发布状态
			this.deployDao.save(deploy);//保存
		}
	}

	/**
	 * 发布吧部署流程需要部署流程id,标题,物理文件路径,原文件名称 (BAR)
	 * @param excludeId
	 * @param subject
	 * @param source
	 * @param path
	 */
	public void deployRelease4BAR(Long excludeId, String subject,
			String source, String path) {
		// 获取bar包
		InputStream barFile = this.getClass().getResourceAsStream(
				Attach.DATA_REAL_PATH+"/workflow/deploy/"+path);
		ZipInputStream inputStream = new ZipInputStream(barFile);
		// 发布bar
		
		org.activiti.engine.repository.Deployment d = repositoryService
				.createDeployment().name(subject)
				.addZipInputStream(inputStream).deploy();
		
		if(null != d){//发布成功 
			Deploy deploy = this.deployDao.load(excludeId);
			deploy.setStatus(Deploy.STATUS_RELEASED); //设置为发布状态
			this.deployDao.save(deploy);//保存
		}
	}

}
