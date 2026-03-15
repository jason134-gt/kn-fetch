Ext.namespace('MyKit.widget');
//初始化对象 begin==============================================================
	Ext.QuickTips.init();  //浮动提示信息init
	var mk = new Ext.LoadMask(Ext.getBody(), {msg:"正在处理中,请稍候.."});
	var cusmShowAlert = function(myMsg){  //自定义提示信息框
						Ext.Msg.show({
						 title : '错误提示信息',
						 msg : myMsg,
						 buttons : Ext.Msg.OK,
						 icon : Ext.MessageBox.ERROR
					});	
	};

	//Ext.Msg.alert('提示信息', jsonObj.error.msg);
	//var  ret = Ext.Msg.wait('正在处理中,请稍候..');  

//初始化对象 end==============================================================
MyKit.widget.FromGridViewPort = function(config) {
	//初始化form的buttons
	var length = config.formConfig.buttons.length;
	for (var index = 0; index < length; index++) {
		config.formConfig.buttons[index].handler = this[config.formConfig.buttons[index].handler];
		config.formConfig.buttons[index].scope = this;
	}
    //初始化grid的cm中的buttons
//	var cmLength = config.gridConfig.cm.length;
//	for (var cmIndex = 0; cmIndex < length; cmIndex++) {
//		if(config.gridConfig.cm[cmIndex].buttons){
//			var bLength = config.gridConfig.cm[cmIndex].buttons.length;
//			for(bIndex =0;bIndex < bLength;bLength++){
//				config.formConfig.buttons[index].handler = this[config.gridConfig.cm[cmIndex].buttons[]];
//		        config.formConfig.buttons[index].scope = this;
//			}
//		}
//	}
	
	MyKit.widget.FromGridViewPort.superclass.constructor.call(this, config);
}
Ext.extend(MyKit.widget.FromGridViewPort, Ext.Viewport, {
	id : 'fromGridViewPort',
	layout : "fit",
	formpanel : null,
	gridPanel : null,
	gridstore : null,
	// ========= 业务页面配置 start ===========
	// formConfig中包含{items,buttons}
	formConfig : null,
	// gridConfig中包含{cm,fields,storeUrl}
	gridConfig : null,
	addConfig : null,
	// ========= 业务页面配置项 end ===========
	initComponent : function() {
		MyKit.widget.FromGridViewPort.superclass.initComponent.call(this);
		 var dxForm = {
					region : "north",
					components : this.formConfig.items,
					buttons : this.formConfig.buttons
				};
		if (!Ext.isEmpty(this.maxCols)){  
			dxForm.maxCols = this.maxCols;//自定义列表,默认值在xForm.sj
			dxForm.lw =  this.lw;//自定义LabelWidth,,默认值在xForm.sj
		}
		
		this.formpanel = new MyKit.widget.DefaultXForm(dxForm);
		this.gridstore = new Ext.data.JsonStore({
					id : 'gridStore',
					url : this.gridConfig.storeUrl,
					totalProperty : 'totalCount',
					root : 'data',
					fields : this.gridConfig.fields,
					remoreSort : true
				});

		this.gridPanel = new MyKit.widget.DefaultGridPanel({
					cm : new MyKit.widget.XColumnModel(this.gridConfig.cm),
					store : this.gridstore,
					sm : this.gridConfig.cm[1],
					bbar : new Ext.PagingToolbar({
								pageSize : 50,
								store : this.gridstore,
								displayInfo : true,
								plugins : new Ext.ux.ProgressBarPager()
							}),
					buttons : [{
								text : '删除',
								handler : this.batchDeleteHandler,
								scope : this
							}, {
								text : '启用',
								handler : this.batchEnableHandler,
								scope : this
							}]
				});

		var gridPanelWraper = new Ext.Panel({
					layout : 'fit',
					region : 'center',
					items : [this.gridPanel]
				});
		var contentPanel = new Ext.Panel({
					id : 'contentPanel',
					layout : 'border',
					autoScroll : true,
					items : [this.formpanel, gridPanelWraper]
				});
		this.add(contentPanel);
		this.gridstore.load();
	},
	// =================================================================
	singleDelete : function() {
		var grid = Ext.getCmp("grid");
		var rowdata = this.record.data;
		Ext.Msg.confirm('删除', '确定要删除这条记录？', function(btn, text) {
					if ('yes' == btn) {
						Ext.Ajax.request({
									url : window.CTXPATH
											+ '/picture/delete.action',
									success : function() {
										var grid = Ext.getCmp("grid");
										grid.store.reload();
									},
									params : {
										'picture.id' : rowdata.id
									},
									scope : this
								});
					}
				}, this);
	},

	addHandler : function() {
		var formpanel = Ext.getCmp("formpanel");
		formpanel.getForm().submit({
					clientValidation : true,
					url : this.addConfig.url,
					params : this.addConfig.params,
					success : function(form, action) {
						Ext.Msg.alert('提示信息', "添加成功！", function() {
									var grid = Ext.getCmp("grid");
									grid.store.reload();
								});
					},
					failure : function(form, action) {
						switch (action.failureType) {
							case Ext.form.Action.CLIENT_INVALID :
								Ext.Msg.alert('提示信息', '校验不通过,请认真填写红色部份！');
								break;
							case Ext.form.Action.CONNECT_FAILURE :
								Ext.Msg.alert('提示信息',
										'远程通迅失败,请检查网络是否正常！');
								break;
							case Ext.form.Action.SERVER_INVALID :
								Ext.Msg.alert('提示信息', action.result.msg);
						}
					}
				});
	},
	updateHandler : function() {
		var formpanel = Ext.getCmp("formpanel");
		formpanel.getForm().submit({
					clientValidation : true,
					url : this.updateConfig.url,
					params : this.updateConfig.params,
					success : function(form, action) {
						Ext.Msg.alert('提示信息', "修改成功！", function() {
									var grid = Ext.getCmp("grid");
									grid.store.reload();
								});
					},
					failure : function(form, action) {
						switch (action.failureType) {
							case Ext.form.Action.CLIENT_INVALID :
								Ext.Msg.alert('提示信息', '客户端校验不通过！');
								break;
							case Ext.form.Action.CONNECT_FAILURE :
								Ext.Msg.alert('提示信息',
										'连接服务端失败，请重新打开页面！');
								break;
							case Ext.form.Action.SERVER_INVALID :
								Ext.Msg.alert('提示信息', action.result.msg);
						}
					}
				});
	},
	batchDeleteHandler : function() {	
		var grid = Ext.getCmp("grid");
		var rs = grid.selModel.getSelections();
		if (0 == rs.length) {
			Ext.Msg.alert("提示信息", "没有选中任何记录");
			return;
		}
		var idArray = [];
		for (var p in rs) {
			if ('function' == typeof rs[p]) {
				continue;
			}
			idArray.push(rs[p].get("id"));
		}

		var ids = idArray.join("|");
		/*/Ext.Msg.alert("提示信息", ids);
		return;
		*/
		Ext.Msg.confirm('删除', '确定要批量删除记录？', function(btn, text) {
					if ('yes' == btn) {
						Ext.Ajax.request({
									url : this.deleteConfig.url,
									success : function(response,options) {
								    var jsonObj = Ext.util.JSON.decode(response.responseText);//String转为json对象
									if (!Ext.isEmpty(jsonObj.error.msg)){
										cusmShowAlert(jsonObj.error.msg);
									} //if end
									//为什么删除错误还要刷新数据,因为可能前面选择的记录已经为被删除
									var grid = Ext.getCmp("grid");
									grid.store.reload();
							
									},
									params : {
										ids : ids
									},
									scope : this
								});
					}
				}, this);

	},
	selectHandler : function() {
		var formpanel = Ext.getCmp("formpanel");
		var grid = Ext.getCmp("grid");
		var store = grid.getStore();
		store.baseParams=formpanel.getForm().getValues();
		store.load( {params:{start:0,limit:50}});
	},
	batchEnableHandler : function() {
		var grid = Ext.getCmp("grid");
		var rs = grid.selModel.getSelections();
		if (0 == rs.length) {
			Ext.Msg.alert("提示信息", "没有选中任何记录");
			return;
		}
		var idArray = [];
		for (var p in rs) {
			if ('function' == typeof rs[p]) {
				continue;
			}
			idArray.push(rs[p].get("id"));
		}
		var ids = idArray.join("|");
		Ext.Msg.confirm('启用', '确定要批量启用记录？', function(btn, text) {
					if ('yes' == btn) {
						Ext.Ajax.request({
									url : this.enableConfig.url,
									success : function() {
										var grid = Ext.getCmp("grid");
										grid.store.reload();
									},
									params : {
										id : ids
									},
									scope : this
								});
					}
				}, this);

	},

	previewHandler : function(c, e, key) {
		key = key || Ext.getCmp("key").getValue();
		var tail = "";
		top.right.location = ''
				+ "key=" + key + tail;
	}

});
