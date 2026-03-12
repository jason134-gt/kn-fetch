package com.yfzx.service.db;

import java.util.List;

import com.stock.common.constants.StockConstants;
import com.stock.common.constants.TopicConstants;
import com.stock.common.model.TopicHost;
import com.stock.common.util.StockUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class TopicHostService {
	static DBAgent dbAgent = DBAgent.getInstance();
	private static final String TOPICHOST_BASE_NS = "com.stock.common.model.TopicHost";
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static TopicHostService instance = new TopicHostService();
	private TopicHostService() {

	}

	public static TopicHostService getInstance() {
		return instance;
	}
	
	
	
	//修改状态
	public int updateStatus(TopicHost topicHost) {
		RequestMessage reqMsg = DAFFactory.buildRequest(TOPICHOST_BASE_NS+".updateStatus",topicHost, StockConstants.common);
		String retrunCode = dbAgent.modifyRecord(reqMsg).getRetrunCode();
		
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return 1;
		}else{
			return 0;
		}
	}

	//根据话题ID查询该话题主持人信息
	public TopicHost selectTopicHostByUidentify(TopicHost topicHost) {
		RequestMessage req = DAFFactory.buildRequest(TOPICHOST_BASE_NS+"."+"selectByUidentify", topicHost, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		if(rm.getResult() == null){
			return null;
		}else{
			return (TopicHost)rm.getResult();
		}
	}
	
	//插入话题主持人信息
	public int insertTopicHost(TopicHost topicHost) {
		RequestMessage req = DAFFactory.buildRequest(TOPICHOST_BASE_NS+"."+"insert", topicHost, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return 1;
		}
		return -2;
	}
	
	//查询所有话题主持人
	@SuppressWarnings("unchecked")
	public List<TopicHost> selectAllHostInfoOrderByTitile() {
		RequestMessage req = DAFFactory.buildRequest(TOPICHOST_BASE_NS+".selectList", StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		if(rm.getResult() == null){
			return null;
		}else{			
			return (List<TopicHost>)rm.getResult();
		}	
	}
	
	//查询当前话题的话题主持人（包括没有审核通过的）
	@SuppressWarnings("unchecked")
	public  List<TopicHost> selectlistTopicHostByUidentify(TopicHost topicHost) {
		RequestMessage req = DAFFactory.buildRequest(TOPICHOST_BASE_NS+".selectListByUidentify",topicHost, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		if(rm.getResult() == null){
			return null;
		}else{			
			return (List<TopicHost>)rm.getResult();
		}	
	}
	
	//联合查询 已经通过审核的话题 以及对应的主持人信息
	@SuppressWarnings("unchecked")
	public List<TopicHost>  selectHasPassed(){
		RequestMessage req = DAFFactory.buildRequest(TOPICHOST_BASE_NS+".selectAllhasPassed", StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		if(rm.getResult() == null){
			return null;
		}else{			
			return (List<TopicHost>)rm.getResult();
		}	
	}
	
	//查询该话题是否有通过审核的主持人
	public TopicHost selectHasHost(TopicHost topicHost) {
		RequestMessage req = DAFFactory.buildRequest(TOPICHOST_BASE_NS+"."+"selectHasHost", topicHost, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		if(rm.getResult() == null){
			return null;
		}else{
			return (TopicHost)rm.getResult();
		}
	}

	public int replaceIntoTopicHost(TopicHost topicHost) {
		RequestMessage req = DAFFactory.buildRequest(TOPICHOST_BASE_NS+"."+"replce", topicHost, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return 1;
		}
		return -2;
	}

	public List<TopicHost> fetchFromLcCache(String uidentify) {
		List<TopicHost>  list = LCEnter.getInstance().get(uidentify, CacheUtil.getCacheName(TopicConstants.TOPICHOST));
		return list;
	}

}
