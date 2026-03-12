package com.yz.stock.portal.service.baseindex;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

public class ProfileService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(ProfileService.class);
	private static ProfileService instance = new ProfileService();
	public static ProfileService getInstance()
	{
		return instance;
	}
	private ProfileService() {

	}
	@SuppressWarnings("rawtypes")
	public List getProfileList(IndexMessage msg) {
		String sqlMapKey = "getProfileList";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey,
				msg, StockConstants.TYPE_PROFILE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List) value;
	}
	

}
