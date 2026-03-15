Ext.namespace('MyKit.widget');
MyKit.widget.DefaultXForm = function(config) {
	MyKit.widget.DefaultXForm.superclass.constructor.call(this, config);
}
Ext.extend(MyKit.widget.DefaultXForm, MyKit.widget.XForm, {
			id : 'formpanel',
			frame : true,
			collapsible : true, 
			autoHeight : true,
			defaultType : 'textfield'
		});