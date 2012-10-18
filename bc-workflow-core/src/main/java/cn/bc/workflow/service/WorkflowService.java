/**
 * 
 */
package cn.bc.workflow.service;

import java.io.InputStream;
import java.util.Map;

import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;

import cn.bc.identity.domain.ActorHistory;
import cn.bc.workflow.deploy.domain.DeployResource;

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
	 * 删除指定的流程实例信息
	 * 
	 * @param instanceId
	 */
	void deleteInstance(String instanceId);

	/**
	 * 启动指定编码流程的最新版本
	 * 
	 * @param key
	 *            流程编码
	 * @return 流程实例的id
	 */
	String startFlowByKey(String key);
	
	/**
	 * 启动指定流程定义id的流程
	 * 
	 * @param id
	 * 
	 * @return
	 */
	String startFlowByDefinitionId(String id);

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
	 * 完成任务
	 * 
	 * @param taskId
	 *            任务ID
	 * @param globalVariables
	 *            全局流程变量
	 * @param localVariables
	 *            本地流程变量
	 */
	void completeTask(String taskId, Map<String, Object> globalVariables,
			Map<String, Object> localVariables);

	/**
	 * 委派任务
	 * 
	 * @param taskId
	 *            任务ID
	 * @param toUser
	 *            所委派给用户的帐号
	 * @return
	 */
	ActorHistory delegateTask(String taskId, String toUser);

	/**
	 * 分派任务
	 * 
	 * @param taskId
	 *            任务ID
	 * @param toUser
	 *            分派给当前岗位任务的用户
	 */
	ActorHistory assignTask(String taskId, String toUser);

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
	 * 获取流程图资源文件流
	 * 
	 * @param deployId
	 *            平台流程部署ID
	 * @return
	 */
	InputStream getDiagram(Long deployId);

	/**
	 * 获取指定流程资源的流程图资源文件流
	 * 
	 * @param deployId
	 *            平台流程部署ID
	 * @return
	 */
	InputStream getDeployDiagram(DeployResource dr);
	
	/**
	 * 获取指定流程部署的流程图资源文件流
	 * 
	 * @param deploymentId
	 *            Activiti流程部署ID
	 * @return
	 */
	InputStream getDeploymentDiagram(String deploymentId);

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
	 * 获取指定流程实例用于格式化Word模板的键值替换参数
	 * <ui>
	 * <li>startUser: {String} 流程发起人姓名</li>
	 * <li>startTime: {Date} 流程发起时间</li>
	 * <li>endTime: {Date} 流程结束时间</li>
	 * <li>duration: {Long} 流转耗时(毫秒)</li>
	 * <li>category: {String} 流程所属分类 </li>
	 * <li>key: {String} 流程编码</li>
	 * <li>name: {String} 流程名称</li>
	 * <li>subject: {String} 流程主题</li>
	 * <li>vs: {Map<String, Object>} 流程变量集(key为流程变量的名称)</li>
	 * <li>comments: {List<FlowAttach> comments} 流程意见集</li>
	 * <li>comments_str: {String} 流程意见字符串连接</li>
	 * <li>[taskCode]: {Map<String, Object>} 流程经办任务数据，每个任务的数据以其任务的编码作为key，其值为Map格式：
	 * 	<ui>
	 * 		<li>owner: {String} </li>
	 * 		<li>assignee: {String} 办理人</li>
	 * 		<li>desc: {String} 附加说明</li>
	 * 		<li>dueDate: {Date} 办理期限</li>
	 * 		<li>priority: {int} 优先级</li>
	 * 		<li>startTime: {Date} 任务发起时间</li>
	 * 		<li>endTime: {Date} 任务结束时间</li>
	 * 		<li>duration: {Long} 任务耗时(毫秒)</li>
	 * 		<li>key: {String} 任务编码</li>
	 * 		<li>name: {String} 任务名称</li>
	 * 		<li>subject: {String} 任务主题</li>
	 * 		<li>vs: {Map<String, Object>} 任务的本地流程变量集(key为流程变量的名称)</li>
	 * 		<li>comments: {List<FlowAttach> comments} 任务的意见集</li>
	 * 		<li>comments_str: {String} 任务的意见字符串连接</li>
	 * 	</ui>
	 * </li>
	 * </ui>
	 * 
	 * @param processInstanceId 流程实例的ID
	 * 
	 * @return
	 */
	Map<String, Object> getProcessHistoryParams(String processInstanceId);

	/**
	 * 获取指定流程实例用于格式化Word模板的键值替换参数
	 * <ui>
	 * <li>owner: {String} </li>
	 * <li>assignee: {String} 办理人</li>
	 * <li>desc: {String} 附加说明</li>
	 * <li>dueDate: {Date} 办理期限</li>
	 * <li>priority: {int} 优先级</li>
	 * <li>startTime: {Date} 任务发起时间</li>
	 * <li>endTime: {Date} 任务结束时间</li>
	 * <li>duration: {Long} 任务耗时(毫秒)</li>
	 * <li>key: {String} 任务编码</li>
	 * <li>name: {String} 任务名称</li>
	 * <li>subject: {String} 任务主题</li>
	 * <li>vs: {Map<String, Object>} 任务的本地流程变量集(key为流程变量的名称)</li>
	 * <li>comments: {List<FlowAttach> comments} 任务的意见集</li>
	 * <li>comments_str: {String} 任务的意见字符串连接</li>
	 * <li>pi: {Map<String, Object>} 流程实例数据，其值为Map格式：
	 * 	<ui>
	 * 		<li>startUser: {String} 流程发起人姓名</li>
	 * 		<li>startTime: {Date} 流程发起时间</li>
	 * 		<li>endTime: {Date} 流程结束时间</li>
	 * 		<li>duration: {Long} 流转耗时(毫秒)</li>
	 * 		<li>category: {String} 流程所属分类 </li>
	 * 		<li>key: {String} 流程编码</li>
	 * 		<li>name: {String} 流程名称</li>
	 * 		<li>subject: {String} 流程主题</li>
	 * 		<li>vs: {Map<String, Object>} 流程变量集(key为流程变量的名称)</li>
	 * 		<li>comments: {List<FlowAttach> comments} 流程意见集</li>
	 * 		<li>comments_str: {String} 流程意见字符串连接</li>
	 * 	</ui>
	 * </li>
	 * </ui>
	 * 
	 * @param taskId 任务实例的ID
	 * @param withProcessInfo 是否返回流程实例的全局数据
	 * 
	 * @return
	 */
	Map<String, Object> getTaskHistoryParams(String taskId, boolean withProcessInfo);
	Map<String, Object> getTaskHistoryParams(String taskId);

	/**
	 * 激活流程
	 * @param id
	 */
	void doActive(String id);

	/**
	 * 暂停流程
	 * @param id
	 */
	void doSuspended(String id);

}