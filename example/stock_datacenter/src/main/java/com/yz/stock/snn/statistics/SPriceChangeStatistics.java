package com.yz.stock.snn.statistics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.model.snn.Gene;
import com.stock.common.model.snn.Statistics;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.cache.DataLoadTimeMng;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.StatisticsService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.manager.ConfigCenter;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.stock.monitor.RealUtil;
import com.yz.stock.realtime.SnnTimer;
import com.yz.stock.sevent.event.SEvent;
import com.yz.stock.sevent.event.SaveStatisticsEvent;
import com.yz.stock.snn.evolution.Evolutioner;
import com.yz.stock.snn.evolution.GeneFilter;
import com.yz.stock.snn.recognise.PatternRecogniser;

/**
 * 股价变动统计
 * 
 * @author：杨真
 * @date：2014-4-7
 */
public class SPriceChangeStatistics {

	Logger logger = LoggerFactory.getLogger(SPriceChangeStatistics.class);
	static SPriceChangeStatistics instance = new SPriceChangeStatistics();
	static ConcurrentHashMap<String, List<SEvent>> _elm = new ConcurrentHashMap<String, List<SEvent>>();// 事件有序组合

	public SPriceChangeStatistics() {

	}

	public static SPriceChangeStatistics getInstance() {
		return instance;
	}

	public Double getSPriceChangeNTradeDay(Integer type, IndexMessage req,
			Date time) {

		if (IndexService.isCompanyMsg(req)) {
			return getCompanySPriceChangeTradeDay(req.getCompanyCode(), time,
					type);
		} else {
			return getPlateSPriceChangeTradeDay(req.getCompanyCode(), time,
					type);
		}
	}

	/**
	 * 后推第三个交易日
	 * 
	 * @param companycode
	 * @param stime
	 * @return
	 */
	public Double getCompanySPriceChangeTradeDay(String companycode,
			Date stime, int nextDay) {
		Double ret = null;
		if (stime == null)
			return ret;
		Date actime = IndexService.getInstance().formatTime(stime,
				StockConstants.TRADE_TYPE, Calendar.DAY_OF_MONTH, companycode);
		if (actime != null) {
			String indexcode = ConfigCenterFactory.getString(
					"snn.stock_price_change_count_indexcode_sindex",
					StockConstants.INDEX_CODE_TRADE_HFQ_S);
			Double s = IndexValueAgent.getIndexValue(companycode, indexcode,
					actime);// 4732:市值
			Date etime = IndexService.getInstance().getNTradeTimeAfterCurDay(
					companycode, actime, nextDay);
			if (etime.compareTo(new Date()) >= 0) {
				etime = IndexService.getInstance().getNTradeTimeAfterCurDay(
						companycode, DateUtil.getDayStartTime(new Date()), -1);
			}
			String eindexcode = ConfigCenterFactory.getString(
					"snn.stock_price_change_count_indexcode_eindex",
					StockConstants.INDEX_CODE_TRADE_HFQ_S);
			Double er = IndexValueAgent.getIndexValue(companycode, eindexcode,
					etime);
			if (er != null && s != null && s != 0)
				ret = (er - s) / s;
			ret = SMathUtil.getDouble(ret, 4);
		}
		return ret;
	}

	/**
	 * 后推第三个交易日
	 * 
	 * @param companycode
	 * @param stime
	 * @return
	 */
	public Double getPlateSPriceChangeTradeDay(String tag, Date stime,
			int nextDay) {
		Double ret = 0.0;
		if (stime == null)
			return ret;
		Double k = IndexValueAgent.getPlateIndexValue(tag,
				StockConstants.INDEX_CODE_TRADE_HFQ_K, stime);

		Date etime = IndexService.getInstance().getNTradeTimeAfterCurDayPlate(
				tag, stime, nextDay);
		Double er = IndexValueAgent.getPlateIndexValue(tag,
				StockConstants.INDEX_CODE_TRADE_S, etime);
		if (er != null && k != null && k != 0)
			ret = (er - k) / k;
		return SMathUtil.getDouble(ret, 4);
	}

	public void backtest(List<Gene> gl, boolean needEvolutive, boolean needSave) {
		List<USubject> sl = USubjectService.getInstance()
				.getCallTestUSubjectList();
		for (USubject s : sl) {
			// 取上市时间
			Date stime = USubjectService.getInstance().getPulishTime(
					s.getUidentify());
			if (stime == null)
				continue;
			Date etime = new Date();
			// 逐个个公司
			while (stime.compareTo(etime) <= 0) {
				IndexMessage im = SMsgFactory.getUMsg(s.getUidentify(), stime);
				im.setNeedAccessExtIndexDb(false);
				im.setNeedAccessCompanyBaseIndexDb(false);
				patternMatch(im, gl, needSave);
				Date detime = IndexService.getInstance()
						.getNTradeTimeAfterCurDay(im, stime, 1);
				if (detime == null) {
					logger.info("[" + s.getName() + "] end time is :" + stime);
					break;
				} else
					stime = detime;
			}
			if (needEvolutive) {
				List<SEvent> el = _elm.get(s.getUidentify());
				Evolutioner.getInstance().evolutive(el);
			}

		}

	}

	public void backOneTest(List<Gene> gl, boolean needEvolutive, USubject s,
			boolean needSave, Date stime) {
		try {
			// logger.info("backtestByPage:uidentify:" + s.getUidentify()
			// + ",name=" + s.getName());
			List<Gene> ngl = GeneFilter.FilterGeneByUSubject(s, gl);
			if (ngl == null || ngl.size() == 0)
				return;
			// 取上市时间
			Date mtime = USubjectService.getInstance().getTradeIndexMinTime(
					s.getUidentify(), StockConstants.INDEX_CODE_TRADE_S);
			if (mtime == null)
				return;
			if (mtime.getTime() > stime.getTime())
				stime = mtime;
			Date etime = new Date();
			// 逐个个公司
			while (stime.compareTo(etime) <= 0) {
				IndexMessage im = SMsgFactory.getUDCIndexMessage(s
						.getUidentify());
				im.setTime(stime);
				// im.setNeedAccessExtIndexDb(false);
				// im.setNeedAccessCompanyBaseIndexDb(false);
				patternMatch(im, ngl, needSave);
				Date detime = IndexService.getInstance()
						.getNTradeTimeAfterCurDay(im, stime, 1);
				if (detime == null) {
					logger.info("[" + s.getName() + "] end time is :" + stime);
					break;
				} else
					stime = detime;
			}
			if (needEvolutive) {
				List<SEvent> el = _elm.get(s.getUidentify());
				if (el != null)
					Evolutioner.getInstance().evolutive(el);
			}
		} catch (Exception e) {
			logger.error("back test failed!Usubject :" + s.getUidentify(), e);
		}

	}

	public void backtestByPage(List<Gene> gl, boolean needEvolutive,
			List<USubject> sl, boolean needSave) {
		for (USubject s : sl) {
			try {
				logger.info("backtestByPage:uidentify:" + s.getUidentify()
						+ ",name=" + s.getName());
				List<Gene> ngl = GeneFilter.FilterGeneByUSubject(s, gl);
				if (ngl == null || ngl.size() == 0)
					continue;
				// 取上市时间
				Date stime = USubjectService.getInstance().getPulishTime(
						s.getUidentify());
				if (stime == null)
					continue;
				Date etime = new Date();
				// 逐个个公司
				while (stime.compareTo(etime) <= 0) {
					IndexMessage im = SMsgFactory.getUDCIndexMessage(s
							.getUidentify());
					im.setTime(stime);
					// im.setNeedAccessExtIndexDb(false);
					// im.setNeedAccessCompanyBaseIndexDb(false);
					patternMatch(im, ngl, needSave);
					Date detime = IndexService.getInstance()
							.getNTradeTimeAfterCurDay(im, stime, 1);
					if (detime == null) {
						logger.info("[" + s.getName() + "] end time is :"
								+ stime);
						break;
					} else
						stime = detime;
				}
				if (needEvolutive) {
					List<SEvent> el = _elm.get(s.getUidentify());
					if (el != null)
						Evolutioner.getInstance().evolutive(el);
				}
			} catch (Exception e) {
				logger.error("back test failed!Usubject :" + s.getUidentify(),
						e);
			}

		}

	}

	private void patternMatch(IndexMessage im, List<Gene> gl, boolean needSave) {
		if (gl == null)
			return;
		// logger.info("patternMatch:"+DateUtil.format2String(im.getTime())+",uidentify:"+im.getUidentify());
		for (Gene g : gl) {
			try {
				// 如果是财务规则,则判断是否是离财报最近的一个交易日,如果不是,就不计算
				if (!GeneFilter.isneedComputeGene(g, im))
					continue;
				if (PatternRecogniser.getInstance().matchAllowNDay(im, g)) {
					List<SEvent> el = _elm.get(im.getCompanyCode());
					if (el == null) {
						el = new ArrayList<SEvent>();
						_elm.put(im.getCompanyCode(), el);
					}
					String objid = StockUtil.getObjectId(im);
					SEvent se = new SEvent(g, PatternRecogniser.getInstance()
							.getGlobalMinTime(), PatternRecogniser
							.getInstance().getGlobalMaxTime());
					se.setSourceid(objid);
					el.add(se);
					// 增加权重
					g.addWeight(1.0);
					addStatisticsToGene(im, g, se.getEtime());
					if (needSave) {
						se.setSave(needSave);
						ClientEventCenter.getInstance().putEvent(se);
					}
				}
				PatternRecogniser.getInstance().clearThreadLocal();
			} catch (Exception e) {
				logger.error("patternMatch failed!", e);
			}
		}

	}

	private void addStatisticsToGene(IndexMessage im, Gene g, Date etime) {

		Statistics s = g.getStatis();
		synchronized (s) {
			Double sck1d = getSPriceChangeNTradeDay(Statistics.k1d, im, etime);
			if (sck1d != null && sck1d != 0) {
				Double lzf = ConfigCenterFactory.getDouble(
						"snn.mapreduce_zf_limit_k1d", 0.101);
				if (sck1d <= lzf) {
					logCount(Statistics.k1d,sck1d,etime,im);
					s.putNewSPriceChange(Statistics.k1d, sck1d);
				} else {
					if (SLogFactory.isopen("snn_mapreduce_zf_limit_log"))
						logger.info("k1d zf exceed lzf=" + lzf + ",zf=" + sck1d
								+ ";uidentify=" + im.getCompanyCode()
								+ ";time=" + etime);
					return;
				}
			}

			Double sck3d = getSPriceChangeNTradeDay(Statistics.k3d, im, etime);
			if (sck3d != null && sck3d != 0) {
				Double lzf = ConfigCenterFactory.getDouble(
						"snn.mapreduce_zf_limit_k3d", 0.3);
				if (sck3d <= lzf) {
					logCount(Statistics.k3d, sck3d,etime,im);
					s.putNewSPriceChange(Statistics.k3d, sck3d);
				} else {
					if (SLogFactory.isopen("snn_mapreduce_zf_limit_log"))
						logger.info("k3d zf exceed lzf=" + lzf + ",zf=" + sck3d
								+ ";uidentify=" + im.getCompanyCode()
								+ ";time=" + etime);
					return;
				}
			}

			Double sck5d = getSPriceChangeNTradeDay(Statistics.k5d, im, etime);
			if (sck5d != null && sck5d != 0) {
				Double lzf = ConfigCenterFactory.getDouble(
						"snn.mapreduce_zf_limit_k5d", 0.5);
				if (sck5d <= lzf) {
					logCount(Statistics.k5d, sck5d,etime,im);
					s.putNewSPriceChange(Statistics.k5d, sck5d);
				} else {
					if (SLogFactory.isopen("snn_mapreduce_zf_limit_log"))
						logger.info("k5d zf exceed lzf=" + lzf + ",zf=" + sck5d
								+ ";uidentify=" + im.getCompanyCode()
								+ ";time=" + etime);
					return;
				}
			}

			Double sck10d = getSPriceChangeNTradeDay(Statistics.k10d, im, etime);
			if (sck10d != null && sck10d != 0) {
				Double lzf = ConfigCenterFactory.getDouble(
						"snn.mapreduce_zf_limit_k10d", 0.8);
				if (sck10d <= lzf) {
					logCount(Statistics.k10d, sck10d,etime,im);
					s.putNewSPriceChange(Statistics.k10d, sck10d);
				} else {
					if (SLogFactory.isopen("snn_mapreduce_zf_limit_log"))
						logger.info("k10d zf exceed lzf=" + lzf + ",zf="
								+ sck10d + ";uidentify=" + im.getCompanyCode()
								+ ";time=" + etime);
					return;
				}
			}

			Double sck1m = getSPriceChangeNTradeDay(Statistics.k1m, im, etime);
			if (sck1m != null && sck1m != 0) {
				Double lzf = ConfigCenterFactory.getDouble(
						"snn.mapreduce_zf_limit_k1m", 1.5);
				if (sck1m <= lzf) {
					logCount(Statistics.k1m, sck1m,etime,im);
					s.putNewSPriceChange(Statistics.k1m, sck1m);
				} else {
					if (SLogFactory.isopen("snn_mapreduce_zf_limit_log"))
						logger.info("k1m zf exceed lzf=" + lzf + ",zf=" + sck1m
								+ ";uidentify=" + im.getCompanyCode()
								+ ";time=" + etime);
					return;
				}
			}

			Double sck3m = getSPriceChangeNTradeDay(Statistics.k3m, im, etime);
			if (sck3m != null && sck3m != 0) {
				Double lzf = ConfigCenterFactory.getDouble(
						"snn.mapreduce_zf_limit_k3m", 2.2);
				if (sck3m <= lzf) {
					logCount(Statistics.k3m, sck3m,etime,im);
					s.putNewSPriceChange(Statistics.k3m, sck3m);
				} else {
					if (SLogFactory.isopen("snn_mapreduce_zf_limit_log"))
						logger.info("k3m zf exceed lzf=" + lzf + ",zf=" + sck3m
								+ ";uidentify=" + im.getCompanyCode()
								+ ";time=" + etime);
					return;
				}
			}

			Double sck1y = getSPriceChangeNTradeDay(Statistics.k1y, im, etime);
			if (sck1y != null && sck1y != 0) {
				Double lzf = ConfigCenterFactory.getDouble(
						"snn.mapreduce_zf_limit_k1y", 3.0);
				if (sck1y <= lzf) {
					logCount(Statistics.k1y, sck1y,etime,im);
					s.putNewSPriceChange(Statistics.k1y, sck1y);
				} else {
					if (SLogFactory.isopen("snn_mapreduce_zf_limit_log"))
						logger.info("k1y zf exceed lzf=" + lzf + ",zf=" + sck1y
								+ ";uidentify=" + im.getCompanyCode()
								+ ";time=" + etime);
					return;
				}
			}
			if (SLogFactory.isopen("snn_mapreduce_statictis_match_success_log"))
				logger.info("match success! k1d=" + sck1d + ",k3d=" + sck3d
						+ ",k5d=" + sck5d + ",k10d=" + sck10d + ",k1m=" + sck1m
						+ ",k3m=" + sck3m + ",k1y=" + sck1y + ";req=" + im
						+ ";etime=" + etime);
			s.addTimes();
			USubject us = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(im.getCompanyCode());
			if (us != null) {
				s.record(Statistics.k3d, sck3d,
						us.getName() + ":" + us.getUidentify());
			}

		}
	}

	private void logCount(int key, Double sc,Date etime, IndexMessage im) {
		if(sc>=0)
		{
			if (SLogFactory.isopen("snn_mapreduce_zf_limit_log_upcompany_"+key))
				System.out.println("up ,key="+key + ",sc="+ sc +  ",uidentify=" + im.getCompanyCode()
						+ ",time=" + DateUtil.format2String(etime));
		}
		else
		{
			if (SLogFactory.isopen("snn_mapreduce_zf_limit_log_downcompany_"+key))
				System.out.println("down ,key="+key + ",sc="+ sc +  ",uidentify=" + im.getCompanyCode()
					+ ",time=" + DateUtil.format2String(etime));
		}
		
		
	}

	public void realMonitor(List<Gene> gl, Date time) {
		int needRealCompute = ConfigCenterFactory.getInt(
				"snn.realMonitor_needRealCompute", 1);
		List<USubject> sl = USubjectService.getInstance()
				.getCallTestUSubjectList();
		for (USubject s : sl) {
			List<Gene> ngl = GeneFilter.FilterGeneByUSubject(s, gl);
			IndexMessage im = SMsgFactory.getUMsg(s.getUidentify(), time);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedAccessCompanyBaseIndexDb(false);
			if (needRealCompute == 1)
				im.setNeedComput(true);
			else
				im.setNeedComput(false);
			patternMatchReal(im, ngl);
		}

	}

	/**
	 * 对指定公司,指定时间进行一次监控
	 * 
	 * @param gl
	 * @param time
	 */
	public void realMonitor(List<Gene> gl, Date time, List<USubject> sl) {
		if (sl == null)
			return;
		int isopen = ConfigCenterFactory.getInt("snn.open_real_monitor",
				1);
		if(isopen!=1)
			return;
		for (USubject s : sl) {
			
			if(s.getUidentify().endsWith(".hk"))
			{
				int nhk = ConfigCenterFactory.getInt("snn.need_check_hk",
						0);
				if(nhk==0)
				{
					continue;
				}
			}
			List<Gene> ngl = GeneFilter.FilterGeneByUSubject(s, gl);
			IndexMessage im = SMsgFactory.getUMsg(s.getUidentify(), time);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedComput(true);
			patternMatchReal(im, ngl);
		}
	}

	public void realMonitor(List<Gene> gl, Date time, USubject s) {
		if (s == null)
			return;
		List<Gene> ngl = GeneFilter.FilterGeneByUSubject(s, gl);
		IndexMessage im = SMsgFactory.getUMsg(s.getUidentify(), time);
		im.setNeedAccessExtIndexDb(false);
		im.setNeedAccessCompanyBaseIndexDb(false);
		im.setNeedComput(true);
		patternMatchReal(im, ngl);

	}

	public void patternMatchReal(IndexMessage im, List<Gene> gl) {
		if (gl == null
				|| CompanyService.getInstance().isSTStock(im.getCompanyCode()))
			return;
		for (Gene g : gl) {
			// 实时监控不对财务规则做
			if (g.getRule() != null
					&& g.getRule().getRuleType() == StockConstants.DEFINE_INDEX)
				continue;
			if (SnnTimer.getInstance().isExistSuccessPattern(im.getUidentify(),
					g.getKey()))
				continue;
			String needOntCheckRule = ConfigCenterFactory.getString("snn.needOntCheckRuleIds", "");
			if(!StringUtil.isEmpty(needOntCheckRule))
			{
				if(needOntCheckRule.contains(g.getKey()))
					continue;
			}
			if (PatternRecogniser.getInstance().matchAllowNDay(im, g)) {
				// 生成一个通知事件，把消息通知给消息订阅者
				TradeAlarmMsg tamsg = new TradeAlarmMsg(im.getUidentify(),
						g.getKey(), PatternRecogniser.getInstance()
								.getGlobalMinTime().getTime(),
						PatternRecogniser.getInstance().getGlobalMaxTime()
								.getTime());
				USubject usubject = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(im.getUidentify());
				Tagrule tr = TagruleService.getInstance()
						.getTagruleByIdFromCache(tamsg.getEventid());
				String sm = tr.getComments();
				tamsg.putAttr("summary", sm);
				tamsg.putAttr("title", usubject.getName()+"(" +usubject.getUidentify()+") "+ tr.getTagDesc());
				tamsg.setMsgType(MsgConst.MSG_TRADEMSG_TYPE_1);
				tamsg.putAttr("ktype", tr.getTimeUnit());
				String c = RealUtil.getSetChancesCategory(String.valueOf(tr.getId()));
				if(!StringUtil.isEmpty(c))
				{
					tamsg.putAttr(StockConstants.CHANCETAG, c);
					tamsg.putAttr("op", 2);
					tamsg.setMsgType(MsgConst.MSG_TRADEMSG_TYPE_3);
				}
				TradeService.getInstance().notifyTheEventChance(tamsg);
				TradeService.getInstance().notifyToUser(tamsg);
				SnnTimer.getInstance().recordTheEvent(im.getUidentify(),
						g.getRule());
				if (g.getRule() != null) {
					if (SLogFactory.isopen("snn_realmonitor_log_isopen"))
						logger.info(g.getRule().getTagDesc()
								+ " : "
								+ im.getUidentify()
								+ ":time="
								+ DateUtil.format2String(new Date(tamsg
										.getTime())));
				} else {
					if (SLogFactory.isopen("snn_realmonitor_log_isopen"))
						logger.info(g.getKey()
								+ ":time="
								+ DateUtil.format2String(new Date(tamsg
										.getTime())));
				}
			}
			PatternRecogniser.getInstance().clearThreadLocal();
		}

	}

	public void saveAllStatistics() {
		List<Gene> gl = Evolutioner.getInstance().getGeneList();
		for (Gene g : gl) {
			Statistics st = g.getStatis();
			st.setHtimes(Long.valueOf(st.times.get()));
			// StatisticsService.getInstance().save(g.getStatis());
			SaveStatisticsEvent sse = new SaveStatisticsEvent(g.getStatis());
			ClientEventCenter.getInstance().putEvent(sse);
		}

	}

	/**
	 * 对最新发季报的公司,进行事件监控
	 * 
	 * @param gl
	 * @param time
	 */
	public void realMonitorCfRule(List<Gene> gl, Date time, List<Company> cl) {
		if (cl == null)
			return;
		for (Company c : cl) {
			USubject us = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(c.getCompanyCode());
			List<Gene> ngl = GeneFilter.FilterGeneByUSubject(us, gl);
			IndexMessage im = SMsgFactory.getUMsg(c.getCompanyCode(), time);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedAccessCompanyBaseIndexDb(false);
			patternMatchRealRule(im, ngl, c);
		}
	}

	private void patternMatchRealRule(IndexMessage im, List<Gene> gl, Company c) {
		if (gl == null)
			return;
		for (Gene g : gl) {
			if (g.getRule() != null
					&& g.getRule().getRuleType() == StockConstants.DEFINE_INDEX) {
				im.setTime(c.getReportTime());
			} else
				continue;
			if (PatternRecogniser.getInstance().matchAllowNDay(im, g)) {
				String objid = StockUtil.getObjectId(im);
				SEvent se = new SEvent(g, PatternRecogniser.getInstance()
						.getGlobalMinTime(), PatternRecogniser.getInstance()
						.getGlobalMaxTime());
				se.setSourceid(objid);
				// 增加权重
				g.addWeight(1.0);
				Statistics st = StatisticsService.getInstance()
						.getStatisticsFromCache(g.getKey());
				if (st != null)
					se.getG().setStatis(st);
				se.setSave(true);
				ClientEventCenter.getInstance().putEvent(se);
			}
			PatternRecogniser.getInstance().clearThreadLocal();
		}

	}

}
