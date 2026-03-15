package com.yfzx.service.stockgame;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.ShareConst;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Trade0001;
import com.stock.common.model.USubject;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.UserExt;
import com.stock.common.model.stockgame.StockDelegation;
import com.stock.common.model.stockgame.StockEquity;
import com.stock.common.model.stockgame.StockRevenue;
import com.stock.common.model.stockgame.StockSnapshoot;
import com.stock.common.model.stockgame.StockTransaction;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.SqlWithTrasitionUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.DcssTradeIndexServiceClient;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.user.UserStockService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.TalkMessageService;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.msg.UsubjectEventService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yfzx.service.trade.TradeCenter;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class StockDelegationService {

	private static final Logger logger = LoggerFactory.getLogger(StockDelegationService.class);

	private static final String NS = "com.stock.portal.dao.stockgame.StockDelegationDao";

	private StockDelegationService(){}

	private static StockDelegationService instance = new StockDelegationService();

	private static DBAgent dbAgent = DBAgent.getInstance();

	public static StockDelegationService getInstance() {
		return instance;
	}

	public long insert(StockDelegation stockDelegation) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", stockDelegation, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			StockDelegation obj = (StockDelegation)rm.getResult();
			return obj.getId();
		}
		return 0;
	}

	public boolean update(StockDelegation stockDelegation) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "update", stockDelegation, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean delete(StockDelegation stockDelegation) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "delete", stockDelegation, StockConstants.common);
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

	public List<StockDelegation> getStockDelegationList(StockDelegation stockDelegation) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "select", stockDelegation, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockDelegation>)rm.getResult();
		} else {
			return null;
		}
	}

	public Integer getCount(StockDelegation stockDelegation) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectCount", stockDelegation, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (Integer)rm.getResult();
		}
		return 0;
	}

	public List<StockDelegation> getStockDelegationListByPage(Integer offset, Integer limit, Long uid) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("offset", offset);
		map.put("limit", limit);
		map.put("uid", uid);
//		map.put("sort", "");
//		map.put("direction", "");
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectList", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockDelegation>)rm.getResult();
		} else {
			return null;
		}
	}

	public boolean dealSuccessStockDelegation(StockDelegation sd) {
		if(sd == null) {
			return false;
		}

		int tradeStatus = TradeCenter.getInstance().getStockTradeStatus(StockUtil.checkStockcode(sd.getStockCode()));
		int tradeSwitch = ConfigCenterFactory.getInt("stock_zjs.stock_game_trade_switch", 1);
		if(tradeSwitch == 1 && tradeStatus == 0) {//休市
			return false;
		}
		Company company = CompanyService.getInstance().getCompanyByCode(sd.getStockCode());//停牌
		if(company == null || CompanyService.getInstance().isStop(company)) {
			return false;
		}
		String leaveStocks = ConfigCenterFactory.getString("stock_zjs.delisting_stocks", "");
		if(leaveStocks.contains(sd.getStockCode())) {
			return false;//退市
		}

//		StockRevenue sr = StockRevenueService.getInstance().getStockRevenueByUid(sd.getUid());
		StockRevenue sr = UserServiceClient.getInstance().getStockRevenueByUid(sd.getUid());
		if(sr == null) {
			return false;
		}
		Company st = CompanyService.getInstance().getCompanyByCode(sd.getStockCode());//停牌
		String unit = StockUtil.getStockPriceUnit(sd.getStockCode());

		Double brokerageFee = ConfigCenterFactory.getDouble("stock_zjs.brokerageFee", 5D);
		Double hkd_to_rmb = ConfigCenterFactory.getDouble("stock_zjs.hkd_to_rmb", 0.7920D);//港元兑人民币
		Double dollar_to_rmb = ConfigCenterFactory.getDouble("stock_zjs.dollar_to_rmb", 6.1383D);//美元兑人民币
		int stockType = StockUtil.checkStockcode(sd.getStockCode());
		Double ratio = 1D;
		String showUnit = "￥";
		if(stockType == 1) {
			showUnit = "HKD";
			ratio = hkd_to_rmb;
		} else if(stockType == 2) {
			showUnit = "$";
			ratio = dollar_to_rmb;
		}

		UserServiceClient stockCache = UserServiceClient.getInstance();

		if(sd.getBargainType() == 0) {
			sd.setDelegationPrice(company.getC());

			if((sd.getDelegationCount() * sd.getDelegationPrice() + brokerageFee) * ratio > sr.getBalance()) {
				return false;
			}

			StockTransaction tran = new StockTransaction();
			tran.setStockCode(sd.getStockCode());
//				tran.setCostPrice(0d);
			tran.setBargainPrice(sd.getDelegationPrice());
			tran.setBargainCount(sd.getDelegationCount());
			tran.setBargainType(sd.getBargainType());// 0:买入  1:卖出
			tran.setBrokerageFee(brokerageFee);
			tran.setSoldOut((byte)0);
//			tran.setRevenuefee(bargainCount * st.getC() * revenueFeeRatio);//印花税
//			tran.setBargainfee(bargainCount * st.getC() * bargainFeeRatio);//过户费
//			Double brokerageFee = bargainCount * st.getC() * brokerageFeeRatio;
//			if(brokerageFee < 5) {
//				brokerageFee = 5d;
//			}
//			Double brokerageFee = 5d;
//			tran.setBrokeragefee(brokerageFee);//佣金
			tran.setBargainTime(new Date());
			tran.setUid(sd.getUid());

			Double balance = 0d;
			Double bargainMoney = sd.getDelegationPrice() * sd.getDelegationCount();
			Double totalCost = bargainMoney + brokerageFee;
			if(stockType == 0) {
				balance = sr.getBalance() - sd.getDelegationPrice() * sd.getDelegationCount() - brokerageFee;
			} else if(stockType == 1) {
				balance = sr.getBalance() - (sd.getDelegationPrice() * sd.getDelegationCount() + brokerageFee) * hkd_to_rmb;
			} else if(stockType == 2) {
				balance = sr.getBalance() - (sd.getDelegationPrice() * sd.getDelegationCount() + brokerageFee) * dollar_to_rmb;
			}
			if(stockType == 0) {
				tran.setT((byte)1);
			} else {
				tran.setT((byte)0);
			}

			StockEquity stockEquity = UserServiceClient.getInstance().getStockEquityByCodeUid(sd.getUid(), sd.getStockCode());
			if(stockEquity == null) {
				stockEquity = StockEquityService.getInstance().getStockEquityByCodeUid(sd.getStockCode(), sd.getUid());
				if(stockEquity != null) {
					UserServiceClient.getInstance().saveStockEquity(sd.getUid(), stockEquity);
				}
			}
			boolean update = false;
			//更新持仓记录
			if(stockEquity == null) {
				stockEquity = new StockEquity();
				stockEquity.setStockCode(sd.getStockCode());
				stockEquity.setEquityCount(sd.getDelegationCount());//持股数
				stockEquity.setCostPrice(totalCost / sd.getDelegationCount());//成本价
				if(stockType == 0) {
					stockEquity.setVendibilityCount(0);//T+1(可卖数量) 单位：手（1手=100股）
				} else {
					stockEquity.setVendibilityCount(sd.getDelegationCount());//港股、美股
				}
				stockEquity.setTotalCost(totalCost);
				stockEquity.setBargainMoney(bargainMoney);
				stockEquity.setUid(sd.getUid());
				stockEquity.setUuid(sd.getUuid());
			} else {
				stockEquity.setStockCode(sd.getStockCode());
				int equitCount = stockEquity.getEquityCount() + sd.getDelegationCount();
				stockEquity.setEquityCount(equitCount);//持股数
				if(stockType != 0) {
					stockEquity.setVendibilityCount(equitCount);//港股、美股
				}
				Double costPrice = (stockEquity.getTotalCost() + totalCost) / stockEquity.getEquityCount();
				stockEquity.setCostPrice(costPrice);
				stockEquity.setTotalCost(stockEquity.getTotalCost() + totalCost);
				stockEquity.setBargainMoney(stockEquity.getBargainMoney() + bargainMoney);
//				boolean result = StockEquityService.getInstance().update(stockEquity);
				update = true;
			}

			//更新余额
			sr.setBalance(balance);
			sr.setStatus((byte)1);
			sr.setOrderTime(new Date());
//			boolean result = stockRevenueService.update(stockRevenue);
			Map<String,Object> sqlMap = new LinkedHashMap<String,Object>();
			sqlMap.put("com.stock.portal.dao.stockgame.StockTransactionDao.insert", tran);
			if(update) {
				sqlMap.put("com.stock.portal.dao.stockgame.StockEquityDao.updateByPrimaryKey", stockEquity);
			} else {
				sqlMap.put("com.stock.portal.dao.stockgame.StockEquityDao.insert", stockEquity);
			}
			sqlMap.put("com.stock.portal.dao.stockgame.StockRevenueDao.updateByPrimaryKey", sr);
			sqlMap.put("com.stock.portal.dao.stockgame.StockDelegationDao.deleteByPrimaryKey", sd.getId());
			if(! SqlWithTrasitionUtil.executSqlWithTrasition(sqlMap)){
				logger.error("DelegationPriceBuy: StockDelegationID: " + sd.getId() + " time: " + getCurrentDate());
				return false;
			} else {
//				USubject uSubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(sd.getStockCode());
//				if(uSubject != null) {//您委托买入的1000股【万科(000002.sz)】在2014-10-11 11:23:33交易成功。
//					UserEventService.getInstance().singleCastTalkMessage(uSubject.getUid(), sd.getUid(), "您委托买入的" + sd.getDelegationCount() + "股【" + company.getSimpileName() + "(" + company.getCompanyCode() + ")】在" + getCurrentDate() + "交易成功，委托价格是：" + sd.getDelegationPrice() + showUnit + "。", null);
//				}
				Long sscUid = ConfigCenterFactory.getLong("stock_zjs.secretaire", 0L);
				if(sscUid != 0) {
					//UserEventService.getInstance().singleCastTalkMessage(sscUid, sd.getUid(), "您委托买入的" + sd.getDelegationCount() + "股【" + company.getSimpileName() + "(" + company.getCompanyCode() + ")】在" + getCurrentDate() + "交易成功，委托价格是：" + sd.getDelegationPrice() + showUnit + "。", null);
					TalkMessageService.getInstance().singlecastTalkMessage(sscUid, sd.getUid(), "您委托买入的" + sd.getDelegationCount() + "股【" + company.getSimpileName() + "(" + company.getCompanyCode() + ")】在" + getCurrentDate() + "交易成功，委托价格是：" + sd.getDelegationPrice() + showUnit + "。", null);
				}

				//update cache
				stockCache.saveStockEquity(stockEquity.getUid(), stockEquity);
				stockCache.saveStockRevenue(sr.getUid(), sr);
				stockCache.saveStockTransaction(tran.getUid(), tran);

				UserStockService.getInstance().addStock(stockEquity.getStockCode(),stockEquity.getUid());

				UserServiceClient.getInstance().removeStockDelegation(sd.getUid(), sd.getId());

				if(hasPublishStockGameVp(sd.getUid())) {
					if(update == false) {
						String title = sd.getDelegationPrice() + unit + "委托买入" + st.getSimpileName() + "(" +st.getCompanyCode() + ")" + sd.getDelegationCount() + "股";
						publishStockGameViewPoint(true,title, sd.getAdjustReason(), sd.getStockCode(),  st.getC(), sd.getUid(), sd.getUuid(), sd.getDelegationCount());
					} else {
						String seuuid =  stockEquity.getUuid();
						if(StringUtils.isNotBlank(seuuid)) {
							TradeService.getInstance().updateStockChanceViewpoint(seuuid,  stockEquity.getEquityCount());
						} else {
							logger.error("DelegationPriceUpdateStockChanceViewpointInStockGame1: " + stockEquity.getId());
						}
					}
				}else{
					if(update == false) {
						String title = sd.getDelegationPrice() + unit + "委托买入" + st.getSimpileName() + "(" +st.getCompanyCode() + ")" + sd.getDelegationCount() + "股";
						publishStockGameViewPoint(false,title, sd.getAdjustReason(), sd.getStockCode(),  st.getC(), sd.getUid(), sd.getUuid(), sd.getDelegationCount());
					}
				}
				return true;
			}
		} else if(sd.getBargainType() == 1) {
			sd.setDelegationPrice(company.getC());

			StockEquity stockEquity = UserServiceClient.getInstance().getStockEquityByCodeUid(sd.getUid(), sd.getStockCode());
			if(stockEquity == null) {
				stockEquity = StockEquityService.getInstance().getStockEquityByCodeUid(sd.getStockCode(), sd.getUid());
				if(stockEquity != null) {
					UserServiceClient.getInstance().saveStockEquity(sd.getUid(), stockEquity);
				}
			}
			if(stockEquity == null) {
				return false;
			}

			if((stockType == 0 && (stockEquity.getEquityCount() < sd.getDelegationCount() || stockEquity.getVendibilityCount()< sd.getDelegationCount())) || //A、B股卖出股票大于可卖数量
					((stockType == 1 || stockType == 2) && (sd.getDelegationCount() > stockEquity.getVendibilityCount() || sd.getDelegationCount() > stockEquity.getEquityCount()))) {
				return false;
			}

			StockTransaction tran = new StockTransaction();
			tran.setStockCode(sd.getStockCode());
			tran.setCostPrice(stockEquity.getCostPrice());
			tran.setBargainPrice(sd.getDelegationPrice());
			tran.setBargainCount(sd.getDelegationCount());
			tran.setBargainType(sd.getBargainType());// 0:买入  1:卖出
			tran.setBrokerageFee(brokerageFee);
			tran.setSoldOut((byte)0);
//			tran.setRevenuefee(bargainCount *  revenueFeeRatio);//印花税
//			tran.setBargainfee(bargainCount *  bargainFeeRatio);//过户费 0
//			Double brokerageFee = bargainCount *  brokerageFeeRatio;
//			if(brokerageFee < 5) {
//				brokerageFee = 5d;
//			}
//			tran.setBrokeragefee(brokerageFee);//佣金
			tran.setBargainTime(new Date());
			tran.setUid(sd.getUid());
			Double fee = sd.getDelegationPrice() * sd.getDelegationCount();
			Double balance = 0D;
//			Double fee = tran.getBargainfee() + tran.getRevenuefee() + tran.getBrokeragefee(); //A股
			if(stockType == 0) {
				balance = sr.getBalance() + fee - brokerageFee;
			} else if(stockType == 1) {
				balance = sr.getBalance() + (fee - brokerageFee) * hkd_to_rmb;
			} else if(stockType == 2) {
				balance = sr.getBalance() + (fee - brokerageFee) * dollar_to_rmb;
			}
//			Long id = StockTransactionService.getInstance().insert(tran);

			//更新持仓记录
			boolean isSoldOut = stockEquity.getVendibilityCount() == sd.getDelegationCount() && stockEquity.getEquityCount() == sd.getDelegationCount();//是否清仓

			boolean delete = false;
			if(! isSoldOut) {//未清仓，计算持股成本
				Double totalPrice = stockEquity.getTotalCost() - (sd.getDelegationPrice() - stockEquity.getCostPrice()) * sd.getDelegationCount() - stockEquity.getCostPrice() * sd.getDelegationCount() + brokerageFee;
				Double costPrice = totalPrice / (stockEquity.getEquityCount() - sd.getDelegationCount());
				stockEquity.setCostPrice(costPrice);
				stockEquity.setVendibilityCount(stockEquity.getVendibilityCount() - sd.getDelegationCount());//可卖数量
				stockEquity.setTotalCost(totalPrice);//总成本
				stockEquity.setBargainMoney(stockEquity.getBargainMoney() - sd.getDelegationCount() * stockEquity.getCostPrice());//交易金额
				stockEquity.setEquityCount(stockEquity.getEquityCount() - sd.getDelegationCount());//持股数
//				boolean result = StockEquityService.getInstance().update(stockEquity);
			} else {
				delete = true;
//				boolean result = StockEquityService.getInstance().deleteByPk(stockEquity.getId());
			}

			//更新余额
			sr.setBalance(balance);
			sr.setStatus((byte)1);
//			boolean result = stockRevenueService.update(stockRevenue);
			Map<String,Object> sqlMap = new LinkedHashMap<String,Object>();
			sqlMap.put("com.stock.portal.dao.stockgame.StockTransactionDao.insert", tran);
			if(delete) {
				sqlMap.put("com.stock.portal.dao.stockgame.StockEquityDao.deleteByPrimaryKey", stockEquity.getId());

//				Map<String, Object> soldOutMap = new HashMap<String, Object>();
//				soldOutMap.put("stockCode", "'" + sd.getStockCode() + "'");
//				soldOutMap.put("bargainType", 1);
//				soldOutMap.put("bargainTime", "'" + getDateFormatter(new Date()) + "'");
//				soldOutMap.put("uid", sd.getUid());
//				soldOutMap.put("soldOut", 0);
//				sqlMap.put("com.stock.portal.dao.stockgame.StockTransactionDao.updateSoldOutStat", soldOutMap);
			} else {
				sqlMap.put("com.stock.portal.dao.stockgame.StockEquityDao.updateByPrimaryKey", stockEquity);
			}
			sqlMap.put("com.stock.portal.dao.stockgame.StockRevenueDao.updateByPrimaryKey", sr);
			sqlMap.put("com.stock.portal.dao.stockgame.StockDelegationDao.deleteByPrimaryKey", sd.getId());
			if(! SqlWithTrasitionUtil.executSqlWithTrasition(sqlMap)){
				logger.error("DelegationPriceSold: StockDelegationID: " + sd.getId() + " time: " + getCurrentDate());
				return false;
			} else {
//				USubject uSubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(sd.getStockCode());
//				if(uSubject != null) {
//					UserEventService.getInstance().singleCastTalkMessage(uSubject.getUid(), sd.getUid(), "您委托卖出的" + sd.getDelegationCount() + "股【" + company.getSimpileName() + "(" + company.getCompanyCode() + ")】在" + getCurrentDate() + "交易成功，委托价格是：" + sd.getDelegationPrice() + showUnit + "。", null);
//				}
				Long sscUid = ConfigCenterFactory.getLong("stock_zjs.secretaire", 0L);
				if(sscUid != 0) {
					UserEventService.getInstance().singleCastTalkMessage(sscUid, sd.getUid(), "您委托卖出的" + sd.getDelegationCount() + "股【" + company.getSimpileName() + "(" + company.getCompanyCode() + ")】在" + getCurrentDate() + "交易成功，委托价格是：" + sd.getDelegationPrice() + showUnit + "。", null);
				}

				//update cache
				stockCache.saveStockRevenue(sr.getUid(), sr);
				stockCache.saveStockTransaction(tran.getUid(), tran);
				if(delete) {
					//updateStockChanceArticle
					stockCache.delStockEquity(stockEquity.getUid(), stockEquity);
				} else {
					stockCache.saveStockEquity(stockEquity.getUid(), stockEquity);
				}
				String seuuid =  stockEquity.getUuid();
				if(StringUtils.isNotBlank(seuuid)) {
					int count = 0;
					if(! delete) {
						count = stockEquity.getEquityCount();
					}
					TradeService.getInstance().updateStockChanceViewpoint(seuuid, count);
				} else {
					logger.error("DelegationNowPriceUpdateStockChanceViewpointInStockGame: " + stockEquity.getId());
				}
				UserStockService.getInstance().addStock(stockEquity.getStockCode(),stockEquity.getUid());

				UserServiceClient.getInstance().removeStockDelegation(sd.getUid(), sd.getId());

				return true;
			}
		}

		return false;
	}

	/**
	 * @param isVpGamer 是否是高手
	 * @param title
	 * @param content
	 * @param stockcode
	 * @param price
	 * @param uid
	 * @param uuid
	 * @param count
	 */
	public void publishStockGameViewPoint(boolean isVpGamer,String title, String content, String stockcode, double price, Long uid, String uuid, int count) {
		String nickname = "";
		UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(uid);
		if(userExt != null) {
			nickname = userExt.getNickname();
		}
		if(StringUtils.isBlank(nickname)) {
			logger.error("sendStockGameViewPoint nicknme is null: " + uid);
			return ;
		}

		Article article = new Article();
		article.setType(ShareConst.VIEWPOINT);
		article.setKeywordLevel("1");
		article.setSystem_category("1");
		article.setContent(content);
		article.setTitle(title);
		article.setBlog_category("1");
		article.setUid(uid);
		article.setNick(nickname);
		article.putAttr(StockConstants.EQUITY_COUNT, count);
		article.putAttr("price", price);

		article.setUuid(uuid);
		long t = Calendar.getInstance().getTimeInMillis();
		article.setTime(t);
		
		StringUtil.sbuArticle(article);
//		SquareMsgCache.getInstance().putMsg(article);
		
		boolean publishResult = false;
		if(isVpGamer ==true){
			//发到高手调仓去
			String tracetrade_adjusttrade = ConfigCenterFactory.getString("stock_zjs.stockgame_tracetrade_adjusttrade", "高手调仓");
			article.setTags(stockcode + "," + tracetrade_adjusttrade);			
			publishResult = TradeService.getInstance().publishStockChance(article);			
		}else{
			//非高手调仓处理流程
			//发布博文 tag=公司code"600773.sh:西藏城投" 和 "XX的投资"
			//挂到用户的投资Topic，如"闵晖的投资"
			//挂到用户的Viewpoint
			USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(stockcode);
			USubject us2 = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(nickname+"的投资");
			StringBuilder tagBuf = new StringBuilder();
			if(us != null){
				tagBuf.append(us.getUidentify()).append(':').append(us.getName());
				if(us2 != null){
					tagBuf.append(',');
				}
			}
			if(us2 != null){
				tagBuf.append(us2.getUidentify());
			}else{
				logger.error(nickname+"的投资不存在");
				return;
			}
			String tags = tagBuf.toString();			
			article.setTags(tags);
			//article.putAttr("text", us2.getUidentify());
			//article.putAttr(StockConstants.CHANCETAG, us2.getUidentify());
			String resultStr = MicorBlogService.getInstance().publishArticle(article);
			if(StockCodes.SUCCESS.equals(resultStr)){
				publishResult = true;
			}
		}
		if(publishResult == true){
			TimeLineService.getInstance().saveTimeLine(String.valueOf(uid), uuid, SAVE_TABLE.VIEWPOINT, t);
			
			//发送观点通知，推到其它用户的"我的订阅"【这块需要在DCSS分发 and 加载我的订阅中加载】
			UserMsg um = SMsgFactory.getBrodCastUserMsgByType(MsgConst.MSG_USUBJECT_TYPE_1);
			um.setS(String.valueOf(uid));
			um.setD(String.valueOf(uid));
			um.putAttr("uuid", uuid);
			um.putAttr("uid", String.valueOf(uid));
			um.setTime(t);			
			UsubjectEventService.getInstance().notifyTheEvent(um);
		}
	}

	/**
	 * 初始化模拟炒股账户
	 * @param uid
	 * @param extra
	 * @return
	 */
	public StockRevenue initStockRevenue(Long uid, Integer extra) {
		StockRevenue cacheRevenue = UserServiceClient.getInstance().getStockRevenueByUid(uid);
		if(cacheRevenue == null) {
			StockRevenue dbSr = StockRevenueService.getInstance().getStockRevenueByUid(uid);
			if(dbSr == null) {
				StockRevenue sr = new StockRevenue();
				sr.setUid(uid);
				sr.setRevenueRatio(0d);
				sr.setFinance(StockConstants.INITIAL_FINANCE + extra);
				sr.setBalance(StockConstants.INITIAL_FINANCE + extra);
				sr.setMarketVal(0D);
				sr.setRank(0);
				long id = StockRevenueService.getInstance().insert(sr);

				StockSnapshoot shoot = new StockSnapshoot();
				shoot.setUid(uid);
				double fund = StockConstants.INITIAL_FINANCE + extra;
				shoot.setDailyStartFund(fund);
				shoot.setWeekStartFund(fund);
				shoot.setMonthStartFund(fund);
				shoot.setYearStartFund(fund);
				StockSnapshootService.getInstance().insert(shoot);

				if(id > 0) {
					sr.setId(id);
					UserServiceClient.getInstance().saveStockRevenue(sr.getUid(), sr);
					return sr;
				}
			} else {
				UserServiceClient.getInstance().saveStockRevenue(dbSr.getUid(), dbSr);
				return dbSr;
			}
		}
		return cacheRevenue;
	}

	public double getProfitByTypeUid(int type, StockRevenue sr) {
		if(sr == null) {
			return 0;
		}
		StockSnapshoot st = UserServiceClient.getInstance().getStockSnapshootByUid(sr.getUid());
		if(st == null) {
			return 0;
		}
		double balance = sr.getBalance();
		double marketVal = sr.getMarketVal();
		double finance = sr.getFinance();
		if(type == StockConstants.DAILY_SR_TYPE) {
			return marketVal + balance - st.getDailyStartFund();
		} else if(type == StockConstants.WEEK_SR_TYPE) {
			return marketVal + balance - st.getWeekStartFund();
		} else if(type == StockConstants.MONTH_SR_TYPE) {
			return marketVal + balance - st.getMonthStartFund();
		} else if(type == StockConstants.YEAR_SR_TYPE) {
			return marketVal + balance - st.getYearStartFund();
		} else if(type == StockConstants.TOTAL_SR_TYPE) {
			return marketVal + balance - finance;
		}
		return 0;
	}

	public double getMarketVals(Long uid) {
		double total = 0d;
		List<StockEquity> eqList = UserServiceClient.getInstance().getStockEquityPagingList(uid, 0, -1);
		if(eqList != null && eqList.size() > 0) {
			for(StockEquity se : eqList) {
				Double c = 0d;
				Company com = CompanyService.getInstance().getCompanyByCodeFromCache(se.getStockCode());
				boolean isLeave = false;
				String leaveStocks = ConfigCenterFactory.getString("stock_zjs.delisting_stocks", "");
				if(leaveStocks.contains(se.getStockCode())) {
					isLeave = true;
				}
				boolean isStop = CompanyService.getInstance().isStop(com);
				if(isStop == false && com != null) {
					c = com.getC();
				} else if(isStop == true || isLeave == true) {
					c = getLastStockTradePrice(se.getStockCode());
				}

				if(c != null && c.doubleValue() > 0) {
					total += se.getEquityCount() * c;
				}
			}
		}
		return total;
	}

	public Double getLastStockTradePrice(String stockcode) {
		Double c = 0D;
		Trade0001 trade = DcssTradeIndexServiceClient.getInstance().getLatestTradeData(stockcode);
		if(trade != null && trade.getF002n() != 0 && trade.getF002n() != 0) {
			c = Double.valueOf(trade.getF002n());
		}
		return c;
	}

	public boolean hasPublishStockGameVp(Long uid) {
		return MicorBlogService.getInstance().hasStockGameAdVp(uid);
	}

	private String getDateFormatter(Date date){
		String simple = "yyyy-MM-dd HH:mm:ss";
		DateFormat df=new SimpleDateFormat(simple);
		return df.format(date);
	}

	private String getCurrentDate() {
		Date date=new Date();
		DateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(date);
	}
}
