package com.yfzx.service.db;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;




public class CashFlowService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(CashFlowService.class);
	private static CashFlowService instance = new CashFlowService();
	public static CashFlowService getInstance()
	{
		return instance;
	}
	private CashFlowService() {

	}
	

	
	
}
