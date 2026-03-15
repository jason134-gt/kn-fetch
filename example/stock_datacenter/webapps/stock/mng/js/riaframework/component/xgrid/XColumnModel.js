Ext.namespace('MyKit.widget');
MyKit.widget.XColumnModel = function(config){
	// call parent constructor
	MyKit.widget.XColumnModel.superclass.constructor.call(this, config);
}
Ext.extend(MyKit.widget.XColumnModel, Ext.grid.ColumnModel, {
	getCellComponent : function(colIndex, rowIndex){
        return this.config[colIndex].component;
    },
    
    getCellButtons : function(colIndex, rowIndex){
        return this.config[colIndex].buttons;
    }

});
