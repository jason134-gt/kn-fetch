package com.yz.stock.realtime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.MessageSenderServiceClient;
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
public class RealtimeComputeDataClientHandle implements IHandler {
	static List<Dbrouter> _udpRouterList;
	static Logger logger = LoggerFactory
			.getLogger(RealtimeComputeDataClientHandle.class);
	static long lastHandleTimes = System.currentTimeMillis();
	static long lastPrintTimes = System.currentTimeMillis();
	static HandleUtil<IEvent> hu = new HandleUtil<IEvent>();
	static {
		initUdpPollRouter();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			public void refresh() {
				initUdpPollRouter();

			}

		});

	}

	private static void initUdpPollRouter() {
		String uips = ConfigCenterFactory.getString(
				"yas.realtime_compute_data_poll_address",
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
			if (nudpRouterList.size() != 0) {
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

		NotifyEvent se = (NotifyEvent) e;
		// 发送给注册了接收消息的端口
		List<IEvent> el = new ArrayList<IEvent>(2);
		el.add(se);
		sendBroadcast(el);

	}

	private static void sendBroadcast(List<IEvent> lmsg) {

		for (Dbrouter dr : _udpRouterList) {
			try {
				MessageSenderServiceClient.getInstance().sendAsynUdpMessage(dr,
						lmsg);
			} catch (Exception e) {
				logger.error("sendAsynUdpMessagePool failed!", e);
			}
		}
	}

}
