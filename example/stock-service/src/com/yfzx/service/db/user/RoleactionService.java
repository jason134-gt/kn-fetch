/**
 * 
 */
package com.yfzx.service.db.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.Roleaction;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author wind
 *
 */
public class RoleactionService {
											
	private final static String BASE_NS = "com.yz.stock.portal.dao.roleaction.RoleactionDao";//MerbersMapper.xml的namespace
	private static DBAgent dbAgent = DBAgent.getInstance();
	private static Logger logger = LoggerFactory.getLogger(RoleactionService.class);
	private static RoleactionService instance = new RoleactionService();
	
	public static RoleactionService getInstance(){
		return instance;
	}
	
	public List<Roleaction> getRoleactionList(){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".select",new Roleaction(), StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Roleaction> roleList = rm.getResult()!=null?(List<Roleaction>)rm.getResult():null;
		return roleList;
	}
	
	public Roleaction getRoleaction(Short id){
		Roleaction ra = new Roleaction();
		ra.setId(id);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".select",ra, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		return rm.getResult()!=null?(Roleaction)rm.getResult():null;
	}
	
	public boolean update(Roleaction model){		
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
	
	public Short insert(Roleaction model){
		Short reShort = Short.valueOf("0");
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", model, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			Roleaction reRa = (Roleaction)rm.getResult();
			reShort = reRa.getId();
		}
		return reShort;
	}
}
