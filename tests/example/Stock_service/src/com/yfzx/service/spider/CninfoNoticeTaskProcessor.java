package com.yfzx.service.spider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minidev.json.JSONArray;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.stock.common.util.DateUtil;
import com.yz.configcenter.ConfigCenterFactory;

public class CninfoNoticeTaskProcessor  implements ITaskProcessor {
	static Logger log = LoggerFactory.getLogger(CninfoNoticeTaskProcessor.class);
	private final static String CNINFO_HOST = "http://www.cninfo.com.cn";
	HashMap<String,String> retryUrl = new HashMap<String,String>();
	
	@Override
	public void process(SpiderConfigBean spiderconfig) {
		//临时公告 JS方式
		//http://www.cninfo.com.cn/disclosure/qtgg/plate/shjysgg_3m.js?ver=201409301422
		//http://www.cninfo.com.cn/disclosure/qtgg/plate/szjysgg_3m.js?ver=201409301418
		String url2 = "http://www.cninfo.com.cn/disclosure/qtgg/plate/shjysgg_3m.js?ver="+DateUtil.getSysDate("yyyyMMddHHmm");
		tmpReportByJs(url2);
		String url3 = "http://www.cninfo.com.cn/disclosure/qtgg/plate/szjysgg_3m.js?ver="+DateUtil.getSysDate("yyyyMMddHHmm");
		tmpReportByJs(url3);
		//http://www.cninfo.com.cn/disclosure/fulltext/plate/szselatest_24h.js?ver=201409221644		
//		String url = "http://www.cninfo.com.cn/disclosure/fulltext/plate/szselatest_24h.js?ver=";
//		url += DateUtil.getSysDate("yyyyMMddHHmm");
		List<Document> docList = new ArrayList<Document>();
		String startUrl = "http://www.cninfo.com.cn/search/search.jsp";
		//每天公告可能很多页 检查总共多少页
		//.sabrosus2>span:last
		String pageSize = "1";
		try {
			Document doc = Jsoup.connect(startUrl).timeout(10000).get();	
			Elements pageElements = doc.select(".sabrosus2>span");
			if(pageElements !=null && pageElements.size() >0){
				pageSize = pageElements.last().text();
				docList.add(doc);
			}
		} catch (Exception e) {			
			e.printStackTrace();
		}
		//http://www.cninfo.com.cn/search/search.jsp?keyword=&marketType=&noticeType=&orderby=date11&pageNo=2&startTime=2014-09-23
		//http://www.cninfo.com.cn/search/search.jsp?pageNo=2
		int size = Integer.parseInt(pageSize);
		for(int i=2;i<=size;i++){
			String tmpUrl = startUrl + "?pageNo="+i	;
			try {
				Document doc = Jsoup.connect(tmpUrl).timeout(10000).get();	
				docList.add(doc);
			} catch (IOException e) {
				log.error("异常"+tmpUrl+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}
		}		
		
		//倒序排列 因为发布公告的时间还是不准
		for(int i=docList.size()-1;i>=0;i--){
			Document doc = docList.get(i);
			try{
				doDocument(doc);
			}catch (Exception e) {
				retryUrl.put(doc.baseUri(), i+"");
			}
		}
		
		
		exeRetry(spiderconfig);
	}
	
	private void doDocument(Document doc){
		Elements items = doc.select(".da_tbl .da_tbl tr");
		int size = items.size();
		for(int i=size-1;i>=0;i--){
			Element e = items.get(i);
			String code = e.select(".dm").text();
			Elements link = e.select(".qsgg a");
			String linkTitle = link.text();
			String linkUrl = CNINFO_HOST+link.attr("href");
			String timeStr = e.select(".ggsj").html().trim().replace("&nbsp;", "").replaceAll("\\s", "");
			SpiderStorageBean ssb = new SpiderStorageBean();				
			//ssb.setTitle(linkTitle+" 公告时间:"+timeStr);
			ssb.setTitle(linkTitle);
			ssb.setContent(linkTitle+" 公告时间:"+timeStr+"。\r\n 公告网址:<a href='"+linkUrl+"' target='new'>" + linkUrl+"</a>");
			ssb.setSummary(linkTitle+" 公告时间:"+timeStr+"。公告网址:<a href='"+linkUrl+"' target='new'>" + linkUrl+"</a>");
			//万 科A等获取不到，需要以后调整成关键字过滤方式
//			List<TagBean> tgList = SpiderStorage.getTags(linkTitle.replace(" ", ""));
			String companycode = null;
			//A股规则
			if(code.startsWith("0") == true || code.startsWith("3")){
				companycode = code + ".sz";
			}else if(code.startsWith("6") == true || code.startsWith("9")){
				companycode = code + ".sh";
			}
			
			if(companycode == null ){
				log.error("公司查不到"+code+" " + linkTitle);
			}else{
//				String companycode = tgList.get(0).getTag().split(":")[0];					
				ssb.setTags(companycode);
				String putLevel = ConfigCenterFactory.getString("spider.CninfoLevel", "C");
				if("C".equals(putLevel)){
					ssb.setPutLevel("C");
				}
				ssb.setTime(System.currentTimeMillis());
				ssb.setKey(linkUrl);
				SpiderStorage.insert(ssb);	
			}
		}
	}
	
	private void exeRetry(SpiderConfigBean spiderconfig){
		if(retryUrl.isEmpty() == true){
			return;
		}
		for(String url :retryUrl.keySet()){
			try {
				Document doc = Jsoup.connect(url).timeout(10000).get();
				doDocument(doc);
			} catch (IOException e) {
				log.error("异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
			}	
		}
		retryUrl.clear();	
	}
	
	/**
	 * 临时公告JS格式
	 */
	private void tmpReportByJs(String url2) {
		long time = System.currentTimeMillis();
//		String url2 = "http://www.cninfo.com.cn/disclosure/qtgg/plate/shjysgg_3m.js?ver="+DateUtil.getSysDate("yyyyMMddHHmm");
		String jsonContent;
		try {
			jsonContent = StockSpider.doSpiderByLoginCookie(url2, "","gbk");
			jsonContent = jsonContent.replace("var szzbAffiches=", "").replace(";", "");
			JSONArray rArr = JsonPath.read(jsonContent,"$[*]");
			for(int i=0;i<rArr.size();i++){
				JSONArray cArr = (JSONArray)rArr.get(i);
				String urlStr = String.valueOf(cArr.get(1));
				String titleStr = String.valueOf(cArr.get(2));
				String datetimeStr = String.valueOf(cArr.get(6)).trim().replace("&nbsp;", "").replaceAll("\\s", "");
				long datetime = StockSpider.getTime(datetimeStr);
				//只处理前面的几条
				if(time-datetime > 1000*3600*24){
					break;
				}
				//万 科A等获取不到，需要以后调整成关键字过滤方式
				List<TagBean> tgList = SpiderStorage.getTags(titleStr);
				if(tgList ==null || tgList.size() ==0){
					break;
				}
				SpiderStorageBean ssb = new SpiderStorageBean();				
//				ssb.setTitle(titleStr+" 公告时间"+datetimeStr);
				ssb.setTitle(titleStr);
				ssb.setContent(titleStr+" 公告时间"+datetimeStr+"。\r\n 公告网址:<a href='"+urlStr+"' target='new'>" + urlStr+"</a>");
				ssb.setSummary(titleStr+" 公告时间"+datetimeStr+"。公告网址:<a href='"+urlStr+"' target='new'>" + urlStr+"</a>");
				//降级只发博文
				ssb.setTags(tgList.get(0).getTag().split(":")[0]);				
				ssb.setTime(datetime);
				ssb.setKey(urlStr);	
				String putLevel = ConfigCenterFactory.getString("spider.CninfoLevel", "C");
				if("C".equals(putLevel)){
					ssb.setPutLevel("C");
				}
				SpiderStorage.insert(ssb);					
			}			
		} catch (Exception e) {
			log.error("临时公告获取失败",e);
		}
	}

}
