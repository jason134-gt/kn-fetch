package com.yfzx.service.spider;

import java.util.Map;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.minidev.json.JSONArray;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.yz.configcenter.ConfigCenterFactory;

public class XueqiuTodayTaskProcessor implements ITaskProcessor {
	static Logger log = LoggerFactory.getLogger(XueqiuTodayTaskProcessor.class);
	
	@Override
	public void process(SpiderConfigBean spiderconfig) {	
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
		try {
			String cookie = spiderconfig.getCookie();
			String startUrl = spiderconfig.getStartUrl();
			Map<String, String> valueMap = spiderconfig.getValueMap();			
			String jsonContent = StockSpider.doSpiderByLoginCookie(startUrl,cookie);
			jsonContent = jsonContent.substring(jsonContent.indexOf("(")+1, jsonContent.lastIndexOf(")"));
			
			JSONArray urlArr = JsonPath.read(jsonContent,"$.topics[*].url");
			for(int i=0;i<urlArr.size();i++){
				String url = urlArr.get(i).toString();
				if(SpiderStorage.get(url) != null){
					continue;
				}
				Document childDoc = Jsoup.connect(url).timeout(10000).cookie("cookie", cookie).get();
				String title = childDoc.select(valueMap.get("title")).text();
				String content = childDoc.select(valueMap.get("content")).html();
				String scriptStr = childDoc.select("#center").select("script").html();
				SpiderStorageBean ssb = new SpiderStorageBean();				
				ssb.setTitle(title);
				ssb.setContent(content);
				
				//处理JS中的数据
				try {
					scriptStr = scriptStr.substring(scriptStr.indexOf("SNB.data.status =")+"SNB.data.status =".length());
					scriptStr = scriptStr.substring(0, scriptStr.indexOf("SNB.data."));
					ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
					engine.eval("var SNB = "+scriptStr+"\r\n var created_at = SNB.created_at;  var edited_at = SNB.edited_at; " +
							"var time = (edited_at == null)?created_at+'':edited_at+'';");
//					System.out.println(engine.get("created_at"));  
//					System.out.println(engine.get("edited_at"));  
					long time  = Long.valueOf(engine.get("time").toString());
					ssb.setTime(time);
				} catch (ScriptException e) {
					log.error("处理url=["+url+"]的内容出错",e);
				}
				
				ssb.setKey(url);
				
				SpiderStorage.insert(ssb);	
				Thread.sleep(threadSleep);
			}
		} catch (Exception e) {			
			log.error("处理XueqiuTodayTaskProcessor出错",e);
		}
	}
	
	public static void main(String[] args) {
		
		System.out.println(new Random(1).nextFloat());
//		ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
//		try {
//			Object obj = engine.eval("var SNB = {data:{status:{}}};SNB.data.status = {id:30288570};var s = SNB.data.status.id;");
//			System.out.println(engine.get("s"));  
//		} catch (ScriptException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
