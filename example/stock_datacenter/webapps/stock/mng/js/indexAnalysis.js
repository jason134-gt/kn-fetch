//显示规则定义面板
function showDefineIndexPanel(ntop,nleft,ihtml,thtml)
{
			$("#defineDiv").css({'position':'absolute','top':ntop,'left':nleft});
			$("#defineDiv").slideDown();
			$("#defineDiv").focus();
			//加上指标数据
			$("#deIndexS").empty();
			$("#deIndexS").append(ihtml);
			//加上模板数据
			$("#deTemplateS").empty();
			$("#deTemplateS").append(thtml);
}
//工作区面板切换
var optType=0;
function showPanel(type)
{
	//由动态面板(type=3)切换到普通面板(type=0,1,2),或是由普通切换到动态都需要重建表,普通之间切换不重建
	if(optType==3&&type!=3)
	{
		optType = type;
		rebuildCharts(type);
	}
	if(optType!=3&&type==3)
	{
		optType = type;
		rebuildCharts(type);
	}
	optType = type;
	$("#workTitle").empty();
	if(type==0)
	{
		$("#workTitle").append("基本分析");
		$("#workDiv_realtime").slideUp();
		$("#workDiv_base").slideDown();
		$("#workDiv_template").slideUp();
		$("#workDiv_dynaic").slideUp();
		hiddenPopPanel();
	}
	if(type==1)
	{
		$("#workTitle").append("实时分析");
		$("#workDiv_realtime").slideDown();
		$("#workDiv_base").slideUp();
		$("#workDiv_template").slideUp();
		$("#workDiv_dynaic").slideUp();
		hiddenPopPanel();
	}
	if(type==2)
	{
		$("#workTitle").append("模板分析");
		$("#workDiv_realtime").slideUp();
		$("#workDiv_base").slideUp();
		$("#workDiv_template").slideDown();
		$("#workDiv_dynaic").slideUp();
		hiddenPopPanel();
	}
	if(type==3)
	{
		$("#workTitle").append("动态分析");
		$("#workDiv_realtime").slideUp();
		$("#workDiv_base").slideUp();
		$("#workDiv_template").slideUp();
		$("#workDiv_dynaic").slideDown();
		hiddenPopPanel();
	}
}
//=================================================mycharts.js==init===================================


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
//隐藏指标面板
function hiddenIndexPanel()
{
	$('#indexPanel').slideUp();
}
//多个指标选择框的编码
var textCode = 0;
var currentObj=null;
//取选择面板中,所选择的指标
function getIndexPanelSelectValue(o)
{
	if(o.selectedIndex!=-1)
	{
		var text = o.options[o.selectedIndex].text;
		var x=text+":"+o.value;
		//把选择指标信息加到
		if(textCode==0)
		{
			$("#div_indexCode").val(x);
		}
		//cIndexName=x;
		if(textCode==1)
		{
			$("#tindexText").val(x);
		}
		if(textCode==2)
		{
			currentObj.value = x;
		}
	
	}
	
}

//=================================================mycharts.js==init==end=================================================
//=================================================mycharts.js==base action===============================================
//根据类型清空不同工作区的数据
function clearData()
{
	if(optType==1)
	{
		$("#realTimeDeText").val("");
	}
	if(optType==3)
	{
		clearDynaicData();
	}
}
//清除动态区数据
function clearDynaicData()
{
	$("#bodyData").val("");
}
//清空图表

function rebuildCharts()
{
	//销毁数组对象
	keepResultArray = new Array();

	if(optType==0)
	{
		chart.destroy();
		chart = new myChart('divCharts');
	}
	if(optType==1)
	{
		chart.destroy();
		chart = new myChart('divCharts');
		
	}
	if(optType==2)
	{
		chart.destroy();
		chart = new myChart('divCharts');
		
	}
	if(optType==3)
	{
		chart.destroy();
		chart = new myChartDynaic('divCharts');
	}
	//清除表格数据
	$("#thead_dataTable").empty();
	$("#tbody_dataTable").empty();
	$("#indexAnalysisDiv").empty();
	$("#cfpanel_thead_dataTable").empty();
	$("#cfpanel_tbody_dataTable").empty();
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
//定义规则
var exp = "";//规则表达式
function appendS(o)
{
	var dst ;
	//区分事件发生在哪个面板
	if(indexDefineFlag==0)
	{
		dst = $("#deText");
	}
	else
	{
		dst = $("#realTimeDeText");
	}

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
//改变图型的显示模型
function changeModel(stype)
{
	isChangeMode = true;
	type=stype;
	chart.destroy();
	if(optType==3)
	{
		chart = new myChartDynaic('divCharts');
		paraseDynaicTableData(header,body);
	}
	else
	{
		chart = new myChart('divCharts');
		var i =0;
		for(i=0;i<keepResultArray.length;i++)
		{
			var keepResult = keepResultArray[i];
			paraseData(keepResult);
		}
	}	
	
}
//隐藏各弹出面板
function hiddenPopPanel()
{
	$("#indexPanel").slideUp();
	 $("#defineDiv").slideUp();
}
//=================================================mycharts.js==base action===============================================
//=================================================mycharts.js==data analysis region======================================


//=================================================mycharts.js==opt==start==============================================
   var keepResultArray = new Array();
   //缓存一份所有指标名列表
	var indexHtml;
	//保存当前分析的指示名
	var cIndexName;
	//规则定义面板标识 0:规则定义区,1:实时分析区
	var indexDefineFlag = 0;
function analysisEventHandler()
{
	if(optType==0)
	{
		baseAnalysis();
	}
	if(optType==1)
	{
		if($("#realTimeDeText").val()=="")
		{
			alert("输入数据为空!");
			return;
		}
		realTimeAnalysis();
		
	}
	if(optType==2)
	{
		templateAnalysis();
	}
	if(optType==3)
	{
		if($("#bodyData").val()=="")
		{
			alert("输入数据为空!");
			return;
		}
		createTable();
	}
}

function append2CompanyText(id,source)
{
	if(id==0)
	{
		$("#div_companyCode").val(source.innerHTML);
	}
	if(id==1)
	{
		if($("#batch_checkBox").is(":checked"))
		{
			$("#batch_companyCode").val(source.innerHTML);
		}
		else
		{
			$("#cfdata_companyCode").val(source.innerHTML);
		}
		
	}
	
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

var isChangeMode = false;
var times =0;//运行次数
var unitName = "";
var isRemoveZero = true;
function paraseData(result)
{
	times +=1;
	$("#thead_dataTable").empty();
	$("#tbody_dataTable").empty();
	//是否已把时间加到表格中
	 var flag = false;
	 var tdvar="";
	 var timevar="" ;
	 var d = result.data;
		var i=0 ;
		var j=0;
		//分隔每个公司
		for( i=0 ;i<d.length;i++)
			{
			var cd = d[i].data;
			if(cd==null||cd.length==0)
				{
				  alert("没有找到数据哦!");
				  return;
				}
				var us = "";
				if(unitName!="")
				{
					us="("+unitName+")";
				}
				var name = d[i].name+us;
				//cIndexName为空，则以后台给的指标名为准
				if(cIndexName=="") name = d[i].name+us;
				var data = [];
				var zNum=0;//记录值的数
				for(j=0;j<cd.length;j++)
					{
						//需要整型,字符串不显示
						var time = cd[j].time;
						var value = cd[j].value;
						if(isRemoveZero&&Math.abs(value)==0)
						{
							zNum++;
							continue;
						}
						if(!flag)
						{
							timevar +="<th>"+Highcharts.dateFormat('%Y-%m-%e', time)+"</th>";
						}

						value = formatByUint(value);
					
						tdvar +="<td>"+value.toFixed(2)+"</td>";
						data.push({
						x: time ,
						y: value
						});
					}
					if(zNum==cd.length)
					{
						alert("数值全为零值!");
						return;
					}
				chart.addSeries({
					name:name,
					data:data
						});
					//如果只是图表中显示模型的切换,则不需要生成表格
					if(!isChangeMode)
					{
						var tableTempId = "indexAnalysisTable_"+times;
						$("#indexAnalysisDiv").append("<table class='datalist2' id='"+tableTempId+"'></table><br/>");
						if(!flag)
						{
						//构建数据表格,加上时间表头
						$("#"+tableTempId+"").append("<thead><tr class='alt1'><th>公司名</th>"+timevar+"</tr></thead>");
						flag = true;
						}
					//加上一行数据
					$("#"+tableTempId+"").append("<tbody><tr class='alt1'><td>"+name+"</td>"+tdvar+"</tr></tbody>");
					}
				
					tdvar = "";
			
			}	
}
//解析数据,并以图表展示
function showResult(result)
{
	//chart.destroy();
	//chart = new myChart();
   	paraseData(result);	
}
var unitValue =1;
function formatByUint(value)
{
	//如果绝对值是小于1的数,则不做转换
	//if(Math.abs(value)/unitValue<0.0001)
	//{
		//return value;
	//}
	return value/unitValue;
}
//***************dynaic analysis event*****************
var tabledata;
//取表头
var header  ;
//取行
var body ;
var name;
function createTable()
{
	$("#thead_dataTable").empty();
	$("#tbody_dataTable").empty();

	tabledata ="";
    tabledata = $("#bodyData").val().split("\n");
	var i;
	for(i=0;i<tabledata.length;i++)
		{
			if(tabledata[i]=="")
			{
				continue;
			}
			var tcells = tabledata[i].split("\t");
			if(i==0)
				{
					appendData2Table("thead_dataTable",tcells,0,i);
				}
			else
				{
				    appendData2Table("tbody_dataTable",tcells,1,i);
				}
			
		}
}
function appendData2Table(id,cells,type,rowid)
{
	if(rowid==20)
	{
		alert();
	}
	$("#"+id+"").append("<tr class='alt1'>");
	var j ;
	for(j=0;j<cells.length;j++)
		{
		if(cells[j]!="")
			{
				if(type==0)
				{
					$("#"+id+"").append("<th>"+cells[j]+"</th>");
				}
				else
				{
					$("#"+id+"").append("<td>"+cells[j]+"</td>");
				}
				
			}
		
		}
	if(type==0)
	{
		$("#"+id+"").append("<th>操作</th>");
	}
	else
	{
		$("#"+id+"").append("<td><input type='button' onclick='createChartByRows("+rowid+")' value='生成图表' /></td>");
	}
	$("#"+id+"").append("</tr>");
}
function createChartByRows(rowid)
{
	var head = tabledata[0].split("\t");
	var head2 =[];
	var i;
	//默认从1开始,每行的第一个字段为指标名
	for(i=1;i<head.length;i++)
		{
			var h=head[i];
			if(h!="")
			{
				head2.push(h);
			}
			
		}
	var body1 = tabledata[rowid].split("\t");
	header = head2;
	body=body1;
	paraseDynaicTableData(head2,body1);
}
function paraseDynaicTableData(header,body)
{
	var hd = header;	
	var cells = body;
	var i =0;
	var data = [];
	//默认从1开始,每行的第一个字段为指标名
	name = cells[0];
	for(i=1;i<cells.length;i++)
		{
		if(cells[i]!="")
			{
				//需要整型,字符串不显示
		var value = parseInt(cells[i]);
					data.push({
					y: value
					});
			}
		

		}
	chart.xAxis[0].setCategories(hd);
	chart.addSeries({
		name:name,
		data:data
			});
}

function showBatchPanel()
{
	$("#companyShowDiv").empty();
	if($("#batch_checkBox").is(":checked"))
	{
		var source = document.getElementById("cfdata_companyCode");
			//获取$("#deText")的位置
		var ntop =  realOffset(source).y+25;
		var nleft = realOffset(source).x;
		$("#BatchPanel").css({'position':'absolute','top':ntop,'left':nleft,'z-index':2});
		$("#BatchPanel").slideDown();
		$("#BatchPanel").focus();
		$("#cfdata_companyCode").attr("readonly",true);
	}
	else
	{
		$("#BatchPanel").slideUp();
		$("#cfdata_companyCode").attr("readonly",false);
	}
	
}
function addCompany()
{
	var c = $("#batch_companyCode").val();
	if(c==null||c=="")
	{
		return ;
	}
	var old = $("#cfdata_companyCode").val();
	var ne = c;
	if(old!=""&&old!=null)
	{
		ne = old+"|"+c;
	}
	
	$("#cfdata_companyCode").val(ne);
	$("#companyShowDiv").append(c+"<br/>");
	$("#batch_companyCode").val("");

}
function hideObject(id)
{
	$("#"+id+"").slideUp();
}
function clearObject(id)
{
	$("#"+id+"").empty();
	$("#cfdata_companyCode").val("");
}
function doScroll()
{
	//取父面板位置
		var source = document.getElementById("div_cfpanel_table");
		//获取$("#deText")的位置
		var ntop =  realOffset(source).y;
		var nleft =  realOffset(source).x;
		alert(source.style.width());
		$("#cfpanel_thead_dataTable").css({'position':'relative','top':ntop,'left':nleft});
}

//**************dynaic analysis event*****************

//=================================================mycharts.js==end==end==============================================