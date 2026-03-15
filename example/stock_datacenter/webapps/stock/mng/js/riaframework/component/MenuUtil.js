/**
 * MenuUtil工具类
 *
 *
 * 工具类，提供各种类型Menu共用的处理函数
 */

/**
 * 构造函数
 */
var MenuUtil = function() {
}

/**
 * 进行数据适配，创建一个二级菜单所需要的Config对象
 *
 * @param {Object}
 *            attr 服务器端返回的二级菜单JSON数据
 * @param {function}
 *            mhandler 菜单项点击的响应函数
 * @return {Button} 二级菜单所需要的Config对象
 */
MenuUtil.createSubMenu = function(attr, config, displayIcon) {
  var o = new Object();
  if (attr) {
    o.items = [];

    if (attr instanceof Array) {

      for (var i = 0, len = attr.length; i < len; i++) {
        var subMenu;
        if(displayIcon=="false" && attr[i].icon){
            delete attr[i].icon;
          }
        // 若存在下级菜单，则递归调用
        if (attr[i].children && attr[i].children.length > 0) {

          var curtMenu = attr[i];
          if(curtMenu.url){
            Ext.apply(curtMenu,config);
          }

          var sub = MenuUtil
              .createSubMenu(attr[i].children, config);

          curtMenu.menu = sub;
          delete attr[i].children;
          subMenu = curtMenu;
        } else {
          // 替换服务器端传回href菜单链接属性
          //attr[i].href = "test.jsp";
          //attr[i].menuhref = "test.jsp";
          Ext.apply(attr[i],config);
          //attr[i].handler = mhandler;
          //debuginfo(attr[i]);
          subMenu = attr[i];
        }
        o.items.push(subMenu);
      }
    } else {
          if(displayIcon=="false" && attr.icon){
            delete attr.icon;
          }
      o.items.push(attr);
    }

  }
  return o;
}

/**
 * 默认的菜单项点击的函数
 *
 * @param {Item}
 *            item 当前响应事件的菜单项
 * @param {EventObject}
 *            e 事件对象
 *
 */
MenuUtil.handleItemClick = function(item, e) {
  // alert(item.toString());
  e.stopEvent();
  debuginfo("clicked " + item.text + " " + e.target + " " + item.href);
}

// 菜单点击事件处理
MenuUtil.handleItemClick2 = function(item, e) {
  // alert(item.toString());
  e.stopEvent();
  debuginfo("clicked22 " + item.text + " " + e.target + " " + item.href);
}


MenuUtil.getIconString = function(icon,css){
    if(icon && css){
      var index = icon.lastIndexOf(".");
      var preFix = icon.substring(0,index);
      var postFix = icon.substring(index+1,icon.length);
      return preFix + css + "." + postFix;
    }else{
      return icon;
    }
}
/**
 * 默认的Tree菜单项点击的函数
 *
 * @param {Node}
 *            node 当前响应事件的菜单项
 * @param {EventObject}
 *            e 事件对象
 *
 */
MenuUtil.handleNodeClick = function(node, e) {
  // alert(item.toString());
  e.stopEvent();
  if (node instanceof Ext.tree.TreeNode) {
    // alert("true");
  }
  debuginfo("clicked " + node.text + " " + node.getPath() + " "
      + node.attributes.href);
}

/**
 * 将调试信息打印到页面上
 *
 * @param {String}
 *            s 当前响应事件的菜单项
 *
 *
 */
var debuginfo = function(s) {
    console.debug(s);
}


MenuUtil.xmlinfo =  function(c){
        var a = [];
                for(var p in c){
                  if(p!='toJSONString' &&  ((typeof p) !="function") && ((typeof p) !="object")){
                  a.push(p+"="+'"'+c[p]+'"');
                  }
                }
            debuginfo("xml str = "+a.join(" "));
        }
