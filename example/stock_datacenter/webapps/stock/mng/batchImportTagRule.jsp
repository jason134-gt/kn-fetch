<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<html>
<head>
 <title>价值投资分析系统</title>
<script src="js/jquery/jquery-1.6.2.js" type="text/javascript"></script>
 <script type="text/javascript">
var chart;
var type = 'spline';
$(document).ready(function() {
	$("dt").click(function(){                  
        $("dd").css("display","none"); 
        $(this).next("dd").css("display","block");
    });//end click

});//end ready

function batchImportTagrule(id){
var text = $("#newTagrule").val();
	var data = {
		text:text
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/tagrule/batchCreateTagRule",data, function(result){
 	  alert("操作结束");
	});
}

</script>

</head>
<body>
<input type="button" onclick="batchImportTagrule()" value="导入"/>用"name;rule;chartdesc"号分格<br/>
<textarea id="newTagrule" style="width:500px;height:600px;overflow-x:auto"></textarea><br/>

</body>
</html>

