package com.yfzx.service.spider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 全景网的公告，时间比较新
 */
public class P5wNoticeTaskProcessor implements ITaskProcessor {

	/* (non-Javadoc)
	 * @see com.yfzx.service.spider.ITaskProcessor#process(com.yfzx.service.spider.SpiderConfigBean)
	 */
	@Override
	public void process(SpiderConfigBean spiderconfig) {
		//http://data.p5w.net/gsgg.html
		String startUrl = "http://data.p5w.net/gsgg.html";
		//$(".mb-list>ul>li>span>a")
		Document doc;
		try {
			doc = Jsoup.connect(startUrl).timeout(10000).get();
			Elements items = doc.select(".mb-list>ul>li>span>a");
			int size = items.size();
			for(int i=0;i<size;i++){
				Element e = items.get(i);
				String href = e.attr("href");	
				String url = "http://data.p5w.net"+href;
				//获取公司
				String title = e.attr("title");
				List<TagBean> tList= SpiderStorage.getTags(title);
				if(tList.size()==0)continue;
				String companycode = tList.get(0).getTag().split(":")[0];
				//设置了公司值，系统自动作为私信发出
				Map<String,String> valueMap = new HashMap<String,String>();
				valueMap.put("title",".zwleft>.title");
				valueMap.put("time","#dSource");
				valueMap.put("content","#dContent");
				StockSpider.insertByHtml(url, "", valueMap, companycode);
			}
		} catch (IOException e1) {			
			e1.printStackTrace();
		}
		
	}

}
