package com.yfzx.service.spider;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Company;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;
import com.yz.configcenter.ConfigCenterFactory;

/**
 * 适配东方财富的公司新闻
 */
public class EastmoneyCompanyTaskProcessor implements ITaskProcessor {

	static Logger log = LoggerFactory.getLogger(QQCompanyTaskProcessor.class);
	HashMap<String,String> retryUrl = new HashMap<String,String>();

	@Override
	public void process(SpiderConfigBean spiderconfig) {

		List<Company> companyList = CompanyService.getInstance().getCompanyListFromCache();
		//公司太多，将公司分组成N个线程抓取，提高效率
		int threadNum = ConfigCenterFactory.getInt("spider.threadNum", 10);
		int totalSize = companyList.size();
		int oneSize = totalSize /threadNum ;
		for(int i=0;i<threadNum;i++){
			if(i != (threadNum-1) ){
				List<Company> childCompanyList = companyList.subList(i*oneSize, (i+1)*oneSize);				
				TimerTask tt =new TimerTask() {
					EastmoneyCompanyTaskProcessor task;
					SpiderConfigBean spiderconfig;
					List<Company> companyList;

					public TimerTask set(SpiderConfigBean spiderconfig,List<Company> companyList,EastmoneyCompanyTaskProcessor task){
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
	
	
	public void processChild(List<Company> companyList,SpiderConfigBean spiderconfig) {
		//http://quote.eastmoney.com/sz000002.html
		//".jd"第一个
		//http://finance.eastmoney.com/news/1355,20140926428744547.html
		//标题=.newText h1 
		//时间=.newText .Info
		//内容=#ContentBody
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);		
		long currentTimeMillis = System.currentTimeMillis();
		HashMap<String,String> parMap = new HashMap<String,String>();			
		parMap.put("time", ".newText .Info,.titlebox .Info");
		parMap.put("title", ".newText h1,.titlebox h1");			
		parMap.put("content", "#ContentBody");
		String cookie = spiderconfig.getCookie();
		//				List<Company> companyList = CompanyService.getInstance().getCompanyListFromCache();
		for(Company company : companyList){
			String companyCode = company.getCompanyCode();
			log.debug("抓取"+companyCode+"公司的新闻");
			String num = companyCode.split("\\.")[0];
			String type = companyCode.split("\\.")[1];
			String url = "http://quote.eastmoney.com/"+type+num+".html";
			//String jsonContent = StockSpider.doSpiderByLoginCookie(url, "");
			Document doc;
			String urlNew="";
			try {
				doc = Jsoup.connect(url).timeout(10000).cookie("cookie", cookie).get();
				Elements jdElements = doc.select(".jd,#cggy1");
				if(jdElements != null && jdElements.size() >0){
					Element e = jdElements.get(0);
					Elements liEs = e.select("li");
					for(Element li : liEs){
						String timeStr = li.select("span").text().trim();
						if(StringUtil.isEmpty(timeStr)){
							continue;
						}
						
						long timeLong = StockSpider.getTime(timeStr);
						if(currentTimeMillis - timeLong < 24*3600*1000){							
							urlNew = li.select("a").get(0).attr("href");					
							StockSpider.insertByHtml(urlNew, cookie, parMap, companyCode,"","B");
							Thread.sleep(threadSleep);
						}
					}
				}else{
					log.error("异常"+url+"不包含.jd,#cggy1元素");
				}
			} catch (Exception e) {	
				retryUrl.put(urlNew,companyCode);
				log.error("异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}
			
		}

		
	}

	private void exeRetry(SpiderConfigBean spiderconfig){
		if(retryUrl.isEmpty() == true){
			return;
		}
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
		HashMap<String,String> parMap = new HashMap<String,String>();			
		parMap.put("time", ".newText .Info,.titlebox .Info");
		parMap.put("title", ".newText h1,.titlebox h1");			
		parMap.put("content", "#ContentBody");
		String cookie = "";		
		for(String url :retryUrl.keySet()){
			String companyCode = retryUrl.get(url);
			try{
				StockSpider.insertByHtml(url, cookie, parMap, companyCode,"","B");
				Thread.sleep(threadSleep);
			}catch (Exception e) {
				log.error("异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}
		}
		retryUrl.clear();	
	}
}
