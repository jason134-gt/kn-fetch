package com.yfzx.service.comet;

import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.handler.c.RemindClientHandler;

public class CometService {
	private static CometService instance = new CometService();
	private CometService()
	{
		
	}
	public static CometService getInstance()
	{
		return instance;
	}
	
	//CometPushMsgType.UN_READ_MSG
	public void putCometEvent(UserMsg um ,CometPushMsgType type){
		UserMsg um3 = SMsgFactory.getSingleUserMsgByType(MsgConst.MSG_REMIND_CLIENT_0);
		um3.setS(um.getS());
		um3.setD(um.getD());
		um3.putAttr("type", type);
		RemindClientHandler.getInstance().notifyTheEvent(um3);
	}

}
