bc平台工作流

浏览地址： https://github.com/rongjihuang/bc-workflow
源码检出： git@github.com:rongjihuang/bc-workflow.git

一) activiti整合记录
版本：5.6
1) 获取数据库脚本
1-1) 构建表
activiti-engine-5.6.jar/org.activiti.db.create/:
activiti.{db}.create.engine.sql
activiti.{db}.create.history.sql
activiti.{db}.create.identity.sql
activiti.{db}.create.cycle.sql

1-2) 删除表
activiti-engine-5.6.jar/org.activiti.db.drop/:
activiti.{db}.drop.engine.sql
activiti.{db}.drop.history.sql
activiti.{db}.drop.identity.sql
activiti.{db}.drop.cycle.sql

1-3) 数据库升级
activiti-engine-5.6.jar/org.activiti.db.upgrade/:
activiti.{db}.upgradestep.53.to.54.engine.sql
activiti.{db}.upgradestep.53.to.54.history.sql
activiti.{db}.upgradestep.53.to.54.identity.sql

1-4) demo的初始化数据
activiti-5.6.zip/setup/files/demo/:
mysql.data.sql
oracle.data.sql
替换文件中一些值：
@activiti.modeler.base.url@ --> http://localhost:8082/activiti-modeler/
@cycle.base.file.path@ --> D:\bcdata\workspace\activiti-cycle-examples
或者运行：>ant db.demo.data -Ddb=oracle 后到目录setup/build/demo复制

原demo的参考值：
@activiti.modeler.base.url@ --> http://localhost:8080/activiti-modeler/
@cycle.base.file.path@ --> D:\OpenSource\activiti\activiti-5.6\workspace\activiti-cycle-examples




