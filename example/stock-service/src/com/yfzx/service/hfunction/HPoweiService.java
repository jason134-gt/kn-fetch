package com.yfzx.service.hfunction;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 * type:-1:由上往下破位，1:由下往上破位，nd：在此位下运行的天数，windex:位置先用的指标
 * return 0:未破位，1：破位
 * $hpw(type,nd,indexcode)
 *      
 * @author：杨真 
 * @date：2014年10月20日
 */
public class HPoweiService implements IFService{

	private static HPoweiService instance = new HPoweiService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HPoweiService() {

	}

	public static HPoweiService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if(vls!=null&&vls.size()>2)
		{
			
			Integer type = Integer.valueOf(vls.get(0));
			Integer nd = Integer.valueOf(vls.get(1));
			String indexcode = vls.get(2);
			ret = powei(type,nd,indexcode,req);
		}
		return ret;
	}

	/**
	 * type:-1:由上往下破位，1:由下往上破位，nd：在此位下运行的天数，windex:位置先用的指标 return 0:未破位，1：破位
	 * 
	 * @param type
	 * @return
	 */
	public Double powei(int type, int nd, String indexcode, IndexMessage req) {
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return 0.0;
		Double ret = 0.0;
		Double kc = IndexValueAgent.getIndexValue(req.getCompanyCode(),
				StockConstants.INDEX_CODE_TRADE_K, c.getTime());
		Double zgc = IndexValueAgent.getIndexValue(req.getCompanyCode(),
				StockConstants.INDEX_CODE_TRADE_ZG, c.getTime());
		Double zdc = IndexValueAgent.getIndexValue(req.getCompanyCode(),
				StockConstants.INDEX_CODE_TRADE_ZD, c.getTime());
		Double wc = IndexValueAgent.getIndexValue(req.getCompanyCode(),
				indexcode, c.getTime());
		if (kc == null || kc == 0 || zgc == null || zgc == 0 || wc == null
				|| wc == 0 || zdc == null || zdc == 0)
			return ret;
		// 是否有交叉
		boolean iscross = false;
		if (type == -1) {
			if (wc <= kc && wc >= zdc)
				iscross = true;
		} else {
			if (wc >= kc && wc <= zgc)
				iscross = true;
		}
		// 时间往前推一天
		c.add(Calendar.DAY_OF_MONTH, -1);
		Date maxtime = new Date();
		int dc = 0;
		// 是否在位的上或下方运行足够长时间
		if (iscross) {
			while (c.getTime().compareTo(mintime) >= 0
					&& c.getTime().compareTo(maxtime) <= 0 && dc < nd) {
				Double s = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						StockConstants.INDEX_CODE_TRADE_S, c.getTime());
				Double cwc = IndexValueAgent.getIndexValue(
						req.getCompanyCode(), indexcode, c.getTime());
				if (s == null || s == 0 || cwc == null || cwc == 0) {
					c.add(Calendar.DAY_OF_MONTH, -1);
					continue;
				}
				if (type == -1) {
					// 收盘价一直在位的上方运行
					if (s < cwc)
						break;
				} else {
					// 收盘价一直在位的下方运行
					if (s > cwc)
						break;
				}
				if (IndexService.getInstance().isTradeDate(c.getTime(),
						req.getCompanyCode()))
					dc++;
				c.add(Calendar.DAY_OF_MONTH, -1);

			}
		}

		if (dc == nd)
			return 1.0;
		return 0.0;
	}

}
