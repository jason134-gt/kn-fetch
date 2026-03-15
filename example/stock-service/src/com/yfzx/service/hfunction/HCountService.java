package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 * 统计
 * timeUnit:时间单位，nd多少天 $hcount(timeUnit,nd,type)
 * type:"yangx"：阳线,"yinx":阴线,"yyb"：阳线数/阴线数 ,"sz":上涨，“xd”：下跌，“zdb”:涨跌比
 * yyeb:阳成交额/阴成交额
 * @author：杨真
 * @date：2014年10月20日
 */
public class HCountService implements IFService {

	private static HCountService instance = new HCountService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HCountService() {

	}

	public static HCountService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 2) {
			String timeUnit = vls.get(0);
			Integer nd = Integer.valueOf(vls.get(1));
			String type = vls.get(2);
			ret = count(req, timeUnit, nd, type);
		}
		return ret;
	}

	public Double count(IndexMessage req, String timeUnit, Integer nd,
			String type) {
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_HF_S));
		Date maxtime = new Date();
		Double count = 0.0;
		int dc = 0;
		Double ac = 0.0;
		Double bc = 0.0;
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {
			
			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode()))
			{
				Double s = IndexValueAgent.getIndexValue(
						req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(timeUnit,
								ComputeUtil.TRADE_S), c.getTime());
				if (type.equals("sz") || type.equals("xd")) {
					Double zs = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(timeUnit,
									ComputeUtil.TRADE_ZS), c.getTime());
					if (s != null && zs != null && s != 0 && zs != 0) {
						if (type.equals("sz")) {
							if (s > zs) {
								count++;
							}
						}
						if (type.equals("xd")) {
							if (s < zs) {
								count++;
							}
						}
						dc++;
					}

				}
				if (type.equals("yangx") || type.equals("yinx")) {
					Double k = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(timeUnit,
									ComputeUtil.TRADE_K), c.getTime());
					if (s != null && k != null && s != 0 && k != 0) {
						if (type.equals("yangx")) {
							if (s > k) {
								count++;
							}
						}
						if (type.equals("yinx")) {
							if (s < k) {
								count++;
							}

						}
						dc++;
					}

				}

				if (type.equals("yyeb")) {
					Double k = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(timeUnit,
									ComputeUtil.TRADE_K), c.getTime());
					if (s != null && k != null && s != 0 && k != 0) {
						Double cje = IndexValueAgent.getIndexValue(
								req.getCompanyCode(),
								HUtilService.getInstance().getIndexcodeByType(timeUnit,
										ComputeUtil.TRADE_CJE), c.getTime());
						if(cje!=null&&cje!=0)
						{
							if (s >= k) {
								ac+=cje;
							}
							if (s < k) {
								bc+=cje;
							}
							dc++;
						}
					}
				}
				if (type.equals("yyb")) {
					Double k = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(timeUnit,
									ComputeUtil.TRADE_K), c.getTime());
					if (s != null && k != null && s != 0 && k != 0) {
						if (s >= k) {
							ac++;
						}
						if (s < k) {
							bc++;
						}
						dc++;
					}
				}
				
				if (type.equals("zdb")) {
					Double zs = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(timeUnit,
									ComputeUtil.TRADE_ZS), c.getTime());
					if (s != null && zs != null && s != 0 && zs != 0) {
						if (s > zs) {
							ac++;
						}

						if (s < zs) {
							bc++;
						}
						dc++;
					}

				}
				
				if (type.equals("zf")) {
					Double czg = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_ZG), c.getTime());
					Double czd = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_ZD), c.getTime());
					if (czg != null && czg != 0 && czd != null && czd != 0) {
						if (czg > ac) {
							ac = czg;
						}
						
						if ((bc == 0 || czd < bc)) {
							bc = czd;
						}
					}
					dc++;
				}
			}
			
			
			
			c.add(d.getTunit(), -d.getInterval());

		}

		if (type.equals("yyb")) {
			if (bc != 0 || ac != 0)
				count = ac / (ac + bc);
		}
		if (type.equals("yyeb")) {
			if (bc != 0 || ac != 0)
				count = ac / bc;
		}
		if (type.equals("zdb")) {
			if (bc != 0 || ac != 0)
				count = ac / (ac + bc);
		}
		if (type.equals("zf")) {
			if (bc != 0 || ac != 0)
				count = (ac-bc) / bc;
		}
		return count;
	}
}
