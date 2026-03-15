package com.yfzx.service.share;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.share.TimeLine;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UsubjectEventService;
import com.yfzx.service.msg.event.IMessageListWapper;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.message.IMessage;

/**
 * 上市公司公告服务
 * @author wind
 *
 */
public class NoticeService {

	private final static Logger logger = LoggerFactory.getLogger(NoticeService.class);
	private static NoticeService instance = new NoticeService();

	public static NoticeService getInstance() {
		return instance;
	}
	
	public String publishStockNotice(Article article) {
		String nosqlResult = insertNoticeNosql(article);
		if(StockCodes.SUCCESS.equals(nosqlResult) == false){
			return StockCodes.ERROR;
		}
		String identify = article.getTags();		
		SimpleArticle sa = articleToSimpleArticle(article);		
		RemindServiceClient.getInstance().putSimpleArticle(article.getUuid(), sa);
		//通知tags
		UserMsg umTag = SMsgFactory
				.getSingleUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_0);
		umTag.setS(String.valueOf(article.getUid()));
		umTag.setD(identify);
		umTag.putAttr("uuid", article.getUuid());
		umTag.setTime(article.getTime());
		//注册发微博消息
		umTag.putAttr("identify", identify);
		umTag.putAttr("notice", "true");
		UsubjectEventService.getInstance().notifyTheEvent(umTag);		
		return StockCodes.SUCCESS;
	}
	
	private String insertNoticeNosql(Article article){
		try{
			CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
			TimeLineService tls = TimeLineService.getInstance();
			if(article ==null || StringUtil.isEmpty(article.getTags()) || StringUtil.isEmpty(article.getUuid()) || article.getUid() <= 0){
				return StockCodes.ERROR;
			}
			String uuid = article.getUuid();
			String identify = article.getTags();
			if(StringUtil.isEmpty(identify)){
				return StockCodes.ERROR;
			}else{
				identify = identify.split("\\^")[0];
			}
			String uid = String.valueOf(article.getUid());
			long timemillis = article.getTime();
			tls.saveTimeLine(identify,uuid, SAVE_TABLE.NOTICE, timemillis);
			tls.saveTimeLine(uid,uuid, SAVE_TABLE.NOTICE, timemillis);
			Map<String, String> map = NosqlBeanUtil.bean2Map(article);
			ch.insert(SAVE_TABLE.ARTICLE.toString(), article.getUuid(), map);
		}catch (Exception e) {
			logger.error("插入Nosql失败",e);
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}
	
	private SimpleArticle articleToSimpleArticle(Article article){
		if(article == null){
			return null;
		}
		SimpleArticle sa = new SimpleArticle();
		sa.setUid(article.getUid());
		sa.setUuid(article.getUuid());
		sa.setTags(article.getTags());
		sa.setSummary(article.getSummary());
		sa.setTime(article.getTime());
		sa.set_attr(article.get_attr());		
		sa.setTitle(article.getTitle());
		sa.setNick(article.getNick());		
		sa.setImg(article.getImg());		
		return sa;
	}
	
	/**
	 * DCSS的CacheLoadeService加载使用，它从nosql加载观点数据，
	 */
	public List<SimpleArticle> listFromNosql(String key,long endtime,int start,int num){
		TimeLineService tls = TimeLineService.getInstance();
		List<TimeLine> tlList = tls.getTimeLineListByTime(key, SAVE_TABLE.NOTICE,endtime, start, num);		
		List<SimpleArticle> saList = MicorBlogService.getInstance().getSimpleArticleList(tlList);
		return saList;
	}
	
	/**
	 * DCSS的CacheLoadeService加载使用，它从nosql加载观点数据，
	 */
	public List<SimpleArticle> listFromNosqlByTime(String key,long startTime,long endTime){
		TimeLineService tls = TimeLineService.getInstance();
		List<TimeLine> tlList = tls.getTimeLineListByTime(key, SAVE_TABLE.NOTICE,startTime,endTime,Integer.MAX_VALUE);		
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
		return RemindServiceClient.getInstance().getNextUsubjectNoticeFavoriteList(usubjectid, time, type, limit);
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
		String nkey = StockUtil.getNoticeArticleKey(code);
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
			list = eq.getMessageList(type,time, limit);
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
}
