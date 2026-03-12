package com.yfzx.service.realtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.model.USubject;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.util.DateUtil;
import com.stock.common.util.LogSvr;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.StringUtil;
import com.stock.common.util.WStockUtil;
import com.stock.common.util.ZLibUtil;
import com.yfzx.service.db.USubjectService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.util.BaseUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class WStockService {

	private static WStockService instance=new WStockService();
	Logger log = LoggerFactory.getLogger(WStockService.class);

	public static WStockService getIntance(){
		return instance;
	}

	/**
	 * 启动时加载一次全量
	 */
	public void getStartAllLoad(){		
		try {
			getSHStockAll();
			Thread.sleep(300l);
		} catch (InterruptedException e) {			
			e.printStackTrace();
		}
		try {
			getSZStockAll();
			Thread.sleep(300l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			getHKStockAll();
			Thread.sleep(300l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取上交所行情
	 * @return
	 */
	public void getSHStockRefresh(){
		connectWstock("SH","n");
	}

	/**
	 * 获取深交所行情
	 * @return
	 */
	public void getSZStockRefresh(){
		connectWstock("SZ","n");

	}

	/**
	 * 获取香港行情
	 * @return
	 */
	public void getHKStockRefresh(){
		connectWstock("HK","n");

	}


	/**
	 * 获取上交所行情 全量 所有全量接口 一天只允许30次
	 * @return
	 */
	public void getSHStockAll(){
		connectWstock("SH","i");
	}

	/**
	 * 获取深交所行情 全量 所有全量接口 一天只允许30次
	 * @return
	 */
	public void getSZStockAll(){
		connectWstock("SZ","i");

	}

	/**
	 * 获取香港行情 全量 所有全量接口 一天只允许30次
	 * @return
	 */
	public void getHKStockAll(){
		connectWstock("HK","i");

	}
	
//	public static void main(String[] args) {
//		new WStockService().getSZStockAll();
//		
//	}

	/**
	 * @param type SH SZ HK
	 * @param refreshOrAll n=增量 i=全量 
	 * @return
	 */
	private void connectWstock(String type,String refreshOrAll){
//		int timechart_log_switch = ConfigCenterFactory.getInt("stock_log.timechart_log_switch", 0);
//		long starttime = System.currentTimeMillis();
		String user = ConfigCenterFactory.getString("realtime_server.wstock_user", "tbq01");
		String password = ConfigCenterFactory.getString("realtime_server.wstock_password", "igushuo66");
		String baseUrl = ConfigCenterFactory.getString("realtime_server.wstock_baseUrl", "http://dl.wstock.cn/cgi-bin/wsRTAPI/wsr2.asp");
		String url = baseUrl+"?m="+type+"&u="+user+"&p="+password+"&t="+refreshOrAll;
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet getHttp = new HttpGet(url);
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 3000);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
		try{
			HttpResponse response = client.execute(getHttp);
			HttpEntity entity = response.getEntity();
			byte[] ysByteArr = EntityUtils.toByteArray(entity);
//			if(timechart_log_switch == 1){
//				log.info("获取Wstock["+type+"]数据耗时"+(System.currentTimeMillis()-starttime)+"毫秒,数据包大小"+ysByteArr.length);
//			}			

//			long starttime2 = System.currentTimeMillis();
			try{
				ByteArrayInputStream inputStream = new ByteArrayInputStream(ysByteArr);			
				byte[] bArrRead = new byte[8];
				while (inputStream.read(bArrRead) != -1) { 
					//首先将前4字节（命名为dwZipSize）作为int32解析（4字节整型，或者DWORD）。
					int dwZipSize = (bArrRead[0]&0xFF) + ((bArrRead[1]&0xFF)<<8) + ((bArrRead[2]&0xFF)<<16) + ((bArrRead[3]&0xFF)<<24);
//					int oldSize = (bArrRead[4]&0xFF) + ((bArrRead[5]&0xFF)<<8) + ((bArrRead[6]&0xFF)<<16) + ((bArrRead[7]&0xFF)<<24);					
					if (dwZipSize > 16777216 || dwZipSize <= 0){						
						//如果dwZipSize>16777216（就是256的三次方，16MB）或者dwZipSize<=0，则这个数据包为信息包，将整个包作为作为文本格式（ASCII码）分析，直接解析为文本格式串(char串)。
						String errorMsg = new String(ysByteArr,"gbk");
						if(errorMsg.startsWith("err")){
							log.error("从wstock拉取数据异常，errorMsg="+errorMsg);
						}
						//非error情况是没有数据的时间，如HK=NULL						
					}else{
						int decSize = dwZipSize -4;
						byte[] bbDec = new byte[decSize];
						int n2 = inputStream.read(bbDec);
						if(n2 >0){
							byte[] byteArr = ZLibUtil.decompress(bbDec);
							byteToRow(byteArr);
						}
					}
				}				
			}catch (Exception e) {
				log.error("获取Wstock数据异常"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}finally{
//				if(timechart_log_switch == 1){
//					log.info("处理Wstock["+type+"]耗时"+(System.currentTimeMillis()-starttime2));
//				}
			}

			
		}catch (Exception e) {
			log.error("获取Wstock数据异常"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
		}
		
	}

	/**
	 * 二进制数据转成记录行
	 * @param byteArr
	 */
	private void byteToRow(byte[] byteArr){
		if(byteArr.length < 156){
			//太小的包不处理
			return ;
		}		
		
		int wstock_log_switch = ConfigCenterFactory.getInt("stock_log.wstock_log_switch", 0);		
		String wstock_log_code = ConfigCenterFactory.getString("stock_log.wstock_log_code", "000002.sz,00001.hk");
		int rowByteSize = 156;
		int rowLength = byteArr.length/156;
		List<StockTrade> stList = new ArrayList<StockTrade>(rowLength);
		Long uptime = null;
		String codeTmp = null;
		StringBuffer sbuf = new StringBuffer();
		for(int i =0 ;i<rowLength;i++){
			int index = rowByteSize*i;
			long timeS = (byteArr[index]&0xFF) + ((byteArr[index+1]&0xFF)<<8) + ((byteArr[index+2]&0xFF)<<16) + ((byteArr[index+3]&0xFF)<<24); 			
			String code = WStockUtil.byteToString(Arrays.copyOfRange(byteArr,index+4,index+16));
			String companyCode = code.substring(2)+"."+code.substring(0,2).toLowerCase();
			
			float bs = 1f;//港股是1，A股是100
			if( companyCode.contains(".sz") || companyCode.contains(".sh") ){
				bs = 100f;
			}
			
			USubject usubject = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(companyCode);
			if (usubject == null) {
				//不在USubject中数据不处理
				continue;					
			}

			String name = WStockUtil.byteToString(Arrays.copyOfRange(byteArr,index+16,index+32));

//			float fPrice3 = WStockUtil.toSingle(byteArr,index + 32);      // 沪深股票为成交总笔数；期货是前一交易日结算价；贵金属等为0
//			float fVol2 = WStockUtil.toSingle(byteArr,index + 36);        // 现量，当前最新成交量
			//			float fOI = WStockUtil.toSingle(byteArr,index + 40);  	       // 持仓，仅期货有效
			//			float fPrice2 = WStockUtil.toSingle(byteArr,index + 44);      // 期货的当日结算价，盘中为0，收盘后交易所才提供
			float fLastClose = WStockUtil.toSingle(byteArr,index + 48);   // 前一交易日收盘价
			float fOpen = WStockUtil.toSingle(byteArr,index + 52);        // 今日开盘价
			float fHigh = WStockUtil.toSingle(byteArr,index + 56);        // 今日最高价
			float fLow = WStockUtil.toSingle(byteArr,index + 60);         // 今日最低价
			float fNewPrice = WStockUtil.toSingle(byteArr,index + 64);    // 今日当前最新价，收盘后就是收盘价
			float fVolume = WStockUtil.toSingle(byteArr,index + 68) * bs;      // 今日总手，总成交量,统一单位，新浪的是股数，前端显示也是股数
			float fAmount = WStockUtil.toSingle(byteArr,index + 72);      // 今日总成交金额           
			float fbp1 = WStockUtil.toSingle(byteArr,index + 76);         // 委托买价1
			float fbp2 = WStockUtil.toSingle(byteArr,index + 80);         // 委托买价2
			float fbp3 = WStockUtil.toSingle(byteArr,index + 84);         // 委托买价3
			float fbp4 = WStockUtil.toSingle(byteArr,index + 88);         // 委托买价4
			float fbp5 = WStockUtil.toSingle(byteArr,index + 92);         // 委托买价5
			float fbv1 = WStockUtil.toSingle(byteArr,index + 96);         // 委托买量1
			float fbv2 = WStockUtil.toSingle(byteArr,index + 100);        // 委托买量2
			float fbv3 = WStockUtil.toSingle(byteArr,index + 104);        // 委托买量3
			float fbv4 = WStockUtil.toSingle(byteArr,index + 108);        // 委托买量4
			float fbv5 = WStockUtil.toSingle(byteArr,index + 112);        // 委托买量5
			float fsp1 = WStockUtil.toSingle(byteArr,index + 116);        // 委托卖价1
			float fsp2 = WStockUtil.toSingle(byteArr,index + 120);        // 委托卖价2
			float fsp3 = WStockUtil.toSingle(byteArr,index + 124);        // 委托卖价3
			float fsp4 = WStockUtil.toSingle(byteArr,index + 128);        // 委托卖价4
			float fsp5 = WStockUtil.toSingle(byteArr,index + 132);        // 委托卖价5
			float fsv1 = WStockUtil.toSingle(byteArr,index + 136);        // 委托卖量1
			float fsv2 = WStockUtil.toSingle(byteArr,index + 140);        // 委托卖量2
			float fsv3 = WStockUtil.toSingle(byteArr,index + 144);        // 委托卖量3
			float fsv4 = WStockUtil.toSingle(byteArr,index + 148);        // 委托卖量4
			float fsv5 = WStockUtil.toSingle(byteArr,index + 152);        // 委托卖量5
			//			sbuf.append(code).append(",").append(name).append(",").append(fPrice3).append(",").append(fVol2).append(",").append(fLastClose)
			//			.append(",").append(fOpen).append(",").append(fHigh).append(",").append(fLow).append(",").append(fNewPrice).append(",")
			//			.append(fVolume).append(",").append(fAmount).append(",").append(time).append("000").append("\r\n");
			//SZ000001,平安银行,137538.0,27929.0,14.53,14.21,15.5,14.0,15.23,3928343.0,5.8836644E9,1418020702000
			// 代码         ,名称          ,        ,最近成交,昨收  ,今开   ,今高 ,今低,当前价,   成交量 ,     成交额, 时间戳
			//    		System.out.println(time+","+code+","+name+","+f1+","+f2+","+f3+","+f4+","+f5+","+f6+","+f7+","+f8);
			
			//没有成交量的
//			if(fVolume < 0.01f){
//				fVolume = 0.01f;
//			}
//			if(fAmount < 0.01f){
//				fAmount = 0.01f;
//			}
			StockTrade stocktrade = new StockTrade(fLow,fHigh,fNewPrice,fLastClose,fOpen,fVolume,fAmount);
			stocktrade.setCode(companyCode);					
			stocktrade.setName(name);
			long time = timeS*1000l;
			stocktrade.setUptime(time);	
			stocktrade.setFbp1(fbp1);
			stocktrade.setFbp2(fbp2);
			stocktrade.setFbp3(fbp3);
			stocktrade.setFbp4(fbp4);
			stocktrade.setFbp5(fbp5);
			stocktrade.setFbv1(fbv1);
			stocktrade.setFbv2(fbv2);
			stocktrade.setFbv3(fbv3);
			stocktrade.setFbv4(fbv4);
			stocktrade.setFbv5(fbv5);
			stocktrade.setFsp1(fsp1);
			stocktrade.setFsp2(fsp2);
			stocktrade.setFsp3(fsp3);
			stocktrade.setFsp4(fsp4);
			stocktrade.setFsp5(fsp5);
			stocktrade.setFsv1(fsv1);
			stocktrade.setFsv2(fsv2);
			stocktrade.setFsv3(fsv3);
			stocktrade.setFsv4(fsv4);
			stocktrade.setFsv5(fsv5);
			sbuf.append(companyCode).append(",");
			if(uptime == null){
				uptime = time;
			}	
			if(codeTmp == null){
				codeTmp = companyCode;
			}
			if(wstock_log_switch == 1){				
				if(wstock_log_code.contains(companyCode)){
					//先打印一下,检查实时性
					String msg = "wstock行情"+DateUtil.format2String(new Date(stocktrade.getUptime()))+","+companyCode+",c="+stocktrade.getC()
							+"fVolume="+fVolume+",fVolume="+fAmount+"__"+new BigDecimal(stocktrade.getCjl()).stripTrailingZeros().toPlainString() +" 子压缩包uptime"+DateUtil.format2String(new Date(uptime));
					if(SLogFactory.isopen("fetch_wstock_isopen"))
						log.info(msg);					
					String fileName = BaseUtil.getConfigPath("wstock/realTrade.log");
					try {
						//输出日志
						LogSvr.logMsg(msg, fileName);
					} catch (IOException e) {
						log.info("IO错误 输出日志失败" + fileName);
					}
				}
			}
					
			RealTradeService.getInstance().exeStockTradeRefresh(stocktrade);
			stList.add(stocktrade);
			//修改成批量发送
//			RealTradeService.getInstance().sendThisGroupRealData(stocktrade.getCode(),stocktrade.getUptime());
		}
		
		//批量发送行情
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
				RealTradeService.getInstance().sendThisGroupRealData(seed, ret, uptime);
			}	
		}
		
		//把原始数据存储到缓存
		int ByteArr = ConfigCenterFactory.getInt("wstock.ByteArr", 0);
		if(ByteArr == 1){
			//部分数据是如HK60830的数据
			try{
				int tSave =0;
				if(codeTmp !=null  ){
					if(codeTmp.contains(".hk")){
						tSave = ConfigCenterFactory.getInt("wstock.hkSaveByte", 0);
					}else{
						tSave = ConfigCenterFactory.getInt("wstock.aSaveByte", 1);
					}
				}				
				if(tSave == 1){
					String dateF =  DateUtil.getSysDate(DateUtil.YYYYMMDD);
					List<byte[]> list = LCEnter.getInstance().get("stl.wstock."+dateF, SCache.CACHE_NAME_wstockcache);
					if(list == null){
						list = new ArrayList<byte[]>();
						LCEnter.getInstance().put("stl.wstock."+dateF, list, SCache.CACHE_NAME_wstockcache);
					}
					list.add(byteArr);
				}				
			}catch (Exception e) {
				log.error("原始数据存储到缓存异常"+uptime+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}
		}
	}	
	
	/**
	 * 二进制数据转成记录行
	 * @param byteArr
	 */
	public void byteToRow_mock(byte[] byteArr,String scompanycode){
		if(byteArr.length < 156){
			//太小的包不处理
			return ;
		}		
		
		int rowByteSize = 156;
		int rowLength = byteArr.length/156;
		Long uptime = null;
		StringBuffer sbuf = new StringBuffer();
		for(int i =0 ;i<rowLength;i++){
			int index = rowByteSize*i;
			long timeS = (byteArr[index]&0xFF) + ((byteArr[index+1]&0xFF)<<8) + ((byteArr[index+2]&0xFF)<<16) + ((byteArr[index+3]&0xFF)<<24); 			
			String code = WStockUtil.byteToString(Arrays.copyOfRange(byteArr,index+4,index+16));
			String companyCode = code.substring(2)+"."+code.substring(0,2).toLowerCase();
			
			float bs = 1f;//港股是1，A股是100
			if( companyCode.contains(".sz") || companyCode.contains(".sh") ){
				bs = 100f;
			}
			
			USubject usubject = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(companyCode);
			if (usubject == null) {
				//不在USubject中数据不处理
				continue;					
			}
			//非指定公司，则返回
			if(!StringUtil.isEmpty(scompanycode)&&!scompanycode.contains(companyCode))
			{
				continue;
			}
			String name = WStockUtil.byteToString(Arrays.copyOfRange(byteArr,index+16,index+32));

//			float fPrice3 = WStockUtil.toSingle(byteArr,index + 32);      // 沪深股票为成交总笔数；期货是前一交易日结算价；贵金属等为0
//			float fVol2 = WStockUtil.toSingle(byteArr,index + 36);        // 现量，当前最新成交量
			//			float fOI = WStockUtil.toSingle(byteArr,index + 40);  	       // 持仓，仅期货有效
			//			float fPrice2 = WStockUtil.toSingle(byteArr,index + 44);      // 期货的当日结算价，盘中为0，收盘后交易所才提供
			float fLastClose = WStockUtil.toSingle(byteArr,index + 48);   // 前一交易日收盘价
			float fOpen = WStockUtil.toSingle(byteArr,index + 52);        // 今日开盘价
			float fHigh = WStockUtil.toSingle(byteArr,index + 56);        // 今日最高价
			float fLow = WStockUtil.toSingle(byteArr,index + 60);         // 今日最低价
			float fNewPrice = WStockUtil.toSingle(byteArr,index + 64);    // 今日当前最新价，收盘后就是收盘价
			float fVolume = WStockUtil.toSingle(byteArr,index + 68) * bs;      // 今日总手，总成交量,统一单位，新浪的是股数，前端显示也是股数
			float fAmount = WStockUtil.toSingle(byteArr,index + 72);      // 今日总成交金额           
			float fbp1 = WStockUtil.toSingle(byteArr,index + 76);         // 委托买价1
			float fbp2 = WStockUtil.toSingle(byteArr,index + 80);         // 委托买价2
			float fbp3 = WStockUtil.toSingle(byteArr,index + 84);         // 委托买价3
			float fbp4 = WStockUtil.toSingle(byteArr,index + 88);         // 委托买价4
			float fbp5 = WStockUtil.toSingle(byteArr,index + 92);         // 委托买价5
			float fbv1 = WStockUtil.toSingle(byteArr,index + 96);         // 委托买量1
			float fbv2 = WStockUtil.toSingle(byteArr,index + 100);        // 委托买量2
			float fbv3 = WStockUtil.toSingle(byteArr,index + 104);        // 委托买量3
			float fbv4 = WStockUtil.toSingle(byteArr,index + 108);        // 委托买量4
			float fbv5 = WStockUtil.toSingle(byteArr,index + 112);        // 委托买量5
			float fsp1 = WStockUtil.toSingle(byteArr,index + 116);        // 委托卖价1
			float fsp2 = WStockUtil.toSingle(byteArr,index + 120);        // 委托卖价2
			float fsp3 = WStockUtil.toSingle(byteArr,index + 124);        // 委托卖价3
			float fsp4 = WStockUtil.toSingle(byteArr,index + 128);        // 委托卖价4
			float fsp5 = WStockUtil.toSingle(byteArr,index + 132);        // 委托卖价5
			float fsv1 = WStockUtil.toSingle(byteArr,index + 136);        // 委托卖量1
			float fsv2 = WStockUtil.toSingle(byteArr,index + 140);        // 委托卖量2
			float fsv3 = WStockUtil.toSingle(byteArr,index + 144);        // 委托卖量3
			float fsv4 = WStockUtil.toSingle(byteArr,index + 148);        // 委托卖量4
			float fsv5 = WStockUtil.toSingle(byteArr,index + 152);        // 委托卖量5
			//			sbuf.append(code).append(",").append(name).append(",").append(fPrice3).append(",").append(fVol2).append(",").append(fLastClose)
			//			.append(",").append(fOpen).append(",").append(fHigh).append(",").append(fLow).append(",").append(fNewPrice).append(",")
			//			.append(fVolume).append(",").append(fAmount).append(",").append(time).append("000").append("\r\n");
			//SZ000001,平安银行,137538.0,27929.0,14.53,14.21,15.5,14.0,15.23,3928343.0,5.8836644E9,1418020702000
			// 代码         ,名称          ,        ,最近成交,昨收  ,今开   ,今高 ,今低,当前价,   成交量 ,     成交额, 时间戳
			//    		System.out.println(time+","+code+","+name+","+f1+","+f2+","+f3+","+f4+","+f5+","+f6+","+f7+","+f8);
			
			//没有成交量的
			if(fVolume < 0.01f){
				fVolume = 0.01f;
			}
			if(fAmount < 0.01f){
				fAmount = 0.01f;
			}
			StockTrade stocktrade = new StockTrade(fLow,fHigh,fNewPrice,fLastClose,fOpen,fVolume,fAmount);
			stocktrade.setCode(companyCode);					
			stocktrade.setName(name);
			long time = timeS*1000l;
			stocktrade.setUptime(time);	
			stocktrade.setFbp1(fbp1);
			stocktrade.setFbp2(fbp2);
			stocktrade.setFbp3(fbp3);
			stocktrade.setFbp4(fbp4);
			stocktrade.setFbp5(fbp5);
			stocktrade.setFbv1(fbv1);
			stocktrade.setFbv2(fbv2);
			stocktrade.setFbv3(fbv3);
			stocktrade.setFbv4(fbv4);
			stocktrade.setFbv5(fbv5);
			stocktrade.setFsp1(fsp1);
			stocktrade.setFsp2(fsp2);
			stocktrade.setFsp3(fsp3);
			stocktrade.setFsp4(fsp4);
			stocktrade.setFsp5(fsp5);
			stocktrade.setFsv1(fsv1);
			stocktrade.setFsv2(fsv2);
			stocktrade.setFsv3(fsv3);
			stocktrade.setFsv4(fsv4);
			stocktrade.setFsv5(fsv5);
			sbuf.append(companyCode).append(",");
			if(uptime == null){
				uptime = time;
			}			
					
			RealTradeService.getInstance().exeStockTradeRefresh_mock(stocktrade);
		}
		
	}
}
