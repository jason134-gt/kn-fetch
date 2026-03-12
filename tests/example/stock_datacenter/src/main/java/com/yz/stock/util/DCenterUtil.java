package com.yz.stock.util;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.StockUtil;
import com.yfzx.service.cache.IndustryExtCacheService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.MatchinfoService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.service.index.CIndexService;

public class DCenterUtil {

	static Set<String> _hadComputeCompanySet = new HashSet<String>();

	public static boolean isPresentInDb(String key) {
		// TODO Auto-generated method stub
		return ComputeIndexManager.getBloomFilter().isPresent(key);
	}

	public static void add2BloomFilter(String key) {
		ComputeIndexManager.getBloomFilter().add(key);

	}

	public static void addBloomFilter(String companyCode, String indexCode,
			Date time) {
		String key = StockUtil.getExtCachekey(companyCode, indexCode, time);
		add2BloomFilter(key);

	}

	public static void removeFromBloomFilter(String key) {
		// TODO Auto-generated method stub
		ComputeIndexManager.getBloomFilter().clear(key);
	}

	public static void removeBloomFilter(String companyCode, String indexCode,
			Date time) {
		String key = StockUtil.getExtCachekey(companyCode, indexCode, time);
		removeFromBloomFilter(key);

	}

//	public synchronized static boolean indexHadCompute(String indexCode,
//			String companyCode) {
//		String key = indexCode + "." + companyCode;
//		if (!_hadComputeCompanySet.contains(key)) {
//			IndexMessage im = SMsgFactory.getUDCIndexMessage(companyCode);
//			im.setTableName(StockUtil
//					.getUExtTableName(companyCode));
//			im.setColumnName("value");
//			im.setIndexCode(indexCode);
//
//			IndexService ins = IndexService.getInstance();
//			Date maxd = ins.getMaxTime(companyCode);
//			Date mind = ins.getMinTime(companyCode);
//			Calendar c1 = Calendar.getInstance();
//			c1.setTime(maxd);
//			Calendar c2 = Calendar.getInstance();
//			c2.setTime(mind);
//			// int quarterNum = StockUtil.computeQuarterNumOf2Date(c1, c2);
//			// int indexNum = ins.getExtIndexValueNumOfCompany(companyCode,
//			// indexCode);
//			// 取最早的指标值
//			im.setTime( maxd);
//			Object omax = CIndexService.getInstance()
//					.getIndexValueWithCache(im);
//			// 取最晚的指标值
//			im.setTime( maxd);
//			Object omin = CIndexService.getInstance()
//					.getIndexValueWithCache(im);
//			// 如果最早和最晚的指标值均存在，则认为此指标已计算过
//			if (omax != null && omin != null) {
//				_hadComputeCompanySet.add(key);
//				return true;
//			} else {
//				return false;
//			}
//
//		}
//		return true;
//	}

	public static int getTableDataCount(String tableName) {

		return IndexService.getInstance().getTableDataCount(tableName);
	}

	
	public static String getBaseTableVokey(String companyCode, Date time) {
		// TODO Auto-generated method stub
		return companyCode + "." + time.getTime();
	}
	
	public static String getDataSourceCode(String tsc)
	{
		return MatchinfoService.getInstance().getDataSourceCodeByTsc(tsc);
	}
	
//	public static void initAllMarkDataOneTimeByType2cache(String type,
//			Date time) {
//		for(int i=0;i<StockConstants.UEXT_TABLE_NUM;i++)
//		{
//			String tableName = StockConstants.UEXT_TABLE_PREFIX + i;
//			List<Map> lm = IndustryService.getInstance().getAllMarkDataOneTimeByType(type,tableName, time);
//			if(lm!=null)
//			{
//				for(Map m : lm)
//				{
//					String tag = (String) m.get("uidentify");
//					Double value = (Double) m.get("value");
//					String iIndexCode = (String) m.get("index_code");
//					String key = StockUtil.getExtCachekey(tag, iIndexCode, time);
//					IndustryExtCacheService.getInstance().put(key, value);
//				}
//			}
//		}
//	
//		
//	}
}
