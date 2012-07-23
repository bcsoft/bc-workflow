create table ACT_ID_GROUP (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    TYPE_ varchar(255),
    primary key (ID_)
);
COMMENT ON TABLE ACT_ID_GROUP IS '用户组';
COMMENT ON COLUMN ACT_ID_GROUP.ID_ IS '标识';
COMMENT ON COLUMN ACT_ID_GROUP.REV_ IS '版本';
COMMENT ON COLUMN ACT_ID_GROUP.NAME_ IS '名称';
COMMENT ON COLUMN ACT_ID_GROUP.TYPE_ IS '类型';

create table ACT_ID_MEMBERSHIP (
    USER_ID_ varchar(64),
    GROUP_ID_ varchar(64),
    primary key (USER_ID_, GROUP_ID_)
);
COMMENT ON TABLE ACT_ID_MEMBERSHIP IS '用户和用户组关联关系';
COMMENT ON COLUMN ACT_ID_MEMBERSHIP.USER_ID_ IS '用户ID';
COMMENT ON COLUMN ACT_ID_MEMBERSHIP.GROUP_ID_ IS '用户组ID';

create table ACT_ID_USER (
    ID_ varchar(64),
    REV_ integer,
    FIRST_ varchar(255),
    LAST_ varchar(255),
    EMAIL_ varchar(255),
    PWD_ varchar(255),
    PICTURE_ID_ varchar(64),
    primary key (ID_)
);
COMMENT ON TABLE ACT_ID_USER IS '用户';
COMMENT ON COLUMN ACT_ID_USER.ID_ IS '用户ID,也是登录帐号名';
COMMENT ON COLUMN ACT_ID_USER.REV_ IS '版本号';
COMMENT ON COLUMN ACT_ID_USER.FIRST_ IS '名称';
COMMENT ON COLUMN ACT_ID_USER.LAST_ IS '姓氏';
COMMENT ON COLUMN ACT_ID_USER.EMAIL_ IS '邮箱';
COMMENT ON COLUMN ACT_ID_USER.PWD_ IS '登录密码';
COMMENT ON COLUMN ACT_ID_USER.PICTURE_ID_ IS '';

create table ACT_ID_INFO (
    ID_ varchar(64),
    REV_ integer,
    USER_ID_ varchar(64),
    TYPE_ varchar(64),
    KEY_ varchar(255),
    VALUE_ varchar(255),
    PASSWORD_ bytea,
    PARENT_ID_ varchar(255),
    primary key (ID_)
);

create index ACT_IDX_MEMB_GROUP on ACT_ID_MEMBERSHIP(GROUP_ID_);
alter table ACT_ID_MEMBERSHIP 
    add constraint ACT_FK_MEMB_GROUP
    foreign key (GROUP_ID_) 
    references ACT_ID_GROUP (ID_);

create index ACT_IDX_MEMB_USER on ACT_ID_MEMBERSHIP(USER_ID_);
alter table ACT_ID_MEMBERSHIP 
    add constraint ACT_FK_MEMB_USER
    foreign key (USER_ID_) 
    references ACT_ID_USER (ID_);
