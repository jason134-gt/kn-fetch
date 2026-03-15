package com.yfzx.service.msg.handler.m;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.UserMsg;
import com.yfzx.service.msg.UserEventService;
import com.yz.mycore.msg.handler.IHandler;

public class UserType14MsgHandler implements IHandler {

	@Override
	public void handle(Object o) {
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		UserMsg um = (UserMsg) e.getMsg();
		String nkey = um.getD()+"^"+um.getMsgType();
		//放入缓存
		UserEventService.getInstance().put2MessageListWapper(nkey, um);

	}

}
