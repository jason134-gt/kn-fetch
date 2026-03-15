package com.yz.stock.monitor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.model.USubject;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.hfunction.HUtilService;
import com.yfzx.service.realtime.RealtimeDataItem;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;

public class RealUtil {
	static Logger log = LoggerFactory.getLogger(RealUtil.class);
	static Long timeOffset = 0l;
	public static int warnType_0 = 0;
	public static int warnType_1 = 1;
	public static int warnType_2 = 2;
	static Map<String,String> _pm ;
	static {
		init();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			public void refresh() {
				init();
			}
		});
	}

	private static void init() {
		try {
			String pls = ConfigCenterFactory.getString("realtime_server.chaces_k_category_map", "7:js_jqjx");
			if(!StringUtil.isEmpty(pls))
			{
				Map<String,String> npm = new HashMap<String,String>();
				for(String pli:pls.split(","))
				{
					String[] pla = pli.split(":");
					String keys = pla[0];
					String c = pla[1];
					if(!StringUtil.isEmpty(keys))
					{
						for(String key:keys.split("\\|"))
						{
							if(!StringUtil.isEmpty(key))
								npm.put(key, c);
						}
					}
					
				}
				if(npm.size()!=0)
					_pm = npm;
			}
			timeOffset = ConfigCenterFactory.getLong(
					"realtime_server.real_moniter_timeOffset", 30000l);
			
		} catch (Exception e) {
			log.error("parsePrerules failed!", e);
		}
	}
	
	private static Long getCreateTime(String uidentify, long ptime, long ctime) {
		Long createtime = ctime - timeOffset;
		if (createtime < ptime)
			createtime = ctime;
		Long tlong = HUtilService.getInstance().getCurDayTradeLong(uidentify,
				ctime);
		if (tlong < timeOffset)
			createtime = ctime;
		return createtime;
	}
	public static void warn(RealtimeDataItem citem, RealtimeDataItem item,
			String info, String warnLog, int warntype, int eventType) {
		if (SLogFactory.isopen("snn_realmonitor_log_isopen_onlywarnlog")) {
			log.info("----------warnlog=" + warnLog);
		}
		USubject usubject = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(citem.getUidentify());
		String name = usubject.getName() + "(" + usubject.getUidentify() + ") ";
		ZfCounter.getInstance().add(citem);
		Long createtime = getCreateTime(item.getUidentify(), item.getTime(),
				citem.getTime());
		TradeAlarmMsg tam = new TradeAlarmMsg();
		tam.setSourceid(citem.getUidentify());
		tam.setStime(item.getTime());
		tam.setEtime(createtime);
		tam.setTime(createtime);
		tam.putAttr("desc", String.valueOf(eventType));
		tam.putAttr("info", info);
		tam.putAttr("summary", TradeService.getInstance().getRealMsgSummery(
				Integer.valueOf(eventType)));
		tam.putAttr("title", name + TradeService.getInstance().getRealMsgDescByRealType(Integer.valueOf(eventType))+"："+info);
		tam.putAttr("price", citem.getC());
		tam.setMsgType(MsgConst.MSG_TRADEMSG_TYPE_0);
		tam.putAttr("ktype", "f");
		String c = getSetChancesCategory(String.valueOf(eventType));
		if(!StringUtil.isEmpty(c))
		{
			tam.putAttr(StockConstants.CHANCETAG, c);
			tam.putAttr("op", 2);
			tam.setMsgType(MsgConst.MSG_TRADEMSG_TYPE_3);
		}
		
		doWarn(tam,warntype);

	}
	
	public static String getSetChancesCategory(String eventType) {
		// TODO Auto-generated method stub
		return _pm.get(eventType);
	}

	private static void doWarn(TradeAlarmMsg tam, int warntype) {
		int openWarn = ConfigCenterFactory
				.getInt("realtime_server.openwarn", 1);
		if (openWarn == 1) {
			try {
				if (warntype == warnType_0) {
					TradeService.getInstance().notifyToUser(tam);
					TradeService.getInstance().notifyTheEventChance(tam);
				}
				if (warntype == warnType_1) {
					TradeService.getInstance().notifyTheEventChance(tam);
				}
				if (warntype == warnType_2) {
					TradeService.getInstance().notifyToUser(tam);
				}
			} catch (Exception e) {
				log.error("warn failed!", e);
			}
		}
		
	}

	public static double computeLpAB(RealtimeDataItem citem, RealtimeDataItem item,
			String indexcode) {

		// 前一天的5日成交量
		// Double lcje = getYesterdayCje(citem, citem.getCje(),
		// citem.getTime());
		Date stime = DateUtil.getDayStartTime(citem.getTime());
		IndexMessage im = SMsgFactory.getUMsg(citem.getUidentify(),
				StockConstants.INDEX_CODE_TRADE_CJE, stime);
		im.setNeedAccessExtIndexDb(false);
		im.setNeedAccessCompanyBaseIndexDb(false);
		Date time = IndexService.getInstance().getNTradeTimeAfterCurDay(im,
				stime, -1);
		if (time == null)
			return -1.0;
		im.setTime(time);
		Double lcje = IndexValueAgent.getIndexValue(citem.getUidentify(),
				indexcode, time);

		if (lcje == null || lcje == 0)
			return -1;
		double lbs = TradeService.getInstance().getTradeTimeRegion(
				citem.getUidentify());
		// 昨天成交量换成毫秒
		double llp = lcje / lbs;

		long tsec = HUtilService.getInstance().getCurDayTradeLong(
				citem.getUidentify(), citem.getTime(), item.getTime());
		// 换成毫秒
		double clp = (citem.getCje() - item.getCje()) / tsec;
		double rlp = clp / llp;
		return rlp;
	}
	
	/**
	 * 不含预估，当日平均量比
	 * 
	 * @param im
	 * @param cje
	 * @param cur
	 * @return
	 */
	public static Double computeCurLbAB(RealtimeDataItem citem, double cje, Long cur,
			String indexcode) {
		Date stime = DateUtil.getDayStartTime(citem.getTime());
		IndexMessage im = SMsgFactory.getUMsg(citem.getUidentify(),
				StockConstants.INDEX_CODE_TRADE_CJE, stime);
		im.setNeedAccessExtIndexDb(false);
		im.setNeedAccessCompanyBaseIndexDb(false);
		Date time = IndexService.getInstance().getNTradeTimeAfterCurDay(im,
				stime, -1);
		if (time == null)
			return -1.0;
		im.setTime(time);
		Double ycje = IndexValueAgent.getIndexValue(citem.getUidentify(),
				indexcode, time);
		if (ycje == null || ycje == 0)
			return -1.0;

		// 预估的成交量
		Double ygcje = 0.0;
		// 取今天到此刻的交易时间
		Long tradeLong = HUtilService.getInstance().getCurDayTradeLong(
				im.getUidentify(), cur);
		// 交易时长需要大于10分种，即开盘前10分种还是取前一天的做对比
		// if (tradeLong != -1 && tradeLong > 5 * 60 * 1000) {
		// 取全天交易时间
		double at = TradeService.getInstance().getTradeTimeRegion(
				im.getUidentify());
		ygcje = cje * at / tradeLong;
		// }
		return ygcje / ycje;
	}
	
	/**
	 * 瞬间量比，与30分钟的平均流速比
	 * 
	 * @param citem
	 * @param item
	 * @return
	 */
	public static Double computeABLb(RealtimeDataItem citem,
			RealtimeDataItem item, Double jlb) {
		if (jlb == 0)
			return 0.0;
		Double blb = jlb / 60;// 换算成秒
		if (citem.getTime() == item.getTime())
			return 1.0;
		// 毫秒
		long tsec = HUtilService.getInstance().getCurDayTradeLong(
				citem.getUidentify(), citem.getTime(), item.getTime());

		// 换成分钟
		tsec = tsec / (60 * 1000);
		double clp = (citem.getCje() - item.getCje()) / tsec;
		double rlp = clp / blb;
		return rlp;
	}

	public static Double computeLsAvg(Double lsavg, int size, Double addcje,
			int count) {
		if (size < count) {
			count = size;
		}
		lsavg = lsavg * (count - 1) / count + addcje * 1 / count;
		return lsavg;
	}

	public static Double computeZfN(RealtimeDataItem citem,
			RealtimeDataItem preitem) {
		if(preitem==null)
			preitem = citem;
		double cp = citem.getC();
		double ojk = preitem.getJk();
		double ozs = preitem.getZs();
		double zfn = 0.0;// 此刻到观察点之间的涨幅

		if (cp != 0 && ojk != 0) {
			zfn = (cp - ojk) / ozs * 100;
		}
		return zfn;
	}

	public static Double computeZSUN(RealtimeDataItem citem,
			RealtimeDataItem preitem) {
		if(preitem==null)
			preitem = citem;
		// 多少分钟内
		long tsec = HUtilService.getInstance().getCurDayTradeLong(
				citem.getUidentify(), citem.getTime(), preitem.getTime())
				/ (1000 * 60);
		if (tsec == 0)
			return 0.0;
		double cp = citem.getC();
		double ojk = preitem.getJk();
		double ozs = preitem.getZs();
		double zfn = 0.0;
		double zsun = 0.0;
		if (cp != 0 && ojk != 0) {
			zfn = (cp - ojk) / ozs * 100;
		}
		zsun = zfn / tsec;
		return zsun;
	}
	
	
	public static String warnLogFomat(RealtimeDataItem citem, Map<String, String> vm, Counter ccounter) {
		StringBuffer sb = new StringBuffer();
		String warnLogFomats = ConfigCenterFactory.getString(
				"realtime_server.warnLogFomats", "");
		sb.append(citem.getUidentify());
		sb.append(";");
		sb.append(DateUtil.format2String(new Date(citem.getTime())));
		sb.append(";");
		if (!StringUtil.isEmpty(warnLogFomats)) {
			for (String f : warnLogFomats.split(";")) {
				try {
					String vv = vm.get(f);
					if(vv==null)
					{
						Object ov = StockUtil.getFieldValueByColumnNameV2(ccounter, f);
						if(ov!=null)
							vv = ov.toString();
					}
					if(vv==null)
						continue;
					sb.append(f);
					sb.append("=");
					Double v = Double.valueOf(vv);
					if (v.isInfinite() || v.isNaN())
						sb.append("0.0");
					else
						sb.append(SMathUtil.getDouble(v, 3));
					sb.append(";");
				} catch (Exception e) {
					log.error("warnLogFomat failed!", e);
				}
			}
		} else {
			sb.append(vm.toString());
		}
		return sb.toString();
	}
	
	public static void preCompileLog(RealtimeDataItem citem, Tagrule tr, String key) {
		if (SLogFactory.isopen("snn_realmonitor_log_isopen_rule_precompile")) {
			IndexMessage im = SMsgFactory.getUDCIndexMessage(citem
					.getUidentify());
			im.setCompanyCode(citem.getUidentify());
			im.setTime(DateUtil.getDayStartTime(citem.getTime()));
			im.setNeedAccessExtRemoteCache(false);
			im.setNeedUseExtDataCache(true);
			im.setNeedComput(true);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedRealComputeIndustryValue(false);
			String compileResult = CRuleService.getInstance().compileRule(
					tr.getRule(), im, tr.getRuleType());
			System.out.println("time="+DateUtil.format2String(new Date(citem.getTime()))+";uid=" + citem.getUidentify() + "," + key
					+ compileResult);
		}

	}
	
	public static void preComplexCompileLog(RealtimeDataItem citem, String exp, String key) {
		if (SLogFactory.isopen("snn_realmonitor_log_isopen_rule_precompile")) {
			IndexMessage im = SMsgFactory.getUDCIndexMessage(citem
					.getUidentify());
			im.setCompanyCode(citem.getUidentify());
			im.setTime(DateUtil.getDayStartTime(citem.getTime()));
			im.setNeedAccessExtRemoteCache(false);
			im.setNeedUseExtDataCache(true);
			im.setNeedComput(true);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedRealComputeIndustryValue(false);
			String compileResult = CRuleService.getInstance().compileRule(
					exp, im, 2);
			System.out.println("time="+DateUtil.format2String(new Date(citem.getTime()))+";uid=" + citem.getUidentify() + "," + key
					+ compileResult);
		}

	}
}
