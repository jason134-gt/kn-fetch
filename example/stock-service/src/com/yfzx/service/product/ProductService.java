package com.yfzx.service.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.product.Order;
import com.stock.common.model.product.Product;
import com.stock.common.model.product.Resource;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;
public class ProductService {
	private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
	private static ProductService instance = new ProductService();
	static DBAgent dbAgent = DBAgent.getInstance();
	
	private static final String PRODUCT_BASE_NS = "com.yz.stock.portal.dao.ProductDao";
	
	private ProductService() {}
	
	public static ProductService getInstance() {
		return instance;
	}
	
	public Short insertProduct(Product product) {
		RequestMessage req = DAFFactory.buildRequest(PRODUCT_BASE_NS + ".insert", product, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			Product o = (Product)rm.getResult();
			return o.getPid();
		}
		return 0;
	}
	
	public boolean updateProduct(Product product) {
		RequestMessage req = DAFFactory.buildRequest(PRODUCT_BASE_NS + ".update", product, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;		
	}
	
	public List<Product> getProducts(Product product) {
		RequestMessage req = DAFFactory.buildRequest(PRODUCT_BASE_NS + ".select", product, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Product> list = (List<Product>)rm.getResult();
		return list;
	}
	
	public Product getProduct(Short pid) {
		RequestMessage req = DAFFactory.buildRequest(PRODUCT_BASE_NS + ".selectByPrimaryKey", pid, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Product)res.getResult();
	}
	
	public List<Product> getProducts(List<Short> pidList) {
		if(pidList == null || pidList.size() == 0) {
			return null;
		}
		
		String pids = "(" + StringUtils.join(pidList, ",") + ")";
		Map<String, String> params = new HashMap<String, String>();
		params.put("pids", pids);
		RequestMessage req = DAFFactory.buildRequest(PRODUCT_BASE_NS + ".selectList", params, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Product> list = (List<Product>)rm.getResult();
		return list;
	}
}
