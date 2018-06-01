package cn.bc.workflow.examples;

import cn.bc.core.util.DateUtils;
import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.EnumFormType;
import org.activiti.engine.impl.form.StringFormType;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration("classpath:spring-test.xml")
public class InnerForm2ProcessTest {
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
  @Deployment(resources = {"cn/bc/workflow/examples/InnerFormProcess.bpmn"})
  public void testStartFormData() {
    // 流程开始事件的表单信息
    String procDefId = repositoryService.createProcessDefinitionQuery()
      .singleResult().getId();
    List<FormProperty> ps = formService.getStartFormData(procDefId)
      .getFormProperties();
    assertEquals(3, ps.size());
    assertEquals("speaker", ps.get(0).getId());
    assertEquals("Speaker", ps.get(0).getName());
    assertEquals(StringFormType.class, ps.get(0).getType().getClass());
    assertEquals(null, ps.get(0).getValue());

    assertEquals("start", ps.get(1).getId());
    assertEquals(null, ps.get(1).getName());
    assertEquals(DateFormType.class, ps.get(1).getType().getClass());
    assertEquals(null, ps.get(1).getValue());
    assertEquals("yyyy-MM-dd",
      ps.get(1).getType().getInformation("datePattern"));

    assertEquals("direction", ps.get(2).getId());
    assertEquals(null, ps.get(2).getName());
    assertEquals(EnumFormType.class, ps.get(2).getType().getClass());
    assertEquals(null, ps.get(2).getValue());
    assertEquals(HashMap.class, ps.get(2).getType()
      .getInformation("values").getClass());
    @SuppressWarnings("unchecked")
    HashMap<String, String> values = (HashMap<String, String>) ps.get(2)
      .getType().getInformation("values");
    assertEquals(4, values.size());
    assertEquals("Go Left", values.get("left"));
    assertEquals("Go Right", values.get("right"));
    assertEquals("Go Up", values.get("up"));
    assertEquals("Go Down", values.get("down"));
  }

  @Test
  @Deployment(resources = {"cn/bc/workflow/examples/InnerFormProcess.bpmn"})
  public void testTaskFormData() {
    String key = "innerFormProcess";
    List<FormProperty> fp;

    // 定义一些流程变量
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("mySpeaker", "mySpeaker");
    variables.put("startDate", DateUtils.getCalendar("2012-01-01"));

    // 开始流程
    runtimeService.startProcessInstanceByKey(key, variables);
    assertEquals(1, runtimeService.createProcessInstanceQuery().count());

    // 获取任务
    Task task = taskService.createTaskQuery().singleResult();
    assertEquals("任务1", task.getName());

    // 获取任务的表单信息
    fp = formService.getTaskFormData(task.getId()).getFormProperties();
    assertEquals(3, fp.size());

    // 验证表单属性
    assertEquals("speaker", fp.get(0).getId());
    assertEquals("Speaker", fp.get(0).getName());
    assertEquals(StringFormType.class, fp.get(0).getType().getClass());
    assertEquals(null, fp.get(0).getValue());

    assertEquals("start", fp.get(1).getId());
    assertEquals(null, fp.get(1).getName());
    assertEquals(DateFormType.class, fp.get(1).getType().getClass());
    assertEquals(null, fp.get(1).getValue());
    assertEquals("yyyy-MM-dd",
      fp.get(1).getType().getInformation("datePattern"));

    assertEquals("direction", fp.get(2).getId());
    assertEquals(null, fp.get(2).getName());
    assertEquals(EnumFormType.class, fp.get(2).getType().getClass());
    assertEquals(null, fp.get(2).getValue());
    assertEquals(HashMap.class, fp.get(2).getType()
      .getInformation("values").getClass());
    @SuppressWarnings("unchecked")
    HashMap<String, String> values = (HashMap<String, String>) fp.get(2)
      .getType().getInformation("values");
    assertEquals(4, values.size());
    assertEquals("Go Left", values.get("left"));
    assertEquals("Go Right", values.get("right"));
    assertEquals("Go Up", values.get("up"));
    assertEquals("Go Down", values.get("down"));

    // 完成任务
    taskService.complete(task.getId());
    assertEquals(0, runtimeService.createProcessInstanceQuery().count());
  }
}
