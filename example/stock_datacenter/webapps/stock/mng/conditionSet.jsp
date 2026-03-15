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
 <link rel="stylesheet" type="text/css" href="css/InvestAnalysis1.css">
 <link href = "css/portal1.css"  type = "text/css" rel="stylesheet"></link>
<script src="js/jquery/jquery-1.6.2.min.js" type="text/javascript"></script>
<script src="js/highcharts/js/highcharts.js" type="text/javascript"></script>
<script type='text/javascript' src='js/jquery-autocomplete/jquery.autocomplete.js'></script>
<script type='text/javascript' src='js/util/Calendar.js'></script>
<script type='text/javascript' src='js/mycharts.js'></script>
<script type='text/javascript' src='js/common.js'></script>
<link rel="stylesheet" type="text/css" href="js/jquery-autocomplete/jquery.autocomplete.css" />
<script type="text/javascript">
var currentChart;//当前图表
var type = 'spline';
var chartOptKey = "chartOpt";
var chartKey = "chart";
var chartChangeKey = "chartChange";
var chartRegionKey = "chartRegion";
var currentIndexText;//当前指标框
var tindex = 0;
var chartDataKey = "chartData";
var chartArray ;
var maxIndex = 0;
$(document).ready(function() {


	
	 initAutoIndexMatcher('indexSelectText');
	 initAutoIndexMatcher('variableIndexText');
	 //初始数据源
	//initDataSource("dataSource");
	 //初始报表体系中基本指标
		queryIndex_V2();
		queryIndex();
	$("#dataSource").change(function(){
		//初始化报表体系
		initTableSystem("dataSource","tableSystem");
	});

	$("#tableSystem").change(function(){
		//初始报表体系中基本指标
		queryIndex_V2();
	});


	$("#indexSelectText").click(function(){
		var v = $("#indexSelectText").val();
		if(v=="请在此处输入指标")
			$("#indexSelectText").val("");
		
	});		
	
	$("#indexSelectText").blur(function(){
		var v = $("#indexSelectText").val();
	
		if(v=="")
			$("#indexSelectText").val("请在此处输入指标");
	});	

	// chart = new myChart();
	$("#testButton").click(function(){
		//setCookie("yz",100);
		getCookie("indexList");
		delCookie("indexList");
	});		
	
	
		$("#tagsSelect").change(function(){
			    clearTagsAllSelect();
				var divArray = document.getElementsByName("tagsindex_div");
				for(var i=0;i<divArray.length;i++)
				{
					var tdiv = divArray[i];
					var id = tdiv.id;
					$("#"+id+"").slideUp();
				}
			if($("#tagsSelect").val()!=-1)
			{
				var selectDivId = $("#tagsSelect").val()+"_div";
				$("#"+selectDivId+"").slideDown();
			}
			
		});	
});//end ready

//自动匹配
function initAutoIndexMatcher(id)
{

		var tsc = $("#tableSystem").val();
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.get("<%=request.getContextPath()%>/cfirule/getCfiruleListByTscid_V2?msg.tableSystemCode="+tsc, function(result){
				if(result.data==null)
				{
					return;
				}
				var data = result.data.split(";");
				if(data!=null)
				{
					c_data = data;

					$("#"+id+"").autocomplete(c_data, {
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
function clearTagsAllSelect()
{
	
	var sindexArray = document.getElementsByName("tagtype_indexcheckbox");
	for(var i=0;i<sindexArray.length;i++)
		{
			var id = sindexArray[i].id;
			if($("#"+id+"").attr('checked')=='checked')
			{
				$("#"+id+"").removeAttr("checked");
			}
		}
}
function removeFromCookie(utext)
{
	var name = "indexList";
	var indexList = getCookie(name);
	if(indexList!=null)
	{
		//如果不存在，则加上
		if(indexList.indexOf(utext)!=-1)
		{
			indexList = indexList.replace(utext+"|","");
		}
		  
	}
	
	setCookie(name,indexList);
}
function showMyIndex()
{
	var divArray = document.getElementsByName("tagsindex_div");
				for(var i=0;i<divArray.length;i++)
				{
					var tdiv = divArray[i];
					var id = tdiv.id;
					$("#"+id+"").slideUp();
				}

				$("#myIndexDiv").slideDown();
}





//初始数据源 did 是数据源选择框的id
var dataSourceData = null;
function initDataSource(did)
{
	 	  
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
		//先清空
	   	$("#"+did+"").empty();
		var i =0;
	  	for(i in data)
		{
	  			//分隔指标编码与名字
	  		var ds = data[i];
	  		var dataSourceCode = ds.dataSourceCode;
	  		var dataSourceName = ds.dataSourceName;
			if(i==0)
			{
				$("#"+did+"").append("<option value='"+dataSourceCode+"' selected>"+dataSourceName+"</option>");
			}
			else
			{
				$("#"+did+"").append("<option value='"+dataSourceCode+"'>"+dataSourceName+"</option>");
			}
			
		}
		//setSelect(did,0);
		//初始化报表体系
		initTableSystem("dataSource","tableSystem");
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
		//初始化指标面板
		queryIndex_V2();
	}
}


function getSelectTableName()
{
	var tableid = $("#head_table_select").val();
	var tableName = "t_asset";
	if(tableid==0)
	{
		tableName = "t_asset:t_indebted_owner";	
	}
	if(tableid==1)
	{
		tableName = "t_cash_flow:t_cash_flow_attacheed";	
	}
	if(tableid==2)
	{
		tableName = "t_profile";	
	}
	if(tableid==3)
	{
		tableName = "t_c_base_financial_index";	
	}
	if(tableid==4)
	{
		tableName = "t_c_ext_index";	
	}
	return tableName;
}
//按表名把指标追加到不同的面板上
function showChartByTableId(ns,index,selectType,type,companyCode,tIndexCode,startTime,endTime)
{
	//资产表
	if(selectType==0&&(type==0||type==1))
	{
		loadData2Chart(ns,index,companyCode,tIndexCode,startTime,endTime);	
		return;
	}
	//利润表
	if(selectType==2&&(type==2||type==3))
	{
		loadData2Chart(ns,index,companyCode,tIndexCode,startTime,endTime);	
		return;
	}
	//或是基本表
	//资产表
	if(selectType==type)
	{
		loadData2Chart(ns,index,companyCode,tIndexCode,startTime,endTime);	
		return;
	}
	return;
	
}



function showIndexPanel(id,source)
{
		//获取$("#deText")的位置
		var ntop =  realOffset(source).y+25;
		var nleft =  realOffset(source).x;
		var id = "#"+id+"";
		$(id).css({'position':'absolute','top':ntop,'left':nleft,'z-index':2});
		$(id).slideDown();
		$(id).focus();
		//当前发生事件的指标输入框
		currentIndexText=source;
}
//取对象的绝对位置
function realOffset(o)
{
  var x = y = 0; 
  do{
  x += o.offsetLeft || 0; 
  y += o.offsetTop || 0;
  o = o.offsetParent;
  }while(o);
  return {"x" : x, "y" : y};
}

function clearObjectData(id)
{
	$("#"+id+"").val("");
}

//自动匹配公司名
function initCompanyAutoMatcher(aa)
{

		var tsc = $("#tableSystem").val();
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.get("<%=request.getContextPath()%>/company/getCompanyByTableSystemString?msg.tableSystemCode="+tsc, function(result){
				if(result.data==null)
				{
					return;
				}
				var data = result.data.split(";");
				if(data!=null)
				{
					c_data = data;

					$("#head_companyCode").autocomplete(c_data,{
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



//============================================================indexpanel 指标面板相关方法========================
//隐藏指标面板
function hiddenIndexPanel()
{
	$('#indexPanel').slideUp();
}
//取公司对应的指标
 var indexArrayHtml = new Array();//存数据源索引与指标列表的映射
function queryIndex_V2()
{
	//取数据源编码
	var tsCode = $("#tableSystem").val();
	//缓存各数据源对应的指标,按下拉框索引
	var dindex =$("#tableSystem").get(0).selectedIndex;
	var indexHtml = indexArrayHtml[dindex];
	if(isNull(indexHtml))
	{
		$.get("<%=request.getContextPath()%>/cfirule/getTagsCfiruleListByTid?msg.tableSystemCode="+tsCode, function(result){
		  
			var data = result.data.split(";");
			if(data!="")
			{
				$("#tagsSelect").empty();
				$("#tagsSelect").append("<option value='-1' selected>选择指标分类</option>");
				var i=0;
			  	for(i in data)
				{
			  	//分隔指标编码与名字
			  		var ia = data[i].split(":");
			  		var tags = ia[0];
			  		//加分类
			  		var tagName = tags.split("_")[0];
			  		var tagCode = tags.split("_")[1];
			  		var tagtext = "<option value='"+tagCode+"' title='"+tagName+"'>"+tagName+"</option>";
			  		$("#tagsSelect").append(tagtext);
			  		var tagDivid = tagCode+"_div";
			  		$("#tagsIndexSelectPanel").append("<div id='"+tagDivid+"' name='tagsindex_div' style='display:none'></div>");
			  		
			  		var indexList = ia[1];
					
			  		var indexArray = indexList.split("|");
			  		for(var j=0 ;j<indexArray.length;j++)
			  			{
				  			//分隔指标编码与名字
					  		var ia = indexArray[j].split("_");
					  		var indexCode = ia[0];
					  		var indexName = ia[1];
							var tableName = ia[2];
							var type = ia[3];
							if(indexName!=null)
							{
								var tindexname = indexName;
								if(indexName.length>10) indexName = indexName.substring(0,10);
								//按表名把指标追加到不到的面板上
								var utext = tindexname+":"+indexCode;
								var checkHtml = "<input type='checkbox' name='tagtype_indexcheckbox' id='tag_"+ indexCode + "' value='"+utext+"' onclick=handleIndex('"+utext+"') />"+indexName+" ";
								$("#"+tagDivid+"").append(checkHtml);
							}
			  			}
	
				}
				
			}
	  });	
	}
	
	  
}

//取公司对应的指标
 var indexArrayHtml1 = new Array();//存数据源索引与指标列表的映射
function queryIndex()
{
	//取数据源编码
	var tsCode = $("#tableSystem").val();
	//缓存各数据源对应的指标,按下拉框索引
	var dindex =$("#tableSystem").get(0).selectedIndex;
	var indexHtml = indexArrayHtml1[dindex];
	if(isNull(indexHtml))
	{
		$.get("<%=request.getContextPath()%>/cfirule/getCfiruleListByTid?msg.tableSystemCode="+tsCode, function(result){
		    //先清空
			$("#assetSelect").empty();
			 //先清空
			$("#cashFlowSelect").empty();
			 //先清空
			$("#profileSelect").empty();
			 //先清空
			$("#extIndexSelect").empty();
				
			var data = result.data.split(";");
			if(data!="")
			{
				var i=0;
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
				//缓存一份html
				indexArrayHtml1[dindex] = $("#indexPanel").html();
			}
	  });	
	}
	else
	{
		$("#indexPanel").empty();
		$("#indexPanel").append(indexHtml);
	}
	  
}

function handleMyIndex(utext)
{
	var indexCode = "myindex_"+utext.split(":")[1];
	var ischecked = $("#"+indexCode+"").attr("checked")=="checked";
	//如果是选中状态，但没有添加到下面的框中，则添加到选择区
	if(ischecked)
	{
		 //取所有选择的指标
		var indexSelectArray = document.getElementsByName('indexcheckbox');
		var tindex = utext;
		var i=0;
		for(i=0;i<indexSelectArray.length;i++)
		{
				var os = indexSelectArray[i];
				if(os.value==tindex)
				{
					alert(tindex+" :指标已添加,请您选择其它指标!");
					return;
				}
		}

			
		var html="<span id='select_"+indexCode+"_span'><input type='checkbox' name='indexcheckbox' value='"+tindex+"' id='select_"+indexCode+"' checked='true'>"+tindex+"  </span>";
		$("#indexSelectDiv").append(html);
	}
	else
	{
		//如果是没选中状态下，则删除选择区中的指标
		var indexSelectArray = document.getElementsByName('indexcheckbox');
		var tindex = utext;
		var i=0;
		for(i=0;i<indexSelectArray.length;i++)
		{
				var os = indexSelectArray[i];
				if(os.value==tindex)
				{
					$("#"+os.id+"_span").remove();
					return;
				}
		}
	}

}


function handleIndex(utext)
{
	var indexCode = "tag_"+utext.split(":")[1];
	var ischecked = $("#"+indexCode+"").attr("checked")=="checked";
	//如果是选中状态，但没有添加到下面的框中，则添加到选择区
	if(ischecked)
	{
		 //取所有选择的指标
		var indexSelectArray = document.getElementsByName('indexcheckbox');
		var tindex = utext;
		var i=0;
		for(i=0;i<indexSelectArray.length;i++)
		{
				var os = indexSelectArray[i];
				if(os.value==tindex)
				{
					alert(tindex+" :指标已添加,请您选择其它指标!");
					return;
				}
		}

			
		var html="<span id='select_"+indexCode+"_span'><input type='checkbox' name='indexcheckbox' value='"+tindex+"' id='select_"+indexCode+"' checked='true'>"+tindex+"  </span>";
		$("#indexSelectDiv").append(html);
		//updateIndexCookie(utext);
	}
	else
	{
		//如果是没选中状态下，则删除选择区中的指标
		var indexSelectArray = document.getElementsByName('indexcheckbox');
		var tindex = utext;
		var i=0;
		for(i=0;i<indexSelectArray.length;i++)
		{
				var os = indexSelectArray[i];
				if(os.value==tindex)
				{
					$("#"+os.id+"_span").remove();
					return;
				}
		}
	}

}

//构建请求消息
function buildReqMsg(ns,index)
{
	//公司编码
	$("#companyCode").val($("#"+ns+"_companyCode_"+index).val());
	$("#companyList").val($("#companyCode").val());
	//指标编码
	$("#indexCode").val($("#"+ns+"_indexCode_"+index).val().split(":")[1]);
	var startTime = $("#"+ns+"_startTime_year_"+index).val()+"-"+$("#"+ns+"_startTime_jidu_"+index).val();
	//超始时间
	$("#startTime").val(startTime);
	var endTime = $("#"+ns+"_endTime_year_"+index).val()+"-"+$("#"+ns+"_endTime_jidu_"+index).val();
	//结束时间
	$("#endTime").val(endTime);
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
		return "开始时间必须小于结束时间!"
	}
	if($("#indexCode").val()=="")	
	{
		return "指标编码不可为空!";
	}
	return "";
}

function compareDate(startDate,endDate) {   
    var startMonth = startDate.substring(5,startDate.lastIndexOf ("-"));  
    var startDay = startDate.substring(startDate.length,startDate.lastIndexOf ("-")+1);  
    var startYear = startDate.substring(0,startDate.indexOf ("-"));  
  
    var endMonth = endDate.substring(5,endDate.lastIndexOf ("-"));  
    var endDay = endDate.substring(endDate.length,endDate.lastIndexOf ("-")+1);  
    var endYear = endDate.substring(0,endDate.indexOf ("-"));  
      
    if (Date.parse(startMonth+"/"+startDay+"/"+startYear) >  
        Date.parse(endMonth+"/"+endDay+"/"+endYear)) {  
        return true;  
    }  
    return false;  
} 


//多个指标选择框的编码
var textCode = 0;
//取选择面板中,所选择的指标
function getIndexPanelSelectValue(o)
{
	if(o.selectedIndex!=-1)
	{
		var text = o.options[o.selectedIndex].text;
		var x=text+":"+o.value;
		$("#"+currentIndexText.id+"").val(x);
	}
	
}




//把指标添加到选择面板
function addIndexToSelectDiv()
{
	 //取所有选择的指标
	var indexSelectArray = document.getElementsByName('indexcheckbox');
	var tindex = $("#indexSelectText").val();
	if(tindex==""||tindex=="请在此处输入指标")
	{
		alert("请输入指标！");
		return;
	}
	var i=0;
	for(i=0;i<indexSelectArray.length;i++)
	{
			var os = indexSelectArray[i];
		    if(os.value==tindex)
			{
				alert(tindex+" :指标已添加,请您选择其它指标!");
				return;
			}
	}
	
	var html="<input type='checkbox' name='indexcheckbox' value='"+tindex+"' checked='true'>"+tindex+"  ";
	$("#indexSelectDiv").append(html);
	updateIndexCookie(tindex)
}

function clearAllSelect()
{
	$("#indexSelectDiv").empty();
	$("#workRegion").empty();

	if($("#condition_tbody_dataTable").children().length>0)
		{
			var flag = confirm("是否清空条件设置区！如果没保存条件设置结果，请先保存！");
			 if(!flag)
			{
				 return;
			}
			else
			{
				$("#condition_tbody_dataTable").empty();
			}
		}
}
function createConditionDiv()
{
		   //取所有选择的指标
	var indexSelectArray = document.getElementsByName('indexcheckbox');
	//如果为空,则取指标选择输入框中的指标
	if(isNull(indexSelectArray)||indexSelectArray.length==0)
	{
		alert("选择的指标不可以为空!");
		return;
	}
	else
	{
		if($("#condition_tbody_dataTable").children().length>0)
		{
			var flag = confirm("是否重新生成条件设置区！如果没保存条件设置结果，请先保存！");
			 if(!flag)
			{
				 return;
			}
			else
			{
				$("#condition_tbody_dataTable").empty();
			}
		}
		var i=0;
		for(i=0;i<indexSelectArray.length;i++)
		{
			var os = indexSelectArray[i];
			//如果是选中的,则展示
			if(os.checked==true)
			{
				createOneConditionDiv(os.value,i);
			}
		}
	}
}
//数据含义，,0,1,0)'  依次为：（变量，同比，环比），（是，否，a,b），（新增区，查询区）
function createOneConditionDiv(uindex,i)
{
	var indexname = uindex.split(":")[0];
	var indexcode = uindex.split(":")[1];
	var html = "";
	html="<tr id='tr_"+i+"_0' name='condition_tr_0'>";
	
	html+="<td  style='width:5%' ><input id='name_"+i+"_0_0' value='"+indexname+"'/></td>";
	html+="<td><textarea style='width:100%' id='a_"+i+"_0_2_0' >\${"+indexname+":"+indexcode+"}</textarea><br/>";
	html+="<input type='button' onclick='showVariable(this,"+i+",0,2,0)' value='变'/>";
	html+="<input type='button' onclick='clearMsg("+i+",0,2,0)' value='清'/></td>";

	html+="<td><select id='condition_"+i+"_0_0'><option value='>='>>=</option><option value='<='><=</option><option value='>'>></option><option value='<'><</option></select></td>";
	html+="<td><textarea style='width:100%' id='a_"+i+"_0_3_0'  ></textarea><br/>";
	html+="<input type='button' onclick='showVariable(this,"+i+",0,3,0)' value='变'/>";
	html+="<input type='button' onclick='clearMsg("+i+",0,3,0)' value='清'/></td>";
	

	html+="<td><textarea style='width:100%' id='a_"+i+"_0_0_0' ></textarea><br/>";
	html+="<input type='button' onclick='showVariable(this,"+i+",0,0,0)' value='变'/>";
	html+="<input type='button' onclick='showHisMsg(this,"+i+",0,0,0)' value='常'/>";
	html+="<input type='button' onclick='clearMsg("+i+",0,0,0)' value='清'/></td>";

	html+="<td><textarea style='width:100%' id='a_"+i+"_0_1_0' ></textarea><br/>";
	html+="<input type='button' onclick='showVariable(this,"+i+",0,1,0)' value='变'/>";
	html+="<input type='button' onclick='showHisMsg(this,"+i+",0,1,0)' value='常'/>";
	html+="<input type='button' onclick='clearMsg("+i+",0,1,0)' value='清'/></td>";
	//html+="<tr id='tr_"+indexcode+"_1' name='condition_tr'><td>"+indexname+"</td><td>< </td><td><input type='text' id='region_"+indexcode+"_1' size='5px'/></td><td><textarea style='width:400px' id='msg_"+indexcode+"_1' /><input type='button' onclick='showVariable(this,"+indexcode+",1)' value='变'/><input type='button' onclick='showHisMsg(this,"+indexcode+",1)' value='常'/><input type='button' onclick='clearMsg("+indexcode+",1)' value='清'/></td>";

	if($("#iscreatehd").attr('checked')=='checked')
	{
		
		html+="<tr id='tr_"+i+"_2' name='condition_tr_0'>";
		html+="<td  style='width:5%' ><input id='name_"+i+"_2_0' value='"+indexname+"--同比'/></td>";
		html+="<td><textarea style='width:100%' id='a_"+i+"_2_2_0' >\$thc(\${"+indexname+":"+indexcode+"},0)</textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+",2,2,0)' value='变'/>";
		html+="<input type='button' onclick='clearMsg("+i+",2,2,0)' value='清'/></td>";

		html+="<td><select id='condition_"+i+"_2_0'><option value='>='>>=</option><option value='<='><=</option><option value='>'>></option><option value='<'><</option></select></td>";
		html+="<td><textarea style='width:100%' id='a_"+i+"_2_3_0'/><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+",2,3,0)' value='变'/>";
		html+="<input type='button' onclick='clearMsg("+i+",2,3,0)' value='清'/></td>";


		html+="<td><textarea style='width:100%'  id='a_"+i+"_2_0_0' ></textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+",2,0,0)' value='变'/>";
		html+="<input type='button' onclick='showHisMsg(this,"+i+",2,0,0)' value='常'/>";
		html+="<input type='button' onclick='clearMsg("+i+",2,0,0)' value='清'/></td>";

		html+="<td><textarea style='width:100%' id='a_"+i+"_2_1_0' ></textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+",2,1,0)' value='变'/>";
		html+="<input type='button' onclick='showHisMsg(this,"+i+",2,1,0)' value='常'/>";
		html+="<input type='button' onclick='clearMsg("+i+",2,1,0)' value='清'/></td>";
		
		html+="<tr id='tr_"+i+"_3' name='condition_tr_0'>";
		html+="<td  style='width:5%' ><input id='name_"+i+"_3_0' value='"+indexname+"--环比'/></td>";
		html+="<td><textarea style='width:100%' id='a_"+i+"_3_2_0' >\$thc(\${"+indexname+":"+indexcode+"},1)</textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+",3,2,0)' value='变'/>";
		html+="<input type='button' onclick='clearMsg("+i+",3,2,0)' value='清'/></td>";

		html+="<td><select id='condition_"+i+"_3_0'><option value='>='>>=</option><option value='<='><=</option><option value='>'>></option><option value='<'><</option></select></td>";
		html+="<td><textarea style='width:100%' id='a_"+i+"_3_3_0' ></textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+",3,3,0)' value='变'/>";
		html+="<input type='button' onclick='clearMsg("+i+",3,3,0)' value='清'/></td>";

		html+="<td><textarea style='width:100%' id='a_"+i+"_3_0_0' ></textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+",3,0,0)' value='变'/>";
		html+="<input type='button' onclick='showHisMsg(this,"+i+",3,0,0)' value='常'/>";
		html+="<input type='button' onclick='clearMsg("+i+",3,0,0)' value='清'/></td>";

		html+="<td><textarea style='width:100%' id='a_"+i+"_3_1_0' ></textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+",3,1,0)' value='变'/>";
		html+="<input type='button' onclick='showHisMsg(this,"+i+",3,1,0)' value='常'/>";
		html+="<input type='button' onclick='clearMsg("+i+",3,1,0)' value='清'/></td>";

	}
	$("#condition_tbody_dataTable").append(html);
}
//type:指标，同比，环比，sf:是否，AB，cq：创建区，查询区
function showVariable(source,indexcode,type,sf,cq)
{
	clearSelectRadioButton();
	var nv = indexcode+"_"+type+"_"+sf+"_"+cq;
	var ov = $("#vCurrentTr").val();
	if($("#variableBuildDiv").is(":visible")&&nv==ov)
	{
		$("#variableBuildDiv").slideUp();
	}
	else
	{
		showIndexPanel('variableBuildDiv',source);
	}

	$("#his_desc").hide();
	$("#vCurrentTr").val(nv);
}
function showHisMsg(source,indexcode,type,sf,cq)
{
	var nv = indexcode+"_"+type+"_"+sf+"_"+cq;
	var ov = $("#msgCurrentTr").val();
	if($("#his_desc").is(":visible")&&nv==ov)
	{
		$("#his_desc").slideUp();
	}
	else
	{
		showIndexPanel('his_desc',source);
	}
	
	$("#variableBuildDiv").hide();

	$("#msgCurrentTr").val(nv);
}
function createIndexVariable()
{
	var nindex = $("#variableIndexText").val();
	if(nindex=="")
	{
		alert("指标不可以为空！");
		$("#variableIndexText").focus();
		return;
	}
	$("#indexVariable").empty();
	var t = $("#v_interval_index").val();
	
	var vi = "\${"+nindex+","+t+"}";
	if(t=='0')
		vi = "\${"+nindex+"}";
	
	var rtype = -1;
	var ra = document.getElementsByName("ff");
	for(var i=0;i<ra.length;i++)
	{
		var r = ra[i];
		if(r.checked==true)
		{
			rtype = r.value;
			break;
		}
	}

	if(rtype>0)
		vi = getExpressBytype(rtype,nindex);
	$("#indexVariable").append(vi);

	appendS(vi,"a_"+$("#vCurrentTr").val());
}
function appends2S(v)
{
	appendS(v,"a_"+$("#vCurrentTr").val());
}
function getExpressBytype(rtype,nindex)
{
	if(rtype==0) return "\$thc('"+nindex+"',0)";
	if(rtype==1) return "\$thc("+nindex+",1)";
	if(rtype==2) return "\$avg("+nindex+",-2)";
	if(rtype==3) return "\$avg("+nindex+",-3)";
	if(rtype==4) return "\$avg("+nindex+",-5)";
	if(rtype==5) return "\$qhy("+nindex+",0)";
	if(rtype==6) return "\$qhy("+nindex+",1)";
	if(rtype==7) return "\$qhy("+nindex+",2)";
	
}
//type:指标，同比，环比，sf:是否，AB，cq：创建区，查询区
function mshowVariable(source,id)
{
	clearSelectRadioButton();
	var nv = id;
	var ov = $("#vCurrentTr").val();
	if($("#variableBuildDiv").is(":visible")&&nv==ov)
	{
		$("#variableBuildDiv").slideUp();
	}
	else
	{
		showIndexPanel('variableBuildDiv',source);
	}

	$("#his_desc").hide();
	$("#vCurrentTr").val(nv);
}
function mshowHisMsg(source,id)
{
	var nv = id;
	var ov = $("#msgCurrentTr").val();
	if($("#his_desc").is(":visible")&&nv==ov)
	{
		$("#his_desc").slideUp();
	}
	else
	{
		showIndexPanel('his_desc',source);
	}
	
	$("#variableBuildDiv").hide();

	$("#msgCurrentTr").val(nv);
}

function clearSelectRadioButton()
{
	var ra = document.getElementsByName("ff");
	for(var i=0;i<ra.length;i++)
	{
		var r = ra[i];
	    r.checked=false;
	}
}
//定义规则
var exp = "";//规则表达式
function appendS(o,vid)
{
	var dst ;
	//区分事件发生在哪个面板

	var id = vid;
	dst = $("#"+id+"");
	//光标的起始位置
	var cx =0;
	var variable = o;
	if(exp=="")
	{
		exp+=variable;
	}
	else
	{
		//当输入区与缓存不相等时，则认为用户做了删除操作，则以输入区内容为准
		if(dst.val()!=exp)
		{
			exp = dst.val();
		}

		//计算字符串的插入位置,并把新串插入到老串中
	    cx = dst[0].selectionStart;
		if(exp.length>cx+1)
		{
			exp = exp.substring(0,cx)+variable+exp.substring(cx,exp.length);
		}
		else
		{
			exp+=variable;
		}
		
	}	
	dst.val(exp);
	//重置光标位置
	dst[0].selectionStart = cx+variable.length;
}
function appendHisS(s)
{
	appendS(s,"a_"+$("#msgCurrentTr").val());
}
function clearMsg(indexcode,type,sf,cq)
{
	var id = "a_"+indexcode+"_"+type+"_"+sf+"_"+cq;
	$("#"+id+"").val("");
}
function mclearMsg(indexcode,id)
{
	$("#a_"+id+"").val("");
}
function saveCondition()
{
		var msgs = "";
		var ctra = document.getElementsByName('condition_tr_0');
		var i=0;
		for(i=0;i<ctra.length;i++)
		{
			var os = ctra[i];//tr
			var index = os.id.split("_")[1];
			var type = os.id.split("_")[2];
			var indexname = $("#name_"+index+"_"+type+"_0").val();

			var name = indexname;
			var a = $("#a_"+index+"_"+type+"_2_0").val();
			var c = $("#condition_"+index+"_"+type+"_0").val();
			var b = $("#a_"+index+"_"+type+"_3_0").val();

			var msg_0 =  $("#a_"+index+"_"+type+"_0_0").val();
			var msg_1 =  $("#a_"+index+"_"+type+"_1_0").val();

			if(a==""||b==""||msg_0==""||msg_1=="")
				continue;

			if(a==null||b==null||msg_0==null||msg_1==null)
				continue;

			a = buildab(a);
			b = buildab(b);
			var condition_desc = a+c+b;
			msgs +=name+"|"+condition_desc+"|"+msg_0+"|"+msg_1+"&";
		}

		if(msgs=="")
			return;

	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/condition/savecondition?type=0",{"msgs":msgs}, function(result){
			alert("操作结束！");
			
	  });

}
function buildab(b)
{
	if(b.indexOf("+")>0||b.indexOf("-")>0||b.indexOf("*")>0||b.indexOf("/")>0)
		b = "("+b+")";
	return b;
}
function saveCondition_v2()
{
		var type =0;
		var msgs = "";
		var ctra = document.getElementsByName('condition_tr_1');
		var i=0;
		for(i=0;i<ctra.length;i++)
		{
			var os = ctra[i];//tr
			var index = os.id.split("_")[1];
			var indexname = $("#name_"+index+"_"+type+"_0_1").val();

			var name = indexname;
			var condition_desc = $("#a_"+index+"_"+type+"_2_1").val();
			var msg_0 =  $("#a_"+index+"_"+type+"_0_1").val();
			var msg_1 =  $("#a_"+index+"_"+type+"_1_1").val();

			if(name==""||condition_desc==""||msg_0==""||msg_1=="")
				continue;
			
			if(name==null||condition_desc==null||msg_0==null||msg_1==null)
				continue;

			msgs +=name+"|"+condition_desc+"|"+msg_0+"|"+msg_1+"&";
		}

		if(msgs=="")
			return;

	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/condition/savecondition?type=0",{"msgs":msgs}, function(result){
			alert("操作结束！");
			//queryCompanysOfTag();
	  });

}

function saveCondition_v3()
{
	
		var msgs = "";

		var name = $("#m_name").val();
		var condition_desc = $("#a_m_condition").val();
		var msg_0 =  $("#a_m_msg_0").val();
		var msg_1 =  $("#a_m_msg_1").val();

		if(name==""||condition_desc==""||msg_0==""||msg_1=="")
			return;
		
		if(name==null||condition_desc==null||msg_0==null||msg_1==null)
			return;

		msgs =name+"|"+condition_desc+"|"+msg_0+"|"+msg_1;

	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/condition/savecondition?type=0",{"msgs":msgs}, function(result){
			alert("操作结束！");
			
	  });

}

function queryCondition()
{
	var name = $("#msg_name").val();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("<%=request.getContextPath()%>/condition/getconditionBytype?name="+name+"&type=0", function(result){
			if(result.success)
			{
				var d = result.data;
				if(d!=null)
				{
					showCResult(d);
				}
			}
			else
			{
					alert("查询失败！");
			}

	  });
}
function showCResult(d)
{
	$("#q_condition_tbody_dataTable").empty();
	for(var i=0;i<d.length;i++)
	{
		var mc = d[i];
		var name = mc.name;
		var msg_0 = mc.msg0;
		var msg_1 = mc.msg1;
		var cdesc = mc.conditionDesc;
		var type = 0;
		var html = "";
		html="<tr id='tr_"+i+"_0' name='condition_tr_1'>";
		html+="<td style='width:5%' ><input id='name_"+i+"_"+type+"_0_1' value='"+name+"'/></td>";

		html+="<td><textarea style='width:100%' id='a_"+i+"_"+type+"_2_1'>"+cdesc+"</textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+","+type+",2,1)' value='变'/>";
		html+="<input type='button' onclick='clearMsg("+i+","+type+",2,1)' value='清'/></td>";

		html+="<td><textarea style='width:100%' id='a_"+i+"_"+type+"_0_1' >"+msg_0+"</textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+","+type+",0,1)' value='变'/>";
		html+="<input type='button' onclick='showHisMsg(this,"+i+","+type+",0,1)' value='常'/>";
		html+="<input type='button' onclick='clearMsg("+i+","+type+",0,1)' value='清'/></td>";

		html+="<td><textarea style='width:100%' id='a_"+i+"_"+type+"_1_1' >"+msg_1+"</textarea><br/>";
		html+="<input type='button' onclick='showVariable(this,"+i+","+type+",1,1)' value='变'/>";
		html+="<input type='button' onclick='showHisMsg(this,"+i+","+type+",1,1)' value='常'/>";
		html+="<input type='button' onclick='clearMsg("+i+","+type+",1,1)' value='清'/></td>";
		
		$("#q_condition_tbody_dataTable").append(html);

	}
}
function resetInterval()
{
	$("#v_interval_index").val(0);
}
function clearqdiv()
{
	$("#q_condition_tbody_dataTable").empty();
}
</script>
</head>
<body>
<div style="text-align:left">
<!--指标面板 -->
   <div id="indexPanel" style="display:none;background:#EAF2D3">
	<table>
		<tr>
			<td>资产表:</td>
			<td><select name="assetSelect" id="assetSelect" onclick="getIndexPanelSelectValue(this)" style="width:150px"></select></td>
		</tr>
		<tr>
			<td>现金流量表:</td>
			<td><select name="cashFlowSelect" id="cashFlowSelect" onclick="getIndexPanelSelectValue(this)" style="width:150px"></select></td>
		</tr>

		<tr>
			<td>利润表:</td>
			<td><select name="profileSelect" id="profileSelect" onclick="getIndexPanelSelectValue(this)" style="width:150px"></select></td>
		</tr>
		<tr>
			<td>扩展指标:</td>
			<td><select name="extIndexSelect" id="extIndexSelect" onclick="getIndexPanelSelectValue(this)" style="width:150px"></select></td>
		</tr>
		<tr><td><!--<input type="button" id="refreshIndex" value="刷新" onclick="queryIndex_V2()"/>--><input type="button" id="indexSelect" value="隐藏" onclick="hiddenIndexPanel()"/></td></tr>
	</table>
</div><!-- indexPanel div-->

 <div id="industry_panel" style="height:200px;display:none;z-index:2;overflow-x:auto;overflow-y:auto">
	 <table class="datalist2" id="industry_panel_table">
    </table>
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
		<input type="hidden" id="tableid" name="msg.tableid"/>
		<input type="hidden" id="tableSystemCode" name="msg.tableSystemCode"/>
		<input type="hidden" id="dataSourceCode" name="msg.dataSourceCode"/>
  </form>
		<div id="head_opt_div">
			
			 数据来源:<select name="rule.dataSourceCode" id="dataSource" >	
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
				 
 报表体系:<select name="rule.tableSystemCode" id="tableSystem" >
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
				</select>
		<br/>
	
		
	
		
		选择指标:<select name="tagsSelect" id="tagsSelect" >	
		</select>
		<input type="text" name="indexSelectText" id="indexSelectText" value="请在此处输入指标" />
		<input type="button" id="addIndexButton" name="addIndexButton" value="添加指标到批量分析区" onclick="addIndexToSelectDiv()"/>

		<div id="tagsIndexSelectPanel"></div>
			
		</div><br/>
		指标选择区:<div id="indexSelectDiv"></div><br/>
		<input type="button" id="createConditionButton" name="createConditionButton" value="生成条件输入区" onclick="createConditionDiv()"/>
		<input type="button" id="clearAllSelect" name="clearAllSelect" value="清空" onclick="clearAllSelect()"/>
		<input type='checkbox' id="iscreatehd" /><span style='color:red'>是否生成同比|环比</span>
		<input type="button" id="saveCondition" name="saveCondition" value="保存" onclick="saveCondition()"/>
		<div id="div_condition_table" style="overflow:auto;height:auto">
					 <table class="datalist2">
						<thead id="condition_thead_dataTable">
						<tr>
							<td>指标名</td>
							<td>A</td>
							<td>条件</td>
							<td>B</td>
							<td>是</td>
							<td>否</td>
						</tr>
						</thead>
						<tbody id="condition_tbody_dataTable"></tbody>
					 </table>
		</div>
	
	<br/><br/><br/>
	复合条件设置区：==============================================================================================================================</br>
	<input type="button" id="saveCondition2" name="saveCondition2" value="保存" onclick="saveCondition_v3()"/>
	<div id="div_mcondition_table" style="overflow:auto;height:auto">
					 <table class="datalist2">
						<thead id="mcondition_thead_dataTable">
						<tr>
							<td>指标名</td>
							<td>条件</td>
							<td>是</td>
							<td>否</td>
						</tr>
						</thead>
						<tbody id="mcondition_tbody_dataTable">
						<tr>
							<td style="width:5%">
								<input type="text" id="m_name"/>
							</td>
							<td>
								<textarea style='width:100%' id="a_m_condition"></textarea><br/>
								<input type="button"  value="变" onclick="mshowVariable(this,'m_condition')"/>
								<input type="button"  value="清" onclick="mclearMsg(this,'m_condition')"/>
							</td>
							<td>
								<textarea style='width:100%' id="a_m_msg_0"></textarea><br/>
								<input type="button"  value="变" onclick="mshowVariable(this,'m_msg_0')"/>
								<input type="button"  value="常" onclick="mshowHisMsg(this,'m_msg_0')"/>
								<input type="button"  value="清" onclick="mclearMsg(this,'m_msg_0')"/>
							</td>
							<td><textarea style='width:100%' id="a_m_msg_1"></textarea><br/>
								<input type="button"  value="变" onclick="mshowVariable(this,'m_msg_1')"/>
								<input type="button"  value="常" onclick="mshowHisMsg(this,'m_msg_1')"/>
								<input type="button"  value="清" onclick="mclearMsg(this,'m_msg_1')"/>
							</td>
						</tr>
						</tbody>
					 </table>
		</div>

	<br/><br/><br/>
	查询区：==============================================================================================================================</br>
	条件名：<input type="text" id='msg_name' /><input type="button" id="querycButton" onclick="queryCondition()" value="查询"/>
	<input type="button" id="saveCondition2" name="saveCondition2" value="保存" onclick="saveCondition_v2()"/>
	<input type="button" id="clearqdiv" name="clearqSelect" value="清空" onclick="clearqdiv()"/>
	<div id="q_condition_table" style="overflow:auto;height:auto">
					 <table class="datalist2">
						<thead id="q_condition_thead_dataTable">
						<tr>
							<td>指标名</td>
							<td>条件</td>
							<td>是</td>
							<td>否</td>
						</tr>
						</thead>
						<tbody id="q_condition_tbody_dataTable"></tbody>
					 </table>
	</div>
<div id="variableBuildDiv" style="border-style:solid; border-width:1px;width:200px;display:none;background:gray">
	指标:<input type="text" name="variableIndexText" id="variableIndexText"  /></br>
	间隔:<select name="v_interval_index" id="v_interval_index" >
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
	</select><br/>
	<input type="radio" name="ff" value="0" onclick="resetInterval()">同比
	<input type="radio" name="ff" value="1" onclick="resetInterval()">环比<br>
	<input type="radio" name="ff" value="2" onclick="resetInterval()">两点
	<input type="radio" name="ff" value="3" onclick="resetInterval()">三点
	<input type="radio" name="ff" value="4" onclick="resetInterval()">五点<br>
	<input type="radio" name="ff" value="5" onclick="resetInterval()">季度
	<input type="radio" name="ff" value="6" onclick="resetInterval()">半年
	<input type="radio" name="ff" value="7" onclick="resetInterval()">年化<br>
	<input type="button"  value="(" onclick="appends2S('(')">
	<input type="button"  value=")" onclick="appends2S(')')">
	<input type="button"  value="+" onclick="appends2S('+')"><br>
	<input type="button"  value="-" onclick="appends2S('-')">
	<input type="button"  value="*" onclick="appends2S('*')">
	<input type="button"  value="/" onclick="appends2S('/')"><br>
	<input type="button"  value="&gt" onclick="appends2S('&gt')">
	<input type="button"  value="&lt" onclick="appends2S('&lt')">
	<input type="button"  value="=" onclick="appends2S('=')"><br>
	<input type="button"  value="or" onclick="appends2S('&&')">
	<input type="button"  value="and" onclick="appends2S('||')">
	<input type="button" id="createIndexVariable" onclick="createIndexVariable()" value="生成"/>
	:<span  id="indexVariable"></span>
	<input type='hidden' id="vCurrentTr" value=""/>
</div>
<div id="his_desc" style="border-style:solid; border-width:1px;width:250px;display:none;background:gray">
			 <ul class="ullist">
			    <li onclick="appendHisS('业绩增长很快')">业绩增长很快</li>
				<li onclick="appendHisS('公司盈利好转')">公司盈利好转</li>
				<li onclick="appendHisS('公司业绩出现重大问题')">公司业绩出现重大问题</li>
			 </ul> 
			 <input type='hidden' id="msgCurrentTr" value=""/>
</div>
</div><!-- end content-->
</body>
</html>

