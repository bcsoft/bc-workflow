if (!window['bc']) window['bc'] = {};
bc.namespace("bc.flowattachForm");
bc.flowattachForm = {
  init: function () {
    $form = $(this);

    //格式化按钮控制
    //var type=$form.find(":input[name='e.type']").val();	
    //var templateId=$form.find(":input[name='e.templateId']").val();
    //if(type == 1 && templateId == "")
    var params = $form.find(":input[name='params']").val();
    if (params.length == 0)
      $form.find("#formattedtr").hide();

    //绑定清除按钮事件
    $form.find("#cleanFileId").click(function () {
      bc.file.clearFileSelect($form.find("#uploadFile"));
      $form.find(":input[name='e.path']").val('');
      $form.find(":input[name='e.subject']").val('');
      $form.find("#formattedtr").hide();
    });

    //绑定下载按钮事件
    $form.find(".downLoadFileId").click(function () {
      var subject = $form.find(":input[name='e.subject']").val();
      var path = $form.find(":input[name='e.path']").val();
      var id = $form.find(":input[name='e.id']").val();
      if (id == "") {
        bc.msg.slide('请先确认添加附件！');
        return;
      }
      if (!bc.validator.validate($form)) return;
      var n = subject;// 获取文件名
      var f = "workflow/attachment/" + path;// 获取附件相对路径			
      // 下载文件
      bc.file.download({f: f, n: n});
    });

    //绑定选择模板按钮
    $form.find("#loadAttachFromTemplate").click(function () {
      //获取流程实例id
      var pid = $form.find(':input[name="e.pid"]').val();
      //ajax请求获取实例ID的流程的名称
      bc.ajax({
        url: bc.root + "/bc-workflow/flowattach/loadProcInstName",
        data: {pid: pid},
        dataType: "json",
        success: function (piName) {
          //根据名称，查询属于本流程的模板
          var category = "流程附件";
          if (piName.name)
            category += "," + category + '/' + piName.name;
          //选择模板
          bc.selectTemplate({
            category: category,
            onOk: function (template) {
              logger.info($.toJSON(template));
              var uid = $form.find(":input[name='e.uid']").val();
              bc.ajax({
                url: bc.root + "/bc-workflow/flowattach/loadAttachFromTemplate",
                data: {tplCode: template.code, uid: uid},
                dataType: 'json',
                success: function (json) {
                  if (json.success) {
                    bc.msg.slide(json.msg);

                    if ($form.find(':input[name="e.subject"]').val() == '')
                      $form.find(':input[name="e.subject"]').val(template.subject);
                    $form.find(':input[name="e.path"]').val(json.path);
                    $form.find(':input[name="params"]').val(template.params);
                    $form.find("#formattedtr").show();
                    if (template.formatted == 'true') {
                      $form.find('input[name="e.formatted"]:first').attr("checked", "checked");
                    } else
                      $form.find('input[name="e.formatted"]:last').attr("checked", "checked");
                  } else
                    bc.msg.alert(json.msg);
                }
              });
            }
          });
        }
      });
    });
  },
  /**意见保存方法*/
  save: function () {
    $page = $(this);
    //意见保存的特殊处理
    if ($page.find(":input[name='e.type']").val() == '2') {
      var desc = $page.find(":input[name='e.desc']").val();
      var subject = $page.find(":input[name='e.subject']").val();
      if (desc == '' && subject == '') {
        bc.msg.alert('请输入意见');
        return;
      } else if (desc == '') {
        if (subject.length > 33) {
          bc.msg.alert('输入的简易意见过长，你可以简化简易描述或者在详细中输入.');
          return;
        }
      } else if (subject == '') {
        if (desc.length < 31) {
          $page.find(":input[name='e.subject']").val(desc);
        } else
          $page.find(":input[name='e.subject']").val(desc.substring(0, 30) + '...');

      } else {
        if (subject.length > 33) {
          bc.msg.alert('输入的简易描述过长，你可以简化简易描述或者在详细中输入.');
          return;
        }
      }
    }

    //调用标准的方法执行保存
    bc.page.save.call($page, {
      callback: function (json) {
        bc.msg.slide(json.msg);
        var type = $page.find(":input[name='e.type']").val();
        //声明返回的信息
        var data = {};
        data.id = $page.find(":input[name='e.id']").val();
        data.type = type;
        data.common = $page.find(":input[name='e.common']").val();
        data.subject = $page.find(":input[name='e.subject']").val();
        data.desc = $page.find(":input[name='e.desc']").val();
        data.author = json.author;
        data.fileDate = json.fileDate;
        data.modifier = json.modifier;
        data.modifiedDate = json.modifiedDate;
        //附件
        if (type == '1') {
          data.uid = $page.find(":input[name='e.uid']").val();
          data.ext = json.ext;
          data.size = json.size;
          data.formatted = json.formatted;
          data.path = json.path;
        }
        logger.info($.toJSON(data));
        $page.data("data-status", data);
        $page.dialog("close");
      }
    });
  },
  /** 文件上传完毕后 */
  afterUploadfile: function (json) {
    logger.info($.toJSON(json));
    if (json.success) {
      var $page = this.closest(".bc-page");
      $page.find(':input[name="e.subject"]').val(json.source);
      $page.find(':input[name="e.path"]').val(json.to);
    } else
      bc.msg.alert(json.msg);
  }
};