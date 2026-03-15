package com.yfzx.service.realtime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.USubject;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.util.DateUtil;
import com.stock.common.util.Spider;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.USubjectService;
import com.yz.configcenter.ConfigCenterFactory;

public class SinaStockService {

	private static SinaStockService instance=new SinaStockService();
	Logger log = LoggerFactory.getLogger(SinaStockService.class);
	
	public static SinaStockService getIntance(){
		return instance;
	}
	
	private static String buildQstr(String sc) {
		String hzs = ConfigCenterFactory.getString("stock_dc.companycode_hzs",
				"sh;sz;hk");
		for (String hz : hzs.split(";")) {
			if (sc.indexOf(hz) >= 0)
				return sc.replace(hz, "") + "." + hz;
		}

		return sc;
	}
	
	/**
	 * 获取A股行情
	 * @param sc
	 */
	public void getAStock(String sc){
		try {
			Long time = Calendar.getInstance().getTimeInMillis();
			// http://hq.sinajs.cn/?_=1395367865527&list=sh600864,sh600519
			// http://hq.sinajs.cn/?_=1395367865527&list=gb_qihu
			String url = "http://hq.sinajs.cn/?_=" + time + "&list=" + sc;
			String s = Spider.urlSpider(url, "gbk");
			if (!StringUtil.isEmpty(s)) {
				Long uptime = null;
				StringBuffer sbuf = new StringBuffer();
				List<StockTrade> stList = new ArrayList<StockTrade>();
				for (String sci : s.split(";")) {
					if (!StringUtil.isEmpty(sci)
							&& sci.split(",").length > 2) {
						String[] heada = sci.split("=")[0].split("_");
						String uidentify = buildQstr(heada[heada.length - 1]);
						USubject c = USubjectService.getInstance()
								.getUSubjectByUIdentifyFromCache(uidentify);
						if (c == null) {
							log.info("not found USubject!uidentify="
									+ uidentify);
							continue;
						}
						String[] sa = sci.split(",");
						//新浪接口上证指数的股数有问题，000001.sh,000002.sh,000003.sh,000004.sh,000005.sh,000006.sh,000007.sh,000008.sh,000820.sh,510210.sh,510211.sh
						String zs = ConfigCenterFactory.getString("wstock.sinashzs", "000001.sh,000002.sh,000003.sh,000004.sh,000005.sh,000006.sh,000007.sh,000008.sh,000820.sh,510210.sh,510211.sh");
						StockTrade stocktrade = null;
						if(zs.contains(uidentify)){
							stocktrade = new StockTrade(Double.valueOf(sa[5]),
								Double.valueOf(sa[4]),
								Double.valueOf(sa[3]),
								Double.valueOf(sa[2]),
								Double.valueOf(sa[1]),
								Double.valueOf(sa[8])*100,
								Double.valueOf(sa[9]));
						}else{
							stocktrade = new StockTrade(Double.valueOf(sa[5]),
									Double.valueOf(sa[4]),
									Double.valueOf(sa[3]),
									Double.valueOf(sa[2]),
									Double.valueOf(sa[1]),
									Double.valueOf(sa[8]),
									Double.valueOf(sa[9]));
						}
						stocktrade.setCode(uidentify);
						stocktrade.setName(c.getName());
						String timeString = sa[sa.length - 3] + " "
								+ sa[sa.length - 2];
						Date update = DateUtil.format(timeString);
						if (update != null) {
							long uptimeTemp = update.getTime();
							stocktrade.setUptime(uptimeTemp);
						}
						sbuf.append(uidentify).append(",");
						if(uptime == null){
							uptime = update.getTime();
						}
						RealTradeService.getInstance().exeStockTradeRefresh(stocktrade);
						stList.add(stocktrade);
					}
				}
//				if(uptime != null){
//					RealTradeService.getInstance().sendThisGroupRealData(sbuf.toString(),uptime);
//				}
				String ret = RealTradeService.getInstance().stocktradeToStr(stList);
				if(StringUtil.isEmpty(ret) ==false){
					String seed = stList.get(0).getCode();
					RealTradeService.getInstance().sendThisGroupRealData(seed, ret, uptime);
				}
			}
		}catch (Exception e) {e.printStackTrace();
			log.error("抓取新浪A股行情异常"+e);
		}
	}
	
	/**
	 * 获取H股行情
	 * @param sc
	 */
	public void getHStock(String sc){
		try {
			Long time = Calendar.getInstance().getTimeInMillis();
			// http://hq.sinajs.cn/?_=1395367865527&list=sh600864,sh600519
			// http://hq.sinajs.cn/?_=1395367865527&list=gb_qihu
			String url = "http://hq.sinajs.cn/?_=" + time + "&list=" + sc;
			String s = Spider.urlSpider(url, "gbk");
			if (!StringUtil.isEmpty(s)) {
				Long uptime = null;
				StringBuffer sbuf = new StringBuffer();
				List<StockTrade> stList = new ArrayList<StockTrade>();
				for (String sci : s.split(";")) {
					if (!StringUtil.isEmpty(sci)
							&& sci.split(",").length > 2) {
						String[] heada = sci.split("=")[0].split("_");
						String uidentify = buildQstr(heada[heada.length - 1]);
						USubject c = USubjectService.getInstance()
								.getUSubjectByUIdentifyFromCache(uidentify);
						if (c == null) {
							log.info("not found USubject!uidentify="
									+ uidentify);
							continue;
						}
						String[] sa = sci.split(",");
						StockTrade stocktrade = new StockTrade(Double.valueOf(sa[5]),
								Double.valueOf(sa[4]),
								Double.valueOf(sa[6]),
								Double.valueOf(sa[3]),
								Double.valueOf(sa[2]),
								Double.valueOf(sa[12]),
								Double.valueOf(sa[11]));
						stocktrade.setCode(uidentify);
						stocktrade.setName(c.getName());
						String end = sa[sa.length - 1];
						if (end.endsWith("\"")) {
							end = end.replace("\"", "");
							if (end.split(":").length == 2) {
								end = end + ":00";
							}
						}
						String timeString = sa[sa.length - 2].replace("/",
								"-") + " " + end;
						Date update = DateUtil.format(timeString);						
						if (update != null) {
							long uptimeTemp = update.getTime();
							stocktrade.setUptime(uptimeTemp);
						}
						sbuf.append(uidentify).append(",");
						if(uptime == null){
							uptime = update.getTime();
						}
						RealTradeService.getInstance().exeStockTradeRefresh(stocktrade);
						stList.add(stocktrade);
					}
				}
//				if(uptime != null){
//					RealTradeService.getInstance().sendThisGroupRealData(sbuf.toString(),uptime);
//				}
				String ret = RealTradeService.getInstance().stocktradeToStr(stList);
				if(StringUtil.isEmpty(ret) ==false){
					String seed = stList.get(0).getCode();
					RealTradeService.getInstance().sendThisGroupRealData(seed, ret, uptime);
				}
			}
		}catch (Exception e) {e.printStackTrace();
			log.error("抓取新浪港股行情异常"+e);
		}
	}
	

}
