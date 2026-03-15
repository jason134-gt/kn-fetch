package com.yfzx.service.db;

import java.util.List;

import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.msg.Message;

public class CIndexService implements IIService {

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static CIndexService instance = new CIndexService();
	private CIndexService() {

	}

	public static CIndexService getInstance() {
		return instance;
	}
	public Double getIndexValue(Message msg) {
		RequestMessage reqMsg = DAFFactory.buildRequest("select_u_ext_index",msg,StockConstants.INDEX_DATA_TYPE);
		Object value = pLayerEnter.queryForObject(reqMsg);
		if (value == null) {
			return null;
		}
		return (Double) value;
	}
	
	@SuppressWarnings("rawtypes")
	public List getIndexList(Message msg) {
		RequestMessage reqMsg = DAFFactory.buildRequest("select_u_ext_index_list",msg,StockConstants.common);
		Object value = pLayerEnter.queryForList(reqMsg);
		if (value == null) {
			return null;
		}
		return (List) value;
	}

	public Object getIndexObject(Message msg) {
		RequestMessage reqMsg = DAFFactory.buildRequest(StockSqlKey.ext_c_index_key_query_2,msg,StockConstants.common);
		return pLayerEnter.queryForObject(reqMsg);
	}

	public List getIndexObjectList(Message msg) {
		RequestMessage reqMsg = DAFFactory.buildRequest(StockSqlKey.ext_c_index_key_query_3,msg,StockConstants.common);
		return pLayerEnter.queryForList(reqMsg);
	}

	

}
