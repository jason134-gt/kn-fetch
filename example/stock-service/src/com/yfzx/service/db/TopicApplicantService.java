package com.yfzx.service.db;

import java.util.List;

import com.stock.common.constants.StockConstants;
import com.stock.common.constants.TopicConstants;
import com.stock.common.model.TopicApplicant;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class TopicApplicantService {
	static DBAgent dbAgent = DBAgent.getInstance();
	private static final String TOPICAPPLICAT_BASE_NS = "com.stock.common.model.TopicApplicant";
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static TopicApplicantService instance = new TopicApplicantService();
	private TopicApplicantService() {

	}

	public static TopicApplicantService getInstance() {
		return instance;
	}
	
	/**
	 * 插入话题 - 用户
	 * @param tu
	 * @return
	 */
	public boolean insert(TopicApplicant tu){
		RequestMessage req = DAFFactory.buildRequest(TOPICAPPLICAT_BASE_NS + ".insert", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 话题或者用户主键 查询
	 * @param tu
	 * @return
	 */
	public List<TopicApplicant> selectList(TopicApplicant tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICAPPLICAT_BASE_NS + ".selectList", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<TopicApplicant> list = (List<TopicApplicant>)rm.getResult();
		return list;
	}
	
	/**
	 * 修改审核状态
	 * @param tu
	 * @return
	 */
	public boolean updateStatus(TopicApplicant tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICAPPLICAT_BASE_NS + ".update", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		return rm.getResult()==null? false:true;
	}
	
	/**
	 * 查询单个
	 * @param usubject
	 * @return
	 */
	public TopicApplicant fetchSingleByCondition(TopicApplicant tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICAPPLICAT_BASE_NS + ".selectSingleByCondition", tu, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (TopicApplicant)res.getResult();
	}
	
	
	/**
	 * 查询是否存在当前话题(缓存)
	 * @param usubject
	 * @return
	 */
	public TopicApplicant isExistFromCache(String uiditify){
		TopicApplicant u = LCEnter.getInstance().get(uiditify,CacheUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
		return u ;
	}

	/**
	 * 根据状态 查询 话题和 用户信息
	 * @param tu
	 * @return
	 */
	public List<TopicApplicant> fetchListByCondition(TopicApplicant tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICAPPLICAT_BASE_NS + ".selectListByCondition", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<TopicApplicant> list = (List<TopicApplicant>)rm.getResult();
		return list;
	}

	public boolean isExist(TopicApplicant tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICAPPLICAT_BASE_NS + ".selectCount", tu, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Integer)res.getResult()>0;
	}

	public List<TopicApplicant> fetchListFromCache() {
		List<TopicApplicant> list = LCEnter.getInstance().get(TopicConstants.ALL_TOPIC_USER,CacheUtil.getCacheName(TopicConstants.TOPICUSER));
		return list;
	}
	public TopicApplicant fetchSingleFromCache(String identify) {
		TopicApplicant tu = LCEnter.getInstance().get(identify,CacheUtil.getCacheName(TopicConstants.TOPICUSER));
		return tu;
	}
	public List<TopicApplicant> fetchListByCondition3(TopicApplicant tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICAPPLICAT_BASE_NS + ".selectListByCondition3", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<TopicApplicant> list = (List<TopicApplicant>)rm.getResult();
		return list;
	}
	
	
}
