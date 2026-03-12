package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.DateUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

/**
 * 
 * 
 * @author：杨真
 * @date：2014-3-25
 */
public class HDaySumService {

	private static HDaySumService instance = new HDaySumService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private HDaySumService() {

	}

	public static HDaySumService getInstance() {
		return instance;
	}
	
	public Double computeDaySum(String companycode,String indexCode,Date lastDate,int perlimit) {
		IndexMessage req = SMsgFactory.getCompanyMsg(companycode, indexCode, DateUtil.getDayStartTime(lastDate));
		req.setNeedAccessExtRemoteCache(false);
		return computeDaySum(req, perlimit);
	}

	/**
	 * 
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	public Double computeDaySum(IndexMessage req, int type) {

		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double Sum = 0.0;
//		
		int count = 0;
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null) 
			{
				log.info("computeDaySum:mintime is null;"+req);
				return 0.0;
			}
		Date maxtime = new Date();
//		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(StockConstants.INDEX_CODE_TRADE_S_W);
		while ( count<type&&c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			
			if(IndexService.getInstance().isTradeDate(c.getTime(), req.getCompanyCode()))
			{
				Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						req.getIndexCode(), c.getTime());
				if (r != null) {
					Sum += r;
					count++;
				} 
			}
			
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return Sum;
	}

	public Double realtimeComputeSum(String companycode,int type) {
		IndexMessage req = SMsgFactory.getCompanyMsg(companycode,
				StockConstants.INDEX_CODE_TRADE_S, DateUtil.getDayStartTime(new Date()));
		req.setNeedAccessExtRemoteCache(false);
		return realtimeComputeSum(req, type);
	}

	
	public Double realtimeComputeSum(IndexMessage req, int type) {
		
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double Sum = 0.0;
//		
		int count = 0;
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null) 
		{
			return 0.0;
		}
		Date maxtime = new Date();
//		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(StockConstants.INDEX_CODE_TRADE_S_W);
		while ( count<type&&c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
					req.getIndexCode(), c.getTime());
			if (r != null) {
				Sum += r;
				count++;
			} 
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		return Sum;
	}
}
