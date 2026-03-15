package com.yfzx.service.msg.handler.m;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.UserMsg;
import com.yfzx.service.share.MicorBlogService;
import com.yz.mycore.msg.handler.IHandler;

/**
 * 登陆广播消息处理器
 * 
 * @author：杨真
 * @date：2014-8-14
 */
public class UserType12MsgHandler implements IHandler {

	@Override
	public void handle(Object o) {
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		UserMsg um = (UserMsg) e.getMsg();
		//将非本机用户的登录状态在本机上记录，方便检查粉丝是否已经登录
//		MicorBlogService.getInstance().loginRecord(Long.valueOf(um.getS()));
		//取订阅列表
		StringBuffer sb = um.getAttr("dyz");
		String doDelete = um.getAttr("doDelete");
		if(sb!=null)
		{
			String[] usa = sb.toString().split(",");
			for (String uid : usa) {
				if("true".equals(doDelete) == true){
					MicorBlogService.getInstance().notifyFollowDeleteLogin(Long.valueOf(um.getS()),Long.valueOf(uid));
				}else{
					//粉丝数较少的 在这里建立关系
					MicorBlogService.getInstance().putFeisiIsLogin(Long.valueOf(um.getS()),Long.valueOf(uid));
				}
			}
		}
	}

}
