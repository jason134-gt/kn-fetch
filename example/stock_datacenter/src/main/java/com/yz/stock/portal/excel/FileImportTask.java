package com.yz.stock.portal.excel;

import java.io.File;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;


public class FileImportTask implements Callable<String> {

	Logger log = LoggerFactory.getLogger(this.getClass());
	File importFile;
	String bakPath;
	String failedPath;
	static String lock = new String();
	ExcelService exs;
	public FileImportTask()
	{
		
	}
	public FileImportTask(File tf,String bakPath,String failedPath,ExcelService exs)
	{
		this.failedPath = failedPath;
		this.importFile = tf;
		this.bakPath = bakPath;
		this.exs = exs;
	}
	public String call() {
		
		try {
			String filePath = importFile.getAbsolutePath();
			// 导入文件夹下的所有文件
			exs.importFile(filePath, bakPath, failedPath);
			log.info("delete file;name:"+filePath);
				// 删除文件夹
			importFile.delete();
			
			//如果失败目录为空(没有导入失败的),就删除.
			File tff = new File(failedPath);
			if (tff.list().length == 0) {
				tff.delete();
			}
		} catch (Exception e) {
			log.error("import data failed!",e);
		}
		return StockCodes.SUCCESS;
	}

}
