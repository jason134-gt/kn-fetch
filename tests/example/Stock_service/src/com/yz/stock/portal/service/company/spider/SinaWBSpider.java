package com.yz.stock.portal.service.company.spider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.DefinitionList;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Company;
import com.stock.common.model.share.Article;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;

public class SinaWBSpider extends WBSpiderFacade {
	
	private static final Logger log = LoggerFactory.getLogger(SinaWBSpider.class);
	Set<String> s = Collections.synchronizedSet(new HashSet<String>());	
	static final String SER_FILE = "Sina.ser";	
	private static SinaWBSpider instance = new SinaWBSpider();
	
	public static SinaWBSpider getInstance(){
		return instance;
	}
	
	
	private void executeSpider(String cookie){
		String startUrl = "http://s.weibo.com/weibo/";
		//"http://s.weibo.com/weibo/"+URLEncoder.encode(URLEncoder.encode("中国银行&xsort=hot&Refer=STopic_hotTopic", "utf-8"),"utf-8");
		//"http://s.weibo.com/weibo/%25E4%25B8%25AD%25E5%259B%25BD%25E9%2593%25B6%25E8%25A1%258C&xsort=hot&page=2";
		List<Company> companyArr = CompanyService.getInstance().getCompanyList();
		String endUrl = "&xsort=hot&Refer=STopic_hotTopic";
		//测试时只留几个
		companyArr = companyArr.subList(0, 3);
		
		DefaultHttpClient client = new DefaultHttpClient();
		client.getParams().setParameter("http.protocol.cookie-policy",
				CookiePolicy.BROWSER_COMPATIBILITY);
		client.getParams().setParameter(
				HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
		client.getParams().setParameter("http.protocol.single-cookie-header",
				true);
		for (Company company : companyArr) {
			boolean run = true;//从配置中心获取 ,如果是false,则中断抓取
			if(run == false){
				log.error("此次不运行Xueqiu！");
				return ;
			}	
			if("暂停上市".equals(company.getF032v()) || "预披露".equals(company.getF032v()) 
					||"已发行未上市".equals(company.getF032v()) ||"*ST".equals(company.getF032v()) ){
				//非正常上市的跳过
				continue;
			}
			String companyName = company.getSimpileName();
			try {
				String spiderUrl = startUrl + URLEncoder.encode(URLEncoder.encode("#"+companyName+"#",UTF8),UTF8)+endUrl;
				log.info(spiderUrl);
				HttpGet get = new HttpGet();
				get.getParams().setParameter("http.protocol.cookie-policy",CookiePolicy.BROWSER_COMPATIBILITY);
				get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:20.0) Gecko/20100101 Firefox/20.0 FirePHP/0.7.");
				get.setHeader("Host", get.getURI().getHost());
				get.setHeader("x-insight","activate");
				get.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				get.setHeader("Accept-Encoding","gzip,deflate");
				get.setHeader("Connection","keep-alive");
				get.setHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
				get.setHeader("Cookie", cookie);
				get.setHeader("Referer","http://weibo.com/u/3272329467?wvr=5&");
				HttpResponse response = client.execute(get);
				HttpEntity entity = response.getEntity();
				Header header = entity.getContentEncoding();// String encodingValue gzip
				if(header == null || !"gzip".equals(header.getValue())){
					continue;//for 跳出 异常处理
				}
				GZIPInputStream gzip = new GZIPInputStream(entity.getContent());
				StringBuffer out = new StringBuffer();   
				byte[] b = new byte[4096]; 

				for (int n; (n = gzip.read(b)) != -1;) {   
				    out.append(new String(b, 0, n));  
				}
				get.abort();
				Parser parser = Parser.createParser(out.toString(),UTF8);
				NodeList nodeList = parser.extractAllNodesThatMatch(sFilter);
		        for (int i = 0; i < nodeList.size(); i++) { 
		        	DefinitionList node = (DefinitionList) nodeList.elementAt(i);
		        	String mid = node.getAttribute("mid"); 
		        	if(s.contains(mid)){
						 //已抓取过
						log.warn("新浪ID="+mid+"之前抓取过");
						continue;
					}else{
						log.info("新浪ID="+mid+"抓取写入");
						s.add(mid);
					}
		        	Node contentNode = node.childAt(1);
		        	NodeList childNodeList = contentNode.getChildren();
		        	NodeList imgList = childNodeList.extractAllNodesThatMatch(filterImg,true);////img class=bigcursor
		        	NodeList emList = childNodeList.extractAllNodesThatMatch(filterEM,true);
		        	String contentStr = "";
		        	String imgStr = "";//图片暂时不下载下来	
		        	for(int j = 0;j<imgList.size();j++){
		        		ImageTag imgTag = (ImageTag) imgList.elementAt(j);
		        		imgStr +="<img src=\"" + imgTag.getAttribute("src") + "\">";
		        	}
		        	for(int j = 0;j<emList.size();j++){
		        		EMTag emTag = (EMTag) emList.elementAt(j);
		        		contentStr +=emTag.getStringText();
		        	}
		        	String tag = "#"+company.getSimpileName()+":"+company.getStockCode()+"#";
		        	String text = tag + " " + imgStr + " " + contentStr;
		        	if(text.length()<80){
		        		continue;
		        	}
		        	Article article = new Article();
					article.setTitle("");
					article.setSummary(text.substring(0, 50)+"...");
					article.setContent(text);
					article.setTags("#"+company.getSimpileName()+":"+company.getStockCode()+"#");
					mockPublishArticle(article);
		        	
		        }
			} catch (UnsupportedEncodingException e) {				
				e.printStackTrace();
			} catch (ClientProtocolException e) {				
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParserException e) {
				e.printStackTrace();
			}
			
			
		}
	}
	

	
	/**
	 * @author wind
	 * 针对http://s.weibo.com/weibo/接口
	 */
	NodeFilter sFilter =
			new NodeFilter() {
				private static final long serialVersionUID = -6847323218717201808L;
				// 实现该方法,用以过滤标签
				public boolean accept(Node node) {					
					if(node instanceof DefinitionList && "feed_list".equals(((DefinitionList) node).getAttribute("class"))){
						//抓取 <dl class="feed_list"
						return true;
					}
					return false;
				}
			};
	//第二层过滤取IMG
	NodeFilter filterImg =
			new NodeFilter() {
				private static final long serialVersionUID = -1610445238554067120L;
				// 实现该方法,用以过滤标签
				public boolean accept(Node node) {					
					if(node instanceof ImageTag && "bigcursor".equals(((DefinitionList) node).getAttribute("class"))){
						//抓取 <dl class="feed_list"
						return true;
					}
					return false;
				}
			};
	static class EMTag extends CompositeTag{   
		private static final long serialVersionUID = -4991491172643596954L;
				private static final String[] mIds = new String[] {"EM"};   
			          
			    public String[] getIds (){   
			        return (mIds);   
			    }   
			    public String[] getEnders (){   
			        return (mIds);   
			    }   
			} 
	
	NodeFilter filterEM =
			new NodeFilter() {
				private static final long serialVersionUID = -5551249857645374735L;
				// 实现该方法,用以过滤标签
				public boolean accept(Node node) {					
					if(node instanceof EMTag ){
						//抓取 <dl class="feed_list"
						return true;
					}
					return false;
				}
			};
	NodeFilter vFilterLi = new NodeFilter(){
		private static final long serialVersionUID = -5551249857645374735L;
		public boolean accept(Node node) {
			if(node instanceof Bullet && "list_feed_li W8_linecolor".equals(((Bullet)node).getAttribute("class"))){
				return true;
			}
			return false;
		}
		
	};
	private void executeSpiderV(String cookie){
		String startUrl = "http://huati.weibo.com/aj_topic/list?";
//		url = "http://huati.weibo.com/aj_topic/list?keyword=%E5%90%8C%E6%B4%B2%E7%94%B5%E5%AD%90" +
//		"&match_area=&all=0&pic=0&hasv=1&atten=0&prov=0&city=0&order=hot&is_olympic=0" +
//		"&topicName=%E5%90%8C%E6%B4%B2%E7%94%B5%E5%AD%90&_t=0&__rnd=1366273342315";
		List<Company> companyArr = CompanyService.getInstance().getCompanyList();
		String endUrl = "&match_area=&all=0&pic=0&hasv=1&atten=0&prov=0&city=0&order=hot&is_olympic=0&_t=0";
		//测试时只留几个
		companyArr = companyArr.subList(0, 3);
		
		DefaultHttpClient client = new DefaultHttpClient();
		client.getParams().setParameter("http.protocol.cookie-policy",
				CookiePolicy.BROWSER_COMPATIBILITY);
		client.getParams().setParameter(
				HttpConnectionParams.CONNECTION_TIMEOUT, 5000);
		client.getParams().setParameter("http.protocol.single-cookie-header",
				true);
		for (Company company : companyArr) {
			boolean run = true;//从配置中心获取 ,如果是false,则中断抓取
			if(run == false){
				log.error("此次不运行Xueqiu！");
				return ;
			}	
			if("暂停上市".equals(company.getF032v()) || "预披露".equals(company.getF032v()) 
					||"已发行未上市".equals(company.getF032v()) ||"*ST".equals(company.getF032v()) ){
				//非正常上市的跳过
				continue;
			}
				String companyName = company.getSimpileName();
				
			try {
				String spiderUrl = startUrl +"keyword=" +URLEncoder.encode(companyName,UTF8)+"&topicName="+endUrl;
				HttpGet get = new HttpGet(spiderUrl);
				get.getParams().setParameter("http.protocol.cookie-policy",CookiePolicy.BROWSER_COMPATIBILITY);
				get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:20.0) Gecko/20100101 Firefox/20.0 FirePHP/0.7.");
				get.setHeader("Host", get.getURI().getHost());
				get.setHeader("x-insight","activate");
				get.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				get.setHeader("Accept-Encoding","gzip,deflate");
				get.setHeader("Content-Type","application/x-www-form-urlencoded");
				get.setHeader("Connection","keep-alive");
				get.setHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
				get.setHeader("Cookie", cookie);
				get.setHeader("Referer","http://huati.weibo.com/k/"+URLEncoder.encode(companyName,UTF8)+"?filter=hasv");
				get.setHeader("X-Requested-With","XMLHttpRequest");
				HttpResponse response = client.execute(get);
				HttpEntity entity = response.getEntity();
				Header header = entity.getContentEncoding();// String encodingValue gzip
				if(header == null || !"gzip".equals(header.getValue())){
					BufferedInputStream bis = new BufferedInputStream(entity.getContent());
					Map resultMap = JacksonMapper.getInstance().readValue(bis, Map.class);
					get.abort();
					Object obj = resultMap.get("data");
					if(obj instanceof Map){
						Map dataMap = (Map)obj;
						String htmlStr = String.valueOf(dataMap.get("html"));
						Parser parser = Parser.createParser(htmlStr,"UTF-8");
						NodeList nodeList = parser.extractAllNodesThatMatch(vFilterLi);	
						for(int i=0;i<nodeList.size();i++){
							Bullet contentNode = (Bullet) nodeList.elementAt(i);
							String mid = contentNode.getAttribute("list-data").replace("mid=", "");
							if(s.contains(mid)){
								 //已抓取过
								log.warn("新浪ID="+mid+"之前抓取过");
								continue;
							}else{
								log.info("新浪ID="+mid+"抓取写入");
								s.add(mid);
							}
							NodeList childNodeList = contentNode.getChildren();
							NodeList imgList = nodeList.extractAllNodesThatMatch(filterImg);////img class=bigcursor
							NodeList emList = nodeList.extractAllNodesThatMatch(filterEM);
							String contentStr = "";
				        	String imgStr = "";//图片暂时不下载下来	
				        	for(int j = 0;j<imgList.size();j++){
				        		ImageTag imgTag = (ImageTag) imgList.elementAt(j);
				        		imgStr +="<img src=\"" + imgTag.getAttribute("src") + "\">";
				        	}
				        	for(int j = 0;j<emList.size();j++){
				        		EMTag emTag = (EMTag) emList.elementAt(j);
				        		contentStr +=emTag.getStringText();
				        	}
				        	String tag = "#"+company.getSimpileName()+":"+company.getStockCode()+"#";
				        	String text = tag + " " + imgStr + " " + contentStr;
				        	if(text.length()<80){
				        		continue;
				        	}
				        	Article article = new Article();
							article.setTitle("");
							article.setSummary(text.substring(0, 50)+"...");
							article.setContent(text);
							article.setTags("#"+company.getSimpileName()+":"+company.getStockCode()+"#");
							mockPublishArticle(article);
						}
					}
				}else{
					//gzip压缩？，此方法没有此内容
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {				
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParserException e) {
				e.printStackTrace();
			}
				
		}
		
	}

	
	public void start() {
		String cookie = "";//从配置中心获取
		cookie = "SINAGLOBAL=8074354244875.453.1366009792466; ULV=1366162882707:4:4:4:4141384711450.2964.1366162882699:1366074267627; un=happyfromtbq@gmail.com; myuid=3272329467; UOR=,,spr_web_sq_firefox_weibo_t001; ALF=1366642620; wvr=5; SUS=SID-3272329467-1366254845-JA-hgric-e1a7c61e626ac879c4310d8148a53791; SSOLoginState=1366162831; _s_tentry=login.sina.com.cn; Apache=4141384711450.2964.1366162882699; SUE=es%3Db85831ac0745a5c1b645b3d907380a60%26ev%3Dv1%26es2%3D6db717d8ef18e36bf8d0fb42fbb54090%26rs0%3Dhxu0uPzX5uh9E%252FHfiZjKDgLdkX1vWYk%252B1JzZuY%252FLApxFgtdyPUL%252B4Qc%252BunzLrWu%252BLUS0CjLzn1b7cqqjCQvWBbYr8cONwxSiLYthjp8kAKU%252BDMKwl4jA327DO3jbZbhVNZnFaN9dejiINJPVbSjBM1wsTLjx3iExHc2yWS4rpkY%253D%26rv%3D0; SUP=cv%3D1%26bt%3D1366254845%26et%3D1366341245%26d%3Dc909%26i%3Da0fe%26us%3D1%26vf%3D0%26vt%3D0%26ac%3D27%26st%3D0%26uid%3D3272329467%26user%3Dhappyfromtbq%2540gmail.com%26ag%3D4%26name%3Dhappyfromtbq%2540gmail.com%26nick%3D%25E6%2596%25B0%25E6%259C%2588%25E7%259A%2584%25E6%25A2%25A6%26fmp%3D%26lcp%3D; NSC_wjq_xfjcp_bqqt_tfswjdf_ud=ffffffff094113e745525d5f4f58455e445a4a423660; WBStore=783eed0532c2dbd6|undefined";
		boolean run = true;//从配置中心获取 
		if(run == false){
			log.error("此次不运行Xueqiu！");
			return ;
		}	
		if(StringUtil.isEmpty(cookie)){
			log.error("缺少cookie值，无法抓取Xueqiu!");
			return ;
		}
		if(s.isEmpty() ){//加载已经抓取过的ID
			Set<String> readSet = (Set<String>) reSerObj(SER_FILE);
			if(readSet != null){
				for(String readedStr :readSet){
					s.add(readedStr);
				}
			}
		}
		super.start();
		try{
			super.start();
			executeSpiderV(cookie);//抓取过程中也需要检查run
		}catch (Exception e) {
			log.error("抓取过程失败");
		}finally{
			serObj(SER_FILE,s);
			s.isEmpty();//置为空
		}
	}
}
