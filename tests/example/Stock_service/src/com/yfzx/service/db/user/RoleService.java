/**
 * 
 */
package com.yfzx.service.db.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.RoleWithBLOBs;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author wind
 *
 */
public class RoleService {
	private final static String BASE_NS = "com.yz.stock.portal.dao.role.RoleDao";//MerbersMapper.xml的namespace
	private static DBAgent dbAgent = DBAgent.getInstance();
	private static Logger logger = LoggerFactory.getLogger(RoleService.class);
	private static RoleService instance = new RoleService();
	public static enum ROLE_TYPE{
		Normal("normal"), ADMIN("admin");
		private  String value;   

		ROLE_TYPE(String str){
			value = str;
		}
		
		public String getValue(){
			return value;
		}
		
		public String toString(){
			return value;
		}
		
		public boolean equals(String anObject){
			return value.equals(anObject);
		}
	}
	
	private RoleService(){
		
	}
	
	public static RoleService getInstance(){
		return instance;
	}
	
	public Integer insert(RoleWithBLOBs model){
		Integer reInt = Integer.valueOf("0");
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", model, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			RoleWithBLOBs reRole = (RoleWithBLOBs)rm.getResult();
			reInt = reRole.getId();
		}
		return reInt;
	}
	
	public boolean update(RoleWithBLOBs model){		
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
	public List<RoleWithBLOBs> getSelectAll(){		
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".select",new RoleWithBLOBs(), StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<RoleWithBLOBs> roleList = rm.getResult()!=null?(List<RoleWithBLOBs>)rm.getResult():null;
		return roleList;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public RoleWithBLOBs getSelect(Integer id){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectByPrimaryKey",id, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		RoleWithBLOBs role = rm.getResult() != null ? (RoleWithBLOBs)rm.getResult():null;
		return role;
	}
	

}
