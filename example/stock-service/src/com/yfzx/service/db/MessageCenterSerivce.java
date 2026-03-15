package com.yfzx.service.db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.ShareConst;
import com.stock.common.constants.StockCodes;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.ArticleUtil;
import com.stock.common.model.share.Comment;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.snn.EConst;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CommonUtil;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.UsubjectEventService;
import com.yfzx.service.nosql.NosqlService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.ViewpointService;
import com.yfzx.service.util.StockChanceUtil;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.msg.ClientEventCenter;

public class MessageCenterSerivce {
	private static Logger log = LoggerFactory.getLogger(MessageCenterSerivce.class);
	private static MessageCenterSerivce instance = new MessageCenterSerivce();
	private MessageCenterSerivce() {


	}
	public static MessageCenterSerivce getInstance() {
		return instance;
	}
	
	/**
	 *  我收到的评论   我发出的评论 @我的评论 缓存到dcss
	 * @param content 我的评论内容
	 * @param auuid article uuid 博文uuid
	 * @param cuuid comment uuid 我回复的评论uuid
	 * @param mycuuid my comment uuid 我的评论uuid
	 * @param myuid 我的uid
	 * @param currentTimeMillis 我的评论时间
	 * @param isReplayComment true：回复评论 false：回复博文
	 */
	public void pushMsg(String content,String auuid,String cuuid,String mycuuid,long myuid,long currentTimeMillis,boolean isReplayComment){
		String simpleContent="";
		Set<String> atSet = Sets.newHashSet();
		Long puid = null;//博文作者uid
		Long cuid = null;//评论者uid
		Article a = MicorBlogService.getInstance().getArticleBySimple(auuid);//获取原文
		if(a==null){
			return;
		}
		simpleContent = a.getSummary();
		puid = a.getUid();
		String sender = String.valueOf(myuid);
		String reciver = String.valueOf(a.getUid());

		if(isReplayComment){
			Comment replyComment = MicorBlogService.getInstance().getComment(auuid, cuuid);
			if(replyComment!=null){
				reciver = String.valueOf(replyComment.getUid());
				simpleContent = replyComment.getContent();
				cuid = replyComment.getUid();
			}
			//回复评论时 推送给评论者
			if(!(String.valueOf(myuid).equals(reciver))){//当推送对象是自己时 不予推送
				UserMsg um = SMsgFactory
						.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_4);
				um.setS(sender);
				um.setD(reciver);
				um.putAttr("puuid",auuid);//文章uuid
				um.putAttr("puid",puid);//博文作者uid
				um.putAttr("cuid",cuid);//评论者uid
				um.putAttr("simpleContent",simpleContent);
				um.putAttr("comment", content);
				um.putAttr("isReplayComment",true);//true ：回复评论 false:回复博文
				um.putAttr("cuuid",cuuid);//被评论的UUID
				um.putAttr("mycuuid",mycuuid.toString());//评论的UUID
				um.putAttr("msgTime",String.valueOf(currentTimeMillis));//评论时间
				pushMsgByType(um,MsgConst.MSG_USER_TYPE_4);
			}
		}
		String author = String.valueOf(puid);
		if(!author.equals(String.valueOf(cuid))){
			//推送 博文作者收到的评论
			if(!(String.valueOf(myuid).equals(author))){//当推送对象是自己时 不予推送
				UserMsg um = SMsgFactory
						.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_4);
				um.setS(sender);
				um.setD(author);
				um.putAttr("puuid",auuid);//文章uuid
				um.putAttr("puid",puid);//博文作者uid
				um.putAttr("cuid",cuid);//评论者uid
				um.putAttr("simpleContent",simpleContent);
				um.putAttr("comment", content);
				um.putAttr("isReplayBlog",true);//true ：评论博文
				um.putAttr("isReplayComment",isReplayComment);//true ：回复评论 false:回复博文
				um.putAttr("cuuid",cuuid);//被评论的UUID
				um.putAttr("mycuuid",mycuuid.toString());//评论的UUID
				um.putAttr("msgTime",String.valueOf(currentTimeMillis));//评论时间
				pushMsgByType(um,MsgConst.MSG_USER_TYPE_4);
			}
		}

		//推送 我发出的评论
		UserMsg um2 = SMsgFactory
				.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_14);
		um2.setS(reciver);
		um2.setD(sender);
		um2.putAttr("puid",puid);//博文作者uid
		um2.putAttr("cuid",myuid);//评论者uid
		um2.putAttr("puuid",auuid);//文章uuid
		um2.putAttr("simpleContent",simpleContent);
		um2.putAttr("comment", content);
		um2.putAttr("isReplayComment",isReplayComment);//true ：回复评论 false:回复博文
		um2.putAttr("cuuid",cuuid);//被评论的UUID
		um2.putAttr("mycuuid",mycuuid.toString());//评论的UUID
		um2.putAttr("msgTime",String.valueOf(currentTimeMillis));//评论时间
		long yunPushId = ConfigCenterFactory.getLong("stock_zjs.yunPushId",
				104600l);
		if(puid != null && yunPushId != puid.longValue()) {//不发送消息给云推送账号
			pushMsgByType(um2,MsgConst.MSG_USER_TYPE_14);
		}
		//推送@我的评论
		String[] atSomeone = CommonUtil.matcherNames(content);
		for(String at : atSomeone){
			if(StringUtils.isEmpty(at)){
				continue;
			}
			 atSet.add(at);
		}
		if(atSet.size()>0){
			Iterator<String> iterator = atSet.iterator();
			while (iterator.hasNext()) {
				String atUid = String.valueOf(UserServiceClient.getInstance().getUidByNickname(iterator.next()));;
				if(!(String.valueOf(myuid).equals(atUid))){
					UserMsg um3 = SMsgFactory
							.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_13);
					um3.setS(String.valueOf(sender));
					um3.setD(atUid);
					um3.putAttr("cuid",cuid);//评论者uid
					um3.putAttr("puuid",auuid);//文章uuid
					um3.putAttr("puid",String.valueOf(a.getUid()));//文章作者uid
					um3.putAttr("pnickname",a.getNick());//文章作者昵称
					um3.putAttr("simpleContent",simpleContent);
					um3.putAttr("comment", content);
					um3.putAttr("cuuid",cuuid);//被评论的UUID
					um3.putAttr("mycuuid",mycuuid.toString());//评论的UUID
					um3.putAttr("msgTime",String.valueOf(currentTimeMillis));//评论时间
					atComment(um3);
				}

			}
		}

	}

	/**
	 * 推送评论
	 * @param a
	 */
	private void pushMsgByType(UserMsg um,int msgType) {
		// 先持久化
		String ret = NosqlService.getInstance().saveMsg(um.getD(),
				String.valueOf(msgType), um);
		if (StockCodes.SUCCESS.equals(ret))
			UserEventService.getInstance().notifyTheEvent(um);
	}
	/**
	 *  at我的评论
	 * @param a
	 */
	private void atComment(UserMsg um) {
		// 先持久化
		String ret = NosqlService.getInstance().saveMsg(um.getD(),
				String.valueOf(MsgConst.MSG_USER_TYPE_13), um);
		if (StockCodes.SUCCESS.equals(ret))
			UserEventService.getInstance().notifyTheEvent(um);
	}

	//消息推送（关注 ）
	public void sendFollowMsg(long uid,long fuid) {
		UserMsg um = SMsgFactory
				.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_5);
		um.setS(String.valueOf(uid));//粉丝
		um.setD(String.valueOf(fuid));//被关注者
		//um.setUuid(String.valueOf(uid));
		long currentTimeMillis = System.currentTimeMillis();
		um.putAttr("msgTime",String.valueOf(currentTimeMillis));
		//重新加载首页博文
		RemindServiceClient.getInstance().reloadIndexBlog(uid,String.valueOf(fuid));
		// 先持久化
		String ret = NosqlService.getInstance().saveMsg(um.getD(),
				String.valueOf(MsgConst.MSG_USER_TYPE_5), um);
		if (StockCodes.SUCCESS.equals(ret))
			UserEventService.getInstance().notifyTheEvent(um);
	}

	//建立博文关系
	public void buildArticleRelationship(String uid,String fuid){
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_3);
		UserMsg um = new UserMsg();
		um.setMsgType(MsgConst.MSG_USER_TYPE_12);
		um.setS(uid);
		// 加上路由seed
		um.setD(fuid);
		StringBuffer sb = new StringBuffer();
		// 加上订阅者串
		sb.append(fuid);
		sb.append(",");
		um.putAttr("dyz", sb);// 订阅者
		((NotifyEvent) ne).setMsg(um);
		ClientEventCenter.getInstance().putEvent(ne);
	}

	/**
	 * 取消发博文关系 uid发送博文不再推送给fuid
	 * @param uid
	 * @param fuid
	 */
	public void delBlogRelationship(String uid,String fuid){
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_3);
		UserMsg um = new UserMsg();
		um.setMsgType(MsgConst.MSG_USER_TYPE_12);
		um.setS(String.valueOf(uid));
		// 加上路由seed
		um.setD(String.valueOf(fuid));
		StringBuffer sb = new StringBuffer();
		// 加上订阅者串
		sb.append(fuid);
		sb.append(",");
		um.putAttr("dyz", sb);// 订阅者
		um.putAttr("doDelete", "true");
		((NotifyEvent) ne).setMsg(um);
		ClientEventCenter.getInstance().putEvent(ne);
	}

	/**
	 *  at我的博文
	 * @param a
	 */
	public void atBlog(Article a) {
		Set<String> atSet = Sets.newHashSet();
		String suid = String.valueOf(a.getUid());
		String ats = a.getAts();
		if(ats==null || ats.length()<=0){
			return;
		}
		String[] atArr = ats.split(",");
		for (String uid : atArr) {
			String[] tArr = uid.split(":");
			String duid = tArr[0].trim();
			if(!StringUtils.isNumeric(duid)){
				duid = String.valueOf(UserServiceClient.getInstance().getUidByNickname(duid));
			}
			if(!StringUtils.isEmpty(duid)){
				atSet.add(duid);
			}
		}
		Iterator<String> iterator = atSet.iterator();
		while (iterator.hasNext()) {
			String duid = iterator.next();
			if(duid!=null){
				UserMsg um = SMsgFactory
						.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_3);
				um.putAttr("uuid", a.getUuid());
				um.setS(suid);
				um.setD(duid);
				if(!suid.equals(duid)){
					// 先持久化
					String ret = NosqlService.getInstance().saveMsg(um.getD(),
							String.valueOf(MsgConst.MSG_USER_TYPE_3), um);
					if (StockCodes.SUCCESS.equals(ret))
						UserEventService.getInstance().notifyTheEvent(um);
				}
			}
		}
	}

	/**
	 *  博文信息缓存到dcss
	 * @param article
	 */
	public void saveArtile2Cache(Article article){
		try {
			//注册发微博消息
			UserMsg um = SMsgFactory
					.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_0);
			um.setS(String.valueOf(article.getUid()));
			um.setD(String.valueOf(article.getUid()));

			SimpleArticle simpleArticle = ArticleUtil.article2simplearticle(article);		
			
			String suuid = article.getSuuid();
			// 如果是转发的博文　需要把转发信息也放在
			if(article.getType() == ShareConst.ZHUANG_ZAI && suuid!=null){
				um.putAttr("type", ShareConst.ZHUANG_ZAI);
				TopicService.getInstance().updateTopicIndex(suuid);
			}else{
				um.putAttr("type", ShareConst.YUAN_CHUANG);
			}
			um.putAttr("title",article.getTitle());
			um.putAttr("source_url",article.getSource_url());
			um.putAttr("uuid", article.getUuid());
			um.setTime(article.getTime());
			// 发表了新博文消息通知好友,我发送了新博文
			UserEventService.getInstance().notifyTheEvent(um);
			RemindServiceClient.getInstance().putSimpleArticle(article.getUuid(), simpleArticle);
			String tags =  article.getTags();
			if(!StringUtils.isEmpty(tags)){
				String[] tagArr = tags.split(",");
				// 通知相关专题 ，有新博文
				for(String identify : tagArr){
					if(StringUtils.isEmpty(identify)){
						continue;
					}
					identify = identify.split(":")[0];
					// 改为单播
					UserMsg umTag = SMsgFactory
							.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
					umTag.setS(String.valueOf(article.getUid()));
					umTag.setD(identify);
					umTag.putAttr("uuid", article.getUuid());
					umTag.setTime(article.getTime());
					//注册发微博消息
					umTag.putAttr("identify", identify);
					UsubjectEventService.getInstance().notifyTheEvent(umTag);
				}
			}
		} catch (Exception e) {
			log.info("博文存入缓存失败"+e);
		}
	}
	/**
	 *  博文信息缓存到dcss(指定话题)只针对评论和回复评论
	 * @param article
	 */
	public void saveArtile2Cache(Article article,String identify){
		try {
			SimpleArticle simpleArticle = new SimpleArticle();
			simpleArticle.setUid(article.getUid());
			simpleArticle.setNick(article.getNick());
			simpleArticle.setUuid(article.getUuid());
			simpleArticle.setTitle(article.getTitle());
			simpleArticle.setSummary(article.getSummary());
			simpleArticle.setImg(article.getImg());
			simpleArticle.setTime(article.getTime());
			simpleArticle.setArticleType(article.getArticleType());
			String suuid = article.getSuuid();
			// 如果是转发的博文　需要把转发信息也放在
			if(article.getType() == ShareConst.ZHUANG_ZAI && suuid!=null){
				simpleArticle.setType( ShareConst.ZHUANG_ZAI);
				simpleArticle.setSuuid(suuid);
				//TopicService.getInstance().updateTopicIndex(suuid);
			}
			RemindServiceClient.getInstance().putSimpleArticle(article.getUuid(), simpleArticle);
			//注册发微博消息
			UserMsg umTag = SMsgFactory
					.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
			umTag.setS(String.valueOf(article.getUid()));
			umTag.setD(identify);
			umTag.putAttr("uuid", article.getUuid());
			umTag.putAttr("commitSelf", "true");//commt标识 
			umTag.setTime(article.getTime());
			//注册发微博消息
			umTag.putAttr("identify", identify);
			UsubjectEventService.getInstance().notifyTheEvent(umTag);
		} catch (Exception e) {
			log.info("博文存入缓存失败"+e);
		}
	}
	/**
	 *  博文（关系）缓存到dcss
	 * @param article
	 */
	public void relationship2Cache(SimpleArticle article){
		try {
			String uid = String.valueOf(article.get_attr().get("tuid"));
			String identify = String.valueOf(article.get_attr().get("text"));
			long timemillis = Long.parseLong(article.get_attr().get("time").toString());
			if(StringUtils.isEmpty(uid) || timemillis<=0l){
				log.info("参数错误");
				return;
			}
			//注册发微博消息
			UserMsg um = SMsgFactory.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_16);
			um.setS(uid);
			um.setD(uid);
			String suuid = article.getSuuid();
			// 如果是转发的博文　需要把转发信息也放在
			if(article.getType() == ShareConst.ZHUANG_ZAI && suuid!=null){
				um.putAttr("type", ShareConst.ZHUANG_ZAI);
			}else{
				um.putAttr("type", ShareConst.YUAN_CHUANG);
			}
			if(StringUtils.isNotBlank(identify)){
				identify = identify.split(":")[0];
			}
			um.setTime(timemillis);
			um.putAttr("identify", identify);
			um.putAttr("uuid",article.getUuid());
			um.putAttr("title", article.getTitle());
			um.putAttr("summary", article.getSummary());
			UserEventService.getInstance().notifyTheEvent(um);
		} catch (Exception e) {
			log.info("博文存入缓存失败"+e);
		}
	}
	
	/**
	 *  话题推荐博文（实体）缓存到dcss
	 * @param article
	 */
	public void recommendArticle2Cache(SimpleArticle article){
		try {
		/*	SimpleArticle simpleArticle = new SimpleArticle();
			simpleArticle.setUid(article.getUid());
			simpleArticle.setNick(article.getNick());
			simpleArticle.setUuid(article.getUuid());
			simpleArticle.setTitle(article.getTitle());
			simpleArticle.setSummary(article.getSummary());
			simpleArticle.setImg(article.getImg());
			simpleArticle.setTime(article.getTime());
			simpleArticle.setTags(article.getTags());
			simpleArticle.setArticleType(article.getArticleType());
			Map<String, Serializable> map = article.get_attr();
			if(map!=null){
				for(String key:map.keySet()){
					simpleArticle.putAttr(key, map.get(key));
				}
			}
			String suuid = article.getSuuid();
			// 如果是转发的博文　需要把转发信息也放在
			if(article.getType() == ShareConst.ZHUANG_ZAI && suuid!=null){
				simpleArticle.setType( ShareConst.ZHUANG_ZAI);
				simpleArticle.setSuuid(suuid);
			}*/
			RemindServiceClient.getInstance().putSimpleArticle(article.getUuid(), article);
		} catch (Exception e) {
			log.info("博文存入缓存失败"+e);
		}
	}
}
