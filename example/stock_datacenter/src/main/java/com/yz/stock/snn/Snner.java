package com.yz.stock.snn;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.CompileMode;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.USubject;
import com.stock.common.model.snn.Gene;
import com.stock.common.model.snn.Statistics;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.cache.DataLoadTimeMng;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.portal.cache.TUExtCacheLoadService;
import com.yz.stock.portal.cache.Trade0001CacheLoadService;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.task.TaskSchedule;
import com.yz.stock.snn.evolution.Evolutioner;
import com.yz.stock.snn.statistics.SPriceChangeStatistics;

/**
 * 回测统计
 * 
 * @author：杨真
 * @date：2014-4-17
 */
public class Snner {
	public static long expTime = Calendar.getInstance().getTimeInMillis();
	Logger log = LoggerFactory.getLogger(this.getClass());
	int pagesize = 50;
	static Snner instance = new Snner();
	Set<String> hadLoad = new HashSet<String>();

	public Snner() {

	}

	public static Snner getInstance() {
		return instance;
	}

	public void start() {
		String stime = "1990-01-01";

		CompileMode.setUseCacheExtData(true);
		CompileMode.setComputeMode(CompileMode.mode_1);
		List<USubject> sl = USubjectService.getInstance()
				.getCallTestUSubjectList();
		int pagecount = sl.size() / pagesize + 1;
		// 原始基因
		for (int i = 0; i < pagecount; i++) {
			System.out
					.println("-------------------------------current page is:"
							+ i + "!--------------------------------");
			List<USubject> csl = getUSubjectListByPage(i);
			loadOnePageUSubjectAllDate(csl, true, DateUtil.format(stime));
			// SPriceChangeStatistics.getInstance().backtestByPage(Evolutioner.getInstance().getGeneList(),true,csl);
			List<Gene> gl = Evolutioner.getInstance().getOrinalGeneList();
			for (USubject us : csl) {
				backTestTask(us, gl, true, false,DateUtil.format(stime));
			}
			waitLastTaskOver();
			printStatisticsLog(gl);
		}
		waitLastTaskOver();
		// 进化后基因
		for (int i = 0; i < pagecount; i++) {
			List<USubject> csl = getUSubjectListByPage(i);
			Trade0001CacheLoadService.getInstance().loadCurpageUSubjectTrade(
					csl, true, DateUtil.format(stime));
			// SPriceChangeStatistics.getInstance().backtestByPage(Evolutioner.getInstance().getEvolutiveGeneList(),false,csl);
			List<Gene> gl = Evolutioner.getInstance().getEvolutiveGeneList();
			for (USubject us : csl) {
				backTestTask(us, gl, false, false,DateUtil.format(stime));
			}
			waitLastTaskOver();
			printStatisticsLog(gl);
		}
		SPriceChangeStatistics.getInstance().saveAllStatistics();
	}

	private Object backTestTask(final USubject s, final List<Gene> gl,
			final boolean needEvolutive, final boolean needSave, final Date stime) {
		// 切记一定要放外面
		final Long pid = Thread.currentThread().getId();
		StockFactory.submitTaskBlocking(new Callable<String>() {

			public String call() throws Exception {
				TaskSchedule.put(pid, Thread.currentThread().getId());
				try {
					SPriceChangeStatistics.getInstance().backOneTest(gl,
							needEvolutive, s, needSave,stime);
				} catch (Exception e) {
					log.error("back test failed!Usubject :" + s.getUidentify(),
							e);
				}
				finally{
					TaskSchedule.remove(pid, Thread.currentThread().getId());
				}
				return StockCodes.SUCCESS;
			}

		});
		return StockCodes.SUCCESS;
	}

	public synchronized String testOneRule(Gene g, Date stime) {
		CompileMode.setUseCacheExtData(true);
		CompileMode.setComputeMode(CompileMode.mode_1);
		List<USubject> sl = USubjectService.getInstance()
				.getCallTestUSubjectListOfHost();
		int pagecount = sl.size() / pagesize + 1;
		// 原始基因
		for (int i = 0; i < pagecount; i++) {
			System.out
					.println("-------------------------------current page is:"
							+ i + "!--------------------------------");
			List<USubject> csl = getUSubjectListByPage(i, sl);
			loadOnePageUSubjectAllDate(csl, false, stime);
			// SPriceChangeStatistics.getInstance().backtestByPage(Evolutioner.getInstance().getGeneList(),true,csl);
			List<Gene> gl = new ArrayList<Gene>();
			gl.add(g);
			for (USubject us : csl) {
				backTestTask(us, gl, false, false,stime);
			}
			waitLastTaskOver();
		}
		return g.getStatis().getDescJSon();
	}

	private void loadOnePageUSubjectAllDate(List<USubject> csl,
			boolean isclear, Date stime) {
		String key = "snn_hadLoadcache_"
				+ DateUtil.formatDate2YYYYMMDDFast(stime);
		hadLoad = LCEnter.getInstance().get(key,
				StockUtil.getExtIndexCacheName(key));
		if (hadLoad == null) {
			hadLoad = new HashSet<String>();
			LCEnter.getInstance().put(key, hadLoad,
					StockUtil.getExtIndexCacheName(key));
		}

		if (isclear)
			hadLoad.clear();
		List<USubject> nusl = new ArrayList<USubject>();
		for (USubject us : csl) {
			if (!hadLoad.contains(us.getUidentify())) {
				Date mtime = USubjectService.getInstance()
						.getTradeIndexMinTime(us.getUidentify(),
								StockConstants.INDEX_CODE_TRADE_S);
				Date ptime = USubjectService.getInstance().getPulishTime(us.getUidentify());
				if(mtime!=null&&mtime.getTime()>stime.getTime()&&ptime.getTime()<mtime.getTime())
				{
					nusl.add(us);
					System.out.println("need load the Usubject :" + us.getName()
							+ ":" + us.getUidentify());
					hadLoad.add(us.getUidentify());
				}
				
			}
		}
		LCEnter.getInstance().put(key, hadLoad,
				StockUtil.getExtIndexCacheName(key));
		Trade0001CacheLoadService.getInstance().loadCurpageUSubjectTrade(nusl,
				isclear, stime);
		TUExtCacheLoadService.getInstance().loadUExtUSubjectData(nusl, isclear,
				DateUtil.format2String(stime));
	}

	private void waitLastTaskOver() {
		while (true) {
			if (!TaskSchedule.hasRunningTask()) {
				break;
			}

		}

	}

	private List<USubject> getUSubjectListByPage(int page) {
		List<USubject> sl = USubjectService.getInstance()
				.getCallTestUSubjectList();

		int start = page * pagesize;
		if (start > sl.size()) {
			System.out.println("=========page is out range!---");
			return null;
		}

		int end = (page + 1) * pagesize;
		if (end > sl.size())
			end = sl.size();
		return sl.subList(start, end);
	}

	public List<USubject> getUSubjectListByPage(int page, List<USubject> sl) {

		int start = page * pagesize;
		if (start > sl.size()) {
			System.out.println("=========page is out range!---");
			return null;
		}

		int end = (page + 1) * pagesize;
		if (end > sl.size())
			end = sl.size();
		return sl.subList(start, end);
	}

	private void printStatisticsLog(List<Gene> gl2) {
		System.out
				.println("============================================================");
		List<Gene> ngl = new ArrayList<Gene>();
		ngl.addAll(gl2);
		Collections.sort(ngl, new Comparator<Gene>() {

			public int compare(Gene arg0, Gene arg1) {
				Statistics st0 = arg0.getStatis();
				Statistics st1 = arg1.getStatis();
				if (st0.getTimes().get() > st1.getTimes().get())
					return 1;
				return 0;
			}

		});
		for (Gene g : ngl) {
			if (g.getStatis().getTimes().get() == 0)
				continue;
			System.out.println(TagruleService.getInstance().translateGk(
					g.getKey())
					+ ";" + g.getStatis().getDesc());
		}

	}
	
	public void clearRecord()
	{
		if(hadLoad!=null)
			hadLoad.clear();
	}
}
