package com.yfzx.service.trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.model.trade.StockTrade;
import com.yz.mycore.lcs.enter.LCEnter;

/**
 * 交易服务类
 * 
 * @author：杨真
 * @date：2014-7-25
 */
public class StockTradeService {

	Logger log = LoggerFactory.getLogger(StockTradeService.class);
	static StockTradeService instance = new StockTradeService();
	public StockTradeService() {

	}

	public static StockTradeService getInstance() {
		return instance;
	}
	private  String getStockTradeKey(String companyCode) {
		return "st." + companyCode;
	}
	public StockTrade getStockTradeFromCache(String uidentify)
	{
		String key = getStockTradeKey(uidentify);
		return LCEnter.getInstance().get(key,
				SCache.CACHE_NAME_marketcache);
	}
}
