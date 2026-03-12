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
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 * 按条件统计
 * timeUnit:时间单位，nd多少天,exp:表达式 $hexpcount(timeUnit,nd,exp)
 * @author：杨真
 * @date：2014年10月20日
 */
public class HExpCountService implements IFService {

	private static HExpCountService instance = new HExpCountService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HExpCountService() {

	}

	public static HExpCountService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 2) {
			String timeUnit = vls.get(0);
			Integer nd = Integer.valueOf(vls.get(1));
			String exp = vls.get(2);
			ret = count(req, timeUnit, nd, exp);
		}
		return ret;
	}

	public Double count(IndexMessage req, String timeUnit, Integer nd,
			String exp) {
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
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {
			Date actime = IndexService.getInstance().formatTime(
					c.getTime(), d, req.getUidentify());
			if (actime != null) {
				req.setTime(actime);
				Double r = CRuleService.getInstance().computeIndex(exp, req, StockConstants.TRADE_TYPE);
				if(r!=null&&r>0)
				{
					count++;
				}
				if (IndexService.getInstance().isTradeDate(c.getTime(),
						req.getCompanyCode()))
					dc++;
			}
			
			c.add(d.getTunit(), -d.getInterval());
		}
		return count;
	}
}
