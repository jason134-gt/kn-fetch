package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Fund0002;
import com.stock.common.model.Fund002CountInfo;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class Fund0002Service {

	private static Fund0002Service instance = new Fund0002Service();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private Fund0002Service() {

	}

	public static Fund0002Service getInstance() {
		return instance;
	}

	public List<Fund0002> queryLatestFund0002List(Date time) {

		Map<String,String> m = new HashMap<String,String>();
		m.put("time", DateUtil.format2String(time));
		String sqlMapKey = "queryLatestFund0002List";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey,m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Fund0002>) value;
	}

	public void initFundZJLR()
	{
		try {
			Date time = StockUtil.getApproPeriod(new Date());
			List<Fund002CountInfo> fcl = LCEnter.getInstance().get(
					SCache.CACHE_KEY_JJ_XJ, SCache.CACHE_NAME_fund0002);
			if (fcl == null)
				return;
			for (Fund002CountInfo fc : fcl) {

				IndexMessage im = SMsgFactory.getUDCIndexMessage(fc.getCompanycode());
				im.setCompanyCode(fc.getCompanycode());
				im.setTime(DateUtil.getDayStartTime(new Date()));
				im.setNeedAccessExtIndexDb(false);
				im.setNeedAccessCompanyBaseIndexDb(false);
				im.setIndexCode("4707");
				Double cprice = IndexValueAgent.getIndexValue(im);
				if (cprice == null)
					cprice = 0.0;
				Double zjlr = fc.getCgsbd() * cprice;
				fc.setZjlr(zjlr);
			}
			List<Fund002CountInfo> ZJLR = new ArrayList<Fund002CountInfo>();
			//资金流入
			Collections.sort(fcl, new Comparator<Fund002CountInfo>() {

				public int compare(Fund002CountInfo o1, Fund002CountInfo o2) {
					// TODO Auto-generated method stub
					return o2.getZjlr().compareTo(o1.getZjlr());
				}
			});
			int size = fcl.size() >= 50 ? 50 : fcl.size();
			ZJLR.addAll(fcl.subList(0, size));
			List<Company> cl = toCompanyList(ZJLR);
			LCEnter.getInstance().put(SCache.CACHE_KEY_JJ_ZJLR_TOP50, cl,
					SCache.CACHE_NAME_COMPANY);
			CompanyService.getInstance().appendTag2Clist(
					SCache.CACHE_KEY_JJ_ZJLR_TOP50, cl);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public List<Company> toCompanyList(List<Fund002CountInfo> fcl) {
		List<Company> cl = new ArrayList<Company>();
		for(Fund002CountInfo fc:fcl)
		{
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(fc.getCompanycode());
			if(c==null) continue;
			cl.add(c);
		}
		return cl;
	}



	public List<Map> loadFundCountInfoFromDb(String companycode, Date uptime) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companyCode", companycode);
		m.put("uptime", DateUtil.format2String(uptime));		
		RequestMessage req = DAFFactory.buildRequest(
				"loadFundCountInfoFromDb", m, StockConstants.common);
		List list = pLayerEnter.queryForList(req);		
		return list;
	}

	public List<Map> loadFundCountInfoFromDbV2(String companycode, Date uptime) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companyCode", companycode);
		m.put("uptime", DateUtil.format2String(uptime));		
		RequestMessage req = DAFFactory.buildRequest(
				"loadFundCountInfoFromDbV2", m, StockConstants.common);
		List list = pLayerEnter.queryForList(req);		
		return list;
	}
	
	public List<Map> loadFundStockPercentFromDb(String companycode, Date uptime) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companyCode", companycode);
		m.put("uptime", DateUtil.format2String(uptime));		
		RequestMessage req = DAFFactory.buildRequest(
				"loadFundStockPercentFromDb", m, StockConstants.common);
		List list = pLayerEnter.queryForList(req);		
		return list;
	}
	
	
	public List<Map> loadFundStockPercentFromDbV2(String companycode, Date uptime) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("companyCode", companycode);
		m.put("uptime", DateUtil.format2String(uptime));		
		RequestMessage req = DAFFactory.buildRequest(
				"loadFundStockPercentFromDbV2", m, StockConstants.common);
		List list = pLayerEnter.queryForList(req);		
		return list;
	}
}
