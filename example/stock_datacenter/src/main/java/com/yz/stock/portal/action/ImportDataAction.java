package com.yz.stock.portal.action;

import org.apache.commons.configuration.Configuration;
import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.mycore.core.manager.BaseFactory;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.excel.AcessImportDataExcelService;
import com.yz.stock.portal.excel.ExcelService;
import com.yz.stock.portal.excel.ImportAddDataExcelService;
import com.yz.stock.portal.excel.StockDataExcelService;
import com.yz.stock.portal.manager.ComputeIndexManager;

public class ImportDataAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5424611799257286358L;
	Logger log = LoggerFactory.getLogger(this.getClass());
	@Action(value = "/file/importData")
	public String importData()
	{
		try {
			ExcelService.getInstance().importAllFile();
		} catch (Exception e) {
			// TODO: handle exception
			log.error("impport data failed!",e);
			return ERROR;
		}
		return SUCCESS;
	}
	//导入证券信息表
	@Action(value = "/file/importStockInfoData")
	public String importStockInfoData()
	{
		try {
			Configuration c = BaseFactory.getConfiguration();
			String stockInfoPath = c.getString("stock.stockInfoPath");
			StockDataExcelService.getInstance().importStockInfoData(stockInfoPath);
		} catch (Exception e) {
			// TODO: handle exception
			log.error("impport data failed!",e);
			return ERROR;
		}
		return SUCCESS;
	}

	//导入从acess中导出的全量数据
	@Action(value = "/file/importAcessExportData")
	public String importAcessExportData()
		{
			try {
				ComputeIndexManager.getInstance().importInit();
				Configuration c = BaseFactory.getConfiguration();
				String importAll = c.getString("stock.importAll");
				AcessImportDataExcelService.getInstance().importAcessExportData(importAll);
			} catch (Exception e) {
				// TODO: handle exception
				log.error("impport data failed!",e);
				return ERROR;
			}
			return SUCCESS;
		}
	
	//导入增量数据
	@Action(value = "/file/importAddZipExportData")
	public String importAddExportData()
		{
			try {
				ComputeIndexManager.getInstance().importInit();
				Configuration c = BaseFactory.getConfiguration();
				String importAdd = c.getString("stock.importAdd");
				ImportAddDataExcelService.getInstance().importExportData(importAdd);
			} catch (Exception e) {
				// TODO: handle exception
				log.error("impport data failed!",e);
				return ERROR;
			}
			return SUCCESS;
		}
	

		
}
