var pojectPath = "";
var remoteImgServer = "http://183.57.43.243:8899"; //待配置到配置中心，由配置中心下发
var imgReadServer = "http://img.igushuo.com/";
var yfCompanyData = new Array();
var myFollowData = new Array();
var myFocusStockArr = new Array();
var access = {};
var loginDialog;
var yfCsData;
var reNewStock;
//请求后台接口的定时器
var updateStockInterval,cmRefresher,creportRefresher,yjkcTimer,currTradeData;

//百度统计 http://tongji.baidu.com
//百度统计账号 happyfromtbq happyfromtbq163
var _hmt = _hmt || [];
var uid = getCookie("uid");

/**************************************************
参数说明：
name	 Cookie名
value	 Cookie值
Days	 Cookie时效，默认为30天
sPath	 Cookie路径
sDomain	 Cookie作用域
bSecure	 Cookie是否加密传输
**************************************************/
function setCookie(name, value, Days, sPath, sDomain, bSecure) {
	Days  =   (Days) ? Days : 30;  //此 cookie 默认保存30天
	var  exp   =  new  Date();    //new Date("December 31, 9998");
	exp.setTime(exp.getTime()  +  Days * 24 * 60 * 60 * 1000);
	var sCookie = name  +  "=" +  escape (value);
	sCookie += ";expires=" + exp.toGMTString();
	sCookie += (sPath) ? ";path=" + sPath : "";
	sCookie += (sDomain) ? ";domain=" + sDomain : "";
	sCookie += (bSecure) ? ";secure" : "";
	document.cookie = sCookie;
}
//读取cookies函数 2015.2.12 ljb修复uuid和uid一样的bug
function getCookie(name) {
	 var cookieValue,reg=new RegExp("(^| )"+name+"=([^;]*)(;|$)");

    if(cookieValue=document.cookie.match(reg)){
 		if (cookieValue[2].indexOf("\"") == 0) {
 			cookieValue[2] = cookieValue[2].substring(1, cookieValue[2].length); //去掉第一个 "
 		}
		if (cookieValue[2].lastIndexOf("\"") == (cookieValue[2].length - 1)) {
			cookieValue[2] = cookieValue[2].substring(0, cookieValue[2].length - 1); //去掉最后一个 "
		}
        return unescape(cookieValue[2]);
    }else{
        return "";
    }

} 
//删除cookie
function delCookie(name) {    
	var  exp  =  new  Date();    
	exp.setTime(exp.getTime()  -  1);    
	var  cval = getCookie(name);    
	if (cval != null)  document.cookie =  name  +  "=" + cval + ";expires=" + exp.toGMTString();
}
//显示登录DIV
function showLoginDiv(jqueryObj) {
	var divId = "showLoginDiv";
	var div = document.getElementById(divId);
	if (div != null)
		document.body.removeChild(div);
	div = document.createElement("div");
	div.id = divId;
	div.style.position = 'absolute';
	var op = jqueryObj.offset(); //jquery offset()方法
	div.style.top = op.top + 20;
	div.style.left = op.left;
	div.style.height = 110;
	div.style.width = 300;
	div.style.zIndex = 150;
	div.style.backgroundColor = '#ecf1f6';
	div.style.border = "1px solid #666";
	div.style.textAlign = "left";
	div.innerHTML = '<form method="post" action="' + pojectPath + '/user/members/login" name="reg" id="member_login"><p><label for="members.nickname">昵&nbsp;&nbsp;&nbsp;&nbsp;称:</label><input type="text" name="members.nickname" id="members.nickname"></p><p>	<label for="members.password">输入密码:</label><input type="password" name="members.password" id="password"></p><p><input type="hidden" name="refererUrl" id="refererUrl" value="' + window.document.location.href + '"><input type="submit" id="regSubmit" class="submit" value="确定登录"> <input type="button" value="关闭" onclick="delThisDiv(\'' + divId + '\')"/></p></form>';
	//document.body.appendChild(div);
	jqueryObj.parent().append(div);
	$("#member_login").validate({
		rules: {
			"members.nickname": {
				required: true,
				minlength: 2,
				maxlength: 32
			},
			"members.password": {
				required: true,
				minlength: 5,
				maxlength: 32
			}
		}
	});
}


function loginOut() {
	$.post(pojectPath + "/stock/user/members/loginOut?end=" + new Date().getTime(), function() {
		$(".logout").text("");
		alert("已退出")
	}); //AJAX请求登录退出
	//setTimeout('window.location.href=pojectPath+"/login.html"', 1000);
}


/**
 * resourceIds:
 * 			   需要访问的资源ID列表，以|分隔(eg: 1|5|12);
 *
 * @return
 * true:表示鉴权通过，执行后面的函数
 * false:表示其他各种情况，返回不执行后面的函数 if(! auth("23")) { return ;}
 */
function auth(resourceIds) {
	var uid = getCookie("uid");

	if (uid == "" || uid == null) {
		goLogin();
		return false;
	}
	return true;
}

// ljb
function goLogin() {
	if($("#loginDiv").length>0){
		return
	}
	var tipsDiv;
	tipsDiv = "<div id='tooltips' style='font-size:11px;height:30px;padding:5px 5px 5px 5px;display:none;'></div>";
	$("body").append(tipsDiv);
	loginDialog = art.dialog({
		content: '<div id="loginDiv">' +
			'<form id="login-form" action="" method="post" >' +
			'<div class="loginBox">' +
			'<div class="loginBoxCenter">' +
			'<p>' + '<label for="username" style="display: none;">' + '邮箱：' + '</label>' + '</p>' +
			'<p>' + '<input type="text" id="login-form-username" name="username" class="loginInput" autocomplete="off" title="请输入用户名" placeholder=" 请输入注册邮箱\/用户名" value="" onkeydown="if(event.keyCode==13){$(\'#doAjaxLogin\').focus();}" />' + '</p>' +
			'<p>' + '<label for="password" style="display: none;">' + '密码：' + '</label>' + '</p>' +
			'<p>' +
			'<input type="password" name="password" id="login-form-password" class="loginInput" title="请输入密码" placeholder=" 请输入密码" value="" onkeydown="if(event.keyCode==13){$(\'#doAjaxLogin\').focus();}" />' +

		'</p>' +
			'</div>' +
			'<div class="loginBoxButtons">' +
			'<input type="checkbox" value="1" checked="true" id="autoLogin" name="autoLogin" />' +

		'<span id="autoLogin">' + '一周内免登录' + '</span>' +
			'<input type="button" class="loginBtn redButton" id="doAjaxLogin" onclick="ajaxLogin()" value="登录" /> ' +
		'</div>' +
			'</div>' +
			'</form>' +
			'</div>',
		title: '登录',
		lock: true,
		top: '280',
	});
/*
	function findPswAndgoRes() {
		var targ = String(window.document.location.href);
		var $forgetLink = $(".forgetLink");
		var $goRes = $(".goregis a");
		$goRes.attr("href", "login.html?target=" + targ);
		$forgetLink.attr("href", "login.html?target=resetpassword");
	};
	findPswAndgoRes();*/
};
var key;
var datetime = new Date();
var keyStr;


function errorTip(elementJobj, text) {
	var ntop = elementJobj.offset().top - 5;
	var nleft = elementJobj.offset().left + 235;
	$("#tooltips").css({
		'position': 'absolute',
		'top': ntop,
		'left': nleft,
		'z-index': 2333,
		"background-color": "#fefefe",
		"border": "1px solid #C6C6A8"
	});
	$("#tooltips").html('<img width="22" height="22" style="width:22;margin: 5px 5px -5px 0px;" src="images/close.png"><span style="color:red;font-size:16px;font-weight: bold;">' + text + '</span>');
	$("#tooltips").show();
};

function hideErrorMsg(obj) {
	$("#loginErrorMsg").html("");
	$("#login-form-username,#login-form-password").css("border-color", "#CCCCCC");
}

function ajaxLogin() {
	var loginMsgJobj = $("#loginErrorMsg");
	if (typeof(errorMsg) != 'undefined') {
		errorMsg = "";
	}
	loginMsgJobj.html("");
	var usernameJobj = $("#login-form-username");
	var username = usernameJobj.val();
	if (username == "" || username == null) {
		usernameJobj.focus();
		usernameJobj.css("border-color", "#E73010");
		errorTip($("#login-form-username"), "用户名不能为空");
		// loginMsgJobj.html("用户名不能为空");
		return;
	}
	var passwordJobj = $("#login-form-password");
	var password = passwordJobj.val();
	if (password == "" || password == null) {
		passwordJobj.focus();
		passwordJobj.css("border-color", "#E73010");
		errorTip($("#login-form-password"), "密码不能为空");
		// loginMsgJobj.html("密码不能为空");
		return;
	}
	if (password.length > 32 || password.length < 6) {
		passwordJobj.focus();
		passwordJobj.css("border-color", "#E73010");
		errorTip($("#login-form-password"), "密码的长度应为6-32个字符");
		return;
	}
	var autoLogin = $("#autoLogin").is(":checked");
	var datetime2 = new Date();
	var jgfz = (datetime2.getTime() - datetime.getTime()) / 600000;



	username = encodeURIComponent(username);
	if (jgfz > 1 || (key == null)) { //重新获取公钥
		$.get("/stock/user/members/cipherStr", function(result) {
			try {
				var arg = result.split(",");
				if (arg.length == 2) {
					key = new RSAKey();
					//linebrk(arg[0], 64)
					key.setPublic(arg[0], arg[1]);
					keyStr = arg[0];
					datetime = datetime2;
					//password = encryptedString(key,password);
					password = key.encrypt(password);
					$.getScript("/stock/user/members/ajaxLogin?redirect=0&username=" + username + "&password=" + password + "&key=" + keyStr + "&autoLogin=" + autoLogin + "&end", function(response, status) {
						if (typeof(errorMsg) != 'undefined' && errorMsg != '') {
							// $("#loginErrorMsg").html(errorMsg);
							errorTip($("#login-form-username"), errorMsg);
						} else if(loginResult == true && loginDialog != undefined) {
							$(".logout").text("退出");
							loginDialog.close();
						}
					});
				}
			} catch (e) {
				console.debug(e);
				// console.log('??');
			}
		});
	} else {
		//password = encryptedString(key,password);
		password = key.encrypt(password);
		$.getScript("/stock/user/members/ajaxLogin?redirect=0&username=" + username + "&password=" + password + "&key=" + keyStr + "&autoLogin=" + autoLogin + "&end", function(response, status) {
			if (typeof(errorMsg) != 'undefined' && errorMsg != '') {
				// $("#loginErrorMsg").html(errorMsg);
				errorTip($("#login-form-username"), errorMsg);
			} else if(loginResult == true && loginDialog != undefined) {
				$(".logout").text("退出");
				loginDialog.close();
			}
		});
	}
}


//原生JS loading效果
;(function($){
	$.yfLoading = function(options){
        var param = $.extend({
            text: "努力加载中...", //文字
            position: "absolute", //定位
            target: 'body', //加载目标
            top: '40%',
            left: '40%',
            modal: false, //遮罩背景
            width: 220,
            zIndex: 2000, //z-index顺序
            remove: false
        }, options || {});

        if(param.remove){
            $(".fyLoading, .loadbg").remove();
            return false;
        }

        //正在加载中图片
       // var loadingImg = '/images/loading_new.gif';
        var loadingImg = '/images/66.gif';



        //是否需要遮罩背景
        if(param.modal){
            var scrollWidth = $("body")[0].scrollWidth,
                socrllHeight = $("body")[0].scrollHeight;

            $(".loadbg").remove();

            $("body").prepend("<div class='loadbg' style='background-color:#000; opacity:0.5;position:absolute; left:0; top:0; z-index:"
                + param.zIndex + "; width:"
                + scrollWidth +"px; height:"
                + socrllHeight +"px;'></div>");
        }

        $(".fyLoading").remove();


        $(param.target).prepend("<div class='fyLoading' style='text-indent:80px;display: block; border:0px solid #980a0a; color:#000000; font-size:14px; font-weight:bold; line-height:120px; box-shadow:0 0 1px #fff;  height:80px; z-index:2"
                + (param.zIndex+1)+";width:"
                + param.width + "px; left:"
                + param.left + "; top:"
                + param.top +";background:url("
                + loadingImg +") no-repeat 30px 26px #fff; position:"
                + param.position + ";'>"
                + param.text + "</div>");
/*        $(param.target).prepend("<div class='fyLoading' style='text-indent:50px;display: block; border:0px solid #980a0a; color:#000000; font-size:14px; font-weight:bold; line-height:30px; box-shadow:0 0 1px #fff;  height:65px; z-index:2"
        		+ (param.zIndex+1)+";width:"
        		+ param.width + "px; left:"
        		+ param.left + "; top:"
        		+ param.top +";background:url("
        		+ loadingImg +") no-repeat 20px 3px #fff; position:"
        		+ param.position + ";'>"
        		+ param.text + "</div>");
*/

    };
})(jQuery);


// ico: error   warning  succeed  sTime(单位为秒)
function noticeShow(eWidth,text,ico,sTime,locktof){
	var config = false;
	if (locktof == null|| locktof == undefined) {
		locktof=config;
	};
	art.dialog({
			title: false,
			cancel: false,
			lock:locktof,
			opacity: 0.5,
			width: eWidth,// 必须指定一个像素宽度值或者百分比，否则浏览器窗口改变可能导致artDialog收缩
			content: "<span style='color: #676767;line-height:18px;padding-top:5px;padding-bottom:5px;'>"+text+"</span>",
			icon: ico,
			time: sTime
	}).show();
}
function deletecomfirm(eWidth,text,fn){
	art.dialog({
	    id: 'testID',
	    padding: '20px 20px 0 20px',
	    lock: true,
	    width: eWidth,
	    title: false,
		cancel: false,
	    opacity: .5,
	    content: text,
	    button: [
	        {
	            name: '确定',
	            callback: fn,
	            focus: true
	        },
	        {
	            name: '取消',
	            callback: function () {
	            }
	        }
	    ]
	});
}
function deletecomfirm2(eWidth,text,fn){
	art.dialog({
		id: 'testID',
		padding: '20px 20px 0 20px',
		lock: true,
		width: eWidth,
		title: false,
		cancel: false,
		opacity: .5,
		content: text,
		button: [
		         {
		        	 name: '现在就去',
		        	 callback: fn,
		        	 focus: true
		         },
		         {
		        	 name: '不了,下次',
		        	 callback: function () {
		        	 }
		         }
		         ]
	});
}
function myComfirm(eWidth,text,name,fn){
	art.dialog({
		id: 'testID',
		padding: '20px 20px 0 20px',
		lock: true,
		width: eWidth,
		title: false,
		cancel: false,
		opacity: .5,
		content: text,
		button: [
		         {
		        	 name: name,
		        	 callback: fn,
		        	 focus: true
		         }
		         ]
	});
}
