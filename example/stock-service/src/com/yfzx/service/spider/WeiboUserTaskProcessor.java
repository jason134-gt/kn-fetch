package com.yfzx.service.spider;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.util.StringUtil;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.stock.portal.service.company.spider.JacksonMapper;

/**
 * 抓取新浪微博某些用户的微博
 */
public class WeiboUserTaskProcessor implements ITaskProcessor {
	static Logger log = LoggerFactory.getLogger(WeiboUserTaskProcessor.class);
	Set<String> retryUrl = new HashSet<String>();
	Pattern p = Pattern.compile("(http|https)(://)[\\w/:?&=\\-_.]+");
	
	@Override
	public void process(SpiderConfigBean spiderconfig) {
		log.debug("启动处理["+spiderconfig.getDesc()+"]任务");
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
		String userStr = ConfigCenterFactory.getString("spider.weibiUser", "windzx,dfcfw");
		String[] userArr = userStr.split(",");
		String cookie = spiderconfig.getCookie();
		//String cookie = "";
		String startUrl = "http://www.weibo.com";// spiderconfig.getStartUrl();
		for(String user : userArr){
			if(StringUtil.isEmpty(user)){
				continue;
			}
			boolean checkSpiderContent = false;
			String url = startUrl+"/"+user;
			try {
				String jsonContent = StockSpider.doSpiderByLoginCookie(url, cookie);
				Document doc = Jsoup.parse(jsonContent); // Jsoup.connect(url).header("cookie", cookie).timeout(10000).get();
				Elements es = doc.select("script");
				for(int i=0; i<es.size(); i++){
					Element e = es.get(i);
					String jsStr = e.html();
					if(jsStr.indexOf("\"domid\":\"Pl_Core_OwnerFeed") >=0 || jsStr.indexOf("pl.content.homeFeed.index")>=0 ){	//pl.content.homeFeed.index
						checkSpiderContent = true;//抓取到内容
						jsStr = jsStr.substring(jsStr.indexOf("(")+1, jsStr.lastIndexOf(")"));						
						Map<String,String> map = JacksonMapper.getInstance().readValue(jsStr, Map.class);
						String html = map.get("html");
//						System.out.println(html);
						Document childDoc = Jsoup.parse(html);
						Elements wbElements = childDoc.select(".WB_detail");
						for(int j=0;j<wbElements.size();j++){
							Element wbElement = wbElements.get(j);
							Elements textArr = wbElement.select(".WB_text");							
							Elements timeArr = wbElement.select("a[node-type=feed_list_item_date]");
							if(textArr ==null || textArr.size() == 0 || timeArr ==null || timeArr.size() == 0){
								continue;
							}
						
							Element textObj = textArr.get(0);
							Element timeObj = timeArr.get(0);
							
							String content = textObj.html() ;
							if(content.contains("@")){
								continue;
							}
							Elements imgArr = wbElement.select("img[node-type=feed_list_media_bgimg]");
							
							String key = startUrl + timeObj.attr("href");
							String time = timeObj.attr("date");
							long timeL = StockSpider.getTime(time);
							SpiderStorageBean oldSsb = SpiderStorage.get(key);
							if(oldSsb != null){								
								continue;
							}
							SpiderStorageBean ssb = new SpiderStorageBean();				
							ssb.setKey(key);
							ssb.setTitle("");
							
							//微博样式调整
							content = StockSpider.toContent(content);							
							String summary = StockSpider.toSummary(content).replace("O网页链接", "");
							//分析出简介中的链接，加上a标签
							Matcher m = p.matcher(summary);
							StringBuffer sb = new StringBuffer();								
							while(m.find()){
								m.appendReplacement(sb, "<a href=\"$0\" target=\"new\">$0</a>");
							}
							m.appendTail(sb);
							
							Elements longWbLinkArr = wbElement.select(".W_btn_c,.W_btn_cardlink");
							if(longWbLinkArr != null && longWbLinkArr.size() > 0){
								String longWbLink = longWbLinkArr.get(0).attr("href");
//									String linkTxt = longWbLinkArr.get(0).text().replace("|", "");
								String linkHtml = "<a href=\""+longWbLink+"\" target=\"new\">"+longWbLink+"</a>";
								sb.append(linkHtml);									
							}
							
							summary = sb.toString();
							content = sb.toString();
							ssb.setSummary(summary);								
							
							if(imgArr != null && imgArr.size() > 0){
								//小图 换成大图片
								String img = imgArr.outerHtml().replace("thumbnail", "mw1024");
								content += "<br>" + img ;
								ssb.setImg(img);
							}
							ssb.setContent(content);
							ssb.setTime(timeL);
							ssb.putAttr("s", "微博");
							SpiderStorage.insert(ssb);
						}
					}
				}
				if(checkSpiderContent == false){
					log.error("内容抓取失败，请检查cookie是否无效,url="+url);
				}
				Thread.sleep(threadSleep);
			} catch (Exception e) {
				retryUrl.add(url);
				log.error("处理url["+url+"]异常"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}finally{
				
			}
		}
		exeRetry(spiderconfig);
	}
	
	private void exeRetry(SpiderConfigBean spiderconfig){
		Iterator<String> iterator= retryUrl.iterator();
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);		
		String cookie = spiderconfig.getCookie();		
		String startUrl = "http://www.weibo.com";
		while(iterator.hasNext()){
			boolean checkSpiderContent = false;
			String url = iterator.next();
			try {
				String jsonContent = StockSpider.doSpiderByLoginCookie(url, cookie);
				Document doc = Jsoup.parse(jsonContent); // Jsoup.connect(url).header("cookie", cookie).timeout(10000).get();
				Elements es = doc.select("script");
				for(int i=0; i<es.size(); i++){
					Element e = es.get(i);
					String jsStr = e.html();
					if(jsStr.indexOf("\"domid\":\"Pl_Core_OwnerFeed") >=0 || jsStr.indexOf("pl.content.homeFeed.index")>=0 ){	//pl.content.homeFeed.index
						checkSpiderContent = true;//抓取到内容
						jsStr = jsStr.substring(jsStr.indexOf("(")+1, jsStr.lastIndexOf(")"));						
						Map<String,String> map = JacksonMapper.getInstance().readValue(jsStr, Map.class);
						String html = map.get("html");
//						System.out.println(html);
						Document childDoc = Jsoup.parse(html);
						Elements textArr = childDoc.select(".WB_detail>.WB_text");
						Elements WB_detail_Arr = childDoc.select(".WB_detail");
						//.chePicMin img[node-type='feed_list_media_bgimg']
						Elements timeArr = childDoc.select(".WB_detail>.WB_func .WB_time");
						for(int j=0;j<textArr.size();j++){
							Element textObj = textArr.get(j);
							Element timeObj = timeArr.get(j);
							
							String content = textObj.html() ;
							if(content.contains("@")){
								continue;
							}
							Elements imgArr = WB_detail_Arr.get(j).select(".chePicMin img[node-type=feed_list_media_bgimg]");
							
							String key = startUrl + timeObj.attr("href");
							String time = timeObj.attr("date");
							long timeL = StockSpider.getTime(time);
							SpiderStorageBean oldSsb = SpiderStorage.get(key);
							if(oldSsb != null){								
								continue;
							}
//							log.info("新增key=["+key+"]");
							SpiderStorageBean ssb = new SpiderStorageBean();				
							ssb.setKey(key);
							ssb.setTitle("");
							if(imgArr != null && imgArr.size() > 0){
								//小图 换成大图片
								String img = imgArr.outerHtml().replace("thumbnail", "mw1024");
								content +=img ;
								ssb.setImg(img);
							}
							ssb.setContent(StockSpider.toContent(content));
							String summary = ssb.getSummary();
							if(StringUtil.isEmpty(summary)){
								summary = StockSpider.toSummary(content);
								ssb.setSummary(summary);
							}							
							ssb.setTime(timeL);
							SpiderStorage.insert(ssb);
						}
//						System.out.println(content);
					}
				}
				if(checkSpiderContent == false){
					log.error("内容抓取失败，请检查cookie是否无效");
				}
				Thread.sleep(threadSleep);
			} catch (Exception e) {
				log.error("处理url["+url+"]异常"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}finally{
				
			}
		}
		retryUrl.clear();
	}
}
