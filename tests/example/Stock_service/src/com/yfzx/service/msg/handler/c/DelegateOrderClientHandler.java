package com.yfzx.service.msg.handler.c;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.DelegateOrderMsg;
import com.yfzx.service.db.DelegateOrderService;
import com.yz.mycore.msg.handler.IHandler;

public class DelegateOrderClientHandler implements IHandler {

	static Logger logger = LoggerFactory.getLogger(DelegateOrderClientHandler.class);
	

	public void handle(Object h) {

		if (h == null)
			return;
		NotifyEvent e = (NotifyEvent) h;
		DelegateOrderMsg dom = (DelegateOrderMsg) e.getMsg();
		DelegateOrderService.getInstance().findCanOrder(dom);
	}

}
