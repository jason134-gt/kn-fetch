package com.yfzx.service.db;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Dictionary;
import com.stock.common.util.StringUtil;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;



public class RelativeIndexsService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static RelativeIndexsService instance = new RelativeIndexsService();
	static Map<String,String> _riMap = new HashMap<String,String>();
	
	static
	{
		buildRelativeIndexsMap();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			public void refresh() {
				buildRelativeIndexsMap();	
			}
			
		});

	}
	private RelativeIndexsService() {

	}

	private static void buildRelativeIndexsMap() {
		
		String s = ConfigCenterFactory.getString("relative_indexs.config", "");
		if(!StringUtil.isEmpty(s))
		{
			String[] ia = s.split("~");
			for(String indexs:ia)
			{
				String[] ica = indexs.split("\\^");
				if(ica.length<=1) continue;
				StringBuilder sb = new StringBuilder();
				for(String indexcode:ica)
				{
					Dictionary d = DictService.getInstance().getDataDictionaryFromCache(indexcode);
					if(d!=null&&!d.getShowName().startsWith("行业"))
					{
						sb.append(d.getShowName()+":"+d.getIndexCode());
						sb.append("^");
					}
				}
				
				for(String indexcode:ica)
				{
					Dictionary d = DictService.getInstance().getDataDictionaryFromCache(indexcode);
					if(d!=null)
					{
						String ret = sb.toString();
						String v = d.getShowName()+":"+d.getIndexCode()+"^";
						ret = ret.replace(v, "");
						if(ret.split("\\^").length>1)
							_riMap.put(d.getIndexCode(), ret);
					}
						
				}
			}
		}
	}

	public static RelativeIndexsService getInstance() {
		return instance;
	}
	public String getRelativeIndexsByIndexcode(String indexcode)
	{
		return _riMap.get(indexcode);
	}
}
