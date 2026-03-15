package com.yz.stock.portal.excel;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Sheet;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockException;
import com.stock.common.model.Company;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;
import com.yz.mycore.core.inter.ExtInit;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.imported.Cell;
import com.yz.stock.portal.model.imported.ImportVo;
import com.yz.stock.portal.model.imported.TableItem;

public class ColumnExcelParse implements IExcelParse, ExtInit {
	
	Configuration c = BaseFactory.getConfiguration();
	static String _partten_A = "（[0-9]+）";
	static Pattern p_A = Pattern.compile(_partten_A);
	static String _partten_B = "\\([0-9]+\\)";
	static Pattern p_B = Pattern.compile(_partten_B);
	Logger log = LoggerFactory.getLogger(this.getClass());

	@SuppressWarnings("rawtypes")
	public void parse(Sheet sheet, String id,File file) throws NumberFormatException, StockException {
		String ret =  StockCodes.FAILED;
		ImportVo iv = (ImportVo) StockFactory.get_pMap().get(id);
		List<TableItem> til = iv.getTableItemList();
		for (int i = 0; i < til.size(); i++) {
			TableItem ti = til.get(i);
			String sheetName = ti.getSheetName();
			if (sheetName.equals(sheet.getName())) {
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

	@SuppressWarnings("rawtypes")
	public void doParse(Sheet sheet, String tableType, int index, int cStart,
			int cEnd, File file, TableItem ti, String tsc) throws NumberFormatException, StockException {

			String companyCode = getCompanyCode(sheet,file);
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companyCode);
			if(c==null)
			{
				throw new StockException(Integer.valueOf(StockCodes.FAILED),"此公司不存在! companyCode ="+companyCode);
				
			}
			List<Cell> cl = ti.getCellList();
			for (int i = 0; i < cl.size(); i++) {
				// 逐行把数据导到数据库
				String row = String.valueOf(cl.get(i).getRowId());
				String colName = cl.get(i).getColumnName();
				int j = 0;
				for (j = 1; j < sheet.getColumns(); j++) {
					// 默认时间为第一行
					String time = getTimeOfData(j, sheet);
					if (row == null) {
						continue;
					}
					String value = sheet.getCell(j, Integer.parseInt(row))
							.getContents();
					if (!StringUtil.isEmpty(value)) {
						// 取掉','号
						value = value.replaceAll(",", "");
						// 如果是数字,则更新
						if (value.matches("[0-9\\.-]+")) {
							String tableName = StockUtil.getTableName(tsc,tableType);
							ExcelService.getInstance().updateData2Db(
									companyCode, tableName, colName, value,
									DateUtil.format(time));
						}

					}

				}
			}
	}

	public String getCompanyCode(Sheet sheet, File file) throws NumberFormatException, StockException {
		
		String companyName = sheet.getCell(0, 0).getContents();
		String companyCode = getCompanyCodeByName(companyName);
		if (StringUtil.isEmpty(companyCode)
				
				|| CompanyService.getInstance().getCompanyByCode(companyCode) == null) {
			String cName = file.getName().substring(0, file.getName().indexOf("."));
			cName = cName.split("[0-9]+")[0];
			Company c = CompanyService.getInstance().getCompanyByName(cName);
			if(c==null)
			{
				String es = "company is not exsit ! companyName :" + companyName+" ;filename :"+file.getAbsolutePath();
				log.warn(es);
				throw new StockException(Integer.valueOf(StockCodes.FAILED),es);
			}
			companyCode = c.getCompanyCode();
		}
		return companyCode;
	}

	public String getTimeOfData(int j, Sheet sheet) {
		// TODO Auto-generated method stub
		return sheet.getCell(j, 0).getContents();
	}

	public String getCompanyCodeByName(String companyName) {
		String companyCode = "";
		try {
			// TODO Auto-generated method stub
			Matcher m_A = p_A.matcher(companyName.trim());
			if (m_A.find()) {
				String tmp = m_A.group();
				companyCode = tmp.substring(1, tmp.length() - 1);
			}
			Matcher m_B = p_B.matcher(companyName.trim());
			if (m_B.find()) {
				String tmp = m_B.group();
				companyCode = tmp.substring(1, tmp.length() - 1);
			}
		} catch (Exception e) {
			// TODO: handle exception
			log.error("parase company code failed!", e);
		}
		return companyCode;
	}

	public void init(Object o) {

		try {
			if (o == null) {
				return;
			}
			Configuration c = (Configuration) o;
			ImportVo iv = new ImportVo();
			String id = c.getString("stock[@id]").trim();
			String importPath = c.getString("stock.importPath");
			String bakPath = c.getString("stock.bakPath");
			String tableSystemCode = c.getString("stock.tableSystemCode").trim();
			iv.setId(id);
			iv.setImportPath(importPath);
			iv.setBakPath(bakPath);
			iv.setTableSystemCode(tableSystemCode);
			
			List sl = c.getList("stock.table.[@sheetName]");
			for (int i = 0; i < sl.size(); i++) {
				TableItem ti = new TableItem();
				String sheetName = c.getString("stock.table(" + i
						+ ")[@sheetName]").trim();
				String region = c.getString("stock.table(" + i + ")[@region](0)")+","+c.getString("stock.table(" + i + ")[@region](1)");
				String tableName = c.getString("stock.table(" + i
						+ ")[@tableName]");
				String parseName = c.getString("stock.table(" + i
						+ ")[@parseName]").trim();
				String tableType = c.getString("stock.table(" + i
						+ ")[@tableType]").trim();
				
				ti.setSheetName(sheetName);
				ti.setRegion(region);
				ti.setTableName(tableName);
				ti.setParseName(parseName);
				ti.setTableType(tableType);
				
				List cells = c.getList("stock.table(" + i
						+ ").cell[@eCellRowId]");
				for (int j = 0; j < cells.size(); j++) {
					String rowId = c.getString("stock.table(" + i
							+ ").cell("+j+")[@eCellRowId]").trim();
					String columnName = c.getString("stock.table(" + i
							+ ").cell("+j+")[@columnName]").trim();
					Cell cc = new Cell();
					cc.setColumnName(columnName);
					cc.setRowId(Integer.valueOf(rowId));
					ti.getCellList().add(cc);
				}
				iv.getTableItemList().add(ti);
				
				StockFactory.get_pMap().put(iv.getId()+"."+sheetName, parseName);
			}
			StockFactory.get_pMap().put(iv.getId(), iv);
		} catch (Exception e) {
			log.error("init failed!",e);
		}

	}

	public void init() {
		// TODO Auto-generated method stub

	}

}
