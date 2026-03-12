package com.yz.stock.monitor;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.event.NotifyEvent;
import com.yfzx.service.realtime.RealtimeDataItem;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.log.LogManager;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.disruptor.MyDisruptor;
import com.yz.mycore.msg.disruptor.MycoreConsumeQueueHandler;
import com.yz.mycore.msg.event.IEvent;
import com.yz.mycore.msg.handler.IHandler;
import com.yz.mycore.msg.message.IMessage;

/**
 * SEvent事件handle 为不阻塞上级队列，此处加了批量处理，按批次进行
 * 
 * @author：杨真
 * @date：2014-4-10
 */
public class RealMoniterHandle implements IHandler {

	static Logger logger = LoggerFactory.getLogger(RealMoniterHandle.class);
	static List<MyDisruptor> mdlist = new ArrayList<MyDisruptor>();
	static
	{
		int mdsize = ConfigCenterFactory.getInt("realtime_server.RealMoniterHandle_mdsize", 3);
		for(int i=0;i<mdsize;i++)
		{
			MyDisruptor md = new MyDisruptor(new MycoreConsumeQueueHandler(new IHandler(){

				@Override
				public void handle(Object ee) {
					try {
						if(ee!=null)
						{
							IEvent e = (IEvent) ee;
							if(e!=null)
							{
								try {
									NotifyEvent se = (NotifyEvent) e;
									IMessage im = se.getMsg();
									RealtimeDataItem rid = (RealtimeDataItem) im;
									RealTimeMonitor.getInstance().putItem2Wapper(rid);
								} catch (Exception e2) {
									logger.error("Thread sleep failed!",e2);
								}
							}
						}
						
						
					} catch (Exception e) {
						logger.error("handle event failed!");
					}
					
				}
				
				
				
			}, 1, 1024*8,0,"RealMoniterHandle_"+i));
			
			mdlist.add(md);
		}
		
	}
	/**
	 * 为不阻塞上及队列，此处加了批量处理，按批次进行
	 */
	public void handle(Object e) {

		if (e == null)
			return;
		NotifyEvent se = (NotifyEvent) e;
		IMessage im = se.getMsg();
		RealtimeDataItem rid = (RealtimeDataItem) im;
		if(rid.isindustry)
		{
//			RealTimeMonitor.getInstance().putItem2IndustryWapper(rid);
		}
		else
		{
			int closehk = ConfigCenterFactory.getInt("realtime_server.close_hk_monitor", 0);
			if(closehk==1&&rid.getUidentify().endsWith("hk"))
			{
				return;
			}
			//按公司编码分发到不同的队列,保证同一个公司只会下发到一个队列
			int index = Math.abs(rid.getUidentify().trim().hashCode()
					% mdlist.size());
			mdlist.get(index).disruptor(se);
		}
		
	}
}
