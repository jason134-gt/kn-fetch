package com.yfzx.service.db;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.util.DbConnectionBroker;
import com.yz.mycore.core.config.BaseConfiguration;
import com.yz.mycore.core.manager.BaseFactory;

public class StockServiceFactory {


	static Logger log = LoggerFactory.getLogger(StockServiceFactory.class);
	
	static DbConnectionBroker myconPool  = null; 
	static {
		init();
	}
	public static void init()
	{
		initDbPool();
	}
	
	public static DbConnectionBroker getMyConPool()
	{
		return myconPool;
	}
	private static void initDbPool() {
		
		try {
			Configuration c = BaseFactory.getConfiguration();
			String user = c.getString("stock.indexcompute.batchWrite.user");
			String jdbcurl = c.getString("stock.indexcompute.batchWrite.jdbcurl");
			String pwd = c.getString("stock.indexcompute.batchWrite.pwd");
			Class.forName("com.mysql.jdbc.Driver");
			int initSize =  c.getInt("stock.indexcompute.batchWrite.initSize");
			int maxActive =  c.getInt("stock.indexcompute.batchWrite.maxActive");
			int connTimeout =  c.getInt("stock.indexcompute.batchWrite.connTimeout");
			String logPath =  c.getString("stock.indexcompute.batchWrite.logPath");
		    logPath = BaseConfiguration.getConfBasePath()+logPath;
			myconPool = new DbConnectionBroker("com.mysql.jdbc.Driver",
					 jdbcurl,
					 user, pwd, initSize, maxActive,
					 logPath, connTimeout); 
		} catch (Exception e) {
			log.error("connect db failed!", e);
		}
	}



	

}
