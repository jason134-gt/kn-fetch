package com.yfzx.service.msg.handler.m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.StockUtil;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.handler.IHandler;

/**
 * 用户登出消息处理器
 * 
 * @author：杨真
 * @date：2014-8-14
 */
public class UserType9MsgHandler implements IHandler {
	private static Logger log = LoggerFactory.getLogger(UserType9MsgHandler.class);
	@Override
	public void handle(Object o) {
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		UserMsg um = (UserMsg) e.getMsg();
		long uid = Long.parseLong(um.getD());
		//标识已登出
		LCEnter.getInstance().remove(uid,SCache.CACHE_NAME_userlogincache);
		//移除登出者的相关cache
		String key = "active_" + uid;
		String cachename = StockUtil.getUserCacheName(key);
		LCEnter.getInstance().remove(key,cachename);
		log.info("uid "+uid+" 退出登录,移除登录缓存，移除活跃用户列表缓存");
	}
}
