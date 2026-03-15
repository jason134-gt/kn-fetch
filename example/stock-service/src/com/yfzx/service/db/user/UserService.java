package com.yfzx.service.db.user;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.stock.common.constants.AAAConstants;
import com.stock.common.model.share.UserExt;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;


public class UserService {

	static UserService instance = new UserService();
	
	private UserService(){}
	
	public static UserService getInstance(){
		return instance;
	}

	
	// 根据用户UID进行 或username 
	private String getThisCacheName(String baseCacheName,String key){
		//如2个DCSS 里面各2个cache D0到D7数据  S0_C0 包含数据D0,D4;S1_C0包含D1,D5;S0_C1包含D2,D6;S1_C1包含D3,D7
		//如D7在 7%2=S1 和7/2%2=C1 ，D6在6%2=S0 6/2%2=C1中 
		//int cacheIndex = StockUtil.getUserIndex(key)/dcssNums %AAAConstants.AAA_CACHE_NUMS;
		int cacheIndex = StockUtil.getUserIndex(key) % AAAConstants.AAA_CACHE_NUMS;
		String cacheName = CacheUtil.getCacheName(baseCacheName)+"_"+cacheIndex;
		return cacheName;
	}	
		
	public UserExt getUserExtByUid(long uid) {
		StringBuilder sb = new StringBuilder();
		long version = System.currentTimeMillis();
		String cacheName = getThisCacheName(AAAConstants.USEREXT_BASE_CACHENAME,String.valueOf(uid));
		UserExt ue = LCEnter.getInstance().get(String.valueOf(uid), cacheName);
		if(ue == null){
			String[] columns = NosqlBeanUtil.getColumns(UserExt.class);
			Map<String,String> getMap = CassandraHectorGateWay.getInstance().get(SAVE_TABLE.USER_EXT.toString(), String.valueOf(uid), columns);
			ue = new UserExt();
			NosqlBeanUtil.map2Bean(ue, getMap);
			LCEnter.getInstance().put(String.valueOf(uid),ue, cacheName);
		}
		String faceurl = ue.getFaceUrl();
		if(StringUtils.isNotBlank(faceurl) && !faceurl.startsWith("http")){
			faceurl = "http://img.igushuo.com/"+faceurl;
			ue.setFaceUrl(faceurl);
		}
		if(faceurl != null && !faceurl.contains("?version")){
			sb.append(faceurl);
			sb.append("?version=");
			sb.append(version);
			faceurl = sb.toString();
			ue.setFaceUrl(faceurl);
		}
		return ue;
	}
	
	/*
	 * 新增或删除博文时  用户博文数+-1
	 */
	public void updateArticleCounts(long uid,boolean add){
		UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(uid);
		if(userExt != null) {
			long count = userExt.getArticle_counts();
			if(add){
				count = count+1;
			}else{
				count =  count-1<0?0:count - 1;
			}
			Map<String, Object> userMap = new HashMap<String, Object>();
			userMap.put("article_counts", count);
			UserServiceClient.getInstance().updateUserExt(uid, userMap);
		}
	}

}
