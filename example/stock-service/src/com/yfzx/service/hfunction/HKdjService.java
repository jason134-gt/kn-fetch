package com.yfzx.service.hfunction;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.RealTimeService;
import com.yfzx.service.db.TUextService;
import com.yfzx.service.db.USubjectService;

/**
 * timeUnit:时间单位，nd多少天 $hkdj(timeUnit,nd,type) type:"k","d","j"
 * 
 * @author：杨真
 * @date：2014年10月20日
 */
public class HKdjService implements IFService {

	private static HKdjService instance = new HKdjService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	public static HKdjService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 2) {
			try {
				String timeUnit = vls.get(0);
				Integer nd = Integer.valueOf(vls.get(1));
				String type = vls.get(2);
				Date wetime = req.getTime();
				wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
						timeUnit, wetime);
				if (wetime != null) {
					IndexMessage mreq = (IndexMessage) req.clone();
					mreq.setTime(wetime);
					if (type.equals("k")) {
						ret = kdjk(mreq, timeUnit, nd);
					}
					if (type.equals("d")) {
						ret = d(mreq, timeUnit, nd, 50.0);
					}
					if (type.equals("j")) {

						ret = kdjJ(mreq, timeUnit, nd);
					}
				}
			} catch (Exception e) {
				log.error("HKdjService failed!", e);
			}

		}
		return ret;
	}

	public Double d(IndexMessage req, String timeUnit, Integer nd, Double dv) {
		Double ret = 50.0;
		try {
			Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
					req.getUidentify(),
					HUtilService.getInstance().getIndexcodeByType(timeUnit,
							ComputeUtil.TRADE_S));
			if(mintime==null) 
				return ret;
			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_S));
			String indexcode = HUtilService.getInstance().getIndexCode(
					getThisKey("kdj_d", timeUnit, nd));
			if (req.getTime().compareTo(mintime) <= 0)
				return ret;
			else {
				ret = getPreD(req, timeUnit, nd, dv, d, mintime, indexcode) * 2
						/ 3 + k(req, timeUnit, nd, dv) * 1 / 3;
			}

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	private Double getPreD(IndexMessage req, String timeUnit, Integer nd,
			Double dv, Dictionary d, Date mintime, String indexcode) {
		Double ret = 0.0;
		try {
			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);

			// 前一个交易日
			Date pretime = IndexService.getInstance().getNextTradeUtilEnd(
					wetime, d, req.getCompanyCode(), -1);
			if (pretime == null)
				return dv;

			if (pretime.compareTo(mintime) < 0)
				return dv;
			
			Double preValue = null;
			String ckey = "";
			if (!StringUtil.isEmpty(indexcode)) {
				// 前一个交易日的参数值
				preValue = IndexValueAgent.getIndexValueNotRealCompute(
						req.getCompanyCode(), indexcode, pretime);
			} else {
				if(DateUtil.getDayStartTime(new Date()).compareTo(pretime)!=0)
				{
					// 临时缓存:格式
					// 000002.sz^1414425600000^2022
					String tindexcode = "kdjd_" + nd;
//					ckey = StockUtil.getExtCachekey(req.getCompanyCode(),
//							tindexcode, pretime);
//					preValue = ExtCacheService.getInstance().get(ckey);
					preValue = TUextService.getInstance().getUExtDouble(req.getCompanyCode(), pretime.getTime(), tindexcode);
				}
				
			}

			if (preValue == null || preValue.isNaN() || preValue.isInfinite()) {

				IndexMessage creq = (IndexMessage) req.clone();
				creq.setTime(pretime);

				preValue = getPreD(creq, timeUnit, nd, dv, d, mintime,
						indexcode) * 2 / 3 + k(creq, timeUnit, nd, dv) * 1 / 3;
				if (preValue != null) {
					ret = preValue;
					if (StringUtil.isEmpty(indexcode)) {
//						ExtCacheService.getInstance().putV(ckey, preValue);
						String tindexcode = "kdjd_" + nd;
						TUextService.getInstance().putData(req.getCompanyCode(), pretime.getTime(), tindexcode, preValue.floatValue());
					} else {
						// 缓存中间结果
						RealTimeService.getInstance().put2LocalCache(
								req.getCompanyCode(), indexcode, preValue,
								pretime);
					}
				}
			} else {
				ret = preValue;
			}

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	private String getThisKey(String prek, String timeUnit, Integer nd) {
		// TODO Auto-generated method stub
		return prek + nd + "_" + timeUnit;
	}

	public Double kdjJ(IndexMessage req, String timeUnit, Integer nd) {
		Double ret = 50.0;
		try {

			ret = 3 * kdjk(req, timeUnit, nd) - 2 * kdjd(req, timeUnit, nd);

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	public Double kdjd(IndexMessage req, String timeUnit, Integer nd) {
		Double ret = 50.0;
		try {

			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);

			IndexMessage mreq = (IndexMessage) req.clone();
			mreq.setTime(wetime);

			ret = d(mreq, timeUnit, nd, 50.0);

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	public Double kdjk(IndexMessage req, String timeUnit, Integer nd) {
		Double ret = 50.0;
		try {

			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);

			IndexMessage mreq = (IndexMessage) req.clone();
			mreq.setTime(wetime);

			ret = k(mreq, timeUnit, nd, 50.0);

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	public Double k(IndexMessage req, String timeUnit, Integer nd, Double dv) {
		Double ret = 50.0;
		try {

			Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
					req.getUidentify(),
					HUtilService.getInstance().getIndexcodeByType(timeUnit,
							ComputeUtil.TRADE_S));
			if(mintime==null) 
				return ret;
			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_S));
			String indexcode = HUtilService.getInstance().getIndexCode(
					getThisKey("kdj_k", timeUnit, nd));

			ret = getPrek(req, timeUnit, nd, dv, d, mintime, indexcode) * 2 / 3
					+ HRsvService.getInstance().rsv(req, timeUnit, nd) * 1 / 3;

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	private Double getPrek(IndexMessage req, String timeUnit, Integer nd,
			Double dv, Dictionary d, Date mintime, String indexcode) {
		Double ret = 0.0;
		try {

			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);

			// 前一个交易日
			Date pretime = IndexService.getInstance().getNextTradeUtilEnd(
					wetime, d, req.getCompanyCode(), -1);
			if (pretime == null)
				return dv;
			if (pretime.compareTo(mintime) <= 0)
				return dv;
			Double preValue = null;
			String ckey = "";
			if (!StringUtil.isEmpty(indexcode)) {
				// 前一个交易日的参数值
				preValue = IndexValueAgent.getIndexValueNotRealCompute(
						req.getCompanyCode(), indexcode, pretime);
			} else {
				if(DateUtil.getDayStartTime(new Date()).compareTo(pretime)!=0)
				{
					// 临时缓存:格式
					// 000002.sz^1414425600000^2022
					String tindexcode = "kdjk_" + nd;
//					ckey = StockUtil.getExtCachekey(req.getCompanyCode(),
//							tindexcode, pretime);
//					preValue = ExtCacheService.getInstance().get(ckey);
					preValue = TUextService.getInstance().getUExtDouble(req.getCompanyCode(), pretime.getTime(), tindexcode);
				}
				
			}
			if (preValue == null || preValue.isNaN() || preValue.isInfinite()) {

				IndexMessage creq = (IndexMessage) req.clone();
				creq.setTime(pretime);
				preValue = getPrek(creq, timeUnit, nd, dv, d, mintime,
						indexcode)
						* 2
						/ 3
						+ HRsvService.getInstance().rsv(creq, timeUnit, nd)
						* 1
						/ 3;
				if (preValue != null) {
					ret = preValue;
					if (StringUtil.isEmpty(indexcode)) {
//						ExtCacheService.getInstance().putV(ckey, preValue);
						String tindexcode = "kdjk_" + nd;
						TUextService.getInstance().putData(req.getCompanyCode(), pretime.getTime(), tindexcode, preValue.floatValue());
					} else {
						// 缓存中间结果
						RealTimeService.getInstance().put2LocalCache(
								req.getCompanyCode(), indexcode, preValue,
								pretime);
					}
				}
			} else {
				ret = preValue;
			}

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

}
