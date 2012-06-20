bc平台工作流

浏览地址： https://github.com/rongjihuang/bc-workflow
源码检出： git@github.com:rongjihuang/bc-workflow.git

一) activiti整合记录
版本：5.9
1) 获取数据库脚本
1-1) 构建表
activiti-engine-5.9.jar/org.activiti.db.create/:
activiti.{db}.create.engine.sql		--> 引擎必须的表
activiti.{db}.create.history.sql	--> 当history level设置为none是，此为可选的表，contain the history and audit information
activiti.{db}.create.identity.sql	--> 可选的表，when using the default identity management as shipped with the engine.

1-2) 删除表
activiti-engine-5.9.jar/org.activiti.db.drop/:
activiti.{db}.drop.engine.sql
activiti.{db}.drop.history.sql
activiti.{db}.drop.identity.sql

1-3) 数据库升级
activiti-engine-5.9.jar/org.activiti.db.upgrade/:

1-4) demo的初始化数据
activiti-5.9.zip/setup/files/demo/:
h2.data.sql --> demo.postgres.data.sql


二) 数据库脚本运行顺序
建表脚本运行顺序：
activiti.postgres.create.engine.sql
activiti.postgres.create.history.sql
activiti.postgres.create.identity.sql

初始化数据的脚本：
demo.postgres.data.sql

删表脚本运行顺序：
activiti.postgres.drop.identity.sql
activiti.postgres.drop.history.sql
activiti.postgres.drop.engine.sql


三) 数据库表名规范
activiti所有表名均以"ACT_"开头，第二部分为两个字符的标识，与service API相对应：
ACT_RE_*: 'RE' repository的相关表.，包含静态的配置信息，如流程定义、流程的资源(images、rules等)。
ACT_RU_*: 'RU' runtime的相关表，包含运行时的数据，如流程实例、用户任务、variables、 jobs等，这些数据只在流程运行时存在，流程结束后就会被清除，以保证最小数据和提高性能。.
ACT_ID_*: 'ID' identity的相关表，包含身份标识信息，如users、 groups等。
ACT_HI_*: 'HI' history的相关表，包含历史数据信息，如以往的process instances、variables、tasks等。
ACT_GE_*: 通用数据及设置 general data, which is used in various use cases.

