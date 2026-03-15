package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Industry;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.TreeNode;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.lcs.enter.LCEnter;



public class TagService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static TagService instance = new TagService();
	static Map<String,String> _jhTagMap = new HashMap<String,String>();//取合分类标签集合
	static Map<String,String> _oneTagMap = new HashMap<String,String>();//一键分析标签集合
	static Map<String,String> _a_cf_tag = new HashMap<String,String>();//a组标签
	static Map<String,String> _mk_cf_tag = new HashMap<String,String>();//a组标签
	static Map<String,String> _cmCfMap = new HashMap<String,String>();//公司主页财务标
	
	static List<String> _oneCategoryList = new ArrayList<String>();//聚合分类列表
	static List<String> _jhCategoryList = new ArrayList<String>();//聚合分类列表
	static Set<String> _rs ;
	static Map<String,String> _cMap = new HashMap<String,String>();
	static String _topic_page = "topic_page.topic_page_config_list";
	static String _main_page = "stock_zjs.main_page_config_list";
	static String _search_page = "search_page.search_page_config_list";
	static String _policy_pool = "stock_zjs.policy_pool_configs";
	static String _one_search = "one_search_page.one_search_page_config_list";
	static String _rp_pool = "_rp_pool";//区域省份
	static String _ind_pool = "_ind_pool";//行业
	static
	{
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			public void refresh() {
				rebuildTagConfig();	
			}
			
		});
		Thread t = new Thread(new Runnable(){

			public void run() {
				long ltime = Calendar.getInstance().getTimeInMillis();
				while(true)
				{
					try {
						long uptime = ConfigCenterFactory.getLong(
								"stock_zjs.policy_pool_rebuild_time",
								10 * 60 * 1000l);
						long ctime = Calendar.getInstance().getTimeInMillis();
						if (ctime - ltime > uptime) {
							rebuildPolicypool();
						}
						Thread.sleep(30000);
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
				
			}

		});
		t.setName("products_update_thread");
		t.start();
	}
	private TagService() {

	}

	public static TagService getInstance() {
		return instance;
	}
	private static void rebuildPolicypool() {
		
		String s = ConfigCenterFactory.getString(_policy_pool, "");
		String ns = dorebuildPolicypool(s);
		if(!StringUtil.isEmpty(ns))
			_cMap.put(_policy_pool, ns);
	}
	private static String checkConfigTag(String cs) {
		StringBuilder asb = new StringBuilder();
		if(!StringUtil.isEmpty(cs))
		{
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;
				String ctype = cca[0];//例：财务提示:1
				StringBuilder sb = new StringBuilder();
				sb.append(ctype);
				sb.append("|");
				String[] va = cca[1].split("\\^");
				for(String v:va)
				{
					String id = v.split(":")[1];
					
					List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(id);
					if(cl!=null&&cl.size()>0)
					{
						sb.append(v);
						if(ctype.indexOf("实时行情")<0) 
						{
							sb.append(":");
							sb.append(cl.size());
						}
						sb.append("^");
					}
					else
					{
						//System.out.println("not found companys ;tags="+v);
					}
				}
				String v = sb.toString();
				if(v.endsWith("^"))
				{
					asb.append(v.substring(0,v.length()-1));
				}
				else
				{
					asb.append(v);
				}
				asb.append(";");
			}
		}
		return asb.toString();
		
	}
	
	/*
	 * 分类下不要重复，例如：
	 * 财务提示:1|现金流不足:cf_234^偿债能力差:cf_989;行情标签:2|kdj买入:kdj_mr^MACD买入:macd_mr;
	 */
	public static void rebuildTagConfig() {
		List<String> nlist = new ArrayList<String>();
		String cs = ConfigCenterFactory.getString("jhc_tag.jh_tag_config", "");
		if(!StringUtil.isEmpty(cs))
		{
			Map<String,String> _njhTagMap = new HashMap<String,String>();
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;
				String ctype = cca[0];//例：财务提示:1
				nlist.add(ctype);
				String[] va = cca[1].split("\\^");
				for(String v:va)
				{
					String id = v.split(":")[1];
					String vv = v+":"+ctype.split(":")[1];
					_njhTagMap.put(id, vv);
				}
			}
			
			_jhTagMap = _njhTagMap;
			_jhCategoryList=nlist;
		}
	
		
		nlist = new ArrayList<String>();
		cs = ConfigCenterFactory.getString("stock_tag_config.one_analysis_config", "");
		if(!StringUtil.isEmpty(cs))
		{
			Map<String,String> _noneTagMap = new HashMap<String,String>();
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;
				String ctype = cca[0];//例：财务提示:1
				nlist.add(ctype);
				String[] va = cca[1].split("\\^");
				for(String v:va)
				{
					String id = v.split(":")[1];
					String vv = v+":"+ctype.split(":")[1];
					_noneTagMap.put(id, vv);
				}
			}
			_oneTagMap = _noneTagMap;
			_oneCategoryList=nlist;
		}
		
		nlist = new ArrayList<String>();
		cs = ConfigCenterFactory.getString("stock_tag_config.company_main_cftag", "");
		if(!StringUtil.isEmpty(cs))
		{
			Map<String,String> _noneTagMap = new HashMap<String,String>();
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;
				String ctype = cca[0];//例：财务提示:1
				nlist.add(ctype);
				String[] va = cca[1].split("\\^");
				for(String v:va)
				{
					String id = v.split(":")[1];
					String vv = v+":"+ctype.split(":")[1];
					_noneTagMap.put(id, vv);
				}
			}
			_cmCfMap = _noneTagMap;
		}
		
		nlist = new ArrayList<String>();
		cs = ConfigCenterFactory.getString("tc_config.a_cf_tag", "");
		if(!StringUtil.isEmpty(cs))
		{
			Map<String,String> _noneTagMap = new HashMap<String,String>();
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;
				String ctype = cca[0];//例：财务提示:1
				nlist.add(ctype);
				String[] va = cca[1].split("\\^");
				for(String v:va)
				{
					String id = v.split(":")[1];
					String vv = v+":"+ctype.split(":")[1];
					_noneTagMap.put(id, vv);
				}
			}
			_a_cf_tag = _noneTagMap;
		}
		
		cs = ConfigCenterFactory.getString("tc_config.mk_cf_tag", "");
		if(!StringUtil.isEmpty(cs))
		{
			Map<String,String> _noneTagMap = new HashMap<String,String>();
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;
				String ctype = cca[0];//例：财务提示:1
				nlist.add(ctype);
				String[] va = cca[1].split("\\^");
				for(String v:va)
				{
					String id = v.split(":")[1];
					String vv = v+":"+ctype.split(":")[1];
					_noneTagMap.put(id, vv);
				}
			}
			_mk_cf_tag = _noneTagMap;
		}
		
		CompanyService.getInstance().initAllCompanyTagsSet();
		
		String s = ConfigCenterFactory.getString(_topic_page, "");
		String ns = checkConfigTag(s);
		if(!StringUtil.isEmpty(ns))
		_cMap.put(_topic_page, ns);
		
		 s = ConfigCenterFactory.getString(_main_page, "");
		 ns = checkConfigTag(s);
		 if(!StringUtil.isEmpty(ns))
		_cMap.put(_main_page, ns);
		
		s = ConfigCenterFactory.getString(_search_page, "");
		ns = checkConfigTag(s);
		if(!StringUtil.isEmpty(ns))
		_cMap.put(_search_page, ns);
		
		s = ConfigCenterFactory.getString(_policy_pool, "");
		ns = dorebuildPolicypool(s);
		if(!StringUtil.isEmpty(ns))
		_cMap.put(_policy_pool, ns);
		
		s = ConfigCenterFactory.getString(_one_search, "");
		ns = dorebuildPolicypool(s);
		if(!StringUtil.isEmpty(ns))
		_cMap.put(_one_search, ns);
		
		//构建实时标签池
		initRealTimeTags();
		
		
		
	}
	public static void initIndustry() {
		StringBuilder sb = new StringBuilder();
		String atag = "i_-1_所有行业";
		sb.append("申万行业(一级):97|");
		sb.append("所有行业");
		sb.append(":");
		sb.append(atag);
		List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(atag);
		if(cl!=null&&cl.size()>0)
		{
			sb.append(":");
			sb.append(cl.size());
		}
		sb.append("^");
		List<TreeNode> tl = IndustryService.getInstance().getIndustryYFZXTreeFromCache(1).get(atag).getChildren();
		for(TreeNode tn:tl)
		{
			Industry ind = (Industry) tn.getReference();
			String tag = ind.getName();
			sb.append(tag.split("_")[2]);
			sb.append(":");
			sb.append(tag);
			 cl = CompanyService.getInstance().getCompanyListByTagFromCache(tag);
			if(cl!=null&&cl.size()>0)
			{
				sb.append(":");
				sb.append(cl.size());
			}
			sb.append("^");
		}
		_cMap.put(_ind_pool, sb.toString());
	}

	public String getIndTags()
	{
		return _cMap.get(_ind_pool);
	}
	public static void initRegionProvinceTags() {
		// TODO Auto-generated method stub
		StringBuilder sb1 = new StringBuilder();
		sb1.append("区域板块:98|");
		
		StringBuilder sb2 = new StringBuilder();
		sb2.append("省份板块:99|");
		List<String> taglist = CompanyService.getInstance().getAllTags();
		for(String tag : taglist)
		{
			if(tag.startsWith("r_"))
			{
				sb1.append(tag.split("_")[1]);
				sb1.append(":");
				sb1.append(tag);
				List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(tag);
				if(cl!=null&&cl.size()>0)
				{
					sb1.append(":");
					sb1.append(cl.size());
				}
				sb1.append("^");
			}
			if(tag.startsWith("p_"))
			{
				sb2.append(tag.split("_")[1]);
				sb2.append(":");
				sb2.append(tag);
				List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(tag);
				if(cl!=null&&cl.size()>0)
				{
					sb2.append(":");
					sb2.append(cl.size());
				}
				sb2.append("^");
			}
		}
		sb1.append(";").append(sb2.toString()).append(";");
		_cMap.put(_rp_pool, sb1.toString());
	}
	public String getRpTags()
	{
		return _cMap.get(_rp_pool);
	}
	private static void initRealTimeTags() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		String ps = ConfigCenterFactory.getString(_policy_pool, "");
		String rs = getRealTimeTags();
		if(rs!=null)
		{
			sb.append(rs);
			if(!rs.endsWith(";"))
				sb.append(";");
		}
		if(ps!=null)
		{
			sb.append(ps);
			if(!ps.endsWith(";"))
				sb.append(";");
		}
		reinitRealTimeTagSet(sb.toString());
	}

	private static String dorebuildPolicypool(String cs) {
		StringBuilder asb = new StringBuilder();
		if(!StringUtil.isEmpty(cs))
		{
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;
				String ctype = cca[0];//例：财务提示:1
				StringBuilder sb = new StringBuilder();
				sb.append(ctype);
				sb.append("|");
				String[] va = cca[1].split("\\^");
				for(String v:va)
				{
					String ids = v.split(":")[1];
//					String name = v.split(":")[0];
					if(StringUtil.isEmpty(ids)) continue;
					String tids = ids.replaceAll("&", "^");
					List<Company> cl = CompanyService.getInstance().computeCompanyListJJV2(tids,null);
					if(cl!=null&&cl.size()>0)
					{
						sb.append(v);
						sb.append(":");
						sb.append(cl.size());
						sb.append("^");
						
						if(TagService.getInstance().isJHTag(ids))
						{
							Map<String,Company> cm = new HashMap<String,Company>();
							for(Company c:cl)
							{
								if(c!=null)
									cm.put(c.getCompanyCode(), c);
							}
							LCEnter.getInstance().put(StockUtil.getHqMKey(ids), cm,
									StockConstants.COMPANY_CACHE_NAME);
						}
						
						
						LCEnter.getInstance().put(ids, cl,
								StockConstants.COMPANY_CACHE_NAME);
					}
					else
					{
						//System.out.println("not found companys ;tags="+v);
					}
				}
				String v = sb.toString();
				if(v.endsWith("^"))
				{
					asb.append(v.substring(0,v.length()-1));
				}
				else
				{
					asb.append(v);
				}
				asb.append(";");
			}
		}
		return asb.toString();
	}

	public List<String> getJHCategoryList()
	{
		return _jhCategoryList;
	}
	public boolean isJHTag(String tag)
	{
		if(!StringUtil.isEmpty(tag)&&_jhTagMap.get(tag)!=null)
			return true;
		return false;
	}
	public String getJHTagInfo(String tag)
	{
		if(!StringUtil.isEmpty(tag))
			return _jhTagMap.get(tag);
		return null;
	}
	/**
	 * 系统首页
	 * @return
	 */
	public String getMainPageProductConfigList()
	{
		return _cMap.get(_main_page);
	}
	/**
	 * 找股页
	 * @return
	 */
	public String getSearchPageProductConfigList()
	{
		return _cMap.get(_search_page);
	}
	/**
	 * 专题页
	 * @return
	 */
	public String getTopicPageProductConfigList()
	{
		return _cMap.get(_topic_page);
	}
	/**
	 * 一键选股项
	 * @return
	 */
	public String getOneSearchPageItemList()
	{
		return _cMap.get(_one_search);
	}
	/**
	 * 一键分析项
	 * @return
	 */
	public String getOneAnalysisPageConfigList()
	{
		return ConfigCenterFactory.getString("stock_zjs.one_analysis_page_config_list", "");
	}
	
	/**
	 * 取策略
	 * @return
	 */
	public String getPolicypoolConfigList()
	{
		return  _cMap.get(_policy_pool);
	}
	
	private static String getRealTimeTags() {
		String cs = ConfigCenterFactory.getString("stock_zjs.jh_tag_config", "");
		if(!StringUtil.isEmpty(cs))
		{
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;
				String ctype = cca[0];//例：财务提示:1
				//实时行情
				if(ctype.indexOf("实时行情")<0) 
					continue;
				return cc;
			}
		}
		return "";
		
	}
	
	private static void reinitRealTimeTagSet(String cs) {
		Set<String> nrs = new HashSet<String>();
		if(!StringUtil.isEmpty(cs))
		{
			String[] ca = cs.split(";");
			for(String cc:ca)
			{
				String[] cca = cc.split("\\|");
				if(cca.length<=1) continue;

				String[] va = cca[1].split("\\^");
				for(String v:va)
				{
					String id = v.split(":")[1];
					nrs.add(id);
				}
			}
			_rs = nrs;
		}
		
	}
	
	/**
	 * 实时标签更新比较频繁，为了保证数据一致性，统一用这种方式来取
	 */
	public static Set<String> getRealTimeJhcTagSet(String companycode) {
		if(_rs==null) return null;
		Set<String> rss = new HashSet<String>();
		for(String tag:_rs)
		{
			Map<String,Company> cm = LCEnter.getInstance().get(StockUtil.getHqMKey(tag), SCache.CACHE_NAME_COMPANY);
			if(cm!=null&&cm.get(companycode)!=null)
				rss.add(tag);
				
		}
		return rss;
	}

	public String getMainAnalysisPlatesConfig() {
		// TODO Auto-generated method stub
		return ConfigCenterFactory.getString("stock_zjs.main_analysis_plates_config", "");
	}
	
	public String getOneAnalysisTagsInfo(String tag)
	{
		return _oneTagMap.get(tag);
	}
	public String getCmCfTag(String tag)
	{
		return _cmCfMap.get(tag);
	}
	public String getACfTag(String tag)
	{
		return _a_cf_tag.get(tag);
	}
	public List<String> getOneAnalysisCategorys()
	{
		return _oneCategoryList;
	}

	public String getMkCfTag(String tag) {
		// TODO Auto-generated method stub
		return _mk_cf_tag.get(tag);
	}
}
