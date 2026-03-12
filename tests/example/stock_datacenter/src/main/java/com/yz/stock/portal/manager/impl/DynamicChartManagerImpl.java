package com.yz.stock.portal.manager.impl;

import java.util.ArrayList;
import java.util.List;

import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.msg.Message;
import com.yfzx.service.db.DictService;
import com.yz.stock.portal.manager.DynamicChartManager;
import com.yz.stock.portal.service.index.IndexDCService;

public class DynamicChartManagerImpl implements DynamicChartManager {

	IndexDCService indexService = IndexDCService.getInstance();
	DictService dService = DictService.getInstance();

	// 对比
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List getDCompareIndex(Message request) {

		IndexMessage req = (IndexMessage) request;
		// 从数据字典中取出指标对应的表名和列名
		req = getTAndC(req);
		// 取指标对应的数据
		List<IndexMessage> ldiqResp = new ArrayList<IndexMessage>();
//		List indexList = indexService.queryIndexList(req);
//		if (indexList != null) {
//			QueryIndexListResp dqResp = new QueryIndexListResp();
//			dqResp.setData(indexList);
//			dqResp.setCompanyName(req.getCompanyName());
//			ldiqResp.add(dqResp);
//		}
		return ldiqResp;
	}

	// 取指标对应的列名与表名
	private IndexMessage getTAndC(IndexMessage req) {
		// TODO Auto-generated method stub
		Dictionary dd = getDictionary(req);
		req.setTableName(dd.getTableName());
		req.setColumnName(dd.getColumnName());
		req.setIndexCode(dd.getIndexCode());
		return req;
	}

	private Dictionary getDictionary(IndexMessage req) {
		// 从数据字典中取出指标对应的表名和列名
		Dictionary dd = dService.getDataDictionary(req.getIndexCode());
		return dd;
	}

	// 同比
	public List<Message> getTCompareIndex(Message request) {

		return null;

	}

	private List computeTCompareIndex(List indexList_one, List indexList_two) {
		// TODO Auto-generated method stub
		return indexList_one;
	}

	// 环比
	public List<Message> getHCompareIndex(Message request) {
		return null;
	}

	private List computeHCompareIndex(List one, List two) {
		// TODO Auto-generated method stub
		return one;
	}

	// 趋势
	public List<Message> getBehaviourIndex(Message request) {
		return null;
	}

}
