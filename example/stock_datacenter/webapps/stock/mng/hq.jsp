<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<html>
<head>
 <title>价值投资分析系统</title>
<script src="js/jquery/jquery-1.6.2.js" type="text/javascript"></script>
 <script type="text/javascript">

$(document).ready(function() {

});//end ready

function importhq_selectstock(){

	var type = $("#type").val(); 
	if(type==0)
		{
			var b = confirm("清除此类型原有数据");
			if(!b)
				return;
		}
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("<%=request.getContextPath()%>/stockpool/importHqData_selectstock?type="+type, function(result){
		alert("操作结束");
	});
}
function importhq_plates(){
	var type = $("#type").val(); 
	if(type==0)
		{
			var b = confirm("清除此类型原有数据");
			if(!b)
				return;
		}
	
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("<%=request.getContextPath()%>/stockpool/importhq_plates?type="+type, function(result){
		alert("操作结束");
	});
}

function refresh_txData(){
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("<%=request.getContextPath()%>/stockpool/refreshStockpool", function(result){
		alert("操作结束");
	});
}
function refresh_stockpool(){
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("<%=request.getContextPath()%>/cache/refresh?dtype=stockpool", function(result){
		alert("操作结束");
	});
}
</script>

</head>
<body>
<input type="button"  onclick="importhq_selectstock()" value="导入行情选股数据"/>
<input type="button"  onclick="importhq_plates()" value="导入行情板块股数据"/>
<select id="type">
		<option value='1' selected>不清除原有类型数据</option>
		<option value='0'>清除原有类型数据</option>
</select><br/>
<input type="button"  onclick="refresh_txData()" value="刷新腾讯特色数据"/>
<input type="button"  onclick="refresh_stockpool()" value="刷新股票池"/>
</body>
</html>

