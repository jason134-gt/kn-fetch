package com.yfzx.service.stockgame;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.stockgame.StockTransaction;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class StockTransactionService {

//	private static final Logger logger = LoggerFactory.getLogger(StockTransactionService.class);

	private static final String NS = "com.stock.portal.dao.stockgame.StockTransactionDao";

	private StockTransactionService() {}

	private static StockTransactionService instance = new StockTransactionService();

	public static StockTransactionService getInstance() {
		return instance;
	}

	private static DBAgent dbAgent = DBAgent.getInstance();

	public Long insert(StockTransaction stockTransaction) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", stockTransaction, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			StockTransaction obj = (StockTransaction)rm.getResult();
			return obj.getId();
		}
		return Long.valueOf(0);
	}

	public boolean update(StockTransaction stockTransaction) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "updateByPrimaryKey", stockTransaction, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean delete(StockTransaction stockTransaction) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "delete", stockTransaction, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public List<StockTransaction> getStockTransactionList(StockTransaction stockTransaction) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "select", stockTransaction, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockTransaction>)rm.getResult();
		} else {
			return null;
		}
	}

	public List<StockTransaction> getStockTransactionListByTime(Date startTime, Date endTime) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("startTime", startTime);
		map.put("endTime", endTime);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getStockTransactionListByTime", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockTransaction>)rm.getResult();
		} else {
			return null;
		}
	}

	private String getDateFormatter(Date date){
		String simple = "yyyy-MM-dd HH:mm:ss";
		DateFormat df=new SimpleDateFormat(simple);
		return df.format(date);
	}

	public List<StockTransaction> getStockTransactionList(Integer bargainType, Integer soldOut, Long uid, Date startTime, Date endTime) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("startTime", "'" + getDateFormatter(startTime) + "'");
		map.put("endTime", "'" + getDateFormatter(endTime) + "'");
		map.put("uid", uid);
		map.put("bargainType", bargainType);
		map.put("soldOut", soldOut);

		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectToCalc", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockTransaction>)rm.getResult();
		} else {
			return null;
		}
	}

	public boolean updateStockTransactionSoldOutStatus(String stockCode, Integer bargainType, Date bargainTime, Long uid) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("stockCode", stockCode);
		map.put("bargainType", bargainType);
		map.put("bargainTime", getDateFormatter(bargainTime));
		map.put("uid", uid);

		RequestMessage req = DAFFactory.buildRequest(NS + "." + "updateSoldOutStat", map, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		}
		return false;
	}

	public Integer getCount(StockTransaction stockTransaction) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectCount", stockTransaction, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (Integer)rm.getResult();
		}
		return 0;
	}

	public List<StockTransaction> getStockTransactionListByPage(Integer offset, Integer limit, Long uid) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("offset", offset);
		map.put("limit", limit);
		map.put("uid", uid);
		map.put("sort", "bargainTime");
		map.put("direction", "desc");
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectList", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<StockTransaction>)rm.getResult();
		} else {
			return null;
		}
	}
}
