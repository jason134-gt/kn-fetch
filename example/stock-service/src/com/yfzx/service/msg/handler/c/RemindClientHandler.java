package com.yfzx.service.msg.handler.c;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.model.snn.EConst;
import com.stock.common.msg.BaseMsg;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.service.IMessageSenderService;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.MessageSenderServiceClient;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.yas.YASFactory;
import com.yfzx.yas.router.ISelectRouter;
import com.yfzx.yas.router.RouterCenter;
import com.yfzx.yas.router.SetSelectRouter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.event.IEvent;
import com.yz.mycore.msg.handler.IHandler;
import com.yz.mycore.msg.message.IMessage;

public class RemindClientHandler implements IHandler {

	static Logger logger = LoggerFactory.getLogger(RemindClientHandler.class);
	//private static AtomicInteger _ai = new AtomicInteger(0);// 处理批次
	private static int batchsize = 100;
	static long lastHandleTimes = System.currentTimeMillis();
	private static ConcurrentHashMap<Integer, List<IEvent>> _bcache = new ConcurrentHashMap<Integer, List<IEvent>>();
	private static RemindClientHandler instance = new RemindClientHandler();

	public static Map<String, Dbrouter> _routerPool = new ConcurrentHashMap<String, Dbrouter>();
	public static Map<String, Dbrouter> _routerUdpPool = new ConcurrentHashMap<String, Dbrouter>();
	static List<Dbrouter> _udpRouterList;
	static String iserviceName = "IMessageSenderService";
	static String send2zjs = "udpSend2zjs";
	private RemindClientHandler()
	{

	}
	public static RemindClientHandler getInstance()
	{
		return instance;
	}
	static {
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				batchsize = ConfigCenterFactory.getInt("yas.msg_batchsize", 100);
			}

		});
	}
	static {
		initRouter();
		initUdpRouter();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			public void refresh() {
				initRouter();
				initUdpRouter();

			}

		});
	}

	private static void reinitRouter() {
		List<Dbrouter> rl = RouterCenter.getInstance().getRouter(iserviceName);
		if (rl != null && _routerPool.keySet().size() != rl.size())
			initRouter();
	}

	private static void initUdpRouter() {
		String uips = ConfigCenterFactory.getString("yas." + send2zjs,
				"udp://192.168.1.102:8858/dcss/IMessageSenderService");
		if (!StringUtil.isEmpty(uips)) {
			// _udpRouterList
			List<Dbrouter> nudpRouterList = new ArrayList<Dbrouter>();
			for (String ips : uips.split(";")) {
				if (!StringUtil.isEmpty(ips)) {
					try {
						Dbrouter ur = new Dbrouter();
						ur.setServiceName(iserviceName);
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
				_routerUdpPool.clear();
				for (Dbrouter dr : _udpRouterList) {
					_routerUdpPool.put(getKey(dr.getSip(), dr.getSport()), dr);
				}
			}
		}

	}

	private static void initRouter() {
		try {
			// TODO Auto-generated method stub
			List<Dbrouter> rl = RouterCenter.getInstance().getRouter(
					iserviceName);
			if (rl == null) {
				logger.error("not found  roueter !");
				return;
			}
			_routerPool.clear();
			for (Dbrouter dr : rl) {
				_routerPool.put(getKey(dr.getSip(), dr.getSport()), dr);
			}
		} catch (Exception e) {
			logger.error("init roueter failed!", e);
		}
	}

	private static String getKey(String sip, int sport) {
		// TODO Auto-generated method stub
		return sip + "^" + sport;
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
			BaseMsg um = (BaseMsg)e.getMsg();
			if(um==null){
				return;
			}
			if(um.getSendType()==MsgConst.SEND_TYPE_1)
			{
				sendAsynUdpMessagePool(e);
			}
			else
			{
				String seed = ((UserMsg) e.getMsg()).getD().toString();
				String selectIp = RemindServiceClient.getInstance().lastLoginIp(Long.parseLong(seed));
				if(selectIp==null){
					return ;
				}
				MessageSenderServiceClient.getInstance().sendAsynUdpMessage(seed,e,new ISelectRouter() {
					@Override
					public String getName() {
						return "SR_RemindClientHandler";
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
							String selectIp = RemindServiceClient.getInstance().lastLoginIp(Long.parseLong(k));
							if(selectIp==null){
								return null;
							}
							String ip = selectIp.split(":")[0];
							int port = Integer.valueOf(selectIp.split(":")[1]);
							dr = RemindClientHandler._routerUdpPool.get(getKey(ip, port));
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


		public void notifyTheEvent(IMessage im) {
			NotifyEvent ne = new NotifyEvent();
			ne.setHType(EConst.EVENT_9);
			ne.setMsg(im);
			ClientEventCenter.getInstance().putEvent2ChildQueue(EConst.EVENT_9, ne);
		}


		public void sendAsynUdpMessagePool(IEvent msg) {
			List<Dbrouter> drl = getUdpDbrouterList();
			for (Dbrouter dr : drl) {
				try {
					getAsynUdpService(dr).sendMessage("", msg);
				} catch (Exception e) {
					logger.error("sendAsynUdpMessagePool failed!", e);
				}
			}
		}

		private List<Dbrouter> getUdpDbrouterList() {

			return _udpRouterList;
		}

		IMessageSenderService getAsynUdpService(Dbrouter dr) {
			return YASFactory.getAsynUdpService(IMessageSenderService.class,
					new SetSelectRouter(dr));
		}
}
