package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.USubject;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.trade.TradeCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

/**
 * 日线，周线，月线共用类
 * 
 * @author：杨真
 * @date：2014年8月28日
 */
public class HUtilService {

	private static HUtilService instance = new HUtilService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	static Map<String, String> _imap = new HashMap<String, String>();

	static {
		init();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			@Override
			public void refresh() {

				init();
			}
		});
	}

	private static void init() {
		String p = ConfigCenterFactory.getString("compute.name_indexcode_maps",
				"");
		if(!StringUtil.isEmpty(p))
		{
			for (String kp : p.split(";")) {
				String[] ks = kp.split(":");
				_imap.put(ks[0], ks[1]);
			}
		}
		else
			_imap.clear();
		
	}

	public String getIndexCode(String k) {
		if (StringUtil.isEmpty(k))
			return null;
		return _imap.get(k);
	}

	private HUtilService() {

	}

	public static HUtilService getInstance() {
		return instance;
	}

	
	

	public int getMonthTradeDateCount(IndexMessage req) {
		int count = 0;
		// 一周开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getDayStartTime(new Date());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		// 从前一天开始算
		c.add(Calendar.DAY_OF_MONTH, -1);
		while (c.getTime().compareTo(wstime) >= 0) {

			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				count++;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return count;
	}

	public int getWeekTradeDateCount(IndexMessage req) {
		int count = 0;
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getDayStartTime(new Date());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		// 从前一天开始算
		c.add(Calendar.DAY_OF_MONTH, -1);
		while (c.getTime().compareTo(wstime) >= 0) {

			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				count++;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return count;
	}
	public Long getCurDayTradeLong(String uidentify,Long cur,Long start) {

		Long ctradeRegion = getTradeTimeRegion(uidentify);
		USubject us = USubjectService.getInstance()
				.getUSubjectByUIdentifyFromCache(uidentify);
		// 说明今天没有开盘
		if (us.getUidentify().endsWith("hk")) {
			if (TradeCenter.getInstance().getTradeStatus(1) != 1
					&& us.getKaiPanTime() == null)
				return -1l;
		} else {
			if (TradeCenter.getInstance().getTradeStatus(0) != 1
					&& us.getKaiPanTime() == null)
				return -1l;
		}
		Calendar c1 = Calendar.getInstance();
		c1.setTimeInMillis(cur);
		int h1 = c1.get(Calendar.HOUR_OF_DAY);
		if(start==null)
		{
			start = DateUtil.getDefaultKPaiTime();
		}
		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis(start);
		int h2 = c2.get(Calendar.HOUR_OF_DAY);
		
		if(us.getKaiPanTime() == null){//停牌的股票 wstock没提供最后时间
			return -1l;
		}
//		Long tradetime =  cur- us.getKaiPanTime();
		Long tradetime =  cur- start;
		if (us.getUidentify().endsWith("hk")) {
			if (h1 >= 16)
				return ctradeRegion;
			if (h1 >= 13&&h2<12) {
				// 去掉港股的午市休市时间
				if (tradetime < 3600000) {
					// 说明上午没有开市
					ctradeRegion = tradetime;
				} else {
					//港股午间休市时间为1小时
					ctradeRegion = tradetime - 3600000;
				}

			} else {
				ctradeRegion = tradetime;
			}
		} else {
			if (h1 >= 15)
				return ctradeRegion;
			if (h1 >= 13 && h2<12) {
				// 去掉A股的午市休市时间
				if (tradetime < 3600000) {
					// 说明上午没有开市
					ctradeRegion = tradetime;
				} else {
					//A股午间休市时间为1.5小时 90分钟
					ctradeRegion = tradetime - 5400000;
				}
			} else {
				ctradeRegion = tradetime;
			}
		}
		return ctradeRegion;
	}
	public Long getCurDayTradeLong(String uidentify,Long cur) {

		USubject us = USubjectService.getInstance()
				.getUSubjectByUIdentifyFromCache(uidentify);
		return getCurDayTradeLong(uidentify, cur, us.getKaiPanTime());
	}

	public Long getTradeTimeRegion(String uidentify) {
		if (uidentify.endsWith("hk")) {
			// 港股交易时长为
			return 330 * 60 * 1000l;
		}
		return 240 * 60 * 1000l;
	}


	/**
	 * 根据类型取对应的指标编码 timeUnit:'d','w','m'
	 * 
	 * @param type
	 * @return
	 */
	public String getIndexcodeByType(String timeUnit, String type) {
		String indexcode = null;
		if (timeUnit.equals(StockConstants.TIMEUNIT_m))
			return getMonthIndexcodeByType(type);
		if (timeUnit.equals(StockConstants.TIMEUNIT_w))
			return getWeekIndexcodeByType(type);
		if (timeUnit.equals(StockConstants.TIMEUNIT_d))
			return getDayIndexcodeByType(type);
		return indexcode;
	}

	private String getDayIndexcodeByType(String type) {
		String indexcode = null;
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZS;
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_S;
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_K;
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_CJL;
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_CJE;
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZDF;
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_SD;
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HSL;
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZG;
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZD;
		}
		if (type.equals(ComputeUtil.TRADE_5AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_5AVG;
		}
		if (type.equals(ComputeUtil.TRADE_10AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_10AVG;
		}
		if (type.equals(ComputeUtil.TRADE_20AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_20AVG;
		}
		if (type.equals(ComputeUtil.TRADE_30AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_30AVG;
		}
		if (type.equals(ComputeUtil.TRADE_60AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_60AVG;
		}

		if (type.equals(ComputeUtil.TRADE_HF_S)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_S;
		}
		if (type.equals(ComputeUtil.TRADE_HF_K)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_K;
		}

		if (type.equals(ComputeUtil.TRADE_HF_ZG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_ZG;
		}
		if (type.equals(ComputeUtil.TRADE_HF_ZD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_ZD;
		}
		if (type.equals(ComputeUtil.TRADE_HF_5AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_5AVG;
		}
		if (type.equals(ComputeUtil.TRADE_HF_10AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_10AVG;
		}
		if (type.equals(ComputeUtil.TRADE_HF_20AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_20AVG;
		}
		if (type.equals(ComputeUtil.TRADE_HF_30AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_30AVG;
		}
		if (type.equals(ComputeUtil.TRADE_HF_60AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HFQ_60AVG;
		}
		
		
		if (type.equals(ComputeUtil.TRADE_5AVGE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_5AVGE;
		}
		if (type.equals(ComputeUtil.TRADE_10AVGE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_10AVGE;
		}
		if (type.equals(ComputeUtil.TRADE_30AVGE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_30AVGE;
		}
		
		if (type.equals(ComputeUtil.TRADE_5AVGL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_5AVGL;
		}
		if (type.equals(ComputeUtil.TRADE_10AVGL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_10AVGL;
		}
		if (type.equals(ComputeUtil.TRADE_30AVGL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_30AVGL;
		}
		return indexcode;
	}

	private String getWeekIndexcodeByType(String type) {
		String indexcode = null;
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZS_W;
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_S_W;
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_K_W;
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_CJL_W;
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_CJE_W;
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZDF_W;
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_SD_W;
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HSL_W;
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZG_W;
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZD_W;
		}
		if (type.equals(ComputeUtil.TRADE_5AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_5AVG_W;
		}
		if (type.equals(ComputeUtil.TRADE_10AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_10AVG_W;
		}
		if (type.equals(ComputeUtil.TRADE_20AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_20AVG_W;
		}
		if (type.equals(ComputeUtil.TRADE_30AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_30AVG_W;
		}
		if (type.equals(ComputeUtil.TRADE_60AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_60AVG_W;
		}

		if (type.equals(ComputeUtil.TRADE_HF_S)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_S_W;
		}
		if (type.equals(ComputeUtil.TRADE_HF_K)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_K_W;
		}

		if (type.equals(ComputeUtil.TRADE_HF_ZG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_ZG_W;
		}
		if (type.equals(ComputeUtil.TRADE_HF_ZD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_ZD_W;
		}
		if (type.equals(ComputeUtil.TRADE_HF_5AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_5AVG_W;
		}
		if (type.equals(ComputeUtil.TRADE_HF_10AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_10AVG_W;
		}
		if (type.equals(ComputeUtil.TRADE_HF_20AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_20AVG_W;
		}
		if (type.equals(ComputeUtil.TRADE_HF_30AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_30AVG_W;
		}
		if (type.equals(ComputeUtil.TRADE_HF_60AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_60AVG_W;
		}
		
		if (type.equals(ComputeUtil.TRADE_5AVGE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_5AVGE_W;
		}
		if (type.equals(ComputeUtil.TRADE_10AVGE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_10AVGE_W;
		}
		return indexcode;
	}

	private String getMonthIndexcodeByType(String type) {
		String indexcode = null;
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZS_M;
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_S_M;
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_K_M;
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_CJL_M;
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_CJE_M;
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZDF_M;
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_SD_M;
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HSL_M;
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZG_M;
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_ZD_M;
		}
		if (type.equals(ComputeUtil.TRADE_5AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_5AVG_M;
		}
		if (type.equals(ComputeUtil.TRADE_10AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_10AVG_M;
		}
		if (type.equals(ComputeUtil.TRADE_20AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_20AVG_M;
		}
		if (type.equals(ComputeUtil.TRADE_30AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_30AVG_M;
		}
		if (type.equals(ComputeUtil.TRADE_60AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_60AVG_M;
		}

		if (type.equals(ComputeUtil.TRADE_HF_S)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_S_M;
		}
		if (type.equals(ComputeUtil.TRADE_HF_K)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_K_M;
		}

		if (type.equals(ComputeUtil.TRADE_HF_ZG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_ZG_M;
		}
		if (type.equals(ComputeUtil.TRADE_HF_ZD)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_ZD_M;
		}
		if (type.equals(ComputeUtil.TRADE_HF_5AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_5AVG_M;
		}
		if (type.equals(ComputeUtil.TRADE_HF_10AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_10AVG_M;
		}
		if (type.equals(ComputeUtil.TRADE_HF_20AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_20AVG_M;
		}
		if (type.equals(ComputeUtil.TRADE_HF_30AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_30AVG_M;
		}
		if (type.equals(ComputeUtil.TRADE_HF_60AVG)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_HF_60AVG_M;
		}
		
		if (type.equals(ComputeUtil.TRADE_5AVGE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_5AVGE_M;
		}
		if (type.equals(ComputeUtil.TRADE_10AVGE)) {
			indexcode = StockConstants.INDEX_CODE_TRADE_10AVGE_M;
		}
		return indexcode;
	}

	public Date getHStartTimeByTimeUnit(String timeUnit, Date time) {

		Date d = DateUtil.getDayStartTime(time);
		if (timeUnit.equals(StockConstants.TIMEUNIT_m))
			return DateUtil.getMonthStartTime(time);
		if (timeUnit.equals(StockConstants.TIMEUNIT_w))
			return DateUtil.getWeekStartTime(time);
		if (timeUnit.equals(StockConstants.TIMEUNIT_d))
			return DateUtil.getDayStartTime(time);
		return d;
	}

	public Date getHEndTimeByTimeUnit(String timeUnit, Date time) {

		Date d = DateUtil.getDayStartTime(time);
		if (timeUnit.equals(StockConstants.TIMEUNIT_m))
			return DateUtil.getMonthEndTime(time);
		if (timeUnit.equals(StockConstants.TIMEUNIT_w))
			return DateUtil.getWeekEndTime(time);
		return d;
	}

	/**
	 * 计算后复权的昨日收盘价
	 * 
	 * @param companyCode
	 * @param d
	 * @return
	 */
	public Double computeHFZS(String companyCode, Dictionary d, Date time) {
		Double hfzs = null;
		Calendar c = Calendar.getInstance();
		c.setTime(time);
		// 往前推一个时间单位
		Date pretime = IndexService.getInstance().getNextTradeUtilEnd(time,
							d, companyCode, -1);
		if(pretime!=null)
		{
			hfzs = IndexValueAgent.getIndexValue(
					companyCode,
					HUtilService.getInstance().getIndexcodeByType(
							d.getTimeUnit(), ComputeUtil.TRADE_HF_S),
					pretime);
		}
		return hfzs;
	}
	
	public String getHfIndexType(String prek, String indexType) {
		if (prek.equals("nf"))
			return indexType;
		return prek + "_" + indexType;
	}
}
