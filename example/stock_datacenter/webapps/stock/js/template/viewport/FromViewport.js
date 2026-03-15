Ext.namespace('MyKit.widget');
MyKit.widget.FromViewPort = function(config) {
	//初始化form的buttons
	var length = config.formConfig.buttons.length;
	for (var index = 0; index < length; index++) {
		config.formConfig.buttons[index].handler = this[config.formConfig.buttons[index].handler];
		config.formConfig.buttons[index].scope = this;
	}
	MyKit.widget.FromViewPort.superclass.constructor.call(this, config);
}
Ext.extend(MyKit.widget.FromViewPort, Ext.Viewport, {
	id : 'fromViewPort',
	layout : "fit",
	formpanel : null,
	// ========= 业务页面配置 start ===========
	// formConfig中包含{items,buttons}
	formConfig : null,
	addConfig : null,
	loadConfig : null,
	// ========= 业务页面配置项 end ===========
	initComponent : function() {
		MyKit.widget.FromViewPort.superclass.initComponent.call(this);
		this.formpanel = new MyKit.widget.DefaultXForm({
					components : this.formConfig.items,
					buttons : this.formConfig.buttons
				});

		this.add(this.formpanel);
		if(this.loadConfig!=null){
			this.formpanel.load({url:this.loadConfig.url,params:this.loadConfig.params});		
		}

	},
	// =================================================================
	addHandler : function() {
		var formpanel = Ext.getCmp("formpanel");
		formpanel.getForm().submit({
					clientValidation : true,
					url : this.addConfig.url,
					params : this.addConfig.params,
					success : function(form, action) {
						Ext.Msg.alert('提示信息', "添加成功！", function() {
								});
					},
					failure : function(form, action) {
						switch (action.failureType) {
							case Ext.form.Action.CLIENT_INVALID :
								Ext.Msg.alert('提示信息', '客户端校验不通过！');
								break;
							case Ext.form.Action.CONNECT_FAILURE :
								Ext.Msg.alert('提示信息', //链接失败
										'连接服务端失败，请重新打开页面！');
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
	loadHandler : function() {
		var formpanel = Ext.getCmp("formpanel");
	 	formpanel.load({
			url : this.updateConfig.url,
			params : this.loadConfig.params
		});
	}


});
