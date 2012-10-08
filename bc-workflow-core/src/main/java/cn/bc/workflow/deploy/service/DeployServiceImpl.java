package cn.bc.workflow.deploy.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import cn.bc.BCConstants;
import cn.bc.core.exception.CoreException;
import cn.bc.core.service.DefaultCrudService;
import cn.bc.docs.domain.Attach;
import cn.bc.identity.domain.Actor;
import cn.bc.identity.service.IdGeneratorService;
import cn.bc.identity.web.SystemContext;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.log.domain.OperateLog;
import cn.bc.log.service.OperateLogService;
import cn.bc.template.domain.TemplateParam;
import cn.bc.workflow.deploy.dao.DeployDao;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.domain.DeployResource;

/**
 * Service接口的实现
 * 
 * @author wis
 * 
 */
public class DeployServiceImpl extends DefaultCrudService<Deploy> implements
		DeployService {
	private static Log logger = LogFactory.getLog(DeployServiceImpl.class);
	private OperateLogService operateLogService;
	private IdGeneratorService idGeneratorService;// 用于生成uid的服务

	@Autowired
	public void setOperateLogService(OperateLogService operateLogService) {
		this.operateLogService = operateLogService;
	}

	@Autowired
	private RepositoryService repositoryService;

	private DeployDao deployDao;

	@Autowired
	public void setDeployDao(DeployDao deployDao) {
		this.deployDao = deployDao;
		this.setCrudDao(deployDao);
	}

	@Autowired
	public void setIdGeneratorService(IdGeneratorService idGeneratorService) {
		this.idGeneratorService = idGeneratorService;
	}

	public Deploy loadByCode(String code) {
		return this.deployDao.loadByCode(code);
	}

	public boolean isUniqueCodeAndVersion(Long currentId, String code,
			String version) {
		return this.deployDao.isUniqueCodeAndVersion(currentId, code, version);
	}

	public List<Map<String, String>> findCategoryOption() {
		return this.deployDao.findCategoryOption();
	}

	public Long isReleased(Long excludeId) {
		return this.deployDao.isReleased(excludeId);
	}

	/**
	 * 发布部署流程需要部署流程id
	 * 
	 * @param id
	 */
	public void dodeployRelease(Long id) {
		try {
			// 判断发布类型
			Deploy deploy = this.deployDao.load(id);
			Deployment d = null;// 流程部署记录
			if (deploy.getType() == Deploy.TYPE_XML) {// 发布XML
				// 获取xml文件流
				InputStream xmlFile = new FileInputStream(Attach.DATA_REAL_PATH
						+ "/" + Deploy.DATA_SUB_PATH + "/" + deploy.getPath());
				// 发布xml
				d = repositoryService.createDeployment()
						.name(deploy.getSource())
						.addInputStream(deploy.getSource(), xmlFile).deploy();
				xmlFile.close(); // 释放文件
			} else if (deploy.getType() == Deploy.TYPE_BAR) {// 发布BAR
				InputStream barFile;
				barFile = new FileInputStream(Attach.DATA_REAL_PATH + "/"
						+ Deploy.DATA_SUB_PATH + "/" + deploy.getPath());
				ZipInputStream inputStream = new ZipInputStream(barFile);

				// 发布bar
				d = repositoryService.createDeployment()
						.name(deploy.getSource())
						.addZipInputStream(inputStream).deploy();
				inputStream.close();// 释放文件
			}

			if (null != d) {// 发布成功
				// 保存deploymentId
				deploy.setDeploymentId(d.getId());
				// 设置为发布状态
				deploy.setStatus(Deploy.STATUS_USING);
				// 设置最后发布信息
				deploy.setDeployer(SystemContextHolder.get().getUserHistory());
				deploy.setDeployDate(Calendar.getInstance());
				this.deployDao.save(deploy);// 保存

				// 记录取消发布日志
				this.operateLogService.saveWorkLog(
						Deploy.class.getSimpleName(),
						deploy.getId().toString(), "发布" + deploy.getSubject()
								+ "的流程信息", null, "deploy");
			} else {
				throw new CoreException("发布失败：id=" + id);
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new CoreException(e);
		}
	}

	/**
	 * 取消部署需要部署流程id,isCascade(是否级联取消)
	 * 
	 * @param deploymentId
	 * @param isCascade
	 */
	public void dodeployCancel(Long deploymentId, Boolean isCascade) {
		// 判断发布类型
		Deploy deploy = this.deployDao.load(deploymentId);
		if (null != deploy && null != deploy.getDeploymentId()) {
			if (isCascade) {
				repositoryService.deleteDeployment(deploy.getDeploymentId(),
						true);// 级联删除流程
			} else {
				repositoryService.deleteDeployment(deploy.getDeploymentId());// 删除流程
			}

			deploy.setStatus(Deploy.STATUS_STOPPED);
			// 设置最后取消信息
			deploy.setDeployer(SystemContextHolder.get().getUserHistory());
			deploy.setDeployDate(Calendar.getInstance());
			// 清空与 [act_re_deployment]表外键关联的id
			deploy.setDeploymentId(null);

			this.deployDao.save(deploy);// 保存

			// 记录取消发布日志
			this.operateLogService.saveWorkLog(Deploy.class.getSimpleName(),
					deploy.getId().toString(), "取消发布" + deploy.getSubject()
							+ "的流程信息", null, "undeploy");
		}
	}

	@Override
	public void delete(Serializable id) {
		Deploy entity = this.deployDao.load(id);
		if(entity.getStatus() == Deploy.STATUS_USING){
			// 先级联取消发布
			this.dodeployCancel((Long) id, true);
		}
		// 删除流转日志、意见、附件 TODO

		// 后彻底删除
		super.delete(id);
		
		// 记录删除部署日志
		this.operateLogService.saveWorkLog(Deploy.class.getSimpleName(),
				entity.getId().toString(), "删除部署" + entity.getSubject()
						+ "的流程信息", null, OperateLog.OPERATE_DELETE);
	}

	@Override
	public void delete(Serializable[] ids) {
		for (Serializable id : ids) {
			super.delete(id);
		}
	}
	
	public void dodeployStop(Long excludeId) {
		Deploy entity = this.deployDao.load(excludeId);
		entity.setStatus(Deploy.STATUS_STOPPED);
		entity = this.deployDao.save(entity);
		
		this.operateLogService.saveWorkLog(Deploy.class.getSimpleName(),
				entity.getId().toString(), "停用" + entity.getSubject()
				+ "的流程信息", null, "stopped");
	}


	/**
	 * 通过流程id判断此信息是否已发起
	 * 
	 * @param excludeId
	 * @return
	 */
	public Long isStarted(String deploymentId) {
		return this.deployDao.isStarted(deploymentId);
	}

	@Override
	public Deploy save(Deploy entity) {
		boolean isNew = entity.isNew();
		entity = this.deployDao.save(entity);
		if (isNew) {
			// 记录新建日志
			this.operateLogService.saveWorkLog(Deploy.class.getSimpleName(),
					entity.getId().toString(), "新建" + entity.getSubject()
							+ "的流程信息", null, OperateLog.OPERATE_CREATE);
		} else {
			if (entity.getStatus() != BCConstants.STATUS_DRAFT){
				// 记录更新日志
				this.operateLogService.saveWorkLog(Deploy.class.getSimpleName(),
						entity.getId().toString(), "维护" + entity.getSubject()
						+ "的流程信息", null, "maintenance");
			}
		}
		return entity;
	}

	/**
	 * 判断指定的编码与版本号是否唯一
	 * 
	 * @param currentId
	 *            当前模板的id
	 * @param codes
	 *            当前模板要使用的编码列表
	 * @return
	 */
	public ArrayList<Object> isUniqueResourceCodeAndExtCheck(Long id,
			String codes) {
		return this.deployDao.isUniqueResourceCodeAndExtCheck(id, codes);
	}

	/**
	 * 通过流程部署记录id和流程编码和部署资源编码查找对应部署资源
	 * 
	 * @param dmId
	 * @param wfCode
	 * @param resCode
	 * @return
	 */
	public DeployResource findDeployResourceByDmIdAndwfCodeAndresCode(
			String dmId, String wfCode, String resCode) {
		return this.deployDao.findDeployResourceByDmIdAndwfCodeAndresCode(dmId,
				wfCode, resCode);
	}

	public DeployResource findDeployResourceCode(String deployCode) {
		return this.deployDao.findDeployResourceCode(deployCode);
	}

	public Deploy doCopyDeploy(Long id) {
		Deploy oldDeploy = this.deployDao.load(id);
		if (oldDeploy == null)
			throw new CoreException("要处理的流程部署已不存在！deployId=" + id);

		// 复制流程部署
		Deploy newDeploy = new Deploy();
		try {
			BeanUtils.copyProperties(oldDeploy, newDeploy);
		} catch (Exception e) {
			throw new CoreException("复制流程部署信息错误！", e);
		}

		// 创建人,修改人,发布人信息
		SystemContext context = SystemContextHolder.get();

		newDeploy.setFileDate(Calendar.getInstance());
		newDeploy.setAuthor(context.getUserHistory());
		newDeploy.setModifiedDate(Calendar.getInstance());
		newDeploy.setModifier(context.getUserHistory());
		newDeploy.setDeployer(null);
		newDeploy.setDeployDate(null);

		// --- 复制新的部署附件  ---
		// 资源文件扩展名
		String extension=StringUtils.getFilenameExtension(oldDeploy.getPath());
		// 声明当前日期时间
		Calendar now = Calendar.getInstance();
		// 文件存储的相对路径（年月），避免超出目录内文件数的限制
		String subFolder = new SimpleDateFormat("yyyyMM").format(now.getTime());
		// 上传文件存储的绝对路径
		String appRealDir=Attach.DATA_REAL_PATH+"/"+Deploy.DATA_SUB_PATH;
		// 所保存文件所在的目录的绝对路径名
		String realFileDir=appRealDir+"/"+subFolder;
		// 不含路径的文件名
		String fileName = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(now.getTime()) + "." + extension;
		// 所保存文件的绝对路径名
		String realFilePath=realFileDir+"/"+fileName;
		// 构建文件要保存到的目录
		File _fileDir = new File(realFileDir);
		if (!_fileDir.exists()) {
			if (logger.isFatalEnabled()) 
				logger.fatal("mkdir=" + realFileDir);
			_fileDir.mkdirs();
		}
		// 直接复制附件
		if (logger.isInfoEnabled())
			logger.info("pure copy file");
		// 复制开始
		try{
			FileCopyUtils.copy(oldDeploy.getInputStream(), new FileOutputStream(
					realFilePath));
		}catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		
		// 流程部署信息
		newDeploy.setDeploymentId(null);// act部署记录id
		newDeploy.setId(null);
		newDeploy.setStatus(BCConstants.STATUS_DRAFT); // 状态草稿
		newDeploy.setUid(this.idGeneratorService.next(Deploy.ATTACH_TYPE));// uid
		newDeploy.setId(null);
		newDeploy.setPath(subFolder+"/"+fileName);
		// 设置新版本号
		String[] version = oldDeploy.getVersion().split("\\.");
		int major = Integer.parseInt(version[0]);
		String result = (major + 1) + ".0";
		newDeploy.setVersion(result);
		
		// 流程部署与使用人关系处理
		Set<Actor> newActorSet = new LinkedHashSet<Actor>();
		if(oldDeploy.getUsers() != null && oldDeploy.getUsers().size() > 0){
			for(Actor actor : oldDeploy.getUsers()){
				newActorSet.add(actor);
			}
		}
		newDeploy.setUsers(newActorSet);
		
		// 流程部署与资源关系处理
		Set<DeployResource> newResourceSet = new LinkedHashSet<DeployResource>();
		if (oldDeploy.getResources() != null
				&& oldDeploy.getResources().size() > 0) {
			DeployResource newDr;
			for (DeployResource oldDr : oldDeploy.getResources()) {
				newDr = new DeployResource();
				// --- 复制新的流程资源 ---
				try {
					BeanUtils.copyProperties(oldDr, newDr);
				} catch (Exception e) {
					throw new CoreException("复制流程资源信息错误！", e);
				}
				
				// --- 复制新的资源附件  ---
				// 资源文件扩展名
				String extensionDr=StringUtils.getFilenameExtension(oldDr.getPath());
				// 声明当前日期时间
				Calendar nowDr = Calendar.getInstance();
				// 文件存储的相对路径（年月），避免超出目录内文件数的限制
				String subFolderDr = new SimpleDateFormat("yyyyMM").format(nowDr.getTime());
				// 上传文件存储的绝对路径
				String appRealDirDr=Attach.DATA_REAL_PATH+"/"+DeployResource.DATA_SUB_PATH;
				// 所保存文件所在的目录的绝对路径名
				String realFileDirDr=appRealDirDr+"/"+subFolderDr;
				// 不含路径的文件名
				String fileNameDr = new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(nowDr.getTime()) + "." + extensionDr;
				// 所保存文件的绝对路径名
				String realFilePathDr=realFileDirDr+"/"+fileNameDr;
				// 构建文件要保存到的目录
				File _fileDirDr = new File(realFileDirDr);
				if (!_fileDirDr.exists()) {
					if (logger.isFatalEnabled()) 
						logger.fatal("mkdir=" + realFileDirDr);
					_fileDirDr.mkdirs();
				}
				// 直接复制附件
				if (logger.isInfoEnabled())
					logger.info("pure copy file");
				// 复制开始
				try{
					FileCopyUtils.copy(oldDr.getInputStream(), new FileOutputStream(
							realFilePathDr));
				}catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
				
				//设置新的流程资源信息
				newDr.setDeploy(newDeploy);
				newDr.setId(null);
				newDr.setUid(this.idGeneratorService.next(DeployResource.ATTACH_TYPE));
				newDr.setPath(subFolderDr+"/"+fileNameDr);
				
				// --- 复制新的模板参数---
				Set<TemplateParam> newTemplateParamSet = new LinkedHashSet<TemplateParam>();
				if (oldDr.getParams() != null && oldDr.getParams().size() > 0) {
					for(TemplateParam param : oldDr.getParams()){
						newTemplateParamSet.add(param);
					}
				}
				newDr.setParams(newTemplateParamSet);
				
				//设置newResourceSet
				newResourceSet.add(newDr);
				
			}
		}
		newDeploy.setResources(newResourceSet);

		return newDeploy;
	}

}
