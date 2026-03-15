package com.yfzx.service.db;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.msg.Message;
import com.stock.common.util.DateUtil;
import com.stock.common.util.Spider;
import com.stock.common.util.StockUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.cache.IndustryExtCacheService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

/**
 * 行情数据中心
 * 
 * @author Administrator
 * 
 */
public class MarketService {

	private static MarketService instance = new MarketService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private MarketService() {

	}

	public static MarketService getInstance() {
		return instance;
	}

	/**
	 * 取总股数
	 * 
	 * @param companyCode
	 * @return
	 */
	public Double getStockNum(String companyCode, Date time) {
		return Stock0001Service.getInstance().getStockNumFromCache(companyCode,
				time);
	}

	Long expiretime = 60 * 1000 * 5l;
	int interval = 60 * 1000 * 5;// 间隔5分钟

	/**
	 * 取最新的股价,实时取 type 0 前一天的收市价 type 1 最新股价
	 * 
	 * @return
	 */
	public Double getStockPrice(String companyCode, String type) {
		Double nprice = null;
		if (companyCode == null)
			return null;
		try {
			// String[] sa = companyCode.split("\\.");
			// Long time = Calendar.getInstance().getTimeInMillis();
			// String url = "http://hq.sinajs.cn/?_="+time+"&list="+sa[1]+sa[0];
			// String s = Spider.urlSpider(url, "gbk");
			// if(!StringUtil.isEmpty(s)&&s.split(",").length>2)
			// {
			if ("0".equals(type)) {
				String key = getStockTradeKey(companyCode);
				StockTrade st = LCEnter.getInstance().get(key,
						SCache.CACHE_NAME_marketcache);
				if (st == null)
					st = getStockTradeInfo(companyCode);
				if (st != null)
					nprice = st.getZs();
			}
			if ("1".equals(type)) {
				StockTrade st = getStockTradeInfo(companyCode);
				if (st != null)
					nprice = st.getC();
			}
			// }
			// 如果最新的股价没有取到，就到最近一期的股价
			if ((nprice == null || nprice == 0)) {
				IndexMessage im = SMsgFactory.getUMsg(companyCode);
				Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
						im.getCompanyCode());
				if(c==null)
					return null;
				Date time = new Date(c.getCreportTime());
				im.setTime(time);
				im.setIndexCode(StockConstants.STOCK_PRICE_INDEX_CODE);
				Dictionary d = DictService.getInstance().getDataDictionary(
						im.getIndexCode());
				// 如果取往期，则直接从缓存中取
				nprice = IndexService.getInstance()
						.getCompanyExtIndexValueFromExtCache(d, im);
			}
		} catch (Exception e) {
			log.error("fetch trade info failed!companycode="+companyCode+";type="+type,e);
		}
		return nprice;
	}

	/**
	 * 取收盘价
	 * 
	 * @return
	 */
	public Double getStockPriceSp(IndexMessage req) {
		// Date time = IndexService.getInstance().formatTime(req.getTime(),
		// StockConstants.TRADE_TYPE, Calendar.DAY_OF_MONTH,
		// req.getCompanyCode());
		// IndexMessage im = SMsgFactory.getIndexMessage(Message.c_data_type);
		// im.setCompanyCode(req.getCompanyCode());
		// im.setTime(time);
		// im.setIndexCode(StockConstants.INDEX_CODE_TRADE_S);
		// // 如果取往期，则直接从缓存中取
		// return IndexService.getInstance().getCompanyBaseIndexFromCache(im);
		return getStockPriceNotFoundBrefore(req);
	}

	/*
	 * 取公司的股价，如果没有找到就往前查找
	 */
	public Double getStockPriceNotFoundBrefore(IndexMessage req) {
		Date wetime = req.getTime();
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		// 取上市时间
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null) 
			mintime = new Date();
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getUidentify(),
					StockConstants.INDEX_CODE_TRADE_S, c.getTime());
			if (r != null) {
				return r;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return 0.0;
	}

	public StockTrade getStockTradeInfo(String companyCode) {
		if (companyCode == null)
			return null;
		String key = getStockTradeKey(companyCode);
		return LCEnter.getInstance().get(key, SCache.CACHE_NAME_marketcache);
	}

	private String getStockTradeKey(String companyCode) {
		// TODO Auto-generated method stub
		return "st." + companyCode;
	}

	/**
	 * 取股票的最新行情数据
	 * 
	 * @return
	 */
	public String getStockLatestTradeInfo(String companyCode, String type) {
		if (companyCode == null)
			return null;
		String[] sa = companyCode.split("\\.");
		Long time = Calendar.getInstance().getTimeInMillis();
		String url = "http://hq.sinajs.cn/?_=" + time + "&list=" + sa[1]
				+ sa[0];
		return Spider.urlSpider(url, "gbk");
	}

	/**
	 * 取分红 0:当年，1：累计 由于数据库中是以万做为单位的，此处转换为元
	 * 
	 * @param companyCode
	 * @return
	 */
	public Double getDividend(String companyCode, Date time) {
		Date stime = DateUtil.format("1990-01-01");
		Calendar c = Calendar.getInstance();
		Date etime = StockUtil.getApproPeriod(c.getTime());
		if (time != null) {
			int year = DateUtil.getYear(time);
			stime = DateUtil.format(year + "-01-01");
			etime = DateUtil.format(year + "-12-31");
		}
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companyCode);
		m.put("stime", DateUtil.format2String(stime));
		m.put("etime", DateUtil.format2String(etime));
		RequestMessage req = DAFFactory.buildRequest("getDividend", m,
				StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Double) value * 10000;
	}

	public Double getDividendFromCache(String companyCode, Date time) {
		// String ck = "Dividend_"+companyCode;
		// if(!StringUtil.isEmpty(time))
		// ck+=time.split(":")[0];
		String ck = StockUtil.joinString("^", "dv",companyCode);
		if (time != null)
			ck = StockUtil.joinString("^", ck,DateUtil.getYear(time));
		Double sn = LCEnter.getInstance()
				.get(ck, SCache.CACHE_NAME_marketcache);
		if (sn == null) {
			//
			sn = getDividend(companyCode, time);
			if (sn != null)
				LCEnter.getInstance()
						.put(ck, sn, SCache.CACHE_NAME_marketcache);
			else
				LCEnter.getInstance().put(ck,
						StockConstants.DEFAULT_DOUBLE_VALUE,
						SCache.CACHE_NAME_marketcache);
		}
		return sn;
	}

	/**
	 * 取首次募资
	 * 
	 * @param companyCode
	 * @return
	 */
	public Double getFirstFinancing(String companyCode) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companyCode);
		RequestMessage req = DAFFactory.buildRequest("getFirstFinancing", m,
				StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return 0.0;
		}
		return (Double) value;
	}

	/**
	 * 后续募资分计
	 * 
	 * @param companyCode
	 * @param type
	 *            配股 或 增发
	 * @return
	 */
	public Double getFinancing(String companyCode, String type) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companyCode);
		m.put("type", type);
		RequestMessage req = DAFFactory.buildRequest("getFinancingType", m,
				StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return 0.0;
		}
		return (Double) value;
	}

	/**
	 * 取后续募资合计
	 * 
	 * @param companyCode
	 * @return
	 */
	public Double getLastFinancing(String companyCode) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companyCode);
		RequestMessage req = DAFFactory.buildRequest("getLastFinancing", m,
				StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return 0.0;
		}
		return (Double) value;
	}

	public Double getAllFinancingFromCache(String companyCode) {
		String ck = "AllFinancing_" + companyCode;
		Double sn = LCEnter.getInstance()
				.get(ck, SCache.CACHE_NAME_marketcache);
		if (sn == null) {
			// 总发行量/面值
			Double first = getFirstFinancing(companyCode);
			Double last = getLastFinancing(companyCode);
			sn = first + last;
			if (sn != null)
				LCEnter.getInstance()
						.put(ck, sn, SCache.CACHE_NAME_marketcache);
		}
		return sn;
	}

	/**
	 * 取分红募资比
	 * 
	 * @param companyCode
	 * @return
	 */
	public Double getFinancingDividendRatio(String companyCode) {
		Double d = getDividendFromCache(companyCode, null);
		Double f = getAllFinancingFromCache(companyCode);

		if (d == null || f == null || f == 0)
			return null;
		return d / f;
	}

	/**
	 * 最近一月的评价数据
	 * 
	 * @param companyCode
	 */
	public List getLastRating(String companyCode) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companyCode);
		RequestMessage req = DAFFactory.buildRequest("getLastRating", m,
				StockConstants.common);
		List list = pLayerEnter.queryForList(req);
		return list;
	}

	public List<Map> loadOneTimeAllCompanyStockPrice(Date time) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", DateUtil.format2String(time));
		RequestMessage req = DAFFactory.buildRequest(
				"loadOneTimeAllCompanyStockPrice", m, StockConstants.common);
		List list = pLayerEnter.queryForList(req);
		return list;
	}

	// 取行业总募资
	public Double getIndustryAllFinancing(IndexMessage req) {
		Double v = null;
		if (req.isNeedRealComputeIndustryValue() && v == null
				&& req.getIndustryIndexType().equals(StockConstants.ravgType)) {
			v = realIndustryAllFinancing(req);
			if (v != null) {
				String iIndexCode = StockUtil.getIndustryCode(
						req.getIndustryIndexType(), req.getIndexCode());
				String key = StockUtil.getExtCachekey(req.getUidentify(),
						iIndexCode, req.getTime());
				IndustryExtCacheService.getInstance().put(key, v);
			}
		}
		return v;
	}

	private Double realIndustryAllFinancing(IndexMessage req) {
		Double sum = 0.0;// 行业的合值
		IndexMessage im = (IndexMessage) req.clone();
		Dictionary d = DictService.getInstance().getDataDictionary(
				im.getIndexCode());
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListByTagFromCache(im.getUidentify());
		if (cl == null || cl.size() == 0) {
			log.error("company list is empty! tag =" + im.getUidentify());
			return null;
		}
		for (Company c : cl) {
			if (CompanyService.getInstance().needRemoveWhenComputeIndustry(c))
				continue;
			if (!MatchinfoService.getInstance()
					.getTscByTableCode(d.getTableCode()).equals(c.getTsc()))
				continue;
			im.setCompanyCode(c.getCompanyCode());
			im.setMsgtype(USubjectService.getInstance().getMsgType(
					im.getUidentify()));
			im.setNeedAccessCompanyBaseIndexDb(false);
			Double cv = getAllFinancingFromCache(c.getCompanyCode());
			if (cv == null)
				continue;

			sum += cv;
		}
		return sum;
	}

	// 取行业总股价
	public Double getIndustryStockPrice(IndexMessage req, String type) {
		Double v = null;
		if (req.isNeedRealComputeIndustryValue() && v == null
				&& req.getIndustryIndexType().equals(StockConstants.ravgType)) {
			v = realIndustryStockPrice(req, type);
			if (v != null) {
				String iIndexCode = StockUtil.getIndustryCode(
						req.getIndustryIndexType(), req.getIndexCode());
				String key = StockUtil.getExtCachekey(req.getUidentify(),
						iIndexCode, req.getTime());
				IndustryExtCacheService.getInstance().put(key, v);
			}
		}
		return v;
	}

	private Double realIndustryStockPrice(IndexMessage req, String type) {
		Double sum = 0.0;// 行业的合值
		IndexMessage im = (IndexMessage) req.clone();
		Dictionary d = DictService.getInstance().getDataDictionary(
				im.getIndexCode());
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListByTagFromCache(im.getUidentify());
		if (cl == null || cl.size() == 0) {
			log.error("company list is empty! tag =" + im.getUidentify());
			return null;
		}
		for (Company c : cl) {
			if (CompanyService.getInstance().needRemoveWhenComputeIndustry(c))
				continue;
			if (!MatchinfoService.getInstance()
					.getTscByTableCode(d.getTableCode()).equals(c.getTsc()))
				continue;
			im.setCompanyCode(c.getCompanyCode());
			im.setMsgtype(USubjectService.getInstance().getMsgType(
					im.getUidentify()));
			im.setNeedAccessCompanyBaseIndexDb(false);
			Double cv = getStockPrice(c.getCompanyCode(), type);
			if (cv == null)
				continue;

			sum += cv;
		}
		return sum;
	}

	public Double getIndustryMarketValue(IndexMessage req) {
		Double sum = 0.0;// 行业的合值
		IndexMessage im = (IndexMessage) req.clone();
		Dictionary d = DictService.getInstance().getDataDictionary(
				im.getIndexCode());
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListByTagFromCache(im.getUidentify());
		if (cl == null || cl.size() == 0) {
			log.error("company list is empty! tag =" + im.getUidentify());
			return null;
		}
		for (Company c : cl) {
			if (CompanyService.getInstance().needRemoveWhenComputeIndustry(c))
				continue;
			if (!MatchinfoService.getInstance()
					.getTscByTableCode(d.getTableCode()).equals(c.getTsc()))
				continue;
			im.setCompanyCode(c.getCompanyCode());
			im.setMsgtype(USubjectService.getInstance().getMsgType(
					im.getUidentify()));
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setIndexCode(StockConstants.INDEX_CODE_MARKET_VALUE);
			Double cv = IndexValueAgent.getIndexValue(im);
			if (cv == null)
				continue;

			sum += cv;
		}
		return sum;
	}

	public Double getIndustryStockNum(IndexMessage req) {

		Double v = null;
		if (req.isNeedRealComputeIndustryValue() && v == null
				&& req.getIndustryIndexType().equals(StockConstants.ravgType)) {
			v = realIndustryStockNum(req);
			if (v != null) {
				String iIndexCode = StockUtil.getIndustryCode(
						req.getIndustryIndexType(), req.getIndexCode());
				String key = StockUtil.getExtCachekey(req.getUidentify(),
						iIndexCode, req.getTime());
				Dictionary d = DictService.getInstance().getDataDictionary(
						req.getIndexCode());
				IndustryExtCacheService.getInstance().put(key, v);
			}
		}
		return v;
	}

	@SuppressWarnings("unchecked")
	public Double realIndustryStockNum(Message para) {
		Double sum = 0.0;// 行业的合值
		IndexMessage im = (IndexMessage) para.clone();
		Dictionary d = DictService.getInstance().getDataDictionary(
				im.getIndexCode());
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListByTagFromCache(im.getUidentify());
		if (cl == null || cl.size() == 0) {
			log.error("company list is empty! tag =" + im.getUidentify());
			return null;
		}
		for (Company c : cl) {
			if (CompanyService.getInstance().needRemoveWhenComputeIndustry(c))
				continue;
			if (!MatchinfoService.getInstance()
					.getTscByTableCode(d.getTableCode()).equals(c.getTsc()))
				continue;
			im.setCompanyCode(c.getCompanyCode());
			im.setMsgtype(USubjectService.getInstance().getMsgType(
					im.getUidentify()));
			im.setNeedAccessCompanyBaseIndexDb(false);
			Double cv = MarketService.getInstance().getStockNum(
					c.getCompanyCode(), im.getTime());
			if (cv == null)
				continue;

			sum += cv;
		}
		return sum;
	}

	public Double getIndustryDivided(IndexMessage req) {
		Double v = null;
		if (req.isNeedRealComputeIndustryValue()
				&& req.getIndustryIndexType().equals(StockConstants.ravgType)) {
			v = realIndustryDivided(req);
			if (v != null) {
				String iIndexCode = StockUtil.getIndustryCode(
						req.getIndustryIndexType(), req.getIndexCode());
				String key = StockUtil.getExtCachekey(req.getUidentify(),
						iIndexCode, req.getTime());
				Dictionary d = DictService.getInstance().getDataDictionary(
						req.getIndexCode());
				IndustryExtCacheService.getInstance().put(key, v);
			}
		}
		return v;
	}

	public Double realIndustryDivided(Message para) {
		IndexMessage im = (IndexMessage) para.clone();
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListByTagFromCache(im.getUidentify());
		Dictionary d = DictService.getInstance().getDataDictionary(
				im.getIndexCode());
		if (cl == null || cl.size() == 0)
			return null;
		Double sum = 0.0;// 行业的合值
		for (Company c : cl) {
			if (CompanyService.getInstance().needRemoveWhenComputeIndustry(c))
				continue;
			if (!MatchinfoService.getInstance()
					.getTscByTableCode(d.getTableCode()).equals(c.getTsc()))
				continue;
			im.setCompanyCode(c.getCompanyCode());
			im.setMsgtype(USubjectService.getInstance().getMsgType(
					im.getUidentify()));
			im.setNeedAccessCompanyBaseIndexDb(false);
			Double cv = MarketService.getInstance().getDividendFromCache(
					c.getCompanyCode(), im.getTime());
			if (cv == null)
				continue;

			sum += cv;
		}
		return sum;
	}
}
