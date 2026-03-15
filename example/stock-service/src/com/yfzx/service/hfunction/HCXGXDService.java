package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.DateUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 * 创N天内的新高或新低 type:czg：创新高|czd:创新低|,nd:天数，
 * $hcxgxd(type,nd,indexcode,ratio) indexcode:日最高价指标|周最高价指标|月最高价指标
 * ratio:缩放比例，默认为1，如果大于1，则认为是预突破，在前期高点附近
 * @author：杨真
 * @date：2014年10月20日
 */
public class HCXGXDService implements IFService {

	private static HCXGXDService instance = new HCXGXDService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HCXGXDService() {

	}

	public static HCXGXDService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 2) {
			String type = vls.get(0);
			Integer nd = Integer.valueOf(vls.get(1));
			String indexcode = vls.get(2);
			Double ratio = Double.valueOf(vls.get(3));
			ret = computeHCXGXD(type, nd, indexcode, req,ratio);
		}
		return ret;
	}

	/**
	 * 
	 * @param type
	 * @param indexcode
	 * @param req
	 * @param ratio 
	 * @return
	 */
	public Double computeHCXGXD(String type, int nd, String indexcode,
			IndexMessage req, Double ratio) {
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Double ret = 0.0;

		Double cur = IndexValueAgent.getIndexValue(req.getCompanyCode(),
				indexcode, c.getTime());
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(
				indexcode);
		
		if (d == null || cur == null || cur == 0.0 )
			return ret;

		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return ret;
		// 从前一天开始
		c.add(d.getTunit(), -d.getInterval());
		Date maxtime = new Date();
		Double rv = 0.0;
		Date hdate = null;
		int dc = 0;
		int tdc = 0;//记录点到开始点的交易日的天数
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {
			Date actime = IndexService.getInstance().formatTime(c.getTime(), d, req.getUidentify());
			if(actime!=null)
			{
				Double cv = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						indexcode, actime);
				if (cv == null || cv == 0) {
					c.add(d.getTunit(), -d.getInterval());
					continue;
				}
				if (type.equals("czg") ) {
					if (cv > rv) {
						rv = cv;
						hdate = actime;
						tdc = dc;
					}
				}
				if (type.equals("czd")) {
					if (rv == 0 || cv < rv) {
						rv = cv;
						hdate = actime;
						tdc = dc;
					}
				}
				dc++;
			}
			
			c.add(d.getTunit(), -d.getInterval());

		}
		if (hdate != null) {
			if (type.equals("czg") ) {
				if (cur*ratio > rv ) {
//					ret = Double.valueOf(DateUtil.getDayLong(wetime.getTime(),
//							hdate.getTime()));
					ret = Double.valueOf(tdc);
				}
			}
			if (type.equals("czd") ) {
				if (cur*ratio < rv ) {
//					ret = Double.valueOf(DateUtil.getDayLong(wetime.getTime(),
//							hdate.getTime()));
					ret = Double.valueOf(tdc);
				}
			}
		}
		return ret;
	}
}
