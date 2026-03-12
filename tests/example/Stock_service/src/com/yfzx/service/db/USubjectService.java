package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.cache.DataLoadTimeMng;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

public class USubjectService {
	static DBAgent dbAgent = DBAgent.getInstance();
	private static final String USUBJECT_BASE_NS = "com.stock.common.model.USubject";
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static USubjectService instance = new USubjectService();
	private USubjectService() {
	}

	public static USubjectService getInstance() {
		return instance;
	}

	public int insertUsubject(USubject usubject){
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".insert", usubject, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return 1;
		}
		return -1;
	}

	public int insertUsubject2(USubject usubject){
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".insert2", usubject, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			//USubject usSubject = (USubject)rm.getResult();
			return 1;
		}
		return 0;
	}

	/**
	 * 查询是否存在当前话题
	 * @param usubject
	 * @return
	 */
	public int isExist(USubject usubject){
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".selectCount2", usubject, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Integer)res.getResult();
	}

	/**
	 * 查询是否存在当前话题(缓存)
	 * @param usubject
	 * @return
	 */
	public USubject isExistFromCache(String uiditify){
		USubject u = LCEnter.getInstance().get(uiditify,CacheUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
		return u ;
	}

	/**
	 * 查询所有自定义的话题
	 * @param uSubject
	 * @return
	 */
	public List<USubject> selectAllCustomTopic(USubject uSubject) {
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".selectAllByType", uSubject, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<USubject> list = (List<USubject>)rm.getResult();
		return list;
	}
	/**
	 * 根据type分页查询数据建立搜索索引
	 * @param type
	 * @param offset
	 * @param limit
	 * @return
	 */
	public List<USubject> selectListToSearch(int type, int isPassed, int offset, int limit) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		if(type != -1) {
			map.put("type", type);
		}
		map.put("isPassed", isPassed);
		map.put("offset", offset);
		map.put("limit", limit);

		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".selectListToIndex", map, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<USubject> list = (List<USubject>)rm.getResult();
		return list;
	}
	/**
	 * 根据type查询对应记录的总数
	 * @param m
	 * @return
	 */
	public int getTotalUSubjectByType(USubject m) {
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS+"."+"selectCountByType", m, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForObject(req);
		int i = (Integer)rm.getResult();
		return i;
	}

	public boolean updateUid(USubject uSubject){
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".updateUid", uSubject, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}
	
	public boolean update(USubject uSubject){
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".updateByPrimaryKey", uSubject, StockConstants.common);
		ResponseMessage rm = dbAgent.modifyRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}

	//修改话题状态
	public boolean updateStatus(USubject uSubject) {
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".updateStatus", uSubject, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			return true;
		}
		return false;
	}

	public USubject getUsubject(USubject usubject) {
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".select2", usubject, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (USubject)res.getResult();
	}

	public List<USubject> getUSubjectList() {
		// TODO Auto-generated method stub
		return LCEnter.getInstance().get(StockConstants.DATA_TYPE_usubject+".alist", StockUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
	}


	public List<USubject> getUSubjectListByType(int type) {
		// TODO Auto-generated method stub
		return LCEnter.getInstance().get(type, StockUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
	}

	public List<USubject> getUSubjectListAStock() {
		//112老是报错 Exception in thread "thread_check_data_update" java.lang.NoSuchFieldError: SUBJECT_A_STOCK_LIST 原因暂时没有查出
		//本地没有问题
//		return LCEnter.getInstance().get("A_STOCK_LIST_KEY", StockUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
		return LCEnter.getInstance().get(StockConstants.SUBJECT_A_STOCK_LIST, StockUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
	}

	public List<USubject> getUSubjectListHStock() {

		return LCEnter.getInstance().get(StockConstants.SUBJECT_H_STOCK_LIST, StockUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
	}

	/**
	 * 上市时间
	 * @param uidentify
	 * @return
	 */
	public Date getPulishTime(String uidentify) {
		Date d = new Date();
		USubject us = getUSubjectByUIdentifyFromCache(uidentify);
		if(us!=null)
		{
			String key = "pulishtime";
			 d = us.getAttr(key);
			if(d==null)
			{
				if(us.getType()==StockConstants.SUBJECT_TYPE_0||us.getType()==StockConstants.SUBJECT_TYPE_3)
					d = CompanyService.getInstance().getCompanyPulishTime(uidentify);
				else
					d= IndustryService.getInstance().getPlatePulishTime(uidentify);
				if(d!=null)
					us.putAttr(key, d);

			}
		}
		return d;
	}

	public Date getTradeIndexMinTime(String uidentify,String indexcode) {
		Date d = new Date();
//		USubject us = getUSubjectByUIdentifyFromCache(uidentify);
//		if(us!=null)
//		{
//			String key = "mintime";
//			 d = us.getAttr(key);
//			if(d==null)
//			{
				Dictionary dd = DictService.getInstance().getDataDictionary(indexcode);
				d = getTradeIndexMinDataLoadTime(uidentify, dd);
//				if(d!=null)
//					us.putAttr(key, d);
//			}
//		}
		return d;
	}

	/**
	 * 取指标数据的起始加载时间
	 * @param uidentify
	 * @return
	 */
	public Date getTradeIndexMinDataLoadTime(String uidentify,Dictionary d) {
		Date ptime = getPulishTime(uidentify);
		if(ptime==null)
			return null;
		Date ltime = null;
		if(StockUtil.isTradeIndex(d.getTctype()))
		{
			Date sd = new Date();
			String dstime ;
			Date stime  ;
			switch(d.getTunit())
			{
			case Calendar.MONTH:
				 dstime = DateUtil.format2String(StockUtil.getNextTime(sd, -72));
				 stime =  DataLoadTimeMng.getInstance().getLoadTime(StockUtil.joinString("^",uidentify,d.getTimeUnit(),d.getTctype()));
				 if(stime==null)
					 stime = DateUtil.format(dstime);;
				 ltime = stime;
				break;
			case Calendar.WEEK_OF_MONTH:
				 dstime = DateUtil.format2String(StockUtil.getNextTime(sd, -17));
				 stime = DataLoadTimeMng.getInstance().getLoadTime(StockUtil.joinString("^", uidentify,d.getTimeUnit(),d.getTctype()));
				 if(stime==null)
					 stime = DateUtil.format(dstime);
				 ltime = stime;
				break;
			case Calendar.DAY_OF_MONTH:
				 dstime = DateUtil.format2String(StockUtil.getNextTime(sd, -4));
				 stime = DataLoadTimeMng.getInstance().getLoadTime(StockUtil.joinString("^", uidentify,d.getTimeUnit(),d.getTctype()));
				 if(stime==null)
					 stime = DateUtil.format(dstime);
				 ltime = stime;
					break;
			}
		}
		if(ltime==null)
			return ptime;
		return ltime.compareTo(ptime)>0?ltime:ptime;
	}


	/**
	 * 取可回测的对象
	 * @return
	 */
	public List<USubject> getCallTestUSubjectList() {
		List<USubject> usl = new ArrayList<USubject>();
		String utypes = ConfigCenterFactory.getString("snn.can_tested_usubject_types", "0");
		String abh = ConfigCenterFactory.getString("snn.can_tested_usubject_types_abh", "sh,sz");
		for(String utype:utypes.split(","))
		{
			List<USubject> tusl = getUSubjectListByType(Integer.valueOf(utype));

			if(tusl!=null)
			{
				for(USubject us:tusl)
				{
					if(abh.contains(us.getUidentify().split("\\.")[1]))
						usl.add(us);
				}
			}

		}
		return usl;
	}

	/**
	 * 取本机需要加载的公司列表
	 * @return
	 */
	public List<USubject> getCallTestUSubjectListOfHost() {
		List<USubject> usl = new ArrayList<USubject>();
		String utypes = ConfigCenterFactory.getString("snn.can_tested_usubject_types", "0");
		String abh = ConfigCenterFactory.getString("snn.can_tested_usubject_types_abh", "sh,sz");
		for(String utype:utypes.split(","))
		{
			List<USubject> tusl = getUSubjectListByType(Integer.valueOf(utype));

			if(tusl!=null)
			{
				for(USubject us:tusl)
				{
					if(abh.contains(us.getUidentify().split("\\.")[1]))
						usl.add(us);
				}
			}

		}
		String mapreduce_rhosts = ConfigCenterFactory.getString("snn.mapreduce_rhosts", "192.168.1.113:8003;192.168.1.113:8004");
		int pagesize = usl.size()/mapreduce_rhosts.split(";").length;
		int page = getHostPage();
		if(page!=-1)
			usl = getUSubjectListByPage(page,pagesize,usl);
		int maxsize = ConfigCenterFactory.getInt("snn.mapreduce_maxsize", 250);
		if(usl.size()>maxsize)
			usl = usl.subList(0, maxsize);
		return usl;
	}
	/**
	 * 数据中心专用方法
	 * @return
	 */
	public int getHostPage()
	{
		int page = -1;
		String mapreduce_rhosts = ConfigCenterFactory.getString("snn.mapreduce_rhosts", "192.168.1.113:8003;192.168.1.113:8004");
		String hosts = System.getProperty("snn.hosts");
		if(!StringUtil.isEmpty(hosts))
		{
			String[] mrsa = mapreduce_rhosts.split(";");
			for(int i=0;i<mrsa.length;i++)
			{
				if(hosts.equals(mrsa[i]))
				{
					page = i;
					break;
				}
			}
		}
		return page;
	}
	private List<USubject> getUSubjectListByPage(int page,int pagesize,List<USubject> usl) {

		int start = page * pagesize;
		if (start > usl.size()) {
			System.out.println("=========page is out range!---");
			return null;
		}

		int end = (page + 1) * pagesize;
		if (end > usl.size())
			end = usl.size();
		return usl.subList(start, end);
	}

	/**
	 * 取可回测的对象
	 * @return
	 */
	public List<USubject> getPlateUSubjectList() {
		List<USubject> usl = new ArrayList<USubject>();
		List<USubject> tusl = getUSubjectListByType(StockConstants.SUBJECT_TYPE_0);
		if(tusl!=null)
			usl.addAll(tusl);
		tusl = getUSubjectListByType(StockConstants.SUBJECT_TYPE_3);
		if(tusl!=null)
			usl.addAll(tusl);
		return usl;
	}

	public USubject getUSubjectByUIdentifyFromCache(String uidentify) {
		// TODO Auto-generated method stub
		return LCEnter.getInstance().get(uidentify.toLowerCase(), StockUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
	}

	public void addUSubjectByUIdentifyFromCache(String uidentify,USubject us) {
		// TODO Auto-generated method stub
		 LCEnter.getInstance().put(uidentify.toLowerCase(),us, StockUtil.getCacheName(StockConstants.DATA_TYPE_usubject));
	}

	public Double getAllFinancing(IndexMessage im) {
		Double ad = null;
		if (IndexService.isCompanyMsg(im)) {
			ad = MarketService.getInstance().getAllFinancingFromCache(im.getUidentify());
		}
		else  {
			ad = MarketService.getInstance().getIndustryAllFinancing(im);
		}
		return ad;
	}

	public Double getStockNum(IndexMessage im) {
		Double ad = null;
		if (IndexService.isCompanyMsg(im)) {
			ad = MarketService.getInstance().getStockNum(im.getCompanyCode(),im.getTime());
		}else{
			ad = MarketService.getInstance().getIndustryStockNum(im);
		}
		return ad;
	}
	public Double getDividend(IndexMessage im) {
		Double ad = null;
		if (IndexService.isCompanyMsg(im)) {
			ad = MarketService.getInstance().getDividendFromCache(im.getCompanyCode(),im.getTime());
		}else{
			ad = MarketService.getInstance().getIndustryDivided(im);
		}
		return ad;
	}

	public int getMsgType(String uidentify) {
		USubject us = getUSubjectByUIdentifyFromCache(uidentify);
		if(us!=null)
			return us.getType();
		return StockConstants.SUBJECT_TYPE_0;
	}

	@SuppressWarnings("rawtypes")
	public List getIndexObjectList(IndexMessage req) {
		RequestMessage reqMsg = DAFFactory.buildRequest("queryUextIndexList",req,StockConstants.common);
		return pLayerEnter.queryForList(reqMsg);
	}

	public int isPassed(USubject uSubject) {
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".selectIsPassed", uSubject, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (Integer)res.getResult();
	}

	public List<USubject> getUSubjectAHZList() {
		List<USubject> usl = new ArrayList<USubject>();
		List<USubject> tusl = getUSubjectListByType(StockConstants.SUBJECT_TYPE_0);
		if(tusl!=null)
			usl.addAll(tusl);
		tusl = getUSubjectListByType(StockConstants.SUBJECT_TYPE_3);
		if(tusl!=null)
			usl.addAll(tusl);
		return usl;
	}

/*	public USubject selectByUidentify(USubject uSubject) {
		RequestMessage req = DAFFactory.buildRequest(USUBJECT_BASE_NS + ".selectByUidentify", uSubject, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (USubject)res.getResult();
	}*/
	public List<USubject> getDataUpUSubjectList(Date uptime) {
		List<USubject> cl = new ArrayList<USubject>();
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
					USubject c = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(ccode);
					if (c != null) {
						cl.add(c);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (cl != null) {
			for (USubject c : cl) {
				IndexService.getInstance().rebuildMaxMinTimeCache(
						c.getUidentify());

			}
		}
		return cl;
	}

	public String getAccountRegion(String uidentify) {
		return CompanyService.getInstance().getAccountRegion(uidentify);
	}

	public List<USubject> ConvertCList2UList(List<Company> cl) {
		List<USubject> usl = new ArrayList<USubject>();
		for(Company c:cl)
		{
			USubject us = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(c.getCompanyCode());
			if(us!=null)
				usl.add(us);
		}
		return usl;
	}

	public List<USubject> getUSubjectListZStock() {
		// TODO Auto-generated method stub
		return getUSubjectListByType(StockConstants.SUBJECT_TYPE_3);
	}


}
