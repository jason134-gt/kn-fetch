package com.yfzx.service.db;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Stock0009;
import com.stock.common.util.DateUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

public class Stock0009Service {

	private static Stock0009Service instance = new Stock0009Service();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private Stock0009Service() {

	}

	public static Stock0009Service getInstance() {
		return instance;
	}


	public List<Stock0009> queryStock0009LatestDataList(String time) {

		Map<String,String> m = new HashMap<String,String>();
		m.put("time", time);
		String sqlMapKey = "queryStock0009LatestDataList";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey,m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Stock0009>) value;
	}

	public List<Stock0009> queryStock0009LatestDataListByPage(Date time,Integer start ,Integer limit) {

		Map<String,Object> m = new HashMap<String,Object>();
		m.put("time", DateUtil.format2String(time));
		m.put("start", start);
		m.put("limit", limit);
		String sqlMapKey = "queryStock0009LatestDataListByPage";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey,m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Stock0009>) value;
	}
	
}
