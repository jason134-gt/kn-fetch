package com.yfzx.service.db.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.Rolemodule;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class RolemoduleService {
	
	private final static String BASE_NS = "com.yz.stock.portal.dao.rolemodule.RolemoduleDao";//MerbersMapper.xml的namespace
	private static DBAgent dbAgent = DBAgent.getInstance();
	private static Logger logger = LoggerFactory.getLogger(RoleService.class);
	private static RolemoduleService instance = new RolemoduleService();
	
	public static RolemoduleService getInstance(){
		return instance;
	}
	
	public List<Rolemodule> getSelectAll(){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".select",new Rolemodule(), StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Rolemodule> roleList = rm.getResult()!=null?(List<Rolemodule>)rm.getResult():null;
		return roleList;
	}
	
}
