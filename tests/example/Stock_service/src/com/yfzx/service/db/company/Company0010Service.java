/**
 * 
 */
package com.yfzx.service.db.company;

import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.company.Company0010;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.stock.common.bloomfilter.QueryParam;
import com.stock.common.constants.StockConstants;
/**
 * 股本结构服务
 * @author wind
 *
 */
public class Company0010Service {
	
	private final static String SELECT_LIST = "com.yz.stock.portal.dao.company0010.selectList";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger("Company0021Service");	
	static Company0010Service instance = new Company0010Service();
	
	private Company0010Service(){		
	}
	
	public static Company0010Service getInstance(){
		return instance;
	}
	
	public List<Company0010> getTop4Company0010(String stockCode){
		Company0010 company0010 = new Company0010();
		company0010.setCompany_code(stockCode);
		company0010.setStock_code(stockCode);
		List value = null;
		try {
			Map map = BeanUtils.describe(company0010);
			QueryParam queryParam = new QueryParam();
			queryParam.setSort("f001d");
			queryParam.setDir("desc");
			queryParam.setStart(0);
			queryParam.setLimit(4);
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
