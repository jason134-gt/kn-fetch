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
import com.yz.configcenter.ConfigCenterFactory;

/**
 * 计算股价在一段时间内,a指标在b指标下运行的天数
 * type:1:a在b上方，0:a在b下方
 * $habbc(type,indexcodeA,indexcodeB,timeAdd)
 * timeAdd:时间增量    
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HAbbcService implements IFService{

	private static HAbbcService instance = new HAbbcService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HAbbcService() {

	}

	public static HAbbcService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>2)
		{
			
			Integer type = Integer.valueOf(vls.get(0));
			String indexcodeA = vls.get(1);
			String indexcodeB = vls.get(2);
			Integer timeAdd = 0;
			if(vls.size()>3)
			{
				timeAdd = Integer.valueOf(vls.get(3));
			}
			ret = abbc(type,indexcodeA,indexcodeB,timeAdd,req);
		}
		return ret;
	}

	/**
	 * 计算股价在一段时间内,a指标在b指标下运行的天数
	 * 
	 * @param type
	 * @param timeAdd 
	 * @param req
	 * @return
	 */
	public Double abbc(int type, String indexcodeA,
			String indexcodeB, Integer timeAdd, IndexMessage req) {
		Double count = 0.0;
		Date wetime = req.getTime();
		
		wetime = DateUtil.getDayStartTime(wetime);
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
		Date atime=null;
		if (timeAdd != 0) {
			atime = IndexService.getInstance().getNextTradeUtilEnd(
					req.getTime(), da, req.getCompanyCode(), timeAdd);
		} else {
			atime = IndexService.getInstance().formatTime(req.getTime(),
					da, req.getCompanyCode());
		}
		if(atime!=null)
		{
			wetime = atime;
		}
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date maxtime = new Date();
		//允许的最大误差天数
		int wcl = ConfigCenterFactory.getInt("compute.habbc_wu_cha_num_limit", 3);
		int wc = 0;
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Date actime = IndexService.getInstance().formatTime(c.getTime(), da, req.getCompanyCode());
			if(actime!=null)
			{
				Double a = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						indexcodeA, c.getTime());
				Double b = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						indexcodeB, c.getTime());
				if (a != null && a != 0 && b != null && b != 0) {
					// 上方运行
					if (type == 1) {
						if (a >= b) {
							count++;
						} else
						{
							if(wc<wcl)
								wc++;
							else
								break;
						}
							
					} else {
						if (a <= b) {
							count++;
						} else
						{
							if(wc<wcl)
								wc++;
							else
								break;
						}
					}
				}
				
			}
			

			c.add(da.getTunit(), -da.getInterval());

		}

		return count;
	}
}
