package com.yfzx.service.trade;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.mapdb.StockMapdb;
import com.yfzx.service.msg.event.ShareTimerTradeDataWapper;
import com.yz.configcenter.ConfigCenterFactory;

/**
 * 分时图服务类
 * 
 * @author：杨真
 * @date：2014-7-25
 */
public class ShareTimerTradeService {

	Logger log = LoggerFactory.getLogger(ShareTimerTradeService.class);
	static ShareTimerTradeService instance = new ShareTimerTradeService();
	String shareTimerMapDbTablename = "shareTimerMapDbTablename"; 
	public ShareTimerTradeService() {

	}

	public static ShareTimerTradeService getInstance() {
		return instance;
	}

	public void registerShareMapDb()
	{
		StockMapdb.getInstance().registerTable(shareTimerMapDbTablename);
	}
	
	public void initShareMapdb(String key,ShareTimerTradeDataWapper stt)
	{
		StockMapdb.getInstance().put(key, stt, shareTimerMapDbTablename);
	}

	public void initShareTimes() {
		registerShareMapDb();
		List<USubject> acl = USubjectService.getInstance()
				.getUSubjectListAStock();
		List<USubject> hcl = USubjectService.getInstance().getUSubjectListHStock();		
		long currentTimeMillis = System.currentTimeMillis();
		for(USubject us : acl)
		{
			String key = ShareTimerTradeService.getShareTimeKey(us.getUidentify(),currentTimeMillis);
			ShareTimerTradeDataWapper tt = StockMapdb.getInstance().get(key, shareTimerMapDbTablename);
			if(tt==null)
			{
				tt = new ShareTimerTradeDataWapper(key);
			}
			initShareMapdb(key,tt);
		}	
		for(USubject us : hcl)
		{
			String key = ShareTimerTradeService.getShareTimeKey(us.getUidentify(),currentTimeMillis);
			ShareTimerTradeDataWapper tt = StockMapdb.getInstance().get(key, shareTimerMapDbTablename);
			if(tt==null)
			{
				tt = new ShareTimerTradeDataWapper(key);
			}
			initShareMapdb(key,tt);
		}	
		StockMapdb.getInstance().commit();
	}
	
	public String getShareTimerTradeDatas(String uidentify)
	{
		String key = ShareTimerTradeService.getShareTimeKey(uidentify,System.currentTimeMillis());
		ShareTimerTradeDataWapper tt = StockMapdb.getInstance().get(key, shareTimerMapDbTablename);
		if(tt!=null)
		{
			return tt.getShareTimerDatas();
		}
		return null;
	}
	
	/**
	 * 获取最近一个交易日的时分图
	 * @param uidentify
	 * @param type
	 * @return
	 */
	public String getShareTimerTradeDatas(String uidentify,int type)
	{
		
		if(StringUtils.isBlank(uidentify)){
			return null;
		}
		try {
//			String date;
//			if(uidentify.indexOf(".hk")>0){
//				date = TradeCenter.getInstance().getHKLastDate();
//				//log.info("hk real trade query time--"+date);
//			}else{
//				date = TradeCenter.getInstance().getALastDate();
//				//log.info("A real trade query time--"+date);
//			}
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//			String key = uidentify+"^"+DateUtil.getDayStartTime(sdf.parse(date));
			String key = ShareTimerTradeService.getShareTimeKey(uidentify);
			ShareTimerTradeDataWapper tt = StockMapdb.getInstance().get(key, shareTimerMapDbTablename);
			if(tt==null)
			{
				//停牌的处理,
				tt = new ShareTimerTradeDataWapper(key);
				initShareMapdb(key,tt);
			}			
			if(tt!=null)
			{
				if(type>0){
					return tt.getLastData();
				}else{
					return tt.getShareTimerDatas();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 依赖了TradeCenter
	 * @param uidentify
	 * @return
	 */
	public static String getShareTimeKey(String uidentify){
		String date = null;
		//最近交易日
		if(uidentify.split("\\^")[0].indexOf(".hk")>0){
			date = TradeCenter.getInstance().getHKLastDate();
		}else{
			date = TradeCenter.getInstance().getALastDate();
		}
		if(StringUtil.isEmpty(date)){
			return getShareTimeKey(uidentify,System.currentTimeMillis());
		}
		Date startDate = DateUtil.format(date, DateUtil.YYYYMMDD);
		String key = StockUtil.joinString("^", uidentify,startDate.getTime());
		return key;
	}
	
	public static String getShareTimeKey(String uidentify,long stUptime){
		Date startDate = DateUtil.getDayStartTime(new Date(stUptime));
		String key = StockUtil.joinString("^",uidentify,startDate.getTime());
		return key;
	}

	
	public ShareTimerTradeDataWapper getShareTimerTradeDataWapper(String uidentify,long stUptime)
	{
			 
		String key = ShareTimerTradeService.getShareTimeKey(uidentify, stUptime);
		ShareTimerTradeDataWapper tt = StockMapdb.getInstance().get(key, shareTimerMapDbTablename);
		if(tt==null)
		{
			tt = new ShareTimerTradeDataWapper(key);
			StockMapdb.getInstance().put(key, tt, shareTimerMapDbTablename);
		}
		return tt;
	}
	
	public ShareTimerTradeDataWapper getShareTimerTradeDataWapper(String uidentify)
	{
			 
		String key = ShareTimerTradeService.getShareTimeKey(uidentify,System.currentTimeMillis());
		ShareTimerTradeDataWapper tt = StockMapdb.getInstance().get(key, shareTimerMapDbTablename);
		if(tt==null)
		{
			tt = new ShareTimerTradeDataWapper(key);
			StockMapdb.getInstance().put(key, tt, shareTimerMapDbTablename);
		}
		return tt;
	}
	
	
	public void put2Mapdb(String key,Object v)
	{
		StockMapdb.getInstance().putAndCommit(key, v, shareTimerMapDbTablename);
	}
	
	public void flush2Disk(){
		try {
			log.info("flushChances2Disk: " + shareTimerMapDbTablename);
			StockMapdb.getInstance().commitAndClose();
		} catch (Exception e) {
			log.error("ShareTimerTradeService flush2Disk failed!",e);
		}		
	}

	public void clearExpiredShareTimerData() {
		clearExpiredShareTimerData(false);
	}
	
	public void clearExpiredShareTimerData(boolean isCompact) {
		try {
			int dayMinCount = ConfigCenterFactory.getInt("wstock.dayMinCount", 2);
			Map m = StockMapdb.getInstance().getTable(shareTimerMapDbTablename);
			if (m != null) {
				ArrayList<String> keyList = new ArrayList<String>();
				Iterator iter = m.keySet().iterator();
				String date;//当天日期
//				SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
				while (iter.hasNext()) {
					String key = (String) iter.next();
					try{
						//这个格式 是目前服务器默认格式，建议设置TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"));
	//					SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy", Locale.ENGLISH);
						if(key.split("\\^")[0].indexOf(".hk")>0){
							date = TradeCenter.getInstance().getHKLastDate();
						}else{
							date = TradeCenter.getInstance().getALastDate();
						}
	//					Long time = sdf.parse(key.split("\\^")[1]).getTime();
						long time = Long.valueOf(key.split("\\^")[1]);
						//保留最近5天的时分图
						long dateDay = time/86400000;
						long currentDay = DateUtil.format(date, DateUtil.YYYYMMDD).getTime()/86400000;
								
						if ( currentDay - dateDay > dayMinCount ){
							keyList.add(key);					
							//不能直接删除 否则会抛出java.util.ConcurrentModificationException  m.remove(key);
						}
					}catch (Exception e) {
						//KEY 不合法的也删掉
						keyList.add(key);
					}
				}
				for(String key : keyList){
					m.remove(key);
				}
			}
			if(isCompact == true){
				StockMapdb.getInstance().commitAndCompact();
			}else{
				StockMapdb.getInstance().commit();
			}
		} catch (Exception e) {
			log.error("clearExpiredShareTimerData failed!",e);
		}
	}
	
	
}
