<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<div class="ws" title='<s:property value="title"/>'
	data-js='<s:url value="/bc-workflow/workspace/workspace.js"/>'
	data-initMethod='bc.flow.workspace.init'
	data-option='<s:property value="pageOption"/>' style="overflow-y: auto;">
	<!-- 异常信息区 -->
	<div class="error" style="display:none;"><s:property value="msg"/>(打开工作空间异常----这里输出友好点的异常信息)</div>
	
	<!-- 公共信息区 -->
	<div class="common ui-widget-content">
		<!-- 标题行 -->
		<div class="header line ui-widget-header">
			<span class="leftIcon ui-icon ui-icon-suitcase"></span>
			<span class="text">公共信息</span>
			<span class="rightIcons">
				<span class="mainOperate flowImage"><span class="ui-icon ui-icon-image"></span><span class="text link">查看流程图</span></span>
				<span class="mainOperate addComment"><span class="ui-icon ui-icon-document"></span><span class="text link">添加意见</span></span>
				<span class="mainOperate addAttach"><span class="ui-icon ui-icon-arrowthick-1-n"></span><span class="text link">添加附件</span></span>
				<span class="reverse"><span class="ui-icon ui-icon-carat-2-n-s" title="反转详细信息区域的显示"></span></span>
				<span class="toggle"><span class="ui-icon ui-icon-triangle-1-n" title="折叠|展开公共信息"></span></span>
			</span>
		</div>
		<!-- 信息列表 -->
		<div class="info ui-widget-content">
			<div class="simple">
				<div class="line form">
					<span class="leftIcon ui-icon ui-icon-document"></span>
					<span class="text link">表单：月即将退出营运车辆确认表</span>
					<span class="rightIcons">
						<span class="itemOperate edit"><span class="ui-icon ui-icon-pencil"></span><span class="text link">编辑</span></span>
						<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
						<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
						<span class="itemOperate delete"><span class="ui-icon ui-icon-closethick"></span><span class="text link">删除</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail low little">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">小张 2012-01-01 08:00</span>
				</div>
			</div>
		</div>
		<div class="info ui-widget-content">
			<div class="simple">
				<div class="line comment">
					<span class="leftIcon ui-icon ui-icon-comment"></span>
					<span class="text link">意见：测试全局意见</span>
					<span class="rightIcons">
						<span class="itemOperate edit"><span class="ui-icon ui-icon-pencil"></span><span class="text link">编辑</span></span>
						<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
						<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
						<span class="itemOperate delete"><span class="ui-icon ui-icon-closethick"></span><span class="text link">删除</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail low little">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">小张 2012-01-01 08:00</span>
				</div>
			</div>
		</div>
		<div class="info ui-widget-content">
			<div class="simple">
				<div class="line attach">
					<span class="leftIcon ui-icon ui-icon-link"></span>
					<span class="text link">附件：测试全局附件</span>
					<span class="rightIcons">
						<span class="itemOperate edit"><span class="ui-icon ui-icon-pencil"></span><span class="text link">编辑</span></span>
						<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
						<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
						<span class="itemOperate delete"><span class="ui-icon ui-icon-closethick"></span><span class="text link">删除</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail low little">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">小张 2012-01-01 08:00</span>
				</div>
			</div>
		</div>
		<div class="info ui-widget-content">
			<div class="simple">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-flag"></span>
					<span class="text">统计信息</span>
					<span class="rightIcons">
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail low little">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">发起时间：2012-01-01 08:00 张三</span>
				</div>
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">结束时间：(未结束)</span>
				</div>
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">办理耗时：2天13小时</span>
				</div>
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">参与人数：10人</span>
				</div>
			</div>
		</div>
	</div>
	
	<!-- 待办信息区 -->
	<div class="todo ui-widget-content">
		<!-- 标题行 -->
		<div class="header line ui-widget-header">
			<span class="leftIcon ui-icon ui-icon-calendar"></span>
			<span class="text">待办信息</span>
			<span class="rightIcons">
				<span class="reverse"><span class="ui-icon ui-icon-carat-2-n-s" title="反转详细信息区域的显示"></span></span>
				<span class="toggle"><span class="ui-icon ui-icon-triangle-1-n" title="折叠|展开公共信息"></span></span>
			</span>
		</div>
		<!-- 信息列表 -->
		<!-- 我的个人任务模板 -->
		<div class="info">
			<div class="simple">
				<div class="line topic ui-state-default ui-state-highlight">
					<span class="leftIcon ui-icon ui-icon-person"></span>
					<span class="text">我的个人任务(背景高亮显示)</span>
					<span class="rightIcons">
						<span class="mainOperate addComment"><span class="ui-icon ui-icon-document"></span><span class="text link">添加意见</span></span>
						<span class="mainOperate addAttach"><span class="ui-icon ui-icon-arrowthick-1-n"></span><span class="text link">添加附件</span></span>
						<span class="mainOperate delegate"><span class="ui-icon ui-icon-person"></span><span class="text link">委派任务</span></span>
						<span class="mainOperate finish"><span class="ui-icon ui-icon-check"></span><span class="text link">完成办理</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-clock"></span>
					<span class="text ui-state-focus">办理期限：2012-01-01 08:00</span>
				</div>
				<div class="line info">
					<div class="simple">
						<div class="line form">
							<div class="ui-widget-content form">[内嵌表单HTML]</div>
						</div>
					</div>
				</div>
				<div class="line info">
					<div class="simple">
						<div class="line form">
							<span class="leftIcon ui-icon ui-icon-document"></span>
							<span class="text link">独立窗口表单：测试任务表单</span>
							<span class="rightIcons">
								<span class="itemOperate edit"><span class="ui-icon ui-icon-pencil"></span><span class="text link">编辑</span></span>
								<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
								<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
								<span class="itemOperate delete"><span class="ui-icon ui-icon-closethick"></span><span class="text link">删除</span></span>
								<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
							</span>
						</div>
					</div>
					<div class="detail">
						<div class="line">
							<div class="ui-widget-content form">[表单详细信息]</div>
						</div>
					</div>
				</div>
				<div class="line info">
					<div class="simple">
						<div class="line comment">
							<span class="leftIcon ui-icon ui-icon-comment"></span>
							<span class="text link">意见：测试任务意见</span>
							<span class="rightIcons">
								<span class="itemOperate edit"><span class="ui-icon ui-icon-pencil"></span><span class="text link">编辑</span></span>
								<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
								<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
								<span class="itemOperate delete"><span class="ui-icon ui-icon-closethick"></span><span class="text link">删除</span></span>
								<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
							</span>
						</div>
					</div>
					<div class="detail low little">
						<div class="line">
							<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
							<span class="text">小张 2012-01-01 08:00</span>
						</div>
					</div>
				</div>
				<div class="line info">
					<div class="simple">
						<div class="line attach">
							<span class="leftIcon ui-icon ui-icon-link"></span>
							<span class="text link">附件：测试任务附件</span>
							<span class="rightIcons">
								<span class="itemOperate edit"><span class="ui-icon ui-icon-pencil"></span><span class="text link">编辑</span></span>
								<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
								<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
								<span class="itemOperate delete"><span class="ui-icon ui-icon-closethick"></span><span class="text link">删除</span></span>
								<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
							</span>
						</div>
					</div>
					<div class="detail low little">
						<div class="line">
							<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
							<span class="text">小张 2012-01-01 08:00</span>
						</div>
					</div>
				</div>
				<div class="line low">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">发起时间：2012-01-01 08:00</span>
				</div>
			</div>
		</div>
		<!-- 我的岗位任务模板 -->
		<div class="info ui-widget-content">
			<div class="simple">
				<div class="line topic ui-state-default ui-state-highlight">
					<span class="leftIcon ui-icon ui-icon-home"></span>
					<span class="text">我的岗位任务</span>
					<span class="rightIcons">
						<span class="mainOperate assign"><span class="ui-icon ui-icon-person"></span><span class="text link">分派任务</span></span>
						<span class="mainOperate claim"><span class="ui-icon ui-icon-check"></span><span class="text link">签领任务</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail low">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">待办岗：综合业务岗</span>
				</div>
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">发起时间：2012-01-01 08:00</span>
				</div>
			</div>
		</div>
		<!-- 别人的待办任务模板 -->
		<div class="info collapse">
			<div class="simple">
				<div class="line topic ui-state-default">
					<span class="leftIcon ui-icon ui-icon-cancel"></span>
					<span class="text">别人的待办任务1</span>
					<span class="rightIcons">
						<span class="text"><span class="ui-icon ui-icon-person"></span><span class="text">李四</span></span>
						<span class="text"><span class="ui-icon ui-icon-clock"></span><span class="text">2012-01-01 08:00</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-sw" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail low">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">待办人：李四</span>
				</div>
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">发起时间：2012-01-01 08:00</span>
				</div>
			</div>
		</div>
		<div class="info collapse">
			<div class="simple">
				<div class="line topic ui-state-default">
					<span class="leftIcon ui-icon ui-icon-cancel"></span>
					<span class="text">别人的待办任务2</span>
					<span class="rightIcons">
						<span class="text"><span class="ui-icon ui-icon-person"></span><span class="text">李四</span></span>
						<span class="text"><span class="ui-icon ui-icon-clock"></span><span class="text">2012-01-01 08:00</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-sw" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail low">
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">待办人：李四</span>
				</div>
				<div class="line">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">发起时间：2012-01-01 08:00</span>
				</div>
			</div>
		</div>
	</div>
	
	<!-- 已办信息区 -->
	<div class="done ui-widget-content">
		<!-- 标题行 -->
		<div class="header line ui-widget-header">
			<span class="leftIcon ui-icon ui-icon-tag"></span>
			<span class="text">已办信息</span>
			<span class="rightIcons">
				<span class="reverse"><span class="ui-icon ui-icon-carat-2-n-s" title="反转详细信息区域的显示"></span></span>
				<span class="toggle"><span class="ui-icon ui-icon-triangle-1-n" title="折叠|展开公共信息"></span></span>
			</span>
		</div>
		<!-- 信息列表 -->
		<div class="info">
			<div class="simple">
				<div class="line topic ui-state-default">
					<span class="leftIcon ui-icon ui-icon-flag"></span>
					<span class="text">我办过的某任务1</span>
					<span class="rightIcons">
						<span class="text"><span class="ui-icon ui-icon-person"></span><span class="text">张三</span></span>
						<span class="text"><span class="ui-icon ui-icon-clock"></span><span class="text">2012-01-02 10:00</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail">
				<div class="line low">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">办理耗时：2012-01-01 08:00～2012-01-02 10:00 (1天2小时)</span>
				</div>
				<div class="line info collapse">
					<div class="simple">
						<div class="line form">
							<span class="leftIcon ui-icon ui-icon-document"></span>
							<span class="text link">表单：测试任务表单</span>
							<span class="rightIcons">
								<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
								<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
								<span class="toggle"><span class="ui-icon ui-icon-carat-1-sw" title="折叠|展开详细信息"></span></span>
							</span>
						</div>
					</div>
					<div class="detail low little">
						<div class="line">
							<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
							<span class="text">小张 2012-01-01 08:00</span>
						</div>
					</div>
				</div>
				<div class="line info collapse">
					<div class="simple">
						<div class="line comment">
							<span class="leftIcon ui-icon ui-icon-comment"></span>
							<span class="text link">意见：测试任务意见</span>
							<span class="rightIcons">
								<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
								<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
								<span class="toggle"><span class="ui-icon ui-icon-carat-1-sw" title="折叠|展开详细信息"></span></span>
							</span>
						</div>
					</div>
					<div class="detail low little">
						<div class="line">
							<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
							<span class="text">小张 2012-01-01 08:00</span>
						</div>
					</div>
				</div>
				<div class="line info collapse">
					<div class="simple">
						<div class="line attach">
							<span class="leftIcon ui-icon ui-icon-link"></span>
							<span class="text link">附件：测试任务附件</span>
							<span class="rightIcons">
								<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
								<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
								<span class="toggle"><span class="ui-icon ui-icon-carat-1-sw" title="折叠|展开详细信息"></span></span>
							</span>
						</div>
					</div>
					<div class="detail low little">
						<div class="line">
							<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
							<span class="text">小张 2012-01-01 08:00</span>
						</div>
					</div>
				</div>
			</div>
		</div>
		<div class="info">
			<div class="simple">
				<div class="line topic ui-state-default">
					<span class="leftIcon ui-icon ui-icon-flag"></span>
					<span class="text">别人办过的某任务1</span>
					<span class="rightIcons">
						<span class="text"><span class="ui-icon ui-icon-person"></span><span class="text">李四</span></span>
						<span class="text"><span class="ui-icon ui-icon-clock"></span><span class="text">2012-01-02 15:00</span></span>
						<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>
					</span>
				</div>
			</div>
			<div class="detail">
				<div class="line low">
					<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
					<span class="text">办理耗时：2012-01-01 08:00～2012-01-02 10:00 (1天2小时)</span>
				</div>
				<div class="line info collapse">
					<div class="simple">
						<div class="line comment">
							<span class="leftIcon ui-icon ui-icon-comment"></span>
							<span class="text link">意见：测试任务意见</span>
							<span class="rightIcons">
								<span class="itemOperate open"><span class="ui-icon ui-icon-document-b"></span><span class="text link">查看</span></span>
								<span class="itemOperate download"><span class="ui-icon ui-icon-arrowthickstop-1-s"></span><span class="text link">下载</span></span>
								<span class="toggle"><span class="ui-icon ui-icon-carat-1-sw" title="折叠|展开详细信息"></span></span>
							</span>
						</div>
					</div>
					<div class="detail low little">
						<div class="line">
							<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>
							<span class="text">李四 2012-01-01 08:00</span>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>