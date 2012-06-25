bc平台工作流

浏览地址： https://github.com/rongjihuang/bc-workflow
源码检出： git@github.com:rongjihuang/bc-workflow.git
依赖：activiti 5.9

一) 数据库脚本运行顺序
建表脚本运行顺序：
activiti.postgres.create.engine.sql
activiti.postgres.create.identity.sql
activiti.postgres.create.history.sql

初始化数据的脚本：
demo.postgres.data.sql

删表脚本运行顺序：
activiti.postgres.drop.history.sql
activiti.postgres.drop.engine.sql
activiti.postgres.drop.identity.sql

二) 数据库表名规范说明
activiti所有表名均以"ACT_"开头，第二部分为两个字符的标识，与service API相对应：
ACT_RE_*: 'RE' repository的相关表.，包含静态的配置信息，如流程定义、流程的资源(images、rules等)。
ACT_RU_*: 'RU' runtime的相关表，包含运行时的数据，如流程实例、用户任务、variables、 jobs等，这些数据只在流程运行时存在，流程结束后就会被清除，以保证最小数据和提高性能。.
ACT_ID_*: 'ID' identity的相关表，包含身份标识信息，如users、 groups等。
ACT_HI_*: 'HI' history的相关表，包含历史数据信息，如以往的process instances、variables、tasks等。
ACT_GE_*: 通用数据及设置 general data, which is used in various use cases.

