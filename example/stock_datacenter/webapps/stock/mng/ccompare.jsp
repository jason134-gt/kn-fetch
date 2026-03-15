<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
	<%@page import="com.yz.stock.portal.model.*"   %> 
<%@page import= "java.util.*"   %> 
<%@page import= "com.yz.mycore.lcs.enter.LCEnter"   %> 
<%@page import= "com.push.stock.common.*"   %> 
<%@page import= "com.yz.stock.util.*"   %> 
<%@page import= "com.yz.stock.portal.service.*"   %> 
<%@page import="com.stock.common.model.*"   %> 
<%@page import= "com.stock.common.constants.*"   %> 
<%@page import= "com.stock.common.util.*"   %> 
<%@page import= "com.yfzx.service.db.*"   %> 

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
$(document).ready(function() {
	//初始化年份
	initYear("cfdata_startTime_year",14);
	initYear("cfdata_endTime_year",16);

	 //初始数据源
	//initDataSource("dataSource");

	$("#dataSource").change(function(){
		//初始化报表体系
		initTableSystem("dataSource","tableSystem");
	});

});//end ready

function getUintMsg(uid)
{
	var unitValue = $("#"+uid+"").val();
	var unitName="";
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
	return unitName+"|"+unitValue;
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
					companyArray[dindex] = c_data;
				}
				
		  });
	}
	if(c_data!=null)
	{
		$("#"+id+"").autocomplete(c_data);
	}
	
}

//查询公司的同一行业信息并生成行业选择面板
function createIndustryPanel(id,source)
{
	if($("#industry_panel").is(":hidden"))
	{
		
		if(id==0)
		{
				//公司编码
			$("#companyCode").val($("#div_companyCode").val());
			if($("#companyCode").val()=="")	
			{
				alert("公司编码不可为空!");
				return;
			}
		}
		else
		{
			
				//公司编码
			$("#companyCode").val($("#cfdata_companyCode").val());
		
			if($("#companyCode").val()=="")	
			{
				alert("公司编码不可为空!");
				return;
			}
		}
			$("#companyList").val($("#companyCode").val());
			
		 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
			   $.getJSON("<%=request.getContextPath()%>/company/getComanyByIndustry", $("#dataForm").serialize(),function(result){
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
			$("#industry_panel_table").empty();
			 var d = result.data;
			var i=0 ;
			//分隔每个公司
			for(i=0 ;i<d.length;i++)
				{
				var company = d[i];
				var tt = company.companyNameChi+":"+company.companyCode;
				$("#industry_panel_table").append("<tr class='alt1'><td><a href='' onclick='append2CompanyText("+id+",this);return false'>"+tt+"</a></td></tr>");

				}	
					
			   });//end getJSON
		//显示面板
		showIndustryPanel(id,source);
		$("#showIndustryButton_cfdata").val("隐藏同行");
	}
	else
	{
		$("#industry_panel").slideUp();
		$("#showIndustryButton_cfdata").val("显示同行");
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
function buildQueryCfdataMsg()
{
	//公司编码
	$("#companyCode").val($("#cfdata_companyCode").val());
	$("#companyList").val($("#companyCode").val());
	//指标编码
	$("#tableid").val($("#cftable_select").val());
	//超始时间
	$("#startTime").val($("#cfdata_startTime_year").val()+"-"+$("#cfdata_startTime_jidu").val());
	//结束时间
	$("#endTime").val($("#startTime").val());
	//报表体系
	$("#tableSystemCode").val($("#tableSystem").val());
	//数据源
	$("#dataSourceCode").val($("#dataSource").val());
}
function checkQueryCfdataMsg()
{
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
	if($("#tableid").val()=="")	
	{
		return "财务报表不可以为空！";
	}
	return "";
}
//查询公司相应的财务数据
var tableid=0;
var cResult ;
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
		   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   tableid=$("#tableid").val();
		   $("#tableSystemCode").val($("#tableSystem").val());
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
			   if(result.data==null||result.data==""||result.data.length==0)
			   {
					alert("没有找到数据!");
					return;
			   }
				showCompanyAllIndexData_column(result);
				cResult = result;
		   });//end getJSON
}
//显示公司财务数据
function showCompanyAllIndexData_column(result)
{
	isRowModel=false;
	var dataSourceCode = $("#dataSource").val();
	var tableSystemCode = $("#tableSystem").val();
	var unitValue = $("#unit_select").val();
	var companyCode=$("#cfdata_companyCode").val();

	$("#cfpanel_thead_dataTable").empty();
	$("#cfpanel_tbody_dataTable").empty();
	//行的list
	var rowList = result.data;
	var i=0;
	for(i=0;i<rowList.length;i++)
	{
		
		var row = rowList[i].cl;
		var c =0;
		for(c=0;c<row.length;c++)
		{
			var cItem = row[c];
			var rowHtml="";
			//为第一列或行时,用i,加上表头
			if(i==0)
			{
				var indexName =  cItem.columnChiName+":"+cItem.indexCode;
				if(cItem.indexCode!="-999")
				{
					rowHtml="<tr class='alt1' id='row_"+c+"'><th><a href=\"#\" onclick=\"showIndexChart('"+dataSourceCode+"','"+tableSystemCode+"','"+indexName+"','"+cItem.indexCode+"','"+companyCode+"','"+unitValue+"');return false;\">"+cItem.columnChiName+"</a></th></tr>";
				}
				else
				{
					rowHtml="<tr class='alt1' id='row_"+c+"'><th>"+cItem.columnChiName+"</th></tr>";
				}
				
			}
			$("#cfpanel_tbody_dataTable").append(rowHtml);
			var value ="";
			//如果指标为-999则不处理,针对时间与公司名
			if(cItem.indexCode!="-999")
				{
					
					if(cItem.value!=0)
					{
						value = formatByUint(cItem.value,unitValue);
						value = value.toFixed(2);
					}
					
				}
			else
				{
					value = cItem.value;
				}
			$("#row_"+c+"").append("<td>"+value+"</td>");
		}
	}
	
	
}

//显示公司财务数据
function showCompanyAllIndexData_row(result)
{
	var dataSourceCode = $("#dataSource").val();
	var tableSystemCode = $("#tableSystem").val();
	var unitValue = $("#unit_select").val();
	var companyCode=$("#cfdata_companyCode").val();

	isRowModel=true;
	$("#cfpanel_thead_dataTable").empty();
	$("#cfpanel_tbody_dataTable").empty();
	//行的list
	var rowList = result.data;
	var i=0;
	for(i=0;i<rowList.length;i++)
	{
		
		var row = rowList[i].cl;
		var c =0;
		var headHtml="<tr class='alt1'>";
		var rowHtml="<tr class='alt1'>";
		for(c=0;c<row.length;c++)
		{
			var cItem = row[c];
			//为第一列或行时,用i,加上表头
			if(i==0)
			{
				var indexName =  cItem.columnChiName+":"+cItem.indexCode;
				if(cItem.indexCode!="-999")
				{
					headHtml+="<th><a href=\"#\" onclick=\"showIndexChart('"+dataSourceCode+"','"+tableSystemCode+"','"+indexName+"','"+cItem.indexCode+"','"+companyCode+"','"+unitValue+"');return false;\">"+cItem.columnChiName+"</a></th>";
				}
				else
				{
					headHtml+="<th>"+cItem.columnChiName+"</th>";
				}
				
			}
			var value ="";
			//如果指标为-999则不处理,针对时间与公司名
			if(cItem.indexCode!="-999")
				{
					if(cItem.value!=0)
					{
						value = formatByUint(cItem.value,unitValue);
						value = value.toFixed(2);
					}
				}
			else
				{
				value = cItem.value;
				}
			rowHtml+="<td>"+value+"</td>";;
		}
		if(i==0)
		{
			$("#cfpanel_thead_dataTable").append(headHtml);
		}
		$("#cfpanel_tbody_dataTable").append(rowHtml);
	}
	
	
}
 var isRowModel=false;
function convertshow()
{
	if(cResult!=null)
	{
		if(isRowModel)
		{
			showCompanyAllIndexData_column(cResult);
		}
		else
		{
			showCompanyAllIndexData_row(cResult);
		}
	}
}
function formatByUint(value,unitValue)
{
	//如果绝对值是小于1的数,则不做转换
	//if(Math.abs(value)/unitValue<0.0001)
	//{
		//return value;
	//}
	return value/unitValue;
}
function showIndexChart(dataSourceCode,tableSystemCode,indexName,cIndexCode,companyCode,unitValue)
{
	window.showModalDialog ("/stock/jsp/portal/oneChartShow.jsp?cIndexCode="+cIndexCode+"&dataSourceCode="+dataSourceCode+"&tableSystemCode="+tableSystemCode+"&indexName="+indexName+"&companyCode="+companyCode+"&unitValue="+unitValue, "指标图表", "dialogHeight=600px;dialogWidth=1050px;dialogLeft=300;dialogTop=100;scroll=yes;resizable=yes;status=no");
}
</script>

</head>
<body>
 <!-- main begins -->
  
 <div>
  
  <div id="div_cfdata" >
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
	公司名称:<input type="text" name="cfdata_companyCode" id="cfdata_companyCode" onkeyup="autoMatcher(this)"/>
		<font color="red">批量</font>:<input type="checkbox" name="batch_checkBox" id="batch_checkBox" onclick="showBatchPanel()"/>
		<div id="BatchPanel" style="display:none;background:green;z-index:3">
			<input type="text" name="batch_companyCode" id="batch_companyCode" size="23px" onkeyup="autoMatcher(this)"/><br/>
			<input type="button" name="clearCompanyButton" id="clearCompanyButton" value="清空" onclick="clearObject('companyShowDiv')"/>
			<input type="button" name="addCompanyButton" id="addCompanyButton" value="增加" onclick="addCompany()"/>
			<input type="button" name="addCompanyConfirmButton" id="addCompanyConfirmButton" value="隐藏" onclick="hideObject('BatchPanel')"/>
			<div id="companyShowDiv"></div>
		</div>
		<input type="button" id="showIndustryButton_cfdata" name="showIndustryButton"value="显示同行" onclick="createIndustryPanel(1,this)"/>
	财务报表:<select name="cftable_select" id="cftable_select" >	
		<option value='0' selected>资产表</option>
		<option value='2' >现金流量表</option>
		<option value='4' >利润表</option>
		<option value='5' >基本指标</option>
		<option value='6' >扩展指标</option>
		</select>
    时间:<select name="cfdata_startTime_year" id="cfdata_startTime_year" >	
		</select><select name="cfdata_startTime_jidu" id="cfdata_startTime_jidu" >	
		<option value='3-30' >一季度</option>
		<option value='6-30' selected>二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select>
		结束时间:<select name="cfdata_endTime_year" id="cfdata_endTime_year" style="display:none">	
		</select><select name="cfdata_endTime_jidu" id="cfdata_endTime_jidu" style="display:none">	
		<option value='3-30' >一季度</option>
		<option value='6-30' selected>二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select>
		单位:<select name="unit_select" id="unit_select" >	
					<option value='1' selected>无单位</option>
					<option value='1000' >千</option>
					<option value='10000' >万</option>
					<option value='1000000'>百万</option>
					<option value='10000000' >千万</option>
					<option value='100000000' >亿</option>
			</select>
		<input type="button" id="queryCfdataButton" name="queryCfdataButton" value="查询" onclick="queryAllCompanyIndexData()"/>
		<input type="button" id="covertButton" name="covertButton" value="转置" onclick="convertshow()"/>
		<br/>	
		<div id="div_cfpanel_table" style="overflow:auto;height:600px">
			 <table class="datalist2">
				<thead id="cfpanel_thead_dataTable"></thead>
				<tbody id="cfpanel_tbody_dataTable"></tbody>
			 </table>
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
  
  <div id="industry_panel" style="height:200px;display:none;z-index:2;overflow-x:auto;overflow-y:auto">
	 <table class="datalist2" id="industry_panel_table">
    </table>
  </div>
 
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

