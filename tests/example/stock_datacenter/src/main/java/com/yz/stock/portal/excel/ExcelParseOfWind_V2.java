package com.yz.stock.portal.excel;


import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Sheet;

import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockException;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.imported.ImportVo;
import com.yz.stock.portal.model.imported.TableItem;

public class ExcelParseOfWind_V2 extends ColumnExcelParse {
	
	Pattern p = Pattern.compile("[0-9]+");
	public String getTimeOfData(int j, Sheet sheet) {
		// TODO Auto-generated method stub
		String ttime = sheet.getCell(j, 0).getContents();
		String year = "";
		Matcher m = p.matcher(ttime);
		while(m.find())
		{
			year = m.group();
		}
		String month = "";
		if(ttime.indexOf("一季")>0)
		{
			month = "03-30";
		}
		if(ttime.indexOf("中报")>0)
		{
			month = "06-30";
		}
		if(ttime.indexOf("三季")>0)
		{
			month = "09-30";
		}
		if(ttime.indexOf("年报")>0)
		{
			month = "12-30";
		}
		return year+"-"+month;
	}

	@SuppressWarnings("rawtypes")
	public void parse(Sheet sheet, String id,File file) throws NumberFormatException, StockException {
		String ret =  StockCodes.FAILED;
		ImportVo iv = (ImportVo) StockFactory.get_pMap().get(id);
		List<TableItem> til = iv.getTableItemList();
		for (int i = 0; i < til.size(); i++) {
			TableItem ti = til.get(i);
			String sheetName = ti.getSheetName();
			//sheet名包含某一个报表名
			if (sheet.getName().contains(sheetName)) {
				String start = ti.getRegion().split(",")[0];
				String end = ti.getRegion().split(",")[1];
				String tableName = ti.getTableName();
				String tableType = ti.getTableType();
				int cStart = Integer.parseInt(start);
				int cEnd = Integer.parseInt(end);
			 doParse(sheet, tableType, i, cStart, cEnd,file,ti,iv.getTableSystemCode());
			} else {
				continue;
			}

		}
	}
	
	public String getCompanyCode(Sheet sheet, File file) throws NumberFormatException, StockException {
		return sheet.getName().split("\\.")[0];
		
	}
}
