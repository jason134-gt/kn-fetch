package com.yz.stock.portal.excel;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.stock.portal.model.CompanyStockInfo;
import com.yz.stock.portal.service.CompanyStockInfoDataService;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StringUtil;



public class AddDataExcelService {

	Logger logger = LoggerFactory.getLogger(this.getClass());
	private static AddDataExcelService instance = new AddDataExcelService();
	AddDataExcelService() {

	}

	public static AddDataExcelService getInstance() {
		return instance;
	}

	public void importStockInfoData() {
		
		try {
			doImportStockInfoData(new File("E:\\stock\\stock\\file\\0007--v.xls"));
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}

	private void doImportStockInfoData(File file) throws BiffException, IOException {
		
		Workbook wb =Workbook.getWorkbook(file);
		try {
			// 取所有sheet
			Sheet[] sheets = wb.getSheets();
			for (Sheet sheet : sheets) {
				parse(sheet, file);
			}
			
		} catch (Exception e) {
			logger.error("import failed!",e);
		}
		finally
		{
			wb.close();
		}
		
	}

	private void parse(Sheet sheet, File file) {
		// TODO Auto-generated method stub
		for(int row =1;row<sheet.getRows();row++)
		{
			CompanyStockInfo csi = new CompanyStockInfo();
			
			for (int j = 0; j < sheet.getColumns(); j++) {
				
				String value = sheet.getCell(j, row)
						.getContents();
				String columnName = sheet.getCell(j, 0)
						.getContents();
				setValue2VO(csi,value,columnName);
			}
			
			CompanyStockInfoDataService.getInstance().insert(csi);
		}
		
	}

	private void setValue2VO(CompanyStockInfo csi, String value,
			String columnName) {
		if(StringUtil.isEmpty(value))
			return;
		try {
			Class clazz = csi.getClass();
			String fieldName = getFiledNameByColumn(columnName);
			Field field = clazz.getDeclaredField(fieldName);
			Method method = clazz.getDeclaredMethod("set"
					+ fieldName.substring(0, 1).toUpperCase()
					+ fieldName.substring(1),field.getType());
			
			if (field.getType().getSimpleName().toString().equals("int")) {
				method.invoke(csi, Integer.valueOf(value));
			}
			if (field.getType().getSimpleName().toString().equals("String")) {
				method.invoke(csi, value);
			}
			
			if (field.getType().getSimpleName().toString().equals("Double")) {
				method.invoke(csi, Double.valueOf(value));
			}
			
			if (field.getType().getSimpleName().toString().equals("Date")) {
				method.invoke(csi, DateUtil.format(value));
			}
			
			if (field.getType().getSimpleName().toString().equals("Long")) {
				method.invoke(csi, Long.valueOf(value));
			}
			
		} catch (Exception e) {
			//logger.error("setValue2VO",e);
		}	
	}

	private String getFiledNameByColumn(String columnName) {
		columnName = columnName.toLowerCase();
		if(columnName.contains("_"))
		{
			StringBuilder sb = new StringBuilder();
			String[] sa = columnName.split("_");
			for(int i=0;i<sa.length;i++)
			{
				if(i!=0&&sa[i].substring(0,1).matches("[a-z]{1}"))
				{
					sb.append(sa[i].substring(0,1).toUpperCase()+ sa[i].substring(1));
				}
				else
				{
					sb.append(sa[i]);
				}
			}
			columnName = sb.toString();
		}
		return columnName;
	}

}
