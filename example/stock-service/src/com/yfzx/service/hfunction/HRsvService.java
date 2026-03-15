package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.ComputeUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 * timeUnit:时间单位，nd多少天
 * $hrsv(timeUnit,nd)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HRsvService implements IFService{

	private static HRsvService instance = new HRsvService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HRsvService() {

	}

	public static HRsvService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>1)
		{
			String timeUnit = vls.get(0);
			Integer nd = Integer.valueOf(vls.get(1));
			ret = rsv(req,timeUnit,nd);
		}
		return ret;
	}

	public Double rsv(IndexMessage req, String timeUnit, Integer nd)
	{
		Double ret = 0.0;
		try {
			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);
			Calendar c = Calendar.getInstance();
			c.setTime(wetime);
			Date mintime = USubjectService.getInstance()
					.getTradeIndexMinTime(
							req.getUidentify(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_S));
			if (mintime == null)
				return 0.0;
			
			Double csp = IndexValueAgent.getIndexValue(
					req.getCompanyCode(),
					HUtilService.getInstance().getIndexcodeByType(timeUnit,
							ComputeUtil.TRADE_HF_S), c.getTime());
			if (csp == null || csp == 0)
				return ret;
			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_S));
			Date maxtime = new Date();
			Double zg = 0.0;
			Double zd = 0.0;
			int dc = 0;
			while (c.getTime().compareTo(mintime) >= 0
					&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {
				
				Date actime = IndexService.getInstance().formatTime(c.getTime(), d, req.getCompanyCode());
				if(actime!=null)
				{
					Double czg = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_ZG), actime);
					Double czd = IndexValueAgent.getIndexValue(
							req.getCompanyCode(),
							HUtilService.getInstance().getIndexcodeByType(
									timeUnit, ComputeUtil.TRADE_HF_ZD), actime);
					if (czg != null && czg != 0 && czd != null && czd != 0) {
						if (czg > zg) {
							zg = czg;
						}
						
						if ((zd == 0 || czd < zd)) {
							zd = czd;
						}
					}
					dc++;
				}
				
				
				c.add(d.getTunit(), -d.getInterval());

			}
			
			if(zg==null||zg==0||zd==null||zg==0)
				return ret;
			if(zg-zd!=0)
				ret = (csp-zd)/(zg-zd)*100;
			
		} catch (Exception e) {
			log.error("HRsvService compute failed!",e);
		}
		return ret;
	}
}
