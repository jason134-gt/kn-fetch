package com.yfzx.service.share;

import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.QueryParam;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.share.Report;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * @author tangbinqi
 * 举报服务
 */
public class ReportService {

	private final static String BASE_NS = "com.stock.portal.dao.Report.ReportDao";
	private static DBAgent dbAgent = DBAgent.getInstance();
	private static Logger logger = LoggerFactory.getLogger(ReportService.class);
	private static ReportService instance = new ReportService();
		
	private ReportService(){		
	}
	
	public static ReportService getInstance(){
		return instance;
	}
	
	/**
	 * 用户举报博文或评论
	 * @param model
	 * @return
	 */
	public Long insert(Report model){
		Long reLong = Long.valueOf("0");
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", model, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			Report reReport = (Report)rm.getResult();
			reLong = reReport.getId();
		}
		return reLong;
	}
	
	/**
	 * 修改举报信息 一般是后台管理员进行举报信息处理
	 * @param model
	 * @return
	 */
	public boolean update(Report model){		
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
	
	/**
	 * 删除举报信息 
	 * @param model
	 * @return
	 */
	public boolean delete(long id ){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".deleteByPrimaryKey",id, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		if (!rm.getRetrunCode().equals(BaseCodes.SUCCESS))
        {
			logger.error(rm.getRetrunCode());
			return false;
        }
		return true;
	}
	
	public Report getSelectFK(long id){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectByPrimaryKey", id, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		if(rm.getResult() == null){
			return null;
		}else{
			return (Report)rm.getResult();
		}
	}
	
	/**
	 * 查询举报信息 分页
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Report> getSelectList(Report report, Map pMap,QueryParam queryParam){		
		Map map = null;
		try {
			map = PropertyUtils.describe(report);			
			map.put("queryParam", queryParam);
			map.putAll(pMap);
		}catch (Exception e) {
			
		}
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+".selectList",map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		if(rm.getResult() == null){
			return null;
		}else{			
			List<Report> roleList = (List<Report>)rm.getResult();
			return roleList;
		}		
	}
	
	public int selectCount(Report report,Map pMap) throws Exception{	
		Map map = null;
		try {
			map = PropertyUtils.describe(report);			
			map.putAll(pMap);
		}catch (Exception e) {
			
		}
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectCount", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		int i = (Integer)rm.getResult();
		return i;		
	}
	
}
