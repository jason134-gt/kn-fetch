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

	initAllTag("tagcode");
	initAllTag("tag");
	
	initIndustry();
	initYear("startTime_year",18);
	$("#industry_0").append("<option value='-1' selected>请选择行业</option>");
	$("#industry_0").change(function (){
		var industrycode = $("#industry_0").val().split(":")[1];
		var rd = getIndustryBycode("industry_1","z",industrycode);
		currenttag = $("#industry_0").val().split(":")[0];
		 ctagcode = $("#industry_0").val().split(":")[1];
	});
	
	$("#industry_1").change(function (){
		var industrycode = $("#industry_1").val().split(":")[1];
		var rd = getIndustryBycode("industry_2","z",industrycode);
		currenttag = $("#industry_1").val().split(":")[0];
		ctagcode = $("#industry_1").val().split(":")[1];
	});
	$("#industry_2").change(function (){
		var industrycode = $("#industry_2").val().split(":")[1];
		var rd = getIndustryBycode("industry_3","z",industrycode);
		currenttag = $("#industry_2").val().split(":")[0];
	});
	$("#showchart").click(function (){
		showScatterChart();
	});
	
	$("#markcompany").click(function (){
			$("#markcompany").val("");
		});
	$("#industry_3").change(function (){
		currenttag = $("#industry_3").val().split(":")[0];
	});
	
					$("#computecapSection").click(function(){
						var stime = $("#sTime").val();
						var etime = $("#eTime").val();
						var type = $("#type").val();
						var loaddata = $("#loaddata").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/caprank/caprankcompute",{"stime":stime,"etime":etime,"type":type,"loaddata":loaddata},function(result){
							   alert("操作结束");
						   });//end getJSON					
					});//end click
					
					$("#computeCapOneTime").click(function(){
						var time = $("#time").val();
						var type = $("#type").val();
						var loaddata = $("#loaddata").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/caprank/caprankcomputeOneTime",{"time":time,"type":type,"loaddata":loaddata},function(result){
							  	alert("操作结束");
						   });//end getJSON					
					});//end click
					
					$("#computeCapOneTimeOneTag").click(function(){
						var time = $("#ttime").val();
						var testIndex = $("#testIndex").val();
						var tag = $("#tag").val();
						var loaddata = $("#loaddata").val();
						 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.post("<%=request.getContextPath()%>/caprank/caprankcomputeOneTimeOneTag",{"time":time,"tag":tag,"testIndex":testIndex,"loaddata":loaddata},function(result){
							  	alert("操作结束");
						   });//end getJSON					
					});//end click
				
	//存入点击列的每一个TD的内容；
    var aTdCont = [];

   //点击列的索引值
    var thi = 0
    
    //重新对TR进行排序
    var setTrIndex = function(tdIndex){
        for(i=0;i<aTdCont.length;i++){
            var trCont = aTdCont[i];
            $("tbody tr").each(function() {
                var thisText = $(this).children("td:eq("+tdIndex+")").text();
                if(thisText == trCont){
                    $("tbody").append($(this));
                }
             });        
        }
    }
    
    //比较函数的参数函数
    var compare_down = function(a,b){
            return a-b;
    }
    
    var compare_up = function(a,b){
            return b-a;
    }
    
    //比较函数
    var fSort = function(compare){
        aTdCont.sort(compare);
    }
    
    //取出TD的值，并存入数组,取出前二个TD值；
    var fSetTdCont = function(thIndex){
            $("tbody tr").each(function() {
                var tdCont = $(this).children("td:eq("+thIndex+")").text();
                aTdCont.push(tdCont);
            });
    }
    //点击时需要执行的函数
    var clickFun = function(thindex){
        aTdCont = [];
        //获取点击当前列的索引值
        var nThCount = thindex;
        //调用sortTh函数 取出要比较的数据
        fSetTdCont(nThCount);
    }
    
    //点击事件绑定函数
    $("th").toggle(function(){
        thi= $(this).index();
        clickFun(thi);
        //调用比较函数,降序
        fSort(compare_up);
        //重新排序行
        setTrIndex(thi);
    },function(){
        clickFun(thi);
        //调用比较函数 升序
        fSort(compare_down);
        //重新排序行
        setTrIndex(thi);
    }) 
	
});//end ready
function initCompanysOfTag(id,tag)
{
	var data={
		tag:tag
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/company/getCompanyByTagWihtShort?"+$.param(data), function(result){
		    var data = new Array(); 
			var dataArr = retObj.split(";");
				for(var i=0;i<dataArr.length;i++){
					var item = dataArr[i].split(":");
					data.push({"mid":item[1],"realname":item[0]});
			}			
			if(data!=null)
			{
				var c_data = data;
				var option = {
					max: 12, 
					minChars: 0, 
					width: 150,
					scrollHeight: 300, 
					matchContains: true, 
					autoFill: false,
					formatItem: function(item) {
						return item.realname + ":" + item.mid;
					},
					formatMatch: function(item, i, max) { 
						var newName = item.realname.replace(/[ ]/g,"");
						return item.realname + " " + item.mid + " " +newName+" " +" " +ZhToPinyinMaster(newName) + " " + ZhToPinyin(newName);
					}, 
					formatResult: function(item) {
						return item.realname + ":" + item.mid;
					}
				};
				var obj = $("#"+id+"");
				$("#"+id).flushCache();
				obj.autocomplete(c_data, option).result(function(event, item) {
					var markcompanycode = item.mid;
					markCompany2MultChart(cur_chart,markcompanycode);
				});
			}
	  });	
}
var cur_chart;
function markCompany2MultChart(chart,cmarkcompany)
{
			var ssa = chart.series;
			for(var c=0;c<ssa.length;c++)
			{
				var series = ssa[c];
				var ss = series.data;
				var sl = ss.length;
				var markpoint ;
				var unmarkpoint;
				//不可以循环中直接把点移走，这样导致数据下标变化
				for(var p=0;p<sl;p++)
				{
					var cpoint = ss[p];
					if(cpoint.id==cmarkcompany)
					{
						markpoint = cpoint;
					}
					if(cpoint.marker!=null)
					{
						unmarkpoint = cpoint;	
					}
					
				}
				if(markpoint!=null)
					{
						cpoint = markpoint;
						var oid = cpoint.id;
						var oname = cpoint.name;
						var ox = cpoint.x;
						var oy = cpoint.y;
						cpoint.remove(false);
						series.addPoint({
							id:oid,
							name:oname,
							x: ox,
							y: oy,
							dataLabels: {
								enabled: true,
								formatter: function() {
									return '<b>'+this.point.name;
								}
							},
							marker: {
								enabled:true,
								fillColor: '#FFFFFF',
								symbol: 'url(/stock/images/sun.png)'
								}
							},false);
					}
					
					if(unmarkpoint!=null)
					{
						cpoint = unmarkpoint;
						var oid = cpoint.id;
						var oname = cpoint.name;
						var ox = cpoint.x;
						var oy = cpoint.y;
							cpoint.remove(false);
							series.addPoint({
							id:oid,
							name:oname,
							x: ox,
							y: oy
							},false);
					}
			}
			chart.redraw();
}
function initAllTag(id)
{
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/industry/getAllTags", function(result){
		  if(retobj==null||retobj=="")
			{
			  return;
			}
			var data = retobj.tags;
			if(data!=null)
			{
				var c_data = new Array();
				for(var i=0;i<data.length;i++)
				{
					var t = data[i];
					if(t.indexOf("mtag_")>=0)
					c_data.push(t);
				}
				var option = {
					max: 12, 
					minChars: 0, 
					width: 150,
					scrollHeight: 300, 
					matchContains: true, 
					autoFill: false
				};
				var obj = $("#"+id+"");
				obj.autocomplete(c_data, option).result(function(event, item) {
				 });;
			}
	  });	
}
function initIndustry()
{
	//取数据源编码
		var rd = getIndustryBycode("industry_0","z","-1");
}
function getIndustryBycode(id,type,industrycode)
{
	 //先清空
	$("#"+id+"").empty();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("<%=request.getContextPath()%>/industry/getIndustryByTypeAndCode",{"type":"z","industrycode":industrycode}, function(result){
		  var rd = result.data;
		  if(rd==null)
			  return ;
			addIndustry2Select(id,rd);
	  });	
}
function addIndustry2Select(id,sdata)
{ 
	 	if(isNull(sdata))
	 		{
	 			return ;
	 		}
	    
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
				var utext = industryName+":"+industryCode;
				$("#"+id+"").append("<option value="+utext+">"+utext+"</option>");
			}
		}

}




function queryscore(source,indexcode,companycode,time)
{
	$("#scorelog").empty();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("<%=request.getContextPath()%>/caprank/getScoreRecord",{"time":time,"indexcode":indexcode,"companycode":companycode}, function(result){
			
			if(result!=null)
			{
				var log = result;
				alert(log);
				//$("#scorelog").append(log);
				//showScorelog(source);
			}
			
	  });
}
function query()
{
	$("#Index_tbody_dataTable").empty();
	var tag = $("#tagcode").val();
	var time = $("#startTime_year").val()+"-"+$("#startTime_jidu").val();
	var indexs = "2131,2125,2126,2127,2128,2129,2130";
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("<%=request.getContextPath()%>/index/getOneTagMutlCompanyOneTimeMutIndex",{"time":time,"tag":tag,"indexs":indexs}, function(result){
			
			if(result.success)
			{
				showResult(result);
			}
			
	  });
}

function showResult(result)
{
	var time = $("#startTime_year").val()+"-"+$("#startTime_jidu").val();
	var ssl = result.data;
	if(ssl==""||ssl==null)
		return;
	for(var k=0;k<ssl.length;k++)
	{
		var ss = ssl[k];
		var cname = ss.name;
		var sd = ss.data;
		var rowHtml = "";
		var scompanycode ;
		for(var i=0;i<sd.length;i++)
		{
			var point = sd[i];
			var companycode = point.companycode;
			scompanycode = companycode;
			var indexcode = point.xindexcode;
			rowHtml +="<td style='cursor:pointer' onclick=\"queryscore(this,'"+indexcode+"','"+companycode+"','"+time+"')\">"+point.v+"</td>";
		}
		rowHtml = "<tr class='alt1'><td><a href='#' onclick=\"openUrl(0,'"+cname+"','"+scompanycode+"')\">"+cname+"</a>|<a href='#' onclick=\"openUrl(1,'"+cname+"','"+scompanycode+"')\">TX</a></td>"+rowHtml;
		rowHtml +="</tr>";
		$("#Index_tbody_dataTable").append(rowHtml);
	}
	
}
function openUrl(type,companyname,companycode)
{
	if(type=='0')
	{
			var scompanycode = companycode.split(".")[0];
			window.open("http://192.168.1.3:8888/stock/company_main.html?companycode="+companycode+"&companyname="+escape(companyname));
	}
	if(type=='1')
	{
			var scompanycode = companycode.split(".")[0];
			window.open("http://stockhtm.finance.qq.com/sstock/ggcx/"+scompanycode+".shtml");
	}
}
var zscatterchart = function(id,xtitle,ytitle,showLegend)
{
	var cwidth = $("#"+id).width()-5;
	return new Highcharts.Chart({
	    chart: {
	        renderTo: id,
			zoomType: 'xy',
			width:cwidth
	    },
	    xAxis: {
	    	title: {
	            text: xtitle
	         }
	    },
	    yAxis: {
		     title: {
		        text: ytitle
		     }
	    },
		legend : {
				enabled:showLegend,
				align : 'center',
				maxHeight:40,
				verticalAlign : 'bottom',
				backgroundColor : '#FFFFFF',
				itemStyle: {
                 fontSize: '12px'
				}
		},
		tooltip: {
            formatter: function() {
        	    return '<b>'+this.point.name+':['+ this.x +'</b> , <b>'+ this.y +']</b>';

         }
        },
		plotOptions: {
            series: {
                cursor: 'pointer',
                point: {
                    events: {
                        click: function() {
							var companycode = this.id;
							var companyname = this.name;
							window.open("http://192.168.1.3:8888/stock/company_main.html?companycode="+this.id+"&companyname="+escape(this.name));
                        }
                    }
                }
            }
        },
	    title: {
	        text: "指标竞争力分析图"
	    },
	    series: []
	});
}
function createScatterChart_common(id,tag,company,xindex,yindex,time,xtitle,ytitle,showLegend,markcompanycode)
{
	try{
	var cname=""  ;
	var ccode=""  ;
	if(company!=null&&company!="")
	{
		 cname = company.split(":")[0];
		 ccode = company.split(":")[1];
	}
	var xindexcode=xindex.split(":")[1];
	var	yindexcode=yindex.split(":")[1];
	var data={
		tag:tag,
		companycode:ccode,
		xindexcode:xindexcode,
		yindexcode:yindexcode,
		time:time 
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/scatter/getScatterIndexData?"+$.param(data), function(result){
		  var rm = scret;
		  if(rm==null||rm.vl==null||rm.vl=="")
			{
			  return;
			}
		var dl = rm.vl.split("~");
		var pointdata = new Array();
		var xavg =rm.xavg;
		var yavg =rm.xavg;
		var chart = new zscatterchart(id,xtitle,ytitle,showLegend);
		cur_chart = chart;
		for(var i=0;i<dl.length;i++)
		{
				//分隔指标编码与名字
				var cd = dl[i].split("^");
				if(cd=="") continue;
				var companyname = cd[0];
				var companycode = cd[1];
				var x = cd[2];
				var y = cd[3];
				x=parseFloat(x).toFixed(2);
				y=parseFloat(y).toFixed(2) ;
			pointdata.push({
					id:companycode,
					name:companyname,
					x: parseFloat(x),
					y: parseFloat(y),
					id:companycode
			});	
		}

	chart.addSeries({
		type: 'scatter',
		name: "同行公司",
        data: pointdata,
    	turboThreshold:3000
			});
	  });	

	 }catch(e)
	 {
		console.log(e);
	 }
}
function showScatterChart()
{
	var tag = $("#tagcode").val();
	var xindexname = $("#xindexs").find("option:selected").text();
	var yindexname = $("#yindexs").find("option:selected").text();
	var xindex = xindexname+":"+$("#xindexs").val();
	var yindex = yindexname+":"+$("#yindexs").val();
	var time = $("#startTime_year").val()+"-"+$("#startTime_jidu").val();
	createScatterChart_common("chartDiv",tag,"",xindex,yindex,time,xindexname,yindexname,true,"");
	
	initCompanysOfTag("markcompany",tag);
}
</script>
</head>
<body>
<div style="text-align:left;width:90%">	
	<div >
	==========================================各能力指标计算===============================================<br/>
	注意：选择一个没有子行业的行业标签<br/>
		盈富一级行业:<select name="industry_0" id="industry_0" >
			<option value='-1'>请选择--</option>
		</select>
		盈富二级行业:<select name="industry_1" id="industry_1" >
			<option value='-1'>请选择--</option>
		</select>
		盈富三级行业:<select name="industry_2" id="industry_2" >
			<option value='-1'>请选择--</option>
		</select>
		盈富四级行业:<select name="industry_3" id="industry_3" >
			<option>请选择--</option>
		</select>
		<br/>
	类型(0：各类子类能力或1：综合能力)：<input type="text" id="type" name="type" value="0"/>
	是否重新加载数据：<input type="text" id="loaddata" name="loaddata" value="0"/><br/>
	<input type="button" id="computecapSection" name="computecapSection" value="计算指定时间段各公司的能力指标值" />
	开始时间：<input type="text" id="sTime" name="sTime" value="2010-09-30"/>
	结束时间：<input type="text" id="eTime" name="eTime" value="2012-09-30"/><br/>
	
	<input type="button" id="computeCapOneTime" name="computeCapOneTime" value="计算某一时间点的各公司的能力指标值" />
	时间：<input type="text" id="time" name="time" value="2012-09-30"/><br/>
	
<input type="button" id="computeCapOneTimeOneTag" name="computeCapOneTimeOneTag" value="计算某一时间点某一行业的各公司的能力指标值" />
	时间：<input type="text" id="ttime" name="ttime" value="2012-09-30"/>
	测试标签：<input type="text" id="tag" name="tag"  />
	<!--
	<select id="tag">
					<option value="合成材料制造业" >合成材料制造业</option>
					<option value="日用电器制造业" selected>日用电器制造业</option>
					<option value="太阳能">太阳能</option>
				</select>
				-->
	指标：<select id="testIndex">
					<option value="-1" selected>--请选择--</option>
					<option value="2125" >偿债能力指标</option>
					<option value="2126" >盈利质量指标</option>
					<option value="2127" >盈利能力指标</option>
					<option value="2128" >运营能力指标</option>
					<option value="2129" >成长能力指标</option>
					<option value="2130" >管控能力指标</option>
					<option value="2131" >综合能力指标</option>
				</select>
				<br/>
	
			<div>
				标签：	<input type="text" id="tagcode" name="tagcode"  />
				<!--<select id="tagcode">
					<option value="合成材料制造业" >合成材料制造业</option>
					<option value="日用电器制造业" selected>日用电器制造业</option>
					<option value="太阳能" >太阳能</option>
					
				</select>-->
				起始时间:<select name="startTime_year" id="startTime_year" >	
				</select><select name="startTime_jidu" id="startTime_jidu" >	
				<option value='3-30' >一季度</option>
				<option value='6-30' >二季度</option>
				<option value='9-30' selected>三季度</option>
				<option value='12-30' >四季度</option>
				</select>
				<input type="button" id="queryButton" name="queryButton" value="查询" onclick="query()"/>
				
			</div>
		</div>
		<div id="div_Index_table" >
					 <table class="datalist2">
						<thead id="Index_thead_dataTable">
						<tr>
							<th>公司名</th>
							<th style='cursor:pointer'>综合能力指标</th>
							<th style='cursor:pointer'>偿债能力指标</th>
							<th style='cursor:pointer'>盈利质量指标</th>
							<th style='cursor:pointer'>盈利能力指标</th>							
							<th style='cursor:pointer'>运营能力指标</th>
							<th style='cursor:pointer'>成长能力指标</th>
							<th style='cursor:pointer'>管控能力指标</th>
						</tr>
						</thead>
						<tbody id="Index_tbody_dataTable"></tbody>
					 </table>
		</div>
		<div id="scorelog" style="dipslay:none"></div>
</div>
<div id="chartShowDiv">
<div>
x:<select id="xindexs">
					<option value="-1" selected>--请选择--</option>
					<option value="2125" >偿债能力指标</option>
					<option value="2126" >盈利质量指标</option>
					<option value="2127" >盈利能力指标</option>
					<option value="2128" >运营能力指标</option>
					<option value="2129" >成长能力指标</option>
					<option value="2130" >管控能力指标</option>
					<option value="2131" >综合能力指标</option>
</select>
y:<select id="yindexs">
					<option value="-1" selected>--请选择--</option>
					<option value="2125" >偿债能力指标</option>
					<option value="2126" >盈利质量指标</option>
					<option value="2127" >盈利能力指标</option>
					<option value="2128" >运营能力指标</option>
					<option value="2129" >成长能力指标</option>
					<option value="2130" >管控能力指标</option>
					<option value="2131" >综合能力指标</option>
</select>
<input type="button" id="showchart" name="showchart" value="图表展示" />标记：	<input type="text" id="markcompany" name="markcompany"  placeholder="请输入公司编码/拼音"/>
</div>
<div id="chartDiv" style="width:90%">

</div>
</div>

</body>
</html>

