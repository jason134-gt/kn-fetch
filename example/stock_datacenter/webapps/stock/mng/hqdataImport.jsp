<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<html>
<head>
 <title>价值投资分析系统</title>
<script src="js/jquery/jquery-1.6.2.js" type="text/javascript"></script>
<script type="text/javascript" src="js/base.js"></script>
 <script type="text/javascript">
var ca;
$(document).ready(function() {
	
	loadCompanys();
	$("#fetch").click(function(){
		$("#middle-div").empty();
		var type = $("#type").val();
		if(type==1)
		{
			var c = $("#values").val();
			$("#values").val("");
			$("#middle-div").append(c);
			var fsr = "";
			$("#middle-div").find("a").each(function(){
				var href = $(this).attr("href");
				if(href==null) return;
				var ncode = href.match(/[0-9]{6}/g);
				var v = $(this).text().trim();
				var code = v.match(/[0-9]{6}/g);
				if(code==null||code.length==0)
				{
					code = ncode;
				}
				if(code==null) return;
				var ccode = code.toString();
				for(var i=0;i<ca.length;i++)
				{
						if(ca[i]=="") continue;
						var caa = ca[i].split(":");
						var companyname = caa[0];
						var companycode = caa[1];
						if(companycode.split(".")[0]==ccode&&fsr.indexOf(companycode)<0)
							fsr+=companyname+":"+companycode+";";
				}
			});
			$("#values").val(fsr);
		}
		importData();
	});
	
	$("#url-nav").find("a").click(function(){
		var v = $(this).text();
		$("#name").val(v);
		$("#values").val("");
		
	});
	$("#import").click(function(){
		importData();
	});
	
	
});//end ready
function loadCompanys()
{
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("<%=request.getContextPath()%>/company/getCompanyByTableSystemString_v3", function(result){
		if(retObj==null)
			{
				return;
			}
		ca = retObj.split(";");
		
		
	});
}
function importData()
{
		var name = $("#name").val();
		var type = $("#type").val();
		var values = $("#values").val();
		if(name==""||type==""||values=="")
			{
				alert("参数不可以为空！");
				return ;
			}
		var nvalues="";
		var va = values.split(";");
		for(var i=0;i<va.length;i++)
				{
					if(va[i]=="") continue;
					var caa = va[i].split(":");
					var companycode="";
					if(caa.length==1)
						companycode=caa[0];
					if(caa.length>1)
						companycode = caa[1];
					if(companycode!="")
						nvalues+=companycode+";";
				}
		var data = {
			name:name.trim(),
			type:type.trim(),
			values:nvalues.trim()
		}
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.post("<%=request.getContextPath()%>/stockpool/addAStockpoolItem",data, function(result){
			alert("操作结束");
		});
}

</script>

</head>
<body>
 需要手动抓取的信息:
 <div id="url-nav">
 	<a href="http://vip.stock.finance.sina.com.cn/q/go.php/vDYData/kind/hylt/index.phtml" target="_blank">领涨龙头</a>
 	<a href="http://vip.stock.finance.sina.com.cn/q/go.php/vDYData/kind/qszs/index.phtml" target="_blank">趋势转升</a>
 	<a href="http://vip.stock.finance.sina.com.cn/q/go.php/vDYData/kind/qszd/index.phtml" target="_blank">趋势转跌</a>
 	<a href="http://vip.stock.finance.sina.com.cn/q/go.php/vDYData/kind/cqgpc/index.phtml" target="_blank">超强买入</a>
 	<a href="http://vip.stock.finance.sina.com.cn/q/go.php/vIR_EndRise/index.phtml" target="_blank">尾盘拉升股</a>
 	<a href="http://vip.stock.finance.sina.com.cn/q/go.php/vIR_EndFall/index.phtml" target="_blank">尾盘打压股</a>
 	<a href="http://vip.stock.finance.sina.com.cn/q/go.php/vIR_IntervalRelease/index.phtml" target="_blank">资金吸筹股</a>
 	<a href="http://vip.stock.finance.sina.com.cn/q/go.php/vIR_Burstout/index.phtml" target="_blank">即将爆发牛股</a>
 	<a href="http://vip.stock.finance.sina.com.cn/datacenter/hqstat.html#lxfl" target="_blank">连续放量个股</a>
 	<a href="http://vip.stock.finance.sina.com.cn/datacenter/hqstat.html#lxsl" target="_blank">连续缩量个股</a>
	<a href="http://vip.stock.finance.sina.com.cn/datacenter/hqstat.html#30xg" target="_blank">盘中创新高个股</a>
 	<a href="http://vip.stock.finance.sina.com.cn/datacenter/hqstat.html#30xd" target="_blank">盘中创新低个股</a>
	<a href="http://vip.stock.finance.sina.com.cn/datacenter/hqstat.html#dqzd" target="_blank">短期涨跌统计Top50</a>
 	<a href="http://vip.stock.finance.sina.com.cn/datacenter/hqstat.html#cqzd" target="_blank">长期涨跌统计Top50</a>
	<a href="http://vip.stock.finance.sina.com.cn/datacenter/hqstat.html#zzd" target="_blank">周涨跌排名Top50</a>
 	<a href="http://vip.stock.finance.sina.com.cn/datacenter/hqstat.html#yzd" target="_blank">月涨跌排名Top30</a>
	<a href="http://finance.qq.com/data/#zlzcpm" target="_blank">当日主力增仓Top50</a>
	<a href="http://finance.qq.com/data/#zljcpm" target="_blank">当日主力减仓Top50</a>
 </div>
 <div style="width:761">
	<table style="width:100%">
		<tr><td>名字</td><td><input id="name" type="text" /></td></tr>
		<tr>
			<td>类型</td>
			<td>
				<select id="type">
					<option value='1' selected>行情相关</option>
					<option value='2'>板块</option>
				</select>
			</td>
		</tr>
		<tr><td>内容</td><td><textarea style="width:100%;height:300px" id="values" type="text" ></textarea></td></tr>
		<tr><td>操作</td><td><input id="fetch" type="button" value="提取"/><!--<input id="import" type="button" value="导入"/>--></td></tr>
	</table>
 </div>
 <div id="middle-div" style="width:761px;height:800px" >
	
 </div>
</body>
</html>

