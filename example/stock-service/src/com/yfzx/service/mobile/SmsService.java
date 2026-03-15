package com.yfzx.service.mobile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yfzx.service.db.user.MembersService;
import com.yz.configcenter.ConfigCenterFactory;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

/**
 * <dependency> <groupId>org.apache.axis</groupId> <artifactId>axis</artifactId>
 * <version>1.4</version> </dependency>
 * 
 * <dependency> <groupId>javax.xml.rpc</groupId>
 * <artifactId>javax.xml.rpc-api</artifactId> <version>1.1.1</version>
 * </dependency>
 * 
 * <dependency> <groupId>commons-discovery</groupId>
 * <artifactId>commons-discovery</artifactId> <version>0.5</version>
 * </dependency> <dependency> <groupId>wsdl4j</groupId>
 * <artifactId>wsdl4j</artifactId> <version>1.6.3</version> </dependency>
 *
 * 调用SmsService hl = SmsService.getInstance(); //register("账号",
 * "密码","公司全称","公司简称","公司地址","联系电话","联系人","email","传真","邮编","联系手机") //
 * hl.register("YCWL-CRM-0100-CCVWFA", "76429888", "深圳市盈富在线信息技术有限公司", "盈富在线",
 * "深圳市宝安区" // , "13316853840", "唐斌奇", "254107347@qq.com", "", "",
 * "13316853840"); 
 * String str = SmsService.getInstance().sendEx("YCWL-CRM-0100-CCVWFA", "76429888","您的新密码是"+i+"【投机侠】", "13316853840", ""); System.out.println(str);
 */
public class SmsService {

	public static String strURL = "http://211.99.191.148";
	static Logger logger = LoggerFactory.getLogger(SmsService.class);
	// safe URL
	// public static String strURL = "https://211.99.191.149:8443";
	// https://211.99.191.149:8443/mms/services/info?wsdl
	private final static int  CHECK_NUM = 3; //验证码
	private final static int  FIND_PWD = 2; //找回密码
	private final static int  INIT_PWD = 1; //初始密码
	public static String nameSpace = "http://info.pica.com";
	private String SHCNN_URI_SEND = "/mms/services/info";
	private static SmsService instance = new SmsService();

	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	String date = sdf.format(new Date());

	private SmsService() {
	}

	/**
	 * @return
	 */
	public static synchronized SmsService getInstance() {
		return instance;
	}

	// 注册方法做账号激活使用，账号激活后就不再使用。
	// regcode register (only use one times)
	public String register(String Regcode, String Pwd, String enterpriseNAME,
			String CSNAME, String Address, String LINKTEL, String LINKMAN,
			String EMAIL, String FAX, String POSTCODE, String MOBILETEL) {
		String result = "";

		try {

			Service services = new Service();
			Call call1 = null;
			call1 = (Call) services.createCall();

			call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));

			call1.setOperationName(new QName(nameSpace, "register"));
			call1.addParameter(new QName(nameSpace, "in0"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in1"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in2"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in3"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in4"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in5"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in6"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in7"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in8"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in9"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in10"),
					XMLType.XSD_STRING, ParameterMode.IN);

			call1.setReturnType(XMLType.XSD_STRING);

			call1.setUseSOAPAction(true);
			call1.setSOAPActionURI("http://zzwx/example");

			result = (String) call1.invoke(new Object[] { Regcode, Pwd,
					enterpriseNAME, CSNAME, Address, LINKTEL, LINKMAN, EMAIL,
					FAX, POSTCODE, MOBILETEL });
			return result;

		}

		catch (Exception ex) {
			ex.printStackTrace();

			System.out.println("ex----");
			return "-100";
		}

	}

	// get regcode balance
	private String getbalance(String Regcode, String Pwd) {
		String result = "";

		try {

			Service services = new Service();
			Call call1 = null;
			call1 = (Call) services.createCall();

			call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));

			call1.setOperationName(new QName(nameSpace, "getbalance"));
			call1.addParameter(new QName(nameSpace, "in0"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in1"), XMLType.XSD_STRING,
					ParameterMode.IN);

			call1.setReturnType(XMLType.XSD_STRING);

			call1.setUseSOAPAction(true);
			call1.setSOAPActionURI("http://zzwx/example");

			result = (String) call1.invoke(new Object[] { Regcode, Pwd });
			return result;

		}

		catch (Exception ex) {
			ex.printStackTrace();

			System.out.println("ex----");
			return "-100";
		}

	}

	// 获取手机回复的短信
	// get phone reply message.
	private String getmo(String Regcode, String Pwd) {
		String result = "";

		try {

			Service services = new Service();
			Call call1 = null;
			call1 = (Call) services.createCall();

			call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));

			call1.setOperationName(new QName(nameSpace, "getmo"));
			call1.addParameter(new QName(nameSpace, "in0"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in1"), XMLType.XSD_STRING,
					ParameterMode.IN);

			call1.setReturnType(XMLType.XSD_STRING);

			call1.setUseSOAPAction(true);
			call1.setSOAPActionURI("http://zzwx/example");

			result = (String) call1.invoke(new Object[] { Regcode, Pwd });
			return result;

		}

		catch (Exception ex) {
			ex.printStackTrace();

			System.out.println("ex----");
			return "-100";
		}

	}

	// 获取手机是否收到的状态回执报告。
	// get phone receive state report.
	private String getReport(String Regcode, String Pwd) {
		String result = "";

		try {

			Service services = new Service();
			Call call1 = null;
			call1 = (Call) services.createCall();

			call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));

			call1.setOperationName(new QName(nameSpace, "getReport"));
			call1.addParameter(new QName(nameSpace, "in0"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in1"), XMLType.XSD_STRING,
					ParameterMode.IN);

			call1.setReturnType(XMLType.XSD_STRING);

			call1.setUseSOAPAction(true);
			call1.setSOAPActionURI("http://zzwx/example");

			result = (String) call1.invoke(new Object[] { Regcode, Pwd });
			System.out.println(result);
			return result;

		}

		catch (Exception ex) {
			ex.printStackTrace();

			System.out.println("ex----");
			return "-100";
		}

	}

	// 获取通道的普通短信发送的长度（接口使用者可以不实现该方法，后台根据发送账号做了自动适配）
	// Interface user no use.
	private String getchannelcheck(String Regcode, String Pwd, String smscontent) {
		String result = "";

		try {

			Service services = new Service();
			Call call1 = null;
			call1 = (Call) services.createCall();

			call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));

			call1.setOperationName(new QName(nameSpace, "getchannelcheck"));
			call1.addParameter(new QName(nameSpace, "in0"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in1"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in2"), XMLType.XSD_STRING,
					ParameterMode.IN);

			call1.setReturnType(XMLType.XSD_STRING);

			call1.setUseSOAPAction(true);
			call1.setSOAPActionURI("http://zzwx/example");

			result = (String) call1.invoke(new Object[] { Regcode, Pwd,
					smscontent });
			return result;

		}

		catch (Exception ex) {
			ex.printStackTrace();
			// try{Thread.sleep(3000);}catch(Exception e){e.printStackTrace();}
			System.out.println("ex----");
			return "-100";
		}

	}

	// modify password。
	private String modifypwd(String Regcode, String Pwd, String newpwd) {
		String result = "";

		try {

			Service services = new Service();
			Call call1 = null;
			call1 = (Call) services.createCall();

			call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));

			call1.setOperationName(new QName(nameSpace, "modifypwd"));
			call1.addParameter(new QName(nameSpace, "in0"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in1"), XMLType.XSD_STRING,
					ParameterMode.IN);
			call1.addParameter(new QName(nameSpace, "in2"), XMLType.XSD_STRING,
					ParameterMode.IN);

			call1.setReturnType(XMLType.XSD_STRING);

			call1.setUseSOAPAction(true);
			call1.setSOAPActionURI("http://zzwx/example");

			result = (String) call1
					.invoke(new Object[] { Regcode, Pwd, newpwd });
			return result;

		}

		catch (Exception ex) {
			ex.printStackTrace();

			System.out.println("ex----");
			return "-100";
		}

	}

	// send sms
	public String sendEx(String Regcode, String Pwd, String content,
			String mobile, String exnum) {

		String result = "";

		byte[] b;

		boolean isSentOk = false;
		int sendtimes = 0;
		while (!isSentOk) {
			try {

				// --------------------https url --------------------------

				// example：（keytool -import -file X:/zrgj.cer -storepass
				// changeit -keystore client.truststore -alias serverkey
				// -noprompt）
				// -----------------using after code--------------------------

				// System.setProperty("javax.net.ssl.trustStore",
				// "c:\\client.truststore");
				// System.setProperty("javax.net.ssl.trustStorePassword",
				// "changeit");

				// ------------------end-----------------------------------------

				Service services = new Service();
				Call call1 = null;
				call1 = (Call) services.createCall();

				call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));

				call1.setOperationName(new QName(nameSpace, "sendSMS"));
				call1.addParameter(new QName(nameSpace, "in0"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in1"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in2"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in3"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in4"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in5"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in6"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in7"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in8"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in9"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.setReturnType(XMLType.XSD_STRING);

				call1.setUseSOAPAction(true);
				call1.setSOAPActionURI("http://zzwx/example");

				
				
				result = (String) call1.invoke(new Object[] {Regcode, Pwd,
						mobile, java.net.URLEncoder.encode(content, "gbk"), "",
						"1", "", "1", "", "4" });
				// System.out.println(result);

				b = result.getBytes("8859_1");
				String name = new String(b, "GBK"); // 转换成GBK字符

				if (name.equals("0")) {
					isSentOk = true;

				} else {

					sendtimes++;
					if (sendtimes > 1) {
						isSentOk = true;
					} else {

						System.out.println("发送结果：" + name);

						isSentOk = false;

					}
				}
			}

			catch (Exception ex) {

				System.out.println("ex----");
				isSentOk = true;
			}
		}
		if (isSentOk) {
			if (sendtimes > 1)
				return "-2";
			else
				return "0";
		} else {
			return "-1";
		}
	}
	
	// send mms
	private String sendMMSEx(String Regcode, String Pwd, String content,
			String mobile, String exnum, String mmsid) {
		String result = "";

		byte[] b;
		int sendtimes = 0;
		boolean isSentOk = false;
		while (!isSentOk) {
			try {
				// --------------------https url --------------------------

				// example：（keytool -import -file X:/zrgj.cer -storepass
				// changeit -keystore client.truststore -alias serverkey
				// -noprompt）
				// -----------------using after code--------------------------

				// System.setProperty("javax.net.ssl.trustStore",
				// "c:\\client.truststore");
				// System.setProperty("javax.net.ssl.trustStorePassword",
				// "changeit");

				// ------------------end-----------------------------------------

				Service services = new Service();
				Call call1 = null;
				call1 = (Call) services.createCall();

				call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));

				call1.setOperationName(new QName(nameSpace, "sendMMS"));
				call1.addParameter(new QName(nameSpace, "in0"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in1"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in2"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in3"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in4"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in5"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in6"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in7"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in8"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in9"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in10"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.setReturnType(XMLType.XSD_STRING);

				call1.setUseSOAPAction(true);
				call1.setSOAPActionURI("http://zzwx/example");

				result = (String) call1.invoke(new Object[] { Regcode, Pwd,
						mobile, java.net.URLEncoder.encode(content, "gbk"), "",
						"1", "", "1", "", "4", mmsid });
				// System.out.println(result);

				String name = result;

				// :OK:[201106306545812971002002]
				if (name.equals("0")) {
					isSentOk = true;

					System.out.println((new java.text.SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss")).format(new Date())
							+ "result:" + name);

				} else {

					sendtimes++;
					if (sendtimes > 1) {
						isSentOk = true;
					} else {

						System.out.println("result:" + name);

						isSentOk = false;
						Thread.sleep(2000);

					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				try {
					Thread.sleep(3000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				isSentOk = true;
			}
		}
		if (isSentOk) {
			if (sendtimes > 1)
				return "-2";
			else
				return "0";
		} else {
			return "-1";
		}
	}
	
	
	/**
	 * 北京云测 短信API
	 * @param mobile	手机号
	 * @param checkNum	验证码(密码)
	 * @param type	1  初始密码 ，2 验证码
	 * @return
	 */
	public String ycService(String mobile,String checkNum,int type) {
		byte[] b;
		int returnCode = 2;//success;
		String result = "";
		boolean isSentOk = false;
		int sendtimes = 0;
		while (!isSentOk) {
			try {
				Service services = new Service();
				Call call1 = null;
				call1 = (Call) services.createCall();
				call1.setTargetEndpointAddress(new URL(strURL + SHCNN_URI_SEND));
				call1.setOperationName(new QName(nameSpace, "sendSMS"));
				call1.addParameter(new QName(nameSpace, "in0"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in1"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in2"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in3"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in4"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in5"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in6"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in7"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in8"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.addParameter(new QName(nameSpace, "in9"),
						XMLType.XSD_STRING, ParameterMode.IN);
				call1.setReturnType(XMLType.XSD_STRING);
				call1.setUseSOAPAction(true);
				call1.setSOAPActionURI("http://zzwx/example");
				String ycRegcode = ConfigCenterFactory.getString("sms.bjyc_regcode", "YCWL-CRM-0100-CCVWFA");
				String ycPwd = ConfigCenterFactory.getString("sms.bjyc_pwd", "76429888");
				String content = ConfigCenterFactory.getString("sms.bjyc_content", "您的初始密码是#code#，感谢您的注册。【投机侠】");
				if(SmsService.CHECK_NUM==type || SmsService.FIND_PWD==type){
					content = "您的验证码是#code#，请在五分钟内完成验证。【投机侠】";
				}
				content = content.replace("#code#", checkNum);
				result = (String) call1.invoke(new Object[] {ycRegcode, ycPwd,
						mobile, java.net.URLEncoder.encode(content, "gbk"), "",
						"1", "", "1", "", "4" });
				b = result.getBytes("8859_1");
				String name = new String(b, "GBK"); // 转换成GBK字符
				if (name.equals("0")) {					
					isSentOk = true;					
				}else{//send msg fail
					isSentOk = true;
					sendtimes++;
					if (sendtimes > 1) {//重发第二次跳出
						isSentOk = true;
						returnCode = 1;
						logger.info("bjycService send msg fail" + name);
					} else {//出错了，重发一次
						isSentOk = false;
						Thread.sleep(2000);
					}
				}
			}
			catch (Exception ex) {
				returnCode = 1;
				logger.info("ycService exception" + ex);
			}
		}
		return String.valueOf(returnCode);
	}
	
	//短信发送服务 haoService
	public String haoService(String mobile,String checkNum,int type){
		String code = "2";
		try {
			String tpl_value = ConfigCenterFactory.getString("sms.haos_sms_tpl_value", "#code#="+checkNum);
			String tpl_id = ConfigCenterFactory.getString("sms.haos_sms_tpl_id", "447");
			if(SmsService.CHECK_NUM==type || SmsService.FIND_PWD==type){
				tpl_id = "478";
				tpl_value = "#company#投机侠#code#"+checkNum;
			}
			String apiKey = ConfigCenterFactory.getString("sms.haos_sms_apiKey", "4001eb649541477395c2e21eaf8748f9");
			String url = ConfigCenterFactory.getString("sms.haos_sms_send_url", "http://apis.haoservice.com/sms/send");
			
			HttpClient httpClient = new HttpClient();
			httpClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
			PostMethod getMethod = new PostMethod(url);
			NameValuePair[] data = {
					new NameValuePair("key", apiKey),
					new NameValuePair("mobile", mobile),
					new NameValuePair("tpl_id", tpl_id),
					new NameValuePair("tpl_value", tpl_value)};
			getMethod.setRequestBody(data);
			getMethod.addRequestHeader("Connection", "close");
			try {
				int statusCode = httpClient.executeMethod(getMethod);
				if (statusCode == HttpStatus.SC_OK) {
					String reStr = getMethod.getResponseBodyAsString();	
					if(reStr.contains("成功")){
						code = "2";
					}else{
						logger.error("haoService返回异常："+reStr);
						code = "1";
					}
					
				}else{
					logger.error("haoService服务异常");
					code = "1";
				}
				
				// return getMethod.getResponseBodyAsString();
			} catch(Exception e) {
				logger.error("haoService服务异常",e);
				code = "1";
			}			
		} catch (Exception e) {
			code = "1";//error
			logger.info("send sMessage error reason ",e);
		}
		return code;	
	}
	
	//短信发送服务 麦讯通
	public String mxtService(String mobile,String checkNum,int type){
		int status = 2;//success
		try {
			String url = ConfigCenterFactory.getString("sms.mxt_sms_send_url", "http://www.mxtong.net.cn/GateWay/Services.asmx/DirectSend");
			String smsContent = ConfigCenterFactory.getString("sms.mxt_sms_content", "您的初始密码是#code#,感谢您的注册。【投机侠】");
			String userID = ConfigCenterFactory.getString("sms.mxt_sms_uid", "968355");
			String account = ConfigCenterFactory.getString("sms.mxt_sms_account", "admin");
			String password = ConfigCenterFactory.getString("sms.mxt_sms_password", "CY6PSB");
			String sendType = ConfigCenterFactory.getString("sms.mxt_sms_sendType", "1");
			String postFixNumber = ConfigCenterFactory.getString("sms.mxt_sms_postFixNumber", "");
			HttpClient httpClient = new HttpClient();
			httpClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
			PostMethod getMethod = new PostMethod(url);
			if(SmsService.CHECK_NUM==type || SmsService.FIND_PWD==type){
				smsContent = "您的验证码是#code#，请在五分钟内完成验证。【投机侠】";
			}
			smsContent = smsContent.replace("#code#", checkNum);
			String Content= java.net.URLEncoder.encode(smsContent, "UTF-8");
			NameValuePair[] data = {
					new NameValuePair("UserID", userID),
					new NameValuePair("Account", account),
					new NameValuePair("Password", password),
					new NameValuePair("Phones", mobile),
					new NameValuePair("SendType",sendType),
					new NameValuePair("SendTime", null),
					new NameValuePair("PostFixNumber", postFixNumber),
					new NameValuePair("Content", Content)};
			getMethod.setRequestBody(data);
			getMethod.addRequestHeader("Connection", "close");
			try {
				int statusCode = httpClient.executeMethod(getMethod);
				if (statusCode != HttpStatus.SC_OK) {
					byte[] responseBody = getMethod.getResponseBody();
					String str = new String(responseBody, "GBK");
					if (str.contains("GBK")) {
						str = str.replaceAll("GBK", "utf-8");
					}
					int beginPoint = str.indexOf("<RetCode>");
					int endPoint = str.indexOf("</RetCode>");
					String result = "mxtService error,RetCode=";
					logger.info(result + str.substring(beginPoint + 9, endPoint));
					logger.info(str);
					logger.info("Method failed: "+ getMethod.getStatusLine());
					status = 1;
				}
				
				// return getMethod.getResponseBodyAsString();
			} catch (HttpException e) {
				status = 1;
				logger.info("mxtService exception " + e);
			} catch (IOException e) {
				status = 1;
				logger.info("mxtService exception " + e);
			} finally {
				getMethod.releaseConnection();
			}
		} catch (UnsupportedEncodingException e) {
			status = 1;
			logger.info("mxtService exception  "+ e);
		}
		return String.valueOf(status);	
	}

	public static void main(String[] args) {
		SmsService sm = new SmsService();
		sm.ycService("13316853840", "486532", 2);
	/*	String money = sm.getbalance("YCWL-CRM-0100-CCVWFA", "76429888");
		System.out.println(money);*/
	}
}