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
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 *  计算某一段时间内的最高，最低值 为了规避除权除息带来的影响，暂时取后复权价来计算
 * type:zg|zd|zz:中值,nd:天数
 * $hfqlhz(type,nd)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HFqlhzService implements IFService{

	private static HFqlhzService instance = new HFqlhzService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HFqlhzService() {

	}

	public static HFqlhzService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>1)
		{
			
			String type = vls.get(0);
			Integer nd = Integer.valueOf(vls.get(1));
			ret = hfqlhz(type,nd,req);
		}
		return ret;
	}

	/**
	 * 计算某一段时间内的最高，最低值,以及中值 为了规避除权除息带来的影响，暂时取后复权价来计算
	 * @param type
	 * @param req
	 * @return
	 */
	public Double hfqlhz(String type, int nd,IndexMessage req) {
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null)
			return 0.0;
		Date maxtime = new Date();
		//从前一天开始
		c.add(Calendar.DAY_OF_MONTH, -1);
		Double ret = 0.0;
		Double zg=0.0;
		Double zd=0.0;
		Double zgs=0.0;//最高收盘
		Double zds=0.0;//最低收盘
		int dc=0;
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0&&dc<nd) {
				Double czg = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_HFQ_ZG, c.getTime());
				if(czg==null||czg==0)
				{
					c.add(Calendar.DAY_OF_MONTH, -1);
					continue;
				}
				if(czg>zg)
				{
					zg=czg;
				}
				Double czd = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_HFQ_ZD, c.getTime());
				if(czd==null||czd==0)
				{
					c.add(Calendar.DAY_OF_MONTH, -1);
					continue;
				}
				if(zd==0||czd<zd)
				{
					zd=czd;
				}
				Double hs = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_HFQ_S, c.getTime());
				if(hs==null||hs==0)
				{
					c.add(Calendar.DAY_OF_MONTH, -1);
					continue;
				}
				if(hs>zgs)
				{
					zgs=hs;
				}
				if(zds==0||hs<zds)
				{
					zds=hs;
				}
			if (IndexService.getInstance().isTradeDate(c.getTime(),
					req.getCompanyCode()))
				dc++;
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		if(type.equals("zg"))
		{
			ret = zg;
		}
		if(type.equals("zd"))
		{
			ret = zd;
		}
		if(type.equals("zds"))
		{
			ret = zds;
		}
		if(type.equals("zgs"))
		{
			ret = zgs;
		}
		if(type.equals("zz"))
		{
			ret = (zg+zd)/2;
		}
		return ret;
	}
}
