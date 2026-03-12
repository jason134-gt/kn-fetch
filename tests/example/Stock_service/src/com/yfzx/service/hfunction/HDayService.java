package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.RealTimeService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

/**
 * 取公司股价
 * 
 * @author：杨真
 * @date：2014-3-25
 */
public class HDayService {

	private static HDayService instance = new HDayService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private HDayService() {

	}

	public static HDayService getInstance() {
		return instance;
	}

	public Double getDayIndex(IndexMessage req, String type) {
		Double wi = null;
		Date time = DateUtil.getDayStartTime(req.getTime());
		if (time == null)
			return wi;
		req.setTime(time);
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_ZS, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_S, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_K, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_CJL, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_CJE, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_ZDF, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_SD, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_HSL, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_ZG, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_ZD, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_ZF)) {
			wi = computeDayZF(req);
		}
		if (type.equals(ComputeUtil.TRADE_5AVG)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_5AVG, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_10AVG)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_10AVG, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_20AVG)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_20AVG, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_30AVG)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_30AVG, req.getTime());
		}
		if (type.equals(ComputeUtil.TRADE_60AVG)) {
			wi = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_60AVG, req.getTime());
		}
		if (wi == null)
			wi = 0.0;
		return wi;
	}

	public Double computeDayIndex(IndexMessage req, String type) {
		Double wi = null;
		Date time = IndexService.getInstance().formatTimeByUnit(req.getTime(),
				Calendar.DAY_OF_MONTH, req.getCompanyCode());
		if (time == null)
			return wi;
		req.setTime(time);
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			wi = computeDayZS(req);
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			wi = computeDayS(req);
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			wi = computeDayK(req);
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			wi = computeDayCJL(req);
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			wi = computeDayCJE(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			wi = computeDayZDF(req);
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			wi = computeDaySD(req);
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			wi = computeDayHSL(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			wi = computeDayZG(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			wi = computeDayZD(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZF)) {
			wi = computeDayZF(req);
		}
		if (wi == null)
			wi = 0.0;
		return wi;
	}

	private Double computeDayZF(IndexMessage req) {
		Double zf = null;
		Double zg = computeDayZG(req);
		Double zd = computeDayZD(req);
		Double zs = computeDayZS(req);
		if (zs != null)
			zf = (zg - zd) / zs;
		return zf;
	}

	private Double computeDayZD(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);

		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_ZD, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	private Double computeDayZG(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_ZG, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	private Double computeDayHSL(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_HSL, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	private Double computeDaySD(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_SD, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	private Double computeDayZDF(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_ZDF, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	private Double computeDayCJE(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_CJE, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	private Double computeDayCJL(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return ret;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_CJL, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	private Double computeDayK(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_K, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	private Double computeDayZS(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_ZS, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	public Double computeDayS(IndexMessage req) {
		Double ret = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Date maxtime = new Date();
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_S, c.getTime());
			if (r != null) {
				ret = r;
				break;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return ret;
	}

	public void computeCompanysDayAvg(String ret) {
		if (!StringUtil.isEmpty(ret)) {
			String[] reta = ret.split("~");
			for (String cs : reta) {
				if (StringUtil.isEmpty(cs) || cs.toLowerCase().equals(".hk"))
					continue;
				String[] csa = cs.split("\\|");
				if (csa.length < 2)
					continue;
				String csh = csa[0];
				String companycode = csh.split("\\^")[0];

				computeDayTradeHavg(companycode);
			}
		}

	}

	public void computeDayTradeHavg(String companycode) {
		IndexMessage req = SMsgFactory.getCompanyMsg(companycode,
				StockConstants.INDEX_CODE_TRADE_S,
				DateUtil.getDayStartTime(new Date()));
		req.setNeedAccessExtRemoteCache(false);

		Double d5 = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S, ComputeUtil.TRADE_5AVG, 5);
		if (d5 != null && d5 != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_5AVG, d5);

		Double d10 = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S, ComputeUtil.TRADE_10AVG, 10);
		if (d10 != null && d10 != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_10AVG, d10);

		Double d20 = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S, ComputeUtil.TRADE_20AVG, 20);
		if (d20 != null && d20 != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_20AVG, d20);

		Double d30 = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S, ComputeUtil.TRADE_30AVG,30);
		if (d30 != null && d30 != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_30AVG, d30);

		Double d5w = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S_W, ComputeUtil.TRADE_5AVG,5);
		if (d5w != null && d5w != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_5AVG_W, d5w);

		Double d10w = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S_W, ComputeUtil.TRADE_10AVG,10);
		if (d10w != null && d10w != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_10AVG_W, d10w);

		Double d20w = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S_W, ComputeUtil.TRADE_20AVG,20);
		if (d20w != null && d20w != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_20AVG_W, d20w);

		Double d30w = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S_W, ComputeUtil.TRADE_30AVG,30);
		if (d30w != null && d30w != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_30AVG_W, d30w);

		Double d5m = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S_M, ComputeUtil.TRADE_5AVG,5);
		if (d5m != null && d5m != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_5AVG_M, d5m);

		Double d10m = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S_M, ComputeUtil.TRADE_10AVG,10);
		if (d10w != null && d10w != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_10AVG_M, d10m);

		Double d20m = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S_M, ComputeUtil.TRADE_20AVG,20);
		if (d20w != null && d20w != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_20AVG_M, d20m);

		Double d30m = HDayAvgService.getInstance().realtimeComputeAvgV2(req,
				StockConstants.INDEX_CODE_TRADE_S_M, ComputeUtil.TRADE_30AVG,30);
		if (d30w != null && d30w != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_30AVG_M, d30m);

		Double d5e = HDayAvgService.getInstance().realtimeComputeAvgV3(req,
				StockConstants.INDEX_CODE_TRADE_CJE, ComputeUtil.TRADE_5AVGE,5);
		if (d5e != null && d5e != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_5AVGE, d5e);

		Double d10e = HDayAvgService.getInstance().realtimeComputeAvgV3(req,
				StockConstants.INDEX_CODE_TRADE_CJE, ComputeUtil.TRADE_10AVGE,10);
		if (d10e != null && d10e != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_10AVGE, d10e);

		Double d30e = HDayAvgService.getInstance().realtimeComputeAvgV3(req,
				StockConstants.INDEX_CODE_TRADE_CJE, ComputeUtil.TRADE_30AVGE,30);
		if (d30e != null && d30e != 0)
			RealTimeService.getInstance().put2LocalCache(companycode,
					StockConstants.INDEX_CODE_TRADE_30AVGE, d30e);

	}

}
