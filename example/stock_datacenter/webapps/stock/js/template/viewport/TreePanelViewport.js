/*******************************************************************************
 * 初始化页面 1.布局 2.初始化树形控件 3.content部分管理
 * 
 ******************************************************************************/

Ext.onReady(function() {
			initPage();
    
    popupWinFrm = null;
    
    /**弹出窗口,输入items对象,必须指定渲染到body,  渲染到body的组件集对不上,无法使用**/
/*    popupMywin  = new Ext.Window({handerId:'',autoScroll:true,height:400,width:800,modal:true,closeAction: 'hide',resizable: false,
    	        html: '<iframe id="popupWinFrm01" src="" width="100%" height="100%" frameborder="0" scroll="auto"></iframe>',
  							  listeners: {
		                			   		'hide': closePopupMyWin
		              					 }
    });
    		popupMywin.show();  //初始化时创建
            popupMywin.hide();  //创建后隐藏

			function closePopupMyWin() {
		        popupMywin.hide();
		    }
  */          

            
            
    /**弹出窗口,输入对象url**/        
	popupWin = new Ext.Window({
					id:'popupWin',
	                closable: true,
	                resizable: false,
	                modal: true,
	                autoScroll: true,
	                closeAction: 'hide',
	                width: 1,
	                height: 1,
	                html: '<iframe id="popupWinFrm" src="" width="100%" height="100%" frameborder="0" scroll="auto"></iframe>',
	                listeners: {
	                   'hide': closePopupWin
	                }
                });
				popupWin.show();
                popupWin.hide();
               
                
    popupWinFrm = Ext.get('popupWinFrm').dom; 
    /*
                statusMessage = new Ext.thsware.ui.StatusMessage({
                  title: '<fmt:message key="common.message.tips"/>',
                  contentEl: 'msgbox',
                  iconCls: 'msgtips'
                });
       */         
			function closePopupWin() {
		        if(!Ext.isEmpty(popupWinFrm)) {
                    popupWinFrm.src = "";
		        }
		        popupWin.hide();
		    }


			
			
		});

Ext.namespace('MyKit.widget');
// MyKit.widget.TreePanelViewPort =new function(config){
// };
MyKit.widget.TreePanelViewport = Ext.extend(Ext.Viewport, {
	id : 'viewport',
	dataUrl : '',
	rootNodeName : '',
	noleafClick : false,
	menuHref : '',
	layout : 'border',

	initComponent : function() {
		MyKit.widget.TreePanelViewport.superclass.initComponent.call(this);
		
		this.treePanel = new MyKit.widget.TreeMenu({
					id : 'menu',
					width : '200',
					region : 'west',
					split : 'true',
					maxSize : '280',
					title : '',
					//collapseMode : 'mini', //最小化时隐藏所有边
					collapsible : 'true', //出现左右箭头,可缩小或展开
					noleafClick : this.noleafClick,
					menuHref :this.menuHref,
					loader : new Ext.tree.TreeLoader({
								dataUrl : this.dataUrl
							}),
					root : {
						xtype : 'asynctreenode',
						expanded : true,
						id : 'root',
						singleClickExpand : true,
						text : this.rootNodeName
					},
					handleOnClick : function(node, e) {
						e.stopEvent();
						var url = this.menuHref+'?id=' + node.attributes.id + '&parent='
								+ node.attributes.parent;
						// 非叶子节点是否可以点击
						if (this.noleafClick) {
							if (node.leaf) {
								if(!node.href){
								   //如果后台没有设置此节点的url就在前台设置
								   node.href = url;
								}
							} else {
								if(!node.url){
									node.url = url;
								}
							}
						} else {
//							alert(node.id);
							// 只允许叶子节点可以点击
							// 将href设置为url
							if(!node.href && !node.attributes.href){
								node.href = node.attributes.href = url;
							}
							
							node.url = node.attributes.url = null;
						}
						if (node.leaf || (!node.leaf && node.attributes.url)) {
							this.fireEvent("menuclick", node.attributes, e);
						}
					}

				});	
		this.treePanel.setHandler(function(node) {
					if (node.id == 'wind'){ //如果为万德数据
						Ext.getCmp("leftContent").show();
					}else{ 
						//非财务三张表
						Ext.getCmp("leftContent").hide();
						var mainPanel = Ext.getCmp("content");
						var url = node.href ? node.href : node.url;
						mainPanel.loadPage(url, true, true);
					}
					Ext.getCmp("mainContent").doLayout(); //重新渲染					
				});

		var participateUrl = 'projTreeMain.html';  
		
			//自定义3个表		//默认为：我参与的工程
			this.customProj = new Ext.Panel({
				items: [{header:false,"autoScroll":true,"border":false,"html":"<table width='98%' class='menu'><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\" "+window.CTXPATH + "/jsp/back/assetindex/assetindex.jsp"+"\",\"资产负责表\", this, true,false)'>资产负责表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/profileindex/profileindex.jsp"+"\",\"利润损益表\", this, true,false)'>利润损益表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/cashflow/cashflow.jsp"+"\",\"现金流量表\", this, true,false)'>现金流量表<\/a><\/td><\/tr><\/table>","title":"项目信息管理"}],
				margins:'0 0 0 5',
				title: '自定义',
				border: false
			});
			
                //默认为：我参与的工程
             this.aProj = new Ext.Panel({
                items: [{header:false,"autoScroll":true,"border":false,"html":"<table width='98%' class='menu'><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\" "+window.CTXPATH + "/jsp/back/assetwinda/assetwinda.jsp"+"\",\"资产负责表\", this, true,false)'>资产负责表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/profilewinda/profilewinda.jsp"+"\",\"利润损益表\", this, true,false)'>利润损益表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/cashflowwinda/cashflowwinda.jsp"+"\",\"现金流量表\", this, true,false)'>现金流量表<\/a><\/td><\/tr><\/table>","title":"项目信息管理"}],
                margins:'0 0 0 5',
	           	title: 'A股',
                border: false
                });
               
			this.bdProj = new Ext.Panel({
                items: [{header:false,"autoScroll":true,"border":false,"html":"<table width='98%' class='menu'><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\" "+window.CTXPATH + "/jsp/back/assetwindabd/assetwindabd.jsp"+"\",\"资产负责表\", this, true,false)'>资产负责表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/profilewindabd/profilewindabd.jsp"+"\",\"利润损益表\", this, true,false)'>利润损益表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/cashflowwindabd/cashflowwindabd.jsp"+"\",\"现金流量表\", this, true,false)'>现金流量表<\/a><\/td><\/tr><\/table>","title":"项目信息管理"}],
	            margins:'0 0 0 5',
                title: '证券',
                border: false
	            });
            
            this.bkProj = new Ext.Panel({
                items: [{header:false,"autoScroll":true,"border":false,"html":"<table width='98%' class='menu'><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\" "+window.CTXPATH + "/jsp/back/assetwindabk/assetwindabk.jsp"+"\",\"资产负责表\", this, true,false)'>资产负责表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/profilewindabk/profilewindabk.jsp"+"\",\"利润损益表\", this, true,false)'>利润损益表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/cashflowwindabk/cashflowwindabk.jsp"+"\",\"现金流量表\", this, true,false)'>现金流量表<\/a><\/td><\/tr><\/table>","title":"项目信息管理"}],
	            margins:'0 0 0 5',
                title: '银行',
                border: false
            });
            
            this.inProj = new Ext.Panel({
                items: [{header:false,"autoScroll":true,"border":false,"html":"<table width='98%' class='menu'><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\" "+window.CTXPATH + "/jsp/back/assetwindain/assetwindain.jsp"+"\",\"资产负责表\", this, true,false)'>资产负责表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/profilewindain/profilewindain.jsp"+"\",\"利润损益表\", this, true,false)'>利润损益表<\/a><\/td><\/tr><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\""+window.CTXPATH + "/jsp/back/cashflowwindain/cashflowwindain.jsp"+"\",\"现金流量表\", this, true,false)'>现金流量表<\/a><\/td><\/tr><\/table>","title":"项目信息管理"}],
	            margins:'0 0 0 5',
	            title: '保险',
                border: false
            });
		
            this.leftPanel = new Ext.Panel({
                title: '项目列表',
                region:'west',
                id: 'leftContent',
                layout:'accordion',                
                collapsible: true,
                split:true,
                width: 180,
                minSize: 200,
                maxSize: 400,
//                html: '<iframe id="leftFrm" src="" width="100%" height="100%" frameborder="0" scroll="auto"></iframe>'
//				html:"<table width='98%' class='menu'><tr><td class='unselect' ><td><a href='javascript:void(0)' onclick='displayMain(\"mainInfo.jsp\",\"新项目申报\", this, false,true)'>新项目申报<\/a><\/td><\/tr><\/table>","title":"项目申报",
//                items:[this.customProj,this.aProj,this.bdProj,this.bkProj,this.inProj]
                items:[this.aProj,this.bdProj,this.bkProj,this.inProj]
            });
            this.leftPanel.hide(); //默认为隐藏
            
//            /* 主窗口的右栏. */
//            var rightPanel = new MyKit.widget.MainPanel({
//                title: '详细信息',
//                region:'center',
//                id: 'rightContent',
//                autoScroll: true,
//                html: '<iframe id="rightFrm" src="" width="100%" height="100%" frameborder="0" scroll="auto"></iframe>',
////                items:[content]
//				  
//            });
            
		this.content = new MyKit.widget.MainPanel({
			id : 'content',
			activeTab : 0,
			enableTabScroll : true,
			region : "center",
				// items : [{
				// xtype : 'iframepanel',
				// defaultSrc : 'add.jsp?parent=',
				// title : '新增根节点',
				// id : "homepage"
				// }]
                
			});
		
            /* 主窗口. */
            this.theContentPanel = new Ext.Panel({
                region:'center',
                layout:'border',
                border: false,
                id: 'mainContent',
                margins: '0 0 0 5',
                items: [
                    this.leftPanel, 
                    this.content

                ]
            });
		

//            
//		var top = new MyKit.widget.MainPanel({
//			id : 'top',
//			activeTab : 0,
//			enableTabScroll : true,
//			region : "north"
//				// items : [{
//				// xtype : 'iframepanel',
//				// defaultSrc : 'add.jsp?parent=',
//				// title : '新增根节点',
//				// id : "homepage"
//				// }]
//			});

		//顶部横幅
		this.add(new Ext.Panel({id : 'aaaa', html:'<table class="top-table"><tr height="36"><td class="top-left">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;投资分析管理系统</td><td class="top-center">&nbsp;</td><td class="top-right">&nbsp;</td></tr></table>',
			region : 'north'
			
}));
		//底部著作
		this.add(new Ext.Panel({id : 'aaaa2', title:'&copy; 2011 <a href="#">深圳市亨通平安软件工作室</a>', bodyCssClass:'mypanel',
			region : 'south',
			listeners:{
			"render":function(){
			this.el.addClass('mypanel');
			}
		}
		}));
		this.add(this.treePanel);
//		this.add(content);
		this.add(this.theContentPanel);
	}

		/* 导航树 */

	});
Ext.reg('treePanelViewport', MyKit.widget.TreePanelViewport);

	var CONTEXT_PATH = "";
            /**
             * 加载右侧主窗口内容的方法.
             * @param targetUrl 待加载的url.
			 * @param rightUrl 加载主窗体的url
             * @param moduleName 模块名.
             * @param srcObj 触发本方法的源对象, 即点击模块时的对象, 一般为<a>.
             * @param isToLeft 加载的url是否放在左边.默认放在左边.
             * @param isRightOnly 是否只显示主窗口右侧. 默认显示主窗口左右侧.
             */
            function displayMain(targetUrl, moduleName, srcObj, isToLeft, isRightOnly) {
                var contextUrl = CONTEXT_PATH + targetUrl;
//                var defaultDisp = null;
//                var defaultTitle = null;
                changeModuleStyle(srcObj);              
//                if(typeof isToLeft != "boolean" || isToLeft) {
//                    defaultDisp = "leftFrm";
//                    defaultTitle = "leftContent";
//                    Ext.getCmp("rightContent").setTitle(moduleName);
//                    Ext.get("rightFrm").dom.src="";
//                    isRightOnly = false;
//                } else {
//                    defaultDisp = "rightFrm";
//                    defaultTitle = "rightContent";
//                }
            
//                Ext.get(defaultDisp).dom.src = contextUrl;
//                Ext.getCmp(defaultTitle).setTitle(moduleName);
            	
            		var mainPanel = Ext.getCmp("content");
					mainPanel.loadPage(contextUrl, true, true);
                
                if(typeof isRightOnly == "boolean" && isRightOnly) {
                    Ext.getCmp("leftContent").hide();
                } else {
                    Ext.getCmp("leftContent").show();
                }
				//Ext.get("rightFrm").dom.src=rightUrl;//演示用！直接显示主窗体内容
                Ext.getCmp("mainContent").doLayout();
            }
            
            /**
             * 更改选定模块的样式.
             *
             * @param srcObj 触发本方法的源对象, 即点击模块时的对象, 一般为<a>.
             */
            function changeModuleStyle(srcObj) {
                if(!srcObj) {
                    return;
                }
                var theRow = srcObj.parentNode.parentNode;
                var table = theRow.parentNode;
                for(var i = 0, len = table.rows.length; i < len; i++) {
                    if(i == theRow.rowIndex) {
                        theRow.childNodes[0].className = "selected";
                        continue;
                    }
                    
                    var row = table.rows[i];
                    if(row.childNodes[0].className != "unselect") {
                        row.childNodes[0].className = "unselect";
                    }
                }
            }


