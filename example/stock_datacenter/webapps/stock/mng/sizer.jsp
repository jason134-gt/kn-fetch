<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@page import= "java.util.*"   %> 
<%
	/* 唐斌奇 增加 
	Enumeration e =(Enumeration)request.getParameterNames();
	while(e.hasMoreElements()){   
		String parName=(String)e.nextElement();
		System.out.println(parName);   
	}*/
	
	String tag = request.getParameter("tag");
	String time = request.getParameter("time");
	String xindexcode = request.getParameter("xindexcode");
	String yindexcode = request.getParameter("yindexcode");
	if(tag==null) tag = "";
	if(time==null) time = "";
	if(xindexcode == null ) xindexcode="";
	if(yindexcode == null ) yindexcode="";
	//唐斌奇增加
	String ctag = request.getParameter("ctag");
	String sizertype = request.getParameter("sizertype");//公司 or 行业
	String showtype = request.getParameter("showtype");//显示类型 象限选股 反函数选股 多指标选股
	String q = request.getParameter("q");
	String quadrant = request.getParameter("quadrant");//第几象限
	String companys = request.getParameter("companys");
	
	//唐斌奇增加
	if(ctag == null) ctag = "";
	if(q==null) q = "";
	List<String> indexArr = new ArrayList<String>();
	Map<String,String> kv = new HashMap<String,String>();
	String[] aArr = q.split(",");//"2300_left|,2300_right,3112_left|,3112_right"
	for(String a : aArr){
		if(a.length() <= 1 )continue;
		String[] kvT = a.split("\\|");
		if(kvT.length == 1)	kv.put(kvT[0],"");
		else kv.put(kvT[0], kvT[1]);
		String indexCode = a.split("\\|")[0].replace("_left","").replace("_right","");
		if(!indexArr.contains(indexCode))indexArr.add(indexCode);
	}
	/*
	if(indexArr.size() == 2 && "0".equals(showtype)){
		xindexcode = indexArr.get(0);
		yindexcode = indexArr.get(1);
	}
	*/
%>
<html>
<head>
 <title>股筛</title>

 <link href = "css/base.css"  type = "text/css" rel="stylesheet"></link>
 <link href = "css/jqpagination.css"  type = "text/css" rel="stylesheet"></link>
<script src="js/jquery/jquery-1.6.2.js" type="text/javascript"></script>
<script src="js/highcharts/js/highcharts.src.js" type="text/javascript"></script>
<script type='text/javascript' src='js/jquery-autocomplete/jquery.autocomplete.js'></script>
<script type='text/javascript' src='js/mycharts.js'></script>
<script type='text/javascript' src='js/indexAnalysis.js'></script>
<script type='text/javascript' src='js/common.js'></script>
<script type='text/javascript' src='js/base.js'></script>
<script type='text/javascript' src='js/stockExt/jquery.jqpagination.js'></script>
<script type="text/javascript" src="js/sparkline/jquery.sparkline.js"></script>
 <link rel="stylesheet" href="css/style.css" type="text/css" media="screen, projection"/>
   <script type="text/javascript" language="javascript" src="js/jquery.dropdownPlain.js"></script>
<link rel="stylesheet" type="text/css" href="js/jquery-autocomplete/jquery.autocomplete.css" />
<script type="text/javascript">
function saveSizerCond(){
	var name = $("#sizerName").val();
	if(name.trim().length ==0){
		alert("请输入名称!");
		return;
	}
	var showtype = $('input[name="stype"]:checked').val();//radio选项
	var q="";
	if(showtype == "0"){//象限选股
		var x = $("#xindex").val();
		var xl = thousandToNumberFormat($("#xindex_minc").val());
		var xr = thousandToNumberFormat($("#xindex_maxc").val());
		
		var y = $("#yindex").val();
		var yl = thousandToNumberFormat($("#yindex_minc").val());
		var yr = thousandToNumberFormat($("#yindex_maxc").val());
		if(x == "" || y == ""){alert("请选择指标");return;}
		q = x.split(":")[1]+"_left|"+xl+","+x.split(":")[1]+"_right|"+xr+","+y.split(":")[1]+"_left|"+yl+","+y.split(":")[1]+"_right|"+yr;//还可以将以前条件存储下来
	}else if(showtype == "1"){//盈富 反函数选股
		
		var xl = thousandToNumberFormat($("#2_xindex_minc").val());
		var xr = thousandToNumberFormat($("#2_xindex_maxc").val());
		var yl = thousandToNumberFormat($("#2_yindex_minc").val());
		var yr = thousandToNumberFormat($("#2_yindex_maxc").val());
		q = "2300_left|"+xl+","+"2300_right|"+xr+","+ "3112_left|"+yl+","+"3112_right|"+yr;
	}else if(showtype == "2"){//多指标选股
		var q = $("form").serialize().replace(/%2C/ig,'');
		q = q.replace(/=/g,"|").replace(/&/g,",");
	}
	var ctag = $("#ctag").val();
	var companys = preSelectResult;
	var companys = companys.split(";").join(",");
	var sizertype = $('input[name="stockOrIndustry"]:checked').val();
	var time = $("#time_year").val()+"-"+$("#time_jidu").val();
	var obj = {
		"name":name,
		"time":time,
		"ctag":ctag,//不自动切换股筛的条件
		"showtype":showtype,//显示类型 1=象限选股,2=盈富选股[反函数],3=多指标选股
		"sizertype":sizertype,//0=筛选公司,1=筛选行业,默认0
		"q":q,
		"companys":companys,
		"quadrant":4
	}
	var objStr = JSON.stringify(obj);
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("/stock/user/mystock/save", {'index':objStr}, function(result){
		console.info(result);
	});
}
function sizerCondScreen() {
	var q="<%=q%>";
	var yq = "<%=time%>";
	var ctag = "<%=ctag%>"
	var s = "";
	var d = "false";
	var pindex="0";
	var totalcount = "-1";
	var url = "<%=request.getContextPath()%>/indextable/getIndexTableData?q="+q+"&pindex="+pindex+"&desc="+d+"&yearQuarter="+yq+"&totalcount="+totalcount+"&sortas="+s+"&tag="+ctag+"&pagesize="+pagesize;
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript(url,function(result){
		if(presult==null) 
		{
			showOrHideLoading(false);
			chart.hideLoading();
			return ;
		}
		showDataResult(presult);
	});
}
//

var rowsList = "",sortRName="";//查询的结果，后面做分页使用
var jp;
var currenttag = "-1" ;
var chart ;
var xtitle = "x";
var ytitle = "y";
var xavg ;
var yavg;
var cScatterData;
var preSelectResult ="";
var xmax,xmin,xavg,xmid;
var ymax,ymin,yavg,ymid;
var indexRuleData;
var cmmmatag ;//当前的MMMA使用的tag
$(document).ready(function() {

	initYear("time_year",18);
	initIndustry("root",-1);
	chart = new scatterchart("chartdiv");
	
	//对页面请求参数进行处理
	var rtag = "<%=tag%>";
	if(rtag!="") currenttag = rtag;
	var rtime = "<%=time%>";
	if(rtime!="") 
		{
		 	$("#time_year").val(rtime.substring(0,4));
		 	$("#time_jidu").val(rtime.substring(5,rtime.length));
		}

	//如果是从行业页面过来的
	if(rtag!="")
	{
		var rxindexcode = "<%=xindexcode%>";
		var ryindexcode = "<%=yindexcode%>";
		var type = $('input[name="stype"]:checked').val();
		$("#selectTip").attr("checked",false);
		doselectStock(type,rtime,rxindexcode,ryindexcode);
		$("#selectTip").attr("checked",true);
	}
	//$("#industry_0").append("<option value='-1' selected>请选择行业</option>");
	$("#industry_0").change(function (){
		//$("#industry_1").empty();
		var industrycode = $("#industry_0").val().split(":")[1];
		var rd = getIndustryBycode("industry_1","z",industrycode);
		//currenttag = $("#industry_0").val().split(":")[0];
		
	});
	refreshIndexSelect("xindextags","xindexselect");
	refreshIndexSelect("yindextags","yindexselect");
	refreshIndexSelect("mutlindextags","multindexselect");
	$("#xindextags").change(function (){
		refreshIndexSelect("xindextags","xindexselect");
		
	});
	$("#yindextags").change(function (){
		refreshIndexSelect("yindextags","yindexselect");
		
	});
	$("#mutlindextags").change(function (){
		refreshIndexSelect("mutlindextags","multindexselect");
		
	});
	$("#xindexselect").change(function (){
		var xtext = $("#xindexselect").find("option:selected").text();
		$("#xindex").val(xtext);
		
	});
	
	$("#ctag").click(function (){
		$("#ctag").val("");
		
	});
	
	$("#yindexselect").change(function (){
		var ytext = $("#yindexselect").find("option:selected").text();
		$("#yindex").val(ytext);
		
	});
	$("#multindexselect").change(function (){
		var ytext = $("#multindexselect").find("option:selected").text();
		$("#addExtInput").val(ytext);
		
	});
	
	$("#industry_1").change(function (){
		//$("#industry_2").empty();
		var industrycode = $("#industry_1").val().split(":")[1];
		var rd = getIndustryBycode("industry_2","z",industrycode);
		//currenttag = $("#industry_1").val().split(":")[0];
	});
	$("#industry_2").change(function (){
		var industrycode = $("#industry_2").val().split(":")[1];
		var rd = getIndustryBycode("industry_3","z",industrycode);
	});
	
	$(document).bind("click", function (e) { 
			if(e.target.id==null||e.target.id=="") return;
			var stockOrIndustry = $("#conditiondiv").find('input[name="stockOrIndustry"]:checked').val();
			if(e.target.id.indexOf("industry")<0&&currenttag!=cmmmatag&&stockOrIndustry!="1")
			{
				initMMMA();
				if(selectStockType==2)
				{
					showLoading(1500);
					var sindexrow = document.getElementsByName("row_MarketCap");
						for(var i=0;i<sindexrow.length;i++)
						{
							var indexcode = sindexrow[i].id
							initMulIndexMMMA(indexcode);
						}
				}	
				cmmmatag = currenttag;
			}
			
			
		});
	$("#cpage").change(function(){
		//showLoading(3000);
		if(selectStockType==2)
		{
			showOrHideLoading(true);
			var pindex = $("#cpage").val();
			var tcount = $("#tdcount").text();
			pindex = pindex-1;
			ajaxSubmitScreen("",false,pindex,tcount);
		}
		else
		{
			showOrHideLoading(true);
			showImageChartTable();
		}
		
	});
	$("#xindex").change(function (){
			//currenttag = $("#industry_1").val().split(":")[0];
			var stockOrIndustry = $("#conditiondiv").find('input[name="stockOrIndustry"]:checked').val();
			if(stockOrIndustry != '1')
				initMMMA("xindex");
			var xtext = $("#xindex").val();
			xtext = xtext.split(":")[0];
			$("#xtreemapname").val(xtext+"规模");
			var indexcode = xtext.split(":")[1];
			$("#xtreemapcode").val(indexcode);
		});
	$("#yindex").change(function (){
		//currenttag = $("#industry_1").val().split(":")[0];
		var stockOrIndustry = $("#conditiondiv").find('input[name="stockOrIndustry"]:checked').val();
		if(stockOrIndustry != '1')
			initMMMA("yindex");
		var ytext = $("#yindex").val();
		ytext = ytext.split(":")[0];

		$("#ytreemapname").val(ytext+"规模");
		var indexcode = ytext.split(":")[1];
		$("#ytreemapcode").val(indexcode);

	});
	
	$("#industry_0").click(function (){
		currenttag = $("#industry_0").val().split(":")[0];
		initctagSpan(currenttag);
	});
	$("#tags_div").mouseover(function(e){
					$('ul',$("#tags_div")).css("visibility","");
	});
	$("#industry_1").click(function (){
		var s = $("#industry_1").val();
		if(s==null||s=="") 
		{
			var p = $("#industry_0").val();
			if(p!=null||p!="")
			{
				alert("请先选择上一级行业！");
				return;
			}
		}
		
		currenttag = $("#industry_1").val().split(":")[0];
		initctagSpan(currenttag);
	});
	$("#industry_2").click(function (){
		var s = $("#industry_2").val();
		if(s==null||s=="") 
		{
			var p = $("#industry_1").val();
			if(p==null||p=="")
			{
				alert("请先选择上一级行业！");
				return;
			}
		}
		currenttag = $("#industry_2").val().split(":")[0];
		initctagSpan(currenttag);
	});

	$("#industry_3").click(function (){
		var s = $("#industry_3").val();
		if(s==null||s=="") 
		{
			var p = $("#industry_2").val();
			if(p==null||p=="")
			{
				alert("请先选择上一级行业！");
				return;
			}
		}
		currenttag = $("#industry_3").val().split(":")[0];
		initctagSpan(currenttag);
	});
	
	
	$("#doselect").click(function(){
		
		var type = $("#conditiondiv").find('input[name="stype"]:checked').val();//选择0和1时
		var time = $("#time_year").val()+"-"+$("#time_jidu").val();
		var xindexcode = $("#xindex").val().split(":")[1];
		var yindexcode = $("#yindex").val().split(":")[1];
		doselectStock(type,time,xindexcode,yindexcode);
	});
	
	initAutoIndexMatcher('addExtInput');
	initAutoIndexMatcher('xindex');
	initAutoIndexMatcher('yindex');
	//getIndexRuleData();
	initAllTagsAutoMatcher("ctag");
	numberInputBind($(".numberInput"));
	
	<%	//唐斌奇 增加
		out.println("$('#ctag').val('"+ctag+"')");
		if("0".equals(showtype)){
			out.println("doselectStock('"+showtype+"','"+time+"','"+indexArr.get(0)+"','"+indexArr.get(1)+"');");
		}else if("1".equals(showtype)){
			%>
			$("input[name='stype']").get(1).checked=true;
			changeType(1);
			<%
			out.println("doselectStock('"+showtype+"','"+time+"','"+indexArr.get(0)+"','"+indexArr.get(1)+"');");
		}else if("2".equals(showtype)){
			%>
			$("input[name='stype']").get(2).checked=true; 
			changeType(2);
			sizerCondScreen();
			<%
		}
		
	%>
});//end ready
function refreshIndexSelect(cid,sid)
{
	//取数据源编码
	var tsCode = "ts_00003";
	var tag = $("#"+cid).val();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getJSON("<%=request.getContextPath()%>/cfirule/getDictionaryByTag?tableSystemCode="+tsCode+"&tag="+tag, function(result){
				  var rd = result.data;
					//初始化指标选择面板
					initIndexSelectPanel(sid,rd);
				
			  });
}
function changeSelectType(stockOrIndustry)
{
		if(stockOrIndustry=="1")
			{
				$("#xrvalue").slideUp();
				$("#yrvalue").slideUp();
				$("#rtableDiv").slideUp();
			}
			else
			{
				$("#xrvalue").slideDown();
				$("#yrvalue").slideDown();
				$("#rtableDiv").slideDown();
			}
			resetbutton();
}
function initctagSpan(currenttag)
{
	//$("#ctag").empty();
	if(currenttag=="-1")
		$("#ctag").val("所有行业");
	else
		$("#ctag").val(currenttag);
}
function doselectStock(type,time,xindexcode,yindexcode)
{
	var type ="0";
	currenttag = $("#ctag").val();
	if(type == "0"){
		var xtext = $("#xindex").val();
		var ytext = $("#yindex").val();
		var xminv = thousandToNumberFormat($("#xindex_minc").val());
		var xmaxv = thousandToNumberFormat($("#xindex_maxc").val());
		var yminv = thousandToNumberFormat($("#yindex_minc").val());
		var ymaxv = thousandToNumberFormat($("#yindex_maxc").val());
		if($("#selectTip").attr("checked")=='checked')
		{
			var scurrenttag = currenttag;
			if(currenttag==-1)
				 scurrenttag = "所有行业";
			if(!confirm("您此次的选股条件是：行业："+scurrenttag+",x指标："+xtext+"("+xminv+","+xmaxv+"),y指标："+ytext+"("+yminv+","+ymaxv+")")) return;
		}
		showOrHideLoading(true);
		chart.showLoading();
		var stockOrIndustry = $("#conditiondiv").find('input[name="stockOrIndustry"]:checked').val();
		if(stockOrIndustry == '1'){
			getIndustryScatter(type,time,xindexcode,yindexcode,xminv,xmaxv,yminv,ymaxv);
		}else{
			getXXScatter(type,time,xindexcode,yindexcode,xminv,xmaxv,yminv,ymaxv);
		}
		
	}else if(type == "1"){
		var xtext = "权益乘数";
		var ytext = "ROE";
		var xminv = thousandToNumberFormat($("#2_xindex_minc").val());
		var xmaxv = thousandToNumberFormat($("#2_xindex_maxc").val());
		var yminv = thousandToNumberFormat($("#2_yindex_minc").val());
		var ymaxv = thousandToNumberFormat($("#2_yindex_maxc").val());
		if($("#selectTip").attr("checked")=='checked')
		{
			var scurrenttag = currenttag;
			if(currenttag==-1)
				 scurrenttag = "所有行业";
			if(!confirm("您此次的选股条件是：行业："+scurrenttag+",x指标："+xtext+"("+xminv+","+xmaxv+"),y指标："+ytext+"("+yminv+","+ymaxv+")")) return;
		}
		showOrHideLoading(true);
		chart.showLoading();
		getFHSScatter(type,time,xindexcode,yindexcode,xminv,xmaxv,yminv,ymaxv);
	}else if(type == "2"){
		showOrHideLoading(true);
		csortArr = new Array();
		ajaxSubmitScreen("",false,0,-1);
	}

}
var scatterchart = function(id)
{
	return new Highcharts.Chart({
	    chart: {
	        renderTo: id,
			zoomType: 'xy'
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
	    credits: {
        	enabled : true,
        	href : "http://www.yfzx.com",
        	text : "盈富在线",
        	style: {
        		cursor: 'pointer',
                color: '#000000',
				fontSize: '15px'
			}
     	},
		tooltip: {
            formatter: function() {
                return '<b>'+this.point.name+':['+ this.x +
                    '</b> , <b>'+ this.y +']</b>';
            }
        },
		plotOptions: {
            series: {
                cursor: 'pointer',
                point: {
                    events: {
                        click: function() {
								//是公司还是行业
								if(this.id.split("\.").length==2)
								{
									window.open("/stock/html/stocks_analysis.html?stockcode="+this.id+"&stockname="+escape(this.name));
								}
								else
								{
									window.open("/stock/html/industry_analysis.html?tagcode="+this.id+"&tagname="+escape(this.name));
								}
                        }
                    }
                }
            }
        },
	    title: {
	        text: '指标选股图'
	    },
	    series: []
	});
}
function getIndustryScatter(type,time,xindexcode,yindexcode,xminv,xmaxv,yminv,ymaxv)
{
	var time = $("#time_year").val()+"-"+$("#time_jidu").val();
	var showAllLeafchild = $("#showAllLeafchild").val();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/scatter/getscatter_h",{"tag":currenttag,"time":time,"type":1,"xindexcode":xindexcode,"yindexcode":yindexcode,"xminv":xminv,"xmaxv":xmaxv,"yminv":yminv,"ymaxv":ymaxv,"showAllLeafchild":showAllLeafchild}, function(result){
		  var rd = result.data;
		  if(rd==null)
			{
			  alert("没有相关公司！");
			  chart.hideLoading();
			  showOrHideLoading(false);
			  return;
			}
			addDataToScatterChart(type,rd);
			showOrHideLoading(false);
	  });	
}

function getXXScatter(type,time,xindexcode,yindexcode,xminv,xmaxv,yminv,ymaxv)
{
	var companys = preSelectResult;
	var url = "<%=request.getContextPath()%>/chartSelectStock/getXX";
	
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post(url,{"time":time,"type":type,"industrytag":currenttag,"xindexcode":xindexcode,"yindexcode":yindexcode,"companys":companys,"xminv":xminv,"xmaxv":xmaxv,"yminv":yminv,"ymaxv":ymaxv}, function(result){
		  var rd = result.data;
		  if(rd==null)
			{
			  alert("没有相关公司！");
			  chart.hideLoading();
			  showOrHideLoading(false);
			  return;
			}
			preSelectResult = "";
			addDataToScatterChart(type,rd);
			rowsList = rd.pointList; 	
			showDataResult();
			$("#doselect").val("继续选股");
			$("#doselect_reset").show();
	  });	
}

function showDataResult(presult){

	showOrHideLoading(false);
	$("#rtableDiv").show();
	if(selectStockType==2)
	{
		showMultIndexTable(presult);
	}
	else
	{
		//showImageChartTable(presult)
	}
}
function updatePageNav(tcount,pindex)
{
	var totalpage = parseInt(tcount/pagesize)+1;
	$("#tdcount").empty();
	$("#tdcount").append(tcount);
	$("#cpage").empty();
	for(var i=0;i<totalpage;i++)
	{
		var pi = i+1;
		$("#cpage").append("<option value='"+pi+"'>"+pi+"</option>");
	}
	$("#cpage").val(pindex+1);
}
function showMultIndexTable(presult)
{
	var tcount = presult.totalcount;
	if(tcount==0) 
	{
		showOrHideLoading(false);
		chart.hideLoading();
		return;
	}
	var pindex = presult.pageindex;
	var rowsList = presult.l;
	updatePageNav(tcount,pindex);
	$("#tablediv").empty();
	$("#rowsdiv").show();
	var headColumnList = rowsList[0].valueArr;
	var htmlStr = "<table class=\"mytable\" width=\"95%\">";	
	htmlStr += "<tr><th align=\"center\" valign=\"middle\">" + "公司名称" + 
	"</th>";
	for(var h=0;h<headColumnList.length;h++)
	{
		var hcolumnchiname = headColumnList[h].split(";")[1];
		var hcolumnname = headColumnList[h].split(";")[0];
		htmlStr +="<th align=\"center\" valign=\"middle\"><a class=\"activelink\" onclick=\"changeSort('"+hcolumnname+"')\"><b>" +hcolumnchiname +"</b></a></th>" ;
	}
	htmlStr +="</tr>";
	for(var i=1;i<rowsList.length;i++){
		htmlStr += "<tr><td align=\"center\" valign=\"middle\"><a href='/stock/html/stocks_analysis.html?stockcode="+rowsList[i].stockCode+"&stockname="+escape(rowsList[i].companyName)+"&end' target='_blank'>"+rowsList[i].companyName+"</a></td>";
		var vl = rowsList[i].valueArr;
		for(var vi=0;vi<vl.length;vi++)
		{
			var vv = vl[vi];
			if(vv<1&&vv>-1)
			{
				vv=vv*100;
				vv=vv.toFixed(2)+"%";
				
			}	
			htmlStr +="<td align=\"center\" valign=\"middle\">" + vv + "</td>";
		}
		htmlStr+="</tr>";
	}
	htmlStr +="</table>"		
	$("#tablediv").append(htmlStr);
	$("#rowsdiv").show();
	showOrHideLoading(false);
}
function showImageChartTable(presult)
{
	var companys = "";
	
	var tcount = rowsList.length;
	if(tcount==0) return;
	var pindex = $("#cpage").val();
	if(pindex==null) pindex=1;
	updatePageNav(tcount,pindex-1);
	var startIndex = (pindex-1)*pagesize;
	var endIndex=startIndex+pagesize;
	if(endIndex>tcount)
		endIndex=tcount;
	if(startIndex>tcount)
		startIndex=tcount;
	for(var i=startIndex;i<endIndex;i++){
		companys += rowsList[i].companycode+";";
	}
	if(companys != ""){
		loadSort(companys);
	}
}
var csortArr = new Array();
function changeSort(columnname){//改变排序的时候，切换
	var pindex = $("#cpage").val();
	var tcount = $("#tdcount").text();
	pindex = pindex-1;
	//改变对应列的排序状态
	var cs = "";
	for(var i=0;i<csortArr.length;i++)
	{
		var c = csortArr[i];
		if(c.n==columnname)
		{
			c.s=!c.s;
			cs = c;
		}
	}
	//如果不存在，则加上
	if(cs=="")
		csortArr.push({n:columnname,s:false});
	showOrHideLoading(true);
	ajaxSubmitScreen(columnname,cs.s,pindex,tcount);
}


function getFHSScatter(type,time,xindexcode,yindexcode,xminv,xmaxv,yminv,ymaxv)
{
	var companys = preSelectResult;
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/chartSelectStock/getFhs",{"time":time,"type":type,"industrytag":currenttag,"xindexcode":xindexcode,"yindexcode":yindexcode,"companys":companys,"xminv":xminv,"xmaxv":xmaxv,"yminv":yminv,"ymaxv":ymaxv}, function(result){
		  var rd = result.data;
		  if(rd==null)
			{
			  alert("没有相关公司！");
			  chart.hideLoading();
			  showOrHideLoading(false);
			  return;
			}
			preSelectResult = "";
			addDataToScatterChart(type,rd);
			rowsList = rd.pointList; 	
			showDataResult();
			$("#doselect").val("继续选股");
			$("#doselect_reset").show();
	  });	
}


//各象限的点数
	var oneCount = 0;
	var twoCount = 0;
	var threeCount = 0;
	var fourCount = 0;
function addDataToScatterChart(type,sdata)
{
	if(isNull(sdata))
	{
		showOrHideLoading(false);
		 chart.hideLoading();
		return ;
	}
	//各象限的点数
	 oneCount = 0;
	 twoCount = 0;
	 threeCount = 0;
	 fourCount = 0;
	//保存相关数据
	cScatterData = sdata;
	var scatter = sdata;
	var data = scatter.pointList;
	if(data==null||data.length==0) 
	{
		chart.hideLoading();
		showOrHideLoading(false);
		return;
	}
	var i =0;
	var pointdata = new Array();
	for(i in data)
	{
	  		//分隔指标编码与名字
	  		var point = data[i];
	  		pointdata.push({
				id:point.companycode,
				name:point.companyname,
				x: point.x ,
				y: point.y
				});	

			preSelectResult +=point.companycode+";";
			//求出点在各个象限的分布数
			var xavg = scatter.xavg;
			var yavg = scatter.yavg;

			if(xavg!=null&&yavg!=null)
			{
				if(point.x>xavg&&point.y>yavg) oneCount++;
				if(point.x<xavg&&point.y>yavg) twoCount++;
				if(point.x<xavg&&point.y<yavg) threeCount++;
				if(point.x>xavg&&point.y<yavg) fourCount++;
			}
	}
	xtitle = scatter.xname;
	ytitle = scatter.yname;
	xavg = scatter.xavg;
	yavg = scatter.yavg;
	chart.destroy();
	chart = new scatterchart("chartdiv");
	
	var sname = "公司";
	var stockOrIndustry = $("#conditiondiv").find('input[name="stockOrIndustry"]:checked').val();
	if(stockOrIndustry=="1")
		sname = "行业";
	chart.addSeries({
		type: 'scatter',
        name: sname,
        turboThreshold:3000,//升级后版本限定显示节点个数
        data: pointdata
			});
		$("#selectResultDiv").empty();
	//把查询结果的简要显示出来
	$("#selectResultDiv").append("结果数："+data.length+" ");
	if(oneCount>0) $("#selectResultDiv").append("第一象限："+oneCount+" ");
	if(twoCount>0) $("#selectResultDiv").append("第二象限："+twoCount+" ");
	if(threeCount>0) $("#selectResultDiv").append("第三象限："+threeCount+" ");
	if(fourCount>0) $("#selectResultDiv").append("第四象限："+fourCount+" ");
	//加上均线
	if(type=='0')
	{
		addPlotLine(scatter,xavg,yavg);
	}
	else if(type == '1'){
		addRoeLine(scatter);
	}
	 chart.hideLoading();
	// showOrHideLoading(false);
}

/*
 * 增加ROE的反函数线
 */
function addRoeLine(scatter)
{	
	var indexavg = $("#roev").val();
	if(indexavg==null||indexavg=="") 
	{
		indexavg = scatter.indexavg;
		$("#roev").val(indexavg);
	}
	doAddRoeLine(indexavg);
}
function updateRoeLine()
{	
	var indexavg = $("#roev").val();
	doAddRoeLine(indexavg);
}
function doAddRoeLine(indexavg)
{
	if(indexavg!=null) 
	{
			var ymax = chart.yAxis[0].getExtremes().dataMax ;
			var ymin = chart.yAxis[0].getExtremes().dataMin ;
			var xmax = chart.xAxis[0].getExtremes().dataMax ;
			var xmin = chart.xAxis[0].getExtremes().dataMin ;
			
			if(xmin<=0) xmin = 1;
			if(ymin<=0) ymin = 0.01;
			var interval = (xmax-xmin)/5;
			//保存相关数据
			var roedata = [[xmin,indexavg/xmin],[xmin+interval,indexavg/(xmin+interval)],[xmin+2*interval,indexavg/(xmin+2*interval)],[xmin+3*interval,indexavg/(xmin+3*interval)],[xmin+4*interval,indexavg/(xmin+4*interval)],[xmax,indexavg/xmax]];
			//加x均线
			chart.addSeries({
				type: 'spline',
				index: 1,
		        name: "ROE="+indexavg,
		        data: roedata,
				marker: {
                    enabled: false
                },
                states: {
                    hover: {
                        lineWidth: 0
                    }
                },
                enableMouseTracking: false
					});
	}
}
function addPlotLine(scatter,xavg,yavg)
{
			if(xavg!=null) 
				chart.xAxis[0].addPlotLine({
						color: '#FF0000',
						width: 2,
						value: xavg,
						label: {
							text: '均线',
							verticalAlign: 'bottom',
							textAlign: 'right',
							y: -10
						}
						});
			if(yavg!=null) 
				chart.yAxis[0].addPlotLine({
						color: '#FF0000',
						width: 2,
						value: yavg,
						label: {
							text: '均线',
							verticalAlign: 'bottom',
							textAlign: 'right',
							x: 100
						}
						});
}


function getIndustryBycode(id,type,industrycode)
{
	 //先清空
	$("#"+id+"").empty();
	//$("#"+id+"").append("<option value='-2' selected>请选择行业</option>");
	if(id=="industry_0")
		$("#"+id+"").append("<option value='-1' selected>所有行业</option>");
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
				showOrHideLoading(false);
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
			$("#"+id+"").change();
		}
		//待定
if(id=="industry_0")
	$("#industry_0").val(currenttag);
}

 //初始化指标面板
function initIndexSelectPanel(id,sdata)
{ 
	 	if(isNull(sdata))
	 		{
				showOrHideLoading(false);
	 			return ;
	 		}
	     //先清空
		$("#"+id+"").empty();
		var data = sdata.split(";");
		if(data!="")
		{
			var i =0;
		  	for(i in data)
			{
		  			//分隔指标编码与名字
		  		var ia = data[i].split(":");
		  		var indexCode = ia[0];
		  		var indexName = ia[1];
				if(indexName.length>10) indexName = indexName.substring(0,10);
				var utext = indexName+":"+indexCode;
				$("#"+id+"").append("<option value="+indexCode+">"+utext+"</option>");
			}
		}
		
		
		if(id=="xindexselect")
			{
				var xtext = $("#xindexselect").find("option:selected").text();
				var xname = xtext.split(":")[0];
				$("#xtreemapname").val(xname+"规模");
				var indexcode = xtext.split(":")[1];
				$("#xtreemapcode").val(indexcode);
				$("#xindex").val(xtext);
			}
		if(id=="yindexselect")
		{
			var ytext = $("#yindexselect").find("option:selected").text();
			var yname = ytext.split(":")[0];
			$("#ytreemapname").val(yname+"规模");
			var indexcode = ytext.split(":")[1];
			$("#ytreemapcode").val(indexcode);
			$("#yindex").val(ytext);
		}
		if(id=="multindexselect")
		{
			var ytext = $("#multindexselect").find("option:selected").text();
			$("#addExtInput").val(ytext);
		}
}
function initMMMA(id)
{
				if(selectStockType==0)
				{
						if(currenttag=='-2') return;
						var time = $("#time_year").val()+"-"+$("#time_jidu").val();
						var utext = $("#xindex").val(); 
						if(utext!="")
						{
							var indexcode = utext.split(":")[1];
							if(id==null||id=="xindex")
							initMMAM("xindex",indexcode,time);
						}
					
						
						utext = $("#yindex").val(); 
						if(utext!="")
						{
							var indexcode = utext.split(":")[1];
							if(id==null||id=="yindex") initMMAM("yindex",indexcode,time);
						}
						
						
				}
				if(selectStockType==1)
				{
						if(currenttag=='-2') return;
						var indexcode = "2300"; 
						var time = $("#time_year").val()+"-"+$("#time_jidu").val();
						if(id==null||id=="2_xindex") initMMAM("2_xindex",indexcode,time)
						indexcode = "2022"; 
						if(id==null||id=="2_yindex") initMMAM("2_yindex",indexcode,time)
						
				}
}
//初始化最大值，最小值，中值，均值
function initMMAM(prefixid,indexcode,time)
{
	showLoading(1500);
	$("#doselect").attr("disabled",true);
	//取数据源编码
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.get("<%=request.getContextPath()%>/industry/getTagMMAM",{"time":time,"tag":currenttag,"indexcode":indexcode}, function(result){
				  var rd = result.data;
					if(rd==null)
						{
							showOrHideLoading(false);
							$("#doselect").attr("disabled",false);
							return;
						}
					var mmam = rd;
					var max = mmam.split(":")[0];
					var min = mmam.split(":")[1];
					var avg = mmam.split(":")[2];
					var mid = mmam.split(":")[3];

					if(max!='null') $("#"+prefixid+"_maxc").val(numberToThousandFormat(max));
					if(min!='null') $("#"+prefixid+"_minc").val(numberToThousandFormat(min));
					$("#"+prefixid+"_avgv").empty();
					if(avg!='null') $("#"+prefixid+"_avgv").append(numberToThousandFormat(avg));
					$("#"+prefixid+"_midv").empty();
					if(mid!='null') $("#"+prefixid+"_midv").append(numberToThousandFormat(mid));

				  $("#doselect").attr("disabled",false);
			  });

}
function initMulIndexMMMA(indexcode)
{
	
	var time = $("#time_year").val()+"-"+$("#time_jidu").val();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.post("<%=request.getContextPath()%>/industry/getTagMMAM",{"time":time,"tag":currenttag,"indexcode":indexcode}, function(result){
				  var rd = result.data;
					if(rd==null)
					{
						showOrHideLoading(false);
						return;
					}
					var mmam = rd;
					var max = mmam.split(":")[0];
					var min = mmam.split(":")[1];
					var avg = mmam.split(":")[2];
					var mid = mmam.split(":")[3];
					
					$("#"+indexcode+"_maxs").empty();
					if(max!='null') $("#"+indexcode+"_maxs").val(numberToThousandFormat(max));
					$("#"+indexcode+"_mins").empty();
					if(min!='null') $("#"+indexcode+"_mins").val(numberToThousandFormat(min));
					$("#"+indexcode+"_avgs").empty();
					if(avg!='null')
					{
						$("#"+indexcode+"_avgs").append(numberToThousandFormat(avg));
					}

				  $("#doselect").attr("disabled",false);
			  });
}
function clearMMAM()
{
	$("#xindex_minc").val("");
			$("#xindex_maxc").val("");
			$("#xindex_maxv").empty();
			$("#xindex_minv").empty();
			$("#xindex_avgv").empty();
			$("#xindex_midv").empty();

			$("#yindex_minc").val("");
			$("#yindex_maxc").val("");
			$("#yindex_maxv").empty();
			$("#yindex_minv").empty();
			$("#yindex_avgv").empty();
			$("#yindex_midv").empty();
}
function resetbutton()
{
	$("#doselect").val("选股");
	$("#doselect_reset").hide();
	preSelectResult = "";
	$("#industry_0").val("-1");
	$("#industry_1").empty();
	$("#industry_2").empty();
	//var industrycode = $("#industry_0").val().split(":")[1];
	//var rd = getIndustryBycode("industry_2","z",industrycode);
	//currenttag = $("#industry_1").val().split(":")[0];
	//initMMAM("xindex");
	//initMMAM("yindex");
	chart.destroy();
	chart = new scatterchart("chartdiv");
	$("#selectResultDiv").empty();
	$("#ctag").empty();
	$("#ctag").append("所有行业");
	clearMMAM();
	$("#rowsdiv").hide();
	$("#roev").val("");
	$("#tablediv").empty();
	$("#cpage").empty();
	$("#tdcount").empty();
	
}
var selectStockType=0;
function changeType(type){	
	selectStockType = type;
	if(type == 0){
		$("#dynamicCondition_1").show();
		$("#dynamicCondition_2").hide();
		$("#dynamicCondition_3").hide();
		$("#chartdiv").show();
	}else if(type == 1){
		$("#dynamicCondition_1").hide();
		$("#dynamicCondition_2").show();
		$("#dynamicCondition_3").hide();
		$("#chartdiv").show();
	}else if(type == 2){
		$("#dynamicCondition_1").hide();
		$("#dynamicCondition_2").hide();
		$("#dynamicCondition_3").show();
		$("#chartdiv").hide();
	}	
	resetbutton();
}


function _showAddCriteriaWizard()
{
		$("#addExtSpan").slideToggle("slow");
}
function addIndex2Area(id)
{	
	var oValue = $("#"+id+"").val();
	if(oValue == null || oValue == ""){
		alert("请输入指标");
		return;
	}
	var indexname = oValue.split(":")[0];
	var indexcode = oValue.split(":")[1];
	var tag = currenttag;
	var time = $("#time_year").val()+"-"+$("#time_jidu").val();
	
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("<%=request.getContextPath()%>/industry/getTagMMAM",{"time":time,"tag":currenttag,"indexcode":indexcode}, function(result){
		var rd = result.data;
		var columnname = indexname;
		//for(var i=0;i<indexRuleData.length;i++){
		//	if(indexRuleData[i].indexCode == indexcode){
		//		columnname = indexRuleData[i].columnName;
		//	}
		//}
		if(rd==null || rd.split(":")[0] == "null"){
			var obj = {indexCode:indexcode,indexName:indexname,maxValue:'',minValue:'',avgValue:'',columnName:columnname};
			appendRow(obj);
			return;
		}			
		var mmam = rd;
		var max = mmam.split(":")[0];
		var min = mmam.split(":")[1];
		var avg = mmam.split(":")[2];
		var mid = mmam.split(":")[3];
		
		var obj = {indexCode:indexcode,indexName:indexname,maxValue:max,minValue:min,avgValue:avg,columnName:columnname};
		appendRow(obj);
	});
	
}
function appendRow(obj) {
	var findexArray = document.getElementsByName("row_MarketCap");
	if(findexArray.length>5)
	{
		alert("指标数超标，筛选只最多支持6个条件！");
		return;
	}
		
	for ( var i = 0; i < findexArray.length; i++) {
		var trv = findexArray[i];
		if (trv.id == obj.indexCode) {
			alert("指标已存在！");
			return;
		}
	}
	var sindexcode = obj.indexCode;
	var rowHtml = "<tr name=\"row_MarketCap\" id='"+sindexcode+"'>";
	rowHtml += "<td class=\"field_name_td\">" + obj.indexName + "<\/td>";
	rowHtml += "<td class=\"align_left_td\">";
	rowHtml += "<input name=\""+sindexcode+"_left\" type=\"text\" size=\"10\" id='"+sindexcode+"_mins' class=\"field_input_default\" value=\""+numberToThousandFormat(obj.minValue)+"\">";
	rowHtml += "<\/td><td width=\"200\" id='"+sindexcode+"_avgs' style='text-align:center'>"+numberToThousandFormat(obj.avgValue)+"<\/td>";
	rowHtml += "<td class=\"align_right_td\">";
	rowHtml += "<input type=\"text\" class=\"field_input_default\" name=\""+sindexcode+"_right\" id='"+sindexcode+"_maxs' value=\""+numberToThousandFormat(obj.maxValue)+"\" size=\"10\">";
	rowHtml += "<\/td><td><img src=\"..\/images\/delete.gif\" class=\"activelink\" onClick=\"formToExt(this,'"
			+ obj.indexName + "','" + obj.indexCode + "')\"><\/td><\/tr>";
	$(".id-criteria_rows_tbody").append(rowHtml);
	numberInputBind($(".field_input_default"));
	$("#addExtInput").val("");
}

function formToExt(_element, indexName, indexCode) {
	var _tdParentElement = _element.parentNode;
	var _trParentElement = _tdParentElement.parentNode;
	var _bodyParentElement = _trParentElement.parentNode;
	_bodyParentElement.removeChild(_trParentElement);
}

function getIndexRuleData() {
	var date = "";
	$.getJSON("<%=request.getContextPath()%>/screen/getIndexArray", date, function(result) {
		if (result.success == null || !result.success) {
			if (result.error != "") {
				alert(result.error.msg);
			} else {
				alert("操作失败!");
			}

			return;
		}
		if (result.data == null || result.data == ""
				|| result.data.length == 0) {
			alert("没有找到数据!");
			return;
		}
		indexRuleData = result.data;		
	});
}
var pagesize=10;
function ajaxSubmitScreen(s,d,pindex,totalcount) {
	var q = $("#screenQ").serialize().replace(/%2C/ig,'');
	q = q.replace(/=/g,"|").replace(/&/g,";");
	var yq = $("#time_year").val()+"-"+$("#time_jidu").val();
	var url = "<%=request.getContextPath()%>/indextable/getIndexTableData?q="+q+"&pindex="+pindex+"&desc="+d+"&yearQuarter="+yq+"&totalcount="+totalcount+"&sortas="+s+"&tag="+currenttag+"&pagesize="+pagesize;
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript(url,function(result){
		if(presult==null) 
		{
			showOrHideLoading(false);
			chart.hideLoading();
			return ;
		}
		showDataResult(presult);	
		//showOrHideLoading(false);
	});
}

function loadSort(companys){
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("/stock/dashboard/getdashboarddataByCompanys?companys="+companys, function(result){
		if(result.data==null)
		{
			showOrHideLoading(false);
			return;
		}
		$("#tablediv").empty();
		var tt = "<table class=\"mytable\" id=\"tableid\"></table>";
		$("#tablediv").append(tt);
		var tbhtml = "";
		tbhtml+="<tr>";
		tbhtml+="<th>股票名称</th>";
		tbhtml+="<th>财务状况</th>";
		tbhtml+="<th>最新股价</th>";
		tbhtml+="<th>行情走势线形缩图</th>";
		tbhtml+="<th>经调整ROE</th>";
		tbhtml+="<th>PB值</th>";
		tbhtml+="<th>PS值</th>";
		tbhtml+="<th>经调整PE值</th>";
		tbhtml+="<th>ev（亿）</th>";
		tbhtml+="<th>估值温度计</th>";
		tbhtml+="<\/tr>";
		$("#tableid").append(tbhtml);
		
		var data = result.data;
		for(var i=0;i<data.length;i++)
			{
				var s = data[i];
				var stockcode = s.stockcode;
				var stockname = s.simplename;
				var rg = s.rg;
				var tdl = s.trades;
				var nsp = s.nsPrice;
				var pb = s.pb;
				var ps = s.ps;
				var pe = s.pe; 
				var ev = s.ev;
				var valuations = s.valuations;
				var roe = s.roe;
				//亿
				ev = ev/100000000;
				ev = ev.toFixed(2);
				tbhtml = "";
				if((i%2)==0){
					tbhtml+="<tr id='tr_"+i+"'>";
				}else{
					tbhtml+="<tr id='tr_"+i+"' class='dbrow'>";
				}				
				var escapeStr = escape(stockname);//中文编解码
				tbhtml+="<td><a href='/stock/html/stocks_analysis.html?stockcode="+stockcode+"&stockname="+escapeStr+"&end' target='_blank'>"+stockname+"</a></td>";
				var src;
				if(rg==-1)
					tbhtml+="<td><img src='/stock/images/red.gif'></img></td>";
				if(rg==1)
					tbhtml+="<td><img src='/stock/images/green.gif'></img></td>";
				if(rg==0)
					tbhtml+="<td>--</td>";
				var s = stockcode.split("\.")[0];
				var qurl = "http://stockhtm.finance.qq.com/sstock/ggcx/"+s+".shtml"
				if(nsp!=null) 
					tbhtml+="<td><a href='"+qurl+"' target='_blank'>"+nsp+"</a></td>";
				else 
					tbhtml+="<td>--</td>";
					
				if(tdl!=null)
					{
						tbhtml+="<td><span id='trades_"+i+"'></span></td>";
						//$("#trades_"+i).sparkline(tdl, {type: 'line'});
					}
					else 
					tbhtml+="<td>--</td>";

				if(roe!=null) 
					tbhtml+="<td>"+roe+"</td>";
				else 
					tbhtml+="<td>--</td>";
					
					if(pb!=null) 
					tbhtml+="<td>"+pb+"</td>";
				else 
					tbhtml+="<td>--</td>";
					
					if(ps!=null) 
					tbhtml+="<td>"+ps+"</td>";
				else 
					tbhtml+="<td>--</td>";
					
					if(pe!=null) 
					tbhtml+="<td>"+pe+"</td>";
				else 
					tbhtml+="<td>--</td>";
					
					if(ev!=null) 
					tbhtml+="<td>"+ev+"</td>";
				else 
					tbhtml+="<td>--</td>";
					
				if(valuations!=null)
					{
						tbhtml+="<td><span id='valuations_"+i+"'></span></td>";
						//$("#valuations_"+i).sparkline(valuations, {type: 'bullet'});
					}
					else 
					tbhtml+="<td>--</td>";
				tbhtml+="</tr>";
				$("#tableid").append(tbhtml);
				
				if(valuations!=null)
					{
						$("#valuations_"+i).sparkline(valuations, {type: 'bullet'});
					}
					
				if(tdl!=null)
					{
						$("#trades_"+i).sparkline(tdl, {type: 'line'});
					}
					
			}
			
		showOrHideLoading(false);
	});
	
	$("#rowsdiv").show();
}
function initAllTagsAutoMatcher(aa)
{
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.getScript("<%=request.getContextPath()%>/industry/getAllTags", function(result){
				if(retobj==null)
				{
					return;
				}
				var data = retobj.tags;
				//data.push("所有行业:-1")
				if(data!=null)
				{
					var c_data = data;
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
function change2treemap(type)
{
	var indexcode ;
	var indexname;
	if(type=="x")
	{
		indexcode = $("#xtreemapcode").val();
		indexname = $("#xtreemapname").val();
	}
		
	if(type=="y")
	{
		indexcode = $("#ytreemapcode").val();
		indexname = $("#ytreemapname").val();
	}
	indexname = indexname.replace("规模","");
	var tagname = $("#ctag").val();	
	var type = "msg_treemap_0";
	var stockOrIndustry = $("#conditiondiv").find('input[name="stockOrIndustry"]:checked').val();
	if(stockOrIndustry==1)
		type="msg_treemap_1";
	showRightFolating();
	var argData = {
		name : tagname,
		indexCode : indexcode,
		indexName : indexname,
		type:type
		};
	var nid = "sizer_0_zc_"+indexcode;
	autoAnalysis(argData,nid);						
}
function toggleIndexSelect(id)
{
	$("#"+id).toggle();
}
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
	$("#ctag").val(industryName);
	$('ul',$("#root")).css("visibility","hidden");
}
</script>
</head>
<body>
<div class="content" style="margin:50px">
	<div id="conditiondiv" style="text-align:left;" class="myfont" >
		 <br/>	 
		时间:<select name="time_year" id="time_year" >	
		</select><select name="time_jidu" id="time_jidu" >	
		<option value='3-30' selected>一季度</option>
		<option value='6-30' >二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>		
		</select>
		<br/>
	
		<div id="dynamicCondition_1">
		
		x轴指标:<input type="input" id="xindex" />
		<span id="xrvalue">Min:<input type="text" name="xindex_minc" id="xindex_minc" value="" style="width:150px;" class="numberInput" />
		Max:<input type="text" name="xindex_maxc" id="xindex_maxc" value="" style="width:150px;" class="numberInput"/>
		<!--Min:<span id="xindex_minv"></span> Max:<span id="xindex_maxv"></span> -->
		Avg:<span id="xindex_avgv"></span>&nbsp;&nbsp;Mid:<span id="xindex_midv"></span>		
		</span>
		<br/>
		y轴指标:<input type="input" id="yindex" />
		<span id="yrvalue">Min:<input type="text" name="yindex_minc" id="yindex_minc" value="" style="width:150px;" class="numberInput"/>
		Max:<input type="text" name="yindex_maxc" id="yindex_maxc" value="" style="width:150px;" class="numberInput"/>
		<!--Min:<span id="yindex_minv"></span>Max:<span id="yindex_maxv"></span>-->
		Avg:<span id="yindex_avgv"></span>&nbsp;&nbsp;Mid:<span id="yindex_midv"></span>  
		</span>
<br/>
		<input type="radio" name="stockOrIndustry" value="0" checked onclick="changeSelectType(0)"/>选股票
		<input type="radio" name="stockOrIndustry" value="1" onclick="changeSelectType(1)"/>选行业
		<select id="showAllLeafchild">
			<option value="0" selected>只展示下一级行业数</option>
			<option value="1">展示所有最底动层行业</option>
		</select>
		<br/>
		</div>
		<div id="dynamicCondition_2" style="display: none;">
		权益乘数:
		Min:<input type="text" name="2_xindex_minc" id="2_xindex_minc" value="" style="width:100px;" class="numberInput"/>---->
		Max:<input type="text" name="2_xindex_maxc" id="2_xindex_maxc" value="" style="width:100px;" class="numberInput"/>
		Avg:<span id="2_xindex_avgv"></span> Mid:<span id="2_xindex_midv"></span> <br/>
		资产收益率ROA:
		Min:<input type="text" name="2_yindex_minc" id="2_yindex_minc" value="" style="width:100px;" class="numberInput"/>---->
		Max:<input type="text" name="2_yindex_maxc" id="2_yindex_maxc" value="" style="width:100px;" class="numberInput"/>
		Avg:<span id="2_yindex_avgv"></span> Mid:<span id="2_yindex_midv"></span> <br/>
		净资产收益率ROE:<input type="text" name="roev" id="roev" value=""/>
		<input type="button" value="更新ROE线" onclick="updateRoeLine()"/>
		</div>
		<div id="dynamicCondition_3" style="display: none;">		
	
		
		</div>
		
		<input type="checkbox" name="selectTip" id="selectTip" checked=true/>
		<input type="button" name="doselect" id="doselect" value="选股" />
		<input type="button" name="doselect" id="doselect_reset" value="重选" style="display:none" onclick="resetbutton()"/>
		 <div id="tags_div">
			<ul id="root" class="dropdown" style="z-index:1010">
			</ul>
		   </div>
		当前行业:<input id="ctag" style="color:red" value="所有行业"/>
	</div>
<div id="mySizer"></div>
<div id="selectResultDiv"></div>
<div id="chartdiv"></div>
<div id="rtableDiv" style="display:none">
<div id="rowsdiv">
<div id="tablediv"></div>
结果总数：<span id="tdcount"></span> ,当前页：<select id="cpage"></select>
</div>
</div>
 </div>
</body>
</html>

