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
import com.stock.common.model.company.Company0013;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * 十大股东服务
 * @author wind
 *
 */
public class Company0013Service {
	
	private final static String SELECT_LIST = "com.yz.stock.portal.dao.company0013.selectList";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(Company0013Service.class);	
	static Company0013Service instance = new Company0013Service();
	
	private Company0013Service(){		
	}
	
	public static Company0013Service getInstance(){
		return instance;
	}
	
	public List<Company0013> getCompany0013Array(String stockCode){
		Company0013 company0013 = new Company0013();
		company0013.setCompany_code(stockCode);
		company0013.setStock_code(stockCode);
		List value = null;
		try {
			Map map = BeanUtils.describe(company0013);			
			map.put("zerof001n", true);
			QueryParam queryParam = new QueryParam();
			queryParam.setSort(" enddate desc,f001n ");		
			queryParam.setDir(" ");
			queryParam.setStart(0);
			queryParam.setLimit(10);
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
