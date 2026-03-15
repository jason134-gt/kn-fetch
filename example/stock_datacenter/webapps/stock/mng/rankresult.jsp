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
<%

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

	initYear("myperiod",15);
	var tscode= $("#tableSystem").val();
	
	//隐藏规则定义面板
	$("#dcButton").click(function(){
		 $("#defineDiv").slideUp();
		 $("#indexPanel").slideUp();
	});//end click



	$("#dataSource").change(function(){
		//初始化报表体系
		initTableSystem("dataSource","tableSystem");
	});

	//根据报表体系标准,更新指标与模板
	$("#tableSystem").change(function(){
		updateRanklist();
		  
	  });//end click



});//end ready
function updateRanklist()
{
	var tsc = $("#tableSystem").val();
	if(tsc!=null&&tsc!="")
	{
			$.getJSON("<%=request.getContextPath()%>/ranking/queryRankByTableSystemCode?tsc="+tsc,function(result){
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
			
						showRankingList(result.data);
					
			
				
		  });
	}
}

function showRankingList(data)
{
	 if(data!=null&&data!="")
	 {
		var html = "";
		$("#rankingCode").empty();
		var i=0;
		for(i=0;i<data.length;i++)
			{
				var rank = data[i];
				var rcode = rank.rankingCode;
				var rname = rank.rankingName;
				html+="<option value='"+rcode+"'>"+rname+"</option>"
			}
		
		$("#rankingCode").append(html);
		
	 }
}
function queryrankById()
{
	if(rankcode!=null&&rankcode!="")
	{
			$.getJSON("<%=request.getContextPath()%>/cfirule/queryrankById?rank.rankcode="+rankcode,function(result){
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


var currentObj;
function showIndexPanel(i,source)
{
	var e = window.event || arguments[0];
	textCode = i;
	if(i==1)
	{
		e.which;
		//获取$("#rankArea")的位置
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

}

function queryCompanyListByRankcode()
{
	var rcode = $("#rankingCode").val();
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	var rankPeriod = $("#myperiod").val()+"-"+$("#myperiod_jidu").val();
	$("#rank_tbody_dataTable").empty();
	$.getJSON("<%=request.getContextPath()%>/ranking/queryCompanyListByRankcode?rankcode="+rcode+"&rankPeriod="+rankPeriod,function(result){
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
		
		showResult(result);
		
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
			rowHtml +="<td>"+cl.companycode+"</td>";
			rowHtml +="<td>"+cl.companyname+"</td>";
			rowHtml +="<td>"+cl.value+"</td>";
			rowHtml +="<td>"+cl.period+"</td>";
			rowHtml +="</tr>";
			$("#rank_tbody_dataTable").append(rowHtml);
		}
}

</script>
</head>
<body>
<div >

<div style="text-align:left">
		 数据来源:<select name="dataSourceCode" id="dataSource" >	
		 	 <%
		 	 	  String selectdsCode = "";
				  LCEnter lce = LCEnter.getInstance();
				  List<DataSource> dsl = lce.get(StockUtil.getListKeyByDataType(StockConstants.DataSource), StockUtil.getCacheName(StockConstants.common));
				  if(dsl!=null&&dsl.size()>0)
				  {
					  
					  selectdsCode = dsl.get(0).getDataSourceCode();
					  for(int i=0;i<dsl.size();i++)
					  {
						  DataSource ds = dsl.get(i);
						  if(ds.getDataSourceCode().equals(selectdsCode))
						  {
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
 报表体系:<select name="rank.tableSystemCode" id="tableSystem" >
 				<%
 					String tsc = "";
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
 									 tsc=ts.getTableSystemCode();
 									  
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
		 榜 单 项 :<select name="rank.rankingCode" id="rankingCode" >
 				<%
 					List<Ranking> rl = null;
 					if(!StringUtil.isEmpty(selectdsCode))
 					{
 						rl = RankingService.getInstance().queryRankingByTableSystemCode(tsc);
 					}
 					else
 					{
 						rl = RankingService.getInstance().queryAllRanking();
 					}
					 if(rl!=null&&rl.size()>0)
					  {
						  for(int i=0;i<rl.size();i++)
						  {
							 Ranking r = rl.get(i);
							 if(r.getTableSystemCode().equals(tsc))
							  {
								  
								  %>
								  	<option value='<%=r.getRankingCode()%>' selected><%=r.getRankingName() %></option>
								  <%
							  }
							  else
							  {
								  %>
								  	<option value='<%=r.getRankingCode()%>' ><%=r.getRankingName() %></option>
								  <%
							  }
						  }
					}
 				%>	
				</select>
		 时期:<select id="myperiod" >	
		</select><select name="myperiod_jidu" id="myperiod_jidu" >	
		<option value='3-30' selected>一季度</option>
		<option value='6-30' selected>二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select>
		<input type="button" id="queryRankingCompanyList" value="查询榜单" onclick="queryCompanyListByRankcode()"/><br/>
		
		==============================================================================================================<br/>
		此榜单结果如下列表所示：<br/>
		 <div style="text-align:left">
		<div id="div_rank_table" style="overflow:auto;height:auto">
					 <table class="datalist2">
						<thead id="rank_thead_dataTable"></thead>
						<tr>
							<td>公司编码</td>
							<td>公司名</td>
							<td>分值</td>
							<td>时间</td>
						</tr>
						<tbody id="rank_tbody_dataTable"></tbody>
					 </table>
		</div>
</div>
		
</div>

</div>
 

</body>
</html>

