bc.myHistoricTaskInstanceSelectView = {
  /** 查看经办流程 **/
  viewflow: function () {
    bc.page.newWin({
      url: bc.root + "/bc-workflow/myHistoricProcessInstances/paging",
      data: "status=" + "",// 装状态重置为全部
      title: "查看经办流程",
      mid: "bc.flow.myHistoricProcessInstances",
      name: "查看经办流程"
    });
  }
};