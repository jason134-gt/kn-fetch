package com.yz.stock.user.action;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.dispatcher.ng.filter.StrutsPrepareAndExecuteFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.user.Members;
import com.stock.common.util.StringUtil;
import com.yz.configcenter.ConfigCenterFactory;




public class MyDispatcher implements Filter{
	private static Logger log = LoggerFactory.getLogger(MyDispatcher.class);
	private static StrutsPrepareAndExecuteFilter filter = new StrutsPrepareAndExecuteFilter();
	
	@Override
	public void destroy() {
		filter.destroy();		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain arg2) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		HttpSession session = request.getSession();
		String uri = request.getServletPath();
		boolean isNocheck = isNocheck(uri);
		if(isNocheck == false){
		Members manager = (Members) session.getAttribute(MembersAction.SESSION_MEMBERS);
			if (manager == null) {
				//用户不存在 使用Cookie中保存的用户名和密码登陆
				String loginCookie =  new MembersAction().loginByCookie(request, response);
				if(com.opensymphony.xwork2.Action.SUCCESS.equalsIgnoreCase(loginCookie) == false){
					if(uri.contains(".jsp")){
						response.setContentType("application/x-javascript; charset=UTF-8");
						String noLoginStr = ConfigCenterFactory.getString("stock_zjs.nologin", "goLogin();");
						response.getWriter().write(noLoginStr);
					}else{
						response.getWriter().write("please login!");
					}
				}
			}		
		}
		filter.doFilter(req, res, arg2);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		filter.init(arg0);
	}
	
	public static boolean isNocheck(String uri){
		//只检查jsp后缀和没有后缀的
		if(uri.contains(".")==false||uri.contains(".jsp")){
			
		}else{
			return true;
		}
		if(uri.contains("config_callback.jsp"))return true;
		if(uri.contains("cache_mng.jsp"))return true;//检查数据连接时,不检查swfupload组件的请求
		if(uri.contains("index.jsp"))return true;		
		if(uri.contains("/realtime/realcompute"))return true;
		String noCheckStr = ConfigCenterFactory.getString("datacenter.noCheckStr","");
		if(StringUtil.isEmpty(noCheckStr))return true;		
		String[] noCheckArr = noCheckStr.split(",");
		for(String noCheck : noCheckArr){
			if(StringUtils.isEmpty(noCheck)){
				continue;
			}
			if(uri.contains(noCheck)){
				return true;
			}
		}
		return false;
	}

}
