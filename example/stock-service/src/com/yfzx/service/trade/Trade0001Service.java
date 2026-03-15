package com.yfzx.service.trade;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Trade0001;
import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.cache.DataLoadTimeMng;
import com.yfzx.service.db.CompanyService;
import com.yz.common.vo.BaseVO;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class Trade0001Service {
	Logger log = LoggerFactory.getLogger(this.getClass());
	static Trade0001Service instance = new Trade0001Service();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	public Trade0001Service() {

	}

	public static Trade0001Service getInstance() {
		return instance;
	}
	
	
	public Trade0001 getTrade0001FromCache(String k) {
		Trade0001 ret = null;
		if (!StringUtil.isEmpty(k))
			ret = LCEnter.getInstance().get(k, StockUtil.getTrade0001Cache(k));
		return ret;
	}
	/**
	 * 取最近一条公司的交易数据
	 * 
	 * @param
	 * @return
	 */
	public Trade0001 getLatestTrade0001FromDB(String companycode) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companycode);
		String sqlMapKey = "getLatestTrade0001FromDB";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, m,
				StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Trade0001) value;
	}
	
	public Trade0001 getLatestTrade0001(String companycode) {
		String key = StockUtil.getVoKey(companycode,
				DateUtil.getDayStartTime(new Date()));
		Trade0001 ret = getTrade0001FromCache(key);
		if (ret == null) {
			ret = getLatestTrade0001FromDB(companycode);
			if (ret != null) {
				LCEnter.getInstance().put(key, ret,
						StockUtil.getTrade0001Cache(key));
			}
		}
		return ret;

	}

	public List<BaseVO> loadDataFromDBSetCompany(String companycode, Date stime) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("companycode", companycode);
		m.put("stime", DateUtil.format2String(stime));
		RequestMessage req = DAFFactory.buildRequest(
				"loadDataFromDBSetCompanyWithtime", m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<BaseVO>) value;
	}
	
	
	/**
	 * 写入每天的指数数据
	 * 调用者：实时中心的RealDataComputeTimer.addTimeTask
	 */
	public void replaceAZs(Trade0001 trade0001){
//		String zss = ConfigCenterFactory.getString(
//				"realtime_server.zs_code_list", "000001.sh,000002.sh");
//		String[] zsArr = zss.split(",");
//		for(String zs : zsArr){	
//			String companyCode = zs;
//			//TODO 根据code得到今天的数据
//			Trade0001 trade0001 = new Trade0001();
		if(trade0001 == null){
			return;
		}
		if(StringUtil.isEmpty(trade0001.getCompanyCode())){
			return;
		}
		RequestMessage req = DAFFactory.buildRequest("com.stock.common.model.Trade0001.repalce_A", trade0001, StockConstants.common);
		ResponseMessage rm = DBAgent.getInstance().createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode) ==false){
			log.error("replaceHK 失败"+trade0001);
		}
//		}
	}
	
	/**
	 * 写入每天的港股数据
	 * 调用者：实时中心的RealDataComputeTimer.addTimeTask
	 * TODO 注意 非交易日不要进行写，停牌时不要进行写
	 */
	public void replaceHK(Trade0001 trade0001){
//		List<USubject> cl = USubjectService.getInstance().getUSubjectListHStock();
//		for(USubject us : cl){	
//			String companyCode = us.getUidentify();
			
//		Trade0001 trade0001 = new Trade0001();
		if(trade0001 == null){
			return;
		}
		if(StringUtil.isEmpty(trade0001.getCompanyCode())){
			return;
		}
		if(trade0001.getF003n() == 0){
			//如果今日开盘是0，则不写数据库
			return;
		}
		RequestMessage req = DAFFactory.buildRequest("com.stock.common.model.Trade0001.repalce_HK", trade0001, StockConstants.common);
		ResponseMessage rm = DBAgent.getInstance().createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode) ==false){
			log.error("replaceHK 失败"+trade0001);
		}		
//		}
	}
	

	public void loadOneCompanyData2Cache(String companycode, Date stime) {
		List<BaseVO> retlist = Trade0001Service.getInstance()
				.loadDataFromDBSetCompany(companycode, stime);
		if (retlist != null)
			addData2Cache(retlist);
	}

	// public Date getHKPublishTimeFromDb(String uidentify) {
	// Map<String,String> m = new HashMap<String,String>();
	// m.put("companycode", uidentify);
	// String sqlMapKey = "getHKPublishTimeFromDb";
	// RequestMessage req = DAFFactory.buildRequest(
	// sqlMapKey, m, StockConstants.common);
	// Object value = pLayerEnter.queryForObject(req);
	// if (value == null) {
	// return null;
	// }
	// return (Date) value;
	// }

	private void addData2Cache(List<BaseVO> dlist) {

		if (dlist != null) {

			for (BaseVO vo : dlist) {
				try {
					// 去掉通用报表中的金融类公司
					Trade0001 av = (Trade0001) vo;
					if (av.getCompanyCode().indexOf(".") < 0)
						continue;
					Company c = CompanyService.getInstance()
							.getCompanyByCodeFromCache(av.getCompanyCode());
					if (c == null)
						continue;
					if (av.getTime() != null)
						DataLoadTimeMng.getInstance().putDataLoadTime(
								av.getCompanyCode(),
								StockConstants.INDEX_CODE_TRADE_CJL,
								av.getTime());
					String cacheName = StockUtil.getTrade0001Cache(vo.getKey());
					LCEnter.getInstance().put(vo.getKey(), vo, cacheName);
				} catch (Exception e) {
					log.error("addData2Cache failed!", e);
				}
			}

		}

	}
}
