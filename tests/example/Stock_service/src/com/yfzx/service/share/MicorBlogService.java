package com.yfzx.service.share;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.collect.Sets;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.AAAConstants;
import com.stock.common.constants.AsyncTaskConstants;
import com.stock.common.constants.SCache;
import com.stock.common.constants.ShareConst;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.TopicConstants;
import com.stock.common.model.TopicUser;
import com.stock.common.model.USubject;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.ArticleMd5Content;
import com.stock.common.model.share.Comment;
import com.stock.common.model.share.ComparatorShortUser;
import com.stock.common.model.share.ComparatorTimeLine;
import com.stock.common.model.share.Favorite;
import com.stock.common.model.share.RelativeArticle;
import com.stock.common.model.share.SUser;
import com.stock.common.model.share.ShortUser;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.model.share.TimeLine;
import com.stock.common.model.share.UserExt;
import com.stock.common.model.share.Viewpoint;
import com.stock.common.model.user.Members;
import com.stock.common.model.user.StockSeq;
import com.stock.common.model.user.UserStock;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.CommonUtil;
import com.stock.common.util.CookieUtil;
import com.stock.common.util.JSEscape;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.chance.ChanceCategoryService;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.db.MessageCenterSerivce;
import com.yfzx.service.db.TopicService;
import com.yfzx.service.db.TopicUserService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.user.MembersService;
import com.yfzx.service.db.user.StockSeqService;
import com.yfzx.service.db.user.UserStockService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.UsubjectEventService;
import com.yfzx.service.msg.event.IMessageListWapper;
import com.yfzx.service.msg.event.MyTalkMessageListWapper;
import com.yfzx.service.msgpush.MobileMsgPushService;
import com.yfzx.service.nosql.NosqlService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yfzx.service.spider.StockSpider;
import com.yfzx.service.stockgame.StockGameFollowService;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.config.BaseConfiguration;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.message.IMessage;

/**
 * @author wind 轻博客服务 提供各种功能 如关注，发文章，评论等功能
 */
public class MicorBlogService {

	private static List<Map<String, String>> grouthTag = new ArrayList<Map<String,String>>();

	private static List<Map<String, String>> characterTag = new ArrayList<Map<String,String>>();

	private static List<Map<String, String>> importNoticeTag = new ArrayList<Map<String,String>>();

	private static List<String> pcCreportFinanceTipTag = new ArrayList<String>();

	private static List<String> pcCreportFinanceCautionTag = new ArrayList<String>();

	private static List<String> conclusionTags  = new ArrayList<String>();

	private static List<String> pcconclusionTags  = new ArrayList<String>();

	private static Map<String, String> conclusionTagsTipMap = new HashMap<String, String>();

	private static Map<String, String> pcconclusionTagsTipMap = new HashMap<String, String>();

	private static Set<Long> essenceVpMembers = new HashSet<Long>();

	private static Set<Long> delVpMembers = new HashSet<Long>();

	private static Map<Long, String> vpMemberMenu = new HashMap<Long, String>();

	private static Set<Long> vpMembers = new HashSet<Long>();

	private static Set<Long> excludeVpMembers = new HashSet<Long>();

	private static Set<String> dxMobileNoticeSet = new HashSet<String>();

	private static Set<String> cdxMobileNoticeSet = new HashSet<String>();

	private static Map<Long, String> subAccountMap = new HashMap<Long, String>();

	private static Map<Long, String> mockUserSubAccounts = new HashMap<Long, String>();

	private static Set<Long> mockUserSet = new HashSet<Long>();

	private static Set<Long> stockGameUserSet = new HashSet<Long>();

	private static Map<String, String> scMap = new HashMap<String, String>();

	private static Map<Long, List<Map<String, Object>>> stockgameMockUserSubAccounts = new HashMap<Long, List<Map<String, Object>>>();

	private static Map<Long, List<Map<String, Object>>> articleCommentMockUserSubAccounts = new HashMap<Long, List<Map<String, Object>>>();

	private static Map<Long, Set<Long>> recommendArticleUsersMap = new HashMap<Long, Set<Long>>();

	private static boolean doRecommend = true;
	private static Map<Long, List<Map<String, Object>>> articlePublishSubAccounts = new HashMap<Long, List<Map<String, Object>>>();

	static {
		initVp();
		initDiagnosisComapnyTag();
		initStockChanceKeyMap();
		
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				initVp();
				initStockChanceKeyMap();
				doRecommend = ConfigCenterFactory.getInt("stock_chance.publish_article_2_recommend", 1)==1;

				initDiagnosisComapnyTag();
			}
		});
	}

	private static void initStockChanceKeyMap(){
		String keyMap = ConfigCenterFactory.getString("stock_chance.square_map", "");
		if(StringUtils.isEmpty(keyMap)){
			logger.info("stock_chance.square_map is null");
			return;
		}
		scMap.clear();
		String[] arr = keyMap.split("\\^");
		for(String a :arr){
			String[] map = a.split(":");
			if(map.length>1){
				scMap.put(map[0], map[1]);
			}
		}
	}
	private static void initVp() {
		String viewpoint_members = ConfigCenterFactory.getString("stock_dbo.vpMembers", "20013,10157:essence_wvp_ecvp;104653,104654:essence_wvp_rtvp");
		String essence_view_point_members = ConfigCenterFactory.getString("stock_dbo.essenceVpMembers", "");
		String del_es_view_point_members = ConfigCenterFactory.getString("stock_dbo.delVpMembers", "");
		String exclude_vp_menu = ConfigCenterFactory.getString("stock_dbo.exclude_vp_menu", "tracetrade_adjusttrade");
		String dxMobileNoticeTag = ConfigCenterFactory.getString("stock_dbo.dxMobileNoticeTag", "");
		String cdxMobileNoticeTag = ConfigCenterFactory.getString("stock_dbo.cdxMobileNoticeTag", "");
		String subAccounts = ConfigCenterFactory.getString("stock_dbo.chanceSubAccounts", "");

		vpMemberMenu.clear();
		essenceVpMembers.clear();
		delVpMembers.clear();
		vpMembers.clear();
		excludeVpMembers.clear();
		subAccountMap.clear();

		List<String> exclude_vp_menu_list = new ArrayList<String>();
		if(StringUtils.isNotBlank(exclude_vp_menu)) {
			for(String str : exclude_vp_menu.split(",")) {
				exclude_vp_menu_list.add(str);
			}
		}

		if(StringUtils.isNotBlank(dxMobileNoticeTag)) {
			dxMobileNoticeSet.clear();
			for(String str : dxMobileNoticeTag.split(",")) {
				dxMobileNoticeSet.add(str);
			}
		}
		if(StringUtils.isNotBlank(cdxMobileNoticeTag)) {
			cdxMobileNoticeSet.clear();
			for(String str : cdxMobileNoticeTag.split(",")) {
				cdxMobileNoticeSet.add(str);
			}
		}

		if(StringUtils.isNotBlank(viewpoint_members)) {
			//20013,10157:essence_wvp_ecvp;104653,104654:essence_wvp_rtvp
			for(String tm : viewpoint_members.split(";")) {//20013,10157:essence_wvp_ecvp
				String[] arr = tm.split(":");
				for(String s : arr[0].split(",")) {//20013,10157
					Long uid = Long.valueOf(s);
					vpMemberMenu.put(uid, arr[1]);
					if(exclude_vp_menu_list.contains(arr[1])) {
						excludeVpMembers.add(uid);
					}
					vpMembers.add(uid);
				}
			}
		}

		if(StringUtils.isNotBlank(essence_view_point_members)) {
			String[] members = essence_view_point_members.split(",");
			for(String id : members) {
				if(StringUtils.isNumeric(id)) {
					essenceVpMembers.add(Long.valueOf(id));
				}
			}
		}
		if(StringUtils.isNotBlank(del_es_view_point_members)) {
			String[] members = del_es_view_point_members.split(",");
			for(String id : members) {
				if(StringUtils.isNotBlank(id)) {
					delVpMembers.add(Long.valueOf(id));
				}
			}
		}
		if(StringUtils.isNotBlank(subAccounts)) {//1:2,3,4;23:22,21
			for(String item : subAccounts.split(";")) {
				String[] uids = item.split(":");//1:2,3,4
				Long uid = Long.valueOf(uids[0]);
				subAccountMap.put(uid, uids[1]);
			}
		}

		String mockUserSubAccountStr = ConfigCenterFactory.getString("stock_dbo.mockUserSubAccounts", "");
		if(StringUtils.isNotBlank(mockUserSubAccountStr)) {//9:1,2,3;1023:4,5,6
			mockUserSubAccounts.clear();
			mockUserSet.clear();
			for(String item : mockUserSubAccountStr.split(";")) {
				String[] arr = item.split(":");
				mockUserSet.add(Long.valueOf(arr[0]));
				mockUserSubAccounts.put(Long.valueOf(arr[0]), arr[1]);
			}
		}

		String recommendArticleUsers = ConfigCenterFactory.getString("stock_dbo.recommend_article_users", "");//20013:25,26,104672,104802
		getSubAccountUids(recommendArticleUsers, recommendArticleUsersMap);

		String stockgameAdjustStocksSubAccounts = ConfigCenterFactory.getString("stock_dbo.stockgameAdjustStocksSubAccounts", "");
		getSubAccount(stockgameAdjustStocksSubAccounts, stockgameMockUserSubAccounts);

		String article_comment_sub_accounts = ConfigCenterFactory.getString("stock_dbo.article_comment_sub_accounts", "");
		getSubAccount(article_comment_sub_accounts, articleCommentMockUserSubAccounts);

		String article_publish_sub_accounts = ConfigCenterFactory.getString("stock_dbo.article_publish_sub_accounts", "");
		getSubAccount(article_publish_sub_accounts,  articlePublishSubAccounts);

		String stockgameAdStockAccounts = ConfigCenterFactory.getString("stock_dbo.stockgameAdStockAccounts", "");
		if(StringUtils.isNotBlank(stockgameAdStockAccounts)) {
			stockGameUserSet.clear();
			for(String str : stockgameAdStockAccounts.split(",")) {
				stockGameUserSet.add(Long.valueOf(str));
			}
		}
	}

	public Set<Long> getTopicRecommendArticleUids(Long uid) {
		return recommendArticleUsersMap.get(uid);
	}

	private static void getSubAccountUids(String accounts, Map<Long, Set<Long>> map) {
		if(StringUtils.isNotBlank(accounts)) {//9:1,2,3;1023:4,5,6
			map.clear();
			for(String item : accounts.split(";")) {
				String[] arr = item.split(":");//9:1,2,3
				Set<Long> uidSet = new HashSet<Long>();
				for(String str : arr[1].split(",")) {
					uidSet.add(Long.valueOf(str));
				}
				map.put(Long.valueOf(Long.valueOf(arr[0])), uidSet);
			}
		}
	}

	private static void getSubAccount(String article_comment_sub_accounts, Map<Long, List<Map<String, Object>>> map) {
		if(StringUtils.isNotBlank(article_comment_sub_accounts)) {//9:1,2,3;1023:4,5,6
			map.clear();
			for(String item : article_comment_sub_accounts.split(";")) {
				String[] arr = item.split(":");//9:1,2,3
				List<Map<String, Object>> userMList = new ArrayList<Map<String,Object>>();
				UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(Long.valueOf(arr[0]));
				if(userExt != null) {
					Map<String, Object> userM = new HashMap<String, Object>();
					userM.put("uid", userExt.getUid());
					userM.put("nick", userExt.getNickname());
					userMList.add(userM);
				}
				for(String str : arr[1].split(",")) {
					userExt = UserServiceClient.getInstance().getUserExtByUid(Long.valueOf(str));
					if(userExt != null) {
						Map<String, Object> userM = new HashMap<String, Object>();
						userM.put("uid", userExt.getUid());
						userM.put("nick", userExt.getNickname());
						userMList.add(userM);
					}
				}
				map.put(Long.valueOf(Long.valueOf(arr[0])), userMList);
			}
		}
	}

	public List<Map<String, Object>> getArticlePublishSubAccounts(Long uid) {
		return articlePublishSubAccounts.get(uid);
	}

	public List<Map<String, Object>> getArticleCommentSubAccounts(Long uid) {
		return articleCommentMockUserSubAccounts.get(uid);
	}

	public boolean hasStockGameAdVp(Long uid) {
		return stockGameUserSet.contains(uid);
	}

	public List<Map<String, Object>> getStockGameSubAccount(Long uid) {
		return stockgameMockUserSubAccounts.get(uid);
	}

	public boolean excludeEssenceVp(Long uid) {
		return excludeVpMembers.contains(uid);
	}

	public boolean hasEssenceVp(long uid) {
		return essenceVpMembers.contains(uid);
	}

	public boolean hasDelVp(Long uid) {
		return delVpMembers.contains(uid);
	}

	public boolean hasVp(Long uid) {
		return vpMembers.contains(uid);
	}

	public Map<Long, String> getVpMemberMenu() {
		return vpMemberMenu;
	}

	public boolean isDxMobileNoticeTag(String tag) {
		return dxMobileNoticeSet.contains(tag);
	}

	public boolean isCdxMobileNoticeTag(String tag) {
		return cdxMobileNoticeSet.contains(tag);
	}

	public String getStockChanceToolMenu() {
		return ConfigCenterFactory.getString("stock_dbo.stock_chance_tool_menu", "essence_wvp_ecvp^精华观点;essence_wvp_rtvp^实时观点;essence_wvp_mgvp^消息观点");
	}

	public String getMobileStockChanceToolMenu() {
		return ConfigCenterFactory.getString("stock_dbo.mobile_stock_chance_tool_menu", "essence_wvp_ecvp^精华观点;essence_wvp_rtvp^实时观点;essence_wvp_mgvp^消息观点");
	}

	public List<Map<String, Object>> getSubAccountsChanceInfo(Long uid) {
		boolean hasVp = false;
		if(! MicorBlogService.getInstance().excludeEssenceVp(uid)) {
			hasVp = MicorBlogService.getInstance().hasVp(uid);
		}

		if(hasVp) {
			List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
			String menu = vpMemberMenu.get(uid);
			String[] arr = menu.split("_");
			String t = arr[arr.length - 1];
			String name = TradeService.getInstance().getNameByType(t);
			String nick = "";
			UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(uid);
			if(userExt != null) {
				nick = userExt.getNickname();
			}
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("uid", uid);
			map.put("nick", nick);
			map.put("name", name);
			list.add(map);
			if(StringUtils.isNotBlank(subAccountMap.get(uid))) {
				for(String id : subAccountMap.get(uid).split(",")) {
					menu = vpMemberMenu.get(Long.valueOf(id));
					arr = menu.split("_");
					t = arr[arr.length - 1];
					name = TradeService.getInstance().getNameByType(t);
					userExt = UserServiceClient.getInstance().getUserExtByUid(Long.valueOf(id));
					if(userExt != null) {
						nick = userExt.getNickname();
					}
		//			sb.append("|").append(id).append(":").append(nick).append(":").append(name);
					map = new HashMap<String, Object>();
					map.put("uid", Long.valueOf(id));
					map.put("nick", nick);
					map.put("name", name);
					list.add(map);
				}
			}
			return list;
		}

		return null;
	}

	public List<Map<String, Object>> getMockUserSubAccounts(Long uid) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		String str = mockUserSubAccounts.get(uid);
		Map<String, Object> umap = new HashMap<String, Object>();
		umap.put("uid", uid);
		String nick = "";
		UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(uid);
		if(userExt != null) {
			nick = userExt.getNickname();
		}
		umap.put("nick", nick);
		list.add(umap);

		if(StringUtils.isNotBlank(str)) {
			for(String id : str.split(",")) {
				umap = new HashMap<String, Object>();
				umap.put("uid", Long.valueOf(id));
				userExt = UserServiceClient.getInstance().getUserExtByUid(Long.valueOf(id));
				if(userExt != null) {
					nick = userExt.getNickname();
				}
				umap.put("nick", nick);
				list.add(umap);
			}
		}
		return list;
	}

	public boolean hasMockVp(Long uid) {
		return mockUserSet.contains(uid);
	}

//	public String getSubAccountsChanceMenu(Long uid) {
//		StringBuilder sb = new StringBuilder();
//		String menu = vpMemberMenu.get(uid);
//		String[] arr = menu.split("_");
//		String t = arr[arr.length - 1];
//		String name = TradeService.getInstance().getNameByType(t);
//		String nick = "";
//		UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(uid);
//		if(userExt != null) {
//			nick = userExt.getNickname();
//		}
//		sb.append(uid).append(":").append(nick).append(":").append(name);
//		for(String id : subAccountMap.get(uid).split(",")) {
//			menu = vpMemberMenu.get(uid);
//			arr = menu.split("_");
//			t = arr[arr.length - 1];
//			name = TradeService.getInstance().getNameByType(t);
//			userExt = UserServiceClient.getInstance().getUserExtByUid(uid);
//			if(userExt != null) {
//				nick = userExt.getNickname();
//			}
//			sb.append("|").append(id).append(":").append(nick).append(":").append(name);
//		}
//		return sb.toString();
//	}

	int serial = -1;
	private static CassandraHectorGateWay ch = CassandraHectorGateWay
			.getInstance();
	private static TimeLineService tls = TimeLineService.getInstance();
	MembersService membersService = MembersService.getInstance();
	private final static Logger logger = LoggerFactory
			.getLogger(MicorBlogService.class);

	private MicorBlogService() {

	}

	private static MicorBlogService instance = new MicorBlogService();

	public static MicorBlogService getInstance() {
		return instance;
	}

	/**
	 * 用户uid收听用户fuid TODO 即用户fuid被用户uid关注
	 * 在TimeLine表中存储顺序的关系，如：uid-follow-{time1:uuid1,time2:uuid2...}
	 * 在follow表中存储数据，如 uid-fuid-{title:标题1,content:内容1,tag:标签1...}
	 */
	public String follow(Long uid, Long fuid) {
		if (uid == fuid)
			return StockCodes.ERROR;// 自己不能收听自己
		try {
			Members fUser = membersService.getMembers(fuid);
			if (fUser == null)
				return StockCodes.ERROR;// 收听的用户不存在
			if (checkFollow(uid, fuid)) {
				return StockCodes.SUCCESS;// 收听的用户已存在
			}
			String nickname = fUser.getNickname();
			String sKey = String.valueOf(uid);
			String fuidStr = String.valueOf(fuid);
			Map<String, Map<String, String>> superMap = new HashMap<String, Map<String, String>>();

			Map<String, String> followMap = new HashMap<String, String>();

			String statusVal = "0";
			Map<String, String> beMap = ch.getSuper(
					SAVE_TABLE.FOLLOW.toString(), fuidStr, sKey);// 查询fuid是否收听uid
			if (!beMap.isEmpty()) {// 已被fuid收听时，状态1=互听
				statusVal = "1";// 状态变更 1=已互听
			}
			String time = String.valueOf(System.currentTimeMillis());
			followMap.put("status", statusVal);
			followMap.put("nickname", nickname);
			followMap.put("time", time);
			superMap.put(fuidStr, followMap);// follow表中存储数据，如 uid-fuid*

			ch.insertSuper(SAVE_TABLE.FOLLOW.toString(), sKey, superMap);

			// TODO 用户fuid被用户uid关注 befollow功能块
			Map<String, Map<String, String>> besuperMap = new HashMap<String, Map<String, String>>();
			Members User = membersService.getMembers(uid);
			if (User == null)
				return StockCodes.ERROR;
			Map<String, String> befollowMap = new HashMap<String, String>();
			befollowMap.put("status", statusVal);
			befollowMap.put("nickname", User.getNickname());
			befollowMap.put("time", time);
			besuperMap.put(sKey, befollowMap);
			ch.insertSuper(SAVE_TABLE.BEFOLLOW.toString(), fuidStr, besuperMap);
			if ("1".equals(statusVal)) {
				// 修改被收听者fuid的记录
				Map<String, String> statusMap = new HashMap<String, String>();
				statusMap.put("status", statusVal);

				Map<String, Map<String, String>> stautsSuperMap = new HashMap<String, Map<String, String>>();
				stautsSuperMap.put(sKey, statusMap);
				ch.insertSuper(SAVE_TABLE.FOLLOW.toString(), fuidStr,
						stautsSuperMap);

				Map<String, Map<String, String>> stautsBesuperMap = new HashMap<String, Map<String, String>>();
				stautsBesuperMap.put(fuidStr, statusMap);
				ch.insertSuper(SAVE_TABLE.BEFOLLOW.toString(), sKey,
						stautsBesuperMap);
			}

			String s = "follow_counts";
			userExtAdd(s, sKey);
			String s2 = "befollow_counts";
			userExtAdd(s2, fuidStr);
			MessageCenterSerivce.getInstance().sendFollowMsg(uid,fuid);
			//增加关系
			RemindServiceClient.getInstance().addUserRelationShip(uid,fuid);
			//建立博文关系
			MessageCenterSerivce.getInstance().buildArticleRelationship(String.valueOf(fuid),String.valueOf(uid));
			
			//增加订阅关系
			RemindServiceClient.getInstance().addSubscribeRelationship(uid,fuid);
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	// 扩展用户信息 某某次数加1 适用于UserExt long型数据
	public void userExtAdd(String cloumn, String uid) {
		String key = uid;
		Map<String, String> getMap = ch.get(SAVE_TABLE.USER_EXT.toString(),
				key, new String[] { cloumn });
		String sValueStr = getMap.get(cloumn);
		long sValue = 0l;
		if (StringUtil.isEmpty(sValueStr) == false) {// 非空时
			try {
				sValue = Long.parseLong(sValueStr);
			} catch (Exception e) {
				logger.error("异常:" + e);
			}
		}
		sValue = sValue + 1l;
		Map<String, String> userExtMap = new HashMap<String, String>();
		userExtMap.put(cloumn, String.valueOf(sValue));
		ch.insert(SAVE_TABLE.USER_EXT.toString(), key, userExtMap);
	}

	// 扩展用户信息 某某次数减1
	private void userExtSubtract(String cloumn, String uid) {
		String key = uid;
		Map<String, String> getMap = ch.get(SAVE_TABLE.USER_EXT.toString(),
				key, new String[] { cloumn });
		String sValueStr = getMap.get(cloumn);
		long sValue = 0l;
		if (StringUtil.isEmpty(sValueStr) == false) {// 不为空时计算 为空时设为0
			try {
				sValue = Long.parseLong(sValueStr);
				sValue = sValue - 1l;
				if (sValue < 0) {
					sValue = 0;
				}
			} catch (Exception e) {
				logger.error("异常:" + e);
			}
		}
		Map<String, String> userExtMap = new HashMap<String, String>();
		userExtMap.put(cloumn, String.valueOf(sValue));
		ch.insert(SAVE_TABLE.USER_EXT.toString(), key, userExtMap);
	}

	/**
	 * 返回true=已关注 false=未关注或关注被取消
	 *
	 * @param uid
	 * @param fuid
	 * @return
	 */
	public boolean checkFollow(Long uid, Long fuid) {
		Map<String, String> followMap = ch.getSuper(
				SAVE_TABLE.FOLLOW.toString(), String.valueOf(uid),
				String.valueOf(fuid));
		if (followMap == null || followMap.isEmpty() || followMap.size() == 0) {
			return false;
		} else {
			return true;

		}
	}

	public String unfollow(Long uid, Long fuid) {
		if (uid == fuid)
			return StockCodes.ERROR;// 自己不能收听自己
		try {
			String sKey = String.valueOf(uid);
			String fuidStr = String.valueOf(fuid);
			if (checkFollow(uid, fuid) == false) {// 已经取消关注了
				return StockCodes.SUCCESS;
			}
			ch.deleteSuperColumn(SAVE_TABLE.FOLLOW.toString(), sKey, fuidStr);
			ch.deleteSuperColumn(SAVE_TABLE.BEFOLLOW.toString(), fuidStr, sKey);

			// 检查是否uid被fuid收听
			Map<String, String> followMap = ch.getSuper(
					SAVE_TABLE.FOLLOW.toString(), fuidStr, sKey);
			if (followMap == null || followMap.isEmpty()
					|| followMap.size() == 0) {
				logger.info("非互听");
			} else {
				// uid被fuid收听,则要将fuid收听uid的状态改为非互听状态,并修改fuid的记录
				Map<String, String> statusMap = new HashMap<String, String>();
				statusMap.put("status", "0");
				Map<String, Map<String, String>> stautsSuperMap = new HashMap<String, Map<String, String>>();
				stautsSuperMap.put(sKey, statusMap);
				ch.insertSuper(SAVE_TABLE.FOLLOW.toString(), fuidStr,
						stautsSuperMap);

				Map<String, Map<String, String>> stautsBesuperMap = new HashMap<String, Map<String, String>>();
				stautsBesuperMap.put(fuidStr, statusMap);
				ch.insertSuper(SAVE_TABLE.BEFOLLOW.toString(), sKey,
						stautsBesuperMap);
			}

			String s = "follow_counts";
			userExtSubtract(s, sKey);
			String s2 = "befollow_counts";
			userExtSubtract(s2, fuidStr);

			// 相应的清除消息中心中的粉丝
			RemindServiceClient.getInstance().clearMessage(fuid,MsgConst.MSG_USER_TYPE_5,String.valueOf(uid));
			//重新加载首页博文
			RemindServiceClient.getInstance().clearIndexBlog(uid,String.valueOf(fuid));
			//取消关注关系
			RemindServiceClient.getInstance().clearUserRelationship(uid, fuid);
			//需要通知DCSS fuid的私信  不用发给我了
			MessageCenterSerivce.getInstance().delBlogRelationship(String.valueOf(fuid),String.valueOf(uid));
			
			//清理订阅关系
			RemindServiceClient.getInstance().clearSubscribeRelationship(uid, fuid);
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	public List<ShortUser> getFollowList(Long uid) {
		int limit = Integer.MAX_VALUE;
		String startSn = null;
		return getFollowList(uid, startSn, limit);
	}

	public List<ShortUser> getFollowList(Long uid,String startSn,int limit){
		String sKey = String.valueOf(uid);
		Map<String, Map<String, String>> getMap = ch.getSuper(
				SAVE_TABLE.FOLLOW.toString(), sKey,startSn,limit);
		List<ShortUser> followList = new ArrayList<ShortUser>();
		if (getMap.isEmpty())
			return followList;
		Iterator<Map.Entry<String, Map<String, String>>> iterator = getMap
				.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Map<String, String>> entry = iterator.next();
			Map<String, String> entryMap = entry.getValue();
			ShortUser su = new ShortUser();
			String fuid = entry.getKey();
			su.setUid(Long.valueOf(fuid));
			su.setNickname(entryMap.get("nickname"));
			if ("1".equals(entryMap.get("status"))) {
				su.setStatus(1);
			} else {
				su.setStatus(0);
			}
			String time = entryMap.get("time");
			long timeL = 0l;
			if (StringUtil.isEmpty(time) == false) {
				try {
					timeL = Long.parseLong(time);
				} catch (Exception e) {
				}
			}
			su.setTime(timeL);
			followList.add(su);
		}
		ComparatorShortUser comparatorshortuser = new ComparatorShortUser();
		Collections.sort(followList, comparatorshortuser);
		return followList;
	}

	/**
	 * 后期需要加缓存
	 *
	 * @param uid
	 * @return
	 */
	public List<Long> getFollowUidList(Long uid) {
		String sKey = String.valueOf(uid);
		Map<String, Map<String, String>> getMap = ch.getSuper(
				SAVE_TABLE.FOLLOW.toString(), sKey);
		List<Long> followList = new ArrayList<Long>();
		if (getMap.isEmpty())
			return followList;
		Iterator<Map.Entry<String, Map<String, String>>> iterator = getMap
				.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Map<String, String>> entry = iterator.next();
			String fuid = entry.getKey();
			followList.add(Long.valueOf(fuid));
		}
		return followList;
	}

	/**
	 * 后期需要加缓存
	 *
	 * @param uid
	 * @return
	 */
	public List<Long> getBefollowUidList(Long uid) {
		String sKey = String.valueOf(uid);
		Map<String, Map<String, String>> getMap = ch.getSuper(
				SAVE_TABLE.BEFOLLOW.toString(), sKey);
		List<Long> befollowList = new ArrayList<Long>();
		if (getMap.isEmpty())
			return befollowList;

		Iterator<Map.Entry<String, Map<String, String>>> iterator = getMap
				.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Map<String, String>> entry = iterator.next();
			String fuid = entry.getKey();
			befollowList.add(Long.valueOf(fuid));
		}
		return befollowList;
	}

	public List<ShortUser> getBefollowList(Long uid) {
		int limit = Integer.MAX_VALUE;
		String startSn = null;
		return getBefollowList(uid, startSn, limit);
	}

	public List<ShortUser> getBefollowList(Long uid,String startSn,int limit){
		String sKey = String.valueOf(uid);
		Map<String, Map<String, String>> getMap = ch.getSuper(
				SAVE_TABLE.BEFOLLOW.toString(), sKey);
		List<ShortUser> befollowList = new ArrayList<ShortUser>();
		if (getMap.isEmpty())
			return befollowList;

		Iterator<Map.Entry<String, Map<String, String>>> iterator = getMap
				.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Map<String, String>> entry = iterator.next();
			String fuid = entry.getKey();
			Map<String, String> entryMap = entry.getValue();
			ShortUser su = new ShortUser();
			su.setUid(Long.valueOf(fuid));
			su.setNickname(entryMap.get("nickname"));
			if ("1".equals(entryMap.get("status"))) {
				su.setStatus(1);
			} else {
				su.setStatus(0);
			}
			String time = entryMap.get("time");
			long timeL = 0l;
			if (StringUtil.isEmpty(time) == false) {
				try {
					timeL = Long.parseLong(time);
				} catch (Exception e) {
				}
			}
			su.setTime(timeL);
			befollowList.add(su);
		}
		ComparatorShortUser comparatorshortuser = new ComparatorShortUser();
		Collections.sort(befollowList, comparatorshortuser);
		return befollowList;
	}

	/**
	 * 访客接口 TODO 可能需要重构 支持到期后[30天?]删除
	 * 在TimeLine表中存储顺序的关系，如：uid-vistor-{time1:time1,time2:time2...}
	 * 在vistor表中存储数据，如 uid-time2-{uid:用户1,vuid:内容1,tag:标签1...}
	 */
	public String addVistors(Long uid, Long vuid) {
		try {
			String sKey = String.valueOf(uid);
			String vuidStr = String.valueOf(vuid);
			long timeMillis = System.currentTimeMillis();
			String timeStr = String.valueOf(timeMillis);
			Map<String, Map<String, String>> superMap = new HashMap<String, Map<String, String>>();

			Map<String, String> vistorMap = new HashMap<String, String>();
			vistorMap.put("uid", sKey);
			vistorMap.put("vuid", vuidStr);
			vistorMap.put("date", timeStr);
			superMap.put(String.valueOf(uid), vistorMap);

			tls.saveTimeLine(sKey, timeStr, TimeLineService.SAVE_TABLE.VISTOR,
					timeMillis);
			ch.insertSuper(SAVE_TABLE.VISTOR.toString(), timeStr, superMap);
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	public String broadcast(Article b) {
		return this.publishArticle(b);
	}

	public String rebroadcast(Article b) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateBroadcastCount(Long uid, String uuid) {
		// TODO Auto-generated method stub

	}

	/*
	 * 取相关推荐
	 */
	public List<RelativeArticle> getRelateArticle(Article article) {
		// TODO Auto-generated method stub
		List<RelativeArticle> raList = null;
		try {
			// Article a = (Article)ch.get(article);
			// raList = a.getRelative_article();
		} catch (Exception e) {
			return raList;
		}
		return raList;
	}

	public String getAtFromContent(String ats, String content) {
		if(StringUtils.isBlank(content)) {
			return "";
		}
		String result = "";
		String regex = "@[_a-zA-Z\\d\\u2E80-\\uFE4F]+";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(content);
		String rep = "";
		List<String> list = new ArrayList<String>();
		while (m.find()){
		  rep = m.group();
		  if(StringUtils.isNotBlank(rep) && rep.length() > 1) {
			  String nick = rep.substring(1, rep.length()).trim();
			  if(! list.contains(nick)) {
				  list.add(nick);
			  }
		  }
		}
		if(StringUtils.isNotBlank(ats)) {
			for(String at : ats.split(",")) {
				if(StringUtils.isNotBlank(at) && ! list.contains(at)) {
					list.add(at);
				}
			}
		}
		result = StringUtils.join(list, ",");
		return result;
	}

	public String updateArticle(Article article) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			Map<String, Object> dcssMap = new HashMap<String, Object>();
			map.put("content", article.getContent());
			map.put("summary", article.getSummary());
			map.put("title", article.getTitle());
			map.put("img", article.getImg());
			map.put("articleType",String.valueOf(article.getArticleType()));
			NosqlService.getInstance().updateArticle2Nosql(article);

			String uuid = article.getUuid();
			dcssMap.put("content", article.getContent());
			dcssMap.put("summary", article.getSummary());
			dcssMap.put("title", article.getTitle());
			dcssMap.put("img", article.getImg());
			dcssMap.put("articleType", article.getArticleType());
			RemindServiceClient.getInstance().updateSimpleArticle(uuid, dcssMap);

			map.put("uuid", article.getUuid());
			map.put("tags", article.getTags());
			saveEsArticleUUids(article.getUuid(), 2);
			//dcss 更新博文修改的部分
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	public boolean checkArticlePublishTimeGap(long uid, String content, long t) {
		ArticleMd5Content a = LCEnter.getInstance().get(uid, StockConstants.ARTICLE_CONTENTMD5_CACHE);
		if(a == null) {
			a = new ArticleMd5Content();
			a.setMd5(CookieUtil.getMD5(content));
			a.setTime(t);
			LCEnter.getInstance().put(uid, a, StockConstants.ARTICLE_CONTENTMD5_CACHE);
			return true;
		} else {
			if(t - a.getTime() > ConfigCenterFactory.getLong("stock_zjs.article_push_gap", 10 * 60 * 1000L)) {
				return true;
			} else {
				String md5 = CookieUtil.getMD5(content);
				if(StringUtils.equals(md5, a.getMd5()) && t - a.getTime() > ConfigCenterFactory.getLong("stock_zjs.article_push_gap", 10 * 60 * 1000L)) {
					return false;
				} else {
					a.setTime(t);
					a.setMd5(md5);
					return true;
				}
			}
		}
	}

	/**
	 * 发布文章
	 *
	 * @param article
	 * @return 在TimeLine表中存储顺序的关系，如：uid-article-{time1:uuid1,time2:uuid2...}
	 *         --过时 在article表中存储数据，如
	 *         uid-uuid1-{title:标题1,content:内容1,tag:标签1...}-- 在Article表中存储数据
	 *         uuid1-{title:标题1,content:内容1,tag:标签1...}
	 */
	public String publishArticle(final Article article) {
		try {
			String topicName = article.getAttr("from");
			article.removeAttr("from");
			String[] atArr = null;
			String[] tagArr = null;
			String tags = article.getTags();
			String ats = article.getAts();
			String sKey = String.valueOf(article.getUid());
			if (StringUtil.isEmpty(tags)) {
				tagArr = new String[] {};
			} else {
				tagArr = tags.split(",");
			}
			if (StringUtil.isEmpty(ats)) {
			}else {
				atArr = ats.split(",");
			}
			StringUtil.sbuArticle(article);
			if(StringUtils.isEmpty(article.getTags())){
				String tag = CommonUtil.getTagsFromContent(article.getSummary());
				article.setTags(tag);
			} else {
				String tag = adjustTag(article.getTags());
				article.setTags(tag);
			}

			String summary = replaceAtUserWithUid(article.getSummary());
			article.setSummary(summary);
			if(article.getArticleType()==0 || article.getArticleType()==3){
				String content = replaceAtUserWithUid(article.getContent());
				article.setContent(content);
			}
			//Map<String, Map<String, String>> superMap = new HashMap<String, Map<String, String>>();
			article.setStatus(1);// 状态=1，正常
			// 如果没有传时间进来，则创建时间
			long timemillis = article.getTime();
			if (timemillis < 1l) {
				timemillis = System.currentTimeMillis();
				article.setTime(timemillis);
			}
			//Map<String, String> articleMap = this.article2Map(article);
			//superMap.put(article.getUuid(), articleMap);
			for (String t : tagArr) {
				// 支持两种格式 000002.sz:万科A 和 000002.sz
				if(StringUtils.isBlank(t)) {
					continue;
				}
				String identify = t.split(":")[0];
				tls.saveTimeLine(identify, article.getUuid(),
						TimeLineService.SAVE_TABLE.TOPIC, timemillis);
				TopicUser tu = TopicUserService.getInstance().fetchSingleFromCache(identify);
				if(tu!=null){//自定义话题
					if(identify.endsWith(".sz") || identify.endsWith(".sh") || identify.endsWith(".hk")){
						//公司话题暂不处理
					}else{
						int MAXLENGTH = ConfigCenterFactory.getInt("stock_zjs.maxSummaryLength", 1);
						boolean noCheck = ConfigCenterFactory.getInt("stock_zjs.update_topic_index_from_publish", 1)==1;
						Map<String,Object> map = Maps.newHashMap();
						map.put("time", article.getTime());
						map.put("identify", identify);
						/*if(tu.getUid().longValue()==article.getUid()
								&& tu.getStatus()==TopicConstants.USER_PASSED
								&& article.getSummary().length()>MAXLENGTH){*/
						if(tu.getStatus()!=TopicConstants.USER_PASSED){
							continue;
						}
						if((tu.getUid().longValue()==article.getUid()
								|| article.getArticleType()==2
								|| article.getSummary().length()>MAXLENGTH)
								&& article.getType()!=ShareConst.ZHUANG_ZAI
								&& noCheck){//代标题的长博文
							map.put("summary", article.getSummary());
							map.put("title", article.getTitle());
							map.put("uuid", article.getUuid());
						}
						TopicService.getInstance().updateTopicCache(map);
					}
				}
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
							tls.saveTimeLine(atUid, article.getUuid(),TimeLineService.SAVE_TABLE.AT, timemillis);
						}
					}
				}
			}
			if(!StringUtil.isEmpty(ats)){
				MessageCenterSerivce.getInstance().atBlog(article);//@推送
			}

			highlightKeywords(article);

			boolean success = NosqlService.getInstance().saveArticle2Nosql(article);

			if(success){
				MessageCenterSerivce.getInstance().saveArtile2Cache(article);
			}
			String s = "article_counts";
			userExtAdd(s, sKey);
			if(article.getType() == ShareConst.VIEWPOINT  && !StringUtils.isEmpty(topicName) && doRecommend){
					TopicService.getInstance().doRecommend(topicName, article.getUuid());
			}
			saveEsArticleUUids(article.getUuid(), 1);
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	private String adjustTag(String tags) {//000631.sz:顺发恒业,601126.sh:四方股份
		StringBuffer s = new StringBuffer();
		for(String tag : tags.split(",")) {
			String[] arr = tag.split(":");
			if(s.toString().contains(arr[0]) || (arr.length >= 2 && s.toString().contains(arr[1]))) {
				continue;
			}
			USubject uSubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(arr[0]);
			if(uSubject != null) {
				if(uSubject.getType() == 0) {
					s.append(uSubject.getUidentify()).append(":").append(uSubject.getName()).append(",");
				} else {
					s.append(arr[0]).append(",");
				}
			} else {
				if(arr.length >= 2) {
					uSubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(arr[1]);
					if(uSubject != null) {
						if(uSubject.getType() == 0) {
							s.append(uSubject.getUidentify()).append(":").append(uSubject.getName()).append(",");
						} else {
							s.append(arr[0]).append(",");
						}
					}
				}
			}
		}
		if(s.length() > 0){
			return s.toString().substring(0, s.toString().length() - 1);
		}else{
			return tags;
		}
	}
	public void highlightKeywords(Article article) {
		int hightKeywords = ConfigCenterFactory.getInt("stock_zjs.articleHightKeywords", 1);
		if(hightKeywords == 0) {
			return ;
		}

		String articleContent = article.getContent();
		String articleSummary = article.getSummary();

		List<USubject> cl = USubjectService.getInstance().getUSubjectListByType(StockConstants.SUBJECT_TYPE_0);
		StringBuilder companySb = new StringBuilder();
		if(cl != null && cl.size() > 0) {
			for(USubject com : cl) {
				String stockcode = com.getUidentify();
				String code  = "";
				if(StringUtils.isNotBlank(stockcode)) {
					code = stockcode.split("\\.")[0];
				}
				companySb.append(com.getName()).append("|")
				.append(stockcode).append("|")
				.append(code).append("|");
			}
		}
		String stockStr = companySb.toString().replaceAll("Ａ", "a").replaceAll("ａ", "a").replaceAll("A", "a")
				.replaceAll("Ｂ", "b").replaceAll("ｂ", "b").replaceAll("B", "b").replaceAll("\\s+", "").toLowerCase();

		article.setContent(replaceKeywords(articleContent, cl, stockStr));
		article.setSummary(replaceKeywords(articleSummary, cl, stockStr));
	}

	private String replaceKeywords(String handleContent) {
		int hightKeywords = ConfigCenterFactory.getInt("stock_zjs.articleHightKeywords", 1);
		if(hightKeywords == 0) {
			return handleContent;
		}
		if(StringUtils.isBlank(handleContent)) {
			return "";
		}
		List<USubject> cl = USubjectService.getInstance().getUSubjectListByType(StockConstants.SUBJECT_TYPE_0);
		StringBuilder companySb = new StringBuilder();
		if(cl != null && cl.size() > 0) {
			for(USubject com : cl) {
				String stockcode = com.getUidentify();
				String code  = "";
				if(StringUtils.isNotBlank(stockcode)) {
					code = stockcode.split("\\.")[0];
				}
				companySb.append(com.getName()).append("|")
				.append(stockcode).append("|")
				.append(code).append("|");
			}
		}
		String stockStr = companySb.toString().replaceAll("Ａ", "a").replaceAll("ａ", "a").replaceAll("A", "a")
				.replaceAll("Ｂ", "b").replaceAll("ｂ", "b").replaceAll("B", "b").replaceAll("\\s+", "").toLowerCase();
		return replaceKeywords(handleContent, cl, stockStr);
	}

	private String replaceKeywords(String handleContent, List<USubject> cl, String stockStr) {
		if(StringUtils.isBlank(handleContent)) {
			return "";
		}
		handleContent = Jsoup.clean(handleContent,StockSpider.myWhitelist2());
		handleContent = Matcher.quoteReplacement(handleContent);

		String topicHighlightCss = ConfigCenterFactory.getString("stock_dbo.topicHighlightCss", "openWhichPage");
		String userHighlightCss = ConfigCenterFactory.getString("stock_dbo.userHighlightCss", "atPopWindow");
		String companyHighlightCss = ConfigCenterFactory.getString("stock_dbo.companyHighlightCss", "companyTip");
		String syncInvestHighlightCss = ConfigCenterFactory.getString("stock_dbo.syncInvestHighlightCss", "syncInvestHighlight");
		String industryClassifyHighlightCss = ConfigCenterFactory.getString("stock_dbo.industryClassifyHighlightCss", "industryClassifyHighlight");

		String stockRegex =("(#[^\\#]+#)|" + stockStr).replaceAll("\\*", "");

		String htmlContentRegex = ">([^<]+)<";
		Pattern htmlContentPattern = Pattern.compile(htmlContentRegex);
		Matcher htmlContentMatcher = htmlContentPattern.matcher(handleContent);
		StringBuffer htmlHighlightSb = new StringBuffer();
		boolean containsHtmlTag = false;
		while(htmlContentMatcher.find()) {
			String htmlContent = htmlContentMatcher.group(0);
			if(StringUtils.isBlank(htmlContent)) {
				continue;
			}
			String filterStockContent = getFilterStockContent(cl, stockStr,
					topicHighlightCss, companyHighlightCss,
					syncInvestHighlightCss, industryClassifyHighlightCss,
					stockRegex, htmlContent);
			htmlContentMatcher.appendReplacement(htmlHighlightSb, filterStockContent);
			containsHtmlTag = true;
		}

		if(! containsHtmlTag) {
			return getFilterStockContent(cl, stockStr,
					topicHighlightCss, companyHighlightCss,
					syncInvestHighlightCss, industryClassifyHighlightCss,
					stockRegex, handleContent);
		} else {
			htmlContentMatcher.appendTail(htmlHighlightSb);
			return htmlHighlightSb.toString();
		}

	}
	private String getFilterStockContent(List<USubject> cl,
			String stockStr, String topicHighlightCss,
			String companyHighlightCss, String syncInvestHighlightCss,
			String industryClassifyHighlightCss, String stockRegex,
			String htmlContent) {
		if(StringUtils.isBlank(htmlContent)) {
			return htmlContent;
		}

		Pattern stockPattern = Pattern.compile(stockRegex);
		Matcher stockMatcher = stockPattern.matcher(htmlContent);
		StringBuffer sb = new StringBuffer();
		while (stockMatcher.find()){
			String stockContent= stockMatcher.group();
			if(StringUtils.isBlank(stockContent)) {
				continue;
			}
			String filterStockContent = stockContent.replaceAll("Ａ", "a").replaceAll("ａ", "a").replaceAll("A", "a")
					.replaceAll("Ｂ", "a").replaceAll("ｂ", "b").replaceAll("B", "b").replaceAll("\\s+", "").toLowerCase();
			if(stockStr.contains(filterStockContent)) {
				String codeName = getStockCodeName(filterStockContent, cl);
				if(StringUtils.isNotBlank(codeName)) {
					String[] codeNameArr = codeName.split("\\|");
					String content = "<a data=\"" + codeNameArr[0]
							+ "\" class=\"" +companyHighlightCss + "\" href=\"/company_main.html?companycode=" + codeNameArr[0]
							+ "&companyname=" + JSEscape.escape(codeNameArr[1]) + "&ut=0" + "&end\">" + stockContent + "</a>";
					stockMatcher.appendReplacement(sb, content);
				}
			} else {
				String item = "";
				if(filterStockContent.contains("#")) {
					item = filterStockContent.split("#")[1];
					if(item.contains(":")) {
						item = item.split(":")[0];
						item = item.replaceAll("Ａ", "a").replaceAll("ａ", "a").replaceAll("A", "a")
								.replaceAll("Ｂ", "b").replaceAll("ｂ", "b").replaceAll("B", "b").replaceAll("\\s+", "").toLowerCase();
						item = getStockCode(item, cl);
					}
				} else {
					item = filterStockContent;
				}
				//类型(公司:0，行业:1，板块:2，指数:3，话题:4,服务号:5 同步投资:6 圈子：7)
				USubject us = RemindServiceClient.getInstance().getUSubjectByUIdentifyFromCache(item);
				if(us != null) {
					if(us.getType() == 0) {
						String content = "<a data=\"" + us.getUidentify() +
								"\" class=\"" +companyHighlightCss + "\" href=\"/company_main.html?companycode=" + us.getUidentify() + "&companyname="
								+ JSEscape.escape(us.getName()) + "&ut=" + us.getType() + "&end\">" + stockContent + "</a>";
						stockMatcher.appendReplacement(sb, content);
					} else if(us.getType() == 4 || us.getType() == 7) {
						String content = "";
						if(StringUtils.equals(item, "吐槽")) {
							content = "<a href=\"/roast.html?ut=-2\">" + stockContent + "</a>";
						} else {
							content = "<a href=\"/topic_detail.html?t=" + JSEscape.escape(item) + "&ut=" + us.getType()
									+ "&end\" class=\"" + topicHighlightCss + "\">" + stockContent + "</a>";
						}
						stockMatcher.appendReplacement(sb, content);
					} else if(us.getType() == 6) {
						String content = "<a href=\"/syncInvest.html?ouid=" + us.getUid() +
								"&t=" + JSEscape.escape(item) + "&ut=" + us.getType() + "&end\" class=\"" + syncInvestHighlightCss + "\">" + stockContent + "</a>";
						stockMatcher.appendReplacement(sb, content);
					} else if(us.getType() == 1) {
						String content = "<a href=\"/industry_classify.html?inTag=" + JSEscape.escape(item) + "&ut=" + us.getType()
								+ "&end\" class=\"" + industryClassifyHighlightCss + "\">" + stockContent + "</a>";
						stockMatcher.appendReplacement(sb, content);
					}
				}
			}
		}
		stockMatcher.appendTail(sb);
		return sb.toString();
	}

	private String getStockCodeName(String q, List<USubject> list) {
		if(list != null && list.size() > 0) {
			for(USubject company : list) {
				if(company.getName() != null && StringUtils.equals(q, company.getName().replaceAll("Ａ", "a").replaceAll("ａ", "a").replaceAll("A", "a")
						.replaceAll("Ｂ", "b").replaceAll("ｂ", "b").replaceAll("B", "b").replaceAll("\\s+", "").toLowerCase())
						|| company.getUidentify() != null && StringUtils.equals(q, company.getUidentify().toLowerCase())
						|| company.getUidentify() != null && StringUtils.equals(q, company.getUidentify().split("\\.")[0].toLowerCase())) {
					return company.getUidentify() + "|" + company.getName();
				}
			}
		}
		return "";
 	}

	private String getStockCode(String q, List<USubject> list) {
		if(list != null && list.size() > 0) {
			for(USubject company : list) {
				if(company.getName() != null && StringUtils.equals(q, company.getName().replaceAll("Ａ", "a").replaceAll("ａ", "a").replaceAll("A", "a")
						.replaceAll("Ｂ", "b").replaceAll("ｂ", "b").replaceAll("B", "b").replaceAll("\\s+", "").toLowerCase())
						|| company.getUidentify() != null && StringUtils.equals(q, company.getUidentify().toLowerCase())
						|| company.getUidentify() != null && StringUtils.equals(q, company.getUidentify().split("\\.")[0].toLowerCase())) {
					return company.getUidentify();
				}
			}
		}
		return "";
 	}

	//把博文发布到指定话题下
	public String publishArticle2Topic(final Article article,String identify) {
		try {
			if(StringUtils.isEmpty(identify)){
				return StockCodes.ERROR;
			}
			identify = identify.split(":")[0];
			String[] atArr = null;
			String ats = article.getAts();
			String sKey = String.valueOf(article.getUid());
			if (StringUtil.isEmpty(ats)) {
			}else {
				atArr = ats.split(",");
			}
			StringUtil.sbuArticle(article);
			String summary = replaceAtUserWithUid(article.getSummary());
			article.setSummary(summary);
			if(article.getArticleType()==0 || article.getArticleType()==3){
				String content = replaceAtUserWithUid(article.getContent());
				article.setContent(content);
			}
			article.setStatus(1);// 状态=1，正常
			long timemillis = article.getTime();
			if (timemillis < 1l) {
				timemillis = System.currentTimeMillis();
				article.setTime(timemillis);
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
							tls.saveTimeLine(atUid, article.getUuid(),TimeLineService.SAVE_TABLE.AT, timemillis);
						}
					}
				}
			}
			if(!StringUtil.isEmpty(ats)){
				MessageCenterSerivce.getInstance().atBlog(article);//@推送
			}
			Map<String, String> map = NosqlBeanUtil.bean2Map(article);
			ch.insert(SAVE_TABLE.ARTICLE.toString(), article.getUuid(), map);
			tls.saveTimeLine(identify, article.getUuid(),TimeLineService.SAVE_TABLE.TOPIC, timemillis);
			MessageCenterSerivce.getInstance().saveArtile2Cache(article,identify);
			String s = "article_counts";
			userExtAdd(s, sKey);
			saveEsArticleUUids(article.getUuid(), 1);
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	public String publishStockChanceCommentArticle(final Article article, boolean doRetweet) {
		try {
			StringUtil.sbuArticle(article);
			if(StringUtils.isEmpty(article.getTags())){
				String tags = CommonUtil.getTagsFromContent(article.getSummary());
				article.setTags(tags);
			}
			saveEsArticleUUids(article.getUuid(), 1);
			String summary = replaceAtUserWithUid(article.getSummary());
			article.setSummary(summary);
			String sKey = String.valueOf(article.getUid());
			String tags = article.getTags();
			String ats = article.getAts();
			String[] tagArr = null;
			if (StringUtil.isEmpty(tags)) {
				tagArr = new String[] {};
			} else {
				tagArr = tags.split(",");
			}
			String[] atArr = null;
			if (StringUtil.isEmpty(ats)) {
			}else {
				atArr = ats.split(",");
			}
			article.setStatus(1);// 状态=1，正常
			// 如果没有传时间进来，则创建时间
			long timemillis = article.getTime();
			if (timemillis < 1l) {
				timemillis = System.currentTimeMillis();
				article.setTime(timemillis);
			}

			if(doRetweet) {
				tls.saveTimeLine(sKey, article.getUuid(),
					TimeLineService.SAVE_TABLE.ARTICLE, timemillis);
				String suuid = article.getSuuid();
				if(StringUtils.isNotBlank(suuid)) {
					Map<String,Object> map = Maps.newHashMap();
					map.put("broadcast_counts", true);
					RemindServiceClient.getInstance().updateSimpleArticle(suuid, map);
					String[] columns = {"broadcast_counts"};
					updateArticleCount(suuid, columns, true);
				}
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
							tls.saveTimeLine(atUid, article.getUuid(),TimeLineService.SAVE_TABLE.AT, timemillis);
						}
					}
				}
			}
			if(!StringUtil.isEmpty(ats)){
				MessageCenterSerivce.getInstance().atBlog(article);//@推送
			}

			final Map<String, String> map = NosqlBeanUtil.bean2Map(article);
			ch.insert(SAVE_TABLE.ARTICLE.toString(), article.getUuid(), map);

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
				TopicService.getInstance().updateTopicIndex(suuid);
			}

			RemindServiceClient.getInstance().putSimpleArticle(article.getUuid(), simpleArticle);
			if(doRetweet) {
				String s = "article_counts";
				userExtAdd(s, sKey);
				//注册发微博消息
				UserMsg um = SMsgFactory
						.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_0);
				um.setS(String.valueOf(article.getUid()));
				um.setD(String.valueOf(article.getUid()));
				um.putAttr("title",article.getTitle());
				um.putAttr("source_url",article.getSource_url());
				um.putAttr("uuid", article.getUuid());
				um.setTime(article.getTime());
				// 如果是转发的博文　需要把转发信息也放在
				if(article.getType() == ShareConst.ZHUANG_ZAI && suuid!=null){
					um.putAttr("type", ShareConst.ZHUANG_ZAI);
				}else{
					um.putAttr("type", ShareConst.YUAN_CHUANG);
				}
				// 发表了新博文消息通知好友,我发送了新博文
				UserEventService.getInstance().notifyTheEvent(um);
			}
			int publishViewPointCommentToDiscuss = ConfigCenterFactory.getInt("stock_zjs.publishViewPointCommentToDiscuss", 1);//是否发布观点讨论到公司空间

			if(doRetweet || publishViewPointCommentToDiscuss == 1) {
				if(!StringUtils.isBlank(article.getTags())){
					// 通知相关专题 ，有新博文
	 				for(String identify : tagArr){
	 					if(StringUtils.isBlank(identify)) {
	 						continue;
	 					}
	 					if(publishViewPointCommentToDiscuss == 1) {
//	 						tls.saveTimeLine(identify, article.getUuid(), TimeLineService.SAVE_TABLE.ARTICLE, timemillis);
	 						tls.saveTimeLine(identify, article.getUuid(), TimeLineService.SAVE_TABLE.TOPIC, timemillis);
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
						umTag.putAttr("commitSelf", "true");
						UsubjectEventService.getInstance().notifyTheEvent(umTag);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	public String mockStockChanceCommentArticle(final Article article, boolean doRetweet) {
		try {
			StringUtil.sbuArticle(article);
			if(StringUtils.isEmpty(article.getTags())){
				String tags = CommonUtil.getTagsFromContent(article.getSummary());
				article.setTags(tags);
			}
			saveEsArticleUUids(article.getUuid(), 1);
			highlightKeywords(article);
			String summary = replaceAtUserWithUid(article.getSummary());
			article.setSummary(summary);
			String sKey = String.valueOf(article.getUid());
			String tags = article.getTags();
			String ats = article.getAts();
			String[] tagArr = null;
			if (StringUtil.isEmpty(tags)) {
				tagArr = new String[] {};
			} else {
				tagArr = tags.split(",");
			}
			String[] atArr = null;
			if (StringUtil.isEmpty(ats)) {
			}else {
				atArr = ats.split(",");
			}
			article.setStatus(1);// 状态=1，正常
			// 如果没有传时间进来，则创建时间
			long timemillis = article.getTime();
			if (timemillis < 1l) {
				timemillis = System.currentTimeMillis();
				article.setTime(timemillis);
			}

			if(doRetweet) {
				tls.saveTimeLine(sKey, article.getUuid(),
					TimeLineService.SAVE_TABLE.ARTICLE, timemillis);
				String suuid = article.getSuuid();
				if(StringUtils.isNotBlank(suuid)) {
					Map<String,Object> map = Maps.newHashMap();
					map.put("broadcast_counts", true);
					RemindServiceClient.getInstance().updateSimpleArticle(suuid, map);
					String[] columns = {"broadcast_counts"};
					updateArticleCount(suuid, columns, true);
				}
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
							tls.saveTimeLine(atUid, article.getUuid(),TimeLineService.SAVE_TABLE.AT, timemillis);
						}
					}
				}
			}
			if(!StringUtil.isEmpty(ats)){
				MessageCenterSerivce.getInstance().atBlog(article);//@推送
			}

			final Map<String, String> map = NosqlBeanUtil.bean2Map(article);
			ch.insert(SAVE_TABLE.ARTICLE.toString(), article.getUuid(), map);

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
				TopicService.getInstance().updateTopicIndex(suuid);
			}

			RemindServiceClient.getInstance().putSimpleArticle(article.getUuid(), simpleArticle);
			if(doRetweet) {
				String s = "article_counts";
				userExtAdd(s, sKey);
				//注册发微博消息
				UserMsg um = SMsgFactory
						.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_0);
				um.setS(String.valueOf(article.getUid()));
				um.setD(String.valueOf(article.getUid()));
				um.putAttr("title",article.getTitle());
				um.putAttr("source_url",article.getSource_url());
				um.putAttr("uuid", article.getUuid());
				um.setTime(article.getTime());
				// 如果是转发的博文　需要把转发信息也放在
				if(article.getType() == ShareConst.ZHUANG_ZAI && suuid!=null){
					um.putAttr("type", ShareConst.ZHUANG_ZAI);
				}else{
					um.putAttr("type", ShareConst.YUAN_CHUANG);
				}
				// 发表了新博文消息通知好友,我发送了新博文
				UserEventService.getInstance().notifyTheEvent(um);
			}
			//是否发布观点讨论到公司空间
			int publishViewPointCommentToDiscuss = ConfigCenterFactory.getInt("spider.publishViewPointCommentToDiscuss", 1);
			if(doRetweet || publishViewPointCommentToDiscuss == 1) {
				if(!StringUtils.isBlank(article.getTags())){
					// 通知相关专题 ，有新博文
	 				for(String identify : tagArr){
	 					if(StringUtils.isBlank(identify)) {
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
			}
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	//@somebody 高亮显示
	public String replaceAtUserWithUid(String content) {
		if(content==null)return "";
		String regex = "@[_a-zA-Z\\d\\u2E80-\\uFE4F]+";
		String aStr = "</a>";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(content);
		String rep = "";
		while (m.find()){
		  rep = m.group();
		  if(StringUtils.isNotBlank(rep) && rep.length() > 1) {
			  int index = content.indexOf(rep);
			  index = index+rep.length();
			  int end = index+aStr.length();
			  int totalSize = content.length();
			  if(end>totalSize){
				  end = totalSize;
			  }
			  String a = content.substring(index,end);
			  if(aStr.equals(a)){
				  continue;
			  }
			  String nick = rep.substring(1, rep.length()).trim();
			  Long atUid = UserServiceClient.getInstance().getUidByNickname(nick);
			  if(atUid == null || atUid == 0) {
				 Members member = MembersService.getInstance().getMembersByNickName(nick);
				 if(member != null) {
					 atUid = member.getUid();
				 }
			  }
			  if(atUid != null && atUid != 0) {
				  content = content.replaceAll(rep.substring(0, rep.length()), "<a class='atPopWindow' target='_blank' href='user_profile.html?uid=" + atUid + "&ut=-1&end' data='" + atUid + "'>" + rep.substring(0, rep.length()) + "</a>");
			  }
		  }
		}
		return content;
	}

	/**
	 *
	 * @param uuid
	 * @param type 1:保存 2：更新  3：删除
	 */
	private void saveEsArticleUUids(String uuid, int type) {
		try {
			UserServiceClient.getInstance().saveAsyncTasks(AsyncTaskConstants.ARTICLES_ASYNC_TASK_KEY, StockUtil.joinString("^", uuid, type));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Article getArticleBySimple(String uuid) {
		Article article = new Article();
		try {
			if(StringUtils.isNotBlank(uuid)){
				article.setKey(uuid);
				String[] columns = ch.getColumns(Article.class);
				Map<String, String> aMap = ch.get(SAVE_TABLE.ARTICLE.toString(),
						uuid, columns);
				NosqlBeanUtil.map2Bean(article, aMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return article;
	}

	// 获取文章及它的评论
	public Article getArticle(Long uid, String uuid) {
		Article article = new Article();
		try {
			article.setKey(uuid);
			String[] columns = ch.getColumns(Article.class);
			Map<String, String> aMap = ch.get(SAVE_TABLE.ARTICLE.toString(),
					uuid, columns);
			if (aMap.isEmpty() || aMap.size() == 0) {
				return null;
			}
			NosqlBeanUtil.map2Bean(article, aMap);

			if (article.getType() == ShareConst.ZHUANG_ZAI) {
				String suuid = article.getSuuid();// 转发的原始文章UUID
				Article retweet_article = new Article();
				Map<String, String> sMap = ch.get(
						SAVE_TABLE.ARTICLE.toString(), suuid, columns);
				NosqlBeanUtil.map2Bean(retweet_article, sMap);
				article.setRetweet_article(retweet_article);
			}

			// 最近N条 第一页评论
			List<String> commentUuidList = tls.getTimeLineByLatest(uuid,
					SAVE_TABLE.COMMENT);
			String[] commentUuidStrArr = new String[commentUuidList.size()];
			commentUuidList.toArray(commentUuidStrArr);
			String[] commitColumns = ch.getColumns(Comment.class);
			Map<String, Map<String, String>> batchMap = ch.get(
					SAVE_TABLE.COMMENT.toString(), commentUuidStrArr,
					commitColumns);
			List<Comment> commentList = new ArrayList<Comment>();
			for (String commentUuid : commentUuidList) {
				Comment c = new Comment();
				Map commentObjMap = batchMap.get(commentUuid);
				NosqlBeanUtil.map2Bean(c, commentObjMap);
				commentList.add(c);
			}
			article.setComments(commentList);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return article;
	}

	public String shortReview(Long uid, String uuid, Integer type) {
		// TODO Auto-generated method stub
		return null;
	}

	public String updateBrowesCount(Long uid, String uuid) {
		try {
			// Article article = new Article();
			// article.setKey(uuid);
			SimpleArticle simpleArticle = getSimpleArticle(uuid);
			if(simpleArticle==null || simpleArticle.getUid()<=0){
				 return StockCodes.SUCCESS;
			 }
			String[] columns = { "browse_counts" };// ch.getColumns(Article.class);
			Map<String, String> aMap = ch.get(SAVE_TABLE.ARTICLE.toString(),
					uuid, columns);
			// NosqlBeanUtil.map2Bean(article, aMap);
			String num = aMap.get("browse_counts")==null?"0":aMap.get("browse_counts");
			int newCounts = Integer.parseInt(num) + 1;
			// 只写1个字段
			aMap.put("browse_counts", String.valueOf(newCounts));
			ch.insert(SAVE_TABLE.ARTICLE.toString(), uuid, aMap);
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		//dcss 浏览次数+1
		Map<String,Object> map = Maps.newHashMap();
		map.put("browse_counts", 1);
		RemindServiceClient.getInstance().updateSimpleArticle(uuid, map);
		return StockCodes.SUCCESS;
	}

	/**
	 * @return 返回 浏览次数，转播次数 评论次数 和发布时间
	 */
	public List<Article> getArticleListOnlyCounts(List<TimeLine> tlList,
			int minCounts) {
		List<Article> articleList = new ArrayList<Article>();
		String[] columns = { "browse_counts", "broadcast_counts",
				"comment_counts", "time" };
		String[] keys = new String[tlList.size()];
		for (int i = 0; i < tlList.size(); i++) {
			keys[i] = tlList.get(i).getUuid();
		}
		Map<String, Map<String, String>> batchMap = ch.get(
				SAVE_TABLE.ARTICLE.toString(), keys, columns);
		for (String uuid : keys) {
			Map<String, String> aMap = batchMap.get(uuid);
			Article article = new Article();
			article.setUuid(uuid);
			NosqlBeanUtil.map2Bean(article, aMap);
			if (article.getBrowse_counts() > minCounts
					|| article.getBroadcast_counts() > minCounts) {
				articleList.add(article);
			}

		}
		return articleList;
	}

	/**
	 * 某个用户的文章列表<br>
	 * 注：文章中只包含UUID
	 *
	 * @param uid
	 * @return
	 */
	public List<Article> getList(Long uid) {
		String uidStr = String.valueOf(uid);
		List<String> uuidList = tls.getTimeLine(uidStr, SAVE_TABLE.ARTICLE);
		List<Article> articleList = new ArrayList<Article>();
		for (String uuid : uuidList) {
			Article article = new Article();
			article.setUuid(uuid);
			articleList.add(article);
		}
		return articleList;
	}

	public List<Article> getArticleList(List<String> uuidList) {
		List<Article> articleList = new ArrayList<Article>();
		Set<String> columnsSet = ch.getColumnsSet(Article.class);
		columnsSet.remove("serialVersionUID");

		// columnsSet.remove("content");
		String[] columns = {};
		columns = columnsSet.toArray(columns);
		String[] keys = new String[uuidList.size()];
		uuidList.toArray(keys);
		Map<String, Map<String, String>> batchMap = ch.get(
				SAVE_TABLE.ARTICLE.toString(), keys, columns);
		for (String uuid : uuidList) {
			Map<String, String> aMap = batchMap.get(uuid);
			Article article = new Article();
			NosqlBeanUtil.map2Bean(article, aMap);

			if (!aMap.isEmpty()) {
				articleList.add(article);
			}
		}

		// 取出所有的转发 的原始文章UUID
		Set<String> uuidSet_zf = new HashSet<String>();
		for (Article article : articleList) {
			if (ShareConst.ZHUANG_ZAI == article.getType()) {
				uuidSet_zf.add(article.getSuuid());
			}
		}
		if (uuidSet_zf.size() > 0) {
			String[] keys_zf = new String[uuidSet_zf.size()];
			keys_zf = uuidSet_zf.toArray(keys_zf);
			Map<String, Map<String, String>> batchMap_zf = ch.get(
					SAVE_TABLE.ARTICLE.toString(), keys_zf, columns);
			for (Article article : articleList) {
				if (ShareConst.ZHUANG_ZAI == article.getType()) {
					Map<String, String> aMap = batchMap_zf.get(article
							.getSuuid());
					Article article_zf = new Article();
					NosqlBeanUtil.map2Bean(article_zf, aMap);
					article.setRetweet_article(article_zf);
				}
			}
		}

		return articleList;
	}

	//消息中心 @我的博文
	public List<Article> getArticleList(List<String> uuidList, long uid) {
		List<Article> articleList = new ArrayList<Article>();
		Set<String> columnsSet = ch.getColumnsSet(Article.class);
		columnsSet.remove("serialVersionUID");

		// columnsSet.remove("content");
		String[] columns = {};
		columns = columnsSet.toArray(columns);
		String[] keys = new String[uuidList.size()];
		uuidList.toArray(keys);
		Map<String, Map<String, String>> batchMap = ch.get(
				SAVE_TABLE.ARTICLE.toString(), keys, columns);
		for (String uuid : uuidList) {
			Map<String, String> aMap = batchMap.get(uuid);
			Article article = new Article();
			NosqlBeanUtil.map2Bean(article, aMap);

			if (aMap.isEmpty() == false) {
				articleList.add(article);
			}else{//如果博文实体不存在 删除缓存
				RemindServiceClient.getInstance().clearMessage(uid, MsgConst.MSG_USER_TYPE_3, uuid);
			}
		}

		// 取出所有的转发 的原始文章UUID
		Set<String> uuidSet_zf = new HashSet<String>();
		for (Article article : articleList) {
			if (ShareConst.ZHUANG_ZAI == article.getType()) {
				uuidSet_zf.add(article.getSuuid());
			}
		}
		if (uuidSet_zf.size() > 0) {
			String[] keys_zf = new String[uuidSet_zf.size()];
			keys_zf = uuidSet_zf.toArray(keys_zf);
			Map<String, Map<String, String>> batchMap_zf = ch.get(
					SAVE_TABLE.ARTICLE.toString(), keys_zf, columns);
			for (Article article : articleList) {
				if (ShareConst.ZHUANG_ZAI == article.getType()) {
					Map<String, String> aMap = batchMap_zf.get(article
							.getSuuid());
					Article article_zf = new Article();
					NosqlBeanUtil.map2Bean(article_zf, aMap);
					article.setRetweet_article(article_zf);
				}
			}
		}

		return articleList;
	}

	public SimpleArticle getSimpleArticle(String uuid){
		SimpleArticle simplearticle = new SimpleArticle();
		try {
			if(StringUtils.isNotBlank(uuid)){
				simplearticle.setKey(uuid);
				String[] columns = ch.getColumns(SimpleArticle.class);
				Map<String, String> aMap = ch.get(SAVE_TABLE.ARTICLE.toString(),
						uuid, columns);
				NosqlBeanUtil.map2Bean(simplearticle, aMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return simplearticle;
	}

	public Viewpoint getViewpoint(String uuid){
		Viewpoint vp = new Viewpoint();
		try {
			if(StringUtils.isNotBlank(uuid)){
				vp.setKey(uuid);
				String[] columns = ch.getColumns(Viewpoint.class);
				Map<String, String> aMap = ch.get(SAVE_TABLE.ARTICLE.toString(),
						uuid, columns);
				NosqlBeanUtil.map2Bean(vp, aMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return vp;
	}

	public List<SimpleArticle> getSimpleArticleListByUUIDLIST(List<String> uuidList) {
		if(uuidList==null || uuidList.size()==0){
			return null;
		}
		List<SimpleArticle> articleList = new ArrayList<SimpleArticle>();
		Set<String> columnsSet = ch.getColumnsSet(SimpleArticle.class);
		columnsSet.remove("serialVersionUID");

		// columnsSet.remove("content");
		String[] columns = {};
		columns = columnsSet.toArray(columns);
		String[] keys = new String[uuidList.size()];
		uuidList.toArray(keys);
		Map<String, Map<String, String>> batchMap = ch.get(
				SAVE_TABLE.ARTICLE.toString(), keys, columns);
		for (String uuid : uuidList) {
			Map<String, String> aMap = batchMap.get(uuid);
			SimpleArticle article = new SimpleArticle();
			NosqlBeanUtil.map2Bean(article, aMap);

			if (aMap.isEmpty() == false) {
				articleList.add(article);
			}
		}

		// 取出所有的转发 的原始文章UUID
		Set<String> uuidSet_zf = new HashSet<String>();
		for (SimpleArticle article : articleList) {
			if (ShareConst.ZHUANG_ZAI == article.getType()) {
				uuidSet_zf.add(article.getSuuid());
			}
		}
		if (uuidSet_zf.size() > 0) {
			String[] keys_zf = new String[uuidSet_zf.size()];
			keys_zf = uuidSet_zf.toArray(keys_zf);
			Map<String, Map<String, String>> batchMap_zf = ch.get(
					SAVE_TABLE.ARTICLE.toString(), keys_zf, columns);
			for (SimpleArticle article : articleList) {
				if (ShareConst.ZHUANG_ZAI == article.getType()) {
					Map<String, String> aMap = batchMap_zf.get(article
							.getSuuid());
					SimpleArticle article_zf = new SimpleArticle();
					NosqlBeanUtil.map2Bean(article_zf, aMap);
					article.setRetweet_article(article_zf);
				}
			}
		}

		return articleList;
	}

	/**
	 * 先查询下此用户的博客有无此分类
	 *
	 * @param uid
	 * @param uuid
	 * @param blogCategory
	 * @return
	 */
	public String updateBlogCategory(Long uid, String uuid, String blogCategory) {
		// TODO Auto-generated method stub
		return null;
	}

	public String createBlogCategory(Long uid, String uuid, String blogCategory) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Article> getArticleSummeryList(Long uid) {
		// TODO Auto-generated method stub
		List<Article> al = getList(uid);
		if (al != null && al.size() != 0) {
			// 暂时这样处理，取出全部，再置空相关字段
			for (Article a : al) {
				a.setComments(null);
				a.setContent("");
			}
		}
		return al;
	}

	public List<Article> getArticleSummeryListByBlogCategory(Long uid,
			String category, int type) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 修改文章的次数 如浏览次数等
	 */
	public void updateArticleCount(String uuid, String column, boolean add) {
		SimpleArticle simpleArticle = getSimpleArticle(uuid);
		if(simpleArticle ==null || simpleArticle.getUid()<=0){
			return;
		}
		String[] columns = { column };
		int count = 0;
		try {
			String columnValue = ch.get(SAVE_TABLE.ARTICLE.toString(), uuid,
					columns).get(column);
			count = Integer.parseInt(columnValue);
		} catch (Exception e) {
			logger.error("类型异常:" + e);
		}
		if (add) {
			count = count + 1;
			TopicService.getInstance().updateTopicIndex(uuid);//update topic_index
		} else {
			count = count - 1>0?count - 1:0;
		}
		Map<String, String> objMap = new HashMap<String, String>();
		objMap.put(column, String.valueOf(count));
		ch.insert(SAVE_TABLE.ARTICLE.toString(), uuid, objMap);
	}

	/**
	 * 修改文章和用户的 次数信息 如浏览次数等
	 */
	public void updateArticleCount(String uuid, String[] columns, boolean add) {
		SimpleArticle simpleArticle = getSimpleArticle(uuid);
		if(simpleArticle==null || simpleArticle.getUid()<=0){
			logger.error("操作的博文不存在");
			return;
		}
		Map<String, String> getMap = ch.get(SAVE_TABLE.ARTICLE.toString(),
				uuid, columns);
		Map<String, String> objMap = new HashMap<String, String>();
		for (String column : columns) {
			String columnValue = getMap.get(column);
			try {
				int count = 0;
				count = Integer.parseInt(columnValue);
				if (add) {
					count = count + 1;
				} else {
					count = count - 1<0?0:count -1;
				}
				objMap.put(column, String.valueOf(count));
			} catch (Exception e) {
				logger.error("类型异常:" + e);
			}
		}
		ch.insert(SAVE_TABLE.ARTICLE.toString(), uuid, objMap);
	}

	// 转发并评论 使用同一个UUID
	@SuppressWarnings("unchecked")
	public String retweetAndComment(Comment comment, Article article) {
		String auuid = comment.getPuuid(); //被转发的博文(原博文)
		String cuuid = comment.getUuid(); //当前评论
		String suuid = comment.getSuuid();//当前博文
		long timemillis = comment.getTime();
		String content = replaceAtUserWithUid(comment.getContent());
		content = replaceKeywords(content);
		comment.setContent(content);
		List<String> columnList = new ArrayList<String>();
		if (comment != null) {
			columnList.add("comment_counts");
			try {
				// 转发
				Map<String, String> commentMap = NosqlBeanUtil.bean2Map(comment);
				tls.saveTimeLine(auuid, cuuid,
						TimeLineService.SAVE_TABLE.COMMENT, timemillis);
				ch.insert(SAVE_TABLE.COMMENT.toString(), comment.getUuid(),
						commentMap);
				String s = "comment_counts";
				userExtAdd(s, comment.getUid() + "");
			} catch (Exception e) {
				logger.error("转发并评论 处理异常:" + e);
			}
		}
		if (article != null) {
			columnList.add("broadcast_counts");
			broadcast(article);
		}
		Article atl = getArticleBySimple(auuid);
		if(atl!=null && StringUtils.isNotBlank(atl.getTags())){
			String[] tagArr = atl.getTags().split(",");
			for (String t : tagArr) {
				if(StringUtils.isBlank(t)) {
					continue;
				}
				String identify = t.split(":")[0];
				USubject uSubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(identify);
				if(uSubject!=null && (uSubject.getType()==StockConstants.SUBJECT_TYPE_4
						|| uSubject.getType()==StockConstants.SUBJECT_TYPE_7)){//自定义话题
					Map<String,Object> map = Maps.newHashMap();
					map.put("identify", identify);
					map.put("time", comment.getTime());
					//map.put("summary", comment.getContent());
					TopicService.getInstance().updateTopicCache(map);

				}
			}
		}

		//增加一条原文评论
		RemindServiceClient.getInstance().addArticleComment(auuid, comment);
		Map<String,Object> map = new HashMap<String, Object>();
		if(comment!=null){
			map.put("comment_counts",true);//dcss 评论数+1
		}
		if (article != null && !StringUtils.isEmpty(suuid) &&  suuid.equals(auuid)) { //一级转发
			map.put("broadcast_counts",true);//dcss 转发数+1
		}
		/*if(article!=null){
			map.put("broadcast_counts", true);//dcss 转发数+1
		}*/
		RemindServiceClient.getInstance().updateSimpleArticle(auuid, map);
		// 被引用文章的评论数和转发数+1
		String[] columns = new String[columnList.size()];
		columns = columnList.toArray(columns);
		updateArticleCount(auuid, columns, true);
		TopicService.getInstance().updateTopicIndex(auuid);//topic_index
		if(!StringUtils.isEmpty(suuid) && !suuid.equals(auuid)){ //当前博文下评论
			UUID uuid = UUID.randomUUID();
			Comment scom = (Comment) comment.clone();
			scom.setUuid(uuid.toString());
			scom.setPuuid(suuid);
			scom.setSuuid(null);
			scom.setRcontent(null);
			String scomment = comment.getContent().replaceAll("<a[^>]+>","");//过滤a标签
			scomment = scomment.split("//@")[0];
			if(StringUtil.isEmpty(scomment)){
				scomment = "转发博文";
			}
			scom.setContent(scomment);
			scom.setTime(System.currentTimeMillis());
			Map<String, String> commentMap = NosqlBeanUtil.bean2Map(scom);
			tls.saveTimeLine(suuid, scom.getUuid(),
					TimeLineService.SAVE_TABLE.COMMENT, scom.getTime());
			ch.insert(SAVE_TABLE.COMMENT.toString(), scom.getUuid(),
					commentMap);
			RemindServiceClient.getInstance().addArticleComment(suuid, scom);
			Map<String,Object> map2 = new HashMap<String, Object>();
			if(scom!=null){
				map2.put("comment_counts",true);//dcss 评论数+1
				map2.put("broadcast_counts",true);//dcss 转发+1
			}
			RemindServiceClient.getInstance().updateSimpleArticle(suuid, map2);
			List<String> scolumnList = new ArrayList<String>();
			scolumnList.add("comment_counts");
			scolumnList.add("broadcast_counts");
			String[] scolumns = new String[scolumnList.size()];
			scolumns = scolumnList.toArray(scolumns);
			updateArticleCount(suuid, scolumns, true);
		}
		return StockCodes.SUCCESS;
	}

	public String addStockChanceComment(Comment comment) {
		String auuid = comment.getPuuid();
		String cuuid = comment.getUuid();
		long timemillis = comment.getTime();
		String content = replaceAtUserWithUid(comment.getContent());
		content = replaceKeywords(content);
		comment.setContent(content);
		List<String> columnList = new ArrayList<String>();
		if (comment != null) {
			columnList.add("comment_counts");
			try {
				Map<String, String> commentMap = NosqlBeanUtil.bean2Map(comment);
				tls.saveTimeLine(auuid, cuuid,
						TimeLineService.SAVE_TABLE.COMMENT, timemillis);
				ch.insert(SAVE_TABLE.COMMENT.toString(), comment.getUuid(),
						commentMap);

				String s = "comment_counts";
				userExtAdd(s, String.valueOf(comment.getUid()));
			} catch (Exception e) {
				logger.error("转发并评论 处理异常:" + e);
			}
		}
		Article atl = getArticleBySimple(auuid);
		if(atl!=null && StringUtils.isNotBlank(atl.getTags())){
			String[] tagArr = atl.getTags().split(",");
			for (String t : tagArr) {
				if(StringUtils.isBlank(t)) {
					continue;
				}
				String identify = t.split(":")[0];
				USubject uSubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(identify);
				if(uSubject!=null && (uSubject.getType()==StockConstants.SUBJECT_TYPE_4
						|| uSubject.getType()==StockConstants.SUBJECT_TYPE_7)){//自定义话题
					Map<String,Object> map = Maps.newHashMap();
					map.put("identify", identify);
					map.put("time", comment.getTime());
					//map.put("summary", comment.getContent());
					TopicService.getInstance().updateTopicCache(map);

				}
			}
		}

		//增加一条原文评论
		RemindServiceClient.getInstance().addArticleComment(auuid, comment);
		Map<String,Object> map = new HashMap<String, Object>();
		if(comment!=null){
			map.put("comment_counts",true);//dcss 评论数+1
		}

		RemindServiceClient.getInstance().updateSimpleArticle(auuid, map);
		// 被引用文章的评论数和转发数+1
		String[] columns = new String[columnList.size()];
		columns = columnList.toArray(columns);
		updateArticleCount(auuid, columns, true);
		TopicService.getInstance().updateTopicIndex(auuid);//topic_index
		return StockCodes.SUCCESS;
	}

	public String addComment(Comment comment) {
		try {
			String sKey = String.valueOf(comment.getPuuid());
			// Map<String,Map<String,String>> superMap = new
			// HashMap<String,Map<String,String>>();
			long timemillis = comment.getTime();
			if (timemillis < 1l) {
				timemillis = System.currentTimeMillis();
				comment.setTime(timemillis);
			}
			Map<String, String> commentMap = NosqlBeanUtil.bean2Map(comment);
			// superMap.put(comment.getUuid(), commentMap);
			// puuid文章的uuid评论
			tls.saveTimeLine(sKey, comment.getUuid(),
					TimeLineService.SAVE_TABLE.COMMENT, timemillis);
			// ch.insertSuper(SAVE_TABLE.COMMENT.toString(), sKey, superMap);
			ch.insert(SAVE_TABLE.COMMENT.toString(), comment.getUuid(),
					commentMap);
			String s = "comment_counts";
			// 增加文章的评论次数
			// Map<String,String> getMap= ch.get(SAVE_TABLE.ARTICLE.toString(),
			// sKey, new String[]{s});
			// int nowCommentCounts =
			// Integer.parseInt(getMap.get("comment_counts"))+1;
			// getMap.put("comment_counts", String.valueOf(nowCommentCounts));
			// ch.insert(SAVE_TABLE.ARTICLE.toString(), sKey,getMap);

			userExtAdd(s, comment.getUid() + "");
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	/**
	 * @param auuid
	 *            评论数据结构变化，此字段已无用处
	 * @param cuuid
	 * @return
	 */
	public Comment getComment(String auuid, String cuuid) {
		String[] columns = ch.getColumns(Comment.class);
		Map<String, String> getMap = ch.get(SAVE_TABLE.COMMENT.toString(),
				cuuid, columns);// ch.getSuper(SAVE_TABLE.COMMENT.toString(),
								// auuid, cuuid);
		Comment comment = new Comment();
		NosqlBeanUtil.map2Bean(comment, getMap);

		return comment;
	}

	public List<Comment> getCommentAndParentComment(String auuid, String cuuid) {
		List<Comment> cl = new ArrayList<Comment>();
		String[] columns = ch.getColumns(Comment.class);
		Map<String, String> getMap = ch.get(SAVE_TABLE.COMMENT.toString(),
				cuuid, columns);// ch.getSuper(SAVE_TABLE.COMMENT.toString(),
								// auuid, cuuid);
		Comment comment = new Comment();
		NosqlBeanUtil.map2Bean(comment, getMap);

		// path 来源外部点击时
		String parents = comment.getPath();
		if (StringUtil.isEmpty(parents) == false) {
			String[] parentArr = parents.split(",");
			Map<String, Map<String, String>> getPMap = ch.get(
					SAVE_TABLE.COMMENT.toString(), parentArr, columns);
			for (String cid : parentArr) {
				Map<String, String> getOneMap = getPMap.get(cid);

				Comment onePComment = new Comment();
				NosqlBeanUtil.map2Bean(onePComment, getOneMap);
				cl.add(onePComment);

			}
		}
		cl.add(comment);
		return cl;
	}

	public String delComment(Long uid, String auuid, String cuuid) {
		if (StringUtil.isEmpty(auuid) || StringUtil.isEmpty(cuuid)) {
			logger.error("删除评论 传递的参数有异常");
			throw new RuntimeException("删除评论 传递的参数有异常");
		}
		RemindServiceClient.getInstance().deleteArticleComment(auuid, cuuid);
		Comment comment = getComment(auuid, cuuid);
		if (comment == null)
			return StockCodes.ERROR;
		String sKey = comment.getPuuid();
		if (StringUtil.isEmpty(comment.getUuid())) {
			logger.error("评论不存在，之前已删除");
			return StockCodes.SUCCESS;
		}
		if (StringUtil.isEmpty(sKey)) {
			logger.error("评论缺少被评论对象的UUID");
			throw new RuntimeException("评论缺少被评论对象的UUID");
		}
		if (comment.getTime() <= 0) {
			logger.warn("缺少时间,无法删除时间轴上内容");// 缺少时间，无法删除
			throw new RuntimeException("缺少时间,无法删除时间轴上内容");
		}
		// 删除此评论在文章上的关系
		tls.deltetTimeLine(auuid, SAVE_TABLE.COMMENT, comment.getTime());
		// 删除评论实体
		ch.delete(SAVE_TABLE.COMMENT.toString(), cuuid);
		String s = "comment_counts";
		// 减少 文章的评论次数
		Map<String, String> getMap = ch.get(SAVE_TABLE.ARTICLE.toString(),
				sKey, new String[] { s });
		int nowCommentCounts = Integer.parseInt(getMap.get("comment_counts")) - 1;
		getMap.put("comment_counts", String.valueOf(nowCommentCounts));
		ch.insert(SAVE_TABLE.ARTICLE.toString(), sKey, getMap);

		userExtSubtract(s, String.valueOf(uid));
		//dcss 评论数减一
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("comment_counts",false);
		RemindServiceClient.getInstance().updateSimpleArticle(auuid, map);

		//删除消息中心缓存
		//删除@我的评论
		if(comment!=null){
			//删除 我发出的评论
			RemindServiceClient.getInstance().clearMessage(comment.getUid(),MsgConst.MSG_USER_TYPE_14, cuuid);
			//删除回复我的评论
			if(comment.getAt()!=null && comment.getPath()!=null){
				long cuid = 0;
				String cuidStr =comment.getAt().split(":")[0];//原评论
				if(StringUtils.isNumeric(cuidStr)){
					try {
						cuid = Long.parseLong(cuidStr);
						RemindServiceClient.getInstance().clearMessage(cuid, MsgConst.MSG_USER_TYPE_4,cuuid);
					} catch (NumberFormatException e) {
					}
				}
			}
			String content = comment.getContent();
			String[] nickNameArr = CommonUtil.matcherNames(content);
			for(String nickname : nickNameArr){
				String atUid = String.valueOf(UserServiceClient.getInstance().getUidByNickname(nickname));;
				if(StringUtils.isNumeric(atUid)){
					RemindServiceClient.getInstance().clearMessage(Long.parseLong(atUid), MsgConst.MSG_USER_TYPE_13, comment.getUuid());
				}
			}
		}
		//删除我收到的评论
		Article article = MicorBlogService.getInstance().getArticleBySimple(auuid);
		long auid = article.getUid();
		RemindServiceClient.getInstance().clearMessage(auid, MsgConst.MSG_USER_TYPE_4,cuuid);
		return StockCodes.SUCCESS;
	}

	public String delFavorite(String uuid) {
		try {
			String sKey = uuid;
			List<TimeLine> tl = tls.getTimeLineList("9", SAVE_TABLE.FAVORITE,
					0, 100);

		} catch (Exception e) {

		}
		return StockCodes.SUCCESS;
	}

	/**
	 * 添加收藏 在TimeLine表中存储顺序的关系，如：uid-favorite-{time1:uuid1,time2:uuid2...}
	 * 在favorite表中存储数据，如 uid-uuid1-{title:标题1,summary:内容1,source_url:url1...}
	 */
	public String addFavorite(Favorite favorite) {
		try {
			if(favorite==null || favorite.getUid()<=0){
				logger.info("收藏的博文不存在");
				return StockCodes.ERROR;
			}
			String sKey = String.valueOf(favorite.getUid());
			Map<String, Map<String, String>> superMap = new HashMap<String, Map<String, String>>();
			long timemillis = favorite.getTime();
			if (timemillis < 1l) {
				timemillis = System.currentTimeMillis();
				favorite.setTime(timemillis);
			}

			Map<String, String> favoriteMap = NosqlBeanUtil.bean2Map(favorite);
			// Map2Bean NosqlBeanUtil.map2Bean(bean, properties)
			superMap.put(favorite.getUuid(), favoriteMap);
			if (this.getFavorite(favorite.getUid(), favorite.getUuid()) == null) {
				tls.saveTimeLine(sKey, favorite.getUuid(), SAVE_TABLE.FAVORITE,
						timemillis);
				String uuid = favorite.getUuid();
				ch.insertSuper(SAVE_TABLE.FAVORITE.toString(), sKey, superMap);
				userExtAdd("favorite_counts", String.valueOf(favorite.getUid()));
				updateArticleCount(uuid, "favorite_counts", true);
				// dcss 博文收藏数加1
				Map<String, Object> map = Maps.newHashMap();
				map.put("favorite_counts", true);
				RemindServiceClient.getInstance().updateSimpleArticle(uuid, map);
			}else{
				//有重复的不处理
				logger.warn("有重复的收藏,uid="+favorite.getUid()+" uuid="+favorite.getUuid());
//				return StockCodes.ERROR;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	public Favorite getFavorite(Long uid, String uuid) {
		Favorite favorite = new Favorite();
		try {
			Map<String, String> favoriteMap = ch.getSuper(
					SAVE_TABLE.FAVORITE.toString(), String.valueOf(uid), uuid);
			if (favoriteMap.isEmpty())
				return null;
			NosqlBeanUtil.map2Bean(favorite, favoriteMap);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return favorite;
	}

	public List<Favorite> getFavoriteList(Long uid, int start, int limit) {
		String sKey = String.valueOf(uid);
		List<String> uuidList = tls.getTimeLine(sKey, SAVE_TABLE.FAVORITE,
				start, limit);
		List<Map<String, String>> getMapList = CassandraHectorGateWay
				.getInstance().getSuper(SAVE_TABLE.FAVORITE.toString(), sKey,
						uuidList);
		List<Favorite> favoriteList = new ArrayList<Favorite>(getMapList.size());
		for (Map<String, String> getMap : getMapList) {
			Favorite favorite = new Favorite();
			NosqlBeanUtil.map2Bean(favorite, getMap);

			favoriteList.add(favorite);
		}
		Set<String> uuidSet = new HashSet<String>();
		for (Favorite f : favoriteList) {
			if (StringUtil.isEmpty(f.getUuid()) == false) {
				uuidSet.add(f.getUuid());
			}
		}
		List<String> list3 = new ArrayList<String>(uuidSet);
		List<Article> articleList = getArticleList(list3);
		for (Favorite f : favoriteList) {
			for (Article article : articleList) {
				if (f.getUuid().equals(article.getUuid())) {
					f.setArticle(article);
					break;
				}
			}
		}
		Collections.sort(favoriteList,new Comparator<Favorite>(){
			public int compare(Favorite o1,Favorite o2){
				return ((Long)o2.getTime()).compareTo(((Long)o1.getTime()));
			}
		});
		return favoriteList;
	}

	public String unfavorite(long uid, String uuid) {
		Favorite favorite = getFavorite(uid, uuid);
		if (favorite == null)
			return StockCodes.ERROR;
		if (favorite.getTime() <= 0) {
			logger.warn("缺少时间,无法删除时间轴上内容");// 缺少时间，无法删除
		}
		tls.deltetTimeLine(String.valueOf(uid), SAVE_TABLE.FAVORITE,
				favorite.getTime());
		ch.deleteSuperColumn(SAVE_TABLE.FAVORITE.toString(),
				String.valueOf(uid), uuid);
		userExtSubtract("favorite_counts", String.valueOf(favorite.getUid()));
		return StockCodes.SUCCESS;
	}

	private Article map2Article(Map<String, String> map) {
		if (map.size() == 0)
			return null;
		Article article = new Article();
		NosqlBeanUtil.map2Bean(article, map);
		return article;
	}

	private Map<String, String> article2Map(Article article) {
		if (article == null)
			return null;
		Map<String, String> map = null;
		try {
			map = NosqlBeanUtil.bean2Map(article);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public List<Article> getLatestPublishInfo(Long uid) {
		String sKey = String.valueOf(uid);
		List<String> uuidList = tls.getTimeLineByLatest(sKey,
				SAVE_TABLE.ARTICLE);
		return getArticleList(uuidList);
	}

	public Boolean hadfollowed(Long uid, Long fuid) {
		// TODO Auto-generated method stub
		return checkFollow(uid, fuid);
	}

	// 转换
	private List<UserExt> conver(List<ShortUser> shortuserList) {
		if (shortuserList == null || shortuserList.size() == 0) {
			return null;
		}
		String[] keys = new String[shortuserList.size()];
		for (int i = 0; i < shortuserList.size(); i++) {
			ShortUser shortuser = shortuserList.get(i);
			keys[i] = shortuser.getKey();
		}
		String[] columns = ch.getColumns(UserExt.class);
		Map<String, Map<String, String>> getMMap = ch.get(
				SAVE_TABLE.USER_EXT.toString(), keys, columns);
		List<UserExt> ueList = new ArrayList<UserExt>(shortuserList.size());
		for (int i = 0; i < shortuserList.size(); i++) {
			ShortUser shortuser = shortuserList.get(i);
			UserExt ue = new UserExt();
			ue.setStatus(shortuser.getStatus());
			NosqlBeanUtil.map2Bean(ue, getMMap.get(shortuser.getKey()));
			ueList.add(ue);
		}
		return ueList;

	}

	/**
	 * 用户动态
	 *
	 * @param uid
	 * @return SUser 所有跟此用户相关的信息 如收听，好友的最新动态，收藏等等 重点完成此功能
	 */
	@SuppressWarnings("unchecked")
	public SUser getUserBlogInfo(Long uid) {
		SUser su = new SUser();
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
		String[] columns = ch.getColumns(UserExt.class);
		Map<String, String> getMap = ch.get(SAVE_TABLE.USER_EXT.toString(),
				String.valueOf(uid), columns);
		if (getMap.isEmpty() || StringUtil.isEmpty(getMap.get("nickname"))) {
			Members mbs = MembersService.getInstance().getMembers(uid);
			try {
				if(mbs != null){
					getMap = NosqlBeanUtil.bean2Map(mbs);
					ch.insert(SAVE_TABLE.USER_EXT.toString(), String.valueOf(uid),
							getMap);
				}
			} catch (Exception e) {
			}
		}
		UserExt ue = new UserExt();
		NosqlBeanUtil.map2Bean(ue, getMap);
		su.setUser(ue);
		// su.setFollowArticles(getNextFollowArticle(uid,0,20));

		// su.setArticles(getList(uid));
		//
		// List<ShortUser> follows = getFollowList(uid);
		// su.setFollows(conver(follows));
		// su.setBefollows(conver(getBefollowList(uid)));
		/*
		 * 好友文章 已经用分页流方式 getNextFollowArticle if(follows != null){ List<Article>
		 * artList = new ArrayList<Article>(); for(ShortUser follow : follows ){
		 * artList.addAll(getList(follow.getUid())); } //关注的用户文章的排序，按时间倒序
		 * ComparatorArticle comparatorarticle= new ComparatorArticle();
		 * Collections.sort(artList,comparatorarticle);
		 * su.setFollowArticles(artList); }
		 */

		// su.setFavorites(getFavoriteList(uid));
		return su;
	}

	/**
	 * 删除文章
	 *
	 * @param uid
	 * @param auuid
	 * @return
	 */
	public String delArticle(Long uid, String auuid) {
		try {
			Article article = MicorBlogService.getInstance().getArticle(uid, auuid);
			saveEsArticleUUids(auuid, 3);
			List<Comment> commentList = MicorBlogService.getInstance().getCommentByPage(auuid, 0, 100);
			// 物理删除
			String sKey = String.valueOf(uid);
			String[] columns = { "time", "type", "suuid", "tags" };
			Map<String, String> getArtMap = ch.get(SAVE_TABLE.ARTICLE.toString(),
					auuid, columns);
			long time = 0l;
			String tags = "";// 600703.sh:三安光电,300303.sz:聚飞光电,300269.sz:联建光电,
			try {
				if(getArtMap.size()==0){
					return StockCodes.ERROR;
				}
				String timeStr = getArtMap.get("time");
				time = Long.parseLong(timeStr);
				tags = getArtMap.get("tags");
			} catch (Exception e) {
				e.printStackTrace();
				return StockCodes.ERROR;
			}
			// 删除时光轴 用户的文章
			tls.deltetTimeLine(sKey, SAVE_TABLE.ARTICLE, time);
			// 删除文章实体
			ch.delete(SAVE_TABLE.ARTICLE.toString(), auuid);
			List<TimeLine> timelineList = tls.getTimeLineList(auuid,
					SAVE_TABLE.COMMENT, 0, Integer.MAX_VALUE);
			tls.deltetTimeLine(auuid, SAVE_TABLE.COMMENT);// 删除时光轴上的文章[带评论列表]
			// 删除文章评论组的实体
			ch.delete(SAVE_TABLE.COMMENT.toString(),
					timelineListToStringArray(timelineList));
			//删除机会广场博文关系
			SquareStockChanceService.getInstance().deleteSquareStockChance(auuid);
			//删除话题推荐博文的关系
			Map<String, Serializable> attr = article.get_attr();
			if(attr!=null){
				String tuid = String.valueOf(article.getAttr("tuid"));
				long time2 =0l;
				String t = String.valueOf(article.getAttr("time"));
				if(StringUtils.isNumericSpace(t)){
					time2 = Long.parseLong(t);
				}
				if(time2>0){
					// 删除时光轴 用户的文章
					tls.deltetTimeLine(tuid, SAVE_TABLE.ARTICLE, time2);
					//删除博文uid-uuid关系
					UserMsg um = SMsgFactory
							.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_16);
					um.setS(tuid);
					um.setD(tuid);
					um.putAttr("uuid", auuid);
					um.putAttr("doDelete", "true");
					UserEventService.getInstance().notifyTheEvent(um);
				}

			}
			if (StringUtil.isEmpty(tags) == false) {
				String[] tagArr = tags.split(",");
				for (String tag : tagArr) {
					if (StringUtil.isEmpty(tag) == true)
						continue;
					String companycode = tag.split(":")[0];
					tls.deltetTimeLine(companycode, SAVE_TABLE.TOPIC, time);
					//删除公司，话题底下 identify-uuid的关系
					RemindServiceClient.getInstance().removeIdentifyArticle(companycode,auuid);
				}
			}
			// 转发的文章,删除原文的broadcast_counts 和 对应的评论
			if ((ShareConst.ZHUANG_ZAI + "").equals(getArtMap.get("type"))) {
				String c0 = "broadcast_counts";
				String suuid = getArtMap.get("suuid");
				updateArticleCount(suuid, c0, false);
				// 原UUID 当前文章UUID=评论UUID
				delComment(uid, suuid, auuid);
				Map<String,Object> map = Maps.newHashMap();
				//map.put("broadcast_counts", false);//dcss 转发数-1
				//RemindServiceClient.getInstance().updateSimpleArticle(suuid, map);
			}

			String s = "article_counts";
			userExtSubtract(s, sKey);

			//发送广播 删除博文uid-uuid关系
			UserMsg um = SMsgFactory
					.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_0);
			um.setS(String.valueOf(article.getUid()));
			um.setD(String.valueOf(article.getUid()));
			um.putAttr("uuid", auuid);
			um.putAttr("doDelete", "true");
			UserEventService.getInstance().notifyTheEvent(um);
			//删除dcss 缓存中的博文
			RemindServiceClient.getInstance().deleteSimpleArticle(auuid);
			//删除消息中心@我的博文
			if(article!=null){
				String content = article.getContent();
				String[] nickNameArr = CommonUtil.matcherNames(content);
				for(String nickname : nickNameArr){
					String atUid = "";
					String n = nickname.split(":")[0];//先做一个兼容
					if(StringUtils.isNumeric(n)){
						atUid = n;
					}else{
						atUid = String.valueOf(UserServiceClient.getInstance().getUidByNickname(nickname));;
					}
					if(StringUtils.isNotBlank(atUid) && ! "null".equals(atUid.trim().toLowerCase())){
						RemindServiceClient.getInstance().clearMessage(Long.parseLong(atUid), MsgConst.MSG_USER_TYPE_3, auuid);
					}
				}
			}
			if(commentList!=null && commentList.size()>0){
				for(Comment c :commentList){
					if(c==null){
						continue;
					}
					//删除我收到的评论
					if(c.getUid()!=uid){//当评论者问文章的作者时 不需要删除
						RemindServiceClient.getInstance().clearMessage(uid, MsgConst.MSG_USER_TYPE_4, c.getUuid());//这里的uid为博文作者uid
					}
					//删除我发出的评论
					RemindServiceClient.getInstance().clearMessage(c.getUid(), MsgConst.MSG_USER_TYPE_14, c.getUuid());//这里是评论者uid
					//删除 消息中心 赞
					RemindServiceClient.getInstance().clearMessage(article.getUid(), MsgConst.MSG_USER_TYPE_2, auuid);
					//删除@我的评论
					String content = c.getContent();
					String[] nickNameArr = CommonUtil.matcherNames(content);
					for(String nickname : nickNameArr){
						String atUid = TopicService.getInstance().searchUidByNickName(nickname);
						if(StringUtils.isNotBlank(atUid)){
							RemindServiceClient.getInstance().clearMessage(Long.parseLong(atUid), MsgConst.MSG_USER_TYPE_13, c.getUuid());
						}
					}
					//删除回复的评论
					if(c.getAt()!=null && c.getPath()!=null){
						long cuid = 0;
						String cuidStr = c.getAt().split(":")[0];//原评论
						if(StringUtils.isNumericSpace(cuidStr)){
							try {
								cuid = Long.parseLong(cuidStr);//评论中被回复者的uid
							} catch (NumberFormatException e) {
								logger.info("异常："+e );
							}
							if(cuid!=0){
								RemindServiceClient.getInstance().clearMessage(cuid, MsgConst.MSG_USER_TYPE_4,c.getUuid());
							}
						}
					}
				}
			}
		} catch (NumberFormatException e) {
			logger.info("del fail "+e);
			return StockCodes.ERROR;

		}

		return StockCodes.SUCCESS;
	}

	/**
	 * 把推荐博文变回原博文
	 *
	 * @param uid
	 * @param auuid
	 * @return
	 */
	public String delRecommendArticle(long uid, String auuid) {
		try {
			SimpleArticle article = RemindServiceClient.getInstance().getSimpleArticle(auuid);
			Long timemillis = Long.parseLong(article.get_attr().get("time").toString());
			// 物理删除
			String sKey = String.valueOf(uid);
			// 删除时光轴 用户的文章
			if(timemillis!=null && timemillis>0)
				tls.deltetTimeLine(sKey, SAVE_TABLE.ARTICLE, timemillis);
			//删除博文uid-uuid关系
			//UserMsg um = SMsgFactory.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_0);
			UserMsg um = SMsgFactory.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_16);
			um.setS(sKey);
			um.setD(sKey);
			um.putAttr("uuid", auuid);
			um.putAttr("doDelete", "true");
			UserEventService.getInstance().notifyTheEvent(um);
			//把博文实体变回原来的样子
			//article.set_attr(null);
			//article.removeAttr("price");
			article.removeAttr("tuid");
			article.removeAttr("text");
			article.removeAttr("time");
			Map<String, String> map = NosqlBeanUtil.bean2Map(article);
			ch.insert(SAVE_TABLE.ARTICLE.toString(),article.getUuid(), map);
/*			Map<String, String> map = Maps.newHashMap();
			map.put("_attr", null);
			//修改博文 去掉_attr字段
			ch.deleteName(SAVE_TABLE.ARTICLE.toString(), article.getUuid(),"_attr");*/
			//ch.insert(SAVE_TABLE.ARTICLE.toString(),article.getUuid(), map);
			RemindServiceClient.getInstance().putSimpleArticle(article.getUuid(), article);
			//把原来的关系链加上
			/*String key = String.valueOf(article.getUid());
			UserMsg um2 = SMsgFactory
					.getBrodCastUserMsgByType(MsgConst.MSG_USER_TYPE_0);
			um2.setS(key);
			um2.setD(key);
			String suuid = article.getSuuid();
			// 如果是转发的博文　需要把转发信息也放在
			if(article.getType() == ShareConst.ZHUANG_ZAI && suuid!=null){
				um2.putAttr("type", ShareConst.ZHUANG_ZAI);
			}else{
				um2.putAttr("type", ShareConst.YUAN_CHUANG);
			}
			um2.putAttr("uuid",article.getUuid());
			um2.setTime(article.getTime());
			UserEventService.getInstance().notifyTheEvent(um2);*/
		} catch (Exception e) {
			logger.info("delRecommendArticle"+e);
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}
	/**
	 * 删除话题博文关系
	 *
	 * @param uid
	 * @param auuid
	 * @return
	 */
	public String delTopicArticle(String identify, String auuid,long time ) {
		try {

		} catch (Exception e) {
			logger.info("delTopicArticle"+e);
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}


	/**
	 * 某用户自己的文章 分页
	 *
	 * @param uid
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<Article> getNextArticle(long uid, int start, int limit) {
		String sKey = String.valueOf(uid);
		List<String> uuidList = tls.getTimeLine(sKey, SAVE_TABLE.ARTICLE,
				start, limit);
		return getArticleList(uuidList);
	}

	/**
	 * 某用户的自己和好友的最新文章 时光轴
	 */
	private List<TimeLine> getNewsArticleTL(long uid, long starttime,
			long endtime) {
		List<ShortUser> follows = getFollowList(uid);
		List<TimeLine> timelineList = new ArrayList<TimeLine>();
		List<String> uidList = new ArrayList<String>();
		uidList.add(String.valueOf(uid));
		// 增加好友的文章
		if (follows != null) {
			for (ShortUser follow : follows) {
				uidList.add(String.valueOf(follow.getUid()));
			}
		}
		String[] uidArr = {};
		uidArr = uidList.toArray(uidArr);

		// 可能未来扩展增加好友时，将所有好友的文章合并压入好友文章TimeLine
		timelineList.addAll(tls.getTimeLineListByTime(uidArr,
				SAVE_TABLE.ARTICLE, starttime, endtime));
		return timelineList;
	}

	private String[] timelineListToStringArray(List<TimeLine> timelineList) {
		List<String> uuidList = new ArrayList<String>();
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}

		String[] uuidStrArray = new String[uuidList.size()];
		uuidList.toArray(uuidStrArray);
		return uuidStrArray;
	}

	private List<String> timelineListToStringList(List<TimeLine> timelineList) {
		List<String> uuidList = new ArrayList<String>();
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}
		return uuidList;
	}

	/**
	 * 提供服务 显示最新XX条 博文列表
	 */
	public List<Article> getNewsArticleList(long uid, long starttime,
			long endtime) {
		List<Article> artList = new ArrayList<Article>();
		List<TimeLine> timelineList = getNewsArticleTL(uid, starttime, endtime);
		// 关注的用户文章的排序，按时间倒序
		ComparatorTimeLine comparatorarticle = new ComparatorTimeLine();
		Collections.sort(timelineList, comparatorarticle);

		artList = getArticleList(timelineListToStringList(timelineList));
		return artList;
	}

	public List<Article> getNextMyArticle(long uid, int start, int limit) {
		List<Article> artList = new ArrayList<Article>();
		String uidStr = String.valueOf(uid);
		List<TimeLine> timelineList = tls.getTimeLineList(uidStr,
				SAVE_TABLE.ARTICLE, start, limit);

		artList = getArticleList(timelineListToStringList(timelineList));
		return artList;
	}

	/**
	 * 某用户好友的文章 分页
	 *
	 * @deprecated
	 */
	public List<Article> getNextFollowArticle(long uid, int start, int limit) {
		return getNextFollowArticle(uid, 0l, start, limit);
	}

	/**
	 * 某用户好友的文章 分页 <br>
	 * 新接口
	 */
	public List<Article> getNextFollowArticle(long uid, long time, int start,
			int limit) {
		List<Article> artList = new ArrayList<Article>();
		// 未来重构 可以模仿新浪微博 转为推送模式
		String[] uidArr = {};
		if (uid == 0l) {
			// activesUsers:10085,10086,20001,20002,20004,20005,20006,20009,20010
			String recommendUsers = ConfigCenterFactory.getString(
					"stock_zjs.recommend_users", "");
			if (StringUtil.isEmpty(recommendUsers) == false
					&& recommendUsers.split(":").length >= 2) {
				uidArr = recommendUsers.split(":")[1].split(",");
			}
		} else {
			List<String> uidList = new ArrayList<String>();
			uidList.add(String.valueOf(uid));
			List<ShortUser> follows = getFollowList(uid);
			// 增加好友
			if (follows != null) {
				for (ShortUser follow : follows) {
					uidList.add(String.valueOf(follow.getUid()));
				}
			}
			List<String> stockList = StockSeqService.getInstance().getUserStockSeqList(uid);
			if(stockList != null && stockList.size() > 0) {
				for(String stock : stockList) {
					USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(stock);
					if(usubject !=null && usubject.getUid() >0l){
						uidList.add(String.valueOf(usubject.getUid()));
					}
				}
			}
			uidArr = uidList.toArray(uidArr);
		}
		List<TimeLine> timelineList = tls.getTimeLineListByTime(uidArr,
				SAVE_TABLE.ARTICLE, time, start, limit);// tls.getTimeLineList(uidArr,SAVE_TABLE.ARTICLE,start,limit);
		List<String> uuidList = new ArrayList<String>();
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}

		artList = getArticleList(uuidList);
		return artList;
	}

	/**
	 * 某用户 相关公司的博文
	 *
	 * @param uid
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<Article> getMyArticleByCompany(long uid, int start, int limit) {
		List<Article> artList = new ArrayList<Article>();
//		List<UserStock> usList = UserServiceClient.getInstance().getStockList(
//				uid);
//		if (usList == null || usList.size() == 0) {
//			usList = UserStockService.getInstance().getSelect(uid);
//		}
//		if (usList == null || usList.size() == 0)
//			return artList;
		List<String> stockList = StockSeqService.getInstance().getUserStockSeqList(uid);
		String[] ccArr = null;
		if(stockList != null && stockList.size() > 0) {
			ccArr = new String[stockList.size()];
			for(int i = 0; i < stockList.size(); i++) {
				ccArr[i] = stockList.get(i);
			}
		}
		if(ccArr != null) {
			List<TimeLine> timelineList = tls.getTimeLineList(ccArr,
					SAVE_TABLE.TOPIC, start, limit);
			List<String> uuidList = new ArrayList<String>();
			for (TimeLine tl : timelineList) {
				uuidList.add(tl.getUuid());
			}
			artList = getArticleList(uuidList);
			return artList;
		}
		return null;
	}

	public List<Article> getArticlePageListByCompany(String companyCode,
			int start, int num) {
		return getArticlePageListByCompany(companyCode, 0l, start, num);
	}

	public List<Article> getArticlePageListByCompany(String companyCode,
			long endtime, int start, int num) {
		List<TimeLine> timelineList = tls.getTimeLineListByTime(companyCode,
				SAVE_TABLE.TOPIC, endtime, start, num); // tls.getTimeLineList(companyCode,
														// SAVE_TABLE.TOPIC,
														// start, num);
		List<Article> artList = new ArrayList<Article>();
		List<String> uuidList = new ArrayList<String>();
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}

		String[] uuidStrList = new String[uuidList.size()];
		uuidList.toArray(uuidStrList);

		if (uuidStrList.length != 0) {
			artList = getArticleList(uuidList);
		}
		return artList;
	}
	public List<SimpleArticle> getSimpleArticlePageListByCompany(String companyCode,
			long endtime, int start, int num) {
		List<TimeLine> timelineList = tls.getTimeLineListByTime(companyCode,
				SAVE_TABLE.TOPIC, endtime, start, num); // tls.getTimeLineList(companyCode,
		// SAVE_TABLE.TOPIC,
		// start, num);
		List<SimpleArticle> artList = new ArrayList<SimpleArticle>();
		if (timelineList!=null && timelineList.size()>0) {
			artList = getSimpleArticleList(timelineList);
		}
		return artList;
	}
	public List<SimpleArticle> getSimpleArticleByTime(String companyCode,
			long startTime, long endTime) {
		List<TimeLine> timelineList = tls.getTimeLineListByTime(companyCode,
				SAVE_TABLE.TOPIC, startTime, endTime, Integer.MAX_VALUE); // tls.getTimeLineList(companyCode,
		// SAVE_TABLE.TOPIC,
		// start, num);
		List<SimpleArticle> artList = new ArrayList<SimpleArticle>();
		if (timelineList!=null && timelineList.size()>0) {
			artList = getSimpleArticleList(timelineList);
		}
		return artList;
	}
	//nosql 查询主持人推荐博文
	public List<SimpleArticle> getRecommendArticlePageListByCompany(String companyCode,
			long endtime, int start, int num) {
		List<TimeLine> timelineList = tls.getTimeLineListByTime(companyCode,
				SAVE_TABLE.TOPICRECOMMEND, endtime, start, num);
		List<SimpleArticle> artList = new ArrayList<SimpleArticle>();
		if (timelineList!=null && timelineList.size()>0) {
			artList = getSimpleArticleList(timelineList);
		}
		return artList;
	}
	//nosql 查询主持人推荐博文(from startTime)
	public List<SimpleArticle> getRecommendArticleByTime(String companyCode,
			long startTime,long endTime) {
		List<TimeLine> timelineList = tls.getTimeLineListByTime(companyCode,
				SAVE_TABLE.TOPICRECOMMEND, startTime, endTime, Integer.MAX_VALUE);
		List<SimpleArticle> artList = new ArrayList<SimpleArticle>();
		if (timelineList!=null && timelineList.size()>0) {
			artList = getSimpleArticleList(timelineList);
		}
		return artList;
	}

	public List<Article> getArticleListByTime(String uid, long starttime,
			long endtime) {
		List<TimeLine> timelineList = tls.getTimeLineListByTime(uid,
				SAVE_TABLE.ARTICLE, starttime, endtime, Integer.MAX_VALUE);
		List<Article> artList = new ArrayList<Article>();
		List<String> uuidList = new ArrayList<String>();
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}
		String[] uuidStrList = new String[uuidList.size()];
		uuidList.toArray(uuidStrList);
		if (uuidStrList.length != 0) {
			artList = getArticleList(uuidList);
		}
		return artList;
	}

	public List<Article> getArticlePageListByAt(String uid, int start, int num) {
		List<TimeLine> timelineList = tls.getTimeLineList(uid, SAVE_TABLE.AT,
				start, num);
		List<Article> artList = new ArrayList<Article>();
		List<String> uuidList = new ArrayList<String>();
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}
		String[] uuidStrList = new String[uuidList.size()];
		uuidList.toArray(uuidStrList);
		if (uuidStrList.length != 0) {
			artList = getArticleList(uuidList);
		}
		return artList;
	}

	public List<Comment> getCommentByPage(String auuid, int start, int limit) {
		List<TimeLine> timelineList = tls.getTimeLineList(auuid,
				SAVE_TABLE.COMMENT, start, limit);
		List<String> uuidList = new ArrayList<String>();
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}
		String[] uuidStrList = new String[uuidList.size()];
		uuidStrList = uuidList.toArray(uuidStrList);

		List<Comment> retList = new ArrayList<Comment>();
		Set<String> columnsSet = ch.getColumnsSet(Comment.class);
		String[] columns = {};
		columns = columnsSet.toArray(columns);
		Map<String, Map<String, String>> getMMap = ch.get(
				SAVE_TABLE.COMMENT.toString(), uuidStrList, columns);
		for (String uuid : uuidStrList) {
			Map commentMap = getMMap.get(uuid);
			Comment comment = new Comment();
			NosqlBeanUtil.map2Bean(comment, commentMap);
			retList.add(comment);
		}
		return retList;
	}
	public Map<String,Object> getCommentByTime(String code,long startTime,int type,int limit){
//		Long endTime =System.currentTimeMillis();
//		if(type==0){
//
//		}else if(type==1){
//			endTime = startTime;
//			startTime = 0L;
//		}else{
//			startTime = 0L;
//		}
//		if(startTime>endTime){
//			logger.error("getArticleListByTime------->开始时间大于结束时间");
//			startTime = endTime;
//		}
//		List<TimeLine> timelineList  = tls.getTimeLineListByTime(code,SAVE_TABLE.COMMENT,startTime,endTime,limit);
//		List<String> uuidList = new ArrayList<String>();
//		for (TimeLine tl : timelineList) {
//			uuidList.add(tl.getUuid());
//		}
//		String[] uuidStrList = new String[uuidList.size()];
//		uuidStrList = uuidList.toArray(uuidStrList);
//
//		List<Comment> retList = new ArrayList<Comment>();
//		Set<String> columnsSet = ch.getColumnsSet(Comment.class);
//		String[] columns = {};
//		columns = columnsSet.toArray(columns);
//		Map<String, Map<String, String>> getMMap = ch.get(
//				SAVE_TABLE.COMMENT.toString(), uuidStrList, columns);
//		for (String uuid : uuidStrList) {
//			Map commentMap = getMMap.get(uuid);
//			Comment comment = new Comment();
//			NosqlBeanUtil.map2Bean(comment, commentMap);
//			retList.add(comment);
//		}
//
		List<Comment> retList = RemindServiceClient.getInstance().getArticleCommentList(code, type, startTime, limit);
		String startTime1 = "";
		String endTime1 = "";
		if(retList!=null && retList.size()>0){
			int size =  retList.size()-1;
			startTime1 = String.valueOf(retList.get(size).getTime());
			endTime1 = String.valueOf(retList.get(0).getTime());
		}
//		if(timelineList!=null && timelineList.size()>0){
//			int size =  timelineList.size()-1;
//			startTime1 = timelineList.get(0).getTimeMillis();
//			endTime1 = timelineList.get(size).getTimeMillis();
//		}
		Map<String,Object> resultMap = new HashMap<String,Object>();
		resultMap.put("commentList", retList);
		resultMap.put("startTime", startTime1);
		resultMap.put("endTime", endTime1);
		return resultMap;

	}

	/**
	 * @param hotType
	 *            热门类型 如day1hot
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<Article> getIndexPageArticle(String hotType, int start,
			int limit) {
		CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
		String[] columns = { "value" };
		Map<String, String> uuidMap = ch.get("hot", hotType, columns);
		if (uuidMap.isEmpty())
			return null;
		String[] uuid = uuidMap.get("value").split(",");
		int endIndex = (start + limit) > uuid.length ? uuid.length
				: (start + limit);
		List<String> uuidList = new ArrayList<String>();
		for (int i = start; i < endIndex; i++) {
			uuidList.add(uuid[i]);
		}
		List<Article> outList = MicorBlogService.getInstance().getArticleList(
				uuidList);
		return outList;
	}

	// 每个财务分析图的前20个评论
	/**
	 * @param companycode
	 * @param sKeyArr
	 *            财务页面定位KEY 如{"chart_01","chart_02","chart_03"};
	 * @return
	 */
	public Map<String, List<Comment>> getCompanyCommit(String companycode,
			String[] sKeyArr) {
		Map<String, List<Comment>> reMap = new HashMap<String, List<Comment>>();
		for (String sKey : sKeyArr) {
			List<Comment> commentList = getCompanyCommitByKeyAndPage(
					companycode, sKey, 0, 20);
			reMap.put(sKey, commentList);
		}
		return reMap;
	}

	public int sizeCompanyCommitByKey(String companycode, String key) {
		return ch
				.getTimeLineTotalCount(TimeLineService.TABLE, companycode, key);
	}

	// 单个财务分析图的评论翻页
	public List<Comment> getCompanyCommitByKeyAndPage(String companycode,
			String key, int start, int limit) {
		List<Comment> commentList = new ArrayList<Comment>();
		List<TimeLine> tlList = ch.getTimeLine(TimeLineService.TABLE,
				companycode, key, start, limit);
		String[] uuidArr = new String[tlList.size()];
		for (int i = 0; i < tlList.size(); i++) {
			uuidArr[i] = tlList.get(i).getUuid();
		}
		String[] columns = ch.getColumns(Comment.class);
		Map<String, Map<String, String>> getMap = ch.get(
				SAVE_TABLE.COMMENT.toString(), uuidArr, columns);
		for (String time : uuidArr) {
			Comment c = new Comment();
			Map<String, String> commentObjMap = getMap.get(time);
			try {
				NosqlBeanUtil.map2Bean(c, commentObjMap);
			} catch (Exception e) {
				logger.error("转换Comment对象出错", e);
			}
			commentList.add(c);
		}
		return commentList;
	}

	/**
	 * 给公司的财务图表加评论
	 *
	 * @param companycode
	 * @param key
	 *            财务页面定位KEY
	 * @param comment
	 * @return
	 */
	public String addCompanyCommit(String companycode, String key,
			Comment comment) {
		try {
			String sKey = String.valueOf(comment.getUuid());
			long timemillis = comment.getTime();
			if (timemillis < 1l) {
				timemillis = System.currentTimeMillis();
				comment.setTime(timemillis);
			}
			Map<String, String> commentMap = NosqlBeanUtil.bean2Map(comment);
			// tls.saveTimeLine(sKey, comment.getUuid(),
			// TimeLineService.SAVE_TABLE.COMMENT,timemillis);
			Map<String, Map<String, String>> superMap = new HashMap<String, Map<String, String>>();
			Map<String, String> tm = new HashMap<String, String>();
			tm.put(String.valueOf(comment.getTime()), sKey);
			superMap.put(key, tm);
			ch.insertSuper(TimeLineService.TABLE, companycode, superMap);
			ch.insert(SAVE_TABLE.COMMENT.toString(), sKey, commentMap);
			String s = "comment_counts";
			this.userExtAdd(s, comment.getUid() + "");
		} catch (Exception e) {
			logger.error("增加评论失败", e);
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;

	}

	/**
	 * 删除公司的财务图表的某条评论
	 *
	 * @param companycode
	 * @param key
	 *            财务页面定位KEY
	 * @param uuid
	 * @return
	 */
	public String deleteCompanyCommit(String companycode, String key,
			String uuid) {
		try {
			Comment comment = this.getComment(companycode, uuid);
			ch.deltetNameInSuperColumn(TimeLineService.TABLE, companycode, key,
					String.valueOf(comment.getTime()));
			ch.delete(SAVE_TABLE.COMMENT.toString(), uuid);
			String s = "comment_counts";
			this.userExtSubtract(s, String.valueOf(comment.getUid()));
		} catch (Exception e) {
			logger.error("删除评论失败", e);
			return StockCodes.ERROR;
		}
		return StockCodes.SUCCESS;
	}

	/**
	 * 获得好友的新博文数<br>
	 * 供主页使用 如果切换成新浪的推模式，需要重构
	 *
	 * @param uid
	 *            用户UID
	 * @return
	 */
	public int sizeNewArticleByFollow(long uid, long startTimemillis,
			long endTimemillis) {
		int reInt = 0;
		List<ShortUser> suList = this.getFollowList(uid);
		for (ShortUser su : suList) {
			int size = tls.getCount(String.valueOf(su.getUid()),
					SAVE_TABLE.ARTICLE, startTimemillis, endTimemillis);
			reInt += size;
		}
		//增加公司的默认用户
//		List<UserStock> usList = UserServiceClient.getInstance().getStockList(uid);
		List<String> list = StockSeqService.getInstance().getUserStockSeqList(uid);
		if(list != null && list.size() > 0){
			for(String stock : list){
				USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(stock);
				if(usubject !=null && usubject.getUid() >0l){
					long usUid = usubject.getUid();
					int size = tls.getCount(String.valueOf(usUid),
							SAVE_TABLE.ARTICLE, startTimemillis, endTimemillis);
					reInt += size;
				}
			}
		}

		int size = tls.getCount(String.valueOf(uid), SAVE_TABLE.ARTICLE,
				startTimemillis, endTimemillis);
		reInt += size;
		return reInt;
	}

	/**
	 * 获得公司的新博文数
	 */
	public int sizeNewArticle(String companycode, long startTimemillis,
			long endTimemillis) {
		int size = tls.getCount(companycode, SAVE_TABLE.TOPIC, startTimemillis,
				endTimemillis);
		// //浏览器同服务器时间有差异 无法精准确定新博文数量 减去自己最新发表
		// if(size == 1){
		// size = 0;
		// }
		return size;
	}

	// 取根据本机ip取出，本用户在本机上存储的活跃用户
	public List<Long> getBefollowListOnlineLocal(Long uid) {

		/*String[] ips = sips.split("\\^");
		int dcssNums = ips.length;
		int index = StockUtil.getUserIndex(String.valueOf(uid))%dcssNums;
		if (serial != -1) {
			//如果此用户的上下文就在本机上存储，就直接从缓存中取，如果不是，就在远端取
			if(index==serial)
			{
				ll = getBefollowListOnlineLocalFromCache(uid, serial);
			}
			else
			{
				ll = UserServiceClient.getInstance()
						.getBefollowListOnlineLocalFromRemoteCache(uid, serial);
			}
		}*/
		return getBefollowListOnlineLocalFromCache(uid);
	}

	/**
	 * activeUserMap是在此notifyFollowLogin方法中完成构建
	 *
	 * @param uid
	 * @param serial
	 * @return
	 */
	public List<Long> getBefollowListOnlineLocalFromCache(Long uid) {
		List<Long> ll = new ArrayList<Long>();
		String key = "active_" + uid;
		String cachename = StockUtil.getUserCacheName(key);
		Map<Long, Long> activeUserMap = LCEnter.getInstance().get(key,
				cachename);
		if (activeUserMap != null) {
			ll.addAll(activeUserMap.keySet());
		}
		return ll;
	}

	public void notifyFollowDeleteLogin(Long gzUid, Long myUid) {
//		if(!isLogin(myUid))
//			return;
		String key = "active_" + myUid ;
		String cachename = StockUtil.getUserCacheName(key);
		Map<Long, Long> activeUserMap = LCEnter.getInstance().get(key,
				cachename);
		if (activeUserMap != null) {
			//删除自选股 取消他们的收听[在线]关系
			activeUserMap.remove(gzUid);
		}

//		String stockWithUser = CacheUtil.getCacheName(AAAConstants.USUBJECT_WITH_USER_CACHENAME);
//		StringBuffer uidBuf = LCEnter.getInstance().get(myUid, stockWithUser);
//		if(uidBuf != null && uidBuf.indexOf("#"+gzUid+"#,") >=0){
//			uidBuf.replace(uidBuf.indexOf("#"+gzUid+"#,"), uidBuf.indexOf("#"+gzUid+"#,")+("#"+gzUid+"#,").length(), "");
//			LCEnter.getInstance().put(myUid, uidBuf, stockWithUser);
//		}
	}

	/**
	 * 删除自选股时
	 * @param gzUid
	 * @param usubjectUid
	 */
	public void notifyUsubjectDelete(Long gzUid, Long usubjectUid) {
		//写在线缓存
		String key = "active_" + usubjectUid ;
		String cachename = StockUtil.getUserCacheName(key);
		Map<Long, Long> activeUserMap = LCEnter.getInstance().get(key,
				cachename);
		if (activeUserMap != null) {
			//删除自选股 取消他们的收听[在线]关系
			activeUserMap.remove(gzUid);
		}
		//公司的所有用户
		String usUidCacheName = CacheUtil.getCacheName(AAAConstants.USUBJECT_WITH_USER_CACHENAME);
		Set<Long> uidSet = LCEnter.getInstance().get(usubjectUid, usUidCacheName);
		if(uidSet != null && uidSet.contains(gzUid) == true ){
			uidSet.remove(gzUid);
		}
	}

	/**
	 * 增加自选股/关注话题时，写缓存
	 * @param gzUid
	 * @param usubjectUid
	 */
	public void notifyUsubjectAdd(Long gzUid, Long usubjectUid) {
		//公司的在线用户
		String key = "active_" + usubjectUid;
		String cachename = StockUtil.getUserCacheName(key);
		Map<Long, Long> activeUserMap = LCEnter.getInstance().get(key,
				cachename);
		if (activeUserMap == null) {//可能是新公司或没人关注的公司
			activeUserMap = new ConcurrentHashMap<Long, Long>();
			LCEnter.getInstance().put(key, activeUserMap, cachename);
		}
		if(activeUserMap.get(gzUid)==null){
			activeUserMap.put(gzUid, System.currentTimeMillis());
		}
		//公司的所有用户
		String usUidCacheName = CacheUtil.getCacheName(AAAConstants.USUBJECT_WITH_USER_CACHENAME);
		Set<Long> uidSet = LCEnter.getInstance().get(usubjectUid, usUidCacheName);
		if(uidSet != null && uidSet.contains(gzUid) == false ){
			uidSet.add(gzUid);
		}
		//启动单个dcss加载
//		else{
//			uidSet = new HashSet<Long>();
//			uidSet.add(gzUid);
//			LCEnter.getInstance().put(usubjectUid,uidSet, usUidCacheName);
//		}
	}

	/**
	 * activeUserMap 需要定时清理，清理任务未实现,suid与uid关系，suid收听uid
	 *
	 * @param gzUid 观众
	 * @param uid 我
	 */
	public void notifyFollowLogin(Long gzUid, Long myUid) {
//		if(!isLogin(myUid))
//			return;
		//计算suid所在的机器序号
		//如果myuid的funs数小于这个数就在myuid所在的机器上建立用户关系

		long maxFansNum  = ConfigCenterFactory.getLong(
				"dcss.max_fans_num", 500L);
		long activityTime  = ConfigCenterFactory.getLong(
				"dcss.user_activity_time", 1000*60*60*24*7L);
		//String sips = ConfigCenterFactory.getString(
		//		"dcss.ext_cache_server_ip_list_udp", "192.168.1.102:6666");
		//String[] ips = sips.split("\\^");
		//int dcssNums = ips.length;
		//int index = StockUtil.getUserIndex(String.valueOf(myUid)) % dcssNums;
		String key = "active_" + myUid;
		String cachename = StockUtil.getUserCacheName(key);
		synchronized (myUid) {
			Map<Long, Long> activeUserMap = LCEnter.getInstance().get(key,
					cachename);
			if (activeUserMap == null) {
				Long ltime = RemindServiceClient.getInstance().lastLoginTime(myUid);
				//判断是否登录
				if(ltime==null){
					//判断myUid粉丝数小于maxFansNum 且myuid的登录时间在activityTime以前 把关系放在myUid登录时候建立
					String fanskey = String.valueOf(myUid);
					int fansNum = CassandraHectorGateWay.getInstance().getCountSize(SAVE_TABLE.BEFOLLOW.toString(), fanskey);
					if(fansNum<maxFansNum){
						//logger.info("uid:"+myUid+" 没有登录，粉丝数 "+fansNum +" 不建立关系");
							return;
					}
				}
				//logger.info("uid:"+myUid+" 已经登录 ，建立关系");
				activeUserMap = new ConcurrentHashMap<Long, Long>();
				LCEnter.getInstance().put(key, activeUserMap, cachename);
			}
			if(activeUserMap.get(gzUid)==null){
				activeUserMap.put(gzUid, System.currentTimeMillis());
			}
//
//			String stockWithUser = CacheUtil.getCacheName(AAAConstants.USUBJECT_WITH_USER_CACHENAME);
//			StringBuffer uidBuf = LCEnter.getInstance().get(myUid, stockWithUser);
//			if(uidBuf != null && uidBuf.indexOf("#"+gzUid+"#,") < 0){
//				uidBuf.append("#"+gzUid+"#,");
//				LCEnter.getInstance().put(myUid, uidBuf, stockWithUser);
//			}
		}
	}

	public void putFeisiIsLogin(Long myUid,Long fsUid){
	/*	if(!isLogin(fsUid)){
			return;
		}*/
		//计算suid所在的机器序号
		/*String sips = ConfigCenterFactory.getString(
				"dcss.ext_cache_server_ip_list_udp", "192.168.1.102:6666");

		String[] ips = sips.split("\\^");
		int dcssNums = ips.length;
		int index = StockUtil.getUserIndex(String.valueOf(myUid)) % dcssNums;
		int sserial = index;*/
		String key = "active_" + myUid;
		String cachename = StockUtil.getUserCacheName(key);
		Map<Long, Long> activeUserMap = LCEnter.getInstance().get(key,
				cachename);
		if (activeUserMap == null) {
			activeUserMap = new ConcurrentHashMap<Long, Long>();
			LCEnter.getInstance().put(key, activeUserMap, cachename);
		}
		if(activeUserMap.get(fsUid)==null){
			activeUserMap.put(fsUid, System.currentTimeMillis());
		}
	}

	public boolean isLogin(Long uid) {
		Long ltime = LCEnter.getInstance().get(uid, SCache.CACHE_NAME_userlogincache);
		if(ltime==null)
			return false;
		return true;
	}

	/**
	 * 获取本机的在线用户 DCSS使用
	 * @return
	 */
	public List<Long> getOnlineUser(){
		Cache logincache = LCEnter.getInstance().getCache(SCache.CACHE_NAME_userlogincache);
		List<Long> keys = logincache.getKeysNoDuplicateCheck();
		return keys;
	}

	public void loginRecord(Long uid,String IP)
	{
		LCEnter.getInstance().put(uid,System.currentTimeMillis(), SCache.CACHE_NAME_userlogincache);
		if(StringUtils.isNotBlank(IP)){
			LCEnter.getInstance().put(uid,IP, SCache.CACHE_NAME_userlogin_ip_cache);
		}
	}	
	
	/**
	 * 初始化我的订阅
	 * @param uid
	 */
	public void initMyIndexWapper(Long uid){
		int myIndexWapperSize = 500;
		Set<String> topicUidList = new HashSet<String>();//关注的话题列表
		//加载关注话题的默认用户
		//建立用户和话题的关注关系
		Map<String, String> tmap = CassandraHectorGateWay.getInstance().get(SAVE_TABLE.TOPICFOLLOW.toString(),String.valueOf(uid));
		List<String> result = CommonUtil.sortMapByVaule(tmap);
		if(result.size()==0){
			String recommondTopic = ConfigCenterFactory.getString("stock_chance.recommend_topic", "分时精选^高手调仓");
			String[] arr = recommondTopic.split("\\^");
			for(String t : arr){
				result.add(t);
			}
		}
		for(String identify : result){
			//建立 uid-话题 关注关系
			USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(identify);
			if(usubject!=null &&  usubject.getUid() >0l && !(usubject.getType()==StockConstants.SUBJECT_TYPE_0)){
				RemindServiceClient.getInstance().addUserTopicRelationship(String.valueOf(uid), identify);
				//建立 uid-虚拟账号 关注关系
				MicorBlogService.getInstance().notifyUsubjectAdd(Long.valueOf(uid),usubject.getUid());
				topicUidList.add(String.valueOf(usubject.getUid()));
			}
		}
		List<TimeLine> timelineList = new ArrayList<TimeLine>();
		//话题加载到我的订阅
		if(topicUidList.size() > 0){
			int asize = myIndexWapperSize;			
			Set<String> uidSet = topicUidList;
			String[] uidArr = new String[uidSet.size()];
			uidArr = uidSet.toArray(uidArr);
			List<TimeLine> timelineListTopic = tls.getTimeLineListByTime(uidArr,
					SAVE_TABLE.ARTICLE, 0l, 0, asize);
			if(timelineListTopic.size() >0 ){
				timelineList.addAll(timelineListTopic);
			}
		}
		
		List<ShortUser> suList = MicorBlogService.getInstance().getFollowList(uid);
		List<String> subsList = new ArrayList<String>();
		if(suList != null && suList.size() > 0){
			for(ShortUser su : suList){
				subsList.add(String.valueOf(su.getUid()));
			}
		}
		// 加载我订阅XX投资的观点
//		List<String> subsList = StockGameFollowService.getInstance().getMyFollowGame(String.valueOf(uid));
		if(subsList.size() > 0){
			int asize = myIndexWapperSize;
			String[] uidArr = new String[subsList.size()];
			uidArr = subsList.toArray(uidArr);
			List<TimeLine> timelineListSubs = tls.getTimeLineListByTime(uidArr,
					SAVE_TABLE.VIEWPOINT, 0l, 0, asize);
			boolean needSort = false;
			if(timelineList!=null && timelineList.size() > 0){
				needSort = true;
			}
			if(timelineListSubs.size() > 0){
				timelineList.addAll(timelineListSubs);
			}else{
				needSort = false;
			}
			
			if(needSort == true){
				ComparatorTimeLine comparatortimeline = new ComparatorTimeLine();
				Collections.sort(timelineList, comparatortimeline);
			}
		}
		
		if(timelineList!=null && timelineList.size() > 0){
			String nKey =  StockUtil.getStockChanceListcKey(String.valueOf(uid));
			
			Map<String,TimeLine> tmpMap = new HashMap<String,TimeLine>();
			if(timelineList!=null){				
				List<TimeLine> removeList = new ArrayList<TimeLine>();
				for(TimeLine tl :timelineList){
					String uuid = tl.getUuid();
					if(tmpMap.containsKey(uuid)){
						removeList.add(tl);
					}else{
						tmpMap.put(uuid, tl);
					}
				}
				timelineList.removeAll(removeList);
				if(timelineList.size() > myIndexWapperSize){
					timelineList = new ArrayList<TimeLine>(timelineList.subList(0, myIndexWapperSize));
				}
			}
			List<SimpleArticle> saList = getSimpleArticleList(timelineList);
			if(saList != null && saList.size() != 0){
								
				int size = saList.size();
				//去掉一些重复的博文 标题完全一样，是抓取过里的文章
				Set<String> tmpSet = new HashSet<String>();
				List<SimpleArticle> removeList = new ArrayList<SimpleArticle>();
				for(int i=(size-1);i>=0;i--){
					SimpleArticle sa = saList.get(i);
					String title = sa.getTitle();
					if(StringUtil.isEmpty(title) == false && title.trim().isEmpty()== false){
						if(tmpSet.contains(title)){
							removeList.add(sa);
						}else{
							tmpSet.add(title);
						}
					}
				}
				saList.removeAll(removeList);
				size = saList.size();
				//将博文数据批量压入DCSS
				RemindServiceClient.getInstance().loadSimpleArticleList(saList.get(0).getUuid(), saList);
				//清理旧数据
				UserEventService.getInstance().clearIMessageListWapper(nKey);
				//历史数据先压入
				for(int i=(size-1);i>=0;i--){
					
					SimpleArticle sa = saList.get(i);
					//新结构 这里只放uid-uuid的对应关系
					Map<String, Serializable> map = sa.get_attr();
					String tuid = String.valueOf(sa.getUid());
					String tuuid = String.valueOf(sa.getUuid());
					String time = String.valueOf(sa.getTime());
					if(map!=null && map.size()>0 ){
						if(map.get("tuid")!=null){
							tuid = String.valueOf(map.get("tuid"));							
						}
					}
					TimeLine tl = tmpMap.get(tuuid);
					time = tl.getTimeMillis();
					
					UserMsg um = new UserMsg();
					um.setS(tuid);
					um.setD(tuid);
					um.setTime(Long.parseLong(time));
					um.putAttr("uuid",tuuid );
					UserEventService.getInstance().put2MessageListWapperBysize(nKey, um, 500);
				}
			}
		}
		
		
	}

	/**
	 * 用户登陆后，异步初始化用户的时间线[它关注好友的最近500篇简单博文]
	 *
	 * @param uid
	 */	
	public void initUserFavoriteWapper(Long uid) {		
		/**
		 * 初始化我的空间 朋友圈
		 */
		String nKey = StockUtil.getFavoriteListcKey(String.valueOf(uid));
		Set<String> friendUidList = new HashSet<String>();//关注的好友列表		
		Set<String> companyUidList = new HashSet<String>();//关注的公司列表
		List<Set<String>> rList = Lists.newArrayList();
		List<ShortUser> follows = getFollowList(uid);
		// 增加好友
		if (follows != null) {
			for (ShortUser follow : follows) {
				friendUidList.add(String.valueOf(follow.getUid()));
			}
		}
		friendUidList.add(String.valueOf(uid));//包括它自己
		//加载关注自选股的默认用户
		List<String> list = StockSeqService.getInstance().getUserStockSeqList(uid);
		if(list != null && list.size() > 0){
			for(String stock : list){
				USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(stock);
				if(usubject !=null && usubject.getUid() >0l){
					companyUidList.add(String.valueOf(usubject.getUid()));										
				}
			}
		}
		
		//合并我的空间的好友列表
		Set<String> myZoneUidList = new HashSet<String>();
		myZoneUidList.addAll(friendUidList);
		myZoneUidList.addAll(companyUidList);		
		
		rList.add(myZoneUidList);//rList.add(friendUidList);
		rList.add(new HashSet<String>());//rList.add(topicUidList);
		rList.add(new HashSet<String>());//rList.add(companyUidList);
		for(int j=0 ;j<rList.size();j++){
			int asize = 300;
			if(j==0){
				asize = 500;
			}else if(j==1){
				asize = 400;
			}else if(j==2){
				asize = 100;
			}
			Set<String> uidSet = rList.get(j);
			String[] uidArr = new String[uidSet.size()];
			uidArr = uidSet.toArray(uidArr);
			List<TimeLine> timelineList = tls.getTimeLineListByTime(uidArr,
					SAVE_TABLE.ARTICLE, 0l, 0, asize);
			if(timelineList!=null){
				Set<String> tmpSet = new HashSet<String>();
				List<TimeLine> removeList = new ArrayList<TimeLine>();
				for(TimeLine tl :timelineList){
					String uuid = tl.getUuid();
					if(tmpSet.contains(uuid)){
						removeList.add(tl);
					}else{
						tmpSet.add(uuid);
					}
				}
				timelineList.removeAll(removeList);
			}
			List<SimpleArticle> saList = getSimpleArticleList(timelineList);
			if(saList != null && saList.size() != 0){
				int size = saList.size();
				//去掉一些重复的博文 标题完全一样，是抓取过里的文章
				Set<String> tmpSet = new HashSet<String>();
				List<SimpleArticle> removeList = new ArrayList<SimpleArticle>();
				for(int i=(size-1);i>=0;i--){
					SimpleArticle sa = saList.get(i);
					String title = sa.getTitle();
					if(StringUtil.isEmpty(title) == false && title.trim().isEmpty()== false){
						if(tmpSet.contains(title)){
							removeList.add(sa);
						}else{
							tmpSet.add(title);
						}
					}
				}
				saList.removeAll(removeList);
				size = saList.size();
				//将博文数据批量压入DCSS
				RemindServiceClient.getInstance().loadSimpleArticleList(saList.get(0).getUuid(), saList);
				if(j==0){
					UserEventService.getInstance().clearIMessageListWapper(nKey);
				}
//				if(j==1){
//					String nkey2 = StockUtil.getStockChanceListcKey(String.valueOf(uid));
//					UserEventService.getInstance().clearIMessageListWapper(nkey2);
//				}
				//历史数据先压入
				for(int i=(size-1);i>=0;i--){
					boolean isRecommend = false;
					SimpleArticle sa = saList.get(i);
					//新结构 这里只放uid-uuid的对应关系
					Map<String, Serializable> map = sa.get_attr();
					String tuid = String.valueOf(sa.getUid());
					String tuuid = String.valueOf(sa.getUuid());
					String time = String.valueOf(sa.getTime());
					if(map!=null && map.size()>0 && j!=0){
						if(map.get("tuid")!=null){
							tuid = String.valueOf(map.get("tuid"));
							isRecommend = true;
						}
						if(map.get("time")!=null){
							time = String.valueOf(map.get("time"));
						}
					}
					if(j == 0 || j == 2){ //好友或公司的博文，压入我的空间
						nKey =  StockUtil.getFavoriteListcKey(String.valueOf(uid));
					}else if(j == 1){
						if(isRecommend){ //话题的博文[推荐后的]，压入我的订阅【机会】
							nKey =  StockUtil.getStockChanceListcKey(String.valueOf(uid));
						}
//						else{
//							nKey =  StockUtil.getFavoriteListcKey(String.valueOf(uid));
//						}
					}
					UserMsg um = new UserMsg();
					um.setS(tuid);
					um.setD(tuid);
					um.setTime(Long.parseLong(time));
					um.putAttr("uuid",tuuid );
					UserEventService.getInstance().put2MessageListWapperBysize(nKey, um, 500);
				}
			}
		}		
		

	}

	/**
	 * 关注好友，加载相关博文
	 *
	 * @param uid
	 */
	public void addUserFavoriteWapper(Long uid,String fuids) {
		String nKey = StockUtil.getFavoriteListcKey(String.valueOf(uid));
		clearUserFavoriteWapper(uid,fuids,false);//先清理再添加
		String[] uidArr = fuids.split(",");
		List<TimeLine> timelineList = tls.getTimeLineListByTime(uidArr,
				SAVE_TABLE.ARTICLE, 0l, 0, 100);
		List<SimpleArticle> saList = getSimpleArticleList(timelineList);

		if(saList != null && saList.size() != 0){
			int size = saList.size();
			size = saList.size();
			//将博文数据批量压入DCSS
			RemindServiceClient.getInstance().loadSimpleArticleList(saList.get(0).getUuid(), saList);
			//UserEventService.getInstance().clearIMessageListWapper(nKey);
			//历史数据先压入
			for(int i=(size-1);i>=0;i--){
				SimpleArticle sa = saList.get(i);
				String tuid = String.valueOf(sa.getUid());
				String tuuid = String.valueOf(sa.getUuid());
				String time = String.valueOf(sa.getTime());
				UserMsg um = new UserMsg();
				um.setS(tuid);
				um.setD(tuid);
				um.setTime(Long.parseLong(time));
				um.putAttr("uuid",tuuid );
				UserEventService.getInstance().put2MessageListWapperBysize(nKey, um, 500);
			}
		}
	}
	/**
	 * 添加自选股/话题后，加载相关博文
	 * @param uid
	 * @param type 0公司 4话题
	 */
	public void addTopicArticleListWapper(Long uid,String fuids,int type) {
		String nKey = StockUtil.getStockChanceListcKey(String.valueOf(uid));
		clearUserStockChanceWapper(uid,fuids,false);
		if(type == StockConstants.SUBJECT_TYPE_0){
			clearUserFavoriteWapper(uid,fuids,false);//先清理再添加
		}
		String[] uidArr = fuids.split(",");
		List<TimeLine> timelineList = tls.getTimeLineListByTime(uidArr,
				SAVE_TABLE.ARTICLE, 0l, 0, 100);
		if(timelineList==null){
			return;
		}
		Set<String> tmpSet = new HashSet<String>();
		List<TimeLine> removeList = new ArrayList<TimeLine>();
		for(TimeLine tl : timelineList){
			if(tl==null){
				continue;
			}
			String uuid = tl.getUuid();
			if(!StringUtil.isEmpty(uuid)){
				if(tmpSet.contains(uuid)){
					removeList.add(tl);
				}else{
					tmpSet.add(uuid);
				}
			}
		}
		timelineList.removeAll(removeList);
		List<SimpleArticle> saList = getSimpleArticleList(timelineList);
		if(saList != null && saList.size() != 0){
			int size = saList.size();
			size = saList.size();
			//将博文数据批量压入DCSS
			RemindServiceClient.getInstance().loadSimpleArticleList(saList.get(0).getUuid(), saList);
			//UserEventService.getInstance().clearIMessageListWapper(nKey);
			//历史数据先压入
			for(int i=(size-1);i>=0;i--){
				boolean isRecommend = false;
				SimpleArticle sa = saList.get(i);
				Map<String, Serializable> map = sa.get_attr();
				String tuid = String.valueOf(sa.getUid());
				String tuuid = String.valueOf(sa.getUuid());
				String time = String.valueOf(sa.getTime());
				if(map!=null && map.size()>0){
					if(map.get("tuid")!=null){
						tuid = String.valueOf(map.get("tuid"));
						isRecommend = true;
					}
					if(map.get("time")!=null){
						time = String.valueOf(map.get("time"));
					}
				}
				if(type == StockConstants.SUBJECT_TYPE_0 && (!isRecommend)){
					 nKey = StockUtil.getFavoriteListcKey(String.valueOf(uid));
				}else{
					nKey = StockUtil.getStockChanceListcKey(String.valueOf(uid));
				}
				UserMsg um = new UserMsg();
				um.setS(tuid);
				um.setD(tuid);
				um.setTime(Long.parseLong(time));
				um.putAttr("uuid",tuuid );
				UserEventService.getInstance().put2MessageListWapperBysize(nKey, um, 500);
			}
		}
	}
	/**
	 * 取消关注好友或者自选股后，删除相关uid-uuid
	 *
	 * @param uid
	 */
	public void clearUserFavoriteWapper(Long uid,String fuids,boolean isInit) {
		String nKey = StockUtil.getFavoriteListcKey(String.valueOf(uid));
		String[] uidArr = fuids.split(",");
		List<TimeLine> timelineList = tls.getTimeLineListByTime(uidArr,
				SAVE_TABLE.ARTICLE, 0l, 0, 200);
		List<SimpleArticle> saList = getSimpleArticleList(timelineList);
		List<String> uuidList = Lists.newArrayList();
		if(saList!=null && saList.size()>0){
			for(SimpleArticle sa : saList){
				String uuid = sa.getUuid();
				if(StringUtils.isNotBlank(uuid)){
					uuidList.add(uuid);
				}
			}
		}
		UserEventService.getInstance().clearIMessageListWapper(nKey,uuidList,isInit);
	}

	/**
	 * 取消关注话题后，删除相关uid-uuid
	 *
	 * @param uid
	 */
	public void clearUserStockChanceWapper(Long uid,String fuids,boolean isInit) {
		String nKey = StockUtil.getStockChanceListcKey(String.valueOf(uid));
		String[] uidArr = fuids.split(",");
		List<TimeLine> timelineList = tls.getTimeLineListByTime(uidArr,
				SAVE_TABLE.ARTICLE, 0l, 0, 200);
		List<SimpleArticle> saList = getSimpleArticleList(timelineList);
		List<String> uuidList = Lists.newArrayList();
		if(saList!=null && saList.size()>0){
			for(SimpleArticle sa : saList){
				String uuid = sa.getUuid();
				if(StringUtils.isNotBlank(uuid)){
					uuidList.add(uuid);
				}
			}
		}
		UserEventService.getInstance().clearIMessageListWapper(nKey,uuidList,isInit);
	}

	public List<SimpleArticle> getNextUserFollowFavoriteListFromCache(long uid,
			long time, int type, int count) {
		String nkey = StockUtil.getFavoriteListcKey(String.valueOf(uid));
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		if(eq==null ||eq.getMessageList().size()==0)
		{
			//如果登陆后，还未加载上来，那就主动拉一次
			initUserFavoriteWapper(uid);
			//拉完之后，需要重新获取一下eq
			eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		}
		if(type==0){
			time = time+1;
		}else{
			time = time-1;
		}

		List<IMessage> list = Lists.newArrayList();
		List<String> uuidList = Lists.newArrayList();
		list = eq.getMessageList(type,time, count);
		if(list != null){
			for(IMessage m : list){
				UserMsg um = (UserMsg)m;
				if(um!=null){
					uuidList.add(String.valueOf(um.getAttr("uuid")));
				}
			}
		}
		List<SimpleArticle>  saList = null;
		if(uuidList.size()>0){
			saList = RemindServiceClient.getInstance().getSimpleArticleList(uuidList.get(0),uuidList);
		}
		return saList;
	}

	/*public List<SimpleArticle> getNextUserFollowStockChanceListFromCache(long uid,
			long time, int type, int count) {
		String nkey = StockUtil.getStockChanceListcKey(String.valueOf(uid));
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		if(eq==null ||eq.getMessageList().size()==0)
		{
			eq = new IMessageListWapper();
		}
		if(type==0){
			time = time+1;
		}else{
			time = time-1;
		}

		List<IMessage> list = Lists.newArrayList();
		List<String> uuidList = Lists.newArrayList();
		list = eq.getMessageList(type,time, count);
		if(list != null){
			for(IMessage m : list){
				UserMsg um = (UserMsg)m;
				if(um!=null){
					uuidList.add(String.valueOf(um.getAttr("uuid")));
				}
			}
		}
		List<SimpleArticle>  saList = null;
		if(uuidList.size()>0){
			saList = RemindServiceClient.getInstance().getSimpleArticleList(uuidList.get(0),uuidList);
		}
		return saList;
	}*/

	public List<SimpleArticle> getNextCompanyArticleListFromCache(String code,
			long time, int type, int limit) {
		String nkey = StockUtil.getUsubjectArticleKey(code);
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		List<SimpleArticle>  saList = null;
		if(eq!=null){
			List<String> uuidList = Lists.newArrayList();
			if(type==1){
				time= time-1;
			}else{
				time = time+1;
			}
			List<IMessage> list = eq.getMessageList(type,time, limit);
			if(list == null){
				list = Lists.newArrayList();
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
	public Map<String,Object> getNextRecommendArticleListFromCache(String code,
			long time, int type, int limit) {
		Map<String,Object> result = Maps.newHashMap();
		String nkey = StockUtil.getRecommendArticleKey(code);
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		List<SimpleArticle> saList = Lists.newArrayList();
		long firstRecommendTime = 0;//list中最小的推荐时间
		long lasetRecommendTime = System.currentTimeMillis();//list中最大的推荐时间
		if(eq!=null){
			List<IMessage> list = Lists.newArrayList();
			List<String> uuidList = Lists.newArrayList();
			Map<String,String> uuid_time = Maps.newHashMap();
			if(type==1){
				time= time-1;
			}else{
				time = time+1;
			}
			list = eq.getMessageList(type,time, limit);

			if(list!=null){
				int size = list.size()-1;
				if(size>=0){
					firstRecommendTime = ((UserMsg)list.get(size)).getTime();
					lasetRecommendTime = ((UserMsg)list.get(0)).getTime();
				}
				for(IMessage m : list){
					UserMsg um = (UserMsg)m;
					if(um!=null){
						String uuid = String.valueOf(um.getAttr("uuid"));
						String recommendTime =String.valueOf(um.getTime());
						uuidList.add(uuid);
						uuid_time.put(uuid, recommendTime);
					}
				}

				if(uuidList.size()>0){
					saList = RemindServiceClient.getInstance().getSimpleArticleList(uuidList.get(0),uuidList);
				}
			}
			//组装MAP 排序
		/*	if(saList!=null && saList.size()>0){
				for(SimpleArticle sa : saList){
					if(sa!=null){
						long recommendTime = Long.parseLong(uuid_time.get(sa.getUuid()));//博文推荐时间
						sa.setRecommendTime(recommendTime);
						time_simpleArticle.put(sa,uuid_time.get(sa.getUuid()));
					}
				}
				saList = CommonUtil.sortMapByVaule(time_simpleArticle);
			}*/
		}
		result.put("firstRecommendTime", firstRecommendTime);
		result.put("lasetRecommendTime", lasetRecommendTime);
		result.put("saList", saList);
		return result;
	}

	public List<SimpleArticle> getNextCompanyArticleListFromCacheByType(String code,
			long time, int type, int limit) {
		String nkey = StockUtil.getUsubjectArticleKey(code);
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		List<SimpleArticle>  saList = null;
		if(eq!=null){
			List<String> uuidList = Lists.newArrayList();
			List<IMessage> list = eq.getMessageList(type,time,limit);
			if(list == null){
				list = Lists.newArrayList();
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


/*	public List<SimpleArticle> getNextUsubjectFavoriteListFromCache(String usubjectid,
			long time, int start, int limit) {
		String nkey = StockUtil.getFavoriteListcKey(usubjectid);
		SimpleArticleListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		if(eq==null)
		{
			//如果登陆后，还未加载上来，那就主动拉一次
			initUsubjectFavoriteWapper(usubjectid);
		}
		return eq.getSimpleArticleList(time,start,limit);
	}*/

	/**
	 * 建立话题的时间轴
	 * 在原有代码中，没有看到公司空间页，摘要列表的构造方式，暂时参考用户的，需要测试
	 * SAVE_TABLE.TOPIC 存着公司关联的博文  建议SAVE_TABLE.ARTICLE以后放主持人设置的文章，目前是用TOPICRECOMMEND放置
	 * @param uid
	 */
	@SuppressWarnings("deprecation")
/*	public void initUsubjectFavoriteWapper(String usubjectid) {
		String sKey = usubjectid;
		List<TimeLine> timelineList = tls.getTimeLineListByTime(sKey, SAVE_TABLE.TOPIC,0l,
				0, 500);
		List<SimpleArticle> saList =  getSimpleArticleList(timelineList);
		for (SimpleArticle sa : saList) {
			String nkey = StockUtil.getFavoriteListcKey(usubjectid);
			//生成一个时间线放入缓存，如果有必要，可以把摘要内容也缓存起来
			UserEventService.getInstance().put2SimpleArticleWapper(nkey, sa, 500);
		}

	}*/


	public List<SimpleArticle> getSimpleArticleList(List<TimeLine> timelineList) {
		List<SimpleArticle> simpleArticleList = new ArrayList<SimpleArticle>();
		Set<String> columnsSet = ch.getColumnsSet(SimpleArticle.class);
		columnsSet.remove("serialVersionUID");
		columnsSet.remove("class");
		// columnsSet.remove("content");
		String[] columns = {};
		columns = columnsSet.toArray(columns);
		String[] keys = new String[timelineList.size()];
		for (int i = 0; i < timelineList.size(); i++) {
			keys[i] = timelineList.get(i).getUuid();
		}
		Map<String, Map<String, String>> batchMap = ch.get(
				SAVE_TABLE.ARTICLE.toString(), keys, columns);
		for (TimeLine timeline : timelineList) {
			String uuid = timeline.getUuid();
			Map<String, String> aMap = batchMap.get(uuid);
			SimpleArticle simpleArticle = new SimpleArticle();
			NosqlBeanUtil.map2Bean(simpleArticle, aMap);
			if (aMap.isEmpty() == false) {
				simpleArticleList.add(simpleArticle);
			}
		}
		return simpleArticleList;
	}

	public void initUserUnReadMessage(Long uid)
	{
		//要初始化的消息类型列表
		String ltypes = ConfigCenterFactory.getString("message.init_user_Unread_message_types", "2,3,4,5,7,13,14");
		for(String type:ltypes.split(","))
		{
			//先建一个空消息wapper
			String key = uid + "^" + type;
			if(String.valueOf(MsgConst.MSG_USER_TYPE_7).equals(type)){
				MyTalkMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
				if(eq==null)
				{
					eq = new MyTalkMessageListWapper();
					eq.setKey(key);
					LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
				}else{
					//每次登录，都清空时间轴的数据 TODO 如果不清空的话，需要加载未读数据，注意不要重复了
					eq.clear();
				}
				//加载nosql中关于这个消息的前500条数据
				List<TalkMessage> uml = NosqlService.getInstance().getTalkMessageList(String.valueOf(uid), type, 0, eq.getMaxsize());//.loadUserNoReadMessage(String.valueOf(uid), type);
				if(uml!=null)
				{
					eq.setKey(String.valueOf(uid));
					for(int i=uml.size()-1; i>=0; i--)//为了和handle7 存入顺序一致  这里需要逆序遍历
					{
						String s = uml.get(i).getS();
						String d = uml.get(i).getD();
						if(StringUtils.isNumericSpace(s) && StringUtils.isNumericSpace(d)){
							eq.put(uml.get(i));
						}
					}
					List<TalkMessage> saList = eq.getMessageList();
					if(saList!=null && saList.size()>0){
						String uidStr = String.valueOf(uid);
						for(TalkMessage tm : saList){
							String fuidStr = uidStr.equals(tm.getS())?tm.getD():tm.getS();
							if(StringUtils.isBlank(fuidStr) || StringUtils.isNumericSpace(fuidStr) == false || "null".equals(fuidStr)){
								logger.error("数据异常点type="+type+",uuid="+tm.getUuid()+",d="+tm.getD()+",s="+tm.getS());
								continue;
							}
							String key2 = uid+"^"+fuidStr;
							IMessageListWapper eq2 = LCEnter.getInstance().get(key2, StockUtil.getEventCacheName(key2));
							if(eq2==null)
							{
								eq2 = new IMessageListWapper();
								LCEnter.getInstance().put(key2,eq2, StockUtil.getEventCacheName(key2));
							}else{
								//每次登录，都清空时间轴的数据 TODO 如果不清空的话，需要加载未读数据，注意不要重复了
								eq2.clear();
							}
							int max = eq2.getMaxsize();
							Long uid2 = Long.parseLong(fuidStr);
							List<IMessage> uml2 = NosqlService.getInstance().getUserHistoryTalkMessageList(uid, uid2, 0, max,TalkMessage.class);
							if(uml2!=null){
									int size = 0;
									for(int j=0;j<uml2.size();j++)
									{
										 eq2.put(uml2.get(j));
										 UserMsg um = (UserMsg)(uml2.get(j));
										 um.getTime();
										 if(um!=null && um.getStatus()==0 && fuidStr.equals(um.getS())){
											 size++;
										 }
									}
								 	tm.setUnreadCount(size);//得到未读消息数
								 	eq2.setUnReadCount(size);
							}
						}
					}
				}
			}else{
				IMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
				if(eq==null)
				{
					eq = new IMessageListWapper();
					LCEnter.getInstance().put(key,eq, StockUtil.getEventCacheName(key));
				}else{
					//每次登录，都清空时间轴的数据 TODO 如果不清空的话，需要加载未读数据，注意不要重复了
					eq.clear();
				}
				//加载nosql中关于这个消息的前500条数据
				List<UserMsg> uml = NosqlService.getInstance().getUserMessageList(String.valueOf(uid), type, 0, eq.getMaxsize());//.loadUserNoReadMessage(String.valueOf(uid), type);
				if(String.valueOf(MsgConst.MSG_USER_TYPE_5).equals(type)){
					 uml = NosqlService.getInstance().getUserMessageListForType5(String.valueOf(uid), type, 0, eq.getMaxsize());
				}
				if(uml!=null)
				{
					int size = 0;
					for(UserMsg um:uml)
					{
						 if(um!=null && um.getStatus()==0 && um.getD()!=null && um.getS()!=null){
							 size++;
						 }
						 if(um!=null){
							 String s = um.getS();
							 String d = um.getD();
							 if(StringUtils.isNumericSpace(s) && StringUtils.isNumericSpace(d)){
								 eq.put(um);
							 }
						 }
						 eq.setUnReadCount(size);//未读消息数
					}
				}
			}
		}
		//标记为已读
		readMessageWithTime(uid, String.valueOf(MsgConst.MSG_USER_TYPE_7));
	}
	public List<Article> getArticleListByTime(long uid,long startTime,long endTime,int num){
		List<TimeLine> timelineList = tls.getTimeLineListByTime(String.valueOf(uid),
				SAVE_TABLE.ARTICLE, startTime, endTime,num);
		List<Article> artList = new ArrayList<Article>();
		List<String> uuidList = new ArrayList<String>();
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}
		String[] uuidStrList = new String[uuidList.size()];
		uuidList.toArray(uuidStrList);
		if (uuidStrList.length != 0) {
			artList = getArticleList(uuidList);
		}
		return artList;
	}
	/**
	 *
	 * @param code
	 * @param startTime  开始时间
	 * @param type 约定的标示 0：拉取startTime开始的最新记录；1：拉取startTime开始的历史记录；否则 拉取当前时间开始的历史记录
	 * @param limit 拉取记录条数
	 * @return
	 */
	public Map<String,Object> getArticleListByTime(String code,long startTime,int type,int limit,SAVE_TABLE table){
		Long endTime =System.currentTimeMillis();
		if(type==0){

		}else if(type==1){
			endTime = startTime;
			startTime = 0L;
		}else{
			startTime = 0L;
		}
		if(startTime>endTime){
			logger.error("getArticleListByTime------->开始时间大于结束时间");
			startTime = endTime;
		}
		List<TimeLine> timelineList = tls.getTimeLineListByTime(code,table,startTime,endTime,limit);
		List<Article> artList = new ArrayList<Article>();
		List<String> uuidList = new ArrayList<String>();
		String startTime1 = "";
		String endTime1 = "";
		if(timelineList!=null && timelineList.size()>0){
			int size =  timelineList.size()-1;
			startTime1 = timelineList.get(0).getTimeMillis();
			endTime1 = timelineList.get(size).getTimeMillis();
		}
		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}

		String[] uuidStrList = new String[uuidList.size()];
		uuidList.toArray(uuidStrList);

		if (uuidStrList.length != 0) {
			artList = getArticleList(uuidList);
		}
		Map<String,Object> resultMap = new HashMap<String,Object>();
		resultMap.put("artList", artList);
		resultMap.put("startTime", startTime1);
		resultMap.put("endTime", endTime1);
		return resultMap;
	}
	/**
	 *
	 * @param code
	 * @param startTime  开始时间
	 * @param type 约定的标示 1 查询历史记录  0 查询最新记录
	 * @param limit 拉取记录条数
	 * @return
	 */
	public Map<String,Object> getArticleListByTimeByType(String code,long startTime,int type,int limit,SAVE_TABLE table){
		List<SimpleArticle> artList2 = RemindServiceClient.getInstance().getNextUsubjectFollowFavoriteListByType(code, startTime, type, limit);
		Map<String,Object> resultMap = new HashMap<String,Object>();
		resultMap.put("artList", artList2);
		return resultMap;
	}

	//根据时间获得某用户自己和好友的文章列表
	public  Map<String,Object> getFollowUserArticleList(long uid,long startTime,int type,int num){
		String[] uidArr = {};
		Long endTime =System.currentTimeMillis();
		if(type==0){

		}else if(type==1){
			endTime = startTime;
			startTime = 0L;
		}else{
			startTime = 0L;
		}
		if(startTime>endTime){
			logger.error("getFollowUserArticleList------>开始时间大于结束时间");
			startTime = endTime;
		}
		if (uid == 0l) {
			// activesUsers:10085,10086,20001,20002,20004,20005,20006,20009,20010
			String recommendUsers = ConfigCenterFactory.getString(
					"stock_zjs.recommend_users", "");
			if (StringUtil.isEmpty(recommendUsers) == false
					&& recommendUsers.split(":").length >= 2) {
				uidArr = recommendUsers.split(":")[1].split(",");
			}
		} else {
			List<String> uidList = new ArrayList<String>();
			uidList.add(String.valueOf(uid));
			List<ShortUser> follows = getFollowList(uid);
			// 增加好友
			if (follows != null) {
				for (ShortUser follow : follows) {
					uidList.add(String.valueOf(follow.getUid()));
				}
			}
			uidArr = uidList.toArray(uidArr);
		}
		String startTime1 = "";
		String endTime1 = "";
		List<TimeLine> timelineList = tls.getTimeLineListByTime(uidArr,SAVE_TABLE.ARTICLE,startTime,endTime, num);
		ComparatorTimeLine comparatorarticle= new ComparatorTimeLine();
		Collections.sort(timelineList,comparatorarticle);

		if(timelineList.size()>num){
			timelineList = timelineList.subList(0, num);
		}
		if(timelineList!=null && timelineList.size()>0){
			int size =  timelineList.size()-1;
			startTime1 = timelineList.get(0).getTimeMillis();
			endTime1 = timelineList.get(size).getTimeMillis();
		}
		List<String> uuidList = new ArrayList<String>();

		for (TimeLine tl : timelineList) {
			uuidList.add(tl.getUuid());
		}
		List<Article> artList = getArticleList(uuidList);
		if(artList==null){
			artList=new ArrayList<Article>();
		}
		Map<String,Object> resultMap = new HashMap<String,Object>();
		resultMap.put("artList", artList);
		resultMap.put("startTime", startTime1);
		resultMap.put("endTime", endTime1);
		return resultMap;
	}
	public List<Favorite> getFavoriteArticleListByTime(long uid,long startTime,long endTime,int num){
		String sKey = String.valueOf(uid);
		List<TimeLine> timeLineList = tls.getTimeLineListByTime(sKey, SAVE_TABLE.FAVORITE,startTime,endTime, num);
		List<Map<String, String>> getMapList = CassandraHectorGateWay.getInstance().getSuper(
				SAVE_TABLE.FAVORITE.toString(), sKey,timelineListToStringList(timeLineList));
		List<Favorite> favoriteList = new ArrayList<Favorite>(getMapList.size());
		for(TimeLine t1:timeLineList){
			for(Map<String,String> map:getMapList){
				if(t1.getUuid().equals(String.valueOf(map.get("uuid")))){
					Favorite favorite=new Favorite();
					NosqlBeanUtil.map2Bean(favorite, map);
					favoriteList.add(favorite);
				}
			}
		}
//		for (Map<String, String> getMap : getMapList) {
//			Favorite favorite = new Favorite();
//			NosqlBeanUtil.map2Bean(favorite, getMap);
//
//			favoriteList.add(favorite);
//		}
		Set<String> uuidSet = new HashSet<String>();
		for (Favorite f : favoriteList) {
			if (StringUtil.isEmpty(f.getUuid()) == false) {
				uuidSet.add(f.getUuid());
			}
		}
		List<String> list3 = new ArrayList<String>(uuidSet);
		List<Article> articleList = getArticleList(list3);
		for (Favorite f : favoriteList) {
			for (Article article : articleList) {
				if (f.getUuid().equals(article.getUuid())) {
					f.setArticle(article);
					break;
				}
			}
		}
		return favoriteList;
	}

	//两个uid之间的关注关系
	public int relationship(long myuid,long fuid){
		int fts = 0;//没关系：0；我关注对方：1对方关注我：2；互相关注：3 ；自己:4
		if(myuid>0&&fuid>0){
			if(myuid == fuid){
				fts = 4;
			}
			boolean follow = MicorBlogService.getInstance().checkFollow(myuid,fuid);
			boolean befollow = MicorBlogService.getInstance().checkFollow(fuid,myuid);
			if(follow && befollow){
				fts = 3;
			}else if(follow){
				fts = 1;
			}else if(befollow){
				fts = 2;
			}
		}
		return fts;
	}

	/**
	 * http://zhanzhang.baidu.com/tools/ping
	 */
	private void sendPingToBaidu(Long uid, String uuid) {
		int sendPingToBaidu = ConfigCenterFactory.getInt("stock_zjs.sendPingToBaidu", 0);
		if(sendPingToBaidu == 0) {
			return ;
		}
	    try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL("http://ping.baidu.com/ping/RPC2"));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			/**
			    博客名称
				博客首页地址
				新发文章地址
				博客rss地址
			 */
			Object[] params = new Object[]{"", "", "http://www.igushuo.com/articledetail?uid=" + uid + "&uuid=" + uuid + "&end", ""};
			Integer result = (Integer) client.execute("weblogUpdates.extendedPing", params);
			logger.info("sendPingToBaidu " + result);
		} catch (Exception e) {
			logger.error(e.toString());
		}
	}

	public void initMessageCenter(long uid) {
		//初始化用户消息列表
		try {
			//初始化公司消息
			StringBuilder sb = new StringBuilder();
			String companyCodeStr = ConfigCenterFactory.getString("stock_zjs.recommend_stocks","000002.sz");
			String[] arr = companyCodeStr.split(",");
			int recommendCount = ConfigCenterFactory.getInt("stock_zjs.recommend_stocks_count", 30);
			List<String> recommendStockList = ChanceCategoryService.getInstance().getRecommendStockByChanceCategoryTags(recommendCount);
			if(recommendStockList==null){
				recommendStockList = Lists.newArrayList();
			}
			if(recommendStockList.size()<6){
				for(String code : arr){
					recommendStockList.add(code);
				}
			}
			if(recommendStockList.size()>6){
				recommendStockList = recommendStockList.subList(0, 5);
			}
			addStock(recommendStockList,uid);//添加自选股
			for(String code : recommendStockList){
				sb.append(code);
				sb.append(",");
			}
			initSelfSelectStockMsg(uid,sb.toString());
			loginFromDcss(uid,true);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}
	/*
	 * 用户注册流程 （PC端 初始化自选股消息）
	 */
	public void initSelfSelectStockMsg(long uid,String recommendStocks) {
		//初始化用户消息列表
		try {
			if(StringUtils.isEmpty(recommendStocks)){
				return;
			}
			//初始化公司消息
			String uidStr = String.valueOf(uid);
			long muid = ConfigCenterFactory.getLong("stock_zjs.message_uid", 10001L);
			String[] codes = recommendStocks.split(",");
			for(String code : codes){
				if(StringUtils.isEmpty(code)){
					continue;
				}
				USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(code);
				if(us==null || us.getType()!=0){
					logger.info("初始化公司消息错误 没有相关公司！ "+code);
					continue;
				}
				long fuid = us.getUid();
				String fuidStr = String.valueOf(fuid);
				String muidStr = String.valueOf(muid);
				List<TalkMessage> mlist = NosqlService.getInstance().getTalkMessageListByUid(muidStr,String.valueOf(fuid),0,10);
				if(mlist == null || mlist.size() ==0 ){
					logger.info("初始化公司消息错误 没有相关消息！ "+code);
					continue;
				}
				for(TalkMessage t : mlist){
					UserMsg body = (UserMsg)t;
					String MsgKey = UUID.randomUUID().toString();
					body.setUuid(MsgKey);
					body.setD(uidStr);
					body.setS(fuidStr);
					body.setStatus(1);
					NosqlService.getInstance().saveTalkMsg(uidStr,fuidStr,body);
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	private void addStock(List<String> stockCodes,long uid){
		int needInUserStockTable = ConfigCenterFactory.getInt("stock_zjs.NeedInUserStockTable", 1);
		try {
			if(stockCodes != null && stockCodes.size()>0) {
				UserStockService usService = UserStockService.getInstance();

				Set<String> set = new HashSet<String>();
				for(int i=0;i<stockCodes.size(); i++) {
					String stockcode = stockCodes.get(i);
					if(StringUtils.isNotBlank(stockcode)) {
						boolean isInDb = usService.getSelect(uid, stockcode) == null ? false : true;
						boolean result = true;
						if(needInUserStockTable == 1 && ! isInDb) {
							UserStock us = new UserStock();
							us.setUid(uid);
							us.setCompanycode(stockcode);
							us.setAddTime(new Date());
							result = usService.insert(us) > 0;
						}

						if(result) {
							set.add(stockcode);
						}
					}
				}//end

				Date updateTime = new Date();
				String seq = StringUtils.join(set, ",");
				if(StockSeqService.getInstance().addStockSeq(uid, seq, updateTime)) {
					for(String stockcode : set) {
						MobileMsgPushService.getInstance().saveSubscribeUnSubscribeMsg(uid, stockcode, 1, 1);
					}
					StockSeq tempSq = new StockSeq();
					tempSq.setCodesSeq(seq);
					tempSq.setUid(uid);
					tempSq.setUpdateTime(updateTime);
					UserServiceClient.getInstance().addStockSeq(uid, tempSq);
				}
			}
		} catch (Exception e) {
			logger.info("初始化添加自选股错误" + e);
		}
	}

	/**
	 * @param uid
	 * @param isLogin 是否强制登陆，true=强制刷新DCSS的用户缓存【用于登录页登录或关注好友】，flase=不强制刷新用户缓存，按超时时间来刷新比如1天刷新一次等等【zjs会话过期后cookie登录】
	 */
	public void loginFromDcss(long uid,boolean isLogin){
		String uidStr = String.valueOf(uid);
		UserMsg um = SMsgFactory
				.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_8);
		um.setS(uidStr);
		um.setD(uidStr);
		List<String> ipList = StockUtil.getLocalServerIpList();
		String ip = ipList.size() > 0 ? ipList.get(0) : "";
		String ipport = ip+ ":" + BaseConfiguration.getYasUdpPort();
		um.putAttr("ipport", ipport);
		um.putAttr("userCurZjs: ", BaseConfiguration.getAppId());
		if( isLogin == true ){
			um.putAttr("isLogin", "true");
		}else{
			um.putAttr("isLogin", "false");
		}
		
		UserEventService.getInstance().notifyTheEvent(um);
	}

	//给推荐博文附加信息
	public void recommendArticle(List<Article> al,long uid){
		if(al!=null && uid>0){//是否关注了话题
			for(Article sa : al){
				Map<String, Serializable> attr = sa.get_attr();
				if(attr!=null && attr.size()>0){
					String text = sa.getAttr("text");
					if(StringUtils.isEmpty(text)){
						continue;
					}
					text = text.split(":")[0];
					int topicType = StockConstants.SUBJECT_TYPE_0;
					boolean isFocus = false;
					USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(text.split(":")[0]);
					List<String> focusList = null;
					if(us!=null && us.getType()==0){
						focusList = StockSeqService.getInstance().getUserStockSeqList(uid);
					}else{
						focusList = RemindServiceClient.getInstance().getUserTopicRelationship(String.valueOf(uid));
						topicType = StockConstants.SUBJECT_TYPE_4;
					}
					if(focusList!=null && focusList.contains(text)){
						isFocus = true;
					}
					if(!isFocus){
						attr.put("time", null);
					}
					sa.putAttr("focus", isFocus);
					sa.putAttr("topicType", topicType);
				}
			}
		}
	}
	public void recommendArticle2(List<SimpleArticle> al,long uid){
		if(al!=null && uid>0){//是否关注了话题
			for(SimpleArticle sa : al){
				Map<String, Serializable> attr = sa.get_attr();
				if(attr!=null && attr.size()>0){
					String text = sa.getAttr("text");
					if(StringUtils.isEmpty(text)){
						continue;
					}
					text = text.split(":")[0];
					int topicType = StockConstants.SUBJECT_TYPE_0;
					boolean isFocus = false;
					USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(text.split(":")[0]);
					List<String> focusList = null;
					if(us!=null && us.getType()==0){
						focusList = StockSeqService.getInstance().getUserStockSeqList(uid);
					}else{
						focusList = RemindServiceClient.getInstance().getUserTopicRelationship(String.valueOf(uid));
						topicType = StockConstants.SUBJECT_TYPE_4;
					}
					if(focusList!=null && focusList.contains(text)){
						isFocus = true;
					}
					sa.putAttr("focus", isFocus);
					sa.putAttr("topicType", topicType);
					if(!isFocus){
						attr.put("time", null);
					}
				}
			}
		}
	}

	//股友圈 博文请求时间
	public String getRequestTime(SimpleArticle simpleArticle,long uid){
		String time=String.valueOf(simpleArticle.getTime());
		Map<String, Serializable> attr =simpleArticle.get_attr();
		if(attr!=null && attr.size()>0){
			String t =String.valueOf(attr.get("time"));
			String text = String.valueOf(attr.get("text"));
			if(StringUtils.isEmpty(text)){
				return time;
			}
			String identify = text.split(":")[0];
			USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(identify);
			List<String> focusList = null;
			if(us!=null && us.getType()==0){
				focusList = StockSeqService.getInstance().getUserStockSeqList(uid);
			}else{
				focusList = RemindServiceClient.getInstance().getUserTopicRelationship(String.valueOf(uid));
			}
			if(StringUtils.isNumericSpace(t) && focusList!=null && focusList.contains(identify)){
				time = t;
			}
		}
		return time;
	}

	/**
	 *  机会博文和评论
	 * @param uid
	 * @param time
	 * @param type
	 * @param limit 拉取博文数
	 * @param climit 拉取评论数
	 * @return
	 */
	public List<Map<String,Object>> getNextUserFollowStockChanceList(
			long uid, long time, int type, int limit,int climit) {
		List<Map<String,Object>> rList = Lists.newArrayList();
		Map<String, Map<String, Object>> result = Maps.newHashMap();
		String nkey = StockUtil.getStockChanceListcKey(String.valueOf(uid));
		IMessageListWapper eq = LCEnter.getInstance().get(nkey, StockUtil.getEventCacheName(nkey));
		if(eq==null)
		{
			eq = new IMessageListWapper();
		}
		if(type==0){
			time = time+1;
		}else{
			time = time-1;
		}
		List<String> uuidList = Lists.newArrayList();
		List<IMessage> list = eq.getMessageList(type,time, limit);
		if(list==null){
			list = Lists.newArrayList();
		}
		for(IMessage m : list){
			UserMsg um = (UserMsg)m;
			if(um!=null){
				uuidList.add(String.valueOf(um.getAttr("uuid")));
			}
		}
		if(uuidList.size()>0){
			 result = RemindServiceClient.getInstance().getSimpleArticleAndCommentsMapReduce(uuidList.get(0), uuidList, false, climit);
		}

		/**
		 * 按uuid的顺序排队
		 */
		for(int i=0;i<uuidList.size();i++){
			String saUuid = uuidList.get(i);
			Map<String,Object> r = result.get(saUuid);
			if(r!=null)
				rList.add(r);
		}
		return rList;
	}

	public boolean readMessageWithTime(Long uid, String type) {
		final long ONE_WEEK = 7*24*60*60*1000;
		long currentTime = System.currentTimeMillis();
		long interval = ConfigCenterFactory.getLong("dcss.clear_message_interval", ONE_WEEK);
		String key = uid+"^"+type;
		try {
			if(String.valueOf(MsgConst.MSG_USER_TYPE_7).equals(type)){//私信消息用的MyTalkMessageListWapper不一样
				MyTalkMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
				if(eq!=null)
				{
					List<TalkMessage> uml = new ArrayList<TalkMessage>();
					for(IMessage m : eq.getMessageList()){
						TalkMessage um = (TalkMessage)m;
						um.setStatus(1);
						uml.add(um);
						um.setUnreadCount(0);
						//把每一条记录都置为已读
						String key2 = um.getD()+"^"+um.getS();
						IMessageListWapper eq2 = LCEnter.getInstance().get(key2, StockUtil.getEventCacheName(key2));
						if(eq2!=null)
						{
							List<TalkMessage> tml = new ArrayList<TalkMessage>();
							for(IMessage m2 : eq2.getMessageList()){
								TalkMessage tm = (TalkMessage)m2;
								long time = tm.getTime();
								Object t = um.getAttr("t");
								int msgType = 0;
								if(t!=null){
									msgType = Integer.parseInt(t.toString());
								}
								if(currentTime-time>interval && msgType!=1){
									um.setStatus(1);
									uml.add(um);
								}
							}
							eq2.setUnReadCount(0);
							if(tml.size() >0){
								NosqlService.getInstance().updateTalkMessageList(String.valueOf(um.getS()), String.valueOf(MsgConst.MSG_USER_TYPE_7), tml);
							}
						}
					}
					NosqlService.getInstance().updateTalkMessageList(String.valueOf(uid), String.valueOf(type), uml);
				}
			}else{
				IMessageListWapper eq = LCEnter.getInstance().get(key, StockUtil.getEventCacheName(key));
				if(eq!=null)
				{
					List<UserMsg> uml = new ArrayList<UserMsg>();
					for(IMessage m : eq.getMessageList()){
						UserMsg um = (UserMsg)m;
						long time = um.getTime();
						if(currentTime-time>interval){
							um.setStatus(1);
							uml.add(um);
						}
					}
					NosqlService.getInstance().updateUserMessageList(String.valueOf(uid), String.valueOf(type), eq.getMaxsize());
					eq.setUnReadCount(0);
				}
			}
		} catch (Exception e) {
			logger.info("readMessageWithTime" +e);
			return false;
		}
		return true;
	}

	public Map<String, String> getConclusionTagsTipMap() {
		return conclusionTagsTipMap;
	}
	public Map<String, String> getPcConclusionTagsTipMap() {
		return pcconclusionTagsTipMap;
	}
	public List<String> getPcConclusionTags() {
		return pcconclusionTags;
	}
	public List<String> getConclusionTags() {
		return conclusionTags;
	}

	private static void initDiagnosisComapnyTag() {
		initMapTagList(ConfigCenterFactory.getString("stock_zjs_finance_tag.grouth_tag", "1631:1,1632:0,766:1,1019:1,767:0,1041:0,1080:1,1094:1,557:1,1081:0,1159:0,558:0,1088:1,1190:1,1086:1,1089:0,1090:0,1087:0,1115:1,1120:0,1118:0,1243:1,1247:0"), grouthTag);
		initMapTagList(ConfigCenterFactory.getString("stock_zjs_finance_tag.character_tag", "1212:1,266:1,265:1,1215:0,347:0,372:0,295:1,312:0,1110:1,1111:1,1104:1,1105:1,1108:0,1109:0,1107:1,250:1,322:0,350:0,1154:0,812:1,365:0"), characterTag);
		initMapTagList(ConfigCenterFactory.getString("stock_zjs_finance_tag.import_notice_tag", "1174:1,1173:0,363:0,291:1,1125:0,1144:1,277:1,353:0,264:1,808:0,809:0,379:0,378:0,381:0,380:0,377:0,371:0,316:0,1136:0,1134:0,1128:0,780:0,1127:1,461:1,1232:0,463:0,1129:0"), importNoticeTag);
		initListTag(ConfigCenterFactory.getString("stock_zjs_finance_tag.finance_tip_tag", "1631,766,1019,1080,1094,557,1088,1190,1086,1115,1243,1212,266,265,295,1110,1111,1104,1105,1107,250,812,1174,291,1144,277,264,1127,461"), pcCreportFinanceTipTag);
		initListTag(ConfigCenterFactory.getString("stock_zjs_finance_tag.finance_caution_tag", "1632,767,1041,1081,1159,558,1089,1090,1087,1120,1118,1247,1215,347,372,312,1108,1109,322,350,1154,365,1173,363,1125,353,808,809,379,378,381,380,377,371,316,1136,1134,1128,780,1232,463,1129"), pcCreportFinanceCautionTag);

		initConclusionTags(ConfigCenterFactory.getString("stock_zjs_finance_tag.pc_finance_conclusion_tags", "cf_1620,cf_1634,cf_1639,cf_1635,cf_1630,cf_1636,cf_1624,cf_1637,cf_1633,cf_1646,cf_1648,cf_1647,cf_1640,cf_1641,cf_1642,cf_1629,cf_1623,cf_1645,cf_1621,cf_1643,cf_1644,cf_1625"), pcconclusionTags);
		initConclusionTagsTipMap(ConfigCenterFactory.getString("stock_zjs_finance_tag.pc_finance_conclusion_tags_tip", "cf_1620@公司经营现处困境，#cf_1634@收入及盈利都出现增速放缓迹象，预示未来公司发展可能趋缓，要注意是否会出现经营见顶信号，如果一旦确认见顶，通常会出现双杀下挫行情，#cf_1639@利润增速明显低于收入增速，要警惕企业经营拐点是否会提前到来，#cf_1635@收入已经出现见顶信号，#cf_1630@收入的持续下降，严重影响市场对公司未来的经营预期，#cf_1636@盈利已经见顶回落，#cf_1624@盈利的持续、大幅下降，更表明未来盈利前景不容乐观，#cf_1637@利润大幅领先收入下跌，不是好现象，#cf_1633@收入及盈利都出现加速下行不良态势，#cf_1646@公司毛利率远高于行业平均，投资者需要了解在目前激烈的市场竞争条件下为何企业还能有这么高的毛利率，#cf_1648@毛利率出现见顶回落可能是竞争优势受到挑战，或是市场经营环境发生逆转，要认真对待，#cf_1647@不要小看毛利率大幅回落，这可能是盈利能力出现下滑的重要信号，抑或是公司的促销政策发生重大变化，即通过降低毛利率来扩大市场占有，#cf_1640@非经常性损益占比这么高，会对公司盈利的持续性带来隐忧，#cf_1641@虽然财务报表上的净利润是亏损的，但扣除非经常损失后公司实际是盈利的，这不一定是个坏消息，#cf_1642@虽然财务报表上的净利润是盈利的，但扣除非经常收益后实际却是亏损的，需要去了解公司为何如此做账的背后动机，#cf_1629@现金回款重视不够，加重经营风险，#cf_1623@高企的债务，削弱资产偿付能力，而银行放贷也会偏向审慎，#cf_1645@公司出现短债长投不良现象，要高度警惕是否已经出现债务危机，#cf_1621@扩张过速，导致短期偿债面临危机，#cf_1643@公司给股东的派息历来很小气，#cf_1644@三年了，作为股东，派息毛都没有，#cf_1625@财报数据变动异常，投资者需分辨公司帐务是否存在问题，炒股须防雷，此需小心，而庄股通常也具此特征，需要明辨，切忌投机品种做成投资。"), pcconclusionTagsTipMap);

		initConclusionTags(ConfigCenterFactory.getString("stock_zjs_finance_tag.finance_conclusion_tags", "cf_1620,cf_1634,cf_1639,cf_1635,cf_1630,cf_1636,cf_1624,cf_1637,cf_1633,cf_1646,cf_1648,cf_1647,cf_1640,cf_1641,cf_1642,cf_1629,cf_1623,cf_1645,cf_1621,cf_1643,cf_1644,cf_1625"), conclusionTags);
		initConclusionTagsTipMap(ConfigCenterFactory.getString("stock_zjs_finance_tag.finance_conclusion_tags_tip", "cf_1620@公司经营现处困境，#cf_1634@收入及盈利都出现增速放缓迹象，预示未来公司发展可能趋缓，要注意是否会出现经营见顶信号，如果一旦确认见顶，通常会出现双杀下挫行情，#cf_1639@利润增速明显低于收入增速，要警惕企业经营拐点是否会提前到来，#cf_1635@收入已经出现见顶信号，#cf_1630@收入的持续下降，严重影响市场对公司未来的经营预期，#cf_1636@盈利已经见顶回落，#cf_1624@盈利的持续、大幅下降，更表明未来盈利前景不容乐观，#cf_1637@利润大幅领先收入下跌，不是好现象，#cf_1633@收入及盈利都出现加速下行不良态势，#cf_1646@公司毛利率远高于行业平均，投资者需要了解在目前激烈的市场竞争条件下为何企业还能有这么高的毛利率，#cf_1648@毛利率出现见顶回落可能是竞争优势受到挑战，或是市场经营环境发生逆转，要认真对待，#cf_1647@不要小看毛利率大幅回落，这可能是盈利能力出现下滑的重要信号，抑或是公司的促销政策发生重大变化，即通过降低毛利率来扩大市场占有，#cf_1640@非经常性损益占比这么高，会对公司盈利的持续性带来隐忧，#cf_1641@虽然财务报表上的净利润是亏损的，但扣除非经常损失后公司实际是盈利的，这不一定是个坏消息，#cf_1642@虽然财务报表上的净利润是盈利的，但扣除非经常收益后实际却是亏损的，需要去了解公司为何如此做账的背后动机，#cf_1629@现金回款重视不够，加重经营风险，#cf_1623@高企的债务，削弱资产偿付能力，而银行放贷也会偏向审慎，#cf_1645@公司出现短债长投不良现象，要高度警惕是否已经出现债务危机，#cf_1621@扩张过速，导致短期偿债面临危机，#cf_1643@公司给股东的派息历来很小气，#cf_1644@三年了，作为股东，派息毛都没有，#cf_1625@财报数据变动异常，投资者需分辨公司帐务是否存在问题，炒股须防雷，此需小心，而庄股通常也具此特征，需要明辨，切忌投机品种做成投资。"), conclusionTagsTipMap);
	}

	private static void initConclusionTags(String finance_conclusion_tags, List<String> conclusionTags) {
		if(StringUtils.isNotBlank(finance_conclusion_tags)) {
			 conclusionTags.clear();
			 for(String tags : finance_conclusion_tags.split("\\|")) {
				 conclusionTags.add(tags);
			 }
		}
	}

	private static void initConclusionTagsTipMap(String finance_conclusion_tags_tip, Map<String, String> conclusionTagsTipMap) {
		if(StringUtils.isNotBlank(finance_conclusion_tags_tip)) {
			conclusionTagsTipMap.clear();
			for(String tags_tip : finance_conclusion_tags_tip.split("#")) {
				String[] arr = tags_tip.split("@");
				if(arr.length >= 2) {
					conclusionTagsTipMap.put(arr[0], arr[1]);
				}
			}
		}
	}

	private static void initMapTagList(String tags, List<Map<String, String>> list) {
		if(StringUtils.isNotBlank(tags)) {
			list.clear();
			for(String tag: tags.split(",")) {
				String[] arr = tag.split(":");
				Map<String, String> item = new HashMap<String, String>();
				item.put("key", "cf_" + arr[0]);
				item.put("t", arr[1]);
				list.add(item);
			}
		}
	}

	private static void initListTag(String tags, List<String> list) {
		if(StringUtils.isNotBlank(tags)) {
			list.clear();
			for(String tag : tags.split(",")) {
				list.add("cf_" + tag);
			}
		}
	}

	public List<String> getFinanceTipTag() {
		return pcCreportFinanceTipTag;
	}

	public List<String> getFinanceCautionTag() {
		return pcCreportFinanceCautionTag;
	}

	public List<Map<String, String>> getGrouthTag() {
		return grouthTag;
	}

	public List<Map<String, String>> getCharacterTag() {
		return characterTag;
	}

	public List<Map<String, String>> getImportNoticeTag() {
		return importNoticeTag;
	}

}
