package cn.bc.workflow.activiti.delegate;

import cn.bc.core.exception.CoreException;
import cn.bc.core.util.JsonUtils;
import cn.bc.core.util.SpringUtils;
import cn.bc.spider.Result;
import cn.bc.spider.callable.TextCallable;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
   * 响应成功后的回调接口。
   * <p>
   * 仅适用于 {@link SendHttpRequestListener} 请求响应成功后的回调处理。
   */
  public interface SuccessCallback {
    void call(VariableScope scope, String responseBody, int statusCode, List<Map.Entry<String, String>> responseHeaders);
  }

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
   * 请求发送成功后回调处理配置。
   * <p>
   * 这了配置的是 spring 的 bean 名称，该 bean 必须实现 {@link SuccessCallback} 接口。
   * 实现者可以利用该接口进行额外的处理，如设置额外的流程变量、读取响应头等。
   */
  private Expression successCallbackBean;

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
    String methodValue = method != null ? (String) method.getValue(execution) : "GET";
    int successStatusCodeValue = successStatusCode != null ? Integer.parseInt(successStatusCode.getValue(execution).toString()) : 200;
    final List<Map.Entry<String, String>> responseHeaders = new ArrayList<>();

    // 构建同步分期还款请求
    TextCallable callable = new TextCallable() {
      @Override
      protected int getSuccessStatusCode() {
        return successStatusCodeValue;
      }

      @Override
      protected Result<String> defaultBadResult(HttpResponse response) {
        try {
          // 请求失败，返回响应体包含的文本信息
          String responseBody = getResponseText();
          if (!StringUtils.isBlank(responseBody)) return new Result<>(false, responseBody);
          else return super.defaultBadResult(response); // 响应体无内容返回默认值
        } catch (Exception e) {
          logger.warn("解析响应失败，返回默认值代替！", e);
          return super.defaultBadResult(response); // 有异常返回默认值
        }
      }

      @Override
      public String parseResponse() throws Exception {
        // 缓存响应头
        responseHeaders.clear();
        responseHeaders.addAll(this.getResponseHeaders());

        // 返回 body
        return super.parseResponse();
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
    Result<String> result = TaskExecutor.get(callable);
    if (result.isSuccess()) {
      if (successCallbackBean != null) {
        String beanName = (String) successCallbackBean.getValue(execution);
        SuccessCallback callback = SpringUtils.getBean(beanName, SuccessCallback.class);
        callback.call(execution, result.getData(), successStatusCodeValue, responseHeaders);
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