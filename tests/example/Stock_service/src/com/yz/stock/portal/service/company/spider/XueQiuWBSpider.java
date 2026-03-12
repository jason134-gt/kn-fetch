package com.yz.stock.portal.service.company.spider;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.configuration.Configuration;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Company;
import com.stock.common.model.share.Article;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.config.ConfigFactory;

/**
 * @author wind
 * 雪球网的爬虫
 * 1、Action层触发，Action层传递Cookie 从火狐中拷贝<br>
 * 2、调度由配置application-quatz.xml,里面包含Cookie,是否运行此爬虫,以及调度策略,建议加到数据中心启动 <br>
 * 3、以前爬过的内容，不要重复爬   "即加载时将最近已经爬过的URL加载到Set中",后面爬后检查是否已经爬过，如果已经爬过，直接跳过<br>
 * 4、爬下来的内容，模拟操作写入我们的微博系统 5、提供停止接口 供Action层调用<br>
 * 使用配置中心
 * 待完善内容：要爬的链接规则 如公司和V用户
 */
public class XueQiuWBSpider extends WBSpiderFacade{
	
	private static final Logger log = LoggerFactory.getLogger(XueQiuWBSpider.class);
	Set<String> s = Collections.synchronizedSet(new HashSet<String>());	
	static final String SER_FILE = "Xueqiu.ser";
	
	private static XueQiuWBSpider instance = new XueQiuWBSpider();
	
	public static XueQiuWBSpider getInstance(){
		return instance;
	}

	private void executeSpider(String cookie) {
		
		int atokenStart = cookie.indexOf("xq_a_token=")+"xq_a_token=".length();
		int atokenEnd = cookie.indexOf(";", atokenStart);
		String access_token = cookie.substring(atokenStart,atokenEnd);
		//http://xueqiu.com/service/getproxy?source=all&symbol=SH601988&range=1365696000000%2C1366127999000&sort=reply&summary_count=20&ex=1&access_url=%2Fstatuses%2Fsummary_search.json&access_token=inDrf7ePkwlZMVsC087Vhr&_=1366096176318
		/*
		String startUrl = "http://xueqiu.com/service/getproxy?source=all&symbol="; //"http://xueqiu.com/S/";
		Calendar now=Calendar.getInstance();
		now.roll(Calendar.DATE, -3);
		//range 时间 其实 用于搜索数据用
		String endUtl = "&range="+ now.getTimeInMillis()/1000*1000 +"%2C"+  System.currentTimeMillis()/1000*1000
				+"&sort=reply&summary_count=20&ex=1&access_url=%2Fstatuses%2Fsummary_search.json" +
				"&access_token="+access_token+"&_=1366096176318";
		*/
		//http://xueqiu.com/service/getter?url=http%3A%2F%2Fapi.xueqiu.com%2Fstatuses%2Fsearch.json&data[count]=5&data[comment]=0&data[symbol]=SH601988&data[hl]=0&data[source]=all&data[sort]=alpha&data[page]=1&data[access_token]=inDrf7ePkwlZMVsC087Vhr&_=1366098573448
		//data[sort]=alpha精华  data[sort]=time 最新
		String startUrl = "http://xueqiu.com/service/getter?url=http%3A%2F%2Fapi.xueqiu.com%2Fstatuses%2Fsearch.json&data[count]=5&data[comment]=0&data[symbol]=";
		String endUrl = "&data[hl]=0&data[source]=all&data[sort]=alpha&data[page]=1&data[access_token]="+access_token+"&_="+System.currentTimeMillis();
		List<Company> companyArr = CompanyService.getInstance().getCompanyList();
		
		//测试时只留几个
//		companyArr = companyArr.subList(0, 3);
		
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
			// 万 科Ａ:000002.sz 到 000002.SZ
			String yfCode = company.getStockCode()/*.split(":")[1]*/.toUpperCase();																				 
			String xqCode = yfCode.split("\\.")[1] + yfCode.split("\\.")[0];
			String spiderUrl = startUrl + xqCode+ endUrl;
			log.info(spiderUrl);
			HttpGet get = new HttpGet();
			get.getParams().setParameter("http.protocol.cookie-policy",
					CookiePolicy.BROWSER_COMPATIBILITY);
			get.setHeader(
					"User-Agent",
					"Mozilla/5.0 (Windows NT 5.1; rv:20.0) Gecko/20100101 Firefox/20.0 FirePHP/0.7.");

			get.setHeader("x-insight", "activate");
			get.setHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			get.setHeader("Accept-Encoding", "gzip,deflate");
			get.setHeader("Connection", "keep-alive");
			get.setHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
			get.setHeader("Cookie", cookie);
			get.setURI(URI.create(spiderUrl));
			get.setHeader("Host", get.getURI().getHost());
			
			try {
				HttpResponse response = client.execute(get);
				HttpEntity entity = response.getEntity();
				Header header = entity.getContentEncoding();// String encodingValue gzip
				if(header == null || !"gzip".equals(header.getValue())){
					continue;//for 跳出 异常处理
				}
				GZIPInputStream gzip = new GZIPInputStream(
						entity.getContent());
				long time = System.currentTimeMillis();
				Map xqBean = JacksonMapper.getInstance().readValue(gzip, Map.class);
				log.info("解析耗时="+(System.currentTimeMillis()-time));
				if(!get.isAborted())get.abort();
				Object obj = xqBean.get("list");
				List<Map> arr = new ArrayList<Map>();;
				if(obj instanceof List){
					arr = (List)obj;
				}
				for(int i = 0 ; i < arr.size();i++){
					Map jo =arr.get(i);
					String xqid = String.valueOf(jo.get("id"));
					 
					String title = String.valueOf(jo.get("title"));
					String text = String.valueOf(jo.get("text"));
					text = text.replaceAll("<[^<]*>", "");
					text = text.replaceAll("\\$", "");
					if(text.length() < 50 )continue;
					String desc = String.valueOf(jo.get("description"));
					if(s.contains(xqid)){
						 //已抓取过
						log.warn("雪球ID="+xqid+" title=["+title +"]之前抓取过");
						continue;
					}else{
						log.info("雪球ID="+xqid+" title=["+title +"]抓取写入");
						s.add(xqid);
					}
					if(text.equals(desc))continue;
					Article article = new Article();
					article.setTitle(title);
					article.setSummary(desc);
					article.setContent(text);
					//article.setTags("#"+company.getSimpileName()+":"+company.getStockCode()+"#");
					mockPublishArticle(article);
				}
				
//				雪球内容过大时，解析失败  替换使用Jackson 	甚至直接解析流中内容			
//				StringBuffer out = new StringBuffer();
//				byte[] b = new byte[4096];
//
//				File storeFile = new File("d:/xueqiu" + xqCode + "_.html");// 写到本地文件，好分析
//				FileOutputStream fos = new FileOutputStream(storeFile);
//				for (int n; (n = gzip.read(b)) != -1;) {
//					out.append(new String(b, 0, n));
//					fos.write(b, 0, n);
//				}
//
//				fos.flush();
//				fos.close();
//				get.abort();
//				
//				JSONObject jsonObject = JSONObject.fromObject(out);
//				JSONArray arr = jsonObject.getJSONArray("list");
//				for(int i = 0 ; i < arr.size();i++){
//					JSONObject jo =arr.getJSONObject(i);
//					int xqid = jo.getInt("id");
//					 
//					String title = jo.getString("title");
//					String txt = jo.getString("txt");
//					String desc = jo.getString("description");
//					if(s.contains(xqid)){
//						 //已抓取过
//						log.warn("雪球ID="+xqid+" title=["+title +"]之前抓取过");
//						continue;
//					}else{
//						log.debug("雪球ID="+xqid+" title=["+title +"]抓取写入");
//						s.add(xqid);
//					}
//					Article article = new Article();
//					article.setTitle(title);
//					article.setSummery(desc);
//					article.setContent(txt);
//					article.setTags("#"+company.getStockCode()+"#");
//					mockPublishArticle(article);
//				}
			} catch (ClientProtocolException e) {
				log.error("连接新浪微博失败" + e.toString());
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(!get.isAborted())get.abort();//关闭get
			}
			try {
				Thread.sleep(1000);//暂停5秒，此参数取配置中心
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
	}
	


	
	void start() {		
		String cookie = "";//从配置中心获取
		
//		config.getString("spider.xueqiu.run");
//		config.getLong("spider.sleep");
		cookie = "Hm_lvt_1db88642e346389874251b5a1eded6e3=1372347133,1372390148,1372513460,1372665793; __utma=1.407531373.1366019268.1372513463.1372665797.79; __utmz=1.1367550738.23.2.utmcsr=127.0.0.1:8080|utmccn=(referral)|utmcmd=referral|utmcct=/stock/index.html; pgv_pvi=1834661888; xq_a_token=8NsxKsUx1YI5DMXIpPUGtf; xq_r_token=xrIXRf5Nzz909yVKD6pK2U; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1372665793; JSESSIONID=aaajncezzSqMyd1hDqz_t; __utmb=1.1.10.1372665797; __utmc=1" ;
		cookie = ConfigCenterFactory.getString("stock_zjs.xueqiucookie",cookie);
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
		try{
			super.start();
			executeSpider(cookie);//抓取过程中也需要检查run
		}catch (Exception e) {
			log.error("抓取过程失败");
		}finally{
			serObj(SER_FILE,s);
			s.isEmpty();//置为空
		}
	}
	
//  支持带参数的任务	
//	public void execute(JobExecutionContext context)
//			throws JobExecutionException {
//		// TODO Auto-generated method stub
//		String jobName = context.getJobDetail().getFullName(); 
//		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
//		//从dataMap中获取myDescription，myValue以及myArray
//		String cookie = dataMap.getString("cookie");
//	}
}
