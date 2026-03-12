package com.yfzx.service.db;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.ShareConst;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockException;
import com.stock.common.constants.TopicConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Topic;
import com.stock.common.model.USubject;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.user.Members;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.SqlWithTrasitionUtil;
import com.stock.common.util.SqlWithTrasitionUtil.ISqlCallback;
import com.stock.common.util.StockUtil;
import com.yfzx.service.chance.ChanceCategoryService;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.client.TopicServiceClient;
import com.yfzx.service.client.es.ESClient;
import com.yfzx.service.comet.CometPushMsgType;
import com.yfzx.service.db.user.MembersService;
import com.yfzx.service.db.user.ResetMemberModel;
import com.yfzx.service.db.user.ResetMemberService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.TalkMessageService;
import com.yfzx.service.msg.UsubjectEventService;
import com.yfzx.service.msg.handler.c.RemindClientHandler;
import com.yfzx.service.msg.handler.m.UsubjectType0MsgHandler;
import com.yfzx.service.nosql.NosqlService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService;
import com.yfzx.service.share.ViewpointService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yfzx.service.spider.HttpRequestProxy;
import com.yfzx.service.util.StockChanceUtil;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class TopicService {
	static DBAgent dbAgent = DBAgent.getInstance();
	private static CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
	private static final String TOPIC_BASE_NS = "com.stock.common.model.Topic";
	private  Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static TopicService instance = new TopicService();
	private static final ExecutorService exec = Executors.newFixedThreadPool(1);//固定1个线程

	private TopicService() {

	}

	public static TopicService getInstance() {
		return instance;
	}

	/**
	 * 插入新话题(不更新summary discussNum readNum)
	 * @param topic
	 * @return
	 */
	public boolean insert(Topic topic){
		RequestMessage req = DAFFactory.buildRequest(TOPIC_BASE_NS + ".insert2", topic, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}

	/**
	 * 查询是否存在当前话题
	 * @param topic
	 * @return
	 */
	public boolean isExist(Topic topic){
		RequestMessage req = DAFFactory.buildRequest(TOPIC_BASE_NS + ".selectCount", topic, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Integer)res.getResult()>0;
	}

	/**
	 * 查询所有自定义的话题
	 * @param topic
	 * @return
	 */
	public List<Topic> selectAllCustomTopic(Topic topic) {
		RequestMessage req = DAFFactory.buildRequest(TOPIC_BASE_NS + ".selectAllCustom", topic, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Topic> list = (List<Topic>)rm.getResult();
		return list;
	}

	/**
	 * 修改话题
	 * @param topic
	 * @return
	 */
	public boolean updateStatus(Topic topic) {
		RequestMessage req = DAFFactory.buildRequest(TOPIC_BASE_NS + ".update", topic, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}

	/**
	 * 查询单个
	 * @param usubject
	 * @return
	 */
	public Topic selectSingleByCondition(Topic topic) {
		RequestMessage req = DAFFactory.buildRequest(TOPIC_BASE_NS + ".selectSingleByCondition", topic, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Topic)res.getResult();
	}

	/**
	 * 获取topic总数
	 * @param type
	 * @return
	 */
	public int getTotalCount(int type) {
		Map<String, Object> params = new HashMap<String, Object>();
		if(type > 0) {
			params.put("type", type);
		}
		RequestMessage req = DAFFactory.buildRequest(TOPIC_BASE_NS+"."+"selectCountByType", params, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		Integer i = (Integer)rm.getResult();
		return i;
	}

	/**
	 * 分页获取topics
	 *
	 * @param type
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<Topic> getTopics(int type, int offset, int limit) {
		Map<String, Object> params = new HashMap<String, Object>();
		if(type > 0) {
			params.put("type", type);
		}
		params.put("offset", offset);
		params.put("limit", limit);

		RequestMessage req = DAFFactory.buildRequest(TOPIC_BASE_NS + ".selectListByType", params, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Topic> list = (List<Topic>)rm.getResult();

		return list;
	}

	public List<Topic> fetchListFromCache() {
		List<Topic> list = TopicServiceClient.getInstance().getListFromCacheByType(TopicConstants.TOPIC_ALL_LIST);
		return list;
	}

	public Topic fetchSingleFromCache(String identify) {
		Topic tu = TopicServiceClient.getInstance().getTopicFromCache(identify);
		return tu;
	}

	public List<Topic> fetchListFromCache(List<String> identifyList) {
		List<Topic> list = TopicServiceClient.getInstance().getTopicListFromCache(identifyList);
		return list;
	}

	private QueryBuilder createQueryBuilder(String searchText, String... fieldNames) {
		return QueryBuilders.multiMatchQuery(searchText, fieldNames);
	}

	//在ES中查找昵称对应的uid
	public String searchUidByNickName(String nickName){
		Client client = ESClient.getInstance().getClient();
		QueryBuilder queryBuilder = createQueryBuilder(nickName,"nickname","uid");
//		long totalCount = getTotalCount(client, queryBuilder, "members", "member");
		if(! ESClient.isExist(client, "members")) {
			return null;
		}
		Long searchTimeoutSeconds = ConfigCenterFactory.getLong("stock_zjs.es_search_timeout_seconds", 8L);
		SearchResponse searchResponse = client.prepareSearch("members").setTypes("member").setQuery(queryBuilder).setTimeout(TimeValue.timeValueSeconds(searchTimeoutSeconds))
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setFrom(0).setSize(1).execute().actionGet();
		SearchHits shs = searchResponse.getHits();
		if(shs!=null && shs.hits().length>0){
			Map<String, Object> temp = shs.hits()[0].getSource();
			if(nickName.equals(temp.get("nickname"))){
				return 	temp.get("uid").toString();
			}
		}
		return null;
	}

	//邮件通知后台审核(异步)
	 public void notifyByEmail(final String emailText){
			exec.execute(new Runnable() {
				@Override
				public void run() {
					try{
						String emails = ConfigCenterFactory.getString("stock_zjs.notify_email_address", "526721484@qq.com");
						String[] arr = emails.split(",");
						for(String email : arr){
							if(StringUtils.isBlank(email)){
								return;
							}
							ResetMemberModel rmm = new ResetMemberModel();
							rmm.setEmailText(emailText);
							rmm.setEmail(email);
							ResetMemberService.getInstance().commonSendEmail(rmm);
						}
					}catch (Exception e) {
						Log.error("发送邮件失败",e);
					}
				}
			});

	}

	/**
	 * 官方私信
	 * @param duid
	 * @param type 0 话题通过审核 1 主持人通过审核 10 身份认证通过审核 20 身份认证未通过审核
	 * @param identify
	 * @param imgurl
	 */
	public void notifyByPLetter(long duid,int type,String identify,String imgurl){
		StringBuilder sb = new StringBuilder();
		String content = "";
		int utype = 4;
		String companyName="";
		if(duid==0 || type<0){
			log.info("notifyByPLetter fail!");
			return;
		}
		if(StringUtils.isNotBlank(identify)){
			USubject us = RemindServiceClient.getInstance().getUSubjectByUIdentifyFromCache(identify);
			if(us!=null){
				utype = us.getType();
				companyName = us.getName();
			}
		}
		Map<String,Serializable> headerMap = Maps.newHashMap();
		switch (type) {
		case 0:
			sb.append("恭喜！您的话题  <ilink>#");
			sb.append(identify);
			sb.append("#</ilink> 已经通过审核.");
			if(utype==0){
				/*sb.append("恭喜！您的话题 #");
				sb.append(identify);
				sb.append("#  已经通过审核, <a href='/company_main.html?companycode=");
				sb.append(identify);
				sb.append("&companyname=");
				sb.append(name);
				sb.append("'>查看详情</a>");*/
				headerMap.put("l", "usubject^"+identify+"^0^"+companyName);
			}else{
				/*String t = StockUtil.escape(identify);
			 	sb.append("恭喜！您的话题 #");
				sb.append(identify);
				sb.append("#  已经通过审核, <a href='/topic_detail.html?t=");
				sb.append(t);
				sb.append("'>查看详情</a>");*/
				headerMap.put("l", "usubject^"+identify);
			}
			break;
		case 1:
			if(utype==0){
			/*	sb.append("恭喜！您已成为话题 #");
				sb.append(identify);
				sb.append("#  的主持人, <a href='/company_main.html?companycode=");
				sb.append(identify);
				sb.append("&companyname=");
				sb.append(name);
				sb.append("'>查看详情</a>");*/
				sb.append("恭喜！您已成为话题 <ilink>#");
				sb.append(identify);
				sb.append("#</ilink> 的主持人.");
				headerMap.put("l", "usubject^"+identify+"^0^"+companyName);
			}else{
				/*String t = StockUtil.escape(identify);
				sb.append("恭喜！您已成为话题 #");
				sb.append(identify);
				sb.append("# 的主持人, <a href='/topic_detail.html?t=");
				sb.append(t);
				sb.append("'>查看详情</a>");*/
				sb.append("恭喜！您已成为话题 <ilink>#");
				sb.append(identify);
				sb.append("#</ilink> 的主持人.");
				headerMap.put("l", "usubject^"+identify);
			}
			break;
		case 10:
				sb.append("恭喜，您已通过身份认证，现在就去创建自己的<ilink> 话题</ilink> 吧!");
				headerMap.put("l", "topic_edit");
			break;
		case 20:
			sb.append("抱歉，您申请的主持人认证没能通过审核，请务必认真填写真实的个人资料，您可以在<ilink>个人设置</ilink>页面修改或查看详情。");
			headerMap.put("l", "user_set");
			break;
		default:
			break;
		}
		content =sb.toString();
		long suid = ConfigCenterFactory.getLong(
				"stock_zjs.system_sender",104483L);//104483 这个uid 作为给用户发官方私信的uid
		//UserEventService.getInstance().singleCastTalkMessage(suid,duid,content,imgurl);
		headerMap.put("i", imgurl);
		TalkMessageService.getInstance().singlecastTalkMessage(suid, duid, content,5,headerMap);

	}

	//调用远程接口 刷新DCSS缓存
	public void remoteUpdateTopicCache(){
		try {
			HttpRequestProxy httpRequestProxy = new HttpRequestProxy();
			String topic_url = ConfigCenterFactory.getString(
					"stock_zjs.topic_cache_url","192.168.1.110");
			//usubject 更新dcss缓存
			/*String usubject_url = ConfigCenterFactory.getString(
					"stock_zjs.usubject_cache_url","192.168.1.110");*/
			String[] ips = topic_url.split("\\^");
			/*String[] ips2 = usubject_url.split("\\^");*/
			for(String ip:ips){
				String topicurl="http://"+ip+"/dcss/cache/refreshAll?dtype=topicUser,topic";
				log.info("request topic refresh url" + topicurl);
				httpRequestProxy.doRequest(topicurl, null, null, "UTF-8");
			}
			/*for(String ip:ips2){
				String url="http://"+ip+"/dcss/cache/refreshAll?dtype=usubject";
				log.info("request usubject refresh url" + url);
				httpRequestProxy.doRequest(url, null, null, "UTF-8");
			}*/
		} catch (Exception e) {
			log.info("update topic to cache fail! ");
			e.printStackTrace();
		}
	}
	//调用远程接口 刷新公司/话题/观点 博文DCSS缓存
	public void remoteRefreshBlog(boolean refreshAll){
		try {
			String url = "/dcss/refresh/cache/blog";
			if(refreshAll){
				url = "/dcss/refresh/cache/allBlog";
			}
			HttpRequestProxy httpRequestProxy = new HttpRequestProxy();
			String topic_url = ConfigCenterFactory.getString(
					"stock_zjs.refresh_blog_url","192.168.1.110");
			String[] ips = topic_url.split("\\^");
			for(String ip:ips){
				String refreshUrl="http://"+ip+url;
				log.info("request refresh blog url" + refreshUrl);
				httpRequestProxy.doRequest(refreshUrl, null, null, "UTF-8");
			}
		} catch (Exception e) {
			log.info("update topic to cache fail! ");
			e.printStackTrace();
		}
	}
	//是否为后台管理员
	public boolean adminCheck(Members m){
		try {
			String uids = ConfigCenterFactory.getString("stock_zjs.admin_uid","10379");
			List<String> uidList = Lists.newArrayList();
			String[] arr = uids.split(",");
			for(String u : arr){
				uidList.add(u);
			}
			String uidstr = String.valueOf(m.getUid());
			if(uidList.contains(uidstr)){
				return false;
			}
			return true;
		} catch (Exception e) {
			log.info("adminCheck fail! "+e);
			return true;
		}
	}

	//更新话题缓存
	public void updateTopicIndex(String auuid){
		//评论数,转发数,赞,收藏,浏览
		final long ONE_WEEK = 7*24*60*60*1000;
		String maxNum = ConfigCenterFactory.getString("stock_zjs.max_comm_bcast_rec_fav_bro_length", "50,50,50,50,100");
		long interval = ConfigCenterFactory.getLong("stock_zjs.topic_recommend_interval", ONE_WEEK);//时间间隔
		SimpleArticle art = RemindServiceClient.getInstance().getSimpleArticle(auuid);
		String tags = art.getTags();
		long time = System.currentTimeMillis();
		if(tags==null || tags.length()==0){
			return;
		}
		long atime = art.getTime();
		long currentTime = System.currentTimeMillis();
		if(currentTime-atime>interval){//博文发布时间太久
			return;
		}
		Object tuid = art.getAttr("tuid");
		if(tuid!=null){//已经是精华博文
			return;
		}
		if(art.getType()==ShareConst.ZHUANG_ZAI){//转发的不推荐
			return;
		}
		int com = art.getComment_counts();
		int bcast = art.getBroadcast_counts();
		int rec = art.getRecommend_counts();
		int fav = art.getFavorite_counts();
		int bro = art.getBrowse_counts();
		if(art.getTags().length()>0){
			String[] arr = tags.split(",");
			for(String tag : arr){
				Topic topic = TopicService.getInstance().fetchSingleFromCache(tag);
				if(topic==null){
					continue;
				}

				String[] nums = maxNum.split(",");
				if(com>Integer.parseInt(nums[0])
						|| bcast>Integer.parseInt(nums[1])
						|| rec>Integer.parseInt(nums[2])
						|| fav>Integer.parseInt(nums[3])
						|| bro>Integer.parseInt(nums[4])){
					String summary = art.getSummary();
					Map<String,Object> map = Maps.newHashMap();
					map.put("time",time);
					map.put("identify", tag);
					map.put("summary", summary);
					map.put("title", art.getTitle());
					map.put("uuid", art.getUuid());
					updateTopicCache(map);
					doRecommend(tag, auuid);
				}
			}
		}
	}

	//更新话题缓存
	public void updateTopicCache(Map<String,Object> map) {
		String identify = String.valueOf(map.get("identify"));
		if(identify==null){
			Log.info("updateTopicCache identify is null");
			return;
		}
		TopicServiceClient.getInstance().updateCache(2, map);
	}

	/**
	 *
	 * @param us
	 * @param pwd 新密码
	 */
	public void doTopicUser(USubject us,String faceUrl){
		try {
			if ((us.getType() == 4 || us.getType() == 7) && us.getUid() == 0) {
				try {
					Members members = new Members();
					//需要过滤 空格，特别是港股"WING ON CO" 需要变成WingOnCo
					String companyBasicName = us.getUidentify();
					if(us.getType() == 4){
						companyBasicName = companyBasicName + "(话题)";
					}
					/*if(companyBasicName.length()>6){
						companyBasicName = companyBasicName.substring(0,6);
					}*/
					String userName = us.getUidentify();
					members.setNickname(companyBasicName);
					members.setUsername(companyBasicName);
					//生成一个虚拟邮箱
					members.setEmail(userName + "@toujixia.com");
					members.setTag("话题");
					members.setDesc("话题:" + us.getName()
							+ "。推荐精彩博文。");
					//members.setUcuid("话题:" + us.getName());
					members.setPassword("yfzx123456");
					members.setFaceUrl(faceUrl);
					//公司时，需要加新增一个模拟用户
					Members members2 = SqlWithTrasitionUtil.exeCallback(new ISqlCallback<Members>() {
								USubject us;
								Members members;
								private ISqlCallback<Members> set(USubject us,
										Members members) {
									this.us = us;
									this.members = members;
									return this;
								}

								public Members exeMybatis() {
									Members oldMembers = MembersService.getInstance().getMembersByAccount(members.getUsername());
									if(oldMembers == null){
										long uid = MembersService.getInstance()
												.register(members);
										if (uid > 0l) {
											us.setUid(uid);
											boolean update = USubjectService
													.getInstance().updateUid(us);
											if (update) {
												log.info(us.getName()
														+ "系统账号创建成功");
											}
											members = MembersService.getInstance()
													.getMembers(uid);
											members.setState(0);
											MembersService.getInstance()
													.updateMembers(members);
											return members;
										} else {
											return null;
										}
									}else{
										if (oldMembers.getUid() > 0l) {
											us.setUid(oldMembers.getUid());
										}
										boolean update = USubjectService
												.getInstance().updateUid(us);
										if (update) {
											log.info(us.getName()
													+ "系统账号创建成功");
										}
										return oldMembers;
									}
								}
							}.set(us, members));
					if (members2 == null) {
						log.error("创建账号失败");
					}
				} catch (Exception e) {
					log.error("创建账号失败", e);
				}
			}
			else if ((us.getType() == 4 || us.getType() == 7) && us.getUid() != 0) {
				try {
					Members members = new Members();
					members.setUid(us.getUid());
					List<Members> mlist = MembersService.getInstance()
							.getMembers(members);
					 USubjectService.getInstance().update(us);
					if (mlist != null && mlist.size() > 0) {
						Members oldMembers = mlist.get(0);
						String oldFaceUrl = oldMembers.getFaceUrl();
						//更换头像
						if(!StringUtils.isEmpty(faceUrl)){
							if(!oldFaceUrl.equals(faceUrl)){
								oldMembers.setFaceUrl(faceUrl);
								MembersService.getInstance().updateMembers(oldMembers);
							}
						}
					}
				} catch (Exception e) {
					log.error("修改账号失败", e);
				}
			}
		} catch (Exception e) {
			log.error("doTopicUser异常", e);
		}
	}

	/**
	 * 建立话题和用户的相互关注关系
	 * @param uid
	 * @param identify
	 */
	public void addTopicUserRelationship(String identify, String uid ) {
		String cacheName = StockUtil.getTopicUserRelationshipDataCacheName(identify);
		List<String> list = LCEnter.getInstance().get(identify, cacheName);
		if(list==null){
			list = Lists.newArrayList();
			LCEnter.getInstance().put(identify,list,cacheName);
		}
		if(!list.contains(uid)){
			list.add(uid);
		}
	}
	/**
	 * 建立用户和话题的相互关注关系
	 * @param uid
	 * @param identify
	 */
	public void addUserTopicRelationship(String uid, String identify ) {
		String cacheName = StockUtil.getUserTopicRelationshipDataCacheName(uid);
		List<String> list = LCEnter.getInstance().get(uid, cacheName);
		if(list==null){
			list = Lists.newArrayList();
			LCEnter.getInstance().put(uid,list,cacheName);
		}
		if(!list.contains(identify)){
			list.add(0,identify);
		}
	}
	/**
	 * 取消话题和用户的相互关注关系
	 * @param uid
	 * @param identify
	 */
	public void delTopicUserRelationship(String identify ,String uid) {
		String cacheName = StockUtil.getTopicUserRelationshipDataCacheName(identify);
		List<String> list = LCEnter.getInstance().get(identify, cacheName);
		if(list!=null && list.contains(uid)){
			list.remove(uid);
		}
	}
	/**
	 * 取消用户和话题的相互关注关系
	 * @param uid
	 * @param identify
	 */
	public void delUserTopicRelationship(String uid, String identify) {
		String cacheName = StockUtil.getUserTopicRelationshipDataCacheName(uid);
		List<String> list = LCEnter.getInstance().get(uid, cacheName);
		if(list!=null && list.contains(identify)){
			list.remove(identify);
		}
	}

	/**
	 * 获取话题-用户关注关系
	 * @param key
	 * @return
	 */
	public List<String> getTopicUserRelationship(String key) {
		String cacheName = StockUtil.getTopicUserRelationshipDataCacheName(key);
		List<String> list = LCEnter.getInstance().get(key, cacheName);
		if(list == null){
			list =Lists.newArrayList();
		}
		return list;
	}
	/**
	 * 获取用户-话题关注关系
	 * @param key
	 * @return
	 */
	public List<String> getUserTopicRelationship(String key) {
		String cacheName = StockUtil.getUserTopicRelationshipDataCacheName(key);
		List<String> list = LCEnter.getInstance().get(key, cacheName);
		if(list == null){
			list =Lists.newArrayList();
		}
		return list;
	}

	//添加话题和用户的关注关系
	public boolean focusTopic(String identify,String uid){
		try {
			Map<String,String> followMap = new HashMap<String,String>();
			Map<String,String> beFollowMap = new HashMap<String,String>();
			identify = identify.trim();
			String now = String.valueOf(System.currentTimeMillis());
			followMap.put(identify,now);
			beFollowMap.put(uid,now);
			//关注话题，通知该话题的虚拟账户
			USubject usubject = RemindServiceClient.getInstance().getUSubjectByUIdentifyFromCache(identify);
			if(usubject==null){
				log.info("没有找到缓存数据 key="+identify);
				return false;
			}
			if(usubject.getType()==StockConstants.SUBJECT_TYPE_0){
				log.info("公司话题，不建立关注关系"+identify);
				return false;
			}
			ch.insert(SAVE_TABLE.TOPICFOLLOW.toString(), uid, followMap);
			ch.insert(SAVE_TABLE.TOPICBEFOLLOW.toString(), identify, beFollowMap);
			RemindServiceClient.getInstance().addUserTopicRelationship(uid,identify);
			RemindServiceClient.getInstance().addTopicUserRelationship(identify,uid);
			long usubjectUid = usubject.getUid();
			if( usubject!=null && usubjectUid >0l){
				//建立 uid-虚拟账号 关注关系
				RemindServiceClient.getInstance().notifyUsubjectAdd(Long.valueOf(uid),usubjectUid);
				//重新加载首页博文
				long luid = Long.parseLong(uid);
				//String k = String.valueOf(usubject.getType());
				String k = "topic";
				RemindServiceClient.getInstance().addTopicArticleListWapper(luid,String.valueOf(usubject.getUid()),StockConstants.SUBJECT_TYPE_4);
			}
		} catch (Exception e) {
			log.info("focusTopic"+ e);
			return false;
		}
		return true;
	}
	//创建话题的用户同时关注话题
	public boolean focusNewTopic(String identify,String uid){
		try {
			Map<String,String> followMap = new HashMap<String,String>();
			Map<String,String> beFollowMap = new HashMap<String,String>();
			identify = identify.trim();
			String now = String.valueOf(System.currentTimeMillis());
			followMap.put(identify,now);
			beFollowMap.put(uid,now);
			//关注话题，通知该话题的虚拟账户
			USubject us = new USubject();
			us.setUidentify(identify);
			//这里要查数据库，把新建话题放入Usubject缓存
			USubject uj = new USubject();
			uj.setUidentify(identify);
			USubject usubject =  USubjectService.getInstance().getUsubject(uj);
			if(usubject==null){
				log.info("没有找到缓存数据 key="+identify);
				return false;
			}
			//放入缓存
			RemindServiceClient.getInstance().addUSubjectByUIdentifyFromCache(identify, usubject);
			if(usubject.getType()==StockConstants.SUBJECT_TYPE_0){
				log.info("公司话题，不建立关注关系"+identify);
				return false;
			}
			RemindServiceClient.getInstance().addUSubjectByUIdentifyFromCache(identify, usubject);
			ch.insert(SAVE_TABLE.TOPICFOLLOW.toString(), uid, followMap);
			ch.insert(SAVE_TABLE.TOPICBEFOLLOW.toString(), identify, beFollowMap);
			RemindServiceClient.getInstance().addUserTopicRelationship(uid,identify);
			RemindServiceClient.getInstance().addTopicUserRelationship(identify,uid);
			long usubjectUid = usubject.getUid();
			if(usubjectUid >0l){
				//建立 uid-虚拟账号 关注关系
				RemindServiceClient.getInstance().notifyUsubjectAdd(Long.valueOf(uid),usubjectUid);
				//重新加载首页博文
				long luid = Long.parseLong(uid);
				RemindServiceClient.getInstance().addTopicArticleListWapper(luid,String.valueOf(usubject.getUid()),StockConstants.SUBJECT_TYPE_4);
			}
		} catch (Exception e) {
			log.info("focusTopic"+ e);
			return false;
		}
		return true;
	}
	//取消话题和用户的关注关系
	public boolean deFocusTopic(String identify,String uid){
		try {
			Map<String,String> followMap = new HashMap<String,String>();
			Map<String,String> beFollowMap = new HashMap<String,String>();
			identify = identify.trim();
			String now = String.valueOf(System.currentTimeMillis());
			followMap.put(identify,now);
			beFollowMap.put(uid,now);
			//关注话题，通知该话题的虚拟账户
			USubject usubject = RemindServiceClient.getInstance().getUSubjectByUIdentifyFromCache(identify);
			ch.deleteName(SAVE_TABLE.TOPICFOLLOW.toString(), uid,identify);
			ch.deleteName(SAVE_TABLE.TOPICBEFOLLOW.toString(), identify,uid);
			RemindServiceClient.getInstance().delUserTopicRelationship(uid,identify);
			RemindServiceClient.getInstance().delTopicUserRelationship(identify,uid);
			if(usubject==null || usubject.getUid()==0l){
				/*USubject us = new USubject();
				us.setUidentify(identify);
				usubject = USubjectService.getInstance().getUsubject(us);
				LCEnter.getInstance().put(identify.toLowerCase(), usubject,StockUtil.getCacheName(StockConstants.DATA_TYPE_usubject));*/
				log.info("没有找到缓存数据 key="+identify);
				return false;
			}
			if(usubject.getType()==StockConstants.SUBJECT_TYPE_0){
				log.info("公司话题，不处理"+identify);
				return false;
			}
			if( usubject!=null && usubject.getUid() >0l){
				//取消 uid-虚拟账号 关注关系
				RemindServiceClient.getInstance().notifyUsubjectDelete(Long.valueOf(uid),usubject.getUid());
				/*if(usubject.getType()==TopicConst.STOCK_CHANCE_TOPIC){
					long usubjectUid = usubject.getUid();
					long luid = Long.parseLong(uid);
					RemindServiceClient.getInstance().clearStockChanceBlog(luid, String.valueOf(usubjectUid));
				}else{
					//重新加载首页博文
					long luid = Long.parseLong(uid);
					RemindServiceClient.getInstance().reloadIndexBlog(luid);
				}*/
				long usubjectUid = usubject.getUid();
				long luid = Long.parseLong(uid);
				RemindServiceClient.getInstance().clearStockChanceBlog(luid, String.valueOf(usubjectUid));
			}
		} catch (NumberFormatException e) {
			log.info(e+"");
			return false;
		}
		return true;
	}


	/**
	 * 推荐博文（关系）
	 * @param article 推荐博文
	 * @param uid 推荐者 uid
	 * @param text 推荐说明
	 * @return
	 */
	public String recommendRelationship(SimpleArticle article) {
		try {
			Object tuidObj = article.get_attr().get("tuid");
			if(tuidObj == null){
				throw new StockException(2001, "article[uuid="+article.getUuid()+"]的tuid不允许为null");
			}
			long adminUid = ConfigCenterFactory.getLong("stock_zjs.admin_blog", 10001L);
			article.putAttr("adminUid", adminUid);
			boolean success = NosqlService.getInstance().relationship2Nosql(article);
			if(success){
				MessageCenterSerivce.getInstance().relationship2Cache(article);
			}
		} catch (Exception e) {
			log.info("publishArticle "+e);
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}


	/**
	 * 修改博文（实体） 标记为已推荐
	 * @param article 推荐博文
	 * @param uid 推荐者 uid
	 * @param text 推荐说明
	 * @return
	 */
	public String reArticle(SimpleArticle article) {
		try {
			long adminUid = ConfigCenterFactory.getLong("stock_zjs.admin_blog", 10002L);
			article.putAttr("adminUid", adminUid);
			boolean success = NosqlService.getInstance().recommendArticle2Nosql(article);
			if(success){
				MessageCenterSerivce.getInstance().recommendArticle2Cache(article);
			}
		} catch (Exception e) {
			log.info("publishArticle "+e);
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}
	/**
	 * 修改博文（实体）推荐到机会广场
	 * @param article 推荐博文
	 * @param uid 推荐者 uid
	 * @param text 推荐说明
	 * @return
	 */
	public String squareArticle(SimpleArticle article) {
		try {
			long adminUid = ConfigCenterFactory.getLong("stock_zjs.admin_blog", 10003L);
			article.putAttr("adminUid", adminUid);
			boolean success = NosqlService.getInstance().recommendArticle2Nosql(article);
			if(success){
				MessageCenterSerivce.getInstance().recommendArticle2Cache(article);
			}
		} catch (Exception e) {
			log.info("publishArticle "+e);
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}
	
	/**
	 * 只推到广场
	 * @param identify
	 * @param uuid
	 * @return
	 */
	public boolean doChanceIndex(String identify,String uuid){
		long recommendTime = System.currentTimeMillis();
		String text = identify;
		SimpleArticle atl = RemindServiceClient.getInstance().getSimpleArticle(uuid);
		String suid = String.valueOf(atl.getUid());
		long usubjectUid = 0;
		USubject usubject = RemindServiceClient.getInstance().getUSubjectByUIdentifyFromCache(identify);
		if(usubject!=null && usubject.getUid()>0){
			usubjectUid = usubject.getUid();
		}
		//推荐到机会广场
		if(usubjectUid>0){		
			atl.putAttr("time", recommendTime);
			price(atl);
			//是否put到机会广场
			boolean square = putRecomendArticle2Square(identify,atl,suid,recommendTime);
			if(square){
				atl.putAttr("squareTime",recommendTime);
			}
			//延时1.5秒后存储数据到NOSQL中，同时推广场和我的订阅，可能存在覆盖写
			try {
				Thread.sleep(1500l);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
//			recommendRelationship(atl);	
			//存储入库+写入远程
			reArticle(atl);
		}
		return true;
	}


	/**
	 * 推荐【机会博文或机器精选等机器自动推荐】,如果在stock_chance.square_map的话题，则会推到机会广场
	 * 手工推荐在TopicAction.doRecommend,因为这里判断了stock_chance.check_topic_list
	 * @param identify
	 * @param uuid
	 * @return
	 */
	public boolean doRecommend(String identify,String uuid){
		long usubjectUid = 0;
		String text = identify;
		long recommendTime = System.currentTimeMillis();
		if(StringUtils.isBlank(identify) ||StringUtils.isBlank(uuid)){
			log.info("参数错误： identify="+ identify+";uuid="+uuid);
			return false;
		}
		SimpleArticle atl = RemindServiceClient.getInstance().getSimpleArticle(uuid);
		//不是推精用户，则坚持话题是否不允许自动推精
		if(ChanceCategoryService.getEssenceVpSet().contains(atl.getUid()) == false){
			String checkList = ConfigCenterFactory.getString("stock_chance.check_topic_list", "");
			if(StringUtils.isNotBlank(checkList)){
				String arr[] = checkList.split("~");
				for(String t : arr){
					if(identify.equals(t)){
						log.info("checkList : " + checkList);
						log.info("话题 #"+identify+"# 在check_topic_list列表中,不推荐为精华");
						return false;
					}
				}
			}		
		}
		
		
		if(atl.getUid()==0){
			atl = MicorBlogService.getInstance().getSimpleArticle(uuid);
			if(atl.getUid()==0){
				log.info("doRecommend 推荐的博文不存在 "+uuid);
				return false;
			}
		}
		if(atl.getType()==ShareConst.ZHUANG_ZAI){//转发的不推荐
			log.info("doRecommend 转发博文不能推荐 "+uuid);
			return false;
		}
		Object attr = atl.getAttr("tuid");
		if( attr instanceof Integer){
			return false;
		}
		Long tuid = (Long)attr;
		if(tuid!=null && tuid>0){//推荐过的话题不能再推荐了
			log.info(uuid + " 推荐过的话题博文不能再推荐了");
			return false;
		}
		USubject usubject = RemindServiceClient.getInstance().getUSubjectByUIdentifyFromCache(identify);
		if(usubject!=null && usubject.getUid()>0){
			usubjectUid = usubject.getUid();
			if(usubject.getType()==StockConstants.SUBJECT_TYPE_0){
				text =usubject.getUidentify()+":"+usubject.getName();
			}
		}else{
			log.info("doRecommend 话题不存在 "+uuid);
			return false;
		}
		Map<String,String> topicRecommend = new HashMap<String,String>();
		topicRecommend.put(uuid,String.valueOf(recommendTime));
		//推荐的博文uid - uuid关系放入nosql
		ch.insert(SAVE_TABLE.TOPICRECOMMEND.toString(), identify,topicRecommend);
		//单播 加入缓存
		UserMsg um = SMsgFactory
				.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
		String suid = String.valueOf(atl.getUid());
		um.setS(suid);
		um.setD(identify);
		um.putAttr("uuid",uuid);
		um.putAttr("identify",identify);
		um.setTime(recommendTime);
		//推荐博文标识
		um.putAttr("isRecommend", true);
		UsubjectEventService.getInstance().notifyTheEvent(um);
		//推荐到机会
		if(usubjectUid>0){
			atl.putAttr("text", text);
			atl.putAttr("tuid", usubjectUid);
			atl.putAttr("time", recommendTime);
			price(atl);
			//是否put到机会广场
			boolean square = putRecomendArticle2Square(identify,atl,suid,recommendTime);
			if(square){
				atl.putAttr("squareTime",recommendTime);
			}
			//延时1.5秒后存储数据到NOSQL中，同时推广场和我的订阅，可能存在覆盖写
			try {
				Thread.sleep(1500l);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
			recommendRelationship(atl);			
			reArticle(atl);
		}
		//缓存到话题首页的精华阅读
		Topic topic = fetchSingleFromCache(identify);
		if(topic!=null){//自定义话题
			String summary = atl.getSummary();
			Map<String,Object> map = Maps.newHashMap();
			map.put("identify", identify);
			map.put("time", recommendTime);
			map.put("summary", summary);
			map.put("title", atl.getTitle());
			map.put("uuid", uuid);
			updateTopicCache(map);
		}
		log.info("话题 #" + identify + "#推荐机会博文成功 ,uuid= "+ uuid);
		return true;
	}

	public List<Map<String,Object>> recommendTopicList(List<String> nameList){
		List<Map<String,Object>> result = Lists.newArrayList();
		if(nameList==null || nameList.size()==0){
			return result;
		}
		List<Topic> topicReduce = RemindServiceClient.getInstance().getTopicReduce(nameList.get(0), nameList);
		for(Topic t : topicReduce){
			 Map<String,Object> map = Maps.newHashMap();
			 int focusNum = 0;
			 String identify = t.getUidentify();
			 List<String> set = RemindServiceClient.getInstance().getTopicUserRelationship(identify);
			 if(set!=null){
				focusNum  = set.size();
			 }
			 map.put("fans", focusNum);
			 map.put("name", identify);
			 map.put("url", t.getLeadimgurl());
			 map.put("lead", t.getLead());
			 result.add(map);
		 }
			return result;
	}

	public List<Map<String,Object>> topicArticleListSortByTime(List<String> uList,long time){
		List<Map<String,Object>> listMap = Lists.newArrayList();
		for(String identify : uList){
			List<SimpleArticle> alist = RemindServiceClient.getInstance().getNextUsubjectFollowFavoriteList(identify, time, 1, 500);
			if(alist!=null){
				for(SimpleArticle sa : alist){
					if(sa==null || StringUtils.isBlank(sa.getUuid()) || StringUtils.isBlank(sa.getTags())){
						continue;
					}
					String stockcode = "";
					if(StringUtils.isNotBlank(sa.getTags())) {//高手调仓1,01831.hk:十方控股
						for(String tag : sa.getTags().split(",")) {
							tag = tag.split(":")[0];
							if(StringUtils.isNotBlank(tag) && (tag.endsWith(".sz") || tag.endsWith(".sh") || tag.endsWith(".hk"))) {
								stockcode = tag;
								break;
							}
						}
					}
					if(StringUtils.isNotBlank(stockcode)) {
						Map<String,Object> rmap = Maps.newHashMap();
						rmap.put("uuid", sa.getUuid());
						rmap.put("time", sa.getTime());
						rmap.put("stockcode", stockcode);
						rmap.put("topicName", identify);
						rmap.put("price", sa.getAttr("price"));
						listMap.add(rmap);
					}
				}
			}
		}
		Collections.sort(listMap,new Comparator<Map<String,Object>>(){
			public int compare(Map<String,Object> m1,Map<String,Object> m2){
				Long t1 = m1.get("time") == null ? 0L : (Long)m1.get("time");
				Long t2 = m2.get("time") == null ? 0L : (Long)m2.get("time");
				if(t2 > t1) {
					return -1;
				} else if(t2 < t1) {
					return 1;
				} else if(t2 == t1) {
					return 0;
				}
				return 0;
			}
		});

		return listMap;
	}

	public List<Map<String,Object>> recommendArticleListSortByTime(List<String> uList,long time){
		List<Map<String,Object>> listMap = Lists.newArrayList();
		for(String identify : uList){
			Map<String,Object> map = RemindServiceClient.getInstance().getNextRecommendFollowFavoriteList(identify, time, 1, 500);
			if(map!=null){
				List<SimpleArticle> alist = (List<SimpleArticle>)map.get("saList");
				if(alist==null){
					continue;
				}
				for(SimpleArticle sa : alist){
					if(sa==null || sa.getUuid()==null || sa.getAttr("time")==null || sa.getTags()==null){
						continue;
					}
					String stockcode = "";
					if(StringUtils.isNotBlank(sa.getTags())) {
						for(String tag : sa.getTags().split(",")) {
							if(StringUtils.isNotBlank(tag) && (tag.endsWith(".sz") || tag.endsWith(".sh") || tag.endsWith(".hk"))) {
								stockcode = tag;
								break;
							}
						}
					}
					if(StringUtils.isNotBlank(stockcode)) {
						Map<String,Object> rmap = Maps.newHashMap();
						rmap.put("uuid", sa.getUuid());
						rmap.put("time", sa.getAttr("time"));
						rmap.put("stockcode", stockcode);
						rmap.put("topicName", identify);
						rmap.put("price", sa.getAttr("price"));
						listMap.add(rmap);
					}
				}
			}
		}
		/*Collections.sort(listMap,new Comparator<Map<String,Object>>(){
			public int compare(Map<String,Object> m1,Map<String,Object> m2){
				return ((Long)m2.get("time")).compareTo(((Long)m1.get("time")));
			}
		});*/

		return listMap;
	}
	public void price(SimpleArticle article){
		if(article == null){
			return;
		}
		String code = "";
		Double price = 0.0;
		String[] arr = article.getTags().split(",");
		for(String tag : arr){
			if(tag.indexOf(".sh")>0 || tag.indexOf(".sz")>0 || tag.indexOf(".hk")>0){
				code = tag;
				break;
			}
		}
		Company st = CompanyService.getInstance().getCompanyByCode(code);
		if(st!=null){
			price = st.getC();
		}
		article.putAttr("price", price);
	}


	/**
	 * 机会博文在话题下存一份
	 * @param identify
	 * @param uuid
	 * @return
	 */
	public boolean doTopicArticle(String identify,String uuid){
		long usubjectUid = 0;
		String text = identify;
		if(StringUtils.isBlank(identify) ||StringUtils.isBlank(uuid)){
			log.info("参数错误： identify="+ identify+";uuid="+uuid);
			return false;
		}
		SimpleArticle atl = RemindServiceClient.getInstance().getSimpleArticle(uuid);
		String tags = atl.getTags();
		if(StringUtils.isEmpty(tags)){
			tags = identify;
		}else{
			if(tags.indexOf(identify)==0){
				tags = tags + "," +	identify;
			}
		}
		atl.setTags(tags);
		if(atl.getUid()==0){
			log.info("doRecommend 推荐的博文不存在 "+uuid);
			return false;
		}
		long articleTime = atl.getTime();
		Object attr = atl.getAttr("tuid");
		if(attr instanceof Integer){
			return false;
		}
		Long tuid = (Long)attr;
		if(tuid!=null && tuid>0){//推荐过的话题不能再推荐了
			log.info(uuid + " 推荐过的话题博文不能再推荐了");
			return false;
		}
		USubject usubject = RemindServiceClient.getInstance().getUSubjectByUIdentifyFromCache(identify);
		if(usubject!=null && usubject.getUid()>0){
			usubjectUid = usubject.getUid();
			if(usubject.getType()==StockConstants.SUBJECT_TYPE_0){
				text =usubject.getUidentify()+":"+usubject.getName();
			}
		}else{
			log.info("doRecommend 话题不存在 "+uuid);
			return false;
		}
		TimeLineService.getInstance().saveTimeLine(identify, atl.getUuid(),
				TimeLineService.SAVE_TABLE.TOPIC, articleTime);
		UserMsg umTag = SMsgFactory
				.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
		umTag.setS(String.valueOf(atl.getUid()));
		umTag.setD(identify);
		umTag.putAttr("uuid", atl.getUuid());
		umTag.setTime(articleTime);
		//注册发微博消息
		umTag.putAttr("identify", identify);
		UsubjectEventService.getInstance().notifyTheEvent(umTag);
		Map<String,String> topicRecommend = new HashMap<String,String>();
		topicRecommend.put(uuid,String.valueOf(articleTime));
		//推荐的博文uid - uuid关系放入nosql
		ch.insert(SAVE_TABLE.TOPICRECOMMEND.toString(), identify,topicRecommend);
		//单播 加入缓存
		UserMsg um = SMsgFactory
				.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
		String suid = String.valueOf(atl.getUid());
		um.setS(suid);
		um.setD(identify);
		um.putAttr("uuid",uuid);
		um.putAttr("identify",identify);
		um.setTime(articleTime);
		//推荐博文标识
		um.putAttr("isRecommend", true);
		UsubjectEventService.getInstance().notifyTheEvent(um);
		//推荐到机会
		if(usubjectUid>0){
			atl.putAttr("text", text);
			atl.putAttr("tuid", usubjectUid);
			atl.putAttr("time", articleTime);
			price(atl);
			recommendRelationship(atl);
			reArticle(atl);
		}
		//缓存到话题首页的精华阅读
		Topic topic = fetchSingleFromCache(identify);
		if(topic!=null){//自定义话题
			String summary = atl.getSummary();
			Map<String,Object> map = Maps.newHashMap();
			map.put("identify", identify);
			map.put("time", articleTime);
			map.put("summary", summary);
			map.put("title", atl.getTitle());
			map.put("uuid", uuid);
			updateTopicCache(map);
		}
		log.info("话题 #" + identify + "#推荐机会博文成功 ,uuid= "+ uuid);
		return true;
	}
	
	public boolean putRecomendArticle2Square(String identify,SimpleArticle sa,String s,long recommendTime){
		boolean result = false;
		if(sa==null || sa.getAttr("squareTime")!=null){
			return result;
		}		
		boolean p = StockChanceUtil.addFromPublish;//全局的是否发布
		boolean r = StockChanceUtil.addFromRecommend;//全局的是否推精
		String order = StockChanceUtil.scMap.get(identify);
		if(order == null){
			return result;
		}
		String keys = order.split("\\|")[0];
		if(r){
			ViewpointService.getInstance(). persistSquareStockChanceWithOutUpdate(keys,sa,recommendTime);
			result = true;			
		}
		if(p && !result){ //已经发布了
			String[] arr = order.split("\\|");
			if(arr.length==2){
				boolean re = Boolean.valueOf(arr[1]);
				if(re == false){
					ViewpointService.getInstance(). persistSquareStockChanceWithOutUpdate(keys,sa,recommendTime);
					result = true;
				}
			}
		}
		boolean isCommit = ConfigCenterFactory.getInt("stock_chance.is_commit", 1)==1;
		if(result && isCommit){//commit推送
			commit2Client(keys,s);
		}
		return result;
	}
	
	//commit 话题广场
	public void commit2Client(String director,String sender){
		 UserMsg um3 = SMsgFactory
					.getBrodCastUserMsgByType(MsgConst.MSG_REMIND_CLIENT_0);
			um3.setS(sender);
			um3.setD(director);
			um3.putAttr("type", CometPushMsgType.BROADCAST_MSG);
			RemindClientHandler.getInstance().notifyTheEvent(um3);

	}
}
