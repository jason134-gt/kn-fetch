package com.yfzx.service.nosql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.model.share.TimeLine;
import com.stock.common.model.share.TimeLineWithUid;
import com.stock.common.model.share.UserExt;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.share.MsgTimeLineService;
import com.yfzx.service.share.TimeLineService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.mycore.msg.message.IMessage;

public class NosqlService {

	//nosql存储talk联系人 uuid:uid方式，分隔符用:
	public final static String TALK_SPLIT_NOSQL = ":";
	private static CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
	private static TimeLineService tls = TimeLineService.getInstance();
	private static NosqlService instance = new NosqlService();
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private NosqlService() {

	}

	public static NosqlService getInstance() {
		return instance;
	}

	/*
	 * 测试方法提供消息 统一处理，包括at消息，对话消息，评论消息
	 * 持久化消息到nosql,先用map模拟nosql,在后期开发时，需要把这块改成直接操作nosql的代码
	 */
	public String saveMsg(String key ,String type,UserMsg body)
	{	
			int msgType = body.getMsgType();
			if( MsgConst.MSG_USER_TYPE_7  == msgType){
				log.error("此接口不允许发私信===="+body);
				return StockCodes.ERROR;
			}
			String MsgKey =  body.getUuid();
			if(StringUtils.isBlank(MsgKey)){
				MsgKey = UUID.randomUUID().toString();
				body.setUuid(MsgKey);
			}
			Map<String,String> objMap = NosqlBeanUtil.bean2Map(body);			
			String sn = MsgTimeLineService.reSn(msgType);
			long timemillis = body.getTime();
			
			if(Long.valueOf(objMap.get("time")) != timemillis){
				System.out.println("###########time not equals###################  key:"+timemillis+" Obj: "+Long.valueOf(objMap.get("time")));
			}
			if(!MsgKey.equals(String.valueOf(objMap.get("uuid")))){
				System.out.println("#############UUID not equals################### key:"+MsgKey+" Obj: "+String.valueOf(objMap.get("uuid")));
			}
			objMap.put("uuid", MsgKey);
			MsgTimeLineService.getInstance().saveTimeLine(key, MsgKey, sn, timemillis);			
			CassandraHectorGateWay.getInstance().insert(MsgTimeLineService.TABLE_SVAE, MsgKey, objMap);
		
		return StockCodes.SUCCESS;
	}
	
	
	/**
	 * @param keyUid 本人
	 * @param snUid + 'talk' 联系人
	 * @param body 
	 * @return
	 */
	public String saveTalkMsg(String keyUid ,String snUid,UserMsg body){			
		int msgType = body.getMsgType();	
		if( MsgConst.MSG_USER_TYPE_7  != msgType){
			log.error("此接口只允许发私信===="+body);
			return StockCodes.ERROR;
		}
		if(body==null 
				|| StringUtils.isBlank(body.getS()) 
				|| StringUtils.isBlank(body.getD()) 
				|| "null".equals(body.getS()) 
				|| "null".equals(body.getD())){
			log.error("body不完整,拒绝存入nosql===="+body);
			return StockCodes.ERROR;
		}
		String MsgKey =  body.getUuid();
		if(StringUtils.isBlank(MsgKey)){
			MsgKey = UUID.randomUUID().toString();
			body.setUuid(MsgKey);
		}
		Map<String,String> objMap = NosqlBeanUtil.bean2Map(body);			
		String sn = MsgTimeLineService.reSn(msgType);
		long timemillis = body.getTime();
		objMap.put("uuid", MsgKey);		
		//System.out.println(Thread.currentThread().getId()+":"+MsgKey+","+body+","+timemillis);
		/*if(msgType==MsgConst.MSG_USER_TYPE_7){
			long suid = Long.parseLong(key);
			long fuid = Long.parseLong(type);
			delOldTalkMessage(suid,fuid);
		}*/
		//保存到 keyUid-'talk'			
		MsgTimeLineService.getInstance().saveTimeLine(keyUid, MsgKey+TALK_SPLIT_NOSQL+ snUid, sn, timemillis);		
		CassandraHectorGateWay.getInstance().insert(MsgTimeLineService.TABLE_SVAE, MsgKey, objMap);
		//保存到 keyUid-snUid
		MsgTimeLineService.getInstance().saveTimeLine(keyUid, MsgKey, snUid, timemillis);
				
		return StockCodes.SUCCESS;
	}
	

	//清除iuid-talk里面的历史私信记录
	public void delOldTalkMessage(Long iuid,Long fuid){
		int start = 0;
		int limit = 100;
		String sn = MsgTimeLineService.reSn(MsgConst.MSG_USER_TYPE_7);
		List<IMessage> list = RemindServiceClient.getInstance().getUserNewTalkMessageList(iuid, fuid, start, limit);
		for(int i=0;i<list.size();i++){
			UserMsg u = (UserMsg)list.get(i);
			if(u!=null){
				Long time = u.getTime();
				if(time!=null){
					MsgTimeLineService.getInstance().deltetTimeLine(String.valueOf(iuid), sn, time);
				}
			}
		}
	}


	/**
	 * 加载用户的未读消息
	 * 
	 * @param key
	 * @param type 
	 * @return
	 * status = 0;//0:未读，1：已读		
	 */
	public List<UserMsg> loadUserNoReadMessage(String key, String type) {
		List<UserMsg> umList2Return = new ArrayList<UserMsg>();
		List<UserMsg> umList = getUserMessageList(key, type, 0, 500);
		for(UserMsg um : umList){
			if(um.getStatus() == 0){
				umList2Return.add(um);
			}
		}
		return umList2Return;
	}

	public int getUserMessageSize(String key, String type){
		int typeInt = Integer.valueOf(type);
		String sn = MsgTimeLineService.reSn(typeInt);
		int count = MsgTimeLineService.getInstance().getCount(key, sn);
		return count;
	}
	public int getTalkMessageSize(Long iuid, long fuid){
		String key = String.valueOf(iuid);
		String sn = String.valueOf(fuid);	
		int count = MsgTimeLineService.getInstance().getCount(key, sn);
		return count;
	}
	
	/**
	 * 总的私信获取
	 * @param key
	 * @param type 必须等于MsgConst.MSG_USER_TYPE_7
	 * @param start
	 * @param limit
	 * @return  List<TalkMessage> 
	 */
	public List<TalkMessage> getTalkMessageList(String key, String type, int start,
			int limit) {
		List<TalkMessage> umList = new ArrayList<TalkMessage>();
		if(String.valueOf(MsgConst.MSG_USER_TYPE_7).equals(type) == false){
			log.warn("type 必须等于MsgConst.MSG_USER_TYPE_7");
			return umList;
		}
		int typeInt = Integer.valueOf(type);
		String sn = MsgTimeLineService.reSn(typeInt);
		if(StringUtil.isEmpty(sn)){
			log.warn("type=" + type + "不存在对应的SN,请确认！");
			return umList;
		}
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime(key, sn, System.currentTimeMillis(), start, limit);
		if(tlList == null || tlList.size() == 0 ){
			return umList;
		}
		List<TimeLineWithUid> tlwuList = new ArrayList<TimeLineWithUid>();
		for(int i=0;i<tlList.size();i++){
			TimeLine tl = tlList.get(i);
			String time = tl.getTimeMillis();
			String[] arr = tl.getUuid().split(TALK_SPLIT_NOSQL);
			if(arr.length == 1){
				TimeLineWithUid  tlwu = new TimeLineWithUid(time,arr[0]);
				tlwuList.add(tlwu);
			}else if(arr.length == 2){
				TimeLineWithUid  tlwu = new TimeLineWithUid(time,arr[0],arr[1]);
				tlwuList.add(tlwu);
			}
		}
		tlList = null;		
		
		String[] mgsUUIDArr = new String[tlwuList.size()];
		for(int i=0;i<tlwuList.size();i++){			
			mgsUUIDArr[i] = tlwuList.get(i).getUuid();
		}
		String[] columnArr = NosqlBeanUtil.getColumns(TalkMessage.class);
		Map<String, Map<String, String>> reMap = CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr);		
		
		for(int i=0;i<tlwuList.size();i++){
			TimeLineWithUid  tlwu = tlwuList.get(i);
			Map<String, String> valueMap = reMap.get(tlwu.getUuid());
			if(valueMap.isEmpty() == false && valueMap.size() > 0 && valueMap.get("uuid") !=null ){
				TalkMessage um = new TalkMessage();
				um = NosqlBeanUtil.map2Bean(um,valueMap);
				umList.add(um);
			}else{//已被删除的消息，显示空				
				TalkMessage um = new TalkMessage();
				um.setTime(Long.valueOf(tlwu.getTimeMillis()));
				um.setBody("");
				um.setUuid(tlwu.getUuid());
				String uid = tlwu.getUid();
				//TODO 有些历史空数据，返回了valueMap含有state=1,来自CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr)
				if(uid == null){
					//消息已经被删除，且没有保存UID的记录清理掉
					MsgTimeLineService.getInstance().deltetTimeLine(key, sn, Long.valueOf(tlwu.getTimeMillis()));
					continue;
				}else{
					um.setS(tlwu.getUid());
				}
				
				um.setD(key);
				umList.add(um);
//				//TODO 删除消息时候，就要确保message和MsgTimeline都删除掉
//				try{
//					if( Long.valueOf(timeline.getTimeMillis()) > 0l){
//						MsgTimeLineService.getInstance().deltetTimeLine(key, sn, Long.valueOf(timeline.getTimeMillis()));
//					}
//				}catch (Exception e) {
//					log.error(timeline.getUuid());
//				}
			}
		}
		return umList;
	}
	/**
	 * 获取 uid 和 fuid 的私信列表 （注意 只适用于type=7）
	 * @param uid 
	 * @param fuid 
	 * @param start
	 * @param limit
	 * @return  List<TalkMessage> 
	 */
	public List<TalkMessage> getTalkMessageListByUid(String uid, String fuid, int start,
			int limit) {
		List<TalkMessage> umList = new ArrayList<TalkMessage>();
		String sn = MsgTimeLineService.reSn(7);
	/*	if(String.valueOf(MsgConst.MSG_USER_TYPE_7).equals(type) == false){
			log.warn("type 必须等于MsgConst.MSG_USER_TYPE_7");
			return umList;
		}
		int typeInt = Integer.valueOf(type);
		String sn = MsgTimeLineService.reSn(typeInt);
		if(StringUtil.isEmpty(sn)){
			log.warn("type=" + type + "不存在对应的SN,请确认！");
			return umList;
		}*/
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime(uid, fuid, 0l, start, limit);
		if(tlList == null || tlList.size() == 0 ){
			return umList;
		}
		List<TimeLineWithUid> tlwuList = new ArrayList<TimeLineWithUid>();
		for(int i=0;i<tlList.size();i++){
			TimeLine tl = tlList.get(i);
			String time = tl.getTimeMillis();
			String[] arr = tl.getUuid().split(TALK_SPLIT_NOSQL);
			if(arr.length == 1){
				TimeLineWithUid  tlwu = new TimeLineWithUid(time,arr[0]);
				tlwuList.add(tlwu);
			}else if(arr.length == 2){
				TimeLineWithUid  tlwu = new TimeLineWithUid(time,arr[0],arr[1]);
				tlwuList.add(tlwu);
			}
		}
		tlList = null;		
		
		String[] mgsUUIDArr = new String[tlwuList.size()];
		for(int i=0;i<tlwuList.size();i++){			
			mgsUUIDArr[i] = tlwuList.get(i).getUuid();
		}
		String[] columnArr = NosqlBeanUtil.getColumns(TalkMessage.class);
		Map<String, Map<String, String>> reMap = CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr);		
		for(int i=0;i<tlwuList.size();i++){
			TimeLineWithUid  tlwu = tlwuList.get(i);
			Map<String, String> valueMap = reMap.get(tlwu.getUuid());
			if(valueMap.isEmpty() == false && valueMap.size() > 0 && valueMap.get("uuid") !=null ){
				TalkMessage um = new TalkMessage();
				um = NosqlBeanUtil.map2Bean(um,valueMap);
				umList.add(um);
			}else{//已被删除的消息，显示空				
				TalkMessage um = new TalkMessage();
				um.setTime(Long.valueOf(tlwu.getTimeMillis()));
				um.setBody("");
				um.setUuid(tlwu.getUuid());
				String uid2 = tlwu.getUid();
				//TODO 有些历史空数据，返回了valueMap含有state=1,来自CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr)
				if(uid2 == null){
					//消息已经被删除，且没有保存UID的记录清理掉
					MsgTimeLineService.getInstance().deltetTimeLine(uid, sn, Long.valueOf(tlwu.getTimeMillis()));
					continue;
				}else{
					um.setS(tlwu.getUid());
				}
				
				um.setD(uid);
				umList.add(um);
//				//TODO 删除消息时候，就要确保message和MsgTimeline都删除掉
//				try{
//					if( Long.valueOf(timeline.getTimeMillis()) > 0l){
//						MsgTimeLineService.getInstance().deltetTimeLine(key, sn, Long.valueOf(timeline.getTimeMillis()));
//					}
//				}catch (Exception e) {
//					log.error(timeline.getUuid());
//				}
			}
		}
		return umList;
	}
	
	/**
	 * 取用户的消息列表
	 *
	 * @param key 用户
	 * @param type 类型MsgConst.MSG_USER_TYPE_0
	 * @return
	 */
	public List<UserMsg> getUserMessageList(String key, String type, int start,
			int limit) {
		List<UserMsg> umList = new ArrayList<UserMsg>();
		int typeInt = Integer.valueOf(type);
		String sn = MsgTimeLineService.reSn(typeInt);
		if(StringUtil.isEmpty(sn)){
			log.warn("type=" + type + "不存在对应的SN,请确认！");
			return umList;
		}
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime(key, sn, 0l, start, limit);
		if(tlList == null || tlList.size() == 0 ){
			return umList;
		}
		String[] mgsUUIDArr = new String[tlList.size()];
		for(int i=0;i<tlList.size();i++){
			mgsUUIDArr[i] = tlList.get(i).getUuid();
		}
		String[] columnArr = NosqlBeanUtil.getColumns(UserMsg.class);
		Map<String, Map<String, String>> reMap = CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr);		
		for(int i=0;i<tlList.size();i++){
			Map<String, String> valueMap = reMap.get(tlList.get(i).getUuid());
			UserMsg um = new UserMsg();
			um = NosqlBeanUtil.map2Bean(um,valueMap);
			umList.add(um);
		}
		return umList;
	}
	public List<Article> getBeanList(String key, SAVE_TABLE type, int start,
			int limit) {
		List<Article> umList = new ArrayList<Article>();
		List<TimeLine> tlList = TimeLineService.getInstance().getTimeLineList(key, type, 0, 10000);
		if(tlList == null || tlList.size() == 0 ){
			return umList;
		}
		String[] mgsUUIDArr = new String[tlList.size()];
		for(int i=0;i<tlList.size();i++){
			mgsUUIDArr[i] = tlList.get(i).getUuid();
		}
		String[] columnArr = NosqlBeanUtil.getColumns(Article.class);
		Map<String, Map<String, String>> reMap = CassandraHectorGateWay.getInstance().get("article", mgsUUIDArr, columnArr);		
		for(int i=0;i<tlList.size();i++){
			Map<String, String> valueMap = reMap.get(tlList.get(i).getUuid());
			Article um = new Article();
			um = NosqlBeanUtil.map2Bean(um,valueMap);
			umList.add(um);
		}
		return umList;
	}
	/**
	 * 取用户的消息列表
	 *
	 * @param key 用户
	 * @param 只适用于type 类型 MsgConst.MSG_USER_TYPE_5  防止用户关注重复加载到缓存
	 * @return
	 */
	public List<UserMsg> getUserMessageListForType5(String key, String type, int start,
			int limit) {
		List<UserMsg> umList = new ArrayList<UserMsg>();
		int typeInt = Integer.valueOf(type);
		String sn = MsgTimeLineService.reSn(typeInt);
		if(StringUtil.isEmpty(sn)){
			log.warn("type=" + type + "不存在对应的SN,请确认！");
			return umList;
		}
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime(key, sn, 0l, start, limit);
		if(tlList == null || tlList.size() == 0 ){
			return umList;
		}
		String[] mgsUUIDArr = new String[tlList.size()];
		for(int i=0;i<tlList.size();i++){
			mgsUUIDArr[i] = tlList.get(i).getUuid();
		}
		//去重用户粉丝消息
		Set<String> sSet = new HashSet<String>();
		String[] columnArr = NosqlBeanUtil.getColumns(UserMsg.class);
		Map<String, Map<String, String>> reMap = CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr);		
		for(int i=0;i<tlList.size();i++){
			Map<String, String> valueMap = reMap.get(tlList.get(i).getUuid());
			UserMsg um = new UserMsg();
			um = NosqlBeanUtil.map2Bean(um,valueMap);
			if(!sSet.contains(um.getS())){
				umList.add(um);
				sSet.add(um.getS());
			}
		}
		return umList;
	}

	/**
	 * 把用户消息置为已读状态
	 * status 1 = 已读
	 * @param key
	 * @param type
	 */
	public void updateUserMessageList(String key, String type, List<UserMsg> uml) {
		//status = 0;//0:未读，1：已读		
		Map<String, Map<String, String>> batchMap = new HashMap<String, Map<String, String>>();
		Map<String,String> map = new HashMap<String,String>();
		for(UserMsg um : uml){
			String uuid = um.getUuid();
			if(StringUtil.isEmpty(uuid) == false && map.containsKey(uuid) == false ){
			//	map.put("uuid", uuid);
//				map.put("status", String.valueOf(um.getStatus()));
				map.put("status", "1");
				batchMap.put(uuid, map);				
			}
		}		
		CassandraHectorGateWay.getInstance().insert(MsgTimeLineService.TABLE_SVAE, batchMap);
	}
	public void updateUserMessageList(String key, String type, int maxSize) {
		//status = 0;//0:未读，1：已读		
		List<UserMsg> uml = NosqlService.getInstance().getUserMessageList(String.valueOf(key), type, 0, maxSize);
		Map<String, Map<String, String>> batchMap = new HashMap<String, Map<String, String>>();
		Map<String,String> map = new HashMap<String,String>();
		for(UserMsg um : uml){
			String uuid = um.getUuid();
			if(StringUtil.isEmpty(uuid) == false && map.containsKey(uuid) == false ){
				//	map.put("uuid", uuid);
//				map.put("status", String.valueOf(um.getStatus()));
				map.put("status", "1");
				batchMap.put(uuid, map);				
			}
		}		
		CassandraHectorGateWay.getInstance().insert(MsgTimeLineService.TABLE_SVAE, batchMap);
	}
	
	/**
	 * 把私信消息置为已读状态
	 *  status 1 = 已读
	 * @param key
	 * @param type
	 */
	public void updateTalkMessageList(String key, String type, List<TalkMessage> tml) {
		//status = 0;//0:未读，1：已读		
		Map<String, Map<String, String>> batchMap = new HashMap<String, Map<String, String>>();
		Map<String,String> map = new HashMap<String,String>();
		for(TalkMessage tm : tml){
			String uuid = tm.getUuid();
			if(StringUtil.isEmpty(uuid) == false && map.containsKey(uuid) == false ){	
				//map.put("uuid", uuid);
//				map.put("status", String.valueOf(tm.getStatus()));
				map.put("status", "1");
				map.put("unreadCount", "0");
				batchMap.put(uuid, map);
			}
		}		
		CassandraHectorGateWay.getInstance().insert(MsgTimeLineService.TABLE_SVAE, batchMap);
	}
	
	/**
	 * @param key
	 * @param type 
	 * @param uml 必需有uuid和timemillis
	 */
	public void deleteUserMessage(String key, String type,List<UserMsg> uml){
		int typeInt = Integer.valueOf(type);
		if(typeInt == MsgConst.MSG_USER_TYPE_7){
			log.warn("请用删除私信接口deleteTalkMessage");
			return ;
			
		}
		String sn = MsgTimeLineService.reSn(typeInt);
		for(UserMsg um : uml){
			String uuid = um.getUuid();
			Long time = um.getTime();	
			
			if(StringUtil.isEmpty(uuid) == false && time != null &&time >0l){
				MsgTimeLineService.getInstance().deltetTimeLine(key, sn, time);
				CassandraHectorGateWay.getInstance().delete(MsgTimeLineService.TABLE_SVAE, uuid);
			}
		}
	}
	
	/**
	 * 删除跟某个用户的私信，只删本人的【私信消息分成2个独立存储在不同位置】
	 * @param key
	 * @param fuid 联系人uid
	 */
	public void deleteTalkMessage(String key, long fuid){
		String sn = MsgTimeLineService.reSn(MsgConst.MSG_USER_TYPE_7);		
		String fuidStr = String.valueOf(fuid);
		//删除talk中的联系人
		List<TimeLine> talkTlList = MsgTimeLineService.getInstance().getTimeLineListByTime(key, sn, 0l,System.currentTimeMillis());
				
		//删除 与该联系人的消息
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime(key,fuidStr, 0l,System.currentTimeMillis());
		if(tlList != null && tlList.size() > 0 ){
			TimeLine firstTL = tlList.get(0);
			for(TimeLine tl :talkTlList){
				long time = Long.valueOf(tl.getTimeMillis());
				String[] arr = tl.getUuid().split(TALK_SPLIT_NOSQL);
				if(arr.length == 1){
					if(arr[0].equals(firstTL.getUuid())){
						MsgTimeLineService.getInstance().deltetTimeLine(key, sn, time);
					}
				}else if(arr.length == 2){
					if(arr[1].equals(fuidStr)){
						MsgTimeLineService.getInstance().deltetTimeLine(key, sn, time);
					}
				}
				
			}
			
	//		MsgTimeLineService.getInstance().deltetTimeLine(key, fuidStr);
			for(TimeLine tl : tlList){
				String uuid = tl.getUuid();
				long time = Long.valueOf(tl.getTimeMillis());
				MsgTimeLineService.getInstance().deltetTimeLine(key, fuidStr, time);
	//			MsgTimeLineService.getInstance().deltetTimeLine(key, sn, time);
				List<TimeLine> tlList2 = MsgTimeLineService.getInstance().getTimeLineListByTime(fuidStr,key, 0l,System.currentTimeMillis());
				if(tlList2==null || tlList2.size()==0){//如果两边的引用都删除了，把实例也删除
					CassandraHectorGateWay.getInstance().delete(MsgTimeLineService.TABLE_SVAE, uuid);
					
				}
			}
		
		}
		
		
		
	}
	
	/**
	 * 删除某条具体消息
	 * @param key
	 * @param fuid
	 * @param tm必须uuid和time字段
	 */
	public void deleteTalkMessage(String key, long fuid,List<TalkMessage> tml ){
//		String sn = MsgTimeLineService.reSn(MsgConst.MSG_USER_TYPE_7);
		String cloumn = "time" ;
		String[] cloumns = {cloumn};
		String fuidStr = String.valueOf(fuid);
		for (TalkMessage tm :tml){
			if(StringUtil.isEmpty(tm.getUuid()) == false && tm.getTime() > 0l){
				String uuid = tm.getUuid();
				Map<String,String> map = CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, uuid,cloumns);
				long time = tm.getTime();
				if(StringUtil.isEmpty(map.get(cloumn))== false){
					time = Long.valueOf(map.get(cloumn));
					MsgTimeLineService.getInstance().deltetTimeLine(key, fuidStr, time);
					//删除聊天消息时【nosql】，不删除联系人
//					MsgTimeLineService.getInstance().deltetTimeLine(key, sn, time);
					//TODO 有待验证
					 TimeLine obj = MsgTimeLineService.getInstance().getTimeLineListByTime(fuidStr,key,time);
					if(obj==null || obj.getUuid()==null){//如果两边的引用都删除了，把实例也删除
						CassandraHectorGateWay.getInstance().delete(MsgTimeLineService.TABLE_SVAE, uuid);
					}
				}
			}
		}
	}

	/**
	 * 取用户的历史聊天记录
	 * 
	 * @param iuid
	 * @param fuid
	 * @param start
	 * @param limit
	 * @return
	 */
	public <T> List<IMessage> getUserHistoryTalkMessageList(Long iuid, long fuid,
			int start, int limit,Class<? extends IMessage> clazz) {
		List<IMessage> imList = new ArrayList<IMessage>();
		String key = String.valueOf(iuid);
		String sn = String.valueOf(fuid);		
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime(key, sn, 0l, start, limit);
		if(tlList == null || tlList.size() == 0 ){
			return imList;
		}
		String[] mgsUUIDArr = new String[tlList.size()];
		for(int i=0;i<tlList.size();i++){
			mgsUUIDArr[i] = tlList.get(i).getUuid();
		}
		String[] columnArr = NosqlBeanUtil.getColumns(TalkMessage.class);
		Map<String, Map<String, String>> reMap = CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr);		
		for(int i=0;i<tlList.size();i++){
			TimeLine timeline = tlList.get(i);
			Map<String, String> valueMap = reMap.get(timeline.getUuid());
			if(valueMap.isEmpty() == false){
				IMessage im = null;
				try {
					im = clazz.newInstance();
				} catch (Exception e) {					
					e.printStackTrace();
				}
				im = NosqlBeanUtil.map2Bean(im,valueMap);
				imList.add(im);
			}else{
				//TODO 删除消息时候，就要确保message和MsgTimeline都删除掉
				try{
					if( Long.valueOf(timeline.getTimeMillis()) > 0l){
						MsgTimeLineService.getInstance().deltetTimeLine(key, sn, Long.valueOf(timeline.getTimeMillis()));
					}
				}catch (Exception e) {
					log.error(timeline.getUuid());
				}
			}
//			IMessage im = null;
//			try {
//				im = clazz.newInstance();
//				im = NosqlBeanUtil.map2Bean(im,valueMap);
//				TalkMessage tm = (TalkMessage)im;
//			   // if(tm.getBody()!=null && tm.getUuid()!=null){
//					imList.add(im);
//				//}
//			}catch (Exception e) {
//				e.printStackTrace();
//			}			
		}
		return imList;
	}
	/**
	 * 取用户的历史聊天记录
	 * 
	 * @param iuid
	 * @param fuid
	 * @param time
	 * @param limit
	 * @return
	 */
	public <T> List<IMessage> getUserHistoryTalkMessageListForPaging(Long iuid, long fuid,int fType,
			long time, int limit,Class<? extends IMessage> clazz) {
		List<IMessage> imList = new ArrayList<IMessage>();
		String key = String.valueOf(iuid);
		String sn = String.valueOf(fuid);	
		Long endTime = System.currentTimeMillis();
		if(time>endTime){
			System.out.println("开始时间大于结束时间 "+(endTime-time));
			time =endTime;
		}
		if(fType==1 && time>0){//查询历史记录
			endTime =time;
			time = 01;
		}
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime( key, sn ,time ,endTime,limit);
		if(tlList == null || tlList.size() == 0 ){
			return imList;
		}
		String[] mgsUUIDArr = new String[tlList.size()];
		for(int i=0;i<tlList.size();i++){
			mgsUUIDArr[i] = tlList.get(i).getUuid();
		}
		String[] columnArr = NosqlBeanUtil.getColumns(TalkMessage.class);
		Map<String, Map<String, String>> reMap = CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr);		
		for(int i=0;i<tlList.size();i++){
			TimeLine timeline = tlList.get(i);
			Map<String, String> valueMap = reMap.get(timeline.getUuid());
			if(valueMap.isEmpty() == false){
				IMessage im = null;
				try {
					im = clazz.newInstance();
				} catch (Exception e) {					
					e.printStackTrace();
				}
				im = NosqlBeanUtil.map2Bean(im,valueMap);
				imList.add(im);
			}else{
				//TODO 删除消息时候，就要确保message和MsgTimeline都删除掉
				try{
					if( Long.valueOf(timeline.getTimeMillis()) > 0l){
						MsgTimeLineService.getInstance().deltetTimeLine(key, sn, Long.valueOf(timeline.getTimeMillis()));
					}
				}catch (Exception e) {
					log.error(timeline.getUuid());
				}
			}
//			Map<String, String> valueMap = reMap.get(tlList.get(i).getUuid());
//			IMessage im = null;
//			try {
//				im = clazz.newInstance();
//				im = NosqlBeanUtil.map2Bean(im,valueMap);
//				TalkMessage tm = (TalkMessage)im;
//				if(im!=null && tm.getBody()!=null && !"null".equals(tm.getBody())){
//					imList.add(im);
//				}
//			}catch (Exception e) {
//				e.printStackTrace();
//			}			
		}
		return imList;
	}

	public List<IMessage> getUserHistoryMessageListByType(Long uid, int type,
			int start, int limit,Class<? extends IMessage> clazz) {
		List<IMessage> imList = new ArrayList<IMessage>();
		String key = String.valueOf(uid);
		int typeInt = Integer.valueOf(type);
		String sn = MsgTimeLineService.reSn(typeInt);
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime(key, sn, 0l, start, limit);
		if(tlList == null || tlList.size() == 0 ){
			return imList;
		}
		String[] mgsUUIDArr = new String[tlList.size()];
		for(int i=0;i<tlList.size();i++){
			mgsUUIDArr[i] = tlList.get(i).getUuid();
		}
		String[] columnArr = NosqlBeanUtil.getColumns(UserMsg.class);
		Map<String, Map<String, String>> reMap = CassandraHectorGateWay.getInstance().get(MsgTimeLineService.TABLE_SVAE, mgsUUIDArr, columnArr);		
		for(int i=0;i<tlList.size();i++){
			TimeLine timeline = tlList.get(i);
			Map<String, String> valueMap = reMap.get(timeline.getUuid());
			if(valueMap.isEmpty() == false){
				IMessage im = null;
				try {
					im = clazz.newInstance();
				} catch (Exception e) {					
					e.printStackTrace();
				}
				im = NosqlBeanUtil.map2Bean(im,valueMap);
				imList.add(im);
			}else{
				//TODO 删除消息时候，就要确保message和MsgTimeline都删除掉
				try{
					if( Long.valueOf(timeline.getTimeMillis()) > 0l){
						MsgTimeLineService.getInstance().deltetTimeLine(key, sn, Long.valueOf(timeline.getTimeMillis()));
					}
				}catch (Exception e) {
					log.error(timeline.getUuid());
				}
			}
//			Map<String, String> valueMap = reMap.get(tlList.get(i).getUuid());
//			IMessage im = null;
//			try {
//				im = clazz.newInstance();
//				im = NosqlBeanUtil.map2Bean(im,valueMap);
//				UserMsg um = (UserMsg)im;
//				if(um!=null && um.getS()!=null && um.getD()!=null){
//					imList.add(im);
//				}
//			}catch (Exception e) {
//				e.printStackTrace();
//			}			
		}
		return imList;
	}
	
	/**
	 * 修正用户扩展数据 article_counts
	 * @param uid
	 */
	public UserExt repairUserExtArticleCounts(long uid){
		String sKey = String.valueOf(uid);		
		String[] columns = NosqlBeanUtil.getColumns(UserExt.class);
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();		
		Map<String,String> getMap = ch.get(SAVE_TABLE.USER_EXT.toString(), sKey, columns);
		UserExt ue = new UserExt();
		NosqlBeanUtil.map2Bean(ue, getMap);	
		long article_counts =  ue.getArticle_counts();
		long articleSize = TimeLineService.getInstance().getCount(sKey, SAVE_TABLE.ARTICLE);
		if(article_counts != articleSize){
			Map<String,String> toNosqlMap = new HashMap<String,String>();
			toNosqlMap.put("article_counts", String.valueOf(articleSize));						
			CassandraHectorGateWay.getInstance().insert(SAVE_TABLE.USER_EXT.toString(), sKey, toNosqlMap);
			ue.setArticle_counts(articleSize);			
		}
		return ue;
	}
	
	/**
	 * 修正用户扩展数据
	 * @param uid
	 */
	public UserExt repairUserExt(long uid){
		String sKey = String.valueOf(uid);		
		String[] columns = NosqlBeanUtil.getColumns(UserExt.class);
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();		
		Map<String,String> getMap = ch.get(SAVE_TABLE.USER_EXT.toString(), sKey, columns);
		UserExt ue = new UserExt();
		NosqlBeanUtil.map2Bean(ue, getMap);	
		long article_counts =  ue.getArticle_counts();
		long follow_counts= ue.getFollow_counts();
		long befollow_counts = ue.getBefollow_counts();
		long articleSize = TimeLineService.getInstance().getCount(sKey, SAVE_TABLE.ARTICLE);
		long flistSize= ch.getSuperCountSize(SAVE_TABLE.FOLLOW.toString(),sKey );
		long bflistSize = ch.getSuperCountSize(SAVE_TABLE.BEFOLLOW.toString(),sKey );
		if(article_counts != articleSize ||	follow_counts != flistSize || befollow_counts != bflistSize ){
			Map<String,String> toNosqlMap = new HashMap<String,String>();
			toNosqlMap.put("article_counts", String.valueOf(articleSize));
			toNosqlMap.put("follow_counts", String.valueOf(flistSize));
			toNosqlMap.put("befollow_counts",String.valueOf(bflistSize));				
			CassandraHectorGateWay.getInstance().insert(SAVE_TABLE.USER_EXT.toString(), sKey, toNosqlMap);
			ue.setArticle_counts(articleSize);
			ue.setFollow_counts(flistSize);
			ue.setBefollow_counts(bflistSize);
		}
		return ue;
	}
	
	/**
	 * 把博文存入nosql
	 * @param article
	 * @return
	 */
	public boolean saveArticle2Nosql(Article article){
		try {
			Map<String, String> map = NosqlBeanUtil.bean2Map(article);
			String key = String.valueOf(article.getUid());
			long timemillis = article.getTime();
			tls.saveTimeLine(key, article.getUuid(),
					TimeLineService.SAVE_TABLE.ARTICLE, timemillis);
			ch.insert(SAVE_TABLE.ARTICLE.toString(), article.getUuid(), map);
		} catch (Exception e) {
			log.info("博文存入nosql 失败" + e);
			return false;
		}
		return true;
	}
	
	/**
	 * 把推荐博文存入(关系) nosql
	 * @param article
	 * @return
	 */
	public boolean relationship2Nosql(SimpleArticle article){
		try {
			String uid = String.valueOf(article.get_attr().get("tuid"));
			long timemillis = Long.parseLong(article.get_attr().get("time").toString());
			tls.saveTimeLine(uid, article.getUuid(),
					TimeLineService.SAVE_TABLE.ARTICLE, timemillis);
		} catch (Exception e) {
			log.info("博文存入nosql 失败" + e);
			return false;
		}
		return true;
	}
	
	/**
	 * 把推荐博文存入(实体)nosql
	 * 注意：子map需要从NOSQL数据库中查出来处理后才能不遗漏数据
	 * @param article
	 * @return
	 */
	public boolean recommendArticle2Nosql(SimpleArticle article){
		try {
			Map<String, Serializable> attr = article.get_attr();
			//attr字段容易覆盖出问题,因为这个子MAP成为一个字段
			Map<String, String> mapOld = ch.get(SAVE_TABLE.ARTICLE.toString(), article.getUuid());
			if(mapOld.isEmpty() == false){
				Article articleOld = new Article();
				articleOld = NosqlBeanUtil.map2Bean(articleOld, mapOld);				
				Map<String, Serializable> attrOld = articleOld.get_attr();
				if(attrOld.isEmpty() == false){
					//遍历已有的数据，如果新的没有，则加入，别丢失了
					for (Map.Entry<String, Serializable> entry : attrOld.entrySet()) {
						 if(attr.containsKey(entry.getKey()) == false){
							 attr.put(entry.getKey(), entry.getValue());
						 }
					}
				}
			}
			Map<String, String> map = NosqlBeanUtil.bean2Map(article);
			ch.insert(SAVE_TABLE.ARTICLE.toString(),article.getUuid(), map);
		} catch (Exception e) {
			log.info("博文存入nosql 失败" + e);
			return false;
		}
		return true;
	}
	
	public boolean updateArticle2Nosql(Article article){
		try {
			Map<String, Serializable> attr = article.get_attr();
			//attr字段容易覆盖出问题,因为这个子MAP成为一个字段
			Map<String, String> mapOld = ch.get(SAVE_TABLE.ARTICLE.toString(), article.getUuid());
			if(mapOld.isEmpty() == false){
				Article articleOld = new Article();
				articleOld = NosqlBeanUtil.map2Bean(articleOld, mapOld);				
				Map<String, Serializable> attrOld = articleOld.get_attr();
				if(attrOld.isEmpty() == false){
					//遍历已有的数据，如果新的没有，则加入，别丢失了
					for (Map.Entry<String, Serializable> entry : attrOld.entrySet()) {
						 if(attr.containsKey(entry.getKey()) == false){
							 attr.put(entry.getKey(), entry.getValue());
						 }
					}
				}
			}
			Map<String, String> map = NosqlBeanUtil.bean2Map(article);
			ch.insert(SAVE_TABLE.ARTICLE.toString(),article.getUuid(), map);
		} catch (Exception e) {
			log.info("博文存入nosql 失败" + e);
			return false;
		}
		return true;
	}
}
