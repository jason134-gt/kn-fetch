package com.yfzx.service.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Template;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;



public class TemplateService {

	 PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private TemplateService()
	{
		
	}
	private static TemplateService instance = new TemplateService();
	public static TemplateService getInstance()
	{
		return instance;
	}
	public Template getTemplateByCode(Integer templateCode) {
		Map<String, Integer> m = new HashMap<String, Integer>();
		m.put("templateCode", templateCode);
		RequestMessage req = DAFFactory.buildRequest("getTemplateByCode",
				m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Template) value;
	}
	public List<Template> getTemplateByName(String name) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("name", name);
		RequestMessage req = DAFFactory.buildRequest("getTemplateByName",
				m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Template>) value;
	}
	public String create(Template t) {
		RequestMessage reqMsg = DAFFactory.buildRequest("com.stock.common.model.Template.insert",t,StockConstants.common);
		return pLayerEnter.insert(reqMsg);
	}
	public String modify(Template t) {
		RequestMessage reqMsg = DAFFactory.buildRequest("com.stock.common.model.Template.update",t,StockConstants.common);
		return pLayerEnter.modify(reqMsg);
	}
}
