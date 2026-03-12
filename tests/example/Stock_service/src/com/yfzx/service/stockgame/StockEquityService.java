package com.yfzx.service.stockgame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.stockgame.StockEquity;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;


public class StockEquityService {
	private static final String NS = "com.stock.portal.dao.stockgame.StockEquityDao";

//	private static final Logger logger = LoggerFactory.getLogger(StockEquityService.class);

	private StockEquityService() {}

	private static StockEquityService instance = new StockEquityService();

	private static DBAgent dbAgent = DBAgent.getInstance();

	public static StockEquityService getInstance() {
		return instance;
	}

	public long insert(StockEquity stockEquity) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", stockEquity, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			StockEquity obj = (StockEquity)rm.getResult();
			return obj.getId();
		}
		return 0;
	}

	public boolean update(StockEquity stockEquity) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "updateByPrimaryKey", stockEquity, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean delete(StockEquity stockEquity) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "delete", stockEquity, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean deleteByPk(Long id) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "deleteByPrimaryKey", id, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public List<StockEquity> getStockEquityList(StockEquity stockEquity) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "select", stockEquity, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockEquity>)rm.getResult();
		} else {
			return null;
		}
	}
	public List<StockEquity> getStockEquityPagingList(Long uid, Integer offset, Integer limit) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("offset", offset);
		map.put("uid", uid);
		map.put("limit", limit);
		map.put("sort", "id");
		map.put("dir", "desc");
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectList", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockEquity>)rm.getResult();
		} else {
			return null;
		}
	}

	public int getStockEquityCount(Long uid) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("uid", uid);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectCount", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return rm.getResult() == null ? 0 : (Integer)rm.getResult();
		} else {
			return 0;
		}
	}

	public StockEquity getStockEquityByCodeUid(String stockcode, Long uid) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("stockCode", "'" + stockcode + "'");
		map.put("uid", uid);
//		map.put("andCon", " where stockCode='" + stockcode + "' and uid=" + uid);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectByCodeUid", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (StockEquity)rm.getResult();
		} else {
			return null;
		}
	}

}
