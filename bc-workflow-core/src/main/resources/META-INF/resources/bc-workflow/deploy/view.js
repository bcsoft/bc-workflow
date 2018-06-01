bc.namespace("bc.deploy");
bc.deploy = {
  init: function () {
    var $page = $(this);
  },

  //发布
  release: function () {

    var $page = $(this);
    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));

    //定义函数
    function releaseInfo() {

      jQuery.ajax({
        url: bc.root + "/bc-workflow/deploys/dodeployRelease",
        data: {
          excludeId: ids[0]
        },
        dataType: "json",
        success: function (json) {
          bc.msg.slide(json.msg);
          bc.grid.reloadData($page);
        }
      });
      return;
    }

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择要发布的信息！");
      return;
    } else if (ids.length == 1) {
      var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
      var $hidden = $tr.data("hidden");

      if ($hidden.status == 0) {
        bc.msg.alert("此信息已发布,不能重复发布!");
        return;
      } else if ($hidden.status == 1) {//状态为禁用的流程
        jQuery.ajax({
          url: bc.root + "/bc-workflow/deploys/dodeployChangeStatus",
          data: {excludeId: ids[0]},
          dataType: "json",
          success: function (json) {
            bc.msg.slide(json.msg);
            bc.grid.reloadData($page);
          }
        });
      } else {
        bc.msg.confirm("确定要发布此流程吗？", function () {
          releaseInfo();
        });
      }

    } else {
      bc.msg.slide("一次只能选择一条信息发布！");
    }
  },

  //取消发布
  releaseCancel: function () {
    var $page = $(this);
    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择要取消发布的信息！");
      return;
    } else if (ids.length == 1) {

      var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
      var $hidden = $tr.data("hidden");

      if ($hidden.status == 0) {

        jQuery.ajax({
          url: bc.root + "/bc-workflow/deploys/isStarted",
          data: {excludeId: ids[0]},
          dataType: "json",
          success: function (json) {
            if (json.started == "true") {
              bc.msg.alert(json.msg);
            } else {
              bc.msg.confirm("确定要取消发布吗？", function () {
                jQuery.ajax({
                  url: bc.root + "/bc-workflow/deploys/dodeployCancel",
                  data: {excludeId: ids[0], isCascade: false},
                  dataType: "json",
                  success: function (json) {
                    bc.msg.slide(json.msg);
                    bc.grid.reloadData($page);
                  }
                });
              });
            }
          }
        });

      } else {
        bc.msg.alert("未发布的信息不能取消发布！");
      }


    } else {
      bc.msg.slide("一次只能选择一条信息取消发布！");
    }
  },

  //级联取消发布
  cascadeCancel: function () {
    var $page = $(this);
    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择要取消发布的信息！");
      return;
    } else if (ids.length == 1) {

      var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
      var $hidden = $tr.data("hidden");

      if ($hidden.status == 0) {

        bc.msg.confirm("确定要级联取消吗？", function () {
          jQuery.ajax({
            url: bc.root + "/bc-workflow/deploys/dodeployCancel",
            data: {excludeId: ids[0], isCascade: true},
            dataType: "json",
            success: function (json) {
              bc.msg.slide(json.msg);
              bc.grid.reloadData($page);
            }
          });
        });

      } else {
        bc.msg.alert("未发布的信息不能取消发布！");
      }


    } else {
      bc.msg.slide("一次只能选择一条信息取消发布！");
    }
  },

  //停用
  stop: function () {
    var $page = $(this);
    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择需要禁用的部署的信息！");
      return;
    } else if (ids.length == 1) {

      var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
      var $hidden = $tr.data("hidden");

      if ($hidden.status == 0) {

        bc.msg.confirm("确定要停用此流程部署信息吗？", function () {
          jQuery.ajax({
            url: bc.root + "/bc-workflow/deploys/dodeployStop",
            data: {excludeId: ids[0]},
            dataType: "json",
            success: function (json) {
              bc.msg.slide(json.msg);
              bc.grid.reloadData($page);
            }
          });
        });

      } else {
        bc.msg.alert("草稿或已停用的信息不能再停用！");
      }


    } else {
      bc.msg.slide("一次只能选择停用一条部署信息！");
    }
  },

  /** 双击视图处理 */
  dblclick: function () {
    var $page = $(this);
    var $grid = $page.find(".bc-grid");
    var $trs = $grid.find(">.data>.right tr.ui-state-highlight");
    var status = $trs.find("td:eq(0)").attr("data-value");
    //如果是草稿状态的调用编辑方法其他的调用打开方法
    if (status == -1) {
      bc.page.edit.call($page);
    } else {
      bc.page.open.call($page);
    }
  },
  /** 访问监控 **/
  accessControl: function () {
    var $page = $(this);
    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));
    if (ids.length == 0) {
      bc.msg.slide("请先选择需要访问配置的信息！");
      return;
    }

    if (ids.length > 1) {
      bc.msg.slide("只能对一个信息操作！");
      return;
    }

    var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
    var $hidden = $tr.data("hidden");

    bc.addAccessControl({
      docId: ids[0],
      docType: $hidden.accessControlDocType,
      docName: $hidden.accessControlDocName,
      showRole: "01"
    });
  }
};
