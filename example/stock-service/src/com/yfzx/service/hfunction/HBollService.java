package com.yfzx.service.hfunction;

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
import com.yfzx.service.db.TUextService;
import com.yfzx.service.db.USubjectService;

/**
 * 布林线 timeUnit:时间单位，nd多少天 $hboll(timeUnit,nd,type,htype) type:"mb","up","dn"
 * htype:"hf":后复权，“qf”:前复权，“nf”:不复权
 * 
 * @author：杨真
 * @date：2014年10月20日
 */
public class HBollService implements IFService {

	private static HBollService instance = new HBollService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	public static HBollService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 2) {
			String timeUnit = vls.get(0);
			Integer nd1 = Integer.valueOf(vls.get(1));
			String type = vls.get(2);
			String htype = vls.get(3);
			try {

				if (type.equals("mb")) {
					return mb(timeUnit, req, nd1, htype);
				}

				if (type.equals("up")) {
					// MA=N日内的收盘价之和÷N
					Double mb = mb(timeUnit, req, nd1, htype);
					// MD=平方根N日的（C－MA）的两次方之和除以N
					Double md = md(timeUnit, req, nd1, htype);
					return mb + 2 * md;
				}

				if (type.equals("dn")) {
					// MA=N日内的收盘价之和÷N
					Double mb = mb(timeUnit, req, nd1, htype);
					// MD=平方根N日的（C－MA）的两次方之和除以N
					Double md = md(timeUnit, req, nd1, htype);
					return mb - 2 * md;
				}
			} catch (Exception e) {
				log.error("HRsvService compute failed!", e);
			}
		}
		return ret;
	}

	private Double mb(String timeUnit, IndexMessage req, Integer nd1,
			String htype) {
		Double ret = 0.0;
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_S));

		Date wetime = req.getTime();
		wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(timeUnit,
				wetime);
		if (wetime != null) {
			Date pretime = IndexService.getInstance().getNextTradeUtilEnd(
					wetime, d, req.getCompanyCode(), -1);
			if (pretime != null) {
				IndexMessage creq = (IndexMessage) req.clone();
				creq.setTime(pretime);
				Double ma = ma(timeUnit, creq, nd1, htype);
				if (ma != null)
					ret = ma;
			}
		}
		return ret;
	}

	private Double md(String timeUnit, IndexMessage req, Integer nd,
			String htype) {
		Double avg = 0.0;
		Date wetime = req.getTime();
		wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(timeUnit,
				wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(),
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_S));
		if (mintime == null)
			return avg;

		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_S));
		Date maxtime = new Date();

//		String ckey = "";
		Double v = null;
		if (DateUtil.getDayStartTime(new Date()).compareTo(wetime) != 0) {
			// 临时缓存:格式
			// 000002.sz^1414425600000^2022
			String tindexcode = "md_" + nd + "_" + htype;
//			ckey = StockUtil.getExtCachekey(req.getCompanyCode(), tindexcode,
//					wetime);
			v = TUextService.getInstance().getUExtDouble(req.getCompanyCode(), wetime.getTime(), tindexcode);
		}

		if (v == null) {
			int dc = 0;
			while (c.getTime().compareTo(mintime) >= 0
					&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {

				Date actime = IndexService.getInstance().formatTime(
						c.getTime(), d, req.getCompanyCode());
				if (actime != null) {
					Double cp = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit,
									HUtilService.getInstance().getHfIndexType(
											htype, ComputeUtil.TRADE_S)),
							actime);
					if (cp != null && cp != 0) {
						IndexMessage creq = (IndexMessage) req.clone();
						creq.setTime(actime);
						// 现价
						Double ma = ma(timeUnit, creq, nd, htype);
						if (ma != null && ma != 0) {
							// 差
							Double cz = cp - ma;
							if (cz != 0) {
								avg += Math.pow(cz, 2);

								dc++;
							}
						}

					}

				}

				c.add(d.getTunit(), -d.getInterval());

			}
			if (dc != 0) {
				avg = Math.sqrt(avg / dc);

				if (avg != null) {
					String tindexcode = "md_" + nd + "_" + htype;
					TUextService.getInstance().putData(req.getCompanyCode(), wetime.getTime(), tindexcode, avg.floatValue());
//					ExtCacheService.getInstance().putV(ckey, avg);
				}
			}
		} else {
			avg = v;
		}

		return avg;
	}

	private Double ma(String timeUnit, IndexMessage req, Integer nd,
			String htype) {
		Double avg = 0.0;
		Date wetime = req.getTime();
		wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(timeUnit,
				wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(),
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_S));
		if (mintime == null)
			return avg;

		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_S));
		Date maxtime = new Date();

		String ckey = "";
		Double v = null;
		if (DateUtil.getDayStartTime(new Date()).compareTo(wetime) != 0) {
			// 临时缓存:格式
			// 000002.sz^1414425600000^2022
			String tindexcode = "ma_" + nd + "_" + htype;
//			ckey = StockUtil.getExtCachekey(req.getCompanyCode(), tindexcode,
//					wetime);
//			v = ExtCacheService.getInstance().get(ckey);
			v = TUextService.getInstance().getUExtDouble(req.getCompanyCode(), wetime.getTime(), tindexcode);
		}

		if (v == null) {
			int dc = 0;
			while (c.getTime().compareTo(mintime) >= 0
					&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {

				Date actime = IndexService.getInstance().formatTime(
						c.getTime(), d, req.getCompanyCode());
				if (actime != null) {
					// 现价
					Double cp = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit,
									HUtilService.getInstance().getHfIndexType(
											htype, ComputeUtil.TRADE_S)),
							actime);
					if (cp != null && cp != 0) {
						// 差
						avg += cp;
					}
					dc++;
				}
				c.add(d.getTunit(), -d.getInterval());
			}

			if (dc != 0) {
				avg = avg / dc;

				if (avg != null) {
//					ExtCacheService.getInstance().putV(ckey, avg);
					String tindexcode = "ma_" + nd + "_" + htype;
					TUextService.getInstance().putData(req.getCompanyCode(), wetime.getTime(), tindexcode, avg.floatValue());
				}
			}
		} else {
			avg = v;
		}

		return avg;
	}

}
