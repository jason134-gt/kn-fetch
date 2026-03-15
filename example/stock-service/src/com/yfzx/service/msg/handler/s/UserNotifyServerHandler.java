package com.yfzx.service.msg.handler.s;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.AAAConstants;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.USubject;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.comet.CometPushMsgType;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.handler.c.RemindClientHandler;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.msg.MessageCenter;
import com.yz.mycore.msg.handler.IHandler;
/**
 * 用户通知消息服务端接收类
 *      
 * @author：杨真 
 * @date：2014-7-26
 */
public class UserNotifyServerHandler implements IHandler {

	static Logger logger = LoggerFactory.getLogger(UserNotifyServerHandler.class);
	static AtomicInteger c = new AtomicInteger(0);
	//private boolean needBroadcast;
	//long visiterUid = ConfigCenterFactory.getLong("stock_zjs.system_uid", 10009L);
	public void handle(Object o) {
		if (o == null)
			return;
		try {
				NotifyEvent e = (NotifyEvent) o;
				if(e!=null){
					UserMsg um = (UserMsg)e.getMsg();
					if(um==null)
						return;
					/*if(um.getMsgType()==MsgConst.MSG_USER_TYPE_2 || um.getMsgType()==MsgConst.MSG_USER_TYPE_3 
							||um.getMsgType()==MsgConst.MSG_USER_TYPE_4 || um.getMsgType()==MsgConst.MSG_USER_TYPE_5
							||um.getMsgType()==MsgConst.MSG_USER_TYPE_6||um.getMsgType()==MsgConst.MSG_USER_TYPE_7
							||um.getMsgType()==MsgConst.MSG_USER_TYPE_13){
						needBroadcast = false;
						//通知客户端刷新消息未读数
						UserMsg um3 = SMsgFactory.getSingleUserMsgByType(MsgConst.MSG_REMIND_CLIENT_0);
						um.getMsgType();
						um3.setS(um.getS());
						um3.setD(um.getD());
						um3.putAttr("type", CometPushMsgType.UN_READ_MSG);
						List<Long> uidlist = Lists.newArrayList();
						boolean isOnline = false;
						boolean isUSubject = false;
						boolean broadcast = false;
						Object isOnlineObject = um.getAttr("isOnline");
						Object isUSubjectObject = um.getAttr("isUSubject");
						Object obj = um.getAttr("broadcast");
						if(isOnlineObject != null ){
							isOnline = Boolean.valueOf(String.valueOf(isOnlineObject));
						}
						if(isUSubjectObject != null ){
							isUSubject = Boolean.valueOf(String.valueOf(isUSubjectObject));
						}
						if(obj != null ){
							broadcast = Boolean.valueOf(String.valueOf(obj));
						}
						if(isOnline){
							um3.setSendType(MsgConst.SEND_TYPE_1);//改为广播
							um3.putAttr("cometAll", true);
						}
						if(isUSubject){
							String uidentify = um.getAttr("uidentify");
							USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
							if(usubject == null){
								USubject us = new USubject();
								us.setUidentify(uidentify);
								usubject = USubjectService.getInstance().getUsubject(us);
							}
							 uidlist = MicorBlogService.getInstance()
									.getBefollowListOnlineLocal(usubject.getUid());
							 if(broadcast){
									Map<String, String> map = CassandraHectorGateWay.getInstance().get(SAVE_TABLE.TOPICBEFOLLOW.toString(),uidentify);
									for(String uidStr : map.keySet()){
										if(StringUtils.isNumericSpace(uidStr)){
											Long uids = Long.parseLong(uidStr);
											if(!uidlist.contains(uids)){
												uidlist.add(uids);
											}
										}
									}
								}
						}
						if(uidlist.size()>0){
							Integer cometSwitch = ConfigCenterFactory.getInt("stock_log.comet_log_switch", 1);
							Map<Integer, List<Long>> map = groupByUid(uidlist);
							Iterator<Integer> iter = map.keySet().iterator();
							while (iter.hasNext()) {
								Integer index = iter.next();
								List<Long> ll_new = map.get(index);
								if(ll_new != null && ll_new.size()>0 ){
									um3.setS(um.getS());
									um3.setD(String.valueOf(ll_new.get(0)));
									um3.putAttr("uidList", ll_new.toArray());
									if(ll_new.size()>0){
										if(cometSwitch == 1) {
											long uid = ll_new.get(0);
											String selectIp = RemindServiceClient.getInstance().lastLoginIp(uid);
											logger.info("comet_push_msg senderUid:"+um.getS()+" =======>ip"+selectIp +" uidList :"+ll_new);
										}
										if(needBroadcast && um.getMsgType()==MsgConst.MSG_USER_TYPE_7){
											um3.setSendType(MsgConst.SEND_TYPE_1);//改为广播
											if(!ll_new.contains(visiterUid)){
												ll_new.add(visiterUid);
											}
										}
										RemindClientHandler.getInstance().notifyTheEvent(um3);
									}
								}
							}
						}else{
							RemindClientHandler.getInstance().notifyTheEvent(um3);
						}
					}*/
				}
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
	
	/**
	 *  根据客户端ＩＰ分组
	 * @param um
	 * @param sul
	 * @return
	 */
	/*private Map<Integer, List<Long>> groupByUid(List<Long> uidList) {
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
	}*/
	
	
}
