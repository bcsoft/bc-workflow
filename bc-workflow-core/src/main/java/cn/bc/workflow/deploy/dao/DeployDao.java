package cn.bc.workflow.deploy.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.bc.core.dao.CrudDao;
import cn.bc.workflow.deploy.domain.Deploy;
import cn.bc.workflow.deploy.domain.DeployResource;

/**
 * 流程部署Dao接口
 * 
 * @author wis
 * 
 */
public interface DeployDao extends CrudDao<Deploy> {
	/**
	 * 根据编码(和版本号)获取模板对象
	 * 
	 * @param code
	 *            如果含字符":"，则进行分拆，前面部分为编码，后面部分为版本号，如果没有字符":"，将获取当前状态为正常的版本
	 * @return 指定编码(和版本号)的模板对象
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
	public boolean isUniqueCodeAndVersion(Long currentId, String code,
			String version);
	/**
	 * 根据编码和id获取编码相同但id不同，且状态为正常的模板对象
	 * 
	 * @param code
	 * @param id
	 * @return
	 */
	public Deploy loadByCodeAndId(String code,Long id);
	
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
	public ArrayList<Object> isUniqueResourceCodeAndExtCheck(Long id, String codes);

	/**
	 * 通过流程部署记录id和流程编码和部署资源编码查找对应部署资源
	 * @param dmId
	 * @param wfCode
	 * @param resCode
	 * @return
	 */	public DeployResource findDeployResourceByDmIdAndwfCodeAndresCode(String dmId,String wfCode, String resCode);

	/**
	 * 通过流程编码获取流程资源
	 * @param deployCode
	 * @return
	 */
	public DeployResource findDeployResourceCode(String deployCode);
}
