package com.yfzx.service.msg.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.service.IMessageSenderService;
import com.stock.common.service.YService;
import com.yz.mycore.msg.ServerEventCenter;
import com.yz.mycore.msg.event.IEvent;

@YService(serviceName = "IMessageSenderService")
public class MessageSenderServiceImpl implements IMessageSenderService {
	Logger logger = LoggerFactory.getLogger(MessageSenderServiceImpl.class);

	@Override
	public Object sendMessage(String seed, List<IEvent> el) {
		if (el == null || el.size() == 0)
			return null;
		boolean isfailed = false;
		for (IEvent e : el) {
			try {
				ServerEventCenter.getInstance().putEvent(e);
			} catch (Exception e2) {
				logger.error("handle msg failed", e2);
				isfailed = true;
			}
		}
		if (isfailed)
			return -1;
		return 0;
	}

	@Override
	public Object sendMessage(String seed, IEvent e) {
		if (e == null)
			return null;
		boolean isfailed = false;
		try {
			ServerEventCenter.getInstance().putEvent(e);
		} catch (Exception e2) {
			logger.error("handle msg failed", e2);
			isfailed = true;
		}
		if (isfailed)
			return -1;
		return 0;
	}

	@Override
	public Object sendSyncMessage(String seed, IEvent e) {
		if (e == null)
			return null;
		boolean isfailed = false;
		try {
			ServerEventCenter.getInstance().invoke(e);
		} catch (Exception e2) {
			logger.error("handle msg failed", e2);
			isfailed = true;
		}
		if (isfailed)
			return -1;
		return 0;
	}

}
