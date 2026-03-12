package com.yfzx.service.spider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minidev.json.JSONArray;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.stock.common.constants.StockCodes;
import com.stock.common.model.USubject;
import com.stock.common.model.share.Article;
import com.stock.common.model.user.Members;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.user.MembersService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;

/**
 * 此用于抓取雪球讨论，提供给版主使用，cookie信息使用XueqiuUserTaskProcessor中的<br>
 * 独立的存储和页面管理
 * @author wind
 *
 */
public class XueqiuTalkTaskProcessor implements ITaskProcessor {

	private static Object lockObj = new Object();
	private static String chcheName = "talkCache";
	static Logger log = LoggerFactory.getLogger(XueqiuTalkTaskProcessor.class);
	public static HashMap<Long,List<Members>> mMap = new HashMap<Long,List<Members>>();
	public static HashMap<Long,List<String>> codeMap = new HashMap<Long,List<String>>();
	static String cookie ;
	static {
		initConfig();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {				
				initConfig();
			}			
		});
	}
	
	private static void initConfig(){
		//配置中心配置	
		String config = ConfigCenterFactory.getString("spider.mockConf", "9:10005,10002:000002.sz,600660.sh;" +
				"10001:10003,10004:000001.sz,600895.sh");
		MembersService ms = MembersService.getInstance();
		HashMap<Long,List<Members>> mMapTmp = new HashMap<Long,List<Members>>();
		HashMap<Long,List<String>> codeMapTmp = new HashMap<Long,List<String>>();
		String[] uidConfigArr = config.split(";");
		for(String uidConfig : uidConfigArr){
			String[] arr = uidConfig.split(":");
			String uid = arr[0];
			String[] mockUidArr = arr[1].split(",");
			String[] mockCodeArr = arr[2].split(",");
			List<Members> tmpList = new ArrayList<Members>();
			for(int i=0;i<mockUidArr.length;i++){	
				
				try{
					long mockUid = Long.valueOf(mockUidArr[i]);
					Members mockMembers = ms.getMembers(mockUid);
					//模拟这些用户登录
					//用户登录消息注册事件
					UserMsg um = SMsgFactory
							.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_8);
					String loginUid = mockUidArr[i];
					um.setS(loginUid);
					um.setD(loginUid);
					UserEventService.getInstance().notifyTheEvent(um);					
					tmpList.add(mockMembers);
				}catch (Exception e) {
					log.error("模拟用户uid="+mockUidArr[i]+"异常");
				}
			}
			List<String> tmpCodeList = new ArrayList<String>();
			for( String mockCode : mockCodeArr ){
				USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(mockCode);
				if(usubject == null){
					continue;
				}else{
					tmpCodeList.add(mockCode);
				}
			}
			codeMapTmp.put(Long.valueOf(uid), tmpCodeList);
			mMapTmp.put(Long.valueOf(uid),tmpList );
		}
		mMap = mMapTmp;
		codeMap=codeMapTmp;
		
		String cookieStr  = ConfigCenterFactory.getString("spider.mockCookie","bid%3d8c096125860593fb8d7b69d0233910d6%5fi3qvtbt6%3b%20cube%5fdiscover%5fguide%3dtrue%3b%20xq%5fa%5ftoken%3dmgsKHil0YFRdor28redpun%3b%20xqat%3dmgsKHil0YFRdor28redpun%3b%20xq%5fr%5ftoken%3dTYZbGbAR0il76z97bg8Qcr%3b%20xq%5ftoken%5fexpire%3dFri%2520Jan%252016%25202015%252013%253A18%253A55%2520GMT%252B0800%2520%28CST%29%3b%20xq%5fis%5flogin%3d1%3b%20Hm%5flvt%5f1db88642e346389874251b5a1eded6e3%3d1420359871%2c1420423685%2c1420440507%2c1420507702%3b%20Hm%5flpvt%5f1db88642e346389874251b5a1eded6e3%3d1420532859");
		cookie = StockUtil.unescape(cookieStr);
	}
	
	public static String mockPublishTalk(long uid,String key,String newContent){
		Article article = XueqiuTalkTaskProcessor.get(key);
		if(article != null){
			Article articleTmp = article.clone(); 
			if(StringUtil.isEmpty(newContent) == false ){
				articleTmp.setContent(newContent);
				String suumary = StockSpider.toSummary(newContent);
				articleTmp.setSummary(suumary);
			}
			//内容太少，则会取消发布
			if(articleTmp.getContent().length() < 4){
				return "内容少于4个字，不允许发布";
			}
			List<Members> tmpList = mMap.get(Long.valueOf(uid));
			Members members =tmpList.get(new Random().nextInt(tmpList.size()));
			SpiderStorage.publishSpiderArticle(members, articleTmp, articleTmp.getTags());
			article.setStatus(1);
		}
		return StockCodes.SUCCESS;
	}
	
	/**
	 * 判断用户是否有权限访问
	 * @param uid
	 * @return
	 */
	public static boolean checkUidExist(Long uid){
		boolean checkExist = mMap.containsKey(uid);
		return checkExist;
	}
	
	@Override
	public void process(SpiderConfigBean spiderconfig) {
		
		Set<Long> uidSet = mMap.keySet();
		for(Long uid : uidSet){
			process(uid);
		}
	}
	public void process(Long uid){	
		int atokenStart = cookie.indexOf("xq_a_token=")+"xq_a_token=".length();
		int atokenEnd = cookie.indexOf(";", atokenStart);
		String access_token = cookie.substring(atokenStart,atokenEnd);
		//首先需要配置版主用户对应的虚拟用户和管理的公司板块
		//uid:mockuid,mockuid2:companycode,topcode;uid:mockuid,mockuid2:companycode,topcode		
		List<String> tmpCodeList = codeMap.get(uid);			
		for( String mockCode : tmpCodeList ){
			USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(mockCode);
			if(usubject == null){
				continue;
			}
			System.out.println(mockCode);
			//http://xueqiu.com/statuses/search.json?count=15&comment=0&symbol=SH600196&hl=0&source=user&sort=time&page=1&_=1421153112418
			String startUrl ="http://xueqiu.com/statuses/search.json?count=50&comment=0&symbol=";
			String endUrl = "&hl=0&source=user&sort=time&page=1&access_token="+access_token+"&_="+System.currentTimeMillis();
			String yfCode = mockCode.toUpperCase();																				 
			String xqCode = null ;
			if(yfCode.endsWith(".HK")){
				xqCode = yfCode.split("\\.")[0];
			}else{
				xqCode = yfCode.split("\\.")[1] + yfCode.split("\\.")[0];
			}
			String spiderUrl = startUrl + xqCode+ endUrl;
			String jsonContent = StockSpider.doSpiderByLoginCookie(spiderUrl,cookie);
			JSONArray titleArr = JsonPath.read(jsonContent,"$list[*].title");
			JSONArray contentArr = JsonPath.read(jsonContent,"$list[*].text");
			JSONArray keyArr =  JsonPath.read(jsonContent,"$list[*].target");
			JSONArray createArr =  JsonPath.read(jsonContent,"$list[*].created_at");
			JSONArray editedArr =  JsonPath.read(jsonContent,"$list[*].edited_at"); 
			JSONArray descArr =  JsonPath.read(jsonContent,"$list[*].description"); 
			JSONArray imgArr =  JsonPath.read(jsonContent,"$list[*].pic"); 
			for(int i=0;i<keyArr.size();i++){
				String key = "http://www.xueqiu.com"+String.valueOf(keyArr.get(i));
				if(XueqiuTalkTaskProcessor.get(key) != null){
					//已处理过的链接，忽略
					continue;
				}
				String title = String.valueOf(titleArr.get(i));
				String content = String.valueOf(contentArr.get(i));					
				String create = String.valueOf(createArr.get(i));
				String desc = String.valueOf(descArr.get(i));
				Object img = String.valueOf(imgArr.get(i));
				Object edited = editedArr.get(i);
				if(edited != null){
					create = String.valueOf(edited);
				}
				
				long time = StockSpider.getTime(create);
				if(content.contains("我刚刚关注了")){
					continue;
				}
				//去掉一些雪球的标记
				content = Jsoup.clean(content,StockSpider.myWhitelist()).replaceAll("\\$", "").replace("!thumb.jpg", "");
				desc = Jsoup.clean(desc,StockSpider.myWhitelist()).replaceAll("\\$", "");
				SpiderStorageBean ssb = new SpiderStorageBean();				
				ssb.setTitle(title);
				//ssb.setContent("#"+mockCode+":"+usubject.getName()+"#"+content);
				ssb.setContent(content);
				ssb.setKey(key);
				ssb.setTime(time);
				ssb.setSummary(desc);
				ssb.setUid(Long.valueOf(uid));	
				ssb.setTags(mockCode);
				if(StringUtil.isEmpty(String.valueOf(img)) ==false ){
					String[] oldImgArr = img.toString().replace("!thumb.jpg", "").split(",");
					StringBuilder newImgBuf = new StringBuilder();
					for(String oldImg : oldImgArr){
						newImgBuf.append("<img src=\""+oldImg+"\"></img>");
					}
					ssb.setImg(newImgBuf.toString());
				}				
				insert(ssb);
			}
		}
	}
	
	public static void insert(SpiderStorageBean ssb) {	
		if(ssb.getTime() == 0l){
			ssb.setTime(System.currentTimeMillis());
		}
		Cache cache = getCache();
		if(cache.get(ssb.getKey()) == null){ 
			Element element = new Element(ssb.getKey(),ssb);
			cache.put(element);	
		}
	}
	
	public static List<SpiderStorageBean> list(long uid,String tags,int start,int limit){
		List<SpiderStorageBean> ssbList = new ArrayList<SpiderStorageBean>();
		Cache cache = getCache();
		Attribute<Long> uidAtt = cache.getSearchAttribute("uid");
		Results rs =  null;
		if(StringUtil.isEmpty(tags)){
			rs = cache.createQuery().addCriteria(uidAtt.eq(uid)).includeKeys().addOrderBy(new Attribute<SpiderStorageBean>("time"), Direction.DESCENDING).maxResults(start+limit).execute();
		}else{
			Attribute<String> tagsAtt = cache.getSearchAttribute("tags");
			rs = cache.createQuery().addCriteria(uidAtt.eq(uid)).addCriteria(tagsAtt.eq(tags)).includeKeys().addOrderBy(new Attribute<SpiderStorageBean>("time"), Direction.DESCENDING).maxResults(start+limit).execute();
		}
		List<Result> list = rs.range(start, limit);		
		List<String> subList = new ArrayList<String>();
		for(Result result : list){
			subList.add(result.getKey().toString());
		}
		for(String key : subList){
			Element element = cache.get(key);
			Object object = element.getValue();
			SpiderStorageBean ssb = (SpiderStorageBean)object;
//			ssb.setContent("");
			ssbList.add(ssb);
		}
		return ssbList;
	}

	public static int size(long uid,String tags){
		Cache cache = getCache();
		Attribute<Long> uidAtt = cache.getSearchAttribute("uid");
		int size = 0;
		if(StringUtil.isEmpty(tags)){
			size = cache.createQuery().addCriteria(uidAtt.eq(uid)).includeKeys().execute().size();	
		}else{
			Attribute<String> tagsAtt = cache.getSearchAttribute("tags");
			size = cache.createQuery().addCriteria(uidAtt.eq(uid)).addCriteria(tagsAtt.eq(tags)).includeKeys().execute().size();	
		}	
		return size;
	}
	
	private static Cache getCache(){		
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		Cache cache = cimpl.getCacheManager().getCache(CacheUtil.getCacheName(chcheName));
		if(cache == null ){
			CacheConfiguration cacheCfg = new CacheConfiguration(CacheUtil.getCacheName(chcheName),1000000);
			cacheCfg.setEternal(true);
			Searchable searchable = new Searchable();
			searchable.addSearchAttribute(new SearchAttribute().name("time"));
			searchable.addSearchAttribute(new SearchAttribute().name("uid"));
			searchable.addSearchAttribute(new SearchAttribute().name("tags"));
			cacheCfg.addSearchable(searchable);				
			if(cache == null){
				synchronized (lockObj) {
					Cache  cacheNew = new Cache(cacheCfg);
					try{
						cimpl.getCacheManager().addCache(cacheNew);
					}catch (Exception e) {
						
					}
				}
			}					
			cache = cimpl.getCache(CacheUtil.getCacheName(chcheName));			
			fileToCache();												
		}
		return cache;
	}
	
	public static void cacheToFile(){
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		cimpl.flushToDisk(chcheName);
	}
	
	public static void fileToCache(){
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		cimpl.flushToCache(chcheName);
	}
	
	public static SpiderStorageBean get(String key){
		Cache cache = getCache();
		Element element = cache.get(key);
		if(element == null)return null;
		Object object = element.getValue();
		SpiderStorageBean ssb = (SpiderStorageBean)object;
		return ssb;
	}

	public static void delete(String key){
		Cache cache = getCache();
		cache.remove(key);
	}
	
	
}
