package com.yfzx.service.msg;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jfree.util.Log;

import com.stock.common.event.NotifyEvent;
import com.stock.common.model.USubject;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.model.snn.EConst;
import com.stock.common.msg.MsgConst;
import com.stock.common.util.StockUtil;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.msg.event.EventQueueWapper;
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
public class UsubjectEventService {

	private static UsubjectEventService instance = new UsubjectEventService();
	private UsubjectEventService()
	{
		
	}
	public static UsubjectEventService getInstance()
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
		ne.setHType(EConst.EVENT_4);
		ne.setMsg(im);
		ClientEventCenter.getInstance().putEvent(ne);
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
	
	public List<IEvent> getEventListFromEventQueue(String key)
	{
		EventQueueWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq!=null)
		{
			return eq.getEventList();
		}
		return null;
	}
	
	/**
	 * @deprecated
	 * 广播系统在线级消息
	 * @param mockUid 模拟的系统用户
	 * @param msg
	 */
	public void broadcastOnlineTalkMessage(long mockUid,String msg){
		TalkMessage tm = new TalkMessage();
		//设置成广播消息,DCSS收听方式的变化，引起很多变化
		tm.setSendType(MsgConst.SEND_TYPE_1);
		tm.setS(String.valueOf(mockUid));
		tm.setD(String.valueOf(mockUid));
		tm.putAttr("isOnline", true);
		tm.putAttr("mockUid",mockUid);
		tm.setBody(msg);
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		UserEventService.getInstance().notifyTheEvent(tm);
	}
	
	/**
	 * @deprecated
	 * 广播发送 公司群消息
	 * @param uidentify 公司code或话题code
	 * @param msg
	 */
	public void broadcastUsbjectTalkMessage(String uidentify,String msg){
		USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
		if(usubject == null){
			Log.error(uidentify+"不存在对应usubject,请检查");
			return ;
		}
		long suid = usubject.getUid();
		if(suid == 0l){
			Log.error(uidentify+"不存在对应虚拟用户,请检查");
			return ;
		}
		long duid = usubject.getUid();
		TalkMessage tm = new TalkMessage();
		//设置成广播消息,DCSS收听方式的变化，引起很多变化
		tm.setSendType(MsgConst.SEND_TYPE_1);
		tm.setS(String.valueOf(suid));
		tm.setD(String.valueOf(duid));
		tm.putAttr("isUSubject", true);
		tm.putAttr("uidentify",usubject.getUidentify());
		tm.setBody(msg);
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		UserEventService.getInstance().notifyTheEvent(tm);
	}
	
	/**
	 * @deprecated  TODO mobileMsg 需要找刘泓江确定在哪里统一处理的
	 * 广播发送 公司群消息(重写)
	 * @param uidentify 公司code或话题code
	 * @param msg
	 */
	public void broadcastUsbjectTalkMessage(String uidentify,String msg,String urlType,String mobileMsg){
		USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
		if(usubject == null){
			Log.error(uidentify+"不存在对应usubject,请检查");
			return ;
		}
		long suid = usubject.getUid();
		if(suid == 0l){
			Log.error(uidentify+"不存在对应虚拟用户,请检查");
			return ;
		}
		long duid = usubject.getUid();
		TalkMessage tm = new TalkMessage();
		//设置成广播消息,DCSS收听方式的变化，引起很多变化
		tm.setSendType(MsgConst.SEND_TYPE_1);
		tm.setS(String.valueOf(suid));
		tm.setD(String.valueOf(duid));
		tm.putAttr("isUSubject", true);
		tm.putAttr("uidentify",usubject.getUidentify());
		tm.putAttr("KLineType",urlType);
		tm.putAttr("md",mobileMsg);
		tm.setBody(msg);
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		UserEventService.getInstance().notifyTheEvent(tm);
	}
	
	/*public void put2SimpleArticleWapper(String key,SimpleArticle sa,int qsize)
	{
		SimpleArticleListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
		if(eq==null)
		{
			 eq = new SimpleArticleListWapper(qsize);
			 LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
		}
		eq.put(sa);
	}*/
	
}
