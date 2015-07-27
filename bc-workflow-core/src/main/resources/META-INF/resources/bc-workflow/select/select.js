bc.namespace("bc.flow");
bc.flow.selectDialog = {
	/** 点击确认按钮后的处理函数 */
	clickOk : function() {
		var $page = $(this);
		
		// 获取选中的行的id单元格
		var $tds = $page.find(".bc-grid>.data>.left tr.ui-state-highlight>td.id");
		if($tds.length == 0){
			bc.msg.slide("请先选择！");
			return false;
		}

		// 获取选中的数据
		var data;
		var $grid = $page.find(".bc-grid");
		if ($grid.hasClass("singleSelect")) {// 单选
			data = {};
			data.id = $tds.attr("data-id");
			var $tr = $grid.find(">.data>.right tr.ui-state-highlight");
			data.name= $tr.find("td:eq(0)").attr("data-value");
			data.version= $tr.find("td:eq(1)").attr("data-value");
			data.deployTime= $tr.find("td:eq(2)").attr("data-value");
			$.extend(data,$tr.data("hidden"));
		}else{
			data=[];
			var $trs = $grid.find(">.data>.right tr.ui-state-highlight");
			$tds.each(function(i){
				var $tr = $($trs.get(i));
				var name = $tr.find("td:eq(0)").attr("data-value");
				var version = $tr.find("td:eq(1)").attr("data-value");
				var deployTime = $tr.find("td:eq(2)").attr("data-value");
				var key=$tr.data("hidden").key;
				data.push({
					id: $(this).attr("data-id"),
					name:name,
					version:version,
					deployTime:deployTime,
					key:key
				});
			});	
		}
		logger.info($.toJSON(data));

		// 返回
	    $page.data("data-status", data);
		$page.dialog("close");
	}
};