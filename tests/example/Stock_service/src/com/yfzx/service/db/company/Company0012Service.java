/**
 * 
 */
package com.yfzx.service.db.company;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.QueryParam;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.company.Company0012;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * 十大流通股东服务
 * @author wind
 *
 */
public class Company0012Service {
	
	private final static String SELECT_LIST = "com.yz.stock.portal.dao.company0012.selectList";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger("Company0021Service");	
	static Company0012Service instance = new Company0012Service();
	
	private Company0012Service(){		
	}
	
	public static Company0012Service getInstance(){
		return instance;
	}
	
	public List<Company0012> getCompany0012Array(String stockCode){
		Company0012 company0012 = new Company0012();
		company0012.setCompany_code(stockCode);
		company0012.setStock_code(stockCode);
		
		List value = null;
		try {
			Map map = BeanUtils.describe(company0012);
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
	
	public List<Map> getCGSList(String companycode ,String uptime) {
		List list=null;
		try {
			Map<String,String> m = new HashMap<String,String>();
			m.put("uptime", uptime);
			m.put("companycode", companycode);
			RequestMessage reqMsg = DAFFactory.buildRequest("selectLatestCompany0012",
					m, StockConstants.COMPANY_TYPE);
			list = pLayerEnter.queryForList(reqMsg);		
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return list;
	}
	
}
