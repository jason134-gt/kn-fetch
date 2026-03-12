package com.yfzx.service.msg.handler.m;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.StockUtil;
import com.yfzx.service.comet.CometPushMsgType;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.event.IMessageListWapper;
import com.yfzx.service.msg.handler.c.RemindClientHandler;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.handler.IHandler;
import com.yz.mycore.msg.message.IMessage;
/**
 * 消息处理器
 *      
 * @author：杨真 
 * @date：2014-8-14
 */
public class UsubjectType0MsgHandler implements IHandler {
	private static Logger log = LoggerFactory.getLogger(UsubjectType0MsgHandler.class);
	
	@Override
	public void handle(Object o) {
		if (o == null)	
			return;
		NotifyEvent e = (NotifyEvent) o;
		UserMsg um = (UserMsg) e.getMsg();
		// comet推送 (companyType 0讨论 1精华 2异动 )
		int companyType = 0 ;
		boolean openLog = ConfigCenterFactory.getInt("stock_log.router_log", 0)==1;
		if(openLog){
			log.info("dcss_reciver UsubjectType0====>"+um.toString());
		}
		String articleUUID = um.getAttr("uuid");
		boolean isRecommend = (Boolean)(um.getAttr("isRecommend")==null?false:true);
		//boolean doDelete = (Boolean)(um.getAttr("doDelete")==null?false:true);
		String identify = um.getAttr("identify");
		String viewpoint = um.getAttr("viewpoint");
		String notice = um.getAttr("notice");
		boolean flag = false;
		String nkey2 = StockUtil.getUsubjectArticleKey(identify);
		if(viewpoint != null ){
			if(articleUUID!=null && identify!=null){
				//异动观点
				nkey2 = StockUtil.getViewpointArticleKey(identify);			
				companyType = 2;
			}
		}else if(notice != null ){
			if(articleUUID!=null && identify!=null){
				//公司公告通知
				nkey2 = StockUtil.getNoticeArticleKey(identify);			
			}
		}else if(articleUUID!=null && identify!=null ){
			//用户在公司空间页发表的博文 放在这里
			if(isRecommend){//如果是推荐博文操作 
				companyType = 1;
				nkey2 = StockUtil.getRecommendArticleKey(identify);
				IMessageListWapper eq = LCEnter.getInstance().get(nkey2, StockUtil.getEventCacheName(nkey2));
				if(eq!=null && eq.getMessageList()!=null && eq.getMessageList().size()>0){
					List<IMessage> messageList = eq.getMessageList();
					UserMsg copyum = new UserMsg();
					for(IMessage im :messageList){
							UserMsg um2 = (UserMsg)im;
							String uuidstr = um2.getAttr("uuid");
							if(articleUUID.equals(uuidstr)){
								copyum = um2;
								flag = true;
								break;
							}
					}
					if(flag){
						eq.remove(copyum);
					}
				}
			}
			//话题跟广场解耦，推精广场只依赖发送的是观点，不是普通讨论，由viewpointservice处理
//			//机会广场 原创博文才会被推荐
//			if(StockChanceUtil.scMap.size()==0){
//				StockChanceUtil.initStockChanceKeyMap();
//			}
//			if(StockChanceUtil.scMap.get(identify)!=null){
//				recommend2ChanceSquare(um);
//			}else{
//				log.info("scMap not contant this identify : " + StockChanceUtil.scMap + " " + identify);
//			}
		}
		String from = um.getAttr("commitSelf");
		UserEventService.getInstance().put2MessageListWapperBysize(nkey2, um, 500);
		//有新博文 通知客户端刷新
	    UserMsg um3 = SMsgFactory
				.getBrodCastUserMsgByType(MsgConst.MSG_REMIND_CLIENT_0);
		um3.setS(um.getS());
		um3.setD(identify);
		if(!StringUtils.isEmpty(from)){
			um3.putAttr("commitSelf", from);
		}
		um3.putAttr("companyType", companyType);
		um3.putAttr("identify", identify);
		um3.putAttr("type", CometPushMsgType.COMPANY_MAIN_PAGE);
		//um3.putAttr("time",um.getTime());
		RemindClientHandler.getInstance().notifyTheEvent(um3);
	}

	//commit 话题广场
	public void commit2Client(String keys,UserMsg um){
		 UserMsg um3 = SMsgFactory
					.getBrodCastUserMsgByType(MsgConst.MSG_REMIND_CLIENT_0);
			um3.setS(um.getS());
			um3.setD(keys);
			um3.putAttr("type", CometPushMsgType.BROADCAST_MSG);
			RemindClientHandler.getInstance().notifyTheEvent(um3);

	}
	
	//机会广场 原创博文才会被推荐
//	public void recommend2ChanceSquare(UserMsg um){
//		try {
//			boolean isSend = false;
//			boolean p = StockChanceUtil.addFromPublish;
//			boolean r = StockChanceUtil.addFromRecommend;
//			boolean hostPublish = StockChanceUtil.hostPublish;
//			String articleUUID = um.getAttr("uuid");
//			String identify = um.getAttr("identify");
//			String viewpoint = um.getAttr("viewpoint");
//			String notice = um.getAttr("notice");
//			boolean isRecommend = (Boolean)(um.getAttr("isRecommend")==null?false:true);
//			String order = StockChanceUtil.scMap.get(identify);
//			String keys = order.split("\\|")[0];
//			SimpleArticle simpleArticle = RemindServiceClient.getInstance().getSimpleArticle(articleUUID);			
//			if(simpleArticle==null){
//				log.info("博文不存在 " + articleUUID);
//				return;
//			}
//			if(simpleArticle.getAttr("squareTime")!=null){
//				log.info("博文已被推荐到机会广场了 " + simpleArticle.getUuid());
//				return;
//			}
//			if(ShareConst.ZHUANG_ZAI == simpleArticle.getType()){
//				log.info("转发的博文不能推荐到机会广场 " + simpleArticle.getUuid());
//				return;
//			}
//			if(viewpoint!=null || notice!=null){
//				log.info("其他类型的博文不推荐到机会广场" + simpleArticle.getUuid());
//				return;
//			}
//			if(isRecommend){
//				//推荐的博文在topicAciton doRecomend()处理
//				return;
//			}
//			simpleArticle.putAttr("topic", identify);
//			if(hostPublish){
//				TopicUser tu = TopicUserService.getInstance().fetchSingleFromCache(identify);
//				long author = 0;
//				if(StringUtils.isNumericSpace(um.getS())){
//					author = Long.parseLong(um.getS());
//				}
//				long hostuid = 0;
//				if(tu!=null){
//					hostuid = tu.getUid();
//				}
//				if(hostuid>0 && author == hostuid){
//					ViewpointService.getInstance(). persistSquareStockChance(keys,simpleArticle);
//					isSend = true;
//				}
//			}
//		/*	if(r && !isSend){
//				if(isRecommend){
//					ViewpointService.getInstance(). persistSquareStockChance(keys,simpleArticle);
//					isSend = true;
//				}
//			}*/
//			if(p && !isSend){
//				String[] arr = order.split("\\|");
//				if(arr.length==2){
//					boolean re = Boolean.valueOf(arr[1]);
//					if(!re){
//						ViewpointService.getInstance(). persistSquareStockChance(keys,simpleArticle);
//						isSend = true;
//					}
//				}
//			}
//			boolean isCommit = ConfigCenterFactory.getInt("stock_chance.is_commit", 1)==1;
//			if(isSend && isCommit){
//				commit2Client(keys,um);
//			}
//		} catch (Exception e) {
//			log.info("recommend2ChanceSquare "+e);
//		}
//	}
}
