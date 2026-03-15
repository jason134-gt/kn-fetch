package com.yz.stock.portal.task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockException;
import com.stock.common.model.Cfirule;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Matchinfo;
import com.stock.common.model.Stockpool;
import com.stock.common.model.Tagrule;
import com.stock.common.model.Trade0001;
import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.DictUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.RealTimeService;
import com.yfzx.service.db.StockPoolService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.hfunction.HDaySumService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.util.BaseProperties;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.portal.cache.TUExtCacheLoadService;
import com.yz.stock.portal.cache.Trade0001CacheLoadService;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.BatchQueueEntity;
import com.yz.stock.portal.service.baseindex.AssertService;
import com.yz.stock.portal.service.index.IndexDCService;
import com.yz.stock.util.ExecuteQueueManager;

public class RuleTimerTask {

	CRuleService crs = CRuleService.getInstance();
	CompanyService cs = CompanyService.getInstance();
	IndexDCService is = IndexDCService.getInstance();
	Logger log = LoggerFactory.getLogger(this.getClass());
	public static RuleTimerTask instance = new RuleTimerTask();
	static String lock = new String();
	public static int pagesize = 50;
	AssertService ass = AssertService.getInstance();
	static Map<String, String> _noDCSet = new ConcurrentHashMap<String, String>();// 没有数据的公司的集合
	static Map<String, String> _havaDCSet = new ConcurrentHashMap<String, String>();// 有数据的公司的集合
	static Map<String, StringBuilder> _cfMap = new ConcurrentHashMap<String, StringBuilder>();// 财务标签MAP
	static HashSet computeIndexSet = new HashSet();
	static {
		computeIndexSet.add(1);
		computeIndexSet.add(0);
	}

	private RuleTimerTask() {

	}

	public static RuleTimerTask getInstance() {
		return instance;
	}

	/**
	 * sTime必须为一个有效的基准时间
	 * 
	 * @param sTime
	 * @param eTime
	 * @param interval
	 * @param tsc
	 */
	// public void updateAll(String interval, String tsc) {
	//
	// try {
	// // 计算一段时间内的某个公司的财务指标值
	// List<Company> cl = getAllCompany();
	// for (Company c : cl) {
	// updateOneCompany(c, interval, tsc);
	// }
	// } catch (Exception e) {
	// // TODO: handle exception
	// log.error("update all data failed!", e);
	// }
	//
	// }

	/**
	 * sTime必须为一个有效的基准时间 更新一个公司某一时间段内的所有指标
	 * 
	 * @param sTime
	 * @param eTime
	 * @param interval
	 * @param tsc
	 */
	// public void updateOneCompany(Company c, String interval, String tsc) {
	// try {
	// // 看要计算的公司有没有数据
	// if (hasDataInAsset(c.getCompanyCode(), tsc)) {
	// // 计算一段时间内的某个公司的财务指标值
	// SContext ctx = SContext.getContext();
	// ctx.setCompany(c);
	// // 扫描公司指示规则表
	// List<Cfirule> cfl = crs.getCfiruleListFromCache(tsc);
	// // 对每个规则,计算出某个时间段的指标值,并存入数据库中
	// for (Cfirule crule : cfl) {
	// ctx.setcRule(crule);
	// // 计算公用指标,以及本公司的私有指标
	// if (StringUtil.isEmpty(crule.getCompanyCode())
	// || crule.getCompanyCode()
	// .equals(c.getCompanyCode())) {
	// Dictionary d = DictService.getInstance()
	// .getDataDictionary(crule.getcIndexCode());
	// ctx.setDictionary(d);
	// computeIndexAndUpdate(ctx, interval);
	//
	// }
	//
	// }
	// } else {
	// log.debug("not compute the company indexRule,besause no asset data ;companyCode "
	// + c);
	// }
	// } catch (Exception e) {
	// // TODO: handle exception
	// log.error("update all data failed!", e);
	// }
	// }
	//
	// private void computeIndexAndUpdate(SContext ctx, String interval) {
	// // TODO Auto-generated method stub
	// try {
	// Company c = ctx.getCompany();
	// Date sTime = IndexService.getInstance().getMinTime(
	// c.getCompanyCode());
	// Date eTime = IndexService.getInstance().getMaxTime(
	// c.getCompanyCode());
	//
	// IndexMessage req = SMsgFactory.getUDCIndexMessage(ctx
	// .getCompanyCode());
	// req.setAccountRegion(ctx.getCompany().getAccountRegion());
	// Cfirule crule = (Cfirule) ctx.getcRule();
	// // String acrossType = crule.getAcrossType();
	// // 取时间的近似值
	// // String[] ara = req.getAccountRegion().split("\\|");
	// // sTime = StockUtil.getApproxiTime(sTime, acrossType,
	// // ara[0]);
	// // eTime = StockUtil.getApproxiTime(eTime, acrossType,
	// // ara[1]);
	// Date sd = sTime;
	// Date ed = eTime;
	// // 如果起始时间,小于结束时间
	// while (sd.compareTo(ed) <= 0) {
	// req.setTime(sTime);
	// // 计算指标
	//
	// // 如果为不跨期指标,则计算出会计区间
	// // if (acrossType.equals(StockConstants.ACROSS_NO)) {
	// // 计算当前会计区间
	// String caRegion = StockUtil.computeCurAccountRegion(sTime,
	// req.getAccountRegion(), req.getcAccountRegion());
	// req.setcAccountRegion(caRegion);
	// // }
	//
	// Double value = crs.computeIndex(req, crule);
	// if (value == null) {
	// // 取下一个时间点
	// sTime = StockUtil.getNextTime(sTime,
	// Integer.parseInt(interval));
	// sd = sTime;
	// continue;
	// }
	// req.setValue(value);
	// req.setIndexCode(crule.getcIndexCode());
	// Dictionary d = ctx.getDictionary();
	// req.setColumnName(d.getColumnName());
	// req.setTableName(d.getTableName());
	// // 更新指示到数据库
	// is.upateIndex2Db(d, req);
	// // 取下一个时间点
	// sTime = StockUtil
	// .getNextTime(sTime, Integer.parseInt(interval));
	// sd = sTime;
	// }
	// } catch (Exception e) {
	// // TODO: handle exception
	// log.error("update all data failed!", e);
	// }
	// }

	private List<Company> getAllCompany() {
		// TODO Auto-generated method stub
		return cs.getCompanyList();
	}

	/*
	 * 计算所有更新的指标
	 */
	@SuppressWarnings("rawtypes")
	public void computeLatestIndexRule(boolean istradeindex,Date stime,Date etime) {
		// 取当天所有新加入到库中的指标
		List<Cfirule> crl = crs.getRuleListFromDb(DateUtil.getSysDate(
				DateUtil.YYYYMMDD, Calendar.getInstance().getTime()),
				istradeindex);
		if (crl == null) {
			log.debug("not get the new rule ;return ");
			return;
		}
		for (Cfirule r : crl) {
			if (DictService.getInstance().isNotSaveOrNotCompute(
					r.getcIndexCode()))
				continue;
			// 如果是行业相关指标，又不计算
			Dictionary d = DictService.getInstance().getDataDictionary(
					r.getcIndexCode());
			if (DictUtil.isIndustryXGIndex(d)) {
				if (!ComputeIndexManager.computeIndustryRelatIndex)
					continue;
			}
			log.info("start compute update rule...r=" + r);
			try {
				doComputeCFR_asyn(r,stime,etime);
			} catch (Exception e) {
				log.error("compute index failed!", e);
			}

		}

	}

	private void doComputeCFR_asyn(final Cfirule r,final Date stime,final Date etime) {
		try {
			//切记一定要放外面
			final Long pid = Thread.currentThread().getId();
			StockFactory.submitTaskBlocking(new Callable<String>(){

				@Override
				public String call() throws Exception {
					TaskSchedule.put(pid, Thread.currentThread().getId());
					try {
						List<Company> cl = CompanyService.getInstance()
								.getCompanyListFromCache();
						for (Company c : cl) {
							IndexService.getInstance().rebuildMaxMinTimeCache(
									c.getCompanyCode());
							compute_notAysn(c.getCompanyCode(), r,stime, etime);
						}
					} catch (Exception e) {
						// TODO: handle exception
					}
					finally{
						TaskSchedule.remove(pid, Thread.currentThread().getId());
					}
					return StockCodes.SUCCESS;
				}});
		} catch (Exception e) {
			log.error("compute index failed!", e);
		}
		
	}

	// private void computeOneCompanyIndexByGiveRule(Company c, Cfirule r,
	// String tsc) {
	//
	//
	// }

	/*
	 * 全量计算
	 */
	@SuppressWarnings("rawtypes")
	public void computeAllIndexRule(String sTime, String eTime, String interval) {
		// 取所有报表体系相关的指标
		LCEnter lcEnter = LCEnter.getInstance();
		List<String> tsl = lcEnter.get(
				StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
				StockConstants.MATCH_INFO_CACHE);
		if (tsl == null) {

			log.error(
					"load table system code list failed!",
					new StockException(Integer
							.valueOf(StockCodes.LOAD_TABLE_SYSTEM_CODE_ERROR),
							"load table system code list failed"));
			return;
		}
		for (String tsc : tsl) {
			// 取所有的指标
			List<Cfirule> crl = crs.getCfiruleListFromCache(tsc);
			if (crl == null) {
				log.debug("not get the new rule ;return ");
				return;
			}
			for (Cfirule r : crl) {
				if (DictService.getInstance().isNotSaveOrNotCompute(
						r.getcIndexCode()))
					continue;
				// 如果是行业相关指标，又不计算
				Dictionary d = DictService.getInstance().getDataDictionary(
						r.getcIndexCode());
				if (DictUtil.isIndustryXGIndex(d)) {
					if (!ComputeIndexManager.computeIndustryRelatIndex)
						continue;
				}
				computeIndex(r, interval, tsc);
			}
		}

	}

	@SuppressWarnings("unchecked")
	public void computeDataUpdateCompany(String interval, Date mtime,Date stime,Date etime) {
		// 取所有报表体系相关的指标
		LCEnter lcEnter = LCEnter.getInstance();
		List<String> tsl = lcEnter.get(
				StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
				StockConstants.MATCH_INFO_CACHE);
		if (tsl == null) {

			log.error(
					"load table system code list failed!",
					new StockException(Integer
							.valueOf(StockCodes.LOAD_TABLE_SYSTEM_CODE_ERROR),
							"load table system code list failed"));
			return;
		}
		for (String tsc : tsl) {
			// 扫描数据库,取出所有数据更新的公司
			List<String> cList = CompanyService.getInstance()
					.getConpanyListOfDateUpdate(tsc, mtime);
			if (cList == null || cList.size() == 0) {
				return;
			}
			for (String ccode : cList) {
				// 取所有的指标
				List<Cfirule> crl = crs.getCfiruleListFromCache(tsc);
				if (crl == null) {
					log.debug("not get the new rule ;return ");
					return;
				}
				try {
					docomputeCFRList_asyn(ccode, crl,stime,etime);
				} catch (Exception e) {
					log.error("compute index failed!", e);
				}
			}
		}

	}

	private void docomputeCFRList_asynHkBaseIndex(final String ccode, final Date stime, final Date etime) {
		try {
			//切记一定要放外面
			final Long pid = Thread.currentThread().getId();
			StockFactory.submitTaskBlocking(new Callable<String>(){

						@Override
						public String call() throws Exception {
							TaskSchedule.put(pid, Thread.currentThread().getId());
							try {
								log.info("#========================#start compute company index ;companycode="
										+ ccode);
								try {
									IndexMessage req = SMsgFactory.getUDCIndexMessage(ccode);
									req.setAccountRegion(USubjectService.getInstance()
											.getAccountRegion(ccode));
									Date dstime  = USubjectService.getInstance().getTradeIndexMinTime(ccode,StockConstants.INDEX_CODE_TRADE_S);
									//取时间大的
								   if(dstime!=null&&stime.compareTo(dstime)>0)
									   dstime = stime;
									if (stime == null)
										return null;
									doComputeHkBaseIndexOfCompany(req, dstime, etime);
								} catch (Exception e) {
									log.error("update all data failed!", e);
								}
								log.info("#****************************#complete compute company index ;companycode="
										+ ccode);
							} catch (Exception e) {
								log.error(
										"complete compute company index failed!companycode = "
												+ ccode, e);
							}
							finally{
								TaskSchedule.remove(pid, Thread.currentThread().getId());
							}
							return null;
						}});
			
		} catch (Exception e) {
			log.error("compute index failed!", e);
		}
		
		
	}
		private void docomputeCFRList_asyn(final String ccode, final List<Cfirule> crl, final Date stime, final Date etime) {
		try {
			//切记一定要放外面
			final Long pid = Thread.currentThread().getId();
			StockFactory.submitTaskBlocking(new Callable<String>(){

						@Override
						public String call() throws Exception {
							TaskSchedule.put(pid, Thread.currentThread().getId());
							try {
								// 先加载此公司的所有extindex数据
								// ComputeIndexManager.getInstance().loadOneCompanyExtIndex(ccode);
								log.info("#========================#start compute company index ;companycode="
										+ ccode);
								compute_notAysn(ccode, crl,stime, etime);
								log.info("#****************************#complete compute company index ;companycode="
										+ ccode);
							} catch (Exception e) {
								log.error(
										"complete compute company index failed!companycode = "
												+ ccode, e);
							}
							finally{
								TaskSchedule.remove(pid, Thread.currentThread().getId());
							}
							return null;
						}});
			
		} catch (Exception e) {
			log.error("compute index failed!", e);
		}
		
		
	}

	// 计算一个公司下的所有指标
	// private void computeCompanyOneIndex(String ccode, Cfirule r,
	// String interval, String tsc) {
	// try {
	// // 对基本指标进行计算,排除模板和中间指标
	// // if (computeIndexSet.contains(r.getType())) {
	// // log.debug("=========================================start crule: "
	// // + r.toString() + ";tsc=" + tsc);
	// // 计算公用指标,以及本公司的私有指标
	// if (StringUtil.isEmpty(r.getCompanyCode())
	// || r.getCompanyCode().equals(ccode)) {
	// // 计算属于本公司的所有指标
	// Company c = cs.getCompanyByCodeFromCache(ccode);
	// computeOneCompanyIndexByGiveRule(c, r, interval, tsc);
	// }
	//
	// // }
	// } catch (Exception e) {
	// log.error("compute index failed!", e);
	// }
	// }

	// 计算一个公司下的所有指标
	// private void computeCompanyOneIndex(String ccode, Cfirule r,
	// String interval, String tsc, Date stime, Date etime) {
	// try {
	// // 对基本指标进行计算,排除模板和中间指标
	// if (computeIndexSet.contains(r.getType())) {
	// log.debug("=========================================start crule: "
	// + r.toString() + ";tsc=" + tsc);
	// // 计算公用指标,以及本公司的私有指标
	// if (StringUtil.isEmpty(r.getCompanyCode())
	// || r.getCompanyCode().equals(ccode)) {
	// // 计算属于本公司的所有指标
	// Company c = cs.getCompanyByCodeFromCache(ccode);
	// if (c != null) {
	// try {
	// // 看要计算的公司有没有数据
	// if (hasDataInAsset(c.getCompanyCode(), tsc)) {
	// try {
	// Date sTime = stime;
	// Date eTime = etime;
	// IndexMessage req = SMsgFactory.getUDCIndexMessage(c
	// .getCompanyCode());
	// req.setAccountRegion(c.getAccountRegion());
	//
	// doComputeIndexOfCompany( req, sTime, eTime);
	// } catch (Exception e) {
	// // TODO: handle exception
	// log.error("update all data failed!", e);
	// }
	// }
	// } catch (Exception e) {
	// log.debug("compute the company indexRule failed," + c, e);
	// }
	// }
	//
	// }
	//
	// }
	// } catch (Exception e) {
	// log.error("compute index failed!", e);
	// }
	// }

	// // 取数据更新了的公司---此处只查看资产表中的各公司的数据是否更新了
	// public List<String> getConpanyListOfDateUpdate(String tsc,String mtime) {
	// List<String> cls = null;
	// try {
	// LCEnter lcEnter = LCEnter.getInstance();
	// Matchinfo mi = lcEnter.get(tsc + "." + StockConstants.TABLE_TYPE_0,
	// StockConstants.MATCH_INFO_CACHE);
	//
	// cls =
	// CompanyService.getInstance().getCompanyOfDataUpdateInAsset(mi.getSystemChildTableName(),mtime);
	// // return null;
	// } catch (Exception e) {
	// log.error("query company of data update failed!", e);
	// }
	// return cls;
	// }

	@SuppressWarnings({ "unused", "unchecked" })
	private void computeIndex(Cfirule r, String interval, String tsc) {

	}

	// 计算某个公司的某个指标
	// public String computeIndexOfCompany(Company c, Cfirule r) {
	//
	//
	// return StockCodes.SUCCESS;
	// }

	private void doComputeIndexOfCompany(IndexMessage req, Date stime,
			Date etime) {
		Dictionary d = DictService.getInstance().getDataDictionary(
				req.getIndexCode());
		Cfirule r = CRuleService.getInstance().getCfruleByCodeFromCache(
				req.getIndexCode());
		Date sd = DateUtil.getDayStartTime(stime);
		Date ed = etime;
		// 如果起始时间,小于结束时间
		while (sd.compareTo(ed) <= 0) {
			try {
				req.setTime(sd);
				// 计算指标
				Date acTime = IndexService.getInstance().formatTime(sd, d,
						req.getCompanyCode());
				if (acTime != null) {
					// 如果是行情周指标或月指标，当周或当月的不计算
					if (StockUtil.isTradeIndex(d.getTctype())) {
						if ((d.getTunit() == Calendar.DAY_OF_MONTH && DateUtil
								.getDayStartTime(acTime).compareTo(
										DateUtil.getDayStartTime(new Date())) == 0)
								|| (d.getTunit() == Calendar.WEEK_OF_MONTH && DateUtil
										.getWeekStartTime(acTime)
										.compareTo(
												DateUtil.getWeekStartTime(new Date())) == 0)
								|| (d.getTunit() == Calendar.MONTH && DateUtil
										.getMonthStartTime(acTime)
										.compareTo(
												DateUtil.getMonthStartTime(new Date())) == 0)) {
							sd = StockUtil.getNextTimeV3(sd,
									Integer.valueOf(d.getInterval()),
									d.getTunit());
							continue;
						}
					}
					req.setTime(acTime);
					// 如果为不跨期指标,则计算出会计区间
					// if (acrossType.equals(StockConstants.ACROSS_NO)) {
					// 计算当前会计区间
					if (!StockUtil.isTradeIndex(d.getTctype())) {
						String caRegion = StockUtil.computeCurAccountRegion(
								acTime, req.getAccountRegion(),
								req.getcAccountRegion());
						req.setcAccountRegion(caRegion);
					}

					// }
					StockFactory.expTime = System.currentTimeMillis();
					Double value = crs.computeIndex(req, r);
					if (value != null && value != 0) {
						req.setValue(value);
						req.setIndexCode(r.getcIndexCode());
						req.setColumnName(d.getColumnName());
						req.setTableName(d.getTableName());
						// 更新指标到数据库
						is.upateIndex2Db(d, req);
					}
				}

			} catch (Exception e) {
				log.error("computeIndexOfCompany failed!", e);
			}
			sd = StockUtil.getNextTimeV3(sd, Integer.valueOf(d.getInterval()),
					d.getTunit());
		}

	}

	private void doComputeHkBaseIndexOfCompany(IndexMessage req, Date stime,
			Date etime) {
		String uidentify  = req.getCompanyCode();
		USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(req.getCompanyCode());	
		if( usubject == null ){
			return;
		}
		Date sd = DateUtil.getDayStartTime(stime);
		Date ed = etime;
		Date publishDate = CompanyService.getInstance().getCompanyPulishTime(uidentify);
		// 如果起始时间,小于结束时间
		while (sd.compareTo(ed) <= 0) {
			try {
				Trade0001 t = new Trade0001();
				t.setCompanyCode(uidentify);
				t.setSecname(usubject.getName());
				t.setF001v("港交所");
				t.setTime(sd);
				if(IndexService.getInstance().isTradeDate(sd, req.getCompanyCode()))
				{
					Double zs = 0.0;//TODO 存在BUG， 2011-10-03 上一个交易日是2011-09-30 所有zs=null，引起后面抛出空指针异常
					
					if(sd.compareTo(publishDate) ==0){//第一天 昨收 改成今日开盘价
						zs = IndexValueAgent.getIndexValue(req.getCompanyCode(),
								StockConstants.INDEX_CODE_TRADE_K, sd);							 
					}else{
						Date pretime = IndexService.getInstance().getNextTrade(sd, Calendar.DAY_OF_MONTH, req.getCompanyCode(), -1);
						if(pretime!=null)
						{
							zs = IndexValueAgent.getIndexValue(req.getCompanyCode(),
									StockConstants.INDEX_CODE_TRADE_S, pretime);							
						}
					}
					if (zs != null ) {
						t.setF002n(zs.floatValue());		
					}else{//可能是TradeBitMap之前的数据有问题，引起第一天提前了
						log.error("计算任务异常 uidentify="+uidentify+",sd="+DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, sd)+",publishDate="+DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, publishDate));
						zs = IndexValueAgent.getIndexValue(req.getCompanyCode(),
								StockConstants.INDEX_CODE_TRADE_K, sd);		
					}
					Double v = IndexValueAgent.getIndexValue(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_S, sd);	
					if (v != null ) {
						t.setF007n(v.floatValue());
					}	
					else
					{
						sd = StockUtil.getNextTimeV3(sd, 1,
								Calendar.DAY_OF_MONTH);
						continue;
					}
					v = IndexValueAgent.getIndexValue(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_K, sd);	
					if (v != null ) {
						t.setF003n(v.floatValue());
					}
					v = IndexValueAgent.getIndexValue(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_CJL, sd);	
					if (v != null ) {
						t.setF004n(v.longValue());
					}
					v = IndexValueAgent.getIndexValue(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_ZG, sd);	
					if (v != null ) {
						t.setF005n(v.floatValue());
					}
					v = IndexValueAgent.getIndexValue(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_ZD, sd);				
					if (v != null ) {
						t.setF006n(v.floatValue());
					}
					
					v = IndexValueAgent.getIndexValue(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_CJE, sd);				
					if (v != null ) {
						t.setF012n(v.floatValue());
					}
					
					double c = t.getF007n();					
					float zdf = 0.0f;
					float zd = 0.0f;
					if(c != 0 && zs != 0 ){//新浪上的c=0是停牌
						zd = new Double(c-zs).floatValue();
						zdf = new Double((c - zs) / zs*100).floatValue();//今日涨幅					
					}
					t.setF010n(zd);
					t.setF011n(zdf);
					
//					v = RealTimeService.getInstance().realTimeComputeIndex(
//							req.getCompanyCode(), StockConstants.INDEX_CODE_TRADE_SD, sd);					
//					if (v != null ) {
//						t.setF010n(v.floatValue());
//					}
//					v = RealTimeService.getInstance().realTimeComputeIndex(
//							req.getCompanyCode(),StockConstants.INDEX_CODE_TRADE_ZDF, sd);
//					if (v != null ) {
//						t.setF011n(v.floatValue());
//					}
					
					v = RealTimeService.getInstance().realTimeComputeIndex(
							req.getCompanyCode(),StockConstants.INDEX_CODE_TRADE_5AVG, sd);	
					if (v != null && v != 0) {
						t.setF018n(new Double(v).floatValue());
					}
					v = RealTimeService.getInstance().realTimeComputeIndex(
							req.getCompanyCode(),StockConstants.INDEX_CODE_TRADE_10AVG, sd);
					if (v != null && v != 0) {
						t.setF019n(new Double(v).floatValue());
					}
					v = RealTimeService.getInstance().realTimeComputeIndex(
							req.getCompanyCode(),StockConstants.INDEX_CODE_TRADE_30AVG, sd);
					if (v != null && v != 0) {
						t.setF020n(new Double(v).floatValue());
					}
					
					v = HDaySumService.getInstance().computeDaySum(
							req.getCompanyCode(),StockConstants.INDEX_CODE_TRADE_CJL,sd,5);
					if (v != null && v != 0) {
						t.setF049n(new Double(v).floatValue());
					}		
					v = HDaySumService.getInstance().computeDaySum(
							req.getCompanyCode(),StockConstants.INDEX_CODE_TRADE_CJL,sd,10);
					if (v != null && v != 0) {
						t.setF050n(new Double(v).floatValue());
					}
					v = HDaySumService.getInstance().computeDaySum(
							req.getCompanyCode(),StockConstants.INDEX_CODE_TRADE_CJL,sd,30);
					if (v != null && v != 0) {
						t.setF051n(new Double(v).floatValue());
					}
					v = HDaySumService.getInstance().computeDaySum(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_CJE,sd,5);
					if (v != null && v != 0) {
						t.setF052n(new Double(v).floatValue());
					}		
					v = HDaySumService.getInstance().computeDaySum(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_CJE,sd,10);
					if (v != null && v != 0) {
						t.setF053n(new Double(v).floatValue());
					}
					v = HDaySumService.getInstance().computeDaySum(req.getCompanyCode(),
							StockConstants.INDEX_CODE_TRADE_CJE,sd,30);
					if (v != null && v != 0) {
						t.setF054n(new Double(v).floatValue());
					}
					
					// 改为指量异步更新
					ExecuteQueueManager.add2IQueue(
							new BatchQueueEntity(StockConstants.TYPE_Update_TRADE0001,
									"v_trade0001_hk", t));
				}
			} catch (Exception e) {
				log.error("computeIndexOfCompany failed!", e);
			}
			sd = StockUtil.getNextTimeV3(sd, 1,
					Calendar.DAY_OF_MONTH);
		}

	}
	public void compute_notAysn(String ccode, List<Cfirule> crl,Date stime,
			Date etime) {
		for (Cfirule r : crl) {
			try {
				compute_notAysn(ccode, r,stime, etime);
			} catch (Exception e) {
				log.error("compute index failed!", e);
			}
		}

	}

	// 检查一个公司从"2007-06-30"---->"2011-06-30"是否有数据,如果没有,则认为数据库中暂时没有此公司的数据,则不对该公司的指标进行计算.
	public boolean hasDataInAsset(String companyCode, String tsc) {
		if (tsc == null) {
			log.info("tsc is null!companycode=" + companyCode);
			return false;
		}
		if (companyCode.trim().startsWith("t")) {
			log.info("没有上市的公司代码!companycode=" + companyCode);
			return false;
		}
		String key = companyCode.trim() + "." + tsc.trim();
		Date maxd = IndexService.getInstance().getMaxTime(companyCode);
		Date mind = IndexService.getInstance().getMinTime(companyCode);
		// 判断此公司是不是在没有数据的公司的集合中
		if (_noDCSet.get(key) == null) {
			// 判断是否在有数据的公司的集合
			if (_havaDCSet.get(key) == null) {
				// 多个线程同步,减少检查公司有没有数据的次数
				synchronized (lock) {
					// 二次检查
					if (_noDCSet.get(key) == null) {
						// 判断是否在有数据的公司的集合
						if (_havaDCSet.get(key) == null) {
							// =======================
							Matchinfo mi = LCEnter.getInstance().get(
									StockUtil.getMatchInfoKey(tsc,
											StockConstants.TABLE_TYPE_0),
									StockConstants.MATCH_INFO_CACHE);

							String assetTableName = mi
									.getSystemChildTableName();
							Boolean hasdata = ass.hasAssetData(mind, maxd,
									companyCode, assetTableName);
							if (!hasdata) {
								// 把公司放入没有数据的集合
								_noDCSet.put(key, key);
								log.debug("the company has not have data,add to nodataCompanySet ;companyCode "
										+ companyCode + ";tsc :" + tsc);
								return false;
							} else {
								_havaDCSet.put(key, key);
								return true;
							}
							// ==========================
						} else {
							return true;
						}
					}
				}
			} else {
				// 在有数据的公司的集合中,则认为有数据
				return true;
			}

		}
		return false;
	}

	public static void clearNoDcSet() {
		_noDCSet.clear();
		_havaDCSet.clear();
	}




	public int isAccord(String companycode, Date time, String rule,
			int indextype) {
		IndexMessage req = SMsgFactory.getUDCIndexMessage(companycode);
		;
		req.setTime(time);
		req.setNeedUseExtDataCache(true);
		req.setNeedAccessExtRemoteCache(true);
		req.setNeedComput(true);
		req.setNeedAccessCompanyBaseIndexDb(false);
		req.setNeedAccessExtIndexDb(false);
		Double d = CRuleService.getInstance()
				.computeIndex(rule, req, indextype);
		if (d == null)
			d = -1.0;
		return d.intValue();
	}

	public void compute_notAysn(String ccode, Cfirule r,Date stime,
			Date etime) {
		// if (DictService.getInstance().isNotSaveOrNotCompute(
		// r.getcIndexCode()))
		// return;
		// // 如果是行业相关指标，又不计算
		Dictionary d = DictService.getInstance().getDataDictionary(
				r.getcIndexCode());
		// if (DictUtil.isIndustryXGIndex(d)) {
		// if (!ComputeIndexManager.computeIndustryRelatIndex)
		// return;
		// }

		try {
			if (StringUtil.isEmpty(r.getCompanyCode())
					|| r.getCompanyCode().equals(ccode)) {
				// 计算属于本公司的所有指标
				USubject c = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(ccode);
				if (c == null)
					return;
				// 非行情指标
				if (!StockUtil.isTradeIndex(r.getType())) {
					if (!c.getTscs().equals(r.getTableSystemCode()))
						return;
				}

				if (c.getUidentify().startsWith("t"))
					return;
				// 看要计算的公司有没有数据
				// if (hasDataInAsset(c.getCompanyCode(), c.getTsc())) {
				try {

					try {
						IndexMessage req = SMsgFactory.getUDCIndexMessage(c
								.getUidentify());
						req.setAccountRegion(USubjectService.getInstance()
								.getAccountRegion(ccode));
						stime = getComputeStime(c, d,stime);
						if (stime == null)
							return;
						req.setIndexCode(d.getIndexCode());
						// System.out.println("--stime="+stime+";etime="+etime);
						doComputeIndexOfCompany(req, stime, etime);
					} catch (Exception e) {
						// TODO: handle exception
						log.error("update all data failed!", e);
					}
				} catch (Exception e) {
					log.debug("compute the company indexRule failed," + c, e);
				}
				// }
			}

			// }
		} catch (Exception e) {
			log.error("compute index failed!", e);
		}

	}




	// 只计算更新的指标
	public void computeOneCompanyNewIndex(Company c, String tsc,
			boolean istradeindex,Date stime,Date etime) {
		if (CompanyService.getInstance().needRemoveB(c))
			return;
		// 取当天所有新加入到库中的指标
		List<Cfirule> crl = crs.getRuleListFromDb(DateUtil.getSysDate(
				DateUtil.YYYYMMDD, Calendar.getInstance().getTime()),
				istradeindex);
		if (crl == null) {
			log.debug("not get the new rule ;return ");
			return;
		}
		List<Cfirule> ncrl = new ArrayList<Cfirule>();
		for (Cfirule cr : crl) {
			if (DictService.getInstance().isNotSaveOrNotCompute(
					cr.getcIndexCode()))
				continue;
			Dictionary d = DictService.getInstance().getDataDictionary(
					cr.getcIndexCode());
			if (DictUtil.isIndustryXGIndex(d)) {
				if (!ComputeIndexManager.computeIndustryRelatIndex)
					continue;
			}
			ncrl.add(cr);
		}
		try {
			docomputeCFRList_asyn(c.getCompanyCode(), ncrl, stime, etime);
		} catch (Exception e) {
			log.error("submit task failed!", e);
		}
	}

	// 计算此公司的所有指标
	public void computeOneCompanyAllIndex(Company c, String interval, String tsc,Date stime,Date etime) {
		if (CompanyService.getInstance().needRemoveB(c))
			return;
		// 取所有的指标
		List<Cfirule> crl = CRuleService.getInstance().getCfiruleListFromCache(
				tsc);
		if (crl == null) {
			log.debug("not get the new rule ;return ");
			return;
		}

		try {
			docomputeCFRList_asyn(c.getCompanyCode(), crl, stime, etime);
		} catch (Exception e) {
			log.error("submit task failed!", e);
		}
	}

	// 按公司逐个计算，只计算更新的指标
	public void computeNewIndexByCompany(boolean istradeindex,Date stime,Date etime) {
		try {
			ComputeIndexManager.getInstance().computeInit();
			// 取所有报表体系相关的指标
			LCEnter lcEnter = LCEnter.getInstance();
			List<String> tsl = lcEnter.get(
					StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
					StockConstants.MATCH_INFO_CACHE);
			if (tsl == null) {
				log.error(
						"load table system code list failed!",
						new StockException(
								Integer.valueOf(StockCodes.LOAD_TABLE_SYSTEM_CODE_ERROR),
								"load table system code list failed"));

				return;
			}
			for (String tsc : tsl) {
				List<Company> cl = getAllCompany();
				for (Company c : cl) {
					if (hasDataInAsset(c.getCompanyCode(), tsc)) {
						computeOneCompanyNewIndex(c, tsc, istradeindex,stime,etime);
					}
				}
			}
		} catch (Exception e) {
			log.error("compute index failed!", e);

		}

	}

	int curpage;

	// 按公司逐个计算，只计算更新的指标
	public void computeAllIndexByPageCompany(int page, boolean isasyn,
			boolean istradeindex, List<USubject> usl,Date stime,Date etime) {
		try {
			List<USubject> cl = getUSubjectListByPage(page, usl);
			if (cl == null)
				return;
			System.out
					.println("["
							+ new Date()
							+ "]=========start compute current page company all ext index data!page="
							+ page + "=============== ");
			TUExtCacheLoadService.getInstance().clearAllExtCache();
			// 现在每次都是全量指标算，所以取掉加载公司原有指标数据的代码
			// loadCurPageCompanysData2Cache(cl);
			// 非实时运算中心需要加载数据，实时运算中心，不需要加载
			String isRealtimer = BaseProperties
					.getWebXmlProperties("isrealtimer");
			if (istradeindex && "0".equals(isRealtimer))
				Trade0001CacheLoadService.getInstance()
						.loadCurpageCompanysTrade(cl,stime);

			List<Cfirule> crl = crs.getRuleListFromDb(DateUtil.getSysDate(
					DateUtil.YYYYMMDD, Calendar.getInstance().getTime()),
					istradeindex);
			if (crl == null) {
				log.debug("not get the new rule ;return ");
				return;
			}
			String notComputeIndexs = ConfigCenterFactory
					.getString(
							"stock_dc.notComputeIndexs",
							"2348,2349,2350,2351,2352,2354,3015,3016,3017,3249,3584,3585,3586,3587,3588,3589,3609,3805,3806,3807,3808,3809,3810,3811,4120,4121,4122,4123,4124,4125,4126,4439,4515,4516,4517,4519,4559,4561,4562,4588,4591,4649,4669,4670,4671,4672,4673,");
			// 去掉不计算的指标
			List<Cfirule> ncrl = new ArrayList<Cfirule>();
			for (Cfirule cr : crl) {
				if (DictService.getInstance().isNotSaveOrNotCompute(
						cr.getcIndexCode()))
					continue;
				Dictionary d = DictService.getInstance().getDataDictionary(
						cr.getcIndexCode());
				if (DictUtil.isIndustryXGIndex(d)) {
					if (!ComputeIndexManager.computeIndustryRelatIndex)
						continue;
				}
				if (notComputeIndexs.contains(cr.getcIndexCode()))
					continue;
				ncrl.add(cr);
			}
			for (USubject c : cl) {
				if (c.getUidentify().startsWith("t"))
					continue;
				// 港股暂时不计算财务指标
				if (!istradeindex) {
					Company cc = CompanyService.getInstance()
							.getCompanyByCodeFromCache(c.getUidentify());
					if (cc == null
							|| !RuleTimerTask.getInstance().hasDataInAsset(
									cc.getCompanyCode(), cc.getTsc()))
						continue;
				}
				IndexService.getInstance().rebuildMaxMinTimeCache(
						c.getUidentify());
				try {
					if (isasyn)
						docomputeCFRList_asyn(c.getUidentify(), ncrl,stime,etime);
					else
						compute_notAysn(c.getUidentify(), ncrl,stime, etime);
				} catch (Exception e) {
					log.error("submit task failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("compute index failed!", e);

		}

	}
	
	public void computeHKBaseIndexByPageCompany(int page, List<USubject> usl,Date stime,Date etime) {
		try {
			List<USubject> cl = getUSubjectListByPage(page, usl);
			if (cl == null)
				return;
			System.out
					.println("["
							+ new Date()
							+ "]=========start compute current page company all ext index data!page="
							+ page + "=============== ");
			TUExtCacheLoadService.getInstance().clearAllExtCache();

			Trade0001CacheLoadService.getInstance()
						.loadCurpageCompanysTrade(cl,stime);
			
			for (USubject c : cl) {
				docomputeCFRList_asynHkBaseIndex(c.getUidentify(),stime,etime);
			}
		} catch (Exception e) {
			log.error("compute index failed!", e);

		}

	}

	// public void computeAllIndexByPageCompany_notAysn(int page) {
	// try {
	// List<Company> cl = getCompanyListByPage(page);
	// if (cl == null)
	// return;
	// System.out
	// .println("["
	// + new Date()
	// + "]=========start compute current page company all ext index data!page="
	// + page + "=============== ");
	// clearAllExtCache();
	// // 现在每次都是全量指标算，所以取掉加载公司原有指标数据的代码
	// // loadCurPageCompanysData2Cache(cl);
	// Trade0001CacheLoadService.getInstance()
	// .loadCurpageCompanysTrade(cl);
	//
	// List<Cfirule> crl = crs.getCfRuleListFromDb(DateUtil.getSysDate(
	// DateUtil.YYYYMMDD, Calendar.getInstance().getTime()));
	// if (crl == null) {
	// log.debug("not get the new rule ;return ");
	// return;
	// }
	// // 去掉不计算的指标
	// List<Cfirule> ncrl = new ArrayList<Cfirule>();
	// for (Cfirule cr : crl) {
	// if (DictService.getInstance().isNotSaveOrNotCompute(
	// cr.getcIndexCode()))
	// continue;
	// ncrl.add(cr);
	// }
	// for (Company c : cl) {
	// IndexService.getInstance().rebuildMaxMinTimeCache(
	// c.getCompanyCode());
	// try {
	// compute_notAysn( c.getCompanyCode(), crl) ;
	// } catch (Exception e) {
	// log.error("submit task failed!", e);
	// }
	// }
	// } catch (Exception e) {
	// log.error("compute index failed!", e);
	//
	// }
	//
	// }

	public void computeIndIndexByTag(String tag, boolean istradeindex,Date stime,Date etime) {
		try {
			// ComputeIndexManager.getInstance().computeInit();
			List<Company> cl = CompanyService.getInstance()
					.getCompanyListByTagFromCache(tag);
			if (cl == null)
				return;
			List<USubject> usl = USubjectService.getInstance()
					.ConvertCList2UList(cl);
			System.out
					.println("=========start compute current page company all ext index data!tag="
							+ tag + "=============== ");
			TUExtCacheLoadService.getInstance().clearAllExtCache();
			loadCurPageCompanysData2Cache(cl);

			if (istradeindex)
				Trade0001CacheLoadService.getInstance()
						.loadCurpageCompanysTrade(usl,stime);

			List<Cfirule> crl = crs.getRuleListFromDb(DateUtil.getSysDate(
					DateUtil.YYYYMMDD, Calendar.getInstance().getTime()),
					istradeindex);
			if (crl == null) {
				log.debug("not get the new rule ;return ");
				return;
			}
			// 去掉不计算的指标
			List<Cfirule> ncrl = new ArrayList<Cfirule>();
			for (Cfirule cr : crl) {
				if (DictService.getInstance().isNotSaveOrNotCompute(
						cr.getcIndexCode()))
					continue;
				Dictionary d = DictService.getInstance().getDataDictionary(
						cr.getcIndexCode());
				if (DictUtil.isIndustryXGIndex(d)) {
					if (!ComputeIndexManager.computeIndustryRelatIndex)
						continue;
				}
				ncrl.add(cr);
			}

			for (Company c : cl) {
				try {
					// 港股暂时不计算财务指标
					if (!istradeindex
							&& !RuleTimerTask.getInstance().hasDataInAsset(
									c.getCompanyCode(), c.getTsc()))
						continue;
					docomputeCFRList_asyn(c.getCompanyCode(), ncrl, stime, etime);
				} catch (Exception e) {
					log.error("submit task failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("compute index failed!", e);

		}

	}

	

	/**
	 * 计算某一公司的所有指标
	 * 
	 * @param c
	 * @param interval
	 */
	// private void computeAllIndexByPageCompany(Company c, String
	// interval,List<Cfirule> ncrl) {
	// try {
	// StockFactory.submitTaskBlocking(new TaskComputeRule_V5(c
	// .getCompanyCode(), ncrl, interval, c.getTsc()));
	// } catch (Exception e) {
	// log.error("submit task failed!", e);
	// }
	// }

	// private void computeAllIndexByPageCompany_notAysn(Company c, String
	// interval) {
	//
	//
	// }

	private List<USubject> getUSubjectListByPage(int page, List<USubject> usl) {

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

	public void loadCurPageCompanysData2Cache(List<Company> cl) {
		System.out
				.println("start load current page company all ext index data! ");
		for (Company c : cl) {
			System.out
					.println("========loading current page company all ext index data! company:"
							+ c.getSimpileName());
			StockFactory.expTime = Calendar.getInstance().getTimeInMillis();
			ComputeIndexManager.getInstance().loadOneCompanyExtIndex(
					c.getCompanyCode());
		}
		System.out
				.println("finish load current page company all ext index data! ");
	}

	// 按公司逐个计算，只计算更新的指标
	public void computeAllIndexByCompany(String interval,Date stime,Date etime) {
		try {
			// ComputeIndexManager.getInstance().computeInit();
			// 取所有报表体系相关的指标
			LCEnter lcEnter = LCEnter.getInstance();
			List<String> tsl = lcEnter.get(
					StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
					StockConstants.MATCH_INFO_CACHE);
			if (tsl == null) {

				log.error(
						"load table system code list failed!",
						new StockException(
								Integer.valueOf(StockCodes.LOAD_TABLE_SYSTEM_CODE_ERROR),
								"load table system code list failed"));

				return;
			}
			for (String tsc : tsl) {
				List<Company> cl = getAllCompany();
				for (Company c : cl) {
					if (hasDataInAsset(c.getCompanyCode(), tsc)) {
						computeOneCompanyAllIndex(c, interval, tsc,stime,etime);
					}
				}
			}
		} catch (Exception e) {
			log.error("compute index failed!", e);

		}

	}

	public void computeOneCompanyOneIndex(USubject c, String indexcode) {
		Cfirule r = crs.getCIndexRuleByCode(indexcode);
		Dictionary d = DictService.getInstance().getDataDictionary(
				r.getcIndexCode());
		if (StockUtil.isTradeIndex(d.getTctype())) {
			List<USubject> cl = new ArrayList<USubject>();
			cl.add(c);
			Trade0001CacheLoadService.getInstance()
					.loadCurpageCompanysTrade(cl,null);
		} else {
			ComputeIndexManager.getInstance().loadOneCompanyExtIndex(
					c.getUidentify());
		}
		// 港股暂时不计算财务指标
		if (!StockUtil.isTradeIndex(d.getTctype())
				&& c.getUidentify().endsWith(".hk"))
			return;
		try {
			IndexMessage req = SMsgFactory.getUDCIndexMessage(c.getUidentify());
			req.setAccountRegion(USubjectService.getInstance()
					.getAccountRegion(c.getUidentify()));

			Date stime = getComputeStime(c, d,DateUtil.format("1990-01-01"));
			if (stime == null)
				return;
			Date etime = IndexService.getInstance().getComputeEtime(c, d);
			req.setIndexCode(d.getIndexCode());
			doComputeIndexOfCompany(req, stime, etime);
		} catch (Exception e) {
			// TODO: handle exception
			log.error("update all data failed!", e);
		}

	}

	public void computeIndexOfCompanyDataUpdate(Date uptime) {
		// 取所有报表体系相关的指标
		LCEnter lcEnter = LCEnter.getInstance();
		List<String> tsl = lcEnter.get(
				StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
				StockConstants.MATCH_INFO_CACHE);
		if (tsl == null) {

			log.error(
					"load table system code list failed!",
					new StockException(Integer
							.valueOf(StockCodes.LOAD_TABLE_SYSTEM_CODE_ERROR),
							"load table system code list failed"));
			return;
		}
		for (String tsc : tsl) {
			// 扫描数据库,取出所有数据更新的公司
			List<String> cList = CompanyService.getInstance()
					.getConpanyListOfDateUpdate(tsc, uptime);
			if (cList == null || cList.size() == 0) {
				return;
			}
			for (String ccode : cList) {
				Company c = CompanyService.getInstance()
						.getCompanyByCodeFromCache(ccode);
				if (CompanyService.getInstance().needRemoveB(c))
					continue;
				// 取所有的指标
				List<Cfirule> crl = crs.getCfiruleListFromCache(tsc);
				if (crl == null) {
					log.debug("not get the new rule ;return ");
					return;
				}
				try {
					// 根据更新时间，取指标的时间区间
					Date stime = IndexService.getInstance().getMinTime(ccode,
							uptime);
					Date etime = IndexService.getInstance().getMaxTime(ccode);
					StockFactory.submitTaskBlocking(docomputeCFRDataUpdate(
							ccode, crl, "3", tsc, stime, etime));
				} catch (Exception e) {
					log.error("compute index failed!", e);
				}
			}
		}

	}

	private Object docomputeCFRDataUpdate(final String ccode, final List<Cfirule> crl,
			String string, String tsc, final Date stime, final Date etime) {
		//切记一定要放外面
		final Long pid = Thread.currentThread().getId();
		StockFactory.submitTaskBlocking(new Callable<String>(){

			@Override
			public String call() throws Exception {
				TaskSchedule.put(pid, Thread.currentThread().getId());
				try {
					// 先加载此公司的所有extindex数据
					ComputeIndexManager.getInstance().loadOneCompanyExtIndex(ccode);
					log.info("#========================#start compute company index ;companycode="
							+ ccode);
					compute_notAysn(ccode, crl,stime, etime);
					log.info("#*************************#complete compute company index ;companycode="
							+ ccode);
				} catch (Exception e) {
					log.error(
							"complete compute company index failed!companycode = "
									+ ccode, e);
				}
				finally{
					TaskSchedule.remove(pid, Thread.currentThread().getId());
				}
				return StockCodes.SUCCESS;
			}
			
		});
		return null;
	}

	public void batchAddCfTag2CompanyByPage(String interval, int page,
			List<Tagrule> crl, boolean loadTradeData, List<USubject> usl) {
		// TODO Auto-generated method stub
		try {
			if (crl == null) {
				log.info("not get the new rule ;return ");
				return;
			}
			List<USubject> cl = getUSubjectListByPage(page, usl);
			if (cl == null)
				return;
			System.out
					.println("=========start compute current page company all ext index data!page="
							+ page + "=============== ");
			TUExtCacheLoadService.getInstance().clearAllExtCache();
			// loadCurPageCompanysData2Cache(cl);
			if (loadTradeData)
				Trade0001CacheLoadService.getInstance()
						.loadCurpageCompanysTrade(cl,null);
			for (USubject us : cl) {
				Company c = CompanyService.getInstance()
						.getCompanyByCodeFromCache(us.getUidentify());
				if (c.getCompanyCode().startsWith("t"))
					continue;
				IndexService.getInstance().rebuildMaxMinTimeCache(
						c.getCompanyCode());
				Date time = CompanyService.getInstance().getLatestReportTime(
						c.getCompanyCode());
				// if (hasDataInAsset(c.getCompanyCode(), c.getTsc())) {
				AddCfTag2OneCompanyByPage(c, interval, time, crl);
				// }
			}
		} catch (Exception e) {
			log.error("compute index failed!", e);

		}
	}

	public void batchAddCfTag2CompanyBySet(String interval, List<Tagrule> crl,
			boolean loadTradeData, List<USubject> cl) {
		// TODO Auto-generated method stub
		try {
			if (loadTradeData)
				Trade0001CacheLoadService.getInstance()
						.loadCurpageCompanysTrade(cl,null);
			for (USubject s : cl) {
				Company c = CompanyService.getInstance()
						.getCompanyByCodeFromCache(s.getUidentify());
				if (c.getCompanyCode().startsWith("t"))
					continue;
				IndexService.getInstance().rebuildMaxMinTimeCache(
						c.getCompanyCode());
				Date time = CompanyService.getInstance().getLatestReportTime(
						c.getCompanyCode());
				if (hasDataInAsset(c.getCompanyCode(), c.getTsc())) {
					AddCfTag2OneCompanyByPage(c, interval, time, crl);
				}
			}
		} catch (Exception e) {
			log.error("compute index failed!", e);

		}
	}

	public void AddCfTag2OneCompanyByPage(final Company c, String interval,
			final Date time, final List<Tagrule> crl) {
		try {
			//切记一定要放外面
			final Long pid = Thread.currentThread().getId();
			StockFactory.submitTaskBlocking(new Callable<String>(){

				@Override
				public String call() throws Exception {
					TaskSchedule.put(pid, Thread.currentThread().getId());
					try {
						if(hasDataInAsset(c.getCompanyCode(), c.getTsc()))
						{
							log.info("compute company cf tag,company:" + c.getSimpileName());
							StringBuilder sb = new StringBuilder();
							for (Tagrule r : crl) {
								// 对于新股去掉估值类标签
								if (CompanyService.getInstance().isNewStockV2(c)) {
									String removeTags = ConfigCenterFactory
											.getString(
													"stock_zjs.new_stock_remove_tags",
													"263;756;754;755;288;286;287;383;411;409;262;408;1220;1221;406;414;412;752;750;751;291;810");
									if (removeTags.indexOf(r.getId() + ";") >= 0)
										continue;
								}
								if (c == null || !c.getTsc().equals(r.getTsc()))
									continue;
								StockFactory.expTime = Calendar.getInstance()
										.getTimeInMillis();
								String tag = StockUtil.getCfTagId(r.getId());
								if (c.containsTheTag(r.getIndustryTag())
										&& TagruleService.getInstance().isAccord(
												c.getCompanyCode(), time, r) > 0) {
									sb.append(tag);
									sb.append(";");
									// System.out.println("========test : companyname:"+c.getSimpileName()+";cftag:"+r.getTagDesc());
								}
							}
							// //一次性更新
							// CompanyService.getInstance().addTags2Company(
							// c.getCompanyCode(), sb.toString());
							// 把公司对应的财务标签写到临时Map中
							if (!StringUtil.isEmpty(sb.toString()))
								_cfMap.put(c.getCompanyCode(), sb);
						}
						
					} catch (Exception e) {
						log.error(
								"complete compute company index failed!companycode = "
										+ c.getSimpileName(), e);
					}
					finally{
						TaskSchedule.remove(pid, Thread.currentThread().getId());
					}
					
					return StockCodes.SUCCESS;
				}
				
			});
		} catch (Exception e) {
			log.error("submit task failed!", e);
		}
	}

	public Date getComputeStime(USubject c, Dictionary d, Date dstime) {
		Date stime = null;
		if (d.getTctype() == StockConstants.TRADE_TYPE) {
			stime  = USubjectService.getInstance().getTradeIndexMinTime(c.getUidentify(),StockConstants.INDEX_CODE_TRADE_S);
		} else {
			stime = IndexService.getInstance().getMinTime(c.getUidentify());
		}
		if (stime == null)
			return dstime;
		else
		{
			//取时间大的
			if(stime.compareTo(dstime)>0)
				return stime;
		}
		return dstime;
	}

	/**
	 * 0：全量更新 1：追加
	 * 
	 * @param type
	 */
	public void flushAllCfTagResult(int type) {
		Map<String, StringBuilder> rm = new HashMap<String, StringBuilder>();
		Iterator<String> iter = (Iterator<String>) _cfMap.keySet().iterator();
		while (iter.hasNext()) {
			String companycode = iter.next();
			String cfs = _cfMap.get(companycode).toString();
			if (!StringUtil.isEmpty(cfs)) {
				String[] cfa = cfs.split(";");
				for (String cf : cfa) {
					StringBuilder sb = rm.get(cf);
					if (sb == null) {
						sb = new StringBuilder();
						rm.put(cf, sb);
					}
					sb.append(companycode);
					sb.append(";");
				}

			}
		}
		//如果是全量则，清除所有财务标签
		if(type==0)
			StockPoolService.getInstance().deleteStockpoolByType(0);
		Iterator<String> iters = (Iterator<String>) rm.keySet().iterator();
		while (iters.hasNext()) {
			try {
				String cf = iters.next();
				String companycodes = rm.get(cf).toString();
				Stockpool sp = new Stockpool();
				sp.setName(cf);
				sp.setType(Stockpool.type_0);
				sp.setPool(companycodes);
				log.info("========================flush the cftag " + cf
						+ " to db=========================");
				if (type == 0) {
					StockPoolService.getInstance().modifyStockpool(sp);
				} else {
					StockPoolService.getInstance().append2Stockpool(sp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void clearTagResult() {
		_cfMap.clear();
	}

}
