package cn.bc.workflow.deploy.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.bc.core.exception.CoreException;
import cn.bc.core.service.DefaultCrudService;
import cn.bc.docs.domain.Attach;
import cn.bc.identity.web.SystemContextHolder;
import cn.bc.log.domain.OperateLog;
import cn.bc.log.service.OperateLogService;
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
	private static Log logger = LogFactory.getLog(DeployServiceImpl.class);
	private OperateLogService operateLogService;
	private RuntimeService runtimeService;

	@Autowired
	public void setRuntimeService(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

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
				deploy.setStatus(Deploy.STATUS_RELEASED);
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
		if (null != deploy) {
			if (isCascade) {
				repositoryService.deleteDeployment(deploy.getDeploymentId(),
						true);// 级联删除流程
			} else {
				repositoryService.deleteDeployment(deploy.getDeploymentId());// 删除流程
			}

			deploy.setStatus(Deploy.STATUS_NOT_RELEASE);
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
		// 先级联取消发布
		this.dodeployCancel((Long) id, true);
		
		// 删除流转日志、意见、附件 TODO

		// 后彻底删除
		super.delete(id);
	}

	@Override
	public void delete(Serializable[] ids) {
		for (Serializable id : ids) {
			super.delete(id);
		}
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
			// 记录更新日志
			this.operateLogService.saveWorkLog(Deploy.class.getSimpleName(),
					entity.getId().toString(), "更新" + entity.getSubject()
							+ "的流程信息", null, OperateLog.OPERATE_UPDATE);
		}
		return entity;
	}

}
