# bc-workflow 变更日志

## 4.4.0 2021-09-18

- 增加 SendHttpRequestListener.SuccessCallback 处理机制
    > 响应成功后的回调处理机制

## 4.3.1 2021-04-02

- 任务的 form 渲染时增加流程的启动时间参数

## 4.3.0 2021-03-26

- 优化发起流程接口的设计与实现，添加可拷贝模块附件为流程附件的功能

## 4.2.0 2020-10-23

- 升级到 bc-parent-4.0.4
- 添加创建流程与模块关联关系的监听器 - CreateModuleRelationListener
- 扩展发起流程接口 - 允许携带业务数据与可自动完成首个待办任务的办理
- 添加可发送 HTTP 请求的监听器 - SendHttpRequestListener

## 4.1.1 2020-07-14

- 设置固定依赖 bc-parent-4.0.3

## 4.1.0 2019-11-18

- 流程公共信息区附件的 "编辑"、"删除" 按钮可见性改为可以通过属性 `wf.disable-common-region-edit` 全局配置，默认值为 true （禁用）

## 4.0.0 2018-06-23

- 延迟获取FormServiceImpl.deployService 避免循环依赖导致的 spring 加载失败
- 使用两空格缩进重新格式化所有源码
- 挂载为 [bc-framework] 的子模块并跟随其版本号

## 3.2.5 2017-10-23

- 委托任务时只显示正常状态的用户

## 3.2.4 2017-07-18

- 修正打开工作空间 WorkspaceServiceImpl.isTaskActor 的空指针异常

## 3.2.3 2017-05-26

- 委托任务时不可委托给禁用的用户
- 流程监控模糊搜索不分大小写

## 3.2.2 (2017-01-05)

- 修正查看经办流程视图默认状态为全部

## 3.2.1 (2016-11-28)

- 修正流程待办任务中添加附件的角色判断问题

## 3.2.0 (2016-08-30)

- 基于 bc-framework-bom 简化版本的依赖配置


[bc-framework]: https://github.com/bcsoft/bc-framework