package com.yfzx.service.hfunction;

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
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.TagruleService;

/**
 * 取指标的值，根据参数 $hdate(timeUnit,timeAdd,rule) timeUnit:"d","w","m"
 * timeAdd:时间增量
 * rule:规则表达式，或是r_1863,t_5458,r_：打头的为指标编码，t_打头的是规则编码
 * @author：杨真
 * @date：2014年10月20日
 */
public class HDateService implements IFService {

	private static HDateService instance = new HDateService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HDateService() {

	}

	public static HDateService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 2) {

			String timeUnit = vls.get(0);
			Integer timeAdd = Integer.valueOf(vls.get(1));
			String rule = vls.get(2);

			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_S));
			Date actime = null;
			if (timeAdd != 0) {
				actime = IndexService.getInstance().getNextTradeUtilEnd(
						req.getTime(), d, req.getCompanyCode(), timeAdd);
			} else {
				actime = IndexService.getInstance().formatTime(req.getTime(),
						d, req.getCompanyCode());
			}
			if(actime!=null)
			{
				IndexMessage nreq = (IndexMessage) req.clone();
				nreq.setTime(actime);
				if(rule.indexOf("r_")>=0)
				{
					Cfirule cr = CRuleService.getInstance().getCfruleByCodeFromCache(rule.split("_")[1]);
					if(cr!=null)
						rule = cr.getRule();
				}
				if(rule.indexOf("t_")>=0)
				{
					Tagrule tr = TagruleService.getInstance().getTagruleByIdFromCache(rule.split("_")[1]);
					if(tr!=null)
						rule = tr.getRule();
				}
				ret = CRuleService.getInstance().computeIndex(rule, nreq, StockConstants.TRADE_TYPE);
			}
		}
		return ret;
	}

	

}
