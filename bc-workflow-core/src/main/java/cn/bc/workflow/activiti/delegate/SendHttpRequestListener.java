package cn.bc.workflow.activiti.delegate;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.JsonUtils;
import cn.bc.spider.Result;
import cn.bc.spider.callable.BaseCallable;
import cn.bc.spider.http.TaskExecutor;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.el.Expression;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 调用可发起 HTTP 请求的监听器。
 * <p>
 * 可用在环节的监听（实现了 TaskListener），也可用在流向和流程的监听（实现了 ExecutionListener）。
 *
 * @author zf
 * @author RJ
 */
@SuppressWarnings("unused")
public class SendHttpRequestListener extends ExcutionLogListener implements TaskListener {
  private static final Logger logger = LoggerFactory.getLogger(SendHttpRequestListener.class);

  /**
   * 是否执行请求的发送
   */
  private Expression ignore;
  /**
   * 请求方法，如 GET
   */
  private Expression method;
  /**
   * 请求的 url，如 http://192.168.0.222/financial/pay-plan/
   */
  private Expression url;
  /**
   * 要发送的请求头，jwt 头系统内部自动添加，这里配置自定义头
   */
  private Expression headers;
  /**
   * 要发送的请求体
   */
  private Expression body;
  /**
   * 请求发送后响应成功的响应码，默认 2xx
   */
  private Expression successStatusCode;
  /**
   * 调用此监听器的模块类型，作为模块与流程关联的标识，如：PayPlan
   */
  private Expression mtype;
  /**
   * 需要获取的响应数据名称，如：Connection，Location, body
   */
  private Expression response;

  @Override
  public void notify(DelegateExecution execution) {
    // 判断是否执行发送请求
    boolean ignoreValue = ignore == null || (ignore.getExpressionText().contains("$") ? (Boolean) ignore.getValue(execution) : Boolean.parseBoolean(ignore.getExpressionText()));
    // 判断是否执行请求的发送
    if (!ignoreValue) {
      logger.debug("ignore = false，无需执行请求的发送");
    } else {
      buildRequest(execution);
    }
  }

  @Override
  public void notify(DelegateTask execution) {
    if (logger.isDebugEnabled()) {
      logger.debug("execution=" + execution.getClass());
      logger.debug("this=" + this.getClass());
      logger.debug("id=" + execution.getId());
      logger.debug("eventName=" + execution.getEventName());
      logger.debug("processInstanceId" + execution.getProcessInstanceId());
      logger.debug("executionId=" + execution.getExecutionId());
      logger.debug("taskDefinitionKey=" + execution.getTaskDefinitionKey());
    }
    // 判断是否执行发送请求
    boolean ignoreValue = ignore == null || (ignore.getExpressionText().contains("$") ? (Boolean) ignore.getValue(execution) : Boolean.parseBoolean(ignore.getExpressionText()));
    // 判断是否执行请求的发送
    if (!ignoreValue) {
      logger.debug("ignore = false，无需执行请求的发送");
    } else {
      buildRequest(execution);
    }
  }

  private void buildRequest(VariableScope execution) {
    String methodValue = method != null ? (String) method.getValue(execution) : null;
    int Status = successStatusCode != null ? (successStatusCode.getExpressionText().contains("$") ? ((Long) successStatusCode.getValue(execution)).intValue() : Integer.parseInt(successStatusCode.getExpressionText())) : 200;
    String[] responseArray = response != null && !StringUtils.isBlank((String) response.getValue(execution)) ? ((String) response.getValue(execution)).replaceAll("\\s*", "").split(",") : null;
    String moduleTypeValue = mtype != null ? (String) mtype.getValue(execution) : null;

    // 构建同步分期还款请求
    BaseCallable<Object> callable = new BaseCallable<Object>() {
      @Override
      protected int getSuccessStatusCode() {
        return Status;
      }

      @Override
      protected Result<Object> defaultBadResult(HttpResponse response) {
        try {
          // 请求失败，返回响应体包含的文本信息
          String Response = getResponseText();
          if (!StringUtils.isBlank(Response)) return new Result<>(false, Response);
          else return super.defaultBadResult(response); // 响应体无内容返回默认值
        } catch (Exception e) {
          logger.warn("解析响应失败，返回默认值代替！", e);
          return super.defaultBadResult(response); // 有异常返回默认值
        }
      }

      @Override
      public Object parseResponse() throws Exception {
        if (responseArray != null) {
          Map<String, String> responseMap = new HashMap<>();
          for (String k : responseArray) {
            // 请求体信息， body 作为 key
            if (k.equalsIgnoreCase("body")) responseMap.put("body", getResponseText());
              // 请求头信息，'k' 作为 key，如：Location
            else responseMap.put(k, getResponseHeader(k));
          }
          // 以 Map 形式返回需要的 response
          return responseMap;
        } else {
          // 以 String 形式返回请求体信息
          return getResponseText();
        }
      }
    };

    // 设置请求参数
    callable.setMethod(methodValue);
    callable.setUrl((String) url.getValue(execution));
    Map<String, String> headersMap = JsonUtils.toMap((String) headers.getValue(execution))
      .entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    callable.addHeader(headersMap);
    callable.setPayload(body.getValue(execution));
    Result<Object> result = TaskExecutor.get(callable);
    if (result.isSuccess()) {
      if ("POST".equalsIgnoreCase(methodValue) && responseArray != null) {
        // 以 responseArray 的值作为 key，解析相应 response 的值作为 value
        @SuppressWarnings("unchecked")
        Map<String, Object> resultData = getResultData((Map<String, Object>) result.getData());
        // 如果是 POST 请求，获取实体类 ID，并设置为全局参数
        if (!resultData.isEmpty() && resultData.get("Location") != null) {
          execution.setVariable("mid", resultData.get("Location"));
          // 设置调用此监听器的模块标识
          if (moduleTypeValue != null) execution.setVariable("mtype", moduleTypeValue);
        }
      }
    } else {
      logger.debug(methodValue + "/" + url.getValue(execution) + "请求发送成功，响应失败：" + result.getError());
      throw new CoreException(methodValue + "/" + url.getValue(execution) + "请求发送成功，接口响应失败：" + result.getError());
    }
  }

  private Map<String, Object> getResultData(Map<String, Object> data) {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      if (null != entry.getValue() && !StringUtils.isBlank(entry.getValue().toString())) {
        String value = entry.getValue().toString();
        if (entry.getKey().equalsIgnoreCase("Location")) {
          String newValue = value.substring(value.lastIndexOf("/") + 1);
          result.put("Location", newValue);
        } else {
          result.put(entry.getKey(), value);
        }
      }
    }
    return result;
  }
}