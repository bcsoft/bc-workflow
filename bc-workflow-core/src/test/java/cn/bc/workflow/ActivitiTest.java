package cn.bc.workflow;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class ActivitiTest {
	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private FormService formService;

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	@Rule
	public ActivitiRule activitiSpringRule;

	@Test
	@Deployment(resources = { "cn/bc/workflow/testProcess.bpmn20.xml" })
	public void simpleProcessTest() {
		String key = "testProcess";
		List<FormProperty> ps;
		ProcessDefinitionQuery processDefinitionQuery;

		// 获取流程开始前的表单信息
		processDefinitionQuery = repositoryService
				.createProcessDefinitionQuery();
		List<ProcessDefinition> pds = processDefinitionQuery
				.processDefinitionKey(key).list();
		assertEquals(1, pds.size());
		assertEquals(key, pds.get(0).getKey());

		String procDefId = repositoryService.createProcessDefinitionQuery()
				.singleResult().getId();
		// System.out.println("procDefId=" + procDefId);
		ps = formService.getStartFormData(procDefId).getFormProperties();
		assertEquals(2, ps.size());

		// 开始流程
		runtimeService.startProcessInstanceByKey(key);
		assertEquals(1, runtimeService.createProcessInstanceQuery().count());

		// 获取任务
		Task task = taskService.createTaskQuery().singleResult();
		assertEquals("任务1", task.getName());

		// 获取任务的表单信息
		ps = formService.getTaskFormData(task.getId()).getFormProperties();
		assertEquals(2, ps.size());
		// assertEquals("dd-MMM-yyyy",
		// ps.get(1).getType().getInformation("datePattern"));
		// System.out.println(ps.get(2).getType().getInformation("values"));

		// 完成任务
		taskService.complete(task.getId());
		assertEquals(0, runtimeService.createProcessInstanceQuery().count());
	}
}
