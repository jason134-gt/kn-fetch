package com.yz.stock.portal.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.realtime.RealTradeService;
import com.yfzx.service.trade.TradeCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.util.BaseUtil;
import com.yz.mycore.lcs.cache.DiskCache;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.common.BaseAction;
import com.yz.stock.monitor.DeUtil;
import com.yz.stock.monitor.RealRuleParse;
import com.yz.stock.monitor.RealTimeMonitor;
import com.yz.stock.monitor.RealtimeCacheWapper;
import com.yz.stock.monitor.ZfCounter;
import com.yz.stock.portal.cache.WStockDataLoadService;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.realtime.RealDataComputeTimer;

public class WStockAction extends BaseAction {

	private static final long serialVersionUID = 4985250150528639824L;
	Logger log = LoggerFactory.getLogger(RealTradeService.class);
	@Action(value = "/wstock/testReplaceHK")
	public void testReplaceHK()
	{	
		RealDataComputeTimer rdct = RealDataComputeTimer.getInstance();
		rdct.testReplaceDB();
	}
	
	@Action(value = "/wstock/mock")
	public synchronized void mock()
	{	
		try {
			String dateF = NetUtil.getParameterString(
					this.getHttpServletRequest(), "date",
					DateUtil.getSysDate(DateUtil.YYYYMMDD));
			String companycode = NetUtil.getParameterString(
					this.getHttpServletRequest(), "companycode");
			int type = NetUtil.getParameterInt(this.getHttpServletRequest(),
					"type", 0);
			Date stime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"stime", null);
			Date etime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"etime", null);
			TradeCenter.getInstance().setAHTradeOpen();
			System.out.println("mock start...");
			RealtimeCacheWapper.clear_preRulecheckFilter();	
			if (!StringUtil.isEmpty(companycode)) {
				for (String codes : companycode.split(";")) {
					String code = codes;
					if(codes.indexOf(":")>0)
					{
						code = codes.split(":")[1];
					}
					RealTimeMonitor.getInstance().clearRealtimeWapper(code);
					USubject us = USubjectService.getInstance()
							.getUSubjectByUIdentifyFromCache(code);
					if (us == null)
						continue;
					else {
						Calendar cc = Calendar.getInstance();
						cc.setTime(DateUtil.format(dateF));
						cc.set(Calendar.HOUR_OF_DAY, 9);
						cc.set(Calendar.MINUTE, 30);
						cc.clear(Calendar.MILLISECOND);
						cc.clear(Calendar.SECOND);
						us.setKaiPanTime(cc.getTimeInMillis());
					}
				}

			} else {
				if (type == 1) {
					List<USubject> cla = USubjectService.getInstance()
							.getUSubjectListAStock();
					List<USubject> clhk = USubjectService.getInstance()
							.getUSubjectListHStock();
					List<USubject> cl = new ArrayList<USubject>();
					cl.addAll(cla);
					cl.addAll(clhk);
					for (USubject us : cl) {
						RealTimeMonitor.getInstance().clearRealtimeWapper(
								us.getUidentify());
						Calendar cc = Calendar.getInstance();
						cc.setTime(DateUtil.format(dateF));
						cc.set(Calendar.HOUR_OF_DAY, 9);
						cc.set(Calendar.MINUTE, 30);
						cc.clear(Calendar.MILLISECOND);
						cc.clear(Calendar.SECOND);
						us.setKaiPanTime(cc.getTimeInMillis());

					}
				}
			}
			if (type == 1) {
				TradeCenter.getInstance().setAHTradeOpen();
				RealTradeService.getInstance()
						.mockWStockAll(dateF, companycode);
				ZfCounter.getInstance().print();
			} else {
				for (String codes : companycode.split(";")) {
					String code = codes;
					if(codes.indexOf(":")>0)
					{
						code = codes.split(":")[1];
					}
					mockOneAsyn(code, dateF, stime, etime);
				}
			}
			System.out.println("mock end...");
		} catch (Exception e) {
			log.error("mock failed!",e);
		}
	}
	private void mockOneAsyn(final String code, final String dateF, final Date stime, final Date etime) {
		// TODO Auto-generated method stub
		StockFactory.submitTaskBlocking(new Callable<String>(){

			@Override
			public String call() throws Exception {
				RealTradeService.getInstance().mockWStockOneCompany(code, dateF,stime,etime);
				return null;
			}
			
		});
	}
	@Action(value = "/wstock/stopMock")
	public String stopMock(){
		RealTradeService.getInstance().stopMock();
		return SUCCESS;
	}
	@Action(value = "/wstock/clearStatistic")
	public String clearStatistic(){
		ZfCounter.getInstance().clear();
		return SUCCESS;
	}
	@Action(value = "/wstock/printStatistic")
	public String printStatistic(){
		ZfCounter.getInstance().print();
		return SUCCESS;
	}
	
	@Action(value = "/wstock/save")
	public String save(){
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		cimpl.flushToDisk(SCache.CACHE_NAME_wstockcache);
		this.setResultData("保存完成");
		return SUCCESS;
	}
	
	@Action(value = "/wstock/saveCopy")
	public String saveCopy(){
		try {
			String cacheName = SCache.CACHE_NAME_wstockcache;
			Cache c = LCEnter.getInstance().getCache(
					SCache.CACHE_NAME_wstockcache);
			if (c != null) {
				List<Object> fvl = new ArrayList<Object>();
				for (Object k : c.getKeys()) {
					Element e = c.get(k);
					if (e != null) {
						List<Object> cvl = new ArrayList<Object>();
						Object vl = e.getValue();
						if (vl != null && vl instanceof List) {
							List vvl = (List) vl;
							for (int i=0;i<vvl.size();i++) {
								Object v = vvl.get(i);
								cvl.add(v);
							}

						}
						fvl.add(new Element(k, cvl));
					}

				}
				String dcachePath = getDiskCachePath(cacheName);
				if (fvl.size() > 0) {
					DiskCache.getInstance().toDisk(dcachePath, fvl);
				}
				EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance()
						.getCacheImpl();
				cimpl.flushToDisk(SCache.CACHE_NAME_wstockcache);
			}
		} catch (Exception e) {
			log.error("mock saveCopy failed!",e);
		}
		this.setResultData("保存完成");
		return SUCCESS;
	}
	private String getDiskCachePath(String cacheName) {

		return BaseUtil.getConfigPath("dcache/" + cacheName + ".cdata");
	}
	
	@Action(value = "/wstock/clear")
	public String clear(){
		RealTradeService.getInstance().clearPreDayData();	
		this.setResultData("清理完成");
		return SUCCESS;
	}
	
	@Action(value = "/wstock/clearPreRulecheckFilter")
	public String clearPreRulecheckFilter(){
		RealtimeCacheWapper.clear_preRulecheckFilter();	
		this.setResultData("清理clearPreRulecheckFilter完成");
		return SUCCESS;
	}
	
	@Action(value = "/wstock/clearWithDate")
	public String clearWithDate(){
		String dateF = NetUtil.getParameterString(this.getHttpServletRequest(), 
				"date", DateUtil.getSysDate(DateUtil.YYYYMMDD));
		Cache c = LCEnter.getInstance().getCache(SCache.CACHE_NAME_wstockcache);
		if(c!=null)
		{
			for(Object k:c.getKeys())
			{
				if(!k.toString().contains(dateF))
					c.remove(k);
			}
			EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
			cimpl.flushToDisk(SCache.CACHE_NAME_wstockcache);
		}
		this.setResultData("清理完成");
		return SUCCESS;
	}
	
	@Action(value = "/wstock/reload")
	public String reload(){
		System.out.println("mock reload start...");
		Cache c = LCEnter.getInstance().getCache(SCache.CACHE_NAME_wstockcache);
		if(c!=null)
		{
			c.removeAll();
		}
		new WStockDataLoadService().loadDataFromDisk("wstock");
		System.out.println("mock reload end...");
		return SUCCESS;
	}
	
	
	@Action(value = "/wstock/encrypt")
	public String encrypt(){
		try {
			String encryptstr = this.getHttpServletRequest().getParameter("encryptstr");
			byte[] jiamiB = DeUtil.encrypt(encryptstr);
			String result = new String(jiamiB,"ISO-8859-1").trim();
			System.out.println("encrypt str = " + result);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("ret", result);
			StockUtil.outputJson(getHttpServletResponse(), JSONUtil.serialize(m));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return SUCCESS;
	}
	@Action(value = "/wstock/printRule")
	public String printRule()
	{
		try {
		//解析有多少个file
		String fileids = ConfigCenterFactory.getString(
				"realtime_server.file_ids", "");
		if(!StringUtil.isEmpty(fileids))
		{
			for(String fileid:fileids.split(","))
			{
				
				String ruleids = ConfigCenterFactory.getString(
						fileid+".rulelists_ids", "");
				if(!StringUtil.isEmpty(ruleids))
				{
					for(String ruleid:ruleids.split(","))
					{
						String rules = ConfigCenterFactory.getString(
								fileid+".rulelists_"+ruleid, "");
						if (!StringUtil.isEmpty(rules)) {
							String nrules = new String(DeUtil.decrypt(rules.getBytes("ISO-8859-1")));
							System.out.println(nrules+"\n");
							System.out.println();
						}
					}
				}
			}
		}
		
	} catch (Exception e) {
		e.printStackTrace();
	}
	return SUCCESS;
	}
}
