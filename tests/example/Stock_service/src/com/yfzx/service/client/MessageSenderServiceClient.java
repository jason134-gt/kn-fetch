package com.yfzx.service.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.router.Dbrouter;
import com.stock.common.service.IMessageSenderService;
import com.stock.common.util.StringUtil;
import com.yfzx.yas.YASFactory;
import com.yfzx.yas.router.ISelectRouter;
import com.yfzx.yas.router.RouterCenter;
import com.yfzx.yas.router.SetSelectRouter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.msg.event.IEvent;

/**
 * 
 * @author：杨真
 * @date：2013-4-24
 */
public class MessageSenderServiceClient {

	public static Map<String, Dbrouter> _routerPool = new ConcurrentHashMap<String, Dbrouter>();
	public static Map<String, Dbrouter> _routerUdpPool = new ConcurrentHashMap<String, Dbrouter>();
	static Logger logger = LoggerFactory.getLogger(RouterCenter.class);
	private static MessageSenderServiceClient instance = new MessageSenderServiceClient();
	static List<Dbrouter> _udpRouterList;
	static String iserviceName = "IMessageSenderService";

	public MessageSenderServiceClient() {

	}

	public static MessageSenderServiceClient getInstance() {
		return instance;
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
		String uips = ConfigCenterFactory.getString("yas." + iserviceName,
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

	IMessageSenderService getService(ISelectRouter isr) {
		reinitRouter();
		return YASFactory.getService(IMessageSenderService.class,
				isr);
	}

	IMessageSenderService getAsynService(ISelectRouter isr) {
		reinitRouter();
		return YASFactory.getService(IMessageSenderService.class,
				isr);
	}

	IMessageSenderService getAsynService(Dbrouter dr) {
		reinitRouter();
		return YASFactory.getAsynService(IMessageSenderService.class,
				new SetSelectRouter(dr));
	}

	IMessageSenderService getAsynUdpService(Dbrouter dr) {
		return YASFactory.getAsynUdpService(IMessageSenderService.class,
				new SetSelectRouter(dr));
	}

		IMessageSenderService getUdpService(ISelectRouter isr) {
		return YASFactory.getUdpService(IMessageSenderService.class,
				isr);
	}
	
	IMessageSenderService getAsynUdpService(ISelectRouter isr) {
		return YASFactory.getAsynUdpService(IMessageSenderService.class,
				isr);
	}
	
	public Object sendMessage(String seed, List<IEvent> lmsg,ISelectRouter isr) {
		return getService(isr).sendMessage(seed, lmsg);
	}

	public void sendAsynMessagePool(List<IEvent> lmsg) {
		Iterator<String> iter = _routerPool.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Dbrouter dr = _routerPool.get(key);
			if (dr != null) {
				try {
					getAsynService(dr).sendMessage("", lmsg);
				} catch (Exception e) {
					logger.error("sendAsynMessagePool failed!", e);
				}
			}
		}
	}

	public void sendAsynUdpMessagePool(List<IEvent> lmsg) {
		List<Dbrouter> drl = getUdpDbrouterList();
		for (Dbrouter dr : drl) {
			try {
				getAsynUdpService(dr).sendMessage("", lmsg);
			} catch (Exception e) {
				logger.error("sendAsynUdpMessagePool failed!", e);
			}
		}
	}

	public void sendAsynUdpMessage(Dbrouter dr, List<IEvent> lmsg) {
		try {
			getAsynUdpService(dr).sendMessage("", lmsg);
		} catch (Exception e) {
			logger.error("sendAsynUdpMessagePool failed!", e);
		}
	}

	public void sendAsynUdpMessage(String seed,List<IEvent> lmsg,ISelectRouter isr) {
		try {
			getAsynUdpService(isr).sendMessage(seed, lmsg);
		} catch (Exception e) {
			logger.error("sendAsynUdpMessagePool failed!", e);
		}
	}
	public Object sendUdpMessage(String seed,List<IEvent> lmsg,ISelectRouter isr) {
		Object sn=null;
		try {
			sn = getUdpService(isr).sendMessage(seed, lmsg);
		} catch (Exception e) {
			logger.error("sendUdpMessage failed!", e);
		}
		return sn;
	}
	
	
	public Object sendMessage(String seed, IEvent msg,ISelectRouter isr) {
		return getService(isr).sendMessage(seed, msg);
	}

	public void sendAsynMessagePool(IEvent msg) {
		Iterator<String> iter = _routerPool.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Dbrouter dr = _routerPool.get(key);
			if (dr != null) {
				try {
					getAsynService(dr).sendMessage("", msg);
				} catch (Exception e) {
					logger.error("sendAsynMessagePool failed!", e);
				}
			}
		}
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

	public void sendAsynUdpMessage(Dbrouter dr, IEvent msg) {
		try {
			getAsynUdpService(dr).sendMessage("", msg);
		} catch (Exception e) {
			logger.error("sendAsynUdpMessagePool failed!", e);
		}
	}

	public void sendAsynUdpMessage(String seed,IEvent msg,ISelectRouter isr) {
		try {
			getAsynUdpService(isr).sendMessage(seed, msg);
		} catch (Exception e) {
			logger.error("sendAsynUdpMessagePool failed!", e);
		}
	}
	public Object sendUdpMessage(String seed,IEvent msg,ISelectRouter isr) {
		Object sn=null;
		try {
			sn = getUdpService(isr).sendMessage(seed, msg);
		} catch (Exception e) {
			logger.error("sendAsynUdpMessagePool failed!", e);
		}
		return sn;
	}
	private List<Dbrouter> getUdpDbrouterList() {

		return _udpRouterList;
	}
	
	/**
	 * tcp同步调用
	 * @param seed
	 * @param e
	 * @param isr
	 * @return
	 */
	public Object sendSyncMessage(String seed,IEvent e,ISelectRouter isr)
	{
		return getService(isr).sendSyncMessage(seed, e);
	}
}
