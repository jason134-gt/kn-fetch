package com.yfzx.service.stockgame;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.stockgame.StockChargeBill;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class StockChargeBillService {
//	private static final Logger logger = LoggerFactory.getLogger(StockChargeBillService.class);

	private static final String NS = "com.stock.portal.dao.stockgame.StockChargeBillDao";

	private StockChargeBillService() {}

	private static StockChargeBillService instance = new StockChargeBillService();

	private static DBAgent dbAgent = DBAgent.getInstance();

	public static StockChargeBillService getInstance() {
		return instance;
	}

	public long insert(StockChargeBill stockChargeBill) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", stockChargeBill, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			StockChargeBill obj = (StockChargeBill)rm.getResult();
			return obj.getId();
		}
		return 0;
	}

	public List<StockChargeBill> getStockChargeBillListByTimeRegion(long uid, Date startTime, Date endTime) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("uid", uid);
		m.put("startTime", startTime);
		m.put("endTime", endTime);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getStockChargeBillListByTimeRegion", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			List<StockChargeBill> list = (List<StockChargeBill>)rm.getResult();
			return list;
		}
		return null;
	}

}
