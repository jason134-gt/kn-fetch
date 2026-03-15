/**
 * 
 */
package com.yfzx.service.db.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.CreditRuleLog;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author wind
 *
 */
public class CreditRuleLogService {
	private final static String BASE_NS = "com.yz.stock.portal.dao.creditRuleLog.CreditRuleLogDao";
	private static DBAgent dbAgent = DBAgent.getInstance();
	private static Logger logger = LoggerFactory.getLogger(CreditRuleLogService.class);
	private static CreditRuleLogService instance = new CreditRuleLogService();
	
	private CreditRuleLogService(){
		
	}
	
	public static CreditRuleLogService getInstance(){
		return instance;
	}
	
	public Integer insert(CreditRuleLog model){
		Integer reInt = Integer.valueOf("0");
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", model, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			CreditRuleLog reRule = (CreditRuleLog)rm.getResult();
			reInt = reRule.getClid();
		}
		return reInt;
	}
	
	public boolean update(CreditRuleLog model){		
		try {
			RequestMessage reqMsg = DAFFactory.buildRequest(BASE_NS+".updateByPrimaryKey",model, StockConstants.common);
			String retrunCode = dbAgent.modifyRecord(reqMsg).getRetrunCode();
			if(BaseCodes.SUCCESS.equals(retrunCode)){
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
			logger.error("operator failed!", e);
			return false;
		}
	}
	
	/**
	 * 获取所有的角色
	 * @return
	 */
	public List<CreditRuleLog> getSelectAll(){		
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".select",new CreditRuleLog(), StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		@SuppressWarnings("unchecked")
		List<CreditRuleLog> ruleList = rm.getResult()!=null?(List<CreditRuleLog>)rm.getResult():null;
		return ruleList;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public CreditRuleLog getSelect(CreditRuleLog crl){
		if(crl.getUid() == null || crl.getRid() == null)return null;//使用联合主键查询
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectByUK",crl, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		CreditRuleLog rule = rm.getResult() != null ? (CreditRuleLog)rm.getResult():null;
		return rule;
	}
		

}
