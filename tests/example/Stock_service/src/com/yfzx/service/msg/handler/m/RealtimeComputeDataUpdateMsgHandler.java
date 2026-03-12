package com.yfzx.service.msg.handler.m;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.RealtimeComputeDataUpdateMsg;
import com.yfzx.service.db.IndexService;
import com.yz.mycore.msg.handler.IHandler;

public class RealtimeComputeDataUpdateMsgHandler implements IHandler {
	
	@Override
	public void handle(Object o) {
		if (o == null)
			return;		
	
		NotifyEvent e = (NotifyEvent) o;
		RealtimeComputeDataUpdateMsg um = (RealtimeComputeDataUpdateMsg) e.getMsg();
		IndexService.getInstance().putRealtimeResult2CacheV3(um.getBody());
	}

}
