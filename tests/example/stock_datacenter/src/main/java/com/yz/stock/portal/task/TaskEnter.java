package com.yz.stock.portal.task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.expression.SimpleExpresion;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.ScoreWeight;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.model.UpdateIndexReq;
import com.stock.common.model.WeightCondition;
import com.stock.common.service.MailService;
import com.stock.common.util.CapUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.DictUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.Fund0002Service;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.StockPoolService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.db.company.Company0022Service;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.config.ConfigFactory;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.BatchQueueEntity;
import com.yz.stock.portal.service.IndustryDCService;
import com.yz.stock.portal.service.TaskService;
import com.yz.stock.portal.service.index.IndexDCService;
import com.yz.stock.util.ExecuteQueueManager;

public class TaskEnter {

	Logger log = LoggerFactory.getLogger(this.getClass());
	static TaskEnter instance = new TaskEnter();
	static String startTime;
	static String endTime;
	static String interval = "3";
	static Object lock = new Object();

	public TaskEnter() {

	}

	public static TaskEnter getInstance() {
		return instance;
	}

	// 增量---对更新了规则进行全量计算(所有公司)
	public void executeTaskOfAdd(boolean istradeindex,Date stime,Date etime) {
		try {
			synchronized (lock) {
				Configuration c = ConfigFactory.getConfiguration();
				interval = c.getString("stock.task.interval");
				log.info("========================execute compute task start of add ....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.computeLatestIndexRule(istradeindex,stime,etime);
			}

		} catch (Exception e) {
			log.error("execute compute task failed!", e);
		}
		log.info(" ================================compute task of add finish!...==============================");
	}

	// 全量--对所有公式进行全量计算
	public void executeTaskOfAll() {
		try {
			synchronized (lock) {
				Configuration c = ConfigFactory.getConfiguration();
				interval = c.getString("stock.task.interval");
				log.error("========================execute compute task start of all....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.computeAllIndexRule(startTime, endTime, interval);
			}
		} catch (Exception e) {
			log.error("execute compute task failed!", e);
		}
		log.info(" ================================compute task of all finish!...==============================");
	}
	
	/*
	 * 增量计算----只计算数据有更新的公司
	 */
	public void computeDataUpdateCompany(Date uptime,Date stime,Date etime) {

		try {
			synchronized (lock) {
				Configuration c = ConfigFactory.getConfiguration();
				interval = c.getString("stock.task.interval");
				log.info("========================execute computeDataUpdateCompany task start of ....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.computeDataUpdateCompany(interval,uptime, stime,etime);
			}
		} catch (Exception e) {
			log.error("execute computeDataUpdateCompany task failed!", e);
		}

		log.info(" ================================compute computeDataUpdateCompany of  finish!...==============================");
	}

	public void computeNewIndexByCompany(boolean istradeindex,Date stime,Date etime) {

		try {
			synchronized (lock) {
				Configuration c = ConfigFactory.getConfiguration();
				startTime = c.getString("stock.task.startTime");
				endTime = c.getString("stock.task.endTime");
				interval = c.getString("stock.task.interval");
				log.info("========================execute computeNewIndexByCompany task start of ....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.computeNewIndexByCompany(istradeindex,stime,etime);
			}
		} catch (Exception e) {
			log.error("execute computeNewIndexByCompany task failed!", e);
		}

	}

	public static AtomicInteger aip = new AtomicInteger(0);

	public void computeAllIndexByPageCompany(int page,boolean isasyn,boolean istradeindex, List<USubject> usl,Date stime,Date etime) {

		try {
			CRuleService.getInstance().updateIndexRuleOfCompute();
			aip.set(page);
			System.out
					.println("["+new Date()+"]**************************************cur page:"
							+ page + "**************************");
			synchronized (lock) {
				log.info("========================execute computeAllIndexByPageCompany task start of ....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.computeAllIndexByPageCompany(page,isasyn, istradeindex,usl,stime,etime);
			}

		} catch (Exception e) {
			log.error("execute computeNewIndexByCompany task failed!", e);
		}

	}

	public void computeAllIndexByPageCompany_notAysn(int page,boolean isasyn,boolean istradeindex, List<USubject> usl,Date stime,Date etime) {

		try {
			CRuleService.getInstance().updateIndexRuleOfCompute();
			aip.set(page);
			System.out
					.println("["+new Date()+"]**************************************cur page:"
							+ page + "**************************");
			synchronized (lock) {
				log.info("========================execute computeAllIndexByPageCompany task start of ....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.computeAllIndexByPageCompany(page, isasyn, istradeindex,usl,stime,etime);
			}

		} catch (Exception e) {
			log.error("execute computeNewIndexByCompany task failed!", e);
		}

	}
	
	public void batchAddCfTag2CompanyByPage(int page,List<Tagrule> crl,boolean loadTradeData, List<USubject> usl) {
		try {
			synchronized (lock) {
				log.info("========================execute computeAllIndexByPageCompany task start of ....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.batchAddCfTag2CompanyByPage(interval, page,crl,loadTradeData,usl);
			}
		} catch (Exception e) {
			log.error("execute computeNewIndexByCompany task failed!", e);
		}

	}

	public void computeAllIndexByCompany(Date stime,Date etime) {

		try {
			synchronized (lock) {
				Configuration c = ConfigFactory.getConfiguration();
				interval = c.getString("stock.task.interval");
				log.info("========================execute computeNewIndexByCompany task start of ....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.computeAllIndexByCompany(interval,stime,etime);
			}
		} catch (Exception e) {
			log.error("execute computeNewIndexByCompany task failed!", e);
		}

	}

	public void computeMMMA(final Date stime, final Date etime,
			String indexcode, final String tags) {

		try {
			log.info("========================execute computeMaxMinAvg task start  ....=========================");

			// 取所有要计算的指标
			List<Dictionary> dl = DictService.getInstance()
					.getAllDictionaryList();
			for (final Dictionary d : dl) {
				// 如果指定了指标，就只计算指定指标的
				if (!StringUtil.isEmpty(indexcode)
						&& !d.getIndexCode().equals(indexcode))
					continue;
				if (DictUtil.needMMMA(d)) {
					StockFactory.submitTaskBlocking(new Runnable() {
						
						public void run() {
							try {
								IndustryDCService.getInstance()
										.computeMaxMinAvgOfIndex(d, stime,
												etime, tags);
							} catch (Exception e) {
								// TODO: handle exception
							}
						}

					});
				}

			}

			log.info("========================execute computeMaxMinAvg task end  ....=========================");
		} catch (Exception e) {
			log.error("execute computeMaxMinAvg task failed!", e);
		}
	}

	/**
	 * 计算一段时间内数据有更新的公司的指标
	 * 
	 * @return
	 */
	public void computeIndexOfCompanyDataUpdate(Date uptime) {
		try {
			synchronized (lock) {
				log.info("========================execute computeDataUpdateCompany task start of ....=========================");
				RuleTimerTask rtt = RuleTimerTask.getInstance();
				rtt.computeIndexOfCompanyDataUpdate(uptime);
			}
		} catch (Exception e) {
			log.error("execute computeDataUpdateCompany task failed!", e);
		}

		log.info(" ================================compute computeDataUpdateCompany of  finish!...==============================");

	}

	static boolean isInit = true;

	/**
	 * 计算数据有更新的公司指标任务的定时间器的入口
	 */
	public void computeIndexOfCompanyDataUpdateOneDataTask() {
		try {
			if (isInit) {
				isInit = false;
				return;
			}
			String ttime = DateUtil.getSysDate(DateUtil.YYYYMMDD, Calendar
					.getInstance().getTime());
			Date uptime = StockUtil.addDate(DateUtil.format(ttime), -5, Calendar.HOUR);
			log.info("====import new data 2 normal db===========");
			// 把最新更新的数据导入正式库
			TaskEnter.getInstance().importNewData2NormalDb(uptime);
			ComputeIndexManager.getInstance().computeInit();
			log.info("====compute data update company index===========");
			TaskEnter.getInstance().computeIndexOfCompanyDataUpdate(uptime);
			log.info("====send mail===========");
			// sendMail(uptime);
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
	}

	public void sendMail(Date uptime) {
		String mailContent = buildMailContent(uptime);
		if (!StringUtil.isEmpty(mailContent)) {
			log.info("====send mail===========mailContent = " + mailContent);
			// 发邮件
			MailService.getInstance().sendMail(
					"数据有更新的公司("
							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
									new Date()) + ")", mailContent);
		}

	}

	public String buildMailContent(Date uptime) {
		LCEnter lcEnter = LCEnter.getInstance();
		List<String> tsl = lcEnter.get(
				StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
				StockConstants.MATCH_INFO_CACHE);
		List<String> codeList = new ArrayList<String>();
		for (String tsc : tsl) {
			// 扫描数据库,取出所有数据更新的公司
			List<String> cList = CompanyService.getInstance()
					.getConpanyListOfDateUpdate(tsc, uptime);
			if (cList != null) {
				codeList.addAll(cList);
			}
		}
		StringBuilder sb = new StringBuilder();

		for (String code : codeList) {
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(code);
			if (c != null) {
				sb.append(c.getSimpileName() + ":" + c.getCompanyCode());
				sb.append(";");
			}

		}
		return sb.toString();
	}

	public void importNewData2NormalDb(Date uptime) {

		System.out.println("importNewData2NormalDb start ...uptime="+uptime);
		TaskService.getInstance().importNewData2NormalDb(uptime);
		System.out.println("importNewData2NormalDb end ...uptime="+uptime);
	}

	public void importNewData2NormalDb_timer(Date uptime) {

		Date curtime = Calendar.getInstance().getTime();
		String uptime2 = StockUtil.addDate_otherV2(DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, curtime), -3, Calendar.DATE);
		//每次导入近三天的数据
		TaskService.getInstance().importNewData2NormalDb_timer(uptime2);
	}
	
	/**
	 * il：标签串
	 * 
	 * @param stime
	 * @param etime
	 * @param il
	 */
	public void caprankcompute(final Date stime, final Date etime,
			List<String> il, String cis) {

		try {
			log.info("========================execute caprankcompute task start  ....=========================");
			String[] ca = cis.split(",");
			for (String rc : ca) {
				final Dictionary d = DictService.getInstance()
						.getDataDictionary(rc);
				if (d == null)
					continue;
				
				List<Dictionary> gdl = getGroupIndex(d
						.getIndexCode());
				// 取所有的四级行业
				for (String tag : il) {
					StockFactory.submitTaskBlocking(new capRunner(stime,etime,d, gdl,tag));
				}

			}

			log.info("========================execute caprankcompute task end  ....=========================");
		} catch (Exception e) {
			log.error("execute computeMaxMinAvg task failed!", e);
		}
	}

	
	/**
	 * 给指标加上权重 指标的权重格式
	 * 0.4;n<0:0.2;n<n*0.3:0.2;n>2*avg:1.2(tag;n<0:0.1;n>2*avg:0.8
	 * )含义是，此指标的权重为0.4，而条件权重为：当n小于0时，再进行0.2
	 * 的缩放比例，第一个是权重值，后台是按条件对值再进行缩放的比例,括号中的是指当为某个行业时的条件
	 * 
	 * @param gd
	 * @param cscore
	 * @param cscore
	 * @param tag
	 *            :行业标签
	 * @param r
	 *            等分的比例，如是4，则当前指标的默认权重为是按每项1/4=25%
	 * @param time
	 * @param log
	 *            :记录打分日志
	 * @return
	 */
	public Double addWeight(Dictionary gd, Double indexvalue, Double cscore,
			String tag, int r, Date time, StringBuffer log) {

		if (indexvalue == null)
			return StockConstants.DEFAULT_DOUBLE_VALUE;
		ScoreWeight sw = gd.getScoreWeight();
		if(sw==null) return StockConstants.DEFAULT_DOUBLE_VALUE;
		Double dweight = sw.getdWeight();
		if (dweight != null) {
			cscore *= dweight * r;// 等效于：dweight/1/r
			if (log != null)
				log.append("乘上默认权重(" + dweight * r + "=" + dweight + "*" + r
						+ ")后得分：" + cscore + "\n");
		}

		// 对每个条件进行处理
		List<WeightCondition> wcl = sw.getWcl();
		if (wcl != null && wcl.size() > 0) {
			for (WeightCondition wc : wcl) {
				String exp = wc.getCondition();
				if (exp.contains("n"))
					exp = exp.replaceAll("n",
							SMathUtil.getDoubleString_V2(indexvalue, 4));
				if (exp.contains("avg")) {
					Double avgvalue = IndustryService.getInstance()
							.getMaxMinAvgMidOneTimeFromDB(tag,
									StockConstants.avgType, gd.getIndexCode(),
									time);
					if (avgvalue == null)
						continue;
					exp = exp.replaceAll("avg",
							SMathUtil.getDoubleString_V2(avgvalue, 4));
				}
				Double w = wc.getWeight();
				String limit = wc.getLimit();
				SimpleExpresion se = SimpleExpresion.getInstance();
				Double d = 0.0;
				try {
					d = se.compute(exp);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (w != null && d > 0) {
					if (limit != null) {
						// 如果有限定条件，看限定条件是否成立
						if (tag.equals(limit)) {
							cscore *= wc.getWeight();

							if (log != null)
								log.append("加上行业(" + limit + ")指定条件（"
										+ wc.getCondition() + "=" + exp
										+ "）权重(" + wc.getWeight() + ")后得分："
										+ cscore + "\n");
						}
					} else {
						cscore *= wc.getWeight();
						if (log != null)
							log.append("加上条件（" + wc.getCondition() + "=" + exp
									+ "）权重(" + wc.getWeight() + ")后得分："
									+ cscore + "\n");
					}
				}

			}
		}
		return cscore;
	}

	public String toCompanycodes(List<Company> cl) {
		String ret = "";
		StringBuffer sb = new StringBuffer();
		for (Company c : cl) {
			if (c.getF031v().equals("013007"))
				continue;// 过滤掉预披露的公司
			sb.append(c.getCompanyCode());
			sb.append(",");
		}
		ret = sb.toString();
		if (ret.endsWith(","))
			ret = ret.substring(0, ret.length() - 1);
		return ret;
	}

	private String toCompanycodes_v2(List<Company> cl) {
		String ret = "";
		StringBuffer sb = new StringBuffer();
		for (Company c : cl) {
			if (c.getF031v().equals("013007"))
				continue;// 过滤掉预披露的公司
			if (CompanyService.getInstance().removeBST(c))
				continue;// 去掉ST,*St等公司,B股上市的公司
			sb.append("'");
			sb.append(c.getCompanyCode());
			sb.append("'");
			sb.append(",");
		}
		ret = sb.toString();
		if (ret.endsWith(","))
			ret = ret.substring(0, ret.length() - 1);
		return ret;
	}

	/**
	 * 
	 * "偿债能力评分"2125" "10016_sw";"偿债能力"， "盈利质量评分"2126" "10005_sw";"盈利质量指标"
	 * "盈利能力评分"2127" "10007_sw";"盈利能力指标" "运营能力评分"2128" "10008_sw";"运营能力指标"
	 * "成长能力评分"2129" "10010_sw";"成长性指标" "管控能力评分"2130" "10009_sw";"管控能力指标"
	 * "综合能力评分"2131" "10017_sw";"综合能力指标"
	 * 
	 * 分类标签后加‘_sw’表示指标是计算能力评分的抽样指标
	 * 
	 * @param indexCode
	 * @return
	 */
	public List<Dictionary> getGroupIndex(String indexCode) {

		List<Dictionary> rdl = null;
		if (indexCode.equals(CapUtil.getCHZNL())) {
			rdl = DictService.getInstance().getDictionaryListByTag(
					CapUtil.getCapTag(CapUtil.getCHZNL()));
		}
		if (indexCode.equals(CapUtil.getYLZL())) {
			rdl = DictService.getInstance().getDictionaryListByTag(
					CapUtil.getCapTag(CapUtil.getYLZL()));
		}
		if (indexCode.equals(CapUtil.getYLNL())) {
			rdl = DictService.getInstance().getDictionaryListByTag(
					CapUtil.getCapTag(CapUtil.getYLNL()));
		}
		if (indexCode.equals(CapUtil.getYYNL())) {
			rdl = DictService.getInstance().getDictionaryListByTag(
					CapUtil.getCapTag(CapUtil.getYYNL()));
		}
		if (indexCode.equals(CapUtil.getCZNL())) {
			rdl = DictService.getInstance().getDictionaryListByTag(
					CapUtil.getCapTag(CapUtil.getCZNL()));
		}
		if (indexCode.equals(CapUtil.getGKNL())) {
			rdl = DictService.getInstance().getDictionaryListByTag(
					CapUtil.getCapTag(CapUtil.getGKNL()));
		}
		if (indexCode.equals(CapUtil.getZHNL())) {
			rdl = DictService.getInstance().getDictionaryListByTag(
					CapUtil.getCapTag(CapUtil.getZHNL()));
		}
		return rdl;
	}

	public List<Dictionary> getGroupAllIndex() {

		List<Dictionary> adl = new ArrayList<Dictionary>();
		
		List<Dictionary> rdl = DictService.getInstance()
				.getDictionaryListByTag(CapUtil.getCapTag(CapUtil.getCHZNL()));
		adl.addAll(rdl);
		
		rdl = DictService.getInstance().getDictionaryListByTag(
				CapUtil.getCapTag(CapUtil.getYLZL()));
		adl.addAll(rdl);

		rdl = DictService.getInstance().getDictionaryListByTag(
				CapUtil.getCapTag(CapUtil.getYLNL()));
		adl.addAll(rdl);

		rdl = DictService.getInstance().getDictionaryListByTag(
				CapUtil.getCapTag(CapUtil.getYYNL()));
		adl.addAll(rdl);
		rdl = DictService.getInstance().getDictionaryListByTag(
				CapUtil.getCapTag(CapUtil.getCZNL()));
		adl.addAll(rdl);
		rdl = DictService.getInstance().getDictionaryListByTag(
				CapUtil.getCapTag(CapUtil.getGKNL()));
		adl.addAll(rdl);
		rdl = DictService.getInstance().getDictionaryListByTag(
				CapUtil.getCapTag(CapUtil.getZHNL()));
		adl.addAll(rdl);
		return adl;
	}

	/**
	 * 单线程执行
	 * 
	 * @param time
	 * @param time2
	 * @param tag
	 * @param cis
	 */
	@SuppressWarnings({ "rawtypes", "static-access", "unchecked" })
	public void caprankcomputeOneTimeOneTag(Date stime, Date etime,
			String tag, String cis) {
		try {
			log.info("========================execute caprankcompute task start  ....=========================");

			String[] ca = cis.split(",");

			for (String rc : ca) {
				Dictionary d = DictService.getInstance().getDataDictionary(rc);
				if (d == null)
					continue;

				Date sTime = stime;

				Date eTime = etime;
				CompanyService cs = CompanyService.getInstance();
				List<Company> cl = cs
						.getCompanyListByTagFromCacheRemoveBST(tag);
				if (cl == null || cl.size() == 0) {
					cl = new ArrayList<Company>();
					List<Company> acl = cs.getCompanyListFromCache();
					for (Company tc : acl) {
						if (tc.getTags().contains(tag))
							cl.add(tc);
					}
				}
				String companycodes = toCompanycodes(cl);
				List<Dictionary> gdl = getGroupIndex(d.getIndexCode());
				if (cl == null || cl.size() == 0 || gdl == null
						|| gdl.size() == 0)
					continue;
				// 此次总分
				Double sum = cl.size() * gdl.size() * 1.0;

				Date sd = sTime;
				Date ed = eTime;
				// 如果起始时间,小于结束时间
				while (sd.compareTo(ed) <= 0) {
					Map<String, Double> scoreMap = new HashMap<String, Double>();
					// 循环计算某个能力下的所有指标
					for (Dictionary gd : gdl) {
						// 取某一指标，某一时间点的指标的有序集合
						List<Map<String, Object>> ml = IndexService
								.getInstance()
								.getCompanysIndexValueMapListFromCache(gd,
										sTime, companycodes);
						if (ml != null) {
							
						char b = gd.getBitSet().charAt(2);
						for (int k = 0; k < ml.size(); k++) {
							Map m = ml.get(k);
							String companycode = (String) m.get("company_code");
							Double v = (Double) m.get(gd.getIndexCode());
							Double cscore = scoreMap.get(companycode);
							if (cscore == null) {
								if (b == '1') {
									cscore = (double) (k + 1);
									if (v == null)
										cscore = (double) ml.size();
								} else {
									cscore = (double) (ml.size() - k);
									if (v == null)
										cscore = 0.0;
								}

								// 给分数加上权重，在还没有和原有分累加前加上
								cscore = addWeight(gd, v, cscore, tag,
										gdl.size(), sTime, null);
							} else {
								if (b == '1') {
									Double nscore = (double) (k + 1);
									if (v == null)
										nscore = (double) ml.size();
									// 给分数加上权重，在还没有和原有分累加前加上
									nscore = addWeight(gd, v, nscore, tag,
											gdl.size(), sTime, null);
									cscore += nscore;
								} else {
									Double nscore = (double) (ml.size() - k);
									if (v == null)
										nscore = 0.0;
									// 给分数加上权重，在还没有和原有分累加前加上
									nscore = addWeight(gd, v, nscore, tag,
											gdl.size(), sTime, null);
									cscore += nscore;
								}
							}

							scoreMap.put(companycode, cscore);

						}
					}
					}
					
					List<Entry<String, Double>> sl = new ArrayList<Entry<String, Double>>();
					sl.addAll(scoreMap.entrySet());
					
					Collections.sort(sl, new Comparator<Entry<String, Double>>(){

						public int compare(
								Entry<String, Double> o1,
								Entry<String, Double> o2) {
							if(o2.getValue()==null)
								return -1;
							if(o1.getValue()==null)
								return 1;
							return o2.getValue().compareTo(o1.getValue());
						}
						
					});
					
					for(int i=0;i<sl.size();i++) {
						Entry<String, Double> e=sl.get(i);
						String companycode = e.getKey();
						Double score = e.getValue();
						if (score == 0)
							continue;
						Double es = score / sum * 100;
						es = SMathUtil.getDouble(es, 2);
						// 把此时间点的此能力指标值更新到数据库
						String tableName = SExt
								.getUExtTableName(companycode,SExt.EXT_TABLE_TYPE_0);
						UpdateIndexReq req = new UpdateIndexReq();
						req.setColumnName(d.getColumnName());
						req.setTableName(tableName);
						req.setTime(sTime);
						req.setValue(String.valueOf(es));
						req.setIndexCode(d.getIndexCode());
						req.setUidentify(companycode);
						req.setIndexName(d.getShowName());
						// 改为指量异步更新
						ExecuteQueueManager
								.add2IQueue(
										new BatchQueueEntity(
												Integer.valueOf(StockConstants.TYPE_EXTINDEX),
												StockConstants.U_EXT_INDEX,
												req));
						String sindexcode = CapUtil.getSindexcodeByCapIndex(d.getIndexCode());
						Dictionary dictSindexcode = DictService.getInstance().getDataDictionary(sindexcode);
						// 更新名次
						req = new UpdateIndexReq();
						req.setColumnName(dictSindexcode.getColumnName());
						req.setTableName(tableName);
						req.setTime(sTime);
						int sort = i+1;
						req.setValue(String.valueOf(sort));
						req.setIndexCode(dictSindexcode.getIndexCode());
						req.setUidentify(companycode);
						req.setIndexName(dictSindexcode.getShowName());
						// 改为指量异步更新
						ExecuteQueueManager
								.add2IQueue(
										new BatchQueueEntity(
												Integer.valueOf(StockConstants.TYPE_EXTINDEX),
												StockConstants.U_EXT_INDEX,
												req));
						dictSindexcode = null;
					}
//					Iterator<String> iter = scoreMap.keySet().iterator();
//					while (iter.hasNext()) {
//						String companycode = iter.next();
//						Double score = scoreMap.get(companycode);
//						if (score == 0)
//							continue;
//						Double es = score / sum * 100;
//						es = SMathUtil.getDouble(es, 2);
//						// 把此时间点的此能力指标值更新到数据库
//						String tableName = StockUtil
//								.getExtIndexTableNameByCompany(companycode);
//						UpdateIndexReq req = new UpdateIndexReq();
//						req.setColumnName(d.getColumnName());
//						req.setTableName(tableName);
//						req.setTime(sTime);
//						req.setValue(String.valueOf(es));
//						req.setIndexCode(d.getIndexCode());
//						req.setCompanyCode(companycode);
//						req.setIndexName(d.getShowName());
//						// 改为指量异步更新
//						ExecuteQueueManager.getInstance().add2IQueue(
//								new BatchQueueEntity(Integer
//										.valueOf(StockConstants.TYPE_EXTINDEX),
//										StockConstants.C_EXT_INDEX, req));
//					}
					// 取下一个时间点
					sTime = StockUtil.getNextTime(sTime, 3);
					sd = sTime;
				}

			}

			log.info("========================execute caprankcompute task end  ....=========================");
		} catch (Exception e) {
			log.error("execute computeMaxMinAvg task failed!", e);
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getScoreRecord(Date time, String companycode,
			String inddexcode) {
		StringBuffer sb = new StringBuffer();
		try {

			Dictionary d = DictService.getInstance().getDataDictionary(
					inddexcode);
			if (d == null)
				return "";

			CompanyService cs = CompanyService.getInstance();
			String tag = CompanyService.getInstance()
					.getCompanyByCodeFromCache(companycode).getMainTag();
			Company c = cs.getCompanyByCode(companycode);
			List<Company> cl = cs.getCompanyListByTagFromCacheRemoveBST(tag);
			if (cl == null || cl.size() == 0) {
				cl = new ArrayList<Company>();
				List<Company> acl = cs.getCompanyListFromCache();
				for (Company tc : acl) {
					if (tc.getTags().contains(tag))
						cl.add(tc);
				}
			}
			String companycodes = toCompanycodes(cl);
			List<Dictionary> gdl = getGroupIndex(d.getIndexCode());
			if (cl == null || cl.size() == 0 || gdl == null || gdl.size() == 0)
				return "";
			// 此次总分
			Double sum = cl.size() * gdl.size() * 1.0;
			sb.append("--公司名：" + c.getSimpileName() + ",所属行业：" + tag + "\n");
			sb.append("--行业公司数：" + cl.size() + ",指标数：" + gdl.size() + "\n");
			Map<String, Double> scoreMap = new HashMap<String, Double>();
			// 循环计算某个能力下的所有指标
			for (Dictionary gd : gdl) {
				// 取某一指标，某一时间点的指标的有序集合
				List<Map<String, Object>> ml = IndexService.getInstance()
						.getCompanysIndexValueMapListFromCache(gd, time,
								companycodes);
				if (ml == null)
					continue;
				// 0:升，1：降
				char b = gd.getBitSet().charAt(2);
				for (int k = 0; k < ml.size(); k++) {
					Map m = ml.get(k);
					String tcompanycode = (String) m.get("company_code");
					if (!tcompanycode.equals(companycode)) {
						continue;
					}
					Double v = (Double) m.get(gd.getIndexCode());
					Double cscore = scoreMap.get(tcompanycode);
					if (cscore == null) {
						if (b == '1') {
							cscore = (double) (k + 1);
							if (v == null)
								cscore = (double) ml.size();
						} else {
							cscore = (double) (ml.size() - k);
							if (v == null)
								cscore = 0.0;
						}

						StringBuffer log = null;
						// 只记录指定公司
						if (tcompanycode.equals(companycode)) {
							sb.append("--" + gd.getShowName() + ",原始分："
									+ cscore + "\n");
							log = sb;
						}
						// 给分数加上权重，在还没有和原有分累加前加上
						cscore = addWeight(gd, v, cscore, tag, gdl.size(),
								time, log);

					} else {
						if (b == '1') {
							Double nscore = (double) (k + 1);
							if (v == null)
								nscore = (double) ml.size();
							StringBuffer log = null;
							// 只记录指定公司
							if (tcompanycode.equals(companycode)) {
								sb.append("--" + gd.getShowName() + ",原始分："
										+ nscore + "\n");
								log = sb;
							}
							// 给分数加上权重，在还没有和原有分累加前加上
							nscore = addWeight(gd, v, nscore, tag, gdl.size(),
									time, log);
							cscore += nscore;
						} else {
							Double nscore = (double) (ml.size() - k);
							if (v == null)
								nscore = 0.0;
							StringBuffer log = null;
							// 只记录指定公司
							if (tcompanycode.equals(companycode)) {
								sb.append("--" + gd.getShowName() + ",原始分："
										+ nscore + "\n");
								log = sb;
							}
							// 给分数加上权重，在还没有和原有分累加前加上
							nscore = addWeight(gd, v, nscore, tag, gdl.size(),
									time, log);
							cscore += nscore;
						}
					}
					scoreMap.put(tcompanycode, cscore);
				}
			}

			Double score = scoreMap.get(companycode);
			sb.append("总原始得分：" + score + "\n原始总分为：" + sum + "\n");
			Double es = score / sum * 100;
			es = SMathUtil.getDouble(es, 2);
			sb.append("映射后得分：" + es + "\n");
		} catch (Exception e) {
			log.error("execute computeMaxMinAvg task failed!", e);
		}
		return sb.toString();
	}

//	/*
//	 * 加载最近两期的收盘价到扩展表
//	 */
//	public void loadLatest2PeriodTimeStockPrice2ExtTable() {
//		loadStockPrice2ExtTable(null, null);
//	}

//	public void loadStockPrice2ExtTable(Date sTime, Date eTime) {
//		try {
//			if (eTime==null)
//				eTime = StockUtil.getApproPeriod(new Date());
//			if (sTime==null)
//				sTime = StockUtil.getNextTime(eTime, -6);
//
//			Date sd = sTime;
//			Date ed = eTime;
//			// 如果起始时间,小于结束时间
//			while (sd.compareTo(ed) <= 0) {
//
//				// 加载某一时间点，所有公司的股价
//				List<Map> ml = loadOneTimeAllCompanyStockPrice(sTime);
//				if (ml != null) {
//					for (Map m : ml) {
//						String companycode = (String) m.get("company_code");
//						BigDecimal p = (BigDecimal) m.get("price");
//						if (p != null) {
//							updateStockPrice2Db(companycode, p.doubleValue(),
//									sd);
//						}
//					}
//				} else {
////					log.error("not fetch the stock Price ,time=" + sd);
//				}
//				// 取下一个时间点
//				sd = StockUtil
//						.getNextTime(sd, Integer.parseInt(interval));
//				sd = DateUtil.getCurApproPeriod(sd);
//			}
//		} catch (Exception e) {
//			// TODO: handle exception
//			log.error("update all data failed!", e);
//		}
//
//	}

	public void loadFund2ExtTable(Date sTime) {
		try {
			if (sTime==null) {
				sTime = StockUtil.getApproPeriod(new Date());
				sTime = StockUtil.getNextTime(sTime, -6);
			}

			List<Company> cl = CompanyService.getInstance()
					.getCompanyListFromCache();
			for (Company c : cl) {

				List<Map> ml = loadFundCountInfoFromDb(c, sTime);
				if (ml != null) {
					try {
						for (Map m : ml) {
							String companycode = (String) m.get("company_code");
							Integer count = Integer.valueOf(m.get("count")
									.toString());
							Date enddate = (Date) m.get("ENDDATE");
							int mn = enddate.getMonth() + 1;
							if (mn != 3 && mn != 6 && mn != 9 && mn != 12)
								continue;
							Date time = enddate;
							update2ExtDb(companycode, count.doubleValue(),
									time, "4688");// 4688：基金家数
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
//					log.error("not fetch the foundition info ,time=" + sTime);
				}

				ml = loadFundStockPercentFromDb(c, sTime);
				if (ml != null) {
					try {
						for (Map m : ml) {
							String companycode = (String) m.get("company_code");
							Object o = m.get("sum");
							if (o == null)
								continue;
							BigDecimal count = (BigDecimal) o;
							Date enddate = (Date) m.get("ENDDATE");
							int mn = enddate.getMonth() + 1;
							if (mn != 3 && mn != 6 && mn != 9 && mn != 12)
								continue;
							Date time = enddate;

							IndexMessage im1 = SMsgFactory.getUMsg(
									companycode, "2290", time);
							im1.setNeedAccessExtIndexDb(false);
							im1.setNeedAccessExtRemoteCache(false);
							im1.setNeedUseExtDataCache(true);
							im1.setNeedComput(false);
							Double sn = IndexValueAgent.getIndexValue(im1);
							if (sn == null||sn==0)
								continue;
							Double bl = count.doubleValue() / sn;
							update2ExtDb(companycode, bl, time, "4689");// 4689：基金持股比例
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
//					log.error("not fetch the stock Price ,time=" + sTime);
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
			log.error("update all data failed!", e);
		}

	}

	public void loadFund2ExtTableV2(Date sTime) {
		try {
			if (sTime==null) {
				sTime = StockUtil.getApproPeriod(new Date());
				sTime = StockUtil.getNextTime(sTime, -6);
			}

			List<Company> cl = CompanyService.getInstance()
					.getCompanyListFromCache();
			for (Company c : cl) {

				List<Map> ml = Fund0002Service.getInstance().loadFundCountInfoFromDbV2(
						c.getCompanyCode(), sTime);
				if (ml != null) {
					try {
						for (Map m : ml) {
							String companycode = (String) m.get("company_code");
							Integer count = Integer.valueOf(m.get("count")
									.toString());
							Date enddate = (Date) m.get("ENDDATE");
							int mn = enddate.getMonth() + 1;
							if (mn != 3 && mn != 6 && mn != 9 && mn != 12)
								continue;
							Date time = enddate;
							update2ExtDb(companycode, count.doubleValue(),
									time, "4688");// 4688：基金家数
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				ml = Fund0002Service.getInstance().loadFundStockPercentFromDbV2(
						c.getCompanyCode(), sTime);
				if (ml != null) {
					try {
						for (Map m : ml) {
							String companycode = (String) m.get("company_code");
							Object o = m.get("sum");
							if (o == null)
								continue;
							BigDecimal count = (BigDecimal) o;
							Date enddate = (Date) m.get("ENDDATE");
							int mn = enddate.getMonth() + 1;
							if (mn != 3 && mn != 6 && mn != 9 && mn != 12)
								continue;
							Date time = enddate;

							IndexMessage im1 = SMsgFactory.getUMsg(
									companycode, "2290", time);
							im1.setNeedAccessExtIndexDb(false);
							im1.setNeedAccessExtRemoteCache(false);
							im1.setNeedUseExtDataCache(true);
							im1.setNeedComput(false);
							Double sn = IndexValueAgent.getIndexValue(im1);
							if (sn == null)
								continue;
							Double bl = count.doubleValue() / sn;
							update2ExtDb(companycode, bl, time, "4689");// 4689：基金持股比例
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} 
			}

		} catch (Exception e) {
			// TODO: handle exception
			log.error("update all data failed!", e);
		}

	}
	
	/**
	 * 人均持股数
	 * 
	 * @param sTime
	 */
	public void loadCGSExtTable(Date sTime) {
		try {
			if (sTime==null) {
				sTime = StockUtil.getApproPeriod(new Date());
				sTime = StockUtil.getNextTime(sTime, -6);
			}

			List<Map> ml = loadCGSInfoFromDb(sTime);
			if (ml != null) {
				try {
					for (Map m : ml) {
						String companycode = (String) m.get("company_code");
						Long f001n = (Long) m.get("f001n");
						Long f002n = (Long) m.get("f002n");
						Date enddate = (Date) m.get("ENDDATE");
						if (f001n == null || f002n == null)
							continue;
						Date time = enddate;
						update2ExtDb(companycode, f001n.doubleValue(), time,
								"4690");// 4690：股数总户数
						update2ExtDb(companycode, f002n.doubleValue(), time,
								"4691");// 4691：人均持股数
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				log.error("not fetch the stock Price ,time=" + sTime);
			}

		} catch (Exception e) {
			// TODO: handle exception
			log.error("update all data failed!", e);
		}

	}

	public void loadCGSExtTableV2(Date sTime) {
		try {
			if (sTime==null) {
				sTime = StockUtil.getApproPeriod(new Date());
				sTime = StockUtil.getNextTime(sTime, -6);
			}

			List<Map> ml = Company0022Service.getInstance().getCGSListV2(sTime);;
			if (ml != null) {
				try {
					for (Map m : ml) {
						String companycode = (String) m.get("company_code");
						Long f001n = (Long) m.get("f001n");
						Long f002n = (Long) m.get("f002n");
						Date enddate = (Date) m.get("ENDDATE");
						if (f001n == null || f002n == null)
							continue;
						Date time = enddate;
						update2ExtDb(companycode, f001n.doubleValue(), time,
								"4690");// 4690：股数总户数
						update2ExtDb(companycode, f002n.doubleValue(), time,
								"4691");// 4691：人均持股数
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				log.error("not fetch the stock Price ,time=" + sTime);
			}

		} catch (Exception e) {
			// TODO: handle exception
			log.error("update all data failed!", e);
		}

	}
	
	private List<Map> loadCGSInfoFromDb(Date sTime) {
		// TODO Auto-generated method stub
		return Company0022Service.getInstance().getCGSList(sTime);
	}

	private List<Map> loadFundStockPercentFromDb(Company c, Date sTime) {
		// TODO Auto-generated method stub
		return Fund0002Service.getInstance().loadFundStockPercentFromDb(
				c.getCompanyCode(), sTime);
	}

	private List<Map> loadFundCountInfoFromDb(Company c, Date sTime) {
		// TODO Auto-generated method stub
		return Fund0002Service.getInstance().loadFundCountInfoFromDb(
				c.getCompanyCode(), sTime);
	}

//	private List<Map> loadOneTimeAllCompanyStockPrice(Date time) {
//		// 取离季度度未最近的一个交易日
//		return MarketService.getInstance().loadOneTimeAllCompanyStockPrice(
//				DateUtil.getTradeTime(time));
//	}

	private void updateStockPrice2Db(String companycode, Double p, Date sTime) {
		Dictionary d = DictService.getInstance().getDataDictionary(
				StockConstants.STOCK_PRICE_INDEX_CODE);
		IndexMessage req = SMsgFactory.getUDCIndexMessage(companycode);
		req.setTime(sTime);
		// 计算指标
		req.setValue(p);
		req.setIndexCode(d.getIndexCode());
		req.setColumnName(d.getColumnName());
		req.setTableName(d.getTableName());
		req.setCompanyCode(companycode);
		// 更新指示到数据库
		IndexDCService.getInstance().upateIndex2Db(d, req);

	}

	private void update2ExtDb(String companycode, Double v, Date time,
			String indexcode) {
		Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
		if(c==null)
			return ;
		Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
		IndexMessage req = SMsgFactory.getUDCIndexMessage(companycode);
		req.setTime(time);
		// 计算指标
		req.setValue(v);
		req.setIndexCode(d.getIndexCode());
		req.setColumnName(d.getColumnName());
		req.setTableName(d.getTableName());
		req.setCompanyCode(companycode);
		// 更新指示到数据库
		IndexDCService.getInstance().upateIndex2Db(d, req);

	}

	public void computeCompanyDataByPage_notAysn(String useOldData, int page,
			List<USubject> usl, Date stime, Date etime) {
		TaskEnter.aip.set(page);
		while(true)
		{
			//超过一分钟，认为此次任务完成，开始计算下一页
//			long settime = ConfigCenterFactory.getLong("stock_dc.compute_next_page_expTime", 60000l);//一分钟
//			if(Calendar.getInstance().getTimeInMillis()-StockFactory.expTime>settime)
//			{
				StockFactory.expTime = Calendar.getInstance().getTimeInMillis();
				int size = usl.size();
				if(TaskEnter.aip.get()*RuleTimerTask.pagesize<size)
				{
					ComputeIndexManager.computeIndustryRelatIndex = false;
					TaskEnter.getInstance().computeAllIndexByPageCompany(TaskEnter.aip.get(),false,false,usl,stime,etime);
					TaskEnter.aip.incrementAndGet();
				}
				else
				{
					break;
				}
					
//			}
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
	}

	public void computeCompanyDataByPage(String useOldData, int page,boolean isasyn,boolean istradeindex,
			List<USubject> usl, Date stime, Date etime) {
		TaskEnter.aip.set(page);
		while(true)
		{
			//超过一分钟，认为此次任务完成，开始计算下一页
//			long settime = ConfigCenterFactory.getLong("stock_dc.compute_next_page_expTime", 60000l);//一分钟
//			if(Calendar.getInstance().getTimeInMillis()-StockFactory.expTime>settime)
			if(!TaskSchedule.hasRunningTask())
			{
				StockFactory.expTime = Calendar.getInstance().getTimeInMillis();
				int size = usl.size();
				if(TaskEnter.aip.get()*RuleTimerTask.pagesize<size)
				{
					ComputeIndexManager.computeIndustryRelatIndex = false;
					TaskEnter.getInstance().computeAllIndexByPageCompany(TaskEnter.aip.get(),isasyn,istradeindex,usl,stime,etime);
					TaskEnter.aip.incrementAndGet();
				}
				else
				{
					break;
				}
					
			}
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
	}

	public void doComputeCFTag_computeDataUpdate(Date uptime,
			String useOldData, int page) {
		TagruleService.getInstance().updateCFRuleOfCompute();
		try {
			List<Company> cl = CompanyService.getInstance()
					.getDataUpCompanyList(uptime);
			if (cl == null || cl.size() == 0) {
				System.out
						.println("["
								+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
										new Date())
								+ "]**************************************update company list is null,uptime:"
								+ uptime);
				return;
			}
			System.out
					.println("=========start compute current page company all cf tag!page="
							+ page + "=============== ");
			TaskEnter.aip.set(page);
//			RuleTimerTask.getInstance().setComputeCompanyList(
//					CompanyService.getInstance().getDataUpCompanyList(uptime));
			List<USubject> usl = USubjectService.getInstance().getDataUpUSubjectList(uptime);
			// 取当天所有新加入到库中的指标
			List<Tagrule> crl = TagruleService.getInstance().getNormalTagrules();
			if (crl == null) {
				log.debug("not get the new rule ;return ");
				return;
			}
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
			//清理此公司的老标签
			for(USubject us :usl)
			{
				try {
					StockPoolService.getInstance().delCompanyAllCfTag(
							us.getUidentify());
				} catch (Exception e) {
					log.error("delCompanyAllCfTag  failed!", e);
				}
			}
			RuleTimerTask.getInstance().flushAllCfTagResult(1);
		} catch (Exception e) {
			log.error("compute index failed!", e);

		}
		
	}

}
