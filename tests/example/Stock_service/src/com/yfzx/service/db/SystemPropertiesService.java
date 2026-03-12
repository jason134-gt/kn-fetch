package com.yfzx.service.db;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

public class SystemPropertiesService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger("SystemPropertiesService");
	private static SystemPropertiesService instance = new SystemPropertiesService();
	

	private SystemPropertiesService() {

	}

	public static SystemPropertiesService getInstance() {
		return instance;
	}


	public void update(String key, String v) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("key", key);
		m.put("value", v);
		RequestMessage req = DAFFactory.buildRequest("SystemPropertiesUpdate", m,
				StockConstants.common);
		pLayerEnter.modify(req);

	}

	public String query(String key) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("key", key);
		RequestMessage req = DAFFactory.buildRequest("SystemPropertiesSelectByKey", m,
				StockConstants.common);
		Object o = pLayerEnter.queryForObject(req);
		if(o==null) return null;
		return (String) o;
	}

}
