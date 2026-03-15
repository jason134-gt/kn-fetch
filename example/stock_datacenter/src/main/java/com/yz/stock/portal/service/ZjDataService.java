package com.yz.stock.portal.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.base.Zjasset;
import com.stock.common.model.base.Zjcash;
import com.stock.common.model.base.Zjprofile;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


public class ZjDataService {

	private static ZjDataService instance = new ZjDataService();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private ZjDataService() {
		try {

		} catch (Exception e) {
			logger.error("connect db failed!",e);
		}
	}

	public static ZjDataService getInstance() {
		return instance;
	}



	@SuppressWarnings("unchecked")
	public List<Zjasset> queryZjAssetByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryZjAssetByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Zjasset>) o;
	}

	public List<Zjprofile> queryZjProfileByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryZjProfileByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Zjprofile>) o;
	}

	public List<Zjcash> queryZjCashByCompanycode(String companycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companycode", companycode);
		String sqlMapKey = "queryZjCashByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, m, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Zjcash>) o;
	}

	

}
