Ext.namespace('MyKit.widget');
/**
 * MyKit.widget.XGridPanel Extension Class
 * 
 *
 */

MyKit.widget.XGridPanel = function(config) {
    // call parent constructor
	MyKit.widget.XGridPanel.superclass.constructor.call(this, config);
}; 

// extend
Ext.extend(MyKit.widget.XGridPanel, Ext.grid.EditorGridPanel, {
	viewConfig : {
		//默认参数   
		forceFit : true
	},

	/**
	 * Returns the grid's ComplexGridView object.
	 * @return {ComplexGridView} The grid view
	 */
	getView : function() {
		if (!this.view) {			
			this.view = new MyKit.widget.XGridView(this.viewConfig);
		}
		
		return this.view;
	},
	
	//修改完成后
//	onEditComplete : function(ed, value, startValue){	
//    	MyKit.widget.XGridPanel.superclass.onEditComplete.call(this,ed,value,startValue);
//    	if(value!=startValue){
//	       this.view.grid.stopEditing();
//	       //重新绘制当前行
//	       this.view.processRows(ed.row, false,true);
//    	}	
//    }
}); // end of extend

Ext.reg('xgrid', MyKit.widget.XGridPanel);
