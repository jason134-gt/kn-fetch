package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;

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
import com.yfzx.service.db.RealTimeService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

/**
 * 日移动平均线
 * 
 * @author：杨真
 * @date：2014-3-25
 */
public class HDayAvgService {

	private static HDayAvgService instance = new HDayAvgService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private HDayAvgService() {

	}

	public static HDayAvgService getInstance() {
		return instance;
	}

	/**
	 * 
	 * 
	 * @param req
	 * @param type
	 * @return
	 */
	public Double computeDayAvg(IndexMessage req, int type) {

		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double avg = 0.0;
//		
		int count = 0;
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		if(mintime==null) 
			{
				log.info("computeDayAvg:mintime is null;"+req);
				return 0.0;
			}
		Date maxtime = new Date();
//		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(StockConstants.INDEX_CODE_TRADE_S_W);
		while ( count<type&&c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			
			if(IndexService.getInstance().isTradeDate(c.getTime(), req.getCompanyCode()))
			{
				Double r = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_S, c.getTime());
				if (r != null) {
					avg += r;
					count++;
				} 
			}
			
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		if (count != type)
		{
//			System.out.println("computeMonthAvg:count!=type,,indexname="+d.getShowName()+"count="+count+";type="+type+";paras="+req);
		}
		if(count!=0)
		avg = avg / count;
		return avg;
	}

	public Double realtimeComputeAvg(String companycode,int type) {
		IndexMessage req = SMsgFactory.getCompanyMsg(companycode,
				StockConstants.INDEX_CODE_TRADE_S, DateUtil.getDayStartTime(new Date()));
		req.setNeedAccessExtRemoteCache(false);
		return realtimeComputeAvg(req, type);
	}
	/**
	 * 此处做了次简化
	 * avg = preavg*4/5+c*1/5;
	 * @param req
	 * @param type
	 * @return
	 */
	public Double realtimeComputeAvgV2(IndexMessage req, String cindexcode,String type,int avgcount) {
		Double avg = 0.0;
		Dictionary d = DictService.getInstance()
				.getDataDictionaryFromCache(
						cindexcode);
		Date actime = IndexService.getInstance().formatTime(
				req.getTime(), d, req.getCompanyCode());
		if(actime!=null)
		{
			Date pretime = IndexService.getInstance().getNextTradeUtilEnd(actime,
					d, req.getCompanyCode(), -1);
		    if(pretime!=null)
		    {
		    	Double cp = IndexValueAgent.getIndexValue(
						req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_S, actime);
		    	Double preavg = IndexValueAgent.getIndexValueNeedCompute(
						req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(d.getTimeUnit(),
								type), pretime);
		    	if(preavg!=null&&preavg!=0&&cp!=null&&cp!=0)
		    	{
		    		avg = preavg*(avgcount-1)/avgcount+cp*1/avgcount;
		    	}
		    }
		}
		return avg;
		
	}
	/**
	 * 有缩放的成交额
	 * @param req
	 * @param cindexcode
	 * @param type
	 * @param avgcount
	 * @return
	 */
	public Double realtimeComputeAvgV3(IndexMessage req, String cindexcode,String type,int avgcount) {
		Double avg = 0.0;
		Dictionary d = DictService.getInstance()
				.getDataDictionaryFromCache(
						cindexcode);
		Date actime = IndexService.getInstance().formatTime(
				req.getTime(), d, req.getCompanyCode());
		if(actime!=null)
		{
			Date pretime = IndexService.getInstance().getNextTradeUtilEnd(actime,
					d, req.getCompanyCode(), -1);
		    if(pretime!=null)
		    {
		    	Double cp = IndexValueAgent.getIndexValue(
						req.getCompanyCode(),
						cindexcode, actime);
		    	
		    	Double preavg = IndexValueAgent.getIndexValueNeedCompute(
						req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(d.getTimeUnit(),
								type), pretime);
		    	if(preavg!=null&&preavg!=0&&cp!=null&&cp!=0)
		    	{
		    		cp=cp*HGetsfbsService.getInstance().getSFBS(d.getTimeUnit(), req);
		    		avg = preavg*(avgcount-1)/avgcount+cp*1/avgcount;
		    	}
		    }
		}
		return avg;
		
	}
	public Double realtimeComputeAvg(IndexMessage req, int type) {
		
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double avg = 0.0;
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
					StockConstants.INDEX_CODE_TRADE_S, c.getTime());
			if (r != null) {
				avg += r;
				count++;
			} 
			c.add(Calendar.DAY_OF_MONTH, -1);

		}
		if (count != type)
		{
//			System.out.println("computeMonthAvg:count!=type,,indexname="+d.getShowName()+"count="+count+";type="+type+";paras="+req);
		}
		if(count!=0)
			avg = avg / count;
		return avg;
	}
}
