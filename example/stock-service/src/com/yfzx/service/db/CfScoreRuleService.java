package com.yfzx.service.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Cfscorerule;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;



public class CfScoreRuleService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static CfScoreRuleService instance = new CfScoreRuleService();

	private CfScoreRuleService() {

	}

	public static CfScoreRuleService getInstance() {
		return instance;
	}

	


	public List<Cfscorerule> getCfScoreRuleById(Integer id) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", id);
		RequestMessage req = DAFFactory.buildRequest(
				"getCFScoreRuleListById",m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Cfscorerule>) value;
	}

	public void updateCfScoreRule(Cfscorerule cfsc) {
		Cfscorerule csfc = getCfScoreRuleByKey(cfsc.getId(),cfsc.getIndexName(),cfsc.getType());
		if(csfc==null)
		{
			insert(cfsc);
		}
		else
		{
			modify(cfsc);
		}
		
	}
	
	public Cfscorerule getCfScoreRuleByKey(Integer id, String indexname,int type) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", id);
		m.put("indexname", indexname);
		m.put("type", type);
		RequestMessage req = DAFFactory.buildRequest(
				"getCfScoreRuleByKey",m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Cfscorerule) value;
	}

	private void insert(Cfscorerule cfsc) {
		RequestMessage reqMsg = DAFFactory.buildRequest("insertCfScoreRule",cfsc,StockConstants.common);
		pLayerEnter.insert(reqMsg);
	}

	private void modify(Cfscorerule cfsc) {
		// TODO Auto-generated method stub
		RequestMessage reqMsg = DAFFactory.buildRequest("updateCfScoreRule",cfsc,StockConstants.common);
		pLayerEnter.modify(reqMsg);
	}

	public void deleteByPrimaryKey(Integer id,String indexname,int type) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", id);
		m.put("indexname", indexname);
		m.put("type", type);
		RequestMessage reqMsg = DAFFactory.buildRequest("deleteCfscoreruleByPrimaryKey",m,StockConstants.common);
		pLayerEnter.delete(reqMsg);
	}

	
}
