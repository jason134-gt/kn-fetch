package com.yfzx.service.msg.handler.c;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.AAAConstants;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.model.snn.EConst;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.MessageSenderServiceClient;
import com.yfzx.yas.router.ISelectRouter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.event.IEvent;
import com.yz.mycore.msg.handler.HandleUtil;
import com.yz.mycore.msg.handler.IHandler;
import com.yz.mycore.msg.message.IMessage;

public class MobileClientHandler implements IHandler {

	private static AtomicInteger _ai = new AtomicInteger(0);// 处理批次
	private static int batchsize = 100;
	static long lastHandleTimes = System.currentTimeMillis();
	private static ConcurrentHashMap<Integer, List<IEvent>> _bcache = new ConcurrentHashMap<Integer, List<IEvent>>();
	static Logger logger = LoggerFactory.getLogger(MobileClientHandler.class);
	private static MobileClientHandler instance = new MobileClientHandler();
	private MobileClientHandler()
	{
		
	}
	public static MobileClientHandler getInstance()
	{
		return instance;
	}

	public void handle(Object h) {
		if (h == null)
			return;
		NotifyEvent e = (NotifyEvent) h;
//		lastHandleTimes = System.currentTimeMillis();
//		HandleUtil.combineEvent(_bcache,_ai,batchsize,e);
		sendSingleMessage(e);
	}


	private void sendSingleMessage(NotifyEvent e) {
			UserMsg um = (UserMsg)e.getMsg();
			if(um.getSendType()==MsgConst.SEND_TYPE_1)
			{
				MessageSenderServiceClient.getInstance().sendAsynUdpMessagePool(e);
			}
			else
			{
				String seed = ((UserMsg) e.getMsg()).getD().toString();
				MessageSenderServiceClient.getInstance().sendAsynUdpMessage(seed,e,new ISelectRouter() {

					@Override
					public String getName() {
						// TODO Auto-generated method stub
						return "SR_MobileClientHandler";
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
									"dcss.ext_zjs_ip_list_udp",
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

	static {
//		Thread bt = new Thread(new Runnable() {
//			public void run() {
//				while (true) {
//					HandleUtil.batchHandle(_bcache,_ai,lastHandleTimes,batchsize,new IHandler(){
//						public void handle(Object h) {
//							if(h==null)
//								return ;
//							List<IEvent> sl = (List<IEvent>) h;
//							batchHandle(sl);
//						}
//						
//					});
//				}
//
//			}
//
//		});
//		bt.setName("MobileClientHandler_batchUserNotifyEventThread");
//		bt.start();
		
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			public void refresh() {
				batchsize = ConfigCenterFactory.getInt("yas.msg_batchsize", 100);
			}

		});
	}


	public void notifyTheEvent(IMessage im) {
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_8);
		ne.setMsg(im);
		ClientEventCenter.getInstance().putEvent2ChildQueue(EConst.EVENT_8, ne);
	}

	/**
	 * 批量处理，把消息分发到订阅了的机器上去
	 * @param sl
	 */
	protected static void batchHandle(List<IEvent> sl) {
		if(sl==null||sl.size()==0) return;
		//对用户消息进行分组
		Map<Integer,List<IEvent>> mlu = groupByUid(sl); 
		Iterator<Integer> iter = mlu.keySet().iterator();
		while(iter.hasNext())
		{
			Integer index = iter.next();
			List<IEvent> ul = mlu.get(index);
			if(ul==null||ul.size()==0)
				continue;
			NotifyEvent ue = (NotifyEvent)ul.get(0);
			String seed = ((UserMsg) ue.getMsg()).getD().toString();
			if(StringUtils.isBlank(seed))//容错处理@lhj
				continue;
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
						return "SR_MobileClientHandler";
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
									"dcss.ext_zjs_ip_list_udp",
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
		
	}

	private static Map<Integer, List<IEvent>> groupByUid(List<IEvent> sl) {
		Map<Integer, List<IEvent>> mlu = new HashMap<Integer,List<IEvent>>();
		for(IEvent e :sl)
		{
			NotifyEvent ue =(NotifyEvent) e;
			String sips = ConfigCenterFactory.getString(
					"dcss.ext_zjs_ip_list_udp",
					"192.168.1.102:6666");
			
			String[] ips = sips.split("\\^");
			int dcssNums = ips.length;
			UserMsg um = (UserMsg)ue.getMsg();
			if(um == null){
				logger.warn("UserMsg is null");
				continue;
			}
			int index = StockUtil.getUserIndex(String.valueOf(um.getD()))%dcssNums;
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
