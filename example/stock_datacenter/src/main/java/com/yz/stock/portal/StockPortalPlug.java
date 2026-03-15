package com.yz.stock.portal;

import net.sf.ehcache.Cache;

import com.stock.common.bloomfilter.BFConst;
import com.stock.common.bloomfilter.BFUtil;
import com.stock.common.constants.StockConstants;
import com.stock.common.expression.SimpleExpresion;
import com.stock.common.model.snn.EConst;
import com.stock.common.util.StockUtil;
import com.yfzx.service.agent.CompileDefaultDataFetcherImpl;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.msg.handler.c.UserNotifyClientHandler;
import com.yfzx.yas.YasManager;
import com.yz.mycore.core.inter.IStopable;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.plug.IPlugIn;
import com.yz.mycore.core.plug.Irefresh;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.stock.portal.cache.LocalCacheManger;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.register.StockRegister;
import com.yz.stock.snn.Snner;
import com.yz.stock.trade.outer.OuterDataCenter;

public class StockPortalPlug implements IPlugIn ,Irefresh,IStopable{

	public void plugIn() {
		BaseFactory.getSysRegister().register(StockConstants.STOCK_REGISTER, new StockRegister());
		StockFactory.getMyRegister().register(StockConstants.SIMPLE_EXPRESION,SimpleExpresion.getInstance());
		//初始化数据到缓存
		LocalCacheManger lcm = LocalCacheManger.getInstance();
		lcm.init();
		StockFactory.init();
		StockUtil.setIDataFetcher(new CompileDefaultDataFetcherImpl());
		YasManager.startClient();
		OuterDataCenter.init();
		CompanyService.getInstance().initAllCompanyTagsSet();
		IndustryService.getInstance().initIndustryJson();
		
		BFUtil.registerFilter(BFConst.newCFNotifyBF, 8, 1000000l);
		BFUtil.registerFilter(BFConst.tradeAlarm, 8, 10000000l);
		
		ClientEventCenter.registerHandle("UserNotifyClientHandlerThread",Integer.valueOf(EConst.EVENT_3),new UserNotifyClientHandler(),2);
//		Thread t = new Thread(new Runnable(){
//			long last = CompanyService.getInstance().getAssetLatestModtime().getTime();
//			public void run() {
//				//先休息一下再工作
//				try {
//					Thread.sleep(300000);
//				} catch (Exception e) {
//					// TODO: handle exception
//				}
//				//初次启动，往前推一天
//				last=last - 1800000l;
//				while(true)
//				{
//						try {
//							try {
//								Calendar uc = Calendar.getInstance();
//								//在最近一次更新时间，提前一个小时有更新的公司
//								long c_interval = ConfigCenterFactory
//										.getLong(
//												"stock_dc.dataupdate_check_compute_interval",
//												60000l);
//								uc.setTimeInMillis(last+c_interval);
//								// 把最新更新的数据导入正式库
//								Date cuptime = uc.getTime();
//								System.out.println("-------------------------------last update cuptime:"+cuptime);
//								TaskEnter.getInstance()
//										.importNewData2NormalDb_timer(cuptime);
//								//检查数据有没有更新
//								if (CompanyService.getInstance().isDataUpdate(
//										cuptime)) {
////									last = Calendar.getInstance()
////											.getTimeInMillis();
//									last = CompanyService.getInstance().getAssetLatestModtime().getTime();
//									System.out.println("-----------------------last update time:"+CompanyService.getInstance().getAssetLatestModtime());
//									DataCenterTimer.getInstance()
//											.datacompute_auto(cuptime);
//									
//									Calendar nc = Calendar.getInstance();
//									nc.setTimeInMillis(System.currentTimeMillis());
//									//运算结束后，把最后一次开始运算时间写到库中
//									SystemPropertiesService.getInstance().update("last_compute_date", DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));
//									
//								}
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//							//每一分钟检查一次
//							long interval = ConfigCenterFactory
//									.getLong(
//											"stock_dc.dataupdate_check_interval",
//											300000l);
//							Thread.sleep(interval);
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					
//				}
//				
//			}
//			
//			
//		});
//		t.setName("thread_check_data_update");
////		t.start();
	}

	public void refresh() {
		// TODO Auto-generated method stub
		
	}

	public void beforeRefresh() {
		Cache c = LCEnter.getInstance().getCache(StockConstants.common);
		if(c!=null)
		{
			c.removeAll();
		}
		Snner.getInstance().clearRecord();
	}

	@Override
	public void stop() {
		BFUtil.flushAllBF();
		
	}

}
