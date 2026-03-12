package com.yfzx.service.msg;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.jfree.util.Log;

import com.stock.common.constants.StockCodes;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.USubject;
import com.stock.common.model.share.Comment;
import com.stock.common.model.share.ShortUser;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.model.share.TimeLine;
import com.stock.common.model.snn.EConst;
import com.stock.common.model.user.StockSeq;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.user.ArticleCommentListWapper;
import com.yfzx.service.msg.event.EventQueueWapper;
import com.yfzx.service.msg.event.IMessageListWapper;
import com.yfzx.service.msg.event.MyTalkMessageListWapper;
import com.yfzx.service.msg.handler.c.UserNotifyClientHandler;
import com.yfzx.service.msg.handler.s.UserNotifyServerHandler;
import com.yfzx.service.nosql.NosqlService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.event.IEvent;
import com.yz.mycore.msg.message.IMessage;

/**
 * 消息服务类
 *      
 * @author：杨真 
 * @date：2014-7-15
 */
public class UserEventService {

	private static UserEventService instance = new UserEventService();
	private UserEventService()
	{
		
	}
	public static UserEventService getInstance()
	{
		return instance;
	}
	public void putEvent2Cache(String key,IEvent ie)
	{
		Map<String,IEvent> iem = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(iem==null)
		{
			iem = new ConcurrentHashMap<String,IEvent>();
			LCEnter.getInstance().put(key,iem, StockUtil.getEventCacheName(key));
		}
		iem.put(ie.getKey(), ie);
	}
	public void removeEventFromCache(String key,IEvent ie)
	{
		Map<String,IEvent> iem = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(iem!=null)
		{
			iem.remove(ie.getKey());
		}
	}
	public Map<String,IEvent> getEventList(String key)
	{
		return LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
	}
	
	public int getEventListSize(String key)
	{
		Map<String,IEvent> me = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(me!=null)
			return me.keySet().size();
		return 0;
	}
	
	public void notifyTheEvent(IMessage im)
	{
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_3);
		ne.setMsg(im);
		ClientEventCenter.getInstance().putEvent(ne);
	}
	//同步TCP
	public void synNotifyTheEvent(IMessage im)
	{
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_3);
		ne.setMsg(im);
		UserNotifyClientHandler.getInstance().sendSyncSingleMessage(ne);
	}
	
	public void putEventListWapper2Cache(String key,IEvent ie)
	{
		Map<String,IEvent> iem = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(iem==null)
		{
			iem = new ConcurrentHashMap<String,IEvent>();
			LCEnter.getInstance().put(key,iem, StockUtil.getEventCacheName(key));
		}
		iem.put(ie.getKey(), ie);
	}
	
	public void putEventQueue2Cache(String key,EventQueueWapper eq)
	{
		LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
	}
	
	public void putEvent2Queue(String key,IEvent ie)
	{
		EventQueueWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq==null)
		{
			 eq = new EventQueueWapper();
			 putEventQueue2Cache(key, eq);
		}
		eq.put(ie);
	}
	
	public void putEvent2Queue(String key,IEvent ie,int qsize,long experied)
	{
		EventQueueWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq==null)
		{
			 eq = new EventQueueWapper(qsize,experied);
			 putEventQueue2Cache(key, eq);
		}
		eq.put(ie);
	}
	public void put2MessageListWapper(String key,IMessage ie)
	{
		IMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq==null)
		{
			eq = new IMessageListWapper();
			LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
		}
	/*	if(ie instanceof TalkMessage){
			if(((TalkMessage) ie).getStatus()==0){
				eq.setUnReadCount(eq.getUnReadCount()+1);
			}
		}*/
		
		eq.put(ie);
	}
	
	public void put2MessageListWapperBysize(String key,IMessage ie,int qsize)
	{
		IMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq==null)
		{
			eq = new IMessageListWapper(qsize);
			LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
		}
		eq.put(ie);
	}
	
	public void clearIMessageListWapper(String key){
		IMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq==null)
		{
			
		}else{
			eq.clear();
		}
	}
	public void clearIMessageListWapper(String key,List<String> uuidList,boolean isInit){
		if(uuidList==null){
			return;
		}
		IMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq!=null)
		{
			 List<IMessage> messageList = eq.getMessageList();
			 List<IMessage> removeList = Lists.newArrayList();
			 if(messageList!=null){
				 for(IMessage  m : messageList){
					 UserMsg um = (UserMsg)m;
					 if(uuidList.contains(um.getAttr("uuid"))){
						 removeList.add(m);
					 }
				 }
				 eq.removeAll(removeList);
				 /*int size = eq.getMessageList().size();
				 int minSize = ConfigCenterFactory.getInt("stock_zjs.min_blog_size", 30);
				 if(size < minSize && isInit){
					 String uidstr = key.split("\\^")[0];
					 long uid = Long.parseLong(uidstr);
					 MicorBlogService.getInstance().initUserFavoriteWapper(uid);
				 }*/
			 }
		}
	}
	
	public void put2TalkMessageListWapper(String key,TalkMessage ie)
	{
		MyTalkMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq==null)
		{
			eq = new MyTalkMessageListWapper();
			eq.setKey(key);
			LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
		}
		eq.put(ie);
	}
	
	public List<IEvent> getEventListFromEventQueue(String key)
	{
		EventQueueWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq!=null)
		{
			return eq.getEventList();
		}
		return null;
	}
	
	
/*	public void put2SimpleArticleWapper(String key,SimpleArticle sa,int qsize)
	{
		SimpleArticleListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq==null)
		{
			 eq = new SimpleArticleListWapper(qsize);
			 LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
		}
		eq.put(sa);
	}*/
	public void put2ArticleCommentWapper(String key,List<Comment> c,int qsize)
	{
		ArticleCommentListWapper eq = LCEnter.getInstance().get(key, StockUtil.getCommentCacheName(key));
		if(eq==null)
		{
			eq = new ArticleCommentListWapper(qsize);
			LCEnter.getInstance().put(key,eq, StockUtil.getCommentCacheName(key));
		}else{
			eq.clear();
		}
		eq.putAll(c);
	}
	
	public ArticleCommentListWapper getArticleCommentWapper(String key)
	{
		return LCEnter.getInstance().get(key, StockUtil.getCommentCacheName(key));
	}
	
	
	/**
	 * @deprecated
	 * 组装发私信消息体 
	 * @param suid
	 * @param duid
	 * @param content
	 * @param img
	 */
	public void singleCastTalkMessage(long suid,long duid,String content,String img){
		if(suid<=0 || duid<=0 || content==null){
			Log.info("参数异常 发送消息失败。");
			return;
		}
		TalkMessage tm = new TalkMessage();
		tm.setS(String.valueOf(suid));
		tm.setD(String.valueOf(duid));
		tm.setBody(content);
		if(img!=null){
			tm.putAttr("img", img);
		}
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		if(!tm.getD().equals(tm.getS())){//设定 不能自己给自己发私信
			String ret = NosqlService.getInstance().saveTalkMsg(tm.getS(), tm.getD(),
					tm);
			if (StockCodes.SUCCESS.equals(ret)){
				UserEventService.getInstance().notifyTheEvent(tm);
			}
		}
	}
}
