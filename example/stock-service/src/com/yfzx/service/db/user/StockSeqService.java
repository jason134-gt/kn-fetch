package com.yfzx.service.db.user;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.StockSeq;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.UserServiceClient;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class StockSeqService {
	private static final Logger logger = LoggerFactory.getLogger(StockSeqService.class);
	private static final String NS = "com.stock.common.model.user.StockSeqDao";
	private StockSeqService() {}

	private static StockSeqService instance = new StockSeqService();

	public static StockSeqService getInstance() {
		return instance;
	}

	private static DBAgent dbAgent = DBAgent.getInstance();

	public Long insert(StockSeq stockSeq) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", stockSeq, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			StockSeq obj = (StockSeq)rm.getResult();
			return obj.getId();
		}
		return Long.valueOf(0);
	}

	public boolean update(StockSeq stockSeq) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "updateByPrimaryKey", stockSeq, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean delete(StockSeq stockSeq) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "delete", stockSeq, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public StockSeq getStockSeqByUid(Long uid) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectByUid", uid, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (StockSeq)rm.getResult();
		} else {
			return null;
		}
	}

	public List<String> getUserStockSeqList(Long uid) {
		List<String> list = null;
		StockSeq stockSeq = UserServiceClient.getInstance().getStockSeq(uid);
		if(stockSeq != null && StringUtils.isNotBlank(stockSeq.getCodesSeq())){
			list = new ArrayList<String>();
			String[] codeArr = stockSeq.getCodesSeq().split(",");
			for(String stock:codeArr) {
				if(! list.contains(stock)) {
					list.add(stock);
				}
			}
		}
		return list;
	}

	public boolean addStockSeq(long uid, String stockcode, Date updateTime) {
		if(StringUtils.isNotBlank(stockcode)) {
			stockcode =  stockcode.replaceAll("\\s+", "").toLowerCase();
		} else {
			return false;
		}
		StockSeq stockSeq = getStockSeqByUid(uid);
		if(stockSeq == null) {
			stockSeq = new StockSeq();
			stockSeq.setUid(uid);
			stockSeq.setCodesSeq(stockcode);
			stockSeq.setUpdateTime(updateTime);
			return insert(stockSeq) > 0;
		} else {
			String stockCodesSeq = stockSeq.getCodesSeq();
			if(StringUtils.isNotBlank(stockCodesSeq)) {
				if(! stockCodesSeq.contains(stockcode)) {
					stockSeq.setCodesSeq(StockUtil.joinString(",", stockcode, stockSeq.getCodesSeq()));
				} else {
					return true;
				}
			} else {
				stockSeq.setCodesSeq(stockcode);
			}
			stockSeq.setUpdateTime(updateTime);
			return update(stockSeq);
		}
	}

	public boolean updateStockSeq(long uid, String stockcodes, Date updateTime) {
		StockSeq stockSeq = getStockSeqByUid(uid);
		logger.info("updateStockSeq: " + uid + "       " + stockcodes);
		if(stockSeq == null) {
			return false;
		} else {
			stockSeq.setCodesSeq(stockcodes);
			stockSeq.setUpdateTime(updateTime);
			return update(stockSeq);
		}
	}

	public boolean delStockSeq(long uid, String stockcode, Date updateTime) {
		StockSeq stockSeq = getStockSeqByUid(uid);
		if(stockSeq == null) {
			return false;
		} else {
			String stockCodesSeq = stockSeq.getCodesSeq();
			if(StringUtils.isNotBlank(stockCodesSeq)) {
				List<String> seqList = new ArrayList<String>();
				for(String stock : stockCodesSeq.split(",")) {
					if(StringUtils.isNotBlank(stock)) {
						if(! seqList.contains(stock)) {
							seqList.add(stock);
						}
					}
				}
				if(StringUtils.isNotBlank(stockcode)) {
					stockcode = stockcode.replaceAll("\\s+", "").toLowerCase();
				}
				seqList.remove(stockcode);
				stockSeq.setCodesSeq(StringUtils.join(seqList, ","));
				stockSeq.setUpdateTime(updateTime);
				return update(stockSeq);
			}  else {
				return true;
			}
		}
	}

	public boolean batchDelStockSeq(long uid, String stockcodes, Date updateTime) {
		StockSeq stockSeq = getStockSeqByUid(uid);
		if(stockSeq == null) {
			return false;
		} else {
			String stockCodesSeq = stockSeq.getCodesSeq();
			if(StringUtils.isNotBlank(stockCodesSeq)) {
				List<String> seqList = new ArrayList<String>();
				for(String stock : stockCodesSeq.split(",")) {
					if(StringUtils.isNotBlank(stock)) {
						if(! seqList.contains(stock)) {
							seqList.add(stock);
						}
					}
				}

				for(String code : stockcodes.split(",")) {
					if(StringUtils.isNotBlank(code)) {
						code =  code.replaceAll("\\s+", "").toLowerCase();
						seqList.remove(code);
					}
				}

				stockSeq.setCodesSeq(StringUtils.join(seqList, ","));
				stockSeq.setUpdateTime(updateTime);
				return update(stockSeq);
			}
			return true;
		}
	}

	public boolean isExistInDcss(Long uid, String stockcode) {
		StockSeq stockSeq = UserServiceClient.getInstance().getStockSeq(uid);
		if(stockSeq != null && StringUtils.isNotBlank(stockSeq.getCodesSeq()) && StringUtils.isNotBlank(stockcode)
				&& stockSeq.getCodesSeq().contains(stockcode.replaceAll("\\s+", "").toLowerCase())){
			return true;
		} else {
			return false;
		}
	}
}
