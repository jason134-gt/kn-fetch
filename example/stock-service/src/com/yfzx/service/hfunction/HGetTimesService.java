package com.yfzx.service.hfunction;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.IndexMessage;
import com.stock.common.util.DateUtil;
import com.yfzx.service.db.IndexService;

/**
 * 取交易时长
 * 
 * $hgettimes()
 * 
 * @author：杨真
 * @date：2014年10月20日
 */
public class HGetTimesService implements IFService {

	private static HGetTimesService instance = new HGetTimesService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HGetTimesService() {

	}

	public static HGetTimesService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null) {

			ret = getTimes(req);
		}
		return ret;
	}

	public Double getTimes(IndexMessage req) {
		Double times = 240.0;
		Date cd = DateUtil.getDayStartTime(new Date());
		if (DateUtil.isSameDay(req.getTime().getTime(), cd.getTime())) {
			// 取今天到此刻的交易时间
			Long tradeLong = HUtilService.getInstance().getCurDayTradeLong(
					req.getUidentify(), System.currentTimeMillis());
			if(tradeLong!=null&&tradeLong!=0&&tradeLong!=-1)
				times = tradeLong/(60*1000.0);		
		}
		return times;
	}
}
