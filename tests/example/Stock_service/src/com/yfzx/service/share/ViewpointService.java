package com.yfzx.service.share;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.ShareConst;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.TopicConstants;
import com.stock.common.model.TopicUser;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.share.TimeLine;
import com.stock.common.model.share.UserExt;
import com.stock.common.model.share.Viewpoint;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.StockCacheUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.chance.ChanceCategoryService;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.db.MessageCenterSerivce;
import com.yfzx.service.db.TopicService;
import com.yfzx.service.db.TopicUserService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.UsubjectEventService;
import com.yfzx.service.msg.event.IMessageListWapper;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.message.IMessage;

/**
 * 观点服务
 *
 */
public class ViewpointService {
	static{
		initStockChanceConfig();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				initStockChanceConfig();
			}
		});
	}

	private static List<String> configList;
	Logger log = LoggerFactory.getLogger(this.getClass());

	private static ViewpointService instance = new ViewpointService();

	public static ViewpointService getInstance() {
		return instance;
	}

	public String updateViewpoint(Article article) {
		return StockCodes.SUCCESS;
	}

	private static void initStockChanceConfig(){
		configList = Lists.newArrayList();
		String config = ConfigCenterFactory.getString("stock_chance.msg_type","100,101,103" );
		String[] msgType = config.split(",");
		for(String t :msgType){
			configList.add(t);
		}
	}

	public String deleteViewpoint(Article article){

		TimeLineService tls = TimeLineService.getInstance();
		String sKey = article.getTags();
		long timemillis = article.getTime();

		try {
			tls.deltetTimeLine(sKey, SAVE_TABLE.VIEWPOINT,timemillis);
			tls.deltetTimeLine(String.valueOf(article.getUid()), SAVE_TABLE.ARTICLE, timemillis);

			CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
			ch.delete(SAVE_TABLE.ARTICLE.toString(), article.getUuid());

			UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(article.getUid());
			if(userExt != null) {
				Map<String, Object> userMap = new HashMap<String, Object>();
				userMap.put("article_counts", userExt.getArticle_counts() - 1 >= 0 ? userExt.getArticle_counts() - 1 : 0);
				UserServiceClient.getInstance().updateUserExt(article.getUid(), userMap);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			log.error("deleteViewpoint: " + e1);
		}

		try {
			RemindServiceClient.getInstance().deleteSimpleArticle(article.getUuid());
			RemindServiceClient.getInstance().deleteViewPointEntryFromEventCache(article.getTags(), article.getUuid());
			deleteViewPointComment(article.getUuid());
		} catch (Exception e) {
			e.printStackTrace();
			log.error("deleteViewpoint: " + e);
		}

		return StockCodes.SUCCESS;
	}

	private void deleteViewPointComment(String uuid) {
		 try {
			CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
			 TimeLineService tls = TimeLineService.getInstance();
			  List<TimeLine> timelineList = tls.getTimeLineList(uuid,
						SAVE_TABLE.COMMENT, 0, Integer.MAX_VALUE);
			  	//删除时光轴上的文章[带评论列表]
				tls.deltetTimeLine(uuid, SAVE_TABLE.COMMENT);
				// 删除文章评论组的实体
				List<String> uuidList = new ArrayList<String>();
				for (TimeLine tl2 : timelineList) {
					uuidList.add(tl2.getUuid());
				}
				String[] uuidStrArray = new String[uuidList.size()];
				String[] arr2 = uuidList.toArray(uuidStrArray);
				ch.delete(SAVE_TABLE.COMMENT.toString(),arr2);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("deleteViewPointComment: " + e);
		}
	}

	/**
	 * DCSS的CacheLoadeService加载使用，它从nosql加载观点数据，
	 */
	public List<SimpleArticle> listFromNosql(String key,long endtime,int start,int num){
		TimeLineService tls = TimeLineService.getInstance();
		List<TimeLine> tlList = tls.getTimeLineListByTime(key, SAVE_TABLE.VIEWPOINT,endtime, start, num);
		List<SimpleArticle> saList = MicorBlogService.getInstance().getSimpleArticleList(tlList);
		return saList;
	}
	/**
	 * DCSS的CacheLoadeService加载使用，它从nosql加载观点数据，
	 */
	public List<SimpleArticle> listFromNosqlBytime(String key,long startTime,long endTime){
		TimeLineService tls = TimeLineService.getInstance();
		List<TimeLine> tlList = tls.getTimeLineListByTime(key, SAVE_TABLE.VIEWPOINT,startTime,endTime,Integer.MAX_VALUE);
		List<SimpleArticle> saList = MicorBlogService.getInstance().getSimpleArticleList(tlList);
		return saList;
	}

	/**
	 * 此方法供zjs使用，  它去DCSS获取数据
	 * @param usubjectid
	 * @param time
	 * @param type
	 * @param limit
	 * @return
	 */
	public List<SimpleArticle> listFromDcss(String usubjectid, long time, int type, int limit){
		return RemindServiceClient.getInstance().getNextUsubjectViewpointFavoriteList(usubjectid, time, type, limit);
	}

	/**
	 * 取观点数量
	 * @param usubjectid
	 * @param time
	 * @param type
	 * @param limit
	 * @return
	 */
	public int getNextUsubjectViewpointFavoriteCount(String usubjectid, long time, int type){
		return RemindServiceClient.getInstance().getNextUsubjectViewpointFavoriteCount(usubjectid, time, type);
	}
	/**
	 * DCSS 从本地缓存中获取
	 * @param code
	 * @param time
	 * @param type
	 * @param limit
	 * @return
	 */
	public List<SimpleArticle> getNextCompanyArticleListFromCache(String code,
			long time, int type, int limit) {
		String nkey = StockUtil.getViewpointArticleKey(code);
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		List<SimpleArticle>  saList = null;
		if(eq!=null){
			List<IMessage> list = new ArrayList<IMessage>();
			List<String> uuidList = new ArrayList<String>();
			if(type==1){
				time= time-1;
			}else{
				time = time+1;
			}
			if(limit == -1) {

			} else {
				list = eq.getMessageList(type,time, limit);
			}
			for(IMessage m : list){
				UserMsg um = (UserMsg)m;
				if(um!=null){
					uuidList.add(String.valueOf(um.getAttr("uuid")));
				}
			}

			if(uuidList.size()>0){
				saList = RemindServiceClient.getInstance().getSimpleArticleList(uuidList.get(0),uuidList);
			}
		}
		return saList;
	}

	public boolean deleteViewPointEntryFromEventCache(String code, String uuid) {
		String nkey = StockUtil.getViewpointArticleKey(code);
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));

		if(eq!=null) {
			List<IMessage> list = eq.getMessageList();
			if(list != null) {
				for(IMessage msg : list) {
					if(msg != null) {
						UserMsg um = (UserMsg)msg;
						if(StringUtils.equals(um.getUuid(), uuid) ||
								um.getAttr("uuid") != null && ((String)um.getAttr("uuid")).equals(uuid)) {
//							return list.remove(msg);
							eq.remove(msg);
						}
					}
				}
			} else {
				return true;
			}
		}

		return true;
	}

	public int getMessageListLatestCount(String code, long time, int type) {
		String nkey = StockUtil.getViewpointArticleKey(code);
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		if(eq!=null){
			if(type==1){
				time= time-1;
			}else{
				time = time+1;
			}
			return eq.getMessageListCount(type, time);
		}
		return 0;
	}


	/**
	 * @param umType 机会类型
	 * @param time
	 * @param type
	 * @param limit
	 * @return
	 */
	public List<TradeAlarmMsg> listUmFromNosql(String umTypeKey,long endtime,int start,int num){
		TimeLineService tls = TimeLineService.getInstance();
		List<TimeLine> tlList = tls.getTimeLineListByTime(umTypeKey, SAVE_TABLE.VIEWPOINT,endtime, start, num);
		if(tlList.size() >0 ){
			List<TradeAlarmMsg> msgList = new ArrayList<TradeAlarmMsg>();
			String[] keys = new String[tlList.size()];
			for (int i = 0; i < tlList.size(); i++) {
				keys[i] = tlList.get(i).getUuid();
			}
			String[] columns = CassandraHectorGateWay.getInstance().getColumns(TradeAlarmMsg.class);
			Map<String, Map<String, String>> batchMap =  CassandraHectorGateWay
					.getInstance().get(
					SAVE_TABLE.ARTICLE.toString(), keys, columns);
			for (TimeLine timeline : tlList) {
				String uuid = timeline.getUuid();
				Map<String, String> aMap = batchMap.get(uuid);
				TradeAlarmMsg msg = new TradeAlarmMsg();
				NosqlBeanUtil.map2Bean(msg, aMap);
				if (aMap.isEmpty() == false) {
					msgList.add(msg);
				}
			}
			return msgList;
		}else{
			return null;
		}
	}

	public String publishViewpoint(SimpleArticle sa,TradeAlarmMsg um){
		if(sa ==null || StringUtil.isEmpty(sa.getTags()) || StringUtil.isEmpty(sa.getUuid()) || sa.getUid() <= 0){
			log.error("publishViewpointX: no tags or uuid or uid = 0" );
			return StockCodes.ERROR;
		}
		
		Viewpoint vp = getViewpoint(sa, um);

		return publishViewpoint(vp,sa,um);
	}

	public Viewpoint getViewpoint(SimpleArticle sa, TradeAlarmMsg um) {
		Viewpoint vp = new Viewpoint();
		sa.setType(ShareConst.VIEWPOINT);
		String tags = sa.getTags();
		String companyCode = tags.split(":")[0];
		if(sa.getAttr("text") != null) {
			vp.putAttr("topic", (String)sa.getAttr("text"));
		}
		sa.setTags(companyCode);
		if(sa.getType() == ShareConst.VIEWPOINT) {
			String content = sa.getAttr("content");
			if(StringUtils.isNotBlank(content)) {
				vp.setContent(content);
			} else {
				vp.setContent(sa.getSummary());
			}
		} else {
			vp.setContent(sa.getSummary());
		}
		Integer equitCount = sa.getAttr(StockConstants.EQUITY_COUNT);
		if(equitCount != null) {
			vp.putAttr(StockConstants.EQUITY_COUNT, equitCount);
		}
		vp.setSummary(sa.getSummary());
		vp.setSourceid(um.getSourceid());
		vp.setEventid(um.getEventid());
		vp.setUid(sa.getUid());
		vp.setStime(um.getStime());
		vp.setEtime(um.getEtime());
		vp.setMsgType(um.getMsgType());
		vp.setSendType(um.getSendType());
		vp.setType(sa.getType());
		vp.setArticleType(sa.getArticleType());
		vp.setImg(sa.getImg());
		String ats = sa.getAttr("ats");
		vp.setAts(ats);
		String keywordLevel = sa.getAttr("keywordLevel");
		vp.setKeywordLevel(keywordLevel);
		String system_category = sa.getAttr("system_category");
		vp.setSystem_category(system_category);
		String blog_category = sa.getAttr("blog_category");
		vp.setBlog_category(blog_category);
//		vp.set_attr(sa.get_attr());
		String desc = um.getAttr("desc");
		if(StringUtils.isNotBlank(desc)) {
			vp.putAttr("desc", desc);
		}
		vp.putAttr("uid", sa.getUid());
		String chancetag = sa.getAttr(StockConstants.CHANCETAG);
		if(StringUtils.isNotBlank(chancetag)) {
			vp.putAttr(StockConstants.CHANCETAG, chancetag);
		}

		Object tmpPrice = null;
		Integer recommend = um.getAttr("recommend");
		if(recommend != null && recommend == 1) {
			tmpPrice = um.getAttr("reprice");
		} else {
			tmpPrice = um.getAttr("price");
		}
		Double price = 0d;
		if(tmpPrice != null) {
			if(tmpPrice instanceof BigDecimal) {
				price = ((BigDecimal)tmpPrice).doubleValue();
			} else if(tmpPrice instanceof Double) {
				price = (Double)tmpPrice;
			}
		}
		if(price != null) {
			vp.putAttr("price", price);
		}

		vp.setTitle(sa.getTitle());
		String utag = um.getAttr("chancetag");
		if(StringUtils.isNotBlank(utag)){
			String atags = sa.getTags()+","+utag;
			sa.setTags(atags);
		}
		vp.setTags(sa.getTags());
		vp.setUuid(sa.getUuid());
		vp.setTime(sa.getTime());
		vp.setNick(sa.getNick());
		vp.setComment_counts(sa.getComment_counts());
		vp.setRecommend_counts(sa.getRecommend_counts());
		vp.setBroadcast_counts(sa.getBroadcast_counts());
		vp.setFavorite_counts(sa.getFavorite_counts());
		vp.setBlog_category(blog_category);
		vp.setKey(sa.getKey());
		vp.setSource_url(sa.getSource_url());
		vp.setSuuid(sa.getSuuid());
		vp.setViewpointType(sa.getViewpointType());

		Object key = um.getAttr("ktype");
		//log.info("ktype = "+ key + "|| code = "+ sa.getTags());
		if(key!=null){//设置kType 区分K线类型(分时:f,日：d,周：w,月：m)
			sa.putAttr("ktype", String.valueOf(key));
		}
		return vp;
	}

	public String publishViewpoint(Viewpoint vp,SimpleArticle sa,TradeAlarmMsg um){
		if(vp.getTags() == null){
			log.info("publishViewpoint tags is null");
			return StockCodes.ERROR;
		}
		String[] identifys = vp.getTags().split(",");
		String reStr = publishPrivateViewpoint(vp,um);
		RemindServiceClient.getInstance().putSimpleArticle(sa.getUuid(), sa);

		if(ChanceCategoryService.getMgVpSet().contains(vp.getUid())) {
			for(String identify : identifys){
				//通知tags
				UserMsg umTag = SMsgFactory
						.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
				umTag.setS(String.valueOf(sa.getUid()));
				umTag.setD(identify);
				umTag.putAttr("uuid", sa.getUuid());
				umTag.setTime(sa.getTime());
				//注册发微博消息
				umTag.putAttr("identify", identify);
				umTag.putAttr("viewpoint", vp.getSummary());
				UsubjectEventService.getInstance().notifyTheEvent(umTag);
			}

		}

		String[] atArr = null;
		if (! StringUtil.isEmpty(vp.getAts())) {
			atArr = vp.getAts().split(",");
		}
		if (atArr != null) {
			for (String a : atArr) {
				// 支持两种格式 9:唐斌奇 和 9
				String[] aArr = a.split(":");
				if(aArr.length !=0){
					String atUid = aArr[0].trim();
					if(!StringUtils.isNumeric(atUid)){
						 atUid = String.valueOf(UserServiceClient.getInstance().getUidByNickname(atUid));
					}
					if(atUid!=null){
						TimeLineService.getInstance().saveTimeLine(atUid, vp.getUuid(),TimeLineService.SAVE_TABLE.AT, vp.getTime());
					}
				}
			}
		}
		if(!StringUtil.isEmpty(vp.getAts())){
			MessageCenterSerivce.getInstance().atBlog(vp);//@推送
		}

		String s = "article_counts";
		MicorBlogService.getInstance().userExtAdd(s, String.valueOf(vp.getUid()));

		if(ConfigCenterFactory.getInt("stock_zjs.send_chance_to_friends", 1) == 1) {
			//注册发微博消息
			UserMsg sm = SMsgFactory
					.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_0);
			sm.setS(String.valueOf(vp.getUid()));
			sm.setD(String.valueOf(vp.getUid()));
			sm.putAttr("type", ShareConst.YUAN_CHUANG);
			sm.putAttr("title",vp.getTitle());
			sm.putAttr("source_url",vp.getSource_url());
			sm.putAttr("uuid", vp.getUuid());
			sm.setTime(vp.getTime());
			// 发表了新博文消息通知好友,我发送了新博文
			UserEventService.getInstance().notifyTheEvent(sm);
		}

		for(String identify : identifys){
			//单播 加入缓存
			UserMsg essenceUm = SMsgFactory
					.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
			essenceUm.setS(String.valueOf(vp.getUid()));
			essenceUm.setD(identify);
			essenceUm.putAttr("uuid", vp.getUuid());
			essenceUm.putAttr("identify", identify);
			essenceUm.setTime(vp.getTime());
			UsubjectEventService.getInstance().notifyTheEvent(essenceUm);
		}

		if(ChanceCategoryService.getEssenceVpSet().contains(sa.getUid())) {
			for(String identify : identifys){
				if(identify.indexOf(".hk")>0 || identify.indexOf(".sz")>0 || identify.indexOf(".sh")>0){
					//单播 加入缓存
					UserMsg essenceUm = SMsgFactory
							.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
					essenceUm.setS(String.valueOf(vp.getUid()));
					essenceUm.setD(identify);
					essenceUm.putAttr("uuid", vp.getUuid());
					essenceUm.putAttr("identify", identify);
					essenceUm.setTime(vp.getTime());
					//推荐博文标识
					essenceUm.putAttr("isRecommend", true);
					UsubjectEventService.getInstance().notifyTheEvent(essenceUm);
				}
			}
		}
		//机器产生的话题博文自动推荐到机会首页
		String st = String.valueOf(um.getMsgType());
		if(configList.contains(st)){
			for(String identify : identifys){
				if(identify.indexOf(".hk")>0 || identify.indexOf(".sz")>0 || identify.indexOf(".sh")>0){
					continue;
				}
				boolean autoRecommend = true;
				if(ChanceCategoryService.getEssenceVpSet().contains(vp.getUid()) == false){
					String checkList = ConfigCenterFactory.getString("stock_chance.check_topic_list", "");
					if(StringUtils.isNotBlank(checkList)){
						String arr[] = checkList.split("~");
						for(String t : arr){
							if(identify.equals(t)){
								log.info("话题 #"+identify+"# 在check_topic_list列表中,不推荐为精华");
								autoRecommend = false;
								break;
							}
						}
					}		
				}
				if(autoRecommend == true){
					//机器精选等分类的，自动推精华，并推送到机会广场和我的订阅中				
					TopicService.getInstance().doRecommend(identify, sa.getUuid());
				}else{
					//高手调仓也类似，是它的上层调用TradeService.getInstance().publishStockChance
					//开盘异动 只进机会广场
					TopicService.getInstance().doChanceIndex(identify, sa.getUuid());
				}
			}
		}
		return reStr;
	}

	private String publishPrivateViewpoint(Viewpoint vp,TradeAlarmMsg um) {
		if(vp ==null || StringUtil.isEmpty(vp.getTags()) || StringUtil.isEmpty(vp.getUuid()) || vp.getUid() <= 0 || vp.getTime() <= 0){
			log.error("publishPrivateViewpointx: no tags or uuid or uid = 0" );
			return StockCodes.ERROR;
		}
		RemindServiceClient.getInstance().saveChanceCategory(StockConstants.SCC_ENTITY, um);
		TimeLineService tls = TimeLineService.getInstance();
		String uuid = vp.getUuid();
		String uid = String.valueOf(vp.getUid());
		long timemillis = vp.getTime();
		
		//类MicorBlogService.publishArticle方法
		tls.saveTimeLine(uid, uuid, SAVE_TABLE.ARTICLE, timemillis);
		Map<String, String> map = NosqlBeanUtil.bean2Map(vp);
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
		ch.insert(SAVE_TABLE.ARTICLE.toString(), vp.getUuid(), map);
		
		String[] tags = {} ;
		if(vp.getTags() != null){
			tags = vp.getTags().split(",");
		}
		for(String tag : tags) {
			if(StringUtils.isBlank(tag)) {
				continue;
			}
			if( tag.endsWith(".sh") || tag.endsWith(".sz") || tag.endsWith("hk") ) {
				String stockcode = tag;
				if(ChanceCategoryService.getMgVpSet().contains(vp.getUid())) {
					tls.saveTimeLine(stockcode,uuid, SAVE_TABLE.VIEWPOINT, timemillis);
				}
				if(ChanceCategoryService.getEssenceVpSet().contains(vp.getUid())) {
					Map<String,String> topicRecommend = new HashMap<String,String>();
					topicRecommend.put(uuid,String.valueOf(timemillis));
					ch.insert(SAVE_TABLE.TOPICRECOMMEND.toString(), stockcode, topicRecommend);
				}
			}else{
				TopicUser tu = TopicUserService.getInstance().fetchSingleFromCache(tag);
				int MAXLENGTH = ConfigCenterFactory.getInt("stock_zjs.maxSummaryLength", 1);
				boolean noCheck = ConfigCenterFactory.getInt("stock_zjs.update_topic_index_from_publish", 1)==1;
				Map<String,Object> topMap = Maps.newHashMap();
				topMap.put("time", vp.getTime());
				topMap.put("identify", tag);
				if(tu.getStatus()!=TopicConstants.USER_PASSED){
					continue;
				}
				if((tu.getUid().longValue()==vp.getUid()
						|| vp.getArticleType()==2
						|| vp.getSummary().length()>MAXLENGTH)
						&& vp.getType()!=ShareConst.ZHUANG_ZAI
						&& noCheck){//代标题的长博文
					topMap.put("summary", vp.getSummary());
					topMap.put("title", vp.getTitle());
					topMap.put("uuid", vp.getUuid());
				}
				//更新话题缓存
				TopicService.getInstance().updateTopicCache(topMap);
			}
			if(StringUtils.isNotBlank(tag)){
				tls.saveTimeLine(tag, uuid, TimeLineService.SAVE_TABLE.TOPIC, timemillis);
			}
		}
		
		return StockCodes.SUCCESS;
	}

	public void persistSquareStockChance(String keys, SimpleArticle sa) {
		log.info("persistSquareStockChance: ");
		if(sa == null || StringUtils.isBlank(sa.getUuid()) || StringUtils.isBlank(keys)) {
			return ;
		}
		long time = System.currentTimeMillis();
		TimeLineService tls = TimeLineService.getInstance();
		TradeAlarmMsg um = new TradeAlarmMsg();
		um.setUuid(sa.getUuid());
		um.setTime(time);
		if(sa.getAttr("topic")!=null){
			um.putAttr("topic", String.valueOf(sa.getAttr("topic")));
		}
		for(String key : keys.split(",")) {
			if(StringUtils.isNotBlank(key)) {
				tls.saveTimeLine(key, sa.getUuid(), SAVE_TABLE.VIEWPOINT, time);
				RemindServiceClient.getInstance().add2SquareChanceWrapper(key, um, 2);
			}
		}
		String all_square_keys = ConfigCenterFactory.getString("stock_zjs.all_square_chance_keys", "allSquareKeys");
		RemindServiceClient.getInstance().add2SquareChanceWrapper(all_square_keys, um, 2);
		sa.putAttr("squareTime",time);
		sa.removeAttr("topic");
		//修改博文实体 标示该博文已被推荐到机会广场
		TopicService.getInstance().squareArticle(sa);
	}
	public void persistSquareStockChanceWithOutUpdate(String keys, SimpleArticle sa,long recommendTime) {
		log.info("persistSquareStockChance: ");
		if(sa == null || StringUtils.isBlank(sa.getUuid()) || StringUtils.isBlank(keys)) {
			return ;
		}
		long time = recommendTime;
		TimeLineService tls = TimeLineService.getInstance();
		TradeAlarmMsg um = new TradeAlarmMsg();
		um.setUuid(sa.getUuid());
		um.setTime(time);
		if(sa.getAttr("topic")!=null){
			um.putAttr("topic", String.valueOf(sa.getAttr("topic")));
		}
		for(String key : keys.split(",")) {
			if(StringUtils.isNotBlank(key)) {
				tls.saveTimeLine(key, sa.getUuid(), SAVE_TABLE.VIEWPOINT, time);
				RemindServiceClient.getInstance().add2SquareChanceWrapper(key, um, 2);
			}
		}
		String all_square_keys = ConfigCenterFactory.getString("stock_zjs.all_square_chance_keys", "allSquareKeys");
		RemindServiceClient.getInstance().add2SquareChanceWrapper(all_square_keys, um, 2);
	}

}
