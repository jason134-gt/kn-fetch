package com.yfzx.service.spider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.sf.ehcache.search.expression.LessThan;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.stock.common.constants.ShareConst;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.model.chance.ChanceCategory;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.Comment;
import com.stock.common.model.share.SimpleArticle;
import com.stock.common.model.user.Members;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.DateUtil;
import com.stock.common.util.LogSvr;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.chance.ChanceCategoryService;
import com.yfzx.service.client.RemindServiceClient;
import com.yfzx.service.db.MessageCenterSerivce;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.TopicService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.user.MembersService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.ViewpointService;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.util.BaseUtil;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.stock.portal.service.company.spider.JacksonMapper;

/**
 * 模拟公司短博文或公司机会评论
 * @author wind
 *
 */
public class TalkMockService {

	private final static Logger log = LoggerFactory.getLogger(TalkMockService.class);

	private static TalkMockService instance = new TalkMockService();
	//UidCompany 里面包括 栏目^公司代码^uuid
	public static HashMap<Long,UidCompany> uidCompanyMap = new HashMap<Long,UidCompany>();
	public static HashMap<Long,List<String>> uidCompanyTmpListMap = new HashMap<Long,List<String>>();
//	private static HashMap<String,Long> companyUidMap = new HashMap<String,Long>();
	private static List<Members> mockUserList = new ArrayList<Members>();
	private static String chcheName = "talkCache";
	private static String cookieXueqiu ;

	private final static Pattern imgPattern = Pattern.compile("<img.*src=(.*?)[^>]*?>",Pattern.CASE_INSENSITIVE);
//	private final static List<Pattern> checkPatternList = new ArrayList<Pattern>();

//	private static HashMap<String,String> companyLanmuMap = new HashMap<String,String>();
//	private static HashMap<String,String> companyChanceUUIDMap = new HashMap<String,String>();

	private static Object lockObj = new Object();
	private TalkMockService(){
	}

	public static TalkMockService getInstance(){
		return instance;
	}

	static {
		initConfig();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				initConfig();
			}
		});
	}


	/**
	 * 清理旧的数据，提供给刷新任务或者接收到新行情接口触发
	 */
	public void clear(){
		synchronized (lockObj) {
			log.info("TalkMockService清理旧数据");
			uidCompanyMap = new HashMap<Long,UidCompany>();
//			companyUidMap = new HashMap<String,Long>();
//			companyLanmuMap = new HashMap<String,String>();
//			companyChanceUUIDMap = new HashMap<String,String>();
			clearNoToday();
		}
	}

	public String getLanmu(String code){
//		return companyLanmuMap.get(code);
		return null;
	}

	public void clearNoToday(){
		int hourNum = ConfigCenterFactory.getInt("spider.clearHourNum", 24);
		Cache cache = getCache();
		Results rs = cache.createQuery().includeKeys().addCriteria(new LessThan("time",Long.valueOf(System.currentTimeMillis()-hourNum*3600*1000))).execute();
		List<Result> list = rs.all();
		for(Result result : list){
			cache.remove(result.getKey());
		}
	}

	private static void initConfig(){
		String cookieStr  = ConfigCenterFactory.getString("spider.mockCookie","bid%3d8c096125860593fb8d7b69d0233910d6%5fi3qvtbt6%3b%20cube%5fdiscover%5fguide%3dtrue%3b%20xq%5fa%5ftoken%3dmgsKHil0YFRdor28redpun%3b%20xqat%3dmgsKHil0YFRdor28redpun%3b%20xq%5fr%5ftoken%3dTYZbGbAR0il76z97bg8Qcr%3b%20xq%5ftoken%5fexpire%3dFri%2520Jan%252016%25202015%252013%253A18%253A55%2520GMT%252B0800%2520%28CST%29%3b%20xq%5fis%5flogin%3d1%3b%20Hm%5flvt%5f1db88642e346389874251b5a1eded6e3%3d1420359871%2c1420423685%2c1420440507%2c1420507702%3b%20Hm%5flpvt%5f1db88642e346389874251b5a1eded6e3%3d1420532859");
		cookieXueqiu = StockUtil.unescape(cookieStr);
		String mockUser = ConfigCenterFactory.getString("spider.mockUser", "10001,10002,10003,10004,10005,10006,10007,10008,10009,10010,10011,10012,10013,10014,10015,10016,10017,10018,10019,10020,"
				+ "10021,10022,10023,10024,10025,10026,10027,10028,10029,10030,10031,10032,10033,10034,10035,10036,10037,10038,10039,10040,10041,10042,10043,10044,10045,10046,10047,10048,10049");
		String[] mockUidArr = mockUser.split(",");
		if(mockUidArr.length > 0){
			mockUserList.clear();
			MembersService ms = MembersService.getInstance();
			for(int i=0;i<mockUidArr.length;i++){
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
				mockUserList.add(mockMembers);
			}
		}
	}

	/**
	 * 自动分配
	 */
//	public synchronized void  autoAssign(){
////		String companycodeArrStr = ConfigCenterFactory.getString("stock_zjs.recommend_stocks", "");;
//		List<Map<String, String>> ccList = null;
//		try{
//			ccList = ChanceCategoryService.getInstance().getStockChanceList();
//		}catch(Exception e){
//			log.error("机会异常");
//		}
//		StringBuilder sbuf = new StringBuilder();
//		if(ccList != null){
//			for(Map<String, String> map : ccList){
//				String stockcode = map.get("stockcode");
//				sbuf.append(stockcode).append(",");
//				String chancename = map.get("chancename");
//				if(StringUtil.isEmpty(chancename) == false ){
//					if("沪深~null~null".equals(chancename)){
//						companyLanmuMap.put(stockcode, "系统推荐自选股");
//					}else{
//						companyLanmuMap.put(stockcode, chancename);
//					}
//				}else{
//					companyLanmuMap.put(stockcode, "空栏目");
//				}
//				String uuid = map.get("uuid");
//				companyChanceUUIDMap.put(stockcode, uuid);
//			}
//
//		}
//
////		if(sbuf.length() >0){
////			companycodeArrStr = sbuf.toString();
////		}
//		String companycodeArrStr = sbuf.toString();
//		String[] companycodeArr = companycodeArrStr.split(",");
//
//		for(String companycode : companycodeArr){
//			if(StringUtil.isEmpty(companycode)){
//				continue;
//			}
//			boolean exist = companyUidMap.containsKey(companycode);
//			if(exist == false){
//				companyUidMap.put(companycode, null);
//			}
//		}
//
//		//分配给用户公司
//		int uidSize = uidCompanyMap.size();
//		if(uidSize == 0 ){
//			return ;
//		}
//
//		int companySize = companyUidMap.size();
//		int maxCompanyOneUser = ConfigCenterFactory.getInt("spider.maxCompany", 50);
//		int companyOneUser = companySize/uidSize + (companySize%uidSize == 0?0:1);
//		if(companyOneUser > maxCompanyOneUser){
//			companyOneUser = maxCompanyOneUser;
//		}
//		//平均算法，直到满
//		for(String companycode : companycodeArr){
//			Long uid = companyUidMap.get(companycode);
//			if(uid == null){
//				List<UidCompany> sortList = new ArrayList<UidCompany>(uidCompanyMap.values());
//				Collections.sort(sortList,new Comparator<UidCompany>(){
//		            public int compare(UidCompany arg0, UidCompany arg1) {
//		            	int i0 = arg0.getSize();
//		            	int i1 = arg1.getSize();
//						return i0-i1;
//		            }
//				});
//				for(UidCompany uidcompany : sortList){
//					List<String> companyList = uidcompany.getList();
//					if(companyList.size() < companyOneUser){
//						companyList.add(companycode);
//						companyUidMap.put(companycode, uidcompany.getUid());
//						break;
//					}
//				}
//
//
////					Set<Long> uidSet = uidCompanyMap.keySet();
////					for(Long uid1 : uidSet){
////						List<String> companyList = uidCompanyMap.get(uid1).getList();
////						if(companyList.size() <= companyOneUser){
////							companyList.add(companycode);
////							companyUidMap.put(companycode, uid1);
////							break;
////						}
////					}
//			}
//		}
//		log.info("用户分配公司完成");
//
//
//	}

	public synchronized void  autoAssign(long loginUid){
		String mockUidChance = ConfigCenterFactory.getString("spider.mockUidChance", "9:消息集市^10001:分时精选");
		if(StringUtil.isEmpty(mockUidChance)){
			return ;
		}
		HashSet<String> set = new HashSet<String>();
		String[] arr = mockUidChance.split("\\^");
		for( String str : arr ){
			String uidStr = str.split(":")[0];
			if(uidStr.equals(String.valueOf(loginUid))){
				String[] arr2 = str.split(":")[1].split(",");
				for(String str2 : arr2){
					set.add(str2);
				}
				break;
			}
		}

		if( set.size() == 0 ){
			return ;
		}
		List<String> uidentifyList = new ArrayList<String>(set);
		List<Map<String, Object>> ccList = getStockChanceList(uidentifyList);	
		StringBuilder sbuf = new StringBuilder();
		if(ccList != null){
			for(Map<String, Object> map : ccList){
				String stockcode = map.get("stockcode") != null ? (String)map.get("stockcode") : "";
				String chancename = map.get("topicName") != null ? (String)map.get("topicName") : "";
				String uuid = map.get("uuid") != null ? (String)map.get("uuid") : "";
				if(StringUtil.isEmpty(chancename) == false ){
					String[] arrChance = chancename.split("~");
					String lanmuName = arrChance[ arrChance.length -1 ];
					if(set.contains(lanmuName)){ //指定用户对应 栏目 的 公司
//						companyLanmuMap.put(stockcode, lanmuName);						
//						companyChanceUUIDMap.put(stockcode, uuid);
						USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(stockcode);
						sbuf.append(lanmuName).append("^").append(stockcode).append("^").append(us.getName()).append("^").append(uuid).append(",");
					}					
				}
			}

		}

//		System.out.println(sbuf);
		
		String companycodeArrStr = sbuf.toString();
		String[] companycodeArr = companycodeArrStr.split(",");

//		for(String companycode : companycodeArr){
//			if(StringUtil.isEmpty(companycode)){
//				continue;
//			}
////			boolean exist = companyUidMap.containsKey(companycode);
////			if(exist == false){
////				companyUidMap.put(companycode, null);
////			}
//		}

		//分配给用户公司
		int uidSize = uidCompanyMap.size();
		if(uidSize == 0 ){
			return ;
		}

		int maxCompanyOneUser = ConfigCenterFactory.getInt("spider.maxCompany", 50);
		int companyOneUser = maxCompanyOneUser;

		//每次刷新取最新的公司
		UidCompany uidCompany = uidCompanyMap.get(loginUid);
		uidCompany.clear();

		for(String companystr : companycodeArr){
			if(StringUtil.isEmpty(companystr) == false){
				if(uidCompany.getSize()< companyOneUser){
					uidCompany.add(companystr);
	//				companyUidMap.put(companycode, uidCompany.getUid());
				}else{
					break;
				}
			}
		}
		
		List<String> tmpList= uidCompanyTmpListMap.get(loginUid);
		if(tmpList == null){
			tmpList = new ArrayList<String>();
			uidCompanyTmpListMap.put(loginUid, tmpList);
			List<USubject> usList = USubjectService.getInstance().getUSubjectListAStock();
			//增加随机公司
			while(uidCompany.getSize()< companyOneUser){			
				USubject us = usList.get(new Random().nextInt(usList.size()));
				String companystrTmp = "无话题^"+us.getUidentify()+"^"+us.getName()+"^null";
				tmpList.add(companystrTmp);		
				uidCompany.add(companystrTmp);
			}
		}
		for(String companystrTmp : tmpList){
			if(StringUtil.isEmpty(companystrTmp) == false){
				if(uidCompany.getSize()< companyOneUser){			
					uidCompany.add(companystrTmp);
				}else{
					break;
				}
			}
		}
		log.info("用户"+loginUid+"分配公司完成");

	}

	public List<Map<String, Object>> getStockChanceList(List<String> uidentifyList) {
//		String mock_stock_chance_uidentifys = ConfigCenterFactory.getString("spider.mock_stock_chance_uidentifys", "");
//		if(StringUtils.isBlank(mock_stock_chance_uidentifys)) {
//			return null;
//		}
//		List<String> uidentifyList = new ArrayList<String>();
//		for(String uidentify : mock_stock_chance_uidentifys.split(",")) {
//			uidentifyList.add(uidentify);
//		}
		
		if(uidentifyList == null || uidentifyList.size() == 0 ){
			return null;
		}
		return TopicService.getInstance().recommendArticleListSortByTime(uidentifyList, new Date().getTime());
	}

	public void login(long uid){
		MembersService ms = MembersService.getInstance();
		try{
			Members mockMembers = ms.getMembers(uid);
			if(mockMembers != null){
				//记录到uidCompanyMap
				UidCompany uc = uidCompanyMap.get(uid);
				if(uc ==null ){
					synchronized (lockObj) {
						uidCompanyMap.put(uid, new UidCompany(uid));
					}
					//模拟这些用户登录
					//用户登录消息注册事件
					UserMsg um = SMsgFactory
							.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_8);
					String loginUid = String.valueOf(uid);
					um.setS(loginUid);
					um.setD(loginUid);
					UserEventService.getInstance().notifyTheEvent(um);
				}
//				|| uc.getList().size() == 0
				//触发分配用户和公司
				autoAssign(uid);
			}
		}catch (Exception e) {
			log.error("模拟用户登录uid="+uid+"异常");
		}
	}

	private boolean checkContains(String findText){
		String containsStr = ConfigCenterFactory.getString("spider.containsStr", "/images/face/,/images/faces/");
		String[] containsArr = containsStr.split(",");
		for(String contains : containsArr){
			if(findText.contains(contains)){
				return true;
			}
		}
		return false;
	}

	/**
	 * 去除雪球的face
	 * @param text
	 * @return
	 */
	public String replaceFace(String text) {
		String replacement = "";
		Matcher matcher =imgPattern.matcher(text);
		matcher.reset();
        boolean result = matcher.find();
        if (result) {
            StringBuffer sb = new StringBuffer();
            do {
            	String findText = matcher.group();
            	if(checkContains(findText)){
            		matcher.appendReplacement(sb, replacement);
            	}else{
            		matcher.appendReplacement(sb, findText);
            	}
                result = matcher.find();
            } while (result);
            matcher.appendTail(sb);
            return sb.toString();
        }
        return text.toString();
    }

	public void process(Long uid){

		//首先需要配置版主用户对应的虚拟用户和管理的公司板块
		//uid:mockuid,mockuid2:companycode,topcode;uid:mockuid,mockuid2:companycode,topcode
		List<String> tmpCodeList = uidCompanyMap.get(uid).getList();

		Set<String> codeSet = new HashSet<String>();
		for(int i=0;i<tmpCodeList.size();i++){
			String str = tmpCodeList.get(i);
			String uidentify = str.split("\\^")[1];
			USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
			if(us!=null){
				codeSet.add(uidentify);
			}
		}
		List<String> codeList = new ArrayList<String>();
		codeList.addAll(codeSet);
		processQQZxg(codeList,uid);
		processXueqiu(codeList,uid);
	}

	private void processQQZxg(List<String> tmpCodeList,long uid){
		for(String mockCode : tmpCodeList){
			String yfCode = mockCode.toLowerCase();
			String	xqCode = yfCode.split("\\.")[1] + yfCode.split("\\.")[0];
//			String openid = ConfigCenterFactory.getString("spider.zxgopenid", "oA0GbjihiuyXs3NX31OItA19z19c");
//			String uin = ConfigCenterFactory.getString("spider.zxguin", "254107347");
//			String nickname = ConfigCenterFactory.getString("spider.zxgnickname", "新月的梦");
//			String token = ConfigCenterFactory.getString("spider.zxgtoken", "OezXcEiiBSKSxW0eoylIeD8qVRb_fmdwoHVJ1kUv5VVfDgpQzbmbTYd_MdQmjyWea3ew7R5UuiecVUsYHO6LOG2sDP_WtcNzLZ_ivLv4gBGK6nR2KzcDsebCTyhxEifr7u_vQmop-NJqlagM5uWguQ");
//			String url = "http://group.finance.qq.com/newstockgroup/rssService/getNewRssList?check=6&_appName=android&_dev=ZTE-N919"
//					+ "&_devId=d8e6a97c8afa1e78442ab562b293af3c3551e4aa&_mid=d8e6a97c8afa1e78442ab562b293af3c3551e4aa&"
//					+ "_md5mid=970DCA4C0E48AB16F5956F15F334436E&_appver=3.9.6&_ifChId=76&_screenW=540&_screenH=960&_osVer=4.1.2&_uin="+uin+"&__random_suffix="+new Random().nextInt(10000);
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
					long time = StockSpider.getTime(timeStr);
					SpiderStorageBean ssb = new SpiderStorageBean();
					ssb.setTitle(null);
					ssb.setContent(content);
					ssb.setKey(key);
					ssb.setTime(time);
					ssb.setSummary(content);
					ssb.setUid(Long.valueOf(uid));
					ssb.setTags(mockCode);
					ssb.setSource_url("腾讯");
					insert(ssb);
				}
				Thread.sleep(1000l);
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

	private void processXueqiu(List<String> tmpCodeList,long uid){
		int atokenStart = cookieXueqiu.indexOf("xq_a_token=")+"xq_a_token=".length();
		int atokenEnd = cookieXueqiu.indexOf(";", atokenStart);
		String access_token = cookieXueqiu.substring(atokenStart,atokenEnd);
		for( String mockCode : tmpCodeList ){
			USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(mockCode);
			if(usubject == null){
				continue;
			}
			log.info("TalkMockService抓取"+mockCode);
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
			String jsonContent = StockSpider.doSpiderByLoginCookie(spiderUrl,cookieXueqiu);
			//第一次模仿浏览器访问 http://xueqiu.com/S/SH000001 获取到Set-Cookie	xq_a_token=7fd433866f17942022661a942c06f2b5fe387c88
			HttpClient httpClient = new HttpClient();
			// 设置代理 http://www.kuaidaili.com/proxylist/2/
//			httpClient.getHostConfiguration().setProxy("110.73.10.235", 80);
//			//获取响应头
//			request.getResponseHeaders();

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
				if(content.contains("我刚刚关注了") || content.contains("(ZH") || content.contains("雪碧")){
					continue;
				}
				//去掉一些雪球的标记
				content = Jsoup.clean(content,StockSpider.myWhitelist()).replaceAll("\\$", "").replace("!thumb.jpg", "")
						.replace("!custom.jpg", "").replace("SZ", "").replace("SH", "");
				content = replaceFace(content);
				content = content.replaceAll("\\([^\\(]*\\)", "");
				desc = Jsoup.clean(desc,StockSpider.myWhitelist()).replaceAll("\\$", "").replace("SZ", "").replace("SH", "");
				desc = replaceFace(desc);
				desc = (desc+" ").replaceAll("\\([^\\(]*\\)", "").replaceAll("(回复)?(//)?@[_a-zA-Z\\d\\u2E80-\\uFE4F]+", ",").replace(",,", ",").replace(" ", "");
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
				ssb.setSource_url("雪球");
				insert(ssb);
			}

			try {
				Thread.sleep(1000l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


	}


	public List<SpiderStorageBean> list(long uid,String tags,int start,int limit){
		try {
			if(tags !=null){
				tags = tags.split("\\^")[1];
			}
		} catch (Exception e) {
			return null;
		}
		List<SpiderStorageBean> ssbList = new ArrayList<SpiderStorageBean>();
		Cache cache = getCache();
		//Attribute<Long> uidAtt = cache.getSearchAttribute("uid");
		Results rs =  null;
		if(StringUtil.isEmpty(tags)){
			rs = cache.createQuery() /*.addCriteria(uidAtt.eq(uid))*/.includeKeys().addOrderBy(new Attribute<SpiderStorageBean>("time"), Direction.DESCENDING).maxResults(start+limit).execute();
		}else{
			Attribute<String> tagsAtt = cache.getSearchAttribute("tags");
			rs = cache.createQuery() /*.addCriteria(uidAtt.eq(uid)) */.addCriteria(tagsAtt.eq(tags)).includeKeys().addOrderBy(new Attribute<SpiderStorageBean>("time"), Direction.DESCENDING).maxResults(start+limit).execute();
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

	public int size(long uid,String tags){
		try {
			if(tags !=null){
				tags = tags.split("\\^")[1];
			}
		} catch (Exception e) {
			return 0;
		}
		Cache cache = getCache();
		//Attribute<Long> uidAtt = cache.getSearchAttribute("uid");
		int size = 0;
		if(StringUtil.isEmpty(tags)){
			size = cache.createQuery()/*.addCriteria(uidAtt.eq(uid))*/.includeKeys().execute().size();
		}else{
			Attribute<String> tagsAtt = cache.getSearchAttribute("tags");
			size = cache.createQuery()/*.addCriteria(uidAtt.eq(uid))*/.addCriteria(tagsAtt.eq(tags)).includeKeys().execute().size();
		}
		return size;
	}

	private void insert(SpiderStorageBean ssb) {
		if(ssb.getTime() == 0l){
			ssb.setTime(System.currentTimeMillis());
		}
		Cache cache = getCache();
		if(cache.get(ssb.getKey()) == null){
			Element element = new Element(ssb.getKey(),ssb);
			cache.put(element);
		}
	}

	private Cache getCache(){
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

	public void cacheToFile(){
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		cimpl.flushToDisk(chcheName);
	}

	public void fileToCache(){
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		cimpl.flushToCache(chcheName);
	}

	public SpiderStorageBean get(String key){
		Cache cache = getCache();
		Element element = cache.get(key);
		if(element == null)return null;
		Object object = element.getValue();
		SpiderStorageBean ssb = (SpiderStorageBean)object;
		return ssb;
	}

	public void delete(String key){
		Cache cache = getCache();
		cache.remove(key);
	}

	public String mockPublishArticle(long uid,String key,String newContent){
		Article article = get(key);
		if(article != null){
			Article articleTmp = article.clone();
			articleTmp.setSource_url(null);
			String tag = article.getTags();
			USubject  usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(tag);	
			if(usubject == null){
				return StockCodes.SUCCESS;
			}
			if(StringUtil.isEmpty(newContent) == false ){				
				newContent = "#"+usubject.getUidentify()+":"+usubject.getName()+"# "+ newContent; //#000333.sz:美的集团# 				
				articleTmp.setContent(newContent);
				articleTmp.setSummary( StringUtil.getMaxString(newContent, 140));
				articleTmp.setContent(newContent);
				String suumary = StockSpider.toSummary(newContent);
				articleTmp.setSummary(suumary);
			}else{
				newContent = articleTmp.getContent(); 
				newContent = "#"+usubject.getUidentify()+":"+usubject.getName()+"# "+ newContent; //#000333.sz:美的集团# 				
				articleTmp.setContent(newContent);
				articleTmp.setSummary( StringUtil.getMaxString(newContent, 140));
			}
			//内容太少，则会取消发布
			if(articleTmp.getContent().length() < 4){
				return "内容少于4个字，不允许发布";
			}

			Members mockMembers = mockUserList.get(new Random().nextInt(mockUserList.size()));
			SpiderStorage.publishSpiderArticle(mockMembers, articleTmp, articleTmp.getTags());
			StringBuilder sbuf = new StringBuilder();
			sbuf.append("replace into  mocklog ( uid,companycode,uuid,uptime,logdesc ) VALUES ( ").append(uid).append(",'")
				.append(articleTmp.getTags()).append("','").append(articleTmp.getUuid()).append("','").append(DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS))
				.append("','").append("发布博文。来源：").append(key).append("');");
			logSql( sbuf.toString() );
			article.setStatus(1);
		}
		return StockCodes.SUCCESS;
	}

	public SimpleArticle getViewpoint(String tag){
		List<SimpleArticle> saList = ViewpointService.getInstance().listFromDcss(tag, System.currentTimeMillis(), 1, 1);
		if(saList != null && saList.size() >0){
			return saList.get(0);
		}
		return null;
	}

	public String mockPublishComment(long uid,String key,String newContent,String tags){
		Article article = get(key);
		return mockPublishCommentBase(uid, key, newContent, tags, article);
	}
		
	private String mockPublishCommentBase(long uid,String key,String newContent,String tags,Article article){
		if(article != null){
//			Members mockMembers = MembersService.getInstance().getMembers(uid);
			Members mockMembers = mockUserList.get(new Random().nextInt(mockUserList.size()));
			Article articleTmp = article.clone();
			articleTmp.setSource_url(null);
			if(StringUtil.isEmpty(newContent) == false ){
				articleTmp.setContent(newContent);
				String suumary = StockSpider.toSummary(newContent);
				articleTmp.setSummary(suumary);
			}else{
				newContent = articleTmp.getContent();
			}
			newContent = newContent.replaceAll("<[^<]*>", "");
			String content140 = StringUtil.getMaxString(newContent, 140);
			//内容太少，则会取消发布
			if(newContent.length() < 4){
				return "内容少于4个字，不允许发布评论";
			}
			if(newContent.length() != content140.length() ){
				return "内容太长，不允许发布评论";
			}

			String uuid = articleTmp.getUuid();
			if(StringUtil.isEmpty(uuid)){
				uuid = String.valueOf(UUID.randomUUID());
				articleTmp.setUuid(uuid);
			}
			articleTmp.setNick(mockMembers.getNickname());
			articleTmp.setUid(mockMembers.getUid());
			articleTmp.setType(2);
			articleTmp.setTime(System.currentTimeMillis());
			String tag =articleTmp.getTags();
			//根据公司找到机会，给机会发布评论
			{
				if( StringUtil.isEmpty(tags) ){
					UidCompany uc = uidCompanyMap.get(uid);
					if(uc == null){
						return "不存在";
					}
					List<String> list = uc.getList();
					for(String str : list){
						if(str.contains(tag)){
							tags = str;
							break;
						}
					}
				}
				String auuid = this.getChanceUUID(tags);
				if(StringUtil.isEmpty(auuid) == true ){
					return "没有机会可以评论，请检查是否有"+tag+"的机会";
				}
				List<String> uuidList = new ArrayList<String>();
				uuidList.add(auuid);
				List<SimpleArticle> saList = RemindServiceClient.getInstance().getSimpleArticleList(auuid, uuidList);
				if(saList == null || saList.size() == 0 ){
					return tag+"机会不存在，请确定是否已删除";
				}
//			List<SimpleArticle> saList = ViewpointService.getInstance().listFromDcss(tag, System.currentTimeMillis(), 1, 1);
//			if(saList != null && saList.size() >0){
//				String auuid = saList.get(0).getUuid();
				Comment comment = new Comment();
				comment.setContent(newContent);
				long currentTimeMillis = System.currentTimeMillis();
				comment.setTime(currentTimeMillis);
				comment.setUid(mockMembers.getUid());
				comment.setUuid(uuid);
				comment.setPuuid(auuid);
				comment.setPath(null);
				comment.setAt(null);
				comment.setNick(mockMembers.getNickname());
				MicorBlogService.getInstance().addStockChanceComment(comment);
				
							
				MessageCenterSerivce.getInstance().pushMsg(newContent,auuid,null,uuid,mockMembers.getUid(),currentTimeMillis, false);
				int zhuanzai_view_to_index = ConfigCenterFactory.getInt("spider.zhuanzai_view_to_index", 0);//是否发转发到首页
				
//				USubject  usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(tag);
//				if(usubject == null){
//					return StockCodes.SUCCESS;
//				}
//				newContent = "#"+usubject.getUidentify()+":"+usubject.getName()+"# "+ newContent; //#000333.sz:美的集团# 				
//				articleTmp.setContent(newContent);
//				articleTmp.setSummary( StringUtil.getMaxString(newContent, 140));
				
				if(zhuanzai_view_to_index == 0){
					articleTmp.setSuuid(auuid);
					int type = ConfigCenterFactory.getInt("spider.vp_cmt_type", 0);
					if(type == 0) {
						articleTmp.setType(ShareConst.YUAN_CHUANG);
					} else if(type == 1) {
						articleTmp.setType(ShareConst.ZHUANG_ZAI);
					}
					
					MicorBlogService.getInstance().mockStockChanceCommentArticle(articleTmp, false);
				} else if(zhuanzai_view_to_index == 1) {
					articleTmp.setType(ShareConst.ZHUANG_ZAI);
					articleTmp.setSuuid(auuid);
					MicorBlogService.getInstance().mockStockChanceCommentArticle(articleTmp, true);
				}
				StringBuilder sbuf = new StringBuilder();
				sbuf.append("replace into  mocklog ( uid,companycode,uuid,uptime,logdesc ) VALUES ( ").append(uid).append(",'")
					.append(tag).append("','").append(uuid).append("','").append(DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS))
					.append("','").append("发布评论。来源：").append(key).append("');");
				logSql( sbuf.toString() );
			}
			article.setStatus(1);
		}
		return StockCodes.SUCCESS;
	}

	/**
	 * @param uid
	 * @param key 不需要的参数
	 * @param newContent
	 * @param tag
	 * @return
	 */
	public String mockPublishCommentByTag(long uid,String key,String newContent,String tags){
		String uidentify = null;
		try {
			if(tags !=null){
				uidentify = tags.split("\\^")[1];
			}
		} catch (Exception e) {
			return "公司"+tags+"不存在";
		}
		USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(uidentify);
		if(usubject == null){
			return "公司"+uidentify+"不存在";
		}
		String content140 = StringUtil.getMaxString(newContent, 140);
		//内容太少，则会取消发布
		if(newContent.length() < 4){
			return "内容少于4个字，不允许发布评论";
		}
		if(newContent.length() != content140.length() ){
			return "内容太长，不允许发布评论";
		}
		
		String uuid = UUID.randomUUID().toString();
		Article articleTmp = new Article();			
		articleTmp.setContent(content140);
		articleTmp.setSummary(content140);				
		articleTmp.setTags(uidentify);
		articleTmp.setUuid(uuid);
		articleTmp.setType(ShareConst.ZHUANG_ZAI);
		articleTmp.setTime(System.currentTimeMillis());		
			
		return mockPublishCommentBase(uid, key, newContent, tags, articleTmp);
	}


	private void logSql(String sqlMsg){
		String fileName = BaseUtil.getConfigPath("spider/"+DateUtil.getSysDate(DateUtil.YYYYMMDD_SHORT)+".sql");
		try {
			//输出日志
			LogSvr.logMsgWithoutDate(sqlMsg, fileName);
		} catch (IOException e) {
			log.info("IO错误 输出日志失败" + fileName);
		}
	}

	public String getChanceUUID(String tags){
		String uuid = null;
		try {
			if(tags !=null){
				uuid = tags.split("\\^")[3];
				return uuid;
			}
		} catch (Exception e) {
			return null;
		}
		return null ;//TODO companyChanceUUIDMap.get(tag);
	}

}
