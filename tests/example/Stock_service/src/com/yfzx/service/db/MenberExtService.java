package com.yfzx.service.db;

import java.util.List;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.MenberExt;
import com.stock.common.model.Topic;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class MenberExtService {
	static DBAgent dbAgent = DBAgent.getInstance();
	private static final String MENBEREXT_BASE_NS = "com.stock.common.model.MenberExt";
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static MenberExtService instance = new MenberExtService();
	private MenberExtService() {

	}

	public static MenberExtService getInstance() {
		return instance;
	}
	
	/**
	 * 插入用户认证信息
	 * @param me
	 * @return
	 */
	public boolean insert(MenberExt me){
		RequestMessage req = DAFFactory.buildRequest(MENBEREXT_BASE_NS + ".insertUserExt", me, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 修改审核状态
	 * @param me
	 * @return
	 */
	public boolean updateStatus(MenberExt me) {
		RequestMessage req = DAFFactory.buildRequest(MENBEREXT_BASE_NS + ".updateStatus", me, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		return rm.getResult()==null? false:true;
	}
	
	/**
	 * 查询单个
	 * @param usubject
	 * @return
	 */
	public MenberExt fetchSingleByCondition(MenberExt me) {
		RequestMessage req = DAFFactory.buildRequest(MENBEREXT_BASE_NS + ".selectSingleByCondition", me, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (MenberExt)res.getResult();
	}
	/**
	 * 查询realname is not null List
	 * @param usubject
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<MenberExt> fetchListByCondition(MenberExt me) {
		RequestMessage req = DAFFactory.buildRequest(MENBEREXT_BASE_NS + ".selectListByCondition", me, StockConstants.common);
		ResponseMessage res = dbAgent.queryForList(req);
		return (List<MenberExt>)res.getResult();
	}
	
	
	/**
	 * 插入email
	 * @param me
	 * @return
	 */
	public boolean insertEmail(MenberExt me) {
		RequestMessage req = DAFFactory.buildRequest(MENBEREXT_BASE_NS + ".insertEmail", me, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}
	/**
	 * 更新用户认证信息
	 * @param menberext
	 * @return
	 */
	public boolean update(MenberExt menberext) {
		RequestMessage req = DAFFactory.buildRequest(MENBEREXT_BASE_NS + ".update", menberext, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		return rm.getResult()==null? false:true;
	}

	/**
	 * 根据条件查询
	 * @param menberext
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<MenberExt> fetchListByCondition2(MenberExt menberext) {
		RequestMessage req = DAFFactory.buildRequest(MENBEREXT_BASE_NS + ".selectListByCondition2", menberext, StockConstants.common);
		ResponseMessage res = dbAgent.queryForList(req);
		return (List<MenberExt>)res.getResult();
	}

}
