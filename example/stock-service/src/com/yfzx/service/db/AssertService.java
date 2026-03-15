package com.yfzx.service.db;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


public class AssertService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger("AssertService");
	private static AssertService instance = new AssertService();
	public static AssertService getInstance()
	{
		return instance;
	}
	private AssertService() {

	}
	


	

	public List getAssetList_Map(Date sTime, Date eTime, String companyCode, String tableName) {
		IndexMessage im = SMsgFactory.getUMsg(companyCode);
		im.setStartTime(sTime);
		im.setEndTime(eTime);
		im.setTableName(tableName);
		String sqlMapKey = "getAssertList_columnModel";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey,
				im, StockConstants.TYPE_ASSET);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List)value;
	}
	
}
