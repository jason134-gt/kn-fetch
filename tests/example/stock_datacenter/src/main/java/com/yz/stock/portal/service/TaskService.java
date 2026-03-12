package com.yz.stock.portal.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.util.DateUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

public class TaskService {

	private static TaskService instance = new TaskService();
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger log = LoggerFactory.getLogger(this.getClass());


	private TaskService() {

	}

	public static TaskService getInstance() {
		return instance;
	}




	public void importNewData2NormalDb(Date uptime) {
		Map<String,String>  vm = new HashMap<String,String>();
		vm.put("uptime", DateUtil.format2String(uptime));
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"p_importData2NormalDb", vm,StockConstants.common);
		pLayerEnter.queryForObject(reqMsg);
		
	}
	public void importNewData2NormalDb_timer(String uptime) {
		Map<String,String>  vm = new HashMap<String,String>();
		vm.put("uptime", uptime);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"p_importData2NormalDb_timer", vm,StockConstants.common);
		pLayerEnter.queryForObject(reqMsg);
		
	}
	public void importChildCapData2MidDb(String uptime) {
		Map<String,String>  vm = new HashMap<String,String>();
		vm.put("uptime", uptime);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"p_importChildCapData2MidDb", vm,StockConstants.common);
		pLayerEnter.queryForObject(reqMsg);
		
	}

}
