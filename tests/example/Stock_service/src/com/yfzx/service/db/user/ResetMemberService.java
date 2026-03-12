package com.yfzx.service.db.user;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stock.common.constants.StockConstants;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

public class ResetMemberService {
	private final static String BASE_NS = "com.yz.stock.portal.dao.members.ResetMembersDao";//ResetMemberDao.xml的namespace
	private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger(ResetMemberService.class);	
	static ResetMemberService instance = new ResetMemberService();
	static DBAgent dbAgent = DBAgent.getInstance();

	private ResetMemberService(){}
	
	public static ResetMemberService getInstance(){
		return instance;
	}
	
	public boolean insert(ResetMemberModel rmd){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"insert", rmd, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
//			//写入Nosql
//			String key = String.valueOf(rmd.getUid());
//			logger.error(key);
//			try {
//				Map map = BeanUtils.describe(rmd);
//				CassandraHectorGateWay.getInstance().insert(SAVE_TABLE.USER_EXT.toString(), key ,map);
//			} catch (IllegalAccessException e) {				
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			} catch (NoSuchMethodException e) {
//				e.printStackTrace();
//			}
			Log.info("resetMemberDao insert success");
			return true;
		}else{
			Log.error("resetMemberDao insert false");
			return false;
		}
	}
	public ResetMemberModel getByToken(String token){
		Map m=new HashMap();
		m.put("token", token);
		RequestMessage req=DAFFactory.buildRequest(BASE_NS+"."+"selectByToken",m,StockConstants.common);
		ResponseMessage rm=dbAgent.queryForObject(req);
		Object obj=rm.getResult();
		if(obj==null){
			return null;
		}
		return (ResetMemberModel)obj;
	}
	public ResetMemberModel getByUid(long uid){
		Map m = new HashMap();
		m.put("uid", uid);
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"selectByUid", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		Object obj = rm.getResult();
		if(obj == null)return null;
		return (ResetMemberModel)obj;
	}
	/*
	public static void main(String[] args) {
		ResetMemberService rms = ResetMemberService.getInstance();
		ResetMemberModel rmm = new ResetMemberModel();
//		rmm.setEmail("happyfromtbq@163.com");
		rmm.setEmail("403751913@qq.com");
		rmm.setToken("ssssssssssss");
		rms.sendEmail("某个用户", rmm);
	}*/
	
	public boolean sendEmail(String nickNmae,ResetMemberModel rmm){
		String host = ConfigCenterFactory.getString("stock_zjs.host", "www.toujixia.com");
		String emailCount=ConfigCenterFactory.getString("stock_zjs.email","admin@toujixia.com");
		String emailPassWord=ConfigCenterFactory.getString("stock_zjs.emailPassWord","toujixia360");
		String emailSmtp=ConfigCenterFactory.getString("stock_zjs.emailSmtp", "smtp.exmail.qq.com");
		
		
		Properties props = new Properties(); // 获取系统环境
//		Authenticator auth=new Email_Auth(emailCount,emailPassWord);
		props.put("mail.smtp.host", emailSmtp);//使用QQ邮箱来发送邮件
		props.put("mail.smtp.auth", "true");
		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.smtp.socketFactory.fallback", "false");
		props.setProperty("mail.smtp.port", "465");
		props.setProperty("mail.smtp.socketFactory.port", "465");
//		Session session=Session.getDefaultInstance(props,auth);
		Session session=Session.getInstance(props, new Email_Auth(emailCount,emailPassWord) );
		MimeMessage message=new MimeMessage(session);
		try{
			String emailText="<html><head></head><body>" +
			 		"<div style='background: none repeat scroll 0 0 #FFFFFF; border: 1px solid #E9E9E9; margin: 10px 0 0; padding: 30px;' class='content'>"+
					 "<p> "+nickNmae+"，你好</p>"+
					 "<p style='border-top: 1px solid #DDDDDD;margin: 15px 0 25px;padding: 15px;' class='answer'>"+
					 "投机侠已经收到了你的密码重置请求，请您点击此链接重置密码（链接将在 24 小时后失效）:"+
					 "<a style='word-break: break-all;' href='http://"+host+"/setnewpassword.html?token="+rmm.getToken()+"' target='_blank'>" +
					 "点击重置</a><br />"+
					 "如果无法点击上述连接，请在浏览器中输入:"+host+"/setnewpassword.html?token="+rmm.getToken()+
					 "</p>"+ 
					 "<p style='border-top: 1px solid #DDDDDD; padding-top:6px; margin-top:25px; color:#838383;' class='footer'>&copy; 2014 投机侠&nbsp;·&nbsp;这是一封系统邮件，请不要直接回复。</p>"+
					 "</div></body></html>";

			 //设置邮件主题
			 message.setSubject("投机侠");
			 Multipart mp = new MimeMultipart("related");// related意味着可以发送html格式的邮件
			 BodyPart bodyPart = new MimeBodyPart();// 正文  
		     bodyPart.setDataHandler(new DataHandler(emailText,  
		                "text/html;charset=GBK"));// 网页格式  
		     mp.addBodyPart(bodyPart);
		     message.setContent(mp);
			 message.setSentDate(new Date());//设置邮件发送日期
			 Address address=new InternetAddress(emailCount,"投机侠");
			 message.setFrom(address);
			 Address toAddress =new InternetAddress(rmm.getEmail());
			 message.setRecipient(Message.RecipientType.TO, toAddress);
			 
			 
//			 Transport.send(message);
			 Transport transport=session.getTransport("smtp");
			 transport.connect(emailSmtp,emailCount,emailPassWord);
			 transport.sendMessage(message,message.getAllRecipients());
			 transport.close();
			 return true;
		}catch(Exception e){
			Log.error("sent Email error:"+e);
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean commonSendEmail(ResetMemberModel rmm){
		String emailCount=ConfigCenterFactory.getString("stock_zjs.email","admin@toujixia.com");
		String emailPassWord=ConfigCenterFactory.getString("stock_zjs.emailPassWord","toujixia360");
		String emailSmtp=ConfigCenterFactory.getString("stock_zjs.emailSmtp", "smtp.exmail.qq.com");
		
		Properties props = new Properties(); // 获取系统环境
//		Authenticator auth=new Email_Auth(emailCount,emailPassWord);
		props.put("mail.smtp.host", emailSmtp);//使用QQ邮箱来发送邮件
		props.put("mail.smtp.auth", "true");
		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.smtp.socketFactory.fallback", "false");
		props.setProperty("mail.smtp.port", "465");
		props.setProperty("mail.smtp.socketFactory.port", "465");
//		Session session=Session.getDefaultInstance(props,auth);
		Session session=Session.getInstance(props, new Email_Auth(emailCount,emailPassWord) );
		MimeMessage message=new MimeMessage(session);
		try{
			 //设置邮件主题
			 message.setSubject("投机侠");
			 Multipart mp = new MimeMultipart("related");// related意味着可以发送html格式的邮件
			 BodyPart bodyPart = new MimeBodyPart();// 正文  
		     bodyPart.setDataHandler(new DataHandler(rmm.getEmailText(),  
		                "text/html;charset=GBK"));// 网页格式  
		     mp.addBodyPart(bodyPart);
		     message.setContent(mp);
			 message.setSentDate(new Date());//设置邮件发送日期
			 Address address=new InternetAddress(emailCount,"投机侠");
			 message.setFrom(address);
			 Address toAddress =new InternetAddress(rmm.getEmail());
			 message.setRecipient(Message.RecipientType.TO, toAddress);
			 
			 
//			 Transport.send(message);
			 Transport transport=session.getTransport("smtp");
			 transport.connect(emailSmtp,emailCount,emailPassWord);
			 transport.sendMessage(message,message.getAllRecipients());
			 transport.close();
			 return true;
		}catch(Exception e){
			Log.error("sent Email error:"+e);
			e.printStackTrace();
			return false;
		}
	}
	/*	与当前时间进行比较
	 * 	true:输入的时间在当前时间之后.
	 * 	false:输入的时间在当前时间之前.
	 * */
	 
	public boolean compareToNow(Date overTime){
		Date now=new Date();
		boolean result=false;
		if(overTime.after(now)||overTime.equals(now)){
			result= true;
		}
		now=null;
		return result;
	}
	/*检查用户是否重复提交邮箱重置申请
	 * 0.表中未有该邮箱的重置申请记录，可以添加记录。
	 * 1.表中有该邮箱的重置申请记录，但该记录的已经过期。
	 * 2。表中有该邮箱的重置申请记录，且该记录未过期。
	 * */
	public List<ResetMemberModel> getByEmail(String email){
		int result=0;
		Map m=new HashMap();
		m.put("email", email);
		RequestMessage req=DAFFactory.buildRequest(BASE_NS+"."+"selectByEmail", m,StockConstants.common);
		ResponseMessage rm=dbAgent.queryForList(req);
		List<ResetMemberModel> list=(List<ResetMemberModel>)rm.getResult();
		//清除多余对象
		rm=null;
		req=null;
		m.clear();
		m=null;
		//设定返回值
		if(list==null||list.size()==0){
			return null;
		}
		return list;
	}
	public boolean delectByUid(long uid){
		RequestMessage req = DAFFactory.buildRequest(BASE_NS+"."+"deleteByPrimaryKey", uid, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String retrunCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(retrunCode)){
			return true;
		}else{
			return false;
		}
	}
	                                     
		
}
