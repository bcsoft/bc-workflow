<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<div class="sidebar-todo"
	data-js='<s:url value="/bc-workflow/todo/sidebar.js"/>'
	data-initMethod='bc.sidebar.todo.init' style="overflow-y: hidden;">
	<!-- 大标题 -->
	<table class="ui-widget-header" cellspacing="0" cellpadding="0">
		<tr>
			<td style="width: 16px;"><span
				class="custom inputIcon ui-icon ui-icon-calendar"></span></td>
			<td style="text-align: left;">待办事项</td>
			<td style="text-align: right;">
				<ul class="inputIcons">
					<li class="more custom inputIcon ui-icon ui-icon-folder-open" title="点击查看更多"></li>
				</ul>
			</td>
		</tr>
	</table>
	
	<!-- 没有待办时显示的提示 -->
	<div class="empty ui-widget-content">(没有待办信息)</div>
	
	<!-- 待办任务列表 -->
	<table class="tasks ui-widget-content" cellspacing="0" cellpadding="0">
		<tr class="task">
			<td class="ui-widget-content">
				<div class="simple ui-state-default"><span class="icons"><span title="折叠|展开任务详情" 
					class="toggle ui-icon ui-icon-carat-1-nw"></span></span><span title="点击领取任务"
					class="name">最简单的发送到人的任务</span>
				</div>
				<div class="detail">
					<div class="createTime low">发起时间：2012-01-01 08:30</div>
					<div class="category low">所属分类：月即将退出营运车辆处理流程</div>
				</div>
			</td>
		</tr>
		<tr class="task">
			<td class="ui-widget-content">
				<div class="simple ui-state-default"><span class="icons"><span title="折叠|展开任务详情" 
					class="toggle ui-icon ui-icon-carat-1-nw"></span><span title="此任务有办理期限"
					class="alarm ui-icon ui-icon-clock"></span><span title="点击领取任务"
					class="group ui-icon ui-icon-person"></span></span><span 
					class="name">标题很长、有办理期限、发送到组的任务，很长很长很长很长很长很长</span>
				</div>
				<div class="detail">
					<div class="durdate"><span class="content ui-state-highlight">办理期限：2012-01-01 12:00</span></div>
					<div class="desc low">附加说明：请尽快安排人员符合交车汇总信息，落实哪些车辆交车、哪些车辆需要续保。</div>
					<div class="createTime low">发起时间：2012-01-01 08:30</div>
					<div class="category low">所属分类：月即将退出营运车辆处理流程</div>
				</div>
			</td>
		</tr>
	</table>
	<s:iterator value="todoList">
		<table class="ui-widget-content" cellspacing="0" cellpadding="0" style="display:none;">
			<tr>
				<td class="ui-widget-content"
					style="width: 30px; text-align: center; vertical-align: top; border-width: 0px 1px 1px 0px;">
					<img id="portrait"
					style="width: 30px; height: 33px; cursor: pointer;"
					src='<s:url value="/bc/image/userPortrait"></s:url>?code=admin' />
				</td>
				<td>
					<table class="ui-widget-content" cellspacing="0" cellpadding="0"
						style="width: 209px; border-width: 0px 0px 1px 0px;">
						<tr>
							<td><s:if test="%{['todoType'] == 0}">
									<span class="ui-icon ui-icon-person"></span>
								</s:if> <s:else>
									<span class="ui-icon ui-icon-contact"></span>
								</s:else></td>
							<td><s:if test="%{['isTimeOver'] == true}">
									<span class="ui-icon ui-icon-clock"></span>
								</s:if></td>
							<td style="width: 107px; text-align: left; font-size: 14px;"><div
									style="width: 107px; text-overflow: ellipsis; white-space: nowrap;">
									<s:property value="%{['title']}" />
								</div></td>
							<td style="width: 70px; text-align: right;"><div
									style="width: 70px; text-overflow: ellipsis; white-space: nowrap;">
									<s:property value="%{['createTime']}" />
								</div></td>
						</tr>
						<tr>
							<td colspan="4" style="text-align: left;"><div
									style="margin-left: 2px; width: 207px; text-overflow: ellipsis; white-space: nowrap;">
									<s:property value="%{['description']}" />
								</div></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
	</s:iterator>
</div>