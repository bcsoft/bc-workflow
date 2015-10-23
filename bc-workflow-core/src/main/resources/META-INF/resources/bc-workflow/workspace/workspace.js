bc.namespace("bc.flow.workspace");
bc.flow.workspace = {
	init: function () {
		var $page = $(this);
		var $common = $page.children(".common");
		var $wsform = $page.children("#wsForm");

		// 记录流程实例的id
		var pid = $wsform.find("input[name='id']").val();
		var subject = $wsform.find("input[name='subject']").val();

		// 查看异常堆栈信息
		$page.children(".error").find(".click2see").click(function () {
			$(this).parent().next().toggleClass("hide");
		});

		// 加载流程、任务表单的js、css文件
		bc.flow.workspace.loadJsCss($page.find(".line>.form>:first-child"));

		// 总区域的折叠或展开
		$page.delegate(".header>.rightIcons>.toggle", {
			click: function (e) {
				var $this = $(this);
				$this.children(".ui-icon").toggleClass("ui-icon-triangle-1-n ui-icon-triangle-1-s");
				$this.closest(".header").next().children(".info").toggleClass("hide");

				return false;
			}
		});

		// 反转详细信息区域的显示
		$page.delegate(".header>.rightIcons>.reverse,.topic>.rightIcons>.reverse", {
			click: function (e) {
				var $h = $(this).closest(".header,.simple");
				var $infos = $h.next().children(".info");
				$infos.toggleClass("collapse");
				$infos.find(">.simple>.line>.rightIcons>.toggle>.ui-icon").toggleClass("ui-icon-carat-1-sw ui-icon-carat-1-ne");
				return false;
			}
		});

		// 单个内部细节的折叠或展开
		$page.delegate(".info>.simple>.line>.rightIcons>.toggle", {
			click: function (e) {
				var $this = $(this);
				$this.children(".ui-icon").toggleClass("ui-icon-carat-1-sw ui-icon-carat-1-ne");

				var $info = $this.closest(".info");
				$info.toggleClass("collapse");

				return false;
			}
		});

		// 公共信息区添加意见、附件，待办信息区添加意见、附件、完成办理、分派任务、签领任务
		$page.delegate(".common>.header>.rightIcons>.mainOperate,.todo>.items>.info>.simple>.line>.rightIcons>.mainOperate", {
			click: function (e) {
				var $this = $(this);
				var $line = $this.closest(".line");
				var $info = $this.closest(".info");
				if ($this.is(".flowImage")) {// 查看流程图
					window.open(bc.root + "/bc-workflow/workflow/diagram?id=" + pid, "_blank");
				} else if ($this.is(".excutionLog")) {// 查看流转日志
					bc.page.newWin({
						name: subject, mid: "excutionLog" + pid,
						url: bc.root + "/bc-workflow/excutionLogs/list?pid=" + pid
					});
				} else if ($this.is(".addComment")) {// 添加意见
					if ($line.is(".header")) {// 添加公共意见
						bc.flow.workspace.addComment.call($line.next(), "common", pid);
					} else if ($line.is(".topic")) {// 添加待办意见
						bc.flow.workspace.addComment.call($info.children(".detail"), "todo", pid, $info.data("id"));
					}
				} else if ($this.is(".addAttach")) {// 添加附件
					if ($line.is(".header")) {// 添加公共附件
						bc.flow.workspace.addAttach.call($line.next(), "common", pid);
					} else if ($line.is(".topic")) {// 添加待办附件
						bc.flow.workspace.addAttach.call($info.children(".detail"), "todo", pid, $info.data("id"));
					}
				} else if ($this.is(".finish")) {// 完成办理
					bc.flow.workspace.finishTask.call($info, pid, $info.data("id"));
				} else if ($this.is(".delegate")) {// 委派任务
					bc.flow.workspace.delegateTask.call($info, $info.data("id"));
				} else if ($this.is(".claim")) {// 签领任务
					bc.flow.workspace.claimTask.call($info, $info.data("id"));
				} else if ($this.is(".assign")) {// 分配任务
					bc.flow.workspace.assignTask.call($info, $info.data("id"));
				} else if ($this.is(".suspended")) {
					bc.msg.confirm("确定暂停此流程吗？", function () {
						jQuery.ajax({
							url: bc.root + "/bc-workflow/flowMonitors/doSuspended",
							data: {id: pid},
							dataType: "json",
							success: function (json) {
								bc.msg.slide(json.msg);
								$this.closest(".bc-page").dialog("close");
								// 刷新边栏
								bc.sidebar.refresh();
							}
						});
					});
				} else if ($this.is(".active")) {
					bc.msg.confirm("确定激活此流程吗？", function () {
						jQuery.ajax({
							url: bc.root + "/bc-workflow/flowMonitors/doActive",
							data: {id: pid},
							dataType: "json",
							success: function (json) {
								bc.msg.slide(json.msg);
								$this.closest(".bc-page").dialog("close");
								var name = $this.closest(".bc-page").find('#wsForm').find(":input[name='subject']").val();
								// 打开工作空间
								bc.flow.openWorkspace({id: pid, name: name});
								// 刷新边栏
								bc.sidebar.refresh();
							}
						});
					});
				} else {
					alert("TODO:添加??,class=" + $this.attr("class"));
				}

				return false;
			}
		});

		// 点击表单、意见、附件
		$page.delegate(".line>.text", {
			click: function (e) {
				var $line = $(this).closest(".line");
				var $info = $line.closest(".info");
				if ($line.is(".form")) {// 打开表单
					alert("TODO:打开表单");
				} else if ($line.is(".comment")) {// 打开意见
					bc.flow.workspace.openComment.call($info, $info.data("id"));
				} else if ($line.is(".attach")) {// 打开附件
					bc.flow.workspace.openAttach.call($info, $info.data("id"));
				} else if ($line.is(".stat")) {// 统计信息折叠
					$info.toggleClass("collapse").find(".rightIcons>.toggle>:first").toggleClass("ui-icon-carat-1-ne ui-icon-carat-1-sw");
				}
				return false;
			}
		});

		// 表单、意见、附件的操作按钮
		$page.delegate(".line>.rightIcons>.itemOperate", {
			click: function (e) {
				var $this = $(this);
				var $info = $this.closest(".info");
				if ($this.is(".edit")) {// 编辑
					if ($info.hasClass("attach")) {
						bc.flow.workspace.editAttach.call($info, $info.data("id"));
					} else if ($info.hasClass("comment")) {
						bc.flow.workspace.editComment.call($info, $info.data("id"));
					} else {
						alert("未支持的编辑类型");
					}
				} else if ($this.is(".open")) {// 查看
					if ($info.hasClass("attach")) {
						bc.flow.workspace.openAttach.call($info, $info.data("id"));
					} else if ($info.hasClass("comment")) {
						bc.flow.workspace.openComment.call($info, $info.data("id"));
					} else {
						alert("未支持的查看类型");
					}
				} else if ($this.is(".download")) {// 下载
					bc.flowattach.download({
						id: $info.data("id"),
						subject: $info.data("subject"),
						path: $info.data("path")
					});
				} else if ($this.is(".delete")) {// 删除
					bc.flowattach.delete_({
						id: $info.data("id"),
						onOk: function (json) {
							// alert($.toJSON(json));
							if (json.success) {
								$info.remove();
							}
						}
					});
				}

				return false;
			}
		});


		// 点击workspace tip Icon
		$page.on("click", ".tip", function () {
			var option;

			if ($(this).has(".taskTile")) {// 配置了taskTile样式，则获取自定义函数：namespace + .getTipConfig
				// 表单form
				var $form = $(this).closest(".simple").siblings(".detail").find(".bc-page");
				//获取form表单的namespace
				var namespace = $form.attr("data-namespace");
				var method = namespace + ".getTipConfig";
				logger.debug("alertOptionMethod=" + method);
				method = bc.getNested(method);
				if (typeof method == "function") {
					option = method.apply($form);
					bc.msg.alert(option);
					return;
				}
			}

			option = JSON.parse($(this).attr("data-option"));
			bc.msg.alert(option);
		});

		// 聚焦到待办的可领取或办理任务区域
		var $focusArr = $page.find(".todo>.items>.info>.simple>.line>.rightIcons>.finish,.todo>.items>.info>.simple>.line>.rightIcons>.claim");
		if ($focusArr.size() > 0) {
			var $focus = $($focusArr.get(0));
			$focus.attr("tabindex", 0);
			$focus.focus();
		}

		// textarea自动高度初始化
		$page.find("textarea.autoHeight").keyup();
	},

	/** 加载流程、任务表单的js、css文件 */
	loadJsCss: function ($forms) {
		$forms.each(function () {
			var $form = $(this);

			var jsCfg = $form.data("js");
			if (!jsCfg || (typeof jsCfg == "string")) {// 原始的js加载
				console.log("workspace:原始的js加载");
				var namespace = $form.attr("data-namespace");
				var dataJs = bc.getJsCss($form.attr("data-js"));
				if (dataJs && dataJs.length > 0) {
					dataJs.push(function () {
						if (namespace) {
							var method = namespace + ".init";
							logger.debug("initMethod=" + method);
							if (method) {
								method = bc.getNested(method);
								if (typeof method == "function") {
									// 执行组件指定的额外初始化方法
									method.call($form);
								} else {
									logger.debug("undefined function: " + namespace + ".init");
								}
							}
						}
					});
					bc.load(dataJs);
				}
			} else if (jQuery.isArray(jsCfg)) {    // 使用 requireJs
				console.log("workspace:使用 requireJs");
				$form.data("requirejs", true);
				for(var i = 0; i < jsCfg.length; i++){
					// 替换 wf: 前缀
					if(jsCfg[i].indexOf("wf:") == 0) jsCfg[i] = "bc-workflow/resource?key=" + jsCfg[i].substring(3);
				}
				require(jsCfg, function () {
					var module = arguments[arguments.length - 1];
					if (typeof module === "function") {                              // 定义为类时
						var instance = new module($form);                            // 实例化类
						$form.data("scope", instance).data("scopeType", "instance");  // 记录此实例
						// 如果有init方法就调用： 一般不需要init方法，直接在构造函数中初始化即可
						instance.init && instance.init($form);
					} else if (typeof module === "object") {                        // 定义为 literal object 时
						$form.data("scope", module).data("scopeType", "module");       // 记录此类
						// 如果有init方法就调用： 一般不需要init方法，直接在构造函数中初始化即可
						module && module.init && module.init($form);
					}
				});
			}

			// 加载js、css文件
		});
	},

	/** 签领任务：上下文为info样式所在的容器 */
	claimTask: function (taskId) {
		// 表单验证
		var $task = $(this);

		bc.msg.confirm("确定签领此任务吗？", function () {
			jQuery.ajax({
				url: bc.root + "/bc-workflow/workflow/claimTask?id=" + taskId,
				dataType: "json",
				success: function (json) {
					if (json.success) {
						bc.msg.slide(json.msg);

						// 刷新边栏
						bc.sidebar.refresh();

						// 关闭自己
						var $page = $task.closest(".bc-page");
						$page.data("data-status", true);
						$page.dialog("close");

						// 打开工作空间
						bc.flow.openWorkspace({id: json.pId, name: json.name});
					} else {
						bc.msg.alert(json.msg);
					}
				}
			});
		});
		// alert("TODO:签领任务：taskId=" + taskId);
	},

	/** 委托任务 */
	delegateTask: function (taskId) {
		// 表单验证
		var $task = $(this);

		// 选择委托人
		bc.identity.selectUser({
			history: false,
			onOk: function (user) {
				jQuery.ajax({
					url: bc.root + "/bc-workflow/workflow/delegateTask",
					data: {id: taskId, toUser: user.account},
					dataType: "json",
					success: function (json) {
						if (json.success) {
							bc.msg.alert(json.msg);

							// 刷新边栏
							bc.sidebar.refresh();

							// 关闭自己
							var $page = $task.closest(".bc-page");
							$page.data("data-status", true);
							$page.dialog("close");
						} else {
							bc.msg.alert(json.msg);
						}
					}
				});
			}
		});

		// alert("TODO:委派任务：taskId=" + taskId);
	},

	/** 完成办理 */
	finishTask: function (procinstId, taskId) {
		// 表单验证:
		// 1) 如果验证返回false,方法将立即返回
		// 2) 如果验证返回true,让用户确定是否继续完成办理
		// 3) 如果验证返回字符串，就用该字符串提示用户并让用户确定是否继续完成办理
		var $task = $(this);
		var $form = $task.find(".line>.form>:first-child");
		var namespace = $form.attr("data-namespace");
		var validateResult;
		if ($form.size() > 0) {
			bc.flow.workspace.validateForm($form, namespace, procinstId, taskId, function (validateResult) {
				if (!validateResult) return false;

				// 获取表单数据
				var formData = null;
				if ($form.size() > 0) {
					formData = bc.flow.workspace.getFormData($form, namespace);
				}

				var vr;
				var reOpen = false;
				if (typeof validateResult == "string") {
					//当返回值的类型为字符串，并且是“REOPEN”时，点击完成任务后将会重新打开此流程空间
					if (validateResult == "REOPEN") {
						vr = "确定要完成此任务的办理吗？"
						reOpen = true;
					} else {
						vr = validateResult;
					}
				} else {
					vr = "确定要完成此任务的办理吗？"
				}

				bc.msg.confirm(vr, function () {
					// alert($form.serialize());
					// 完成办理
					jQuery.ajax({
						url: bc.root + "/bc-workflow/workflow/completeTask?id=" + taskId,
						data: formData ? {formData: $.toJSON(formData)} : null,
						dataType: "json",
						success: function (json) {
							if (json.success) {
								bc.msg.slide(json.msg);

								// 刷新边栏
								bc.sidebar.refresh();

								// 关闭自己
								var $page = $task.closest(".bc-page");
								$page.data("data-status", true);
								$page.dialog("close");

								if (reOpen)
									bc.flow.openWorkspace({id: procinstId, name: "工作空间"});
							} else {
								bc.msg.alert(json.msg);
							}
						}
					});
				});

			});

		} else {//任务没有设置form表单文件时 调用此JS
			bc.msg.confirm((typeof validateResult == "string" ? validateResult : "确定要完成此任务的办理吗？"), function () {
				// alert($form.serialize());
				// 完成办理
				jQuery.ajax({
					url: bc.root + "/bc-workflow/workflow/completeTask?id=" + taskId,
					data: null,
					dataType: "json",
					success: function (json) {
						if (json.success) {
							bc.msg.slide(json.msg);

							// 刷新边栏
							bc.sidebar.refresh();

							// 关闭自己
							var $page = $task.closest(".bc-page");
							$page.data("data-status", true);
							$page.dialog("close");
						} else {
							bc.msg.alert(json.msg);
						}
					}
				});
			});
		}
	},

	/**
	 * 发起子流程
	 * @param {String} taskId 发起子流程的任务Id
	 * @param {String} flowCode 流程编码
	 * @param {functioin} callback 子流程发起成功的回调函数
	 */
	startSubProcess: function (taskId, flowCode, callback) {
		// $zone 是workspace的子流程待办区域
		// $zone.attr("data-namespace") 命名空间，默认会调用“命名空间+.getFormData”函数获得表单内容
		var $zone = $(this);
		var namespace = $zone.attr("data-namespace");
		var validateResult;
		if ($zone.size() < 0)
			return;

		bc.flow.workspace.validateForm($zone, namespace, null, taskId, function (validateResult) {
			if (!validateResult) return false;

			// 表单验证:
			// 1) 如果验证返回false,方法将立即返回
			// 2) 如果验证返回true,让用户确定是否继续完成办理
			// 3) 如果验证返回字符串，就用该字符串提示用户并让用户确定是否继续完成办理
			var vr = (typeof validateResult == "string")
				? validateResult : "确定要完成此任务的办理吗？";

			// 获取表单数据
			var zoneData = null;
			if ($zone.size() > 0)
				zoneData = bc.flow.workspace.getFormData($zone, namespace);

			bc.msg.confirm(vr, function () {
				// 发起子流程
				jQuery.ajax({
					url: bc.root + "/bc-workflow/workflow/startSubProcess",
					data: {
						id: taskId,
						key: flowCode,
						formData: zoneData ? $.toJSON(zoneData) : null
					},
					dataType: "json",
					success: function (json) {
						if (json.success) {
							bc.msg.slide(json.msg);

							// 刷新边栏
							bc.sidebar.refresh();

							// 调用外部的回调函数
							if (typeof callback == "function")
								callback.call($zone, json);
						} else {
							bc.msg.alert(json.msg);
						}
					}
				});
			});
		});
	},

	/** 分派任务 */
	assignTask: function (taskId) {
		// 表单验证
		var $task = $(this);

		// 选择分派人
		bc.flow.selectUser({
			taskId: taskId,
			onOk: function (user) {
				jQuery.ajax({
					url: bc.root + "/bc-workflow/workflow/assignTask",
					data: {id: taskId, toUser: user.account},
					dataType: "json",
					success: function (json) {
						if (json.success) {
							bc.msg.alert(json.msg);

							// 刷新边栏
							bc.sidebar.refresh();

							// 关闭自己
							var $page = $task.closest(".bc-page");
							$page.data("data-status", true);
							$page.dialog("close");
						} else {
							bc.msg.alert(json.msg);
						}
					}
				});
			}
		});
		// alert("TODO:分配任务：taskId=" + taskId);
	},

	/**
	 * 查看意见
	 *
	 * @desc 上下文为'.info'的jquery对象
	 * @param {String} commentId - 意见ID
	 */
	openComment: function (commentId) {
		bc.flowattach.open({id: commentId});
	},

	/**
	 * 编辑意见
	 *
	 * @desc 上下文为'.info'的jquery对象
	 * @param {String} commentId - 意见ID
	 */
	editComment: function (commentId) {
		var $container = this;
		bc.flowattach.edit({
			type: 2,
			id: commentId,
			onOk: function (json) {
				// alert($.toJSON(json));
				$container.find(">.simple>.line>.text").text(json.subject);
				var $desc = $container.find(">.detail>.line.desc");
				$desc.find(">pre.text").html(json.desc);
				$desc.toggleClass("hide", (!json.desc || json.desc.length == 0));
			}
		});
	},

	/**
	 * 添加意见
	 *
	 * @desc 上下文为意见列表容器的jquery对象
	 * @param {String}
	 *            targetType 容器类型：common-公共信息容器，todo-待办信息容器
	 * @param {String}
	 *            pid 流程实例id
	 * @param {String}
	 *            tid 任务实例id
	 */
	addComment: function (targetType, pid, tid) {
		var $container = this;
		bc.flowattach.create({
			type: 2,
			common: (targetType == "common"),
			pid: pid,
			tid: tid,
			onOk: function (json) {
				// alert($.toJSON(json));
				var simpleLine = bc.flow.workspace.TPL.LINE.format("comment", "ui-icon-comment", "link", json.subject, bc.flow.workspace.TPL.COMMENT_BUTTONS);
				var detail_descLine = bc.flow.workspace.TPL.DESC_LINE.format(!json.desc || json.desc.length == 0 ? "hide" : "", json.desc);
				var detail_authorLine = bc.flow.workspace.TPL.TEXT_LINE.format("low little", json.author + " " + json.fileDate);
				var info = bc.flow.workspace.TPL.INFO.format(json.id, "", "", "", simpleLine, detail_descLine + detail_authorLine, "", "comment");

				if (targetType == "common") {// 公共信息：插在统计信息前
					$container.children(":last").before(info);
				} else if (targetType == "todo") {// 待办信息：插到普通信息前
					$container.children(".normalFirst").before(info);
				} else {
					alert("不支持的容器类型");
				}
			}
		});
	},

	/**
	 * 查看附件
	 *
	 * @desc 上下文为'.info'的jquery对象
	 * @param {String}
	 *            attachId 附件ID
	 */
	openAttach: function (attachId) {
		bc.flowattach.inline({
			id: attachId,
			subject: this.data("subject"),
			path: this.data("path")
		});
	},

	/**
	 * 编辑附件
	 *
	 * @desc 上下文为'.info'的jquery对象
	 * @param {String}
	 *            attachId 附件ID
	 */
	editAttach: function (attachId) {
		var $container = this;
		bc.flowattach.edit({
			type: 1,
			id: attachId,
			onOk: function (json) {
				// alert($.toJSON(json))
				$container.find(">.simple>.line>.text").text(json.subject);
			}
		});
	},

	/**
	 * 添加附件
	 *
	 * @desc 上下文为附件列表容器的jquery对象
	 * @param {String}
	 *            targetType 容器类型：common-公共信息容器，todo-待办信息容器
	 * @param {String}
	 *            pid 流程实例id
	 * @param {String}
	 *            tid 任务实例id
	 */
	addAttach: function (targetType, pid, tid) {
		var $container = this;
		bc.flowattach.create({
			type: 1,
			common: (targetType == "common"),
			pid: pid,
			tid: tid,
			onOk: function (json) {
				// alert($.toJSON(json));
				var simpleLine = bc.flow.workspace.TPL.LINE.format("attach", "ui-icon-link", "link", json.subject, bc.flow.workspace.TPL.ATTACH_BUTTONS);
				var detailLine = bc.flow.workspace.TPL.TEXT_LINE.format("low little", json.author + " " + json.fileDate);
				var info = bc.flow.workspace.TPL.INFO.format(json.id, json.subject, json.size, json.path, simpleLine, detailLine, "", "attach");

				if (targetType == "common") {// 公共信息：插在意见和统计信息前
					$container.children(".comment,.stat").filter(":first").before(info);
				} else if (targetType == "todo") {// 待办信息：插在意见和普通信息前
					$container.children(".comment,.normalFirst").filter(":first").before(info);
				} else {
					alert("不支持的容器类型");
				}
			}
		});
	},

	/** 表单验证 */
	validateForm: function ($form, namespace, procinstId, taskId, callback) {
		var scope = $form.data("scope");
		var method, result;
		if (scope) {        // 新的 requireJs 模块方式
			method = typeof scope === "object" ? scope["validateForm"] : null;
			if (typeof method == "function") {// 自定义表单验证方法
				result = method.apply(scope, arguments);
			} else {// 默认的表单验证方法
				result = bc.validator.validate($form);
			}
			callback && callback.call($form, result);
		}else{              // 旧的代码
			if (namespace) {
				method = namespace + ".validateForm";
				//logger.debug("validateMethod=" + method);
				method = bc.getNested(method);
				if (typeof method == "function") {  // 自定义表单验证方法
					//新的验证方法
					result = method.apply($form, arguments);

					//兼容旧的自定义验证方法
					if (result === true || result === false || typeof result == "string") {
						callback.call($form, result);
					}
				} else {        // 默认的表单验证方法
					//logger.debug("use default validate because undefined function: " + namespace + ".validateForm");
					result = bc.validator.validate($form);
					callback && callback.call($form, result);
				}
			}else{  // 默认的表单验证方法
				result = bc.validator.validate($form);
				callback && callback.call($form, result);
			}
		}
	},

	/** 获取表单数据 */
	getFormData: function ($form, namespace) {
		var scope = $form.data("scope");
		var method, result;
		if (scope) {        // 新的 requireJs 模块方式
			method = typeof scope === "object" ? scope["getFormData"] : null;
			if (typeof method == "function") {// 自定义方法
				result = method.apply(scope, arguments);
			} else {// 默认的方法
				result = getFormDataByDefaultWay($form);
			}
		}else {              // 旧的代码
			if (namespace) {
				method = namespace + ".getFormData";
				logger.debug("getFormData Method=" + method);
				method = bc.getNested(method);
				if (typeof method == "function") {
					result = method.apply($form, arguments);// 自定义方法
				} else {
					result = getFormDataByDefaultWay($form);
				}
			}else{  // 默认的方法
				result = getFormDataByDefaultWay($form);
			}
		}
		return result;

		// 默认的表单数据获取方法
		function getFormDataByDefaultWay($form) {
			var $inputs = $form.find(":input:not(.ignore)").not(".radios [type=radio], .checkboxes [type=checkbox]");
			var $rcs = $form.find(".radios, .checkboxes");
			if ($inputs.size() + $rcs.size() == 0) return false;

			var data = [];
			$inputs.each(function () {
				var $input = $(this);
				data.push({
					name: this.name,
					value: $input.val(),
					type: $input.attr("data-type") || "string",
					scope: $input.attr("data-scope") || "local"
				});
			});

			// 处理用 .radios、.checkboxes 容器标记分组的单选、多选框的数据
			$rcs.each(function () {
				var $rc = $(this);
				var $ms = $rc.find(":checked");// 收集被选中的单选、多选框

				// 没有选中任何值时不设置流程变量
				if ($ms.size() == 0) {
					console.log("nothing was checked for " + $rc.attr("class"));
					return;
				}

				// 生成选中值的流程变量数据
				// -- 流程变量的名称为容器的 data-name 属性值，如果没有定义则使用其内第一个控件的 name 属性值
				var name = $rc.attr("data-name") || $rc.find("[type=checkbox]:eq(0),[type=radio]:eq(0)").attr("name");
				var value;
				if ($rc.is(".checkboxes")) {     // 多选框：多个选中值用逗号连接在一起
					value = $ms.map(function () {
						return this.value;
					}).join(",");
				} else {                        // 单选框
					value = $ms.val();
				}
				data.push({
					name: name,
					value: value,
					type: $rc.attr("data-type") || "string",
					scope: $rc.attr("data-scope") || "local"
				});
			});

			return data;
		}
	},

	/** 重新排序已办信息区的内容 */
	reorderDones: function (data) {
		// 创建通用的排序函数
		if (!$.fn.sorted) {
			$.fn.sorted = function (customOptions) {
				var options = {
					reversed: false,
					by: function (a) {
						return a.text();
					}
				};
				$.extend(options, customOptions);
				var $data = $(this);
				var arr = $data.get();
				arr.sort(function (a, b) {
					var valA = options.by($(a));
					var valB = options.by($(b));
					if (options.reversed) {
						return (valA < valB) ? 1 : (valA > valB) ? -1 : 0;
					} else {
						return (valA < valB) ? -1 : (valA > valB) ? 1 : 0;
					}
				});
				return $(arr);
			};
		}

		logger.info("reorderDones:" + $.toJSON(data));
		var $page = $(this);
		var $done = $page.children(".done");
		var $header = $done.children(".header");
		var $items = $done.children(".items");

		// 获取已办任务列表
		var $sortedTasks = $items.clone().children('.info');

		// 对已办任务进行排序
		var type = $header.find(">.order.type>.ui-state-active").attr("data-value");
		$sortedTasks = $sortedTasks.sorted({
			reversed: $header.find(">.order.dir>.ui-state-active").attr("data-value") != "true",
			by: function (v) {
				return $(v).attr('data-' + type);
			}
		});

		// 执行排序处理
		$items.quicksand($sortedTasks, {
			duration: 0,
			easing: 'easeInOutQuad'
		});
	}
};

/** 页面模板 */
bc.flow.workspace.TPL = {};
bc.flow.workspace.TPL.ITEM_BUTTON = '<span class="itemOperate {0}"><span class="ui-icon {2}"></span><span class="text link">{1}</span></span>';
bc.flow.workspace.TPL.ITEM_BUTTON_EDIT = bc.flow.workspace.TPL.ITEM_BUTTON.format("edit", "编辑", "ui-icon-pencil");
bc.flow.workspace.TPL.ITEM_BUTTON_OPEN = bc.flow.workspace.TPL.ITEM_BUTTON.format("open", "查看", "ui-icon-document-b");
bc.flow.workspace.TPL.ITEM_BUTTON_DOWNLOAD = bc.flow.workspace.TPL.ITEM_BUTTON.format("download", "下载", "ui-icon-arrowthickstop-1-s");
bc.flow.workspace.TPL.ITEM_BUTTON_DELETE = bc.flow.workspace.TPL.ITEM_BUTTON.format("delete", "删除", "ui-icon-closethick");

bc.flow.workspace.TPL.COMMENT_BUTTONS = bc.flow.workspace.TPL.ITEM_BUTTON_EDIT + bc.flow.workspace.TPL.ITEM_BUTTON_OPEN + bc.flow.workspace.TPL.ITEM_BUTTON_DELETE;
bc.flow.workspace.TPL.ATTACH_BUTTONS = bc.flow.workspace.TPL.ITEM_BUTTON_EDIT + bc.flow.workspace.TPL.ITEM_BUTTON_OPEN + bc.flow.workspace.TPL.ITEM_BUTTON_DOWNLOAD + bc.flow.workspace.TPL.ITEM_BUTTON_DELETE;

bc.flow.workspace.TPL.LINE = [
	'<div class="line {0}">'
	, '	<span class="leftIcon ui-icon {1}"></span>'
	, '	<span class="text {2}">{3}</span>'
	, '	<span class="rightIcons">{4}'
	, '		<span class="toggle"><span class="ui-icon ui-icon-carat-1-ne" title="折叠|展开详细信息"></span></span>'
	, '	</span>'
	, '</div>'
].join("");

bc.flow.workspace.TPL.DESC_LINE = [
	'<div class="line desc {0}">'
	, '	<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>'
	, '	<pre class="ui-widget-content text">{1}</pre>'
	, '</div>'
].join("");

bc.flow.workspace.TPL.TEXT_LINE = [
	'<div class="line {0}">'
	, '	<span class="leftIcon ui-icon ui-icon-carat-1-e"></span>'
	, '	<span class="text">{1}</span>'
	, '</div>'
].join("");

bc.flow.workspace.TPL.INFO = [
	'<div class="info ui-widget-content {7}" data-id="{0}" data-subject="{1}" data-size="{2}" data-path="{3}">'
	, '	<div class="simple">{4}</div>'
	, '	<div class="detail {6}">{5}</div>'
	, '</div>'
].join("");

