<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
	<%@page import="com.yz.stock.portal.model.*"   %> 
<%@page import= "java.util.*"   %> 
<%@page import= "com.yz.mycore.lcs.enter.LCEnter"   %> 
<%@page import="com.stock.common.model.*"   %> 
<%@page import= "com.stock.common.constants.*"   %> 
<%@page import= "com.stock.common.util.*"   %> 
<%@page import= "com.yz.stock.portal.service.*"   %> 
<%@page import= "com.yz.stock.util.*"   %> 
<%@page import= "com.yfzx.service.db.*"   %> 



<%
	String rankcode = request.getParameter("rankcode");
 	Ranking rank = null;
 	if(!StringUtil.isEmpty(rankcode))
 	{
 		rank = RankingService.getInstance().queryRankingByCode(Integer.valueOf(rankcode));
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

	var tscode= $("#tableSystem").val();
	//加载缺省指标
	//queryIndex(tscode);
	initAutoIndexMatcher('tindexText');
	//隐藏规则定义面板
	$("#dcButton").click(function(){
		 $("#defineDiv").slideUp();
		 $("#indexPanel").slideUp();
	});//end click

	//保存规则
	$("#saverankButton").click(function(){
		 //消息参数校验
		   if($("#rankName").val()=="")	
			{
				alert("指标名称不能为空！");
				return ;
			}
			if($("#rankArea").val()=="")	
			{
				alert("定义的规则不能为空！");
				return ;
			}
		
			
			$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
			$.getJSON("<%=request.getContextPath()%>/ranking/saveRanking", $("#rankForm").serialize(),function(result){
					if(result.success)
					{
						alert("操作成功！");
						//queryIndex($("#tableSystem").val());
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
			$("#qrankDiv").slideDown()
		}
		else
		{
			hiddenAllrank();
			$("#qrankDiv").slideUp()
		}
		
	});

});//end ready
function initAutoIndexMatcher(aa)
{

		var tsc = $("#tableSystem").val();
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.getScript("/stock/cfirule/getCfiruleListByTscid_V2?msg.tableSystemCode=ts_00003", function(result){
				var rd = retObj ;
				var data = rd.split(";");
				if(data!=null)
				{
					var c_data = data;
					var option = {
						max: 12, 
						minChars: 0, 
						width: 150,
						scrollHeight: 300, 
						matchContains: true, 
						autoFill: false 
						};
					var obj = $("#"+aa+"");
					obj.autocomplete(c_data, option);
				}
				
		  });
	

}
function updateIndexWhenTsChange()
{
	var tscode = $("#tableSystem").val();
		 //根据报表体系,加载对应的缺省指标
		queryIndex(tscode);
		//根据报表体系,加载对应的模板
		//queryTemplateCode(tscode);

		clearAllrankData();
}
function queryrankById()
{
	var rankcode = <%=rankcode%>;
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
function addValue2Panel(data)
{
	$("#rankName").val(data.name);


	$("#tableSystem").val(data.tableSystemCode);

}
function showrankDefinePanel(index,source)
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

//取公司对应的指标
function queryIndex(tsCode)
{
	  $.get("<%=request.getContextPath()%>/cfirule/getCfiruleListByTidWithOutCache?msg.tableSystemCode="+tsCode, $("#hidden_form").serialize(), function(result){
		   //先清空
		   $("#sf_indexCodeSelect").empty();
		    //先清空
			$("#assetSelect").empty();
			 //先清空
			$("#cashFlowSelect").empty();
			 //先清空
			$("#profileSelect").empty();
			 //先清空
			$("#extIndexSelect").empty();
			
				
		   //分隔不同指标
		   $("#sf_indexCodeSelect").append("<option value='' selected>-----请选择-----</option>");
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
			  		$("#sf_indexCodeSelect").append("<option value="+indexCode+">"+indexName+"</option>");
					//按表名把指标追加到不到的面板上
					initIndexPanel(indexCode,indexName,tableName,type);
				}
			}
			//缓存一份指标值
			indexHtml = $("#sf_indexCodeSelect").html();
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

	var id = "rankArea";
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
	$("#"+id+"").val("");
}
function clearAllrankData()
{
	$("#rankArea").val("");
	
}
function showQrankArea(id,source)
{
	hiddenAllrank();
	//获取$("#rankArea")的位置
		var ntop =  $("#rankArea").offset().top;
		var nleft =  $("#rankArea").offset().left+450;
		$("#"+id+"").css({'position':'absolute','top':ntop,'left':nleft});
		$("#"+id+"").slideDown();
		$("#"+id+"").focus();
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
	
	updateIndexWhenTsChange();
}

function loadrankDefine(url)
{
	$.get("<%=request.getContextPath()%>"+url, function(result){
    $("#modifyrankDiv").html(result);
	});
}
function showModifyrankPanel(rankcode)
{

  $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get ('<%=request.getContextPath()%>/ranking/queryRankingByCode?rank.rankingCode='+rankcode, function(result){

		 if(result.success)
			{
					if(result.data!="")
				{
						var rank = result.data;
						$("#dataSource").val(rank.dataSourceCode);
						$("#rankCode").val(rank.rankingCode);
						$("#rankName").val(rank.rankingName);
						$("#rankArea").val(rank.rankingComments);
						$("#tableSystem").val(rank.tableSystemCode);

				}
			 }
			   else
			 {
					 alert("操作失败!");
			 }
	});
}

function queryrank()
{
	  $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$("#rank_tbody_dataTable").empty();
	$.getJSON("<%=request.getContextPath()%>/ranking/queryRankingByCondition", $("#rankQueryForm").serialize(),function(result){
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
function delRanking(rankcode)
{
	  $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("<%=request.getContextPath()%>/ranking/delRankingBycode?rank.rankingCode="+rankcode,function(result){
		  if(result.success)
		   {
			   queryrank();
		   }
		   else
			{
				alert("操作失败!");
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
			rowHtml +="<td>"+cl.rankingCode+"</td>";
			rowHtml +="<td>"+cl.tableSystemCode+"</td>";
			rowHtml +="<td>"+cl.rankingName+"</td>";
			rowHtml +="<td>"+cl.rankingRule+"</td>";
			rowHtml +="<td>"+cl.rankingComments+"</td>";
			rowHtml +="<td>"+cl.uptime+"</td>";
			rowHtml +="<td><a href=\"\" onclick=\"showModifyrankPanel("+cl.rankingCode+");return false;\">修改</a>"+  
			" | <a href=\"\" onclick=\"delRanking("+cl.rankingCode+");return false;\">删除</a></td>";
			$("#rank_tbody_dataTable").append(rowHtml);
		}
}


</script>
</head>
<body>
 <div style="text-align:left">
 <!-- main begins -->
  <div id="defineDiv" style="display:none" >
	<table>
	<tr>
	<td>指标:</td><td><input type="text" id="tindexText" name="tindexText" onclick="showIndexPanel(1,this)" style="width:150px"/></td>
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
	</select></td><td><input type="button" name="addIndexButton" id="addIndexButton" value="增加" onclick="createIndexVar()" /></td></tr>
	<tr>
	<td><input type="button" name="addButton" id="addButton" value="+" onclick="appendS(this.value)"/>
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
	<tr><td><input type="button" name="Button_point" id="Button_point" value="." onclick="appendS(this.value)"/></td>
	<td><input type="button" name="dcButton" id="dcButton" value="隐藏" /></td>
	</tr>
	</table>
</div>
<div id="indexPanel_2" class="indexPanelCss_2">
</div>

<div >

<div style="text-align:left">
		<form id="rankForm">
		 数据来源:<select name="dataSourceCode" id="dataSource" >	
		 	 <%
		 	 	  String selectdsCode = "";
				  LCEnter lce = LCEnter.getInstance();
				  List<DataSource> dsl = lce.get(StockUtil.getListKeyByDataType(StockConstants.DataSource), StockUtil.getCacheName(StockConstants.common));
				  if(dsl!=null&&dsl.size()>0)
				  {
					  
					  selectdsCode = dsl.get(0).getDataSourceCode();
					  if(rank!=null)
					  {
						  selectdsCode = MatchinfoService.getInstance().getDataSourceCodeByTsc(rank.getTableSystemCode());
					  }
					  for(int i=0;i<dsl.size();i++)
					  {
						  DataSource ds = dsl.get(i);
						  if(i==0)
						  {
							 selectdsCode = ds.getDataSourceCode();
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
 					if(!StringUtil.isEmpty(selectdsCode))
 					{
 						List<TableSystem> tsl = TableSystemService.getInstance().getTableSystemListByDs(selectdsCode);
 						 if(tsl!=null&&tsl.size()>0)
 						  {
 							  for(int i=0;i<tsl.size();i++)
 							  {
 								 TableSystem ts = tsl.get(i);
 								 if(DCenterUtil.getDataSourceCode(ts.getTableSystemCode()).equals(selectdsCode))
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
		榜单名称:<input type="text" id="rankName" name="rank.rankingName" value="<%=rank!=null?rank.getRankingName():""%>"/><br/>
		榜单编码:<input type="text" id="rankCode" name="rank.rankingCode" value="<%=rank!=null?rank.getRankingCode():""%>" readonly/><br/>
		
		榜单规则:<textarea name="rank.rankingComments" id="rankArea" value="<%=rank!=null?rank.getRankingComments():""%>" style="width:450px;height:150px" onclick="showrankDefinePanel(0,this)"></textarea><br/>
		</form>
		<input type="button" id="cleardeTextButton" name="cleardeTextButton" value="清空数据" onclick="clearObjectData('rankArea')"/>
		<input type="button" id="saverankButton" name="saverankButton" value="保存规则" /><br/><br/>	<br/><br/>
		==============================================================================================================
		 
		 
		 <div style="text-align:left">
		 <div>
		 <form id="rankQueryForm">
				榜单名称:<input type="text" id="name" name="rank.rankingName" />
			
				<input type="button" id="queryButton" name="queryButton" value="查询" onclick="queryrank()"/>
		</form>
		</div>
		<div id="div_rank_table" style="overflow:auto;height:auto">
					 <table class="datalist2">
						<thead id="rank_thead_dataTable"></thead>
						<tr>
							<td>榜单编码</td>
							<td>报表体系编码</td>
							<td>榜单名称</td>
							<td>榜单规则</td>
							<td>榜单规则注解</td>
							<td>更新时间</td>
							<td>操作</td>
						</tr>
						<tbody id="rank_tbody_dataTable"></tbody>
					 </table>
		</div>
</div>
		
</div>

</div>
 
 </div>
</body>
</html>

