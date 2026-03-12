package com.yfzx.service.chance;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.expression.EqualTo;

import org.apache.commons.lang.StringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.model.chance.ChanceCategory;
import com.stock.common.model.share.TimeLine;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.UtilDate;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.TopicService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.MsgTimeLineService;
import com.yfzx.service.share.ViewpointService;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.core.util.BaseUtil;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.mycore.lcs.enter.LCEnter;

public class ChanceCategoryService {
	private static final Logger logger = LoggerFactory.getLogger(ChanceCategoryService.class);

	private static ChanceCategoryService instance = new ChanceCategoryService();

	private static DBAgent dbAgent = DBAgent.getInstance();

	private ChanceCategoryService() {}

	public static ChanceCategoryService getInstance() {
		return instance;
	}

//	private static Set<Long> cmVpSet = new HashSet<Long>();
	private static Set<Long> essenceVpSet = new HashSet<Long>();
	private static Set<Long> mgVpSet = new HashSet<Long>();
	private static Set<Long> mobileVpNoticeSet = new HashSet<Long>();

	static {
		initVp();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				initVp();
			}
		});
	}

	private static void initVp() {
//		String cmvp = ConfigCenterFactory.getString("stock_zjs.cmvp", "10001,102755,20013");
		String essencevp = ConfigCenterFactory.getString("stock_zjs.essencevp", "20013");
		String mgvp = ConfigCenterFactory.getString("stock_zjs.mgvp", "20013,104600");
		String mobileVpNoticeMembers = ConfigCenterFactory.getString("stock_zjs.mobileVpNoticeMembers", "");

//		cmVpSet.clear();
		essenceVpSet.clear();
		mgVpSet.clear();
		mobileVpNoticeSet.clear();

//		if(StringUtils.isNotBlank(cmvp)) {
//			for(String vp : cmvp.split(",")) {
//				cmVpSet.add(Long.valueOf(vp));
//			}
//		}

		if(StringUtils.isNotBlank(essencevp)) {
			for(String vp : essencevp.split(",")) {
				essenceVpSet.add(Long.valueOf(vp));
			}
		}

		if(StringUtils.isNotBlank(mgvp)) {
			for(String vp : mgvp.split(",")) {
				mgVpSet.add(Long.valueOf(vp));
			}
		}

		if(StringUtils.isNotBlank(mobileVpNoticeMembers)) {
			for(String vp : mobileVpNoticeMembers.split(",")) {
				mobileVpNoticeSet.add(Long.valueOf(vp));
			}
		}
	}

//	public static Set<Long> getCmVpSet() {
//		return cmVpSet;
//	}

	public static Set<Long> getEssenceVpSet() {
		return essenceVpSet;
	}

	public static Set<Long> getMgVpSet() {
		return mgVpSet;
	}

	public static Set<Long> getMobileNoticeVpSet() {
		return mobileVpNoticeSet;
	}

	private static final String NS = "com.stock.portal.dao.chance.ChanceCategoryDao";

	public long insert(ChanceCategory cc) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", cc, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			ChanceCategory obj = (ChanceCategory)rm.getResult();
			return obj.getId();
		}
		return 0;
	}

	public boolean update(ChanceCategory cc) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "update", cc, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean delete(ChanceCategory cc) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "delete", cc, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean deleteByPk(Long id) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "deleteByPrimaryKey", id, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public List<ChanceCategory> getChanceCategoryList(String order, String dir, Integer start, Integer limit) {
		Map<String, Object> m = new HashMap<String, Object>();
		if(StringUtils.isNotBlank(order)) {
			m.put("order", order);
		}
		if(StringUtils.isNotBlank(dir)) {
			m.put("dir", dir);
		}
		m.put("start", start);
		m.put("limit", limit);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getChanceCategoryPageData2Cache", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<ChanceCategory>)rm.getResult();
		} else {
			return null;
		}
	}

	public int getChanceCount() {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getChanceCategoryAllCount", new HashMap<String, Object>(), StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (Integer)rm.getResult();
		} else {
			return 0;
		}
	}

	public boolean isInDb(Integer type, String tag) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("type", type);
		m.put("tag", tag);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "isExist", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (Integer)rm.getResult() > 0;
		} else {
			return false;
		}
	}

	/**
	 * 获取当日产生的机会
	 * ChanceCategory对象
	 * tag:stockcode
	 * insertTime: 机会产生时间
	 * type: MsgConst.MSG_TRADEMSG_TYPE_0 超短线     MsgConst.MSG_TRADEMSG_TYPE_1 短线  MsgConst.MSG_TRADEMSG_TYPE_3 精华
	 * @return
	 */
	public List<Map<String, String>> getStockChanceList() {
		return getStockChanceList(new Date());
	}

	public List<Map<String, String>> getStockChanceList(Date beginTime) {
//		List<ChanceCategory> list = getChanceCategoryList();
		int count = getChanceCount();
		int limit = 1000;
		int num = count % limit == 0 ? count / limit : count / limit +1;
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		for(int i = 0; i < num; i++) {
			List<ChanceCategory> list = getChanceCategoryList("", "", i * limit, limit);

			Date nowBegin = DateUtil.getDayStartTime(beginTime.getTime());
			List<String> firstList = TradeService.getInstance().getDxFirstKeyList();

			Integer ftype = ConfigCenterFactory.getInt("stock_zjs.mock_comment_chance_ftype", 0);
			Integer size = ConfigCenterFactory.getInt("stock_zjs.mock_chance_size", 2000);

			if(list != null && list.size() > 0) {
				for(ChanceCategory cc : list) {
					String tag = cc.getTag();
					String tt = "";
					for(String firstKey : firstList) {
						String key = "";
						String name = "";
						if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_0) {
							tt = StockConstants.CDX;
							key = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS, tt, tag);
							name =  TradeService.getInstance().getCategoryName(StockConstants.SCSEP, firstKey, StockConstants.JS, tt)+StockConstants.SCSEP+TradeService.getInstance().getRealMsgDescByRealType(Integer.valueOf(tag));
						} else if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_1) {
							tt = StockConstants.DX;
							key = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS, tt, tag);
							Tagrule tr = TagruleService.getInstance().getTagruleById(Integer.valueOf(tag));
							if(tr != null) {
								name = TradeService.getInstance().getCategoryName(StockConstants.SCSEP, firstKey, StockConstants.JS, tt)+StockConstants.SCSEP+tr.getTagDesc();
							}
						} else if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_3) {
							key = StockUtil.joinString(StockConstants.UL, (Object[])(firstKey + StockConstants.UL + tag).split(StockConstants.UL));
							name = TradeService.getInstance().getCategoryName(StockConstants.SCSEP, (Object[])(firstKey + StockConstants.UL + tag).split(StockConstants.UL));
						}
	//					int limit = ConfigCenterFactory.getInt("stock_zjs.mock_stock_limit", 50);
	//					List<IMessage> msgList = RemindServiceClient.getInstance().getChanceMessageList(key, ftype, nowBegin.getTime(), limit);
						List<TradeAlarmMsg> tradeAlarmMsgList = ViewpointService.getInstance().listUmFromNosql(key, beginTime.getTime(), 0, size);
						if(tradeAlarmMsgList != null &&  tradeAlarmMsgList.size() > 0) {
							ChanceCategory chance = null;
							for(TradeAlarmMsg msg : tradeAlarmMsgList) {
								if(ftype == 0) {
									if(msg.getTime() <= nowBegin.getTime()) {
										chance = new ChanceCategory();
										chance.setInsertTime(new Date(msg.getTime()));
										Map<String, String> map = new HashMap<String, String>();
										map.put("stockcode", msg.getSourceid());
										map.put("chancekey", key);
										map.put("chancename", name);
										map.put("uuid", msg.getUuid());
									}
								} else if(ftype == 1) {
									if(msg.getTime() >= nowBegin.getTime()) {
										chance = new ChanceCategory();
										chance.setInsertTime(new Date(msg.getTime()));
										Map<String, String> map = new HashMap<String, String>();
										map.put("stockcode", msg.getSourceid());
										map.put("chancekey", key);
										map.put("chancename", name);
										map.put("uuid", msg.getUuid());
									}
								}

								chance = new ChanceCategory();
								chance.setInsertTime(new Date(msg.getTime()));
								Map<String, String> map = new HashMap<String, String>();
								map.put("stockcode", msg.getSourceid());
								map.put("chancekey", key);
								map.put("chancename", name);
								map.put("uuid", msg.getUuid());
	//							chance.setTag(msg.getSourceid() + ";" + key + ";" + name);
	//							chance.setType(msg.getMsgType());
								result.add(map);
							}
						}

					}
				}
			}
		}
		return result;
	}

	//公司是否有消息（lhj）
	private boolean isAdd(String code){
		long muid = ConfigCenterFactory.getLong("stock_zjs.message_uid", 10001L);
		USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(code);
		if(us==null || us.getType()!=0) {
			logger.info("初始化公司消息错误 没有相关公司！ "+code);
			return false;
		}
		long fuid = us.getUid();
		String muidStr = String.valueOf(muid);
		List<TimeLine> tlList = MsgTimeLineService.getInstance().getTimeLineListByTime(muidStr, String.valueOf(fuid),System.currentTimeMillis(),0, 10);
		if(tlList == null || tlList.size() ==0 ){
			return false;
		}
		return true;
	}

	public List<String> getRecommendStocks(int limit) {
		String mock_stock_chance_uidentifys = ConfigCenterFactory.getString("stock_zjs.recommend_stock_chance_uidentifys", "");
		if(StringUtils.isBlank(mock_stock_chance_uidentifys)) {
			return null;
		}

		List<String> uidentifyList = new ArrayList<String>();
		for(String uidentify : mock_stock_chance_uidentifys.split(",")) {
			uidentifyList.add(uidentify);
		}

		List<Map<String, Object>> list = TopicService.getInstance().recommendArticleListSortByTime(uidentifyList, new Date().getTime());

		List<String> result = new ArrayList<String>();
		if(list != null && list.size() > 0) {
			for(Map<String, Object> map : list) {
				String stockcode = map.get("stockcode") == null ? "" : (String)map.get("stockcode");
				if(isAdd(stockcode) && ! result.contains(stockcode)) {
					result.add(stockcode);
				}
			}
		}

		if(result.size() > limit) {
			List<Integer> randomList = getNoRepeatRandomList(0, result.size(), limit);
			List<String> stockList= new ArrayList<String>();
			for(Integer i : randomList) {
				stockList.add(result.get(i));
			}
			return stockList;
		} else {
			return result;
		}
	}

	public List<String> getRecommendStockByChanceCategoryTags(int limit) {
		List<ChanceCategory> list = getChanceCategoryList();
		List<String> stockList = null;
		List<String> firstList = TradeService.getInstance().getDxFirstKeyList();

		if(list != null && list.size() > 0) {
			stockList = new ArrayList<String>();

			List<String> dxList = new ArrayList<String>();
			List<String> cdxList = new ArrayList<String>();
			List<String> jhList = new ArrayList<String>();

			for(ChanceCategory cc : list) {
				String tag = cc.getTag();
				String tt = "";
				String key = "";
				for(String firstKey : firstList) {
					if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_0) {
						tt = StockConstants.CDX;
						key = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS, tt, tag);
						extraMsgList(key, limit, cdxList);
					} else if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_1) {
						tt = StockConstants.DX;
						key = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS, tt, tag);
						extraMsgList(key, limit, dxList);
					} else if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_3) {
						key = StockUtil.joinString(StockConstants.UL, (Object[])(firstKey + StockConstants.UL + tag).split(StockConstants.UL));
						extraMsgList(key, limit,  jhList);
					}
				}
			}

			int cdxStocksSize = 0;
			if(cdxList.size() > 0) {
				cdxStocksSize = limit / 3;
				extractStockList(stockList, cdxList, cdxStocksSize);
			}

			int jhStockSize = 0;
			if(jhList.size() > 0) {
				jhStockSize = limit / 3;
				extractStockList(stockList, jhList, jhStockSize);
			}

			if(dxList.size() > 0) {
				int dxStockSize = limit - cdxStocksSize - jhStockSize;
				if(dxStockSize > 0) {
					extractStockList(stockList, dxList, dxStockSize);
				}
			}
		}
		return stockList;
	}

	private void extraMsgList(String key, int limit, List<String> list) {
		int recommend_stock_magnification = ConfigCenterFactory.getInt("stock_zjs.recommend_stock_magnification", 3);
		List<Map<String, Object>> msgList = RemindServiceClient.getInstance().getLatestNewChanceMessageList(key, limit * recommend_stock_magnification);

		if(msgList != null && msgList.size() > 0) {
			int fetchAll = ConfigCenterFactory.getInt("stock_zjs.fetch_recommend_stock_by_chance", 1);//0:all  1:a股
			for(Map<String, Object> msg : msgList) {
				String stockcode = (String)msg.get("stockcode");
				if(fetchAll == 1 && StringUtils.isNotBlank(stockcode) && (stockcode.endsWith("sz") || stockcode.endsWith("sh"))) {
					if(isAdd(stockcode)) {
						list.add(stockcode);
						if(list.size() > limit * recommend_stock_magnification) {
							break;
						}
					}
				} else if(fetchAll == 0) {
					if(isAdd(stockcode)) {
						list.add(stockcode);
						if(list.size() > limit * recommend_stock_magnification) {
							break;
						}
					}
				}
			}
		}
	}

	private void extractStockList(List<String> stockList,
			List<String> msgList, int cdxStocksSize) {
		List<Integer> randomList = getNoRepeatRandomList(0, msgList.size(), cdxStocksSize);
		if(randomList != null && randomList.size() > 0) {
			for(Integer i : randomList) {
				String stockcode =msgList.get(i);
				if(! stockList.contains(stockcode)) {
					stockList.add(stockcode);
				}
			}
		}
	}

	/**
	 * listSize <= end - from
	 * @param from
	 *
	 * @param end
	 * @param listSize
	 * @return
	 */
	public static List<Integer> getNoRepeatRandomList(int from, int end, int listSize) {
		if(from < 0 || end < 0 || listSize <= 0 || end <= from || end - from < listSize) {
			return null;
		}

		int num = 0;
		List<Integer> list = new ArrayList<Integer>();
		while(num <= listSize - 1) {
			int random = getRandomByRegion(from, end);
			if(! list.contains(random)) {
				num++;
				list.add(random);
			}
		}
		return list;
	}

	/**
	 * from(inclusive) ~ end(exclusive)
	 * @param from
	 * @param end
	 * @return
	 */
	private static int getRandomByRegion(int from, int end) {
        Random random = new Random();
        return random.nextInt(end)%(end - from + 1) + from;
	}

	/**
	 *
	 * @param notifyEvent
	 * @return
	 */
	public boolean saveChanceCategory(TradeAlarmMsg um) {
		try {
			if(um == null) {
				return false;
			}

			String tag = "";
			if(um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_0) {
				tag = String.valueOf(um.getAttr("desc"));
			} else if(um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_1) {
				tag = String.valueOf(um.getEventid());
			} else if(um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_3) {
				Long uid = um.getAttr("uid");
				if(uid == null) {
					logger.error("saveChanceCategory3: uid is null " );
				}
				Map<Long, String> vpMemberMenuMap = MicorBlogService.getInstance().getVpMemberMenu();
				tag = vpMemberMenuMap.get(uid);
				if(StringUtils.isEmpty(tag)) {
					logger.error("saveChanceCategory31: tag is null " );
					return false;
				}
			}

			if(getChanceCategoryFromCache(tag, um.getMsgType()) == null) {
				ChanceCategory cc = new ChanceCategory();
				cc.setT((byte)1);
				cc.setTag(tag);
				cc.setType(um.getMsgType());

				saveChanceCategoryToCache(cc);

				if(! isInDb(um.getMsgType(), tag)) {
					cc.setInsertTime(new Date());
					boolean r = insert(cc) > 0;
					if(! r) {
						logger.error("saveChanceCategoryFailure: " + tag + "   " + um.getMsgType());
					}
				}
			}
		} catch (Exception e) {
			logger.error("saveChanceCategory: " + e);
		}

		return true;
	}

	public ChanceCategory getChanceCategoryFromCache(String tag, int type) {
		String cacheName = StockUtil.getChanceCategoryName();
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		Cache cache = cimpl.getCacheManager().getCache(cacheName);
		Query query = cache.createQuery().includeKeys().includeValues();
		Results results = query.addCriteria(new EqualTo("tag", tag).and(new EqualTo("type", type))).execute();
		List<Result> rsList = results.all();
		if(rsList.size() > 0) {
			return (ChanceCategory)rsList.get(0).getValue();
		}
		return null;
	}

	public void saveChanceCategoryToCache(ChanceCategory cc) {
		if(cc == null) {
			return ;
		}
		String cacheName = StockUtil.getChanceCategoryName();
		LCEnter.getInstance().put(cc.getId(), cc, cacheName);
	}

	public void saveChanceCategoryToList(ChanceCategory cc) {
		try {
			String cacheName = StockUtil.getChanceCategoryTagsName();
			List<ChanceCategory> tagList = LCEnter.getInstance().get(StockConstants.CHANCECATEGORYTAGS, cacheName);
			if(tagList==null)
			{
				tagList = new ArrayList<ChanceCategory>();
				tagList.add(cc);
				LCEnter.getInstance().put(StockConstants.CHANCECATEGORYTAGS, tagList, cacheName);
			} else {
				boolean isIn = false;
				for(ChanceCategory ctag : tagList) {
					if(ctag.getType() == cc.getType() && StringUtils.equals(ctag.getTag(), cc.getTag())) {
						isIn = true;
					}
				}

				if(! isIn) {
					tagList.add(cc);
				}
			}
		} catch (Exception e) {
			logger.error("saveChanceCategoryToList: " + e);
		}
	}

	public List<ChanceCategory> getChanceCategoryList() {
		String cacheName = StockUtil.getChanceCategoryTagsName();
		return LCEnter.getInstance().get(StockConstants.CHANCECATEGORYTAGS, cacheName);
	}

	public List<String> getKeyListByNotifyEvent(TradeAlarmMsg um) {
		if(um == null) {
			return null;
		}

		String key = "";
		if(um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_0) {
			List<String> list = new ArrayList<String>();
			String firstKey = TradeService.getInstance().getFirstByType(um.getSourceid());
			key = StockUtil.joinString(StockConstants.UL, firstKey,StockConstants.JS,StockConstants.CDX,um.getAttr("desc"));
			list.add(key);
			key = StockUtil.joinString(StockConstants.UL, firstKey,StockConstants.JS,StockConstants.CDX);
			list.add(key);
			key = StockUtil.joinString(StockConstants.UL, firstKey,StockConstants.JS,StockConstants.CDX);
			list.add(key);
			list.add(firstKey);
			return list;
		} else if(um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_1) {
			List<String> list = new ArrayList<String>();
			String firstKey = TradeService.getInstance().getFirstByType(um.getSourceid());
			key = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS,StockConstants.DX, um.getEventid());
			list.add(key);
			key = StockUtil.joinString(StockConstants.UL, firstKey,StockConstants.JS,StockConstants.DX);
			list.add(key);
			key = StockUtil.joinString(StockConstants.UL, firstKey,StockConstants.JS);
			list.add(key);
			list.add(firstKey);
			return list;
		} else if(um.getMsgType() == MsgConst.MSG_TRADEMSG_TYPE_3) {
			Long uid = um.getAttr("uid");
			String menu = "";
			String chancetag = um.getAttr(StockConstants.CHANCETAG);
			if(StringUtils.isNotBlank(chancetag)) {
				menu = chancetag;
			} else {
				if(uid != null) {
					Map<Long, String> vpMemberMenuMap = MicorBlogService.getInstance().getVpMemberMenu();
					menu = vpMemberMenuMap.get(uid);
					if(StringUtils.isBlank(menu)) {
						logger.error("getKeyListByNotifyEvent31: menu is null===> " + uid);
						return null;
					}
				} else {
					logger.error("getKeyListByNotifyEvent3: uid is null===> ");
					menu = ConfigCenterFactory.getString("stock_zjs.essence_chance_tag", "essence_wvp_ecvp");
				}
			}

			String firstKey = TradeService.getInstance().getFirstByType(um.getSourceid());
			Object[] arr = (firstKey + StockConstants.UL + menu).split(StockConstants.UL);
			int len = arr.length;
			List<String> list = new ArrayList<String>();

			for(int i = 0; i < len; i++) {
				if(i == 0) {
					list.add(StockUtil.joinString(StockConstants.UL, arr));
				} else if(i > 0 && i < len - 1) {
					Object[] tempArr = new Object[len - i];
					for(int j = 0; j < len - i; j++) {
						tempArr[j] = arr[j];
					}
					list.add(StockUtil.joinString(StockConstants.UL, tempArr));
				} else if(i == len - 1) {
					list.add(firstKey);
				}
			}
			return list;
		}
		return null;
	}

	/**
	 * 清理昨日过期的分时图
	 */
	public void cleanYesterdayFenshiChance() {
		logger.info("cleanYesterdayFenshiChance: " + UtilDate.getDateFormatter());
		if(ConfigCenterFactory.getInt("stock_zjs.cleanYesterdayFenshiChance", 0) == 1) {
			return ;
		}

		int chanceCount = getChanceCount();
		int pageSize = 100;
		int num = chanceCount % pageSize == 0 ? chanceCount / pageSize : chanceCount / pageSize + 1;
		List<String> firstList = TradeService.getInstance().getDxFirstKeyList();
		for(int i = 0; i < num; i++) {
			int offset = i * pageSize;
			List<ChanceCategory> list = getChanceCategoryList("", "", offset, pageSize);

			if(list != null && list.size() > 0) {
				for(ChanceCategory cc : list) {
					if(cc == null) {
						continue;
					}
					String tag = cc.getTag();
					String tt = "";

					if(firstList != null && firstList.size() > 0) {
						for(String firstKey : firstList) {
							if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_0) {
								tt = StockConstants.CDX;

								String key1 = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS, tt, tag);
								RemindServiceClient.getInstance().removeChanceWrapper(key1);
								RemindServiceClient.getInstance().deleteChancesCategoryByKey(StockConstants.SCC_ENTITY, key1);

								String key2 = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS, tt);
								RemindServiceClient.getInstance().removeChanceWrapper(key2);
								RemindServiceClient.getInstance().deleteChancesCategoryByKey(StockConstants.SCC_ENTITY, key2);

								String key3 = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS);
								RemindServiceClient.getInstance().removeChanceWrapper(key3);
								RemindServiceClient.getInstance().deleteChancesCategoryByKey(StockConstants.SCC_ENTITY, key3);

								String key4 = firstKey;
								RemindServiceClient.getInstance().removeChanceWrapper(key4);
								RemindServiceClient.getInstance().deleteChancesCategoryByKey(StockConstants.SCC_ENTITY, key4);

							}
						}
					}

				}
			}

		}
	}

	public void cleanChanceCategoryChanceWrapper() {
		int chanceCount = ChanceCategoryService.getInstance().getChanceCount();
		int pageSize = 100;
		int num = chanceCount % pageSize == 0 ? chanceCount / pageSize : chanceCount / pageSize + 1;
		List<String> firstList = TradeService.getInstance().getDxFirstKeyList();
		TradeService.getInstance().clearChanceCategory();
		for(int i = 0; i < num; i++) {
			int offset = i * pageSize;
			List<ChanceCategory> list = ChanceCategoryService.getInstance().getChanceCategoryList("", "", offset, pageSize);

			if(list != null && list.size() > 0) {
				for(ChanceCategory cc : list) {
					if(cc == null) {
						continue;
					}

					String tag = cc.getTag();
					String tt = "";

					if(firstList != null && firstList.size() > 0) {
						for(String firstKey : firstList) {
							if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_1) {
								tt = StockConstants.DX;
								Tagrule tr = TagruleService.getInstance().getTagruleById(Integer.valueOf(tag));
								if(tr == null) {
									continue;
								}
								cleanChanceWrapper(firstKey, tag, tt, cc.getType());
							} else if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_0) {
								tt = StockConstants.CDX;
								cleanChanceWrapper(firstKey, tag, tt, cc.getType());
							} else if(cc.getType() == MsgConst.MSG_TRADEMSG_TYPE_3) {
								cleanChanceWrapper(firstKey, tag, tt, cc.getType());
							}
						}
					}
				}
			}
		}
	}

	private void cleanChanceWrapper(String firstKey, String tag, String tt, int type) {
		if(type == MsgConst.MSG_TRADEMSG_TYPE_0 || type == MsgConst.MSG_TRADEMSG_TYPE_1) {
			String key1 = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS, tt, tag);
			String key2 = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS, tt);
			String key3 = StockUtil.joinString(StockConstants.UL, firstKey, StockConstants.JS);
			String key4 = firstKey;

			RemindServiceClient.getInstance().removeChanceWrapper(key1);
			RemindServiceClient.getInstance().removeChanceWrapper(key2);
			RemindServiceClient.getInstance().removeChanceWrapper(key3);
			RemindServiceClient.getInstance().removeChanceWrapper(key4);
		} else if(type == MsgConst.MSG_TRADEMSG_TYPE_3) {
			Object[] arr = (firstKey + StockConstants.UL + tag).split(StockConstants.UL);
			int len = arr.length;

			for(int i = 0; i < len; i++) {
				if(i == 0) {
					RemindServiceClient.getInstance().removeChanceWrapper(StockUtil.joinString(StockConstants.UL, arr));
				} else if(i > 0 && i < len - 1) {
					Object[] tempArr = new String[len - i];
					for(int j = 0; j < len - i; j++) {
						tempArr[j] = arr[j];
					}
					RemindServiceClient.getInstance().removeChanceWrapper(StockUtil.joinString(StockConstants.UL, tempArr));
				} else if(i == len - 1) {
					RemindServiceClient.getInstance().removeChanceWrapper(firstKey);
				}
			}
		}
	}

	public boolean saveEssenceUuidsToCache(String uuid) {
		boolean result = false;
		try {
			Set<String> set = LCEnter.getInstance().get(StockConstants.ESSENCECHANCEUUIDS, StockConstants.ESSENCECACHE);
			if(set == null) {
				set = new HashSet<String>();
				result = set.add(uuid);
				LCEnter.getInstance().put(StockConstants.ESSENCECHANCEUUIDS, set, StockConstants.ESSENCECACHE);
			} else {
				result = set.add(uuid);
				if(set.size() > ConfigCenterFactory.getInt("stock_zjs.essenceMaxCount", 3000)) {
					set.remove(set.size() - 1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}
	private static final String esssence_chance = "esssence_chance";
	private static final String essence = "essence";
	public void saveEssenceUuidsToDisk(String uuid) {
		try {
			logger.info("saveEssenceUuidsToDisk: " + UtilDate.getDateFormatter());
			String path = BaseUtil.getAppRootWithAppName() + File.separator + esssence_chance;
			File dirFile = new File(path);
			if(! dirFile.exists()) {
				dirFile.mkdirs();
			}
			String essenceFileName = path + File.separator + essence;
			File essenceFile = new File(essenceFileName);
			if(! essenceFile.exists()) {
				essenceFile.createNewFile();
			}

			DB db = DBMaker.newFileDB(essenceFile).make();
			Set<String> set = db.getHashSet(essence);
			Set<String> cacheSet = LCEnter.getInstance().get(StockConstants.ESSENCECHANCEUUIDS, StockConstants.ESSENCECACHE);
			if(cacheSet != null && set !=null) {
				if(StringUtils.isNotBlank(uuid)) {
					cacheSet.remove(uuid);
				}
				set.clear();
				set.addAll(cacheSet);
			}
			db.commit();
			db.close();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}

	public void recoveryEssenceChanceUuidsToCache() {
		try {
			logger.info("recoveryEssenceChanceUuidsToCache: " + UtilDate.getDateFormatter());
			String path = BaseUtil.getAppRootWithAppName() + File.separator + esssence_chance;
			File dirFile = new File(path);
			if(! dirFile.exists()) {
				dirFile.mkdirs();
			}
			String essenceFileName = path + File.separator + essence;
			File essenceFile = new File(essenceFileName);
			if(essenceFile.exists()) {
				DB db = DBMaker.newFileDB(essenceFile).make();
				Set<String> set = db.getHashSet(essence);
				logger.info("essenceSize: " + set.size());
				if(set != null && set.size() > 0) {
					Set<String> tset = new HashSet<String>();
					tset.addAll(set);
					LCEnter.getInstance().remove(StockConstants.ESSENCECHANCEUUIDS, StockConstants.ESSENCECACHE);
					LCEnter.getInstance().put(StockConstants.ESSENCECHANCEUUIDS, tset, StockConstants.ESSENCECACHE);
				}
				db.close();
			} else {
				logger.error("recoveryEssenceChanceUuidsToCache: no file");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean existInEssenceUuids(String uuid) {
		boolean result = false;
		try {
			Set<String> set = LCEnter.getInstance().get(StockConstants.ESSENCECHANCEUUIDS, StockConstants.ESSENCECACHE);
			if(set == null) {
				result = false;
			} else {
				result = set.contains(uuid);
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}

}
