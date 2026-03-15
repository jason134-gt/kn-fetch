package com.yfzx.service.hfunction;

import java.util.ArrayList;
import java.util.Calendar;
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
 * timeUnit:时间单位，nd多少天 $hrsi(timeUnit,nd)
 * 
 * @author：杨真
 * @date：2014年10月20日
 */
public class HRsiService implements IFService {

	private static HRsiService instance = new HRsiService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	String sz = "sz";
	String xd = "xd";

	public static HRsiService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 1) {
			String timeUnit = vls.get(0);
			Integer nd = Integer.valueOf(vls.get(1));

//			ret = rsi(timeUnit, req, nd);
//			System.out.println(ret);
			Double rs = rsa(req, timeUnit, nd, sz)
					/ rsa(req, timeUnit, nd, xd);
			ret = rs / (1 + rs)*100;
		}
		return ret;
	}

	public Double rsi(String timeUnit, IndexMessage req, Integer nd) {
		Double ret = 0.0;
		try {

			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);
			Calendar c = Calendar.getInstance();
			c.setTime(wetime);
			Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
					req.getUidentify(),
					HUtilService.getInstance().getIndexcodeByType(timeUnit,
							ComputeUtil.TRADE_HF_S));
			if (mintime == null)
				return 0.0;

			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_S));
			Date maxtime = new Date();
			Double sz_sum = 0.0;
			Double xd_sum = 0.0;
			int dc = 0;
			while (c.getTime().compareTo(mintime) >= 0
					&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {

				Date actime = IndexService.getInstance().formatTime(
						c.getTime(), d, req.getCompanyCode());
				if (actime != null) {
					// 昨收
					Double hfzs = HUtilService.getInstance().computeHFZS(
							req.getCompanyCode(), d, actime);

					// Double hfzs = IndexValueAgent.getIndexValue(
					// req.getCompanyCode(),
					// HUtilService.getInstance().getIndexcodeByType(
					// timeUnit, ComputeUtil.TRADE_ZS),
					// actime);

					// 现价
					Double cp = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_S), actime);
					if (hfzs != null && cp != null && hfzs != 0 && cp != 0) {
						// 差
						// Double ce = cp - hfzs;
						Double ce = IndexValueAgent
								.getIndexValue(
										req.getCompanyCode(),
										HUtilService.getInstance()
												.getIndexcodeByType(timeUnit,
														ComputeUtil.TRADE_SD),
										actime);
						if (ce < 0)
							xd_sum += Math.abs(ce);
						else
							sz_sum += Math.abs(ce);
					}
					dc++;
				}

				c.add(d.getTunit(), -d.getInterval());

			}

			if (xd_sum == null || xd_sum == 0 || sz_sum == null || sz_sum == 0)
				return ret;
			ret = sz_sum / (xd_sum + sz_sum) * 100;

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	public Double rsia(String timeUnit, IndexMessage req, Integer nd,
			String type) {
		Double ret = 0.0;
		try {

			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);
			Calendar c = Calendar.getInstance();
			c.setTime(wetime);
			Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
					req.getUidentify(),
					HUtilService.getInstance().getIndexcodeByType(timeUnit,
							ComputeUtil.TRADE_HF_S));
			if (mintime == null)
				return 0.0;

			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_S));
			Date maxtime = new Date();
			Double sz_sum = 0.0;
			Double xd_sum = 0.0;
			int dc = 0;
			while (c.getTime().compareTo(mintime) >= 0
					&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {

				Date actime = IndexService.getInstance().formatTime(
						c.getTime(), d, req.getCompanyCode());
				if (actime != null) {
					// 昨收
					Double hfzs = HUtilService.getInstance().computeHFZS(
							req.getCompanyCode(), d, actime);

					// Double hfzs = IndexValueAgent.getIndexValue(
					// req.getCompanyCode(),
					// HUtilService.getInstance().getIndexcodeByType(
					// timeUnit, ComputeUtil.TRADE_ZS),
					// actime);

					// 现价
					Double cp = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_S), actime);
					if (hfzs != null && cp != null && hfzs != 0 && cp != 0) {
						// 差
						// Double ce = cp - hfzs;
						Double ce = IndexValueAgent
								.getIndexValue(
										req.getCompanyCode(),
										HUtilService.getInstance()
												.getIndexcodeByType(timeUnit,
														ComputeUtil.TRADE_SD),
										actime);
						if (ce < 0)
							xd_sum += Math.abs(ce);
						else
							sz_sum += Math.abs(ce);
					}
					dc++;
				}

				c.add(d.getTunit(), -d.getInterval());

			}

			if (xd_sum == null || xd_sum == 0 || sz_sum == null || sz_sum == 0)
				return ret;
			if (type.equals(sz)) {
				ret = sz_sum / nd;
			}
			if (type.equals(xd)) {
				ret = xd_sum / nd;
			}

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	public Double rsa(IndexMessage req, String timeUnit, Integer nd, String type) {
		Double ret = 0.0;
		try {

			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);
			if(wetime==null) 
				return ret;
			IndexMessage mreq = (IndexMessage) req.clone();
			mreq.setTime(wetime);

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
					getThisKey(type, timeUnit, nd));
			Date sctime = IndexService.getInstance().getNextTradeUtilEnd(
					mintime, d, req.getCompanyCode(), nd);

			ret = doRsa(req, timeUnit, nd, d, mintime, indexcode, wetime ,
					type, sctime);

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	// 求第一天的rs
	private Double computeDv(IndexMessage req, String timeUnit, Integer nd,
			String type) {
		Double rs = 0.0;
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_HF_S));
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(),
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_S));
		if(mintime==null) 
			return rs;
		// 时间往后推n日
		Date pretime = IndexService.getInstance().getNextTradeUtilEnd(mintime,
				d, req.getCompanyCode(), nd);
		if (pretime != null) {
			IndexMessage creq = (IndexMessage) req.clone();
			creq.setTime(pretime);
			rs = rsia(timeUnit, creq, nd, type);
		}
		return rs;
	}

	private Double preRsa(IndexMessage req, String timeUnit, Integer nd,
			Dictionary d, Date mintime, String indexcode, String type,
			Date sctime) {
		Double ret = 0.0;
		try {

			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);

			if (wetime.compareTo(mintime) <= 0)
				return computeDv(req, timeUnit, nd, type);
			// 前一个交易日
			Date pretime = IndexService.getInstance().getNextTradeUtilEnd(
					wetime, d, req.getCompanyCode(), -1);
			if (pretime == null)
				return ret;
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
					String tindexcode = "rsia_" + type+nd;
//					ckey = StockUtil.getExtCachekey(req.getCompanyCode(),
//							tindexcode, pretime);
//					preValue = ExtCacheService.getInstance().get(ckey);
					preValue = TUextService.getInstance().getUExtDouble(req.getCompanyCode(), pretime.getTime(), tindexcode);
				}
				
			}

			if (preValue == null || preValue.isNaN() || preValue.isInfinite()) {

				IndexMessage creq = (IndexMessage) req.clone();
				creq.setTime(pretime);

				preValue = doRsa(creq, timeUnit, nd, d, mintime, indexcode,
						wetime, type, sctime);
				if (preValue != null) {
					ret = preValue;
					if (StringUtil.isEmpty(indexcode)) {
//						ExtCacheService.getInstance().putV(ckey, preValue);
						String tindexcode = "rsia_" + type+nd;
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

	private Double doRsa(IndexMessage req, String timeUnit, int nd,
			Dictionary d, Date mintime, String indexcode, Date ctime, String type, Date sctime) {
		Double ret = 50.0;
		// 今日涨跌额
		Double ce = IndexValueAgent.getIndexValue(
				req.getCompanyCode(),
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_SD), req.getTime());
		if (req.getTime().compareTo(sctime) < 0)
			return 50.0;
		if (req.getTime().compareTo(sctime) == 0)
			return computeDv(req, timeUnit, nd, type);
		if (type.equals(xd)) {
			if (ce < 0)
				ret = (preRsa(req, timeUnit, nd, d, mintime, indexcode,
						type, sctime) * (nd - 1) + Math.abs(ce))
						/ nd;
			else
				ret = (preRsa(req, timeUnit, nd, d, mintime, indexcode,
						type, sctime) * (nd - 1)) / nd;
		}
		if (type.equals(sz)) {
			if (ce > 0)
				ret = (preRsa(req, timeUnit, nd, d, mintime, indexcode,
						type, sctime) * (nd - 1) + Math.abs(ce))
						/ nd;
			else
				ret = (preRsa(req, timeUnit, nd, d, mintime, indexcode,
						type, sctime) * (nd - 1)) / nd;
		}
		return ret;
	}

	private String getThisKey(String prek, String timeUnit, Integer nd) {
		// TODO Auto-generated method stub
		return "rsi_" + prek + "_" + nd + "_" + timeUnit;
	}
}
