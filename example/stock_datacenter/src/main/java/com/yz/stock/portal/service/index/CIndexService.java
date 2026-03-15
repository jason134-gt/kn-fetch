package com.yz.stock.portal.service.index;

import java.util.List;

import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.model.IndexMessage;
import com.stock.common.msg.Message;
import com.yfzx.service.db.IIService;
import com.yfzx.service.db.TUextService;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

public class CIndexService implements IIService {

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static CIndexService instance = new CIndexService();
	private CIndexService() {

	}

	public static CIndexService getInstance() {
		return instance;
	}
	public Double getIndexValue(Message msg) {
		RequestMessage reqMsg = DAFFactory.buildRequest(StockSqlKey.ext_c_index_key_query_0,msg,StockConstants.TYPE_EXT_INDEX);
		Object value = pLayerEnter.queryForObject(reqMsg);
		if (value == null) {
			return null;
		}
		return (Double) value;
	}
	
	public Double getIndexValueWithCache(Message msg) {
		IndexMessage im = (IndexMessage) msg;
		RequestMessage reqMsg = DAFFactory.buildRequest(im.getKey(),StockSqlKey.ext_c_index_key_query_0,msg,StockConstants.TYPE_EXT_INDEX);
		Object value = pLayerEnter.queryForObject(reqMsg);
		if (value == null) {
			return null;
		}
		return (Double) value;
	}
	
	@SuppressWarnings("rawtypes")
	public List getIndexList(Message msg) {
		RequestMessage reqMsg = DAFFactory.buildRequest(StockSqlKey.ext_c_index_key_query_1,msg,StockConstants.common);
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

	//只查缓存
	public Double getIndexValueFromCache(IndexMessage msg) {
	
		return TUextService.getInstance().getUExtDouble(msg.getCompanyCode(), msg.getTime().getTime(), msg.getIndexCode());

	}

}
