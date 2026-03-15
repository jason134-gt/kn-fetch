package com.yfzx.service.db.user;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.UserCredentialsDataSourceAdapter;

import com.stock.common.constants.AsyncTaskConstants;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.USubject;
import com.stock.common.model.share.Invite;
import com.stock.common.model.user.Members;
import com.stock.common.model.user.ThirdUser;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.CipherUtil;
import com.stock.common.util.HttpUtil;
import com.stock.common.util.NosqlBeanUtil;
import com.stock.common.util.RSAUtil;
import com.stock.common.util.SqlWithTrasitionUtil;
import com.stock.common.util.SqlWithTrasitionUtil.ISqlCallback;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.client.UserServiceClient;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.user.RoleService.ROLE_TYPE;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class MembersService  {

	private final static Pattern pattern = Pattern.compile("([a-zA-Z]*)([ａ-ｚＡ-Ｚ]*)([\\d]*)([\u4e00-\u9fa5]*)");
	private final static String BASE_NS = "com.yz.stock.portal.dao.members.MembersDao";//MerbersMapper.xml的namespace
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(MembersService.class);
	static MembersService instance = new MembersService();
	static DBAgent dbAgent = DBAgent.getInstance();

	private static Set<String> sensibleWordsSet = new HashSet<String>();

	private MembersService(){}

	public static MembersService getInstance(){
		return instance;
	}

	public static Set<String> getSensibleWordsSet() {
		return sensibleWordsSet;
	}

	static {
		init();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				init();
			}
		});
	}

	private static void init() {
		sensibleWordsSet.clear();
		String sensibleWords = ConfigCenterFactory.getString("stock_zjs.sensible_words", "igushuo,爱股说,爱股,徐文辉,邹峻,盈富在线,盈富");
		if(StringUtils.isNotBlank(sensibleWords)) {
			for(String word : sensibleWords.split(",")) {
				sensibleWordsSet.add(word);
			}
		}
	}

	public boolean containsSensibleWord(String content) {
		if(StringUtils.isBlank(content)) {
			return false;
		}
		if(sensibleWordsSet.size() == 0) {
			return false;
		}
		for(String sensibleWord : sensibleWordsSet) {
			if(content.toLowerCase().contains(sensibleWord)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 公共解密方法，根据RSA解析加密串, 获取原始密码
	 * @param keyPair
	 * @param passwordBefore
	 * @param pubKey
	 * @return
	 */
	public String getPasswordByRsa(KeyPair keyPair,String passwordBefore,String pubKey){
		String passwordAfter ;
		if(keyPair == null || pubKey == null){
			logger.error("登录超时，请重刷新页面!");
			throw new RuntimeException("登录超时，请重刷新页面!");
		}else{
			RSAPrivateKey rsaPri = (RSAPrivateKey) keyPair.getPrivate();
			RSAPublicKey rsaPub = (RSAPublicKey) keyPair.getPublic();
			String pktext  = rsaPub.getModulus().toString(16);
			if(!pktext.equals(pubKey)){//页面的公钥跟本地Session不一致
				logger.error("页面公钥跟会话公钥不一致!\r\n页面pk="+pubKey+"\r\n会话pk="+pktext);
				throw new RuntimeException("页面公钥跟会话公钥不一致!请重新刷新页面!");
			}
			//String pritext  = rsaPri.getPrivateExponent().toString(16);
			byte[] en_result = new BigInteger(passwordBefore, 16).toByteArray();
			try {
				byte[] de_result = RSAUtil.decrypt(rsaPri,en_result);
			    passwordAfter = new String(de_result);
			} catch (Exception e) {
				logger.error("解密异常");
				throw new RuntimeException("密码读取失败，请刷新页面或联系管理员");
			}
		}
        return passwordAfter;
	}

	public long register(Members members){
		String oldPassword = members.getPassword();
		members.setPassword(CipherUtil.generatePassword(oldPassword));

		Date date = new Date();
		members.setCountry("中国");
		members.setRegdate(date);
		members.setRoleType(ROLE_TYPE.Normal.getValue());
		members.setRoleId(3);
		members.setCredits(0);
		members.setLevel(0);
		members.setExtcredits1(0);
		members.setExtcredits2(0);
		members.setExtcredits3(0);
		members.setExtcredits4(0);
		members.setExtcredits5(0);
		members.setExtcredits6(0);
		members.setExtcredits7(0);
		members.setExtcredits8(0);
		members.setValidateType(true);
		members.setValidateCheck(false);
		members.setLogincount(1);
		members.setLoginip("");
		members.setLogindate(date);
		members.setValidateCardId("");
		members.setGender(false);

		boolean result = insert(members);
		System.out.println("insert result:"+result);
		if(result){
//			UserServiceClient.getInstance().saveMembers(members.getUsername(), members);
			MembersService.getInstance().saveMembers(members);
			return members.getUid();
		}else{
			logger.error("创建用户失败");
			throw new RuntimeException("创建用户失败，请联系管理员");
		}
	}


	/**
	 * 插入用户
	 * @param m
	 */
	public boolean insert(Members m){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", m, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			insertNosql(m);
			saveMembersES(m.getUid(), 1);
			return true;
		}else{
			return false;
		}
	}

	private void saveMembersES(long uid, int type) {
		try {
			UserServiceClient.getInstance().saveAsyncTasks(AsyncTaskConstants.MEMBERS_ASYNC_TASK_KEY, StockUtil.joinString("^", uid, type));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void insertNosql(Members m){
		//写入Nosql
		String key = String.valueOf(m.getUid());
		logger.info(key);
		try {
//			Map map = BeanUtils.describe(m);
			Map map = NosqlBeanUtil.bean2Map(m);
			CassandraHectorGateWay.getInstance().insert(SAVE_TABLE.USER_EXT.toString(), key ,map);

			//同步到DCSS
			UserServiceClient.getInstance().updateUserExt(m.getUid(), map);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Members getMembers(Long uid){
		Members m = new Members();
		m.setUid(uid);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"select", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		if(rm.getResult() == null){
			return null;
		}
		return (Members)rm.getResult();
	}

	public Members getMembersByNickName(String nickName) {
		Members m = new Members();
		m.setNickname(nickName);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectByNickName", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		if(rm.getResult() == null){
			return null;
		}
		return (Members)rm.getResult();
	}

	/**
	 * 获取List<Members>
	 * @param m
	 */
	public List<Members> getMembers(Members m){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"select", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Members> list = (List<Members>)rm.getResult();
		return list;
	}
	/**
	 * Members
	 * @param m
	 */
	public Members getSingleMembers(Members m){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectByAccount", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		return (Members)rm.getResult();
	}

	public List<Members> getAllMembers() {
		RequestMessage req = DAFFactory.buildRequest(BASE_NS + "." + "loadmember2cache",
				StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null) {
			return null;
		}
		return (List<Members>) o;
	}

	public List<Members> getMembersByUids(Map params) {
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectMembersByUids", params, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Members> list = (List<Members>)rm.getResult();
		return list;
	}

	public Members getMembersByUserName(String username, String password){
//		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance().getCacheImpl();
//		Cache cache = cimpl.getCache(CacheUtil.getCacheName(new Members().getDataType()));//"testCacheForUser");
//		cache.registerCacheWriter(new UserCacheWriter());
//		Object value = cache.get(username);
//
//		//TODO 联调 DCSS的用户接口测试代码  密码跟DCSS不一致时，返回new Members();
//		Members m2 = UserServiceClient.getInstance().getMemberByUserName(username,"mima");
//		//DCSS 没有启动
//		if(m2 == null){
//			m2 = new Members();
//		}
//		m2.setUid(9l);
//		Object ue = UserServiceClient.getInstance().getUserExtByUid(m2.getUid());
//		HashMap<String,Object> hashmap = new HashMap<String,Object> ();
//		hashmap.put("nickname", "昵称测试");
//		hashmap.put("article_counts", 10000);
//		UserServiceClient.getInstance().updateUserExt(9l, hashmap);
//		ue = UserServiceClient.getInstance().getUserExtByUid(m2.getUid());
//
//		List list1 = UserServiceClient.getInstance().getStockList(m2.getUid());
//		List list2 = UserServiceClient.getInstance().getOrderList(m2.getUid());


		Members cacheMembers = UserServiceClient.getInstance().getMemberByUserName(username, password);
//
		if (cacheMembers == null || cacheMembers.getUid() == null || cacheMembers.getUid() <= 0) {
			Members queryMembers = new Members();
			queryMembers.setUsername(username);
			List<Members> list = getMembers(queryMembers);
			if(list != null && list.size() !=0 ){
				Members members = list.get(0);
				if(members != null) {
//					UserServiceClient.getInstance().saveMembers(members.getUsername(), members);
					MembersService.getInstance().saveMembers(members);
				}

				if(password.equals(members.getPassword())) {
					return members;
				} else {
					return null;
				}
			}
		}

		return cacheMembers;
	}

	public Members getMembersByAccount(String account){
		Members cacheMembers = UserServiceClient.getInstance().getMemberByAccount(account);
		if (cacheMembers == null || cacheMembers.getUid() == null || cacheMembers.getUid() <= 0) {
			Members queryMembers = new Members();
			queryMembers.setUsername(account);
			queryMembers.setEmail(account);
			queryMembers.setPhone(account);
//			List<Members> list = getMembers(queryMembers);
			Members members = getSingleMembers(queryMembers);
			if(members != null){
				MembersService.getInstance().saveMembers(members);
				return members;
			}
		}

		return cacheMembers;
	}
	
	//管理员登录 直接查询数据库 
	public Members getAdminMembersByAccount(String account){
			Members queryMembers = new Members();
			queryMembers.setUsername(account);
			queryMembers.setEmail(account);
			queryMembers.setPhone(account);
			Members members = getSingleMembers(queryMembers);
			return members;
	}

	public List<Members> getMembersByTagFromCache(String tag){
		Object value = LCEnter.getInstance().get(tag,
				CacheUtil.getCacheName(StockConstants.DATA_TYPE_MEMBER));
		if (value == null) {
			return null;
		}
		return ((List<Members>) value);
	}

	/**
	 *  唐斌奇修改 支持p_serach.html 分页异步查询
	 * @param m
	 * @return
	 */
	public List<Members> getMembersByPage(Map m){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectList", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Members> list = (List<Members>)rm.getResult();
		return list;
	}

	/**
	 *  唐斌奇修改 支持p_serach.html 分页异步查询
	 */
	public int selectCount(Members m) {
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectCount", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		int i = (Integer)rm.getResult();
		return i;
	}


	public boolean updateMembers(Members m){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"updateByPrimaryKey", m, StockConstants.common);
		String retrunCode = dbAgent.modifyRecord(req).getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			String key = String.valueOf(m.getUid());
			try {
				//Map map = BeanUtils.describe(m);
				Map map = NosqlBeanUtil.bean2Map(m);
				CassandraHectorGateWay.getInstance().insert(SAVE_TABLE.USER_EXT.toString(), key ,map);
			} catch (Exception e) {
				e.printStackTrace();
			}
			saveMembersES(m.getUid(), 2);
			return true;
		}else{
			return false;
		}
	}

	/**
	 * @param m
	 * @return
	 */
	public boolean checkIsExist(Members m){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"select", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List list = (List)rm.getResult();
		if(list == null ||list.size() ==0)return false;
		else return true;
	}

	public List<Members> getMembers(long uid,int count){
		Map m = new HashMap();
		m.put("uid", uid);
		m.put("count", count);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectStartCount", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<Members> list = (List<Members>)rm.getResult();
		return list;
	}

	private static CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();

	public Invite getInviteByKey(String inviteKey){
		Invite invite = null;
		try{
			String[] columns = ch.getColumns(Invite.class);
			Map<String, String> getMap = ch.get("invite",inviteKey,columns);
			invite = new Invite();
			//BeanUtils.populate(invite, getMap);
			NosqlBeanUtil.map2Bean(invite, getMap);
		}catch (Exception e) {
			logger.error(""+e);
		}
		return invite;
	}

	public void activateInviteByKey(String inviteKey){
		Map<String, String> inviteMap = new HashMap<String,String>();
		inviteMap.put("activate", "true");
		ch.insert("invite", inviteKey, inviteMap);
	}

	public String getInviteKeyByPhone(String phone){
		String reStr = "";
		int radix = 35;
		try{
			long phoneNum = Long.valueOf(phone);
			String tmpStr = Long.toString(phoneNum,radix);
			String inviteKey = new StringBuffer(tmpStr).reverse().toString().toUpperCase();
			Map<String, String> getMap = ch.get("invite",inviteKey,new String[]{"key"});
			reStr = getMap.get("key");
		}catch (Exception e) {
			logger.error("该phone在库中没有"+e);
		}
		return reStr;
	}

	public String inviteByPhones(long uid,String phones){
		if(phones == null){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		String[] phoneArr = phones.split(",");
		long time = System.currentTimeMillis();
		for(String phone : phoneArr){
			try{
				//过滤掉号码中无效的字符
				phone = phone.replace(" ", "").replace("-", "");
				long phoneNum = Long.valueOf(phone);
				Invite invite = generateInviteByPhone(uid,phoneNum,time);
				if(invite != null){
					sb.append(phone).append(":").append(invite.getKey()).append(",");
				}
			}catch(Exception e){
				logger.error("手机号码异常="+e);
			}
		}
		return sb.toString();
	}

	private Invite generateInviteByPhone(long uid,long phoneNum,long time){
		int radix = 35;
		String tmpStr = Long.toString(phoneNum,radix);
		String inviteKey = new StringBuffer(tmpStr).reverse().toString().toUpperCase();
		Invite invite = new Invite();
		invite.setKey(inviteKey);
		invite.setTime(time);
		invite.setStr(String.valueOf(phoneNum));
		invite.setType(0);
		invite.setUid(uid);
		Map<String, String> getMap = new HashMap<String,String>();
		try{
			getMap = ch.get("invite",inviteKey,new String[]{"key","activate"});
		}catch (Exception e) {
			logger.error("Nosql表invite不存在="+e);
		}
		if(getMap.isEmpty() == false){
			if("false".equals(getMap.get("activate"))){
				logger.warn("手机号"+phoneNum+"已邀请过，继续邀请！");
				return invite;
			}else{
				logger.error("手机号"+phoneNum+"已注册！");
				return null;
			}
		}
		try {
			//写Nosql
			@SuppressWarnings("unchecked")
			Map<String, String> inviteMap = NosqlBeanUtil.bean2Map(invite);//BeanUtils.describe(invite);
			ch.insert("invite",inviteKey, inviteMap);
		} catch (Exception e) {
			logger.error("BeanUtils.describe异常="+e);
		}

		//TODO 发送短信


		return invite;
	}

	//此方法未实现，请实现，加缓存中
	public Members getUserByUidFromCache(String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	public void doCompanyUser(USubject us){
		try {
			if (us.getType() == 0 && us.getUid() == 0) {
				try {
					Members members = new Members();
					//需要过滤 空格，特别是港股"WING ON CO" 需要变成WingOnCo
					String companyBasicName;
					StringBuffer nameBuf = new StringBuffer();
					Matcher m = pattern.matcher(us.getName());
					while (m.find()) {
						String str = m.group();
						if (str.length() > 1) {
							str = str.substring(0, 1).toUpperCase()
									+ str.substring(1).toLowerCase();
						} else if (str.length() == 1) {
							str = str.substring(0, 1).toUpperCase();
						}
						nameBuf.append(str);
					}
					companyBasicName = nameBuf.toString() + "("
							+ us.getUidentify() + ")";

					members.setNickname(companyBasicName);
					members.setUsername(us.getUidentify());
					//因为company缓存 加载在后面
//					Company c = CompanyService.getInstance()
//							.getCompanyByCodeFromCache(us.getUidentify());
////					if (c != null && StringUtil.isEmpty(c.getF012v()) == false) { //邮箱没有的是预披露的公司，待上市
//						members.setEmail(c.getF012v());
//					} else {
						//没有提供邮箱的话，生成一个虚拟邮箱
						members.setEmail(us.getUidentify() + "@toujixia.com");
//					}
					members.setTag("上市公司");
					members.setDesc("上市公司:" + us.getName()
							+ "。免责声明:此账号用于发布公司提醒和公司新闻,仅供参考,投资者风险自负。");
					members.setUcuid("上市公司:" + us.getName());
					members.setPassword("yfzx654321");
					//公司时，需要加新增一个模拟用户
					Members members2 = SqlWithTrasitionUtil
							.exeCallback(new ISqlCallback<Members>() {
								USubject us;
								Members members;

								private ISqlCallback<Members> set(USubject us,
										Members members) {
									this.us = us;
									this.members = members;
									return this;
								}

								public Members exeMybatis() {
									Members oldMembers = MembersService.getInstance().getMembersByAccount(members.getUsername());
									if(oldMembers == null){
										long uid = MembersService.getInstance()
												.register(members);
										if (uid > 0l) {
											us.setUid(uid);
											boolean update = USubjectService
													.getInstance().updateUid(us);
											if (update) {
												logger.info(us.getName()
														+ "系统账号创建成功");
											}
											members = MembersService.getInstance()
													.getMembers(uid);
											members.setState(0);
											MembersService.getInstance()
													.updateMembers(members);
											return members;
										} else {
											return null;
										}
									}else{
										if (oldMembers.getUid() > 0l) {
											us.setUid(oldMembers.getUid());
										}
										return oldMembers;
									}
								}
							}.set(us, members));
					if (members2 == null) {
						logger.error("创建账号失败");
					}
				} catch (Exception e) {
					logger.error("创建账号失败", e);
				}
			}
			else if (us.getType() == 0 && us.getUid() != 0) {
				try {
					Members members = new Members();
					members.setUsername(us.getUidentify());
					List<Members> mlist = MembersService.getInstance()
							.getMembers(members);
					if (mlist != null && mlist.size() > 0) {
						Members oldMembers = mlist.get(0);//getMembersByUserName(us.getUidentify());
						//需要过滤 空格，特别是港股"WING ON CO" 需要变成WingOnCo
						String companyBasicName;
						StringBuffer nameBuf = new StringBuffer();
						Matcher m = pattern.matcher(us.getName());
						while (m.find()) {
							String str = m.group();
							if (str.length() > 1) {
								str = str.substring(0, 1).toUpperCase()
										+ str.substring(1).toLowerCase();
							} else if (str.length() == 1) {
								str = str.substring(0, 1).toUpperCase();
							}
							nameBuf.append(str);
						}
						companyBasicName = nameBuf.toString() + "("
								+ us.getUidentify() + ")";
						String oldNickname = oldMembers.getNickname();
						//存在用户名修改的情况
						if (companyBasicName.equals(oldNickname) == false
								|| "上市公司".equals(oldMembers.getTag()) == false) {
							oldMembers.setNickname(companyBasicName);
							oldMembers.setTag("上市公司");
							oldMembers.setDesc("上市公司:" + us.getName()
									+ "。免责声明:此账号用于发布公司提醒和公司新闻,仅供参考,投资者风险自负。");
							oldMembers.setUcuid("上市公司:" + us.getName());
							MembersService.getInstance().updateMembers(
									oldMembers);
						}
					}
				} catch (Exception e) {
					logger.error("名称修改失败", e);
				}
			}
		} catch (Exception e) {
			logger.error("doCompanyUser异常", e);
		}
	}

//	public Members getMembersByAccount(String account, String password) {
//		Members cacheMembers = UserServiceClient.getInstance().getMemberByUserName(account, password);
//		if (cacheMembers == null || cacheMembers.getUid() == null || cacheMembers.getUid() <= 0) {
//
//			Members queryMembers = new Members();
//			queryMembers.setUsername(account);
//			queryMembers.setEmail(account);
//			queryMembers.setPhone(account);
//			Members members = getSingleMembers(queryMembers);
//			if(members != null){
//				if(members != null) {
//					UserServiceClient.getInstance().saveMembersByAccount(account, members);
//					UserServiceClient.getInstance().saveMembers(queryMembers.getUsername(), members);
//				}
//				if(password.equals(members.getPassword())) {
//					return members;
//				} else {
//					return null;
//				}
//			}
//		}
//
//		return cacheMembers;
//	}

//	public Members getMembersByAccount(String account) {
//		Members cacheMembers = UserServiceClient.getInstance().getMemberByAccount(account);
//		if (cacheMembers == null || cacheMembers.getUid() == null || cacheMembers.getUid() <= 0) {
//			Members queryMembers = new Members();
//			queryMembers.setUsername(account);
//			queryMembers.setEmail(account);
//			queryMembers.setPhone(account);
//			cacheMembers = getSingleMembers(queryMembers);
//			if(cacheMembers != null){
//				UserServiceClient.getInstance().saveMembersByAccount(account, cacheMembers);
//				UserServiceClient.getInstance().saveMembers(queryMembers.getUsername(), cacheMembers);
//			}
//		}
//		return cacheMembers;
//	}

	public void saveMembers(Members members) {
		if(StringUtils.isNotBlank(members.getUsername())) {
			UserServiceClient.getInstance().saveMembers(members.getUsername(), members);
		}
		if(StringUtils.isNotBlank(members.getEmail())) {
			UserServiceClient.getInstance().saveMembers(members.getEmail(), members);
		}
		if(StringUtils.isNotBlank(members.getPhone())) {
			UserServiceClient.getInstance().saveMembers(members.getPhone(), members);
		}
	}

	/**
	 *
	 * @param code 第三方code
	 * @param wxVersion 0:老版本 （股今），1新版本（爱股说）
	 * @return
	 */
	public ThirdUser getSnsInfoFromCode(String code,int wxVersion){
		ThirdUser th = new ThirdUser();
		String access_token = null;
		String expires_in = null;
		String openid = null;
		String unionid = null;
		String appid = ConfigCenterFactory.getString("third.sns_new_weixin_appid", "wxac61e167fe7d945c");
		String secret = ConfigCenterFactory.getString("third.sns_new_weixin_secret", "c67dc834dcff9017bde531177c6a99e3");
		try {
			if(wxVersion==0){//old version
				 appid = ConfigCenterFactory.getString("third.sns_weixin_appid", "wx65c6ca83ab0a1007");
				 secret = ConfigCenterFactory.getString("third.sns_weixin_secret", "18fa506225953baac2f7d746fd50ba91");
			}
			String url = ConfigCenterFactory.getString("stock_zjs.sns_weixin_req_access_token_url", "https://api.weixin.qq.com/sns/oauth2/access_token");
			StringBuilder s = new StringBuilder();
			s.append(url).append("?appid=").append(appid).append("&secret=").append(secret).append("&code=").append(code).append("&grant_type=authorization_code");
			String content = HttpUtil.sendGetRequest(s.toString());
			if(StringUtils.isNotBlank(content)) {
				JSONObject jsonObj = JSONObject.fromObject(content);
				String errcode = jsonObj.get("errcode") == null ? "" :jsonObj.getString("errcode");
				if(StringUtils.isBlank(errcode)) {
					 access_token = jsonObj.getString("access_token");
					 expires_in = jsonObj.getString("expires_in");
					 openid = jsonObj.getString("openid");
					 unionid = jsonObj.getString("unionid");
					if(!StringUtils.isEmpty(unionid)){
						th.setOpenId(unionid);
					}else{
						th.setOpenId(openid);
					}
					th.setExpireStr(expires_in);
					th.setToken(access_token);
				}
			}
			String url2 = "https://api.weixin.qq.com/sns/userinfo";
			StringBuilder s2 = new StringBuilder();
			s2.append(url2).append("?").append("access_token=").append(access_token).append("&openid=").append(openid);
			String content2 = HttpUtil.sendGetRequest(s2.toString());
			if(StringUtils.isNotBlank(content2)) {
				JSONObject jsonObj = JSONObject.fromObject(content2);
				String errcode = jsonObj.get("errcode") == null ? "" :jsonObj.getString("errcode");
				if(StringUtils.isBlank(errcode)) {
					String nickname = new String(jsonObj.getString("nickname").getBytes("iso-8859-1"), "utf8");
					th.setNickname(nickname);
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return th;
	}

	/**
	 *  字符串解析出密码
	 * @param baseStr
	 * @return
	 */
	public String resolvePassWord(String baseStr){
		String password = null;
		try {
			String publicKey = ConfigCenterFactory.getString("sms.public_key", "yfzx");
			byte[] by = Base64.decode(baseStr);
			String pass = new String(by,"utf-8");
			int size = pass.length();
			int pszie = publicKey.length();
			if(size<=pszie){
				return null;
			}
			pass = pass.substring(0,size-pszie);
			String key = pass.substring(size-pszie,size);
			if(publicKey.equals(key)){
				password = pass;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return password;
	}

	public boolean delMembersCache(String account) {
		return UserServiceClient.getInstance().delMemberCache(account);
	}
}