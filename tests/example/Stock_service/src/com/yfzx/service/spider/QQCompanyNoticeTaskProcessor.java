package com.yfzx.service.spider;

import java.io.CharArrayWriter;
import java.util.HashMap;
import java.util.Iterator;
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
 * 腾讯公司公告 任务处理
 */
public class QQCompanyNoticeTaskProcessor implements ITaskProcessor {

	static Logger log = LoggerFactory.getLogger(QQCompanyTaskProcessor.class);
	HashMap<String,String> retryUrl = new HashMap<String,String>();
	
	/* (non-Javadoc)
	 * @see com.yfzx.service.spider.ITaskProcessor#process(com.yfzx.service.spider.SpiderConfigBean)
	 */
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
					QQCompanyNoticeTaskProcessor task;
					SpiderConfigBean spiderconfig;
					List<Company> companyList;
					
					public TimerTask set(SpiderConfigBean spiderconfig,List<Company> companyList,QQCompanyNoticeTaskProcessor task){
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

		}
	}
		
	// http://news.gtimg.cn/notice_more.php?q=sh600660&page=1
	// var finance_notice ={'total':4,'current':1,'data':[
	//["2014-08-16 00:00:00","1200132543","010113,01010503,01190501,01230101",",,,","\u798f\u8000\u73bb\u7483\uff1a2014\u5e74\u7b2c\u4e00\u6b21\u4e34\u65f6\u80a1\u4e1c\u5927\u4f1a\u51b3\u8bae\u516c\u544a","http://stockhtm.finance.qq.com/sstock/quotpage/q/600660.htm#notice-detail?id=1200132543&d=20140816","","sh600660"],
	public void processChild(List<Company> companyList,SpiderConfigBean spiderconfig) {
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
		String startUrl = spiderconfig.getStartUrl();
		String cookie = spiderconfig.getCookie();
		Map<String,String> valueMap = spiderconfig.getValueMap();
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
				jsonContent = jsonContent.substring(jsonContent.indexOf("var finance_notice =")+"var finance_notice =".length());
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
					JSONArray dataObj = (JSONArray)dataArr.get(i);
					//时间不准,都是00:00:00 所以此代码暂时不用
					String dateStr = String.valueOf(dataObj.get(0));
					String noticTitle = String.valueOf(dataObj.get(4));
					String noticeUrl = String.valueOf(dataObj.get(5));
					
					//超过1天的内容，不再抓取
					if(time-StockSpider.getTime(dateStr) > 1000*3600*24){
						continue;
					}
//					StockSpider.insertByHtml(findUrl, cookie, valueMap,companyCode);
					//因为公告 主要是提醒用户，发送私信，所以不去分析文章具体信息，所以直接存储，供SpiderStorage发送私信给用户
					
					SpiderStorageBean ssb = new SpiderStorageBean();				
					ssb.setTitle(noticTitle);
					ssb.setContent(noticTitle);
					ssb.setSummary(noticTitle);
					ssb.setTags(companyCode);
					ssb.setKey(noticeUrl);
					
					SpiderStorage.insert(ssb);	
					Thread.sleep(threadSleep);					
				}
			}catch (Exception e) {
				retryUrl.put(url,companyCode);
				log.error("异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}finally{
				
			}
		}
	}
	
	private void exeRetry(SpiderConfigBean spiderconfig){		
		retryUrl.clear();
	}
	
}
