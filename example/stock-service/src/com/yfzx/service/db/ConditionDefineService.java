package com.yfzx.service.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.ConditionDefine;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;



public class ConditionDefineService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static ConditionDefineService instance = new ConditionDefineService();

	private ConditionDefineService() {

	}

	public static ConditionDefineService getInstance() {
		return instance;
	}



	public String create(ConditionDefine mc) {
		String ret = StockCodes.SUCCESS;
		ConditionDefine dmc = query(mc.getName(),mc.getType());
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

	public String modify(ConditionDefine mc) {
	
		RequestMessage reqMsg = DAFFactory.buildRequest("com.stock.common.model.ConditionDefine.update",mc,StockConstants.common);
		return pLayerEnter.modify(reqMsg);
	}

	public String insert(ConditionDefine mc) {
		RequestMessage reqMsg = DAFFactory.buildRequest("com.stock.common.model.ConditionDefine.insert",mc,StockConstants.common);
		return pLayerEnter.insert(reqMsg);
	}

	public ConditionDefine query(String name, Integer type) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("name", name);
		m.put("type", type);
		RequestMessage req = DAFFactory.buildRequest(
				"queryByNameType", m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (ConditionDefine) value;
	}
	
	public ConditionDefine getConditionDefineById(Integer id) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", id);
		RequestMessage req = DAFFactory.buildRequest(
				"getConditionDefineById", m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (ConditionDefine) value;
	}

	public List<ConditionDefine> queryListByName(String name, String type) {
		if(name==null) name = "";
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("name", name);
		m.put("type", type);
		RequestMessage req = DAFFactory.buildRequest(
				"queryListByName", m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<ConditionDefine>) value;
	}
	public ConditionDefine queryById(Integer id) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", id);
		RequestMessage req = DAFFactory.buildRequest(
				"getConditionDefineById", m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (ConditionDefine) value;
	}

	public void createById(ConditionDefine mc) {
		ConditionDefine cd = queryById(mc.getId());
		if(cd==null)
		{
			ConditionDefineService.getInstance().insert(mc);
		}
		else
		{
			ConditionDefineService.getInstance().modifyByid(mc);
		}
	}

	private void modifyByid(ConditionDefine mc) {
		RequestMessage reqMsg = DAFFactory.buildRequest("updateByid",mc,StockConstants.common);
		 pLayerEnter.modify(reqMsg);
		
	}
}
