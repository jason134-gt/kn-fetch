package com.yz.stock.realtime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.BFConst;
import com.stock.common.bloomfilter.BFUtil;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.model.snn.Gene;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.trade.TradeCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.plug.IPlugIn;
import com.yz.stock.portal.cache.Trade0001CacheLoadService;
import com.yz.stock.snn.evolution.Evolutioner;
import com.yz.stock.snn.statistics.SPriceChangeStatistics;
import com.yz.stock.snn.zinput.KnowledgeInput;

/**
 * 定时器，承担事件实时监控任务
 * 
 * @author：杨真
 * @date：2014-4-10
 */
public class SnnTimer implements IPlugIn {

	Logger logger = LoggerFactory.getLogger(SnnTimer.class);
	static SnnTimer instance = new SnnTimer();
	Set<Long> _hasComputeToday = new HashSet<Long>();
	Map<String, Long> _sucessMap = new ConcurrentHashMap<String, Long>();// 已配型成功缓存

	public SnnTimer() {

	}

	public static SnnTimer getInstance() {
		return instance;
	}

	// 港股或A股，只要有一个开市，就算开市
	private int isTradeOpen() {
		int st0 = TradeCenter.getInstance().getTradeStatus(0);
		int st1 = TradeCenter.getInstance().getTradeStatus(1);
		if (st0 == 1 || st1 == 1)
			return 1;
		return 0;
	}

	public void timerCheck(List<USubject> usl) {
		List<Gene> gl = Evolutioner.getInstance().getGoodGeneList();
		if (gl != null) {
			try {
				SPriceChangeStatistics.getInstance()
						.realMonitor(gl, new Date(),usl);
			} catch (Exception e) {
				logger.error("real monitor failed!", e);
			}

		}
	}

	public void baseRuleCheck(List<USubject> usl) {
		logger.info("baseRuleCheck start ...");
		List<Gene> gl = getNeedCheckRule();
		if (gl != null) {
			try {
				SPriceChangeStatistics.getInstance()
						.realMonitor(gl, new Date(),usl);
			} catch (Exception e) {
				logger.error("real monitor failed!", e);
			}

		}
		logger.info("baseRuleCheck complete ...");
	}

	private List<Gene> getNeedCheckRule() {
		List<Gene> ngl = new ArrayList<Gene>();
		List<Gene> gl = KnowledgeInput.getInstance().getAllInitGene();
		String ngs = ConfigCenterFactory.getString("snn.need_check_rule_ids",
				"");
		if (StringUtil.isEmpty(ngs))
			return null;
		Set<String> ss = new HashSet<String>();
		for (String ngid : ngs.split(",")) {
			ss.add(ngid);
		}
		if (gl != null) {
			for (Gene g : gl) {
				if (ss.contains(g.getKey()))
				{
					if(g.getRule()!=null&&!StringUtil.isEmpty(g.getRule().getComments()))
					{
							ngl.add(g);
							if(SLogFactory.isopen("snn_NeedCheckRule_log_check"))
								logger.info("check rule!"+g.getRule());
					}
					else
					{
						if(SLogFactory.isopen("snn_NeedCheckRule_log_nocheck"))
							logger.info("comments is nul!"+g.getRule());
					}
						
				}
			}
		}
		return ngl;
	}

	public void start() {
//		IndexCacheService.getInstance().refreshTrade0001AndFlush2Disk();
		Thread t = new Thread(new Runnable() {

			public void run() {

				while (true) {
//					TradeCenter.getInstance().judgmentATradeClosed();
					int state = isTradeOpen();
					Date td = DateUtil.getDayStartTime(new Date());
					// 如果为开盘状态,就开始监控
					if (state == 1) {
						// 如果是今天第一次进来,则清掉前一天的所有超时的配型成功记录
						if (!_hasComputeToday.contains(td.getTime())) {
							if(Trade0001CacheLoadService.getInstance().todayTradeDataHadLoad())
							{
//								IndexCacheService.getInstance()
//								.refreshTrade0001AndFlush2Disk();
							}
							_hasComputeToday.clear();
							_hasComputeToday.add(td.getTime());
							clearOldRecord();
						}
						int ast0 = TradeCenter.getInstance().getTradeStatus(0);
						if(ast0==1)
						{
							List<USubject> sl = USubjectService.getInstance().getUSubjectListAStock();
							baseRuleCheck(sl);
						}
						int hkst = TradeCenter.getInstance().getTradeStatus(1);
						if(hkst==1)
						{
							int nhk = ConfigCenterFactory.getInt("snn.need_check_hk",
									0);
							if(nhk==1)
							{
								List<USubject> sl = USubjectService.getInstance().getUSubjectListHStock();
								baseRuleCheck(sl);
							}
							
						}
						
					}
					try {
						Thread.sleep(30000l);
						BFUtil.flushDisk(BFConst.tradeAlarm);
					} catch (Exception e) {
						logger.error("thread sleep failed!", e);
					}
				}

			}

		});
		t.setName("SnnTimer_checkBaseRule");
		t.start();
	}

	public boolean isExistSuccessPattern(String uid, String rid) {
		String k = getUkey(uid, rid);
		Long experiedtime = _sucessMap.get(k);
		if (experiedtime == null) {
			return false;
		} else {
			if (System.currentTimeMillis() > experiedtime) {
				_sucessMap.remove(k);
				return false;
			}
		}
		return true;
	}

	protected void clearOldRecord() {
		Iterator<String> iter = _sucessMap.keySet().iterator();
		while (iter.hasNext()) {
			String k = iter.next();
			Long experiedtime = _sucessMap.get(k);
			if (System.currentTimeMillis() > experiedtime)
				_sucessMap.remove(k);
		}

	}

	public void recordTheEvent(String uidentifyid, Tagrule rule) {
		String k = getUkey(uidentifyid, String.valueOf(rule.getId()));
		Long experiedTime = computeExperiedTime(rule);
		if (experiedTime != null)
			_sucessMap.put(k, experiedTime);
	}

	private Long computeExperiedTime(Tagrule rule) {
		Long etime = null;
		int t = rule.getTunit();
		Date et = new Date();
		switch (t) {
		case Calendar.MONTH:
			et = StockUtil.getNextTimeV3(new Date(), 1, Calendar.MONTH);
			etime = DateUtil.getMonthStartTime(et).getTime();
			break;
		case Calendar.WEEK_OF_MONTH:
			et = StockUtil.getNextTimeV3(new Date(), 1, Calendar.WEEK_OF_MONTH);
			etime = DateUtil.getWeekStartTime(et).getTime();
			break;
		case Calendar.DAY_OF_MONTH:
			et = StockUtil.getNextTimeV3(new Date(), 1, Calendar.DAY_OF_MONTH);
			etime = DateUtil.getDayStartTime(et).getTime();
			break;
		}
		return etime;
	}

	private String getUkey(String uid, String rid) {
		// TODO Auto-generated method stub
		return uid + "^" + rid;
	}

	public void clearSuccessRecordMap() {
		_sucessMap.clear();
	}

	@Override
	public void plugIn() {

		start();
	}
}
