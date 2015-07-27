<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ page import="cn.bc.web.ui.html.toolbar.*"%>
<form class="bc-conditionsForm draggable ui-widget-content ui-state-highlight">
	<ul class="conditions" style="min-width:17.3em;">
		<li class="condition">
			<div class="label">类型</div>
			<div class="value checkboxes" data-condition='{"type":"long","key":"d.type_"}' >
				<ul class="conditions">
					<li class="condition">
						<label><input type="checkbox" name="checkboxField1" value="0"><span>Activiti BPMN 2.0 XML流程图文件</span></label>
					</li>
					<li class="condition">
						<label><input type="checkbox" name="checkboxField1" value="1"><span>Activiti BPMN 2.0  流程打包文件</span></label>
					</li>
				</ul>
			</div>
		</li>
		<li class="condition">
			<div class="label">所属分类</div>
			<div class="value">
				<input type="text" class="bc-select ui-widget-content" 
					data-maxHeight="150px"
					data-source='<s:property value="categorys"/>'>
				<input type="hidden" data-condition='{"type":"string","ql":"d.category=?"}'>
				<ul class="inputIcons">
					<li class="bc-select inputIcon ui-icon ui-icon-triangle-1-s" title='<s:text name="title.click2select"/>'></li>
					<li class="clearSelect inputIcon ui-icon ui-icon-close" title='<s:text name="title.click2clear"/>'></li>
				</ul>
			</div>
		</li>
	</ul>
</form>