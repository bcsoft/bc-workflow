<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<div class="bc-page" data-type='form' title='<s:property value="title"/>'
	data-saveUrl='<s:url value="/bc-workflow/flowattach/save" />'
	data-js='<s:url value="/bc-workflow/flowattach/form.js"/>,<s:url value="/bc/template/template.js"/>'
	data-initMethod='bc.flowattachForm.init'
	data-option='<s:property value="formPageOption"/>' style="overflow-y: auto;">
	<s:form name="flowAttachForm" theme="simple" >
		<table  cellspacing="2" cellpadding="0" style="width:500px;"  >
			<tbody>
				<tr class="widthMarker">
					<td style="min-width: 60px;width: 60px;"></td>
					<td >&nbsp;</td>
				</tr>
				<!-- 附件  -->
				<s:if test="e.type==1||e.type==3">
					<tr>
						<td class="label" style="min-width: 60px;width: 60px;">*<s:text name="flowattach.subject"/>:</td>
						<td class="value">
							<s:textfield name="e.subject" cssClass="ui-widget-content" data-validate="required"/>
						</td>
					</tr>
					<tr>
						<td class="label" style="min-width: 60px;width: 60px;">*<s:text name="flowattach.path"/>:</td>
						<td class="value"  >
							<div class="relative">
								<s:textfield name="e.path" cssClass="ui-widget-content" readonly="true" data-validate="required"/>
								<ul class="inputIcons" style="padding-right:8px">
									<li id="loadAttachFromTemplate" class="inputIcon ui-icon ui-icon-plus" title='点击从模板中添加' >
									<li id="upLoadFileId" class="inputIcon ui-icon ui-icon-arrowthickstop-1-n" style="position: relative;">
										<input type="file" class="auto uploadFile" id="uploadFile" name="uploadFile" title="点击上传文件"
											data-cfg='{"callback":"bc.flowattachForm.afterUploadfile","subdir":"workflow/attachment","source":":input[name=\"e.subject\"]","to":":input[name=\"e.path\"]","ptype":"FlowAttach","puid":"<s:property value="e.uid"/>"}'
											style="position: absolute;left: 0;top: 0;width: 100%;height: 100%;filter: alpha(opacity = 10);opacity: 0;cursor: pointer;">
									</li>
									<li class="downLoadFileId inputIcon ui-icon ui-icon-arrowthickstop-1-s" title='下载附件' >
									<li id="cleanFileId" class="clearSelect inputIcon ui-icon ui-icon-close" title='清除附件'/>	 
								</ul>
							</div>
						</td>
					</tr>
					<tr>
						<td class="topLabel" style="min-width: 60px;width: 60px;"><s:text name="flowattach.desc"/>:</td>		
						<td class="value">
							<s:textarea rows="4" name="e.desc"  cssClass="ui-widget-content noresize" />
						</td>		
					</tr>
					<tr id="formattedtr">
						<td class="label" style="min-width: 60px;width: 60px;"><s:text name="flowattach.formatted"/>:</td>		
						<td class="value">
							<s:radio name="e.formatted" list="#{'true':'是','false':'否'}" cssStyle="width:auto;"/>
						</td>	
					</tr>
					<tr>
						<td class="label" colspan="2" style="min-width: 60px;width: 60px;">
							<div class="formTopInfo">
								登记：<s:property value="e.author.name" />(<s:date name="e.fileDate" format="yyyy-MM-dd HH:mm:ss"/>)
								<s:if test="%{e.modifier != null}">
								，最后修改：<s:property value="e.modifier.name" />(<s:date name="e.modifiedDate" format="yyyy-MM-dd HH:mm:ss"/>)
								</s:if>
							</div>
						</td>		
					</tr>
				</s:if>
				<!-- 意见 -->
				<s:elseif test="e.type==2">
					<tr style="display:none;">
						<td class="value" style="min-width: 60px;" colspan="2">
								<s:textfield name="e.subject" cssClass="ui-widget-content" placeholder="在这里输入意见的简要描述" />
						</td>		
					</tr>
					<tr>
						<td class="value" style="min-width: 60px;" colspan="2">
								<s:textarea rows="7" name="e.desc" cssClass="ui-widget-content noresize" placeholder="在这里输入意见" />
						</td>		
					</tr>
				</s:elseif>
			</tbody>
		</table>
		<s:hidden name="e.id" />
		<s:hidden name="e.uid" />
		<s:hidden name="e.common" />
		<s:hidden name="e.type" />
		<s:hidden name="e.pid" />
		<s:hidden name="e.tid" />
		<s:hidden name="params"/>
		<s:if test="e.type==1">
			<s:hidden name="e.ext" />
			<s:hidden name="e.size" />
		</s:if>
		<s:hidden name="e.author.id" />
		<input type="hidden" name="e.fileDate" value='<s:date format="yyyy-MM-dd HH:mm:ss" name="e.fileDate" />'/>
	</s:form>
</div>