# Activiti 整合记录
版本：5.9

## 数据库脚本
### 构建表
```
activiti-engine-5.9.jar/org.activiti.db.create/:
activiti.{db}.create.engine.sql		--> 引擎必须的表
activiti.{db}.create.history.sql	--> 当history level设置为none是，此为可选的表，contain the history and audit information
activiti.{db}.create.identity.sql	--> 可选的表，when using the default identity management as shipped with the engine.
```

### 删除表
```
activiti-engine-5.9.jar/org.activiti.db.drop/:
activiti.{db}.drop.engine.sql
activiti.{db}.drop.history.sql
activiti.{db}.drop.identity.sql
```

### 数据库升级
```
activiti-engine-5.9.jar/org.activiti.db.upgrade/:
```

### demo 初始化数据
```
activiti-5.9.zip/setup/files/demo/:
h2.data.sql --> demo.postgres.data.sql
```

## Activiti 相关学习资料：
### eclipse配置
- XML文件自动完成和验证的配置：
```
Window --> Preferences --> XML --> XML Catalog --> Add...
加入 ${activiti.home}/docs/xsd/BPMN20.xsd 和 ${activiti.home}/docs/xsd/activiti-bpmn-extensions-5.4.xsd两个文件即可
```

- 流程设计器插件：
```
Help -> Install New Software, 点击“Add”按钮，填写如下信息
Name: Activiti BPMN 2.0 designer
Location: http://activiti.org/designer/update/
确认"Contact all updates sites.." 被勾选中
```

### Activiti Designer 编辑器的特性：
```
	保存时将自动生成 BPMN 2.0 XML 文件和流程图的图片(自动生成图片功能可以在Eclipse preferences的Activiti节中切换)；
	可以导入 BPMN 2.0 XML 文件：
		方法1) 将XML配置文件放到 src/main/resources/diagrams 目录(扩展名必须为.bpmn20.xml),打开时就会自动生成
		方法2) 项目右键菜单中选择“Import BPMN 2.0 file”
	可以打包为bar或jar文件发布：项目右键菜单中选择“Create deployment artifacts”；
		使用Activiti Explorer上传此文件将流程发布到引擎，如果包含jar则需将其放到/WEB-INF/lib目录以让Activiti Engine找到
	自动生成单元测试：.bpmn20.xml文件的右键菜单选择；
	Activiti project是以Maven project的形式生成，可以执行mvn eclipse:eclipse来配置依赖，但对于流程设计器Maven依赖不是必须的，只是在单元测试时需要；
```

### Activiti-5.2工作流引擎-源码解析（引擎初始化）
```
ref: http://homeland520.blog.163.com/blog/static/8195886820112125758209/?recommendBlog
org.activiti.engine.impl.cfg. ProcessEngineConfigurationImpl
其初始化的方法为：
 protected void init() {
    initHistoryLevel();
    initExpressionManager();
    initVariableTypes();
    initFormEngines();
    initFormTypes();
    initScriptingEngines();
    initBusinessCalendarManager();
    initCommandContextFactory();
    initTransactionContextFactory();
    initCommandExecutors();
    initServices();						--> 初始化工作流引擎的各个Service
    initIdGenerator();					--> 初始化自增长序列生成器
    initDeployers();
    initJobExecutor();
    initDataSource();					--> 初始化数据源
    initTransactionFactory();			--> 初始化事务工厂
    initSqlSessionFactory();			--> 初始化sqlSessionFactory，主要是初始化iBatis的配置等相关信息
    initSessionFactories();				--> 初始化各个主要Service的SessionFactory
    initJpa();							--> 初始化JPA
  }
  
Activiti-5.2工作流引擎-数据库表结构(持续......) http://homeland520.blog.163.com/blog/static/81958868201112564938465/
	流程文件部署主要涉及到3个表，分别是：ACT_GE_BYTEARRAY、ACT_RE_DEPLOYMENT、ACT_RE_PROCDEF。主要完成“部署包”-->“流程定义文件”-->“所有包内文件”的解析部署关系。
	从表结构中可以看出，流程定义的元素需要每次从数据库加载并解析，因为流程定义的元素没有转化成数据库表来完成，当然流程元素解析后是放在缓存中的
	
Activiti-5.2工作流引擎-源码解析（整体结构）  http://homeland520.blog.163.com/blog/static/8195886820111257735459/
	-- 有各个接口的方法名截图
	RepositoryService 			--> 管理流程部署
	RuntimeService 				--> 管理运行时流程实例的操作
	TaskService 				--> 管理运行时任务的操作
	ManagementService 			--> 管理定时任务的操作
	IdentityService 			--> 管理组织结构的操作
	HistoryService 				--> 管理流程实例、任务实例等历史数据的操作
	FormService 				--> 管理任务表单的操作
```

### Activiti-5.2工作流引擎-源码解析（业务流程部署）  http://homeland520.blog.163.com/blog/static/819588682011213223991/

### 其它
```
http://ecm-kit.15604.n6.nabble.com/Activiti-5-9-td5000877.html#a5000883
5.9版本改变了很多东东，除了解决了很多bug外，还实现了很多新功能，比较突出的几点如下： 
1. 不再有.activiti和.bpmn20.xml 文件了，取而代之的是一个.bpmn文件。所以，以前因为.activiti文件和.bpmn20.xml文件之间相互同步而产生的那些恼人的问题就随之而去了（这是我最happy的地方，以前因为这个问题，我很回避用.activit文件。动不动就产生死锁，i5的台机都要等半天。。。）。所以想从老版本.bpmn20.xml文件移植到新的.bpmn文件，只要简单的重命名文件即可，因此也不存在什么移植过程了。 
2. 支持嵌入式子过程。 
3. 支持signal events和event sub processes。 
4. 支持boundary events。 
5. 升级到Graphiti 0.9。 
6. 解决了很多bug。 
---------------------------------------------------------------------------
Activiti Workflow HelloWorld示例与测试环境搭建 2012-03-21 5.9
http://www.ecmkit.com/2012/03/21/activiti-workflow-hell/
---------------------------------------------------------------------------
从Activiti Designer5.8升级到5.9遇到的问题 2012-05-01 5.9.2
http://www.kafeitu.me/activiti/2012/05/01/activiti-designer-5.8-to-5.9.html
---------------------------------------------------------------------------
在Share中添加工作流
http://www.ecmkit.com/2012/03/29/%E5%9C%A8share%E4%B8%AD%E6%B7%BB%E5%8A%A0%E5%B7%A5%E4%BD%9C%E6%B5%81/
---------------------------------------------------------------------------
Activiti in Action（实战Activiti）-BPMN 2.0 what’s in it for developers.docx
http://ishare.iask.sina.com.cn/f/21631754.html
---------------------------------------------------------------------------
同步或者重构Activiti Identify用户数据的多种方案比较
http://www.kafeitu.me/activiti/2012/04/23/synchronize-or-redesign-user-and-role-for-activiti.html
```