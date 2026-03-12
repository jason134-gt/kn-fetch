package com.yfzx.service.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

/**
 * 行情数据中心
 * @author Administrator
 *
 */
public class MAnalysisService {

	private static MAnalysisService instance = new MAnalysisService();
	Logger log = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private MAnalysisService() {

	}

	public static MAnalysisService getInstance() {
		return instance;
	}


	

	
}
