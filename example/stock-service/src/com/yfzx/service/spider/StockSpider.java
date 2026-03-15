package com.yfzx.service.spider;

import java.io.CharArrayWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minidev.json.JSONArray;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.stock.common.bloomfilter.BFConst;
import com.stock.common.bloomfilter.BFUtil;
import com.stock.common.model.Company;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.stock.portal.service.company.spider.JacksonMapper;

public class StockSpider {
	private static HashMap<String,TimerTask> taskMap = new HashMap<String,TimerTask>();
	private static HashMap<String,String> cronTaskMap = new HashMap<String,String>();
	static String config = "{\"qq\":{\"key\":\"qq\",\"type\":\"html\",\"startType\":\"0\",\"startUrl\":\"http://finance.qq.com/stock/\",\"cookie\":\"\",\"childLevel\":\"2\",\"selectUrl\":\"#newsList .Q-hot a\",\"urlRegex\":\"^http://finance.qq.com/a/[0-9]+/[0-9]+.htm$\",\"valueMap\":{\"content\":\"#Cnt-Main-Article-QQ\",\"title\":\"#C-Main-Article-QQ .hd h1\"},\"encode\":\"utf-8\"}}";
	static Logger log = LoggerFactory.getLogger(StockSpider.class);

	public static void start(String taskKey){
		startSpiderThread(taskKey);
	}
	public static void startAll(){
		String spiderConfig = getConfigStr();
		try {
			Map<String,String> resultMap = JacksonMapper.getInstance().readValue(spiderConfig, Map.class);
			Set<String> set =  resultMap.keySet();
			for(String taskKey:set){
				startSpiderThread(taskKey);
			}
		}catch (Exception e) {
			log.error("启动所有任务",e.getMessage());
		}
	}

	public static List<SpiderConfigBean> keys(){
		List<SpiderConfigBean> reList  = new ArrayList<SpiderConfigBean>();
		String spiderConfig = getConfigStr();

		try {
			Map<String,Object> configMap = JacksonMapper.getInstance().readValue(spiderConfig, Map.class);
			Set<String> set =  configMap.keySet();
			for(String taskKey:set){
				Object obj = configMap.get(taskKey);
				if(obj == null)continue;
				SpiderConfigBean spiderconfig = new SpiderConfigBean();
				org.apache.commons.beanutils.BeanUtils.copyProperties(spiderconfig, obj);
				String desc = spiderconfig.getDesc();
				SpiderConfigBean reBean = new SpiderConfigBean();
				if(StringUtil.isEmpty(desc)){
					reBean.setKey(taskKey);
					reBean.setDesc(taskKey);
				}else{
					reBean.setKey(taskKey);
					reBean.setDesc(desc);
				}
				reList.add(reBean);
			}
		}catch (Exception e) {
			log.error("stock.spider keys ",e.getMessage());
		}
		return reList;
	}


	private synchronized static void startSpiderThread(String taskKey){
		if(taskMap.containsKey(taskKey) == false){
			TimerTask tt =new TimerTask() {
				String taskKey ;
				public TimerTask set(String taskKey){
					this.taskKey = taskKey;
					return this;
				}
				public void run() {
					log.info("抓取["+taskKey+"]开始");
					long starttime = System.currentTimeMillis();
					try{
						runSpider(taskKey);
					}catch (Exception e) {
						log.error("抓取["+taskKey+"]失败" + e.getMessage());
					}finally{
						long endtime = System.currentTimeMillis();
						log.info("抓取["+taskKey+"]完成耗时"+(endtime-starttime)+"毫秒");
						taskMap.remove(taskKey);
						try {
							Thread.sleep(10000l);
						} catch (InterruptedException e) {
						}
						//十秒后线程结束并保存BF
						BFUtil.flushDisk(BFConst.spiderKeyFilter);
						this.cancel();
					}
				}
			}.set(taskKey);
			taskMap.put(taskKey, tt);
			Timer timer = new Timer("Sprider["+taskKey+"]",true);
			timer.schedule(tt,1);
		}else{
			long lastExeTime = taskMap.get(taskKey).scheduledExecutionTime();
			long taskTimeout = ConfigCenterFactory.getLong("spider.taskTimeout", (400*1000l));
			//已存在任务，并超时的话 处理
			if(System.currentTimeMillis() - lastExeTime > taskTimeout ){
				taskMap.get(taskKey).cancel();
				taskMap.remove(taskKey);
				BFUtil.flushDisk(BFConst.spiderKeyFilter);
				log.warn("任务Sprider["+taskKey+"]超时，删除列表，重新开始");
			}else{
				log.warn("任务Sprider["+taskKey+"]未完成");
			}
		}
	}

	private static String getConfigStr(){
		String confStr = ConfigCenterFactory.getString("spider.config","");
		String configStr = StockSpider.config;
		if( StringUtil.isEmpty(confStr) == false){
			configStr = StockUtil.unescape(confStr);
		}
		return configStr;
	}

	private static void runSpider(String taskKey){
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
		try {
			String configStr = getConfigStr();
			Map<String,Object> configMap = JacksonMapper.getInstance().readValue(configStr, Map.class);
			Object obj = configMap.get(taskKey);
			if(obj == null)return;
			SpiderConfigBean spiderconfig = new SpiderConfigBean();
			org.apache.commons.beanutils.BeanUtils.copyProperties(spiderconfig, configMap.get(taskKey));
			String jsonOrHtml = spiderconfig.getType();
			spiderconfig.getChildLevel();
			String startType = spiderconfig.getStartType();
			String cookie = spiderconfig.getCookie();
			Map<String, String> valueMap = spiderconfig.getValueMap();
			String startUrl = spiderconfig.getStartUrl();
			if("1".equals(startType)){//根据公司来抓取
				List<Company> companyList = CompanyService.getInstance().getCompanyListFromCache();
				for(Company company : companyList){
					String companyCode = company.getCompanyCode();
					String num = companyCode.split("\\.")[0];
					String type = companyCode.split("\\.")[1];
					//url的拼接比较麻烦，配置化实现先尝试支持几种常见的 ，另外港股和A股是分开的，有各自的URL
					//使用模版方式进行拼接URL
					VelocityContext context = new VelocityContext();
					context.put("num", num);
					context.put("type", type);
					CharArrayWriter writer = new CharArrayWriter();
					Velocity.evaluate(context, writer, "", startUrl);
					//Velocity.evaluate(context, writer, "", "http://quotes.money.163.com/#if(${type}=='sz')1#elseif(${type}=='sh')0#end\r\n${num}.html");
					String url = writer.toString();
					//String url = startUrl.replace("{num}", num).replace("{type}", type);

					if("json".equals(jsonOrHtml)){
						insertByJson(url, cookie, valueMap, "");
					}else if("html".equals(jsonOrHtml)){
						if("2".equals(spiderconfig.getChildLevel())){
							try{
								Document doc = Jsoup.connect(url).timeout(10000).cookie("cookie", cookie).get();
								String selectUrl = spiderconfig.getSelectUrl();
								Elements items = doc.select(selectUrl);
								String urlRegexStr = spiderconfig.getUrlRegex();
								Pattern pattern = Pattern.compile(urlRegexStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
								int size = items.size();
								for(int i=0;i<size;i++){
									Element e = items.get(i);
									String href = e.attr("href");
									Matcher matcher = pattern.matcher(href);
									boolean b = matcher.matches();
									if(b == false)continue;
									if(SpiderStorage.get(href) != null){
										continue;
									}
									insertByHtml(href, cookie, valueMap,companyCode);
									Thread.sleep(threadSleep);
								}
							}catch (Exception e) {
								log.error("公司url["+url+"]出错",e);
							}
						}else{
							String key = DateUtil.getSysDate("yyyy-MM-dd HH")+"时 "+startUrl;
							insertByHtml(url, cookie, valueMap,companyCode,key);
							Thread.sleep(threadSleep);
						}
					}
				}//end for
			}else if("0".equals(startType)){//财经门户处抓取，一般都是html
				if("json".equals(jsonOrHtml)){
					log.error(startUrl+"暂未支持抓取");
				}else  if("html".equals(jsonOrHtml)){
					Document doc = Jsoup.connect(startUrl).timeout(10000).get();
					if("1".equals(spiderconfig.getChildLevel())){
						String title = doc.select(valueMap.get("title")).text();
						String content = doc.select(valueMap.get("content")).html();
						SpiderStorageBean ssb = new SpiderStorageBean();
						ssb.setTitle(title);
						ssb.setContent(content);
						String timeCss = valueMap.get("time");
						if(StringUtil.isEmpty(timeCss) == false ){
							String timeStr = doc.select(timeCss).text();
							if(timeStr.length() > 19){
								timeStr = timeStr.substring(0, 19);
							}
							long time = getTime(timeStr);
							ssb.setTime(time);
						}
						ssb.setKey(DateUtil.getSysDate("yyyy-MM-dd HH")+"时 "+startUrl);
						SpiderStorage.insert(ssb);
						Thread.sleep(threadSleep);
					}else if("2".equals(spiderconfig.getChildLevel())){
						String selectUrl = spiderconfig.getSelectUrl();
						Elements items = doc.select(selectUrl);
						String urlRegexStr = spiderconfig.getUrlRegex();//"^http://finance.qq.com/a/\\d*/\\d*.htm$";
						Pattern pattern = Pattern.compile(urlRegexStr, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
						int size = items.size();
						for(int i=0;i<size;i++){
							Element e = items.get(i);
							String href = e.attr("href");
							Matcher matcher = pattern.matcher(href);
							boolean b = matcher.matches();
							if(b == false)continue;
							if(SpiderStorage.get(href) != null){
								continue;
							}
							insertByHtml(href, cookie, valueMap,"");
							Thread.sleep(threadSleep);
						}
					}
				}// end html
			}//end 	startType
			else{
				String pkg = StockSpider.class.getPackage().getName();
				ITaskProcessor taskProcessor = (ITaskProcessor)Class.forName(pkg+"."+taskKey).newInstance();
				taskProcessor.process(spiderconfig);
			}
		}catch (Exception e) {e.printStackTrace();
			log.error("任务=["+taskKey+"]出错"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
		}

	}

	public static String doSpiderByLoginCookie(String url,String cookie){
		return doSpiderByLoginCookie(url,cookie,null);
	}

	public static String doSpiderByLoginCookie(String url,String cookie,String encoding){
		try {
			Map<Object,Object> header = new HashMap<Object,Object>();
			header.put("Cookie", cookie);
			String str = new HttpRequestProxy().doRequest(url, null, header, encoding);
			return str; //IOUtils.toInputStream(str);
			//http://z1y1m1.blog.163.com/blog/static/518373272013626778409/
			//会返回CircularRedirectException 设置ClientPNames.ALLOW_CIRCULAR_REDIRECTS, false 无效
		}catch (Exception e) {
			log.error("网络抓链接异常=["+url+"]" + e.getMessage());
		}
		return null;
	}

	/**
	 * 目前只支持格式如<br>
	 * [1405578816000]<br>
	 * [2014-07-05 02:09]<br>
	 * [2014-07-17 13:56:58]<br>
	 * [07-17]<br>
	 * [07-17 20:56]<br>
	 * [今天 00:26]<br>
	 * @return
	 */
	public static long getTime(String str){
		Date date = null;
		try {
			//1405578816000
			if(Pattern.matches("^\\d{0,13}$", str)){
				return Long.parseLong(str);
			}else if(str.length() == 16 && Pattern.matches("^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2} [0-9]{1,2}:[0-9]{1,2}$", str)){
				//2014-07-05 02:09
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(str);
			}else if(str.length() == 19 && Pattern.matches("^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2} [0-9]{1,2}:[0-9]{1,2}:[0-9]{1,2}$", str)){
				//2014-07-17 13:56:58
				date = DateUtil.format(str, "yyyy-MM-dd HH:mm:ss");
			}else if(Pattern.matches("^[0-9]{1,2}-[0-9]{1,2}$", str)){
				//07-17
				Date sysDate = new Date();
				String year = DateUtil.getSysDateYYYYMMDD(sysDate).substring(0, 5);
				String hms = DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, sysDate).substring(10);
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(year +str+hms);
			}else if(Pattern.matches("^[0-9]{1,2}-[0-9]{1,2} [0-9]{1,2}:[0-9]{1,2}$", str)){
				//07-17 20:56
				String year = DateUtil.getSysDateYYYYMMDD(new Date()).substring(0, 5);
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(year+str);
			}else if(str.contains("今天")){
				//今天 00:26
				String ymd = DateUtil.getSysDateYYYYMMDD(new Date());
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(str.replace("今天", ymd));
			}
		} catch (ParseException e) {
			log.error("时间转换出错"+e.getMessage());
		}
		if(date != null){
			return date.getTime();
		}else{
			return 0l;
		}
	}
	/**
	 * 去除A链接,保留表格
	 */
	public static Whitelist myWhitelist(){
		return new Whitelist().addTags(new String[] { "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em", "i", "li", "ol", "p", "pre", "q", "small", "strike", "strong", "sub", "sup", "u", "ul","tr" }).
		addAttributes("blockquote", new String[] { "cite" }).addAttributes("q", new String[] { "cite" }).addProtocols("blockquote", "cite", new String[] { "http", "https" }).
		addProtocols("cite", "cite", new String[] { "http", "https" }).addTags(new String[] { "img" }).addAttributes("img", new String[] { "align", "alt", "height", "src", "title", "width" })
		.addProtocols("img", "src", new String[] { "http", "https" }).addAttributes("p", "style").addAttributes("span", "style")
		.addAttributes("table", "summary", "width").addAttributes("td", "abbr", "axis", "colspan", "rowspan", "width");
	}

	public static Whitelist myWhitelist2(){
		return new Whitelist().addTags(new String[] { "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em", "i", "li", "ol", "p", "pre", "q", "small", "strike", "strong", "sub", "sup", "u", "ul","tr" }).
		addAttributes("blockquote", new String[] { "cite" }).addAttributes("q", new String[] { "cite" }).addProtocols("blockquote", "cite", new String[] { "http", "https" }).
		addProtocols("cite", "cite", new String[] { "http", "https" })
		.addTags(new String[] { "img" }).addAttributes("img", new String[] { "align", "alt", "height", "src", "title", "width" })
		.addAttributes("p", "style").addAttributes("span", "style")
		.addAttributes("table", "summary", "width").addAttributes("td", "abbr", "axis", "colspan", "rowspan", "width");
	}

	public static void insertByHtml(String url,String cookie,Map<String, String> valueMap,String tag){
		insertByHtml(url, cookie, valueMap, tag,"");
	}

	public static void insertByHtml(String url,String cookie,Map<String, String> valueMap,String tag,String key){
		insertByHtml(url, cookie, valueMap, tag, key, "");
	}
	public static void insertByHtml(String url,String cookie,Map<String, String> valueMap,String tag,String key,String putLevel){
		try{
			if(StringUtil.isEmpty(key)){
				key = url;
			}
			if(SpiderStorage.get(key) != null){
				return;
			}
			if(BFUtil.checkAndAdd(BFConst.spiderKeyFilter, "url="+key)){
				//减少重复抓取[此类都是具体新闻链接,只需要抓取一次]
				return;
			}
			Document childDoc = Jsoup.connect(url).timeout(10000).cookie("cookie", cookie).get();
			String title = childDoc.select(valueMap.get("title")).text();
			String content = childDoc.select(valueMap.get("content")).html();
			//过滤掉各种非法字符，如脚本，样式等等 http://www.open-open.com/jsoup/whitelist-sanitizer.htm
			content = Jsoup.clean(content,myWhitelist()/*Whitelist.basicWithImages().addAttributes("a", "target").addAttributes("p", "style").addAttributes("span", "style")*/);
			String img = childDoc.select(valueMap.get("content")+ " img").outerHtml();
			String timeCss = valueMap.get("time");
			SpiderStorageBean ssb = new SpiderStorageBean();
			ssb.setTitle(title);
			ssb.setContent(toContent(content));
			if(StringUtils.isEmpty(img) == false){
				ssb.setImg(img);
			}
			if(StringUtil.isEmpty(timeCss) == false ){
				String timeStr = childDoc.select(timeCss).text();
				timeStr = timeStr.replace("年", "-").replace("月", "-").replace("日", "");
				if(timeStr.length() > 16){
					//如果跟时间相关的字段太长，需要抽取出正确的时间
					Pattern p = Pattern
							.compile("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2} [0-9]{1,2}(:[0-6][0-9]){1,2}");
					Matcher m = p.matcher(timeStr);
					String newTimeStr = "";
					while (m.find()) {
						if (!"".equals(m.group())) {
							newTimeStr = m.group();
							break;
						}
					}
					if(StringUtil.isEmpty(newTimeStr)){
						log.error("时间获取失败"+timeStr);
						return ;
					}
					timeStr = newTimeStr;
				}
				long time = getTime(timeStr);
				ssb.setTime(time);
			}
			String summary = ssb.getSummary();
			if(StringUtil.isEmpty(summary)){
				summary = toSummary(content);
				ssb.setSummary(summary);
			}
			if(StringUtil.isEmpty(key)){
				ssb.setKey(url);
			}else{
				ssb.setKey(key);
			}
			//有些新闻包含多个公司，由后续处理
			ssb.setTags(tag);
			if(StringUtil.isEmpty(putLevel) ==false){
				ssb.setPutLevel(putLevel);
			}
			if( StringUtil.isEmpty(valueMap.get("s")) == false ){
				ssb.putAttr("s", valueMap.get("s"));
			}
			if(StringUtil.isEmpty(ssb.getContent())){
				ssb.setContent(toContent(content));
			}
			SpiderStorage.insert(ssb);
		}catch (Exception e) {e.printStackTrace();
			log.error("处理url=["+url+"]的内容出错" +e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
//			log.error("处理url=["+url+"]的内容出错" , e);
		}
	}

	public static void insertByJson(String url,String cookie,Map<String, String> valueMap,String tag){
		try{
			String title = null,content = null ;
			String jsonContent = doSpiderByLoginCookie(url, cookie);

			JSONArray titleArr = JsonPath.read(jsonContent,valueMap.get("title"));
			JSONArray contentArr = JsonPath.read(jsonContent,valueMap.get("content"));
			JSONArray keyArr =  JsonPath.read(jsonContent,valueMap.get("key"));
			String timeCss = valueMap.get("time");
			JSONArray timeArr = null;
			if(StringUtil.isEmpty(timeCss) == false ){
				timeArr = JsonPath.read(jsonContent,timeCss);
			}
			for(int i=0;i<contentArr.size();i++){
				if(titleArr == null || titleArr.get(i) == null)continue;
				title = titleArr.get(i).toString();
				content = contentArr.get(i).toString();
				SpiderStorageBean ssb = new SpiderStorageBean();
				ssb.setKey(keyArr.get(i).toString());
				ssb.setTitle(title);
				ssb.setContent(toContent(content));

				if(StringUtil.isEmpty(timeCss) == false ){
					String timeStr = timeArr.get(i).toString();
					if(timeStr.length() > 19){
						timeStr = timeStr.substring(0, 19);
					}
					long time = getTime(timeStr);
					ssb.setTime(time);
				}
				String summary = ssb.getSummary();
				if(StringUtil.isEmpty(summary)){
					summary = toSummary(content);
					ssb.setSummary(summary);
				}
				SpiderStorage.insert(ssb);
			}
		}catch (Exception e) {
			log.error("处理url=["+url+"]的内容出错 " + e.getMessage());
		}
	}

	/**
	 * 获取正文的简介
	 */
	public static String toSummary(String content){
		String summary = content.replaceAll("<script[^<]*>.*</script>", "").replaceAll("<!--[^<]*>[^(-->)]*<![^<]*-->", "").replaceAll("<[^<]*>", "").replace(" ", "").replace("\r\n", "").replace("\r", "").replace("\n", "").replace("\t", "").replace("&nbsp;", "");
//		if(summary.length() > 140){
//			summary = summary.substring(0, 140);
//		}
		if(content.length()<=140){
			return summary;
		}else{
			int TotalLength=140*2;
			int resultInt = 0;
			int chineseCount=0;
			int forLength = (TotalLength > summary.length())?summary.length():TotalLength;
			for(int i=0;i<forLength;i++){
				char c = summary.charAt(i);
				if(0<c&&c<128){
					chineseCount++;
				}else{
					chineseCount = chineseCount+ 2;
				}
				resultInt ++;
				if(chineseCount >= TotalLength){
					break;
				}
			}
			return summary.substring(0,resultInt);
		}
	}

	/**
	 * 获取正文的过滤后正文<br>
	 * 过滤脚本和
	 */
	public static String toContent(String content){
		String tmpContent = content.replaceAll("<script[^<]*>.*</script>", "").replaceAll("<!--[^<]*>[^(-->)]*<![^<]*-->", "");
		return tmpContent;
	}

	public static void main(String[] args) {

//		String str = "【中日关系从悬崖边缘折回】- 在过去两年紧张关系不断升级后http://www.pingwest.com/tencent-to-enter-tv-game/，" +
//				"中日关系在最近几周悄然被从悬崖边上拉了回来，http://192.168.1.112/article-info.html?uid=9&uuid=c8121b08-63fc-477c-8fdb-ae8d9ffd552e这两个亚洲最大经济体的关系终于有所缓和。" +
//				"http://t.cn/RhTWfYn中 http://t.cn/RhTWfYn2.html";
//		String str = "中日关系从悬崖边缘折回";
//		// 日期格式 如果如2014年5月6日 12时23分33秒 或2014/05/06 12:23:33 建议将字符替换掉
//
//			Pattern p = Pattern
//					.compile("(http|https)(://)[\\w/:?&=\\-_.]+");
//			Matcher m = p.matcher(str);
//			StringBuffer sb = new StringBuffer();
//			//在匹配到的字符串中替换掉某个字符，如在匹配到的ABC中把B替换为@
//			while(m.find()){
//				m.appendReplacement(sb, "<a href=\"$0\" target=\"new\">$0</a>");
//			}
//			m.appendTail(sb);
//			System.out.println(sb.toString());

//		System.out.println(getTime("今天 20:56"));
		String url = "http://www.weibo.com/2246141871";
		String cookie = "SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WWz3xCHI1bkXZzUuchcn6q95JpX5K2t; SINAGLOBAL=8692660790471.508.1403582404870; ULV=1411876347348:18:1:1:2984808068909.4414.1411876347318:1407317856040; UOR=,,login.sina.com.cn; __utma=15428400.295583652.1406518894.1406518894.1406518894.1; __utmz=15428400.1406518894.1.1.utmcsr=s.weibo.com|utmccn=(referral)|utmcmd=referral|utmcct=/friend/common; SUB=_2AkMjewkAa8NlrABYnvsQxGPqaYpH-jyQo8_2An7uJhIyHRgv7nQuqSUOsZsfylMxLj8Zj45KQZqPtv1Sag..; myuid=3963588857; login_sid_t=0a863d9d727347d166206c5f09b6f514; _s_tentry=login.sina.com.cn; Apache=2984808068909.4414.1411876347318; appkey=; SUS=SID-3963588857-1411876407-GZ-yvk35-57daa271cab51e34ad730df0f1ea8d20; SUE=es%3D517e122e98708d869133d055c3e34e31%26ev%3Dv1%26es2%3D9826775bd8d77b2ce23569fccbfc86d9%26rs0%3DRGTvmmWfnpezwqPgPOZgnC7hMSr%252BBYcDk7wAH60qKn6ARPHzF9fxaU%252FWx1JxyOUPIaZ7pgAXq%252FxXCSextf7PdNUj97k1Vw3%252FxZV57wzNGkP6Eyy%252B9KEgD0lp6CXSw2OAp%252B3ZmliAtta%252F%252F109jlRhThG%252FqLYe5HecWzyThefwUlo%253D%26rv%3D0; SUP=cv%3D1%26bt%3D1411876407%26et%3D1411962807%26d%3Dc909%26i%3D8d20%26us%3D1%26vf%3D0%26vt%3D0%26ac%3D0%26st%3D0%26uid%3D3963588857%26name%3Dadmin%2540igushuo.com%26nick%3D%25E7%2588%25B1%25E8%2582%25A1%25E8%25AF%25B4%26fmp%3D%26lcp%3D2014-01-02%252009%253A13%253A37; ALF=1443412407; SSOLoginState=1411876407; wvr=5; SWB=usrmdinst_20";
		String cotent = StockSpider.doSpiderByLoginCookie(url, cookie);
		System.out.println(cotent);
	}


}
