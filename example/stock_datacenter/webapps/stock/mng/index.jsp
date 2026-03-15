<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<html>
<head>
 <title>数据中心</title>
 <link rel="stylesheet" type="text/css" href="/stock/mng/css/InvestAnalysis1.css">
<link rel="stylesheet" type="text/css" href="/stock/mng/js/jquery-autocomplete/jquery.autocomplete.css" />
<script src="/stock/mng/js/jquery/jquery-1.6.2.min.js" type="text/javascript"></script>
<script src="/stock/mng/js/highcharts/js/highcharts.src.js" type="text/javascript"></script>
<script type='text/javascript' src='/stock/mng/js/jquery-autocomplete/jquery.autocomplete.js'></script>
<script type='text/javascript' src='/stock/mng/js/util/Calendar.js'></script>
<script type='text/javascript' src='/stock/mng/js/mycharts.js'></script>
<script type='text/javascript' src='/stock/mng/js/indexAnalysis.js'></script>

<script type="text/javascript" src="js/login.js"></script>
<script type="text/javascript" src="js/artDialog4.1.7/artDialog.source.js?skin=blue"></script>
<script type="text/javascript" src="js/rsa/jsbn/jsbn.js"></script> 
<script type="text/javascript" src="js/rsa/jsbn/prng4.js"></script>
<script type="text/javascript" src="js/rsa/jsbn/rng.js"></script>
<script type="text/javascript" src="js/rsa/jsbn/rsa.js"></script>
 <script type="text/javascript">
var chart;
var type = 'spline';

 $(function(){
	$(".logout").click(function(){
			loginOut();
	}); 
/* 	$(".noactive").click(function(){
		var id = $(this).attr("id")
		var num = Number(id);
		var url = "/stock";
		var p;
		switch(num){
			case 1:	
				p = "/mng/rule.jsp";
				break;
			case 2:	
				p = "/mng/conditionSet.jsp";
				break;
			case 3:	
				p = "/mng/setreferencevalue_2.jsp";
				break;
		} 
		url = url+p;
		$.post(url, function(result){
	    	$("#contentDiv").html(result);
		});
		return false;
	});  */
}) 

$(document).ready(function() {
	 var uid = getCookie("adminuid");
	 var isadmin = getCookie("cn.yfzx.admin");
	if (uid == "" || uid == null || isadmin=="" || isadmin==null) {
		goLogin();
		return false;
	}else{
		$(".logout").text("退出");
	}  
	$("dt").click(function(){                  
        $("dd").css("display","none"); 
        $(this).next("dd").css("display","block");
    });//end click

});//end ready

function mouseOver(id){
document.getElementById(id).style.fontSize="1.6em";
document.getElementById(id).style.backgroundColor="#86a0e9";
}
function mouseOut(id){
document.getElementById(id).style.fontSize="1.3em";
document.getElementById(id).style.backgroundColor="transparent";
}
function loadData2Content(url)
{
	 var uid = getCookie("adminuid");
		if (uid == "" || uid == null) {
			goLogin();
			return false;
		}else{
			$(".logout").text("退出");
		}  
	$.post("/stock"+url, function(result){
    	$("#contentDiv").html(result);
	});
} 
</script>

</head>
<body>
 <div class="content">
 <div class="main-header">
  <h1>价值投资分析系统</h1>
<a class="logout" style="margin-right: 20px;  float: right; color:red;cursor:pointer"></a>
  </div> <!-- header ends -->
 <!-- main begins -->
  
 <div class="main">
		<div class="col-left">
		  <div class="col-left-in">
		   <dl class="left-border">

			<dt id="CompanyID" class="dthead" onmouseover="mouseOver('CompanyID')" onmouseout="mouseOut('CompanyID')">投资分析</dt>
			<dd class="ddlist">
			 <ul class="ullist">
			  <li><a class="noactive" href="" onclick="loadData2Content('/mng/analysis_2.jsp');return false;">实时分析</a></li>
			 </ul>    
			</dd>
			
			<dt id="RuleID" class="dthead" onmouseover="mouseOver('RuleID')" onmouseout="mouseOut('RuleID')">后台管理</dt>
			<dd class="ddlist">
			 <ul class="ullist">
			 <li><a class="noactive" href="" onclick="loadData2Content('/mng/rule.jsp');return false;">规则管理</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/conditionSet.jsp');return false;">条件引擎管理</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/setreferencevalue_2.jsp');return false;">设置参考值</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/tagrule.html');return false;">打标规则管理</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/sizer.jsp');return false;">股筛</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/dcheck.html');return false;">数据校验规则定义</a></li>
			    <li><a class="noactive" href="" onclick="loadData2Content('/mng/cfscorerule.html');return false;">财务评分</a></li>
			    <li><a class="noactive" href="" onclick="loadData2Content('/mng/batchValuation.jsp');return false;">估值</a></li>
			    <li><a class="noactive" href="" onclick="loadData2Content('/mng/hq.jsp');return false;">行情数据导入</a></li>
			    <li><a class="noactive" href="" onclick="loadData2Content('/mng/hqdataImport.jsp');return false;">股票池导入</a></li>
			    <li><a class="noactive" href="" onclick="loadData2Content('/mng/batchImportTagRule.jsp');return false;">批量导入规则</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/rankingDefine.jsp');return false;">榜单定义</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/rankCompute.jsp');return false;">榜单计算</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/rankresult.jsp');return false;">榜单结果查看</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/task.jsp');return false;">任务管理</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/dataImportAndExport.jsp');return false;">数据导入导出</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/dataCompute.jsp');return false;">数据计算</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/capCompute.jsp');return false;">能力评分</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/addWeight2Index.jsp');return false;">定义权重</a></li>
			   <li><a class="noactive" href="" onclick="loadData2Content('/mng/mmmaompute.jsp');return false;">行业值计算</a></li>
			    <li><a class="noactive" href="" onclick="loadData2Content('/mng/tagruleCompute.jsp');return false;">给公司打财务标签</a></li>
				<li><a class="noactive" href="" onclick="loadData2Content('/mng/datascan.jsp');return false;">扫雷</a></li>
				<li><a class="noactive" href="" onclick="loadData2Content('/mng/cache_mng.jsp');return false;">刷新缓存</a></li>
				<li><a class="noactive" href="" onclick="loadData2Content('/mng/rulemonitor.jsp');return false;">监控</a></li>
			 </ul>    
			</dd>
			
			
		   </dl>       
		  </div> <!-- /col-left-in -->   
		 </div> <!-- /col-left -->
	
	<div class="col-center-content" id="contentDiv"></div>
</div> <!-- main ends -->
 
 </div>
 <!-- content ends -->
 <script language="javascript">
 var rows = document.getElementsByTagName('tr');
 for (var i=0;i<rows.length;i++){
  rows[i].onmouseover = function(){  //鼠标在行上面的时候
   this.className += 'altrow';
   }
 rows[i].onmouseout = function(){  //鼠标离开时
  this.className = this.className.replace('altrow','');
  }
  }
 </script>
 
</body>
</html>

