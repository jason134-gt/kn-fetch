package com.yfzx.service.msg.handler.m;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.yfzx.service.comet.CometPushMsgType;
import com.yfzx.service.comet.CometService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.handler.c.RemindClientHandler;
import com.yz.mycore.msg.handler.IHandler;
/**
 * 消息处理器
 *      
 * @author：杨真 
 * @date：2014-8-14
 */
public class UserType2MsgHandler implements IHandler {

	@Override
	public void handle(Object o) {
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		UserMsg um = (UserMsg) e.getMsg();
		String nkey = um.getD()+"^"+um.getMsgType();
		//放入缓存
		UserEventService.getInstance().put2MessageListWapper(nkey, um);
		//commit推送
		CometService.getInstance().putCometEvent(um, CometPushMsgType.UN_READ_MSG);
		
	}
	
}
