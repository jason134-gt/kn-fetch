package com.yfzx.service.share;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.chance.ChanceMsgWapper;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.StockCacheUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class SquareStockChanceService {
	private Logger logger = LoggerFactory.getLogger(SquareStockChanceService.class);

	public static SquareStockChanceService instance = new SquareStockChanceService();

	private SquareStockChanceService() {}

	public static SquareStockChanceService getInstance() {
		return instance;
	}

	public void initSquareStockChances() {
		Date endTime = new Date();

		String all_square_keys = ConfigCenterFactory.getString("stock_zjs.all_square_chance_keys", "allSquareKeys");
		String loadKeys = ConfigCenterFactory.getString("stock_chance.square_chance_keys", "");

		boolean flag = StockCacheUtil.isInThisDcss(all_square_keys, StockCacheUtil.getAppIndex());
		if(flag) {
			int square_chance_size2 = ConfigCenterFactory.getInt("stock_zjs.square_chance_size2", 300);
			for(String key : loadKeys.split(",")) {
				if(StringUtils.isBlank(key)) {
					continue;
				}
				List<TradeAlarmMsg> tradeAlarmMsgList = ViewpointService.getInstance().listUmFromNosql(key, endTime.getTime(), 0, square_chance_size2);
				if(tradeAlarmMsgList != null && tradeAlarmMsgList.size() > 0) {
					for(TradeAlarmMsg msg : tradeAlarmMsgList) {
						add2SquareChanceWaper(all_square_keys, msg, 1);
					}
				}
			}
		}

		int square_chance_size2 = ConfigCenterFactory.getInt("stock_zjs.square_chance_size2", 300);
		for(String key : loadKeys.split(",")) {
			if(StringUtils.isBlank(key)) {
				continue;
			}
			boolean isInThisDcss = StockCacheUtil.isInThisDcss(key, StockCacheUtil.getAppIndex());
			if(isInThisDcss) {
				List<TradeAlarmMsg> tradeAlarmMsgList = ViewpointService.getInstance().listUmFromNosql(key, endTime.getTime(), 0, square_chance_size2);
				if(tradeAlarmMsgList != null && tradeAlarmMsgList.size() > 0) {
					for(TradeAlarmMsg msg : tradeAlarmMsgList) {
						add2SquareChanceWaper(key, msg, 2);
					}
				}
			}
		}
	}

	public int add2SquareChanceWaper(String key, TradeAlarmMsg um, int level) {
		ChanceMsgWapper eq = LCEnter.getInstance().get(key,
				StockUtil.getEventCacheName(key));
		if (eq == null) {
			eq = new ChanceMsgWapper();
			LCEnter.getInstance().put(key, eq,
					StockUtil.getEventCacheName(key));
		}
		// 放入分类id
		eq.putSquareItem(um, level);
		return eq.getMessageList().size();
	}

	public void deleteSquareStockChance(String uuid) {
		try {
			SimpleArticle sa = RemindServiceClient.getInstance().getSimpleArticle(uuid);
			if(sa == null || sa.getAttr("squareTime")==null) {
				logger.error("deleteSquareStockChanceFailure: article or sqaureTime is null " + uuid + " sqaureTime " + sa.getAttr("sqaureTime") );
				return ;
			}
			long time = Long.parseLong(sa.getAttr("squareTime").toString());
			sa.removeAttr("squareTime");
			Map<String, String> map = NosqlBeanUtil.bean2Map(sa);
			CassandraHectorGateWay.getInstance().insert(SAVE_TABLE.ARTICLE.toString(),sa.getUuid(), map);
			RemindServiceClient.getInstance().putSimpleArticle(sa.getUuid(), sa);
			TimeLineService tls = TimeLineService.getInstance();
			String all_square_keys = ConfigCenterFactory.getString("stock_zjs.all_square_chance_keys", "allSquareKeys");
			tls.deltetTimeLine(all_square_keys, SAVE_TABLE.VIEWPOINT, time);
			RemindServiceClient.getInstance().deleteSquareStockChance(all_square_keys, uuid);
			String loadKeys = ConfigCenterFactory.getString("stock_chance.square_chance_keys", "");
			for(String key : loadKeys.split(",")) {
				tls.deltetTimeLine(key, SAVE_TABLE.VIEWPOINT, time);
				RemindServiceClient.getInstance().deleteSquareStockChance(key, uuid);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		int uidIndex = StockUtil.getUserIndex("squale_key2")% 2;
		System.out.println(uidIndex);
	}
}
