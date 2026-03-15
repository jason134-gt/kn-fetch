package com.yz.stock.trade.outer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.trade.ScldVO;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.util.DateUtil;
import com.stock.common.util.Spider;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.trade.outer.cache.RankCacheService;
import com.yz.stock.trade.outer.qq.QQDataAgent;
import com.yz.stock.trade.outer.qq.WYDataAgent;

public class OuterDataCenter {
	static Logger log = LoggerFactory.getLogger(OuterDataCenter.class);
	static long _refreshInterval = 180 * 1000;// 系统刷新间隔
	static long _lastRefreshTime = System.currentTimeMillis();
	static long _lastRefreshStockTradeTime = System.currentTimeMillis();

	public static void init() {

		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					// refreshStockTradeInfoCache();
					// refreshOuterData();
				} catch (Exception e) {
					log.error("refresh outer data failed!", e);
				}
				while (true) {
					try {
						long curtime = System.currentTimeMillis();
						if (curtime - _lastRefreshTime > _refreshInterval) {
//							log.info("---------------refresh outer data start!-------------------");
							refreshOuterData();
//							log.info("---------------refresh outer data end!-------------------");
							_lastRefreshTime = curtime;
						}
						if (curtime - _lastRefreshStockTradeTime > _refreshInterval) {
//							refreshStockTradeInfoCache();
							_lastRefreshStockTradeTime = curtime;
						}
						Thread.sleep(15000);
					} catch (Exception e) {
						log.error("refresh outer data failed!", e);
					}
				}

			}
		});

//		t.setName("OuterDataRefreshThread");
//		t.start();
	}

	protected static void refreshOuterData() {
		try {
			refreshSCLDCache_cl();
			refreshCJLJJCache_cl();
			refreshCJLTZCache_cl();
			refreshLXSZCache_cl();
			refreshLXXDCache_cl();
			refreshJQXGCache_cl();
			refreshJQXDCache_cl();
			refreshDMDPX0Cache_cl();
			refreshDMDPX1Cache_cl();
			refreshZDBCache_cl();

		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	public static void refreshStockTradeInfoCache() {
		log.info("---------------refresh StockTradeInfo outer data start !-------------------");
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListFromCache();
		//对公司分成多组，拉取数据时按组取
		int pagesize = ConfigCenterFactory.getInt("stock_dc.fetch_sina_hq_page", 50);
		int page = cl.size()/pagesize;
		List<String> sl = new ArrayList<String>();
		for(int i=0;i<page;i++)
		{
			int start = i*pagesize;
			if(start>cl.size())
				break;
			int end = start+pagesize;
			if(end>cl.size())
				end=cl.size();
			List<Company> scl = cl.subList(start, end);
			StringBuffer sb = new StringBuffer();
			for (Company c : scl) {
				 String[] ca = c.getCompanyCode().split("\\.");
				 String sc = ca[1] + ca[0];
				 sb.append(sc);
				 sb.append(",");
			}
			sl.add(sb.toString());
		}
		for(String sc : sl)
		{
			try {
				Long time = Calendar.getInstance().getTimeInMillis();
				//http://hq.sinajs.cn/?_=1395367865527&list=sh600864,sh600519
				String url = "http://hq.sinajs.cn/?_=" + time + "&list="
						+ sc;
				String s = Spider.urlSpider(url, "gbk");
				if (!StringUtil.isEmpty(s)) {
					for(String sci:s.split(";"))
					{
						if (!StringUtil.isEmpty(sci) && sci.split(",").length > 2) {
							String[] heada = sci.split("=")[0].split("_");
							String companycode = getcompanycode(heada[heada.length-1]);
							Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
							if(c==null) continue;
							String key = getStockTradeKey(companycode);
//							String[] ca = companycode.split("\\.");
							String[] sa = sci.split(",");
							StockTrade st = LCEnter.getInstance().get(key,
									SCache.CACHE_NAME_marketcache);
							if (st == null) {
								st = new StockTrade(Double.valueOf(sa[5]),
										Double.valueOf(sa[4]), Double.valueOf(sa[3]),
										Double.valueOf(sa[2]), Double.valueOf(sa[1]),
										Double.valueOf(sa[8]), Double.valueOf(sa[9]));
								st = buildSt(sa, st);
								LCEnter.getInstance().put(key, st,
										SCache.CACHE_NAME_marketcache);

								CompanyService.getInstance().initCompanyIndexData(c);
							}
							if (st != null) {
								st = buildSt(sa, st);
								//					initCompanyIndexData(c);
								reinitCompanyTradeInfo(c, st);
							}
						}
					}
				}
				
			} catch (Exception e) {
				log.error("---------------refresh StockTradeInfo outer data faild!-------------------",e);
			}
		}
//		for (Company c : cl) {
//			try {
//				String key = getStockTradeKey(c.getCompanyCode());
//				String[] ca = c.getCompanyCode().split("\\.");
//				Long time = Calendar.getInstance().getTimeInMillis();
//				//http://hq.sinajs.cn/?_=1395367865527&list=sh600864,sh600519
//				String url = "http://hq.sinajs.cn/?_=" + time + "&list="
//						+ ca[1] + ca[0];
//				String s = Spider.urlSpider(url, "gbk");
//				if (!StringUtil.isEmpty(s) && s.split(",").length > 2) {
//					String[] sa = s.split(",");
//					StockTrade st = LCEnter.getInstance().get(key,
//							SCache.CACHE_NAME_marketcache);
//					if (st == null) {
//						st = new StockTrade(Double.valueOf(sa[5]),
//								Double.valueOf(sa[4]), Double.valueOf(sa[3]),
//								Double.valueOf(sa[2]), Double.valueOf(sa[1]),
//								Double.valueOf(sa[8]), Double.valueOf(sa[9]));
//						st = buildSt(sa, st);
//						LCEnter.getInstance().put(key, st,
//								SCache.CACHE_NAME_marketcache);
//
//						
//						CompanyService.getInstance().initCompanyIndexData(c);
//					}
//					if (st != null) {
//						st = buildSt(sa, st);
//						//					initCompanyIndexData(c);
//						reinitCompanyTradeInfo(c, st);
//					}
//				}
//			} catch (Exception e) {
//				log.error("---------------refresh StockTradeInfo outer data faild!-------------------"+e);
//			}
//		}
		log.info("---------------refresh StockTradeInfo outer data end!-------------------");
	}
	private static String getcompanycode(String sc) {
		String hzs = ConfigCenterFactory.getString("stock_dc.companycode_hzs", "sh;sz;hk");
		for(String hz:hzs.split(";"))
		{
			if(sc.indexOf(hz)>=0)
				return sc.replace(hz, "")+"."+hz;
		}
		
		return sc;
	}
	/**
	 * 定时更新股价信息
	 */
//	public static void refreshStockTradeInfoCache() {
//		log.info("---------------refresh StockTradeInfo outer data start !-------------------");
//		List<Company> cl = CompanyService.getInstance()
//				.getCompanyListFromCache();
//		for (Company c : cl) {
//			String key = getStockTradeKey(c.getCompanyCode());
//			String[] ca = c.getCompanyCode().split("\\.");
//			Long time = Calendar.getInstance().getTimeInMillis();
//			String url = "http://hq.sinajs.cn/?_=" + time + "&list=" + ca[1]
//					+ ca[0];
//			String s = Spider.urlSpider(url, "gbk");
//			if (!StringUtil.isEmpty(s) && s.split(",").length > 2) {
//				String[] sa = s.split(",");
//				StockTrade st = LCEnter.getInstance().get(key,
//						SCache.CACHE_NAME_marketcache);
//				if (st == null) {
//					st = new StockTrade(Double.valueOf(sa[5]),
//							Double.valueOf(sa[4]), Double.valueOf(sa[3]),
//							Double.valueOf(sa[2]), Double.valueOf(sa[1]),
//							Double.valueOf(sa[8]), Double.valueOf(sa[9]));
//					st = buildSt(sa, st);
//					LCEnter.getInstance().put(key, st,
//							SCache.CACHE_NAME_marketcache);
//
//					String ctime = StockUtil.getDefaultPeriodTime("");
//					// 取综合能力评分
//					IndexMessage im1 = SMsgFactory.getUMsg(
//							c.getCompanyCode(), StockConstants.INDEX_CODE_ZHNL,
//							ctime);
//					im1.setNeedAccessExtIndexDb(false);
//					im1.setNeedAccessCompanyBaseIndexDb(false);
//					Double xv = IndexValueAgent.getIndexValue(im1);
//					if (xv != null && xv != 0)
//						st.setZhnl(xv.intValue());
//					initCompanyIndexData(c);
//				}
//				if (st != null) {
//					st = buildSt(sa, st);
//
//					reinitCompanyTradeInfo(c, st);
//				}
//			}
//		}
//		log.info("---------------refresh StockTradeInfo outer data end!-------------------");
//	}

	private static StockTrade buildSt(String[] sa, StockTrade st) {
		Double d = Double.valueOf(sa[5]);
		if (d != null && d != 0)
			st.setL(d);
		d = Double.valueOf(sa[4]);
		if (d != null && d != 0)
			st.setH(d);
		d = Double.valueOf(sa[3]);
		if (d != null && d != 0)
			st.setC(d);
		d = Double.valueOf(sa[2]);
		if (d != null && d != 0)
			st.setZs(d);
		d = Double.valueOf(sa[1]);
		if (d != null && d != 0)
			st.setJk(d);
		d = Double.valueOf(sa[8]);
		if (d != null && d != 0)
			st.setCjl(d);
		d = Double.valueOf(sa[9]);
		if (d != null && d != 0)
			st.setCje(d);
		return st;
	}

	private static void reinitCompanyTradeInfo(Company c, StockTrade st) {
		c.setL(st.getL());
		c.setH(st.getH());
		c.setC(st.getC());
		c.setZs(st.getZs());
		c.setJk(st.getJk());
		c.setCjl(st.getCjl());
		c.setCje(st.getCje());
	}

	private static void initCompanyIndexData(Company c) {
		Date ctime = CompanyService.getInstance().getLatestReportTime(
				c.getCompanyCode());
		// 取综合能力评分
		IndexMessage im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
				StockConstants.INDEX_CODE_ZHNL, ctime);
		im1.setNeedAccessExtIndexDb(false);
		im1.setNeedAccessCompanyBaseIndexDb(false);
		Double xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
		{
			c.setZhnl(xv);
			c.putAttr(StockConstants.INDEX_CODE_ZHNL, xv);
		}
		
		
		im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
				StockConstants.INDEX_CODE_YLNL, ctime);
		im1.setNeedAccessExtIndexDb(false);
		im1.setNeedAccessCompanyBaseIndexDb(false);
		 xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.putAttr(StockConstants.INDEX_CODE_YLNL, xv);
		
		 im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
				StockConstants.INDEX_CODE_YLZL, ctime);
		im1.setNeedAccessExtIndexDb(false);
		im1.setNeedAccessCompanyBaseIndexDb(false);
		 xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.putAttr(StockConstants.INDEX_CODE_YLZL, xv);
		
		 im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
				StockConstants.INDEX_CODE_CZNL, ctime);
		im1.setNeedAccessExtIndexDb(false);
		im1.setNeedAccessCompanyBaseIndexDb(false);
		 xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.putAttr(StockConstants.INDEX_CODE_CZNL, xv);
		
		 im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
				StockConstants.INDEX_CODE_YYNL, ctime);
		im1.setNeedAccessExtIndexDb(false);
		im1.setNeedAccessCompanyBaseIndexDb(false);
		 xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.putAttr(StockConstants.INDEX_CODE_YYNL, xv);
		
		 im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
				StockConstants.INDEX_CODE_GKNL, ctime);
		im1.setNeedAccessExtIndexDb(false);
		im1.setNeedAccessCompanyBaseIndexDb(false);
		 xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.putAttr(StockConstants.INDEX_CODE_GKNL, xv);
		
		im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
				StockConstants.INDEX_CODE_CCZNL, ctime);
		im1.setNeedAccessExtIndexDb(false);
		im1.setNeedAccessCompanyBaseIndexDb(false);
		 xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.putAttr(StockConstants.INDEX_CODE_CCZNL, xv);
		
		im1.setTime(ctime);
		// {"2022","2350","2349","2352","3016"};
		im1.setIndexCode(StockConstants.INDEX_CODE_PB);
		xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.setPB(xv);
		// pe
		im1.setIndexCode(StockConstants.INDEX_CODE_PE);
		xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.setPE(xv);
		// ps
		im1.setIndexCode(StockConstants.INDEX_CODE_PS);
		xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.setPS(xv);
		// ev
		im1.setIndexCode(StockConstants.INDEX_CODE_EV);
		xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.setEV(xv);

		// roe
		im1.setIndexCode(StockConstants.INDEX_CODE_ROE);
		xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.setRoe(xv);
		// mv
		im1.setIndexCode(StockConstants.INDEX_CODE_MV);
		xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.setMv(xv);

		// sum stock num
		im1.setIndexCode(StockConstants.INDEX_CODE_ASN);
		xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.setAsn(xv);
		// 每股收益
		im1.setIndexCode(StockConstants.INDEX_CODE_PSY);
		xv = IndexValueAgent.getIndexValue(im1);
		if (xv != null && xv != 0)
			c.setPsy(xv);

		// 股息收益率
		IndexMessage imgx = (IndexMessage) im1.clone();
		imgx.setTime(StockUtil.getNextTime(im1.getTime(), -12));
		imgx.setIndexCode(StockConstants.INDEX_CODE_GXSY);
		xv = IndexValueAgent.getIndexValue(imgx);
		if (xv != null && xv != 0)
			c.setGxsy(xv);
	}

	private static String getStockTradeKey(String companyCode) {
		// TODO Auto-generated method stub
		return "st." + companyCode;
	}

	/**
	 * 刷新市场雷达缓存
	 */
	private static void refreshSCLDCache() {
		List<ScldVO> allsl = new ArrayList<ScldVO>();
		List<ScldVO> qsl = QQDataAgent.getInstance().getSCLDData();
		List<ScldVO> wysl = WYDataAgent.getInstance().getSCLDData();
		if (qsl != null)
			allsl.addAll(qsl);
		if (wysl != null)
			allsl.addAll(wysl);
		Collections.sort(allsl, new Comparator<ScldVO>() {

			public int compare(ScldVO o1, ScldVO o2) {
				if (o1.getTime().getTime() < o2.getTime().getTime())
					return 1;
				return 0;
			}

		});
		String jsoString = toSCLDJsoString(allsl);
		RankCacheService.getInstance().put(SCache.CACHE_KEY_SCLD_RANK,
				jsoString);
	}

	/**
	 * 刷新市场雷达缓存
	 */
	private static void refreshSCLDCache_cl() {
		List<ScldVO> allsl = new ArrayList<ScldVO>();
		List<ScldVO> qsl = QQDataAgent.getInstance().getSCLDData();
		List<ScldVO> wysl = WYDataAgent.getInstance().getSCLDData();
		if (qsl != null)
			allsl.addAll(qsl);
		if (wysl != null)
			allsl.addAll(wysl);
		Collections.sort(allsl, new Comparator<ScldVO>() {

			public int compare(ScldVO o1, ScldVO o2) {
				if (o1.getTime().getTime() < o2.getTime().getTime())
					return 1;
				return 0;
			}

		});
		List<Company> cl = new ArrayList<Company>();
		for (ScldVO v : allsl) {
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
					v.getCompanycode());
			cl.add(c);
		}
		LCEnter.getInstance().put(SCache.CACHE_KEY_SCLD_RANK, cl,
				StockConstants.COMPANY_CACHE_NAME);
	}

	/**
	 * 刷新成交量突增缓存
	 */
	private static void refreshCJLTZCache() {
		try {
			String cjltz = WYDataAgent.getInstance().getCJLTZData();

			RankCacheService.getInstance().put(SCache.CACHE_KEY_CJLTZ, cjltz);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	/**
	 * 刷新成交量聚减缓存
	 */
	private static void refreshCJLJJCache() {
		try {
			String cjljj = WYDataAgent.getInstance().getCJLJJData();

			RankCacheService.getInstance().put(SCache.CACHE_KEY_CJLJJ, cjljj);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static String toSCLDJsoString(List<ScldVO> allsl) {
		StringBuffer sb = new StringBuffer();
		sb.append("var scld=\"");
		for (ScldVO vo : allsl) {
			sb.append(vo.getCompanycode());
			sb.append("|");
			sb.append(vo.getCompanyname());
			sb.append("|");
			sb.append(vo.getPrice());
			sb.append("|");
			sb.append(vo.getYdInfo());
			sb.append("|");
			sb.append(vo.getYdType());
			sb.append("|");
			sb.append(DateUtil.getSysDate(DateUtil.HHMMSS, vo.getTime()));
			sb.append("^");
		}
		return sb.substring(0, sb.length() - 1) + "\"";
	}

	private static void refreshLXSZCache() {
		try {
			String LXSZ = WYDataAgent.getInstance().getLXSZData();

			RankCacheService.getInstance().put(SCache.CACHE_KEY_LXSZ, LXSZ);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshLXXDCache() {
		try {
			String LXXD = WYDataAgent.getInstance().getLXXDData();

			RankCacheService.getInstance().put(SCache.CACHE_KEY_LXXD, LXXD);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshJQXGCache() {
		try {
			String LXSZ = WYDataAgent.getInstance().getJQXGData();

			RankCacheService.getInstance().put(SCache.CACHE_KEY_JQXG, LXSZ);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshJQXDCache() {
		try {
			String LXXD = WYDataAgent.getInstance().getJQXDData();

			RankCacheService.getInstance().put(SCache.CACHE_KEY_JQXD, LXXD);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshDMDPX0Cache() {
		try {
			String DDPX0 = QQDataAgent.getInstance().getDDPX0Data();

			RankCacheService.getInstance().put(SCache.CACHE_KEY_DDPX0, DDPX0);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshDMDPX1Cache() {
		try {
			String DMDPX1 = QQDataAgent.getInstance().getDDPX1Data();

			RankCacheService.getInstance().put(SCache.CACHE_KEY_DDPX1, DMDPX1);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	public static void refreshZDBCache() {
		try {
			QQDataAgent.getInstance().refreshZDBCache();

		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static String getSCLD() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_SCLD_RANK);
	}

	public static String getCJLJJ() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_CJLJJ);
	}

	public static String getCJLTZ() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_CJLTZ);
	}

	public static String getLXSZ() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_LXSZ);
	}

	public static String getLXXDCache() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_LXXD);
	}

	public static String getJQXDCache() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_JQXD);
	}

	public static String getJQXGCache() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_JQXG);
	}

	public static String getDMDPX0Cache() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_DDPX0);
	}

	public static String getDMDPX1Cache() {
		return RankCacheService.getInstance().get(SCache.CACHE_KEY_DDPX1);
	}

	public static String getZD10Cache(int type) {
		return QQDataAgent.getInstance().getZD10Cache(type);
	}

	/**
	 * 刷新成交量突增缓存
	 */
	private static void refreshCJLTZCache_cl() {
		try {
			List<Company> cjltz = WYDataAgent.getInstance().getCJLTZData_cl();

			LCEnter.getInstance().put(SCache.CACHE_KEY_CJLTZ, cjltz,
					StockConstants.COMPANY_CACHE_NAME);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	/**
	 * 刷新成交量聚减缓存
	 */
	private static void refreshCJLJJCache_cl() {
		try {
			List<Company> ret = WYDataAgent.getInstance().getCJLJJData_cl();
			LCEnter.getInstance().put(SCache.CACHE_KEY_CJLJJ, ret,
					StockConstants.COMPANY_CACHE_NAME);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshLXSZCache_cl() {
		try {
			List<Company> ret = WYDataAgent.getInstance().getLXSZData_cl();
			LCEnter.getInstance().put(SCache.CACHE_KEY_LXSZ, ret,
					StockConstants.COMPANY_CACHE_NAME);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshLXXDCache_cl() {
		try {
			List<Company> ret = WYDataAgent.getInstance().getLXXDData_cl();
			LCEnter.getInstance().put(SCache.CACHE_KEY_LXXD, ret,
					StockConstants.COMPANY_CACHE_NAME);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshJQXGCache_cl() {
		try {
			List<Company> ret = WYDataAgent.getInstance().getJQXGData_cl();
			LCEnter.getInstance().put(SCache.CACHE_KEY_JQXG, ret,
					StockConstants.COMPANY_CACHE_NAME);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshJQXDCache_cl() {
		try {
			List<Company> ret = WYDataAgent.getInstance().getJQXDData_cl();
			LCEnter.getInstance().put(SCache.CACHE_KEY_JQXD, ret,
					StockConstants.COMPANY_CACHE_NAME);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshDMDPX0Cache_cl() {
		try {
			List<Company> ret = QQDataAgent.getInstance().getDDPX0Data_cl();
			LCEnter.getInstance().put(SCache.CACHE_KEY_DDPX0, ret,
					StockConstants.COMPANY_CACHE_NAME);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static void refreshDMDPX1Cache_cl() {
		try {
			List<Company> DMDPX1 = QQDataAgent.getInstance().getDDPX1Data_cl();
			LCEnter.getInstance().put(SCache.CACHE_KEY_DDPX1, DMDPX1,
					StockConstants.COMPANY_CACHE_NAME);
		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	public static void refreshZDBCache_cl() {
		try {
			QQDataAgent.getInstance().refreshZDBCache_cl();

		} catch (Exception e) {
			log.error("refresh failed!", e);
		}
	}

	private static String getSCLD_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_SCLD_RANK,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static String getCJLJJ_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_CJLJJ,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static String getCJLTZ_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_CJLTZ,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static String getLXSZ_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_LXSZ,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static String getLXXDCache_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_LXXD,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static String getJQXDCache_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_JQXD,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static String getJQXGCache_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_JQXG,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static String getDMDPX0Cache_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_DDPX0,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static String getDMDPX1Cache_cl() {
		return LCEnter.getInstance().get(SCache.CACHE_KEY_DDPX1,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public static List<Company> getZD10Cache_cl(int type) {
		return QQDataAgent.getInstance().getZD10Cache_cl(type);
	}

	public static void main(String[] args) {
		// refreshSCLDCache();
		// refreshCJLTZCache();
		// WYDataAgent.getInstance().getLXSZData();
		refreshDMDPX0Cache();
	}
}
