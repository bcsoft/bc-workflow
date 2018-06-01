bc.namespace("bc.myTodoView");
bc.myTodoView = {
  init: function () {
    var $page = $(this);
  },
  //领取
  signTask: function () {
    var $page = $(this);
    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));

    //获取选中的行
    var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
    var $hidden = $tr.data("hidden");

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择要签领的信息！");
      return;
    } else if (ids.length == 1) {
      // 检测是否已有签领记录，没有才允许继续签领，避免重复签领
      jQuery.ajax({
        url: bc.root + "/bc-workflow/todo/personals/isSigned",
        data: {excludeId: ids[0]},
        dataType: "json",
        success: function (json) {
          // 如果已经签领过就提示用户
          if (json.signed == "true") {
            bc.msg.alert('此任务已经签领.');
          } else {
            bc.msg.confirm("确定签领此任务吗？", function () {
              jQuery.ajax({
                url: bc.root + "/bc-workflow/todo/personals/claimTask",
                data: {excludeId: ids[0]},
                dataType: "json",
                success: function (json) {
                  bc.msg.slide(json.msg);
                  bc.grid.reloadData($page);
                  bc.sidebar.refresh();
                  // 打开工作空间
                  bc.flow.openWorkspace({id: $hidden.procinstId});
                }
              });
            });
            return;
          }
        }
      });

    } else {
      bc.msg.slide("一次只能签领一条任务！");
    }
  },

  //委托
  delegateTask: function () {
    var $page = $(this);
    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));
    var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
    var $hidden = $tr.data("hidden");

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择一条任务！");
      return;
    } else if (ids.length == 1) {
      var status = $tr.find("td:eq(0)").attr("data-value");

      if (status == "2") {
        bc.msg.alert("已暂停的任务不能进行委托！");
        return;
      }

      if ($hidden.assignee != null) {
        // 选择委托人
        bc.identity.selectUser({
          history: false,
          onOk: function (user) {
            jQuery.ajax({
              url: bc.root + "/bc-workflow/workflow/delegateTask",
              data: {id: ids[0], toUser: user.account},
              dataType: "json",
              success: function (json) {
                if (json.success) {//成功刷新边栏
                  bc.msg.slide(json.msg);
                  bc.grid.reloadData($page);
                  bc.sidebar.refresh();
                } else {
                  bc.msg.alert(json.msg);
                }
              }
            });
          }
        });
      } else {
        bc.msg.alert("不能委托岗位任务！");
      }
    } else {
      bc.msg.slide("一次只能委托一条任务！");
    }

  },

  //指派
  assignTask: function () {
    var $page = $(this);
    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));
    var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
    var $hidden = $tr.data("hidden");

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择一条任务！");
      return;
    } else if (ids.length == 1) {
      if ($hidden.assignee == null) {

        // 选择指派人
        bc.flow.selectUser({
          taskId: ids[0],
          onOk: function (user) {
            jQuery.ajax({
              url: bc.root + "/bc-workflow/workflow/assignTask",
              data: {id: ids[0], toUser: user.account},
              dataType: "json",
              success: function (json) {
                if (json.success) {//成功刷新边栏
                  bc.msg.slide(json.msg);
                  bc.grid.reloadData($page);
                  bc.sidebar.refresh();
                } else {
                  bc.msg.alert(json.msg);
                }
              }
            });
          }
        });
      } else {
        bc.msg.alert("该任务已经领取,不能指派！");
      }
    } else {
      bc.msg.slide("一次只能分派一条任务！");
    }
  },

  open: function () {
    var $page = $(this);
    //获取选中的行
    var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
    var $hidden = $tr.data("hidden");

    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择要查看的信息！");
      return;
    } else if (ids.length == 1) {
      // 打开工作空间
      bc.flow.openWorkspace({id: $hidden.procinstId});
    } else {
      bc.msg.slide("一次只能选择一条信息查看！");
    }
  },

  /** 发起流程 **/
  startflow: function () {
    var $page = $(this);
    bc.flow.start({
      $view: $page,
      constraint: true,
      onStart: function (json) {
        if (json.success === false) {
          bc.msg.alert(json.msg);// 仅显示失败信息
        } else {
          bc.msg.slide(json.msg);
          //重新加载列表
          bc.grid.reloadData($page);
          //刷新待办边框
          bc.sidebar.refresh();

          // 打开工作空间
          bc.flow.openWorkspace({id: json.processInstance});
        }
      }
    })
  },
  /** 更多按钮 **/
  selectMenuButtonItem: function (option) {
    var $page = $(this);

    switch (option.value) {
      case "signTask":
        bc.myTodoView.signTask.call($page);
        break;
      case "delegateTask":
        bc.myTodoView.delegateTask.call($page);
        break;
      case "delete":
        bc.myTodoView.delete_.call($page);
        break;
      case "assignTask":
        bc.myTodoView.assignTask.call($page);
        break;
      case "requirement":
        bc.historicTaskInstanceSelectView.requirement.call($page);
        break;
      default:
        alert("other");
    }

  },

  /**
   * 删除
   */
  delete_: function () {
    var $page = $(this);
    //获取选中的行
    var $tr = $page.find(".bc-grid>.data>.right tr.ui-state-highlight");
    var $hidden = $tr.data("hidden");
    var wf_code = $hidden.wf_code;			// 流水号
    var procinstName = $hidden.procinstName;// 流程名称
    var msg = !wf_code
      ? "确定要删除选定的 <b>" + procinstName + "</b> 吗？"
      : "确定要删除选定的 <b>" + procinstName + "(流水号" + wf_code + ")</b> 吗？";

    // 获取用户选中的条目
    var ids = bc.grid.getSelected($page.find(".bc-grid"));

    // 检测是否选中条目
    if (ids.length == 0) {
      bc.msg.slide("请先选择要删除的流程！");
      return;
    } else if (ids.length == 1) {
      bc.msg.confirm(msg, function () {
        // 删除流程
        bc.ajax({
          dataType: "json",
          data: {id: $hidden.procinstId},
          url: bc.root + "/bc-workflow/historicProcessInstances/deleteNotDeal2Personal",
          success: function (json) {
            if (json.success) {
              bc.msg.slide(json.msg);
            } else {
              bc.msg.alert(json.msg);
            }
            bc.grid.reloadData($page);
          }
        });
      });
    } else {
      bc.msg.slide("一次只能删除一条流程！");
    }
  }
};
