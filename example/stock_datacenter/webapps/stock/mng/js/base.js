// JavaScript Document

var pojectPath = "/stock";
/*
 * 右侧浮动窗 显示隐藏事件
 */
function showRightFolating(){
	
	var div = document.getElementById("left_folating");	
	var main_td = document.getElementById("left_folating_main");
	if(div.clientWidth > 19 ){
		//div.style.width='19px';
		//main_td.style.display="none";
		$("#left_folating_main").slideUp(900);;
		$("#left_folating").animate({width:"19px"},1000);
		
	}else{		
		//div.style.width=document.body.clientWidth*0.6-2;
		
		main_td.style.display ="table-cell";
		main_td.style.width = document.body.clientWidth*0.6-2 - (26+2);
		var divRightChart = document.getElementById("rightChart");
		divRightChart.style.width = document.body.clientWidth*0.6 -(26+2) ;	
		
		var toWidth =document.body.clientWidth*0.6-2;
		$("#left_folating").animate({width:toWidth},1000);
	}
	div.style.height = document.body.clientHeight -2;	
}

function initRightFolating(){
	var div = document.getElementById("left_folating");		
	div.style.width = '19px';
	div.style.height = document.body.clientHeight -2;
	var tip_td  = document.getElementById("left_folating_tip");
	tip_td.style.height = document.body.clientHeight -2;
}

function resizeRithtFolating(){
	var div = document.getElementById("left_folating");	
	if(div.clientWidth > 19 ){
		div.style.width=document.body.clientWidth*0.6-2;
	}else{
		div.style.width='19px';		
	}
	div.style.height = document.body.clientHeight -2;
	var tip_td  = document.getElementById("left_folating_tip");
	tip_td.style.height = document.body.clientHeight -2;
}

var xtitle = "x";
var ytitle = "y";

var scatterchart = function(id)
{
	return new Highcharts.Chart({
	    chart: {
	        renderTo: id,
			zoomType: 'xy'
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
		tooltip: {
            formatter: function() {
                return '<b>'+this.point.name+':['+ this.x +
                    '</b> , <b>'+ this.y +']</b>';
            }
        },
		plotOptions: {
            series: {
                cursor: 'pointer',
                point: {
                    events: {
                        click: function() {
							if(confirm('remove this point'))
							{	
								this.remove();
							}
                        }
                    }
                }
            }
        },
	    title: {
	        text: '指标选股图'
	    },
	    series: []
	});
}
/*
 * JS排序
 */
function createComparsionFunction(propertyName,isDesc)
{	
	if(isDesc != true){
	    return function(object1, object2)
	    {
	        var value1 = object1[propertyName];
	        var value2 = object2[propertyName];
	        if (value1 < value2)
	        {
	            return -1;
	        } else if (value1 > value2)
	        {
	            return 1;
	        } else
	        {
	            return 0;
	        }
	    }
	}else{
		return function(object1, object2)
	    {
	        var value1 = object1[propertyName];
	        var value2 = object2[propertyName];
	        if (value1 < value2)
	        {
	            return 1;
	        } else if (value1 > value2)
	        {
	            return -1;
	        } else
	        {
	            return 0;
	        }
	    }
	}
}
	
	
/*
 * HTML参数获取
 */
function getQueryStr(str){ 
	var LocString=String(window.document.location.href);  
    /*var rs = new RegExp("(^|)"+str+"=([^/&]*)(/&|$)","gi").exec(LocString), tmp;  
  
    if(tmp=rs){  
        return tmp[2];  
    } */
	
	var splitArray = LocString.split(/\?|&/ig)
	for(var i=0;i<splitArray.length;i++){
		var splitStr = splitArray[i];
		var arg = splitStr.split('=');
		if(arg.length == 2){
			if(arg[0] == str){
				return unescape(arg[1]);
			}
		} 
	}
  
    // parameter cannot be found  
    return "";  
}  
/*
 * DIV加载页面
 */
function loadPage(id, url ,data) { 
	$("#"+id).addClass("loader"); 
	$("#"+id).append("Loading......");
	$.ajax({ 
		type: "post", 
		url: url, 
		data : data,
		cache: false, 
		//error: function() {alert('加载页面' + url + '时出错！');}, 
		success: function(msg) { 
			$("#"+id).empty().append(msg); 
			$("#"+id).removeClass("loader"); 
		} 
	}); 
}

var now = new Date();                    //当前日期   
var nowMonth = now.getMonth()+1;           //当前月   11
var nowYear = now.getFullYear();             //当前年 2012

/*
 * 获取最近的季度时间
 * yearNum 年跨度
 */
function getLastQuarter(yearNum){
	var reStr;	
	switch(nowMonth){
		case 1:			
			reStr =(nowYear-1-yearNum) + "-12-30";			
			break;
		case 2:			
			reStr =(nowYear-1-yearNum) + "-12-30";	
			break;
		case 3:			
			reStr =(nowYear-1-yearNum) + "-12-30";		
			break;
		case 4:			
			reStr =(nowYear-yearNum) + "-03-30";			
			break;
		case 5:			
			reStr =(nowYear-yearNum) + "-03-30";	
			break;
		case 6:			
			reStr =(nowYear-yearNum) + "-03-30";		
			break;
		case 7:			
			reStr =(nowYear-yearNum) + "-06-30";			
			break;
		case 8:			
			reStr =(nowYear-yearNum) + "-06-30";	
			break;
		case 9:			
			reStr =(nowYear-yearNum) + "-06-30";		
			break;
		case 10:			
			reStr =(nowYear-yearNum) + "-09-30";			
			break;
		case 11:			
			reStr =(nowYear-yearNum) + "-09-30";	
			break;
		case 12:			
			reStr =(nowYear-yearNum) + "-09-30";		
			break;
		default: 
	        reStr ="";	
	} 
	return reStr;
}

/*
 * 绑定数字输入框
 * 绑定时间有只能输入数字,千分制显示
 */
function numberInputBind(obj){
	obj.focus(function(){
	    $(this).val( thousandToNumberFormat($(this).val()) );
	});
	obj.blur(function(){	
	  $(this).val(numberToThousandFormat($(this).val()));
	});
	obj.keydown(function(e){
		// 注意此处不要用keypress方法，否则不能禁用 Ctrl+V 与
		// Ctrl+V,具体原因请自行查找keyPress与keyDown区分，十分重要，请细查
		if ($.browser.msie) {  // 判断浏览器
			if (((e.keyCode > 47) && (e.keyCode < 58)) || ((e.keyCode > 95) && (e.keyCode < 106)) 
					|| (e.keyCode == 109) || (e.keyCode == 110) || (e.keyCode == 173) || (e.keyCode == 190)    
					|| (e.keyCode == 8) || (e.keyCode == 46)
			) { 　// 判断键值
				return true;  
	        }else{  
	              return false;  
	        }
		}else{  
		    if (((e.which > 47) && (e.which < 58)) || ((e.which > 95) && (e.which < 106))
		    		|| (e.which == 109) || (e.which == 110) || (e.which == 173) || (e.which == 190)    
		    		|| (e.which == 8) || (e.which == 46)
		    ) {  
		    	return true;  
		    } else {  
		        return false;  
		    }
		}
	}).focus(function() {
         this.style.imeMode='disabled';   // 禁用输入法,禁止输入中文字符
		// imeMode有四种形式，分别是：
		// active 代表输入法为中文
		// inactive 代表输入法为英文
		// auto 代表打开输入法 (默认)
		// disable 代表关闭输入法
 　　　});
}

/*
 * 千分制转成常规数字显示
 */
function thousandToNumberFormat(num)
{
       if(num == '')return '';
       var x = num.split(',');
       return parseFloat(x.join(""));
}
    
/*
 * 常规数字 转成千分制显示
 */
function numberToThousandFormat(num)
{		
	  if('number'== typeof num)num=num+'';	
	  if(num == '')return '';
      var re=/(-?\d+)(\d{3})/
      while(re.test(num)){   
            num=num.replace(re,"$1,$2");
      }
      return num;
}
/*
 * 公司选择器绑定 
 * inputObj=$("#secode") buttonObj=$("#secodeExe")
 */
function companySelectBind(inputObj,buttonObj){
	buttonObj.click(function(){
		var secodeVal = inputObj.val().split(":");
		if(secodeVal.length == 2 ){
			openLink(secodeVal[1],secodeVal[0]);
		}
	});
	
	inputObj.bind('keyup',function(event) {
		if(event.keyCode==13){   
			var secodeVal = inputObj.val().split(":");
			if(secodeVal.length == 2 ){
				openLink(secodeVal[1],secodeVal[0]);
			}
		}
	});
	initCompanyAutoMatcher(inputObj);
}
function openLink(stockcode,stockname){
	var escapeStr = escape(stockname);//中文编解码
	window.open(pojectPath+"/html/stocks_analysis.html?stockcode="+stockcode+"&stockname="+escapeStr+"&end");
}
function loadScript(url, callback){
    var script = document.createElement("script")
    script.type = "text/javascript";

    if (script.readyState){  //IE
        script.onreadystatechange = function(){
            if (script.readyState == "loaded" ||
                    script.readyState == "complete"){
                script.onreadystatechange = null;
                callback();
            }
        };
    } else {  //Others
        script.onload = function(){
            callback();
        };
    }
    script.src = url;
    document.getElementsByTagName("head")[0].appendChild(script);
}
/*loadScript(pojectPath+"/js/code.js", function(){
    //初始化你的代码
});*/	
//自动匹配公司名
function initCompanyAutoMatcher(aa)
{
	/*
	if (typeof(stockCode) == "undefined") { 
		setTimeout("initCompanyAutoMatcher('"+aa+"')",1000);
		return;
	}
	var c_data = stockCode.split(";");
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
	*/
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.getScript(pojectPath+"/company/getCompanyByTableSystemString_v3", function(result){
		if(retObj==null)
			{
				return;
			}
		var data = retObj.split(";");
		if(data!=null)
		{
			var c_data = data;
			var option = {
			max: 12, 
			minChars: 0, 
			width: 150,
			scrollHeight: 300, 
			matchContains: true, 
			autoFill: false,
			formatItem: function(item) {
				var rowArr = item[0].split(":");
	            return rowArr[0] + " " + rowArr[1] + " " + rowArr[2]; 
	        },formatResult: function(item) {
	        	var rowArr = item[0].split(":");
				return rowArr[0]+":"+rowArr[1];
			}
			};
			var obj = $("#"+aa+"");
			obj.autocomplete(c_data, option);
		}
		
	});
}
function initAutoIndexMatcher(aa)
{

		var tsc = $("#tableSystem").val();
		$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		$.getScript(pojectPath+"/cfirule/getCfiruleListByTscid_V2?msg.tableSystemCode=ts_00003", function(result){
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
function initYear(id,selectIndex)
{
	var yearVar="<option value=\"1995\">1995</option><option value=\"1996\">1996</option><option value=\"1997\">1997</option><option value=\"1998\">1998</option><option value=\"1999\">1999</option><option value=\"2000\">2000</option>";
	yearVar+="<option value=\"2001\">2001</option><option value=\"2002\">2002</option><option value=\"2003\">2003</option><option value=\"2004\">2004</option><option value=\"2005\">2005</option><option value=\"2006\">2006</option>";
	yearVar+="<option value=\"2007\">2007</option><option value=\"2008\">2008</option><option value=\"2009\">2009</option><option value=\"2010\">2010</option><option value=\"2011\">2011</option><option value=\"2012\">2012</option><option value=\"2013\">2013</option><option value=\"2014\">2014</option><option value=\"2015\">2015</option>";
	$("#"+id+"").append(yearVar);
	$("#"+id+"").get(0).options[parseInt(selectIndex)].selected=true;
	
}
function showLoading(time)
{
	$("#loading").show();
	setTimeout("$(\"#loading\").hide()",time);
}
function addMystockDB(newstock){
	$.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
	$.post(pojectPath+"/user/mystock/save", {'stock':newstock}, function(result){
		if(result.success==null||!result.success){
			alert("操作失败!");
		}
	});
}
//写cookies函数,先进先出,保存20个
function addMyStock(newStock){
	var name = "stockArr";
	var s1 = getCookie(name);
	var stockArray =getMyStock(name);
	for(var i=0;i<stockArray.length;i++){
		if(newStock == stockArray[i]){
			return;//重复值不重新添加
		}
	}
	stockArray.unshift(newStock);//从头部加一个
	if(stockArray.length >20){
		stockArray.pop();//删除最后一个
	}
	var s2 = stockArray.join(";");
	setCookie(name,s2,7,"/");
}
function getMyStock(name){
	var s1 = getCookie(name);
	var stockArray;
	if(s1 != ''){
		stockArray = s1.split(";");
	}else{
		stockArray = new Array();
	}
	return stockArray;
}
/**************************************************
参数说明：
name	 Cookie名
value	 Cookie值
Days	 Cookie时效，默认为30天
sPath	 Cookie路径
sDomain	 Cookie作用域
bSecure	 Cookie是否加密传输
**************************************************/
function setCookie(name,value,Days,sPath,sDomain,bSecure)
{
	Days = (Days)?Days:30; //此 cookie 默认保存30天
	var exp  = new Date();    //new Date("December 31, 9998");
	exp.setTime(exp.getTime() + Days*24*60*60*1000);
	var sCookie = name + "="+ escape (value);
	sCookie += ";expires=" + exp.toGMTString();
	sCookie += (sPath) ? ";path=" + sPath : "";
	sCookie += (sDomain) ? ";domain=" + sDomain : "";
	sCookie += (bSecure) ? ";secure" : "";
	document.cookie = sCookie;
}
 
//读取cookies函数
function getCookie(name)
{
	if (document.cookie.length>0)
	{
		c_start=document.cookie.indexOf(name + "=")
		if (c_start!=-1)
		{ 
			c_start=c_start + name.length+1 
			c_end=document.cookie.indexOf(";",c_start)
			if (c_end==-1) c_end=document.cookie.length
			return unescape(document.cookie.substring(c_start,c_end))
		}
	}
	return "";

}
 
//删除cookie
function delCookie(name){
    var exp = new Date();
    exp.setTime(exp.getTime() - 1);
    var cval=getCookie(name);
    if(cval!=null) document.cookie= name + "="+cval+";expires="+exp.toGMTString();
}
function deleteMyStockDB(obj,stockContent){
	$.ajax({contentType:"application/x-www-form-urlencoded; charset=utf-8",async: false});
	$.post(pojectPath+"/user/mystock/del",{stock:stockContent},function(result){
		if(result.data==null){
			alert("操作失败!");
			return;
		}
		removeRow(obj);
	});
}
function deleteMySizerDB(obj,sizerContent){
	sizerContent = unescape(sizerContent);
	$.ajax({contentType:"application/x-www-form-urlencoded; charset=utf-8",async: false});
	$.post(pojectPath+"/user/mystock/del",{index:sizerContent},function(result){
		if(result.data ==null ){
			alert("操作失败!");
			return;
		}
		removeRow(obj);
	});
}
function removeRow(_element){
	var _tdParentElement = _element.parentNode;
	var _trParentElement = _tdParentElement.parentNode;
	var _bodyParentElement = _trParentElement.parentNode;
	_bodyParentElement.removeChild(_trParentElement);
}
//type=1 显示我的股筛
function ajaxCheckLogin(type){
	if(type == null )type=0;
	$.ajax({contentType:"application/x-www-form-urlencoded; charset=utf-8"});
	$.getScript(pojectPath+"/user/members/checkLogin",function(){
		var checkLoginResult = checkLogin;
		if(checkLoginResult == true){
			$("#login").empty();
			$("#login").append("用户["+nickname+"]已登录<input type='button' value='退出' onClick='loginOut()'>");
			//$("#login").append("<br><div id='calculator'></div>");
			
			$.ajax({contentType:"application/x-www-form-urlencoded; charset=utf-8",async: false});
			$.get(pojectPath+"/user/mystock/get",function(result){
				var sessionData = result.data;
				if(sessionData!=null){
					if( null != sessionData.stockarr){
						var sStockArray = JSON.parse(sessionData.stockarr);//sessionData.stockarr.split(";");
						if(sStockArray!= null && sessionData.stockarr.trim().length > 1){
							var userStock = "<table width=\"100%\" class=\"mytable\"><thead><tr><th height=\"20\" >名称<\/th><th>操作<\/th><\/tr></thead>"
							userStock +="<tbody>";
							var showSize = (sStockArray.length<10)?sStockArray.length:10;
							for(var i=0;i<showSize;i++){
								//var stock = sStockArray[i].split(":");bj2[i].companyName+":"+obj2[i].companycode
								userStock += "<tr><td height=\"20\"><a onclick=\"openLink('"+sStockArray[i].companycode+"','"+sStockArray[i].companyName+"')\" target='_blank'>"
									+sStockArray[i].companyName+"<\/a><\/td><td><img onclick=\"deleteMyStockDB(this,'"+sStockArray[i].companycode+"')\" class='activelink' src='/stock/images/delete.gif'><\/td><\/tr>"
							}
							userStock +="<\/tbody>";
							$("#login").append("<h4>我的自选股</h4><div>"+userStock+"</div>");
							//$( "#calculator" ).accordion({heightStyle: "content"});
						}
					}
					if(null != sessionData.indexarr && type==1){
						var sSizerArray = sessionData.indexarr.split(";");
						if(sSizerArray!= null && sessionData.indexarr.trim().length > 1){
							var userSizer = "<table width=\"100%\" class=\"mytable\"><thead><tr><th height=\"20\" >名称<\/th><th>操作<\/th><\/tr></thead>"
								userSizer +="<tbody>";
							var showSize = (sSizerArray.length<10)?sSizerArray.length:10;
							for(var i=0;i<showSize;i++){
								var sizer = sSizerArray[i];
								var obj2 = JSON.parse(unescape(sizer));
								userSizer += "<tr><td height=\"20\"><a onclick=\"initSizerByCond('"+escape(sizer)+"')\" target='_blank'>"+obj2.name+"<\/a><\/td><td><img onclick=\"deleteMySizerDB(this,'"+escape(sizer)+"')\" class='activelink' src='/stock/images/delete.gif'><\/td><\/tr>"
							}
							userSizer +="<\/tbody>";
							$("#login").append("<h4>我的股筛</h4><div>"+userSizer+"</div>");
							//$( "#calculator" ).accordion({heightStyle: "content"});
						}
					}
				}
			});
		}else{
			$("#refererUrl").val(window.document.location.href);
		}
		var userStock = "<table width=\"100%\" class=\"mytable\"><thead><tr><th height=\"20\" >名称<\/th><th>最新股价<\/th><\/tr></thead>"
		userStock +="<tbody>";
		var stockArray =getMyStock("stockArr");
		var showSize = (stockArray.length<10)?stockArray.length:10;
		for(var i=0;i<showSize;i++){
			var stock = stockArray[i].split(":");
			userStock += "<tr><td height=\"20\"><a onclick=\"openLink('"+stock[1]+"','"+stock[0]+"')\" target='_blank'>"+stock[0]+"<\/a><\/td><td>10.00↓<\/td>"
		}
		userStock +="<\/tbody><\/table>";
		$("#login").append("<h4>最近浏览</h4><div>"+userStock+"</div>");		
	});
}
function loginOut(){
	$.get(pojectPath+"/user/members/loginOut",function(){
		window.location.reload();//刷新本页
	});//AJAX请求登录退出
}
function showOrHideLoading(sh)
{
	if(sh) 
		$("#loading").show();
	else
		$("#loading").hide();
}
//单位：0:%百分比,1：无单位,2:千,3:万,4:百万,5:千万,6:亿
function getUnitShow(unit)
{
	if(unit==0)
		return "%";
	if(unit==2)
		return "千";
	if(unit==3)
		return "万";
	if(unit==4)
		return "百万";
	if(unit==5)
		return "千万";
	if(unit==6)
		return "亿";
	return "";
}
function formatUnit(value,unit)
{
	if(unit==0)
		return value*100;
	if(unit==1||unit==-1)
		return value;
	if(unit==2)
		return value/1000;
	if(unit==3)
		return value/10000;
	if(unit==4)
		return value/1000000;
	if(unit==5)
		return value/10000000;
	if(unit==6)
		return value/100000000;
}
function openSingle(ccode,cname)
{
	window.open(pojectPath+"/html/stocks_analysis.html?stockcode="+ccode+"&stockname="+escape(cname));
}


function getUnitDesc(utype) {
		utype = parseInt(utype);
		var ret = "";
		switch (utype) {
		case 0:
			ret = "%";
			break;
		case 1:
			ret = "";
			break;
		case 2:
			ret = "千";
			break;
		case 3:
			ret = "万";
			break;
		case 4:
			ret = "百万";
			break;
		case 5:
			ret = "千万";
			break;
		case 6:
			ret = "亿";
			break;
		}
		return ret;
}