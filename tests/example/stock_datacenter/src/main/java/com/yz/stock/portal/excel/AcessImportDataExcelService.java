package com.yz.stock.portal.excel;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Industry;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.IndustryService;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.BatchQueueEntity;
import com.yz.stock.portal.model.CompanyIndustry;
import com.yz.stock.portal.model.Companyasset0290;
import com.yz.stock.portal.model.Companycash0292;
import com.yz.stock.portal.model.Companyprofile0291;
import com.yz.stock.portal.model.IndustryCompanyArray;
import com.yz.stock.portal.service.Companyasset0290DataService;
import com.yz.stock.portal.service.Companycash0292DataService;
import com.yz.stock.portal.service.Companyprofile0291DataService;
import com.yz.stock.util.DCenterUtil;
import com.yz.stock.util.ExecuteQueueManager;

public class AcessImportDataExcelService {

	int page = 5000;
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private static AcessImportDataExcelService instance = new AcessImportDataExcelService();

	AcessImportDataExcelService() {

	}

	public static AcessImportDataExcelService getInstance() {
		return instance;
	}

	private void doImportStockInfoData(File file) throws BiffException,
			IOException {

		WorkbookSettings wbSetting = new WorkbookSettings();
		wbSetting.setUseTemporaryFileDuringWrite(true);
		wbSetting.setGCDisabled(false);
		Workbook wb = Workbook.getWorkbook(file, wbSetting);
		try {
			// 取所有sheet
			Sheet[] sheets = wb.getSheets();
			// 现在只导第一个sheet
			for (int i = 0; i < 1; i++) {
				Sheet sheet = sheets[i];
				dispatch(sheet, file);
			}

		} catch (Exception e) {
			logger.error("import failed!", e);
		} finally {
			wb.close();
			System.gc();
			file.delete();
//			while (Runtime.getRuntime().totalMemory() > 1024 * 1024 * 800) {
//				try {
//					System.out
//							.println("==============================================================================sleep");
//					wb.close();
//					System.gc();
//					logger.info("==============================================================================sleep");
//					Thread.sleep(10000l);
//				} catch (Exception e2) {
//					logger.error("gc failed!", e2);
//				}
//			}
		}

	}

	public void dispatch(Sheet sheet, File file) {
		// 分成多页,多线程进行导入
		int times = sheet.getRows() / page;

		if (sheet.getRows() % page > 0)
			times += 1;
//		List<Future> lf = new ArrayList<Future>();
		// 分批
		for (int t = 0; t < times; t++) {
			 StockFactory.submitTaskBlocking(new ImportAcessTask(file, sheet, t,
					this));
//			lf.add(f);

		}
		// 等待此次任务全部完成
//		for (Future f : lf) {
//			try {
//				f.get();
//			} catch (Exception e) {
//				// TODO: handle exception
//			}
//		}
	}

	public void doImport(File file, Sheet sheet, int pageIndex) {
		int startIndex = pageIndex * page;
		int endIndex = startIndex + page;
		endIndex = endIndex > sheet.getRows() ? sheet.getRows() : endIndex;
		if (startIndex == 0)
			startIndex = 1;
		boolean isValidateData = false;
		for (int row = startIndex; row < endIndex; row++) {

			try {
				String name = file.getName();
				Object vo = getImportVO(name);
				if (vo == null)
					continue;
			    isValidateData = false;
				for (int j = 0; j < sheet.getColumns(); j++) {

					try {
						Cell cell = sheet.getCell(j, row);
						String value = "";
						if (cell.getType() == CellType.DATE) {
							DateCell dc = (DateCell) cell;
							value = DateUtil.getSysDate(
									DateUtil.YYYYMMDDHHMMSS, dc.getDate());
						}
						else
						{
							value = cell.getContents();
						}
						//是母公司本期或上期的数据，不给系统中导入
						if(value.contains(StockConstants.PARENT_ABOVE)||value.contains(StockConstants.PARENT_BASE))
						{
							isValidateData = true;
							break;
						}
						String columnName = sheet.getCell(j, 0).getContents()
								.trim();
						StockUtil.setValue2VO(vo, value, columnName);
					} catch (Exception e) {
						logger.error("fetch data from excel failed!", e);
						throw e;
					}

				}
				//是母公司本期或上期的数据，不给系统中导入
				if(isValidateData) continue;
				//唐斌奇 2012-07-16 增加全量处理行业 数据库处理
				if (name.contains("0018")){
					CompanyIndustry v = (CompanyIndustry)vo;
					String obOrgid0018  = v.getObOrgid0018();//公司组织ID
					if(obOrgid0018 == null ) continue;
//					String f003v0018 = v.getF003v0018();//证监会行业编码,不准确
					String f007v0018 = v.getF007v0018();
					Industry industry = IndustryService.getInstance().getIndustryCSRCByName(f007v0018);
					Company company = CompanyService.getInstance().getCompanyByCodeFromCache(obOrgid0018);
//					company.setIndustryCode(industry.getIndustryCode());
//					company.setIndustryCode(f003v0018);
					CompanyService.getInstance().updateCompany(company);
					
				}
				//End 唐斌奇 2012-07-16 增加全量处理行业 数据库处理 
				
				//唐斌奇 2012-07-19 增加处理自己的<<industry_company.xls>> 批量
				if(name.contains("industry_company")){
					IndustryCompanyArray v = (IndustryCompanyArray)vo;
					String yfzxIndustryCode = v.getIndustryCode();
					List<String> companyNameArray = v.getCompanyList();
					
					boolean checkIsExist = IndustryService.getInstance().getIndustryYFZXTree().containsKey(yfzxIndustryCode);
					if(checkIsExist == false){
						continue;
					}
					for(String companyName : companyNameArray){
						Company getCompanyByName = CompanyService.getInstance().getCompanyByName(companyName);
						if(getCompanyByName == null ){
							logger.warn(companyName+ " 公司不存在");
							continue;//名称不在数据库
						}else{
//							getCompanyByName.setIndustryCodeYFZX(yfzxIndustryCode);
							CompanyService.getInstance().updateCompany(getCompanyByName);
						}
					}
					
				}
				//End 唐斌奇 2012-07-19 增加处理自己的<<industry_company.xls>> 批量
				
				if (name.contains("0290")) {
					Companyasset0290 v = (Companyasset0290) vo;
					if (!StockUtil.checkData(v.getF003v0290()))
						continue;
					v.setObModtime0290(Calendar.getInstance().getTime());
					// Companyasset0290DataService.getInstance().insert(v);
//					Companyasset0290 qv = Companyasset0290DataService
//							.getInstance().queryBySecid(v);
//					if (qv == null) {
					String key = DCenterUtil.getBaseTableVokey(v.getObOrgid0290(),v.getF001d0290());
					boolean isPresent = DCenterUtil.isPresentInDb(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_asset_0290));
					if(!isPresent)
					{
						DCenterUtil.add2BloomFilter(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_asset_0290));
						ExecuteQueueManager.add2IQueue(
								new BatchQueueEntity(StockConstants.TYPE_ASSET0290, StockConstants.TABLE_NAME_tb_company_asset_0290,v));
					} else {
//						if (StockUtil.compareUpdateVOEqualsQuv(v, qv))
//							continue;
						ExecuteQueueManager.add2UQueue(
								new BatchQueueEntity(StockConstants.TYPE_ASSET0290, StockConstants.TABLE_NAME_tb_company_asset_0290, v));
					}
				}
				if (name.contains("0291")) {
					Companyprofile0291 v = (Companyprofile0291) vo;
					if (!StockUtil.checkData(v.getF003v0291()))
						continue;
					v.setObModtime0291(Calendar.getInstance().getTime());
					// Companyprofile0291DataService.getInstance().insert(v);
//					Companyprofile0291 qv = Companyprofile0291DataService
//							.getInstance().queryBySecid(v);
//					if (qv == null) {
					String key = DCenterUtil.getBaseTableVokey(v.getObOrgid0291(),v.getF001d0291());
					boolean isPresent = DCenterUtil.isPresentInDb(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_profile_0291));
					if(!isPresent)
					{
						DCenterUtil.add2BloomFilter(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_profile_0291));
						ExecuteQueueManager.getInstance().add2IQueue(
								new BatchQueueEntity(StockConstants.TYPE_PROFILE0291, StockConstants.TABLE_NAME_tb_company_profile_0291, v));
					} else {
//						if (StockUtil.compareUpdateVOEqualsQuv(v, qv))
//							continue;
						ExecuteQueueManager.getInstance().add2UQueue(
								new BatchQueueEntity(StockConstants.TYPE_PROFILE0291, StockConstants.TABLE_NAME_tb_company_profile_0291, v));
					}
				}
				if (name.contains("0292")) {
					Companycash0292 v = (Companycash0292) vo;
					if (!StockUtil.checkData(v.getF003v0292()))
						continue;
					v.setObModtime0292(Calendar.getInstance().getTime());
					// Companycash0292DataService.getInstance().insert(v);
//					Companycash0292 qv = Companycash0292DataService
//							.getInstance().queryBySecid(v);
//					if (qv == null) {
					String key = DCenterUtil.getBaseTableVokey(v.getObOrgid0292(),v.getF001d0292());
					boolean isPresent = DCenterUtil.isPresentInDb(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_cash_0292));
					if(!isPresent)
					{
						DCenterUtil.add2BloomFilter(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_cash_0292));
						ExecuteQueueManager.getInstance().add2IQueue(
								new BatchQueueEntity(StockConstants.TYPE_CASH0292, StockConstants.TABLE_NAME_tb_company_cash_0292, v));
					} else {
//						if (StockUtil.compareUpdateVOEqualsQuv(v, qv))
//							continue;
						ExecuteQueueManager.getInstance().add2UQueue(
								new BatchQueueEntity(StockConstants.TYPE_CASH0292, StockConstants.TABLE_NAME_tb_company_cash_0292, v));
					}

				}
			} catch (Exception e) {
				logger.error("fetch data from excel failed!", e);
			}

		}
	}

	public void parse(Sheet sheet, File file) {

		for (int row = 1; row < sheet.getRows(); row++) {
			String name = file.getName();
			Object vo = getImportVO(name);
			if (vo == null)
				continue;

			for (int j = 0; j < sheet.getColumns(); j++) {

				String value = sheet.getCell(j, row).getContents();
				String columnName = sheet.getCell(j, 0).getContents();
				StockUtil.setValue2VO(vo, value, columnName);
			}

			if (name.contains("0290")) {
				Companyasset0290 v = (Companyasset0290) vo;
				if (!StockUtil.checkData(v.getF003v0290()))
					continue;
				Companyasset0290DataService.getInstance().insert(v);
			}

			if (name.contains("0291")) {
				Companyprofile0291 v = (Companyprofile0291) vo;
				if (!StockUtil.checkData(v.getF003v0291()))
					continue;

				Companyprofile0291DataService.getInstance().insert(v);
			}

			if (name.contains("0292")) {
				Companycash0292 v = (Companycash0292) vo;
				if (!StockUtil.checkData(v.getF003v0292()))
					continue;

				Companycash0292DataService.getInstance().insert(v);
			}

		}

	}

	private Object getImportVO(String name) {
		if (name.contains("0290"))
			return new Companyasset0290();
		if (name.contains("0291"))
			return new Companyprofile0291();
		if (name.contains("0292"))
			return new Companycash0292();
		//唐斌奇 2012-07-16 增加全量处理行业 增加行业类型
		if (name.contains("0018")){
			return new CompanyIndustry();
		}
		//唐斌奇 2012-07-19 增加全量处理行业 处理到yfzx的行业
		if (name.contains("industry_company")){
			return new IndustryCompanyArray();
		}//End 唐斌奇 2012-07-19 
		
		
		return null;
	}

	public void importAcessExportData(String path) {
		try {
			// 默认输入的是文件目录
			File fdir = new File(path);
			File[] fs = fdir.listFiles();
			for (File f : fs) {
				if (f.isDirectory()) {
					importAcessExportData(f.getPath());
					f.delete();
				} else {
					if (canImport(f)) {
						doImportStockInfoData(f);
						// f.delete();
					}
				}

			}

		} catch (Exception e) {
			logger.error("importAcessExportData failed!", e);
		}
	}

	private boolean canImport(File f) {
		// TODO Auto-generated method stub
		return (f.getName().endsWith(".xls") || f.getName().endsWith(".xlsx"));
//				&& !f.getName().contains("018");
	}
}
