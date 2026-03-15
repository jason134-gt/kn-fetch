package com.yfzx.service.msg.event;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Trade0001;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.util.DateUtil;
import com.stock.common.util.LogSvr;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.DcssTradeIndexServiceClient;
import com.yfzx.service.trade.ShareTimerTradeService;
import com.yfzx.service.trade.StockTradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.util.BaseUtil;

/**
 * 分时图数据包装类
 * 
 * @author：杨真
 * @date：2014年9月4日
 */
public class ShareTimerTradeDataWapper implements Serializable {

	private static final long serialVersionUID = 2408136364565673335L;
	private final static Logger log = LoggerFactory.getLogger(ShareTimerTradeDataWapper.class);
	SortedSet<Integer> hhmmSet = Collections.synchronizedSortedSet(new TreeSet<Integer>());
	String key;
	//上一次收到的点 ，新点进来后，覆盖，处理完后最新一点
	StockTrade slast;
	/**
	 * 最后持久化的行情【上一分钟的最后一点】
	 */
	StockTrade lastStorage;
	//临时对象，用于过程中使用
	StockTrade defualtlast;
	StringBuffer sb = new StringBuffer();
	Long lastPersitanceTime = System.currentTimeMillis();
	long lastRealTradeTime =0l;
	

	public ShareTimerTradeDataWapper(String key) {
		//key必须是uidentify+"^"+DateUtil.getDayStartTime(new Date());格式
		this.key = key;
	}
	/**
	 * 只保留每分种的最后一个价格
	 * @param item
	 */
	public void put(StockTrade item) {
		//数据异常
		if(item.getCode() == null || item.getUptime() == 0l){				
			return;
		}
		
		String companycode = this.getKey().split("\\^")[0];
		long currentTimeMillis = System.currentTimeMillis();	
		int sharetimer_log_switch = ConfigCenterFactory.getInt("stock_log.sharetimer_log_switch", 0);
		if(sharetimer_log_switch ==  1){
			if(currentTimeMillis - item.getUptime() > 60000){
				log.error("ShareTimerTradeDataWapper 消息间隔超过1分钟["+companycode+"]");
			}
		}

		int iMinute = DateUtil.longtimeToMinuteofday(item.getUptime());

		//TODO 这段代码用处不太大，用于防止异常的数据插入。可以删掉		
//		long time = currentTimeMillis;
//		int iDay = DateUtil.longtimeToDayofyear(item.getUptime());		
//		try{
//			long keyTime = Long.valueOf(this.getKey().split("\\^")[1]);
//			time = keyTime;
//		}catch (Exception e) {			
//		}
//		int cDay = DateUtil.longtimeToDayofyear(time);
//		
//		// 限制有行情当天，接收到所有数据，不接收后一天的数据
//		if(iDay != cDay){
//			return;
//		}
		
		//a股和港股的开盘，收盘，午间休市时间 【当天的分钟数，如9:30=9小时*60分钟+30分钟=570分钟】
		int minute_open ;		
		int minute_noon_start;
		int minute_noon_end;
		int minute_close ;		
		if(isHStock(companycode)){
//			stockType = StockConstants.STOCK_TYPE_HK;
			minute_open = ConfigCenterFactory.getInt("stock_zjs.hk_minute_open", 570);			
			minute_noon_start = ConfigCenterFactory.getInt("stock_zjs.hk_minute_noon_start", 719);
			minute_noon_end = ConfigCenterFactory.getInt("stock_zjs.hk_minute_noon_end", 780);
			minute_close = ConfigCenterFactory.getInt("stock_zjs.hk_minute_open", 960);			
		}else{
//			stockType = StockConstants.STOCK_TYPE_A;
			minute_open = ConfigCenterFactory.getInt("stock_zjs.a_minute_open", 570);			
			minute_noon_start = ConfigCenterFactory.getInt("stock_zjs.a_minute_noon_start", 689);
			minute_noon_end = ConfigCenterFactory.getInt("stock_zjs.a_minute_noon_end", 780);
			minute_close = ConfigCenterFactory.getInt("stock_zjs.a_minute_close", 900);
		}
		
		
		
		//打印日志	
		int show_log = ConfigCenterFactory.getInt("stock_log.timechart_log_switch", 0);
		if(show_log==1){
			String wstock_log_code = ConfigCenterFactory.getString("stock_log.wstock_log_code", "000002.sz,00001.hk");
			if(wstock_log_code.contains(companycode)){
				String thisTime = DateUtil.format2String(new Date(item.getUptime()));
				String fileName = BaseUtil.getConfigPath("wstock/zjsTrade.log");
				String mesInfo = item.getCode()+"当前"+thisTime+"--"+getStr(item);
				try {
					LogSvr.logMsg(mesInfo, fileName);
				} catch (IOException e) {
					log.info("IO错误 输出日志失败" + fileName);
				}
			}
		}
		
		//9点15 到9点30之前的数据 盘前交易-->改成9点30 
		if( minute_open-30 <= iMinute   && iMinute < minute_open ){
			iMinute = minute_open;
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(item.getUptime());
			c.set(Calendar.MINUTE, minute_open%60);
			c.set(Calendar.SECOND, 0);
			item.setUptime(c.getTimeInMillis());
			slast = item;
			return;
		}
		if( ((minute_noon_start+20)<iMinute&&iMinute<minute_noon_end) ||  (minute_close +20) < iMinute  ){
			return;
		}
		
		synchronized (lastPersitanceTime) {
		
			//今天第一条数据
			if(slast==null){				
				slast = item;
				StockTrade tmpStockTrade = new StockTrade();
				if(minute_open < iMinute){
					if(sharetimer_log_switch ==  1){
						log.info("["+companycode+"]put第一次补全");
					}
					int tmpMinute = minute_open;
					Calendar c = Calendar.getInstance();
					c.setTimeInMillis(item.getUptime());
					c.set(Calendar.SECOND, 59);
					int h = tmpMinute / 60;
					int m = tmpMinute % 60;
					c.set(Calendar.HOUR_OF_DAY, h);
					c.set(Calendar.MINUTE, m);					
					//调整成只补头和尾的点
					double cprice = item.getZs();
					long tmpStime = c.getTimeInMillis();
					tmpStockTrade.setUptime(tmpStime);					
					tmpStockTrade.setC(cprice);
					tmpStockTrade.setZs(cprice);
					tmpStockTrade.setJk(cprice);
					append2Cache(tmpStockTrade);
					int tmpMinute2;
					if(minute_close == iMinute || minute_noon_start == iMinute){
						tmpMinute2 = iMinute;
					}else{
						tmpMinute2 = iMinute - 1;//当前时间的上一分钟,作为最后一点
					}					
					if(minute_noon_start<iMinute&&iMinute<minute_noon_end ){
						tmpMinute2 = minute_noon_start;
					}
					else if(minute_close < iMinute){
						tmpMinute2 = minute_close;
					}
					//首尾2点不重复时
					if(tmpMinute2 != tmpMinute){
						tmpMinute=tmpMinute2;
						h = tmpMinute / 60;
						m = tmpMinute % 60;
						c.set(Calendar.HOUR_OF_DAY, h);
						c.set(Calendar.MINUTE, m);
						tmpStime = c.getTimeInMillis();						
						if( iMinute >= minute_close){
							item.setUptime(tmpStime);
							append2Cache(item);
						}else{
							tmpStockTrade.setUptime(tmpStime);
							append2Cache(tmpStockTrade);
						}						
					}
					slast = item;//slast = tmpStockTrade;
					lastPersitanceTime = System.currentTimeMillis();
					ShareTimerTradeService.getInstance().put2Mapdb(key, this);	
				}				
			}else{
				if(slast.getUptime() == item.getUptime()){
					//秒数都相同的数据，不重复加载
					return;
				}
				int sMinute = DateUtil.longtimeToMinuteofday(slast.getUptime());
				//在同一分钟内，且新来的要大些，则直接替换，否则就把上一个加到队列保存下来
				if(iMinute == sMinute){
					if(slast.getUptime()<item.getUptime()){						
						slast = item;	
						//持久化问题可能影响slast=item
						long chancePersitanceTimeInterval = ConfigCenterFactory.getLong("stock_zjs.ShareTimerPersitanceTimeInterval2", 30000l);
						//间隔一段时间持久化一次
						if(System.currentTimeMillis() - lastPersitanceTime>chancePersitanceTimeInterval)
						{
							synchronized (lastPersitanceTime) {
								if(System.currentTimeMillis() - lastPersitanceTime>chancePersitanceTimeInterval)
								{										
									//持久化
									lastPersitanceTime = System.currentTimeMillis();
									ShareTimerTradeService.getInstance().put2Mapdb(key, this);					
								}
							}
						}
					}
				}else{//非同一分钟了
					//秒数都相同的数据，不重复加载
					if(slast.getUptime() == item.getUptime()){						
						return;
					}
					
//					int lMinute ;
//					if(lastStorage ==null){
//						lMinute = minute_open -1;
//					}else{
//						lMinute = DateUtil.longtimeToMinuteofday(lastStorage.getUptime());
//					}
//					
//					//插入的数据跟上次持久的数据时间差2分钟,且最后以一个点不存在  进行补全
//					if( (iMinute - lMinute > 2 && lMinute != minute_close )){
//						StockTrade tmpStockTrade  = slast;						
//						if(sharetimer_log_switch ==1 ){
//							log.info("["+companycode+"]put补全");
//						}
//						Calendar c = Calendar.getInstance();
//						c.setTimeInMillis(item.getUptime());
//						c.set(Calendar.SECOND, 59);						
////						if(lastStorage == null){//slast存在，但是lastStorage不存在，来了新一分钟的数据
////							append2Cache(slast);
////						}
//						//重复的，里面有去除
//						if( DateUtil.longtimeToMinuteofday(slast.getUptime()) < minute_close){
//							append2Cache(slast);
//						}
//						
//						int tmpMinute;
//						if(minute_close == iMinute || minute_noon_start == iMinute){
//							tmpMinute = iMinute;
//						}else{
//							tmpMinute = iMinute - 1;//当前时间的上一分钟,作为最后一点
//						}
//						if(minute_noon_start<tmpMinute&&tmpMinute<minute_noon_end ){
//							tmpMinute = minute_noon_start;
//						}
//						else if(minute_close <= tmpMinute){
//							tmpMinute = minute_close;
//							tmpStockTrade = item;
//						}				
//						int	h = tmpMinute / 60;
//						int	m = tmpMinute % 60;
//						c.set(Calendar.HOUR_OF_DAY, h);
//						c.set(Calendar.MINUTE, m);
//						long tmpStime = c.getTimeInMillis();						
//						tmpStockTrade.setUptime(tmpStime);
//						append2Cache(tmpStockTrade);					
//						slast = item;
//						lastPersitanceTime = System.currentTimeMillis();
//						//补全时候不存储
////						ShareTimerTradeService.getInstance().put2Mapdb(key, this);	
//					}else{
//						//不需要补全的，将上一个点持久化
//						if(DateUtil.longtimeToMinuteofday(slast.getUptime()) <=minute_close){
//							append2Cache(slast);
//						}
//						//替换成最新的
//						slast = item;
//						//等于收盘时，进行点追加
//						if( iMinute >= minute_close){
//							addLastPoint(item,minute_close);
//						}
//					}
					
					//不需要补全的，将上一个点持久化
					if(DateUtil.longtimeToMinuteofday(slast.getUptime()) <=minute_close){
						append2Cache(slast);
					}
					//替换成最新的
					slast = item;
					//持久化问题可能影响slast=item
					long chancePersitanceTimeInterval = ConfigCenterFactory.getLong("stock_zjs.ShareTimerPersitanceTimeInterval2", 30000l);
					//间隔一段时间持久化一次
					if(System.currentTimeMillis() - lastPersitanceTime>chancePersitanceTimeInterval)
					{
						synchronized (lastPersitanceTime) {
							if(System.currentTimeMillis() - lastPersitanceTime>chancePersitanceTimeInterval)
							{										
								//持久化
								lastPersitanceTime = System.currentTimeMillis();
								ShareTimerTradeService.getInstance().put2Mapdb(key, this);					
							}
						}
					}
				}
				
				//等于收盘时，进行点追加 //中午收盘时，新浪接口不一定能拉出11:29的正确数据
				if(iMinute >= minute_noon_start && iMinute < minute_noon_end ){
					boolean toWrite = false;
					if(iMinute > minute_noon_start && iMinute < minute_noon_end){
						//30-35的都直接写
						toWrite = true;
					}else if(iMinute == minute_noon_start){
						//超过50秒的,写入
						int offset = TimeZone.getDefault().getRawOffset();
						int secOfMinute = (int) ((item.getUptime() + offset) % 86400000 % 60000/1000);
						if(secOfMinute > 50){
							toWrite = true;
						}
					}
					
					if(toWrite == true){
						addLastPoint(item,minute_noon_start);
					}
				}else{
					if( iMinute >= minute_close ){
						addLastPoint(item,minute_close);
					}
				}				
			}
		}
	}
	
	/**
	 * @param item
	 * @param minute_close 最后的收市分钟数[a股，港股不一样]
	 */
	private void addLastPoint(StockTrade item,int minute_close){
		if(item.getC() == 0){//停牌情况下，不处理
			return;
		}
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(item.getUptime());		
//		int h = c.get(Calendar.HOUR_OF_DAY);
//		int m = c.get(Calendar.MINUTE);
		c.set(Calendar.MINUTE, minute_close%60);
		lastStorage = item;
		defualtlast = item;
		slast = item;
		lastPersitanceTime = System.currentTimeMillis();
		item.setUptime(c.getTimeInMillis());
		if(hhmmSet.contains((minute_close))){			
			int indexOf = sb.deleteCharAt(sb.length()-1).lastIndexOf("~");
			sb.delete(indexOf+1,sb.length()).append(getStr(item));
		}else{
			hhmmSet.add((minute_close));
			sb.append(getStr(item));
		}
		ShareTimerTradeService.getInstance().put2Mapdb(key, this);
		//打印日志	
		int show_log = ConfigCenterFactory.getInt("stock_log.timechart_log_switch", 0);
		if(show_log==1){
			String wstock_log_code = ConfigCenterFactory.getString("stock_log.wstock_log_code", "000002.sz,00001.hk");
			if(wstock_log_code.contains(item.getCode())){
				log.info("trade_sf  add last point "+item.getUptime() +" "+ getStr(item));		
			}
		}
		
	}
	
	//最后一条记录时间
	public long getRealTradeDate(){
		if(lastStorage!=null){
			return lastStorage.getUptime();
		}else{
			return lastRealTradeTime;
		}
	}
	
	private String getStr(StockTrade item){
		lastRealTradeTime = item.getUptime();
		String companycode = this.getKey().split("\\^")[0];
		StringBuffer strBuf = new StringBuffer();
		if(isHStock(companycode)){
			strBuf.append(SMathUtil.getDouble(item.getC(),3));
			strBuf.append("^");
			strBuf.append(item.getCjl());
			strBuf.append("^");
			strBuf.append(item.getUptime());
			strBuf.append("^");
			strBuf.append(SMathUtil.getDouble(item.getZs(),3));
			strBuf.append("^");
			strBuf.append(SMathUtil.getDouble(item.getH(),3));
			strBuf.append("^");
			strBuf.append(SMathUtil.getDouble(item.getL(),3));
			strBuf.append("^");
			strBuf.append(SMathUtil.getDouble(item.getCje(),3));
			strBuf.append("^");
			String thisTime = DateUtil.format2String(new Date(item.getUptime()));
			strBuf.append(thisTime);
			strBuf.append("~");
		}else{
			strBuf.append(SMathUtil.getDouble(item.getC(),2));
			strBuf.append("^");
			strBuf.append(item.getCjl());
			strBuf.append("^");
			strBuf.append(item.getUptime());
			strBuf.append("^");
			strBuf.append(SMathUtil.getDouble(item.getZs(),2));
			strBuf.append("^");
			strBuf.append(SMathUtil.getDouble(item.getH(),2));
			strBuf.append("^");
			strBuf.append(SMathUtil.getDouble(item.getL(),2));
			strBuf.append("^");
			strBuf.append(SMathUtil.getDouble(item.getCje(),2));
			strBuf.append("^");
			String thisTime = DateUtil.format2String(new Date(item.getUptime()));
			strBuf.append(thisTime);
			strBuf.append("~");
		}		
		return strBuf.toString();
	}

	private void append2Cache(StockTrade item) {
		try{
			if(item.getC() == 0){
				return;
			}
			int iMinute = DateUtil.longtimeToMinuteofday(item.getUptime());
			if(iMinute == 0 ){
				return;
			}
			
			int hhmm = iMinute;
			if(hhmmSet.contains(hhmm)){
	//			log.error("存入了同一分钟的内容,item="+NosqlBeanUtil.bean2Map(item)+",slast="+NosqlBeanUtil.bean2Map(slast)+"last="+NosqlBeanUtil.bean2Map(last));
				return;
			}
			if(hhmmSet.isEmpty() ==false){
				int lastHhmm = hhmmSet.last();
				if(hhmm < lastHhmm ){//新加的点，比以前的点时间更小
					return;
				}
			}
			hhmmSet.add(hhmm);
			if(lastStorage != null){
				//解决下一时刻的成交量小于上一时刻成交量
				if(lastStorage.getCjl() > item.getCjl() ){
					String fileName = BaseUtil.getConfigPath("wstock/zjsTrade.log");
					String mesInfo = item.getCode()+"成交量异常 "+getStr(lastStorage)+"--"+getStr(item);
					try {
						LogSvr.logMsg(mesInfo, fileName);
					} catch (IOException e) {
						log.info("IO错误 输出日志失败" + fileName);
					}
					item.setCjl(lastStorage.getCjl());
				}
			}
			lastStorage = item;
			defualtlast = item;
			//开盘当前价用今开替换
			if(iMinute == 570){
				if(item.getJk() == 0){
					String fileName = BaseUtil.getConfigPath("wstock/zjsTrade.log");
					String mesInfo = item.getCode()+",今开为0,"+getStr(item);
					try {
						LogSvr.logMsg(mesInfo, fileName);
					} catch (IOException e) {
					}
				}else{
					item.setC(item.getJk());
				}
			}
			sb.append(getStr(item));
		}catch(Exception e){
			log.error("append2Cache异常"+e);
		}
	}

	public void clear() {
		clearSB();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	//得到昨收价
	private double getZsByThird(String companycode){
		if(slast!= null && slast.getZs() != 0){			
			return slast.getZs();			
		}
		double c = 0.0; // c=15.3;
		Trade0001 trade = DcssTradeIndexServiceClient.getInstance().getLatestTradeData(companycode);
		if(trade != null ) {
			if(trade.getF007n() != 0 && trade.getF007n() != 0){
				c = Double.valueOf(trade.getF007n());
			}else if(trade.getF002n() != 0 && trade.getF002n() != 0){
				c = Double.valueOf(trade.getF002n());
			}
		}else{
			StockTrade st = StockTradeService.getInstance().getStockTradeFromCache(companycode);
			c = st.getC();
		}
		return c;
	}
	
	
	
	private boolean isHStock(String companycode){
		if(companycode.endsWith(".hk")){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 获取时分数据
	 * @return
	 */
	public String getShareTimerDatas() {
		String companycode = this.getKey().split("\\^")[0];
		//判断是否休市 0休市 1开市		
		long fangwenTime = System.currentTimeMillis();
//		accessRepair(companycode,fangwenTime);
		return getShareTimerDatas(companycode,fangwenTime);
	}
	
	
	/**
	 * 访问补全功能，提供给拉全量和拉单个的,为防止写间隔和读间隔一样，两边都频繁访问，引起冲突，特修改读间隔为5分钟。
	 * @param companycode
	 * @param fangwenTime
	 */
	void accessRepair(String companycode, long fangwenTime){
		int fwMinute = DateUtil.longtimeToMinuteofday(fangwenTime); 
		int sharetimer_log_switch = ConfigCenterFactory.getInt("stock_log.sharetimer_log_switch", 0);
		int interval = ConfigCenterFactory.getInt("stock_zjs.readInterval", 5);			
		int minute_open ;		
		int minute_noon_start;
		int minute_noon_end;
		int minute_close ;		
		if(isHStock(companycode)){
			minute_open = ConfigCenterFactory.getInt("stock_zjs.hk_minute_open", 570);			
			minute_noon_start = ConfigCenterFactory.getInt("stock_zjs.hk_minute_noon_start", 719);
			minute_noon_end = ConfigCenterFactory.getInt("stock_zjs.hk_minute_noon_end", 780);
			minute_close = ConfigCenterFactory.getInt("stock_zjs.hk_minute_open", 960);			
		}else{
			minute_open = ConfigCenterFactory.getInt("stock_zjs.a_minute_open", 570);			
			minute_noon_start = ConfigCenterFactory.getInt("stock_zjs.a_minute_noon_start", 689);
			minute_noon_end = ConfigCenterFactory.getInt("stock_zjs.a_minute_noon_end", 780);
			minute_close = ConfigCenterFactory.getInt("stock_zjs.a_minute_close", 900);
		}	
		boolean checkNeedBq = false;//检查是否需要补全
		if(lastStorage == null ){
			//如果是休市日期,如果原始数据空缺，需要补全
			//String dateStr = DateUtil.getDayStartTime(fangwenTime).toString();
			String dateStr = String.valueOf(DateUtil.getDayStartTime(fangwenTime).getTime());
			if(this.key.contains(dateStr) == false){
				checkNeedBq = true;
				fwMinute = minute_close + interval;
			}
			if(fwMinute - minute_open > interval){
				checkNeedBq = true;
			}			
		}else{	
			//已经有数据后，不再进行补全，减少读冲突
			int accessRepairNoNull = ConfigCenterFactory.getInt("stock_zjs.accessRepairNoNull", 0);	
			if(accessRepairNoNull == 1){
				long ltime = lastStorage.getUptime();
				int	lMinute = DateUtil.longtimeToMinuteofday(ltime);			
				if(minute_close == lMinute){
					//如果最后一个点已经存在，则不需要补全
				}else{
					if(fwMinute - lMinute > interval){
						checkNeedBq = true;//确定需要补全
					}else{
						//如果日期不一致，也是需要补全的
						int lDay = DateUtil.longtimeToDayofyear(ltime);
						int fwDay = DateUtil.longtimeToDayofyear(fangwenTime);
						if(lDay != fwDay){
							checkNeedBq = true;
							fwMinute = minute_close + interval;
						}
					}
				}
			}
		}
		if(fwMinute > (minute_close+interval)){
			if(checkNeedBq == true){				
				fwMinute = minute_close + interval;
			}
		}		
	
		if(checkNeedBq){
			if(sharetimer_log_switch == 1){
				log.info(companycode+"getShareTimerDatas补全");
			}
			synchronized (lastPersitanceTime) {
				int tmpMinute;
				StockTrade tmpStockTrade = null ;
				Calendar c = Calendar.getInstance();
				try{
					long keyTime = Long.valueOf(this.getKey().split("\\^")[1]);
					c.setTimeInMillis(keyTime);
				}catch (Exception e) {			
				}
				c.set(Calendar.SECOND, 59);
				if(lastStorage == null  ){//没有一个数据时，需要补全2个点	
					//补全9点30的点
					tmpMinute = minute_open ;								
					int h = tmpMinute / 60;
					int m = tmpMinute % 60;
					c.set(Calendar.HOUR_OF_DAY, h);
					c.set(Calendar.MINUTE, m);
					long tmpStime = c.getTimeInMillis();							
					double cprice = getZsByThird(companycode);
					if(cprice > 0){
						tmpStockTrade = new StockTrade();	
						tmpStockTrade.setC(cprice);
						tmpStockTrade.setZs(cprice);
						tmpStockTrade.setJk(cprice);
						tmpStockTrade.setUptime(tmpStime);
						append2Cache(tmpStockTrade);
						
						tmpMinute = fwMinute - 1;//把上一分钟固定下来，用的数据是最新的数据
						if(slast != null){
							tmpStockTrade = slast;
						}
						if(minute_noon_start<tmpMinute&&tmpMinute<minute_noon_end ){
							tmpMinute = minute_noon_start;
						}
						else if(minute_close <= tmpMinute){
							tmpMinute = minute_close;							
						}
						h = tmpMinute / 60;
						m = tmpMinute % 60;
						c.set(Calendar.HOUR_OF_DAY, h);
						c.set(Calendar.MINUTE, m);
						tmpStime = c.getTimeInMillis();	
						tmpStockTrade.setUptime(tmpStime);
						append2Cache(tmpStockTrade);
						if(slast == null || slast.getUptime() < tmpStockTrade.getUptime() ){
							slast = tmpStockTrade;
						}
						lastPersitanceTime = System.currentTimeMillis();
//						ShareTimerTradeService.getInstance().put2Mapdb(key, this);		
					}else{
						log.error("getZsByThird获取的股价为0"+key);
					}		
				}else{	
					tmpStockTrade = lastStorage;
					tmpMinute = fwMinute - 1;//把上一分钟固定下来，用的数据是最新的数据
					if(slast != null){
						tmpStockTrade = slast;
					}
					if(minute_noon_start<tmpMinute&&tmpMinute<minute_noon_end ){
						tmpMinute = minute_noon_start;
					}else if(minute_close <= tmpMinute){
						tmpMinute = minute_close;						
					}
					
					int h = tmpMinute / 60;
					int m = tmpMinute % 60;
					c.set(Calendar.HOUR_OF_DAY, h);
					c.set(Calendar.MINUTE, m);
					long tmpStime = c.getTimeInMillis();	
					tmpStockTrade.setUptime(tmpStime);
					append2Cache(tmpStockTrade);
					
					//可能存在临时补的点，比slast时间更晚  tmpStockTrade其实是lastStorage数据
					if(slast == null || slast.getUptime() < tmpStime){
						slast.setUptime(tmpStime);
					}					
					lastPersitanceTime = System.currentTimeMillis();

				}
				
			}
		}
	}
	
	String getShareTimerDatas(String companycode, long fangwenTime) {
		int fwMinute = DateUtil.longtimeToMinuteofday(fangwenTime); 
		int minute_open ;
		int minute_close ;	
		if(isHStock(companycode)){
			minute_open = ConfigCenterFactory.getInt("stock_zjs.hk_minute_open", 570);
			minute_close = ConfigCenterFactory.getInt("stock_zjs.hk_minute_open", 960);	
		}else{
			minute_open = ConfigCenterFactory.getInt("stock_zjs.a_minute_open", 570);
			minute_close = ConfigCenterFactory.getInt("stock_zjs.a_minute_close", 900);
		}	
		if(StringUtil.isEmpty(sb.toString())){
			long keyTime = 0;
			try{
				keyTime = Long.valueOf(this.getKey().split("\\^")[1]);
				
			}catch (Exception e) {			
			}
			if(DateUtil.longtimeToDayofyear(keyTime) < DateUtil.longtimeToDayofyear(fangwenTime)){
				fwMinute = minute_close;	//下一天的
			}
			//兼容好9:30-9:31的点 以及
			if(slast != null){
				if(minute_open-15 <= fwMinute && fwMinute <= minute_open+1 ){					
					slast.setC(slast.getJk());
					return getStr(slast);	
				}				
			}
			
				
			
			//次功能
			Calendar c = Calendar.getInstance();			
			c.setTimeInMillis(keyTime);
			try{
				keyTime = Long.valueOf(this.getKey().split("\\^")[1]);
				c.setTimeInMillis(keyTime);
			}catch (Exception e) {			
			}
			c.set(Calendar.SECOND, 59);
			int tmpMinute = minute_open ;								
			int h = tmpMinute / 60;
			int m = tmpMinute % 60;
			c.set(Calendar.HOUR_OF_DAY, h);
			c.set(Calendar.MINUTE, m);									
			double cprice = getZsByThird(companycode);
			if(cprice > 0){	
				StringBuffer sbuf = new StringBuffer();
				StockTrade tmpStockTrade = new StockTrade();	
				tmpStockTrade.setC(cprice);
				tmpStockTrade.setZs(cprice);
				tmpStockTrade.setJk(cprice);
				tmpStockTrade.setUptime(c.getTimeInMillis());
				
				sbuf.append(getStr(tmpStockTrade));
				if(this.slast == null ){
					this.slast = tmpStockTrade;
				}
				
				tmpMinute = fwMinute -1 ;				
//				
//				if(minute_close <= tmpMinute){
//					tmpMinute = minute_close;							
//				}
//				h = tmpMinute / 60;
//				m = tmpMinute % 60;
//				c.set(Calendar.HOUR_OF_DAY, h);
//				c.set(Calendar.MINUTE, m);
//				tmpStockTrade.setUptime(c.getTimeInMillis());
//				sbuf.append(getStr(tmpStockTrade));
				int interval = ConfigCenterFactory.getInt("stock_zjs.readInterval", 5);
				if(tmpMinute - minute_open >= interval){
					sb = sbuf;
				}
				return sbuf.toString();
			}
			return sb.toString();
		}else{
			return sb.toString();	
		}        		
	}
	
	
	public String getSB(){
		return sb.toString();
	}
	
	public void clearSB(){
		synchronized (lastPersitanceTime) {			
			slast=null;
			lastStorage=null;
			defualtlast=null;
			sb = sb.delete(0,sb.length());
			hhmmSet.clear();
		}
	}
	
	public StockTrade getNewPoint(){
		return slast;
	}
	
	public String getLastData(){		
		long fangwenTime = System.currentTimeMillis();
		getLastData(fangwenTime);
		return getLastData(fangwenTime);
	}
	
	String getLastData(long fangwenTime){
		String data = "";
		String companycode = this.getKey().split("\\^")[0];
		int fwMinute = DateUtil.longtimeToMinuteofday(fangwenTime); 
		int minute_open ;
		if(isHStock(companycode)){
			minute_open = ConfigCenterFactory.getInt("stock_zjs.hk_minute_open", 570);
		}else{
			minute_open = ConfigCenterFactory.getInt("stock_zjs.a_minute_open", 570);
		}
//		accessRepair(companycode,fangwenTime);
		if(StringUtil.isEmpty(sb.toString())){
			//兼容好9:30-9:31的点
			if(slast != null){
				if(minute_open == fwMinute || minute_open+1 == fwMinute ){					
					slast.setC(slast.getJk());
					return getStr(slast);	
				}else{					
					return sb.toString();
				}				
			}else{
				
				return sb.toString();	
			}
		}else{
			data = getStr(lastStorage);
		}		
		return data;
	}
}
