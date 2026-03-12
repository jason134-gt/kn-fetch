package com.yfzx.service.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.model.Trade0001;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.service.IDcssTradeIndexService;
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
public class DcssTradeIndexServiceClient {

	static Map<String,Dbrouter> _routerPool = new ConcurrentHashMap<String,Dbrouter>();
	static Logger logger = LoggerFactory.getLogger(RouterCenter.class);
	private static DcssTradeIndexServiceClient instance = new DcssTradeIndexServiceClient();
	static Map<String, Dbrouter> _routerUdpPool = new ConcurrentHashMap<String, Dbrouter>();
	static List<Dbrouter> _udpRouterList;
	static String iserviceName = "IDcssTradeIndexService";
	public DcssTradeIndexServiceClient() {

	}

	public static DcssTradeIndexServiceClient getInstance() {
		return instance;
	}
	static
	{
		initRouter();
		initUdpRouter();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			public void refresh() {
				initRouter();
				initUdpRouter();
			}
			
		});
	}
	private static void initUdpRouter() {
		String uips = ConfigCenterFactory.getString("yas." + iserviceName,
				"udp://192.168.1.112:6667/dcss/IMessageSenderService;udp://192.168.1.112:6668/dcss/IMessageSenderService");
		if (!StringUtil.isEmpty(uips)) {
			// _udpRouterList
			List<Dbrouter> nudpRouterList = new ArrayList<Dbrouter>();
			for (String ips : uips.split(";")) {
				if (!StringUtil.isEmpty(ips)) {
					try {
						Dbrouter ur = new Dbrouter();
						ur.setServiceName(iserviceName);
						ur.setServiceAddress(ips);
						nudpRouterList.add(ur);
					} catch (Exception e) {
						logger.error("parse udp router failed!", e);
					}
				}

			}
			if (nudpRouterList.size() != 0)
			{
				_udpRouterList = nudpRouterList;
				_routerUdpPool.clear();
				for (Dbrouter dr : _udpRouterList) {
					_routerUdpPool.put(getKey(dr.getSip(), dr.getSport()), dr);
				}
			}
		}

	}
	IDcssTradeIndexService getService()
	{
		reinitRouter();
		return YASFactory.getService(IDcssTradeIndexService.class,new ISelectRouter(){

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "SR_tcp_"+iserviceName;
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
					int index =  SExt.getUExtTableIndex(rk,SExt.EXT_TABLE_TYPE_2)%ips.length;
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
	
	IDcssTradeIndexService getAsynUdpService()
	{
		reinitRouter();
		return YASFactory.getAsynUdpService(IDcssTradeIndexService.class,new ISelectRouter(){

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "SR_udp_"+iserviceName;
			}
			//此处先以简单的哈希取余的方式路由
			public Dbrouter selectRouter(String methodName, Object[] args) {
				Dbrouter dr = null;
				try {
					if (_routerUdpPool.keySet().size() == 0)
						return null;
					String k = args[0].toString();
					if (StringUtil.isEmpty(k))
						return null;
					String[] ka = k.toString().split("\\^");
					// 路由key,以公司编码路由，这样同一公司的数据只会存储在一个缓存服务器上
					String rk = ka[0];
					String sips = ConfigCenterFactory.getString(
							"dcss.ext_cache_server_ip_list_udp",
							"192.168.1.102:6666");

					String[] ips = sips.split("\\^");
					int dcssNums = ips.length;
					// 根据用户uid或者username求余 得到
					// 不建议客户端感知后端使用每台DCSS有多少个cache
					int index = StockUtil.getUserIndex(rk) % dcssNums; // StockUtil.getUExtTableIndex(rk)%ips.length;
					String selectIp = ips[index];
					String ip = selectIp.split(":")[0];
					int port = Integer.valueOf(selectIp.split(":")[1]);
					dr = _routerUdpPool.get(getKey(ip, port));
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
	
	public String getTradeDataByType(String companycode,int type, 
			Date stime, Date etime) 
	{
		return getService().getTradeDataByType(companycode,type,  
				 stime,  etime);
	}
	//seed,路由key
	public String getRealTimeTradeIndexs(String seed,String companycodes,String indexcodes,Date time)
	{
		return getService().getRealTimeTradeIndexs(seed,companycodes, indexcodes, time);
	}

	public void putRealTimeResult2Cache(String seed, String ret,boolean needComputeAvg) {
		getService().putRealTimeResult2Cache(seed,ret,needComputeAvg);
	}
	public String getCompanyIndexData(String companycode, String indexcode,
			Date stime, Date etime) {
		return getService().getCompanyIndexData(companycode, indexcode, stime, etime);
	}
	/**
	 * 取公司最近一个交易日的数据，如果为停牌，则取停牌前最后一个交易日数据
	 * @param companycode
	 * @return
	 */
	public Trade0001 getLatestTradeData(String companycode)
	{
		return getService().getLatestTradeData(companycode);
	}

	public String getTradeDataByType(String companycode, int type, Date time,Date etime,
			int ftype, int limit) {
		return getService().getTradeDataByType(companycode, type, time,etime,ftype,limit);
	}
}
