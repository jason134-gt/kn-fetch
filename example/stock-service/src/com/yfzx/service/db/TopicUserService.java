package com.yfzx.service.db;

import java.util.List;

import com.stock.common.constants.StockConstants;
import com.stock.common.constants.TopicConstants;
import com.stock.common.model.TopicUser;
import com.yfzx.service.client.TopicServiceClient;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class TopicUserService {
	static DBAgent dbAgent = DBAgent.getInstance();
	private static final String TOPICUSER_BASE_NS = "com.stock.common.model.TopicUser";
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static TopicUserService instance = new TopicUserService();
	private TopicUserService() {

	}

	public static TopicUserService getInstance() {
		return instance;
	}
	
	/**
	 * 插入话题 - 用户
	 * @param tu
	 * @return
	 */
	public boolean insert(TopicUser tu){
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".insert", tu, StockConstants.common);
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
	public List<TopicUser> selectList(TopicUser tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".selectList", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<TopicUser> list = (List<TopicUser>)rm.getResult();
		return list;
	}
	
	/**
	 * 修改审核状态
	 * @param tu
	 * @return
	 */
	public boolean updateStatus(TopicUser tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".update", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		return rm.getResult()==null? false:true;
	}
	
	/**
	 * 查询单个
	 * @param usubject
	 * @return
	 */
	public TopicUser fetchSingleByCondition(TopicUser tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".selectSingleByCondition", tu, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (TopicUser)res.getResult();
	}
	
	/**
	 * 查询单个
	 * @param usubject
	 * @return
	 */
	public TopicUser fetchSingleByIdentify(TopicUser tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".selectSingleByIdentify", tu, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (TopicUser)res.getResult();
	}
	
	
	/**
	 * 查询是否存在当前话题(缓存)
	 * @param usubject
	 * @return
	 */
	public TopicUser isExistFromCache(String uiditify){
		TopicUser u = LCEnter.getInstance().get(uiditify,CacheUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
		return u ;
	}

	/**
	 * 根据状态 查询 话题和 用户信息
	 * @param tu
	 * @return
	 */
	public List<TopicUser> fetchListByCondition(TopicUser tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".selectListByCondition", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<TopicUser> list = (List<TopicUser>)rm.getResult();
		return list;
	}

	public boolean isExist(TopicUser tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".selectCount", tu, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Integer)res.getResult()>0;
	}

	public List<TopicUser> fetchListByCondition2(TopicUser tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".selectListByCondition2", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<TopicUser> list = (List<TopicUser>)rm.getResult();
		return list;
	}
	public List<TopicUser> fetchListFromCache() {
		List<TopicUser> list = TopicServiceClient.getInstance().getTopicUserListFromCache();
		return list;
	}
	public List<TopicUser> fetchListFromCacheByUid(long uid) {
		List<TopicUser> list =  TopicServiceClient.getInstance().getTopicUserByUidFromCache(uid);
		return list;
	}
	public TopicUser fetchSingleFromCache(String identify) {
		TopicUser tu = TopicServiceClient.getInstance().getTopicUserFromCache(identify);
		return tu;
	}
	public List<TopicUser> fetchListByCondition3(TopicUser tu) {
		RequestMessage req = DAFFactory.buildRequest(TOPICUSER_BASE_NS + ".selectListByCondition3", tu, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<TopicUser> list = (List<TopicUser>)rm.getResult();
		return list;
	}
	
	
}
