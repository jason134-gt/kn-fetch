package com.yfzx.service.msg.handler.m;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.StatictisMsg;
import com.yfzx.service.count.StatisticsService;
import com.yz.mycore.msg.handler.IHandler;

public class StatictisMsgHandler implements IHandler {

	@Override
	public void handle(Object o) {
		
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		StatictisMsg um = (StatictisMsg) e.getMsg();
		StatisticsService.getInstance().putStatisticsItem2Cache(um.getKey(), um.getStatictisItemName(), um.getAdd());
	}

}
