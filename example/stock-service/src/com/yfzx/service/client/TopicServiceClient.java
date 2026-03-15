package com.yfzx.service.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.TopicConstants;
import com.stock.common.model.Topic;
import com.stock.common.model.TopicUser;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.service.ITopicService;
import com.yfzx.service.util.ServiceUtil;
import com.yfzx.yas.YASFactory;
import com.yfzx.yas.router.ISelectRouter;
import com.yfzx.yas.router.RouterCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;

public class TopicServiceClient implements ITopicService{
	static Map<String,Dbrouter> _routerPool = new ConcurrentHashMap<String,Dbrouter>();
	static Logger logger = LoggerFactory.getLogger(TopicServiceClient.class);
	private static TopicServiceClient instance = new TopicServiceClient();
	static String iserviceName = "ITopicService";
	public TopicServiceClient() {}

	public static TopicServiceClient getInstance() {
		return instance;
	}
	static
	{
		initRouter();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			public void refresh() {
				initRouter();
				
			}
			
		});
	}
	private static void reinitRouter()
	{
		List<Dbrouter> rl = RouterCenter.getInstance().getRouter(
				iserviceName);
		if(rl!=null&&_routerPool.keySet().size()!=rl.size())
			initRouter();
	}
	private static void initRouter() {
		try {
			// TODO Auto-generated method stub
			List<Dbrouter> rl = RouterCenter.getInstance().getRouter(
					iserviceName);
			if(rl==null)
			{
				logger.error("not found  roueter !");
				return ;
			}
			for (Dbrouter dr : rl) {
				_routerPool.put(getKey(dr.getSip(), dr.getSport()), dr);
			}
		} catch (Exception e) {
			logger.error("init roueter failed!",e);
		}
	}
	private static String getKey(String sip, int sport) {
		// TODO Auto-generated method stub
		return sip+"^"+sport;
	}
	ITopicService getService()
	{
		reinitRouter();
		return YASFactory.getService(ITopicService.class,new ISelectRouter(){

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "SR_"+iserviceName;
			}
			//根据缓存名来路由，不同的缓存可能放在不同的服务器上
			public Dbrouter selectRouter(String methodName, Object[] args) {
				Dbrouter dr = null;
				try {
					String serverId = ConfigCenterFactory.getString(TopicConstants.TOPICID, "dcss01");
					String selectIp = ServiceUtil.getServerIP(serverId);
					//String selectIp = "192.168.1.110:7777";
					String ip = selectIp.split(":")[0];
					int port = Integer.valueOf(selectIp.split(":")[1]);
					dr = _routerPool.get(getKey(ip, port));
				} catch (Exception e) {
					logger.error("select router failed!",e);
				}
				return dr;
			}
			
		});
	}

	@Override
	public List<Topic> getListFromCacheByType(int type) {
	
		return getService().getListFromCacheByType(type);
	}

	@Override
	public Topic getTopicFromCache(String key) {
		
		return getService().getTopicFromCache(key);
	}
	@Override
	public List<Topic> getTopicListFromCache(List<String> keyList) {

		return getService().getTopicListFromCache(keyList);
	}

	@Override
	public void updateCache(int type, Map<String,Object> map) {
		getService().updateCache(type, map);
		
	}

	@Override
	public List<Topic> getListByPageing(int type, int pageindex, int limit) {
		return getService().getListByPageing(type, pageindex, limit);
	}
	@Override
	public Map<String,Object> getUnfocusListByUid(int type, int start, int limit,long uid) {
		return getService().getUnfocusListByUid(type, start, limit,uid);
	}

	@Override
	public List<TopicUser> getTopicUserListFromCache() {
		return getService().getTopicUserListFromCache();
	}

	@Override
	public List<TopicUser> getTopicUserListFromCache(int uid) {
		return getService().getTopicUserListFromCache(uid);
	}

	@Override
	public TopicUser getTopicUserFromCache(String key) {
		return getService().getTopicUserFromCache(key);
	}

	@Override
	public int countSize(int type) {
		if(getService()!=null){
			return getService().countSize(type);
		}else{
			return 0;
		}
	}

	public List<TopicUser> getTopicUserByUidFromCache(long uid) {
		
		return getService().getTopicUserByUidFromCache(uid);
	}

	@Override
	public List<Topic> getMobileListByPageing(int type, int limit, int ftype,
			long sortby) {
		return getService().getMobileListByPageing(type, limit, ftype, sortby);
	}

	@Override
	public Map<String, Object> getListByPageing2(int type, int pageindex, int limit) {
		return getService().getListByPageing2(type, pageindex, limit);
	}
}
