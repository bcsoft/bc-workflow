-- ##BC平台workflow的 postgresql 建表脚本##
-- 流转日志
create table BC_WF_EXCUTION_LOG (
  ID            integer      not null,
  TYPE_         varchar(255) not null,
  LISTENTER     varchar(255) not null,
  EID           varchar(255) not null,
  PID           varchar(255) not null,
  TID           varchar(255),
  ECODE         varchar(255),
  ENAME         varchar(255),
  FORMKEY       varchar(1000),
  DESC_         varchar(1000),
  AUTHOR_ID     integer      not null,
  AUTHOR_CODE   varchar(255) not null,
  AUTHOR_NAME   varchar(255) not null,
  ASSIGNEE_ID   integer,
  ASSIGNEE_CODE varchar(255),
  ASSIGNEE_NAME varchar(255),
  FILE_DATE     timestamp    not null,
  constraint BCWFPK_EXCUTION_LOG primary key (ID)
);
comment on table BC_WF_EXCUTION_LOG is '流转日志';
comment on column BC_WF_EXCUTION_LOG.TYPE_ is '日志类型：参考ExcutionLog.TYPE_XXX常数的定义';
comment on column BC_WF_EXCUTION_LOG.LISTENTER is '监听器类型';
comment on column BC_WF_EXCUTION_LOG.EID is '执行实例的ID';
comment on column BC_WF_EXCUTION_LOG.PID is '流程实例ID';
comment on column BC_WF_EXCUTION_LOG.TID is '任务ID';
comment on column BC_WF_EXCUTION_LOG.ECODE is '执行实例的编码：对应流程、任务、流向的definitionKey';
comment on column BC_WF_EXCUTION_LOG.ENAME is '执行实例的名称：对应流程、任务、流向的名称';
comment on column BC_WF_EXCUTION_LOG.FORMKEY is '表单配置';
comment on column BC_WF_EXCUTION_LOG.DESC_ is '备注';
comment on column BC_WF_EXCUTION_LOG.AUTHOR_ID is '创建人ID(ActorHistory)';
comment on column BC_WF_EXCUTION_LOG.AUTHOR_CODE is '创建人帐号';
comment on column BC_WF_EXCUTION_LOG.AUTHOR_NAME is '创建人姓名';
comment on column BC_WF_EXCUTION_LOG.ASSIGNEE_ID is '处理人ID(ActorHistory)';
comment on column BC_WF_EXCUTION_LOG.ASSIGNEE_CODE is '处理人帐号';
comment on column BC_WF_EXCUTION_LOG.ASSIGNEE_NAME is '处理人姓名';
comment on column BC_WF_EXCUTION_LOG.FILE_DATE is '创建时间';
create index BCWFIDX_EXCUTION_LOG_TASK on BC_WF_EXCUTION_LOG (TYPE_, EID);

-- 流程部署
create table BC_WF_DEPLOY (
  ID            integer      not null,
  UID_          varchar(36),
  DEPLOYMENT_ID varchar(64),
  ORDER_        varchar(255),
  STATUS_       integer      not null default 0,
  TYPE_         integer      not null default 0,
  CATEGORY      varchar(255) not null,
  CODE          varchar(255) not null,
  VERSION_      varchar(255) not null,
  SUBJECT       varchar(255),
  PATH          varchar(255),
  SIZE_         integer      not null default 0,
  DESC_         varchar(4000),
  SOURCE        varchar(255),
  DEPLOYER_ID   integer,
  DEPLOY_DATE   timestamp,
  FILE_DATE     timestamp    not null,
  AUTHOR_ID     integer      not null,
  MODIFIER_ID   integer,
  MODIFIED_DATE timestamp,
  constraint BCPK_WF_DEPLOY primary key (ID)
);
comment on table BC_WF_DEPLOY is '流程部署管理';
comment on column BC_WF_DEPLOY.ORDER_ is '排序号';
comment on column BC_WF_DEPLOY.STATUS_ is '状态：0-已发布,-1-未发布';
comment on column BC_WF_DEPLOY.TYPE_ is '类型：0-XML,1-BAR';
comment on column BC_WF_DEPLOY.CATEGORY is '所属分类';
comment on column BC_WF_DEPLOY.CODE is '编码';
comment on column BC_WF_DEPLOY.VERSION_ is '版本号';
comment on column BC_WF_DEPLOY.SUBJECT is '标题';
comment on column BC_WF_DEPLOY.PATH is '物理文件保存的相对路径';
comment on column BC_WF_DEPLOY.DESC_ is '描述';
comment on column BC_WF_DEPLOY.SOURCE is '原始文件名';
comment on column BC_WF_DEPLOY.DEPLOYER_ID is '最后部署人';
comment on column BC_WF_DEPLOY.DEPLOY_DATE is '最后部署时间';
comment on column BC_WF_DEPLOY.FILE_DATE is '创建时间';
comment on column BC_WF_DEPLOY.AUTHOR_ID is '创建人ID';
comment on column BC_WF_DEPLOY.MODIFIER_ID is '最后修改人ID';
comment on column BC_WF_DEPLOY.MODIFIED_DATE is '最后修改时间';
alter table BC_WF_DEPLOY
  add constraint BCFK_WF_DEPLOY_DEPLOYER foreign key (DEPLOYER_ID)
references BC_IDENTITY_ACTOR_HISTORY (ID);
alter table BC_WF_DEPLOY
  add constraint BCFK_WF_DEPLOY_AUTHORID foreign key (AUTHOR_ID)
references BC_IDENTITY_ACTOR_HISTORY (ID);
alter table BC_WF_DEPLOY
  add constraint BCFK_WF_DEPLOY_MODIFIER foreign key (MODIFIER_ID)
references BC_IDENTITY_ACTOR_HISTORY (ID);
alter table BC_WF_DEPLOY
  add constraint BCUK_WF_DEPLOY_CODE_VERSION unique (CODE, VERSION_);

-- 流程部署使用人
create table BC_WF_DEPLOY_ACTOR (
  DID integer not null,
  AID integer not null,
  constraint BCPK_WF_DEPLOY_ACTOR primary key (DID, AID)
);
comment on table BC_WF_DEPLOY_ACTOR is '流程部署使用人';
comment on column BC_WF_DEPLOY_ACTOR.DID is '流程部署id';
comment on column BC_WF_DEPLOY_ACTOR.AID is '使用人id';
alter table BC_WF_DEPLOY_ACTOR
  add constraint BCFK_BC_WF_DEPLOY_ACTOR_DEPLOY foreign key (DID)
references BC_WF_DEPLOY (ID);
alter table BC_WF_DEPLOY_ACTOR
  add constraint BCFK_BC_WF_DEPLOY_ACTOR_ACTOR foreign key (AID)
references BC_IDENTITY_ACTOR (ID);

-- 流程附件、意见
create table BC_WF_ATTACH (
  ID            integer      not null,
  UID_          varchar(36)  not null,
  TID           varchar(255),
  PID           varchar(255) not null,
  TYPE_         int,
  COMMON        boolean      not null default true,
  SUBJECT       varchar(255),
  PATH_         varchar(255),
  EXT           varchar(255),
  SIZE_         integer               default 0,
  DESC_         varchar(4000),
  FORMATTED     boolean,
  TEMPLATE_ID   integer,
  FILE_DATE     timestamp    not null,
  AUTHOR_ID     integer      not null,
  MODIFIED_DATE timestamp,
  MODIFIER_ID   integer,
  constraint BCPK_WF_ATTACH primary key (ID)
);
comment on table BC_WF_ATTACH is '流程附加信息';
comment on column BC_WF_ATTACH.TID is '流程任务id';
comment on column BC_WF_ATTACH.PID is '流程实例id';
comment on column BC_WF_ATTACH.TYPE_ is '类型：1-附件，2-意见';
comment on column BC_WF_ATTACH.COMMON is '是否为公共信息，true是，false任务信息';
comment on column BC_WF_ATTACH.SUBJECT is '标题';
comment on column BC_WF_ATTACH.PATH_ is '附件路径';
comment on column BC_WF_ATTACH.EXT is '附件扩展名';
comment on column BC_WF_ATTACH.SIZE_ is '附件大小';
comment on column BC_WF_ATTACH.DESC_ is '附加信息的类型为附件时:附件备注。附加信息的类型为意见时:意见信息。';
comment on column BC_WF_ATTACH.FORMATTED is '附件是否需要格式化,类型为意见时字段为空';
comment on column BC_WF_ATTACH.TEMPLATE_ID is '模板ID';
comment on column BC_WF_ATTACH.FILE_DATE is '创建时间';
comment on column BC_WF_ATTACH.AUTHOR_ID is '创建人ID';
comment on column BC_WF_ATTACH.MODIFIED_DATE is '最后修改时间';
comment on column BC_WF_ATTACH.MODIFIER_ID is '最后修改人ID';
alter table BC_WF_ATTACH
  add constraint BCFK_WF_ATTACH_AUTHOR foreign key (AUTHOR_ID)
references BC_IDENTITY_ACTOR_HISTORY (ID);
alter table BC_WF_ATTACH
  add constraint BCFK_WF_ATTACH_MODIFIER foreign key (MODIFIER_ID)
references BC_IDENTITY_ACTOR_HISTORY (ID);
