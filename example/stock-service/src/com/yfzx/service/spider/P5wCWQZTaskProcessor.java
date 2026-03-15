package com.yfzx.service.spider;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Company;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;

/**
 * 传闻求证
 * @author wind
 *
 */
public class P5wCWQZTaskProcessor implements ITaskProcessor {
	
	static Logger log = LoggerFactory.getLogger(P5wCWQZTaskProcessor.class);
	private final static String BASE_KEY = "http://www.p5w.net/stock/cwqz/";
	private final static String CWQZ_Y_URL = "y/";
	private final static String CWQZ_N_URL = "n/";
	private final static Pattern p = Pattern.compile("[0-9]{4}[-/][0-9]{1,2}[-/][0-9]{1,2}");
	
	@Override
	public void process(SpiderConfigBean spiderconfig) {
		Document doc;
		String url = BASE_KEY+CWQZ_Y_URL;
		try {
			
			doc = Jsoup.connect(url).timeout(10000).get();
			exeDoc(doc);
		}catch (Exception e) {
			log.error("抓取传闻求证异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
		}
		url = BASE_KEY+CWQZ_N_URL;
		try {
			doc = Jsoup.connect(url).timeout(10000).get();
			exeDoc(doc);
		}catch (Exception e) {
			log.error("抓取传闻求证异常"+url+"\r\n"+e.fillInStackTrace()+"\r\n"+ e.getStackTrace()[0]);
		}
	}
	
	private void exeDoc(Document doc){
		Elements elements = doc.select(".one");
		for(int i=0;i<elements.size();i++){
			Element element = elements.get(i);
			String dawen = element.select(".da_wen").text();
			String sDateStr = null;
			Matcher m = p.matcher(dawen);
			//获取出时间串
			while (m.find()) {
				if (StringUtil.isEmpty(m.group()) == false ) {				
					sDateStr = m.group().replace("/", "-").trim();
					break;
				}
			}
			if(DateUtil.getSysDateYYYYMMDD(new Date(System.currentTimeMillis()-3600000l*24)).equals(sDateStr)){
				SpiderStorageBean ssb = new SpiderStorageBean();
				String companyCode = element.select(".logo").select("a").get(0).text();
				if(companyCode.startsWith("0")||companyCode.startsWith("2")||companyCode.startsWith("3")){
					companyCode = companyCode+".sz";
				}else{
					companyCode = companyCode+".sh";
				}
				Company company =CompanyService.getInstance().getCompanyByCodeFromCache(companyCode);
				if(company != null){
					String wen = element.select(".wen>.shang").text();
					String title = "传闻求证:"+company.getSimpileName()+"("+companyCode+")"+ sDateStr;
					String content = "传闻："+wen+"<br>证实："+dawen+"<br>资讯来源：全景网";
					String summary = content.replaceAll("<br/?>", " ").replaceAll("<[^>]+>","");//"证实："+dawen;
					summary = StringUtil.getMaxString(summary, 140);
					String key = BASE_KEY+"?code="+companyCode+"&time="+sDateStr;
					ssb.setTags(companyCode);
					ssb.setContent(content);
					ssb.setSummary(summary);
					ssb.setKey(key);
					ssb.setTitle(title);
					ssb.setPutLevel("D");
					ssb.putAttr("s", "传闻求证");
					SpiderStorage.insert(ssb);
				}
			}
			
		}
	}
	
}
