
var myChart = function(id)
{
	return new Highcharts.Chart({
      chart: {
         renderTo: id.toString(),
         defaultSeriesType: type,
         marginRight: 130,
         marginBottom: 25,
         showAxes: true
      },
      title: {
         text: '指标分析图表',
         x: -20 //center
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
      subtitle: {
         text: '----stock',
         x: -10
      },
      xAxis: {
    	  	type:"datetime",//时间轴要加上这个type，默认是linear
    	    maxPadding : 0.05,
    	    minPadding : 0.05,
    	    tickInterval : 24 * 3600 * 1000 * 90,//两天画一个x刻度
    	        //或者150px画一个x刻度，如果跟上面那个一起设置了，则以最大的间隔为准
    	    //tickPixelInterval : 150,
    	    tickWidth:1,//刻度的宽度
    	    lineColor : '#990000',//自定义刻度颜色
    	    lineWidth :1,//自定义x轴宽度
    	    gridLineWidth :1,//默认是0，即在图上没有纵轴间隔线
    	    //自定义x刻度上显示的时间格式，根据间隔大小，以下面预设的小时/分钟/日的格式来显示
    	  dateTimeLabelFormats:
    	    {
    	     second: '%H:%M:%S',
    	     minute: '%e. %m %H:%M',
    	     hour: '%m/%e %H:%M',
    	     day: '%e日/%m',
    	     week: '%e. %m',
    	     month: '%y-%m ',
    	     year: '%y'
    	    }
      },
      yAxis: {
         title: {
            text: '指标值'
         },
			
         plotLines: [{
            value: 0,
            width: 1,
            color: '#808080'
         }]
      },
	plotOptions: {
          series: {
              lineWidth: 1,
			   marker: {
                            enabled: false,
                            states: {
                                hover: {
                                    enabled: true,
                                    radius: 3
                                }
                            }
                        }
          }
      },
      tooltip: {
         formatter: function() {
        	    return '<b>'+ this.series.name +':'+cIndexName+'</b><br/>'+
        	    Highcharts.dateFormat('%y-%m-%e', this.x) +': '+  
        	    Highcharts.numberFormat(this.y, 2);

         }
      },
      legend: {
         layout: 'vertical',
         align: 'right',
         verticalAlign: 'top',
         x: 0,
         y: 100,
         borderWidth: 0
      },
      series: []
   });
};

var myChartDynaic = function(id)
{
	return new Highcharts.Chart({
      chart: {
         renderTo: id.toString(),
         defaultSeriesType: type,
         marginRight: 130,
         marginBottom: 25,
         showAxes: true
      },
      title: {
         text: '指标分析图表',
         x: -20 //center
      },
      subtitle: {
         text: '----动态图',
         x: -10
      },
      xAxis: {
    	   maxPadding : 0.05,
    	    minPadding : 0.05,
    	        //或者150px画一个x刻度，如果跟上面那个一起设置了，则以最大的间隔为准
    	    //tickPixelInterval : 150,
    	    tickWidth:1,//刻度的宽度
    	    lineColor : '#990000',//自定义刻度颜色
    	    lineWidth :1,//自定义x轴宽度
    	    gridLineWidth :1,//默认是0，即在图上没有纵轴间隔线
      },
      yAxis: {
         title: {
            text: '指标值'
         },
			
         plotLines: [{
            value: 0,
            width: 1,
            color: '#808080'
         }]
      },
	plotOptions: {
          series: {
              lineWidth: 1
          }
      },
      tooltip: {
         formatter: function() {
        	    return '<b>'+ this.series.name +'</b><br/>'+
        	    Highcharts.numberFormat(this.y, 2);

         }
      },
      legend: {
         layout: 'vertical',
         align: 'right',
         verticalAlign: 'top',
         x: -10,
         y: 100,
         borderWidth: 0
      },
      series: []
   });
};
