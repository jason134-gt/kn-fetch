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
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.USubjectService;

/**
 * 连续n天上涨,下跌,阴线，阳线
 * type:sz|xd|yangx|yinx
 * $hlxnd(timeUnit,type)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HLxndService implements IFService{

	private static HLxndService instance = new HLxndService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HLxndService() {

	}

	public static HLxndService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>0)
		{
			String timeUnit = vls.get(0);
			String type = vls.get(1);
			ret = computeHlxnd(timeUnit,type,req);
		}
		return ret;
	}

	/**
	 * 计算连续上涨,下跌,收阳线，收阴线的天数
	 * @param type
	 * @param type 
	 * @param req
	 * @return
	 */
	public Double computeHlxnd(String timeUnit, String type, IndexMessage req) {
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null)
			return 0.0;
		Dictionary d = DictService.getInstance()
				.getDataDictionaryFromCache(
						HUtilService.getInstance().getIndexcodeByType(
								timeUnit, ComputeUtil.TRADE_S));
		Date maxtime = new Date();
		Double count = 0.0;
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double s = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					HUtilService.getInstance().getIndexcodeByType(timeUnit,
							ComputeUtil.TRADE_S), c.getTime());
			if(type.equals("sz")||type.equals("xd"))
			{
				Double zs = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(timeUnit,
								ComputeUtil.TRADE_ZS), c.getTime());
				if(s!=null&&zs!=null&&s!=0&&zs!=0)
				{
					if(type.equals("sz"))
					{
						if(s>zs)
						{
							count++;
						}
						else
							break;
					}
					if(type.equals("xd"))
					{
						if(s<zs)
						{
							count++;
						}
						else
							break;
					}
				}
				
			}
			if(type.equals("yangx")||type.equals("yinx"))
			{
				Double k = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(timeUnit,
								ComputeUtil.TRADE_K), c.getTime());
				if(s!=null&&k!=null&&s!=0&&k!=0)
				{
					if(type.equals("yangx"))
					{
						if(s>k)
						{
							count++;
						}
						else
							break;
					}
					if(type.equals("yinx"))
					{
						if(s<k)
						{
							count++;
						}
						else
							break;
					}
				}
				
			}
			
			c.add(d.getTunit(), -d.getInterval());

		}
		return count;
	}
}
