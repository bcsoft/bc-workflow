bc平台工作流

浏览地址： https://github.com/rongjihuang/bc-workflow
源码检出： git@github.com:rongjihuang/bc-workflow.git
依赖：activiti 5.9

一) 数据库脚本运行顺序
建表脚本运行顺序：
activiti.postgresql.create.identity.sql
activiti.postgresql.create.engine.sql
activiti.postgresql.create.history.sql

初始化数据的脚本：
activiti.postgresql.data.bc-workflow.sql
注：activiti.postgresql.data.demo.sql为官方网站上demo的初始化数据，平台中不要执行

删表脚本：
activiti.postgresql.drop..sql

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

)关于流程变量数据的保存总结：
    在流转时，流程变量的值保存在表act_ru_valiable表中，变量名在列name_、变量值在列text_、变量数据类型在列type_(integer|long|string|...)。
    流转总的流程变量在act_hi_detail表中是没有记录的，直到流程结束这些变量的值才会被移动到表中act_hi_detail，
变量名在列、变量值在列、标量数据类型在列。
    如果变量的数据类型为Integer、Long、String类型，则type_列记录的值对应integer|long|string，
对于Integer、Long类型会同时在列long_(bitint)记录其数字值、在text_列记录其文本值。
对于Double类型，type_列记录的值为double，会在列double_列记录数字值，其他值列无值记录。
对于Float类型，是怪胎，最好别用，type_列记录的值为serializable，其值将记录在act_ge_bytearray值,
通过列bytearray_id_的值记录表act_ge_bytearray相应关联数据行的id(1409)，表act_ge_bytearray的列bytes_记录数据序列化后的值。
对于对象类型的流程变量，其type_列保存的值为serializable，其值将记录在act_ge_bytearray表中。

    如果需要流程结束后依然记录流程变量的历史信息，必须将history的级别设置为最高级别full，注意设为full后流转中的变量也已被记录了。
    流程变量的历史信息是记录在表act_hi_detail中的，对应type_列的值为"VariableUpdate",name_列为变量名，var_type_为值类型，
值的记录方式与表act_ru_valiable相同，列名也相同。

) 关于流程变量的值更新
    如果流程变量在不同的任务环节中被修改，历史记录会记录所有变更历史，并通过版本号来区分，每更新一次产生一条新的记录，版本号在原来的基础上加1，
在表act_hi_detail中初始版号为0，在act_ru_valiable中初始版本号为1.特别注意的是act_ru_valiable只会保留最新的更新记录，所有旧版本的
流程变量都不会在表中出现，流转中就已经被移到act_hi_detail中了，act_hi_detail中也会记录当前流转中最新的那条记录。

）关于任务中设置更新流程变量
    使用setValiable方法设置的是全局的流程变量，使用setValiableLocal设置的是当前任务的流程变量，两者互相独立。

）关于表单属性的数据记录与更新
    表单属性的历史信息是记录在表act_hi_detail中的，对应type_列的值为"FormProperty",name_列为变量名，var_type_的值为空
(因只能是String类型)，rev_的值也为空（没有版本号）
值的记录方式与表act_ru_valiable相同，列名也相同。
    特别注意的是，表单属性的名称不能与流程变量的名称相同，否则会乱了，因为表单属性在内部是以流程变量的形式存在的。如果同名了，在提交表单数据后，
除了表单属性有记录外，也会把同名的流程变量当做被更新，历史信息中会同时与流程变量、表单属性的更新记录。
    不过本地流程变量和全局流程变量是可以同名的，系统内部会独立分开处理互不影响。
    在act_ru_valiable中是无法区分是流程变量还是表单属性的，同名的时候只会存在一条记录。

）表单formkey的保存
    流转历史中并没有记录相应任务的formkey，导致查看历史时不知如何加载表单，一个简单的方法就是每次创建任务时在监听器中检测formkey的配置，
如果有就将其以任务本地流程变量的形式记下来，方便日后从历史数据中重新获取。

）表单的加载和渲染：
    submitTaskFormData提交的表单属性永远都是关联到任务上的，也就是说相当于本地流程变量的作用范围。如果要实现全局的表单，
似乎要看看submitStartFormData的函数签名。由于submit后任务会自动结束，所以表单数据的临时保存似乎也是个问题。一个简单的解决方案
是自己建个表单属性数据的保存表，记录待办中任务用户填写并临时保存的表单数据。在任务完成时将其删除（因为已经保存到历史表中无需重复记录）。
快速实现时可以先不支持待办信息的临时保存，必须在某一时刻全部完成。
    读取任务信息时，同时读取表单属性和流程变量信息，如果发现有formkey，将这些信息当做上下文，读取formkey指向的模板，然后用那些
参数进行格式化，再展现给用户。经办表单只读，待办表单对待办人则可编辑。

）表单formkey的格式：
[来源类型]::[显示方式]::[配置值]
来源类型 -- default（默认）、tpl(从模板获取)、url（通过url获取）
显示方式 -- nested（嵌套显示，默认）、seperate（独立窗口显示）

) 自定义表单引擎的方法
activiti的默认表单引擎为： org.activiti.engine.impl.form.FormEngine and org.activiti.engine.impl.form.JuelFormEngine
ProcessEngineConfiguration.customFormEngines
网上某篇自定义表单的文章：https://github.com/peholmst/VaadinActivitiDemo/wiki/Custom-Forms
JUEL不支持Loop的：http://forums.activiti.org/en/viewtopic.php?f=6&t=314&p=1292&hilit=juel+juel+foreach#p1292


