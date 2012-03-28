bc平台工作流

浏览地址： https://github.com/rongjihuang/bc-workflow
源码检出： git@github.com:rongjihuang/bc-workflow.git

一) activiti整合记录
版本：5.9
1) 获取数据库脚本
1-1) 构建表
activiti-engine-5.9.jar/org.activiti.db.create/:
activiti.{db}.create.engine.sql
activiti.{db}.create.history.sql
activiti.{db}.create.identity.sql

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

