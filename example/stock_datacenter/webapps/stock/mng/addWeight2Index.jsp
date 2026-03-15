<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@page import="com.yz.stock.portal.model.*"   %> 
<%@page import= "java.util.*"   %> 
<%@page import= "com.yz.mycore.lcs.enter.LCEnter"   %> 
<%@page import= "com.push.stock.common.*"   %> 
<%@page import= "com.yz.stock.portal.service.*"   %> 
<%@page import= "com.stock.common.util.*"   %> 
<%@page import= "com.stock.common.model.*"   %> 
<%@page import= "com.stock.common.constants.*"   %> 
<%@page import= "com.yfzx.service.db.*"   %> 


<html>
<head>
 <title>指标打标</title>
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



});//end ready

function saveWeight()
{
	var indexarray = document.getElementsByName("weight");
	var ups = "" ;
	for(var i=0;i<indexarray.length;i++)
		{
			var indexid = indexarray[i].id;
			var wexp = indexarray[i].value;
			var bid = indexid+"_bit"
			var bit = $("#"+bid).val();
			ups+=indexid+"#"+wexp+"#"+bit;
			ups+=",";
		}
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/dictionaryMng/updateMutIndexWeightCondition",{"ups":ups}, function(result){
			
			alert("操作结束");
			
	  });
}
function queryIndexsOfTag()
{
	$("#Index_tbody_dataTable").empty();
	var tagcode = $("#tagcode").val()+"_sw";
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("<%=request.getContextPath()%>/dict/getIndexsOfOneTags?tagcode="+tagcode, function(result){
			
			if(result!=null)
			{
				showResult(result);
			}
			
	  });
}

function showResult(result)
{
	var data = jQuery.parseJSON(result);
	//var data = result;
	if(data==""||data==null)
		return;
	var i;
	for(i=0;i<data.length;i++)
		{
			var cl = data[i];
			var rowHtml = "<tr class='alt1'>";
			rowHtml +="<td>"+cl.indexCode+"</td>";
			rowHtml +="<td>"+cl.columnChiName+"</td>";
			rowHtml +="<td><textarea  name='weight' id='"+cl.indexCode+"' style='width:100%'>"+cl.weightExp+"</textarea></td>";
			var bit = cl.bitSet.charAt(2);
			rowHtml +="<td><input type='text' name='weight_bit' id='"+cl.indexCode+"_bit' value='"+bit+"'/></td>";
			rowHtml +="</tr>";
			$("#Index_tbody_dataTable").append(rowHtml);
		}
}

</script>
</head>
<body>


		 <div>
		 <form id="IndexQueryForm">
				指标分类：<select id="tagcode">
					<option value="10005" selected>盈利质量指标</option>
					<option value="10007" >盈利能力指标</option>
					<option value="10008" >运营能力指标</option>
					<option value="10009" >管控能力指标</option>
					<option value="10010" >成长能力指标</option>
					<option value="10016" >偿债能力指标</option>
					<option value="10017" >综合能力指标</option>
				</select>
				<input type="button" id="queryIndexWeightOfTagButton" name="queryIndexWeightOfTagButton" value="查询" onclick="queryIndexsOfTag()"/>
				<input type="button" id="saveIndexsButton" name="saveIndexsButton" value="保存" onclick="saveWeight()"/>
		</form>
		</div>
		<div id="div_Index_table" style="overflow:auto;height:auto">
					 <table class="datalist2">
						<thead id="Index_thead_dataTable"></thead>
						<tr>
							<td>指标编码</td>
							<td>指标名</td>
							<td>权重表达式</td>
							<td>升(0)/降(1)</td>
						</tr>
						<tbody id="Index_tbody_dataTable"></tbody>
					 </table>
		</div>


</body>
</html>

