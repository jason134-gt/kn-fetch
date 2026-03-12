package com.yfzx.service.db;

import java.util.Date;

import com.stock.common.constants.SCache;
import com.stock.common.model.Stock0004;
import com.stock.common.util.DateUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class Stock0004Service {

	private static Stock0004Service instance = new Stock0004Service();

	private Stock0004Service() {

	}

	public static Stock0004Service getInstance() {
		return instance;
	}


	public Stock0004 getStock0004ByCodeFromCache(String companycode) {

		return LCEnter.getInstance().get(companycode, SCache.CACHE_NAME_STOCK0004);
	}

	/*
	 * 是否是除权日
	 */
	public boolean isCQR(String companycode)
	{
		Stock0004 s4 = getStock0004ByCodeFromCache(companycode);
		if(s4!=null)
		{
			if(!CompanyService.getInstance().isHStock(companycode))
			{
				//是A股
				if(CompanyService.getInstance().isBStock(companycode)==-1)
				{
					Date td = DateUtil.getDayStartTime(new Date());
					if(s4.getF020d() != null && s4.getF020d().equals(td))
						return true;
				}
				else
				{
					Date td = DateUtil.getDayStartTime(new Date());
					if(s4.getF021d() != null && s4.getF021d().equals(td))
						return true;
				}
			}
		}
		return false;

	}

}
