/**
 * 主页待办初始化函数
 *
 * @author rongjihuang@gmail.com
 * @date 2012-06-27
 * @dep jquery
 */
jQuery(function ($) {
  logger.info("loadToDoPanel...");
  var $container = $("#right");

  // 显示加载动画
  $container.html('<img src="' + bc.root + '/bc/libs/themes/default/images/loader/loader02_64x64.gif" style="position:absolute;top:50%;margin-top:-32px;;left:50%;margin-left:-32px;"/>');

  // 加载待办页面
  bc.ajax({
    url: bc.root + "/bc-workflow/todo/personals/sidebar",
    dataType: "html",
    success: function (html) {
      var $dom = $(html);

      function _init() {
        $container.empty().append($dom);
        var method = $dom.attr("data-initMethod");
        logger.debug("initMethod=" + method);
        if (method) {
          method = bc.getNested(method);
          if (typeof method == "function") {
            method.call($dom);
          } else {
            alert("undefined function: " + $dom.attr("data-initMethod"));
          }
        }
      }

      var dataJs = $dom.attr("data-js");
      if (dataJs && dataJs.length > 0) {
        //先加载js文件后执行模块指定的初始化方法
        dataJs = dataJs.split(",");//逗号分隔多个文件

        // 处理预定义的js、css文件
        var t;
        for (var i = 0; i < dataJs.length; i++) {
          if (dataJs[i].indexOf("js:") == 0) {//预定义的js文件
            t = bc.loader.preconfig.js[dataJs[i].substr(3)];
            if (t) {
              t = bc.root + t;
              logger.debug(dataJs[i] + "=" + t);
              dataJs[i] = t;
            } else {
              alert("没有预定义“" + dataJs[i] + "”的配置，请在loader.preconfig.js文件中添加相应的配置！");
            }
          } else if (dataJs[i].indexOf("css:") == 0) {//预定义的css文件

          }
        }

        dataJs.push(_init);
        bc.load(dataJs);
      } else {
        //执行模块指定的初始化方法
        _init();
      }
    }
  });
});