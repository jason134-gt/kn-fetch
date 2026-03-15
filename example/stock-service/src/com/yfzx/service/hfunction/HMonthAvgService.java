package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;

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
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

/**
 * 月移动平均线
 * 
 * @author：杨真
 * @date：2014-3-25
 */
public class HMonthAvgService {

	private static HMonthAvgService instance = new HMonthAvgService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private HMonthAvgService() {

	}

	public static HMonthAvgService getInstance() {
		return instance;
	}

	/**
	 * 
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	public Double computeMonthAvg(IndexMessage req, int type) {
		Date wetime = req.getTime();
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double avg = 0.0;
		IndexMessage treq = (IndexMessage) req.clone();
		int count=0;
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(treq.getUidentify(),StockConstants.INDEX_CODE_TRADE_S_M);
		if(mintime==null) return avg;
		Date maxtime = new Date();
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(StockConstants.INDEX_CODE_TRADE_S_M);
		Cfirule cr = CRuleService.getInstance().getCfruleByCodeFromCache(StockConstants.INDEX_CODE_TRADE_S_M);
		while ( count<type&&c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Date actime = IndexService.getInstance().formatTime(c.getTime(), d, treq.getUidentify());
			if(actime!=null)
			{
				treq.setTime(actime);
				Double r = null;
				treq.setNeedUseExtDataCache(true);
				//先取缓存
				if (treq.isNeedUseExtDataCache()) {
					r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_S_M, treq.getTime());
				}
				//取不到就计算
				if (treq.isNeedComput() &&( r == null || r == 0)) {
				    r = CRuleService.getInstance().computeIndex(treq,cr);
				}
				if (r != null)
				{
					avg += r;
					count++;
				}
			}
			c.add(Calendar.MONTH, - 1);

		}
		if(count!=type)
		{
//			System.out.println("computeMonthAvg:count!=type,indexname="+d.getShowName()+"count="+count+";type="+type+";paras="+treq);
		}
		if(count!=0)
		avg = avg / count;
		return avg;
	}
}
