package com.yfzx.service.trade;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.util.DateUtil;
import com.stock.common.util.Spider;
import com.stock.common.util.StringUtil;
import com.yz.configcenter.ConfigCenterFactory;

/**
 * 交易中心
 *
 * @author：杨真
 * @date：2014-5-7
 */
public class TradeCenter {
	Logger log = LoggerFactory.getLogger(TradeCenter.class);
	static TradeCenter instance = new TradeCenter();
	private final static Map<Object,Object> _attr = new HashMap<Object,Object>();

//	private final static Map<String,String> mapByCode = new HashMap<String,String>();
	Pattern p = Pattern.compile("[0-9]{4}[-/][0-9]{1,2}[-/][0-9]{1,2}");
	static int a = 0;
	static int hk = 1;
	static String abStock = "ab";//ab股是否在交易时段内
	static String hkStock = "hk";//港股 是否在交易时段内
	static String aIndex = "sh000001";//上证指数
	static String hkIndex = "hkHSI";//港股指数

	//今天是否开过市0:未过市,1:开市
	public static Map<Long,Integer> _isOpenTradeMap = new HashMap<Long,Integer>();
	public TradeCenter() {

	}

	public static TradeCenter getInstance() {
		return instance;
	}
	/**
	 * 0:休市，1：开市
	 */
	public void judgmentATradeClosed() {
//		log.info("检查开市休市任务启动");
		//A股的休市时间判断
//		int isclosed = judgmentTradeClosed("sh000001,sz399001,sh000300,sz399415,sz399006");
		int isclosed = judgmentTradeClosed(aIndex);
		//服务器时间的实际不准，跟真实的可能存在几秒差异,且数据再15:05还有数据  且9.25开始有集合竞价数据
		String astartenttime = ConfigCenterFactory.getString("stock_zjs.judgmentTradeClosed_a_startenttime", "9:24-11:36|12:59-15:06");
		//ab股的交易时段
		String abStockTradeTime = ConfigCenterFactory.getString("stock_zjs.stock_game_ab_trade_time", "9:30-11:30|13:00-15:00");

		int isnormaltime = isNormalTradeTime(astartenttime);
		int isAbNormalTime = isNormalTradeTime(abStockTradeTime);
		if(isclosed==1&&isnormaltime==1)
		{
			_attr.put(a, 1);
//			log.info("检查当前A股是开市");
		}
		else{
			_attr.put(a, 0);
//			log.info("检查当前A股是休市");
		}

		if(isclosed==1&&isAbNormalTime==1)
		{
			_attr.put(abStock, 1);
		}
		else{
			_attr.put(abStock, 0);
		}

//		log.info("检查休市任务完成");
		//港股的休市时间判断--同上，逻辑后面再加
		isclosed = judgmentTradeClosed(hkIndex);
		astartenttime = ConfigCenterFactory.getString("stock_zjs.judgmentTradeClosed_h_startenttime", "9:29-12:06|12:59-16:06");
		//港股的交易时段
		String hkStockradeTime = ConfigCenterFactory.getString("stock_zjs.stock_game_hk_trade_time", "9:30-12:00|13:00-16:00");
		isnormaltime = isNormalTradeTime(astartenttime);
		if(isclosed==1&&isnormaltime==1)
		{
			_attr.put(hk, 1);
//			log.info("检查当前港股是开市");
		}
		else{
			_attr.put(hk, 0);
//			log.info("检查当前港股是休市");
		}
		int isHkNormalTime = isNormalTradeTime(hkStockradeTime);
		if(isclosed==1&&isHkNormalTime==1)
		{
			_attr.put(hkStock, 1);
		}
		else{
			_attr.put(hkStock, 0);
		}

		int state = TradeCenter.getInstance().getTradeStatus(0);
		initTradeIsOpened(state);
	}

	/**
	 * 设置为开市状态，专为数据回测试用
	 * @return
	 */
	public void setAHTradeOpen()
	{
		_attr.put(hk, 1);
		_attr.put(a, 1);
	}
	/**
	 * 是否开市，包括9:25的集合竞价 ，也包括15:00最后集合竞价
	 * 0 = a:A股
	 * 1 = hk:港股
	 * @return 0:休市，1：开市
	 */
	public int getTradeStatus(int type)
	{
		return _attr.get(type) == null ? 0 : (Integer) _attr.get(type);
	}
	/**
	 * 返回是否在交易时段
	 * 0 = a:A股
	 * 1 = hk:港股
	 * @return 0:休市，1：开市
	 */
	public int getStockTradeStatus(int type) {
		if(type == 0) {
			return _attr.get(abStock) == null ? 0 : (Integer)_attr.get(abStock);
		} else if(type == 1) {
			return _attr.get(hkStock) == null ? 0 : (Integer)_attr.get(hkStock);
		}
		return 0;
	}

	/**
	 * 获取A股最近的开盘日期
	 * @return
	 */
	public String getALastDate(){
		return _attr.get(aIndex) == null? null:(String)_attr.get(aIndex);
	}
	/**
	 * 获取港股最近的开盘日期
	 * @return
	 */
	public String getHKLastDate(){
		return _attr.get(hkIndex) == null? null:(String)_attr.get(hkIndex);
	}
	/**
	 * 0:休市时间段，1：开市时间段
	 * @param setime
	 * @return
	 */
	public int isNormalTradeTime(String setime) {
		String[] sea = setime.split("\\|");
		//上午开市时间
		String ams = sea[0].split("-")[0];
		Date amstime = formatTimebyFs(ams);
		//上午休市时间
		String ame = sea[0].split("-")[1];
		Date ametime = formatTimebyFs(ame);
		//下午开市时间
		String pms = sea[1].split("-")[0];
		Date pmstime = formatTimebyFs(pms);
		//下午休市时间
		String pme = sea[1].split("-")[1];
		Date pmetime = formatTimebyFs(pme);
		Date ctime = Calendar.getInstance().getTime();
		if(ctime.compareTo(amstime)>=0&&ctime.compareTo(ametime)<=0||ctime.compareTo(pmstime)>=0&&ctime.compareTo(pmetime)<=0)
			return 1;
		return 0;
	}

	/**
	 * 支持格式：hh:ss
	 * @param ams
	 * @return
	 */
	private Date formatTimebyFs(String ams) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(ams.split(":")[0]));
		c.set(Calendar.MINUTE, Integer.valueOf(ams.split(":")[1]));
		c.clear(Calendar.SECOND);
		return c.getTime();
	}

	/**
	 * 判断当前是否是交易日 【SH是3秒，SZ是6秒；港股刷新频率是1分钟】,不判断交易时段
	 * 0:休市，1：开市
	 */
	public int judgmentTradeClosed(String jcode) {
		int isclosed = 0;
		try {

			//由原来股价变化 转成上证指数变化 确定是否开市或休市 也由原因的一次任务，变更成3次任务 //防止任务失败
			Long time = Calendar.getInstance().getTimeInMillis();
			//http://hq.sinajs.cn/list=sh000001,sz399001,sh000300,sz399415,sz399006
			//上证指数：s_sh000001;恒生指数：hkHSI
			String url = "http://hq.sinajs.cn/?_=" + time + "&list="+jcode;
			String s = Spider.urlSpider(url, "gbk");
			if (!StringUtil.isEmpty(s)) { //var hq_str_s_sh000001="上证指数,2045.733,-7.138,-0.35,809745,6116243";
				String sDateStr = null;
				Matcher m = p.matcher(s);
				//获取出时间串
				while (m.find()) {
					if (StringUtil.isEmpty(m.group()) == false ) {
						sDateStr = m.group().replace("/", "-").trim();
						break;
					}
				}
				Date date = new Date();
				if(DateUtil.getSysDateYYYYMMDD(date).equals(sDateStr)){
					if(jcode.equals(aIndex) ){//上证指数 9点半之前就有数据，而且日期是今天的
						Date amstime = formatTimebyFs("9:29");
						//9点半之后
						if(date.getTime() > amstime.getTime()  ){
							isclosed = 1;
							_attr.put(jcode, sDateStr);
						}
					}else if(jcode.equals(hkIndex)){
						isclosed = 1;
						_attr.put(jcode, sDateStr);
					}
				}else{
					_attr.put(jcode, sDateStr);
				}


//				for (String sci : s.split(";")) {
//					if (!StringUtil.isEmpty(sci) && sci.split("=").length == 2) {
//						String[] sa = sci.split("=");
//						String code = sa[0]; //var hq_str_s_sh000001
//						String value = sa[1];
//						if(mapByCode.containsKey(code)){
//							String oldValue = mapByCode.get(code);
//							if(oldValue.equals(value)){
//								isclosed = 0; //值相对就休市
//							}else{
//								isclosed = 1; //开市
//								mapByCode.put(code, value);
//							}
//						}else{
//							mapByCode.put(code, value);
//						}
//					}
//				}
			}

		} catch (Exception e) {
			//log.error("---------------refresh StockTradeInfo outer data faild!-------------------"+ e);
		}

		return isclosed;
	}
	private static String getcompanycode(String sc) {
		String hzs = ConfigCenterFactory.getString(
				"stock_dc.companycode_hzs", "sh;sz;hk");
		for (String hz : hzs.split(";")) {
			if (sc.indexOf(hz) >= 0)
				return sc.replace(hz, "") + "." + hz;
		}

		return sc;
	}
	public static void main(String[] args) {
//		int isnormaltime = TradeCenter.getInstance().isNormalTradeTime("9:30-11:30|13:00-15:00");

		TradeCenter.getInstance().judgmentATradeClosed();
		TradeCenter.getInstance().judgmentATradeClosed();
	}

	//只对日数据做处理
	public boolean isClearRealData(int type) {
		//非日数据不处理
		/*if(type!=0)
			return false;*/
		Date td = DateUtil.getDayStartTime(new Date());
		Integer tradeIsOpened = _isOpenTradeMap.get(td.getTime());
		if(tradeIsOpened==null||tradeIsOpened==0)
		{
			_isOpenTradeMap.clear();
			_isOpenTradeMap.put(td.getTime(),new Integer(0));
			return true;
		}
		return false;
	}

	public void initTradeIsOpened(int state) {
		Date td = DateUtil.getDayStartTime(new Date());
		Integer tradeIsOpened = _isOpenTradeMap.get(td.getTime());
		if(tradeIsOpened==null)
		{
			_isOpenTradeMap.clear();
			tradeIsOpened=new Integer(0);
			_isOpenTradeMap.put(td.getTime(),tradeIsOpened);

		}
		if(tradeIsOpened!=1)
		{
			if(state==1)
			{
				tradeIsOpened = 1;
				_isOpenTradeMap.put(td.getTime(),tradeIsOpened);
			}
		}

	}

	public void setTradeOpen(Integer status)
	{
		_isOpenTradeMap.put(DateUtil.getDayStartTime(new Date()).getTime(), status);
	}

	//传入时间大于最近一个交易日时间 则返回最近交易日时间
	public long getLastTradeDate(long time,String companycode){
		String LastTradeDate;
		long lastTradeTime = 0l;
		if(companycode.endsWith(".hk")){
			 LastTradeDate = getHKLastDate();
		}else{
			LastTradeDate = getALastDate();
		}
		Date d = DateUtil.format(LastTradeDate, DateUtil.YYYYMMDD);
		if(d!=null){
			 lastTradeTime = DateUtil.format(LastTradeDate, DateUtil.YYYYMMDD).getTime();

		}
		if(lastTradeTime>0 && time>lastTradeTime){
			time = lastTradeTime;
		}
		return time;
	}
}
