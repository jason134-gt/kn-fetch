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
	
					$("#batchComputeCfTagByPage").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') useOldData = 'true';
						var page = $("#page").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("/stock/tagrule/batchAddCfTag2Company?useOldData="+useOldData+"&page="+page,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#batchComputeCfTagByPage_all").click(function(){
						var useOldData = $("#useOldData").val();
						if(useOldData=='1') useOldData = 'true';
						var page = $("#page").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("/stock/tagrule/batchComputeCfTagByPage_all?useOldData="+useOldData+"&page="+page,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#batchAddCfTag_CompanyDateUpdate").click(function(){
						 var uptime = $("#uptime").val();
						 var page = $("#page").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("/stock/tagrule/batchAddCfTag_CompanyDateUpdate?uptime="+uptime+"&page="+page,function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click
					
					$("#computeUnnormalTagrule").click(function(){
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("/stock/tagrule/computeUnnormalTagrule",function(result){
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
	是否使用老数据：<select id="useOldData">
		<option value='0'>否</option>
		<option value='1' selected>是</option>
	</select><br/>
	<input type="button" id="batchComputeCfTagByPage" name="batchComputeCfTagByPage" value="批量给公司打财务标签" />
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
	<input type="button" id="batchComputeCfTagByPage_all" name="batchComputeCfTagByPage_all" value="批量给公司打财务标签_连续（不需要预加载财务数据）" />
	<br/><input type="button" id="batchAddCfTag_CompanyDateUpdate"  value="给数据有更新的公司打标" />
	<input type="text" id="uptime"  value="" />
	<br/><br/>
	<input type="button" id="computeUnnormalTagrule"  value="计算需要预加载财务数据的规则(如能力排名等)" />
	
	<br/>
	
</div>
 
 </div>
</body>
</html>

