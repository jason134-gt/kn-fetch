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
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 * 平均 $havg(nd)
 * 
 * @author：杨真
 * @date：2014-3-25
 */
public class HAvgService implements IFService {

	private static HAvgService instance = new HAvgService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HAvgService() {

	}

	public static HAvgService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 0) {
			Integer nd = Integer.valueOf(vls.get(0));
			String indexcode = vls.get(1);
			Date wetime = req.getTime();
			Calendar c = Calendar.getInstance();
			c.setTime(wetime);
			Double avg = 0.0;
			int dc = 0;
			Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
					req.getUidentify(), indexcode);
			if (mintime == null)
				return avg;
			Date maxtime = new Date();
			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(indexcode);
			while (dc < nd && c.getTime().compareTo(mintime) >= 0
					&& c.getTime().compareTo(maxtime) <= 0) {
				Date actime = IndexService.getInstance().formatTime(
						c.getTime(), d, req.getUidentify());
				if (actime != null) {
					Double r = IndexValueAgent.getIndexValueNeedCompute(
							req.getCompanyCode(), indexcode, actime);
					if (r != null) {
						avg += r;
						dc++;
					}
				}
				c.add(d.getTunit(), -d.getInterval());

			}
			if (dc != nd) {
				// System.out.println("computeMonthAvg:count!=type,indexname="+d.getShowName()+"count="+count+";type="+type+";paras="+treq);
			}
			if (dc != 0)
				avg = avg / dc;
			return avg;
		}
		return ret;
	}

}
