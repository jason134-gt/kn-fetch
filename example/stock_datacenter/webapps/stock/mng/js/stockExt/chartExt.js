/*
 * 图表扩展组件，提供串珠图,瀑布图等
 */
var myCredits = {
	enabled : true,
	href : "http://www.yfzx.com",
	text : "盈富在线",
	style : {
		cursor : 'pointer',
		color : '#000000',
		fontSize : '15px'
	}
};
/*
 * 瀑布图
 */
var waterfallChart = function(id,name,ytitle,cgs,sname,rawData)
{
	//function for creating the dotted connector lines between columns
	//pass in raw data
	this.createDottedData = function(rd) {
		var dottedData = new Array();
		for (var i=0, len=rd.length; i<len; ++i ) {
		    dottedData[i] = rd[i];
		}
		//add final dashed line to pseudo-column
		dottedData[rd.length] = rd[rd.length-1];
		return dottedData;
	}

	this.createPoint = function(lowVal, yVal) {
	    var element = new Object();
	    element.low = lowVal;
	    element.y = yVal;
	    //override color when delta is negative
	    if (yVal< lowVal) {
	        element.color = '#AA4643';
	    }
	    return element;
	};

	//function parses raw data and builds structure for low.hi columns
	this.createLowHiData =function(rd) {
	    var lows = new Array();
	    for (var i=0, len=rd.length; i<len; ++i ) {
	        if (i == 0) {
	            lows[i] = createPoint(0, rd[i]);
	        } else {
	            lows[i] = createPoint(rd[i-1], rd[i]);
	        }
	    }
	    //add pseudo-column
	    lows[rd.length] = createPoint(0, rd[rd.length-1]);
	    return lows;
	}
	var pointWidth = 30;
	if(cgs.length > 10){pointWidth=25;}
	return  new Highcharts.Chart({
		chart: {
			renderTo: id,
			type: 'column'
		},
		credits:myCredits,
		xAxis: {
			categories: cgs,
			title: {
				text: ''
			},
			labels: {
				enabled:false,
                staggerLines: 3
            }
		},
		yAxis: {
			title: {
				text: ytitle
			}
		},
		plotOptions: {
			series: {
				pointWidth: pointWidth,
				cursor: 'pointer',
                point: {
                    events : {
					click : function(event) {
							showRightFolating();
						
							var indexname = this.category.split(":")[0];
							var indexcode = this.category.split(":")[1];
							var argData = {
								name : curcompanyname,
								code : curcompanycode,
								indexCode : indexcode,
								indexName : indexname,
							};
							var nid = id+"_"+indexcode;
							autoAnalysis(argData,nid);
						}
					}
                }
			}
		},
		legend: {
			enabled: false
		},
		title: {
			text: name
		},
		tooltip: {
			formatter: function() {
				var indexname = this.x.split(":")[0];
				if (this.point.x == 0 || this.point.x == (this.series.data.length -1)) {
					return indexname+":"+this.y.toFixed(2);
				} else {
					return indexname+":"+Math.abs((this.y - this.series.data[this.point.x - 1].y).toFixed(2));
				}
			}
		},
		
		series: [ {
			name: null,
			type: 'line',
			lineWidth:1,
			dashStyle: 'dash',
			shadow:false,
			step:true,
			color:'#333',
			marker:{
				enabled:false
			},
			data: createDottedData(rawData),
		},{
			name: sname,
			color:'#3D96AE',
			shadow:false,
			borderWidth:1,
			borderColor:'#000',
			groupPadding:0,
			data: createLowHiData(rawData),
			dataLabels: {
				enabled: true,
				color: '#000',
				formatter: function() {
					if (this.point.x == 0 || this.point.x == (this.series.data.length -1)) {
						return this.y.toFixed(2);
					} else {
						// show delta between this.y and previous this.y
						return Math.abs((this.y - this.series.data[this.point.x - 1].y).toFixed(2));
					}
				}
			},
			
			pointPadding:.1,
		}]

	});
}


/*
 * 串珠图
 * cags = ['盈利质量', '盈利能力', '运营能力', '管控能力', '成长能力','偿债能力']
 */
var beadsChart = function(divID,cgs,mainData,otherData)
{
	var redPlotLines = new Array();
	for(var i=0;i<cgs.length;i++){
		redPlotLines[i]={
			color: 'rgba(255, 0, 0, 0.5)',
	        width: 2,
	        value: i
		};
	}
	new Highcharts.Chart({
	     chart: {
	         renderTo: divID,
	         type: 'scatter',
	         zoomType: 'xy',
	     },
	     title: {
	         text: '同行公司各指标对比图'
	     },
	     credits : myCredits,
	     xAxis: {
		        categories: cgs,
		        labels:{
		        	//rotation: -45,//倾斜45度
					//y:50,
					//staggerLines: 2, //两行显示
		            formatter: function() {
						var reValue= "";
						var trSize = 5;
						var rowNum = parseInt(this.value.length/trSize);
						for(var i=0;i<=rowNum;i++){
							var tmpStr = this.value.substr(trSize*i,trSize);
							if(tmpStr!="")reValue += tmpStr+"<br/>";
						}
						return reValue;
					},
					style:{
		                fontWeight: 'bold',
		                fontSize:'13px'
		            }
		        },
		        plotLines:redPlotLines
		 },
	     yAxis: {
	         max:100,
	         min:-100,
	         labels: {
	             enabled: false
	         },
	         title: {
	             text: ''
	         }
	     },
	     tooltip: {
	         formatter: function() {
	        	 	if(this.point.name === undefined){
	        	 		return ''+this.series.name+ '（'+this.x+']比率='+this.y+'%';
	        	 	}else{
	        	 		 return ''+this.point.name + '（'+this.x+'='+this.point.value+')';
	        	 	}	                
	         }
	     },
	     legend: {
	    	 enabled: false,
	         layout : 'vertical',
	         align : 'center',
	         verticalAlign : 'bottom',
	         backgroundColor : '#FFFFFF'
	     },
	     plotOptions: {
	         scatter: {
	             marker: {
	                 radius: 3,
	                 states: {
	                     hover: {
	                         enabled: true,
	                         lineColor: 'rgb(100,100,100)'
	                     }
	                 }
	             },
	             states: {
	                 hover: {
	                     marker: {
	                         enabled: false
	                     }
	                 }
	             }
	         },
			 series: {
                cursor: 'pointer',
                point: {
                    events: {
                        click: function() {
							//reloadpage(this.id,this.name);
							//window.location.href="/stock/html/stocks_analysis.html?stockcode="+this.id+"&stockname="+escape(this.name); 
							window.open("/stock/html/stocks_analysis.html?stockcode="+this.id+"&stockname="+escape(this.name));
                        }
                    }
                }
            }
	     },
	     series: [{
	         name: '其它公司值',
	         data: otherData
	     },{
	         name: '本公司值',
	         type: 'spline', 
			 marker: {
				lineWidth: 3,
				lineColor: null
			},
	         data: mainData/*[{x:0,y:43,name:'本公司',value:'2.53'}, 
	                {x:1,y:72,name:'本公司',value:'3.63'}, 
	                {x:2,y:61,name:'本公司',value:'1.63'}, 
	                {x:3,y:56,name:'本公司',value:'1.25'}, 
	                {x:4,y:64,name:'本公司',value:'1.80'}
	                ]*/
	     }]
	 });	
}

var dualAxesChart = function(divID,yaxisArray,seriesArrayData,myEvents){
	return new Highcharts.Chart({
		chart : {
			renderTo : divID,
			events : myEvents/*{
				click : function(event) {//事件，需要传递进来?
					showRightFolating();
					if (showRightingChartId != id) {
						showRightingChartId = id;
						var argData = {
							company : postData.companyName + ":"
									+ postData.companycode,
							indexCode : postData.rule,
							indexName : postData.ruleName,
							startTime : postData.stime,

							endTime : postData.etime,
						};
						autoAnalysis(argData);
					}

				}
			}*/
		},
		xAxis : {
			type : "datetime",//时间轴要加上这个type，默认是linear
			//startOnTick: true,
			//自定义x刻度上显示的时间格式，根据间隔大小，以下面预设的小时/分钟/日的格式来显示
			dateTimeLabelFormats : {
				month : '%Q'
			}
		},
		yAxis : yaxisArray,
		tooltip : {
			formatter : function() {
				return '<b>' + this.series.name + ':' + this.y + '</b>';
			}
		},
		credits : myCredits,
		plotOptions : {
			series : {
				pointWidth : 7,
				borderWidth : 0,
				shadow : false
			}
		},
		legend : {
			layout : 'vertical',
			align : 'center',

			verticalAlign : 'bottom',
			backgroundColor : '#FFFFFF'
		},

		series : seriesArrayData
	});
} 
/*
 * Bar图
 * 机构评级和机构估值
 */
var barChart = function(divID,titleName,cgs,seriesArrayData){
	return new Highcharts.Chart({
        chart: {
            renderTo: divID,
            type: 'bar'
        },
        title: {
            text: titleName
        },
        xAxis: {
            categories: cgs/*['强烈看涨', '看涨', '看平', '看跌', '强烈看跌']*/,
            title: {
                text: null
            },
            labels:{
                style:{
                    color: '#FF0000',
                    fontWeight: 'bold',
                    fontSize:'14px'
                }
            }
        },
        yAxis: {
            min: 0,
            title: {
                text: '',
                align: 'high'
            }
        },
        tooltip: {
            formatter: function() {
                return ''+
                    this.series.name +': '+ this.y +'';
            }
        },
        plotOptions: {
            bar: {
                dataLabels: {
                    enabled: true
                }
            }
        },
        legend : {			
			align : 'center',

			verticalAlign : 'bottom',
			backgroundColor : '#FFFFFF'
		},
        credits: myCredits,
        series: seriesArrayData
        /*[{
            name: '2012-11-20',
            data: [1, 2, 5, 2, 0]
        }, {
            name: '2012-11-10',
            data: [1, 1, 6, 2, 0]
        }, {
            name: '2012-11-01',
            data: [0, 1, 6, 3, 0]
        }]*/
    });
}
/*
 * 堆积bar图
 */
var stackedBarChart = function(divID,cgs,seriesData){
	return new Highcharts.Chart({
        chart: {
            renderTo: divID,
            type: 'bar'
        },
        title: {
            text: ''
        },
        xAxis: {
            categories: cgs,//['募资', '派现'],
            labels:{
                style:{
                    color: '#FF0000',
                    fontWeight: 'bold',
                    fontSize:'14px'
                }
            }
        },
        yAxis: {
            min: 0 ,
            title: {
                text: ''
            },
            stackLabels: {
                enabled: true,
                style: {
                    fontWeight: 'bold',
                    color: 'red'
                }
            }
        },
        credits: myCredits,
        legend: {
            backgroundColor: '#FFFFFF',
            reversed: true
        },
        tooltip: {
            formatter: function() {
                return ''+
                    this.series.name +': '+ this.y +'';
            }
        },
        plotOptions: {
            series: {
                stacking: 'normal'
            }
        },
        series: seriesData/*[
		{
			name: '增发',
			data: [{x:0,y:30}]
		},{
			name: '增发S',
			data: [{x:1,y:20}]
		},{
            name: '配股',
            data: [15,0]
        },{
            name: '增发',
            data: [80,0]
        },{
            name: '新股发行',
            data: [2,0]
        },{
            name: '现金分红',
            data: [0,56.07]
        }]*/
    });
}
/*
 * 堆积柱状图
 */
var stackedColumnChart = function(divID){
	return new Highcharts.Chart({
        chart: {
            renderTo: divID,
            type: 'column'
        },
        title: {
            text: '公司产品服务'
        },
        xAxis: {
            categories: ['2011Q3', '2011Q4', '2012Q1', '2012Q2', '2012Q3']
        },
        yAxis: {
            min: 0,
            title: {
                text: ''
            },
            stackLabels: {
                enabled: true,
                style: {
                    fontWeight: 'bold',
                    color: 'red'
                }
            }
        },
        credits: myCredits,
        legend: {
            backgroundColor: '#FFFFFF',
            reversed: true
        },
        tooltip: {
            formatter: function() {
                return '<b>'+ this.x +'</b><br/>'+
                    this.series.name +': '+ this.y +'<br/>'+
                    'Total: '+ this.point.stackTotal;
            }
        },
        plotOptions: {
            column: {
                stacking: 'normal',
                dataLabels: {
                    enabled: true,
                    color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white'
                }
            }
        },
        series: [{
            name: '产品A',
            data: [5, 3, 4, 7, 2]
        }, {
            name: '产品B',
            data: [2, 2, 3, 2, 1]
        }, {
            name: '产品C',
            data: [3, 4, 4, 2, 5]
        }]
    });
}
/*
 * 双饼图,股本结构中
 */
var doublePieChart = function(divID){
	var colors = Highcharts.getOptions().colors;
	var categories = ['流通A股', '流通B股', '股改限售股'];
	var name = '股本结构';
	var data = [{
        y: 80.87,
        color: colors[0],
        drilldown: {
            name: '十大流通A股股东',
            categories: ['华润股份有限公司', '中国银行-易方达深证100交易型开放式指数证券投资基金', '刘元生','中国建设银行-博时主题行业股票证券投资基金','其它流通A股股东'],
            data: [14.73, 1.36, 1.22, 0.92, 62.64],
            color: colors[0]
        }
    }, {
        y: 11.96,
        color: colors[1],
        drilldown: {
            name: '流通B股股东',
            categories: ['流通B股股东'],
            data: [11.96],
            color: colors[1]
        }
    }, {
        y: 7.17,
        color: colors[2],
        drilldown: {
            name: '股改限售股',
            categories: ['CEO奖股','其它股改限售股'],
            data: [4,3.17],
            color: colors[2]
        }
    }];


	// Build the data arrays
	var browserData = [];
	var versionsData = [];
	for (var i = 0; i < data.length; i++) {
	
	    // add browser data
	    browserData.push({
	        name: categories[i],
	        y: data[i].y,
	        color: data[i].color
	    });
	
	    // add version data
	    for (var j = 0; j < data[i].drilldown.data.length; j++) {
	        var brightness = 0.2 - (j / data[i].drilldown.data.length) / 5 ;
	        versionsData.push({
	            name: data[i].drilldown.categories[j],
	            y: data[i].drilldown.data[j],
	            color: Highcharts.Color(data[i].color).brighten(brightness).get()
	        });
	    }
	}

	// Create the chart And Return
	return new Highcharts.Chart({
	    chart: {
	        renderTo: divID,
	        type: 'pie'
	    },
	    title: {
	        text: '股本股东结构图'
	    },
	    yAxis: {
	        title: {
	            text: 'Total percent market share'
	        }
	    },
	    credits: myCredits,
	    plotOptions: {
	        pie: {
	            shadow: false 
	        }	        
	    },
	    tooltip: {
	        valueSuffix: '%'
	    },
	    series: [{
	        name: '股本',
	        data: browserData,
	        size: '60%',
	        dataLabels: {
	            formatter: function() {
	                return this.y > 5 ? this.point.name : null;
	            },
	            color: 'white',
	            distance: -30
	        }
	    }, {
	        name: '股东',
	        data: versionsData,
	        innerSize: '60%',
	        dataLabels: {
	            formatter: function() {
	                // display only if larger than 1
	                return this.y > 1 ? '<b>'+ this.point.name +':</b> '+ this.y +'%'  : null;
	            }
	        }
	    }]
	});
}

var showchart = function(id, ctitle,yaxis) {
		return new Highcharts.Chart({
			chart : {
				renderTo : id,
				width : '600'
			},
			title: {
	         text: ctitle
			},
			xAxis : {
				type : "datetime",//时间轴要加上这个type，默认是linear
				//startOnTick: true,
				//自定义x刻度上显示的时间格式，根据间隔大小，以下面预设的小时/分钟/日的格式来显示
				dateTimeLabelFormats : {
					month : '%Q'
				}
			},
			yAxis : yaxis,
			tooltip : {
				formatter : function() {
					var unit = this.point.id.split("_")[1];
					var value = this.y;
					if(unit==0)
						value = this.y+"%";
					return '<b>' + this.series.name + ':' + value + '</b>';
				}
			},
			credits : {
				enabled : true,
				href : "http://www.yfzx.com",
				text : "盈富在线",
				style : {
					cursor : 'pointer',
					color : '#000000',
					fontSize : '15px'
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
				layout : 'vertical',
				align : 'center',

				verticalAlign : 'bottom',
				backgroundColor : '#FFFFFF'
			},
			series : []
		});
	}
function showChartWithData(id,indexname,unit)
{
		var tag = ctagname;
		if(tag=="-1") tag = "所有行业";
		indexname=indexname;
		var dchartid = id+"_divchart";
		currentShowChartdiv = id;
		$("#chartdiv").empty();
		var dchart = document.getElementById(dchartid);
		if(dchart==null||dchart=="")
		{
			$("#chartdiv").empty();
			$("#chartdiv").append("<div id='"+dchartid+"' style='display:none;width:600px;height:210px;'></div>");

			   showIndexPanel(dchartid,document.getElementById(id));
               $("#"+dchartid).css('z-index',3)
			   var yaxis = [{
						title:
							{
								text:indexname
							},
							lineWidth: 1
					}, {
							title:
								{
									text:indexname+"-同比"
								},
							lineWidth: 1,
							opposite: true,		
					}];
			   var chart = new showchart(dchartid,indexname+"分析图", yaxis);
			   var ds = $("#data_"+id+"").html();
			   var result = jQuery.parseJSON(ds);
			   paraseShowChartData(indexname, result, chart);
		}
		else
		{
			$("#"+id+"_divchart").slideDown();
			dchart.redraw();
		}

}
	var unitName = "";
	var isRemoveZero = true;
	//cctype :图表类型
function paraseShowChartData(name, data, chart) {
		var ctype="spline";
		var d = data;
		var i = 0;
		var j = 0;
		//分隔每个公司
		for (i = 0; i < d.length; i++) {
			var cd = d[i].d;
			if (cd == null || cd.length == 0) {
				//alert("没有找到数据哦!");
				return;
			}
			var mtime;
			var bunit = d[i].u;
			var ushow = "";
			if(bunit!=1&&bunit>=0) ushow = "(单位："+getUnitShow(bunit)+")";
			//if(ushow=="(单位：)")
				//alert(bunit);
			var nname = name
			var data = [];
			var zNum = 0;//记录值的数
			for (j = 0; j < cd.length; j++) {
				//需要整型,字符串不显示
				var time = cd[j].t;
				var value = cd[j].v;
				if (j == 0)
					mtime = time;
				if (isRemoveZero && Math.abs(value) == 0) {
					zNum++;
					continue;
				}
				//取掉失真的点
				//if (cctype != null && cctype == "line" && Math.abs(value) >= 10)
				if(value==null) continue;
				//if(value>1)
				value = formatUnit(value,bunit);
				value = Number(Number(value).toFixed(2));
				data.push({
					x : time,
					y : value,
					id:time+"_"+bunit
				});
			}
			if (zNum == cd.length) {
				//alert("数值全为零值!");
				return;
			}
			var ctype = "column";//默认柱状图,当cctype==null时
			if(i==1) 
			{
				ctype="spline";
				nname = nname+"同比";
			}
			if(bunit==0) ctype = "spline";	
			chart.addSeries({
				name : nname,
				type : ctype,
				data : data,
				yAxis : i,
				marker : {
					enabled : true,
					radius : 0,
					states : {
						hover : {
							fillColor : 'red',
							radius : 5
						}
					}
				},

				enableMouseTracking : true
				});
			chart.yAxis[i].setTitle({
				text: nname+ushow
			});
			}	
}
function hiddenChartDiv(id)
{
	  $("#"+id+"_divchart").slideUp();
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
  