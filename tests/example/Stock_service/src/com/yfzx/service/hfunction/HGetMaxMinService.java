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
 *  取某个指标在一定天数内的最大最小值
 * type:max-最大值，min-最小值，indexcode:指标编码,nd:(日|周|月)数
 * $hgmm(type,nd,indexcode)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HGetMaxMinService implements IFService{

	private static HGetMaxMinService instance = new HGetMaxMinService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HGetMaxMinService() {

	}

	public static HGetMaxMinService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>1)
		{
			
			String type = vls.get(0);
			Integer nd = Integer.valueOf(vls.get(1));
			String indexcode = vls.get(2);
			ret = hgetmaxmin(type,indexcode,nd,req);
		}
		return ret;
	}

	
	public Double hgetmaxmin(String type,String indexcode, int nd,IndexMessage req) {
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null)
			return 0.0;
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				indexcode);
		
		if (d == null  )
			return 0.0;
		Date maxtime = new Date();
		//从前一天开始
		c.add(d.getTunit(), -d.getInterval());
		Double ret = 0.0;
		Double max=0.0;
		Double min=0.0;
		int dc=0;
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0&&dc<nd) {
			Date actime = IndexService.getInstance().formatTime(c.getTime(), d, req.getUidentify());
			if(actime!=null)
			{
				Double v = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						indexcode, actime);
				if(v!=null&&v!=0)
				{
					if(v>max)
					{
						max=v;
					}
					if(min==0||v<min)
					{
						min=v;
					}
					dc++;
				}
			}
				
//			c.add(Calendar.DAY_OF_MONTH, -1);
			c.add(d.getTunit(), -d.getInterval());

		}
		if(type.equals("max"))
		{
			ret = max;
		}
		if(type.equals("min"))
		{
			ret = min;
		}
		return ret;
	}
}
