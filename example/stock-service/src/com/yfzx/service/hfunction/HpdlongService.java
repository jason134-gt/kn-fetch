package com.yfzx.service.hfunction;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.IndexMessage;
import com.stock.common.model.trade.TradeBitMap;
import com.yfzx.service.trade.TradeBitMapService;

/**
 * 已上市的天数
 *  $hpdl()
 * @author：杨真
 * @date：2014年10月20日
 */
public class HpdlongService implements IFService {

	private static HpdlongService instance = new HpdlongService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HpdlongService() {

	}

	public static HpdlongService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() >= 0) {
			TradeBitMap tb = TradeBitMapService.getInstance().getBitMap(req.getCompanyCode());
			if(tb!=null)
			{
				ret = Double.valueOf(tb.getMaxIndex());
			}
		}
		return ret;
	}
}
