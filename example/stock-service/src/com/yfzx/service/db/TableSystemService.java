package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.List;

import com.yz.mycore.lcs.enter.LCEnter;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockException;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Matchinfo;
import com.stock.common.model.TableSystem;
import com.stock.common.util.StockUtil;

public class TableSystemService {

	public static TableSystemService instance = new TableSystemService();
	TableSystemService()
	{
		
	}
	public static TableSystemService getInstance()
	{
		return instance;
	}
	public List<TableSystem> getTableSystemListByDs(String dscode) throws NumberFormatException, StockException {
		LCEnter lce = LCEnter.getInstance();
		List<TableSystem> tsl = new ArrayList<TableSystem>();
		List<String> mil = lce.get(dscode, StockUtil.getCacheName(StockConstants.MATCHINFO));
		if(mil==null)
		{
			throw new StockException(Integer.valueOf(StockCodes.FAILED),"get TableSystem list failed!");
		}
		else
		{
			for(String tsc : mil)
			{
				TableSystem ts = lce.get(StockConstants.tableSystem+"."+tsc, StockUtil.getCacheName(StockConstants.common));
				if(ts!=null)
				{
					tsl.add(ts);
				}
			}
		}
		return tsl;
	}
	public boolean isThisTscDictionary(String tsc,Dictionary d) {
		Matchinfo mi = MatchinfoService.getInstance().getMatchInfoByTableCode(d.getTableCode());
		if(mi!=null&&mi.getTableSystemCode().equals(tsc))
			return true;
		return false;
	}

}
