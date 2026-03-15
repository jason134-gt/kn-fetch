package com.yfzx.service.msg.handler.m;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.BFConst;
import com.stock.common.bloomfilter.BFUtil;
import com.stock.common.constants.AAAConstants;
import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.comet.CometPushMsgType;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.event.IMessageListWapper;
import com.yfzx.service.msg.handler.c.RemindClientHandler;
import com.yfzx.service.share.MicorBlogService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.handler.IHandler;
import com.yz.mycore.msg.message.IMessage;
/**
 * 发表博文消息处理器
 *      
 * @author：杨真 
 * @date：2014-8-14
 */
public class UserType0MsgHandler implements IHandler {
	private static Logger log = LoggerFactory.getLogger(UserType0MsgHandler.class);
	int serial = -1;
	private boolean needBroadcast;
	long visiterUid = ConfigCenterFactory.getLong("stock_zjs.system_uid", 10009L);
	@Override
	public void handle(Object o) {
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		Object obj = e.getMsg();
		//后台打印错误，此代码为了定位问题
		if( (obj instanceof UserMsg) == false ){
			IMessage tam = (IMessage)obj;
			log.error(tam.getKey() + "###"+ tam.getMsgType());
		}
		UserMsg um = (UserMsg) e.getMsg();
		String doDelete = um.getAttr("doDelete");//是否为删除博文
		String articleUUID = um.getAttr("uuid");
		log.info("接收到uuid="+articleUUID+"的文章消息");
		Long suid = Long.parseLong(um.getS());
		// 取此消息，在本机上的订阅列表
		List<Long> ll = MicorBlogService.getInstance()
				.getBefollowListOnlineLocalFromCache(Long.valueOf(um.getS()));
		if(!ll.contains(suid)){
			ll.add(suid);//这里要把＂自己＂加进来，因为我发的博文对我应该是可见的
		}
		if(um!=null&& articleUUID!=null)
		{
			if (ll != null && ll.size()>0) {
				List<Long> removeUidList = new ArrayList<Long>();
				for (Long uid : ll) {
					String nkey = StockUtil.getFavoriteListcKey(String.valueOf(uid));
					if("true".equals(doDelete)){
						IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
						if(eq!=null){
							List<IMessage> list = eq.getMessageList();
							//List<IMessage> removelist = Lists.newArrayList();
							UserMsg copy = null;
							for(IMessage m : list){
								UserMsg um2 = (UserMsg)m;
								if(um.getAttr("uuid").equals(um2.getAttr("uuid"))){
									copy = um2.clone();
									break;
								}
							}
							if(copy!=null){
								eq.remove(copy);
							}
							//eq.removeAll(removelist);
						}
					}else{
						String title = um.getAttr("title");
						if(StringUtil.isEmpty(title) || title.trim().isEmpty()){
							UserEventService.getInstance().put2MessageListWapperBysize(nkey, um, 500);
						}else{
							String key = uid+"_"+title;
							boolean checkExist = BFUtil.checkAndAdd(BFConst.articleKeyFilter, key);							
							if(checkExist == false){
								UserEventService.getInstance().put2MessageListWapperBysize(nkey, um, 500);
							}else{
								//用户同一标题,不加入用户队列，不推送到zjs端
								removeUidList.add(uid);
							}
						}
					}
				}
				
				ll.removeAll(removeUidList);
				
				if(!"true".equals(doDelete)){
					needBroadcast = false;
					Integer cometSwitch = ConfigCenterFactory.getInt("stock_log.comet_log_switch", 1);
					// 我的粉丝们，在本机的直接写缓存，在其它机器的异步远程写
					Map<Integer, List<Long>> map = groupByUid(ll);
					Iterator<Integer> iter = map.keySet().iterator();
					while (iter.hasNext()) {
						Integer index = iter.next();
						List<Long> ll_new = map.get(index);
						if(ll_new != null && ll_new.size()>0 ){
							UserMsg um3 = SMsgFactory
									.getSingleUserMsgByType(MsgConst.MSG_REMIND_CLIENT_0);
							um3.putAttr("type", CometPushMsgType.INDEX_PAGE);
							um3.setS(um.getS());
							um3.setD(String.valueOf(ll_new.get(0)));
							if(ll_new.contains(suid)){
								ll_new.remove(suid);
							}
							um3.putAttr("uidList", ll_new.toArray());
							if(ll_new.size()>0){
								if(cometSwitch == 1) {
									long uid = ll_new.get(0);
									String selectIp = RemindServiceClient.getInstance().lastLoginIp(uid);
									log.info("comet_push_msg senderUid:"+suid+" =======>ip"+selectIp +" uidList :"+ll_new);
								}
								if(needBroadcast){//包含游客 就广播
									um3.setSendType(MsgConst.SEND_TYPE_1);
									if(!ll_new.contains(visiterUid)){
										ll_new.add(visiterUid);
									}
								}
								RemindClientHandler.getInstance().notifyTheEvent(um3);
							}
						}
					}
				}
				
			}
		}

	}
	
	/**
	 *  根据客户端ＩＰ分组
	 * @param um
	 * @param sul
	 * @return
	 */
	private Map<Integer, List<Long>> groupByUid(List<Long> uidList) {
		Map<Integer, List<Long>> mlu = new HashMap<Integer, List<Long>>();		
		String sips = ConfigCenterFactory.getString(AAAConstants.REFRESH_CLIENT_IP_LIST, "192.168.1.110:5555");
		String[] ips = sips.split("\\^");
		int zjsNums = ips.length;
		for (Long uid : uidList) {
			if(uid.longValue()==visiterUid){
				needBroadcast = true;
			}
			String ip = RemindServiceClient.getInstance().lastLoginIp(uid);
			for(int i=0;i<zjsNums;i++){
				if(ips[i].equals(ip)){
					List<Long> list = mlu.get(i);
					if(list==null){
						list = new ArrayList<Long>();
						mlu.put(i, list);
					}
					list.add(uid);
				}
			}
			
		}
		return mlu;
	}
	
}
