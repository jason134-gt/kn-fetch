package com.yz.stock.realtime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Trade0001;
import com.stock.common.model.USubject;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.msg.RealtimeComputeDataUpdateMsg;
import com.stock.common.util.CountableThreadPool;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.StockUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexCacheService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.RealTimeService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.hfunction.HDayService;
import com.yfzx.service.hfunction.HDaySumService;
import com.yfzx.service.realtime.RealTradeService;
import com.yfzx.service.realtime.SinaStockService;
import com.yfzx.service.realtime.WStockService;
import com.yfzx.service.trade.Trade0001Service;
import com.yfzx.service.trade.TradeCenter;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.inter.IStartable;
import com.yz.mycore.core.inter.IStopable;
import com.yz.mycore.daf.manager.CacheRefreshTimer;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.monitor.RealtimeCacheWapper;

/**
 * 实时数据计算
 * 
 * @author：杨真
 * @date：2014-4-10
 */
public class RealDataComputeTimer implements IStopable, IStartable {

	Logger log = LoggerFactory.getLogger(RealDataComputeTimer.class);
	static RealDataComputeTimer instance = new RealDataComputeTimer();
	Set<Long> _hasComputeToday = new HashSet<Long>();
	Set<Long> _hasComputeToday1 = new HashSet<Long>();
	static Map<String, List<String>> _rcache = new HashMap<String, List<String>>();

	public RealDataComputeTimer() {

	}

	public static RealDataComputeTimer getInstance() {
		return instance;
	}

	boolean isclosed = false;
	static long lastAllCompute = System.currentTimeMillis();
	static long lastDayAvgCompute = System.currentTimeMillis();
	Long lastJudgment = System.currentTimeMillis();
	boolean isrefreshed = false;

	public void startRealCompute() {
		fetchRealtimeDateSinaAll();
		fetchRealtimeDate();

		Thread t = new Thread(new Runnable() {

			public void run() {

				while (true) {

					int isopen = ConfigCenterFactory.getInt(
							"realtime_server.open_real_time_compute_thread", 0);
					long computeInterval = ConfigCenterFactory.getLong(
							"realtime_server.real_compute_interval", 1800000l);
					if (System.currentTimeMillis() - lastJudgment > 20000) {
						TradeCenter.getInstance().judgmentATradeClosed();
						lastJudgment = System.currentTimeMillis();
					}
					int state = isTradeOpen();
					Date td = DateUtil.getDayStartTime(new Date());
					// 如果今天还没有算过，就计算
					if (!_hasComputeToday.contains(td.getTime())) {
						fetchRealtimeDateSinaAll();
						computeAIndexs();
						computeAHIndexs();
						computeDayTradeHavg();
						doSendIndexs();
						_hasComputeToday.clear();
						_hasComputeToday.add(td.getTime());
						isrefreshed = false;
						RealtimeCacheWapper.clear_preRulecheckFilter();	
					} else {
						if (isopen == 1 && state == 1) {
							// 全量算
							if (System.currentTimeMillis() - lastAllCompute >= computeInterval) {
								computeAIndexs();
								doSendIndexs();
								lastAllCompute = System.currentTimeMillis();
							}
							// 每五分种计算下日均线
							if (System.currentTimeMillis() - lastDayAvgCompute >= 300000) {
								computeAHIndexs();
								computeDayTradeHavg();
								doSendIndexs();
								lastDayAvgCompute = System.currentTimeMillis();
							}
						}
					}
					if (IndexService.getInstance().checkNeedFetchOrCompute()) {
						computeAIndexs();
						computeAHIndexs();
					}
					try {
						Thread.sleep(60000l);
					} catch (Exception e) {
						log.error("thread sleep failed!", e);
					}
				}

			}

		});
		t.setName("realtimerComputeThread");
		t.start();

		CacheRefreshTimer.getInstance().registerStart(this);
		CacheRefreshTimer.getInstance().registerStop(this);
	}

	public void startRealtimeFetch() {

		Thread t1 = new Thread(new Runnable() {

			public void run() {
				while (true) {
					long starttime = System.currentTimeMillis();
					int isopen = ConfigCenterFactory.getInt(
							"realtime_server.open_real_time_compute_thread", 0);
					long interval = ConfigCenterFactory
							.getLong(
									"realtime_server.fetch_realtime_tradedata_interval",
									1000l);
					if (System.currentTimeMillis() - lastJudgment > 20000) {
						TradeCenter.getInstance().judgmentATradeClosed();
						lastJudgment = System.currentTimeMillis();
					}
					int state = isTradeOpen();
					Date td = DateUtil.getDayStartTime(new Date());
					// 如果今天还没有算过，就计算
					if (!_hasComputeToday1.contains(td.getTime())) {
						// 清除前一天的开市时间
						clearTradeOpenTime();
						reloadPreDayData();
						fetchRealtimeAndSend();
						_hasComputeToday1.clear();
						_hasComputeToday1.add(td.getTime());
						RealtimeCacheWapper.clear_preRulecheckFilter();	
						addTimeTask();
					} else {
						if (isopen == 1 && state == 1) {
							fetchRealtimeAndSend();
						}
					}

					try {
						// 运行时间 如果超过interval,则跳过线程暂停 减少暂停时间
						long runtime = System.currentTimeMillis() - starttime;
						if (runtime >= interval) {
							Thread.sleep(interval);
						} else {
							Thread.sleep((interval - runtime));
						}
					} catch (Exception e) {
						log.error("thread sleep failed!", e);
					}
				}

			}
			/**
			 * 重新加载前几天的行情与扩展指标数据
			 */
			private void reloadPreDayData() {
				try {
					int pred = ConfigCenterFactory.getInt(
							"realtime_server.reloadPreDayData_pred", -4);
					Date ptime = StockUtil.getNextTimeV3(
							DateUtil.getDayStartTime(new Date()), pred,
							Calendar.DAY_OF_MONTH);
					log.info("load preday tradedata,time =" + ptime);
					IndexCacheService.getInstance().refreshAndFlush2Disk(ptime);
					log.info("load preday tradedata end,time =" + ptime);
				} catch (Exception e) {
					log.error("reloadPreDayData  failed!", e);
				}
				
			}

		});
		t1.setName("realtimerFetchTradeDataThread");
		t1.start();
	}

	/**
	 * 增加一个定时任务 需要在休市的时候 执行一次全量行情 A股是15:02 H股是16:02
	 */
	private void addTimeTask() {
		Date sysStartDate = new Date();
		{
			/*** 晚上11点半 清理掉所有wstock数据 SCache.CACHE_NAME_wstockcache ***/			
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 30);
			calendar.set(Calendar.SECOND, 0);
			Date runDate = calendar.getTime();
			// 如果你设定在凌晨2点执行任务。但你是在2点以后发布的程序或是重启过服务，那这样的情况下，任务会立即执行
			if (runDate.after(sysStartDate)) {
				Timer timer = new Timer();				
				timer.schedule(new TimerTask() {
					public void run() {						
						try{
							RealTradeService.getInstance().clearAllData();
							EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();							
							cimpl.flushToDisk(SCache.CACHE_NAME_wstockcache);
						}catch(Exception e){
							log.error("清理行情数据出错",e);
						}
					}						
				}, runDate);
			}
			
		}
		
		{
			/*** 定制每日15:01执行全量A股方法 ***/
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, 15);
			calendar.set(Calendar.MINUTE, 2);
			calendar.set(Calendar.SECOND, 0);
			Date runDate = calendar.getTime();
			// 如果你设定在凌晨2点执行任务。但你是在2点以后发布的程序或是重启过服务，那这样的情况下，任务会立即执行
			if (runDate.after(sysStartDate)) {
				Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					public void run() {
						String wstockIsopen = ConfigCenterFactory.getString(
								"realtime_server.wstock_isopen", "false");
						if ("true".equals(wstockIsopen)) {
							log.info("addTimeTask 每日15:02执行全量A股方法wstock");
							WStockService.getIntance().getSZStockAll();
							WStockService.getIntance().getSHStockAll();						
						} else {
							log.info("addTimeTask 每日15:02执行全量A股方法sina");
							List<USubject> cl = USubjectService.getInstance()
									.getUSubjectListAStock();
							getRealTimeTradeData_A(cl);
						}
						
						String dateStr = TradeCenter.getInstance().getALastDate();
						String nowDateStr = DateUtil.getSysDate(DateUtil.YYYYMMDD);
						if(nowDateStr.equals(dateStr)){
							String zss = ConfigCenterFactory.getString(
									"realtime_server.zs_code_list", "000001.sh,000002.sh");
							String[] zsArr = zss.split(",");
							for(String zs : zsArr){
								String uidentify = zs;
								Trade0001 trade0001 = getTrade0001ByCode(uidentify);
								if(trade0001 != null){
								trade0001.setF001v("深交所");
									USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
									trade0001.setSecname(us.getName());							
									Trade0001Service.getInstance().replaceAZs(trade0001);
								}
							}							
						}
					}
				}, runDate);
			}
		}
		{
			Calendar calendar2 = Calendar.getInstance();
			/*** 定制每日16:01执行全量H股方法 ***/
			calendar2.set(Calendar.HOUR_OF_DAY, 16);
			calendar2.set(Calendar.MINUTE, 2);
			calendar2.set(Calendar.SECOND, 0);
			Date runDate2 = calendar2.getTime();
			if (runDate2.after(sysStartDate)) {
				Timer timer2 = new Timer();
				timer2.schedule(new TimerTask() {
					public void run() {
						List<USubject> cl = USubjectService.getInstance().getUSubjectListHStock();
						String wstockIsopen = ConfigCenterFactory.getString(
								"realtime_server.wstock_isopen", "false");
						if ("true".equals(wstockIsopen)) {
							log.info("每日16:02执行全量H股方法wstock");
							WStockService.getIntance().getHKStockAll();
						} else {
							log.info("每日16:02执行全量H股方法sina");						
							getRealTimeTradeData_H(cl);
						}					
						String dateStr = TradeCenter.getInstance().getHKLastDate();
						String nowDateStr = DateUtil.getSysDate(DateUtil.YYYYMMDD);
						if(nowDateStr.equals(dateStr)){
							for(USubject us : cl){
								String uidentify = us.getUidentify();
								Trade0001 trade0001 = getTrade0001ByCode(uidentify);
								if(trade0001 != null){
									trade0001.setF001v("港交所");
									trade0001.setSecname(us.getName());
									//TODO 增加把HK入库的操作
									Trade0001Service.getInstance().replaceHK(trade0001);
								}
							}
							
							log.info("定时下午4点执行存储"+SCache.CACHE_NAME_wstockcache);
//							RealTradeService.getInstance().clearPreDayData();
							EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
							cimpl.flushToDisk(SCache.CACHE_NAME_wstockcache);
						}
					}
				}, runDate2);
			}
		}
	}
		
	/**
	 * TODO 远程测试方法，测试通过后，删掉
	 */
	public void testReplaceDB(){
		List<USubject> cl = USubjectService.getInstance().getUSubjectListHStock();
		WStockService.getIntance().getHKStockAll();
		String dateStr = TradeCenter.getInstance().getHKLastDate();
		String nowDateStr = DateUtil.getSysDate(DateUtil.YYYYMMDD);
		if(nowDateStr.equals(dateStr)){
			for(USubject us : cl){
				String uidentify = us.getUidentify();
				Trade0001 trade0001 = getTrade0001ByCode(uidentify);
				if(trade0001 != null){
					trade0001.setF001v("港交所");
					trade0001.setSecname(us.getName());
					//TODO 增加把HK入库的操作
					Trade0001Service.getInstance().replaceHK(trade0001);
				}
			}
		}
		{
			WStockService.getIntance().getSZStockAll();
			WStockService.getIntance().getSHStockAll();	
			String zss = ConfigCenterFactory.getString(
					"realtime_server.zs_code_list", "000001.sh,000002.sh");
			String[] zsArr = zss.split(",");
			for(String zs : zsArr){
				String uidentify = zs;
				Trade0001 trade0001 = getTrade0001ByCode(uidentify);
				trade0001.setF001v("深交所");
				USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
				trade0001.setSecname(us.getName());							
				Trade0001Service.getInstance().replaceAZs(trade0001);
			}
		}
	}
	
	private Trade0001 getTrade0001ByCode(String uidentify){
		String key = getStockTradeKey(uidentify);
		//StockTrade to Trade0001
		StockTrade st = LCEnter.getInstance().get(key,
				SCache.CACHE_NAME_marketcache);
		if( st == null ){
			log.error("getTrade0001ByCode 缺少公司"+uidentify);
			return null;
		}
		Date time = DateUtil.getDayStartTime(st.getUptime());
		Trade0001 trade0001 = new Trade0001();							
		trade0001.setCompanyCode(uidentify);
		trade0001.setTime(time);//st.uptime是从wstock获取过来的
		trade0001.setF002n(new Double(st.getZs()).floatValue());
		trade0001.setF003n(new Double(st.getJk()).floatValue());
		trade0001.setF004n(new Double(st.getCjl()).longValue());
		trade0001.setF005n(new Double(st.getH()).floatValue());
		trade0001.setF006n(new Double(st.getL()).floatValue());
		trade0001.setF007n(new Double(st.getC()).floatValue());
		trade0001.setF012n(new Double(st.getCje()).longValue());
		
		
		Double v = null;
//		v = RealTimeService.getInstance().realTimeComputeIndex(
//				uidentify, "2348", time);//3584
//		if (v != null && v != 0) {
//			trade0001.setF009n(new Double(v).floatValue());
//		}
//		v = RealTimeService.getInstance().realTimeComputeIndex(
//				uidentify, "2350", time);
//		if (v != null && v != 0) {
//			trade0001.setF065n(new Double(v).floatValue());
//		}
//		v = RealTimeService.getInstance().realTimeComputeIndex(
//				uidentify, "3249", time);
//		if (v != null && v != 0) {
//			trade0001.setF066n(new Double(v).floatValue());
//		}
//		v = RealTimeService.getInstance().realTimeComputeIndex(
//				uidentify, "2352", time);
//		if (v != null && v != 0) {
//			trade0001.setF067n(new Double(v).floatValue());
//		}
		
		trade0001.setF010n(new Double(st.getC()-st.getZs()).floatValue());
		float zdf = 0.0f;
		if(st.getC() != 0 && st.getZs() != 0 ){//新浪上的c=0是停牌
			zdf = new Double((st.getC() - st.getZs()) / st.getZs() * 100 ).floatValue();//今日涨幅					
		}
		trade0001.setF011n(zdf);
		//我们的市值是总市值还是 港股的没有股本，
//		v = RealTimeService.getInstance().realTimeComputeIndex(
//				uidentify, "3015", time);
//		if (v != null && v != 0) {
//			trade0001.setF016n(new Double(v).floatValue());
//		}
		/*
			`F008N` BIGINT(20) NULL DEFAULT NULL COMMENT '总笔数',
		`F009N` DECIMAL(8,2) NULL DEFAULT NULL COMMENT '市盈率',
			`F065N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '市净率',
			`F066N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '市现率',
			`F067N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '市销率',
		`F010N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '升跌',
		`F011N` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '涨跌幅',		
			`F013N` DECIMAL(10,3) NULL DEFAULT NULL COMMENT '周转率',
			`F014N` BIGINT(20) NULL DEFAULT NULL COMMENT '发行总股本',
			`F015N` BIGINT(20) NULL DEFAULT NULL COMMENT '流通股本',
			`F016N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '市值',
			`F017N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '流通市值',
		*/
		v = RealTimeService.getInstance().realTimeComputeIndex(
				uidentify, StockConstants.INDEX_CODE_TRADE_5AVG, time);
		if (v != null && v != 0) {
			trade0001.setF018n(new Double(v).floatValue());
		}
		v = RealTimeService.getInstance().realTimeComputeIndex(
				uidentify, StockConstants.INDEX_CODE_TRADE_10AVG, time);
		if (v != null && v != 0) {
			trade0001.setF019n(new Double(v).floatValue());
		}
		v = RealTimeService.getInstance().realTimeComputeIndex(
				uidentify, StockConstants.INDEX_CODE_TRADE_30AVG, time);
		if (v != null && v != 0) {
			trade0001.setF020n(new Double(v).floatValue());
		}
		
		/*
		`F018N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '5日均价',
		`F019N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '10日均价',
		`F020N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '30日均价',
			`F021N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '120日均价',
			`F040N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '向后复权后今日开盘价',
			`F041N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '向后复权后最近成交价',
			`F042N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '向后复权后最高成交价',
			`F043N` DECIMAL(10,4) NULL DEFAULT NULL COMMENT '向后复权后最低成交价',
		*/
		//4745 是从trade2704中获取的数据
		/*
			`F044N` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '5日涨跌幅',
			`F045N` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '10日涨跌幅',
			`F046N` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '30日涨跌幅',
			`F047N` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '120日涨跌幅',
			`F048N` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '52周涨跌幅（360日）',
			`F049N` DECIMAL(18,4) NULL DEFAULT NULL COMMENT '5日成交总量',
			`F050N` DECIMAL(18,4) NULL DEFAULT NULL COMMENT '10日成交总量',
			`F051N` DECIMAL(18,4) NULL DEFAULT NULL COMMENT '30日成交总量',
			`F052N` DECIMAL(18,4) NULL DEFAULT NULL COMMENT '5日成交金额',
			`F053N` DECIMAL(18,4) NULL DEFAULT NULL COMMENT '10日成交金额',
			`F054N` DECIMAL(18,4) NULL DEFAULT NULL COMMENT '30日成交金额',
			`F055N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '5日换手率',
			`F056N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '10日换手率',
			`F057N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '30日换手率',
			`F058N` DECIMAL(18,2) NULL DEFAULT NULL COMMENT '120日换手率',
			`F060N` DECIMAL(18,6) NULL DEFAULT NULL COMMENT '5日流动性指标',
		*/	
		
		v = HDaySumService.getInstance().computeDaySum(uidentify,
			StockConstants.INDEX_CODE_TRADE_CJL,time,5);
		if (v != null && v != 0) {
			trade0001.setF049n(new Double(v).floatValue());
		}		
		v = HDaySumService.getInstance().computeDaySum(uidentify,
				StockConstants.INDEX_CODE_TRADE_CJL,time,10);
		if (v != null && v != 0) {
			trade0001.setF050n(new Double(v).floatValue());
		}
		v = HDaySumService.getInstance().computeDaySum(uidentify,
				StockConstants.INDEX_CODE_TRADE_CJL,time,30);
		if (v != null && v != 0) {
			trade0001.setF051n(new Double(v).floatValue());
		}
		v = HDaySumService.getInstance().computeDaySum(uidentify,
				StockConstants.INDEX_CODE_TRADE_CJE,time,5);
		if (v != null && v != 0) {
			trade0001.setF052n(new Double(v).floatValue());
		}		
		v = HDaySumService.getInstance().computeDaySum(uidentify,
				StockConstants.INDEX_CODE_TRADE_CJE,time,10);
		if (v != null && v != 0) {
			trade0001.setF053n(new Double(v).floatValue());
		}
		v = HDaySumService.getInstance().computeDaySum(uidentify,
				StockConstants.INDEX_CODE_TRADE_CJE,time,30);
		if (v != null && v != 0) {
			trade0001.setF054n(new Double(v).floatValue());
		}		
		
		return trade0001;
	}
	
	private static String getStockTradeKey(String companyCode) {		
		return "st." + companyCode;
	}

	// 港股或A股，只要有一个开市，就算开市
	private int isTradeOpen() {
		int st0 = TradeCenter.getInstance().getTradeStatus(0);
		int st1 = TradeCenter.getInstance().getTradeStatus(1);
		if (st0 == 1 || st1 == 1)
			return 1;
		return 0;
	}

	// public void computeAIndexs() {
	// if (isclosed)
	// return;
	// lastAllCompute = System.currentTimeMillis();
	// try {
	// log.info("****************************************real time compute start!****************************************");
	// if (SLogFactory.isopen("realtime_compute_logopen"))
	// log.info("start compute real time index!....");
	// computeAIndexsOfNeedRealtime();
	// if (SLogFactory.isopen("realtime_compute_logopen"))
	// log.info("end compute real time index!....");
	// } catch (Exception e) {
	// log.error("real compute data failed!", e);
	// }
	// log.info("****************************************real time compute end!****************************************");
	// try {
	// Thread.sleep(1000l);
	// } catch (Exception e) {
	// log.error("thread sleep failed!", e);
	// }
	//
	// }

	public void doSendIndexs() {
		try {
			log.info("start save index 2 remote cache!....");
			String codes = ConfigCenterFactory.getString(
					"stock_dc.resave_realtime_indexcodes",
					"2348,2349,2350,2351,2352,3015,3249");
			List<USubject> cl = USubjectService.getInstance()
					.getUSubjectAHZList();
			reSave2RemoteCache(cl, codes, true);
			log.info("end save index 2 remote cache!....");
		} catch (Exception e) {
			log.error("real compute data failed!", e);
		}
	}

	public void fetchRealtimeAndSend() {
		if (isclosed)
			return;
		fetchRealtimeDate();
		// 调用层已经休眠过了，Thread.sleep代码不需要了 ，下一层fetchRealtimeDate()里面有try和日志，也可以省掉
		// try {
		// log.info("****************************************real time fetch start!****************************************");
		// fetchRealtimeDate();
		// // log.info("start save base index 2 remote cache!....");
		// // String codes = ConfigCenterFactory.getString(
		// // "stock_dc.resave_realtime_notCompute_indexcodes",
		// // "4707,4709,4717,4711,4727,4726,4725,4713,4715");
		// // List<USubject> cl =
		// //
		// USubjectService.getInstance().getUSubjectListByType(StockConstants.SUBJECT_TYPE_0);
		// // reSave2RemoteCache(cl,codes,false);
		// // log.info("end save base index 2 remote cache!....");
		// } catch (Exception e) {
		// log.error("real compute data failed!", e);
		// }
		// log.info("****************************************real time fetch end!****************************************");
		//
		// try {
		// Thread.sleep(1000l);
		// } catch (Exception e) {
		// log.error("thread sleep failed!", e);
		// }
	}

	public void fetchFromSinaAndSend() {
		try {
			List<USubject> cl = USubjectService.getInstance()
					.getUSubjectListAStock();
			getRealTimeTradeData_A(cl);
			// }
			// if(st1 == 1){
			cl = USubjectService.getInstance().getUSubjectListHStock();
			getRealTimeTradeData_H(cl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 全量获取新浪行情数据
	 */
	private void fetchRealtimeDateSinaAll() {
		try {
			log.info("start 全量获取新浪行情数据!....");
			List<USubject> cl = USubjectService.getInstance()
					.getUSubjectListAStock();
			String skey = "sl_a";
			List<String> sl = _rcache.get(skey);
			if (sl == null) {
				sl = getSl(cl, 0);
				if (sl != null)
					_rcache.put(skey, sl);
			}
			for (int i = 0; i < sl.size(); i++) {
				String sc = sl.get(i);
				SinaStockService.getIntance().getAStock(sc);
				try {
					Thread.sleep(300l);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			cl = USubjectService.getInstance().getUSubjectListHStock();
			skey = "sl_h";
			sl = _rcache.get(skey);
			if (sl == null) {
				sl = getSl(cl, 0);
				if (sl != null)
					_rcache.put(skey, sl);
			}
			for (int i = 0; i < sl.size(); i++) {
				String sc = sl.get(i);
				SinaStockService.getIntance().getHStock(sc);
				try {
					Thread.sleep(300l);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			log.info("end 全量获取新浪行情数据!....");
			TradeCenter.getInstance().judgmentATradeClosed();
		} catch (Exception e) {
			log.error("real fetch data failed!", e);
		}
	}

	private void fetchRealtimeDate() {
		try {

			long starttime = System.currentTimeMillis();
			List<USubject> cl = null;
			// int st0 = TradeCenter.getInstance().getTradeStatus(0);
			// int st1 = TradeCenter.getInstance().getTradeStatus(1);

			String wstockIsopen = ConfigCenterFactory.getString(
					"realtime_server.wstock_isopen", "false");
			if ("true".equals(wstockIsopen)) {
				if (SLogFactory.isopen("fetch_wstock_isopen"))
					log.info("start fetch real time trade info from wstock!....");
				// long systime = System.currentTimeMillis();
				// 接口频率不能低于0.3秒
				Future<Long> shFuture = CountableThreadPool.getInstance()
						.submit(new Callable<Long>() {
							@Override
							public Long call() throws Exception {
								long systime = System.currentTimeMillis();
								WStockService.getIntance().getSHStockRefresh();
								return System.currentTimeMillis() - systime;
							};
						});
				// 此处先注销，看wstock是否会否会被防火墙屏蔽
				// try {
				// long subTime = System.currentTimeMillis() - systime;
				// if(subTime < 300l){
				// Thread.sleep(300l-subTime);
				// }
				// systime = System.currentTimeMillis();
				// } catch (Exception e) {
				// }

				Future<Long> szFuture = CountableThreadPool.getInstance()
						.submit(new Callable<Long>() {
							@Override
							public Long call() throws Exception {
								long systime = System.currentTimeMillis();
								WStockService.getIntance().getSZStockRefresh();
								return System.currentTimeMillis() - systime;
							};
						});
				// try {
				// long subTime = System.currentTimeMillis() - systime;
				// if(subTime < 300l){
				// Thread.sleep(300l-subTime);
				// }
				// } catch (Exception e) {
				// }

				Future<Long> hkFuture = CountableThreadPool.getInstance()
						.submit(new Callable<Long>() {
							@Override
							public Long call() throws Exception {
								long systime = System.currentTimeMillis();
								WStockService.getIntance().getHKStockRefresh();
								return System.currentTimeMillis() - systime;
							};
						});
				// try {
				// long subTime = System.currentTimeMillis() - systime;
				// if(subTime < 300l){
				// Thread.sleep(300l-subTime);
				// }
				// systime = System.currentTimeMillis();
				// } catch (Exception e) {
				// }

				Long shLong = shFuture.get(30000, TimeUnit.MILLISECONDS);
				Long szLong = szFuture.get(30000, TimeUnit.MILLISECONDS);
				Long hkLong = hkFuture.get(30000, TimeUnit.MILLISECONDS);
				int timechart_log_switch = ConfigCenterFactory.getInt(
						"stock_log.timechart_log_switch", 0);
				if (timechart_log_switch == 1) {
					log.info("处理shLong耗时" + shLong + ",处理szLong耗时" + szLong
							+ ",处理hkLong耗时" + hkLong);
				}
				if (SLogFactory.isopen("fetch_wstock_isopen"))
					log.info("end fetch real time trade info from wstock!.... 耗时="
							+ (System.currentTimeMillis() - starttime) + "毫秒");
			} else {
				if (SLogFactory.isopen("fetch_wstock_isopen"))
					log.info("start fetch real time trade info from sina!....");
				// if(st0 == 1){
				cl = USubjectService.getInstance().getUSubjectListAStock();
				getRealTimeTradeData_A(cl);
				// }
				// if(st1 == 1){
				cl = USubjectService.getInstance().getUSubjectListHStock();
				getRealTimeTradeData_H(cl);
				// }
				if (SLogFactory.isopen("fetch_wstock_isopen"))
					log.info("end fetch real time trade info from sina!.... 耗时="
							+ (System.currentTimeMillis() - starttime) + "毫秒");
			}

		} catch (Exception e) {
			log.error("real fetch data failed!", e);
		}

	}

	private void reSave2RemoteCache(List<USubject> cl, String indexcodes,
			boolean computeAvg) {

		if (cl == null)
			return;
		Map<Integer, StringBuffer> pm = new HashMap<Integer, StringBuffer>();
		String sips = ConfigCenterFactory.getString(
				"dcss.ext_cache_server_ip_list", "192.168.1.102:7777");

		String[] ips = sips.split("\\^");
		// 对公司分成多组，拉取数据时按组取
		int pagesize = ConfigCenterFactory.getInt(
				"stock_zjs.fetch_reatime_hq_page", 20);
		int page = cl.size() / pagesize;
		for (int i = 0; i < page; i++) {
			int start = i * pagesize;
			if (start > cl.size())
				break;
			int end = start + pagesize;
			if (end > cl.size())
				end = cl.size();
			pm.clear();
			List<USubject> scl = cl.subList(start, end);
			for (USubject c : scl) {
				Integer index = SExt.getUExtTableIndex(c.getUidentify(),
						SExt.EXT_TABLE_TYPE_2) % ips.length;
				StringBuffer sb = pm.get(index);
				if (sb == null) {
					sb = new StringBuffer();
					pm.put(index, sb);
				}
				sb.append(c.getUidentify());
				sb.append(",");
			}
			try {
				Iterator<Integer> iter = pm.keySet().iterator();
				while (iter.hasNext()) {
					Integer index = iter.next();
					StringBuffer sb = pm.get(index);
					if (sb != null) {
						String cs = sb.toString();
						String seed = cs.split(",")[0];
						String ret = getRealTimeTradeIndexs(cs, indexcodes,
								DateUtil.getDayStartTime(new Date()));
						//改为udp方式
//						DcssTradeIndexServiceClient.getInstance()
//								.putRealTimeResult2Cache(seed, ret, computeAvg);
						RealtimeComputeDataUpdateMsg sbm = new RealtimeComputeDataUpdateMsg(seed, ret);
						TradeService.getInstance().notifyTheRealtimeComputeDataUpdateEvent(sbm);
					}
				}
			} catch (Exception e) {
				log.error("realtime fetch data failed!", e);
			}
		}
	}

	public String getRealTimeTradeIndexs(String companycodes,
			String indexcodes, Date time) {
		StringBuffer sb = IndexService.getInstance().getRealTimeTradeIndexs(
				companycodes, indexcodes, time);
		return sb.toString();
	}

	public void computeAIndexs() {
		String indexcodes = ConfigCenterFactory.getString(
				"stock_dc.a_realtime_compute_indexcodes",
				"2348,2349,2350,2351,2352,3015,3249,3276,3017,4591,4800");
		// 计算A股特有指标
		List<USubject> ul = USubjectService.getInstance()
				.getUSubjectListAStock();
		if (SLogFactory.isopen("realtime_compute_logopen"))
			log.info("realtime compute A stock indexs start ....");
		for (USubject us : ul) {
			computeOneCompanyRealIndexS(us, indexcodes);
		}
		if (SLogFactory.isopen("realtime_compute_logopen"))
			log.info("realtime compute A stock indexs end ....");
	}

	public void computeAHIndexs() {
		String indexcodes = ConfigCenterFactory
				.getString(
						"stock_dc.ah_realtime_compute_indexcodes",
						"4766,4767,4770,4773,4771,4772,4768,4769,4776,4775,4776,4781,4782,4779,4780,4777,4778,4783");
		if (SLogFactory.isopen("realtime_compute_logopen"))
			log.info("realtime compute A stock indexs start ....");
		List<USubject> ul = USubjectService.getInstance().getUSubjectListAStock();
		for (USubject us : ul) {
			computeOneCompanyRealIndexS(us, indexcodes);
		}
		ul = USubjectService.getInstance().getUSubjectListHStock();
		for (USubject us : ul) {
			computeOneCompanyRealIndexS(us, indexcodes);
		}
		if (SLogFactory.isopen("realtime_compute_logopen"))
			log.info("realtime compute A stock indexs end ....");

	}

	private void computeOneCompanyRealIndexS(USubject us, String indexcodes) {

		Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
				us.getUidentify());
		if (c == null) {
			return;
		}
		// Stock0001 s = Stock0001Service.getInstance()
		// .getStock0001ByCompanycodeFromCache(c.getCompanyCode());
		// if (s == null)
		// continue;

		String[] ia = indexcodes.split(",");
		for (String indexcode : ia) {
			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(indexcode);
			if (d == null)
				continue;

			Date time = DateUtil.getDayStartTime(new Date());
			Double v = RealTimeService.getInstance().realTimeComputeIndex(
					us.getUidentify(), indexcode, time);
			if (v != null && v != 0) {
				RealTimeService.getInstance().put(us.getUidentify(),
						Integer.valueOf(indexcode), v);
			}

		}
	}

	private void getRealTimeTradeData_A(List<USubject> cl) {
		log.info("---------------fetch a stock realtime data start !-------------------");
		// sina行情接口
		String skey = "sl_a";
		List<String> sl = _rcache.get(skey);
		if (sl == null) {
			sl = getSl(cl, 0);
			if (sl != null)
				_rcache.put(skey, sl);
		}
		for (int i = 0; i < sl.size(); i++) {
			String sc = sl.get(i);
			SinaStockService.getIntance().getAStock(sc);
		}
		log.info("---------------fetch a stock realtime data end !-------------------");
	}

	public void clearTradeOpenTime() {
		List<USubject> cl = USubjectService.getInstance()
				.getUSubjectListByType(StockConstants.SUBJECT_TYPE_0);
		for (USubject us : cl) {
			us.removeAttr("_kpt");
		}
	}

	private void getRealTimeTradeData_H(List<USubject> cl) {
		log.info("---------------fetch h stock realtime data start !-------------------");
		String skey = "sl_h";
		List<String> sl = _rcache.get(skey);
		if (sl == null) {
			sl = getSl(cl, 0);
			if (sl != null)
				_rcache.put(skey, sl);
		}
		for (int i = 0; i < sl.size(); i++) {
			String sc = sl.get(i);
			SinaStockService.getIntance().getHStock(sc);

		}

		log.info("---------------fetch h stock realtime data end !-------------------");
	}

	/**
	 * type:0:sl,1:osl
	 * 
	 * @param cl
	 * @param type
	 * @return
	 */
	private List<String> getSl(List<USubject> cl, int type) {
		// 对公司分成多组，拉取数据时按组取
		int pagesize = ConfigCenterFactory.getInt(
				"stock_dc.fetch_sina_hq_page", 50);
		int page = cl.size() / pagesize;
		List<String> sl = new ArrayList<String>();
		List<String> osl = new ArrayList<String>();
		for (int i = 0; i < page; i++) {
			int start = i * pagesize;
			if (start > cl.size())
				break;
			int end = start + pagesize;
			if (end > cl.size())
				end = cl.size();
			List<USubject> scl = cl.subList(start, end);
			StringBuffer sb = new StringBuffer();
			StringBuffer osb = new StringBuffer();
			for (USubject c : scl) {
				// Company cc =
				// CompanyService.getInstance().getCompanyByCodeFromCache(c.getUidentify());
				// if(cc==null)
				// continue;
				String uid = c.getUidentify().toLowerCase();
				if (!uid.contains(".sh") && !uid.contains(".sz")
						&& !uid.contains(".hk"))
					continue;
				String[] ca = c.getUidentify().split("\\.");
				String sc = ca[1] + ca[0];
				sb.append(sc);
				sb.append(",");

				osb.append(c.getUidentify());
				osb.append(",");
			}
			sl.add(sb.toString());
			osl.add(osb.toString());
		}
		if (type == 0)
			return sl;
		if (type == 1)
			return osl;
		return sl;
	}

	public void start() {
		isclosed = false;
		_hasComputeToday.clear();
		
	}

	public void stop() {
		isclosed = true;
		
		log.info("RealDataComputeTimer执行保存"+SCache.CACHE_NAME_wstockcache);
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		cimpl.flushToDisk(SCache.CACHE_NAME_wstockcache);
	}

	public void computeDayTradeHavg() {
		List<USubject> ul = USubjectService.getInstance().getUSubjectAHZList();
		for (USubject us : ul) {
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
					us.getUidentify());
			if (c == null) {
				// log.info("company is not exsit!companyname:" + us.getName());
				continue;
			}
			// Stock0001 s = Stock0001Service.getInstance()
			// .getStock0001ByCompanycodeFromCache(c.getCompanyCode());
			// if (s == null)
			// continue;
			HDayService.getInstance().computeDayTradeHavg(c.getCompanyCode());
		}

	}

	public void realMonitorIndustry() {
		Thread t = new Thread(new Runnable() {

			public void run() {

				while (true) {
					int isopen = ConfigCenterFactory.getInt(
							"realtime_server.open_real_time_compute_thread", 0);
					// long computeInterval = ConfigCenterFactory.getLong(
					// "realtime_server.real_compute_interval", 7200000l);
					if (System.currentTimeMillis() - lastJudgment > 20000) {
						TradeCenter.getInstance().judgmentATradeClosed();
						lastJudgment = System.currentTimeMillis();
					}
					int state = isTradeOpen();
					Date td = DateUtil.getDayStartTime(new Date());
					Date pred = IndexService.getInstance()
							.getNTradeTimeAfterCurDay("000001.sh", td, -1);
					if (isopen == 1 && state == 1) {

						try {

							String monitorItems = ConfigCenterFactory
									.getString(
											"realtime_server.industry_monitor_item",
											"");
							String[] mis = monitorItems.split(",");
							for (int i = 0; i < mis.length; i++) {
								String tag = mis[i];

								try {
									Double avgCje = 0.0;
									Double avgZf = 0.0;
									Double avgPcje = 0.0;
									List<Company> cl = CompanyService
											.getInstance()
											.getCompanyListByTagFromCache(tag);
									if (cl != null && cl.size() != 0) {
										int count = 0;

										for (Company c : cl) {

											IndexMessage im1;
											Double xv = 0.0;
											Date ctime = new Date();
											String zhnlp = "2021";
											// 收益率
											ctime = IndexService.getInstance()
													.getRealtime(zhnlp,
															c.getCompanyCode());
											if (ctime != null) {
												im1 = SMsgFactory.getUMsg(
														c.getCompanyCode(),
														zhnlp, ctime);
												im1.setNeedAccessExtIndexDb(false);
												im1.setNeedAccessCompanyBaseIndexDb(false);
												im1.setNeedAccessExtRemoteCache(true);
												xv = IndexValueAgent
														.getIndexValue(im1);
												if (xv == null || xv == 0
														|| xv < 0.10) {
													continue;
												}
											}

											Double v = IndexValueAgent.getIndexValue(
													c.getCompanyCode(),
													StockConstants.INDEX_CODE_TRADE_CJE,
													td);
											if (v != null && v != 0) {
												avgCje += v;
											}

											v = IndexValueAgent.getIndexValue(
													c.getCompanyCode(),
													StockConstants.INDEX_CODE_TRADE_CJE,
													pred);
											if (v != null && v != 0) {
												avgPcje += v;
											}

											v = IndexValueAgent.getIndexValue(
													c.getCompanyCode(),
													StockConstants.INDEX_CODE_TRADE_ZDF,
													td);
											if (v != null && v != 0) {
												avgZf += v;
											}
											count++;
										}
										if (count != 0 && count > 3) {
											avgCje /= count;
											avgZf /= count;
											avgPcje /= count;
//											RealTradeService
//													.getInstance()
//													.put2RealMonitorIndustry(
//															tag,
//															avgCje,
//															avgZf,
//															avgPcje,
//															System.currentTimeMillis());
										}
									}
								} catch (Exception e) {
									log.error("compute failed!", e);
								}

							}

						} catch (Exception e) {
							log.error("compute failed!", e);
						}
					}
					try {
						Thread.sleep(10000l);
					} catch (Exception e) {
						log.error("thread sleep failed!", e);
					}
				}

			}

		});
//		t.setName("realtimerMonitorIndustryThread");
//		t.start();

	}
}
