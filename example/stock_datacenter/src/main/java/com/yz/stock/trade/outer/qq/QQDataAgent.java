package com.yz.stock.trade.outer.qq;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.sf.json.JSONObject;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.trade.ScldVO;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.util.DateUtil;
import com.stock.common.util.Spider;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.MarketService;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.trade.outer.cache.RankCacheService;


public class QQDataAgent {
	private QQDataAgent() {

	}

	private static QQDataAgent instance = new QQDataAgent();

	public static QQDataAgent getInstance() {
		return instance;
	}

	/**
	 * 取市场异动信息
	 * @return
	 */
	public List<ScldVO> getSCLDData() {
		String url = "http://stock.gtimg.cn/data/index.php?appn=radar&t=latest&v=vLATEST";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		List<ScldVO>  scl = new ArrayList<ScldVO>();
		s = s.trim();
		JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("=")+1,s.length()-1));
		String data = jo.get("data").toString();
		String[] dl = data.split("\\^"); 
		String curdate = DateUtil.getSysDate(DateUtil.YYYYMMDD, Calendar.getInstance().getTime());
		for(String d:dl)
		{
			String[] dm = d.split("~");
			if(dm.length<2) continue;
			String t = curdate+" "+dm[0];
			String companycode = dm[1].replaceAll("[a-zA-Z]+", "")+"."+dm[1].replaceAll("[0-9]+", "");
			String companyname = dm[2];
			String price = dm[3];
			String type = dm[4];
			String info = dm[5]+"手";
			ScldVO sv = new ScldVO(companycode,companyname,Double.valueOf(price),type,info,t);
			scl.add(sv);
		}
		return scl;
	}
	
	/**
	 * 大买单频现股
	 * @return
	 */
	public String getDDPX0Data() {
		String url = "http://stock.gtimg.cn/data/view/dataPro.php?t=6&p=1";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseDDPX(s,"ddpx0");
	}
	private String parseDDPX(String s, String name) {
		StringBuffer sb = new StringBuffer();
		sb.append("var "+name+"=\"");
		String ms = s.substring(s.indexOf("=")+1,s.length()-1).replace("'", "");
		ms.trim();
		String[] sa = ms.split("\\^");
		for(int i=0;i<sa.length;i++)
		{
			String ts = sa[i];
			String[] tsa = ts.split("~");
			String companycode = tsa[0].replaceAll("[a-zA-Z]+", "")+"."+tsa[0].replaceAll("[0-9]+", "");
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
			sb.append(companycode);
			sb.append("|");
			sb.append(c.getSimpileName());
			sb.append("|");
			String price = tsa[1];
			sb.append(price);
			sb.append("|");
			StockTrade st = MarketService.getInstance().getStockTradeInfo(companycode);
			Double f = st.getCzf();
			sb.append(f);
			sb.append("^");
		}
		return sb.substring(0, sb.length()-1)+"\"";
	}

	/**
	 * 大卖单频现股
	 * @return
	 */
	public String getDDPX1Data() {
		String url = "http://stock.gtimg.cn/data/view/dataPro.php?t=5&p=1";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseDDPX(s,"ddpx1");
	}
	
	
	
	/**
	 * 大买单频现股
	 * @return
	 */
	public List<Company> getDDPX0Data_cl() {
		String url = "http://stock.gtimg.cn/data/view/dataPro.php?t=6&p=1";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseDDPX_cl(s,"ddpx0");
	}
	private List<Company> parseDDPX_cl(String s, String name) {
		List<Company> cl = new ArrayList<Company>();

		String ms = s.substring(s.indexOf("=")+1,s.length()-1).replace("'", "");
		ms.trim();
		String[] sa = ms.split("\\^");
		for(int i=0;i<sa.length;i++)
		{
			String ts = sa[i];
			String[] tsa = ts.split("~");
			String companycode = tsa[0].replaceAll("[a-zA-Z]+", "")+"."+tsa[0].replaceAll("[0-9]+", "");
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
			cl.add(c);
		}
		return cl;
	}

	/**
	 * 大卖单频现股
	 * @return
	 */
	public List<Company> getDDPX1Data_cl() {
		String url = "http://stock.gtimg.cn/data/view/dataPro.php?t=5&p=1";
		String s = Spider.urlSpider(url, "gbk");
		if(StringUtil.isEmpty(s))
			return null;
		return parseDDPX_cl(s,"ddpx1");
	}
	
	private enum ListType{
		SZDF("szdf","http://stock.gtimg.cn/data/view/rank.php?t=rankasz/chr&p=1&o=1&l=10&v=list_data")/*深圳跌幅前10*/,
		SZZF("szzf","http://stock.gtimg.cn/data/view/rank.php?t=rankasz/chr&p=1&o=0&l=10&v=list_data")/*深圳涨幅前10*/,
		SHDF("shdf","http://stock.gtimg.cn/data/view/rank.php?t=rankash/chr&p=1&o=1&l=10&v=list_data")/*上海跌幅前10*/,
		SHZF("shzf","http://stock.gtimg.cn/data/view/rank.php?t=rankash/chr&p=1&o=0&l=10&v=list_data")/*上海涨幅前10*/;
		
		private String typeName ;
		private String typeValue;
		ListType(String typeName,String typeValue){
			this.typeName = typeName;
			this.typeValue = typeValue;
		}
		public String toString(){
			return this.typeValue;
		}
		public String getTypeName(){
			return this.typeName;
		}
	};
	/*
	 * 涨跌前10
	 */
	public void refreshZDBCache()
	{
		for(ListType listtype : ListType.values()){			
			String url = listtype.toString();
			String s = Spider.urlSpider(url, "gbk");
			if(StringUtil.isEmpty(s))
				return ;
			StringBuffer sb = new StringBuffer();
			sb.append("var "+listtype.getTypeName()+"=\"");
			s = s.trim();
			JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("=")+1,s.length()-1));
			String data = jo.get("data").toString();
			String[] dl = data.split(","); 
			for(String d:dl)
			{
				String tcompanycode = d;
				String companycode = tcompanycode.replaceAll("[a-zA-Z]+", "")+"."+tcompanycode.replaceAll("[0-9]+", "");
				sb.append(companycode);
				sb.append("|");
				Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
				if(c==null) continue;
				sb.append(c.getSimpileName());
				sb.append("|");
				StockTrade st = MarketService.getInstance().getStockTradeInfo(companycode);
				if(st==null) continue;
				sb.append(st.getC());
				sb.append("|");
				sb.append(st.getCzf());
				sb.append("^");
			}
			String rs = sb.substring(0, sb.length()-1)+"\"";
			RankCacheService.getInstance().put(listtype.getTypeName(), rs);
			
		}
	}
	public static void main(String[] args)
	{
		QQDataAgent.getInstance().getSCLDData();
	}

	public String getZD10Cache(int type) {
		String ret = "";
		switch(type)
		{
		case 0 :
			ret = RankCacheService.getInstance().get(ListType.SHZF.typeName);
			break;
		case 1 :
			ret = RankCacheService.getInstance().get(ListType.SHDF.typeName);
			break;
		case 2 :
			ret = RankCacheService.getInstance().get(ListType.SZZF.typeName);
			break;
		case 3 :
			ret = RankCacheService.getInstance().get(ListType.SZDF.typeName);
			break;
			
		}
		return ret;
	}
	
	
	/*
	 * 涨跌前10
	 */
	public void refreshZDBCache_cl()
	{
		for(ListType listtype : ListType.values()){			
			String url = listtype.toString();
			String s = Spider.urlSpider(url, "gbk");
			if(StringUtil.isEmpty(s))
				return ;
			List<Company> cl = new ArrayList<Company>();
			s = s.trim();
			JSONObject jo = JSONObject.fromObject(s.substring(s.indexOf("=")+1,s.length()-1));
			String data = jo.get("data").toString();
			String[] dl = data.split(","); 
			for(String d:dl)
			{
				String tcompanycode = d;
				String companycode = tcompanycode.replaceAll("[a-zA-Z]+", "")+"."+tcompanycode.replaceAll("[0-9]+", "");
				Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
				cl.add(c);
			}
			LCEnter.getInstance().put(listtype.getTypeName(), cl, StockConstants.COMPANY_CACHE_NAME);
		}
	}

	public List<Company> getZD10Cache_cl(int type) {
		List<Company> ret = null;
		switch(type)
		{
		case 0 :
			ret = LCEnter.getInstance().get(ListType.SHZF.typeName,StockConstants.COMPANY_CACHE_NAME);
			break;
		case 1 :
			ret = LCEnter.getInstance().get(ListType.SHDF.typeName,StockConstants.COMPANY_CACHE_NAME);
			break;
		case 2 :
			ret = LCEnter.getInstance().get(ListType.SZZF.typeName,StockConstants.COMPANY_CACHE_NAME);
			break;
		case 3 :
			ret = LCEnter.getInstance().get(ListType.SZDF.typeName,StockConstants.COMPANY_CACHE_NAME);
			break;
			
		}
		return ret;
	}
}
