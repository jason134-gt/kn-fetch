package com.yz.stock.portal.excel;

import java.io.File;
import java.util.concurrent.Callable;

import jxl.Sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.yz.stock.portal.task.TaskSchedule;


public class ImportAcessTask implements Callable<String> {

	Logger log = LoggerFactory.getLogger(this.getClass());
	File file;
	int pageIndex;
	Sheet sheet;
	Object service;
	public ImportAcessTask(File file,Sheet sheet,int pageIndex,Object service)
	{
		this.file = file;
		this.sheet = sheet;
		this.pageIndex = pageIndex;
		this.service = service;
	}
	public ImportAcessTask()
	{
		
	}

	public String call() {
		try {
			log.info("========================start import file ! filename = "+file);
			if(service instanceof AcessImportDataExcelService)
			AcessImportDataExcelService.getInstance().doImport(file, sheet, pageIndex);
			
			if(service instanceof ImportAddDataExcelService)
				ImportAddDataExcelService.getInstance().doImport(sheet, file,pageIndex);
			log.info("===========================complete import file ! filename = "+file);
		} catch (Exception e) {
			log.error("import data failed!",e);
		}
		return StockCodes.SUCCESS;
	}

}
