package com.yz.stock.snn.recognise;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.model.snn.Gene;
import com.stock.common.model.snn.SPair;
import com.stock.common.model.snn.SnnConst;
import com.stock.common.util.DateUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.stock.snn.Snner;
import com.yz.stock.snn.evolution.Evolutioner;

/**
 * 形态识别器
 * 
 * @author：杨真
 * @date：2014-4-7
 */
public class PatternRecogniser {
	Logger logger = LoggerFactory.getLogger(PatternRecogniser.class);
	static PatternRecogniser instance = new PatternRecogniser();

	public PatternRecogniser() {

	}

	public static PatternRecogniser getInstance() {
		return instance;
	}

	// private boolean match(String rule, IndexMessage req) {
	// Double r = CRuleService.getInstance().computeIndex(rule, req);
	// if (r > 0)
	// return true;
	// return false;
	// }

	public boolean match(Tagrule rule, IndexMessage req) {
		Double r = CRuleService.getInstance().computeIndex(rule.getRule(), req,
				rule.getRuleType());
		if (r!=null&&r > 0)
			return true;
		return false;
	}

	/**
	 * 符合形态的最大和最小“结束”时间
	 */
	private ThreadLocal<SPair> _estLocal = new ThreadLocal<SPair>() {
		protected SPair initialValue() {
			// 第一个记录局部时间，第二个记录全局时间，局部时间每次都要清理，全局的整个过程只清理一次
			return new SPair(new SPair(), new SPair());
		}
	};

	public Boolean match(IndexMessage req, Gene g) {
		if (g.getGeneration() == SnnConst.generation_0)
			return PatternRecogniser.getInstance().match(g.getRule(), req);
		else {
			return complexMatch(req, g);
		}
	}

	/**
	 * 记录一组事件的时间交集
	 * 
	 * @param time
	 */
	private void recordTime(Date time) {
		SPair sp = _estLocal.get();
		// 记录局部时间
		dorecordTime(sp.getFirst(), time);
		// 记录全局时间
		dorecordTime(sp.getSecond(), time);
	}

	private void dorecordTime(Object psp, Date time) {
		SPair sp = (SPair) psp;
		Date f = sp.getFirst();
		Date s = sp.getSecond();
		if (f == null || f.compareTo(time) > 0)
			sp.setFirst(time);
		if (s == null || s.compareTo(time) < 0)
			sp.setSecond(time);

	}

	private void clearLocalTime() {
		SPair psp = _estLocal.get();
		psp.getFirst();
		SPair sp = psp.getFirst();
		sp.setFirst(null);
		sp.setSecond(null);
	}

	/**
	 * 全清
	 */
	public void clearThreadLocal() {
		_estLocal.remove();
	}

	/**
	 * 允许n天的时差
	 * 
	 * @param req
	 * @return
	 */
	public Boolean matchAllowNDay(IndexMessage req, Gene g) {
		// logger.info("matchAllowNDay:time:"+DateUtil.format2String(req.getTime())+",uidentify:"+req.getUidentify()+";g.key:"+g.getKey());
		//上市天数小于规定数的，不进来匹配
		int pds = CompanyService.getInstance().getPublishDaysCount(req.getUidentify());
		if(pds<100)
			return false;
		if (g.getGeneration() == SnnConst.generation_0) {
			if (g.getRule() == null)
				return false;
			if (g.getRule().getRuleType() != StockConstants.DEFINE_INDEX)
				return matchAllowNDay_V1(req, g);
			else
				return matchAllowNDay_V2(req, g);
		} else {
			return complexMatch(req, g);
		}
	}

	/**
	 * 允许n天的时差
	 * 
	 * @param req
	 * @return
	 */
	public Boolean matchAllowNDay_V1(IndexMessage req, Gene g) {
		Boolean ret = false;
		Date stime = req.getTime();
		Tagrule tr = g.getRule();
		Date acstime = null;
		if(tr!=null&&DateUtil.getDayStartTime(stime).getTime()!=DateUtil.getDayStartTime(System.currentTimeMillis()).getTime())
		{
			acstime = IndexService.getInstance().formatTime(stime, tr.getRuleType(),tr.getTunit(), req.getUidentify());
		}
		if(acstime==null)
		{
			stime = DateUtil.getDayStartTime(stime);
		}
		else
			stime = acstime;
		Calendar c = Calendar.getInstance();
		c.setTime(stime);
		Date rstime = stime;
		// 同一时间点允许的时差
		int ctimeRegion = getTimeRegion(g.getRule());
		if(ctimeRegion==0)
		{
			IndexMessage nreq = (IndexMessage) req.clone();
			nreq.setTime(stime);
			//交易日，或当天都算
			if ((IndexService.getInstance().isTradeDate(c.getTime(),
					req.getUidentify())||IndexService.getInstance().isCurDay(c.getTime()))
					&& PatternRecogniser.getInstance().match(g.getRule(), nreq)) {
				recordTime(rstime);
				recordTime(stime);
				ret = true;
			}
		}
		else
		{
			Date etime = IndexService.getInstance().getNTradeTimeAfterCurDay(req,
					c.getTime(), -ctimeRegion);
			if (etime == null)
				etime = stime;
			IndexMessage nreq = (IndexMessage) req.clone();
			nreq.setTime(stime);
			while (c.getTime().compareTo(etime) >= 0) {
				nreq.setTime(c.getTime());
				if (IndexService.getInstance().isTradeDate(c.getTime(),
						req.getUidentify())
						&& PatternRecogniser.getInstance().match(g.getRule(), nreq)) {
					recordTime(rstime);
					recordTime(stime);
					ret = true;
					break;
				}
				c.add(Calendar.DAY_OF_MONTH, -1);
			}
		}
		
		return ret;

	}

	private int getTimeRegion(Tagrule rule) {
		if (rule == null)
			return 0;
		return 0;
	}

	/**
	 * 
	 * @param req
	 * @param g
	 * @return
	 */
	public Boolean matchAllowNDay_V2(IndexMessage req, Gene g) {
		Date time = req.getTime();
		// 记录开始时间
		Date stime = DateUtil.getlastPeriod(time);
		Date rtime = time;
		Tagrule tr = g.getRule();
		time = IndexService.getInstance().formatTime(time, tr.getRuleType(),
				tr.getTunit(), req.getUidentify());

		IndexMessage nreq = (IndexMessage) req.clone();
		nreq.setTime(time);
		boolean ismatch = PatternRecogniser.getInstance().match(g.getRule(),
				nreq);
		if (ismatch) {
			recordTime(stime);
			// 记录结束时间
			recordTime(rtime);
		}
		return ismatch;
	}

	/**
	 * 从后往前推着算
	 * 
	 * @param req
	 * @return
	 */
	private Boolean complexMatch(IndexMessage req, Gene g) {
		try {
			IndexMessage nreq = (IndexMessage) req.clone();
			// 1458^1547^4578~2458^1447^4578，”~“符号是用来区分连续时间点，连接点时差允许两周（默认）
			Date stime = req.getTime();
			// 允许的连续策略的时差
			// int timeRegion = ConfigCenterFactory.getInt(
			// "stock_dc.snn_gene_timeRegion", 10);
			// 时间依次往后推，第一组的时间段，默认是etime = stime
			Date etime = stime;
			String[] lss = g.getKey().split("~");
			// 依时间从后往前推
			for (int i = lss.length - 1; i >= 0; i--) {
				String ss = lss[i];
				String[] gs = ss.split("\\^");
				boolean ismatch = false;
				while (stime.compareTo(etime) >= 0) {
					boolean notmatch = false;
					// 1458^1547^4578，”^“符号是用来区分相同时间点，时间点必须在允许的时间段内同时发生
					for (String gk : gs) {
						Gene ng = Evolutioner.getInstance().getGene(gk);
						// 如果有一个不满足，就跳出
						if (!matchAllowNDay(nreq, ng)) {
							notmatch = true;
							break;
						}
					}
					if (notmatch) {
						Date dstime = IndexService.getInstance()
								.getNTradeTimeAfterCurDay(nreq, stime, -1);
						if (dstime == null)
							break;
						stime = dstime;
						nreq.setTime(stime);
					} else {
						// 一段时间内，只要有一个时间点就可以
						ismatch = true;
						break;
					}

				}
				// 如果设置的时间内，没有满足的，就跳出
				if (!ismatch)
					return false;
				else {
					// 取此组的最大，最小开始时间,时间依次后推，计算下一个时间交集
					Date maxTime = getLocalMaxTime();
					Date minTime = getLocalMinTime();
					etime = minTime;
					stime = maxTime;
					nreq.setTime(stime);
					// 清除掉本组时间段记录
					clearLocalTime();
				}
			}

		} catch (StackOverflowError e) {
			e.printStackTrace();
		}
		return true;
	}

	private Date getLocalMaxTime() {
		SPair psp = _estLocal.get();
		SPair sp = psp.getFirst();
		return sp.getSecond();
	}

	private Date getLocalMinTime() {
		SPair psp = _estLocal.get();
		SPair sp = psp.getFirst();
		return sp.getFirst();
	}

	public Date getGlobalMaxTime() {
		SPair psp = _estLocal.get();
		SPair sp = psp.getSecond();
		return sp.getSecond();
	}

	public Date getGlobalMinTime() {
		SPair psp = _estLocal.get();
		SPair sp = psp.getSecond();
		return sp.getFirst();
	}
}
