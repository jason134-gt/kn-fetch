package com.yfzx.service.spider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.ansj.domain.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Company;
import com.stock.common.model.USubject;
import com.yfzx.service.client.es.TextSplitService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.USubjectService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.stock.portal.service.company.spider.JacksonMapper;

public class QQZxgTaskProcessor implements ITaskProcessor {

	static Logger log = LoggerFactory.getLogger(QQZxgTaskProcessor.class);
	
	@Override
	public void process(SpiderConfigBean spiderconfig) {
		List<Company> companyList = CompanyService.getInstance().getCompanyListFromCache();
		//公司太多，将公司分组成N个线程抓取，提高效率
		int threadNum = ConfigCenterFactory.getInt("spider.threadNum", 10);
		int totalSize = companyList.size();
		int oneSize = totalSize /threadNum ;
		for(int i=0;i<threadNum;i++){
			if(i != (threadNum-1) ){
				List<Company> childCompanyList = companyList.subList(i*oneSize, (i+1)*oneSize);				
				TimerTask tt =new TimerTask() {
					QQZxgTaskProcessor task;
					SpiderConfigBean spiderconfig;
					List<Company> companyList;
					
					public TimerTask set(SpiderConfigBean spiderconfig,List<Company> companyList,QQZxgTaskProcessor task){
						this.companyList = companyList;
						this.spiderconfig = spiderconfig;
						this.task = task;
						return this;
					}
					public void run() {
						task.processChild(companyList, spiderconfig);
						this.cancel();
					}
				}.set(spiderconfig,childCompanyList,this);
				Timer timer = new Timer("Sprider["+spiderconfig.getKey()+ "_"+i+"]",true);							
				timer.schedule(tt,0);	
			}else{
				List<Company> childCompanyList = companyList.subList(i*oneSize,totalSize);	
				processChild(childCompanyList, spiderconfig);				
			}
		}
	}

	public void processChild(List<Company> companyList,SpiderConfigBean spiderconfig) {
		long threadSleep = ConfigCenterFactory.getLong("spider.threadSleep", 1000l);
		long time = System.currentTimeMillis();
		for(Company company : companyList){
			String companyCode = company.getCompanyCode();
			USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(companyCode);
			if(us == null){
				continue;
			}			
			String yfCode = companyCode.toLowerCase();
			String xqCode = yfCode.split("\\.")[1] + yfCode.split("\\.")[0];
			String openid = ConfigCenterFactory.getString("spider.zxgopenid", "oA0Gbjq7KS52Fpe751OOAykW8dVU");
			String uin = ConfigCenterFactory.getString("spider.zxguin", "21275042761");
			String nickname = ConfigCenterFactory.getString("spider.zxgnickname", "拉风一点");
			String token = ConfigCenterFactory.getString("spider.zxgtoken", "OezXcEiiBSKSxW0eoylIeD8qVRb_fmdwoHVJ1kUv5VU3jR3lGpTpP9uYsvEvtYEl_uUBnCIPWTeKPbELbXyCFqVf6eYg_u-hH7G--C0qb220-2nFXdBz-7CSd4JBb-QWaTqwBAdH_z1ABgii0nJsDA");
			String url = null;
			int wxNewRss =  ConfigCenterFactory.getInt("spider.wxNewRss", 0);
			if(wxNewRss == 1){
				url = "http://group.finance.qq.com/newstockgroup/rssService/getNewRssList?_rndtime="+(System.currentTimeMillis()/1000)+"&"
						+ "_appName=ios&_dev=iPad2,5&_devId=b6b05574d2ad39ee9294db10b4af3c6230a15dd7&_appver=3.8.0"
						+ "&_ifChId=&_isChId=1&_osVer=8.0&_uin="+uin+"&_wxuin="+uin;
			}else{
				url = "http://group.finance.qq.com/newstockgroup/rssService/getRssList?check=6&_rndtime="+(System.currentTimeMillis()/1000)+"&"
						+ "_appName=ios&_dev=iPad2,5&_devId=b6b05574d2ad39ee9294db10b4af3c6230a15dd7&_appver=3.8.0"
						+ "&_ifChId=&_isChId=1&_osVer=8.0&_uin="+uin+"&_wxuin="+uin;
			}
			HashMap<String,String> postData = new HashMap<String,String>();
			postData.put("limit", "10");
			postData.put("begin", "-1");
			postData.put("code", xqCode);
			postData.put("stock_id", xqCode);
			postData.put("openid", openid);
			postData.put("token", token);
			postData.put("nickname", nickname);
			postData.put("picture", openid);
			HashMap<String,String> header = new HashMap<String,String>();
			header.put("Referer", "http://zixuanguapp.finance.qq.com");
			header.put("Accept-Encoding","gzip");
			header.put("User-Agent","Portfolio/3.7.0");
			header.put("Host","group.finance.qq.com");
			header.put("Connection","Keep-Alive");
			header.put("Content-Type","application/x-www-form-urlencoded");
			try {
				String jsonContent = new HttpRequestProxy().doRequest(url, postData, header, "utf-8");
				Map<String,Object> map = JacksonMapper.getInstance().readValue(jsonContent, Map.class);
				ArrayList<Map<String,Object>> reList = new ArrayList<Map<String,Object>>();
				selectContent(map,reList);
				for(Map<String,Object> reMap : reList){
					String content = String.valueOf(reMap.get("content"));
					content = content.replaceAll("\\[[^\\[]*\\]", "").replace(" ", "");
					if(content.contains("重组")){
						
					}else if(content.contains("内幕")){
						
					}else{
						continue;
					}
//					List<Term> tList =TextSplitService.getInstance().splitWordFromContent(content);
//					for(Term t : tList){
//						System.out.println(t);
//					}
					String created_at = String.valueOf(reMap.get("created_at"));
					String key = null;
					Object comment_id_Obj = reMap.get("comment_id");
					if(comment_id_Obj != null){
						key = String.valueOf(comment_id_Obj);
					}else{
						Object subject_id_Obj = reMap.get("subject_id");
						key = String.valueOf(subject_id_Obj);
					}
					String timeStr = created_at.substring(0, 19).replace("T", " ");
					long timeContent = StockSpider.getTime(timeStr);
					if(timeContent - time >  (1000l*3600*24) ){
						continue;
					}
					SpiderStorageBean ssb = new SpiderStorageBean();
					String newContent = ("#"+us.getUidentify()+":"+us.getName()+"#"+content).substring(0, 140);
					ssb.setTitle(null);
					ssb.setContent(newContent);
					ssb.setKey(key);
					ssb.setTime(timeContent);
					ssb.setSummary(newContent);
					ssb.setUid(9);
					ssb.setTags(companyCode);
					ssb.setSource_url("腾讯自选股");
					SpiderStorage.insertNoPublish(ssb);	
				}//end for reList
				Thread.sleep(threadSleep);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void selectContent(Map<String,Object> map,ArrayList<Map<String,Object>> reList){
		Set<String> keySet = map.keySet();
		if(keySet.contains("content")){
			reList.add(map);
			return;
		}
		for(String key: keySet){
			Object obj = map.get(key);
			if(obj instanceof Map){
				Map<String,Object> cMap = (Map<String,Object>)obj;
				selectContent(cMap,reList);
			}else if(obj instanceof List){
				List<Object> oList = (List<Object>) obj;
				for(Object c2Obj : oList){
					if(c2Obj instanceof Map){
						Map<String,Object> c2Map = (Map<String,Object>)c2Obj;
						selectContent(c2Map,reList);
					}
				}
			}
		}
	}
}
