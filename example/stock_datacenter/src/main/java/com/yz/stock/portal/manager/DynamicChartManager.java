package com.yz.stock.portal.manager;

import java.util.List;

import com.stock.common.msg.Message;


public interface DynamicChartManager {

	List<Message> getDCompareIndex(Message req);

	List<Message> getTCompareIndex(Message req);

	List<Message> getHCompareIndex(Message req);

	List<Message> getBehaviourIndex(Message req);

}
