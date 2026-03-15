package com.yz.stock.user.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.user.Members;
import com.stock.common.util.CipherUtil;
import com.stock.common.util.CookieUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.RSAUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.user.MembersService;
import com.yfzx.service.db.user.ResetMemberService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.stock.common.BaseAction;

/**
 * @author tangbinqi
 * 注 之前的UC代码都去除 Discuz! Ucenter API for JAVA 去除
 */
public class MembersAction extends BaseAction{

	private  Logger log = LoggerFactory.getLogger(this.getClass());
	private Members members =new Members();
	private InputStream inputStream;
	//@Resource(name = "membersManagerImpl")
	private MembersService membersService = MembersService.getInstance();
	private ResetMemberService rmService=ResetMemberService.getInstance();
	private String refererUrl;
	public final static String SESSION_URL_LOGIN_BEFORE = "URL_LOGIN_BEFORE";
	public final static String SESSION_PRIVILEGES = "PRIVILEGES";
	public final static String SESSION_KEYPAIR = "KEYPAIR";
	public final static String SESSION_MOBILETOKEN="MobileToken";

	@Action(value = "/user/members/cipherStr",
			results = { @Result(name = "success",type="stream",params={"contentCharSet","utf-8"})})
	public String cipherStr(){
		//ActionContext context = ServletActionContext.getContext();
        //取得Session[]数组
        //context.getSession();
		HttpSession session = getHttpSession();//一次Session中使用一个RSA
		KeyPair keyPair = (KeyPair)session.getAttribute(SESSION_KEYPAIR);
		if(keyPair == null){
			try {
				keyPair = RSAUtil.generateKeyPair();
			} catch (Exception e) {
				log.error("生成RSA keyPair失败，请联系管理员!");
			}
			session.setAttribute(SESSION_KEYPAIR, keyPair);
		}

		RSAPublicKey rsaPub = (RSAPublicKey) keyPair.getPublic();
		String text  = rsaPub.getModulus().toString(16)+","+ rsaPub.getPublicExponent().toString(16);
		try {
			inputStream = new ByteArrayInputStream(text.getBytes("utf-8"));
		} catch (IOException e) {
			e.printStackTrace();
			return ERROR;
		}
		return SUCCESS;
	}



	/**
	 * 登录成功后，做一些登录相关的事情，如加载权限
	 * @param request
	 * @param membersDB
	 * @return
	 */
	private String doAfterLogin(HttpServletRequest request,Members membersDB, boolean isLogin){
		HttpSession session = request.getSession();//不能使用this.getHttpSession		
		session.setAttribute(SESSION_MEMBERS, membersDB);

		//设置权限
		List<String> pList = new ArrayList<String>();
		pList.add("myInfo.addStock");
		session.setAttribute(("PRIVILEGES"), pList);

		//记录登录信息
		String ipAddr = StockUtil.getIpAddr(request);
		log.info("用户"+membersDB.getUsername()+"登录，IP="+ipAddr);
		return SUCCESS;
	}



	// 新接口 ajax登录方式
	@Action(value = "/user/members/ajaxLogin",
			results = { @Result(name = "success",type="stream",params={"contentCharSet","utf-8"})})
	public String ajaxLogin(){
		String text =  ""; //"var errorMsg = '用户不存在';";
		long startTime = System.currentTimeMillis();
		try{
			String username = NetUtil.getParameterString(getHttpServletRequest(), "username");
			String password = NetUtil.getParameterString(getHttpServletRequest(), "password");
			String autoLogin = NetUtil.getParameterString(getHttpServletRequest(), "autoLogin");
			Integer redirect = NetUtil.getParameterInt(getHttpServletRequest(), "redirect", 1);
			String key = NetUtil.getParameterString(getHttpServletRequest(), "key");
			HttpSession session = getHttpSession();//一次Session中使用一个RSA
			KeyPair keyPair = (KeyPair)session.getAttribute(SESSION_KEYPAIR);
			if(keyPair == null){
				log.error("登录超时，请重刷新页面!");
				throw new Exception("0001");
			}else{
				//log.info("密文："+ password);
				RSAPrivateKey rsaPri = (RSAPrivateKey) keyPair.getPrivate();
				RSAPublicKey rsaPub = (RSAPublicKey) keyPair.getPublic();
				String pktext  = rsaPub.getModulus().toString(16);
				if(!pktext.equals(key)){//页面的公钥跟本地Session不一致
					log.error("页面公钥跟会话公钥不一致!\r\n页面pk="+key+"\r\n会话pk="+pktext);
					throw new Exception("0001");
				}
				//String pritext  = rsaPri.getPrivateExponent().toString(16);
				//log.info(pktext+"\r\n"+pritext);
				byte[] en_result = new BigInteger(password, 16).toByteArray();
		        byte[] de_result = RSAUtil.decrypt(rsaPri,en_result);
		        StringBuffer sb = new StringBuffer();
		        password = new String(de_result);//jsbn不用倒过来 //sb.append(new String(de_result)).reverse().toString();
		        //log.info("还原密文："+ password);
			}
			log.error("到解密耗时"+(System.currentTimeMillis()-startTime)+"毫秒");
			String doAfterPassword = CipherUtil.generatePassword(password);//处理后的密码

			HttpServletRequest request = ServletActionContext.getRequest();

			//Members members = membersService.getMembersByUserName(username, doAfterPassword);//(queryMembers);
			Members membersDB = membersService.getAdminMembersByAccount(username);
			log.error("到查询数据耗时"+(System.currentTimeMillis()-startTime)+"毫秒");
			if (membersDB == null||membersDB.getUid()==null) {
				throw new Exception("账号不存在。");
			}else if(!doAfterPassword.equals(membersDB.getPassword())){
				throw new Exception("账号或密码错误，请重新输入。");
			}else if(!"admin".equals(membersDB.getRoleType())) {
				throw new Exception("非管理员账号。");
			}else{
				String paramRefererUrl = this.getRefererUrl();
				if(paramRefererUrl == null || paramRefererUrl.contains("login") ){//外部参数没有设置,才从Session中获取重定向Url
					//会话中的SESSION_URL_LOGIN_BEFORE 可能有影响
					paramRefererUrl = String.valueOf(session.getAttribute(SESSION_URL_LOGIN_BEFORE));
//					String lurl = ConfigCenterFactory.getString("stock_zjs.user_index_url", "index.html");
					if(StringUtil.isEmpty(paramRefererUrl) || paramRefererUrl.contains("login") ){
						paramRefererUrl = "/";//会自动跳到默认的首页 //lurl+"?uid="+members.getUid()+"&end";//"/index.html";
					}else{
						session.setAttribute(SESSION_URL_LOGIN_BEFORE, "");//登录成功后，清理登录前设置的URL
					}
				}
				if("true".equalsIgnoreCase(autoLogin)){//设定自动登录
					CookieUtil.saveAdminLoginCookie(membersDB, getHttpServletResponse(),true);
				}else{
					CookieUtil.saveAdminLoginCookie(membersDB, getHttpServletResponse(),false);
				}
				//String doAfter = doAfterLogin(request,membersDB, isLogin == 1);
				HttpSession session2 = request.getSession();//不能使用this.getHttpSession		
				session2.setAttribute(SESSION_MEMBERS, membersDB);

				//设置权限
				/*List<String> pList = new ArrayList<String>();
				pList.add("myInfo.addStock");
				session2.setAttribute(("PRIVILEGES"), pList);*/

				//记录登录信息
				String ipAddr = StockUtil.getIpAddr(request);
				log.info("用户"+membersDB.getUsername()+"登录，IP="+ipAddr);
				log.error("到登录写Cookie等耗时"+(System.currentTimeMillis()-startTime)+"毫秒");
				/*if(!SUCCESS.equals(doAfter)){
					throw new Exception("登录后权限异常");
				}*/

				if( new Integer(3).equals(membersDB.getState()) ) {
					text = "location.replace('recommend_stocks.html');";
				} else {
					if(redirect == 0 ) {//弹出框异步登陆，不跳转
						text = "var loginResult=true;";
//						boolean hasVp = false;
//						StringBuilder sbr = new StringBuilder();
//						if(! MicorBlogService.getInstance().excludeEssenceVp(members.getUid())) {
//							hasVp = MicorBlogService.getInstance().hasVp(members.getUid());
//						}
//						int essenceVp = MicorBlogService.getInstance().hasEssenceVp(members.getUid()) ? 1 : 0;
//						sbr.append("var chanceVp = '").append(hasVp ? 1 : 0).append("|").append(essenceVp).append("';");
//						text = sbr.append(text).toString();
					} else {
						text = "location.replace('"+paramRefererUrl+"');";//不能返回的重定向
					}
				}
			}
		}catch (Exception e) {
			text ="var errorMsg = '" + e.getMessage() + "';";
		}
		try {
			inputStream = new ByteArrayInputStream(text.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return SUCCESS;
	}


	/**
	 * 退出登录
	 * @return
	 */
	@Action(value = "/user/members/loginOut")
	public String loginOut(){
		HttpSession session = ServletActionContext.getRequest().getSession();
		/*Object obj = session.getAttribute(SESSION_MEMBERS);
		if(obj == null){
			return SUCCESS;
		}*/
		session.removeAttribute(SESSION_MEMBERS);
		session.setAttribute(SESSION_MEMBERS,null);
		CookieUtil.clearAdminCookie(getHttpServletResponse());//手工退出，清理Cookie
		//CookieUtil.clearCookieJsessionId(getHttpServletResponse());
		return SUCCESS;
	}


	/**
	 * 后台Action没有登录 统一处理
	 */
	@Action(value="/NoLogin.do")
	public void doNoLogin(){
		try {
			HttpServletResponse response = this.getHttpServletResponse();
			response.setContentType("application/x-javascript; charset=UTF-8");
			String noLoginStr = ConfigCenterFactory.getString("stock_zjs.nologin", "goLogin();");
			response.getWriter().write(noLoginStr);
		} catch (IOException e) {
			log.error("处理没有登录失败");
		}
	}

	/**
	 * 后台Action没有权限 统一处理
	 */
	@Action(value="/NoPrivilege.do")
	public void doNoPrivilege(){
		try {
			HttpServletResponse response = this.getHttpServletResponse();
			response.setContentType("application/x-javascript; charset=UTF-8");
			String content = ConfigCenterFactory.getString("stock_zjs.noPrivilege", "authDialog('此项功能需要会员才可以使用！', '立即开通');");
			response.getWriter().write(content);
		} catch (IOException e) {
			log.error("处理无权限提醒失败");
		}
	}

	@Action(value="/NoEnoughPrivilege.do")
	public void noEnoughPrivilege() {
		try {
			HttpServletResponse response = this.getHttpServletResponse();
			response.setContentType("application/x-javascript; charset=UTF-8");
			String content = ConfigCenterFactory.getString("stock_zjs.noEnoughPrivilege", "authDialog('您购买的会员权限不足，请升级！', '立即升级');");
			response.getWriter().write(content);
		} catch (IOException e) {
			log.error("处理权限不足提醒失败");
		}
	}

	@Action(value="/OrderExpired.do")
	public void orderExpired() {
		try {
			HttpServletResponse response = this.getHttpServletResponse();
			response.setContentType("application/x-javascript; charset=UTF-8");
			String content = ConfigCenterFactory.getString("stock_zjs.orderExpired", "authDialog('您购买的会员已过期，请续费使用！', '立即续费');");
			response.getWriter().write(content);
		} catch (IOException e) {
			log.error("处理订单过期提醒失败");
		}
	}

	public Members getMembers() {
		return members;
	}


	public void setMembers(Members members) {
		this.members = members;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public String getRefererUrl() {
		return refererUrl;
	}

	public void setRefererUrl(String refererUrl) {
		this.refererUrl = refererUrl;
	}
	
	public String loginByCookie(HttpServletRequest request,HttpServletResponse response){
		MembersService membersService = MembersService.getInstance();
		try {
			String loginCookie = CookieUtil.getloginCookie(request, response);//"username:password"
			if(StringUtils.isEmpty(loginCookie))return ERROR;

			Members membersCookie = CookieUtil.getMembersByLoginCookie(loginCookie);
			if(membersCookie == null)return ERROR;
			String username = membersCookie.getUsername();

			Members members = membersService.getMembersByAccount(username);
			if(members == null) return ERROR;

			Boolean checkPassword = CookieUtil.checkPasswordAfterReadCookie(loginCookie, members);
			if(checkPassword){
				return doAfterLogin(request, members, true);
			}else{
				//清理cookie
				CookieUtil.clearCookie(response);
				return ERROR;
			}
		}catch (Exception e) {
			e.printStackTrace();
			return ERROR;
		}
	}	
}
