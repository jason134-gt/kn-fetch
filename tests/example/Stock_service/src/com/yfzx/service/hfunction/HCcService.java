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
 * 计算股价在一段时间内,a指标与b指标有几个交叉点
 * $hcc(nd,indexcodeA,indexcodeB)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HCcService implements IFService{

	private static HCcService instance = new HCcService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HCcService() {

	}

	public static HCcService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>2)
		{
			
			Integer nd = Integer.valueOf(vls.get(0));
			String indexcodeA = vls.get(1);
			String indexcodeB = vls.get(2);
			ret = hcc(nd,indexcodeA,indexcodeB,req);
		}
		return ret;
	}

	/**
	 * 计算股价在一段时间内,a指标与b指标有几个交叉点
	 * 
	 * @param type
	 * @param req
	 * @return
	 */
	public Double hcc(int nd, String indexcodeA,
			String indexcodeB, IndexMessage req) {
		Double count = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return count;
		Dictionary da = DictService.getInstance()
				.getDataDictionaryFromCache(
						indexcodeA);

		Dictionary db = DictService.getInstance()
				.getDataDictionaryFromCache(
						indexcodeB);
		if(da.getTunit()!=db.getTunit())
			return count;
		Date maxtime = new Date();
		
		int dc = 0;
		Double prea = null;
		Double preb = null;
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {
			Date actime = IndexService.getInstance().formatTime(c.getTime(), da, req.getCompanyCode());
			if(actime!=null)
			{
				Double a = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						indexcodeA, c.getTime());
				Double b = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						indexcodeB, c.getTime());
				if (a != null && a != 0 && b != null && b != 0) {
					if (prea == null)
						prea = a;
					if (preb == null)
						preb = b;
					if (prea <= preb && a >= b || prea >= preb && a <= b)
						count++;
					prea = a;
					preb = b;

					dc++;
				}
				
			}
			
			c.add(da.getTunit(), -da.getInterval());

		}

		return count;
	}
}
