/**
 * 
 */
package com.yfzx.service.db.company;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.company.Company0019;
import com.stock.common.util.DateUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * 主营收入 按行业
 * @author wind
 *
 */
public class Company0019Service {

	private final static String SELECT_LIST = "com.yz.stock.portal.dao.company0019.selectList";
	private final static String SELECT_LAST_LIST = "com.yz.stock.portal.dao.company0019.selectLastList";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(Company0019Service.class);	
	static Company0019Service instance = new Company0019Service();
	
	private Company0019Service(){		
	}
	
	public static Company0019Service getInstance(){
		return instance;
	}
	
	public List<Company0019> getLastCompany0019Array(String stockCode){
		List value = null;
		try {
			Map<String,String> m = new HashMap<String,String>();
			m.put("seccode", stockCode);
			RequestMessage reqMsg = DAFFactory.buildRequest(SELECT_LAST_LIST,
					m, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return value;
	}
	
	@SuppressWarnings("unchecked")
	public List<Company0019> getCompanyIncomeComposeListByOneTime(String companycode,Date time){
		Object value = null;
		try {
			Map<String,String> m = new HashMap<String,String>();
			m.put("companycode", companycode);
			m.put("time", DateUtil.format2String(time));
			RequestMessage reqMsg = DAFFactory.buildRequest("company0019.selectCompanyIncomeComposeListByOneTime",
					m, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if(value==null) return null;
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return (List<Company0019>) value;
	}
	
	@SuppressWarnings("unchecked")
	public List<Company0019> getCompanyIncomeComposeListBySectionTime(String companycode,Date sTime,Date eTime){
		Object value = null;
		try {
			Map<String,String> m = new HashMap<String,String>();
			m.put("companycode", companycode);
			m.put("sTime", DateUtil.format2String(sTime));
			m.put("eTime", DateUtil.format2String(eTime));
			RequestMessage reqMsg = DAFFactory.buildRequest("company0019.selectCompanyIncomeComposeListBySectionTime",
					m, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if(value==null) return null;
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return (List<Company0019>) value;
	}

	public List<Company0019> getCompanyLastestIncomeComposeListByOneTime(
			String companycode) {
		Object value = null;
		try {
			Map<String,String> m = new HashMap<String,String>();
			m.put("companycode", companycode);
			RequestMessage reqMsg = DAFFactory.buildRequest("company0019.getCompanyLastestIncomeComposeListByOneTime",
					m, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if(value==null) return null;
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return (List<Company0019>) value;
	}
	
	public List<Company0019> getCompanyLastestIncomeComposeListByTime(
			String companycode,Date time) {
		Object value = null;
		try {
			Map<String,String> m = new HashMap<String,String>();
			m.put("companycode", companycode);
			m.put("time", DateUtil.format2String(time));
			RequestMessage reqMsg = DAFFactory.buildRequest("company0019.getCompanyLastestIncomeComposeListByTime",
					m, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if(value==null) return null;
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return (List<Company0019>) value;
	}
	
}
