package com.yfzx.service.spider;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minidev.json.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.stock.common.util.StringUtil;
import com.yz.configcenter.ConfigCenterFactory;

/**
 * 此类监控用户最新数据，链接需要一直抓取
 */
public class XueqiuUserTaskProcessor implements ITaskProcessor {
	static Logger log = LoggerFactory.getLogger(XueqiuUserTaskProcessor.class);
	Set<String> retryUrl = new HashSet<String>();
	
	@Override
	public void process(SpiderConfigBean spiderconfig) {
		//此AJAX接口比http://xueqiu.com/windzixun的HTML中嵌套JS简单方便
		//http://xueqiu.com/statuses/user_timeline.json?user_id=1689987310&page=1&type=0&_=1406169011245
		//用ajax接口，则只能用uid查询 万得资讯 uid=1689987310 用户名windzixun
		log.debug("启动处理["+spiderconfig.getDesc()+"]任务");
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
		String userStr = ConfigCenterFactory.getString("spider.xueqiuUser", "1689987310,1524241372");
		String[] userArr = userStr.split(",");
		String cookie = spiderconfig.getCookie();
		for(String uid : userArr){
			if(StringUtil.isEmpty(uid)){
				continue;
			}
			String url  = "http://www.xueqiu.com/statuses/user_timeline.json?user_id="+uid
					+"&page=1&type=0&_="+System.currentTimeMillis();
			try{				
				String jsonContent = StockSpider.doSpiderByLoginCookie(url, cookie);
				JSONArray titleArr = JsonPath.read(jsonContent,"$statuses[*].title");
				JSONArray contentArr = JsonPath.read(jsonContent,"$statuses[*].text");
				JSONArray keyArr =  JsonPath.read(jsonContent,"$statuses[*].target");
				JSONArray createArr =  JsonPath.read(jsonContent,"$statuses[*].created_at");
				JSONArray editedArr =  JsonPath.read(jsonContent,"$statuses[*].edited_at"); 
				JSONArray descArr =  JsonPath.read(jsonContent,"$statuses[*].description"); 
				JSONArray imgArr =  JsonPath.read(jsonContent,"$statuses[*].firstImg"); 
				for(int i=0;i<keyArr.size();i++){
					String key = "http://www.xueqiu.com"+String.valueOf(keyArr.get(i));
					if(SpiderStorage.get(key) != null){
						//已处理过的链接，忽略
						continue;
					}
//					SpiderStorage.putUrl(key);
					String title = String.valueOf(titleArr.get(i));
					String content = String.valueOf(contentArr.get(i));					
					String create = String.valueOf(createArr.get(i));
					String desc = String.valueOf(descArr.get(i));
					Object img = String.valueOf(imgArr.get(i));
					Object edited = editedArr.get(i);
					if(edited != null){
						create = String.valueOf(edited);
					}
					
					long time = StockSpider.getTime(create);
					SpiderStorageBean ssb = new SpiderStorageBean();				
					ssb.setTitle(title);
					content = content + "<br>来源:<a href=\"http://www.xueqiu.com/\" target=\"_blank\">雪球</a>";
					ssb.setContent(content);
					ssb.setKey(key);
					ssb.setTime(time);
					ssb.setSummary(desc);
					if(img != null){
						ssb.setImg(img.toString().replace("!thumb.jpg", ""));
					}
					SpiderStorage.insert(ssb);	
				}
				Thread.sleep(threadSleep);
			}catch (Exception e) {
				retryUrl.add(url);
				log.error("异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}			
		}
		exeRetry(spiderconfig);			
	}
	
	private void exeRetry(SpiderConfigBean spiderconfig){
		Iterator<String> iterator= retryUrl.iterator();
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);		
		String cookie = spiderconfig.getCookie();
		while(iterator.hasNext()){
			String url = iterator.next();
			try{				
				String jsonContent = StockSpider.doSpiderByLoginCookie(url, cookie);
				JSONArray titleArr = JsonPath.read(jsonContent,"$statuses[*].title");
				JSONArray contentArr = JsonPath.read(jsonContent,"$statuses[*].text");
				JSONArray keyArr =  JsonPath.read(jsonContent,"$statuses[*].target");
				JSONArray createArr =  JsonPath.read(jsonContent,"$statuses[*].created_at");
				JSONArray editedArr =  JsonPath.read(jsonContent,"$statuses[*].edited_at"); 
				JSONArray descArr =  JsonPath.read(jsonContent,"$statuses[*].description"); 
				JSONArray imgArr =  JsonPath.read(jsonContent,"$statuses[*].firstImg"); 
				for(int i=0;i<keyArr.size();i++){
					String key = "http://www.xueqiu.com"+String.valueOf(keyArr.get(i));
					if(SpiderStorage.get(key) != null){
						//已处理过的链接，忽略
						continue;
					}
//					SpiderStorage.putUrl(key);
					String title = String.valueOf(titleArr.get(i));
					String content = String.valueOf(contentArr.get(i));					
					String create = String.valueOf(createArr.get(i));
					String desc = String.valueOf(descArr.get(i));
					Object img = String.valueOf(imgArr.get(i));
					Object edited = editedArr.get(i);
					if(edited != null){
						create = String.valueOf(edited);
					}
					
					long time = StockSpider.getTime(create);
					SpiderStorageBean ssb = new SpiderStorageBean();				
					ssb.setTitle(title);
					content = content + "<br>来源:<a href=\"http://www.xueqiu.com/\" target=\"_blank\">雪球</a>";
					ssb.setContent(content);
					ssb.setKey(key);
					ssb.setTime(time);
					ssb.setSummary(desc);
					if(img != null){
						ssb.setImg(img.toString().replace("!thumb.jpg", ""));
					}
					SpiderStorage.insert(ssb);	
				}
				Thread.sleep(threadSleep);
			}catch (Exception e) {				
				log.error("异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}
		}
		retryUrl.clear();
	}

}
