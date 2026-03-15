package com.yz.stock.portal.service.company.spider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.Div;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Company;
import com.stock.common.model.share.Article;
import com.stock.common.model.user.Members;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;

public class QQWBSpider extends WBSpiderFacade {
	//http://search.t.qq.com/index.php?k=%E4%B8%87%E7%A7%91A&pos=234&s_hot=1
	private static final Logger log = LoggerFactory.getLogger(QQWBSpider.class);
	Set<String> s = Collections.synchronizedSet(new HashSet<String>());
	private final static String SER_FILE = "qq.ser";
	List<Members> memberList = null;
	private static QQWBSpider instance = new QQWBSpider();
	
	public static QQWBSpider getInstance(){
		return instance;
	}
	
	/**
	 * 供Quartz调用
	 */
	public void start() {
		String cookie = "";//从配置中心获取
		cookie = "pgv_pvid=1192697680; ts_uid=2940256270; pt2gguin=o0254107347; RK=xk4KA8Pe2i; " +
				"luin=o0254107347; lskey=0001000004eba5b0e10caef8c56b25fa03fe80828049acfed1ffba4d" +
				"7c009ccff5e179c582e6d423f4b41fcb; ts_refer=xui.ptlogin2.qq.com/cgi-bin/qlogin; " +
				"o_cookie=254107347; ts_uid=2940256270; wbilang_254107347=zh_CN; wbilang_10000=zh_CN;" +
				" wb_regf=%3B0%3B%3Bsearch.t.qq.com%3B0; ts_last=/index.php; pgv_info=ssid=s471692900;" +
				" ts_sid=332951808";
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
			e.printStackTrace();
		}finally{
			serObj(SER_FILE,s);
			//s.isEmpty();//置为空
		}
	}

	//http://search.t.qq.com/index.php?k=%23%E5%90%8C%E6%B4%B2%E7%94%B5%E5%AD%90%23&pos=234&s_hot=1
		//加#搜索更准确 k	#同洲电子# 	pos	234 	s_hot	1 
	private void executeSpider(String cookie) {
		List<Company> companyArr = CompanyService.getInstance().getCompanyList();
		String startUrl = "http://search.t.qq.com/index.php?k=";
		String endUrl = "&pos=234&s_hot=1";
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
			HttpGet get = new HttpGet();
			try {
				String spiderUrl = startUrl + URLEncoder.encode("#"+companyName+"#",UTF8)+endUrl;
				log.info(spiderUrl);	
				get.setURI(URI.create(spiderUrl));
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
				InputStream gzip = new GZIPInputStream(entity.getContent());
				StringBuffer out = new StringBuffer();   
				byte[] b = new byte[4096]; 

				for (int n; (n = gzip.read(b)) != -1;) {   
				    out.append(new String(b, 0, n));  
				}
				if(!get.isAborted())get.abort();
				Parser parser = Parser.createParser(out.toString(),UTF8);
				NodeList ulList =  parser.extractAllNodesThatMatch(sFilter);
				if(ulList.size() ==0)continue;
				BulletList ul = (BulletList) ulList.elementAt(0);
				NodeList liList = ul.getChildren();
				for(int i = 0; i < liList.size(); i++){
					Node li = liList.elementAt(i);
					if((li instanceof Bullet)== false)continue;
					Bullet liNode = (Bullet) li;
					String id = liNode.getAttribute("id");
					if(s.contains(id)){
						 //已抓取过
						log.warn("QQID="+id+"之前已抓取");
						continue;
					}else{
						log.info("QQID="+id+"抓取写入");
						s.add(id);
					}
					NodeList divNodeList = liNode.getChildren().extractAllNodesThatMatch(sFilterDiv,true);
					if(divNodeList.size() == 0 ) continue;
					Div div = (Div) divNodeList.elementAt(0);
					StringBean sb = new StringBean();
					sb.setLinks(false);//设置结果中去点链接
//					sb.setURL(url);
//					sb.visitTag(div); 没看懂
					divNodeList.visitAllNodesWith(sb);
					String contentStr = sb.getStrings();
					if(contentStr.length() < 80)continue;
					Article article = new Article();
					article.setTitle("");
					article.setSummary(contentStr.substring(0, 50)+"...");
					article.setContent(contentStr);
					article.setTags("#"+company.getSimpileName()+":"+company.getStockCode()+"#");
					mockPublishArticle(article);
					
				}
				
			} catch (UnsupportedEncodingException e) {				
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(!get.isAborted())get.abort();
			}
			try {
				Thread.sleep(1000);//暂停5秒，此参数取配置中心
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
		
	}
	//对于 http://search.t.qq.com
	NodeFilter sFilter =
		new NodeFilter() {
			private static final long serialVersionUID = -1045407683800456182L;
			// 实现该方法,用以过滤标签
			public boolean accept(Node node) {					
				if(node instanceof BulletList && "talkList".equals(((BulletList)node).getAttribute("id"))){
					return true;
				}
				return false;
			}
		};
	NodeFilter sFilterDiv =
			new NodeFilter() {
				private static final long serialVersionUID = -1045407683860456182L;
				// 实现该方法,用以过滤标签
				public boolean accept(Node node) {					
					if(node instanceof Div && "msgCnt".equals(((Div)node).getAttribute("class"))){
						return true;
					}
					return false;
				}
			};
	

}
