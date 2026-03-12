/**
 * 
 */
package com.yfzx.service.db.company;

import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.company.Stock0010;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author wind
 *
 */
public class Stock0010Service {

	private final static String SELECT = "com.yz.stock.portal.dao.stock0010.select";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(Stock0010Service.class);	
	static Stock0010Service instance = new Stock0010Service();
	
	private Stock0010Service(){		
	}
	
	public static Stock0010Service getInstance(){
		return instance;
	}
	
	public Stock0010 getSelect(String stockCode){
		Stock0010 value = null;
		Stock0010 inStock0010 = new Stock0010();
		inStock0010.setSeccode(stockCode);
		try {
			Map map = BeanUtils.describe(inStock0010);
			RequestMessage reqMsg = DAFFactory.buildRequest(SELECT,
					map, StockConstants.COMPANY_TYPE);
			value = (Stock0010) pLayerEnter.queryForObject(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return value;
	}
	
	
}
