<%@ page contentType="text/html;charset=UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<div title='<s:text name="deploy.title"/> - v<s:property value="e.version" />' data-type='form' class="bc-page"
	data-saveUrl='<s:url value="/bc-workflow/deploy/save" />'
	data-js='<s:url value="/bc-workflow/deploy/form.js" />,<s:url value="/bc/identity/identity.js" />'
	data-initMethod='bc.deployForm.init'
	data-option='<s:property value="formPageOption"/>' style="overflow-y:auto;">
	<s:form name="deployForm" theme="simple" >
			<table class="formFields"   cellspacing="2" cellpadding="0" style="width:800px;">
				<tbody>
					<tr class="widthMarker">
						<td>&nbsp;</td>
						<td style="width: 300px;"></td>
						<td style="width: 80px;"></td>
						<td style="width: 300px;"></td>
					</tr>
					<tr>
						<!-- 类型 -->
						<td class="label">*<s:text name="deploy.type"/>:</td>
						<td class="value">
							<s:select name="e.type" list="#{0:getText('deploy.type.xml'),1:getText('deploy.type.bar')}" listKey="key" listValue="value" data-validate="required" 
									cssClass="ui-widget-content"></s:select>
						</td>
						<!-- 所属分类-->
						<td class="label">*<s:text name="deploy.category"/>:</td>
						<td class="value"><s:textfield name="e.category" cssClass="ui-widget-content" data-validate="required" /></td>
					</tr>
					<tr>
						<!-- 标题  -->
						<td class="label">*<s:text name="deploy.tfsubject"/>:</td>
						<td class="value">
							<s:textfield name="e.subject" cssClass="ui-widget-content" data-validate="required"/>
						</td>
						<!-- 编码-->
						<td class="label">*<s:text name="deploy.code"/>:</td>
						<td class="value"><s:textfield name="e.code" cssClass="ui-widget-content" data-validate="required" /></td>
					</tr>
					<tr>
						<td class="label">*<s:text name="deploy.tfpath"/>:</td>
						<td class="value"  >
							<div class="relative">
								<s:textfield name="e.path" cssClass="ui-widget-content" readonly="true" data-validate="required"/>
								<ul class="inputIcons">
									<li id="upLoadFileId" class="inputIcon ui-icon ui-icon-arrowstop-1-n" style="position: relative;">
										<input type="file" class="auto uploadFile" id="uploadFile" name="uploadFile" title="点击上传文件"
											data-cfg='{"callback":"bc.deployForm.afterUploadfile","subdir":"workflow/deploy","source":":input[name=\"e.code\"]","to":":input[name=\"e.path\"]","ptype":"deploy","puid":"<s:property value="e.uid"/>"}'
											style="position: absolute;left: 0;top: 0;width: 100%;height: 100%;filter: alpha(opacity = 10);opacity: 0;cursor: pointer;">
									</li>
									<li id="downLoadFileId" class="downLoadFileId inputIcon ui-icon ui-icon-arrowstop-1-s" title='<s:text name="deploy.download"/>' >
									<li id="cleanFileId" class="clearSelect inputIcon ui-icon ui-icon-close" title='清除此列信息'></li>				 
								</ul>
							</div>
						</td>
						<!-- 排序号-->
						<td class="label"><s:text name="deploy.order"/>:</td>
						<td class="value"><s:textfield name="e.orderNo" cssClass="ui-widget-content" /></td>
					</tr>
					<tr class="tplFile">
						<!-- 原始文件名  -->
						<td class="label">*<s:text name="deploy.source"/>:</td>
						<td class="value">
							<s:textfield name="e.source" cssClass="ui-widget-content" readonly="true" data-validate="required"/>
						</td>
						<td class="label"></td>
						<td class="value"></td>
					</tr>
					<tr>
						<!-- 使用人 -->
						<td class="topLabel"><s:text name="deploy.user"/>:</td>
						<td class="value" colspan="3">
							<div id="assignUsers" style="position:relative;margin: 0;padding: 1px 0;min-height:21px;margin: 0;font-weight: normal;width: 100%;" 
								class="ui-widget-content borderBox" 
								data-removeTitle='<s:text name="title.click2remove"/>'>
								<ul class="inputIcons">
									 	<li class="inputIcon ui-icon ui-icon-person" title='<s:text name="group.title.click2addUsers"/>' id="addUsers">
									 	<li class="inputIcon ui-icon ui-icon-contact" title='<s:text name="actor.title.click2addGroups"/>' id="addGroups">
									 	<li class="inputIcon ui-icon ui-icon-home" title='<s:text name="deploy.title.addUnitOrDepartment"/>' id="addUnitOrDepartments">
								</ul>
								<s:if test="%{ownedUsers != null && !ownedUsers.isEmpty()}">
									<s:if test="%{!isReadOnly}">
									<ul class="horizontal deployUserUl" style="padding: 0 50px 0 0;">
									<s:iterator value="ownedUsers">
									<li class="horizontal deployUserLi" style="position: relative;margin:0 2px;float: left;padding: 0;"
										data-id=<s:property value="['id']"/>>
										<span class="text" >
											<s:if test="%{type==3}">
												<s:property value="['pname']+'/'+['name']" />
											</s:if>
											<s:else>
												<s:property value="['name']" />
											</s:else>
										</span>
										<span class="click2remove verticalMiddle ui-icon ui-icon-close" style="margin: -8px -2px;" title='<s:text name="title.click2remove"/>'></span>
									</li>
									</s:iterator>
									</ul>
									</s:if>
									<s:else>
										<s:iterator value="ownedUsers" status="of">
											<span class="text" style="display:inline-block;">
												<s:if test="%{type==3}"><s:property value="['pname']+'/'+['name']" /></s:if><s:else><s:property value="['name']" /></s:else><s:if test="#of.last==false">、</s:if>  
											</span>
										</s:iterator>
									</s:else>
								</s:if>	
							</div>
						</td>
					</tr>
					<tr>
						<!-- 备注 -->
						<td class="topLabel"><s:text name="deploy.desc"/>:</td>
						<td class="value" colspan="3"><textarea name="e.desc" class="autoHeight ui-widget-content">${e.desc}</textarea></td>
					</tr>
				</tbody>
			</table>
			<!-- 资源配置 -->
			<div class="ui-widget-content" style="border-width:1px 0 0 0;margin-bottom:8px;width: 100%;">
				<div class="ui-widget-header" style="position:relative;border-width: 0;padding: 0.25em;">
					<span class="text">资源配置:</span>
					<ul class="inputIcons">
						<li id="addLine" class="inputIcon ui-icon ui-icon-circle-plus"
							title='添加一个流程资源'></li>
						<li id="deleteLine" class="inputIcon ui-icon ui-icon-circle-close"
							title='删除流程资源'></li>
					</ul>
				</div>
				<div class="bc-grid header" style="overflow-x:auto;">
					<table class="table" id="resourceTables" cellspacing="0" cellpadding="0" style="width: 100%;">
						<tr class="ui-state-default header row">
							<td class="first" style="width: 8px;">&nbsp;</td>
							<td class="middle" style="width: 10em;">类型</td>
							<td class="middle" style="width: 10em;">资源编码</td>
							<td class="middle" style="width: 10em;">名称</td>
							<td class="middle" style="width: 10em;">文件路径</td>
							<td class="middle" style="width: 3em;">格式化</td>
							<td class="last" style="min-width: 15em;">参数</td>
						</tr>
						<s:iterator var="r" value="e.resources">
						<tr class="ui-widget-content row" data-id='<s:property value="id"/>'>
							<td class="id first" style="padding:0;text-align:left;"><span class="ui-icon"></span>
								<!-- UID -->
								<input id="uid" name="uid" type="hidden" value='<s:property value="uid"/>'/>
								<!-- 文件大小 -->
								<input id="size" name="size" type="hidden" name="size"
									data-validate="required" value='<s:property value="size"/>'/>
								<!-- 原始文件名 -->
								<input id="source" name="source" type="hidden" name="source"
									data-validate="required" value='<s:property value="source"/>'/>
							</td>
							<!-- 类型 -->
							<td class="middle" style="padding:0;text-align:left;">
								<s:select value="%{templateType.id+',' + templateType.extension}" name="templateType.id" list="typeList" listKey="key" listValue="value" data-validate="required" 
										cssClass="ui-widget-content borderBox onmouseovertips" cssStyle="width:100%;height:100%;border:none;margin:0;padding:0 10px 0 2px"></s:select>
							</td>
							<!-- 资源编码 -->
							<td class="middle" style="padding:0;text-align:left;">
								<input id="code" name="code" style="width:100%;height:100%;border:none;margin:0;padding:0 0 0 2px;" type="text" class="ui-widget-content borderBox onmouseovertips" 
									data-validate="required" value='<s:property value="code"/>'/>
							</td>
							<!-- 名称 -->
							<td class="middle" style="padding:0;text-align:left;">
								<input id="subject" name="subject" style="width:100%;height:100%;border:none;margin:0;padding:0 0 0 2px;" type="text" class="ui-widget-content borderBox onmouseovertips" 
									data-validate="required" value='<s:property value="subject"/>'/>
							</td>
							<!-- 文件路径 -->
							<td class="middle" style="padding:0;text-align:left;">
								<div class="relative">
									<input id="path" name="path" style="width:100%;height:100%;border:none;margin:0;padding:0 0 0 2px;width: 6em;" type="text" class="ui-widget-content bc-wf-deploy-path borderBox onmouseovertips" 
										data-validate="required" value='<s:property value="path"/>' readonly="readonly" />
									<ul class="inputIcons" style="padding-right:2px">
										<li id="upLoadFileId" class="inputIcon ui-icon ui-icon-arrowstop-1-n" style="position: relative;">
											<input type="file" class="auto uploadFile" id="uploadFile" name="uploadFile" title="点击上传文件"
												data-cfg='{"callback":"bc.deployForm.afterUploadResourcefile","subdir":"workflow/deploy/resource","to":":input[name=\"path\"]","ptype":"deployResource","puid":"<s:property value="uid"/>"}'
												style="position: absolute;left: 0;top: 0;width: 100%;height: 100%;filter: alpha(opacity = 10);opacity: 0;cursor: pointer;">
										</li>
										<li class="downLoadFileId inputIcon ui-icon ui-icon-arrowstop-1-s" title='流程资源下载' >
										<li class="clearSelect inputIcon ui-icon ui-icon-close" data-cfg='{"callback":"bc.deployForm.clearTrContent"}'  title='<s:text name="title.click2clear"/>'></li>				 
										<!-- 
										<li class="formatTest inputIcon ui-icon ui-icon ui-icon-lightbulb" title='点击格式化测试'></li>				 
										 -->
									</ul>
								</div>
							</td>
							<!-- 格式化 -->
							<td class="middle" style="padding:0;text-align:left;">
								<s:checkbox name="formatted" theme="simple" />
							</td>
							<!-- 参数 -->
							<td class="last" style="padding:0;text-align:left;">
								<div class="templateParams" style="position:relative;margin: 0;padding: 1px 0;min-height:19px;margin: 0;font-weight: normal;width: 98%;"
									data-removeTitle='<s:text name="title.click2remove"/>'>
									<s:if test="%{formatted == true}">
										<ul class="inputIcons" style="top:10px">
											 <li class="addParam inputIcon ui-icon ui-icon-circle-plus" title="点击添加参数">
										</ul>
									</s:if>
									<s:if test="%{params != null && !params.isEmpty()}">
										<ul class="horizontal templateParamUl" style="padding: 0 50px 0 0;">
											<s:iterator value="params" var="p">
												<li class="horizontal templateParamLi" style="position: relative;margin:0 2px;float: left;padding: 0;"
													data-id=<s:property value="['id']"/>>
													<span class="text" ><a href="#" style="color: #1F1F1F;"><s:property value="['name']" /></a></span>
													<s:if test="%{!isReadOnly}">
														<span class="click2remove verticalMiddle ui-icon ui-icon-close" style="margin: -8px -2px;" title='<s:text name="title.click2remove"/>'></span>
													</s:if>
												</li>
											</s:iterator>
										</ul>
									</s:if>	
								</div>	
							</td>
						</tr>
						</s:iterator>
					</table>
				</div>
				<p class="formComment">备注：如需要配置流程图，请添加一个“png”格式的图片资源并将资源编码设置和流程编码一致</p>
			</div>
			<div class="formFields">
				<table class="formFields"  cellspacing="2" cellpadding="0">
					<tr>
						<td class="label" colspan="4">
							<div class="formTopInfo">
								<div>状态：<s:property value="%{statusesValue[e.status]}" /></div>
								<div>创建人：<s:property value="e.author.name" /> <s:date name="e.fileDate" format="yyyy-MM-dd HH:mm:ss"/></div>
								<s:if test="%{e.modifier != null}">
								<div>最后修改：<s:property value="e.modifier.name" /> <s:date name="e.modifiedDate" format="yyyy-MM-dd HH:mm:ss"/></div>
								</s:if>
								<s:if test="%{e.deployer != null}">
								<div>最后发布/取消人：<s:property value="e.deployer.name" /> <s:date name="e.deployDate" format="yyyy-MM-dd HH:mm:ss"/></div>
								</s:if>
							</div>
						</td>
					</tr>	
				</table>
			</div>
		<s:hidden name="e.id" />
		<s:hidden name="e.deploymentId" />
		<s:hidden name="e.version" />
		<s:hidden name="e.uid" />
		<s:hidden name="e.status" />
		<s:hidden name="e.author.id" />
		<!-- 流程部署用户配置信息 -->
		<s:hidden name="assignUserIds" />
		<!-- 流程部署资源信息 -->
		<s:hidden name="resources"/>
		<input type="hidden" id="flag"/>
		<input type="hidden" name="e.fileDate" value='<s:date format="yyyy-MM-dd HH:mm:ss" name="e.fileDate" />'/>
		<s:hidden name="e.deployer.id" />
		<input type="hidden" name="e.deployDate" value='<s:date format="yyyy-MM-dd HH:mm:ss" name="e.deployDate" />'/>
	</s:form>
</div>