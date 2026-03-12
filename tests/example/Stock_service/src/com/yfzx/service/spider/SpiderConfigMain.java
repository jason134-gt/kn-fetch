package com.yfzx.service.spider;

import java.util.HashMap;
import java.util.Random;

import com.stock.common.util.StockUtil;
import com.yz.stock.portal.service.company.spider.JacksonMapper;


public class SpiderConfigMain {

//	public static void main(String[] args) {
//		new GithubRepoPageProcessor().getSite().setCharset("Utf-8");
//	    Spider.create(new GithubRepoPageProcessor())
//	            //从https://github.com/code4craft开始抓    
//	            .addUrl("https://github.com/code4craft")
//	            //设置Scheduler，使用Redis来管理URL队列
//	            .setScheduler(new RedisScheduler("localhost"))
//	            //设置Pipeline，将结果以json方式保存到文件
//	            .addPipeline(new JsonFilePipeline("D:\\data\\webmagic"))
//	            //开启5个线程同时执行
//	            .thread(5)
//	            //启动爬虫
//	            .run();
//	}
	
	public static void main(String[] args) {
		HashMap<String,SpiderConfigBean> configMap = new HashMap<String,SpiderConfigBean>();
//		try{
//			SpiderConfigBean scb = new SpiderConfigBean();
//			scb.setKey("qq");
//			scb.setDesc("腾讯股票首页");
//			scb.setEncode("utf-8");
//			scb.setStartType("0");
//			scb.setChildLevel("2");
//			scb.setType("html");
//			//scb.setStartUrl("http://finance.qq.com/stock/");
//			String startUrl = "http://finance.qq.com/stock/";
//			scb.setStartUrl(startUrl);
//			scb.setSelectUrl("#newsList .Q-hot a");
//			scb.setUrlRegex("^http://finance.qq.com/a/[0-9]+/[0-9]+.htm$");
//			HashMap<String,String> parMap = new HashMap<String,String>();
//			parMap.put("title", "#C-Main-Article-QQ .hd h1");
//			parMap.put("content", "#Cnt-Main-Article-QQ");
//			parMap.put("time", ".pubTime");
//			scb.setValueMap(parMap);
//			configMap.put(scb.getKey(), scb);			
//		}catch (Exception e) {			
//		}
		//TODO 增加几大门户 和 和讯 东方财富
		//公告信息  全景网 巨潮资讯网
		try{			
			SpiderConfigBean scb = new SpiderConfigBean();
			scb.setKey("CninfoNoticeTaskProcessor");
			scb.setDesc("巨潮资讯网公告");
			scb.setCookie("");
			scb.setEncode("utf-8");
			scb.setStartType("CninfoNoticeTaskProcessor");			
			configMap.put(scb.getKey(), scb);
		}catch (Exception e) {			
		}	
		try{			
			SpiderConfigBean scb = new SpiderConfigBean();
			scb.setKey("EastmoneyCompanyTaskProcessor");
			scb.setDesc("东方财富公司新闻");
			scb.setCookie("");
			scb.setEncode("utf-8");
			scb.setStartType("EastmoneyCompanyTaskProcessor");			
			configMap.put(scb.getKey(), scb);
		}catch (Exception e) {			
		}
//		try{			
//			SpiderConfigBean scb = new SpiderConfigBean();
//			scb.setKey("Eastmoney2");
//			scb.setDesc("假的");
//			scb.setCookie("");
//			scb.setEncode("utf-8");
//			scb.setStartType("Eastmoney2");			
//			configMap.put(scb.getKey(), scb);
//		}catch (Exception e) {			
//		}
		
//		try{
//			//网易抓新闻和公告
//			SpiderConfigBean scb = new SpiderConfigBean();
//			scb.setKey("163company");
//			scb.setDesc("网易股票公司页");
//			scb.setEncode("utf-8");
//			scb.setStartType("1");
//			scb.setChildLevel("2");
//			scb.setType("html");
//			String startUrl = "http://quotes.money.163.com/#if(${type}=='sz')1#elseif(${type}=='sh')0#end\r\n${num}.html";
//			scb.setStartUrl(startUrl);
//			scb.setSelectUrl(".tip_box_wrapper ul.disc_list>li>a");
//			scb.setUrlRegex("^http://money.163.com/1[0-9]+/[0-9]+/[0-9]+/[0-9a-zA-Z]+.html$");
//			HashMap<String,String> parMap = new HashMap<String,String>();
//			parMap.put("time",".ep-info div.left,span.info");
//			parMap.put("title", "#h1title");
//			parMap.put("content", "#endText");
//			scb.setValueMap(parMap);
//			configMap.put(scb.getKey(), scb);	
////			VelocityContext context = new VelocityContext();
////			context.put("num", "000002");
////			context.put("type", "sh");
////			CharArrayWriter writer = new CharArrayWriter();  
////			Velocity.evaluate(context, writer, "", "http://quotes.money.163.com/#if($type=='sz')1#elseif($type=='sh')0#end\r\n${num}.html");
////			String str = writer.toString();
//			//System.out.println(str);
//		}catch (Exception e) {		
//			e.printStackTrace();
//		}
//		try{
//			SpiderConfigBean scb = new SpiderConfigBean();
//			scb.setKey("xueqiu");
//			scb.setDesc("雪球公司热门");
//			String cookie2 = "Hm_lvt_1db88642e346389874251b5a1eded6e3=1405340784,1405405703,1405430561,1405492871; __utma=1.1927254516.1403582354.1405430559.1405492871.10; __utmz=1.1403582354.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); xq_a_token=xuuTXfRrmxqvbxT2hyjT83; xq_r_token=yEnNBBXHP6zpsV0wVQwfDz; xqat=xuuTXfRrmxqvbxT2hyjT83; xq_token_expire=Sun%20Aug%2010%202014%2014%3A41%3A17%20GMT%2B0800%20%28CST%29; bid=8c096125860593fb8d7b69d0233910d6_hwyl2ngu; xq_im_active=true; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1405492889; __utmb=1.2.10.1405492871; __utmc=1; xq_is_login=1";
//			scb.setCookie(cookie2);
//			scb.setEncode("utf-8");
//			scb.setStartType("1");
//			scb.setChildLevel("1");
//			scb.setType("json");
//			String startUrl = "http://xueqiu.com/statuses/search.json?count=15&comment=0&symbol=${type}${num}&hl=0&source=all&sort=alpha&page=1&_=1404293775280";
//			scb.setStartUrl(startUrl);			
//			HashMap<String,String> parMap = new HashMap<String,String>();
//			parMap.put("key", "$.list[*].target");
//			parMap.put("time", "$.list[*].created_at");
//			parMap.put("title", "$.list[*].title");			
//			parMap.put("content", "$.list[*].text");
//			scb.setValueMap(parMap);
//			configMap.put(scb.getKey(), scb);
//		}catch (Exception e) {		
//			e.printStackTrace();
//		}
		
//		try{
//			SpiderConfigBean scb = new SpiderConfigBean();
//			scb.setKey("XueqiuTodayTaskProcessor");
//			scb.setDesc("雪球今日话题");
//			String startUrl = "http://api.xueqiu.com/tips/topic/current.json?size=5&callback=jQuery18305470844250407456_1405650739887&_=1405652128538";
//			String cookie2 = "Hm_lvt_1db88642e346389874251b5a1eded6e3=1405340784,1405405703,1405430561,1405492871; __utma=1.1927254516.1403582354.1405430559.1405492871.10; __utmz=1.1403582354.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); xq_a_token=xuuTXfRrmxqvbxT2hyjT83; xq_r_token=yEnNBBXHP6zpsV0wVQwfDz; xqat=xuuTXfRrmxqvbxT2hyjT83; xq_token_expire=Sun%20Aug%2010%202014%2014%3A41%3A17%20GMT%2B0800%20%28CST%29; bid=8c096125860593fb8d7b69d0233910d6_hwyl2ngu; xq_im_active=true; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1405492889; __utmb=1.2.10.1405492871; __utmc=1; xq_is_login=1";
//			scb.setCookie(cookie2);
//			scb.setEncode("utf-8");
//			scb.setStartType("xueqiuToday");
//			scb.setStartUrl(startUrl);			
//			HashMap<String,String> parMap = new HashMap<String,String>();			
//			parMap.put("time", ".createTime");
//			parMap.put("title", ".detail h3");			
//			parMap.put("content", ".detail .statusContent");
//			scb.setValueMap(parMap);
//			configMap.put(scb.getKey(), scb);
//			
//		}catch (Exception e) {
//			// TODO: handle exception
//		}
		
//		try{
//			SpiderConfigBean scb = new SpiderConfigBean();
//			scb.setKey("xueqiuTalk");
//			scb.setDesc("雪球访谈");
//			String cookie2 = ("Hm_lvt_1db88642e346389874251b5a1eded6e3=1403582354,1403939060,1404293743,1404356833; __utma=1.1927254516.1403582354.1404293743.1404356833.5; __utmz=1.1403582354.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); xq_a_token=8awMNYIE7natuD6t93TOui; xq_r_token=spgDe6n8qvixUa9qzcNfkC; xqat=8awMNYIE7natuD6t93TOui; xq_token_expire=Wed%20Jul%2023%202014%2015%3A04%3A29%20GMT%2B0800%20(CST); xq_is_login=1; bid=8c096125860593fb8d7b69d0233910d6_hwyl2ngu; xq_im_active=true; __utmc=1; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1404356833");
//			scb.setCookie(cookie2);
//			scb.setEncode("utf-8");
//			scb.setStartType("0");
//			scb.setChildLevel("2");
//			scb.setType("html");
//			String startUrl = "http://xueqiu.com/talks/all";
//			scb.setStartUrl(startUrl);	
//			scb.setSelectUrl(".history ul#backwordList>li>a ");
//			scb.setUrlRegex("^http://xueqiu.com/talks/item/[0-9]+$");					
//			HashMap<String,String> parMap = new HashMap<String,String>();			
//			parMap.put("title", ".topic .cont h3");			
//			parMap.put("content", ".interviewLists .content");
//			scb.setValueMap(parMap);
//			configMap.put(scb.getKey(), scb);
//		}catch (Exception e) {		
//			e.printStackTrace();
//		}
		
//		try{
//			SpiderConfigBean scb = new SpiderConfigBean();
//			scb.setKey("p5w");
//			scb.setDesc("传闻求证");
//			scb.setEncode("utf-8");
//			scb.setStartType("0");
//			scb.setChildLevel("1");
//			scb.setType("html");
//			String startUrl = "http://www.p5w.net/stock/cwqz/y/";
//			scb.setStartUrl(startUrl);			
//			HashMap<String,String> parMap = new HashMap<String,String>();
//			//parMap.put("key", "$.list[*].target");
//			parMap.put("title", ".logo");			
//			parMap.put("content", ".logo,.wen");
//			scb.setValueMap(parMap);
//			configMap.put(scb.getKey(), scb);
//		}catch (Exception e) {		
//			e.printStackTrace();
//		}
		try{
			SpiderConfigBean scb = new SpiderConfigBean();
			scb.setKey("XueqiuUserTaskProcessor");
			scb.setDesc("雪球用户抓取");
			String cookie2 = "xq_a_token=IUkytDmbCaz82ipso9DyRH; xqat=IUkytDmbCaz82ipso9DyRH; xq_r_token=zscZyAVHBQJpJvgYbdbzVC; xq_token_expire=Sun%20Oct%2012%202014%2014%3A25%3A13%20GMT%2B0800%20(CST); xq_is_login=1; bid=8c096125860593fb8d7b69d0233910d6_i06ac4zy; _gat=1; __utma=1.1554215390.1411443194.1411443194.1411443194.1; __utmb=1.1.10.1411443194; __utmc=1; __utmz=1.1411443194.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); Hm_lvt_1db88642e346389874251b5a1eded6e3=1411111908,1411349352,1411389798,1411435662; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1411443194; xq_im_active=true";
			scb.setCookie(cookie2);
			scb.setEncode("utf-8");
			scb.setStartType("XueqiuUserTaskProcessor");			
			configMap.put(scb.getKey(), scb);
			
		}catch (Exception e) {
			// TODO: handle exception
		}
			
		try{
			SpiderConfigBean scb = new SpiderConfigBean();
			scb.setKey("QQCompanyTaskProcessor");
			scb.setDesc("腾讯公司页新闻");
			scb.setEncode("utf-8");
			scb.setStartType("QQCompanyTaskProcessor");
			scb.setChildLevel("2");
			scb.setType("html");
			String startUrl = "http://news2.gtimg.cn/lishinews.php?name=finance_news&symbol=${type}${num}&page=1&_du_r_t=";//+ new Random(1).nextFloat();//0.6021240734188649
			scb.setStartUrl(startUrl);
			scb.setSelectUrl("table.new-table tr>th>a");
			scb.setUrlRegex("^http://finance.qq.com/a/[0-9]+/[0-9]+.htm$");
			HashMap<String,String> parMap = new HashMap<String,String>();
			parMap.put("title", "#C-Main-Article-QQ .hd h1");
			parMap.put("content", "#Cnt-Main-Article-QQ");
			parMap.put("time", ".pubTime");
			scb.setValueMap(parMap);
			configMap.put(scb.getKey(), scb);
		}catch (Exception e) {		
			e.printStackTrace();
		}
		
		try{
			SpiderConfigBean scb = new SpiderConfigBean();
			scb.setKey("WeiboUserTaskProcessor");
			scb.setDesc("微博用户抓取");
			String cookie = "SINAGLOBAL=8637536298483.61.1395977580458; __utma=15428400.83881725.1403684708.1403684708.1403684708.1; __utmz=15428400.1403684708.1.1.utmcsr=weibo.com|utmccn=(referral)|utmcmd=referral|utmcct=/find/s; __gads=ID=a5fc11bda42a0bad:T=1405049301:S=ALNI_MYwXjcjXbNqzOlSxdpFi8woOdmBsQ; wvr=5; SUS=SID-3963588857-1412041425-GZ-lep83-bf6f2bb9fa4abff20bc34912abbe8d20; SUE=es%3De6e49fd85ddd8e55d12536e7790624df%26ev%3Dv1%26es2%3Dbc66ac91b00e18dce1be30cbca76e7d1%26rs0%3DwYCRcypIZolkp8Ta3txColxfENodDL45sp0eb1rYGb4GFbU6vuJV%252B%252F3v09OEfbBoNjbic3Ic0IP1RYM08tv93x02K16jzemEgGU5GWUJC4ijkmQzLdg6HCOkXXHCc0hSAOQQWz0R987dmt%252BSdCktqxHfoS9dvMQ7CKbqO8BrZJo%253D%26rv%3D0; SUP=cv%3D1%26bt%3D1412041425%26et%3D1412127825%26d%3Dc909%26i%3D8d20%26us%3D1%26vf%3D0%26vt%3D0%26ac%3D0%26st%3D0%26uid%3D3963588857%26name%3Dadmin%2540igushuo.com%26nick%3D%25E7%2588%25B1%25E8%2582%25A1%25E8%25AF%25B4%26fmp%3D%26lcp%3D2014-01-02%252009%253A13%253A37; SUB=_2AkMjdoXma8NlrABYnvsQxGPqaYpH-jyQpUsQAn7uJhIyGxgv7m9TqSVmXqcIBYNW1PgmjxNwCkGJR9hZ8Q..; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WWz3xCHI1bkXZzUuchcn6q95JpX5KMt; ALF=1443577421; SSOLoginState=1412041425; YF-V5-G0=7a7738669dbd9095bf06898e71d6256d; YF-Ugrow-G0=bcf9d6eeb05f5fa151daa66289455f9b; _s_tentry=login.sina.com.cn; UOR=,,login.sina.com.cn; Apache=8446355236228.556.1412041477059; ULV=1412041477064:114:22:3:8446355236228.556.1412041477059:1411961741414; YF-Page-G0=ee5462a7ca7a278058fd1807a910bc74";
			scb.setCookie(cookie);
			scb.setEncode("utf-8");
			scb.setStartType("WeiboUserTaskProcessor");
			configMap.put(scb.getKey(), scb);
		}catch (Exception e) {		
			e.printStackTrace();
		}
		
		try{
			String configStr = JacksonMapper.getInstance().writeValueAsString(configMap);
			System.out.println();
			System.out.print(StockUtil.escape(configStr));
			System.out.println();
		}catch (Exception e) {
			
		}
		
		
//			String content = "<script>for(var i=0;i<c.length;i++){</script><div id='fin_kline_mod' class='fin_kline_mod' bosszone='financeKline'>";
//			content += "<div class='inner_box'>      \r\n               ";
//			content += "</div>";
//			content += "<!--[if !IE]>|xGv00|deddd684786bf7623f5de7aea<47145af<![endif]-->";
//			content += "</div></div><p style=\"TEXT-INDENT: 2em\">券商股午后直线拉升，截至发稿，板块整体上涨逾1%，个股方面</p><!--[if !IE]>|xGv00|deddd684786bf7623f5de7aea<47145af<![endif]--><script>for(var i=0;i<c.length;i++){</script>";
//			String desc = content.replaceAll("<script[^<]*>.*</script>", "").replaceAll("<!--[^<]*>[^(-->)]*<![^<]*-->", "").replaceAll("<[^<]*>", "").replace(" ", "").replace("\r\n", "").replace("\r", "").replace("\n", "").replace("\t", "").replace("&nsbp;", "");
//			System.out.println(desc);
		
	}	

}
