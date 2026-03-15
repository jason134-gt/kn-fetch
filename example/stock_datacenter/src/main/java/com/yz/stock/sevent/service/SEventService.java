package com.yz.stock.sevent.service;

import java.util.List;

import com.stock.common.constants.StockConstants;
import com.stock.common.util.StockUtil;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.sevent.event.SEvent;


public class SEventService {

	static SEventService instance = new SEventService();
	public SEventService()
	{
		
	}
	public static SEventService getInstance()
	{
		return instance;
	}

	public List<SEvent> query(String uidentify) {
		return LCEnter.getInstance().get(uidentify, StockUtil.getCacheName(StockConstants.DATA_TYPE_sevent));
	}
	public List<SEvent> queryByGid(String eid)
	{
		return LCEnter.getInstance().get(eid, StockUtil.getCacheName(StockConstants.DATA_TYPE_sevent));
	}
}
