var trendschart = function(id,yaxis) {
		var cwidth = $("#"+id).width()-5;
		return new Highcharts.Chart({
			chart : {
				renderTo : id,
				width : cwidth,
			},
			title: {
	         text: "财务指标趋势图"
			},
			yAxis :yaxis,
			xAxis : {
				type : "datetime",//时间轴要加上这个type，默认是linear
				//自定义x刻度上显示的时间格式，根据间隔大小，以下面预设的小时/分钟/日的格式来显示
				dateTimeLabelFormats : {
					month : '%Q'
				},
				title: {
					text: null
				}
			},
			plotOptions : {
				series : {
					pointWidth : 11,
					borderWidth : 0,
					shadow : false
				}
			},
			legend : {
				align : 'center',
				maxHeight:40,
				verticalAlign : 'bottom',
				backgroundColor : '#FFFFFF',
				itemStyle: {
                 fontSize: '10px'
				}
			},
			credits : {
				enabled : false
			},
			series : []
		});
}
var zscatterchart = function(id,xtitle,ytitle,showLegend)
{
	var cwidth = $("#"+id).width()-5;
	return new Highcharts.Chart({
	    chart: {
	        renderTo: id,
			zoomType: 'xy',
			width:cwidth
	    },
	    xAxis: {
	    	title: {
	            text: xtitle
	         }
	    },
	    yAxis: {
		     title: {
		        text: ytitle
		     }
	    },
		legend : {
				enabled:showLegend,
				align : 'center',
				maxHeight:40,
				verticalAlign : 'bottom',
				backgroundColor : '#FFFFFF',
				itemStyle: {
                 fontSize: '12px'
				}
		},
		tooltip: {
            formatter: function() {
        	    return '<b>'+this.point.name+':['+ this.x +'</b> , <b>'+ this.y +']</b>';

         }
        },
		plotOptions: {
            series: {
                cursor: 'pointer',
                point: {
                    events: {
                        click: function() {
							var companycode = this.id;
							var companyname = this.name;
							window.open("/stock/company_main.html?companycode="+this.id+"&companyname="+escape(this.name));
                        }
                    }
                }
            }
        },
	    title: {
	        text: "指标竞争力分析图"
	    },
	    series: []
	});
}
function createScatterChart_common(id,tag,company,xindex,yindex,time,xtitle,ytitle,showLegend,markcompanycode)
{
	try{
	var cname=""  ;
	var ccode=""  ;
	if(tag==""&&company=="")
	{
		 cname = curcompanyname;
		 ccode = curcompanycode;
	}
	if(company=="")
	{
		markcompanycode = "";
	}
	if(company!=null&&company!="")
	{
		 cname = company.split(":")[0];
		 ccode = company.split(":")[1];
	}
	var xindexcode=xindex.split(":")[1];
	var	yindexcode=yindex.split(":")[1];
	var data={
		tag:tag,
		companycode:ccode,
		xindexcode:xindexcode,
		yindexcode:yindexcode,
		time:time 
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/scatter/getScatterIndexData?"+$.param(data), function(result){
		  var rm = scret;
		  if(rm==null||rm.vl==null||rm.vl=="")
			{
			  return;
			}
		var dl = rm.vl.split("~");
		var pointdata = new Array();
		var xavg =rm.xavg;
		var yavg =rm.xavg;
		var chart = new zscatterchart(id,xtitle,ytitle,showLegend);
		for(var i=0;i<dl.length;i++)
		{
				//分隔指标编码与名字
				var cd = dl[i].split("^");
				if(cd=="") continue;
				var companyname = cd[0];
				var companycode = cd[1];
				var x = cd[2];
				var y = cd[3];
				x=parseFloat(x).toFixed(2);
				y=parseFloat(y).toFixed(2) ;
				
				
			   if(markcompanycode==""&&i==0)
			   {
					markcompanycode = cd[1];
			   }
					
				if(companycode==markcompanycode)
				{
					
					pointdata.push({
					id:companycode,
					name:companyname,
					x: parseFloat(x),
					y: parseFloat(y),
					id:companycode,
					dataLabels: {
						enabled: true,
						formatter: function() {
								return '<b>'+this.point.name;
						 }
					},
					marker: {
						fillColor: '#FFFFFF',
						symbol: 'url(/stock/images/sun.png)'
						}
					});	
			  
				}
				else
				{
					pointdata.push({
					id:companycode,
					name:companyname,
					x: parseFloat(x),
					y: parseFloat(y),
					id:companycode
					});	
				}
		}

	chart.addSeries({
		type: 'scatter',
		name: "同行公司",
        data: pointdata,
    	turboThreshold:3000
			});
	  });	

	 }catch(e)
	 {
		console.log(e);
	 }
}
function createTrendsAnalysis(id,company,xindexs,yindexs,stime,etime,chart)
{
	try{
	var cchart = chart;
	var companyname = company.split(":")[0];
	var companycode = company.split(":")[1];
	var data={
		companycode:companycode,
		xindexs:xindexs,
		yindexs:yindexs,
		stime:stime,
		etime:etime
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/trends/getOneChartGroupIndexTrendsData?"+$.param(data), function(result){
		  var rm = taret;
		  if(rm==null||rm.vl==null||rm.vl=="")
			{
			  return;
			}
		var dl = rm.vl.split("~");
		
		var cname =rm.cname;
		var yaxis = [{
					title:
					{
						text:null
					},
					lineWidth: 1,
					opposite : 0//0=左侧，1=右侧
					},{
					title:
					{
						text:null
					},
					lineWidth: 1,
					opposite : 1//0=左侧，1=右侧
					}];	
		/*
		if(chart!=null)
			cchart=chart;
		*/
		if(cchart==null)
			cchart = new trendschart(id,yaxis);
		
		if(yindexs!=""&&cchart.yAxis.length<2)
		{
			cchart.addAxis({
					title:
					{
						text:null
					},
					lineWidth: 1,
					opposite: 1
			});
		}
		for(var i=0;i<dl.length;i++)
		{
				if(dl[i]=="") continue;
				//分隔指标编码与名字
				var cd = dl[i].split("&");
				if(cd=="") continue;
				var indexname = cd[0].split(":")[0];
				var indexcode = cd[0].split(":")[1];
				var yindex = parseInt(cd[1]);
				var stype = cd[2];
				var vl = cd[3].split("|");
				var pointdata = new Array();
				for(var k=0;k<vl.length;k++)
				{
					if(vl[k]=="") continue;
					var y=parseFloat(vl[k].split("^")[0]).toFixed(2);
					var time=vl[k].split("^")[1];
					pointdata.push({
					x: parseInt(time),
					y: parseFloat(y)
					});	
				}
			
				cchart.addSeries({
						type: stype,
						name:companyname+":"+indexname,
						yAxis : yindex,
						data: pointdata
							});
		}

	
	  });	

	 }catch(e)
	 {
		console.log(e);
	 }
}
var cur_ind_trends_chart;
function createIndustryTrendsAnalysis(id,tag,xindexs,yindexs,stime,etime,type,chart)
{
	
	try{
	var data={
		tag:tag,
		xindexs:xindexs,
		yindexs:yindexs,
		stime:stime,
		etime:etime,
		type:type
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/trends/getMultIndexIndustryTrendsData?"+$.param(data), function(result){
		  var rm = taret;
		  if(rm==null||rm.vl==null||rm.vl=="")
			{
			  return;
			}
		var dl = rm.vl.split("~");
		var yaxis = [{
					title:
					{
						text:null
					},
					lineWidth: 1,
					opposite : 0//0=左侧，1=右侧
					},{
					title:
					{
						text:null
					},
					lineWidth: 1,
					opposite : 1//0=左侧，1=右侧
					}];	
		if(chart==null)
		   chart = new trendschart(id,yaxis);
		if(yindexs!=""&&chart.yAxis.length<2)
		{
			chart.addAxis({
					title:
					{
						text:null
					},
					lineWidth: 1,
					opposite: 1
			});
		}
		for(var i=0;i<dl.length;i++)
		{
				if(dl[i]=="") continue;
				//分隔指标编码与名字
				var cd = dl[i].split("&");
				if(cd=="") continue;
				var indexname = cd[0].split(":")[0];
				var indexcode = cd[0].split(":")[1];
				var yindex = parseInt(cd[1]);
				var stype = cd[2];
				var vl = cd[3].split("|");
				var pointdata = new Array();
				for(var k=0;k<vl.length;k++)
				{
					if(vl[k]=="") continue;
					var y=parseFloat(vl[k].split("^")[0]).toFixed(2);
					var time=vl[k].split("^")[1];
					pointdata.push({
					x: parseInt(time),
					y: parseFloat(y)
					});	
				}
			
				chart.addSeries({
						type: stype,
						name:tag+":"+indexname,
						yAxis : yindex,
						data: pointdata
							});
		}

	
	  });	

	 }catch(e)
	 {
		console.log(e);
	 }
}
var cur_tm_index="年化营业收入:1902";//当前的treemap图所选的指标
var cur_industry="";//当前标签
var tm_tree;
var tm_gdata;
function createTmChart(chartid,industryTag,index,time,companycode)
{
	var indexname = index.split(":")[0];
	var indexcode = index.split(":")[1];
	cur_tm_index = index;
	var data={
		tag:industryTag,
		companycode:companycode,
		indexcode:indexcode,
		time:time 
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/treemap/getCompanysIndexDataOfIndustry?"+$.param(data), function(result){
		  var rm = tmret;
		  if(rm==null||rm.vl==null||rm.vl=="")
			{
			  //alert("没有相关行业！");
			  return;
			}
			var tagname = rm.tag;
			cur_industry = tagname;
			var scale = rm.avg;
			var ddl = rm.vl.split("~");
			var da = new Array();
			da.push(['Location', 'Parent', 'Market trade volume (size)', 'Market increase/decrease (color)']);
			var p = indexname+"规模比较("+tagname+")";
			da.push([p,    null,                 0,                               0]);
			if(ddl.length==0) return;
			for(var i=0;i<ddl.length;i++)
			{
				if(ddl[i]=="") continue;
				var d = ddl[i].split("^");
				var cname = d[0];
				var value = d[2]/scale*100;
				value = value.toFixed(0);
				//生成色彩
				var color = i;
				if(i%2==0) 
					color = i*0.5;
				  else
				    color = -i*0.5;
				da.push([cname,p,parseInt(value),color]);
			}
			if(da.length>0)
			{
				var gdata = google.visualization.arrayToDataTable(da);
				var tree = new google.visualization.TreeMap(document.getElementById(chartid));
				tm_tree=tree;
				tm_gdata=gdata;
				var ttf = function(){
					var scode = null;
					var sName = gdata.getValue(tree.getSelection()[0].row,0);
				
					for(var i=0;i<ddl.length;i++)
					{
						var d = ddl[i].split("^");
						var cname = d[0];
						if(cname==sName)
							{
								scode = d[1];
								break;
							}
					}
					
					window.open("/stock/company_main.html?companycode="+scode+"&companyname="+escape(sName));
				};
				google.visualization.events.addListener(tree, 'select',ttf); 
				tree.draw(gdata, {
				  minColor: '#f00',
				  midColor: '#ddd',
				  maxColor: '#0d0',
				  headerHeight: 15,
				  fontColor: 'black',
				  showScale: false});
			}
	  });
}

function createTmChart_common(chartid,industryTag,index,time,companycode)
{
	var indexName = index.split(":")[0];
	var indexcode = index.split(":")[1];
	var data={
		tag:industryTag,
		companycode:companycode,
		indexcode:indexcode,
		time:time 
	};
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript("/stock/treemap/getCompanysIndexDataOfIndustry?"+$.param(data), function(result){
		  var rm = tmret;
		  if(rm==null||rm.vl==null||rm.vl=="")
			{
			  //alert("没有相关行业！");
			  return;
			}
			var tagname = rm.tag;
			var scale = rm.avg;
			var ddl = rm.vl.split("~");
			var da = new Array();
			da.push(['Location', 'Parent', 'Market trade volume (size)', 'Market increase/decrease (color)']);
			var p = indexName+"规模比较("+tagname+")";
			da.push([p,    null,                 0,                               0]);
			if(ddl.length==0) return;
			for(var i=0;i<ddl.length;i++)
			{
				if(ddl[i]=="") continue;
				var d = ddl[i].split("^");
				var cname = d[0];
				var value = d[2]/scale*100;
				value = value.toFixed(0);
				//生成色彩
				var color = i;
				if(i%2==0) 
					color = i*0.5;
				  else
				    color = -i*0.5;
				da.push([cname,p,parseInt(value),color]);
			}
			if(da.length>0)
			{
				var gdata = google.visualization.arrayToDataTable(da);
				var tree = new google.visualization.TreeMap(document.getElementById(chartid));
				var ttf = function(){
					var scode = null;
					var sName = gdata.getValue(tree.getSelection()[0].row,0);
				
					for(var i=0;i<ddl.length;i++)
					{
						var d = ddl[i].split("^");
						var cname = d[0];
						if(cname==sName)
							{
								scode = d[1];
								break;
							}
					}
					
					window.open("/stock/company_main.html?companycode="+scode+"&companyname="+escape(sName));
				};
				google.visualization.events.addListener(tree, 'select',ttf); 
				tree.draw(gdata, {
				  minColor: '#f00',
				  midColor: '#ddd',
				  maxColor: '#0d0',
				  headerHeight: 15,
				  fontColor: 'black',
				  showScale: false});
			}
	  });
}