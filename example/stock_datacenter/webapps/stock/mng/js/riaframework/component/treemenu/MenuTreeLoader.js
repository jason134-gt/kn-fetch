// Create user extensions namespace (MyKit.widget)
Ext.namespace('MyKit.widget');


/**
 * MyKit.widget.MenuTreeLoader Extension Class
 * 提供从某个url加载某个节点的子节点功能，扩展了对服务器端返回的数据的解析方法
 * 
 * @author gongjt
 * @version 1.0
 * 
 * @class MyKit.widgetMenuTreeLoader
 * @extends Ext.Panel
 * @constructor
 * @param {Object}
 *            config Configuration options
 */
MyKit.widget.MenuTreeLoader = function(config) {
	firstPath : 'tt',
	// call parent constructor
	MyKit.widget.MenuTreeLoader.superclass.constructor.call(this, config);

}; // end of MyKit.widgetMenuTreeLoader constructor

// extend
Ext.extend(MyKit.widget.MenuTreeLoader, Ext.tree.TreeLoader, {
	
	/**
	 * @cfg {Boolean} 是否显示自定义icon
	 */
	displayIcon : "false",
	
	/**
	 * 处理返回的json数据，并完全加载第一个子节点的数据
	 * 
	 * @param {Object}
	 *            response The XHR object containing the response data. See The
	 *            XMLHttpRequest Object for details.
	 * @param {Ext.data.Node}
	 *            node 节点对象
	 * @param {Function}
	 *            callback 回调函数 function的定义需要符合下列规范 function : (n) n :
	 *            Ext.data.Node，节点对象
	 * 
	 * 
	 */
	processResponse : function(response, node, callback) {
		var json = response.responseText;
		try {
			var o = eval("(" + json + ")");
			o = o.data;
			node.beginUpdate();
            node.beginUpdate();
            for(var i = 0, len = o.length; i < len; i++){
                var n = this.createNode(o[i]);
                if(n){
                    node.appendChild(n);
                }
            }
            node.endUpdate();
            if(typeof callback == "function"){
                callback(this, node);
            }
            //添加结点加载完成事件处理函数
            this.on("load",this.expandchildNodes);
        }catch(e){
            this.handleFailure(response);
        }

	},
	
	/**
	 * 如果needExpand为true，expand结点下的所有结点
	 */
	expandchildNodes: function(scope,node){
		 var cs = node.childNodes;
		 for(var i = 0; i < cs.length; i++){
            	if(!cs[i].expanded && cs[i].attributes.needExpand){
            		cs[i].expand(false,false);
            		scope.expandchildNodes(scope,cs[i]);
            	}
         }
	},
	
	    /**
    * Override this function for custom TreeNode node implementation
    */
    createNode : function(attr){
        // apply baseAttrs, nice idea Corey!
    	
        if(this.baseAttrs){
            Ext.applyIf(attr, this.baseAttrs);
        }
        //attr.icon = Ext.BLANK_IMAGE_URL;
        if(this.displayIcon=="false" && attr.icon){
        	delete attr.icon;
        }
        if(this.applyLoader !== false){
            attr.loader = this;
        }
        if(typeof attr.uiProvider == 'string'){
           attr.uiProvider = this.uiProviders[attr.uiProvider] || eval(attr.uiProvider);
        }
        attr.singleClickExpand = true;
        return(attr.leaf ?
                        new Ext.tree.TreeNode(attr) :
                        new Ext.tree.AsyncTreeNode(attr));
    },
    
	/**
	 * 得到第一个二级菜单的路径
	 * 
	 */
	getFirstPath : function() {
		return this.firstPath;
	}
}); // end of extend
// end of file
//注册xtype
Ext.reg('menutreeloader', MyKit.widget.MenuTreeLoader);

/**
 * 转换函数，转换为extjs规范的json object
 * 
 * @param {Object}
 *            XML转换后的JSON Object *
 */
//ConvertMenuTreeLoader = function(o) {
//	//调用父类的converter
//	if (o.displayIcon && o.displayIcon == "false") {
//		o.displayIcon = false;
//	} else {
//		o.displayIcon = true;
//	}
//}
//
//// 注册转换函数
//ConvertMgr.regConverter("menutreeloader",ConvertMenuTreeLoader);
