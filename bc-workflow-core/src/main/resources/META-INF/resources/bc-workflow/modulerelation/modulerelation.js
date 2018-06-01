/**
 * 选择流程模块关系
 * @param {Object} option 配置参数
 * @option {Long} moduleId [可选]  模块id
 * @option {String} moduleType [可选]  模块类型
 * @option {String} processKey [可选]   流程编码
 * @option {Boolean} complete [可选]  已完成的流程，默认false
 * @option {Boolean} multiple [可选]是否允许多选，默认false
 * @option {Boolean} paging [可选]是否分页，默认false
 * @option {Function} onOk 选择完毕后的回调函数，
 * 单选返回一个对象 格式为{
 * 					id:[id],				--流程id	
 * 					subject:[subject],		--流程主题
 * 					name:[name],			--流程名称
 * 					key:[key],				--流程编码
 * 					}
 * 如果为多选则返回的是对象集合，[对象1,对象2]。
 */
bc.flow.selectWorkflowModuleRelation = function (option) {
  // 构建默认参数
  option = jQuery.extend({
    mid: 'selectWorkflowModuleRelation',
    paging: false,
    title: '选择流程信息'
  }, option);

  // 将一些配置参数放到data参数内(这些参数是提交到服务器的参数)
  option.data = jQuery.extend({
    multiple: false
  }, option.data);
  if (option.title)
    option.data.title = option.title;
  if (option.multiple === true)
    option.data.multiple = true;

  //弹出选择对话框
  bc.page.newWin(jQuery.extend({
    url: bc.root + "/bc-workflow/selectWorkflowModuleRelation/" + (option.paging ? "paging" : "list"),
    name: option.title,
    mid: option.mid,
    afterClose: function (status) {
      if (status && typeof(option.onOk) == "function") {
        option.onOk(status);
      }
    }
  }, option));
}
