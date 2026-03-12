package com.yz.stock.portal.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.base.Bxasset;
import com.stock.common.model.base.Bxcash;
import com.stock.common.model.base.Bxprofile;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


public class BxDataService {

	private static BxDataService instance = new BxDataService();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private BxDataService() {
		try {

		} catch (Exception e) {
			logger.error("connect db failed!",e);
		}
	}

	public static BxDataService getInstance() {
		return instance;
	}


	@SuppressWarnings("unchecked")
	public List<Bxasset> queryBxAssetByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryBxAssetByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Bxasset>) o;
	}

	public List<Bxprofile> queryBxProfileByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryBxProfileByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Bxprofile>) o;
	}

	public List<Bxcash> queryBxCashByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryBxCashByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Bxcash>) o;
	}

	

}
