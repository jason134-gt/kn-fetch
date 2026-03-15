package com.yz.stock.sevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.model.snn.EConst;
import com.yfzx.service.msg.handler.c.USubjectNotifyClientHandler;
import com.yfzx.yas.YasManager;
import com.yz.mycore.core.inter.IStopable;
import com.yz.mycore.core.plug.IPlugIn;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.stock.monitor.RealMoniterHandle;
import com.yz.stock.monitor.RealTimeMonitor;
import com.yz.stock.realtime.RealtimeComputeDataClientHandle;
import com.yz.stock.realtime.RealtimePriceClientHandle;
import com.yz.stock.sevent.handler.SEventHandle;
import com.yz.stock.sevent.handler.SaveStatisticsHandle;
import com.yz.stock.sevent.handler.TradeAlarmMsgClientHandle;

/**
 * sevent 启动类
 * 
 * @author：杨真
 * @date：2014-7-12
 */
public class EventPlug implements IPlugIn,IStopable {

	Logger log = LoggerFactory.getLogger(EventPlug.class);
	
	public void plugIn() {
		ClientEventCenter.registerHandle("SEventHandleThread",
				Integer.valueOf(EConst.EVENT_0), new SEventHandle(), 2);
		ClientEventCenter.registerHandle("TradeAlarmNotifyHandleThread",
				Integer.valueOf(EConst.EVENT_1),
				new TradeAlarmMsgClientHandle(), 2);
		ClientEventCenter.registerHandle("SaveStatisticsHandleThread",
				Integer.valueOf(EConst.EVENT_2), new SaveStatisticsHandle(),
				2);
		RealTimeMonitor.open();
		ClientEventCenter.registerHandle("RealMoniterHandleThread",
				Integer.valueOf(EConst.EVENT_5), new RealMoniterHandle(),
				32,0);
		ClientEventCenter.registerHandle("RealtimePriceClientHandle",
				Integer.valueOf(EConst.EVENT_6), new RealtimePriceClientHandle(),
				32,0);
		
		ClientEventCenter.registerHandle("RealtimeComputeDataClientHandle",
				Integer.valueOf(EConst.EVENT_10), new RealtimeComputeDataClientHandle(),
				2,0);
		
		//在stockplug里已测试过
		ClientEventCenter.registerHandle("USubjectNotifyClientHandlerThread",Integer.valueOf(EConst.EVENT_4),new USubjectNotifyClientHandler(),2);
		
		YasManager.startUdpClient();

	}

	@Override
	public void stop() {
		log.info("执行保存"+SCache.CACHE_NAME_wstockcache);
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		cimpl.flushToDisk(SCache.CACHE_NAME_wstockcache);
	}
}
