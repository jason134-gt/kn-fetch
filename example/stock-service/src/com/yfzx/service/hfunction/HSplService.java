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
 * 计算股价在一段时间内的位置
 * nm:月数,l:位置下限，h:位置上限，位置范围：0--1
 * $hspl(nm,l,h)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HSplService implements IFService{

	private static HSplService instance = new HSplService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HSplService() {

	}

	public static HSplService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>2)
		{
			
			Integer nd = Integer.valueOf(vls.get(0));
			Double l  = Double.valueOf(vls.get(1));
			Double h = Double.valueOf(vls.get(2));
			ret = computeSpLocation(nd,l,h,req);
		}
		return ret;
	}

	/**
	 * 计算股价在一段时间内的位置,为了规避除权除息带来的影响，暂时取后复权价来计算
	 * @param type
	 * @param req
	 * @return
	 */
	public Double computeSpLocation(int nd,Double l,Double h,IndexMessage req) {
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null)
			return 0.0;
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				StockConstants.INDEX_CODE_TRADE_HF_K_M);
		if (d == null  )
			return 0.0;
		Double ret = 0.0;
	    Double csp = IndexValueAgent.getIndexValue(req.getCompanyCode(),
				StockConstants.INDEX_CODE_TRADE_HFQ_S, c.getTime());
	    if(csp==null||csp==0)
	    	return ret;
		Date maxtime = new Date();
		Double zg=0.0;
		Double zd=0.0;
		int dc=0;
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0&&dc<nd) {
			Date actime = IndexService.getInstance().formatTime(c.getTime(), d, req.getUidentify());
			if(actime!=null)
			{
				Double czg = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_HF_ZG_M, actime);
				if(czg==null||czg==0)
				{
					c.add(d.getTunit(), -d.getInterval());
					continue;
				}
				if(czg>zg)
				{
					zg=czg;
				}
				Double czd = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_HF_ZD_M, actime);
				if(czd==null||czd==0)
				{
					c.add(d.getTunit(), -d.getInterval());
					continue;
				}
				if((zd==0||czd<zd))
				{
					zd=czd;
				}
				dc++;
			}
				
			c.add(d.getTunit(), -d.getInterval());

		}
		if(zg==null||zg==0||zd==null||zg==0)
			return ret;
		ret = (csp-zd)/(zg-zd);
		if(nd/dc<1.5&&ret>l&&ret<=h)
			return 1.0;
		return 0.0;
	}
}
