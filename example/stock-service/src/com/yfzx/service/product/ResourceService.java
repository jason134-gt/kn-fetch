package com.yfzx.service.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.product.Resource;
import com.stock.common.model.user.Members;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class ResourceService {
	private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);
	private static ResourceService instance = new ResourceService();
	static DBAgent dbAgent = DBAgent.getInstance();
	
	private static final String RESOURCE_BASE_NS = "com.yz.stock.portal.dao.ResourceDao";
	
	private ResourceService() {}
	
	public static ResourceService getInstance() {
		return instance;
	}
	
	public Short insertResource(Resource resource) {
		RequestMessage req = DAFFactory.buildRequest(RESOURCE_BASE_NS + ".insert", resource, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			Resource o = (Resource)rm.getResult();
			return o.getRid();
		}
		return 0;		
	}
	
	public boolean updateResource(Resource resource) {
		RequestMessage req = DAFFactory.buildRequest(RESOURCE_BASE_NS + ".update", resource, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;		
	}
	
	public Resource getResource(Short rid) {
		RequestMessage req = DAFFactory.buildRequest(RESOURCE_BASE_NS + ".selectByPrimaryKey", rid, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Resource)res.getResult();
	}
	
	public List<Resource> getResourceList(List<Short> ridList) {
		if(ridList == null || ridList.size() == 0) {
			return null;
		}
		String rids = "(" + StringUtils.join(ridList, ",") + ")";
		Map<String, String> params = new HashMap<String, String>();
		params.put("rids", rids);
		RequestMessage req = DAFFactory.buildRequest(RESOURCE_BASE_NS+"."+"selectList", params, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Resource> list = (List<Resource>)rm.getResult();
		return list;		
	}
}
