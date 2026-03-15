package com.yfzx.service.spider;

import java.io.CharArrayWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.stock.common.model.Company;
import com.yfzx.service.db.CompanyService;
import com.yz.configcenter.ConfigCenterFactory;

/**
 * 腾讯公司的新闻
 */
public class QQCompanyTaskProcessor implements ITaskProcessor {

	static Logger log = LoggerFactory.getLogger(QQCompanyTaskProcessor.class);
	HashMap<String,String> retryUrl = new HashMap<String,String>();
	
	/* (non-Javadoc)
	 * @see com.yfzx.service.spider.ITaskProcessor#process(com.yfzx.service.spider.SpiderConfigBean)
	 */
	@Override
	public void process(SpiderConfigBean spiderconfig) {
//		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
//		String startUrl = spiderconfig.getStartUrl();
//		String cookie = spiderconfig.getCookie();
//		Map<String,String> valueMap = spiderconfig.getValueMap();
//		long time = System.currentTimeMillis();		
		
		List<Company> companyList = CompanyService.getInstance().getCompanyListFromCache();
		//公司太多，将公司分组成N个线程抓取，提高效率
		int threadNum = ConfigCenterFactory.getInt("spider.threadNum", 10);
		int totalSize = companyList.size();
		int oneSize = totalSize /threadNum ;
		for(int i=0;i<threadNum;i++){
			if(i != (threadNum-1) ){
				List<Company> childCompanyList = companyList.subList(i*oneSize, (i+1)*oneSize);				
				TimerTask tt =new TimerTask() {
					QQCompanyTaskProcessor task;
					SpiderConfigBean spiderconfig;
					List<Company> companyList;
					
					public TimerTask set(SpiderConfigBean spiderconfig,List<Company> companyList,QQCompanyTaskProcessor task){
						this.companyList = companyList;
						this.spiderconfig = spiderconfig;
						this.task = task;
						return this;
					}
					public void run() {
						task.processChild(companyList, spiderconfig);
						this.cancel();
					}
				}.set(spiderconfig,childCompanyList,this);
				Timer timer = new Timer("Sprider["+spiderconfig.getKey()+ "_"+i+"]",true);							
				timer.schedule(tt,0);	
			}else{
				List<Company> childCompanyList = companyList.subList(i*oneSize,totalSize);	
				processChild(childCompanyList, spiderconfig);
				//第一次抓取失败的，重新抓取一次
				exeRetry(spiderconfig);
			}			
//			log.error("抓取"+companyCode+"公司的新闻");
//			String num = companyCode.split("\\.")[0];
//			String type = companyCode.split("\\.")[1];
//			//url的拼接比较麻烦，配置化实现先尝试支持几种常见的 ，另外港股和A股是分开的，有各自的URL
//			//使用模版方式进行拼接URL					
//			VelocityContext context = new VelocityContext();
//			context.put("num", num);
//			context.put("type", type);
//			CharArrayWriter writer = new CharArrayWriter(); 
//			Velocity.evaluate(context, writer, "", startUrl);			
//			String url = writer.toString();
//			
//			try{
//				//公司的链接在HTML里的JS脚本中
//				InputStream is = StockSpider.doSpiderByLoginCookie(url, "");
//				String jsonContent = IOUtils.toString(is);
//				jsonContent = jsonContent.substring("var finance_news=".length());
//				JSONArray urlArr = JsonPath.read(jsonContent,"$data.data[*].url");
//				JSONArray datetimeArr = JsonPath.read(jsonContent,"$data.data[*].datetime");
//				for(int i=0;i<urlArr.size();i++){
//					String findUrl = urlArr.get(i).toString();
//					String dateStr = datetimeArr.get(i).toString();
//					//超过1天的内容，不再抓取
//					if(time-StockSpider.getTime(dateStr) > 1000*3600*24){
//						continue;
//					}
//					StockSpider.insertByHtml(findUrl, cookie, valueMap,companyCode);								
//					Thread.sleep(threadSleep);					
//				}
//			}catch (Exception e) {
//				log.error("",e);
//			}
		}
	}
		
		
	public void processChild(List<Company> companyList,SpiderConfigBean spiderconfig) {
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
		String startUrl = spiderconfig.getStartUrl();
		String cookie = spiderconfig.getCookie();
		Map<String,String> valueMap = spiderconfig.getValueMap();
		valueMap.put("s", "腾讯新闻");
		long time = System.currentTimeMillis();
//			List<Company> companyList = CompanyService.getInstance().getCompanyListFromCache();
		for(Company company : companyList){
			String companyCode = company.getCompanyCode();
			log.debug("抓取"+companyCode+"公司的新闻");
			String num = companyCode.split("\\.")[0];
			String type = companyCode.split("\\.")[1];
			//url的拼接比较麻烦，配置化实现先尝试支持几种常见的 ，另外港股和A股是分开的，有各自的URL
			//使用模版方式进行拼接URL					
			VelocityContext context = new VelocityContext();
			context.put("num", num);
			context.put("type", type);
			CharArrayWriter writer = new CharArrayWriter(); 
			Velocity.evaluate(context, writer, "", startUrl);			
			String url = writer.toString()+ new Random(1).nextFloat();
			
			try{
				//公司的链接在HTML里的JS脚本中					
				String jsonContent = StockSpider.doSpiderByLoginCookie(url, "");
				jsonContent = jsonContent.substring(jsonContent.indexOf("var finance_news=")+"var finance_news=".length());
				JSONArray dataArr = null ;
				try{
					dataArr = JsonPath.read(jsonContent,"$data.data[*]");
				}catch (Exception e) {
					log.error("异常"+jsonContent,e.getMessage());
					throw e;
				}
//					JSONArray urlArr = JsonPath.read(jsonContent,"$data.data[*].url");
//					JSONArray datetimeArr = JsonPath.read(jsonContent,"$data.data[*].datetime");
				for(int i=0;i<dataArr.size();i++){
					JSONObject dataObj = (JSONObject)dataArr.get(i);
//						String findUrl = urlArr.get(i).toString();
//						String dateStr = datetimeArr.get(i).toString();
					String findUrl = dataObj.get("url").toString();
					String dateStr = dataObj.get("datetime").toString();
					//超过1天的内容，不再抓取
					if(time-StockSpider.getTime(dateStr) > 1000*3600*24){
						continue;
					}
					StockSpider.insertByHtml(findUrl, cookie, valueMap,companyCode);								
					Thread.sleep(threadSleep);			
					
//					long timeLong = StockSpider.getTime(dateStr);
//					long qtLong = StockSpider.getTime("2014-11-20 00:00:00");
//					long oneYueLong = StockSpider.getTime("2014-10-01 00:00:00");
//					if(timeLong < qtLong && timeLong > oneYueLong){	
//						StockSpider.insertByHtml(findUrl, cookie, valueMap,companyCode,"","B");
//						Thread.sleep(threadSleep);					
//					}

				}
			}catch (Exception e) {
				retryUrl.put(url,companyCode);
				log.error("异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}finally{
				
			}
		}
	}
	
	private void exeRetry(SpiderConfigBean spiderconfig){
		if(retryUrl.isEmpty() == true){
			return;
		}
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);		
		String cookie = spiderconfig.getCookie();
		Map<String,String> valueMap = spiderconfig.getValueMap();
		valueMap.put("s", "腾讯新闻");
		long time = System.currentTimeMillis();
//		Iterator<String> iterator= retryUrl.iterator();
//		while(iterator.hasNext()){
//		String url = iterator.next();
		for(String url :retryUrl.keySet()){
			String companyCode = retryUrl.get(url);
			try{
				//公司的链接在HTML里的JS脚本中					
				String jsonContent = StockSpider.doSpiderByLoginCookie(url, "");
				jsonContent = jsonContent.substring(jsonContent.indexOf("var finance_news=")+"var finance_news=".length());
				JSONArray dataArr = null ;
				try{
					dataArr = JsonPath.read(jsonContent,"$data.data[*]");
				}catch (Exception e) {
					log.error("异常"+jsonContent,e.getMessage());
					throw e;
				}
//					JSONArray urlArr = JsonPath.read(jsonContent,"$data.data[*].url");
//					JSONArray datetimeArr = JsonPath.read(jsonContent,"$data.data[*].datetime");
				for(int i=0;i<dataArr.size();i++){
					JSONObject dataObj = (JSONObject)dataArr.get(i);
//						String findUrl = urlArr.get(i).toString();
//						String dateStr = datetimeArr.get(i).toString();
					String findUrl = dataObj.get("url").toString();
					String dateStr = dataObj.get("datetime").toString();
					//超过1天的内容，不再抓取
					if(time-StockSpider.getTime(dateStr) > 1000*3600*24){
						continue;
					}
					StockSpider.insertByHtml(findUrl, cookie, valueMap,companyCode);								
					Thread.sleep(threadSleep);					
				}
			}catch (Exception e) {				
				log.error("异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}
		}
		retryUrl.clear();
	}

}
