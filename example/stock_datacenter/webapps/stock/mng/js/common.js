function isNull(o)
{
	if(o==null||o==""||o=='null')
		{
			return true;
		}
	return false;
}
//设置选择框的选定
//function setSelect(id,index)
//{
	//if(index>0)
	//{
	//	$("#"+id+"").get(0).options[parseInt(index)].selected=true;
	//}
	
//}
function showIndexPanel(id,source,top,left)
{
	var ntop =  realOffset(source).y+top;
	var nleft =  realOffset(source).x+left;
	$("#"+id+"").css({'position':'absolute','top':ntop,'left':nleft,'z-index':2});
	$("#"+id+"").slideDown();
	$("#"+id+"").focus();
	return;
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
//初始化年份

function initYear(id,selectIndex)
{
	var yearVar="<option value=\"1995\">1995</option><option value=\"1996\">1996</option><option value=\"1997\">1997</option><option value=\"1998\">1998</option><option value=\"1999\">1999</option><option value=\"2000\">2000</option>";
	yearVar+="<option value=\"2001\">2001</option><option value=\"2002\">2002</option><option value=\"2003\">2003</option><option value=\"2004\">2004</option><option value=\"2005\">2005</option><option value=\"2006\">2006</option>";
	yearVar+="<option value=\"2007\">2007</option><option value=\"2008\">2008</option><option value=\"2009\">2009</option><option value=\"2010\">2010</option><option value=\"2011\">2011</option><option value=\"2012\">2012</option>";
	$("#"+id+"").append(yearVar);
	$("#"+id+"").get(0).options[parseInt(selectIndex)].selected=true;
	
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


