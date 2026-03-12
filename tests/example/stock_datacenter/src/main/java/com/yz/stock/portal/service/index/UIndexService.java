package com.yz.stock.portal.service.index;

import java.util.List;

import com.stock.common.msg.Message;
import com.yfzx.service.db.IIService;


public class UIndexService implements IIService{

	private static UIndexService instance = new UIndexService();
	private UIndexService() {

	}

	public static UIndexService getInstance() {
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
