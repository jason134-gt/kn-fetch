package com.yz.stock.util;

import java.util.List;
import java.util.concurrent.Callable;

import com.yz.stock.portal.excel.IDataHandler;

public class HandlerFuture  implements Callable<String>
{
	String opt;
	List ul;
	
	IDataHandler h;
	public HandlerFuture(String opt,List ul,IDataHandler h)
	{
		this.opt = opt;
		this.ul = ul;
		this.h = h;
	}
	public String call() throws Exception {
		
		h.handle(opt, ul);
		
		return null;
	}
	
}