package com.yz.stock.portal.excel;

import java.io.File;

import com.stock.common.constants.StockException;

import jxl.Sheet;

public interface IExcelParse {

	public void parse(Sheet sheet, String id, File file)throws NumberFormatException, StockException ;
}
