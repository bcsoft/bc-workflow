/**
 * 
 */
package cn.bc.workflow.service;

import java.io.InputStream;
import java.util.Map;

import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;

/**
 * 工作流常用方法封装的Service
 * 
 * @author dragon
 */
public interface WorkflowService {
	/**
	 * 发布模板库中指定编码的流程（仅支持zip或bar包）
	 * 
	 * @param templateCode
	 *            模板编码，如果含字符":"，则进行分拆，前面部分为编码，后面部分为版本号，如果没有字符":"，将获取当前状态为正常的版本
	 * @return
	 */
	Deployment deployZipFromTemplate(String templateCode);

	/**
	 * 发布模板库中指定编码的流程（仅支持xml文件）
	 * 
	 * @param templateCode
	 *            模板编码，如果含字符":"，则进行分拆，前面部分为编码，后面部分为版本号，如果没有字符":"，将获取当前状态为正常的版本
	 * @return
	 */
	Deployment deployXmlFromTemplate(String templateCode);

	/**
	 * 删除指定的发布历史
	 * 
	 * @param deploymentId
	 * @throwns RuntimeException 如果有相应的流程在运行、有运行历史、有定时任务，就会抛出异常
	 */
	void deleteDeployment(String deploymentId);

	/**
	 * 删除指定的发布历史
	 * 
	 * @param deploymentId
	 * @param cascade
	 *            是否级联删除所有历史信息
	 */
	void deleteDeployment(String deploymentId, boolean cascade);

	/**
	 * 启动指定编码流程的最新版本
	 * 
	 * @param key
	 *            流程编码
	 * @return 流程实例的id
	 */
	String startFlowByKey(String key);

	/**
	 * 领取任务
	 * 
	 * @param taskId
	 *            任务ID
	 */
	void claimTask(String taskId);

	/**
	 * 完成任务
	 * 
	 * @param taskId
	 *            任务ID
	 */
	void completeTask(String taskId);

	/**
	 * 委派任务
	 * 
	 * @param taskId
	 *            任务ID
	 * @param toUser
	 *            所委派给用户的帐号
	 */
	void delegateTask(String taskId, String toUser);

	/**
	 * 加载指定的流程实例
	 * 
	 * @param id
	 * @return
	 */
	ProcessInstance loadInstance(String id);

	/**
	 * 加载指定的流程定义
	 * 
	 * @param id
	 * @return
	 */
	ProcessDefinition loadDefinition(String id);

	/**
	 * 获取指定流程实例的流程图资源文件流
	 * 
	 * @param processInstanceId
	 *            流程实例ID
	 * @return
	 */
	InputStream getInstanceDiagram(String processInstanceId);

	/**
	 * 获取指定流程部署的相关资源文件流
	 * 
	 * @param deploymentId
	 *            流程部署ID
	 * @param resourceName
	 *            资源名称
	 * @return
	 */
	InputStream getDeploymentResource(String deploymentId, String resourceName);
	
	/**
	 * 获取指定流程实例公共信息的键值替换参数
	 * 
	 * @param processInstanceId
	 * 
	 * @return
	 */
	Map<String, Object> getInstanceParams(String processInstanceId);
	
	/**
	 * 获取指定任务的键值替换参数
	 * 
	 * @param taskId
	 * 
	 * @return
	 */
	Map<String, Object> getTaskParams(String taskId);
}