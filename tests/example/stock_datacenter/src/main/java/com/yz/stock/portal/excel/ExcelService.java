package com.yz.stock.portal.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.plug.IPlugIn;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockException;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.service.index.IndexDCService;
import com.stock.common.util.DateUtil;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class ExcelService implements IPlugIn {

	Logger logger = LoggerFactory.getLogger(this.getClass());
	private static ExcelService instance = new ExcelService();
	private static Map<String, IExcelParse> _eMap = new ConcurrentHashMap<String, IExcelParse>();
	static String sheetPrefix = "sheet_";
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static String bakPath = "";
	String fdirName;// 本次文件导入的备份目录文件名
	private String _fpostfix = ".xls";// 要导入的文件后缀
	AtomicInteger ai = new AtomicInteger(0);
	private String failedDirName = "failed";

	public ExcelService() {

	}

	public static ExcelService getInstance() {
		return instance;
	}

	public void importData2Db(String filePath) {
		try {
			importData2Db(new File(filePath));
		} catch (Exception e) {
			// TODO: handle exception
			logger.error(
					"import data from excel to database failed! filePath = "
							+ filePath, e);
		}
	}

	private String importData2Db(File file) throws NumberFormatException,
			StockException, BiffException, IOException {
		String ret = StockCodes.SUCCESS;
		
		Workbook wb =  Workbook.getWorkbook(file);

		try {
			// 默认取import下的下级目录名做为id
			String[] pa = file.getAbsolutePath().split("\\\\");
			String id = getImportConfigId(pa);
			if (id.isEmpty()) {
				return ret;
			}
			// 取所有sheet
			Sheet[] sheets = wb.getSheets();
			for (Sheet sheet : sheets) {
				IExcelParse ep = getExcelParse_v2(sheet, id);
				if (ep == null) {
					continue;
				}
				ep.parse(sheet, id, file);
			}
			
		} catch (Exception e) {
			logger.error("import failed!",e);
			ret = StockCodes.FAILED;
		}
		finally
		{
			wb.close();
		}
		return ret;

	}

	// 默认取import下的下级目录名做为id
	private String getImportConfigId(String[] pa) {
		for (int i = 0; i < pa.length; i++) {
			String n = pa[i];
			if (n.trim().equals("import")) {
				return pa[i + 1];
			}
		}
		return "";
	}

	private IExcelParse getExcelParse(Sheet sheet, String id) {
		// TODO Auto-generated method stub
		String name = id + "." + sheet.getName();
		String parseName = "";
		Object o = StockFactory.get_pMap().get(name);
		if (o == null) {
			return null;
		} else {
			parseName = (String) o;
		}

		return _eMap.get(parseName);
	}

	String assetName = "资产负债表";
	String cashName = "现金流量表";
	String profileName = "利润表";

	private IExcelParse getExcelParse_v2(Sheet sheet, String id) {
		// TODO Auto-generated method stub
		String name = sheet.getName();
		String parseName = "";
		if (name.contains(assetName)) {
			name = assetName;
		}
		if (name.contains(cashName)) {
			name = cashName;
		}
		if (name.contains(profileName)) {
			name = profileName;
		}
		String key = id + "." + name;
		Object o = StockFactory.get_pMap().get(key);
		if (o == null) {
			return null;
		} else {
			parseName = (String) o;
		}

		return _eMap.get(parseName);
	}

	public void plugIn() {
		// TODO Auto-generated method stub
		_eMap.put(StockConstants.COLUMN_EXCEL_PARSE, new ColumnExcelParse());
		_eMap.put(StockConstants.COLUMN_EXCEL_PARSE_OF_WIND,
				new ColumnExcelParseOfWind());
		_eMap.put(StockConstants.ExcelParseOfWind_V2, new ExcelParseOfWind_V2());
		initParseMap();
	}

	private void initParseMap() {
		// TODO Auto-generated method stub

		Configuration c = BaseFactory.getConfiguration();
		// 取导入文件存放的文件夹
		bakPath = c.getString("stock.bakPath");
		// List sl = c.getList("stock.table[@sheetName]");
		// for (int i = 0; i < sl.size(); i++) {
		// String sheetName = c
		// .getString("stock.table(" + i + ")[@sheetName]");
		// String parseName = c
		// .getString("stock.table(" + i + ")[@parseName]");
		// StockFactory.getMyRegister().register(sheetName, parseName);
		// }

	}

	public void updateData2Db(String companyCode, String tableName,
			String colName, String indexValue, Date time) {
		IndexDCService.getInstance().updateBaseIndex2Db(companyCode, tableName,
				colName, indexValue, time);

	}

	public void importAllFile() {
		try {
			// 创建备份根目录
			String bakVersion = DateUtil.getSysDate(DateUtil.YYYYMMDD)
					+ ai.getAndIncrement() ;
			String rootBakDir = createBakdDir(BaseFactory.getFilePath("\\file\\bak"), bakVersion);
			String importPath = "\\file\\import\\";
			File f = new File(BaseFactory.getFilePath(importPath));
			File[] lf = f.listFiles();
			for (File tf : lf) {

				String[] ta = tf.getAbsolutePath().split("\\\\");
				String d = ta[ta.length - 1];
				ai.set(0);
				// 创建备份目录
				String bakDir = createBakdDir(rootBakDir, d);
				String failedDir = createBakdDir(rootBakDir, failedDirName);
				// 导入文件夹下的所有文件
				importFile(BaseFactory.getFilePath(importPath + d + "\\"),
						bakDir, failedDir);
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("file import failed!", e);
		}
	}

	private boolean createBakDir() {
		fdirName = BaseFactory.getFilePath(bakPath)
				+ DateUtil.getSysDate(DateUtil.YYYYMMDD) + ai.getAndIncrement()
				+ "/";
		File f = new File(fdirName);
		if (f.exists()) {
			fdirName = BaseFactory.getFilePath(bakPath)
					+ DateUtil.getSysDate(DateUtil.YYYYMMDD)
					+ ai.getAndIncrement() + "/";
			f = new File(fdirName);
		}
		return f.mkdirs();
	}

	@SuppressWarnings({ "unchecked", "unused" })
	public void importFile(String srcPath, String backDir, String failedDir) {
		File f = new File(srcPath);
		File[] lf = f.listFiles();
		for (File tf : lf) {
			if (tf.isDirectory()) {
				// 创建文件夹的备份目录
				String[] ta = tf.getAbsolutePath().split("\\\\");
				String d = ta[ta.length - 1];
				String bakDir = createBakdDir(backDir, d);
				String tfailedDir = createBakdDir(failedDir, d);
				try {
					//多线程异步导入,每个子目录一个程
//					Future<String> fs = StockFactory.submit(new FileImportTask(
//							tf, bakDir, tfailedDir, this));
					//fs.get();
					new FileImportTask(
							tf, bakDir, tfailedDir, this).call();
				} catch (Exception e) {
					logger.error("导入失败",e);
				}
			} else {
				if (tf.getName().endsWith(_fpostfix)) {
					try {
						logger.info("import file ;fileName :"
								+ tf.getAbsolutePath());
						boolean isuccess = true;
						String ret = StockCodes.FAILED;
						try {

							// 把.xls文件导入数据库
							ret = importData2Db(tf);
						} catch (Exception e) {
							logger.error("import file failed!", e);
							isuccess = false;
						}
						
						if (ret.equals(StockCodes.SUCCESS)) {
							// 成功处理
							afterHander(tf, backDir);
						} else {
							// 失败处理
							afterHander(tf, failedDir);
						}
						logger.info("import this file finish ! fileName :"+tf.getName());
					} catch (Exception e) {
						logger.error(
								"import file failed! fileName :" + tf.getName(),
								e);
					}
				}
			}

		}
	}

	private String getFailedPath(String backDir) {
		String[] ta = backDir.split("\\\\");
		String d = ta[ta.length - 1];
		return backDir.replace(d, failedDirName);
	}

	private void afterHander(File tf, String fdirName) throws Exception {
		// TODO Auto-generated method stub
		// 如果成功,则把文件拷贝到bak目录下,并删除原文件
		// 创建一个文件
		if (!fdirName.endsWith("\\")) {
			fdirName += "\\";
		}
		File nf = createFile(fdirName, tf.getName());
		// 拷贝文件
		copyFile(tf, nf);
		// 删除文件
		tf.delete();
	}

	// private String createBakDirByCopyFile(File tf) {
	// // TODO Auto-generated method stub
	// String fdir = tf.getParentFile().getPath();
	// String dd = fdir.substring(fdir.indexOf("import") + 6, fdir.length());
	// return createBakDir(dd);
	//
	// }

	private String createBakDir(String dd) {
		if (!dd.startsWith("\\")) {
			dd = "\\" + dd;
		}
		String bakPath = "\\file\\bak" + dd;
		if (!bakPath.endsWith("\\")) {
			bakPath += "\\";
		}
		String fdirName = BaseFactory.getFilePath(bakPath)
				+ DateUtil.getSysDate(DateUtil.YYYYMMDD) + ai.getAndIncrement()
				+ "/";

		File f = new File(fdirName);
		if (f.exists()) {
			fdirName = BaseFactory.getFilePath(bakPath)
					+ DateUtil.getSysDate(DateUtil.YYYYMMDD)
					+ ai.getAndIncrement() + "/";
			f = new File(fdirName);
		}
		f.mkdirs();
		return fdirName;
	}

	private String createBakdDir(String bakPath, String suffix) {
		if (!bakPath.endsWith("\\")) {
			bakPath += "\\";
		}
		// 创建失败目录
		String filedPath = bakPath + suffix
				+ "\\";
		File ff = new File(filedPath);
		if (!ff.exists()) {
			ff.mkdirs();
		} 
		return filedPath;
	}

//	private String createBakDir(String bakPath, String bakVersion) {
//
//		if (!bakPath.endsWith("\\")) {
//			bakPath += "\\";
//		}
//		String fdirName = bakPath + bakVersion;
//
//		File f = new File(fdirName);
//		if (f.exists()) {
//			fdirName = bakPath
//					+ bakVersion + "\\";
//			f = new File(fdirName);
//		}
//		f.mkdirs();
//		return fdirName;
//	}

	private File createFile(String fdirName, String name) {
		File f = new File(fdirName + name);
		return f;

	}

	public long copyFile(File f1, File f2) throws Exception {
		long time = new Date().getTime();
		int length = 2097152;
		FileInputStream in = new FileInputStream(f1);
		FileOutputStream out = new FileOutputStream(f2);
		FileChannel inC = in.getChannel();
		FileChannel outC = out.getChannel();
		ByteBuffer b = null;
		while (true) {
			if (inC.position() == inC.size()) {
				inC.close();
				outC.close();
				return new Date().getTime() - time;
			}
			if ((inC.size() - inC.position()) < length) {
				length = (int) (inC.size() - inC.position());
			} else
				length = 2097152;
			b = ByteBuffer.allocateDirect(length);
			inC.read(b);
			b.flip();
			outC.write(b);
			outC.force(false);
		}
	}


}
