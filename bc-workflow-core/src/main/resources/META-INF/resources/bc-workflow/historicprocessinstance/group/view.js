bc.namespace("bc.groupHistoricprocessinstance");
bc.groupHistoricprocessinstance = {
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

    var pid = $tds.attr("data-id");

    var data = {
      docId: pid,
      docName: $hidden.accessControlDocName,
      docType: $hidden.accessControlDocType,
      url: "/bc-workflow/workspace/open?id=" + pid,
      deployId: $hidden.deployId
    }


    // 打开工作空间
    bc.flow.openWorkspace({id: pid, data: {isAccess: true, accessJson: $.toJSON(data)}});
  }
};