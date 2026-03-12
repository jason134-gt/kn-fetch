package com.yfzx.service.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.snn.Statistics;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class StatisticsService {

	 PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static StatisticsService instance = new StatisticsService();
	public StatisticsService()
	{
		
	}
	public static StatisticsService getInstance()
	{
		return instance;
	}
	
	public void save(Statistics st)
	{
		Statistics nst = query(st.getEid());
		if(nst==null)
			insert(st);
		else
			modify(st);
	}

	private Statistics query(String eid)
	{
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("eid", eid);
		RequestMessage req = DAFFactory.buildRequest(
				"getStatisticsByEid", m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Statistics) value;
	}
	
	private String modify(Statistics st) {
		
		RequestMessage reqMsg = DAFFactory.buildRequest("modifyStatisticsByEid",st,StockConstants.data_type_statistics);
		return pLayerEnter.modify(reqMsg);
	}

	private String insert(Statistics st) {
		RequestMessage reqMsg = DAFFactory.buildRequest("insertStatisticsByEid",st,StockConstants.data_type_statistics);
		return pLayerEnter.insert(reqMsg);
	}
	
	public List<Statistics> queryAll() {
		RequestMessage req = DAFFactory.buildRequest(
				"queryAllStatistics", StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (List<Statistics>) value;
	}

	public Statistics getStatisticsFromCache(String eid) {
		
		return LCEnter.getInstance().get(eid, SCache.CACHE_NAME_STATISTICS);
	}
}
