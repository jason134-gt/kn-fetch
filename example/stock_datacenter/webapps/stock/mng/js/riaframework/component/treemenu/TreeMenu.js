// Create user extensions namespace (MyKit.widget)
Ext.namespace('MyKit.widget');

/**
 * MyKit.widget.TreeMenu Extension Class
 * 树形样式的菜单，支持多级菜单显示，数据来源于服务器端返回的JSON格式数据，菜单点击后的动作需要由用户自行实现回调函数。
 * 
 * 
 */

MyKit.widget.TreeMenu = function(config) {
	// call parent constructor
	MyKit.widget.TreeMenu.superclass.constructor.call(this, config);
	this.addEvents("menuclick");

}; // end of MyKit.widget.TreeMenu constructor

// extend
Ext.extend(MyKit.widget.TreeMenu, Ext.tree.TreePanel, {
	/**
	 * @cfg {Store} url 服务器端提供菜单数据的URL地址
	 */
	// url : '../CommMenuServlet',
	// html:"<div id='spreg'></div>",
	/**
	 * @cfg {Store} ds 服务端返回的菜单数据集
	 */
	ds : null,
	/**
	 * @cfg {Boolean} 是否允许拖拽菜单
	 */
	enableDD : false,
	/**
	 * @cfg {Boolean} 是否根节点可见
	 */
	rootVisible : true,

	/**
	 * @cfg {Boolean} 是否允许自动出现滚动条
	 */
	autoScroll : true,
	/**
	 * @cfg {Boolean} 是否预先加载子节点
	 */
	preloadChildren : true,
	/**
	 * @cfg {Boolean} 是否折叠根节点
	 */
	collapseFirst : false,

	// 以下为新增加的属性
	/**
	 * @cfg {String} 根节点的名称
	 */
	rootText : 'root',
	/**
	 * @cfg {Boolean} 是否折叠根节点
	 */
	rootExpanded : true,
	/**
	 * @cfg {function} menuHandler 点击菜单的回调函数
	 */
	menuHandler : MenuUtil.handleItemClick,

	/**
	 * 初始化组件
	 */
	initComponent : function() {
		MyKit.widget.TreeMenu.superclass.initComponent.call(this);
		this.on("click", this.handleOnClick);
	},
	
	/**
	 * 设置菜单项点击后的回调函数
	 * 
	 * @param {function}
	 *            f 回调函数
	 */
	setHandler : function(f) {
		this.menuHandler = f;
		this.on("menuclick", f);
	},
	
	/**
	 * 处理菜单点击事件

	 * 
	 * @param {TreeNode} node 点击的节点
	 * @param {Event} e 事件
	 *            
	 */
	handleOnClick : function(node,e){
		e.stopEvent();
		if(node.leaf || (!node.leaf && node.attributes.url)){
		 this.fireEvent("menuclick", node.attributes,e);
		}
	},
	/**
	 * 定位到指定的菜单
	 * 
	 * @param {function}
	 *            完整的菜单路径，使用各个菜单节点的id，以/作为分隔符，如/menu1/menu11/menu112
	 */
	locate : function(path) {
		if (path) {
			this.expendPath(path);
		}
		return;
	},

	/**
	 * 加载json菜单数据并构造出完整菜单
	 */
	loadData : function() {
		var self = this;
		// 注册点击事件
		this.on("click", this.menuHandler);
		// 构造根节点
		/*
		 * var root = new Ext.tree.AsyncTreeNode({ text : self.rootText, id :
		 * 'root', expanded : self.rootExpanded }); this.setRootNode(root);
		 */
		if (this.el) {

			this.render();

		}

	},

	/**
	 * 重新刷新菜单
	 */
	refresh : function() {
		this.getRootNode().reload();
	}

}); // end of extend

// end of file
// 注册xtype
Ext.reg('treemenu', MyKit.widget.TreeMenu);

/**
 * 转换函数，转换为extjs规范的json object
 * 
 * @param {Object}
 *            XML转换后的JSON Object *
 */
//ConvertTreeMenu = function(o) {
//	// 调用TreePanel的转换//
//	ConvertTreePanel(o);
//	if (o.rootExpanded && o.rootExpanded == "false") {
//		o.rootExpanded = false;
//	} else {
//		o.rootExpanded = true;
//	}
//
//	if (o.menutreeloader) {
//		o.loader = new MyKit.widget.MenuTreeLoader(o.menutreeloader);
//	}
//	if(o.menuHandler){
//		o.menuHandler=eval(o.menuHandler);
//	}
//
//}
//
//// 注册转换函数
//ConvertMgr.regConverter("treemenu", ConvertTreeMenu);
