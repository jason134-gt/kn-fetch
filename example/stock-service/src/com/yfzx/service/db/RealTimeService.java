package com.yfzx.service.db;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.stock.common.model.IndexMessage;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.DcssCompanyExtIndexServiceClient;
import com.yfzx.service.factory.SMsgFactory;


/**
 * 实时计算数据服务
 *      
 * @author：杨真 
 * @date：2014-4-16
 */
public class RealTimeService {

	static RealTimeService instance = new RealTimeService();
//	ConcurrentHashMap<String,Map<Object, Double>> _rtcache = new ConcurrentHashMap<String, Map<Object, Double>>();
	public RealTimeService()
	{
		
	}
	public static RealTimeService getInstance()
	{
		return instance;
	}
	public void put(String uidentify, Integer key, Double v) {
		put2LocalCache(uidentify,String.valueOf(key),v);
//		put2RemoteCache(uidentify,String.valueOf(key),v);
//		Map<Object,Double> m = _rtcache.get(uidentify);
//		if(m==null)
//		{
//			m = new HashMap<Object,Double>();
//			_rtcache.put(uidentify, m);
//		}
//		m.put(key, v);
	}
	
	private void put2RemoteCache(String uidentify, String indexcode, Double v) {
		Date time = DateUtil.getDayStartTime(new Date());
		String key = StockUtil.getExtCachekey(uidentify,
				indexcode, time);
		DcssCompanyExtIndexServiceClient.getInstance().put(key, v);
		
	}
	public void put2LocalCache(String uidentify, String indexcode, Double v) {
		Date time = DateUtil.getDayStartTime(new Date());
		String key = StockUtil.getExtCachekey(uidentify,
				indexcode, time);
		IndexService.getInstance().put2Cache(key,v);
		
	}
	public void put2LocalCache_mock(String uidentify, Date time,Integer indexcode, Double v) {
		String key = StockUtil.getExtCachekey(uidentify,
				String.valueOf(indexcode), time);
		IndexService.getInstance().put2Cache(key,v);
		
	}
	public void put2LocalCache(String uidentify, String indexcode, Double v,Date time) {
		String key = StockUtil.getExtCachekey(uidentify,
				indexcode, time);
		IndexService.getInstance().put2Cache(key,v);
		
	}
//	public Double get(String uidentify, Integer key) {
//		// TODO Auto-generated method stub
//		Map<Object,Double> m = _rtcache.get(uidentify);
//		if(m!=null)
//		{
//			 return m.get(key);
//		}
//		return null;
//	}
//	
//	public void putMap(String uidentify, Map<Object, Double> m) {
//		_rtcache.put(uidentify, m);
//	}
	public Double realTimeComputeIndex(String uidentify, String indexcode,Date time) {
		
		IndexMessage im = SMsgFactory.getUDCIndexMessage(uidentify);
		im.setTime(time);
		im.setIndexCode(indexcode);
		im.setNeedComput(true);
		im.setNeedUseExtDataCache(false);
		im.setNeedAccessCompanyBaseIndexDb(false);
		im.setNeedAccessExtIndexDb(false);
		return CRuleService.getInstance().computeIndex(im);
	}

}
