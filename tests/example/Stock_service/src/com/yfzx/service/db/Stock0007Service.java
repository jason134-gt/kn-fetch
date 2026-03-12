package com.yfzx.service.db;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Stock0007;
import com.stock.common.util.DateUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

public class Stock0007Service {

	private static Stock0007Service instance = new Stock0007Service();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private Stock0007Service() {

	}

	public static Stock0007Service getInstance() {
		return instance;
	}


	public List<Stock0007> queryStock0007LatestDataList(Date time) {

		Map<String,String> m = new HashMap<String,String>();
		m.put("time", DateUtil.format2String(time));
		String sqlMapKey = "queryStock0007LatestDataList";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey,m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Stock0007>) value;
	}

}
