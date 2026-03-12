package com.yfzx.service.db;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.company.Stock0001;
import com.stock.common.model.company.StockSell;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class Stock0001Service {

	private static Stock0001Service instance = new Stock0001Service();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private Stock0001Service() {

	}

	public static Stock0001Service getInstance() {
		return instance;
	}



	/**
	 * 没有查缓存
	 * @param companyCode
	 * @return
	 */
	public Stock0001 getStock0001ByCompanycode(String companyCode) {
		return getStock0001ByCompanycodeFromCache(companyCode);
	}

	public Stock0001 getStock0001ByCompanycodeFromDB(String companyCode) {
		// TODO Auto-generated method stub
		Stock0001 s = new Stock0001();
		s.setCompanyCode(companyCode);
		String sqlMapKey = "com.stock.common.model.company.Stock0001.select";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, s, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Stock0001) value;
	}
	public Stock0001 getStock0001ByCompanycodeFromCache(String companyCode) {
		return LCEnter.getInstance().get(companyCode, CacheUtil.getCacheName(StockConstants.DATA_TYPE_STOCK0001));
	}

	
	/**
	 * 取总股数
	 * @param companyCode
	 * @return
	 */
	public Double getStockNumFromCache(String companyCode,Date time)
	{
		String ck = StockUtil.joinString("_","sn", companyCode,DateUtil.formatYYYYMMDD2YYYYMM(time));
		Double sn = LCEnter.getInstance().get(ck, SCache.CACHE_NAME_marketcache);sn=null;
		if(sn==null)
		{
			//总发行量
			Double asn = getStockSellByTimeFromCache(companyCode,time);
			Stock0001 s = getStock0001ByCompanycodeFromCache(companyCode);
			if(s==null||s.getF012n()==0||asn==null) return null;
			//总发行量/面值
			sn = asn/s.getF012n()*10000;
			if(sn!=null)
				LCEnter.getInstance().put(ck, sn, SCache.CACHE_NAME_marketcache);
		}
//		return sn;
		
		time  = IndexService.getTradeTime(companyCode, time);
		if(time==null) return 0.0;
		Double sn2 = IndexValueAgent.getIndexValue(companyCode,
					StockConstants.INDEX_CODE_TRADE_ZGB, time);
		if(sn!=null&&sn2!=null&&sn2!=0&&sn<sn2)
			sn = sn2;
//		IndexService.getTradeTime(companyCode, StockUtil.getNextTimeV3(new Date(), -1, Calendar.DAY_OF_MONTH));
		return sn;
	}

	/**
	 * 取发行总量
	 * @param companyCode
	 * @param time
	 * @return
	 */
	public Double getStockSellByTime(String companyCode, String time) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companyCode);
		m.put("time", time);
		String sqlMapKey = "getStockSellByTime";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.dictionary);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Double) value;
	}
	/**
	 * 从缓存中取出对应公司的发行量变化列表（已排序），取出第一个满足条件的
	 * @param companyCode
	 * @param time
	 * @return
	 */
	public Double getStockSellByTimeFromCache(String companyCode, Date time) {
		long ltime = StockUtil.getNextTimeV3(time, 3,Calendar.MONTH).getTime();
		long currentTimeMillis = System.currentTimeMillis();
		if(ltime > currentTimeMillis){//计算出来的时间，比当前时间还大时
			ltime = currentTimeMillis;
		}
		List<StockSell> ssl = LCEnter.getInstance().get(companyCode, SCache.CACHE_NAME_stocksellcache);
		if(ssl==null||ssl.size()==0) return StockConstants.DEFAULT_DOUBLE_VALUE;
		Double rss = StockConstants.DEFAULT_DOUBLE_VALUE;
		for(StockSell ss : ssl)
		{
			long ttime = ss.getTime();
			if(ttime<=ltime)
			{
				rss= ss.getValue();
				break;
			}
				
		}
		if(rss.equals(StockConstants.DEFAULT_DOUBLE_VALUE))
		{
			//如果是大于最大值，则到当前公司的发行量数
			StockSell maxss = ssl.get(0);
			StockSell minss = ssl.get(ssl.size()-1);
			if(ltime>maxss.getTime())
				rss = getCurrentStockSell(companyCode);
			if(ltime<minss.getTime())
				rss = minss.getValue();
		}
		return rss;
	}
	
	private Double getCurrentStockSell(String companyCode) {
		Double rss = StockConstants.DEFAULT_DOUBLE_VALUE;
		Stock0001 s = getStock0001ByCompanycode(companyCode);
		if(s!=null)
			rss = Double.valueOf(s.getStockSellNum());
		return rss;
	}

	/**
	 * 取发行总量
	 * @param companyCode
	 * @param time
	 * @return
	 */
	public List<Map> getStockSellList() {

		String sqlMapKey = "getStockSellList";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Map>) value;
	}

	public Date getPulishTime(String companycode)
	{
		Stock0001 s01 = Stock0001Service.getInstance().getStock0001ByCompanycode(companycode);
		if(s01==null)
			return null;
		return s01.getF006d();
	}

	public Double getStockMianZhi(String companycode) {
		Stock0001 s01 = Stock0001Service.getInstance().getStock0001ByCompanycode(companycode);
		if(s01==null)
			return null;
		return s01.getF012n();
	}
}
