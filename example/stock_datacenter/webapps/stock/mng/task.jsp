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
$(document).ready(function() {

					initYear("tmsgStartTime_year",11);
					initYear("tmsgEndTime_year",17);
					//导入文件
					$("#importButton").click(function(){
						
						 $.ajax({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
						   $.getJSON("<%=request.getContextPath()%>/file/importData",function(result){
							   if(result.success)
								  {
								  	alert("操作结束!"); 
								  }
						   });//end getJSON					
					});//end click

					//运行任务,测试阶段只处理单个公司				 
				 $("#taskButton").click(function(){
						$("#tmsgStartTime").val($('#tmsgStartTime_year').val()+'-'+$('#tmsgStartTime_jidu').val());
						$("#tmsgEndTime").val($('#tmsgEndTime_year').val()+'-'+$('#tmsgEndTime_jidu').val());
						if($("#tmsgStartTime").val()==""||$("#tmsgEndTime").val()=="")
						 {
							alert("时间不可以为空!");
							return ;
						 }

					 	var flag = confirm("您确定要运行任务吗?此操作可能时间较长");
						if(!flag)
						{
							return;
						}
						$("#tmsgCompanyInfo").val($("#tmsgCompanyArea").val());
					 	var uType = $("#taskUpdateType").val();
						//如果为部分更新,则公司信息不可以为空
					 	if(uType=='1')
					 	{
					 		if($("#tmsgCompanyInfo").val()=="")
					 			{
					 				alert("公司编码不可以为空!");
					 				return;
					 			}
					 		
					 	}
					   $.ajax({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
					   $.getJSON("<%=request.getContextPath()%>/task/computeIndex?uType="+uType, $("#taskForm").serialize(),function(result){
						   if(result.success)
							  {
							  	alert("操作结束!"); 
							  }
							  else
							   {
								 alert(result.error.msg);
							   }
					   });//end getJSON
					 });//end click

				//task更新事件
				$("#taskUpdateType").change(function(){
					
					if($("#taskUpdateType").val()=="1")
					{
						$("#taskCompanyDiv").slideDown();
						$("#cleartmsgCompanyAreaButton").slideDown();
					}
					else
					{
						$("#taskCompanyDiv").slideUp();
						$("#cleartmsgCompanyAreaButton").slideUp();
					}
				});//end change

});//end ready

//自动匹配公司名
function autoMatcher(aa)
{
	var c_data ;

	var id = aa.id;
	

		$.ajax({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.get("<%=request.getContextPath()%>/company/getCompanyListString", function(result){
				if(result.data==null)
				{
					return;
				}
				var data = result.data.split(";");
				if(data!=null)
				{
					c_data = data;
					$("#"+id+"").autocomplete(c_data);
				}
				
		  });

	
}

function clearObjectData(id)
{
	$("#"+id+"").val("");
}
function hideObjectAndUncheck(id)
{
	$("#"+id+"").slideUp();
}
function clearObject(id)
{
	$("#"+id+"").empty();
	$("#tmsgCompanyArea").val("");
}
function addCompany()
{
	var c = $("#batch_companyCode").val();
	if(c==null||c=="")
	{
		return ;
	}
	var old = $("#tmsgCompanyArea").val();
	var ne = c.split(":")[1];
	if(old!=""&&old!=null)
	{
		ne = old+"|"+c.split(":")[1];
	}
	
	$("#tmsgCompanyArea").val(ne);
	$("#companyShowDiv").append(c+"<br/>");
	$("#batch_companyCode").val("");

}
function showBatchPanel(source)
{
		$("#companyShowDiv").empty();

		//var source = document.getElementById("cfdata_companyCode");
			//获取$("#deText")的位置
		var ntop =  realOffset(source).y;
		var nleft = realOffset(source).x+285;
		$("#BatchPanel").css({'position':'absolute','top':ntop,'left':nleft,'z-index':2});
		$("#BatchPanel").slideDown();
		$("#BatchPanel").focus();
		$("#cfdata_companyCode").attr("readonly",true);
	
	
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
</script>
</head>
<body>
 <div class="content">
<div class="col-center-top-right">
	<form id="taskForm">
		起始时间:<select name="tmsgStartTime_year" id="tmsgStartTime_year" >	
		</select><select name="tmsgStartTime_jidu" id="tmsgStartTime_jidu" >	
		<option value='3-30' selected>一季度</option>
		<option value='6-30' >二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select><br/>
		结束时间:<select name="tmsgEndTime_year" id="tmsgEndTime_year" >	
		</select><select name="tmsgEndTime_jidu" id="tmsgEndTime_jidu" >	
		<option value='3-30' selected>一季度</option>
		<option value='6-30' >二季度</option>
		<option value='9-30' >三季度</option>
		<option value='12-30' >四季度</option>
		</select><br/>	
		
		<input type="hidden" id="tmsgCompanyInfo" name="tmsg.companyInfo" />
		时间间隔:<select name="tmsg.interval" id="tmsg_interval">
			<option value='3' selected>3</option>
			<option value='6'>6</option>
			<option value='9'>9</option>
			<option value='12'>12</option>
		</select><br/>
		更新类型:<select id="taskUpdateType" name="tmsg.updateType">
			<option value='0' selected>全部更新</option>
			<option value='1'>部分更新</option>
		</select><br/>
		<input type="hidden" name="tmsg.startTime" id="tmsgStartTime" value=""/>
		<input type="hidden" name="tmsg.endTime" id="tmsgEndTime" value=""/>
	</form>
	<div id="taskCompanyDiv" style="display:none">
		公司: <font size="3" color="red">格式:公司编码|公司编码,以"|"分隔</font><br/>
		<textarea id="tmsgCompanyArea" style="width:280px;height:150px" onclick="showBatchPanel(this)"></textarea><br/>
	</div>
	<div id="BatchPanel" style="display:none;background:green;z-index:3">
			<input type="text" name="batch_companyCode" id="batch_companyCode" size="23px" onkeyup="autoMatcher(this)" /><br/>
			<input type="button" name="clearCompanyButton" id="clearCompanyButton" value="清空" onclick="clearObject('companyShowDiv')"/>
			<input type="button" name="addCompanyButton" id="addCompanyButton" value="增加" onclick="addCompany()"/>
			<input type="button" name="addCompanyConfirmButton" id="addCompanyConfirmButton" value="隐藏" onclick="hideObjectAndUncheck('BatchPanel')"/>
			<div id="companyShowDiv"></div>
		</div>

	<input type="button" id="cleartmsgCompanyAreaButton" name="cleartmsgCompanyAreaButton" value="清空数据" onclick="clearObjectData('tmsgCompanyArea');clearObject('companyShowDiv')" style="display:none"/>
	<input type="button" id="taskButton" name="taskButton" value="运行任务" /><br/>
</div>
 
 </div>
</body>
</html>

