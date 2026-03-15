package com.yfzx.service.db;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Indexcategory;
import com.stock.common.util.StringUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;




public class IndexCategoryService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger("IndexCategoryService");
	DictService ds = DictService.getInstance();
	private static IndexCategoryService instance = new IndexCategoryService();

	private IndexCategoryService() {

	}

	public static IndexCategoryService getInstance() {
		return instance;
	}

	public List<Indexcategory> getAllIndexcategory() {
		RequestMessage req = DAFFactory.buildRequest(SCache.KEY_ALL_INDEXCATEGORY,
				"loadindexcategory2cache", StockConstants.DATA_TYPE_indexcategory);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List) value;
	}

	public Indexcategory getIndexCategoryByCode(int categoryCode) {
		Indexcategory inc = new Indexcategory();
		inc.setIndexCategoryCode(categoryCode);
		RequestMessage req = DAFFactory.buildRequest(inc.getKey(),
				"getIndexCategoryByCode", inc, StockConstants.DATA_TYPE_indexcategory);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Indexcategory) value;
	}


	public List<Dictionary> getDictionaryListByTag(
			String tag) {
		
		return DictService.getInstance().getDictionaryListByTag(tag);

	}
	
	
	public void addTags2Index(String indexcode, String ts) {
		if(StringUtil.isEmpty(indexcode)||StringUtil.isEmpty(ts))
			return;
		Dictionary d = ds.getDataDictionary(indexcode);
		if(d!=null)
		{
			String otags = d.getTags();
			StringBuilder sb = new StringBuilder();
			if(!StringUtil.isEmpty(otags))
				sb.append(otags);
			if(!StringUtil.isEmpty(sb.toString())&&!sb.toString().endsWith(";"))
				sb.append(";");
			String[] tags = ts.split(";");
			for(String t: tags)
			{
				
				if(!sb.toString().contains(t))
					{
						sb.append(t);
						sb.append(";");
					}
			}
			ds.updateTags(indexcode,sb.toString());
		}
		
	}
	
	public void delTagsOfIndex(String indexcode, String ts) {
		if(StringUtil.isEmpty(indexcode)||StringUtil.isEmpty(ts))
			return;
		Dictionary d = ds.getDataDictionary(indexcode);
		if(d!=null)
		{
			String otags = d.getTags();
			String[] tags = ts.split(";");
			for(String t: tags)
			{
				
				if(otags.contains(t))
					{
						otags = otags.replace(t+";", "");
					}
			
			}
			ds.updateTags(indexcode,otags);
		}
		
	}

}
