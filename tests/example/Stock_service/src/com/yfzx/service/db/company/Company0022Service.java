package com.yfzx.service.db.company;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.QueryParam;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.company.Company0022;
import com.stock.common.util.DateUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


public class Company0022Service {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger("Company0022Service");
	private final static String NAMESPACE = "com.stock.portal.dao.Company0022.Company0022Dao";
	
	private static Company0022Service instance = new Company0022Service();

	private Company0022Service() {

	}

	public static Company0022Service getInstance() {
		return instance;
	}


	/**
	 * 获取某家公司的股东变化情况
	 * 
	 * @param companyCode
	 *            股票代码
	 * @return
	 */
	public List<Company0022> getCompany0022List(String companyCode) {
		Company0022 company0022 = new Company0022();
		company0022.setCompanyCode(companyCode);

		List value = null;
		try {
			Map map = BeanUtils.describe(company0022);
			QueryParam queryParam = new QueryParam();
			queryParam.setSort(" enddate desc ");		
			queryParam.setDir(" ");
			queryParam.setStart(0);
			queryParam.setLimit(12);//最近12期
			map.put("queryParam", queryParam);
			
			RequestMessage reqMsg = DAFFactory.buildRequest(NAMESPACE+".selectList",
					map, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return value;
	}
	
	public List<Map> getCGSList(Date uptime) {
		List list=null;
		try {
			Map<String,String> m = new HashMap<String,String>();
			m.put("uptime", DateUtil.format2String(uptime));
			RequestMessage reqMsg = DAFFactory.buildRequest("queryRjcg",
					m, StockConstants.COMPANY_TYPE);
			list = pLayerEnter.queryForList(reqMsg);		
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return list;
	}
	
	public List<Map> getCGSListV2(Date uptime) {
		List list=null;
		try {
			Map<String,String> m = new HashMap<String,String>();
			m.put("uptime", DateUtil.format2String(uptime));
			RequestMessage reqMsg = DAFFactory.buildRequest("queryRjcgV2",
					m, StockConstants.COMPANY_TYPE);
			list = pLayerEnter.queryForList(reqMsg);		
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return list;
	}
}
