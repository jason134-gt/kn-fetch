package com.yz.stock.sevent.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.msg.BaseMsg;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.MessageSenderServiceClient;
import com.yfzx.yas.router.ISelectRouter;
import com.yz.common.vo.Pair;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.msg.event.IEvent;
import com.yz.mycore.msg.handler.HandleUtil;
import com.yz.mycore.msg.handler.IHandler;

/**
 * 市场异动消息处理器
 * 
 * @author：杨真
 * @date：2014-4-10
 */
public class TradeAlarmMsgClientHandle implements IHandler {
	static Logger logger = LoggerFactory.getLogger(TradeAlarmMsgClientHandle.class);
	private static AtomicInteger _ai = new AtomicInteger(0);// 处理批次
	private static int batchsize = 20;
	static long lastHandleTimes = System.currentTimeMillis();
	static List<Dbrouter> _udpRouterList;
	private static ConcurrentHashMap<Integer, Pair<Long,List<IEvent>>> _bcache = new ConcurrentHashMap<Integer, Pair<Long,List<IEvent>>>();
	static HandleUtil<IEvent> hu = new HandleUtil<IEvent>();
	static {
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
		bt.setName("TradeAlarmMsgbatchThread");
		bt.start();
		batchsize = ConfigCenterFactory.getInt("yas.msg_batchsize", 100);
		initUdpPollRouter();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			public void refresh() {
				initUdpPollRouter();
				batchsize = ConfigCenterFactory.getInt("yas.msg_batchsize", 100);
			}

		});
	}
	
	public void handle(Object h) {

		if (h == null)
			return;
		NotifyEvent e = (NotifyEvent) h;
		sendByPolicy(e);
//		lastHandleTimes = System.currentTimeMillis();
//		hu.combineEvent(_bcache,_ai,batchsize,e);
	}
	
	private static void sendByPolicy(IEvent ie) {		
			NotifyEvent ue = (NotifyEvent)ie;
			String seed = ((TradeAlarmMsg) ue.getMsg()).getAttr("k").toString();
			BaseMsg um = (BaseMsg)ue.getMsg();
			if(um.getSendType()==MsgConst.SEND_TYPE_1)
			{
				
				MessageSenderServiceClient.getInstance().sendAsynUdpMessagePool(ue);
			}
			else
			{
				
				MessageSenderServiceClient.getInstance().sendAsynUdpMessage(seed,ue,new ISelectRouter() {

					@Override
					public String getName() {
						// TODO Auto-generated method stub
						return "SR_TradeAlarmMsgClientHandle";
					}
					// 根据缓存名来路由，不同的缓存可能放在不同的服务器上
					public Dbrouter selectRouter(String methodName,
							Object[] args) {
						Dbrouter dr = null;
						try {
							if (MessageSenderServiceClient._routerUdpPool.keySet().size() == 0)
								return null;
							String k = args[0].toString();
							if (StringUtil.isEmpty(k))
								return null;
							String[] ka = k.toString().split("\\^");
							// 路由key,以公司编码路由，这样同一公司的数据只会存储在一个缓存服务器上
							String rk = ka[0];
							String sips = ConfigCenterFactory.getString(
									"dcss.ext_cache_server_ip_list_udp",
									"192.168.1.102:6666");

							String[] ips = sips.split("\\^");
							int dcssNums = ips.length;
							// 根据用户uid或者username求余 得到
							// 不建议客户端感知后端使用每台DCSS有多少个cache
							int index = StockUtil.getUserIndex(rk) % dcssNums; // StockUtil.getUExtTableIndex(rk)%ips.length;
							String selectIp = ips[index];
							String ip = selectIp.split(":")[0];
							int port = Integer.valueOf(selectIp.split(":")[1]);
							dr = MessageSenderServiceClient._routerUdpPool.get(getKey(ip, port));
							// 如果各中不同类型的数据存在不同的缓存服务器上，则根据服务名取相应缓存的服务地址，此处根据不同的情
							// 况，自己实现
						} catch (Exception e) {
							logger.error("select router failed!", e);
						}
						return dr;
					}

				});
			}

	}

	private static void initUdpPollRouter() {
		String uips = ConfigCenterFactory.getString("yas.trade_alarm_type_1_poll_address" ,
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
	 * 批量处理，把消息分发到订阅了的机器上去
	 * @param sl
	 */
	protected static void batchHandle(List<IEvent> sl) {
		
		if(sl==null||sl.size()==0) return;
		sendByPolicy(sl);
		//发送给注册了接收消息的端口
		sendBroadcast(sl);
		
		
	}
	
	private static void sendBroadcast(List<IEvent> sl) {
		
		for(Dbrouter dr :_udpRouterList)
		{
			try {
				MessageSenderServiceClient.getInstance().sendAsynUdpMessage(dr, sl);
			} catch (Exception e) {
				logger.error("sendAsynUdpMessagePool failed!", e);
			}
		}
	}

	private static void sendByPolicy(List<IEvent> sl) {
		Map<Integer,List<IEvent>> mlu = groupByUid(sl); 
		Iterator<Integer> iter = mlu.keySet().iterator();
		while(iter.hasNext())
		{
			Integer index = iter.next();
			List<IEvent> ul = mlu.get(index);
			if(ul==null||ul.size()==0)
				continue;
			NotifyEvent ue = (NotifyEvent)ul.get(0);
			String seed = ((TradeAlarmMsg) ue.getMsg()).getSourceid().toString();
			if(index==MsgConst.BRODCAST_GROUP_ID)
			{
				//广播
//				MessageSenderServiceClient.getInstance().sendAsynMessagePool(ul);
				MessageSenderServiceClient.getInstance().sendAsynUdpMessagePool(ul);
			}
			else
			{
				//单播
//				MessageSenderServiceClient.getInstance().sendMessage(seed,ul);
				MessageSenderServiceClient.getInstance().sendAsynUdpMessage(seed,ul,new ISelectRouter() {

					@Override
					public String getName() {
						// TODO Auto-generated method stub
						return "SR_TradeAlarmMsgClientHandle";
					}
					// 根据缓存名来路由，不同的缓存可能放在不同的服务器上
					public Dbrouter selectRouter(String methodName,
							Object[] args) {
						Dbrouter dr = null;
						try {
							if (MessageSenderServiceClient._routerUdpPool.keySet().size() == 0)
								return null;
							String k = args[0].toString();
							if (StringUtil.isEmpty(k))
								return null;
							String[] ka = k.toString().split("\\^");
							// 路由key,以公司编码路由，这样同一公司的数据只会存储在一个缓存服务器上
							String rk = ka[0];
							String sips = ConfigCenterFactory.getString(
									"dcss.ext_cache_server_ip_list_udp",
									"192.168.1.102:6666");

							String[] ips = sips.split("\\^");
							int dcssNums = ips.length;
							// 根据用户uid或者username求余 得到
							// 不建议客户端感知后端使用每台DCSS有多少个cache
							int index = StockUtil.getUserIndex(rk) % dcssNums; // StockUtil.getUExtTableIndex(rk)%ips.length;
							String selectIp = ips[index];
							String ip = selectIp.split(":")[0];
							int port = Integer.valueOf(selectIp.split(":")[1]);
							dr = MessageSenderServiceClient._routerUdpPool.get(getKey(ip, port));
							// 如果各中不同类型的数据存在不同的缓存服务器上，则根据服务名取相应缓存的服务地址，此处根据不同的情
							// 况，自己实现
						} catch (Exception e) {
							logger.error("select router failed!", e);
						}
						return dr;
					}

				});
//				MessageSenderServiceClient.getInstance().sendAsynUdpMessage(seed,ul,new IYasHandler(){
//
//					@Override
//					public void handle(Object o) {
////						System.out.println("test call back handler!"+o);
//						
//					}
//					
//				});
//				MessageSenderServiceClient.getInstance().sendUdpMessage(seed,ul);
			}
		}
		
	}

	private static Map<Integer, List<IEvent>> groupByUid(List<IEvent> sl) {
		Map<Integer, List<IEvent>> mlu = new HashMap<Integer,List<IEvent>>();
		for(IEvent e :sl)
		{
			NotifyEvent ue =(NotifyEvent) e;
			String sips = ConfigCenterFactory.getString(
					"dcss.ext_cache_server_ip_list",
					"192.168.1.102:7777");
			
			String[] ips = sips.split("\\^");
			TradeAlarmMsg um = (TradeAlarmMsg)ue.getMsg();
			int index = SExt.getUExtTableIndex(um.getSourceid(),SExt.EXT_TABLE_TYPE_2)%ips.length;
			if(um.getSendType()==MsgConst.SEND_TYPE_1)
			{
				index = MsgConst.BRODCAST_GROUP_ID;
			}
			List<IEvent> ul = mlu.get(index);
			if(ul==null)
			{
				ul = new ArrayList<IEvent>();
				mlu.put(index, ul);
			}
			ul.add(e);
		}
		return mlu;
	}

	private static String getKey(String sip, int sport) {
		// TODO Auto-generated method stub
		return sip + "^" + sport;
	}
}
