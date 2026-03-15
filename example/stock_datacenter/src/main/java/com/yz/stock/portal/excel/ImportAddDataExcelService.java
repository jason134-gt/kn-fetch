package com.yz.stock.portal.excel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import com.yz.stock.portal.model.CompanyStockInfo;
import com.yz.stock.portal.model.Companyasset0290;
import com.yz.stock.portal.model.Companycash0292;
import com.yz.stock.portal.model.Companyprofile0291;
import com.yz.stock.portal.service.CompanyStockInfoDataService;
import com.yz.stock.portal.service.Companyasset0290DataService;
import com.yz.stock.portal.service.Companycash0292DataService;
import com.yz.stock.portal.service.Companyprofile0291DataService;
import com.yz.stock.util.DCenterUtil;
import com.yz.stock.util.ExecuteQueueManager;

public class ImportAddDataExcelService {

	int page = 5000;
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private static ImportAddDataExcelService instance = new ImportAddDataExcelService();
	String Code_071001 = "071001";
	String Code_033003 = "033003";
	String Code_033003_name = "定期报告";

	Set<String> notFoundCompanySet = new HashSet<String>();

	ImportAddDataExcelService() {

	}

	public static ImportAddDataExcelService getInstance() {
		return instance;
	}

	private void doExcelData(File file) throws BiffException, IOException {

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
				// parse(sheet, file);
				dispatch(sheet, file);
			}

		} catch (Exception e) {
			logger.error("import failed!", e);
		} finally {
//			printNotFoundCompany();
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

	private void printNotFoundCompany() {
		// TODO Auto-generated method stub
		for (String c : notFoundCompanySet) {
			logger.info("=======================not found the company stock info !!companyStockCode = "
					+ c);
		}
		notFoundCompanySet.clear();
	}

	public void doImport(Sheet sheet, File file, int pageIndex) {

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
				String companyStockCode = sheet.getCell(1, row).getContents()
						.trim();
				isValidateData = false;
				// 默认从第三列开始 行业的需要	从第1列开始			
				for (int j = 0/*2*/; j < sheet.getColumns(); j++) {
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
						
						String columnName = sheet.getCell(j, 0).getContents();
						StockUtil.setValue2VO(vo, value, columnName);

					} catch (Exception e) {
						logger.error("fetch data from excel failed!", e);
						continue;
					}
				}
				
				//是母公司本期或上期的数据，不给系统中导入
				if(isValidateData) continue;
				
				//唐斌奇增加 暂时用处不大，暂缓
				if(name.contains("行业")){
					CompanyIndustry v = (CompanyIndustry) vo;
					String obSeccode0007 = v.getObSeccode0007();
					if(obSeccode0007 == null ) continue;
					else{
						obSeccode0007 = obSeccode0007.trim();
					}
					
					//如果Company包含Stock_Code,则没有必要从Public_0007表中查询
					CompanyStockInfo retcsi = CompanyStockInfoDataService
							.getInstance().queryByCompanyStockCodeA2H(
									obSeccode0007);
					logger.info(companyStockCode + obSeccode0007);
					if(retcsi == null){
						if(!notFoundCompanySet.contains(companyStockCode))
							add2NotFoundCompanySet(companyStockCode);
						continue;
					}
					
					//根据最低一级的行业，获取行业编码
//					String f004v0018 = v.getF004v0018();
//					String f005v0018 = v.getF005v0018();
//					String f006v0018 = v.getF006v0018();
					String f007v0018 = v.getF007v0018();
					Industry industry = IndustryService.getInstance().getIndustryCSRCByName(f007v0018);
					
					String industryCode = industry.getIndustryCode();
					String obSecid0007 = retcsi.getObSecid0007();
					
					Company company = CompanyService.getInstance().getCompanyByCodeFromCache(obSecid0007);
//					company.setIndustryCode(industryCode);
					CompanyService.getInstance().updateCompany(company);
					
				}// end if(name.contains("行业"))
				
				if (name.contains("资产")) {
					Companyasset0290 v = (Companyasset0290) vo;
					if (!StockUtil.checkData(v.getF003v0290()))
						continue;
					// 用证券编码查机构编码
					CompanyStockInfo retcsi = CompanyStockInfoDataService
							.getInstance().queryByCompanyStockCodeA2H(
									companyStockCode);
					if (retcsi == null) {
						if(!notFoundCompanySet.contains(companyStockCode))
							notFoundCompanySet.add(companyStockCode);
						continue;
					}

					v.setObOrgid0290(retcsi.getObSecid0007());
					v.setObOrgname0290(retcsi.getF015v0007());
					v.setF002v0290(Code_071001);
					v.setF004v0290(Code_033003);
					v.setF005v0290(Code_033003_name);
					v.setObModtime0290(Calendar.getInstance().getTime());
//					Companyasset0290 retv = Companyasset0290DataService
//							.getInstance().queryBySecid(v);
//					if (retv != null) {
					String key = DCenterUtil.getBaseTableVokey(v.getObOrgid0290(),v.getF001d0290());
					boolean isPresent = DCenterUtil.isPresentInDb(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_asset_0290));
					if(isPresent)
					{
						// Companyasset0290 updatev = buildUpdateVo(v,retv);
						// 修改update sql
						// Companyasset0290DataService.getInstance().updateByCode(v);
//						if (StockUtil.compareUpdateVOEqualsQuv(v, retv))
//							continue;
						DCenterUtil.add2BloomFilter(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_cash_0292));
						ExecuteQueueManager.add2UQueue(
								new BatchQueueEntity(StockConstants.TYPE_ASSET0290, StockConstants.TABLE_NAME_tb_company_asset_0290, v));
					} else {
						// Companyasset0290DataService.getInstance().insertRedirect(v);
						ExecuteQueueManager.add2IQueue(
								new BatchQueueEntity(StockConstants.TYPE_ASSET0290, StockConstants.TABLE_NAME_tb_company_asset_0290, v));
					}

				}
				if (name.contains("利润")) {
					Companyprofile0291 v = (Companyprofile0291) vo;
					if (!StockUtil.checkData(v.getF003v0291()))
						continue;
					CompanyStockInfo retcsi = CompanyStockInfoDataService
							.getInstance().queryByCompanyStockCodeA2H(
									companyStockCode);
					if (retcsi == null) {
						if(!notFoundCompanySet.contains(companyStockCode))
							notFoundCompanySet.add(companyStockCode);
						continue;
					}

					v.setObOrgid0291(retcsi.getObSecid0007());
					v.setObOrgname0291(retcsi.getF015v0007());
					v.setF002v0291(Code_071001);
					v.setF004v0291(Code_033003);
					v.setF005v0291(Code_033003_name);
					v.setObModtime0291(Calendar.getInstance().getTime());
//					Companyprofile0291 retv = Companyprofile0291DataService
//							.getInstance().queryBySecid(v);
//					if (retv != null) {
					String key = DCenterUtil.getBaseTableVokey(v.getObOrgid0291(),v.getF001d0291());
					boolean isPresent = DCenterUtil.isPresentInDb(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_profile_0291));
					if(isPresent)
					{
						// Companyprofile0291DataService.getInstance().updateByCode(v);
//						if (StockUtil.compareUpdateVOEqualsQuv(v, retv))
//							continue;
						DCenterUtil.add2BloomFilter(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_profile_0291));
						ExecuteQueueManager.getInstance().add2UQueue(
								new BatchQueueEntity(StockConstants.TYPE_PROFILE0291, StockConstants.TABLE_NAME_tb_company_profile_0291, v));
					} else {
						// Companyprofile0291DataService.getInstance().insertRedirect(v);
						ExecuteQueueManager.getInstance().add2IQueue(
								new BatchQueueEntity(StockConstants.TYPE_PROFILE0291, StockConstants.TABLE_NAME_tb_company_profile_0291, v));
					}

					// Companyprofile0291DataService.getInstance().insert(v);
				}
				if (name.contains("现金")) {
					Companycash0292 v = (Companycash0292) vo;
					if (!StockUtil.checkData(v.getF003v0292()))
						continue;

					CompanyStockInfo retcsi = CompanyStockInfoDataService
							.getInstance().queryByCompanyStockCodeA2H(
									companyStockCode);
					if (retcsi == null) {
						if(!notFoundCompanySet.contains(companyStockCode))
							notFoundCompanySet.add(companyStockCode);
						continue;
					}

					v.setObOrgid0292(retcsi.getObSecid0007());
					v.setObOrgname0292(retcsi.getF015v0007());
					v.setF002v0292(Code_071001);
					v.setF004v0292(Code_033003);
					v.setF005v0292(Code_033003_name);
					v.setObModtime0292(Calendar.getInstance().getTime());
//					Companycash0292 retv = Companycash0292DataService
//							.getInstance().queryBySecid(v);
//					if (retv != null) {
					String key = DCenterUtil.getBaseTableVokey(v.getObOrgid0292(),v.getF001d0292());
					boolean isPresent = DCenterUtil.isPresentInDb(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_cash_0292));
					if(isPresent)
					{
						// Companycash0292DataService.getInstance().updateByCode(v);
//						if (StockUtil.compareUpdateVOEqualsQuv(v, retv))
//							continue;
						DCenterUtil.add2BloomFilter(StockUtil.getBaseVoBFKey(key, StockConstants.TABLE_NAME_tb_company_cash_0292));
						ExecuteQueueManager.getInstance().add2UQueue(
								new BatchQueueEntity(StockConstants.TYPE_CASH0292, StockConstants.TABLE_NAME_tb_company_cash_0292, v));
					} else {
						// Companycash0292DataService.getInstance().insertRedirect(v);
						ExecuteQueueManager.getInstance().add2IQueue(
								new BatchQueueEntity(StockConstants.TYPE_CASH0292, StockConstants.TABLE_NAME_tb_company_cash_0292, v));
					}

					// Companycash0292DataService.getInstance().insert(v);
				}//end if (name.contains("现金")) 
				
			} catch (Exception e) {
				logger.error("fetch data from excel failed!", e);
			}

		}

	}

	private void add2NotFoundCompanySet(String companyStockCode) {
		logger.info("=======================not found the company stock info !!companyStockCode = "
				+ companyStockCode);
		notFoundCompanySet.add(companyStockCode);
	}

	public void parse(Sheet sheet, File file) {

		for (int row = 1; row < sheet.getRows(); row++) {
			String name = file.getName();
			Object vo = getImportVO(name);
			if (vo == null)
				continue;
			String companyStockCode = sheet.getCell(1, row).getContents()
					.trim();
			// 默认从第第三列开始
			for (int j = 2; j < sheet.getColumns(); j++) {

				Cell cell = sheet.getCell(j, row);

				String value = sheet.getCell(j, row).getContents();
				if (cell.getType() == CellType.DATE) {
					DateCell dc = (DateCell) cell;
					value = DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
							dc.getDate());
				}

				String columnName = sheet.getCell(j, 0).getContents();
				StockUtil.setValue2VO(vo, value, columnName);
			}

			if (name.contains("资产")) {
				Companyasset0290 v = (Companyasset0290) vo;
				if (!StockUtil.checkData(v.getF003v0290()))
					continue;
				// 用证券编码查机构编码
				CompanyStockInfo retcsi = CompanyStockInfoDataService
						.getInstance().queryByCompanyStockCodeA2H(companyStockCode);
				if (retcsi == null) {
					if(!notFoundCompanySet.contains(companyStockCode))
						add2NotFoundCompanySet(companyStockCode);
					continue;
				}

				v.setObOrgid0290(retcsi.getObSecid0007());
				v.setObOrgname0290(retcsi.getF015v0007());
				v.setF002v0290(Code_071001);
				v.setF004v0290(Code_033003);
				v.setF005v0290(Code_033003_name);
				v.setObModtime0290(Calendar.getInstance().getTime());
				Companyasset0290 retv = Companyasset0290DataService
						.getInstance().queryBySecid(v);
				if (retv != null) {
					// Companyasset0290 updatev = buildUpdateVo(v,retv);
					// 修改update sql
					Companyasset0290DataService.getInstance().updateByCode(v);
				} else {
					Companyasset0290DataService.getInstance().insertRedirect(v);
				}

			}

			if (name.contains("利润")) {
				Companyprofile0291 v = (Companyprofile0291) vo;
				if (!StockUtil.checkData(v.getF003v0291()))
					continue;
				CompanyStockInfo retcsi = CompanyStockInfoDataService
						.getInstance().queryByCompanyStockCodeA2H(companyStockCode);
				if (retcsi == null) {
					if(!notFoundCompanySet.contains(companyStockCode))
						notFoundCompanySet.add(companyStockCode);
					continue;
				}

				v.setObOrgid0291(retcsi.getObSecid0007());
				v.setObOrgname0291(retcsi.getF015v0007());
				v.setF002v0291(Code_071001);
				v.setF004v0291(Code_033003);
				v.setF005v0291(Code_033003_name);
				v.setObModtime0291(Calendar.getInstance().getTime());
				Companyprofile0291 retv = Companyprofile0291DataService
						.getInstance().queryBySecid(v);
				if (retv != null) {
					Companyprofile0291DataService.getInstance().updateByCode(v);
				} else {
					Companyprofile0291DataService.getInstance().insertRedirect(
							v);
				}

				// Companyprofile0291DataService.getInstance().insert(v);
			}

			if (name.contains("现金")) {
				Companycash0292 v = (Companycash0292) vo;
				if (!StockUtil.checkData(v.getF003v0292()))
					continue;

				CompanyStockInfo retcsi = CompanyStockInfoDataService
						.getInstance().queryByCompanyStockCodeA2H(companyStockCode);
				if (retcsi == null) {
					if(!notFoundCompanySet.contains(companyStockCode))
						notFoundCompanySet.add(companyStockCode);
					continue;
				}

				v.setObOrgid0292(retcsi.getObSecid0007());
				v.setObOrgname0292(retcsi.getF015v0007());
				v.setF002v0292(Code_071001);
				v.setF004v0292(Code_033003);
				v.setF005v0292(Code_033003_name);
				v.setObModtime0292(Calendar.getInstance().getTime());
				Companycash0292 retv = Companycash0292DataService.getInstance()
						.queryBySecid(v);
				if (retv != null) {
					Companycash0292DataService.getInstance().updateByCode(v);
				} else {
					Companycash0292DataService.getInstance().insertRedirect(v);
				}

				// Companycash0292DataService.getInstance().insert(v);
			}

		}

	}

	// private <T>T buildUpdateVo(Object v1,
	// Object v2) {
	// try {
	// Class clazz1 = v1.getClass();
	// Class clazz2 = v2.getClass();
	// Field[] f1 = clazz1.getDeclaredFields();
	// for (Field tf : f1) {
	// try {
	// String tfieldName = tf.getName();
	// Method getMethod = clazz1.getDeclaredMethod("get"
	// + tfieldName.substring(0, 1).toUpperCase()
	// + tfieldName.substring(1));
	// Method setMethod = clazz2.getDeclaredMethod("set"
	// + tfieldName.substring(0, 1).toUpperCase()
	// + tfieldName.substring(1), tf.getType());
	// //用get方法取get值
	// Object gValue = getMethod.invoke(v1);
	// if (gValue == null)
	// continue;
	//
	// Method getMethod2 = clazz2.getDeclaredMethod("get"
	// + tfieldName.substring(0, 1).toUpperCase()
	// + tfieldName.substring(1));
	// Object gValue2 = getMethod.invoke(v2);
	// if(gValue.equals(gValue2)) continue;
	// //把取到的值赋给新对象的set方法
	// setMethod.invoke(v2, StockUtil.getValueByType(tf.getType()
	// .getSimpleName(), gValue.toString()));
	// } catch (Exception e) {
	// // TODO: handle exception
	// }
	// }
	// } catch (Exception e) {
	// logger.error("buildUpdateVo falied!",e);
	// }
	// return (T)v2;
	// }

	private Object getImportVO(String name) {
		if (name.contains("资产"))
			return new Companyasset0290();
		if (name.contains("利润"))
			return new Companyprofile0291();
		if (name.contains("现金"))
			return new Companycash0292();
		//唐斌奇 2012-07-02 增加 
		if (name.contains("行业")){
			return new CompanyIndustry();
		}
		return null;
	}

	public void importExportData(String path) {
		try {
			// 默认输入的是文件目录
			File fdir = new File(path);
			File[] fs = fdir.listFiles();

			for (File f : fs) {
				if (f.isDirectory()) {
					importExportData(f.getPath());
					f.delete();
				}

				if (f.getName().endsWith(".xls")) {
					if(f.getName().contains("行业"))
					{
						f.delete();
						continue;
					}
					logger.info("import file fileName =" + f.getName());
					doExcelData(f);
					//f.delete();
				}

			}

		} catch (Exception e) {
			logger.error("importAcessExportData failed!", e);
		}

	}

	private void doUnZipExcelData(File f, String path) {
		try {
			int BUFFER = 1024;
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(f);
			ZipInputStream zis = new ZipInputStream(
					new BufferedInputStream(fis));
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				System.out.println("Extracting: " + entry);
				int count;
				byte data[] = new byte[1024];
				FileOutputStream fos = new FileOutputStream(path);
				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
