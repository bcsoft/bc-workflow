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

formData 变量集合中 json 对象的各个属性字段及说明如下：

| 属性名  | 属性说明
|--------|------------------------
| name   | 变量名
| value  | 该变量的值
| type   | 变量的类型，可以的值为 long、string、boolean、long[]、string[]、boolean[]等
| scope  | 变量应用范围，可以的值为 local-本地变量、global-全局变量、both-本地+全局

> 启动流程时选择 key 或 id 任意一个定义参数即可。
> 启动流程需要携带流程变量才定义 formData，否则不需定义 formData。

在指定模块发起携参流程时复制模块中的指定附件到流程附件的设计说明：
  在流程变量集合 formData 中添加一个流程变量 _attachCopyInfo，用于记录模块附件信息，以便发起流程时找到模块附件并复制到流程任务中，其格式如下：
  `{"name": "_attachCopyInfo", "value": "[\"ptype\", \"puid\", \"search\"], "type":"string[]", "scope": "global"}`
  value 值说明：由模块类型(ptype)、模块uid(puid)、附件名(search)(可以是模糊值)组成，用于查找指定附件。
  scope 值说明：local-复制模块附件到首待办任务中，global-复制模块附件到全局，both-复制附件到全局与首待办中。

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
