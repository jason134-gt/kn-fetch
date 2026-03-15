package com.yfzx.service.db;

import java.util.List;

import com.stock.common.msg.Message;


public interface IIService {

	public Double getIndexValue(Message msg);
	@SuppressWarnings("rawtypes")
	public List getIndexList(Message msg);
	public Object getIndexObject(Message msg);
	@SuppressWarnings("rawtypes")
	public List getIndexObjectList(Message msg);
}
