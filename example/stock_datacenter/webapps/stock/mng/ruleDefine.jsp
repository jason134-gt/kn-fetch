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

<%
String cIndexCode = request.getParameter("cIndexCode");
String indexName = request.getParameter("indexName");
String dataSourceCode = request.getParameter("dataSourceCode");
String tableSystemCode = request.getParameter("tableSystemCode");
if(indexName==null||StringUtil.isEmpty(tableSystemCode))
{
	indexName="";
	if(!StringUtil.isEmpty(cIndexCode))
	{
		com.stock.common.model.Dictionary d = DictService.getInstance().getDataDictionary(cIndexCode);
		if(d!=null)
		{
			Matchinfo m = MatchinfoService.getInstance().getMatchInfoByTableCode(d.getTableCode());
			dataSourceCode = m.getDataSourceCode();
			tableSystemCode = m.getTableSystemCode();
		}
	}
}


%>
<html>
<head>
 <title>价值投资分析系统</title>
<link rel="stylesheet" type="text/css" href="css/InvestAnalysis2.css">
<script src="js/jquery/jquery-1.6.2.min.js" type="text/javascript"></script>
<script src="js/highcharts/js/highcharts.js" type="text/javascript"></script>
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
$(document).ready(function() {
	//初始数据源
	//initDataSource("dataSource");
	$("#deIndexName").val('<%=indexName%>');
	if($("#deIndexName").val()!="")
	{
		$("#deIndexName").attr("readonly",true);
	}
	$("#dataSource").val('<%=dataSourceCode%>');
	$("#tableSystem").val('<%=tableSystemCode%>');

	var tscode = '<%=tableSystemCode%>';
	if(isNull(tscode))
	{
		tscode = $("#tableSystem").val();
	}

	//加载缺省指标
	queryIndex(tscode);
	//加载模板
	//queryTemplateCode(tscode);
	queryRuleById();
	//隐藏规则定义面板
	$("#dcButton").click(function(){
		 $("#defineDiv").slideUp();
		 $("#indexPanel").slideUp();
	});//end click

	//保存规则
	$("#saveRuleButton").click(function(){
		 //消息参数校验
		   if($("#deIndexName").val()=="")	
			{
				alert("指标名称不能为空！");
				return ;
			}
			if($("#q0RuleArea").val()=="")	
			{
				alert("定义的规则不能为空！");
				return ;
			}
			
			$("#q1RuleComments").val($("#q1RuleArea").val());
			$("#q2RuleComments").val($("#q2RuleArea").val());
			$("#q3RuleComments").val($("#q3RuleArea").val());
			$("#q4RuleComments").val($("#q4RuleArea").val());
			$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
			$.getJSON("<%=request.getContextPath()%>/cfirule/saveRule", $("#ruleForm").serialize(),function(result){
					if(result.success)
					{
						alert("操作成功！");
						queryIndex($("#tableSystem").val());
					}
					else
					{
						alert("操作失败！");
					}

			   });//end getJSON
	});//end click

	$("#dataSource").change(function(){
		//初始化报表体系
		initTableSystem("dataSource","tableSystem");
	});

	//根据报表体系标准,更新指标与模板
	$("#tableSystem").change(function(){
		updateIndexWhenTsChange();
		  
	  });//end click

	$("#acrossType").change(function(){
		if($("#acrossType").val()=="0")
		{
			$("#qRuleDiv").slideDown()
		}
		else
		{
			hiddenAllRule();
			$("#qRuleDiv").slideUp()
		}
		
	});

});//end ready
function updateIndexWhenTsChange()
{
	var tscode = $("#tableSystem").val();
		 //根据报表体系,加载对应的缺省指标
		queryIndex(tscode);
		//根据报表体系,加载对应的模板
		//queryTemplateCode(tscode);
		clearAllRuleData();
}
function queryRuleById()
{
	var cIndexCode = <%=cIndexCode%>;
	if(cIndexCode!=null&&cIndexCode!="")
	{
			$.getJSON("<%=request.getContextPath()%>/cfirule/queryRuleById", "rule.cIndexCode="+cIndexCode,function(result){
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
						//alert("没有找到数据!");
						return;
				   }
				addValue2Panel(result.data);
				
		  });
	}
}
function addValue2Panel(data)
{
	$("#deIndexName").val(data.name);
	$("#deRuleType").val(data.type);
	$("#acrossType").val(data.acrossType);
	$("#tableSystem").val(data.tableSystemCode);
	$("#interval").val(data.interval);
	$("#timeUnit").val(data.timeUnit);
	$("#deIndexName").attr("readonly",true);
	if(data.acrossType==0)
	{
		$("#qRuleDiv").slideDown();
	}
	$("#q0RuleArea").val(data.comments);
	if(data.q1RuleComments!=null&&data.q1RuleComments!="")
	{
		
		//showQRuleArea("q1RuleDiv");
		$("#q1RuleArea").val(data.q1RuleComments);
	}
	if(data.q2RuleComments!=null&&data.q2RuleComments!="")
	{
		
		//showQRuleArea("q2RuleDiv");
		$("#q2RuleArea").val(data.q2RuleComments);
	}
	if(data.q3RuleComments!=null&&data.q3RuleComments!="")
	{
		
		//showQRuleArea("q3RuleDiv");
		$("#q3RuleArea").val(data.q3RuleComments);
	}
	if(data.q4RuleComments!=null&&data.q4RuleComments!="")
	{
		
		//showQRuleArea("q4RuleDiv");
		$("#q4RuleArea").val(data.q4RuleComments);
	}
	$("#deCompany").val(data.companyCode);	
}
function showRuleDefinePanel(index,source)
{
		$("#indexPanel").slideUp();
		var ntop =  realOffset(source).y+130;
		var nleft = realOffset(source).x+100;
		//显示规则定义面板
		showDefineIndexPanel(ntop,nleft);
		//隐藏模板与变量定义行
		$("#variableRow").show();
		$("#templateRow").show();
		 //设置发生事件的面板
		indexDefineFlag = index;
		exp=source.value;
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
//构建实时分析区的消息
function buildRealReqMsg()
{
	//公司编码
	var cList = getAllCompany();
	$("#realTimeCompanyList").val(cList);
	//超始时间
	$("#realTimeStartTime").val($("#sf_startTime_year").val()+"-"+$("#sf_startTime_jidu").val());
	//结束时间
	$("#realTimeEndTime").val($("#sf_endTime_year").val()+"-"+$("#sf_endTime_jidu").val());
	//指标编码
	$("#realTimeIndexCode").val($("#sf_indexCodeSelect").val());
	//时间间隔
	$("#interval").val($("#sf_interval").val());
	return ;
}
//取公司对应的指标
function queryIndex(tsCode)
{
	  $.getScript("<%=request.getContextPath()%>/cfirule/getCfiruleListByTidWithOutCache_V2?msg.tableSystemCode="+tsCode, function(result){
		  	if(retObj==null)
				{
					return;
				}
				var data = retObj.split(";");
				if(data!=null)
				{
					var c_data = data;
					 $("#tindexText").flushCache();
					$("#tindexText").autocomplete(c_data, {
					max: 12, //列表里的条目数
					minChars: 0, //自动完成激活之前填入的最小字符
					width: 150, //提示的宽度，溢出隐藏
					scrollHeight: 300, //提示的高度，溢出显示滚动条
					matchContains: true, //包含匹配，就是data参数里的数据，是否只要包含文本框里的数据就显示
					autoFill:false,//自动填充
					cacheLength:0
					});
				}

	  });	
}
//取后端模板
var templateHtml ;
var templateData ;//保存模板数据
function queryTemplateCode(tsCode)
{
  $.get("<%=request.getContextPath()%>/cfirule/getComplileTemplateList?msg.tableSystemCode="+tsCode, $("#hidden_form").serialize(), function(result){
	   //先清空
	   $("#deTemplateS").empty();
	   //分隔不同指标
	   $("#deTemplateS").append("<option value='' selected>-----请选择-----</option>");
		var data = result.data;
		if(data!=null)
		{
			var i=0;
		  	for(i=0;i<data.length;i++)
			{
		  			//分隔指标编码与名字
		  		var ia = data[i];
		  		var indexCode = ia.cIndexCode;
		  		var indexName = ia.name;
				var rule = ia.rule;
		  		$("#deTemplateS").append("<option value="+indexCode+'|'+rule+">"+indexName+"</option>");
			}
			templateHtml = $("#deTemplateS").html();
			templateData = data;
		}
  });	
}
//按表名把指标追加到不到的面板上
function initIndexPanel(indexCode,indexName,tableName,type)
{
	var tindexName = indexName;
	if(indexName.length>10) indexName = indexName.substring(0,10);
	var text = "<option value='"+indexCode+"' title='"+tindexName+"'>"+indexName+"</option>";
	
	if(type==0||type==1)
	{
		$("#assetSelect").append(text);	
	}
	if(type==2||type==3)
	{
		$("#cashFlowSelect").append(text);	
	}
	if(type==4)
	{
		$("#profileSelect").append(text);	
	}
	if(type==6)
	{
		$("#extIndexSelect").append(text);	
	}

}

//显示规则定义面板
function showDefineIndexPanel(ntop,nleft)
{
			ntop = ntop+20;
			$("#defineDiv").css({'position':'absolute','top':ntop,'left':nleft});
			$("#defineDiv").slideDown();
			$("#defineDiv").focus();
			//加上模板数据
			$("#deTemplateS").empty();
			$("#deTemplateS").append(templateHtml);
}
var currentObj;
function showIndexPanel(i,source)
{
	var e = window.event || arguments[0];
	textCode = i;
	if(i==1)
	{
		e.which;
		//获取$("#q0RuleArea")的位置
		var ntop =  $("#tindexText").offset().top+$("#tindexText").height()+10;
		var nleft =  $("#tindexText").offset().left+70;
		$("#indexPanel").css({'position':'absolute','top':ntop,'left':nleft});
		$("#indexPanel").slideDown();
		$("#indexPanel").focus();
	}

}
function hiddenIndexPanel()
{
	if(textCode==1)
	{
		$("#indexPanel").slideUp();
	}
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
		//把选择指标信息加到
		if(textCode==1)
		{
			$("#tindexText").val(x);
		}
	}
	
}
//定义规则
var exp = "";//规则表达式
function appendS(o)
{
	var dst ;
	//区分事件发生在哪个面板

	var id = "q"+indexDefineFlag+"RuleArea";
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
//取模板中所有变量的值
function getAllVariable()
{

	var x=document.getElementsByName("tVariable");
	var k =0;
	var cl = "";
	for(k=0;k<x.length;k++)
	{
		
		cl += x[k].id+":"+x[k].value+"|";
	}
	$("#variableList").val(cl);
}
//生成指标变量,并把变量加到定义面板上
function createIndexVar()
{
	//var o = document.getElementById("deIndexS");
	//var text = o.options[o.selectedIndex].text;
	//var x=text+":"+o.value;
	var t=document.getElementById("de_interval_index").value;
	var x = $("#tindexText").val();
	var v = createVariable(x,t);
	appendS(v);
}
function createTemplateVar()
{
	var o = document.getElementById("deTemplateS");
	var text = o.options[o.selectedIndex].text;
	var x="t"+","+text+":"+o.value.split("\|")[0];
	var t=document.getElementById("de_interval_template").value;
	var v = createVariable(x,t);
	appendS(v);
}
function createVar()
{
	var x=document.getElementById("deVariableS_index").value;
	var t=document.getElementById("deVariableS_time").value;
	var v = createVariable(x,t);
	appendS(v);
}
//拼装变量
function createVariable(x,t)
{
	if(t=="0")
	{
		return "\${"+x+"}";
	}
	return "\${"+x+","+t+"}";
}
function clearObjectData(id)
{
	$("#q0RuleArea").val("");
	$("#q1RuleArea").val("");
	$("#q2RuleArea").val("");
	$("#q3RuleArea").val("");
	$("#q4RuleArea").val("");
}
function clearAllRuleData()
{
	$("#q0RuleArea").val("");
	$("#q1RuleArea").val("");
	$("#q2RuleArea").val("");
	$("#q3RuleArea").val("");
	$("#q4RuleArea").val("");
}
function showQRuleArea(id,source)
{
	hiddenAllRule();
	//获取$("#q0RuleArea")的位置
		var ntop =  $("#q0RuleArea").offset().top;
		var nleft =  $("#q0RuleArea").offset().left+450;
		$("#"+id+"").css({'position':'absolute','top':ntop,'left':nleft});
		$("#"+id+"").slideDown();
		$("#"+id+"").focus();
}
function hiddenAllRule()
{
	$("#q1RuleDiv").slideUp();
	$("#q2RuleDiv").slideUp();
	$("#q3RuleDiv").slideUp();
	$("#q4RuleDiv").slideUp();
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
	}
	$("#deIndexName").val('<%=indexName%>');
	if($("#deIndexName").val()!="")
	{
		$("#deIndexName").attr("readonly",true);
	}

	updateIndexWhenTsChange();
}

function addIndex2Area(id)
{
	var x = $("#"+id+"").val();
	$("#tindexText").val(x);	
}
</script>
</head>
<body>
 <div style="text-align:left">


<div id="indexPanel" style="display:none;z-index:3;text-align:left" >
	指标：<input type="text" id="sindex" ><input type="button" value="添加" onclick="addIndex2Area('sindex')" >
</div>
 <!-- main begins -->
  <div id="defineDiv" style="display:none" >
	<table>
	<tr>
	<td>指标:</td><td><input type="text" id="tindexText" name="tindexText"  style="width:150px"/></td>
	<td>时间间隔:<select name="de_interval_index" id="de_interval_index" >
			<option value='-24'>-24</option>
			<option value='-21'>-21</option>			
			<option value='-18'>-18</option>		
			<option value='-15'>-15</option>			
			<option value='-12'>-12</option>				
			<option value='-9'>-9</option>				
			<option value='-6'>-6</option>
			<option value='-3'>-3</option>
			<option value='-1'>-1</option>
			<option value='0' selected>0</option>
			<option value='1'>1</option>
			<option value='3'>3</option>
			<option value='6'>6</option>
			<option value='9'>9</option>
			<option value='12'>12</option>
			<option value='15'>15</option>
			<option value='18'>18</option>
			<option value='21'>21</option>
			<option value='24'>24</option>	
	</select></td><td><input type="button" name="addIndexButton" id="addIndexButton" value="增加" onclick="createIndexVar()" /></td></tr>

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
</div>
<div id="indexPanel_2" class="indexPanelCss_2">
</div>

<div >

<div style="text-align:left">
		<form id="ruleForm">
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
						  if(!StringUtil.isEmpty(dataSourceCode))
						  {
							  if(ds.getDataSourceCode().equals(dataSourceCode))
							  {
								  selectdsCode =  ds.getDataSourceCode();
								  %>
								  	<option value='<%=ds.getDataSourceCode()%>' selected><%=ds.getDataSourceName() %></option>
								  <%
							  }
						  }
						  else
						  {
							  selectdsCode = dsl.get(0).getDataSourceCode();
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
				</select><br/>
		指标名称:<input type="text" id="deIndexName" name="rule.name" /><br/>
		<input type="hidden" id="cIndexCode" name="rule.cIndexCode" value="<%=cIndexCode%>"/>
		规则类型:<select id="deRuleType" name="rule.type">
			<option value='1' selected>基本指标</option>
			<option value='2'>行情指标</option>
		</select><br/>
		<!--  -->
		<!-- 跨期类型: -->
		<select id="acrossType" name="rule.acrossType" style="display:none">
			<option value='0' selected>不跨期</option>
			<option value='1' >跨期</option>
		</select>
		
		间隔:<select id="interval" name="rule.interval" >
			<option value='3' selected>3</option>
			<option value='1' >1</option>
			<option value='5' >5</option>
			<option value='7' >7</option>
			<option value='10' >10</option>
			<option value='20' >20</option>
			<option value='30' >30</option>
			<option value='60' >60</option>
			<option value='120' >120</option>
			<option value='250' >250</option>
		</select>
		间隔单位:<select id="timeUnit" name="rule.timeUnit" >
			<option value='m' selected>月</option>
			<option value='w' >周</option>
			<option value='d' >日</option>
			<option value='h' >小时</option>
			<option value='f' >分钟</option>
		</select>
		<div id="qRuleDiv" >
		季度规则:<input type="button" id="q1RuleButton" name="q1RuleButton" value="Q1规则" onclick="showQRuleArea('q1RuleDiv',this)"/>
		<input type="button" id="q2RuleButton" name="q2RuleButton" value="Q2规则" onclick="showQRuleArea('q2RuleDiv',this)"/>
		<input type="button" id="q3RuleButton" name="q3RuleButton" value="Q3规则" onclick="showQRuleArea('q3RuleDiv',this)"/>
		<input type="button" id="q4RuleButton" name="q4RuleButton" value="Q4规则" onclick="showQRuleArea('q4RuleDiv',this)"/>
		</div>
		<!-- 
		所属公司:<input type="text" name="rule.companyCode" id="deCompany" onkeyup="autoMatcher(this)"/><br/>
		-->
		<input type="hidden" name="rule.q1RuleComments" id="q1RuleComments"/>
		<input type="hidden" name="rule.q2RuleComments" id="q2RuleComments"/>
		<input type="hidden" name="rule.q3RuleComments" id="q3RuleComments"/>
		<input type="hidden" name="rule.q4RuleComments" id="q4RuleComments"/>

		规则定义:<textarea name="rule.comments" id="q0RuleArea" style="width:450px;height:150px" onclick="showRuleDefinePanel(0,this)"></textarea><br/>
		</form>
		<div id="q1RuleDiv" style="display:none">Q1 规则 :<textarea  id="q1RuleArea" style="width:450px;height:150px" onclick="showRuleDefinePanel(1,this)" ></textarea></div>
		</form>
		<div id="q2RuleDiv" style="display:none">Q2 规则 :<textarea  id="q2RuleArea" style="width:450px;height:150px" onclick="showRuleDefinePanel(2,this)"></textarea></div>
		</form>
		<div id="q3RuleDiv" style="display:none">Q3 规则 :<textarea  id="q3RuleArea" style="width:450px;height:150px" onclick="showRuleDefinePanel(3,this)"></textarea></div>
		</form>
		<div id="q4RuleDiv" style="display:none">Q4 规则 :<textarea  id="q4RuleArea" style="width:450px;height:150px" onclick="showRuleDefinePanel(4,this)"></textarea></div>
		</form>
		<input type="button" id="cleardeTextButton" name="cleardeTextButton" value="清空数据" onclick="clearObjectData('q0RuleArea')"/>
		<input type="button" id="saveRuleButton" name="saveRuleButton" value="保存规则" /><br/><br/>	
</div>

</div>
 
 </div>
</body>
</html>

