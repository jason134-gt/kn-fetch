/**
 * 
 */
package com.yfzx.service.db.user;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.CreditRule;
import com.stock.common.model.user.CreditRuleLog;
import com.stock.common.model.user.Members;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author wind
 *
 */
public class CreditRuleService {
	private final static String BASE_NS = "com.yz.stock.portal.dao.creditRule.CreditRuleDao";
	private static DBAgent dbAgent = DBAgent.getInstance();
	private static Logger logger = LoggerFactory.getLogger(CreditRuleService.class);
	private static CreditRuleService instance = new CreditRuleService();
	public static enum CYCLE_TYPE{
		ONCE(0,"一次性"), 
		DAY(1,"每天"),
		HOUR(2,"整点"),
		MINUTES(3,"间隔分钟"),
		NO(4,"不限");		
		
		private String name;   
		private int value;
		//一次性/每天/整点/间隔分钟/不限
		
		CYCLE_TYPE(int value ,String name){
			this.value = value;
			this.name = name;
		}
		
		public int getValue(){
			return value;
		}
		
		public String toString(){
			return "[ vaule = " + String.valueOf(value)+" & name = " + name + " ]";
		}
		
	}
	private CreditRuleService(){
		
	}
	
	public static CreditRuleService getInstance(){
		return instance;
	}
	
	public Integer insert(CreditRule model){
		Integer reInt = Integer.valueOf("0");
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", model, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			CreditRule reRule = (CreditRule)rm.getResult();
			reInt = reRule.getRid();
		}
		return reInt;
	}
	
	public boolean update(CreditRule model){		
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
	public List<CreditRule> getSelectAll(){		
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".select",new CreditRule(), StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		@SuppressWarnings("unchecked")
		List<CreditRule> ruleList = rm.getResult()!=null?(List<CreditRule>)rm.getResult():null;
		return ruleList;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public CreditRule getSelect(Integer id){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectByPrimaryKey",id, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		CreditRule rule = rm.getResult() != null ? (CreditRule)rm.getResult():null;
		return rule;
	}
	
	
	/**
	 * 执行用户的积分处理【包括日志和用户表】
	 * @param members
	 * @param privilegeValue
	 */
	public void exeCreditLogic(Members members,String privilegeValue){
		
		List<CreditRule> crList = this.getSelectAll();
		CreditRule localCr = null;
		for(CreditRule cr : crList){
			if(privilegeValue.equalsIgnoreCase(cr.getAction())){
				localCr = cr;
				break;
			}			
		}
		
		if(localCr != null){
			CreditRuleLog crl = new CreditRuleLog();
			crl.setUid(members.getUid());
			crl.setRid(localCr.getRid());
			CreditRuleLogService crlService = CreditRuleLogService.getInstance();
			CreditRuleLog crlDB = crlService.getSelect(crl);
			boolean addCredit = false;//判断是否加积分	
			Date localDate = new Date();
			if(crlDB ==null){//当此积分第一次操作时
				addCredit = true;
				crl.setDateline(localDate);
				crl.setStarttime(localDate);
				crl.setTotal(1);
				crl.setCyclenum(1);
				crl.setExtcredits1(localCr.getExtcredits1());
				crl.setExtcredits2(localCr.getExtcredits2());
				crl.setExtcredits3(localCr.getExtcredits3());
				crl.setExtcredits4(localCr.getExtcredits4());
				crl.setExtcredits5(localCr.getExtcredits5());
				crl.setExtcredits6(localCr.getExtcredits6());
				crl.setExtcredits7(localCr.getExtcredits7());
				crl.setExtcredits8(localCr.getExtcredits8());
				crlService.insert(crl);
			}else{
				if(CYCLE_TYPE.NO.getValue() == localCr.getCycletype()){
					crl.setDateline(localDate);
					crl.setCyclenum(1);
					addCredit = true;
				}else if(CYCLE_TYPE.ONCE.getValue() == localCr.getCycletype()){
					addCredit = false;
				}else if(CYCLE_TYPE.DAY.getValue() == localCr.getCycletype()){//配置每天
					long localTime = System.currentTimeMillis();
					long dbTime = crlDB.getDateline().getTime();
					long cycletime = 3600*1000;
					if(localTime-dbTime-cycletime>0){//超过一天
						addCredit = true;
						crl.setDateline(localDate);
						crl.setCyclenum(1);
					}else{//一天内
						if(crlDB.getCyclenum()>=localCr.getRewardnum()){//规则日志次数 大于等于 规则次数 
							addCredit = false;							
						}else{//本天第2到N次
							crl.setCyclenum(crlDB.getCyclenum()+1);
							addCredit = true;
						}												
					}
				}else{//其它两种方式先当无限使用,后期优化
					crl.setDateline(localDate);
					crl.setCyclenum(1);
					addCredit = true;
				}
				if(addCredit == true){
					crl.setClid(crlDB.getClid());
					crl.setTotal(crlDB.getTotal()+1);
					crl.setExtcredits1(crlDB.getExtcredits1()+localCr.getExtcredits1());
					crl.setExtcredits2(crlDB.getExtcredits2()+localCr.getExtcredits2());
					crl.setExtcredits3(crlDB.getExtcredits3()+localCr.getExtcredits3());
					crl.setExtcredits4(crlDB.getExtcredits4()+localCr.getExtcredits4());
					crl.setExtcredits5(crlDB.getExtcredits5()+localCr.getExtcredits5());
					crl.setExtcredits6(crlDB.getExtcredits6()+localCr.getExtcredits6());
					crl.setExtcredits7(crlDB.getExtcredits7()+localCr.getExtcredits7());
					crl.setExtcredits8(crlDB.getExtcredits8()+localCr.getExtcredits8());
					crlService.update(crl);
				}
			}
			if(addCredit == false)return;//不加积分时，直接结束			
			
			MembersService ms = MembersService.getInstance();
//			Members saveMerbers = new Members();
//			saveMerbers.setUid(members.getUid());
			//Session的members
			members.setExtcredits1(members.getExtcredits1()+localCr.getExtcredits1());
			members.setExtcredits2(members.getExtcredits2()+localCr.getExtcredits2());
			members.setExtcredits3(members.getExtcredits3()+localCr.getExtcredits3());
			members.setExtcredits4(members.getExtcredits4()+localCr.getExtcredits4());
			members.setExtcredits5(members.getExtcredits5()+localCr.getExtcredits5());
			members.setExtcredits6(members.getExtcredits6()+localCr.getExtcredits6());
			members.setExtcredits7(members.getExtcredits7()+localCr.getExtcredits7());
			members.setExtcredits8(members.getExtcredits8()+localCr.getExtcredits8());
			ms.updateMembers(members);
		}
	}

}
