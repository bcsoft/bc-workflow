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
	 * 保存方法，自动将同编码的另外一条状态为正常的模板转为禁用
	 * 
	 * @param deploy
	 * @return
	 */
	public void saveTpl(Deploy deploy);
	
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
	 * 发布吧部署流程需要部署流程id,标题,物理文件路径,原文件名称 (XML)
	 * @param excludeId
	 * @param subject
	 * @param source
	 * @param path
	 */
	public void deployRelease4XML(Long excludeId, String subject, String source, String path);

	/**
	 * 发布吧部署流程需要部署流程id,标题,物理文件路径,原文件名称 (BAR)
	 * @param excludeId
	 * @param subject
	 * @param source
	 * @param path
	 */
	public void deployRelease4BAR(Long excludeId, String subject, String source, String path);

	
}
