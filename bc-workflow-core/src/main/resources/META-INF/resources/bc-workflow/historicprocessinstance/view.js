bc.namespace("bc.historicprocessinstance");
bc.historicprocessinstance = {
	init : function() {
		var $page = $(this);
	},
	
	//激活
	active : function (){
		
		var $page = $(this);
		// 获取用户选中的条目
		var ids = bc.grid.getSelected($page.find(".bc-grid"));
		
		// 检测是否选中条目
		if(ids.length ==0){
			bc.msg.slide("请先选择要激活的流程信息！");
			return;
		}else if(ids.length == 1){			
			
			var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
			var $hidden = $tr.data("hidden");
			
			if($hidden.status == 2){
				bc.msg.confirm("确定激活此流程吗？",function(){
					jQuery.ajax({
						url: bc.root + "/bc-workflow/historicProcessInstances/doActive", 
						data: {id: ids[0]},
						dataType: "json",
						success: function(json) {
							bc.msg.slide(json.msg);
							bc.grid.reloadData($page);
						}
					});
				});
			}else{
				bc.msg.alert("只能激活已暂停的流程");
			}

		}else{
			bc.msg.slide("一次只能选择一条流程信息激活！");
		}
	},
	
	//暂停
	suspended : function (){
		var $page = $(this);
		// 获取用户选中的条目
		var ids = bc.grid.getSelected($page.find(".bc-grid"));
		
		// 检测是否选中条目
		if(ids.length ==0){
			bc.msg.slide("请先选择要暂停的流程信息！");
			return;
		}else if(ids.length == 1){
			
			var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
			var $hidden = $tr.data("hidden");
			
			if($hidden.status == 1){
				
				bc.msg.confirm("确定暂停此流程吗？",function(){
					jQuery.ajax({
						url: bc.root + "/bc-workflow/historicProcessInstances/doSuspended", 
						data: {id: ids[0]},
						dataType: "json",
						success: function(json) {
							bc.msg.slide(json.msg);
							bc.grid.reloadData($page);
						}
					});
				});
				
			}else{
				bc.msg.alert("只能暂停流转中的流程");
			}

			
		}else{
			bc.msg.slide("一次只能选择一条流程信息暂停！");
		}
	},
	/** 访问监控 **/
	accessControl : function(){
		var $page = $(this);
		// 获取用户选中的条目
		var ids = bc.grid.getSelected($page.find(".bc-grid"));
		if(ids.length == 0){
			bc.msg.slide("请先选择需要访问配置的信息！");
			return;
		}
		
		if(ids.length > 1){
			bc.msg.slide("只能对一个信息操作！");
			return;
		}
		
		var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
		var $hidden = $tr.data("hidden");
		
		bc.addAccessControl({
			docId:ids[0],
			docType:$hidden.accessControlDocType,
			docName:$hidden.accessControlDocName,
			showRole:"01"
		});
	}	

};
