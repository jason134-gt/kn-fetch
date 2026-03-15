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
 <title>价值投资分析系统</title>
<link rel="stylesheet" type="text/css" href="css/InvestAnalysis2.css">
<script src="js/jquery/jquery-1.6.2.min.js" type="text/javascript"></script>
<script src="js/highcharts/js/highcharts.src.js" type="text/javascript"></script>
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
 
$(document).ready(function(){                          
    $("dt").click(function(){                  
        $("dd").css("display","none"); 
        $(this).next("dd").css("display","block");
    });
}); 
</script>
<script type="text/javascript">
function mouseOver(id){
document.getElementById(id).style.fontSize="1.6em";
document.getElementById(id).style.backgroundColor="#86a0e9";
}
function mouseOut(id){
document.getElementById(id).style.fontSize="1.3em";
document.getElementById(id).style.backgroundColor="transparent";
}
</script>
<script type='text/javascript'>
var chart;
var type = 'spline';

var curcompanycode = null;
$(document).ready(function() {
	//初始化页面
	//initIndexAnalysis();
	//构造图表
	chart = new myChart('divCharts');
	initIndexPanelByDs("tableSystem");
	var tscode = $("#tableSystem").val();
	//加载缺省指标
	queryIndex(tscode);
	
	initIndustry("root",-1);
	initCompanyAutoMatcher('div_companyCode');
	initAutoIndexMatcher('div_indexCode');
	initUint();
	initYear("startTime_year",15);
	initYear("endTime_year",19);

	$(document).bind("click", function (e) { 

			if(e.target.id!="showIndustryButton")
			{
				$("#industry_panel").slideUp();
				$("#showIndustryButton").val("显示同行");
			}
			
		});
	$("#companyalltags").click(function(){
			loadCompanysTags();
	});

	$("#tags_div").mouseover(function(e){
		$('ul',$("#tags_div")).css("visibility","");
	});
	$("#div_indexCode").click(function(e){
		$("#div_indexCode").val("");
	});
	
	$("#div_companyCode").click(function(e){
		$("#div_companyCode").val("");
	});
	
	
	$("#div_companyCode").blur(function(){
		var companys = $("#div_companyCode").val();
		if(companys.indexOf(":")<0) return;
		loadCompanysTags();
	});


	$("#tableSystem").change(function(){
		clearData();
		hiddenIndexPanel();
		$("#defineDiv").slideUp();
		$("#div_indexCode").val("");
		var tscode = $("#tableSystem").val();
		 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		 $.getScript("<%=request.getContextPath()%>/cfirule/getCfiruleListByTidWithOutCache_V2?msg.tableSystemCode="+tscode, function(result){
		  	if(retObj==null)
				{
					return;
				}
				var data = retObj.split(";");
				if(data!=null)
				{
					var c_data = data;
					$("#div_indexCode").flushCache();
					$("#div_indexCode").autocomplete(c_data, {
					max: 12, //列表里的条目数
					minChars: 0, //自动完成激活之前填入的最小字符
					width: 150, //提示的宽度，溢出隐藏
					scrollHeight: 300, //提示的高度，溢出显示滚动条
					matchContains: true, //包含匹配，就是data参数里的数据，是否只要包含文本框里的数据就显示
					autoFill: false //自动填充
					});
					$("#tindexText").flushCache();
					$("#tindexText").autocomplete(c_data, {
					max: 12, //列表里的条目数
					minChars: 0, //自动完成激活之前填入的最小字符
					width: 150, //提示的宽度，溢出隐藏
					scrollHeight: 300, //提示的高度，溢出显示滚动条
					matchContains: true, //包含匹配，就是data参数里的数据，是否只要包含文本框里的数据就显示
					autoFill: false //自动填充
					});
					
				}

	  });
		
	});
	$("#dataSource").change(function(){
		//初始化报表体系
		initTableSystem("dataSource","tableSystem");
	});

	//规则定义面板的事件处理方法
	$("#realTimeDeText").click(function(e){
		e.which;
		//获取$("#deText")的位置
		var ntop =  $("#realTimeDeText").offset().top+$("#realTimeDeText").height()+10;
		var nleft =  $("#realTimeDeText").offset().left;
		//缓存各数据源对应的指标,按下拉框索引
		var dindex =$("#dataSource").get(0).selectedIndex;

		$("#defineDiv").css({'position':'absolute','top':ntop,'left':nleft});
			$("#defineDiv").slideDown();
			$("#defineDiv").focus();
		//设置发生事件的面板
		indexDefineFlag = 1;
	});//end click

	//隐藏规则定义面板
	$("#dcButton").click(function(){
		 $("#defineDiv").slideUp();
	});//end click
	//取模板中的变量
	$("#div_templateCode").change(function(){
		  $("#variableDiv").empty();
		  //解析嵌套模板变量
		  paraseTemplateVar();
		  
	  });//end click

	//改变显示单位
	$("#unit_select").change(function(){
			unitValue = $("#unit_select").val();
	  });//end click

});//end ready
function initIndustry(uid,prefix)
{

	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("<%=request.getContextPath()%>/industry/getYfzxAllIndustryJsonData", function(result){
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
	$("#companyalltags").val(industryName);
	$('ul',$("#root")).css("visibility","hidden");
	var b = $("#industry_batch").val();
	if(b==0)
	{
		getIndexSpecialValue(4,industryName);
	}
	else
	{
		
		getIndexSpecialValue(4,industryName);
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.get("<%=request.getContextPath()%>/industry/getYFZXIndustryChildrenByName",{"name":industryName,'type':b}, function(result){
			  var sdata = result.data;
			  if(sdata==null)
				  return ;
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
						getIndexSpecialValue(4,industryName);
					}
				}
		  });	

	}
	
}
function loadCompanysTags()
{
	$("#industry_panel").slideUp();
		$("#showIndustryButton").val("显示同行");
		var companys = $("#div_companyCode").val();
		if(companys==""||companys.split(":").length<2)
			{
				alert("请先输入公司！");
				return ;
			}
		var companycode = companys.split(":")[1];
		if(curcompanycode!=companycode)
		{
			//getCompanyAlltags(companycode);
			curcompanycode = companycode;
		}
}

function initAutoIndexMatcher(aa)
{

		var tsc = $("#tableSystem").val();
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.getScript("<%=request.getContextPath()%>/cfirule/getCfiruleListByTscid_V2?msg.tableSystemCode="+tsc, function(result){
				
				if(retObj==null)
				{
					return;
				}
				var data = retObj.split(";");
				if(data!=null)
				{
					var c_data = data;

					$("#"+aa).autocomplete(c_data, {
					max: 30, //列表里的条目数
					minChars: 0, //自动完成激活之前填入的最小字符
					width: 150, //提示的宽度，溢出隐藏
					scrollHeight: 300, //提示的高度，溢出显示滚动条
					matchContains: true, //包含匹配，就是data参数里的数据，是否只要包含文本框里的数据就显示
					autoFill: false //自动填充
					});
				}
		  });
	

}

function getCompanyAlltags(companycode)
{
	 $("#companyalltags").empty();
	 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   $.getJSON("<%=request.getContextPath()%>/company/getComanyAllTags?companycode="+companycode, $("#dataForm").serialize(),function(result){
			   if(result.success)
				   {
						$("#companyalltags").append("<option value='所有行业'>所有行业</option>");
				   		var l = result.data;
				   		for(var i=0;i<l.length;i++)
				   			{
				   				var tag = l[i];
				   				$("#companyalltags").append("<option value='"+tag+"'>"+tag+"</option>");
				   			}
				   }
				
		   });//end getJSON
}
function initUint()
{
	unitValue = $("#unit_select").val();
	if(unitValue==0.01)
	{
		unitName="单位:%";
	}
	if(unitValue==1)
	{
		unitName="";
	}
	if(unitValue==1000)
	{
		unitName="单位:千";
	}
	if(unitValue==10000)
	{
		unitName="单位:万";
	}
	if(unitValue==1000000)
	{
		unitName="单位:百万";
	}
	if(unitValue==10000000)
	{
		unitName="单位:千万";
	}
	if(unitValue==100000000)
	{
		unitName="单位:亿";
	}

}
var tZero = "";
function initZero()
{
		var c = $("#zero_checkBox").is(":checked");
		if(c)
		{
			isRemoveZero = true;
			tZero="去掉0值";
		}
		else
		{
			isRemoveZero = false;
			tZero="不去掉0值";
		}
}
//初始化系统主页
function initIndexAnalysis()
{
	//构造图表
	 chart = new myChart('divCharts');
	initIndexAndRulePanel();
}
function initIndexAndRulePanel()
{
	//初始数据源
	initDataSource("dataSource");
	//根据报表体系,加载对应的缺省指标
	//initIndexPanelByDs("dataSource","div_companyCode");

}
//初始数据源 did 是数据源选择框的id
var dataSourceData = null;
function initDataSource(did)
{
	 	  //先清空
	   	  $("#"+did+"").empty();
	 	  if(dataSourceData==null||dataSourceData=="")
	 		  {
		 		  $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		 		  $.get("<%=request.getContextPath()%>/dataSource/getDataSourceList", function(result){
		 			 var data = result.data;
		 			initDataSourceSelect(did,data);
		 			//缓存一份
		 			dataSourceData = data;
		 		  });
	 		  }
	 	  else
	 		  {
	 		 	initDataSourceSelect(did,dataSourceData);
	 		  }
		  
}
function initDataSourceSelect(did,data)
{
	if(data!="")
	{
		var i =0;
	  	for(i in data)
		{
	  			//分隔指标编码与名字
	  		var ds = data[i];
	  		var dataSourceCode = ds.dataSourceCode;
	  		var dataSourceName = ds.dataSourceName;
			if(i==0)
			{
				$("#"+did+"").append("<option value="+dataSourceCode+" selected>"+dataSourceName+"</option>");
			}
			else
			{
				$("#"+did+"").append("<option value="+dataSourceCode+">"+dataSourceName+"</option>");
			}
			
		}
	  	//setSelect(did,0);
		//初始化报表体系
		initTableSystem("dataSource","tableSystem");
	}
}
/*
 * did:数据源select 的id
 * cid:公司输入框id
 */
 var indexArray = new Array();//存数据源索引与指标列表的映射
function fetchIndexAndShow(index,source,top,left)
{
	textCode = index; 
	currentObj=source;
	//initIndexPanelByDs("tableSystem");
	//显示指标选择面板
	showIndexPanel("indexPanel",source,top,left);
}

function fetchIndexRule()
{
	initIndexPanelByDs("tableSystem");
}
function initIndexPanelByDs(tid)
{
	//取数据源编码
	var tsCode = $("#"+tid+"").val();
	
	
			  $.get("<%=request.getContextPath()%>/cfirule/getCfiruleListByTid?msg.tableSystemCode="+tsCode, function(result){
				  var rd = result.data;
					//初始化指标选择面板
					initIndexSelectPanel(rd);
				
			  });	
		
	
}
 //初始化指标面板
function initIndexSelectPanel(sdata)
{ 
	 	if(isNull(sdata))
	 		{
	 			return ;
	 		}
	     //先清空
		$("#assetSelect").empty();
		 //先清空
		$("#cashFlowSelect").empty();
		 //先清空
		$("#profileSelect").empty();
		 //先清空
		$("#extIndexSelect").empty();
	
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
				var tableName = ia[2];
				var type = ia[3];
				//按表名把指标追加到不到的面板上
				initIndexPanel(indexCode,indexName,tableName,type);
			}
		}
}

//初始数据源 did 是数据源选择框的id
var tableSystemHtml = new Array();
function initTableSystem(did,tid)
{

		  var dsc=$("#"+did+"").val();
		  var dindex =$("#"+did+"").get(0).selectedIndex;
		  var tsHtml = tableSystemHtml[dindex];
	 	  if(tsHtml==null||tsHtml=="")
	 		  {
		 		  $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		 		  $.get("<%=request.getContextPath()%>/tableSystem/getTableSystemListByDs?msg.dataSourceCode="+dsc, function(result){
		 			 var data = result.data;
		 			initTableSystemSelect(tid,data);
		 			//缓存一份html
					tableSystemHtml[dindex] = $("#tableSystem").html();
		 		  });
	 		  }
	 	  else
	 		  {
	 		 	$("#tableSystem").empty();
				$("#tableSystem").append(tsHtml);
	 		  }
}
function initTableSystemSelect(tid,data)
{
	if(data!="")
	{
		 //先清空
	   	$("#tableSystem").empty();
		var i =0;
	  	for(i in data)
		{
	  			//分隔指标编码与名字
	  		var ts = data[i];
	  		var tableSystemCode = ts.tableSystemCode;
	  		var tableSystemName = ts.tableSystemName;
			if(i==0)
			{
				$("#"+tid+"").append("<option value='"+tableSystemCode+"' selected>"+tableSystemName+"</option>");
			}
			else
			{
				$("#"+tid+"").append("<option value='"+tableSystemCode+"'>"+tableSystemName+"</option>");
			}
			
		}
		//setSelect(tid,0);
	initIndexPanelByDs("tableSystem");
	initRuleSelect("tableSystem");
	}
}
//取公司对应的指标
function queryIndex(tsCode)
{
	  $.getScript("<%=request.getContextPath()%>/cfirule/getCfiruleListByTidWithOutCache_V2?msg.tableSystemCode="+tsCode, function(result){
		  	if(retObj==null)
				{
					return;
				}
				//var jresult = jQuery.parseJSON(result);
				var data = retObj.split(";");
				if(data!=null)
				{
					var c_data = data;

					$("#sindex").autocomplete(c_data, {
					max: 12, //列表里的条目数
					minChars: 0, //自动完成激活之前填入的最小字符
					width: 150, //提示的宽度，溢出隐藏
					scrollHeight: 300, //提示的高度，溢出显示滚动条
					matchContains: true, //包含匹配，就是data参数里的数据，是否只要包含文本框里的数据就显示
					autoFill: false //自动填充
					});
					
					$("#tindexText").flushCache();
					$("#tindexText").autocomplete(c_data, {
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

//构建请求消息
function buildReqMsg()
{
	//公司编码
	$("#companyCode").val($("#div_companyCode").val());
	$("#companyList").val($("#companyCode").val());
	//指标编码
	$("#indexCode").val($("#div_indexCode").val().split(":")[1]);
	//超始时间
	$("#startTime").val($("#startTime_year").val()+"-"+$("#startTime_jidu").val());
	//结束时间
	$("#endTime").val($("#endTime_year").val()+"-"+$("#endTime_jidu").val());
	//时间间隔
	$("#interval").val($("#div_interval").val());
	//报表体系
	$("#tableSystemCode").val($("#tableSystem").val());
	//数据源
	$("#dataSourceCode").val($("#dataSource").val());
	
	//取模板中的变量列表
	getAllVariable();
	return ;
}
//构建实时分析区的消息
function buildRealReqMsg()
{
	//公司编码
	var cList = $("#div_companyCode").val();
	$("#realTimeCompanyList").val(cList);
	//超始时间
	$("#realTimeStartTime").val($("#startTime_year").val()+"-"+$("#startTime_jidu").val());
	//结束时间
	$("#realTimeEndTime").val($("#endTime_year").val()+"-"+$("#endTime_jidu").val());
	//报表体系
	$("#realTableSystemCode").val($("#tableSystem").val());
	//数据源
	$("#realDataSourceCode").val($("#dataSource").val());
	return ;
}
//基本指标校验
function checkValidate()
{
	//如果与后台有请求,则认为不是图表中模型的切换
	isChangeMode = false;
	if($("#companyCode").val()=="")	
	{
		return "公司编码不可为空!";
	}
	if($("#startTime").val()=="")	
	{
		return "开始时间不可为空!";
	}
	if($("#endTime").val()=="")	
	{
		return "结束时间不可为空!";
	}
	if(compareDate($("#startTime").val(),$("#endTime").val()))
	{
		return "开始时间必须小于结束时间!";
	}
	return "";
}
//实时指标校验
function checkRealTimeValidate()
{
	//如果与后台有请求,则认为不是图表中模型的切换
	isChangeMode = false;
	if($("#realTimeCompanyList").val()=="")	
	{
		return "公司编码不可为空!";
	}
	return "";
}

//基本分析
function qhy(type)
{
		initUint();
		initZero();
		if(!window.confirm("您的选择参数为:\n"+unitName+" "+tZero))
		{
			return;
		}
	  //构建请求消息
	    buildReqMsg();
	 //消息参数校验
	   var ret = checkValidate();
	   if(ret!="")
		{
		   alert(ret);
		   return;
		}
		if($("#indexCode").val()=="")	
		{
			alert("指标编码不可为空!");
			return;
		}
	   cIndexName = $("#div_indexCode").val();
	   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	   $.getJSON("<%=request.getContextPath()%>/index/getIndexQhy?type="+type, $("#dataForm").serialize(),function(result){
			 if(result.success==null||!result.success)
				   {
					  if(result.error!="")
					   {
							 alert(result.error.msg);
					   }
					   else
					   {
							 alert("操作失败!");
					   }
					  
					   return;
				   }
				   if(result.data==""||result.data.length==0)
				   {
						alert("没有打到数据!");
						return;
				   }
				   //以后台给的指标名为准
				   cIndexName = "";
			showResult(result);
		  	//保存此次查询结果
			keepResultArray[keepResultArray.length] = result;
	   });//end getJSON
}

//基本分析
function baseAnalysis()
{
		initUint();
		initZero();
		if(!window.confirm("您的选择参数为:\n"+unitName+" "+tZero))
		{
			return;
		}
	  //构建请求消息
	    buildReqMsg();
	 //消息参数校验
	   var ret = checkValidate();
	   if(ret!="")
		{
		   alert(ret);
		   return;
		}
		if($("#indexCode").val()=="")	
		{
			alert("指标编码不可为空!");
			return;
		}
	   cIndexName = $("#div_indexCode").val();
	   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	   $.getJSON("<%=request.getContextPath()%>/index/getIndexValueList", $("#dataForm").serialize(),function(result){
			 if(result.success==null||!result.success)
				   {
					  if(result.error!="")
					   {
							 alert(result.error.msg);
					   }
					   else
					   {
							 alert("操作失败!");
					   }
					  
					   return;
				   }
				   if(result.data==""||result.data.length==0)
				   {
						alert("没有打到数据!");
						return;
				   }
			showResult(result);
		  	//保存此次查询结果
			keepResultArray[keepResultArray.length] = result;
	   });//end getJSON
}
//实时分析
function realTimeAnalysis()
{
		initUint();
		initZero();
		if(!window.confirm("您的选择参数为:\n"+unitName+" "+tZero))
		{
			return;
		}

			//构建请求消息
		   buildRealReqMsg();
		  //消息参数校验
		   var ret = checkRealTimeValidate();
		   if(ret!="")
			{
			   alert(ret);
			   return;
			}
			//超始时间
			$("#realTimeStartTime").val($("#startTime_year").val()+"-"+$("#startTime_jidu").val());
			//结束时间
			$("#realTimeEndTime").val($("#endTime_year").val()+"-"+$("#endTime_jidu").val());
			if($("#realTimeEndTime").val()<$("#realTimeStartTime").val())
			{
				alert("开始时间必须小于结束时间!");
				 return;
			}
			 cIndexName = "实时分析指标";
			 $("#realTimeDeText_hidden").val($("#realTimeDeText").val());
		   if($("#realTimeDeText_hidden").val()==""||$("#realTimeDeText_hidden").val()==null)
			   {
			   		alert("规则不可以为空!");
			   		return;
			   }
			   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
			   $.getJSON("<%=request.getContextPath()%>/index/realTimeAnalysis", $("#realAnalysisForm").serialize(),function(result){
				    if(result.success==null||!result.success)
				   {
					   if(result.error!="")
					   {
							 alert(result.error.msg);
					   }
					   else
					   {
							 alert("操作失败!");
					   }
					  
					   return;
				   }
				    if(result.data==null||result.data==""||result.data.length==0)
					   {
							alert("没有找到数据!");
							return;
					   }
				showResult(result);
				  	//保存此次查询结果
				keepResultArray[keepResultArray.length] = result;
			   });//end getJSON
}
//同比与环比分析
function AnalysisCAndH(type)
{
		initUint();
		initZero();
		if(!window.confirm("您的选择参数为:\n"+unitName+" "+tZero))
		{
			return;
		}
	 	//构建请求消息
		   buildRealReqMsg();
		  //消息参数校验
		   var ret = checkRealTimeValidate();
		   if(ret!="")
			{
			   alert(ret);
			   return;
			}
			//超始时间
			$("#realTimeStartTime").val($("#startTime_year").val()+"-"+$("#startTime_jidu").val());
			//结束时间
			$("#realTimeEndTime").val($("#endTime_year").val()+"-"+$("#endTime_jidu").val());
			if($("#realTimeEndTime").val()<$("#realTimeStartTime").val())
			{
				alert("开始时间必须小于结束时间!");
				 return;
			}
		var optName = "";
		var rule;
		var indexName = $("#div_indexCode").val();
		var iCode = indexName.split(":")[1];
		if(type==0)
		{
			rule = "\${"+iCode+"}"+"/\${"+iCode+",-12}-1";
			optName="同比";
		}
		else
		{
			rule = "\${"+iCode+"}"+"/\${"+iCode+",-3}-1";
			optName="环比";
		}
		$("#realTimeDeText_hidden").val(rule);
	   cIndexName = $("#div_indexCode").val()+"--->"+optName;
		   if($("#realTimeDeText_hidden").val()==""||$("#realTimeDeText_hidden").val()==null)
			   {
			   		alert("规则不可以为空!");
			   		return;
			   }
			   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
			   $.getJSON("<%=request.getContextPath()%>/index/realTimeAnalysis", $("#realAnalysisForm").serialize(),function(result){
				    if(result.success==null||!result.success)
				   {
					   if(result.error!="")
					   {
							 alert(result.error.msg);
					   }
					   else
					   {
							 alert("操作失败!");
					   }
					  
					   return;
				   }
				    if(result.data==null||result.data==""||result.data.length==0)
					   {
							alert("没有找到数据!");
							return;
					   }
				showResult(result);
				  	//保存此次查询结果
				keepResultArray[keepResultArray.length] = result;
			   });//end getJSON
}

//同比与环比分析
function computeAvg(points)
{
		initUint();
		initZero();
		if(!window.confirm("您的选择参数为:\n"+unitName+" "+tZero))
		{
			return;
		}
	 	//构建请求消息
		   buildRealReqMsg();
		  //消息参数校验
		   var ret = checkRealTimeValidate();
		   if(ret!="")
			{
			   alert(ret);
			   return;
			}
			//超始时间
			$("#realTimeStartTime").val($("#startTime_year").val()+"-"+$("#startTime_jidu").val());
			//结束时间
			$("#realTimeEndTime").val($("#endTime_year").val()+"-"+$("#endTime_jidu").val());
			if($("#realTimeEndTime").val()<$("#realTimeStartTime").val())
			{
				alert("开始时间必须小于结束时间!");
				 return;
			}
		var optName = "";
		var rule;
		var indexName = $("#div_indexCode").val();
		var iCode = indexName.split(":")[1];
		if(points=="2") optName="两点";
		if(points=="3") optName="三点";
		if(points=="5") optName="五点";
		//几点
		$("#realPoints").val(points)
		$("#realTimeIndexCode").val($("#div_indexCode").val().split(":")[1]);
		$("#realTimeDeText_hidden").val(rule);
	   cIndexName = $("#div_indexCode").val()+"-->"+optName;
		  
			   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
			   $.getJSON("<%=request.getContextPath()%>/index/getAverageIndex", $("#realAnalysisForm").serialize(),function(result){
				    if(result.success==null||!result.success)
				   {
					   if(result.error!="")
					   {
							 alert(result.error.msg);
					   }
					   else
					   {
							 alert("操作失败!");
					   }
					  
					   return;
				   }
				    if(result.data==null||result.data==""||result.data.length==0)
					   {
							alert("没有找到数据!");
							return;
					   }
				 
				showResult(result);
				  	//保存此次查询结果
				keepResultArray[keepResultArray.length] = result;
			   });//end getJSON
}


//模板分析
function templateAnalysis()
{
		initUint();
		initZero();
		if(!window.confirm("您的选择参数为:\n"+unitName+" "+tZero))
		{
			return;
		}

			//构建请求消息
		    buildReqMsg();
			//模板编码
			var tempCode=$("#div_templateCode").val().split("\|")[0];
			$("#templateCode").val(tempCode);
			//消息参数校验
		   var ret = checkValidate();
		   if(ret!="")
			{
			   alert(ret);
			   return;
			}
			if($("#endTime").val()<$("#startTime").val())
			{
				alert("开始时间必须小于结束时间!");
				 return;
			}
		   if($("#templateCode").val()==""||$("#templateCode").val()==null)
			   {
			   		alert("模板编码不可以为空!");
			   		return;
			   }
			   //保存模板名
			   var o = $("#div_templateCode").get(0);
			cIndexName = o.options[o.selectedIndex].text;;
			$("#interval").val($("#temp_interval").val());
		   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   $.getJSON("<%=request.getContextPath()%>/index/templateAnalysisByTreee", $("#dataForm").serialize(),function(result){
			   if(result.success==null||!result.success)
				   {
					   if(result.error!="")
					   {
							 alert(result.error.msg);
					   }
					   else
					   {
							 alert("操作失败!");
					   }
					  
					   return;
				   }
				   if(result.data==""||result.data.length==0)
				   {
						alert("没有打到数据!");
						return;
				   }
				showResult(result);
			  	//保存此次查询结果
				keepResultArray[keepResultArray.length] = result;
				
		   });//end getJSON
}
//查询公司的同一行业信息并生成行业选择面板
function createIndustryPanel(id,source)
{
	if($("#industry_panel").is(":hidden"))
	{
		var tag = $("#companyalltags").val();
		
		if(tag==""||tag=="-1")
		{
			alert("分类标签不可以为空！");
			$("#companyalltags").focus();
			return ;
		}

		 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
			   $.get("<%=request.getContextPath()%>/company/getComanyListByTags?tag="+tag,function(result){
				   if(result==null)
						  return;
				var companys = result;
			$("#industry_panel_table").empty();
			 var d = companys.split(";");
			var i=0 ;
			//分隔每个公司
			for(i=0 ;i<d.length;i++)
				{
				var company = d[i];
				//var tt = company.chName+":"+company.companyCode;
				var tt = company;
				$("#industry_panel_table").append("<tr class='alt1'><td><a href='' onclick='append2CompanyText("+id+",this);return false'>"+tt+"</a></td></tr>");

				}	
					
			   });//end getJSON
		//显示面板
		showIndustryPanel(id,source);
			   if(id==0)
				   {
				   $("#showIndustryButton").val("隐藏同行");
				   }
			   else
				   {
				   
				   $("#showIndustryButton_cfdata").val("隐藏同行");
				   }
		
	}
	else
	{
		$("#industry_panel").slideUp();
		   if(id==0)
			   {
			   $("#showIndustryButton").val("显示同行");
			   }
		   else
			   {
			   $("#showIndustryButton_cfdata").val("显示同行");
			   }
		
	}
	
}
function showIndustryPanel(id,source)
{
		//获取$("#deText")的位置
		var ntop =  realOffset(source).y+25;
		var nleft =  realOffset(source).x;
		$("#industry_panel").css({'position':'absolute','top':ntop,'left':nleft,'z-index':2});
		$("#industry_panel").slideDown();
		$("#industry_panel").focus();
		return;
}
//查询公司相应的财务数据
var tableid=0;
function queryAllCompanyIndexData()
{
			buildQueryCfdataMsg();
			//消息参数校验
		   var ret = checkQueryCfdataMsg();
		   if(ret!="")
			{
			   alert(ret);
			   return;
			}

			if(compareDate($("#startTime").val(),$("#endTime").val()))
			{
				alert("开始时间必须小于结束时间!");
				 return;
			}
		   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   tableid=$("#tableid").val();
		   $.getJSON("<%=request.getContextPath()%>/index/queryAllCompanyIndexData", $("#dataForm").serialize(),function(result){
			   if(result.success==null||!result.success)
				   {
						if(result.error!="")
					   {
							 alert(result.error.msg);
					   }
					   else
					   {
							 alert("操作失败!");
					   }
					  
					   return;
				   }
				   if(result.data==""||result.data.length==0)
				   {
						alert("没有找到数据!");
						return;
				   }
				showCompanyAllIndexData(result);
		   });//end getJSON
}
function hideObjectAndUncheck(id)
{
	$("#"+id+"").slideUp();
	$("#batch_checkBox").attr("checked",false);
}

function showCfdataPanel()
{
	window.showModalDialog ("<%=request.getContextPath()%>/jsp/portal/cfdata.jsp", "指标数据", "dialogHeight=600px;dialogWidth=1050px;dialogLeft=300;dialogTop=100;scroll=yes;resizable=yes;status=no");
}
function getIndexSpecialValue(type,tag)
{
	initUint();
	initZero();
	//if(!window.confirm("您的选择参数为:\n"+unitName+" "+tZero))
	//{
	//	return;
	//}
 		//构建请求消息
	   buildRealReqMsg();

		//超始时间
		var sTime = $("#startTime_year").val()+"-"+$("#startTime_jidu").val();
		//结束时间
		var eTime = $("#endTime_year").val()+"-"+$("#endTime_jidu").val();
		if(eTime<sTime)
		{
			alert("开始时间必须小于结束时间!");
			 return;
		}
	var optName = "";
	var rule;
	var indexName = $("#div_indexCode").val();
	if(indexName=="")
	{
		alert("指标编码不可以为空！");
		return;
	}
	var iCode = indexName.split(":")[1];
	var mark = $("#companyalltags").val();
	if(tag!=null&&tag!="")
		mark = tag;
	if(mark==-1)
	{
			alert("请先选择分类信息！");
			$("#companyalltags").focus();
			return;
	}
	var indexcode = $("#div_indexCode").val().split(":")[1];

   cIndexName = $("#div_indexCode").val()+"--->"+optName;
	  
		   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   $.getJSON("<%=request.getContextPath()%>/index/getMaxMinAvgMid?type="+type+"&indexcode="+indexcode+"&sTime="+sTime+"&eTime="+eTime+"&mark="+mark,function(result){
			    if(result.success==null||!result.success)
			   {
				   if(result.error!="")
				   {
						 //alert(result.error.msg);
				   }
				   else
				   {
						 //alert("操作失败!");
				   }
				  
				   return;
			   }
			    if(result.data==null||result.data==""||result.data.length==0)
				   {
						//alert("没有找到数据!");
						return;
				   }
			showResult(result);
			
		   });//end getJSON
}
function addIndex2Area(id)
{
	var x = $("#"+id+"").val();
	$("#tindexText").val(x);	
}
</script>

</head>
<body>
 <!-- main begins -->
  
 <div class="col-real-main">
  
  <div class="col-real-left">
   <div class="col-real-left-top">
    <div class="col-real-left-top-left">
     	<input type="button" id="lineButton" name="lineButton" value="线型" onclick="changeModel('spline')"/>
		<input type="button" id="columnButton" name="columnButton" value="列型" onclick="changeModel('column')"/>
		<input type="button" id="areaButton" name="areaButton" value="区域" onclick="changeModel('area')"/>
		
	</div>
	<div class="col-real-left-top-center">
	</div>
	<div class="col-real-left-top-right">
	
		<input type="button" id="baseAnalysisButton" name="baseAnalysisButton" value="基本分析" onclick="showPanel(0)"/>
	   <input type="button" id="realAnalysisButton" name="realAnalysisButton" value="实时分析" onclick="showPanel(1)"/>
	   <input type="button" id="templateAnalysisButton" name="templateAnalysisButton" value="模板分析" style="display:none" onclick="showPanel(2)"/>
	   <input type="button" id="dynaicAnalysisButton" name="dynaicAnalysisButton" value="动态分析" onclick="showPanel(3)"/>
	   <input type="button" id="clearChartsButton" name="clearChartsButton" value="清空图表" onclick="rebuildCharts()"/>
	  
   </div>
   </div><!-- col-center-top ends -->

   <div class="col-real-center-center" id="divCharts" style="height:600px">
  </div> <!-- col-center-center ends -->

  <div id="col-real-center-bottom">
   <div id=colcentable style="overflow:auto">
     <table class="datalist2">
		
		<thead id="thead_dataTable"></thead>
		<tbody id="tbody_dataTable"></tbody>
    </table>
	<div id="indexAnalysisDiv"></div>
   </div>
  </div>  <!-- col-center-bottom ends -->
  </div>     <!-- col-left ends -->
  
 <div class="col-real-right">
  <div class="col-real-right-in">
  <div class="hdg"><h3 id="workTitle">基本分析</h3>
   <input type="checkbox" name="zero_checkBox" id="zero_checkBox" checked=true/><font color="red">去掉0值</font>
	单位:<select name="unit_select" id="unit_select" >	
			<option value='0.01'>%</option>
			<option value='1' selected>无单位</option>
			<option value='1000' >千</option>
			<option value='10000' >万</option>
			<option value='1000000' >百万</option>
			<option value='10000000' >千万</option>
			<option value='100000000' >亿</option>
		</select>
  </div>
  <div id="baseWork">
  数据操作: 
  <input type="button" id="analysisButton" name="analysisButton"value="分析" onclick="analysisEventHandler()"/>
  <br/>
		 
 数据来源:<select name="dataSourceCode" id="dataSource" >	
		 	 <%
		 	 	  String selectdsCode = "";
				  LCEnter lce = LCEnter.getInstance();
				  List<DataSource> dsl = lce.get(StockUtil.getListKeyByDataType(StockConstants.DataSource), StockUtil.getCacheName(StockConstants.common));
				  if(dsl!=null&&dsl.size()>0)
				  {
					  for(int i=0;i<dsl.size();i++)
					  {
						  DataSource ds = dsl.get(i);
						  if(i==0)
						  {
							  selectdsCode =  ds.getDataSourceCode();
							  %>
							  	<option value='<%=ds.getDataSourceCode()%>' selected><%=ds.getDataSourceName() %></option>
							  <%
						  }
						  else
						  {
							  %>
							  	<option value='<%=ds.getDataSourceCode()%>' ><%=ds.getDataSourceName() %></option>
							  <%
						  }
					  }
				  }
				  %>
				</select><br/>
 报表体系:<select name="tableSystemCode" id="tableSystem" >
 				<%
 					if(!StringUtil.isEmpty(selectdsCode))
 					{
 						List<TableSystem> tsl = TableSystemService.getInstance().getTableSystemListByDs(selectdsCode);
 						 if(tsl!=null&&tsl.size()>0)
 						  {
 							  for(int i=0;i<tsl.size();i++)
 							  {
 								 TableSystem ts = tsl.get(i);
 								 if(i==0)
 								  {
 									  
 									  %>
 									  	<option value='<%=ts.getTableSystemCode()%>' selected><%=ts.getTableSystemName() %></option>
 									  <%
 								  }
 								  else
 								  {
 									  %>
 									  	<option value='<%=ts.getTableSystemCode()%>' ><%=ts.getTableSystemName() %></option>
 									  <%
 								  }
 							  }
 						}
 					}
 				%>	
				</select><br/>
  公司名称:<input type="text" name="companyCode" id="div_companyCode"/> <br/>
  
  <div style="clear:both">当前行业:<input type="text" id="companyalltags" />
  
  <input type="button" id="showIndustryButton" name="showIndustryButton"value="显示同行" onclick="createIndustryPanel(0,this)"/><br/>
批(0:否，1：是）：<select id="industry_batch" >
		<option value="0">0</option>
		<option value="1">1</option>
		<option value="2">2</option>
		</select>
  </div>
  <div id="tags_div">
	<ul id="root" class="dropdown" style="z-index:1010">
	</ul>
   </div>
  <br/>

  起始时间:<select name="startTime_year" id="startTime_year" >	
		</select><select name="startTime_jidu" id="startTime_jidu" >	
		<option value='3-31' >一季度</option>
		<option value='6-30' selected>二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-31' >四季度</option>
		</select><br/>
		结束时间:<select name="endTime_year" id="endTime_year" >	
		</select><select name="endTime_jidu" id="endTime_jidu" >	
		<option value='3-31' >一季度</option>
		<option value='6-30' selected>二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-31' >四季度</option>
		</select><br/>	
  
  </div>
  <div id="workDiv_base">
  <!--  
		财务指标:<input type="text" name="div_indexCode" id="div_indexCode" onclick="fetchIndexAndShow(0,this,25,0)"/>
		-->
		财务指标:<input type="text" name="div_indexCode" id="div_indexCode" />
		<input type="button" id="analysisButton_c" name="analysisButton"value="同比" onclick="AnalysisCAndH(0)"/>
		<input type="button" id="analysisButton_h" name="analysisButton"value="环比" onclick="AnalysisCAndH(1)"/><br/>
		<input type="button" id="twoPointAvgButton" name="twoPointAvgButton"value="两点平均" onclick="computeAvg(2)"/>
		<input type="button" id="threePointAvgButton" name="threePointAvgButton"value="三点平均" onclick="computeAvg(3)"/>
		<input type="button" id="fivePointAvgButton" name="fivePointAvgButton"value="五点平均" onclick="computeAvg(5)"/><br/>
  		<input type="button" id="getIndexValueButton_0" name="getIndexValueButton_0"value="最大值" onclick="getIndexSpecialValue(0)"/>
  		<input type="button" id="getIndexValueButton_1" name="getIndexValueButton_1"value="最小值" onclick="getIndexSpecialValue(1)"/>
  		<input type="button" id="getIndexValueButton_2" name="getIndexValueButton_2"value="平均值" onclick="getIndexSpecialValue(2)"/>
  		<input type="button" id="getIndexValueButton_3" name="getIndexValueButton_3"value="中值" onclick="getIndexSpecialValue(3)"/>
  		<input type="button" id="getIndexValueButton_4" name="getIndexValueButton_4"value="行业值" onclick="getIndexSpecialValue(4)"/><br/>
  		<input type="button" id="qhyButton_0" name="qhyButton_0"value="季度" onclick="qhy(0)"/>
  		<input type="button" id="qhyButton_1" name="qhyButton_1"value="半年" onclick="qhy(1)"/>
  		<input type="button" id="qhyButton_2" name="qhyButton_2"value="年化" onclick="qhy(2)"/>
  		<br/>
  </div>
    <div id="workDiv_realtime" style="display:none">
		<form id="realAnalysisForm">
		<input type="hidden" id="realTimeIndexName" name="real.name" value="实时分析指标/模板"/>
		<input type="hidden" id="realTimeIndexCode" name="real.indexCode" />
		<input type="hidden" id="realTimeCompanyList" name="real.companyList"/>
		<input type="hidden" id="realTimeStartTime" name="real.startTime" />
		<input type="hidden" id="realTimeEndTime" name="real.endTime" />
		<input type="hidden" id="realPoints" name="real.points" />
		<input type="hidden" id="realTableSystemCode" name="real.tableSystemCode"/>
		<input type="hidden" id="realDataSourceCode" name="real.dataSourceCode"/>
		时间间隔:<select name="real.interval" id="realtime_realTime_interval">
			<option value='3' selected>3</option>
			<option value='6'>6</option>
			<option value='9'>9</option>
			<option value='12'>12</option>
			<option value='1' >1</option>
			<option value='5' >5</option>
			<option value='7' >7</option>
			<option value='10' >10</option>
			<option value='20' >20</option>
			<option value='30' >30</option>
			<option value='60' >60</option>
			<option value='120' >120</option>
			<option value='250' >250</option>
		</select><br/>
		间隔单位:<select id="timeUnit" name="real.timeUnit" >
			<option value='m' selected>月</option>
			<option value='w' >周</option>
			<option value='d' >日</option>
			<option value='h' >小时</option>
			<option value='f' >分钟</option>
		</select><br/>
		规则类型:<select id="deRuleType" name="real.indextype">
			<option value='1' selected>财务规则指标</option>
			<option value='2'>行情规则指标</option>
		</select>
		<span style='color:red'>实时计算时：间隔，间隔单位，规则类型一定要匹配，系统默认只加载最近三个月的行情数据</span><br/>
		<input type="button" id="clearChartsButton_read" name="clearChartsButton" value="清空数据" onclick="clearData()"/><br/>
		<input type="hidden" id="realTimeDeText_hidden" name="real.ruleComments" value=""/>
		
		</form>	
		<textarea name="real.ruleComments.input" id="realTimeDeText" style="width:100%;height:200px;overflow:scroll" ></textarea>
		
  </div>
  <div id="workDiv_template" style="display:none">
		时间间隔:<select name="temp_interval" id="temp_interval">
			<option value='3' selected>3</option>
			<option value='6' >6</option>
			<option value='9'>9</option>
			<option value='12'>12</option>
		</select><br/>
         模板名称:<select name="div_templateCode" id="div_templateCode">
			<option value='' selected>-----请选择-----</option>
		</select><br/>
		<input type="button" id="clearChartsButton_template" name="clearChartsButton" value="清空数据" onclick="clearData()"/><br/>
		<div id="variableDiv" style="width:100%;height:300px; overflow:scroll; border:1px solid;"></div>
		
  </div>
    <div id="workDiv_dynaic" style="display:none">
		<form id="realAnalysisForm">
		表数据区:<font style="color:red">备注:按行取数据,请直接从excel中把数据贴进来</font><br/>
		<input type="button" id="clearChartsButton_dync" name="clearChartsButton" value="清空数据" onclick="clearData()"/><br/>
		<textarea name="bodyData" id="bodyData" style="width:100%;height:300px;overflow:scroll;overflow-x:scroll" wrap="off" ></textarea>
		</form>				
	</div>
  </div>
  <form id="dataForm">
		<input type="hidden" id="companyList" name="msg.companyList" value=""/>
		<input type="hidden" id="companyCode" name="msg.companyCode" value=""/>
		<input type="hidden" id="startTime" name="msg.startTime" value=""/>
		<input type="hidden" id="endTime" name="msg.endTime" value=""/>
		<input type="hidden" id="indexCode" name="msg.indexCode" value=""/>
		<input type="hidden" id="templateCode" name="msg.templateCode" value=""/>
		<input type="hidden" id="interval" name="msg.interval" value=""/>
		<input type="hidden" id="variableList" name="msg.variableList"/>
		<input type="hidden" id="tableid" name="msg.tableid"/><br/>
		<input type="hidden" id="tableSystemCode" name="msg.tableSystemCode"/>
		<input type="hidden" id="dataSourceCode" name="msg.dataSourceCode"/>
  </form>
  <div id="indexPanel" class="indexPanelCss" style="display:none">
	指标：<input type="text" id="sindex" ><input type="button" value="添加" onclick="addIndex2Area('sindex')" >
</div><!-- indexPanel div-->
  <div id="industry_panel" style="height:200px;display:none;z-index:2;overflow-x:auto;overflow-y:auto">
	 <table class="datalist2" id="industry_panel_table">
    </table>
  </div>
<div class="define" id="defineDiv" style="display:none">
	<table>
	<tr>
	<td>指标:</td><td><input type="text" id="tindexText" name="tindexText" style="width:150px"/></td>
	<td>时间间隔:<select name="de_interval_index" id="de_interval_index" >
			<option value='-24'>-24</option>
			<option value='-21'>-21</option>			
			<option value='-18'>-18</option>		
			<option value='-15'>-15</option>			
			<option value='-12'>-12</option>				
			<option value='-9'>-9</option>				
			<option value='-6'>-6</option>
			<option value='-3'>-3</option>
			<option value='0' selected>0</option>
			<option value='3'>3</option>
			<option value='6'>6</option>
			<option value='9'>9</option>
			<option value='12'>12</option>
			<option value='15'>15</option>
			<option value='18'>18</option>
			<option value='21'>21</option>
			<option value='24'>24</option>	
	</select>
		
	</td><td><input type="button" name="addIndexButton" id="addIndexButton" value="增加" onclick="createIndexVar()" /></td></tr>
	
	<tr><td><input type="button" name="addButton" id="addButton" value="+" onclick="appendS(this.value)"/>
	<input type="button" name="delButton" id="delButton" value="-" onclick="appendS(this.value)"/></td>
	<td><input type="button" name="malButton" id="malButton" value="*" onclick="appendS(this.value)"/>
	<input type="button" name="minusButton" id="minusButton" value="/" onclick="appendS(this.value)"/></td></tr>
	
	<tr><td><input type="button" name="rbracketsButton" id="rbracketsButton" value="(" onclick="appendS(this.value)"/>
	<input type="button" name="lbracketsButton" id="lbracketsButton" value=")" onclick="appendS(this.value)"/></td>	
	
	<td><input type="button" name="Button_0" id="Button_0" value="0" onclick="appendS(this.value)"/>
	<input type="button" name="Button_1" id="Button_1" value="1" onclick="appendS(this.value)"/></td></tr>

	<tr><td><input type="button" name="Button_2" id="Button_2" value="2" onclick="appendS(this.value)"/>
	<input type="button" name="Button_3" id="Button_3" value="3" onclick="appendS(this.value)"/></td>
	<td><input type="button" name="Button_4" id="Button_4" value="4" onclick="appendS(this.value)"/>
	<input type="button" name="Button_5" id="Button_5" value="5" onclick="appendS(this.value)"/></td></tr>	
	
	<tr><td><input type="button" name="Button_6" id="Button_6" value="6" onclick="appendS(this.value)"/>
	<input type="button" name="Button_7" id="Button_7" value="7" onclick="appendS(this.value)"/></td>
	<td><input type="button" name="Button_8" id="Button_8" value="8" onclick="appendS(this.value)"/>
	<input type="button" name="Button_9" id="Button_9" value="9" onclick="appendS(this.value)"/></td></tr>	
	<tr><td><input type="button" name="Button_point" id="Button_point" value="." onclick="appendS(this.value)"/></td><td><input type="button" name="dcButton" id="dcButton" value="隐藏" /></td></tr>
	</table>
</div><!-- define div-->
   </div> <!-- col-right ends --><!-- col-right-in ends -->
 
</div> <!-- main ends -->


 <!-- gf-foot ends -->
    
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

