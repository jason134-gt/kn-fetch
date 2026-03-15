package com.yfzx.service.stockgame;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Trade0001;
import com.stock.common.model.USubject;
import com.stock.common.model.share.UserExt;
import com.stock.common.model.stockgame.StockEquity;
import com.stock.common.model.stockgame.StockRevenue;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockCacheUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.DcssTradeIndexServiceClient;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.SystemPropertiesService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.trade.TradeBitMapService;
import com.yz.common.vo.BaseVO;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.dao.db.DBDefaultDaoImpl;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class StockRevenueService {
	private static final Logger logger = LoggerFactory.getLogger(StockRevenueService.class);

	private static final String NS = "com.stock.portal.dao.stockgame.StockRevenueDao";

	private StockRevenueService() {}

	private static StockRevenueService instance = new StockRevenueService();

	private static DBAgent dbAgent = DBAgent.getInstance();

	public static StockRevenueService getInstance() {
		return instance;
	}

	public void insertUsubject(StockRevenue sr) {
		if(sr == null) {
			return ;
		}

		Long uid = sr.getUid();
		UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(uid);
		if(userExt != null) {
			USubject u = new USubject();
			u.setName(userExt.getNickname());
			/*int count = USubjectService.getInstance().isExist(u);
			if(count > 0) {
				return;
			}*/
			USubject usubject = new USubject();
			usubject.setUidentify(userExt.getNickname() + "的投资");
			usubject.setName(userExt.getNickname());
			usubject.setType(6);
			usubject.setUid(uid);
			usubject.setCreateTime(new Date());
			USubjectService.getInstance().insertUsubject2(usubject);
			SystemPropertiesService.getInstance().update("last_usubject_date",DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, new Date()));
			try {
				RemindServiceClient.getInstance().addUSubjectByUIdentifyFromCache(usubject.getUidentifyIgnoCase(), usubject);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			logger.error("initStockRevenueFailure: " + sr.getUid() + "    " + sr.getFinance());
		}
	}

	public long insert(StockRevenue stockRevenue) {
		insertUsubject(stockRevenue);

		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", stockRevenue, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			StockRevenue obj = (StockRevenue)rm.getResult();
			return obj.getId();
		}
		return 0;
	}

	public boolean update(StockRevenue stockRevenue) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "updateByPrimaryKey", stockRevenue, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean delete(StockRevenue stockRevenue) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "delete", stockRevenue, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean deleteByPk(Long id) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "deleteByPrimaryKey", id, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public List<StockRevenue> getStockRevenueList(StockRevenue stockRevenue) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "select", stockRevenue, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockRevenue>)rm.getResult();
		} else {
			return null;
		}
	}

	public List<StockRevenue> topRankList(Integer limit) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("limit", limit);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "topRankList", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockRevenue>)rm.getResult();
		} else {
			return null;
		}
	}

	public List<StockRevenue> selectList(Integer offset, Integer limit, String sort, String dir) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("offset", offset);
		map.put("limit", limit);
		map.put("sort", sort);
		map.put("dir", dir);

		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectList", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockRevenue>)rm.getResult();
		} else {
			return null;
		}
	}

	public List<StockRevenue> selectByUids(List<Long> uidList, String orderBy) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("orderBy", orderBy);
		map.put("uids", "(" + StringUtils.join(uidList, ",") + ")");

		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectByUids", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockRevenue>)rm.getResult();
		} else {
			return null;
		}
	}

	public Integer selectCount() {
		Map<String, Object> map = new HashMap<String, Object>();
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectCount", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		return (Integer)rm.getResult();
	}

	public StockRevenue getStockRevenueByUid(Long uid) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectByUid", uid, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (StockRevenue)rm.getResult();
		} else {
			return null;
		}
	}

	/**
	 * @param orderBy 排序规则
	 * @param orderTime 最近下单时间
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<StockRevenue> selectRankList(String orderBy,Date orderTime,Integer offset, Integer limit) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("orderBy", orderBy);
		map.put("offset", offset);
		map.put("limit", limit);
		if( orderTime != null ){
			map.put("orderTime", orderTime);
		}
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectRankList", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockRevenue>)rm.getResult();
		} else {
			return null;
		}
	}

	public List<StockRevenue> getPagingStockRevenueList(Integer start, Integer limit) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("start", start);
		map.put("limit", limit);

		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getstock_revenuePageData2Cache", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockRevenue>)rm.getResult();
		} else {
			return null;
		}
	}

	public int getStockRevenueCount() {
		Map<String, Object> map = new HashMap<String, Object>();
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getstock_revenueAllCount", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		return (Integer)rm.getResult();
	}

	public double getStockGameUserCurMarketVal(Long uid) {
		StockEquity se = new StockEquity();
		se.setUid(uid);
		List<StockEquity> seList = StockEquityService.getInstance().getStockEquityList(se);

		Double hkd_to_rmb = ConfigCenterFactory.getDouble("stock_zjs.hkd_to_rmb", 0.7920D);//港元兑人民币
		Double dollar_to_rmb = ConfigCenterFactory.getDouble("stock_zjs.dollar_to_rmb", 6.1383D);//美元兑人民币

		double currentMarketVal = 0d;
		if(seList != null && seList.size() > 0) {
			for(StockEquity stockEquity : seList) {
				Company comapny = CompanyService.getInstance().getCompanyByCode(stockEquity.getStockCode());
				if(comapny != null) {
					int stockType = StockUtil.checkStockcode(comapny.getCompanyCode());
					double ratio = 1.0d;
					if(stockType == 1) {
						ratio = hkd_to_rmb;
					} else if(stockType == 2) {
						ratio = dollar_to_rmb;
					}
					boolean isStop = CompanyService.getInstance().isStop(comapny);
					if(isStop) {//停牌判断
						Trade0001 trade = DcssTradeIndexServiceClient.getInstance().getLatestTradeData(stockEquity.getStockCode());
						if(trade != null && trade.getF002n() != 0 && trade.getF002n() != 0) {
							currentMarketVal += trade.getF002n() * ratio * stockEquity.getEquityCount();//股票市值
						}
					} else {
						currentMarketVal += comapny.getC() * ratio * stockEquity.getEquityCount();//股票市值
					}
				}
			}
		}
		return currentMarketVal;
	}

	
	public String getRevenueRatioColumnOrderBy(int type) {
		String key = "";
		if(type == StockConstants.DAILY_SR_TYPE) {
			key = StockConstants.DAILY_SR;
		} else if(type == StockConstants.WEEK_SR_TYPE) {
			key = StockConstants.WEEK_SR;
		} else if(type == StockConstants.MONTH_SR_TYPE) {
			key = StockConstants.MONTH_SR;
		} else if(type == StockConstants.YEAR_SR_TYPE) {
			key = StockConstants.YEAR_SR;
		} else if(type == StockConstants.TOTAL_SR_TYPE) {
			key = StockConstants.TOTAL_SR;
		}
		return key;
	}
	
	public String getRevenueRatioKey(int type,boolean isOrderTime) {
		String key = getRevenueRatioColumnOrderBy(type);		
		if(isOrderTime ==true){
			key = key+"_ist";
		}
		return key;
	}

	public void putRevenueRatio(Integer type, StockRevenue sr,
			Map<String, Object> map) {
		if(type == StockConstants.DAILY_SR_TYPE) {
			map.put("revenueratio", sr.getRevenueRatio());
		} else if(type == StockConstants.WEEK_SR_TYPE) {
			map.put("revenueratio", sr.getWeekRevenueRatio());
		} else if(type == StockConstants.MONTH_SR_TYPE) {
			map.put("revenueratio", sr.getMonthRevenueRatio());
		} else if(type == StockConstants.YEAR_SR_TYPE) {
			map.put("revenueratio", sr.getYearRevenueRatio());
		} else if(type == StockConstants.TOTAL_SR_TYPE) {
			map.put("revenueratio", sr.getTotalRevenueRatio());
		}
	}

	public void loadTop100StockGameUsers(boolean isInDcss) {
		Integer limit = 100;		
		if(isInDcss) {
			for(int type = 1; type <= 5; type++) {
				boolean isInThisDcss = StockCacheUtil.isInThisDcss(String.valueOf(type), StockCacheUtil.getAppIndex());
				if(isInThisDcss) {
					String columnOrderBy = getRevenueRatioColumnOrderBy(type);
					//加载最近有操作的排名
					boolean isOrderTime = true;
//					String key = StockRevenueService.getInstance().getRevenueRatioKey(type,isOrderTime);
					Date beforeDate = getOrderTimeByType(isInThisDcss, type);					
					List<StockRevenue> srList = StockRevenueService.getInstance().selectRankList(columnOrderBy,beforeDate, 0, limit);
					saveTopRankStockRevenueList(type, srList,isOrderTime);
					//加载所有的排名
					isOrderTime = false;
//					key = StockRevenueService.getInstance().getRevenueRatioKey(type,isOrderTime);
					beforeDate = null;
					srList = StockRevenueService.getInstance().selectRankList(columnOrderBy,beforeDate, 0, limit);
					saveTopRankStockRevenueList(type, srList,isOrderTime);
					
				}
			}
		} else {
			for(int type = 1; type <= 5; type++) {
				//加载最近有操作的排名
				boolean isOrderTime = true;
				String columnOrderBy = getRevenueRatioColumnOrderBy(type);
//				String key = StockRevenueService.getInstance().getRevenueRatioKey(type,isOrderTime);				
				Date beforeDate = getOrderTimeByType(false, type); 
				List<StockRevenue> srList = StockRevenueService.getInstance().selectRankList(columnOrderBy, beforeDate,0, limit);
				UserServiceClient.getInstance().saveTopRankStockRevenueList(type, srList,isOrderTime);
				
				//加载所有的排名
				isOrderTime = false;							
				beforeDate = null;
//				key = StockRevenueService.getInstance().getRevenueRatioKey(type,isOrderTime);
				srList = StockRevenueService.getInstance().selectRankList(columnOrderBy, beforeDate,0, limit);
				UserServiceClient.getInstance().saveTopRankStockRevenueList(type, srList,isOrderTime);
			}
		}
	}
	
	
	/**
	 * @param isTradeBitMap 该系统是否有TradeBitMap [search工程未加载]
	 * @param type 同StockRevenueService.getInstance().getRevenueRatioKey
	 * @return 默认返回null
	 */
	public Date getOrderTimeByType(boolean isTradeBitMap,int type){
		String companycode = "000001.sh";//上证指数
		Date beforeDate = null ;
		if( type == StockConstants.DAILY_SR_TYPE ){
			int daySubnum = ConfigCenterFactory.getInt("stock_dbo.daySubnum", 5);
			if(daySubnum <1 ) return null;
			if(isTradeBitMap == true){				
				Long beforeDateLong = TradeBitMapService.getInstance().getNext(companycode, System.currentTimeMillis(), -daySubnum, Calendar.DAY_OF_MONTH);
				if(beforeDateLong != null){
					beforeDate = new Date(beforeDateLong);
				}
			}else{				
				List<Trade0001> list = loadDataFromDBByPage_miss(companycode, daySubnum);					
				if( list != null && list.size() > 0 ){
					beforeDate = list.get(0).getTime();
				}
			}
		}else if( type == StockConstants.WEEK_SR_TYPE ){
			int weekSubnum = ConfigCenterFactory.getInt("stock_dbo.weekSubnum", 4);
			if(weekSubnum <1 ) return null;
			if(isTradeBitMap == true){					
				Long beforeDateLong = TradeBitMapService.getInstance().getNext(companycode, System.currentTimeMillis(), -weekSubnum, Calendar.WEEK_OF_MONTH);
				if(beforeDateLong != null){
					beforeDate = new Date(beforeDateLong);
				}
			}else{				
				List<Trade0001> list = loadDataFromDBByPage_miss(companycode, weekSubnum*5);					
				if( list != null && list.size() > 0 ){
					beforeDate = list.get(0).getTime();
				}
			}			
		}else if( type == StockConstants.MONTH_SR_TYPE ){
			int monthSubnum = ConfigCenterFactory.getInt("stock_dbo.monthSubnum", 6);
			if(monthSubnum <1 ) return null;
			if(isTradeBitMap == true){				
				Long beforeDateLong = TradeBitMapService.getInstance().getNext(companycode, System.currentTimeMillis(), -monthSubnum, Calendar.MONTH);
				if(beforeDateLong != null){
					beforeDate = new Date(beforeDateLong);
				}
			}else{				
				List<Trade0001> list = loadDataFromDBByPage_miss(companycode, monthSubnum*25);					
				if( list != null && list.size() > 0 ){
					beforeDate = list.get(0).getTime();
				}
			}
		}
		logger.info(type+"=="+DateUtil.format2String(beforeDate));
		return beforeDate;
	}
	
	/**
	 * tangbinqi增加，获得第N个数据 需要Trade0001Mapper.xml
	 * @param companycode
	 * @param limit
	 * @return
	 */
	private List<Trade0001> loadDataFromDBByPage_miss(String companycode, int limit) {
		if(limit < 1)return null;
		Map<String, Object> m = new HashMap<String, Object>();
		String limitStr = (limit-1)+",1";
		m.put("limit", limitStr);
		m.put("companycode", companycode);
		List<Trade0001> retList = null;
		RequestMessage req = DAFFactory.buildRequest(
				"loadDataFromDBByPage_miss", m, StockConstants.common);
		ResponseMessage resp = new DBDefaultDaoImpl().queryForList(req);
		if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
			retList = (List) resp.getResult();
		}
		return retList;
	}

	public boolean saveTopRankStockRevenueList(int type, List<StockRevenue> seList,boolean isOrderTime) {
		if(seList == null || seList.size() == 0) {
			return false;
		}
		String key = StockRevenueService.getInstance().getRevenueRatioKey(type,isOrderTime);

		List<Long> uidList = new ArrayList<Long>();
		for(StockRevenue sr : seList) {
			uidList.add(sr.getUid());
		}
		try {
			logger.info(key+" loaddata " + uidList.size());
			LCEnter.getInstance().put(key, uidList, StockConstants.STOCK_RANK_CACHE);
		} catch (Exception e) {
			logger.error("saveTopRankStockRevenueList: " + e);
			return false;
		}

		return true;
	}
}
