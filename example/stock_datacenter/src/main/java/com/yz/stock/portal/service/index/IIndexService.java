package com.yz.stock.portal.service.index;

import java.util.List;

import com.stock.common.msg.Message;
import com.yfzx.service.db.IIService;


public class IIndexService implements IIService {

	private static IIndexService instance = new IIndexService();
	private IIndexService() {

	}

	public static IIndexService getInstance() {
		return instance;
	}
	public Double getIndexValue(Message msg) {
		// TODO Auto-generated method stub
		return null;
	}

	public List getIndexList(Message msg) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getIndexObject(Message msg) {
		// TODO Auto-generated method stub
		return null;
	}

	public List getIndexObjectList(Message msg) {
		// TODO Auto-generated method stub
		return null;
	}

}
