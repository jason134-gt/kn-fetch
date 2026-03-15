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
						var type = $("#exportType").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/file/exportData2File?exportCompanyCode="+exportCompanyCode+"&type="+type,function(result){
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
	==========================================证券所数据导入导出===============================================<br/>
	<input type="button" id="importButton" name="importButton" value="导入文件" /><br/>
	<input type="button" id="importAcessButton" name="importAcessButton" value="导入Acess全量数据" /><br/>
	<input type="button" id="importAddButton" name="importAddButton" value="导入增量excel数据" /><br/>
	<input type="button" id="exportButton" name="exportButton" value="导出公司数据" />
	<input type="text" id="exportCompanyCode" name="exportCompanyCode"  />
	类型：<select id="exportType">
		<option value="0">通用-asset</option>
		<option value="1">通用-profile</option>
		<option value="2">通用-cash</option>
		<option value="3">银行-asset</option>
		<option value="4">银行-profile</option>
		<option value="5">银行-cash</option>
		<option value="6">保险-asset</option>
		<option value="7">保险-profile</option>
		<option value="8">保险-cash</option>
		<option value="9">证券-asset</option>
		<option value="10">证券-profile</option>
		<option value="11">证券-cash</option>
	</select>

</div>
 
 </div>
</body>
</html>

