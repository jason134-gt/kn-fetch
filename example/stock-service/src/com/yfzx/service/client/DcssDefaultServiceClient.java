package com.yfzx.service.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.router.Dbrouter;
import com.stock.common.service.IDcssDefaultService;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.yasmsg.DcssReqMsg;
import com.yfzx.yas.YASFactory;
import com.yfzx.yas.router.ISelectRouter;
import com.yfzx.yas.router.RouterCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;

/**
 * 缓存服务器默认客户端
 * 路由自己主选择
 *      
 * @author：杨真 
 * @date：2013-4-24
 */
public class DcssDefaultServiceClient {

	static Map<String,Dbrouter> _routerPool = new ConcurrentHashMap<String,Dbrouter>();
	static Logger logger = LoggerFactory.getLogger(RouterCenter.class);
	private static DcssDefaultServiceClient instance = new DcssDefaultServiceClient();
	static String iserviceName = "IDcssCompanyExtIndexService";
	public DcssDefaultServiceClient() {

	}

	public static DcssDefaultServiceClient getInstance() {
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
	IDcssDefaultService getService()
	{
		reinitRouter();
		return YASFactory.getService(IDcssDefaultService.class,new ISelectRouter(){

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "SR_"+iserviceName;
			}
			//根据缓存名来路由，不同的缓存可能放在不同的服务器上
			public Dbrouter selectRouter(String methodName, Object[] args) {
				Dbrouter dr = null;
				try {
					if(_routerPool.keySet().size()==0) return null;
					String cacheName = args[args.length - 1].toString();
					if (StringUtil.isEmpty(cacheName))
						return null;
					String sip = ConfigCenterFactory.getString(
							"dcss.default_cache_server_ip",
							"192.168.1.102:8855");
					String ip = sip.split(":")[0];

					int port = Integer.valueOf(sip.split(":")[1]);
					dr = _routerPool.get(getKey(ip, port));
					//如果各中不同类型的数据存在不同的缓存服务器上，则根据服务名取相应缓存的服务地址，此处根据不同的情 况，自己实现
				} catch (Exception e) {
					logger.error("select router failed!",e);
				}
				return dr;
			}
			
		});
	}
	
	public <K, V> V get(K k, String cacheName) {
		return getService().get(k,cacheName);
	}

	public <K, V> void put(K k, V v, String cacheName) {
		 getService().put(k, v,cacheName);

	}

	public <K> void remove(K k, String cacheName) {
		getService().remove(k,cacheName);

	}
	
	/**
	 * 路由选择算法是由数据存储策略决定的，如果有特殊的数据存储策略，则自已实现
	 * @param rl
	 * @param r
	 * @return
	 */
	@SuppressWarnings("unchecked")
//	private Dbrouter selectRouter(List<Dbrouter> rl, DcssReqMsg r) {
//		List<Dbrouter> mrl = new ArrayList<Dbrouter>();
//		//先通过类型找出存储的服务器
//		for(Dbrouter ri : rl)
//		{
//			Object attr = ri.getAttribute();
//			if(attr!=null)
//			{
//				Map<String,String> m = (Map<String, String>) attr;
//				if(m.get(r.getT())!=null)
//					mrl.add(ri);
//			}
//		}
//		if(mrl.size()==0) return null;
//		return computeRouter(mrl,r);
//	}

	/**
	 * 根据不同的需要，计算路由，此处先用哈希取余来计算
	 * 以后改为一致哈希
	 * @param mrl
	 * @param r
	 * @return
	 */
	private Dbrouter computeRouter(List<Dbrouter> mrl, DcssReqMsg r) {
		
		int i =  StockUtil.hashMod(r.getK().toString(),mrl.size());
		return mrl.get(i);
	}
}
