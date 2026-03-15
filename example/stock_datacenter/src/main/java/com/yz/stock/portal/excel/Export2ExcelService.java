package com.yz.stock.portal.excel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jxl.Cell;
import jxl.CellType;
import jxl.DateCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Industry;
import com.stock.common.model.base.Asset0290;
import com.stock.common.model.base.Bxasset;
import com.stock.common.model.base.Bxcash;
import com.stock.common.model.base.Bxprofile;
import com.stock.common.model.base.Cash0292;
import com.stock.common.model.base.Profile0291;
import com.stock.common.model.base.Yhasset;
import com.stock.common.model.base.Yhcash;
import com.stock.common.model.base.Yhprofile;
import com.stock.common.model.base.Zjasset;
import com.stock.common.model.base.Zjcash;
import com.stock.common.model.base.Zjprofile;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.TreeNode;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndustryService;
import com.yz.stock.portal.model.CompanyStockInfo;
import com.yz.stock.portal.model.Companyasset0290;
import com.yz.stock.portal.model.Companycash0292;
import com.yz.stock.portal.model.Companyprofile0291;
import com.yz.stock.portal.service.BxDataService;
import com.yz.stock.portal.service.CompanyStockInfoDataService;
import com.yz.stock.portal.service.Companyasset0290DataService;
import com.yz.stock.portal.service.Companycash0292DataService;
import com.yz.stock.portal.service.Companyprofile0291DataService;
import com.yz.stock.portal.service.YhDataService;
import com.yz.stock.portal.service.ZjDataService;

public class Export2ExcelService {

	int page = 5000;
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private static Export2ExcelService instance = new Export2ExcelService();
	String Code_071001 = "071001";
	String Code_033003 = "033003";
	String Code_033003_name = "定期报告";

	List<String> notFoundCompanyList = new ArrayList<String>();

	Export2ExcelService() {

	}

	public static Export2ExcelService getInstance() {
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
				parse(sheet, file);
			}

		} catch (Exception e) {
			logger.error("import failed!", e);
		} finally {
			printNotFoundCompany();
			wb.close();
			System.gc();

			while (Runtime.getRuntime().totalMemory() > 1024 * 1024 * 800) {
				try {
					System.out
							.println("==============================================================================sleep");
					wb.close();
					System.gc();
					logger.info("==============================================================================sleep");
					Thread.sleep(10000l);
				} catch (Exception e2) {
					logger.error("gc failed!", e2);
				}
			}
		}

	}

	private void printNotFoundCompany() {
		// TODO Auto-generated method stub
		for (String c : notFoundCompanyList) {
			logger.info("=======================not found the company stock info !!companyStockCode = "
					+ c);
		}
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
					notFoundCompanyList.add(companyStockCode);
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
				if (!StockUtil.checkData(v.getF003v0291().trim()))
					continue;
				CompanyStockInfo retcsi = CompanyStockInfoDataService
						.getInstance().queryByCompanyStockCodeA2H(companyStockCode);
				if (retcsi == null) {
					notFoundCompanyList.add(companyStockCode);
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
					notFoundCompanyList.add(companyStockCode);
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

	
	private Object getImportVO(String name) {
		if (name.contains("资产"))
			return new Companyasset0290();
		if (name.contains("利润"))
			return new Companyprofile0291();
		if (name.contains("现金"))
			return new Companycash0292();
		return null;
	}

	public void exportData2File(String exportPath, String companyCode, Integer type) {

		try {
			if (!exportPath.endsWith("\\")) {
				exportPath += "\\";
			}
			Company retc = CompanyService.getInstance().getCompanyByCodeFromCache(companyCode);
			if (retc == null) {
				logger.error("not found the company where companycode = "
						+ companyCode);
				return;
			}
			String ePath = exportPath + retc.getSimpileName();
			File edir = creatdir(ePath);
			if (edir == null) {
				logger.error("create file failed! name = " + ePath);
				return;
			}

			doExportByType(retc,edir,type);
			

		} catch (Exception e) {
			logger.error("failed !",e);
		}

	}
	
	
	/**
	 * 导出《industry_company.xls》
	 * 唐斌奇 增加 2012-07-19
	 * @param exportPath
	 */
	public void exportData2FileOnIndustry( String exportPath ){
		
		try{
			if (!exportPath.endsWith(File.separator)) {
				exportPath += File.separator;
			}
			
			File ifile = new File(exportPath +  "industry_company.xls");
			if (ifile.exists())
			{
				ifile.delete();				
			}
			ifile.createNewFile();
			
			//获取数据
			HashMap<String,String> mappingCSRC2YFZX = this.mappingCSRC2YFZX();			
			List<Industry> yfzxArr = IndustryService.getInstance().getAllIndustryYFZX();
			HashMap<String,List<Company>> indOnComMap = new HashMap<String,List<Company>>();//存储YFZX行业关联的公司
			for(Industry yfzxIndustry : yfzxArr){
				List<Company> comArr = new ArrayList<Company>();
				indOnComMap.put(yfzxIndustry.getIndustryCode(), comArr);
			}
			indOnComMap.put("", new ArrayList<Company>());
			
			List<Company> companyArr = CompanyService.getInstance().getCompanyList();
			for(Company company : companyArr){				
//				String yfzxIndustryCode =  company.getIndustryCodeYFZX() ;
//				if(yfzxIndustryCode == null || "".equals(yfzxIndustryCode.trim())){
//					yfzxIndustryCode = mappingCSRC2YFZX.get(company.getIndustryCode());//没有YFZX行业时,使用证劵会的映射
//				}
//				if(yfzxIndustryCode == null || indOnComMap.get(yfzxIndustryCode)==null){
//					indOnComMap.get("").add(company);
//				}else{
//					indOnComMap.get(yfzxIndustryCode).add(company);
//				}
				
			}
			
			//写Excel文件
			WorkbookSettings wbSetting = new WorkbookSettings();
			wbSetting.setUseTemporaryFileDuringWrite(true);
			wbSetting.setGCDisabled(false);
			WritableWorkbook wwb = Workbook.createWorkbook(ifile, wbSetting);
			
			if (wwb != null) {
				// 创建一个可写入的工作表
				// Workbook的createSheet方法有两个参数，第一个是工作表的名称，第二个是工作表在工作薄中的位置
				WritableSheet ws = wwb.createSheet("sheet1", 0);
				ws.setColumnView(1,20);
				ws.setColumnView(2,600);
				
				//需要增加表头
				Label topt = new Label( 0 ,0, "Industry_Code");
				ws.addCell(topt);
				topt = new Label( 1 ,0, "Name");
				ws.addCell(topt);
				topt = new Label( 2 ,0, "Company_Array");
				ws.addCell(topt);
				
				for(int i=0;i<yfzxArr.size();i++){
					Industry yfzxIndustry = yfzxArr.get(i);
					List<Company> comArr = indOnComMap.get(yfzxIndustry.getIndustryCode());
					
					Label tl = new Label( 0 ,i + 1, yfzxIndustry.getIndustryCode());
					ws.addCell(tl);
					Label t2 = new Label( 1 ,i + 1, yfzxIndustry.getName());
					ws.addCell(t2);
					StringBuffer value3 = new StringBuffer();
					for(Company company :comArr){
						value3.append(company.getSimpileName()+";");
					}
					WritableCellFormat wcf = new WritableCellFormat();
					wcf.setWrap(true);//自动换行 					
					Label t3 = new Label(2 ,i + 1, value3.toString(),wcf);
					ws.addCell(t3);
				}
				
				//未在行业内的写文件
				List<Company> comArr = indOnComMap.get("");
				StringBuffer value3 = new StringBuffer();
				for(Company company :comArr){
					value3.append(company.getSimpileName()+";");
				}
				WritableCellFormat wcf = new WritableCellFormat();
				wcf.setWrap(true);//自动换行 					
				Label t3 = new Label(2 ,yfzxArr.size() + 1, value3.toString(),wcf);
				ws.addCell(t3);
				
				
				wwb.write();
				// 关闭资源，释放内存
				wwb.close();
			}
		}catch (Exception e) {
			logger.error("failed !",e);
		}		
	}
	
	/**
	 * 从CSRC到YFZX的行业映射关系
	 * @return
	 */
	private HashMap<String,String> mappingCSRC2YFZX(){
		HashMap<String,TreeNode> csrcTreeMap = IndustryService.getInstance().getIndustryCSRCTree();
		HashMap<String,TreeNode> yfzxTreeMap = IndustryService.getInstance().getIndustryYFZXTree();
		HashMap<String,String> returnMap = new HashMap<String,String>();
		
		//先全部置为null
		List<Industry> csrcIndArr = IndustryService.getInstance().getAllIndustryCSRC();
		for(Industry csrcIndustry : csrcIndArr){
			returnMap.put(csrcIndustry.getIndustryCode(), null);
		}
		
		List<TreeNode> levelOneCSRCTreeNode = csrcTreeMap.get("").getChildren();
		List<TreeNode> levelOneYFZXTreeNode = yfzxTreeMap.get("").getChildren();		
		findMapping(returnMap,levelOneCSRCTreeNode,levelOneYFZXTreeNode);
		doNullMapping(returnMap,csrcTreeMap,yfzxTreeMap);
		return returnMap;
	}
	
	/**
	 * 处理没有映射关系的行业，找到上层的映射
	 * @param returnMap
	 * @param csrcTreeMap
	 * @param yfzxTreeMap
	 */
	@SuppressWarnings("rawtypes")
	private void doNullMapping(HashMap<String,String> returnMap,HashMap<String,TreeNode> csrcTreeMap,HashMap<String,TreeNode> yfzxTreeMap){
		Map doMap = (Map)returnMap.clone();
		for(Iterator iter = doMap.entrySet().iterator();iter.hasNext();){
			Map.Entry entry = (Map.Entry)iter.next();
			if(entry.getValue()==null){
				TreeNode csrcNode = csrcTreeMap.get(entry.getKey()).getParent();
				while(csrcNode.getLevel()!=0){
					String parentCode = ((Industry)csrcNode.getReference()).getIndustryCode();
					if(returnMap.get(parentCode)!= null){						
						returnMap.put((String)entry.getKey(),returnMap.get(parentCode));
						break;//找到即退出
					}else{
						csrcNode = csrcNode.getParent();
					}
				}				
			}
		}
	}
	
	/**
	 * 找到同层次的映射关系
	 * @param returnMap
	 * @param levelCSRCTreeNodeArr
	 * @param levelYFZXTreeNodeArr
	 */
	private void findMapping(HashMap<String,String> returnMap,List<TreeNode> levelCSRCTreeNodeArr,List<TreeNode> levelYFZXTreeNodeArr){
		for(TreeNode csrcNode :levelCSRCTreeNodeArr){
			Industry csrcIndustry = (Industry)csrcNode.getReference();
			for(TreeNode yfzxNode :levelYFZXTreeNodeArr){				
				Industry yfzxIndustry = (Industry)yfzxNode.getReference();
				if(csrcIndustry.getName().equals(yfzxIndustry.getName())){
					returnMap.put(csrcIndustry.getIndustryCode(), yfzxIndustry.getIndustryCode());
					//递归处理
					List<TreeNode> levelNextCSRCTreeNodeArr = csrcNode.getChildren();
					List<TreeNode> leveloneYFZXTreeNodeArr = yfzxNode.getChildren();
					findMapping(returnMap,levelNextCSRCTreeNodeArr,leveloneYFZXTreeNodeArr);
					break;
				}else{
					continue;//跳过
				}
			}
		}
	}

	private void doExportByType(Company cp, File edir, int type) {
		try {
			// TODO Auto-generated method stub
			List retList = getDataList(type, cp.getCompanyCode());
			if (retList != null && retList.size() > 0) {
				WorkbookSettings wbSetting = new WorkbookSettings();
				wbSetting.setUseTemporaryFileDuringWrite(true);
				wbSetting.setGCDisabled(false);
				File ifile = new File(getFileName(edir.getAbsolutePath(),cp.getStockCode(),type));
				if (ifile.exists())
				{
					ifile.delete();
					ifile.createNewFile();
				}
				
				WritableWorkbook wwb = Workbook
						.createWorkbook(ifile, wbSetting);
				if (wwb != null) {
					// 创建一个可写入的工作表
					// Workbook的createSheet方法有两个参数，第一个是工作表的名称，第二个是工作表在工作薄中的位置
					WritableSheet ws = wwb.createSheet("sheet1", 0);
					List<Dictionary> columnList = DictService.getInstance()
							.getDictListByTableCode(getTCCodeByType(type));

					// 加上列名指标
					addColumn(columnList, ws);

					// 从第二列开始
					for (int c = 0; c < retList.size(); c++) {

						Object vo = retList.get(c);
						// 第行写上时间
						Label labelC = new Label(c + 1, 0, DateUtil.getSysDate(
								DateUtil.YYYYMMDD, getVoDate(type, vo)));
						ws.addCell(labelC);
						int row = 0;
						// 从第三行开始
						for (Dictionary d : columnList) {
							try {

								if (d.getType() == StockConstants.INDEX_TYPE_01)
									continue;

								String value = StockUtil.getStringValueFromVO(
										vo, d.getColumnName());
								Label tl = new Label(c + 1, row + 3, value);

								ws.addCell(tl);
								row++;
							} catch (RowsExceededException e) {
								e.printStackTrace();
							} catch (WriteException e) {
								e.printStackTrace();
							}

						}
					}
					try {
						// 从内存中写入文件中
						wwb.write();
						// 关闭资源，释放内存
						wwb.close();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (WriteException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getTCCodeByType(int type) {
		if(type==0) return StockConstants.sct_0010;
		if(type==1) return StockConstants.sct_0011;
		if(type==2) return StockConstants.sct_0012;
		
		if(type==3) return "sct_0014";
		if(type==4) return "sct_0015";
		if(type==5) return "sct_0016";
		if(type==6) return "sct_0017";
		if(type==7) return "sct_0018";
		if(type==8) return "sct_0019";
		if(type==9) return "sct_0020";
		if(type==10) return "sct_0021";
		if(type==11) return "sct_0022";
		return null;
	}

	private String getFileName(String prefixPath,String stockCode,int type) {
		String name = "";
		if(type==0)
		{
			name = prefixPath + "\\资产负债表_"+type+"_"
					+ stockCode + ".xls";
		}
		if(type==1)
		{
			name = prefixPath + "\\利润表_"+type+"_"
					+ stockCode + ".xls";
		}
		if(type==2)
		{
			name = prefixPath + "\\现金流量表_"+type+"_"
					+ stockCode + ".xls";
		}
		
		if(type==3)
		{
			name = prefixPath + "\\资产负债表_"+type+"_"
					+ stockCode + ".xls";
		}
		if(type==4)
		{
			name = prefixPath + "\\利润表_"+type+"_"
					+ stockCode + ".xls";
		}
		if(type==5)
		{
			name = prefixPath + "\\现金流量表_"+type+"_"
					+ stockCode + ".xls";
		}
		
		if(type==6)
		{
			name = prefixPath + "\\资产负债表_"+type+"_"
					+ stockCode + ".xls";
		}
		if(type==7)
		{
			name = prefixPath + "\\利润表_"+type+"_"
					+ stockCode + ".xls";
		}
		if(type==8)
		{
			name = prefixPath + "\\现金流量表_"+type+"_"
					+ stockCode + ".xls";
		}
		
		if(type==9)
		{
			name = prefixPath + "\\资产负债表_"+type+"_"
					+ stockCode + ".xls";
		}
		if(type==10)
		{
			name = prefixPath + "\\利润表_"+type+"_"
					+ stockCode + ".xls";
		}
		if(type==11)
		{
			name = prefixPath + "\\现金流量表_"+type+"_"
					+ stockCode + ".xls";
		}
		return name;
	}

	private Date getVoDate(int type,Object v) {
		Date d = null;
		if(type==0)
		{
			Asset0290 vo = (Asset0290) v;
			d = vo.getTime();
		}
		
		if(type==1)
		{
			Profile0291 vo = (Profile0291) v;
			d = vo.getTime();
		}
		
		if(type==2)
		{
			Cash0292 vo = (Cash0292) v;
			d = vo.getTime();
		}
		
		if(type==3)
		{
			Yhasset vo = (Yhasset) v;
			d = vo.getTime();
		}
		
		if(type==4)
		{
			Yhprofile vo = (Yhprofile) v;
			d = vo.getTime();
		}
		
		if(type==5)
		{
			Yhcash vo = (Yhcash) v;
			d = vo.getTime();
		}
		
		if(type==6)
		{
			Bxasset vo = (Bxasset) v;
			d = vo.getTime();
		}
		
		if(type==7)
		{
			Bxprofile vo = (Bxprofile) v;
			d = vo.getTime();
		}
		
		if(type==8)
		{
			Bxcash vo = (Bxcash) v;
			d = vo.getTime();
			
		}
		
		if(type==9)
		{
			Zjasset vo = (Zjasset) v;
			d = vo.getTime();
			
		}
		
		if(type==10)
		{
			Zjprofile vo = (Zjprofile) v;
			d = vo.getTime();
			
		}
		
		if(type==11)
		{
			Zjcash vo = (Zjcash) v;
			d = vo.getTime();
		}
		
		
		return d;
	}

	private List getDataList(int type,String companycode) {
		List cl =  null;
		if(type==0)
		{
			 cl = Companyasset0290DataService
						.getInstance().queryAsset0290ListByCompanyCode(companycode);
		}
		
		if(type==1)
		{
			 cl = Companyprofile0291DataService
						.getInstance().queryProfile0291ListByCompanycode(companycode);
		}
		
		if(type==2)
		{
			 cl = Companycash0292DataService
						.getInstance().queryCash0292ListByCompanycode(companycode);
		}
		if(type==3)
		{
			 cl = YhDataService
						.getInstance().queryYhAssetByCompanycode(companycode);
		}
		if(type==4)
		{
			 cl = YhDataService
						.getInstance().queryYhProfileByCompanycode(companycode);
		}
		if(type==5)
		{
			 cl = YhDataService
						.getInstance().queryYhCashByCompanycode(companycode);
		}
		if(type==6)
		{
			 cl = BxDataService
						.getInstance().queryBxAssetByCompanycode(companycode);
		}
		if(type==7)
		{
			 cl = BxDataService
						.getInstance().queryBxProfileByCompanycode(companycode);
		}
		if(type==8)
		{
			 cl = BxDataService
						.getInstance().queryBxCashByCompanycode(companycode);
		}
		if(type==9)
		{
			 cl = ZjDataService
						.getInstance().queryZjAssetByCompanycode(companycode);
		}
		if(type==10)
		{
			 cl = ZjDataService
						.getInstance().queryZjProfileByCompanycode(companycode);
		}
		if(type==11)
		{
			 cl = ZjDataService
						.getInstance().queryZjCashByCompanycode(companycode);
		}
	
		return cl ;
	}

	private void addColumn(List<Dictionary> columnList, WritableSheet ws) {
		int row = 0;
		for (Dictionary d : columnList) {
			try {
			
				if(d.getType()==StockConstants.INDEX_TYPE_01) continue;
				
				Label tl = new Label(0, row+3, d.getColumnChiName());

				ws.addCell(tl);
				row++;
			} catch (RowsExceededException e) {
				e.printStackTrace();
			} catch (WriteException e) {
				e.printStackTrace();
			}
		}

	}

	private File creatdir(String fpath) {

		File f = new File(fpath);
		if (!f.exists())
			f.mkdir();
		// TODO Auto-generated method stub
		return f;
	}

}
