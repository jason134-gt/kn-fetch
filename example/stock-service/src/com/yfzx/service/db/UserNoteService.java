package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.nosql.UserNote;

public class UserNoteService {
	private static UserNoteService userNoteService = new UserNoteService();	
	DBAgent dbAgent = DBAgent.getInstance();
	
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	private UserNoteService(){		
	}
	
	public static UserNoteService getInstance(){
		return userNoteService;
	}
	
	public void insert(UserNote u){
		RequestMessage req = DAFFactory.buildRequest("com.yz.stock.portal.action.common.testInsertNote", u, StockConstants.COMPANY);
		dbAgent.createRecord(req);
	}
	
	public List getByUserID(String userID){	
		UserNote u = new UserNote();
		u.setUserID(userID);
		RequestMessage req = DAFFactory.buildRequest("com.yz.stock.portal.action.common.testSelectNote", u, StockConstants.COMPANY);
		Object value = dbAgent.queryForList(req);
		if (value == null) {
			return null;
		}
		ResponseMessage resp = (ResponseMessage) value;
		List<Object> l = new ArrayList<Object>();
		if (resp.getResult() != null ) {	
			Object obj = resp.getResult();
			if(obj instanceof List){
				l.addAll((List)obj);
			}			
		}
		return l;		
	}

}
