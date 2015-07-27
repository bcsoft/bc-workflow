bc.namespace("bc.deploy");
bc.deployForm = {
	init : function(option,readonly) {
		var $form = $(this);
		
		//动态绑定事件  鼠标提示内容
		$form.find("#resourceTables").delegate(".onmouseovertips","mouseover",function(){
			$(this).attr('title',$(this).val());
		});
		
		if(readonly) return;
		
		var liTpl = '<li class="horizontal deployUserLi ui-widget-content ui-corner-all ui-state-highlight" data-id="{0}"'+
		'style="position: relative;margin:0 2px;float: left;padding: 0;border-width: 0;">'+
		'<span class="text">{1}</span>'+
		'<span class="click2remove verticalMiddle ui-icon ui-icon-close" style="margin: -8px -2px;" title={2}></span></li>';
		var ulTpl = '<ul class="horizontal deployUserUl" style="padding: 0 45px 0 0;"></ul>';
		var title = $form.find("#assignUsers").attr("data-removeTitle");
		
		//绑定清除按钮事件
		$form.find("#cleanFileId").click(function(){
			$form.find(":input[name='e.subject']").val('');
			$form.find(":input[name='e.source']").val('');
		});
		
		//绑定下载按钮事件
		$form.find("#downLoadFileId").click(function(){
			var subject=$form.find(":input[name='e.subject']").val();
			var path=$form.find(":input[name='e.path']").val();
			var id=$form.find(":input[name='e.id']").val();
			if(id==""){
				bc.msg.slide('请先保存流程部署信息！');
				return;
			}
			
			if(subject.length <= 0 && path.length){
				bc.msg.alert('下载失败! 流程名称,附件不能为空!');
				return;
			}
			
			var n =  subject;// 获取文件名
			var f = "workflow/deploy/" + path;// 获取附件相对路径			
			// 下载文件
			bc.file.download({f: f, n: n,ptype:"deploy",puid:$form.find(":input[name='e.uid']").val()});
		});
		
		
		//绑定添加用户的按钮事件处理
		$form.find("#addUsers").click(function(){
			var $ul = $form.find("#assignUsers .deployUserUl");
			var $lis = $ul.find("li");
			var selecteds="";
			$lis.each(function(i){
				selecteds+=(i > 0 ? "," : "") + ($(this).attr("data-id"));//
			});
			bc.identity.selectUser({
				multiple: true,//可多选
				history: false,
				selecteds: selecteds,
				onOk: function(users){
					$.each(users,function(i,user){
						if($lis.filter("[data-id='" + user.id + "']").size() > 0){//已存在
							logger.info("duplicate select: id=" + user.id + ",name=" + user.name);
						}else{//新添加的
							if(!$ul.size()){//先创建ul元素
								$ul = $(ulTpl).appendTo($form.find("#assignUsers"));
							}
							$(liTpl.format(user.id,user.name,title))
							.appendTo($ul).find("span.click2remove")
							.click(function(){
								$(this).parent().remove();
							});
						}
					});
				}
			});
		});
		
		//绑定添加岗位的按钮事件处理
		$form.find("#addGroups").click(function(){
			var $ul = $form.find("#assignUsers .deployUserUl");
			var $lis = $ul.find("li");
			var selecteds = "";
			$lis.each(function(i){
				selecteds += (i > 0 ? "," : "") + $(this).attr("data-id");//已选择的id
			});
			bc.identity.selectGroup({
				multiple: true,
				selecteds: selecteds,
				onOk: function(groups){
					//添加当前没有分派的岗位
					$.each(groups,function(i,group){
						if($lis.filter("[data-id='" + group.id + "']").size() > 0){//已存在
							logger.info("duplicate select: id=" + group.id + ",name=" + group.name);
						}else{//新添加的
							if(!$ul.size()){//先创建ul元素
								$ul = $(ulTpl).appendTo($form.find("#assignUsers"));
							}
							$(liTpl.format(group.id,group.name,title))
							.appendTo($ul).find("span.click2remove")
							.click(function(){
								$(this).parent().remove();
							});
						}
					});
				}
			});
		});
		
		//绑定添加单位或部门的按钮事件处理
		$form.find("#addUnitOrDepartments").click(function(){
			var $ul = $form.find("#assignUsers .deployUserUl");
			var $lis = $ul.find("li");
			var selecteds = "";
			$lis.each(function(i){
				selecteds += (i > 0 ? "," : "") + $(this).attr("data-id");//已选择的id
			});
			bc.identity.selectUnitOrDepartment({
				multiple: true,
				selecteds: selecteds,
				onOk: function(groups){
					//添加当前没有分派的岗位
					$.each(groups,function(i,group){
						if($lis.filter("[data-id='" + group.id + "']").size() > 0){//已存在
							logger.info("duplicate select: id=" + group.id + ",name=" + group.name);
						}else{//新添加的
							if(!$ul.size()){//先创建ul元素
								$ul = $(ulTpl).appendTo($form.find("#assignUsers"));
							}
							$(liTpl.format(group.id,group.name,title))
							.appendTo($ul).find("span.click2remove")
							.click(function(){
								$(this).parent().remove();
							});
						}
					});
				}
			});
		});
		
		//绑定删除角色、用户的按钮事件处理
		$form.find("span.click2remove").click(function(){
			$(this).parent().remove();
		});
		
		//------------添加行-------------------
		var tableEl=$form.find("#resourceTables")[0];
		
		$form.find("#addLine").click(function() {
			var url=bc.root + "/bc-workflow/deploy/setInputUid";
			$.ajax({
				url:url,
				dataType: "json",
				success:function(json){
					//插入行
					var newRow=tableEl.insertRow(tableEl.rows.length);
					newRow.setAttribute("class","ui-widget-content row");
					//插入列
					var cell=newRow.insertCell(0);
					cell.style.padding="0";
					cell.style.textAlign="left";
					cell.setAttribute("class","id first");
					cell.innerHTML='<span class="ui-icon"></span>' //空白头列
					+'<input id="uid" name="uid" type="hidden" value='+json.uid+' />' //uid
					+'<input id="size" name="size" type="hidden" name="size" data-validate="required"/>' //文件大小
					+'<input id="source" name="source" type="hidden" name="source" data-validate="required"/>'; //原始文件名

					$.ajax({
						url:bc.root + "/bc-workflow/deploy/loadTemplateTypeList",
						dataType: "json",
						success:function(jsonArray){
							//插入模板类型
							cell=newRow.insertCell(1);
							cell.style.padding="0";
							cell.style.textAlign="left";
							cell.setAttribute("class","middle");
							var typeRow='<select name="r.templateType.id" class="ui-widget-content borderBox onmouseovertips" style="width:100%;height:100%;border:none;margin:0;padding:0 10px 0 2px"'
								typeRow+='data-validate="required">';
							typeRow+='<option value=""></option>';
							//logger.info($.toJSON(jsonArray));
							for(var i=0;i<jsonArray.length;i++){
								typeRow+='<option value="';
								typeRow+=jsonArray[i].key;
								typeRow+='">';
								typeRow+=jsonArray[i].value;
								typeRow+='</option>';
							}
							typeRow+='</select>';
							cell.innerHTML=typeRow;
						}
					});
					
					//插入编码
					cell=newRow.insertCell(1);
					cell.style.padding="0";
					cell.style.textAlign="left";
					cell.setAttribute("class","middle");
					cell.innerHTML='<input id="code" name="code" style="width:100%;height:100%;border:none;margin:0;padding:0 0 0 2px;"'
						+' type="text" class="ui-widget-content borderBox onmouseovertips" data-validate="required" />';
					
					//插入名称
					cell=newRow.insertCell(2);
					cell.style.padding="0";
					cell.style.textAlign="left";
					cell.setAttribute("class","middle");
					cell.innerHTML='<input id="subject" name="subject" style="width:100%;height:100%;border:none;margin:0;padding:0 0 0 2px;"'
						+'type="text" class="ui-widget-content borderBox onmouseovertips" data-validate="required" />';
					
					//插入文件路径
					cell=newRow.insertCell(3);
					cell.style.padding="0";
					cell.style.textAlign="left";
					cell.setAttribute("class","middle");
					cell.innerHTML='<div class="relative">'
									+'<input id="path" name="path" style="width:100%;height:100%;border:none;margin:0;padding:0 0 0 2px;width: 6em;" type="text"'
									+'class="ui-widget-content bc-wf-deploy-path borderBox onmouseovertips" data-validate="required" readonly="readonly" />'
									+'<ul class="inputIcons" style="padding-right:2px">'
										+'<li id="upLoadFileId" class="inputIcon ui-icon ui-icon-arrowstop-1-n" style="position: relative;">'
											+'<input type="file" class="auto uploadFile" id="uploadFile" name="uploadFile" title="点击上传文件"'
											+'data-cfg=\'{"callback":"bc.deployForm.afterUploadResourcefile","subdir":"workflow/deploy/resource","to":":input[name=\\"path\\"]","ptype":"deployResource","puid":"'+json.uid+'"}\''
											+'style="position: absolute;left: 0;top: 0;width: 100%;height: 100%;filter: alpha(opacity = 10);opacity: 0;cursor: pointer;"/>'
										+'</li>'
										+'<li class="downLoadFileId inputIcon ui-icon ui-icon-arrowstop-1-s" title="流程资源下载"></li>'
										+'<li class="clearSelect inputIcon ui-icon ui-icon-close" data-cfg=\'{"callback":"bc.deployForm.clearTrContent"}\' title="清除此列信息"></li>'
										//+'<li class="formatTest inputIcon ui-icon ui-icon ui-icon-lightbulb" title=\'点击格式化测试\'></li>'
									+'</ul>'
									+'<div>';
					
					//插入格式化
					cell=newRow.insertCell(4);
					cell.style.padding="0";
					cell.style.textAlign="left";
					cell.setAttribute("class","middle");
					cell.innerHTML='<input type="checkbox" name="formatted" value="false" />'
						
					//插入参数
					cell=newRow.insertCell(5);
					cell.style.padding="0";
					cell.style.textAlign="left";
					cell.setAttribute("class","last");
					cell.innerHTML='<div class="templateParams" style="position:relative;margin: 0;padding: 1px 0;min-height:19px;margin: 0;font-weight: normal;width: 98%;" data-removeTitle=\'移除参数\'>'
//										+'<ul class="inputIcons" style="top:10px">'
//											+'<li class="addParam inputIcon ui-icon ui-icon-circle-plus" title="点击添加参数">'
//										+'</ul>'
								  +'</div>'
				}
			});
		});
		
		//点击选中行
		$form.find("#resourceTables").delegate("tr.ui-widget-content.row>td.id","click",function(){
			$(this).parent().toggleClass("ui-state-highlight").find("td:eq(0)>span.ui-icon").toggleClass("ui-icon-check");
		});
		$form.find("#resourceTables").delegate("tr.ui-widget-content.row input","focus",function(){
			$(this).closest("tr.row").removeClass("ui-state-highlight").find("td:eq(0)>span.ui-icon").removeClass("ui-icon-check");
		});
		//删除表中选中的
		$form.find("#deleteLine").click(function() {
			var $trs = $form.find("#resourceTables tr.ui-state-highlight");
			if($trs.length == 0){
				bc.msg.slide("请先选择要删除的资源！");
				return;
			}
			bc.msg.confirm("确定要删除选定的 <b>"+$trs.length+"</b>个资源吗？",function(){
				for(var i=0;i<$trs.length;i++){
					$($trs[i]).remove();
				}
			});
			
		});
		
		//动态绑定事件  资源列表的下载按钮
		$form.find("#resourceTables").delegate(".downLoadFileId","click",function(){
			//向上找到tr父级元素
			var $tr= $(this).closest("tr");
			
			var subject=$tr.find(":input[name='subject']").val();
			var path=$tr.find(":input[name='path']").val();
			var id=$form.find(":input[name='e.id']").val();
			if(id==""){
				bc.msg.slide('请先保存流程部署信息！');
				return;
			}
			
			if(!bc.validator.validate($tr)) return;
			
			var n =  subject;// 获取文件名
			var f = "workflow/deploy/resource/" + path;// 获取附件相对路径			
			// 下载文件
			bc.file.download({f: f, n: n,ptype:"deployResource",puid:$tr.find(":input[name='uid']").val()});

		});
		
		//动态绑定事件  资源列表的格式化测试按钮
		$form.find("#resourceTables").delegate(".formatTest","click",function(){
			//向上找到tr父级元素
			var $tr= $(this).closest("tr");
		});
		
		//动态绑定事件  资源列表的是否格式化按钮控制添加参数按钮
		$form.find("#resourceTables").delegate("input[name^='formatted']","change",function(){
			var $div = $(this).closest('td').next('.last').find('.templateParams');
			if(this.checked){
				var str = '<ul class="inputIcons" style="top:10px">'
					  	   +'<li class="addParam inputIcon ui-icon ui-icon-circle-plus" title="点击添加参数">'
					     +'</ul>'
					     + $div.html() ;
				$div.html(str);
			}else{
				$div.html('');
			}
			
		});
		
		var liTpl2 = '<li class="horizontal templateParamLi ui-widget-content ui-corner-all ui-state-highlight" data-id="{0}"'+
		'style="position: relative;margin:0 2px;float: left;padding: 0;border-width: 0;">'+
		'<span class="text"><a href="#">{1}</a></span>'+
		'<span class="click2remove verticalMiddle ui-icon ui-icon-close" style="margin: -8px -2px;" title={2}></span></li>';
		var ulTpl2 = '<ul class="horizontal templateParamUl" style="padding: 0 45px 0 0;"></ul>';
		var title2 = "移除参数";
		
		//动态绑定事件  添加参数按钮
		$form.find("#resourceTables").delegate(".addParam","click",function(){
			//向上找到tr父级元素
			var $div = $(this).closest('div');
			var $ul = $div.find(".templateParamUl");
			var $lis = $ul.find("li");
			var selecteds="";
			$lis.each(function(i){selecteds+=(i > 0 ? "," : "") + ($(this).attr("data-id"));});
			bc.page.newWin({
				url: bc.root + "/bc/selectTemplateParam/list",
				multiple: true,
				title:'选择模板参数',
				name: '选择模板参数',
				mid: 'selectTemplateParams',
				afterClose: function(params){
					$.each(params,function(i,param){
						if($lis.filter("[data-id='" + param.id + "']").size() > 0){//已存在
							logger.info("duplicate select: id=" + param.id + ",name=" + param.name);
						}else{//新添加的
							if(!$ul.size())//先创建ul元素
								$ul = $(ulTpl2).appendTo($div);
							
							var $liObj=$(liTpl2.format(param.id,param.name,title2)).appendTo($ul);
							
							//绑定查看事件
							$liObj.find("span.text").click(function(){
								bc.page.newWin({
									url: bc.root + "/bc/templateParam/edit?id="+param.id,
									name: "模板参数",
									mid:  "templateParam"+param.id
								})
							});
							
							//绑定删除事件
							$liObj.find("span.click2remove")
							.click(function(){
								$(this).parent().remove();
							});
						}
					});
				}
			});
		});
		
		//动态绑定事件  查看参数按钮
		var $objs = $form.find("#resourceTables").find('.horizontal').children('span.text');
		//var $objs = $form.find("#resourceTables>.horizontal>span.text");
		$.each($objs,function(i,obj){
			//绑定查看
			$(obj).click(function(){
				bc.page.newWin({
					url: bc.root + "/bc/templateParam/edit?id="+$(obj).parent().attr('data-id'),
					name: "模板参数",
					mid:  "templateParam"+$(obj).parent().attr('data-id')
				})
			});
		});

		//绑定删除参数按钮事件处理
		$form.find("#resourceTables").find("span.click2remove").click(function(){
			$(this).parent().remove();
		});
		
//		//动态绑定事件  资源列表的清除按钮
//		$form.find("#resourceTables").delegate(".clearSelect","click",function(){;
//			//向上找到tr父级元素
//			alert($(this).parent().html());
//			return false;
//			var $tr= $(this).closest("tr");
//			$tr.find(":input[name='path']").val('');
//			$tr.find(":input[name='subject']").val('');
//			$tr.find(":input[name='code']").val('');
//			$tr.find(":input[name='uid']").val('');
//			$tr.find(":input[name='type']").val('');
//			$tr.find(":input[name='size']").val('');
//			$tr.find(":input[name='source']").val('');
//		});

	},
	
	/** 文件上传完毕后 */
	afterUploadfile : function(json){
		logger.info($.toJSON(json));
		if(json.success){
			var $page = this.closest(".bc-page");
			var lastIndex = json.source.lastIndexOf(".");
			var filename = lastIndex != -1 ? json.source.substring(0,lastIndex) : json.source;
			$page.find(':input[name="e.code"]').val(filename); 		// 编码
			$page.find(':input[name="e.path"]').val(json.to); 		// 路径
			$page.find(':input[name="e.source"]').val(json.source); // 原始文件名
			var $subject = $page.find(':input[name="e.subject"]');
			if($subject.val().length == 0) $subject.val(filename); 	// 名称
		}else{
			bc.msg.alert(json.msg);
		}
	},
	
	/** 流程资源文件上传完毕后 */
	afterUploadResourcefile : function(json){
		var $tr = $(this).closest(".ui-widget-content");
		logger.info($.toJSON(json));
		if(json.success){
			var $page = this.closest(".bc-page");
			var lastIndex = json.source.lastIndexOf(".");
			var extName = lastIndex != -1 ? json.source.substring(lastIndex+1,lastIndex.length) : null;
			if(extName != null && extName.length > 0){
				var $select= $($tr.find("td>select")[0]);
				$select.children().each(function(){
					var typeValue = this.value.split(",")[1];
					if(typeValue == extName){
						this.selected = true;
					}
				});
			}else{
				bc.msg.alert("上存错误!您上存的文件必须带后缀格式. ");
				$tr.find(':input[name="uid"]').val(''); 		// uid
				$tr.find(':input[name="code"]').val(''); 		// 编码
				$tr.find(':input[name="path"]').val(''); 		// 路径
				$tr.find(':input[name="source"]').val(''); // 原始文件名
				$tr.find(':input[name="subject"]').val(''); // 标题
				$tr.find(':input[name="size"]').val(''); // 文件大小
				$subject.val('');
			}
			var filename = lastIndex != -1 ? json.source.substring(0,lastIndex) : json.source;
			$tr.find(':input[name="code"]').val(filename); 		// 编码
			$tr.find(':input[name="path"]').val(json.to); 		// 路径
			$tr.find(':input[name="source"]').val(json.source); // 原始文件名
			$tr.find(':input[name="size"]').val(json.size); // 文件大小
			var $subject = $tr.find(':input[name="subject"]');
			if($subject.val().length == 0){
				$subject.val(filename); 	// 名称
			}
		}else{
			bc.msg.alert(json.msg);
		}
	},
	
	clearTrContent : function(){
		var $tr= $(this).closest("tr");
		$tr.find(":input[name='path']").val('');
		$tr.find(":input[name='subject']").val('');
		$tr.find(":input[name='code']").val('');
		$tr.find(":input[name='uid']").val('');
		$tr.find(":input[name='type']").val('');
		$tr.find(":input[name='size']").val('');
		$tr.find(":input[name='source']").val('');
	},
	
	/*============================  按钮操作  ============================*/
	
	/**
	 * 查看流程图
	 */
	showDiagram : function(){
		var $form = $(this);
		var did = $form.find(":input[name='e.deploymentId']").val();
		var id = $form.find(":input[name='e.id']").val();
		if(did.length == 0){
			bc.msg.alert("请先发布流程！");
			return;
		}
		window.open(bc.root + "/bc-workflow/deploy/diagram?id=" + id,"_blank");
	},
	
	/** 维护处理 */
	doMaintenance : function() {
		var $page = $(this);
		// 关闭当前窗口
		bc.msg.confirm("警告:若此修改需要重新发布请选择升级版本!! </br>确定维护此流程部署吗？",function(){
			$page.dialog("close");
			// 重新打开可编辑表单
			bc.page.newWin({
				name: "维护" + $page.find(":input[name='e.subject']").val() + "的流程部署",
				title: "维护" + $page.find(":input[name='e.subject']").val() + "的流程部署",
				mid: "deploy" + $page.find(":input[name='e.id']").val(),
				url: bc.root + "/bc-workflow/deploy/edit",
				data: {id: $page.find(":input[name='e.id']").val(),isDoMaintenance: true},
				afterClose: function(status){
					if(status) bc.grid.reloadData($page);
				}
			});
		});
		
	},
	
	doLevelUp : function() {
		var $page = $(this);
		// 关闭当前窗口
		bc.msg.confirm("确定升级此流程部署吗？",function(){
			$page.dialog("close");
			// 重新打开可编辑表单
			bc.page.newWin({
				name: "对" + $page.find(":input[name='e.subject']").val() + "的流程部署升级",
				mid: "contract4Labour" + $page.find(":input[name='e.id']").val(),
				url: bc.root + "/bc-workflow/deploy/create",
				data: {id: $page.find(":input[name='e.id']").val(),isDoLevelUp: true},
				afterClose: function(status){
					if(status) bc.grid.reloadData($page);
				}
			});
		});
	},
	
	/**
	 * 保存
	 */
	save : function(){
		var $form = $(this);
		
		//验证表单
		if(!bc.validator.validate($form)) return;
		
		//过滤当前code相同的资源
		var codes = '';
		var i=0;
		var $ary = $form.find('#resourceTables :input[name="code"]');
		var arySize = $ary.length;
		
		$ary.each(function(){
			codes += $(this).val();
			if(i+1 < arySize){
				codes += ",";
			}
			i++;
		});
		
		var codeAry = codes.split(',');
		var ncodeAry = codeAry.sort();
		for(var i=0;i<codeAry.length;i++){
			if(ncodeAry[i]==ncodeAry[i+1]){
				bc.msg.alert("\""+ncodeAry[i]+"\""+"资源编码不能重复,请重新输入.");
				return;
			}
		}
		
//		//部署资源code唯一性检测
//		$.ajax({
//			url:bc.root+"/bc-workflow/deploy/isUniqueResourceCodeAndExtCheck",
//			data:{id:$form.find(":input[name='e.id']").val(),codes:codes},
//			dataType:"json",
//			success:function(json){
//				logger.info($.toJSON(json));
//				var result = json.result 
//				if(result){
//					bc.msg.alert(json.result+"以上资源编码已存在!请重新输入.");
//					$form.find("#flag").val("false");
//				}else{
//					$form.find("#flag").val("true");
//				}
//			}
//		});
//		var flag = $form.find("#flag").val();
//		if(flag == "false"){
//			return;
//		}
		
		//先将资源集合合并到隐藏域
		var resources=[];
		var flag = false;
		$form.find("#resourceTables tr:gt(0)").each(function(){
			var $hiddens = $(this).find(".id.first>input");//找样式为id first的td
			var $inputs = $(this).find(":not(.id.first)>input");//找不是样式为id first的td
			var $divInput= $(this).find("td>div>input");
			var $selects= $(this).find("td>select");

			var formattedVale;
			if($inputs[4].checked){
				formattedVale = "true";
			}else{
				formattedVale = "false";
			}
			var typeValue = $selects[0].value;
			if(typeValue.indexOf(',') > 0){
				typeValue = typeValue.split(",")[0];
			}
			var json = {
				uid: $hiddens[0].value,
				size: $hiddens[1].value,
				source: $hiddens[2].value,
				type: typeValue,
				code: $inputs[0].value,
				subject: $inputs[1].value,
				path: $divInput[0].value,
				formatted: formattedVale
			};
			//处理资源id
			var id = $(this).attr("data-id");
			if(id && id.length > 0){
				json.id = id;
			}
			//处理模板参数的id
			var templateParamIds=[];
			$(this).find('.templateParamLi').each(function(){
				templateParamIds.push($(this).attr("data-id"));
			});
			if(templateParamIds.length == 0 && formattedVale == "true"){
				flag = true;
			}
			json.paramIds = templateParamIds.join(",");
			
			resources.push(json);
		});
		if(flag){
			bc.msg.alert("资源列表中含有已选中格式化的资源,但没有添加参数,请添加参数!!");
			return;
		}
		
		$form.find(":input[name='resources']").val($.toJSON(resources));
		
		//将用户的id合并到隐藏域
		var ids=[];
		$form.find("#assignUsers .deployUserLi").each(function(){
			ids.push($(this).attr("data-id"));
		});
		$form.find(":input[name=assignUserIds]").val(ids.join(","));
		
		//定义函数
		function saveInfo(){
			var id=$form.find(":input[name='e.id']").val();
			var code=$form.find(":input[name='e.code']").val();
			var version=$form.find(":input[name='e.version']").val();
			var url=bc.root+"/bc-workflow/deploy/isUniqueCodeAndVersion";
			$.ajax({
				url:url,
				data:{id:id,code:code,version:version},
				dataType:"json",
				success:function(json){
					var result=json.result;
					if(result=='save'){
						bc.page.save.call($form);
					}else{
						//系统中已有此编码
						bc.msg.alert("此编码、版本号已被其它部署流程使用，请修改编码或版本号！");
					}
				}
			});
		}
		
		
		//模板类型后缀名
		var path=$form.find(":input[name='e.path']").val();
		
		//验证后缀名
		var lastIndex=path.lastIndexOf(".");
		if(lastIndex==-1){
			bc.msg.alert('上传的文件后缀名错误！');
			return;
		}
		//后缀名
		var ext=path.substr(lastIndex+1);
		var typeExt1 = "xml";
		var typeExt2 = "bpmn";
		var typeExt3 = "bar";
		var typeExt4 = "zip";
		var typeSelect = $form.find(":input[name='e.type']")[0].selectedIndex;
		
		//判断上传文件的后缀名是否与模板类型的后缀名相同  
		if(typeSelect == 0){//XML
			if(ext == typeExt1 || ext == typeExt2){
				saveInfo();
			}else{
				bc.msg.alert("只能上传扩展名为"+'".bpmn20.xml"或"bpmn的文件"');
				return;
			}
		}
		
		if(typeSelect == 1){//BAR
			if(ext == typeExt3 || ext == typeExt4){
				saveInfo();
			}else{
				bc.msg.alert("只能上传扩展名为"+'".bar"或".zip的文件"');
				return;
			}
		}
		
	}

};