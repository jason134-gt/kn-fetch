package com.yfzx.service.stockgame;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.stockgame.StockSnapshoot;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class StockSnapshootService {
	private static final String NS = "com.stock.portal.dao.stockgame.StockSnapshootDao";

	private StockSnapshootService() {}

	private static StockSnapshootService instance = new StockSnapshootService();

	private static DBAgent dbAgent = DBAgent.getInstance();

	public static StockSnapshootService getInstance() {
		return instance;
	}

	public long insert(StockSnapshoot stockSnapshoot) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", stockSnapshoot, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			StockSnapshoot obj = (StockSnapshoot)rm.getResult();
			return obj.getId();
		}
		return 0;
	}

	public boolean updateByPrimaryKey(StockSnapshoot stockSnapshoot) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "updateByPrimaryKey", stockSnapshoot, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		}
		return false;
	}

	public StockSnapshoot getStockSnapshoot(StockSnapshoot stockSnapshoot) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "select", stockSnapshoot, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (StockSnapshoot)rm.getResult();
		}
		return null;
	}
}
