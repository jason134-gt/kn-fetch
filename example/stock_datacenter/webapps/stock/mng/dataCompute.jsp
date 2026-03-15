<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@page import= "com.yfzx.service.db.*"   %> 
<%@page import= "java.util.*"   %> 
<%@page import= "com.yz.mycore.lcs.enter.LCEnter"   %> 
<%@page import="com.stock.common.model.*"   %> 
<%@page import= "com.stock.common.constants.*"   %> 
<%@page import= "com.stock.common.util.*"   %> 
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

	initAutoIndexMatcher("indexcode");
	initCompanyAutoMatcher("companycode");
	initIndustry();
				
					//运行任务(全量)
					$("#taskAllButton").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeIndexAll?useOldData="+useOldData,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					//运行任务(增量_指标更新)
					$("#taskAddButton").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeIndexNew?useOldData="+useOldData,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click

					
					$("#setTimeCompute1").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/datacomputetimer/datacomputecroncf",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#setTimeCompute3").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/datacomputetimer/datacomputecrontrade",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#setTimeCompute2").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/datacomputetimer/compute1day",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					//只计算数据发生更新的公司的规则
					$("#taskAddNewIndexCompanyButton").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeIndexNewByCompany?useOldData="+useOldData,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					//只计算数据发生更新的公司的规则
					$("#taskAddAllIndexCompanyButton").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeAllIndexByCompany?useOldData="+useOldData,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
				
					$("#computeIndIndexByTag").click(function(){
						var indtag = $("#indtag").val();
						var data = {
								tag:indtag
						};
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/task/computeIndIndexByTag",data,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#computeIndIndexByTag_all").click(function(){
						var indtag = $("#indtag").val();
						var data = {
								tag:indtag
						};
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/task/computeIndIndexByTag_all",data,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#computeAllIndexByCompanyPage").click(function(){
						var useOldData = $("#useOldData").val();
						var notComputeIndustryRelatIndex= $("#notComputeIndustryRelatIndex").val();
						var stime= $("#indloadstime").val();
						var etime= $("#indloadetime").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						var page = $("#page").val();
						var data = {
								useOldData:useOldData,
								page:page,
								stime:stime,
								etime:etime,
								notComputeIndustryRelatIndex:notComputeIndustryRelatIndex
						};
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/task/computeAllIndexByPageCompany",data,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					
					
					
					
					$("#computeHKBaseIndexByPageCompany").click(function(){
						var useOldData = $("#useOldData").val();
						var isadd = $("#isadd").val();
						var notComputeIndustryRelatIndex= $("#notComputeIndustryRelatIndex").val();
						var stime= $("#stime").val();
						var etime= $("#indloadetime").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						var page = $("#page").val();
						var indextype = $("#indextype").val();
						var utype = $("#utype").val();
						var data = {
								useOldData:useOldData,
								page:page,
								stime:stime,
								notComputeIndustryRelatIndex:notComputeIndustryRelatIndex,
								isadd:isadd,
								utype:utype
						};
							 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							   $.post("<%=request.getContextPath()%>/trade/computeHKBaseIndexByPageCompany_all",data,function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							   });//end getJSON		
						
									
					});//end click
					
					
					$("#computeAllIndexByCompanyPage_all").click(function(){
						var useOldData = $("#useOldData").val();
						var isadd = $("#isadd").val();
						var notComputeIndustryRelatIndex= $("#notComputeIndustryRelatIndex").val();
						var stime= $("#stime").val();
						var etime= $("#indloadetime").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						var page = $("#page").val();
						var indextype = $("#indextype").val();
						var utype = $("#utype").val();
						var data = {
								useOldData:useOldData,
								page:page,
								stime:stime,
								notComputeIndustryRelatIndex:notComputeIndustryRelatIndex,
								isadd:isadd,
								utype:utype
						};
						if(indextype==0)
							{
							 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							   $.post("<%=request.getContextPath()%>/task/computeAllIndexByPageCompany_all",data,function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							   });//end getJSON		
							}
						else
							{
							 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							   $.post("<%=request.getContextPath()%>/trade/computeAllIndexByPageCompany_all",data,function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							   });//end getJSON		
							}
									
					});//end click
					
					//只计算数据发生更新的公司的规则
					$("#taskAddCompanyButton").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/task/computeDataUpdateCompany?useOldData="+useOldData,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
			
				
					
				
	
					$("#inittradebitmap").click(function(){
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						  $.post("<%=request.getContextPath()%>/tradebitmap/initTradeBitMap?type=0",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#checktradebitmap").click(function(){
						 var checktime = $("#checktime").val();
						 var checktype = $("#checktype").val();
						 if(checktime==null)
							 return;
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						  $.post("<%=request.getContextPath()%>/tradebitmap/checkTradeBitMap?stime="+checktime+"&type="+checktype,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#loadTradeUextDataFromTime").click(function(){
						 var checktime = $("#checktime").val();
						 var checktype = $("#checktype").val();
						 if(checktime==null)
							 return;
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						  $.post("<%=request.getContextPath()%>/tradebitmap/loadTradeUextDataFromTime?stime="+checktime+"&type="+checktype,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					

					$("#computeIndexDateUpdate").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						var upTime = $("#upTime").val();
						var sendmail = $("#sendmail").val();
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/task/computeIndexOfCompanyDataUpdate?useOldData="+useOldData,{"upTime":upTime,"sendmail":sendmail},function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#getcompanyByUpdateTime").click(function(){
						var getupTime = $("#getupTime").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/company/getDateUpdateCompanyByTime",{"upTime":getupTime},function(result){
							   if(result.success)
								  {
								   $("#showUpdateCompany").empty();
								  	 var data = result.data;
								  	 var da = data.split(";");
								  	 for(var i=0;i<da.length;i++)
								  		 {
								  		 	var cs = da[i];
								  		 	$("#showUpdateCompany").append(cs+"<br/>");
								  		 }
								  }
						   });//end getJSON					
					});//end click
					
					$("#importAllCompanySPrice").click(function(){
						var stime = $("#pstime").val();
						var etime = $("#petime").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/stockprice/initStockPrice",{"stime":stime,"etime":etime},function(result){
							   alert("操作结束");
						   });//end getJSON					
					});//end click
					
					$("#onecompanyoneindex").click(function(){
						var indexcode = $("#indexcode").val();
						var companycode = $("#companycode").val();
						if(companycode==""||indexcode=="") return;
						indexcode = indexcode.split(":")[1];
						companycode = companycode.split(":")[1];
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') 
							useOldData = 'true';
						else
							useOldData = 'false';
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/task/computeOneIndexOfCompany",{"indexcode":indexcode,"companycode":companycode,"useOldData":useOldData},function(result){
							   alert("操作结束");
						   });//end getJSON					
					});//end click
					
					$("#realtimeDataCompute").click(function(){
						$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						$.post("<%=request.getContextPath()%>/realtime/realcompute",function(result){
							alert("操作结束");
						});				
					});//end click
					
					
					
					$("#mockTest").click(function(){
						var companycode = $("#mockTest_companycode").val();
						var date = $("#mockTest_date").val();
						var stime = $("#mockTest_stime").val();
						var etime = $("#mockTest_etime").val();
						var type = $("#mockTest_type").val();
						var data = {
								companycode:companycode,
								date:date,
								stime:stime,
								etime:etime,
								type:type
						};
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							  $.post("<%=request.getContextPath()%>/wstock/mock",data,function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							  });//end getJSON		
			
					});//end click
					
					$("#mockSave").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							  $.post("<%=request.getContextPath()%>/wstock/save",function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							  });//end getJSON		
			
					});//end click
					
					$("#mockStop").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							  $.post("<%=request.getContextPath()%>/wstock/stopMock",function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							  });//end getJSON		
			
					});//end click
					
					$("#mockSaveCopy").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							  $.post("<%=request.getContextPath()%>/wstock/saveCopy",function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							  });//end getJSON		
			
					});//end click
					
					
					$("#mockPrintStatistic").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							  $.post("<%=request.getContextPath()%>/wstock/printStatistic",function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							  });//end getJSON		
			
					});//end click
					$("#mockClearStatistic").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							  $.post("<%=request.getContextPath()%>/wstock/clearStatistic",function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							  });//end getJSON		
			
					});//end click
					$("#clearPreRulecheckFilter").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							  $.post("<%=request.getContextPath()%>/wstock/clearPreRulecheckFilter",function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							  });//end getJSON		
			
					});//end click
					
					$("#mockWstockReload").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
							  $.post("<%=request.getContextPath()%>/wstock/reload",function(result){
								   if(result.success)
									  {
									  	alert("操作结束!"); 
									  }
							  });//end getJSON		
			
					});//end click
					
					
					$("#encrypt").click(function(){

						var encryptstr = $("#encryptstr").val();
						var data = {
								encryptstr:encryptstr
						};
						
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						 $.post("<%=request.getContextPath()%>/wstock/encrypt",data,function(result){
							 $("#encryptret").empty();	  
							 $("#encryptret").append(result.ret); 
							  });//end getJSON		
			
					});//end click
					
					$("#encryptclear").click(function(){

						$("#encryptstr").val("");
						$("#encryptret").empty();
			
					});//end click
					
					
					$("#printRule").click(function(){

						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						 $.post("<%=request.getContextPath()%>/wstock/printRule",function(result){
							 	alert("操作结束！");
							  });//end getJSON		
			
					});//end click
					
					
});//end ready
function initIndustry()
{
	//取数据源编码
		var rd = getIndustryBycode("indtag","s","-1");
}
function getIndustryBycode(id,type,industrycode)
{
	 //先清空
	$("#"+id+"").empty();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("<%=request.getContextPath()%>/industry/getIndustryByTypeAndCode",{"type":type,"industrycode":industrycode}, function(result){
		  var rd = result.data;
		  if(rd==null)
			  return ;
			addIndustry2Select(id,rd);
	  });	
}
function addIndustry2Select(id,sdata)
{ 
	 	if(sdata==null)
	 	{
	 			return ;
	 	}
	    $("#"+id+"").append("<option value='i_-1_所有行业'>所有行业</option>");
		var data = sdata;
		if(data!="")
		{
			var i =0;
		  	for(i in data)
			{
		  		//分隔指标编码与名字
		  		var ind = data[i];
		  		var industryCode = ind.industryCode;
				if(industryCode==null)
					continue;
		  		var industryName = ind.name;
				//if(indexName.length>10) indexName = indexName.substring(0,10);
				var k = "i_"+industryCode+"_"+industryName;
				$("#"+id+"").append("<option value="+industryName+">"+industryName+"</option>");
			}
		}

}
//自动匹配公司名
function initCompanyAutoMatcher(aa)
{
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("/stock/company/getCompanyByTableSystemString?msg.tableSystemCode=ts_00003", function(result){
			if(result.data==null)
			{
				return;
			}
			var data = result.data.split(";");
			if(data!=null)
			{
				var c_data = data;
				var option = {
				max: 12, 
				minChars: 0, 
				width: 150,
				scrollHeight: 300, 
				matchContains: true, 
				autoFill: false 
				};
				var obj = $("#"+aa+"");
				obj.autocomplete(c_data, option);
			}
			
	});
}
function initAutoIndexMatcher(aa)
{

		var tsc = $("#tableSystem").val();
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.getScript("/stock/cfirule/getCfiruleListByTscid_V2?msg.tableSystemCode=ts_00003", function(result){
				var rd = retObj ;
				var data = rd.split(";");
				if(data!=null)
				{
					var c_data = data;
					var option = {
						max: 12, 
						minChars: 0, 
						width: 150,
						scrollHeight: 300, 
						matchContains: true, 
						autoFill: false 
						};
					var obj = $("#"+aa+"");
					obj.autocomplete(c_data, option);
				}
				
		  });
	

}
</script>
</head>
<body>
 <div class="content">
<div class="col-center-top-right">	
	是否使用老数据：<select id="useOldData">
		<option value='0'>否</option>
		<option value='1' selected>是</option>
	</select><br/>
	是否是增量计算：增量只计算近三年内的(财务招标)，行情是近两天的：<select id="isadd">
		<option value='0'>否</option>
		<option value='1' selected>是</option>
	</select><br/>
	指标类型：<select id="indextype">
		<option value='0' selected>财务指标</option>
		<option value='1' >行情指标</option>
	</select>
	数据类型：<select id="utype">
		<option value='0' selected>沪深</option>
		<option value='1' >港股</option>
		<!-- <option value='2' >指数</option> -->
	</select><br/>
	起始时间：<input type="text" id="stime" name="stime" value="2011-03-31" /><br/>
	==========================================指标运算===============================================<br/>
	<input type="button" id="taskAllButton" name="taskAllButton" value="运行任务(全量)" /><br/>
	<input type="button" id="taskAddAllIndexCompanyButton" name="taskAddAllIndexCompanyButton" value="运行任务(全量_逐个公司计算)" /><br/>
	<input type="button" id="taskAddButton" name="taskAddButton" value="运行任务(增量_指标更新)" /><br/>
	<input type="button" id="taskAddNewIndexCompanyButton" name="taskAddNewIndexCompanyButton" value="运行任务(增量_指标更新但逐个公司来运算)" /><br/>
	<input type="button" id="taskAddCompanyButton" name="taskAddCompanyButton" value="运行任务(增量_公司数据更新)" /><br/>
	<input type="button" id="computeAllIndexByCompanyPage" name="computeAllIndexByCompanyPage" value="分页计算公司的所有指标" />
	<select id="page">
		<option value="0">0</option>
		<option value="1">1</option>
		<option value="2">2</option>
		<option value="3">3</option>
		<option value="4">4</option>
		<option value="5">5</option>
		<option value="6">6</option>
		<option value="7">7</option>
		<option value="8">8</option>
		<option value="9">9</option>
		<option value="10">10</option>
		<option value="11">11</option>
		<option value="12">12</option>
		<option value="13">13</option>
		<option value="14">14</option>
		<option value="15">15</option>
		<option value="16">16</option>
		<option value="17">17</option>
		<option value="18">18</option>
		<option value="19">19</option>
	</select>
	<input type="button" id="computeAllIndexByCompanyPage_all" name="computeAllIndexByCompanyPage_all" value="分页计算公司的所有指标_连续" /><br/><br/>
	
	<input type="button" id="computeIndIndexByTag" name="computeIndIndexByTag" value="分行业计算与行业相关的指标" />
	<select id="indtag">
		<option value="">所有行业</option>
		</select>
	<input type="button" id="computeIndIndexByTag_all" name="computeIndIndexByTag_all" value="分行业计算与行业相关的指标_连续" /><br/><br/>
	
	
	<input type="button" id="onecompanyoneindex" name="onecompanyoneindex" value="计算指定公司指定指标" />
	公司编码：<input type="text" id="companycode" name="companycode" />
	指标编码：<input type="text" id="indexcode" name="indexcode" /><br/>
	
	<input type="button" id="computeIndexDateUpdate" name="computeIndexDateUpdate" value="计算指定时间数据有公司的指标" />
	更新时间：<input type="text" id="upTime" name="upTime" value="2012-09-30"/>
	是否发邮件 ：<input type="text" id="sendmail" name="sendmail" value="0"/><br/>
	<input type="button" id="getcompanyByUpdateTime" name="getcompanyByUpdateTime" value="查询指标时间数据更新的公司" />
	更新时间：<input type="text" id="getupTime" name="getupTime" value="2012-09-30"/><br/>
	<div id="showUpdateCompany"></div>
	==========================
	<input type="button" id="importAllCompanySPrice" name="importAllCompanySPrice" value="导入所有公司的股价及基金数" />
	起始时间：<input type="text" id="pstime"  />
	结束时间：<input type="text" id="petime" /><br/>
	
	
	<input type="button" id="setTimeCompute1" name="setTimeCompute1" value="凌晨定时计算财务" />
	<input type="button" id="setTimeCompute3" name="setTimeCompute3" value="凌晨定时计算行情" />
	<input type="button" id="setTimeCompute2" name="setTimeCompute2" value="上后10点数据导入后的定时计算" />
	
	<input type="button" id="realtimeDataCompute" name="realtimeDataCompute" value="行情数据拉取与计算" />
	<br/>
	<input type="button" id="inittradebitmap" name="inittradebitmap" value="初始化tradebitmap" /><br/>
	<input type="text" id="checktime" name="checktime" value="" placeholder="请输入校对起始时间"/>
	<input type="text" id="checktype" name="checktype" value="" placeholder="类型：0:只修复bitmap,1:修改bitmap和行情"/>
	<input type="button" id="checktradebitmap" name="checktradebitmap" value="校对tradebitmap" />
	<input type="button" id="loadTradeUextDataFromTime" name="loadTradeUextDataFromTime" value="加截行情与扩展数据" />
	<br/>
	<input type="button" id="computeHKBaseIndexByPageCompany" name="computeHKBaseIndexByPageCompany" value="分页计算港股基本数据" />
	
	
	<br/>
	<input type="button" id="mockTest" name="mockTest" value="模拟发数据" />
	<input type="button" id="mockSave" name="mockSave" value="模拟数据保存" />
	<input type="button" id="mockSaveCopy" name="mockSaveCopy" value="模拟数据保存_复本" />
	<input type="button" id="mockClearStatistic" name="mockClearStatistic" value="清理实时统计数据" />
	<input type="button" id="mockPrintStatistic" name="mockPrintStatistic" value="打印实时统计数据" />
	<input type="button" id="mockWstockReload" name="mockWstockReload" value="重新加载模拟数据" />
	<input type="button" id="clearPreRulecheckFilter" name="clearPreRulecheckFilter" value="清理filter" />
	<input type="button" id="mockStop" name="mockStop" value="停止mock" />
	
	<br/>
	<input type="text" id="mockTest_companycode" name="mockTest_companycode" value="" placeholder="公司编码"/>
	<input type="text" id="mockTest_date" name="mockTest_date" value="" placeholder="日期"/>
	<input type="text" id="mockTest_stime" name="mockTest_stime" value="" placeholder="数据区间结束时间"/>
	<input type="text" id="mockTest_etime" name="mockTest_etime" value="" placeholder="数据区间结束时间"/>
	<input type="text" id="mockTest_type" name="mockTest_type" value="" placeholder="0:格式化数据，1：原始数据"/>
	
	<br/><br/>
	
	<textarea  id="encryptstr" style="width:100%;height:150px"></textarea>
	<input type="button" id="encryptclear" name="encryptclear" value="clear" />
	<input type="button" id="encrypt" name="encrypt" value="encrypt" />
	<input type="button" id="printRule" name="printRule" value="printRule" /><br/>
	<textarea  id="encryptret" style="width:100%;height:150px"></textarea>
</div>
 
 </div>
</body>
</html>

