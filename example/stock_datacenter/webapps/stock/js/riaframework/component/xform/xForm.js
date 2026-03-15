Ext.namespace('MyKit.widget');
/**
 *
 * @author gongjt
 * @version 1.1
 *
 * MyKit.widget.XForm 
 * @class MyKit.widget.XForm
 * @extends Ext.FormPanel
 */
MyKit.widget.XForm = function(config) {
    MyKit.widget.XForm.superclass.constructor.call(this, config);
}

Ext.extend(MyKit.widget.XForm, Ext.form.FormPanel, {

    /**
     * 面板默认显示参数
     */
    collapsible : true,
    titleCollapse : true,
    frame : true,
    minButtonWidth : 40,
    
    /**
     * label文字对齐方式, 默认值是查询面板风格的右对齐.
     */
    labelAlign : 'right',

    /**
     * 最大列数, 默认值是3
     */
    maxCols : 3,

    /**
     * 列宽, 默认值是每列33%. 另外, 所有列的总宽度为组件面板(cpWidth)的宽度
     */
    cw : .33,

    /**
     * LabelWidth, 默认值是80
     */
    lw : 80,

    /**
     * 组件面板的宽度, 默认值是80%
     */
    cpWidth : .8,

    /**
     * 按钮面板的宽度, 默认值是20%
     */
    bpWidth : .2,
    
    // 面板行数
    rows : 1,

    // 默认按钮位置
    buttonAlign : 'center',

    /**
     * 封装待布局的组件数组
     */
    components : [],

    /**
     * 面板中的按钮
     */
    xbuttons : [],

    panelMap : null,

    rowsMap : null,

    btnsPanel : null,

    /**
     * 给查询组件的面板布局
     */
    initLayout : function() {

        if(!this.id) {
            this.id = Ext.id();
        }
        this.panelMap = new Map();
        this.rowsMap = new Map();

        this.cw = (1 / this.maxCols).toFixed(2);    // 计算列宽比例
        if(this.xbuttons.length > 0) {    // 计算组件面板的宽度
            this.cpWidth = (1 - this.bpWidth).toFixed(2);
        } else {
            this.cpWidth = 1;
            this.bpWidth = 0;
            this.labelAlign = 'left';    // 无按钮,转为详情风格,文字左对齐.
        }

        // 定义组件面板, 用于装提供待布局的组件
        var cmpsPanel = new Ext.Panel({
            columnWidth : this.cpWidth,
            layout : 'form'
        });

        var tempConfig = {
            layout : 'column',
            height : 30
        };

        // 添加组件到组件面板中
        var mainPanel = new Ext.Panel(tempConfig);
        mainPanel.id = this.id + '-xrow' + this.rows;
        var emptyCols = this.maxCols;
        for(var i = 0; i < this.components.length; i++) {

            var cmpTemp = this.components[i];

            //为component下的一级子元素设置缺省的anchor属性，以适应不同分辨率。
            if(!cmpTemp.anchor && !cmpTemp.width && cmpTemp.xtype != 'panel' && cmpTemp.xtype != 'label' && cmpTemp.xtype != 'button' && cmpTemp.xtype != 'uploadpanel') {
                try {
                    cmpTemp.anchor = '90%';
                }catch(e){}
            }

            //如果是隐藏属性，添加到form后结束循环，不参与布局。
            if(cmpTemp.xtype == 'hidden') {
                cmpsPanel.add(cmpTemp);
                continue;
            }

            var cols = cmpTemp.cols || 1;
            cols = cols > this.maxCols ? this.maxCols : cols;
            var formPanel = new Ext.Panel({
                columnWidth : this.cw * cols,
                layout : 'form',
                defaultType: 'textfield',
                labelAlign : this.labelAlign,
                labelWidth : cmpTemp.lw || this.lw
            });
            formPanel.add(cmpTemp);
            if(cmpTemp.id) {
                formPanel.id = 'panel_' + cmpTemp.id;
            } else {
                formPanel.id = Ext.id();
            }

            if(emptyCols >= cols) {
                mainPanel.add(formPanel);
                this.addCmp(mainPanel.id);
                this.panelMap.put(formPanel.id, mainPanel.id);
                emptyCols = (emptyCols - cols).toFixed(3);
            } else {
                cmpsPanel.add(mainPanel);
                this.rows++;
                mainPanel = new Ext.Panel(tempConfig);
                mainPanel.id = this.id + '-xrow' + this.rows;
                emptyCols = this.maxCols;
                mainPanel.add(formPanel);
                this.addCmp(mainPanel.id);
                this.panelMap.put(formPanel.id, mainPanel.id);
                emptyCols = (emptyCols - cols).toFixed(3);
            }
            if(this.xbuttons.length == 0) {
                if(cmpTemp.height) {
                    mainPanel.setHeight(cmpTemp.height+5);
                } else {
                    mainPanel.setHeight("");
                }
            }

        }
        if(emptyCols < this.maxCols) {
            cmpsPanel.add(mainPanel);
        }

        // 定义按钮面板, 用于装查询, 清除之类的按扭
        if(this.xbuttons.length > 0) {
            var bpConfig = {
                columnWidth : this.bpWidth,
                layout : 'form',
                buttonAlign : this.buttonAlign,
                id  : this.id + '_btnsPanel',
                minButtonWidth : this.minButtonWidth,
                style : 'margin-top: -7',
                items : [{
                    xtype : 'panel',
                    id : this.id + 'btnsBlankPanel',
                    width : 0,
                    height : 30 * (this.rows - 1)
                }]
            };
            if(this.rows == 1 && Ext.isIE) {
                bpConfig.style = 'margin-top: -20';
            }
            this.btnsPanel = new Ext.Panel(bpConfig);
    
            // 放置按钮到按钮面板中
            for(var i = 0; i < this.xbuttons.length; i++) {
                this.btnsPanel.addButton(this.xbuttons[i]);
            }
    
            // 设置主面板布局, 并将组件面板和按钮面板放入其中
            this.layout = 'form';
            this.items = [{
                xtype : 'panel',
                layout : 'column',
                items : [cmpsPanel, this.btnsPanel]
            }];
        } else {
            this.btnsPanel = null;
            this.layout = 'form';
            this.items = [{
                xtype : 'panel',
                layout : 'column',
                items : [cmpsPanel]
            }];
        }
    },
    
    renderBtn : function() {
        if(this.btnsPanel != null) {
            var btnsBlankPanel = document.getElementById(this.id + 'btnsBlankPanel');
            if(btnsBlankPanel) {
                btnsBlankPanel.parentNode.removeChild(btnsBlankPanel);
            }
            var blockPanel = new Ext.Panel({
                xtype : 'panel',
                id : this.id + 'btnsBlankPanel',
                width : 0,
                height : 30 * (this.rows - 1)
            });
            this.btnsPanel.add(blockPanel);
            
            if(this.rows == 1 && Ext.isIE) {
                var btnsPanel = document.getElementById(this.id + '_btnsPanel');
                btnsPanel.style.margin = '-20px 0px 0px';
            } else {
                var btnsPanel = document.getElementById(this.id + '_btnsPanel');
                btnsPanel.style.margin = '-7px 0px 0px';
            }
            this.btnsPanel.doLayout();
        }
    },
    
    addCmp : function(rowId) {
        var val = this.rowsMap.get(rowId);
        if(val != null) {
            val++;
            this.rowsMap.remove(rowId);
            this.rowsMap.put(rowId, val);
        } else {
            this.rowsMap.put(rowId, 1);
        }
        
    },
    
    deleteCmp : function(rowId) {
        var val = this.rowsMap.get(rowId);
        val--;
        this.rowsMap.remove(rowId);
        this.rowsMap.put(rowId, val);
    },
    
    checkHide : function(rowId) {
        var rows = this.rowsMap.get(rowId);
        if(rows == 0) {
            var rowpanel = document.getElementById(rowId);
            if(rowpanel && rowpanel.style.display=="") {
                rowpanel.style.display="none";
                this.rows--;
                this.renderBtn();
            }
        }

    },
    
    hide : function() {
        if(arguments.length == 0) {
            if(this.fireEvent("beforehide", this) !== false){
                this.hidden = true;
                if(this.rendered){
                    this.onHide();
                }
                this.fireEvent("hide", this);
            }
            return this;
        }
        for(var i = 0;i < arguments.length;i++) {
            var tempReset = Ext.getCmp(arguments[i]);
            if(tempReset) {
                this.xClear(tempReset); //隐藏前清空组件值
            }
            var tempCmp = document.getElementById('panel_' + arguments[i]);
            if(tempCmp && tempCmp.style.display=="") {
            	tempCmp.style.display="none";
            	var rowId = this.panelMap.get('panel_' + arguments[i]);
            	this.deleteCmp(rowId);
            	this.checkHide(rowId);
            }
        }
    },
    
    checkShow : function(rowId) {
        var rows = this.rowsMap.get(rowId);
        if(rows > 0) {
            var rowpanel = document.getElementById(rowId);
            if(rowpanel && rowpanel.style.display=="none") {
                rowpanel.style.display="";
                this.rows++;
                this.renderBtn();
            }
        }
    },
    
    show : function() {
        if(arguments.length == 0) {
            if(this.fireEvent("beforeshow", this) !== false){
                this.hidden = false;
                if(this.autoRender){
                    this.render(typeof this.autoRender == 'boolean' ? Ext.getBody() : this.autoRender);
                }
                if(this.rendered){
                    this.onShow();
                }
                this.fireEvent("show", this);
            }
            return this;
        }
        for(var i = 0;i < arguments.length;i++) {
            var tempReset = Ext.getCmp(arguments[i]);
            if(tempReset) {
                this.xReset(tempReset);
            }
            var tempCmp = document.getElementById('panel_' + arguments[i]);
            if(tempCmp && tempCmp.style.display=="none") {
            	tempCmp.style.display="";
            	var rowId = this.panelMap.get('panel_' + arguments[i]);
            	this.addCmp(rowId);
            	this.checkShow(rowId);
            }
        }
    },

    xClear : function(cmp) {
        try {
            cmp.reset();
            if(cmp.xtype == 'radio' || cmp.xtype == 'checkbox') {
                cmp.setValue(false);
            }
        } catch(e) {
            var fi = function(component,container){return true;};
            if(typeof(cmp.findBy) == 'function') {
                var arr = cmp.findBy(fi, this);
                for(var i = 0;i < arr.length;i++) {
                    this.xClear(arr[i]);
                }
            }
        }
    },
    
    xReset : function(cmp) {
        try {
            cmp.reset();
        } catch(e) {
            var fi = function(component,container){return true;};
            if(typeof(cmp.findBy) == 'function') {
                var arr = cmp.findBy(fi, this);
                for(var i = 0;i < arr.length;i++) {
                    this.xReset(arr[i]);
                }
            }
        }
    },

    initComponent : function() {

        // 初始化布局
        this.initLayout()
        MyKit.widget.XForm.superclass.initComponent.call(this);

    }

});

// 注册此组件的xtype类型
Ext.reg('XForm', MyKit.widget.XForm);

//ConvertXForm = function(o){
//    ConvertForm(o);
//
//    var temp = [];
//    if(o.component && o.component._children.length > 0) {
//      for(var i = 0;i < o.component._children.length;i++){
//        ConvertMgr.convert(o.component._children[i]);
//        if(o.component._children[i].cols) {
//            ConvertFloat(o.component._children[i], 'cols');
//        }
//        if(o.component._children[i].lw) {
//            ConvertInt(o.component._children[i], 'lw');
//        }
//        temp.push(o.component._children[i]);
//      }
//      delete o.component._children;
//      o.component = temp;
//    }
//
//    temp = [];
//    if(o.xbuttons && o.xbuttons._children.length > 0) {
//      for(var i = 0;i < o.xbuttons._children.length;i++){
//        ConvertMgr.convert(o.xbuttons._children[i]);
//        temp.push(o.xbuttons._children[i]);
//      }
//      delete o.xbuttons._children;
//      o.xbuttons = temp;
//    }
//    delete temp;
//
//    ConvertFloat(o,'bpWidth');
//    ConvertInt(o,'maxCols');
//    ConvertInt(o,'lw');
//
//}
//
//ConvertMgr.regConverter('XForm', ConvertXForm);