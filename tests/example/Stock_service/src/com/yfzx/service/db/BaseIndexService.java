package com.yfzx.service.db;


import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


public class BaseIndexService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(BaseIndexService.class);
	private static BaseIndexService instance = new BaseIndexService();
	public static BaseIndexService getInstance()
	{
		return instance;
	}
	private BaseIndexService() {

	}
	
	static String jdbcurl = "jdbc:mysql://localhost:3306/stock";
	static String user = "root";
	static String pwd = "yangzhen";
	static Connection con ;
	
	static 
	{
		init();
	}
	static void init()
	{
		try {
			Configuration c = BaseFactory.getConfiguration();
			user = c.getString("stock.indexcompute.batchWrite.user");
			jdbcurl = c.getString("stock.indexcompute.batchWrite.jdbcurl");
			pwd = c.getString("stock.indexcompute.batchWrite.pwd");
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(jdbcurl, user, pwd);
		} catch (Exception e) {
			logger.error("connect db failed!",e);
		}
	}
	

	
	public List getProfileList(IndexMessage msg) {
		String sqlMapKey = "getProfileList";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey,
				msg, StockConstants.TYPE_PROFILE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List) value;
	}
	public List getBaseIndexList(IndexMessage msg) {
		String sqlMapKey = "getBaseIndexList";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey,
				msg, StockConstants.BASE_INDEX_TYPE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List) value;
	}



}
