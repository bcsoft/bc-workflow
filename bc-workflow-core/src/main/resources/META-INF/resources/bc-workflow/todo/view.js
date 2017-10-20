bc.namespace("bc.todoView");
bc.todoView = {
	init : function() {
		var $page = $(this);
	},
	//委托
	delegateTask : function (){
		var $page = $(this);
		// 获取用户选中的条目
		var ids = bc.grid.getSelected($page.find(".bc-grid"));
		var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
		var $hidden = $tr.data("hidden");
		
		// 检测是否选中条目
		if(ids.length ==0){
			bc.msg.slide("请先选择一条任务！");
			return;
		}else if(ids.length == 1){
			var status=$tr.find("td:eq(0)").attr("data-value");
			var type=$hidden.type;
			
			if(status == "2"){
				bc.msg.alert("已暂停的任务不能进行委托！");
				return;
			}
			
			if(type == 1){
				// 选择委托人
				bc.identity.selectUser({
					history: false,
					status: "0",
					onOk : function(user) {
						jQuery.ajax({
							url: bc.root + "/bc-workflow/workflow/delegateTask", 
							data: {id: ids[0],toUser: user.account},
							dataType: "json",
							success: function(json) {
								if(json.success){//成功刷新边栏
									bc.msg.slide(json.msg);
									bc.grid.reloadData($page);
									bc.sidebar.refresh();
								}else{
									bc.msg.alert(json.msg);
								}
							}
						});
					}
				});
			}else{
				bc.msg.alert("不能委托岗位任务！");
				return;
			}
		}else{
			bc.msg.slide("一次只能委托一条任务！");
			return;
		}

	},
	//指派
	assignTask : function (){
		var $page = $(this);
		// 获取用户选中的条目
		var ids = bc.grid.getSelected($page.find(".bc-grid"));
		var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
		var $hidden = $tr.data("hidden");
		
		// 检测是否选中条目
		if(ids.length ==0){
			bc.msg.slide("请先选择一条任务！");
			return;
		}else if(ids.length == 1){
			if($hidden.type == "2"){
				// 选择指派人
				bc.flow.selectUser({
					taskId: ids[0],
					onOk : function(user) {
						jQuery.ajax({
							url: bc.root + "/bc-workflow/workflow/assignTask", 
							data: {id: ids[0],toUser: user.account},
							dataType: "json",
							success: function(json) {
								if(json.success){//成功刷新边栏
									bc.msg.slide(json.msg);
									bc.grid.reloadData($page);
									bc.sidebar.refresh();
								}else{
									bc.msg.alert(json.msg);
								}
							}
						});
					}
				});
			}else{
				bc.msg.alert("该任务已经领取,不能分派！");
			}
		}else{
			bc.msg.slide("一次只能分派一条任务！");
		}
	},
	open : function (){
		var $page = $(this);
		//获取选中的行
		var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
		var $hidden = $tr.data("hidden");
		
		// 获取用户选中的条目
		var ids = bc.grid.getSelected($page.find(".bc-grid"));
		
		// 检测是否选中条目
		if(ids.length ==0){
			bc.msg.slide("请先选择要查看的信息！");
			return;
		}else if(ids.length == 1){
			// 打开工作空间
			bc.flow.openWorkspace({id : $hidden.procinstId});
		}else{
			bc.msg.slide("一次只能选择一条信息查看！");
		}
	}
};
