Ext.namespace('MyKit.widget');
MyKit.widget.XGridView = function(config) {
	MyKit.widget.XGridView.superclass.constructor.call(this, config);
};

Ext.extend(MyKit.widget.XGridView, Ext.grid.GridView, {
	/**
	 * @private Adds CSS classes and rowIndex to each row
	 * @param {Number}
	 *            startRow The row to start from (defaults to 0)
	 */
	processRows : function(startRow, skipStripe) {

		if (!this.ds || this.ds.getCount() < 1) {
			return;
		}

		var rows = this.getRows(), length = rows.length, row, i;

		skipStripe = skipStripe || !this.grid.stripeRows;
		startRow = startRow || 0;

		for (i = 0; i < length; i++) {
			row = rows[i];
			if (row) {

				row.rowIndex = i;
				if (!skipStripe) {
					row.className = row.className.replace(this.rowClsRe, ' ');
					if ((i + 1) % 2 === 0) {
						row.className += ' x-grid3-row-alt';
					}
				}
				// 处理列中的组件
				if (this.cm instanceof MyKit.widget.XColumnModel) {
					for (var n = 0; n < this.cm.getColumnCount(); n++) {
						var cell = this.getCell(i, n);
						// =================buttons==============================
						var buttons = this.cm.getCellButtons(n, i);
						if (buttons) {
							cell.removeChild(cell.firstChild);
							var tpl =new Ext.XTemplate(
							       '<TABLE width=100% align=center>' +
									   '<TR id={id}>{colTpl}</TR>' +
									'</TABLE>');
							tpl.compile();
						    var id = "grid-row-col-"+i+"-"+n;
						    var tplData ={"id":id} ;
							tpl.append(cell,tplData);
							var tr = Ext.get(id);
							var buttonWidth = cell.clientWidth / buttons.length -10;
							for(var k = 0; k < buttons.length; k++){
								var colTpl = new Ext.XTemplate('<TD id={colId} width={width} align=center></TD>');
								colTpl.compile();
								
								var id = "grid-row-col-"+i+"-"+n+"-"+k;
								var colTplData ={"colId":id} ;
								colTpl.append(tr,colTplData);
								
								var td = Ext.get(id);
								var button = new Ext.Button(buttons[k]);
								button.width = buttonWidth;
								button.record = this.ds.getAt(i);
								button.render(td);
							}
							
						}else{
							// =================component============================
							var component = this.cm.getCellComponent(n, i);
							if (component) {
								var field = this.cm.getDataIndex(n);
								var comp = Ext.ComponentMgr.create(component);
								comp.value = this.ds.getAt(i).data[field];
								// 添加单击事件
								comp.handler = component.handler;
								comp.record = this.ds.getAt(i);
								comp.render(cell);

								if (this.cm.config[n].align != undefined) {
									comp.el.dom.align = this.cm.config[n].align;
								} else {
									comp.el.dom.align = 'left';
								}

							}
						}
					}
				}
			}
		}

		// add first/last-row classes
		if (startRow === 0) {
			Ext.fly(rows[0]).addClass(this.firstRowCls);
		}

		Ext.fly(rows[length - 1]).addClass(this.lastRowCls);
	}

});
