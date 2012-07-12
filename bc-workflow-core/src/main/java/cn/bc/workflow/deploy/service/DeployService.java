package cn.bc.workflow.deploy.service;

import java.util.List;
import java.util.Map;

import cn.bc.core.service.CrudService;
import cn.bc.workflow.deploy.domain.Deploy;

/**
 * Service接口
 * 
 * @author wis
 * 
 */
public interface DeployService extends CrudService<Deploy> {
	/**
	 * 根据编码获取模板对象
	 * 
	 * @param code
	 *            如果含字符":"，则进行分拆，前面部分为编码，后面部分为版本号，如果没有字符":"，将获取当前状态为正常的版本
	 * @return 指定编码的模板对象
	 */
	public Deploy loadByCode(String code);

	/**
	 * 判断指定的编码与版本号是否唯一
	 * 
	 * @param currentId
	 *            当前模板的id
	 * @param code
	 *            当前模板要使用的编码
	 * @param version
	 *            当前模板要使用的版本号
	 * @return
	 */
	public boolean isUniqueCodeAndVersion(Long currentId, String code,String version);

	/**
	 * 查找模板分类
	 * @return
	 */
	public List<Map<String, String>> findCategoryOption();

	/**
	 * 通过流程部署id判断此信息是否发布
	 * @param excludeId
	 * @return
	 */
	public Long isReleased(Long excludeId);

	/**
	 * 发布部署流程需要部署流程id
	 * @param excludeId
	 */
	public void dodeployRelease(Long excludeId);

	/**
	 * 取消部署需要部署流程id,isCascade(是否级联取消)
	 * @param excludeId
	 * @param isCascade
	 */
	public void dodeployCancel(Long excludeId,Boolean isCascade);

	/**
	 * 通过流程id判断此信息是否已发起
	 * @param excludeId
	 * @return
	 */
	public Long isStarted(String deploymentId);

	
}
