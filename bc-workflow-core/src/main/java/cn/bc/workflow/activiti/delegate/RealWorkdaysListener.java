package cn.bc.workflow.activiti.delegate;

import cn.bc.core.util.SpringUtils;
import cn.bc.workday.service.WorkdayService;
import cn.bc.workflow.historictaskinstance.service.HistoricTaskInstanceService;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.TaskListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 记录指定两个任务之间实际工作日的监听器
 *
 * @author LeeDane
 */
public class RealWorkdaysListener implements TaskListener {
  private static final Log logger = LogFactory
    .getLog(RealWorkdaysListener.class);

  //默认保存到数据库的实际工作日的变量名称
  public static final String DEFAULT_WORKING_DAY_NAME = "realworkingdays";

  //默认每周工作日的天数，天
  public static final int DEFAULT_WORKING_DAY_EVERY_WEEK = 5;

  /**
   * 指定开始任务的编码,没有配置，默认是流程的发起时间
   * 格式如："t030PreliminaryInvestigation"
   */
  private Expression startTask4Code;

  /**
   * 开始时间对应的需要偏移(增加或减去)的时间,必须是整数
   */
  private Expression startOffsetDays;

  /**
   * 指定结束任务的编码，没配置，默认是系统的当前时间
   * 格式如："t030PreliminaryInvestigation"
   */
  private Expression endTask4Code;

  /**
   * 指定多个code(可以理解为将这几个code看作一组)，将根据多个code找数据库中符合条件的最后一个任务的结束时间,没有配置，
   * 默认是流程的发起时间,配置了“endTask4Code”,这个将无效
   * 格式如："t030PreliminaryInvestigation,t040PreliminaryInvestigation"
   * ,其中"+"表示增加一天，后面必须是整数
   */
  private Expression endTask4Codes;

  /**
   * 结束时间对应的需要偏移(增加或减去)的时间,必须是整数
   */
  private Expression endOffsetDays;

  /**
   * 每周的工作日天数，没有配置，默认是5
   */
  private Expression workdaysEveryWeek;

  /**
   * 实际工作日的变量名称，默认是“realworkingdays”
   */
  private Expression realworkingdayName;

  /**
   * 是否是本地的变量，默认是true
   */
  private Expression isLocal;

  private HistoricTaskInstanceService historicTaskInstanceService;

  private WorkdayService workdayService;

  public RealWorkdaysListener() {
    historicTaskInstanceService = SpringUtils.getBean(HistoricTaskInstanceService.class);
    workdayService = SpringUtils.getBean(WorkdayService.class);
  }

  @Override
  public void notify(DelegateTask delegateTask) {
    if (logger.isDebugEnabled()) {
      logger.debug("startTask4Code=" + startTask4Code);
      logger.debug("endTask4Code=" + endTask4Code);
      logger.debug("workdaysEveryWeek=" + workdaysEveryWeek);
      logger.debug("isLocal=" + isLocal);
    }
    String processInstanceId = delegateTask.getProcessInstanceId(); // 流程定义的id
    Date fromDate, toDate = null;

    String startTaskCode = startTask4Code != null ? startTask4Code.getExpressionText() : null;
    String endTaskCode = endTask4Code != null ? endTask4Code.getExpressionText() : null;
    String endTaskCodes = endTask4Codes != null ? endTask4Codes.getExpressionText() : null;


    //处理是否需要增加后者减少天数
    //保存开始的加或者减对应的天数，必须是整数
    int startOffsetDay = startOffsetDays != null ? Integer
      .parseInt(startOffsetDays.getExpressionText()) : 0;
    //保存开始的加或者减对应的天数，必须是整数
    int endOffsetDay = endOffsetDays != null ? Integer
      .parseInt(endOffsetDays.getExpressionText()) : 0;

    if (startTaskCode != null) {
      fromDate = this.historicTaskInstanceService
        .findProcessInstanceTaskStartTime(processInstanceId,
          startTaskCode);
    } else { // 采取默认的方式，找流程发起时间
      fromDate = this.historicTaskInstanceService
        .findProcessInstanceStartTime(processInstanceId);
    }


    if (endTaskCode != null || endTaskCodes != null) {//根据用户自定义的任务编码
      List<String> lsCodes = new ArrayList<String>();
      if (endTaskCode != null) {
        lsCodes.add(endTaskCode);
      } else {
        String[] codes = endTaskCodes.split(",");
        for (String code : codes) {
          lsCodes.add(code);
        }
      }
      toDate = this.historicTaskInstanceService
        .findProcessInstanceTaskEndTime(processInstanceId, lsCodes);
    } else { // 采取默认的方式，赋值当前时间
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      try {
        toDate = format.parse(format.format(new Date()));
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }

    //用户有自定义每周的工作日天数，就取用户自定义的
    int workdaysEveryWeeks = DEFAULT_WORKING_DAY_EVERY_WEEK;
    if (workdaysEveryWeek != null)
      workdaysEveryWeeks = Integer.parseInt(workdaysEveryWeek
        .getExpressionText());

    //将开始和结束的日期做相应的加减处理
    fromDate = dealDateAddOrSub(fromDate, startOffsetDay);
    toDate = dealDateAddOrSub(toDate, endOffsetDay);

    //在数据库中找到该时间段内的真正的工作日天数
    int realworkingdays = workdayService.getRealWorkingdays(fromDate, toDate, workdaysEveryWeeks);

    //用户有自定义实际工作日名称，就取用户自定义的
    String workdayName = DEFAULT_WORKING_DAY_NAME;
    if (realworkingdayName != null)
      workdayName = realworkingdayName.getExpressionText();

    if (isLocal != null && String.valueOf(isLocal.getExpressionText()) == "false") {// 全局变量
      delegateTask.setVariable(workdayName == null ? "realworkingdays" : workdayName, realworkingdays);
    } else {// 设为本地变量
      Map<String, Integer> map = new HashMap<String, Integer>();
      map.put(workdayName == null ? "realworkingdays" : workdayName, realworkingdays);
      delegateTask.setVariablesLocal(map);
    }

  }

  /**
   * 返回经过计算相加减后的日期
   *
   * @param dateOrigin 原始的日期
   * @param days       天数,可以是负数
   * @return
   */
  private Date dealDateAddOrSub(Date dateOrigin, int days) {
    Date dateNew = null;
    if (days != 0) {
      Calendar rightNow = Calendar.getInstance();
      rightNow.setTime(dateOrigin);
      rightNow.add(Calendar.DAY_OF_YEAR, days);//日期加减相应天数
      dateNew = rightNow.getTime();
    } else {
      dateNew = dateOrigin;
    }
    return dateNew;
  }
}
