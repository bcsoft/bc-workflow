package cn.bc.workflow.deploy.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.bc.core.service.CrudService;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.domain.DeployResource;

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
	 * @param deploymentId
	 * @param isCascade
	 */
	public void dodeployCancel(Long deploymentId,Boolean isCascade);

	/**
	 * 通过流程id判断此信息是否已发起
	 * @param excludeId
	 * @return
	 */
	public Long isStarted(String deploymentId);

	/**
	 * 判断指定的编码与版本号是否唯一
	 * 
	 * @param currentId
	 *            当前模板的id
	 * @param codes
	 *            当前模板要使用的编码列表
	 * @return
	 */
	public ArrayList<Object> isUniqueResourceCodeAndExtCheck(
			Long id, String codes);

	/**
	 * 通过流程部署记录id和流程编码和部署资源编码查找对应部署资源
	 * @param dmId
	 * @param wfCode
	 * @param resCode
	 * @return
	 */
	public DeployResource findDeployResourceByDmIdAndwfCodeAndresCode(String dmId,String wfCode,String resCode);
	
	/**
	 * 通过流程编码获取流程资源
	 * @param deployCode 资源编码
	 * @param deployId 部署ID
	 * @return
	 */
	public DeployResource findDeployResourceByCode(Long deployId, String deployCode);

	/**
	 * 升级流程部署操作
	 * @param id
	 * @return
	 */
	public Deploy doCopyDeploy(Long id);

	/**
	 * 禁用流程部署
	 * @param excludeId
	 */
	public void dodeployStop(Long excludeId);

	
	/**
	 * 将状态改为使用中
	 * @param excludeId
	 */
	public void dodeployChangeStatus(Long excludeId);
}
