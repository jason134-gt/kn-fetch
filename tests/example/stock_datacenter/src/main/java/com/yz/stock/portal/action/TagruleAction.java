package com.yz.stock.portal.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.CompileMode;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.model.company.Stock0001;
import com.stock.common.model.snn.Gene;
import com.stock.common.model.snn.SnnConst;
import com.stock.common.util.DateUtil;
import com.stock.common.util.HttpTookit;
import com.stock.common.util.NetUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.cache.DataLoadTimeMng;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.Stock0001Service;
import com.yfzx.service.db.StockPoolService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.hfunction.HDayService;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseProperties;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.cache.TUExtCacheLoadService;
import com.yz.stock.portal.cache.Trade0001CacheLoadService;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.task.RuleTimerTask;
import com.yz.stock.portal.task.TaskEnter;
import com.yz.stock.portal.task.TaskSchedule;
import com.yz.stock.snn.Snner;

/**
 * 实时分析action
 * 
 * @author user
 * 
 */
public class TagruleAction extends BaseAction {

	Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();
	public static List<Company> csl = null;

	@Action(value = "/tagrule/batchAddCfTag2Company")
	public String batchAddCfTag2Company() {

		String ret = ERROR;
		try {

			ComputeIndexManager.getInstance().computeInit();
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			int page = NetUtil.getParameterInt(getHttpServletRequest(), "page",
					-1);
			Date time = NetUtil.getParameterDate(getHttpServletRequest(),
					"time", StockUtil.getDefaultPeriodTime(null));
			if (page < 0) {
				System.out.println("please set page!page=" + page);
				return ERROR;
			}

			List<Tagrule> crl = TagruleService.getInstance().getNormalTagrules();
			if (crl == null) {
				log.debug("not get the new rule ;return ");
				return ERROR;
			}
			initCompile(useOldData);
			List<USubject> usl = USubjectService.getInstance().getUSubjectListAStock();
			TaskEnter.getInstance()
					.batchAddCfTag2CompanyByPage(page, crl, true,usl);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}


	/**
	 * 公司数据更新的
	 * 
	 * @return
	 */
	@Action(value = "/tagrule/batchAddCfTag_CompanyDateUpdate")
	public String batchAddCfTag_CompanyDateUpdate() {

		String ret = ERROR;
		try {

			ComputeIndexManager.getInstance().computeInit();
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			int page = NetUtil.getParameterInt(getHttpServletRequest(), "page",
					0);
			// String time = NetUtil.getParameterString(getHttpServletRequest(),
			// "time",StockUtil.getDefaultPeriodTime(""));
			if (page < 0) {
				System.out.println("please set page!page=" + page);
				return ERROR;
			}
			Date uptime = NetUtil.getParameterDate(getHttpServletRequest(),
					"uptime", Calendar.getInstance().getTime());
			RuleTimerTask.getInstance().clearTagResult();
			doComputeCFTag_computeDataUpdate(uptime, useOldData, page);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	public void doComputeCFTag_computeDataUpdate(Date uptime,
			String useOldData, int page) {
		initCompile(useOldData);
		TaskEnter.getInstance().doComputeCFTag_computeDataUpdate( uptime,
				 useOldData,  page);
		
	}

	public void clearAllExtCache() {
		System.out.println("=========clear all ext data cache!");
		for (int i = 0; i < StockConstants.UEXT_CACHE_NUM; i++) {
			try {
				ExtCacheService.getInstance().clear("uextcache_" + i);
			} catch (Exception e) {
				log.error("load Asset data failed!", e);
			}
		}
	}

	/**
	 * 以公司分页计算
	 * 
	 * @return
	 */
	@Action(value = "/tagrule/batchComputeCfTagByPage_all")
	public String batchComputeCfTagByPage_all() {
		String ret = ERROR;
		try {
			TagruleService.getInstance().updateCFRuleOfCompute();
			List<Tagrule> crl = TagruleService.getInstance().getNormalTagrules();
			if (crl == null) {
				log.debug("not get the new rule ;return ");
				return ERROR;
			}

			ComputeIndexManager.getInstance().computeInit();
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			final int page = NetUtil.getParameterInt(getHttpServletRequest(),
					"page", -1);
			final Date time = NetUtil.getParameterDate(getHttpServletRequest(),
					"time", StockUtil.getDefaultPeriodTime(null));
			if (page < 0) {
				System.out.println("please set page!page=" + page);
				return ERROR;
			}
//			RuleTimerTask.getInstance().setComputeCompanyList(
//					CompanyService.getInstance().getCompanyList());
			List<USubject> usl = USubjectService.getInstance().getUSubjectListAStock();
			deleteAllComputeTag();
			RuleTimerTask.getInstance().clearTagResult();
			initCompile(useOldData);
			TaskEnter.aip.set(page);

			// 分页计算公司数据
			while (true) {
				// 超过一分钟，认为此次任务完成，开始计算下一页
				// long settime =
				// ConfigCenterFactory.getLong("stock_dc.compute_next_page_expTime",
				// 60000l);//一分钟
				// if(Calendar.getInstance().getTimeInMillis()-StockFactory.expTime>settime)
				if (!TaskSchedule.hasRunningTask()) {
					StockFactory.expTime = Calendar.getInstance()
							.getTimeInMillis();
					int size = usl.size();
					if (TaskEnter.aip.get() * RuleTimerTask.pagesize < size) {
						ComputeIndexManager.computeIndustryRelatIndex = false;
						TaskEnter.getInstance().batchAddCfTag2CompanyByPage(
								TaskEnter.aip.get(), crl, true,usl);
						TaskEnter.aip.incrementAndGet();
					} else {
						break;
					}

				}
				try {
					Thread.sleep(10000);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			RuleTimerTask.getInstance().flushAllCfTagResult(0);

			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 用户计算需要预加截财务数据的规则
	 * @return
	 */
	@Action(value = "/tagrule/computeUnnormalTagrule")
	public String computeUnnormalTagrule() {
		ComputeIndexManager.getInstance().computeInit();
		String useOldData = this.getHttpServletRequest().getParameter(
				"useOldData");

		String ret = ERROR;
		try {

			String tagruleids = ConfigCenterFactory
					.getString("stock_dc.unnormal_tagrule_ids",
							"1226,1227,1228,1229,1230,1231,1233,1234,1236,1237,1238,1239,1240,1241,1605,1606");

			List<Tagrule> crl = new ArrayList<Tagrule>();
			for (String tid : tagruleids.split(",")) {
				Tagrule tr = TagruleService.getInstance().getTagruleById(
						Integer.valueOf(tid));
				if (tr != null) {
					crl.add(tr);
				}
			}
//			RuleTimerTask.getInstance().setComputeCompanyList(
//					CompanyService.getInstance().getCompanyList());
			List<USubject> usl = USubjectService.getInstance().getUSubjectListAStock();
			deleteAllComputeTag();
			RuleTimerTask.getInstance().clearTagResult();
			initCompile(useOldData);

			String preindexs = ConfigCenterFactory.getString(
					"stock_dc.unnormal_tagrule_pre_load_indexs",
					"4688,4689,4690,4691,4692,4693,4694,4695,4696,4697,4698");
			ComputeIndexManager.getInstance().loadOneIndexAllExtData(preindexs);
			RuleTimerTask.getInstance().batchAddCfTag2CompanyBySet("3", crl,
					false, usl);
			while (true) {
				if (TaskSchedule.hasRunningTask()) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						
					}
				}
				else
					break;
			}
			
			RuleTimerTask.getInstance().flushAllCfTagResult(1);

			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	private void deleteAllComputeTag() {
		// 取当天所有新加入到库中的指标
		List<Tagrule> crl = TagruleService.getInstance().getLatestTagruleList();
		if (crl == null) {
			log.debug("not get the new rule ;return ");
			return;
		}
		List<Company> cl = CompanyService.getInstance().getCompanyList();
		for (Company c : cl) {
			StringBuilder sb = new StringBuilder();

			// 删除所有的老标签
			for (Tagrule tr : crl) {
				String dtag = StockUtil.getCfTagId(tr.getId());
				sb.append(dtag);
				sb.append(";");
			}
			CompanyService.getInstance().delTagsOfCompany(c.getCompanyCode(),
					sb.toString());
		}

	}

	private void initCompile(String useOldData) {
		if (!StringUtil.isEmpty(useOldData))
			CompileMode.setUseCacheExtData(Boolean.valueOf(useOldData));

	}

	@Action(value = "/tagrule/createTagRule")
	public String createTagRule() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String id = req.getParameter("id");
			String rule = req.getParameter("rule");
			String tagDesc = req.getParameter("tagDesc");
			String chartDesc = req.getParameter("chartDesc");
			String tsc = req.getParameter("tsc");
			String comments = req.getParameter("comments");
			String tag = req.getParameter("tag");
			String type = req.getParameter("type");
			String ruleType = NetUtil.getParameterString(
					this.getHttpServletRequest(), "ruleType", "0");
			String timeUnit = NetUtil.getParameterString(
					this.getHttpServletRequest(), "timeUnit", "m");
			String timeInterval = NetUtil.getParameterString(
					this.getHttpServletRequest(), "timeInterval", "3");
			if (StringUtil.isEmpty(rule) || StringUtil.isEmpty(tagDesc)) {
				log.error("req para error!");
				return ERROR;
			}
			Tagrule tr = new Tagrule();
			tr.setChartDesc(chartDesc);
			tr.setRuleOriginal(rule);
			tr.setTagDesc(tagDesc);
			tr.setTsc(tsc);
			tr.setIndustryTag(tag);
			tr.setType(Integer.valueOf(type));
			tr.setRuleType(Integer.valueOf(ruleType));
			tr.setTimeUnit(timeUnit);
			tr.setTimeInterval(Integer.valueOf(timeInterval));
			if (!StringUtil.isEmpty(id))
				tr.setId(Integer.valueOf(id));
			if (!StringUtil.isEmpty(comments))
				tr.setComments(comments);
			String ret = TagruleService.getInstance().modifyTagrule(tr);
			if (!ret.equals(StockCodes.SUCCESS))
				return ERROR;
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/batchCreateTagRule")
	public String batchCreateTagRule() {
		try {
			String text = this.getHttpServletRequest().getParameter("text");
			if (StringUtil.isEmpty(text))
				return ERROR;
			String[] ma = text.split("\\n");
			for (String a : ma) {
				try {
					if (StringUtil.isEmpty(a.trim()))
						continue;
					String[] vs = a.split(";");
					if (vs.length < 3)
						continue;
					String type = "2";
					String tag = "所有行业";
					String tagDesc = vs[0];
					String rule = vs[1];
					String chartDesc = vs[2];
					String tsc = "ts_00003";
					if (StringUtil.isEmpty(rule) || StringUtil.isEmpty(tagDesc)
							|| StringUtil.isEmpty(chartDesc)) {
						continue;
					}
					Tagrule tr = new Tagrule();
					tr.setChartDesc(chartDesc.trim().replace("\t", ""));
					tr.setRuleOriginal(rule.trim().replace("\t", ""));
					tr.setTagDesc(tagDesc.trim().replace("\t", ""));
					tr.setTsc(tsc);
					tr.setIndustryTag(tag);
					tr.setType(Integer.valueOf(type));
					String ret = TagruleService.getInstance().modifyTagrule(tr);
				} catch (Exception e) {
					// TODO: handle exception
				}

			}

		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/getTagruleByTagdesc")
	public String getTagruleByTagdesc() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String tagDesc = req.getParameter("tagDesc");
			if (StringUtil.isEmpty(tagDesc)) {
				log.error("req para error!");
				return ERROR;
			}
			List<Tagrule> ml = TagruleService.getInstance().queryListByTagDesc(
					tagDesc);
			this.setResultData(ml);
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/getTagruleByTagdescOrId")
	public String getTagruleByTagdescOrId() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String tagDesc = req.getParameter("tagDesc");
			String id = req.getParameter("id");
			if (StringUtil.isEmpty(id) && StringUtil.isEmpty(tagDesc)) {
				log.error("req para error!");
				return ERROR;
			}
			List<Tagrule> ml = new ArrayList<Tagrule>();
			if (!StringUtil.isEmpty(id)) {
				Tagrule tr = TagruleService.getInstance().getTagruleById(
						Integer.valueOf(id));
				if (tr != null)
					ml.add(tr);
			} else {
				ml = TagruleService.getInstance().queryListByTagDesc(tagDesc);
			}
			if (ml != null && ml.size() > 0)
				this.setResultData(ml);
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/getTagruleByid")
	public String getTagruleByid() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String id = req.getParameter("id");
			if (StringUtil.isEmpty(id)) {
				log.error("req para error!");
				return ERROR;
			}
			Tagrule tr = TagruleService.getInstance().getTagruleById(
					Integer.valueOf(id));
			if (tr != null)
				this.setResultData(tr);
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/testTagrule")
	public String testTagrule() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String rule = req.getParameter("rule");
			String companycode = req.getParameter("companycode");
			String ttime = NetUtil.getParameterString(
					this.getHttpServletRequest(), "time", "");
			String ruleType = NetUtil.getParameterString(
					this.getHttpServletRequest(), "ruleType", "0");
			String timeUnit = NetUtil.getParameterString(
					this.getHttpServletRequest(), "timeUnit", "m");
			String ruleInterval = NetUtil.getParameterString(
					this.getHttpServletRequest(), "ruleInterval", "3");
			Date time;
			if (StringUtil.isEmpty(rule) || StringUtil.isEmpty(companycode)) {
				log.error("req para error!");
				return ERROR;
			}
			rule = rule.trim();
			Tagrule tr = new Tagrule();
			tr.setRuleOriginal(rule);
			if (StringUtil.isEmpty(ttime)) {
				time = IndexService.getInstance().formatTime(new Date(),
						Integer.valueOf(ruleType), DateUtil.getTUint(timeUnit),
						companycode);
			} else
				time = DateUtil.format(ttime);
			if (time == null)
				return ERROR;
			int accord = TagruleService.getInstance().isAccord(companycode,
					time, StockUtil.getRuleByComments(rule),
					Integer.valueOf(ruleType));
			IndexMessage im = SMsgFactory.getUDCIndexMessage(companycode);
			im.setCompanyCode(companycode);
			im.setTime(time);
			im.setNeedAccessExtRemoteCache(false);
			im.setNeedUseExtDataCache(true);
			im.setNeedComput(true);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedRealComputeIndustryValue(true);
			String compileResult = CRuleService.getInstance().compileRule(
					StockUtil.getRuleByComments(rule), im,
					Integer.valueOf(ruleType));
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("accord", Integer.valueOf(accord));
			m.put("compileResult", compileResult);
			this.setResultData(m);
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/mapreduce/m/batch")
	public String mapreducem_batch() {
		try {
			String stime = NetUtil.getParameterString(
					this.getHttpServletRequest(), "stime", "2009-01-01");
			List<Gene> gl = new ArrayList<Gene>();
			List<Tagrule> trl = TagruleService.getInstance().queryAllTagrules();
			for(Tagrule tr:trl)
			{
				if(tr.getRuleType()==2)
				{
					try {
						Gene g = new Gene(String.valueOf(tr.getId()), tr,
								SnnConst.generation_0, tr.getTsc());
						List<USubject> usl = USubjectService.getInstance()
								.getCallTestUSubjectListOfHost();
						for (USubject us : usl) {
							Stock0001 s = Stock0001Service.getInstance()
									.getStock0001ByCompanycodeFromCache(
											us.getUidentify());
							if (s == null)
								continue;
							USubject cus = USubjectService.getInstance()
									.getUSubjectByUIdentifyFromCache(
											us.getUidentify());
							if (cus != null) {
								cus.removeAttr("mintime");
							}
						}
						Snner.getInstance().testOneRule(g,
								DateUtil.format(stime));
						gl.add(g);
						System.out.println(g.getRule().getTagDesc()+":"+g.getStatis().getDescJSon());
					} catch (Exception e) {
						e.printStackTrace();
					}	
				}
			}
			System.out.println("=========================================================");
			for(Gene g:gl)
			{
				System.out.println(g.getRule().getTagDesc()+":"+g.getStatis().getDescJSon());
			}
			
			
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	static Boolean isrunning = false;
	@Action(value = "/tagrule/mapreduce/m")
	public String mapreducem() {
		try {
			synchronized (isrunning) {
				if(isrunning)
				{
					log.error("mapreducem is running!try again later!");
					return ERROR;
				}
					
			}
			String isSnnServer = BaseProperties
					.getWebXmlProperties("isSnnServer");
			if(StringUtil.isEmpty(isSnnServer)||!"1".equals(isSnnServer))
			{
				log.error("is not snn server!");
				return ERROR;
			}
			HttpServletRequest req = this.getHttpServletRequest();
			String rule = req.getParameter("rule");
			String tagDesc = req.getParameter("tagDesc");
			
			if (StringUtil.isEmpty(rule) || StringUtil.isEmpty(tagDesc)) {
				log.error("req para error!");
				return ERROR;
			}
			int hostpage = USubjectService.getInstance().getHostPage();
			//如果hostpage为0,则认为是主机，主机需要把规则发到协作机上去
			if(hostpage==0)
			{
				String mapreduce_rhosts = ConfigCenterFactory.getString("snn.mapreduce_rhosts", "192.168.1.113:8003");
				
				Map<String,String> params = getParametersMap(req);
				String[] mrsa = mapreduce_rhosts.split(";");
				List<Future<String>> fl = new ArrayList<Future<String>>();
				//包括自己也发一条
				for(int i=0;i<mrsa.length;i++)
				{
					String url = "http://"+mrsa[i]+"/stock/tagrule/mapreduce/r";
					Future<String> f = sendTask2MapreduceR(url,params);
					fl.add(f);
				}
				int fsize = 0;
				Map<String,Map<String,Object>> m = new HashMap<String,Map<String,Object>>();
				//等待各从机数据运算结果
				for(Future<String> f:fl)
				{
					try {
						String s = f.get();
						if(!s.equals("-9999"))
						{
							fsize++;
						}
						List<Map<String, Object>> lm = (List<Map<String, Object>>) ((Map<String, Object>) JSONUtil
								.deserialize(s)).get("reduce");
						if (lm != null) {
							for (int i = 0; i < lm.size(); i++) {
								Map<String, Object> rm = lm.get(i);
								String k = (String) rm.get("k");
								Map<String, Object> mm = m.get(k);
								if (mm == null) {
									m.put(k, rm);
								} else {
									Iterator<String> iter = rm.keySet()
											.iterator();
									while (iter.hasNext()) {
										String rk = iter.next();
										Object rv = rm.get(rk);
										if (!rk.equals("k")) {
											Object mv = mm.get(rk);
											Double mmv = Double.valueOf(rv
													.toString())
													+ Double.valueOf(mv
															.toString());
											mm.put(rk,
													SMathUtil.getDouble(mmv, 2));
										}
									}
								}

							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				Iterator<String> iter = m.keySet().iterator();
				while(iter.hasNext())
				{
					String rk = iter.next();
					Map<String,Object> cm = m.get(rk);
					Iterator<String> citer = cm.keySet().iterator();
					while(citer.hasNext())
					{
						String crk = citer.next();
						//对平均涨幅求平均
						if(crk.equals("zf"))
						{
							Object mv = cm.get(crk);
							Double mmv = Double.valueOf(mv.toString())/fsize;
							cm.put(crk, SMathUtil.getDouble(mmv, 2));
						}
					}
				}
				
				
				StockUtil.outputJson(getHttpServletResponse(), JSONUtil.serialize(m));
			}
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/tagrule/mapreduce/r")
	public String mapreducer() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String id = req.getParameter("id");
			String rule = req.getParameter("rule");
			String tagDesc = req.getParameter("tagDesc");
			String chartDesc = req.getParameter("chartDesc");
			String tsc = req.getParameter("tsc");
			String comments = req.getParameter("comments");
			String tag = req.getParameter("tag");
			String type = req.getParameter("type");
			String ruleType = NetUtil.getParameterString(
					this.getHttpServletRequest(), "ruleType", "0");
			String timeUnit = NetUtil.getParameterString(
					this.getHttpServletRequest(), "timeUnit", "m");
			String timeInterval = NetUtil.getParameterString(
					this.getHttpServletRequest(), "timeInterval", "3");
			String mstime = NetUtil.getParameterString(
					this.getHttpServletRequest(), "mstime", "1990-01-01");
			if (StringUtil.isEmpty(rule) || StringUtil.isEmpty(tagDesc)) {
				log.error("req para error!");
				return ERROR;
			}
			
			Tagrule tr = new Tagrule();
			tr.setChartDesc(chartDesc);
			tr.setRuleOriginal(rule);
			tr.setTagDesc(tagDesc);
			tr.setTsc(tsc);
			tr.setIndustryTag(tag);
			tr.setType(Integer.valueOf(type));
			tr.setRuleType(Integer.valueOf(ruleType));
			tr.setTimeUnit(timeUnit);
			tr.setTimeInterval(Integer.valueOf(timeInterval));
			if (!StringUtil.isEmpty(id))
				tr.setId(Integer.valueOf(id));
			if (!StringUtil.isEmpty(comments))
				tr.setComments(comments);
			int rid = new Random().nextInt();
			tr.setId(rid);
			Gene g = new Gene(String.valueOf(tr.getId()), tr, SnnConst.generation_0,tr.getTsc());
			
			String stime = mstime;
			List<USubject> usl = USubjectService.getInstance()
					.getCallTestUSubjectListOfHost();
			for (USubject us : usl) {
				Stock0001 s = Stock0001Service
						.getInstance()
						.getStock0001ByCompanycodeFromCache(us.getUidentify());
				if (s == null)
					continue;
				USubject cus = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(us.getUidentify());
				if (cus != null) {
					cus.removeAttr("mintime");
				}
			}
			String s = Snner.getInstance().testOneRule(g,DateUtil.format(stime));
			System.out.println(s);
			StockUtil.outputJson(getHttpServletResponse(), s);
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@SuppressWarnings("unchecked")
	private Future<String> sendTask2MapreduceR(final String url, final Map<String, String> params) {
		return StockFactory.submit(new Callable<String>(){

			@Override
			public String call() throws Exception {
				
				return HttpTookit.doPost(url, params,-1);
			}
			
		});
		
	}

	private Map<String, String> getParametersMap(HttpServletRequest request) {
		Map<String, String> pm = new HashMap<String, String>();
		Enumeration e = request.getParameterNames();
		while (e.hasMoreElements()) {
			Object k = e.nextElement();
			Object v = request.getParameter(k.toString());
			if (v != null) {
				pm.put(k.toString(), v.toString());
			}
		}
		return pm;
	}
	
	@Action(value = "/tagrule/testAllCompanyTagrule")
	public String testAllCompanyTagrule() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String rule = req.getParameter("rule");
			String ttime = NetUtil.getParameterString(
					this.getHttpServletRequest(), "time", "");
			int checkCount = NetUtil.getParameterInt(
					this.getHttpServletRequest(), "checkCount", 1);
			String ruleType = NetUtil.getParameterString(
					this.getHttpServletRequest(), "ruleType", "0");
			String timeUnit = NetUtil.getParameterString(
					this.getHttpServletRequest(), "timeUnit", "m");
			if (StringUtil.isEmpty(rule)) {
				log.error("req para error!");
				return ERROR;
			}
			rule = rule.trim();
			Tagrule tr = new Tagrule();
			tr.setRuleOriginal(rule);
			Date time = null;
			if (!StringUtil.isEmpty(ttime))
				time = DateUtil.format(ttime);
			else
				time = new Date();
			List<Company> cl = getCallTestUSubjectList();
			StringBuilder sb = new StringBuilder();
			int checked = 0;
			int xh = 0;
			do {
				for (Company c : cl) {
					try {
						Date actime = IndexService.getInstance()
								.formatTime(time, Integer.valueOf(ruleType),
										DateUtil.getTUint(timeUnit),
										c.getCompanyCode());
						if (actime == null)
							continue;
						int accord = TagruleService.getInstance().isAccord(
								c.getCompanyCode(), actime,
								StockUtil.getRuleByComments(rule),
								Integer.valueOf(ruleType));
						if (accord > 0) {
							checked++;
							sb.append(c.getSimpileName() + ":"
									+ c.getCompanyCode());
							sb.append(";");
						}
						if (checkCount != -1 && checked >= checkCount)
							break;
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				xh++;
				if (checkCount != -1 && checked >= checkCount)
					break;
				if (!sb.toString().endsWith("d;") && checkCount != -1
						&& checked > 0 && checked < checkCount) {
					sb.append("date:" + DateUtil.formatDate2YYYYMMDDFast(time));
					sb.append("d;");
				}
				if (Integer.valueOf(ruleType) == StockConstants.TRADE_TYPE) {
					time = StockUtil.getNextTimeV3(time, -1,
							Calendar.DAY_OF_MONTH);
				}

			} while (Integer.valueOf(ruleType) == StockConstants.TRADE_TYPE
					&& xh < 700);
			sb.append("date:" + DateUtil.formatDate2YYYYMMDDFast(time));
			sb.append("d;");
			this.setResultData(sb.toString());
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/convert2result")
	public String convert2result() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String companycodes = req.getParameter("companycodes");
			String ttime = NetUtil.getParameterString(
					this.getHttpServletRequest(), "time", "");
			if (StringUtil.isEmpty(companycodes)) {
				log.error("req para error!");
				return ERROR;
			}
			StringBuilder sb = new StringBuilder();
			for (String cs : companycodes.split(";")) {
					try {
						Company c = CompanyService.getInstance().getCompanyByCodeFromCache(cs);
							sb.append(c.getSimpileName() + ":"
									+ c.getCompanyCode());
							sb.append(";");
					} catch (Exception e) {
						e.printStackTrace();
					}

			}
			sb.append("date:" + DateUtil.formatDate2YYYYMMDDFast(ttime));
			sb.append("d;");
			this.setResultData(sb.toString());
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/tagrule/dcheckAllCompanyTagrule")
	public String dcheckAllCompanyTagrule() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String rule = req.getParameter("rule");
			String ttime = NetUtil.getParameterString(
					this.getHttpServletRequest(), "time", "");
			int checkCount = NetUtil.getParameterInt(
					this.getHttpServletRequest(), "checkCount", 1);
			String ruleType = NetUtil.getParameterString(
					this.getHttpServletRequest(), "ruleType", "0");
			String timeUnit = NetUtil.getParameterString(
					this.getHttpServletRequest(), "timeUnit", "m");
			if (StringUtil.isEmpty(rule)) {
				log.error("req para error!");
				return ERROR;
			}
			rule = rule.trim();
			Tagrule tr = new Tagrule();
			tr.setRuleOriginal(rule);
			Date time = null;
			if (!StringUtil.isEmpty(ttime))
				time = DateUtil.format(ttime);
			else
				time = new Date();
			List<Company> cl = getCallTestUSubjectList();
			StringBuilder sb = new StringBuilder();
			int checked = 0;
			int xh = 0;
			do {
				for (Company c : cl) {
					try {
						Date actime = IndexService.getInstance()
								.formatTime(time, Integer.valueOf(ruleType),
										DateUtil.getTUint(timeUnit),
										c.getCompanyCode());
						if (actime == null)
							continue;
						int accord = TagruleService.getInstance()
								.isAccordNeedCompute(c.getCompanyCode(), actime,
										StockUtil.getRuleByComments(rule),
										Integer.valueOf(ruleType), false);
						if (accord > 0) {
							checked++;
							sb.append(c.getSimpileName() + ":"
									+ c.getCompanyCode());
							sb.append(";");
						}
						if (checkCount != -1 && checked >= checkCount)
							break;
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				xh++;
				if (checkCount != -1 && checked >= checkCount)
					break;
				if (!sb.toString().endsWith("d;") && checkCount != -1
						&& checked > 0 && checked < checkCount) {
					sb.append("date:" + DateUtil.formatDate2YYYYMMDDFast(time));
					sb.append("d;");
				}
				if (Integer.valueOf(ruleType) == StockConstants.TRADE_TYPE) {
					time = StockUtil.getNextTimeV3(time, -1,
							Calendar.DAY_OF_MONTH);
				}

			} while (Integer.valueOf(ruleType) == StockConstants.TRADE_TYPE
					&& xh < 700);
			sb.append("date:" + DateUtil.formatDate2YYYYMMDDFast(time));
			sb.append("d;");
			this.setResultData(sb.toString());
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	/*
	 * 实时计算当前公司的所有财务标签
	 */
	@Action(value = "/tagrule/realComputeOneCompanyAllCFTag")
	public String realComputeOneCompanyAllCFTag() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String companycode = req.getParameter("companycode");
			Date time = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"time", StockUtil.getDefaultPeriodTime(null));

			if (StringUtil.isEmpty(companycode) || time == null) {
				log.error("req para error!");
				return ERROR;
			}
			String ret = TagruleService.getInstance()
					.realComputeOneCompanyAllCFTag(companycode, time);
			if (!StringUtil.isEmpty(ret))
				this.setResultData(ret);
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/delTagrule")
	public String delTagrule() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String id = req.getParameter("id");
			if (StringUtil.isEmpty(id)) {
				log.error("req para error!");
				return ERROR;
			}
			String ret = TagruleService.getInstance().delTagruleById(
					Integer.valueOf(id));
			if (!ret.equals(StockCodes.SUCCESS))
				return ERROR;
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/loadTestTradeData")
	public String loadTestTradeData() {
		try {
			Date stime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"stime", StockUtil.getNextTime(
							StockUtil.getApproPeriod(new Date()), -36));
			int start = NetUtil.getParameterInt(this.getHttpServletRequest(),
					"start", 0);
			int end = NetUtil.getParameterInt(this.getHttpServletRequest(),
					"end", 100);

			List<Company> sl = CompanyService.getInstance()
					.getCompanyListFromCache();
			csl = sl.subList(start, end);
			Trade0001CacheLoadService.getInstance()
					.loadCompanysTradeNotClearCache(csl, stime);
			TUExtCacheLoadService.getInstance().loadUExtCompanyData(csl,true,DateUtil.format2String(stime));
			for (Company us : csl) {
				Stock0001 s = Stock0001Service
						.getInstance()
						.getStock0001ByCompanycodeFromCache(us.getCompanyCode());
				if (s == null)
					continue;
				USubject cus = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(us.getCompanyCode());
				if (cus != null) {
					cus.removeAttr("mintime");
				}
			}
			for (Company c : csl) {
				Stock0001 s = Stock0001Service.getInstance()
						.getStock0001ByCompanycodeFromCache(c.getCompanyCode());
				if (s == null)
					continue;
				HDayService.getInstance().computeDayTradeHavg(
						c.getCompanyCode());
			}
			System.out.println("loadTestTradeData end ....");
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/loadSetIndexData")
	public String loadSetIndexData() {
		try {
			int start = NetUtil.getParameterInt(this.getHttpServletRequest(),
					"start", 0);
			int end = NetUtil.getParameterInt(this.getHttpServletRequest(),
					"end", 100);
			String indexcodes = NetUtil.getParameterString(
					this.getHttpServletRequest(), "indexcodes", "");
			String companycode = NetUtil.getParameterString(
					this.getHttpServletRequest(), "companycode", "");
			if ( StringUtil.isEmpty(indexcodes))
				return ERROR;
			
			List<Company> sl = CompanyService.getInstance()
					.getCompanyListFromCache();
			csl = sl.subList(start, end);
			System.out.println("loadTestTradeData start ....");
			String[] ia = indexcodes.split(",");
			for (String indexcode : ia) {
				Dictionary d = DictService.getInstance().getDataDictionaryFromCache(indexcode);
				if(d==null)
					continue;
				System.out.println("loadTestTradeData indexcode="+indexcode);
				if(!StringUtil.isEmpty(companycode))
				{
					ComputeIndexManager.getInstance().loadOneIndexOneCompanyData(d, companycode);
				}
				else
				{
					for (Company us : csl) {
						Stock0001 s = Stock0001Service
								.getInstance()
								.getStock0001ByCompanycodeFromCache(us.getCompanyCode());
						if (s == null)
							continue;
						USubject cus = USubjectService.getInstance()
								.getUSubjectByUIdentifyFromCache(us.getCompanyCode());
						if (cus != null) {
							cus.removeAttr("mintime");
						}
						ComputeIndexManager.getInstance().loadOneIndexOneCompanyData(d, us.getCompanyCode());
					}
				}
				
			}
			
			
			System.out.println("loadTestTradeData end ....");
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/tagrule/loadTestCompanyTradeData")
	public String loadTestCompanyTradeData() {
		try {
			int loadCfData = NetUtil.getParameterInt(
					this.getHttpServletRequest(), "loadCfData", 0);
			Date stime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"stime", StockUtil.getNextTime(
							StockUtil.getApproPeriod(new Date()), -36));
			String companycode = NetUtil.getParameterString(
					this.getHttpServletRequest(), "companycode", "");
			String indexcodes = NetUtil.getParameterString(
					this.getHttpServletRequest(), "indexcodes", "");
			if (StringUtil.isEmpty(companycode)
					|| StringUtil.isEmpty(indexcodes))
				return ERROR;
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
					companycode);
			if (c == null)
				return ERROR;
			List<Company> sl = new ArrayList<Company>();
			sl.add(c);
			Trade0001CacheLoadService.getInstance()
					.loadCompanysTradeNotClearCache(sl, stime);
			if (csl == null)
				csl = new ArrayList<Company>();
			if (!csl.contains(c))
				csl.add(c);
			for (Company us : csl) {
				Stock0001 s = Stock0001Service
						.getInstance()
						.getStock0001ByCompanycodeFromCache(us.getCompanyCode());
				if (s == null)
					continue;
				USubject cus = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(us.getCompanyCode());
				if (cus != null) {
					cus.removeAttr("mintime");
				}
			}
			if (loadCfData == 1) {
				TUExtCacheLoadService.getInstance().loadUExtData(companycode);
			}
			TradeService.getInstance().computeCompanySetIndexs(csl, indexcodes);
			System.out.println("loadTestCompanyTradeData end ....");
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tagrule/computeIndexOfNeedRealtime")
	public String computeIndexOfNeedRealtime() {
		try {
			String indexcodes = NetUtil.getParameterString(
					this.getHttpServletRequest(), "indexcodes", "");
			Date stime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"stime", StockUtil.getNextTime(
							StockUtil.getApproPeriod(new Date()), -36));
			List<Company> ccsl = null;
			if (csl != null)
				ccsl = csl;
			else
				ccsl = CompanyService.getInstance().getCompanyListFromCache();
			if (StringUtil.isEmpty(indexcodes) || ccsl == null) {
				System.out.println("indexcodes is null");
				return ERROR;
			}
			TradeService.getInstance()
					.computeCompanySetIndexs(ccsl, indexcodes);
			System.out.println("computeIndexOfNeedRealtime end ....");
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	public List<Company> getCallTestUSubjectList() {
		List<Company> sl = csl;
		if (sl == null)
			sl = CompanyService.getInstance().getCompanyListFromCache();
		return sl;
	}
}
