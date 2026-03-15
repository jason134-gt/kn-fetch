//function columnArray(){
//	var arr = new Array(); 
//}

function ScreenWriteResult(){
	var strBodyStart = ("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"4\">")
	+("	<tbody>");
	var strBodyEnd =
	("	<\/tbody>")
	+("<\/table>")
	+("");
	
this.toHtml = function(resultArray,s,d){
	return strBodyStart + this.writeRowNum(resultArray) + this.writeTop(resultArray[0],s,d) + this.writeRow(resultArray)  + strBodyEnd;
};
	
this.writeTop = function(resultObj,s,d){
	var strTop =
	 ("		<tr valign=\"top\" class=\"hilite\">")	
	+("			<td width=\"60\" align=\"left\" class=\"top_row\"><b>"+resultObj.companyName+"<\/b><\/td>")
	+("			<td width=\"20%\" align=\"left\" class=\"top_row\"><b>"+resultObj.stockCode+"<\/b><\/td>");
	var valueArr = resultObj.valueArr;
	for(var j =0,vLen =valueArr.length;j<vLen;j++){
		var cArr = valueArr[j].split(";");
		strTop += 	("<td width=\"100px\" style=\"text-align: right;\" class=\"top_row\">&nbsp;");
		if(cArr[0] == s){
			if(d=="true"){
				strTop += "<img width=\"14\" height=\"12\" src=\"images/sort_down.gif\">";
			}else if(d == "false"){
				strTop += "<img width=\"14\" height=\"12\" src=\"images/sort_up.gif\">";
			}
		}
		strTop +=("<a onclick=\"_changeSort('"+cArr[0]+"')\" class=\"activelink\">")
		+("<b>"+ cArr[1]+"<\/b><\/a><\/td>");
	}
	strTop +=("			<td class=\"top_row\">&nbsp;<\/td>")
	+("		<\/tr>");
	return strTop;
};

this.writeRow = function(resultArray){
	var strRow ="";	
	for (var i = 1, len = resultArray.length; i < len; i++) {
		var resultObj = resultArray[i];
		if(resultObj == null )break;
		strRow += ("		<tr class=\"highlightGrey\">")
		+("			<td width=\"20%\" style=\"text-align: left; white-space: nowrap;\"><a")
		+("				target=\"_blank\"")
		+("				href=\"html\/company\/"+ resultObj.stockCode +"\/index.html\">"+ resultObj.companyName+"<\/a>&nbsp;&nbsp;<\/td>")
		+("			<td width=\"60\" align=\"left\"><a target=\"_blank\"")
		+("				href=\"\/finance?q=3A300261\">"+resultObj.stockCode+"<\/a><\/td>");
		var valueArr = resultObj.valueArr;
		for(var j =0,vLen =valueArr.length;j<vLen;j++){
			strRow +=("			<td width=\"100px\" style=\"text-align: right;\">"+valueArr[j]+"<\/td>			");
		}
		strRow +=("			<td><\/td>")
		+("		<\/tr>")
		+("		");
	}	
	return strRow;	
}
this.writeRowNum = function(resultArray){
	var strRowNum = 
	("		<tr class=\"tptr\">")
	+("			<td class=\"rgt\" colspan=\"6\">查询结果 共" + (resultArray.length -1) + " 条				")

	+("			<td colspan=\"1\"><\/td>");
	+("		<\/tr>");
	return strRowNum;
};

}
function writeStockHeader(/*arrStr*/reObj,code,name){
	/*var arr = arrStr.split(",");
	var name = arr[0];
	var dayPrice = arr[1];//今天开盘价
	var yestdayPrice = arr[2];//昨日收盘价
	var nowPrice = arr[3];//目前价格
	var dayMaxPrice = arr[4];
	var dayMinPrice = arr[5];
	var tradingNum = arr[8];
	var tradingPrice = arr[9];
	var time = arr[30] + " " + arr[31];*/
	var dayPrice = reObj.jk;//今天开盘价
	var yestdayPrice = reObj.zs;//昨日收盘价
	var nowPrice = reObj.c;//目前价格
	var dayMaxPrice = reObj.h;
	var dayMinPrice = reObj.l
	var tradingNum = reObj.cjl;
	var tradingPrice = reObj.cje;
	
	var text = "<table class='myTable'><tr><td class='col_1'><div class='name'>"+name+"</div><span class='code'>("+code+")</span></td><td class='col_2'><div class='stock_detail'><table><tr><td>";
	if(nowPrice==0){
		text += "<strong style='color:red;'>" + nowPrice+"</strong>";
	}else{
		text += "<strong style='color:red;'>" + nowPrice+"</strong>&nbsp; "+(nowPrice-yestdayPrice).toFixed(2)+" <br>"+((nowPrice/yestdayPrice-1)*100).toFixed(2)+"%";
	}
	text +="</td><td>今开：<strong>"+dayPrice+"</strong><br>昨收：<strong >"+yestdayPrice+"</strong></td><td>最高：<strong >"+dayMaxPrice+"</strong><br>最低：<strong>"+dayMinPrice+"</strong></td><td>成交量：<strong>"+(tradingNum>100000000?((tradingNum/100000000).toFixed(2)+"亿"):((tradingNum/1000000).toFixed(2)+"万"))+"手</strong><br>成交额：<strong>"+(tradingPrice>100000000?((tradingPrice/100000000).toFixed(2)+"亿"):((tradingPrice/10000).toFixed(2)+"万"))+"元</strong></td></tr></table></div></td></tr></table>";
	return text;
}