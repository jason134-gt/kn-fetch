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
 * 阴量统计
 * timeUnit:时间单位，nd多少天 $hslht(type,indexcodeA,indexcodeB)
 * type:0:统计阴量，1：统计阳量
 * 
 * @author：杨真
 * @date：2014年10月20日
 */
public class HSlhtService implements IFService {

	private static HSlhtService instance = new HSlhtService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HSlhtService() {

	}

	public static HSlhtService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 1) {
			String type = vls.get(0);
			String indexcodeA = vls.get(1);
			String indexcodeB = vls.get(2);
			ret = slht(req, type,indexcodeA, indexcodeB);
		}
		return ret;
	}

	public Double slht(IndexMessage req, String type, String indexcodeA,
			String indexcodeB) {
		Double count = 0.0;
		Date wetime = req.getTime();
		wetime = DateUtil.getDayStartTime(wetime);
		Calendar c = Calendar.getInstance();
		c.setTime(wetime);
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				req.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return count;
		Date dtime = DateUtil.getDayStartTime(new Date());
		Dictionary da = DictService.getInstance()
				.getDataDictionaryFromCache(
						HUtilService.getInstance().getIndexcodeByType(
								"d", indexcodeA));

		Dictionary db = DictService.getInstance()
				.getDataDictionaryFromCache(
						HUtilService.getInstance().getIndexcodeByType(
								"d", indexcodeB));
		if(da.getTunit()!=db.getTunit())
			return count;
		Date maxtime = new Date();
		
		while (c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Date actime = IndexService.getInstance().formatTime(c.getTime(), da, req.getCompanyCode());
			if(actime!=null)
			{
				Double zs = IndexValueAgent.getIndexValue(
						req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(
								da.getTimeUnit(), ComputeUtil.TRADE_ZS), actime);
				
				Double cp = IndexValueAgent.getIndexValue(
						req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(
								da.getTimeUnit(), ComputeUtil.TRADE_S), actime);
				if(zs==null||zs==0||cp==null||cp==0)
				{
					c.add(da.getTunit(), -da.getInterval());
					continue;
				}
				Double sd = cp - zs;
//				if(sd!=0)
//					count++;
				if(sd==0||sd>0&&type.equals("0")||sd<0&&type.equals("1"))
				{
					c.add(da.getTunit(), -da.getInterval());
					continue;
				}
				Double a = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(
								"d", indexcodeA), actime);
				Double b = IndexValueAgent.getIndexValue(req.getCompanyCode(),
						HUtilService.getInstance().getIndexcodeByType(
								"d", indexcodeB), actime);
				if (a != null && a != 0 && b != null && b != 0) {
						Double sfbs = 1.0;
						//如果为当天，则乘上缩放比例
						if(dtime.compareTo(actime)==0)
						{
							sfbs = HGetsfbsService.getInstance().getSFBS("d", req);
						}
						if (a*sfbs <= b*1.2) {
							count++;
						} else
							break;
					
				}
				
			}

			c.add(da.getTunit(), -da.getInterval());

		}

		return count;
	}
}
