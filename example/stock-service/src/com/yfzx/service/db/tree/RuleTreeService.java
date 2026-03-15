package com.yfzx.service.db.tree;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.tree.RuleElement;
import com.stock.common.model.tree.RuleTreeNode;
import com.stock.common.model.tree.SimpleRuleTreeNode;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.UnitUtil;
import com.yfzx.dc.compile.FunctionCompile;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;

public class RuleTreeService{

	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	// 匹配指标规则
	Pattern p = Pattern.compile("\\$\\{[0-9]+[,0-9+-]*\\}");
	Pattern zp = Pattern.compile("\\$\\{[\\u4e00-\\u9fa5a-zA-Z：0-9]+[\\u4e00-\\u9fa5()）：（、a-zA-Z0-9_-|/\\\\]*:{1}[0-9]+[,0-9+-]*\\}");
	// 匹配-,+号的数学表达式
	Pattern mP = Pattern.compile("[0-9-]+[+-]+[0-9-+]+");
	FunctionCompile fcompile = FunctionCompile.getInstance();
	static Map<String,RuleTreeNode> tm = new HashMap<String,RuleTreeNode>();
	DictService ds = DictService.getInstance();
	static Map<String,String> prm = new HashMap<String,String>();
	private  CRuleService crs = CRuleService.getInstance();
	private static RuleTreeService instance = new RuleTreeService();
	public static RuleTreeService getInstance()
	{
		return instance;
	}
	private RuleTreeService() {

	}
	static
	{
		initNewRuleMap();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			public void refresh() {
				initNewRuleMap();
				//重新构建下
				RuleTreeService.getInstance().parseAllRule2Tree();
				
			}
			
		});
	}
	public void parseAllRule2Tree() {
		tm.clear();
		List<Dictionary> dl = ds.getAllDictionaryListFromCache();
		for(Dictionary d : dl)
		{
			if(!StockUtil.isBaseIndex(d.getType()))
			{
				parseRuleTree(d.getIndexCode());
			}
				
				
		}
	}
	private static void initNewRuleMap() {
		
		String nrs = ConfigCenterFactory.getString("stock_zjs.new_parse_rule", "");
		if(!StringUtil.isEmpty(nrs))
		{
			prm.clear();
			for(String nr:nrs.split(";"))
			{
				if(StringUtil.isEmpty(nr)) continue;
				try {
					String indexcode = nr.split("\\|")[0].trim();
					String rule = nr.split("\\|")[1].trim().split(",")[0];
					String showRule = nr.split("\\|")[1].trim().split(",")[1];
					if (!StringUtil.isEmpty(rule)&&!rule.equals("\"\""))
						prm.put(indexcode, rule);
					if (!StringUtil.isEmpty(showRule)&&!showRule.equals("\"\""))
						prm.put(getIndexcodeShowRuleKey(indexcode), showRule);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
	}
	private static String getIndexcodeShowRuleKey(String indexcode) {
		// TODO Auto-generated method stub
		return "sr_"+indexcode;
	}
	public static String getIndexcodeShowRule(String indexcode) {
		
		return prm.get(getIndexcodeShowRuleKey(indexcode));
	}
	public RuleTreeNode parseRuleTree(String pindexcode) {
		RuleTreeNode rtn = null;
		try {
			if(StringUtil.isEmpty(pindexcode)) 
			{
				logger.error("indexcode is not found!,indexcode = "+pindexcode);
				return null;
			}
			Dictionary dict = DictService.getInstance().getDataDictionary(pindexcode);
			if (dict == null) {
				logger.warn("not found dictionary ! indexCode :"
						+ pindexcode);
				return null;
			}
			
			rtn = tm.get(pindexcode);
			if(rtn==null)
			{
				rtn = new RuleTreeNode();
				tm.put(pindexcode, rtn);
			}
			else
			{
				return rtn;
			}
			String rcomments = "";
			String s="";
			String nrc = getNewRuleComments(pindexcode);
			if(!StringUtil.isEmpty(nrc))
			{
				rcomments = getRuleComments(nrc);
				s = StockUtil.getRuleByComments(nrc);
			}
//			if(StringUtil.isEmpty(s))
//				return null;
//			if(StringUtil.isEmpty(s))
//			{
//				Cfirule crule = CRuleService.getInstance().getCIndexRuleByCode(pindexcode);
//				if(crule==null) 
//				{
//					logger.error("indexcode is not found!,indexcode = "+pindexcode);
//					return null;
//				}
//				rcomments = getRuleComments(crule.getComments());
//				s = crule.getRule();
//			}
			RuleElement re = new RuleElement(dict.getShowName(),dict,rcomments);
			rtn.setReference(re);
			
			
			StringBuilder sb = new StringBuilder();
			// 如果包含有函数，则由函数对象编译
			if (fcompile.isContainFunction(s)) {
				//如果是函数，则不向下解析
				return rtn;
			}
			sb.append(s);
			Matcher m = p.matcher(s);
			while (m.find()) {
				String t = m.group();
				String indexCode = "";
				String[] tmp = t.split(",");
				if (tmp.length == 1) {
					indexCode = t.substring(2, t.length() - 1);

				} else {
					indexCode = tmp[0].substring(2, tmp[0].length());

				}
				Dictionary d = DictService.getInstance().getDataDictionary(indexCode);
				if (d == null) {
					logger.warn("not found dictionary ! indexCode :"
							+ indexCode);
					continue;
				}
				RuleTreeNode crtn = null;
				if (StockUtil.isBaseIndex(d.getType())) {
					String nnrc = getNewRuleComments(d.getIndexCode());
					if(!StringUtil.isEmpty(nnrc))
					{
						crtn = parseRuleTree(indexCode);
					}
					else
					{
						crtn = tm.get(indexCode);
						if(crtn==null)
						{
							crtn = new RuleTreeNode();
							tm.put(indexCode, crtn);
						}
						RuleElement cre = new RuleElement(d.getShowName(),d,"");
						crtn.setReference(cre);
					}
				}
				else
				{
					crtn = parseRuleTree(indexCode);
				}
				if(crtn!=null)
				{
					crtn.putCall(rtn);
					rtn.putChildren(crtn);
				}
					
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("compile failed", e);
		}

		return rtn;
	}

	public String getNewRuleComments(String pindexcode) {
		
		return prm.get(pindexcode);
	}
	private String getRuleComments(String s) {
		StringBuilder sb = new StringBuilder();
		try {
			// 如果包含有函数，则由函数对象编译
//			if (fcompile.isContainFunction(s)) {
//				//如果是函数，则不向下解析
//				return null;
//			}
			
			sb.append(s);
			Matcher m = zp.matcher(s);
			while (m.find()) {
				String t = m.group();
				String indexCode = "";
				// 以月份为单位
				int timeAdd = 0;
				String[] tmp = t.split(",");
				if (tmp.length == 1) {
					indexCode = t.substring(2, t.length() - 1).split(":")[1];

				} else {
					indexCode = tmp[0].substring(2, tmp[0].length()).split(":")[1];
					String tExp = tmp[1].substring(0, tmp[1].length() - 1);
					// 如果时间变量为表达式,则先运算之
					if (isExpression(tExp)) {
						Double d = CRuleService.getInstance().executeExpresion(tExp);
						if (d != null) {
							timeAdd = d.intValue();
						}

					} else {
						timeAdd = Integer.valueOf(tExp);
					}

				}
				Dictionary d = DictService.getInstance().getDataDictionary(indexCode);
				if (d == null) {
					logger.warn("not found dictionary ! indexCode :"
							+ indexCode);
					return null;
				}
				String sn = getShowName(d.getShowName(),timeAdd);
				// 替换掉规则中的指标
				sb = replacePatten2Value(sb, t, sn);
			}
			
			
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("compile failed", e);
		}
		return sb.toString();
	}

	private String getShowName(String showName, int timeAdd) {
		String sn = showName;
		String t = "";
		switch(timeAdd)
		{
		case 3: 
			t="(下期)";
			break;
		case 6: 
			t="(下下期)";
			break;
		case 9: 
			t="(下下下期)";
			break;
		case 12: 
			t="(下年同期)";
			break;
		case -3: 
			t="(上期)";
			break;
		case -6: 
			t="(上上期)";
			break;
		case -9: 
			t="(上上上期)";
			break;
		case -12: 
			t="(上年同期)";
			break;
		}
		 if(!StringUtil.isEmpty(t))
			 sn = sn+t;
		return sn;
	}

	private StringBuilder replacePatten2Value(StringBuilder sb, String patten,
			String value) {
		// TODO Auto-generated method stub
		int start = sb.indexOf(patten);
		int end = start + patten.length();
		return sb.replace(start, end, value);
	}

	private boolean isExpression(String tExp) {
		// TODO Auto-generated method stub
		Matcher m = mP.matcher(tExp);
		if (m.matches()) {
			return true;
		}
		return false;
	}

	public RuleTreeNode getRuleTreeNodeFromCache(String indexcode)
	{
		return tm.get(indexcode);
	}
	/**
	 * 如果没有取到就实时解析，慎用
	 * @param indexcode
	 * @return
	 */
	public RuleTreeNode getRuleTreeNodeAndReal(String indexcode) {
		RuleTreeNode rtn = getRuleTreeNodeFromCache(indexcode);
		 if(rtn==null)
			 rtn = parseRuleTree(indexcode);
		return rtn;
	}
	
	/**
	 * 
	 * @param rtn 
	 * @param depth  要拷贝的深度
	 * cdepth 当前深度
	 * @return 
	 */
	public  SimpleRuleTreeNode getSimpleRuleTreeNode(RuleTreeNode rtn, int depth,int cdepth) {
		if(cdepth>depth) return null;
		SimpleRuleTreeNode  srn = new SimpleRuleTreeNode();
		RuleElement re = (RuleElement) rtn.getReference();
		Dictionary rd = re.getD();
		srn.setC(rd.getIndexCode());
		srn.setN(rd.getShowName());
		srn.setR(re.getRuleComments());
		cdepth +=1;
//		for(RuleTreeNode trtn : rtn.getCalls())
//		{
//			SimpleRuleTreeNode tsrtn = getSimpleRuleTreeNode(trtn,depth,cdepth);
//			if(tsrtn!=null)
//				srn.putCall(tsrtn);
//		}
		for(RuleTreeNode tc : rtn.getChildrens())
		{
			SimpleRuleTreeNode ttc = getSimpleRuleTreeNode(tc,depth,cdepth);
			if(ttc!=null)
				srn.putChildren(ttc);
		}
		return srn;
	}
	
	/**
	 * 支持同一公司不同期比较，也支持不同公司同一期比较
	 * 建议参数都填好
	 * @param rtn
	 * @param depth
	 * @param cdepth
	 * @param companycode_a 公司A
	 * @param companycode_b 公司B
	 * @param timea 公司A的时间点A
	 * @param timeb 公司A的时间点B
	 * @return
	 */
	public SimpleRuleTreeNode getSimpleRuleTreeNode2CompnaySameTime(RuleTreeNode rtn,int depth,int cdepth,
			String companycode_a,String companycode_b,Date timea,Date timeb) {
		if(cdepth>depth) return null;
		SimpleRuleTreeNode  srn = new SimpleRuleTreeNode();
		RuleElement re = (RuleElement) rtn.getReference();
		Dictionary rd = re.getD();
		srn.setC(rd.getIndexCode());
		srn.setN(rd.getShowName());
		srn.setR(re.getRuleComments());
		srn.setUt(rd.getUnit());
		if(rtn.getChildrens()!=null&&rtn.getChildrens().size()>0)
			srn.setIsl(false);
		if(StringUtil.isEmpty(companycode_b)){//相同公司比较时
			companycode_b = companycode_a;
		}
		if(timeb==null){//不同公司 同一期比较时
			timeb = timea;
		}
		
		Date time = StockUtil.getDefaultPeriodTime(null);//最近一期时间
		//传入的时间 要小于最后一期时间
		if(timea==null){
			timea = CompanyService.getInstance().getLatestReportTime(companycode_a);
		}
		if(timeb==null){
			timeb = CompanyService.getInstance().getLatestReportTime(companycode_b);
		}
		if(time.compareTo(timea) < 0){
			timea = time;
		}
		if(time.compareTo(timeb) < 0){
			timeb = time;
		}
		if(time==null) return srn;
		IndexMessage ima = SMsgFactory.getUMsg(companycode_a, rd.getIndexCode(), timea);
		ima.setNeedAccessExtRemoteCache(true);
		//取指标值
		Double va = IndexValueAgent.getIndexValue(ima);
		
		IndexMessage imb = SMsgFactory.getUMsg(companycode_b, rd.getIndexCode(), timeb);
		imb.setNeedAccessExtRemoteCache(true);
		//取指标值
		Double vb = IndexValueAgent.getIndexValue(imb);
		if(va!=null&&vb!=null&&va!=0&&vb!=0)
		{
			Double t = (va-vb) / Math.abs(vb);
			srn.setT(t);
			srn.setV(SMathUtil.getDouble(UnitUtil.formatByDefaultUnit(rd, va), 2));
		}
		cdepth +=1;
		for(RuleTreeNode tc : rtn.getChildrens())
		{
			SimpleRuleTreeNode ttc = getSimpleRuleTreeNode2CompnaySameTime(tc,depth,cdepth,companycode_a,companycode_b,timea,timeb);
			if(ttc!=null)
			{
				String showRule = RuleTreeService.getIndexcodeShowRule(ttc.getC());
				if(!StringUtil.isEmpty(showRule))
					ttc.setShowRule(showRule);
				srn.putChildren(ttc);
			}
		}
		return srn;
	}
	
	/**
	 * 取不同公司，同一时间的对比
	 * @param rtn
	 * @param depth
	 * @param cdepth
	 * @param companycode_a
	 * @param companycode_b
	 * @param time
	 * @return
	 */
	public SimpleRuleTreeNode getSimpleRuleTreeNode2CompnaySameTime(RuleTreeNode rtn,int depth,int cdepth,
			String companycode_a,String companycode_b) {
		if(cdepth>depth) return null;
		SimpleRuleTreeNode  srn = new SimpleRuleTreeNode();
		RuleElement re = (RuleElement) rtn.getReference();
		Dictionary rd = re.getD();
		srn.setC(rd.getIndexCode());
		srn.setN(rd.getShowName());
		srn.setR(re.getRuleComments());
		Date time = StockUtil.getApproPeriod(Calendar.getInstance().getTime());
		Date timea = CompanyService.getInstance().getLatestReportTime(companycode_a);
		Date timeb = CompanyService.getInstance().getLatestReportTime(companycode_b);
		if(timea.compareTo(timeb)>0)
			time = timeb;
		else
			time = timea;
		if(time==null) return srn;
		IndexMessage ima = SMsgFactory.getUMsg(companycode_a, rd.getIndexCode(), time);
		//取指标值
		Double va = IndexValueAgent.getIndexValue(ima);
		
		IndexMessage imb = SMsgFactory.getUMsg(companycode_b, rd.getIndexCode(), time);
		//取指标值
		Double vb = IndexValueAgent.getIndexValue(imb);
		if(va!=null&&vb!=null&&va!=0&&vb!=0)
		{
			Double t = (va-vb) / Math.abs(vb);
			srn.setT(t);
		}
		cdepth +=1;
//		for(RuleTreeNode trtn : rtn.getCalls())
//		{
//			SimpleRuleTreeNode tsrtn = getSimpleRuleTreeNode(trtn,depth,cdepth);
//			if(tsrtn!=null)
//				srn.putCall(tsrtn);
//		}
		for(RuleTreeNode tc : rtn.getChildrens())
		{
			SimpleRuleTreeNode ttc = getSimpleRuleTreeNode2CompnaySameTime(tc,depth,cdepth,companycode_a,companycode_b);
			if(ttc!=null)
				srn.putChildren(ttc);
		}
		return srn;
	}
	/**
	 * 取同一公司，指定时间段的对比
	 * @param rtn
	 * @param depth
	 * @param cdepth
	 * @param companycode
	 * @param time
	 * @return
	 */
	public SimpleRuleTreeNode getSimpleRuleTreeNode1CompanySetTime(RuleTreeNode rtn,int depth,int cdepth,
			String companycode,Date settime) {
		if(cdepth>depth) return null;
		SimpleRuleTreeNode  srn = new SimpleRuleTreeNode();
		RuleElement re = (RuleElement) rtn.getReference();
		Dictionary rd = re.getD();
		srn.setC(rd.getIndexCode());
		srn.setN(rd.getShowName());
		srn.setR(re.getRuleComments());
		
		Date atime = CompanyService.getInstance().getLatestReportTime(companycode);
		IndexMessage ima = SMsgFactory.getUMsg(companycode, rd.getIndexCode(), atime);
		//取指标值
		Double va = IndexValueAgent.getIndexValue(ima);
		//默认为同比
		Date timeb = StockUtil.getNextTimeV3(atime, -12,Calendar.MONTH);
		if(settime!=null)
			timeb = settime;
		IndexMessage imb = SMsgFactory.getUMsg(companycode, rd.getIndexCode(), timeb);
		//取指标值
		Double vb = IndexValueAgent.getIndexValue(imb);
		if(va!=null&&vb!=null&&va!=0&&vb!=0)
		{
			Double t = (va-vb) / Math.abs(vb);
			srn.setT(t);
		}
		cdepth +=1;
//		for(RuleTreeNode trtn : rtn.getCalls())
//		{
//			SimpleRuleTreeNode tsrtn = getSimpleRuleTreeNode(trtn,depth,cdepth);
//			if(tsrtn!=null)
//				srn.putCall(tsrtn);
//		}
		for(RuleTreeNode tc : rtn.getChildrens())
		{
			SimpleRuleTreeNode ttc = getSimpleRuleTreeNode1CompanySetTime(tc,depth,cdepth,companycode,settime);
			if(ttc!=null)
				srn.putChildren(ttc);
		}
		return srn;
	}
	/**
	 * 
	 * @return
	 */
	public List<RuleTreeNode> getAllNoParentRuleTreeNodeFromCache() {
		List<RuleTreeNode> allNoParent = new ArrayList<RuleTreeNode>();
		Iterator<String> iter = tm.keySet().iterator();
		while(iter.hasNext())
		{
			String k = iter.next();
			RuleTreeNode rtn = tm.get(k);
			if(rtn.getCalls().size()==0)
			{
				allNoParent.add(rtn);
			}
		}
		return allNoParent;
	}
}
