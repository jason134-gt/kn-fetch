package com.yz.stock.portal.excel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.stock.common.constants.StockConstants;

public  abstract class ImportDataBaseService implements IDataHandler{

	public static Set<String> uset = new HashSet<String>(); 
	
	static
	{
		
		uset.add("obObjectId");
		uset.add("obModtime0292");
		uset.add("obModtime0290");
		uset.add("obModtime0291");
		uset.add("serialVersionUID");
	}
	
	public void handle(String opt, List<Object> ul) {
		if(opt.equals(StockConstants.OPT_UPDATE))
		{
			BatchUpdate(ul);
		}
		if(opt.equals(StockConstants.OPT_INSERT))
		{
			BatchInsert(ul);
		}
		
	}

	public abstract void BatchInsert(List<Object> ul) ;

	public abstract void BatchUpdate(List<Object> ul) ;

	public abstract List doLoadTableKeyList(String tableName, int start,int limit);

}
