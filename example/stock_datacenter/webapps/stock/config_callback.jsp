<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@page import= "com.stock.common.util.*"   %> 
<%@page import= "com.yz.configcenter.manager.*"   %> 
<%@page import= "com.yfzx.yas.router.*"   %> 

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
	String type = NetUtil.getParameterString(request, "type","0");
	//配置下发
	if(type.equals("0"))
	{
		String filename = NetUtil.getParameterString(request, "filename","");
		String contents = NetUtil.getParameterString(request, "contents","");
		if(StringUtil.isEmpty(filename)||StringUtil.isEmpty(contents))
			return ;
		ConfigCenter.getInstance().writeConfigFile2Local(filename,new StringBuffer(contents));
		
	}
	//路由下发
	if(type.equals("1"))
	{
		String serviceName = NetUtil.getParameterString(request, "servicename","");
		String address = NetUtil.getParameterString(request, "serviceAddress","");
		int status = NetUtil.getParameterInt(request, "status",0);
		RouterCenter.getInstance().updateRouter(serviceName, address, status);
	}
	//配置文件下发结束通知
	if(type.equals("2"))
		{
		ConfigCenter.getInstance().notifyUpEnd();
		}
	
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>

</body>
</html>