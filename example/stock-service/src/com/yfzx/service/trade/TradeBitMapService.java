package com.yfzx.service.trade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.SExt;
import com.stock.common.model.Company;
import com.stock.common.model.USubject;
import com.stock.common.model.trade.TradeBitMap;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.TUextService;
import com.yfzx.service.db.USubjectService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.lcs.enter.LCEnter;

/**
 * 交易日位图服务类
 * 
 * @author：杨真
 * @date：2014年12月30日
 */
public class TradeBitMapService {
	Logger log = LoggerFactory.getLogger(this.getClass());
	static TradeBitMapService instance = new TradeBitMapService();
	public  int missCount = 20;
	public TradeBitMapService() {

	}

	public static TradeBitMapService getInstance() {
		return instance;
	}

	public boolean isTradeDay(String companycode, Date time) {
		boolean isTrade = false;
		time = DateUtil.getDayStartTime(time);
		TradeBitMap tbm = LCEnter.getInstance().get(companycode,
				SCache.CACHE_NAME_tradedatamapcache);
		if (tbm != null) {
			isTrade = tbm.isTradeDay(time.getTime());
		}
		return isTrade;
	}

	public void setTradeDay(String companycode, Date time) {
		time = DateUtil.getDayStartTime(time);
		TradeBitMap tbm = LCEnter.getInstance().get(companycode,
				SCache.CACHE_NAME_tradedatamapcache);
		if (tbm != null) {
			tbm.setTradeDay(time.getTime());
		} 
	}

	/**
	 * type:0:全量，1：增量
	 * 
	 * @param type
	 */
	public void initBitMap(int type) {
		if (type == 0) {
			initAll();
		} else {
			initAdd();
		}
	}

	public void checkBitMap(Date stime)
	{
		List<USubject> usl = USubjectService.getInstance()
				.getUSubjectListAStock();
		for (USubject us : usl) {
			Company c = CompanyService.getInstance()
					.getCompanyByCodeFromCache(us.getUidentify());
			if (c == null) {
				continue;
			}
			checkOne_A(us.getUidentify(), stime);
		}

//		usl = USubjectService.getInstance()
//				.getUSubjectListZStock();
//		for (USubject us : usl) {
//			Company c = CompanyService.getInstance()
//					.getCompanyByCodeFromCache(us.getUidentify());
//			if (c == null) {
//				continue;
//			}
//			checkOne_A(us.getUidentify(), null);
//		}
		
		usl = USubjectService.getInstance().getUSubjectListHStock();
		for (USubject us : usl) {
			Company c = CompanyService.getInstance()
					.getCompanyByCodeFromCache(us.getUidentify());
			if (c == null) {
				continue;
			}
			checkOne_HK(us.getUidentify(), stime);
		}
	}
	private void checkOne_HK(String companycode, Date stime) {
		SqlSession sse = null;
		TradeBitMap ntbm = null;
		ResultSet rs = null;
		try {
			sse = ((SqlSessionFactory) BaseFactory
					.getSqlSessionFactory()).openSession();
			Connection con = sse.getConnection();
			Long publishtime = getPublishTime(companycode);
			if(publishtime==null)
			{
				log.error("publish time is null!companycode="+companycode+";stime="+stime);
				return;
			}
			Statement stmt = con.createStatement();
			String sql = " select  TIME from v_trade0001_hk  where company_code = '"
					+ companycode + "' order by time asc";
			if (stime != null) {
				sql = " select  TIME from v_trade0001_hk  where  TIME > '"
						+ DateUtil.format2String(stime)
						+ "' "
						+ "and company_code = '" + companycode + "' order by time asc";
			}

			rs = stmt.executeQuery(sql);

			if (rs != null) {
				try {
					while (rs.next()) {
						try {
							Date time = rs.getDate("time");
							if(ntbm==null)
							{
								ntbm = new TradeBitMap(companycode, publishtime);
								stime = time;
							}
							
							ntbm.setTradeDay(time.getTime());
							
						} catch (Exception e) {
							log.error("put data 2 cache failed!", e);
						}
					}
				} catch (Exception e) {
					log.error("put data 2 cache failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("query data failed!", e);
		} finally {
			try {
				if(sse!=null)
					sse.close();
			} catch (Exception e2) {
				log.error("close db connection failed!", e2);
			}
		}
		if(ntbm==null)
		{
//			log.error("ntbm is null!companycode="+companycode+";stime="+stime);
			return;
		}
		check(ntbm,stime);
		
	}

	private void checkOne_A(String companycode, Date stime) {
		SqlSession sse = null;
		TradeBitMap ntbm = null;
		ResultSet rs = null;
		try {
			sse = ((SqlSessionFactory) BaseFactory
					.getSqlSessionFactory()).openSession();
			Connection con = sse.getConnection();
			Long publishtime = getPublishTime(companycode);
			if(publishtime==null)
			{
				log.error("publish time is null!companycode="+companycode+";stime="+stime);
				return;
			}
			Statement stmt = con.createStatement();
			String sql = " select  TIME from v_trade0001  where company_code = '"
					+ companycode + "' order by time asc";
			if (stime != null) {
				sql = " select  TIME from v_trade0001  where  TIME >= '"
						+ DateUtil.format2String(stime)
						+ "' "
						+ "and company_code = '" + companycode + "' order by time asc";
			}

			rs = stmt.executeQuery(sql);

			if (rs != null) {
				try {
					while (rs.next()) {
						try {
							Date time = rs.getDate("time");
							if(ntbm==null)
							{
								ntbm = new TradeBitMap(companycode, publishtime);
								stime = time;
							}
							
							ntbm.setTradeDay(time.getTime());
							
						} catch (Exception e) {
							log.error("put data 2 cache failed!", e);
						}
					}
				} catch (Exception e) {
					log.error("put data 2 cache failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("query data failed!", e);
		} finally {
			try {
				if(sse!=null)
					sse.close();
			} catch (Exception e2) {
				log.error("close db connection failed!", e2);
			}
		}
		if(ntbm==null)
		{
//			log.error("ntbm is null!companycode="+companycode+";stime="+stime);
			return;
		}
		check(ntbm,stime);
	}

	private void check(TradeBitMap ntbm,Date stime) {
		if(ntbm==null)
		{
			return;
		}
		
		TradeBitMap tbm = LCEnter.getInstance().get(ntbm.getKey(),
				SCache.CACHE_NAME_tradedatamapcache);
		try {
			if (tbm == null || tbm.getPublishTime() != ntbm.getPublishTime()) {
				log.error("check TradeBitMap is null!" + ntbm);
			} else {
				int sindex = tbm.getDayIndex(stime.getTime());
				if(sindex<0)
				{
					sindex = 0;
				}
				int maxindex = tbm.getMaxIndex();
				for (int i = sindex; i < ntbm.getDbs().length() && i < maxindex; i++) {
					if (tbm.getDbs().get(i) != ntbm.getDbs().get(i)) {
						tbm.getDbs().set(i, ntbm.getDbs().get(i));
						log.error("check TradeBitMap is not equals!"
								+ DateUtil.format2String(new Date(tbm
										.getTimeFromIndex(i))) + ";"
								+ tbm.toString());
					}
				}
			}
		} catch (Exception e) {
			log.error("check TradeBitMap failed!",e);
		}
		
	}

	private void initAdd() {
		int pre = ConfigCenterFactory
				.getInt("stock_dc.bitmap_check_pre_day", 7);
		Date stime = StockUtil.getNextTimeV3(
				DateUtil.getDayStartTime(new Date()), -pre,
				Calendar.DAY_OF_MONTH);
		List<USubject> usl = USubjectService.getInstance()
				.getUSubjectListAStock();
		for (USubject us : usl) {
			Company c = CompanyService.getInstance()
					.getCompanyByCodeFromCache(us.getUidentify());
			if (c == null) {
				continue;
			}
			initOne_A(us.getUidentify(), stime);
		}

//		usl = USubjectService.getInstance()
//				.getUSubjectListZStock();
//		for (USubject us : usl) {
//			Company c = CompanyService.getInstance()
//					.getCompanyByCodeFromCache(us.getUidentify());
//			if (c == null) {
//				continue;
//			}
//			initOne_A(us.getUidentify(), null);
//		}
		
		usl = USubjectService.getInstance().getUSubjectListHStock();
		for (USubject us : usl) {
			Company c = CompanyService.getInstance()
					.getCompanyByCodeFromCache(us.getUidentify());
			if (c == null) {
				continue;
			}
			initOne_HK(us.getUidentify(), stime);
		}

	}

	private void initAll() {

		List<USubject> usl = USubjectService.getInstance()
				.getUSubjectListAStock();
		for (USubject us : usl) {
			Company c = CompanyService.getInstance()
					.getCompanyByCodeFromCache(us.getUidentify());
			if (c == null) {
				continue;
			}
			initOne_A(us.getUidentify(), null);
		}

//		usl = USubjectService.getInstance()
//				.getUSubjectListZStock();
//		for (USubject us : usl) {
//			Company c = CompanyService.getInstance()
//					.getCompanyByCodeFromCache(us.getUidentify());
//			if (c == null) {
//				continue;
//			}
//			initOne_A(us.getUidentify(), null);
//		}
		
		usl = USubjectService.getInstance().getUSubjectListHStock();
		for (USubject us : usl) {
			Company c = CompanyService.getInstance()
					.getCompanyByCodeFromCache(us.getUidentify());
			if (c == null) {
				continue;
			}
			initOne_HK(us.getUidentify(), null);
		}
	}

	private void initOne_HK(String companycode, Date stime) {
		SqlSession sse = null;
		TradeBitMap tbm = null;
		//增量,取原有的
		if(stime!=null)
		{
			tbm = LCEnter.getInstance().get(companycode,
					SCache.CACHE_NAME_tradedatamapcache);
		}
		ResultSet rs = null;
		try {
			sse = ((SqlSessionFactory) BaseFactory
					.getSqlSessionFactory()).openSession();
			Connection con = sse.getConnection();
			Statement stmt = con.createStatement();
			String sql = " select  TIME from v_trade0001_hk  where company_code = '"
					+ companycode + "' order by time asc";
			if (stime != null) {
				sql = " select  TIME from v_trade0001_hk  where  TIME > '"
						+ DateUtil.format2String(stime)
						+ "' "
						+ "and company_code = '" + companycode + "' order by time asc";
			}

			rs = stmt.executeQuery(sql);

			if (rs != null) {
				try {
					while (rs.next()) {
						try {
							Date time = rs.getDate("time");
							if (tbm == null) {
								Long publishtime = getPublishTime(companycode);
								if(publishtime==null)
									publishtime = time.getTime();
								//升序排列，默认最小的时间，就是其上市时间
								tbm = new TradeBitMap(companycode, publishtime);
							}
							tbm.setTradeDay(time.getTime());
						} catch (Exception e) {
							log.error("put data 2 cache failed!", e);
						}
					}
				} catch (Exception e) {
					log.error("put data 2 cache failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("query data failed!", e);
		} finally {
			try {
				if(sse!=null)
				sse.close();
			} catch (Exception e2) {
				log.error("close db connection failed!", e2);
			}
		}

		//更新缓存
		if(tbm!=null)
			LCEnter.getInstance().put(companycode, tbm,
					SCache.CACHE_NAME_tradedatamapcache);
	}

	private void initOne_A(String companycode, Date stime) {
		SqlSession sse = null;
		TradeBitMap tbm = null;
		//增量,取原有的
		if(stime!=null)
		{
			tbm = LCEnter.getInstance().get(companycode,
					SCache.CACHE_NAME_tradedatamapcache);
		}
		ResultSet rs = null;
		try {
			sse = ((SqlSessionFactory) BaseFactory
					.getSqlSessionFactory()).openSession();
			Connection con = sse.getConnection();
			Statement stmt = con.createStatement();
			String sql = " select  TIME from v_trade0001  where company_code = '"
					+ companycode + "' order by time asc";
			if (stime != null) {
				sql = " select  TIME from v_trade0001  where  TIME > '"
						+ DateUtil.format2String(stime)
						+ "' "
						+ "and company_code = '" + companycode + "' order by time asc";
			}

			rs = stmt.executeQuery(sql);

			if (rs != null) {
				try {
					while (rs.next()) {
						try {
							Date time = rs.getDate("time");
							if (tbm == null) {
								Long publishtime = getPublishTime(companycode);
								if(publishtime==null)
									publishtime = time.getTime();
								//升序排列，默认最小的时间，就是其上市时间
								tbm = new TradeBitMap(companycode, publishtime);
							}
							tbm.setTradeDay(time.getTime());
						} catch (Exception e) {
							log.error("put data 2 cache failed!", e);
						}
					}
				} catch (Exception e) {
					log.error("put data 2 cache failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("query data failed!", e);
		} finally {
			try {
				if(sse!=null)
				sse.close();
			} catch (Exception e2) {
				log.error("close db connection failed!", e2);
			}
		}

		//更新缓存
		if(tbm!=null)
			LCEnter.getInstance().put(companycode, tbm,
					SCache.CACHE_NAME_tradedatamapcache);
	}

	public TradeBitMap getBitMap(String uidentify) {
		return LCEnter.getInstance().get(uidentify,
				SCache.CACHE_NAME_tradedatamapcache);
		
	}
	
	public Long getPublishTime(String uidentify)
	{
		TradeBitMap tbm = LCEnter.getInstance().get(uidentify,
				SCache.CACHE_NAME_tradedatamapcache);
		if(tbm!=null)
		{
			return tbm.getPublishTime();
		}
		return null;
	}
	
	public Long getLatestTradeTime(String uidentify)
	{
		TradeBitMap tbm = LCEnter.getInstance().get(uidentify,
				SCache.CACHE_NAME_tradedatamapcache);
		if(tbm!=null)
		{
			return tbm.getLastTradeTime();
		}
		return null;
	}

	public void checkTradeData(Date stime) {
		List<USubject> usl = USubjectService.getInstance().getUSubjectListAStock();
		if(usl!=null)
		{
			for(USubject us:usl)
			{
				docheckTradeData(stime,us);
			}
		}
		List<USubject> husl = USubjectService.getInstance().getUSubjectListHStock();
		if(husl!=null)
		{
			for(USubject us:husl)
			{
				docheckTradeData(stime,us);
			}
		}
	}

	private void docheckTradeData(Date stime, USubject us) {
		Long minEqualsTime = null;
		int loadcount = 0;
		TradeBitMap tbm = LCEnter.getInstance().get(us.getUidentify(),
				SCache.CACHE_NAME_tradedatamapcache);
		try {
				if(tbm==null)
					return;
				int sindex = tbm.getDayIndex(stime.getTime());
				if(sindex<0)
				{
					sindex = 0;
				}
				int maxindex = tbm.getMaxIndex();
				for (int i = sindex; i < tbm.getDbs().length() && i < maxindex; i++) {
					Long theTime = tbm.getTimeFromIndex(i);
					
					String key = StockUtil.getVoKey(us.getUidentify(),theTime);
					Object o = LCEnter.getInstance().get(key, StockUtil.getTrade0001Cache(key));
					//如果trademap中存在，而缓存不存在，则记录
					if(tbm.getDbs().get(i))
					{
						if(o==null)
						{
							if(minEqualsTime==null||minEqualsTime>theTime)
								minEqualsTime = theTime;
						}
						else
						{
							loadcount++;
						}
						
						
					}
					else
					{
						if(o!=null)
						{
							LCEnter.getInstance().remove(key, StockUtil.getTrade0001Cache(key));
							IndexService.getInstance().clearTradeDataFromCache(new Date(theTime), false,us.getUidentify());
						}
					}
				}
				//缓存中有不匹配的记录，并且此公司要在此服务中加载了数据
				if(minEqualsTime!=null&&loadcount>missCount)
				{
					log.info("load miss tradedata,time ="
							+ DateUtil.format2String(new Date(minEqualsTime))+";uidentify ="+us);
					Trade0001Service.getInstance().loadOneCompanyData2Cache(us.getUidentify(), new Date(minEqualsTime));
					doloadUExtData(us.getUidentify(), DateUtil.format2String(new Date(minEqualsTime)));
				}
		} catch (Exception e) {
			log.error("check TradeBitMap failed!",e);
		}
		
		
	}

	public void doloadUExtData(String companyCode, String stime) {
		// 加载日行情扩展指标
		String tableName = SExt.getUExtTableName(companyCode, SExt.EXT_TABLE_TYPE_2);
		TUextService.getInstance().loadOneUSubjectData2CacheByTimeWithSql(companyCode, tableName, stime);

		// 加载周行情扩展指标
		tableName = SExt.getUExtTableName(companyCode, SExt.EXT_TABLE_TYPE_3);
		TUextService.getInstance().loadOneUSubjectData2CacheByTimeWithSql(companyCode, tableName, stime);

		// 加载月行情扩展指标
		tableName = SExt.getUExtTableName(companyCode, SExt.EXT_TABLE_TYPE_4);
		TUextService.getInstance().loadOneUSubjectData2CacheByTimeWithSql(companyCode, tableName, stime);

	}
	/**
	 * 取公司上市天数
	 * @param uidentify
	 * @return
	 */
	public int getPublishDaysCount(String uidentify) {
		TradeBitMap tbm = LCEnter.getInstance().get(uidentify,
				SCache.CACHE_NAME_tradedatamapcache);
		if(tbm!=null)
		{
			return tbm.getDayIndex(System.currentTimeMillis());
		}
		return 0;
	}
	
	public Long getNext(String uidentify,Long stime, int next, int per) {
		TradeBitMap tbm = LCEnter.getInstance().get(uidentify,
				SCache.CACHE_NAME_tradedatamapcache);
		if(tbm!=null)
		{
			return tbm.getNext(stime, next, per);
		}
		return null;
	}

	public int getMissCount() {
		return missCount;
	}

	public void setMissCount(int missCount) {
		this.missCount = missCount;
	}
	
	
}
