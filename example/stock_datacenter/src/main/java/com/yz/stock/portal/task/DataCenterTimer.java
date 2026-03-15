package com.yz.stock.portal.task;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.CompileMode;
import com.stock.common.model.USubject;
import com.stock.common.model.company.Stock0001;
import com.stock.common.util.CapUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.Stock0001Service;
import com.yfzx.service.db.SystemPropertiesService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.plug.PlugManager;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.portal.cache.TUExtCacheLoadService;
import com.yz.stock.portal.manager.ComputeIndexManager;

public class DataCenterTimer {

	Logger log = LoggerFactory.getLogger(this.getClass());
	static DataCenterTimer instance = new DataCenterTimer();

	public DataCenterTimer() {

	}

	public static DataCenterTimer getInstance() {
		return instance;
	}

	static boolean isInit = true;

	public void datacompute() {
		// 初次加载不运行
		if (isInit) {
			isInit = false;
			return;
		}
		
		// 两天之内有更新的公司
		Date uptime = StockUtil.getNextTimeV3(new Date(), -2, Calendar.DATE);
		uptime = DateUtil.getDayStartTime(uptime);
		init(uptime);
		Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
		Date etime = new Date();
//		computeIndexOfCompanyDataUpdate_notAysn(uptime);
		computeIndexOfCompanyDataUpdate(uptime,stime,etime);
		// computeRAvgOnetimeByPage();
		// caprankcomputeChildIndexOneTime();
		// caprankcomputeZHNLOneTime();
		batchAddCfTag_CompanyDateUpdate();
		sendMail(null);
		Calendar nc = Calendar.getInstance();
		nc.setTimeInMillis(System.currentTimeMillis());
		// 运算结束后，把最后一次开始运算时间写到库中
		SystemPropertiesService.getInstance().update("last_compute_date",
				DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));

	}

	public void datacompute_cron_cf() {
		
		// 两天之内有更新的公司
		Date uptime = StockUtil.getNextTimeV3(new Date(), -2, Calendar.DATE);
		uptime = DateUtil.getDayStartTime(uptime);
		init(uptime);
		Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
		Date etime = new Date();
//		computeIndexOfCompanyDataUpdate_notAysn(uptime);
		computeIndexOfCompanyDataUpdate(uptime,stime,etime);
		//计算行情数据
//		computeTradeIndex_add(uptime);
		// computeRAvgOnetimeByPage();
		// caprankcomputeChildIndexOneTime();
		// caprankcomputeZHNLOneTime();
		batchAddCfTag_CompanyDateUpdate();
		sendMail(null);
		Calendar nc = Calendar.getInstance();
		nc.setTimeInMillis(System.currentTimeMillis());
		// 运算结束后，把最后一次开始运算时间写到库中
		SystemPropertiesService.getInstance().update("last_compute_date",
				DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));

	}

	public void datacompute_cron_trade() {
		inittrade();
		// 两天之内有更新的公司
		Date uptime = StockUtil.getNextTimeV3(new Date(), -5, Calendar.DATE);
		uptime = DateUtil.getDayStartTime(uptime);
		Date etime = new Date();
//		computeIndexOfCompanyDataUpdate_notAysn(uptime);
//		computeIndexOfCompanyDataUpdate(uptime);
		//计算行情数据
		computeTradeIndex_add(uptime,uptime,etime);
		// computeRAvgOnetimeByPage();
		// caprankcomputeChildIndexOneTime();
		// caprankcomputeZHNLOneTime();
//		batchAddCfTag_CompanyDateUpdate();
//		sendMail(null);
//		Calendar nc = Calendar.getInstance();
//		nc.setTimeInMillis(System.currentTimeMillis());
//		// 运算结束后，把最后一次开始运算时间写到库中
//		SystemPropertiesService.getInstance().update("last_compute_date",
//				DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));

	}
	
	public void datacomputeFromTime(Date uptime) {
		init(uptime);
		uptime = DateUtil.getDayStartTime(uptime);
		Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
		Date etime = new Date();
//		computeIndexOfCompanyDataUpdate_notAysn(uptime);
		computeIndexOfCompanyDataUpdate(uptime,stime,etime);
//		//计算行情数据
//		computeTradeIndex_add(uptime);
		// computeRAvgOnetimeByPage();
		// caprankcomputeChildIndexOneTime();
		// caprankcomputeZHNLOneTime();
		batchAddCfTag_CompanyDateUpdate();
		sendMail(null);
		Calendar nc = Calendar.getInstance();
		nc.setTimeInMillis(System.currentTimeMillis());
		// 运算结束后，把最后一次开始运算时间写到库中
		SystemPropertiesService.getInstance().update("last_compute_date",
				DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));

	}
	
	public void datacomputeCfFromTime(Date uptime) {
		init(uptime);
		uptime = DateUtil.getDayStartTime(uptime);
		Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
		Date etime = new Date();
//		computeIndexOfCompanyDataUpdate_notAysn(uptime);
		computeIndexOfCompanyDataUpdate(uptime,stime,etime);

		sendMail(null);
		Calendar nc = Calendar.getInstance();
		nc.setTimeInMillis(System.currentTimeMillis());
		// 运算结束后，把最后一次开始运算时间写到库中
		SystemPropertiesService.getInstance().update("last_compute_date",
				DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));

	}
	
	public void datacomputeTradeFromTime(Date uptime) {
		inittrade();
		uptime = DateUtil.getDayStartTime(uptime);
		//计算行情数据
		computeTradeIndex_add(uptime,uptime,new Date());
		sendMail(null);
		Calendar nc = Calendar.getInstance();
		nc.setTimeInMillis(System.currentTimeMillis());
		// 运算结束后，把最后一次开始运算时间写到库中
		SystemPropertiesService.getInstance().update("last_compute_date",
				DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));

	}
	public void datacompute_auto(Date uptime) {
		init(uptime);
		Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
		Date etime = new Date();
		computeIndexOfCompanyDataUpdate(uptime,stime,etime);
		//计算行情数据
//		computeTradeIndex_add(uptime);
		// computeRAvgOnetimeByPage();
		// caprankcomputeChildIndexOneTime();
		// caprankcomputeZHNLOneTime();
		batchAddCfTag_CompanyDateUpdate();
		sendMail(uptime);

	}

	private void sendMail(Date uptime) {
		if (uptime == null)
			uptime = StockUtil.getNextTimeV3(new Date(), -1, Calendar.DATE);
		TaskEnter.getInstance().sendMail(uptime);
		Long sleepTime = ConfigCenterFactory.getLong("stock_dc.cf_rule_compute_notifyUser_waitetime", 600000l);
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CompanyService.getInstance().notifyUserCfUpdate();
	}

	/*
	 * 计算数据有更新的公司
	 */
	public void computeIndexOfCompanyDataUpdate(Date uptime,Date stime,Date etime) {

		try {
			System.out
					.println("["
							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
									new Date())
							+ "]**************************************company data update timer start...**************************");
			CRuleService.getInstance().updateIndexRuleOfCompute();
			// 把最新更新的数据导入正式库
			TaskEnter.getInstance().importNewData2NormalDb(uptime);
//			ComputeIndexManager.getInstance().computeInit();

			CompileMode.setUseCacheExtData(true);
//			List<Company> cl = CompanyService.getInstance()
//					.getDataUpCompanyList(uptime);
			List<USubject> usl = USubjectService.getInstance().getDataUpUSubjectList(uptime);
			if (usl == null || usl.size() == 0) {
				System.out
						.println("["
								+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
										new Date())
								+ "]**************************************update company list is null,uptime:"
								+ uptime);
				return;
			}
			System.out.println("数据有更新的公司列表："+TaskEnter.getInstance().buildMailContent(uptime));
			// 重建公司最新季报时间
			CompanyService.getInstance().rebuildMaxMinTimeCache();
//			RuleTimerTask.getInstance().setComputeCompanyList(cl);
			TaskEnter.getInstance().computeCompanyDataByPage("true",
					0,true,false,usl,stime,etime);
			System.out
					.println("["
							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
									new Date())
							+ "]**************************************company data update timer end...**************************");
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
	}

	/*
	 * 计算数据有更新的公司
	 */
	public void computeTradeIndex_add(Date uptime,Date stime,Date etime) {

		try {
			System.out
					.println("["
							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
									new Date())
							+ "]**************************************trade index compute :company data update timer start...**************************");
//			CRuleService.getInstance().updateIndexRuleOfCompute();
//			// 把最新更新的数据导入正式库
//			TaskEnter.getInstance().importNewData2NormalDb(uptime);
//			ComputeIndexManager.getInstance().computeInit();

			CompileMode.setUseCacheExtData(true);
			CompileMode.isAddCompute = true;
//			TaskSchedule.setStime(uptime);
			// 重建公司最新季报时间
			CompanyService.getInstance().rebuildMaxMinTimeCache();
//			RuleTimerTask.getInstance().setComputeCompanyList(CompanyService.getInstance().getCompanyList());
			List<USubject> usl = USubjectService.getInstance().getUSubjectAHZList();
			TaskEnter.getInstance().computeCompanyDataByPage("true", 0,true,true,usl,stime,etime);
			System.out
					.println("["
							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
									new Date())
							+ "]**************************************trade index compute : company data update timer end...**************************");
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
	}
	
	public void computeIndexOfCompanyDataUpdate_notAysn(Date uptime,Date stime,Date etime) {

		try {
			System.out
					.println("["
							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
									new Date())
							+ "]**************************************company data update timer start...**************************uptime="
							+ uptime);
			CRuleService.getInstance().updateIndexRuleOfCompute();
			// 把最新更新的数据导入正式库
			TaskEnter.getInstance().importNewData2NormalDb(uptime);
			ComputeIndexManager.getInstance().computeInit();

			CompileMode.setUseCacheExtData(true);
//			List<Company> cl = CompanyService.getInstance()
//					.getDataUpCompanyList(uptime);
			List<USubject> usl = USubjectService.getInstance().getDataUpUSubjectList(uptime);
			if (usl == null || usl.size() == 0) {
				System.out
						.println("["
								+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
										new Date())
								+ "]**************************************update company list is null,uptime:"
								+ uptime);
				return;
			}
			// 重建公司最新季报时间
			CompanyService.getInstance().rebuildMaxMinTimeCache();
//			RuleTimerTask.getInstance().setComputeCompanyList(cl);
			TaskEnter.getInstance().computeCompanyDataByPage_notAysn("true", 0,usl,stime,etime);
			System.out
					.println("["
							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
									new Date())
							+ "]**************************************company data update timer end...**************************uptime="
							+ uptime);
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
	}

//	public void computeRAvgOnetimeByPage() {
//		try {
//			System.out
//					.println("["
//							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
//									new Date())
//							+ "]**************************************company data update timer start...**************************");
//
//			Date stime = StockUtil.getApproPeriod(new Date());
//			new IndustryDataComputeTaskAction().doComputeRavgOnetimeByPage(
//					stime, stime, 0, "true");
//			System.out
//					.println("["
//							+ DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,
//									new Date())
//							+ "]**************************************company data update timer start...**************************");
//
//		} catch (Exception e) {
//			// TODO: handle exception
//			log.error("compute index failed!", e);
//		}
//
//	}

	public void caprankcomputeChildIndexOneTime() {

		try {
			Date time = StockUtil.getApproPeriod(new Date());
			IndustryService is = IndustryService.getInstance();

			List<String> tags = is.getAllMainTags();
			String cis = CapUtil.getChildCaps();// 综合能力与其它能力分开算，因为需要先算出其它的能力，再把其它能力数据转入中间表，才能算综合能力
			TUExtCacheLoadService el = new TUExtCacheLoadService();
			el.LoadExtData2Cache(time, time);

			TaskEnter.getInstance().caprankcompute(time, time, tags, cis);

		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
	}

//	public void caprankcomputeZHNLOneTime() {
//
//		try {
//
//			Date time = StockUtil.getApproPeriod(new Date());
//			IndustryService is = IndustryService.getInstance();
//
//			List<String> tags = is.getAllMainTags();
//			String cis = CapUtil.getZHNL();
//			cis = CapUtil.getZHNL();
//			new CaprankTaskAction().loadCapIndex2Cache(time, time);
//
//			TUExtCacheLoadService el = new TUExtCacheLoadService();
//			el.LoadExtData2Cache(time, time);
//
//			TaskEnter.getInstance().caprankcompute(time, time, tags, cis);
//		} catch (Exception e) {
//			// TODO: handle exception
//			log.error("compute index failed!", e);
//		}
//	}

	public void batchAddCfTag_CompanyDateUpdate() {

		try {
			Date uptime = StockUtil
					.getNextTimeV3(new Date(), -2, Calendar.DATE);
//			ComputeIndexManager.getInstance().computeInit();
//			TagruleService.getInstance().updateCFRuleOfCompute();
			TaskEnter.getInstance().doComputeCFTag_computeDataUpdate(uptime,
					"true", 0);
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
	}

	public void init(Date suptime) {
		try {
			// 把最新更新的数据导入正式库
			TaskEnter.getInstance().importNewData2NormalDb(suptime);
			LCEnter.getInstance().refresh();
			PlugManager.getInstance().refreshSystemPlug();
			Date uptime = StockUtil
					.getNextTimeV3(new Date(), -2, Calendar.DATE);
			// TaskEnter.getInstance().loadStockPrice2ExtTable(uptime, etime);
			TaskEnter.getInstance().loadFund2ExtTableV2(uptime);
			TaskEnter.getInstance().loadCGSExtTableV2(uptime);
			CRuleService.getInstance().updateIndexRuleOfCompute();
			TagruleService.getInstance().updateCFRuleOfCompute();
			CompileMode.isAddCompute = true;
			CompileMode.setComputeMode(CompileMode.mode_1);

			CompileMode.setUseCacheExtData(true);
			
			List<USubject> ausl = USubjectService.getInstance().getUSubjectListAStock();
			List<USubject> husl = USubjectService.getInstance().getUSubjectListHStock();
			ausl.addAll(husl);
			for (USubject us : ausl) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void inittrade() {
		try {
			LCEnter.getInstance().refresh();
			PlugManager.getInstance().refreshSystemPlug();
//			Date uptime = StockUtil
//					.getNextTimeV3(new Date(), -2, Calendar.DATE);
			// TaskEnter.getInstance().loadStockPrice2ExtTable(uptime, etime);
//			TaskEnter.getInstance().loadFund2ExtTableV2(uptime);
//			TaskEnter.getInstance().loadCGSExtTableV2(uptime);
			CRuleService.getInstance().updateIndexRuleOfCompute();
			TagruleService.getInstance().updateCFRuleOfCompute();
			CompileMode.isAddCompute = true;
			
			CompileMode.setComputeMode(CompileMode.mode_1);

			CompileMode.setUseCacheExtData(true);
			
			List<USubject> ausl = USubjectService.getInstance().getUSubjectListAStock();
			List<USubject> husl = USubjectService.getInstance().getUSubjectListHStock();
			ausl.addAll(husl);
			for (USubject us : ausl) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 导入基金和持股人数
	 */
	public void importFoundCgrsSetTime() {
		Date uptime = StockUtil
				.getNextTimeV3(new Date(), -2, Calendar.DATE);
		init(uptime);
		Calendar nc = Calendar.getInstance();
		nc.setTimeInMillis(System.currentTimeMillis());
		// 运算结束后，把最后一次开始运算时间写到库中
		SystemPropertiesService.getInstance().update("last_compute_date",
				DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));

	}
}
