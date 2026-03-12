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
 * 周数据
 * 
 * @author：杨真
 * @date：2014-3-25
 */
public class HWeekService {

	private static HWeekService instance = new HWeekService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private HWeekService() {

	}

	public static HWeekService getInstance() {
		return instance;
	}

	/**
	 * 正常情况下，行情数据没有全部加载，所以以这种笨办法取
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	public Double getWeekIndex(IndexMessage req, String type) {
		// 一月开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		// 一月结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double v = 0.0;
		while (c.getTime().compareTo(wstime) >= 0) {

			v = getWeekIndexV2(req.getCompanyCode(),c.getTime(), type);
			if (v != null && v != 0)
				break;
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return v;
	}

	/**
	 * 取周指标，对传入时间格式化
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	private Double getWeekIndexV2(String companycode,Date time, String type) {
		Double wi = 0.0;
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZS_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_S_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_K_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_CJL_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_CJE_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZDF_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_SD_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_HSL_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZG_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZD_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_ZF)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_ZF_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_5AVG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_5AVG_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_10AVG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_10AVG_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_20AVG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_20AVG_W, time);
		}
		if (type.equals(ComputeUtil.TRADE_30AVG)) {
			wi = IndexValueAgent.getIndexValue(companycode,
					StockConstants.INDEX_CODE_TRADE_30AVG_W, time);
		}
		// if (type.equals(ComputeUtil.TRADE_60AVG)) {
		// wi = IndexValueAgent.getIndexValue(companycode,
		// StockConstants.INDEX_CODE_TRADE_60AVG_W, req.getTime());
		// }
		if (wi == null)
			wi = 0.0;
		return wi;
	}

	/**
	 * 
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	public Double computeWeekIndex(IndexMessage req, String type) {
		Double wi = 0.0;
		Date time = IndexService.getInstance().formatTimeByUnit(req.getTime(),
				Calendar.WEEK_OF_MONTH, req.getCompanyCode());
		if (time == null)
			return wi;
		req.setTime(time);
		if (type.equals(ComputeUtil.TRADE_ZS)) {
			wi = computeWeekZS(req);
		}
		if (type.equals(ComputeUtil.TRADE_S)) {
			wi = computeWeekS(req);
		}
		if (type.equals(ComputeUtil.TRADE_K)) {
			wi = computeWeekK(req);
		}
		if (type.equals(ComputeUtil.TRADE_CJL)) {
			wi = computeWeekCJL(req);
		}
		if (type.equals(ComputeUtil.TRADE_CJE)) {
			wi = computeWeekCJE(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZDF)) {
			wi = computeWeekZDF(req);
		}
		if (type.equals(ComputeUtil.TRADE_SD)) {
			wi = computeWeekSD(req);
		}
		if (type.equals(ComputeUtil.TRADE_HSL)) {
			wi = computeWeekHSL(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZG)) {
			wi = computeWeekZG(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZD)) {
			wi = computeWeekZD(req);
		}
		if (type.equals(ComputeUtil.TRADE_ZF)) {
			wi = computeWeekZF(req);
		}
		if (wi == null)
			wi = 0.0;
		return wi;
	}

	private Double computeWeekZF(IndexMessage req) {
		Double zf = null;
		Double zg = computeWeekZG(req);
		Double zd = computeWeekZD(req);
		Double zs = computeWeekZS(req);
		if (zs != null && zg != null && zd != null)
			zf = (zg - zd) / zs;
		return zf;
	}

	private Double computeWeekSD(IndexMessage req) {
		Double s = computeWeekS(req);
		Double k = computeWeekZS(req);
		if (k == null || s == null)
			return null;
		return s - k;
	}

	private Double computeWeekZDF(IndexMessage req) {
		Double s = computeWeekS(req);
		Double k = computeWeekZS(req);
		Double zdf = null;
		if (k != null && s != null)
			zdf = (s - k) / k * 100;
		return zdf;
	}

	// 计算上周收盘价
	private Double computeWeekZS(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wstime);
		// 一周结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
		while (c.getTime().compareTo(wetime) <= 0) {
			// 如果不是交易日，就往后推一天
			if (!IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode())) {
				c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + 1);
			} else {
				return IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_ZS, c.getTime());
			}

		}
		return null;
	}

	// 计算本周开盘价
	private Double computeWeekK(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(wstime);
		// 一周结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
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

	// 计算本周收盘价
	private Double computeWeekS(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
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

	// 计算本周最高价
	private Double computeWeekZG(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
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

	// 计算本周最低价
	private Double computeWeekZD(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
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

	// 计算本周成交量
	private Double computeWeekCJL(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
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

	// 计算本周成交量
	private Double computeWeekCJE(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
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

	// 计算本周换手率
	private Double computeWeekHSL(IndexMessage req) {
		Double cjl = computeWeekCJL(req);
		Double zgb = computeWeekZGB(req);
		Double hsl = null;
		if (zgb != null && cjl != null)
			hsl = cjl / zgb;
		return hsl;
	}

	private Double computeWeekZGB(IndexMessage req) {
		// 一周开始时间
		Date wstime = DateUtil.getWeekStartTime(req.getTime());
		// 一周结束时间
		Date wetime = DateUtil.getWeekEndTime(req.getTime());
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
