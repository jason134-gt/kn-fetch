package com.yfzx.service.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;

public class StockChanceUtil {
	private final static Logger logger = LoggerFactory.getLogger(StockChanceUtil.class);
	public static Map<String, String> scMap = new HashMap<String, String>();
	public static boolean addFromPublish = false;//话题的讨论推荐到机会广场
	public static boolean addFromRecommend = false;//话题的精华推荐到机会广场
	public static boolean hostPublish = false;//只有主持人发的推荐到话题广场
	static {
		initStockChanceKeyMap();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				initStockChanceKeyMap();
			}
		});
	}

	public static void initStockChanceKeyMap(){
		String keyMap = ConfigCenterFactory.getString("stock_chance.square_map", "");
		String p = ConfigCenterFactory.getString("stock_chance.add_from_publish", "false");
		String r = ConfigCenterFactory.getString("stock_chance.add_from_recommend", "fasle");
		String oh = ConfigCenterFactory.getString("stock_chance.host_publish", "fasle");
		if("true".equals(p)){
			addFromPublish = true;
		}else{
			addFromPublish = false;
		}
		if("true".equals(r)){
			addFromRecommend = true;
		}else{
			addFromRecommend = false;
		}
		if("true".equals(oh)){
			hostPublish = true;
		}else{
			hostPublish = false;
		}
		if(StringUtils.isEmpty(keyMap)){
			logger.info("stock_chance.square_map is null");
			return;
		}
		scMap.clear();
		String[] arr = keyMap.split("\\^");
		for(String a :arr){
			String[] map = a.split(":");
			if(map.length>1){
				scMap.put(map[0], map[1]);
			}
		}
	}


}
