package com.yfzx.service.msgpush;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.json.JSONException;
import org.apache.struts2.json.JSONUtil;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ApnsService;
import com.notnoop.exceptions.NetworkIOException;
import com.stock.common.constants.AsyncTaskConstants;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.USubject;
import com.stock.common.model.msgpush.MobileToken;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.model.share.UserExt;
import com.stock.common.msg.UserMsg;
import com.stock.common.util.CookieUtil;
import com.stock.common.util.StockCacheUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.PushClient;
import com.yfzx.service.client.UserServiceClient;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.core.util.BaseUtil;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class MobileMsgPushService {

	private static final String NS = "com.stock.portal.dao.msgpush.MobileTokenDao";

	private static Logger logger = LoggerFactory.getLogger(MobileMsgPushService.class);

	private MobileMsgPushService() {}

	private static MobileMsgPushService instance = new MobileMsgPushService();

	private static DBAgent dbAgent = DBAgent.getInstance();

	private static ApnsService apnsService = null;

	//android mqtt topic
	private static final String USER_PRIVATE_MSG_TOPIC = "/user/privatemsg/";//用户私信topic

	private static final String COMPANY_PRIVATE_MSG_TOPIC = "/company/privatemsg/";//公司私信topic

	private static final String USER_STOCK_CHANCE_MSG_TOPIC = "/user/stockchance/";

	private static final int COMPANY_PRIVATEMSG_TYPE = 7;

	private static final int STOCK_CHANCE_MSG_TYPE = 10;

	private static final int USER_STOCK_SU_TYPE = -1;

	private static final int TOPIC_FOCUS_TYPE = -2;

	private static final int QOS2 = 2;

	static {
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				logger.info("========================>replace_old_ios_certificate");
				if(apnsService != null) {
					apnsService.stop();
					apnsService = null;
					logger.info("========================>close_apns_service");
				}
				initApnsService();
			}
		});
		initApnsService();
	}

	public String getIOSChanceNotifyMsg(UserMsg um) {
		String title = um.getAttr("title");
		String summary = um.getAttr("summary");
		String uuid = um.getAttr("uuid");

		title = MobileMsgPushService.getFilterStr(title);
		summary = MobileMsgPushService.getFilterStr(summary);

		String body = "";
		if(StringUtils.isNotBlank(title)) {
			body = StockUtil.joinString("\n", title, summary);
		} else {
			body = summary;
		}
		return APNS.newPayload().alertBody(body)
				.badge(1)
				.sound("default")
				.customField("uuid", uuid)
				.customField("type", STOCK_CHANCE_MSG_TYPE)
				.shrinkBody()
				.build();
	}

	private static void initApnsService() {
		try {//ApnsDelegate  messageSendFailed  ApnsServiceBuilder  withDelegate(ApnsDelegate delegate)
			//PushDevelopment.p12   stock_push_product.p12
			String cf = ConfigCenterFactory.getString("stock_zjs.ios_certificate", "stock_push_product.p12");
			String fileName = BaseUtil.getConfigPath("ios_certificate/" + cf);
			String ios_cert_passs =  ConfigCenterFactory.getString("stock_zjs.ios_certificate_pass", "123456");
			InputStream inputStream = new FileInputStream(fileName);

			if("stock_push_development.p12".equals(cf)) {
				//sandbox
				apnsService = APNS.newService().withCert(inputStream, ios_cert_passs).withSandboxDestination().asQueued().build();//withReconnectPolicy(new ReconnectPolicies.Always()).build();
			} else {
				//production
				apnsService = APNS.newService().withCert(inputStream, ios_cert_passs).withProductionDestination().asQueued().build();//.withReconnectPolicy(new ReconnectPolicies.Always()).build();
			}
			apnsService.start();
			if(inputStream != null) {
				inputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static MobileMsgPushService getInstance() {
		return instance;
	}

	public static ApnsService getApnsService() {
		return apnsService;
	}

	public Long insert(MobileToken iosToken) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "insert", iosToken, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			MobileToken obj = (MobileToken)rm.getResult();
			return obj.getId();
		}
		return Long.valueOf(0);
	}

	public List<MobileToken> getmtList(String token) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("device_token", token);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getmtList", m, StockConstants.common);
		ResponseMessage res = dbAgent.queryForList(req);
		String retrunCode = res.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<MobileToken>)res.getResult();
		}
		return null;
	}

	/**
	 *
	 * @param mobileType 0：IOS   1：Android 2：all
	 * @return
	 */
//	public List<MobileToken> getAllMobileTokens(int mobileType) {
//		MobileToken m = new MobileToken();
//		if(mobileType != 2) {
//			m.setMobileType((byte)mobileType);
//		}
//
//		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getAllMobileTokens", m, StockConstants.common);
//		ResponseMessage rm = dbAgent.queryForList(req);
//		String retrunCode = rm.getRetrunCode();
//		if(BaseCodes.SUCCESS.equals(retrunCode)){
//			return (List<MobileToken>)rm.getResult();
//		} else {
//			return null;
//		}
//	}

//	public boolean update(MobileToken iosToken) {
//		RequestMessage req = DAFFactory.buildRequest(NS + "." + "update", iosToken, StockConstants.common);
//		ResponseMessage rm = dbAgent.modifyRecord(req);
//		String retrunCode = rm.getRetrunCode();
//		if(BaseCodes.SUCCESS.equals(retrunCode)){
//			return true;
//		} else {
//			return false;
//		}
//	}

	public boolean updateByPrimaryKey(MobileToken iosToken) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "updateByPrimaryKey", iosToken, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean updateByTokenUid(String token, Long uid, byte appState) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("device_token", token);
		map.put("uid", uid);
		map.put("appState", appState);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "updateByTokenUid", map, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean deleteById(Long id) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "deleteById", id, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public boolean deleteByUid(Long uid) {
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "deleteByUid", uid, StockConstants.common);
		ResponseMessage rm = dbAgent.deleteRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		} else {
			return false;
		}
	}

	public List<MobileToken> getMobileToken(Long uid) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("uid", uid);
		map.put("appState", 0);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getMobileToken", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<MobileToken>)rm.getResult();
		} else {
			return null;
		}
	}

	public Integer selectCount(Integer mobileType) {
		Map<String, Object> map = new HashMap<String, Object>();
		if(mobileType != 2) {
			map.put("mobileType", mobileType);
		}
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "selectCount", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (Integer)rm.getResult();
		} else {
			return null;
		}
	}

	public List<MobileToken> getPagingMobileTokens(Integer mobileType, Integer offset, Integer limit) {
		Map<String, Object> map = new HashMap<String, Object>();
		if(mobileType != 2) {
			map.put("mobileType", mobileType);
		}
		map.put("offset", offset);
		map.put("limit", limit);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getPagingMobileTokens", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<MobileToken>)rm.getResult();
		} else {
			return null;
		}
	}

	public MobileToken getMobileToken(Long uid, String mobileToken) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("uid", uid);
		map.put("device_token", mobileToken);
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getMobileTokenByUidToken", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (MobileToken)rm.getResult();
		} else {
			return null;
		}
	}

	public List<MobileToken> getMobileTokensByUidList(List<Long> uidList) {
		if(uidList == null || uidList.size() == 0) {
			return null;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("uids", "(" + StringUtils.join(uidList, ",") + ")");
		RequestMessage req = DAFFactory.buildRequest(NS + "." + "getMobileTokensByUidList", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return (List<MobileToken>)rm.getResult();
		} else {
			return null;
		}
	}

	public void sendCompanyPrivacyMsgToAndroid(USubject companyUsSubject, TalkMessage tm) {
		if(! StockUtil.checkMobileMsgPush()) {
			return ;
		}

		if(companyUsSubject == null || companyUsSubject.getType() != 0) {
			logger.error("send_company_private_msg, company_usubject_is_null");
			return ;
		}

		String nick = "";
		String headImg = "";
		long companyUid = companyUsSubject.getUid();
		UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(companyUid);
		if(userExt != null) {
			nick = userExt.getNickname();
			headImg = userExt.getFaceUrl();
		}
		if(StringUtils.isNotBlank(nick)) {
			nick = nick.substring(0, nick.indexOf("("));
		} else {
			logger.error("EmptyCompanyNick: " + companyUid + "  " + companyUsSubject.getUidentify());
		}
		headImg = StringUtils.isBlank(headImg) ? "" : headImg;
		long time = new Date().getTime();
		String md = tm.getAttr("md") == null ? "" : (String)tm.getAttr("md");
		if(StringUtils.isBlank(md)) {
			md = tm.getBody();
		}

		if(StringUtils.isNotBlank(md)) {
			md = getFilterContent(md);
		} else {
			logger.error("sendCompanyPrivacyMsgEmptyMd: " + companyUsSubject.getUid() + "   " + companyUsSubject.getUidentify());
			return ;
		}
		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("uid", companyUid);
		msgMap.put("nick", nick);
		msgMap.put("content", md);
		msgMap.put("type", COMPANY_PRIVATEMSG_TYPE);
		msgMap.put("time", time);
		msgMap.put("headImg", headImg);
		msgMap.put("isCompany", 1);
		MqttMessage message = new MqttMessage();
		message.setQos(ConfigCenterFactory.getInt("stock_zjs.company_msg_qos", QOS2));
		try {
			message.setPayload(JSONUtil.serialize(msgMap).getBytes());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			String topic = StockUtil.joinString("", COMPANY_PRIVATE_MSG_TOPIC,  companyUsSubject.getUidentify());
			logger.info("sendCompanyPrivacyMsgAndroidTopic: " + topic);
			PushClient.getInstance().publish(topic, message);
		} catch (Exception e1) {
			logger.info("sendCompanyPrivacyMsgToAndroidFailure: " + e1);
		}
	}

	public void sendCompanyPrivacyMsgToIOS(USubject companyUsSubject, Set<Long> uidSet, TalkMessage tm) {
		if( checkIosPushEnable() == false ){
			return;
		}
		if(companyUsSubject == null || companyUsSubject.getType() != 0) {
			logger.error("send_company_private_msg, company_usubject_is_null");
			return ;
		}

		if(uidSet == null || uidSet.size() == 0) {
			logger.error("uidSet_is_empty   " + companyUsSubject.getUid() + "   " + companyUsSubject.getUidentify());
			return ;
		}

		String nick = "";
		long companyUid = companyUsSubject.getUid();
		UserExt userExt = UserServiceClient.getInstance().getUserExtByUid(companyUid);
		if(userExt != null) {
			nick = userExt.getNickname();
		}
		if(StringUtils.isNotBlank(nick)) {
			nick = nick.substring(0, nick.indexOf("("));
		} else {
			logger.error("EmptyCompanyNick: " + companyUid + "  " + companyUsSubject.getUidentify());
		}
		String md = tm.getAttr("md") == null ? "" : (String)tm.getAttr("md");
		if(StringUtils.isBlank(md)) {
			md = tm.getBody();
		}

		if(StringUtils.isNotBlank(md)) {
			md = getFilterContent(md);
		} else {
			logger.error("sendCompanyPrivacyMsgEmptyMd: " + companyUsSubject.getUid() + "   " + companyUsSubject.getUidentify());
			return ;
		}

		String c = "";
		if(StringUtils.isNotBlank(nick)) {
			c = nick + "：" + md;
		} else {
			c = md;
		}

		String payload = APNS.newPayload().alertBody(c)
				.customField("uid", companyUid)
				.customField("nick", nick)
				.customField("type", COMPANY_PRIVATEMSG_TYPE)
				.customField("isCompany", 1)
				.shrinkBody()
				.build();

		for(Long uid : uidSet) {
			boolean isInThisDcss = StockCacheUtil.isInThisDcss(uid, StockCacheUtil.getAppIndex());
			if(isInThisDcss) {
				Set<MobileToken> t = LCEnter.getInstance().get(uid, StockUtil.getMobileTokenCacheName(uid));
				if(t != null && t.size() > 0) {
					log(t, uid);
					sendIOSMsg(t, payload);
				} else {
					logger.error("sendCompanyMsgToIOS, No Token IN Cache: " + uid);
				}
			} else {
				Set<MobileToken> t = UserServiceClient.getInstance().getMobileTokenSet(uid);
				if(t != null && t.size() > 0) {
					log(t, uid);
					sendIOSMsg(t, payload);
				} else {
					logger.error("sendCompanyMsgToIOS, No Token IN Cache: " + uid);
				}
			}
		}
	}

	private void sendIOSMsg(Set<MobileToken> tokenSet, String payload) {
		if(checkIosPushEnable() == false){
			return;
		}
		Set<String> iosTokenStrSet = new HashSet<String>();
		for(MobileToken token : tokenSet) {
			if(token != null && token.getAppState().byteValue() == 0) {
				iosTokenStrSet.add(token.getDevice_token());
			}
		}

		try {
			if(apnsService != null) {
				Collection<? extends ApnsNotification> list = apnsService.push(iosTokenStrSet, payload);
				int ios_log_switch = ConfigCenterFactory.getInt("push.ios_log_switch", 0);
				if(ios_log_switch == 1){
					for(ApnsNotification an : list){
						logger.info("IOS ApnsNotification："+ an);
					}
				}
			} else {
				logger.error("apnsService not init, no_ios_certificate !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			}
		} catch (NetworkIOException e) {
			logger.info("sendMsgToIOS:   " + e);
		}
	}

	private String getFilterContent(String content) {
		if(StringUtils.isNotBlank(content)) {
			content = StringEscapeUtils.unescapeHtml4(content);
			content = content.replaceAll("&nbsp;", " ").replaceAll("<br>", "\n").replaceAll("<br/>", "\n")
					.replaceAll("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", "[图片]")
					.replaceAll("<a(?:\\s+.+?)*?\\s+href=\"([^\"]*?)\".+>(.*?)</a>", "$1");
			return StockUtil.removeHtmlTag(content);
		}
		return "";
	}

	public void sendChanceNotifyMsgToAndroid(UserMsg um) {
		if(! StockUtil.checkMobileMsgPush()) {
			return ;
		}

		int type = 1;

		String identify = um.getAttr("identify");
		if(StringUtils.isNotBlank(identify)) {
			if(identify.endsWith(".sz") || identify.endsWith(".sh") || identify.endsWith(".hk")) {
				type = 2;
			}
		} else {
			logger.error("sendChanceNotifyMsgToAndroid: identify empty");
			return ;
		}

		String title = um.getAttr("title");
		String summary = um.getAttr("summary");
		String uuid = um.getAttr("uuid");

		title = getFilterStr(title);
		summary = getFilterStr(summary);

		String body = "";
		if(StringUtils.isNotBlank(title)) {
			body = StockUtil.joinString("\n", title, summary);
		} else {
			body = summary;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("content", body);
		map.put("uuid", uuid);
		map.put("type", STOCK_CHANCE_MSG_TYPE);

		MqttMessage message = new MqttMessage();
		message.setQos(ConfigCenterFactory.getInt("stock_zjs.chance_msg_qos", QOS2));
		try {
			message.setPayload(JSONUtil.serialize(map).getBytes());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			String topic = "";
			if(type == 1) {
				topic = StockUtil.joinString("", USER_STOCK_CHANCE_MSG_TOPIC, CookieUtil.getMD5(identify));
			} else if(type == 2) {
				topic = StockUtil.joinString("", COMPANY_PRIVATE_MSG_TOPIC, identify);
			}

			logger.info("sendUserStockChanceMsgTopic: " + topic);
			PushClient.getInstance().publish(topic, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendChanceNotifyMsgToIOS(Long uid, String payload) {
		if( checkIosPushEnable() ==false ){
			return ;
		}
		boolean isInThisDcss = StockCacheUtil.isInThisDcss(uid, StockCacheUtil.getAppIndex());

		if(isInThisDcss) {
			Set<MobileToken> tokenSet = LCEnter.getInstance().get(uid, StockUtil.getMobileTokenCacheName(uid));
			if(tokenSet != null && tokenSet.size() > 0) {
				log(tokenSet, uid);
				sendIOSMsg(tokenSet, payload);
			} else {
				logger.error("sendChanceNotifyMsgToIOS, No Token IN cache: " + uid);
			}
		} else {
			Set<MobileToken> tokenSet = UserServiceClient.getInstance().getMobileTokenSet(uid);
			if(tokenSet != null && tokenSet.size() > 0) {
				log(tokenSet, uid);
				sendIOSMsg(tokenSet, payload);
			} else {
				logger.error("sendChanceNotifyMsgToIOS, No Token IN cache: " + uid);
			}
		}
	}

	private void log(Set<MobileToken> tokenSet, Long uid) {
		int log_switch = ConfigCenterFactory.getInt("push.push_log_switch", 0);
		if(log_switch == 1) {
			StringBuilder s = new StringBuilder();
			for(MobileToken token : tokenSet) {
				s.append(token.getDevice_token()).append("|").append(token.getAppState()).append("^");
			}
			logger.info("sendChanceNotifyMsgToIOS: " + uid + "      " + s);
		}
	}

	private static String getFilterStr(String content) {
		if(StringUtils.isNotBlank(content)) {
			content = StringEscapeUtils.unescapeHtml4(content);
			content.replaceAll("&nbsp;", " ").replaceAll("<p>", "").replaceAll("</p>", "\n").replaceAll("<br>", "\n").replaceAll("<br/>", "\n")
					.replaceAll("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", "[图片]")
					.replaceAll("<a(?:\\s+.+?)*?\\s+href=\"([^\"]*?)\".+>(.*?)</a>", "$1");
			return StockUtil.removeHtmlTag(content);
		}
		return "";
	}

	//用户私信
	public void sendUserPrivacyMsg(TalkMessage tm, int type) {
		if(tm == null) {
			return ;
		}
		String content = tm.getBody();
		String nick = "";
		String headImg = "";
		Long uid = Long.valueOf(tm.getS());
		UserExt ut = UserServiceClient.getInstance().getUserExtByUid(uid);
		if(ut != null) {
			nick = ut.getNickname();
			headImg = ut.getFaceUrl();
		}
		String c = "";

		if(StringUtils.isNotBlank(content)) {
			content = StockUtil.emotion2Tag(content);
			content = StringEscapeUtils.unescapeHtml4(content);
			content = content.replaceAll("&nbsp;", " ").replaceAll("<br>", "\n").replaceAll("<br/>", "\n").replaceAll("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", "[图片]");
			content = StockUtil.removeHtmlTag(content);
		}
		if(StringUtils.isNotBlank(nick)) {
			c = StockUtil.joinString(":", nick, content);
		} else {
			c = content;
		}
		headImg = StringUtils.isBlank(headImg) ? "" : headImg;
		long time = new Date().getTime();

		Map<String, Object> msgMap = new HashMap<String, Object>();
		msgMap.put("uid", uid);
		msgMap.put("nick", nick);
		msgMap.put("content", content);
		msgMap.put("type", type);
		msgMap.put("time", time);
		msgMap.put("headImg", headImg);
		msgMap.put("isCompany", 0);
		MqttMessage message = new MqttMessage();
		message.setQos(ConfigCenterFactory.getInt("stock_zjs.user_msg_qos", QOS2));
		try {
			message.setPayload(JSONUtil.serialize(msgMap).getBytes());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			String topic = StockUtil.joinString("", USER_PRIVATE_MSG_TOPIC, tm.getD());
			logger.info("sendUserPrivacyMsgTopic: " + topic);
			PushClient.getInstance().publish(topic, message);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//IOS
		if( checkIosPushEnable() == false ){
			return;
		}
		Set<MobileToken> tokenSet = null;
		Long destUid = Long.valueOf(tm.getD());
		boolean isInThisDcss = StockCacheUtil.isInThisDcss(destUid, StockCacheUtil.getAppIndex());
		if(isInThisDcss) {
			tokenSet = LCEnter.getInstance().get(destUid, StockUtil.getMobileTokenCacheName(destUid));
		} else {
			tokenSet = UserServiceClient.getInstance().getMobileTokenSet(destUid);
		}

		if(tokenSet == null || tokenSet.size() == 0) {
			logger.error("NoRelatedIOSTokensInCache !!!!!!!!!!!!!!" + tm.getD());
			return ;
		}

		String payload = APNS.newPayload().alertBody(c)
				.customField("uid", uid)
				.customField("nick", nick)
				.customField("type", type)
				.customField("isCompany", 0)
				.shrinkBody()
				.build();

		if(tokenSet != null && tokenSet.size() > 0) {
			log(tokenSet, destUid);
			sendIOSMsg(tokenSet, payload);
		}
	}

	/**
	 *
	 * @param suid
	 * @param companycode
	 * @param status
	 * @param type 1自选股 2话题
	 * @return
	 */
	public boolean sendSubscribeUnSubscribeMsg(long suid, String companycode, int status, int type) {//
		Map<String, Object> msgMap = new HashMap<String, Object>();
		//关注用户：
		MqttMessage message = new MqttMessage();
		msgMap.put("code", companycode);
		msgMap.put("status", status);
		if(type == 1) {
			message.setQos(ConfigCenterFactory.getInt("stock_zjs.stock_msg_qos", QOS2));
			msgMap.put("type", USER_STOCK_SU_TYPE);
		} else if(type == 2) {
			message.setQos(ConfigCenterFactory.getInt("stock_zjs.topic_msg_qos", QOS2));
			msgMap.put("type", TOPIC_FOCUS_TYPE);
		}
		try {
			message.setPayload(JSONUtil.serialize(msgMap).getBytes());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			return PushClient.getInstance().publish(StockUtil.joinString("", USER_PRIVATE_MSG_TOPIC, suid), message);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 *
	 * @param uid
	 * @param stockcode
	 * @param focusType
	 * @param type  1自选股 2话题
	 */
	public void saveSubscribeUnSubscribeMsg(Long uid, String stockcode, int focusType, int type) {
		try {
			String item = "";
			if(type == 1) {
				item = StockUtil.joinString("^", uid, stockcode, focusType, type);
			} else if(type == 2) {
				item = StockUtil.joinString("^", uid, CookieUtil.getMD5(stockcode), focusType, type);
			}
			UserServiceClient.getInstance().saveAsyncTasks(AsyncTaskConstants.STOCKS_ASYNC_TASK_KEY, item);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getDateFormatter(){
		Date date=new Date();
		DateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(date);
	}
	
	private boolean checkIosPushEnable(){
		boolean iosPushEnable=ConfigCenterFactory.getInt("stock_zjs.ios_push_enable",1)==1;
		if(!iosPushEnable){
			logger.info("ios push fuction  is close");
		}
		return iosPushEnable;
	}

}
