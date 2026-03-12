/**
 * 
 */
package com.yfzx.service.db.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.ThirdUser;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author tbq
 *
 */
public class ThirdUserService {
	private final static String BASE_NS = "com.yz.stock.portal.dao.user.ThirdUserDao";
	private static DBAgent dbAgent = DBAgent.getInstance();
	private static Logger logger = LoggerFactory.getLogger(ThirdUserService.class);
	private static ThirdUserService instance = new ThirdUserService();
	public static enum THIRD_USER_TYPE{
		WEIBO(0), QQ(1);
		private int value;   

		THIRD_USER_TYPE(int i){
			value = i;
		}
		
		public int getValue(){
			return value;
		}
		
		public String toString(){
			return String.valueOf(value);
		}
		
		public boolean equals(int anObject){
			return value== anObject;
		}
	}
	
	private ThirdUserService(){
		
	}
	
	public static ThirdUserService getInstance(){
		return instance;
	}
	
	public boolean insert(ThirdUser model){		
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", model, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean update(ThirdUser model){		
		//必须有值
		//model.getOpenId() ;
		//model.getPlatform();
		
		//一般用于绑定用户
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
	 * 获取所有的第三方用户
	 * @return
	 */
	public List<ThirdUser> getSelectAll(){		
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".select",new ThirdUser(), StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<ThirdUser> tuList = rm.getResult()!=null?(List<ThirdUser>)rm.getResult():null;
		return tuList;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public ThirdUser getSelect(String openId ,Integer platform){
		ThirdUser tu = new ThirdUser();
		tu.setOpenId(openId);
		tu.setPlatform(platform);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectKey",tu, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		tu = rm.getResult() != null ? (ThirdUser)rm.getResult():null;
		return tu;
	}
	
	public List<ThirdUser> getThirdUsersByUid(long uid) {
		RequestMessage req = DAFFactory.buildRequest(BASE_NS + "." + "getThirdUsersByUid", uid, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<ThirdUser>)rm.getResult();
		} else {
			return null;
		}
	}
	
	public boolean deleteThirdUserBind(String openId, Long uid) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("openId", openId);
		m.put("uid", uid);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS + "." + "deleteThirdUserBind", m, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true; 
		} else {
			return false;
		}
	}
	public boolean deleteFromPlatformAndUid(int type, Long uid) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("platform", type);
		m.put("uid", uid);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS + "." + "deleteFromPlatformAndUid", m, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true; 
		} else {
			return false;
		}
	}
}
