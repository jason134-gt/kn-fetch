package com.yfzx.service.db;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.Msgconst;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


public class MsgconstService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static MsgconstService instance = new MsgconstService();

	private MsgconstService() {

	}

	public static MsgconstService getInstance() {
		return instance;
	}



	public String create(Msgconst mc) {
		String ret = StockCodes.SUCCESS;
		Msgconst dmc = query(mc.getMsg());
		if(dmc==null)
		{
			ret = insert(mc);
		}
		else
		{
			ret = modify(mc);
		}
		return ret;
	}

	public String modify(Msgconst mc) {
	
		RequestMessage reqMsg = DAFFactory.buildRequest("com.stock.common.model.Msgconst.update",mc,StockConstants.common);
		return pLayerEnter.modify(reqMsg);
	}

	public String insert(Msgconst mc) {
		RequestMessage reqMsg = DAFFactory.buildRequest("com.stock.common.model.Msgconst.insert",mc,StockConstants.common);
		return pLayerEnter.insert(reqMsg);
	}

	public Msgconst query(String msg) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("msg", msg);
		RequestMessage req = DAFFactory.buildRequest(
				"com.stock.common.model.Msgconst.select", m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Msgconst) value;
	}

}
