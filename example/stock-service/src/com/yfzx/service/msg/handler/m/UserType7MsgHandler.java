package com.yfzx.service.msg.handler.m;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.AAAConstants;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.USubject;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.comet.CometPushMsgType;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.event.IMessageListWapper;
import com.yfzx.service.msg.event.MyTalkMessageListWapper;
import com.yfzx.service.msg.handler.c.RemindClientHandler;
import com.yfzx.service.msgpush.MobileMsgPushService;
import com.yfzx.service.nosql.NosqlService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.util.ServiceUtil;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.handler.IHandler;

/**
 * 私信处理器
 *
 * @author：杨真
 * @date：2014-8-14
 */
public class UserType7MsgHandler implements IHandler {
	private static Logger log = LoggerFactory.getLogger(UserType7MsgHandler.class);
	private boolean needBroadcast = false;
	long visiterUid = ConfigCenterFactory.getLong("stock_zjs.system_uid", 10009L);

	private TalkMessage toUsubjectTM(TalkMessage tm,String uidStr){
		TalkMessage tmpTm = tm.clone();//new TalkMessage();
		tmpTm.setD(uidStr);
//		tmpTm.setS(tm.getS());
//		tmpTm.setBody(tm.getBody());
//		tmpTm.setTime(tm.getTime());
		//如果UUID一样，这样就会NOSQL只存一份，某个用户删除时有影响,全新生成uuid,2个DCSS同步复杂
		String newUUID = tm.getUuid()+"_u"+uidStr; //UUID.randomUUID().toString();
		tmpTm.setUuid(newUUID);
//		tmpTm.setMsgType(tm.getMsgType());
//		tmpTm.setSendType(tm.getSendType());
//		tmpTm.setStatus(tm.getStatus());
//		tmpTm.set_attr(tm.get_attr());
		return tmpTm;
	}

	int serial = -1;

	@Override
	public void handle(Object o) {
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		TalkMessage tm = (TalkMessage) e.getMsg();
		//接收者接到消息都是未读的
		tm.setStatus(0);
		if (tm != null) {
			Set<Long> commitUidSet = new HashSet<Long>();
			boolean openLog = ConfigCenterFactory.getInt("stock_log.router_log", 0)==1;
			if(openLog){
				log.info("dcss_reciver====>"+tm.toString());
			}
			//查看是否是公司或话题，类似群组，根据类型查询出它的关注者，在线关注者
			Object isOnlineObject = tm.getAttr("isOnline");
			boolean isOnline = false;
			if(isOnlineObject != null ){
				isOnline = Boolean.valueOf(String.valueOf(isOnlineObject));
			}
			Object isUSubjectObject = tm.getAttr("isUSubject");
			boolean isUSubject = false;
			if(isUSubjectObject != null ){
				isUSubject = Boolean.valueOf(String.valueOf(isUSubjectObject));
			}
			Object isGameObject = tm.getAttr("isGame");
			boolean isGame = false;
			if(isGameObject != null ){
				isGame = Boolean.valueOf(String.valueOf(isGameObject));
			}
			
			if(isOnline == true){
				//群发在线用户消息
				List<Long> keys = MicorBlogService.getInstance().getOnlineUser();
				Map<String,TalkMessage> tmMap = new HashMap<String,TalkMessage>();
				for (int i = 0; i < keys.size(); i++) {
					Object uidObj = keys.get(i);
					String uidStr = String.valueOf(uidObj);
					TalkMessage tmpTm = toUsubjectTM(tm, uidStr);
					tmMap.put(uidStr, tmpTm);
				}
				writeUSubjectTalkMessage(tmMap,keys);

			}else if(isUSubject == true){//公司消息 也需要群发
				Set<Long> commitUidSetTmp = doUsubjectSwitch(tm);
				if(commitUidSetTmp == null){
					return;
				}
				commitUidSet = commitUidSetTmp;
			}else if(isGame == true){
				Set<Long> commitUidSetTmp = doGameSwitch(tm);
				if(commitUidSetTmp == null){
					return;
				}
				commitUidSet = commitUidSetTmp;
			}else{//普通消息
				Set<Long> commitUidSetTmp = doGeneralSwitch(tm);
				if(commitUidSetTmp == null){
					return;
				}
				commitUidSet = commitUidSetTmp;	
			}

			UserMsg commit = commitObj(tm);
			if(isOnline){
				commit.setSendType(MsgConst.SEND_TYPE_1);//改为广播
				commit.putAttr("cometAll", true);
			}else{
				if(commitUidSet.size()>0){
					Map<Integer, List<Long>> map = groupByUid(commitUidSet);
					Iterator<Integer> iter = map.keySet().iterator();
					while (iter.hasNext()) {
						Integer index = iter.next();
						List<Long> ll_new = map.get(index);
						if(ll_new != null && ll_new.size()>0 ){
							commit.setS(tm.getS());
							commit.setD(String.valueOf(ll_new.get(0)));
							commit.putAttr("uidList", ll_new.toArray());
							Integer cometSwitch = ConfigCenterFactory.getInt("stock_log.comet_log_switch", 1);
							if(cometSwitch == 1) {
								long uid = ll_new.get(0);
								String selectIp = RemindServiceClient.getInstance().lastLoginIp(uid);
								log.info("comet_push_msg senderUid:"+tm.getS()+" =======>ip"+selectIp +" uidList :"+ll_new);
							}
							if(needBroadcast){
								commit.setSendType(MsgConst.SEND_TYPE_1);//改为广播
								if(!ll_new.contains(visiterUid)){
									ll_new.add(visiterUid);
								}
							}
							RemindClientHandler.getInstance().notifyTheEvent(commit);
						}
					}
				}else{
					RemindClientHandler.getInstance().notifyTheEvent(commit);
				}
			}
		}
	}
	
	private Set<Long> doGeneralSwitch(TalkMessage tm){	
		//uuid 分成2份，即message存2份，空间增加一倍
		String oldUuid = tm.getUuid();
		String newUuid = UUID.randomUUID().toString();
		tm.setUuid(newUuid);
		Set<Long> commitUidSet = new HashSet<Long>();
		// 持久化接收到的消息
		String ret = NosqlService.getInstance().saveTalkMsg(tm.getD(),
				tm.getS(), tm);
		if (StockCodes.SUCCESS.equals(ret)) {
			String nkey = tm.getD() + "^" + tm.getS();
			// 放入缓存
			UserEventService.getInstance().put2MessageListWapper(nkey, tm);

			// 放入以先后顺序排列的D用户好友私信【总】提醒列表中
			String tk = ServiceUtil.getTalkMessageListKey(tm.getD());
			MyTalkMessageListWapper eq = LCEnter.getInstance().get(tk,
					StockUtil.getEventCacheName(tk));
			if (eq == null) {
				eq = new MyTalkMessageListWapper();
				eq.setKey(tk);
				LCEnter.getInstance().put(tk, eq,
						StockUtil.getEventCacheName(tk));
			}
			eq.put(tm);
			//注销时因为？MobileMsgPushService.getInstance().sendUserPrivacyMsg(tm, MsgConst.MSG_USER_TYPE_7);
			//commitUidSet.add(Long.valueOf(tm.getD()));
		}else{
			return null;
		}
		//MessageTestAction 只持久化了我发出的消息，需要 放到缓存里，跟下面nkey的相反
		{
			//双机时，需要通过消息实现
//				String nkey = tm.getS() + "^" + tm.getD();
//				UserEventService.getInstance().put2MessageListWapper(nkey, tm);
			//会话消息重构，发给S^D的时间轴
//			TalkMessage tm2 =  new TalkMessage();
//			tm2 = NosqlBeanUtil.map2Bean(tm2,NosqlBeanUtil.bean2Map(tm));
			TalkMessage tm2 = tm.clone();
			tm2.setUnreadCount(0);
			tm2.setD(tm.getS());
			tm2.setS(tm.getD());
			tm2.setUuid(oldUuid);
			tm2.setStatus(1);//MSG_USER_TYPE_15 是将自己的消息存放DCSS，状态需要回到1
			tm2.setMsgType(MsgConst.MSG_USER_TYPE_15);
			UserEventService.getInstance().synNotifyTheEvent(tm2);

			// 是单机，可以直接访问内存 或类似分组到本机的
//			String sips = ConfigCenterFactory.getString(
//					AAAConstants.SERVER_ID_LIST, "dcss01");
//			String[] ips = sips.split("\\^");
//			if(ips.length ==1){
//				NotifyEvent ne = new NotifyEvent();
//				ne.setHType(EConst.EVENT_3);
//				ne.setMsg(tm2);
//				new UserType15MsgHandler().handle(ne);
//			}
		}
		MobileMsgPushService.getInstance().sendUserPrivacyMsg(tm, MsgConst.MSG_USER_TYPE_7);
		return commitUidSet;
	}
	
	/**
	 * @param tm 
	 * @return 返回null表示usubject不存在
	 */
	private Set<Long> doGameSwitch(TalkMessage tm){		
		String uidS = tm.getS();//模拟用户“投资订阅”
		String uidD = tm.getD();//玩模拟游戏的用户
		Long uidd = Long.valueOf(uidD);
		//加载关注用户【在线】,通知消息
		List<Long> ll = MicorBlogService.getInstance().getBefollowListOnlineLocal(uidd);
		Map<String,TalkMessage> tmMap = new HashMap<String,TalkMessage>();
		Set<Long> commitUidSet = new HashSet<Long>();
		if(ll != null && ll.size() >0 ){
			for(Long uid : ll){
				//本地在线的粉丝用户
				String uidStr = String.valueOf(uid);
				TalkMessage tmpTm = toUsubjectTM(tm, uidStr);
				tm.setS(uidS);
				tmpTm.get_attr().remove("isUSubject");
				tmpTm.get_attr().remove("uidentify");
				tmMap.put(uidStr, tmpTm);
			}
			//写入NOSQL，并写入本地缓存
			writeUSubjectTalkMessage(tmMap,ll);
			commitUidSet.addAll(ll);
		}
		//TODO 未在线的用户，是否要通知
		return commitUidSet;
	}
	
	/**
	 * @param tm 
	 * @return 返回null表示usubject不存在
	 */
	private Set<Long> doUsubjectSwitch(TalkMessage tm){		
		String uidentify = tm.getAttr("uidentify");
		USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
		if(usubject==null){
			log.info("缓存对象为null 请检查" + uidentify);
			return null;
		}
		Set<Long> commitUidSet = new HashSet<Long>();
		long usubjectUid = usubject.getUid();
		if(usubject.getType() == 0){
			//加载关注用户【在线】,通知消息
			List<Long> ll = MicorBlogService.getInstance()
					.getBefollowListOnlineLocal(usubjectUid);
			String usuid = String.valueOf(usubjectUid);
			if("3".equals(String.valueOf(tm.getAttr("t")))){//精华推荐
				for(Long luid : ll){
					String uidStr = String.valueOf(luid);
					sendRecommedBlog(tm,uidStr,usuid);
				}
				commitUidSet.addAll(ll);
			}else{
				//先处理完在线用户
				Map<String,TalkMessage> tmMap = new HashMap<String,TalkMessage>();
				if(ll != null && ll.size() >0 ){
					for(Long uid : ll){
						String uidStr = String.valueOf(uid);
						TalkMessage tmpTm = toUsubjectTM(tm, uidStr);
						tmpTm.get_attr().remove("isUSubject");
						tmpTm.get_attr().remove("uidentify");
						tmMap.put(uidStr, tmpTm);
					}
					writeUSubjectTalkMessage(tmMap,ll);
				}
				//公司 获取公司的所有好友，进行消息的持久化
				//公司下所有的用户 只有一个DCSS有,加载于UserInfoCacheLoadService,TODO 此处需要优化
				String usUidCacheName = CacheUtil.getCacheName(AAAConstants.USUBJECT_WITH_USER_CACHENAME);
				Set<Long> uidSet = LCEnter.getInstance().get(usubjectUid, usUidCacheName);

				MobileMsgPushService.getInstance().sendCompanyPrivacyMsgToAndroid(usubject, tm);
				MobileMsgPushService.getInstance().sendCompanyPrivacyMsgToIOS(usubject, uidSet, tm);
			}
		}else if(usubject.getType() == StockConstants.SUBJECT_TYPE_4 || usubject.getType() == StockConstants.SUBJECT_TYPE_7){//话题
			if("3".equals(String.valueOf(tm.getAttr("t")))){//精华推荐
				boolean send2Online = ConfigCenterFactory.getInt("dcss.send_mail_to_online_user", 0)>0;
				List<String> list = RemindServiceClient.getInstance().getTopicUserRelationship(uidentify);
				String usubjectStr = String.valueOf(usubjectUid);
				if(send2Online){
					List<Long> ll = MicorBlogService.getInstance().getBefollowListOnlineLocalFromCache(usubjectUid);
					for(Long uidOnline :ll){
						String uidStr = String.valueOf(uidOnline);
						sendRecommedBlog(tm,uidStr,usubjectStr);
						if(StringUtils.isNumericSpace(uidStr)){
							long uid = Long.parseLong(uidStr);
							commitUidSet.add(uid);
						}
					}
				}else{
					if(list!=null){
						for(String uidStr :list){
							sendRecommedBlog(tm,uidStr,usubjectStr);
							if(StringUtils.isNumericSpace(uidStr)){
								long uid = Long.parseLong(uidStr);
								commitUidSet.add(uid);
							}
						}
					}
				}

			}
		}else if(usubject.getType() == 5){
			//公众号 TODO 暂未处理
		}
		return commitUidSet;
	}

	//发送推荐博文给订阅了该话题的本机用户
	private void sendRecommedBlog(TalkMessage tm,String uidStr,String suid){
		if(!StringUtils.isNumericSpace(suid)){
			suid  = ConfigCenterFactory.getString("dcss.topic_recommend_uid", "104563"); //给一个固定账号发送精华信息
		}
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_ID_LIST, "dcss01");
		String[] ips = sips.split("\\^");
		int dcssNums = ips.length;
		if(!StringUtils.isNumericSpace(uidStr)){
			return;
		}
		int index = StockUtil.getUserIndex(uidStr) % dcssNums;
		if(serial==-1)
		{
			serial = ServiceUtil.getSerial(sips);
		}
		if(serial == index){
			TalkMessage tm3 = tm.clone();
			//使用clone，提高效率
//			TalkMessage tm3 = new TalkMessage();
//			tm3 = NosqlBeanUtil.map2Bean(tm3,NosqlBeanUtil.bean2Map(tm));
			tm3.get_attr().remove("isUSubject");
			tm3.get_attr().remove("uidentify");
			tm3.setD(uidStr);
			tm3.setS(suid);
			// 持久化接收到的消息
			String ret = NosqlService.getInstance().saveTalkMsg(tm3.getD(),
					tm3.getS(), tm3);
			if (StockCodes.SUCCESS.equals(ret)) {
				String nkey = tm3.getD() + "^" + tm3.getS();
				// 放入缓存
				UserEventService.getInstance().put2MessageListWapper(nkey, tm3);
				// 放入以先后顺序排列的D用户好友私信【总】提醒列表中
				String tk = ServiceUtil.getTalkMessageListKey(tm3.getD());
				MyTalkMessageListWapper eq = LCEnter.getInstance().get(tk,
						StockUtil.getEventCacheName(tk));
				if (eq == null) {
					eq = new MyTalkMessageListWapper();
					eq.setKey(suid);
					LCEnter.getInstance().put(tk, eq,
							StockUtil.getEventCacheName(tk));
				}
				eq.put(tm3);
			}
		}
	}
	/**
	 * 对于在另外的DCSS用户，进行远程写入信息
	 * @param uid
	 * @param tm
	 * @param uidArr
	 * @return
	 */
	public boolean writeUSubjectTalkMessage(Map<String,TalkMessage> tmMap, List<Long> uidArr) {
		try{
			String sips = ConfigCenterFactory.getString(
					AAAConstants.SERVER_ID_LIST, "dcss01");
			List<Long> otherUidArr = new ArrayList<Long>();
			String[] ips = sips.split("\\^");
			int dcssNums = ips.length;
			for (Long uid : uidArr) {
				// NotifyEvent ue =(NotifyEvent) e;
				int index = StockUtil.getUserIndex(String.valueOf(uid)) % dcssNums;
				if(serial==-1)
				{
					serial = ServiceUtil.getSerial(sips);
				}
				//在本机上的,写入缓存
				if(serial == index)
				{

					long iuid = uid;
					TalkMessage tmpTm = tmMap.get(String.valueOf(uid));
					// 持久化接收到的消息
					NosqlService.getInstance().saveTalkMsg(tmpTm.getD(),
							tmpTm.getS(), tmpTm);
					String fuid = tmpTm.getS();//公司的虚拟用户

					String key = iuid + "^" + fuid;
					IMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
					if(eq == null)
					{
						eq = new IMessageListWapper();
						LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
					}
					eq.put(tmpTm);

					String key2 = iuid+"^"+String.valueOf(MsgConst.MSG_USER_TYPE_7);
					MyTalkMessageListWapper eq2 = LCEnter.getInstance().get(key2, StockUtil.getEventCacheName(key2));
					if(eq2 == null)
					{
						eq2 = new MyTalkMessageListWapper();
						eq2.setKey(key2);
						LCEnter.getInstance().put(key2,eq2, StockUtil.getEventCacheName(key2));
					}
					eq2.put(tmpTm);
				}else{//不在本机的，先加入数组中
					otherUidArr.add(uid);
				}
			}
			//新的逻辑都在本机
			//不在本机的,调用远程方法
//			if(otherUidArr.size()>0){
//				Map<String,TalkMessage> tmMapOther = new HashMap<String,TalkMessage>();
//				for (Long uid : otherUidArr) {
//					String uidStr = String.valueOf(uid);
//					tmMapOther.put(uidStr, tmMap.get(uidStr));
//				}
//				RemindServiceClient.getInstance().writeUSubjectTalkMessage(otherUidArr.get(0), tmMapOther, otherUidArr);
//			}

			return true;
		}catch (Exception e) {
			return false;
		}
	}

	/**
	 *  根据客户端ＩＰ分组
	 * @param um
	 * @param sul
	 * @return
	 */
	private Map<Integer, List<Long>> groupByUid(Set<Long> uidList) {
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

	private UserMsg commitObj(TalkMessage tm){
		UserMsg um3 = SMsgFactory.getSingleUserMsgByType(MsgConst.MSG_REMIND_CLIENT_0);
		um3.setS(tm.getS());
		um3.setD(tm.getD());
		um3.putAttr("type", CometPushMsgType.UN_READ_MSG);
		return um3;
	}
}
