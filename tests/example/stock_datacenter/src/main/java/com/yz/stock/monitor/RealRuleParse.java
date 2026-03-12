package com.yz.stock.monitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SLogFactory;
import com.yfzx.service.hfunction.HUtilService;
import com.yfzx.service.realtime.RealtimeDataItem;
import com.yz.configcenter.ConfigCenterFactory;

/**
 * 格式：起始时间:结束时间~规则类型:规则;起始时间:结束时间~规则类型:规则;... 多个用“;”号分隔
 * 
 * 规则类型：【0：前置规则|1：观察点类型|2：实时规则】^规则 0：前置规则;格式:0^【0:分时，1：非分时规则】^详细规则
 * 1：观察点类型，格式：规则类型^【0：建立，1：取消】:详细规则^【0：建立，1：取消】:详细规则
 * 2：实时规则，格式：0^【0:分时，1：非分时规则】^详细规则 详细规则： 0：分时规则，格式：告警类型^异动类型:异动描述^分时规则公式
 * 1：非实时规则，格式：规则类型^规则公式 例：
 * 0:10~0^0^0^0:快速上涨^$oczf$>2.5~1^0^$oczf$>2.5^1^$czf$<2.5
 * ~2^0^2^${日后复权5日均价:4863}>0~2^0^0^0:快速上涨^$oczf$>2.5 | | | | | | | | 起 结 前 分 告 异
 * 异 实 始 束 置 时 警 动 动 时 时 时 规 规 类 类 描 规 间 间 则 则 型 型 述 则
 * 
 * @author：杨真
 * @date：2015年4月15日
 */
public class RealRuleParse {
	static Logger log = LoggerFactory.getLogger(RealRuleParse.class);
	static Map<String, Long> _ws = new ConcurrentHashMap<String, Long>();// 记录已生成的告警项
	String orule;
	Map<String, String> nameMap = new ConcurrentHashMap<String, String>();
	Pattern p = Pattern.compile("\\$[a-zA-Z_0-9]+\\$");
	int eventType = -1;// 事件类型：快速上涨等，留用
	String eventTypeDesc = "";// 事件类型：快速上涨等，留用
	int secondStart = 0;// 有效时间段的起始时间
	int secondEnd = 330;// 有效时间段的结束时间
	int warnType = 0;// 告警类型：0:发机会且发私信提醒，1：只发机会，不发私信，2：只发私信不发机会
	// 实时条件
	public List<RuleParse> realRules;
	// 前置条件，一天只检查一次
	public List<RuleParse> preRules;
	// 生成观察点的规则
	public RuleParse observerCreateRule;
	// 取消息观察点的规则
	public RuleParse observerDelRule;

	RuleChildFilter _preRulecheckFilter_not = new RuleChildFilter();// 不满足前置条件的公司
	RuleChildFilter _preRulecheckFilter_ok = new RuleChildFilter();// 满足前置条件的公司

	long experied = 600000l;
	Long lastClear = 0l;

	int findtype;// 寻找观察点的类型,0:找最低点，1：找最高点
	Map<String,Counter>  observerMap = new ConcurrentHashMap<String,Counter>();
	public static int ruleType_0 = 0;
	public static int ruleType_1 = 1;
	public static int ruleType_2 = 2;
	String txlx="txlx0";//提醒类型，相同的提醒类型，只提醒一次
	/**
	 * 格式：起始时间:结束时间~规则类型:规则;起始时间:结束时间~规则类型:规则;... 多个用“;”号分隔
	 * 
	 * 规则类型：【0：前置规则|1：观察点类型|2：实时规则】^规则 0：前置规则;格式:0^【0:分时，1：非分时规则】^详细规则
	 * 1：观察点类型，格式：规则类型^【0：建立，1：取消】:详细规则^【0：建立，1：取消】:详细规则
	 * 2：实时规则，格式：0^【0:分时，1：非分时规则】^详细规则 详细规则： 0：分时规则，格式：告警类型^异动类型:异动描述^分时规则公式
	 * 1：非实时规则，格式：规则类型^规则公式 例：
	 * 0:10~0^0^0^0:快速上涨^$oczf$>2.5~1^0^$oczf$>2.5^1^$czf$
	 * <2.5~2^0^2^${日后复权5日均价:4863}>0~2^0^0^0:快速上涨^$oczf$>2.5
	 * 
	 * @param rrule
	 */
	public RealRuleParse(String rrule) {
		List<RuleParse> realRules = new ArrayList<RuleParse>();
		List<RuleParse> preRules = new ArrayList<RuleParse>();
		try {
			orule = rrule;
			String[] prrules = rrule.trim().split("~");
			secondStart = Integer.valueOf(prrules[0].split(":")[0]);
			secondEnd = Integer.valueOf(prrules[0].split(":")[1]);

			warnType = Integer.valueOf(prrules[0].split(":")[2]);
			eventType = Integer.valueOf(prrules[0].split(":")[3]);
			eventTypeDesc = prrules[0].split(":")[4];
			txlx = prrules[0].split(":")[5];
			if(prrules[0].split(":").length>6)
			{
				experied = Long.valueOf(prrules[0].split(":")[6]);
			}
			for (int i = 1; i < prrules.length; i++) {
				String rule = prrules[i];
				Integer type = Integer.valueOf(rule.split("\\^")[0]);
				// 为前期规则
				if (type == ruleType_0) {
					RuleParse preRule = new RuleParse(rule.trim());
					preRules.add(preRule);
				}
				// 观察点规则
				if (type == ruleType_1) {
					parseObserverRule(rule.trim());
				}
				// 实时规则
				if (type == ruleType_2) {
					RuleParse realRule = new RuleParse(rule.trim());
					realRules.add(realRule);
				}
			}
		} catch (Exception e) {
			log.error("RealRuleParse failed!", e);
		}
		this.realRules = realRules;
		this.preRules = preRules;
	}

	private void parseObserverRule(String rrule) {
		String[] prrules = rrule.trim().split("\\^");
		findtype = Integer.valueOf(prrules[1]);
		Integer type1 = Integer.valueOf(prrules[2]);
		Integer type2 = Integer.valueOf(prrules[4]);
		if (type1 == 0)
			observerCreateRule = RuleParse.creatRealRule(prrules[3]);
		if (type2 == 1)
			observerDelRule = RuleParse.creatRealRule(prrules[5]);
	}

	public void check(Counter ccounter, RealtimeDataItem citem,
			Map<String, String> _valueMap, RealtimeCacheWapper rcWapper) {
		try {
			String key = this.txlx +"_"+ citem.getUidentify();
			if (_ws.containsKey(key))
				return;
			Counter observer = getObserver(citem.getUidentify());
			// 创建观察点
			if (observerCreateRule != null
					&&  observer== null
					&& observerCreateRule.check(ccounter, _valueMap, citem,
							ruleType_1)) {
				observer = rcWapper.findObserver(findtype);
				if(observer!=null)
				{
					putObserver(citem.getUidentify(), observer);
				}
			}
			if(observer==null)
				return;
			if (observer != null) {
				double oczf = RealUtil.computeZfN(citem, observer.getItem());// 此刻到观察点之间的涨幅
				// 涨速
				Double ozsu = RealUtil.computeZSUN(citem, observer.getItem());
				if (ozsu == 0)
				{
					observer = null;
					removeObserver(citem.getUidentify());
					return;
				}
				// 此时刻与观察点这段时间内的量与前一日平均成交量的比
				double colb = RealUtil.computeLpAB(citem, observer.getItem(),
						StockConstants.INDEX_CODE_TRADE_CJE);
				// 此时刻与观察点这段时间内的量 与 5日内的平均流量的比
				double colb5 = RealUtil.computeLpAB(citem, observer.getItem(),
						StockConstants.INDEX_CODE_TRADE_5AVGE);
				// 此时刻与观察点这段时间内的量 与 30日内的平均流量的比
				double colb30 = RealUtil.computeLpAB(citem, observer.getItem(),
						StockConstants.INDEX_CODE_TRADE_30AVGE);

				_valueMap.put("colb", String.valueOf(colb));
				_valueMap.put("colb5", String.valueOf(colb5));
				_valueMap.put("colb30", String.valueOf(colb30));

				_valueMap.put("ozsu", String.valueOf(ozsu));
				_valueMap.put("oczf", String.valueOf(oczf));
				// 多少分钟内
				long tsec = HUtilService.getInstance().getCurDayTradeLong(
						citem.getUidentify(), citem.getTime(),
						observer.getItem().getTime())
						/ (1000 * 60);
				Double maxCheckSecond = ConfigCenterFactory
						.getDouble(
								"realtime_server.realtime_monitor_check_maxCheckSecond",
								15.0);
				// 超过10分钟，则去掉检查点
				if (tsec == 0 || tsec > maxCheckSecond)
				{
					observer = null;
					removeObserver(citem.getUidentify());
					return;
				}
			}

			// 在规则的有效时间段内的才检验
			if (ccounter.getCurSeconds() >= getSecondStart()
					&& ccounter.getCurSeconds() <= getSecondEnd()) {

				if (preRules!=null&&preRules.size()>0&&!preRuleCheck(ccounter, citem, _valueMap))
					return;

				for (RuleParse rp : realRules) {
					if (!rp.check(ccounter, _valueMap, citem, ruleType_2)) {
						return;
					}
				}
				if (SLogFactory.isopen("snn_realmonitor_log_isopen_prewarn")) {
					log.info("start:^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
					log.info("warnLogFomat="
							+ RealUtil.warnLogFomat(citem, _valueMap, ccounter));
					log.info("----------citem:" + citem);

					log.info("----------time="
							+ DateUtil.format2String(new Date(ccounter
									.getItem().getUptime())) + ",ccounter:"
							+ ccounter);
					if (observer != null)
						log.info("----------time="
								+ DateUtil.format2String(new Date(observer
										.getItem().getUptime())) + ",observer:"
								+ observer);
					log.info("end:^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
				}
				// 如果检察成功，则去掉检查点
				String info = getEventInfo(_valueMap);
				if (SLogFactory
						.isopen("snn_realmonitor_log_isopen_onlywarnlogFormat")) {
					log.info("【事件类型】："
							+ info
							+ "----------指标="
							+ RealUtil.warnLogFomat(citem, _valueMap, ccounter)
							+ ";ztSeconds="
							+ ZfCounter.getInstance().getZTSeconds(
									citem.getUidentify()) + ";");
				}
				RealtimeDataItem preitem = null;
				if (observer != null) {
					preitem = observer.getItem();
				} 
				if (_ws.containsKey(key))
					return;
				RealUtil.warn(citem, preitem, info,
						RealUtil.warnLogFomat(citem, _valueMap, ccounter),
						getWarnType(), getEventType());
				_ws.put(key, citem.getUptime());
				observer = null;
				removeObserver(citem.getUidentify());
			}

			// 创建观察点
			if (observer != null && observerDelRule != null
					&& observerDelRule.check(ccounter, _valueMap, citem, 1)) {
				observer = null;
				removeObserver(citem.getUidentify());
			}
		} catch (Exception e) {
			log.error("expression compute failed!exp=" + this.eventTypeDesc, e);
		}
	}

	private boolean preRuleCheck(Counter ccounter, RealtimeDataItem citem,
			Map<String, String> _valueMap) {
		// 对于非实时的前置规则，一天只检查一次
		if (_preRulecheckFilter_not.isFind(citem.getUidentify())) {
			Long pexperied = _preRulecheckFilter_not.get(citem.getUidentify());
			// 失效时间，单位：小时
			long experiedHour = ConfigCenterFactory.getLong(
					"realtime_server.real_moniter_findObserver_sizeLimit", 8l);
			if (citem.getTime() - pexperied > experiedHour * 3600 * 1000) {
				_preRulecheckFilter_not.remove(citem.getUidentify());
			}
		}

		// 对于非实时的前置规则，一天只检查一次
		if (_preRulecheckFilter_ok.isFind(citem.getUidentify())) {
			Long pexperied = _preRulecheckFilter_ok.get(citem.getUidentify());
			// 失效时间，单位：小时
			long experiedHour = ConfigCenterFactory.getLong(
					"realtime_server.real_moniter_findObserver_sizeLimit", 8l);
			if (citem.getTime() - pexperied > experiedHour * 3600 * 1000) {
				_preRulecheckFilter_ok.remove(citem.getUidentify());
			}
		}
		if (!_preRulecheckFilter_not.isFind(citem.getUidentify())) {
			if (preRules != null) {
				boolean isOk = false;
				boolean hasPreRules = false;
				// 如果当天已检查通过，则不进行重复检查，直接通过
				if (!_preRulecheckFilter_ok.isFind(citem.getUidentify())) {
					// 先处理非实时的
					for (RuleParse preRule : preRules) {
						// 如果只要有一个满足返回，就返回true
						if (!preRule.isReal()) {
							hasPreRules=true;
							if (preRule.check(ccounter, _valueMap, citem,
									ruleType_0)) {
								_preRulecheckFilter_ok.put(citem.getUidentify(),
										citem.getTime());
								isOk = true;
								break;
							} 
						}
					}
					//有非实时的前置规则，且不满足
					if(hasPreRules&&!isOk)
					{
						_preRulecheckFilter_not.put(citem.getUidentify(),
								citem.getTime());
						return false;
					}
				}
				boolean hasRealPreRules = false; 
				// 处理实时的
				for (RuleParse preRule : preRules) {
					if (preRule.isReal()) {
						hasRealPreRules = true;
						if (preRule.check(ccounter, _valueMap, citem,
								ruleType_0)) {
							return true;
						}
					}
				}
				//没有实时的前置规则
				if(!hasRealPreRules)
					return true;
			}

		}

		return false;

	}

	// private StringBuilder replacePatten2Value(StringBuilder sb, String
	// patten,
	// String value) {
	// while (true) {
	// int start = sb.indexOf(patten);
	// if (start >= 0) {
	// int end = start + patten.length();
	// sb = sb.replace(start, end, value);
	// } else {
	// break;
	// }
	//
	// }
	// return sb;
	// }

	public String getOrule() {
		return orule;
	}

	public void setOrule(String orule) {
		this.orule = orule;
	}

	public Map<String, String> getNameMap() {
		return nameMap;
	}

	public void setNameMap(Map<String, String> nameMap) {
		this.nameMap = nameMap;
	}

	public int getWarnType() {
		return warnType;
	}

	public void setWarnType(int warnType) {
		this.warnType = warnType;
	}

	public String getEventInfo(Map<String, String> _valueMap) {
		return eventTypeDesc;
	}

	public String getEventTypeDesc() {
		return eventTypeDesc;
	}

	public void setEventTypeDesc(String eventTypeDesc) {
		this.eventTypeDesc = eventTypeDesc;
	}

	public int getSecondStart() {
		return secondStart;
	}

	public void setSecondStart(int secondStart) {
		this.secondStart = secondStart;
	}

	public int getSecondEnd() {
		return secondEnd;
	}

	public void setSecondEnd(int secondEnd) {
		this.secondEnd = secondEnd;
	}

	public int getEventType() {
		return eventType;
	}

	public void setEventType(int eventType) {
		this.eventType = eventType;
	}

	public void clear_now() {
		_ws.clear();
		System.out.println("_preRulecheckFilter_ok:" + _preRulecheckFilter_ok);
		System.out
				.println("_preRulecheckFilter_not:" + _preRulecheckFilter_not);
		_preRulecheckFilter_not.clear();
		_preRulecheckFilter_ok.clear();
		observerMap.clear();
		System.out.println("_preRulecheckFilter_ok:" + _preRulecheckFilter_ok);
		System.out
				.println("_preRulecheckFilter_not:" + _preRulecheckFilter_not);
	}

	public void clearWs(RealtimeDataItem item) {
		if (item.getUptime() - lastClear > experied) {
			Iterator<String> iter = _ws.keySet().iterator();
			while (iter.hasNext()) {
				String k = iter.next();
				if(!k.contains(this.txlx))
					continue;
				long t = _ws.get(k);
				if (k.endsWith(".hk")) {
					if (item.getUptime() - t > experied * 1.5)
						_ws.remove(k);
				} else {
					if (item.getUptime() - t > experied)
						_ws.remove(k);
				}
			}
			lastClear = item.getUptime();
		}

	}

	public int getFindtype() {
		return findtype;
	}

	public void setFindtype(int findtype) {
		this.findtype = findtype;
	}

	public Counter getObserver(String uidentify)
	{
		return observerMap.get(uidentify);
	}
	public void putObserver(String uidentify,Counter observer)
	{
		if(observer==null)
			return ;
		observerMap.put(uidentify,observer);
	}
	public void removeObserver(String uidentify)
	{
		observerMap.remove(uidentify);
	}
	@Override
	public String toString() {
		return "RealRuleParse [orule=" + orule + ", nameMap=" + nameMap
				+ ", eventType=" + eventType + ", eventTypeDesc="
				+ eventTypeDesc + ", secondStart=" + secondStart
				+ ", secondEnd=" + secondEnd + ", warnType=" + warnType
				+ ", realRules=" + realRules + ", preRules=" + preRules
				+ ", observerCreateRule=" + observerCreateRule
				+ ", observerDelRule=" + observerDelRule + ", findtype=" + findtype + "]";
	}

}
