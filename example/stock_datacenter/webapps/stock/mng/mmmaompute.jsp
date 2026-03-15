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
	initAutoIndexMatcher("rindexcode_0");
	initAutoIndexMatcher("indexcode_0");
					
	$("#computemma").click(function(){
		var useOldData = $("#useOldData").val();
		if(useOldData=='1') useOldData = 'true';
		 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   $.post("<%=request.getContextPath()%>/task/computeMaxMinAvg?useOldData="+useOldData,function(result){
			   if(result.success)
				  {
				  	alert("操作结束!"); 
				  }
		   });//end getJSON					
	});//end click
	
	
	$("#onetagoneindexcomputemma").click(function(){
		var useOldData = $("#useOldData").val();
		if(useOldData=='1') useOldData = 'true';
		var tag = $("#tag").val();
		var indexcode = $("#indexcode_0").val().split(":")[1];
		 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   $.post("<%=request.getContextPath()%>/task/computeMaxMinAvgOneTagOneIndex?useOldData="+useOldData,{"tag":tag,"indexcode":indexcode},function(result){
			   if(result.success)
				  {
				  	alert("操作结束!"); 
				  }
		   });//end getJSON					
	});//end click
	
	$("#computemmatime").click(function(){
		var useOldData = $("#useOldData").val();
		if(useOldData=='1') useOldData = 'true';
		var sTime = $("#sTime").val();
		var eTime = $("#eTime").val();
		 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   $.post("<%=request.getContextPath()%>/task/computeMaxMinAvgOnetime?useOldData="+useOldData,{"sTime":sTime,"eTime":eTime},function(result){
			   if(result.success)
				  {
				  	alert("操作结束!"); 
				  }
		   });//end getJSON					
	});//end click
	
	
					$("#computeravg").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') useOldData = 'true';
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/task/computeRAvg?useOldData="+useOldData,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					
					$("#onetagoneindexcomputeravg").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') useOldData = 'true';
						var tag = $("#rtag").val();
						var indexcode = $("#rindexcode_0").val().split(":")[1];
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/task/computeRAvgOneTagOneIndex?useOldData="+useOldData,{"tag":tag,"indexcode":indexcode},function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					
					
					
					$("#computeravgtime").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') useOldData = 'true';
						var sTime = $("#rsTime").val();
						var eTime = $("#reTime").val();
						var page = $("#page").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/task/computeRAvgOnetimeByPage?useOldData="+useOldData,{"sTime":sTime,"eTime":eTime,"page":page},function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click

				
					
				
});//end ready

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
	==========================================指标运算===============================================<br/>

	<input type="button" id="computemma" name="computemma" value="计算MMMA(全量)" /><br/>
	<input type="button" id="onetagoneindexcomputemma" name="onetagoneindexcomputemma" value="计算指定分类指定指标的MMMA" />
	分类tag名：<input type="text" id="tag" name="tag" />
	指标编码：<input type="text" id="indexcode_0" name="indexcode_0" />备注：如果指标为空则计算的是指标签的所有指标<br/>
	<input type="button" id="computemmatime" name="computemmatime" value="计算指定时间段内的MMMA" />
	开始时间：<input type="text" id="sTime" name="sTime" value="2010-09-30"/>
	结束时间：<input type="text" id="eTime" name="eTime" value="2012-09-30"/><br/>
	
	======================================================================<br/>
	<input type="button" id="computeravg" name="computeravg" value="计算RAvg(全量)" /><br/>
	<input type="button" id="onetagoneindexcomputeravg" name="onetagoneindexcomputeravg" value="计算指定分类指定指标的Ravg" />
	分类tag名：<input type="text" id="rtag" name="rtag" />
	指标编码：<input type="text" id="rindexcode_0" name="rindexcode_0" />备注：如果指标为空则计算的是指标签的所有指标<br/>
	<input type="button" id="computeravgtime" name="computeravgtime" value="计算指定时间段内的RAvg(分页)" />
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
	开始时间：<input type="text" id="rsTime" name="rsTime" value="2010-09-30"/>
	结束时间：<input type="text" id="reTime" name="reTime" value="2012-09-30"/><br/>
	
</div>
 
 </div>
</body>
</html>

