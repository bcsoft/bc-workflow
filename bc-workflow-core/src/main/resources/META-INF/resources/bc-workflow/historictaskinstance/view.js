bc.historicTaskInstanceSelectView = {
  /** 打开工作空间 */
  open: function () {
    var $page = $(this);

    // 获取选中的行的id单元格
    var $tds = $page.find(".bc-grid>.data>.left tr.ui-state-highlight>td.id");
    if ($tds.length == 0) {
      bc.msg.slide("请先选择！");
      return false;
    } else if ($tds.length > 1) {
      bc.msg.slide("一次只可以查看一条信息！请确认你只选择一条信息！");
      return false;
    }

    var $grid = $page.find(".bc-grid");
    var $tr = $grid.find(">.data>.right tr.ui-state-highlight");
    var $hidden = $tr.data("hidden");

    // 打开工作空间
    bc.flow.openWorkspace({id: $hidden.procinstId});
  },
  /** 数据纠正流程 **/
  requirement: function () {
    var $page = $(this);

    // 获取选中的行的id单元格
    var $tds = $page.find(".bc-grid>.data>.left tr.ui-state-highlight>td.id");
    if ($tds.length == 0) {
      bc.msg.slide("请先选择！");
      return false;
    } else if ($tds.length > 1) {
      bc.msg.slide("一次只能对一条信息发起数据纠正流程");
      return false;
    }

    var $grid = $page.find(".bc-grid");
    var $tr = $grid.find(">.data>.right tr.ui-state-highlight");
    var $hidden = $tr.data("hidden");

    var data = {
      procinstId: $hidden.procinstId, //流程实例id
      procinstName: $hidden.procinstName, //流程实例名称
      procinstKey: $hidden.procinstKey, //流程KEY
      procinstSubject: $hidden.subject, //流程主题
      procinstTaskName: $hidden.procinstTaskName, //任务名称
      procinstTaskKey: $hidden.procinstTaskKey, //任务KEY
      procinstTaskId: $tds.attr('data-id') //任务id
    }
    bc.msg.confirm("确定对<b>" + $hidden.procinstName + "</b>的<b>" + $hidden.procinstTaskName + "</b>任务发起<b>数据纠正处理流程</b>吗？"
      , function () {
        bc.ajax({
          url: bc.root + "/bc-workflow/historicTaskInstances/startFlow",
          data: {startFlowKey: "Requirement", processData: $.toJSON(data)},
          dataType: "json",
          success: function (json) {
            if (json.success) {
              // 刷新边栏
              bc.sidebar.refresh();

              // 打开工作空间
              bc.flow.openWorkspace({id: json.procinstId});
            }
          }
        });
      }, function () {

      }, "发起数据纠正流程确认窗口");

  }
};