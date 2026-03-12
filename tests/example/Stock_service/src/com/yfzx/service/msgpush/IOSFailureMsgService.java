package com.yfzx.service.msgpush;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.msgpush.IOSFailureMsg;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class IOSFailureMsgService {
	
	private static final String NS = "com.stock.portal.dao.msgpush.IOSFailureMsgDao";
	
	private static DBAgent dbAgent = DBAgent.getInstance();
	
	public Long insert(IOSFailureMsg msg) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", msg, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			IOSFailureMsg obj = (IOSFailureMsg)rm.getResult();
			return obj.getId();
		}
		return Long.valueOf(0);		
	}
	
	public boolean update(IOSFailureMsg msg) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "update", msg, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}			
	}
	
	public boolean delete(long id) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "deleteByPrimaryKey", id, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}	
	}
	
	public Integer getMsgCount(Integer retryTimes) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("retryTimes", retryTimes);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getMsgCount", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (Integer)rm.getResult();
		} else {
			return null;
		}
	}
	
	public List<IOSFailureMsg> getMsgList(Integer retryTimes, Integer start, Integer limit) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("retryTimes", retryTimes);
		map.put("start", start);
		map.put("limit", limit);
		
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getMsgList", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<IOSFailureMsg>)rm.getResult();
		} else {
			return null;
		}
	}
}
