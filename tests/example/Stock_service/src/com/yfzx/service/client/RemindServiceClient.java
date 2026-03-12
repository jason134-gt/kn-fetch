package com.yfzx.service.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.stock.common.constants.AAAConstants;
import com.stock.common.constants.ShareConst;
import com.stock.common.model.Topic;
import com.stock.common.model.USubject;
import com.stock.common.model.chance.StockChanceEntity;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.model.share.Comment;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.model.weixin.WeiXinAuthModel;
import com.stock.common.msg.BaseMsg;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.msg.UserMsg;
import com.stock.common.service.IRemindService;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.Pair;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yfzx.yas.YASFactory;
import com.yfzx.yas.router.ISelectRouter;
import com.yfzx.yas.router.RouterCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.msg.message.IMessage;

/**
 * 提醒服务客户端
 *
 * @author：杨真
 * @date：2014-6-2
 */
public class RemindServiceClient implements IRemindService{

	static Map<String,Dbrouter> _routerPool = new ConcurrentHashMap<String,Dbrouter>();
	static Logger logger = LoggerFactory.getLogger(RouterCenter.class);
	private static RemindServiceClient instance = new RemindServiceClient();
	static String iserviceName = "IRemindService";
	public RemindServiceClient() {

	}

	public static RemindServiceClient getInstance() {
		return instance;
	}
	static
	{
		initRouter();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			public void refresh() {
				initRouter();

			}

		});
	}
	private static void reinitRouter()
	{
		List<Dbrouter> rl = RouterCenter.getInstance().getRouter(
				iserviceName);
		if(rl!=null&&_routerPool.keySet().size()!=rl.size())
			initRouter();
	}
	private static void initRouter() {
		try {
			// TODO Auto-generated method stub
			List<Dbrouter> rl = RouterCenter.getInstance().getRouter(
					iserviceName);
			if(rl==null)
			{
				logger.error("not found  roueter !");
				return ;
			}
			for (Dbrouter dr : rl) {
				_routerPool.put(getKey(dr.getSip(), dr.getSport()), dr);
			}
		} catch (Exception e) {
			logger.error("init roueter failed!",e);
		}
	}
	private static String getKey(String sip, int sport) {
		// TODO Auto-generated method stub
		return sip+"^"+sport;
	}
	IRemindService getService()
	{
		reinitRouter();
		IRemindService irs = YASFactory.getService(IRemindService.class,new ISelectRouter(){

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "SR_"+iserviceName;
			}
			//根据缓存名来路由，不同的缓存可能放在不同的服务器上
			public Dbrouter selectRouter(String methodName, Object[] args) {
				Dbrouter dr = RemindServiceClient.selectRouter(methodName,args);
				if(dr == null){
					logger.error("select router failed!");
				}
				return dr;
			}
		});
		if(irs == null){
			logger.error("select router failed!");
		}
		return irs;
	}

	private static Dbrouter selectRouter(String methodName, Object[] args) {
		Dbrouter dr = null;
		try {
			if(_routerPool.keySet().size()==0) return null;
			//k格式：000002.sz
			String k = args[0].toString();
			if (StringUtil.isEmpty(k))
				return null;
			String[] ka = k.toString().split("\\^");
			//路由key,以公司编码路由，这样同一公司的数据只会存储在一个缓存服务器上
			String rk = ka[0];

			//获取提供这个服务又哪些服务器 192.168.1.102:8855^192.168.1.103:8855^192.168.1.104:8855
			String sips = ConfigCenterFactory.getString(
					AAAConstants.SERVER_IP_LIST,
					"192.168.1.103:7777");

			String[] ips = sips.split("\\^");
			int dcssNums = ips.length;
			//根据用户uid或者username求余 得到
			//不建议客户端感知后端使用每台DCSS有多少个cache
			int index = StockUtil.getUserIndex(rk)%dcssNums; //StockUtil.getUExtTableIndex(rk)%ips.length;
			String selectIp =  ips[index];
			String ip = selectIp.split(":")[0];
			int port = Integer.valueOf(selectIp.split(":")[1]);
			dr = _routerPool.get(getKey(ip, port));
		} catch (Exception e) {
			logger.error("select router failed!",e);
		}
		if(dr == null){
			logger.error("select router failed!");
		}
		return dr;
	}

	/**
	 * 不返回结果的异步调用
	 */
	IRemindService getAsynService()
	{
		reinitRouter();
		return YASFactory.getAsynService(IRemindService.class,new ISelectRouter(){

			@Override
			public String getName() {
				return "SR_"+iserviceName;
			}
			//根据缓存名来路由，不同的缓存可能放在不同的服务器上
			public Dbrouter selectRouter(String methodName, Object[] args) {
				return RemindServiceClient.selectRouter(methodName,args);
			}
		});
	}

	@Override
	public boolean clearMessage(Long uid, Integer type) {
		return getService().clearMessage(uid, type);
	}

	@Override
	public boolean clearMessage(Long uid, Integer type, String uuid) {
		return  getService().clearMessage(uid, type, uuid);
	}

	@Override
	public boolean clearTalkMessage(Long iuid, Long fuid) {
		return getService().clearTalkMessage(iuid, fuid);
	}

	@Override
	public boolean clearTalkMessage(Long iuid, Long fuid, String uuid) {
		return getService().clearTalkMessage(iuid,fuid, uuid);
	}

	@Override
	public boolean readMessage(Long uid, Integer type) {
		return getService().readMessage(uid, type);
	}

	@Override
	public boolean readTalkMessage(Long iuid, Long duid) {
		return getService().readTalkMessage(iuid, duid);
	}

	@Override
	public List<Pair<Integer, Integer>> getUserUnReadMessageCountList(Long uid,List<Integer> ltype) {
		// TODO Auto-generated method stub
		return getService().getUserUnReadMessageCountList(uid,ltype);
	}

	@Override
	public int getUserNewBlogMessageCount(Long uid,long time) {
		// TODO Auto-generated method stub
		return getService().getUserNewBlogMessageCount(uid,time);
	}

	@Override
	public List<SimpleArticle> getNextUserFollowFavoriteList(long uid, long time,
			int type, int limit) {
		// TODO Auto-generated method stub
		return getService().getNextUserFollowFavoriteList(uid, time, type, limit);
	}

	@Override
	public List<Map<String, Object>> getNextUserFollowStockChanceList(long uid, long time,
			int type, int limit,int climit) {
		// TODO Auto-generated method stub
		return getService().getNextUserFollowStockChanceList(uid, time, type, limit,climit);
	}

	@Override
	public List<SimpleArticle> getNextUsubjectFollowFavoriteList(String usubjectid,
			long time, int type, int limit) {
		// TODO Auto-generated method stub
		return getService().getNextUsubjectFollowFavoriteList(usubjectid, time, type, limit);
	}

	@Override
	public List<TalkMessage> getNextUserTalkMessageList(Long uid,int start,int limit) {
		// TODO Auto-generated method stub
		return getService().getNextUserTalkMessageList(uid,start,limit);
	}

	@Override
	public int getUsubjectNewBlogMessageCount(String usubjectid,long time) {
		// TODO Auto-generated method stub
		return getService().getUsubjectNewBlogMessageCount(usubjectid,time);
	}

	@Override
	public List<IMessage> getUserNewTalkMessageList(Long iuid, Long fuid,
			int start, int limit) {
		// TODO Auto-generated method stub
		return getService().getUserNewTalkMessageList(iuid, fuid, start, limit);
	}

	@Override
	public List<IMessage> getUserHistoryTalkMessageList(Long iuid, Long fuid,
			int start, int limit) {
		// TODO Auto-generated method stub
		return getService().getUserHistoryTalkMessageList(iuid, fuid, start, limit);
	}

	@Override
	public List<IMessage> getUserHistoryMessageListByType(Long uid, int type,
			int start, int limit) {
		return getService().getUserHistoryMessageListByType(uid, type, start, limit);
	}
	@Override
	public List<IMessage> getUserHistoryMessageListByTypeForPaging(Long uid, int type,
			int start, int limit) {
		return getService().getUserHistoryMessageListByTypeForPaging(uid, type, start, limit);
	}

	@Override
	public int getMessageSize(Long uid, Integer type) {
		return getService().getMessageSize(uid, type);
	}

	@Override
	public int getTalkMessageSize(Long iuid, Long duid) {
		return getService().getTalkMessageSize(iuid, duid);
	}

	@Override
	public int getUnReadTalkMessageSize(Long iuid, Long fuid) {
		 return getService().getUnReadTalkMessageSize(iuid, fuid);
	}

	@Override
	public List<IMessage> getUserNewTalkMessageListForPaging(Long iuid,
			Long fuid, int fType,long time, int limit) {
		return getService().getUserNewTalkMessageListForPaging(iuid,fuid,fType,time,limit);
	}

	@Override
	public boolean writeUSubjectTalkMessage(Long uid,Map<String,TalkMessage> tmMap, List<Long> uidArr) {
		return getService().writeUSubjectTalkMessage(uid,tmMap, uidArr);
	}

	@Override
	public void putMobileAttr(Long uid ,Map<String, Serializable> attr) {
		getService().putMobileAttr(uid,attr);

	}

	@Override
	public List<TalkMessage> getTalkMessageList(Long uid,int ftype, long stime, int limit) {
		// TODO Auto-generated method stub
		return getService().getTalkMessageList(uid,ftype, stime, limit);
	}

	@Override
	public List<TalkMessage> getTalkMessageList(Long uid,long etime, int maxcount) {
		// TODO Auto-generated method stub
		return getService().getTalkMessageList(uid,etime, maxcount);
	}

	@Override
	public List<SimpleArticle> getSimpleArticleList(String uuid,List<String> uuidList) {
		List<SimpleArticle> saList = getSimpleArticleListMapReduce(uuid, uuidList);
		Set<String> zhuanfa_uuidSet = Sets.newHashSet();
		Map<String,SimpleArticle> map = Maps.newHashMap();
		if(saList!=null){
			for(SimpleArticle sa : saList){
				String suuid =  sa.getSuuid();//原文的UUID
				if(sa.getType()==ShareConst.ZHUANG_ZAI && suuid!=null){//组装转发博文
					zhuanfa_uuidSet.add(suuid);
				}
			}
			List<String> zhuanfa_uuidList = Lists.newArrayList(zhuanfa_uuidSet);
			if(zhuanfa_uuidList.size()>0){
				List<SimpleArticle> zf_saList = getSimpleArticleListMapReduce(zhuanfa_uuidList.get(0), zhuanfa_uuidList);
				if(zf_saList!=null){//在缓存查
					for(SimpleArticle sa : zf_saList){
						map.put(sa.getUuid(), sa);
					}
					for(SimpleArticle sa : saList){
						String suuid =  sa.getSuuid();//原文的UUID
						if(sa.getType()==ShareConst.ZHUANG_ZAI && suuid!=null && map.size()>0){//组装转发博文
							SimpleArticle retweet_article = map.get(sa.getSuuid());
							sa.setRetweet_article(retweet_article);
						}
					}
				}else{//缓存没有在nosql查
					CassandraHectorGateWay ch =	CassandraHectorGateWay.getInstance();
					Set<String> columnsSet = ch.getColumnsSet(SimpleArticle.class);
					columnsSet.remove("serialVersionUID");
					String[] columns = {};
					columns = columnsSet.toArray(columns);
					if (zhuanfa_uuidSet.size() > 0) {
						String[] keys_zf = new String[zhuanfa_uuidSet.size()];
						keys_zf = zhuanfa_uuidSet.toArray(keys_zf);
						Map<String, Map<String, String>> batchMap_zf = ch.get(
								SAVE_TABLE.ARTICLE.toString(), keys_zf, columns);
						for (SimpleArticle article : saList) {
							if (ShareConst.ZHUANG_ZAI == article.getType()) {
								Map<String, String> aMap = batchMap_zf.get(article
										.getSuuid());
								SimpleArticle article_zf = new SimpleArticle();
								NosqlBeanUtil.map2Bean(article_zf, aMap);
								article.setRetweet_article(article_zf);
							}
						}
					}

				}

			}
		}


		return saList;
	}

	/**
	 * 并发取  需要取到结果的异步方式【内部还是使用getService()方式,只是使用了ExecutorService.submit的返回值Future 】
	 * @param uuid
	 * @param uuidList
	 * @return
	 */
	public List<SimpleArticle> getSimpleArticleListMapReduce(String uuid,
			List<String> uuidList) {
		List<SimpleArticle> saList = new ArrayList<SimpleArticle>();
		Map<String,SimpleArticle> retMap = new HashMap<String,SimpleArticle>();
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_IP_LIST,
				"192.168.1.103:7777");
		String[] ips = sips.split("\\^");


		Map<Integer,List<String>> listMap = new HashMap<Integer,List<String>>();
		for(int i=0;i<uuidList.size();i++){
			String saUuid = uuidList.get(i);
			int index = Math.abs(saUuid.hashCode())%ips.length;
			if( listMap.containsKey(index) == false ){
				listMap.put(index, new ArrayList<String>());
			}
			List<String> tmp = listMap.get(index);
			tmp.add(uuidList.get(i));
		}

		Set<Future<Map<String,SimpleArticle>>> fset = new HashSet<Future<Map<String,SimpleArticle>>>();
		/*
		 * 异步分发
		 */
		Iterator<Entry<Integer,List<String>>> iter = listMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, List<String>> entry = iter.next();
			List<String> tmp = entry.getValue();
			if(tmp!= null && tmp.size() > 0){
				String seed = tmp.get(0);
				List<String> msg = tmp;
				Future<Map<String,SimpleArticle>> f = getSimpleArticleMapAsyn(seed,msg);
				if(f!=null)
					fset.add(f);
			}
		}
		/*
		 * 统一取
		 */
		Iterator<Future<Map<String,SimpleArticle>>> fiter = fset.iterator();
		while (fiter.hasNext()) {
			 Future<Map<String,SimpleArticle>> entry = fiter.next();
			 try {
				Map<String, SimpleArticle> rm = entry.get(30000,
						TimeUnit.MILLISECONDS);
				if (rm != null)
					retMap.putAll(rm);
			} catch (Exception e) {
				logger.error("getSimpleArticleListMapReduce failed!",e);
			}
		}

		/**
		 * 合并结果
		 */
		for(int i=0;i<uuidList.size();i++){
			String saUuid = uuidList.get(i);
			SimpleArticle sa = retMap.get(saUuid);
			if(sa!=null)
				saList.add(sa);
		}

		return saList;//getService().getSimpleArticleList(uuid,uuidList);
	}

	/**
	 * 并发取  需要取到结果的异步方式【内部还是使用getService()方式,只是使用了ExecutorService.submit的返回值Future 】
	 * @param uuid
	 * @param uuidList
	 * @return
	 */
	public Map<String,SimpleArticle> getSimpleArticleListByMsgMapReduce(String uuid,
			List<IMessage> uuidList) {
		Map<String,SimpleArticle> retMap = new HashMap<String,SimpleArticle>();
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_IP_LIST,
				"192.168.1.103:7777");
		String[] ips = sips.split("\\^");

		Map<Integer,List<String>> listMap = new HashMap<Integer,List<String>>();
		for(int i=0;i<uuidList.size();i++){
			String saUuid = ((BaseMsg)uuidList.get(i)).getUuid();
			int index = Math.abs(saUuid.hashCode())%ips.length;
			if( listMap.containsKey(index) == false ){
				listMap.put(index, new ArrayList<String>());
			}
			List<String> tmp = listMap.get(index);
			tmp.add(saUuid);
		}
		Set<Future<Map<String,SimpleArticle>>> fset = new HashSet<Future<Map<String,SimpleArticle>>>();
		/*
		 * 异步分发
		 */
		Iterator<Entry<Integer,List<String>>> iter = listMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, List<String>> entry = iter.next();
			List<String> tmp = entry.getValue();
			if(tmp!= null && tmp.size() > 0){
				String seed = tmp.get(0);
				List<String> msg = tmp;
				Future<Map<String,SimpleArticle>> f = getSimpleArticleMapAsyn(seed,msg);
				if(f!=null)
					fset.add(f);
			}
		}
		/*
		 * 统一取
		 */
		Iterator<Future<Map<String,SimpleArticle>>> fiter = fset.iterator();
		while (fiter.hasNext()) {
			 Future<Map<String,SimpleArticle>> entry = fiter.next();
			 try {
				Map<String, SimpleArticle> rm = entry.get(30000,
						TimeUnit.MILLISECONDS);
				if (rm != null)
					retMap.putAll(rm);
			} catch (Exception e) {
				logger.error("getSimpleArticleListMapReduce failed!",e);
			}
		}

		return retMap;
	}
	private Future<Map<String, SimpleArticle>> getSimpleArticleMapAsyn(
			final String seed, final List<String> msg) {
		return YASFactory.submit_tcp(new Callable<Map<String,SimpleArticle>>(){

			@Override
			public Map<String, SimpleArticle> call() throws Exception {
				Map<String, SimpleArticle> map = getService().getSimpleArticleMap(seed,msg);
				return map;
			}

		});
	}

	/**
	 * 并发取  需要取到结果的异步方式【内部还是使用getService()方式,只是使用了ExecutorService.submit的返回值Future 】
	 * @param uuid
	 * @param uuidList
	 * @return
	 */
	public Map<String, Map<String, Object>> getSimpleArticleAndCommentsMapReduceV0(String uuid,
			List<String> uuidList, boolean onlyComment, int limit) {
		Map<String,Map<String, Object>> retMap = new HashMap<String,Map<String, Object>>();
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_IP_LIST,
				"192.168.1.103:7777");
		String[] ips = sips.split("\\^");

		Map<Integer,List<String>> listMap = new HashMap<Integer,List<String>>();
		for(int i=0;i<uuidList.size();i++){
			String saUuid = uuidList.get(i);
			int index = Math.abs(saUuid.hashCode())%ips.length;
			if( listMap.containsKey(index) == false ){
				listMap.put(index, new ArrayList<String>());
			}
			List<String> tmp = listMap.get(index);
			tmp.add(saUuid);
		}

		Set<Future<Map<String, Map<String, Object>>>> fset = new HashSet<Future<Map<String, Map<String, Object>>>>();
		/*
		 * 异步分发
		 */
		Iterator<Entry<Integer,List<String>>> iter = listMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, List<String>> entry = iter.next();
			List<String> tmp = entry.getValue();
			if(tmp!= null && tmp.size() > 0){
				Future<Map<String, Map<String, Object>>> f = getSimpleArticleAndCommentAsyncV0(tmp.get(0),tmp, onlyComment, limit);
				if(f!=null)
					fset.add(f);
			}
		}
		/*
		 * 统一取
		 */
		Iterator<Future<Map<String, Map<String, Object>>>> fiter = fset.iterator();
		while (fiter.hasNext()) {
			 Future<Map<String, Map<String, Object>>> entry = fiter.next();
			 try {
				 Map<String, Map<String, Object>> rm = entry.get(30000,
						TimeUnit.MILLISECONDS);
				if (rm != null)
					retMap.putAll(rm);
			} catch (Exception e) {
				logger.error("getSimpleArticleAndCommentsMapReduceV0 failed!",e);
			}
		}
		return retMap;
	}
	private Future<Map<String, Map<String, Object>>> getSimpleArticleAndCommentAsyncV0(final String seed, final List<String> msg, final boolean onlyComment, final int limit) {
		return YASFactory.submit_tcp(new Callable<Map<String, Map<String, Object>>>(){
			@Override
			public Map<String, Map<String, Object>> call() throws Exception {
				Map<String, Map<String, Object>> map = getService().getSimpleArticleAndComentsV0(seed, msg, onlyComment, limit);
				return map;
			}
		});
	}

	/**
	 * 并发取  需要取到结果的异步方式【内部还是使用getService()方式,只是使用了ExecutorService.submit的返回值Future 】
	 * @param uuid
	 * @param uuidList
	 * @return
	 */
	public Map<String, Map<String, Object>> getSimpleArticleAndCommentsMapReduce(String uuid,
			List<String> uuidList, boolean onlyComment, int limit) {
		Map<String,Map<String, Object>> retMap = new HashMap<String,Map<String, Object>>();
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_IP_LIST,
				"192.168.1.103:7777");
		String[] ips = sips.split("\\^");

		Map<Integer,List<String>> listMap = new HashMap<Integer,List<String>>();
		for(int i=0;i<uuidList.size();i++){
			String saUuid = uuidList.get(i);
			int index = Math.abs(saUuid.hashCode())%ips.length;
			if( listMap.containsKey(index) == false ){
				listMap.put(index, new ArrayList<String>());
			}
			List<String> tmp = listMap.get(index);
			tmp.add(saUuid);
		}

		Set<Future<Map<String, Map<String, Object>>>> fset = new HashSet<Future<Map<String, Map<String, Object>>>>();
		/*
		 * 异步分发
		 */
		Iterator<Entry<Integer,List<String>>> iter = listMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, List<String>> entry = iter.next();
			List<String> tmp = entry.getValue();
			if(tmp!= null && tmp.size() > 0){
				Future<Map<String, Map<String, Object>>> f = getSimpleArticleAndCommentAsync(tmp.get(0),tmp, onlyComment, limit);
				if(f!=null)
					fset.add(f);
			}
		}
		/*
		 * 统一取
		 */
		Iterator<Future<Map<String, Map<String, Object>>>> fiter = fset.iterator();
		while (fiter.hasNext()) {
			 Future<Map<String, Map<String, Object>>> entry = fiter.next();
			 try {
				 Map<String, Map<String, Object>> rm = entry.get(30000,
						TimeUnit.MILLISECONDS);
				if (rm != null)
					retMap.putAll(rm);
			} catch (Exception e) {
				logger.error("getSimpleArticleAndCommentsMapReduce failed!",e);
			}
		}
		return retMap;
	}
	private Future<Map<String, Map<String, Object>>> getSimpleArticleAndCommentAsync(final String seed, final List<String> msg, final boolean onlyComment, final int limit) {
		return YASFactory.submit_tcp(new Callable<Map<String, Map<String, Object>>>(){
			@Override
			public Map<String, Map<String, Object>> call() throws Exception {
				Map<String, Map<String, Object>> map = getService().getSimpleArticleAndComents(seed, msg, onlyComment, limit);
				return map;
			}
		});
	}

	/**
	 * 并发取  需要取到结果的异步方式【内部还是使用getService()方式,只是使用了ExecutorService.submit的返回值Future 】
	 * @param uuid
	 * @param uuidList
	 * @return
	 */
	public List<Topic> getTopicReduce(String identify,List<String> iList) {
		List<Topic> rlist = Lists.newArrayList();
		Map<String,Topic> retMap = new HashMap<String,Topic>();
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_IP_LIST,
				"192.168.1.103:7777");
		String[] ips = sips.split("\\^");

		Map<Integer,List<String>> listMap = new HashMap<Integer,List<String>>();
		for(int i=0;i<iList.size();i++){
			String idy = iList.get(i);
			int index = Math.abs(idy.hashCode())%ips.length;
			if( listMap.containsKey(index) == false ){
				listMap.put(index, new ArrayList<String>());
			}
			List<String> tmp = listMap.get(index);
			tmp.add(idy);
		}

		Set<Future<Map<String,Topic>>> fset = new HashSet<Future<Map<String,Topic>>>();
		/*
		 * 异步分发
		 */
		Iterator<Entry<Integer,List<String>>> iter = listMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, List<String>> entry = iter.next();
			List<String> tmp = entry.getValue();
			if(tmp!= null && tmp.size() > 0){
				Future<Map<String, Topic>> f = getTopicAsync(tmp.get(0),tmp);
				if(f!=null)
					fset.add(f);
			}
		}
		/*
		 * 统一取
		 */
		Iterator<Future<Map<String,Topic>>> fiter = fset.iterator();
		while (fiter.hasNext()) {
			Future<Map<String, Topic>> entry = fiter.next();
			try {
				Map<String, Topic> rm = entry.get(30000,
						TimeUnit.MILLISECONDS);
				if (rm != null)
					retMap.putAll(rm);
			} catch (Exception e) {
				logger.error("getTopicReduce failed!",e);
			}
		}

		/**
		 * 合并结果
		 */
		for(int i=0;i<iList.size();i++){
			String idy = iList.get(i);
			Topic t= retMap.get(idy);
			if(t!=null)
				rlist.add(t);
		}
		return rlist;
	}

	private Future<Map<String, Topic>> getTopicAsync(final String seed, final List<String> msg) {
		return YASFactory.submit_tcp(new Callable<Map<String,Topic>>(){
			@Override
			public Map<String,Topic> call() throws Exception {
				Map<String, Topic> map = getService().getTopic(seed, msg);
				return map;
			}
		});
	}

	/**
	 * 返回个map是为了便于合并
	 */
	@Override
	public Map<String,SimpleArticle> getSimpleArticleMap(String uuid,
			List<String> uuidList) {
		return this.getService().getSimpleArticleMap(uuid, uuidList);
	}

	@Override
	public List<Comment> getArticleCommentList(String uuid,int start,int end) {
		return getService().getArticleCommentList(uuid,start,end);
	}

	@Override
	public List<Comment> getArticleCommentList(String uuid, int ftype,long etime, int maxcount){
		return getService().getArticleCommentList(uuid, ftype, etime, maxcount);
	}

	@Override
	public void putSimpleArticle(String uuid, SimpleArticle sa) {
		getService().putSimpleArticle(uuid,sa);
	}

	@Override
	public void loadSimpleArticleList(String uuid, List<SimpleArticle> saList) {
		//分组
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_IP_LIST,
				"192.168.1.103:7777");
		String[] ips = sips.split("\\^");

		Map<Integer,List<SimpleArticle>> listMap = new HashMap<Integer,List<SimpleArticle>>();
		if(saList==null){
			return;
		}
		for(int i=0;i<saList.size();i++){
			String saUuid = saList.get(i).getUuid();
			/*if(saList.get(i).get_attr()!=null && saList.get(i).get_attr().get("tuuid")!=null){
				saUuid = String.valueOf(saList.get(i).get_attr().get("tuuid"));
			}*/
			if(StringUtils.isEmpty(saUuid)){
				continue;
			}
			int index = Math.abs(saUuid.hashCode())%ips.length;
			if( listMap.containsKey(index) == false ){
				listMap.put(index, new ArrayList<SimpleArticle>());
			}
			List<SimpleArticle> tmp = listMap.get(index);
			tmp.add(saList.get(i));
		}

		Iterator<Entry<Integer,List<SimpleArticle>>> iter = listMap.entrySet().iterator();
		while (iter.hasNext()) {
			List<SimpleArticle> tmp = iter.next().getValue();
			if(tmp!= null && tmp.size() > 0){
				// 不需要返回值的异步
				String key = tmp.get(0).getUuid();
				/*if(tmp.get(0).get_attr()!=null && tmp.get(0).get_attr().get("tuuid")!=null){
					key = String.valueOf(tmp.get(0).get_attr().get("tuuid"));
				}*/
				this.getAsynService().loadSimpleArticleList(key,tmp);
			}
		}
//		getService().loadSimpleArticleList(uuid,saList);
	}

	public void loadArticleCommentList(List<String> uuidArr) {
		//分组
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_IP_LIST,
				"192.168.1.103:7777");
		String[] ips = sips.split("\\^");

		Map<Integer,Map<String,List<Comment>>> listMap = new HashMap<Integer,Map<String,List<Comment>>>();
		for(int i=0;i<uuidArr.size();i++){
			String saUuid = uuidArr.get(i);
			int index = Math.abs(saUuid.hashCode())%ips.length;
			Map<String,List<Comment>> map;
			if( listMap.containsKey(index) == false ){
				map = new HashMap<String,List<Comment>>();
				listMap.put(index, new HashMap<String,List<Comment>>());
			}else
				map = listMap.get(index);
			List<Comment> tmp = MicorBlogService.getInstance().getCommentByPage(saUuid, 0, 100);
			map.put(saUuid, tmp);
		}

		Iterator<Entry<Integer,Map<String,List<Comment>>>> iter = listMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map<String,List<Comment>> tmp = iter.next().getValue();
			if(tmp!= null && tmp.size() > 0){
				for(String identify: tmp.keySet()){
					if(tmp.get(identify)!= null && tmp.get(identify).size() >0){
						//不需要返回值的异步
						this.getAsynService().loadArticleComentList(identify,tmp.get(identify));
					}
				}
			}
		}
	}

	@Override
	public void addOrSubSimpleArticle(String uuid, String cloumn,
			boolean addOrSubtract) {
		getService().addOrSubSimpleArticle(uuid, cloumn,addOrSubtract);
	}

	@Override
	public void updateSimpleArticle(String uuid, Map<String, Object> map) {
		getService().updateSimpleArticle(uuid, map);
	}

	@Override
	public void deleteSimpleArticle(String uuid) {
		getService().deleteSimpleArticle(uuid);
	}
	@Override
	public SimpleArticle getSimpleArticle(String uuid) {
		SimpleArticle sa = getService().getSimpleArticle(uuid);
		if(sa ==null){
			sa = new SimpleArticle();
		}
		return sa;
	}
	@Override
	public void loadArticleComentList(String uuid, List<Comment> tmp) {
		getService().loadArticleComentList(uuid,tmp);

	}

	@Override
	public void deleteArticleComment(String auuid, String cuuid) {
		getService().deleteArticleComment(auuid, cuuid);

	}

	@Override
	public boolean addArticleComment(String auuid, Comment c) {
		return getService().addArticleComment(auuid, c);
	}

	@Override
	public List<TalkMessage> getNextUserTalkMessageListByPaging(Long uid,
			int ftype, long etime, int maxcount) {
		return getService().getNextUserTalkMessageListByPaging(uid, ftype,etime,maxcount);
	}

	@Override
	public List<SimpleArticle> getNextUsubjectFollowFavoriteListByType(
			String usubjectid, long time, int type, int limit) {
		return getService().getNextUsubjectFollowFavoriteListByType(usubjectid, time, type, limit);
	}

	@Override
	public Map<String,Object> getNextRecommendFollowFavoriteList(
			String usubjectid, long time, int type, int limit) {
		return getService().getNextRecommendFollowFavoriteList(usubjectid, time, type, limit);
	}

	@Override
	public int getRecommendNewBlogMessageCount(String usubjectid, long time) {
		return getService().getRecommendNewBlogMessageCount(usubjectid, time);
	}

	@Override
	public void delNextRecommendFollowFavoriteList(String usubjectid, String uuid) {
		 getService().delNextRecommendFollowFavoriteList(usubjectid, uuid);

	}

	@Override
	public List<SimpleArticle> getNextUsubjectViewpointFavoriteList(
			String usubjectid, long time, int type, int limit) {
		return getService().getNextUsubjectViewpointFavoriteList(usubjectid, time, type, limit);
	}

	public boolean deleteViewPointEntryFromEventCache(String code, String uuid) {
		try {
			return getService().deleteViewPointEntryFromEventCache(code, uuid);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public int getNextUsubjectViewpointFavoriteCount(String usubjectid,
			long time, int type) {
		// TODO Auto-generated method stub
		return getService().getNextUsubjectViewpointFavoriteCount(usubjectid, time, type);
	}

	public List<SimpleArticle> getNextUsubjectNoticeFavoriteList(String usubjectid,long time,int type,int limit){
		return getService().getNextUsubjectNoticeFavoriteList(usubjectid, time, type, limit);
	}

	@Override
	public List<Comment> getArticleCommentListByTime(String uuid, long time,
			int type, int limit) {
		return getService().getArticleCommentListByTime(uuid, time, type, limit);
	}

	@Override
	public void reloadIndexBlog(long uid) {
		 getService().reloadIndexBlog(uid);

	}

	@Override
	public void reloadIndexBlog(long uid,String fuids) {
		getService().reloadIndexBlog(uid,fuids);

	}
	@Override
	public void clearIndexBlog(long uid,String fuids) {
		getService().clearIndexBlog(uid,fuids);

	}

	public void removeIdentifyArticle(String companycode, String auuid) {
		getService().removeIdentifyArticle(companycode,auuid);

	}

	public void remoteWriteUserType0(long uid,UserMsg umTmp,List<Long> uidList,String doDelete){
		getAsynService().remoteWriteUserType0(uid, umTmp, uidList, doDelete);
	}

	@Override
	public boolean readTalkMessage(Long iuid, String[] fuidArray) {
		return getService().readTalkMessage(iuid, fuidArray);
	}

	@Override
	public Long lastLoginTime(long uid) {
		return getService().lastLoginTime(uid);
	}

	@Override
	public void clearUserRelationship(long fansUid, long guanzhuUid) {
		getService().clearUserRelationship(fansUid, guanzhuUid);
	}

	public void addUserRelationShip(Long uid, Long fuid) {
		getService().addUserRelationShip(uid,fuid);

	}

	@Override
	public boolean clearTalkMessage(Long iuid, String fuids) {
		return getService().clearTalkMessage(iuid, fuids);
	}

	@Override
	public boolean clearTalkMessage2(Long iuid, Long fuid, String uuids) {

		return getService().clearTalkMessage2(iuid, fuid,uuids);
	}

	@Override
	public String lastLoginIp(long uid) {
		return getService().lastLoginIp(uid);
	}

	@Override
	public Map<String, Map<String, Object>> getSimpleArticleAndComents(
			String uuid, List<String> uuidList, boolean onlyComment, int limit) {
		return getService().getSimpleArticleAndComents(uuid, uuidList, onlyComment, limit);
	}
	@Override
	public Map<String, Map<String, Object>> getSimpleArticleAndComentsV0(
			String uuid, List<String> uuidList, boolean onlyComment, int limit) {
		return getService().getSimpleArticleAndComentsV0(uuid, uuidList, onlyComment, limit);
	}
	@Override
	public Map<String,Topic> getTopic(String identfy, List<String> iList) {
		return getService().getTopic(identfy, iList);
	}

	@Override
	public void notifyUsubjectAdd(Long uid, Long usubjectUid) {
		 getService().notifyUsubjectAdd(uid, usubjectUid);

	}
	@Override
	public void notifyUsubjectDelete(Long uid, Long usubjectUid) {
		getService().notifyUsubjectDelete(uid, usubjectUid);

	}
	@Override
	public void addTopicUserRelationship(String identify,String uid) {
		getService().addTopicUserRelationship(identify,uid);

	}
	@Override
	public void delTopicUserRelationship(String identify,String uid) {
		getService().delTopicUserRelationship(identify,uid);

	}
	@Override
	public void addUserTopicRelationship(String uid,String identify) {
		getService().addUserTopicRelationship(uid,identify);

	}
	@Override
	public void delUserTopicRelationship(String uid,String identify) {
		getService().delUserTopicRelationship(uid,identify);

	}
	@Override
	public List<String> getTopicUserRelationship(String key) {
		return getService().getTopicUserRelationship(key);

	}
	@Override
	public List<String> getUserTopicRelationship(String key) {
		return getService().getUserTopicRelationship(key);

	}
	@Override
	public List<Topic> getTopicByUidForIndex(String uid,int index,int limit) {
		return getService().getTopicByUidForIndex(uid,index,limit);

	}

	@Override
	public USubject getUSubjectByUIdentifyFromCache(String identify) {
		return getService().getUSubjectByUIdentifyFromCache(identify);
	}
	@Override
	public void addUSubjectByUIdentifyFromCache(String identify,USubject us) {
		 getService().addUSubjectByUIdentifyFromCache(identify,us);
	}

	public int add2ChanceWaper(String key, String name, TradeAlarmMsg um,
			int maxsize, boolean isleaf, Long lastTime, int level) {
		try {
			return getService().add2ChanceWaper(key, name, um, maxsize, isleaf, lastTime, level);
		} catch (Exception e) {
			logger.error("add2ChanceWaper: " + key + "                   " + e);
		}
		return 0;
	}

	public int add2SquareChanceWrapper(String key, TradeAlarmMsg um, int level) {
		try {
			return getService().add2SquareChanceWrapper(key, um, level);
		} catch (Exception e) {
			logger.error("add2SquareChanceWrapper: " + e);
		}
		return 0;
	}

	public void addStockChance(String key, TradeAlarmMsg um) {
		try {
			getService().addStockChance(key, um);
		} catch (Exception e) {
			logger.error("addStockChance: " + key + "                    " + e);
		}
	}

	public void bulidChanceCategory(String fixedKey, String key, String name, boolean isleaf) {
		try {
			getService().bulidChanceCategory(fixedKey, key, name, isleaf);
		} catch (Exception e) {
			logger.error("bulidChanceCategory: " + key + "                  " + e);
		}
	}

	public void modifyChancesCategoryLeaf(String fixedKey, String key, USubject usubject, int chanceMsgWrapSize,  Long lastTime, int opt) {
		try {
			getService().modifyChancesCategoryLeaf(fixedKey, key, usubject, chanceMsgWrapSize, lastTime, opt);
		} catch (Exception e) {
			logger.error("modifyChancesCategorySecond: " + key + "              " + e);
		}
	}

	public StockChanceEntity getStockChanceEntity(String fixedKey) {
		try {
			return getService().getStockChanceEntity(fixedKey);
		} catch (Exception e) {
			logger.error("getStockChanceEntity: " + e);
		}
		return null;
	}

	public void modifyCategoryTime(String fixedKey, StockChanceEntity sc) {
		getService().modifyCategoryTime(fixedKey, sc);
	}

	public boolean removeChanceWrapper(String key) {
		try {
			return getService().removeChanceWrapper(key);
		} catch (Exception e) {
			logger.error("removeChanceWrapper: " + key + "                " + e);
			return false;
		}
	}

	public boolean deleteChancesCategoryByKey(String fixedKey, String key) {
		try {
			return getService().deleteChancesCategoryByKey(fixedKey, key);
		} catch (Exception e) {
			logger.error("deleteChancesCategoryByKey: " + key + "                     " + e);
		}
		return false;
	}

	public boolean clearChanceCategory(String fixedKey) {
		try {
			return getService().clearChanceCategory(fixedKey);
		} catch (Exception e) {
			logger.error("clearChanceCategory: " + e);
		}
		return false;
	}

	public boolean deleteStockChance(String key, String uuid, boolean isLeaf, long time) {
		try {
			return getService().deleteStockChance(key, uuid, isLeaf, time);
		} catch (Exception e) {
			logger.error("deleteStockChance: " + e);
		}
		return false;
	}

	public List<IMessage> getChanceMessageList(String key, int ftype, long time) {
		try {
			return getService().getChanceMessageList(key, ftype, time);
		} catch (Exception e) {
			logger.error("getChanceMessageListxd: " + e);
		}
		return null;
	}

	public void reloadAllStockChance(String key) {
		try {
			getService().reloadAllStockChance(key);
		} catch (Exception e) {
			logger.info("reloadAllStockChance: " + key + "                    " + e);
		}
	}

	public boolean saveChanceCategory(String key, TradeAlarmMsg um) {
		return getService().saveChanceCategory(key, um);
	}

	@Override
	public List<Map<String, Object>> getChanceMessageList(String key, int ftype,
			long time, int limit) {
		return getService().getChanceMessageList(key, ftype, time, limit);
	}

	@Override
	public int getIMessageListCount(String key, int ftype, long time) {
		return getService().getIMessageListCount(key, ftype, time);
	}

	@Override
	public List<Map<String, Object>> getLatestNewChanceMessageList(String key, int limit) {
		return getService().getLatestNewChanceMessageList(key, limit);
	}

	@Override
	public IMessage getChanceMessage(String key, String uuid) {
		return getService().getChanceMessage(key, uuid);
	}

//	@Override
//	public List<IMessage> getPagingChanceMessageList(String key,
//			int oldTotalCount, int pageNo, int limit) {
//		return getService().getPagingChanceMessageList(key, oldTotalCount, pageNo, limit);
//	}

	@Override
	public long getLastChanceUpdateTime(String fixedKey) {
		return getService().getLastChanceUpdateTime(fixedKey);
	}

	@Override
	public long getLatestMessageTime(String key) {
		return getService().getLatestMessageTime(key);
	}

	@Override
	public List<com.yz.common.vo.Pair<String, String>> getChancesCategoryLeafList(String fixedKey, Long ctime) {
		return getService().getChancesCategoryLeafList(fixedKey, ctime);
	}

	@Override
	public void clearStockChanceBlog(long uid, String fuids) {
		getService().clearStockChanceBlog(uid, fuids);
	}

	@Override
	public int getStockChanceMessageCount(Long uid, long time) {
		return getService().getStockChanceMessageCount(uid, time);
	}
	@Override
	public int getChanceSqualeCount(String key, long time) {
		return getService().getChanceSqualeCount(key, time);
	}

	public WeiXinAuthModel getWeiXinAuthModel(String key) {
		return getService().getWeiXinAuthModel(key);
	}

	@Override
	public void addTopicArticleListWapper(long uid, String fuids,int type) {
		 getService().addTopicArticleListWapper(uid, fuids, type);

	}

	@Override
	public boolean readMessageWithTime(Long uid, String type) {
		// TODO Auto-generated method stub
		return getService().readMessageWithTime(uid, type);
	}

	@Override
	public void addCommentCounts(String uuid, int count) {
		getService().addCommentCounts(uuid, count);
	}

	public void deleteSquareStockChance(String key, String uuid) {
		getService().deleteSquareStockChance(key, uuid);
	}

	@Override
	public void updateArticleAttr(String uuid, Map<String, Object> map) {
		getService().updateArticleAttr(uuid, map);
		
	}

	@Override
	public void addSubscribeRelationship(Long uid, Long fuid) {
		getService().addSubscribeRelationship(uid, fuid);
	}

	@Override
	public void clearSubscribeRelationship(Long uid, Long fuid) {
		getService().clearSubscribeRelationship(uid, fuid);
	}
}
