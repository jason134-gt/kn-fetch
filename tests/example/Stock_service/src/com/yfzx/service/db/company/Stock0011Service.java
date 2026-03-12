/**
 * 
 */
package com.yfzx.service.db.company;

import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.company.Stock0011;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author wind
 *
 */
public class Stock0011Service {

	private final static String SELECT_LIST = "com.yz.stock.portal.dao.stock0011.select";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(Stock0011Service.class);	
	static Stock0011Service instance = new Stock0011Service();
	
	private Stock0011Service(){		
	}
	
	public static Stock0011Service getInstance(){
		return instance;
	}
	
	public List<Stock0011> getSelectList(String stockCode){
		List<Stock0011> value = null;
		Stock0011 inStock0011 = new Stock0011();
		inStock0011.setSeccode(stockCode);
		try {
			Map map = BeanUtils.describe(inStock0011);
			RequestMessage reqMsg = DAFFactory.buildRequest(SELECT_LIST,
					map, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return value;
	}
	
	
}
