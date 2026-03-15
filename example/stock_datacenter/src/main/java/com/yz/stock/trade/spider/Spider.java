package com.yz.stock.trade.spider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Rss;
import com.stock.common.util.StringUtil;

public class Spider {

	static Logger log = LoggerFactory.getLogger(Spider.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    try{  
//	    	urlSpider();
	    	rssReader("http://rss.sina.com.cn/roll/finance/hot_roll.xml","新浪财经");
              
        }catch (Exception e) {  
            e.printStackTrace();  
        }  

	}

	/**
	 * 暂时从新浪取实时股价
	 * @param companyCode
	 * @return
	 */
	public static  Double getNewPrice(String companyCode) {
		Double nprice = null;
		if(companyCode==null) return null;
		String[] sa = companyCode.split("\\.");
		String url = "http://hq.sinajs.cn/list="+sa[1]+sa[0];
		String s = Spider.urlSpider(url, "gbk");
		if(!StringUtil.isEmpty(s)&&s.split(",").length>2)
			nprice = Double.valueOf(s.split(",")[1]);
		return nprice;
	}
	
	public static String urlSpider(String surl,String ecode) {
		StringBuffer buffer = new StringBuffer();
		try {
			// TODO Auto-generated method stub
//			String ecode = "UTF-8";
			//    	String ecode = "gbk";
			URL url = new URL(
					surl);
			//    	URL url = new URL("http://www.google.com.hk/search?q=万科hl=zh-CN&safe=strict&client=aff-cs-360chromium&hs=9RY&prmd=imvnsuzl&source=lnms&tbm=nws&sa=X&ei=OYKTUIeSGYmRiQea-4DACA&ved=0CA0Q_AUoBA&prmdo=1");  
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setRequestProperty("accept", "text/xml;text/html");
			conn.setRequestProperty("Content-Type", "text/html; charset="
					+ ecode);
			conn.setUseCaches(false);
			conn.setRequestProperty("User-Agent",
					"Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
			BufferedReader is = new BufferedReader(new InputStreamReader(
					conn.getInputStream(), ecode));
		
			String str;
			while ((str = is.readLine()) != null) {
				buffer.append(str);
				buffer.append("\n");

			}
			//        str = buffer.toString().replaceAll("<script(.|\n)+?</script>", "").replaceAll("<(.|\n)+?>", "").replaceAll("&nbsp;", " ");  
			//        String[] s = str.split("\n");  
			//        buffer = new StringBuffer();  
			//        for(int i=0;i<s.length;i++){  
			//            if(s[i].trim().equals("") ){  
			//                continue;  
			//            }else{  
			//                buffer.append(s[i]);  
			//                buffer.append("\n");  
			//            }  
			//        }  
			is.close();
		} catch (Exception e) {
			// TODO: handle exception
		}  
        return buffer.toString();

	}
	/**
	 * rss xml reader 
	 */
	@SuppressWarnings("rawtypes")
	public static List<Rss> rssReader(String url,String author) {
		List<Rss> rl = new ArrayList<Rss>();
		try {
			XMLConfiguration xc = new XMLConfiguration(new URL(url)
					);
			List l = xc.getList("channel.item.title");
			if(l!=null&&l.size()>0)
			{
				for(int i=0;i<l.size();i++)
				{
					Rss rss = new Rss();
					String  title = xc.getString("channel.item("+i+").title");
					rss.setTitle(title.trim());
					String  link = xc.getString("channel.item("+i+").link");
					if(link==null) continue;
					rss.setLink(link);
					String  description = xc.getString("channel.item("+i+").description");
					if(!StringUtil.isEmpty(description)) rss.setDescription(description.trim());
					String  pubDate = xc.getString("channel.item("+i+").pubDate");
					if(!StringUtil.isEmpty(pubDate)) rss.setPubDate(pubDate);
					String  guid = xc.getString("channel.item("+i+").guid");
					if(!StringUtil.isEmpty(guid)) rss.setGuid(guid);
					rss.setAuthor(author);
					rl.add(rss);
				}
			}
		} catch (Exception e) {
			log.error("rss reader failed!,url="+url,e);
		}
		return rl;
	}
}
