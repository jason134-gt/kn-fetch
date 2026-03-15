var ctype = "spline";
var cIndexName;
var rchart;
var ryaxis = [{
			  title:
					{
						text:''
					},
					lineWidth: 1
			},{
					title:
					{
						text:''
					},
					lineWidth: 1,
					opposite: true,
					showEmpty: false
			}];
function rebuildCharts_2()
{

	if(rchart!=null){
			rchart.destroy();
		}
	rchart = new rightChart("rightChart",ctype);
	showRightingChartId = null;
	
}

var showRightingChartId ;
//点击事件触发自动分析
function autoAnalysis(argData,id){
	if(showRightingChartId==id) return;
	var type = argData.type;
	if(type=="msg_c_or_i")
	{
			//是行业还是公司
		if(argData.code.split("\.").length==2)
		{
			showHighTag(0);
			
			var c = argData.name+":"+argData.code;
			$("#div_companyCode").val(c);
			$("#unit_select option[value='1']").attr("selected",true);
			
			//$("#startTime_year option[value='2009']").attr("selected",true);
			$("#startTime_jidu option[value='09-30']").attr("selected",true); 
			//$("#endTime_year option[value='2012']").attr("selected",true);	
			$("#endTime_jidu option[value='09-30']").attr("selected",true); 
			$("#div_indexCode").val(argData.indexName+':'+argData.indexCode);
			$("#dataSource").val('ds_0002');
			$("#tableSystem").val('ts_00003');
			var companys = $("#div_companyCode").val();
			if(companys.indexOf(":")<0) return;
			loadCompanysTags();
			rebuildCharts_2();
			showRightingChartId = id;
			baseAnalysis();
		}
		else
		{
			showHighTag(2);
			
			$("#companyalltags").empty();
			$("#companyalltags").append("<option value='"+argData.name+"'>"+argData.name+"</option>");
			$("#div_indexCode").val(argData.indexName+':'+argData.indexCode);
			rebuildCharts_2();
			getIndexSpecialValue(2);
		}
	}
	if(type=="msg_treemap_0")
	{
		showHighTag(3);
		$("#div_indexCode").val(argData.indexName+':'+argData.indexCode);
		$("#left_ctag").val(argData.name);
		showIndexTreemap();
	}
	if(type=="msg_treemap_1")
	{
		showHighTag(4);
		$("#div_indexCode").val(argData.indexName+':'+argData.indexCode);
		$("#left_ctag").val(argData.name);
		showIndexTreemap();
	}
	
}
var lcompanycode ;
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
		if(lcompanycode!=companycode)
		{
			getCompanyAlltags(companycode);
			lcompanycode = companycode;
		}
}
function getCompanyAlltags(companycode)
{
	 $("#companyalltags").empty();
	 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   $.getJSON("/stock/company/getComanyAllTags?companycode="+companycode, $("#dataForm").serialize(),function(result){
			   if(result.success)
				   {
				   		var l = result.data;
				   		for(var i=0;i<l.length;i++)
				   			{
				   				var tag = l[i];
				   				$("#companyalltags").append("<option value='"+tag+"'>"+tag+"</option>");
				   			}
				   }
				
		   });//end getJSON
}
var rightChart = function(id,type)
{
	return new Highcharts.Chart({
      chart: {
         renderTo: id,
         defaultSeriesType: type,
		 margin: [50, 40, 80, 50]
      },
      title: {
         text: '指标分析图表'
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
	  yAxis:ryaxis,
      xAxis: {
    	  	type:"datetime",//时间轴要加上这个type，默认是linear
    	    //自定义x刻度上显示的时间格式，根据间隔大小，以下面预设的小时/分钟/日的格式来显示
			dateTimeLabelFormats:
    	    {
    	     month: '%Q'
    	    }
      },
      tooltip: {
         formatter: function() {
				var unit = this.point.id.split("_")[1];
				var value = this.y;
				if(unit==0)
					value = this.y+"%";
				return '<b>'+ this.series.name+'</b><br/>'+
        	    Highcharts.dateFormat('%y-%m-%e', this.x) +': '+  
        	    value;

         }
      },
      series: []
   });
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

var unitValue =1;
function formatByUint(value)
{
	return value/unitValue;
}

//基本分析
function baseAnalysis()
{
		initUint();
		
	    //公司编码
		var companycode = $("#div_companyCode").val();
		var companyList= companycode;
		//指标编码
		var indexCode=$("#div_indexCode").val().split(":")[1];
		//超始时间
		var startTime=$("#startTime_year").val()+"-"+$("#startTime_jidu").val();
		//结束时间
		var endTime=$("#endTime_year").val()+"-"+$("#endTime_jidu").val();
		//时间间隔
		var interval=$("#div_interval").val();
		//报表体系
		var tableSystemCode=$("#tableSystem").val();
		//数据源
		var dataSourceCode=$("#dataSource").val();
		
	
	   if(companycode==""||companycode==null)	
		{
			alert("公司编码不可为空!");
			$("#div_companyCode").focus();
			return;
		}
		
		if(compareDate(startTime,endTime))
		{
			alert("开始时间必须小于结束时间!");
			return;
		}
	
		if(indexCode==null||indexCode=="")
			{
				alert("请输入指标！");
				$("#div_indexCode").focus();
				return;
			}
	   cIndexName = $("#div_indexCode").val().split(":")[0];
	   
	   var data = {"msg.companyList":companyList,"msg.companyCode":companycode,"msg.startTime":startTime," msg.endTime":endTime,"msg.indexCode":indexCode,"msg.interval":3,"msg.tableSystemCode":tableSystemCode,"msg.dataSourceCode":dataSourceCode};
	   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	   $.post("/stock/index/getIndexValueList", data,function(resultData){
			 if(resultData.success==null||!resultData.success)
				   {
					  if(resultData.error!="")
					   {
							 //alert(resultData.error.msg);
					   }
					   else
					   {
							// alert("操作失败!");
					   }
					rchart.hideLoading();
					   return;
				   }
				   if(resultData.data==""||resultData.data.length==0)
				   {
						//alert("没有打到数据!");
						return;
				   }
			if(rchart==null) 
					rchart = new rightChart("rightChart",ctype); 
			showResultRight(rchart,resultData,0);

	   });//end getJSON
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
var times =0;//运行次数
var unitName = "";
var isRemoveZero = true;
function showResultRight(rchart,result,yindex)
{

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
				  //alert("没有找到数据哦!");
				  return;
				}
				var bunit = d[i].unit;
				var ushow = "";
				if(bunit!=1&&bunit>=0) ushow = "(单位："+getUnitShow(bunit)+")";
				var name = d[i].name+":"+cIndexName+ushow;
				//cIndexName为空，则以后台给的指标名为准
				if(cIndexName=="") name = d[i].name;
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
						if(value==null) continue;
						value = formatUnit(value,bunit)
						value = Number(Number(value).toFixed(2));
						
						data.push({
						x: time ,
						y: value,
						id:time+"_"+bunit
						});
					}
					if(zNum==cd.length)
					{
						//alert("数值全为零值!");
						return;
					}
				//rchart.yAxis[yindex].setTitle({
				//text: name+ushow
				//});
				rchart.addSeries({
					name:name,
					type:ctype,
					yAxis:yindex,
					data:data
						});
				rchart.hideLoading();
			}	
}
var lctag;
//查询公司的同一行业信息并生成行业选择面板
function createIndustryPanel(id,source)
{
	if($("#industry_panel").is(":hidden"))
	{
		var company = $("#div_companyCode").val();
		if(company==null||company==""||company.split(":").length!=2)
		{
			alert("公司格式错误！");
			return;
		}
		var companycode = company.split(":")[1];
		var tag = $("#companyalltags").val();
		//初级模式
		if(mtype==0)
		{
			tag = "";
		}
		
		//高级模式
		if(mtype==1)
		{
			if(tag==""||tag=="-1")
			{
				alert("分类标签不可以为空！");
				$("#companyalltags").focus();
				return ;
			}
			if(tag ==lctag)
			{
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
			lctag=tag;
			companycode = "";
		}
		
		 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		 $.get("/stock/company/getComanyListByTags?tag="+tag+"&companycode="+companycode,function(result){
				   if(result==null)
						  return;
				var companys = result;
			$("#left_industry_panel_table").empty();
			 var d = companys.split(";");
			var i=0 ;
			//分隔每个公司
			for(i=0 ;i<d.length;i++)
				{
				var company = d[i];
				//var tt = company.chName+":"+company.companyCode;
				var tt = company;
				$("#left_industry_panel_table").append("<tr class='alt1'><td><a href='' onclick='append2CompanyText("+id+",this);return false'>"+tt+"</a></td></tr>");

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
		lctag = tag;
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
function append2CompanyText(id,source)
{
	if(id==0)
	{
		$("#div_companyCode").val(source.innerHTML);
	}
}
function showIndustryPanel(id,source)
{
		//获取$("#deText")的位置
		var ntop =  source.offsetTop+25;
		var nleft =  source.offsetLeft+60;
		$("#industry_panel").css({'position':'absolute','top':ntop,'left':nleft,'z-index':2,'background-color':'#D9D9D9'});
		$("#industry_panel").slideDown();
		$("#industry_panel").focus();
		return;
}
//同比与环比分析
//同比与环比分析
function AnalysisCAndH(type)
{
		initUint();
		//公司编码
		var companycode = $("#div_companyCode").val();
		var companyList= companycode;
		//指标编码
		var indexCode=$("#div_indexCode").val().split(":")[1];
		//超始时间
		var startTime=$("#startTime_year").val()+"-"+$("#startTime_jidu").val();
		//结束时间
		var endTime=$("#endTime_year").val()+"-"+$("#endTime_jidu").val();
		//时间间隔
		var interval=$("#div_interval").val();
		//报表体系
		var tableSystemCode=$("#tableSystem").val();
		//数据源
		var dataSourceCode=$("#dataSource").val();
		
		if(indexCode==null||indexCode=="")
			{
				alert("请输入指标！");
				$("#div_indexCode").focus();
				return;
			}
		var iCode = indexCode;
		var optName = "";
		var rule;
		var indexName = $("#div_indexCode").val();		
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
	   
	
	
		if(companycode==""||companycode==null)	
		{
			alert("公司编码不可为空!");
			$("#div_companyCode").focus();
			return;
		}
		
		if(compareDate(startTime,endTime))
		{
			alert("开始时间必须小于结束时间!");
			return;
		}
	
	   cIndexName = $("#div_indexCode").val().split(":")[0];
	   cIndexName+="-"+optName;
	   var data = {"real.ruleComments":rule,"real.companyList":companyList,"real.companyCode":companycode,"real.startTime":startTime," real.endTime":endTime,"real.indexCode":indexCode,"real.interval":3,"real.tableSystemCode":tableSystemCode,"real.dataSourceCode":dataSourceCode};

			   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
			   $.getJSON("/stock/index/realTimeAnalysis", data,function(result){
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
				 if(rchart==null) 
					rchart = new rightChart("rightChart","spline");
				ctype="spline";
				showResultRight(rchart,result,1);
			   });//end getJSON
}
function computeAvg(points)
{
		initUint();

		//公司编码
		var companycode = $("#div_companyCode").val();
		var companyList= companycode;
		//指标编码
		var indexCode=$("#div_indexCode").val().split(":")[1];
		//超始时间
		var startTime=$("#startTime_year").val()+"-"+$("#startTime_jidu").val();
		//结束时间
		var endTime=$("#endTime_year").val()+"-"+$("#endTime_jidu").val();
		//时间间隔
		var interval=$("#div_interval").val();
		//报表体系
		var tableSystemCode=$("#tableSystem").val();
		//数据源
		var dataSourceCode=$("#dataSource").val();
		
	
	   //如果与后台有请求,则认为不是图表中模型的切换
		if(companycode==""||companycode==null)	
		{
			alert("公司编码不可为空!");
			$("#div_companyCode").focus();
			return;
		}
		
		if(compareDate(startTime,endTime))
		{
			alert("开始时间必须小于结束时间!");
			return;
		}
	
		if(indexCode==null||indexCode=="")
			{
				alert("请输入指标！");
				$("#div_indexCode").focus();
				return;
			}
	   cIndexName = $("#div_indexCode").val().split(":")[0];
	   var data = {"real.companyList":companyList,"real.companyCode":companycode,"real.startTime":startTime," real.endTime":endTime,"real.indexCode":indexCode,"real.interval":3,"real.tableSystemCode":tableSystemCode,"real.dataSourceCode":dataSourceCode};
	   
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
			   $.getJSON("/stock/index/getAverageIndex", data,function(result){
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
				 if(rchart==null) 
					rchart = new rightChart("rightChart",ctype); 
				showResultRight(rchart,result,0);
			   });//end getJSON
}
function getIndexSpecialValue(type)
{
	initUint();
	var optName = "";
	var rule;
	var indexName = $("#div_indexCode").val();
	var iCode = indexName.split(":")[1];
	var mark = $("#companyalltags").val();
	var sTime=$("#startTime_year").val()+"-"+$("#startTime_jidu").val();
	//结束时间
	var eTime=$("#endTime_year").val()+"-"+$("#endTime_jidu").val();
	if(mark==-1)
	{
			alert("请先选择分类信息！");
			$("#companyalltags").focus();
			return;
	}
	var indexcode = $("#div_indexCode").val().split(":")[1];

   cIndexName = $("#div_indexCode").val()+"--"+optName;
	  
		   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		   $.getJSON("/stock/index/getMaxMinAvgMid?type="+type+"&indexcode="+indexcode+"&sTime="+sTime+"&eTime="+eTime+"&mark="+mark,function(result){
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
			if(rchart==null) 
					rchart = new rightChart("rightChart",ctype); 
			showResultRight(rchart,result,0);
			
		   });//end getJSON
}
//季度半年年化
function qhy(type)
{
		initUint();
		
		//公司编码
		var companycode = $("#div_companyCode").val();
		var companyList= companycode;
		//指标编码
		var indexCode=$("#div_indexCode").val().split(":")[1];
		//超始时间
		var startTime=$("#startTime_year").val()+"-"+$("#startTime_jidu").val();
		//结束时间
		var endTime=$("#endTime_year").val()+"-"+$("#endTime_jidu").val();
		//时间间隔
		var interval=$("#div_interval").val();
		//报表体系
		var tableSystemCode=$("#tableSystem").val();
		//数据源
		var dataSourceCode=$("#dataSource").val();
		
	
	   //如果与后台有请求,则认为不是图表中模型的切换
		if(companycode==""||companycode==null)	
		{
			alert("公司编码不可为空!");
			$("#div_companyCode").focus();
			return;
		}
		
		if(compareDate(startTime,endTime))
		{
			alert("开始时间必须小于结束时间!");
			return;
		}
	
		if(indexCode==null||indexCode=="")
			{
				alert("请输入指标！");
				$("#div_indexCode").focus();
				return;
			}
	   cIndexName = $("#div_indexCode").val().split(":")[0];
	   var data = {"msg.companyList":companyList,"msg.companyCode":companycode,"msg.startTime":startTime," msg.endTime":endTime,"msg.indexCode":indexCode,"msg.interval":3,"msg.tableSystemCode":tableSystemCode,"msg.dataSourceCode":dataSourceCode};
	  
	   cIndexName = $("#div_indexCode").val();
	   $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	   $.getJSON("/stock/index/getIndexQhy?type="+type, data,function(result){
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
			if(rchart==null) 
					rchart = new rightChart("rightChart",ctype); 
			showResultRight(rchart,result,0);
		  	//保存此次查询结果
			keepResultArray[keepResultArray.length] = result;
	   });//end getJSON
}
function showIndexTreemap()
{
		var time = "2012-09-30";
		//指标编码
		var indexcode=$("#div_indexCode").val().split(":")[1];
		if(indexcode==null||indexcode=="")
			{
				alert("请输入指标！");
				$("#div_indexCode").focus();
				return;
			}
		var indexname = $("#div_indexCode").val().split(":")[0];
		var tname = indexname;
		doAddIndex2Chart(time,indexcode,mtype,tname)
}
function leftinitctagSpan(tctagname)
{
	if(tctagname=="-1")
		$("#left_ctag").val("所有行业");
	else
		$("#left_ctag").val(tctagname);
}
function initIndustryBycode(id,type,industrycode)
{
	 //先清空
	$("#"+id+"").empty();
	if(id=="left_industry_0")
		$("#"+id+"").append("<option value='-1' selected>所有行业</option>");
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.get("/stock/industry/getIndustryByTypeAndCode",{"type":"z","industrycode":industrycode}, function(result){
		  var rd = result.data;
		  if(rd==null)
			  return ;
			addIndustry2Select(id,rd);
	  });	
}
function addIndustry2Select(id,sdata)
{ 
	 	if(sdata==null)
	 		return ;
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
}
function doAddIndex2Chart(time,indexcode,type,tname)
{
	if(type==4)
		getAllIndustryByParentTag(time,indexcode,tname);
	if(type==3)
		getIndexDataOfTagAllCompany(time,indexcode,tname);

}
//行业
function getAllIndustryByParentTag(time,indexcode,tname)
{
	var type = "1";
	var tagname = $("#left_ctag").val();
	if(tagname=="所有行业") tagname = "-1";
	var time = "2012-06-30";
	if(tagname==null) tagname = -1;
	$.post("/stock/scatter/getchildtagByptag",{"tag":tagname,"time":time,"type":type,"indexcode":indexcode}, function(result){
		  var rd = result.data;
		  if(rd==null)
			{
			  alert("没有相关行业！");
			  return;
			}
			var scatter = rd;
			var scale = scatter.indexavg;
			var ddl = scatter.pointList;
			var da = new Array();
			da.push(['Location', 'Parent', 'Market trade volume (size)', 'Market increase/decrease (color)']);
			var p = tagname+"同级行业-"+tname+"-规模";
			da.push([p,    null,                 0,                               0]);
			if(ddl.length==0)
				return;
			for(var i=0;i<ddl.length;i++)
			{
				var d = ddl[i];
				var cname = d.companyname;
				var value = d.v/scale*100;
				value = value.toFixed(0);
				
				//cname = cname+":"+(d.v/10000).toFixed(2);
				//生成色彩
				var color = i;
				if(i%2==0) 
					color = i*0.5;
				  else
				    color = -i*0.5;
				da.push([cname,p,parseInt(value),color]);
			}
			
			var gdata = google.visualization.arrayToDataTable(da);
			var tree = new google.visualization.TreeMap(document.getElementById('rightChart'));
			var ttf = function(){
					var scode = null;
					var sName = gdata.getValue(tree.getSelection()[0].row,0);
					if(sName.indexOf("规模")>0) return;
					for(var i=0;i<ddl.length;i++)
					{
						var d = ddl[i];
						var cname = d.companyname;
						if(cname==sName)
							{
								scode = d.companycode;
								break;
							}
					}
					window.open("/stock/html/industry_analysis.html?tagcode="+scode+"&tagname="+escape(sName));
				};
				google.visualization.events.addListener(tree, 'select',ttf); 
				tree.draw(gdata, {
				  minColor: '#f00',
				  midColor: '#ddd',
				  maxColor: '#0d0',
				  headerHeight: 15,
				  fontColor: 'black',
				  showScale: false});
	  });
}
function getIndexDataOfTagAllCompany(time,indexcode,tname)
{
	var tagname = $("#left_ctag").val();
	if(tagname=="所有行业") tagname = "-1";
	$.ajax({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post("/stock/index/getIndexOneTimeOneTagCompany",{"time":time,"tag":tagname,"indexcode":indexcode}, function(result){
		  var rd = result.data;
		  if(rd==null)
			{
			  alert("没有相关公司！");
			  return;
			}
			var ddl = rd.data;
			var total = rd.total;
			var da = new Array();
			da.push(['Location', 'Parent', 'Market trade volume (size)', 'Market increase/decrease (color)']);
			var p = tagname+"行业-"+tname+"-规模";
			da.push([p,    null,                 0,                               0]);
			//此处用均值缩放
			total = total/ddl.length;
			for(var i=0;i<ddl.length;i++)
			{
				var d = ddl[i];
				var cname = d.companyname;
				var value = d.v/total*100;
				value = value.toFixed(0);
				
				//cname = cname+":"+(d.v/10000).toFixed(2);
				//生成色彩
				var color = i;
				if(i%2==0) 
					color = i*0.0005;
				  else
				    color = -i*0.0005;
				da.push([cname,p,parseInt(value),color]);
			}
			var gdata = google.visualization.arrayToDataTable(da);
			var tree = new google.visualization.TreeMap(document.getElementById('rightChart'));
				var ttf = function(){
					var scode = null;
					var sName = gdata.getValue(tree.getSelection()[0].row,0);
					if(sName.indexOf("规模")>0) return;
					for(var i=0;i<ddl.length;i++)
					{
						var d = ddl[i];
						var cname = d.companyname;
						if(cname==sName)
							{
								scode = d.companycode;
								break;
							}
					}
					window.open("/stock/html/stocks_analysis.html?stockcode="+scode+"&stockname="+escape(sName));
				};
				google.visualization.events.addListener(tree, 'select',ttf); 
				tree.draw(gdata, {
				  minColor: '#f00',
				  midColor: '#ddd',
				  maxColor: '#0d0',
				  headerHeight: 15,
				  fontColor: 'black',
				  showScale: false});
	  });	
}	
function changeType(type)
{
	ctype=type;
}
var mtype = 0;
function showHighTag(type)
{
	if(type!=2)
		mtype=type;
	$("#c_div").slideDown();
	if(type==0)
	{
		$("#unitDSTS").show();
		$("#treemap_0").slideUp();
		$("#high_0").slideUp();
		$("#high_1").slideUp();
		$("#high_2").slideUp();
		$("#left_industry_Div").slideUp();
		$("#m0").css("background","red");
		$("#m1").css("background","");
		$("#m2").css("background","");
		$("#m3").css("background","");
		$("#m4").css("background","");
	}
	if(type==1)
	{
		$("#unitDSTS").show();
		$("#treemap_0").slideUp();
		$("#high_0").toggle();
		$("#high_1").toggle();
		$("#high_2").toggle();
		$("#left_industry_Div").slideDown();
		$("#m1").css("background","red");
		$("#m0").css("background","");
		$("#m2").css("background","");
		$("#m3").css("background","");
		$("#m4").css("background","");

	}
	if(type==2)
	{
		$("#left_industry_Div").slideDown();
		$("#treemap_0").slideUp();
		$("#unitDSTS").show();
		$("#c_div").slideUp();
		$("#high_0").slideUp();
		$("#high_1").slideUp();
		$("#high_2").slideUp();
		$("#m2").css("background","red");
		$("#m1").css("background","");
		$("#m0").css("background","");
		$("#m3").css("background","");
		$("#m4").css("background","");
	}
	if(type==3)
	{
		$("#treemap_0").slideDown();
		$("#unitDSTS").slideUp();
		$("#left_industry_Div").slideUp();
		$("#c_div").hide();
		$("#high_0").slideUp();
		$("#high_1").slideUp();
		$("#high_2").slideUp();
		$("#m3").css("background","red");
		$("#m4").css("background","");
		$("#m1").css("background","");
		$("#m0").css("background","");
		$("#m2").css("background","");
		$("#left_ctag").removeAttr("disabled");
		$("#left_ctag").css("background","");
		$("#company_radio").attr("checked","true");
	}
	if(type==4)
	{
		$("#treemap_0").slideDown();
		$("#unitDSTS").slideUp();
		$("#left_industry_Div").slideUp();
		$("#c_div").hide();
		$("#high_0").slideUp();
		$("#high_1").slideUp();
		$("#high_2").slideUp();
		$("#m3").css("background","red");
		$("#m4").css("background","");
		$("#m1").css("background","");
		$("#m0").css("background","");
		$("#m2").css("background","");
		$("#left_ctag").attr("disabled","true");
		$("#left_ctag").css("background","gray");
		$("#industry_radio").attr("checked","true");
	}
	
}
function changeleftMtype(type)
{
	mtype=type;
	if(type==4)
	{
		$("#left_ctag").attr("disabled","true");
		$("#left_ctag").css("background","gray");
	}
	if(type==3)
	{
		$("#left_ctag").removeAttr("disabled");
		$("#left_ctag").css("background","");
	}
		
}
