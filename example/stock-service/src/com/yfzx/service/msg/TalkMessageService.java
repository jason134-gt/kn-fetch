package com.yfzx.service.msg;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.model.USubject;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.msg.MsgConst;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.nosql.NosqlService;

/**
 * @author Administrator
 * 定义 headerMap 类似HTTP协议的HEADER头部。 
 * t	Type 类型	默认值1。1:普通对话，2:公司新闻消息，3:话题或公司推荐消息，4:异动消息,5:内部消息[涉及跳转]
 * h	H1 标题	如“中国电建去年实现净利润48亿元 同比增长5%”
 * l	Link链接参数	根据type来解析，以^分割,多个参数组用~分隔。如t=2,l是新闻的来源地址，如t=3，l是推荐博文的uid^uuid，如t=4，l=code^klinktype
 * i	image图片	支持多个，以^分割
 * s	信息来源	如腾讯新闻
 * d	device手机来源	如iphone6,HTCM9
 * m	Map地址	
 */
public class TalkMessageService {

	private static TalkMessageService instance = new TalkMessageService();
	private final static Logger logger = LoggerFactory.getLogger(TalkMessageService.class);
	
	private TalkMessageService()
	{
		
	}
	public static TalkMessageService getInstance()
	{
		return instance;
	}	
	
	
	public TalkMessage singlecastTalkMessage(long suid,long duid,String content,String img){
		Map<String,Serializable> headerMap = new HashMap<String,Serializable>();
		headerMap.put("i", img);
//		headerMap.put("d", "pc");
		return this.singlecastTalkMessage(suid, duid, content, 1, headerMap);
	}
	
	
	
	/**
	 * @param suid 来源用户
	 * @param duid 目标用户
	 * @param content 内容
	 * @param type 类型  1:普通对话，2:公司新闻消息，3:话题或公司推荐消息，4:异动消息
	 * @param headerMap 类似HTTP协议的HEADER头部。
	 */
	public TalkMessage singlecastTalkMessage(long suid,long duid,String content,int type,Map<String,Serializable> headerMap){
		//设定 不能自己给自己发私信
		if(suid<=0 || duid<=0 || suid==duid || content==null){
			logger.info("参数异常 发送消息失败。");
			return null;
		}
		TalkMessage tm = new TalkMessage();
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		tm.setS(String.valueOf(suid));
		tm.setD(String.valueOf(duid));
		tm.setBody(content);
		tm.putAttr("t", type);
		head2Msg(tm, headerMap);		
		String ret = NosqlService.getInstance().saveTalkMsg(tm.getS(), tm.getD(),
				tm);
		if (StockCodes.SUCCESS.equals(ret)){
			UserEventService.getInstance().synNotifyTheEvent(tm);//.notifyTheEvent(tm);
		}		
		return tm;
	}
	
	
	/**
	 * @param suid 来源用户
	 * @param duid 目标用户
	 * @param content 内容
	 * @param type 类型  1:普通对话，2:公司新闻消息，3:话题或公司推荐消息，4:异动消息
	 * @param headerMap 类似HTTP协议的HEADER头部。
	 */
	public TalkMessage singlecastTalkMessageAsyn(long suid,long duid,String content,int type,Map<String,Serializable> headerMap){
		//设定 不能自己给自己发私信
		if(suid<=0 || duid<=0 || suid==duid || content==null){
			logger.info("参数异常 发送消息失败。");
			return null;
		}
		TalkMessage tm = new TalkMessage();
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		tm.setS(String.valueOf(suid));
		tm.setD(String.valueOf(duid));
		tm.setBody(content);
		tm.putAttr("t", type);
		head2Msg(tm, headerMap);		
		String ret = NosqlService.getInstance().saveTalkMsg(tm.getS(), tm.getD(),
				tm);
		if (StockCodes.SUCCESS.equals(ret)){
			UserEventService.getInstance().notifyTheEvent(tm);//.notifyTheEvent(tm);
		}		
		return tm;
	}
	
	/**
	 * 模拟炒股消息群发
	 * @param mockUid 模拟用户“投资订阅”
	 * @param duid 模拟用户UID
	 * @param content
	 * @param type
	 * @param headerMap
	 * @return
	 */
	public TalkMessage broadcastTalkMessageWithGame(long mockUid,long duid,String content,int type,Map<String,Serializable> headerMap){
		TalkMessage tm = new TalkMessage();
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		//设置成广播消息,DCSS收听方式的变化，引起很多变化
		tm.setSendType(MsgConst.SEND_TYPE_1);
		tm.setS(String.valueOf(mockUid));//
		tm.setD(String.valueOf(duid));
		tm.putAttr("isGame", true);//这类消息不存储
		tm.putAttr("mockUid",mockUid);		
		tm.putAttr("t", type);
		head2Msg(tm, headerMap);
		tm.setBody(content);		
		UserEventService.getInstance().notifyTheEvent(tm);
		return tm;
	}
	
	
	/**
	 * 广播系统在线级消息 此消息不存储
	 * @param mockUid 模拟的系统用户
	 * @param msg
	 */
	public TalkMessage broadcastOnlineTalkMessageWithoutSave(long mockUid,String msg,int type,Map<String,Serializable> headerMap){
		TalkMessage tm = new TalkMessage();
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		//设置成广播消息,DCSS收听方式的变化，引起很多变化
		tm.setSendType(MsgConst.SEND_TYPE_1);
		tm.setS(String.valueOf(mockUid));
		tm.setD(String.valueOf(mockUid));
		tm.putAttr("isOnline", true);//这类消息不存储
		tm.putAttr("mockUid",mockUid);		
		tm.putAttr("t", type);
		head2Msg(tm, headerMap);
		tm.setBody(msg);		
		UserEventService.getInstance().notifyTheEvent(tm);
		return tm;
	}
	
	public TalkMessage broadcastUsbjectTalkMessage(String uidentify,String msg){
		Map<String,Serializable> headerMap = new HashMap<String,Serializable>();
//		headerMap.put("d", "pc");
		return this.broadcastUsbjectTalkMessage(uidentify, msg, 1, headerMap);
	}
	
	/**
	 * 广播话题级群消息【包括公司和话题、其它】，如type=2,3,4,它的存储在DCSS的UserType7MsgHandler处且只存储接收者那份
	 * TODO h头部怎么设置 方法名要改成broadcastUsbjectTalkMessageByType4
	 * @param uidentify
	 * @param msg
	 * @param type  类型	默认值1。1:普通对话，2:公司新闻消息，3:话题或公司推荐消息，4:异动消息
	 * @param headerMap
	 */
	public TalkMessage broadcastUsbjectTalkMessage(String uidentify,String msg,int type,Map<String,Serializable> headerMap){
		USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);//RemindServiceClient.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
		if(usubject == null){
			logger.error(uidentify+"不存在对应usubject,请检查");
			return null;
		}
		long suid = usubject.getUid();
		if(suid == 0l){
			logger.error(uidentify+"不存在对应虚拟用户,请检查");
			return null;
		}
		long duid = usubject.getUid();
		TalkMessage tm = new TalkMessage();
		//设置成广播消息,DCSS收听方式的变化，引起很多变化
		tm.setSendType(MsgConst.SEND_TYPE_1);
		tm.setS(String.valueOf(suid));
		tm.setD(String.valueOf(duid));
		tm.putAttr("isUSubject", true);
		tm.putAttr("uidentify",usubject.getUidentify());
		tm.putAttr("t", type);
		head2Msg(tm, headerMap);
		tm.setBody(msg);
		String uuid = UUID.randomUUID().toString();
		tm.setUuid(uuid);
		UserEventService.getInstance().notifyTheEvent(tm);
		return tm;
	}
	
	private void head2Msg(TalkMessage tm,Map<String,Serializable> headerMap){
		if(headerMap != null){
			if(headerMap.get("i")!=null){
				tm.putAttr("i", headerMap.get("i"));
			}
			if(headerMap.get("h")!=null){
				tm.putAttr("h", headerMap.get("h"));
			}
			if(headerMap.get("l")!=null){
				tm.putAttr("l", headerMap.get("l"));
			}
			if(headerMap.get("s")!=null){
				tm.putAttr("s", headerMap.get("s"));
			}
			if(headerMap.get("d")!=null){
				tm.putAttr("d", headerMap.get("d"));
			}
			if(headerMap.get("m")!=null){
				tm.putAttr("m", headerMap.get("m"));
			}
		}
	}
}
