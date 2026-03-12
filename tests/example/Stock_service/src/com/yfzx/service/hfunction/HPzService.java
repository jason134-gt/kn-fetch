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
 * 盘整
 * $hpz(l,h)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HPzService implements IFService{

	private static HPzService instance = new HPzService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HPzService() {

	}

	public static HPzService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>=2)
		{
			
			Double l = Double.valueOf(vls.get(0));
			Double h = Double.valueOf(vls.get(1));
			
			ret = computeHpz(l,h,req);
		}
		return ret;
	}

	/**
	 * 计算连续振幅在某一个范围内的天数,振幅<hzf且 最高振幅>=lzf&& 最高振幅<=hzf,取向后复权价
	 * @param type
	 * @param req
	 * @return
	 */
	public Double computeHpz(Double lzf,Double hzf, IndexMessage req) {
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null)
			return 0.0;
		Date maxtime = new Date();
		Double count = 0.0;
		Double zg=0.0;
		Double zd=0.0;
		Double zgzf = 0.0;
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double s = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					StockConstants.INDEX_CODE_TRADE_HFQ_S, c.getTime());
			if(s==null||s==0)
			{
				c.add(Calendar.DAY_OF_MONTH, -1);
				continue;
			}
			if(s>zg)
			{
				zg=s;
			}
			if(zd==0||s<zd)
			{
				zd=s;
			}
			Double zf = (zg-zd)/zd;
			if(Math.abs(zf)<=hzf)
			{
				count++;
			}
			else
				break;
			if(Math.abs(zf)>zgzf)
				zgzf=Math.abs(zf);
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		if(zgzf<=hzf&&zgzf>=lzf)
			return count;
		else
			return 0.0;
	}
}
