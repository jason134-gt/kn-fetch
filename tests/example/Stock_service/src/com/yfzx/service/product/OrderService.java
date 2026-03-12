package com.yfzx.service.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.product.Order;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;
public class OrderService {
	private final static Logger logger = LoggerFactory.getLogger(OrderService.class);
	private static OrderService instance = new OrderService();
	static DBAgent dbAgent = DBAgent.getInstance();
	
	private static final String ORDER_BASE_NS = "com.yz.stock.portal.dao.OrderDao";
	
	private OrderService() {}
	
	public static OrderService getInstance() {
		return instance;
	}
	
	public Long insertOrder(Order order) {
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS + ".insert", order, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			Order o = (Order)rm.getResult();
			return o.getOid();
		}
		return null;
	}
	
	public boolean updateOrder(Order order) {
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS + ".updateByPrimary", order, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}
	
	public Order getOrder(long oid) {
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS + ".selectByPrimaryKey", oid, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Order)res.getResult();
	}
	
	public Order getOrderBySn(String sn) {
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS + ".selectBySn", sn, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Order)res.getResult();
	}	
	
	/**
	 * 
	 * @param uid
	 * @param valid 有效性：-1：过期  1：正常  0：所有
	 * @param fetchSize
	 * @param offset
	 * @return
	 */
	public List<Order> getMemberOrders(long uid, int valid, int fetchSize, int offset) {
		Map params = new HashMap();
		params.put("uid", uid);
		params.put("fetchSize", fetchSize);
		params.put("offset", offset);
		if(valid != 0) {
			params.put("valid", valid);
		}
		
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS+"."+"selectList", params, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Order> list = (List<Order>)rm.getResult();
		return list;
	}
	
	public List<Order> getMemberVipOrders(Order order) {
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS+"."+"select", order, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Order> list = (List<Order>)rm.getResult();
		return list;
	}
	
	public List<Order> getMemberOrdersByCon(Order order) {
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS+"."+"selectByCon", order, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Order> list = (List<Order>)rm.getResult();
		return list;
	}
	
	public Order getMemberOrderByUidAndSn(Long uid, String orderSn, Integer valid) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("uid", uid);
		params.put("orderSn", orderSn);
		params.put("valid", valid);
		
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS + ".getMemberOrderByUidAndSn", params, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Order)res.getResult();		
	}
	
	public Order getMemberLastOrder(Long uid, Integer pid, Integer valid) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("uid", uid);
		params.put("pid", pid);
		params.put("valid", valid);
		
		RequestMessage req = DAFFactory.buildRequest(ORDER_BASE_NS + ".getMemberLastOrder", params, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Order)res.getResult();		
	}	
	
}
