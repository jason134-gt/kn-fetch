package com.yfzx.service.hfunction;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.IndexMessage;
import com.stock.common.util.DateUtil;
import com.yfzx.service.trade.TradeService;

/**
 * 取缩放倍数
 * type:d:日缩放倍数，w:周缩放倍数，m:月缩放倍数
 * $hgetbs(type)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HGetsfbsService implements IFService{

	private static HGetsfbsService instance = new HGetsfbsService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HGetsfbsService() {

	}

	public static HGetsfbsService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>0)
		{
			
			String type = vls.get(0);
			ret = getSFBS(type,req);
		}
		return ret;
	}

	// 取缩放倍数
		public Double getSFBS(String type, IndexMessage req) {
			Double times = 1.0;
			Date cd = DateUtil.getDayStartTime(new Date());
			if (DateUtil.isSameDay(req.getTime().getTime(), cd.getTime())) {
				// 取今天到此刻的交易时间
				Long tradeLong = HUtilService.getInstance().getCurDayTradeLong(req.getUidentify(),System.currentTimeMillis());
				if (tradeLong != -1) {
					if (type.equals("d")) {
						// 取全天交易时间
						double at = TradeService.getInstance().getTradeTimeRegion(
								req.getUidentify());
						times = at / tradeLong;

					}
					if (type.equals("w")) {
						int wcount =  HUtilService.getInstance().getWeekTradeDateCount(req);
						double at = TradeService.getInstance().getTradeTimeRegion(
								req.getUidentify());
						times = 5 * at / (at * wcount + tradeLong);

					}
					if (type.equals("m")) {
						int mcount =  HUtilService.getInstance().getMonthTradeDateCount(req);
						double at = TradeService.getInstance().getTradeTimeRegion(
								req.getUidentify());
						times = 20 * at / (at * mcount + tradeLong);

					}
				}

			}
			return times;
		}
}
