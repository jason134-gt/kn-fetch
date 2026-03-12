package com.yz.stock.portal.excel;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Sheet;

public class ColumnExcelParseOfWind extends ColumnExcelParse {
	
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


}
