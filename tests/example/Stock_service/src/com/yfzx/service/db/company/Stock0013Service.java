/**
 * 
 */
package com.yfzx.service.db.company;

import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.QueryParam;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.company.Stock0013;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author wind
 *
 */
public class Stock0013Service {

	private final static String SELECT_LIST = "com.yz.stock.portal.dao.stock0013.selectList";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(Stock0013Service.class);	
	static Stock0013Service instance = new Stock0013Service();
	
	private Stock0013Service(){		
	}
	
	public static Stock0013Service getInstance(){
		return instance;
	}
	
	public List<Stock0013> getSelectList(String stockCode){
		List<Stock0013> value = null;
		Stock0013 stock0013 = new Stock0013();
		stock0013.setSeccode(stockCode);
		QueryParam queryParam = new QueryParam();
		queryParam.setSort(" f001d ");
		queryParam.setDir(" desc ");
		queryParam.setStart(0);
		queryParam.setLimit(10);		
		try {
			Map map = BeanUtils.describe(stock0013);
			map.put("queryParam", queryParam);
			RequestMessage reqMsg = DAFFactory.buildRequest(SELECT_LIST,
					map, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return value;
	}
	
	
}
