package com.yfzx.service.msg.handler.s;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.yz.mycore.msg.MessageCenter;
import com.yz.mycore.msg.handler.IHandler;
/**
 * 用户通知消息服务端接收类
 *      
 * @author：杨真 
 * @date：2014-7-26
 */
public class MobileServerHandler implements IHandler {

	static Logger logger = LoggerFactory.getLogger(MobileServerHandler.class);
	static AtomicInteger c = new AtomicInteger(0);
	public void handle(Object o) {
		if (o == null)
			return;
		try {
				NotifyEvent e = (NotifyEvent) o;
				List<IHandler> hl = MessageCenter.getInstance().getHandlerByType(e.getMsg().getMsgType());
				if (hl != null) {
					for (IHandler h : hl) {
						try {
							h.handle(e);
						} catch (Exception e2) {
							logger.error("handle event failed!", e2);
						}
					}
				}
		} catch (Exception e) {
			logger.error("handle msg failed!",e);
		}
		
	}

}
