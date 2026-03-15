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
import com.stock.common.model.company.CompanyExt;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


/**
 * @author wind
 *
 */
public class CompanyExtService {

	private final static String NAMESPACES = "com.stock.common.model.company.CompanyExt";
	private final static String SELECT_LIST = NAMESPACES + ".select";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(CompanyExtService.class);	
	static CompanyExtService instance = new CompanyExtService();
	
	private CompanyExtService(){		
	}
	
	public static CompanyExtService getInstance(){
		return instance;
	}
	
	public CompanyExt getSelect(String stockCode){
		CompanyExt value = null;
		CompanyExt companyExt = new CompanyExt();
		companyExt.setStockCode(stockCode);
		try {			
			RequestMessage reqMsg = DAFFactory.buildRequest(SELECT_LIST,
					companyExt, StockConstants.COMPANY_TYPE);
			value = (CompanyExt) pLayerEnter.queryForObject(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return value;
	}
	
	public List<CompanyExt> getSelectAll(){
		List<CompanyExt> value = null;
		
		try {			
			RequestMessage reqMsg = DAFFactory.buildRequest(NAMESPACES + "." + "selectAll", StockConstants.COMPANY_TYPE);
			Object o =  pLayerEnter.queryForList(reqMsg);
			if(o!=null)
				value = (List<CompanyExt>) o;
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return value;
	}
	
	private void insert(CompanyExt companyExt){
		String reCode;
		try {
			Map map = BeanUtils.describe(companyExt);
			RequestMessage reqMsg = DAFFactory.buildRequest(NAMESPACES+".insert",
					companyExt, StockConstants.COMPANY_TYPE);
			reCode = pLayerEnter.insert(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
	}
	
	private void update(CompanyExt companyExt){
		String reCode;
		try {
			Map map = BeanUtils.describe(companyExt);
			RequestMessage reqMsg = DAFFactory.buildRequest(NAMESPACES+".updateByPrimaryKey",
					companyExt, StockConstants.COMPANY_TYPE);
			reCode = pLayerEnter.modify(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
	}
	
	public void saveDB(CompanyExt companyExt){
		CompanyExt selCompanyExt = getSelect(companyExt.getStockCode());
		if(selCompanyExt == null){
			insert(companyExt);
		}else{
			update(companyExt);
		}
	}
	
	
}
