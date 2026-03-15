package com.yfzx.service.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Ranking;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

public class RankingService {

	private static RankingService instance = new RankingService();
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger log = LoggerFactory.getLogger(this.getClass());


	private RankingService() {

	}

	public static RankingService getInstance() {
		return instance;
	}




	public String saveRanking(Ranking rank) {
		String ret = StockCodes.FAILED;
		if(StringUtil.isEmpty(rank.getRankingName()))
			return ret;
		Ranking dbRank = queryRankingByCode(rank.getRankingCode());
		if(dbRank!=null)
		{
			if(!StringUtil.isEmpty(rank.getRankingComments()))
				dbRank.setRankingComments(rank.getRankingComments());
			if(!StringUtil.isEmpty(rank.getRankingComments()))
				dbRank.setRankingRule(StockUtil.getRuleByComments(rank.getRankingComments()));
			if(!StringUtil.isEmpty(rank.getRankingName()))
				dbRank.setRankingName(rank.getRankingName());
			ret = modifyRankByCode(dbRank);
			
		}
		else
		{
			if(!StringUtil.isEmpty(rank.getRankingComments()))
				rank.setRankingRule(StockUtil.getRuleByComments(rank.getRankingComments()));
			ret = creatRank(rank);
		}
		return ret;
	}



	public Ranking queryRankByName(String rankingName) {
		Map<String,String>  vm = new HashMap<String,String>();
		vm.put("rankingName", rankingName);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"queryRankByName", vm,StockConstants.DATA_TYPE_ranking);
		Object o = pLayerEnter.queryForObject(reqMsg);
		if (o == null) {
			return null;
		}
		return (Ranking) o;
	}

	private String creatRank(Ranking rank) {
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"com.stock.common.model.Ranking.insert", rank, StockConstants.DATA_TYPE_ranking);
		return pLayerEnter.insert(reqMsg);
	}

	private int getMaxRankcode() {
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"getMaxRankcode", StockConstants.DATA_TYPE_ranking);
		Object o = pLayerEnter.queryForObject(reqMsg);
		if (o == null) {
			return -1;
		}
		Map m = (Map) o;
		Object mo = m.get("max(ranking_code)");
		if (mo != null) {
			return Integer.valueOf(mo.toString());
		}
		return -1;
	}

	private String modifyRankByCode(Ranking dbRank) {
	
		
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"modifyRankByCode", dbRank, StockConstants.DATA_TYPE_ranking);
		return pLayerEnter.modify(reqMsg);
	}

	public String modifyRanking(Ranking rank) {
		Ranking dbRank = queryRankingByCode(rank.getRankingCode());
		if(dbRank==null) 
		{
			log.info("rank not exsit! rankcode = "+rank.getRankingCode());
			return StockCodes.FAILED;
		}
		
		if(!StringUtil.isEmpty(rank.getRankingComments()))
			dbRank.setRankingComments(rank.getRankingComments());
		if(!StringUtil.isEmpty(rank.getRankingComments()))
			dbRank.setRankingRule(StockUtil.getRuleByComments(rank.getRankingComments()));
		if(!StringUtil.isEmpty(rank.getRankingName()))
			dbRank.setRankingName(rank.getRankingName());
		return modifyRankByCode(dbRank);
	}



	public Ranking queryRankingByCode(Integer rankingCode) {
		Map<String,Integer>  vm = new HashMap<String,Integer>();
		vm.put("rankingCode", rankingCode);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"queryRankingByCode", vm,StockConstants.DATA_TYPE_ranking);
		Object o = pLayerEnter.queryForObject(reqMsg);
		if (o == null) {
			return null;
		}
		return (Ranking) o;
	}

	public List<Ranking> queryAllRanking() {

		RequestMessage reqMsg = DAFFactory.buildRequest(
				"queryAllRanking",StockConstants.DATA_TYPE_ranking);
		Object o = pLayerEnter.queryForList(reqMsg);
		if (o == null) {
			return null;
		}
		return (List<Ranking>) o;
	}

	public String delRankingBycode(Integer rankingCode) {
		Map<String,Integer> m = new HashMap<String,Integer>();
		m.put("rankingCode", rankingCode);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"delRankingBycode", m, StockConstants.DATA_TYPE_ranking);
		return pLayerEnter.delete(reqMsg);
	}
	
	
	public List<Ranking> queryRankingByTableSystemCode(String tsc)
	{
		Map<String,String>  vm = new HashMap<String,String>();
		vm.put("tableSystemCode", tsc);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"queryRankingByTableSystemCode",vm,StockConstants.DATA_TYPE_ranking);
		Object o = pLayerEnter.queryForList(reqMsg);
		if (o == null) {
			return null;
		}
		return (List<Ranking>) o;
	}
}
