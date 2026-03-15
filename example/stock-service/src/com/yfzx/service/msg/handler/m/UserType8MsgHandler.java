package com.yfzx.service.msg.handler.m;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.AAAConstants;
import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.USubject;
import com.stock.common.model.share.UserExt;
import com.stock.common.model.snn.EConst;
import com.stock.common.model.user.StockSeq;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.CommonUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.nosql.NosqlService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yfzx.service.util.ServiceUtil;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.event.IEvent;
import com.yz.mycore.msg.handler.IHandler;

/**
 * 用户登陆消息处理器
 * 
 * @author：杨真
 * @date：2014-8-14
 */
public class UserType8MsgHandler implements IHandler {

	int serial = -1;
	static Logger logger = LoggerFactory.getLogger(UserType8MsgHandler.class);
	
	
	@Override
	public void handle(Object o) {
		if (o == null)
			return;
		NotifyEvent e = (NotifyEvent) o;
		UserMsg oum = (UserMsg) e.getMsg();
		long uid =  0l;
		try{
			uid = Long.valueOf(oum.getD());
		}catch(Exception exception){
			logger.error("UserMsg对象的D不是用户UID，oum.getD()="+ oum.getD());
			return ;
		}
		
		String loginIP = oum.getAttr("ipport");
		String loginkey = uid+"lg";
		//是否强制登陆，刷新用户缓存
		String isLogin = oum.getAttr("isLogin");
		if("true".equals(isLogin) == false){
			isLogin = "false";
		}
		//登录刷新用户缓存间隔时间
		long LOGIN_INTERVAL_TIME = ConfigCenterFactory.getLong("dcss.user_login_interval_time",1000*60*60*24L);
		
		//记录下我在本机登陆
		MicorBlogService.getInstance().loginRecord(uid,loginIP);
		Long loginTime = LCEnter.getInstance().get(uid, SCache.CACHE_NAME_userlogincache);//本次登录时间
		Long lastLoginTime = LCEnter.getInstance().get(loginkey, SCache.CACHE_NAME_userlogincache);//上次登录时间
//		logger.error(loginTime + "=" + lastLoginTime);
		if(lastLoginTime==null){
			lastLoginTime = 0l;
		}
		if(loginTime!=null && loginTime>0 ){
			long time = loginTime - lastLoginTime;
			LCEnter.getInstance().put(loginkey,loginTime,SCache.CACHE_NAME_userlogincache);
			if("true".equals(isLogin) == false){//不是强制刷新，需要检查用户是否失效，如果失效，会刷新用户缓存
				if(time < LOGIN_INTERVAL_TIME){
					//logger.info("用户["+uid+"] 两次登录时间间隔小于"+LOGIN_INTERVAL_TIME+"毫秒，不用初始化用户数据!");
					return;
				}
			}else{
				//防止同一个用户触发了多次登陆操作 3秒作为一个保护方式
				if(time < 3000l){
					return ;
				}
			}
		}
		long  maxFansNum = ConfigCenterFactory.getLong("dcss.max_fans_num", 500L);
		logger.info("配置的粉丝数阀值 "+maxFansNum);
//		long activityTime = ConfigCenterFactory.getLong("dcss.user_activity_time", 1000*60*60*24L);
		logger.info("用户uid="+uid+"登录"+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS));
		int loginLog = ConfigCenterFactory.getInt("dcss.loginLog", 0);
		if(loginLog == 1){
			long currentTimeMillis = System.currentTimeMillis();
			//加载我的订阅
			MicorBlogService.getInstance().initMyIndexWapper(uid);
			logger.info("initMyIndexWapper="+(System.currentTimeMillis() - currentTimeMillis) + "毫秒" );
			//加载用户未读消息
			MicorBlogService.getInstance().initUserUnReadMessage(uid);
			logger.info("initUserUnReadMessage="+(System.currentTimeMillis() - currentTimeMillis) + "毫秒" );
			//初始化登陆用户的时间线
//			MicorBlogService.getInstance().initUserFavoriteWapper(uid);
//			logger.info("initUserFavoriteWapper="+(System.currentTimeMillis() - currentTimeMillis) + "毫秒" );
		}else{
			//加载我的订阅
			MicorBlogService.getInstance().initMyIndexWapper(uid);			
			//加载用户未读消息
			MicorBlogService.getInstance().initUserUnReadMessage(uid);
			//初始化登陆用户的时间线 我的空间屏蔽了，所以它也屏蔽
			//MicorBlogService.getInstance().initUserFavoriteWapper(uid);
		}
		
		//我的关注处理，复杂流程在notifyFollowLogin
		List<Long> ll = MicorBlogService.getInstance().getFollowUidList(
				Long.valueOf(oum.getD()));
		ll.remove(uid);//先移除自己 
		for (Long followUid : ll) {
			MicorBlogService.getInstance().notifyFollowLogin(Long.valueOf(oum.getS()),followUid);
		}
		
		//我的粉丝
		String key = "active_" + uid;
		String cachename = StockUtil.getUserCacheName(key);
		Map<Long, Long> activeUserMap = LCEnter.getInstance().get(key,cachename);
		//检查本机上是否已经建立了关系
		if(activeUserMap == null || activeUserMap.size()==0){
			int fansNum = CassandraHectorGateWay.getInstance().getCountSize(SAVE_TABLE.BEFOLLOW.toString(), oum.getD());
			// 粉丝数小于配置的值
			if(fansNum<maxFansNum){
				List<Long> befollowUidList = MicorBlogService.getInstance().getBefollowUidList(Long.valueOf(oum.getD()));
				befollowUidList.remove(uid);
				Map<Integer, IEvent> me = groupByUid(oum, befollowUidList);
				Iterator<Integer> iter = me.keySet().iterator();
				while (iter.hasNext()) {
					Integer index = iter.next();
					IEvent ie = me.get(index);				
					ClientEventCenter.getInstance().putEvent(ie);
				}
			}
		}
		ll = null;
		//我关注的自选股,DCSS启动时，已经加载好自选股的用户，需要告诉公司我在线
		List<Long> ll_usubjectUid = new ArrayList<Long>();
//		String cacheName = getThisCacheName(AAAConstants.MYSTOCK_BASE_CACHENAME,String.valueOf(uid));	
//		List<UserStock> stockList = LCEnter.getInstance().get(String.valueOf(uid), cacheName);
		StockSeq stockSeq = LCEnter.getInstance().get(uid, StockUtil.getStockSeqCacheName(uid));
		if(stockSeq != null && StringUtils.isNotBlank(stockSeq.getCodesSeq())){
			
			for(String stockcode : stockSeq.getCodesSeq().split(",") ){
				USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(stockcode);
				if(usubject==null){
					continue;
				}
				long companyUid = usubject.getUid();
				if( companyUid > 0l ){
					MicorBlogService.getInstance().notifyUsubjectAdd(uid, companyUid);
				}
			}
//			if (ll_usubjectUid != null) {
//				MicorBlogService.getInstance().notifyUsubjectAddRemote(uid, ll_usubjectUid);
//			}
		}
//		if (ll_usubjectUid != null) {
//			// 由于每个用户的自选股[默认账户]分组
//			Map<Integer, IEvent> me = groupByUid(oum, ll_usubjectUid);
//			Iterator<Integer> iter = me.keySet().iterator();
//			while (iter.hasNext()) {
//				Integer index = iter.next();
//				IEvent ie = me.get(index);				
//				ClientEventCenter.getInstance().putEvent(ie);
//			}
//		}
//		ll_usubjectUid = null;
		
//		//TODO 我需要查找哪些粉丝已经登录,生成自己本身的活跃用户数据
//		List<Long> fsList = MicorBlogService.getInstance().getBefollowUidList(Long.valueOf(oum.getD()));//.getBefollowListOnlineLocal(Long.valueOf(oum.getD()));
//		if (fsList != null) {
//			for(long fsUid : fsList){
//				//登录消息建议是广播消息，这样任何一台都有全量的userlogincache
//				MicorBlogService.getInstance().putFeisiIsLogin(Long.valueOf(oum.getD()), fsUid);		
//			}
//		}	
		
		//UserServiceImplgetUserExtByUid
		//登录时，补充UserInfoCacheLoadService的不足,校正一些博文数，粉丝数，关注数等信息
		UserExt ue = NosqlService.getInstance().repairUserExt(uid);
		String cacheName_2 = getThisCacheName(AAAConstants.USEREXT_BASE_CACHENAME,oum.getD());
		LCEnter.getInstance().put(oum.getD(), ue, cacheName_2);

//		Object obj = LCEnter.getInstance().get(oum.getD(), cacheName_2);
//		if(obj == null){
//			String[] columns = NosqlBeanUtil.getColumns(UserExt.class);
//			CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
//			String sKey = String.valueOf(uid);
//			Map<String,String> getMap = ch.get(SAVE_TABLE.USER_EXT.toString(), sKey, columns);
//			UserExt ue = new UserExt();
//			NosqlBeanUtil.map2Bean(ue, getMap);	
//			long article_counts =  ue.getArticle_counts();
//			long follow_counts= ue.getFollow_counts();
//			long befollow_counts = ue.getBefollow_counts();
//			long articleSize = TimeLineService.getInstance().getCount(sKey, SAVE_TABLE.ARTICLE);
//			long flistSize= ch.getSuperCountSize(SAVE_TABLE.FOLLOW.toString(),sKey );
//			long bflistSize = ch.getSuperCountSize(SAVE_TABLE.BEFOLLOW.toString(),sKey );
//			if(article_counts != articleSize ||	follow_counts != flistSize || befollow_counts != bflistSize ){
//				Map<String,String> toNosqlMap = new HashMap<String,String>();
//				toNosqlMap.put("article_counts", String.valueOf(articleSize));
//				toNosqlMap.put("follow_counts", String.valueOf(flistSize));
//				toNosqlMap.put("befollow_counts",String.valueOf(bflistSize));				
//				CassandraHectorGateWay.getInstance().insert(SAVE_TABLE.USER_EXT.toString(), sKey, toNosqlMap);
//				ue.setArticle_counts(articleSize);
//				ue.setFollow_counts(flistSize);
//				ue.setBefollow_counts(bflistSize);
//				LCEnter.getInstance().put(oum.getD(), ue, cacheName_2);
//			}
//		}
	}
	
	
//	private String getThisCacheName(String baseCacheName,String key){
//		//如2个DCSS 里面各2个cache D0到D7数据  S0_C0 包含数据D0,D4;S1_C0包含D1,D5;S0_C1包含D2,D6;S1_C1包含D3,D7
//		//如D7在 7%2=S1 和7/2%2=C1 ，D6在6%2=S0 6/2%2=C1中 
//		//int cacheIndex = StockUtil.getUserIndex(key)/dcssNums %AAAConstants.AAA_CACHE_NUMS;
//		int cacheIndex = StockUtil.getUserIndex(key) % AAAConstants.AAA_CACHE_NUMS;
//		String cacheName = CacheUtil.getCacheName(baseCacheName)+"_"+cacheIndex;
//		return cacheName;
//	}	


	// 每个组只生成一个消息事件
	private Map<Integer, IEvent> groupByUid(UserMsg oum, List<Long> sul) {
		Map<Integer, IEvent> mlu = new HashMap<Integer, IEvent>();		
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_ID_LIST, "dcss01");

		String[] ips = sips.split("\\^");
		int dcssNums = ips.length;
		for (Long uid : sul) {
			// NotifyEvent ue =(NotifyEvent) e;
			int index = StockUtil.getUserIndex(String.valueOf(uid)) % dcssNums;
			if(serial==-1)
			{
				serial = ServiceUtil.getSerial(sips);
			}
			//不在本机上的，要重新发出去
			if(serial!=index)
			{
				IEvent ne = mlu.get(index);
				if (ne == null) {
					ne = new NotifyEvent();
					ne.setHType(EConst.EVENT_3);
					UserMsg um = oum.clone();
					um.setMsgType(MsgConst.MSG_USER_TYPE_12);
					um.setS(oum.getD());
					// 加上路由seed
					um.setD(String.valueOf(uid));
					StringBuffer sb = new StringBuffer();
					// 加上订阅者串
					sb.append(uid);
					sb.append(",");
					um.putAttr("dyz", sb);// 订阅者
					um.putAttr("stype", oum.getMsgType());// 原始的消息类型
					((NotifyEvent) ne).setMsg(um);
					mlu.put(index, ne);
				} else {
					NotifyEvent nne = (NotifyEvent) ne;
					UserMsg um = (UserMsg) nne.getMsg();					
					StringBuffer sb = (StringBuffer) um.getAttr("dyz");
					sb.append(uid);
					sb.append(",");					
				}
			}
			else
			{
				//分布在本机的关注组  建立登录者和他的粉丝关系
				MicorBlogService.getInstance().putFeisiIsLogin(Long.valueOf(oum.getS()),Long.valueOf(uid));
			}
		}
		//可能存在某个机器没有本人的关注用户,也先推一条消息过去，方便以后"查找哪些粉丝已经登录"
//		for(int index=0;index<dcssNums;index++){
//			IEvent ne = mlu.get(index);
//			if(ne == null){
//				ne = new NotifyEvent();
//				ne.setHType(EConst.EVENT_3);
//				UserMsg um = oum.clone();
//				um.setMsgType(MsgConst.MSG_USER_TYPE_12);
//				um.setS(oum.getD());
//				mlu.put(index, ne);
//			}
//		}

		return mlu;
	}
	
	private String getThisCacheName(String baseCacheName,String key){	
		int cacheIndex = StockUtil.getUserIndex(key) %AAAConstants.AAA_CACHE_NUMS;
		String cacheName = CacheUtil.getCacheName(baseCacheName)+"_"+cacheIndex;
		return cacheName;		
	}
}
