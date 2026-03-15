package com.yz.stock.realtime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.msg.StockPriceBatchMsg;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.DcssTradeIndexServiceClient;
import com.yfzx.service.client.MessageSenderServiceClient;
import com.yz.common.vo.Pair;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.msg.event.IEvent;
import com.yz.mycore.msg.handler.HandleUtil;
import com.yz.mycore.msg.handler.IHandler;

/**
 * SEvent事件handle 为不阻塞上级队列，此处加了批量处理，按批次进行
 * 
 * @author：杨真
 * @date：2014-4-10
 */
public class RealtimePriceClientHandle implements IHandler {
	static List<Dbrouter> _udpRouterList;
	static Logger logger = LoggerFactory.getLogger(RealtimePriceClientHandle.class);
	private static AtomicInteger _ai = new AtomicInteger(0);// 处理批次
	private AtomicInteger _fcount = new AtomicInteger(0);// 处理批次
	private static int batchsize = 20;
	static long lastHandleTimes = System.currentTimeMillis();
	static long lastPrintTimes = System.currentTimeMillis();
	private static ConcurrentHashMap<Integer, Pair<Long,List<IEvent>>> _bcache = new ConcurrentHashMap<Integer, Pair<Long,List<IEvent>>>();
	static HandleUtil<IEvent> hu = new HandleUtil<IEvent>();
	static int combin = 0;
	static
	{
		initUdpPollRouter();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			public void refresh() {
				initUdpPollRouter();
				batchsize = ConfigCenterFactory.getInt("realtime_server.msg_batchsize", 50);
				combin = ConfigCenterFactory.getInt("realtime_server.msg_batchsize_combin", 0);
			}

		});
		
	
		
		Thread bt = new Thread(new Runnable() {

			public void run() {
				while (true) {
					hu.batchHandle(_bcache,_ai,lastHandleTimes,batchsize,new IHandler(){

						public void handle(Object h) {
							if(h==null)
								return ;
							List<IEvent> sl = (List<IEvent>) h;
							batchHandle(sl);
							
						}
						
					});
				}

			}

		});
		bt.setName("batchCombinPriceUpdataEventThread");
		bt.start();
	}
	
	private static void initUdpPollRouter() {
		String uips = ConfigCenterFactory.getString("yas.stock_price_poll_address" ,
				"udp://192.168.1.102:5555/dcss/IMessageSenderService");
		if (!StringUtil.isEmpty(uips)) {
			// _udpRouterList
			List<Dbrouter> nudpRouterList = new ArrayList<Dbrouter>();
			for (String ips : uips.split(";")) {
				if (!StringUtil.isEmpty(ips)) {
					try {
						Dbrouter ur = new Dbrouter();
						ur.setServiceName("IMessageSenderService");
						ur.setServiceAddress(ips);
						nudpRouterList.add(ur);
					} catch (Exception e) {
						logger.error("parse udp router failed!", e);
					}
				}

			}
			if (nudpRouterList.size() != 0)
			{
				_udpRouterList = nudpRouterList;
			}
		}
		
	}
	/**
	 * 为不阻塞上及队列，此处加了批量处理，按批次进行
	 */
	public void handle(Object e) {

		if (e == null)
			return;
		if(combin==1)
		{
			IEvent se = (IEvent) e;
			lastHandleTimes = System.currentTimeMillis();
			hu.combineEvent(_bcache,_ai,batchsize,se);
		}
		else
		{
			NotifyEvent se = (NotifyEvent) e;
//			sendByPolicy(e);
			//发送给注册了接收消息的端口
			List<IEvent> el = new ArrayList<IEvent>();
			el.add(se);
			sendBroadcast(el);
		}
		if(SLogFactory.isopen("price_update_flow_count_log"))
		{
			_fcount.incrementAndGet();
			if(System.currentTimeMillis()-lastPrintTimes>30000)
			{
				System.out.println("fount="+_fcount.get()+";time="+DateUtil.format2String(new Date(System.currentTimeMillis())));
				lastPrintTimes = System.currentTimeMillis();
			}
		}
		
		
	}
	
	protected static void batchHandle(List<IEvent> sl) {
		try {
			if (sl == null || sl.size() == 0)
				return;
//			sendByPolicy(sl);
			sendBroadcast(sl);
		} catch (Exception e) {
			logger.error("get db connection failed!", e);
		}

	}
	
	private static void sendBroadcast(List<IEvent> lmsg) {
		
		for(Dbrouter dr :_udpRouterList)
		{
			try {
				MessageSenderServiceClient.getInstance().sendAsynUdpMessage(dr, lmsg);
			} catch (Exception e) {
				logger.error("sendAsynUdpMessagePool failed!", e);
			}
		}
	}

//	private static void sendByPolicy(Object e) {
//		NotifyEvent ne = (NotifyEvent) e;
//		StockPriceBatchMsg msg = (StockPriceBatchMsg) ne.getMsg();
//		DcssTradeIndexServiceClient.getInstance().putRealTimeResult2Cache(
//				msg.getSeed(), msg.getBody(), false);
//		
//	}
}
