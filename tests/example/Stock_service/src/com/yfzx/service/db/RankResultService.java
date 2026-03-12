package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.RankResultItem;
import com.stock.common.model.Ranking;
import com.stock.common.model.Rankresult;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
public class RankResultService {

	private static RankResultService instance = new RankResultService();
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger log = LoggerFactory.getLogger(this.getClass());


	private RankResultService() {

	}

	public static RankResultService getInstance() {
		return instance;
	}


	

	public String creatRank(Rankresult rankresult) {
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"com.stock.common.model.Rankresult.insert", rankresult, StockConstants.DATA_TYPE_rankresult);
		return pLayerEnter.insert(reqMsg);
	}

	public String saveRank(Rankresult rankresult) {
		Rankresult rr = queryRankresultByCode(rankresult.getRankCode(),DateUtil.getSysDate(DateUtil.YYYYMMDD, rankresult.getRankPeriod()));
		if(rr!=null)
		{
			return updateCompanysByCode(rankresult);
		}
		else
		{
			return creatRank(rankresult);
		}
		
	}
	
	public String updateCompanysByCode(Rankresult rankresult) {
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"updateCompanysByCode", rankresult, StockConstants.DATA_TYPE_rankresult);
		return pLayerEnter.modify(reqMsg);
	}

	public Rankresult queryRankresultByName(String rankName, String period) {
		Map<String,String>  vm = new HashMap<String,String>();
		vm.put("rankName", rankName);
		vm.put("period", period);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"queryRankresultByName", vm,StockConstants.DATA_TYPE_rankresult);
		Object o = pLayerEnter.queryForObject(reqMsg);
		if (o == null) {
			return null;
		}
		return (Rankresult) o;
	}
	
	public Rankresult queryRankresultByCode(Integer rankCode, String period) {
		Map<String,Object>  vm = new HashMap<String,Object>();
		vm.put("rankCode", rankCode);
		vm.put("period", period);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"queryRankresultByCode", vm,StockConstants.DATA_TYPE_rankresult);
		Object o = pLayerEnter.queryForObject(reqMsg);
		if (o == null) {
			return null;
		}
		return (Rankresult) o;
	}

	public List<RankResultItem> getListCompanys(String rankname,String period)
	{
		if(StringUtil.isEmpty(rankname)) return null;
		Rankresult rr = queryRankresultByName(rankname,period);
		List<RankResultItem> rl = buildRankResultItemList(rr);
		return rl;
	}
	
	public List<RankResultItem> getListCompanys(Integer rankCode,String period)
	{
		if(rankCode==null) return null;
		Rankresult rr = queryRankresultByCode(rankCode,period);
		List<RankResultItem> rl = buildRankResultItemList(rr);
		return rl;
	}

	private List<RankResultItem> buildRankResultItemList(Rankresult rr) {
		List<RankResultItem> rl = new ArrayList<RankResultItem>();
		if(rr!=null)
		{
			String companys = rr.getRankCompanys();
			if(!StringUtil.isEmpty(companys))
			{
				String[] ca = companys.split(";");
				for(String cv : ca)
				{
					if(!StringUtil.isEmpty(cv))
					{
						String cc = cv.split(":")[0];
						String v = cv.split(":")[1];
						Company c = CompanyService.getInstance().getCompanyByCodeFromCache(cc);
						if(c!=null)
						{
							RankResultItem rri = new RankResultItem();
							rri.setCompanycode(cc);
							rri.setCompanyname(c.getSimpileName());
							if(!StringUtil.isEmpty(v))
								rri.setValue(Double.valueOf(v));
							rri.setPeriod(DateUtil.getSysDate(DateUtil.YYYYMMDD, rr.getRankPeriod()));
							rl.add(rri);
						}
					}
				}
			}
		}
	return rl;
		
	}

	//计算某一期的某个榜单
	public String computeOneRankresult(Integer rankingCode,Date period) {
		Map<String,Double> sm =  new ConcurrentSkipListMap<String, Double>(new Comparator<String>(){
			public int compare(String s0, String s1) {
		
				Double d1 = Double.valueOf(s0.split(":")[1]);
				Double d2 = Double.valueOf(s1.split(":")[1]);
				if(d2>=d1) return 1;
				return -1;
			}
			
		} );
		List<Company> cl = CompanyService.getInstance().getCompanyList();
		for(Company c: cl)
		{
			//加载某个公司的扩展表数据
			Ranking r = RankingService.getInstance().queryRankingByCode(rankingCode);
			Double v = computeRanking(r.getRankingRule(),period,c);
			if(v!=null&&v!=0)
			{
				sm.put(c.getCompanyCode()+":"+v, v);
			}
			
		}
		return toStringRankResult(sm);
	}

	private String toStringRankResult(Map<String, Double> sm) {
		int i=0;
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String,Double>> iter = sm.entrySet().iterator();
		while(iter.hasNext()&&i<100)
		{
			Entry<String,Double> entry = iter.next();
			String companyCode = entry.getKey().split(":")[0];
			sb.append(companyCode);
			sb.append(":");
			sb.append(entry.getValue());
			sb.append(";");
			i++;
		}
		return sb.toString();
	}

	private Double computeRanking(String rule, Date period, Company c) {
		IndexMessage im = SMsgFactory.getUDCIndexMessage(c.getCompanyCode());
		im.setTime(period);
		im.setCompanyCode(c.getCompanyCode());
		return CRuleService.getInstance().computeIndex(rule, im,StockConstants.DEFINE_INDEX);
		
	}

	public void computeOneRankresultAndSave(Integer rankingCode, Date time) {
		String rrs = computeOneRankresult(rankingCode, time);
		if(!StringUtil.isEmpty(rrs))
		{
			Ranking rank = RankingService.getInstance().queryRankingByCode(rankingCode);
			Rankresult rr = new Rankresult();
			rr.setRankCode(rankingCode);
			rr.setRankCompanys(rrs);
			rr.setRankPeriod(time);
			rr.setTableSystemCode(rank.getTableSystemCode());
			saveRank(rr);
		}
		
	}

	public List<Ranking> queryRankingNameList(String time) {
		Map<String,Object>  vm = new HashMap<String,Object>();
		vm.put("time", time);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"queryRankingNameList", vm,StockConstants.DATA_TYPE_rankresult);
		Object o = pLayerEnter.queryForList(reqMsg);
		if (o == null) {
			return null;
		}
		return (List<Ranking>) o;
	}
	
}
