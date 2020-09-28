# 流程引擎 Rest API

Rest 的上下文路径 `{context-path}` 默认值为 `/bc-workflow`。
以下的 Rest URL 路径均为相对于此路径下的子路径。如 `/a?p=v` 的实际访问路径应为 `{context-path}/a?p=v`。

| SN | Method | Url                                | Description
|----|--------|------------------------------------|-------------
| 1  | POST   | /workflow/startFlow?key=x&id=x     | 启动流程

## 1. 启动流程

**请求：**
```
POST /workflow/startFlow?key=x&id=x
Content-Type : application/x-www-form-urlencoded

{mid=x&mtype=x&autoCompleteFirstTask=x&formData=x}
```

|  Name                 | Type    |  Description
|-----------------------|---------|---------------
| key                   | string  | 流程编码，如：PayPlanApproval
| id                    | string  | 流程定义的主键，如：PayPlanApproval:1:16591403
| mid                   | string  | 模块 ID，模块与流程关联标识之一，如：16
| mtype                 | string  | 模块类型，模块与流程关联标识之一,如：PayPlan
| autoCompleteFirstTask | boolean | 是否自动完成首个待办的办理，默认 false
| formData              | string  | 流程变量集合，标准 json 格式，如：`[{"name":"driverId","value":"106194","type":"int","scope":"global"}…]`

> 启动流程时选择 key 或 id 任意一个定义参数即可。
> 启动流程需要携带流程变量才定义 formData，否则不需定义 formData。

**响应：**

```
200 OK
Content-Type : application/json

{success：true，msg："启动成功!"，processInstance：$processInstanceId}
```

如果启动失败，响应返回：
```
200 OK
Content-Type : application/json

{success：false，msg：$e}
```
