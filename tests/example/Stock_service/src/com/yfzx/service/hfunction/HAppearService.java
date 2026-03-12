package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Cfirule;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;

/**
 * 按条件统计
 * timeUnit:时间单位，nd多少天,exp:表达式 $happear(timeUnit,nd,exp)
 * @author：杨真
 * @date：2014年10月20日
 */
public class HAppearService implements IFService {

	private static HAppearService instance = new HAppearService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HAppearService() {

	}

	public static HAppearService getInstance() {
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
		// 从前一天开始
		c.add(Calendar.DAY_OF_MONTH, -1);
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				HUtilService.getInstance().getIndexcodeByType(timeUnit,
						ComputeUtil.TRADE_HF_S));
		if(exp.indexOf("r_")>=0)
		{
			Cfirule cr = CRuleService.getInstance().getCfruleByCodeFromCache(exp.split("_")[1]);
			if(cr!=null)
				exp = cr.getRule();
		}
		if(exp.indexOf("t_")>=0)
		{
			Tagrule tr = TagruleService.getInstance().getTagruleByIdFromCache(exp.split("_")[1]);
			if(tr!=null)
				exp = tr.getRule();
		}
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
					return 1.0;
				}
				dc++;
			}
			c.add(d.getTunit(), -d.getInterval());
		}
		return count;
	}
}
