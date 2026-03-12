package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockException;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.expression.IExpresion;
import com.stock.common.model.Cfirule;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.msg.Message;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.dc.compile.ComplexCompile;
import com.yfzx.dc.compile.FunctionCompile;
import com.yfzx.dc.compile.ICompile;
import com.yfzx.dc.compile.TimeCompile;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;


public class CRuleService {

	private static CRuleService instance = new CRuleService();
	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Map<String, ICompile> _cMap = new ConcurrentHashMap<String, ICompile>();
	static String cPrefix = "complie_";
	DictService dService = DictService.getInstance();
	static Logger logger = LoggerFactory.getLogger(CRuleService.class);
	Logger log = LoggerFactory.getLogger(this.getClass());
	static {
		_cMap.put(cPrefix + StockConstants.BASE_INDEX, new TimeCompile());
		_cMap.put(cPrefix + StockConstants.DEFINE_INDEX, new TimeCompile());
		_cMap.put(cPrefix + StockConstants.TRADE_TYPE, new ComplexCompile());
//		_cMap.put(cPrefix + StockConstants.RULE_TEMPLATE, new RuleTemplate());
		_cMap.put(cPrefix + StockConstants.COMPILE_FUNCTION, new FunctionCompile());
		// 预加载一个指标规则的列表
		// init();
	}

	private CRuleService() {

	}

	/**
	 * 对规则进行编译与解析
	 */
	public static void init() {
		try {
			// 取报表体系编码
			LCEnter lcEnter = LCEnter.getInstance();
			List<String> tsl = lcEnter.get(
					StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
					StockConstants.MATCH_INFO_CACHE);
			if (tsl == null) {

				logger.error(
						"load table system code list failed!",
						new StockException(
								Integer.valueOf(StockCodes.LOAD_TABLE_SYSTEM_CODE_ERROR),
								"load table system code list failed"));
				return;
			}
//			for (String tsc : tsl) {
//				List<Cfirule> cfl = CRuleService.getInstance()
//						.getTemplateByTableSystemCode(tsc);
//				if (cfl != null) {
					// 解析模板成树型结构
//					CRuleService.getInstance().paraseTemplate2Tree(cfl);
//				}
//				// LCEnter.getInstance().put(getListKey(tsc), value,
//				// CacheUtil.getCacheName(StockConstants.C_INDEX_RULE));
//			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("load cfirule list failed!", e);
		}
		return;
	}

	public List<Cfirule> getTemplateByTableSystemCode(String tsc) {
		RequestMessage req = DAFFactory.buildRequest(
				tsc,
				"getTemplateByTableSystemCode", StockConstants.C_INDEX_RULE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			logger.error("load cfirule failed!",
					new StockException(Integer.valueOf(StockCodes.FAILED),
							"load cfirule list failed"));
			return null;
		}
		return (List<Cfirule>) value;
	}

	@SuppressWarnings("unchecked")
	private List<Cfirule> getLatestIndexRuleList(String time) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("updateTime", time);
		RequestMessage req = DAFFactory.buildRequest("getLatestIndexRuleList",
				m, StockConstants.C_INDEX_RULE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Cfirule>) value;
	}

	public List<Cfirule> getRuleListFromDb(String time,boolean istradeindex) {
		List<Cfirule> crl = null;
		if(istradeindex)
		{
			crl = getTradeRuleListFromDb(DateUtil.getSysDate(
					DateUtil.YYYYMMDD, Calendar.getInstance().getTime()));
		}
		else
		{
			crl = getCfRuleListFromDb(DateUtil.getSysDate(
					DateUtil.YYYYMMDD, Calendar.getInstance().getTime()));
		}
		return crl;
	}
	
	/**
	 * 取财务指标
	 * @param time
	 * @return
	 */
	public List<Cfirule> getCfRuleListFromDb(String time) {
		List<Cfirule> ncrl = new ArrayList<Cfirule>();
		Map<String, String> m = new HashMap<String, String>();
		m.put("updateTime", time);
		RequestMessage req = DAFFactory.buildRequest("getLatestIndexRuleList",
				m, StockConstants.C_INDEX_RULE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		List<Cfirule> crl =  (List<Cfirule>) value;
		for(Cfirule cr : crl)
		{
			if(StockUtil.isCfIndex(cr.getType()))
				ncrl.add(cr);
		}
		return ncrl;
		
	}
	/**
	 * 取行情指标
	 * @param time
	 * @return
	 */
	public List<Cfirule> getTradeRuleListFromDb(String time) {
		List<Cfirule> ncrl = new ArrayList<Cfirule>();
		Map<String, String> m = new HashMap<String, String>();
		m.put("updateTime", time);
		RequestMessage req = DAFFactory.buildRequest("getLatestIndexRuleList",
				m, StockConstants.C_INDEX_RULE);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		List<Cfirule> crl =  (List<Cfirule>) value;
		for(Cfirule cr : crl)
		{
			if(StockUtil.isTradeIndex(cr.getType()))
				ncrl.add(cr);
		}
		return ncrl;
		
	}
	
	public void updateIndexRuleOfCompute() {
		int open = ConfigCenterFactory.getInt("stock_dc.open_update_cf_rule", 1);
		if(open==0)
			return;
		RequestMessage req = DAFFactory.buildRequest("updateIndexRuleOfCompute",
				 StockConstants.C_INDEX_RULE);
		pLayerEnter.modify(req);
		
	}
	
	// //解析模板成树型结构
	@SuppressWarnings("unchecked")
	private void paraseTemplate2Tree(Object value) {

		List<Cfirule> cl = ((List<Cfirule>) value);
		for (Cfirule r : cl) {
			complieTemplate2Tree(r);
			// 把编译后的结果存入缓存
			LCEnter.getInstance().put(r.getcIndexCode(), r,
					CacheUtil.getCacheName(StockConstants.C_INDEX_RULE));
		}
	}

	private void complieTemplate2Tree(Cfirule r) {
		// TODO Auto-generated method stub
		int type = r.getType();
		ICompile compile = getCompile(type);
		compile.compileTemplate2Tree(r);
	}

	/*
	 * tsc：报表体系编码
	 */
	// private static String getListKey(String tsc) {
	// // TODO Auto-generated method stub
	// return "cfirule.list."+tsc;
	// }

	public static CRuleService getInstance() {
		return instance;
	}

	public Double computeIndex(Message request) {
		// 计算
		Double value = null;
		try {
			IndexMessage req = (IndexMessage) request;
			// 取指标规则
			Cfirule cRule = getCfruleByCodeFromCache(req.getIndexCode());
			value = computeIndex(req, cRule);
			value = SMathUtil.getDouble(value, StockConstants.DECIMAL_NUM);
		} catch (Exception e) {

			log.error(this.getClass().getSimpleName()
					+ ".computeIndex : compute failed!", e);

		}
		return value;
	}
	static Set<String> fset = new HashSet<String>();
	public Double computeIndex(Message request, Cfirule cRule) {
		// 计算
		Double value = StockConstants.DEFAULT_DOUBLE_VALUE;
		try {
			IndexMessage req = (IndexMessage) request;
			// 编译指标
			String expresion = complieRule(cRule, req);
			if (StringUtil.isEmpty(expresion)) {
				IndexMessage im = (IndexMessage) req;
				if(cRule==null)
					return null;
				String key = cRule.getRule()+im.getUidentify();
				if(!fset.contains(key))
				{
					logger.error("compile failed!  ...s:"+cRule.getRule()+";msg:"+req.toString());
				}
				fset.add(key);
				return null;
			}
			// 计算指标
			value = executeExpresion(expresion);
			if (value != null) {
				value = SMathUtil.getDouble(value, StockConstants.DECIMAL_NUM);
//				log.debug("rule : " + cRule.getRule() + " |expression : "
//						+ expresion + " |value :" + value);
			}
		} catch (Exception e) {

			log.error(this.getClass().getSimpleName()
					+ ".computeIndex : compute failed!", e);
		}
		return value;
	}
	public Double computeIndex(Message request, Tagrule cRule) {
		// 计算
		Double value = StockConstants.DEFAULT_DOUBLE_VALUE;
		try {
			IndexMessage req = (IndexMessage) request;
			// 编译指标
			String expresion = complieRule(cRule, req);
			if (StringUtil.isEmpty(expresion)) {
				IndexMessage im = (IndexMessage) req;
				String key = cRule.getRule()+im.getUidentify();
				if(!fset.contains(key))
				{
					logger.error("compile failed! rule:"+cRule.getRule()+";msg:"+req.toString());
				}
				fset.add(key);
				return null;
			}
			// 计算指标
			value = executeExpresion(expresion);
			if (value != null) {
				value = SMathUtil.getDouble(value, StockConstants.DECIMAL_NUM);
//				log.debug("rule : " + cRule.getRule() + " |expression : "
//						+ expresion + " |value :" + value);
			}
		} catch (Exception e) {

			log.error(this.getClass().getSimpleName()
					+ ".computeIndex : compute failed!", e);
		}
		return value;
	}
	public Double computeIndex(String r,Message request,int indextype) {
		// 计算
		Double value = StockConstants.DEFAULT_DOUBLE_VALUE;
		try {
			IndexMessage req = (IndexMessage) request;
			ICompile compile = getCompile(indextype);
			// 编译指标
			String expresion =  compile.compile(r, req);
			if (StringUtil.isEmpty(expresion)) {
				log.warn("compile failed! " + expresion);
				return StockConstants.DEFAULT_DOUBLE_VALUE;
			}
			// 计算指标
			value = executeExpresion(expresion);
			if (value != null) {
				value = SMathUtil.getDouble(value, StockConstants.DECIMAL_NUM);
			}
		} catch (Exception e) {

			log.error(this.getClass().getSimpleName()
					+ ".computeIndex : compute failed!", e);
		}
		return value;
	}
	public String compileRule(String r,Message request,int indextype) {
		// 计算
		String ret = "";
		try {
			IndexMessage req = (IndexMessage) request;
			ICompile compile = getCompile(indextype);
			ret =  compile.compile(r, req);
		
		} catch (Exception e) {

			log.error(this.getClass().getSimpleName()
					+ ".computeIndex : compute failed!", e);
		}
		return ret;
	}

	public Double executeExpresion(String expresion) {
		// TODO Auto-generated method stub
		Double value = StockConstants.DEFAULT_DOUBLE_VALUE;
		try {
			if (StringUtil.isEmpty(expresion)) {
				return value;
			}
			
			String[] sa = expresion.split("/");
			if(sa.length==2)
			{
				if(sa[1].matches("[-]*[\\.\\d]+"))
				{
					if(Double.valueOf(sa[1])==0)
						return StockConstants.DEFAULT_DOUBLE_VALUE;
				}
				
			}
			
			IExpresion ex = getExpresion(StockConstants.SIMPLE_EXPRESION);
			value = ex.compute(expresion);
		} catch (Exception e) {
			// TODO: handle exception
//			logger.info("comput expression failed!+expression : " + expresion);

		}
		logger.debug("executeExpresion : " + expresion + " |value :" + value);
		return value;
	}


	private IExpresion getExpresion(String simpleExpresion) {
		// TODO Auto-generated method stub
		return StockUtil.getExpression();
	}

	public String complieRule(Cfirule cRule, IndexMessage req) {
		if(cRule==null) return null;
		int type = cRule.getType();
		// String rule = cRule.getRule();
		String rule = cRule
				.getRuleByTime(req.getTime(), req.getAccountRegion());
		ICompile compile = getCompile(type);
		return compile.compile(rule, req);
	}

	public String complieRule(Tagrule cRule, IndexMessage req) {
		if(cRule==null) return null;
		int type = cRule.getRuleType();
		// String rule = cRule.getRule();
//		String rule = cRule
//				.getRuleByTime(req.getTime(), req.getAccountRegion());
		ICompile compile = getCompile(type);
		return compile.compile(cRule.getRule(), req);
	}
	
	public String complieRuleWithNameSpace(Cfirule cRule, IndexMessage req) {
		// TODO Auto-generated method stub
		int type = cRule.getType();
		// String rule = cRule.getRule();
		String rule = cRule
				.getRuleByTime(req.getTime(), req.getAccountRegion());
		ICompile compile = getCompile(type);
		String ns = cRule.getcIndexCode();
		return compile.compile(rule, req, ns);
	}

	private ICompile getCompile(int key) {
		// TODO Auto-generated method stub
		return _cMap.get(cPrefix + key);
	}

	public Cfirule getCIndexRuleByCode(String indexCode) {
		Cfirule cRule = new Cfirule(indexCode);
		String sqlMapKey = StockUtil.getBuildSqlMapKey(cRule,
				"getCIndexRuleByCode");
		RequestMessage req = DAFFactory.buildRequest(indexCode, sqlMapKey,
				cRule, StockConstants.C_INDEX_RULE);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Cfirule) value;
	}

	// 根据类型,选择对应的编译器,进行编译
	public String complieRule(String s, int type, Message msg) {
		// TODO Auto-generated method stub
		return getCompile(type).compile(s, msg);
	}

	public List<Cfirule> getCfiruleList(String tsc) {
		return LCEnter.getInstance().get(
				tsc,
				CacheUtil.getCacheName(StockConstants.C_INDEX_RULE));
	}

	public List<Cfirule> getCfiruleListFromCache(String key) {
		Object value = LCEnter.getInstance().get(
				key,CacheUtil.getCacheName(StockConstants.C_INDEX_RULE));
		// 如果没取到,再加载一次,并进行编译
		if (value == null) {
			try {
				throw new StockException(Integer.valueOf(StockCodes.FAILED),"get All rule failed!");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(this.getClass().getSimpleName()
						+ ".getAllCfiruleListFromCache ", e);
			}
		}
		// 再从缓存中取
		return (List<Cfirule>) value;
	}
	
	public String saveRule(Cfirule rule) {
		String ret = StockCodes.INSERT_FAILED;
		if (!StringUtil.isEmpty(rule.getcIndexCode())) {
			Cfirule r = getCIndexRuleByCode(rule.getcIndexCode());
			if (r != null) {
				r.setComments(rule.getComments());
				r.setRule(StockUtil.getRuleByComments(rule.getComments()));
				r.setQ1Rule(StockUtil.getRuleByComments(rule
						.getQ1RuleComments()));
				r.setQ1RuleComments(rule.getQ1RuleComments());
				r.setQ2Rule(StockUtil.getRuleByComments(rule
						.getQ2RuleComments()));
				r.setQ2RuleComments(rule.getQ2RuleComments());
				r.setQ3Rule(StockUtil.getRuleByComments(rule
						.getQ3RuleComments()));
				r.setQ3RuleComments(rule.getQ3RuleComments());
				r.setQ4Rule(StockUtil.getRuleByComments(rule
						.getQ4RuleComments()));
				r.setQ4RuleComments(rule.getQ4RuleComments());
				r.setType(rule.getType());
				r.setAcrossType(rule.getAcrossType());
				r.setTableSystemCode(rule.getTableSystemCode());
				return modifyRule(r);
			}
		}
		
		 rule  = parseRule(rule);
		// 构建消息,如果数据字典中的指标已存在,则不更新最大的indexCode
		Dictionary ud = dService.getDataDictionary(rule.getcIndexCode());
		if(ud!=null)
		{
			rule.setcIndexCode(ud.getIndexCode());
		}
		else
		{
			String mindexCode = getMaxIndexCodeOfDictionary(rule);
			rule.setcIndexCode(mindexCode);
		}
		// 如果不是模板类型的话，则需要把新增的指标加到数据字典中去
		if (ud == null) {
			// 把新定义的规则加入到数据字典中
			Dictionary d = new Dictionary();
			d.setIndexCode(rule.getcIndexCode());
			d.setTableName(StockConstants.U_EXT_INDEX);
			// d.setColumnName(rule.getName());
			d.setColumnName("value");
			d.setColumnChiName(rule.getName().trim());
			d.setShowName(d.getColumnChiName().trim());
			d.setType(Integer.valueOf(StockConstants.INDEX_TYPE_6));
			d.setTableCode(MatchinfoService.getInstance().getTableCodeByTsc(
					rule.getTableSystemCode(), StockConstants.INDEX_TYPE_6));
			d.setInterval(rule.getInterval());
			d.setTimeUnit(rule.getTimeUnit());
			d.setTctype(rule.getType());
			ret = dService.create(d);
			ud=d;
			if (!ret.equals(StockCodes.SUCCESS)) {
				return StockCodes.INSERT_FAILED;
			}
		}
		rule.setName(ud.getColumnChiName());
		
		
		// 新增规则
		String cret = createRule(rule);
		if(!StockCodes.SUCCESS.equals(cret))
		{
			cret = dService.deleteDictionayByCode(rule.getcIndexCode());
		}
		return cret;
	}

	private String createRule(Cfirule rule) {
		// TODO Auto-generated method stub
		RequestMessage reqMsg = DAFFactory.buildRequest(rule.getClass()
				.getName() + "." + StockSqlKey.cfirule_key_2, rule,
				StockConstants.C_INDEX_RULE);
		return pLayerEnter.insert(reqMsg);
	}

	private String getMaxIndexCodeOfDictionary(Cfirule r) {
		// TODO Auto-generated method stub
		Integer max = dService.getMaxIndexCode();
		if (max == null) {
			max = 10000;
		}
		return String.valueOf(max + 1);
	}

	private Cfirule parseRule(Cfirule r) {
		if (!StringUtil.isEmpty(r.getCompanyCode())) {
			r.setCompanyCode(r.getCompanyCode().split(":")[1]);
		}
		String rule = StockUtil.getRuleByComments(r.getComments());
		r.setRule(rule);
		if (!StringUtil.isEmpty(r.getQ1RuleComments())) {
			String q1 = StockUtil.getRuleByComments(r.getQ1RuleComments());
			r.setQ1Rule(q1);
		}
		if (!StringUtil.isEmpty(r.getQ2RuleComments())) {
			String q2 = StockUtil.getRuleByComments(r.getQ2RuleComments());
			r.setQ2Rule(q2);
		}
		if (!StringUtil.isEmpty(r.getQ3RuleComments())) {
			String q3 = StockUtil.getRuleByComments(r.getQ3RuleComments());
			r.setQ3Rule(q3);
		}
		if (!StringUtil.isEmpty(r.getQ4RuleComments())) {
			String q4 = StockUtil.getRuleByComments(r.getQ4RuleComments());
			r.setQ4Rule(q4);
		}
		return r;
	}

	private Integer getMaxTemplateIndexCode() {
		RequestMessage reqMsg = DAFFactory.buildRequest(
				StockSqlKey.cfirule_key_3, StockConstants.common);
		Object o = pLayerEnter.queryForObject(reqMsg);
		if (o == null) {
			return null;
		}
		Map m = (Map) o;
		Object mo = m.get("max(c_index_code)");
		if (mo != null) {
			return Integer.valueOf(mo.toString());
		}
		return null;
	}

	public List<Cfirule> getRuleByCondition(Cfirule rule) {
		RequestMessage reqMsg = DAFFactory.buildRequest("getRuleByCondition",
				rule, StockConstants.common);
		Object o = pLayerEnter.queryForList(reqMsg);
		if (o == null) {
			return null;
		}

		return (List<Cfirule>) o;
	}

	public String modifyRule(Cfirule rule) {
		if (rule == null || StringUtil.isEmpty(rule.getcIndexCode())) {
			return StockCodes.PARA_ERROR;
		}
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"updateRuleByIndexCode", rule, StockConstants.C_INDEX_RULE);
		return pLayerEnter.modify(reqMsg);
	}

	public void modifyRuleName(String indexcode, String indexname) {
		Map<String,String> rm = new HashMap<String,String>();
		rm.put("indexcode", indexcode);
		rm.put("indexname", indexname);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"modifyRuleName", rm, StockConstants.C_INDEX_RULE);
		pLayerEnter.modify(reqMsg);
		
	}
	
	public Cfirule getCfruleByCodeFromCache(String indexcode)
	{
		return LCEnter.getInstance().get(indexcode, StockUtil.getCacheName(StockConstants.C_INDEX_RULE));
	}


}
