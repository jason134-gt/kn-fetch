<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<html>
<head>
 <title>价值投资分析系统</title>
<script src="js/jquery/jquery-1.6.2.js" type="text/javascript"></script>
 <script type="text/javascript">

$(document).ready(function() {

});//end ready

function batchvaluation(){
var stime = $("#stime").val();
var ftime = $("#stime").val();
	var data = {
		stime:stime,
		ftime:ftime
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("<%=request.getContextPath()%>/valuation/getYfValuationTest?"+$.param(data), function(result){
		var ret = batchYfvaluation;
		var ra = ret.split("~");
		$("#valua-test").empty();
					ra.sort(function(a, b){
									if(a=="") return 1;
									if(b=="") return -1;
									var va=a.split("^")[3]; ;
									var vb=b.split("^")[3] ;
									if(va==null||va==="NaN"||va=="Infinity") return 1;
									if(vb==null||vb==="NaN"||vb=="Infinity") return -1;
									var ra = parseFloat(va);
									var rb = parseFloat(vb);
									if(ra== rb) return 0;
									return ra> rb? -1: 1;
								});
								
		for(var i=0;i<ra.length;i++)
		{
			var s = ra[i];
			if(s=="") continue;
			var sa = s.split("^");
			var companys = sa[0];
			var vv = parseFloat(sa[1]).toFixed(2);
			if(vv==null||vv==="NaN"||vv=="Infinity") continue;
			var cprice = parseFloat(sa[2]).toFixed(2);
			var ration = (parseFloat(sa[3])*100).toFixed(2);
			var url="<%=request.getContextPath()%>/company_main.html?companycode="+companys.split(":")[1]+"&companyname="+escape(companys.split(":")[0])
			$("#valua-test").append("<tr value="+ration+"><td><a href='"+url+"' target='_blank' >"+companys+"</a></td><td>"+cprice+"</td><td>"+vv+"</td><td>"+ration+"</td></tr>");
		}
	});
}
function showRation()
{
	var v = $("#glr").val();
	$("#valua-test").find("tr").each(function(){
	
		var ration = $(this).attr("value");
		if(parseFloat(ration)<parseFloat(v))
			$(this).hide();
		else
			$(this).show();
	});
}
function showAB()
{
	var v = $("#glrAB").val();
	$("#valua-test").find("tr").each(function(){
	
		var name = $(this).find("td").first().text();
		if(v==0)
		{
			if(name.indexOf("Ｂ")>0||name.indexOf("B")>0||name.indexOf("pb")>0)
				$(this).hide();
			else
				$(this).show();
		}
		if(v==1)
		{
			if((name.indexOf("Ｂ")>=0||name.indexOf("B")>=0)&&name.indexOf("pb")<0)
				$(this).show();
			else
				$(this).hide();
		}
		if(v==-1)
		{
			$(this).show();
		}
		if(v==2)
		{
			if(name.indexOf("Ｂ")>0||name.indexOf("B")>0)
				$(this).hide();
			else
				$(this).show();
		}
		if(v==3)
		{
			if(name.indexOf("Ｂ")>=0||name.indexOf("B")>=0)
				$(this).show();
			else
				$(this).hide();
		}
	});
}
</script>

</head>
<body>
stime:<input type="text" id="stime" value="2013-03-30"/>ftime:
<input type="text" id="ftime" value="2013-03-30"/>
<input type="button" id="valuation" onclick="batchvaluation()" value="估值"/>

<table style="width:600px;">
	<thead>
		<th>公司
			<select onchange="showAB()" id="glrAB">
				<option value="-1">--</option>
				<option value="0">A</option>
				<option value="1">B</option>
				<option value="2">A_with_pb</option>
				<option value="3">B_with_pb</option>
			</select>
		</th><th>当前股价</th><th>估值</th>
		<th>差率
			<select onchange="showRation()" id="glr">
				<option value="0">--</option>
				<option value="30">30</option>
				<option value="60">60</option>
				<option value="100">100</option>
			</select>
		</th>
	</thead>
	<tbody id="valua-test">
	
	</tbody>
</table>

 
</body>
</html>

