package com.yz.stock.monitor;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SLogFactory;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.realtime.RealtimeDataItem;

/**
 * 实时规则解析类 格式：0|1|$oczf$>2.5 && $zsu$>0.35 && $czf$<5
 * 
 * @author：杨真
 * @date：2015年4月15日
 */
public class RuleParse {
	static Logger log = LoggerFactory.getLogger(RuleParse.class);
	String orule;
	Map<String, String> nameMap = new ConcurrentHashMap<String, String>();
	Pattern p = Pattern.compile("\\$[a-zA-Z_0-9]+\\$");
	
	public Tagrule trule;
	int type = -1;
	
	public RuleParse()
	{
		
	}
	public RuleParse(String rule)
	{
		if(!StringUtil.isEmpty(rule))
		{
			type = Integer.valueOf(rule.split("\\^")[1]); 
			
			String ttrule = subRule(rule); 
			//为实时
			if(type==0)
			{
				parseSecondRealRule(ttrule);
			}
			if(type==1)
			{
				trule = parsePreRule(ttrule);
			}
			//为复合
			if(type==2)
			{
				parseSecondRealRule(ttrule);
			}
		}
	}

	public boolean check(Counter ccounter, Map<String, String> _valueMap, RealtimeDataItem citem, int ruleType)
	{
		if(type==-1)
			return false;
		if(type==0)
		{
			String exp = parse(ccounter,_valueMap,orule);
			if (SLogFactory.isopen("snn_realmonitor_log_isopen_rule_precompile_"+ruleType)) {
				log.info("----------time="
						+ DateUtil.format2String(new Date(citem
								.getUptime())) + ",uidentify="
						+ citem.getUidentify() + ",exp="
						+ StockUtil.getExpression().compute(exp) + "|"
						+ exp);
			}
			if (StockUtil.getExpression().compute(exp) > 0) {
				return true;
			}
		}
		if(type==1)
		{
			int accord = TagruleService.getInstance().isAccord(
					citem.getUidentify(),
					DateUtil.getDayStartTime(citem.getTime()), trule.getRule(),
					trule.getRuleType());
			RealUtil.preCompileLog(citem, trule, "accord=" + accord
					+ ",precompile=");
			if (accord >0) {
				return true;
			}
		}
		if(type==2)
		{
			String exp = parse(ccounter,_valueMap,orule);
			if(!StringUtil.isEmpty(exp))
			{
				int accord = TagruleService.getInstance().isAccord(
						citem.getUidentify(),
						DateUtil.getDayStartTime(citem.getTime()), exp,
						2);
				RealUtil.preComplexCompileLog(citem, exp, "accord=" + accord
						+ ",precompile=");
				if (accord >0) {
					return true;
				}
			}
		}
		return false;
	}
	private String subRule(String rule) {
		StringBuilder sb = new StringBuilder();
		String[] sra = rule.split("\\^");
		for(int i=2;i<sra.length ;i++ )
		{
			String sr = sra[i];
			sb.append(sr);
			sb.append("^");
		}
		return sb.toString();
	}
	private Tagrule parsePreRule(String prerules) {
		Tagrule rt = null;
		try {
			if (!StringUtil.isEmpty(prerules)) {
				for (String pr : prerules.split(";")) {
					pr = pr.trim();
					if (pr.indexOf("$") >= 0) {
						String[] pra = pr.split("\\^");
						String ruletype = pra[0];
						String rule = pra[1];
						// 认为是表达式
						Tagrule tr = new Tagrule();
						tr.setRuleOriginal(rule.trim());
						tr.setRuleType(Integer.valueOf(ruletype));
						tr.setTimeUnit("d");
						tr.setTimeInterval(1);
						rt = tr;
					}
				}
			}
		} catch (Exception e) {
			log.error("parsePrerules failed!", e);
		}
		return rt;
	}

	private void parseSecondRealRule(String rrule) {
		try {
			String[] rulea = rrule.split("\\^");
			orule = rulea[0];
			StringBuilder sb = new StringBuilder();
			sb.append(orule);
			Matcher m = p.matcher(orule);
			while (m.find()) {
				String t = m.group();
				String tn = t.replaceAll("\\$", "");
				nameMap.put(t, tn);
			}
			orule = StockUtil.getRuleByComments(orule);
		} catch (Exception e) {
			log.error("RealRuleParse failed!",e);
		}

	}

	public String parse(Counter ccounter, Map<String, String> valueMap,String orule) {
		StringBuilder sb = new StringBuilder();
		sb.append(orule);
		try {
			Iterator<String> iter = nameMap.keySet().iterator();
			while (iter.hasNext()) {
				String t = iter.next();
				String tname = nameMap.get(t);
				String v = valueMap.get(tname);
				if(v==null||v.equals("null"))
				{
					Object ov = StockUtil.getFieldValueByColumnNameV2(ccounter, tname);
					if(ov!=null)
						v = ov.toString();
				}
				if(v==null||v.equals("null"))
				{
					log.error("not found the value!tname ="+tname);
					v = "0.0";
				}
				Double d = Double.valueOf(v);
				if(d.isInfinite()||d.isNaN())
					d = 0.0;
				Double fv = SMathUtil.getDouble(d, 3);
				sb = replacePatten2Value(sb, t, String.valueOf(fv));
			}
		} catch (Exception e) {
			log.error("parse failed!", e);
		}
		return sb.toString();
	}

	private StringBuilder replacePatten2Value(StringBuilder sb, String patten,
			String value) {
		while (true) {
			int start = sb.indexOf(patten);
			if (start >= 0) {
				int end = start + patten.length();
				sb = sb.replace(start, end, value);
			} else {
				break;
			}

		}
		return sb;
	}

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

	public static RuleParse creatRealRule(String rule) {
		RuleParse rp = new RuleParse();
		try {
			rp.initRealRule(rule);
		} catch (Exception e) {
			log.error("RealRuleParse failed!",e);
		}
		return rp;
	}

	public  void initRealRule(String rule) {
		try {
			type=0;
			orule = rule;
			StringBuilder sb = new StringBuilder();
			sb.append(orule);
			Matcher m = p.matcher(orule);
			while (m.find()) {
				String t = m.group();
				String tn = t.replaceAll("\\$", "");
				nameMap.put(t, tn);
			}
		} catch (Exception e) {
			log.error("RealRuleParse failed!",e);
		}
	}
	
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	@Override
	public String toString() {
		return "RuleParse [orule=" + orule + ", nameMap=" + nameMap
				+ ", trule=" + trule + ", type=" + type + "]";
	}

	public boolean isReal()
	{
		return this.type==0;
	}
	
	
}
