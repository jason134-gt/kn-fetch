package com.yz.stock.portal.manager;

import java.util.List;

import com.stock.common.msg.Message;


public interface IIndexManager {

	@SuppressWarnings("rawtypes")
	List getDCompareIndex(Message req);

}
