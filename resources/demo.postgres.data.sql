-- 设置id从1000开始，保留1000个id内部使用
update ACT_GE_PROPERTY set VALUE_ = '1000' where NAME_ = 'next.dbid';

-- 清空Activiti的用户、用户组信息
delete from act_id_membership;
delete from act_id_user;
delete from act_id_group;

-- 同步用户数据到Activiti的用户表ACT_ID_USER
INSERT INTO act_id_user(id_, rev_, first_, last_, email_, pwd_, picture_id_)
    select a.code,1,a.name,null,a.email,a.code,null
		from bc_identity_actor a 
		where a.type_=4 
		and not exists (select 0 from act_id_user where id_ = a.code)
		order by a.order_;

-- 同步用户组数据到Activiti的用户组表ACT_ID_GROUP
INSERT INTO act_id_group(id_, rev_, name_, type_)
    select a.code,1,a.name,'security-role'
		from bc_identity_actor a 
		where a.type_=3 
		and not exists (select 0 from act_id_group where id_ = a.code)
		order by a.order_;

-- 同步用户、用户组之间的关联关系数据到Activiti的用户、用户组表ACT_ID_GROUP
INSERT INTO act_id_membership(user_id_, group_id_)
    select u.code,g.code
		from bc_identity_actor_relation r
		inner join bc_identity_actor g on g.id=r.master_id
		inner join bc_identity_actor u on u.id=r.follower_id
		where r.type_=0 and g.type_=3 and u.type_=4 
		and not exists (select 0 from act_id_membership where group_id_ = g.code and user_id_ = u.code)
		order by g.order_,r.order_;