package com.yz.stock.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.yfzx.service.realtime.RealTradeService;
import com.yfzx.service.realtime.RealtimeDataItem;
import com.yz.mycore.lcs.enter.LCEnter;
/**
 * 分时数据处理器，只保存近5分钟的实时数据
 *      
 * @author：杨真 
 * @date：2014-7-24
 */
public class RealTimeMonitor {
	static 
	{
		init();
	}
	
	static Logger log = LoggerFactory.getLogger(RealTimeMonitor.class);
	static RealTimeMonitor instance = new RealTimeMonitor();
	public RealTimeMonitor() {

	}

	public static RealTimeMonitor getInstance() {
		return instance;
	}
	
	private static void init() {
//		Thread t = new Thread(new Runnable(){
//
//			public void run() {
//				while(true)
//				{
//					try {
//						clearExperiedData();
//					} catch (Exception e) {
//						log.error("clearExperiedData failed",e);
//					}
//					try {
//						Thread.sleep(1000);
//					} catch (Exception e) {
//						log.error("clearExperiedData failed",e);
//					}
//				}
//				
//			}
//
//			private void clearExperiedData() {
//				List<USubject> ul = USubjectService.getInstance().getUSubjectListByType(StockConstants.SUBJECT_TYPE_0);
//				if(ul!=null)
//				{
//					for(USubject us:ul)
//					{
//						RealtimeCacheWapper rw = getRealTimeDataItemFromCache(us.getUidentify());
//						if(rw!=null)
//						{
//							rw.clear();
//						}
//					}
//				}
//			}
//
//			private RealtimeCacheWapper getRealTimeDataItemFromCache(
//					String uidentify) {
//				return LCEnter.getInstance().get(uidentify, SCache.CACHE_NAME_realtimedatacache);
//			}
//			
//			
//		});
//		t.setName("clearExperiedDataThread");
//		t.start();
		
	}
	
	
	public void putRealtimeDataItem2Cache(RealtimeDataItem item)
	{
		RealtimeCacheWapper rw = LCEnter.getInstance().get(item.getUidentify(), SCache.CACHE_NAME_realtimedatacache);
		if(rw==null)
		{
			rw=new RealtimeCacheWapper(item);
			LCEnter.getInstance().put(item.getUidentify(),rw, SCache.CACHE_NAME_realtimedatacache);
		}
	}
	
	public void putItem2Wapper(RealtimeDataItem item)
	{
		RealtimeCacheWapper rw = LCEnter.getInstance().get(item.getUidentify(), SCache.CACHE_NAME_realtimedatacache);
		if(rw!=null)
			rw.check(item);
		else
		{
			putRealtimeDataItem2Cache(item);
		}
	}

	public void clearRealtimeWapper(String uidentify)
	{
		RealtimeCacheWapper rw = LCEnter.getInstance().get(uidentify, SCache.CACHE_NAME_realtimedatacache);
		if(rw!=null)
			rw.clear_now();
	}
	public static void open() {
		RealTradeService.getInstance().openRealMonitor();
		
	}

	
}
