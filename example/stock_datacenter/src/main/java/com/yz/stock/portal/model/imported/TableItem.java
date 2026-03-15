package com.yz.stock.portal.model.imported;

import java.util.ArrayList;
import java.util.List;

public class TableItem {

		private String sheetName;
		private String region;
		private String tableName ;
		private String parseName ;
		private String tableType;
		List<Cell> cellList = new ArrayList<Cell>();
		
		public String getTableType() {
			return tableType;
		}
		public void setTableType(String tableType) {
			this.tableType = tableType;
		}
		public List<Cell> getCellList() {
			return cellList;
		}
		public void setCellList(List<Cell> cellList) {
			this.cellList = cellList;
		}
		public String getSheetName() {
			return sheetName;
		}
		public void setSheetName(String sheetName) {
			this.sheetName = sheetName;
		}
		public String getRegion() {
			return region;
		}
		public void setRegion(String region) {
			this.region = region;
		}
		public String getTableName() {
			return tableName;
		}
		public void setTableName(String tableName) {
			this.tableName = tableName;
		}
		public String getParseName() {
			return parseName;
		}
		public void setParseName(String parseName) {
			this.parseName = parseName;
		}
		
		
}
