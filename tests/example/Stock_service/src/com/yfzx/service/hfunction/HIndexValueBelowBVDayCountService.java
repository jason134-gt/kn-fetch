package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.DateUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 * timeUnit:时间单位，nd多少天  $hivbdc(indexcode,l,h)
 * l:下限，h:上限
 * 某指标，在某个取值区间内运行的天数
 * @author：杨真
 * @date：2014年10月20日
 */
public class HIndexValueBelowBVDayCountService implements IFService {

	private static HIndexValueBelowBVDayCountService instance = new HIndexValueBelowBVDayCountService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	public static HIndexValueBelowBVDayCountService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double count = 0.0;
		if (vls != null && vls.size() > 2) {
			String indexcode = vls.get(0);
			Double l = Double.valueOf(vls.get(1));
			Double h = Double.valueOf(vls.get(2));
			
			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(
							indexcode);
			if(d==null)
				return count;
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
				Date actime = IndexService.getInstance().formatTime(c.getTime(), d, req.getCompanyCode());
				if(actime!=null)
				{
					Double a = IndexValueAgent.getIndexValue(req.getCompanyCode(),
							indexcode, c.getTime());

					if (a != null && a != 0 ) {
						if(a>=l&&a<=h)
							count++;
					}
					else
						break;
				}
				
				c.add(d.getTunit(), -d.getInterval());

			}

		}
		return count;
	}


	
}
