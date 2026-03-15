// Very simple plugin for adding a close context menu to tabs
Ext.ux.TabCloseMenu = function(){
    var tabs, menu, ctxItem;
    this.init = function(tp){
        tabs = tp;
        tabs.on('contextmenu', onContextMenu);
    }

    function onContextMenu(ts, item, e){
        if(!menu){ // create context menu on first right click
            menu = new Ext.menu.Menu([{
                id: tabs.id + '-close',
                text: '关闭',
                handler : function(){
                    tabs.remove(ctxItem);
                }
            },{
                id: tabs.id + '-close-others',
                text: '关闭其他标签',
                handler : function(){
                    tabs.items.each(function(item){
                        if(item.closable && item != ctxItem){
                            tabs.remove(item);
                        }
                    });
                }
            },{
                id: tabs.id + '-fresh',
                text: '刷新',
                handler : function(){
                    ctxItem.iframe.dom.contentWindow.location.reload();
                }
            }]);
        }
        ctxItem = item;
        var items = menu.items;
        items.get(tabs.id + '-close').setDisabled(!item.closable);
        var disableOthers = true;
        tabs.items.each(function(){
            if(this != item && this.closable){
                disableOthers = false;
                return false;
            }
        });
        items.get(tabs.id + '-close-others').setDisabled(disableOthers);
        menu.showAt(e.getPoint());
    }
};

Ext.namespace('MyKit.widget');
MyKit.widget.MainPanel = Ext.extend(Ext.TabPanel,
                {
                    deferredRender :false,
                    layoutOnTabChange:true ,
                    plugins : new Ext.ux.TabCloseMenu(),

					afterRender : function(){
						MyKit.widget.MainPanel.superclass.afterRender.call(this); 
						this.on('tabchange',function(){						 
						  var width = this.getSize().width;
						  this.setWidth(width-1);						 
						  this.setWidth(width);						 
						});	
					},

                    loadPage : function(href,createNew,closable){
                        var activeTab = this.getActiveTab();
                        //如果tab是不可关闭的，不能这个tab上再加载
                        if (activeTab && !createNew && activeTab.closable) {
                            activeTab.setTitle("加载...");
                            activeTab.setSrc(href);
                        } else {
                            var tabId = "mainpanel-"+href;
                            var parentId = activeTab ? activeTab.id :"" ;
                            var tab = this.getComponent(tabId);
                            if(tab){
                                this.setActiveTab(tab);
                                tab.setTitle("加载...");
                                tab.setSrc(href);
                            }
                            else{
                                Ext.QuickTips.init();
                                var p = this.add(new Ext.ux.ManagedIframePanel( {
                                        id : tabId,
                                        title : '加载...',
                                        parentId : parentId,
                                        closable : closable,
                                        defaultSrc : href,
                                        loadMask : {
                                            msg : '正在加载内容...'
                                        },
                                        listeners : {
                                            documentloaded : function(iframe) {
                                                var win=iframe.dom.contentWindow;
                                                var doc=win.document;
                                               
                                                if(doc.location.protocol!='about:') {
                                                    var tl = doc.title || "无标题";
                                                    this.setTitle(tl);
                                                     //取页面中菜单长路径信息
                                                    //暂时先注释掉
//                                                    var pageTabtip = win.tabtip;
//                                                    if( pageTabtip){//如果有
//                                                    	tl = pageTabtip;
//                                                    }else{//没有取父页面的
//                                                        tl = Ext.Element.fly("content__"+this.parentId).child('span.x-tab-strip-text', true).qtip
//                                                    }
//                                                    Ext.Element.fly("content__"+this.id).child('span.x-tab-strip-text', true).qtip = tl;
                                                }
                                            }
                                        }
                                    }));
                            this.setActiveTab(p);
                        }
                       }
                    },
                    resumePage : function(src,defaultSrc,tabTip,parentId){
                        var tab = this.getActiveTab();
                            var tabId = "mainpanel-"+defaultSrc;
                            var tab = this.getComponent(tabId);
                            if(tab) {
                                this.setActiveTab(tab);
                                tab.setTitle("加载...");
                                tab.setSrc(src);
                            } else {
                                Ext.QuickTips.init();
                                var p = this.add(new Ext.ux.ManagedIframePanel({
                                        id : tabId,
                                        title : '加载...',
                                        tapTip:tabTip,
                                        parentId : parentId,
                                        defaultSrc : appendTS(defaultSrc),
                                        loadMask : {
                                            msg : '正在加载内容...'
                                        },
                                        listeners : {
                                            documentloaded : function(iframe) {
                                                var win=iframe.dom.contentWindow;
                                                var doc=win.document;
                                                
                                                if(doc.location.protocol!='about:') {
                                                    var tl = doc.title || "无标题";
                                                    this.setTitle(tl);
                                                    if(this.tabTip) {
                                                        tl =this.tabTip;
                                                    }
                                                    Ext.Element.fly("content__"+this.id).child('span.x-tab-strip-text', true).qtip = tl;
                                                }
                                            }
                                        }
                                    }));
                            this.setActiveTab(p);
                            if(src != defaultSrc) {
                                p.setTitle("加载...");
                                p.setSrc(appendTS(src));
                            }
                        }

                    }
                });

Ext.reg('mainpanel', MyKit.widget.MainPanel);

//ConvertMainPanel = function(o) {
//    ConvertTabPanel(o);
//}
//ConvertMgr.regConverter("mainpanel", ConvertMainPanel);