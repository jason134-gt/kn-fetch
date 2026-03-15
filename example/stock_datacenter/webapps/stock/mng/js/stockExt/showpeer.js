
//查询公司的同一行业信息并生成行业选择面板
function showPeer(type,source)
{
	if($("#industry_panel").is(":hidden"))
	{
		var companycode = curcompanycode;
		var companyname = curcompanyname;
		var tag = "";
		 $.ajaxSetup({contentType:"application/x-www-form-urlencoded; charset=UTF-8"});
		 $.get("/stock/company/getComanyListByTags?tag="+tag+"&companycode="+companycode,function(result){
				   if(result==null)
						  return;
				var companys = result;
			$("#industry_panel_table").empty();
			 var d = companys.split(";");
			var i=0 ;
			//分隔每个公司
			for(i=0 ;i<d.length;i++)
				{
				var company = d[i];
				//var tt = company.chName+":"+company.companyCode;
				var tt = company;
				$("#industry_panel_table").append("<tr class='alt1'><td><a href='' onclick='openPeerlink(this);return false'>"+tt+"</a></td></tr>");

				}	
					
			   });//end getJSON
				//显示面板
				showIndustryPanel_v2(source);
			   if(type==0)
				   {
				   $("#showIndustryButton").val("隐藏同行");
				   }
			   else
				   {
				   
				   $("#showIndustryButton_cfdata").val("隐藏同行");
				   }
	}
	else
	{
		$("#industry_panel").slideUp();
		   if(type==0)
			   {
			   $("#showIndustryButton").val("显示同行");
			   }
		   else
			   {
			   $("#showIndustryButton_cfdata").val("显示同行");
			   }
		
	}
	
}

function showIndustryPanel_v2(source)
{
		//获取$("#deText")的位置
		var ntop =  source.offsetTop+25;
		var nleft =  source.offsetLeft+60;
		$("#industry_panel").css({'position':'absolute','top':ntop,'left':nleft,'z-index':2,'background-color':'#D9D9D9'});
		$("#industry_panel").slideDown();
		$("#industry_panel").focus();
		return;
}