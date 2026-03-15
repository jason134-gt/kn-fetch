package com.yfzx.service.client;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.service.IDcssIndustryExtIndexService;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.yas.YASFactory;
import com.yfzx.yas.router.ISelectRouter;
import com.yfzx.yas.router.RouterCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
/**
 * 扩展指标缓存客户端
 *      
 * @author：杨真 
 * @date：2013-4-24
 */
public class DcssIndustryExtIndexServiceClient {

	static Map<String,Dbrouter> _routerPool = new ConcurrentHashMap<String,Dbrouter>();
	static Logger logger = LoggerFactory.getLogger(RouterCenter.class);
	private static DcssIndustryExtIndexServiceClient instance = new DcssIndustryExtIndexServiceClient();
	static String iserviceName = "IDcssIndustryExtIndexService";
	public DcssIndustryExtIndexServiceClient() {

	}

	public static DcssIndustryExtIndexServiceClient getInstance() {
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
	IDcssIndustryExtIndexService getService()
	{
		reinitRouter();
		return YASFactory.getService(IDcssIndustryExtIndexService.class,new ISelectRouter(){

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "SR_"+iserviceName;
			}
			//此处先以简单的哈希取余的方式路由
			public Dbrouter selectRouter(String methodName, Object[] args) {
				Dbrouter dr = null;
				try {
					if(_routerPool.keySet().size()==0) return null;
					//k格式：000002.sz^201206^1867
					String k = args[0].toString();
					if (StringUtil.isEmpty(k))
						return null;
					String[] ka = k.toString().split("\\^");
					//路由key,以公司编码路由，这样同一公司的数据只会存储在一个缓存服务器上
					String rk = ka[0];
					
					//192.168.1.102:8855^192.168.1.103:8855^192.168.1.104:8855
					String sips = ConfigCenterFactory.getString(
							"dcss.ext_cache_server_ip_list",
							"192.168.1.102:7777");
					
					String[] ips = sips.split("\\^");
					//先求出表的号
					int index =  SExt.getUExtTableIndex(rk,SExt.EXT_TABLE_TYPE_1)%ips.length;
					String selectIp =  ips[index];
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

	public <K, V> V get(K k) {
		return getService().get(k);
	}

	public <K, V> void put(K k, V v) {
		 getService().put(k, v);

	}

	public <K> void remove(K k) {
		getService().remove(k);

	}
	
	/*
	 * 取图表某一组指标数据
	 */
	public String getOneChartIndIndexData(String companycode,String xindexs,String yindexs,Date stime,Date etime, String indtype)
	{
		return getService().getOneChartIndIndexData(companycode, xindexs, yindexs, stime, etime,indtype);
	}
	/*
	 * 取多图表多组指标数据
	 * 指标是在配置中心预先配置好的
	 * ck为配置中心的配置项的key
	 */
	public String getMultChartIndIndexData(String companycode,String ck,Date stime,Date etime, String indtype)
	{
		return getService().getMultChartIndIndexData(companycode, ck,stime, etime,indtype);
	}
	
	/*
	 * 取某公司某指标的数据
	 */
	public String getIndIndexData(String tag,String indexcode,Date stime,Date etime, String indtype)
	{
		return getService().getIndIndexData(tag, indexcode, stime, etime,indtype);
	}


}
