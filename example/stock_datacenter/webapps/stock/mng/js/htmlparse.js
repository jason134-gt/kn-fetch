function load_cf_table_data_js(templatecode,htmlName,parameters)
{
	var url = "http:\/\/localhost:8883\/stock\/html\/getcfcontent?templateId="+templatecode+"&htmlName="+htmlName+"&parameters="+parameters;
	//var s = "<script src=\""+url+"\"><\/script>";
	//document.write(s);
	var script = document.createElement('script');  
	script.setAttribute('src', url);  
	//load javascript  
	document.getElementsByTagName('head')[0].appendChild(script);  
}