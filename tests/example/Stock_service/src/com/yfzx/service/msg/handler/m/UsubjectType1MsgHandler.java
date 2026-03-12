package com.yfzx.service.msg.handler.m;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.StockUtil;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.stockgame.StockGameFollowService;
import com.yz.mycore.msg.handler.IHandler;

/**
 * 发送"XXX的投资"的下单消息，用于分发到订阅该话题的用户[mySubs]页面
 * @author tangbinqi
 *
 */
public class UsubjectType1MsgHandler implements IHandler {
	private static Logger log = LoggerFactory.getLogger(UsubjectType1MsgHandler.class);
	
	@Override
	public void handle(Object o) {
		if (o == null)	
			return;
		NotifyEvent e = (NotifyEvent) o;
		UserMsg um = (UserMsg) e.getMsg();		
		String articleUUID = um.getAttr("uuid");
		String uid = um.getAttr("uid");
		long time = um.getTime();
		//put消息引用给本台DCSS的订阅用户
//		List<String> subscribeList = StockGameFollowService.getInstance().getGameFollow(uid);
//		for(String subuid : subscribeList){
//			try{
//				Long subuidLong  = Long.valueOf(subuid);
//				boolean isLogin = MicorBlogService.getInstance().isLogin(subuidLong);
//				if(isLogin == true){				
//					String nKey =  StockUtil.getStockChanceListcKey(subuid);
//					UserMsg umTmp = new UserMsg();
//					umTmp.setS(uid);
//					umTmp.setD(subuid);
//					um.setTime(time);
//					um.putAttr("uuid",articleUUID);
//					UserEventService.getInstance().put2MessageListWapperBysize(nKey, um, 500);
//				}				
//			}catch(Exception exception){
//				log.error("",exception);
//			}			
//		}
		
		List<Long> subscribeList = MicorBlogService.getInstance().getBefollowListOnlineLocal(Long.valueOf(uid));
		for(Long subuid : subscribeList){
			String subuidStr = String.valueOf(subuid);
			String nKey =  StockUtil.getStockChanceListcKey(subuidStr);
			UserMsg umTmp = new UserMsg();
			umTmp.setS(uid);
			umTmp.setD(subuidStr);
			um.setTime(time);
			um.putAttr("uuid",articleUUID);
			UserEventService.getInstance().put2MessageListWapperBysize(nKey, um, 500);
		}
	}

}
