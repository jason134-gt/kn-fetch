package com.yfzx.service.realtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.sf.ehcache.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.USubject;
import com.stock.common.model.snn.EConst;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.msg.StockPriceBatchMsg;
import com.stock.common.util.DateUtil;
import com.stock.common.util.LogSvr;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.RealTimeService;
import com.yfzx.service.db.SelectStockService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.trade.TradeCenter;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.log.LogManager;
import com.yz.mycore.core.util.BaseUtil;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.ClientEventCenter;

/**
 * 实时行情统一处理 
 * @author tbq
 *
 */
public class RealTradeService {

	static RealTradeService instance = new RealTradeService();
	Logger log = LoggerFactory.getLogger(RealTradeService.class);
	boolean isopenRealMonitor = false;
	boolean isStopMock = false;
	public RealTradeService() {

	}

	public static RealTradeService getInstance() {
		return instance;
	}
	public void stopMock()
	{
		isStopMock = true;
	}
	public void  openRealMonitor()
	{
		isopenRealMonitor = true;
	}
	/**
	 * 执行行情刷新，[删除掉通知 包括从realtime通知dcss]
	 * @param stocktrade
	 */
	public void exeStockTradeRefresh(StockTrade stocktrade){
		try{
			String uidentify = stocktrade.getCode();
			USubject usubject = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(uidentify);
			if (usubject == null) {
				log.info("not found USubject!uidentify="
						+ uidentify);
				return;
			}	
			if(stocktrade.getUptime() <=0){
				throw new RuntimeException("uidentify="
						+ uidentify+" 行情时间不允许为空");				
			}
			double zs = stocktrade.getZs();//昨日收盘价
			double jk = stocktrade.getJk();//今日开盘价
			double h = stocktrade.getH();//最高价
			double l = stocktrade.getL();//最低价
			double c = stocktrade.getC();//当前价
			double cjl = stocktrade.getCjl();//成交量
			double cje = stocktrade.getCje();//成交额
			String timeString = String.valueOf(stocktrade.getUptime());//时间戳字符串
			RealTimeService rtService = RealTimeService.getInstance();
			rtService.put(uidentify,StockConstants.INDEX_CODE_H_L,l);
			rtService.put(uidentify,StockConstants.INDEX_CODE_H_H,h);
			rtService.put(uidentify,StockConstants.INDEX_CODE_H_C,c);
			rtService.put(uidentify,StockConstants.INDEX_CODE_H_ZS,zs);
			rtService.put(uidentify,StockConstants.INDEX_CODE_H_JK,jk);
			rtService.put(uidentify,StockConstants.INDEX_CODE_H_CJL,cjl);
			rtService.put(uidentify,StockConstants.INDEX_CODE_H_CJE,cje);
			Double zdf = 0d;
			Double sd = 0d;
			if(c != 0 && zs != 0 ){//新浪上的c=0是停牌
				zdf = SMathUtil.getDouble((c - zs) / zs *100,4);//今日涨幅
				sd = SMathUtil.getDouble(c - zs,3);//今日涨幅
			}
			
			RealTimeService.getInstance().put(uidentify,
					StockConstants.INDEX_CODE_H_ZDF, zdf);	
			RealTimeService.getInstance().put(uidentify,
					StockConstants.INDEX_CODE_H_SD, sd);
			// 取前一个交易日的总股本，放入今天
			Date ptime = IndexService.getTradeTime(uidentify,
					StockUtil.getNextTimeV3(new Date(), -1,
							Calendar.DAY_OF_MONTH));
			if (ptime != null) {
				Double pzgb = IndexValueAgent.getIndexValue(
						uidentify,
						StockConstants.INDEX_CODE_TRADE_ZGB,
						ptime);
				if (pzgb != null)
					RealTimeService
							.getInstance()
							.put(uidentify,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_ZGB),
									pzgb);
	
				// 取当前股票的现价
				Double hfbk = IndexValueAgent.getIndexValue(
						uidentify,
						StockConstants.INDEX_CODE_TRADE_HFQ_K,
						ptime);
				Double bk = IndexValueAgent.getIndexValue(
						uidentify,
						StockConstants.INDEX_CODE_TRADE_K,
						ptime);
				if (hfbk != null && bk != null && hfbk != 0
						&& bk != 0 && c !=0 ) {
					// 后复权因子
					Double fqyz = hfbk / bk;
					Double hfqk = jk * fqyz;
					RealTimeService
							.getInstance()
							.put(uidentify,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_HFQ_K),
									hfqk);
					Double hfqs = c * fqyz;
					RealTimeService
							.getInstance()
							.put(uidentify,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_HFQ_S),
									hfqs);
					Double hfqzg = h * fqyz;
					RealTimeService
							.getInstance()
							.put(uidentify,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_HFQ_ZG),
									hfqzg);
					Double hfqzd = l * fqyz;
					RealTimeService
							.getInstance()
							.put(uidentify,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_HFQ_ZD),
									hfqzd);
				}
			}
			usubject.saveKaiPanTime(stocktrade.getUptime());
			String key = getStockTradeKey(uidentify);
			StockTrade st = LCEnter.getInstance().get(key,
					SCache.CACHE_NAME_marketcache);
			if (st == null) {
				st = new StockTrade(l,h,c,zs,jk,cjl,cje);
				st.setCode(uidentify);
				LCEnter.getInstance().put(key, st,
						SCache.CACHE_NAME_marketcache);
			}else{
				st.setL(l);
				st.setH(h);
				st.setC(c);
				st.setZs(zs);
				st.setJk(jk);
				st.setCjl(cjl);
				st.setCje(cje);
			}
			
			long uptime = Long.valueOf(timeString);
			st.setUptime(uptime);
			put2RealMonitor(st);
			if( st.getName() ==null ||st.getName().equals(stocktrade.getName()) ==false ){
				st.setName(stocktrade.getName());
			}
			//单独发送给出批量发送
//			sendThisGroupRealData(uidentify,uptime);
			
			if(uidentify.contains(".hk")){
				int hkSave = ConfigCenterFactory.getInt("wstock.hkSave", 0);
				if(hkSave == 1){
					String dateF =  DateUtil.getSysDate(DateUtil.YYYYMMDD,new Date(stocktrade.getUptime()));
					String keyList = getStockTradeListKey(uidentify,dateF);
					WStockMessageListWapper eq2 = LCEnter.getInstance().get(keyList, 
							SCache.CACHE_NAME_wstockcache);
					if(eq2==null)
					{
						eq2 = new WStockMessageListWapper();
						LCEnter.getInstance().put(keyList, eq2, SCache.CACHE_NAME_wstockcache);
					}
					eq2.put(stocktrade);
				}
			}else{
				int aSave = ConfigCenterFactory.getInt("wstock.aSave", 1);
				if(aSave == 1){
					String dateF =  DateUtil.getSysDate(DateUtil.YYYYMMDD,new Date(stocktrade.getUptime()));
					String keyList = getStockTradeListKey(uidentify,dateF);
					WStockMessageListWapper eq2 = LCEnter.getInstance().get(keyList, 
							SCache.CACHE_NAME_wstockcache);
					if(eq2==null)
					{
						eq2 = new WStockMessageListWapper();
						LCEnter.getInstance().put(keyList, eq2, SCache.CACHE_NAME_wstockcache);
					}
					eq2.put(stocktrade);
				}
			}
			//
		}catch (Exception e) {
			log.error("exeStockTradeRefresh error"+e.getMessage());
		}
	}
	
	
	public void exeStockTradeRefresh_mock(StockTrade stocktrade){
		try{
			String uidentify = stocktrade.getCode();
			USubject usubject = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(uidentify);
			if (usubject == null) {
				log.info("not found USubject!uidentify="
						+ uidentify);
				return;
			}	
			if(stocktrade.getUptime() <=0){
				throw new RuntimeException("uidentify="
						+ uidentify+" 行情时间不允许为空");				
			}
			double zs = stocktrade.getZs();//昨日收盘价
			double jk = stocktrade.getJk();//今日开盘价
			double h = stocktrade.getH();//最高价
			double l = stocktrade.getL();//最低价
			double c = stocktrade.getC();//当前价
			double cjl = stocktrade.getCjl();//成交量
			double cje = stocktrade.getCje();//成交额
			String timeString = String.valueOf(stocktrade.getUptime());//时间戳字符串
			//mock当天的时间
			Date time = DateUtil.getDayStartTime(stocktrade.getUptime());
			RealTimeService rtService = RealTimeService.getInstance();
			rtService.put2LocalCache_mock(uidentify,time,StockConstants.INDEX_CODE_H_L,l);
			rtService.put2LocalCache_mock(uidentify,time,StockConstants.INDEX_CODE_H_H,h);
			rtService.put2LocalCache_mock(uidentify,time,StockConstants.INDEX_CODE_H_C,c);
			rtService.put2LocalCache_mock(uidentify,time,StockConstants.INDEX_CODE_H_ZS,zs);
			rtService.put2LocalCache_mock(uidentify,time,StockConstants.INDEX_CODE_H_JK,jk);
			rtService.put2LocalCache_mock(uidentify,time,StockConstants.INDEX_CODE_H_CJL,cjl);
			rtService.put2LocalCache_mock(uidentify,time,StockConstants.INDEX_CODE_H_CJE,cje);
			Double zdf = 0d;
			Double sd = 0d;
			if(c != 0 && zs != 0 ){//新浪上的c=0是停牌
				zdf = SMathUtil.getDouble((c - zs) / zs *100,4);//今日涨幅
				sd = SMathUtil.getDouble(c - zs,3);//今日涨幅
			}
			
			RealTimeService.getInstance().put2LocalCache_mock(uidentify,time,
					StockConstants.INDEX_CODE_H_ZDF, zdf);	
			RealTimeService.getInstance().put2LocalCache_mock(uidentify,time,
					StockConstants.INDEX_CODE_H_SD, sd);
			// 取前一个交易日的总股本，放入今天
			Date ptime = IndexService.getTradeTime(uidentify,
					StockUtil.getNextTimeV3(new Date(), -1,
							Calendar.DAY_OF_MONTH));
			if (ptime != null) {
				Double pzgb = IndexValueAgent.getIndexValue(
						uidentify,
						StockConstants.INDEX_CODE_TRADE_ZGB,
						ptime);
				if (pzgb != null)
					RealTimeService
							.getInstance()
							.put2LocalCache_mock(uidentify,time,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_ZGB),
									pzgb);
	
				// 取当前股票的现价
				Double hfbk = IndexValueAgent.getIndexValue(
						uidentify,
						StockConstants.INDEX_CODE_TRADE_HFQ_K,
						ptime);
				Double bk = IndexValueAgent.getIndexValue(
						uidentify,
						StockConstants.INDEX_CODE_TRADE_K,
						ptime);
				if (hfbk != null && bk != null && hfbk != 0
						&& bk != 0 && c !=0 ) {
					// 后复权因子
					Double fqyz = hfbk / bk;
					Double hfqk = jk * fqyz;
					RealTimeService
							.getInstance()
							.put2LocalCache_mock(uidentify,time,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_HFQ_K),
									hfqk);
					Double hfqs = c * fqyz;
					RealTimeService
							.getInstance()
							.put2LocalCache_mock(uidentify,time,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_HFQ_S),
									hfqs);
					Double hfqzg = h * fqyz;
					RealTimeService
							.getInstance()
							.put2LocalCache_mock(uidentify,time,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_HFQ_ZG),
									hfqzg);
					Double hfqzd = l * fqyz;
					RealTimeService
							.getInstance()
							.put2LocalCache_mock(uidentify,time,
									Integer.valueOf(StockConstants.INDEX_CODE_TRADE_HFQ_ZD),
									hfqzd);
				}
			}
			usubject.saveKaiPanTime(stocktrade.getUptime());
			String key = getStockTradeKey(uidentify);
			StockTrade st = LCEnter.getInstance().get(key,
					SCache.CACHE_NAME_marketcache);
			if (st == null) {
				st = new StockTrade(l,h,c,zs,jk,cjl,cje);
				st.setCode(uidentify);
				LCEnter.getInstance().put(key, st,
						SCache.CACHE_NAME_marketcache);
			}else{
				st.setL(l);
				st.setH(h);
				st.setC(c);
				st.setZs(zs);
				st.setJk(jk);
				st.setCjl(cjl);
				st.setCje(cje);
			}
			
			long uptime = Long.valueOf(timeString);
			st.setUptime(uptime);
			put2RealMonitor(st);
			if( st.getName() ==null ||st.getName().equals(stocktrade.getName()) ==false ){
				st.setName(stocktrade.getName());
			}
			
			//
		}catch (Exception e) {
			log.error("exeStockTradeRefresh error"+e.getMessage());
		}
	}
	public void mockWStockAll(String dateF, String companycode){
		List<byte[]> list = LCEnter.getInstance().get("stl.wstock."+dateF, SCache.CACHE_NAME_wstockcache);
		if(list != null){
			int index = 0;
			for(int i=0;i<list.size();i++)
			{
				byte[] ba = list.get(i);
				try {
					int isstop = ConfigCenterFactory.getInt("wstock.isstop", 0);
					if(isstop==1||isStopMock)
					{
						isStopMock = false;
						return;
					}
					if (SLogFactory.isopen("byteToRow_mock_log_open")) {
						System.out.println("byteToRow_mock index="+index+++",size="+list.size());
					}
					WStockService.getIntance().byteToRow_mock(ba,companycode);
					Thread.sleep(1);
				} catch (Exception e) {
					log.error("mockWStockAll failed!",e);
				}
			}
		}		
	}
	public void mockWStockAll(String dateF){
		List<USubject> cl = USubjectService.getInstance()
				.getUSubjectListAStock();
		
		List<USubject> cl2 = USubjectService.getInstance().getUSubjectListHStock();
		cl.addAll(cl2);
		List<List<StockTrade>> sll = new ArrayList<List<StockTrade>>(); 
		for(USubject us : cl){
			String uidentify = us.getUidentify();
			String keyList = getStockTradeListKey(uidentify,dateF);
			WStockMessageListWapper eq2 = LCEnter.getInstance().get(keyList, 
					SCache.CACHE_NAME_wstockcache);
			if(eq2!=null){
				List<StockTrade> list = eq2.getMessageList();
				sll.add(list);
			}
		}
		cl = null;
		cl2 = null;
		if(sll != null){
			Runnable run = new Runnable() {
				private List<List<StockTrade>> sll;
				public void run() {	
					if(sll != null && sll.size() > 0){
						List<StockTrade> stll = null ;//最长的List;
						int longSize = 0;
						for(int i=0;i<sll.size();i++){
							int tmpSize = sll.get(i).size();
							if(tmpSize > longSize){
								longSize = tmpSize;
								stll = sll.get(i);
							}
						}
						 
						for(int i=0;i<longSize;i++){
							try{
								int isstop = ConfigCenterFactory.getInt("wstock.isstop", 0);
								if(isstop==1||isStopMock)
								{
									isStopMock = false;
									return;
								}
								List<StockTrade> stList = new ArrayList<StockTrade>();
								for(int j=0;j<sll.size();j++){
									List<StockTrade> tList = sll.get(j);
									int maxIndex = tList.size()-1;
									if(maxIndex >= i ){
										stList.add(tList.get(i));
									}
									
								}
								//分页发包
								int pageNum = ConfigCenterFactory.getInt("wstock.pageNum", 50);
								if(pageNum <= 0){								
									pageNum = stList.size();
								}
								int sendNum = stList.size()/pageNum + (stList.size()%pageNum==0?0:1);
								for(int k=0;k<sendNum;k++){
									int fromIndex = k*pageNum;
									int toIndex = k*pageNum+pageNum;
									if(toIndex > stList.size() ){
										toIndex = stList.size();
									}
									List<StockTrade> tmpList = new ArrayList<StockTrade>(stList.subList(fromIndex, toIndex));
									String ret = RealTradeService.getInstance().stocktradeToStr(tmpList);
									if(StringUtil.isEmpty(ret) ==false){
										String seed = tmpList.get(0).getCode();
										RealTradeService.getInstance().sendThisGroupRealData(seed, ret, tmpList.get(0).getUptime());
									}	
								}
								
								int mockSleep = ConfigCenterFactory.getInt("wstock.mockSleep", -1);
								if( mockSleep < 0 ){
									if(i == stll.size()-1){
										//最后一点
									}else{
										StockTrade st1 = stll.get(i);
										StockTrade st2 = stll.get(i+1);
										long sleepTime = st2.getUptime()-st1.getUptime();
										if(sleepTime<0)
											sleepTime=1;
										Thread.sleep(sleepTime);
											
									}
								}
								else
								{
									Thread.sleep(mockSleep);
								}
							}catch (Exception e) {
								log.error("模拟全量写进程出错",e);
							}
						}
					}
				}
				
				public Runnable init(List<List<StockTrade>> sll) {
					this.sll = sll;
					return this;
				}
			}.init(sll);	
			Thread thread = new Thread(run);
			thread.setName("模拟全量写进程");
			thread.start();
		}		
	}
	
	public void mockWStockOneCompany(String uidentify,String dateF,Date stime,Date etime){
		Date mstime = stime;
		Date metime = etime;
		if(mstime==null)
			mstime = DateUtil.getDayStartTime(DateUtil.format(dateF));
		if(metime==null)
			metime = DateUtil.getDayEndTime(DateUtil.format(dateF));
		
		final Date ustime = mstime;//指定测试区间的起始时间
		final Date uetime = metime;//指定测试区间的结束时间
		Long lasttime = mstime.getTime();
		String keyList = getStockTradeListKey(uidentify,dateF);
		WStockMessageListWapper eq2 = LCEnter.getInstance().get(keyList, 
				SCache.CACHE_NAME_wstockcache);
		if(eq2!=null){
			List<StockTrade> list = eq2.getMessageList();
			if(list != null){
				for(int i=0;i<list.size();i++){
					try{
						int isstop = ConfigCenterFactory.getInt("wstock.isstop", 0);
						if(isstop==1||isStopMock)
						{
							isStopMock = false;
							return;
						}
						StockTrade st1 = list.get(i);
						List<StockTrade> stList = new ArrayList<StockTrade>(1);
						stList.add(st1);
						String ret = RealTradeService.getInstance().stocktradeToStr(stList);
						if(StringUtil.isEmpty(ret) ==false){
							String seed = stList.get(0).getCode();
							Long itime = st1.getUptime();
							if(itime>ustime.getTime()&&itime<uetime.getTime()&&itime>lasttime)
							{
								lasttime = itime;
								if(SLogFactory.isopen("mock_log"))
									System.out.println("send mock data,stime="+ustime+";etime="+uetime+";time="+new Date(itime)+";lasttime="+new Date(lasttime)+";st="+st1);
								RealTradeService.getInstance().exeStockTradeRefresh_mock(st1);
								RealTradeService.getInstance().sendThisGroupRealData(seed, ret, st1.getUptime());
							}
							else
								continue;
						}	
						int mockSleep = ConfigCenterFactory.getInt("wstock.mockSleep", -1);
						if( mockSleep < 0 ){
							if(i == list.size()-1){
								//最后一点
							}else{
								StockTrade st2 = list.get(i+1);
								long sleepTime = st2.getUptime()-st1.getUptime();
								if(sleepTime<0)
									sleepTime=1;
								Thread.sleep(sleepTime);
							}
						}
						else
						{
							Thread.sleep(mockSleep);
						}
					}catch (Exception e) {
						log.error(uidentify+"模拟写进程出错",e);
					}
				}
			}
//			Runnable run = new Runnable() {
//				private String companycode ;	
//				private List<StockTrade> list;
//				public void run() {	
//					
//				}
//				
//				public Runnable init(String companycode,List<StockTrade> list) {
//					this.companycode = companycode;
//					this.list = list;
//					return this;
//				}
//			}.init(uidentify,list);	
//			Thread thread = new Thread(run);
//			thread.setName(uidentify+"模拟写进程");
//			thread.start();
			
		}
	}
	
	private static String getStockTradeListKey(String companyCode,String dateF) {		
		return "stl." + companyCode+"."+dateF;
	}
	
	public void clearAllData() {
		//先保留今天的
		clearPreDayData();
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance()
				.getCacheImpl();
		cimpl.flushToDisk(SCache.CACHE_NAME_wstockcache);
		
		//全部清理
		Cache c = cimpl.getCache(SCache.CACHE_NAME_wstockcache);
		c.removeAll();		
		log.info("清理行情数据"+SCache.CACHE_NAME_wstockcache+"");
	}
		
	public void clearPreDayData() {
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance()
				.getCacheImpl();
		Cache c = cimpl.getCache(SCache.CACHE_NAME_wstockcache);
		List keyList = c.getKeys();
//		int clearDay = ConfigCenterFactory.getInt("wstock.clearDay", -1);
		int clearDayLog = ConfigCenterFactory.getInt("wstock.clearDayLog", 0);
		Long stime = System.currentTimeMillis();
		String aLastDate = TradeCenter.getInstance().getALastDate();
		if(StringUtil.isEmpty(aLastDate)){
			TradeCenter.getInstance().judgmentATradeClosed();
		}
		long atime = DateUtil.format(aLastDate).getTime();
		String hkLastDate = TradeCenter.getInstance().getHKLastDate();
		long hktime = DateUtil.format(hkLastDate).getTime();
		for(Object key : keyList){
			//1天前的数据都清理掉 //注意周六周日 //ehcache已经加了缓存时间 log.info(Arrays.toString(keyList.toArray()));
			if( key == null ){
				continue;
			}
			try{
				String keyStr = String.valueOf(key);
				if(keyStr.contains("stl.wstock.")){
					//原始数据 时间跟上证指数时间一致
					String[] arr = keyStr.split("\\.");
					String uidentify = "000001.sh";
					String dateF = arr[2];
					Long clearEndTime = atime;//TradeBitMapService.getInstance().getNext(uidentify, stime, clearDay, Calendar.DAY_OF_MONTH);
					if(clearEndTime != null){
						long keyDateTime = DateUtil.format(dateF, DateUtil.YYYYMMDD).getTime();						
						if(keyDateTime < clearEndTime){
							if(clearDayLog == 1){
								log.info("清理行情数据key="+key+"");
							}
							c.remove(key);
						}
					}else{
						log.error("清理行情数据key="+key+"失败");	//+ ",clearDay="+clearDay+"为空");
					}
				}else{
					String[] arr = keyStr.split("\\.");
					String uidentify = arr[1]+"."+arr[2];
					String dateF = arr[3];				
					Long clearEndTime = null ;
					if(keyStr.contains(".hk")){
						clearEndTime = hktime;
					}else{
						clearEndTime = atime;
					}
					//TradeBitMapService.getInstance().getNext(uidentify, stime, clearDay, Calendar.DAY_OF_MONTH);
					if(clearEndTime != null ){
						long keyDateTime = DateUtil.format(dateF, DateUtil.YYYYMMDD).getTime();
						if(keyDateTime < clearEndTime){
							if(clearDayLog == 1){
								log.info("清理行情数据key="+key+"");
							}
							c.remove(key);
						}
					}else{
						log.error("清理行情数据key="+key+"失败");	//+ ",clearDay="+clearDay+"为空");
					}
				}				
			}catch (Exception e) {
				log.error("清理行情数据出错key="+key);
			}
		}
	}
	
	public String stocktradeToStr(List<StockTrade> stList){
		if(stList == null || stList.size()==0 ){
			return null;
		}
		StringBuilder sbuf = new StringBuilder();
		for(int i=0;i<stList.size();i++){
			StockTrade st = stList.get(i);			
//			4707,昨日收盘价
//			4709,今日开盘价
//			4711,成交数量
//			4713,最高成交价
//			4715,最低成交价
//			4717,最近成交价
//			4725,升跌
//			4726,涨跌幅
//			4727,成交金额
			double zs = st.getZs();//昨日收盘价
			double jk = st.getJk();//今日开盘价
			double h = st.getH();//最高价
			double l = st.getL();//最低价
			double c = st.getC();//当前价
			double cjl = st.getCjl();//成交量
			double cje = st.getCje();//成交额
			Double zdf = 0d;
			Double sd = 0d;
			if(c != 0 && zs != 0 ){//新浪上的c=0是停牌
				zdf = (c - zs) / zs;//今日涨幅
				zdf = SMathUtil.getDouble(zdf*100,4);
				sd = c - zs;//今日涨幅
				sd = SMathUtil.getDouble(sd,3);
			}
			sbuf.append(st.getCode()).append('^').append(st.getUptime()).append('|')
			.append("4707").append(':').append(zs).append('^').append("4709").append(':').append(jk).append('^')
			.append("4711").append(':').append(cjl).append('^').append("4713").append(':').append(h).append('^')
			.append("4715").append(':').append(l).append('^').append("4717").append(':').append(c).append('^')
			.append("4725").append(':').append(sd).append('^').append("4726").append(':').append(zdf).append('^')
			.append("4727").append(':').append(cje).append('~');
//		000598.sz^1425398400000|4707:8.35^4709:8.28^4717:8.15^4711:7.5431904E7^4727:6.15053056E8^4726:-0.024^4725:-0.2^4713:8.28^4715:8.09^~
		}
		return sbuf.toString();
	}
	
	private static String getStockTradeKey(String companyCode) {		
		return "st." + companyCode;
	}
	
	public void put2RealMonitor(StockTrade st) {
		if (!isopenRealMonitor)
			return;
		RealtimeDataItem ri = new RealtimeDataItem(st);
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_5);
		ne.setMsg(ri);
		ClientEventCenter.getInstance().putEvent2ChildQueue(EConst.EVENT_5, ne,
				1000);
	}
	
//	public void put2RealMonitorIndustry(String uidentify, double cjl, double zf,
//			Double pcje, long uptime) {
//		if (!isopenRealMonitor)
//			return;
//		RealtimeDataItem ri = new RealtimeDataItem(uidentify, uptime, cjl, zf,pcje,true);
//		NotifyEvent ne = new NotifyEvent();
//		ne.setHType(EConst.EVENT_5);
//		ne.setMsg(ri);
//		ClientEventCenter.getInstance().putEvent2ChildQueue(EConst.EVENT_5, ne,
//				1000);
//	}
	
	public void sendThisGroupRealData(String cs,long uptime) {
		try {
			String indexcodes = ConfigCenterFactory.getString(
					"stock_dc.resave_realtime_notCompute_indexcodes",
					"4707,4709,4717,4711,4727,4726,4725,4713,4715");
			String seed = cs.split(",")[0];
			String ret = getRealTimeTradeIndexs(cs, indexcodes,
					DateUtil.getDayStartTime(new Date()));
			// 由实时发送，改为异步发送
			// DcssTradeIndexServiceClient.getInstance().putRealTimeResult2Cache(
			// seed, ret, false);
			StockPriceBatchMsg sbm = new StockPriceBatchMsg(seed, ret);
			sbm.setTime(uptime);
			TradeService.getInstance().notifyTheStockPriceEvent(sbm);
			int wstock_log_switch = ConfigCenterFactory.getInt("stock_log.wstock_log_switch", 0);		
			String wstock_log_code = ConfigCenterFactory.getString("stock_log.wstock_log_code", "000002.sz,00001.hk");
			if(wstock_log_switch == 1){				
				if(wstock_log_code.contains(cs)){
					String msg = "发送行情cd=["+cs+"],ret=["+ret+"]";									
					String fileName = BaseUtil.getConfigPath("wstock/realTrade.log");
					try {
						//输出日志
						LogSvr.logMsg(msg, fileName);
					} catch (IOException e) {
						log.info("IO错误 输出日志失败" + fileName);
					}
				}
			}
		} catch (Exception e) {
			log.error("send this group data failed!", e);
		}

	}
	
	public void sendThisGroupRealData(String seed,String ret,long uptime) {
		try {
//			String indexcodes = ConfigCenterFactory.getString(
//					"stock_dc.resave_realtime_notCompute_indexcodes",
//					"4707,4709,4717,4711,4727,4726,4725,4713,4715");
//			String seed = cs.split(",")[0];
//			String ret = getRealTimeTradeIndexs(cs, indexcodes,
//					DateUtil.getDayStartTime(new Date()));
			// 由实时发送，改为异步发送
			// DcssTradeIndexServiceClient.getInstance().putRealTimeResult2Cache(
			// seed, ret, false);
			StockPriceBatchMsg sbm = new StockPriceBatchMsg(seed, ret);
			String msgUuid = sbm.getUuid();
			sbm.setTime(uptime);
			TradeService.getInstance().notifyTheStockPriceEvent(sbm);
			LogManager.info("处理实时行情msgUuid="+msgUuid+"，uptime="+uptime+",当前时间="+System.currentTimeMillis());
			
			int wstock_log_switch = ConfigCenterFactory.getInt("stock_log.wstock_log_switch", 0);		
			String wstock_log_code = ConfigCenterFactory.getString("stock_log.wstock_log_code", "000002.sz,00001.hk");
			if(wstock_log_switch == 1){				
				if(wstock_log_code.contains(ret)){
					String msg = "发送行情cd=["+seed+"],ret=["+ret+"]";									
					String fileName = BaseUtil.getConfigPath("wstock/realTrade.log");
					try {
						//输出日志
						LogSvr.logMsg(msg, fileName);
					} catch (IOException e) {
						log.info("IO错误 输出日志失败" + fileName);
					}
				}
			}
		} catch (Exception e) {
			log.error("send this group data failed!", e);
		}

	}
	
	private String getRealTimeTradeIndexs(String companycodes,
			String indexcodes, Date time) {
		StringBuffer sb = IndexService.getInstance().getRealTimeTradeIndexs(
				companycodes, indexcodes, time);
		return sb.toString();
	}
	
	public String getLastRealTradeData(String code){
		List<Long> tl = new ArrayList<Long>(2);
		tl.add(0l);
		tl.add(0l);
		String stockStr =  SelectStockService.getInstance().getStockInfoForMobile(code, tl);
		return stockStr;
	}
}
