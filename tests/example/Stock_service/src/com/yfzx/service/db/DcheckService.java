package com.yfzx.service.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dcheck;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;



public class DcheckService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static DcheckService instance = new DcheckService();

	private DcheckService() {

	}

	public static DcheckService getInstance() {
		return instance;
	}

	public String modifyDcheckByid(Dcheck dc) {
	
		RequestMessage reqMsg = DAFFactory.buildRequest("modifyDcheckByid",dc,StockConstants.common);
		return pLayerEnter.modify(reqMsg);
	}

	public String insert(Dcheck dc) {
		RequestMessage reqMsg = DAFFactory.buildRequest("insertDcheck",dc,StockConstants.common);
		return pLayerEnter.insert(reqMsg);
	}


	
	public Dcheck getDcheckById(Integer id) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", id);
		RequestMessage req = DAFFactory.buildRequest(
				"getDcheckById", m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Dcheck) value;
	}
	

	
	public String delDcheckById(Integer id) {
		RequestMessage reqMsg = DAFFactory.buildRequest("delDcheckById",id,StockConstants.common);
		return pLayerEnter.delete(reqMsg);
	}


	public String modifyDcheck(Dcheck dc) {
		String ret = "";
		if(dc.getId()>0)
		{
			Dcheck rtr = getDcheckById(dc.getId());
			if(rtr==null)
			{
				ret = insert(dc);
			}
			else
			{
				ret = modifyDcheckByid(dc);
			}
		}
		else
		{
			ret = insert(dc);
		}
		return ret;
	}

	public List<Dcheck> queryDcheckListByDesc(String desc) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("desc", desc);
		RequestMessage req = DAFFactory.buildRequest(
				"queryDcheckListByDesc", m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Dcheck>) value;
	}
	
	public List<Dcheck> queryDcheckListAll() {
		RequestMessage req = DAFFactory.buildRequest(
				"queryDcheckListAll", StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Dcheck>) value;
	}
}
