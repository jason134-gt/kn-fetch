<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>


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
var currenttag = "-1" ;
var currenttagcode = "-1" ;
var chart ;
var xtitle = "x";
var ytitle = "y";
var categorys = new Array();
var cindexname ;
var tchart;
$(document).ready(function() {

	initYear("time_year",18);
	initIndustryAutoMatcher("ttag");
	 initAutoIndexMatcher('tindex');
	 initAutoIndexMatcher('xindex');
	
	initYear("startTime_year",14);
	initYear("endTime_year",18);
	
	initIndustry("root",-1);
	chart = new ndchart("NormalDistributionChart_0");
	tchart = new timechart("numTrendsChart");
	
	
	$("#tags_div").mouseover(function(){
		$('ul',$("#tags_div")).css("visibility","");
	});
	
	
	$("#addIndex").click(function(){
		var time = $("#time_year").val()+"-"+$("#time_jidu").val();
		if($("#xindex").val()==null||$("#xindex").val()=="")
			{
				alert("请输入指标！");
				$("#xindex").focus();
				return;
			}
		var indexcode = $("#xindex").val().split(":")[1];
		doAddIndex2Chart(time,indexcode);
	});
});//end ready

function initIndustry(uid,prefix)
{

	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/industry/getYfzxAllIndustryJsonData", function(result){
		  var si = yfzxjd;
		  if(si==null||si=="")
			  return ;
		  indData = si;
		  doInitIndustry("root",indData,0);	  
	  });	
}
function doInitIndustry(uid,si,type)
{
	var name =  si.n;
	var pid = name.split(":")[1];
	var pindustryName = name.split(":")[0];
	var liid = pid+"_li_"+type;
	var ctag = "";
	if(pid=="-1") 
	{
		liid = "root_li_"+type;
		ctag = "所有行业";
	}
	$("#"+uid+"").append("<li id='"+liid+"' onclick=\"appendTags(event,'"+pindustryName+"');return false;\"><span style=\"width:100%\" >"+pindustryName+"</span></li>");
	var child = si.c;
	if(child!=null&&child.length>0)
	{
		var subid = pid+"_sub_"+type;
		if(pid=="-1") 
		 subid = "root_sub_"+type;
		$("#"+liid+"").append("<ul class='sub_menu' id='"+subid+"' style='margin-left:-15'></ul>");
		
		for(var i=0;i<child.length;i++)
		{
			var csi = child[i];
			doInitIndustry(subid,csi,type)	
		}	
	}
}
function stopBubble(e) {  
    var e = e ? e : window.Event;  
    if (window.Event) { // IE  
        e.cancelBubble = true;   
    } else { // FF  
        //e.preventBubble();   
        e.stopPropagation();   
    }   
} 
function appendTags(event,industryName)
{
	stopBubble(event);
	$('ul',$("#root")).css("visibility","hidden");
	
	currenttag = industryName;
	$("#ctag").text(industryName);
}

function doAddIndex2Chart(time,indexcode)
{
				var xtext = $("#xindex").val().split(":")[1];
				cindexname = xtext.split(":")[0];
				getNormalDistributeData(time,indexcode);

}

function initIndustryAutoMatcher(aa)
{
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.get("<%=request.getContextPath()%>/industry/getAllIndustry", function(result){
				if(result==null)
				{
					return;
				}
				var data = result.split(";");
				//data.push("所有行业:-1")
				if(data!=null)
				{
					c_data = data;
					$("#"+aa+"").autocomplete(c_data,{
					max: 12, //列表里的条目数
					minChars: 0, //自动完成激活之前填入的最小字符
					width: 150, //提示的宽度，溢出隐藏
					scrollHeight: 300, //提示的高度，溢出显示滚动条
					matchContains: true, //包含匹配，就是data参数里的数据，是否只要包含文本框里的数据就显示
					autoFill: false //自动填充
					});
				}
				
		  });
	
	
	
}
var ndchart = function(id)
{
	return  new Highcharts.Chart({
        chart: {
            renderTo: id,
            type: 'spline'
        },
		xAxis: {
			tickmarkPlacement: 'between'
        },
		title: {
	        text: '公司数量分布图'
	    },
        yAxis:{
			labels: {
					enabled: true
				},
			title:{
					text:'公司数量'
			}
		},
		tooltip: {
            formatter: function() {
                return '<b>指标值区间:'+this.point.name+',公司数量:'+ this.point.y +'</b>';
            }
        },
        series: []
    });
}
var bindex = 0;
function getNormalDistributeData(time,indexcode)
{
	var inum = $("#inum").val();
	$("#ctag").empty();
	var tagname = currenttag;
	if(currenttag=="-1") tagname = "所有行业";
	$("#ctag").append(tagname);
	var rlow = $("#frlow").val();
	var rhigh = $("#frhigh").val();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/reference/getNormalDistributeData",{"time":time,"tag":currenttag,"indexcode":indexcode,"inum":inum,"rlow":rlow,"rhigh":rhigh}, function(result){
		  var rd = result.data;
		  if(rd==null)
			{
			  alert("没有相关公司！");
			
			  return;
			}
			//生成正态分布图
			createNormalDistributionChart(rd);
			
	  });	
}
function createNormalDistributionChart(sdata)
{
	if(sdata==null)
	{
			return ;
	}

	var data = sdata.data;
	var avg = sdata.total;
	//var bid = "NormalDistributionChart_"+bindex;
	//$("#chartregion").append("<div id='"+bid+"'></div>");
	var i =0;
	var sumnum = 0;
	var pointdata = new Array();
	for(i in data)
	{
	  		//分隔指标编码与名字
	  		var point = data[i];
			pointdata.push({
				name:point.xname,
				x:point.x,
				y:point.y
			});	
			
			sumnum+=point.y;
			
	}
	//if(chart==null)
		//chart = new ndchart(bid);
	var time = $("#time_year").val()+"-"+$("#time_jidu").val();
	var indexname = $("#xindex").val().split(":")[0];
	chart.addSeries({
					name:currenttag+":"+indexname+"-分布("+time+")",
					data:pointdata,
						});
	addLine(chart,avg);
	
	$("#avg").empty();
	$("#avg").append(avg);
	
	$("#sumnum").empty();
	$("#sumnum").append(sumnum);
	
	var dinum = sumnum/30;
	if(dinum<15) dinum = 15;
	$("#dinum").empty();
	$("#dinum").append(dinum.toFixed(0));
	
	if($("#multchart").attr("checked")=="checked")
		bindex+=1;	
	
}

function addLine(chart,v)
{
			var color = chart.series[chart.series.length-1].color;

			chart.xAxis[0].addPlotLine({
						color: color,
						width: 2,
						value: v,
						label: {
							text:  'avg:'+v,
							verticalAlign: 'bottom',
							textAlign: 'right',
							y: 10
						}
						});
			
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
	 	if(sdata==null)
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
				
				$("#"+id+"").append("<option value='"+utext+"' >"+utext+"</option>");
			}
			$("#"+id+"").change();
		}

}

function save()
{
	var rlow = $("#s_rlow").val();
	var rhigh = $("#s_rhigh").val();
	var indexcode = $("#xindex").val().split(":")[1];
	var desc = $("#desc").val();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/reference/setIndexReferenceValue",{"tag":currenttagcode,"indexcode":indexcode,"rlow":rlow,"rhigh":rhigh,"desc":desc}, function(result){
			
			alert("操作结束");
			
	  });
}
function batchsave()
{
	var indexarray = document.getElementsByName("reference");
	var ups = new Array() ;
	for(var i=0;i<indexarray.length;i++)
		{
			var indexid = indexarray[i].id;
			var v = indexarray[i].value;
			var indexcode = indexid.split("_")[0];
			var industrycode = indexid.split("_")[1];
			var type = indexid.split("_")[2];
			if(type==1) continue;
			var rlow = $("#"+indexcode+"_"+industrycode+"_0").val();
			var rhigh = $("#"+indexcode+"_"+industrycode+"_1").val();
			var desc = $("#"+indexcode+"_"+industrycode+"_desc").val();
			ups+="industrycode="+industrycode+"|indexcode="+indexcode+"|rlow="+rlow+"|rhigh="+rhigh+"|desc="+desc;
			ups+="||";
		}
		
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/reference/batchsetIndexReferenceValue",{"ups":ups}, function(result){
			
			alert("操作结束");
			
	  });
}
function query()
{
	$("#Index_tbody_dataTable").empty();
	var ttag = $("#ttag").val();
	var tindex = $("#tindex").val();
	if(tindex==null&&ttag==null)
		{
			alert("行业与指标不可以同时为空！");
			return ;
		}
	var tagname ="";
	var indexcode = "";
	if(tindex!=null&&tindex!="") indexcode = tindex.split(":")[1];
	if(ttag!=null&&ttag!="") tagname = ttag.split(":")[0];
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("<%=request.getContextPath()%>/reference/getIndexReferenceListByTagOrIndex",{"tagname":tagname,"indexcode":indexcode}, function(result){
			
			if(result.success)
			{
				showResult(result);
			}
			
	  });
}

function showResult(result)
{
	var data = result.data;
	if(data==""||data==null)
		return;
	var i;
	for(i=0;i<data.length;i++)
		{
			var cl = data[i];
			var rowHtml = "<tr class='alt1'>";
			rowHtml +="<td>"+cl.indexCode+"</td>";
			rowHtml +="<td>"+cl.industryCode+"</td>";
			rowHtml +="<td>"+cl.industryName+"</td>";
			if(cl.avg==null) cl.avg = "";
			rowHtml +="<td>"+cl.avg+"</td>";
			if(cl.rlow==null) cl.rlow = "";
			rowHtml +="<td><input type='text' name='reference' id='"+cl.indexCode+"_"+cl.industryCode+"_0' value='"+cl.rlow+"'/></td>";
			if(cl.rhigh==null) cl.rhigh = "";
			rowHtml +="<td><input type='text'  id='"+cl.indexCode+"_"+cl.industryCode+"_1' value='"+cl.rhigh+"'/></td>";
			if(cl.desc==null) cl.desc = "";
			rowHtml +="<td><textarea type='text'  id='"+cl.indexCode+"_"+cl.industryCode+"_desc'>"+cl.desc+"</textarea></td>";
			rowHtml +="</tr>";
			$("#Index_tbody_dataTable").append(rowHtml);
		}
}

function reset()
{
	chart.destroy();

	chart = new ndchart("NormalDistributionChart_0");
	
	
}
function resetTChart()
{
	tchart.destroy();
	tchart = new timechart("numTrendsChart");
}
var timechart = function(id,yaxis)
{
	return new Highcharts.Chart({
	    chart: {
	        renderTo: id
	    },
	     xAxis: {
    	  	type:"datetime",//时间轴要加上这个type，默认是linear
			//startOnTick: true,
    	    //自定义x刻度上显示的时间格式，根据间隔大小，以下面预设的小时/分钟/日的格式来显示
    	  dateTimeLabelFormats:
    	    {
				month: '%y-%m'
    	    }
      },
	  title: {
	        text: '公司数量走势图'
	    },
		yAxis: [{
					title:{
						text:'公司数量'
					},
					lineWidth: 1
				}, {
					title:
					{
					   text:"公司数量占总量的比"
					},
					lineWidth: 1,
					opposite: true,		
				}],
		
		tooltip: {
            formatter: function() {
                return '<b>公司数量:'+ this.y +'</b>';
            }
        },
		
	    series: []
	});
}

function showNumTrends()
{
	var indexcode = $("#xindex").val().split(":")[1];
	var rlow = $("#index_rlow").val();
	var rhigh = $("#index_rhigh").val();
	//超始时间
	var stime = $("#startTime_year").val()+"-"+$("#startTime_jidu").val();
	//结束时间
	var etime = $("#endTime_year").val()+"-"+$("#endTime_jidu").val();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/reference/getCompanyNumTrends",{"tag":currenttag,"indexcode":indexcode,"rlow":rlow,"rhigh":rhigh,"stime":stime,"etime":etime}, function(result){
		  var sdata = result.data;
		  if(sdata==null)
			{
			  alert("没有相关公司！");
			
			  return;
			}
			var tagname = currenttag;
			if(currenttag=="-1") tagname = "所有行业";
			var data = sdata.data;
			var sum = sdata.total;
			$("#tdesc").empty();
			var i =0;
			var pointdata = new Array();
			var zbp = new Array();
			for(i in data)
			{
					//分隔指标编码与名字
					var point = data[i];
					pointdata.push({
						x:point.time,
						y:point.y
					});	
					var zb = point.y/sum;
					zb = zb.toFixed(2);
					zbp.push({
						x:point.time,
						y:parseFloat(zb)
					});	
					
			}			
			
			var time = $("#time_year").val()+"-"+$("#time_jidu").val();
			var indexname = $("#xindex").val().split(":")[0];
			var rs = "";
			if(rlow!=null&&rlow!="")
				rs+=" l:"+rlow;
			if(rhigh!=null&&rhigh!="")
				rs+=" h:"+rhigh;
			tchart.addSeries({
							name:tagname+":"+indexname+rs+"-公司数量走势",
							data:pointdata,
							yAxis:0
								});
			tchart.addSeries({
							name:"占"+tagname+"上市公司比重走势",
							data:zbp,
							yAxis:1
								});
			$("#tdesc").append(tagname+":"+stime+"之前上市，到"+etime+"为止还为正常上市的公司总数为："+sum);			
			
	  });	
}
</script>
</head>
<body>
<div style="text-align:left;float:left">
	<div >
		
		时间:<select name="time_year" id="time_year" >	
		</select><select name="time_jidu" id="time_jidu" >	
		<option value='3-30' selected>一季度</option>
		<option value='6-30' >二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select><br/>
		<div id="tags_div" style="color:red">
						<ul id="root" class="dropdown" style="z-index:1010">
						</ul>
		</div>
	
		<div style="clear:left">
		x指标:<input type="text" name="xindex" id="xindex" />

		<br/>
		下限：<input type="text" name="index_rlow" id="frlow" value=""/><br/>
		上限：<input type="text" name="index_rhigh" id="frhigh" value=""/><br/>
		等分数:<input type="text" name="inum" id="inum" value="" />默认等分数:<span id="dinum" ></span><br/>
		
		均值:<span id="avg" style="color:red"></span>,总数:<span id="sumnum" style="color:red"></span>
		当前行业:<span id="ctag" style="color:red"></span><br/>
		<input type="button" name="addIndex" id="addIndex" value="展示" />
		<!--
		模式：<input type="checkbox" name="multchart" id="multchart"  onclick="reset()"/>多图
		-->
		<input type="button" name="reset" id="reset" value="重置" onclick="reset()"/><br/>
		</div>
		
	</div>
<div id="NormalDistributionChart_0" style="width:800px">	
</div>
<div>
		公司数量变化图：<br/>
		起始时间:<select name="startTime_year" id="startTime_year" >	
		</select><select name="startTime_jidu" id="startTime_jidu" >	
		<option value='3-30' >一季度</option>
		<option value='6-30' selected>二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select>
		结束时间:<select name="endTime_year" id="endTime_year" >	
		</select><select name="endTime_jidu" id="endTime_jidu" >	
		<option value='3-30' selected>一季度</option>
		<option value='6-30' >二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select><br/>
		下限：<input type="text" name="index_rlow" id="index_rlow" value=""/>---->
		上限：<input type="text" name="index_rhigh" id="index_rhigh" value=""/>
		<input type="button" name="showNumTrends" id="showNumTrends" value="展示" onclick="showNumTrends()"/>
		<input type="button" name="resettchart" id="resettchart" value="重置" onclick="resetTChart()"/>
		<br/><span id="tdesc" style="color:red"></span>
		<div id="numTrendsChart" style="width:800px">	
		</div>
</div>
<br/><br/><br/><br/>
设置参考值区：-------------------------------------------------------------------------<br/>
		下限：<input type="text" name="s_rlow" id="s_rlow" value=""/>---->
		上限：<input type="text" name="s_rhigh" id="s_rhigh" value=""/>
		说明：<textarea id="desc"></textarea>
		<input type="button" name="savelh" id="savelh" value="保存" onclick="save()"/>
<br/><br/><br/><br/>
-------------------------------------------------------------------------<br/>
<div>
				行业标签：<input type="text" name="ttag" id="ttag" value="" />
				指标编码：<input type="text" name="tindex" id="tindex" value="" />
				<input type="button" id="queryIndexWeightOfTagButton" name="queryIndexWeightOfTagButton" value="查询" onclick="query()"/>
				<input type="button" id="saveIndexsButton" name="saveIndexsButton" value="保存" onclick="batchsave()"/>

		</div>
		<div id="div_Index_table" style="overflow:auto;height:auto">
					 <table class="datalist2">
						<thead id="Index_thead_dataTable">
						<tr>
							<th>指标编码</th>
							<th>行业编码</th>
							<th>行业名</th>
							<th>均值</th>
							<th>下限</th>
							<th>上限</th>
							<th>说明</th>
						</tr>
						</thead>
						<tbody id="Index_tbody_dataTable"></tbody>
					 </table>
		</div>
 </div>
</body>
</html>

