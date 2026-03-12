package com.yfzx.service.msg.handler.m;

import com.stock.common.event.NotifyEvent;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.msg.MsgConst;
import com.stock.common.util.StockUtil;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.event.MyTalkMessageListWapper;
import com.yfzx.service.util.ServiceUtil;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.handler.IHandler;

/**
 * 我的私信 存储到缓存
 */
public class UserType15MsgHandler implements IHandler {

	public void handle(Object o) {
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		TalkMessage tm = (TalkMessage) e.getMsg();
		if (tm != null) {
			//反转回去
			String d = tm.getD();
			String s = tm.getS();
			tm.setS(d);
			tm.setD(s);
			tm.setMsgType(MsgConst.MSG_USER_TYPE_7);
			//存储到S^D上
			String nkey = tm.getS() + "^" + tm.getD();
			UserEventService.getInstance().put2MessageListWapper(nkey, tm);
			

			// 放入以先后顺序排列的D用户好友私信【总】提醒列表中
			String tk = ServiceUtil.getTalkMessageListKey(tm.getS());
			MyTalkMessageListWapper eq = LCEnter.getInstance().get(tk,
					StockUtil.getEventCacheName(tk));
			if (eq == null) {
				eq = new MyTalkMessageListWapper();
				eq.setKey(tk);
				LCEnter.getInstance().put(tk, eq,
						StockUtil.getEventCacheName(tk));
			}
			eq.put(tm);
		}
	}
}
