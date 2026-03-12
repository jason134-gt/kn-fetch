package com.yz.stock.portal.model.imported;

import java.util.ArrayList;
import java.util.List;

public class ImportVo {

	private String id;
	
	private String importPath;
	
	private String bakPath;
	
	private String tableSystemCode;
	
	private List<TableItem> tableItemList = new ArrayList<TableItem>();
	
	
	public String getTableSystemCode() {
		return tableSystemCode;
	}
	public void setTableSystemCode(String tableSystemCode) {
		this.tableSystemCode = tableSystemCode;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getImportPath() {
		return importPath;
	}
	public void setImportPath(String importPath) {
		this.importPath = importPath;
	}
	public String getBakPath() {
		return bakPath;
	}
	public void setBakPath(String bakPath) {
		this.bakPath = bakPath;
	}
	public List<TableItem> getTableItemList() {
		return tableItemList;
	}
	public void setTableItemList(List<TableItem> tableItemList) {
		this.tableItemList = tableItemList;
	}
	
	
	
}
