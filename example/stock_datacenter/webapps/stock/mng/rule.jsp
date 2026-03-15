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
	$("#dataSource").change(function(){
		//初始化报表体系
		initTableSystem("dataSource","tableSystem");
	});

});//end ready
function loadRuleDefine(url)
{
	$.get("<%=request.getContextPath()%>"+url, function(result){
    $("#modifyRuleDiv").html(result);
	});
}
function showModifyRulePanel(cIndexCode)
{

	window.showModalDialog ('<%=request.getContextPath()%>/mng/ruleDefine.jsp?cIndexCode='+cIndexCode, '规则管理', 'dialogHeight=500px;dialogWidth=1050px;dialogLeft=300;dialogTop=100;scroll=yes;resizable=yes;status=no');
}
//保存公司列表
var companyArray = new Array();
//自动匹配公司名
function autoMatcher(aa)
{
	var dindex =$("#tableSystem").get(0).selectedIndex;
	var c_data = companyArray[dindex];

	var id = aa.id;
	if(c_data==null)
	{
		var tsc = $("#tableSystem").val();
		$.get("<%=request.getContextPath()%>/company/getCompanyByTableSystemString?msg.tableSystemCode="+tsc, function(result){
				if(result.data==null)
				{
					return;
				}
				var data = result.data.split(";");
				if(data!=null)
				{
					c_data = data;
					companyArray[dindex] = c_data;
				}
				
		  });
	}
	if(c_data!=null)
	{
		$("#"+id+"").autocomplete(c_data);
	}
	
}
function queryRule()
{
	$("#cfrule_tbody_dataTable").empty();
	$.getJSON("<%=request.getContextPath()%>/cfirule/getRuleByCondition", $("#ruleForm").serialize(),function(result){
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
		
  });
}
function showResult(result)
{
	var data = result.data;
	var i;
	for(i=0;i<data.length;i++)
		{
			var cl = data[i];
			var rowHtml = "<tr class='alt1'>";
			rowHtml +="<td><a href=\"\" onclick=\"showModifyRulePanel("+cl.cIndexCode+");return false;\">修改</a></td>";
			rowHtml +="<td>"+cl.cIndexCode+"</td>";
			rowHtml +="<td>"+cl.name+"</td>";
			rowHtml +="<td>"+cl.type+"</td>";
			rowHtml +="<td>"+cl.acrossType+"</td>";
			rowHtml +="<td>"+cl.comments+"</td>";
			rowHtml +="<td>"+cl.q1Rule+"</td>";
			rowHtml +="<td>"+cl.q1RuleComments+"</td>";
			rowHtml +="<td>"+cl.q2Rule+"</td>";
			rowHtml +="<td>"+cl.q2RuleComments+"</td>";
			rowHtml +="<td>"+cl.q3Rule+"</td>";
			rowHtml +="<td>"+cl.q3RuleComments+"</td>";
			rowHtml +="<td>"+cl.q4Rule+"</td>";
			rowHtml +="<td>"+cl.q4RuleComments+"</td>";
			rowHtml +="<td>"+cl.tableSystemCode+"</td>";
			rowHtml +="<td>"+cl.updateTime+"</td>";
			$("#cfrule_tbody_dataTable").append(rowHtml);
		}
}
function addRule()
{
	window.showModalDialog ('<%=request.getContextPath()%>/mng/ruleDefine.jsp', '规则管理', 'dialogHeight=500px;dialogWidth=1050px;dialogLeft=300;dialogTop=100;scroll=yes;resizable=yes;status=no');
}
//初始数据源 did 是数据源选择框的id
var dataSourceData = null;
function initDataSource(did)
{
	 	  
	 	  if(dataSourceData==null||dataSourceData=="")
	 		  {
		 		  $.ajax({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
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
		 		  $.ajax({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
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
//保存公司列表
var companyArray = new Array();
//自动匹配公司名
function autoMatcher(aa)
{
	var dindex =$("#tableSystem").get(0).selectedIndex;
	var c_data = companyArray[dindex];

	var id = aa.id;
	if(c_data==null)
	{
		var tsc = $("#tableSystem").val();
		$.ajax({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.get("<%=request.getContextPath()%>/company/getCompanyByTableSystemString?msg.tableSystemCode="+tsc, function(result){
				if(result.data==null)
				{
					return;
				}
				var data = result.data.split(";");
				if(data!=null)
				{
					c_data = data;
					companyArray[dindex] = c_data;
				}
				
		  });
	}
	if(c_data!=null)
	{
		$("#"+id+"").autocomplete(c_data);
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
}
</script>
</head>
<body>
 <div style="text-align:left">
		 <div>
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
				</select>
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
				指标名称:<input type="text" id="name" name="rule.name" />
				公司编码:<input type="text" id="companyCode" name="rule.companyCode" onkeyup="autoMatcher(this)"/><br/>
				
				<!-- 跨期类型: --><select id="acrossType" name="rule.acrossType" style="display:none">
					<option value='0' selected>不跨期</option>
					<option value='1'>跨期</option>
				</select>
				开始时间:<input type="text" id="startTime" name="rule.startTime" onclick="SelectDate(this,'yyyy-MM-dd')"/>
				结束时间:<input type="text" id="endTime" name="rule.endTime" onclick="SelectDate(this,'yyyy-MM-dd')"/>
				规则类型:<select id="deRuleType" name="rule.type">
					<option value='1' selected>基本指标</option>
					<option value='2'>行情指标</option>
				</select>
				<input type="button" id="queryButton" name="queryButton" value="查询" onclick="queryRule()"/>
				<input type="button" id="ruleDefineButton" name="ruleDefineButton" value="规则定义" onclick="addRule()"/>
		</form>
		</div>
		<div id="div_cfrule_table" style="overflow:auto;height:700px">
					 <table class="datalist2">
						<thead id="cfrule_thead_dataTable"></thead>
						<tr>
							<td>操作</td>
							<td>指标编码</td>
							<td>指标名称</td>
							<td>指标类型</td>
							<td>跨期类型</td>
							<td>规则</td>
							<td>Q1公式</td>
							<td>Q1公式备注</td>
							<td>Q2公式</td>
							<td>Q2公式备注</td>
							<td>Q3公式</td>
							<td>Q3公式备注</td>
							<td>Q4公式</td>
							<td>Q4公式备注</td>
							<td>报表体系编码</td>
							<td>更新时间</td>
						</tr>
						<tbody id="cfrule_tbody_dataTable"></tbody>
					 </table>
		</div>
</div>
</body>
</html>

