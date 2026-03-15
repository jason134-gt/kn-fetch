<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@page import= "java.util.*"   %> 
<%@page import= "com.yz.mycore.lcs.enter.LCEnter"   %> 
<%@page import="com.stock.common.model.*"   %> 
<%@page import= "com.stock.common.constants.*"   %> 
<%@page import= "com.stock.common.util.*"   %> 
<%@page import= "com.yz.stock.portal.service.*"   %> 
<%@page import= "com.yz.stock.portal.model.*"   %> 
<%@page import= "com.yfzx.service.db.*"   %> 

<html>
<head>
 <title>价值投资分析系统</title>
<link rel="stylesheet" type="text/css" href="css/InvestAnalysis2.css">
<script src="js/jquery/jquery-1.6.2.min.js" type="text/javascript"></script>
<script src="js/highcharts/js/highcharts.js" type="text/javascript"></script>
<script type='text/javascript' src='js/jquery-autocomplete/jquery.autocomplete.js'></script>
<script type='text/javascript' src='js/util/Calendar.js'></script>
<script type='text/javascript' src='js/mycharts.js'></script>
<script type='text/javascript' src='js/indexAnalysis.js'></script>
<script type='text/javascript' src='js/common.js'></script>
<script type="text/javascript" src="js/base.js"></script>
<script type="text/javascript" src="js/jquery.ichoose/plug.choose.js"></script>
	<script type="text/javascript" src="js/jquery.ichoose/mod.udatas.js"></script>
   <link rel="stylesheet" href="css/style.css" type="text/css" media="screen, projection"/>
   <script type="text/javascript" language="javascript" src="js/jquery.dropdownPlain.js"></script>
<link rel="stylesheet" type="text/css" href="js/jquery-autocomplete/jquery.autocomplete.css" />
<script type="text/javascript">
$(document).ready(function() {

	initYear("myperiod",15);
					//导入文件
					$("#importButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/file/importData",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					//运行任务(全量)
					$("#taskAllButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeIndexAll",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					//运行任务(增量_指标更新)
					$("#taskAddButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeIndexNew",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					//缓存刷新
					$("#refreshCacheButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/cache/refresh",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					
					//只计算数据发生更新的公司的规则
					$("#taskAddNewIndexCompanyButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeIndexNewByCompany",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					//只计算数据发生更新的公司的规则
					$("#taskAddAllIndexCompanyButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeAllIndexByCompany",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					
					//只计算数据发生更新的公司的规则
					$("#taskAddCompanyButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeDataUpdateCompany",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					
					$("#importAcessButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/file/importAcessExportData",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					
					$("#importAddButton").click(function(){
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/file/importAddZipExportData",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					
					$("#exportButton").click(function(){
						var exportCompanyCode = $("#exportCompanyCode").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/file/exportData2File?exportCompanyCode="+exportCompanyCode,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#computeRank").click(function(){
			
						var rankPeriod = $("#myperiod").val()+"-"+$("#myperiod_jidu").val();
						var rankCode =  $("#rankCode").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/ranking/computeRankresult?rankPeriod="+rankPeriod+"&rankCode="+rankCode,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click

});//end ready

</script>
</head>
<body>
 <div class="content">
<div class="col-center-top-right">	
	
	==========================================榜单运算===============================================<br/>
	<form id="rankForm" action="">
	  时期:<select id="myperiod" >	
		</select><select name="myperiod_jidu" id="myperiod_jidu" >	
		<option value='3-30' selected>一季度</option>
		<option value='6-30' selected>二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select>
	<select id="rankCode" name="rankCode">
	<option value="" selected>所有榜单</option>
	<%
	List<Ranking> rl = RankingService.getInstance().queryAllRanking();
	if(rl!=null&&rl.size()>0)
	{
		for(Ranking r : rl)
		{
			%>
			<option value="<%=r.getRankingCode()%>"><%=r.getRankingName()%></option>
			<%
		}
	}
	%>
	</select>
	
		<input type="button" id="computeRank" value="计算榜单"/><br/>
	</form>

</div>
 
 </div>
</body>
</html>

