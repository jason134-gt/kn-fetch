package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.IndexService;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

/**
 * 月数据
 * 
 * @author：杨真
 * @date：2014-3-25
 */
public class HMonthService {

	private static HMonthService instance = new HMonthService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private HMonthService() {

	}

	public static HMonthService getInstance() {
		return instance;
	}

	/**
	 * 正常情况下，行情数据没有全部加载，所以以这种笨办法取
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	public Double getMonthIndex(IndexMessage req, String type) {
		// 一月开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		// 一月结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double v = 0.0;
		while (c.getTime().compareTo(wstime) >= 0) {

			v = getMonthIndexV2(req.getCompanyCode(),c.getTime(), type);
			if (v != null && v != 0)
				break;
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return v;
	}

	/**
	 * 取月指标，对传入时间需要格式化
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	private Double getMonthIndexV2(String companycode,Date time, String type) {
		Double wi = 0.0;
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZS_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_S_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_K_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_CJL_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_CJE_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZDF_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_SD_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_HSL_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZG_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZD_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_ZF)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZF_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_5AVG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_5AVG_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_10AVG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_10AVG_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_20AVG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_20AVG_M, time);
		}
		if (type.equals(ComputeUtil.TRADE_30AVG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_30AVG_M, time);
		}
		if (wi == null)
			wi = 0.0;
		return wi;
	}

	/**
	 * 取月指标，对传入时间需要格式化
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	public Double computeMonthIndex(IndexMessage req, String type) {
		Double wi = 0.0;
		Date time = IndexService.getInstance().formatTimeByUnit(req.getTime(),
				Calendar.MONTH, req.getCompanyCode());
		if (time == null)
			return wi;
		req.setTime(time);
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			wi = computeMonthZS(req);
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			wi = computeMonthS(req);
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			wi = computeMonthK(req);
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			wi = computeMonthCJL(req);
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			wi = computeMonthCJE(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			wi = computeMonthZDF(req);
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			wi = computeMonthSD(req);
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			wi = computeMonthHSL(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			wi = computeMonthZG(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			wi = computeMonthZD(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZF)) {
			wi = computeMonthZF(req);
		}
		if (wi == null)
			wi = 0.0;
		return wi;
	}

	private Double computeMonthZF(IndexMessage req) {
		Double zf = null;
		Double zg = computeMonthZG(req);
		Double zd = computeMonthZD(req);
		Double zs = computeMonthZS(req);
		if (zs != null && zd != null && zg != null)
			zf = (zg - zd) / zs;
		return zf;
	}

	private Double computeMonthZDF(IndexMessage req) {
		Double s = computeMonthS(req);
		Double k = computeMonthZS(req);
		Double zdf = null;
		if (k != null && s != null)
			zdf = (s - k) / k * 100;
		return zdf;
	}

	private Double computeMonthSD(IndexMessage req) {
		Double s = computeMonthS(req);
		Double k = computeMonthZS(req);
		Double zd = null;
		if (k != null && s != null)
			zd = s - k;
		return zd;
	}

	// 计算上月收盘价
	private Double computeMonthZS(IndexMessage req) {
		// 一月开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wstime);
		// 一月结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		while (c.getTime().compareTo(wetime) <= 0) {
			// 如果不是交易日，就往后推一天
			if (!IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				c.add(Calendar.DAY_OF_MONTH, 1);
			} else {
				return IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_ZS, c.getTime());
			}

		}
		return null;
	}

	// 计算本月开盘价
	private Double computeMonthK(IndexMessage req) {
		// 一月开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wstime);
		// 一月结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		while (c.getTime().compareTo(wetime) <= 0) {
			// 如果不是交易日，就往后推一天
			if (!IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				c.add(Calendar.DAY_OF_MONTH, 1);
			} else {
				return IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_K, c.getTime());
			}

		}
		return null;
	}

	// 计算本月收盘价
	private Double computeMonthS(IndexMessage req) {
		// 一月开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		// 一月结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		while (c.getTime().compareTo(wstime) >= 0) {

			if (!IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				c.add(Calendar.DAY_OF_MONTH, -1);
			} else {
				return IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_S, c.getTime());
			}

		}
		return null;
	}

	// 计算本月最高价
	private Double computeMonthZG(IndexMessage req) {
		// 一月开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		// 一月结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double max = null;
		while (c.getTime().compareTo(wstime) >= 0) {

			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_ZG, c.getTime());
				if (r != null) {
					if (max == null)
						max = r;
					else if (r > max)
						max = r;
				}
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return max;
	}

	// 计算本月最低价
	private Double computeMonthZD(IndexMessage req) {
		// 一月开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		// 一月结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double min = null;
		while (c.getTime().compareTo(wstime) >= 0) {

			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_ZD, c.getTime());
				if (r != null) {
					if (min == null)
						min = r;
					else if (r < min)
						min = r;
				}
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return min;
	}

	// 计算本月成交量
	private Double computeMonthCJL(IndexMessage req) {
		// 一月开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		// 一月结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double cjl = 0.0;
		while (c.getTime().compareTo(wstime) >= 0) {

			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_CJL, c.getTime());
				if (r != null)
					cjl += r;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return cjl;
	}

	// 计算本月成交量
	private Double computeMonthCJE(IndexMessage req) {
		// 一月开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		// 一月结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double cje = 0.0;
		while (c.getTime().compareTo(wstime) >= 0) {

			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_CJE, c.getTime());
				if (r != null)
					cje += r;
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return cje;
	}

	// 计算本月换手率
	private Double computeMonthHSL(IndexMessage req) {
		Double cjl = computeMonthCJL(req);
		Double zgb = computeMonthZGB(req);
		Double hsl = 0.0;
		if (zgb != null && cjl != null)
			hsl = cjl / zgb;
		return hsl;
	}

	private Double computeMonthZGB(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getMonthStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getMonthEndTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		while (c.getTime().compareTo(wstime) >= 0) {

			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				return IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_ZGB, c.getTime());
			}
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return null;
	}
}
