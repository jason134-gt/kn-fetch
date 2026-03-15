package com.yfzx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.IndexMessage;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

public class StockCenter {
	private static StockCenter instance = new StockCenter();
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger log = LoggerFactory.getLogger(this.getClass());
	static int isTest = 1;
	static
	{
		init();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			public void refresh() {
				init();
				
			}
			
		});
	}
	private StockCenter() {

	}

	private static void init() {
		isTest = ConfigCenterFactory.getInt("stock_zjs.is_test_work", 0);
	}

	public static StockCenter getInstance() {
		return instance;
	}
	
	public boolean isNeedAccessDcss(IndexMessage im)
	{
//		if(isTest==1)
//			return true;
		if(im.isNeedAccessExtRemoteCache())
			return true;
		return false;
	}
}
