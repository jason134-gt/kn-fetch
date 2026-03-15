package com.yfzx.service.msg.handler.mp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobilePushHandler  {

	static Logger logger = LoggerFactory.getLogger(MobilePushHandler.class);
	private static MobilePushHandler instance = new MobilePushHandler();
	private MobilePushHandler()
	{

	}
	public static MobilePushHandler getInstance()
	{
		return instance;
	}


}
