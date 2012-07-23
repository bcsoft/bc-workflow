-- http://homeland520.blog.163.com/blog/static/81958868201112564938465/
create table ACT_GE_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
);
COMMENT ON TABLE ACT_GE_PROPERTY IS '属性数据表,存储整个流程引擎级别的数据';
COMMENT ON COLUMN ACT_GE_PROPERTY.NAME_ IS '属性名称,如activiti的数据脚本名schema.version';
COMMENT ON COLUMN ACT_GE_PROPERTY.VALUE_ IS '属性值,如activiti的版本号5.9';
COMMENT ON COLUMN ACT_GE_PROPERTY.REV_ IS '修订号';

insert into ACT_GE_PROPERTY
values ('schema.version', '5.9', 1);

insert into ACT_GE_PROPERTY
values ('schema.history', 'create(5.9)', 1);

insert into ACT_GE_PROPERTY
values ('next.dbid', '1000', 1);

create table ACT_GE_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ bytea,
    GENERATED_ boolean,
    primary key (ID_)
);
COMMENT ON TABLE ACT_GE_BYTEARRAY IS '用来保存部署文件的大文本数据';
COMMENT ON COLUMN ACT_GE_BYTEARRAY.ID_ IS '资源文件编号,自增长';
COMMENT ON COLUMN ACT_GE_BYTEARRAY.REV_ IS '版本号';
COMMENT ON COLUMN ACT_GE_BYTEARRAY.NAME_ IS '资源文件名称';
COMMENT ON COLUMN ACT_GE_BYTEARRAY.DEPLOYMENT_ID_ IS '来自于父表ACT_RE_DEPLOYMENT中的主键';
COMMENT ON COLUMN ACT_GE_BYTEARRAY.BYTES_ IS '存储文本字节流';
COMMENT ON COLUMN ACT_GE_BYTEARRAY.GENERATED_ IS '';

create table ACT_RE_DEPLOYMENT (
    ID_ varchar(64),
    NAME_ varchar(255),
    DEPLOY_TIME_ timestamp,
    primary key (ID_)
);
COMMENT ON TABLE ACT_RE_DEPLOYMENT IS '用来存储部署时需要被持久化保存下来的信息';
COMMENT ON COLUMN ACT_RE_DEPLOYMENT.ID_ IS '部署编号,自增长';
COMMENT ON COLUMN ACT_RE_DEPLOYMENT.NAME_ IS '部署的包名称';
COMMENT ON COLUMN ACT_RE_DEPLOYMENT.DEPLOY_TIME_ IS '部署时间';

create table ACT_RU_EXECUTION (
    ID_ varchar(64),
    REV_ integer,
    PROC_INST_ID_ varchar(64),
    BUSINESS_KEY_ varchar(255),
    PARENT_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SUPER_EXEC_ varchar(64),
    ACT_ID_ varchar(255),
    IS_ACTIVE_ boolean,
    IS_CONCURRENT_ boolean,
    IS_SCOPE_ boolean,
    IS_EVENT_SCOPE_ boolean,
    SUSPENSION_STATE_ integer,
    primary key (ID_),
    unique (PROC_DEF_ID_, BUSINESS_KEY_)
);
COMMENT ON TABLE ACT_RU_EXECUTION IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.ID_ IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.REV_ IS '版本号';
COMMENT ON COLUMN ACT_RU_EXECUTION.PROC_INST_ID_ IS '流程实例编号';
COMMENT ON COLUMN ACT_RU_EXECUTION.BUSINESS_KEY_ IS '业务编号';
COMMENT ON COLUMN ACT_RU_EXECUTION.PARENT_ID_ IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.PROC_DEF_ID_ IS '流程ID';
COMMENT ON COLUMN ACT_RU_EXECUTION.SUPER_EXEC_ IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.ACT_ID_ IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.IS_ACTIVE_ IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.IS_CONCURRENT_ IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.IS_SCOPE_ IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.IS_EVENT_SCOPE_ IS '';
COMMENT ON COLUMN ACT_RU_EXECUTION.SUSPENSION_STATE_ IS '';

create table ACT_RU_JOB (
    ID_ varchar(64) NOT NULL,
	  REV_ integer,
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    primary key (ID_)
);
COMMENT ON TABLE ACT_RU_JOB IS '运行时定时任务数据表';

-- 注意：此表与ACT_RE_DEPLOYMENT是多对一的关系，即一个部署的bar包里可能包含多个流程定义文件，
-- 每个流程定义文件都会有一条记录在ACT_RE_PROCDEF表内，每条流程定义的数据，都会对应ACT_GE_BYTEARRAY表
-- 内的一个资源文件和PNG图片文件。
-- 与ACT_GE_BYTEARRAY的关联是通过程序用ACT_GE_BYTEARRAY.NAME_与ACT_RE_PROCDEF.RESOURCE_NAME_完成的，
-- 在数据库表结构内没有体现。
create table ACT_RE_PROCDEF (
    ID_ varchar(64),
    REV_ integer,
    CATEGORY_ varchar(255),
    NAME_ varchar(255),
    KEY_ varchar(255),
    VERSION_ integer,
    DEPLOYMENT_ID_ varchar(64),
    RESOURCE_NAME_ varchar(4000),
    DGRM_RESOURCE_NAME_ varchar(4000),
    HAS_START_FORM_KEY_ boolean,
    SUSPENSION_STATE_ integer,
    primary key (ID_)
);
COMMENT ON TABLE ACT_RE_PROCDEF IS '业务流程定义数据表';
COMMENT ON COLUMN ACT_RE_PROCDEF.ID_ IS '流程ID,由“流程编号:流程版本号:自增长ID”组成';
COMMENT ON COLUMN ACT_RE_PROCDEF.REV_ IS '';
COMMENT ON COLUMN ACT_RE_PROCDEF.CATEGORY_ IS '流程命令空间(该编号是流程文件targetNamespace的属性值)';
COMMENT ON COLUMN ACT_RE_PROCDEF.NAME_ IS '流程名称(该编号就是流程文件process元素的name属性值)';
COMMENT ON COLUMN ACT_RE_PROCDEF.KEY_ IS '流程编号(该编号就是流程文件process元素的id属性值)';
COMMENT ON COLUMN ACT_RE_PROCDEF.VERSION_ IS '流程版本号(由程序控制,新增即为1,修改后依次加1)';
COMMENT ON COLUMN ACT_RE_PROCDEF.DEPLOYMENT_ID_ IS '部署编号';
COMMENT ON COLUMN ACT_RE_PROCDEF.RESOURCE_NAME_ IS '资源文件名称';
COMMENT ON COLUMN ACT_RE_PROCDEF.DGRM_RESOURCE_NAME_ IS '图片资源文件名称';
COMMENT ON COLUMN ACT_RE_PROCDEF.HAS_START_FORM_KEY_ IS '是否有Start Form Key';
COMMENT ON COLUMN ACT_RE_PROCDEF.SUSPENSION_STATE_ IS '';

create table ACT_RU_TASK (
    ID_ varchar(64),
    REV_ integer,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    TASK_DEF_KEY_ varchar(255),
    OWNER_ varchar(64),
    ASSIGNEE_ varchar(64),
    DELEGATION_ varchar(64),
    PRIORITY_ integer,
    CREATE_TIME_ timestamp,
    DUE_DATE_ timestamp,
    primary key (ID_)
);
COMMENT ON TABLE ACT_RU_TASK IS '运行时任务数据表';

create table ACT_RU_IDENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    GROUP_ID_ varchar(64),
    TYPE_ varchar(255),
    USER_ID_ varchar(64),
    TASK_ID_ varchar(64),
    primary key (ID_)
);
COMMENT ON TABLE ACT_RU_IDENTITYLINK IS '任务参与者数据表,存储当前节点参与者的信息';

create table ACT_RU_VARIABLE (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(255) not null,
    NAME_ varchar(255) not null,
    EXECUTION_ID_ varchar(64),
	  PROC_INST_ID_ varchar(64),
    TASK_ID_ varchar(64),
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double precision,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
);
COMMENT ON TABLE ACT_RU_VARIABLE IS '运行时流程变量数据表';

create table ACT_RU_EVENT_SUBSCR (
    ID_ varchar(64) not null,
    REV_ integer,
    EVENT_TYPE_ varchar(255) not null,
    EVENT_NAME_ varchar(255),
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTIVITY_ID_ varchar(64),
    CONFIGURATION_ varchar(255),
    CREATED_ timestamp not null,
    primary key (ID_)
);

create index ACT_IDX_EXEC_BUSKEY on ACT_RU_EXECUTION(BUSINESS_KEY_);
create index ACT_IDX_TASK_CREATE on ACT_RU_TASK(CREATE_TIME_);
create index ACT_IDX_IDENT_LNK_USER on ACT_RU_IDENTITYLINK(USER_ID_);
create index ACT_IDX_IDENT_LNK_GROUP on ACT_RU_IDENTITYLINK(GROUP_ID_);
create index ACT_IDX_EVENT_SUBSCR_CONFIG_ on ACT_RU_EVENT_SUBSCR(CONFIGURATION_);

create index ACT_IDX_BYTEAR_DEPL on ACT_GE_BYTEARRAY(DEPLOYMENT_ID_);
alter table ACT_GE_BYTEARRAY
    add constraint ACT_FK_BYTEARR_DEPL
    foreign key (DEPLOYMENT_ID_) 
    references ACT_RE_DEPLOYMENT (ID_);

create index ACT_IDX_EXE_PROCINST on ACT_RU_EXECUTION(PROC_INST_ID_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PROCINST 
    foreign key (PROC_INST_ID_) 
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_EXE_PARENT on ACT_RU_EXECUTION(PARENT_ID_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_PARENT
    foreign key (PARENT_ID_) 
    references ACT_RU_EXECUTION (ID_);
    
create index ACT_IDX_EXE_SUPER on ACT_RU_EXECUTION(SUPER_EXEC_);
alter table ACT_RU_EXECUTION
    add constraint ACT_FK_EXE_SUPER
    foreign key (SUPER_EXEC_) 
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_TSKASS_TASK on ACT_RU_IDENTITYLINK(TASK_ID_);
alter table ACT_RU_IDENTITYLINK
    add constraint ACT_FK_TSKASS_TASK
    foreign key (TASK_ID_) 
    references ACT_RU_TASK (ID_);
    
create index ACT_IDX_TASK_EXEC on ACT_RU_TASK(EXECUTION_ID_);
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_EXE
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_);
    
create index ACT_IDX_TASK_PROCINST on ACT_RU_TASK(PROC_INST_ID_);
alter table ACT_RU_TASK
    add constraint ACT_FK_TASK_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_);
    
create index ACT_IDX_TASK_PROCDEF on ACT_RU_TASK(PROC_DEF_ID_);
alter table ACT_RU_TASK
  add constraint ACT_FK_TASK_PROCDEF
  foreign key (PROC_DEF_ID_)
  references ACT_RE_PROCDEF (ID_);
  
create index ACT_IDX_VAR_EXE on ACT_RU_VARIABLE(EXECUTION_ID_);
alter table ACT_RU_VARIABLE 
    add constraint ACT_FK_VAR_EXE
    foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_);

create index ACT_IDX_VAR_PROCINST on ACT_RU_VARIABLE(PROC_INST_ID_);
alter table ACT_RU_VARIABLE
    add constraint ACT_FK_VAR_PROCINST
    foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION(ID_);

create index ACT_IDX_VAR_BYTEARRAY on ACT_RU_VARIABLE(BYTEARRAY_ID_);
alter table ACT_RU_VARIABLE 
    add constraint ACT_FK_VAR_BYTEARRAY 
    foreign key (BYTEARRAY_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_JOB_EXCEPTION on ACT_RU_JOB(EXCEPTION_STACK_ID_);
alter table ACT_RU_JOB 
    add constraint ACT_FK_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_) 
    references ACT_GE_BYTEARRAY (ID_);

create index ACT_IDX_EVENT_SUBSCR on ACT_RU_EVENT_SUBSCR(EXECUTION_ID_);
alter table ACT_RU_EVENT_SUBSCR
    add constraint ACT_FK_EVENT_EXEC
    foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION(ID_);
	
-- ADD BY DRAGON
create index ACT_FK_VAR_TASK on ACT_RU_VARIABLE(TASK_ID_);
create index ACT_FK_PROCDEF_DEPLOYMENT on ACT_RE_PROCDEF(DEPLOYMENT_ID_);
create index ACT_FK_TASK_PARENT on ACT_RU_TASK(PARENT_TASK_ID_);
