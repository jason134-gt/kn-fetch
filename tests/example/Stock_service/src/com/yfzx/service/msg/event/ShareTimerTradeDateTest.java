package com.yfzx.service.msg.event;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.stock.common.model.trade.StockTrade;
import com.stock.common.util.DateUtil;
import com.stock.common.util.LogSvr;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.mapdb.StockMapdb;
import com.yfzx.service.trade.ShareTimerTradeService;
import com.yz.mycore.core.util.BaseUtil;

public class ShareTimerTradeDateTest {
	
	HashMap<String,ShareTimerTradeDataWapper> map = new HashMap<String,ShareTimerTradeDataWapper>();
	/**
	 * TODO 测试只读的话，修改ShareTimerTradeDataWapper.getZs 设置一个价格【因为要从DCSS获取昨收价】
	 * @param companycode
	 */
	public void mock(String companycode){
		
		ShareTimerTradeDataWapper tt = ShareTimerTradeService
				.getInstance().getShareTimerTradeDataWapper(
						companycode);
		map.put(companycode, tt);
		StockMapdb.getInstance().initMapdb();
//		//启动1个写进程 10个读进程
//		{
//			Calendar cc = Calendar.getInstance();
//			cc.setTime(new Date());
//			cc.set(Calendar.HOUR_OF_DAY, 9);
//			cc.set(Calendar.MINUTE, 00);
//			cc.set(Calendar.SECOND, 00);
//			long fangwenTime = cc.getTime().getTime();
//			ShareTimerTradeDataWapper tt = ShareTimerTradeService
//					.getInstance().getShareTimerTradeDataWapper(
//							companycode);
//			String str = companycode+"读进程="+tt.getShareTimerDatas(companycode,fangwenTime);
//			
////			cc.set(Calendar.HOUR_OF_DAY, 18);
////			fangwenTime = cc.getTime().getTime();			
////			str =  tt.getShareTimerDatas(companycode,fangwenTime);
////			str = toNewString(str);
////			String fileName = BaseUtil.getConfigPath("wstock/shareTimer.log");	
////			try {
////				LogSvr.logMsg(str, fileName);
////			} catch (IOException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
//		}
		
////		
//		模拟put 3秒一次 单线程
		Runnable run = new Runnable() {
			private String companycode ;
			@Override
			public void run() {	
				ShareTimerTradeDataWapper tt = map.get(companycode);
				System.out.println(tt.hashCode());
				Calendar cc = Calendar.getInstance();
				cc.setTime(new Date());
				cc.set(Calendar.HOUR_OF_DAY, 9);
				cc.set(Calendar.MINUTE, 29);
				cc.set(Calendar.SECOND, 57);
				float cjl = 1000f;
				float cje = 139900f;
				float zs = 13.92f;
				float jk = 13.99f;
				float h = 16.00f;
				float l = 13.44f;
				float c = 13.99f; 
				int forSize = 6881;
				for(int i=0;i<=forSize;i++)	{
					cc.add(Calendar.SECOND, 3);
					//模拟中午休市
					if( (2400 < i && i< 4200) || i>6800 ){
						continue;
					}
					
					long uptime = cc.getTime().getTime();
					float random = (float)Math.random();
					c = l + (h-l)*random ;
					cjl = cjl + 1000f*random ;
					cje = cje + 139900f*random ;
					StockTrade item = new StockTrade(l, h, c, zs, jk, cjl, cje);
					item.setCode(companycode);
					item.setUptime(uptime);
//					if(2<i && i<500){
//						continue;
//					}
//					//模拟中断网络
//					if( 1800 < i && i < 2400 ){
//						continue;
//					}
					
					StockTrade itemClone;
					try {
						itemClone = item.clone();
						tt.put(itemClone);
						if(i==0)System.out.println("开始写");
					} catch (CloneNotSupportedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
//					if(i%forSize == 6580){
//						String ret = tt.getShareTimerDatas(companycode,uptime);	
//						String str = toNewString(ret);
//						String fileName = BaseUtil.getConfigPath("wstock/shareTimer.log");						
//						try {
//							//输出日志
//							LogSvr.logMsg(str, fileName);
//						} catch (IOException e) {
//						
//						}
//					}
				}	
				System.out.println("写完");
				String ret = tt.getShareTimerDatas(companycode,System.currentTimeMillis());	
				String str = toNewString(ret);
				String fileName = BaseUtil.getConfigPath("wstock/shareTimer.log");						
				try {
					//输出日志
					LogSvr.logMsg(str, fileName);
				} catch (IOException e) {
				
				}
			}
			public Runnable init(String companycode) {
				this.companycode = companycode;
				return this;
			}
		}.init(companycode);	
		Thread thread = new Thread(run);
		thread.setName(companycode+"写进程");
		thread.start();
		
		//模拟读 10个线程 1秒一次
		for(int i=0;i<10;i++){
			Runnable runRead = new Runnable() {
				private String companycode ;
				private int intV ;
				@Override
				public void run() {
					ShareTimerTradeDataWapper tt = map.get(companycode);
					System.out.println(tt.hashCode());					
					long starttime = System.currentTimeMillis();
					Calendar cc = Calendar.getInstance();
					cc.setTime(new Date());
					cc.set(Calendar.HOUR_OF_DAY, 9);
					cc.set(Calendar.MINUTE, 25);
					cc.set(Calendar.SECOND, 59);
//							cc.add(Calendar.DAY_OF_YEAR, 1);
					long fangwenTime = cc.getTime().getTime();
					for(int i=0;i<=8100;i++)	{
						fangwenTime = fangwenTime+3000;
						tt.accessRepair(companycode, fangwenTime);
						String str = tt.getShareTimerDatas(companycode,fangwenTime);
//						String str = tt.getLastData(fangwenTime);
						int count = str.length();
//						tt.getRealTradeDate();
					}
					System.out.println(System.currentTimeMillis() - starttime);
					String ret = tt.getShareTimerDatas();	
					String str = toNewString(ret);
					String fileName = BaseUtil.getConfigPath("wstock/shareTimer"+intV+".log");						
					try {
						//输出日志
						LogSvr.logMsg(str, fileName);
					} catch (IOException e) {
					
					}
				}
				public Runnable init(String companycode,int i) {
					this.companycode = companycode;
					this.intV = i;
					return this;
				}
			}.init(companycode,i);	
			Thread threadRead = new Thread(runRead);
			threadRead.setName(companycode+"读取第"+i+"进程");
			threadRead.start();	
		}
	}
	
	private String toNewString(String ret){
		StringBuffer sbuf = new StringBuffer();
		if(StringUtil.isEmpty(ret) ==false){				
			String[] rowDataStrArr = ret.split("~");				
			for(String rowDataStr : rowDataStrArr){
				String[] ceilDataStrArr = rowDataStr.split("\\^");
				if(ceilDataStrArr.length < 3){
					continue;
				}
				String timeStr = ceilDataStrArr[2];
				String dateStr = DateUtil.format2String(new Date(Long.valueOf(timeStr)));
				double c = Double.valueOf(ceilDataStrArr[0]);
				String cjl = new BigDecimal(ceilDataStrArr[1]).stripTrailingZeros().toPlainString();
				double zdf = 0d;							
				String zdfStr = new DecimalFormat("#0.0000").format(zdf);
				sbuf.append(dateStr).append(",").append(c).append(",").append(cjl).append(",").append(",").append(zdfStr).append("\r\n");
			}
			if(sbuf.length() >1){
				sbuf.deleteCharAt(sbuf.length()-1);
			}							
		}
		return sbuf.toString();
	}
	
	public static void main(String[] args) {
		new ShareTimerTradeDateTest().mockHK();
//		StringBuffer sb = new StringBuffer();
//		sb.append("ssddfd~sdfdsfds~ddddddd~wwwwww~");
//		int indexOf = sb.deleteCharAt(sb.length()-1).lastIndexOf("~");
//		sb.delete(indexOf+1,sb.length()).append("11111~");
//		System.out.println(sb.toString());
		
//		new ShareTimerTradeDateTest().mock("000002.sz");
		
//		ShareTimerTradeDateTest.longtimeToMinuteofday(System.currentTimeMillis());
//		long time = System.currentTimeMillis();
//		for(int i=0;i<10000000;i++){			
//			int offset = TimeZone.getDefault().getRawOffset();
//			int minuteOfDay = (int)((time+offset)%86400000/60000);
//		}
//		System.out.println(System.currentTimeMillis() - time);
//		time = System.currentTimeMillis();
//		for(int i=0;i<10000000;i++){		
//			Calendar cc = Calendar.getInstance();
//			cc.setTimeInMillis(time);
//			int h = cc.get(Calendar.HOUR_OF_DAY);
//			int m =cc.get(Calendar.MINUTE);
//			int minuteOfDay = h*60+m;
//		}
//		System.out.println(System.currentTimeMillis() - time);'
//		Calendar c = Calendar.getInstance();
//		c.set(Calendar.HOUR_OF_DAY, 9);
//		c.set(Calendar.MINUTE, 30);
//		c.set(Calendar.SECOND, 59);
//		System.out.println(c.getTimeInMillis());;
//		c.setTimeInMillis(System.currentTimeMillis());
//		c.set(Calendar.HOUR_OF_DAY, 9);
//		c.set(Calendar.MINUTE, 30);
//		c.set(Calendar.SECOND, 59);
//		System.out.println(c.getTimeInMillis());;
	}
	
	public void mockHK(){
		String companycode = "00610.hk";
		ShareTimerTradeDataWapper tt = ShareTimerTradeService
				.getInstance().getShareTimerTradeDataWapper(
						companycode);
		map.put(companycode, tt);
		StockMapdb.getInstance().initMapdb();
		String dataAll = "2.63^0.0^1427765462000^2.62^2.62^2.62^0.0^2015-03-31 09:31:02~"
				+"2.63^0.0^1427765856000^2.62^2.62^2.62^0.0^2015-03-31 09:37:36~"
				+"2.63^0.0^1427766200000^2.62^2.62^2.62^0.0^2015-03-31 09:43:20~"
				+"2.63^12000.0^1427768551000^2.62^2.63^2.63^31560.0^2015-03-31 10:22:31~"
				+"2.63^12000.0^1427768922000^2.62^2.63^2.63^31560.0^2015-03-31 10:28:42~"
				+"2.63^14000.0^1427769251000^2.62^2.63^2.63^36820.0^2015-03-31 10:34:11~"
				+"2.63^14000.0^1427771010000^2.62^2.63^2.63^36820.0^2015-03-31 11:03:30~"
				+"2.64^38000.0^1427771747000^2.62^2.64^2.63^100180.0^2015-03-31 11:15:47~"
				+"2.64^38000.0^1427771807000^2.62^2.64^2.63^100180.0^2015-03-31 11:16:47~"
				+"2.65^234000.0^1427771987000^2.62^2.65^2.63^619580.0^2015-03-31 11:19:47~"
				+"2.65^234000.0^1427772056000^2.62^2.65^2.63^619580.0^2015-03-31 11:20:56~"
				+"2.65^234000.0^1427772090000^2.62^2.65^2.63^619580.0^2015-03-31 11:21:30~"
				+"2.65^254000.0^1427772252000^2.62^2.65^2.63^672580.0^2015-03-31 11:24:12~"
				+"2.65^266000.0^1427773237000^2.62^2.65^2.63^704380.0^2015-03-31 11:40:37~"
				+"2.65^266000.0^1427773200000^2.62^2.65^2.63^704380.0^2015-03-31 11:40:00~"
				+"2.65^266000.0^1427773200000^2.62^2.65^2.63^704380.0^2015-03-31 11:40:00~"
				+"2.63^286000.0^1427779720000^2.62^2.65^2.63^757040.0^2015-03-31 13:28:40~"
				+"2.64^316000.0^1427781000000^2.62^2.65^2.63^836240.0^2015-03-31 13:50:00~"
				+"2.64^316000.0^1427781158000^2.62^2.65^2.63^836240.0^2015-03-31 13:52:38~"
				+"2.64^316000.0^1427782357000^2.62^2.65^2.63^836240.0^2015-03-31 14:12:37~"
				+"2.64^316000.0^1427782487000^2.62^2.65^2.63^836240.0^2015-03-31 14:14:47~"
				+"2.64^316000.0^1427782714000^2.62^2.65^2.63^836240.0^2015-03-31 14:18:34~"
				+"2.62^416000.0^1427783526000^2.62^2.65^2.62^1098240.0^2015-03-31 14:32:06~"
				+"2.62^440000.0^1427786378000^2.62^2.65^2.62^1161120.0^2015-03-31 15:19:38~"
				+"2.62^440000.0^1427787413000^2.62^2.65^2.62^1161120.0^2015-03-31 15:36:53~"
				+"2.63^440000.0^1427788785000^2.62^2.65^2.62^1161120.0^2015-03-31 15:59:45~";
		
		String[] dataArr = dataAll.split("~");
		for(String dataRow : dataArr){
			if(StringUtil.isEmpty(dataRow) ==false){
				String[] dataCeilArr = dataRow.split("\\^");				
				StockTrade st = new StockTrade();
				st.setC(Double.valueOf(dataCeilArr[0]));
				st.setCjl(Double.valueOf(dataCeilArr[1]));
				st.setUptime(Long.valueOf(dataCeilArr[2]));
				st.setZs(Double.valueOf(dataCeilArr[3]));
				st.setH(Double.valueOf(dataCeilArr[4]));
				st.setL(Double.valueOf(dataCeilArr[5]));
				st.setCode(companycode);
				tt.put(st);
			}
		}
		
		String ret = tt.getShareTimerDatas(companycode,System.currentTimeMillis());	
		String str = toNewString(ret);
		String fileName = BaseUtil.getConfigPath("wstock/shareTimer.log");						
		try {
			//输出日志
			LogSvr.logMsg(str, fileName);
		} catch (IOException e) {
		
		}
	}
}
