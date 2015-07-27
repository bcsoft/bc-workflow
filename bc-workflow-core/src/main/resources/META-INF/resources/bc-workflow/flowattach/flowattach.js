/**
 * 流程附件处理函数
 *
 * @author lbj
 * @date 2012-07-06
 */
if(!window['bc'])window['bc']={};
bc.flowattach = {
		/**
		 * 新建流程附加信息
		 * @param {Object} option 配置参数
		 * @option {String} pid 流程实例ID 必填 
		 * @option {String} tid 流程任务ID 
		 * @option {int} type 类型：1-附件，2-意见 默认附件
		 * @option {boolean} common 是否公共信息 默认公共
		 * @option {Function} onOk 点击保存后的回调函数，
		 * 选返回一个对象 格式为{
		 * 					id:[id],				--id
		 * 					type:[type],			--type 类型：1-附件，2-意见
		 * 					common:[common],		--common 是否公共信息
		 * 					subject:[subject],		--标题
		 * 					path:[path],			--附件路径
		 * 					ext:[ext],				--附件扩展名
		 * 					desc:[desc]				--附件的时候为备注,意见的时候为意见信息
		 * 					formatted:[formatted]	--格式化
		 * 					author:[author]			--创建人
		 * 					fileDate:[fileDate]		--创建时间
		 * 					}
		 */
		create : function(option){
			// 将一些配置参数放到data参数内(这些参数是提交到服务器的参数)
			option.data = jQuery.extend({
				pid:option.pid,
				tid:option.tid?option.tid:'',
				type:option.type?option.type:1,
				common: option.common === true
			},option.data);

			var attachType=option.data.type==1?'attachment':'comment';
				
			bc.page.newWin(jQuery.extend({
				name:option.data.type==1?'添加附件':'请输入意见',
				title:option.data.type==1?'添加附件':'请输入意见',
			   	url:bc.root +"/bc-workflow/flowattach/create",
				mid:'flowattach.create.'+attachType+'.'+option.data.pid+option.data.tid,
				afterClose: function(status){
					if(status && typeof(option.onOk) == "function")
						option.onOk(status);
				}
			},option));
		},	
		/**
		 * 编辑流程附加信息
		 * @param {Object} option 配置参数
         * @option {int} id id号
         * @option {string} subject 标题
         * @option {int} type 类型：1-附件，2-意见 
		 * @option {Function} onOk 点击保存后的回调函数，
		 * 选返回一个对象 格式为{
		 * 					id:[id],				--id
		 * 					type:[type],			--type 类型：1-附件，2-意见
		 * 					common:[common],		--common 是否公共信息
		 * 					subject:[subject],		--附件类型下的标题
		 * 					path:[path],			--附件路径
		 * 					ext:[ext],				--附件扩展名
		 * 					desc:[desc]				--附件的时候为备注,意见的时候为意见信息
		 * 					formatted:[formatted]	--格式化
		 * 					author:[author]			--创建人
		 * 					fileDate:[fileDate]		--创建时间
		 * 					modifier:[modifier]		--最后修改人 
		 * 					modifiedDate:[modifiedDate]	--最后修改时间 
		 * 				}
		 */
		edit : function(option){
			var name=option.type==1?'维护附件':'维护意见';
			if(option.subject)
				name+=':'+option.subject;
			
			bc.page.newWin(jQuery.extend({
				name:name,
				title:option.type==1?'维护附件':'维护意见',
			   	url: bc.root +"/bc-workflow/flowattach/edit?id="+option.id,
				mid: 'flowattach.edit.'+option.id,
				afterClose: function(status){
					if(status && typeof(option.onOk) == "function")
						option.onOk(status);	
				}
			},option));
		},
		/**
		 * 查看流程附加信息
		 * @option {int} id 
		 * @option {string} subject 标题
		 * @option {int} type 类型：1-附件，2-意见 
		 *
		 */
		open : function(option){
			var name=option.type==1?'查看附件':'查看意见';
			if(option.subject)
				name+=':'+option.subject;

			bc.page.newWin(jQuery.extend({
				title:option.type==1?'查看附件':'查看意见',
				name:name,
			   	url: bc.root +"/bc-workflow/flowattach/open?id="+option.id,
				mid: 'flowattach.open.'+option.id
			},option));
		},
		/**
		 * 删除流程附加信息
		 * @option {int} id 
		 * @option {int} type 类型：1-附件，2-意见 
		 * @option {Function} onOk 点击删除后的回调函数,返回json信息{success:[success],msg:[msg]}
		 *
		 */
		delete_ : function(option){
			var attachType=option.type==1?'意见':'附件'
			bc.msg.confirm("确定要删除"+attachType+"吗？",function(){
				bc.ajax({
					url: bc.root +"/bc-workflow/flowattach/delete?id="+option.id,
					dataType: "json",
					success: function(json) {
						//调用callback
						if(typeof(option.onOk) == "function"){
							option.onOk(json);
							return;
						}
						// 仅显示失败信息
						if(json.success == 'false'){
							bc.msg.alert(json.msg);
							return;
						}
						//输出完成删除信息	
						bc.msg.slide(json.msg);
					}
				});
			});
		},
		/**
		 * 下载附件，只支持附件
		 * @id {id} id
		 */
		download : function(option){
			var url = bc.root + "/bc-workflow/flowattachfile/download?id=" + option.id;			
			var win = window.open(url, "blank");
			if(typeof option.callback == "function"){
				option.callback.call(this,option,win);
			}
			return win;
		},
		/**
		 * 在线查看附件，只支持附件
		 * @id {id} id	
		 *
		 */
		inline : function(option){
			var url = bc.root + "/bc-workflow/flowattachfile/inline?id=" + option.id;
			var win = window.open(url, "_blank");
			if(typeof option.callback == "function"){
				option.callback.call(this,option,win);
			}
			return win;
		}
};



