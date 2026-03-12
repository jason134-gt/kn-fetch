package com.yz.stock.portal.manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.BloomFilter;
import com.stock.common.constants.CompileMode;
import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Extindex;
import com.stock.common.model.Industry;
import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.cache.IndustryExtCacheService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.TUextService;
import com.yfzx.service.db.USubjectService;
import com.yz.common.vo.BaseVO;
import com.yz.mycore.core.inter.IOperator;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.plug.PlugManager;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.dao.db.DBDefaultDaoImpl;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.portal.cache.TUExtCacheLoadService;
import com.yz.stock.portal.excel.ImportDataBaseService;
import com.yz.stock.portal.service.Companyasset0290DataService;
import com.yz.stock.portal.service.Companycash0292DataService;
import com.yz.stock.portal.service.Companyprofile0291DataService;
import com.yz.stock.portal.task.TaskEnter;
import com.yz.stock.util.DCenterUtil;

/**
 * 指标计算管理器 负责指标计算的初始化工作
 * 
 * @author Administrator
 * 
 */
public class ComputeIndexManager {

	static int pageSize = 10000;
	static boolean iscomputeInit = false;
	static boolean isimportInit = false;
	IOperator dbOpt = new DBDefaultDaoImpl();
	static boolean isInitIndustryTags = false;
	/**
	 * 计算与行业相关的指标
	 */
	public static boolean  computeIndustryRelatIndex = false;
	static BloomFilter bf = null;
	int BLOOMFILTER_SIZE = 10000000;
	public static ComputeIndexManager instance = new ComputeIndexManager();
	Logger logger = LoggerFactory.getLogger(this.getClass());

	public static ComputeIndexManager getInstance() {
		return instance;
	}

	public static BloomFilter getBloomFilter() {
		return bf;
	}

	public void computeInit() {
		CompileMode.setComputeMode(CompileMode.mode_1);
//		refreshBaseTable();
		refreshCache();
		if (iscomputeInit)
			return;
		iscomputeInit = true;
//		docomputeInit();
		initIndustryTags() ;
//		OuterDataCenter.refreshStockTradeInfoCache();
//		TaskEnter.getInstance().loadLatest2PeriodTimeStockPrice2ExtTable();
//		Dictionary d = DictService.getInstance().getDataDictionary("2290");
//		ComputeIndexManager.getInstance().loadOneIndexAllExtData(d);
//		TaskEnter.getInstance().loadFund2ExtTable(null) ;
//		TaskEnter.getInstance().loadCGSExtTable(null);
	}

	private void refreshCache() {
		LCEnter.getInstance().refresh();
		PlugManager.getInstance().refreshSystemPlug();
		
	}

	private void refreshBaseTable() {
		EhcacheImpl cimpl = (EhcacheImpl) CacheConfig
				.getInstance().getCacheImpl();
		//刷新基础报表数据
		cimpl.refreshCacheByDataType("asset");
	}

	Set<String> jr =  new HashSet<String>();
	Set<String> bx =  new HashSet<String>();
	Set<String> zj =  new HashSet<String>();
	public void initIndustryTags() {
		if (isInitIndustryTags)
			return;
		isInitIndustryTags = true;
		jr.add("金融");
		jr.add("银行");
		bx.add("保险");
		zj.add("证券");
		zj.add("期货");
		zj.add("基金");
		doInitIndustryTags();
		
	}
	Map<String,List<String>> _tagsMap = new HashMap<String,List<String>>(); 
	private void doInitIndustryTags() {
		List<String> ls = CompanyService.getInstance().getAllTags();
		if(ls!=null&&ls.size()!=0)
		{
			for(String t : ls)
			{
				String tsc = gettype(t);
				if(StringUtil.isEmpty(tsc)) continue;
				List<String> tsl = _tagsMap.get(tsc);
				if(tsl==null)
				{
					tsl = new ArrayList<String>();
					_tagsMap.put(tsc, tsl);
					if(tsc.equals(StockConstants.TS_00003))
						tsl.add(StockUtil.getIndTagId(IndustryService.root,IndustryService.root_name));
				}
				tsl.add(t);
			}
		}
	}

	
	private String gettype(String t) {
		Industry ind = IndustryService.getInstance().getIndustryYFZXByName(t);
		if(ind==null) 
			return null; 
		if(isJR(t)) return StockConstants.TS_00004;
		if(isBX(t)) return StockConstants.TS_00005;
		if(isZJ(t)) return StockConstants.TS_00006;
		return StockConstants.TS_00003;
	}

	private boolean isZJ(String t) {
		for(String s:zj)
		{
			if(t.contains(s)) 
				return true;
		}
		return false;
	}

	private boolean isBX(String t) {
		for(String s:bx)
		{
			if(t.contains(s)) 
				return true;
		}
		return false;
	}

	private boolean isJR(String t) {
		for(String s:jr)
		{
			if(t.contains(s)) 
				return true;
		}
		return false;
	}

	public void importInit() {
		if (isimportInit)
			return;
		isimportInit = true;
		doImportInit();

	}

	private void doImportInit() {

		initBaseTableBloomFilter();
	}

	private void initBaseTableBloomFilter() {

		bf = BloomFilter.getFilter(BLOOMFILTER_SIZE, 0.1);
		buildBaseTableBloomFilter(Companyasset0290DataService.getInstance(),
				StockConstants.TABLE_NAME_tb_company_asset_0290);
		buildBaseTableBloomFilter(Companyprofile0291DataService.getInstance(),
				StockConstants.TABLE_NAME_tb_company_profile_0291);
		buildBaseTableBloomFilter(Companycash0292DataService.getInstance(),
				StockConstants.TABLE_NAME_tb_company_cash_0292);
	}

	private void buildBaseTableBloomFilter(ImportDataBaseService dService,
			String tableName) {

		try {
			int sum = getTableDataCount(tableName);
			if (sum == 0)
				return;
			int pCount = sum / pageSize + 1;
			for (int i = 0; i < pCount; i++) {
				int start = i * pageSize;
				List vl = dService.doLoadTableKeyList(tableName, start,
						pageSize);
				if (vl != null && vl.size() > 0) {
					for (Object o : vl) {
						BaseVO bv = (BaseVO) o;
						bf.add(StockUtil.getBaseVoBFKey(bv.getKey(), tableName));
					}
				}
			}
		} catch (Exception e) {
			logger.error("import data failed!", e);
		}
	}

	private void docomputeInit() {
//		bf = BloomFilter.getFilter(BLOOMFILTER_SIZE, 0.1);
		// 初始化缓存
//		SCacheUtil.loadCompanyBaseData2Cache();

	}

	private void initExtCache() {
		try {
			Object bean = BaseFactory
					.getBean(StockConstants.BEAN_ExtIndexCacheLoadService);
			if (bean == null) {
				return;
			}
			TUExtCacheLoadService ils = (TUExtCacheLoadService) bean;
			ils.dl.add(StockConstants.TYPE_EXT_INDEX);
			ils.loadData2Cache();
		} catch (Exception e) {
			logger.error("init cache failed!", e);
		}

	}




	private void buildeOneExtTableBloomFilter(String tableName) {
		int sum = getTableDataCount(tableName);
		if (sum == 0)
			return;
		int pCount = sum / pageSize + 1;
		for (int i = 0; i < pCount; i++) {
			int start = i * pageSize;
			List vl = doLoadData(tableName, start);
			if (vl != null && vl.size() > 0) {
				for (Object o : vl) {
					BaseVO bv = (BaseVO) o;
					bf.add(bv.getKey());
				}
			}
		}

	}

	private List doLoadData(String tableName, int start) {
		IOperator dbOpt = new DBDefaultDaoImpl();
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("start", start);
		m.put("limit", pageSize);
		m.put("tableName", tableName);
		List<BaseVO> retList = null;
		RequestMessage req = DAFFactory.buildRequest(
				StockSqlKey.getExtIndexListByPage, m, StockConstants.common);
		ResponseMessage resp = dbOpt.queryForList(req);
		if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
			retList = (List) resp.getResult();
		}
		return retList;
	}

	private int getTableDataCount(String tableName) {

		return DCenterUtil.getTableDataCount(tableName);
	}
	//一个公司的数据只会放在一个扩展表中，此处不用循环所有表取
	public void loadOneCompanyExtIndex(String ccode) {
		String tableName = null;
		USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(ccode);
		if(us.getType()==StockConstants.SUBJECT_TYPE_0)
		    tableName = SExt.getUExtTableName(ccode,SExt.EXT_TABLE_TYPE_0);
		else
			tableName = SExt.getUExtTableName(ccode,SExt.EXT_TABLE_TYPE_1);
		if(tableName==null)
			return ;
		ComputeIndexManager.getInstance().loadOneCompanyExtData(tableName, ccode);
	}
	
	public void loadOneCompanyExtIndex(String ccode,int type)
	{
		String tableName = SExt.getUExtTableName(ccode,type);
		if(tableName==null)
			return ;
		ComputeIndexManager.getInstance().loadOneCompanyExtData(tableName, ccode);
	}
	public List<BaseVO> loadDataFromDBByPage(String dType, String tableName,
			int start, int limit, String stime, String etime) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("start", start);
		m.put("limit", limit);
		m.put("tableName", tableName);
		m.put("stime", stime);
		m.put("etime", etime);
		List<BaseVO> retList = null;
		RequestMessage req = DAFFactory.buildRequest(
				"getextindexPageData2CacheBytime", m, dType);
		ResponseMessage resp = dbOpt.queryForList(req);
		if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
			retList = (List) resp.getResult();
		}
		return retList;

	}
	public void addData2Cache(List<BaseVO> dlist) {
		// TODO Auto-generated method stub
		if (dlist != null) {
			for (BaseVO vo : dlist) {
				try {
					Extindex evo = (Extindex) vo;
					TUextService.getInstance().putData(evo.getCompanyCode(), evo.getTime().getTime(), evo.getIndexCode(), evo.getValue());
//					String key = StockUtil.getExtKeyByCompanyAndTime(
//							evo.getCompanyCode(), evo.getTime());
//					Map<Object, Float> cm = ExtCacheService.getInstance()
//							.getMap(key);
//					if (cm == null) {
//						cm = new ConcurrentHashMap<Object, Float>();
//						ExtCacheService.getInstance().putMap(key, cm);
//					}
//					Object o = cm.get(Integer.valueOf(evo.getIndexCode()));
//					if (o == null || !o.equals(evo.getValue()))
//						cm.put(Integer.valueOf(evo.getIndexCode()),
//								evo.getValue());
				} catch (Exception e) {
					logger.error("put data 2 cache failed!",e);
				}

			}
		}
	}

	public int getPageSize(String dType, String tableName, String stime,
			String etime) {
		int count = 0;
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("tableName", tableName);
		m.put("stime", stime);
		m.put("etime", etime);
		RequestMessage req = DAFFactory.buildRequest(
				"getextindexAllCountByTime", m, StockConstants.common);
		ResponseMessage resp = dbOpt.queryForObject(req);
		if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
			count = (Integer) resp.getResult();
		}
		if (count == 0)
			return 0;
		return count / pageSize + 1;

	}
	
	private void loadOneCompanyExtData(String tableName, String ccode) {
		
		List dl = doLoadOneCompanyExtData(tableName,ccode);
		if(dl!=null&&dl.size()>0)
		{
			for(Object o : dl)
			{

				try {
					Extindex evo = (Extindex) o;
//					String key = StockUtil.getExtKeyByCompanyAndTime(
//							evo.getCompanyCode(), evo.getTime());
//					Map<Object, Float> cm = ExtCacheService.getInstance()
//							.getMap(key);
//					if (cm == null) {
//						cm = new ConcurrentHashMap<Object, Float>();
//						ExtCacheService.getInstance().putMap(key, cm);
//					}
//					Object oo = cm.get(Integer.valueOf(evo.getIndexCode()));
//					if (oo == null || !oo.equals(evo.getValue()))
//						cm.put(Integer.valueOf(evo.getIndexCode()),
//								evo.getValue());
					TUextService.getInstance().putData(evo.getCompanyCode(), evo.getTime().getTime(), evo.getIndexCode(), evo.getValue());
				} catch (Exception e) {
					logger.error("put data 2 cache failed!",e);
				}
			}
		}
		
	}

	private List doLoadOneCompanyExtData(String tableName, String ccode) {
		
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("tableName", tableName);
		m.put("companyCode", ccode);
		List<BaseVO> retList = null;
		RequestMessage req = DAFFactory.buildRequest(
				StockSqlKey.getloadOneCompanyExtData, m, StockConstants.common);
		ResponseMessage resp = dbOpt.queryForList(req);
		if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
			retList = (List) resp.getResult();
		}
		return retList;
	}

	/**
	 * 加载某一扩展指标的所有数据
	 * @param d
	 */
	public void loadOneIndexAllExtData(Dictionary d) {
		//如果是基本指标就返回
		if(StockUtil.isBaseIndex(d.getType())) return;
		for (int i = 0; i < SExt.UEXT_U_TABLE_NUM; i++) {
			String tableName = SExt.UEXT_U_TABLE_PREFIX + i;
			loadOneIndexExtData(tableName,d.getIndexCode());
		}
	}

	public void loadOneIndexOneCompanyData(Dictionary d,String uidentify) {
		//如果是基本指标就返回
		if(StockUtil.isBaseIndex(d.getType())) return;
		String tableName = SExt.getUExtTableName(uidentify,SExt.EXT_TABLE_TYPE_0);
		if(tableName==null)
			return ;
		loadOneIndexExtData(tableName,d.getIndexCode());
	}
	
	/**
	 * 加载某一扩展指标的所有数据--从转换后的表中加载
	 * @param d
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void loadOneIndexAllExtDataFromCV(Dictionary d) {
		//如果是基本指标就返回
		if(StockUtil.isBaseIndex(d.getType())) return;
		List<Map> l = IndexService.getInstance().getCompanyIndexListFromMidCompanyIndexExt(d);
		if(l!=null&&l.size()>0)
		{
			for(Map m : l)
			{
				
				Extindex evo = new Extindex();
				evo.setCompanyCode(String.valueOf(m.get("companycode")));
				evo.setIndexCode(d.getIndexCode());
				evo.setTime(DateUtil.format(String.valueOf(m.get("time"))));
				evo.setIndexName(d.getShowName());
				Object v =  m.get(StockUtil.getExtCovertTableIndexCode(d.getIndexCode()));
				if(v==null)
				{
					v = StockConstants.DEFAULT_DOUBLE_VALUE;
				}
				evo.setValue(Float.valueOf(v.toString()));
//				ExtCacheService.getInstance().put(evo.getKey(),  evo.getValue());
				TUextService.getInstance().putData(evo.getCompanyCode(), evo.getTime().getTime(), evo.getIndexCode(), evo.getValue());
			}
		}
	}

	private void loadOneIndexExtDataWithSql(String tableName, String indexcode) {
		SqlSession sse = StockFactory.getSqlSessionFactory().openSession();
		Connection con = sse.getConnection();

		ResultSet rs = null;
		try {
			Statement stmt = con.createStatement();
			
			String sql = " select  COMPANY_CODE, INDEX_CODE, TIME,VALUE from "
					+ tableName + " where  INDEX_CODE= '" + indexcode + "' ";
			rs = stmt.executeQuery(sql);
			
			if (rs != null) {
				try {
					while (rs.next()) {
						try {
							String tindexcode = rs.getString("index_code");
							String companycode = rs.getString("company_code");
							Float v = rs.getFloat("value");
							if(v==null)
								continue;
							Date time = rs.getDate("time");
//							String key = StockUtil.getIndExtKeyByCompanyAndTime(
//									companycode, time);
//							Map<Object, Double> cm = IndustryExtCacheService.getInstance().getMap(
//									key);
//							if (cm == null) {
//								cm = new ConcurrentHashMap<Object, Double>();
//								IndustryExtCacheService.getInstance().putMap(key, cm);
//							}
//							Object o = cm.get(tindexcode);
//							if (o == null || !o.equals(v))
//								cm.put(tindexcode, v);
							
							TUextService.getInstance().putData(companycode, time.getTime(), tindexcode, v);
						} catch (Exception e) {
							logger.error("put data 2 cache failed!", e);
						}

					}
				} catch (Exception e) {
					logger.error("put data 2 cache failed!", e);
				}
			}
		} catch (Exception e) {
			logger.error("query data failed!", e);
		} finally {
			try {
				sse.close();
			} catch (Exception e2) {
				logger.error("close db connection failed!", e2);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void loadOneIndexExtData(String tableName, String indexcode) {
		List dl = doLoadOneIndexExtData(tableName,indexcode);
		if(dl!=null&&dl.size()>0)
		{
			for(Object o : dl)
			{

				try {
					Extindex evo = (Extindex) o;
					TUextService.getInstance().putData(evo.getCompanyCode(), evo.getTime().getTime(), evo.getIndexCode(), evo.getValue());
//					String key = StockUtil.getExtKeyByCompanyAndTime(
//							evo.getCompanyCode(), evo.getTime());
//					Map<Object, Float> cm = ExtCacheService.getInstance()
//							.getMap(key);
//					if (cm == null) {
//						cm = new ConcurrentHashMap<Object, Float>();
//						ExtCacheService.getInstance().putMap(key, cm);
//					}
//					Object oo = cm.get(Integer.valueOf(evo.getIndexCode()));
//					if (oo == null || !oo.equals(evo.getValue()))
//						cm.put(Integer.valueOf(evo.getIndexCode()),
//								evo.getValue());
				} catch (Exception e) {
					logger.error("put data 2 cache failed!",e);
				}
			}
		}
	}

	private void loadIndexsExtData(String tableName, String indexcodes) {
		List dl = doLoadIndexsExtData(tableName,indexcodes);
		if(dl!=null&&dl.size()>0)
		{
			for(Object o : dl)
			{

				try {
					Extindex evo = (Extindex) o;
//					String key = StockUtil.getExtKeyByCompanyAndTime(
//							evo.getCompanyCode(), evo.getTime());
//					Map<Object, Float> cm = ExtCacheService.getInstance()
//							.getMap(key);
//					if (cm == null) {
//						cm = new ConcurrentHashMap<Object, Float>();
//						ExtCacheService.getInstance().putMap(key, cm);
//					}
//					Object oo = cm.get(Integer.valueOf(evo.getIndexCode()));
//					if (oo == null || !oo.equals(evo.getValue()))
//						cm.put(Integer.valueOf(evo.getIndexCode()),
//								evo.getValue());
					TUextService.getInstance().putData(evo.getCompanyCode(), evo.getTime().getTime(), evo.getIndexCode(), evo.getValue());
				} catch (Exception e) {
					logger.error("put data 2 cache failed!",e);
				}
			}
		}
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List doLoadOneIndexExtData(String tableName, String indexcode) {
		IOperator dbOpt = new DBDefaultDaoImpl();
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("tableName", tableName);
		m.put("indexcode", indexcode);
		List<BaseVO> retList = null;
		RequestMessage req = DAFFactory.buildRequest(
				"getloadOneIndexExtData", m, StockConstants.common);
		ResponseMessage resp = dbOpt.queryForList(req);
		if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
			retList = (List) resp.getResult();
		}
		return retList;
	}

	private List doLoadIndexsExtData(String tableName, String indexcodes) {
		IOperator dbOpt = new DBDefaultDaoImpl();
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("tableName", tableName);
		m.put("indexcodes", "("+indexcodes+")");
		List<BaseVO> retList = null;
		RequestMessage req = DAFFactory.buildRequest(
				"getloadIndexsExtData", m, StockConstants.common);
		ResponseMessage resp = dbOpt.queryForList(req);
		if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
			retList = (List) resp.getResult();
		}
		return retList;
	}
	public void loadAllRAvgExtData(Dictionary d) {
		// TODO Auto-generated method stub
		initAllMarkDataOneIndex2cache(StockConstants.ravgType, d.getIndexCode());
	}
	
	@SuppressWarnings("rawtypes")
	public static void initAllMarkDataOneIndex2cache(String type,String indexcode) {
		Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
		for(int i=0;i<SExt.UEXT_I_TABLE_NUM;i++)
		{
			String tableName = SExt.UEXT_I_TABLE_PREFIX + i;
			List<Map> lm = IndustryService.getInstance().getOneIndexAllMarkData(type,tableName,indexcode);
			if(lm!=null)
			{
				for(Map m : lm)
				{
					String tag = (String) m.get("uidentify");
					Double value = (Double) m.get("value");
					String iIndexCode = (String) m.get("index_code");
					Date t = (Date) m.get("time");
					String key = StockUtil.getExtCachekey(tag, iIndexCode, t);
					
					IndustryExtCacheService.getInstance().put(key, value);
				}
			}
		}
	
		
	}

	public List<String> getTagsByTsc(String tsc) {
		// TODO Auto-generated method stub
		return _tagsMap.get(tsc);
	}
	
	public List<String> getTagsByTscV2(String tsc) {
		List<String> ls = _tagsMap.get(tsc);
		if(ls==null)
		{
			ls = new ArrayList<String>();
			_tagsMap.put(tsc, ls);
			List<USubject> usl = USubjectService.getInstance().getUSubjectListByType(StockConstants.SUBJECT_TYPE_1);
			if(usl!=null)
			{
				for(USubject us :usl)
				{
					if(us.getTscs().contains(tsc))
						ls.add(us.getUidentify());
				}
			}
		}
		
		return ls;
	}

	public void loadOneIndexAllExtData(String ds) {
		for (int i = 0; i < SExt.UEXT_U_TABLE_NUM; i++) {
			String tableName = SExt.UEXT_U_TABLE_PREFIX + i;
			loadIndexsExtData(tableName,ds);
		}
		
	}
}
