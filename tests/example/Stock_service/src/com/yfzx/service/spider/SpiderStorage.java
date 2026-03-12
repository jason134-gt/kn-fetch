package com.yfzx.service.spider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.BFConst;
import com.stock.common.bloomfilter.BFUtil;
import com.stock.common.constants.SCache;
import com.stock.common.constants.StockCodes;
import com.stock.common.model.Company;
import com.stock.common.model.Industry;
import com.stock.common.model.USubject;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.UserExt;
import com.stock.common.model.user.Members;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.MultiStringReplacer;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.user.MembersService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.TalkMessageService;
import com.yfzx.service.msg.UserEventService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.NoticeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.mycore.lcs.enter.LCEnter;

/**
 * 支持存储缓存
 */
public class SpiderStorage {
	static Logger log = LoggerFactory.getLogger(QQCompanyTaskProcessor.class);
	private static MultiStringReplacer msr = new MultiStringReplacer();
	private static String chcheName = new SpiderStorageBean().getDataType();
	private static List<Members> memberList = new ArrayList<Members>();

	private static Object lockObj = new Object();
	static {
		initMembers();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				initMembers();
			}
		});
	}

	public static List<Members> getMockMembersList() {
		return memberList;
	}

	private static void initMembers(){
		//配置中心配置
		String mockUsersStr = ConfigCenterFactory.getString("spider.mockUser", "9,10043,10044,10045,10046,10047,10048");//配置中心获取
		MembersService ms = MembersService.getInstance();

		if(StringUtils.isEmpty(mockUsersStr) == false){
			List<Members> tmpMemberList = new ArrayList<Members>();
			String[] uidArr = mockUsersStr.split(",");
			for(int i=0;i<uidArr.length;i++){
				try{
					long uid = Long.valueOf(uidArr[i]);
					//模拟这些用户登录
					//用户登录消息注册事件
					UserMsg um = SMsgFactory
							.getSingleUserMsgByType(MsgConst.MSG_USER_TYPE_8);
					String loginUid = uidArr[i];
					um.setS(loginUid);
					um.setD(loginUid);
					UserEventService.getInstance().notifyTheEvent(um);
					tmpMemberList.add(ms.getMembers(uid));
				}catch (Exception e) {
					log.error("推荐的用户uid="+uidArr[i]+"异常");
				}
			}
			memberList = tmpMemberList;
		}

		List<Industry> industryList = LCEnter.getInstance().get(SCache.CACHE_KEY_INDUSTRY_All_YFZX, SCache.CACHE_NAME_INDUSTRY);
		for(Industry industry:industryList){
			msr.add(industry.getName(), "");
		}
		//过滤微博中一些博文，必须是公司 或对应行业的等博文，才进行写入操作
		String cjWordStr = ConfigCenterFactory.getString("spider.keywords",
				"行业,企业,协议,利率,投资,交易,CPI,日报,周报,税务,央行,价格,承销商,投行,环比,同比,经济,K线,行情,基本面,消息面,技术面");
		String[] cjWordArr = cjWordStr.split(",");
		for(String cj : cjWordArr){
			if(StringUtil.isEmpty(cj) ==  false){
				msr.add(cj, "");
			}
		}

	}

	private static Cache getCache(){
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
		Cache cache = cimpl.getCacheManager().getCache(CacheUtil.getCacheName(chcheName));
		if(cache == null ){
			CacheConfiguration cacheCfg = new CacheConfiguration(CacheUtil.getCacheName(chcheName),1000000);
			cacheCfg.setEternal(true);
			Searchable searchable = new Searchable();
			searchable.addSearchAttribute(new SearchAttribute().name("time"));
			cacheCfg.addSearchable(searchable);
			if(cache == null){
				synchronized (lockObj) {
					Cache  cacheNew = new Cache(cacheCfg);
					try{
						cimpl.getCacheManager().addCache(cacheNew);
					}catch (Exception e) {
						log.error("创建缓存SpiderStorageBean失败"+e.getMessage());
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

	public static void insertNoPublish(SpiderStorageBean ssb) {
		if(ssb.getTime() == 0l){
			ssb.setTime(System.currentTimeMillis());
		}
		Cache cache = getCache();
		if(cache.get(ssb.getKey()) == null){
			Element element = new Element(ssb.getKey(),ssb);
			cache.put(element);
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

			insertAfter(ssb);
		}
	}

	public static void clearNoToday(){
		int hourNum = ConfigCenterFactory.getInt("spider.clearHourNum", 24);
		Cache cache = getCache();
		Results rs = cache.createQuery().includeKeys().addCriteria(new LessThan("time",Long.valueOf(System.currentTimeMillis()-hourNum*3600*1000))).execute();
		List<Result> list = rs.all();
		for(Result result : list){
			cache.remove(result.getKey());
		}
	}

	public static void doSchedulerTask(){
		log.info("执行清理Spider历史开始");
		clearNoToday();
		cacheToFile();
		log.info("执行清理Spider历史结束");
	}

	@SuppressWarnings("unchecked")
	public static List<SpiderStorageBean> list(int start,int limit){
		List<SpiderStorageBean> ssbList = new ArrayList<SpiderStorageBean>();
		Cache cache = getCache();
		Results rs = cache.createQuery().includeKeys().addOrderBy(new Attribute<SpiderStorageBean>("time"), Direction.DESCENDING).maxResults(start+limit).execute();
		List<Result> list = rs.range(start, limit);

		//		List<String> keyList = cache.getKeys();
		//		int toIndex = 0;
		//		if(keyList.size() < start){
		//			return ssbList;
		//		}else if(keyList.size() < start+limit){
		//			toIndex = keyList.size();
		//		}else{
		//			toIndex = start+limit;
		//		}
		//		List<String> subList = keyList.subList(start, toIndex);
		List<String> subList = new ArrayList<String>();
		for(Result result : list){
			subList.add(result.getKey().toString());
		}
		for(String key : subList){
			Element element = cache.get(key);
			Object object = element.getValue();
			SpiderStorageBean ssb = (SpiderStorageBean)object;
			ssb.setContent("省略");
			ssbList.add(ssb);
		}
		return ssbList;
	}

	public static int size(){
		Cache cache = getCache();
		int size = cache.getKeys().size();
		return size;
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

	public static void clear(){
		Cache cache = getCache();
		cache.removeAll();
	}

	//初始化key,用于分析文章
	private static String regexKey = null;
	private static Map<String,String> mapKeyToTopic = null; //初始写入,后续只读	601006.sh:大秦铁路
	static{
		initKey();
	}
	static void initKey(){
		if(regexKey == null){
			List<Company> companyArr = CompanyService.getInstance().getCompanyList();
			List<String> kList = new ArrayList<String>();
			mapKeyToTopic = new HashMap<String,String>();
			for(Company c :companyArr){
				String code = c.getStockCode().toLowerCase();
				String tag = code+":"+c.getSimpileName();

				String[] codeArr = code.split("\\.");
				if(codeArr.length == 2){
					//支持000002sz
					String newCode_a = codeArr[0]+codeArr[1];
					kList.add(newCode_a);
					mapKeyToTopic.put(newCode_a,tag);
					//支持sz000002
					String newCode_b = codeArr[1]+codeArr[0];
					kList.add(newCode_b);
					mapKeyToTopic.put(newCode_b,tag);
				}else{
					continue;
				}
				String name = c.getSimpileName().toLowerCase().replace("*", "\\*");//原始内容 可能是[万  科Ａ]  正则表达
				kList.add(name);
				mapKeyToTopic.put(name,tag);
				String name1 = name.replace("Ａ", "A").replace("Ｂ","B");//转换大小写 支持[万  科A]
				if(!name.equals(name1)){
					kList.add(name1);
					mapKeyToTopic.put(name1,tag);
				}
				String name2 = name1.replace(" ", "");//去除空格 支持[万科A]
				if(!name2.endsWith(name1)){
					kList.add(name2);
					mapKeyToTopic.put(name2,tag);
				}
				String name3 = name2.replaceAll("[AB]", "");//去除英文 支持无字母[万科],以前版本有错误,TCL集团 不能转成集团
				if(!name3.equals(name2)){
					kList.add(name3);
					if(!mapKeyToTopic.containsKey(name3)){
						mapKeyToTopic.put(name3,tag);
					}else{
						if(mapKeyToTopic.get(name3).startsWith("2") || mapKeyToTopic.get(name3).startsWith("9")){//如果第一次插入的B股，则覆盖
							mapKeyToTopic.put(name3,tag);
						}
					}
				}

			}
			regexKey = StringUtils.join(kList,"|");
		}
	}

	/**
	 * 从文本中分析获取话题Topic，并返回话题出现的次数
	 * @param content
	 * @return
	 */
	public static List<TagBean> getTags(String content){
		// \\pP \\p=表示 Unicode 属性  P表示 Unicode 字符集七个字符属性之一：标点字符   Z：分隔符（比如空格、换行等）
		content = content.replaceAll("<script[^<]*>.*</script>", "").replaceAll("<!--[^<]*>[^(-->)]*<![^<]*-->", "").replaceAll("<[^<]*>", "").replaceAll("[\\pP\\pZ‘’“”]", "");
//				replaceAll("<[^<]*>", "").replace(" ", "").replace("\r\n", "").replace("\r", "").replace("\n", "").
//				replace("\t", "").replace("&nbsp;", "").replaceAll("[\\pP‘’“”]", "");
		Pattern keywordPattern = Pattern.compile(regexKey,Pattern.CASE_INSENSITIVE);
		Matcher matcher = keywordPattern.matcher(content);
		HashMap<String,Integer> map = new HashMap<String,Integer>();//去除重复
		while (matcher.find()) {
			String k = matcher.group();
			k = k.replace("*", "\\*").toLowerCase(); //因为正则表达式 \* 代表实际 * ,同.等
			String topic = mapKeyToTopic.get(k);
			if(!map.containsKey(topic)){
				map.put(topic, 1);
			}else{
				Integer i = map.get(topic);
				map.put(topic, i+1);
			}
		}
		List<TagBean> tagList = new ArrayList<TagBean>();
		for(String key : map.keySet()){
			Integer i = map.get(key);
			TagBean tag = new TagBean();
			tag.setTag(key);
			tag.setNum(i);
			tagList.add(tag);
		}
		if(tagList.size() > 1){
			ComparatorTagBean ctb = new ComparatorTagBean();
			Collections.sort(tagList,ctb);
		}
		return tagList;
	}

	/**
	 * 如果存在非法内容，或者长度不够，都返回false
	 */
	private static boolean checkContent(String content){
		int minLength = ConfigCenterFactory.getInt("spider.contentMinLength", 15);
		if(content.length() < minLength){
			return false;
		}
		if(content.length() < 50){
			String reg = ConfigCenterFactory.getString("spider.unInsertContent", "(买入\\$.+\\$)|(关注股票\\$.+\\$)|(买入目标价\\$[0-9.]+)");
			Pattern pattern = Pattern.compile(reg,Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(content);
	        boolean check = matcher.matches();
	        if(check){
	        	return false;
	        }
		}
		return true;

	}

	private static void insertAfter(SpiderStorageBean ssb){
		String key = ssb.getKey();
		if(BFUtil.checkAndAdd(BFConst.spiderKeyFilter, key)){
			//已处理过KEY,不再处理
			return;
		}
		String unInsertTitleStr = ConfigCenterFactory.getString("spider.unInsertTitle", "融资融券信息,大宗交易数据,龙虎榜数据");
		if(StringUtil.isEmpty(unInsertTitleStr) ==false){
			String[] unInsertTitleArr = unInsertTitleStr.split(",");
			for(String unInsertTitle : unInsertTitleArr){
				if(ssb.getTitle().contains(unInsertTitle)){
					return;
				}
			}
		}
//		//融资融券信息 丢弃
//		if(ssb.getTitle().contains("融资融券信息")){
//			return ;
//		}
//		if(ssb.getTitle().contains("大宗交易数据")){
//			return ;
//		}
//		if(ssb.getTitle().contains("龙虎榜数据")){
//			return ;
//		}

		String autoInsertArticle = ConfigCenterFactory.getString("spider.autoInsertArticle", "false");
		if("true".equals(autoInsertArticle)){
			String companyCode = ssb.getTags();
			String tags = null;//ssb.getTags();
			long time = ssb.getTime();
			long insertDBTimeMillis = ConfigCenterFactory.getLong("spider.insertDBTimeMillis", 3600000l);
			if(System.currentTimeMillis() - time > insertDBTimeMillis){
				return;
			}

			String content = ssb.getContent();
			if(checkContent(content) == false){
				return;
			}
			if(StringUtils.isEmpty(tags)){
				List<TagBean> tagListT = getTags(ssb.getTitle());
				//标题有公司，则不抽取博文的正文的标题
				if( StringUtils.isEmpty(tags) ){
					tags = "";
					for(int i=0;i<tagListT.size();i++){
						if(i>5)break;
						TagBean tagBean = tagListT.get(i);
						tags += tagBean.getTag() + ",";
					}
					if(tags.length() >1){
						tags = tags.substring(0, tags.length() -1 );
					}
					ssb.setTags(tags);
				}
				if( StringUtils.isEmpty(tags) ){
					List<TagBean> tagList = getTags(content);
					tags = "";
					for(int i=0;i<tagList.size();i++){
						if(i>5)break;
						TagBean tagBean = tagList.get(i);
						tags += tagBean.getTag() + ",";
					}
					if(tags.length() >1){
						tags = tags.substring(0, tags.length() -1 );
					}
					ssb.setTags(tags);
				}
			}
			//随机用户
			Article article = ssb;
			article.setSource_url(ssb.getKey());
			Random random = new Random();
			Members members =memberList.get(random.nextInt(memberList.size()));
			//有tag表示从公告或公司新闻处抓取的结果，可能存在发送消息的要求
			if(StringUtils.isEmpty(companyCode) == false){
				String[] companyArr = tags.split(",");
				Set<String> companyCodeSet = new HashSet<String>();
				//注销此行，防止文章无关情况
				//companyCodeSet.add(companyCode);
				for(String company :companyArr){
					String tmpcompanyCode = company.split(":")[0];
					if(StringUtil.isEmpty(tmpcompanyCode) == false){
						companyCodeSet.add(tmpcompanyCode);
					}
				}
				//配置中心配置开关，都放开
				String putMessage = ConfigCenterFactory.getString("spider.putMessage", "true");
				String putArticle = ConfigCenterFactory.getString("spider.putArticle", "true");

				//A等级发私信，B等级发博文  默认null，同时都发
				String putLevel =  ssb.getPutLevel() ;
				if(putLevel == null){
					//同时发私信 又发博文，如QQ新闻
					//包含多个公司的新闻，目前设置以博文方式发出
					if(companyCodeSet.size() > 1){
						putMessage = "false";
						putArticle = "true";
					}
				}else if("A".equals(putLevel)){
					//只发私信
					if("true".equals(putMessage)){
						putMessage = "true";
					}
					putArticle = "false";
					//包含多个公司的新闻，目前设置以博文方式发出
					if(companyCodeSet.size() != 1){
						putMessage = "false";
						putArticle = "true";
					}
				}else if("B".equals(putLevel)){
					//只发博文 如
					if("true".equals(putArticle)){
						putArticle = "true";
					}
					putMessage = "false";
				}else if("C".equals(putLevel))	{
					//公司公告的博文都只有一个公司
					USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(companyCode);

					if(usubject.getUid() > 0l){
						//公司默认用户
						UserExt ue = UserServiceClient.getInstance().getUserExtByUid(usubject.getUid());
						UUID uuid = UUID.randomUUID();
						article.setUid(ue.getUid());
						article.setNick(ue.getNickname());
						article.setUuid(uuid.toString());
						//将博文里的时间【抓取的】，改成当前系统时间
						article.setTime(System.currentTimeMillis());
						article.setTags(usubject.getUidentify());
						NoticeService.getInstance().publishStockNotice(article);
					}
					return;
				}else if("D".equals(putLevel))	{
					//传闻求证 发博文，同时发私信，私信内容特殊一点
					for(String tmpcompanyCode : companyCodeSet){
						String url = ssb.getKey();
//						String msg = ssb.getTitle() + "<br/>"+ssb.getContent()+"<br>来源<a href='"+url+"' target='new'>" + url+"</a>";
//						UsubjectEventService.getInstance().broadcastUsbjectTalkMessage(tmpcompanyCode, msg);
						String summary = ssb.getSummary();
						if(StringUtil.isEmpty(summary)){
							summary = StockSpider.toSummary(ssb.getContent());
						}
						String msg = summary;
						Map<String,Serializable> headerMap = new HashMap<String,Serializable>();
						headerMap.put("h", ssb.getTitle());
						headerMap.put("l", url);
						String s = ssb.getAttr("s");
						if(StringUtil.isEmpty(s) == false){
							headerMap.put("s", url);
						}
						TalkMessageService.getInstance().broadcastUsbjectTalkMessage(tmpcompanyCode, msg, 2, headerMap);
					}
					for(String tmpcompanyCode :companyCodeSet){

						USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(tmpcompanyCode);
						if(usubject.getUid() > 0l){
							//公司默认用户
							UserExt ue = UserServiceClient.getInstance().getUserExtByUid(usubject.getUid());
							members = new Members();
							members.setUid(usubject.getUid());
							members.setNickname(ue.getNickname());
							String articleTags = usubject.getUidentify()+":"+usubject.getName();
							article.setTags(articleTags);
							publishSpiderArticle(members,article,articleTags);
						}
					}
					return;
				}

				//作为私信通知出来
				if("true".equals(putMessage)){
					for(String tmpcompanyCode : companyCodeSet){
						String url = ssb.getKey();
//						String msg = ssb.getTitle() + "<br/>来源<a href='"+url+"' target='new'>" + url+"</a>";
//						UsubjectEventService.getInstance().broadcastUsbjectTalkMessage(tmpcompanyCode, msg);

						String summary = ssb.getSummary();
						if(StringUtil.isEmpty(summary)){
							summary = StockSpider.toSummary(ssb.getContent());
						}
						String msg = summary;
						Map<String,Serializable> headerMap = new HashMap<String,Serializable>();
						headerMap.put("h", ssb.getTitle());
						headerMap.put("l", url);
						String s = ssb.getAttr("s");
						if(StringUtil.isEmpty(s) == false){
							headerMap.put("s", url);
						}
						TalkMessageService.getInstance().broadcastUsbjectTalkMessage(tmpcompanyCode, msg, 2, headerMap);
					}
				}

//				//使用虚拟的公司用户 写文章，并绑定到公司下，推到对应的用户处
				if("true".equals(putArticle)){
					for(String tmpcompanyCode :companyCodeSet){

						USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(tmpcompanyCode);

						if(usubject.getUid() > 0l){
							// MembersService.getInstance().getMembers(usubject.getUid());
							//公司默认用户
							UserExt ue = UserServiceClient.getInstance().getUserExtByUid(usubject.getUid());
							members = new Members();
							members.setUid(usubject.getUid());
							members.setNickname(ue.getNickname());
							String articleTags = usubject.getUidentify()+":"+usubject.getName();
							article.setTags(articleTags);
							publishSpiderArticle(members,article,articleTags);
						}
					}
				}
			}else{
				if(StringUtils.isEmpty(tags)){
					//如果不是财经相关的博文，不进行写入
//					Pair<String, Integer> pair = msr.findFirstWord(ssb.getTitle()+" " + content);
					boolean existWords = msr.existWords(ssb.getTitle()+" " + content);
					if(existWords == false)return;
				}
				publishSpiderArticle(members, article, tags);
			}
		}else{
			log.error("spider.autoInsertArticle != true");
		}
	}

	public static void publishSpiderArticle(Members members,Article article,String tags){

		UUID uuid = UUID.randomUUID();
		article.setUid(members.getUid());
		//将博文里的时间【抓取的】，改成当前系统时间
		article.setTime(System.currentTimeMillis());
		article.setNick(members.getNickname());
		article.setUuid(uuid.toString());
		article.setTags(tags);
		//publishArticle 已经有发送微博消息给消息中心
		String publishResult = MicorBlogService.getInstance().publishArticle(article);
		if(StockCodes.SUCCESS.equals(publishResult) == false){
			log.warn("publishResult ="+publishResult);
		}

		UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(members.getUid());
		if(userExt != null) {
			//同步到DCSS中
			Map<String, Object> userMap = new HashMap<String, Object>();
			userMap.put("article_counts", userExt.getArticle_counts() + 1);
			UserServiceClient.getInstance().updateUserExt(members.getUid(), userMap);
		}
//		//写完后检查是否一致，如果不一致，重写文章数等信息 TODO 上线时删掉
//		NosqlService.getInstance().repairUserExtArticleCounts(members.getUid());
	}

}
