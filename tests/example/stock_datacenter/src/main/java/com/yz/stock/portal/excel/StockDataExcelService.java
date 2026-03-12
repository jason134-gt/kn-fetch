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
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;



public class StockDataExcelService {

	Logger logger = LoggerFactory.getLogger(this.getClass());
	private static StockDataExcelService instance = new StockDataExcelService();
	StockDataExcelService() {

	}

	public static StockDataExcelService getInstance() {
		return instance;
	}

	public void importStockInfoData(String path) {
		
		try {
			doImportStockInfoData(new File(path));
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
				StockUtil.setValue2VO(csi,value,columnName);
			}
			
			CompanyStockInfoDataService.getInstance().insert(csi);
		}
		
	}



}
