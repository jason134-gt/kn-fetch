Ext.namespace('MyKit.widget');
MyKit.widget.DefaultGridPanel = function(config) {
	MyKit.widget.DefaultGridPanel.superclass.constructor.call(this, config);
}
Ext.extend(MyKit.widget.DefaultGridPanel, MyKit.widget.XGridPanel, {
			id : 'grid',
			stripeRows : true,
			// height:400,
			frame : true,
			loadMask : true,
			autoScroll : true,
			collapsible : true,
			buttonAlign : 'left',
		/*	viewConfig : {
				forceFit : true
			}
*/
			viewConfig : {   //Extjs gridpanel横向滚动条
			    layout : function() {   
			        if (!this.mainBody) {   
			            return; // not rendered   
			        }   
			        var g = this.grid;   
			        var c = g.getGridEl();   
			        var csize = c.getSize(true);   
			        var vw = csize.width;   
			        if (!g.hideHeaders && (vw < 20 || csize.height < 20)) { // display:   
			            // none?   
			            return;   
			        }   
			        if (g.autoHeight) {   
			            if (this.innerHd) {   
			                this.innerHd.style.width = (vw) + 'px';   
			            }   
			        } else {   
			            this.el.setSize(csize.width, csize.height);   
			            var hdHeight = this.mainHd.getHeight();   
			            var vh = csize.height - (hdHeight);   
			            this.scroller.setSize(vw, vh);   
			            if (this.innerHd) {   
			                this.innerHd.style.width = (vw) + 'px';   
			            }   
			        }   
			        if (this.forceFit) {   
			            if (this.lastViewWidth != vw) {   
			                this.fitColumns(false, false);   
			                this.lastViewWidth = vw;   
			            }   
			        } else {   
			            this.autoExpand();   
			            this.syncHeaderScroll();   
			        }   
			        this.onLayout(vw, vh);   
			    }   
			},
			
		});