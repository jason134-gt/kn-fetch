package com.yz.stock.portal.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.base.Yhasset;
import com.stock.common.model.base.Yhcash;
import com.stock.common.model.base.Yhprofile;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


public class YhDataService {

	private static YhDataService instance = new YhDataService();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private YhDataService() {
		try {

		} catch (Exception e) {
			logger.error("connect db failed!",e);
		}
	}

	public static YhDataService getInstance() {
		return instance;
	}


	@SuppressWarnings("unchecked")
	public List<Yhasset> queryYhAssetByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryYhAssetByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Yhasset>) o;
	}

	public List<Yhprofile> queryYhProfileByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryYhProfileByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Yhprofile>) o;
	}

	public List<Yhcash> queryYhCashByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryYhCashByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Yhcash>) o;
	}

	

}
