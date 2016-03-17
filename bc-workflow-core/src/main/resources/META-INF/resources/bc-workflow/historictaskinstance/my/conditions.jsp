<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ page import="cn.bc.web.ui.html.toolbar.*"%>
<form class="bc-conditionsForm draggable ui-widget-content ui-state-highlight">
	<ul class="conditions" style="min-width:19.3em;">
		<li class="condition">
			<div class="label">发起时间</div>
			<div class="value">
				<div class="bc-dateContainer">
					<input type="text" class="bc-date ui-widget-content" data-validate="date" style="width:9em;"
						data-condition='{"type":"startDate","ql":"t.start_time_>=?"}'>
					<ul class="inputIcons">
						<li class="selectCalendar inputIcon ui-icon ui-icon-calendar"></li>
						<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
					</ul>
				</div>～<div class="bc-dateContainer">
					<input type="text" class="bc-date ui-widget-content" data-validate="date" style="width:9em;"
						data-condition='{"type":"endDate","ql":"t.start_time_<=?"}'>
					<ul class="inputIcons">
						<li class="selectCalendar inputIcon ui-icon ui-icon-calendar"></li>
						<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
					</ul>
				</div>
			</div>
			<div class="clear"></div>
		</li>
		<li class="condition">
			<div class="label">完成时间</div>
			<div class="value">
				<div class="bc-dateContainer">
					<input type="text" class="bc-date ui-widget-content" data-validate="date" style="width:9em;"
						data-condition='{"type":"startDate","ql":"t.end_time_>=?"}'>
					<ul class="inputIcons">
						<li class="selectCalendar inputIcon ui-icon ui-icon-calendar"></li>
						<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
					</ul>
				</div>～<div class="bc-dateContainer">
					<input type="text" class="bc-date ui-widget-content" data-validate="date" style="width:9em;"
						data-condition='{"type":"endDate","ql":"t.end_time_<=?"}'>
					<ul class="inputIcons">
						<li class="selectCalendar inputIcon ui-icon ui-icon-calendar"></li>
						<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
					</ul>
				</div>
			</div>
			<div class="clear"></div>
		</li>
		<li class="condition">
			<div class="label">办理期限</div>
			<div class="value">
				<div class="bc-dateContainer">
					<input type="text" class="bc-date ui-widget-content" data-validate="date" style="width:9em;"
						data-condition='{"type":"startDate","ql":"t.due_date_>=?"}'>
					<ul class="inputIcons">
						<li class="selectCalendar inputIcon ui-icon ui-icon-calendar"></li>
						<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
					</ul>
				</div>～<div class="bc-dateContainer">
					<input type="text" class="bc-date ui-widget-content" data-validate="date" style="width:9em;"
						data-condition='{"type":"endDate","ql":"t.due_date_<=?"}'>
					<ul class="inputIcons">
						<li class="selectCalendar inputIcon ui-icon ui-icon-calendar"></li>
						<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
					</ul>
				</div>
			</div>
			<div class="clear"></div>
		</li>
		<li class="condition">
			<div class="label">所属流程</div>
			<div class="value">
				<input type="text" class="bc-select ui-widget-content" 
					data-maxHeight="150px"
					data-source='<s:property value="processList"/>'>
				<input type="hidden" data-condition='{"type":"string","ql":"pd.name_=?"}'>
				<ul class="inputIcons">
					<li class="bc-select inputIcon ui-icon ui-icon-triangle-1-s" title='<s:text name="title.click2select"/>'></li>
					<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
				</ul>
			</div>
		</li>
		<li class="condition">
			<div class="label">任务</div>
			<div class="value">
				<input type="text" class="bc-select ui-widget-content" 
					data-maxHeight="150px"
					data-source='<s:property value="taskList"/>'>
				<input type="hidden" data-condition='{"type":"string","ql":"t.name_=?"}'>
				<ul class="inputIcons">
					<li class="bc-select inputIcon ui-icon ui-icon-triangle-1-s" title='<s:text name="title.click2select"/>'></li>
					<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
				</ul>
			</div>
		</li>
		<li class="condition">
			<div class="value">
				<div class="bc-dateContainer">
					<div class="value radios" data-condition='{"type":"string","ql":"((t.end_time_ is not null and t.due_date_ is null) or t.end_time_ <= t.due_date_)"}' >
						<label><input type="radio" name="radioField1" ><span>按期完成</span></label>
					</div>
				</div>
				<div class="bc-dateContainer">
					<div class="value radios" data-condition='{"type":"string","ql":"t.end_time_ > t.due_date_"}' >
						<label><input type="radio" name="radioField1" ><span>过期办理</span></label>
					</div>
				</div>
			</div>
		</li>
		<li class="condition">
			<div class="label">流程状态</div>
			<div class="value">
				<div class="bc-dateContainer">
					<div class="value radios" data-condition="{'type':'string','ql':'e.suspension_state_ = \'1\''}" >
						<label><input type="radio" name="radioField2" ><span>流转中</span></label>
					</div>
				</div>
				<div class="bc-dateContainer">
					<div class="value radios" data-condition="{'type':'string','ql':'e.suspension_state_ = \'2\''}" >
						<label><input type="radio" name="radioField2" ><span>已暂停</span></label>
					</div>
				</div>
				<div class="bc-dateContainer">
					<div class="value radios" data-condition='{"type":"string","ql":"p.end_time_ is not null"}' >
						<label><input type="radio" name="radioField2" ><span>已结束</span></label>
					</div>
				</div>
			</div>
		</li>
	</ul>
</form>