bc平台工作流

浏览地址： https://github.com/rongjihuang/bc-workflow
源码检出： git@github.com:rongjihuang/bc-workflow.git
依赖：activiti 5.9

一) 数据库脚本运行顺序
建表脚本运行顺序：
activiti.postgres.create.identity.sql
activiti.postgres.create.engine.sql
activiti.postgres.create.history.sql

初始化数据的脚本：
activiti.postgres.data.bc-workflow.sql
注：activiti.postgres.data.demo.sql为官方网站上demo的初始化数据，平台中不要执行

删表脚本：
activiti.postgres.drop..sql

二) 数据库表名规范说明
activiti所有表名均以"ACT_"开头，第二部分为两个字符的标识，与service API相对应：
ACT_RE_*: 'RE' repository的相关表.，包含静态的配置信息，如流程定义、流程的资源(images、rules等)。
ACT_RU_*: 'RU' runtime的相关表，包含运行时的数据，如流程实例、用户任务、variables、 jobs等，这些数据只在流程运行时存在，流程结束后就会被清除，以保证最小数据和提高性能。.
ACT_ID_*: 'ID' identity的相关表，包含身份标识信息，如users、 groups等。
ACT_HI_*: 'HI' history的相关表，包含历史数据信息，如以往的process instances、variables、tasks等。
ACT_GE_*: 通用数据及设置 general data, which is used in various use cases.

三）业务处理方法整理
1）发起流程时自动添加表单、附件的处理
启动流程时，监听启动事件，监听器做自己的特殊处理，如交车确认流程启动时，自动获取本月的交车信息列表

获取任务的formkey
获取流程变量
解析formkey得到表单，并用流程变量数据对表单进行格式化，将格式化后的表单展现给用户


1）任务监听器：监听任务的创建(create)、分配(assignment)、完成(complete)
org.activiti.engine.delegate.TaskListener
只能配置在usertask上，不能配置在流程上

2）执行监听器：流程的启动(start)、结束(end)，活动的实例化，分支的转换(talk)
org.activiti.engine.delegate.ExecutionListener
配置在Servicetask上、分支上、流程上

3）JavaDelegate监听器：
org.activiti.engine.delegate.JavaDelegate
配置在ServiceTask上、分支上

2）静态附件和动态附件的处理