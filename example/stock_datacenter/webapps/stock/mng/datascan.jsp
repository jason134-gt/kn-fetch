<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@page import="com.stock.common.model.*"   %> 
<%@page import= "com.stock.common.constants.*"   %> 
<%@page import= "com.stock.common.util.*"   %> 
<%@page import= "java.util.*"   %> 
<%@page import= "com.yz.mycore.lcs.enter.LCEnter"   %> 
<%@page import= "com.yfzx.service.db.*"   %> 
<%@page import= "com.yz.stock.portal.service.*"   %> 

<html>
<head>
 <title>价值投资分析系统</title>
<link rel="stylesheet" type="text/css" href="js/jquery-autocomplete/jquery.autocomplete.css" />
 <link rel="stylesheet" type="text/css" href="css/InvestAnalysis1.css">
 <link href = "css/portal1.css"  type = "text/css" rel="stylesheet"></link>
<script src="js/jquery/jquery-1.6.2.js" type="text/javascript"></script>
<script src="js/highcharts/js/highcharts.src.js" type="text/javascript"></script>
<script type='text/javascript' src='js/jquery-autocomplete/jquery.autocomplete.js'></script>
<script type="text/javascript" src="js/base.js"></script>
 <link rel="stylesheet" href="css/style.css" type="text/css" media="screen, projection"/>
   <script type="text/javascript" language="javascript" src="js/jquery.dropdownPlain.js"></script>
   <script type="text/javascript" src="js/jquery.ichoose/plug.choose.js"></script>
	<script type="text/javascript" src="js/jquery.ichoose/mod.udatas.js"></script>
<script type="text/javascript">
$(document).ready(function() {

					//导入文件
					$("#scan").click(function(){
						
						 $.ajax({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getScript("<%=request.getContextPath()%>/dcheck/scanAllCompanyDcheck",function(result){
							   var ret = sdret.split("~");
							   for(var i=0;i<ret.length;i++)
							   {
									$("#result").append(ret[i]);
							   }
						   });//end getJSON					
					});//end click
					

});//end ready

</script>
</head>
<body>
 <div class="content">
<div class="col-center-top-right">	
	<input type="button" id="scan" name="scan" value="扫雷" /><br/>
	
</div>
 <div id="result"></div>
 </div>
</body>
</html>

