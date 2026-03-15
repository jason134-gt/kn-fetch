package com.yfzx.service.trade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.BFConst;
import com.stock.common.bloomfilter.BFUtil;
import com.stock.common.constants.ShareConst;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.model.chance.StockChanceEntity;
import com.stock.common.model.company.Stock0001;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.share.UserExt;
import com.stock.common.model.share.Viewpoint;
import com.stock.common.model.snn.EConst;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.RealtimeComputeDataUpdateMsg;
import com.stock.common.msg.StockPriceBatchMsg;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.chance.ChanceCategoryService;
import com.yfzx.service.client.DcssTradeIndexServiceClient;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.RealTimeService;
import com.yfzx.service.db.Stock0001Service;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.msg.TalkMessageService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yfzx.service.share.ViewpointService;
import com.yz.common.vo.Pair;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.message.IMessage;

/**
 * 交易服务类
 * 
 * @author：杨真
 * @date：2014-7-25
 */
public class TradeService {
	static Logger log = LoggerFactory.getLogger(TradeService.class);

	public static TradeService instance = new TradeService();

	private TradeService() {
	}

	public static TradeService getInstance() {
		return instance;
	}

	String chanceMapDbTablename = "chanceMapDbTablename";
	String _kptkey = "_kpt";

	static Map<String, String> _knm = new HashMap<String, String>();
	static Map<String, String> _vpm = new HashMap<String, String>();
	// 用户定制策略列表
	static Map<String, String> _userRulepm = new HashMap<String, String>();
	static Map<String, String> _realDescMap = new HashMap<String, String>();
	static Set<String> _realSet = new HashSet<String>();
	static Set<String> _norealSet = new HashSet<String>();
	static {
		initKMap();

		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			@Override
			public void refresh() {
				initKMap();
			}
		});
	}

	private static void initKMap() {
		String kns = ConfigCenterFactory.getString("stock_zjs.chance_names",
				"chancea:沪深,js:技术面,cdx:分时异动,dx:日线异动,essence:网友观点");
		if (_knm.size() > 0) {
			_knm.clear();
		}
		for (String kn : kns.split(",")) {
			try {
				_knm.put(kn.split(":")[0], kn.split(":")[1]);
			} catch (Exception e) {
				log.error("initKMap failed!", e);
			}
		}
		String realDescs = ConfigCenterFactory.getString("stock_zjs.realDescs",
				"0:突然上涨,1:突然下跌,2:突然放量,3:盘中异动");
		for (String rd : realDescs.split(",")) {
			try {
				_realDescMap.put(rd.split(":")[0], rd.split(":")[1]);
			} catch (Exception e) {
				log.error("initKMap failed!", e);
			}
		}

		String needViewpoint_realtypes = ConfigCenterFactory.getString(
				"snn.need_viewpoint_trade_alarm_100", "0,2,1602,1603");
		if (_vpm.size() > 0) {
			_vpm.clear();
		}
		for (String kn : needViewpoint_realtypes.split(",")) {
			try {
				_vpm.put(kn, kn);
			} catch (Exception e) {
				log.error("initKMap failed!", e);
			}
		}

		String user_rule_maps = ConfigCenterFactory.getString(
				"snn.user_rule_maps", "2,1603:20,21;1603:20,21");
		if (!StringUtil.isEmpty(user_rule_maps)) {
			_userRulepm.clear();
		}
		for (String kn : user_rule_maps.split(";")) {
			try {
				if (!StringUtil.isEmpty(kn)) {
					String[] kna = kn.split(":");
					String rids = kna[0];
					String uids = kna[1];
					if(!StringUtil.isEmpty(rids))
					{
						for (String rid : rids.split(","))
						{
							_userRulepm.put(rid, uids);
						}
					}
					
				}
				_vpm.put(kn, kn);
			} catch (Exception e) {
				log.error("initKMap failed!", e);
			}
		}

		String needNotify_realtypes = ConfigCenterFactory.getString(
				"snn.need_notify_trade_alarm_100", "0,2");
		if(!StringUtil.isEmpty(needNotify_realtypes))
		{
			for(String kd:needNotify_realtypes.split(","))
			{
				_realSet.add(kd);
			}
		}
		
		needNotify_realtypes = ConfigCenterFactory.getString(
				"snn.need_notify_trade_alarm_101", "1602,1603");
		if(!StringUtil.isEmpty(needNotify_realtypes))
		{
			for(String kd:needNotify_realtypes.split(","))
			{
				_norealSet.add(kd);
			}
		}
	}

	public void notifyTheEventChance(TradeAlarmMsg um) {
		if (um == null) {
			return;
		}

		int send_hk_chance_switch = ConfigCenterFactory.getInt(
				"stock_zjs.send_hk_chance_switch", 0);// 暂时屏蔽港股
		if (send_hk_chance_switch == 0
				&& StringUtils.isNotBlank(um.getSourceid())
				&& um.getSourceid().endsWith(".hk")) {
			log.info("push_hk_chance: " + um.getSourceid());
			return;
		}

		String pdtype = um.getAttr("desc");
		if (StringUtil.isEmpty(pdtype)) {
			pdtype = um.getEventid();
			Tagrule tr = TagruleService.getInstance().getTagruleByIdFromCache(
					um.getEventid());
			if (tr == null || StringUtil.isEmpty(tr.getComments())) {
				return;
			}
		}

		// 推送机会
		if (!StringUtil.isEmpty(pdtype) && _vpm.containsKey(pdtype)) {
			// 发布观点
			SimpleArticle simplearticle = getSimpleArticleByUm(um);
			um.putAttr("uid", simplearticle.getUid());
			um.putAttr("title", simplearticle.getTitle());
			um.putAttr("summary", simplearticle.getSummary());
			um.putAttr(StockConstants.SIMPLE_ARTICLE_ENTITY, simplearticle);
			TradeService.getInstance().addStockChance(um);
		}

	}

	public boolean publishStockChance(Article article) {
		StringUtil.sbuArticle(article);
		String summary = MicorBlogService.getInstance().replaceAtUserWithUid(
				article.getSummary());
		article.setSummary(summary);
		article.setStatus(1);// 状态=1，正常
		long timemillis = System.currentTimeMillis();
		article.setTime(timemillis);

		SimpleArticle simplearticle = getSimpleArticleFromArticle(article);

		TradeAlarmMsg um = new TradeAlarmMsg();
		um.setSourceid(article.getTags());
		um.setStime(article.getTime());
		um.setTime(article.getTime());
		um.setEtime(article.getTime());
		um.setUuid(article.getUuid());
		um.setMsgType(MsgConst.MSG_TRADEMSG_TYPE_3);
		um.putAttr("title", article.getTitle());
		um.putAttr("summary", article.getSummary());
		um.putAttr("uid", article.getUid());
		Double price = article.getAttr("price");
		if (price != null) {
			um.putAttr("price", price);
		}
		String resultStr = ViewpointService.getInstance().publishViewpoint(simplearticle, um);
		if(StockCodes.SUCCESS.equals(resultStr)){
			return true;
		}else{
			return false;
		}		
	}

	public SimpleArticle getSimpleArticleFromArticle(Article article) {
		SimpleArticle simplearticle = new SimpleArticle();
		simplearticle.setSummary(article.getSummary());
		simplearticle.setTitle(article.getTitle());
		simplearticle.setUid(article.getUid());
		simplearticle.setTags(article.getTags());
		simplearticle.setUuid(article.getUuid());
		simplearticle.setTime(article.getTime());
		simplearticle.setImg(article.getImg());
		simplearticle.setType(article.getType());
		simplearticle.setArticleType(article.getArticleType());
		simplearticle.setNick(article.getNick());
		simplearticle.setTime(article.getTime());
		simplearticle.setBroadcast_counts(article.getBroadcast_counts());
		simplearticle.setBrowse_counts(article.getBrowse_counts());
		simplearticle.setComment_counts(article.getComment_counts());
		simplearticle.setFavorite_counts(article.getFavorite_counts());
		simplearticle.setKey(article.getKey());
		simplearticle.setRecommend_counts(article.getRecommend_counts());
		simplearticle.setSource_url(article.getSource_url());
		simplearticle.setSuuid(article.getSuuid());
		simplearticle.setViewpointType(article.getViewpointType());
		simplearticle.putAttr("ats", article.getAts());
		simplearticle.putAttr("content", article.getContent());
		simplearticle.putAttr("keywordLevel", article.getKeywordLevel());
		simplearticle.putAttr("system_category", article.getSystem_category());
		simplearticle.putAttr("blog_category", article.getBlog_category());
		Integer equitycount = article.getAttr(StockConstants.EQUITY_COUNT);
		if (equitycount != null) {
			simplearticle.putAttr(StockConstants.EQUITY_COUNT, equitycount);
		}
		String chancetag = article.getAttr(StockConstants.CHANCETAG);
		if (StringUtils.isNotBlank(chancetag)) {
			simplearticle.putAttr(StockConstants.CHANCETAG, chancetag);
		}
		return simplearticle;
	}

	public void addStockChance(TradeAlarmMsg um) {
		um.setSendType(MsgConst.SEND_TYPE_0);
		if (um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_0) {
			String desc = um.getAttr("desc");
			String firstKey = TradeService.getInstance().getFirstByType(
					um.getSourceid());
			String k = StockUtil.joinString(StockConstants.UL, firstKey,
					StockConstants.JS, StockConstants.CDX, desc);
			um.putAttr("k", k);
			NotifyEvent ne = new NotifyEvent();
			ne.setHType(EConst.EVENT_1);
			ne.setMsg(um);
			ClientEventCenter.getInstance().putEvent(ne);
		}

		if (um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_1) {
			Tagrule tr = TagruleService.getInstance().getTagruleByIdFromCache(
					um.getEventid());
			if (tr == null)
				return;
			String firstKey = TradeService.getInstance().getFirstByType(
					um.getSourceid());
			String k = StockUtil.joinString(StockConstants.UL, firstKey,
					StockConstants.JS, StockConstants.DX, tr.getId());
			um.putAttr("k", k);
			NotifyEvent ne = new NotifyEvent();
			ne.setHType(EConst.EVENT_1);
			ne.setMsg(um);
			ClientEventCenter.getInstance().putEvent(ne);
		}

		if (um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_3) {
			Integer op = um.getAttr("op");
			String firstKey = TradeService.getInstance().getFirstByType(
					um.getSourceid());

			if (op == null) {
				Long uid = um.getAttr("uid");
				if (uid == null) {
					log.error("TradeServiceAddStockChance3: uid is null===> ");
					return;
				}

				Integer recommend = um.getAttr("recommend");
				String menu = "";
				if (recommend == null) {
					Map<Long, String> vpMemberMenuMap = MicorBlogService
							.getInstance().getVpMemberMenu();
					menu = vpMemberMenuMap.get(uid);
					if (StringUtils.isBlank(menu)) {
						log.error("TradeServiceAddStockChance4: menu is null===> "
								+ uid);
						return;
					}
				} else if (recommend == 1) {
					menu = um.getAttr("recommend_menu");
					if (StringUtils.isBlank(menu)) {
						log.error("TradeServiceAddStockChance5: menu is null===> "
								+ uid);
						return;
					}
				}

				Object[] arr = (firstKey + StockConstants.UL + menu)
						.split(StockConstants.UL);
				String k = StockUtil.joinString(StockConstants.UL, arr);

				um.putAttr(StockConstants.CHANCETAG, menu);
				um.putAttr("k", k);
				NotifyEvent ne = new NotifyEvent();
				ne.setHType(EConst.EVENT_1);
				ne.setMsg(um);
				ClientEventCenter.getInstance().putEvent(ne);
			} else if (op == 1 || op == 3) {
				um.putAttr("k", "recover_essence_uuids");
				NotifyEvent ne = new NotifyEvent();
				ne.setHType(EConst.EVENT_1);
				ne.setMsg(um);
				ClientEventCenter.getInstance().putEvent(ne);
			} else if (op == 2) {
				String menu = um.getAttr(StockConstants.CHANCETAG);
				if (StringUtils.isBlank(menu)) {
					log.error("TradeServiceAddStockChance6: op=2 menu is null===> ");
					return;
				}

				Object[] arr = (firstKey + StockConstants.UL + menu)
						.split(StockConstants.UL);
				String k = StockUtil.joinString(StockConstants.UL, arr);

				um.putAttr("k", k);
				NotifyEvent ne = new NotifyEvent();
				ne.setHType(EConst.EVENT_1);
				ne.setMsg(um);
				ClientEventCenter.getInstance().putEvent(ne);
			}
		}
	}

	public void notifyTheStockPriceEvent(StockPriceBatchMsg im) {
		im.setMsgType(MsgConst.MSG_STOCK_PRICE_TYPE_0);
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_6);
		ne.setMsg(im);
		ClientEventCenter.getInstance().putEvent2ChildQueue(EConst.EVENT_6, ne,
				1000);
	}

	public void notifyTheRealtimeComputeDataUpdateEvent(
			RealtimeComputeDataUpdateMsg im) {
		im.setMsgType(MsgConst.MSG_TRADEMSG_TYPE_2);
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_10);
		ne.setMsg(im);
		ClientEventCenter.getInstance().putEvent2ChildQueue(EConst.EVENT_10,
				ne, 1000);
	}

	/**
	 * 取日交易时间长
	 * 
	 * @param uidentify
	 * @return
	 */
	public double getTradeTimeRegion(String uidentify) {
		if (uidentify.endsWith("hk")) {
			// 港股交易时长为
			return 330 * 60 * 1000;
		}
		return 240 * 60 * 1000;
	}

	public void computeCompanySetIndexs(List<Company> csl, String indexcodes) {
		for (Company us : csl) {
			{
				Stock0001 s = Stock0001Service
						.getInstance()
						.getStock0001ByCompanycodeFromCache(us.getCompanyCode());
				if (s == null)
					return;
				System.out.println("start compute company:"
						+ us.getSimpileName());
				String[] ia = indexcodes.split(",");
				for (String indexcode : ia) {
					Dictionary d = DictService.getInstance()
							.getDataDictionaryFromCache(indexcode);
					if (d == null)
						continue;
					Date time = DateUtil.getDayStartTime(new Date());

					Date mintime = USubjectService.getInstance()
							.getTradeIndexMinTime(us.getCompanyCode(),
									indexcode);
					Date maxtime = time;

					Calendar c = Calendar.getInstance();
					c.setTime(mintime);
					while ((c.getTime().compareTo(mintime) >= 0)
							&& (c.getTime().compareTo(maxtime) <= 0)) {
						try {
							Date acTime = IndexService.getInstance()
									.formatTime(c.getTime(), d,
											us.getCompanyCode());
							if (acTime != null) {
								Double v = RealTimeService.getInstance()
										.realTimeComputeIndex(
												us.getCompanyCode(), indexcode,
												acTime);
								if (v != null && v != 0) {
									RealTimeService.getInstance()
											.put2LocalCache(
													us.getCompanyCode(),
													indexcode, v, acTime);
								}
							}
						} catch (Exception e) {
							log.error("computeIndexOfCompany failed!", e);
						}
						Date nd = StockUtil.getNextTimeV3(c.getTime(),
								Integer.valueOf(d.getInterval()), d.getTunit());
						c.setTime(nd);

					}

				}
			}
		}
	}

	public long getLastChanceUpdateTime() {
		return RemindServiceClient.getInstance().getLastChanceUpdateTime(
				StockConstants.SCC_ENTITY);
	}

	public List<Map<String, Object>> getChanceMessageList(String key,
			int ftype, long time, int limit) {
		return RemindServiceClient.getInstance().getChanceMessageList(key,
				ftype, time, limit);
	}

	public int getIMessageListCount(String key, int ftype, long time) {
		return RemindServiceClient.getInstance().getIMessageListCount(key,
				ftype, time);
	}

	public List<Map<String, Object>> getLatestNewChanceMessageList(String key,
			int limit) {
		return RemindServiceClient.getInstance().getLatestNewChanceMessageList(
				key, limit);
	}

	public IMessage getChanceMessage(String key, String uuid) {
		return RemindServiceClient.getInstance().getChanceMessage(key, uuid);
	}

	// public List<IMessage> getPagingChanceMessageList(String key, int
	// oldTotalCount, int pageNo, int limit) {
	// return RemindServiceClient.getInstance().getPagingChanceMessageList(key,
	// oldTotalCount, pageNo, limit);
	// }

	public String getRealMsgDescByRealType(int dtype) {
		return _realDescMap.get(String.valueOf(dtype));
	}

	public List<Map<String, String>> getRealMsgList(int msgType) {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		if (msgType == MsgConst.MSG_TRADEMSG_TYPE_0) {
			String real_msg_desc_list = ConfigCenterFactory.getString(
					"stock_zjs.real_msg_desc_list",
					"0:突然上涨,1:突然下跌,2:突然放量,3:盘中异动");
			String[] arr = real_msg_desc_list.split(",");
			for (String str : arr) {
				String[] tmp = str.split(":");
				String key = tmp[0];
				String val = tmp[1];
				Map<String, String> item = new HashMap<String, String>();
				item.put("k", key);
				item.put("v", val);
				list.add(item);
			}
		} else if (msgType == MsgConst.MSG_TRADEMSG_TYPE_1) {
			List<Tagrule> tagList = TagruleService.getInstance()
					.getAllTagrules();
			if (tagList != null && tagList.size() > 0) {
				for (Tagrule tag : tagList) {
					Map<String, String> item = new HashMap<String, String>();
					item.put("k", String.valueOf(tag.getId()));
					item.put("v", tag.getTagDesc());
					list.add(item);
				}
			}
		}

		return list;
	}

	public String getRealMsgSummery(int dtype) {
		String key = "realtime_server.chance_fs_" + dtype;
		return ConfigCenterFactory.getString(key, "");
	}

	public void notifyToUserPlate(TradeAlarmMsg um) {
		// 通知订阅些消息的用户
		String needNotify_realtypes = ConfigCenterFactory.getString(
				"snn.need_notify_trade_alarm_100", "0,2");
		String dtype = um.getAttr("desc");
		if (!StringUtil.isEmpty(dtype) && needNotify_realtypes.contains(dtype)) {
			USubject us = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(um.getSourceid());
			if (us != null) {
				String info = um.getAttr("info");
				/*
				 * String msg =
				 * "实时行情异动：<br/><a onclick=\"setCookie('companysLeftLi','cmLi1')\" href=\"/company_main.html?companycode="
				 * + us.getUidentify() + "&amp;p=0&amp;companyname=" +
				 * StockUtil.escape(us.getName()) + "\" target=\"_blank\">" +
				 * TradeService.getInstance().getRealMsgDescByRealType(
				 * Integer.valueOf(dtype)) + "：" + info + " <br/>发生时段：" +
				 * DateUtil.getSysDate(DateUtil.HHMMSS, new Date(um.getStime()))
				 * + "-" + DateUtil.getSysDate(DateUtil.HHMMSS, new
				 * Date(um.getEtime())) +
				 * "</a></br><span class=\"kLineDiv\"><img nickname=\"" +
				 * us.getName() + "(" + us.getUidentify() + ")\" suid=\"" +
				 * us.getUidentify() + "\" src=\"" + getKUrl(us, "min") + "\"" +
				 * "KLineType=\"0\"" + "alt=\"日K线\"></span>";
				 */
				// 广播给所有用户
				long bkydUid = ConfigCenterFactory.getLong(
						"stock_zjs.bkyd_uid", 104599l);
				Map<String, Serializable> headerMap = new HashMap<String, Serializable>();
				headerMap.put("l", us.getName() + "^" + us.getUidentify()
						+ "^0");// 时分图
				headerMap.put("i", getKUrl(us, "min"));// 时分图
				String body = "实时行情异动：<br/>"
						+ TradeService.getInstance().getRealMsgDescByRealType(
								Integer.valueOf(dtype))
						+ ":"
						+ info
						+ " 发生时段：<br/>"
						+ DateUtil.getSysDate(DateUtil.HHMMSS,
								new Date(um.getStime()))
						+ "-"
						+ DateUtil.getSysDate(DateUtil.HHMMSS,
								new Date(um.getEtime()));
				TalkMessageService.getInstance()
						.broadcastOnlineTalkMessageWithoutSave(bkydUid, body,
								4, headerMap);
			}

		}
	}

	public void notifyToUser(TradeAlarmMsg um) {
		

		// 通知订阅些消息的用户
		String dtype = um.getAttr("desc");
		if (!StringUtil.isEmpty(dtype) && _realSet.contains(dtype)) {
		
			USubject us = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(um.getSourceid());
			if (us != null) {
				String info = um.getAttr("info");
				
				String zss = ConfigCenterFactory.getString(
						"realtime_server.zs_code_list", "000001.sh");

				String body = "实时行情异动"
						+ "<br/>"
						+ TradeService.getInstance().getRealMsgDescByRealType(
								Integer.valueOf(dtype))
						+ ": "
						+ info
						+ "<br/>"
						+ "发生时段："
						+ DateUtil.getSysDate(DateUtil.HHMMSS,
								new Date(um.getStime()))
						+ "-"
						+ DateUtil.getSysDate(DateUtil.HHMMSS,
								new Date(um.getEtime()));
				if (zss.contains(us.getUidentify())) {
					// 广播给所有用户
					long bkydUid = ConfigCenterFactory.getLong(
							"stock_zjs.bkyd_uid", 104599l);
					Map<String, Serializable> headerMap = new HashMap<String, Serializable>();
					headerMap.put("l", us.getName() + "^" + us.getUidentify()
							+ "^0");// 时分图
					headerMap.put("i", getKUrl(us, "min"));// 时分图
					TalkMessageService.getInstance()
							.broadcastOnlineTalkMessageWithoutSave(bkydUid,
									body, 4, headerMap);
					// UsubjectEventService.getInstance()
					// .broadcastOnlineTalkMessage(bkydUid, msg);
				} else {
					// 广播给订阅了的用户
					Map<String, Serializable> headerMap = new HashMap<String, Serializable>();
					headerMap.put("l", us.getName() + "^" + us.getUidentify()
							+ "^0");// 时分图
					headerMap.put("i", getKUrl(us, "min"));// 时分图
					TalkMessageService.getInstance()
							.broadcastUsbjectTalkMessage(um.getSourceid(),
									body, 4, headerMap);
					// UsubjectEventService.getInstance()
					// .broadcastUsbjectTalkMessage(um.getSourceid(), msg,
					// "0", mobileStr);
				}

			}

		}

		// 通知订阅些消息的用户
		String eid = um.getEventid();
		if (!StringUtil.isEmpty(eid)) {
			Tagrule tr = TagruleService.getInstance().getTagruleByIdFromCache(
					eid);

			if (tr != null && _norealSet.contains(eid)) {
				String k = um.getSourceid() + "_"
						+ getTimeByUnit(tr.getTunit()) + "_" + eid;
				if (BFUtil.checkAndAdd(BFConst.tradeAlarm, k))
					return;
				USubject us = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(um.getSourceid());
				if (us != null) {
					

					String body = "日行情异动:" + "<br/>" + "告警类型: "
							+ tr.getTagDesc() + "<br/>" + "发生时段："
							+ DateUtil.getSysDate(DateUtil.HHMMSS, new Date(um.getTime()));

					Map<String, Serializable> headerMap = new HashMap<String, Serializable>();
					headerMap.put("l", us.getName() + "^" + us.getUidentify()
							+ "^1");// 时分图
					headerMap.put("i", getKUrl(us, "daily"));// img(for pc)

					TalkMessageService.getInstance()
							.broadcastUsbjectTalkMessage(um.getSourceid(),
									body, 4, headerMap);
					// UsubjectEventService.getInstance()
					// .broadcastUsbjectTalkMessage(um.getSourceid(), msg,
					// "1", mobileStr);
				}

			}
		}

		String suid  = ConfigCenterFactory.getString(
				"snn.user_rule_suid", "114692");
		if (!StringUtil.isEmpty(eid)) {
			// 把用户定制的策略以私信的方式发给用户
			String uids = _userRulepm.get(eid);
			if(!StringUtil.isEmpty(uids))
			{
				Tagrule tr = TagruleService.getInstance().getTagruleByIdFromCache(
						eid);
				
				if (tr != null) {
					USubject us = USubjectService.getInstance()
							.getUSubjectByUIdentifyFromCache(um.getSourceid());
					if (us != null) {

						String body =  "日行情异动:" + "<br/>" 
								+ "公司名称："
								+ us.getName() + "(" + us.getUidentify()+")" + "<br/>" 
								+ "告警类型: "
								+ tr.getTagDesc() + "<br/>" + "发生时段："
								+ DateUtil.getSysDate(DateUtil.HHMMSS, new Date(um.getTime()));

						Map<String, Serializable> headerMap = new HashMap<String, Serializable>();
						headerMap.put("l", us.getName() + "^" + us.getUidentify()
								+ "^1");// 时分图
						headerMap.put("i", getKUrl(us, "daily"));// img(for pc)

						// 取用户列表
						for (String uid : uids.split(",")) {
							TalkMessageService.getInstance().singlecastTalkMessageAsyn(
									Long.valueOf(suid), Long.valueOf(uid), body, 4, headerMap);
						}
					}

				}
			}
		}
		
		if (!StringUtil.isEmpty(dtype) )  {
			String uids = _userRulepm.get(dtype);
			if(!StringUtil.isEmpty(uids))
			{
				USubject us = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(um.getSourceid());
				if (us != null) {
					String info = um.getAttr("info");

					String body = "实时行情异动"
							+ "<br/>"
							+ "公司名称："
							+ us.getName() + "(" + us.getUidentify()+")" + "<br/>" 
							+ TradeService.getInstance().getRealMsgDescByRealType(
									Integer.valueOf(dtype))
							+ "："
							+ info
							+ "<br/>"
							+ "发生时段："
							+ DateUtil.getSysDate(DateUtil.HHMMSS,
									new Date(um.getStime()))
							+ "-"
							+ DateUtil.getSysDate(DateUtil.HHMMSS,
									new Date(um.getEtime()));
					

					Map<String, Serializable> headerMap = new HashMap<String, Serializable>();
					headerMap.put("l", us.getName() + "^" + us.getUidentify()
							+ "^0");// 时分图
					headerMap.put("i", getKUrl(us, "min"));// 时分图
					// 取用户列表
					for (String uid : uids.split(",")) {
						TalkMessageService.getInstance().singlecastTalkMessageAsyn(
								Long.valueOf(suid), Long.valueOf(uid), body, 4, headerMap);
					}
				}
			}
		}

	}

	private String getTimeByUnit(int tunit) {
		Long etime = null;
		Date et = new Date();
		switch (tunit) {
		case Calendar.MONTH:
			et = new Date();
			etime = DateUtil.getMonthStartTime(et).getTime();
			break;
		case Calendar.WEEK_OF_MONTH:
			et = new Date();
			etime = DateUtil.getWeekStartTime(et).getTime();
			break;
		case Calendar.DAY_OF_MONTH:
			et = new Date();
			etime = DateUtil.getDayStartTime(et).getTime();
			break;
		}
		return etime.toString();
	}

	private String getKUrl(USubject us, String type) {
		String[] ca = us.getUidentify().split("\\.");
		String sc = ca[1] + ca[0];
		String imgurl = "http://image.sinajs.cn/newchart/" + type + "/n/" + sc
				+ ".gif?v=" + System.currentTimeMillis();
		if (us.getUidentify().indexOf("hk") > 0) {
			imgurl = "http://image.sinajs.cn/newchart/hk_stock/" + type + "/"
					+ us.getUidentify().split("\\.")[0] + ".gif?_="
					+ System.currentTimeMillis();
		}
		return imgurl;
	}

	String _categoryskey = "chance__categorylist";

	public List<Pair<String, String>> getChancesCategoryList() {
		StockChanceEntity sc = RemindServiceClient.getInstance()
				.getStockChanceEntity(StockConstants.SCC_ENTITY);
		if (sc != null) {
			return sc.getCategorylist();
		}
		return null;
	}

	public List<Pair<String, String>> getChancesCategoryLeafList(
			StockChanceEntity sc, Long ctime) {
		List<Pair<String, String>> lp = null;
		if (sc != null && sc.getCategorylist().size() > 0) {
			lp = new ArrayList<Pair<String, String>>();
			for (int i = 0; i < sc.getCategorylist().size(); i++) {
				Pair<String, String> ps = sc.getCategorylist().get(i);
				String ckey = ps.first.split(":")[2];
				if (ps.second == null) {
					continue;
				}
				String luptime = ps.second.split(":")[2];
				if (ckey.equals("1") && ctime < Long.valueOf(luptime))
					lp.add(ps);
			}
			return lp;
		}
		return lp;
	}

	// public List<Pair<String, String>> getChancesCategoryLeafList(Long ctime)
	// {
	// StockChanceEntity sc =
	// RemindServiceClient.getInstance().getStockChanceEntity(StockConstants.SCC_ENTITY);
	// List<Pair<String, String>> lp = null;
	// if(sc != null && sc.getCategorylist().size() > 0) {
	// lp = new ArrayList<Pair<String, String>>();
	// for (int i = 0; i < sc.getCategorylist().size(); i++) {
	// Pair<String, String> ps = sc.getCategorylist().get(i);
	// String ckey = ps.first.split(":")[2];
	// if (ps.second == null) {
	// continue;
	// }
	// String luptime = ps.second.split(":")[2];
	// if (ckey.equals("1") && ctime < Long.valueOf(luptime))
	// lp.add(ps);
	// }
	// return lp;
	// }
	// return lp;
	// }

	public long getLatestMessageTime(String key) {
		return RemindServiceClient.getInstance().getLatestMessageTime(key);
	}

	public boolean deleteChancesCategoryByKey(String key) {
		return RemindServiceClient.getInstance().deleteChancesCategoryByKey(
				StockConstants.SCC_ENTITY, key);
	}

	public boolean clearChanceCategory() {
		return RemindServiceClient.getInstance().clearChanceCategory(
				StockConstants.SCC_ENTITY);
	}

	public String getNameByType(String k) {
		return _knm.get(k);
	}

	public String getFirstByType(String sourceid) {
		String k = "";
		if (sourceid.endsWith("hk")) {
			k = "chancehk";
		}
		if (sourceid.endsWith("sz") || sourceid.endsWith("sh")) {
			k = "chancea";
		}
		return k;
	}

	public List<String> getDxFirstKeyList() {
		String firsts = ConfigCenterFactory.getString(
				"stock_zjs.chance_init_first", "chancehk,chancea");
		List<String> list = new ArrayList<String>();
		for (String key : firsts.split(",")) {
			list.add(key);
		}
		return list;
	}

	public SimpleArticle getSimpleArticleByUm(TradeAlarmMsg um) {
		SimpleArticle sa = new SimpleArticle();
		sa.setType(ShareConst.VIEWPOINT);
		sa.set_attr(um.getAllAttr());
		// String name = "";
		String uidentify = um.getSourceid();
		String tags = null;
		USubject usubject = USubjectService.getInstance()
				.getUSubjectByUIdentifyFromCache(uidentify);
		if (usubject != null) {
			tags = StockUtil.joinString(":", usubject.getUidentify(),
					usubject.getName());
			// name = usubject.getName() + "(" + um.getSourceid() + ") ";
		} else {
			log.error("TradeServicegetSimpleArticleByUm: " + uidentify);
			tags = uidentify;
		}
		String summary = um.getAttr("summary");
		String title = um.getAttr("title");
		sa.setSummary(summary);
		sa.setTitle(title);

		long yunPushId = ConfigCenterFactory.getLong("stock_zjs.yunPushId",
				104600l);
		UserExt userext = null;
		try {
			userext = UserServiceClient.getInstance()
					.getUserExtByUid(yunPushId);
		} catch (Exception e) {
			log.error("获取UserExt异常", e);
		}
		sa.setUid(yunPushId);
		if (userext == null) {
			sa.setNick("爱股说云推送");
		} else {
			sa.setNick(userext.getNickname());
		}
		long time = um.getTime();
		String uuid = um.getUuid();
		sa.setTags(tags);
		sa.setUuid(uuid);
		sa.setTime(time);
		return sa;
	}

	public String getCategoryName(String split, Object... args) {
		String ret = null;
		if (args != null) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < args.length; i++) {
				Object s = args[i];
				String t = "";
				if (s != null) {
					t = (String) s;
				}
				String tmp = TradeService.getInstance().getNameByType(t);
				if (StringUtils.isBlank(tmp)) {
					log.error("nullchancekey: " + t + joinArgs(args));
				}
				sb.append(tmp);
				if (i != args.length - 1)
					sb.append(split);
			}
			ret = sb.toString();
		}
		return ret;

	}

	private String joinArgs(Object... args) {
		StringBuffer s = new StringBuffer();
		for (Object t : args) {
			if (t != null) {
				s.append((String) t + "       ");
			}
		}
		return s.toString();
	}

	// 是否为交易日
	public boolean isTradeDay(String companycode) {
		String lastTradeDate = "";
		boolean openLog = ConfigCenterFactory.getInt("stock_log.trade_log", 0) == 1;
		if (companycode.endsWith(".hk")) {
			lastTradeDate = TradeCenter.getInstance().getHKLastDate();
		} else {
			lastTradeDate = TradeCenter.getInstance().getALastDate();
		}
		String nowDate = DateUtil.getSysDateYYYYMMDD(new Date());
		if (openLog) {
			log.info("isTradeDay==>lastTradeDate: " + lastTradeDate
					+ " nowDate: " + nowDate);
		}
		if (lastTradeDate.equals(nowDate)) {
			Company c = CompanyService.getInstance().getCompanyByCode(
					companycode);
			boolean isTradeDay = !(CompanyService.getInstance().isStop(c));
			return isTradeDay;
		} else {
			return false;
		}

		/*
		 * String realData =
		 * IndexService.getInstance().getRealtimeTrade(companycode,0); String
		 * lastData = getStockLastPoint(companycode);
		 * if(StringUtils.isEmpty(realData) || StringUtils.isEmpty(lastData)){
		 * return false; } boolean open_log =
		 * ConfigCenterFactory.getInt("stock_log.trade_log", 0)==1;
		 * StringBuilder l_sb = new StringBuilder(); StringBuilder r_sb = new
		 * StringBuilder(); if(StringUtils.isEmpty(lastData) ||
		 * StringUtils.isEmpty(realData)){ return false; } String[] ldArr =
		 * lastData.split("\\^"); String[] rdArr = realData.split("\\^");
		 * if(ldArr.length<8 || rdArr.length<8){ return false; } double k =
		 * Double.parseDouble(rdArr[2]); if(k<=0){ return false; }
		 * if(StringUtils.isNumericSpace(ldArr[0]) &&
		 * StringUtils.isNumericSpace(rdArr[0])){ long ltime =
		 * Long.parseLong(ldArr[0]); long rtime = Long.parseLong(rdArr[0]);
		 * //k:开盘价，s:收盘价,zg:最高价，zd:最低价，cjl:成交量，cje:成交额，zdf:涨跌幅，
		 * l_sb.append(CommonUtil.setScale(ldArr[1], 2)); l_sb.append("_");
		 * l_sb.append(CommonUtil.setScale(ldArr[2], 2)); l_sb.append("_");
		 * l_sb.append(CommonUtil.setScale(ldArr[3], 2)); l_sb.append("_");
		 * l_sb.append(CommonUtil.setScale(ldArr[4], 2)); l_sb.append("_");
		 * //非交易日 成交量有差异 不作为判断依据 l_sb.append(CommonUtil.setScale(ldArr[5],0));
		 * l_sb.append("_"); l_sb.append(CommonUtil.setScale(ldArr[6], 0));
		 * l_sb.append("_"); l_sb.append(CommonUtil.setScale(ldArr[7], 2));
		 * 
		 * r_sb.append(CommonUtil.setScale(rdArr[1], 2)); r_sb.append("_");
		 * r_sb.append(CommonUtil.setScale(rdArr[2], 2)); r_sb.append("_");
		 * r_sb.append(CommonUtil.setScale(rdArr[3], 2)); r_sb.append("_");
		 * r_sb.append(CommonUtil.setScale(rdArr[4], 2)); r_sb.append("_");
		 * r_sb.append(CommonUtil.setScale(rdArr[5], 0)); r_sb.append("_");
		 * r_sb.append(CommonUtil.setScale(rdArr[6],0)); r_sb.append("_");
		 * r_sb.append(CommonUtil.setScale(rdArr[7], 2));
		 * 
		 * String key_L = l_sb.toString(); String key_R = r_sb.toString();
		 * if(open_log){
		 * log.info("companycode: "+companycode+"\n ltime: "+ltime+
		 * " key_L :"+key_L +"\n rtime: "+rtime+"key_R :"+key_R
		 * +" 非交易日?"+key_L.equals(key_R)); } if(rtime>0 && ltime>0 &&
		 * rtime>ltime && !key_L.equals(key_R)){ return true; } }
		 */
	}

	// 把最近一个交易日的日K数据放入本地缓存
	public void put2Local(String companycode) {
		String key = companycode + StockConstants.DATA_LAST_TRADE;
		String cacheName = StockUtil.getKChartLastDataCacheName(companycode);
		String lastpoint = DcssTradeIndexServiceClient.getInstance()
				.getTradeDataByType(companycode, 0, new Date(), null, 0, 1);
		LCEnter.getInstance().put(key, lastpoint, cacheName);
	}

	/**
	 * K线判断版本号 以小版本号为准 配置中没有小版本号 以大版本为准
	 * 
	 * @param companycode
	 * @param paramV
	 *            传入版本号
	 * @param bigV
	 *            大版本号
	 * @param version
	 *            小版本号
	 * @return
	 */
	public boolean checkVersion(String companycode, long paramV, long bigV,
			String version) {
		boolean result = false;
		long localV = -1;
		if (bigV == -1) {// 配置没有下发 返回
			return false;
		}
		if (StringUtils.isNumericSpace(version)) {
			localV = Long.parseLong(version);
		} else {// 没有配置小版本号
			if (paramV != bigV) {
				result = true;
			}
		}
		if (localV != -1 && localV != paramV) {
			result = true;
		}
		return result;
	}

	public String getStockLastPoint(String companycode) {
		String cacheName = StockUtil.getKChartLastDataCacheName(companycode);
		return LCEnter.getInstance().get(
				companycode + StockConstants.DATA_LAST_TRADE, cacheName);
	}

	// 格式化K线数据 便于查看
	public String formatData(String data) {
		StringBuilder sb = new StringBuilder();
		String[] arr = data.split("~");
		if (!StringUtils.isEmpty(data)) {
			for (String d : arr) {
				StringBuilder sbu = new StringBuilder();
				int index = d.indexOf("^");
				String t = d.substring(0, index);
				String l = d.substring(index);
				if (StringUtils.isNumericSpace(t)) {
					String formatTime = DateUtil.getSysDateYYYYMMDD(new Date(
							Long.parseLong(t)));
					sbu.append(formatTime);
					sbu.append(l);
					sb.append(sbu.toString());
					sb.append("||");
				}
			}
		}
		return sb.toString();
	}

	// 格式化K线数据 便于查看(moblie)
	public String formatDataMoblie(String data) {
		StringBuilder sb = new StringBuilder();
		String[] arr = data.split("\n");
		for (String d : arr) {
			StringBuilder sbu = new StringBuilder();
			int index = d.indexOf(",");
			String t = d.substring(0, index);
			String l = d.substring(index);
			if (StringUtils.isNumericSpace(t)) {
				String formatTime = DateUtil.getSysDateYYYYMMDD(new Date(Long
						.parseLong(t)));
				sbu.append(formatTime);
				sbu.append(l);
				sb.append(sbu.toString());
				sb.append("||");
			}
		}
		return sb.toString();
	}

	public List<String> getKeysFromTradeAlarmMsg(String key, String uuid) {
		IMessage msg = TradeService.getInstance().getChanceMessage(key, uuid);
		if (msg != null) {
			TradeAlarmMsg um = (TradeAlarmMsg) msg;
			return ChanceCategoryService.getInstance().getKeyListByNotifyEvent(
					um);
		}
		return null;
	}

	public void updateStockChanceViewpoint(String uuid, int equitCount) {
		Viewpoint vp = MicorBlogService.getInstance().getViewpoint(uuid);
		if (vp != null) {
			vp.putAttr(StockConstants.EQUITY_COUNT, equitCount);
			Map<String, String> map = NosqlBeanUtil.bean2Map(vp);
			CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
			ch.insert(SAVE_TABLE.ARTICLE.toString(), vp.getUuid(), map);
		} else {
			log.error("updateStockChanceViewpoint: vp is null " + uuid);
		}

		SimpleArticle sa = MicorBlogService.getInstance()
				.getSimpleArticle(uuid);
		if (sa != null) {
			log.info("updateStockChanceViewpoint: "
					+ (sa.getAttr(StockConstants.EQUITY_COUNT) == null ? 0 : sa
							.getAttr(StockConstants.EQUITY_COUNT)));
			RemindServiceClient.getInstance()
					.putSimpleArticle(sa.getUuid(), sa);
		}
	}
}
