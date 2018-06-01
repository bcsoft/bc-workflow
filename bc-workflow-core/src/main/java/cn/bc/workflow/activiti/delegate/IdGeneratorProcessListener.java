/**
 *
 */
package cn.bc.workflow.activiti.delegate;

import cn.bc.core.util.DateUtils;
import cn.bc.core.util.SpringUtils;
import cn.bc.identity.service.IdGeneratorService;
import cn.bc.web.formater.NumberFormater;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;

/**
 * 初始化流程实例添加流水号变量值的监听器
 * <p>
 * 只能配置在流程的启动事件中
 * 流水号生成类型配置说明: "BC-WORKFLOW"+"。type4Code（自定义编码）"+".type4Date（按年份或月份或日份生成）"
 * <p>
 * 生成的流水号返回格式说明：
 * formatNumber：格式化原始值
 * ${yyyy}：4位年
 * ${MM}：月
 * ${dd}：日
 * 例子：（用户配置：GW${yyyy}${MM}${dd}${formatNumber}
 * 返回 ：GW201211120001）
 * </p>
 *
 * @author lbj
 */
public class IdGeneratorProcessListener implements ExecutionListener {
  private static final Log logger = LogFactory
    .getLog(IdGeneratorProcessListener.class);

  // 流程专用KEY
  private static final String WORKFLOW_KEY = "BC-WORKFLOW";

  /**
   * 定义流水号产生方式的日期类型 yyyy 年 MM 月 dd 日
   */
  private Expression type4Date;

  /**
   * 定义流水号产生方式
   */
  private Expression type4Code;

  /**
   * 格式化原始值 默认 "0000"
   */
  private Expression formatNumber;

  /**
   * 定义流水号的返回格式
   * <p>
   * 格式 自定义值${yyyy}${MM}${dd}${formatNumber}
   * <p>
   * 默认：${formatNumber}
   */
  private Expression pattern;

  /**
   * 流水号变量的key值，默认wf_code
   */
  private Expression pcode;

  public void notify(DelegateExecution execution) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("type4Date=" + type4Date);
      logger.debug("type4Code=" + type4Code);
      logger.debug("formatNumber=" + formatNumber);
      logger.debug("pattern=" + pattern);
      logger.debug("pcode=" + pcode);
    }
    String type = WORKFLOW_KEY;

    if (type4Code != null)
      type += "." + type4Code.getExpressionText();

    String formatDate = "";
    if (type4Date != null) {
      formatDate = DateUtils.formatCalendar(Calendar.getInstance(), type4Date.getExpressionText());
      type += "." + formatDate;
    }

    IdGeneratorService idGeneratorService = SpringUtils.getBean(
      "idGeneratorService", IdGeneratorService.class);
    NumberFormater nf = new NumberFormater(formatNumber.getExpressionText());
    String formatter = nf.format(idGeneratorService.nextValue(type));

    String codeValue = "";
    if (pattern == null || pattern.getExpressionText().equals("")) {
      codeValue = formatter;
    } else {
      String patternStr = pattern.getExpressionText();
      if (patternStr.indexOf("${formatNumber}") != -1) {
        patternStr = patternStr.replaceAll("\\$\\{formatNumber\\}", formatter);
      } else {
        patternStr += formatter;
      }

      if (patternStr.indexOf("${yyyy}") != -1) {
        patternStr = patternStr.replaceAll("\\$\\{yyyy\\}", DateUtils.formatCalendar(
          Calendar.getInstance(), "yyyy"));
      }
      if (patternStr.indexOf("${MM}") != -1) {
        patternStr = patternStr.replaceAll("\\$\\{MM\\}",
          DateUtils.formatCalendar(Calendar.getInstance(), "MM"));
      }
      if (patternStr.indexOf("${dd}") != -1) {
        patternStr = patternStr.replaceAll("\\$\\{dd\\}",
          DateUtils.formatCalendar(Calendar.getInstance(), "dd"));
      }

      codeValue = patternStr;
    }
    // 流程添加流水变量
    execution.setVariable(
      pcode != null ? pcode.getExpressionText()
        : "wf_code", codeValue);
  }

}
