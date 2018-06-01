package cn.bc.workflow.examples;

import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class CustomFormProcessTest {
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
  @Deployment(resources = {
    "cn/bc/workflow/examples/CustomFormProcess.bpmn20.xml",
    "cn/bc/workflow/examples/CustomForm.form"})
  public void testFormDataFromFormKey() {
    // 流程开始事件的表单信息
    String procDefId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();

    // 验证表单属性配置为空
    List<FormProperty> ps = formService.getStartFormData(procDefId)
      .getFormProperties();
    Assert.assertNotNull(ps);
    Assert.assertEquals(0, ps.size());

    // 验证自定义的表单信息
    String formKey = formService.getStartFormData(procDefId).getFormKey();
    Assert.assertEquals("cn/bc/workflow/examples/CustomForm.form", formKey);// 表单资源路径
    Object startForm = formService.getRenderedStartForm(procDefId);
    Assert.assertNotNull(startForm);
    System.out.println("--startForm 0--");
    System.out.println(startForm);
    System.out.println("--startForm 1--");

    // 定义流程变量并启动流程
    Map<String, String> formProperties = new HashMap<String, String>();
    formProperties.put("info", "测试信息");
    formService.submitStartFormData(procDefId, formProperties);

    // 验证管理岗有一条任务
    Task task = taskService.createTaskQuery()
      .taskCandidateGroup("chaojiguanligang").singleResult();
    Assert.assertEquals("任务1", task.getName());
    Object taskForm = formService.getRenderedTaskForm(task.getId());
    Assert.assertNotNull(taskForm);
    System.out.println("--taskForm 0--");
    System.out.println(taskForm);
    System.out.println("--taskForm 1--");

    // 管理员领取并完成任务
    taskService.claim(task.getId(), "admin");
    Map<String, Object> formProperties2 = new HashMap<String, Object>();
    formProperties.put("userCode", "admin");
    taskService.complete(task.getId(), formProperties2);

    // 验证管理员有任务2
    task = taskService.createTaskQuery().taskAssignee("admin")
      .singleResult();
    Assert.assertEquals("任务2", task.getName());
    taskForm = formService.getRenderedTaskForm(task.getId());
    Assert.assertNotNull(taskForm);

    // 完成任务(自动结束流程了)
    taskService.complete(task.getId());
    assertEquals(0, runtimeService.createProcessInstanceQuery().count());
  }
}
