package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.List;

import org.jfree.util.Log;

import ch.qos.logback.classic.Logger;

import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Matchinfo;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;

public class MatchinfoService {

	private static MatchinfoService instance = new MatchinfoService();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private MatchinfoService() {

	}

	public static MatchinfoService getInstance() {
		return instance;
	}

	public List<Dictionary> getDataDictionaryList(String[] sa) {
		// TODO Auto-generated method stub
		List<Dictionary> ltm = new ArrayList<Dictionary>();

		for (String t : sa) {
			Dictionary tm = getDataDictionary(t);
			if (tm != null) {
				ltm.add(tm);
			}
		}

		return null;
	}

	public Dictionary getDataDictionary(String indexCode) {
		// TODO Auto-generated method stub
		Dictionary d = new Dictionary(indexCode);
		String sqlMapKey = StockUtil.getBuildSqlMapKey(d,
				StockSqlKey.dictionary_key_0);
		RequestMessage req = DAFFactory.buildRequest(d.getIndexCode(),
				sqlMapKey, d, StockConstants.dictionary);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Dictionary) value;
	}

	public List<Matchinfo> getMatchInfoByTsc(String tsc) {
		// TODO Auto-generated method stub
		return LCEnter.getInstance().get(tsc, StockConstants.MATCH_INFO_CACHE);
	}

	public String getDataSourceCodeByTsc(String tsc)
	{
		List<Matchinfo> ml = LCEnter.getInstance().get(tsc, StockConstants.MATCH_INFO_CACHE);
		if(ml!=null&&ml.size()>0)
		{
			return ml.get(0).getDataSourceCode();
		}
		return null;
	}
	public Matchinfo getMatchInfoByTableCode(String tableCode)
	{
		return LCEnter.getInstance().get(tableCode, StockConstants.MATCH_INFO_CACHE);
	}
	
	public boolean tscIsMatch(String tableCode,String tsc)
	{
		if(StringUtil.isEmpty(tableCode)||StringUtil.isEmpty(tsc))
		{
			return false;
		}
		if(getMatchInfoByTableCode(tableCode)==null)
		{
			System.out.println("get matchinfo is null! "+tableCode);
		}
		if(getMatchInfoByTableCode(tableCode).getTableSystemCode().equals(tsc))
			return true;
		return false;
	}
	// 查询子表编码
	public String getTableCodeByTsc(String tsc, int type) {
		List<Matchinfo> mil = MatchinfoService.getInstance().getMatchInfoByTsc(
				tsc);
		if (mil != null) {
			for (Matchinfo m : mil) {
				if (m.getSystemChildTableType().equals(String.valueOf(type))) {
					return m.getSystemChildTableCode();
				}
			}
		}
		return "";
	}

	public String getTscByTableCode(String tc) {
		// TODO Auto-generated method stub
		return MatchinfoService.getInstance().getMatchInfoByTableCode(tc).getTableSystemCode();
	}
}
