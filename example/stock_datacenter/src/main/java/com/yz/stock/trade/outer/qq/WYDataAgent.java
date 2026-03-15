package com.yz.stock.trade.outer.qq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.stock.common.model.Company;
import com.stock.common.model.trade.ScldVO;
import com.stock.common.util.Spider;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;


public class WYDataAgent {
	private WYDataAgent() {

	}

	private static WYDataAgent instance = new WYDataAgent();

	public static WYDataAgent getInstance() {
		return instance;
	}

	/**
	 * callback_758268710({
    "page": 0,
    "count": 25,
    "order": -1,
    "total": 1622,
    "pagecount": 65,
    "time": "2013-01-26 15:19:37",
    "key": "finance\/hs\/realtimedata\/radar\/9873cfa9706bb7fe0536f939729f7f84.json",
    "list": [
        {
            "PRICE": 11.48,
            "PERCENT": 0.02775290957923,
            "SYMBOL": "600060",
            "CODE": "0600060",
            "NAME": "\u6d77\u4fe1\u7535\u5668",
            "DATE": "2013-01-25 15:00:00",
            "NUMBER": [
                0.017009847806625
            ],
            "TYPES": [
                "QUICK_UP"
            ],
            "RN": 1,
            "INFO": "1.70%",
            "TYPE": "\u5feb\u901f\u4e0a\u6da8 ",
            "TYPE_COLOR": 1,
            "TITLE": "1\u5206\u949f\u5185\u6da8\u5e45\u8d85\u8fc71.5% "
        }
    ]
})
	 *市场雷达
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<ScldVO> getSCLDData() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/radar.php?host=/hs/realtimedata/service/radar.php&page=0&fields=CODE,NAME,PRICE,PERCENT,DATE,TYPES,SYMBOL,NUMBER&sort=DATE&order=desc&count=25&type=query&callback=callback_758268710&req=61518";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		List<ScldVO>  scl = new ArrayList<ScldVO>();
		s = s.trim();
		JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("(")+1,s.length()-1));
		List<JSONObject> jl = ((List<JSONObject>) jo.get("list"));
		for(JSONObject tjo:jl)
		{
			String t = (String) tjo.get("DATE");
			String suffix =tjo.getString("CODE").toString().charAt(0)=='0'?"sh":"sz";
			String companycode = tjo.get("SYMBOL")+"."+suffix;
			String companyname = tjo.get("NAME").toString();
			Double price = Double.valueOf(tjo.get("PRICE").toString());
			String type = getInfoByType(((JSONArray) tjo.get("TYPES")).getString(0));
			String info = (String) tjo.get("INFO");
			ScldVO sv = new ScldVO(companycode,companyname,price,type,info,t);
			scl.add(sv);
		}
		return scl;
	}
	static Map<String,String> _m = new HashMap<String,String>();
	static 
	{
		_m.put("QUICK_UP", "快速上涨");
		_m.put("LIMIT_DOWN_OPEN", "跌停打开");
		_m.put("LIMIT_DOWN", "跌停");
		_m.put("BUY", "大买单");
		_m.put("SELL", "大卖单");
		_m.put("QUICK_DOWN", "快速下跌");
		_m.put("LIMIT_UP_OPEN", "涨停打开");	
		_m.put("LIMIT_UP", "涨停"); 	
	}
	private String getInfoByType(String k) {
		return _m.get(k);
	}

	public static void main(String[] args)
	{
		WYDataAgent.getInstance().getLXSZData();
	}
	/**
	 * 成交量突增
	 * @return
	 */
	public String getCJLTZData() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/turnoverupdown.php?host=/hs/realtimedata/service/turnoverupdown.php&page=0&query=upDown:1&fields=RN,SYMBOL,NAME,PRICE,LB,VOLUME,TURNOVER,HS,,PERCENT,CODE&sort=LB&order=desc&count=50&type=query&callback=callback_2015582577&req=11447";
		String s = Spider.urlSpider(url, "gbk");
		return parseCjlRet(s,"cjltz");
	}
	/**
	 * 成交量突增
	 * @return
	 */
	public List<Company> getCJLTZData_cl() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/turnoverupdown.php?host=/hs/realtimedata/service/turnoverupdown.php&page=0&query=upDown:1&fields=RN,SYMBOL,NAME,PRICE,LB,VOLUME,TURNOVER,HS,,PERCENT,CODE&sort=LB&order=desc&count=50&type=query&callback=callback_2015582577&req=11447";
		String s = Spider.urlSpider(url, "gbk");
		return parseCjlRet_cl(s,"cjltz");
	}
	
	/**
	 * 成交量聚减
	 * @return
	 */
	public String getCJLJJData() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/turnoverupdown.php?host=/hs/realtimedata/service/turnoverupdown.php&page=0&query=upDown:-1&fields=RN,SYMBOL,NAME,PRICE,LB,VOLUME,TURNOVER,HS,,PERCENT,CODE&sort=LB&order=asc&count=25&type=query&callback=callback_1032390704&req=11447";
		String s = Spider.urlSpider(url, "gbk");
		return parseCjlRet(s,"cjljj");
	}

	/**
	 * 成交量聚减
	 * @return
	 */
	public List<Company> getCJLJJData_cl() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/turnoverupdown.php?host=/hs/realtimedata/service/turnoverupdown.php&page=0&query=upDown:-1&fields=RN,SYMBOL,NAME,PRICE,LB,VOLUME,TURNOVER,HS,,PERCENT,CODE&sort=LB&order=asc&count=25&type=query&callback=callback_1032390704&req=11447";
		String s = Spider.urlSpider(url, "gbk");
		return parseCjlRet_cl(s,"cjljj");
	}
	
	private String parseCjlRet(String s,String key) {
		if(StringUtil.isEmpty(s))
			return null;
		StringBuffer sb = new StringBuffer();
		sb.append("var "+key+"=\"");
		s = s.trim();
		JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("(")+1,s.length()-1));
		List<JSONObject> jl = ((List<JSONObject>) jo.get("list"));
		for(JSONObject tjo:jl)
		{
			String suffix =tjo.getString("CODE").toString().charAt(0)=='0'?"sh":"sz";
			String companycode = tjo.get("SYMBOL")+"."+suffix;
			sb.append(companycode);
			sb.append("|");
			String companyname = tjo.get("NAME").toString();
			sb.append(companyname);
			sb.append("|");
			Double price = Double.valueOf(tjo.get("PRICE").toString());
			sb.append(price);
			sb.append("|");
			Double hs = Double.valueOf(tjo.get("HS").toString());
			sb.append(hs);
			sb.append("|");
			Double lb = Double.valueOf(tjo.get("LB").toString());
			sb.append(lb);
			sb.append("^");
			
		}
		return sb.substring(0, sb.length()-1)+"\"";
	}

	private List<Company> parseCjlRet_cl(String s,String key) {
		if(StringUtil.isEmpty(s))
			return null;
		List<Company> cl = new ArrayList<Company>();
		s = s.trim();
		JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("(")+1,s.length()-1));
		List<JSONObject> jl = ((List<JSONObject>) jo.get("list"));
		for(JSONObject tjo:jl)
		{
			String suffix =tjo.getString("CODE").toString().charAt(0)=='0'?"sh":"sz";
			String companycode = tjo.get("SYMBOL")+"."+suffix;
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
			cl.add(c);
			
		}
		return cl;
	}
	
	/*
	 * 连续上涨
	 */
	public String getLXSZData() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/marketIndexes.php?host=/hs/realtimedata/service/marketIndexes.php&page=0&query=CONTINUOUS_UPDOWN.RIGHT_NOW:int_1&fields=CONTINUOUS_UPDOWN,CODE,RN,SYMBOL,NAME,TYPE,PRICE,PERCENT&sort=CONTINUOUS_UPDOWN.DAYS&order=desc&count=50&type=query&callback=callback_219252403&req=11734";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		 return parseLXSZData(s,"lxsz");
	}

	/*
	 * 连续上涨
	 */
	public List<Company> getLXSZData_cl() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/marketIndexes.php?host=/hs/realtimedata/service/marketIndexes.php&page=0&query=CONTINUOUS_UPDOWN.RIGHT_NOW:int_1&fields=CONTINUOUS_UPDOWN,CODE,RN,SYMBOL,NAME,TYPE,PRICE,PERCENT&sort=CONTINUOUS_UPDOWN.DAYS&order=desc&count=50&type=query&callback=callback_219252403&req=11734";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		 return parseLXSZData_cl(s,"lxsz");
	}
	/*
	 * 连续下跌
	 */
	public List<Company> getLXXDData_cl() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/marketIndexes.php?host=/hs/realtimedata/service/marketIndexes.php&page=0&query=CONTINUOUS_UPDOWN.RIGHT_NOW:int_-1;&fields=CONTINUOUS_UPDOWN,RN,CODE,SYMBOL,NAME,TYPE,PRICE,PERCENT&sort=CONTINUOUS_UPDOWN.DAYS&order=desc&count=25&type=query&callback=callback_1536277048&req=11747";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseLXSZData_cl(s,"lxxd");
	}
	/*
	 * 连续下跌
	 */
	public String getLXXDData() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/marketIndexes.php?host=/hs/realtimedata/service/marketIndexes.php&page=0&query=CONTINUOUS_UPDOWN.RIGHT_NOW:int_-1;&fields=CONTINUOUS_UPDOWN,RN,CODE,SYMBOL,NAME,TYPE,PRICE,PERCENT&sort=CONTINUOUS_UPDOWN.DAYS&order=desc&count=25&type=query&callback=callback_1536277048&req=11747";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseLXSZData(s,"lxxd");
	}
	private String parseLXSZData(String s, String k) {
		StringBuffer sb = new StringBuffer();
		sb.append("var "+k+"=\"");
		s = s.trim();
		JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("(")+1,s.length()-1));
		List<JSONObject> jl = ((List<JSONObject>) jo.get("list"));
		for(JSONObject tjo:jl)
		{
			String suffix =tjo.getString("CODE").toString().charAt(0)=='0'?"sh":"sz";
			String companycode = tjo.get("SYMBOL")+"."+suffix;
			sb.append(companycode);
			sb.append("|");
			String companyname = tjo.get("NAME").toString();
			sb.append(companyname);
			sb.append("|");
			Double price = Double.valueOf(tjo.get("PRICE").toString());
			sb.append(price);
			sb.append("|");
			Double f = Double.valueOf(tjo.get("PERCENT").toString());
			sb.append(f);
			sb.append("|");
			Integer days = (Integer) ((JSONObject) tjo.get("CONTINUOUS_UPDOWN")).get("DAYS");
			sb.append(days);
			sb.append("^");

		}
		return sb.substring(0, sb.length()-1)+"\"";
		
	}
	
	private List<Company> parseLXSZData_cl(String s, String k) {
		s=s.trim();
		List<Company> cl = new ArrayList<Company>();
		JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("(")+1,s.length()-1));
		List<JSONObject> jl = ((List<JSONObject>) jo.get("list"));
		for(JSONObject tjo:jl)
		{
			String suffix =tjo.getString("CODE").toString().charAt(0)=='0'?"sh":"sz";
			String companycode = tjo.get("SYMBOL")+"."+suffix;
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
			cl.add(c);
			

		}
		return cl;
		
	}

	/*
	 * 近期新高
	 */
	public String getJQXGData() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/marketIndexes.php?host=/hs/realtimedata/service/marketIndexes.php&page=0&query=HIGH_LOW_RECENTLY.HIGH_LOW:int_1&fields=RN,SYMBOL,NAME,CODE,TYPE,PRICE,HIGH_LOW_RECENTLY,PERCENT&sort=PRICE&order=DESC&count=25&type=query&callback=callback_173556563&req=11750";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseJQXGData(s,"jqxg");
	}

	/*
	 * 近期新低
	 */
	public String getJQXDData() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/marketIndexes.php?host=/hs/realtimedata/service/marketIndexes.php&page=0&query=HIGH_LOW_RECENTLY.HIGH_LOW:int_-1&fields=RN,SYMBOL,CODE,NAME,TYPE,PRICE,HIGH_LOW_RECENTLY,PERCENT&sort=PRICE&order=ASC&count=25&type=query&callback=callback_9967058&req=2159";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseJQXGData(s,"jqxd");
	}
	private String parseJQXGData(String s, String k) {
		StringBuffer sb = new StringBuffer();
		sb.append("var "+k+"=\"");
		s = s.trim();
		JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("(")+1,s.length()-1));
		List<JSONObject> jl = ((List<JSONObject>) jo.get("list"));
		for(JSONObject tjo:jl)
		{
			String suffix =tjo.getString("CODE").toString().charAt(0)=='0'?"sh":"sz";
			String companycode = tjo.get("SYMBOL")+"."+suffix;
			sb.append(companycode);
			sb.append("|");
			String companyname = tjo.get("NAME").toString();
			sb.append(companyname);
			sb.append("|");
			Double price = Double.valueOf(tjo.get("PRICE").toString());
			sb.append(price);
			sb.append("|");
			Double f = Double.valueOf(tjo.get("PERCENT").toString());
			sb.append(f);
			sb.append("^");

		}
		return sb.substring(0, sb.length()-1)+"\"";
	}
	
	/*
	 * 近期新高
	 */
	public List<Company> getJQXGData_cl() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/marketIndexes.php?host=/hs/realtimedata/service/marketIndexes.php&page=0&query=HIGH_LOW_RECENTLY.HIGH_LOW:int_1&fields=RN,SYMBOL,NAME,CODE,TYPE,PRICE,HIGH_LOW_RECENTLY,PERCENT&sort=PRICE&order=DESC&count=25&type=query&callback=callback_173556563&req=11750";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseJQXGData_cl(s,"jqxg");
	}

	/*
	 * 近期新低
	 */
	public List<Company> getJQXDData_cl() {
		String url = "http://quotes.money.163.com/hs/realtimedata/service/marketIndexes.php?host=/hs/realtimedata/service/marketIndexes.php&page=0&query=HIGH_LOW_RECENTLY.HIGH_LOW:int_-1&fields=RN,SYMBOL,CODE,NAME,TYPE,PRICE,HIGH_LOW_RECENTLY,PERCENT&sort=PRICE&order=ASC&count=25&type=query&callback=callback_9967058&req=2159";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseJQXGData_cl(s,"jqxd");
	}
	private List<Company> parseJQXGData_cl(String s, String k) {
		List<Company> cl = new ArrayList<Company>();
		s = s.trim();
		JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("(")+1,s.length()-1));
		List<JSONObject> jl = ((List<JSONObject>) jo.get("list"));
		for(JSONObject tjo:jl)
		{
			String suffix =tjo.getString("CODE").toString().charAt(0)=='0'?"sh":"sz";
			String companycode = tjo.get("SYMBOL")+"."+suffix;

			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
			cl.add(c);

		}
		return cl;
	}
}
