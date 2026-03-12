package com.yfzx.service.stockgame;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.stock.common.model.share.ShortUser;
import com.stock.common.util.CassandraHectorGateWay;
import com.yfzx.service.msg.TalkMessageService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.configcenter.ConfigCenterFactory;

public class StockGameFollowService {

	private static StockGameFollowService instance = new StockGameFollowService();

	public static StockGameFollowService getInstance() {
		return instance;
	}

	public boolean follow(String stockGameUid,String uid,boolean addOrDel){
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
		Map<String,String> followMap = new HashMap<String,String>();
		Map<String,String> beFollowMap = new HashMap<String,String>();
		String now = String.valueOf(new Date().getTime());
		followMap.put(stockGameUid,now);
		beFollowMap.put(uid,now);
		//status=1;
		if(addOrDel){//插入
			ch.insert(SAVE_TABLE.TRADE_FOLLOW.toString(), uid, followMap);
			ch.insert(SAVE_TABLE.TRADE_BEFOLLOW.toString(), stockGameUid, beFollowMap);
		}else if(addOrDel==false){//删除
			ch.deleteName(SAVE_TABLE.TRADE_FOLLOW.toString(), uid,stockGameUid);
			ch.deleteName(SAVE_TABLE.TRADE_BEFOLLOW.toString(), stockGameUid,uid);
		}
		return true;
	}

	/**
	 * 我关注了哪些模拟炒股
	 * @param uid
	 * @return
	 */
	public List<String> getMyFollowGame(String uid){
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
		Map<String,String> map = ch.get(SAVE_TABLE.TRADE_FOLLOW.toString(), uid);
		Set<String> set = map.keySet();
		List<String> followGameList = new ArrayList<String>();
		for(String gameUid : set){
			followGameList.add(gameUid);
		}
		return followGameList;
	}

	/**
	 * 查看关注模拟炒股的用户组【数据量大的话，使用分页取】
	 * @param gameUid
	 * @return
	 */
	public List<String> getGameFollow(String gameUid){
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
		Map<String,String> map = ch.get(SAVE_TABLE.TRADE_BEFOLLOW.toString(), gameUid);
		Set<String> set = map.keySet();
		List<String> uidList = new ArrayList<String>();
		for(String uid : set){
			uidList.add(uid);
		}
		return uidList;
	}

	public Integer getGameFollowCount(String gameUid){
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
		int count = ch.getCountSize(SAVE_TABLE.TRADE_BEFOLLOW.toString(), gameUid);
//		Map<String,String> map = ch.get(SAVE_TABLE.TRADE_BEFOLLOW.toString(), gameUid);
//		Set<String> set = map.keySet();
//		return set.size();
		return count;
	}

	public void sendStockEquityChangeMsg(Long uid, String content,Map<String,Serializable> headerMap) {
		Long sscUid = ConfigCenterFactory.getLong("stock_zjs.invest_secretaire", 0L);
		if(sscUid == 0) {
			return;
		}
//		List<String> subscribeList = StockGameFollowService.getInstance().getGameFollow(uid.toString());
//		if(subscribeList != null && subscribeList.size() > 0) {
//			for(String ssUid : subscribeList) {
//				//UserEventService.getInstance().singleCastTalkMessage(sscUid, Long.valueOf(ssUid), content, null);
//				TalkMessageService.getInstance().singlecastTalkMessage(sscUid,  Long.valueOf(ssUid), content, 6, headerMap);
//			}
//		}		
//		List<ShortUser> subscribeList = MicorBlogService.getInstance().getBefollowList(uid);
		//合并消息，作为广播消息发送
		TalkMessageService.getInstance().broadcastTalkMessageWithGame(sscUid,uid, content, 6, headerMap);
		
	}
}
