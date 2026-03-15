
 Ext.ux.XManagedIframePanel = Ext.extend(Ext.ux.ManagedIframePanel, {


    initComponent : function(){

        var unsup = false;
        if(this.unsupportedText){
            unsup =typeof this.unsupportedText == 'object'? {children:[this.unsupportedText]}:{html:this.unsupportedText};
        }

        this.bodyCfg ||
           (this.bodyCfg =
               {tag:'div'
               ,cls:'x-panel-body'
               ,children:[Ext.apply({tag:'iframe',allowtransparency:true,
                           frameBorder  :0,
                           scrolling:'no',
                           cls          :'x-managed-iframe',
                           id :"iframe-"+this.id,
                           style        :{width:'100%',height:'100%'}
                          },unsup)
                          ]
           });

         Ext.ux.XManagedIframePanel.superclass.initComponent.call(this);
         this.addEvents({documentloaded:true, domready:true});

         if(this.defaultSrc){
            this.on('render', this.setSrc.createDelegate(this,[this.defaultSrc],0), this, {single:true});
        }
    }

});

Ext.reg('xiframepanel', Ext.ux.XManagedIframePanel);

//ConvertXIFramePanel=function(o){
//    ConvertPanel(o);
//    ConvertBoolean(o,'loadMask');
//    ConvertBoolean(o,'animCollapse');
//    ConvertBoolean(o,'autoScroll');
//    ConvertBoolean(o,'closable');		
//}
//ConvertMgr.regConverter("xiframepanel",ConvertXIFramePanel);

