/**
 *
 */
package com.yfzx.service.db.user;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.QueryParam;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.UserStock;
import com.stock.common.model.user.UserStockKey;
import com.stock.common.remind.BaseRemindFunction;
import com.stock.common.remind.RemindFacade;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.msgpush.MobileMsgPushService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * 带提醒功能的自选股
 * @author tangbinqi
 *
 */
public class UserStockService {
	private final static String BASE_NS = "com.stock.portal.dao.UserStock.UserStockDao";
	private static DBAgent dbAgent = DBAgent.getInstance();
	private static Logger logger = LoggerFactory.getLogger(UserStockService.class);
	private static UserStockService instance = new UserStockService();

	private UserStockService(){
	}

	public static UserStockService getInstance(){
		return instance;
	}

	/**
	 * 用户增加自选股使用
	 * @param model
	 * @return
	 */
	public Long insert(UserStock model){
		Long reLong = Long.valueOf("0");
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", model, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			UserStock reUserStock = (UserStock)rm.getResult();
			reLong = reUserStock.getUid();
		}
		return reLong;
	}

	/**
	 * 增加提醒功能使用
	 * @param model
	 * @return
	 */
	public boolean update(UserStock model){
		try {
			RequestMessage reqMsg = DAFFactory.buildRequest(BASE_NS+".updateByPrimaryKey",model, StockConstants.common);
			String retrunCode = dbAgent.modifyRecord(reqMsg).getRetrunCode();
			if(BaseCodes.SUCCESS.equals(retrunCode)){
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
			logger.error("operator failed!", e);
			return false;
		}
	}

	public boolean addStock(String stockcode, long uid) {
		if(StringUtils.isNotBlank(stockcode)) {
			stockcode = stockcode.replaceAll("\\s+", "").toLowerCase();
		} else {
			return false;
		}
		boolean result = false;
		int needInUserStockTable = ConfigCenterFactory.getInt("stock_zjs.NeedInUserStockTable", 1);
		boolean isExistInUserstockDb = needInUserStockTable == 1 ? (getSelect(uid, stockcode) != null ? true : false) : false;
		Date date = new Date();
		if(isExistInUserstockDb == false){//不存在重复，即插入
			UserStock us = new UserStock();
			us.setUid(uid);
			us.setCompanycode(stockcode);
			us.setAddTime(date);
			result = needInUserStockTable == 1 ? insert(us) > 0 : true;
			logger.info("pcaddStockToDb: " + needInUserStockTable + "   " + isExistInUserstockDb + "   " + result + "    " + uid);
			if(result) {
				result = StockSeqService.getInstance().addStockSeq(uid, stockcode, date);
				logger.info("pcaddStockToSeq: " + result  + "    " + uid);
				if(result) {
					MobileMsgPushService.getInstance().saveSubscribeUnSubscribeMsg(uid, stockcode, 1, 1);
					result = UserServiceClient.getInstance().addStockSeq(uid, stockcode, date);
					logger.info("pcaddStockToDcss: " + result  + "    " + uid);
				}
			}
		} else {
			logger.info("pcaddStockExistIndb: " + isExistInUserstockDb);
			if(StockSeqService.getInstance().addStockSeq(uid, stockcode, date)) {
				MobileMsgPushService.getInstance().saveSubscribeUnSubscribeMsg(uid, stockcode, 1, 1);
				logger.info("pcaddStockToSeq: " + result  + "    " + uid);
				result = UserServiceClient.getInstance().addStockSeq(uid, stockcode, date);
				logger.info("pcaddStockToDcss: " + result  + "    " + uid);
			}
		}

		return result;
	}

	public boolean delStock(String stockcode, long uid) {
		if(StringUtils.isNotBlank(stockcode)) {
			stockcode = stockcode.replaceAll("\\s+", "").toLowerCase();
		} else {
			return false;
		}
		boolean result = false;
		UserStock us = new UserStock();
		us.setUid(uid);
		us.setCompanycode(stockcode);
		Date updateTime = new Date();
		int needInUserStockTable = ConfigCenterFactory.getInt("stock_zjs.NeedInUserStockTable", 1);
		result = needInUserStockTable == 1 ? delete(us) : true;
		logger.info("pcDelStock: " + needInUserStockTable + "   " + result + "    " + uid);
		if(StockSeqService.getInstance().delStockSeq(uid, stockcode, updateTime)) {
			MobileMsgPushService.getInstance().saveSubscribeUnSubscribeMsg(uid, stockcode, -1, 1);
			logger.info("pcDelStockSeqDbSuccess");
			result = UserServiceClient.getInstance().delStockSeq(uid, stockcode, updateTime);
			logger.info("pcDelStockSeqDcss " + result);
		}

		return result;
	}

	/**
	 * 删除自选股使用
	 * @param model
	 * @return
	 */
	public boolean delete(UserStock model ){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".deleteByPrimaryKey",model, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		if (!rm.getRetrunCode().equals(BaseCodes.SUCCESS))
        {
			logger.error(rm.getRetrunCode());
			return false;
        }
		return true;
	}

	/**
	 * 查询某个公司 多少用户关注
	 * @param companyCode
	 * @return
	 */
	public int getSelectCount(String companyCode){
		UserStock usk = new UserStock();
		usk.setCompanycode(companyCode);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectCount",usk, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		if(rm.getResult() == null){
			return 0;
		}else{
			return Integer.parseInt(String.valueOf(rm.getResult()));
		}
	}

	/**
	 * 分页查询  某个公司 有哪些用户关注
	 * @param companyCode
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<UserStock> getSelectByPage(String companyCode,int start,int limit){
		QueryParam qp = new QueryParam();
		qp.setSort("add_time");
		qp.setDir("asc");
		qp.setStart(start);
		qp.setLimit(limit);
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("companycode", companyCode);
		map.put("queryParam", qp);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectList",map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		if(rm.getResult() == null){
			return null;
		}else{
			List<UserStock> roleList = (List<UserStock>)rm.getResult();
			return roleList;
		}
	}

	/**
	 * 获取某个用户的自选股[多个]等信息
	 * @return
	 */
	public List<UserStock> getSelect(Long uid){
		UserStock model = new UserStock();
		model.setUid(uid);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".select",model, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		if(rm.getResult() == null){
			return null;
		}else{
			@SuppressWarnings("unchecked")
			List<UserStock> roleList = (List<UserStock>)rm.getResult();
			return roleList;
		}
	}

	public UserStock getSelect(Long uid,String companyCode){
		UserStockKey usk = new UserStockKey();
		usk.setUid(uid);
		usk.setCompanycode(companyCode);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectByPrimaryKey",usk, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		if(rm.getResult() == null){
			return null;
		}else{
			UserStock us = (UserStock)rm.getResult();
			return us;
		}
	}
	/**
	 * 获取add_time最近的股票
	 */
	public UserStock selectEarliestStock(Long uid) {
		UserStock userStock = new UserStock();
		userStock.setUid(uid);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectEarliestStock",userStock, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		if(rm.getResult() == null){
			return null;
		}else{
			return (UserStock)rm.getResult();
		}
	}

	public List<UserStock> selectStockListByUid(Long uid) {
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectStockListByUid",uid, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		if(rm.getResult() == null){
			return null;
		}else{
			return (List<UserStock>)rm.getResult();
		}
	}

	/**
	 * 测试使用 参数存在数据库中
	 * @param uid
	 * @param companyCode
	 */
	public void testRemind(Long uid,String companyCode){
		UserStockKey usk = new UserStockKey();
		usk.setUid(uid);
		usk.setCompanycode(companyCode);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectByPrimaryKey",usk, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		UserStock us = null;
		if(rm.getResult() != null){
			us = (UserStock)rm.getResult();
		}else{
			return;
		}

		if(us.getRemind1()!= null ){
			String str = us.getRemind1();
			BaseRemindFunction brf = RemindFacade.getRemind("remind1");
			String[] args = str.split(",");
			String ruleStr = brf.encode(args);
			System.out.println(ruleStr);
			brf.doProcess();
		}
		if(us.getRemind5()!= null ){
			String str = us.getRemind5();
			BaseRemindFunction brf = RemindFacade.getRemind("remind5");
			String[] args = str.split(",");
			String ruleStr = brf.encode(args);
			System.out.println(ruleStr);
		}
		if(us.getRemind9()!= null ){
			String str = us.getRemind9();
			BaseRemindFunction brf = RemindFacade.getRemind("remind9");
			String[] args = str.split(",");
			String ruleStr = brf.encode(args);
			System.out.println(ruleStr);
		}
	}

}
