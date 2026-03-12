package com.yfzx.service.agent;

import java.util.Date;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.store.chm.ConcurrentHashMap;

import com.stock.common.datacenter.DefaultDataFetcher;
import com.stock.common.model.Cfirule;
import com.stock.common.model.Company;
import com.stock.common.model.ConditionDefine;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Indexreference;
import com.stock.common.model.Template;
import com.stock.common.msg.Message;
import com.stock.common.msg.common.CIndexSeries;
import com.stock.common.msg.common.SimpleSeries;
import com.stock.common.util.FConst;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.ConditionDefineService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.MarketService;
import com.yfzx.service.db.ReferenceService;
import com.yfzx.service.db.Stock0001Service;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.hfunction.HAbbcService;
import com.yfzx.service.hfunction.HAppearService;
import com.yfzx.service.hfunction.HAvgService;
import com.yfzx.service.hfunction.HBollService;
import com.yfzx.service.hfunction.HCXGXDService;
import com.yfzx.service.hfunction.HCcService;
import com.yfzx.service.hfunction.HCountService;
import com.yfzx.service.hfunction.HDateService;
import com.yfzx.service.hfunction.HDayAvgService;
import com.yfzx.service.hfunction.HExpCountService;
import com.yfzx.service.hfunction.HExpService;
import com.yfzx.service.hfunction.HFqlhzService;
import com.yfzx.service.hfunction.HGService;
import com.yfzx.service.hfunction.HGetMaxMinService;
import com.yfzx.service.hfunction.HGetTimesService;
import com.yfzx.service.hfunction.HGetsfbsService;
import com.yfzx.service.hfunction.HKdjService;
import com.yfzx.service.hfunction.HLxndService;
import com.yfzx.service.hfunction.HMacdService;
import com.yfzx.service.hfunction.HMonthAvgService;
import com.yfzx.service.hfunction.HMonthService;
import com.yfzx.service.hfunction.HNdlhzService;
import com.yfzx.service.hfunction.HPoweiService;
import com.yfzx.service.hfunction.HPzService;
import com.yfzx.service.hfunction.HRsiService;
import com.yfzx.service.hfunction.HRsvService;
import com.yfzx.service.hfunction.HSlhtService;
import com.yfzx.service.hfunction.HSplService;
import com.yfzx.service.hfunction.HWeekAvgService;
import com.yfzx.service.hfunction.HWeekService;
import com.yfzx.service.hfunction.HpdlongService;
import com.yfzx.service.hfunction.IFService;


public class CompileDefaultDataFetcherImpl extends DefaultDataFetcher {

	static Map<String,IFService> _fcache = new ConcurrentHashMap<String,IFService>();
	public Dictionary getDataDictionary(String indexCode) {
		// TODO Auto-generated method stub
		return DictService.getInstance().getDataDictionaryFromCache(indexCode);
	}

	public Cfirule getCIndexRuleByCode(String indexCode) {
		// TODO Auto-generated method stub
		return CRuleService.getInstance().getCfruleByCodeFromCache(indexCode);
	}

	public Double executeExpresion(String expression) {
		// TODO Auto-generated method stub
		return CRuleService.getInstance().executeExpresion(expression);
	}

	public Double computeIndex(String rule, IndexMessage msg,int indextype) {
		// TODO Auto-generated method stub
		return CRuleService.getInstance().computeIndex(rule, msg, indextype);
	}

	public List<Dictionary> getDictionaryByTableType(String tablecode) {
		// TODO Auto-generated method stub
		return DictService.getInstance().getDictListByTableCode(tablecode);
	}
	
	public String getImageServerPath() {
		// TODO Auto-generated method stub
		return null;
	}

	public Company getCompanyByCode(String c) {
		// TODO Auto-generated method stub
		return CompanyService.getInstance().getCompanyByCode(c);
	}

	public List<Map> getIndexMapList(IndexMessage im) {
		// TODO Auto-generated method stub
		return IndexValueAgent.getIndexMapList(im);
	}

	public Template getTemplateByCode(Integer templateCode) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getIndustryIndexValue(String mark, String indexcode,
			int type, Date time) {
		// TODO Auto-generated method stub
		return IndustryService.getInstance().getIndustryIndexValueFromExt(mark, indexcode, type, time);
	}

	
	public Double getDividend(String companycode, Date time)  {
		// TODO Auto-generated method stub
		return MarketService.getInstance().getDividendFromCache(companycode,time);
	}

	public Double getAllFinancing(String companyCode) {
		// TODO Auto-generated method stub
		return MarketService.getInstance().getAllFinancingFromCache(companyCode);
	}

	public Double getAllStockNum(String companycode,Date time) {
		// TODO Auto-generated method stub
		return MarketService.getInstance().getStockNum(companycode,time);
	}

	public Double getStockPrice(String companycode,String type) {
		// TODO Auto-generated method stub
		return MarketService.getInstance().getStockPrice(companycode,type);
	}
	
	public List<Company> getCompanyListByTag(String tag) {
		// TODO Auto-generated method stub
		return CompanyService.getInstance().getCompanyListByTagFromCache(tag);
	}
	
	public Indexreference getIndexReference(IndexMessage im) {
		// TODO Auto-generated method stub
		return ReferenceService.getInstance().getIndexReference(im);
	}
	
	public List<Dictionary> getFCDictionary(String capTag) {
		// TODO Auto-generated method stub
		return DictService.getInstance().getFCDictionary(capTag);
	}
	

	
	public Double getCompanyExtIndexValue(Dictionary d, IndexMessage msg) {
		// TODO Auto-generated method stub
		return IndexService.getInstance().getCompanyExtIndexValue(d,msg);
	}

	public Double getIndustryExtIndexValue(Dictionary d, IndexMessage msg) {
		// TODO Auto-generated method stub
		return IndustryService.getInstance().getIndustryExtIndexValue(d,msg);
	}
	
	public Double getCompanyBaseIndexValue(IndexMessage msg) {
		// TODO Auto-generated method stub
		return IndexService.getInstance().getCompanyBaseIndexValue(msg);
		
	}

	public ConditionDefine getConditionDefineById(String id) {
		// TODO Auto-generated method stub
		return ConditionDefineService.getInstance().getConditionDefineById(Integer.valueOf(id));
	}


	public Map<String, Object> getCompanyMap(Map<String, Object> pm) {

		return CompanyService.getInstance().getCompanyMap(pm);
	}

	public Map<String, String> getCompanyInfoByCode(String companycode) {
		// TODO Auto-generated method stub
		return CompanyService.getInstance().getCompanyInfoByCode(companycode);
	}

	public String getIndustryCodeByCompanyCode(String companyCode) {
		// TODO Auto-generated method stub
		return IndustryService.getInstance().getIndustryCodeByCompanycode(companyCode);
	}

	public List<CIndexSeries> getIndexSeriesList(IndexMessage im, String rule) {
		// TODO Auto-generated method stub
		return IndexService.getInstance().getIndexSeriesList(im,rule);
	}

	public Double getIndustryBaseIndexValue(IndexMessage req) {
		// TODO Auto-generated method stub
		return IndustryService.getInstance().getIndustryBaseIndexValue(req);
	}
	public Double getIndexValue(IndexMessage im) {
		// TODO Auto-generated method stub
		return IndexValueAgent.getIndexValue(im);
	}
	
	public Double getExtIndexValue(Dictionary d, Message msg) {
		IndexMessage im = (IndexMessage) msg;
		return IndexValueAgent.getExtIndexValue(d, im);
	}

	public Double getBaseIndexValue(Dictionary d, Message msg) {
		// TODO Auto-generated method stub
		return IndexValueAgent.getBaseIndexValue(d, msg);
	}
	
	public List<SimpleSeries> getIndexSeriesListForShowChart(IndexMessage req,
			String rule) {
		// TODO Auto-generated method stub
		return IndexService.getInstance().getIndexSeriesListForShowChart(req, rule);
	}
	
	public void cacheMidResult(Dictionary d, IndexMessage midmsg, Double mr) {
		IndexValueAgent.cacheMidResult(d, midmsg, mr);
		
	}
	
	
	public Double computeIndustryValue(IndexMessage iim) {
		return IndustryService.getInstance().computeIndustryIndex(iim);
	}
	public String getIndustryNameOfCompany(String companyCode) {
		// TODO Auto-generated method stub
		return CompanyService.getInstance().getIndustryNameOfCompany(companyCode);
	}
	
	@Override
	public Double getIndustryAllFinancing(IndexMessage msg) {
		// TODO Auto-generated method stub
		return MarketService.getInstance().getIndustryAllFinancing(msg);
	}

	@Override
	public Double getIndustryStockPrice(IndexMessage im, String type) {
		return MarketService.getInstance().getIndustryStockPrice(im,type);
	}
	
	@Override
	public Double getIndustryMarketValue(IndexMessage im) {
		// TODO Auto-generated method stub
		return MarketService.getInstance().getIndustryMarketValue(im);
	}
	@Override
	public Double getStockPriceSp(IndexMessage im) {
		// TODO Auto-generated method stub
		return MarketService.getInstance().getStockPriceSp(im);
	}
	@Override
	public Double getWeekIndex(IndexMessage msg,String type) {
		// TODO Auto-generated method stub
		return HWeekService.getInstance().getWeekIndex(msg,type);
	}
	@Override
	public Double getMonthIndex(IndexMessage req,String type) {
		// TODO Auto-generated method stub
		return HMonthService.getInstance().getMonthIndex(req,type);
	}
	public Double computeWeekIndex(IndexMessage msg,String type) {
		// TODO Auto-generated method stub
		return HWeekService.getInstance().computeWeekIndex(msg,type);
	}

	public Double computeMonthIndex(IndexMessage req,String type) {
		// TODO Auto-generated method stub
		return HMonthService.getInstance().computeMonthIndex(req, type);
	}
	@Override
	public Double computeHWeekAvg(IndexMessage req, Integer type) {
		// TODO Auto-generated method stub
		return HWeekAvgService.getInstance().computeWeekAvg(req, type);
	}

	@Override
	public Double computeHDayAvg(IndexMessage req, Integer type) {
		// TODO Auto-generated method stub
		return HDayAvgService.getInstance().computeDayAvg(req, type);
	}

	@Override
	public Double computeHMonthAvg(IndexMessage req, Integer type) {
		// TODO Auto-generated method stub
		return HMonthAvgService.getInstance().computeMonthAvg(req, type);
	}
	
	@Override
	public Date formatTime(Date time, Dictionary d, String companyCode) {
		// TODO Auto-generated method stub
		return IndexService.getInstance().formatTime(time, d, companyCode);
	}
	public IndexMessage getUMsg(String uidentify) {
		// TODO Auto-generated method stub
		return SMsgFactory.getUMsg(uidentify);
	}
	
	public Double getAllFinancing(IndexMessage im) {
		// TODO Auto-generated method stub
		return USubjectService.getInstance().getAllFinancing(im);
	}
	public Double getStockNum(IndexMessage msg) {
		// TODO Auto-generated method stub
		return USubjectService.getInstance().getStockNum(msg);
	}
	public Double getDividend(IndexMessage im) {
		// TODO Auto-generated method stub
		return USubjectService.getInstance().getDividend(im);
	}
	public Double getTradeIndexValue(IndexMessage im) {
		// TODO Auto-generated method stub
		return IndexService.getInstance().getTradeIndexValue(im);
	}
	public boolean isCompanyMsg(IndexMessage im) {
		// TODO Auto-generated method stub
		return IndexService.isCompanyMsg(im);
	}
	
	@Override
	public Date getNextTradeUtilEnd(Date time, Dictionary d,
			String companyCode, int n) {
		
		return IndexService.getInstance().getNextTradeUtilEnd(time, d, companyCode, n);
	}
	
	@Override
	public Double getStockMianZhi(String companyCode) {
		// TODO Auto-generated method stub
		return Stock0001Service.getInstance().getStockMianZhi(companyCode);
	}
	
	@Override
	public Double invokeFunctionImpl(IndexMessage req, String fname,
			List<String> vls) {
		IFService ifs = _fcache.get(fname);
		if(ifs!=null)
		{
			return ifs.doInvoke(req, vls);
		}
		return 0.0;
	}
	
	static 
	{
		_fcache.put(FConst.FUNCTION_NAME_hrsv, HRsvService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hrsi, HRsiService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hkdj, HKdjService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hmacd, HMacdService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hboll, HBollService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hcount, HCountService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hslht, HSlhtService.getInstance());
		
		_fcache.put(FConst.FUNCTION_NAME_hlxnd,  HLxndService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hpz,  HPzService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hndlhz,  HNdlhzService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hspl,  HSplService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hfqlhz,  HFqlhzService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hpw,  HPoweiService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hgetbs,  HGetsfbsService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_habbc,  HAbbcService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hcc,  HCcService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hpowei, HPoweiService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hg, HGService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_havg, HAvgService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hdate, HDateService.getInstance());
		
		_fcache.put(FConst.FUNCTION_NAME_hexpcount, HExpCountService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hexp, HExpService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_happear, HAppearService.getInstance());
		
		_fcache.put(FConst.FUNCTION_NAME_hcxgxd, HCXGXDService.getInstance());
		
		_fcache.put(FConst.FUNCTION_NAME_hpdl, HpdlongService.getInstance());
		
		_fcache.put(FConst.FUNCTION_NAME_hgmm, HGetMaxMinService.getInstance());
		_fcache.put(FConst.FUNCTION_NAME_hgettimes, HGetTimesService.getInstance());
	}
}
