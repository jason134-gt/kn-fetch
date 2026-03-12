package com.yfzx.service.hfunction;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.RealTimeService;
import com.yfzx.service.db.TUextService;
import com.yfzx.service.db.USubjectService;

/**
 * timeUnit:时间单位，nd多少天 $hmacd(timeUnit,nd1,nd2,nd3,type)
 * type:"macd","dea","dif","ema"
 * @author：杨真
 * @date：2014年10月20日
 */
public class HMacdService implements IFService {

	private static HMacdService instance = new HMacdService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	public static HMacdService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 3) {
			String timeUnit = vls.get(0);
			Integer nd1 = Integer.valueOf(vls.get(1));
			Integer nd2 = Integer.valueOf(vls.get(2));
			Integer nd3 = Integer.valueOf(vls.get(3));
			String type = vls.get(4);
			try {
				
				Date wetime = req.getTime();
				wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
						timeUnit, wetime);
				IndexMessage mreq = (IndexMessage) req.clone();
				mreq.setTime(wetime);
				Date mintime = USubjectService.getInstance()
						.getTradeIndexMinTime(
								req.getUidentify(),
								HUtilService.getInstance().getIndexcodeByType(
										timeUnit, ComputeUtil.TRADE_S));
				if (mintime == null)
					return 0.0;
				Dictionary d = DictService.getInstance()
						.getDataDictionaryFromCache(
								HUtilService.getInstance().getIndexcodeByType(
										timeUnit, ComputeUtil.TRADE_HF_S));
		
				if(type.equals("macd"))
				{
					Double dif = dif(mreq, nd1, nd2, timeUnit, mintime, d,type);
					Double dea = dea(mreq, nd1, nd2, nd3, timeUnit, mintime, d,type);
					Double macd = 2*(dif-dea);
					ret = macd;
				}
				if(type.equals("dif"))
				{
					Double dif = dif(mreq, nd1,nd2, timeUnit, mintime, d,type);
					ret = dif;
				}
				if(type.equals("ema"))
				{
					Double ema = ema(mreq,nd1,timeUnit,mintime,d,type);
					ret = ema;
				}
				if(type.equals("dea"))
				{
					Double dea = dea(mreq, nd1,nd2,nd3, timeUnit, mintime, d,type);
					ret = dea;
				}
			} catch (Exception e) {
				log.error("HRsvService compute failed!", e);
			}
		}
		return ret;
	}

	public Double dea(IndexMessage req, Integer nd1, Integer nd2, Integer nd3, String timeUnit,
			Date mintime, Dictionary d, String type) {
		Double dea = 0.0;
			try {
				Double dif = dif(req, nd1, nd2, timeUnit, mintime, d,type);

				// 前一个交易日
				Date pretime = IndexService.getInstance().getNextTradeUtilEnd(
						req.getTime(), d, req.getCompanyCode(), -1);
				if (pretime == null)
					return dea;
				IndexMessage creq = (IndexMessage) req.clone();
				creq.setTime(pretime);
				Double predif = dif(creq, nd1, nd2, timeUnit, mintime, d,type);
				Double pxs = 2.0/(nd3+1);
				dea = pxs*dif+(1-pxs)*predif;

			} catch (Exception e) {
				log.error("HRsvService compute failed!", e);
			}
		
		return dea;
		
	}
	
	public Double dif(IndexMessage mreq, Integer nd1, Integer nd2, String timeUnit,
			Date mintime, Dictionary d, String type) {
		Double dif = 0.0;
			try {
				Double nd1Ema = ema(mreq, nd1, timeUnit, mintime, d,type);
				Double nd2Ema = ema(mreq, nd2, timeUnit, mintime, d,type);
				dif = nd1Ema - nd2Ema;

			} catch (Exception e) {
				log.error("HRsvService compute failed!", e);
			}
		
		return dif;
		
	}
	
	public Double ema(IndexMessage req, Integer nd, String timeUnit,
			Date mintime, Dictionary d, String type ) {
		String indexcode = HUtilService.getInstance().getIndexCode(
				getThisKey("ema_",timeUnit, nd));
		Double ema = 0.0;
		//平滑系统数
		Double pxs = 2.0/(nd+1);
		Double preEma = preEma(req,nd,timeUnit,mintime,d,indexcode,pxs,type);
		Double di = computeDI(req,timeUnit);
		ema = pxs*di+(1-pxs)*preEma;
		return ema;
	}

	private Double preEma(IndexMessage req, Integer nd, String timeUnit, Date mintime, Dictionary d, String indexcode, Double pxs, String type) {
		Double ret = 0.0;
		try {
			Date wetime = req.getTime();
			wetime = HUtilService.getInstance().getHStartTimeByTimeUnit(
					timeUnit, wetime);

			// 前一个交易日
			Date pretime = IndexService.getInstance().getNextTradeUtilEnd(
					wetime, d, req.getCompanyCode(), -1);
			if (pretime == null)
				return ret;
			if (wetime.compareTo(mintime) <= 0)
			{
				return computeDI(req,timeUnit);
			}
			Double preValue = null;
			String ckey = "";
			if (!StringUtil.isEmpty(indexcode)) {
				// 前一个交易日的参数值
				preValue = IndexValueAgent.getIndexValueNotRealCompute(
						req.getCompanyCode(), indexcode, pretime);
			} else {
				//不缓存当天的
				if(DateUtil.getDayStartTime(new Date()).compareTo(pretime)!=0)
				{
					// 临时缓存:格式
					// 000002.sz^1414425600000^2022
					String tindexcode = "ema_"+nd;
//					ckey = StockUtil.getExtCachekey(req.getCompanyCode(),
//							tindexcode, pretime);
//					preValue = ExtCacheService.getInstance().get(ckey);
					preValue = TUextService.getInstance().getUExtDouble(req.getCompanyCode(), pretime.getTime(), tindexcode);
				}
				
			}
			if (preValue == null || preValue.isNaN() || preValue.isInfinite()) {

				IndexMessage creq = (IndexMessage) req.clone();
				creq.setTime(pretime);
				preValue = ema(creq, nd, timeUnit, mintime, d,type);
				if (preValue != null) {
					ret = preValue;
					if (StringUtil.isEmpty(indexcode)) {
//						ExtCacheService.getInstance().putV(ckey, preValue);
						String tindexcode = "ema_"+nd;
						TUextService.getInstance().putData(req.getCompanyCode(), pretime.getTime(), tindexcode, preValue.floatValue());
					} else {
						// 缓存中间结果
						RealTimeService.getInstance().put2LocalCache(
								req.getCompanyCode(), indexcode, preValue,
								pretime);
					}
				}
			} else {
				ret = preValue;
			}

		} catch (Exception e) {
			log.error("HRsvService compute failed!", e);
		}
		return ret;
	}

	private Double computeDI(IndexMessage req, String timeUnit) {
		Double di = 0.0;
		Date actime = req.getTime();
		// 现价
		Double cp = IndexValueAgent.getIndexValue(
				req.getCompanyCode(),
				HUtilService.getInstance().getIndexcodeByType(
						timeUnit, ComputeUtil.TRADE_HF_S),
				actime);
		if(cp==null)
			return di;
		// 最高
		Double h = IndexValueAgent.getIndexValue(
				req.getCompanyCode(),
				HUtilService.getInstance().getIndexcodeByType(
						timeUnit, ComputeUtil.TRADE_HF_ZG),
				actime);
		// 最低
		Double l = IndexValueAgent.getIndexValue(
				req.getCompanyCode(),
				HUtilService.getInstance().getIndexcodeByType(
						timeUnit, ComputeUtil.TRADE_HF_ZD),
				actime);
		if(l!=null&&h!=null)
			di = (cp*2+l+h)/4;
//		di = cp;
		return di;
	}

	private String getThisKey(String prek,String timeUnit, Integer nd) {
		// TODO Auto-generated method stub
		return prek + nd + "_" + timeUnit;
	}
}
