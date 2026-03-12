package com.yfzx.service.db;

import java.util.List;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Topic;
import com.stock.common.model.TopicBackup;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class TopicBackupService {
	static DBAgent dbAgent = DBAgent.getInstance();
	private static final String TOPICBACKUP_BASE_NS = "com.stock.common.model.TopicBackup";
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static TopicBackupService instance = new TopicBackupService();
	private TopicBackupService() {

	}

	public static TopicBackupService getInstance() {
		return instance;
	}
	
	/**
	 * 插入新话题
	 * @param topicBackup
	 * @return
	 */
	public boolean insert(Topic topicBackup){
		RequestMessage req = DAFFactory.buildRequest(TOPICBACKUP_BASE_NS + ".insert", topicBackup, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 查询是否存在当前话题
	 * @param topicBackup
	 * @return
	 */
	public boolean isExist(Topic topicBackup){
		RequestMessage req = DAFFactory.buildRequest(TOPICBACKUP_BASE_NS + ".selectCount", topicBackup, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Integer)res.getResult()>0;
	}
	
	/**
	 * 查询所有自定义的话题
	 * @param topicBackup
	 * @return
	 */
	public List<Topic> selectAllCustomTopic(Topic topicBackup) {
		RequestMessage req = DAFFactory.buildRequest(TOPICBACKUP_BASE_NS + ".selectAllCustom", topicBackup, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Topic> list = (List<Topic>)rm.getResult();
		return list;
	}
	
	/**
	 * 修改话题
	 * @param topicBackup
	 * @return
	 */
	public boolean updateStatus(Topic topicBackup) {
		RequestMessage req = DAFFactory.buildRequest(TOPICBACKUP_BASE_NS + ".update", topicBackup, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;		
	}
	/**
	 * 删除话题
	 * @param topicBackup
	 * @return
	 */
	public boolean delete(Topic topicBackup) {
		RequestMessage req = DAFFactory.buildRequest(TOPICBACKUP_BASE_NS + ".delete", topicBackup, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;		
	}
	
	/**
	 * 查询单个
	 * @param usubject
	 * @return
	 */
	public Topic selectSinge(Topic topicBackup) {
		RequestMessage req = DAFFactory.buildRequest(TOPICBACKUP_BASE_NS + ".selectSinge", topicBackup, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Topic)res.getResult();
	}
	
	
	/**
	 * 查询是否存在当前话题(缓存)
	 * @param usubject
	 * @return
	 */
	public TopicBackup isExistFromCache(String uiditify){
		TopicBackup u = LCEnter.getInstance().get(uiditify,CacheUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
		return u ;
	}

	
	
}
