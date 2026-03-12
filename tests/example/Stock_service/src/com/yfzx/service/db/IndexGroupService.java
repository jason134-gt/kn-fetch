package com.yfzx.service.db;

import java.util.HashMap;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Indexgroup;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;



public class IndexGroupService {

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static IndexGroupService instance = new IndexGroupService();
	private IndexGroupService()
	{
		
	}
	
	public static IndexGroupService getInstance() {
		// TODO Auto-generated method stub
		return instance;
	}

	public Indexgroup getIndexGroupByKey(String indexCode, String grouptype) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexCode", indexCode);
		m.put("type", grouptype);
		String key = grouptype+"_"+indexCode;
		RequestMessage req = DAFFactory.buildRequest(key,"getIndexGroupByKey",m,StockConstants.DATA_TYPE_indexgroup);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Indexgroup) value;
	}

	/**
	 * desc type:indexcode;type:indexcode,组成
	 * @param type
	 * @param ig
	 * @return
	 */
	public String getQhyIndexByType(String type, Indexgroup ig) {
		String desc = ig.getIndexGroupDesc();
		int sindex = desc.indexOf(type+":");
		return desc.substring(sindex+2,desc.indexOf(";", sindex));
	}

	public void create(Indexgroup ig) {
		
		Indexgroup dig = getIndexGroupByKey(ig.getIndexGroupKeys(), String.valueOf(ig.getType()));
		if(dig==null)
		{
			insert(ig);
		}
		else
		{
			modify(ig);
		}
	}

	private void modify(Indexgroup ig) {
		RequestMessage req = DAFFactory.buildRequest("com.stock.common.model.Indexgroup.update", ig, StockConstants.DATA_TYPE_indexgroup);
		 pLayerEnter.modify(req);
		
	}

	private void insert(Indexgroup ig) {
		RequestMessage req = DAFFactory.buildRequest("com.stock.common.model.Indexgroup.insert", ig, StockConstants.DATA_TYPE_indexgroup);
		 pLayerEnter.insert(req);
		
	}

}
