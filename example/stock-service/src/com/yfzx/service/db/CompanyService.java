package com.yfzx.service.db;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.BFConst;
import com.stock.common.bloomfilter.BFUtil;
import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.model.Company;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Industry;
import com.stock.common.model.Matchinfo;
import com.stock.common.model.SSeries;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.model.company.Company0018;
import com.stock.common.model.company.Company0019;
import com.stock.common.model.company.Stock0001;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.msg.common.CIndexSeries;
import com.stock.common.msg.common.DataItem;
import com.stock.common.msg.common.SimpleDataItemV2;
import com.stock.common.util.ComparetorUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.Spider;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.TreeNode;
import com.stock.common.util.VmUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.company.Company0018Service;
import com.yfzx.service.db.company.Company0019Service;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.msg.TalkMessageService;
import com.yfzx.service.msg.UsubjectEventService;
import com.yfzx.service.trade.StockTradeService;
import com.yfzx.service.trade.TradeBitMapService;
import com.yfzx.service.trade.TradeCenter;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class CompanyService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger("CompanyService");
	private static CompanyService instance = new CompanyService();
	private Map<String, String> _abMap = new ConcurrentHashMap<String, String>();
	static {
		// 初始加载AB股信息
		initLatestJBCompany();
		CompanyService.getInstance().initABStock();

		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			public void refresh() {
				initLatestJBCompany();
				CompanyService.getInstance().initABStock();

			}

		});
	}

	private CompanyService() {

	}

	public static CompanyService getInstance() {
		return instance;
	}

	public static void initLatestJBCompany() {
		// TODO Auto-generated method stub
		Date uptime = ConfigCenterFactory.getDate("stock_zjs.latest_jb_uptime",
				StockUtil.getApproPeriod(new Date()));

		String key = SCache.CACHE_KEY_LATEST_PUBLISH_JB_COMPANYS;
		List<Company> cl = CompanyService.getInstance().getDataUpCompanyList(
				uptime);
		LCEnter.getInstance().put(key, cl, SCache.CACHE_NAME_COMPANY);
		CompanyService.getInstance().appendTag2Clist(key, cl);
	}

	public void initAllCompanyBaseIndexsData() {
		for (Company c : getCompanyListFromCache()) {
			initCompanyIndexData(c);
		}

	}

	private void initABStock() {
		String abs = ConfigCenterFactory.getString("stock_zjs.AB_Stock", "");
		if (!StringUtil.isEmpty(abs)) {
			for (String ab : abs.split(";")) {
				try {
					String ac = ab.split("\\^")[0].split(":")[1];
					String bc = ab.split("\\^")[1].split(":")[1];
					_abMap.put(ac.toLowerCase(), bc.toLowerCase());
					_abMap.put(bc.toLowerCase(), ac.toLowerCase());
				} catch (Exception e) {
					Log.error("build ABStock failed!ab=" + ab);
				}
			}

		}

	}

	public Map<String, Object> getCompanyMap(Map<String, Object> pm) {
		String companyCode = String.valueOf(pm.get("CompanyCode"));
		// 加载公司数据
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			Company vmCompany = CompanyService.getInstance().getCompanyByCode(
					companyCode);
			List<Company0018> company0018Array = Company0018Service
					.getInstance().getLastCompany0018Array(
							vmCompany.getStockCode());
			List<Company0019> company0019Array = Company0019Service
					.getInstance().getLastCompany0019Array(
							vmCompany.getStockCode());
			TemplateService ts = TemplateService.getInstance();
			map.put("vmUtil", new VmUtil());
			map.put("company", vmCompany);
			map.put("ts", ts);
			map.put("company0018Array", company0018Array);
			map.put("company0019Array", company0019Array);
		} catch (Exception e) {
			logger.error("build company map failed!");
		}
		return map;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, String> getCompanyInfoByCode(String companycode) {
		Map<String, String> m = new HashMap<String, String>();
		try {
			Company c = CompanyService.getInstance().getCompanyByCode(
					companycode);
			Class clazz = c.getClass();
			Field[] fa = clazz.getDeclaredFields();
			for (Field f : fa) {
				String fname = f.getName();
				Method gmethod = clazz.getDeclaredMethod("get"
						+ fname.substring(0, 1).toUpperCase()
						+ fname.substring(1));
				Object v = gmethod.invoke(c);
				if (v != null) {
					m.put("company." + fname, v.toString());
				}

			}
		} catch (Exception e) {
			logger.error("build company map failed!");
		}
		return m;
	}

	public Company getCompanyByCode(String companyCode) {

		return getCompanyByCodeFromCache(companyCode);
	}

	public Company getCompanyByCodeFromDB(String companyCode) {
		Company com = new Company();
		com.setCompanyCode(companyCode);
		String sqlMapKey = StockUtil.getBuildSqlMapKey(com,
				StockSqlKey.company_key_0);
		RequestMessage req = DAFFactory.buildRequest(companyCode, sqlMapKey,
				com, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Company) value;
	}

	public List<Company> getCompanyByCodes(String companyCodes) {
		Company com = new Company();
		String sqlMapKey = StockUtil.getBuildSqlMapKey(com, "selectList");
		RequestMessage req = DAFFactory.buildRequest(companyCodes, sqlMapKey,
				com, StockConstants.COMPANY_TYPE);
		Object o = pLayerEnter.queryForList(req);
		if (o == null) {
			return null;
		}
		return (List<Company>) o;
	}

	public Company getCompanyByCodeFromCache(String companyCode) {
		Object value = LCEnter.getInstance().get(companyCode,
				CacheUtil.getCacheName(StockConstants.COMPANY_TYPE));
		if (value == null) {
			return null;
		}
		return ((Company) value);
	}

	public Company getCompanyByStockCode(String stockCode) {
		Company com = new Company();
		com.setStockCode(stockCode);
		String sqlMapKey = StockUtil.getBuildSqlMapKey(com,
				StockSqlKey.company_key_0 + "ByStockCode");
		RequestMessage req = DAFFactory.buildRequest(stockCode, sqlMapKey, com,
				StockConstants.COMPANY_TYPE);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Company) value;
	}

	public List<Company> getCompanyList() {
		RequestMessage req = DAFFactory.buildRequest("loadcompany2cache",
				false, StockConstants.COMPANY_TYPE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Company>) value;
	}

	/**
	 * 预加载到缓存
	 */
	private static void init() {
		try {
			// TODO Auto-generated method stub
			// RequestMessage req = DAFFactory.buildRequest(
			// StockSqlKey.company_key_1, false, StockConstants.COMPANY_TYPE);
			// Object value = pLayerEnter.queryForList(req);
			// if (value == null) {
			// return;
			// }
			// LCEnter.getInstance().put(getListKey(), value,
			// CacheUtil.getCacheName(StockConstants.COMPANY_TYPE));
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("load company list failed!", e);
		}
		return;
	}

	private static String getListKey() {
		// TODO Auto-generated method stub
		return StockConstants.ALL_COMPANY_CACHE_KEY;
	}

	// 取所有的有效的标签
	public List<String> getAllTags() {
		// TODO Auto-generated method stub
		return LCEnter.getInstance().get(StockConstants.ALL_COMPANY_TAGS_KEY,
				CacheUtil.getCacheName(StockConstants.COMPANY_TYPE));
	}

	public List getCompanyListByTagFromDb(String tag) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("tags", tag);
		RequestMessage req = DAFFactory.buildRequest(
				"getCompanyByIndustryCode", m, StockConstants.COMPANY_TYPE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List) value;
	}

	public List getCompanyByIndustryCode(Company c) {
		RequestMessage req = DAFFactory.buildRequest(
				"getCompanyByIndustryCode", c, StockConstants.COMPANY_TYPE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List) value;
	}

	@SuppressWarnings("unchecked")
	public List<Company> getCompanyListByTagFromCache(String tag) {
		Object value = LCEnter.getInstance().get(tag,
				CacheUtil.getCacheName(StockConstants.COMPANY_TYPE));
		if (value == null) {
			return null;
		}
		return ((List<Company>) value);
	}

	@SuppressWarnings("unchecked")
	public List<Company> getCompanyListByTagFromCacheRemoveBST(String tag) {
		List<Company> ncl = new ArrayList<Company>();
		Object value = LCEnter.getInstance().get(tag,
				CacheUtil.getCacheName(StockConstants.COMPANY_TYPE));
		if (value == null) {
			return null;
		}
		List<Company> cl = ((List<Company>) value);
		for (Company c : cl) {
			if (removeBST(c))
				continue;
			ncl.add(c);
		}
		return ncl;
	}

	public Company getCompanyByName(String cName) {
		Company c = new Company();
		c.setSimpileName(cName);
		RequestMessage req = DAFFactory.buildRequest("getCompanyByName", c,
				StockConstants.COMPANY_TYPE);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Company) value;
	}

	public Company getCompanyByNameFromCache(String simpileName) {
		Object value = LCEnter.getInstance().get(
				StockUtil.getCompanyNameKey(simpileName),
				CacheUtil.getCacheName(StockConstants.COMPANY_TYPE));
		if (value == null) {
			return null;
		}
		return ((Company) value);
	}

	// public List<Company> getConpanyListOfDateUpdate(Date mtime) {
	// List<Company> cl = new ArrayList<Company>();
	// List<String> cls = new ArrayList<String>();
	// try {
	// LCEnter lcEnter = LCEnter.getInstance();
	// List<TableSystem> tsl = TableSystemService.getInstance()
	// .getTableSystemListByDs("ds_0002");
	// for (TableSystem ts : tsl) {
	// Matchinfo mi = lcEnter.get(ts.getTableSystemCode() + "."
	// + StockConstants.TABLE_TYPE_0,
	// StockConstants.MATCH_INFO_CACHE);
	//
	// List<String> ncls = CompanyService.getInstance()
	// .getCompanyOfDataUpdateInAsset(
	// mi.getSystemChildTableName(), mtime);
	// if(ncls!=null)
	// cls.addAll(ncls);
	// }
	// // return null;
	// } catch (Exception e) {
	// logger.error("query company of data update failed!", e);
	// }
	// for (String code : cls) {
	// Company c = CompanyService.getInstance().getCompanyByCode(code);
	// cl.add(c);
	// }
	// return cl;
	// }

	// 取数据更新了的公司---此处只查看资产表中的各公司的数据是否更新了
	public List<String> getConpanyListOfDateUpdate(String tsc, Date mtime) {
		List<String> cls = null;
		try {
			LCEnter lcEnter = LCEnter.getInstance();
			Matchinfo mi = lcEnter.get(tsc + "." + StockConstants.TABLE_TYPE_0,
					StockConstants.MATCH_INFO_CACHE);

			cls = CompanyService.getInstance().getCompanyOfDataUpdateInAsset(
					mi.getSystemChildTableName(), mtime);
			// return null;
		} catch (Exception e) {
			logger.error("query company of data update failed!", e);
		}
		return cls;
	}

	public List<String> getCompanyOfDataUpdateInAsset(String assetTableName,
			Date stime) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("tableName", assetTableName);
		m.put("modtime", DateUtil.format2String(stime));
		RequestMessage req = DAFFactory.buildRequest("getUpdateDataCompany", m,
				StockConstants.COMPANY_TYPE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List) value;
	}

	public Date getAssetLatestModtime() {
		RequestMessage req = DAFFactory.buildRequest("getAssetLatestModtime",
				StockConstants.COMPANY_TYPE);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Date) value;
	}

	public List<Map> getCompanyListOfDataUpdateInAsset(String assetTableName,
			Date stime) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("tableName", assetTableName);
		m.put("modtime", DateUtil.format2String(stime));
		RequestMessage req = DAFFactory.buildRequest(
				"getUpdateDataCompanyList", m, StockConstants.COMPANY_TYPE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List) value;
	}

	public void insertCompany(Company c) {
		// TODO Auto-generated method stub
		RequestMessage req = DAFFactory.buildRequest(c.getKey(),
				"com.yz.stock.portal.model.company.Company.insert", c,
				StockConstants.COMPANY_TYPE);
		pLayerEnter.insert(req);
	}

	public void updateCompany(Company c) {
		// TODO Auto-generated method stub
		RequestMessage req = DAFFactory.buildRequest(c.getKey(),
				"com.yz.stock.portal.model.company.Company.updateByCode", c,
				StockConstants.COMPANY_TYPE);
		pLayerEnter.modify(req);
	}

	public void addTags2Company(String companycode, String ts) {
		if (StringUtil.isEmpty(companycode) || StringUtil.isEmpty(ts))
			return;
		Company c = getCompanyByCodeFromCache(companycode);
		if (c != null) {
			String otags = c.getTags();
			Set<String> otagsSet = new HashSet<String>();
			if (!StringUtil.isEmpty(otags)) {
				for (String ot : otags.split(";"))
					if (!StringUtil.isEmpty(ot)
							&& (ot.startsWith("i_") || ot.startsWith("mtag_")))
						otagsSet.add(ot);
			}

			String[] tags = ts.split(";");
			for (String t : tags) {

				if (!otagsSet.contains(t)) {
					otagsSet.add(t);
				}
			}
			StringBuffer sb = new StringBuffer();
			if (!otagsSet.isEmpty()) {
				for (String tag : otagsSet) {
					sb.append(tag);
					sb.append(";");
				}
			}
			updateCompanyTags(companycode, sb.toString());
		}

	}

	public void addTags2Company(Company c, String ts) {
		if (StringUtil.isEmpty(ts))
			return;
		if (c != null) {
			String otags = c.getTags();
			Set<String> otagsSet = new HashSet<String>();
			if (!StringUtil.isEmpty(otags)) {
				for (String ot : otags.split(";"))
					if (!StringUtil.isEmpty(ot))
						otagsSet.add(ot);
			}

			String[] tags = ts.split(";");
			for (String t : tags) {

				if (!otagsSet.contains(t)) {
					otagsSet.add(t);
				}
			}
			StringBuffer sb = new StringBuffer();
			if (!otagsSet.isEmpty()) {
				for (String tag : otagsSet) {
					sb.append(tag);
					sb.append(";");
				}
			}
			updateCompanyTags(c.getCompanyCode(), sb.toString());
		}

	}

	public void addMtag2Company(String companycode, String ts) {
		if (StringUtil.isEmpty(companycode) || StringUtil.isEmpty(ts))
			return;
		Company c = getCompanyByCodeFromCache(companycode);
		if (c != null) {
			String otags = c.getTags();
			Set<String> otagsSet = new HashSet<String>();
			if (!StringUtil.isEmpty(otags)) {
				for (String ot : otags.split(";"))
					if (!StringUtil.isEmpty(ot) && !ot.startsWith("mtag_"))
						otagsSet.add(ot);
			}

			String[] tags = ts.split(";");
			for (String t : tags) {

				if (!otagsSet.contains(t)) {
					otagsSet.add(t);
				}
			}
			StringBuffer sb = new StringBuffer();
			if (!otagsSet.isEmpty()) {
				for (String tag : otagsSet) {
					sb.append(tag);
					sb.append(";");
				}
			}
			updateCompanyTags(companycode, sb.toString());
		}

	}

	public void addMtag2Company(Company c, String ts) {
		if (StringUtil.isEmpty(ts))
			return;
		if (c != null) {
			String otags = c.getTags();
			Set<String> otagsSet = new HashSet<String>();
			if (!StringUtil.isEmpty(otags)) {
				for (String ot : otags.split(";"))
					if (!StringUtil.isEmpty(ot) && !ot.startsWith("mtag_"))
						otagsSet.add(ot);
			}

			String[] tags = ts.split(";");
			for (String t : tags) {

				if (!otagsSet.contains(t)) {
					otagsSet.add(t);
				}
			}
			StringBuffer sb = new StringBuffer();
			if (!otagsSet.isEmpty()) {
				for (String tag : otagsSet) {
					sb.append(tag);
					sb.append(";");
				}
			}
			updateCompanyTags(c.getCompanyCode(), sb.toString());
		}

	}

	public void delTagsOfCompany(String companycode, String ts) {
		if (StringUtil.isEmpty(companycode) || StringUtil.isEmpty(ts))
			return;
		Company c = getCompanyByCodeFromCache(companycode);
		if (c != null) {
			String otags = c.getTags();
			Set<String> otagsSet = new HashSet<String>();
			if (!StringUtil.isEmpty(otags)) {
				for (String ot : otags.split(";"))
					if (!StringUtil.isEmpty(ot))
						otagsSet.add(ot);
			}

			String[] dtags = ts.split(";");
			for (String t : dtags) {

				if (otagsSet.contains(t)) {
					otagsSet.remove(t);
				}
			}
			StringBuffer sb = new StringBuffer();
			if (!otagsSet.isEmpty()) {
				for (String tag : otagsSet) {
					sb.append(tag);
					sb.append(";");
				}
			}
			updateCompanyTags(companycode, sb.toString());
		}

	}

	public void delTagsOfCompany(Company c, String ts) {
		if (StringUtil.isEmpty(ts))
			return;
		if (c != null) {
			String otags = c.getTags();
			Set<String> otagsSet = new HashSet<String>();
			if (!StringUtil.isEmpty(otags)) {
				for (String ot : otags.split(";"))
					if (!StringUtil.isEmpty(ot))
						otagsSet.add(ot);
			}

			String[] dtags = ts.split(";");
			for (String t : dtags) {

				if (otagsSet.contains(t)) {
					otagsSet.remove(t);
				}
			}
			StringBuffer sb = new StringBuffer();
			if (!otagsSet.isEmpty()) {
				for (String tag : otagsSet) {
					sb.append(tag);
					sb.append(";");
				}
			}
			updateCompanyTags(c.getCompanyCode(), sb.toString());
		}

	}

	private void updateCompanyTags(String companycode, String otags) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companycode);
		m.put("tags", otags);
		RequestMessage req = DAFFactory.buildRequest("updateCompanyTags", m,
				StockConstants.COMPANY_TYPE);
		pLayerEnter.modify(req);

	}

	/**
	 * 先暂时就取1
	 * 
	 * @param tag
	 * @param companycode
	 * @return
	 */
	public Double getWeightOfTag(String tag, String companycode) {
		// TODO Auto-generated method stub
		return 1.0;
	}

	public List<Company> getCompanyListFromCache() {
		return LCEnter.getInstance().get(StockConstants.ALL_COMPANY_CACHE_KEY,
				StockConstants.COMPANY_CACHE_NAME);
	}

	public void initCompanyIndexData(Company c) {
		IndexMessage im1;
		Double xv = 0.0;
		Date ctime = new Date();
		// 取综合能力评分
//		ctime = IndexService.getInstance().getRealtime(
//				StockConstants.INDEX_CODE_ZHNL, c.getCompanyCode());
		ctime = ConfigCenterFactory.getDate("stock_zjs.ind_Latest_period_time",StockUtil.getApproPeriod(new Date()));
//		System.out.println("====================>" + UtilDate.getDateFormatter(ctime));
		if (ctime != null) {
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_ZHNL, ctime);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0) {
				c.setZhnl(xv);
				c.putAttr(StockConstants.INDEX_CODE_ZHNL, xv);
			}
		}

//		ctime = IndexService.getInstance().getRealtime(
//				StockConstants.INDEX_CODE_YLNL, c.getCompanyCode());
		if (ctime != null) {
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_YLNL, ctime);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.putAttr(StockConstants.INDEX_CODE_YLNL, xv);
		}

//		ctime = IndexService.getInstance().getRealtime(
//				StockConstants.INDEX_CODE_YLZL, c.getCompanyCode());
		if (ctime != null) {
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_YLZL, ctime);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.putAttr(StockConstants.INDEX_CODE_YLZL, xv);
		}

//		ctime = IndexService.getInstance().getRealtime(
//				StockConstants.INDEX_CODE_CZNL, c.getCompanyCode());
		if (ctime != null) {
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_CZNL, ctime);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.putAttr(StockConstants.INDEX_CODE_CZNL, xv);
		}

//		ctime = IndexService.getInstance().getRealtime(
//				StockConstants.INDEX_CODE_YYNL, c.getCompanyCode());
		if (ctime != null) {
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_YYNL, ctime);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.putAttr(StockConstants.INDEX_CODE_YYNL, xv);
		}

//		ctime = IndexService.getInstance().getRealtime(
//				StockConstants.INDEX_CODE_GKNL, c.getCompanyCode());
		if (ctime != null) {
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_GKNL, ctime);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.putAttr(StockConstants.INDEX_CODE_GKNL, xv);
		}

//		ctime = IndexService.getInstance().getRealtime(
//				StockConstants.INDEX_CODE_CCZNL, c.getCompanyCode());
		if (ctime != null) {
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_CCZNL, ctime);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.putAttr(StockConstants.INDEX_CODE_CCZNL, xv);
		}

		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_PB, c.getCompanyCode());
		if (ctime != null) {
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_PB, ctime);
			im1.setIndexCode(StockConstants.INDEX_CODE_PB);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.setPB(xv);
		}
		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_PE, c.getCompanyCode());
		if (ctime != null) {
			// pe
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_PE, ctime);
			im1.setIndexCode(StockConstants.INDEX_CODE_PE);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.setPE(xv);
		}
		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_PS, c.getCompanyCode());
		if (ctime != null) {
			// ps
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_PS, ctime);
			im1.setIndexCode(StockConstants.INDEX_CODE_PS);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.setPS(xv);
		}
		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_EV, c.getCompanyCode());
		if (ctime != null) {
			// ev
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_EV, ctime);
			im1.setIndexCode(StockConstants.INDEX_CODE_EV);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.setEV(xv);
		}
		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_ROE, c.getCompanyCode());
		if (ctime != null) {
			// roe
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_ROE, ctime);
			im1.setIndexCode(StockConstants.INDEX_CODE_ROE);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.setRoe(xv);
		}
		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_MV, c.getCompanyCode());
		if (ctime != null) {
			// mv
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_MV, ctime);
			im1.setIndexCode(StockConstants.INDEX_CODE_MV);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.setMv(xv);
		}

		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_ASN, c.getCompanyCode());
		if (ctime != null) {
			// sum stock num
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_ASN, ctime);
			im1.setIndexCode(StockConstants.INDEX_CODE_ASN);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.setAsn(xv);
		}
		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_PSY, c.getCompanyCode());
		if (ctime != null) {
			// 每股收益
			im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_PSY, ctime);
			im1.setIndexCode(StockConstants.INDEX_CODE_PSY);
			xv = IndexValueAgent.getIndexValue(im1);
			if (xv != null && xv != 0)
				c.setPsy(xv);
		}

		ctime = IndexService.getInstance().getRealtime(
				StockConstants.INDEX_CODE_GXSY, c.getCompanyCode());
		if (ctime != null) {
			// 股息收益率
			IndexMessage imgx = SMsgFactory.getUMsg(c.getCompanyCode(),
					StockConstants.INDEX_CODE_GXSY, ctime);
			imgx.setIndexCode(StockConstants.INDEX_CODE_GXSY);
			xv = IndexValueAgent.getIndexValue(imgx);
			if (xv != null && xv != 0)
				c.setGxsy(xv);
		}
	}

	/**
	 * type : 0 按产品 type : 1 按行业
	 * 
	 * @param companycode
	 * @param time
	 * @param type
	 * @return
	 */
	public Map<String, List<SimpleDataItemV2>> getCompanyIncomeComposeByOneTime(
			String companycode, Date time, String type) {
		Map<String, SimpleDataItemV2> m1 = new HashMap<String, SimpleDataItemV2>();
		Map<String, List<SimpleDataItemV2>> m = new HashMap<String, List<SimpleDataItemV2>>();
		if ("0".equals(type)) {
			List<Company0018> cl = Company0018Service.getInstance()
					.getCompanyIncomeComposeListByOneTime(companycode, time);
			if (cl != null && cl.size() > 0) {
				// //业务收入
				// List<DataItem> sdl = new ArrayList<DataItem>();
				// //业务成本
				// List<DataItem> cdl = new ArrayList<DataItem>();
				// //毛利
				// List<DataItem> mdl = new ArrayList<DataItem>();
				//
				// //毛利率
				// List<DataItem> vdl = new ArrayList<DataItem>();
				for (Company0018 c : cl) {

					// 产品名
					String pname = c.getF004v();
					if (pname.equals("合计"))
						continue;
					// 业务收入值
					buildDataV1(pname, c.getF012n(), StockConstants.SHOU_RU,
							time, m1, m);

					// 业务成本值
					buildDataV1(pname, c.getF014n(), StockConstants.CHEN_BEN,
							time, m1, m);

					// 毛利
					buildDataV1(pname, c.getF016n(), StockConstants.MAO_LI,
							time, m1, m);

					// 毛利率
					buildDataV1(pname, c.getF018n(), StockConstants.MAO_LI_LV,
							time, m1, m);

				}

			}
		} else {
			List<Company0019> cl = Company0019Service.getInstance()
					.getCompanyIncomeComposeListByOneTime(companycode, time);
			if (cl != null && cl.size() > 0) {
				// 业务收入
				// List<DataItem> sdl = new ArrayList<DataItem>();
				// //业务成本
				// List<DataItem> cdl = new ArrayList<DataItem>();
				// //毛利
				// List<DataItem> mdl = new ArrayList<DataItem>();
				// //毛利率
				// List<DataItem> vdl = new ArrayList<DataItem>();
				for (Company0019 c : cl) {
					// 分类名
					String pname = c.getF001v();

					// 业务收入值
					buildDataV1(pname, c.getF008n(), StockConstants.SHOU_RU,
							time, m1, m);

					// 业务成本值
					buildDataV1(pname, c.getF010n(), StockConstants.CHEN_BEN,
							time, m1, m);

					// 毛利
					buildDataV1(pname, c.getF012n(), StockConstants.MAO_LI,
							time, m1, m);

					// 毛利率
					buildDataV1(pname, c.getF014n(), StockConstants.MAO_LI_LV,
							time, m1, m);

				}

			}
		}
		ComparetorUtil.sortDataItem(m.get(StockConstants.SHOU_RU));
		ComparetorUtil.sortDataItem(m.get(StockConstants.CHEN_BEN));
		ComparetorUtil.sortDataItem(m.get(StockConstants.MAO_LI));
		ComparetorUtil.sortDataItem(m.get(StockConstants.MAO_LI_LV));
		return m;
	}

	/**
	 * type : 0 按产品 type : 1 按行业
	 * 
	 * @param companycode
	 * @param time
	 * @param type
	 * @return
	 */
	public Map<String, List<SimpleDataItemV2>> getCompanyLastestIncomeComposeByTime(
			Company tc, String type) {
		Map<String, SimpleDataItemV2> m1 = new HashMap<String, SimpleDataItemV2>();
		Map<String, List<SimpleDataItemV2>> m = new HashMap<String, List<SimpleDataItemV2>>();
		if ("0".equals(type)) {
			List<Company0018> cl = Company0018Service.getInstance()
					.getCompanyLastestIncomeComposeListByTime(
							tc.getCompanyCode(), tc.getReportTime());
			if (cl != null && cl.size() > 0) {

				for (Company0018 c : cl) {

					// 产品名
					String pname = c.getF004v();
					Date time = c.getEnddate();
					if (pname.equals("合计"))
						continue;
					// 业务收入值
					buildDataV1(pname, c.getF012n(), StockConstants.SHOU_RU,
							time, m1, m);

					// 业务成本值
					buildDataV1(pname, c.getF014n(), StockConstants.CHEN_BEN,
							time, m1, m);

					// 毛利
					buildDataV1(pname, c.getF016n(), StockConstants.MAO_LI,
							time, m1, m);

					// 毛利率
					buildDataV1(pname, c.getF018n(), StockConstants.MAO_LI_LV,
							time, m1, m);

				}

			}
		} else {
			List<Company0019> cl = Company0019Service.getInstance()
					.getCompanyLastestIncomeComposeListByTime(
							tc.getCompanyCode(), tc.getReportTime());
			if (cl != null && cl.size() > 0) {

				for (Company0019 c : cl) {
					// 分类名
					String pname = c.getF001v();
					Date time = c.getEnddate();
					// 业务收入值
					buildDataV1(pname, c.getF008n(), StockConstants.SHOU_RU,
							time, m1, m);

					// 业务成本值
					buildDataV1(pname, c.getF010n(), StockConstants.CHEN_BEN,
							time, m1, m);

					// 毛利
					buildDataV1(pname, c.getF012n(), StockConstants.MAO_LI,
							time, m1, m);

					// 毛利率
					buildDataV1(pname, c.getF014n(), StockConstants.MAO_LI_LV,
							time, m1, m);

				}

			}
		}
		ComparetorUtil.sortDataItem(m.get(StockConstants.SHOU_RU));
		ComparetorUtil.sortDataItem(m.get(StockConstants.CHEN_BEN));
		ComparetorUtil.sortDataItem(m.get(StockConstants.MAO_LI));
		ComparetorUtil.sortDataItem(m.get(StockConstants.MAO_LI_LV));
		return m;
	}

	/**
	 * type : 0 按产品 type : 1 按行业
	 * 
	 * @param companycode
	 * @param time
	 * @param type
	 * @return
	 */
	public Map<String, List<SimpleDataItemV2>> getCompanyLastestIncomeComposeByOneTime(
			String companycode, String type) {
		Map<String, SimpleDataItemV2> m1 = new HashMap<String, SimpleDataItemV2>();
		Map<String, List<SimpleDataItemV2>> m = new HashMap<String, List<SimpleDataItemV2>>();
		if ("0".equals(type)) {
			List<Company0018> cl = Company0018Service.getInstance()
					.getCompanyLastestIncomeComposeListByOneTime(companycode);
			if (cl != null && cl.size() > 0) {

				for (Company0018 c : cl) {

					// 产品名
					String pname = c.getF004v();
					Date time = c.getEnddate();
					if (pname.equals("合计"))
						continue;
					// 业务收入值
					buildDataV1(pname, c.getF012n(), StockConstants.SHOU_RU,
							time, m1, m);

					// 业务成本值
					buildDataV1(pname, c.getF014n(), StockConstants.CHEN_BEN,
							time, m1, m);

					// 毛利
					buildDataV1(pname, c.getF016n(), StockConstants.MAO_LI,
							time, m1, m);

					// 毛利率
					buildDataV1(pname, c.getF018n(), StockConstants.MAO_LI_LV,
							time, m1, m);

				}

			}
		} else {
			List<Company0019> cl = Company0019Service.getInstance()
					.getCompanyLastestIncomeComposeListByOneTime(companycode);
			if (cl != null && cl.size() > 0) {

				for (Company0019 c : cl) {
					// 分类名
					String pname = c.getF001v();
					Date time = c.getEnddate();
					// 业务收入值
					buildDataV1(pname, c.getF008n(), StockConstants.SHOU_RU,
							time, m1, m);

					// 业务成本值
					buildDataV1(pname, c.getF010n(), StockConstants.CHEN_BEN,
							time, m1, m);

					// 毛利
					buildDataV1(pname, c.getF012n(), StockConstants.MAO_LI,
							time, m1, m);

					// 毛利率
					buildDataV1(pname, c.getF014n(), StockConstants.MAO_LI_LV,
							time, m1, m);

				}

			}
		}
		ComparetorUtil.sortDataItem(m.get(StockConstants.SHOU_RU));
		ComparetorUtil.sortDataItem(m.get(StockConstants.CHEN_BEN));
		ComparetorUtil.sortDataItem(m.get(StockConstants.MAO_LI));
		ComparetorUtil.sortDataItem(m.get(StockConstants.MAO_LI_LV));
		return m;
	}

	private void buildDataV1(String pname, Double v, String type, Date time,
			Map<String, SimpleDataItemV2> m1,
			Map<String, List<SimpleDataItemV2>> m) {
		List<SimpleDataItemV2> dl = m.get(type);
		if (dl == null) {
			dl = new ArrayList<SimpleDataItemV2>();
			m.put(type, dl);
		}
		Double sd = v;
		String sk = getKeyDI(type, pname);
		SimpleDataItemV2 sdi = m1.get(sk);
		if (sdi == null) {
			sdi = new SimpleDataItemV2(pname, sd, time);
			m1.put(sk, sdi);
			dl.add(sdi);
		} else {
			// 把相关名的类合并
			sdi.setV(sd + sdi.getV());
		}

	}

	private String getKeyDI(String prefix, String name) {
		// TODO Auto-generated method stub
		return prefix + "_" + name;
	}

	public Map<String, List<CIndexSeries>> getCompanyIncomeComposeListBySectionTime(
			String companycode, Date stime, Date etime, String type) {
		Map<String, DataItem> m1 = new HashMap<String, DataItem>();
		Map<String, List<DataItem>> mdl = new HashMap<String, List<DataItem>>();
		Map<String, List<CIndexSeries>> mlcis = new HashMap<String, List<CIndexSeries>>();
		if ("0".equals(type)) {
			List<Company0018> cl = Company0018Service.getInstance()
					.getCompanyIncomeComposeListBySectionTime(companycode,
							stime, etime);
			if (cl != null && cl.size() > 0) {
				for (Company0018 c : cl) {

					// 产品名
					String pname = c.getF004v();
					if (pname.equals("合计"))
						continue;
					buildData(StockConstants.SHOU_RU, pname, c.getF012n(),
							c.getEnddate(), mlcis, m1, mdl);

					// 业务成本值
					buildData(StockConstants.CHEN_BEN, pname, c.getF014n(),
							c.getEnddate(), mlcis, m1, mdl);

					// 毛利
					buildData(StockConstants.MAO_LI, pname, c.getF016n(),
							c.getEnddate(), mlcis, m1, mdl);

					// //毛利率
					buildData(StockConstants.MAO_LI_LV, pname, c.getF018n(),
							c.getEnddate(), mlcis, m1, mdl);

				}

			}
		} else {
			List<Company0019> cl = Company0019Service.getInstance()
					.getCompanyIncomeComposeListBySectionTime(companycode,
							stime, etime);
			if (cl != null && cl.size() > 0) {
				//
				for (Company0019 c : cl) {
					// //分类名
					String pname = c.getF001v();
					buildData(StockConstants.SHOU_RU, pname, c.getF008n(),
							c.getEnddate(), mlcis, m1, mdl);

					// //业务成本值
					buildData(StockConstants.CHEN_BEN, pname, c.getF010n(),
							c.getEnddate(), mlcis, m1, mdl);

					// //毛利
					buildData(StockConstants.MAO_LI, pname, c.getF012n(),
							c.getEnddate(), mlcis, m1, mdl);

					// //毛利率
					buildData(StockConstants.MAO_LI_LV, pname, c.getF014n(),
							c.getEnddate(), mlcis, m1, mdl);
					//
				}
			}
		}
		mlcis = buildMLCIS(mlcis, stime, etime);
		return mlcis;
	}

	private Map<String, List<CIndexSeries>> buildMLCIS(
			Map<String, List<CIndexSeries>> mlcis, Date stime, Date etime) {
		Iterator<String> iter = mlcis.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			List<CIndexSeries> cisl = mlcis.get(key);
			for (CIndexSeries cis : cisl) {
				List<DataItem> dil = cis.getData();
				dil = buildDataItemListByTime(dil, stime, etime);
				cis.setData(dil);
			}

		}

		return mlcis;
	}

	private List<DataItem> buildDataItemListByTime(List<DataItem> dil,
			Date stime, Date etime) {
		List<DataItem> ndil = new ArrayList<DataItem>();

		Date sd = stime;
		Date ed = etime;
		// 如果起始时间,小于结束时间
		while (sd.compareTo(ed) <= 0) {
			String d0 = DateUtil.formatDate2YYYYMMFast(sd);
			DataItem ndi = null;
			for (DataItem di : dil) {
				String d1 = DateUtil.getSysDate(DateUtil.YYYYMM,
						new Date(di.getTime()));
				if (d0.equals(d1)) {
					ndi = di;
					dil.remove(di);
					break;
				}
			}
			// 构造一个0值
			if (ndi == null)
				ndi = new DataItem(sd, StockConstants.DEFAULT_DOUBLE_VALUE);

			ndil.add(ndi);
			// 取下一个时间点
			sd = StockUtil.getNextTimeV3(sd, 3, Calendar.MONTH);
		}
		return ndil;
	}

	private void buildData(String type, String pname, Double v, Date endDate,
			Map<String, List<CIndexSeries>> mlcis, Map<String, DataItem> m1,
			Map<String, List<DataItem>> mdl) {
		List<CIndexSeries> psdl = mlcis.get(type);
		if (psdl == null) {
			psdl = new ArrayList<CIndexSeries>();
			mlcis.put(type, psdl);
		}
		List<DataItem> dil = mdl.get(getKeyDI(type, pname));
		if (dil == null) {
			dil = new ArrayList<DataItem>();
			mdl.put(pname, dil);
			CIndexSeries cis = new CIndexSeries();
			cis.setName(pname);
			cis.setData(dil);
			psdl.add(cis);
		}
		Date time = endDate;
		// 业务收入值
		Double sd = v;
		String sk = getKeyDIT(type, pname, time);
		DataItem sdi = m1.get(sk);
		if (sdi == null) {
			sdi = new DataItem(pname, sd, time);
			m1.put(sk, sdi);
			dil.add(sdi);
		} else {
			// 把相关名的类合并
			sdi.setValue(sd + sdi.getValue());
		}

	}

	private String getKeyDIT(String prefix, String name, Date time) {
		// TODO Auto-generated method stub
		return prefix + "_" + name + "_" + time.getTime();
	}

	/**
	 * 通过机构id来查同一公司不同地点的上市信息
	 * 
	 * @param orgid
	 * @return
	 */
	public List<Company> getCompanyListByOrgid(String orgid) {
		return LCEnter.getInstance().get(orgid,
				CacheUtil.getCacheName(StockConstants.COMPANY));
	}

	/*
	 * 此处只是针对证券会的行业 取子行业,如果子行业中，同行公司少于5个，则取上级行业
	 * 
	 * 修改：公司的主行业改为申万三级行业（或是公司的主tag)
	 */
	public String getIndustryNameOfCompany(String companyCode) {
		Company c = getCompanyByCode(companyCode);
		// if(!c.getMainTag().replace("mtag_", "").equals(c.getF048v()))
		// {
		// System.out.println("==================mtag not equal sw 3 tag...........mtag:"+c.getMainTag()+",tag:"+c.getF048v());
		// }
		return c.getMainTag().replace("mtag_", "i_").toLowerCase();
		// if(StringUtil.isEmpty(companyCode)) return null;
		// String ret = null;
		// Company c =
		// CompanyService.getInstance().getCompanyByCode(companyCode);
		// Industry ind = IndustryService.getInstance().getIndustryByCompany(c);
		// if(ind==null) return ret;
		// List<Company> cl = getCompanyListByTagFromCache(ind.getName());
		// if(cl==null||cl.size()<StockConstants.INDUSTRY_COMPANY_LOW)
		// {
		// Industry pind =
		// IndustryService.getInstance().getIndustryCSRCByCode(ind.getParentCode());
		// if(pind!=null)
		// ret = pind.getName();
		// }
		// else
		// ret = ind.getName();
		// return ret;
	}

	public Company getCompanyBySimpleNameFromCache(String simpilename) {

		return LCEnter.getInstance().get(
				StockUtil.getCompanyNameKey(simpilename),
				StockUtil.getCacheName(StockConstants.DATA_TYPE_company));
	}

	public List getPeerCompanyListByCompanycode(String companycode) {
		Company c = CompanyService.getInstance().getCompanyByCode(companycode);
		if (c == null)
			return null;
		String industryName = CompanyService.getInstance()
				.getIndustryNameOfCompany(companycode);
		return getCompanyListByTagFromCache(industryName);
	}

	public Date getLatestReportTime(String companyCode) {
		return getCompanyByCodeFromCache(companyCode).getReportTime();
	}

	/**
	 * 是否为未上市公司
	 * 
	 * @param companyCode
	 * @return
	 */
	public Boolean notSH(String companyCode) {
		return Stock0001Service.getInstance().getStock0001ByCompanycode(
				companyCode) == null;
	}

	public List<String> getYfxzIndustryOfCompany(String companycode) {
		companycode = companycode.trim();
		Company c = getCompanyByCodeFromCache(companycode);
		String mtags = c.getMainTag();
		List<String> ls = new ArrayList<String>();
		if (!StringUtil.isEmpty(mtags)) {
			String ttags = mtags.replace("mtag_", "i_");
			TreeNode tn = IndustryService.getInstance()
					.getIndustryYFZXTreeFromCache(1).get(ttags);
			if (tn != null) {
				TreeNode onetag = tn.getParent().getParent();
				TreeNode twotag = tn.getParent();
				ls.add(((Industry) onetag.getReference()).getName());// 一级
				ls.add(((Industry) twotag.getReference()).getName());// 二级
				ls.add(ttags);// 三级
			}
		}
		return ls;
	}

	public List<String> getCompanyTags(String companycode) {
		Company c = getCompanyByCode(companycode);
		String ctags = c.getTags();
		List<String> ls = new ArrayList<String>();
		if (!StringUtil.isEmpty(ctags)) {

			String[] ta = c.getTags().split(";");
			// 如果不在缓存中存在的tag不加入
			for (String t : ta) {
				Object o = getCompanyListByTagFromCache(t);
				if (o != null) {
					ls.add(t);
				}
			}

		}
		return ls;
	}

	/**
	 * 如果是B股上市的公司，看其是否在A股上市，如果是则以A股为准，对B股的各方面数据不计入
	 * 
	 * @param c
	 * @return
	 */
	public boolean removeB(Company c) {
		if (c == null)
			return true;
		Stock0001 s = Stock0001Service.getInstance()
				.getStock0001ByCompanycodeFromCache(c.getCompanyCode());
		if (s == null)
			return true;
		return "001002".equals(s.getF002v());
	}

	/**
	 * 计算行业时，需要去掉的公司 B股(在A股已上市)，未上市的公司，预披露公司
	 * 
	 * @param c
	 * @return
	 */
	public boolean needRemoveWhenComputeIndustry(Company c) {
		if (c == null)
			return true;
		// 013001：正常上市
		if (c.getF031v().equals("013001")) {
			Stock0001 s = Stock0001Service.getInstance()
					.getStock0001ByCompanycodeFromCache(c.getCompanyCode());
			if (s == null)
				return true;
			// 是B股且在A股也上市
			if ("001002".equals(s.getF002v()) && isABStock(c))
				return true;
			else
				return false;
		}
		return true;
	}

	private boolean isABStock(Company c) {

		return _abMap.get(c.getCompanyCode()) != null;
	}

	/**
	 * 是B股，但也在A股上市
	 * 
	 * @param c
	 * @return
	 */
	public boolean needRemoveB(Company c) {

		if (_abMap.get(c.getCompanyCode()) != null) {
			Stock0001 s = Stock0001Service.getInstance()
					.getStock0001ByCompanycodeFromCache(c.getCompanyCode());
			if (s == null)
				return true;
			return "001002".equals(s.getF002v());
		}
		return false;
	}

	/*
	 * -1:非B股 0:上交所B股 1:深交所B股
	 */
	public int isBStock(String companycode) {
		if (companycode == null)
			return -1;
		Stock0001 s = Stock0001Service.getInstance()
				.getStock0001ByCompanycodeFromCache(companycode);
		if (s == null)
			return -1;
		if ("001002".equals(s.getF002v())) {
			if (companycode.endsWith(".sh"))
				return 1;
			else
				return 0;
		}
		return -1;
	}

	// 去重，同一公司在不同的地点上市，会有多个companycode,些处去掉B股，本应按证券信息表中的上市地点来区分，为了简便用名字中是否含有B来区分
	public boolean removeBST(Company c) {
		Stock0001 s = Stock0001Service.getInstance()
				.getStock0001ByCompanycodeFromCache(c.getCompanyCode());
		if (s == null)
			return true;
		if ("001002".equals(s.getF002v()))
			return true;
		// TODO Auto-generated method stub
		return c.getSimpileName().contains("Ｂ")
				|| c.getSimpileName().contains("*")
				|| c.getSimpileName().contains("S")
				|| c.getSimpileName().contains("s")
				|| c.getCompanyCode().startsWith("T")
				|| c.getCompanyCode().contains("B");
	}

	public boolean isSTStock(String companycode)
	{
		USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(companycode);
				if(us!=null)
				{
					return  us.getName().contains("*")
							|| us.getName().contains("S")
							|| us.getName().contains("s")
							|| us.getUidentify().startsWith("T");
				}
		return true;
	}

	public List<Company> getCompanyListByTsc(String tsc) {
		return getCompanyListByTagFromCache(tsc);
	}

	/**
	 * 更新sw三级行业
	 * 
	 * @param f048v
	 */
	public void updateSw3rdTag(String companycode, String tag) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companycode);
		m.put("tag", tag);
		RequestMessage req = DAFFactory.buildRequest("updateSw3rdTag", m,
				StockConstants.COMPANY_TYPE);
		pLayerEnter.modify(req);
	}

	public void updateSw2rdTag(String companycode, String tag) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companycode);
		m.put("tag", tag);
		RequestMessage req = DAFFactory.buildRequest("updateSw2rdTag", m,
				StockConstants.COMPANY_TYPE);
		pLayerEnter.modify(req);
	}

	public void updateSw1rdTag(String companycode, String tag) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("companycode", companycode);
		m.put("tag", tag);
		RequestMessage req = DAFFactory.buildRequest("updateSw1rdTag", m,
				StockConstants.COMPANY_TYPE);
		pLayerEnter.modify(req);
	}

	public List<Company> getDataUpCompanyList(Date uptime) {
		List<Company> cl = new ArrayList<Company>();
		// 取所有报表体系相关的指标
		LCEnter lcEnter = LCEnter.getInstance();
		List<String> tsl = lcEnter.get(
				StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
				StockConstants.MATCH_INFO_CACHE);
		if (tsl == null) {
			return null;
		}
		for (String tsc : tsl) {
			// 扫描数据库,取出所有数据更新的公司
			List<String> cList = CompanyService.getInstance()
					.getConpanyListOfDateUpdate(tsc, uptime);
			if (cList == null || cList.size() == 0) {
				continue;
			}
			for (String ccode : cList) {

				try {
					Company c = CompanyService.getInstance().getCompanyByCode(
							ccode);
					if (c != null) {
						cl.add(c);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (cl != null) {
			for (Company c : cl) {
				IndexService.getInstance().rebuildMaxMinTimeCache(
						c.getCompanyCode());

			}
		}
		return cl;
	}

	// check
	// private boolean isCompute(Company c) {
	// Boolean isc = c.getAttr("computed");
	// if(isc!=null&&isc)
	// return true;
	// else
	// {
	// String ctime = c.getReportTime();
	// // roe
	// IndexMessage im1 = SMsgFactory.getCompanyMsg(
	// c.getCompanyCode(), StockConstants.INDEX_CODE_ROE,
	// ctime);
	// im1.setNeedAccessExtIndexDb(true);
	// im1.setNeedAccessCompanyBaseIndexDb(false);
	// Double xv = IndexValueAgent.getIndexValue(im1);
	// xv = IndexValueAgent.getIndexValue(im1);
	// if (xv != null && xv != 0)
	// return true;
	// }
	// return false;
	// }

	public void initAllCompanyTagsSet() {
		List<Company> cl = getCompanyListFromCache();
		for (Company c : cl) {
			Set<String> _jhtagSet = new HashSet<String>();
			Set<String> _tagSet = new HashSet<String>();
			String tags = c.getTags();
			if (!StringUtil.isEmpty(tags)) {
				for (String tag : tags.split(";")) {
					_tagSet.add(tag);
					if (TagService.getInstance().isJHTag(tag))
						_jhtagSet.add(tag);

				}
			}
			c.setTagSet(_tagSet);
			c.setJhtagSet(_jhtagSet);
		}
	}

	/**
	 * scl :指定结果集
	 * 
	 * @param tids
	 * @param scl
	 * @return
	 */
	// public List<Company> computeCompanyListJJ(String tids, List<Company> scl)
	// {
	// List<Company> jjcl = null;
	// try {
	// if (tids.split("\\^").length >= 1) {
	// List<Map<String, List<Company>>> ks = new ArrayList<Map<String,
	// List<Company>>>();
	// for (String tid : tids.split("\\^")) {
	// List<Company> cl = CompanyService.getInstance()
	// .getCompanyListByTagFromCache(tid.trim());
	// if (cl == null || cl.size() == 0)
	// continue;
	// Map<String, List<Company>> sortMap = new HashMap<String,
	// List<Company>>();
	// sortMap.put(tid, cl);
	// ks.add(sortMap);
	// }
	// if (scl != null) {
	// Map<String, List<Company>> sortMap = new HashMap<String,
	// List<Company>>();
	// sortMap.put("set_company_range", scl);
	// ks.add(sortMap);
	// }
	// // 排序，从小到大
	// Collections.sort(ks,
	// new Comparator<Map<String, List<Company>>>() {
	//
	// public int compare(Map<String, List<Company>> o1,
	// Map<String, List<Company>> o2) {
	// String tid1 = (String) o1.keySet().toArray()[0];
	// String tid2 = (String) o2.keySet().toArray()[0];
	// return o1.get(tid1).size()
	// - o2.get(tid2).size();
	// }
	// });
	// // 求交集
	// if (ks.size() != 0) {
	// String k = (String) ks.get(0).keySet().toArray()[0];
	// List<Company> cl = ks.get(0).get(k);
	// // 取公司数据量最小的列表
	// Map<String, Company> jjl = convertCL2Map(cl);
	// for (int j = 1; j < ks.size(); j++) {
	// Iterator<String> iter = jjl.keySet().iterator();
	// while (iter.hasNext()) {
	// String ccode = iter.next();
	// k = (String) ks.get(j).keySet().toArray()[0];
	// cl = ks.get(j).get(k);
	// Map<String, Company> tjjl = convertCL2Map(cl);
	// Company tc = tjjl.get(ccode);
	// if (tc == null)
	// jjl.remove(ccode);
	//
	// }
	// }
	// jjcl = convertMap2Cl(jjl);
	// }
	//
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// return jjcl;
	// }

	/**
	 * scl :指定结果集
	 * 
	 * @param tids
	 *            :如+sdgd^-gdbh
	 * @param scl
	 * @return
	 */
	public List<Company> computeCompanyListJJV2(String tids, List<Company> scl) {
		List<Company> jjcl = null;
		List<String> atidl = new ArrayList<String>();
		List<String> dtidl = new ArrayList<String>();
		List<Company> minl = new ArrayList<Company>(0);// 公司数最小的集合
		try {
			if (StringUtil.isEmpty(tids) && scl == null)
				return null;
			String mintid = "";
			if (tids.split("\\^").length >= 1) {
				for (String tid : tids.split("\\^")) {
					// 求交集的集合
					if (!tid.startsWith("+") && !tid.startsWith("-")) {
						atidl.add(tid);
					}
					// 求交集的集合
					if (tid.startsWith("+")) {
						tid = tid.substring(1, tid.length());
						atidl.add(tid);
					}
					// 要剔除的集合
					if (tid.startsWith("-")) {
						tid = tid.substring(1, tid.length());
						dtidl.add(tid);
						continue;
					}
					List<Company> cl = CompanyService.getInstance()
							.getCompanyListByTagFromCache(tid.trim());
					if (cl == null || cl.size() == 0)
						continue;
					if (minl.size() == 0) {
						minl = cl;
						mintid = tid;
					}
					// 保留求交集中公司数据量小的集合
					if (cl.size() < minl.size()) {
						minl = cl;
						mintid = tid;
					}

				}

				if (scl != null) {
					minl = scl;
					mintid = "";

				}
				if (!StringUtil.isEmpty(mintid))
					atidl.remove(mintid);
				if (minl.size() > 0) {
					if (dtidl.size() == 0 && atidl.size() == 0)
						jjcl = minl;
					else {
						Map<String, Company> jjl = convertCL2Map(minl);
						Iterator<String> iter = jjl.keySet().iterator();
						while (iter.hasNext()) {
							String ccode = iter.next();
							Company c = jjl.get(ccode);
							Set<String> actset = CompanyService.getInstance()
									.getAllTagSets(c);
							// 剔除不在交集中的公司
							if (atidl.size() > 0) {
								for (String atid : atidl) {
									if (!StringUtil.isEmpty(atid)
											&& !actset.contains(atid))
										jjl.remove(ccode);
								}
							}
							// 剔除在要删除集合中的公司
							if (dtidl.size() > 0) {
								for (String dtid : dtidl) {
									if (!StringUtil.isEmpty(dtid)
											&& actset.contains(dtid))
										jjl.remove(ccode);
								}
							}
						}
						jjcl = convertMap2Cl(jjl);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jjcl;
	}

	private List<Company> convertMap2Cl(Map<String, Company> jjl) {
		List<Company> cl = new ArrayList<Company>();
		for (Company c : jjl.values()) {
			if (c != null)
				cl.add(c);
		}
		return cl;
	}

	private Map<String, Company> convertCL2Map(List<Company> cl) {

		Map<String, Company> rm = new ConcurrentHashMap<String, Company>();
		try {
			for (Company c : cl) {
				if (c == null)
					continue;
				rm.put(c.getCompanyCode(), c);
			}
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		return rm;
	}

	public void removeAllCompanysOneTag(String removetag) {
		List<Company> cl = getCompanyListFromCache();
		for (Company c : cl) {
			if (c == null)
				continue;
			c.removeTagFromCompanyCache(removetag);
		}

	}

	public void flushAllCompanysOneTag(List<Company> cl) {
		for (Company c : cl) {
			c.flushTag();
		}

	}

	Object uplock = new Object();

	public void appendTag2Clist(String key, List<Company> cl) {
		if (cl != null) {
			synchronized (uplock) {
				// 清除老标签
				CompanyService.getInstance().removeAllCompanysOneTag(key);

				for (Company c : cl) {
					c.appendTag2CompanyCache(key);
				}
				// 更新所有公司的标签
				CompanyService.getInstance().flushAllCompanysOneTag(cl);
			}
		}

	}

	public String getMzpx(Company c) {
		String mzpx = c.getMzpx();
		if (StringUtil.isEmpty(mzpx)) {
			mzpx = doGetMzpx(c);
			if (!StringUtil.isEmpty(mzpx))
				c.putMzpx(mzpx);
		}
		return mzpx;
	}

	private String doGetMzpx(Company c) {
		StringBuilder sb = new StringBuilder();
		String[] args = { "新股发行", "增发", "配股", "现金分红" };
		String[] barType = { "募资", "派现" };
		SSeries ss = new SSeries();
		ss.setName("发增配派");

		MarketService ms = MarketService.getInstance();
		Double firstValue = ms.getFirstFinancing(c.getCompanyCode());// 新股发行
		sb.append(barType[0] + ":" + 0);
		sb.append("^");
		sb.append(args[0] + ":" + firstValue);
		sb.append("~");

		Double zfValue = ms.getFinancing(c.getCompanyCode(), args[1]);// 增发
		sb.append(barType[0] + ":" + 0);
		sb.append("^");
		sb.append(args[1] + ":" + zfValue);
		sb.append("~");

		Double pgValue = ms.getFinancing(c.getCompanyCode(), args[2]);// 配股
		sb.append(barType[0] + ":" + 0);
		sb.append("^");
		sb.append(args[2] + ":" + pgValue);
		sb.append("~");

		Double fhValue = ms.getDividendFromCache(c.getCompanyCode(), null);// 现金分红
		sb.append(barType[0] + ":" + 1);
		sb.append("^");
		if (fhValue != null)
			sb.append(args[3] + ":" + (fhValue / 10000));
		sb.append("~");
		return sb.toString();
	}

	public Set<String> getJHTagSets(Company c) {
		Set<String> actset = new HashSet<String>();
		// 对公司的标签进行分类统计
		Set<String> ctset = c.getJHTagSet();
		Set<String> rrset = TagService.getRealTimeJhcTagSet(c.getCompanyCode());
		if (rrset != null && rrset.size() != 0)
			actset.addAll(rrset);
		if (ctset != null && ctset.size() != 0)
			actset.addAll(ctset);
		return actset;
	}

	public Set<String> getAllTagSets(Company c) {
		Set<String> actset = new HashSet<String>();
		// 对公司的标签进行分类统计
		Set<String> ctset = c.getTagSet();
		Set<String> rrset = TagService.getRealTimeJhcTagSet(c.getCompanyCode());
		if (rrset != null && rrset.size() != 0)
			actset.addAll(rrset);
		if (ctset != null && ctset.size() != 0)
			actset.addAll(ctset);
		return actset;
	}

	@SuppressWarnings("deprecation")
	public boolean isNewStock(Company c) {
		if ("013006".equals(c.getF031v()))
			return true;
		Stock0001 s = Stock0001Service.getInstance().getStock0001ByCompanycode(
				c.getCompanyCode());
		if (s == null)
			return true;
		Calendar cp = Calendar.getInstance();
		cp.setTime(s.getF006d());
		// 近一年内上市的
		if (cp.get(Calendar.YEAR) + 1 >= Calendar.getInstance().get(
				Calendar.YEAR))
			return true;
		return false;
	}

	public boolean isNewStockV2(Company c) {
		if (c.getF031v() == null || c.getF031v().equals("013006"))
			return true;
		else
			return false;
	}

	public boolean isDataUpdate(Date uptime) {
		List<Company> cl = CompanyService.getInstance().getDataUpCompanyList(
				uptime);
		if (cl != null && cl.size() > 0) {
			System.out.println("["
					+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, new Date())
					+ "]data update :" + cl);
			return true;
		}

		return false;
	}

	/**
	 * 清掉最近一天有数据更新的公司的报告期
	 */
	public void rebuildMaxMinTimeCache() {
		Date curtime = Calendar.getInstance().getTime();
		Date uptime = StockUtil.getNextTimeV3(curtime, -2, Calendar.DATE);

		List<Company> cl = CompanyService.getInstance().getDataUpCompanyList(
				uptime);
		if (cl != null) {
			for (Company c : cl) {
				IndexService.getInstance().rebuildMaxMinTimeCache(
						c.getCompanyCode());
			}
		}
	}

	public Date getCompanyPulishTime(String uidentify) {
		// return Stock0001Service.getInstance().getPulishTime(uidentify);
		Long time = TradeBitMapService.getInstance().getPublishTime(uidentify);
		if (time != null)
			return new Date(time);
		return null;
	}

	public int getPublishDaysCount(String uidentify) {
		// return Stock0001Service.getInstance().getPulishTime(uidentify);
		return TradeBitMapService.getInstance().getPublishDaysCount(uidentify);
	}

	public boolean isAStock(String companyCode) {
		// TODO Auto-generated method stub
		return companyCode != null
				&& (companyCode.endsWith(".sz") || companyCode.endsWith(".sh"));
	}

	public boolean isHStock(String companyCode) {
		// TODO Auto-generated method stub
		return companyCode != null && companyCode.endsWith(".hk");
	}

	private static void buildHTradeInfo(Company c) {
		try {
			String key = getStockTradeKey(c.getCompanyCode());
			String[] ca = c.getCompanyCode().split("\\.");
			Long time = Calendar.getInstance().getTimeInMillis();
			// http://hq.sinajs.cn/?_=1395367865527&list=sh600864,sh600519
			String url = "http://hq.sinajs.cn/?_=" + time + "&list=" + ca[1]
					+ ca[0];
			String s = Spider.urlSpider(url, "gbk");
			if (!StringUtil.isEmpty(s) && s.split(",").length > 2) {
				String[] sa = s.split(",");
				StockTrade st = LCEnter.getInstance().get(key,
						SCache.CACHE_NAME_marketcache);
				if (st == null) {
					st = new StockTrade(Double.valueOf(sa[5]),
							Double.valueOf(sa[4]), Double.valueOf(sa[6]),
							Double.valueOf(sa[3]), Double.valueOf(sa[2]),
							Double.valueOf(sa[12]), Double.valueOf(sa[11]));
					st = CompanyService.getInstance().buildHSt(sa, st);
					LCEnter.getInstance().put(key, st,
							SCache.CACHE_NAME_marketcache);

					CompanyService.getInstance().initCompanyIndexData(c);
				}
				if (st != null) {
					st = CompanyService.getInstance().buildHSt(sa, st);
					// initCompanyIndexData(c);
					String end = sa[sa.length - 1];
					if (end.endsWith("\"")) {
						end = end.replace("\"", "");
						if (end.split(":").length == 2) {
							end = end + ":00";
						}
					}
					String timeString = sa[sa.length - 2].replace("/", "-")
							+ " " + end.substring(0, end.length() - 3);
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm");
					Date update = null;
					;
					try {
						update = sdf.parse(timeString);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (update != null) {
						long uptime = update.getTime();
						st.setUptime(uptime);
						c.putAttr("_realtime", uptime);
					}

					CompanyService.getInstance().reinitCompanyTradeInfo(c, st);
				}
			}
		} catch (Exception e) {
			logger.error(
					"---------------buildHTradeInfo faild!-------------------",
					e);
		}

	}

	public void buildATradeInfo(Company c) {
		try {
			String key = getStockTradeKey(c.getCompanyCode());
			String[] ca = c.getCompanyCode().split("\\.");
			Long time = Calendar.getInstance().getTimeInMillis();
			// http://hq.sinajs.cn/?_=1395367865527&list=sh600864,sh600519
			String url = "http://hq.sinajs.cn/?_=" + time + "&list=" + ca[1]
					+ ca[0];
			String s = Spider.urlSpider(url, "gbk");
			if (!StringUtil.isEmpty(s) && s.split(",").length > 2) {
				String[] sa = s.split(",");
				StockTrade st = LCEnter.getInstance().get(key,
						SCache.CACHE_NAME_marketcache);
				if (st == null) {
					st = new StockTrade(Double.valueOf(sa[5]),
							Double.valueOf(sa[4]), Double.valueOf(sa[3]),
							Double.valueOf(sa[2]), Double.valueOf(sa[1]),
							Double.valueOf(sa[8]), Double.valueOf(sa[9]));
					st = CompanyService.getInstance().buildASt(sa, st);
					LCEnter.getInstance().put(key, st,
							SCache.CACHE_NAME_marketcache);

					CompanyService.getInstance().initCompanyIndexData(c);
				}
				if (st != null) {
					st = CompanyService.getInstance().buildASt(sa, st);

					String timeString = sa[sa.length - 3] + " "
							+ sa[sa.length - 2];
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
					Date update = null;
					;
					try {
						update = sdf.parse(timeString);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (update != null) {
						long uptime = update.getTime();
						st.setUptime(uptime);
						c.putAttr("_realtime", uptime);
					}

					// initCompanyIndexData(c);
					CompanyService.getInstance().reinitCompanyTradeInfo(c, st);
				}

			}
		} catch (Exception e) {
			logger.error("---------------buildATradeInfo faild!-------------------"
					+ e);
		}

	}

	private static String getStockTradeKey(String companyCode) {
		// TODO Auto-generated method stub
		return "st." + companyCode;
	}

	// private static StockTrade buildSt(String[] sa, StockTrade st) {
	// Double d = Double.valueOf(sa[5]);
	// if (d != null && d != 0)
	// st.setL(d);
	// d = Double.valueOf(sa[4]);
	// if (d != null && d != 0)
	// st.setH(d);
	// d = Double.valueOf(sa[3]);
	// if (d != null && d != 0)
	// st.setC(d);
	// d = Double.valueOf(sa[2]);
	// if (d != null && d != 0)
	// st.setZs(d);
	// d = Double.valueOf(sa[1]);
	// if (d != null && d != 0)
	// st.setJk(d);
	// d = Double.valueOf(sa[8]);
	// if (d != null && d != 0)
	// st.setCjl(d);
	// d = Double.valueOf(sa[9]);
	// if (d != null && d != 0)
	// st.setCje(d);
	// return st;
	// }
	public StockTrade buildASt(String[] sa, StockTrade st) {
		Double d = Double.valueOf(sa[5]);
		if (d != null && d != 0)
			st.setL(d);
		d = Double.valueOf(sa[4]);
		if (d != null && d != 0)
			st.setH(d);
		d = Double.valueOf(sa[3]);
		if (d != null && d != 0)
			st.setC(d);
		d = Double.valueOf(sa[2]);
		if (d != null && d != 0)
			st.setZs(d);
		d = Double.valueOf(sa[1]);
		if (d != null && d != 0)
			st.setJk(d);
		d = Double.valueOf(sa[8]);
		if (d != null && d != 0)
			st.setCjl(d);
		d = Double.valueOf(sa[9]);
		if (d != null && d != 0)
			st.setCje(d);
		return st;
	}

	public StockTrade buildHSt(String[] sa, StockTrade st) {
		Double d = Double.valueOf(sa[5]);
		if (d != null && d != 0)
			st.setL(d);
		d = Double.valueOf(sa[4]);
		if (d != null && d != 0)
			st.setH(d);
		d = Double.valueOf(sa[6]);
		if (d != null && d != 0)
			st.setC(d);
		d = Double.valueOf(sa[3]);
		if (d != null && d != 0)
			st.setZs(d);
		d = Double.valueOf(sa[2]);
		if (d != null && d != 0)
			st.setJk(d);
		d = Double.valueOf(sa[12]);
		if (d != null && d != 0)
			st.setCjl(d);
		d = Double.valueOf(sa[11]);
		if (d != null && d != 0)
			st.setCje(d);
		return st;
	}

	/**
	 * 数据中心用
	 * 
	 * @param c
	 * @param st
	 */
	public void reinitCompanyTradeInfo(Company c, StockTrade st) {
		// 把前一刻的股价保留下来
		Double pv = c.getC();
		if (pv != null && pv != 0)
			pv = st.getZs();
		if (pv != null)
			c.putAttr("_pre_price", pv);
		c.setL(st.getL());
		c.setH(st.getH());
		c.setC(st.getC());
		c.setZs(st.getZs());
		c.setJk(st.getJk());
		c.setCjl(st.getCjl());
		c.setCje(st.getCje());
		Long priceUptime = st.getUptime();
		if (priceUptime != null)
			c.putAttr("_priceUptime", priceUptime);
	}

	/**
	 * 前端用
	 * 
	 * @param c
	 * @param st
	 */
	public void updateCompanyStockPrice(Company c, StockTrade st) {
		// 把前一刻的股价保留下来
		Double pv = c.getC();
		if (pv != null && pv != 0)
			pv = st.getZs();
		if (pv != null)
			c.putAttr("_pre_price", pv);
		c.setL(st.getL());
		c.setH(st.getH());
		c.setC(st.getC());
		c.setZs(st.getZs());
		c.setJk(st.getJk());
		c.setCjl(st.getCjl());
		c.setCje(st.getCje());
		Long priceUptime = System.currentTimeMillis();
		if (priceUptime != null)
			c.putAttr("_priceUptime", priceUptime);
		Long priceLasttime = st.getUptime();
		if (priceLasttime != null)
			c.putAttr("_priceLasttime", priceUptime);
	}

	public void buildTradeInfo(Company c) {
		if (CompanyService.getInstance().isAStock(c.getCompanyCode()))
			buildATradeInfo(c);
		if (CompanyService.getInstance().isHStock(c.getCompanyCode()))
			buildHTradeInfo(c);
	}

	/**
	 * 是否停牌
	 * 
	 * @param c
	 * @return
	 */
	public boolean isStop(Company c) {
		if (c != null) {
			if (c.getJk() == 0) { // 今开等于0 则表示停牌
				return true;
			}
			String stockcode = c.getCompanyCode();
			Long lastPriceUpdateTime = c.getAttr("_realtime") == null ? 0
					: (Long) c.getAttr("_realtime");
			long latestStockTradeTime = getStockLatestTradeTime(stockcode);
			if (latestStockTradeTime == 0l) {
				TradeCenter.getInstance().judgmentATradeClosed();
				latestStockTradeTime = getStockLatestTradeTime(stockcode);
			}
			if (lastPriceUpdateTime == 0) {// 获取不到最后一次股价更新时间
				// lastpointformat:
				// 时间，zs:昨日收盘价，k:开盘价，s:收盘价,zg:最高价，zd:最低价，cjl:成交量，cje:成交额，zdf:涨跌幅(周转率?4726)，sd:升跌，hsl:换手率
				// 5 10 20 30均线
				String lastPoint = TradeService.getInstance()
						.getStockLastPoint(stockcode);
				boolean r1 = StringUtils.isBlank(lastPoint);
				logger.info("fetchLastStockPriceUpdateTimeFromStockLastPoint: "
						+ stockcode);
				if (!r1) {
					String[] arr = lastPoint.split("\\^");
					if (arr.length >= 1) {
						lastPriceUpdateTime = Long.valueOf(arr[0]);
						c.putAttr("_realtime", lastPriceUpdateTime);
					}
				} else {
					logger.error("fetchLastStockTradeLastPoint: " + stockcode);
				}
				if (lastPriceUpdateTime == 0) {
					logger.error("fetchLastStockPriceUpdateTimeFromStockLastPointFailure: "
							+ stockcode);
				}
			}

			if (lastPriceUpdateTime != 0 && latestStockTradeTime != 0
					&& lastPriceUpdateTime < latestStockTradeTime) {// 昨日开盘、今日收盘
				return true;
			}

			boolean isStockTradeNull = false;
			if (lastPriceUpdateTime != 0 && latestStockTradeTime != 0
					&& lastPriceUpdateTime > latestStockTradeTime) {
				StockTrade stockTrade = StockTradeService.getInstance()
						.getStockTradeFromCache(stockcode);
				if (stockTrade == null) {
					isStockTradeNull = true;
					logger.error("isStopGetStockTradeFailure: " + stockcode);
				} else {// 上午开盘下午停牌
					if (stockTrade.getC() == 0 && stockTrade.getCjl() == 0) {
						return true;
					}

					if (stockTrade.getC() != 0 && stockTrade.getCjl() == 0
							|| stockTrade.getC() == 0
							&& stockTrade.getCjl() != 0) {
						logger.error("judgeStockStopStateBySina1 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
								+ stockcode);
						buildTradeInfo(c);
						if (c.getC() != null && c.getC().doubleValue() == 0
								&& c.getCjl() != null && c.getCjl() == 0) {
							return true;
						}
					}
				}
			}

			if (lastPriceUpdateTime == 0 || isStockTradeNull == true) {// fetch
																		// from
																		// sina
				logger.error("judgeStockStopStateBySina2 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
						+ stockcode);
				buildTradeInfo(c);
				if (c.getC() != null && c.getC().doubleValue() == 0
						&& c.getCjl() != null && c.getCjl() == 0) {
					return true;
				}
			}

		}
		return false;
	}

	private long getStockLatestTradeTime(String stockcode) {
		int type = StockUtil.checkStockcode(stockcode);
		SimpleDateFormat sdf = new SimpleDateFormat(DateUtil.YYYYMMDD);
		try {
			if (type == 0) {
				String d = TradeCenter.getInstance().getALastDate();
				if (StringUtils.isNotBlank(d)) {
					return sdf.parse(d).getTime();
				} else {
					logger.error("getALastDateIsBlank !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				}
			} else if (type == 1) {
				String d = TradeCenter.getInstance().getHKLastDate();
				if (StringUtils.isBlank(d)) {
					logger.error("getHKLastDate !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				} else {
					return sdf.parse(d).getTime();
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public void notifyUserCfUpdate() {

		Date cuptime = StockUtil.getNextTimeV3(new Date(), -1, Calendar.DATE);
		cuptime = DateUtil.getDayStartTime(cuptime);
		List<Company> cl = CompanyService.getInstance().getDataUpCompanyList(
				cuptime);
		for (Company c : cl) {
			String k = c.getCompanyCode() + "_" + c.getReportTime();
			if (BFUtil.checkAndAdd(BFConst.newCFNotifyBF, k))
				continue;
			StringBuilder sb = new StringBuilder();
			String tags = c.getTags();
			int i = 0;
			if (!StringUtil.isEmpty(tags)) {
				for (String tag : tags.split(";")) {
					String tinfo = TagService.getInstance().getMkCfTag(tag);
					if (tinfo == null)
						continue;
					else {
						if (tag.startsWith(StockConstants.CF_TAG_PREFIX)) {
							String tid = tag.split("_")[1];
							Tagrule tr = TagruleService.getInstance()
									.getTagruleByIdFromCache(tid);
							if (tr == null)
								continue;
							sb.append(tr.getTagDesc());
							sb.append(";");
							i++;
							if (i > 2) {
								break;
							}

						}
					}
				}
			}
			// 内容太少，不处理
			if (sb.length() < 1)
				continue;
			String tagBody = sb.substring(0, sb.length() - 1) + "...";
			String name = c.getSimpileName()
					+ DateUtil.getJDString(c.getReportTime()) + "财报更新：";
			String link = "<br/><a onclick=\"setCookie('companysLeftLi','cmLi1')\" href=\"/company_main.html?companycode="
					+ c.getCompanyCode()
					+ "&amp;p=0&amp;companyname="
					+ StockUtil.escape(c.getSimpileName())
					+ "\" target=\"_blank\">" + tagBody + "</a>";
			String msgBody = name + link;
			logger.info("notify cf update:" + msgBody);
			
			Map<String,Serializable> headerMap = new HashMap<String,Serializable>();
			//headerMap.put("h", "财报更新："+DateUtil.getJDString(c.getReportTime())+"#"+c.getCompanyCode()+":"+c.getSimpileName()+"#");
			headerMap.put("l", "usubject"+c.getCompanyCode()+"^1");//内部链接参数code^p，其中p=0空间页，1=简报页，2=财报页，3=行业页
			String msginfo = "财报更新："+DateUtil.getSysDate(DateUtil.HHMMSS,
							new Date())+"发布"+DateUtil.getJDString(c.getReportTime())+"<ilink>#"+c.getCompanyCode()+":"+c.getSimpileName()+"#</ilink>"+DateUtil.getJDString(c.getReportTime())+"的财报";			
			TalkMessageService.getInstance().broadcastUsbjectTalkMessage(c.getCompanyCode(), msginfo, 5, headerMap);
			// 构造一个出新季报消息，带最新财务标签
//			UsubjectEventService.getInstance().broadcastUsbjectTalkMessage(
//					c.getCompanyCode(), msgBody);
		}
		BFUtil.flushDisk(BFConst.newCFNotifyBF);
	}

	public String getAccountRegion(String uidentify) {
		Company c = getCompanyByCodeFromCache(uidentify);
		if (c != null) {
			return c.getAccountRegion();
		}
		return "12-31|12-31";
	}

	public void initHkZCompanys() {

		List<USubject> cl = USubjectService.getInstance().getUSubjectAHZList();
		for (USubject us : cl) {
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
					us.getUidentify());
			if (c == null) {
				c = new Company();
				c.setSimpileName(us.getName());
				c.setCompanyCode(us.getUidentify());
				LCEnter.getInstance().put(
						StockUtil.getCompanyNameKey(c.getSimpileName()), c,
						StockConstants.COMPANY_CACHE_NAME);
				LCEnter.getInstance().put(c.getKey(), c,
						StockConstants.COMPANY_CACHE_NAME);
			}
		}
	}

}
